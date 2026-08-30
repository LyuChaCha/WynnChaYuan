#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把技能樹檔案整成統一的格式。

<h2>為什麼需要這支工具</h2>
翻譯團隊分批交各職業的技能樹。交付方式不只一種：mage 當初給的是 Wynnability
編輯器的匯出檔（要跑 ``import-ability-tree.py`` 轉），assassin 則是直接改我們
自己的檔送回來。**後者不經過匯入工具，於是匯入時本來會做的整理就都沒做**——
檔案的內容是對的，形狀卻跟先前的職業不一樣。

實際差在三個地方（assassin 對 mage）：

* ``keep`` 沒被拿掉。匯入工具只在「名稱的譯文真的被改動」時才 ``pop("keep")``，
  而團隊這次自己就把中文名填好了，那一步沒觸發，91 個 ``keep: "en"`` 留在檔裡。
  程式不讀它，畫面不受影響，但它跟「技能名稱全面中文化」的決定相矛盾，
  留著會誤導下一個看檔案的人。
* 三條 ``kind: block`` 少了 ``lines``。它跟 ``flat`` 是一對，由 ``build.py``
  一起產生；只有一半在，就看不出這一條原文有幾行。
* 欄位順序有四種。同一種東西長成四個樣子，diff 會變得很難讀。

<h2>做法</h2>
只做**機械性**的整理，一個字都不改：拿掉 ``keep``、照 ``src`` 補回 ``lines``、
把欄位排成同一個順序。``src``/``dst``/``tree``/``code`` 這些內容欄位原樣不動。

    python tools/ability-format.py            # 只報告
    python tools/ability-format.py --write    # 真的寫回去

沒有指定檔案就掃 ``translations/*/ability/*.json``。
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"

# 欄位的標準順序。取自 mage——它是唯一走完整匯入流程的職業，也就是這個格式
# 本來該長的樣子。不在這張表裡的欄位會排在最後面（照原本的相對順序），
# 這樣以後多出新欄位也不會被工具吃掉。
ORDER = ["src", "dst", "role", "kind", "ctx",
         "lines", "flat", "_raw", "tree", "keep", "ability", "code"]

# ``keep`` 是給譯者看的註記（「這一條保留原文」），程式不讀。
#
# **只有已經翻好的才拿掉。**zh_tw 決定技能名稱全面中文化、名字也真的填了中文，
# 那個註記就只剩誤導作用；但其餘七個語言的技能名一條都還沒翻，對他們來說
# ``keep: "en"`` 仍然是有效的指示，一律拿掉等於把交代給譯者的話擦掉。
#
# 這也正是匯入工具的行為：``import-ability-tree.py`` 只在真的寫進 ``dst``
# 的那一刻才 ``entry.pop("keep", None)``。


def tidy(entry: dict) -> tuple[dict, list[str]]:
    """整理一條，回報動了哪些地方。"""
    notes = []
    out = dict(entry)

    if "keep" in out and out.get("dst"):
        del out["keep"]
        notes.append("拿掉 keep")

    # lines 與 flat 是一對，由 build.py 一起產生。只有 flat 的話照 src 補回來——
    # 這不是猜的：檔案裡其餘每一條都滿足 lines == src 的行數。
    if out.get("kind") == "block" and "flat" in out and "lines" not in out:
        out["lines"] = len(out["src"].split("\n"))
        notes.append("補回 lines=%d" % out["lines"])

    order = {name: i for i, name in enumerate(ORDER)}
    tail = len(ORDER)
    before = list(out)
    after = sorted(before, key=lambda k: (order.get(k, tail), before.index(k)))
    if before != after:
        notes.append("欄位重新排序")
    return {k: out[k] for k in after}, notes


def check(entries: dict) -> list[str]:
    """整理不了、但值得講一聲的事。

    翻沒翻不在這裡管——那是 validate.py 與進度表的事，混進來只會把
    真正該看的那一行淹掉（其他語言大多還沒翻，一掃就是幾百行）。
    """
    warned = []
    for key, row in entries.items():
        if row.get("kind") == "block" and "flat" in row:
            want = " ".join(row["src"].split("\n"))
            if row["flat"] != want:
                warned.append("%s：flat 跟 src 對不起來（有人手改過？）" % key)
    return warned


def label_of(path: Path) -> str:
    """語言 + 檔名。掃全部語言時，光看檔名分不出是哪一份。"""
    try:
        return "/".join(path.resolve().relative_to(TRANSLATIONS).parts[::2])
    except ValueError:
        return path.name


def run(path: Path, write: bool) -> int:
    raw = path.read_bytes().decode("utf-8")
    crlf = "\r\n" in raw
    data = json.loads(raw)
    entries = data.get("entries")
    if entries is None:
        print("  %s：沒有 entries，跳過" % path.name)
        return 0

    touched = {}
    for key, row in entries.items():
        fixed, notes = tidy(row)
        if notes:
            touched[key] = notes
        entries[key] = fixed

    for line in check(entries)[:5]:
        print("  ! " + line)

    if not touched:
        print("  %-24s 已經是標準格式" % label_of(path))
        return 0

    tally: dict[str, int] = {}
    for notes in touched.values():
        for note in notes:
            head = note.split("=")[0]
            tally[head] = tally.get(head, 0) + 1
    print("  %-24s %d 條要整：%s" % (
            label_of(path), len(touched),
            "、".join("%s×%d" % (k, v) for k, v in sorted(tally.items()))))

    if write:
        text = json.dumps(data, ensure_ascii=False, indent=1) + "\n"
        if crlf:
            text = text.replace("\n", "\r\n")
        path.write_bytes(text.encode("utf-8"))
    return len(touched)


def main(argv: list[str]) -> int:
    write = "--write" in argv
    named = [Path(a) for a in argv if not a.startswith("--")]
    files = named or sorted(TRANSLATIONS.glob("*/ability/*.json"))
    if not files:
        print("找不到技能樹檔案")
        return 2
    total = sum(run(path, write) for path in files)
    if total and not write:
        print("\n加 --write 才會真的寫回去")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
