#!/usr/bin/env python3
"""裁出設定畫面用的 Noto Sans，並產生對應的字型定義。

為什麼要裁
----------
設定畫面用平滑字型（Noto Sans）而不是原版的點陣字。完整的中日韓字型一顆十幾 MB，
四種語言就是四十幾 MB；而設定畫面會出現的字就那幾千個。所以只留用得到的字：

* 介面語言檔（lang/*.json）、貢獻者名單、更新說明裡出現的字——這些是一定會畫出來的；
* 程式裡寫死的中文（狀態列的訊息）；
* 各語言語料裡最常用的一千多個字——預覽裡的譯文、之後新寫的更新說明多半落在裡面。

沒收進來的字會退回原版的點陣字（字型定義的最後一項是 minecraft:default），不會變方框，
只是那幾個字長得不一樣。**加了介面字串之後要重跑這支**，SettingsFontTest 會擋沒收到的字。

產生什麼
--------
assets/wynnchayuan/font/ui/
    latin.ttf tc.ttf sc.ttf jp.ttf kr.ttf   各語言自己的字
    tc_x.ttf sc_x.ttf jp_x.ttf kr_x.ttf     別的語言會借用的那幾個字（語言名稱、貢獻者暱稱）
    sym.ttf                                 打勾、打叉、小三角
    <語言>_<倍率>.json                       字型定義；倍率是「一個字點畫成螢幕上幾個點」
    OFL.txt                                 授權

倍率為什麼要分開：TTF 字是先畫成點陣再貼上去的，畫的解析度（oversample）跟實際貼出來的
大小一比一時邊緣才是乾淨的。介面縮放 2、3、4 各用各的定義，放大的標題再往上挑。

用法
----
    pip install fonttools
    python tools/build-ui-fonts.py <放原始字型的資料夾>

原始字型是 Google Fonts 的可變字型（SIL OFL 1.1）：
    NotoSans[wdth,wght].ttf  NotoSansTC[wght].ttf（或 Windows 內附的 NotoSansTC-VF.ttf）
    NotoSansSC[wght].ttf  NotoSansJP[wght].ttf  NotoSansKR[wght].ttf
    NotoSansSymbols2-Regular.ttf
來源：https://github.com/google/fonts/tree/main/ofl
"""
from __future__ import annotations

import collections
import glob
import io
import json
import os
import shutil
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "wynnchayuan")
OUT = os.path.join(ASSETS, "font", "ui")

# 字重：比 Regular 重一點。深色半透明的底上，細的筆畫會糊掉。
WEIGHT = 500
# 字級（GUI 像素）。9 的時候中文字跟原版的一格差不多大，英數的大寫高度約 6.5。
SIZE = 9.0
# 一個字點畫成螢幕上幾個點。見檔頭「倍率為什麼要分開」。
DOTS = [2, 3, 4, 6]

# 每種介面語言用哪一顆字。沒列到的語言（英文、俄文、西班牙文…）用 latin。
FAMILY = {"zh_tw": "tc", "zh_cn": "sc", "ja_jp": "jp", "ko_kr": "kr"}
SOURCES = {
    "latin": ["NotoSans[wdth,wght].ttf"],
    "tc": ["NotoSansTC[wght].ttf", "NotoSansTC-VF.ttf"],
    "sc": ["NotoSansSC[wght].ttf"],
    "jp": ["NotoSansJP[wght].ttf"],
    "kr": ["NotoSansKR[wght].ttf"],
    # 打勾、打叉、小三角：上面幾顆都沒有，狀態列的訊息卻天天在用
    "sym": ["NotoSansSymbols2-Regular.ttf"],
}
SYMBOLS = "✔✘✓✗▸▾▴◂"
# 語料裡最常用的幾個字要收。
TOP = {"tc": 1800, "sc": 1800, "jp": 1500, "kr": 1200}
CORPUS = {"tc": "zh_tw", "sc": "zh_cn", "jp": "ja_jp", "kr": "ko_kr"}

NATIVE = ["繁體中文", "简体中文", "日本語", "한국어", "Русский", "Español", "English"]


def ranges(*pairs):
    out = set()
    for lo, hi in pairs:
        out.update(range(lo, hi + 1))
    return out


# 每一顆都收的：英數、西歐、西里爾、常用標點與幾個符號
BASE = ranges((0x20, 0x7E), (0xA0, 0x17F), (0x400, 0x45F), (0x490, 0x491),
              (0x2010, 0x2027), (0x2030, 0x203A), (0x2190, 0x2193))
