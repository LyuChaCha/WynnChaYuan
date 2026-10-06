#!/usr/bin/env python3
"""撞名報表：同一個譯名對到兩個不同原文，靠六個語言投票決定要不要修。

為什麼需要
----------
validate.py 的 check_duplicates 管的是「同一句原文有兩種譯法」，但反過來
的錯它看不到：兩個<b>不同</b>的原文被譯成同一個名字。橡木和獸人都叫
「オーク」、鯉魚和錦鯉都叫「잉어」——玩家在遊戲裡看到兩個東西同名，
會以為是同一個。

不過「譯名相同」本身不一定是錯。遊戲原文自己就有措辭變體（Notice Board
／Noticeboard、Ye sir／Yessir），這些<b>本來就該</b>共用譯名，報出來只是
雜訊。單看一個語言分不出這兩種情況，所以這裡的判準是<b>跨語言投票</b>：

* 四個以上語言分得開 → 這是兩個不同的東西，撞在一起的語言要修
* 四個以上語言撞在一起 → 這是同一個東西，不要動
* 中間（2、3 個語言分得開）→ 待判，大半是原文變體，偶爾混著真錯

還有一類是維護者已經決定「刻意同譯」的整套性案例（例如 zh_tw 的 Corrupt
整族都譯「腐敗」）。這些列在旁邊的 check-collisions.keep.txt，一行一對，
報表裡歸到「整套」，提醒大家不要單列去改；整套要動的時候整族一起動。

範圍只收名稱檔（npc、label、cave、dungeon、raid、quest-name、
discovery-name、ingredient、material、gear-*）。散文檔（misc、quest/*）
的「撞名」幾乎全是折行片段的誤報，這個判準在那裡不成立。

這支程式是<b>人看的報表</b>，不是硬性守門：合法的例外不可能窮舉，所以
永遠回傳 0，也不進 CI。對照組在 tools/reference/——修前的 79 對與修完
剩下的 26 對（見 issue #1019）。

用法
----
    python tools/check-collisions.py              # 全部名稱檔
    python tools/check-collisions.py npc.json     # 只看某一個檔
"""

from __future__ import annotations

import json
import re
import sys
from collections import Counter
from itertools import combinations
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"

# 「刻意同譯」清單，一行一對：標題A|標題B 為什麼。比對前會再正規化一次，
# 所以寫原始鍵或寫正規化標題都可以。
KEEP_FILE = Path(__file__).with_suffix(".keep.txt")

# 報表上的語言順序。跟 tools/reference/ 的對照組一致，方便 diff。
LANGS = ["zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es"]

# 只收名稱檔。散文檔的撞名幾乎全是折行片段的誤報（misc.json 實測
# 192 對裡可用訊號趨近於零），不在這裡報。
NAME_FILES = [
    "npc.json",
    "label.json",
    "cave.json",
    "dungeon.json",
    "raid.json",
    "quest-name.json",
    "discovery-name.json",
    "ingredient.json",
    "material.json",
    "gear-accessory.json",
    "gear-armour.json",
    "gear-weapon.json",
]

# 鍵分組時用的佔位符：{#}、{~}、{p}、{~1}、{c:gold}……整組拿掉再比。同一個
# 名字帶不帶佔位符只是抓取時機的差別（「Lost Naga」跟「Lost Naga {#}{#}\n{#}」
# 是同一隻），不是兩個東西。
PLACEHOLDER = re.compile(r"\{[^}]*\}")

# 譯文比對時只拿掉「格式」佔位符：顏色與樣式（{#}、{c:gold}、{c1}、{w1}、{/}）。
# {p}、{u}、{~} 要留住——「술 취한 {p} 선원」跟「술 취한 선원」是不同形狀的字串
# （一個帶名字、一個是光名牌），「綠寶石商人 {~}」跟「綠寶石商人」也是（一個帶
# 價格欄）。拿掉它們才會撞的，不是撞名。
FORMAT = re.compile(r"\{#\}|\{/\}|\{[cw][1-9]\}|\{c:[^}]*\}")

# 正規化標題時視為分隔的字元。注意 ❤、☠ 這類符號要留住——
# 「Boar」跟「Boar ❤」是兩個不同的鍵，吃掉就分不開了。
PUNCT = re.compile(r"[-–—'.,!?;:\"'()\[\]/\\~_]")

# 詞尾的 s：this→thi、does→doe。原文的單複數變體（Miner Zombie／
# Zombie Miners）該當同一個東西。前面不是英數字的 s 留著（Captain's →
# captain s，撇號已經先被換成空格了）。
TRAILING_S = re.compile(r"(?<=[a-z0-9])s\b")


def title(key: str) -> str:
    """鍵的正規化。同名不同包裝的鍵（占位符、大小寫、單複數）收斂成同一個。"""
    s = PLACEHOLDER.sub(" ", key.lower())
    s = PUNCT.sub(" ", s)
    s = re.sub(r"\s+", " ", s).strip()
    s = TRAILING_S.sub("", s)
    return re.sub(r"\s+", " ", s).strip()


def worth(key: str) -> bool:
    """這個鍵是不是一個「名字」。純佔位符抓到的殘渣（{~}k{#}{~}k 之類）
    正規化後只剩單字母，不是名字，整把跳過。"""
    return any(len(w) >= 2 or not w.isascii() for w in title(key).split())


