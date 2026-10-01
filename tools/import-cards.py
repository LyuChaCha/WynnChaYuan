#!/usr/bin/env python3
"""把玩家回傳的 cards.json 併進語料。

為什麼需要
----------
`cards.json` 跟 `captured.json` 是兩份不同的收集檔，回報問題的人常常只附
後者。但「任務書卡片只翻一半」的答案在前者：

  src    算繪端<b>實際拿去查表</b>的那一句——幾行原文攤平成一句，
         行與行之間接一個半形空白。
  lines  還沒攤平的逐行原樣。只是給人看斷行斷在哪，<b>不是</b>要翻的東西。
  card   這一段屬於哪一張卡（tooltip 第一行）。

語料裡如果只有逐行的碎片（`Talk to Ormrod in the {p}`、`at [{~}, {~}, -{~}]`）
而沒有整段，算繪端會整張卡放棄——拿碎片拼會夾出半中半英。
`_meta.events.skipped.perLine` 記的就是這件事。

`merge_captured.py` 不吃這份：它照 `domain` 分檔，而卡片全是 `domain: gui`，
整批會被丟進 gui.json。所以有了這支。

怎麼補
------
不重新翻。卡片的每一塊都是幾個欄位拼起來的，而那些欄位語料裡多半早就有
譯文（`✔À Combat Lv. Min: {~}`、`{#}Length: Long`、`{#}- +{~} Emeralds`），
所以照 `{#}` 把 src 拆開逐段查表，**全部查得到才**照原順序組回去。
查不到任何一段就跳過，列進報告給人翻——不自己造新詞。

拆 src 而不是拆 lines，是因為 lines 照畫面寬度斷，會斷在詞中間
（`The Steel` / `Feather Fast Travel`），那種永遠拼不起來。

用法
----
    python tools/import-cards.py <cards.json 的路徑>            # 只看會加什麼
    python tools/import-cards.py <cards.json 的路徑> --write    # 寫回語料
    python tools/import-cards.py <cards.json 的路徑> --todo x.txt  # 導出待翻清單
    python tools/import-cards.py --selftest                    # 組合與歸屬的規則

已經有譯文的一律不動，只補新的。

哪一塊進哪一個檔看 `where()`。**認不出歸屬的也會列進 `--todo`**——先前是
直接 continue，於是「沒有卡片符合」與「每一張都補好了」印出來一模一樣。
"""
from __future__ import annotations

import argparse
import collections
import io
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "src/main/resources/assets/wynnchayuan/translations"

# 卡片種類 -> 該進哪個譯文檔。mini-quest.json 的說明本來就寫明「必須是併成
# 一句的整段」，跟這裡要放的東西是同一件事。首領祭壇沒有專屬檔，先進 misc。
ROUTE = {
    "Quest": "quest-ui.json",
    "Mini-Quest": "mini-quest.json",
    "Secret Discovery": "discovery.json",
    "World Discovery": "discovery.json",
    "Dungeon": "dungeon.json",
    "Boss Altar": "misc.json",
    "Cave": "cave.json",
    "Lootrun Camp": "lootrun.json",
}

# 組合時會用到的詞。簡中不是繁中轉出來的，用詞也不一樣（腐化不是腐敗、
# 地牢不是地城），寫死成繁中會被 tools/check-zh-cn.py 擋下來。
WORD = {
    "zh_tw": {"quest": "任務", "enter": "可進入", "corrupt": "腐敗",
              "frag": "碎片", "key": "鑰匙"},
    "zh_cn": {"quest": "任务", "enter": "可进入", "corrupt": "腐化",
              "frag": "碎片", "key": "钥匙"},
}

SPLIT = re.compile(r"(?=\{#\})")
QUESTROW = re.compile(r"^([✔✖✘ÀÁ]+)\s*Quest: (.+)$")
LABELLED = re.compile(r"^(\{#\})?([A-Z][a-z]+): (.+)$")
REWARD = re.compile(r"^(\{#\})?- \+(\{~\} )?(.+)$")
TITLE = re.compile(r"^(.+?) (\[[^\]]+\]) (.+) - ([A-Z][a-z].*)$")
TITLE2 = re.compile(r"^(.+?) (\[[^\]]+\]) ([A-Z][a-z].*)$")
ACCESS = re.compile(r"^Access to (?:the )?(.+)$")
HAN_A = "一"
HAN_Z = "鿿"

