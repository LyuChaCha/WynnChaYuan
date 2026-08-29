"""把玩家收集到的 `captured.json` 併進語料。

官方 CDN 只給裝備、素材與配方。介面文字、坐騎、粉末、Corkian、對話提示
<b>都不在裡面</b>——那些只能靠玩家在遊戲裡撞到才收得到。模組已經在收了
（`config/wynnchayuan/captured.json`），但收完之後沒有路可以進語料，
於是那個檔就躺在那裡。

這個工具補上那一段：讀 `captured.json`，把<b>語料還沒有</b>的條目
分到對應的譯文檔，`dst` 留空給人翻。

分類先看 `ctx`、再看 `domain`。對話的 `ctx` 長成
`dialogue/Cook Assistant#Aledar`——模組在收的當下就把追蹤器上的任務名
和面前 NPC 的名字記進去了，所以對話會直接進 `quest/cook-assistant.json`，
`speaker` 也一併填好。譯者拿到的不再是幾百句斷了脈絡的台詞，
而是照任務分好堆、標好誰在講話的段落。

任務檔不存在就<b>新建</b>一個——`quest/` 底下是來源，
`quest-dialogue.json` 是 `tools/quest-bundle.py` 產生的，所以不必改 `_index.json`。

用法：
    python tools/import-captured.py 某人的-captured.json
    python tools/import-captured.py 某人的-captured.json --write

分不出任務的一律進 `TARGET` 對應的檔，再分不出來就 `misc.json`——
寧可集中在一個檔裡等人重新歸類，也不要散進錯的檔案，
那會讓載入順序莫名其妙地打架。
"""

from __future__ import annotations

import collections
import json
import re
import sys
from pathlib import Path

TRANSLATIONS = Path("src/main/resources/assets/wynnchayuan/translations/zh_tw")

# domain -> 要放進哪個檔
TARGET = {
    "gui": "misc.json",
    "label": "npc.json",
    "npc": "npc.json",
    "chat": "misc.json",
    "quest": "quest.json",
    "item": "misc.json",
}

PARTS = TRANSLATIONS / "quest"

# ctx 長成 dialogue/Cook Assistant#Aledar 或 dialogue/choices/Cook Assistant
CTX = re.compile(r"^dialogue(?:/choices)?/([^#]+)(?:#(.*))?$")


def quest_of(entry: dict) -> tuple[str, str] | None:
    """從 ctx 拆出（任務名, 說話者）。

    收的當下追蹤器上寫著什麼就是什麼，所以這是線索不是權威——玩家可能追著 A
    卻順手跟 B 的 NPC 講話。分錯了頂多是排序不理想，不影響譯文本身。
    """
    match = CTX.match(entry.get("ctx", ""))
    if not match:
        return None
    quest = match.group(1).strip()
    return (quest, (match.group(2) or "").strip()) if quest else None


def slug(name: str) -> str:
    """任務名 -> 檔名。與 tools/quest-bundle.py 同一套規則。"""
    out = re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")
    return out or "unknown"


# 裝備／道具名：稀有度符號<b>緊貼著</b>名字，後面可能跟等級或層級。
# 「緊貼著」是關鍵——`{#} Click to Open` 這種提示符號後面有空格，那是要翻的。
ITEM_NAME = re.compile(
    r"^(?:\{#\}){2,}\S[^{}]*?(?:\s*\[(?:Tier )?\{~\}\])?$"
    r"|^\{#\}[^{}]+\{#\}$")


def worth_keeping(src: str) -> bool:
    """值不值得收進語料。

    收集器是逐行抓的，抓到的東西不見得是「一句話」。有兩種東西看起來像新資料，
    收進來卻只會害人：

    <ul>
    <li><b>斷掉的半句</b>——技能說明與物品敘述在原文就被折行了，抓到的是
        {@code orbs deal {~} damage, gain} 這種片段。整段翻譯走的是另一條路
        （會先把折行接回去），這裡收半句進來，譯者翻了也用不到，而且很可能翻錯：
        沒有前後文，那半句根本讀不出意思。小寫開頭或逗號結尾就是接續句的痕跡。
    <li><b>裝備名</b>——那是官方 CDN 的地盤，而且照專案的原則名字保留原文。
        收進來只會讓譯者面對一長串不該動的東西。
    </ul>
    """
    text = src.strip()
    if len(text) < 2 or sum(c.isalpha() for c in text) < 2:
        return False
    if text.endswith(",") or text[0].islower():
        return False
    return not ITEM_NAME.match(text)


