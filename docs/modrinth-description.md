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

| Content | How it is shown |
|---|---|
| Item tooltips | A panel beside the tooltip, or written into the tooltip itself |
| Quest dialogue, choices | Inside **Wynncraft's own dialogue box** (frame, nameplate and portrait kept), or a separate box |
| NPC nameplates, floating text | A box while you look at one, or replaced in place |
| Quest tracker | Its own box |
| Ability trees | All five classes: nodes, descriptions, archetypes |
| Major IDs | Name and full description |
| Lootruns, raids, dungeons | Missions, boons, beacons, aspects, gambits, loot panels |
| Discoveries | Names, descriptions, secret discovery stories |
| Chat and title text | Server messages and the big text in the middle of the screen (player chat is never translated) |

Gear names stay in English by default; they can be turned on in F6.

### Other features

| Feature | What it does |
|---|---|
| Market search | Type the translated name in the trade market; the English one is sent |
| Sync from GitHub | Translation fixes arrive on your next launch, no new download |
| Corpus sharing | Untranslated lines go to the translation team (see below) |
| Copy chat | Copy recent chat lines for a report |
| F9 screenshot | Captures the translation panel to the clipboard or a file |
| F6 settings | A mode for each kind of text; translation, fallback and interface language; draggable, resizable boxes |

Client-side only. The server does not need it.

### Languages

| Language | Status |
|---|---|
| Traditional Chinese | Main language, everything |
| Simplified Chinese | Everything, on par with Traditional Chinese |
| Japanese | Everything, on par with Traditional Chinese |
| Russian | Interface, ability trees, nameplates, item lore; quest dialogue in progress |
| Korean | Item tooltip labels and Major IDs only |

Switch under F6 → Data, without changing the game's language.

### Progress

