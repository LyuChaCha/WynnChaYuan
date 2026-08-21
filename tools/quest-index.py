#!/usr/bin/env python3
"""產生任務對話的認領清單。

為什麼需要
----------
3,494 句放在同一個檔案裡，翻譯的人打開只會看到一片茫茫的 JSON——
不知道有哪些任務、哪個大哪個小、哪些已經有人動過。

這份清單把它攤成一張表：每個任務幾句、翻了多少、誰在翻。
要認領的人先看這裡，就不會兩個人撞在同一個任務上。

用法
----
    python tools/quest-index.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/resources/assets/wynnchayuan/translations/quest-dialogue.json"
OUT = ROOT / "docs/quest-dialogue.md"

# 一個人一次接得完的量。超過這個就建議先講一聲，免得卡著別人。
SMALL = 20


def main() -> int:
    if not SRC.is_file():
        print("找不到 quest-dialogue.json，先跑 tools/fetch-quest-dialogue.py")
        return 2

    entries = json.loads(SRC.read_text(encoding="utf-8"))["entries"]
    quests: dict[str, dict] = {}
    for entry in entries.values():
        q = quests.setdefault(entry["quest"], {
            "lines": 0, "done": 0, "dialogue": 0, "speakers": set(), "stages": set()})
        q["lines"] += 1
        q["done"] += 1 if entry.get("dst", "").strip() else 0
        q["dialogue"] += 1 if entry.get("kind") == "dialogue" else 0
        if entry.get("speaker"):
            q["speakers"].add(entry["speaker"])
        if entry.get("stage"):
            q["stages"].add(entry["stage"])

    total = sum(q["lines"] for q in quests.values())
    done = sum(q["done"] for q in quests.values())
    untouched = [n for n, q in quests.items() if q["done"] == 0]
    starters = sorted((q["lines"], n) for n, q in quests.items()
                      if q["done"] == 0 and q["lines"] <= SMALL)

    out = [
        "# 任務對話認領清單",
        "",
        f"**{done:,} / {total:,} 句已翻（{done * 100 / total:.1f}%），"
        f"共 {len(quests)} 個任務，還沒有人動的有 {len(untouched)} 個。**",
        "",
        "資料在 [`quest-dialogue.json`](../src/main/resources/assets/wynnchayuan/"
        "translations/quest-dialogue.json)，鍵是「任務#序號」，"
        "所以同一個任務的句子都排在一起，從上往下翻就好。",
        "",
        "每一條長這樣——`quest`、`stage`、`speaker` 是給你看上下文用的，**不用翻**：",
        "",
        "```json",
        '"Cook Assistant#004": {',
        '  "src": "Unfortunately, a Grook took my last cake, and I ran out of ingredients!",',
        '  "dst": "",',
        '  "quest": "Cook Assistant",',
        '  "stage": "Stage 1",',
        '  "speaker": "The Cook"',
        "}",
        "```",
        "",
        "> 對話取自 [Wynncraft wiki](https://wynncraft.wiki.gg/)，跟遊戲裡"
        "**不保證逐字相同**。翻的時候發現不一樣，以遊戲裡的為準，直接改 `src`。",
        "",
        "---",
        "",
        "## 認領方式",
        "",
        "**動手前先講一聲**（[開一則 issue](https://github.com/LyuChaCha/"
        "WynnChaYuan/issues) 或直接找 LyuChaCha），說你要接哪個任務。",
        "兩個人同時翻同一個任務，合併時會互相覆蓋。",
        "",
    ]

    if starters:
        out += [
            f"## 適合第一次接的（{SMALL} 句以內、還沒有人動）",
            "",
            "| 任務 | 句數 |",
            "|---|---|",
        ]
        out += [f"| {name} | {n} |" for n, name in starters[:15]]
        out += ["", "---", ""]

    out += [
        "## 全部任務",
        "",
        "「台詞」是 NPC 講的話，其餘是任務目標（顯示在追蹤器上）。",
        "",
        "| 任務 | 句數 | 台詞 | 角色 | 階段 | 進度 |",
        "|---|---|---|---|---|---|",
    ]
    for name in sorted(quests):
        q = quests[name]
        pct = q["done"] * 100 / q["lines"]
        mark = "✅" if pct >= 100 else (f"{pct:.0f}%" if q["done"] else "—")
        out.append(f"| {name} | {q['lines']} | {q['dialogue']} | "
                   f"{len(q['speakers'])} | {len(q['stages'])} | {mark} |")

    out += ["", "---", "",
            "這份清單由 `tools/quest-index.py` 產生，翻譯有進度就重跑一次。"]

    OUT.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"{len(quests)} 個任務、{total} 句 -> {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
