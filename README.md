# WynnChaYuan

Wynncraft 繁體中文翻譯模組。**不取代原文**——譯文顯示在旁邊的獨立面板，
原本的畫面完全不動。

## 為什麼不直接把文字換成中文

Wynncraft 是多人遊戲。畫面上只剩中文的話，跟其他玩家討論「去找 Blacksmith」時
就對不上話——老玩家只認得英文名。而且遊戲的排版大量依賴材質包符號與
不可見的對齊字元，直接替換很容易破圖。

所以這個模組的做法是：**原文留著，譯文另外顯示**。

| 內容 | 呈現方式 |
|---|---|
| 物品 tooltip | 旁邊另開一塊翻譯面板 |
| NPC 名牌 | 注視時在準心下方跳一個小框（原文不動） |
| 任務對話 | 畫面下方的獨立小框 |
| 任務追蹤 | 畫面左側的獨立小框 |

## 安裝

需要 [Wynntils](https://modrinth.com/mod/wynntils)（4.2 以上）與 Fabric API。

把 jar 放進 `mods/`，進遊戲按 **F6** 開設定。
首次啟動會自動在 `config/wynncollect/translations/` 產生翻譯工作檔。

## 參與翻譯

翻譯檔就是純 JSON，只要填 `dst`：

```json
"a1b2c3": {
  "src": "Combat Level",
  "dst": "戰鬥等級",
  "role": "name",
  "ctx": ["gear/weapon"]
}
```

改完存檔，在遊戲內按 **F6 → 重新載入譯文檔**即可生效，不用重開遊戲。

### 檔案分工

| 檔案 | 內容 | 來源 |
|---|---|---|
| `ui-labels.json` | 介面標籤 | 手寫 |
| `gear.json` | 裝備名稱與傳說敘述 | 官方 CDN |
| `ability.json` | 技能 | 官方 CDN |
| `ingredient.json` `material.json` `tome.json` `aspect.json` `charm.json` | 材料等 | 官方 CDN |

前者手寫，後者由 `tools/build.py` 從官方資料生成——**重跑會保留已填的 `dst`**，
所以遊戲改版後可以安全地重新生成。

### 三種佔位符

譯文裡必須原樣保留，位置可依中文語序調整：

| 佔位符 | 代表 | 範例 |
|---|---|---|
| `{#}` | 材質包符號 | `{#} Mana Cost: {~}` → `{#} 魔力消耗: {~}` |
| `{~}` | 數值 | `- +{~} Emeralds` → `- +{~} 綠寶石` |
| `{p}` | 地名（永遠不翻） | `{p} Citizen` → `{p} 市民` |

`{p}` 讓 137 個地名共用同一條譯文，也保證地名不會被翻掉。
**佔位符數量對不上時整行會放棄翻譯**——寧可顯示原文，也不要畫出錯位的東西。

## 建置

```bash
gradle build
```

Wynntils 只發佈到 Modrinth／CurseForge，沒有 Maven 座標，所以要**自行下載
fabric 版**放進 `libs/`（不隨本專案散布）。少了它建置會直接停下並說明該怎麼做。

> 不要改用 `maven.modrinth:wynntils:v4.2.8`——看起來可行，實測會抓到 NeoForge 版：
> Modrinth 上 fabric 與 neoforge 共用同一個 version_number。

`gradle build` 會自動跑三組驗證：符號判斷、翻譯來回、以及
「Python 語料工具與 mod 端算出相同模板」的一致性比對。最後這項尤其重要——
兩邊規則只要有一點差異，遊戲裡就會靜默查不到譯文。

## 重新生成語料

```bash
python tools/fetch.py    # 從官方 CDN 下載
python tools/build.py    # 抽取、分類、參數化 → 工作檔
```

## 資料來源與致謝

- 物品與技能資料取自 [Wynntils](https://github.com/Wynntils/Wynntils) 使用的公開 CDN
- 材質包符號與排版由 Wynncraft 提供，本模組僅顯示、不修改

## 授權

[MIT](LICENSE)
