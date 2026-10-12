# wynnchayuan.com：網站與登記站

一支 Cloudflare Worker 加一個 KV，管這幾件事：

- **網站**：首頁、披風一覽、帳號、管理。
- **誰有裝 WynnChaYuan**：名牌前面的灰色葉子。
- **誰的披風要給別人看**：本人在 F6 的選擇。
- **玩家上傳的披風**，以及違規披風的下架與封禁。

Worker 的名字還是 `wynnchayuan-corpus`（沿用語料收集站那一支與它的 KV，綁定叫 `CORPUS`）。舊的 `/submit`、`/drain`、`/ack` 不在了。

| 檔案 | |
|---|---|
| `worker.js` | API 與路由 |
| `site.js` | 四張網頁（共用樣式、頁首、頁尾） |
| `worker.test.mjs` | `node tools/presence/worker.test.mjs` |
| `dev.mjs` | 在本機用假資料開網站來看 |

## 網頁

| | |
|---|---|
| `/` | 首頁：模組是什麼、下載、徽章與披風 |
| `/capes` | 披風一覽。每張卡片是披風的正面；點開是一個穿著它的人偶，可以拖曳旋轉（立體的部分用 jsDelivr 上的 skinview3d，載不到就只看平面的圖） |
| `/account` | 帳號：登入狀態、自己的披風、上傳與移除 |
| `/admin` | 管理（開發者）：封禁名單、新增成員 |

### 登入

網站沒辦法自己確認 Minecraft 帳號，所以登入是**從遊戲裡開**：F6 →「徽章與披風」→「披風一覽」。模組拿它的通行證向 `/api/web-link` 換一條一天有效的網址（憑證在 `#` 後面，那一段不會送到任何伺服器），瀏覽器開了就是登入的。網頁把憑證收進 `localStorage`、從網址列上拿掉。

## API

| | | |
|---|---|---|
| `POST /api/login` | `{name, serverId}` | 向 Mojang 確認身分，發通行證（模組用） |
| `POST /api/web-link` | | 拿一條替瀏覽器登入的網址（要通行證） |
| `GET /api/me` | | 我是誰、有沒有披風、是不是開發者（要通行證） |
| `POST /api/hello` | `{cape}` | 報到（要通行證） |
| `GET /api/list` | | 名單：有裝的人、有上傳披風的人、關掉披風的人 |
| `PUT /api/cape` | 圖檔本體 | 上傳披風（要通行證） |
| `DELETE /api/cape` | | 拿掉自己的披風（要通行證） |
| `GET /api/cape/<代碼>` | | 下載某個人的披風 |
| `GET /api/capes` | | 所有上傳的披風，相同的圖併成一筆 |
| `GET /api/status` | | 有幾個人、幾件披風 |
| `GET /api/admin/capes` | | 同上，多了每一件是誰傳的（開發者） |
| `POST /api/admin/remove` | `{id}` 或 `{hash}`，加 `ban` | 拿掉披風、要不要封禁（開發者） |
| `GET /api/admin/bans` | | 封禁名單（開發者） |
| `POST /api/admin/unban` | `{uuid}` | 解除封禁（開發者） |
| `POST /api/admin/credits` | `{section, name, mc, color}` | 替名單加一個人：開一個 PR（開發者） |

## 它記什麼

- **記的**：玩家的 UUID（當成鍵）、要不要顯示披風、哪一天報到的；上傳的披風圖檔，連同上傳的人的帳號名（管理的人要知道是誰傳的）；被封禁的人（UUID、帳號名、時間、誰封的）。十四天沒再報到就從名單上消失；圖檔留著，回來報到就又出現。
- **不記的**：IP、所在的世界、任何遊戲內容。
- **公開的名單與一覽不放 UUID 與帳號名**：放的是 UUID 雜湊出來的代碼。誰傳了哪一件只有開發者看得到。

## 身分怎麼驗

Minecraft 多人伺服器自己那一套：模組向 Mojang 說「我要加入 <隨機字串>」，登記站再問 Mojang「這個帳號剛剛是不是加入了 <隨機字串>」。模組的登入憑證只給 Mojang，不會經過這裡。驗過之後發一張七天的通行證（HMAC 簽的，密鑰第一次用到時自己產生、存在 KV）；網頁的那一張只有一天。

所以別人沒辦法替你報到、替你換披風。離線帳號驗不過。

## 誰可以上傳

`wrangler.toml` 的 `UPLOADS`：

- `tiered`（預設）：驗過身分的人都能傳 **PNG**；**GIF** 只有 `credits.json` 上有遊戲 ID 的成員（開發者、贊助者、貢獻者）。
- `listed`：只有名單上的成員能傳。
- `anyone`：驗過身分的人都能傳，PNG、GIF 都可以。
- `off`：不收。

圖的規則跟模組的 `CapeRules` 同一套：只收照範本畫的（22×17、44×34、88×68、176×136），20 MB 以內，GIF 最多 120 格。見 `capes/README.md`。

相同的圖只存一份：圖檔照內容存（`blob:<SHA-256>`），每個人的披風那一筆（`cape:<代碼>`）只記它指到哪一張。沒有人用的圖在換圖、刪除時順手清掉。

## 管理

**不可以上傳的內容：政治、不雅或色情、違反法規、種族歧視或仇恨。**

誰有管理權：`credits.json` 開發者那一區的人。每個請求都會重新查一次名單，所以從名單上拿掉就立刻沒有權限。

- **刪除、封禁**：在 `/capes` 點開一件披風，開發者看得到是誰傳的，還有「刪除這件披風」與「刪除並封禁上傳的人」。刪除是照圖刪：所有用同一張圖的人一起拿掉。
- **封禁名單**：`/admin`。誰、什麼時候、誰封的，可以解除。
- **新增成員**：`/admin` 的表單。送出之後在倉庫開一個分支、改 `credits.json`、開 PR；合併還是照倉庫的規矩來（檢查要過、要有人按）。要先設好 `GITHUB_TOKEN`：

  ```bash
  npx wrangler secret put GITHUB_TOKEN
  ```

  用 fine-grained token，只給這個倉庫，權限 **Contents: Read and write** 與 **Pull requests: Read and write**。沒設的話表單會說還沒設定，其他功能不受影響。

不在遊戲裡的時候可以另外設一個密語，用 `curl` 管理（沒設就沒有這條路）：

```bash
npx wrangler secret put ADMIN_TOKEN
```

```bash
curl -X POST https://wynnchayuan.com/api/admin/remove \
  -H "Authorization: Bearer <密語>" -H "Content-Type: application/json" \
  -d '{"id": "<那個人的 24 字元代碼>", "ban": true}'
```

## 部署

```bash
cd tools/presence
npx wrangler login
npx wrangler deploy
```

Windows 的 PowerShell 擋 `npx.ps1` 的話（「因為這個系統上已停用指令碼執行」），改打 `npx.cmd`：

```bash
npx.cmd wrangler deploy
```

第一次部署到自己的網域時，wrangler 會自己建好 DNS 紀錄與憑證，一兩分鐘後生效。

## 在本機看網站

```bash
node tools/presence/dev.mjs
```

用放在記憶體裡的假資料跑同一支 Worker，塞了 `capes/templates` 的示範披風與一筆封禁。真的登入要連 Mojang，這裡沒有接，改成 `/dev-login?as=dev`（當成開發者）與 `/dev-login?as=user`（當成一般玩家）。

## 測試

```bash
node tools/presence/worker.test.mjs
```
