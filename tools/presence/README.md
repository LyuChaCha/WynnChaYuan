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
| `GET /capes` | | 所有上傳的披風，相同的圖併成一筆 |
| `GET /gallery` | | 披風一覽（網頁） |
| `POST /mod-link` | | 開發者拿一條帶著管理權的一覽網址（要通行證） |
| `POST /admin/remove` | `{id}` 或 `{hash}`，加 `ban` | 拿掉披風、要不要禁止再傳（開發者，或 `ADMIN_TOKEN`） |

## 它記什麼

- **記的**：玩家的 UUID（當成鍵）、要不要顯示披風、哪一天報到的、上傳的披風圖檔。十四天沒再報到就從名單上消失；圖檔留著，回來報到就又出現。
- **不記的**：IP、所在的世界、任何遊戲內容。帳號名只在驗證與「是不是名單上的人」那一刻用到，不存。
- **公開的名單不放 UUID**：放的是 UUID 雜湊出來的代碼，模組把畫面上那個人的 UUID 算成代碼再去找。

## 身分怎麼驗

Minecraft 多人伺服器自己那一套：模組向 Mojang 說「我要加入 <隨機字串>」，登記站再問 Mojang「這個帳號剛剛是不是加入了 <隨機字串>」。模組的登入憑證只給 Mojang，不會經過這裡。驗過之後發一張七天的通行證（HMAC 簽的，密鑰第一次用到時自己產生、存在 KV）。

所以別人沒辦法替你報到、替你換披風。離線帳號驗不過，也就不會出現在名單上。

## 誰可以上傳

`wrangler.toml` 的 `UPLOADS`：

- `tiered`（預設）：驗過身分的人都能傳 **PNG**；**GIF** 只有 `credits.json` 上有遊戲 ID 的成員（開發者、贊助者、貢獻者）。
- `listed`：只有名單上的成員能傳。
- `anyone`：驗過身分的人都能傳，PNG、GIF 都可以。
- `off`：不收。

圖的規則跟模組的 `CapeRules` 同一套：只收照範本畫的（22×17、44×34、88×68、176×136），20 MB 以內，GIF 最多 120 格。見 `capes/README.md`。

## 相同的圖只存一份

圖檔照內容存（`blob:<SHA-256>`），每個人的披風那一筆（`cape:<代碼>`）只記它指到哪一張。好幾個人傳同一張圖只佔一份空間；沒有人用的圖在換圖、刪除時順手清掉。

## 披風一覽與下架

`/gallery` 是一張網頁，列出所有上傳的披風（相同的圖算一張）。誰都可以看。

**不可以上傳的內容：政治、不雅或色情、違反法規、種族歧視或仇恨。**

開發者（`credits.json` 的開發者那一區）在遊戲裡按 F6 →「徽章與披風」→「披風一覽」開這一頁，每張披風會多出兩顆按鈕：

- **刪除**：所有用這張圖的人的披風一起拿掉。
- **刪除並禁止再傳**：同上，而且那些人之後不能再上傳。

做法：遊戲裡開的那一條網址帶著兩個小時有效的管理憑證，放在網址 `#` 後面（那一段不會送到任何伺服器）。登記站每次都會再確認那個人還在開發者名單上。

不在遊戲裡的時候可以用密語（只要設一次）：

```bash
npx wrangler secret put ADMIN_TOKEN
```

```bash
curl -X POST https://wynnchayuan-corpus.green09good.workers.dev/admin/remove \
  -H "Authorization: Bearer <密語>" -H "Content-Type: application/json" \
  -d '{"id": "<那個人的 24 字元代碼>", "ban": true}'
```

`ban` 是 `true` 的話那個人之後不能再傳，`false` 是解除。也可以用 `{"hash": "<圖的 SHA-256>"}` 一次拿掉所有用同一張圖的人。

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
