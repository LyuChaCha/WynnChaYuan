<p align="center">
  <img src="https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/docs/icon.png" width="180" alt="WynnChaYuan">
</p>

# WynnChaYuan — Wynncraft translation mod

> A Wynncraft translation mod that shows the translation **beside** the original
> instead of replacing it. Traditional Chinese is 98% done; seven more languages
> are open. Client-side. Requires Wynntils.

> [!IMPORTANT]
> **This is a beta (0.1.0).** Features and translations are still being worked on. If you
> run into anything, find **LyuChaCha** on Discord — all reports are welcome.
>
> **The translations are mostly AI-generated; only some have been proofread by a
> human.** Expect mistranslations, unnatural phrasing, and inconsistent proper
> nouns. If that bothers you, please hold off on using it for now.

---

## English

### What it translates

| Content | How you see it |
|---|---|
| **Item tooltips** | A translation panel beside the tooltip — or written into the tooltip itself |
| **NPC dialogue** | Translated **inside Wynncraft's own dialogue box**, keeping its frame, nameplate and portrait |
| **Dialogue choices** | Same three modes as the dialogue, set separately |
| **NPC nameplates and floating text** | A small box under the crosshair while you look at one — crafting stations and "Right-Click with empty hand" prompts included |
| **Quest tracker** | Its own box at the side of the screen |
| **Ability trees** | All five classes — every node, description and archetype |
| **Major IDs** | All 325 |
| **System chat** | Quest completions, reward lists, entering and leaving areas |
| **Title text** | The big text in the middle of the screen |
| **Screenshots** | `F9` captures the translation panel to your clipboard or a file |

### It does not replace the original — by default

Two practical reasons.

**Wynncraft is a multiplayer game.** If your screen only says the Chinese for
*Blacksmith*, you cannot follow a conversation about going to see one. Veteran
players, the trade market and the wiki all use English names.

**Wynncraft's layout is held together by resource-pack glyphs.** Column alignment,
element icons and borders are drawn with invisible spacing characters and custom
fonts. Replacing text naively pulls tooltips apart.

So: **keep the original, show the translation next to it.** If you don't need the
English, item tooltips, dialogue and choices can each be switched to **in-place**
mode independently. Both modes use the same piece-by-piece replacement, so icons,
colours and column alignment survive identically.

### Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Loader | Fabric |
| Dependencies | [Wynntils](https://www.curseforge.com/minecraft/mc-mods/wynntils) 4.2+ · Fabric API |

Drop the jar into `mods/` and press **F6** in game.

**Client-side only.** The server does not need it and cannot tell you are using it.
It does not modify packets, automate anything, or send anything anywhere.

### Settings (F6)

- **Item translation** — separate panel / in-place / off
- **Quest dialogue** — separate box / in-place / off
- **Dialogue choices** — separate box / in-place / off, set independently
- **NPC nameplates** — off / look at one / in-place, with hold time, range and aim angle
- **System chat** and **title text** — on or off
- **Panel position** — follow the mouse or pin it; drag all boxes at once to arrange them
- **Border colour** — any hex colour
- **Translation source** — GitHub (shared) or your own local files
- **Market search** — type the translated name, the English one gets sent
- **Share with the translation team** — on by default; sends the lines the mod could not translate

### Translations update themselves

Translations are not baked into the jar. The mod syncs the latest set from GitHub
at startup, so **once a translation is merged, everyone gets it the next time they
launch** — no new download needed.

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

### Progress

<!-- 進度:開始 -->
更新於 2026-09-12。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | █████████░ 90.4% | 37,450 / 41,405 |
| `zh_cn` 简体中文 | █░░░░░░░░░ 11.5% | 3,296 / 28,776 |
| `de_de` Deutsch | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `es_es` Español | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `fr_fr` Français | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `ja_jp` 日本語 | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
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
| NPC nameplates, menus, system messages | — | 4,380 + 1,050 collected | **No official list** - only what players run into |

Quest dialogue, NPC nameplates and menu text **have no public data source** - not in the Wynncraft API, not on Wynntils' CDN. They only arrive when a player actually runs into them in game, so the last row is an honest blank:

> We know how much we **have**. We do not know how much there **is**.

| Category | Translated | Collected | Estimated total | Where the estimate comes from |
|---|---:|---:|---:|---|
| Quest dialogue | 22,383 | 22,383 | 22,383 | all 157/157 quests collected - what we have is all there is |
| Secret discovery stories | 68 | 68 | ~8,228 | only 1 of 121 discoveries collected; scaled up from those (tiny sample - an order of magnitude, not a figure) |
| Gear lore | 986 | 990 | 990 | official CDN, downloaded wholesale - what we have is all there is |
| Ingredients, materials, tomes, aspects | 1,603 | 1,603 | 1,603 | official CDN, downloaded wholesale - what we have is all there is |
| Ability trees | 1,914 | 1,914 | 1,914 | official CDN, downloaded wholesale - what we have is all there is |
| NPC nameplates, menus, system messages | 10,589 | 14,422 | — | **no list exists** - only what players run into; cannot be estimated |

> All together: **at least 49,540 lines estimated**, **41,380 collected** (84%), **37,543 translated** (76% of the estimate, 90.7% of what we have).

"At least" because nameplates and menu text have no list; that row counts only what has **already been collected**, so the real number is larger.

| 類別 | 收集進度 | 數量 | 分母從哪來 |
|---|---|---:|---|
| 任務對話 | ██████████ | 157 / 157 個任務 | 官方任務清單（wiki） |
| 祕密發現的故事 | ░░░░░░░░░░ | 1 / 121 個發現 | 官方祕密發現清單（wiki） |
| 裝備的傳說敘述 | ██████████ | 全部 | 官方 CDN，整批下載 |
| 材料、素材、書卷、Aspect | ██████████ | 全部 | 官方 CDN，整批下載 |
| 技能樹 | ██████████ | 全部 | 官方 CDN，整批下載 |
| NPC 名牌、介面、系統訊息 | — | 已收 4,380 + 1,050 條 | **沒有官方清單**，只能靠玩家遇到 |

任務對話、NPC 名牌與介面文字**沒有任何公開資料可以爬**——不在 Wynncraft API，也不在 Wynntils 的 CDN。只能靠玩家在遊戲裡實際遇到時由模組收集回來，所以「還差多少」這件事，名牌與介面那一列是誠實的空白：

> 我們知道**已經收到**多少，不知道**總共**有多少。

| 類別 | 已翻譯 | 已收集 | 預估總數 | 這個預估怎麼來的 |
|---|---:|---:|---:|---|
| 任務對話 | 22,383 | 22,383 | 22,383 | 157/157 個任務都收齊了，收到的就是全部 |
| 祕密發現的故事 | 68 | 68 | ~8,228 | 121 個發現只收到 1 個，照那 1 個平均 68 句往外推（樣本很少，只是個量級） |
| 裝備的傳說敘述 | 986 | 990 | 990 | 官方 CDN 整批下載，收到的就是全部 |
| 材料、素材、書卷、Aspect | 1,603 | 1,603 | 1,603 | 官方 CDN 整批下載，收到的就是全部 |
| 技能樹 | 1,914 | 1,914 | 1,914 | 官方 CDN 整批下載，收到的就是全部 |
| NPC 名牌、介面、系統訊息 | 10,589 | 14,422 | — | **沒有清單**，只能靠玩家遇到；估不出來 |

> 全部加起來：**預估至少 49,540 句**，已收集 **41,380 句**（84%），已翻譯 **37,543 句**（佔預估的 76%、佔已收集的 90.7%）。

「至少」是因為名牌與介面那一類沒有清單，它在總數裡只算了**已經收到的**——真正的數字只會更大。
<!-- 涵蓋率:結束 -->

**Gear names are deliberately left in English** (5,389 of them). They are proper
nouns, and the trade market, the wiki and other players all use them.

### Helping translate

**No programming needed.** The translation files are plain JSON — click the pencil
icon on GitHub and type. Submitted translations are checked automatically for
placeholder counts, resource-pack glyphs pasted into the text and place names left
untranslated; those are wrong regardless of wording, so a machine catches them and
people can focus on whether the translation reads well.

- [Contributing guide](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CONTRIBUTING.md) (written in Traditional Chinese)
- [Glossary](https://github.com/LyuChaCha/WynnChaYuan/blob/main/GLOSSARY.md) — e.g. `Reflection` is ranged thorns damage, not "a reflection"

Translate one line and your name goes in the credits (F6 → About), with your
Minecraft head — and you get an extra line above your nametag, visible only to
others running this mod.

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

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE). Item and ability
data come from the public CDN used by Wynntils. Resource-pack glyphs and layout are
Wynncraft's; this mod only displays them.

**Not affiliated with Wynncraft or the Wynntils team.** A community translation project.

---

## 繁體中文

> [!IMPORTANT]
> **目前是 Beta（0.1.0）。** 功能與譯文都還在調整，遇到問題請到 Discord 找
> **LyuChaCha**，任何回報都歡迎。
>
> **譯文目前主要由 AI 產出，只有一部分經過人工校稿。** 會有錯譯、語氣不自然、
> 或是專有名詞前後不一致的地方。介意這件事的話，建議先不要使用。

### 翻譯範圍

| 內容 | 呈現方式 |
|---|---|
| **物品 tooltip** | 旁邊另開一塊翻譯面板，或直接寫進原本的 tooltip |
| **NPC 對話** | 譯文寫進 **Wynncraft 自己那個對話框**，框、名牌、頭像原樣保留 |
| **對話選項** | 跟對話一樣有三種模式，可以分開設 |
| **NPC 名牌與漂浮字** | 注視時在準心下方跳一個小框；工作站、「空手右鍵」那些字都算 |
| **任務追蹤** | 畫面側邊的獨立小框 |
| **技能樹** | 五個職業全部——每個節點、說明與流派 |
| **Major ID** | 325 條全部 |
| **系統訊息** | 任務完成、獎勵清單、進出區域那些聊天訊息 |
| **畫面中央大字** | 標題與副標題 |
| **譯文截圖** | **F9** 把翻譯面板拍下來，複製到剪貼簿或存成檔案 |

### 預設不取代原文

兩個現實問題。

**一、Wynncraft 是多人遊戲。** 畫面上只剩中文的話，跟別人討論「去找 Blacksmith」
就對不上話——老玩家只認得英文名，交易市場與 wiki 也都是英文。

**二、Wynncraft 的排版靠材質包符號撐著。** 那些欄位對齊、元素圖示、外框，
用的是不可見的排版字元與自訂字型。粗暴地替換文字很容易讓整個 tooltip 破圖。

所以做法是：**原文留著，譯文另外顯示**。不需要對照英文的人，物品、對話、選項
三者可以**各自**改成就地取代。兩種模式用的是同一套逐片段替換，圖示、顏色與
欄位對齊的保真程度完全相同。

### 安裝

| 需求 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| 載入器 | Fabric |
| 前置 | [Wynntils](https://www.curseforge.com/minecraft/mc-mods/wynntils) 4.2 以上 · Fabric API |

把 jar 放進 `mods/`，進遊戲按 **F6** 開設定。

**純客戶端模組**，伺服器不需要安裝，也不會知道你在用。不改封包、不自動化任何
操作，也不送出任何東西。

### 設定（F6）

- **物品翻譯**：另開面板 / 就地取代 / 關閉
- **任務對話**：另開小框 / 就地取代 / 關閉
- **對話選項**：另開小框 / 就地取代 / 關閉，跟對話分開設
- **NPC 名牌**：關閉 / 注視時顯示 / 就地取代，可調停留秒數、偵測距離與準心夾角
- **系統訊息**與**中央大字**：開或關
- **面板位置**：跟隨滑鼠或固定；四個框同時顯示，方便一次排好不互相擋
- **框線顏色**：自訂 16 進位色碼
- **譯文來源**：GitHub（統一）或本機（測試自己的翻譯用）
- **市集搜尋轉英文**：用中文搜尋，送出前自動換回英文原名
- **分享給翻譯團隊**：預設開啟，把模組翻不出來的句子送回來

### 翻譯會自己更新，不用重新下載模組

譯文不是寫死在 jar 裡的。模組啟動時會從 GitHub 同步最新版本，所以**譯者把翻譯
合併進去，所有人下次進遊戲就拿到了**。

離線或連不上時會用上次的快取，再不行才用 jar 內建的版本。斷網只是「沒有最新
的」，不會變成沒有翻譯。

### 裝了模組就是在幫忙

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

### 目前進度

<!-- 進度:開始 -->
更新於 2026-09-12。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | █████████░ 90.4% | 37,450 / 41,405 |
| `zh_cn` 简体中文 | █░░░░░░░░░ 11.5% | 3,296 / 28,776 |
| `de_de` Deutsch | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `es_es` Español | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `fr_fr` Français | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `ja_jp` 日本語 | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 0.0% | 0 / 28,728 |
| `ru_ru` Русский | ░░░░░░░░░░ 0.0% | 0 / 28,728 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
<!-- 進度:結束 -->

**全部的語料收集到多少**是另一個問題。上面的百分比，分母是
「我們手上有的句子」——一句沒有人在遊戲裡遇到過的台詞，既不在分子也不在分母裡。

（上面英文區塊裡那張「收集進度」表就是答案，中英各一份。）

**裝備名稱刻意保留原文**（共 5,389 條）。那些是專有名詞，交易市場、wiki 與
其他玩家用的都是英文。

### 參與翻譯

**不需要會寫程式。** 翻譯檔是純 JSON，在 GitHub 網頁上點鉛筆就能改。送出的翻譯
會自動跑格式檢查：佔位符數量、材質包符號、地名保留——這些無論翻得好不好都一定
錯，讓機器擋掉，人就能專心看「翻得對不對」。

- [參與翻譯的完整說明](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CONTRIBUTING.md)
- [專有名詞對照表](https://github.com/LyuChaCha/WynnChaYuan/blob/main/GLOSSARY.md)
  （`Reflection` 是「遠程反傷」不是「反射」那類）

翻一條名字就會出現在名單上（F6 →「關於」），含 Minecraft 頭像；名單上的人，
名牌上方還會多一行標記（只有裝了本模組的人看得到）。

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

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE)。物品與技能資料
取自 Wynntils 使用的公開 CDN；材質包符號與排版由 Wynncraft 提供，本模組僅顯示、
不修改。

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。

---

**[GitHub](https://github.com/LyuChaCha/WynnChaYuan)** · **[回報問題 / Issues](https://github.com/LyuChaCha/WynnChaYuan/issues)** · **[更新日誌 / Changelog](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CHANGELOG.md)**
