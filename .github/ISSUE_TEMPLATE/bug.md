---
name: 回報問題
about: 翻譯沒出現、版面跑掉、方框、崩潰等
title: ''
labels: bug
---

**問題**：

**截圖**（版面問題請務必附）：

**`captured.json` 的 `_meta.events` 段落**：

```json
"events": {
  "panel.shown": 0,
  "panel.noMatch": 0
}
```

> 這段能分辨「沒讀到」與「讀到了但字典沒有」，是排查的關鍵。
> 檔案在 `config/wynnchayuan/captured.json`。

**模組版本**：
