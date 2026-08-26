#!/usr/bin/env python3
"""從官方 CDN 匯入 Major ID，產生 major-id.json。

為什麼要有這支
--------------
Major ID 的名稱與說明散在每一件裝備的資料裡（161 種，5,390 件裝備）。
不必進遊戲、也不必翻 wiki，官方 CDN 就有全部。

名稱與說明分開存
----------------
遊戲裡的 Major ID 說明會<b>依 tooltip 寬度自動斷行</b>，斷在哪裡取決於玩家的
畫面設定。我們的比對是逐行進行的，所以整段說明多半對不上——但<b>名稱</b>
是獨立的一段，一定對得上。

所以名稱是這個檔真正會生效的部分；說明照樣收進來，一來對得上時就有用，
二來譯者翻名稱時看得到上下文才知道那個技能在做什麼。

用法:  python tools/fetch-majorids.py [--dry-run]
"""
from __future__ import annotations

import html
import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/wynnchayuan/translations/major-id.json"
INDEX = ROOT / "src/main/resources/assets/wynnchayuan/translations/_index.json"
URL = "https://cdn.wynntils.com/static/Reference/gear.json"

TAGS = re.compile(r"<[^>]+>")
# 遊戲端的規則（見 GlyphSplitter.NUMBERS）：正負號<b>不</b>算數字的一部分，
# 所以 `+2` 送過來是 `+{~}`。把號吃進佔位符的話，鍵就跟遊戲對不上——
# aspect 的說明整批（175 段裡的 156 段）就是這樣一聲不吭地沒在顯示。
NUMBER = re.compile(r"\d+(?:[.,]\d+)*%?")
# 私用區與 Wynncraft 的排版平面。與 GlyphSplitter.isGlyphCodePoint 同一套範圍。
GLYPH_RUN = re.compile(
    "[%s-%s%s-%s%s-%s]+"
    % (chr(0xE000), chr(0xF8FF), chr(0xCF000), chr(0xD1000),
       chr(0xF0000), chr(0x10FFFF))
)


def clean(text: str) -> str:
    """把 CDN 的 HTML 標記清掉，換成遊戲端<b>查表時用的模板</b>。

    <h2>為什麼不能只剝標籤</h2>
    元素圖示是標籤的<b>內文</b>，不是標籤本身：

        <span class='font-common'>U+E005</span>100%

    剝掉 <span> 之後 U+E005 原封不動留著。但遊戲端送過來的模板是把每一段
    符號換成 {#}、每個數值換成 {~} 之後才拿去查表的，所以留著原始碼位的鍵
    <b>永遠對不上</b>——而且完全不會報錯，畫面上只是「這條 Major ID 沒翻」。
    `Cherry Bombs` 的四個元素傷害就是這樣整條掉掉的。
    """
    plain = re.sub(r"\s+", " ", html.unescape(TAGS.sub("", text))).strip()
    return NUMBER.sub("{~}", GLYPH_RUN.sub("{#}", plain))


def fetch() -> dict[str, str]:
    req = urllib.request.Request(URL, headers={"User-Agent": "WynnChaYuan"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        data = json.loads(resp.read())

    items = data if isinstance(data, list) else data.get("items", data)
    if isinstance(items, dict):
        items = list(items.values())

    found: dict[str, str] = {}
    for item in items:
        major = item.get("majorIds") or {}
        if isinstance(major, dict):
            for name, desc in major.items():
                found.setdefault(name.strip(), clean(str(desc)))
    return dict(sorted(found.items()))


def main(argv: list[str]) -> int:
    major = fetch()
    print(f"CDN 上共 {len(major)} 種 Major ID")

    old = {}
    if OUT.is_file():
        prev = json.loads(OUT.read_text(encoding="utf-8"))
        old = {k: v.get("dst", "") for k, v in prev.get("entries", {}).items()}

    entries = {}
    for name, desc in major.items():
        entries[name] = {"src": name, "dst": old.get(name, ""), "role": "name"}
        key = f"{name}::desc"
        entries[key] = {"src": desc, "dst": old.get(key, ""), "role": "desc"}

    payload = {
        "_meta": {
            "domain": "major-id",
            "lang": "zh_tw",
            "count": len(entries),
            "note": "Major ID。名稱一定對得上；說明會依畫面寬度自動斷行，"
                    "整段多半對不上，但翻名稱時看得到上下文。"
                    "由 tools/fetch-majorids.py 從官方 CDN 產生。",
        },
        "entries": entries,
    }

    if "--dry-run" in argv:
        for name in list(major)[:5]:
            print(f"    {name}: {major[name][:70]}")
        print("\n(--dry-run，沒有寫入)")
        return 0

    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n",
                   encoding="utf-8")
    print(f"已寫入 {OUT.name}：{len(entries)} 條（{len(major)} 名稱 + {len(major)} 說明）")

    index = json.loads(INDEX.read_text(encoding="utf-8"))
    if OUT.name not in index["files"]:
        # 排在裝備前面：Major ID 比裝備敘述常看到
        at = index["files"].index("ability.json") if "ability.json" in index["files"] \
            else len(index["files"])
        index["files"].insert(at, OUT.name)
        INDEX.write_text(json.dumps(index, ensure_ascii=False, indent=1) + "\n",
                         encoding="utf-8")
        print(f"已加入 _index.json")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
