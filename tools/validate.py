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
GLOSSARY_FILE = ROOT / "GLOSSARY.md"
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"
PLACES_FILE = ROOT / "src/main/resources/assets/wynnchayuan/places.json"

PLACEHOLDERS = ("{#}", "{~}", "{p}", "{u}")

# 看起來想寫佔位符但寫錯的樣子。這種錯在遊戲裡會原樣顯示出來。
BAD_PLACEHOLDER = re.compile(r"[｛{]\s*[#~pu]\s*[｝}]")

# 帶編號的數值佔位符：{~1} 到 {~9}。指名要原文的第幾個數值。
NUMBERED = re.compile(r"\{~[1-9]\}")


def load_glossary() -> dict[str, str]:
    """讀 GLOSSARY.md 裡的對照表。

    直接解析 markdown 而不是另外開一份 json，是為了<b>只有一份</b>：
    兩個檔案的話，改了一邊忘了另一邊，表就會開始說謊——而說謊的對照表
    比沒有對照表更糟，因為大家會照著它翻。

    只認「原文 | 譯文」這種兩欄以上的表格列，跳過表頭與分隔線。
    含斜線的複合欄位（風／土／雷）跳過，那是說明不是對照。
    """
    terms: dict[str, str] = {}
    if not GLOSSARY_FILE.is_file():
        return terms

    # 只認表頭是「原文 | 譯文」的表。GLOSSARY 裡還有別種表——
    # 角色語氣、雙關說明——那些第二欄不是譯文，讀進來會變成假的對照，
    # 於是 npc.json 的「The Cook」被指控沒照對照表翻。
    in_terms = False
    for line in GLOSSARY_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line.startswith("|"):
            in_terms = False
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) >= 2 and cells[0] == "原文" and cells[1] == "譯文":
            in_terms = True
            continue
        if not in_terms or line.startswith("|---"):
            continue
        if len(cells) < 2:
            continue
        src, dst = cells[0], cells[1]
        if not src or not dst or src in ("原文",) or set(dst) <= set("-: "):
            continue
        if "/" in src or "／" in src:
            continue                   # 複合欄位，不是單一詞條
        terms[src] = dst
    return terms


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
               places: set[str], glossary: dict[str, str]) -> list[Problem]:
    """檢查一組原文／譯文。"""
    out: list[Problem] = []

    if not dst.strip():
        return out                     # 還沒翻，不是錯

    if dst.strip() == src.strip():
        out.append(Problem("warn", path, key,
                           "譯文和原文完全相同 —— 如果是刻意保留原文可以忽略"))

    # 佔位符：數量必須一模一樣。少一個 -> 遊戲裡那個數字／符號會憑空消失；
    # 多一個 -> 會有多餘的符號被塞進去或直接顯示成 {~}
    numbered = len(NUMBERED.findall(dst))
    for ph in PLACEHOLDERS:
        want, got = src.count(ph), dst.count(ph)
        if ph == "{~}":
            # 帶編號的 {~1} 指名要第幾個數值，中文語序跟英文相反時會用到。
            # 它不算「消耗一個」，所以只要「沒編號的 + 帶編號的」湊得出來就行。
            if got + numbered >= want and got <= want:
                continue
            got += numbered
        if want != got:
            out.append(Problem("error", path, key,
                               f"佔位符 {ph} 數量不符：原文 {want} 個，譯文 {got} 個"))

    bad = BAD_PLACEHOLDER.findall(dst)
    wrong = [b for b in bad if b not in PLACEHOLDERS]
    if wrong:
        out.append(Problem("error", path, key,
                           f"佔位符寫錯：{wrong}（要正好是 {{#}} {{~}} {{p}} {{u}}，不能有空格或全形括號）"))

    # 換行數不必一致。中文比英文緊湊，原文分兩行的句子往往一行就講完，
    # 硬湊行數會斷在莫名其妙的地方（issue #44）。模組會把整段的佔位符依序填回，
    # 行數不同也對得上。
    #
    # 只在譯文「比原文還多行」時提醒：那多半是誤按了換行，而且真的會把版面撐高。
    if dst.count(chr(10)) > src.count(chr(10)):
        out.append(Problem("warn", path, key,
                           f"譯文行數比原文多（{src.count(chr(10)) + 1} → "
                           f"{dst.count(chr(10)) + 1} 行）—— 中文通常更短，"
                           f"確認一下是不是多按了換行"))

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
    # 全形標點自帶右側留白，後面刻意不接空格——模組也是這樣處理的。
    # 不排除的話，每一條「標籤：」的譯文都會被警告一次。
    dropped_after_punctuation = (src[-1:].isspace() and not dst[-1:].isspace()
                                 and dst[-1:] in "：，。、！？；）」』】")
    if not dropped_after_punctuation and (
            src[:1].isspace() != dst[:1].isspace()
            or src[-1:].isspace() != dst[-1:].isspace()):
        out.append(Problem("warn", path, key,
                           "首尾空白與原文不一致 —— 那些空白是用來對齊欄位的"))

    # 太長會把 tooltip 撐爆。中文通常比英文短，長很多多半是把說明也寫進去了
    if len(dst) > max(20, len(src) * 2):
        out.append(Problem("warn", path, key,
                           f"譯文比原文長很多（{len(src)} → {len(dst)} 字），可能會撐爆版面"))

    # 整條就是對照表裡的詞，卻翻成別的說法 —— 同一個詞兩種譯法，
    # 玩家會以為是兩個不同的東西
    agreed = glossary.get(src.strip())
    if agreed and agreed != dst.strip():
        out.append(Problem("warn", path, key,
                           f"與對照表不一致：GLOSSARY.md 寫「{agreed}」。"
                           f"要改譯法請先改 GLOSSARY.md，讓所有人一起跟著改"))

    return out


