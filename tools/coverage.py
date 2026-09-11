#!/usr/bin/env python3
"""語料涵蓋率：已經收集到的佔全部的多少。

為什麼跟 `update-docs.py` 是兩件事
----------------------------------
進度表回答的是「**收集到的**語料翻了幾成」。它的分母是「我們手上有的句子」
——一句沒被玩家遇到過的台詞，既不在分子也不在分母裡，於是 98.9% 這個數字
會讓人以為只剩 1.1%。

涵蓋率回答的是另一半：「**全部**的語料，我們收到了幾成」。

有官方清單的類別（任務、祕密發現、裝備、技能、材料）算得出真正的分母；
名牌、介面、聊天那些沒有任何公開清單，只能靠玩家跑到才知道——那就<b>老實
寫「沒有清單」</b>，不要用猜的數字充版面。

用法：
    python tools/coverage.py            # 印出來看
    python tools/coverage.py --write    # 寫進 README 等文件的標記之間
    python tools/coverage.py --refresh  # 重新抓官方任務清單（需要網路）
    python tools/coverage.py --check    # CI 用：過期就非零退出
"""

from __future__ import annotations

import glob
import html
import json
import re
import sys
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSLATIONS = ROOT / "src/main/resources/assets/wynnchayuan/translations/zh_tw"
# 官方那兩份清單的快取。放在 tools/ 而不是 raw/，因為 raw/ 是 gitignore 的
# ——涵蓋率要在 CI 裡算得出來，它讀的東西就必須進倉庫。
CACHE = ROOT / "tools/coverage-data.json"
DISCOVERIES = ROOT / "raw/discoveries.json"

START = "<!-- 涵蓋率:開始 -->"
END = "<!-- 涵蓋率:結束 -->"

# 檔案 -> 要寫哪一種語言的表。英文的 README 給不讀中文的譯者看，
# 它需要的是同一份數字、不是同一段中文。
TARGETS = {
    "README.md": "zh",
    "README.en.md": "en",
    "docs/modrinth-description.md": "both",
    "docs/curseforge-description.md": "both",
}

API = "https://wynncraft.wiki.gg/index.php"
PARAMS = {
    "title": "Special:CargoExport",
    "tables": "Quests",
    "fields": "Quests._pageTitle,Quests.name",
    "limit": "2000",
    "format": "json",
}
AGENT = ("WynnChaYuan/dev (translation mod; "
         "+https://github.com/LyuChaCha/WynnChaYuan)")


def normalise(name: str) -> str:
    """wiki 的頁面標題對回任務名。

    <p>兩個差異：wiki 的 Cargo 匯出把單引號寫成 HTML 實體（``&#039;``），
    而同名的地點／道具存在時，任務頁會加上 ``(Quest)`` 後綴。兩個都不處理的話
    比對會有二十幾個假的缺口——實測過。
    """
    name = html.unescape(name or "").strip()
    return re.sub(r"\s*\(Quest\)$", "", name)


def fetch_quests() -> list[str]:
    url = API + "?" + urllib.parse.urlencode(PARAMS)
    req = urllib.request.Request(url, headers={"User-Agent": AGENT})
    with urllib.request.urlopen(req, timeout=30) as response:
        rows = json.load(response)
    names = sorted({normalise(r.get("name") or r.get("_pageTitle"))
                    for r in rows})
    # 「???」是 wiki 上的佔位頁，不是任務。
    return [n for n in names if n and n != "???"]


def refresh() -> int:
    """重新抓官方清單並存成快取。

    <p>祕密發現的總數從 ``raw/discoveries.json`` 讀——抓那一份的工具已經存在
    （``tools/fetch-discoveries.py``），這裡只把<b>數字</b>抄進快取，
    因為 ``raw/`` 是 gitignore 的，CI 讀不到。
    """
    names = fetch_quests()
    secrets = 0
    if DISCOVERIES.is_file():
        data = json.loads(DISCOVERIES.read_text(encoding="utf-8"))
        secrets = len(data.get("entries", {}))
    elif CACHE.is_file():
        secrets = json.loads(CACHE.read_text(encoding="utf-8")).get("secrets", 0)
    CACHE.write_text(json.dumps({
        "_note": "官方清單的快取，用來算語料涵蓋率。由 tools/coverage.py --refresh "
                 "產生：任務來自 wynncraft.wiki.gg 的 Cargo 表，祕密發現的數量"
                 "來自 tools/fetch-discoveries.py 抓下來的 raw/discoveries.json。"
                 "存進倉庫是為了讓涵蓋率在 CI 與離線時也算得出來。",
        "source": "https://wynncraft.wiki.gg/",
        "count": len(names),
        "secrets": secrets,
        "quests": names,
    }, ensure_ascii=False, indent=1) + "\n", encoding="utf-8", newline="\n")
    print(f"任務清單已更新：{len(names)} 個；祕密發現 {secrets} 個")
    return 0


def collected_quests() -> set[str]:
    """已經收到對話的任務。"""
    out = set()
    for path in glob.glob(str(TRANSLATIONS / "quest/*.json")):
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        name = data.get("_meta", {}).get("quest")
        if name:
            out.add(normalise(name))
    return out


def cache() -> dict:
    return json.loads(CACHE.read_text(encoding="utf-8"))


def flat_count(name: str) -> int:
    path = TRANSLATIONS / name
    if not path.is_file():
        return 0
    data = json.loads(path.read_text(encoding="utf-8"))
    return sum(1 for k in data if not k.startswith("_"))


