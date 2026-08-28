#!/usr/bin/env python3
"""逐字打字模擬：找出「講到一半，中文自己換成別的字」的台詞。

為什麼需要
----------
NPC 的台詞是一個字一個字打出來的，所以同一句話在打完之前會以幾十種長度
進到查表程式。打到一半的那半句<本身>常常剛好也是語料裡的另一條——於是
畫面上先貼出 A 的譯文，再多打幾個字換成 B 的譯文，玩家看到的是中文自己
改口。這種問題<只有在打字過程中>才看得到，翻譯本身完全正確，
從語料檔上一個字都看不出來。

程式端已經擋掉大部分（見 TranslationStore#hasLonger 與 #curatedRival），
但擋不掉「同一句話被翻了兩次、而且翻得不一樣」——那要人決定留哪一個。
這支工具就是把還剩下的那些找出來。

做法是照著 DialogueRewriter#line 與 TranslationStore#matchPrefix 的實際流程
跑一遍：每一句台詞從第一個字開始餵進去，記下每一幀畫面上會顯示什麼，
只要<已經顯示出去的字被改掉>就記一筆。

用法
----
    python tools/typing-audit.py            # 檢查 zh_tw
    python tools/typing-audit.py zh_tw ja_jp

結果寫到 typing-out.txt。回傳 0 一律成功——這是給人看的報告，
不是合併前的關卡（那是 validate.py）。
"""
import bisect
import io
import json
import os
import sys

ROOT = os.path.join(
    "src", "main", "resources", "assets", "wynnchayuan", "translations")

MIN_PREFIX = 12       # TranslationStore.MIN_PREFIX_LENGTH
MIN_QUEST_PREFIX = 4  # TranslationStore.MIN_QUEST_PREFIX
SAME_LINE = 65        # TranslationStore.SAME_LINE_PERCENT
RIVAL_SCAN = 16       # TranslationStore.RIVAL_SCAN
AGREED = 12           # DialogueRewriter.AGREED
NAME_ROOM = 24        # DialogueRewriter.NAME_ROOM

# 只有 NPC 對話會走逐字打字這條路；tooltip 與 GUI 標籤一律是整段精確查表。
DIALOGUE = ("quest.json", "quest-dialogue.json", "quest-ui.json", "quest-name.json")


def is_dialogue(rel):
    return rel in DIALOGUE or rel.startswith("quest/")


def words(text):
    out = set()
    cur = []
    for ch in text:
        if ch.isalnum():
            cur.append(ch.lower())
        elif cur:
            out.add("".join(cur))
            cur = []
    if cur:
        out.add("".join(cur))
    return out


WORD_CACHE = {}


def cached_words(text):
    got = WORD_CACHE.get(text)
    if got is None:
        got = words(text)
        WORD_CACHE[text] = got
    return got


def same_line(a, b):
    x, y = cached_words(a), cached_words(b)
    if not x or not y:
        return False
    both = len(x & y)
    return both * 100 >= len(x | y) * SAME_LINE


