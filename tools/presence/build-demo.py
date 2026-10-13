#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""功能頁那塊「即時預覽」要用的字：產生 demo-data.js。

    python tools/presence/build-demo.py           重寫 demo-data.js
    python tools/presence/build-demo.py --check   跟現在的檔案比，不一樣就以 1 結束

網站的預覽照遊戲裡設定畫面右邊那一塊做（client/ui/Preview.java）：左邊幾個真的選項，
右邊一張小場景。兩種字都不另外寫、也不另外翻，直接從模組拿：

  ui    選項的名稱與說明——模組設定畫面的語言檔（lang/*.json），網站的八種語言各一份
  text  場景裡的字——語料（translations/<語言>/），七種譯文語言各一份；原文就是鍵

所以語料或語言檔改了那幾條，重跑這一支，網站就跟著變。場景裡用的原文跟 Preview.java
同一組（都是每個語言都翻了的短字串）。
"""
import io
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'wynnchayuan')
TARGET = os.path.join(HERE, 'demo-data.js')

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
    ('Bony Bow', 'gear-weapon.json'),
    ('Durability', 'ui-labels.json'),
    ('Health', 'ui-labels.json'),
    ('Mana', 'ability-labels.json'),
    ('Well, what are we waiting for?', 'quest/king-s-recruit.json'),
    ('Yes', 'quest.json'),
    ('Goodbye.', 'quest-dialogue.json'),
    ('Zombie', 'npc.json'),
    ('Weapon Merchant', 'npc.json'),
    ('Level Up!', 'misc.json'),
    ('World Event', 'wynntils.json'),
    ('Click To Track', 'wynntils.json'),
    ('The trade has been completed!', 'misc.json'),
    ('Daily Objective:', 'raid.json'),
    ('- Loot Chests T{~}+: {~}/{~}', 'raid.json'),
    ('- Mobs slain: {~}/{~}', 'raid.json'),
]


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


def build():
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
    body = json.dumps({'ui': ui, 'text': text}, ensure_ascii=False, indent=1)
    return ('/**\n * 功能頁「即時預覽」用的字。這個檔是 build-demo.py 從模組的語言檔與語料產生的，不要手改。\n */\n'
            'export const DEMO = ' + body + ';\n')


def main():
    out = build()
    if '--check' in sys.argv:
        try:
            with io.open(TARGET, encoding='utf-8', newline='') as handle:
                have = handle.read().replace('\r\n', '\n')
        except IOError:
            have = ''
        if have != out:
            sys.exit('demo-data.js 跟語言檔／語料不一樣了：重跑 python tools/presence/build-demo.py')
        print('demo-data.js 是最新的。')
        return
    with io.open(TARGET, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write(out)
    print('寫好了 demo-data.js')


if __name__ == '__main__':
    main()
