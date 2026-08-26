"""把翻譯團隊的 Wynnability 匯出檔併進語料。六個職業通用。

<h2>來源長什麼樣</h2>
Wynnability 編輯器的匯出檔，每個技能一整塊 `description`——中文散文夾著
<b>仍是英文</b>的數值標籤：

    §6Click Combo: §d§lRIGHT§7-§d§lLEFT§7-§d§lLEFT
    §7從天空中召喚一顆緩慢但強大的隕石，造成大範圍的高傷害。
    §b✺ §7Mana Cost: §f50
    §c⚔ §7Total Damage: §f400% §8(of your DPS)

<h2>怎麼跟我們的語料對起來</h2>
靠<b>技能在樹上的位置</b>。他們的 `cellMap` 是格子索引，我們的每一條有 `tree`
（`01/mage/01/000/004` = 頁/列/欄）。換算規則見 {@link cell_to_tree}。

<p>兩個容易踩到的地方：<b>頁碼</b>與<b>欄位換行</b>。少算頁碼會讓不同頁的技能
擠在一起（我第一次就是這樣，89 個只配到 4 個）；`idx % 9` 為 0 時算出來的欄是
-1，其實是<b>上一列的最後一欄</b>，漏了這個會有 8 個配不到。

<p>這把鑰匙拿 mage 驗證過：83 個技能全部配到，而且與另外<b>獨立</b>用散文語意
逐條對照的結果<b>完全一致</b>（0 個分歧）。兩種互不相干的方法得到同一個答案，
才敢拿它自動處理其餘職業。

<h2>只拿散文，不拿標籤</h2>
他們的數值標籤還是英文（`Mana Cost:`、`Range:`、`Blocks`），而我們已經翻好了
（魔力消耗、施放範圍、格）。整塊照抄等於把英文放回去，所以只取<b>散文</b>那幾行。

<h2>數字與圖示要換回佔位符</h2>
他們把數字寫死了（`+{~}` 翻成「增加 2」），圖示則是<b>真的字元</b>（⚡ ✺ 💎），
而我們的原文那個位置是 `{#}`。<b>任一種佔位符數量對不上，重建時整條會被放棄</b>
——畫面上不報錯，那條譯文永遠不出現。換完仍對不上的一律不套用。

<h2>多段敘述靠「提到誰」配對</h2>
兩邊的順序與斷句位置都不一樣：`Arcane Transfer` 他們寫三行、我們是兩條，
而且順序是反的。但每一段都會<b>指名</b>它在講哪個技能，所以用那個當鑰匙
（見 {@link regroup}），順序就完全不重要了。

用法：
    python tools/import-ability-tree.py <匯出檔.json>
    python tools/import-ability-tree.py <匯出檔.json> --write
"""

from __future__ import annotations

import io
import json
import pathlib
import re
import sys

BASE = pathlib.Path("src/main/resources/assets/wynnchayuan/translations/zh_tw")
NL = chr(10)

# 技能樹一列有幾欄。Wynncraft 六個職業都一樣。
COLUMNS = 9

PLACEHOLDER = "{#}"

COLOUR = re.compile(r"§#[0-9a-fA-F]{8}|§[0-9a-frlonmk]")

# 數值那幾行的開頭。這些我們已經翻好了，不從對方的檔案拿。
STAT = re.compile(
    r"^[^A-Za-z0-9一-鿿]*"
    r"(Click Combo|Mana Cost|Total Damage|Total Heal|Range|Area of Effect|Duration"
    r"|Cooldown|Damage|Earth|Thunder|Water|Fire|Air|Neutral|Effect|Charges|Vision"
    r"|Required|Min\.|Blocks|Main Attack|Spell|Heal|Max|Slow|Knockback|Pulse|Drift)")

# 技能圖示。他們的散文裡是真的字元，我們的原文那個位置是 `{#}`。
# 中文標點在 U+3000 區段，不在這裡面，不會被誤換。
ICON = re.compile("[-←-⯿"
                  "\U0001F300-\U0001FAFF\U000F0000-\U0010FFFF]")

NUMBER = re.compile(r"\d[\d,]*(?:\.\d+)?%?")

