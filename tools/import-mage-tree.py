"""把翻譯團隊的 Wynnability mage 匯出檔併進語料。

<h2>來源長什麼樣</h2>
那是 Wynnability 編輯器的匯出檔：89 個技能，每個帶一整塊 `description`——
中文散文夾著<b>仍是英文</b>的數值標籤：

    §6Click Combo: §d§lRIGHT§7-§d§lLEFT§7-§d§lLEFT
    §7從天空中召喚一顆緩慢但強大的隕石，造成大範圍的高傷害。
    §b✺ §7Mana Cost: §f50
    §c⚔ §7Total Damage: §f400% §8(of your DPS)

<h2>為什麼要手工對照</h2>
兩邊沒有任何可靠的機械鍵：

<ul>
<li><b>流水號</b>對不上——前四個吻合，第五、六就互換了
<li><b>樹上位置</b>（`cellMap` 對我們的 `tree`）89 個裡只有 4 個吻合
<li><b>英文錨點</b>不存在——他們的散文 0 條還留著英文
</ul>

<p>所以 {@code NAMES} 是<b>逐條用散文語意核對</b>出來的。例如
「疾襲：增加冰蛇的傷害並使其速度翻倍」對上 `Blitz`；
「熾光」與「耀光」都是「增加奧法尼姆傷害」，靠在樹上的先後順序分辨
`Incandescence` 與 `Gleam`。

<h2>只拿散文，不拿標籤</h2>
他們的數值標籤還是英文（`Mana Cost:`、`Range:`、`Blocks`、`(of your DPS)`），
而我們已經翻好了（魔力消耗、施放範圍、格）。整塊照抄等於把英文放回去，
所以只取<b>散文</b>那幾行。

<h2>多段敘述靠「提到誰」配對</h2>
一個技能有兩段以上敘述時，兩邊的<b>順序與斷句位置都不一樣</b>——「蛇笛」他們是
冰蛇在前、我們是冰凍龍捲風在前；祕法回流他們寫三行、我們是兩條。
照順序配就是張冠李戴。

<p>但每一段都會指名它在講哪個技能，所以改用那個當鑰匙（見 {@code regroup}）。
配不出來的一律不套用，只寫進對照表。

用法：
    python tools/import-mage-tree.py <匯出檔.json>
    python tools/import-mage-tree.py <匯出檔.json> --write
"""

from __future__ import annotations

import io
import json
import pathlib
import re
import sys

BASE = pathlib.Path("src/main/resources/assets/wynnchayuan/translations")
MAGE = BASE / "ability" / "mage.json"
REPORT = pathlib.Path("docs/mage-tree-diff.md")
NL = chr(10)

# 對方的技能 ID -> 我們的英文技能名。逐條用散文語意核對，見檔頭說明。
# 11-15 五個元素精通在 SHARED（六個職業共用，住 shared.json）；
# 78 魔力流轉我們語料裡沒有。
NAMES = {
    1: "Meteor", 2: "Wand Proficiency I", 3: "Cheaper Meteor", 4: "Shooting Star",
    5: "Teleport", 6: "Wand Proficiency II", 7: "Wisdom", 8: "Heal", 9: "Ice Snake",
    10: "Cheaper Teleport", 16: "Distortion", 17: "Thunderstorm", 18: "Sunshower",
    19: "Burning Sigil", 20: "Crashing Comet", 21: "Astral Fragmentation",
    22: "Ophanim", 23: "Arcane Transfer", 24: "Cheaper Heal", 25: "Interweave",
    26: "Displacement", 27: "Purification", 28: "Larger Heal", 29: "Larger Mana Bank",
    30: "Cheaper Ice Snake", 31: "Cheaper Teleport II", 32: "Frozen Tornado",
    33: "Fortitude", 34: "Pyrokinesis", 35: "Resilient Light", 36: "Snake Nest",
    37: "Seance", 38: "Warp Blast", 39: "Gospel of Light", 40: "Orphion's Pulse",
    41: "Incandescence", 42: "Arcane Restoration", 43: "Meteor Shower",
    44: "Void Acceleration", 45: "Lightweaver", 46: "Arcane Speed",
    47: "Larger Mana Bank II", 48: "Psychokinesis", 49: "Chaos Explosion",
    50: "Cheaper Meteor II", 51: "Cheaper Ice Snake II", 52: "Crystallize",
    53: "Vacuokinesis", 54: "Dimensional Tear", 55: "Sentient Snake", 56: "Augury",
    57: "Searing Light", 58: "Arcane Power", 59: "Rift Rupture",
    60: "Everlasting Light", 61: "Larger Mana Bank III", 62: "Etheric Slash",
    63: "Frigid Grasp", 64: "Time Dilation", 65: "Divination", 66: "Sunflare",
    67: "Halo", 68: "Arcane Overflow", 69: "Memory Recollection", 70: "Manastorm",
    71: "Cheaper Heal II", 72: "Freezing Sigil", 73: "Arctic Snake", 74: "Gleam",
    75: "Accelerated Strike", 76: "Influx Shift", 77: "Devitalize",
    79: "Riftbound", 80: "Judrajim", 81: "Diffraction", 82: "Time Vortex",
    83: "Portal to the Beyond", 84: "Paradox", 85: "Blitz",
    86: "Induced Instability", 87: "Dawn", 88: "Gravitational Collapse",
    89: "Tangled Origin",
}

