"""Subset the site's rounded faces (all SIL OFL 1.1) to the site's text, one set per script, into site-fonts.js.

python tools/presence/build-site-fonts.py [--check] [--src <folder>]

The source fonts are not in the repository; --src (or WCY_SITE_FONT_SRC) names the folder that holds them.
Where each comes from and its licence: README.md and font-licenses/.
"""
import base64
import hashlib
import io
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
CREDITS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'wynnchayuan', 'credits.json')
TARGET = os.path.join(HERE, 'site-fonts.js')
SOURCES = ['site-pages.js', 'site-layout.js', 'site-constants.js']
EXTRA = '，。、；：？！「」『』（）《》〈〉…—・×→←↑↓～％＋－／＝·“”‘’★©'

NUNITO = ('Nunito[wght].ttf', {'wght': (400, 800)})
HUNINN = ('jf-openhuninn-2.1.ttf', None)
ZEN = ('ZenMaruGothic-Regular.ttf', None)
HAN_CN = ('ResourceHanRoundedCN-VF.otf', {'wght': 400, 'ROND': 100})
HAN_KR = ('ResourceHanRoundedKR-VF.otf', {'wght': 400, 'ROND': 100})
FILLS = [NUNITO, HUNINN, HAN_CN, HAN_KR]

SETS = [
    ('latin', None, NUNITO),
    ('tc', 'zh', HUNINN),
    ('sc', 'zh-cn', HAN_CN),
    ('ja', 'ja', ZEN),
    ('ko', 'ko', HAN_KR),
]


def site_text():
    code = ("Promise.all([import('./site-strings.js'), import('./demo-data.js')]).then(([m, d]) => "
            "process.stdout.write(JSON.stringify({ text: m.TEXT, names: m.LANG_NAMES, demo: d.DEMO.ui })))")
    out = subprocess.run(['node', '--no-warnings', '-e', code], cwd=HERE, capture_output=True, check=True)
    return json.loads(out.stdout.decode('utf-8'))


def letters(value):
    found = set()
    def walk(item):
        if isinstance(item, str):
            found.update(re.sub(r'<[^>]+>', '', item))
        elif isinstance(item, dict):
            for inner in item.values():
                walk(inner)
        elif isinstance(item, list):
            for inner in item:
                walk(inner)
    walk(value)
    return found


def cjk(chars):
    return {c for c in chars if ord(c) >= 0x2E80}


def wanted():
    data = site_text()
    shared = set(chr(c) for c in range(0x20, 0x7f)) | set(EXTRA) | letters(data['names'])
    for text in list(data['text'].values()) + list(data['demo'].values()):
        shared |= {c for c in letters(text) if 0x20 <= ord(c) < 0x2E80}
    with io.open(CREDITS, encoding='utf-8') as handle:
        people = letters(json.load(handle))
    markup = set()
    for name in SOURCES:
        with io.open(os.path.join(HERE, name), encoding='utf-8') as handle:
            markup |= cjk(handle.read())
    need = {}
    for code, lang, _ in SETS:
        chars = set(shared) | {c for c in people if 0x20 <= ord(c)}
        if lang:
            chars |= cjk(letters(data['text'][lang])) | cjk(letters(data['demo'].get(lang, {}))) | markup
        need[code] = ''.join(sorted(c for c in chars if c not in '  '))
    return need


class Sources:
    def __init__(self, folder):
        self.folder = folder
        self.maps = {}

    def path(self, name):
        found = os.path.join(self.folder, name)
        if not os.path.isfile(found):
            sys.exit('missing source font: ' + found + ' (see README.md, site-fonts.js)')
        return found

    def cmap(self, name):
        if name not in self.maps:
            from fontTools.ttLib import TTFont
            self.maps[name] = set(TTFont(self.path(name), lazy=True).getBestCmap())
        return self.maps[name]


def options(hinting, kern=True):
    from fontTools import subset as tool
    chosen = tool.Options()
    chosen.layout_features = ['kern'] if kern else []
    chosen.name_IDs = ['*']
    chosen.name_languages = ['*']
    chosen.notdef_outline = True
    chosen.hinting = hinting
    return chosen


def rename(font, label):
    table = font['name']
    table.names = [n for n in table.names if n.nameID in (0, 7, 13, 14) or n.nameID >= 256]
    style = 'Regular'
    family = 'WCY Site ' + label
    for nid, value in ((1, family), (2, style), (3, family + ' ' + style), (4, family + ' ' + style),
                       (6, re.sub(r'[^A-Za-z0-9]', '', family) + '-' + style)):
        table.setName(value, nid, 3, 1, 0x409)
    if 'fvar' in font:
        for instance in font['fvar'].instances:
            instance.postscriptNameID = 0xFFFF
    if 'STAT' in font and font['STAT'].table.ElidedFallbackNameID not in {n.nameID for n in table.names}:
        font['STAT'].table.ElidedFallbackNameID = 2