# ---- 這一筆 src 是「一段被寬度折斷的句子」，還是「各自獨立的幾列」----
#
# `src` 永遠是攤平的一句，但攤平之前是什麼，兩種情況完全不同：
#
#   折斷的句子  Bring [{~} Fluffy Fur] to the / Slaying Post at / [{~}, {~}, -{~}]
#   獨立的幾列  ✔À Mining Lv. Min: {~} / {#}Distance: … / {#}Length: …
#
# 前者照 src 收是對的（算繪端查的就是整段）；後者照 src 收等於把 #915／#922／
# #924 拆掉的東西原封不動做回去——整段那條路假設它是被寬度折斷的同一句話，
# 命中之後照面板寬度重新折行，四列被擠成兩列，<b>每一列的顏色也跟著貼錯位置</b>。
#
# 判準分兩半。結構上認得出來的「新的一列」——縮排圖示、條列、勾叉、
# 「標籤: 值」、屬性列、純圖示墊行——是 #915／#922／#924 三種樣式的共同特徵。
# 認不出結構的（`Left Click to view contents` / `Shift Right-Click to sell …`）
# 再看<b>斷點的位置</b>：真的是寬度折斷的話，下一列的第一個詞一定塞不進這一列；
# 塞得進去還斷了，就是遊戲自己斷的。見 hard_break。
PAD = re.compile(r"^(?:\{#\})+$")
BULLETROW = re.compile(r"^(?:\{#\})* ?- ")
STATUSROW = re.compile(r"^[✔✖✘ÀÁ✦]")
FIELDROW = re.compile(r"^(?:\{#\})* ?[A-Z][A-Za-z0-9 .'/()-]{0,28}:(?= |$)")
STATROW = re.compile(r"^(?:\{#\})* ?[A-Z][A-Za-z ]{0,28}\{#\}\s*[-+]\s*\{~\}")
# 圖示後面接一個半形空白，就是遊戲在起一列時放的縮排／項目圖示——市集面板
# 的 `{#} Left-Click to quick stash`、素材的 `{#}{#} Scribing{#}`、價格列的
# `{#} {~}² ✮ {~}²` 都是這樣。接著沒有空白的話才要求大寫，為的是把 Wynntils
# 疊層那種句子中段（`{#}this Lootrun. Gain`）擋在外面。
INDENTROW = re.compile(r"^(?:\{#\})+(?: |(?=[A-Z]))")
KINDROW = re.compile(r"\[[^\]]+\]$")
LETTER = re.compile(r"[A-Za-z]")

# 句子折到這些詞就斷，等於下一列是續行。只收虛詞：動詞收進來就開始誤判
# （`Shift Right-Click to sell` 這種獨立的一列也會被當成續行的前半）。
CONNECTORS = frozenset("""
a an the and or but to of in on at for with from by as into onto over under
than that this these those your their its his her our my if when while
which who whom whose is are was were be been being am will would shall
should can could may might must do does did than then so
""".split())

# 佔位符在畫面上的實際寬度跟字面長度差很多，量斷點的時候要換算。
# `{~}` 剛好是三個字元、數值也多半三位數，所以不必動。
WIDTH = {"{#}": 1, "{p}": 8, "{u}": 8}


def han(ch: str) -> bool:
    return bool(ch) and HAN_A <= ch <= HAN_Z


def latin(ch: str) -> bool:
    return bool(ch) and ch.isascii() and (ch.isalnum() or ch in "[(")


def glue(a: str, b: str) -> str:
    """接起來。漢字與拉丁字母之間要一個半形空白，漢字之間不要。"""
    if not a or not b:
        return (a or "") + (b or "")
    if (han(a[-1]) and latin(b[0])) or (latin(a[-1]) and han(b[0])):
        return a + " " + b
    return a + b


def weld(parts: list[str]) -> str:
    """把逐段的譯文接回一句。

    預設接一個半形空白（原文就是這樣攤平的），只有**接縫兩邊都是漢字**
    才不留。注意只看接縫，不能整句掃——譯名本身可能就含空白
    （quest-name.json 的「使者 第二部」），掃過去會把它吃掉。
    """
    out = ""
    for p in parts:
        if not out:
            out = p
            continue
        out += ("" if (han(out[-1:]) and han(p[:1])) else " ") + p
    return out


def vis(text: str) -> int:
    """這一列在畫面上大概多寬（以字元計）。只用來比斷點，不求精確。"""
    out = len(text)
    for token, width in WIDTH.items():
        out += text.count(token) * (width - len(token))
    return out


def rowstart(row: str) -> str | None:
    """這一列的開頭是不是「一列的開頭」。認不出來回傳 {@code None}。

    這五種樣式都是遊戲自己起新列時加的記號，續行不會有：

      pad     `{#}{#}{#}…`       純圖示的墊行
      bullet  `{#}- +{~} XP`     獎勵條列（#922）
      status  `✔À …` `✦ …`      需求的勾叉與項目符號
      field   `{#}Length: Short` 標籤: 值（#915）
      stat    `Health {#}-{~} …` 屬性列
      indent  `{#}Something`     縮排圖示後面接大寫

    `indent` 刻意要求後面是**大寫**：Wynntils 疊層那些片段
    （`{#}this Lootrun. Gain`）也以 `{#}` 開頭，但它是句子的中段。
    """
    if PAD.match(row):
        return "pad"
    if BULLETROW.match(row):
        return "bullet"
    if STATUSROW.match(row):
        return "status"
    if FIELDROW.match(row):
        return "field"
    if STATROW.match(row):
        return "stat"
    if INDENTROW.match(row):
        return "indent"
    return None


