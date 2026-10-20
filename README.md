<p align="center">
  <img src="logo/2026/wynnchayuan-banner-animated-680x240.gif" width="680" alt="WynnChaYuan">
</p>

# WynnChaYuan

English: **[README.en.md](README.en.md)**

WynnChaYuan 是一個專門做 Wynncraft 多語言翻譯的開源公益專案（Fabric 1.21.11 的客戶端模組），會把遊戲裡的英文換成你的語言，**原文要不要留著由你自己決定**：譯文可以顯示在旁邊的面板，也可以直接寫進原本的位置。

**它會一直免費。以後也不會出現「付費才能用完整翻譯／功能」這種事。**

[下載](https://github.com/LyuChaCha/WynnChaYuan/releases/latest) ·
[官網](https://wynnchayuan.com/) ·
[功能介紹](https://wynnchayuan.com/features) ·
[披風一覽](https://wynnchayuan.com/capes) ·
[協作翻譯](https://wynnchayuan.com/translate) ·
[回饋與建議](https://wynnchayuan.com/feedback) ·
[更新日誌](CHANGELOG.md) ·
[回報問題](https://github.com/LyuChaCha/WynnChaYuan/issues) ·
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/wynnchayuan) ·
[Modrinth](https://modrinth.com/mod/wynnchayuan) ·
[Ko-fi](https://ko-fi.com/lyuchacha)

> [!IMPORTANT]
> **關於譯文。** 部分譯文由人工輸入再經 AI 轉出，一小部分直接由 AI 翻譯，
> 大部分基礎內容都經過人工校稿。其他語言目前基本上都是圍繞著繁體中文再做 AI 翻譯，
> 所以可能會有錯譯與用詞不一致的問題。
> 遇到問題請到 Discord 找 **LyuChaCha**，或到 [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues) 回報。

## 功能

### 翻譯顯示

- **顯示方式自由選**：物品說明、任務對話、對話選項、名牌與漂浮字、聊天訊息等，都可以各自選擇**關閉**、**就地取代**（直接寫進原本的位置）或**另開面板**（譯文顯示在旁邊，原文不動）。
- **寫進 Wynncraft 自己的對話框**：任務對話與選項可以直接寫進原本的對話框，邊框、名字、頭像都保留，逐字打出的節奏跟原文同步；太長塞不下的句子改用跑馬燈捲動。也可以改成另開小框。
- **面板位置自己決定**：每個小框都能拖曳定位；對話、選項與任務追蹤還能從角落拉動改大小。
- **輔助語言**：你的語言還沒翻到的句子，可以改顯示另一種語言，或留著原文。
- **名稱要不要翻**：任務、洞窟、探索點這些內容的名稱，可以選譯名、譯名加原文，或保留原文。裝備名稱預設保留原文（那是專有名詞，交易市場與 wiki 都用英文），想看譯名可以在設定裡打開；按住 Shift 可以暫時看另一種名稱。
- **譯文自動更新**：新翻譯合併進 GitHub 之後，**不用更新模組**就拿得到。第一次開遊戲會自動下載；之後每次開遊戲都會檢查，有新譯文就在聊天室提示一行，點「按這裡更新」馬上生效。到設定「資料」打開「自動更新翻譯」，開遊戲時就會自己下載、不再詢問。沒網路時用上次下載的版本（或 jar 內建的那一份）。

### 翻譯範圍

| 內容 | 翻了什麼、怎麼顯示 |
|---|---|
| 物品說明 | 裝備敘述、屬性、Major ID；旁邊另開面板，或寫進原本的說明框。未鑑定物品的可能性清單附原文 |
| 技能樹 | 五個職業的節點、說明與流派 |
| 任務對話、對話選項 | 寫進 **Wynncraft 自己的對話框**（框、名牌、頭像不動），也可另開小框 |
| 任務追蹤 | 獨立小框，抬頭照實際追蹤的東西顯示（任務、世界事件、洞窟、討伐戰…） |
| NPC 名牌、漂浮字 | 注視時跳小框，或就地取代；工作站、互動提示與商店倒數看板也算 |
| 聊天訊息 | 伺服器發的訊息（任務完成、獎勵、進出區域、世界事件開始、在聊天欄出現的 NPC 台詞），Lootrun 結算、信標這類分欄訊息保持對齊。隊友給的增益、死亡訊息、丟炸彈這類帶別人名字的公告也翻，名字原樣保留；登入的歡迎訊息逐行翻、保留顏色。玩家發言與 Tab 玩家清單的名字不翻 |
| 記分板 | 畫面右邊那一欄的討伐戰目標、Lootrun、隊伍與公會戰；比框寬的譯文會換行 |
| 選單與介面 | 交易市場、公會、商城等畫面 |
| Lootrun、討伐戰、地城 | 使命、加持、信標、Aspect、Gambit、結算與戰利品面板、地城名稱與鑰匙 |
| 探索點、祕密發現 | 名稱、說明與故事 |
| 畫面中央大字 | 標題與副標題 |

### 市集搜尋

在交易市場的搜尋欄**直接打你的語言**就能查：模組會自動送出英文名稱，打字的同時列出符合的結果（↑↓ 選、Tab 填入）。

### 其他模組支援

- **[WynnMarketSearch](https://modrinth.com/mod/wynnmarketsearch)**：它的搜尋欄可以打譯名，結果在英文名旁邊並排顯示譯名。
- **[Wynnventory](https://modrinth.com/mod/wynnventory)**：價格框、獎勵畫面、設定畫面都有翻，按住 Shift 看原文。價格框照實際顯示的物品說明擺放，太長會縮小；開啟它的「固定在左側」時，物品說明會讓開。

完整的相容清單見下面的[模組相容](#模組相容)。

### 附帶的工具

- **複製聊天**：列出最近的聊天訊息，點一則就複製，回報時可以直接貼上英文原文（按鍵預設沒綁）。
- **譯文截圖**：把翻譯面板複製到剪貼簿或存成圖片（按鍵預設沒綁）。
- **遊戲內更新**：有新版時在聊天室提示一次，點那一則的「點這裡直接更新」（等同輸入 `/wcy update`），或在更新說明的畫面按「直接更新」，就會從 GitHub Releases 下載，關遊戲時自動換上。只有你按了才會下載。
- **加速 Wynntils 物品繪製**：開背包、銀行、市集時更順，畫面不變（F6「功能」，預設開啟）。
- **披風與葉子**：可以穿上自己的披風，名字前面也有一片葉子，見[披風、灰色葉子與會送出的資料](#披風灰色葉子與會送出的資料)。

**純客戶端**，伺服器不需要裝。

### 設定

三種開法，挑順手的：

- 聊天輸入 `/wcy`（或 `/wynnchayuan`）
- [Mod Menu](https://modrinth.com/mod/modmenu) 的模組清單 → WynnChaYuan → 設定
- 到遊戲的「按鍵設定」→ WynnChaYuan，自己綁一個鍵（**預設不綁**，不佔你的按鍵）

| 分頁 | 內容 |
|---|---|
| 物品 | 物品翻譯（另開面板／就地取代／關閉）、翻譯物品名稱、市集搜尋、譯文截圖、模組支援 |
| 面板 | 跟隨滑鼠或固定、放在哪一側、間距、調整面板位置；風格顏色（設定畫面自己的主題色）與框線顏色（遊戲裡小框的框）分開設 |
| 對話 | 任務對話、對話選項（另開小框／就地取代／關閉）、停留秒數、對話／追蹤小框 |
| 世界與聊天 | 名牌與漂浮字（含偵測距離與準心夾角）、聊天訊息、記分板、畫面中央大字、複製聊天 |
| 資料 | 譯文語言、輔助語言、介面語言、譯文來源、自動更新翻譯、更新翻譯、譯文版本、收集未翻譯字串、收集介面文字、匯出未翻譯字串、如何提交、診斷檔 |
| 功能 | 我的徽章（葉子要展示哪幾個身分、顏色、彩色模式）、披風（顯示披風、穿上我的披風、披風一覽）、效能（加速 Wynntils 物品繪製） |

滑鼠移到項目上會顯示說明；左上可以搜尋設定，右邊的預覽會跟著你調的值即時變化。

## 披風、灰色葉子與會送出的資料

- **披風**：可以穿上自己的 WynnChaYuan 披風，其他裝了本模組的玩家看得到。到 <https://wynnchayuan.com> 上傳高清披風（F6「功能」→「披風一覽」會順便替你登入），或在[披風一覽](https://wynnchayuan.com/capes)看到喜歡的直接穿上，隨時能換回自己原本那一件。PNG 大家都能傳（1 MB 以內）；GIF 與 20 MB 的上限給開發者、贊助者、貢獻者。要照 [`capes/templates`](https://github.com/LyuChaCha/WynnChaYuan/tree/main/capes/templates) 的範本畫，最大 1056×816。超過 7 天沒有人穿的披風會被移除。
- **葉子**：裝了本模組的玩家，名字前面都有一片葉子（沒有身分的是灰色），彼此看得到。開發者的彩色模式、要展示哪幾個身分的葉子，別人看到的跟你設的一樣。
- **這兩項需要連到我們的伺服器（wynnchayuan.com）**。模組在你進 Wynncraft 時報到一次，送出的是：你的 Minecraft UUID、Mojang 簽過的公開個人檔案（名字與外觀）、用來證明帳號是本人的公鑰與簽章、要不要顯示披風，以及徽章的設定（葉子要不要彩色、要展示哪幾個身分）。上傳披風時另外送出你選的那張圖。**私鑰與登入憑證不會離開遊戲；不送所在的世界、聊天或任何遊戲內容，伺服器也不記 IP。** 公開的名單上只有雜湊過的代碼，沒有 UUID 與名字。
- **這一項沒有開關**：只要裝了模組、進了 Wynncraft 就會報到，所以每個使用者都有葉子。不想讓別人看到自己的披風，可以在 F6「功能」關掉「穿上我的披風」。
- **網站帳號**（選用）：在 wynnchayuan.com 用 Discord 註冊後，在遊戲裡輸入 `/wcy link 綁定碼` 把遊戲 ID 綁上去。我們只讀 Discord 的帳號代號與顯示名稱。
- **直接更新**：只有你按了「直接更新」（或輸入 `/wcy update`）才會從 GitHub Releases 下載新版，關遊戲時換上。

完整的說明見網站的[隱私政策](https://wynnchayuan.com/privacy)。

## 支援語言與進度

| 語言 | 內容 |
|---|---|
| `zh_tw` 繁體中文 | 主要語言，所有內容 |
| `zh_cn` 简体中文 | 所有內容，含任務對話 |
| `ja_jp` 日本語 | 所有內容，含任務對話 |
| `ko_kr` 한국어 | 所有內容，含任務對話 |
| `ru_ru` Русский | 所有內容，含任務對話 |
| `es_es` Español | 所有內容，含任務對話 |
| `tr_tr` Türkçe | 所有內容，含任務對話 |

在 **設定 →「資料」** 切換，不必改遊戲語言，也不用重開：

- **譯文語言**：看哪一種語言的翻譯
- **輔助語言**：譯文語言還沒翻到的句子改顯示哪一種（或原文）
- **介面語言**：設定畫面本身的語言（另有英文）

### 翻到哪裡了

目前以繁體中文為主，其他語言跟著繁體中文補齊。

- **已完成**：官方記載、CDN 與 Wynntils 有收錄的文字大部分都已經翻完，包含物品說明、技能樹、任務對話與選項、任務追蹤、NPC 名牌、系統聊天訊息、市集與公會等介面、Lootrun／討伐戰／地城，以及畫面中間的大標題。
- **尚未完成**：Secret 相關內容、非任務 NPC 的對話、還沒被發現的文案、原始記載本身有錯的語料，以及遊戲最新一次更新的內容。這些會陸續補上，**遇到的時候回報會快很多**（見[怎麼幫忙](#怎麼幫忙)）。

<!-- 進度:開始 -->
更新於 2026-10-20 / Updated 2026-10-20

**翻了哪些**：任務對話與任務書、物品（名稱、詞條、敘述、Major ID）、技能樹、介面（設定畫面、背包、交易市場、公會、地圖、追蹤欄）、NPC 與地區名稱、看板與聊天公告。

**大概翻到哪**：7 種語言目前都在 **95% 以上**，繁體中文最完整。剩下的多半是零星的名稱與半句話，而且遊戲還在更新——**一定還有漏的**。看到沒翻、翻錯或版面跑掉的，[開個 issue](https://github.com/LyuChaCha/WynnChaYuan/issues) 告訴我們就好。

**What's covered**: quest dialogue and the quest book, items (names, stats, lore, Major IDs), the ability tree, the interface (settings, inventory, trade market, guild, map, tracker), NPC and place names, signs and chat announcements.

**Roughly how far**: every language is past **95%**, Traditional Chinese being the most complete. What is left is mostly stray names and half-sentences, and the game keeps changing — **there will be gaps**. Found something untranslated, wrong, or laid out badly? [Open an issue](https://github.com/LyuChaCha/WynnChaYuan/issues).

每一種語言還缺哪些檔案 / Per-language breakdown: [PROGRESS.md](docs/PROGRESS.md)
<!-- 進度:結束 -->

## 安裝

| 需求 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| 載入器 | Fabric |
| 前置 | [Wynntils](https://modrinth.com/mod/wynntils) 4.2 以上、[Fabric API](https://modrinth.com/mod/fabric-api) |

1. 下載 jar：[GitHub Releases](https://github.com/LyuChaCha/WynnChaYuan/releases/latest) ·
   [Modrinth](https://modrinth.com/mod/wynnchayuan) ·
   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/wynnchayuan)
2. 放進 `mods/`。進遊戲後在聊天輸入 `/wcy`，或從 Mod Menu 開設定。

之後有新版時，遊戲裡會提示，按「直接更新」就能換上；譯文則不必等新版，見上面的「譯文自動更新」。

### 模組相容

| 模組 | 關係 | 說明 |
|---|---|---|
| [Wynntils](https://modrinth.com/mod/wynntils) 4.2 以上 | **必要** | 物品、任務、聊天的資料都從它來；它自己的畫面（綜合頁面、地圖、任務與洞穴名稱）也會翻 |
| [Fabric API](https://modrinth.com/mod/fabric-api) | **必要** | |
| [Mod Menu](https://modrinth.com/mod/modmenu) | 選裝，有支援 | 模組清單上多一顆設定按鈕 |
| [WynnMarketSearch](https://modrinth.com/mod/wynnmarketsearch) | 選裝，有支援 | 它的搜尋面板打譯名也找得到，結果列在英文名旁邊附上譯名；送出去的仍是英文名 |
| [Wynnventory](https://modrinth.com/mod/wynnventory) | 選裝，有支援 | 它的價格框、獎勵畫面與設定畫面也翻成譯文（物品名跟著「翻譯物品名稱」的設定）；按住 Shift 看原文。價格框照實際顯示的物品說明擺放，開了「固定在左側」時物品說明會讓開。有裝才會在設定裡出現開關 |
| [WynnMod](https://modrinth.com/mod/wynnmod) | 相容 | 它重畫的裝備說明也會翻成譯文（設定「物品」→「模組支援」可以關掉）。它用來判斷討伐戰結果的那幾句話，裝了它的時候保留英文，連勝才不會算錯（沒裝的人照常看到譯文） |
| 會在 tooltip 加區塊的模組（Nori、Wynnpool…） | 相容 | 那些區塊照原樣留著。翻譯面板因此比原文多出幾行時，把區塊標籤填進 `config/wynnchayuan/third-party-sections.json` 就會濾掉 |

沒列在上面的模組多半也能一起用：本模組只在客戶端換畫面上的字。遇到衝突請[開 Issue](https://github.com/LyuChaCha/WynnChaYuan/issues) 並附上模組清單。

## 翻到一半變回英文？

**這是正常的**，代表那一句還不在語料裡，模組找不到譯文就顯示原文。

照下面的[怎麼幫忙](#怎麼幫忙)告訴我們，補好之後所有人下次進遊戲就看得到，不必更新模組。

## 怎麼幫忙

翻譯讀起來怪、用詞想統一、想要新功能，都歡迎丟上來。**不用會翻譯，也不用會寫程式**：

- **截圖回報**：看到還是英文、翻錯或版面跑掉的地方，截圖給我們就幫很大了。聊天訊息可以用「複製聊天」直接複製原文，翻譯面板可以用「譯文截圖」存成圖片。
- **匯出未翻譯字串**：任務對話與 NPC 名牌沒有官方資料可抓，只能靠玩家在遊戲裡遇到。
  1. **設定 →「資料」→「匯出未翻譯字串」**，會打開 `config/wynnchayuan/export`，裡面是 `captured.json`。
  2. 打開檔案看一遍，刪掉任何個人資訊（別人的名字、公會名、私人對話）。
  3. **設定 →「資料」→「如何提交」**打開 [Issue 表單](https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml)，把檔案拖進去；或在 Discord 傳給 **LyuChaCha**。
- **[協作翻譯](https://wynnchayuan.com/translate)**：搜尋你在遊戲裡看到的句子、只看還沒翻的條目。各語言的譯者可以在這裡提出更好的譯法、對有爭議的術語投票，管理員定案後會送進語料（譯者由管理員指定，想加入可以在 Discord 找 **LyuChaCha**）。
- **[回饋與建議](https://wynnchayuan.com/feedback)**：用 Discord 登入後就能送出譯文建議、內容建議、網站建議與功能建議；也可以在 Discord 用機器人的 `/feedback`（繁中介面是 `/回饋`）。譯文建議會自動在 GitHub 開一個公開的 Issue，上面會顯示你的 Discord 顯示名稱。
- **[GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues)**：翻錯、用詞不對、當機或畫面壞掉都可以回報，附上英文原文、log 與截圖最好。
- **Pull Request**：直接改譯文或程式，見下面的[參與翻譯](#參與翻譯)。
- **Discord**：找 **LyuChaCha**。

模組**不會自動送出遊戲裡的文字**（披風與葉子會送出什麼，見上面「披風、灰色葉子與會送出的資料」）。匯出檔已經濾掉玩家名字與公會、隊伍、喊話、私訊，但濾網是猜的，送出前請自己看過。

## 參與翻譯

不需要會寫程式：譯文是 JSON，在 GitHub 網頁上填 `dst` 再開 PR 就行。各語言的譯者也可以直接在[協作翻譯](https://wynnchayuan.com/translate)頁提案與投票，不必碰 GitHub。

- [CONTRIBUTING.md](CONTRIBUTING.md)：流程、佔位符、顏色標記
- [GLOSSARY.md](GLOSSARY.md)：專有名詞對照
- [給翻譯團隊](docs/for-translators.md)：最近的慣例與改動

想在遊戲裡試自己的譯文：到設定把「譯文來源」切成**本機**，改完按「更新翻譯」那一列的「重讀本機檔」。

## 網站與 Discord 機器人

[wynnchayuan.com](https://wynnchayuan.com/) 有八種語言：

| 頁面 | 內容 |
|---|---|
| [首頁](https://wynnchayuan.com/) | 下載、安裝說明、團隊與贊助 |
| [功能](https://wynnchayuan.com/features) | 翻譯範圍與附帶的工具，附即時預覽 |
| [披風](https://wynnchayuan.com/capes) | 披風一覽：看大家的披風、加入最愛、直接穿上 |
| [帳號](https://wynnchayuan.com/account) | 用 Discord 註冊、綁定遊戲 ID、上傳披風 |
| [協作翻譯](https://wynnchayuan.com/translate) | 搜尋語料、提案、術語投票 |
| [回饋](https://wynnchayuan.com/feedback) | 回饋與建議 |

**Discord 機器人**（在有加入機器人的伺服器裡使用）：

| 指令 | 用途 |
|---|---|
| `/status`（`/狀態`） | 網站狀態：玩家、披風、待審核、協作翻譯 |
| `/cape`（`/披風`） | 看某位玩家現在的 WynnChaYuan 披風 |
| `/translate`（`/翻譯`） | 搜尋語料的原文或譯文 |
| `/feedback`（`/回饋`） | 給團隊的回饋或建議，跟網站的回饋頁送到同一個地方 |

## 從原始碼建置

```bash
gradle build
```

需要 JDK 21。Wynntils 沒有 Maven 座標，要先從 [Modrinth](https://modrinth.com/mod/wynntils/versions)
下載 **Fabric 版** jar 放進 `libs/`（缺了建置會停下並說明）。語料檢查：`python tools/validate.py`。

## 贊助

**翻譯免費，也會一直免費。** 所有譯文與翻譯功能都由團隊無償提供，以後也不會出現「付費才能用完整翻譯／功能」這種事。

贊助完全出於自願：

- **不贊助，翻譯品質不會打折。** 每個人拿到的譯文、更新與修正都是同一份，不會因為沒贊助而變少或變慢。
- **贊助者多的只有趣味性的東西**：名字前面的青藍色葉子、列在名單上，以及 GIF 披風、20 MB 的披風上限這類裝飾性功能。
- **贊助者的意見會優先採納**：回報與建議我們會優先看、優先處理。

想請我們喝杯茶：**<https://ko-fi.com/lyuchacha>**

單次 **20 USD** 或每月 **2 USD** 以上列入贊助者名單：出現在下方、設定 →「關於／貢獻者」，名字前面也會多一片青藍色的葉子。

## 身分與徽章

名字前面那片葉子的顏色，看你在名單上的身分。名單就是下面「翻譯團隊」那一份，由維護者確認後加入，需要附上 Minecraft ID。

| 身分 | 葉子 | 怎麼成為 |
|---|---|---|
| 開發者 | 金色 | 協助程式的開發與維護 |
| 贊助者 | 青藍色 | 單次 **20 USD** 或每月 **2 USD** 以上（[Ko-fi](https://ko-fi.com/lyuchacha)） |
| 貢獻者 | 綠色 | 符合下面任何一項 |
| 其他裝了模組的人 | 灰色 | —（裝了模組就有，其他裝了模組的玩家看得到） |

**貢獻者的標準**——看有沒有被採用，不看數量：

- **為自己的語言貢獻譯文**：補上缺的譯文，或交來的 `captured.json` 被用來補齊，並合併進主線。
- **正確的修正**：指出錯譯、漏譯或用詞不一致，經確認後修正。
- **程式方面的實質協助**：被合併的修正或功能，或是能重現問題、幫忙找到原因的回報。

在名單上的人可以到 設定 →「功能」調整自己那片葉子的顏色；有好幾個身分的人可以挑要放哪幾片，最少一片。

## 翻譯團隊

同一份名單也在遊戲內 **設定 →「關於／貢獻者」**。要加人改 [`credits.json`](src/main/resources/assets/wynnchayuan/credits.json)。

感謝繁中／簡中社群一路以來的幫忙。

<!-- credits:begin -->

<!-- 這一段由 tools/sync-credits.py 從 credits.json 產生，不要手動改。 -->

### 開發者

| 名稱 | Minecraft ID |
|---|---|
| LyuChaCha | `Green_teaTW` |

### 贊助者

| 名稱 | Minecraft ID |
|---|---|
| ㄉ綠 | `MlyuL` |
| 蛇蛇 | `snakiesnake` |
| hey you great | `Brokeboys_` |
| Zero | `HEYAZero` |

### 貢獻者

| 名稱 | Minecraft ID |
|---|---|
| suSCP | `SCP_Night_sky` |
| 隨意 | `brine7459` |
| Chicken_sky | `Chicken_sky` |
| Pure | `21_Pure` |
| 泥巴先生 | `MrMud8033112` |
| 幻影Joker | `NOT_Joker` |
| Pootato | `Pootato__` |
| N02sAyLa | `eric18960` |
| 鳥鳥 | `Smellybird_` |
| 98 | `Jackandmina98` |
| Jimmy | `0110jimmy` |
| 雪花 | `ThEsnowF` |
| Roy | `aaroye` |
| Chq | `Chqrish` |
| 邊緣安德 | `Enderchen2580` |
| AdoreFuSei | `AdoreFuSei` |
| anilken | `Pandik` |
| yool | `yool141` |
| 早安白月 | `ZLGX` |
| 百里莫凌 | `BaiLiML` |

### 資料來源

| 名稱 | Minecraft ID |
|---|---|
| Wynntils（物品／技能 CDN） | — |
| Wynncraft | — |

翻一條就會出現在這裡。見 [CONTRIBUTING.md](CONTRIBUTING.md)。

<!-- credits:end -->

## 連結

- 官網：<https://wynnchayuan.com/>
- 功能介紹：<https://wynnchayuan.com/features>
- 披風一覽：<https://wynnchayuan.com/capes>
- 協作翻譯：<https://wynnchayuan.com/translate>
- 回饋與建議：<https://wynnchayuan.com/feedback>
- GitHub：<https://github.com/LyuChaCha/WynnChaYuan>
- CurseForge：<https://www.curseforge.com/minecraft/mc-mods/wynnchayuan>
- Modrinth：<https://modrinth.com/mod/wynnchayuan>
- Ko-fi（自由贊助，翻譯永遠免費）：<https://ko-fi.com/lyuchacha>
- Discord：**LyuChaCha**

## 來源與授權

- 物品與技能資料：[Wynntils](https://github.com/Wynntils/Wynntils) 使用的公開 CDN
- 任務與祕密發現清單：[Wynncraft Wiki](https://wynncraft.wiki.gg/)（CC BY-SA）
- 對話框的中日韓與俄文字形：[Fusion Pixel 10px](https://github.com/TakWolf/fusion-pixel-font)（SIL OFL 1.1，全文見
  `src/main/resources/assets/wynnchayuan/font/ofl-fusion.txt`）。字型缺字的句子維持原文，不畫方框
- 材質包符號與排版屬於 Wynncraft，本模組只顯示、不修改
- 程式碼：[GNU AGPLv3 或之後的版本](LICENSE)
- 翻譯與資料（`src/main/resources/assets/wynnchayuan/translations/` 的語料、`GLOSSARY.md` 等）：[CC BY-NC-SA 4.0](LICENSE-DATA)。
  取自 Wynncraft Wiki 的內容仍依原本的 CC BY-SA

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。
