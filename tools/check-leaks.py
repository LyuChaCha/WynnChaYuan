#!/usr/bin/env python3
"""語料裡有沒有夾帶別人的名字。

為什麼三道濾網之外還要這一道
----------------------------
模組端、收集站、匯入工具三道濾網擋的都是「<b>要進來</b>的東西」。但濾網是
一路補出來的——每補一次，就表示在那之前有東西穿了過去，而<b>穿過去的那些
已經在倉庫裡了</b>，沒有任何檢查會回頭看它們一眼。

實際掃過一次，公開倉庫裡躺著七條：

    {#} {#}Thank Changa Flavour
    {#} {#}Changa Flavour has thrown a
    ✔ Reisen Plank has opted in
    ✔ {~} dmg Guardian has opted in
    Leader: player{~}
    Hyedam_{~}
    {#} HEYAZero would like to trade! …

七條的譯文都是空的——沒有人會去翻它們，也就永遠不會有人發現。

所以這一道看的是<b>已經在倉庫裡</b>的東西。濾網管入口，這裡管存量。

名字沒有形狀，所以還有一條不看形狀的
------------------------------------
上面那些認的都是「遊戲寫死的模板 + 別人的名字」。玩家自己取的寵物、坐騎與飾品名
沒有模板可以靠——`Woopie` 跟 `Grume` 在結構上分不出來。那一條改看三個特徵同時
成立：鍵是「一個名字 + ` {#}{#}`」、譯文空白、而且<b>只有一個語言有</b>。
見 {@link LONE_PLATE}。它只報不刪——官方內容跟玩家寵物的差別只有人查得出來。

用法：
    python tools/check-leaks.py             # 掃過就好
    python tools/check-leaks.py --write     # 直接刪掉掃到的（單名名牌那條不動）
    python tools/check-leaks.py --selftest  # 規則的正例與反例，改規則之後跑
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations"

# 只收<b>句型</b>，不收名字。
#
# 名字沒有形狀可以認——`Changa Flavour` 跟 `Cloud Tavern` 在結構上分不出來。
# 但「某某 has opted in」這種句子是遊戲寫死的模板，主詞永遠是別人，
# 而且<b>翻了也沒有用</b>：那一行每個人看到的都不一樣。
#
# 跟模組端的 PlayerDataFilter、收集站的 LOOKS_PERSONAL、匯入工具的 NAMED
# 是同一批句型。四邊都要有，因為四邊管的是不同的時機。
SHAPES = [
    (r"\shas thrown a", "誰丟了炸彈"),
    # 炸彈不只 Loot 一種：Profession Speed、Profession Experience、Combat
    # Experience…，每一種到期都一句廣播，主詞永遠是丟炸彈的那個人。
    (r"\bBomb has expired", "誰的炸彈到期了"),
    # 全服開箱廣播：「某某 has gotten a Chocolate Wybel from their crate!」
    # 名字跟折行位置都會變，認句尾那一段最穩。折行可能落在 from 與 their
    # 之間，也可能落在 their 與 crate 之間，兩個位置都要認得。
    (r"from\s*(?:\n\{#\}\s*)?their\s*(?:\n\{#\}\s*)?crate!", "誰開到了東西"),
    (r"\shad their anatomy refashioned", "誰死了"),
    (r"(?m)^(?:\{#\}\s*)+Thank ", "謝謝某某"),
    (r"\shas opted in", "誰報名了"),
    (r"\shas chosen the", "誰選了增益"),
    (r"\shas given you\s", "誰給了你東西"),
    (r"(?m)^Leader:\s", "隊長是誰"),
    (r"would like to trade", "誰要跟你交易"),
    (r"\sshouts:", "誰在喊話"),
    (r"has logged into server", "誰上線了"),
    # 好友上下線的廣播，主詞永遠是別人的帳號名：`[!] Bob is now online.`。
    # 整行到「is now」之間不會有冒號——有冒號就是誰在說話，而遊戲真的有
    # 這種台詞（討伐戰頭目的開場白「The Mummyboard: OS Version M-37 is now
    # online.」），那是正當的譯文，不能當成夾帶玩家名。
    (r"(?m)^[^:\n]*?\sis now (?:online|offline)", "誰上下線了"),
    (r"(?m)^(?:\{#\})*\S+ has died", "誰死了"),
    (r"['’]s Totem of Tales", "誰的石碑"),
    # 「某某的怪物圖騰到期了」。這一句有兩種版本：`{u}'s mob totem` 是
    # 已經匿名化的（上面的 ALLOW 會放行），帶真名的那一種要擋。
    (r"['’]s Mob Totem\b", "誰的怪物圖騰"),
    # 住宅島的訪客通知。主詞是別人的帳號名，而且那一行每個島主看到的都不一樣。
    (r"\sis visiting this island", "誰來參觀你的島"),
    (r"\binvited .+ into your island", "你邀了誰進島"),
    # 無名異常體的死亡廣播。跟其他討伐戰死法同一類，只是動詞不一樣。
    (r"\shad their existence effaced", "誰被抹除了"),
    (r"\bControlled by\b", "誰的公會佔著這塊地"),
    (r"(?m)^Owned by\s", "這東西是誰的公會的"),
    # 交易所的篩選器把玩家<b>自己打的搜尋字</b>顯示出來，而且是邊打邊更新，
    # 所以「dark e」這種打到一半的也會收進來。翻了沒有意義，變體無限多。
    (r"(?m)^-?\s*Name Contains:\s", "玩家在交易所打的搜尋字"),
    # 玩家打的公會招人廣告：「TW Guild ! DM Owner or Chiefs」。遊戲自己的字
    # 不會叫你去私訊會長或幹部。
    (r"\bDM (?:the )?(?:Owner|Chiefs?)\b", "公會招人廣告"),
    # 討伐戰與掠奪的死亡廣播。每一種死法各一句模板，主詞永遠是別人。
    (r"\swas devoured by\b", "誰被吞了"),
    (r"\shad their skull shattered\b", "誰被打爆頭了"),
    (r"\smet their demise\b", "誰死了"),
    (r"\smet their fate\b", "誰死了"),
    (r"\shas perished\b", "誰死了"),
    (r"\sis preparing to descend", "誰要下去討伐戰了"),
    # 各地區的死亡廣播，一種死法一句模板，主詞永遠是別人。
    # 註：`was reduced to ashes` 要連 `was` 一起認——語料裡有一句正當的
    # 「has been almost entirely reduced to ashes」，那是地景敘述不是廣播。
    (r"\sbecame another casualty of War", "誰死了"),
    (r"Cleansing fire melted .+ away", "誰死了"),
    (r"\swas entirely corrupted\b", "誰死了"),
    (r"\swas reduced to ashes\b", "誰死了"),
    (r"\swas instantly evaporated\b", "誰死了"),
    (r"\sceased to be anything human\b", "誰死了"),
    (r"\swas buried beneath earth\b", "誰死了"),
    (r"\smade a fatal mistake\b", "誰死了"),
    # 一般地圖上的死亡快訊。Wynncraft 每種怪各有一句俏皮的死法模板，
    # 主詞永遠是死掉的那個玩家，2026-09-12 那批就漏了五句進來。
    (r"\sfell victim to\b", "誰死了"),
    (r"\swas clobbered by\b", "誰死了"),
    (r"\swas bashed into paste by\b", "誰死了"),
    (r"\swas impaled by\b", "誰死了"),
    (r"\swas silenced by\b", "誰死了"),
    (r"\swas stomped by\b", "誰死了"),
    (r"\swas weaker than\b", "誰死了"),
    (r"took a shortcut to death's door", "誰死了"),
    (r"There's only ashes where\b", "誰死了"),
    (r"-- oops, other way around", "誰被怪物殺了"),
    # 2026-09-14 那批收件匣又冒出六種死法，主詞一樣是死掉的玩家。
    (r"\sfailed to evade\b", "誰死了"),
    (r"\swas shot down by\b", "誰死了"),
    (r"\swas de-animated by\b", "誰死了"),
    (r"\sbecame swiss cheese\b", "誰死了"),
    (r"\sto thank for their death\b", "誰死了"),
    (r"\sgot a new skull piercing\b", "誰死了"),
    # 通用的那一條：行首一個字黏著 {~}、空一格接小寫動詞——
    # 「Sparkl{~} failed…」「zhanhua{~} was…」。帳號名尾巴的數字收集時變成 {~}，
    # 遊戲自己的句子不會用「單字{~}」當主語開頭。
    (r"(?m)^(?:\{#\}\s*)*[A-Za-z]{2,}\{~\}\s+[a-z]", "帳號名開頭的廣播"),
    # 升等廣播。主詞是別人的帳號名；自己的那一句會被匿名成 {u}，上面的
    # ALLOW 會放行。
    (r"\sis now combat level", "誰升等了"),
    # 公會大廳／住宅島的招牌：「Smol Rat's HQ」。撇號前面是玩家或公會取的名字。
    (r"['’]s HQ\b", "誰的大廳"),
    (r"[A-Za-z]{2,}_\{~\}", "帳號名裡的底線加數字"),
    # 玩家把寵物或物品改成自己的名字：`Tomzd{~}'s Bird`、`{~}Seele's gift to …`。
    # 帳號名裡的數字收集時變成 {~}，數字在前在後都有。遊戲自己的字串不會
    # 出現「單字黏著一個 {~} 再接 's」——`Monte's Village` 沒有 {~}，不會中。
    (r"(?:[A-Za-z]{2,}\{~\}|\{~\}[A-Za-z]{2,})'s\s", "拿帳號名當寵物或物品名"),
]

# 遊戲自己的說明裡會出現這些句型，但帶的是<b>字面的佔位符</b>而不是真名。
#
#   {#} Use "/trade <name>" to safely trade items with other players!
#
# 那是該收的內容——教學訊息，每個人看到的都一樣。
ALLOW = ("<name>", "{u}")

# 公會大廳的立牌與改過名字的物品
# ------------------------------
# 上面那些句型認的是「遊戲寫死的模板 + 別人的名字」。公會大廳是另一回事：
# 整段字都是玩家自己打的。實際掃到的長這樣（下面用 ⏎ 代表換行）：
#
#     Teleporter ⏎ to afk bata
#     Undead Heart ⏎ noki's heart
#     Coconut Ring ⏎ from zxfire
#     Uchouten Tea House's HQ ⏎ by Uchouten Tea House
#
# 第一行是遊戲真的有的東西（傳送點、某件物品），第二行是玩家取的名字——
# 常常直接就是某個人的 ID。這種東西翻了也沒有意義：每個公會的立牌都不一樣。
SIGN_SHAPES = [
    (r"(?s)\A(?:\{#\})*Teleporter\nto .", "公會大廳的傳送立牌"),
    (r"(?m)^by [A-Z]", "立牌上的「by 某某」"),
    # 改過名字的未鑑定裝備。第一行是遊戲的名字（`{#}{#}Unidentified Wand`），
    # 第二行是玩家自己打的字（`cutter of melons #{~}`）。下面的 player_sign
    # 抓不到它——`Unidentified Wand` 不在物品語料裡，那是個泛用名。
    # 但遊戲不會在未鑑定裝備的名字底下再放一行，有第二行就是玩家改的。
    (r"\A(?:\{#\})*Unidentified [A-Z][a-z]+\n\S", "改過名字的未鑑定裝備"),
    # 自製物品的名字是遊戲產的，一定是 Title Case
    # （`Nimble Food of Quick Roasting [3/3]`）。開頭是小寫就是玩家改的
    # （`anni mr mana regen [1/3]`、`gxp cxp gathering combat xp [2/3]`）。
    #
    # 「句號加空格」收尾的不是名字，是被折行切碎的句子碎片——討伐戰計分板
    # 的目標列會碎成 `tower. [{~}/{~}]`、`area. [{~}/{~}]` 這種形狀，跟玩家
    # 改的名字一字不差地撞上這條（2026-10-01 的收件匣實際踩到）。自製物品
    # 的名字是專有名詞，永遠不會帶句號，所以拿它當分界。
    (r"\A(?:\{#\})*[a-z][^{}\[\]\n]*(?<!\.) \[\{~\}/\{~\}\]", "改過名字的自製物品"),
    # 掛單卡上的自製裝備名。`[~數值]` 是自製品的擲骰標記，官方掉落品不會有；
    # 前面那五個圖示是名字列的排版。名字有兩種來源，兩種都不該收：遊戲按材料
    # 自動組出來的（`Menacing Blade of Rage`，組合是無限多的），以及掛單的人
    # 自己改的（`NEED Mythic`、`super ascend`、`catgirl shoes`）。
    #
    # 上面那條只認得開頭小寫的，Title Case 的一路漏進來：掃過一次，公開倉庫
    # 裡躺著 155 條，譯文全部等於原文——沒有人翻得動，也就永遠不會有人發現。
    #
    # 只認名字列。同一張卡上的**詞條行**（`Life Steal {#}+{~}/{~}s [~{~}]`）
    # 是遊戲的字串，該翻也翻好了，而那一行不是五個圖示開頭。
    (r"\A(?:\{#\}){5}[^\n]+ \[~\{~\}\]\Z", "掛單卡上的自製裝備名"),
    # 別的模組印在聊天室的東西。WynnAspects 每一則都以 ›› 開頭。
    (r"››", "第三方模組的聊天訊息"),
    # 隊伍計分板那一欄的隊友：「- [||{~}||] PoorChaC [{~}]」。名字被欄寬
    # 截斷、數字又先變成 {~}，靠字形完全認不出來——一份實機 captured.json
    # 裡出現次數最高的前十一條全是這個。認那一列的排版就準。
    (r"(?m)^\s*-\s*\[\|+", "隊伍計分板上的隊友"),
    # 同一欄沒有血條的那種：「- {~}jimmy」。要求整列只有那一個詞，
    # 不然會連「- {~}x 松木板」那種數量列一起擋掉（量過是一百多條）。
    (r"(?m)^\s*-\s*\{~\}[A-Za-z_][A-Za-z0-9_]*\s*$", "隊伍計分板上的隊友"),
    # 交易、公會倉庫與隊伍收送：「{~}ay_joker → PoorChaCha: {~} Emeralds」。
    # 箭頭的一邊（常常兩邊）就是玩家 ID。共通點是箭頭加上至少一個佔位符，
    # 量過整份語料，七萬多條有譯文的條目一條都不會被擋到。
    (r"(?m)^[^\n]*→[^\n]*\{[#~pu]\d?\}[^\n]*$", "帶箭頭的交易／收送紀錄"),
    (r"(?m)^[^\n]*\{[#~pu]\d?\}[^\n]*→[^\n]*$", "帶箭頭的交易／收送紀錄"),
]

# 第二行是模板的標記。有任何一個就不是玩家打的字——遊戲自己的第二行一定
# 帶 {#} 圖示、✔ 勾、✫ 星等或進度條。
#
# {~} 不算：玩家取的名字裡也會有數字（"insu spell 5 times"）。
TEMPLATE_MARKS = ("{#}", "✔", "✖", "✫", "[|")


# 帳號名黏著 {~}，而且<b>不在行首</b>
# ----------------------------------
# 上面 SHAPES 裡已經有兩條認這種名字：一條要求它在行首、後面接小寫動詞，
# 一條要求它後面接 `'s`。實際掃過一次，夾在句子中間的一路漏過去：
#
#     - air{~} (Pending...)
#     {#} Key Collector: air{~} has already opened the entrance
#     ♦ … ⭐ {~}yue 🕒{~}h …
#     Hydroxi{~} {#}{#}
#     {~}yue was an easy meal for the Grootslang.
#
# 十條，譯文幾乎都是空的。`Hydroxi{~} {#}{#}` 六個語言都「翻」過了，
# 但譯文就是原文本身——那只是把名字抄了一遍，照樣是別人的名字。
#
# 所以這一條<b>不看位置</b>：三個以上字母直接黏著 {~}、中間沒有空白就算。
# 帳號名尾巴或開頭的數字收集時會變成 {~}，所以兩個方向都要認。
# 兩個字母以內不收——`{~}s`、`{~}k`、`{~}th` 那些是單位與序數。
GLUED = re.compile(r"(?<![A-Za-z])[A-Za-z]{3,}\{~\}|\{~\}[A-Za-z]{3,}(?![A-Za-z])")

# 這幾個不是名字，是真的黏在數字上的字：
#   {~}stx       交易市場的「{~} 組」（stacks）
#   {~}min       分鐘。其他時間單位都在兩個字母以內，GLUED 本來就不會中
#   {~}xpcombat  經驗卷軸的名字
#   LOBBY{~}     大廳伺服器的編號
GLUED_OK = ("{~}stx", "{~}min", "{~}xpcombat", "LOBBY{~}")


def glued_name(src: str) -> str | None:
    """帳號名黏著 {~} —— 上面那兩條因為限定位置而漏掉的那一種。"""
    for match in GLUED.finditer(src):
        token = match.group(0)
        if token in GLUED_OK:
            continue
        # 兩邊都被 {~} 夾著的不是名字，是被切碎的代號（`{~}cbd{~}f`）
        if token.startswith("{~}") and src[match.end():match.end() + 3] == "{~}":
            continue
        return "帳號名黏著 {~}"
    return None


# 只有一個語言有、譯文空白的單名名牌
# ----------------------------------
# 上面每一條認的都是<b>一句話的形狀</b>。玩家自己取的寵物、坐騎與飾品名沒有
# 形狀可以認——`Woopie` 跟 `Grume` 在結構上分不出來：都是一個詞、標題大小寫、
# 後面跟著等級膠囊那兩個圖示。模組端的 {@code looksPlayerNamed} 就是這樣漏掉
# 它們的，那一條靠「不符合標題大小寫」判斷，剛好取成標題大小寫的自訂名一路穿過去。
#
# 2026-10-01 那批 capture 帶進來 21 條這種名牌。#926 的作者當時就寫了「約 20 條
# 單詞人名／寵物名無法確認是 NPC 還是玩家寵物，刻意留空待翻」——然後它們就留在
# 倉庫裡了，而且譯文是空的，沒有人會去翻，也就永遠不會有人再想起這件事。
# 拿去官方 wiki 對過：二十條一條都查不到，只有 `Liff` 是真的怪物（Auburn Forest
# 的 105 級）。
#
# 一條一條看字形沒有出路。但這些條目有三個<b>互相獨立</b>的特徵同時成立：
#
#   1. 鍵的形狀就是「一個名字 + ` {#}{#}`」——只有一行、沒有別的詞、沒有佔位符
#   2. 譯文是空的
#   3. 只出現在<b>一個</b>語言的同名檔案裡
#
# 第三條是關鍵。官方的怪物名牌每個語言的 capture 都收得到——`Cursed Shrieker
# {#}{#}` 六個語言都有、六個都還沒翻，這一條不會報它；而某個玩家的寵物只會出現在
# 剛好跟他同隊的那一個人的 capture 裡。第一條單獨看會中一大片官方怪物
# （`Drowsy Wybel {#}{#}`），要三條一起才收斂。
#
# <b>這一條不進 --write。</b>官方內容跟玩家寵物的差別只有人查得出來，照形狀刪會
# 把 `Liff` 一起刪掉。報出來有兩條出路，都該由人決定：查到是官方內容就補上譯文
# （補了就不符合第 2 條，不會再報），查不到就刪掉那一條。
LONE_PLATE = re.compile(r"\A[A-Za-z][A-Za-z.'’ -]*[A-Za-z.] \{#\}\{#\}\Z")


def lone_plate_keys(
        corpus: dict[str, dict[str, dict[str, str]]]) -> set[tuple[str, str, str]]:
    """哪些條目同時符合上面那三個條件。

    :param corpus: 語言 → 檔名 → 鍵 → 譯文
    :return: {(語言, 檔名, 鍵)}
    """
    owners: dict[tuple[str, str], int] = {}        # (檔名, 鍵) → 幾個語言有
    for files in corpus.values():
        for rel, pairs in files.items():
            for key in pairs:
                owners[(rel, key)] = owners.get((rel, key), 0) + 1

    out: set[tuple[str, str, str]] = set()
    for lang, files in corpus.items():
        for rel, pairs in files.items():
            for key, dst in pairs.items():
                if dst or not LONE_PLATE.match(key):
                    continue
                if owners[(rel, key)] == 1:
                    out.add((lang, rel, key))
    return out


def dst_rows(path: Path):
    """檔案裡的 (鍵, 譯文)。{@link rows} 給的是原文，這一條規則看的是譯文。"""
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return
    entries = data.get("entries") if isinstance(data, dict) else None
    if isinstance(entries, dict):
        for key, value in entries.items():
            if isinstance(value, dict):
                yield key, value.get("dst") or ""
        return
    if not isinstance(data, dict):
        return
    for key, value in data.items():
        if not key.startswith("_") and isinstance(value, str):
            yield key, value


def lone_plates() -> dict[Path, set[str]]:
    """掃過整份語料，回傳 檔案 → 要報出來的鍵。"""
    corpus: dict[str, dict[str, dict[str, str]]] = {}
    paths: dict[tuple[str, str], Path] = {}
    for lang_dir in sorted(p for p in TRANSLATIONS.iterdir() if p.is_dir()):
        for path in sorted(lang_dir.rglob("*.json")):
            if path.name.startswith("_"):
                continue
            rel = path.relative_to(lang_dir).as_posix()
            paths[(lang_dir.name, rel)] = path
            corpus.setdefault(lang_dir.name, {})[rel] = dict(dst_rows(path))

    out: dict[Path, set[str]] = {}
    for lang, rel, key in lone_plate_keys(corpus):
        out.setdefault(paths[(lang, rel)], set()).add(key)
    return out


def player_sign(src: str, items: set) -> str | None:
    """第一行是遊戲的物品名、其餘是玩家自己打的字 —— 回傳說明，否則 None。"""
    if "\n" not in src:
        return None
    head, rest = src.split("\n", 1)
    if not rest.strip() or any(mark in rest for mark in TEMPLATE_MARKS):
        return None
    base = re.sub(r"\s*\[\{~\}/\{~\}\]$", "", head).strip()
    return "改過名字的物品" if base in items else None


def known_items() -> set:
    """語料裡已有的物品名，拿來認立牌的第一行。"""
    names: set[str] = set()
    for name in ("gear-weapon.json", "gear-armour.json", "gear-accessory.json",
                 "material.json", "ingredient.json", "tome.json", "charm.json",
                 "aspect.json"):
        path = TRANSLATIONS / "zh_tw" / name
        if not path.exists():
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        entries = data.get("entries")
        if isinstance(entries, dict):
            for value in entries.values():
                if isinstance(value, dict) and isinstance(value.get("src"), str):
                    names.add(value["src"])
        else:
            names.update(k for k in data if not k.startswith("_"))
    return names


def rows(path: Path):
    """檔案裡的 (鍵, 原文)。兩種格式都認。"""
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return
    entries = data.get("entries")
    if isinstance(entries, dict):
        for key, value in entries.items():
            if isinstance(value, dict) and isinstance(value.get("src"), str):
                yield key, value["src"]
        return
    for key, value in data.items():
        if not key.startswith("_") and isinstance(value, str):
            yield key, key


def drop(path: Path, keys: set[str]) -> int:
    """把掃到的那幾條從檔案裡拿掉。"""
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = data.get("entries")
    gone = 0
    if isinstance(entries, dict):
        for key in list(entries):
            if key in keys:
                del entries[key]
                gone += 1
        meta = data.get("_meta")
        if isinstance(meta, dict) and "count" in meta:
            meta["count"] = len(entries)
    else:
        for key in list(data):
            if key in keys:
                del data[key]
                gone += 1
    if gone:
        path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n",
                        encoding="utf-8", newline="\n")
    return gone


# --------------------------------------------------------------------------
# 自我檢查

def selftest() -> int:
    """`python tools/check-leaks.py --selftest`

    這條新規則是「放寬」而不是「收緊」——上面兩條都限定位置，這一條不看位置。
    放寬的規則出錯的方式是誤殺：把 `{~}stx`、`{~}min` 之類真的黏在數字上的字
    當成人名，`--write` 一跑就把譯文刪掉，而且 git 不會報衝突，譯文就永久
    消失了。所以正例與反例都要留下來能重跑。

    正例是 #901 實際清掉的那幾條原文。
    """
    bad = 0

    def check(src: str, want: bool) -> None:
        nonlocal bad
        got = glued_name(src) is not None
        ok = got == want
        print(("  [PASS] " if ok else "  [FAIL] ")
              + ("該抓到" if want else "不該抓到") + "：" + src[:64]
              + ("" if ok else "（實際%s抓到）" % ("" if got else "沒")))
        bad += 0 if ok else 1

    # 要抓到 —— 名字夾在句子中間，上面兩條限定位置的規則都漏過去
    for src in (
            "- air{~} (Pending...)",
            "{#} Key Collector: air{~} has already opened the entrance",
            "♦ Guild Raid ⭐ {~}yue 🕒{~}h ago",
            "Hydroxi{~} {#}{#}",
            "{~}yue was an easy meal for the Grootslang.",
            "Tomzd{~} has joined your party",
    ):
        check(src, True)

    # 不該抓到 —— 真的黏在數字上的字
    for src in (
            "{~}stx",                      # 交易市場的「{~} 組」
            "Sell for {~}stx to the market",
            "{~}min",                      # 分鐘
            "{~}xpcombat",                 # 經驗卷軸的名字
            "LOBBY{~}",                    # 大廳伺服器編號
            "+{~}s to your Timer",         # 秒，兩個字母以內
            "{~}nd attempt",               # 序數
            "{~}cbd{~}f",                  # 被切碎的代號，兩邊都被 {~} 夾著
            "Talk to Ormrod in the {p}",   # 完全沒有 {~}
    ):
        check(src, False)

    # 「改過名字的自製物品」那一條：句號收尾的是折行碎片，不是名字
    crafted = next(p for p, why in SIGN_SHAPES if why == "改過名字的自製物品")
    crafted_re = re.compile(crafted)

    def check_crafted(src: str, want: bool) -> None:
        nonlocal bad
        got = crafted_re.search(src) is not None
        ok = got == want
        print(("  [PASS] " if ok else "  [FAIL] ")
              + ("該抓到" if want else "不該抓到") + "：" + src[:64]
              + ("" if ok else "（實際%s抓到）" % ("" if got else "沒")))
        bad += 0 if ok else 1

    # 要抓到 —— 玩家改的小寫開頭名字
    for src in (
            "anni mr mana regen [{~}/{~}]",
            "gxp cxp gathering combat xp [{~}/{~}]",
    ):
        check_crafted(src, True)

    # 不該抓到 —— 討伐戰計分板目標的折行碎片（句號收尾）
    for src in (
            "tower. [{~}/{~}]",
            "area. [{~}/{~}]",
    ):
        check_crafted(src, False)

    # 單名名牌那一條。形狀（LONE_PLATE）單獨看會中一大片官方怪物，所以要連
    # 「譯文空白」與「只有一個語言有」一起測——收斂全靠那兩條。
    def check_plate(src: str, want: bool) -> None:
        nonlocal bad
        got = LONE_PLATE.match(src) is not None
        ok = got == want
        print(("  [PASS] " if ok else "  [FAIL] ")
              + ("是名牌的形狀" if want else "不是名牌的形狀") + "："
              + json.dumps(src, ensure_ascii=False)[:66]
              + ("" if ok else "（實際%s中）" % ("" if got else "沒")))
        bad += 0 if ok else 1

    # 是這個形狀 —— 包含官方怪物，形狀本身不區分
    for src in ("Woopie {#}{#}", "K. Rool {#}{#}", "Wee Woo {#}{#}",
                "BBANG {#}{#}", "Liff {#}{#}", "Drowsy Wybel {#}{#}"):
        check_plate(src, True)

    # 不是這個形狀
    for src in (
            "{p} {#}{#}",                              # 地名佔位符，不是名字
            "{#} Blinders {#}{#}",                     # 名字前面還有圖示
            "{#}{#}",                                  # 只有圖示
            "Smidgen {#}{#}\n{#}\n{#} Distorted {~}s",  # 多行
            "Toxic Puddle - {~}❤",                     # 不是名牌模板
            "News Vendor",                             # NPC 名牌，沒有等級膠囊
            "Hydroxi{~} {#}{#}",                       # 帳號名黏著 {~}，GLUED 那條管
    ):
        check_plate(src, False)

    def check_lone(why: str, corpus: dict, want: set) -> None:
        nonlocal bad
        got = {(lang, rel, key) for lang, rel, key in lone_plate_keys(corpus)}
        ok = got == want
        print(("  [PASS] " if ok else "  [FAIL] ") + why
              + ("" if ok else f"（實際 {sorted(got)}，預期 {sorted(want)}）"))
        bad += 0 if ok else 1

    # 玩家的寵物：只有一個語言有，譯文空白 —— 要報
    check_lone(
        "只有一個語言有、譯文空白 → 報",
        {"zh_tw": {"label.json": {"Woopie {#}{#}": ""}},
         "zh_cn": {"label.json": {}}},
        {("zh_tw", "label.json", "Woopie {#}{#}")})

    # 官方怪物：好幾個語言的 capture 都收到，都還沒翻 —— 不報
    check_lone(
        "多個語言都有（都還沒翻）→ 不報",
        {"zh_tw": {"npc.json": {"Cursed Shrieker {#}{#}": ""}},
         "ja_jp": {"npc.json": {"Cursed Shrieker {#}{#}": ""}}},
        set())

    # 補上譯文就不再報 —— 這是「查到是官方內容」那條出路
    check_lone(
        "有譯文 → 不報",
        {"zh_tw": {"label.json": {"Liff {#}{#}": "Liff {#}{#}"}},
         "zh_cn": {"label.json": {}}},
        set())

    # 同一個鍵在不同檔案分開算：npc.json 的那一條兩個語言都有，不報；
    # label.json 的那一條只有 zh_tw 有，要報
    check_lone(
        "不同檔案分開算",
        {"zh_tw": {"label.json": {"Grume {#}{#}": ""},
                   "npc.json": {"Grume {#}{#}": ""}},
         "ja_jp": {"npc.json": {"Grume {#}{#}": ""}}},
        {("zh_tw", "label.json", "Grume {#}{#}")})

    print("\n" + ("自我檢查全過。" if not bad else f"有 {bad} 項不對。"))
    return 1 if bad else 0


def main(argv: list[str]) -> int:
    if "--selftest" in argv:
        return selftest()
    write = "--write" in argv
    compiled = [(re.compile(p), why) for p, why in SHAPES + SIGN_SHAPES]
    items = known_items()

    found: dict[Path, set[str]] = {}
    total = 0
    for path in sorted(TRANSLATIONS.rglob("*.json")):
        if path.name.startswith("_"):
            continue
        for key, src in rows(path):
            if any(a in src for a in ALLOW):
                continue
            why = next((w for p, w in compiled if p.search(src)), None)
            if why is None:
                why = player_sign(src, items)
            if why is None:
                why = glued_name(src)
            if why:
                rel = path.relative_to(TRANSLATIONS).as_posix()
                print(f"  [{rel}] {why}")
                print(f"      {json.dumps(src, ensure_ascii=False)[:100]}")
                found.setdefault(path, set()).add(key)
                total += 1

    # 「只有一個語言有、譯文空白的單名名牌」。刻意不併進 found——這一條要人
    # 查過才能決定是補譯文還是刪掉，--write 幫不上忙。
    lone = lone_plates()
    lonely = sum(len(keys) for keys in lone.values())
    for path, keys in sorted(lone.items()):
        rel = path.relative_to(TRANSLATIONS).as_posix()
        for key in sorted(keys):
            print(f"  [{rel}] 只有這個語言有、譯文空白的單名名牌")
            print(f"      {json.dumps(key, ensure_ascii=False)[:100]}")

    if not total and not lonely:
        print("語料裡沒有夾帶別人的名字。")
        return 0

    if total:
        print(f"\n掃到 {total} 條夾帶玩家名的條目。")
        if not write:
            print("跑 python tools/check-leaks.py --write 刪掉它們。")

    if lonely:
        print(f"\n掃到 {lonely} 條只有一個語言有、譯文空白的單名名牌。")
        print("拿去官方 wiki 查：查得到就把譯文補上，查不到（是玩家自己取的寵物、")
        print("坐騎或飾品名）就把那一條刪掉。這一條不能用 --write 代勞——官方內容")
        print("跟玩家寵物的差別只有人查得出來。")

    if not write:
        return 1

    gone = 0
    for path, keys in found.items():
        gone += drop(path, keys)
    print(f"\n刪掉 {gone} 條。")
    return 1 if lonely else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
