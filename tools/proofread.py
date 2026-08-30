#!/usr/bin/env python3
"""產生校稿清單：哪些檔是 AI 翻的、還沒有人校過。

為什麼需要
----------
AI 翻得快，但快不等於對。語氣、雙關、角色口癖這些東西機器容易翻得「通順
但不對」——通順反而更難被發現。所以每一份 AI 譯文都應該由人再看一遍。

問題是「看一遍」需要知道<b>要看哪些</b>。合併紀錄裡混著團隊自己翻的、
AI 翻的、以及只是搬移分類的改動，翻譯團隊沒辦法從 git log 分辨。
這份清單把它攤成一張表。

怎麼標記
--------
每個譯文檔的 `_meta.review` 記著兩件事::

    "review": {
      "translator": "claude",
      "proofread": false
    }

* ``translator`` —— ``claude`` 是 AI 翻的、``team`` 是人翻的。
  沒有這個欄位就算「未標記」，會另外列出來，不會被當成已經校過。
* ``proofread`` —— ``false`` 表示還沒校。校完把它<b>改成校稿者與日期</b>，
  例如 ``"SCPNightsky 2026-08-30"``。字串一律視為已校稿。

改完重跑一次這支程式（或讓 rebuild 工作流程跑）就會更新清單。

為什麼是檔層級而不是逐條
------------------------
一個任務就是一個檔，而 AI 是<b>整篇一次翻完</b>的——校稿的人也是整篇讀下來
才看得出語氣有沒有一致。逐條標記會產生兩萬多個布林值，維護成本遠高於它
帶來的好處，而且沒有人會去逐條打勾。

散在共用檔（misc.json、gui.json）裡的零星條目沒辦法用檔層級表示，
這支程式不會假裝它們不存在——清單結尾會指到 CHANGELOG，那裡逐批記著
每次加了什麼。

用法
----
    python tools/proofread.py            # 產生 docs/PROOFREADING.md
    python tools/proofread.py --check    # 只檢查是不是過期（給 CI 用）
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw"
OUT = ROOT / "docs/PROOFREADING.md"

# 這幾個是產生物或索引，不是人要校的東西。
SKIP = {"quest-dialogue.json", "_index.json"}


class Entry:
    """一個譯文檔在清單上的一列。"""

    def __init__(self, path: Path, name: str, total: int, done: int,
                 translator: str | None, proofread) -> None:
        self.path = path
        self.name = name
        self.total = total
        self.done = done
        self.translator = translator
        self.proofread = proofread

    @property
    def rel(self) -> str:
        return self.path.relative_to(ROOT).as_posix()

    @property
    def link(self) -> str:
        # 清單放在 docs/ 底下，連結要往上跳一層
        return f"[`{self.path.name}`](../{self.rel})"


def count(data) -> tuple[int, int]:
    """回傳（總句數, 已翻句數）。

    兩種格式都要吃：任務檔是 ``{"entries": {key: {"src", "dst"}}}``，
    分類檔是扁平的 ``{原文: 譯文}``。
    """
    entries = data.get("entries", data) if isinstance(data, dict) else {}
    if not isinstance(entries, dict):
        return 0, 0
    total = done = 0
    for key, value in entries.items():
        if key.startswith("_"):
            continue
        if isinstance(value, str):
            dst = value
        elif isinstance(value, dict):
            dst = value.get("dst") or ""
        else:
            continue
        total += 1
        if dst.strip():
            done += 1
    return total, done


def collect() -> list[Entry]:
    out: list[Entry] = []
    for path in sorted(TRANSLATIONS.rglob("*.json")):
        if path.name in SKIP:
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        if not isinstance(data, dict):
            continue
        total, done = count(data)
        if total == 0:
            continue
        meta = data.get("_meta") or {}
        review = meta.get("review") or {}
        name = meta.get("quest") or path.name
        out.append(Entry(path, name, total, done,
                         review.get("translator"), review.get("proofread")))
    return out


def render(entries: list[Entry]) -> str:
    # 只有「AI 翻的」且「還沒校」的才是待辦。已翻完的優先，沒翻完的還在動，
    # 現在校也是白校。
    pending = [e for e in entries
               if e.translator == "claude" and not isinstance(e.proofread, str)
               and e.done == e.total]
    in_progress = [e for e in entries
                   if e.translator == "claude" and not isinstance(e.proofread, str)
                   and e.done < e.total]
    reviewed = [e for e in entries if isinstance(e.proofread, str)]
    unmarked = [e for e in entries if e.translator is None]

    pending.sort(key=lambda e: -e.total)
    reviewed.sort(key=lambda e: e.name)
    unmarked.sort(key=lambda e: -e.total)

    lines: list[str] = []
    add = lines.append

    add("# 校稿清單")
    add("")
    add(f"**待校稿 {len(pending)} 個檔、{sum(e.total for e in pending):,} 句。**"
        f" 已校稿 {len(reviewed)} 個。")
    add("")
    add("> 這份清單是**產生物**，跑 `python tools/proofread.py` 重產。不要手改。")
    add("")
    add("AI 翻得快，但快不等於對。語氣、雙關、角色口癖這些東西機器容易翻得")
    add("**通順但不對**——而通順反而更難被發現。所以每一份 AI 譯文都該由人再看一遍。")
    add("")
    add("---")
    add("")
    add("## 怎麼標記校稿完畢")
    add("")
    add("打開那個檔，把 `_meta.review.proofread` 從 `false` 改成**你的名字與日期**：")
    add("")
    add("```json")
    add('"review": {')
    add('  "translator": "claude",')
    add('  "proofread": "SCPNightsky 2026-08-30"')
    add("}")
    add("```")
    add("")
    add("然後重跑 `python tools/proofread.py`（或直接讓 CI 跑）。")
    add("")
    add("**改到一半也沒關係**——`proofread` 只要還是 `false`，它就會一直留在待辦上。")
    add("覺得某一條譯得不對就直接改，不需要問過我；這份清單的用意就是讓人有最後一票。")
    add("")
    add("---")
    add("")

    if pending:
        add("## 待校稿")
        add("")
        add("| 內容 | 句數 | 檔案 |")
        add("|---|---:|---|")
        for e in pending:
            add(f"| {e.name} | {e.total:,} | {e.link} |")
        add("")

    if in_progress:
        add("## 還在翻，先別校")
        add("")
        add("翻完之前校稿會白做工——這裡列出來只是讓你知道它們存在。")
        add("")
        add("| 內容 | 進度 | 檔案 |")
        add("|---|---:|---|")
        for e in in_progress:
            add(f"| {e.name} | {e.done:,} / {e.total:,} | {e.link} |")
        add("")

    if reviewed:
        add("## 已校稿")
        add("")
        add("| 內容 | 句數 | 校稿者 |")
        add("|---|---:|---|")
        for e in reviewed:
            add(f"| {e.name} | {e.total:,} | {e.proofread} |")
        add("")

    if unmarked:
        add("## 未標記譯者")
        add("")
        add("沒有 `_meta.review` 的檔。**這裡不代表已經校過**——")
        add("只代表沒有人記錄過它是誰翻的。多半是團隊自己翻的、或早期沒有這個欄位。")
        add("")
        add("<details><summary>展開（%d 個檔）</summary>" % len(unmarked))
        add("")
        add("| 內容 | 句數 | 檔案 |")
        add("|---|---:|---|")
        for e in unmarked:
            add(f"| {e.name} | {e.total:,} | {e.link} |")
        add("")
        add("</details>")
        add("")

    add("---")
    add("")
    add("## 散在共用檔裡的條目")
    add("")
    add("`misc.json`、`gui.json` 這類共用檔裡也有 AI 加的條目，但它們跟團隊翻的")
    add("混在同一個檔，沒辦法用檔層級標記。那些逐批記在 "
        "[CHANGELOG](../CHANGELOG.md) 裡——")
    add("每一則都寫著加了什麼、為什麼那樣譯。要校那部分請從 CHANGELOG 往回看。")
    add("")
    return "\n".join(lines) + "\n"


def main() -> int:
    check = "--check" in sys.argv
    entries = collect()
    if not entries:
        print("找不到任何譯文檔")
        return 1

    text = render(entries)
    old = OUT.read_text(encoding="utf-8") if OUT.is_file() else ""

    if check:
        if old != text:
            print("docs/PROOFREADING.md 過期了，跑 python tools/proofread.py")
            return 1
        print("校稿清單是最新的")
        return 0

    if old == text:
        print("校稿清單沒有變化")
        return 0

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text, encoding="utf-8", newline="\n")
    pending = sum(1 for e in entries
                  if e.translator == "claude"
                  and not isinstance(e.proofread, str)
                  and e.done == e.total)
    print(f"待校稿 {pending} 個檔 -> {OUT.relative_to(ROOT).as_posix()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
