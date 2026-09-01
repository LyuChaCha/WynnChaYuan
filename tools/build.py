#!/usr/bin/env python3
"""從 raw/ 的官方資料抽出「該翻譯的字串」，產生譯者可直接編輯的工作檔。

核心規則（決定一段文字要不要記錄）
---------------------------------
GLYPH   材質包圖示（PUA 字元，或 font 屬性存在）  -> 不記錄，原樣保留
SYMBOL  完全沒有字母，純標點／空白／分隔線        -> 不記錄
NUMERIC 純數字或數字帶單位（50、400%、+8）        -> 不記錄，交給 {re} 佔位符
TEXT    含有字母                                  -> 記錄
        ├─ sentence  長句（>=25 字元或含句末標點）-> priority 高，先替換
        └─ term      短詞                          -> priority 低

重跑時會保留 workspace/ 既有的譯文，只補進新字串、標記消失的字串。

用法:  python tools/build.py [--lang zh_tw]
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "raw"
WORKSPACE = ROOT / "workspace"

# ---------------------------------------------------------------- 字元判斷

def is_pua(ch: str) -> bool:
    """私用區字元 = 材質包圖示。"""
    o = ord(ch)
    return 0xE000 <= o <= 0xF8FF or 0xF0000 <= o <= 0x10FFFD or 0xE0000 <= o <= 0xE0FFF


def has_letter(text: str) -> bool:
    """是否含有任何字母（拉丁、中日韓皆算）。"""
    return any(ch.isalpha() and not is_pua(ch) for ch in text)


SENTENCE_MIN_LEN = 25
SENTENCE_END = re.compile(r"[.!?:][\s\"']*$")
# 必須至少含一個數字，否則 "-" "/" 這類分隔符會被誤判成數值
NUMERIC_ONLY = re.compile(r"^(?=[^\d]*\d)[\s\d.,+\-%/×x()\[\]]+$")

# 佔位符刻意不含字母，才不會讓「只有圖示的分隔行」被誤判為有文字內容
PH_GLYPH = "{#}"
PH_NUM = "{~}"
PH_PLACE = "{p}"

# ---------------------------------------------------------------- 參數化
#
# 這一段必須與 mod 端的 LineParts / GlyphSplitter 規則「完全一致」。
# 兩邊只要有一點差異，遊戲裡查表就會靜默落空——譯者翻了卻不會生效，
# 而且從畫面上完全看不出原因。
#
# mod 端對應：GlyphSplitter.NUMBERS / PlaceNames.PATTERN

# 前面緊接著字母的不算數值：A16-L31 是型號，抽成 A{~}-L{~} 之後
# 譯者看到的是兩個沒有意義的佔位符，根本不知道那句話在講什麼。
NUMBER_RE = re.compile(r"(?<![A-Za-z])\d+(?:[.,]\d+)*%?")


def _load_places() -> list[str]:
    """地名清單。

    先看 fetch.py 抓下來的原始檔，沒有就用<b>模組實際載入的那一份</b>——
    runtime 用的是後者，兩邊不一致的話語料裡沒有 {p}、遊戲裡卻有，
    含地名的條目就會全部查不到，而且從畫面上完全看不出原因。
    """
    src = RAW / "places.json"
    if not src.exists():
        src = ROOT / "src/main/resources/assets/wynnchayuan/places.json"
    if not src.exists():
        return []
    try:
        data = json.loads(src.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return []
    names = {p["name"].strip() for p in data.get("labels", [])
             if isinstance(p, dict) and p.get("name", "").strip()}
    names |= {p.strip() for p in data.get("places", []) if str(p).strip()}
    return sorted(names)


PLACES = _load_places()
# 長的優先，否則「Nemract Docks」會被切成「Nemract」+「Docks」。
# 前後加邊界避免 Ragni 命中 Ragnite；區分大小寫，才不會把普通名詞 swamp
# 誤判成地名 Swamp。
PLACE_RE = re.compile(
    r"(?<![A-Za-z0-9])(?:"
    + "|".join(re.escape(p) for p in sorted(PLACES, key=len, reverse=True))
    + r")(?![A-Za-z0-9])"
) if PLACES else None


def parametrize(text: str) -> str:
    """把地名與數字換成佔位符。地名先做，它可能含數字。"""
    if PLACE_RE is not None:
        text = PLACE_RE.sub(PH_PLACE, text)
    return NUMBER_RE.sub(PH_NUM, text)

# HTML lore: <span class='font-ascii' style='color:#AAAAAA'>text</span>
TAG_RE = re.compile(r"<[^>]+>")
BR_RE = re.compile(r"<br\s*/?>", re.I)


def classify(text: str, has_font_attr: bool = False) -> str:
    """回傳 GLYPH / SYMBOL / NUMERIC / sentence / term。"""
    if has_font_attr:
        return "GLYPH"
    stripped = text.strip()
    if not stripped:
        return "SYMBOL"
    if all(is_pua(c) or c.isspace() for c in stripped):
        return "GLYPH"
    if not has_letter(stripped):
        return "NUMERIC" if NUMERIC_ONLY.match(stripped) else "SYMBOL"
    if len(stripped) >= SENTENCE_MIN_LEN or SENTENCE_END.search(stripped):
        return "sentence"
    return "term"


# 這些領域的 role="name" 不是「物品名稱」。
#
# F6 的「翻譯物品名稱」是為了讓裝備名稱保持英文，對得上 wiki 與交易市場。
# 技能名稱、Major ID 名稱跟交易市場毫無關係，不該被那個開關一起關掉。
# 沒列在這裡的一律當物品——漏標一個裝備檔會讓開關無聲失效，
# 漏標一個技能檔頂多是名稱跟著關掉，後者看得出來。
NON_ITEM_DOMAINS = {"ability", "major-id"}


def key_of(text: str) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:12]


def strip_html(raw: str) -> list[str]:
    """把 gear/material 的 HTML lore 拆成一行行純文字。"""
    if not raw:
        return []
    raw = BR_RE.sub("\n", raw)
    raw = TAG_RE.sub("", raw)
    raw = (raw.replace("&quot;", '"').replace("&amp;", "&")
              .replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'"))
    return [ln.strip() for ln in raw.split("\n") if ln.strip()]


# ---------------------------------------------------------------- 收集器

class Collector:
    def __init__(self) -> None:
        self.entries: dict[str, dict] = {}
        self.stats: Counter = Counter()

    def add(self, text: str, ctx: str, *, has_font_attr: bool = False,
            role: str = "text", raw: str | None = None) -> None:
        kind = classify(text, has_font_attr)
        self.stats[kind] += 1
        if kind in ("GLYPH", "SYMBOL", "NUMERIC"):
            return
        original = text.strip()
        text = parametrize(original)
        if raw is None and text != original:
            raw = original          # 參數化前的原文，compile 時還原用
        k = key_of(text)
        if k in self.entries:
            ctxs = self.entries[k]["ctx"]
            if ctx not in ctxs and len(ctxs) < 4:
                ctxs.append(ctx)
            return
        entry = {"src": text, "dst": "", "role": role, "kind": kind, "ctx": [ctx]}
        if raw is not None and raw != text:
            # 底線開頭 = 譯者不必理會，compile.py 用它還原精確的 matcher
            entry["_raw"] = raw
        self.entries[k] = entry

    def add_block(self, lines: list[str], ctx: str,
                  raw_lines: list[str] | None = None) -> None:
        """把硬換行的多行敘述合併成一個翻譯單位。

        Wynncraft 的技能敘述是按顯示寬度硬斷行的，逐行翻譯會產出破碎的中文。
        WynnScribe 的 matcher 支援含 \\n 的多行比對，所以整段記錄才是正確做法。
        """
        if not lines:
            return
        original_block = "\n".join(lines)
        block = parametrize(original_block)
        if raw_lines is None and block != original_block:
            raw_lines = lines          # 參數化前的原文，compile 時還原用
        self.stats["block"] += 1
        k = key_of(block)
        if k in self.entries:
            ctxs = self.entries[k]["ctx"]
            if ctx not in ctxs and len(ctxs) < 4:
                ctxs.append(ctx)
            return
        entry = {
            "src": block,
            "dst": "",
            "role": "desc",
            "kind": "block",
            "ctx": [ctx],
            "lines": len(lines),
            "flat": parametrize(" ".join(lines)),  # 給譯者看的通順版本
        }
        if raw_lines:
            entry["_raw"] = "\n".join(raw_lines)
        self.entries[k] = entry


# ---------------------------------------------------------------- 各資料來源

def templatize_line(segs: list) -> str:
    """把一行的片段串成一個完整的模板字串。

    技能敘述常見形如 ["Your ", "Meteor", " will deal ", "200%", " damage"]，
    逐片段記錄會產生 'will deal' 這種無意義的碎片。整行模板化才是正確的翻譯單位：

        圖示片段（有 font 屬性） -> {#}   譯者原樣保留，不會誤刪符號
        純數值片段               -> {~}   由 {re} 佔位符在執行期填回
        其餘文字片段             -> 原樣併入

    回傳 (模板, 原文)。模板為空字串代表這行沒有可翻譯文字
    （純圖示或純數值的分隔行）。
    """
    parts: list[str] = []
    raw: list[str] = []
    for s in segs:
        if not isinstance(s, dict):
            continue
        text = s.get("text", "")
        if not text:
            continue
        raw.append(text)
        if "font" in s or all(is_pua(ch) for ch in text if not ch.isspace()):
            parts.append(PH_GLYPH)
            continue
        # 數值「不」在這裡替換 —— 交給 parametrize() 統一處理。
        # 以前這裡自己做一套（把 +45% 整個吞成 {~}），與 parametrize() 的
        # 「只吃數字、保留正負號」不一致，於是同一份工具產出兩種模板，
        # 而 mod 端只認得後者，導致那些條目翻了也不會生效。
        parts.append(text)

    line = "".join(parts).strip()
    # 佔位符不含字母，所以這裡等同於「這行有沒有真正的文字」
    return (line, "".join(raw).strip()) if has_letter(line) else ("", "")


def collect_abilities(data: dict, c: Collector) -> None:
    for cls, payload in data.items():
        for node in payload.get("nodes", []):
            name = node.get("name") or ""
            if name:
                c.add(name, f"{cls}/技能名稱", role="name")

            run: list[str] = []
            run_raw: list[str] = []

            def flush() -> None:
                nonlocal run, run_raw
                if run:
                    c.add_block(run, f"{cls}/{name}", run_raw)
                    run, run_raw = [], []

            for line in node.get("description") or []:
                segs = line if isinstance(line, list) else [line]
                tmpl, raw = templatize_line(segs)
                if not tmpl:
                    flush()          # 純圖示／空行 = 段落分隔
                    continue
                if tmpl.startswith(PH_GLYPH):
                    # 以圖示開頭 = 數值屬性行（「◆ Total Damage: {~}」）。
                    # 這類行在數百個技能間高度重複，單獨記錄才能重複利用譯文。
                    flush()
                    c.add(tmpl, f"{cls}/{name}", role="stat", raw=raw)
                    continue
                run.append(tmpl)
                run_raw.append(raw)
            flush()


def collect_named_items(data: dict, c: Collector, label: str) -> None:
    """gear / tomes / ingredients / materials / tools：key 就是顯示名稱。"""
    for name, info in data.items():
        if not isinstance(info, dict):
            continue
        sub = info.get("type") or info.get("subType") or label
        c.add(name, f"{label}/{sub}", role="name")
        lore = strip_html(info.get("lore") or "")
        if lore:
            c.add_block(lore, f"{label}/{name}/lore")


def collect_aspects(data: dict, c: Collector) -> None:
    for cls, entries in data.items():
        if not isinstance(entries, dict):
            continue
        for name, info in entries.items():
            c.add(name, f"aspect/{cls}", role="name")
            if isinstance(info, dict):
                lore = strip_html(info.get("lore") or "")
                if lore:
                    c.add_block(lore, f"aspect/{name}/lore")


def collect_flat_names(data, c: Collector, label: str) -> None:
    """services / sets：只取名稱。

    地名不走這裡——地名由 {p} 佔位符原樣保留，不需要翻譯；
    放進工作檔只會讓譯者做白工，而且參數化後會全部塌成同一條 "{p}"。
    """
    if isinstance(data, dict):
        names = data.keys()
    elif isinstance(data, list):
        names = [d.get("name") for d in data if isinstance(d, dict) and d.get("name")]
    else:
        return
    for n in names:
        if isinstance(n, str):
            c.add(n, label, role="name")


DOMAINS = {
    "ability":    ("abilities_v2", collect_abilities),
    "gear":       ("gear",         lambda d, c: collect_named_items(d, c, "gear")),
    "ingredient": ("ingredients",  lambda d, c: collect_named_items(d, c, "ingredient")),
    "material":   ("materials",    lambda d, c: collect_named_items(d, c, "material")),
    "tome":       ("tomes",        lambda d, c: collect_named_items(d, c, "tome")),
    "tool":       ("tools",        lambda d, c: collect_named_items(d, c, "tool")),
    "aspect":     ("aspects",      collect_aspects),
    "charm":      ("charms",       lambda d, c: collect_named_items(d, c, "charm")),
}


# ---------------------------------------------------------------- 合併寫出

def merge_and_write(domain: str, collector: Collector, lang: str) -> tuple[int, int, int]:
    out = WORKSPACE / f"{domain}.json"
    existing: dict[str, dict] = {}
    if out.exists():
        try:
            existing = json.loads(out.read_text(encoding="utf-8")).get("entries", {})
        except (json.JSONDecodeError, OSError) as exc:
            print(f"  ! 既有檔案讀取失敗，將重建: {exc}", file=sys.stderr)

    # 譯文照 <b>src</b> 認回來，不照鍵。
    #
    # 先前是照鍵（existing.get(k)）。鍵是內容的雜湊，看起來很穩，但那表示
    # <b>鍵的規則一旦改動，所有譯文就全部認不回來</b>——七千多條瞬間變成
    # 「沒翻過」，而且舊條目會以 stale 的身分留下來，變成一份重複。
    # 換句話說，鍵的長相被鎖死了，連「把亂碼換成看得懂的編號」都做不到。
    #
    # 照 src 認就沒有這個限制：src 才是這一條到底是哪一句話的依據，
    # 也正是查表時真正用的東西。
    by_src = {e.get("src"): e for e in existing.values() if e.get("dst")}

    merged: dict[str, dict] = {}
    kept = added = 0
    for k, entry in collector.entries.items():
        prev = by_src.pop(entry.get("src"), None)
        if prev and prev.get("dst"):
            entry["dst"] = prev["dst"]
            kept += 1
        else:
            added += 1
        merged[k] = entry

    # 原始資料已移除、但譯者翻過的字串：保留並標記，不要直接丟掉
    stale = 0
    for prev in by_src.values():
        prev["stale"] = True
        merged[key_of(prev.get("src", ""))] = prev
        stale += 1

    # 長句排前面，方便譯者依「長句優先」的順序作業；
    # 這個順序也正是 matcher 該有的 priority 順序（長字串先替換才不會被短詞切碎）
    order = {"block": 0, "sentence": 1, "term": 2}
    rows = [e for _, e in sorted(
        merged.items(),
        key=lambda kv: (order.get(kv[1].get("kind"), 3), -len(kv[1]["src"]), kv[1]["src"])
    )]
    # 鍵用「領域 + 第幾條」，不用雜湊。
    #
    # 雜湊對程式夠用，對翻譯的人是一串亂碼：打開檔案看不出這是第幾條、
    # 也看不出屬於哪一批。編號本來就是給人看的識別，查表走的是 src。
    #
    # 排序完才編號，所以號碼跟檔案裡的先後一致。反過來說，將來排序規則改了，
    # 號碼會整批重排——那沒關係，譯文是照 src 認回來的（見上面）。
    stem = domain.replace("/", "-")
    ordered = {f"{stem}#{i:04d}": e for i, e in enumerate(rows)}

    payload = {
        "_meta": {
            "domain": domain,
            "itemNames": domain not in NON_ITEM_DOMAINS,
            "lang": lang,
            "generated": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "count": len(ordered),
            "translated": sum(1 for e in ordered.values() if e.get("dst")),
            "note": "只需填 dst。src 內的材質包圖示與數字已在抽取階段排除。",
        },
        "entries": ordered,
    }
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=1), encoding="utf-8")
    return kept, added, stale


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--lang", default="zh_tw")
    args = ap.parse_args()

    WORKSPACE.mkdir(parents=True, exist_ok=True)
    grand = Counter()
    total_entries = 0

    print(f"{'domain':<12} {'記錄':>7} {'段落':>7} {'單句詞':>7} {'圖示':>7} {'數字':>7} {'符號':>7}")
    print("-" * 62)

    for domain, (rawname, fn) in DOMAINS.items():
        src = RAW / f"{rawname}.json"
        if not src.exists():
            continue
        try:
            data = json.loads(src.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError) as exc:
            print(f"{domain:<12} 讀取失敗: {exc}", file=sys.stderr)
            continue

        c = Collector()
        fn(data, c)
        kept, added, stale = merge_and_write(domain, c, args.lang)
        grand.update(c.stats)
        total_entries += len(c.entries)

        s = c.stats
        kinds = Counter(e["kind"] for e in c.entries.values())
        print(f"{domain:<12} {len(c.entries):>7,} {kinds['block']:>7,} "
              f"{kinds['sentence'] + kinds['term']:>7,} "
              f"{s['GLYPH']:>7,} {s['NUMERIC']:>7,} {s['SYMBOL']:>7,}")
        if kept or stale:
            print(f"{'':>12} 沿用譯文 {kept:,}／新增 {added:,}／已失效 {stale:,}")

    print("-" * 62)
    print(f"{'合計':<12} {total_entries:>7,} 條待翻譯")
    print(f"\n跳過的：圖示 {grand['GLYPH']:,}／數字 {grand['NUMERIC']:,}／符號 {grand['SYMBOL']:,}")
    print(f"輸出於 {WORKSPACE}")


if __name__ == "__main__":
    main()
