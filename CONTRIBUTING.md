# 參與翻譯

翻譯檔就是純 JSON，只要把 `dst` 填上中文。**不需要會寫程式。**

目前進度：**135 / 9,132**（1.5%）。

---

## 最快的方式：在 GitHub 網頁上改

不用安裝任何東西，不用會 Git。

1. 到 [translations 資料夾](../../tree/main/src/main/resources/assets/wynnchayuan/translations)
2. 點你想翻的檔案（**建議從 `ui-labels.json` 或 `npc.json` 開始**，那些每天都看得到）
3. 按右上角的 **鉛筆圖示**（Edit this file）
4. 找到 `"dst": ""`，把中文填進去
5. 拉到最下面，按 **Commit changes** → 選 **Create a new branch and start a pull request**
6. 送出

合併之後，**所有裝這個模組的人下次進遊戲就會拿到你的翻譯**——不需要重新下載模組。

> 大檔（`gear-weapon.json` 有 550KB）在網頁編輯器上會有點慢，
> 用瀏覽器的搜尋功能（Ctrl+F）找關鍵字比較快。

---

## 從哪裡開始翻

檔案依「玩家看到的頻率」排序，**由上往下翻效益最高**：

| 檔案 | 條目 | 什麼時候會看到 |
|---|---:|---|
| `ui-labels.json` | 83 | 每次看物品都會看到（力量、戰鬥等級…） |
| `npc.json` | 37 | 走在城裡就會看到 |
| `quest.json` | 15 | 做任務時 |
| `ability.json` | 1,240 | 開技能樹時 |
| `ingredient.json` | 969 | 做職業時 |
| `gear-*.json` | 6,379 | 裝備的傳說敘述，多數人一輩子看不到幾條 |

不確定翻什麼就跑這個：

```bash
python tools/progress.py --next 30
```

---

## 三種佔位符：原樣保留，位置可調

| 佔位符 | 代表 | 例子 |
|---|---|---|
| `{#}` | 材質包符號 | `{#} Mana Cost: {~}` → `{#} 魔力消耗: {~}` |
| `{~}` | 數值 | `- +{~} Emeralds` → `- +{~} 綠寶石` |
| `{p}` | 地名（**永遠不翻**） | `{p} Citizen` → `{p} 市民` |

**數量必須一樣**，位置可以依中文語序調整。少一個或多一個，那一行就會整行放棄翻譯，
顯示原文——寧可不翻，也不要畫出錯位的東西。

`{p}` 特別值得注意：`{p} Citizen` 翻一次，**137 個地名全部適用**，
而且地名本身永遠保持英文（跟其他玩家溝通時對得上）。

## 翻譯慣例

- **裝備與物品名稱保留英文**。那是專有名詞，翻了反而對不上社群討論與 wiki
- **地名保留英文**（用 `{p}` 自動處理）
- **技能名稱**建議保留英文或附註，例如 `Meteor` → `Meteor（隕石）`
- 語氣照原文。Wynncraft 的 NPC 講話很口語，不用翻得太正式

---

## 不想碰 GitHub？

開一個 [Issue](../../issues/new)，把你翻好的內容貼上來就好，格式像這樣：

```
Combat Level = 戰鬥等級
Health Regen = 生命回復
```

有人會幫你併進去。

---

## 幫忙收集沒被翻譯的句子

任務對話與 NPC 名稱**沒有官方資料可抓**，只能靠玩家在遊戲裡實際遇到。

模組會自動把沒翻到的句子記進
`config/wynnchayuan/captured.json`。玩一段時間後把這個檔開 Issue 附上來，
就能變成大家共用的待翻條目。

> **附上前請先看一眼。** 模組會過濾玩家名稱、好友名單、座標，
> 但 Wynncraft 的通知格式很多，可能有漏網。看到別人的名字就刪掉那一條，
> 順便回報一下，我們補過濾規則。

維護者這樣併：

```bash
python tools/merge_captured.py captured.json --dry-run   # 先看會加什麼
python tools/merge_captured.py captured.json
```

---

## 自己先試翻譯效果

不用等合併：

1. 遊戲裡按 **F6** → 把「譯文來源」切成 **本機**
2. 改 `config/wynnchayuan/translations/` 底下的檔案
3. **F6 → 重新載入譯文檔**（不用重開遊戲）

改壞了就把檔案刪掉，下次啟動會自動放回原始版本。

---

## 給開發者

```bash
gradle build      # 會自動跑三組驗證
```

Wynntils 沒有 Maven 座標，要自行從
[Modrinth](https://modrinth.com/mod/wynntils/versions) 下載 **fabric 版**放進 `libs/`。

改動 `tools/build.py` 的參數化規則時特別注意：**mod 端有一份對應的實作**
（`LineParts` / `GlyphSplitter`），兩邊算出的模板必須完全一致，
否則遊戲裡會靜默查不到譯文。`TemplateParityTest` 就是在擋這件事。
