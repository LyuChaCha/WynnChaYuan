"""照 `_raw` 校正被寫壞的 `{#}` 模板。

<h2>壞在哪裡</h2>
語料裡有一批條目的 `src` 跟遊戲實際送出的模板對不起來：

    _raw : Nearby Bleeding ␣␣ enemies will take
    src  : Nearby Bleeding {#}{#}enemies will take
    應為 : Nearby Bleeding {#} enemies will take

一個符號變成<b>兩個</b>佔位符，前後的空格還被吃掉一個。翻譯團隊回報的
「shaman 的 ability tree 好像有符號漏洞」就是這個。

<p>兩個後果：查表永遠落空（鍵跟遊戲算出來的不一樣），而且譯者看到
`{#}{#}` 會以為真的有兩個符號，照著抄進譯文——就算哪天 `src` 修好了，
譯文也跟著錯。

<h2>怎麼判斷哪個是對的</h2>
遊戲端的規則很單純：<b>一段連續的符號碼位就是一個 `{#}`</b>，其餘字元
（含空格）原樣保留。`_raw` 是收集當下的原始文字，照它重算就是正解。

<h2>為什麼不整份重算</h2>
`_raw` 是純文字，沒有字型資訊。而遊戲端判斷「這是不是符號」靠的是字型——
`{#}{#}{#}{#}{#}Az Rune` 那種前綴是<b>五個不同字型</b>的符號各自成段，
從純文字看不出來，重算會把它們併成一個。

<p>所以只修「碼位規則算得出來、而且差異僅止於 `{#}` 的重複與相鄰空格」
的條目。其餘一律不動，只報告。改壞語料比留著壞條目糟得多。

用法：
    python tools/fix-glyph-templates.py
    python tools/fix-glyph-templates.py --write
"""

from __future__ import annotations

import io
import json
import pathlib
import re
import sys

skipped = 0

BASE = pathlib.Path("src/main/resources/assets/wynnchayuan/translations")
NL = chr(10)

PLACEHOLDER = "{#}"

# 私用區與 Wynncraft 的排版平面。與 GlyphSplitter.isGlyphCodePoint 同一套範圍。
GLYPH_RANGES = ((0xE000, 0xF8FF), (0xF0000, 0x10FFFF), (0xCF000, 0xD1000))


def is_glyph(ch: str) -> bool:
    cp = ord(ch)
    return any(low <= cp <= high for low, high in GLYPH_RANGES)


def expected(raw: str) -> str:
    """把 `_raw` 換成模板：<b>一個符號碼位一個 `{#}`</b>，其餘（含空格）原樣。

    <h2>為什麼是一個碼位一個，不是一段一個</h2>
    技能敘述裡連著出現的符號是<b>元素圖示</b>，每一個顏色都不同——顏色不同就是
    不同的樣式段，遊戲端會各給一個 `{#}`：

        _raw  (✤✦✹✽❋ Damage: +45%)
        遊戲   {#}{#}{#}{#}{#} Damage: {~}     ← 五個圖示、五個佔位符、空格是空格

    <p>（外框那種同色連續符號遊戲端會併成一個，但那只出現在遊戲內收集的資料裡，
    這裡處理的是 CDN 匯入的技能敘述，不會有。）

    <p>空格<b>永遠不變成 `{#}`</b>——那正是要修的毛病。
    """
    return "".join(PLACEHOLDER if is_glyph(ch) else ch for ch in raw)


def skeleton(text: str) -> str:
    """只留下「非佔位符」的骨架，用來確認兩邊講的是同一句話。

    把每一段連續的 `{#}` 壓成一個記號、空白壓成一格。這樣比對就只在乎
    「文字內容有沒有變」，不在乎符號被寫成幾個、空格是不是被吃掉——
    而那兩件事正是要修的。
    """
    marked = re.sub(r"(?:\{#\})+", "\x00", text)
    marked = re.sub(r"\s+", " ", marked)
    # 記號<b>旁邊</b>的空格也要一起抹掉。被吃掉的正是那些空格，
    # 留著比對的話每一條都會被判成「講的不是同一句話」而跳過。
    return re.sub(r" ?\x00 ?", "\x00", marked).strip()


