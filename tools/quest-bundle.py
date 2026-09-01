#!/usr/bin/env python3
"""劇情對話：一個故事一個檔案，合起來給模組用。

為什麼要兩種形態
----------------
**翻譯的人要小檔案。** 兩萬多句擠在一個 JSON 裡，打開就卡、搜尋很慢，
兩個人同時改還會在同一個檔案裡打架。一個故事一個檔，接了哪個就開哪個。

**模組要大檔案。** 譯文是啟動時從 GitHub 一個一個抓下來的，157 個檔就是
157 次連線——那會讓每次進遊戲都慢上好幾秒，還容易被擋。

所以：`quest/`、`secret/` 底下是<b>來源</b>（人改的），
`quest-dialogue.json`、`secret-dialogue.json` 是<b>產生物</b>（程式讀的）。
改完跑一次這支就好。

為什麼祕密發現要跟任務分開
--------------------------
兩者走的是<b>同一條對話路徑</b>，所以機制共用；但它們在遊戲裡是不同的東西
——任務有進度、有獎勵、列在任務書裡，祕密發現只是散在地圖上的故事。
先前把「Bob 的傳說」擺進 `quest/`，翻譯的人打開任務資料夾就會看到一個
不是任務的東西，而任務進度表也會多算一筆。分成兩個資料夾，兩邊都乾淨。

用法
----
    python tools/quest-bundle.py            # 由來源資料夾產生兩個 bundle
    python tools/quest-bundle.py --split    # 反向：把現有的大檔拆成一個故事一個檔
    python tools/quest-bundle.py --check    # 只檢查有沒有忘記重新產生（CI 用）
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw"


class Kind:
    """一種劇情來源：一個資料夾配一個產生物。"""

    def __init__(self, folder: str, bundle: str, domain: str, what: str, unit: str):
        self.parts = BASE / folder
        self.bundle = BASE / bundle
        self.folder = folder
        self.domain = domain
        self.what = what
        self.unit = unit

    @property
    def note(self) -> str:
        return (f"{self.what}。這個檔案是<b>產生物</b>——請改 {self.folder}/ 底下"
                f"對應{self.unit}的檔案，再跑 tools/quest-bundle.py。"
                f"直接改這裡下次會被蓋掉。")

    @property
    def part_note(self) -> str:
        return f"一個{self.unit}一個檔。改完跑 tools/quest-bundle.py。"


KINDS = (
    Kind("quest", "quest-dialogue.json", "quest-dialogue", "任務對話", "任務"),
    Kind("secret", "secret-dialogue.json", "secret-dialogue", "祕密發現的對話", "故事"),
)


def slug(name: str) -> str:
    """名稱轉成安全的檔名。

    空白與符號在網址裡要跳脫，而譯文是用網址一個一個抓的——檔名裡留著空白，
    同步就會失敗，而且失敗得很難查。
    """
    s = re.sub(r"[^A-Za-z0-9]+", "-", name).strip("-")
    return (s or "unnamed").lower()


def split(kind: Kind) -> int:
    if not kind.bundle.exists():
        return 0
    data = json.loads(kind.bundle.read_text(encoding="utf-8"))
    stories: dict[str, dict] = {}
    for key, entry in data["entries"].items():
        stories.setdefault(entry["quest"], {})[key] = entry
    kind.parts.mkdir(parents=True, exist_ok=True)
    for name, entries in stories.items():
        payload = {
            "_meta": {"quest": name, "count": len(entries),
                      "translated": sum(1 for e in entries.values() if e.get("dst")),
                      "note": kind.part_note},
            "entries": entries,
        }
        (kind.parts / f"{slug(name)}.json").write_text(
            json.dumps(payload, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"拆成 {len(stories)} 個檔 -> {kind.parts.relative_to(ROOT)}/")
    return 0


def build(kind: Kind) -> dict:
    entries: dict[str, dict] = {}
    for path in sorted(kind.parts.glob("*.json")):
        entries.update(json.loads(path.read_text(encoding="utf-8"))["entries"])
    return {
        "_meta": {
            # 讓 validate.py 知道這是產生物。產生物參與「兩個檔案譯法不同」
            # 的檢查一定會誤報——它的內容<b>本來就</b>是別的檔案的副本。
            "generated": True,
            "domain": kind.domain,
            "itemNames": False,
            "lang": "zh_tw",
            "count": len(entries),
            "translated": sum(1 for e in entries.values() if e.get("dst")),
            "note": kind.note,
        },
        "entries": entries,
    }


def main(argv: list[str]) -> int:
    if "--split" in argv:
        for kind in KINDS:
            split(kind)
        return 0

    stale = 0
    for kind in KINDS:
        if not kind.parts.is_dir():
            print(f"找不到 {kind.parts.relative_to(ROOT)}/，跳過")
            continue
        payload = build(kind)
        text = json.dumps(payload, ensure_ascii=False, indent=1) + "\n"
        if "--check" in argv:
            current = kind.bundle.read_text(encoding="utf-8") if kind.bundle.exists() else ""
            if current == text:
                print(f"{kind.bundle.name} 是最新的。")
            else:
                print(f"{kind.bundle.name} 沒有跟著 {kind.folder}/ 更新"
                      f" —— 跑 python tools/quest-bundle.py")
                stale += 1
            continue
        kind.bundle.write_text(text, encoding="utf-8")
        print(f"{len(list(kind.parts.glob('*.json')))} 個檔、"
              f"{payload['_meta']['count']} 句 -> {kind.bundle.name}")
    return 1 if stale else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