BASE |= {ord(c) for c in "✔✘●○★☆·•…—–→←↑↓×÷±≈≤≥©®™€£¥"}
# 中日韓的標點、全形英數、假名
CJK_BASE = ranges((0x3000, 0x303F), (0x3040, 0x30FF), (0xFF00, 0xFFEF), (0xFE30, 0xFE4F))


def strings(node, out):
    if isinstance(node, str):
        out.append(node)
    elif isinstance(node, dict):
        for v in node.values():
            strings(v, out)
    elif isinstance(node, list):
        for v in node:
            strings(v, out)


def chars_of(path):
    with io.open(path, encoding="utf-8") as f:
        text = f.read()
    return {ord(c) for c in text if ord(c) > 0x7F}


def json_chars(path):
    out = []
    with io.open(path, encoding="utf-8") as f:
        strings(json.load(f), out)
    return {ord(c) for s in out for c in s}


def corpus_top(lang, n):
    """這個語言的譯文裡最常出現的 n 個字（只算非 ASCII）。"""
    count = collections.Counter()
    base = os.path.join(ASSETS, "translations", lang)
    for path in glob.glob(os.path.join(base, "**", "*.json"), recursive=True):
        if os.path.basename(path) == "quest.json":
            continue                           # 合併檔，內容跟 quest/ 底下重複
        try:
            with io.open(path, encoding="utf-8") as f:
                data = json.load(f)
        except (OSError, ValueError):
            continue
        out = []
        collect_dst(data, out)
        for s in out:
            count.update(c for c in s if ord(c) > 0x2E7F)
    return {ord(c) for c, _ in count.most_common(n)}


def collect_dst(node, out):
    """扁平檔的值、或條目裡的 dst。鍵（原文）不算。"""
    if isinstance(node, dict):
        if isinstance(node.get("dst"), str):
            out.append(node["dst"])
        for key, value in node.items():
            if key in ("src", "_meta", "similar", "candidates"):
                continue
            if isinstance(value, str) and key != "dst":
                out.append(value)
            else:
                collect_dst(value, out)
    elif isinstance(node, list):
        for value in node:
            collect_dst(value, out)


# 更新說明在倉庫根目錄；打包的時候才抄進 assets，所以原始碼樹裡 assets 底下沒有這個檔。
# （第一版照 assets 找，找不到就安靜跳過——更新說明裡的字整批沒收進去。）
VERSION = os.path.join(ROOT, "version.json")


def default_notes_chars():
    """
    每一版「預設那一份」說明裡的非英文字。

    預設那一份照理是英文，但 0.1.0 整段是中文，後面幾版也夾著「词缀」「意象」這種引用。
    它是用英文那一組字型畫的，所以這些字要放進大家互相借用的那一包。
    """
    with io.open(VERSION, encoding="utf-8") as f:
        notes = json.load(f).get("notes", {})
    out = set()
    for one in notes.values():
        text = [one.get("headline", "")] + list(one.get("items", []))
        out |= {ord(c) for line in text for c in line if ord(c) > 0x7F}
    return out


def shared_chars():
    """不分語言都可能畫出來的：貢獻者名單、更新說明、程式裡寫死的字、語言名稱。"""
    out = set()
    out |= json_chars(os.path.join(ASSETS, "credits.json"))
    out |= json_chars(VERSION)
    for path in glob.glob(os.path.join(ROOT, "src", "main", "java", "**", "*.java"),
                          recursive=True):
        out |= chars_of(path)
    out |= {ord(c) for name in NATIVE for c in name}
    return out


def wanted(family):
    if family == "sym":
        return {ord(c) for c in SYMBOLS}
    out = set(BASE)
    lang_dir = os.path.join(ASSETS, "lang")
    if family == "latin":
        for path in glob.glob(os.path.join(lang_dir, "*.json")):
            lang = os.path.basename(path)[:-5]
            if lang not in FAMILY:
                out |= json_chars(path)
        # 拉丁語系的介面也會畫到更新說明與貢獻者名單裡的拉丁、西里爾字
        out |= {cp for cp in shared_chars() if cp < 0x2E80}
        return out
    out |= CJK_BASE
    lang = CORPUS[family]
    out |= json_chars(os.path.join(lang_dir, lang + ".json"))
    out |= json_chars(os.path.join(lang_dir, "en_us.json"))
    out |= shared_chars()
    out |= corpus_top(lang, TOP[family])
    return out


