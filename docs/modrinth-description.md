# WynnChaYuan · Wynncraft 繁體中文翻譯

> **English:** Traditional Chinese (zh-TW) translation for **Wynncraft**.
> By default it **does not replace the original text** — translations appear in a
> separate panel so English names stay visible for talking with other players.
> Requires [Wynntils](https://modrinth.com/mod/wynntils). Client-side only.

---

## 這個模組在做什麼

把 Wynncraft 的物品、技能、NPC、任務與介面翻成繁體中文。

但它**預設不會把原文換掉**——譯文顯示在旁邊另一塊面板，英文原名照樣留在畫面上。

## 為什麼不直接換成中文

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

## 翻譯範圍

| 內容 | 呈現方式 |
|---|---|
| 物品 tooltip | 旁邊另開翻譯面板，或就地取代（可選） |
| NPC 名牌 | 注視時在準心附近跳一個小框，原文不動 |
| 任務對話 | 畫面下方的獨立小框 |
| 任務追蹤 | 畫面側邊的獨立小框 |
| 介面文字 | 公會、任務書、技能樹等 GUI |

四個框的位置都可以**拖曳調整**，而且是四個同時顯示——才看得出會不會互相擋到。

---

## 安裝

| 需求 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| 載入器 | Fabric |
| 前置 | [Wynntils](https://modrinth.com/mod/wynntils) 4.2 以上 · Fabric API |

把 jar 放進 `mods/`，進遊戲按 **F6** 開設定。首次啟動會自動產生翻譯工作檔。

**純客戶端模組**，伺服器不需要安裝，也不會知道你在用。

---

## 設定（F6）

- **物品翻譯**：另開面板 / 就地取代 / 關閉
- **面板位置**：跟隨滑鼠或固定，可拖曳排版
- **NPC 名牌**：模式、停留秒數、偵測距離與準心夾角
- **框線顏色**：自訂 16 進位色碼
- **譯文來源**：GitHub（統一）或本機（測試自己的翻譯用）

---

## 翻譯會自己更新，不用重新下載模組

譯文不是寫死在 jar 裡的。模組啟動時會從 GitHub 同步最新版本，
所以**譯者把翻譯合併進去，所有人下次進遊戲就拿到了**。

想立刻確認的話，F6 有「從 GitHub 重新抓譯文」，不必重開遊戲。

離線或連不上時會用上次的快取；再不行才用 jar 內建的版本。所以斷網只是
「沒有最新的」，不會變成沒有翻譯。

---

## 目前進度與參與翻譯

**173 / 9,167（1.9%）——非常歡迎幫忙。**

日常最常看到的部分已經翻完：

| 檔案 | 進度 |
|---|---|
| 物品介面標籤 | 85 / 85 ✅ |
| NPC 名稱 | 36 / 36 ✅ |
| 介面文字 | 34 / 34 ✅ |
| 任務 | 15 / 15 ✅ |
| 技能說明 | 3 / 1,240 |
| 材料 | 0 / 969 |
| 裝備敘述 | 0 / 6,379 |

**不需要會寫程式。** 翻譯檔是純 JSON，在 GitHub 網頁上點鉛筆就能改。

- [參與翻譯的完整說明](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CONTRIBUTING.md)
- [專有名詞對照表](https://github.com/LyuChaCha/WynnChaYuan/blob/main/GLOSSARY.md)
  （`Reflection` 是「遠程反傷」不是「反射」那類）

送出的翻譯會自動跑格式檢查：佔位符數量、換行數、材質包符號、地名保留——
這些無論翻得好不好都一定錯，讓機器擋掉，人就能專心看「翻得對不對」。

---

## 常見問題

**會不會被伺服器判定作弊？**
不會。這是純客戶端的顯示模組，不改封包、不自動化任何操作，也不送出任何東西。

**跟其他 Wynncraft 模組衝突嗎？**
預設的面板模式**完全不修改** tooltip 內容，所以不會干擾同樣掛在那個事件上的
其他模組。Nori、Wynnpool 這類會加自己區塊的模組也做過相容處理。

**我的名字可以出現在名單上嗎？**
翻一條就會。名單顯示在 F6 →「關於／貢獻者」，含 Minecraft 頭像；
名單上的人，名牌上方還會多一行標記（只有裝了本模組的人看得到）。

---

## 授權與致謝

[MIT](https://github.com/LyuChaCha/WynnChaYuan/blob/main/LICENSE)。

- 物品與技能資料取自 [Wynntils](https://modrinth.com/mod/wynntils) 使用的公開 CDN
- 材質包符號與排版由 Wynncraft 提供，本模組僅顯示、不修改

與 Wynncraft 官方及 Wynntils 團隊**無隸屬關係**，是社群自發的翻譯專案。

**[GitHub](https://github.com/LyuChaCha/WynnChaYuan)** · **[回報問題](https://github.com/LyuChaCha/WynnChaYuan/issues)** · **[更新日誌](https://github.com/LyuChaCha/WynnChaYuan/blob/main/CHANGELOG.md)**
