#!/usr/bin/env python3
"""簡體中文裡的繁中直譯痕跡。

為什麼需要
----------
簡體不是繁體的字元轉換。字轉過來了、用詞沒轉，讀起來就是「繁體味」——
而那種句子<b>完全合法</b>：JSON 沒錯、佔位符沒錯、字也都是簡體，沒有任何
既有的檢查會抱怨。只有真的講簡體的人才看得出來，而那時候已經上線了。

所以這裡列一張表：左邊是台灣的說法，右邊是大陸的說法。命中就報。

判準是「兩邊<b>真的</b>不一樣」，不是「我覺得這樣比較好」——
`品質`／`质量` 這種兩邊都在用的不列，列了只會變成雜訊，然後被關掉。

用法：
    python tools/check-zh-cn.py
    python tools/check-zh-cn.py --lang zh_cn
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"

# 左邊是「字轉了、詞沒轉」之後會長出來的樣子，右邊是大陸真正的說法。
#
# 只收兩邊<b>真的</b>不一樣的。`品质`／`质量` 那種兩邊都在用的不列——
# 列了只會變成雜訊，然後整個檢查被關掉。
CONVERTED = {
    "资讯": "信息",
    "讯息": "消息",
    "介面": "界面",
    "档案": "文件",
    "程式": "程序",
    "软体": "软件",
    "硬体": "硬件",
    "网路": "网络",
    "滑鼠": "鼠标",
    "萤幕": "屏幕",
    "视窗": "窗口",
    "预设": "默认",
    "储存": "保存",
    "搜寻": "搜索",
    "设定": "设置",
    "登入": "登录",
    "连线": "连接",
    "剪贴簿": "剪贴板",
    "解析度": "分辨率",
    "记忆体": "内存",
    "选单": "菜单",
    "栏位": "槽位",
    "伫列": "队列",
    "字型": "字体",
    "资料夹": "文件夹",
    "回复": "恢复",
    "透过": "通过",
    "藉由": "通过",
    "品项": "项目",
    "并用": "同时使用",
    "不死族": "亡灵",
    "腐败": "腐化",
    "太空人": "宇航员",
    "爵士鼓": "架子鼓",
    "驾驶员": "飞行员",
    "地城": "地牢",
    "讨伐战": "副本",
    "魔力": "法力",
    "窃取": "偷取",
    "载入": "加载",
    "帐号": "账号",
    "拖曳": "拖动",
    "卷动": "滚动",
    "重新整理": "刷新",
    "影片": "视频",
    "特典": "特权",
    "网志": "博客",
    "行动装置": "移动设备",
    "光碟": "光盘",
    "列印": "打印",
    "预设值": "默认值",
    "伺服器": "服务器",
}

# 這幾個詞在某些脈絡是對的，命中也不報。
ALLOW = {
    # 「资料」在大陸也講「资料」（例如「参考资料」），只有「資料＝data」
    # 那個意思要換成「数据」。分不出來，乾脆不管。
    "资料",
}


def rows(path: Path):
    """一個檔案裡的 (鍵, 譯文)。兩種格式都認。"""
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return
    entries = data.get("entries")
    if isinstance(entries, dict):
        for key, value in entries.items():
            if isinstance(value, dict) and isinstance(value.get("dst"), str):
                yield key, value["dst"]
        return
    for key, value in data.items():
        if not key.startswith("_") and isinstance(value, str):
            yield key, value


def main(argv: list[str]) -> int:
    lang = "zh_cn"
    if "--lang" in argv:
        lang = argv[argv.index("--lang") + 1]
    if lang == "zh_tw":
        # 這張表是<b>單向</b>的：左邊的說法在簡中要換掉，在繁中卻正是對的。
        # 拿它去掃繁中會得到三百多個「建議」，照著改等於把繁中改成大陸用語。
        print("這張表只適用簡體中文。繁中的用詞請看 GLOSSARY.md。")
        return 2
    base = TRANSLATIONS / lang
    if not base.is_dir():
        print(f"沒有 {base}")
        return 2

    hits = 0
    checked = 0
    for path in sorted(base.rglob("*.json")):
        if path.name.startswith("_"):
            continue
        for key, dst in rows(path):
            if not dst.strip():
                continue
            checked += 1
            for bad, good in CONVERTED.items():
                if bad in ALLOW or bad not in dst:
                    continue
                rel = path.relative_to(base).as_posix()
                short = key if len(key) <= 42 else key[:42] + "…"
                print(f"  [{rel}] 「{bad}」建議用「{good}」")
                print(f"      {short}  ->  {dst[:60]}")
                hits += 1

    print(f"\n{lang}：檢查 {checked} 條，{hits} 處繁中直譯的痕跡")
    return 1 if hits else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