SLOT = re.compile(r"\{~\d?\}")


def cell_to_tree(index: int, rows_per_page: int) -> tuple[int, int, int]:
    """格子索引換算成（頁, 列, 欄）。

    <p>格子是<b>整棵樹連續編號</b>的，所以頁碼要自己除出來。而每一列的第 0 格
    在我們的座標裡是<b>上一列的最後一欄</b>——那是兩邊起算點差一格造成的，
    漏掉它 mage 會有 8 個技能配不到。
    """
    per_page = COLUMNS * rows_per_page
    page = index // per_page + 1
    row = (index % per_page) // COLUMNS
    column = index % COLUMNS - 1
    if column < 0:
        column = COLUMNS - 1
        row -= 1
    if row < 0:
        page -= 1
        row = rows_per_page - 1
    return page, row, column


def prose(description: str) -> list[str]:
    """只留散文，丟掉數值那幾行。"""
    out = []
    for line in description.split(NL):
        text = COLOUR.sub("", line).strip()
        if text and not STAT.match(text) and not text.startswith("("):
            out.append(text)
    return out


def mentions(text: str, vocabulary: dict[str, str]) -> frozenset[str]:
    """這一段提到了哪幾個技能。

    <p>比對從<b>長的名字先試</b>：「節約·冰蛇」含有「冰蛇」，先比短的會配錯。
    """
    found = set()
    for name in sorted(vocabulary, key=len, reverse=True):
        if name and name in text:
            found.add(vocabulary[name])
    return frozenset(found)


def regroup(lines: list[str], descs: list[dict],
            zh_to_en: dict[str, str], en_to_en: dict[str, str]) -> dict[int, list[str]]:
    """把對方的每一行歸到我們的哪一條敘述底下。

    <h2>為什麼不能照順序一對一</h2>
    兩邊的<b>斷句位置不一樣</b>。`Arcane Transfer` 我們是兩條，他們寫成三行，
    而且他們的第三行對應我們的第一條——順序是反的。

    <h2>鑰匙</h2>
    每一段都會<b>指名</b>它在講哪個技能（見 {@link mentions}）。指名重疊最多的
    就是對應的那一條；沒指名的接在<b>上一行</b>後面，因為那是同一句話的下半截。
    """
    wanted = [mentions(entry["src"], en_to_en) for entry in descs]
    out: dict[int, list[str]] = {}
    previous = 0 if len(descs) == 1 else None
    for line in lines:
        found = mentions(line, zh_to_en)
        target = None
        if found:
            best = 0
            for index, want in enumerate(wanted):
                overlap = len(found & want)
                if overlap > best:
                    best, target = overlap, index
        if target is None:
            target = previous
        if target is None:
            continue
        out.setdefault(target, []).append(line)
        previous = target
    return out


def parameterised(text: str, want: str) -> str | None:
    """把寫死的數字與圖示換回佔位符。

    @param want 我們的原文，用來比對兩種佔位符的數量
    @return 換好的譯文；數量對不上就回傳 {@code None}，交給人看
    """
    fixed = ICON.sub(PLACEHOLDER, NUMBER.sub("{~}", text))
    # 圖示前後常常各留一個空格，換成 {#} 之後會變成兩個，收成一個
    fixed = re.sub(r" +", " ", fixed).strip()
    return fixed if (len(SLOT.findall(fixed)) == len(SLOT.findall(want))
                     and fixed.count(PLACEHOLDER) == want.count(PLACEHOLDER)) else None