def norm_value(value: str) -> str:
    """譯文的比對形：拿掉格式佔位符、壓掉空白。「酒保 {#}{#}」跟「酒保」同，
    但「{p} 銀行員」跟「銀行員」不同——{p} 是字串的一部分。"""
    return re.sub(r"\s+", " ", FORMAT.sub("", value)).strip()


def load_keep(path: Path) -> dict[frozenset[str], str]:
    keep: dict[frozenset[str], str] = {}
    if not path.is_file():
        return keep
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "|" not in line:
            continue
        pair, _, reason = line.partition(" # ")
        a, _, b = pair.partition("|")
        keep[frozenset((title(a), title(b)))] = reason.strip()
    return keep


def run(translations: Path, keep_file: Path, names: list[str]) -> None:
    keep = load_keep(keep_file)

    # 六個語言的名稱檔全部讀進來：data[lang][檔名][鍵] = 譯文
    data: dict[str, dict[str, dict[str, str]]] = {lang: {} for lang in LANGS}
    for lang in LANGS:
        for name in names:
            path = translations / lang / name
            if not path.is_file():
                continue
            try:
                raw = json.loads(path.read_text(encoding="utf-8"))
            except Exception:
                continue
            data[lang][name] = {
                k: v for k, v in raw.items()
                if not k.startswith("_") and isinstance(v, str)
            }

    grand = Counter()
    for name in names:
        # 先把六個語言出現過的鍵收斂成「標題組」：同一個名字的各種
        # 占位符包裝（Lost Naga、Lost Naga {#}{#}\n{#}……）併成一組，
        # 組內不比——它們本來就該同譯。
        groups: dict[str, set[str]] = {}
        for lang in LANGS:
            for key in data[lang].get(name, {}):
                if worth(key):
                    groups.setdefault(title(key), set()).add(key)
        titles = sorted(groups)

        # 每一個標題組、每一個語言的代表譯文：取組內最短鍵的譯文。
        # 最短鍵是這個名字的「本體」——「Orc」是名牌，「!!!\nOrc」是同一句
        # 台詞掛上驚嘆號的變體，本體才適合當這一組的代表。
        rep_norm: dict[str, dict[str, str]] = {}   # rep_norm[lang][標題]
        rep_show: dict[str, dict[str, str]] = {}
        for lang in LANGS:
            entries = data[lang].get(name, {})
            rep_norm[lang] = {}
            rep_show[lang] = {}
            for t in titles:
                cands = [
                    (key, entries[key]) for key in groups[t]
                    if key in entries and norm_value(entries[key])
                ]
                if not cands:
                    continue
                best = min(cands, key=lambda kv: (len(kv[0]), kv[0]))
                rep_show[lang][t] = best[1]
                rep_norm[lang][t] = norm_value(best[1])

        # 收對子：哪個語言裡，同一個譯名對到兩個以上不同的標題組
        pairs: set[frozenset[str]] = set()
        for lang in LANGS:
            by_value: dict[str, list[str]] = {}
            for t, nv in rep_norm[lang].items():
                by_value.setdefault(nv, []).append(t)
            for ts in by_value.values():
                for a, b in combinations(sorted(ts), 2):
                    pairs.add(frozenset((a, b)))

        # 每一對做跨語言投票
        rows = []
        for pair in pairs:
            a, b = sorted(pair)
            votes, colliding = 0, set()
            for lang in LANGS:
                va, vb = rep_norm[lang].get(a, ""), rep_norm[lang].get(b, "")
                if not va or not vb:
                    continue  # 有一邊沒譯，這個語言投不了票
                if va == vb:
                    colliding.add(lang)
                else:
                    votes += 1
            if frozenset((a, b)) in keep:
                category = "整套"
            elif votes >= 4:
                category = "要修"
            elif votes >= 2:
                category = "待判"
            else:
                category = "同一個東西"
            rows.append((category, votes, a, b, colliding))

        # 分段：要修 → 待判 → 整套 → 同一個東西。段內按票數降、標題升。
        order = {"要修": 0, "待判": 1, "整套": 2, "同一個東西": 3}
        rows.sort(key=lambda r: (order[r[0]], -r[1], r[2], r[3]))
        counts = Counter(r[0] for r in rows)
        grand.update(counts)
        print(f"# {name}：要修 {counts['要修']} 對（全部 {len(rows)} 對）\n")
        for category, votes, a, b, colliding in rows:
            print(f"[{votes}/6 分得開] {category}   {a} / {b}")
            if category == "整套":
                print(f"   ↳ {keep[frozenset((a, b))]}")
            for lang in LANGS:
                va = rep_show[lang].get(a, "")
                vb = rep_show[lang].get(b, "")
                mark = "  <<" if lang in colliding else ""
                print(f"   {lang}   {va:<31}| {vb}{mark}")
            print()

    if len(names) > 1:
        print("、".join(f"{k} {grand[k]} 對" for k in ("要修", "待判", "整套", "同一個東西")))


def main(argv: list[str]) -> int:
    names = [n for n in NAME_FILES if not argv or n in argv or n[:-5] in argv]
    if not names:
        print(f"沒有名稱檔符合：{' '.join(argv)}")
        return 0
    run(TRANSLATIONS, KEEP_FILE, names)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