def extras():
    """別的語言會借用的字：語言名稱與貢獻者暱稱。"""
    out = {ord(c) for name in NATIVE for c in name}
    out |= json_chars(os.path.join(ASSETS, "credits.json"))
    # 拉丁那一顆沒有的幾個符號，跟中日韓的借
    out |= {ord(c) for c in "●○→←↑↓　·…★☆"}
    out |= default_notes_chars()
    return {cp for cp in out if cp > 0x7F}


def orphans(source_dir):
    """
    某一顆該畫、它的來源字型卻沒有的字：繁中的說明裡引了簡體字（「词缀」）這一類。

    這種字只有別顆有，所以也要進互相借用的那一包，不然會退回原版的點陣字，
    一行裡混著兩種字。
    """
    out = set()
    for family in CORPUS:
        have = set(TTFont(find(source_dir, SOURCES[family])).getBestCmap())
        out |= {cp for cp in wanted(family) if cp > 0x7F and cp not in have}
    return out


def find(source_dir, names):
    for name in names:
        path = os.path.join(source_dir, name)
        if os.path.exists(path):
            return path
    raise SystemExit("找不到 " + " 或 ".join(names) + "（在 " + source_dir + "）")


def cut(source, codepoints, out_path):
    """裁字、定字重、存檔。回傳實際收進去的碼位。"""
    options = subset.Options()
    options.layout_features = []               # 遊戲不做排版替換，只查「字 → 字形」
    options.hinting = False
    options.notdef_outline = True
    options.name_IDs = [0, 1, 2, 3, 4, 5, 6, 13, 14]   # 版權與授權那幾條要留著
    options.name_languages = [0x409]
    options.drop_tables += ["DSIG", "vhea", "vmtx", "VORG", "BASE", "STAT", "MVAR"]
    font = TTFont(source)
    have = set(font.getBestCmap())
    keep = sorted(cp for cp in codepoints if cp in have)
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=keep)
    subsetter.subset(font)
    if "fvar" in font:
        axes = {a.axisTag: (WEIGHT if a.axisTag == "wght" else a.defaultValue)
                for a in font["fvar"].axes}
        font = instancer.instantiateVariableFont(font, axes)
    font.save(out_path)
    return set(keep)


def definition(lang, dots):
    own = FAMILY.get(lang, "latin")

    def ttf(name):
        return {"type": "ttf", "file": "wynnchayuan:ui/" + name + ".ttf", "size": SIZE,
                "oversample": float(dots), "shift": [0.0, 0.0]}

    providers = [ttf(own), ttf("sym")]
    for other in ("tc", "sc", "jp", "kr"):
        if other != own:
            providers.append(ttf(other + "_x"))
    # 都沒有的字退回原版字型，不會變方框
    providers.append({"type": "reference", "id": "minecraft:default"})
    return {"providers": providers}


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    source_dir = sys.argv[1]
    os.makedirs(OUT, exist_ok=True)
    for old in glob.glob(os.path.join(OUT, "*")):
        os.remove(old)

    extra = extras() | orphans(source_dir)
    total = 0
    for family, names in SOURCES.items():
        source = find(source_dir, names)
        path = os.path.join(OUT, family + ".ttf")
        kept = cut(source, wanted(family), path)
        size = os.path.getsize(path)
        total += size
        print(f"{family}.ttf  {len(kept):5d} 字  {size / 1024:7.0f} KB")
        if family not in ("latin", "sym"):
            path = os.path.join(OUT, family + "_x.ttf")
            kept = cut(source, extra, path)
            size = os.path.getsize(path)
            total += size
            print(f"{family}_x.ttf {len(kept):4d} 字  {size / 1024:7.0f} KB")

    for lang in list(FAMILY) + ["latin"]:
        for dots in DOTS:
            path = os.path.join(OUT, f"{lang}_{dots}.json")
            with io.open(path, "w", encoding="utf-8", newline="\n") as f:
                json.dump(definition(lang, dots), f, indent=2)
                f.write("\n")
    licence = os.path.join(source_dir, "OFL.txt")
    if os.path.exists(licence):
        shutil.copy(licence, os.path.join(OUT, "OFL.txt"))
    else:
        print("注意：來源資料夾沒有 OFL.txt，授權檔沒有更新")
    print(f"合計 {total / 1024 / 1024:.2f} MB，字型定義 {(len(FAMILY) + 1) * len(DOTS)} 份")


if __name__ == "__main__":
    main()
