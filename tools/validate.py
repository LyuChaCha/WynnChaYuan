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
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw"
# 所有語言的根。譯文按語言分層之後，檢查要<b>每一種語言各跑一輪</b>——
# 尤其是「同一個原文兩種譯法」那條：zh_tw 與 ja_jp 的同一句話本來就會
# 長得不一樣，混在一起比會每一條都報錯。
LANG_ROOT = TRANSLATIONS.parent
PLACES_FILE = ROOT / "src/main/resources/assets/wynnchayuan/places.json"

# 語言資料夾的名字：zh_tw、ja_jp、en_us……
LANG_CODE = re.compile(r"[a-z]{2}_[a-z]{2}")

PLACEHOLDERS = ("{#}", "{~}", "{p}", "{u}")

# 看起來想寫佔位符但寫錯的樣子。這種錯在遊戲裡會原樣顯示出來。
BAD_PLACEHOLDER = re.compile(r"[｛{]\s*[#~pu]\s*[｝}]")

# 帶編號的數值佔位符：{~1} 到 {~9}。指名要原文的第幾個數值。
NUMBERED = re.compile(r"\{~[1-9]\}")

# 顏色佔位符。只出現在<b>譯文</b>——原文的鍵是遊戲送來的字，不會有這種東西。
#
#   {c1}–{c9}      用原文的第 N 個顏色（依第一次出現的順序編號）
#   {c:#FF55FF}    自己指定色碼
#   {c:gold}       自己指定原版顏色名稱
#   {/}            到此為止，回到這一段原本的樣式
#
# 哪一個編號是哪一個顏色，看 majorid-debug.txt 的「可用的顏色」那一段。
COLOUR = re.compile(r"\{c(?:[1-9]|:[^}]+)\}")

# 看起來想寫顏色佔位符但寫錯的樣子：{c}、{c0}、{ c1 }、{c12}、{C1}……
# 這種錯不會讓譯文消失，會<b>原樣印在畫面上</b>，玩家直接看到 {c1}。
BAD_COLOUR = re.compile(r"[｛{]\s*[cC][^}｝]*[｝}]")

# Minecraft 的十六個原版顏色名稱，{c:名稱} 只認這些。
# Wynncraft 大部分的顏色不在裡面（元素色、稀有度色都是自訂色碼），
# 那些請用 {cN} 直接搬原文的，不要自己挑一個相近的原版色。
VANILLA_COLOURS = {
    "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
    "gold", "gray", "dark_gray", "blue", "green", "aqua", "red",
    "light_purple", "yellow", "white",
}

# {/}：顏色到此為止，回到這一段原本的樣式。
COLOUR_END = "{/}"

# 色碼：井字號加六位十六進位。
HEX_COLOUR = re.compile(r"#[0-9A-Fa-f]{6}$")


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


# 這些語言的譯文比英文<b>緊湊</b>：一個方塊字抵得上一個英文單字。
# 其他語言（西、德、法、俄……）反過來，往往比原文長。長度檢查要分開看，
# 否則德文會被整批警告「比原文長很多」——那是德文，不是錯誤。
COMPACT = ("zh", "ja", "ko")


def compact(lang: str) -> bool:
    return lang.split("_")[0] in COMPACT


# 每種語言的譯文<b>不該</b>出現哪些文字系統。
#
# 為什麼要查這個：譯文常常是一大批一次寫進去的，中間混進一兩個別種文字的詞
# （`我們всі都想…`、`就有條路можно下去`）不會讓 JSON 壞掉、不會讓佔位符對不上、
# 也不會被任何既有規則攔下來——它只會在遊戲裡靜靜地顯示成一串亂碼。
# 實際發生過兩次，兩次都是靠人眼看到的。
#
# 只列<b>確定不屬於</b>該語言的系統：拉丁字母到處都要用（專有名詞維持原文），
# 所以永遠不查；假名對 ja_jp 是正常的，對 zh_tw 就不是。
FOREIGN_SCRIPTS = {
    "西里爾字母": (0x0400, 0x04FF),
    "希臘字母": (0x0370, 0x03FF),
    "諺文": (0xAC00, 0xD7AF),
    "假名": (0x3040, 0x30FF),
    "阿拉伯字母": (0x0600, 0x06FF),
    "希伯來字母": (0x0590, 0x05FF),
    "泰文": (0x0E00, 0x0E7F),
    "天城文": (0x0900, 0x097F),
}

