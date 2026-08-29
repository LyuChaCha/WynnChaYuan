#!/usr/bin/env python3
"""把維基抓取時被丟掉的座標補回目標行。

問題
----
維基把座標寫成模板：

    » Talk to [[Sayleros]] at {{MapLink|x=477|y=70|z=-1623}}.

`fetch-quest-dialogue.py` 的 `clean()` 有一條「其餘模板一律丟掉」，
於是整個 MapLink 連座標一起消失，收進語料的變成：

    Talk to Sayleros at .

遊戲裡這句話是帶著座標的（`Talk to Sayleros at [477, 70, -1623].`），
所以這種 key **永遠比對不到**，翻了也不會顯示。`clean()` 已經修好，
但既有的 `quest/*.json` 還帶著壞掉的 `src`。

為什麼不直接重跑 fetch
----------------------
`fetch-quest-dialogue.py` 寫的是 `quest-dialogue.json`，而那份現在是
`quest-bundle.py` 從 `quest/*.json` 產生的**衍生檔**。重跑抓取不會修到
工作檔，下一次 bundle 又會把它蓋回去。所以這裡直接修工作檔。

安全機制
--------
只在「舊 src 等於新 src 把座標拿掉之後的樣子」時才動。維基頁若在這之間
改過、句子順序位移，比對就會失敗，那一條寧可留著不動也不亂改。
`dst` 與其他欄位完全不碰。

用法
----
    python tools/fix-maplink-coords.py --dry-run    # 只看會改哪些
    python tools/fix-maplink-coords.py              # 實際寫入
"""

from __future__ import annotations

import argparse
import collections
import json
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import importlib

fetch = importlib.import_module("fetch-quest-dialogue")

ROOT = Path(__file__).resolve().parent.parent
QUESTS = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw/quest"

# 座標被抽成佔位符之後長這樣：`[{~}, {~}, -{~}]`，每個數字前面都可能有負號
#
# 這裡<b>不能</b>連前面的空白一起吃掉。當初 clean() 丟掉模板之後留下的是
# 「at .」（at 後面還有一個空格），把空白一起吃掉會還原成「at.」，
# 於是每一條都比對失敗。
COORDS = re.compile(r"\[-?\{~\}, -?\{~\}, -?\{~\}\]")


def without_coords(text: str) -> str:
    """把座標整段拿掉，還原成當初壞掉的樣子，用來比對。"""
    return re.sub(r"\s+", " ", COORDS.sub("", text)).strip()


def broken(text: str) -> bool:
    """看起來像是座標被剝掉的目標行。

    `at` 與 `to` 都要看——「Bring back ... will to .」也是同一種傷。
    這裡寧可寬鬆：抓進來的句子還要通過下面「拿掉座標之後要一模一樣」
    那道比對，一般散文結尾的 to／at 過不了那關，不會被誤改。
    """
    return bool(re.search(r"\b(?:at|to)\s*\.?\s*$|\bat\s+(?:and|using)\b", text))


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true", help="只列出，不寫入")
    ap.add_argument("--delay", type=float, default=0.4)
    args = ap.parse_args(argv)

    # 先找出哪些檔案有壞掉的目標行，只抓那幾頁
    targets: dict[Path, dict] = {}
    for path in sorted(QUESTS.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        hits = {k: v for k, v in data["entries"].items()
                if isinstance(v, dict) and v.get("src") and broken(v["src"])}
        if hits:
            targets[path] = {"data": data, "hits": hits}

    if not targets:
        print("沒有需要修的目標行。")
        return 0

    print(f"有壞掉目標行的檔案：{len(targets)} 個，"
          f"共 {sum(len(t['hits']) for t in targets.values())} 句")

    stats = collections.Counter()
    for path, info in targets.items():
        data, hits = info["data"], info["hits"]
        page = next(iter(data["entries"])).rsplit("#", 1)[0]
        try:
            wikitext = fetch.get({"action": "parse", "page": page,
                                  "prop": "wikitext"})
            wikitext = wikitext.get("parse", {}).get("wikitext", "")
        except Exception as e:                                # noqa: BLE001
            print(f"  ! {page}：{e}")
            stats["抓取失敗"] += 1
            continue
        fresh = {f"{page}#{i:03d}": row["src"]
                 for i, row in enumerate(fetch.parse(page, wikitext))}

        changed = 0
        for key, entry in hits.items():
            new = fresh.get(key)
            if not new:
                stats["維基上找不到對應句"] += 1
                continue
            if without_coords(new) != entry["src"].strip():
                stats["對不上（維基已改動）"] += 1
                continue
            if new == entry["src"]:
                continue
            print(f"    {key}")
            print(f"      - {entry['src']}")
            print(f"      + {new}")
            if not args.dry_run:
                entry["src"] = new
            changed += 1
            stats["已修正"] += 1

        if changed and not args.dry_run:
            text = json.dumps(data, ensure_ascii=False, indent=1) + "\n"
            raw = path.read_bytes()
            if b"\r\n" in raw:
                text = text.replace("\n", "\r\n")
            path.write_bytes(text.encode("utf-8"))
        time.sleep(args.delay)

    print()
    for what, n in stats.most_common():
        print(f"  {what}：{n}")
    if args.dry_run:
        print("\n（--dry-run，沒有寫入。記得之後跑 tools/quest-bundle.py）")
    else:
        print("\n記得跑 tools/quest-bundle.py 重新產生 quest-dialogue.json")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
