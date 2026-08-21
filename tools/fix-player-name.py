"""把抓取階段被吃掉的玩家名字補回 src。

維基的對話表格裡，玩家名字那一格是空的。抓下來就變成
「...ah. . So you are needed here too」——中間那個位置本來是玩家的名字。

這不只是難看：遊戲裡那個位置會被抽成 `{u}` 佔位符，所以 src 少了 `{u}`
的條目<b>永遠對不上</b>，不管翻得多好都不會顯示。

判斷方式全部是「英文本來不會這樣寫」的標點組合：
  * 逗號前面有空白        `Urgh... , please`   -> 名字在逗號前
  * 逗號後面緊接著標點    `Listen, . Achara`   -> 名字在逗號後
  * 兩個標點中間空一格    `...ah. . So`
  * 整句以單一標點開頭    `. I think.`

刻意不動的：以 `...` 開頭的句子（那是正常的語氣停頓，不是被吃掉的名字）。

用法：
    python tools/fix-player-name.py           # 只報告
    python tools/fix-player-name.py --write   # 實際寫回
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

QUESTS = Path("src/main/resources/assets/wynnchayuan/translations/quest")

RULES = [
    # 逗號前面有空白：名字被吃掉了，補在逗號前
    (re.compile(r"(\S) ,"), r"\1 {u},"),
    # 逗號後面緊接著標點
    (re.compile(r", (?=[.!?])"), ", {u}"),
    # 兩個標點中間空一格
    (re.compile(r"([.!?]) (?=[.!?] )"), r"\1 {u}"),
    # 整句以單一標點開頭。`...` 開頭是正常的停頓，不能動
    (re.compile(r"^([.!?])(?![.!?]) "), r"{u}\1 "),
    (re.compile(r"^(!!|\?!) "), r"{u}\1 "),
]


# 幾乎沒有字母的行是刻意打亂的台詞（`..... .. .....`），那種殘缺是氣氛的一部分
MIN_LETTERS = 3


def garbled(text: str) -> bool:
    """這一行本來就是壞的嗎。

    `.;I. {u},told;.` 這種是<b>刻意</b>寫壞的台詞，殘缺本身就是氣氛。
    它字母夠多，逃得過 MIN_LETTERS，但標點比字母還多——正常英文不會這樣。
    把它「修好」等於把演出抹掉。
    """
    letters = sum(c.isalpha() for c in text)
    marks = sum(not c.isalnum() and not c.isspace() for c in text)
    return letters < MIN_LETTERS or marks >= letters


def fixed(text: str) -> str:
    if garbled(text):
        return text
    out = text
    for pattern, repl in RULES:
        out = pattern.sub(repl, out)
    # 一行補超過一個，多半不是玩家名字而是別的東西被吃掉了——
    # 「There's five of them. Strength, , Intelligence, , and Agility」
    # 少的是元素名。這種寧可不動，留給人工判斷。
    if out.count("{u}") - text.count("{u}") > 1:
        return text
    return out


def main(argv: list[str]) -> int:
    write = "--write" in argv
    total = 0
    touched = 0
    for path in sorted(QUESTS.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        changed = 0
        for entry in data["entries"].values():
            before = entry["src"]
            after = fixed(before)
            if after == before:
                continue
            entry["src"] = after
            changed += 1
            if total < 12:
                print(f"  {path.name}")
                print(f"    - {before[:78]}")
                print(f"    + {after[:78]}")
            total += 1
        if changed and write:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                            encoding="utf-8", newline="\n")
            touched += 1
    print()
    print(f"{total} 條的玩家名字補回來了，散在 {touched or '（未寫入）'} 個檔案")
    if not write:
        print("這是預覽。加上 --write 才會實際寫回。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
