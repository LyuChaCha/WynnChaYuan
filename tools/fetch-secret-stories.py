#!/usr/bin/env python3
"""從 Wynncraft wiki 抓祕密發現的**過場故事**到 raw/secret-stories.json。

跟 fetch-discoveries.py 的分工
------------------------------
fetch-discoveries.py 抓的是 infobox 的 lore 欄（Wynntils 顯示的那段簡介）。
這支抓的是**走進發現點時播放的逐句故事**——secret-dialogue 語料在收的那種。
目前語料只有 The Legend of Bob 一個故事（68 句），因為文本一直只能靠玩家
在遊戲裡遇到才收得到。

為什麼可以從 wiki 抓
-------------------
比對過：Ragni's Secret Library 頁的 == The Chapters == 章節文字，跟語料裡
The Legend of Bob 的 src **逐句相同**（語料只是套了 parametrize 的佔位符）。
wiki 上兩種轉錄格式都收：

1. span 型（Ragni's / Lusuco's Secret Library）：
   <span style="color:#ff55ff">'''標題'''</span><br>
   <span style="color:gray">''第一句''<br>''第二句''</span>
2. 模板型（Aldwell Library）：
   * {{c|#aaa|''句子''}}

普查結果（2026-09）：121 個發現裡 25 頁有這兩種格式的轉錄，共 323 句；
其餘頁面 wiki 上沒有人轉錄，那部分還是只能靠遊戲內收集。

已知不收的第三種格式：Aldwell Library 等頁把發現點的 **NPC 對話**用任務頁的
格式（'''Aldwin:''' 開頭的列表）轉錄。那是聊天欄文字不是過場字幕，歸屬的
語料域不同（npc/misc），要收的話該走 fetch-quest-dialogue 那條路，不在這支。

為什麼寫進 raw/ 而不是直接進語料
--------------------------------
同 fetch-discoveries.py：raw/ 是暫存區。要不要進語料、怎麼對齊遊戲內的
斷行（語料 #000 是「標題+第一句」連在一起）是翻譯團隊的事。

用法
----
    python tools/fetch-secret-stories.py            抓取（有舊檔就比對）
    python tools/fetch-secret-stories.py --write    確定要覆蓋

來源：https://wynncraft.wiki.gg/wiki/Secret_Discoveries （CC BY-SA）
"""
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

RAW = Path(__file__).resolve().parent.parent / "raw"
DEST = RAW / "secret-stories.json"

INDEX = "Secret_Discoveries"
# redirects=1：見 fetch-discoveries.py，索引頁有幾個名稱是重新導向頁。
# 故事在任意章節，所以這裡抓**整頁**（fetch-discoveries 只抓 section 0）。
API = ("https://wynncraft.wiki.gg/api.php"
       "?action=parse&format=json&prop=wikitext&redirects=1&page=")
UA = ("WynnChaYuan-corpus/1.0 (zh_tw translation mod; "
      "+https://github.com/LyuChaCha/WynnChaYuan)")

# 對 wiki 客氣一點（同 fetch-discoveries.py）。
DELAY = 0.5

# 索引頁撇號是彎的（U+2019），條目頁是直的（同 fetch-discoveries.py）。
QUOTES = ("’", "'")

# 連結與 HTML 的剝法與 fetch-discoveries.py 的 lore() 一致。
LINK_PIPE = re.compile(r"\[\[([^\]|]+)\|([^\]]+)\]\]")
LINK_PLAIN = re.compile(r"\[\[([^\]]+)\]\]")
TAG = re.compile(r"<[^>]+>")
REF = re.compile(r"<ref>.*?</ref>", re.S)
SPAN = re.compile(r"<span[^>]*>(.*?)</span>", re.S)
CTPL = re.compile(r"\{\{c\|[^|]*\|(.*?)\}\}", re.S)
BR = re.compile(r"<br\s*/?>", re.I)


def fetch(page: str) -> tuple[str, str] | None:
    """回傳 (實際頁面標題, 整頁 wikitext)。查不到回傳 None。"""
    url = API + urllib.parse.quote(page.replace(" ", "_"))
    try:
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except (urllib.error.URLError, json.JSONDecodeError, TimeoutError) as e:
        print(f"    ! {page}: {e}", file=sys.stderr)
        return None
    if "error" in payload:
        return None
    parsed = payload["parse"]
    return parsed.get("title", page), parsed["wikitext"]["*"]


def wikitext(page: str) -> tuple[str, str] | None:
    """{@link fetch}，外加撇號的兩種寫法都試。見 {@link QUOTES}。"""
    got = fetch(page)
    if got is not None:
        return got
    for a, b in (QUOTES, QUOTES[::-1]):
        if a in page:
            got = fetch(page.replace(a, b))
            if got is not None:
                return got
    return None


def names(index: str) -> list[str]:
    """索引頁上那些 '''[[探索點名稱]]''' 的粗體連結（同 fetch-discoveries.py）。"""
    out = []
    for m in re.finditer(r"'''\[\[([^\]|]+?)(?:\|[^\]]*)?\]\]'''", index):
        name = m.group(1).strip()
        if name and name not in out:
            out.append(name)
    return out


