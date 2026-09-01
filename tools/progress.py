#!/usr/bin/env python3
"""看翻譯進度，並列出「接下來最值得翻的」。

譯者打開 gear-weapon.json 會看到 2,769 條空的 dst，完全不知道從哪開始。
這個工具的用途就是回答那個問題。

用法:
    python tools/progress.py              # 各檔進度
    python tools/progress.py --next 30    # 列出建議優先翻的 30 條
    python tools/progress.py --file npc   # 只看某個檔
"""
from __future__ import annotations

import argparse
import io
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw"

# 玩家實際看到的頻率大致如此：介面標籤每開一次背包就看到，
# 裝備傳說敘述可能一輩子看不到幾次。翻譯順序照這個排效益最高。
PRIORITY = {
    "ui-labels": 0,
    "npc": 1,
    "quest": 2,
    "ability": 3,
    "ingredient": 4,
    "material": 5,
    "gear-weapon": 6,
    "gear-armour": 7,
    "gear-accessory": 8,
    "tome": 9,
    "aspect": 10,
    "charm": 11,
}


def load(path: Path) -> list[dict]:
    """統一成 [{src, dst, role, kind}] 的形式，兩種檔案格式都吃。"""
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as exc:
        print(f"  ! {path.name} 讀取失敗: {exc}")
        return []

    if "entries" in data:
        # `keep: en` 是<b>刻意</b>保留原文的條目（技能名稱），不是還沒翻。
        # 算進分母的話，那幾個檔案永遠停在八成，看不出真正還有多少要做。
        return [
            {"src": v.get("src", ""), "dst": v.get("dst", ""),
             "role": v.get("role", ""), "kind": v.get("kind", "")}
            for v in data["entries"].values() if not v.get("keep")
        ]
    return [
        {"src": k, "dst": v, "role": "", "kind": ""}
        for k, v in data.items() if not k.startswith("_")
    ]


def files() -> list[Path]:
    """所有譯文檔，含子資料夾。

    技能樹依職業拆成 `ability/mage.json` 那樣之後，只掃頂層就會<b>整片漏掉</b>——
    進度表看起來憑空少了一千多條，而漏掉的偏偏是正在動工的那一塊。
    任務對話（`quest/`）與祕密發現（`secret/`）也在子資料夾裡，但它們各自有
    合併檔，兩邊都算會重複，所以照舊排除。
    """
    found = [p for p in sorted(TRANSLATIONS.rglob("*.json"))
             if not p.name.startswith("_")
             and p.parent.name not in ("quest", "secret")]
    return sorted(found, key=lambda p: PRIORITY.get(p.stem, 99))


def report() -> None:
    print(f"{'檔案':<20}{'進度':>18}  {'已翻':>6} / {'總數':>6}")
    print("-" * 56)
    total = done = 0
    for path in files():
        rows = load(path)
        if not rows:
            continue
        d = sum(1 for r in rows if r["dst"].strip())
        total += len(rows)
        done += d
        pct = d / len(rows) * 100
        bar = "█" * int(pct / 5) + "░" * (20 - int(pct / 5))
        print(f"{path.stem:<20}{bar} {pct:5.1f}%  {d:>6,} / {len(rows):>6,}")
    print("-" * 56)
    if total:
        print(f"{'合計':<20}{'':>18}  {done:>6,} / {total:>6,}"
              f"   ({done / total * 100:.1f}%)")


def suggest(limit: int, only: str | None) -> None:
    """列出建議接下來翻的條目。

    排序理由：檔案優先度（玩家看到的頻率）→ 短詞優先。
    短詞多半是重複出現的標籤，翻一條在很多地方生效；
    長敘述翻起來慢又只出現一次，留到後面。
    """
    candidates = []
    for path in files():
        if only and path.stem != only:
            continue
        rank = PRIORITY.get(path.stem, 99)
        for row in load(path):
            if not row["dst"].strip():
                candidates.append((rank, len(row["src"]), path.stem, row["src"]))

    candidates.sort()
    if not candidates:
        print("沒有待翻的條目了。")
        return

    print(f"建議接下來翻的 {min(limit, len(candidates))} 條"
          f"（還剩 {len(candidates):,} 條）\n")
    current = None
    for _, _, stem, src in candidates[:limit]:
        if stem != current:
            current = stem
            print(f"── {stem}.json")
        shown = src.replace("\n", " ⏎ ")
        print(f"   {shown[:78]}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--next", type=int, metavar="N", help="列出建議優先翻的 N 條")
    ap.add_argument("--file", metavar="NAME", help="只看某個檔（不含 .json）")
    args = ap.parse_args()

    if not TRANSLATIONS.is_dir():
        print(f"找不到譯文資料夾：{TRANSLATIONS}")
        return
    if args.next:
        suggest(args.next, args.file)
    else:
        report()


if __name__ == "__main__":
    main()
