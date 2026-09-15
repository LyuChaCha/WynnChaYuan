<p align="center">
  <img src="docs/icon.png" width="160" alt="WynnChaYuan">
</p>

# WynnChaYuan

English: **[README.en.md](README.en.md)**

Wynncraft 的多語言翻譯模組。**原文保留**，譯文顯示在旁邊，或直接寫進原本的位置。

> [!IMPORTANT]
> **目前是 Beta。** 譯文大多由 AI 產出，只有一部分經過人工校稿，會有錯譯與用詞不一致。
> 遇到問題請到 Discord 找 **LyuChaCha**，或到 [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues) 回報。

## 翻譯範圍

| 內容 | 顯示方式 |
|---|---|
| 物品 tooltip | 旁邊另開面板，或寫進原本的 tooltip |
| 任務對話、對話選項 | 寫進 **Wynncraft 自己的對話框**（框、名牌、頭像不動），或另開小框；兩者分開設 |
| NPC 名牌、漂浮字 | 注視時跳小框，或就地取代；工作站與「空手右鍵」提示也算 |
| 任務追蹤 | 獨立小框，抬頭照實際追蹤的東西顯示（任務、世界事件、洞窟、討伐戰…） |
| 技能樹 | 五個職業的節點、說明與流派 |
| Major ID | 名稱與整段敘述 |
| Lootrun、討伐戰、地城 | 使命、增益、信標、Aspect、Gambit、結算與戰利品面板、地城名稱 |
| 探索點、祕密發現 | 名稱、說明與故事 |
| 聊天訊息 | 伺服器發的訊息（任務完成、獎勵、進出區域）；就地取代或原文加譯文。玩家發言不翻 |
| 畫面中央大字 | 標題與副標題 |

裝備名稱預設保留原文——那是專有名詞，交易市場與 wiki 都用英文。F6 可以打開。

## 其他功能

