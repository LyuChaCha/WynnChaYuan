# -*- coding: utf-8 -*-
"""讓其他語言的原文跟上 zh_tw。

用法：

    python tools/sync-languages.py            # 只報告，不寫檔
    python tools/sync-languages.py --write
    python tools/sync-languages.py --write --lang zh_cn

為什麼需要
----------
新收進來的原文只會寫進 ``zh_tw/``（``import-captured.py`` 與收件匣都是），
其他語言的資料夾是開張那天從 zh_tw 複製的，之後再也沒動過。結果是：

* 其他語言的譯者看不到那一萬多條新句子，想翻也沒得翻；
* zh_tw 刪掉的壞原文，其他語言還留著，蓋住了修好的那一條（簡中「法术伤害」
  同名那次就是這樣）。

做法
----
**結構以 zh_tw 為準，譯文依原文文字帶回。**

* 檔案、鍵、順序、每一條的 role／kind／quest 那些欄位全部照 zh_tw。
* 各語言原本翻好的 dst，照 **src 文字**找回來填上。任務檔的鍵重新編號過，
  同一個鍵在兩邊可能是不同句子，所以不能照鍵對——任務檔用
  「任務名 + 原文 + 第幾次出現」對。
* zh_tw 已經沒有的原文，連同它的譯文一起不留，數量會印出來。那些多半是
  壞掉的舊原文（玩家名被剝成「, 」、座標空掉的「at .」）。
* ``quest-dialogue.json``／``secret-dialogue.json`` 是合併檔，不在這裡動，
  跑完請接著跑 ``tools/quest-bundle.py``。

新建出來的檔不帶 zh_tw 的說明文字：那是繁體，放進簡中會被 check-zh-cn 擋，
放進日文也沒人看得懂。
"""
from __future__ import annotations

import difflib
import json
import re
import sys
from collections import OrderedDict, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANG_ROOT = ROOT / "src/main/resources/assets/wynnchayuan/translations"
SOURCE = "zh_tw"
BUNDLES = {"quest-dialogue.json", "secret-dialogue.json"}

# _meta 裡照 zh_tw 走的結構欄位；其他（note、review…）是各語言自己的
STRUCTURAL_META = ("domain", "itemNames", "gearNames", "quest", "legend", "generated", "source")


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=OrderedDict)


def dump(path: Path, data) -> None:
    text = json.dumps(data, ensure_ascii=False, indent=1) + "\n"
    old = path.read_bytes() if path.exists() else b""
    if b"\r\n" in old:
        text = text.replace("\n", "\r\n")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(text.encode("utf-8"))


NEAR = 0.9


def marks(text: str):
    return sorted(re.findall(r"\{[#~pu]\}", re.sub(r"\{~\d+\}", "{~}", text)))


def near(src: str, candidates: list[tuple[str, str]]):
    """candidates 裡跟 src 最像、而且佔位符一樣的那一條的位置；沒有就 None。"""
    best, best_ratio = None, NEAR
    want = marks(src)
    for i, (old_src, _dst) in enumerate(candidates):
        m = difflib.SequenceMatcher(None, src, old_src)
        if m.real_quick_ratio() < best_ratio or m.quick_ratio() < best_ratio:
            continue
        ratio = m.ratio()
        if ratio >= best_ratio and marks(old_src) == want:
            best, best_ratio = i, ratio
    return best


def is_workspace(data) -> bool:
    return isinstance(data, dict) and isinstance(data.get("entries"), dict)


def sync_flat(tw, old):
    out = OrderedDict()
    kept = added = 0
    for key, value in tw.items():
        if key.startswith("_"):
            if old is not None and key in old:
                out[key] = old[key]
            elif key == "_note" or (key == "_meta" and isinstance(value, dict)):
                if key == "_meta":
                    out[key] = OrderedDict((k, v) for k, v in value.items() if k != "note")
            else:
                out[key] = value
            continue
        if not isinstance(value, str):
            out[key] = value
            continue
        prev = old.get(key) if old is not None else None
        if isinstance(prev, str) and prev.strip():
            out[key] = prev
            kept += 1
        else:
            out[key] = ""
            added += key not in (old or {})
    leftovers = [(k, v) for k, v in (old or {}).items()
                 if not k.startswith("_") and isinstance(v, str) and v.strip() and k not in tw]
    for key in out:
        if key.startswith("_") or not isinstance(out[key], str) or out[key] or not leftovers:
            continue
        hit = near(key, leftovers)
        if hit is not None:
            out[key] = leftovers.pop(hit)[1]
            kept += 1
    had = sum(1 for k, v in (old or {}).items()
              if not k.startswith("_") and isinstance(v, str) and v.strip())
    return out, kept, added, had - kept