def hard_break(above: str, below: str, first: bool, width: int) -> bool:
    """這兩列之間是「各自獨立」的斷點（True），還是寬度折出來的（False）。

    順序有講究，每一條都是踩出來的：

    1. 墊行（`{#}{#}{#}…`）兩邊一定是斷點——它根本沒有文字可以折。
    2. 下一列認得出是新的一列（見 {@link rowstart}）就是斷點。條列以 `-`
       開頭、續行也可能以 `-` 開頭（`-{~} to -{~}`），所以**先問結構再問字元**。
    3. 下一列不是大寫開頭（小寫、`+`、`(`、數字、`{~}`）——續行的樣子。
    4. 上一列以句點／逗號收尾：那是散文，不是欄位。欄位收在值或 `:` 上。
    5. 上一列以虛詞收尾，或括號還沒收——`… to the` / `Shift Right-Click (`。
    6. 上一列是卡片標題（類型方括號收尾）而且是第一列：標題與狀態之間
       沒有任何記號可認，切點就在方括號之後（#924）。
    7. 都不是的話量斷點：下一列的第一個詞<b>塞得進</b>上一列的話，
       這個斷行不是寬度造成的。寬度拿這一段最寬的那一列當估計值——
       估得偏窄，所以這一條偏向判成「折斷的句子」，也就是偏向原本的行為。
    """
    if PAD.match(above) or PAD.match(below):
        return True
    if rowstart(below):
        return True
    if not re.match(r"^[A-Z]", below):
        return False
    tail = above.rstrip()
    if tail[-1:] in ".!?…,;":
        return False
    if tail.count("(") != tail.count(")") or tail.count("[") != tail.count("]"):
        return False
    if re.sub(r"[^A-Za-z]", "", tail.rsplit(" ", 1)[-1]).lower() in CONNECTORS:
        return False
    if first and KINDROW.search(tail):
        return True
    return vis(tail) + 1 + vis(below.split(" ")[0]) <= width


def shape_of(src: str, lines: list[str]) -> str:
    """這一筆要照整段收（`block`）還是照 lines 逐列收（`lines`）。

    兩種斷點混在同一段裡回傳 `mixed`：那種整段收會重新折行、逐列收又會夾出
    半中半英，兩邊都錯，所以不猜，列進 `--todo` 給人看。

    **`lines` 接不回 `src` 就一律照整段收。** 收集端換了攤平規則、或是手改過
    的 cards.json，都會對不上；對不上的時候逐列收是在拿不確定的資料下手。
    """
    rows = [r for r in (lines or []) if r]
    if len(rows) < 2 or " ".join(rows) != src:
        return "block"
    width = max(vis(r) for r in rows)
    calls = [hard_break(rows[i], rows[i + 1], i == 0, width)
             for i in range(len(rows) - 1)]
    if all(calls):
        return "lines"
    return "mixed" if any(calls) else "block"


def load_corpus(lang: str) -> dict[str, str]:
    """整個語言的 src -> dst。只收已經有譯文的。"""
    out: dict[str, str] = {}
    for path in sorted(BASE.joinpath(lang).rglob("*.json")):
        try:
            doc = json.loads(path.read_text(encoding="utf-8"))
        except (ValueError, OSError):
            continue
        entries = doc.get("entries", doc)
        if not isinstance(entries, dict):
            continue
        for key, val in entries.items():
            if key.startswith("_"):
                continue
            if isinstance(val, dict):
                src, dst = val.get("src", key), val.get("dst", "")
            else:
                src, dst = key, val
            if isinstance(dst, str) and dst.strip():
                out.setdefault(src, dst)
    return out