def bare(src: str) -> str:
    """脫掉外框，露出裡面那句話。

    裝備名在物品欄長成 `{#}{#}{#}Hydrostatic Greaves [{~}]`——前面是稀有度符號、
    後面是等級；防具鱗片則是 `Arcanist Scale{#}[{~}]`。語料裡收的是裸名，
    兩者其實同一個東西；不正規化就會每一件都多收一條，看起來像新資料，
    實際上全是重複。
    """
    out = re.sub(r"\{#\}\s*\[\{~\}\]$", "", src)
    out = out.replace("{#}", "").strip()
    return re.sub(r"\s*\[(?:Tier )?\{~\}\]$", "", out).strip()


PLACEHOLDER = re.compile(r"\{[#~pu]\d?\}")
# 秒數的 s 黏在數值後面（{~}s），要跟著數值一起拿掉——但只在那個位置，
# 不然 Class 會被啃成 Cla、Blocks 變成 Block。
SECONDS = re.compile(r"\{~\d?\}s\b")
FILLER = re.compile(r"[-+±.,:;()\[\]0-9%/²¼]+")


def word_groups(src: str) -> list[str]:
    """把一行拆成「意思完整的詞組」。

    `Walk Speed {#}+{~} [{~}]` 拆出來就只有 `Walk Speed`——符號、數值、括號
    都是骨架，不帶意思。
    """
    body = PLACEHOLDER.sub("  ", SECONDS.sub("  ", src))
    return [part for part in
            (FILLER.sub(" ", chunk).strip() for chunk in re.split(r"\s{2,}", body))
            if part and any(c.isalpha() for c in part)]


# 撐起句型但不帶意思的字：`+{~} to {~}`、`-{~} tier`、`{~}stx Total`
SKELETON = {"to", "tier", "x", "stx", "total", "hits s", "min", "max"}


def already_piecewise(src: str, have: set[str]) -> bool:
    """整行的每個詞組都已經翻得出來了嗎？

    <h2>為什麼這種行不能收</h2>
    整行模板的優先度<b>高於</b>逐片段。`Walk Speed {#}+{~} [{~}]` 現在走的是
    逐片段：`Walk Speed` 換成中文、後面的符號與數值原樣留著，欄位對齊也是
    照原本的位移字元算的。這時候再補一條整行模板進去，它會<b>蓋掉</b>那條
    正常的路——而整行模板得自己重算對齊，一沒算對就是欄位歪掉。

    <p>也就是說，這種行收進來最好的情況是沒有差別，最壞的情況是把本來好的弄壞。
    那就不要收。
    """
    groups = word_groups(src)
    return bool(groups) and all(
            g in have or g + ":" in have or g.lower() in SKELETON
            for g in groups)


# 別人的名字。Minecraft 帳號名是英數加底線，長度 3-16。
NAMED = re.compile(
    r"\bCrafted by\b|\bParty\b.*\binvit|'s (?:party|guild|island|house)"
    # 領地名牌：「Controlled by Paladins United [Lv. 32]」。公會名稱跟玩家名稱
    # 一樣是別人的資料。它沒有底線，所以下面那條「帳號名的形狀」抓不到——
    # 一次 Lootrun 就有 83 條這樣穿了過去。
    r"|\bControlled by\b"
    r"|\b[A-Za-z0-9]*_[A-Za-z0-9_]{2,}\b")


def names_a_player(src: str) -> bool:
    """這一行是不是掛著別人的帳號名。

    <p>模組端已經有 {@code PlayerDataFilter} 擋在前面，但那是<b>收集</b>時的關卡；
    這裡是<b>進倉庫</b>前的最後一道。語料是要推上公開倉庫的，別人的名字一旦
    跟著進去就洗不掉了——多擋一次的代價只是偶爾漏收一句公用句型，
    比起把陌生人的帳號名散出去，那個代價便宜得多。
    """
    return bool(NAMED.search(src))


