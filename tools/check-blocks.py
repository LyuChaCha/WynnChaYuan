# -*- coding: utf-8 -*-
"""賭注敘述的逐行條目必須留空。

## 為什麼需要這一條

討伐戰的賭注敘述在 `raid.json` 裡有<b>兩種</b>條目：

* **整段**一條，鍵是把英文那幾行用空格接起來的
  （`For each {~} health you lack, make your attacks {~} weaker`）；
* **逐行**好幾條，鍵是英文被 tooltip 寬度折出來的每一行。

逐行那幾條<b>刻意留空</b>——空條目不會載入，整段才蓋得上去。中文比英文短，
整段翻出來一行就放得下；照英文的斷點走就會在句子中間硬斷開，還留一個懸空的
逗號。實機回報的原話是「gambit 幫忙整理一次排版，讓它好讀，而不是莫名斷句」。

`GambitBlockTest` 在守這件事，但它是 Java 測試，而<b>語料 PR 不跑
`gradle build`</b>——2026-10-04 的 #1009 就是這樣把八條逐行的填了回去，
七關 CI 全綠。所以要有一支純 Python 的檢查擋在語料那一路上。

## 為什麼是寫死的清單

「逐行的一律留空」推不出來：`raid.json` 裡有二十幾條逐行條目<b>是</b>翻好的
（人數不足的提示、消耗品上限…），那些讀起來沒問題，沒有人要求改成整段。
真正被回報過、而且已經改成整段的只有下面這幾句。鍵在遊戲改措辭之後會消失，
所以找不到的鍵直接跳過，不當成錯。
"""
import glob
import io
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'wynnchayuan',
                    'translations')
# 這幾句已經改成整段收了，逐行的必須留空
MUST_BE_BLANK = [
    # 啟用賭注的說明
    'The more gambits you enable the',
    'more rewards next ones will give',
    # 開始討伐戰會鎖裝備
    'temporarily lock your',
    'equipment and potions',
    # 淌血戰士
    'For each {~} health you lack,',
    'make your attacks {~} weaker',
    # 血友者
    'Every hit you take will deal',
    'health as damage',
]

bad = 0
for path in sorted(glob.glob(os.path.join(BASE, '*', 'raid.json'))):
    lang = os.path.basename(os.path.dirname(path))
    data = json.loads(io.open(path, encoding='utf-8').read())
    for key in MUST_BE_BLANK:
        if key not in data:
            continue                      # 遊戲改措辭了，不是錯
        if not isinstance(data[key], str) or not data[key].strip():
            continue
        bad += 1
        print('[錯誤] %s/raid.json :: %s' % (lang, key))
        print('       這一行屬於一段「整段收」的賭注敘述，逐行的必須留空，')
        print('       不然中文會被釘死在英文的斷點上。現在寫著：%s'
              % data[key][:40])
        joined = [k for k in data
                  if isinstance(data[k], str) and data[k].strip()
                  and k != key and key in k.replace('\n', ' ')]
        if joined:
            print('       整段那一條是：%s' % joined[0].replace('\n', ' ')[:70])

if bad:
    print()
    print('%d 條逐行的賭注敘述有譯文，必須留空。' % bad)
    sys.exit(1)
print('賭注敘述的逐行條目都留空了。')
