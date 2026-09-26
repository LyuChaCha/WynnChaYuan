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
加編號只是噪音。譯文已經有編號的一律不動。

<h2>不能自動編號的，指名出來</h2>
有兩種條目不動它，而且會在最後<b>逐條列出檔名與鍵</b>，讓人自己去對：

    數量對不上    照順序編號會把數值接到錯的欄位上，只有人看得出來該接哪個
    十個以上      編號只寫得到 {~9}，見 MAX_INDEX

<p>以前這兩種都是靜靜跳過的，第二種還會讓整支工具掛掉：編號用的
`iter(range(1, 10))` 取完第九個就 `StopIteration`，而且那個 traceback 裡
<b>完全看不出是哪一條語料</b>。不帶參數直接跑就會中。

用法：
    python tools/index-numbers.py
    python tools/index-numbers.py --write
"""

from __future__ import annotations

import itertools
import json
import pathlib
import re
import sys

BASE = pathlib.Path("src/main/resources/assets/wynnchayuan/translations/zh_tw")
NL = chr(10)

BARE = re.compile(r"\{~\}")
# 認得多位數的 {~10}，這樣萬一哪天真的有，也算「已經有編號了」而不是被當成沒編號
INDEXED = re.compile(r"\{~\d+\}")

# 編號只寫得到一位數。validate.py 的 NUMBERED（`\{~[1-9]\}`）就是這樣認的，
# 寫成 {~10} 那邊會當成完全沒有佔位符，直接報「數量不符」。
MAX_INDEX = 9


def index_them(dst: str) -> str:
    """把 `{~}` 逐個換成 `{~1}`、`{~2}`……照原本的順序。

    <p>計數器用不設上限的 `itertools.count` 是故意的：要不要編號是
    {@code classify} 的事，這裡再放一個會取完的計數器，只會在別人改壞
    上游條件時炸成一個看不出是哪條語料的 `StopIteration`。
    """
    counter = itertools.count(1)
    return BARE.sub(lambda _: "{~" + str(next(counter)) + "}", dst)


def classify(src: str, dst: str) -> tuple[str, str]:
    """這一條要編號、安靜跳過，還是指名出來給人看。

    @return ("index", "")、("skip", "")，或 ("report", 要印出來的原因)
    """
    if not src or not dst:
        return "skip", ""
    if INDEXED.search(dst):
        return "skip", ""                       # 已經有編號了
    total = len(BARE.findall(src))
    here = len(BARE.findall(dst))
    if total < 2:
        return "skip", ""                       # 只有一個數值不會錯位
    if here != total:
        # 譯者刻意增刪，或是漏抄了一個。照順序編號會編出一組指錯欄位的號碼，
        # 比不編還糟——只能指名出來。
        return "report", f"譯文 {here} 個 {{~}}，原文 {total} 個，數量對不上"
    if total > MAX_INDEX:
        return "report", (f"譯文 {here} 個 {{~}}，原文 {total} 個，"
                          f"超過編號上限 {{~{MAX_INDEX}}}")
    return "index", ""


def main(argv: list[str]) -> int:
    write = "--write" in argv
    changed = 0
    refused: list[tuple[str, str, str]] = []
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
            action, note = classify(src, dst)
            if action == "report":
                refused.append((str(path.relative_to(BASE)), key, note))
                continue
            if action != "index":
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

    if refused:
        print()
        print(f"沒有編號 {len(refused)} 條，要人工確認"
              "——鍵印的是完整原文，可以直接拿去檔案裡搜：")
        for where, key, note in refused:
            print(f"  {where}  {note}")
            print(f"      鍵 {key!r}")

    print()
    print(f"編號 {changed} 條"
          + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
