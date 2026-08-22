"""給技能樹的每一條加上代號與技能名稱，並照遊戲版面排序。

翻譯者打開 `ability/mage.json` 看到的是一串雜湊當鍵，根本不知道
「{#}{#}Range: {~} Blocks」是哪個技能的說明。這個工具補上兩個欄位：

* `ability` —— 這一條屬於哪個技能。跨技能共用的會全部列出來。
* `code`    —— 遊戲版面上的位置，例如 `MAGE-P1-R00-C04`
               （法師，第 1 頁，第 0 列，第 4 欄）。

排序照 `tree`（職業 → 頁 → 列 → 欄），也就是遊戲裡由上而下、由左而右；
同一個技能的名稱、說明、數值會排在一起。

用法：
    python tools/tag-abilities.py
    python tools/tag-abilities.py --write
"""

from __future__ import annotations

import collections
import json
import sys
from pathlib import Path

ABILITIES = Path("src/main/resources/assets/wynnchayuan/translations/ability")

# name 在前、desc 次之、stat 最後——跟遊戲裡一格 tooltip 的排法一致
ROLE_ORDER = {"name": 0, "desc": 1, "stat": 2}


def code_of(tree: str, fallback: str) -> str:
    """`01/mage/01/000/004` -> `MAGE-P1-R00-C04`"""
    parts = tree.split("/") if tree else []
    if len(parts) < 5:
        return fallback.upper()
    _, klass, page, row, col = parts[:5]
    return f"{klass.upper()}-P{int(page)}-R{int(row):02d}-C{int(col):02d}"


def abilities_of(entry: dict) -> str:
    names = []
    for ctx in entry.get("ctx") or []:
        name = ctx.split("/", 1)[-1]
        if name and name != "技能名稱" and name not in names:
            names.append(name)
    if not names and entry.get("role") == "name":
        names = [entry.get("src", "")]
    return "、".join(names)


def main(argv: list[str]) -> int:
    write = "--write" in argv
    total = 0
    for path in sorted(ABILITIES.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        entries = data["entries"]
        for entry in entries.values():
            entry["ability"] = abilities_of(entry)
            entry["code"] = code_of(entry.get("tree", ""), path.stem)
            total += 1

        def sort_key(item):
            key, value = item
            tree = value.get("tree", "")
            return (tree, ROLE_ORDER.get(value.get("role"), 9), value.get("src", ""))

        data["entries"] = collections.OrderedDict(
            sorted(entries.items(), key=sort_key))
        data["_meta"]["legend"] = ("ability = 這一條屬於哪個技能（共用的會列出全部）；"
                                   "code = 遊戲版面上的位置，P 頁 R 列 C 欄。"
                                   "排序即遊戲裡由上而下、由左而右。")
        if write:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                            encoding="utf-8", newline="\n")
        first = next(iter(data["entries"].values()))
        print(f"  {path.name:14} {len(entries):4} 條   例：{first['code']}  {first['ability']}")

    print()
    print(f"{total} 條加上代號與技能名稱" + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
