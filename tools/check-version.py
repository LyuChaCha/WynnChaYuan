#!/usr/bin/env python3
"""version.json 有沒有跟著版本號一起更新。

為什麼要擋
----------
``version.json`` 是「現在最新是哪一版」的線上真相：模組啟動時讀它，不一樣就
告訴玩家有新版可以更新（見 ``Releases``）。所以它一旦忘了改，後果是**沉默的**
——玩家永遠不會被通知，而沒有任何東西會壞掉、沒有任何紅燈會亮。

同一份也負責 F6 的「本版更新內容」。少了這一版的 ``notes``，那個畫面就是一句
「這一版沒有附更新說明」——不會當掉，只是空的。

三件事都是「忘了做也不會壞」的那一類，所以只能靠檢查擋。

用法：
    python tools/check-version.py
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSION_FILE = ROOT / "version.json"
GRADLE = ROOT / "gradle.properties"

# Beta 期間的寫法：0.1.0 或 0.1.0_1。見 README 的「版本號」。
SHAPE = re.compile(r"^\d+\.\d+\.\d+(?:_\d+)?$")


def mod_version() -> str | None:
    for line in GRADLE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line.startswith("mod_version="):
            return line.split("=", 1)[1].strip()
    return None


def main() -> int:
    problems: list[str] = []

    running = mod_version()
    if not running:
        print("gradle.properties 裡找不到 mod_version")
        return 1

    try:
        data = json.loads(VERSION_FILE.read_text(encoding="utf-8"))
    except Exception as e:                                  # noqa: BLE001
        print(f"version.json 讀不起來：{e}")
        return 1

    latest = data.get("latest", "")
    if latest != running:
        problems.append(
            f"version.json 的 latest 是 {latest!r}，"
            f"gradle.properties 的 mod_version 是 {running!r} —— 兩邊要一致，"
            f"不然發了新版玩家不會被通知")

    if not SHAPE.match(running):
        problems.append(
            f"版本號 {running!r} 不符合 Beta 期間的寫法（0.1.0 或 0.1.0_1）")

    notes = data.get("notes", {})
    if running not in notes:
        problems.append(
            f"version.json 的 notes 少了 {running!r} 那一版 —— "
            f"F6 的「本版更新內容」會是空的")
    else:
        one = notes[running]
        if not one.get("headline", "").strip():
            problems.append(f"notes[{running!r}] 少了 headline")
        if not one.get("items"):
            problems.append(f"notes[{running!r}] 少了 items")

    download = data.get("download", {})
    for key in ("curseforge", "modrinth", "github"):
        url = download.get(key, "")
        if url and not url.startswith("https://"):
            problems.append(f"download.{key} 不是 https 開頭：{url!r}")
    if not any(download.get(k) for k in ("curseforge", "modrinth", "github")):
        problems.append("download 裡一個下載頁都沒有")

    if problems:
        for p in problems:
            print(f"  [錯誤] {p}")
        print(f"\nversion.json：{len(problems)} 個問題")
        return 1
    print(f"version.json 跟 mod_version（{running}）一致，"
          f"更新說明 {len(notes[running].get('items', []))} 條")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
