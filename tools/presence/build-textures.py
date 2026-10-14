#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""預覽畫面用的材質：從 Wynncraft 的資源包與官方的物品圖示組出來，打包成 demo-textures.js。

    python tools/presence/build-textures.py                      把 textures/ 裡現有的檔打包成 demo-textures.js
    python tools/presence/build-textures.py --pack <資源包.zip>   先從資源包重組 textures/ 裡的介面材質，再打包
    python tools/presence/build-textures.py --icons              先重抓官方的物品圖示，再打包
    python tools/presence/build-textures.py --shots <資料夾>      先重做兩張背景（world.png、street.png），再打包
    python tools/presence/build-textures.py --skin <玩家名字>     先重抓背包裡那個人的皮膚（Mojang 的公開資料），再打包
    python tools/presence/build-textures.py --check              textures/ 與 demo-textures.js 對得上就以 0 結束

使用者 2026-10-14：「材質部分可以使用 Wynncraft 的材質，可以不用生成」。功能頁與首頁那一面
「遊戲畫面」原本是程式畫的示意圖；現在物品說明的框、對話框與頭像、血條、快捷列、背包都是
遊戲裡真的那一張，位置照實機截圖擺。

這些圖是 Wynncraft 的（© Wynncraft），不在這個倉庫的授權範圍裡；只用在網站的預覽上，
不進模組的 jar。來源：

  介面材質   伺服器資源包（玩過 Wynncraft 的人，客戶端的 downloads/ 底下有一份快取）
  物品圖示   官方的 CDN（cdn.wynncraft.com/nextgen/itemguide/，官方 API 的 icon 欄位指到的那一批）
  背景       實機截圖（F1 拍的，畫面上沒有介面與玩家名字），裁切、縮小。兩張都在 Detlas 一帶——
             預覽裡的人物是 22 等，背景要是那個等級會去的地方（使用者 2026-10-14 指出原本那兩張是 120 等的區域）。
             street.png 是街上的隊伍尋找器：NPC 頭上的名牌先從截圖上拿掉了（網頁自己畫，才能跟著語言換）
  皮膚       背包裡那個人：使用者自己的皮膚（Green_teaTW），Mojang 的 session server 查得到的公開資料

