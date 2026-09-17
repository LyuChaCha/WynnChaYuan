<p align="center">
  <img src="docs/icon.png" width="160" alt="WynnChaYuan">
</p>

# WynnChaYuan

English: **[README.en.md](README.en.md)**

Wynncraft 的多語言翻譯模組（Fabric 1.21.11）。**原文保留**，譯文顯示在旁邊，或直接寫進原本的位置。

[下載](https://github.com/LyuChaCha/WynnChaYuan/releases/latest) ·
[更新日誌](CHANGELOG.md) ·
[回報問題](https://github.com/LyuChaCha/WynnChaYuan/issues) ·
[Ko-fi](https://ko-fi.com/lyuchacha)

> [!IMPORTANT]
> **目前是 Beta。** 譯文大多由 AI 產出，只有一部分經過人工校稿，會有錯譯與用詞不一致。
> 遇到問題請到 Discord 找 **LyuChaCha**，或到 [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues) 回報。

## 功能

### 翻譯範圍

| 內容 | 翻了什麼、怎麼顯示 |
|---|---|
| 物品 tooltip | 裝備敘述、屬性、Major ID；旁邊另開面板，或寫進原本的 tooltip |
| 技能樹 | 五個職業的節點、說明與流派 |
| 任務對話、對話選項 | 寫進 **Wynncraft 自己的對話框**（框、名牌、頭像不動），逐字打出的節奏跟原文同步；也可另開小框 |
| 任務追蹤 | 獨立小框，抬頭照實際追蹤的東西顯示（任務、世界事件、洞窟、討伐戰…） |
| NPC 名牌、漂浮字 | 注視時跳小框，或就地取代；工作站與互動提示也算 |
| 聊天訊息 | 伺服器發的訊息（任務完成、獎勵、進出區域），Lootrun 結算、信標這類分欄訊息保持對齊。玩家發言不翻 |
| 選單與介面 | 交易市場、公會、商城等畫面 |
| Lootrun、討伐戰、地城 | 使命、賜福、信標、Aspect、Gambit、結算與戰利品面板、地城名稱與鑰匙 |
| 探索點、祕密發現 | 名稱、說明與故事 |
| 畫面中央大字 | 標題與副標題 |

裝備名稱預設保留原文——那是專有名詞，交易市場與 wiki 都用英文。F6 可以打開。

### 其他

- **譯文自動更新**：譯文從 GitHub 下載，新的翻譯合併後下次進遊戲就生效，**不必更新模組**。離線時用上次的快取或 jar 內建版本
- **市集搜尋**：在交易市集用你的語言搜尋，送出前自動換回英文原名；打字時列出候選，↑↓ 選、Tab 填入
- **複製聊天**：列出最近的聊天訊息，點一則複製，方便回報（按鍵預設沒綁）
- **譯文截圖**：**F9** 把翻譯面板複製到剪貼簿或存成檔案（可改綁）
- **面板調整**：小框都能拖曳定位；對話、選項、任務追蹤可拉右下角改大小
- **更新提示**：有新版時在聊天室提示一次，連到 GitHub Releases
- **貢獻者標記**：名單上的人，名牌上方多一行標記，只有裝了本模組的人看得到（可關）

**純客戶端**，伺服器不需要裝。

### F6 設定

| 分頁 | 內容 |
|---|---|
| 物品 | 物品翻譯（另開面板／就地取代／關閉）、翻譯物品名稱、市集搜尋、譯文截圖 |
| 面板 | 跟隨滑鼠或固定、放在哪一側、間距、框線顏色、調整面板位置 |
| 對話 | 任務對話、對話選項（另開小框／就地取代／關閉）、停留秒數、對話／追蹤小框 |
| 世界與聊天 | 名牌與漂浮字（含偵測距離與準心夾角）、聊天訊息、畫面中央大字、複製聊天 |
| 資料 | 譯文語言、輔助語言、介面語言、譯文來源、重新載入、收集未翻譯字串、收集介面文字、匯出未翻譯字串、如何提交、診斷檔 |

滑鼠移到項目上會顯示說明。

## 支援語言與進度

| 語言 | 內容 |
|---|---|
| `zh_tw` 繁體中文 | 主要語言，所有內容 |
| `zh_cn` 简体中文 | 所有內容 |
| `ja_jp` 日本語 | 所有內容，含任務對話 |
| `ru_ru` Русский | 所有內容，含任務對話 |
| `ko_kr` 한국어 | 物品欄位標籤與 Major ID |

在 **F6 →「資料」** 切換，不必改遊戲語言，也不用重開：

- **譯文語言**：看哪一種語言的翻譯
- **輔助語言**：譯文語言還沒翻到的句子改顯示哪一種（或原文）
- **介面語言**：F6 設定畫面本身的語言（另有英文）

<!-- 進度:開始 -->
更新於 2026-09-17。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | ██████████ 95.2% | 43,197 / 45,398 |
| `zh_cn` 简体中文 | █████████░ 94.2% | 41,204 / 43,724 |
| `ja_jp` 日本語 | █████████░ 94.0% | 41,119 / 43,722 |
| `ru_ru` Русский | █████████░ 93.7% | 40,970 / 43,721 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 1.8% | 778 / 43,582 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
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
2. 放進 `mods/`，進遊戲按 **F6** 開設定。

## 翻到一半變回英文？

**這是正常的**，代表那一句還不在語料裡，模組找不到譯文就顯示原文。

照下面的[怎麼幫忙](#怎麼幫忙)把它匯出交給我們，補好之後所有人下次進遊戲就看得到，不必更新模組。

## 怎麼幫忙

任務對話與 NPC 名牌沒有官方資料可抓，只能靠玩家在遊戲裡遇到。**不用翻任何東西**，把你遇到的缺口交給我們就好：

1. **F6 →「資料」→「匯出未翻譯字串」**，會打開 `config/wynnchayuan/export`，裡面是 `captured.json`。
2. 打開檔案看一遍，刪掉任何個人資訊（別人的名字、公會名、私人對話）。
3. **F6 →「資料」→「如何提交」**打開 [Issue 表單](https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml)，把檔案拖進去；或在 Discord 傳給 **LyuChaCha**。

模組**不會自動送出任何東西**。匯出檔已經濾掉玩家名字與公會、隊伍、喊話、私訊，但濾網是猜的，送出前請自己看過。

翻錯或用詞不對，直接到 [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues) 回報，附上英文原文最好——聊天訊息可以用「複製聊天」直接複製。

## 參與翻譯

不需要會寫程式：譯文是 JSON，在 GitHub 網頁上填 `dst` 就行。

- [CONTRIBUTING.md](CONTRIBUTING.md)：流程、佔位符、顏色標記
- [GLOSSARY.md](GLOSSARY.md)：專有名詞對照
- [給翻譯團隊](docs/for-translators.md)：最近的慣例與改動

想在遊戲裡試自己的譯文：F6 把「譯文來源」切成**本機**，改完按「重新載入」。

## 從原始碼建置

```bash
gradle build
```

需要 JDK 21。Wynntils 沒有 Maven 座標，要先從 [Modrinth](https://modrinth.com/mod/wynntils/versions)
下載 **Fabric 版** jar 放進 `libs/`（缺了建置會停下並說明）。語料檢查：`python tools/validate.py`。

## 贊助

免費，也會一直免費。想請我們喝杯茶：**<https://ko-fi.com/lyuchacha>**

每月 **3 USD** 或單次 **10 USD** 以上列入贊助者名單：出現在下方、F6 →「關於／貢獻者」，名牌上方也會多一行標記。贊助不影響翻譯內容，沒有付費功能。

## 翻譯團隊

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

## 來源與授權

- 物品與技能資料：[Wynntils](https://github.com/Wynntils/Wynntils) 使用的公開 CDN
- 任務與祕密發現清單：[Wynncraft Wiki](https://wynncraft.wiki.gg/)（CC BY-SA）
- 對話框的中日韓與俄文字形：[Fusion Pixel 10px](https://github.com/TakWolf/fusion-pixel-font)（SIL OFL 1.1，全文見
  `src/main/resources/assets/wynnchayuan/font/ofl-fusion.txt`）。字型缺字的句子維持原文，不畫方框
- 材質包符號與排版屬於 Wynncraft，本模組只顯示、不修改
- 程式碼：[MIT](LICENSE)

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。