def clean_line(text: str) -> str:
    """剝 wiki 標記成純文字。剝完含大括號的由呼叫方剔除（佔位符語法會撞）。"""
    s = REF.sub("", text)
    s = LINK_PIPE.sub(r"\2", s)
    s = LINK_PLAIN.sub(r"\1", s)
    s = TAG.sub("", s)
    s = re.sub(r"'''?", "", s)
    s = (s.replace("&quot;", '"').replace("&amp;", "&")
          .replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'"))
    return re.sub(r"\s+", " ", s).strip()


def is_story_line(s: str) -> bool:
    """太短的是標題殘片或裝飾；故事的句子不會只有幾個字。"""
    return len(s) >= 25 and any(c.isalpha() for c in s)


def extract(text: str) -> list[dict]:
    """從整頁 wikitext 抽故事。回傳 [{'title': str|None, 'lines': [...]}, ...]，
    一個 span 區塊或一段連續的 {{c}} 模板算一組。

    標題的認定：span 裡的第一段如果是粗體且沒有句號結尾（'Chapter I:' 這種），
    當標題；否則整段都是內文。語料裡 #000 是「標題+第一句」連體，這裡**刻意
    不黏**——斷行怎麼對齊遊戲是 import 階段的事，暫存區保留原貌比較好查。
    """
    blocks: list[dict] = []

    for m in SPAN.finditer(text):
        inner = m.group(1)
        # 標題段：'''...''' 粗體。可能跟內文在同一個 span 裡（用 <br> 隔開）。
        parts = [clean_line(p) for p in BR.split(inner)]
        parts = [p for p in parts if p]
        if not parts:
            continue
        title = None
        bold = re.match(r"^'''(.*?)'''$", m.group(1).split("<br")[0].strip())
        if bold and not re.search(r"[.!?]$", clean_line(bold.group(1))):
            title = parts.pop(0)
        lines = [p for p in parts if is_story_line(p)]
        if lines:
            blocks.append({"title": title, "lines": lines})

    # {{c}} 模板型：連續出現的列表項（'* {{c|...}}'），一段一段收。
    for section in re.split(r"^==+[^=].*?==+\s*$", text, flags=re.M):
        group: list[str] = []
        for line in section.splitlines():
            m = CTPL.search(line)
            if m:
                t = clean_line(m.group(1))
                if is_story_line(t):
                    group.append(t)
            elif group:
                break
        if len(group) >= 2:  # 單行的大多是提示訊息，不是故事
            blocks.append({"title": None, "lines": group})

    return blocks


def main() -> int:
    write = "--write" in sys.argv
    RAW.mkdir(exist_ok=True)

    print(f"索引：{INDEX}")
    got = wikitext(INDEX)
    if got is None:
        print("  索引頁抓不到，放棄", file=sys.stderr)
        return 1
    pages = names(got[1])
    print(f"  找到 {len(pages)} 個隱藏探索點\n")

    stories: dict[str, list[dict]] = {}
    for i, page in enumerate(pages, 1):
        page_got = wikitext(page)
        if page_got:
            blocks = extract(page_got[1])
            if blocks:
                stories[page] = blocks
        if i % 20 == 0 or i == len(pages):
            print(f"  {i}/{len(pages)}　已有 {len(stories)} 頁抓到故事")
        time.sleep(DELAY)

    # ★ 大括號絕對不能留下（同 fetch-discoveries.py：跟我們的佔位符語法撞）。
    dropped = 0
    for page in list(stories):
        for block in stories[page]:
            before = len(block["lines"])
            block["lines"] = [ln for ln in block["lines"]
                              if "{" not in ln and "}" not in ln]
            dropped += before - len(block["lines"])
        if not any(b["lines"] for b in stories[page]):
            del stories[page]
    if dropped:
        print(f"\n  ★ {dropped} 行仍含大括號，已剔除（wiki 模板沒剝乾淨）")

    total = sum(len(b["lines"]) for bs in stories.values() for b in bs)
    print(f"\n有故事的頁：{len(stories)} / {len(pages)}，共 {total} 句")
    for page, blocks in sorted(stories.items(),
                               key=lambda kv: -sum(len(b["lines"]) for b in kv[1])):
        n = sum(len(b["lines"]) for b in blocks)
        print(f"  {n:>4} 句  {page}")

    if DEST.exists() and not write:
        old = json.loads(DEST.read_text(encoding="utf-8"))
        old_pages = set(old.get("stories", {}))
        new_pages = set(stories)
        for p in sorted(new_pages - old_pages):
            print(f"  [新增] {p}")
        for p in sorted(old_pages - new_pages):
            print(f"  [消失] {p}")
        for p in sorted(old_pages & new_pages):
            if old["stories"][p] != stories[p]:
                print(f"  [有變] {p}")
        print("\n（比對模式。確定要覆蓋的話加 --write）")
        return 0

    payload = {
        "_meta": {
            "source": f"https://wynncraft.wiki.gg/wiki/{INDEX}",
            "license": "CC BY-SA",
            "note": "祕密發現的過場故事。暫存區——進語料前先對齊遊戲內斷行",
        },
        "stories": stories,
    }
    DEST.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n",
                    encoding="utf-8", newline="\n")
    print(f"\n寫進 {DEST}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
