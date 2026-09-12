#!/usr/bin/env python3
"""哪些任務對話從來沒有被實機證實過。

為什麼需要這一份
----------------
任務對話有九成八抓自 wiki，而 wiki 是人打的、而且常常沒跟上 Wynncraft 改台詞。
所以「這個任務 100% 翻完」跟「玩家在遊戲裡看得到中文」是<b>兩件事</b>：

    語料（wiki）  Follow this road, he's just past the item identifier…
    實機          My brother shouldn't be hard to find, he's obsessed with stairs…

同一句話，Wynncraft 重寫過，wiki 沒跟上。查表完全落空，畫面上就是純英文——
而進度表照樣寫 100%，因為那一條「翻好了」。

這支程式把落差攤出來：每個任務有多少條是<b>只有 wiki 說過</b>、
還沒有任何一次實機收集證實過。數字愈大，那個任務在遊戲裡愈可能出現英文。

`source` 欄位怎麼來的
---------------------
`tools/fetch-quest-dialogue.py` 從 wiki 抓的標 `wiki`，
`tools/import-captured.py` 從玩家的 captured.json 收的標 `capture`。
（舊版寫成 `captured`，兩種都算實機。）

用法：
    python tools/wiki-gap.py            # 印出最需要注意的前 25 個任務
    python tools/wiki-gap.py --all      # 全部
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
QUESTS = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw/quest"

# 實機來的標記。舊版寫 captured、新版寫 capture，兩種都算。
FROM_GAME = {"capture", "captured"}


def main(argv: list[str]) -> int:
    rows = []
    total_wiki = total_game = 0
    for path in sorted(QUESTS.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:                                     # noqa: BLE001
            continue
        entries = data.get("entries", {})
        wiki = sum(1 for v in entries.values()
                   if v.get("source") not in FROM_GAME)
        game = len(entries) - wiki
        total_wiki += wiki
        total_game += game
        if wiki:
            rows.append((wiki, game, path.stem))

    rows.sort(reverse=True)
    show = rows if "--all" in argv else rows[:25]
    print("只有 wiki 說過、實機沒證實過的任務對話")
    print()
    print("%-42s %8s %8s" % ("任務", "只有 wiki", "實機證實"))
    print("-" * 60)
    for wiki, game, name in show:
        print("%-42s %8d %8d" % (name, wiki, game))
    if len(show) < len(rows):
        print("...（還有 %d 個任務，加 --all 看全部）" % (len(rows) - len(show)))
    print()
    both = total_wiki + total_game
    print("合計 %d 條，其中 %d 條（%.1f%%）只有 wiki 說過。"
          % (both, total_wiki, 100.0 * total_wiki / max(both, 1)))
    print("這個比例是「明明翻好了、遊戲裡卻是英文」的<b>風險上限</b>，不是進度。")
    print()
    print("它不會因為有人去玩就往下掉——收集端刻意不重收語料裡已經有的句子"
          "（見 TranslationStore#seenSources），所以「wiki 剛好寫對」的那些")
    print("永遠不會被標成實機證實。真正會動的是另一邊：wiki 寫錯的那些會以"
          "「新句子」的身分被收進來，近似的由 tools/near-miss.py 直接把 src")
    print("改對，差太多的當成新條目進語料等人翻。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