# 五個元素精通不在 mage.json 裡——六個職業都有，所以放在 shared.json。
# 使用者回報「id 11~15 沒翻譯」，查下去發現那五條的英文原文早就在語料裡了
# （`Increase your base damage from all Water attacks.`），只是 dst 空著。
SHARED = {
    11: "Air Mastery", 12: "Earth Mastery", 13: "Fire Mastery",
    14: "Thunder Mastery", 15: "Water Mastery",
}

COLOUR = re.compile(r"§#[0-9a-fA-F]{8}|§[0-9a-frlonmk]")

# 數值那幾行的開頭。這些我們已經翻好了，不從對方的檔案拿。
STAT = re.compile(
    r"^[^A-Za-z0-9一-鿿]*"
    r"(Click Combo|Mana Cost|Total Damage|Total Heal|Range|Area of Effect|Duration"
    r"|Cooldown|Damage|Earth|Thunder|Water|Fire|Air|Neutral|Effect|Charges"
    r"|Required|Min\.|Blocks|Main Attack|Spell|Heal|Max|Slow|Knockback|Pulse|Drift)")


PLACEHOLDER = "{#}"

# 技能圖示。他們的散文裡是<b>真的字元</b>，我們的原文那個位置是 `{#}`。
# 涵蓋私用區、Wynncraft 用到的符號區段與表情符號。
# 中文標點在 U+3000 區段，不在這裡面，不會被誤換。
ICON = re.compile("[\uE000-\uF8FF\u2190-\u2BFF"
                  "\U0001F300-\U0001FAFF\U000F0000-\U0010FFFF]")

NUMBER = re.compile(r"\d[\d,]*(?:\.\d+)?%?")