# 語言 -> 這個語言合理會用到的系統（拉丁與 CJK 之外）
NATIVE_SCRIPTS = {
    "ru": {"西里爾字母"},
    "uk": {"西里爾字母"},
    "bg": {"西里爾字母"},
    "sr": {"西里爾字母"},
    "el": {"希臘字母"},
    "ko": {"諺文"},
    "ja": {"假名"},
    "ar": {"阿拉伯字母"},
    "he": {"希伯來字母"},
    "th": {"泰文"},
    "hi": {"天城文"},
}


# 一個<b>小寫</b>英文單字直接黏在漢字上。
#
# 專有名詞不會被抓到：它們有大寫開頭（`Slykaar的住所`、`與Lanu對話`），
# 前後也不會出現「小寫開頭又緊貼漢字」的形狀。真正會命中的是打字時
# 手滑留下的英文詞（`給我control住`）與中英夾雜的口語（`很有style地`）。
#
# 這條是<b>警告</b>不是錯誤：夾雜有時是刻意的口語風格，該由譯者自己判斷。
GLUED_ENGLISH = re.compile(
    r"(?<![A-Za-z])[a-z]{2,}(?=[一-鿿])"
    r"|(?<=[一-鿿])[a-z]{2,}(?![A-Za-z])")


def foreign_script(dst: str, lang: str) -> tuple[str, str] | None:
    """譯文裡有沒有明顯不屬於這個語言的文字。

    回傳 (系統名, 命中的那一小段) ，沒有就回 None。
    """
    allowed = NATIVE_SCRIPTS.get(lang.split("_")[0], set())
    for i, ch in enumerate(dst):
        code = ord(ch)
        for name, (low, high) in FOREIGN_SCRIPTS.items():
            if low <= code <= high and name not in allowed:
                return name, dst[max(0, i - 6):i + 7]
    return None


