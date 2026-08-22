"""把翻譯進度寫回文件。

進度數字寫死在 `README.md` 與 `CONTRIBUTING.md` 裡，翻了幾千條之後那些數字
就變成謊話——新來的譯者照著「從哪裡開始翻」的表去翻，翻到的卻是早就翻完的檔案。

所以改成自動產生。內容夾在標記之間，重跑就整段換掉：

    <!-- 進度:開始 -->
    ...自動產生...
    <!-- 進度:結束 -->

用法：
    python tools/update-docs.py
    python tools/update-docs.py --check    # CI 用：過期就非零退出
"""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

TRANSLATIONS = Path("src/main/resources/assets/wynnchayuan/translations")

START = "<!-- 進度:開始 -->"
END = "<!-- 進度:結束 -->"

# 玩家看到的頻率。數字是自動算的，這裡只排順序與說明。
ORDER = [
    ("ui-labels.json", "每次看物品都會看到（力量、戰鬥等級…）"),
    ("npc.json", "走在城裡就會看到"),
    ("gui.json", "選單與介面"),
    ("quest.json", "任務名稱與任務介面"),
    ("ability/*.json", "開技能樹時"),
    ("major-id.json", "傳奇裝備的特殊詞條"),
    ("quest-dialogue.json", "任務對話"),
    ("ingredient.json", "做職業時"),
    ("material.json", "做職業時"),
    ("tome.json", "書卷"),
    ("aspect.json", "Raid 的 Aspect"),
    ("gear-*.json", "裝備的傳說敘述"),
]


def count(paths: list[Path]) -> tuple[int, int]:
    done = total = 0
    for path in paths:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        rows = data.get("entries")
        if isinstance(rows, dict):
            for value in rows.values():
                if not isinstance(value, dict) or value.get("keep"):
                    continue
                total += 1
                if value.get("dst", "").strip():
                    done += 1
        else:
            for key, value in data.items():
                if key.startswith("_") or not isinstance(value, str):
                    continue
                total += 1
                if value.strip():
                    done += 1
    return done, total


def bar(done: int, total: int) -> str:
    if total == 0:
        return "—"
    filled = round(done / total * 10)
    return "█" * filled + "░" * (10 - filled)


def table() -> tuple[str, int, int]:
    lines = ["| 檔案 | 進度 | 已翻 / 總數 | 什麼時候會看到 |",
             "|---|---|---:|---|"]
    grand_done = grand_total = 0
    for pattern, when in ORDER:
        paths = sorted(TRANSLATIONS.glob(pattern))
        if not paths:
            continue
        done, total = count(paths)
        if total == 0:
            continue
        grand_done += done
        grand_total += total
        lines.append(f"| `{pattern}` | {bar(done, total)} {done / total:.0%} "
                     f"| {done:,} / {total:,} | {when} |")
    percent = grand_done / grand_total if grand_total else 0
    lines.append(f"| **合計** | {bar(grand_done, grand_total)} **{percent:.1%}** "
                 f"| **{grand_done:,} / {grand_total:,}** | |")
    return "\n".join(lines), grand_done, grand_total


def replace(path: Path, body: str) -> bool:
    text = path.read_text(encoding="utf-8")
    if START not in text or END not in text:
        print(f"  ! {path.name} 少了 {START} / {END} 標記，跳過")
        return False
    head, rest = text.split(START, 1)
    _, tail = rest.split(END, 1)
    fresh = f"{head}{START}\n{body}\n{END}{tail}"
    if fresh == text:
        return False
    path.write_text(fresh, encoding="utf-8", newline="\n")
    return True


def main(argv: list[str]) -> int:
    check = "--check" in argv
    body, done, total = table()
    stamp = subprocess.run(["git", "log", "-1", "--format=%ad", "--date=short"],
                           capture_output=True, text=True).stdout.strip()
    body = (f"**目前進度 {done:,} / {total:,}"
            f"（{done / total:.1%}）**，更新於 {stamp}。\n\n{body}")
    stale = []
    for name in ("README.md", "CONTRIBUTING.md", "docs/modrinth-description.md"):
        path = Path(name)
        if path.is_file() and replace(path, body):
            stale.append(name)
    if check:
        if stale:
            print("進度表過期：" + "、".join(stale))
            print("跑 python tools/update-docs.py 更新")
            return 1
        print("進度表是新的")
        return 0
    print(f"進度 {done:,} / {total:,}（{done / total:.1%}）")
    print("更新了：" + ("、".join(stale) if stale else "（沒有變化）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
