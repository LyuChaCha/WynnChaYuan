<p align="center">
  <img src="https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/docs/icon.png" width="180" alt="WynnChaYuan">
</p>

# WynnChaYuan

**A multi-language translation mod for Wynncraft.** The original text is kept; the translation is shown beside it or written in its place.

**Wynncraft 多語言翻譯模組。** 原文保留，譯文顯示在旁邊，或直接寫進原本的位置。

**[GitHub](https://github.com/LyuChaCha/WynnChaYuan)** · **[Report a line / 回報](https://github.com/LyuChaCha/WynnChaYuan/issues)** · **[Changelog / 更新日誌](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CHANGELOG.md)** · **[Ko-fi](https://ko-fi.com/lyuchacha)** · Discord: **LyuChaCha**

---

## English

> **Beta.** The translations are mostly AI-generated and only partly proofread. Expect mistranslations and inconsistent terms.

### What it translates

| Content | What is translated, and how it is shown |
|---|---|
| Item tooltips | Gear lore, stats, Major IDs; a panel beside the tooltip, or written into the tooltip itself |
| Ability trees | All five classes: nodes, descriptions, archetypes |
| Quest dialogue, choices | Inside **Wynncraft's own dialogue box** (frame, nameplate and portrait kept), typed out in step with the original; or a separate box |
| Quest tracker | Its own box; the heading says what is tracked |
| NPC nameplates, floating text | A box while you look at one, or replaced in place |
| Chat messages | Server messages; column layouts such as the Lootrun summary and beacons stay aligned (player chat is never translated) |
| Menus and interfaces | Trade market, guild, store and other screens |
| Lootruns, raids, dungeons | Missions, boons, beacons, aspects, gambits, loot panels, dungeon names |
| Discoveries | Names, descriptions, secret discovery stories |
| Title text | The big text in the middle of the screen |

Gear names stay in English by default; they can be turned on in F6.

### More

| Feature | What it does |
|---|---|
| Translations update themselves | New translations are downloaded from GitHub on your next launch, no mod update needed |
| Market search | Type the translated name in the trade market; the English one is sent |
| Copy chat | Copy recent chat lines for a report |
| F9 screenshot | Captures the translation panel to the clipboard or a file |
| F6 settings | A mode for each kind of text; translation, fallback and interface language; draggable, resizable boxes |
| Update notice | A one-time chat notice linking to GitHub Releases |

Client-side only. The server does not need it.

### Languages

| Language | Coverage |
|---|---|
| Traditional Chinese | Main language, everything |
| Simplified Chinese | Everything, quest dialogue included |
| Japanese | Everything, quest dialogue included |
| Russian | Everything, quest dialogue included |
| Korean | Everything, quest dialogue included |
| Spanish | Everything, quest dialogue included |

Switch under F6 → Data, without changing the game's language.

### Progress

<!-- 進度:開始 -->
更新於 2026-09-25。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | ██████████ 99.4% | 49,883 / 50,207 |
| `ru_ru` Русский | ██████████ 96.3% | 48,364 / 50,207 |
| `zh_cn` 简体中文 | ██████████ 96.3% | 48,363 / 50,207 |
| `ko_kr` 한국어 | ██████████ 96.3% | 48,362 / 50,207 |
| `ja_jp` 日本語 | ██████████ 96.3% | 48,353 / 50,207 |
| `es_es` Español | ██████████ 95.9% | 48,161 / 50,207 |

各語言還缺哪些檔案 / Per-language breakdown: [PROGRESS.md](https://github.com/LyuChaCha/WynnChaYuan/blob/main/docs/PROGRESS.md)
<!-- 進度:結束 -->

### Install

| Requirement | Version |
|---|---|
| Minecraft | 1.21.11 |
| Loader | Fabric |
| Dependencies | [Wynntils](https://modrinth.com/mod/wynntils) 4.2+, [Fabric API](https://modrinth.com/mod/fabric-api) |

Put the jar in `mods/` and press **F6** in game.

### A line switches back to English halfway?

**That is normal.** The sentence is not in the corpus yet, so the original is shown. Export it and send it to us as described below; once it is added, everyone gets it on their next launch.

### How to help

Press **F6 → Data → Export untranslated strings**. The folder with `captured.json` opens; look through the file, delete anything personal, then attach it to the [issue form](https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml) (**F6 → Data → How to submit**) or send it to **LyuChaCha** on Discord. You do not have to translate anything.

The mod never sends anything by itself. The export already leaves out player names and guild, party, shout and private chat.

### Sponsoring

Free, and it will stay free. [Ko-fi](https://ko-fi.com/lyuchacha): 3 USD/month or 10 USD one-off puts you on the sponsor list. No paid features.

### Licence

Code: [GNU AGPLv3 or later](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE). Translations and data: [CC BY-NC-SA 4.0](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE-DATA). Item and ability data come from the public CDN used by Wynntils. Dialogue-box glyphs use [Fusion Pixel](https://github.com/TakWolf/fusion-pixel-font) (SIL OFL 1.1).

**Not affiliated with Wynncraft or the Wynntils team.** A community translation project.

---

## 繁體中文

> **目前是 Beta。** 譯文大多由 AI 產出，只有一部分經過人工校稿，會有錯譯與用詞不一致。

### 翻譯範圍

| 內容 | 翻了什麼、怎麼顯示 |
|---|---|
| 物品 tooltip | 裝備敘述、屬性、Major ID；旁邊另開面板，或寫進原本的 tooltip |
| 技能樹 | 五個職業的節點、說明與流派 |
| 任務對話、選項 | 寫進 **Wynncraft 自己的對話框**（框、名牌、頭像不動），逐字打出的節奏跟原文同步；也可另開小框 |
| 任務追蹤 | 獨立小框，抬頭照實際追蹤的東西顯示 |
| NPC 名牌、漂浮字 | 注視時跳小框，或就地取代 |
| 聊天訊息 | 伺服器訊息；Lootrun 結算、信標這類分欄訊息保持對齊（玩家發言不翻） |
| 選單與介面 | 交易市場、公會、商城等畫面 |
| Lootrun、討伐戰、地城 | 使命、賜福、信標、Aspect、Gambit、戰利品面板、地城名稱 |
| 探索點 | 名稱、說明、祕密發現的故事 |
| 畫面中央大字 | 標題與副標題 |

裝備名稱預設保留原文，F6 可以打開。

### 其他功能

| 功能 | 說明 |
|---|---|
| 譯文自動更新 | 新的翻譯下次進遊戲就從 GitHub 下載，不必更新模組 |
| 市集搜尋 | 在交易市集打譯名，送出前自動換回英文原名 |
| 複製聊天 | 複製最近的聊天訊息，方便回報 |
| F9 截圖 | 把譯文面板複製到剪貼簿或存成檔案 |
| F6 設定 | 每一類文字各自的模式；譯文、輔助、介面語言；小框可拖曳、可改大小 |
| 更新提示 | 有新版時在聊天室提示一次，連到 GitHub Releases |

純客戶端，伺服器不需要裝。

### 支援語言

| 語言 | 內容 |
|---|---|
| 繁體中文 | 主要語言，所有內容 |
| 簡體中文 | 所有內容，含任務對話 |
| 日文 | 所有內容，含任務對話 |
| 俄文 | 所有內容，含任務對話 |
| 韓文 | 所有內容，含任務對話 |
| 西班牙文 | 所有內容，含任務對話 |

在 F6 →「資料」切換，不必改遊戲語言。

### 翻譯進度

<!-- 進度:開始 -->
更新於 2026-09-25。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | ██████████ 99.4% | 49,883 / 50,207 |
| `ru_ru` Русский | ██████████ 96.3% | 48,364 / 50,207 |
| `zh_cn` 简体中文 | ██████████ 96.3% | 48,363 / 50,207 |
| `ko_kr` 한국어 | ██████████ 96.3% | 48,362 / 50,207 |
| `ja_jp` 日本語 | ██████████ 96.3% | 48,353 / 50,207 |
| `es_es` Español | ██████████ 95.9% | 48,161 / 50,207 |

各語言還缺哪些檔案 / Per-language breakdown: [PROGRESS.md](https://github.com/LyuChaCha/WynnChaYuan/blob/main/docs/PROGRESS.md)
<!-- 進度:結束 -->

### 安裝

| 需求 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| 載入器 | Fabric |
| 前置 | [Wynntils](https://modrinth.com/mod/wynntils) 4.2 以上、[Fabric API](https://modrinth.com/mod/fabric-api) |

jar 放進 `mods/`，進遊戲按 **F6**。

### 翻到一半變回英文？

**這是正常的**，代表那一句還不在語料裡，所以顯示原文。照下面的方式匯出交給我們，補好之後所有人下次進遊戲就看得到。

### 怎麼幫忙

按 **F6 →「資料」→「匯出未翻譯字串」**，會打開放著 `captured.json` 的資料夾。看過檔案、刪掉個人資訊之後，附到 [Issue 表單](https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml)（**F6 →「資料」→「如何提交」**），或在 Discord 傳給 **LyuChaCha**。不用翻任何東西。

模組不會自動送出任何東西；匯出檔已經濾掉玩家名字與公會、隊伍、喊話、私訊。

### 贊助

免費，也會一直免費。[Ko-fi](https://ko-fi.com/lyuchacha)：每月 3 USD 或單次 10 USD 以上列入贊助者名單。沒有付費功能。

### 授權

程式碼：[GNU AGPLv3 或之後的版本](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE)；翻譯與資料：[CC BY-NC-SA 4.0](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE-DATA)。物品與技能資料取自 Wynntils 使用的公開 CDN；對話框字形使用 [Fusion Pixel](https://github.com/TakWolf/fusion-pixel-font)（SIL OFL 1.1）。

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。
