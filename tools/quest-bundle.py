#!/usr/bin/env python3
"""任務對話：一個任務一個檔案，合起來給模組用。

為什麼要兩種形態
----------------
**翻譯的人要小檔案。** 兩萬多句擠在一個 JSON 裡，打開就卡、搜尋很慢，
兩個人同時改還會在同一個檔案裡打架。一個任務一個檔，接了哪個就開哪個。

**模組要大檔案。** 譯文是啟動時從 GitHub 一個一個抓下來的，157 個檔就是
157 次連線——那會讓每次進遊戲都慢上好幾秒，還容易被擋。

所以：`quest/` 底下是<b>來源</b>（人改的），`quest-dialogue.json` 是
<b>產生物</b>（程式讀的）。改完跑一次這支就好。

用法
----
    python tools/quest-bundle.py            # 由 quest/ 產生 quest-dialogue.json
    python tools/quest-bundle.py --split    # 反向：把現有的大檔拆成一個任務一個檔
    python tools/quest-bundle.py --check    # 只檢查有沒有忘記重新產生（CI 用）
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "src/main/resources/assets/wynnchayuan/translations"
BUNDLE = BASE / "quest-dialogue.json"
PARTS = BASE / "quest"

NOTE = ("任務對話。這個檔案是<b>產生物</b>——請改 quest/ 底下對應任務的檔案，"
        "再跑 tools/quest-bundle.py。直接改這裡下次會被蓋掉。")


def slug(name: str) -> str:
    """任務名稱轉成安全的檔名。

    空白與符號在網址裡要跳脫，而譯文是用網址一個一個抓的——檔名裡留著空白，
    同步就會失敗，而且失敗得很難查。
    """
    s = re.sub(r"[^A-Za-z0-9]+", "-", name).strip("-")
    return (s or "unnamed").lower()


def split() -> int:
    data = json.loads(BUNDLE.read_text(encoding="utf-8"))
    quests: dict[str, dict] = {}
    for key, entry in data["entries"].items():
        quests.setdefault(entry["quest"], {})[key] = entry
    PARTS.mkdir(parents=True, exist_ok=True)
    for name, entries in quests.items():
        payload = {
            "_meta": {"quest": name, "count": len(entries),
                      "translated": sum(1 for e in entries.values() if e.get("dst")),
                      "note": "一個任務一個檔。改完跑 tools/quest-bundle.py。"},
            "entries": entries,
        }
        (PARTS / f"{slug(name)}.json").write_text(
            json.dumps(payload, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"拆成 {len(quests)} 個檔 -> {PARTS.relative_to(ROOT)}/")
    return 0


def build() -> dict:
    entries: dict[str, dict] = {}
    for path in sorted(PARTS.glob("*.json")):
        entries.update(json.loads(path.read_text(encoding="utf-8"))["entries"])
    return {
        "_meta": {
            # 讓 validate.py 知道這是產生物。產生物參與「兩個檔案譯法不同」
            # 的檢查一定會誤報——它的內容<b>本來就</b>是別的檔案的副本。
            "generated": True,
            "domain": "quest-dialogue",
            "itemNames": False,
            "lang": "zh_tw",
            "count": len(entries),
            "translated": sum(1 for e in entries.values() if e.get("dst")),
            "note": NOTE,
        },
        "entries": entries,
    }


def main(argv: list[str]) -> int:
    if "--split" in argv:
        return split()
    if not PARTS.is_dir():
        print(f"找不到 {PARTS.relative_to(ROOT)}/，先跑 --split")
        return 2

    payload = build()
    text = json.dumps(payload, ensure_ascii=False, indent=1) + "\n"
    if "--check" in argv:
        current = BUNDLE.read_text(encoding="utf-8") if BUNDLE.exists() else ""
        if current == text:
            print("quest-dialogue.json 是最新的。")
            return 0
        print("quest-dialogue.json 沒有跟著 quest/ 更新 —— 跑 python tools/quest-bundle.py")
        return 1
    BUNDLE.write_text(text, encoding="utf-8")
    print(f"{len(list(PARTS.glob('*.json')))} 個檔、{payload['_meta']['count']} 句 "
          f"-> {BUNDLE.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
