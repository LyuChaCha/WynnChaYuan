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

TRANSLATIONS = Path("src/main/resources/assets/wynnchayuan/translations/zh_tw")

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
    ("gear-*.json", "裝備的傳說敘述（名稱是專有名詞，不計入）"),
]


def optional(path: Path, entry: dict) -> bool:
    """這一條算不算「該翻而還沒翻」。

    裝備與武器的<b>名稱</b>不算。那些是專有名詞，翻了反而對不上 wiki、
    交易市場與社群討論——模組本身的 `translateItemNames` 預設就是關的，
    也就是說即使翻了，玩家預設也看不到。

    <p>把 5,389 個沒人打算翻的名稱算進分母，進度數字就會永遠難看，
    而且看不出真正還缺什麼。裝備的<b>傳說敘述</b>照常算，那是要翻的。
    """
    return path.name.startswith("gear-") and entry.get("role") == "name"


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
                if optional(path, value):
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


def language_roster() -> str:
    """每一種語言各自翻到哪裡。

    <p>下面那張進度表算的是 zh_tw。只有一種語言時寫「目前進度」沒有問題，
    多了一種之後就會被讀成「整個專案的進度」——想來幫忙翻簡體的人會以為
    那邊只剩尾巴，實際上一個字都還沒有。所以把各語言的數字分開列。
    """
    root = TRANSLATIONS.parent
    rows = []
    for base in sorted(d for d in root.iterdir()
                       if d.is_dir() and not d.name.startswith("_")):
        # 用<b>跟下面那張表同一套</b>帳：同樣的檔案清單、同樣的
        # optional() 排除規則。自己另外數一遍的話，任務對話會被算兩次
        # ——quest/*.json 與它們產生出來的 quest-dialogue.json 都在——
        # 於是同一個語言在標題與這一行會出現兩個不同的百分比。
        done = total = 0
        for pattern, _ in ORDER:
            paths = sorted(base.glob(pattern))
            if paths:
                d, t = count(paths)
                done += d
                total += t
        if total:
            rows.append(f"`{base.name}` {done / total:.1%}")
    return "　·　".join(rows)


def write_language_manifest() -> list[str]:
    """把「jar 裡有哪些語言」寫成一份清單，給 Java 端讀。

    <p>依<b>實際資料夾</b>產生，不必手動維護——新增一種語言只要建一個資料夾，
    下次跑這支就會被收進去。手維護的清單一定會有人忘了改，
    而忘了改的後果是那個語言在遊戲裡選不到。
    """
    root = TRANSLATIONS.parent
    langs = sorted(d.name for d in root.iterdir()
                   if d.is_dir() and not d.name.startswith("_"))
    (root / "_languages.json").write_text(
        json.dumps({
            "_note": "打包了哪些語言。由 tools/update-docs.py 依實際資料夾產生，"
                     "不要手改。新增一種語言：複製 zh_tw/ 成新的語言代碼資料夾，"
                     "把所有 dst 清空，再跑一次這支工具。",
            "languages": langs,
        }, ensure_ascii=False, indent=1) + chr(10),
        encoding="utf-8")
    return langs


def main(argv: list[str]) -> int:
    check = "--check" in argv
    langs = write_language_manifest()
    if len(langs) > 1:
        print("語言：" + "、".join(langs))
    body, done, total = table()
    stamp = subprocess.run(["git", "log", "-1", "--format=%ad", "--date=short"],
                           capture_output=True, text=True).stdout.strip()
    body = (f"**繁體中文（zh_tw）進度 {done:,} / {total:,}"
            f"（{done / total:.1%}）**，更新於 {stamp}。\n\n"
            f"各語言：{language_roster()}\n\n{body}")
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
