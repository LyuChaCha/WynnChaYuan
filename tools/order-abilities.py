#!/usr/bin/env python3
"""照技能樹的<b>實際版面</b>重排 ability.json。

為什麼不是重新 capture
----------------------
官方 CDN 的每個技能節點都帶 `location: {page, row, col}`——那就是它在技能樹
畫面上的位置。照 `(職業, page, row, col)` 排序，得到的就是玩家看到的
「由上而下、由左而右」，比在遊戲裡逐格截圖準確也快得多。

順序寫成 `tree` 欄位存進檔案裡，所以：
  * 譯者在檔案裡看到的順序就是技能樹的順序
  * `sort-corpus.py` 之後會照這個欄位排，不必再連網

用法:  python tools/order-abilities.py
"""
from __future__ import annotations

import json
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ABILITY = ROOT / "src/main/resources/assets/wynnchayuan/translations/ability.json"
URL = "https://cdn.wynntils.com/static/Reference/abilities_v2.json"

# 技能樹在遊戲裡的職業順序
CLASS_ORDER = ["warrior", "mage", "archer", "assassin", "shaman"]


def fetch_layout() -> dict[str, str]:
    """`職業/技能名` → 可排序的字串鍵。"""
    req = urllib.request.Request(URL, headers={"User-Agent": "WynnChaYuan"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        data = json.loads(resp.read())

    layout: dict[str, str] = {}
    for cls, payload in data.items():
        rank = CLASS_ORDER.index(cls) if cls in CLASS_ORDER else 99
        for node in payload.get("nodes", []):
            loc = node.get("location") or {}
            name = node.get("name") or ""
            if not name:
                continue
            # 補零成固定寬度，字串排序才等於數字排序
            layout[f"{cls}/{name}"] = (
                f"{rank:02d}/{cls}/{loc.get('page', 0):02d}"
                f"/{loc.get('row', 0):03d}/{loc.get('col', 0):03d}")
    return layout


def ctx_list(entry: dict) -> list[str]:
    ctx = entry.get("ctx") or []
    return ctx if isinstance(ctx, list) else [ctx]


def main() -> int:
    layout = fetch_layout()
    print(f"CDN 上共 {len(layout)} 個技能節點")

    data = json.loads(ABILITY.read_text(encoding="utf-8"))
    entries = data["entries"]

    stamped = 0
    for entry in entries.values():
        # 一條說明可能被好幾個技能共用，取版面上<b>最前面</b>的那個當代表，
        # 它才會固定落在第一次會用到它的地方
        keys = [layout[c] for c in ctx_list(entry) if c in layout]
        if not keys and entry.get("role") == "name":
            # 技能<b>名稱</b>那類的 ctx 是「職業/技能名稱」這個固定字串，
            # 對不上版面表——但它的 src 本身就是技能名，用那個查
            for c in ctx_list(entry):
                cls = str(c).split("/")[0]
                hit = layout.get(f"{cls}/{entry.get('src', '')}")
                if hit:
                    keys.append(hit)
        if keys:
            entry["tree"] = min(keys)
            stamped += 1
        else:
            entry["tree"] = "zz"       # 對不上的排在最後，一眼看得出來
    print(f"標上版面順序 {stamped} / {len(entries)} 條")

    role_order = {"name": 0, "stat": 1, "desc": 2}
    ordered = sorted(entries.items(),
                     key=lambda kv: (kv[1]["tree"],
                                     role_order.get(kv[1].get("role", ""), 9),
                                     kv[1].get("src", "")))
    data["entries"] = dict(ordered)
    data.setdefault("_meta", {})["note"] = (
        "照技能樹的實際版面排序（職業 → 頁 → 列 → 欄），"
        "也就是遊戲裡由上而下、由左而右的順序。tree 欄位由 "
        "tools/order-abilities.py 產生。")
    ABILITY.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                       encoding="utf-8")

    print("\n前 12 條：")
    for entry in list(data["entries"].values())[:12]:
        print(f"  {entry['tree']:<28} {entry.get('role','?'):5s} {entry['src'][:36]!r}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
