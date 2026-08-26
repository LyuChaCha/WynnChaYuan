#!/usr/bin/env python3
"""把語料檔重新排序，讓同一類的條目排在一起。

為什麼需要
----------
條目是用<b>雜湊</b>當鍵的，所以檔案裡的順序等於隨機。技能說明尤其明顯：
五個職業的技能全部混在一起，譯者翻到一半根本不知道自己在翻哪一棵技能樹，
也沒辦法「把一棵樹翻完」。

排序依據是每筆自己帶的 `ctx`（例如 `mage/Judrajim`），那本來就記著它從哪來。
`dst` 完全不動——這隻程式只搬動順序。

用法:  python tools/sort-corpus.py [檔名...]     # 不給檔名就處理全部
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw"


def ctx_key(entry: dict) -> tuple:
    """排序鍵：先分類，再看角色，最後照原文。

    `ctx` 可能是字串或字串陣列（一條說明會被好幾個技能共用），
    取<b>字典序最小</b>的那個當代表——同一條共用的說明才會固定落在同一處，
    不會因為來源順序不同而跳來跳去。
    """
    ctx = entry.get("ctx") or ""
    if isinstance(ctx, list):
        ctx = min(ctx) if ctx else ""
    # name 排在 desc 前面：先看到技能叫什麼，再看它的說明
    role_order = {"name": 0, "stat": 1, "desc": 2}
    return (str(ctx), role_order.get(entry.get("role", ""), 9), entry.get("src", ""))


def sort_file(path: Path) -> bool:
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = data.get("entries")
    if not isinstance(entries, dict):
        return False        # 扁平格式的檔案本來就是人工排的，不要動

    order = sorted(entries.items(), key=lambda kv: ctx_key(kv[1]))
    if [k for k, _ in order] == list(entries):
        return False        # 已經是排好的

    data["entries"] = dict(order)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                    encoding="utf-8")
    return True


def main(argv: list[str]) -> int:
    files = ([TRANSLATIONS / a for a in argv] if argv
             else sorted(f for f in TRANSLATIONS.glob("*.json")
                         if not f.name.startswith("_")))
    changed = 0
    for f in files:
        if not f.is_file():
            print(f"找不到 {f.name}")
            continue
        if sort_file(f):
            print(f"  已重排 {f.name}")
            changed += 1
    print(f"\n{changed} 個檔案重新排序（dst 未變動）")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
