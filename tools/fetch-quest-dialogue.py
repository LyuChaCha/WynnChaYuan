#!/usr/bin/env python3
"""從 Wynncraft wiki 抓任務對話。

為什麼從 wiki 抓
----------------
遊戲內收集要有人真的把 158 個任務全部跑一遍，而且對話一句一句進來，
收到的是一大堆看不出關聯的句子——誰說的、屬於哪個任務、順序如何，
全都得靠猜。

wiki 的任務頁把對話寫成結構化模板：

    {{Dialogue|green|The Cook|I can't believe what is happening to me.}}

顏色、**說話的人**、台詞，三樣都在，而且照階段排好。這正是譯者需要的上下文。

限制（重要）
------------
wiki 是人寫的，跟遊戲裡的字串<b>不保證逐字相同</b>——標點、大小寫、
甚至漏字都可能有。對不上的話遊戲裡就查不到。

所以這份資料的定位是「**先把量做起來、讓譯者看得到上下文**」，
遊戲內收集仍然是權威來源：實際跑過的句子會蓋掉這裡的版本。
每一條都標了 `source: "wiki"`，之後要比對或清理找得到。

用法
----
    python tools/fetch-quest-dialogue.py               # 全部
    python tools/fetch-quest-dialogue.py --limit 5     # 先試幾個
"""

from __future__ import annotations

import argparse
import html
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build import parametrize                       # noqa: E402  同一套規則

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw/quest-dialogue.json"
API = "https://wynncraft.wiki.gg/api.php"
UA = "WynnChaYuan/1.0 (https://github.com/LyuChaCha/WynnChaYuan)"

# {{Dialogue|顏色|說話的人|台詞}}。台詞裡可能還有巢狀的 {{...}}，所以不能用貪婪比對。
DIALOGUE = re.compile(r"\{\{Dialogue\|([^|}]*)\|([^|}]*)\|(.+?)\}\}(?=\s*$|\s*\n)", re.S)
# 另一種寫法：* '''說話的人:''' 台詞
#
# wiki 上兩種格式並存，而且一頁只用一種。只認 {{Dialogue}} 的話，用這種寫法的
# 頁面會一句台詞都抓不到——而那是一半以上的頁面。
BOLD_LINE = re.compile(
    r"^\*+\s*'''\s*([^':]{1,40}?)\s*:?\s*'''\s*:?\s*(.+?)\s*$", re.M)

# » 開頭的是任務目標，遊戲裡顯示在追蹤器上
OBJECTIVE = re.compile(r"^»\s*(.+?)\s*$", re.M)
STAGE = re.compile(r"^==+ *(Stage [^=]+?) *=+$", re.M)


def get(params: dict) -> dict:
    url = API + "?" + urllib.parse.urlencode({**params, "format": "json",
                                              "formatversion": "2"})
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.loads(r.read().decode("utf-8"))


def quest_pages() -> list[str]:
    url = ("https://wynncraft.wiki.gg/index.php?title=Special:CargoExport"
           "&tables=Quests&fields=_pageName&limit=500&format=json")
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        rows = json.loads(r.read().decode("utf-8"))
    seen, out = set(), []
    for row in rows:
        name = html.unescape(row.get("_pageName", "")).strip()
        if name and name != "???" and name not in seen:
            seen.add(name)
            out.append(name)
    return out


