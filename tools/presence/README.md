# 登記站

一支 Cloudflare Worker 加一個 KV，管三件事：

- **誰有裝 WynnChaYuan**：名牌前面的灰色葉子。
- **誰的披風要給別人看**：本人在 F6 的選擇。
- **玩家上傳的披風**。

沿用先前語料收集站那一支 Worker（`wynnchayuan-corpus`）與它的 KV（綁定叫 `CORPUS`），網址不變。舊的 `/submit`、`/drain`、`/ack` 不在了。

## 路由

| | | |
|---|---|---|
| `POST /login` | `{name, serverId}` | 向 Mojang 確認身分，發通行證 |
| `POST /hello` | `{cape}` | 報到（要通行證） |
| `GET /list` | | 名單 |
| `PUT /cape` | 圖檔本體 | 上傳披風（要通行證） |
| `DELETE /cape` | | 拿掉自己的披風（要通行證） |
| `GET /cape/<代碼>` | | 下載某個人的披風 |
| `POST /admin/remove` | `{id, ban}` | 拿掉一件披風、要不要禁止再傳（要 `ADMIN_TOKEN`） |

## 它記什麼

- **記的**：玩家的 UUID（當成鍵）、要不要顯示披風、哪一天報到的、上傳的披風圖檔。十四天沒再報到就從名單上消失；圖檔留著，回來報到就又出現。
- **不記的**：IP、所在的世界、任何遊戲內容。帳號名只在驗證與「是不是名單上的人」那一刻用到，不存。
- **公開的名單不放 UUID**：放的是 UUID 雜湊出來的代碼，模組把畫面上那個人的 UUID 算成代碼再去找。

## 身分怎麼驗

Minecraft 多人伺服器自己那一套：模組向 Mojang 說「我要加入 <隨機字串>」，登記站再問 Mojang「這個帳號剛剛是不是加入了 <隨機字串>」。模組的登入憑證只給 Mojang，不會經過這裡。驗過之後發一張七天的通行證（HMAC 簽的，密鑰第一次用到時自己產生、存在 KV）。

所以別人沒辦法替你報到、替你換披風。離線帳號驗不過，也就不會出現在名單上。

## 誰可以上傳

`wrangler.toml` 的 `UPLOADS`：

- `listed`（預設）：`credits.json` 上有遊戲 ID 的成員。
- `anyone`：驗過身分的人都可以。
- `off`：不收。

圖的規則（尺寸、大小、格數）跟模組的 `CapeRules` 同一套，見 `capes/README.md`。

## 拿掉不該出現的披風

先設一個管理用的密語（只要做一次）：

```bash
npx wrangler secret put ADMIN_TOKEN
```

之後：

```bash
curl -X POST https://wynnchayuan-corpus.green09good.workers.dev/admin/remove \
  -H "Authorization: Bearer <密語>" -H "Content-Type: application/json" \
  -d '{"id": "<那個人的 24 字元代碼>", "ban": true}'
```

`ban` 是 `true` 的話那個人之後不能再傳；`false` 是解除。代碼就是披風網址 `/cape/` 後面那一段。沒設 `ADMIN_TOKEN` 的話這條路由不開。

## 部署

```bash
cd tools/presence
npx wrangler login
npx wrangler deploy
```

## 測試

```bash
node tools/presence/worker.test.mjs
```
