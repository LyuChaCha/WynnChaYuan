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
    r"\bCrafted by\b|\bParty\b.*\binvit"
    # 「某某的狀態」也是別人的資料：`YuChaYuan's Status`。
    # 注意<b>不能</b>把所有 `'s` 都擋掉——`Monte's Village` 是遊戲裡的領地名。
    r"|'s (?i:party|guild|island|house|status)\b"
    # 領地名牌：「Controlled by Paladins United [Lv. 32]」。公會名稱跟玩家名稱
    # 一樣是別人的資料。它沒有底線，所以下面那條「帳號名的形狀」抓不到——
    # 一次 Lootrun 就有 83 條這樣穿了過去。
    r"|\bControlled by\b"
    # 公會標籤 `[YCY]`、`[SEQ]`、`[CTRN]`——三到四個大寫字母的方括號。
    #
    # 這是公會資料最穩定的形狀，公會名本身反而抓不到（`JFZN JAPAN`、
    # `Cloud Tavern` 跟一般英文字沒兩樣）。認標籤就連帶擋掉了整行：
    # `- Cloud Tavern [CTRN]`、`✔ Herb Cave ({~}) [SEQ]`、
    # `… taken control of Apprentice Huts from [SEQ]!`。
    #
    # 代價是那幾行<b>介面字串</b>也跟著收不進來。刻意的：語料是公開的，
    # 別人的公會名一旦進去就洗不掉，少翻一行領地清單便宜得多。
    r"|\[[A-Z]{3,4}\]"
    # 公會欄位：`- Guild: 某公會`。公會名本身抓不到，但欄位標籤抓得到。
    #
    # 註：這裡不能寫 \b——在雙引號字串裡 \b 是<b>退格字元</b>，
    # 先前就是這樣寫成了 `|\x08Guild:` 而永遠不會命中。
    r"|Guild:\s"
    # 裸帳號名：整條就是一個詞，而且<b>小寫後面接大寫</b>
    #（`PoorChaCha`、`YuanYoIn`、`ChangJenChief`）。
    #
    # 這是帳號名最穩定的形狀。全大寫的縮寫（`DPS`、`INGREDIENT`）不符合，
    # 因為要求前面是小寫。裝備名（`StoneWall`、`HellRaiser`）會被誤擋，
    # 但那些是從官方 CDN 進語料的、不走這個工具，所以沒有損失。
    r"|^(?:\{#\})*[A-Za-z][a-z0-9]*[a-z][A-Z][A-Za-z0-9]*$"
    # 組隊系統會把玩家名字嵌進句子裡，那些句子的<b>結構</b>是固定的：
    #   `{#} Party Finder: Hey 某人, over here! ...`
    #   `- [{~}] 某人`（隊伍成員清單）
    #
    # 不能改用「句中任一個駝峰詞」去抓——實測會誤傷 `WynnExcavation`（15 條
    # 裝備 lore）、`HellRaiser`、`PvP`，以及喊叫的台詞 `HehehaHAAAAA`、
    # `GwaaAAAAAH`。認句型比認名字準得多。
    r"|Party Finder: Hey\s"
    r"|^-\s\[\{~\}\]\s\S+$"
    # 同一類：隊長欄位與收禮通知，名字都黏在固定句型上。
    r"|^Leader:\s"
    r"|\shas given you\s"
    # 討伐戰的公告同樣把名字黏在固定句型上。
    r"|\shas opted in"
    r"|\shas chosen the"
    # 死亡訊息：`PoorChaCha is six feet under`。句型是遊戲寫死的，
    # 名字一定在最前面，認句尾那一段最穩。
    r"|\sis six feet under"
    # 公會／玩家名牌後面接等級：`YuChaYuan [Lv. 106]`。
    r"|^(?:\{#\})*[A-Za-z][a-z0-9]*[a-z][A-Z][A-Za-z0-9]*\s\[Lv\."
    # 名牌後面只接圖示：`ImagineKami {#}{#}`。
    r"|^(?:\{#\})*[A-Za-z][a-z0-9]*[a-z][A-Z][A-Za-z0-9]*\s(?:\{#\})+$"
    # 帳號名的數字會先被抽成佔位符，`FengLingYu_1234` 變成 `FengLingYu_{~}`，
    # 剛好躲過下面那條「底線後面至少兩個字元」的規則。
    r"|[A-Za-z]{2,}_\{~\}"
    # 交易邀請一句夾兩個名字。
    r"|would like to trade"
    r"|/trade\s"
    # 島嶼進出的廣播與分隔線。
    r"|\sleft this island"
    r"|'s Side"
    # 玩家名牌：`{#}PeeYan Hunter`——帳號名後面接職業。
    # 上面那條「整條就是一個帳號名」要求整條到底，接了職業就漏掉。
    r"|^(?:\{#\})*[A-Za-z][a-z0-9]*[a-z][A-Z][A-Za-z0-9]*\s"
    r"(?:Warrior|Mage|Archer|Assassin|Shaman|Knight|Ninja|Hunter|Skyseer|Dark Wizard)$"
    # 公會階級名牌：`YuChaYuan\n< Season 24 - Platinum >`、`YuChaYuan\n< Traders >`。
    # 名字在第一行、階級用角括號包在第二行——認角括號那一段就夠。
    r"|\n\s*<[^>]*>"
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


