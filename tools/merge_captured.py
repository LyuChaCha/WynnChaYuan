#!/usr/bin/env python3
"""把玩家收集到的 captured.json 併進翻譯工作檔。

任務對話與 NPC 名稱沒有官方資料可抓，只能靠玩家在遊戲裡遇到才會被記下來。
這個工具把那些收集結果變成大家共用的待翻條目。

用法:
    python tools/merge_captured.py <captured.json 的路徑>
    python tools/merge_captured.py captured.json --dry-run    # 只看會加什麼

已經填好的 dst 一律不動——只補新條目。
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"

# captured.json 的 domain 對應到哪個工作檔
DOMAIN_TO_FILE = {
    "quest": "quest.json",
    "npc": "npc.json",
    "chat": "quest.json",      # 系統訊息多半與任務流程相關，併在一起
}


def load_flat(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as exc:
        print(f"  ! {path.name} 讀取失敗，略過: {exc}", file=sys.stderr)
        return {}


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("captured", help="captured.json 的路徑")
    ap.add_argument("--dry-run", action="store_true", help="只顯示，不寫檔")
    args = ap.parse_args()

    src = Path(args.captured)
    if not src.exists():
        print(f"找不到 {src}")
        return
    try:
        data = json.loads(src.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as exc:
        print(f"讀取失敗: {exc}")
        return

    entries = data.get("entries", {})
    if not entries:
        print("captured.json 裡沒有任何條目。")
        return

    # 兩種排序，因為兩種內容的翻譯方式不一樣：
    #
    #   任務對話 → 照<b>收集順序</b>。同一段對話的十幾句話必須排在一起，
    #              而且要照原本的先後——譯者看不出上下文就翻不對語氣，
    #              也分不清誰在回答誰。
    #   其他     → 照<b>出現次數</b>由多到少。那些是各自獨立的短詞，
    #              先翻常看到的效益最高。
    by_file: dict[str, list[tuple[int, str]]] = {}
    for v in entries.values():
        target = DOMAIN_TO_FILE.get(v.get("domain"))
        text = v.get("src", "").strip()
        if not target or not text:
            continue
        order = (v.get("seq", 0) if v.get("domain") == "quest"
                 else -v.get("seen", 1))
        by_file.setdefault(target, []).append((order, text))

    total_added = 0
    for name, rows in sorted(by_file.items()):
        path = TRANSLATIONS / name
        existing = load_flat(path)
        added = []
        for order, text in sorted(rows):
            if text not in existing:
                added.append((order, text))

        if not added:
            print(f"{name}: 沒有新條目")
            continue

        print(f"\n{name}: 新增 {len(added)} 條")
        for order, text in added[:8]:
            shown = text.replace("\n", " ⏎ ")
            print(f"   {shown[:70]}")
        if len(added) > 8:
            print(f"   … 另外 {len(added) - 8} 條")

        if not args.dry_run:
            for _, text in added:
                existing[text] = ""          # 留空等人翻
            path.write_text(
                json.dumps(existing, ensure_ascii=False, indent=1) + "\n",
                encoding="utf-8")
        total_added += len(added)

    print(f"\n合計新增 {total_added} 條"
          + ("（--dry-run，沒有寫檔）" if args.dry_run else ""))


if __name__ == "__main__":
    main()