class Builder:
    """照語料把一小段原文拼成譯文。拼不出來回 None。"""

    def __init__(self, corpus: dict[str, str], lang: str):
        self.c = corpus
        self.w = WORD[lang]

    def thing(self, name: str) -> str | None:
        """物品／地點名。腐敗版、「…碎片」、「…鑰匙」照規則產生。"""
        found = self.c.get(name)
        if found:
            return found
        rules = (
            (r"^Corrupted (.+)$", self.w["corrupt"], ""),
            (r"^(.+) Fragments$", "", self.w["frag"]),
            (r"^(.+) Key$", "", self.w["key"]),
        )
        for pattern, pre, post in rules:
            m = re.match(pattern, name)
            if not m:
                continue
            inner = self.c.get(m.group(1)) or self.thing(m.group(1))
            if inner:
                return glue(glue(pre, inner), post)
        return None

    def tag(self, bracketed: str) -> str | None:
        """`[Quest]` 這種標籤。語料裡通常只收不帶方括號的。"""
        return self.c.get(bracketed) or (
            "[%s]" % self.c[bracketed[1:-1]]
            if bracketed[1:-1] in self.c else None)

    def line(self, text: str) -> str | None:
        m = QUESTROW.match(text)
        if m:
            name = self.c.get(m.group(2))
            return "%s %s: %s" % (m.group(1), self.w["quest"], name) if name else None

        m = LABELLED.match(text)
        if m:
            label, value = self.c.get(m.group(2) + ":"), self.c.get(m.group(3))
            if label and value:
                return (m.group(1) or "") + label + " " + value
            return None

        m = REWARD.match(text)
        if m:
            head = (m.group(1) or "") + "- +" + ("{~} " if m.group(2) else "")
            body = m.group(3)
            gate = ACCESS.match(body)
            if gate:
                # 「可進入」只對地方講得通；服務類（快速旅行、升降梯…）
                # 要用「可使用」，光看字串分不出來，所以整行收進語料。
                place = self.thing(gate.group(1))
                return head + glue(self.w["enter"], place) if place else None
            item = self.thing(body)
            return head + item if item else None

        m = TITLE.match(text)
        if m:
            name, bracket, middle, state = m.groups()
            parts = (self.thing(name), self.tag(bracket),
                     self.c.get(middle), self.c.get(state))
            return "%s %s %s - %s" % parts if all(parts) else None

        m = TITLE2.match(text)
        if m:
            name, bracket, state = m.groups()
            parts = (self.thing(name), self.tag(bracket), self.c.get(state))
            return "%s %s %s" % parts if all(parts) else None

        return None

    def row(self, text: str) -> str | None:
        """單獨一列。**不照 `{#}` 拆**——列裡面的 `{#}` 是圖示或縮排，
        不是欄界（`Health {#}-{~} [{~}]` 拆開就什麼都查不到了）。"""
        return self.c.get(text) or self.line(text)

    def block(self, src: str) -> str | None:
        """一整塊。照 `{#}` 拆開逐段拼；拆不出欄位就整句試一次。"""
        segments = [s.strip() for s in SPLIT.split(src) if s.strip()]
        if len(segments) < 2:
            return self.c.get(src) or self.line(src)
        out = []
        for seg in segments:
            done = self.c.get(seg) or self.line(seg)
            if done is None:
                return None
            out.append(done)
        return weld(out)


def kind_of(card: str) -> str | None:
    m = re.search(r"\[([^\]]+)\]\s*$", card or "")
    return m.group(1) if m else None


def where(card: str, src: str) -> str | None:
    """這一塊該進哪一個譯文檔；認不出來回傳 {@code None}。

    <h2>方括號認不出賜福卡</h2>
    內容書那些卡的標題結尾帶著類型（{@code Theatre Royal [Cave]}），所以
    {@link kind_of} 認得出來。但 Lootrun 的**賜福卡與使命卡**標題是純名字
    ——{@code Bad Omen}、{@code Patient Champion}、{@code Porphyrophobia}
    ——沒有方括號可認，整批都掉出去了。

    實測使用者那一份 143 張卡的 cards.json：**一張都沒被認出來**，而工具
    回報的是「組出 0 條，還缺 0 條」，看起來像是沒東西可補。

    <p>「Lootrun」這個詞只出現在 Lootrun 的內容裡，拿它當訊號不會誤判別的卡。
    認不出來的那些現在會列進 {@code --todo}，不再默默消失。
    """
    kind = kind_of(card)
    if kind in ROUTE:
        return ROUTE[kind]
    if "Lootrun" in src:
        return "lootrun.json"
    return None


