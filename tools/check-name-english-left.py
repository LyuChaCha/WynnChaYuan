# -*- coding: utf-8 -*-
#!/usr/bin/env python3
"""句子裡還留著英文名，而別的語言在同一句裡翻了。

名字在整段句子裡有兩種下場：用譯名，或照留英文。繁中量過是 1315 比 90——
**用譯名是慣例**，所以那 90 處值得一條一條看。

但「留英文」有合法的時候：地區名、任務道具名、派系名（`Skyraider` 整族在對話裡
都留英文，而且裝備名 `… of the Skyraider` 也是）。單看一個語言分不出來，所以
判準是**跨語言**：同一句（同一個 src）裡，其他幾個語言有沒有把這個名字翻掉。

* 五個語言都翻了 -> 這個語言是孤例，該翻
* 大家都留英文 -> 慣例，不要動
* 中間 -> 待判

用法：python tools/check-name-english-left.py [語言...]
"""
import collections
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
BASE = ROOT / "src/main/resources/assets/wynnchayuan/translations"
LANGS = ("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es")
ARGS = [a for a in sys.argv[1:] if a in LANGS] or list(LANGS)
CJK = re.compile(r"[぀-ヿ㐀-鿿가-힯]")
COMMON = {
    "Guard", "Miner", "Merchant", "Villager", "Soldier", "Captain", "Scout",
    "Recommended", "Chapter", "Adventurer", "Apprentice",
}


def rows(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = data.get("entries")
    if isinstance(entries, dict):
        return [(v.get("src", "") or "", v.get("dst", "") or "")
                for v in entries.values() if isinstance(v, dict)]
    return [(k, v) for k, v in data.items()
            if not k.startswith("_") and isinstance(v, str)]


def plate(lang):
    out = {}
    for src, dst in rows(BASE / lang / "npc.json"):
        if ("\n" in src or "{" in src or not dst or src == dst or len(src) < 8
                or src in COMMON or "," in src or src[-1] in ".!?:"):
            continue
        if len(src.split()) < 2 and not (len(src) >= 7 and src[0].isupper()):
            continue
        out[src] = dst
    return out


def by_src(lang):
    out = {}
    for path in sorted((BASE / lang).rglob("*.json")):
        if path.name.startswith("_") or path.name == "npc.json":
            continue
        for src, dst in rows(path):
            if dst:
                out.setdefault(src, dst)
    return out


def run(LANG):
    names = plate(LANG)
    bound = {n: re.compile(r"(?<![A-Za-z0-9_'])" + re.escape(n)
             + r"(?:'s|s)?(?![A-Za-z0-9_])")
             for n in names}
    others = {lang: (plate(lang), by_src(lang))
              for lang in LANGS if lang != LANG}
    mine = by_src(LANG)

    tally = collections.defaultdict(lambda: [0, 0, []])   # name -> [翻, 留, 例]
    for src, dst in mine.items():
        if len(src) < 30:
            continue
        for name in names:
            if name not in src or not bound[name].search(src) or name not in dst:
                continue
            votes_tr, votes_en = 0, 0
            for lang, (pl, rs) in others.items():
                od, trans = rs.get(src), pl.get(name)
                if not od or not trans:
                    continue
                if name in od:
                    votes_en += 1
                elif (CJK.search(trans) and trans in od) or (
                        not CJK.search(trans) and trans.lower() in od.lower()):
                    votes_tr += 1
            t = tally[name]
            t[0] += votes_tr
            t[1] += votes_en
            if len(t[2]) < 2:
                t[2].append(dst[:70])

    print(f"######## {LANG}：{len(tally)} 個名字在句子裡留了英文\n")
    for name in sorted(tally, key=lambda n: -tally[n][0]):
        tr, en, ex = tally[name]
        verdict = ("該翻（孤例）" if tr >= 4 and en == 0 else
                   "慣例，別動" if en > tr else "待判")
        print(f"{name}  名牌「{names[name]}」  其他語言：翻 {tr}／留 {en}  -> {verdict}")
        for e in ex:
            print(f"     {e}")
        print()


def main():
    for lang in ARGS:
        run(lang)
    return 0


if __name__ == "__main__":
    sys.exit(main())
