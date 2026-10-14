#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""功能頁那塊「即時預覽」要用的字與字型：產生 demo-data.js 與 demo-fonts.js。

    python tools/presence/build-demo.py           重寫兩個檔（字型那一個要裝 fontTools）
    python tools/presence/build-demo.py --check   跟現在的檔案比，不一樣就以 1 結束

網站的預覽照遊戲裡設定畫面右邊那一塊做（client/ui/Preview.java）：左邊幾個真的選項，
右邊一張小場景。兩種字都不另外寫、也不另外翻，直接從模組拿：

  ui    選項的名稱與說明——模組設定畫面的語言檔（lang/*.json），網站的八種語言各一份
  text  場景裡的字——語料（translations/<語言>/），七種譯文語言各一份；原文就是鍵

所以語料或語言檔改了那幾條，重跑這一支，網站就跟著變。

字型：場景裡的字用遊戲裡同一套點陣字（Fusion Pixel 10px，SIL OFL 1.1，模組本來就附著），
畫面才像遊戲。整套有四份、每份快 4 MB，網站只用得到幾百個字——從每一份裁出場景裡出現的
那些字，寫進 demo-fonts.js（base64），Worker 當成 /font/px-<代號>.ttf 送出去。裁出來的檔
留著原本的版權與授權說明（name 表），授權全文在 font/ofl-fusion.txt。
"""
import base64
import hashlib
import io
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'wynnchayuan')
TARGET = os.path.join(HERE, 'demo-data.js')
FONT_TARGET = os.path.join(HERE, 'demo-fonts.js')

# 網站的語言 → 模組設定畫面的語言檔
SITE_LANGS = {'zh': 'zh_tw', 'zh-cn': 'zh_cn', 'en': 'en_us', 'ja': 'ja_jp', 'ko': 'ko_kr',
              'ru': 'ru_ru', 'es': 'es_es', 'tr': 'tr_tr'}

# 譯文語言（預覽上面那個選單的順序）
TEXT_LANGS = ['zh_tw', 'zh_cn', 'ja_jp', 'ko_kr', 'ru_ru', 'es_es', 'tr_tr']

# 設定畫面的字：分頁、選項的名稱與說明、每一段的叫法
UI_KEYS = [
    'tab.items', 'tab.dialogue', 'tab.world', 'preview.title', 'data.language',
    'items.tooltip', 'items.tooltip.hint', 'items.names', 'items.names.hint', 'items.names.both',
    'panel.gap', 'panel.gap.hint',
    'dialogue.mode', 'dialogue.mode.hint', 'dialogue.choices', 'dialogue.choices.hint',
    'world.nametag', 'world.nametag.hint', 'world.chat', 'world.chat.hint',
    'world.tracker', 'world.tracker.hint', 'world.titles', 'world.titles.hint',
    'world.bossbar', 'world.bossbar.hint', 'world.scoreboard', 'world.scoreboard.hint',
    'world.helditem', 'world.helditem.hint',
    'mode.off', 'mode.on', 'mode.panel', 'mode.box', 'mode.replace', 'mode.both', 'mode.lookat',
]

# 場景裡的原文，與它在語料的哪一個檔
SAMPLES = [
    # 物品：Bony Bow 的說明，照遊戲現在的版面——每秒傷害、攻速、兩條需求、兩條詞條
    ('Bony Bow', 'gear-weapon.json'),
    ('{~} DPS', 'misc.json'),
    ('{#} Fast ({~} hits/s)', 'misc.json'),
    ('{#} Class Type{#}Archer/Hunter', 'misc.json'),
    ('{#} Combat Level{#}{~}', 'gui.json'),
    ('Agility', 'ui-labels.json'),
    ('Main Attack Damage', 'ui-labels.json'),
    # 對話
    ('Well, what are we waiting for?', 'quest/king-s-recruit.json'),
    ('Yes', 'quest.json'),
    ('Goodbye.', 'quest-dialogue.json'),
    ('to continue', 'misc.json'),
    # 世界與聊天
    ('Zombie', 'npc.json'),
    ('Weapon Merchant', 'npc.json'),
    ('Level Up!', 'misc.json'),
    ('World Event', 'wynntils.json'),
    ('Click To Track', 'wynntils.json'),
    ('The trade has been completed!', 'misc.json'),
    ('Daily Objective:', 'raid.json'),
    ('- Loot Chests T{~}+: {~}/{~}', 'raid.json'),
    ('- Mobs slain: {~}/{~}', 'raid.json'),
    # 功能頁卡片上的例句（別的幾句上面已經有了）
    ('Ability Points', 'gui.json'),
    ('Trade Market', 'npc.json'),
    ('Secret Discovery', 'quest-ui.json'),
]

# 字型：代號 → (模組附的那一份, 用它的譯文語言)。拉丁、西里爾、土耳其字母都用繁中那一份的
FONTS = [
    ('tc', 'fusion_pixel_10px_zh_hant.ttf', ['zh_tw', 'ru_ru', 'es_es', 'tr_tr']),
    ('sc', 'fusion-pixel-10pxl-zh_hans.ttf', ['zh_cn']),
    ('ja', 'fusion-pixel-10px-ja.ttf', ['ja_jp']),
    ('ko', 'fusion-pixel-10px-ko.ttf', ['ko_kr']),
]
# 每一份都帶的：鍵盤上打得出來的字（原文、數字、NPC 的名字）與場景裡自己寫的幾個符號
ALWAYS = ''.join(chr(c) for c in range(0x20, 0x7F)) + '…★▼►·'
# 這套字型沒有、網頁自己畫的符號（勾、叉）。裁字的時候不算缺字
DRAWN = '✔✖✓'


def load(path):
    with io.open(path, encoding='utf-8-sig') as handle:
        return json.load(handle)


def find(node, src):
    """語料的檔有好幾種形狀（條目帶 src／dst、或原文直接當鍵）：都找。"""
    if isinstance(node, dict):
        if node.get('src') == src and isinstance(node.get('dst'), str) and node['dst']:
            return node['dst']
        direct = node.get(src)
        if isinstance(direct, str) and direct and src not in ('src', 'dst'):
            return direct
        for value in node.values():
            if isinstance(value, (dict, list)):
                hit = find(value, src)
                if hit:
                    return hit
    elif isinstance(node, list):
        for value in node:
            hit = find(value, src)
            if hit:
                return hit
    return None


def collect():
    problems = []
    ui = {}
    for site, mod in SITE_LANGS.items():
        lang = load(os.path.join(ASSETS, 'lang', mod + '.json'))
        ui[site] = {}
        for key in UI_KEYS:
            value = lang.get('wynnchayuan.' + key)
            if not value:
                problems.append('%s 的語言檔沒有 wynnchayuan.%s' % (mod, key))
            ui[site][key] = value or ''
    text = {}
    for lang in TEXT_LANGS:
        text[lang] = {}
        cache = {}
        for src, name in SAMPLES:
            if name not in cache:
                cache[name] = load(os.path.join(ASSETS, 'translations', lang, *name.split('/')))
            value = find(cache[name], src)
            if not value:
                problems.append('%s 的 %s 沒有「%s」的譯文' % (lang, name, src))
            text[lang][src] = value or ''
    if problems:
        sys.exit('\n'.join(problems))
    return ui, text


def build(ui, text):
    body = json.dumps({'ui': ui, 'text': text}, ensure_ascii=False, indent=1)
    return ('/**\n * 功能頁「即時預覽」用的字。這個檔是 build-demo.py 從模組的語言檔與語料產生的，不要手改。\n */\n'
            'export const DEMO = ' + body + ';\n')