def run(cards_path: Path, lang: str, write: bool,
        use_dst: bool = False) -> list[tuple[str, str]]:
    corpus = load_corpus(lang)
    builder = Builder(corpus, lang)
    doc = json.loads(cards_path.read_text(encoding="utf-8"))
    entries = doc.get("entries", doc)

    made: dict[str, dict[str, str]] = collections.defaultdict(
        collections.OrderedDict)
    todo: list[tuple[str, str]] = []
    seen = 0
    lost = 0
    shapes: collections.Counter[str] = collections.Counter()
    covered = 0
    dropped_dst = 0
    haystack: list[str] = []

    def under_block(row: str) -> bool:
        """這一列已經被某個<b>更長的、已翻好的</b>原文蓋著。

        逐列條目會讓算繪端先命中它、把整段那條路蓋掉——raid.json 那 13 條空的
        逐行片段就是這樣，填回去 GambitBlockTest 直接紅。所以這種列不寫，
        列進 `--todo` 讓人決定要不要反過來把那個整段拆開。

        整段是用單一半形空白接起來的，所以兩邊補一個空白再找就不會把
        `Health` 配到 `Healing Efficiency` 身上。用一整串字串一次找完：
        語料二十幾萬條，逐條比對會慢到沒人想跑。
        """
        if not haystack:
            haystack.append("\x00".join(" %s " % s for s in corpus))
        return (" %s " % row) in haystack[0]

    for key, val in entries.items():
        if key.startswith("_") or not isinstance(val, dict):
            continue
        seen += 1
        src = val.get("src", "")
        if not src:
            continue
        card = str(val.get("card") or "")
        # 攤平的那一句該不該當成一條語料，先問清楚。照 src 無條件收就是把
        # #915／#922／#924 拆掉的東西做回去，見 shape_of。
        shape = shape_of(src, val.get("lines") or [])
        shapes[shape] += 1
        if shape == "mixed":
            todo.append(("(整段與逐列混在一段裡) " + card, src))
            continue
        if src in corpus:
            # 整段早就有譯文。逐列的那種其實正是該拆開的條目，但這支工具
            # 只負責匯入，不動既有語料——重複的列寫進去只會互相蓋。
            if shape == "lines":
                covered += 1
            continue
        if shape == "lines" and use_dst and str(val.get("dst") or "").strip():
            # 收集者填的 dst 是<b>整段</b>的譯文，沒有斷行資訊，切不開。
            dropped_dst += 1
        units = [src] if shape == "block" else [
            r for r in val.get("lines") or [] if r]
        name = where(card, src)
        for unit in units:
            if not LETTER.search(unit):
                continue        # 純圖示的墊行（`{#}{#}{#}…`），沒有字要翻
            if unit in corpus:
                continue        # 這一列本來就有譯文（拆過的欄位多半如此）
            if name is None:
                # 認不出歸屬的也要報。先前是 continue，於是「沒有卡片符合」與
                # 「每一張都補好了」印出來一模一樣，而前者才是常態。
                lost += 1
                todo.append(("(認不出歸屬) " + card, unit))
                continue
            if shape == "lines" and under_block(unit):
                todo.append(("(被更長的整段蓋著，先拆那一段) " + card, unit))
                continue
            # cards.json 的 dst 是收集者自己填的，沒有記是哪一個語言，
            # 所以只有在指定單一 --lang 時才敢用；而且它是整段的，逐列不採用。
            dst = ((val.get("dst") if use_dst and shape == "block" else None)
                   or (builder.block(unit) if shape == "block"
                       else builder.row(unit)))
            if dst:
                made[name][unit] = dst
            else:
                todo.append((card, unit))

    total = 0
    for name, table in sorted(made.items()):
        path = BASE / lang / name
        # 這批檔案有的是 CRLF 有的是 LF，讀寫都要用 newline="" 原樣進出，
        # 不然整檔的行尾會被統一掉，diff 變成幾千行。
        with io.open(path, encoding="utf-8", newline="") as fh:
            raw = fh.read()
        newline = "\r\n" if "\r\n" in raw else "\n"
        loaded = json.loads(raw, object_pairs_hook=collections.OrderedDict)
        target = loaded.get("entries", loaded)
        added = 0
        for src, dst in table.items():
            if src in target and str(target[src]).strip():
                continue
            target[src] = dst
            added += 1
        keys = [k for k in target if not k.startswith("_")]
        meta = loaded.get("_meta")
        if isinstance(meta, dict) and "count" in meta:
            meta["count"] = len(keys)
            meta["translated"] = sum(
                1 for k in keys
                if str((target[k].get("dst") if isinstance(target[k], dict)
                        else target[k]) or "").strip())
        if write and added:
            with io.open(path, "w", encoding="utf-8", newline=newline) as fh:
                fh.write(json.dumps(loaded, ensure_ascii=False, indent=1) + "\n")
        print("  %-20s +%d" % (name, added))
        total += added
    print("[%s] 看過 %d 塊：組出 %d 條，還缺 %d 條（其中 %d 條認不出歸屬）"
          % (lang, seen, total, len(todo), lost))
    print("      其中 %d 塊是各自獨立的幾列（照 lines 逐列收）、"
          "%d 塊是折斷的一句（照 src 整段收）、%d 塊兩種混在一起（不收）"
          % (shapes["lines"], shapes["block"], shapes["mixed"]))
    if shapes["mixed"]:
        print("      混在一起的那些要人看一眼：整段收會重新折行，"
              "逐列收會夾出半中半英，兩邊都錯。加 --todo 看是哪些。")
    if covered:
        print("      另有 %d 塊的整段已經有譯文，而它其實是獨立的幾列"
              "——那正是 #915／#922／#924 要拆的那種，值得回頭拆掉。" % covered)
    if dropped_dst:
        print("      %d 塊的 dst 是收集者照整段填的，逐列收切不開，沒有採用。"
              % dropped_dst)
    if lost:
        print("      認不出歸屬的多半是還沒寫進 ROUTE 的卡種。"
              "加 --todo 看是哪些，值得收的就補一條規則進 where()。")
    return todo