資源包的 zip 被動過手腳（local header 的檔名長度是 0、PNG 的 zlib 校驗碼是錯的），一般的
工具打不開，所以這裡自己解。材質裡 alpha 是 1 的像素是給它的 shader 看的記號，要當成透明。
"""
import base64
import glob
import hashlib
import io
import json
import os
import struct
import sys
import zipfile
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.join(HERE, 'textures')
TARGET = os.path.join(HERE, 'demo-textures.js')
T = 'assets/minecraft/textures/'
CDN = 'https://cdn.wynncraft.com/nextgen/itemguide/3.3/'
# 官方的物品圖示：檔名 → CDN 上的名字。Bony Bow 的圖示就是 bow.air1（官方 API 查得到）
ICONS = {'bow': 'bow.air1', 'spear': 'spear.fire2', 'wand': 'wand.water3', 'dagger': 'dagger.thunder3',
         'relik': 'relik.earth2'}
MIME = {'.png': 'image/png', '.webp': 'image/webp'}


# ------------------------------------------------------------------ 資源包
class Pack:
    def __init__(self, path):
        self.zip = zipfile.ZipFile(path)
        with open(path, 'rb') as handle:
            self.raw = handle.read()

    def read(self, name):
        info = self.zip.getinfo(name)
        off = info.header_offset
        nlen, elen = struct.unpack('<HH', self.raw[off + 26:off + 30])
        start = off + 30 + nlen + elen
        if info.compress_type == 8:
            return zlib.decompressobj(-15).decompress(self.raw[start:start + max(info.compress_size, 0) + 8000000])
        blob = self.raw[start:start + 20000000]          # 沒壓縮存的：大小是假的，PNG 自己有 IEND 可以收尾
        end = blob.find(b'IEND')
        return blob[:end + 8] if end > 0 else blob[:max(info.file_size, 0)]

    def image(self, name):
        """解一張 PNG（RGBA）。alpha 是 1 的記號像素清成透明，254 的補成不透明。"""
        from PIL import Image
        data = self.read(T + name)
        assert data[:8] == bytes([137, 80, 78, 71, 13, 10, 26, 10]), name
        pos = 8
        idat = b''
        plte = trns = None
        while pos < len(data):
            length, kind = struct.unpack('>I4s', data[pos:pos + 8])
            body = data[pos + 8:pos + 8 + length]
            pos += 12 + length
            if kind == b'IHDR':
                w, h, depth, ctype, _, _, lace = struct.unpack('>IIBBBBB', body)
            elif kind == b'IDAT':
                idat += body
            elif kind == b'PLTE':
                plte = body
            elif kind == b'tRNS':
                trns = body
            elif kind == b'IEND':
                break
        assert lace == 0 and (depth == 8 or ctype in (0, 3)), (name, lace, depth, ctype)
        px = zlib.decompressobj(-15).decompress(idat[2:])     # 跳過 zlib 的頭，尾巴的校驗碼不看
        channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
        stride = (w * channels * depth + 7) // 8
        step = max(1, channels * depth // 8)                  # 還原 filter 時「左邊那一格」隔幾個位元組

        def sample(line, x):
            """調色盤或灰階的一格（位元深度可能不到 8）。"""
            if depth == 8:
                return line[x]
            per = 8 // depth
            return (line[x // per] >> (8 - depth * (x % per + 1))) & ((1 << depth) - 1)
        rows = []
        prev = bytearray(stride)
        at = 0
        for _y in range(h):
            kind = px[at]
            line = bytearray(px[at + 1:at + 1 + stride])
            at += 1 + stride
            for x in range(stride):
                a = line[x - step] if x >= step else 0
                b = prev[x]
                c = prev[x - step] if x >= step else 0
                if kind == 1:
                    line[x] = (line[x] + a) & 255
                elif kind == 2:
                    line[x] = (line[x] + b) & 255
                elif kind == 3:
                    line[x] = (line[x] + (a + b) // 2) & 255
                elif kind == 4:
                    pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                    line[x] = (line[x] + (a if pa <= pb and pa <= pc else (b if pb <= pc else c))) & 255
            rows.append(line)
            prev = line
        img = Image.new('RGBA', (w, h))
        out = img.load()
        for y in range(h):
            line = rows[y]
            for x in range(w):
                if ctype == 6:
                    r, g, b, a = line[x * 4:x * 4 + 4]
                elif ctype == 2:
                    r, g, b = line[x * 3:x * 3 + 3]
                    a = 255
                elif ctype == 4:
                    r = g = b = line[x * 2]
                    a = line[x * 2 + 1]
                elif ctype == 3:
                    v = sample(line, x)
                    r, g, b = plte[v * 3:v * 3 + 3]
                    a = trns[v] if trns and v < len(trns) else 255
                else:
                    v = sample(line, x)
                    r = g = b = v * 255 // ((1 << depth) - 1)
                    a = 0 if trns and v == struct.unpack('>H', trns[:2])[0] else 255
                out[x, y] = (0, 0, 0, 0) if a < 8 else (r, g, b, 255 if a >= 250 else a)
        return img


def find_pack():
    """Prism／官方啟動器的 downloads/ 底下最新的那一份。"""
    roots = [os.path.expandvars(r'%APPDATA%\PrismLauncher\instances\*\minecraft\downloads\*\*'),
             os.path.expandvars(r'%APPDATA%\.minecraft\downloads\*\*')]
    found = [p for root in roots for p in glob.glob(root) if os.path.isfile(p) and os.path.getsize(p) > 5000000]
    return sorted(found, key=os.path.getmtime)[-1] if found else None


def save(img, name):
    os.makedirs(DIR, exist_ok=True)
    img.save(os.path.join(DIR, name), optimize=True)
    print('  %-18s %d×%d' % (name, img.width, img.height))


def from_pack(path):
    from PIL import Image
    pack = Pack(path)
    tex = pack.image
    print('資源包：' + path)

    # ---- 物品說明的框：底與邊疊成一張，網頁用九宮格拉開（上下 12、左右 10）
    for out, base in (('tip-rare.png', 'rare_'), ('tip-plain.png', '')):
        frame = tex('gui/sprites/tooltip/' + base + 'background.png')
        frame.alpha_composite(tex('gui/sprites/tooltip/' + base + 'frame.png'))
        save(frame, out)

    # ---- 說明框裡的小圖
    shapes = tex('font/tooltip/emblem/frame.png')            # 6 欄（形狀）× 6 列（等級），一格 64
    kinds = tex('font/tooltip/emblem/sprite.png')            # 一格 32；第一列第五個是弓
    emblem = shapes.crop((0, 2 * 64, 64, 3 * 64))            # 菱形、第三列：Bony Bow 的 emblem 是 diamond_2
    emblem.alpha_composite(kinds.crop((4 * 32, 0, 5 * 32, 32)), (16, 16))
    save(emblem.crop(emblem.getbbox()), 'emblem-bow.png')
    attr = tex('font/tooltip/attribute/sprite.png')          # 一格 16：地、雷、水、火、風、無屬性、…、攻速
    for name, index in (('ico-air.png', 4), ('ico-neutral.png', 5), ('ico-speed.png', 7)):
        save(attr.crop((index * 16, 0, index * 16 + 16, 16)), name)
    save(tex('font/tooltip/requirement/sprite.png'), 'req.png')            # 力量…敏捷，上列亮、下列暗，一格 24
    save(tex('font/tooltip/requirement/linear.png'), 'mark.png')           # 空格、勾、叉，一格 10
    save(tex('font/tooltip/divider/line.png'), 'divider.png')
    save(tex('font/tooltip/socket.png').crop((0, 0, 10, 10)), 'socket.png')

    # ---- 對話框：頭像框（左）、對話框、名牌、底下那一條，照實機的相對位置疊成一張
    #      （實機截圖量的：框壓在對話框左邊 33 格上、名牌在框的右上、底條在對話框下面往右 16 格）
    frame = tex('font/hud/dialogue/style/default/frame.png')               # 64×80
    box = tex('font/hud/dialogue/style/default/box.png')                   # 256×80
    plate = tex('font/hud/dialogue/style/default/nameplate.png').crop((0, 0, 112, 20))
    footer = tex('font/hud/dialogue/style/default/footer.png')             # 224×18
    face = tex('font/hud/dialogue/portrait/wynn/main/aledar.png').crop((0, 0, 48, 64))     # 一格 48×64，第一格是平常的表情
    dialogue = Image.new('RGBA', (31 + 256, 106), (0, 0, 0, 0))
    dialogue.alpha_composite(footer, (47, 87))
    dialogue.alpha_composite(box, (31, 9))
    dialogue.alpha_composite(frame, (0, 0))
    dialogue.alpha_composite(face, (8, 8))                                # 框裡那一塊正好是 48×64
    dialogue.alpha_composite(plate, (64, 0))
    save(dialogue, 'dialogue.png')
    choice = tex('font/hud/dialogue/style/default/choice.png')             # 左半是平常的、右半是選到的
    both = Image.new('RGBA', (144, 32), (0, 0, 0, 0))
    both.alpha_composite(choice.crop((0, 0, 144, 16)), (0, 0))
    both.alpha_composite(choice.crop((144, 0, 288, 16)), (0, 16))
    save(both, 'choice.png')
    save(tex('font/hud/dialogue/style/default/cursor.png').crop((0, 0, 16, 16)), 'cursor.png')

    # ---- 底下那一排：快捷列、兩邊的旗子與上面的橫桿、生命與魔力、等級
    hot = tex('gui/sprites/hud/hotbar.png')                                # 182×22
    banner = tex('font/hud/gameplay/default/bottom_middle/hotbar.png')     # 208×64；橫桿在第 31–38 列
    health = tex('font/hud/gameplay/default/bottom_middle/health_background.png').crop((0, 0, 72, 20))
    mana = tex('font/hud/gameplay/default/bottom_middle/mana_background.png')
    health_bar = tex('font/hud/gameplay/default/bottom_middle/health_bar.png')
    mana_bar = tex('font/hud/gameplay/default/bottom_middle/mana_bar.png')
    xp = tex('font/hud/gameplay/default/bottom_middle/experience_bar.png')  # 一列 3 格高，越下面越滿
    level = tex('font/hud/gameplay/default/bottom_middle/hex_meter.png').crop((0, 0, 24, 27))
    mark = tex('gui/sprites/hud/hotbar_selection.png')
    W, top, below = 232, 30, 3
    hud = Image.new('RGBA', (W, top + 22 + below), (0, 0, 0, 0))
    hx, hy = (W - 182) // 2, top

    def fill(bar, x0, x1, y):
        """血條那一格：拿貼圖第一段中間那一直行，橫向鋪滿。"""
        column = bar.crop((4, 0, 5, 6))
        for x in range(x0, x1):
            hud.alpha_composite(column, (x, y))

    hud.alpha_composite(banner, ((W - 208) // 2, hy - 38))                 # 橫桿正好壓在快捷列上面
    state = 34                                                             # 經驗值：五十三段裡的第幾段
    hud.alpha_composite(xp.crop((0, state * 3, 179, state * 3 + 3)), ((W - 179) // 2, hy - 38 + 33))
    hud.alpha_composite(hot, (hx, hy))
    hud.alpha_composite(health, (hx - 10, hy - 25))
    fill(health_bar, hx - 10 + 11, hx - 10 + 66, hy - 25 + 11)
    hud.alpha_composite(mana, (hx + 119, hy - 25))
    fill(mana_bar, hx + 119 + 5, hx + 119 + 60, hy - 25 + 11)
    hud.alpha_composite(level, (hx + 91 - 12, hy - 27))
    hud.alpha_composite(mark, (hx - 1, hy - 1))
    save(hud, 'hud.png')

    # ---- 背包（遊戲裡那一張木頭的）。貼圖靠左上角的記號像素定位，圖本身在 (32,36) 起
    inventory = tex('gui/container/inventory.png').crop((32, 36, 219, 211))
    save(inventory, 'inventory.png')

    # ---- 放在格子裡的幾樣東西
    for name, source in (('item-book.png', 'wynn/gui/player/content_book.png'),
                         ('item-heal.png', 'wynn/potion/healing_full.png'),
                         ('item-mana.png', 'wynn/potion/mana_full.png'),
                         ('item-emerald.png', 'wynn/pouch/emerald_full.png')):
        save(tex(source), name)

    # ---- 功能頁每一列前面的小圖：遊戲通用的白色圖示（一格 16），網頁用遮罩上色
    save(tex('font/common.png'), 'glyphs.png')


def from_cdn():
    """官方的物品圖示：大的那幾張（一千多格）縮成 64 格，小的原樣。"""
    import urllib.request
    from PIL import Image
    for name, remote in ICONS.items():
        request = urllib.request.Request(CDN + remote + '.webp', headers={'user-agent': 'WynnChaYuan-site (github.com/LyuChaCha/WynnChaYuan)'})
        with urllib.request.urlopen(request, timeout=30) as answer:
            img = Image.open(io.BytesIO(answer.read())).convert('RGBA')
        if img.width > 64:
            img = img.crop(img.getbbox())
            side = max(img.size)
            square = Image.new('RGBA', (side, side), (0, 0, 0, 0))
            square.alpha_composite(img, ((side - img.width) // 2, (side - img.height) // 2))
            img = square.resize((64, 64), Image.LANCZOS)
        save(img, 'item-' + name + '.png')


def from_shots(folder):
    """背景：world.png、street.png 兩張實機截圖，裁成 960×480 的比例、縮到 960 寬。"""
    from PIL import Image
    for name in ('world', 'street'):
        img = Image.open(os.path.join(folder, name + '.png')).convert('RGB')
        want = img.width * 480 // 960
        top = max(0, (img.height - want) * 2 // 5)
        img = img.crop((0, top, img.width, top + want)).resize((960, 480), Image.LANCZOS)
        os.makedirs(DIR, exist_ok=True)
        img.save(os.path.join(DIR, 'bg-' + name + '.webp'), quality=72, method=6)
        print('  bg-%s.webp' % name)


def from_mojang(player):
    """背包裡那個人的皮膚：名字 → UUID → 材質的網址，都是 Mojang 公開的查詢。"""
    import urllib.request
    from PIL import Image

    def get(url):
        with urllib.request.urlopen(url, timeout=30) as answer:
            return answer.read()
    uuid = json.loads(get('https://api.mojang.com/users/profiles/minecraft/' + player))['id']
    profile = json.loads(get('https://sessionserver.mojang.com/session/minecraft/profile/' + uuid))
    textures = json.loads(base64.b64decode(profile['properties'][0]['value']))['textures']
    skin = textures['SKIN']
    if (skin.get('metadata') or {}).get('model') == 'slim':
        print('  注意：這個皮膚是細手臂的體型，網頁上那個人是照一般體型貼的')
    img = Image.open(io.BytesIO(get(skin['url'].replace('http://', 'https://')))).convert('RGBA')
    if img.size != (64, 64):
        sys.exit('皮膚不是 64×64（%d×%d）：網頁上那個人只會貼這一種' % img.size)
    save(img, 'skin.png')


# ------------------------------------------------------------------ 打包
def bundle():
    files = {}
    for path in sorted(glob.glob(os.path.join(DIR, '*'))):
        name = os.path.basename(path)
        kind = MIME.get(os.path.splitext(name)[1])
        if not kind:
            continue
        with open(path, 'rb') as handle:
            data = handle.read()
        files[name] = {'type': kind, 'v': hashlib.sha256(data).hexdigest()[:10],
                       'data': base64.b64encode(data).decode('ascii')}
    body = json.dumps(files, ensure_ascii=False, indent=1)
    return ('/**\n * 預覽畫面用的材質（Wynncraft 的介面、物品圖示與實機截圖，© Wynncraft）。\n'
            ' * 這個檔是 build-textures.py 從 textures/ 打包的，不要手改。\n */\n'
            'export const TEXTURES = ' + body + ';\n')


def main():
    args = sys.argv[1:]
    if '--check' in args:
        try:
            with io.open(TARGET, encoding='utf-8', newline='') as handle:
                have = handle.read().replace('\r\n', '\n')
        except IOError:
            have = ''
        if have != bundle():
            sys.exit('demo-textures.js 跟 textures/ 不一樣了：重跑 python tools/presence/build-textures.py')
        print('demo-textures.js 是最新的。')
        return
    if '--pack' in args:
        at = args.index('--pack')
        path = args[at + 1] if at + 1 < len(args) and not args[at + 1].startswith('--') else find_pack()
        if not path:
            sys.exit('找不到資源包：用 --pack <檔案> 指定')
        from_pack(path)
    if '--icons' in args:
        from_cdn()
    if '--shots' in args:
        from_shots(args[args.index('--shots') + 1])
    if '--skin' in args:
        from_mojang(args[args.index('--skin') + 1])
    out = bundle()
    with io.open(TARGET, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write(out)
    print('寫好了 demo-textures.js（%d KB）' % (len(out) // 1024))


if __name__ == '__main__':
    main()
