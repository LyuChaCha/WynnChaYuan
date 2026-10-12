# -*- coding: utf-8 -*-
"""網站的圖示：從 logo/2026 的原檔縮出來，寫成 icons.js（base64，Worker 直接送）。

    python tools/presence/make-icons.py

- 分頁上的小圖示用透明底的那一張，把四周的留白裁掉——32 像素的格子裡葉子才夠大；
  亮色、暗色的分頁列都看得清楚。
- 加到手機主畫面的那一張（apple-touch-icon）不能透明，用深色底的那一張。

換了 logo 就重跑一次，再部署。
"""
import base64
import io
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
LOGOS = os.path.join(HERE, '..', '..', 'logo', '2026')


def png(image):
    out = io.BytesIO()
    image.save(out, 'PNG', optimize=True)
    return base64.b64encode(out.getvalue()).decode('ascii')


def tight(image, margin):
    """裁掉透明的留白，再補一圈 margin（比例），維持正方形。"""
    box = image.getchannel('A').getbbox()
    cut = image.crop(box)
    side = round(max(cut.size) * (1 + 2 * margin))
    canvas = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    canvas.paste(cut, ((side - cut.width) // 2, (side - cut.height) // 2), cut)
    return canvas


leaf = tight(Image.open(os.path.join(LOGOS, 'wynnchayuan-logo-transparent-1024.png')).convert('RGBA'), 0.04)
dark = Image.open(os.path.join(LOGOS, 'wynnchayuan-logo-dark-1024.png')).convert('RGBA')
# 深色底那一張本身是圓角方塊，四個角是透明的：手機主畫面的圖示要整張不透明，先墊同一個底色
ground = Image.new('RGBA', dark.size, dark.getpixel((dark.width // 2, 8)))
ground.alpha_composite(dark)

icons = {
    'FAVICON': png(leaf.resize((48, 48), Image.LANCZOS)),
    'ICON_192': png(leaf.resize((192, 192), Image.LANCZOS)),
    'APPLE_TOUCH': png(ground.convert('RGB').resize((180, 180), Image.LANCZOS)),
}
text = ('/**\n * 網站的圖示（PNG，base64）。這個檔是 make-icons.py 產生的，不要手改。\n'
        ' *\n * FAVICON：分頁上的小圖示（48×48，透明底）。\n'
        ' * ICON_192：分享連結、書籤用的大一點的那一張（192×192，透明底）。\n'
        ' * APPLE_TOUCH：加到手機主畫面的那一張（180×180，深色底）。\n */\n')
for name, data in icons.items():
    text += "export const %s = '%s';\n" % (name, data)
with open(os.path.join(HERE, 'icons.js'), 'w', encoding='utf-8', newline='\n') as f:
    f.write(text)
print('寫好了 icons.js：' + '、'.join('%s %.1f KB' % (k, len(v) * 3 / 4 / 1024) for k, v in icons.items()))
