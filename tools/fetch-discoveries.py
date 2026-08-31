#!/usr/bin/env python3
"""從 Wynncraft wiki 抓隱藏探索點的敘述到 raw/discoveries.json。

為什麼是 wiki 而不是 CDN
------------------------
探索點的那段敘述**遊戲本身不會送**——是 Wynntils 自己去 wiki 抓來顯示的。
Wynntils 的 urls.json 裡就有這一條：

    "id": "apiWikiDiscoveryQuery",
    "url": "https://wynncraft.wiki.gg/api.php?action=parse&prop=wikitext&section=0&page=%{name}"

所以我們抓的跟 Wynntils 顯示的是**同一份**，不會有「抓來的跟畫面上的對不起來」
的問題——那是這類做法最常翻船的地方。

cdn.wynntils.com 沒有探索點的端點（試過 discoveries / discovery 等路徑全是 404），
wiki 的 Cargo 也只有 Ingredients / Mobs / NPCs / Quests，沒有 Discoveries。
wiki API 是唯一的路。

為什麼寫進 raw/ 而不是直接進語料
--------------------------------
raw/ 是暫存區（見 fetch.py）。要不要進語料、怎麼翻是翻譯團隊的事；121 條空譯文
直接塞進語料只會洗掉 validate.py 的訊號。

wiki 會變
---------
那邊有人改一個字，我們的鍵就對不上了。所以這支工具可以重跑：raw/ 已經有檔案時
會**比對並列出哪幾條變了**，而不是默默覆蓋。

用法
----
    python tools/fetch-discoveries.py            抓取（有舊檔就比對）
    python tools/fetch-discoveries.py --write    確定要覆蓋

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
DEST = RAW / "discoveries.json"

INDEX = "Secret_Discoveries"
API = ("https://wynncraft.wiki.gg/api.php"
       "?action=parse&format=json&prop=wikitext&section=0&page=")
UA = ("WynnChaYuan-corpus/1.0 (zh_tw translation mod; "
      "+https://github.com/LyuChaCha/WynnChaYuan)")

# 對 wiki 客氣一點。121 頁 × 0.5 秒約一分鐘，不值得為了快而被擋。
DELAY = 0.5


def wikitext(page: str, section0: bool = True) -> str | None:
    url = API + urllib.parse.quote(page.replace(" ", "_"))
    if not section0:
        url = url.replace("&section=0", "")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except (urllib.error.URLError, json.JSONDecodeError, TimeoutError) as e:
        print(f"    ! {page}: {e}", file=sys.stderr)
        return None
    if "error" in payload:
        return None
    return payload["parse"]["wikitext"]["*"]


def names(index: str) -> list[str]:
    """索引頁上那些 '''[[探索點名稱]]''' 的粗體連結。"""
    out = []
    for m in re.finditer(r"'''\[\[([^\]|]+?)(?:\|[^\]]*)?\]\]'''", index):
        name = m.group(1).strip()
        if name and name not in out:
            out.append(name)
    return out


def lore(text: str) -> str | None:
    """infobox 的 lore 欄，剝掉 wiki 標記。

    抓到下一個「| 欄位 =」或 infobox 結尾為止——lore 常常是多行的。
    """
    m = re.search(r"^\|\s*lore\s*=\s*(.*?)(?=^\s*\|\s*\w+\s*=|^\}\})",
                  text, re.M | re.S)
    if not m:
        return None
    s = m.group(1).strip()
    # wiki 模板。{{--}} 是破折號，其餘的一律丟掉。
    #
    # 這一步非做不可：大括號跟我們的佔位符語法（{#} {~} {p} {cN}）撞在一起，
    # 帶進語料會被當成佔位符解析——輕則譯文對不上，重則整串印到畫面上。
    s = s.replace("{{--}}", "—")
    s = re.sub(r"\{\{[^}]*\}\}", "", s)
    s = re.sub(r"\[\[([^\]|]+)\|([^\]]+)\]\]", r"\2", s)   # [[目標|顯示]] -> 顯示
    s = re.sub(r"\[\[([^\]]+)\]\]", r"\1", s)              # [[名稱]] -> 名稱
    s = re.sub(r"<ref>.*?</ref>", "", s, flags=re.S)
    s = re.sub(r"<[^>]+>", "", s)
    s = re.sub(r"'''?", "", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s or None


def main() -> int:
    write = "--write" in sys.argv
    RAW.mkdir(exist_ok=True)

    print(f"索引：{INDEX}")
    index = wikitext(INDEX, section0=False)
    if index is None:
        print("  索引頁抓不到，放棄", file=sys.stderr)
        return 1
    pages = names(index)
    print(f"  找到 {len(pages)} 個隱藏探索點\n")

    found: dict[str, str] = {}
    missing: list[str] = []
    for i, page in enumerate(pages, 1):
        text = wikitext(page)
        got = lore(text) if text else None
        if got:
            found[page] = got
        else:
            missing.append(page)
        if i % 20 == 0 or i == len(pages):
            print(f"  {i}/{len(pages)}　已取得 {len(found)} 條")
        time.sleep(DELAY)

    print(f"\n有敘述 {len(found)} 條，沒有 {len(missing)} 條")

    # ★ 大括號絕對不能留下。它跟佔位符語法（{#} {~} {p} {cN}）撞在一起，帶進語料
    #   會被當成佔位符解析——譯文對不上，甚至整串印到畫面上。剝不乾淨就寧可不收。
    braces = {k: v for k, v in found.items() if "{" in v or "}" in v}
    if braces:
        print(f"  ★ {len(braces)} 條仍含大括號，已剔除（wiki 模板沒剝乾淨）：")
        for k in list(braces)[:8]:
            print(f"      {k}")
        for k in braces:
            del found[k]
            missing.append(k)
    if missing:
        print("  沒有 lore 欄：" + "、".join(missing[:12])
              + ("…" if len(missing) > 12 else ""))

    # 已經有舊檔就先比對，不默默覆蓋——wiki 那邊改一個字，我們的鍵就對不上了。
    if DEST.exists():
        old = json.loads(DEST.read_text(encoding="utf-8")).get("entries", {})
        added = [k for k in found if k not in old]
        gone = [k for k in old if k not in found]
        changed = [k for k in found if k in old and old[k] != found[k]]
        print(f"\n跟 {DEST.name} 比對：新增 {len(added)}、"
              f"消失 {len(gone)}、內容改過 {len(changed)}")
        for k in changed[:10]:
            print(f"  ~ {k}")
            print(f"      舊：{old[k][:80]}")
            print(f"      新：{found[k][:80]}")
        if not write:
            print("\n沒有寫入。確定要覆蓋就加 --write")
            return 0

    payload = {
        "_meta": {
            "note": ("隱藏探索點的敘述。遊戲本身不送這段文字，是 Wynntils 去 "
                     "wiki 抓來顯示的，所以這裡抓的跟畫面上的是同一份。"
                     "這是暫存資料，不是語料——要不要進語料由翻譯團隊決定。"),
            "source": "https://wynncraft.wiki.gg/wiki/Secret_Discoveries",
            "licence": "CC BY-SA（wiki.gg）",
            "count": len(found),
            "missing": missing,
        },
        "entries": dict(sorted(found.items())),
    }
    DEST.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n",
                    encoding="utf-8")
    print(f"\n寫入 {DEST}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
