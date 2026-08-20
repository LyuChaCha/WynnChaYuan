# docs

發佈平台用的文案，放在版控裡才有得改、有得追。

| 檔案 | 用途 |
|---|---|
| [modrinth-description.md](modrinth-description.md) | Modrinth 專案頁的描述，直接貼進 Settings → Description |

## Modrinth 頁面還需要什麼

描述之外，這幾項在 Modrinth 後台設定，沒有對應的檔案：

- **Summary**（列表頁的一句話）：建議
  `Wynncraft 繁體中文翻譯 — 譯文顯示在獨立面板，不取代原文`
- **Categories**：`Utility`、`Social`（翻譯類沒有專屬分類）
- **Environment**：Client `required` / Server `unsupported`
- **License**：MIT
- **Links**：Source 與 Issues 都指向 GitHub repo
- **Gallery**：截圖。**這一項最重要**——翻譯模組光看文字說不清楚
  「原文留著、譯文另外顯示」是什麼樣子。建議放：
  1. 物品 tooltip 的原文與譯文並排
  2. 就地取代模式
  3. NPC 對話小框
  4. F6 設定頁

改版時記得同步更新描述裡的翻譯進度數字（`python tools/progress.py`）。