<!-- 進度:開始 -->
更新於 2026-09-15。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_cn` 简体中文 | █████████░ 93.9% | 40,096 / 42,722 |
| `zh_tw` 繁體中文 | █████████░ 93.8% | 41,135 / 43,835 |
| `ja_jp` 日本語 | █████████░ 93.7% | 40,013 / 42,722 |
| `ru_ru` Русский | █████████░ 93.3% | 39,866 / 42,722 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 1.8% | 778 / 42,714 |

各語言還缺哪些檔案 / Per-language breakdown: [PROGRESS.md](https://github.com/LyuChaCha/WynnChaYuan/blob/main/docs/PROGRESS.md)
<!-- 進度:結束 -->

### A line switches back to English halfway?

**That is normal.** The sentence is not in the corpus yet, so the original is shown. Report it to **LyuChaCha** on Discord or on [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues) and it will be filled; everyone gets it on their next launch.

### How to help

Press **F6 → Data → Export untranslated strings**. The folder with `captured.json` opens; look through the file, delete anything personal, then attach it to the [issue form](https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml) (**F6 → Data → How to submit**) or send it to **LyuChaCha** on Discord. You do not have to translate anything.

The mod never sends anything by itself. The export already leaves out player names and guild, party, shout and private chat.

### Install

| Requirement | Version |
|---|---|
| Minecraft | 1.21.11 |
| Loader | Fabric |
| Dependencies | [Wynntils](https://modrinth.com/mod/wynntils) 4.2+, [Fabric API](https://modrinth.com/mod/fabric-api) |

Put the jar in `mods/` and press **F6** in game.

### Sponsoring

Free, and it will stay free. [Ko-fi](https://ko-fi.com/lyuchacha): 3 USD/month or 10 USD one-off puts you on the sponsor list. No paid features.

### Licence

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE). Item and ability data come from the public CDN used by Wynntils. Dialogue-box glyphs use [Fusion Pixel](https://github.com/TakWolf/fusion-pixel-font) (SIL OFL 1.1).

**Not affiliated with Wynncraft or the Wynntils team.** A community translation project.

---

## 繁體中文

> **目前是 Beta。** 譯文大多由 AI 產出，只有一部分經過人工校稿，會有錯譯與用詞不一致。

### 翻譯範圍

| 內容 | 顯示方式 |
|---|---|
| 物品 tooltip | 旁邊另開面板，或寫進原本的 tooltip |
| 任務對話、選項 | 寫進 **Wynncraft 自己的對話框**（框、名牌、頭像不動），或另開小框 |
| NPC 名牌、漂浮字 | 注視時跳小框，或就地取代 |
| 任務追蹤 | 獨立小框 |
| 技能樹 | 五個職業的節點、說明與流派 |
| Major ID | 名稱與整段敘述 |
| Lootrun、討伐戰、地城 | 使命、增益、信標、Aspect、Gambit、戰利品面板 |
| 探索點 | 名稱、說明、祕密發現的故事 |
| 聊天與中央大字 | 伺服器訊息與畫面中央的大字（玩家發言不翻） |

裝備名稱預設保留原文，F6 可以打開。

### 其他功能

| 功能 | 說明 |
|---|---|
| 市集搜尋 | 在交易市集打譯名，送出前自動換回英文原名 |
| 從 GitHub 同步 | 譯文修正合併後下次進遊戲就生效，不必重新下載模組 |
| 匯出語料 | 沒翻到的句子匯出成檔案，自己看過再交給翻譯團隊（見下文） |
| 複製聊天 | 複製最近的聊天訊息，方便回報 |
| F9 截圖 | 把譯文面板複製到剪貼簿或存成檔案 |
| F6 設定 | 每一類文字各自的模式；譯文、輔助、介面語言；小框可拖曳、可改大小 |

純客戶端，伺服器不需要裝。

### 語言

| 語言 | 狀態 |
|---|---|
| 繁體中文 | 主要語言，所有內容 |
| 簡體中文 | 所有內容，進度與繁中相當 |
| 日文 | 所有內容，進度與繁中相當 |
| 俄文 | 介面、技能樹、名牌、物品敘述；任務對話翻譯中 |
| 韓文 | 只有物品欄位標籤與 Major ID |

在 F6 →「資料」切換，不必改遊戲語言。

### 翻譯進度

<!-- 進度:開始 -->
更新於 2026-09-15。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_cn` 简体中文 | █████████░ 93.9% | 40,096 / 42,722 |
| `zh_tw` 繁體中文 | █████████░ 93.8% | 41,135 / 43,835 |
| `ja_jp` 日本語 | █████████░ 93.7% | 40,013 / 42,722 |
| `ru_ru` Русский | █████████░ 93.3% | 39,866 / 42,722 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 1.8% | 778 / 42,714 |

各語言還缺哪些檔案 / Per-language breakdown: [PROGRESS.md](https://github.com/LyuChaCha/WynnChaYuan/blob/main/docs/PROGRESS.md)
<!-- 進度:結束 -->

### 翻到一半變回英文？

**這是正常的**，代表那一句還不在語料裡，所以顯示原文。回報給 Discord 的 **LyuChaCha** 或 [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues) 就會補上，所有人下次進遊戲就看得到。

### 怎麼幫忙

按 **F6 →「資料」→「匯出未翻譯字串」**，會打開放著 `captured.json` 的資料夾。看過檔案、刪掉個人資訊之後，附到 [Issue 表單](https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml)（**F6 →「資料」→「如何提交」**），或在 Discord 傳給 **LyuChaCha**。不用翻任何東西。

模組不會自動送出任何東西；匯出檔已經濾掉玩家名字與公會、隊伍、喊話、私訊。

### 安裝

| 需求 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| 載入器 | Fabric |
| 前置 | [Wynntils](https://modrinth.com/mod/wynntils) 4.2 以上、[Fabric API](https://modrinth.com/mod/fabric-api) |

jar 放進 `mods/`，進遊戲按 **F6**。

### 贊助

免費，也會一直免費。[Ko-fi](https://ko-fi.com/lyuchacha)：每月 3 USD 或單次 10 USD 以上列入贊助者名單。沒有付費功能。

### 授權

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE)。物品與技能資料取自 Wynntils 使用的公開 CDN；對話框字形使用 [Fusion Pixel](https://github.com/TakWolf/fusion-pixel-font)（SIL OFL 1.1）。

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。