def check_pair(path: str, key: str, src: str, dst: str,
               places: set[str], glossary: dict[str, str],
               lang: str = "zh_tw") -> list[Problem]:
    """檢查一組原文／譯文。"""
    out: list[Problem] = []

    if not dst.strip():
        return out                     # 還沒翻，不是錯

    if dst.strip() == src.strip():
        out.append(Problem("warn", path, key,
                           "譯文和原文完全相同 —— 如果是刻意保留原文可以忽略"))

    # 混進別種文字的字。只看譯文——原文是遊戲送來的，它想長什麼樣是它的事。
    stray = foreign_script(dst, lang)
    if stray:
        name, around = stray
        out.append(Problem("error", path, key,
                           f"譯文裡混進了{name}：「{around}」"
                           f" —— {lang} 用不到這種文字，遊戲裡會顯示成亂碼"))

    if compact(lang):
        glued = GLUED_ENGLISH.search(dst)
        if glued:
            around = dst[max(0, glued.start() - 10):glued.end() + 10]
            out.append(Problem("warn", path, key,
                               f"小寫英文單字黏在漢字上：「{around}」"
                               f" —— 如果是刻意的中英夾雜可以忽略"))

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

    # 顏色佔位符。寫錯不會讓譯文消失，會原樣印在畫面上——玩家直接看到 {c1}，
    # 而且沒有人會想到那是譯文檔裡的錯字。
    bad_colours = [b for b in BAD_COLOUR.findall(dst) if not COLOUR.fullmatch(b)]
    if bad_colours:
        out.append(Problem("error", path, key,
                           f"顏色佔位符寫錯：{bad_colours}（要正好是 {{c1}}–{{c9}}、"
                           f"{{c:#FF55FF}} 或 {{c:gold}}，不能有空格、全形括號或兩位數）"))
    for spec in COLOUR.findall(dst):
        if not spec.startswith("{c:"):
            continue
        name = spec[3:-1]
        if HEX_COLOUR.match(name) or name in VANILLA_COLOURS:
            continue
        out.append(Problem("error", path, key,
                           f"{spec} 不是認得出來的顏色 —— 色碼要寫成 #RRGGBB，"
                           f"名稱只認 Minecraft 原版那十六個"
                           f"（{', '.join(sorted(VANILLA_COLOURS))}）。"
                           f"Wynncraft 自己的顏色請用 {{c1}}–{{c9}} 直接搬原文的"))
    if COLOUR.search(src) or COLOUR_END in src:
        out.append(Problem("error", path, key,
                           "原文的鍵裡不該有顏色佔位符 —— 那是給譯文用的，"
                           "寫進鍵裡會永遠查不到"))
    # 沒有色段在開著的時候寫 {/}，那個 {/} 什麼都不會做。
    #
    # 這裡看<b>位置</b>而不是只數個數。數個數漏得掉「數量剛好相等、但 {/}
    # 寫在 {cN} 前面」的情形（`{/}文字{c1}…`），而那正是誤解語意的樣子。
    #
    # 反過來「色段開著沒關」<b>不報</b>：一路染到這一段結尾是正當寫法
    # （`提升 Uppercut 的傷害\n{c1}與魔力消耗。`），而且色段的作用範圍
    # 本來就止於這一條，不會外溢到別的條目。
    stray_end = 0
    open_span = False
    for piece in re.split(r"(\{c(?:[1-9]|:[^}]+)\}|\{/\})", dst):
        if piece == COLOUR_END:
            if not open_span:
                stray_end += 1
            open_span = False
        elif COLOUR.fullmatch(piece or ""):
            open_span = True
    if stray_end:
        out.append(Problem("warn", path, key,
                           f"有 {stray_end} 個 {COLOUR_END} 前面沒有開著的色段 —— "
                           f"那個 {COLOUR_END} 不會有任何作用，"
                           f"確認是不是刪掉 {{cN}} 時忘了一起刪"))

    # 換行數不必一致。中文比英文緊湊，原文分兩行的句子往往一行就講完，
    # 硬湊行數會斷在莫名其妙的地方（issue #44）。模組會把整段的佔位符依序填回，
    # 行數不同也對得上。
    #
    # 只在譯文「比原文還多行」時提醒：那多半是誤按了換行，而且真的會把版面撐高。
    if dst.count(chr(10)) > src.count(chr(10)):
        out.append(Problem("warn", path, key,
                           f"譯文行數比原文多（{src.count(chr(10)) + 1} → "
                           f"{dst.count(chr(10)) + 1} 行）—— "
                           f"確認一下是不是多按了換行"))

    # § 格式碼：譯文可以自己帶（讓翻譯團隊排版用），但後面必須是有效的碼，
    # 否則遊戲裡會原樣印出一個「§z」——看起來像亂碼，而且沒有人會想到是這裡。
    for bad in re.findall(r"§(.)", dst):
        if bad.lower() not in "0123456789abcdefklmnor":
            out.append(Problem("error", path, key,
                               f"§ 後面不是有效的格式碼（§{bad}）—— "
                               f"遊戲裡會原樣印出來。顏色是 §0–§9 §a–§f，"
                               f"§r 回到原本的樣式"))

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
    # 中日韓一個字抵一個英文單字，長很多多半是把說明也寫進去了。
    # 其他語言本來就比英文長（德文尤其），門檻放寬，不然整批都是雜訊。
    ratio = 2 if compact(lang) else 3
    if len(dst) > max(20, len(src) * ratio):
        out.append(Problem("warn", path, key,
                           f"譯文比原文長很多（{len(src)} → {len(dst)} 字），可能會撐爆版面"))

    # 整條就是對照表裡的詞，卻翻成別的說法 —— 同一個詞兩種譯法，
    # 玩家會以為是兩個不同的東西
    # GLOSSARY.md 是<b>中文</b>的對照表。拿它去比西班牙文，每一條都會被指控
    # 「與對照表不一致」——那不是提醒，那是把檢查變成雜訊。
    # 其他語言要有自己的對照表時再說。
    agreed = glossary.get(src.strip()) if lang.startswith("zh") else None
    if agreed and agreed != dst.strip():
        out.append(Problem("warn", path, key,
                           f"與對照表不一致：GLOSSARY.md 寫「{agreed}」。"
                           f"要改譯法請先改 GLOSSARY.md，讓所有人一起跟著改"))

    # 括號一律用半形。
    #
    # 全形括號佔一整個中文字的寬度，而 Wynncraft 的版面是照<b>英文</b>算好的：
    # 對話框、tooltip、漂浮字都有固定寬度，一對全形括號就多吃掉一個字的空間，
    # 該對齊的地方就對不上。半形括號兩個加起來才等於一個中文字。
    #
    # 這一條訂成錯誤而不是警告：它是機械性的，沒有「這裡刻意要全形」的情況，
    # 而且一旦混進去就會散佈到整個語料（2026-08-28 一次清掉 77 條）。
    if "（" in dst or "）" in dst:
        out.append(Problem("error", path, key,
                           "譯文裡有全形括號（）—— 一律用半形 ()，"
                           "全形的會多吃掉一個中文字的寬度，版面會對不齊"))

    return out