class Store:
    def __init__(self, lang):
        self.entries = {}
        self.quest = {}
        self.wiki = set()
        self.where = {}
        base = os.path.join(ROOT, lang)
        index = os.path.join(base, "_index.json")
        if os.path.isfile(index):
            files = json.load(io.open(index, encoding="utf-8")).get("files", [])
        else:
            files = sorted(f for f in os.listdir(base) if f.endswith(".json"))
        for rel in files:
            path = os.path.join(base, rel)
            if not os.path.isfile(path):
                continue
            obj = json.load(io.open(path, encoding="utf-8"))
            holder = obj.get("entries") if isinstance(obj.get("entries"), dict) else obj
            for key, val in holder.items():
                if key.startswith("_"):
                    continue
                if isinstance(val, str):
                    src, dst, quest, wiki = key, val, None, False
                elif isinstance(val, dict):
                    src, dst = val.get("src"), val.get("dst")
                    quest, wiki = val.get("quest"), val.get("source") == "wiki"
                else:
                    continue
                if not src or not dst or not str(dst).strip():
                    continue
                src, dst = src.strip(), str(dst).strip()
                self.entries[src] = dst
                self.where[src] = rel
                if wiki:
                    self.wiki.add(src)
                if quest and quest.strip():
                    self.quest.setdefault(quest.strip(), []).append(src)
        self.keys = sorted(self.entries)
        for q in self.quest:
            self.quest[q] = sorted(set(self.quest[q]))

    # --- TranslationStore 的三支：unique / settle / curatedRival ---

    def unique(self, keys, key):
        i = bisect.bisect_left(keys, key)
        if i >= len(keys) or not keys[i].startswith(key):
            return None
        first = keys[i]
        if i + 1 < len(keys) and keys[i + 1].startswith(key):
            return self.settle(keys, i, key, first)
        return first

    def settle(self, keys, i, key, first):
        curated = None
        scanned = 0
        while i < len(keys) and keys[i].startswith(key):
            scanned += 1
            if scanned > RIVAL_SCAN or not same_line(first, keys[i]):
                return None
            if keys[i] not in self.wiki:
                if curated is not None:
                    return None
                curated = keys[i]
            i += 1
        return curated

    def curated_rival(self, hit, key):
        if hit not in self.wiki:
            return None
        i = bisect.bisect_left(self.keys, key)
        scanned = 0
        while i < len(self.keys) and self.keys[i].startswith(key):
            scanned += 1
            if scanned > RIVAL_SCAN:
                break
            k = self.keys[i]
            if k not in self.wiki and k != hit and same_line(hit, k):
                return k
            i += 1
        return None

    def has_longer(self, key):
        i = bisect.bisect_right(self.keys, key)
        return i < len(self.keys) and self.keys[i].startswith(key)

    def match_prefix(self, partial, evidence, quest):
        key = partial.strip()
        if not key:
            return None
        if quest and len(key) >= MIN_QUEST_PREFIX and quest in self.quest:
            hit = self.unique(self.quest[quest], key)
            if hit is not None:
                return self.curated_rival(hit, key) or hit
        if evidence < MIN_PREFIX:
            return None
        return self.unique(self.keys, key)


def within(source, typed):
    if source.startswith(typed):
        return True
    room = min(len(source), len(typed))
    agreed = 0
    while agreed < room and source[agreed] == typed[agreed]:
        agreed += 1
    return (agreed >= AGREED and agreed < len(source)
            and len(typed) <= len(source) + NAME_ROOM)


def typed_so_far(full, typed, total):
    """DialogueRewriter#typedSoFar：譯文照打字進度按比例露出來。"""
    if total <= 0 or typed >= total:
        return full
    take = int(round(len(full) * typed / total))
    take = max(0, min(len(full), take))
    brace = full.rfind("{", 0, max(0, take - 1) + 1)
    if brace >= 0 and full.find("}", brace) >= take:
        take = brace
    return full[:take]


def type_out(store, line, quest):
    """回傳每一幀命中的語料鍵（照 DialogueRewriter#line 的順序）。"""
    seen = []
    said = ""
    spoken = None
    for k in range(1, len(line) + 1):
        raw = line[:k]
        brace = raw.rfind("{")
        if brace >= 0 and "}" not in raw[brace:]:
            continue                       # 佔位符打到一半，跳過這一幀
        typed = raw.strip()
        if not typed:
            continue
        source = None
        if typed in store.entries:
            # FIX 候選：字還在長、而且後面還有更長的候選時，先不要定案。
            if not (k < len(line) and store.has_longer(typed)):
                source = typed             # 整段精確命中
        if source is None and spoken and said and raw.startswith(said) \
                and within(spoken, typed):
            source = spoken                # 沿用上一幀認出來的那條
        if source is None:
            source = store.match_prefix(typed, len(raw), quest)
        if source is not None:
            said = raw
            spoken = source
            dst = store.entries[source]
            shown = dst if source == typed else typed_so_far(
                dst, len(typed), len(source))
            seen.append((k, source, shown))
        elif not raw.startswith(said):
            said = ""
            spoken = None
    return seen


