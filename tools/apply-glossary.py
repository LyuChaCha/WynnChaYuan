"""對照表改了之後，把既有譯文一起改掉。

`GLOSSARY.md` 是準則。但改了對照表，已經翻好的幾千條譯文不會自己跟著變——
於是同一個詞在新舊譯文裡並存，而這種不一致在遊戲裡<b>看不出來</b>，
只有玩家會覺得「怎麼一下叫這個一下叫那個」。

用法（可以給很多組）：

    python tools/apply-glossary.py "作用範圍=技能範圍"
    python tools/apply-glossary.py --write "作用範圍=技能範圍" "範圍=施放範圍"

加上 `--src 英文詞` 可以限定「只改原文含有那個英文詞的條目」，
避免「範圍」這種到處都是的字被誤改：

    python tools/apply-glossary.py --write --src "Area of Effect" "作用範圍=技能範圍"
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

TRANSLATIONS = Path("src/main/resources/assets/wynnchayuan/translations/zh_tw")


def files() -> list[Path]:
    found = sorted(p for p in TRANSLATIONS.rglob("*.json")
                   if not p.name.startswith("_"))
    return found


def rows(data: dict):
    """回傳 (src, 取 dst, 設 dst) 三元組，兩種檔案結構都吃。"""
    entries = data.get("entries")
    if isinstance(entries, dict):
        for key, value in entries.items():
            if isinstance(value, dict):
                yield value.get("src", key), value.get("dst", ""), value
    else:
        for key, value in data.items():
            if not key.startswith("_") and isinstance(value, str):
                yield key, value, None


def main(argv: list[str]) -> int:
    write = "--write" in argv
    argv = [a for a in argv if a != "--write"]
    need_src = None
    if "--src" in argv:
        at = argv.index("--src")
        need_src = argv[at + 1]
        del argv[at:at + 2]
    pairs = []
    for arg in argv:
        if "=" not in arg:
            print(f"看不懂的參數：{arg}（要寫成 舊譯=新譯）")
            return 2
        old, new = arg.split("=", 1)
        pairs.append((old, new))
    if not pairs:
        print(__doc__)
        return 2

    total = 0
    for path in files():
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        changed = False
        flat_changes: dict[str, str] = {}
        for src, dst, holder in rows(data):
            if not dst:
                continue
            if need_src and need_src not in src:
                continue
            fixed = dst
            for old, new in pairs:
                if old in fixed and new not in fixed:
                    fixed = fixed.replace(old, new)
            if fixed == dst:
                continue
            if total < 10:
                print(f"  {path.name} :: {src[:52]}")
                print(f"    - {dst[:72]}")
                print(f"    + {fixed[:72]}")
            total += 1
            changed = True
            if holder is not None:
                holder["dst"] = fixed
            else:
                flat_changes[src] = fixed
        for key, value in flat_changes.items():
            data[key] = value
        if changed and write:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                            encoding="utf-8", newline="\n")

    print()
    print(f"{total} 條譯文跟著對照表改了" + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
