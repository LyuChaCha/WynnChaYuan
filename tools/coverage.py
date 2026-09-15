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

不再寫進文件
------------
這兩張表以前夾在 README 與商店頁的 ``<!-- 涵蓋率:開始 -->`` 標記之間。
2026-09-15 改版文件時拿掉了：玩家讀不懂「預估總數」，而祕密發現那一列
是從一個樣本往外推的量級，放在首頁看起來像知道的數字。現在只給維護者
在終端機看。

``--write`` 與 ``--check`` 留著，因為工作流程還在呼叫它們：
``--write`` 什麼都不寫；``--check`` 確認沒有文件又把舊標記放回去
——放回去的話那段數字不會再有人更新，只會慢慢變成謊話。

用法：
    python tools/coverage.py            # 印出來看
    python tools/coverage.py --refresh  # 重新抓官方任務清單（需要網路）
    python tools/coverage.py --check    # CI 用：文件裡還有舊標記就非零退出
    python tools/coverage.py --write    # 相容用，不寫任何東西
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

# 以前會被寫入的標記。現在只用來確認它們沒有被放回文件裡。
START = "<!-- 涵蓋率:開始 -->"
END = "<!-- 涵蓋率:結束 -->"
DOCS = (
    "README.md",
    "README.en.md",
    "CONTRIBUTING.md",
    "docs/modrinth-description.md",
    "docs/curseforge-description.md",
)

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


_CACHE: dict | None = None


def cache() -> dict:
    global _CACHE
    if _CACHE is None:
        _CACHE = json.loads(CACHE.read_text(encoding="utf-8"))
    return _CACHE


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


# 每一類是由哪幾個語料檔組成的。
#
# 跟 update-docs.py 的 ORDER 涵蓋同一批檔案，只是那邊一檔一列、這邊按「玩家
# 會在哪裡看到」歸成六類。少列一個檔的代價是它從分母裡消失——所以底下的
# {@code leftover} 會把沒歸到類的檔印出來。
GROUPS = [
    ("任務對話", ["quest-dialogue.json"]),
    ("祕密發現的故事", ["secret-dialogue.json"]),
    ("裝備的傳說敘述", ["gear-*.json"]),
    ("材料、素材、書卷、Aspect",
     ["ingredient.json", "material.json", "tome.json", "charm.json",
      "aspect.json", "aspect-desc.json"]),
    ("技能樹", ["ability/*.json", "ability-labels.json", "ability-terms.json"]),
    ("NPC 名牌、介面、系統訊息",
     ["npc.json", "gui.json", "label.json", "misc.json", "ui-labels.json",
      "quest-ui.json", "quest.json", "quest-name.json", "discovery.json",
      "discovery-name.json", "lootrun.json", "raid.json", "dungeon.json",
      "guild.json", "major-id.json", "major-id-terms.json",
      "profession-terms.json", "chat-terms.json", "dialogue-choice.json",
      "wynntils.json", "unsorted.json"]),
]


def optional(path: Path, entry: dict) -> bool:
    """裝備的<b>名稱</b>不算。跟 update-docs.py 同一條規則，改要一起改。"""
    return path.name.startswith("gear-") and entry.get("role") == "name"


def tally(patterns: list[str]) -> tuple[int, int]:
    """（已翻譯, 已收集）。"""
    done = total = 0
    for pattern in patterns:
        for name in sorted(glob.glob(str(TRANSLATIONS / pattern))):
            path = Path(name)
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except Exception:
                continue
            entries = data.get("entries")
            if isinstance(entries, dict):
                for value in entries.values():
                    if not isinstance(value, dict) or value.get("keep"):
                        continue
                    if optional(path, value):
                        continue
                    total += 1
                    if value.get("dst", "").strip():
                        done += 1
            else:
                for key, value in data.items():
                    if key.startswith("_") or not isinstance(value, str):
                        continue
                    total += 1
                    if value.strip():
                        done += 1
    return done, total


def estimate(label: str, collected: int) -> tuple[int | None, str]:
    """這一類總共大概有多少句，以及那個數字是怎麼來的。

    <p>估不出來就回傳 {@code None}。這一欄的用處全在那句「怎麼來的」——
    沒有依據的數字比空白更糟，因為它看起來是知道的。
    """
    saved = cache()
    if label == "任務對話":
        quests = saved["quests"]
        got = len([q for q in quests if q in collected_quests()])
        if got >= len(quests):
            return collected, f"{got}/{len(quests)} 個任務都收齊了，收到的就是全部"
        # 還沒收齊就照「每個任務平均幾句」往外推
        per = collected / max(got, 1)
        return round(per * len(quests)), (
            f"已收 {got}/{len(quests)} 個任務，照每個任務平均 {per:.0f} 句推算")
    if label == "祕密發現的故事":
        total = saved.get("secrets", 0)
        got = len(glob.glob(str(TRANSLATIONS / "secret/*.json")))
        if not total or not got:
            return None, "還沒收到任何一個，估不出來"
        per = collected / got
        return round(per * total), (
            f"{total} 個發現只收到 {got} 個，"
            f"照那 {got} 個平均 {per:.0f} 句往外推（樣本很少，只是個量級）")
    if label in ("裝備的傳說敘述", "材料、素材、書卷、Aspect", "技能樹"):
        return collected, "官方 CDN 整批下載，收到的就是全部"
    return None, "**沒有清單**，只能靠玩家遇到；估不出來"