def merge_dialogue(quest: str, existing: dict, fresh: list[dict],
                   session: list[str]) -> dict:
    """把新台詞插進<b>劇情順序</b>裡，然後整份重新編號。

    <h2>先前是怎麼放的</h2>
    新條目一律<b>接在檔尾</b>，編號是 {@code 任務名#c012} 這種另起一套的流水號。
    於是同一段對話被拆成兩截：前面是照劇情排好的，後面是這次補進來的一坨，
    順序完全對不上。翻譯的人得先自己拼回先後才知道在講什麼。

    <h2>怎麼知道該插在哪</h2>
    靠<b>錨點</b>。這次在遊戲裡跑到的台詞裡，有一部分語料早就有了——那些被跳過，
    但它們的順序告訴我們新台詞的位置：一句新台詞如果出現在已知的第 12 句之後、
    第 13 句之前，它就該插在檔案裡那兩句中間。

    <p>跑到的順序裡完全沒有錨點的（整段對話都是新的），就照原順序放在最前面——
    那代表它們發生在目前檔案裡所有台詞之前，或者這是個全新的任務檔。

    <h2>為什麼要整份重新編號</h2>
    插進中間就得有個號碼。與其用 {@code #012a} 這種補丁編號，不如整份重排成
    {@code #000..#NNN}——翻譯的人一眼就看得出這是第幾句，而編號只是識別，
    查表走的是 src。
    """
    old = list(existing.items())
    at = {}
    for i, (_, entry) in enumerate(old):
        at.setdefault(entry.get("src", ""), i)
    pending = {e["src"]: e for e in fresh}

    after: dict[int, list[dict]] = collections.defaultdict(list)
    head: list[dict] = []
    anchor = None
    for src in session:
        if src in at:
            anchor = at[src]
        elif src in pending:
            (after[anchor] if anchor is not None else head).append(pending.pop(src))

    merged = list(head)
    for i, (_, entry) in enumerate(old):
        merged.append(entry)
        merged.extend(after.get(i, []))
    # 這次的順序裡沒出現過的（理論上不會有）放最後，總之不能弄丟
    merged.extend(pending.values())

    return {f"{quest}#{i:03d}": entry for i, entry in enumerate(merged)}