def selftest() -> int:
    """組合規則的自我檢查：`python tools/import-cards.py --selftest`

    這裡錯了不會報錯，只會安靜地把接縫的空白、簡繁用詞弄壞，
    而且要等 CI 的 check-zh-cn.py 才看得出來，所以留一份能隨時重跑的案例。
    """
    corpus = {
        "{#}Rewards:": "{#}獎勵:",
        "{#}- +{~} XP": "{#}- +{~} 經驗",
        "{#}- +{~} Emeralds": "{#}- +{~} 綠寶石",
        "Emeralds": "綠寶石",
        "Conviction": "信念",
        "Eldritch Outlook": "詭異瞭望台",
        "Canopy": "樹冠",
        "Length:": "長度:",
        "Long": "長",
        "Solidarity of Steel": "鋼鐵同心",
        "The Steel Feather Part I": "鋼鐵之羽 第一部",
        "Already completed": "已完成",
        "Quest": "任務",
        "A Journey Beyond": "彼界之旅",
    }
    b = Builder(corpus, "zh_tw")
    cases = [
        ("欄位逐段拼回去",
         b.block("{#}Rewards: {#}- +{~} XP {#}- +{~} Emeralds"),
         "{#}獎勵: {#}- +{~} 經驗 {#}- +{~} 綠寶石"),
        ("語料只有物品名，整行照規則補",
         b.block("{#}Rewards: {#}- +{~} XP {#}- +{~} Conviction"),
         "{#}獎勵: {#}- +{~} 經驗 {#}- +{~} 信念"),
        ("腐敗版與碎片是規則產生的，漢字之間不留空白",
         b.line("{#}- +{~} Corrupted Eldritch Outlook Fragments"),
         "{#}- +{~} 腐敗詭異瞭望台碎片"),
        ("可進入後面接漢字不留空白",
         b.line("{#}- +Access to the Canopy"), "{#}- +可進入樹冠"),
        ("詞表拼得出 {#}Length: Long",
         b.line("{#}Length: Long"), "{#}長度: 長"),
        ("標題列三段",
         b.block("Solidarity of Steel [Quest] The Steel Feather Part I "
                 "- Already completed"),
         "鋼鐵同心 [任務] 鋼鐵之羽 第一部 - 已完成"),
        ("沒有中間那段的標題列",
         b.block("A Journey Beyond [Quest] Already completed"),
         "彼界之旅 [任務] 已完成"),
        ("缺一段就整塊放棄，不半中半英",
         b.block("{#}Rewards: {#}- +{~} XP {#}- +{~} Nonexistent Trinket"), None),
    ]
    bad = 0
    for what, got, want in cases:
        ok = got == want
        print(("  [PASS] " if ok else "  [FAIL] ") + what
              + ("" if ok else "（實際 %r，預期 %r）" % (got, want)))
        bad += 0 if ok else 1

    # 譯名本身可能就含空白，接縫規則不可以整句掃掉
    ok = weld(["✔À 任務: 使者 第二部", "{#}長度: 長"]) == "✔À 任務: 使者 第二部 {#}長度: 長"
    print(("  [PASS] " if ok else "  [FAIL] ") + "譯名裡原有的空白要留著")
    bad += 0 if ok else 1

    # 簡中要用自己的詞，寫死繁中會被 check-zh-cn.py 擋下來
    cn = Builder({"A Journey Beyond": "彼界之旅", "Quest": "任务",
                  "Already completed": "已完成", "Canopy": "树冠"}, "zh_cn")
    got = cn.block("A Journey Beyond [Quest] Already completed")
    ok = got == "彼界之旅 [任务] 已完成"
    print(("  [PASS] " if ok else "  [FAIL] ") + "簡中用簡中的詞"
          + ("" if ok else "（實際 %r）" % (got,)))
    bad += 0 if ok else 1
    got = cn.line("{#}- +Access to the Canopy")
    ok = got == "{#}- +可进入树冠"
    print(("  [PASS] " if ok else "  [FAIL] ") + "簡中的「可进入」"
          + ("" if ok else "（實際 %r）" % (got,)))
    bad += 0 if ok else 1

    # ---- 歸屬。認錯了會把整批卡寫進不該去的檔，認不出來則是<b>默默</b>漏掉 ----
    for what, card, src, want in (
            ("內容書的卡照方括號", "Theatre Royal [Cave]", "Anything", "cave.json"),
            ("任務卡", "The Steel Feather [Quest]", "Anything", "quest-ui.json"),
            ("★ 賜福卡沒有方括號，靠 src 裡的 Lootrun 認",
             "Bad Omen",
             "For the rest of this Lootrun, gain +{~} Loot (Max x{~})"
             " everytime you get a Curse", "lootrun.json"),
            ("★ 使命卡同理", "Patient Champion",
             "Once you reach {~} Challenges completed during your"
             " Lootrun, gain +{~} {#}Strength", "lootrun.json"),
            # 認不出來是<b>對的</b>——裝備 tooltip 的名稱歸 gear-*.json，
            # 不該由這支工具去猜。重點是它現在會列進 --todo，不再無聲消失。
            ("裝備 tooltip 不歸這裡管", "{#}Bonder{#}",
             "Health Regen {#}+{~} [{~}]", None),
            ("方括號裡不是卡種也不硬猜", "Emerald Pouch [Tier {~}]",
             "- {~} Rows", None),
    ):
        got = where(card, src)
        ok = got == want
        print(("  [PASS] " if ok else "  [FAIL] ") + what
              + ("" if ok else "（要 %r，實際 %r）" % (want, got)))
        bad += 0 if ok else 1

    # ---- 整段還是逐列。判錯的兩個方向都不會報錯，只會安靜地弄壞畫面：
    #      把獨立的幾列當成一句 → 重新折行，欄位黏在一起、顏色貼錯位置；
    #      把折斷的一句當成幾列 → 逐列條目蓋掉整段那條路，夾出半中半英。
    #      正反例都要，而且反例用的是<b>實機真的收到過</b>的那幾種段落。 ----
    for what, src, lines, want in (
            # 正例：各自獨立的幾列（照 lines 逐列收）
            ("需求欄位（#915）",
             "✔À Mining Lv. Min: {~} {#}Distance: Medium ({~} Blocks)"
             " {#}Length: Short {#}Difficulty: Easy",
             ["✔À Mining Lv. Min: {~}", "{#}Distance: Medium ({~} Blocks)",
              "{#}Length: Short", "{#}Difficulty: Easy"], "lines"),
            ("需求裡的前置任務也是自己一列",
             "✔À Combat Lv. Min: {~} ✔À Quest: The Price of Ingenuity"
             " {#}Length: Long",
             ["✔À Combat Lv. Min: {~}", "✔À Quest: The Price of Ingenuity",
              "{#}Length: Long"], "lines"),
            ("獎勵條列（#922）",
             "{#}Rewards: {#}- +{~} XP {#}- +{~} Emeralds",
             ["{#}Rewards:", "{#}- +{~} XP", "{#}- +{~} Emeralds"], "lines"),
            ("標題＋狀態（#924），切點在類型方括號之後",
             "Gather Carp II [Mini-Quest] Currently in progress",
             ["Gather Carp II [Mini-Quest]", "Currently in progress"], "lines"),
            ("屬性列：每一列都是一個詞條",
             "Health {#}-{~} [{~}] Healing Efficiency {#}+{~} [{~}]"
             " Reflection {#}+{~} [{~}]",
             ["Health {#}-{~} [{~}]", "Healing Efficiency {#}+{~} [{~}]",
              "Reflection {#}+{~} [{~}]"], "lines"),
            ("素材的詞條沒有空白也算一列",
             "Spell Damage{#}+{~} to +{~} Health Regen{#}+{~} to +{~}",
             ["Spell Damage{#}+{~} to +{~}", "Health Regen{#}+{~} to +{~}"],
             "lines"),
            ("純圖示的墊行兩邊一定是斷點",
             "Damage {#}-{~} [{~}] {#}{#}{#}{#}",
             ["Damage {#}-{~} [{~}]", "{#}{#}{#}{#}"], "lines"),
            ("「標籤: 值」底下接條列",
             "Discoveries: - Territorial: {~}/{~} [{~}] - World: {~}/{~} [{~}]",
             ["Discoveries:", "- Territorial: {~}/{~} [{~}]",
              "- World: {~}/{~} [{~}]"], "lines"),
            ("✦ 也是項目符號",
             "Unassigned Skill Points: {~} ✦ Unused Ability Points: {~}",
             ["Unassigned Skill Points: {~}", "✦ Unused Ability Points: {~}"],
             "lines"),
            # ★ 認不出結構的兩列，靠斷點位置判：「Shift」塞得進上一列還斷了
            ("★ 兩句操作說明，結構認不出來但斷點塞得進去",
             "Left Click to view contents Shift Right-Click to sell"
             " ({~}-{~}²)",
             ["Left Click to view contents",
              "Shift Right-Click to sell ({~}-{~}²)"], "lines"),
            # 反例：一句被寬度折斷（照 src 整段收，也就是原本的行為）
            ("★ 續行以小寫開頭",
             "For the rest of this Lootrun, gain +{~} Loot (Max x{~})"
             " everytime you get a Curse",
             ["For the rest of this Lootrun, gain",
              "+{~} Loot (Max x{~}) everytime you", "get a Curse"], "block"),
            ("★ 上一列以虛詞收尾，下一列照樣是大寫開頭",
             "Bring [{~} Fluffy Fur] to the Slaying Post"
             " [Combat Lv. {~}] at [{~}, {~}, -{~}]",
             ["Bring [{~} Fluffy Fur] to the",
              "Slaying Post [Combat Lv. {~}] at", "[{~}, {~}, -{~}]"], "block"),
            ("★ 兩個完整句子，但最後一列短得塞得回去 → 是折斷的散文",
             "You have discovered a new area! Explore it to find secrets.",
             ["You have discovered a new area!",
              "Explore it to find secrets."], "block"),
            ("★ 縮排圖示後面接小寫是句子中段，不是新的一列",
             "{#}Effects{#}this Lootrun. Gain {#}{~}% Potency",
             ["{#}Effects{#}this Lootrun. Gain", "{#}{~}% Potency"], "block"),
            ("括號還沒收就斷，是折斷的",
             "Gain +{~} Loot (Max x{~} per Chest)",
             ["Gain +{~} Loot (Max", "x{~} per Chest)"], "block"),
            ("只有一列就沒有斷點可談", "Static Boon", ["Static Boon"], "block"),
            ("lines 接不回 src 就不猜，照整段收",
             "Health {#}-{~} [{~}] Reflection {#}+{~} [{~}]",
             ["Health {#}-{~} [{~}]"], "block"),
            ("沒有 lines 欄位也照整段收",
             "Health {#}-{~} [{~}] Reflection {#}+{~} [{~}]", [], "block"),
            # 混在一起：兩種斷點都有，兩邊都會錯，所以不收
            ("★ 標題＋狀態底下接一段折斷的敘述 → 混在一起，不收",
             "Scorched Earth [Cave] Fight through the burning wastes"
             " to reach the heart",
             ["Scorched Earth [Cave]", "Fight through the burning wastes",
              "to reach the heart"], "mixed"),
    ):
        got = shape_of(src, lines)
        ok = got == want
        print(("  [PASS] " if ok else "  [FAIL] ") + what
              + ("" if ok else "（要 %r，實際 %r）" % (want, got)))
        bad += 0 if ok else 1

    # 逐列拼的時候不可以照 `{#}` 再拆一次：列裡面的 `{#}` 是圖示或縮排。
    rb = Builder({"Health {#}-{~} [{~}]": "生命 {#}-{~} [{~}]",
                  "{#}Length: Short": "{#}長度: 短"}, "zh_tw")
    for what, row, want in (
            ("屬性列整列查表", "Health {#}-{~} [{~}]", "生命 {#}-{~} [{~}]"),
            ("欄位整列查表", "{#}Length: Short", "{#}長度: 短"),
            ("查不到就不要硬湊", "Reflection {#}+{~} [{~}]", None),
    ):
        got = rb.row(row)
        ok = got == want
        print(("  [PASS] " if ok else "  [FAIL] ") + what
              + ("" if ok else "（要 %r，實際 %r）" % (want, got)))
        bad += 0 if ok else 1

    print("全過。" if not bad else "%d 項沒過。" % bad)
    return 1 if bad else 0