def bar(done: int, total: int) -> str:
    if total <= 0:
        return "—"
    filled = min(10, round(done / total * 10))
    return "█" * filled + "░" * (10 - filled)


def rows() -> list[tuple[str, str, str, str]]:
    """（類別, 進度條, 數字, 說明）。"""
    saved = cache()
    quests = saved["quests"]
    have = collected_quests()
    got = len([q for q in quests if q in have])
    out = [("任務對話", bar(got, len(quests)), f"{got} / {len(quests)} 個任務",
            "官方任務清單（wiki）")]

    secrets = saved.get("secrets", 0)
    stories = len(glob.glob(str(TRANSLATIONS / "secret/*.json")))
    out.append(("祕密發現的故事", bar(stories, secrets),
                f"{stories} / {secrets} 個發現", "官方祕密發現清單（wiki）"))

    # CDN 來的本來就是全的——它不是「收集」來的，是整批下載的。
    for label in ("裝備的傳說敘述", "材料、素材、書卷、Aspect", "技能樹"):
        out.append((label, bar(1, 1), "全部", "官方 CDN，整批下載"))

    out.append(("NPC 名牌、介面、系統訊息", "—",
                f"已收 {flat_count('npc.json'):,} + {flat_count('gui.json'):,} 條",
                "**沒有官方清單**，只能靠玩家遇到"))
    return out


EN = {
    "任務對話": "Quest dialogue",
    "祕密發現的故事": "Secret discovery stories",
    "裝備的傳說敘述": "Gear lore",
    "材料、素材、書卷、Aspect": "Ingredients, materials, tomes, aspects",
    "技能樹": "Ability trees",
    "NPC 名牌、介面、系統訊息": "NPC nameplates, menus, system messages",
    "官方任務清單（wiki）": "Official quest list (wiki)",
    "官方祕密發現清單（wiki）": "Official secret discovery list (wiki)",
    "官方 CDN，整批下載": "Official CDN, downloaded wholesale",
    "**沒有官方清單**，只能靠玩家遇到": "**No official list** - only what players run into",
    "全部": "all",
}


def english(text: str) -> str:
    """表格欄位的英文。查不到就原樣留著——寧可有一格中文，也不要瞎翻。"""
    if text.endswith(" 個任務"):
        return text[:-len(" 個任務")] + " quests"
    if text.endswith(" 個發現"):
        return text[:-len(" 個發現")] + " discoveries"
    if text.startswith("已收 "):
        return text.replace("已收 ", "").replace(" 條", "") + " collected"
    return EN.get(text, text)


def english_block() -> str:
    lines = ["| Category | Collected | Count | Where the denominator comes from |",
             "|---|---|---:|---|"]
    for label, progress, number, note in rows():
        lines.append(f"| {english(label)} | {progress} | {english(number)} "
                     f"| {english(note)} |")
    lines.append("")
    lines.append("Quest dialogue, NPC nameplates and menu text **have no public "
                 "data source** - not in the Wynncraft API, not on Wynntils' CDN. "
                 "They only arrive when a player actually runs into them in game, "
                 "so the last row is an honest blank:")
    lines.append("")
    lines.append("> We know how much we **have**. "
                 "We do not know how much there **is**.")
    return "\n".join(lines)


def body_for(kind: str) -> str:
    """這個檔要寫哪一種語言的表。"""
    if kind == "en":
        return english_block()
    if kind == "both":
        return english_block() + "\n\n" + block()
    return block()


def block() -> str:
    lines = ["| 類別 | 收集進度 | 數量 | 分母從哪來 |", "|---|---|---:|---|"]
    for label, progress, number, note in rows():
        lines.append(f"| {label} | {progress} | {number} | {note} |")
    lines.append("")
    lines.append("任務對話、NPC 名牌與介面文字**沒有任何公開資料可以爬**"
                 "——不在 Wynncraft API，也不在 Wynntils 的 CDN。"
                 "只能靠玩家在遊戲裡實際遇到時由模組收集回來，"
                 "所以「還差多少」這件事，名牌與介面那一列是誠實的空白：")
    lines.append("")
    lines.append("> 我們知道**已經收到**多少，不知道**總共**有多少。")
    return "\n".join(lines)


def replace(path: Path, body: str) -> bool:
    text = path.read_text(encoding="utf-8")
    if START not in text or END not in text:
        return False
    out = []
    rest = text
    while START in rest and END in rest:
        head, rest = rest.split(START, 1)
        _, rest = rest.split(END, 1)
        out.append(f"{head}{START}\n{body}\n{END}")
    fresh = "".join(out) + rest
    if fresh == text:
        return False
    path.write_text(fresh, encoding="utf-8", newline="\n")
    return True


def main(argv: list[str]) -> int:
    if "--refresh" in argv:
        return refresh()
    if not CACHE.is_file():
        print(f"沒有 {CACHE.relative_to(ROOT)}，"
              f"先跑 python tools/coverage.py --refresh")
        return 2
    if "--write" not in argv and "--check" not in argv:
        print(block())
        return 0
    stale = [name for name, kind in TARGETS.items()
             if (ROOT / name).is_file()
             and replace(ROOT / name, body_for(kind))]
    if "--check" in argv:
        if stale:
            print("涵蓋率表過期：" + "、".join(stale))
            print("跑 python tools/coverage.py --write 更新")
            return 1
        print("涵蓋率表是新的")
        return 0
    print("更新了：" + ("、".join(stale) if stale else "（沒有變化）"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