_ROWS_CACHE: list | None = None


def estimate_rows() -> list[tuple[str, int, int, int | None, str]]:
    """（類別, 已翻譯, 已收集, 預估總數, 依據）。

    <p>算一次要把整份語料讀過一遍，其中 quest-dialogue.json 是兩萬多條，
    所以記住結果，同一次執行裡不重讀。
    """
    global _ROWS_CACHE
    if _ROWS_CACHE is not None:
        return _ROWS_CACHE
    out = []
    for label, patterns in GROUPS:
        done, total = tally(patterns)
        guess, why = estimate(label, total)
        out.append((label, done, total, guess, why))
    _ROWS_CACHE = out
    return out


def leftover() -> list[str]:
    """沒有被歸進任何一類的語料檔。漏掉一個就等於它從分母裡消失了。"""
    counted = set()
    for _, patterns in GROUPS:
        for pattern in patterns:
            for name in glob.glob(str(TRANSLATIONS / pattern)):
                counted.add(Path(name).name)
    everything = {p.name for p in TRANSLATIONS.rglob("*.json")
                  if not p.name.startswith("_")}
    # quest/ 與 secret/ 底下是譯者的工作檔，遊戲讀的是合併後的那一份，
    # 兩邊都算會重複計算一次。
    for folder in ("quest", "secret"):
        everything -= {p.name for p in (TRANSLATIONS / folder).glob("*.json")}
    return sorted(everything - counted)


def summary() -> tuple[int, int, int, bool]:
    """（已翻譯, 已收集, 預估總數, 有沒有估不出來的類別）。"""
    done = collected = guessed = 0
    partial = False
    for _, one_done, one_total, guess, _ in estimate_rows():
        done += one_done
        collected += one_total
        # 估不出來的那一類，就拿「已經收到的」當它的下界——
        # 整份總數因此是「至少」，而不是一個假裝知道的數字。
        guessed += guess if guess is not None else one_total
        partial = partial or guess is None
    return done, collected, guessed, partial


def estimate_block() -> str:
    lines = ["| 類別 | 已翻譯 | 已收集 | 預估總數 | 這個預估怎麼來的 |",
             "|---|---:|---:|---:|---|"]
    for label, done, total, guess, why in estimate_rows():
        shown = f"~{guess:,}" if guess is not None else "—"
        if guess is not None and guess == total:
            shown = f"{guess:,}"
        lines.append(f"| {label} | {done:,} | {total:,} | {shown} | {why} |")
    done, collected, guessed, partial = summary()
    lines.append("")
    lines.append(f"> 全部加起來：**預估{'至少 ' if partial else ' '}{guessed:,} 句**"
                 f"，已收集 **{collected:,} 句**（{collected / guessed * 100:.0f}%）"
                 f"，已翻譯 **{done:,} 句**（佔預估的 {done / guessed * 100:.0f}%、"
                 f"佔已收集的 {done / collected * 100:.1f}%）。")
    if partial:
        lines.append("")
        lines.append("「至少」是因為名牌與介面那一類沒有清單，"
                     "它在總數裡只算了**已經收到的**——真正的數字只會更大。")
    return "\n".join(lines)


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


def stale_markers() -> list[str]:
    """哪些文件還留著舊的涵蓋率標記。"""
    found = []
    for name in DOCS:
        path = ROOT / name
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8")
        if START in text or END in text:
            found.append(name)
    return found


def main(argv: list[str]) -> int:
    if "--refresh" in argv:
        return refresh()
    if "--check" in argv:
        found = stale_markers()
        if found:
            print("這些文件還有涵蓋率標記：" + "、".join(found))
            print("涵蓋率表已經不寫進文件，標記之間的數字不會再更新——請整段拿掉。")
            return 1
        print("文件裡沒有涵蓋率標記")
        return 0
    if "--write" in argv:
        print("涵蓋率表已經不寫進文件，沒有東西要更新。"
              "要看數字請直接跑 python tools/coverage.py")
        return 0
    if not CACHE.is_file():
        print(f"沒有 {CACHE.relative_to(ROOT)}，"
              f"先跑 python tools/coverage.py --refresh")
        return 2
    print(block() + "\n\n" + estimate_block())
    missing = leftover()
    if missing:
        print("\n沒有歸進任何一類的檔案：" + "、".join(missing))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