def existing() -> set[str]:
    """語料裡已經有的原文，含還沒翻的。"""
    seen: set[str] = set()
    for path in TRANSLATIONS.rglob("*.json"):
        if path.name.startswith("_"):
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        rows = data.get("entries")
        if isinstance(rows, dict):
            for key, value in rows.items():
                seen.add(value.get("src", key) if isinstance(value, dict) else key)
        else:
            seen.update(k for k in data if not k.startswith("_"))
    return seen


def main(argv: list[str]) -> int:
    write = "--write" in argv
    files = [a for a in argv if not a.startswith("--")]
    if not files:
        print(__doc__)
        return 2

    have = existing()
    by_file: dict[str, list[tuple[str, dict]]] = collections.defaultdict(list)
    skipped = 0
    for name in files:
        data = json.loads(Path(name).read_text(encoding="utf-8"))
        for key, entry in data.get("entries", {}).items():
            src = entry.get("src", "")
            if (not src or src in have or bare(src) in have
                    or not worth_keeping(src)
                    or already_piecewise(src, have)
                    or names_a_player(src)):
                skipped += 1
                continue
            have.add(src)                      # 同一批裡的重複只收一次
            have.add(bare(src))
            found = quest_of(entry)
            if found:
                quest, speaker = found
                entry["_quest"], entry["_speaker"] = quest, speaker
                by_file["quest/" + slug(quest) + ".json"].append((key, entry))
            else:
                by_file[TARGET.get(entry.get("domain"), "misc.json")].append((key, entry))

    total = 0
    for target, rows in sorted(by_file.items()):
        path = TRANSLATIONS / target
        if not path.is_file():
            if not target.startswith("quest/"):
                print(f"  ! 沒有 {target}，跳過 {len(rows)} 條")
                continue
            # 新任務：直接開一個檔。quest/ 是來源、quest-dialogue.json 是產生物，
            # 所以不必動 _index.json，跑一次 quest-bundle.py 就進得去。
            print(f'  + 新任務 {target}')
            fresh = json.dumps(
                {"_meta": {"quest": rows[0][1]["_quest"], "count": 0, "translated": 0,
                           "note": "一個任務一個檔。改完跑 tools/quest-bundle.py。"},
                 "entries": {}}, ensure_ascii=False, indent=1) + "\n"
            # 預覽時只在記憶體裡擺一份：跑一次沒加 --write 就在倉庫裡多出
            # 一堆空任務檔，那比沒分類還糟。
            if write:
                path.write_text(fresh, encoding="utf-8", newline="\n")
            data = json.loads(fresh)
        else:
            data = json.loads(path.read_text(encoding="utf-8"))
        flat = "entries" not in data
        print(f"  {target}  +{len(rows)} 條")
        for key, entry in rows[:5]:
            print(f"      {entry['src'][:64]!r}")
        if len(rows) > 5:
            print(f"      …另外 {len(rows) - 5} 條")
        if target.startswith("quest/"):
            # 照收集順序排：同一段劇情的台詞本來就是照 seq 進來的
            rows.sort(key=lambda row: row[1].get("seq", 0))
            quest = data["_meta"].get("quest") or rows[0][1]["_quest"]
            used = len(data["entries"])
            for offset, (_, entry) in enumerate(rows):
                data["entries"][f"{quest}#c{used + offset:03d}"] = {
                    "src": entry["src"], "dst": "",
                    "role": entry.get("role", "desc"),
                    "kind": "dialogue",
                    "quest": quest,
                    "stage": "",
                    "speaker": entry.get("_speaker", ""),
                    "source": "captured",
                }
            data["_meta"]["count"] = len(data["entries"])
        elif flat:
            for _, entry in rows:
                data[entry["src"]] = ""
        else:
            for key, entry in rows:
                data["entries"][key] = {
                    "src": entry["src"], "dst": "",
                    "role": entry.get("role", "desc"),
                    "kind": entry.get("kind") or "sentence",
                    "ctx": [entry.get("ctx", "captured")],
                    "from": "captured",
                }
        total += len(rows)
        if write:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                            encoding="utf-8", newline="\n")

    print()
    print(f"新增 {total} 條，跳過 {skipped} 條（語料已有或不值得收）"
          + ("" if write else "（預覽，加 --write 才寫回）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
