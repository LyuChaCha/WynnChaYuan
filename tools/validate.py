#!/usr/bin/env python3
"""檢查譯文檔，把機械性的錯誤在合併前擋下來。

為什麼需要
----------
翻譯是開放給大家提交的，而錯掉的譯文在遊戲裡的表現往往<不是>「翻得怪」，
而是「版面爛掉」或「整句消失」——那種問題丟到玩家端才會發現，而且很難
回推是哪一筆造成的。

這裡只檢查<機器看得出來>的錯：佔位符掉了、換行數不對、把材質包符號直接
貼進譯文、地名被翻掉。這些都是無論翻得好不好都一定錯的東西。

翻得對不對、語氣順不順，機器判斷不了，那需要人看——見 CONTRIBUTING.md。
所以這支程式的定位是「先擋掉一定錯的」，不是「保證翻對」。

用法
----
    python tools/validate.py                 # 檢查全部
    python tools/validate.py ui-labels.json  # 只檢查某幾個檔

回傳 0 代表沒有錯誤（可能仍有警告）。
"""

from __future__ import annotations

import json
import re
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"
PLACES_FILE = ROOT / "src/main/resources/assets/wynnchayuan/places.json"

PLACEHOLDERS = ("{#}", "{~}", "{p}")

# 看起來想寫佔位符但寫錯的樣子。這種錯在遊戲裡會原樣顯示出來。
BAD_PLACEHOLDER = re.compile(r"[｛{]\s*[#~p]\s*[｝}]")


def load_places() -> set[str]:
    data = json.loads(PLACES_FILE.read_text(encoding="utf-8"))
    return {p.strip() for p in data.get("places", []) if p.strip()}


def is_glyph(ch: str) -> bool:
    """材質包符號：私用區或未指派碼位。

    Wynncraft 用的是第 13 平面（U+D0000 起），那是「未指派」而不是私用區——
    只查私用區會漏掉一大半，這點在 Java 端也踩過同一個坑。
    """
    return unicodedata.category(ch) in ("Co", "Cn")


class Problem:
    def __init__(self, level: str, path: str, key: str, message: str):
        self.level = level
        self.path = path
        self.key = key
        self.message = message

    def __str__(self) -> str:
        mark = "錯誤" if self.level == "error" else "警告"
        return f"  [{mark}] {self.path} :: {self.key}\n         {self.message}"


def check_pair(path: str, key: str, src: str, dst: str,
               places: set[str]) -> list[Problem]:
    """檢查一組原文／譯文。"""
    out: list[Problem] = []

    if not dst.strip():
        return out                     # 還沒翻，不是錯

    if dst.strip() == src.strip():
        out.append(Problem("warn", path, key,
                           "譯文和原文完全相同 —— 如果是刻意保留原文可以忽略"))

    # 佔位符：數量必須一模一樣。少一個 -> 遊戲裡那個數字／符號會憑空消失；
    # 多一個 -> 會有多餘的符號被塞進去或直接顯示成 {~}
    for ph in PLACEHOLDERS:
        want, got = src.count(ph), dst.count(ph)
        if want != got:
            out.append(Problem("error", path, key,
                               f"佔位符 {ph} 數量不符：原文 {want} 個，譯文 {got} 個"))

    bad = BAD_PLACEHOLDER.findall(dst)
    wrong = [b for b in bad if b not in PLACEHOLDERS]
    if wrong:
        out.append(Problem("error", path, key,
                           f"佔位符寫錯：{wrong}（要正好是 {{#}} {{~}} {{p}}，不能有空格或全形括號）"))

    # 換行：技能說明是固定幾行的排版，行數變了整塊會擠掉
    if src.count("\n") != dst.count("\n"):
        out.append(Problem("error", path, key,
                           f"換行數不符：原文 {src.count(chr(10))} 個，"
                           f"譯文 {dst.count(chr(10))} 個"))

    # 材質包符號不能直接貼進譯文 —— 那些由程式填回原位，手寫的會是錯的碼位
    glyphs = {ch for ch in dst if is_glyph(ch)}
    if glyphs:
        codes = ", ".join(f"U+{ord(c):04X}" for c in sorted(glyphs))
        out.append(Problem("error", path, key,
                           f"譯文含材質包符號（{codes}）—— 請用 {{#}} 佔位，程式會填回去"))

    # 地名永遠保留原文，翻掉的話跟其他玩家對不上話
    for place in places:
        if re.search(rf"\b{re.escape(place)}\b", src) and place not in dst:
            out.append(Problem("warn", path, key,
                               f"原文有地名「{place}」但譯文裡找不到 —— 地名不翻譯"))

    # 首尾空白會影響對齊（Wynncraft 用它排版）
    if (src[:1].isspace() != dst[:1].isspace()
            or src[-1:].isspace() != dst[-1:].isspace()):
        out.append(Problem("warn", path, key,
                           "首尾空白與原文不一致 —— 那些空白是用來對齊欄位的"))

    # 太長會把 tooltip 撐爆。中文通常比英文短，長很多多半是把說明也寫進去了
    if len(dst) > max(20, len(src) * 2):
        out.append(Problem("warn", path, key,
                           f"譯文比原文長很多（{len(src)} → {len(dst)} 字），可能會撐爆版面"))

    return out


def check_file(file: Path, places: set[str]) -> list[Problem]:
    name = file.name
    try:
        raw = file.read_text(encoding="utf-8")
    except UnicodeDecodeError as e:
        return [Problem("error", name, "(整個檔案)", f"不是 UTF-8：{e}")]

    try:
        data = json.loads(raw)
    except json.JSONDecodeError as e:
        return [Problem("error", name, f"第 {e.lineno} 行",
                        f"JSON 格式錯誤：{e.msg}")]

    problems: list[Problem] = []

    if "entries" in data and isinstance(data["entries"], dict):
        # 有 _meta 的格式：{hash: {src, dst, role}}
        for key, entry in data["entries"].items():
            if not isinstance(entry, dict):
                problems.append(Problem("error", name, key, "應該是物件"))
                continue
            problems += check_pair(name, key, entry.get("src", ""),
                                   entry.get("dst", ""), places)
    else:
        # 扁平格式：{原文: 譯文}
        for key, value in data.items():
            if key.startswith("_"):
                continue               # _note 之類的說明欄位
            if not isinstance(value, str):
                problems.append(Problem("error", name, key, "譯文應該是字串"))
                continue
            problems += check_pair(name, key, key, value, places)

    return problems


def main(argv: list[str]) -> int:
    places = load_places()

    if argv:
        files = [TRANSLATIONS / a for a in argv]
        missing = [f for f in files if not f.is_file()]
        if missing:
            print("找不到：" + ", ".join(m.name for m in missing))
            return 2
    else:
        files = sorted(f for f in TRANSLATIONS.glob("*.json")
                       if not f.name.startswith("_"))

    errors = 0
    warnings = 0
    for file in files:
        problems = check_file(file, places)
        if not problems:
            continue
        print(f"\n{file.name}")
        for p in problems:
            print(p)
            if p.level == "error":
                errors += 1
            else:
                warnings += 1

    print()
    print(f"檢查了 {len(files)} 個檔案：{errors} 個錯誤，{warnings} 個警告")
    if errors:
        print("錯誤必須修掉才能合併；警告請自己判斷是不是刻意的。")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