def words(text: str) -> str:
    """只留下文字本身：拿掉所有 `{#}`，空白壓成一格。

    <p>{@link skeleton} 把 `{#}` 壓成一個記號，那是為了「符號位置有沒有跑掉」；
    這裡相反——要比的是<b>兩邊講的是不是同一句話</b>，符號有幾個正是待修的部分，
    不能拿來當比對條件。
    """
    return re.sub(r"\s+", " ", text.replace(PLACEHOLDER, " ")).strip()


def numbers(text: str) -> int:
    return len(re.findall(r"\{~\d?\}", text))


def multi_glyph_run(raw: str) -> bool:
    """`_raw` 裡有沒有<b>連續兩個以上</b>的符號碼位。

    有的話這一條不能碰。遊戲端是<b>一個字型段一個 `{#}`</b>，
    `{#}{#}{#}{#}{#}Az Rune` 那種前綴就是五個不同字型的符號各自成段——
    從純文字看不出字型，重算會把它們併成一個，那是把對的改成錯的。

    <p>要修的那批長相很單純：一個符號、前後是空格。限縮在這個範圍內，
    誤傷的機會就沒有了。
    """
    i = 0
    while i < len(raw):
        if is_glyph(raw[i]):
            start = i
            while i < len(raw) and is_glyph(raw[i]):
                i += 1
            if i - start > 1:
                return True
        else:
            i += 1
    return False


# 千分位與小數的分隔符<b>後面必須還有數字</b>。先前寫成 `[\d,]*`，
# 於是 `(Max 4, 0.1s Cooldown)` 裡的 `4,` 連逗號一起被當成數字吃掉——
# 骨架比對時那個逗號憑空消失，兩邊對不起來，整條就被判成「講的不是
# 同一句話」而跳過。`Marked` 那條技能敘述就是這樣卡住的。
NUMBER = re.compile(r"\d+(?:[,.]\d+)*%?")

NUMBER_SLOT = "{~}"

# 站在<b>空格位置</b>的 `{#}`：<b>前後都直接貼著字</b>，沒有任何空白。
# 那正是被寫錯的那一個——原文那裡是一個空格。
#
# 先前只認「前面是佔位符右大括號」的情況，於是 `Tear{#}會過載`、
# `Lightweaver{#}命中` 這種（前面是英文字尾）修不到，整條就被跳過——
# 而那正是使用者回報「翻了卻沒出現」的那幾條。
#
# `({#})` 不會被誤判：左括號不在前置字元的範圍裡。
SPACE_SLOT = re.compile(r"(?<=[\w一-鿿}])\{#\}(?=[\w一-鿿])")

# 同一個錯，但後面接的是<b>全形標點</b>：`Mark {#}{#}。`
#
# 中文句子常常在符號之後就直接收句，那個位置原文是一個空格，譯者照著壞掉的
# `src` 抄成了第二個 `{#}`。上面那條規則的向前查看只認文字，接標點的認不出來，
# 於是整條被判成「改不動」而跳過——`Marked` 那條技能敘述就是這樣卡了兩輪。
#
# 這裡換成<b>整個拿掉</b>而不是換成空格：全形標點自帶左側留白，
# 補一個半形空格反而會變成「{#} 。」。
PUNCT_SLOT = re.compile(r"(?<=[\w一-鿿}])\{#\}(?=[，。、；：！？）」』】])")


def fix_dst(dst: str, want_count: int) -> str | None:
    """把譯文裡跟著抄錯的 `{#}` 還原成空格。

    <p>譯者是照 `src` 抄的：`src` 錯，譯文就跟著錯。只改 `src` 不改譯文的話，
    佔位符數量對不上，重建時整條會被放棄——那條譯文等於白翻。

    @return 修好的譯文；對不齊就回傳 None，交給人看
    """
    fixed = dst
    if fixed.startswith(PLACEHOLDER):
        # 開頭那串多出來的收成一個，並把原文本來就有的空格補回去——
        # 少了它畫面上會變成「圖示緊貼著中文」，跟原文的 `{#} Effect:` 對不上。
        fixed = re.sub(r"^(?:\{#\})+\s*", PLACEHOLDER + " ", fixed)
    fixed = SPACE_SLOT.sub(" ", fixed)
    if fixed.count(PLACEHOLDER) != want_count:
        fixed = PUNCT_SLOT.sub("", fixed)
    return fixed if fixed.count(PLACEHOLDER) == want_count else None