def is_generated(file: Path) -> bool:
    """這個檔案是不是由別的檔案產生的。

    產生物不該被檢查——它的內容<b>本來就</b>是別處的副本，跨檔比對必然報
    「兩份譯法不同」。實際發生過：譯者改了 quest/ 底下的任務檔，
    quest-dialogue.json 還是舊的，於是每一個翻譯 PR 都紅燈，
    而譯者在網頁上根本沒辦法重新產生。
    """
    try:
        data = json.loads(file.read_text(encoding="utf-8"))
    except Exception:
        return False
    return bool(data.get("_meta", {}).get("generated"))


def check_file(file: Path, places: set[str],
               glossary: dict[str, str]) -> list[Problem]:
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
                                   entry.get("dst", ""), places, glossary)
    else:
        # 扁平格式：{原文: 譯文}
        for key, value in data.items():
            if key.startswith("_"):
                continue               # _note 之類的說明欄位
            if not isinstance(value, str):
                problems.append(Problem("error", name, key, "譯文應該是字串"))
                continue
            problems += check_pair(name, key, key, value, places, glossary)

    return problems


def check_duplicates(files: list[Path]) -> list[Problem]:
    """同一個原文出現在兩個檔案裡，而且譯法不同。

    這種錯在遊戲裡<b>完全看不出來</b>：載入順序決定誰贏，改了輸的那一份
    會覺得「我明明改了卻沒生效」。而且順序是 _index.json 決定的，
    調整檔案順序就可能無聲地換掉譯文。

    譯法相同的重複只是冗餘，不影響結果，所以只報不同的。
    """
    seen: dict[str, tuple[str, str]] = {}
    out: list[Problem] = []
    for file in files:
        try:
            data = json.loads(file.read_text(encoding="utf-8"))
        except Exception:
            continue                   # 格式問題由 check_file 負責回報
        rows = (data["entries"] if isinstance(data.get("entries"), dict) else None)
        pairs = ([(v.get("src", ""), v.get("dst", "")) for v in rows.values()]
                 if rows else
                 [(k, v) for k, v in data.items()
                  if not k.startswith("_") and isinstance(v, str)])
        for src, dst in pairs:
            if not src or not dst:
                continue
            if src in seen and seen[src][1] != dst:
                first_file, first_dst = seen[src]
                out.append(Problem("error", file.name, src,
                                   f"與 {first_file} 的譯法不同："
                                   f"「{first_dst}」vs「{dst}」。"
                                   f"載入順序決定誰生效，改到輸的那一份會沒反應"))
            else:
                seen.setdefault(src, (file.name, dst))
    return out


def main(argv: list[str]) -> int:
    places = load_places()
    glossary = load_glossary()

    if argv:
        files = [TRANSLATIONS / a for a in argv]
        missing = [f for f in files if not f.is_file()]
        if missing:
            print("找不到：" + ", ".join(m.name for m in missing))
            return 2
    else:
        files = sorted(f for f in TRANSLATIONS.glob("*.json")
                       if not f.name.startswith("_"))
        # 任務對話一個任務一個檔，放在子資料夾裡
        files += sorted(TRANSLATIONS.glob("quest/*.json"))
        # 技能樹也是一職業一個檔。先前<b>整個沒被檢查</b>——而它正好是改動最頻繁
        # 的一批。實測撈出兩條 {#} 數量不符的，那種條目在遊戲裡是<b>整條不顯示</b>，
        # 畫面上跟「還沒翻」長得一模一樣，沒有人會發現。
        files += sorted(TRANSLATIONS.glob("ability/*.json"))
        files = [f for f in files if not is_generated(f)]

    errors = 0
    warnings = 0

    dupes = check_duplicates(files)
    if dupes:
        print()
        print("跨檔重複")
        for p in dupes:
            print(p)
            errors += 1

    for file in files:
        problems = check_file(file, places, glossary)
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
    print(f"檢查了 {len(files)} 個檔案：{errors} 個錯誤，{warnings} 個警告"
          f"（對照表 {len(glossary)} 個詞）")
    if errors:
        print("錯誤必須修掉才能合併；警告請自己判斷是不是刻意的。")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
