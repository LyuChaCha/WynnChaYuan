#!/usr/bin/env python3
"""從 Wynncraft wiki 抓任務清單，補進 quest.json。

為什麼需要
----------
常有人問：「要翻譯任務，是不是得有人把每個任務都跑一次？」

**任務名稱不用。** wiki 有 Cargo 資料表，一次就能拿到全部（目前約 158 個），
不必進遊戲。這支程式做的就是這件事。

**任務對話要。** 對話文字沒有任何公開資料來源——不在 Wynncraft API，
也不在 Wynntils 的 CDN。只能靠玩家在遊戲裡實際遇到時由模組自動收集
（見 CONTRIBUTING.md 的「幫忙收集沒被翻譯的句子」）。

好消息是那是<b>累積</b>的：每個人玩自己的進度，收到的句子併起來就是全部，
沒有人需要一個人跑完 158 個任務。

用法:  python tools/fetch-quests.py [--dry-run]
"""
from __future__ import annotations

import html
import json
import sys
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
QUEST_FILE = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw/quest.json"

# wiki 的 Cargo 匯出。欄位取自 Wynntils 自己在用的同一張表。
API = "https://wynncraft.wiki.gg/index.php"
PARAMS = {
    "title": "Special:CargoExport",
    "tables": "Quests",
    "fields": "Quests._pageTitle,Quests.name",
    "limit": "2000",
    "format": "json",
}


def fetch() -> list[str]:
    url = API + "?" + urllib.parse.urlencode(PARAMS)
    req = urllib.request.Request(url, headers={
        "User-Agent": "WynnChaYuan translation project (github.com/LyuChaCha/WynnChaYuan)"
    })
    with urllib.request.urlopen(req, timeout=60) as resp:
        rows = json.loads(resp.read())

    names = []
    for row in rows:
        name = row.get("name") or row.get("_pageTitle") or ""
        name = html.unescape(name).strip()
        # 「???」是 wiki 上還沒公開名稱的佔位，不是真的任務名
        if name and name != "???":
            names.append(name)
    return sorted(set(names))


def main(argv: list[str]) -> int:
    dry = "--dry-run" in argv
    names = fetch()
    print(f"wiki 上共 {len(names)} 個任務")

    data = json.loads(QUEST_FILE.read_text(encoding="utf-8"))
    added = [n for n in names if n not in data]

    print(f"其中 {len(added)} 個還不在 quest.json")
    for n in added[:10]:
        print(f"    {n}")
    if len(added) > 10:
        print(f"    …還有 {len(added) - 10} 個")

    if dry:
        print("\n(--dry-run，沒有寫入)")
        return 0
    if not added:
        return 0

    # dst 留空代表「還沒翻」，模組會顯示原文，不會出錯
    for n in added:
        data[n] = ""
    QUEST_FILE.write_text(
        json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"\n已寫入 {len(added)} 個待翻的任務名稱")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