def repair(entry: dict) -> str | None:
    """這一條該改成什麼；不該動就回傳 None。

    <h2>做法：照 `_raw` 整條重算</h2>
    `_raw` 是收集當下的原始文字，符號位置與空格<b>本來就是對的</b>。
    所以不去跟壞掉的 `src` 逐段對齊——那會被多出來的 `{#}` 卡住，
    而多出來的 `{#}` 正是要修的東西：

        _raw  You gain +5 Distortion ✹ for every enemy weakened.
        重算  You gain +{~} Distortion {#} for every enemy weakened.
        舊的  You gain +{~}{#}Distortion {#} for every enemy weakened.
                            ^^^^ 這個其實是一個空格

    <p>先比骨架確認兩邊講的是同一句話，重算之後再比數值佔位符的數量——
    數量對得上，就表示重算的結果跟語料原本的認知一致。
    """
    raw = entry.get("_raw")
    src = entry.get("src")
    if not raw or not src or PLACEHOLDER not in src:
        return None
    if "{p}" in src or "{u}" in src:
        return None                     # 地名與玩家名不在這支的處理範圍

    # `_raw` 沒有參數化，`src` 有。先把兩邊的數值都換成同一個記號再比骨架，
    # 否則 `+{~}s` 跟 `+6s` 永遠對不起來。正負號<b>不</b>算進去——
    # 小數點也只有後面接數字時才算，否則 `+2.` 會把句末的句號一起吃掉。
    # 遊戲端的 {~} 不含符號（`+{~}`），連符號一起吃掉兩邊就對不齊了。
    #
    # 比的時候把 `{#}` <b>整個拿掉</b>，不是壓成一個記號。多出來的那個 `{#}`
    # 正是要修的東西，留著比對只會讓每一條都被判成「講的不是同一句話」。
    # 內容一樣、數值個數也一樣，就足以確認重算的結果沒有跑掉。
    want = expected(raw)
    if words(re.sub(r"\{~\d?\}", "\x01", src)) != words(NUMBER.sub("\x01", want)):
        return None                     # 講的不是同一句話，別碰

    fixed = NUMBER.sub(NUMBER_SLOT, want)
    if fixed.count(PLACEHOLDER) > src.count(PLACEHOLDER):
        return None                     # 只拿掉多餘的，絕不無中生有
    if fixed == src or numbers(fixed) != numbers(src):
        return None
    return fixed


def main(argv: list[str]) -> int:
    write = "--write" in argv
    global skipped
    changed = 0
    skipped = 0
    for path in sorted(BASE.rglob("*.json")):
        if path.name.startswith("_"):
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        rows = data.get("entries")
        if not isinstance(rows, dict):
            continue
        touched = []
        for key, entry in rows.items():
            if not isinstance(entry, dict):
                continue
            fixed = repair(entry)
            if fixed is None:
                if entry.get("_raw") and re.search(r"(?:\{#\}){2,}", entry.get("src", "")):
                    skipped += 1
                continue
            # 譯文也照抄了那個多出來的 `{#}`。src 改了而 dst 沒改的話，
            # 佔位符數量對不上，整條會在重建時被放棄——比原本更糟。
            dst = entry.get("dst") or ""
            if dst:
                dst = fix_dst(dst, fixed.count(PLACEHOLDER))
                if dst is None:
                    skipped += 1
                    continue            # 譯者放的位置跟原文不一樣，交給人看
                entry["dst"] = dst
            touched.append((key, entry["src"], fixed))
            entry["src"] = fixed
            if entry.get("flat"):
                entry["flat"] = fixed.replace(NL, " ")
        if not touched:
            continue
        print(f"  {path.relative_to(BASE)}  {len(touched)} 條")
        for key, before, after in touched[:3]:
            print(f"      {before.splitlines()[0][:64]!r}")
            print(f"   -> {after.splitlines()[0][:64]!r}")
        changed += len(touched)
        if write:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + NL,
                            encoding="utf-8", newline=NL)

    print()
    print(f"校正 {changed} 條，跳過 {skipped} 條（符號結構對不上，要人看）"
          + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