def wanted(text):
    """每一份字型要有哪些字：{代號: 排好的字串}。"""
    out = {}
    for code, name, langs in FONTS:
        chars = set(ALWAYS)
        for lang in langs:
            for value in text[lang].values():
                chars.update(value)
        chars -= set(DRAWN) | {'\n', '\r', '\t'}
        out[code] = ''.join(sorted(chars))
    return out


def subset(name, chars):
    """從模組附的那一份裁出 chars。回傳 (位元組, 字型裡沒有的字)。"""
    from fontTools import subset as tool
    from fontTools.ttLib import TTFont
    font = TTFont(os.path.join(ASSETS, 'font', name))
    have = font.getBestCmap()
    missing = ''.join(c for c in chars if ord(c) not in have)
    options = tool.Options()
    options.layout_features = []            # 點陣字用不到連字與字距表
    options.name_IDs = ['*']                # 版權與授權說明留著（OFL 的要求）
    options.name_languages = ['*']
    options.notdef_outline = True
    options.glyph_names = False
    options.hinting = False
    worker = tool.Subsetter(options)
    worker.populate(unicodes=[ord(c) for c in chars if ord(c) in have])
    worker.subset(font)
    out = io.BytesIO()
    font.save(out)
    return out.getvalue(), missing


def build_fonts(text):
    need = wanted(text)
    fonts = {}
    for code, name, langs in FONTS:
        data, missing = subset(name, need[code])
        if missing:
            sys.exit('%s 沒有這些字：%s（網頁自己畫的符號加進 DRAWN，不然換一句示範）' % (name, missing))
        fonts[code] = {'covers': need[code], 'v': hashlib.sha256(data).hexdigest()[:10],
                       'data': base64.b64encode(data).decode('ascii')}
    body = json.dumps(fonts, ensure_ascii=False, indent=1)
    return ('/**\n * 功能頁「即時預覽」用的點陣字：Fusion Pixel 10px（© 2022 TakWolf，SIL Open Font License 1.1）\n'
            ' * 裁出場景裡出現的那些字。這個檔是 build-demo.py 產生的，不要手改。\n */\n'
            'export const FONTS = ' + body + ';\n')


def read(path):
    try:
        with io.open(path, encoding='utf-8', newline='') as handle:
            return handle.read().replace('\r\n', '\n')
    except IOError:
        return ''


def main():
    ui, text = collect()
    out = build(ui, text)
    if '--check' in sys.argv:
        if read(TARGET) != out:
            sys.exit('demo-data.js 跟語言檔／語料不一樣了：重跑 python tools/presence/build-demo.py')
        # 字型不重裁（CI 不必裝 fontTools）：只看現有的那一份涵蓋的字夠不夠
        have = read(FONT_TARGET)
        for code, chars in wanted(text).items():
            marker = json.dumps({'covers': chars}, ensure_ascii=False)[1:-1]
            if marker not in have:
                sys.exit('demo-fonts.js 的 %s 涵蓋的字跟語料不一樣了：重跑 python tools/presence/build-demo.py' % code)
        print('demo-data.js 與 demo-fonts.js 是最新的。')
        return
    with io.open(TARGET, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write(out)
    with io.open(FONT_TARGET, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write(build_fonts(text))
    print('寫好了 demo-data.js 與 demo-fonts.js')


if __name__ == '__main__':
    main()
