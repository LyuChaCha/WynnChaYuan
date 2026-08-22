"""把玩家收集到的 `captured.json` 併進語料。

官方 CDN 只給裝備、素材與配方。介面文字、坐騎、粉末、Corkian、對話提示
<b>都不在裡面</b>——那些只能靠玩家在遊戲裡撞到才收得到。模組已經在收了
（`config/wynnchayuan/captured.json`），但收完之後沒有路可以進語料，
於是那個檔就躺在那裡。

這個工具補上那一段：讀 `captured.json`，把<b>語料還沒有</b>的條目
依 domain 分到對應的譯文檔，`dst` 留空給人翻。

用法：
    python tools/import-captured.py 某人的-captured.json
    python tools/import-captured.py 某人的-captured.json --write

分類規則見 `TARGET`。分不出來的一律進 `misc.json`——寧可集中在一個檔裡
等人重新歸類，也不要散進錯的檔案，那會讓載入順序莫名其妙地打架。
"""

from __future__ import annotations

import collections
import json
import sys
from pathlib import Path

TRANSLATIONS = Path("src/main/resources/assets/wynnchayuan/translations")

# domain -> 要放進哪個檔
TARGET = {
    "gui": "misc.json",
    "label": "npc.json",
    "npc": "npc.json",
    "chat": "misc.json",
    "quest": "quest.json",
    "item": "misc.json",
}

# 這些不值得收：純數字、純符號、太短
def worth_keeping(src: str) -> bool:
    if len(src.strip()) < 2:
        return False
    letters = sum(c.isalpha() for c in src)
    return letters >= 2


def existing() -> set[str]:
    """語料裡已經有的原文，含還沒翻的。"""
    seen: set[str] = set()
    for path in TRANSLATIONS.rglob("*.json"):
        if path.name.startswith("_"):
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        rows = data.get("entries")
        if isinstance(rows, dict):
            for key, value in rows.items():
                seen.add(value.get("src", key) if isinstance(value, dict) else key)
        else:
            seen.update(k for k in data if not k.startswith("_"))
    return seen


def main(argv: list[str]) -> int:
    write = "--write" in argv
    files = [a for a in argv if not a.startswith("--")]
    if not files:
        print(__doc__)
        return 2

    have = existing()
    by_file: dict[str, list[tuple[str, dict]]] = collections.defaultdict(list)
    skipped = 0
    for name in files:
        data = json.loads(Path(name).read_text(encoding="utf-8"))
        for key, entry in data.get("entries", {}).items():
            src = entry.get("src", "")
            if not src or src in have or not worth_keeping(src):
                skipped += 1
                continue
            have.add(src)                      # 同一批裡的重複只收一次
            target = TARGET.get(entry.get("domain"), "misc.json")
            by_file[target].append((key, entry))

    total = 0
    for target, rows in sorted(by_file.items()):
        path = TRANSLATIONS / target
        if not path.is_file():
            print(f"  ! 沒有 {target}，跳過 {len(rows)} 條")
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        flat = "entries" not in data
        print(f"  {target}  +{len(rows)} 條")
        for key, entry in rows[:5]:
            print(f"      {entry['src'][:64]!r}")
        if len(rows) > 5:
            print(f"      …另外 {len(rows) - 5} 條")
        if flat:
            for _, entry in rows:
                data[entry["src"]] = ""
        else:
            for key, entry in rows:
                data["entries"][key] = {
                    "src": entry["src"], "dst": "",
                    "role": entry.get("role", "desc"),
                    "kind": entry.get("kind") or "sentence",
                    "ctx": [entry.get("ctx", "captured")],
                    "from": "captured",
                }
        total += len(rows)
        if write:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                            encoding="utf-8", newline="\n")

    print()
    print(f"新增 {total} 條，跳過 {skipped} 條（語料已有或不值得收）"
          + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
