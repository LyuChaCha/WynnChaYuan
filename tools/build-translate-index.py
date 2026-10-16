#!/usr/bin/env python3
"""網站「協作翻譯」頁的搜尋索引：每個語言一個精簡的 JSON，只有檔名、鍵、原文、譯文。

為什麼要有
----------
語料一個語言二十幾 MB、兩百多個檔，網頁每次都整包下載太重，Worker 也不能一次讀那麼多檔。
這份索引只留翻譯的人要看的欄位，網頁讀它來搜尋與瀏覽；要改哪一句，Worker 再照檔名與鍵去改真的檔。

合併檔（quest-dialogue.json、secret-dialogue.json）不收：它們是 quest/、secret/ 產生的，改要改來源。

用法
----
    python tools/build-translate-index.py <輸出資料夾>

輸出 <語言>.json：{"files": [檔名…], "rows": [[檔號, 鍵或 null, 原文, 譯文, 說話者?], …]}
扁平檔的鍵就是原文，所以寫 null 省空間；任務檔的鍵是條目編號。
另外寫 meta.json：{"built": 時間, "commit": 這份語料的 commit}。
"""
from __future__ import annotations

import io
import json
import os
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TR = ROOT / "src/main/resources/assets/wynnchayuan/translations"
SKIP = {"quest-dialogue.json", "secret-dialogue.json"}


def build(lang: str) -> dict:
    files: list[str] = []
    rows: list[list] = []
    base = TR / lang
    for path in sorted(base.rglob("*.json")):
        rel = path.relative_to(base).as_posix()
        if rel in SKIP or rel.startswith("_") or "/_" in rel:
            continue
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        index = len(files)
        files.append(rel)
        if isinstance(data.get("entries"), dict):
            for key, entry in data["entries"].items():
                if not isinstance(entry, dict):
                    continue
                row = [index, key, entry.get("src", ""), entry.get("dst") or ""]
                if entry.get("speaker"):
                    row.append(entry["speaker"])
                rows.append(row)
        else:
            for key, value in data.items():
                if isinstance(value, str) and not key.startswith("_"):
                    rows.append([index, None, key, value])
    return {"files": files, "rows": rows}


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    langs = sorted(p.name for p in TR.iterdir() if p.is_dir() and not p.name.startswith("_"))
    for lang in langs:
        data = build(lang)
        text = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
        (out / f"{lang}.json").write_text(text, encoding="utf-8")
        print(f"{lang}: {len(data['files'])} 個檔、{len(data['rows'])} 條、{len(text.encode('utf-8')) // 1024} KB")
    try:
        commit = subprocess.run(["git", "rev-parse", "HEAD"], cwd=ROOT, capture_output=True,
                                encoding="utf-8").stdout.strip()
    except OSError:
        commit = ""
    (out / "meta.json").write_text(json.dumps({"built": int(time.time() * 1000), "commit": commit,
                                                "langs": langs}), encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
