<p align="center">
  <img src="https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/docs/icon.png" width="180" alt="WynnChaYuan">
</p>

# WynnChaYuan · Wynncraft translation mod

**Summary field (English, required by Modrinth):**

> Wynncraft translation mod — shown beside the original text, not replacing it. Traditional Chinese 98% done, seven more languages open. Requires Wynntils.

> [!IMPORTANT]
> **This is a beta (0.1.0).** Features and translations are still being worked on. If you
> run into anything, find **LyuChaCha** on Discord — all reports are welcome.
>
> **The translations are mostly AI-generated; only some have been proofread by a
> human.** Expect mistranslations, unnatural phrasing, and inconsistent proper
> nouns. If that bothers you, please hold off on using it for now.

---

## What it does

| | |
|---|---|
| **Item tooltips** | A translation panel beside the tooltip — or written into the tooltip itself, your choice |
| **NPC dialogue** | Translated **inside Wynncraft's own dialogue box**, keeping its frame, nameplate and portrait |
| **Dialogue choices** | Same three modes as the dialogue, set separately |
| **NPC nameplates** | A small box under the crosshair while you look at one |
| **Quest tracker** | A translated box on the left |
| **Ability tree** | Every node, description and archetype |
| **System chat and title text** | Quest completions, reward lists, and the big text in the middle of the screen |
| **Screenshots** | `F9` captures the translation panel — copy to clipboard or save to a file |

**Seven languages have a folder.** Traditional Chinese is 98% done; Simplified
Chinese, Japanese, Russian, Spanish, Korean, German and French are open and
waiting for translators. A language with nothing in it is not shipped and does
not appear in the language list.

---

## English

Traditional Chinese (zh-TW) translation for **Wynncraft**: items, abilities, NPCs,
quests and menus.

By default it **does not replace the original text**. Translations appear in a
separate panel while the English stays exactly where it was.

### Why not just replace the English?

Two practical reasons.

**Wynncraft is a multiplayer game.** If your screen only says the Chinese for
*Blacksmith*, you cannot follow a conversation about going to see one. Veteran
players, the trade market and the wiki all use English names.

**Wynncraft's layout is held together by resource-pack glyphs.** Column alignment,
element icons and borders are drawn with invisible spacing characters and custom
fonts. Replacing text naively pulls tooltips apart.

So the approach is: **keep the original, show the translation next to it.** Glance
at it when you want the meaning; the English is still there when you need to talk
to someone.

> If you don't need the English, item translation can be switched to **in-place**
> mode in the settings. Both modes use the same piece-by-piece replacement, so
> icons, colours and column alignment are preserved identically — the only
> difference is whether the original stays on screen.

### What gets translated

| Content | How it is shown |
|---|---|
| Item tooltips | A separate panel beside the tooltip, or in-place (optional) |
| NPC nametags | A small box near the crosshair when you look at them; the nametag itself is untouched |
| Quest dialogue | Its own box at the bottom of the screen |
| Quest tracker | Its own box at the side of the screen |
| Menus | Guild, content book, ability tree and other GUIs |

All four boxes can be **dragged into position**, and they are shown together while
you arrange them so you can tell whether they overlap.

### Installing

