#!/usr/bin/env python3
"""/class 原型名與簡介的 bitmap 字型。

為什麼不能像對話框那樣用 TTF 加 shift
----------------------------------------
Wynncraft 資源包的 ``shaders/include/text.glsl`` 靠**字型圖集 (0,0) 那個像素**決定
要不要把文字搬到畫面的某一角（``screenAnchor``）：原型名／簡介那幾份字型裡的
U+0001 是一個 256 px 高的標記字，排在文字最前面、第一個被烘進圖集，整張圖集都
帶著標記色，shader 就把從那張圖集畫出來的每個字搬到 CENTER_LEFT。

Minecraft 的 ``FontTexture`` 是按 ``isColored`` 分圖集的：bitmap（RGBA）是 colored，
TTF（灰階）不是，兩者永遠不會進同一張。所以 TTF 的中文就算字型裡有標記、標記也
先畫，中文那張圖集的 (0,0) 仍然不是標記——譯文全落在 action bar 自己的高度
（2026-10-05 實機）。

做法：把這幾條譯文用到的字光柵化成 PNG 字表，做成 bitmap provider，``ascent`` 直接
照 Wynncraft 同一格的值（name 0/1/2 = -26/-58/-90、desc = -35/-67/-99），跟標記字
同一張圖集。TTF 留在最後當備援（字表沒收到的字至少看得見，只是位置會掉）。

用法::

    python tools/selector-font.py            # 全部語言
    python tools/selector-font.py zh_tw      # 只做一種

字表放 ``assets/wynnchayuan/textures/font/selector/<lang>.png``；語料改了要重跑。
"""
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/wynnchayuan"
FONTS = ASSETS / "font/actionbar"
SHEETS = ASSETS / "textures/font/selector"
CORPUS = ASSETS / "translations"

MARKER = "\x01"

# Wynncraft 同一格的 ascent（從資源包的字型定義量出來的）
SLOTS = {
    # 名字往上 2 px、簡介往下 1 px：Wynncraft 兩格只差 9 px，英文大寫 7 px 高
    # 放得下，中文 10 px 高會貼在一起（使用者 2026-10-05：「希望字體再分開一點」）。
    "selector_name_0": -24,
    "selector_name_1": -56,
    "selector_name_2": -88,
    "selector_desc_0": -36,
    "selector_desc_1": -68,
    "selector_desc_2": -100,
}

ARCHETYPES = [
    "Fallen", "Battle Monk", "Paladin",
    "Trapper", "Boltslinger", "Sharpshooter",
    "Light Bender", "Riftwalker", "Arcanist",
    "Shadestepper", "Trickster", "Acrobat",
    "Summoner", "Ritualist", "Acolyte",
]

COLS = 16
CELL_H = 11      # Fusion Pixel 10px：9 px 在基線上、1-2 px 在基線下
BASELINE = 9


def chars_for(lang):
    """那 29 條譯文用到的字。

    原型名不一定在 misc.json：Sharpshooter 與 Acrobat 的譯名早就在
    gear-weapon.json（巢狀格式，src/dst 一對一個物件）。所以整個語言資料夾都掃，
    扁平的看鍵、巢狀的看 src。
    """
    out = set()

    def take(key, dst):
        if isinstance(dst, str) and (key.startswith(MARKER) or key in ARCHETYPES):
            out.update(c for c in dst if ord(c) > 0x7F and c != MARKER)

    def walk(node):
        if isinstance(node, dict):
            if isinstance(node.get("src"), str):
                take(node["src"], node.get("dst"))
            for key, value in node.items():
                if isinstance(value, str):
                    take(key, value)
                else:
                    walk(value)
        elif isinstance(node, list):
            for item in node:
                walk(item)

    # 只看這兩個檔：敘述在 misc.json，原型名在 scoped/archetype.json。
    # 整個資料夾掃的話會把 gear-weapon.json 那把叫 Sharpshooter 的弓也收進來。
    for rel in ("misc.json", "scoped/archetype.json"):
        path = CORPUS / lang / rel
        if not path.is_file():
            continue
        try:
            walk(json.loads(path.read_text(encoding="utf-8")))
        except ValueError:
            continue
    return sorted(out)


def ttf_for(lang):
    spec = json.loads((FONTS / lang / "selector_name_0.json").read_text(encoding="utf-8"))
    for p in spec["providers"]:
        if p.get("type") == "ttf":
            ns, path = p["file"].split(":", 1)
            # ttf 的 file 是相對 assets/<ns>/font/ 的
            return ROOT / "src/main/resources/assets" / ns / "font" / path, p
    raise SystemExit(f"{lang}: selector_name_0.json 裡沒有 ttf provider")


def render(lang, chars, ttf):
    font = ImageFont.truetype(str(ttf), 10)
    glyphs = []
    width = 1
    for ch in chars:
        scratch = Image.new("L", (24, 24), 0)
        ImageDraw.Draw(scratch).text((0, BASELINE), ch, font=font, fill=255, anchor="ls")
        px = scratch.load()
        cols = [x for x in range(24) for y in range(CELL_H) if px[x, y] > 127]
        w = (max(cols) + 1) if cols else 1
        width = max(width, w)
        glyphs.append(scratch.crop((0, 0, 24, CELL_H)))
    rows = (len(chars) + COLS - 1) // COLS
    sheet = Image.new("RGBA", (COLS * width, rows * CELL_H), (0, 0, 0, 0))
    for i, g in enumerate(glyphs):
        cell = Image.new("RGBA", (width, CELL_H), (0, 0, 0, 0))
        src = g.load()
        dst = cell.load()
        for y in range(CELL_H):
            for x in range(width):
                if src[x, y] > 127:
                    dst[x, y] = (255, 255, 255, 255)
        sheet.paste(cell, ((i % COLS) * width, (i // COLS) * CELL_H))
    SHEETS.mkdir(parents=True, exist_ok=True)
    sheet.save(SHEETS / f"{lang}.png")
    table = []
    for r in range(rows):
        row = "".join(chars[r * COLS:(r + 1) * COLS])
        table.append(row + "\x00" * (COLS - len(row)))
    return table, width


def write_fonts(lang, table, ttf_provider):
    for name, ascent in SLOTS.items():
        path = FONTS / lang / f"{name}.json"
        spec = json.loads(path.read_text(encoding="utf-8"))
        reference = [p for p in spec["providers"] if p.get("type") == "reference"]
        providers = reference + [{
            "type": "bitmap",
            "file": f"wynnchayuan:font/selector/{lang}.png",
            "height": CELL_H,
            "ascent": ascent,
            "chars": table,
        }, ttf_provider]
        path.write_text(json.dumps({"providers": providers}, ensure_ascii=False, indent=2)
                        + "\n", encoding="utf-8")


def main():
    langs = sys.argv[1:] or sorted(p.name for p in FONTS.iterdir() if p.is_dir())
    for lang in langs:
        chars = chars_for(lang)
        if not chars:
            print(f"{lang}: 沒有 ASCII 以外的字，跳過")
            continue
        ttf, provider = ttf_for(lang)
        table, width = render(lang, chars, ttf)
        write_fonts(lang, table, provider)
        print(f"{lang}: {len(chars)} 個字，格寬 {width} px，{len(table)} 列 -> {SHEETS / (lang + '.png')}")


if __name__ == "__main__":
    main()
