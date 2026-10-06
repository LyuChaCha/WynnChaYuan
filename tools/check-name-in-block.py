#!/usr/bin/env python3
"""名字在整段句子裡被另外翻了一次。

為什麼需要
----------
``npc.json`` 收的是名牌上的名字——玩家在怪或 NPC 頭上看到的那個。同一個名字
也會出現在任務目標、告示、聊天那些<b>整段</b>條目的句子裡，而翻整句的時候很
容易順手另外造一個名字：名牌寫「獄警」、任務對話寫「監獄守衛」、另一句又寫
「獄卒」。玩家會以為是三個人。

這一類 ``validate.py`` 的 ``check_duplicates`` 看不到（兩邊的 src 本來就不同，
一個是名字、一個是一整句），``check-collisions.py`` 也不是這一類（它管的是
「兩個不同的原文共用一個譯名」）。

判準
----
整段的 src 含某個名牌名字（帶詞界，複數與所有格算同一個）
→ 那一段的 dst 就該含該名字的譯文。中日韓要求整串出現；俄西有變格與陰陽性，
所以砍掉每個詞的詞尾兩個字母當詞幹再比。

哪些要修、哪些不要
------------------
原則是<b>以名牌為準</b>——#1036 對對話前綴做過同一件事。但報表裡有幾類是
合法的，不要照著改：

* <b>語序</b>：名牌「廚師 Hamsey」、句子裡「Hamsey 廚師」。兩種中文都通順，
  韓文更是整族都把稱謂放在名字後面（``Cob 이등병``）。硬換還會把拉丁字母
  兩側的空白弄壞。
* <b>助詞與冠詞</b>：「Corkus 的代表」、俄文的 ``тюремной стражи``。
* <b>省略</b>：日文「キャラバンを降りて…御者と話す」前面已經講過キャラバン。
* <b>src 是另一個名字</b>：``Magical Book Merchant`` 不是 ``Book Merchant``。
* <b>刻意留英文</b>：任務道具名與地區名有自己的慣例，報表會單獨標出來。

所以這支是<b>人看的報表</b>，永遠回傳 0，也不進 CI——合法的例外不可能窮舉。

用法
----
    python tools/check-name-in-block.py ja_jp            # 一列一列看
    python tools/check-name-in-block.py ja_jp --summary  # 一個名字一段，看全貌
"""

from __future__ import annotations

import collections
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"

LANGS = ["zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es"]

# 名字夠長才收：短的、或本身是普通詞的，撈到的幾乎都不是那個 NPC
MIN_LEN = 8

# 當名字用但也是普通詞的，整批跳過
COMMON = {
    "Guard", "Miner", "Merchant", "Villager", "Soldier", "Captain", "Scout",
    "Farmer", "Blacksmith", "Sailor", "Fisherman", "Hunter", "Thief", "Mage",
    "Archer", "Warrior", "Assassin", "Shaman", "Zombie", "Skeleton", "Spider",
    "Slime", "Creeper", "Wolf", "Bat", "Pig", "Cow", "Sheep", "Chicken",
    "Recommended", "Chapter", "Adventurer",
}

CJK = re.compile(r"[぀-ヿ㐀-鿿가-힯]")


def rows(path: Path) -> list[tuple[str, str]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = data.get("entries")
    if isinstance(entries, dict):
        return [(v.get("src", "") or "", v.get("dst", "") or "")
                for v in entries.values() if isinstance(v, dict)]
    return [(k, v) for k, v in data.items()
            if not k.startswith("_") and isinstance(v, str)]


def stems(text: str) -> list[str]:
    """俄文會變格、西文有陰陽性：砍掉詞尾兩個字母當詞幹（至少留三個）。"""
    out = []
    for word in re.split(r"[\s\-]+", text):
        word = word.strip(".,:;!?()[]'’").lower()
        if len(word) < 3:
            continue
        out.append(word if CJK.search(word) else word[:max(3, len(word) - 2)])
    return out


def plate_names(lang: str) -> dict[str, str]:
    """名牌上的名字：npc.json 裡單行、沒有佔位符、看起來是名字的那些鍵。"""
    out = {}
    for src, dst in rows(TRANSLATIONS / lang / "npc.json"):
        if ("\n" in src or "{" in src or not dst or src == dst
                or len(src) < MIN_LEN or src in COMMON):
            continue
        # 逗號結尾或句中帶逗號的「名字」其實是折行片段
        if "," in src or src[-1] in ".!?:":
            continue
        # 要嘛兩個詞以上，要嘛是一個夠長的專有名詞
        if len(src.split()) < 2 and not (len(src) >= 7 and src[0].isupper()):
            continue
        out[src] = dst
    return out


def scan(lang: str):
    names = plate_names(lang)
    # 複數與所有格算同一個名字：「The Royal Guards escort you」、
    # 「the Scroll Merchant's residence」
    bound = {n: re.compile(r"(?<![A-Za-z0-9_'])" + re.escape(n)
             + r"(?:'s|s)?(?![A-Za-z0-9_])")
             for n in names}

    hits = []
    tally = {n: collections.defaultdict(collections.Counter) for n in names}
    for path in sorted((TRANSLATIONS / lang).rglob("*.json")):
        if path.name.startswith("_"):
            continue
        rel = str(path.relative_to(TRANSLATIONS / lang)).replace("\\", "/")
        for src, dst in rows(path):
            if not dst:
                continue
            for name, trans in names.items():
                if name not in src or not bound[name].search(src):
                    continue
                if name in dst:
                    tally[name]["（留英文）"][rel] += 1
                elif CJK.search(trans):
                    tally[name][trans if trans in dst else "？"][rel] += 1
                elif all(s in dst.lower() for s in stems(trans)):
                    tally[name][trans][rel] += 1
                else:
                    tally[name]["？"][rel] += 1
                if rel == "npc.json" or len(src) < 30:
                    continue
                if (name in dst
                        or (CJK.search(trans) and trans in dst)
                        or (not CJK.search(trans)
                            and all(s in dst.lower() for s in stems(trans)))):
                    continue
                hits.append((rel, name, trans, src, dst))
    return names, hits, tally


def report_rows(lang: str, hits) -> None:
    seen = set()
    print(f"# {lang}：{len(hits)} 處\n")
    for rel, name, trans, src, dst in hits:
        if (name, rel) in seen:
            continue
        seen.add((name, rel))
        print(f"[{rel}] {name} -> 名牌是「{trans}」")
        print(f"   原文 {src[:120]!r}")
        print(f"   譯文 {dst[:120]!r}\n")


def report_summary(lang: str, names, tally) -> None:
    out = [(n, names[n], v) for n, v in tally.items()
           if len({k for k in v if k != "？"}) > 1 or "？" in v]
    print(f"# {lang}：{len(out)} 個名字有兩種以上寫法\n")
    for name, trans, variants in sorted(out):
        total = {k: sum(v.values()) for k, v in variants.items()}
        print(f"{name}   名牌「{trans}」")
        for key in sorted(total, key=lambda x: -total[x]):
            mark = "  <- 名牌" if key == trans else ""
            where = "、".join(f"{f}×{n}"
                              for f, n in variants[key].most_common(4))
            print(f"   {total[key]:>3}  {key:<22} {where}{mark}")
        print()


def main(argv: list[str]) -> int:
    summary = "--summary" in argv
    langs = [a for a in argv if a in LANGS] or LANGS
    for lang in langs:
        names, hits, tally = scan(lang)
        if summary:
            report_summary(lang, names, tally)
        else:
            report_rows(lang, hits)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