def sync_workspace(tw, old, lang):
    old_entries = old.get("entries", {}) if old is not None else {}
    # 同一句原文在同一個任務裡可能出現好幾次，照出現順序一一對上
    pool = defaultdict(list)
    for entry in old_entries.values():
        if isinstance(entry, dict) and str(entry.get("dst") or "").strip():
            pool[(entry.get("quest"), entry.get("src"))].append(entry["dst"])
    used = defaultdict(int)

    entries = OrderedDict()
    kept = 0
    for key, entry in tw["entries"].items():
        new = OrderedDict((k, v) for k, v in entry.items())
        ident = (entry.get("quest"), entry.get("src"))
        candidates = pool.get(ident, [])
        if used[ident] < len(candidates):
            new["dst"] = candidates[used[ident]]
            used[ident] += 1
            kept += 1
        else:
            new["dst"] = ""
        entries[key] = new

    # zh_tw 事後修過的原文（錯字、補回 {u}、剝掉「***」）照文字對不上。
    # 相似度夠高、佔位符又完全一樣的，舊譯文照樣能用；佔位符不同的不沿用——
    # 原文多了一個 {u}，舊譯文就少一個，貼上去會是錯的。
    leftovers = defaultdict(list)
    for ident, dsts in pool.items():
        for dst in dsts[used[ident]:]:
            leftovers[ident[0]].append((ident[1] or "", dst))
    for entry in entries.values():
        if str(entry.get("dst") or "").strip() or not leftovers.get(entry.get("quest")):
            continue
        hit = near(entry.get("src") or "", leftovers[entry.get("quest")])
        if hit is not None:
            entry["dst"] = leftovers[entry.get("quest")].pop(hit)[1]
            kept += 1

    dropped = sum(len(v) for v in pool.values()) - kept

    meta = OrderedDict()
    tw_meta = tw.get("_meta") if isinstance(tw.get("_meta"), dict) else {}
    old_meta = old.get("_meta") if old is not None and isinstance(old.get("_meta"), dict) else {}
    for k, v in tw_meta.items():
        if k in STRUCTURAL_META:
            meta[k] = v
        elif k == "lang":
            meta[k] = lang
        elif k in old_meta:
            meta[k] = old_meta[k]
    for k, v in old_meta.items():
        if k not in meta and k not in ("count", "translated"):
            meta[k] = v
    if "count" in tw_meta:
        meta["count"] = len(entries)
    if "translated" in tw_meta:
        meta["translated"] = sum(1 for e in entries.values() if str(e.get("dst") or "").strip())

    out = OrderedDict()
    for k, v in tw.items():
        if k == "_meta":
            out[k] = meta
        elif k == "entries":
            out[k] = entries
        elif old is not None and k in old:
            out[k] = old[k]
        elif not k.startswith("_"):
            out[k] = v
    return out, kept, len(entries) - kept, dropped


def sync_language(lang: str, write: bool) -> dict:
    src_root = LANG_ROOT / SOURCE
    dst_root = LANG_ROOT / lang
    report = {"files_new": 0, "files_removed": 0, "kept": 0, "dropped": 0, "entries": 0}
    tw_files = {p.relative_to(src_root).as_posix() for p in src_root.rglob("*.json")}

    for rel in sorted(tw_files):
        name = rel.rsplit("/", 1)[-1]
        if name in BUNDLES:
            continue
        tw = load(src_root / rel)
        target = dst_root / rel
        if name.startswith("_"):
            # _index.json 之類：內容照 zh_tw，說明文字留各語言自己的
            if write:
                out = OrderedDict(tw)
                if target.exists():
                    prev = load(target)
                    for k in ("_note",):
                        if k in prev:
                            out[k] = prev[k]
                elif "_note" in out:
                    del out["_note"]
                dump(target, out)
            continue
        old = load(target) if target.exists() else None
        if old is None:
            report["files_new"] += 1
        if is_workspace(tw):
            out, kept, _added, dropped = sync_workspace(tw, old if is_workspace(old) else None, lang)
            report["entries"] += len(out["entries"])
        else:
            out, kept, _added, dropped = sync_flat(tw, old if isinstance(old, dict) and not is_workspace(old) else None)
            report["entries"] += sum(1 for k in out if not k.startswith("_"))
        report["kept"] += kept
        report["dropped"] += dropped
        if write:
            dump(target, out)

    for path in sorted(dst_root.rglob("*.json")):
        rel = path.relative_to(dst_root).as_posix()
        if rel not in tw_files:
            old = load(path)
            rows = old.get("entries", old) if isinstance(old, dict) else {}
            report["dropped"] += sum(1 for k, v in rows.items() if not k.startswith("_")
                                     and str((v.get("dst") if isinstance(v, dict) else v) or "").strip())
            report["files_removed"] += 1
            if write:
                path.unlink()
    return report


def main(argv: list[str]) -> int:
    write = "--write" in argv
    langs = [p.name for p in sorted(LANG_ROOT.iterdir()) if p.is_dir() and p.name != SOURCE]
    if "--lang" in argv:
        langs = [argv[argv.index("--lang") + 1]]
    for lang in langs:
        r = sync_language(lang, write)
        print(f"{lang}：{r['entries']:,} 條原文，帶回 {r['kept']:,} 條譯文，"
              f"丟掉 {r['dropped']:,} 條（原文已不在 zh_tw），"
              f"新增 {r['files_new']} 個檔、移除 {r['files_removed']} 個檔")
    if write:
        print("記得接著跑 tools/quest-bundle.py 與 tools/update-docs.py")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