def is_generated(file: Path) -> bool:
    """這個檔案是不是由別的檔案產生的。

    產生物不該被檢查——它的內容<b>本來就</b>是別處的副本，跨檔比對必然報
    「兩份譯法不同」。實際發生過：譯者改了 quest/ 底下的任務檔，
    quest-dialogue.json 還是舊的，於是每一個翻譯 PR 都紅燈，
    而譯者在網頁上根本沒辦法重新產生。

    只認 ``"generated": true``（布林）。抽取來的語料在同一個欄位放的是
    <b>時間戳字串</b>（``"2026-08-19T..."``），意思是「什麼時候從官方資料抽的」，
    那些是要人翻的正本，不是副本。先前用 ``bool()`` 判斷，字串一律為真，
    於是 ingredient、material、tome、aspect、gear-* 這一整批
    <b>整個沒被檢查過</b>——七千多條。裡面確實藏著撞句（``Black Hole`` 在
    ingredient.json 是「黑孔洞」、在 ability/assassin.json 是「引力黑洞」），
    而那種錯在遊戲裡看不出來：載入順序決定誰生效。
    """
    try:
        data = json.loads(file.read_text(encoding="utf-8"))
    except Exception:
        return False
    return data.get("_meta", {}).get("generated") is True


def check_file(file: Path, places: set[str],
               glossary: dict[str, str], lang: str = "zh_tw") -> list[Problem]:
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
                                   entry.get("dst", ""), places, glossary, lang)
    else:
        # 扁平格式：{原文: 譯文}
        for key, value in data.items():
            if key.startswith("_"):
                continue               # _note 之類的說明欄位
            if not isinstance(value, str):
                problems.append(Problem("error", name, key, "譯文應該是字串"))
                continue
            problems += check_pair(name, key, key, value, places, glossary, lang)

    return problems


def check_substitutable_names(files: list[Path]) -> list[Problem]:
    """`_meta.itemNames: false` 只能用在「名稱會出現在別的敘述裡」的檔案。

    <h2>這個旗標實際上在決定什麼</h2>
    `false` 會讓該檔 `role: name` 的條目進入<b>可替換的詞</b>表
    （見 TranslationStore#noteTerm）——也就是那些名稱會被塞進任何剛好含有
    同樣字串的文字裡。技能與 Major ID 需要這個行為（「提升 Meteor 的傷害」），
    道具名稱<b>不需要</b>，而且會出事。

    <p>實際發生過：`ingredient.json` 誤標成 `false` 之後，素材 `Dark Matter`
    （暗物質）的譯名被貼到<b>同名的盔甲</b>上，`Charred Bone`（焦黑的骨）
    貼到<b>同名的武器</b>上——裝備名稱是刻意保留英文的。順帶一提，這也讓
    F6 的「翻譯物品名稱」開關對那些檔案<b>無聲失效</b>，因為 terms 那條路
    不受開關管。

    <h2>為什麼用白名單而不是自動判斷</h2>
    「這個名稱會不會出現在別的敘述裡」機器判斷不了——它是語意問題。
    白名單短、而且加新檔案時會得到一則說得很清楚的錯誤，
    比讓它靜靜地開始覆蓋別人好。
    """
    # 只有這些檔案的名稱該被當成可替換的詞。
    allowed = {"major-id.json"}
    allowed_dirs = {"ability"}

    out: list[Problem] = []
    for path in files:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:                                     # noqa: BLE001
            continue
        if not isinstance(data, dict):
            continue
        meta = data.get("_meta")
        if not isinstance(meta, dict) or meta.get("itemNames") is not False:
            continue
        entries = data.get("entries")
        names = 0
        if isinstance(entries, dict):
            names = sum(1 for v in entries.values()
                        if isinstance(v, dict) and v.get("role") == "name"
                        and (v.get("dst") or "").strip())
        if names == 0:
            continue                       # 沒有名稱條目，這個旗標不影響任何事
        if path.name in allowed or path.parent.name in allowed_dirs:
            continue
        out.append(Problem("error", path.name, "_meta.itemNames",
                           f"標成 false 會讓這個檔的 {names} 個名稱變成"
                           f"「可替換的詞」，塞進任何含有同樣字串的文字裡。"
                           f"這是給技能與 Major ID 用的；道具檔請改成 true"))
    return out


