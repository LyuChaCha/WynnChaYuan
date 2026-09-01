#!/usr/bin/env python3
"""把語料檔的雜湊鍵換成看得懂的編號。

為什麼
------
抽取出來的語料是用<b>內容雜湊</b>當鍵的（``fea1ab0ad4dd``）。對程式夠用，
對翻譯的人卻是一串亂碼：打開檔案看不出這是第幾條、也看不出屬於哪一批，
更沒辦法跟同事說「你翻到第幾條了」。

``sort-corpus.py`` 已經把順序排成有意義的了（照 ctx，同一棵技能樹排在一起），
但鍵還是亂碼——這隻補上另外一半：照檔案裡<b>現在的順序</b>編號成
``gear-armour#0123``、``ability-mage#0007``。

安全嗎
------
鍵只是識別，<b>查表走的是 src</b>（見 TranslationStore#readWorkspace）。
改鍵不影響遊戲裡的任何一個字。

唯一會被鍵綁住的是 ``build.py`` 重跑時怎麼認回舊譯文——那邊已經改成照 src 認，
所以鍵可以自由改。這隻工具跑之前會擋下來確認這件事。

不會動的
--------
* 本來就看得懂的鍵（``Accretion Chain``、``The Sewers of Ragni#012``）——
  那些已經是好鍵了，換成號碼反而變差。混在同一個檔裡也沒關係：號碼取自
  它在檔案裡的<b>位置</b>，所以順序照樣讀得出來。
* ``quest/`` 底下的任務檔——那些早就是 ``任務名#012`` 了。
* 產生物（``_meta.generated`` 是 true 的）。

用法
----
    python tools/renumber-corpus.py            預覽
    python tools/renumber-corpus.py --write    寫回
    python tools/renumber-corpus.py --check    只檢查（CI 用，有亂碼鍵就非零退出）
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "src/main/resources/assets/wynnchayuan/translations"
LANG = re.compile(r"[a-z]{2}_[a-z]{2}")

# 內容雜湊：12 個十六進位字元，後面可以再接 #p0 這種段落序號
HASHKEY = re.compile(r"^[0-9a-f]{12}(#\w+)?$")


def files(base: Path) -> list[Path]:
    out = sorted(f for f in base.glob("*.json") if not f.name.startswith("_"))
    out += sorted(base.glob("ability/*.json"))
    return out                             # quest/ 不收：那邊早就是好鍵了


def stem_of(data: dict, path: Path) -> str:
    """編號的前綴。優先用 _meta.domain，沒有就用檔名。"""
    domain = (data.get("_meta") or {}).get("domain")
    if not isinstance(domain, str) or not domain.strip():
        domain = path.stem
    return domain.replace("/", "-").strip()


def renumber(path: Path) -> tuple[dict, int] | None:
    """回傳（改好的內容, 換掉幾個鍵）。不用改就回 None。"""
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"  ! {path.name} 讀不起來：{exc}", file=sys.stderr)
        return None
    if (data.get("_meta") or {}).get("generated") is True:
        return None                        # 產生物，改了也會被蓋回去
    rows = data.get("entries")
    if not isinstance(rows, dict):
        return None                        # 平鋪檔（鍵就是原文），本來就看得懂
    stem = stem_of(data, path)

    fresh: dict[str, object] = {}
    changed = 0
    for i, (key, entry) in enumerate(rows.items()):
        if key.startswith("_") or not HASHKEY.match(key):
            fresh[key] = entry             # 本來就看得懂的鍵不動
            continue
        # 號碼取自<b>位置</b>，所以就算同一個檔裡混著好鍵，順序照樣讀得出來
        new = f"{stem}#{i:04d}"
        if new in fresh:                   # 理論上不會撞，撞了寧可不改
            fresh[key] = entry
            continue
        fresh[new] = entry
        changed += 1
    if not changed:
        return None
    data["entries"] = fresh
    return data, changed


def main(argv: list[str]) -> int:
    write = "--write" in argv
    check = "--check" in argv

    # build.py 重跑時若還是照鍵認回舊譯文，改鍵會讓所有譯文變成「沒翻過」。
    # 那個改動是這隻工具的前提，所以在這裡擋一道。
    build = ROOT / "tools/build.py"
    if build.is_file() and "by_src" not in build.read_text(encoding="utf-8"):
        print("！ build.py 還是照鍵認回舊譯文。先把它改成照 src 認，"
              "否則重跑一次就會把所有譯文變成沒翻過。", file=sys.stderr)
        return 2

    bases = sorted(d for d in BASE.iterdir()
                   if d.is_dir() and LANG.fullmatch(d.name)) or [BASE]
    total = touched = 0
    for base in bases:
        for path in files(base):
            got = renumber(path)
            if not got:
                continue
            data, changed = got
            rel = path.relative_to(BASE).as_posix()
            print(f"  {rel:<38} {changed} 個鍵")
            total += changed
            touched += 1
            if write:
                path.write_text(
                    json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                    encoding="utf-8", newline="\n")

    if check:
        print(f"\n還有 {total} 個雜湊鍵" if total else "\n沒有雜湊鍵了")
        return 1 if total else 0
    print(f"\n{touched} 個檔、{total} 個鍵"
          + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