def selftest() -> int:
    """merge_dialogue 的自我檢查：`python tools/import-captured.py --selftest`

    插入位置這種邏輯錯了不會報錯，只會讓對話順序悄悄變得莫名其妙，
    所以放一份能隨時重跑的案例在這裡。
    """
    q = "Cook Assistant"
    e = lambda src, dst="": {"src": src, "dst": dst}
    base = {f"{q}#000": e("A", "甲"), f"{q}#001": e("B", "乙"),
            f"{q}#002": e("C", "丙")}
    order = lambda r: [v["src"] for v in r.values()]
    cases = [
        ("插在兩句已知台詞中間",
         (dict(base), [e("B2")], ["A", "B", "B2", "C"]), ["A", "B", "B2", "C"]),
        ("兩句新的各自插在不同位置",
         (dict(base), [e("A2"), e("C2")], ["A", "A2", "B", "C", "C2"]),
         ["A", "A2", "B", "C", "C2"]),
        ("發生在所有已知台詞之前的放最前面",
         (dict(base), [e("Z")], ["Z", "A", "B", "C"]), ["Z", "A", "B", "C"]),
        ("全新任務照收集順序",
         ({}, [e("P"), e("Q"), e("R")], ["P", "Q", "R"]), ["P", "Q", "R"]),
        ("沒有錨點時一條都不能弄丟",
         (dict(base), [e("X")], []), None),
    ]
    bad = 0
    for what, (old, fresh, session), want in cases:
        got = order(merge_dialogue(q, old, fresh, session))
        ok = sorted(got) == sorted(["A", "B", "C", "X"]) if want is None             else got == want
        print(("  [PASS] " if ok else "  [FAIL] ") + what
              + ("" if ok else f"（實際 {got}）"))
        bad += 0 if ok else 1
    # 既有譯文不能被動到
    kept = [v.get("dst") for v in
            merge_dialogue(q, dict(base), [e("B2")], ["A", "B", "B2", "C"]).values()]
    ok = kept == ["甲", "乙", "", "丙"]
    print(("  [PASS] " if ok else "  [FAIL] ") + "既有譯文原封不動"
          + ("" if ok else f"（實際 {kept}）"))
    bad += 0 if ok else 1
    # 編號要連號，翻譯的人是靠它看順序的
    keys = list(merge_dialogue(q, dict(base), [e("B2")], ["A", "B", "B2", "C"]))
    ok = keys == [f"{q}#{i:03d}" for i in range(4)]
    print(("  [PASS] " if ok else "  [FAIL] ") + "整份重新編號且連號"
          + ("" if ok else f"（實際 {keys}）"))
    bad += 0 if ok else 1
    print("對話插入：" + ("全部通過" if bad == 0 else f"{bad} 項失敗"))
    return 1 if bad else 0


def main(argv: list[str]) -> int:
    if "--selftest" in argv:
        return selftest()
    write = "--write" in argv
    files = [a for a in argv if not a.startswith("--")]
    if not files:
        print(__doc__)
        return 2

    have = existing()
    by_file: dict[str, list[tuple[str, dict]]] = collections.defaultdict(list)
    # 這一次實際跑到的劇情順序，一個任務一份。<b>包含已經翻好的那些</b>——
    # 它們是錨點：新台詞要插在哪裡，只能靠「它前面那句已知的台詞在檔案裡的位置」
    # 推出來。見 merge_dialogue。
    session: dict[str, list[str]] = collections.defaultdict(list)
    skipped = 0
    for name in files:
        data = json.loads(Path(name).read_text(encoding="utf-8"))
        rows_in = sorted(data.get("entries", {}).items(),
                         key=lambda kv: kv[1].get("seq", 0))
        for key, entry in rows_in:
            src = entry.get("src", "")
            known = bool(src) and (src in have or bare(src) in have)
            found = quest_of(entry)
            if found and src and (known or worth_keeping(src)):
                # 已知的也要記進順序，它是錨點
                session[found[0]].append(src)
            if (not src or known
                    or not worth_keeping(src)
                    or already_piecewise(src, have)
                    or names_a_player(src)):
                skipped += 1
                continue
            have.add(src)                      # 同一批裡的重複只收一次
            have.add(bare(src))
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
            fresh = [{
                "src": entry["src"], "dst": "",
                "role": entry.get("role", "desc"),
                "kind": "dialogue",
                "quest": quest,
                "stage": "",
                "speaker": entry.get("_speaker", ""),
                "source": "captured",
            } for _, entry in rows]
            data["entries"] = merge_dialogue(
                quest, data["entries"], fresh, session.get(quest, []))
            data["_meta"]["count"] = len(data["entries"])
            data["_meta"]["translated"] = sum(
                1 for e in data["entries"].values() if str(e.get("dst", "")).strip())
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