def main() -> int:
    if "--selftest" in sys.argv[1:]:
        return selftest()
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("cards", type=Path, help="玩家回傳的 cards.json")
    ap.add_argument("--write", action="store_true", help="寫回語料")
    ap.add_argument("--lang", action="append",
                    help="只處理這個語言（可重複）。指定單一語言時，cards.json 裡填好的 dst 會直接採用；預設 zh_tw 與 zh_cn")
    ap.add_argument("--todo", type=Path, help="把組不出來的整段寫成清單")
    args = ap.parse_args()

    if not args.cards.is_file():
        print("找不到 %s" % args.cards, file=sys.stderr)
        return 2

    langs = args.lang or ["zh_tw", "zh_cn"]
    for lang in langs:
        if lang not in WORD:
            print("還沒有 %s 的組合用詞，先在 WORD 裡補一組" % lang, file=sys.stderr)
            return 2

    # 指定了單一語言，才把 cards.json 裡收集者填好的 dst 當成那個語言的。
    use_dst = len(langs) == 1 and bool(args.lang)
    left: dict[str, list[tuple[str, str]]] = {}
    for lang in langs:
        left[lang] = run(args.cards, lang, args.write, use_dst)

    if args.todo:
        with io.open(args.todo, "w", encoding="utf-8") as fh:
            for lang, rows in left.items():
                fh.write("=== %s 還缺 %d 條 ===\n" % (lang, len(rows)))
                for card, src in sorted(rows):
                    fh.write("%s\t%s\n" % (card, src))
                fh.write("\n")
        print("待翻清單寫到 %s" % args.todo)

    if not args.write:
        print("\n（預覽，加 --write 才寫回）")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
