#!/usr/bin/env python3
"""比對兩個版本的翻譯輸出：同一份語料快照，base 與目前這棵樹各跑一次 goldenDump。

用法:
    python tools/golden-diff.py                          # 跟 origin/main 比
    python tools/golden-diff.py --base v1.0.5            # 跟某個 tag／commit 比
    python tools/golden-diff.py --samples a.log;b.txt    # 加上本機的聊天 log、line-debug.txt
    python tools/golden-diff.py --twice                  # 同一棵樹跑兩次，確認輸出是確定的

輸出完全相同時結束碼 0；有差異時印出前幾條、完整差異寫到 build/golden/diff.txt，結束碼 1。
"""
from __future__ import annotations

import argparse
import collections
import itertools
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / "build" / "golden"
TRANSLATIONS = Path("src/main/resources/assets/wynnchayuan/translations")
HARNESS = [
    Path("src/test/java/com/wynnchayuan/golden"),
    Path("src/test/java/com/wynnchayuan/render/DialogueGoldenFrames.java"),
]
TASK = re.compile(r"^tasks\.register\('goldenDump'.*?^}\n", re.S | re.M)


def git(*args: str, cwd: Path = ROOT) -> str:
    return subprocess.run(["git", *args], cwd=cwd, check=True,
                          capture_output=True, text=True).stdout.strip()


def gradle() -> str:
    found = os.environ.get("GRADLE") or shutil.which("gradle")
    if not found:
        sys.exit("找不到 gradle：放進 PATH，或用環境變數 GRADLE 指定")
    return found


def run_dump(tree: Path, out: Path, corpus: Path, samples: str) -> None:
    cmd = [gradle(), "--no-daemon", "--console=plain", "-q", "goldenDump",
           f"-Pgolden.out={out}", f"-Pgolden.corpus={corpus}"]
    if samples:
        cmd.append(f"-Pgolden.samples={samples}")
    print(f"→ {tree}")
    subprocess.run(cmd, cwd=tree, check=True)


def prepare_base(ref: str, base: Path) -> None:
    if base.exists():
        subprocess.run(["git", "worktree", "remove", "--force", str(base)], cwd=ROOT)
        shutil.rmtree(base, ignore_errors=True)
    git("worktree", "add", "--detach", str(base), ref)
    (base / "libs").mkdir(exist_ok=True)
    for jar in (ROOT / "libs").glob("*.jar"):
        shutil.copy2(jar, base / "libs" / jar.name)
    if not (base / HARNESS[0]).exists():
        for part in HARNESS:
            src, dst = ROOT / part, base / part
            if src.is_dir():
                shutil.copytree(src, dst, dirs_exist_ok=True)
            else:
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, dst)
        task = TASK.search((ROOT / "build.gradle").read_text(encoding="utf-8"))
        with open(base / "build.gradle", "a", encoding="utf-8") as f:
            f.write("\n" + task.group(0))


def compare(old: Path, new: Path) -> int:
    kinds = collections.Counter()
    shown: list[str] = []
    total = 0
    with open(old, encoding="utf-8") as a, open(new, encoding="utf-8") as b,             open(WORK / "diff.txt", "w", encoding="utf-8") as report:
        for left, right in itertools.zip_longest(a, b):
            total += 1
            if left == right:
                continue
            for sign, line in (("-", left), ("+", right)):
                if line is None:
                    continue
                fields = line.split("	")
                kinds[(sign, fields[0], fields[3] if len(fields) > 3 else "?")] += 1
                report.write(f"{total}{sign} {line}")
                if len(shown) < 20:
                    shown.append(f"{total}{sign} {line.rstrip()[:300]}")
    if not kinds:
        print(f"完全相同（{total:,} 行）")
        return 0
    print(f"有差異（共 {total:,} 行）")
    for (sign, lang, kind), n in sorted(kinds.items()):
        print(f"  {sign} {lang:6} {kind:10} {n:,}")
    for line in shown:
        print("  " + line)
    print(f"完整差異：{WORK / 'diff.txt'}")
    return 1


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="origin/main")
    ap.add_argument("--samples", default="", help="以路徑分隔字元（Windows 是 ;）隔開的檔案")
    ap.add_argument("--twice", action="store_true")
    ap.add_argument("--keep", action="store_true", help="留著 base 的 worktree")
    args = ap.parse_args()

    WORK.mkdir(parents=True, exist_ok=True)
    corpus = WORK / "corpus"
    shutil.rmtree(corpus, ignore_errors=True)
    shutil.copytree(ROOT / TRANSLATIONS, corpus)

    new = WORK / "new.txt"
    if args.twice:
        old = WORK / "again.txt"
        run_dump(ROOT, old, corpus, args.samples)
        run_dump(ROOT, new, corpus, args.samples)
        return compare(old, new)

    base = WORK / "base"
    old = WORK / "base.txt"
    prepare_base(args.base, base)
    try:
        run_dump(base, old, corpus, args.samples)
    finally:
        if not args.keep:
            subprocess.run(["git", "worktree", "remove", "--force", str(base)], cwd=ROOT)
    run_dump(ROOT, new, corpus, args.samples)
    return compare(old, new)


if __name__ == "__main__":
    sys.exit(main())
