#!/usr/bin/env python3
"""把扁平檔（quest.json、npc.json）裡其實屬於某個任務的台詞，搬進那個任務的工作檔。

為什麼要有
----------
quest.json 是「還沒歸類」的收容所。台詞留在那裡，翻譯的人看不到它前後是誰在講什麼；
遊戲端也只能全庫查表，打字打到一半時認不出是哪一句（任務限定的前綴比對只看任務檔）。

為什麼不能只改繁中再跑 sync-languages
----------------------------------------
sync-languages 只從<b>同一個檔</b>帶回譯文。句子換了檔，其他語言找不到舊譯文，
會算進「丟掉 N 條」。這支七個語言一次搬，照原文帶各自的譯文。

用法
----
    python tools/move-to-quest.py <清單.json>            # 試跑，只印結果
    python tools/move-to-quest.py <清單.json> --write    # 寫入

清單是 [{src, from, to, after, kind?, speaker?}]：
- from：扁平檔（例如 quest.json）
- to：任務檔（例如 quest/a-journey-home.json）
- after：插在哪一條後面（任務檔裡的鍵，例如 "A Journey Home#014"）
- kind：dialogue（預設）或 objective；speaker 不確定就留空，寫錯的說話者會被任務名單放大

規矩
----
- source 一律寫 captured。寫 wiki 的話，同一句的校訂版會輸給 wiki 版。
- 任務檔整份重新編號成 #000..#NNN（跟 import-captured 一樣），七個語言用同一份對照表；
  七個語言的鍵不一致的檔不碰。
- 搬完核對：每個語言「扁平檔少掉的譯文」等於「任務檔多出的譯文」。
- 之後要跑 tools/quest-bundle.py。
"""
from __future__ import annotations

import collections
import io
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TR = ROOT / "src/main/resources/assets/wynnchayuan/translations"
LANGS = ["zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"]
FLAT_LINE = re.compile(r'^\s*("(?:[^"\\]|\\.)*")\s*:\s*("(?:[^"\\]|\\.)*")\s*,?\s*$')


def read(path: Path) -> str:
    return io.open(path, encoding="utf-8", newline="").read()


def nl_of(raw: str) -> str:
    return "\r\n" if "\r\n" in raw else "\n"


def dump(data: dict, nl: str) -> str:
    return (json.dumps(data, ensure_ascii=False, indent=1) + "\n").replace("\n", nl)


def drop_flat(raw: str, srcs: set[str]) -> tuple[str, dict[str, str]]:
    """扁平檔逐行拿掉這幾條，回傳新的文字與拿掉的 {原文: 譯文}。不重 dump。"""
    nl = nl_of(raw)
    lines = raw.split(nl)
    taken = {}
    keep = []
    for line in lines:
        m = FLAT_LINE.match(line)
        if m:
            src = json.loads(m.group(1))
            if src in srcs and src not in taken:
                taken[src] = json.loads(m.group(2))
                continue
        keep.append(line)
    # 拿掉的是最後一條的話，前一條的逗號要去掉
    for i in range(len(keep) - 1, -1, -1):
        if keep[i].strip() == "}":
            j = i - 1
            while j >= 0 and not keep[j].strip():
                j -= 1
            if j >= 0 and keep[j].rstrip().endswith(","):
                keep[j] = keep[j].rstrip()[:-1]
            break
    text = nl.join(keep)
    json.loads(text)
    return text, taken


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    plan = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    write = "--write" in sys.argv

    by_file: dict[str, list[dict]] = collections.defaultdict(list)
    for row in plan:
        by_file[row["to"]].append(row)

    # 七個語言的鍵一致才動
    usable = []
    for to in by_file:
        keys = [list(json.loads(read(TR / lang / to))["entries"]) for lang in LANGS]
        if any(k != keys[0] for k in keys):
            print("略過（七個語言的鍵不一致）:", to)
            continue
        anchors = {row["after"] for row in by_file[to]}
        missing = anchors - set(keys[0])
        if missing:
            print("略過（找不到插入點）:", to, sorted(missing))
            continue
        usable.append(to)

    report = {}
    for lang in LANGS:
        flats: dict[str, str] = {}
        flat_taken: dict[str, dict[str, str]] = {}
        wanted_by_flat = collections.defaultdict(set)
        for to in usable:
            for row in by_file[to]:
                wanted_by_flat[row["from"]].add(row["src"])
        for flat, srcs in wanted_by_flat.items():
            text, taken = drop_flat(read(TR / lang / flat), srcs)
            flats[flat] = text
            flat_taken[flat] = taken
        moved = translated = blank = 0
        outputs = {}
        for to in usable:
            raw = read(TR / lang / to)
            data = json.loads(raw)
            after = collections.defaultdict(list)
            for row in by_file[to]:
                after[row["after"]].append(row)
            entries = []
            for key, entry in data["entries"].items():
                entries.append(entry)
                for row in after.get(key, []):
                    dst = flat_taken.get(row["from"], {}).get(row["src"], "")
                    new = collections.OrderedDict([
                        ("src", row["src"]), ("dst", dst), ("role", "desc"),
                        ("kind", row.get("kind") or "dialogue"), ("quest", entry.get("quest", "")),
                        ("stage", entry.get("stage", "")), ("speaker", row.get("speaker") or ""),
                        ("source", "captured")])
                    entries.append(new)
                    moved += 1
                    translated += 1 if dst.strip() else 0
                    blank += 0 if dst.strip() else 1
            prefix = next(iter(data["entries"])).rsplit("#", 1)[0]
            data["entries"] = collections.OrderedDict(
                (f"{prefix}#{i:03d}", e) for i, e in enumerate(entries))
            meta = data.get("_meta", {})
            if "count" in meta:
                meta["count"] = len(entries)
            if "translated" in meta:
                meta["translated"] = sum(1 for e in entries if str(e.get("dst") or "").strip())
            outputs[to] = dump(data, nl_of(raw))
        lost = sum(len(t) for t in flat_taken.values())
        lost_tr = sum(1 for t in flat_taken.values() for v in t.values() if v.strip())
        report[lang] = (moved, translated, blank, lost, lost_tr)
        if lost_tr != translated:
            print(f"!! {lang}：扁平檔少了 {lost_tr} 條譯文，任務檔只多 {translated} 條，不寫")
            continue
        if write:
            for flat, text in flats.items():
                io.open(TR / lang / flat, "w", encoding="utf-8", newline="").write(text)
            for to, text in outputs.items():
                io.open(TR / lang / to, "w", encoding="utf-8", newline="").write(text)
    for lang, (moved, translated, blank, lost, lost_tr) in report.items():
        print(f"{lang}：搬 {moved} 條（帶譯文 {translated}、空白 {blank}），扁平檔拿掉 {lost} 條")
    print(f"{len(usable)} 個任務檔", "（已寫入，記得跑 quest-bundle.py）" if write else "（試跑）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