def audit(lang, out):
    store = Store(lang)
    quest_of = {}
    base = os.path.join(ROOT, lang)
    index = os.path.join(base, "_index.json")
    files = json.load(io.open(index, encoding="utf-8")).get("files", []) \
        if os.path.isfile(index) else []
    for rel in files:
        path = os.path.join(base, rel)
        if not os.path.isfile(path) or not is_dialogue(rel):
            continue
        obj = json.load(io.open(path, encoding="utf-8"))
        holder = obj.get("entries") if isinstance(obj.get("entries"), dict) else obj
        for key, val in holder.items():
            if key.startswith("_") or not isinstance(val, dict):
                continue
            src = (val.get("src") or "").strip()
            if src and val.get("quest"):
                quest_of.setdefault(src, val["quest"].strip())

    lines = [s for s in store.keys
             if is_dialogue(store.where[s]) and len(s) >= MIN_PREFIX]
    bad = []
    minor = []
    for line in lines:
        quest = quest_of.get(line)
        seen = type_out(store, line, quest)
        # 玩家看得到的才算問題：譯文一路往後長是正常的，
        # <b>已經顯示出去的字被改掉</b>才是回報的「講到一半自己換字」。
        jumps = []
        prev = ""
        prev_src = None
        for k, src, shown in seen:
            agreed = 0
            while (agreed < len(prev) and agreed < len(shown)
                   and prev[agreed] == shown[agreed]):
                agreed += 1
            if prev and agreed < len(prev) and src != prev_src:
                # 只是縮回去（新的是舊的開頭）＝同樣的字少露幾個，一閃而過；
                # 字真的變成別的字才是回報的「講到一半自己換字」。
                kind = "縮短" if prev.startswith(shown) else "改口"
                jumps.append((kind, k, prev_src, prev, src, shown))
            prev, prev_src = shown, src
        # 舊的那條短到連前綴門檻都不到（「...」這種）＝只是開頭一兩個字閃一下，
        # 跟回報的「整個詞換掉」不是同一件事，分開算。
        real = [j for j in jumps
                if j[0] == "改口" and len(j[2]) >= MIN_PREFIX]
        if real:
            bad.append((line, quest, real))
        elif jumps:
            minor.append((line, quest, jumps))

    out.write("=" * 74 + chr(10))
    out.write(lang + "：模擬 " + str(len(lines)) + " 句台詞，"
              + str(len(bad)) + " 句會在打到一半時把已經顯示的中文換成別的字，另有 "
              + str(len(minor)) + " 句只是開頭一兩個字閃一下" + chr(10))
    out.write("=" * 74 + chr(10) + chr(10))
    for line, quest, jumps in bad:
        out.write("● " + line[:160] + chr(10))
        out.write("   任務：" + str(quest) + chr(10))
        for kind, k, old_src, old_shown, new_src, new_shown in jumps:
            out.write("   打到第 " + str(k) + " 字，畫面上的中文被改掉：" + chr(10))
            out.write("        原本顯示  " + old_shown[:80] + chr(10))
            out.write("        變成      " + new_shown[:80] + chr(10))
            out.write("        來源 A  " + store.where[old_src]
                      + ("  [wiki]" if old_src in store.wiki else "  [校訂]")
                      + "  " + old_src[:110] + chr(10))
            out.write("        來源 B  " + store.where[new_src]
                      + ("  [wiki]" if new_src in store.wiki else "  [校訂]")
                      + "  " + new_src[:110] + chr(10))
        out.write(chr(10))
    return len(lines), len(bad), len(minor)


def main():
    langs = sys.argv[1:] or ["zh_tw"]
    out = io.open("typing-out.txt", "w", encoding="utf-8")
    for lang in langs:
        total, bad, minor = audit(lang, out)
        print((lang + ": " + str(total) + " lines, " + str(bad)
               + " rewrite, " + str(minor) + " minor"))
    out.close()
    print("-> typing-out.txt")


main()
