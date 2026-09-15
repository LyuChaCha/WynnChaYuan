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
    python tools/near-miss.py 某人的-captured.json --write --report near-miss.json
    python tools/near-miss.py --render-body near-miss.json    # PR 說明的摘要（markdown）
    python tools/near-miss.py --selftest

`--write` 只動<b>已經有 dst</b> 的條目，而且只改 `src`。沒有譯文的條目不碰——
那種情況該走 import-captured.py，不是在這裡硬湊。扁平檔（`{原文: 譯文}`）
改的是鍵本身，譯文與鍵的順序原樣保留。

同一句原文在其他語言的同一個檔裡也跟著改（照 src 文字比對，不照鍵）：
不跟的話，那些語言的譯文會跟 zh_tw 修好之前一樣永遠對不上。

`--report` 把結果寫成 JSON，給 `import-captured.py --near-miss` 與 PR 說明用：

    {"fixed": [...], "blocked": [...], "candidates": [...]}

每一項是 `{src, corpus_src, file, key, ratio, ctx, dst, reason?}`：

* `fixed`：改對了的（沒加 `--write` 時是「會改」的）
* `blocked`：夠像但不能寫，`reason` 是 `prefix`（收到的是半截）、
  `untranslated`（語料那條也還沒翻）、`placeholders`（佔位符數量不同）、
  `claimed`（同一條已經被另一句改走）