def check_gear_name_switch(files: list[Path]) -> list[Problem]:
    """有名稱條目的檔案要表態：受不受 F6「翻譯物品名稱」開關管。

    <h2>那個開關是為誰設的</h2>
    只為<b>裝備</b>——裝備名稱保持英文才對得上 wiki、交易市場與社群討論。
    素材、材料、典籍、面向、護符跟交易市場無關，被它關掉沒有道理。

    <h2>為什麼要擋</h2>
    開關<b>預設是關的</b>，所以標錯的後果是「譯文靜靜地不見」：
    玩家打開素材袋，標題與說明都是中文，九個素材名稱全是英文，
    看起來就像翻譯憑空消失，沒有任何錯誤訊息可循。v1.99.73 就是這樣。

    <p>所以規則寫死成兩邊都要明說：`gear-*.json` 不可以標 false，
    其餘有名稱的道具檔<b>必須</b>標 false。新增一個道具檔忘了標，
    會在這裡得到一則講清楚的錯誤，而不是在遊戲裡少一半譯文。
    """
    out: list[Problem] = []
    for path in files:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:                                     # noqa: BLE001
            continue
        if not isinstance(data, dict):
            continue
        meta = data.get("_meta")
        meta = meta if isinstance(meta, dict) else {}
        if meta.get("itemNames") is False:
            continue                       # 走 terms 那條路，這個開關管不到
        entries = data.get("entries")
        if not isinstance(entries, dict):
            continue
        names = sum(1 for v in entries.values()
                    if isinstance(v, dict) and v.get("role") == "name"
                    and (v.get("dst") or "").strip())
        if names == 0:
            continue                       # 沒有名稱條目，這個旗標不影響任何事

        gear = path.name.startswith("gear-")
        flag = meta.get("gearNames")
        if gear and flag is False:
            out.append(Problem("error", path.name, "_meta.gearNames",
                               "裝備檔不可以標成 false —— 那會讓「翻譯物品名稱」"
                               "開關對它失效，裝備名稱是刻意保留英文的"))
        elif not gear and flag is not False:
            out.append(Problem("error", path.name, "_meta.gearNames",
                               f"這個檔有 {names} 個名稱，但沒有標 gearNames: false，"
                               f"於是它們會跟裝備一起被「翻譯物品名稱」開關關掉——"
                               f"而那個開關預設就是關的。不是裝備的話請補上 false"))
    return out


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


def check_misfiled(files: list[Path]) -> list[Problem]:
    """有歸屬的台詞卻放在 quest.json 裡。

    ``quest.json`` 的 ``_note`` 自己就寫了：「還沒歸到任務的 NPC 台詞。
    有歸屬的請放 quest/ 底下對應的任務檔」。同一句同時存在於兩邊時，
    載入順序決定誰生效——就算現在兩邊譯法一樣，只要有人只改其中一邊，
    另一邊就會無聲地贏回去，而畫面上完全看不出原因。

    ``check_duplicates`` 只在譯法<b>不同</b>時報錯，抓不到這種「暫時一致」的
    重複，所以另外擋在這裡：只要重複就報，不管譯法一不一樣。
    """
    owned: dict[str, str] = {}
    for file in files:
        if file.parent.name != "quest":
            continue
        try:
            data = json.loads(file.read_text(encoding="utf-8"))
        except Exception:
            continue
        for row in (data.get("entries") or {}).values():
            if row.get("src"):
                owned.setdefault(row["src"], file.name)

    out: list[Problem] = []
    for file in files:
        if file.name != "quest.json":
            continue
        try:
            data = json.loads(file.read_text(encoding="utf-8"))
        except Exception:
            continue
        rows = data["entries"] if isinstance(data.get("entries"), dict) else data
        for key in rows:
            if key.startswith("_") or key not in owned:
                continue
            out.append(Problem("error", file.name, key,
                               f"這句話已經歸給 quest/{owned[key]} 了。"
                               f"兩邊都有的話，載入順序決定誰生效，"
                               f"改到輸的那一份會沒反應——請從 quest.json 移除"))
    return out