def load(path: pathlib.Path) -> tuple[dict, dict[str, list[dict]]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    index: dict[str, list[dict]] = {}
    for entry in data["entries"].values():
        if isinstance(entry, dict) and entry.get("ability"):
            index.setdefault(entry["ability"], []).append(entry)
    return data, index


def main(argv: list[str]) -> int:
    write = "--write" in argv
    files = [a for a in argv if not a.startswith("--")]
    if not files:
        print(__doc__)
        return 2

    source = json.loads(pathlib.Path(files[0]).read_text(encoding="utf-8"))
    abilities = source["abilities"]
    rows_per_page = source["properties"]["rowsPerPage"]
    clazz = source["properties"]["classs"]
    print(f"職業：{clazz}")

    class_path = BASE / "ability" / f"{clazz}.json"
    if not class_path.is_file():
        print(f"  ! 找不到 {class_path}")
        return 1
    shared_path = BASE / "ability" / "shared.json"
    class_data, by_ability = load(class_path)
    shared_data, shared_index = load(shared_path)
    # 元素精通那類六個職業共用，住在 shared.json
    for name, entries in shared_index.items():
        by_ability.setdefault(name, entries)

    # 位置 -> 我們的技能名
    at_position: dict[tuple[int, int, int], str] = {}
    for entries in by_ability.values():
        for entry in entries:
            if entry.get("role") == "name" and entry.get("tree"):
                parts = entry["tree"].split("/")
                at_position[(int(parts[2]), int(parts[3]), int(parts[4]))] = \
                    entry["ability"]

    # 對方的技能 -> 我們的技能名
    pairs: dict[str, str] = {}
    for cell, info in source["cellMap"].items():
        aid = info.get("abilityID")
        if not aid:
            continue
        english = at_position.get(cell_to_tree(int(cell), rows_per_page))
        if english:
            pairs[aid] = english
    print(f"  位置配到 {len(pairs)} / 我們有 {len(at_position)} 個技能")

    zh_to_en = {abilities[a]["_plainname"]: e for a, e in pairs.items()}
    en_to_en = {e: e for e in pairs.values()}

    renamed = 0
    replaced = 0
    review: list[tuple[str, str, str, str]] = []

    for aid, english in sorted(pairs.items(), key=lambda kv: int(kv[0])):
        their = abilities[aid]
        ours = by_ability[english]

        for entry in ours:
            if entry.get("role") == "name" and entry.get("dst") != their["_plainname"]:
                entry["dst"] = their["_plainname"]
                entry.pop("keep", None)     # 技能名全面中文化，keep 也跟著拿掉
                renamed += 1

        descs = [e for e in ours if e.get("role") == "desc"]
        lines = prose(their["description"])
        if not descs or not lines:
            continue
        grouped = regroup(lines, descs, zh_to_en, en_to_en)
        for index, entry in enumerate(descs):
            mine = grouped.get(index)
            if not mine:
                review.append((english + "（配不出對應的段落）",
                               entry["src"], entry.get("dst", ""), " ⏎ ".join(lines)))
                continue
            joined = NL.join(mine)
            after = parameterised(joined, entry["src"])
            if after is None:
                review.append((english + "（佔位符對不上，未自動套用）",
                               entry["src"], entry.get("dst", ""), joined))
            elif entry.get("dst") != after:
                review.append((english, entry["src"], entry.get("dst", ""), after))
                entry["dst"] = after
                replaced += 1

    print(f"  技能名稱改成中文：{renamed} 條")
    print(f"  敘述換成對方的版本：{replaced} 條")
    print(f"  需要人看的：{len(review) - replaced} 條")

    if not write:
        print("（預覽，加 --write 才寫回）")
        return 0

    class_path.write_text(json.dumps(class_data, ensure_ascii=False, indent=1) + NL,
                          encoding="utf-8", newline=NL)
    shared_path.write_text(json.dumps(shared_data, ensure_ascii=False, indent=1) + NL,
                           encoding="utf-8", newline=NL)
    report = pathlib.Path(f"docs/{clazz}-tree-diff.md")
    report.parent.mkdir(parents=True, exist_ok=True)
    with io.open(report, "w", encoding="utf-8", newline=NL) as out:
        out.write(f"# {clazz} 技能樹：與翻譯團隊匯出檔的差異" + NL + NL)
        out.write("由 `tools/import-ability-tree.py` 產生。" + NL + NL)
        out.write("標「未自動套用」的請人工挑——硬套會張冠李戴，"
                  "或是佔位符數量對不上讓整條失效。" + NL + NL)
        for name, src, before, after in review:
            out.write(f"## {name}" + NL + NL)
            out.write(f"- 原文：`{src}`" + NL)
            out.write(f"- 舊譯：{before or '（空）'}" + NL)
            out.write(f"- 新譯：{after}" + NL + NL)
    print(f"  對照表寫到 {report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