def subset(sources, source, chars, label):
    from fontTools import subset as tool
    from fontTools.ttLib import TTFont
    from fontTools.varLib import instancer
    name, where = source
    font = TTFont(sources.path(name))
    have = sources.cmap(name)
    covered = ''.join(c for c in chars if ord(c) in have)
    cff2 = 'CFF2' in font
    worker = tool.Subsetter(options(cff2, not cff2))
    worker.populate(unicodes=[ord(c) for c in covered])
    worker.subset(font)
    if where:
        pinned = all(not isinstance(value, tuple) for value in where.values())
        font = instancer.instantiateVariableFont(font, where, downgradeCFF2=pinned)
    if cff2:
        again = tool.Subsetter(options(False, False))
        again.populate(unicodes=[ord(c) for c in covered])
        again.subset(font)
    rename(font, label)
    font.recalcTimestamp = False
    font.flavor = 'woff'
    out = io.BytesIO()
    font.save(out)
    return out.getvalue(), covered


def ranges(chars):
    points = sorted(ord(c) for c in chars)
    parts = []
    start = prev = points[0]
    for point in points[1:] + [None]:
        if point is not None and point == prev + 1:
            prev = point
            continue
        parts.append('U+%X' % start if start == prev else 'U+%X-%X' % (start, prev))
        if point is not None:
            start = prev = point
    return ','.join(parts)


def face(data, extra):
    one = dict(extra)
    one['v'] = hashlib.sha256(data).hexdigest()[:10]
    one['data'] = base64.b64encode(data).decode('ascii')
    return one


def build(sources, need):
    fonts = {}
    for code, _, first in SETS:
        data, covered = subset(sources, first, need[code], code)
        faces = [face(data, {'file': code, 'weight': '400 800'})]
        print('%s: %d characters, %d bytes woff' % (code, len(covered), len(data)))
        left = ''.join(c for c in need[code] if c not in covered)
        fill = 0
        for source in FILLS:
            if source[0] == first[0] or not left:
                continue
            take = ''.join(c for c in left if ord(c) in sources.cmap(source[0]))
            if not take:
                continue
            fill += 1
            weight = '400 800' if source[1] and isinstance(source[1].get('wght'), tuple) else '400'
            data, _ = subset(sources, source, take, code + ' fill')
            faces.append(face(data, {'file': '%s-f%d' % (code, fill), 'weight': weight, 'range': ranges(take)}))
            print('%s-f%d: %d characters, %d bytes woff' % (code, fill, len(take), len(data)))
            covered += take
            left = ''.join(c for c in left if c not in take)
        fonts[code] = {'covers': covered, 'absent': left, 'faces': faces}
    return fonts


def main():
    need = wanted()
    if '--check' in sys.argv:
        with io.open(TARGET, encoding='utf-8') as handle:
            text = handle.read()
        fonts = json.loads(text[text.index('{'):text.rindex('}') + 1])
        missing = {code: ''.join(c for c in need[code] if c not in fonts[code]['covers'] + fonts[code]['absent'])
                   for code, _, _ in SETS}
        missing = {code: chars for code, chars in missing.items() if chars}
        if missing:
            sys.exit('site-fonts.js is missing: ' + json.dumps(missing, ensure_ascii=False) + ' (rerun build-site-fonts.py)')
        return
    folder = os.environ.get('WCY_SITE_FONT_SRC')
    if '--src' in sys.argv:
        folder = sys.argv[sys.argv.index('--src') + 1]
    if not folder:
        sys.exit('pass --src <folder with the source fonts> or set WCY_SITE_FONT_SRC (see README.md)')
    fonts = build(Sources(folder), need)
    body = json.dumps(fonts, ensure_ascii=False, indent=1)
    with io.open(TARGET, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write('/**\n * Site text faces, all SIL Open Font License 1.1 (font-licenses/): Nunito (latin), '
                     'jf open huninn 2.1 (tc), Resource Han Rounded CN 1.910 (sc), Zen Maru Gothic (ja),\n'
                     ' * Resource Han Rounded KR 1.910 (ko), each subset to the site text and renamed "WCY Site <set>"; '
                     'the -f faces fill characters the main face lacks.\n'
                     ' * Generated by build-site-fonts.py; do not edit by hand.\n */\n'
                     'export const UI_FONTS = ' + body + ';\n')


if __name__ == '__main__':
    main()
