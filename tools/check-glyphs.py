# -*- coding: utf-8 -*-
"""譯文不可以用到「Wynncraft 拿去當圖示」的普通字元。

## 為什麼需要這一條

Wynncraft 的資源包把一批<b>看起來很正常</b>的 Unicode 字元指到自己的圖上。
`minecraft:default` 引用的 `deprecated` 字型裡有：

    Á        -> font/screen/static/fade.png   全螢幕淡出黑幕
    ² ¼ ½    -> font/currency.png             綠寶石／方塊／液態
    Ⓐ–Ⓛ      -> font/profession.png           採集職業
    ⓐ–ⓩ      -> font/language/high_gavellian.png
    ⑴–⑿ ０１２ -> font/language/wynnic.png       遊戲裡的古語與數字
    ❤ ✔ ⚔ ☀ … -> font/common.png              各種符號

譯文寫了其中任何一個，畫面上出現的<b>不是那個字</b>，而是那張圖。
實機回報：西班牙文的「Área descubierta」害整個畫面變全黑——那不是缺字，
是我們請遊戲畫了一張蓋住整個畫面的黑圖。

## 原文本來就有的不算

Wynncraft 自己的字串就用這些圖：綠寶石數量是「1½²」、採集等級是「Ⓕ」。
譯文照抄是<b>對的</b>。所以只看「原文沒有、譯文才多出來」的那些。

## Á 不在這份清單裡

它是西班牙文真的需要的字母，而且 Wynncraft 的對話字型
（`hud/dialogue/text/…`，沒有引用 `deprecated`）畫得出來。所以它留在語料裡，
由 `LineTranslator#deIcon` 在畫不出來的地方換成 `A`。見那支方法的註解。

用法：python tools/check-glyphs.py [--quiet]
"""
import io
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'src', 'main', 'resources', 'assets', 'wynnchayuan',
                    'translations')

# 由伺服器資源包的 assets/minecraft/font/{default,deprecated,overlay}.json
# 抽出來的：非私用區、而且被指到圖片而不是字母的碼位。
# 私用區（U+E000–U+F8FF 等）不必列——那本來就沒有人會打進譯文。
ICONS = {
    '²': 'font/currency.png',
    '¼': 'font/currency.png',
    '½': 'font/currency.png',
    '⌚': 'font/common.png',
    '⌛': 'font/common.png',
    '☀': 'font/common.png',
    '☠': 'font/common.png',
    '⚔': 'font/common.png',
    '⚕': 'font/common.png',
    '⚘': 'font/common.png',
    '⛨': 'font/common.png',
    '✃': 'font/common.png',
    '✔': 'font/common.png',
    '✖': 'font/common.png',
    '✜': 'font/common.png',
    '✣': 'font/common.png',
    '✤': 'font/common.png',
    '✦': 'font/common.png',
    '✮': 'font/common.png',
    '✹': 'font/common.png',
    '✺': 'font/common.png',
    '❁': 'font/common.png',
    '❉': 'font/common.png',
    '❋': 'font/common.png',
    '❤': 'font/common.png',
    '➲': 'font/common.png',
    '➼': 'font/common.png',
    '➽': 'font/common.png',
    '⬌': 'font/common.png',
    '⬟': 'font/common.png',
    '⬠': 'font/common.png',
    '⬡': 'font/common.png',
    '⬣': 'font/common.png',
    '⬤': 'font/common.png',
    '⭑': 'font/common.png',
}
# 連續的幾段一起補上，免得手抄漏掉。
for _cp in range(0x2474, 0x2480):               # ⑴–⑿
    ICONS[chr(_cp)] = 'font/language/wynnic.png'
for _cp in range(0x249c, 0x24b6):               # ⒜–⒵
    ICONS[chr(_cp)] = 'font/language/wynnic.png'
for _cp in range(0x24b6, 0x24c2):               # Ⓐ–Ⓛ
    ICONS[chr(_cp)] = 'font/profession.png'
for _cp in range(0x24d0, 0x24ea):               # ⓐ–ⓩ
    ICONS[chr(_cp)] = 'font/language/high_gavellian.png'
for _cp in range(0xff10, 0xff13):               # ０１２
    ICONS[chr(_cp)] = 'font/language/wynnic.png'


REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 我們自己那幾份文字：version.json 的更新說明、lang/*.json 的 F6 介面字。
#
# 這些<b>不經過</b> LineTranslator#deIcon —— 那一支只跑譯文。這裡寫什麼就原樣
# 送去畫，而畫它們的是 minecraft:default，也就是引用 deprecated 的那一份。
#
# 2026-09-25 踩到：0.2.3 的更新說明裡為了解釋「那個字母是全螢幕黑幕」而真的
# 寫了那個字母，於是 F6 的更新說明整片黑——說明自己示範了它在講的那個 bug。
#
# 只擋會蓋住整個畫面的那一個。其餘圖示字元（✔ 之類）在 F6 的提示裡本來就用著，
# 畫出來是圖也剛好是想要的。
FATAL = {chr(0xC1)}


def our_files():
    import glob
    yield os.path.join(REPO, 'version.json')
    for path in sorted(glob.glob(os.path.join(
            REPO, 'src', 'main', 'resources', 'assets', 'wynnchayuan',
            'lang', '*.json'))):
        yield path


def walk_strings(node, where=''):
    if isinstance(node, dict):
        for k, v in node.items():
            yield from walk_strings(v, where + '/' + str(k))
    elif isinstance(node, list):
        for i, v in enumerate(node):
            yield from walk_strings(v, where + '[%d]' % i)
    elif isinstance(node, str):
        yield where, node


def ours_problems():
    """我們自己的字串裡有沒有會蓋掉畫面的字元。"""
    out = []
    for path in our_files():
        if not os.path.isfile(path):
            continue
        try:
            doc = json.loads(io.open(path, encoding='utf-8').read())
        except (ValueError, OSError):
            continue
        for where, text in walk_strings(doc):
            for ch in sorted(set(text) & FATAL):
                out.append((os.path.basename(path), where, ch, text))
    return out


def entries(path):
    """逐條吐出 (鍵, 原文, 譯文)。扁平檔與有 entries 的都吃得下。"""
    try:
        doc = json.load(io.open(path, encoding='utf-8'))
    except (ValueError, OSError):
        return
    body = doc.get('entries', doc) if isinstance(doc, dict) else None
    if not isinstance(body, dict):
        return
    for key, value in body.items():
        if key == '_meta':
            continue
        if isinstance(value, dict):
            yield key, value.get('src', ''), value.get('dst', '')
        elif isinstance(value, str):
            yield key, key, value


def main():
    quiet = '--quiet' in sys.argv
    found = []
    for dirpath, _, names in os.walk(ROOT):
        for name in sorted(names):
            if not name.endswith('.json'):
                continue
            path = os.path.join(dirpath, name)
            rel = path.replace(os.sep, '/').split('/translations/')[1]
            for key, src, dst in entries(path):
                if not dst:
                    continue
                for ch in sorted(set(dst) & set(ICONS)):
                    if ch in src:
                        continue          # 原文本來就有，那是遊戲的圖示
                    found.append((rel, key, ch, ICONS[ch], dst))

    ours = ours_problems()
    for name, where, ch, text in ours:
        print('我們自己的字串裡有會蓋掉整個畫面的字元：')
        print('  [%s] %s' % (name, where))
        print('      U+%04X -> font/screen/static/fade.png（全螢幕黑幕）' % ord(ch))
        print('      %s' % text[:110])
        print('  要提到它就用文字描述，不要把字元本身寫進去。')

    if not found and not ours:
        if not quiet:
            print('譯文沒有用到被挪用成圖示的字元。')
        return 0
    if not found:
        return 1

    print('這些譯文用到了 Wynncraft 拿去當圖示的字元，畫出來會是圖不是字：')
    print()
    for rel, key, ch, file, dst in found[:60]:
        print('  [%s] %s' % (rel, key))
        print('      U+%04X %s -> %s' % (ord(ch), ch, file))
        print('      %s' % dst[:110])
    if len(found) > 60:
        print('  ……還有 %d 條' % (len(found) - 60))
    print()
    print('掃到 %d 條。換成別的寫法，或者改用原文本來就有的那個字元。' % len(found))
    return 1


if __name__ == '__main__':
    sys.exit(main())
