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

# 主語言。它一定會被打包——其他語言沒翻到的地方靠它墊底，
# 見 Languages#fallbackFor。
DEFAULT_LANG = "zh_tw"

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
    """把檔案裡<b>每一組</b>標記之間的內容換成最新的進度。

    會有多組是因為 modrinth-description.md 是雙語的，英文與中文各有一段。
    只換第一組的話，另一段會留在原地繼續變舊——而「一半新一半舊」
    比整份都舊更難發現，因為沒有人會想到同一個檔案裡的兩個數字會不一樣。
    """
    text = path.read_text(encoding="utf-8")
    if START not in text or END not in text:
        print(f"  ! {path.name} 少了 {START} / {END} 標記，跳過")
        return False
    out = []
    rest = text
    while START in rest and END in rest:
        head, rest = rest.split(START, 1)
        _, rest = rest.split(END, 1)
        out.append(f"{head}{START}\n{body}\n{END}")
    fresh = "".join(out) + rest
    if fresh == text:
        return False
    path.write_text(fresh, encoding="utf-8", newline="\n")
    return True


# 語言代碼對母語名字。認不出來的就只顯示代碼——寧可少一欄，
# 也不要在別人的語言名字上瞎猜。
LANG_NAMES = {
    "zh_tw": "繁體中文",
    "zh_cn": "简体中文",
    "ja_jp": "日本語",
    "ru_ru": "Русский",
    "en_us": "English",
    "ko_kr": "한국어",
    "de_de": "Deutsch",
    "fr_fr": "Français",
    "es_es": "Español",
    "pt_br": "Português",
}


def language_totals() -> list[tuple[str, int, int]]:
    """每一種語言各自翻了幾條，由多到少。

    <p>用<b>跟檔案明細表同一套</b>帳：同樣的檔案清單、同樣的 optional()
    排除規則。自己另外數一遍的話，任務對話會被算兩次——`quest/*.json`
    與它們產生出來的 `quest-dialogue.json` 都在——同一個語言就會在
    兩張表出現兩個不同的百分比。
    """
    root = TRANSLATIONS.parent
    rows = []
    for base in sorted(d for d in root.iterdir()
                       if d.is_dir() and not d.name.startswith("_")):
        done = total = 0
        for pattern, _ in ORDER:
            paths = sorted(base.glob(pattern))
            if paths:
                d, t = count(paths)
                done += d
                total += t
        if total:
            rows.append((base.name, done, total))
    rows.sort(key=lambda r: (-r[1] / r[2], r[0]))
    return rows


def language_table(rows: list[tuple[str, int, int]]) -> str:
    """各語言的進度表，翻得最多的排最前面。"""
    lines = ["| 語言 | 進度 | 已翻 / 總數 |", "|---|---|---:|"]
    for code, done, total in rows:
        name = LANG_NAMES.get(code)
        label = f"`{code}` {name}" if name else f"`{code}`"
        lines.append(f"| {label} | {bar(done, total)} {done / total:.1%} "
                     f"| {done:,} / {total:,} |")
    return "\n".join(lines)


# 每一個檔案的英文說明，給非中文的翻譯團隊看。
ORDER_EN = {
    "ui-labels.json": "Seen on every item tooltip (Strength, Combat Level, ...)",
    "npc.json": "NPC names, seen while walking around town",
    "gui.json": "Menus and interface text",
    "quest.json": "Quest names and the quest interface",
    "ability/*.json": "Ability tree",
    "major-id.json": "Major IDs on mythic gear",
    "quest-dialogue.json": "NPC dialogue (by far the largest file)",
    "ingredient.json": "Crafting ingredients",
    "material.json": "Crafting materials",
    "tome.json": "Tomes",
    "aspect.json": "Raid aspects",
    "gear-*.json": "Gear lore (names stay in English, not counted)",
}