* `candidates`：相似度 0.80–0.90，只列出來給人看，<b>永遠不寫</b>
"""

from __future__ import annotations

import argparse
import difflib
import json
import re
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANG_ROOT = ROOT / "src/main/resources/assets/wynnchayuan/translations"
SOURCE = "zh_tw"
TRANSLATIONS = LANG_ROOT / SOURCE
LANG_CODE = re.compile(r"[a-z]{2}_[a-z]{2}")

# 合併檔是 quest-bundle.py 從 quest/、secret/ 產生的。改在這裡的會被下一次
# 重建蓋掉，所以要改就改來源檔。
BUNDLES = {"quest-dialogue.json", "secret-dialogue.json"}

# 相似度門檻。
#
# 0.90 以下開始出現「兩句不同的台詞剛好結構像」的誤判——尤其是任務目標行
# （`Talk to X at [...]` 這種），它們彼此之間本來就只差一個名字。
# 寧可漏掉幾句讓人自己找，也不要把譯文接到錯的原文上。
THRESHOLD = 0.90

# 候選的下限。0.80–0.90 之間的不寫，只列出來：人看一眼就分得出是同一句
# 還是另一句，而既有的譯文對翻新句子的人是很好的參考。
CANDIDATE = 0.80

# 太短的句子不比。`...`、`Hm?`、`Yes.` 這種，任兩句的相似度都會很高，
# 而它們本來就是重複出現的通用短語，接錯了也看不出來。
MIN_LENGTH = 12


def corpus_files(base: Path = TRANSLATIONS) -> list[Path]:
    """要比對的來源檔。順序就是同一句原文重複時誰先被挑中。

    `secret/` 先前沒有列進來，於是祕密發現的台詞只在合併檔
    `secret-dialogue.json` 裡被找到——改在合併檔上的 src 下一次重建就消失。
    """
    files = sorted(base.glob("quest/*.json"))
    files += sorted(base.glob("secret/*.json"))
    files += sorted(base.glob("*.json"))
    files += sorted(base.glob("ability/*.json"))
    return [f for f in files
            if f.name not in BUNDLES and not f.name.startswith("_")]


def rows_of(data) -> tuple[dict, bool]:
    """(條目所在的那一層, 是不是扁平檔)"""
    if isinstance(data, dict) and isinstance(data.get("entries"), dict):
        return data["entries"], False
    return (data if isinstance(data, dict) else {}), True


def load_corpus(base: Path = TRANSLATIONS) -> dict[str, tuple[Path, str, str]]:
    """@return src -> (檔案, key, dst)"""
    index: dict[str, tuple[Path, str, str]] = {}
    for path in corpus_files(base):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:                                     # noqa: BLE001
            continue
        rows, flat = rows_of(data)
        for key, value in rows.items():
            if flat:
                if key.startswith("_") or not isinstance(value, str):
                    continue
                src, dst = key, value
            elif isinstance(value, dict):
                src, dst = value.get("src"), value.get("dst", "")
            else:
                continue
            if isinstance(src, str) and src.strip():
                index.setdefault(src, (path, key, dst if isinstance(dst, str) else ""))
    return index


def mismatched(src: str, dst: str) -> str:
    """新的原文裡有哪一種佔位符是譯文放不回去的。

    <p>`--write` 的前提是「譯文早就對了，只是 src 沒對上」。新的 src 一旦帶了
    譯文沒有的佔位符，那個前提就不成立——寫下去只會讓 validate 紅掉，
    而且畫面上會變成圖示消失或錯位。這種要人看過再處理，不是工具的事。
    """
    for tag in ("{#}", "{~}", "{p}", "{u}"):
        if src.count(tag) != dst.count(tag):
            return tag
    return ""


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


def captured_rows(path: Path) -> list[tuple[str, str]]:
    """(src, ctx)，同一句只留第一次出現的。"""
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = data.get("entries", data)
    out, seen = [], set()
    for value in entries.values():
        if isinstance(value, dict) and isinstance(value.get("src"), str):
            src = value["src"]
            if src not in seen:
                seen.add(src)
                ctx = value.get("ctx", "")
                out.append((src, ctx if isinstance(ctx, str) else ""))
    return out


def classify(rows: list[tuple[str, str]], index: dict[str, tuple[Path, str, str]],
             base: Path = TRANSLATIONS, threshold: float = THRESHOLD,
             floor: float = CANDIDATE) -> dict[str, list[dict]]:
    report: dict[str, list[dict]] = {"fixed": [], "blocked": [], "candidates": []}
    known = list(index)
    claimed: set[tuple[str, str]] = set()
    for src, ctx in rows:
        if src in index or len(src) < MIN_LENGTH:
            continue
        match = difflib.get_close_matches(src, known, n=1, cutoff=min(floor, threshold))
        if not match:
            continue
        best = match[0]
        path, key, dst = index[best]
        ratio = difflib.SequenceMatcher(None, src, best).ratio()
        item = {"src": src, "corpus_src": best,
                "file": path.relative_to(base).as_posix(), "key": key,
                "ratio": round(ratio, 3), "ctx": ctx, "dst": dst}
        if ratio < threshold:
            report["candidates"].append(item)
            continue
        if truncated(src, best):
            item["reason"] = "prefix"
        elif not dst.strip():
            item["reason"] = "untranslated"
        elif mismatched(src, dst):
            item["reason"] = "placeholders"
            item["tag"] = mismatched(src, dst)
        elif (item["file"], key) in claimed:
            item["reason"] = "claimed"
        else:
            claimed.add((item["file"], key))
            report["fixed"].append(item)
            continue
        report["blocked"].append(item)
    return report


def write_json(path: Path, raw: bytes, data) -> None:
    text = json.dumps(data, ensure_ascii=False, indent=1) + "\n"
    if b"\r\n" in raw:
        text = text.replace("\n", "\r\n")
    path.write_bytes(text.encode("utf-8"))


def rename_in(data, old: str, new: str, key: str | None = None,
              check_dst: bool = False):
    """把一份檔案裡原文是 `old` 的條目改成 `new`。

    @param key       只改這個鍵（zh_tw 那份知道是哪一條）；None 表示照 src 文字找
    @param check_dst 那一條已經有譯文、而佔位符放不回去時不改（其他語言用）
    @return (新的資料, 改了幾條)
    """
    rows, flat = rows_of(data)
    if flat:
        if old not in rows or new in rows or not isinstance(rows[old], str):
            return data, 0
        if check_dst and rows[old].strip() and mismatched(new, rows[old]):
            return data, 0
        # 重建一份才能讓鍵留在原本的位置——扁平檔的順序是人排過的
        return {(new if k == old else k): v for k, v in rows.items()}, 1
    count = 0
    for k, entry in rows.items():
        if not isinstance(entry, dict) or entry.get("src") != old:
            continue
        if key is not None and k != key:
            continue
        dst = entry.get("dst") or ""
        if check_dst and isinstance(dst, str) and dst.strip() and mismatched(new, dst):
            continue
        entry["src"] = new
        count += 1
    return data, count


def other_languages(base: Path) -> list[Path]:
    root = base.parent
    return sorted(d for d in root.iterdir()
                  if d.is_dir() and d != base and LANG_CODE.fullmatch(d.name))


def apply(report: dict[str, list[dict]], base: Path = TRANSLATIONS) -> dict:
    """真的寫進去。改不成的（檔案在這之間變了）從 fixed 移到 blocked。

    @return {"files": {rel: 條數}, "mirrored": {語言: 條數}}
    """
    by_file: dict[str, list[dict]] = {}
    for item in report["fixed"]:
        by_file.setdefault(item["file"], []).append(item)

    done: list[dict] = []
    files: dict[str, int] = {}
    for rel, items in by_file.items():
        path = base / rel
        raw = path.read_bytes()
        data = json.loads(raw.decode("utf-8"))
        changed = 0
        for item in items:
            data, n = rename_in(data, item["corpus_src"], item["src"], key=item["key"])
            if n:
                changed += 1
                done.append(item)
            else:
                item["reason"] = "unchanged"
                report["blocked"].append(item)
        if changed:
            write_json(path, raw, data)
            files[rel] = changed
    report["fixed"] = done

    mirrored: dict[str, int] = {}
    for lang in other_languages(base):
        total = 0
        for rel in sorted({item["file"] for item in done}):
            path = lang / rel
            if not path.is_file():
                continue
            raw = path.read_bytes()
            data = json.loads(raw.decode("utf-8"))
            changed = 0
            for item in done:
                if item["file"] != rel:
                    continue
                data, n = rename_in(data, item["corpus_src"], item["src"], check_dst=True)
                changed += n
            if changed:
                write_json(path, raw, data)
                total += changed
        if total:
            mirrored[lang.name] = total
    return {"files": files, "mirrored": mirrored}


# --------------------------------------------------------------------------
# PR 說明

REASON_LABEL = {
    "prefix": "收到的是半截",
    "untranslated": "既有條目也還沒翻",
    "placeholders": "佔位符數量不同",
    "claimed": "同一條已被另一句改走",
    "unchanged": "寫入時已找不到",
    "candidate": "相似度未達 0.90",
}

TABLE_ROWS = 40
CELL = 90


def cell(text: str) -> str:
    """表格裡的一格：截短、換行攤平、`|` 跳脫，包成 code 免得被當成格式。"""
    text = (text or "").replace("\r", "").replace("\n", " ⏎ ")
    if len(text) > CELL:
        text = text[:CELL - 1] + "…"
    if not text:
        return "（空）"
    text = text.replace("|", "\\|")
    fence = "``" if "`" in text else "`"
    return f"{fence} {text} {fence}" if fence == "``" else f"`{text}`"


def render_body(report: dict) -> str:
    fixed = report.get("fixed", [])
    blocked = report.get("blocked", [])
    candidates = report.get("candidates", [])
    mirrored = report.get("mirrored", {})
    listed = [dict(i, reason=i.get("reason", "candidate")) for i in blocked + candidates
              if i.get("imported")]
    by_reason: dict[str, int] = {}
    for item in blocked:
        by_reason[item.get("reason", "?")] = by_reason.get(item.get("reason", "?"), 0) + 1

    out = ["### 原文差一點的句子（near-miss）", ""]
    out.append(f"- 改對原文：{len(fixed)} 條"
               + (f"（其他語言跟著改 {sum(mirrored.values())} 條）" if mirrored else ""))
    out.append(f"- 夠像但沒改：{len(blocked)} 條"
               + ("（" + "、".join(f"{REASON_LABEL.get(r, r)} {n}"
                                  for r, n in sorted(by_reason.items())) + "）"
                  if by_reason else ""))
    out.append(f"- 相似度 0.80–0.90 的候選：{len(candidates)} 條（不會自動寫入）")
    fields = sum(1 for i in listed if i.get("hint") == "field")
    out.append(f"- 上面兩類裡收成新條目的：{len(listed)} 條"
               f"（{fields} 條在任務檔的 `similar` 欄位留了提示，"
               f"{len(listed) - fields} 條在扁平檔、提示只列在這裡）")
    unlisted = len(blocked) + len(candidates) - len(listed)
    if unlisted:
        out.append(f"- 另外 {unlisted} 條沒有收進語料（import-captured 擋掉或已存在），"
                   "可能夾帶玩家名，不列出")

    if fixed:
        out += ["", "#### 改對的原文", "",
                "| 舊原文 | 新原文 | 檔案 |", "|---|---|---|"]
        for item in fixed[:TABLE_ROWS]:
            out.append(f"| {cell(item['corpus_src'])} | {cell(item['src'])} "
                       f"| {cell(item['file'])} |")
        if len(fixed) > TABLE_ROWS:
            out.append(f"\n…另外 {len(fixed) - TABLE_ROWS} 條")

    if listed:
        out += ["", "#### 收成新條目、但語料裡有很像的一句", "",
                "新條目的 dst 是空的；右邊是那句很像的既有原文與它的譯文，翻的時候可以參考。",
                "",
                "| 新收的原文 | 相近的既有原文 | 既有譯文 | 相似度 | 原因 | 收進 |",
                "|---|---|---|---|---|---|"]
        for item in listed[:TABLE_ROWS]:
            out.append(f"| {cell(item['src'])} | {cell(item['corpus_src'])} "
                       f"| {cell(item.get('dst') or '')} | {item['ratio']:.2f} "
                       f"| {REASON_LABEL.get(item['reason'], item['reason'])} "
                       f"| {cell(item['imported'])} |")
        if len(listed) > TABLE_ROWS:
            out.append(f"\n…另外 {len(listed) - TABLE_ROWS} 條")
    return "\n".join(out) + "\n"


# --------------------------------------------------------------------------
# 自我檢查

def selftest() -> int:
    """`python tools/near-miss.py --selftest`

    改 src 是「錯了也不會報錯」的那種操作：扁平檔先前根本沒改到、卻照樣
    印出「改了 N 條」。所以把每一種分流都放一個能重跑的案例。
    """
    bad = 0

    def check(what: str, ok: bool, got=None) -> None:
        nonlocal bad
        print(("  [PASS] " if ok else "  [FAIL] ") + what
              + ("" if ok else f"（實際 {got!r}）"))
        bad += 0 if ok else 1

    flat_old = "Welcome to the town of Ragni, traveller."
    flat_new = "Welcome to the town of Ragni, traveler."
    quest_old = "You must reach the from this house quickly."
    quest_new = "You must reach the rooftops from this house quickly."
    secret_old = "The whispers grow loudr beneath the old well."
    secret_new = "The whispers grow louder beneath the old well."
    prefix_old = "The ancient guardian awakens beneath the old stone bridge."
    prefix_new = "The ancient guardian awakens beneath the old stone"
    tag_old = "Talk to the guard at the gate of Detlas."
    tag_new = "Talk to the {#}guard{#} at the gate of Detlas."
    blank_old = "Nobody has translated this lonely sentence yet."
    blank_new = "Nobody has translated this lonely sentence yet!"
    band_old = "Gather twenty wolf pelts and return to the hunter."
    band_new = "Gather ten wolf pelts and bring them to the hunter."
    other = "Completely unrelated line about emerald trading."

    with tempfile.TemporaryDirectory() as tmp:
        base = Path(tmp) / "zh_tw"
        ja = Path(tmp) / "ja_jp"
        for d in (base / "quest", base / "secret", ja / "quest"):
            d.mkdir(parents=True)
        dump = lambda p, d: p.write_text(json.dumps(d, ensure_ascii=False, indent=1) + "\n",
                                         encoding="utf-8")
        dump(base / "misc.json", {"_note": "說明", "Before": "前", flat_old: "歡迎來到拉格尼。",
                                  tag_old: "跟底特拉斯城門的守衛說話。",
                                  blank_old: "", "After": "後"})
        dump(base / "quest-dialogue.json", {"_meta": {"generated": True}, "entries": {
            "x": {"src": secret_old, "dst": "合併檔不該被挑中"}}})
        dump(base / "quest" / "cook.json", {"_meta": {"quest": "Cook"}, "entries": {
            "Cook#000": {"src": quest_old, "dst": "你得從這間房子上去。", "quest": "Cook"},
            "Cook#001": {"src": prefix_old, "dst": "古老的守衛醒來。", "quest": "Cook"},
            "Cook#002": {"src": band_old, "dst": "收集二十張狼皮。", "quest": "Cook"}}})
        dump(base / "secret" / "well.json", {"_meta": {"quest": "Well"}, "entries": {
            "Well#000": {"src": secret_old, "dst": "低語聲越來越大。", "quest": "Well"}}})
        dump(ja / "misc.json", {"Before": "前", flat_old: "ラーニへようこそ。", "After": "後"})
        dump(ja / "quest" / "cook.json", {"_meta": {"quest": "Cook"}, "entries": {
            "Cook#000": {"src": quest_old, "dst": "屋根まで行け。", "quest": "Cook"}}})

        index = load_corpus(base)
        check("合併檔與 _ 開頭的檔不進語料",
              index.get(secret_old, (Path(),))[0].name == "well.json",
              index.get(secret_old))
        rows = [(flat_new, "gui"), (quest_new, "dialogue/Cook"), (secret_new, "dialogue/Well"),
                (prefix_new, "dialogue/Cook"), (tag_new, "chat"), (blank_new, "chat"),
                (band_new, "dialogue/Cook"), (other, "chat"), (flat_old, "gui"),
                ("Short one.", "chat")]
        report = classify(rows, index, base)
        fixed = {i["src"]: i for i in report["fixed"]}
        blocked = {i["src"]: i.get("reason") for i in report["blocked"]}
        cands = {i["src"]: i for i in report["candidates"]}

        check("扁平檔的近似句列為 fixed", flat_new in fixed, sorted(fixed))
        check("任務檔的近似句列為 fixed", quest_new in fixed, sorted(fixed))
        check("secret/ 也比對得到", secret_new in fixed
              and fixed[secret_new]["file"] == "secret/well.json", fixed.get(secret_new))
        check("前綴擋下（prefix）", blocked.get(prefix_new) == "prefix", blocked)
        check("佔位符不同擋下（placeholders）", blocked.get(tag_new) == "placeholders", blocked)
        check("既有條目沒翻擋下（untranslated）", blocked.get(blank_new) == "untranslated", blocked)
        band_ratio = difflib.SequenceMatcher(None, band_new, band_old).ratio()
        check(f"0.80–0.90 列為候選（{band_ratio:.3f}）",
              band_new in cands and CANDIDATE <= band_ratio < THRESHOLD
              and band_new not in fixed and band_new not in blocked, report["candidates"])
        check("差很遠、完全命中、太短的都不列",
              all(s not in fixed and s not in blocked and s not in cands
                  for s in (other, flat_old, "Short one.")), report)

        dup = classify([(flat_new, ""), (flat_old.replace("Ragni", "Ragni "), "")], index, base)
        check("同一條被兩句搶時，第二句擋下（claimed）",
              len(dup["fixed"]) == 1 and [i.get("reason") for i in dup["blocked"]] == ["claimed"],
              dup)

        result = apply(report, base)
        misc = json.loads((base / "misc.json").read_text(encoding="utf-8"))
        check("扁平檔真的改了鍵、譯文與順序不變",
              list(misc) == ["_note", "Before", flat_new, tag_old, blank_old, "After"]
              and misc[flat_new] == "歡迎來到拉格尼。", list(misc))
        cook = json.loads((base / "quest" / "cook.json").read_text(encoding="utf-8"))
        check("任務檔改了 src、dst 不變",
              cook["entries"]["Cook#000"] == {"src": quest_new, "dst": "你得從這間房子上去。",
                                              "quest": "Cook"}, cook["entries"]["Cook#000"])
        check("被擋與候選的條目原封不動",
              cook["entries"]["Cook#001"]["src"] == prefix_old
              and cook["entries"]["Cook#002"]["src"] == band_old
              and tag_old in misc and blank_old in misc, cook["entries"])
        well = json.loads((base / "secret" / "well.json").read_text(encoding="utf-8"))
        check("secret/ 的來源檔改到了", well["entries"]["Well#000"]["src"] == secret_new, well)
        check("計數照實際改動",
              result["files"] == {"misc.json": 1, "quest/cook.json": 1, "secret/well.json": 1},
              result["files"])
        ja_misc = json.loads((ja / "misc.json").read_text(encoding="utf-8"))
        ja_cook = json.loads((ja / "quest" / "cook.json").read_text(encoding="utf-8"))
        check("其他語言照 src 文字跟著改、保留自己的譯文",
              list(ja_misc) == ["Before", flat_new, "After"]
              and ja_misc[flat_new] == "ラーニへようこそ。"
              and ja_cook["entries"]["Cook#000"]["src"] == quest_new
              and result["mirrored"] == {"ja_jp": 2}, (ja_misc, result["mirrored"]))

        # 舊寫法那一句不能再送：改完之後它對語料來說就是「差一點的新句子」，會被改回去
        again = apply(classify([r for r in rows if r[0] != flat_old],
                               load_corpus(base), base), base)
        check("重跑一次不會再改", again["files"] == {} and again["mirrored"] == {}, again)

        # 被 import-captured 收進去的才列表格；有 | 與換行的格子不能弄壞表格
        report["mirrored"] = result["mirrored"]
        for item in report["blocked"] + report["candidates"]:
            if item["src"] in (tag_new, band_new):
                item["imported"], item["hint"] = "misc.json", "body"
        report["candidates"].append({"src": "A | B\nC", "corpus_src": "A | B", "file": "misc.json",
                                     "key": "A | B", "ratio": 0.85, "ctx": "", "dst": "甲",
                                     "imported": "misc.json", "hint": "body"})
        body = render_body(report)
        check("PR 摘要列出改對的原文",
              f"`{flat_old}`" in body and f"`{flat_new}`" in body, body)
        check("PR 摘要只列收成新條目的被擋句，並帶既有譯文",
              "跟底特拉斯城門的守衛說話。" in body and prefix_new not in body, body)
        widths = {len(re.findall(r"(?<!\\)\|", line))
                  for line in body.splitlines() if line.startswith("|")}
        check("表格格子裡的 | 與換行被處理掉",
              "`A \\| B ⏎ C`" in body and widths == {4, 7}, (widths, body))

    print("near-miss：" + ("全部通過" if bad == 0 else f"{bad} 項失敗"))
    return 1 if bad else 0


# --------------------------------------------------------------------------

def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("captured", type=Path, nargs="?")
    ap.add_argument("--write", action="store_true",
                    help="把命中的 src 改成遊戲實際送出的字串")
    ap.add_argument("--threshold", type=float, default=THRESHOLD)
    ap.add_argument("--report", type=Path,
                    help="把 fixed／blocked／candidates 寫成 JSON")
    ap.add_argument("--render-body", type=Path, metavar="REPORT",
                    help="讀 --report 的輸出，印出 PR 說明用的 markdown")
    ap.add_argument("--selftest", action="store_true")
    args = ap.parse_args(argv)

    if args.selftest:
        return selftest()
    if args.render_body:
        if not args.render_body.is_file():
            print("（這一輪沒有 near-miss 報告）")
            return 0
        report = json.loads(args.render_body.read_text(encoding="utf-8"))
        sys.stdout.write(render_body(report))
        return 0
    if args.captured is None:
        ap.error("要給 captured.json（或 --render-body／--selftest）")

    index = load_corpus()
    print(f"語料 {len(index)} 條原文")
    rows = [(s, c) for s, c in captured_rows(args.captured)
            if s not in index and len(s) >= MIN_LENGTH]
    print(f"收集到 {len(rows)} 條語料裡沒有的句子（已濾掉太短的）")
    print()

    report = classify(rows, index, threshold=args.threshold)
    for item in report["fixed"] + report["blocked"]:
        state = "已翻" if item["dst"] else "未翻"
        print(f"[{item['ratio']:.3f}] {item['file']} :: {item['key']}  ({state})")
        print(f"  語料：{item['corpus_src']}")
        print(f"  遊戲：{item['src']}")
        if item["dst"]:
            print(f"  譯文：{item['dst']}")
        reason = item.get("reason")
        if reason == "prefix":
            print("  ↑ 收集到的是語料的前綴，判定為半截，不會寫入。")
            print("    這是舊版收集端的傷（見 DialogueBuffer）——語料那份才是對的。")
        elif reason == "placeholders":
            print(f"  ↑ 新的原文有 {item['tag']} 個譯文放不回去，不會寫入。")
            print("    先把圖示補進譯文（例如把名稱包成 {#}名稱{#}）再跑一次。")
        elif reason == "claimed":
            print("  ↑ 同一條已經被另一句改走，這句不寫入。")
        print()

    hits = len(report["fixed"]) + len(report["blocked"])
    print(f"接近但對不上的：{hits} 條，其中 {len(report['fixed'])} 條可以直接改 src；"
          f"另有 {len(report['candidates'])} 條相似度 {CANDIDATE:.2f}–{args.threshold:.2f} 的候選")

    if args.write:
        result = apply(report)
        for rel, n in result["files"].items():
            print(f"改了 {rel}：{n} 條")
        for lang, n in result["mirrored"].items():
            print(f"  {lang} 跟著改：{n} 條")
        report["mirrored"] = result["mirrored"]
        print(f"\n實際改了 {sum(result['files'].values())} 條")
        if result["files"]:
            print("記得跑 tools/quest-bundle.py 與 tools/validate.py")
    elif report["fixed"]:
        print("\n（加 --write 可以把已翻的那些 src 改成遊戲版本）")

    if args.report:
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=1) + "\n",
                               encoding="utf-8")
        print(f"報告寫到 {args.report}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
