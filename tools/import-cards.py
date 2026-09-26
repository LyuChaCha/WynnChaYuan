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

已經有譯文的一律不動，只補新的。
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


def run(cards_path: Path, lang: str, write: bool,
        use_dst: bool = False) -> list[tuple[str, str]]:
    corpus = load_corpus(lang)
    builder = Builder(corpus, lang)
    doc = json.loads(cards_path.read_text(encoding="utf-8"))
    entries = doc.get("entries", doc)

    made: dict[str, dict[str, str]] = collections.defaultdict(
        collections.OrderedDict)
    todo: list[tuple[str, str]] = []
    for key, val in entries.items():
        if key.startswith("_") or not isinstance(val, dict):
            continue
        kind = kind_of(str(val.get("card") or ""))
        if kind not in ROUTE:
            continue
        src = val.get("src", "")
        if not src or src in corpus:
            continue
        # cards.json 的 dst 是收集者自己填的，沒有記是哪一個語言，
        # 所以只有在指定單一 --lang 時才敢用。
        dst = (val.get("dst") if use_dst else None) or builder.block(src)
        if dst:
            made[ROUTE[kind]][src] = dst
        else:
            todo.append((str(val.get("card") or ""), src))

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
    print("[%s] 組出 %d 條，還缺 %d 條" % (lang, total, len(todo)))
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
