<p align="center">
  <img src="https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/docs/icon.png" width="180" alt="WynnChaYuan">
</p>

# WynnChaYuan — Wynncraft translation mod

> A Wynncraft translation mod that shows the translation **beside** the original
> instead of replacing it. Traditional Chinese is 98% done; seven more languages
> are open. Client-side. Requires Wynntils.

> [!IMPORTANT]
> **This is a beta.** Features and translations are still being worked on. If you
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

### Translations update themselves

Translations are not baked into the jar. The mod syncs the latest set from GitHub
at startup, so **once a translation is merged, everyone gets it the next time they
launch** — no new download needed.

Offline, it falls back to the last cached copy, then to the version bundled in the
jar. Losing your connection means "not the newest", never "no translations".

### Installing the mod helps finish it

Quest dialogue and NPC names **have no official data source**. There is no file to
scrape — somebody has to walk up to that NPC in game.

So the mod can collect the lines it could not translate. Turn on
**"收集未翻譯字串" / Collect untranslated strings** in F6, play normally, and it
writes them to `config/wynnchayuan/captured.json`. Attach that file to a
[GitHub issue](https://github.com/LyuChaCha/WynnChaYuan/issues) and those lines
become translatable for everyone.

> **Please glance at the file before attaching it.** The mod filters out player
> names, friend lists and coordinates, but Wynncraft has a lot of notification
> formats and something may slip through. Delete any line with somebody's name in
> it — and tell us, so the filter can be fixed.

You do not have to translate anything to help. Just playing with collection on
tells the project what players actually run into.

### Progress

<!-- 進度:開始 -->
更新於 2026-08-30。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | ██████████ 98.0% | 27,749 / 28,308 |
| `de_de` Deutsch | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `es_es` Español | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `fr_fr` Français | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `ja_jp` 日本語 | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `ru_ru` Русский | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `zh_cn` 简体中文 | ░░░░░░░░░░ 0.0% | 0 / 26,762 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
<!-- 進度:結束 -->

Everything you see minute to minute is done:

| | |
|---|---|
| Quest dialogue | 22,067 / 22,067 ✅ |
| Item and UI labels | 288 / 288 ✅ |
| Ability panel labels | 303 / 303 ✅ |
| Major IDs | 325 / 325 ✅ |
| Ingredients, materials, tomes, charms, aspects | all ✅ |
| Ability trees (5 classes) | 1,250 / 1,251 |
| Gear lore | 986 / 990 |
| Menus | 866 / 875 |
| Quest names | 221 / 336 |
| NPC names | 512 / 1,039 |
| Misc UI strings | 628 / 1,625 |

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

### Licence and credits

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE). Item and ability
data come from the public CDN used by Wynntils. Resource-pack glyphs and layout are
Wynncraft's; this mod only displays them.

**Not affiliated with Wynncraft or the Wynntils team.** A community translation project.

---

## 繁體中文

> [!IMPORTANT]
> **目前是 Beta 階段。** 功能與譯文都還在調整，遇到問題請到 Discord 找
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

### 翻譯會自己更新，不用重新下載模組

譯文不是寫死在 jar 裡的。模組啟動時會從 GitHub 同步最新版本，所以**譯者把翻譯
合併進去，所有人下次進遊戲就拿到了**。

離線或連不上時會用上次的快取，再不行才用 jar 內建的版本。斷網只是「沒有最新
的」，不會變成沒有翻譯。

### 裝了模組就是在幫忙

任務對話與 NPC 名稱**沒有官方資料可以抓**。沒有檔案可以爬，只能靠玩家在遊戲裡
真的走到那個 NPC 面前。

所以模組可以幫忙收集它翻不出來的句子。在 F6 打開**「收集未翻譯字串」**，照常
玩，它會把那些句子記進 `config/wynnchayuan/captured.json`。玩一段時間後把這個
檔[開 Issue](https://github.com/LyuChaCha/WynnChaYuan/issues) 附上來，那些句子
就變成大家共用的待翻條目。

> **附上前請先看一眼。** 模組會過濾玩家名稱、好友名單與座標，但 Wynncraft 的
> 通知格式很多，可能有漏網。看到別人的名字就刪掉那一條，順便回報一下，
> 我們補過濾規則。

**你不用翻任何東西也能幫上忙。** 光是開著收集玩，就等於告訴這個專案「玩家實際
會遇到什麼」。

### 目前進度

<!-- 進度:開始 -->
更新於 2026-08-30。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | ██████████ 98.0% | 27,749 / 28,308 |
| `de_de` Deutsch | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `es_es` Español | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `fr_fr` Français | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `ja_jp` 日本語 | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `ru_ru` Русский | ░░░░░░░░░░ 0.0% | 0 / 26,762 |
| `zh_cn` 简体中文 | ░░░░░░░░░░ 0.0% | 0 / 26,762 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
<!-- 進度:結束 -->

日常最常看到的部分已經翻完：

| | |
|---|---|
| 任務對話 | 22,067 / 22,067 ✅ |
| 物品與介面標籤 | 288 / 288 ✅ |
| 技能面板標籤 | 303 / 303 ✅ |
| Major ID | 325 / 325 ✅ |
| 素材、材料、典籍、護符、意象 | 全部 ✅ |
| 技能樹（五職業） | 1,250 / 1,251 |
| 裝備背景敘述 | 986 / 990 |
| 選單 | 866 / 875 |
| 任務名稱 | 221 / 336 |
| NPC 名稱 | 512 / 1,039 |
| 雜項介面字串 | 628 / 1,625 |

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

### 授權與致謝

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE)。物品與技能資料
取自 Wynntils 使用的公開 CDN；材質包符號與排版由 Wynncraft 提供，本模組僅顯示、
不修改。

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。

---

**[GitHub](https://github.com/LyuChaCha/WynnChaYuan)** · **[回報問題 / Issues](https://github.com/LyuChaCha/WynnChaYuan/issues)** · **[更新日誌 / Changelog](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CHANGELOG.md)**
