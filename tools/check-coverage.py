# -*- coding: utf-8 -*-
"""對話的譯文裡不可以有「對話字型畫不出來」的字。

## 為什麼需要這一條

對話的就地取代有一道守門：那一句的譯文**每一個字**都要在該語言的對話字型裡，
差一個字就整句退回英文（見 `render/DialogueRewriter`）。而且**退回時沒有任何
訊號**——畫面上看起來只是「這句沒翻」，去查語料又明明有譯文，於是會往「語料
缺了」的方向找，怎麼找都找不到。

2026-10-04 掃出來八句簡體中文就是這樣：

    An Iron Heart Part I#015   我̶叫̴什^么……名̷字̸     ← Zalgo 的組合刪除線
    A Journey Beyond#082       ❂暗                  ← 自己加的元素圖示

譯者從原文照抄了組合符號（U+0334–U+0338），或是自己補了一個遊戲圖示的字元。
兩種都是合法的 Unicode、JSON 也沒錯，既有的檢查一條都不會抱怨。

## 只看對話

只有對話走就地取代，所以只掃對話類的檔：`quest-dialogue.json`、
`dialogue-choice.json`、`secret-dialogue.json`、`quest/*.json`。別的地方
（物品提示框、介面）走別的繪製路徑，字型不是這一份。

佔位符（`{~}`、`{#}`、`{p}`、`{cN}`）與換行不算，那些在畫之前就被換掉了。

## 覆蓋表從哪來

`assets/wynnchayuan/font/dialogue/<lang>/coverage.txt`，由
`tools/font-coverage.py` 從字型抽出來的碼位區間。沒有那個檔的語言跳過
（還沒有對話字型，就沒有這道守門）。

用法：python tools/check-coverage.py
"""

from __future__ import annotations

import glob
import io
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(HERE, 'src', 'main', 'resources', 'assets',
                      'wynnchayuan')
BASE = os.path.join(ASSETS, 'translations')
FONT = os.path.join(ASSETS, 'font', 'dialogue')
LF = chr(10)
HOLE = re.compile(r'\{[^}]*\}')
DIALOGUE = ('quest-dialogue.json', 'dialogue-choice.json',
            'secret-dialogue.json')


def ranges(lang):
    """該語言對話字型收錄的碼位區間；沒有覆蓋表回 None。"""
    path = os.path.join(FONT, lang, 'coverage.txt')
    if not os.path.exists(path):
        return None
    out = []
    for line in io.open(path, encoding='utf-8'):
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        lo, _, hi = line.partition('-')
        out.append((int(lo, 16), int(hi or lo, 16)))
    out.sort()
    return out


def covered(cp, rs):
    lo, hi = 0, len(rs) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        a, b = rs[mid]
        if cp < a:
            hi = mid - 1
        elif cp > b:
            lo = mid + 1
        else:
            return True
    return False


def dialogue_files(lang):
    root = os.path.join(BASE, lang)
    out = [os.path.join(root, n) for n in DIALOGUE]
    out += sorted(glob.glob(os.path.join(root, 'quest', '*.json')))
    return [p for p in out if os.path.exists(p)]


def main():
    found = []
    skipped = []
    for lang in sorted(os.listdir(BASE)):
        root = os.path.join(BASE, lang)
        if not os.path.isdir(root):
            continue
        rs = ranges(lang)
        if rs is None:
            skipped.append(lang)
            continue
        for path in dialogue_files(lang):
            data = json.loads(io.open(path, encoding='utf-8').read())
            entries = data.get('entries') if isinstance(data, dict) else None
            if not isinstance(entries, dict):
                continue
            for key, row in entries.items():
                if not isinstance(row, dict):
                    continue
                dst = row.get('dst') or ''
                if not dst.strip():
                    continue
                bare = HOLE.sub('', dst).replace(LF, '')
                miss = sorted({c for c in bare if not covered(ord(c), rs)})
                if miss:
                    rel = os.path.relpath(path, BASE).replace(os.sep, '/')
                    found.append((rel, key, miss, dst))

    if skipped:
        print('沒有對話字型覆蓋表、跳過的語言：%s' % '、'.join(skipped))
    if not found:
        print('對話譯文都在對話字型裡畫得出來。')
        return 0

    print()
    print('對話的譯文裡有對話字型畫不出來的字。')
    print('那一句會整句退回英文，而且沒有任何訊號——畫面上只是「沒翻」。')
    print()
    for rel, key, miss, dst in found[:60]:
        print('  [%s] %s' % (rel, key))
        print('      缺 %s' % '  '.join(
            'U+%04X %s' % (ord(c), c) for c in miss))
        print('      %s' % dst.replace(LF, ' / ')[:110])
    if len(found) > 60:
        print('  ……還有 %d 條' % (len(found) - 60))
    print()
    print('掃到 %d 條。組合符號（U+0300–U+036F）直接拿掉，遊戲圖示的字元'
          '換成普通字（見 tools/check-glyphs.py）。' % len(found))
    return 1


if __name__ == '__main__':
    sys.exit(main())
