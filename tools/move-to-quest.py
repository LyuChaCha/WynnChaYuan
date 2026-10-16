#!/usr/bin/env python3
"""把台詞搬到它該在的檔：扁平檔（quest.json、npc.json…）與任務檔（quest/、secret/）之間，哪個方向都行。

為什麼要有
----------
quest.json 是「還沒歸類」的收容所，任務檔裡也有追著任務時順路跟別人講話、被記成那個任務的句子。
放錯地方，翻譯的人看不到前後文；遊戲端也會在追那個任務時，拿錯的句子去比對。

為什麼不能只改繁中再跑 sync-languages
----------------------------------------
sync-languages 只從<b>同一個檔</b>帶回譯文。句子換了檔，其他語言找不到舊譯文，
會算進「丟掉 N 條」。這支七個語言一次搬，照原文帶各自的譯文。

用法
----
    python tools/move-to-quest.py <清單.json>            # 試跑，只印結果
    python tools/move-to-quest.py <清單.json> --write    # 寫入

清單是 [{src, from, to, after?, after_src?, kind?, speaker?}]：
- from／to：檔名，相對於語言資料夾（quest.json、quest/a-journey-home.json…）。
  quest/ 與 secret/ 底下是任務檔，其他都當扁平檔（{原文: 譯文}）。
- 搬進任務檔時，插在哪一條後面：after（鍵）或 after_src（原文）；都沒給就接在最後。
- kind：dialogue（預設）或 objective。speaker：不確定就留空，寫錯的說話者會被任務名單放大。
  從任務檔搬到任務檔，沒給的欄位沿用原本那一條的。

規矩
----
- 搬進任務檔的新條目 source 一律寫 captured。寫 wiki 的話，同一句的校訂版會輸給 wiki 版。
- 動到的任務檔整份重新編號成 #000..#NNN（跟 import-captured 一樣），七個語言用同一份對照表；
  七個語言的鍵不一致的檔不碰。搬空的任務檔刪掉。
- 搬去扁平檔時，扁平檔已經有同一句就不重複加（譯文不同會停下來，要人決定）。
- 搬完核對：每個語言搬出去的譯文數等於搬進來的譯文數，對不上就不寫那個語言。
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


def is_work(name: str) -> bool:
    return name.startswith(("quest/", "secret/"))


def read(path: Path) -> str:
    return io.open(path, encoding="utf-8", newline="").read()


def nl_of(raw: str) -> str:
    return "\r\n" if "\r\n" in raw else "\n"


def dump(data: dict, nl: str) -> str:
    return (json.dumps(data, ensure_ascii=False, indent=1) + "\n").replace("\n", nl)


def bump(text: str, n: int) -> str:
    head = text[:900]
    if '"_meta"' not in head or n == 0:
        return text
    for field in ("count", "translated"):
        m = re.search(r'("%s": )(\d+)' % field, head)
        if m:
            head = head[:m.start()] + m.group(1) + str(int(m.group(2)) + n) + head[m.end():]
    return head + text[900:]


def drop_flat(raw: str, srcs: set[str]) -> tuple[str, dict[str, str]]:
    """扁平檔逐行拿掉這幾條，回傳新的文字與拿掉的 {原文: 譯文}。不重 dump。"""
    nl = nl_of(raw)
    keep, taken = [], {}
    for line in raw.split(nl):
        m = FLAT_LINE.match(line)
        if m:
            src = json.loads(m.group(1))
            if src in srcs and src not in taken:
                taken[src] = json.loads(m.group(2))
                continue
        keep.append(line)
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


def append_flat(raw: str, add: list[tuple[str, str]]) -> str:
    """扁平檔檔尾接上新的幾條。不重 dump。"""
    if not add:
        return raw
    nl = nl_of(raw)
    body = raw.rstrip()
    assert body.endswith("}")
    body = body[:-1].rstrip()
    sep = "," if not body.endswith("{") else ""
    lines = sep + ",".join(nl + " " + json.dumps(s, ensure_ascii=False) + ": "
                           + json.dumps(d, ensure_ascii=False) for s, d in add)
    text = body + lines + nl + "}" + nl
    json.loads(text)
    return bump(text, len(add))


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    plan = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    write = "--write" in sys.argv

    # 動到的任務檔，七個語言的鍵要一致
    works = sorted({r["from"] for r in plan if is_work(r["from"])} | {r["to"] for r in plan if is_work(r["to"])})
    bad = set()
    for name in works:
        keys = []
        for lang in LANGS:
            path = TR / lang / name
            keys.append(list(json.loads(read(path))["entries"]) if path.exists() else None)
        if any(k != keys[0] for k in keys):
            print("略過（七個語言的鍵不一致或檔案不全）:", name)
            bad.add(name)
    rows = [r for r in plan if r["from"] not in bad and r["to"] not in bad]

    for lang in LANGS:
        flat_text: dict[str, str] = {}
        work_data: dict[str, dict] = {}
        work_raw: dict[str, str] = {}

        def flat(name):
            if name not in flat_text:
                flat_text[name] = read(TR / lang / name)
            return flat_text[name]

        def work(name):
            if name not in work_data:
                work_raw[name] = read(TR / lang / name)
                work_data[name] = json.loads(work_raw[name], object_pairs_hook=collections.OrderedDict)
            return work_data[name]

        # 1. 從來源拿出來
        out_tr = 0
        moving = []                                   # (row, dst, 原本的條目或 None)
        by_flat = collections.defaultdict(set)
        for r in rows:
            if not is_work(r["from"]):
                by_flat[r["from"]].add(r["src"])
        taken_flat = {}
        for name, srcs in by_flat.items():
            flat_text[name], taken_flat[name] = drop_flat(flat(name), srcs)
        for r in rows:
            if is_work(r["from"]):
                entries = work(r["from"])["entries"]
                key = next((k for k, e in entries.items() if e.get("src") == r["src"]), None)
                old = entries.pop(key) if key else None
                dst = (old or {}).get("dst", "") or ""
            else:
                old = None
                dst = taken_flat[r["from"]].get(r["src"], "")
            out_tr += 1 if dst.strip() else 0
            moving.append((r, dst, old))

        # 2. 放進目標
        in_tr = 0
        flat_add = collections.defaultdict(list)
        parsed = {}
        inserts = collections.defaultdict(list)       # 任務檔 -> [(錨點鍵或 None, 新條目)]
        stop = False
        for r, dst, old in moving:
            if not is_work(r["to"]):
                if r["to"] not in parsed:
                    parsed[r["to"]] = json.loads(flat(r["to"]))
                existing = parsed[r["to"]]
                if any(s == r["src"] for s, _ in flat_add[r["to"]]):
                    in_tr += 1 if dst.strip() else 0
                    continue
                if r["src"] in existing:
                    if existing[r["src"]] != dst and dst.strip() and existing[r["src"]].strip():
                        print(f"!! {lang}：{r['to']} 已經有「{r['src'][:40]}」而且譯文不同，停下來")
                        stop = True
                    in_tr += 1 if dst.strip() else 0   # 視同搬進去了（同一句已經在）
                    continue
                flat_add[r["to"]].append((r["src"], dst))
                in_tr += 1 if dst.strip() else 0
                continue
            target = work(r["to"])
            anchor = r.get("after")
            if not anchor and r.get("after_src"):
                anchor = next((k for k, e in target["entries"].items() if e.get("src") == r["after_src"]), None)
                if anchor is None:
                    print(f"!! {lang}：{r['to']} 找不到「{r['after_src'][:40]}」，接在最後")
            first = next(iter(target["entries"].values()), {})
            new = collections.OrderedDict([
                ("src", r["src"]), ("dst", dst), ("role", (old or {}).get("role", "desc")),
                ("kind", r.get("kind") or (old or {}).get("kind") or "dialogue"),
                ("quest", first.get("quest", (old or {}).get("quest", ""))),
                ("stage", (old or {}).get("stage", "") if old and old.get("quest") == first.get("quest") else ""),
                ("speaker", r.get("speaker") if r.get("speaker") is not None else (old or {}).get("speaker", "")),
                ("source", "captured")])
            inserts[r["to"]].append((anchor, new))
            in_tr += 1 if dst.strip() else 0
        if stop:
            continue

        for name, items in inserts.items():
            entries = work(name)["entries"]
            after = collections.defaultdict(list)
            tail = []
            for anchor, new in items:
                (after[anchor] if anchor in entries else tail).append(new)
            merged = []
            for k, e in entries.items():
                merged.append(e)
                merged.extend(after.get(k, []))
            merged.extend(tail)
            work(name)["entries"] = collections.OrderedDict((str(i), e) for i, e in enumerate(merged))

        # 3. 重新編號、重算 _meta、搬空的刪掉
        outputs, removed = {}, []
        for name, data in work_data.items():
            entries = list(data["entries"].values())
            if not entries:
                removed.append(name)
                continue
            raw = work_raw[name]
            original_keys = list(json.loads(raw)["entries"])
            prefix = original_keys[0].rsplit("#", 1)[0] if original_keys else (data.get("_meta", {}).get("quest") or name)
            data["entries"] = collections.OrderedDict((f"{prefix}#{i:03d}", e) for i, e in enumerate(entries))
            meta = data.get("_meta", {})
            if "count" in meta:
                meta["count"] = len(entries)
            if "translated" in meta:
                meta["translated"] = sum(1 for e in entries if str(e.get("dst") or "").strip())
            outputs[name] = dump(data, nl_of(raw))
        for name, add in flat_add.items():
            flat_text[name] = append_flat(flat(name), add)

        if out_tr != in_tr:
            print(f"!! {lang}：搬出去 {out_tr} 條譯文、搬進來 {in_tr} 條，對不上，不寫")
            continue
        print(f"{lang}：搬 {len(moving)} 條（帶譯文 {in_tr}），改 {len(outputs)} 個任務檔、{len(flat_text)} 個扁平檔"
              + (f"，刪掉 {len(removed)} 個空檔" if removed else ""))
        if write:
            for name, text in flat_text.items():
                io.open(TR / lang / name, "w", encoding="utf-8", newline="").write(text)
            for name, text in outputs.items():
                io.open(TR / lang / name, "w", encoding="utf-8", newline="").write(text)
            for name in removed:
                (TR / lang / name).unlink()
    print("（已寫入，記得跑 quest-bundle.py）" if write else "（試跑）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
