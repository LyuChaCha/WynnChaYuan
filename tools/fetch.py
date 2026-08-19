#!/usr/bin/env python3
"""下載 Wynntils 的靜態資料庫到 raw/。

這些是 Wynntils 自己在用的官方資料來源（cdn.wynntils.com），
涵蓋全部裝備、技能、材料等，不需要玩家在遊戲裡逐一遇到。

用法:  python tools/fetch.py
"""
import json
import sys
import urllib.request
from pathlib import Path

RAW = Path(__file__).resolve().parent.parent / "raw"

# id -> 檔名。取自 Wynntils 內建的 assets/wynntils/urls.json
SOURCES = {
    "abilities_v2": "https://cdn.wynntils.com/static/Reference/abilities_v2.json",
    "gear":         "https://cdn.wynntils.com/static/Reference/gear.json",
    "ingredients":  "https://cdn.wynntils.com/static/Reference/ingredients.json",
    "materials":    "https://cdn.wynntils.com/static/Reference/materials.json",
    "tomes":        "https://cdn.wynntils.com/static/Reference/tomes.json",
    "aspects":      "https://cdn.wynntils.com/static/Reference/aspects.json",
    "charms":       "https://cdn.wynntils.com/static/Reference/charms.json",
    "tools":        "https://cdn.wynntils.com/static/Reference/tools.json",
    "sets":         "https://cdn.wynntils.com/static/Reference/sets.json",
    "places":       "https://cdn.wynntils.com/static/Reference/places.json",
    "services":     "https://cdn.wynntils.com/static/Reference/services.json",
}


def fetch(name: str, url: str) -> None:
    dest = RAW / f"{name}.json"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "wynn-corpus/1.0"})
        with urllib.request.urlopen(req, timeout=60) as resp:
            payload = resp.read()
        json.loads(payload)  # 驗證是合法 JSON 再落地
        dest.write_bytes(payload)
        print(f"  {name:14s} {len(payload):>9,} bytes")
    except Exception as exc:  # noqa: BLE001
        print(f"  {name:14s} 失敗: {exc}", file=sys.stderr)


def main() -> None:
    RAW.mkdir(parents=True, exist_ok=True)
    print(f"下載到 {RAW}")
    for name, url in SOURCES.items():
        fetch(name, url)
    print("完成。接著執行: python tools/build.py")


if __name__ == "__main__":
    main()
