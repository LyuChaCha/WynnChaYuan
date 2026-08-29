#!/usr/bin/env python3
"""找出「已經翻好、卻因為原文差一點而永遠對不上」的句子。

為什麼需要
----------
任務對話大多抓自 wiki，而 wiki 是人打的——標點、大小寫、省略號的個數、
一個 typo，都可能跟遊戲實際送出的字串不一樣。差一個字元，查表就完全落空。

這種情況在畫面上跟「還沒翻」<b>一模一樣</b>：顯示原文。所以它會一直躺在那裡，
沒有人發現我們其實早就把那句翻好了。

這個工具把玩家收集到的 `captured.json`（＝遊戲真正送出的字串）拿去跟語料比對：

* 完全命中的 → 語料已經有了，跳過
* **極為接近**的 → 這就是我們要找的：譯文已經存在，只是 `src` 對不上
* 差很遠的 → 真的是新句子，交給 `import-captured.py` 收進去

用法
----
    python tools/near-miss.py 某人的-captured.json
    python tools/near-miss.py 某人的-captured.json --write   # 直接把 src 改成遊戲版本

`--write` 只動<b>已經有 dst</b> 的條目，而且只改 `src`。沒有譯文的條目不碰——
那種情況該走 import-captured.py，不是在這裡硬湊。
"""

from __future__ import annotations

import argparse
import difflib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw"

# 相似度門檻。
#
# 0.90 以下開始出現「兩句不同的台詞剛好結構像」的誤判——尤其是任務目標行
# （`Talk to X at [...]` 這種），它們彼此之間本來就只差一個名字。
# 寧可漏掉幾句讓人自己找，也不要把譯文接到錯的原文上。
THRESHOLD = 0.90

# 太短的句子不比。`...`、`Hm?`、`Yes.` 這種，任兩句的相似度都會很高，
# 而它們本來就是重複出現的通用短語，接錯了也看不出來。
MIN_LENGTH = 12


def corpus_files() -> list[Path]:
    files = sorted(TRANSLATIONS.glob("quest/*.json"))
    files += sorted(TRANSLATIONS.glob("*.json"))
    files += sorted(TRANSLATIONS.glob("ability/*.json"))
    return [f for f in files if f.name != "quest-dialogue.json"]


def load_corpus() -> tuple[dict[str, tuple[Path, str, str]], set[str]]:
    """@return (src -> (檔案, key, dst))，以及所有 src 的集合"""
    index: dict[str, tuple[Path, str, str]] = {}
    for path in corpus_files():
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:                                     # noqa: BLE001
            continue
        entries = data.get("entries", data)
        if not isinstance(entries, dict):
            continue
        for key, value in entries.items():
            if isinstance(value, dict):
                src, dst = value.get("src"), value.get("dst", "")
            else:
                src, dst = key, value
            if isinstance(src, str) and src.strip():
                index.setdefault(src, (path, key, dst or ""))
    return index, set(index)


def truncated(caught: str, corpus: str) -> bool:
    """收集到的那份是不是語料的半截。

    <h2>為什麼一定要擋</h2>
    這個工具的前提是「遊戲送出的字串才是權威」。但舊版的收集端會把<b>打到一半</b>
    的句子當成完整句送出（見 `DialogueBuffer` 的說明），那種資料的權威性是反的：
    語料裡完整的那份才對。

    照著寫下去會把好好的原文改成半截，而且連帶把本來對得上的譯文弄壞——
    比原本的問題更糟。前綴關係就是那種資料的特徵，直接擋掉。
    """
    return len(caught) < len(corpus) and corpus.startswith(caught)


def captured_sources(path: Path) -> list[str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = data.get("entries", data)
    out = []
    for value in entries.values():
        if isinstance(value, dict) and isinstance(value.get("src"), str):
            out.append(value["src"])
    return out


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("captured", type=Path)
    ap.add_argument("--write", action="store_true",
                    help="把命中的 src 改成遊戲實際送出的字串")
    ap.add_argument("--threshold", type=float, default=THRESHOLD)
    args = ap.parse_args(argv)

    index, known = load_corpus()
    print(f"語料 {len(index)} 條原文")

    caught = [s for s in captured_sources(args.captured)
              if s not in known and len(s) >= MIN_LENGTH]
    print(f"收集到 {len(caught)} 條語料裡沒有的句子（已濾掉太短的）")
    print()

    edits: dict[Path, list[tuple[str, str, str]]] = {}
    hits = 0
    for src in caught:
        match = difflib.get_close_matches(src, known, n=1, cutoff=args.threshold)
        if not match:
            continue
        best = match[0]
        path, key, dst = index[best]
        ratio = difflib.SequenceMatcher(None, src, best).ratio()
        state = "已翻" if dst else "未翻"
        print(f"[{ratio:.3f}] {path.name} :: {key}  ({state})")
        print(f"  語料：{best}")
        print(f"  遊戲：{src}")
        if dst:
            print(f"  譯文：{dst}")
        print()
        hits += 1
        if truncated(src, best):
            print("  ↑ 收集到的是語料的前綴，判定為半截，不會寫入。")
            print("    這是舊版收集端的傷（見 DialogueBuffer）——語料那份才是對的。")
            print()
            continue
        if dst:
            edits.setdefault(path, []).append((key, best, src))

    print(f"接近但對不上的：{hits} 條，其中 {sum(len(v) for v in edits.values())} 條已經有譯文")

    if not args.write:
        if edits:
            print("\n（加 --write 可以把已翻的那些 src 改成遊戲版本）")
        return 0

    for path, items in edits.items():
        raw = path.read_bytes()
        data = json.loads(raw.decode("utf-8"))
        entries = data.get("entries", data)
        for key, _old, new in items:
            entry = entries.get(key)
            if isinstance(entry, dict):
                entry["src"] = new
        text = json.dumps(data, ensure_ascii=False, indent=1) + "\n"
        if b"\r\n" in raw:
            text = text.replace("\n", "\r\n")
        path.write_bytes(text.encode("utf-8"))
        print(f"改了 {path.name}：{len(items)} 條")
    print("\n記得跑 tools/quest-bundle.py 與 tools/validate.py")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