def stray_flat_files() -> list[Path]:
    """語言分層之前的舊路徑上還有沒有檔案。

    譯文按語言分層之後，所有語料都在 ``translations/<語言>/`` 底下。
    但貢獻者的 fork 常常停在分層之前，於是 PR 改的是
    ``translations/quest/xxx.json`` 這種舊路徑——GitHub 只會顯示
    「conflicting」，不會說原因，於是同一個坑一再有人踩進去。

    這裡直接把它變成一條看得懂的錯誤：檔案放錯地方了，搬到 zh_tw/ 底下。
    """
    stray = sorted(LANG_ROOT.glob("*.json"))
    stray += sorted(LANG_ROOT.glob("quest/*.json"))
    stray += sorted(LANG_ROOT.glob("ability/*.json"))
    return [f for f in stray if not f.name.startswith("_")]


def languages() -> list[Path]:
    """有哪些語言資料夾。沒有分層時（舊版）就回傳根目錄本身。

    只認 ``xx_yy`` 這種語言代碼。先前是「不是底線開頭的資料夾都算」，
    於是有人把檔案放回舊的 ``translations/quest/`` 時，那個資料夾會被
    當成一種語言拿去跑一輪檢查——輸出多一段 ``── quest ──``，
    看起來像是真的有這個語言。
    """
    dirs = sorted(d for d in LANG_ROOT.iterdir()
                  if d.is_dir() and LANG_CODE.fullmatch(d.name))
    return dirs or [LANG_ROOT]


def collect(base: Path) -> list[Path]:
    files = sorted(f for f in base.glob("*.json") if not f.name.startswith("_"))
    # 任務對話一個任務一個檔，放在子資料夾裡
    files += sorted(base.glob("quest/*.json"))
    # 技能樹也是一職業一個檔。先前<b>整個沒被檢查</b>——而它正好是改動最頻繁
    # 的一批。實測撈出兩條 {#} 數量不符的，那種條目在遊戲裡是<b>整條不顯示</b>，
    # 畫面上跟「還沒翻」長得一模一樣，沒有人會發現。
    files += sorted(base.glob("ability/*.json"))
    return [f for f in files if not is_generated(f)]


def main(argv: list[str]) -> int:
    places = load_places()
    glossary = load_glossary()

    if argv:
        files = [TRANSLATIONS / a for a in argv]
        missing = [f for f in files if not f.is_file()]
        if missing:
            print("找不到：" + ", ".join(m.name for m in missing))
            return 2
        groups = [(TRANSLATIONS.name, files)]
    else:
        groups = [(base.name, collect(base)) for base in languages()]

    errors = 0
    warnings = 0
    files = []

    # 放在舊路徑上的檔案。先報這個——後面每一條檢查都不會提到它，
    # 而它正是 PR 顯示 conflicting 的真正原因。
    for f in stray_flat_files():
        rel = f.relative_to(ROOT).as_posix()
        print(f"  [錯誤] {rel}")
        print(f"         這是語言分層之前的舊路徑。譯文現在放在 "
              f"translations/<語言>/ 底下，")
        print(f"         請把它搬到 translations/zh_tw/"
              f"{f.relative_to(LANG_ROOT).as_posix()}。")
        errors += 1

    for lang, group in groups:
        # 每一個檔案記著它屬於哪一種語言——長度門檻與對照表都要看語言，
        # 只留檔名的話下面就分不出「這是德文所以比較長」還是「這條翻壞了」。
        files.extend((lang, f) for f in group)
        if len(groups) > 1:
            print()
            print(f"── {lang} ──")
        # 「同一個原文兩種譯法」只在<b>同一種語言之內</b>才是問題
        dupes = (check_duplicates(group) + check_misfiled(group)
                 + check_substitutable_names(group)
                 + check_gear_name_switch(group))
        if dupes:
            print()
            print("跨檔重複")
            for p in dupes:
                print(p)
                errors += 1

    for lang, file in files:
        problems = check_file(file, places, glossary, lang)
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