def clean(text: str) -> str:
    """把 wiki 標記剝成純文字。

    只處理實際出現過的幾種——剝得太積極會把台詞本身吃掉。
    """
    # {{c|顏色|字}} 要在連結之前拆掉。顯示文字裡包著顏色模板的寫法確實存在
    # （`[[Stable Key|{{c|...|[1 Stable Key]}}]]`），先拆顏色，下面的連結規則
    # 才看得到真正的顯示文字。
    text = re.sub(r"\{\{c\|[^|}]*\|([^}]*)\}\}", r"\1", text)    # {{c|顏色|字}}
    # [[頁面|顯示]]。顯示文字裡可能有一層方括號（道具寫成 `[1 Stable Key]`），
    # 所以不能用 [^\]]* ——那會停在第一個 ] ，整條連結原封不動留在台詞裡。
    text = re.sub(r"\[\[([^|\[\]]*)\|((?:[^\[\]]|\[[^\[\]]*\])*)\]\]", r"\2", text)
    text = re.sub(r"\[\[([^\]]*)\]\]", r"\1", text)              # [[頁面]]
    # {{element|f|Fire|}}、{{e|e|Strength|}}：模板畫的是圖示，但<b>名字是台詞的一部分</b>。
    # 先前被下面那條「其餘模板」連名字一起丟掉，於是變成
    # 「The elements are... , , , and... ?」——那幾行永遠對不上遊戲裡的文字。
    text = re.sub(r"\{\{(?:element|e)\|[^|}]*\|([^|}]*)\|?\}\}", r"\1", text)
    # {{MapLink|x=477|y=70|z=-1623}} 與 {{RenderLocation| x = ..| y = ..| z = ..}}：
    # 座標<b>是台詞的一部分</b>。遊戲裡的目標行長成
    # 「Talk to Sayleros at [477, 70, -1623].」，被下面那條「其餘模板」整個丟掉之後
    # 就變成「Talk to Sayleros at .」——那種 key 永遠比對不到，翻了也不會顯示。
    # 還原成方括號形式，交給 parametrize 抽成 [{~}, {~}, -{~}]。
    #
    # 兩種模板的參數寫法不一致（等號兩邊有沒有空白、RenderLocation 前面還可能多一個
    # location=），所以中間用 [^}]*? 跳過，只認 x/y/z 三個值。
    text = re.sub(
        r"\{\{\s*(?:[Mm]ap[Ll]ink|[Rr]ender[Ll]ocation)\s*\|[^}]*?"
        r"\bx\s*=\s*(-?\d+)\s*\|[^}]*?"
        r"\by\s*=\s*(-?\d+)\s*\|[^}]*?"
        r"\bz\s*=\s*(-?\d+)\s*\}\}",
        r"[\1, \2, \3]", text)
    text = re.sub(r"\{\{[^}]*\}\}", "", text)                    # 其餘模板
    text = re.sub(r"'''?([^']*)'''?", r"\1", text)               # 粗體斜體
    text = re.sub(r"<[^>]+>", "", text)                          # html
    text = text.replace("<br>", " ").replace("&nbsp;", " ")
    text = re.sub(r"'{2,}", "", text)                            # 落單的粗體標記
    return html.unescape(re.sub(r"\s+", " ", text)).strip()


def parse(page: str, wikitext: str) -> list[dict]:
    """一頁抓出來的所有台詞與目標，照原文順序。"""
    rows: list[dict] = []
    stage = ""
    # 依出現位置把階段標題與內容交錯處理，順序才是對的
    marks = [(m.start(), "stage", m.group(1)) for m in STAGE.finditer(wikitext)]
    marks += [(m.start(), "line", (m.group(2), m.group(3)))
              for m in DIALOGUE.finditer(wikitext)]
    marks += [(m.start(), "line", (m.group(1), m.group(2)))
              for m in BOLD_LINE.finditer(wikitext)]
    marks += [(m.start(), "goal", m.group(1)) for m in OBJECTIVE.finditer(wikitext)]
    for _, kind, value in sorted(marks):
        if kind == "stage":
            stage = value
            continue
        if kind == "goal":
            speaker, text = "", clean(value)
            role = "objective"
        else:
            speaker, text = clean(value[0]), clean(value[1])
            role = "dialogue"
        if not text or len(text) < 2:
            continue
        # 數字與地名要抽成佔位符，才對得上遊戲裡的模板
        # （座標「[-1096, 42, -5384]」每個玩家看到的都一樣，但抽掉之後
        #  同一句話只要翻一次）。規則直接沿用 build.py，兩邊不能有差異。
        text = parametrize(text)
        rows.append({"src": text, "dst": "", "role": "desc", "kind": role,
                     "quest": page, "stage": stage, "speaker": speaker,
                     "source": "wiki"})
    return rows


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0, help="只抓前幾個任務")
    ap.add_argument("--delay", type=float, default=0.4, help="每次請求間隔（秒）")
    args = ap.parse_args(argv)

    pages = quest_pages()
    if args.limit:
        pages = pages[:args.limit]
    print(f"任務頁 {len(pages)} 個")

    existing = {}
    if OUT.exists():
        old = json.loads(OUT.read_text(encoding="utf-8"))
        existing = {k: v.get("dst", "") for k, v in old.get("entries", {}).items()}

    entries: dict[str, dict] = {}
    for i, page in enumerate(pages, 1):
        try:
            data = get({"action": "parse", "page": page, "prop": "wikitext"})
            wikitext = data.get("parse", {}).get("wikitext", "")
        except Exception as e:
            print(f"  ! {page}：{e}")
            continue
        rows = parse(page, wikitext)
        for order, row in enumerate(rows):
            key = f"{page}#{order:03d}"
            row["dst"] = existing.get(key, "")      # 已經翻好的留著
            entries[key] = row
        print(f"  [{i}/{len(pages)}] {page}：{len(rows)} 句")
        time.sleep(args.delay)

    payload = {
        "_meta": {
            "domain": "quest-dialogue",
            "itemNames": False,
            "lang": "zh_tw",
            "count": len(entries),
            "translated": sum(1 for e in entries.values() if e["dst"]),
            "note": ("任務對話，取自 Wynncraft wiki。鍵是「任務#序號」，"
                     "所以同一段劇情會排在一起。wiki 是人寫的，跟遊戲裡的字串"
                     "不保證逐字相同——對不上的以遊戲內收集為準。"),
        },
        "entries": entries,
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n",
                   encoding="utf-8")
    print(f"\n共 {len(entries)} 句 -> {OUT.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
