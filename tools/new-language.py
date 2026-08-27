# -*- coding: utf-8 -*-
"""開一種新語言的資料夾。

用法：

    python tools/new-language.py zh_cn

做的事：把 ``translations/zh_tw/`` 整份複製成 ``translations/<語言>/``，
**把每一條 dst 清空**，計數歸零，然後跑一次 ``tools/update-docs.py``
讓 ``_languages.json`` 認得這個新語言。

為什麼不從 zh_tw 機器轉換過去
------------------------------
繁簡之間差的不只是字。「品質／质量」、「資料／数据」、「程式／程序」、
「網路／网络」——字元轉換器換不掉這些，換出來的是「用簡體字寫的台灣中文」。

更麻煩的是進度表。README 的進度是數「有幾條 dst 不是空的」，所以機器轉換
過去的那一刻，zh_cn 會顯示成跟 zh_tw 一樣的完成度——但沒有任何一個字
被人看過。那個數字會誤導所有人，包括想來幫忙的人：他們看到 30% 會以為
剩下的才是工作，實際上前面那 30% 也還沒校過。

空的比較誠實：翻過的就是翻過的。要拿 zh_tw 當底稿的人可以自己開著對照，
那是翻譯者的選擇，不該由這支工具替他決定。
"""
import io
import json
import re
import shutil
import subprocess
import sys
from collections import OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANG_ROOT = ROOT / "src/main/resources/assets/wynnchayuan/translations"
SOURCE = LANG_ROOT / "zh_tw"

# 語言資料夾的名字：zh_cn、ja_jp、en_us……
LANG_CODE = re.compile(r"[a-z]{2}_[a-z]{2}")


def blank(node):
    """把整份資料裡的 dst 清空，順便把 translated 計數歸零。"""
    if isinstance(node, dict):
        out = OrderedDict()
        for key, value in node.items():
            if key == "dst" and isinstance(value, str):
                out[key] = ""
            elif key == "translated" and isinstance(value, int):
                out[key] = 0
            else:
                out[key] = blank(value)
        return out
    if isinstance(node, list):
        return [blank(v) for v in node]
    return node


def blank_file(data):
    """一整份譯文檔清空。

    <p>語料有<b>兩種</b>格式：帶 ``entries`` 的完整格式（每一條有 src／dst／
    role…），以及 ``{"Earth": "地屬性"}`` 這種直接把原文當 key 的平鋪格式。

    <p>只處理前者的話，平鋪的那些檔（ui-labels、label、gui 的一部分）
    會原封不動帶著繁體譯文進到新語言裡——新語言一開張就顯示 3.4% 完成度，
    而那 3.4% 是別的語言的字。
    """
    if isinstance(data, dict) and "entries" in data:
        return blank(data)
    if isinstance(data, dict):
        out = OrderedDict()
        for key, value in data.items():
            out[key] = value if key.startswith("_") or not isinstance(value, str) \
                       else ""
        return out
    return blank(data)


def copy_tree(lang: str) -> tuple[int, int]:
    target = LANG_ROOT / lang
    files = 0
    entries = 0
    for src in sorted(SOURCE.rglob("*.json")):
        rel = src.relative_to(SOURCE)
        dst = target / rel
        dst.parent.mkdir(parents=True, exist_ok=True)

        data = json.load(io.open(src, encoding="utf-8"),
                         object_pairs_hook=OrderedDict)
        cleaned = blank_file(data)
        io.open(dst, "w", encoding="utf-8").write(
            json.dumps(cleaned, ensure_ascii=False, indent=1) + "\n")
        files += 1
        entries += len(cleaned.get("entries", {}))
    # 非 JSON 的附帶檔案（目前沒有，但將來加了不該被漏掉）
    for src in sorted(SOURCE.rglob("*")):
        if src.is_file() and src.suffix != ".json":
            dst = target / src.relative_to(SOURCE)
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)
    return files, entries


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print(__doc__)
        return 2
    lang = argv[0]
    if not LANG_CODE.fullmatch(lang):
        print(f"語言代碼要長成 xx_yy 的樣子（收到「{lang}」）。")
        return 2
    if (LANG_ROOT / lang).exists():
        print(f"{lang}/ 已經存在了，這支工具不會覆蓋既有的譯文。")
        return 2
    if not SOURCE.is_dir():
        print("找不到 zh_tw/，沒有東西可以當範本。")
        return 2

    files, entries = copy_tree(lang)
    print(f"{lang}/：{files} 個檔、{entries} 條，dst 全部留空")

    result = subprocess.run(
        [sys.executable, str(ROOT / "tools/update-docs.py")],
        cwd=ROOT, capture_output=True)
    print(result.stdout.decode("utf-8", "replace").strip())
    if result.returncode:
        print(result.stderr.decode("utf-8", "replace").strip())
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