SLOT = re.compile(r"\{~\d?\}")


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

    <h2>拿來當配對的鑰匙</h2>
    一個技能有兩段以上敘述時，兩邊的順序不保證一致——「蛇笛」他們是冰蛇在前、
    我們是冰凍龍捲風在前。照順序配就是張冠李戴。

    <p>但每一段都會<b>指名</b>它在講哪個技能：我們寫 {@code Frozen Tornado}、
    他們寫「冰凍龍捲風」。有了中英對照（{@code NAMES} 加上他們的
    {@code _plainname}）就能把兩邊指的同一個技能認出來，順序完全不重要。

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
    兩邊的<b>斷句位置不一樣</b>。`Arcane Transfer` 我們是兩條，他們寫成三行：

    <pre>
      我們 [0] Heal will now transfer the contents of your Mana Bank...
      我們 [1] Meteor, Ice Snake, and Etheric Slash will add +N Mana...
      他們 (1) 隕石、冰蛇和裂界斬命中敵對目標時，魔力儲庫 ✺ 增加 13 點魔力。
      他們 (2) 冰凍龍捲風和流星雨增加 4 點魔力。
      他們 (3) 治癒不再回復血量，但會提取 魔力儲庫 ✺ 的魔力至玩家身上。
    </pre>

    他們的 (1)(2) 合起來才是我們的 [1]，而 (3) 是我們的 [0]——順序還是反的。

    <h2>鑰匙</h2>
    每一段都會<b>指名</b>它在講哪個技能（見 {@link #mentions}）。指名重疊最多的
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
            target = previous               # 沒指名：接在上一行後面
        if target is None:
            continue                        # 開頭就指不出來，交給人看
        out.setdefault(target, []).append(line)
        previous = target
    return out


def parameterised(text: str, want: str) -> str | None:
    """把寫死的數字換回 `{~}`。

    <h2>為什麼非換不可</h2>
    對方的譯文把數字寫死了：`Increase your Max Orbs from Lightweaver by +{~}.`
    他們翻成「織光的光球數量增加 2。」——那個 <b>2</b> 在我們這邊必須是 `{~}`，
    因為技能升級之後遊戲送來的是 3、4、5。

    <p>更糟的是佔位符數量對不上時，重建那一步會<b>整條放棄</b>：
    畫面上不會報錯，只是那條譯文永遠不出現。測試就是這樣抓到的。

    <p>圖示同理：他們的散文裡是<b>真的字元</b>（⚡ ✺ ✨ 💎），我們的原文那個位置
    是 `{#}`。不換的話 `{#}` 的數量也對不上，一樣整條被放棄。

    @param want 我們的原文長什麼樣，用來比對兩種佔位符的數量
    @return 換好的譯文；數量對不上就回傳 {@code null}，交給人看
    """
    fixed = ICON.sub(PLACEHOLDER, NUMBER.sub("{~}", text))
    # 圖示前後常常各留一個空格，換成 {#} 之後會變成兩個，收成一個
    fixed = re.sub(r" +", " ", fixed).strip()
    return fixed if (len(SLOT.findall(fixed)) == len(SLOT.findall(want))
                     and fixed.count(PLACEHOLDER) == want.count(PLACEHOLDER)) else None


def main(argv: list[str]) -> int:
    write = "--write" in argv
    files = [a for a in argv if not a.startswith("--")]
    if not files:
        print(__doc__)
        return 2

    source = json.loads(pathlib.Path(files[0]).read_text(encoding="utf-8"))
    abilities = source["abilities"]
    data = json.loads(MAGE.read_text(encoding="utf-8"))
    rows = data["entries"]
    shared_path = BASE / "ability" / "shared.json"
    shared_data = json.loads(shared_path.read_text(encoding="utf-8"))

    by_ability: dict[str, list[dict]] = {}
    for entry in rows.values():
        if isinstance(entry, dict) and entry.get("ability"):
            by_ability.setdefault(entry["ability"], []).append(entry)
    for entry in shared_data["entries"].values():
        if isinstance(entry, dict) and entry.get("ability"):
            by_ability.setdefault(entry["ability"], []).append(entry)

    # 中英技能名的對照，兩個方向都要：他們的段落寫中文名，我們的原文寫英文名。
    zh_to_en = {abilities[str(a)]["_plainname"]: e
                for a, e in {**NAMES, **SHARED}.items() if str(a) in abilities}
    en_to_en = {e: e for e in list(NAMES.values()) + list(SHARED.values())}

    renamed = 0
    replaced = 0
    review: list[tuple[str, str, str, str]] = []
    missing: list[str] = []

    both = dict(NAMES)
    both.update(SHARED)                    # 元素精通住在 shared.json
    for aid, english in sorted(both.items()):
        their = abilities.get(str(aid))
        ours = by_ability.get(english)
        if their is None or ours is None:
            missing.append(f"{aid} / {english}")
            continue

        # 名稱：整棵樹改用中文，所以 keep 也要跟著拿掉
        for entry in ours:
            if entry.get("role") == "name":
                if entry.get("dst") != their["_plainname"]:
                    entry["dst"] = their["_plainname"]
                    entry.pop("keep", None)
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
                               entry["src"], entry.get("dst", ""),
                               " ⏎ ".join(lines)))
                continue
            joined = NL.join(mine)
            after = parameterised(joined, entry["src"])
            if after is None:
                review.append((english + "（數值對不上，未自動套用）",
                               entry["src"], entry.get("dst", ""), joined))
            elif entry.get("dst") != after:
                review.append((english, entry["src"], entry.get("dst", ""), after))
                entry["dst"] = after
                replaced += 1

    print(f"技能名稱改成中文：{renamed} 條")
    print(f"敘述換成對方的版本：{replaced} 條")
    print(f"需要人看的（多段或對不上）：{len(review) - replaced} 條")
    if missing:
        print(f"對照表列了但找不到的：{missing}")

    if write:
        MAGE.write_text(json.dumps(data, ensure_ascii=False, indent=1) + NL,
                        encoding="utf-8", newline=NL)
        shared_path.write_text(json.dumps(shared_data, ensure_ascii=False, indent=1) + NL,
                               encoding="utf-8", newline=NL)
        REPORT.parent.mkdir(parents=True, exist_ok=True)
        with io.open(REPORT, "w", encoding="utf-8", newline=NL) as out:
            out.write("# mage 技能樹：與翻譯團隊匯出檔的差異" + NL + NL)
            out.write("由 `tools/import-mage-tree.py` 產生。" + NL + NL)
            out.write("「多段」那些<b>沒有</b>自動套用——兩邊的段落順序不保證一致，"
                      "猜錯就是張冠李戴，請人工挑。" + NL + NL)
            for name, src, before, after in review:
                out.write(f"## {name}" + NL + NL)
                out.write(f"- 原文：`{src}`" + NL)
                out.write(f"- 舊譯：{before or '（空）'}" + NL)
                out.write(f"- 新譯：{after}" + NL + NL)
        print(f"對照表寫到 {REPORT}")
    else:
        print("（預覽，加 --write 才寫回）")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