def write_progress_doc(rows: list[tuple[str, int, int]]) -> None:
    """每一種語言各自缺哪些檔案，寫成 docs/PROGRESS.md。

    <p>README 只放各語言的總進度——那是給「路過的人」看的。真正要動手翻的人
    需要的是另一件事：<b>我的語言還缺哪幾個檔</b>。兩者混在同一張表裡，
    七種語言乘上十二個檔案就是八十四列，兩種讀者都讀不下去。

    <p>這份是產生的，不要手改。
    """
    out = ["<!-- 這份文件由 tools/update-docs.py 產生，不要手改。 -->",
           "<!-- Generated by tools/update-docs.py - do not edit by hand. -->",
           "",
           "# 翻譯進度 / Translation progress",
           "",
           "每一種語言各自還缺哪些檔案。總覽在 [README](../README.md)。",
           "",
           "What is left to translate, per language. "
           "For the overview see the [README](../README.en.md).",
           ""]
    root = TRANSLATIONS.parent
    for code, done, total in rows:
        name = LANG_NAMES.get(code, code)
        out.append(f"## `{code}` {name} — {done / total:.1%}"
                   f"（{done:,} / {total:,}）")
        out.append("")
        out.append("| 檔案 / File | 進度 / Progress | 已翻 / Done |"
                   " 什麼時候會看到 / Where it shows |")
        out.append("|---|---|---:|---|")
        base = root / code
        for pattern, when in ORDER:
            paths = sorted(base.glob(pattern))
            if not paths:
                continue
            d, tt = count(paths)
            if tt == 0:
                continue
            note = when
            english = ORDER_EN.get(pattern)
            if english:
                note = when + "<br>" + english
            out.append(f"| `{pattern}` | {bar(d, tt)} {d / tt:.0%} "
                       f"| {d:,} / {tt:,} | {note} |")
        out.append("")
    Path("docs/PROGRESS.md").write_text(
        chr(10).join(out).rstrip() + chr(10), encoding="utf-8", newline=chr(10))


def write_language_manifest() -> list[str]:
    """把「jar 裡有哪些語言」寫成一份清單，給 Java 端與打包用。

    <p>依<b>實際資料夾</b>產生，不必手動維護——新增一種語言只要建一個資料夾，
    下次跑這支就會被收進去。手維護的清單一定會有人忘了改，
    而忘了改的後果是那個語言在遊戲裡選不到。

    <h2>一條譯文都沒有的語言不列進去</h2>
    新語言剛開的時候是一份空骨架：結構完整、原文齊全、譯文全空。那份骨架
    是給<b>翻譯的人</b>用的，從 repo 拿就好；打進 jar 對玩家沒有任何用處，
    卻要他多下載一份完整的原文。

    <p>實測三種空語言讓 jar 從 3.0MB 變成 9.5MB——多出來的 6.5MB
    一個字都不會顯示。列進去還有反效果：語言是照遊戲設定自動選的，
    一個空語言被選到，玩家看到的就是什麼都沒翻。

    <p>{@code zh_tw} 一定在列表裡，它是回退的底。
    """
    root = TRANSLATIONS.parent
    shipped = []
    for name, done, _total in language_totals():
        if done or name == DEFAULT_LANG:
            shipped.append(name)
    shipped.sort()
    (root / "_languages.json").write_text(
        json.dumps({
            "_note": "打包了哪些語言。由 tools/update-docs.py 產生，不要手改。"
                     "一條譯文都沒有的語言不會列進來，也不會打進 jar——"
                     "空骨架是給翻譯的人用的，玩家不需要多下載一份原文。"
                     "新增一種語言請跑 tools/new-language.py。",
            "languages": shipped,
        }, ensure_ascii=False, indent=1) + chr(10),
        encoding="utf-8")
    return shipped


def main(argv: list[str]) -> int:
    check = "--check" in argv
    if "--files" in argv:
        # 檔案明細不再寫進 README，但翻譯的人還是需要「還缺什麼」，
        # 所以留一個看得到的入口。
        detail, done, total = table()
        print(detail)
        return 0
    langs = write_language_manifest()
    if len(langs) > 1:
        print("語言：" + "、".join(langs))
    body, done, total = table()
    stamp = subprocess.run(["git", "log", "-1", "--format=%ad", "--date=short"],
                           capture_output=True, text=True).stdout.strip()
    rows = language_totals()
    write_progress_doc(rows)
    if len(rows) > 1:
        # 多語言時只列各語言的總進度。檔案明細那張是給翻譯的人看的，
        # 一種語言就已經十幾列，四種語言擺上來沒有人會讀完——
        # 而讀者在這裡真正想知道的只有「我的語言翻到哪了」。
        #
        # 明細沒有消失，跑 python tools/update-docs.py --files 就看得到。
        body = (f"更新於 {stamp}。\n\n{language_table(rows)}\n\n"
                f"每一種語言**還缺哪些檔案**見 "
                f"[docs/PROGRESS.md](docs/PROGRESS.md)。<br>"
                f"Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).")
    else:
        body = (f"**目前進度 {done:,} / {total:,}"
                f"（{done / total:.1%}）**，更新於 {stamp}。\n\n{body}")
    stale = []
    for name in ("README.md", "CONTRIBUTING.md",
                 "docs/modrinth-description.md", "docs/curseforge-description.md"):
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