| Requirement | Version |
|---|---|
| Minecraft | 1.21.11 |
| Loader | Fabric |
| Dependencies | [Wynntils](https://modrinth.com/mod/wynntils) 4.2+ · Fabric API |

Drop the jar into `mods/` and press **F6** in game. Translation working files are
created on first launch.

**Client-side only.** The server does not need it and cannot tell you are using it.

### Settings (F6)

- **Item translation** — separate panel / in-place / off
- **Panel position** — follow the mouse or pin it; drag to arrange
- **NPC nametags** — mode, hold time, detection range and aim angle
- **Border colour** — any hex colour
- **Translation source** — GitHub (shared) or local files, for testing your own

### Translations update themselves

Translations are not baked into the jar. The mod syncs the latest set from GitHub
at startup, so **once a translation is merged, everyone gets it the next time they
launch** — no new download needed. There is a re-fetch button in F6 if you want it
immediately.

Offline, it falls back to the last cached copy, then to the version bundled in the
jar. Losing your connection means "not the newest", never "no translations".

### Installing the mod helps finish it

Quest dialogue and NPC names **have no official data source**. There is no file to
scrape — somebody has to walk up to that NPC in game.

So the mod records the lines it could not translate and, **by default**, sends
them back to the translation team (F6 -> *Share with the translation team* turns
this off). A hundred people each playing their own way add up to the whole game.

| | |
|---|---|
| **Sent** | The game's own English text: quest dialogue, menus, item lore, NPC nameplates, server announcements |
| **Never sent** | Your account, UUID, coordinates, which world you are on; guild, party, shout and private chat - **anything other people typed** |

Three independent personal-data filters stand in the way: one in the mod before
sending, one in the collector, one before anything reaches the repository - each
with its own tests. The mod explains this in chat once, on your first launch,
and never again.

If you would rather not share, turn sharing off and leave only **Collect
untranslated strings** on: the lines go to `config/wynnchayuan/captured.json`
for you to read through and attach to a
[GitHub issue](https://github.com/LyuChaCha/WynnChaYuan/issues) yourself.

**You do not have to translate anything to help.** Just playing with it on
already tells the project what players actually run into.

### Progress, and helping out

<!-- 進度:開始 -->
更新於 2026-09-13。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | █████████░ 91.9% | 38,457 / 41,831 |
| `zh_cn` 简体中文 | ██░░░░░░░░ 20.3% | 5,846 / 28,770 |
| `ja_jp` 日本語 | ░░░░░░░░░░ 2.1% | 612 / 28,728 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 2.1% | 612 / 28,728 |
| `de_de` Deutsch | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `es_es` Español | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `fr_fr` Français | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `ru_ru` Русский | ░░░░░░░░░░ 0.0% | 0 / 28,728 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
<!-- 進度:結束 -->

**How much of the whole game has been collected** is a different
question. The percentages above are out of *the lines we have* - a line nobody has
ever run into sits in neither the numerator nor the denominator.

<!-- 涵蓋率:開始 -->
| Category | Collected | Count | Where the denominator comes from |
|---|---|---:|---|
| Quest dialogue | ██████████ | 157 / 157 quests | Official quest list (wiki) |
| Secret discovery stories | ░░░░░░░░░░ | 1 / 121 discoveries | Official secret discovery list (wiki) |
| Gear lore | ██████████ | all | Official CDN, downloaded wholesale |
| Ingredients, materials, tomes, aspects | ██████████ | all | Official CDN, downloaded wholesale |
| Ability trees | ██████████ | all | Official CDN, downloaded wholesale |
| NPC nameplates, menus, system messages | — | 4,628 + 1,050 collected | **No official list** - only what players run into |

Quest dialogue, NPC nameplates and menu text **have no public data source** - not in the Wynncraft API, not on Wynntils' CDN. They only arrive when a player actually runs into them in game, so the last row is an honest blank:

> We know how much we **have**. We do not know how much there **is**.

| Category | Translated | Collected | Estimated total | Where the estimate comes from |
|---|---:|---:|---:|---|
| Quest dialogue | 22,401 | 22,444 | 22,444 | all 157/157 quests collected - what we have is all there is |
| Secret discovery stories | 68 | 68 | ~8,228 | only 1 of 121 discoveries collected; scaled up from those (tiny sample - an order of magnitude, not a figure) |
| Gear lore | 988 | 990 | 990 | official CDN, downloaded wholesale - what we have is all there is |
| Ingredients, materials, tomes, aspects | 1,603 | 1,603 | 1,603 | official CDN, downloaded wholesale - what we have is all there is |
| Ability trees | 1,858 | 1,920 | 1,920 | official CDN, downloaded wholesale - what we have is all there is |
| NPC nameplates, menus, system messages | 11,539 | 14,806 | — | **no list exists** - only what players run into; cannot be estimated |

> All together: **at least 49,991 lines estimated**, **41,831 collected** (84%), **38,457 translated** (77% of the estimate, 91.9% of what we have).

"At least" because nameplates and menu text have no list; that row counts only what has **already been collected**, so the real number is larger.

| 類別 | 收集進度 | 數量 | 分母從哪來 |
|---|---|---:|---|
| 任務對話 | ██████████ | 157 / 157 個任務 | 官方任務清單（wiki） |
| 祕密發現的故事 | ░░░░░░░░░░ | 1 / 121 個發現 | 官方祕密發現清單（wiki） |
| 裝備的傳說敘述 | ██████████ | 全部 | 官方 CDN，整批下載 |
| 材料、素材、書卷、Aspect | ██████████ | 全部 | 官方 CDN，整批下載 |
| 技能樹 | ██████████ | 全部 | 官方 CDN，整批下載 |
| NPC 名牌、介面、系統訊息 | — | 已收 4,628 + 1,050 條 | **沒有官方清單**，只能靠玩家遇到 |

任務對話、NPC 名牌與介面文字**沒有任何公開資料可以爬**——不在 Wynncraft API，也不在 Wynntils 的 CDN。只能靠玩家在遊戲裡實際遇到時由模組收集回來，所以「還差多少」這件事，名牌與介面那一列是誠實的空白：

> 我們知道**已經收到**多少，不知道**總共**有多少。

| 類別 | 已翻譯 | 已收集 | 預估總數 | 這個預估怎麼來的 |
|---|---:|---:|---:|---|
| 任務對話 | 22,401 | 22,444 | 22,444 | 157/157 個任務都收齊了，收到的就是全部 |
| 祕密發現的故事 | 68 | 68 | ~8,228 | 121 個發現只收到 1 個，照那 1 個平均 68 句往外推（樣本很少，只是個量級） |
| 裝備的傳說敘述 | 988 | 990 | 990 | 官方 CDN 整批下載，收到的就是全部 |
| 材料、素材、書卷、Aspect | 1,603 | 1,603 | 1,603 | 官方 CDN 整批下載，收到的就是全部 |
| 技能樹 | 1,858 | 1,920 | 1,920 | 官方 CDN 整批下載，收到的就是全部 |
| NPC 名牌、介面、系統訊息 | 11,539 | 14,806 | — | **沒有清單**，只能靠玩家遇到；估不出來 |

> 全部加起來：**預估至少 49,991 句**，已收集 **41,831 句**（84%），已翻譯 **38,457 句**（佔預估的 77%、佔已收集的 91.9%）。

「至少」是因為名牌與介面那一類沒有清單，它在總數裡只算了**已經收到的**——真正的數字只會更大。
<!-- 涵蓋率:結束 -->

**Gear names are deliberately left in English** (5,389 of them). They are proper
nouns, and the trade market, the wiki and other players all use them.

**No programming needed.** The translation files are plain JSON — click the pencil
icon on GitHub and type.

- [Contributing guide](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CONTRIBUTING.md) (written in Traditional Chinese)
- [Glossary](https://github.com/LyuChaCha/WynnChaYuan/blob/main/GLOSSARY.md) — e.g. `Reflection` is ranged thorns damage, not "a reflection"

Submitted translations are checked automatically for placeholder counts,
resource-pack glyphs pasted into the text, and place names left untranslated.
Those are wrong regardless of translation quality, so a machine catches them and
people can focus on whether the wording is right.

### FAQ

**Will this get me banned?**
No. It is a client-side display mod. It does not modify packets, automate anything,
or send anything anywhere.

**Does it conflict with other Wynncraft mods?**
The default panel mode does not modify tooltip content at all, so it does not
interfere with other mods listening to the same event. Mods that add their own
tooltip sections (Nori, Wynnpool) are explicitly handled.

**Can my name be in the credits?**
Translate one line and it will be. The list is under F6 → About/Contributors, with
Minecraft heads — and people on it get an extra line above their nametag, visible
only to others running this mod.

### Sponsoring

This project is free and will stay free. If you would like to buy us a tea:
**<https://ko-fi.com/lyuchacha>**

| | |
|---|---|
| **3 USD/month** or more | Listed in the sponsor credits |
| **10 USD** one-off or more | Listed in the sponsor credits |

The list appears on GitHub, in game under **F6 -> About / Contributors**, and as
an extra line above your nameplate. Sponsoring does not influence what gets
translated, and there are no paid features.

### Versioning

Beta versions start at `0.1.0`. A major or notable update bumps it to `0.1.1`;
a bug fix or small change becomes `0.1.0_1`. **Translation-only updates are not
released** - translations sync themselves from GitHub, so there is nothing to
download.

### Licence and credits

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE).

- Item and ability data come from the public CDN used by [Wynntils](https://modrinth.com/mod/wynntils)
- Resource-pack glyphs and layout are Wynncraft's; this mod only displays them

**Not affiliated with Wynncraft or the Wynntils team.** A community translation project.

---

## 繁體中文

### 這個模組在做什麼

把 Wynncraft 的物品、技能、NPC、任務與介面翻成繁體中文。

但它**預設不會把原文換掉**——譯文顯示在旁邊另一塊面板，英文原名照樣留在畫面上。

### 為什麼不直接換成中文

兩個現實問題：

**一、Wynncraft 是多人遊戲。** 畫面上只剩中文的話，跟別人討論「去找 Blacksmith」
就對不上話——老玩家只認得英文名，交易市場與 wiki 也都是英文。

**二、Wynncraft 的排版靠材質包符號撐著。** 那些欄位對齊、元素圖示、外框，
用的是不可見的排版字元與自訂字型。粗暴地替換文字很容易讓整個 tooltip 破圖。

所以做法是：**原文留著，譯文另外顯示**。想知道意思看一眼，要跟人溝通時原文就在那裡。

> 不需要對照英文的人，可以在設定裡改成**就地取代**——譯文直接寫進原本的
> tooltip，畫面更乾淨。兩種模式用的是同一套逐片段替換，圖示、顏色與欄位對齊
> 的保真程度完全相同，差別只在要不要保留原文。

---

### 翻譯範圍

| 內容 | 呈現方式 |
|---|---|
| 物品 tooltip | 旁邊另開翻譯面板，或就地取代（可選） |
| NPC 名牌 | 注視時在準心附近跳一個小框，原文不動 |
| 任務對話 | 畫面下方的獨立小框 |
| 任務追蹤 | 畫面側邊的獨立小框 |
| 介面文字 | 公會、任務書、技能樹等 GUI |

四個框的位置都可以**拖曳調整**，而且是四個同時顯示——才看得出會不會互相擋到。

---

### 安裝

| 需求 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| 載入器 | Fabric |
| 前置 | [Wynntils](https://modrinth.com/mod/wynntils) 4.2 以上 · Fabric API |

把 jar 放進 `mods/`，進遊戲按 **F6** 開設定。首次啟動會自動產生翻譯工作檔。

**純客戶端模組**，伺服器不需要安裝，也不會知道你在用。

---

### 設定（F6）

- **物品翻譯**：另開面板 / 就地取代 / 關閉
- **面板位置**：跟隨滑鼠或固定，可拖曳排版
- **NPC 名牌**：模式、停留秒數、偵測距離與準心夾角
- **框線顏色**：自訂 16 進位色碼
- **譯文來源**：GitHub（統一）或本機（測試自己的翻譯用）
- **市集搜尋轉英文**：用中文搜尋，送出前自動換回英文原名
- **分享給翻譯團隊**：預設開啟，把模組翻不出來的句子送回來

---

### 翻譯會自己更新，不用重新下載模組

譯文不是寫死在 jar 裡的。模組啟動時會從 GitHub 同步最新版本，
所以**譯者把翻譯合併進去，所有人下次進遊戲就拿到了**。

想立刻確認的話，F6 有「從 GitHub 重新抓譯文」，不必重開遊戲。

離線或連不上時會用上次的快取；再不行才用 jar 內建的版本。所以斷網只是
「沒有最新的」，不會變成沒有翻譯。

---

## 裝了模組就是在幫忙

任務對話與 NPC 名稱**沒有官方資料可以抓**。沒有檔案可以爬，只能靠玩家在遊戲裡
真的走到那個 NPC 面前。

所以模組會把它翻不出來的句子記下來，並且**預設**送回翻譯團隊
（F6 →「分享給翻譯團隊」可以關）。一百個人各玩各的，語料就是所有人的總和。

| | |
|---|---|
| **會送** | 遊戲自己的英文字：任務對話、介面、物品說明、NPC 名牌、伺服器公告 |
| **不會送** | 你的帳號、UUID、座標、在哪個世界；公會、隊伍、喊話、私訊那些**別人打的字** |

個資濾網一共三道——模組送出前一道、收集站一道、進倉庫前一道，三道各自獨立
寫、各有各的測試。第一次進遊戲時模組會在聊天室說明一次，之後不再提。

不想分享的話，把分享關掉、只留「收集未翻譯字串」，句子就只會寫進
`config/wynnchayuan/captured.json`，自己看過再
[開 Issue](https://github.com/LyuChaCha/WynnChaYuan/issues) 附上來。

**你不用翻任何東西也能幫上忙。** 光是開著玩，就等於告訴這個專案「玩家實際
會遇到什麼」。

---

### 目前進度與參與翻譯

<!-- 進度:開始 -->
更新於 2026-09-13。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | █████████░ 91.9% | 38,457 / 41,831 |
| `zh_cn` 简体中文 | ██░░░░░░░░ 20.3% | 5,846 / 28,770 |
| `ja_jp` 日本語 | ░░░░░░░░░░ 2.1% | 612 / 28,728 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 2.1% | 612 / 28,728 |
| `de_de` Deutsch | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `es_es` Español | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `fr_fr` Français | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `ru_ru` Русский | ░░░░░░░░░░ 0.0% | 0 / 28,728 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
<!-- 進度:結束 -->

**全部的語料收集到多少**是另一個問題。上面的百分比，分母是
「我們手上有的句子」——一句沒有人在遊戲裡遇到過的台詞，既不在分子也不在分母裡。

（上面英文區塊裡那張「收集進度」表就是答案，中英各一份。）

**裝備名稱刻意保留原文**（共 5,389 條）。那些是專有名詞，交易市場、wiki 與
其他玩家用的都是英文。

**不需要會寫程式。** 翻譯檔是純 JSON，在 GitHub 網頁上點鉛筆就能改。

- [參與翻譯的完整說明](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CONTRIBUTING.md)
- [專有名詞對照表](https://github.com/LyuChaCha/WynnChaYuan/blob/main/GLOSSARY.md)
  （`Reflection` 是「遠程反傷」不是「反射」那類）

送出的翻譯會自動跑格式檢查：佔位符數量、材質包符號、地名保留——
這些無論翻得好不好都一定錯，讓機器擋掉，人就能專心看「翻得對不對」。

---

### 常見問題

**會不會被伺服器判定作弊？**
不會。這是純客戶端的顯示模組，不改封包、不自動化任何操作，也不送出任何東西。

**跟其他 Wynncraft 模組衝突嗎？**
預設的面板模式**完全不修改** tooltip 內容，所以不會干擾同樣掛在那個事件上的
其他模組。Nori、Wynnpool 這類會加自己區塊的模組也做過相容處理。

**我的名字可以出現在名單上嗎？**
翻一條就會。名單顯示在 F6 →「關於／貢獻者」，含 Minecraft 頭像；
名單上的人，名牌上方還會多一行標記（只有裝了本模組的人看得到）。

---

### 贊助

這個專案是免費的，也會一直免費。想請我們喝杯茶的話：
**<https://ko-fi.com/lyuchacha>**

| 方式 | 回饋 |
|---|---|
| 每月贊助 **3 USD** 以上 | 列入贊助者名單 |
| 單次贊助 **10 USD** 以上 | 列入贊助者名單 |

名單會出現在 GitHub 首頁、遊戲內的 **F6 →「關於／貢獻者」**，名牌上方也會多
一行贊助者標記。贊助不影響翻譯內容，也不會有任何付費才能用的功能。

### 版本號

Beta 期間從 `0.1.0` 起算。重大或重點更新進到 `0.1.1`，修 bug 與小調整則是
`0.1.0_1`。**純翻譯的更新不發版**——譯文會自己從 GitHub 同步，不需要重新
下載模組。

### 授權與致謝

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE)。

- 物品與技能資料取自 [Wynntils](https://modrinth.com/mod/wynntils) 使用的公開 CDN
- 材質包符號與排版由 Wynncraft 提供，本模組僅顯示、不修改

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。

**[GitHub](https://github.com/LyuChaCha/WynnChaYuan)** · **[回報問題](https://github.com/LyuChaCha/WynnChaYuan/issues)** · **[更新日誌](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CHANGELOG.md)**
