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
    # Profession：採礦、釣魚、鍛造那一整套。大陸的網遊一律叫「生活技能」，
    # 「专业」是照字面搬的——讀起來像大學科系，而且跟 Class（职业）擺在
    # 同一個畫面上時，兩個詞看起來像同一件事。
    "专业": "生活技能",
}

# 只在繁體裡才有的字。出現在簡中語料裡就是<b>連字都沒轉</b>。
#
# 為什麼要有這一條：上面那張詞表比對的是「詞」，前提是字已經轉成簡體了。
# 但骨架是從繁體複製過來的，說明欄整段是繁體原文——詞表一條都比不到，
# 193 個檔就這樣過關。這一條擋的是更前面的那一層。
#
# 只收<b>簡繁真的不同形</b>的常用字。兩邊同形的（的、和、在）當然不收；
# 簡體裡也用得到的異體字也不收，不然會冒出一堆假警報。
TRAD_ONLY = (
    # 高頻虛詞與常用字
    "個們這來對開關沒還當為與並將"
    "會時間長點種類樣結給級組經統體"
    "國學問題選單導專業務員動參區"
    "發現實際進數無東圖團園圍場塊"
    "說話語譯讀誰講詞論訊調變讓認識議請謝證護課試"
    "標籤產備復錯銀錢鐵鋼鎖鐘陣隨險雙難電靈"
    "頁項順須預領頭顏願風飛飯養馬駕驗鬥魚鳥麗齊"
    "聲聯聰聽腦臉藝蟲號衛裝複見規視覺親觀"
    "計訂記訓討設訪許訴診評買賣質賽軍軟輪輸轉農遠"
    "擊擔據斷書條絲兩嚴豐臨烏雲廳慶億則剛創辦"
    "師帳廣廠廢彈強歸錄戰戲戶歲歷殺氣漢潔濟"
    "爐爭牆獨獲獻獸環畢異療盜監盤碼確禮積穩競筆"
    "納純紛紙細終絕絡綜綠維網緊線編緩縣縮總織繩繪"
    "罰羅聞職脅舉舊艙蘇蘋蠻術衝補裡製覽觸"
    "負責貨貪貴費賀賊賓賜賞購贈贏趕跡躍軌較載輔輕"
    "辭邊遷鄰鍾鑽長門閃閉閱闊關陽隊階隱雖雜離"
    "韓響頂頓頻顆額類顧顯飄餅餐餘館駐騎騙驚鬆鬧魯"
    "鮮鴨鷹麥麵黃黨齡龍龜"
)

# 這幾個詞在某些脈絡是對的，命中也不報。
ALLOW = {
    # 「资料」在大陸也講「资料」（例如「参考资料」），只有「資料＝data」
    # 那個意思要換成「数据」。分不出來，乾脆不管。
    "资料",
}


def rows(path: Path):
    """一個檔案裡的 (鍵, 譯文)。兩種格式都認。

    <p>說明欄（{@code _note}、{@code _meta.note}）也算一條。它不是譯文，
    但它是簡中譯者打開檔案看到的第一段字——整段繁體擺在那裡，等於在說
    「這份東西是從繁體搬來的，照著轉就好」。實測 193 個檔都是這樣。
    """
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return
    note = data.get("_note")
    if isinstance(note, str) and note.strip():
        yield "_note", note
    meta = data.get("_meta")
    if isinstance(meta, dict) and isinstance(meta.get("note"), str):
        yield "_meta.note", meta["note"]
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
        for key, dst in rows(path):
            if not dst.strip():
                continue
            checked += 1
            rel = path.relative_to(base).as_posix()
            short = key if len(key) <= 42 else key[:42] + "…"

            # 先問「字轉了沒」。連字都是繁體的話，下面的詞表一條也比不到，
            # 報一堆「建議用」只會蓋掉真正的問題。
            trad = sorted({c for c in dst if c in TRAD_ONLY})
            if trad:
                print(f"  [{rel}] 還是繁體字：{''.join(trad)}")
                print(f"      {short}  ->  {dst[:60]}")
                hits += 1
                continue

            for bad, good in CONVERTED.items():
                if bad in ALLOW or bad not in dst:
                    continue
                print(f"  [{rel}] 「{bad}」建議用「{good}」")
                print(f"      {short}  ->  {dst[:60]}")
                hits += 1

    print(f"\n{lang}：檢查 {checked} 條，{hits} 處繁中直譯的痕跡")
    return 1 if hits else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
