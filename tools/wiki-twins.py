#!/usr/bin/env python3
"""同一句台詞收了兩份：一份抄自 wiki、一份玩家實機抓回來的。

為什麼會有這種東西
------------------
任務對話九成八抓自 wiki，而 wiki 的轉錄跟實機常常差一點——`Now leave` 對
`Then leave`、`…` 對 `...`、彎引號對直引號、行首多一個 `*`。收集端把實機那一句
當成新條目收進來（這一步是對的），但舊的那一條還在，於是同一句話有兩份。

**這不只是佔位子。** 對話是逐字打出來的，而查表會拿打到一半的前綴去比對：

    實機   Then leave, and forget the horrors you have witnessed here today...
    wiki   Now leave, and forget the horrors you have witnessed here today...

打到 `...leave, and forget the horrors you have witnessed here today...` 的時候
兩條都命中，先貼出 wiki 那條的譯文，再多打幾個字換成實機那條的——畫面上就是
中文自己改口。`tools/typing-audit.py` 報的就是這個。

實機端已經有 ``TranslationStore#curatedRival`` 在擋（命中 wiki 版時去找同一句的
校訂版），但它要詞重疊率過門檻，差得多的擋不掉。

怎麼配對
--------
**在同一個任務檔裡**找 ``source: wiki`` 與 ``source`` 含 ``captured`` 的近似對：
正規化後（小寫、標點換空白）相似度 ≥0.90，而且**一對一**——同一條 wiki 對上
兩條實機、或反過來，就不動，那種要人看。

不要去解析 ``tools/typing-audit.py`` 的報告：它把長句截掉，解出來的是前綴，
對不回語料（實測 101 組裡只有 34 組配得上）。

原文的結構六個語言要一樣，所以配對一律照 ``zh_tw`` 算，再對每個語言刪同一批
原文。

刪掉之前要搬什麼
----------------
* ``stage``／``speaker``——實機條目缺的就從 wiki 條目補上，不然譯者會失去上下文。
* ``dst``——**只有實機條目空著的時候才搬**，而且佔位符個數要一致。實機條目自己
  有譯文的就不動：兩份譯文留哪一個不是機器能決定的。

用法
----
    python tools/wiki-twins.py              # 只報告
    python tools/wiki-twins.py --write
    python tools/wiki-twins.py --write --lang zh_tw

改完記得跑 ``tools/quest-bundle.py`` 重建合併檔——``typing-audit.py`` 讀的是
合併檔，只改 ``quest/`` 底下它看不到。
"""
from __future__ import annotations

import difflib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"
LANGS = ("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es")
SAME_AT = 0.90
PUNCT = re.compile(r"[^\w{}~#pu]+", re.UNICODE)


def norm(text: str) -> str:
    """比相似度之前先抹掉標點與大小寫——差別幾乎都在那裡。"""
    return PUNCT.sub(" ", text.lower()).strip()


def marks(text: str) -> tuple[int, ...]:
    return tuple(text.count(m) for m in ("{~}", "{#}", "{p}", "{u}"))


def quest_files(lang: str) -> list[Path]:
    base = TRANSLATIONS / lang
    return sorted(base.glob("quest/*.json")) + sorted(base.glob("secret/*.json"))


def read(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def pair_up() -> tuple[list[tuple[str, str, str]], int]:
    """照 zh_tw 配對。回 ([(wiki 的 src, 實機的 src, 檔名)], 配不出來的組數)。"""
    pairs: list[tuple[str, str, str]] = []
    ambiguous = 0
    for path in quest_files("zh_tw"):
        entries = read(path)["entries"]
        wiki = [(k, v) for k, v in entries.items() if v.get("source") == "wiki"]
        live = [(k, v) for k, v in entries.items()
                if "captured" in str(v.get("source"))]
        if not wiki or not live:
            continue
        hits: dict[str, list[str]] = {}
        for wk, wv in wiki:
            for lk, lv in live:
                if wv["src"] == lv["src"]:
                    continue
                if difflib.SequenceMatcher(
                        None, norm(wv["src"]), norm(lv["src"])).ratio() >= SAME_AT:
                    hits.setdefault(wk, []).append(lk)
        claimed: dict[str, list[str]] = {}
        for wk, cands in hits.items():
            if len(cands) != 1:
                ambiguous += 1          # 一條 wiki 對上好幾條實機
                continue
            claimed.setdefault(cands[0], []).append(wk)
        for lk, wks in claimed.items():
            if len(wks) != 1:
                ambiguous += len(wks)   # 好幾條 wiki 對上同一條實機
                continue
            pairs.append((entries[wks[0]]["src"], entries[lk]["src"], path.name))
    return pairs, ambiguous


def sweep(lang: str, pairs: list[tuple[str, str, str]], write: bool) -> None:
    docs: dict[Path, dict] = {}
    where: dict[str, list[tuple[Path, str]]] = {}
    for path in quest_files(lang):
        data = read(path)
        docs[path] = data
        for key, entry in data["entries"].items():
            where.setdefault(entry["src"], []).append((path, key))

    removed = moved_dst = moved_meta = skipped = 0
    touched: set[Path] = set()
    for wiki_src, live_src, _name in pairs:
        w, l = where.get(wiki_src), where.get(live_src)
        # 這個語言沒有那一條、或同一句出現在兩個地方，就不動
        if not w or not l or len(w) != 1 or len(l) != 1 or w[0][0] != l[0][0]:
            skipped += 1
            continue
        (path, wk), (_, lk) = w[0], l[0]
        we = docs[path]["entries"][wk]
        le = docs[path]["entries"][lk]
        if we.get("source") != "wiki" or "captured" not in str(le.get("source")):
            skipped += 1
            continue
        for field in ("stage", "speaker"):
            if we.get(field) and not le.get(field):
                le[field] = we[field]
                moved_meta += 1
        if not le["dst"].strip() and we["dst"].strip() \
                and marks(wiki_src) == marks(live_src):
            le["dst"] = we["dst"]
            moved_dst += 1
        del docs[path]["entries"][wk]
        removed += 1
        touched.add(path)

    print("%-6s 刪 %d 條（救回譯文 %d、補欄位 %d），對不上的 %d"
          % (lang, removed, moved_dst, moved_meta, skipped))
    if not write:
        return
    for path in sorted(touched):
        data = docs[path]
        meta = data.get("_meta", {})
        if "count" in meta:
            meta["count"] = len(data["entries"])
        if "translated" in meta:
            meta["translated"] = sum(
                1 for v in data["entries"].values() if v["dst"].strip())
        path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                        encoding="utf-8", newline="\n")
    print("       寫了 %d 個檔" % len(touched))


def main(argv: list[str]) -> int:
    write = "--write" in argv
    langs = list(LANGS)
    if "--lang" in argv:
        langs = [argv[argv.index("--lang") + 1]]
    pairs, ambiguous = pair_up()
    print("照 zh_tw 配出 %d 對，一對多／多對一的 %d 個沒動（相似度 %.2f 以上）"
          % (len(pairs), ambiguous, SAME_AT))
    if not write:
        for wiki_src, live_src, name in pairs:
            print("[%s]" % name)
            print("   wiki：", wiki_src.replace("\n", " ⏎ "))
            print("   實機：", live_src.replace("\n", " ⏎ "))
    for lang in langs:
        sweep(lang, pairs, write)
    if write:
        print("記得跑 tools/quest-bundle.py 重建合併檔。")
    elif pairs:
        print("加 --write 才會真的刪。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