| 功能 | 說明 |
|---|---|
| 市集搜尋 | 在交易市集用你的語言搜尋，送出前自動換回英文原名；打字時列出候選，↑↓ 選、Tab 填入 |
| 譯文自動同步 | 譯文從 GitHub 同步，修正合併後下次進遊戲就生效，**不必重新下載模組**。離線時用上次的快取或 jar 內建版本 |
| 分享語料 | 沒翻到的句子送回翻譯團隊，預設開啟（見[裝了就是在幫忙](#裝了就是在幫忙)） |
| 複製聊天 | 列出最近的聊天訊息，點一則複製，方便回報（按鍵預設沒綁） |
| 譯文截圖 | **F9** 把翻譯面板複製到剪貼簿或存成檔案（可改綁） |
| 面板調整 | 所有小框都能拖曳定位；對話、選項、任務追蹤可拉右下角改大小；面板可跟隨滑鼠或固定、改框線顏色 |
| 更新提示 | 有新版時在聊天室提示一次；F6 →「更新說明」看各版改了什麼 |
| 貢獻者標記 | 名單上的人，名牌上方多一行標記，只有裝了本模組的人看得到（可關） |

**純客戶端。** 伺服器不需要裝。

## F6 設定

| 分頁 | 內容 |
|---|---|
| 物品 | 物品翻譯（另開面板／就地取代／關閉）、翻譯物品名稱、市集搜尋、譯文截圖 |
| 面板 | 跟隨滑鼠或固定、放在哪一側、間距、框線顏色、調整面板位置 |
| 對話 | 任務對話、對話選項（另開小框／就地取代／關閉）、停留秒數、對話／追蹤小框 |
| 世界與聊天 | 名牌與漂浮字（含偵測距離與準心夾角）、聊天訊息、畫面中央大字、複製聊天 |
| 資料 | 譯文語言、輔助語言、介面語言、譯文來源、重新載入、收集未翻譯字串、收集介面文字、分享給翻譯團隊、診斷檔 |

滑鼠移到項目上會顯示說明。

## 語言

| 語言 | 狀態 |
|---|---|
| `zh_tw` 繁體中文 | 主要語言，所有內容 |
| `zh_cn` 简体中文 | 所有內容，進度與繁中相當 |
| `ja_jp` 日本語 | 所有內容，進度與繁中相當 |
| `ru_ru` Русский | 介面、技能樹、名牌、物品敘述；任務對話翻譯中 |
| `ko_kr` 한국어 | 只有物品欄位標籤與 Major ID |

在 **F6 →「資料」** 切換，不必改遊戲語言，也不用重開：

- **譯文語言**：看哪一種語言的翻譯
- **輔助語言**：譯文語言還沒翻到的句子改顯示哪一種（或原文）
- **介面語言**：F6 設定畫面本身的語言（另有英文）

## 翻譯進度

<!-- 進度:開始 -->
更新於 2026-09-15。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_cn` 简体中文 | █████████░ 93.7% | 39,854 / 42,537 |
| `zh_tw` 繁體中文 | █████████░ 93.6% | 40,463 / 43,215 |
| `ja_jp` 日本語 | █████████░ 93.5% | 39,771 / 42,537 |
| `ru_ru` Русский | ████░░░░░░ 40.8% | 17,348 / 42,537 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 1.5% | 621 / 42,529 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
<!-- 進度:結束 -->

## 翻到一半變回英文？

**這是正常的**，代表那一句還不在語料裡，模組找不到譯文就顯示原文。

回報給翻譯團隊就會補上：Discord 找 **LyuChaCha**，或到
[GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues)。
補好之後所有人下次進遊戲就看得到，不必更新模組。附上英文原文最好——
聊天訊息可以用「複製聊天」直接複製。

## 裝了就是在幫忙

**F6 →「資料」→「分享給翻譯團隊」預設開啟。** 你遇到、還沒翻到的句子會自動送回
翻譯團隊，翻好之後變成所有人的譯文。任務對話與 NPC 名牌沒有官方資料可抓，
只能靠玩家在遊戲裡遇到——**不用翻任何東西，裝著玩就是在幫忙。**

| | |
|---|---|
| **會送** | 遊戲自己的英文字：任務對話、介面、物品說明、NPC 名牌、伺服器公告 |
| **不會送** | 帳號、UUID、座標、所在世界；公會、隊伍、喊話、私訊等別人打的字 |

玩家名字由三道獨立的濾網擋掉（模組送出前、收集站、進倉庫前）。第一次進遊戲時會在聊天室說明一次。

不想分享可以關掉，只開「收集未翻譯字串」：句子寫進 `config/wynnchayuan/captured.json`，
自己看過再附到 Issue。

## 安裝

| 需求 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| 載入器 | Fabric |
| 前置 | [Wynntils](https://modrinth.com/mod/wynntils) 4.2 以上、Fabric API |

jar 放進 `mods/`，進遊戲按 **F6** 開設定。

下載：[GitHub Releases](https://github.com/LyuChaCha/WynnChaYuan/releases/latest) ·
[Modrinth](https://modrinth.com/mod/wynnchayuan) ·
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/wynnchayuan)

## 參與翻譯

不需要會寫程式：譯文是 JSON，在 GitHub 網頁上填 `dst` 就行。

- [CONTRIBUTING.md](CONTRIBUTING.md)：流程、佔位符、顏色標記
- [GLOSSARY.md](GLOSSARY.md)：專有名詞對照
- [給翻譯團隊](docs/for-translators.md)：最近的慣例與改動

想在遊戲裡試自己的譯文：F6 把「譯文來源」切成**本機**，改完按「重新載入」。

## 建置

```bash
gradle build
```

Wynntils 沒有 Maven 座標，要先從 [Modrinth](https://modrinth.com/mod/wynntils/versions)
下載 **Fabric 版** jar 放進 `libs/`（缺了建置會停下並說明）。語料檢查：`python tools/validate.py`。

## 贊助

免費，也會一直免費。想請我們喝杯茶：**<https://ko-fi.com/lyuchacha>**

| 方式 | 回饋 |
|---|---|
| 每月 **3 USD** 以上 | 列入贊助者名單 |
| 單次 **10 USD** 以上 | 列入贊助者名單 |

名單出現在下方、F6 →「關於／貢獻者」，名牌上方也會多一行標記。贊助不影響翻譯內容，沒有付費功能。

## 團隊

同一份名單也在遊戲內 **F6 →「關於／貢獻者」**。要加人改 [`credits.json`](src/main/resources/assets/wynnchayuan/credits.json)。

<!-- credits:begin -->

<!-- 這一段由 tools/sync-credits.py 從 credits.json 產生，不要手動改。 -->

### 開發者

| 名稱 | Minecraft ID |
|---|---|
| LyuChaCha | `Green_teaTW` |
| 芋圓YuYuan | `s103064` |

### 贊助者

| 名稱 | Minecraft ID |
|---|---|
| LyuChaCha | `Green_teaTW` |
| ㄉ綠 | `MlyuL` |

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

### 資料來源

| 名稱 | Minecraft ID |
|---|---|
| Wynntils（物品／技能 CDN） | — |
| Wynncraft | — |

翻一條就會出現在這裡。見 [CONTRIBUTING.md](CONTRIBUTING.md)。

<!-- credits:end -->

## 資料來源與授權

- 物品與技能資料：[Wynntils](https://github.com/Wynntils/Wynntils) 使用的公開 CDN
- 任務與祕密發現清單：[Wynncraft Wiki](https://wynncraft.wiki.gg/)（CC BY-SA）
- 對話框的中日韓與俄文字形：[Fusion Pixel 10px](https://github.com/TakWolf/fusion-pixel-font)（SIL OFL 1.1，全文見
  `src/main/resources/assets/wynnchayuan/font/ofl-fusion.txt`）。字型缺字的句子維持原文，不畫方框
- 材質包符號與排版屬於 Wynncraft，本模組只顯示、不修改
- 程式碼：[MIT](LICENSE)

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。
