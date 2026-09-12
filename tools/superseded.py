#!/usr/bin/env python3
"""Wynncraft 改過台詞、舊的那一條還留在語料裡。

為什麼會有這種東西
------------------
任務對話九成八抓自 wiki。Wynncraft 改台詞、wiki 沒跟上時，實機送出的字串
就跟語料裡的對不上，玩家看到英文。收集端會把實機那一句當成<b>新條目</b>
收進來——這一步是對的，但它只做了一半：

* 新的那一條進來了，<b>沒有譯文</b>
* 舊的那一條還在，<b>有譯文</b>，而且永遠不會再被用到

於是同一句話在語料裡有兩份，翻好的那份是死的。進度表還把它算成已翻。

差一點點的（相似度 0.9 以上）由 ``tools/near-miss.py`` 直接把 src 改對，
不會走到這裡。這支程式管的是<b>改比較多</b>的那些——譯文救得回來，
但要人看過再決定怎麼改，機器不能自己貼。

<h2>為什麼不自動搬譯文</h2>
`Who knows, maybe there's a treasure up there?` 改成
`Who knows, maybe there's a treasure or something up there?` 幾乎照抄就好；
但 `Are you ready to continue on towards {p}?` 改成
`I'd say we should keep heading towards {p}, but...` 意思整個變了。
機器分不出這兩種，而貼錯的中文比留著英文更糟。

<h2>舊的那一條為什麼不能就這樣留著</h2>
不只是佔位子。對話是逐字打出來的，而查表會拿<b>打到一半的前綴</b>去比對：

    實機   Who knows, maybe there's a treasure or something up there?
    舊的   Who knows, maybe there's a treasure up there?

打到 ``…there's a treasure`` 的時候兩句都對得上，畫面上會先貼出舊的那一句的
中文；再打幾個字岔開，才掉回英文。玩家看到的就是「翻到一半變回原文」。
所以高相似度的那些該刪掉，不是留著當備份。

用法：
    python tools/superseded.py            # 列出還沒翻的新條目配對
    python tools/superseded.py --all      # 全部列出（新條目已翻的也列）
    python tools/superseded.py --drop     # 刪掉高相似度那些的舊條目
"""

from __future__ import annotations

import difflib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
QUESTS = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw/quest"

# 實機來的標記。舊版寫 captured、新版寫 capture。
FROM_GAME = {"capture", "captured"}

# 相似度的兩端。
#
# 0.90 以上是 near-miss 的地盤，那種直接改 src 就好，不該出現在這份清單。
# 0.55 以下開始出現「同一個 NPC 兩句不相干的話」——任務對話本來就用字重複。
HIGH = 0.90
LOW = 0.55

# 太短的不比：`...`、`Yes.`、`Follow me!` 這種任兩句都很像。
MIN_LENGTH = 25


# --drop 只動這個門檻以上的。
#
# 0.80 以上看過去全部是同一句換個說法（`treasure` → `treasure or something`、
# `finally be able` → `be able to finally`）。0.80 以下開始混進「同一個 NPC
# 兩句本來就有點像的話」，那種刪掉會真的少一句，要人看過。
DROP_AT = 0.80


def main(argv: list[str]) -> int:
    show_done = "--all" in argv or "--drop" in argv
    drop = "--drop" in argv
    pairs = []
    for path in sorted(QUESTS.glob("*.json")):
        try:
            entries = json.loads(path.read_text(encoding="utf-8")).get("entries", {})
        except Exception:                                     # noqa: BLE001
            continue
        game = [v for v in entries.values()
                if v.get("source") in FROM_GAME
                and len(v.get("src", "")) >= MIN_LENGTH]
        wiki = [v for v in entries.values()
                if v.get("source") not in FROM_GAME
                and len(v.get("src", "")) >= MIN_LENGTH
                and v.get("dst", "").strip()]
        for new in game:
            if new.get("dst", "").strip() and not show_done:
                continue
            best = None
            for old in wiki:
                # 說話者不同就不是同一句被改寫
                if (new.get("speaker") and old.get("speaker")
                        and new["speaker"] != old["speaker"]):
                    continue
                ratio = difflib.SequenceMatcher(
                    None, new["src"], old["src"]).ratio()
                if LOW <= ratio < HIGH and (best is None or ratio > best[0]):
                    best = (ratio, old)
            if best:
                pairs.append((best[0], path.stem, new, best[1], path))

    pairs.sort(key=lambda r: -r[0])

    if drop:
        gone = 0
        by_file = {}
        for ratio, _quest, new, old, path in pairs:
            # 只刪「新的那一句已經翻好」的，不然刪完那個位置就沒有中文了
            if ratio < DROP_AT or not new.get("dst", "").strip():
                continue
            by_file.setdefault(path, []).append(old["src"])
        for path, srcs in by_file.items():
            data = json.loads(path.read_text(encoding="utf-8"))
            entries = data.get("entries", {})
            for key in [k for k, v in entries.items() if v.get("src") in srcs]:
                del entries[key]
                gone += 1
            meta = data.get("_meta")
            if isinstance(meta, dict):
                if "count" in meta:
                    meta["count"] = len(entries)
                if "translated" in meta:
                    meta["translated"] = sum(
                        1 for v in entries.values() if v.get("dst"))
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                            encoding="utf-8", newline="\n")
        print("刪掉 %d 條被改寫掉的舊條目（相似度 %.2f 以上）。" % (gone, DROP_AT))
        print("記得跑 tools/quest-bundle.py 重建合併檔。")
        return 0

    for ratio, quest, new, old, _path in pairs:
        print("[%s] %.2f  %s" % (quest, ratio, new.get("speaker") or ""))
        print("   實機：", new["src"].replace("\n", " ⏎ "))
        print("   舊的：", old["src"].replace("\n", " ⏎ "))
        print("   舊譯：", old["dst"].replace("\n", " ⏎ "))
        print()
    print("疑似被改寫、舊譯文還留著的：%d 組" % len(pairs))
    if pairs:
        print("舊譯文多半改一下就能用在新的那一句上，但意思變了的要重翻——"
              "所以這裡只列出來，不自動搬。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
