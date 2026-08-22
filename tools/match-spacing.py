"""讓譯文的半形間隔跟原文一致。

<h2>問題</h2>
原文在括號、符號前面留了一個<b>半形空格</b>，譯文卻把它吃掉了：

    src  {#} Area of Effect: {~} Blocks (Circle-Shaped)
    dst  {#}技能範圍: {~} 格(圓形)
            ^ 少一格            ^ 少一格

畫面上是「圖示緊貼中文」「數值緊貼括號」，跟旁邊沒被翻譯的行對不齊，
一眼就看得出來是兩套排版。

<h2>規則</h2>
只在<b>原文那個位置本來就有空格</b>時補：

<ul>
<li>`(` 前面——原文寫 `Blocks (Circle-Shaped)`，譯文就該是 `格 (圓形)`
<li>`{#}` 後面——原文寫 `{#} Effect:`，譯文就該是 `{#} 效果:`
</ul>

<p>原文沒有空格的地方一律不動。全形括號的譯文也不動——那是譯者的選擇，
這支只補間隔，不改標點。

用法：
    python tools/match-spacing.py
    python tools/match-spacing.py --write
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

BASE = pathlib.Path("src/main/resources/assets/wynnchayuan/translations")
NL = chr(10)

# 譯文裡「前面沒有空白」的半形左括號
TIGHT_PAREN = re.compile(r"(?<=[^\s(])\(")

# 譯文裡「後面直接接字」的 {#}
TIGHT_GLYPH = re.compile(r"\{#\}(?=[^\s({])")


def spaced(src: str, dst: str) -> str:
    fixed = dst
    if " (" in src:
        fixed = TIGHT_PAREN.sub(" (", fixed)
    if "{#} " in src:
        fixed = TIGHT_GLYPH.sub("{#} ", fixed)
    return fixed


def main(argv: list[str]) -> int:
    write = "--write" in argv
    changed = 0
    for path in sorted(BASE.rglob("*.json")):
        if path.name.startswith("_"):
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        rows = data.get("entries")
        if not isinstance(rows, dict):
            continue
        touched = []
        for key, entry in rows.items():
            if not isinstance(entry, dict):
                continue
            src, dst = entry.get("src", ""), entry.get("dst", "")
            if not src or not dst:
                continue
            fixed = spaced(src, dst)
            if fixed == dst:
                continue
            touched.append((src, dst, fixed))
            entry["dst"] = fixed
        if not touched:
            continue
        print(f"  {path.relative_to(BASE)}  {len(touched)} 條")
        for src, before, after in touched[:2]:
            print(f"      原文 {src.splitlines()[0][:58]!r}")
            print(f"      舊譯 {before.splitlines()[0][:58]!r}")
            print(f"      新譯 {after.splitlines()[0][:58]!r}")
        changed += len(touched)
        if write:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + NL,
                            encoding="utf-8", newline=NL)

    print()
    print(f"補了 {changed} 條的半形間隔"
          + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
