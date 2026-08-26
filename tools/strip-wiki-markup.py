"""清掉抓取階段沒清乾淨的維基標記。

殘留的標記讓那些條目<b>永遠對不上</b>遊戲裡的文字——`{{RenderLocation`、
`[[連結|顯示文字]]`、多出來的 `}}`，遊戲畫面上都不存在。

用法：
    python tools/strip-wiki-markup.py           # 只報告
    python tools/strip-wiki-markup.py --write
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

QUESTS = Path("src/main/resources/assets/wynnchayuan/translations/zh_tw/quest")

# `{{模板` 之後的東西在畫面上都不存在
TEMPLATE_TAIL = re.compile(r"\s*\{\{.*$", re.DOTALL)
# `[[目標|` 後面已經接著顯示文字（多半以 `[` 開頭）就整個丟掉，
# 否則拿目標名當顯示文字——`[[Gold Chunk|` 指的就是 Gold Chunk
LINK_OPEN = re.compile(r"\[\[([^\]|]*)\|")
LINK_PLAIN = re.compile(r"\[\[([^\]|]*)\]\]")


def clean(text: str) -> str:
    out = LINK_PLAIN.sub(lambda m: m.group(1).split("#")[-1], text)

    def open_link(match: re.Match[str]) -> str:
        after = out[match.end():].lstrip()
        if after.startswith(("[", "{")):
            return ""                      # 顯示文字就跟在後面
        return match.group(1).split("#")[-1]

    out = LINK_OPEN.sub(open_link, out)
    out = TEMPLATE_TAIL.sub("", out)
    out = out.replace("]]]", "]").replace("]]", "")
    out = out.replace("'''", "")
    while out.endswith("}}"):
        out = out[:-2].rstrip()
    return out.strip()


def main(argv: list[str]) -> int:
    write = "--write" in argv
    total = 0
    for path in sorted(QUESTS.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        changed = False
        for entry in data["entries"].values():
            before = entry["src"]
            after = clean(before)
            if after == before:
                continue
            print(f"  {path.name}")
            print(f"    - {before[:88]}")
            print(f"    + {after[:88]}")
            entry["src"] = after
            changed = True
            total += 1
        if changed and write:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                            encoding="utf-8", newline="\n")
    print()
    print(f"清掉 {total} 條的殘留標記" + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
