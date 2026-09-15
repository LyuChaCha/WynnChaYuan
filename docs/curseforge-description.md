![WynnChaYuan](https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/docs/icon.png)

# WynnChaYuan

**A multi-language translation mod for Wynncraft.** The original text is kept; the translation is shown beside it or written in its place.

**Wynncraft 多語言翻譯模組。** 原文保留，譯文顯示在旁邊，或直接寫進原本的位置。

**[GitHub](https://github.com/LyuChaCha/WynnChaYuan)** · **[Report a line / 回報](https://github.com/LyuChaCha/WynnChaYuan/issues)** · **[Changelog / 更新日誌](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CHANGELOG.md)** · **[Ko-fi](https://ko-fi.com/lyuchacha)** · Discord: **LyuChaCha**

---

## English

> **Beta.** The translations are mostly AI-generated and only partly proofread. Expect mistranslations and inconsistent terms.

### What it translates

- **Item tooltips**: a panel beside the tooltip, or written into the tooltip itself
- **Quest dialogue and choices**: inside Wynncraft's own dialogue box (frame, nameplate and portrait kept), or a separate box
- **NPC nameplates and floating text**: a box while you look at one, or replaced in place
- **Quest tracker**, **ability trees** (all five classes) and **Major IDs**
- **Lootruns, raids and dungeons**: missions, boons, beacons, aspects, gambits, loot panels
- **Discoveries**, **server chat messages** and **title text** (player chat is never translated)

Gear names stay in English by default; they can be turned on in F6.

### Other features

- **Market search in your language**: type the translated name in the trade market, the English one is sent
- **Translations sync from GitHub**: fixes arrive on your next launch, no new download
- **Corpus sharing**: untranslated lines go to the translation team (see below)
- **Copy chat**: copy recent chat lines for a report
- **F9**: screenshot of the translation panel
- **F6 settings**: a mode for each kind of text; translation, fallback and interface language; draggable and resizable boxes

Client-side only. The server does not need it.

### Languages

- **Traditional Chinese**: main language, everything
- **Simplified Chinese** and **Japanese**: everything, on par with Traditional Chinese
- **Russian**: interface, ability trees, nameplates, item lore; quest dialogue in progress
- **Korean**: item tooltip labels and Major IDs only

Switch under F6 → Data, without changing the game's language.

### Progress

<!-- 進度:開始 -->
更新於 2026-09-15。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_cn` 简体中文 | █████████░ 93.7% | 39,854 / 42,537 |
| `zh_tw` 繁體中文 | █████████░ 93.7% | 40,893 / 43,650 |
| `ja_jp` 日本語 | █████████░ 93.5% | 39,771 / 42,537 |
| `ru_ru` Русский | ████░░░░░░ 40.8% | 17,348 / 42,537 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 1.5% | 621 / 42,529 |

各語言還缺哪些檔案 / Per-language breakdown: [PROGRESS.md](https://github.com/LyuChaCha/WynnChaYuan/blob/main/docs/PROGRESS.md)
<!-- 進度:結束 -->

### A line switches back to English halfway?

**That is normal.** The sentence is not in the corpus yet, so the original is shown. Report it to **LyuChaCha** on Discord or on [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues) and it will be filled; everyone gets it on their next launch.

### Installing it helps

**F6 → Data → Share with the translation team** is on by default. Lines you run into that have no translation are sent to the team and become translations for everyone. You do not have to translate anything.

Only the game's own English text is sent. Never your account, coordinates, or anything other players typed; player names are filtered out. You can turn it off at any time.

### Install

- Minecraft **1.21.11**, **Fabric**
- [Wynntils](https://www.curseforge.com/minecraft/mc-mods/wynntils) **4.2+**
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

Put the jar in `mods/` and press **F6** in game.

### Sponsoring

Free, and it will stay free. [Ko-fi](https://ko-fi.com/lyuchacha): 3 USD/month or 10 USD one-off puts you on the sponsor list. No paid features.

### Licence

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE). Item and ability data come from the public CDN used by Wynntils. Dialogue-box glyphs use Fusion Pixel (SIL OFL 1.1).

**Not affiliated with Wynncraft or the Wynntils team.** A community translation project.

---

## 繁體中文

> **目前是 Beta。** 譯文大多由 AI 產出，只有一部分經過人工校稿，會有錯譯與用詞不一致。

### 翻譯範圍

- **物品 tooltip**：旁邊另開面板，或寫進原本的 tooltip
- **任務對話與選項**：寫進 Wynncraft 自己的對話框（框、名牌、頭像不動），或另開小框
- **NPC 名牌與漂浮字**：注視時跳小框，或就地取代
- **任務追蹤**、**技能樹**（五個職業）與 **Major ID**
- **Lootrun、討伐戰、地城**：使命、增益、信標、Aspect、Gambit、戰利品面板
- **探索點**、**伺服器聊天訊息**與**畫面中央大字**（玩家發言不翻）

裝備名稱預設保留原文，F6 可以打開。

### 其他功能

- **市集搜尋**：在交易市集打譯名，送出前自動換回英文原名
- **譯文從 GitHub 同步**：修正合併後下次進遊戲就生效，不必重新下載模組
- **分享語料**：沒翻到的句子送回翻譯團隊（見下文）
- **複製聊天**：複製最近的聊天訊息，方便回報
- **F9**：譯文面板截圖
- **F6 設定**：每一類文字各自的模式；譯文、輔助、介面語言；小框可拖曳、可改大小

純客戶端，伺服器不需要裝。

### 語言

- **繁體中文**：主要語言，所有內容
- **簡體中文**、**日文**：所有內容，進度與繁中相當
- **俄文**：介面、技能樹、名牌、物品敘述；任務對話翻譯中
- **韓文**：只有物品欄位標籤與 Major ID

在 F6 →「資料」切換，不必改遊戲語言。

### 翻譯進度

<!-- 進度:開始 -->
更新於 2026-09-15。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_cn` 简体中文 | █████████░ 93.7% | 39,854 / 42,537 |
| `zh_tw` 繁體中文 | █████████░ 93.7% | 40,893 / 43,650 |
| `ja_jp` 日本語 | █████████░ 93.5% | 39,771 / 42,537 |
| `ru_ru` Русский | ████░░░░░░ 40.8% | 17,348 / 42,537 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 1.5% | 621 / 42,529 |

各語言還缺哪些檔案 / Per-language breakdown: [PROGRESS.md](https://github.com/LyuChaCha/WynnChaYuan/blob/main/docs/PROGRESS.md)
<!-- 進度:結束 -->

### 翻到一半變回英文？

**這是正常的**，代表那一句還不在語料裡，所以顯示原文。回報給 Discord 的 **LyuChaCha** 或 [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues) 就會補上，所有人下次進遊戲就看得到。

### 裝了就是在幫忙

**F6 →「資料」→「分享給翻譯團隊」**預設開啟。你遇到、還沒翻到的句子會送回翻譯團隊，翻好之後變成所有人的譯文。不用翻任何東西。

只送遊戲自己的英文字，不送帳號、座標，也不送別的玩家打的字；玩家名字會被濾掉。隨時可以關。

### 安裝

- Minecraft **1.21.11**、**Fabric**
- [Wynntils](https://www.curseforge.com/minecraft/mc-mods/wynntils) **4.2 以上**
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

jar 放進 `mods/`，進遊戲按 **F6**。

### 贊助

免費，也會一直免費。[Ko-fi](https://ko-fi.com/lyuchacha)：每月 3 USD 或單次 10 USD 以上列入贊助者名單。沒有付費功能。

### 授權

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE)。物品與技能資料取自 Wynntils 使用的公開 CDN；對話框字形使用 Fusion Pixel（SIL OFL 1.1）。

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。
