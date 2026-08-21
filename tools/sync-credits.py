#!/usr/bin/env python3
"""把 README 的名單同步成 credits.json 的內容。

為什麼需要
----------
同一份名單有兩個地方要改：遊戲內的 credits.json，以及 README。手動維護兩份
的結果就是它們會分岔——實際發生過：遊戲裡加了人、換了人，README 還停在
好幾版之前，看的人以為自己沒被收錄。

一份資料、一個來源。credits.json 是真的那一份（遊戲讀它），README 由它產生。

用法
----
    python tools/sync-credits.py            # 改寫 README
    python tools/sync-credits.py --check    # 只檢查，不一致回傳 1（給 CI 用）
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CREDITS = ROOT / "src/main/resources/assets/wynnchayuan/credits.json"
README = ROOT / "README.md"

BEGIN = "<!-- credits:begin -->"
END = "<!-- credits:end -->"


def render() -> str:
    data = json.loads(CREDITS.read_text(encoding="utf-8"))
    lines = [BEGIN, "",
             "<!-- 這一段由 tools/sync-credits.py 從 credits.json 產生，不要手動改。 -->"]
    for section in data.get("sections", []):
        members = section.get("members", [])
        if not members:
            continue
        lines += ["", f"### {section['role']}", "",
                  "| 名稱 | Minecraft ID |", "|---|---|"]
        for m in members:
            mc = m.get("mc", "")
            lines.append(f"| {m['name']} | " + (f"`{mc}` |" if mc else "— |"))
    lines += ["", "翻一條就會出現在這裡。見 [CONTRIBUTING.md](CONTRIBUTING.md)。", "", END]
    return "\n".join(lines)


def main(argv: list[str]) -> int:
    text = README.read_text(encoding="utf-8")
    if BEGIN not in text or END not in text:
        print(f"README 裡找不到 {BEGIN} / {END} 標記，先手動放一次。")
        return 2

    head, rest = text.split(BEGIN, 1)
    _, tail = rest.split(END, 1)
    updated = head + render() + tail

    if updated == text:
        print("README 的名單已經是最新的。")
        return 0
    if "--check" in argv:
        print("README 的名單與 credits.json 不一致 —— 跑 python tools/sync-credits.py")
        return 1
    README.write_text(updated, encoding="utf-8", newline="\n")
    print("已更新 README 的名單。")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
