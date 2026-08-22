"""替譯文裡重複出現的 `{~}` 補上編號。

<h2>為什麼要編號</h2>
`{~}` 是照<b>順序</b>取的：譯文裡第一個 `{~}` 拿原文第一個數值，第二個拿第二個。
中文語序常常跟英文不一樣，一調換順序，數值就跟著錯位：

    src  Healing {~} health to allies within {~} blocks
    dst  對 {~} 格內的隊友治療 {~} 點生命      ← 距離拿到了治療量

<p>畫面上不會報錯，只會顯示成一個<b>看起來很合理的錯數字</b>——這種錯最難發現，
先前就是靠使用者截圖才抓到十條。

<p>編號版 `{~1}`、`{~2}` 直接指名要原文的第幾個，語序怎麼調都不會錯。

<h2>這支工具做什麼</h2>
把譯文裡的 `{~}` 逐個換成 `{~1}`、`{~2}`……<b>照現在的順序</b>。
今天的行為完全不變（順序取用的結果一樣），但從此順序是<b>寫明的</b>：
譯者要調語序時，把編號跟著搬就好，不必再心算第幾個。

<p>只處理「原文有兩個以上數值」的條目——只有一個的時候不會錯位，
加編號只是噪音。譯文已經有編號的、數量對不上的，一律不動。

用法：
    python tools/index-numbers.py
    python tools/index-numbers.py --write
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

BASE = pathlib.Path("src/main/resources/assets/wynnchayuan/translations")
NL = chr(10)

BARE = re.compile(r"\{~\}")
INDEXED = re.compile(r"\{~\d\}")


def index_them(dst: str) -> str:
    counter = iter(range(1, 10))
    return BARE.sub(lambda _: "{~" + str(next(counter)) + "}", dst)


def needs_indexing(src: str, dst: str) -> bool:
    if not src or not dst:
        return False
    if INDEXED.search(dst):
        return False                    # 已經有編號了
    total = len(BARE.findall(src))
    # 只有一個數值不會錯位；兩邊數量對不上表示譯者刻意增刪，不能自動處理
    return total >= 2 and len(BARE.findall(dst)) == total


def main(argv: list[str]) -> int:
    write = "--write" in argv
    changed = 0
    for path in sorted(BASE.rglob("*.json")):
        if path.name.startswith("_"):
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        rows = data.get("entries")
        flat = not isinstance(rows, dict)
        if flat:
            rows = data
        touched = []
        for key, entry in rows.items():
            if key.startswith("_"):
                continue
            if isinstance(entry, dict):
                src, dst = entry.get("src", key), entry.get("dst", "")
            else:
                src, dst = key, entry
            if not needs_indexing(src, dst):
                continue
            fixed = index_them(dst)
            touched.append((src, dst, fixed))
            if isinstance(entry, dict):
                entry["dst"] = fixed
            else:
                rows[key] = fixed
        if not touched:
            continue
        print(f"  {path.relative_to(BASE)}  {len(touched)} 條")
        for src, before, after in touched[:2]:
            print(f"      原文 {src.splitlines()[0][:60]!r}")
            print(f"      舊譯 {before.splitlines()[0][:60]!r}")
            print(f"      新譯 {after.splitlines()[0][:60]!r}")
        changed += len(touched)
        if write:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + NL,
                            encoding="utf-8", newline=NL)

    print()
    print(f"編號 {changed} 條"
          + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
