#!/usr/bin/env python3
"""語料裡有沒有夾帶別人的名字。

為什麼三道濾網之外還要這一道
----------------------------
模組端、收集站、匯入工具三道濾網擋的都是「<b>要進來</b>的東西」。但濾網是
一路補出來的——每補一次，就表示在那之前有東西穿了過去，而<b>穿過去的那些
已經在倉庫裡了</b>，沒有任何檢查會回頭看它們一眼。

實際掃過一次，公開倉庫裡躺著七條：

    {#} {#}Thank Changa Flavour
    {#} {#}Changa Flavour has thrown a
    ✔ Reisen Plank has opted in
    ✔ {~} dmg Guardian has opted in
    Leader: player{~}
    Hyedam_{~}
    {#} HEYAZero would like to trade! …

七條的譯文都是空的——沒有人會去翻它們，也就永遠不會有人發現。

所以這一道看的是<b>已經在倉庫裡</b>的東西。濾網管入口，這裡管存量。

用法：
    python tools/check-leaks.py           # 掃過就好
    python tools/check-leaks.py --write   # 直接刪掉掃到的
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"

# 只收<b>句型</b>，不收名字。
#
# 名字沒有形狀可以認——`Changa Flavour` 跟 `Cloud Tavern` 在結構上分不出來。
# 但「某某 has opted in」這種句子是遊戲寫死的模板，主詞永遠是別人，
# 而且<b>翻了也沒有用</b>：那一行每個人看到的都不一樣。
#
# 跟模組端的 PlayerDataFilter、收集站的 LOOKS_PERSONAL、匯入工具的 NAMED
# 是同一批句型。四邊都要有，因為四邊管的是不同的時機。
SHAPES = [
    (r"\shas thrown a", "誰丟了炸彈"),
    (r"Loot Bomb has expired", "誰的炸彈到期了"),
    (r"(?m)^(?:\{#\}\s*)+Thank ", "謝謝某某"),
    (r"\shas opted in", "誰報名了"),
    (r"\shas chosen the", "誰選了增益"),
    (r"\shas given you\s", "誰給了你東西"),
    (r"(?m)^Leader:\s", "隊長是誰"),
    (r"would like to trade", "誰要跟你交易"),
    (r"\sshouts:", "誰在喊話"),
    (r"has logged into server", "誰上線了"),
    (r"\sis now (?:online|offline)", "誰上下線了"),
    (r"(?m)^(?:\{#\})*\S+ has died", "誰死了"),
    (r"['’]s Totem of Tales", "誰的石碑"),
    (r"\bControlled by\b", "誰的公會佔著這塊地"),
    (r"[A-Za-z]{2,}_\{~\}", "帳號名裡的底線加數字"),
]

# 遊戲自己的說明裡會出現這些句型，但帶的是<b>字面的佔位符</b>而不是真名。
#
#   {#} Use "/trade <name>" to safely trade items with other players!
#
# 那是該收的內容——教學訊息，每個人看到的都一樣。
ALLOW = ("<name>", "{u}")


def rows(path: Path):
    """檔案裡的 (鍵, 原文)。兩種格式都認。"""
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return
    entries = data.get("entries")
    if isinstance(entries, dict):
        for key, value in entries.items():
            if isinstance(value, dict) and isinstance(value.get("src"), str):
                yield key, value["src"]
        return
    for key, value in data.items():
        if not key.startswith("_") and isinstance(value, str):
            yield key, key


def drop(path: Path, keys: set[str]) -> int:
    """把掃到的那幾條從檔案裡拿掉。"""
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = data.get("entries")
    gone = 0
    if isinstance(entries, dict):
        for key in list(entries):
            if key in keys:
                del entries[key]
                gone += 1
        meta = data.get("_meta")
        if isinstance(meta, dict) and "count" in meta:
            meta["count"] = len(entries)
    else:
        for key in list(data):
            if key in keys:
                del data[key]
                gone += 1
    if gone:
        path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                        encoding="utf-8", newline="\n")
    return gone


def main(argv: list[str]) -> int:
    write = "--write" in argv
    compiled = [(re.compile(p), why) for p, why in SHAPES]

    found: dict[Path, set[str]] = {}
    total = 0
    for path in sorted(TRANSLATIONS.rglob("*.json")):
        if path.name.startswith("_"):
            continue
        for key, src in rows(path):
            if any(a in src for a in ALLOW):
                continue
            for pattern, why in compiled:
                if pattern.search(src):
                    rel = path.relative_to(TRANSLATIONS).as_posix()
                    print(f"  [{rel}] {why}")
                    print(f"      {json.dumps(src, ensure_ascii=False)[:100]}")
                    found.setdefault(path, set()).add(key)
                    total += 1
                    break

    if not total:
        print("語料裡沒有夾帶別人的名字。")
        return 0

    if not write:
        print(f"\n掃到 {total} 條夾帶玩家名的條目。")
        print("跑 python tools/check-leaks.py --write 刪掉它們。")
        return 1

    gone = 0
    for path, keys in found.items():
        gone += drop(path, keys)
    print(f"\n刪掉 {gone} 條。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
