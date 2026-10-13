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
| `site.js` | 四張網頁（共用樣式、頁首、頁尾），中文與英文 |
| `mojang-keys.js` | 釘住的 Mojang 公鑰（`update-mojang-keys.mjs` 產生的） |
| `lang/*.json`、`site-text.js` | 其他語言的字，與 `build-site-text.mjs` 併出來給 Worker 讀的那一份 |
| `demo-data.js` | 功能頁即時預覽用的字：選項的名稱與說明（模組設定畫面的語言檔）、場景裡的譯文（語料）。`python tools/presence/build-demo.py` 產生，`--check` 比對；那幾條語料或語言檔改了要重跑 |
| `icons.js` | 網站的圖示（`make-icons.py` 從 `logo/2026` 縮出來的；`/favicon.png`、`/icon-192.png`、`/apple-touch-icon.png`） |
| `worker.test.mjs` | `node tools/presence/worker.test.mjs` |
| `dev.mjs` | 在本機用假資料開網站來看 |

## 網頁

| | |
|---|---|
| `/` | 首頁：模組是什麼、下載、團隊（開發者、贊助者、貢獻者照 `credits.json` 畫成名冊，一個人一列，點下去是他在 Wynncraft 的頁面；下面是管理頁登記的貢獻公會，有旗幟與圖示）、關於贊助 |
| `/features` | 功能：最上面是即時預覽（照遊戲裡設定畫面右邊那一塊做：左邊幾個真的選項，右邊一張小場景，物品／對話／世界與聊天三頁、七種譯文語言），下面照分類列出翻譯範圍、工具、葉子與披風、相容的模組 |
| `/capes` | 披風一覽。每張卡片是披風的正面；點開是一個穿著它的人偶，可以拖曳旋轉（立體的部分用 jsDelivr 上的 skinview3d，載不到就只看平面的圖）。可以排序（最新、最早、最多人使用、人氣、最近有人穿上）與篩選（PNG／GIF／我的最愛／我用過的）；登入的人可以**穿上**、**復原**、按星星加進**我的最愛**。管理的人看得到全部，多「不公開」「待審核」兩個篩選 |
| `/account` | 帳號：登入狀態、自己的披風、上傳與移除 |
| `/admin` | 管理（開發者與管理員）：封禁名單、特別名單、貢獻公會、新增成員；管理員名單只有開發者看得到 |

每一頁的頁首有「下載」（Modrinth、CurseForge、GitHub Releases）與贊助（Ko-fi）；頁尾有下載點、GitHub 與兩份授權、聯絡方式，以及「與 Wynncraft、Mojang、Microsoft 沒有關係」那一行。玩家的頭像與全身圖用 [minotar](https://minotar.net)（照名字畫，不必先查 UUID）。

### 語言

八種：繁體中文、简体中文、English、日本語、한국어、Русский、Español、Türkçe（跟模組支援的語言一樣）。

- 繁體中文與英文的字在 `site.js` 的 `TEXT`，這兩份是正本；頁面樣板用 `%%鍵%%` 標位置。
- 其他語言一個檔一種，在 `lang/<代碼>.json`，鍵跟英文那一份一樣。改完要重跑：

  ```bash
  node tools/presence/build-site-text.mjs
  ```

  它把 `lang/*.json` 併成 `site-text.js`（Worker 讀的是這一份）。少翻的鍵網頁會拿英文補，測試會指出是哪幾條。
- 加一種語言：在 `lang/` 加一份、在 `site.js` 的 `LANG_NAMES` 加它的名字。

用哪一種：網址上的 `?lang=ja`（貼連結給人用）> 訪客在頁首選過的（記在 `lang` 這個 cookie）> 瀏覽器的語言 > 英文。簡體的瀏覽器（`zh-CN`、`zh-SG`、`zh-Hans`）給簡體，其餘的中文給繁體。

### 登入

兩條路，進來之後是同一個身分：

1. **網站帳號（Discord）＋綁定遊戲 ID**——使用者 2026-10-12：「像 Wynntils 一樣先註冊，然後再綁定遊戲 ID」。
   - 帳號頁按「用 Discord 登入」（OAuth，只要 `identify`：代號與顯示名稱）。第一次登入就是註冊。
   - 登入後按「取得綁定碼」，網頁給一行 `/wcy link XXXXXXXX`（十分鐘內有效）。
   - 玩家在遊戲裡打那一行。模組帶著它驗過身分的通行證把碼送來，兩邊就綁起來；網頁會自己更新。
   - 之後用 Discord 在任何瀏覽器登入都是那個玩家，不必開遊戲。一個網站帳號綁一個遊戲帳號，重綁會把舊的解掉。
   - **反過來（先遊戲、後 Discord）不必打指令**：從遊戲開的網頁已經驗過遊戲帳號，帳號頁按「綁定 Discord」，去 Discord 登入回來就綁好了。
2. **從遊戲裡直接開**：F6 →「徽章與披風」→「披風一覽」。模組拿它的通行證向 `/api/web-link` 換一條一天有效的網址，瀏覽器開了就是登入的。

Discord 只證明 Discord 帳號。「這個遊戲帳號是本人」靠的是模組那張通行證（見下面〈身分怎麼驗〉），所以**綁定一定要從遊戲裡做**——讓人在網頁上自己填遊戲 ID 的話，誰都能把自己綁成開發者。

兩條路的憑證都放在網址的 `#` 後面（那一段不會送到任何伺服器），網頁收進 `localStorage`、從網址列上拿掉。

#### 開 Discord 登入要設的東西

沒設的話「用 Discord 登入」的按鈕不會出現，其他功能照常。

1. 到 <https://discord.com/developers/applications> 按 **New Application**，取個名字（玩家登入時會看到）。
2. 左邊 **OAuth2**：記下 **Client ID**；按 **Reset Secret** 拿 **Client Secret**（只顯示一次）。
3. 同一頁的 **Redirects** 加這兩條：

   ```
   https://wynnchayuan.com/auth/discord/callback
   https://www.wynnchayuan.com/auth/discord/callback
   ```

4. 在這個資料夾把兩個值設成 secret（各跑一次，貼上值按 Enter）：

   ```bash
   npx wrangler secret put DISCORD_CLIENT_ID
   ```

   ```bash
   npx wrangler secret put DISCORD_CLIENT_SECRET
   ```

不需要 Bot、不需要其他 scope。設好不必重新部署，`/api/status` 的 `discord` 會變成 `true`。

## API

| | | |
|---|---|---|
| `POST /api/challenge` | | 出一道題（登入的第一步） |
| `POST /api/login` | 憑證、對題目的簽名、個人檔案 | 驗過身分就發通行證（模組用） |
| `POST /api/web-link` | | 拿一條替瀏覽器登入的網址（要通行證） |
| `GET /auth/discord` | | 去 Discord 登入（網站帳號） |
| `GET /auth/discord/callback` | | Discord 登入完回來的地方 |
| `POST /api/link/start` | | 網站帳號要一組綁定碼 |
| `POST /api/link/intent` | | 已經以遊戲帳號登入的人要綁 Discord：開一張綁定單，去 Discord 登入回來就直接綁（不必打指令） |
| `POST /api/link` | `{code}` | 遊戲裡打 `/wcy link` 送來的：把遊戲 ID 綁到那個網站帳號（要模組的通行證） |
| `POST /api/unlink` | | 網站帳號解除綁定 |
| `GET /api/me` | | 我是誰、有沒有披風、是不是開發者（要通行證） |
| `POST /api/hello` | `{cape, rainbow, show}` | 報到（要通行證）：要不要顯示披風、葉子要不要彩色、要展示哪幾個身分的葉子 |
| `GET /api/list` | | 名單：有裝的人、有上傳披風的人、關掉披風的人、葉子開了彩色的人、各人挑了哪幾片葉子 |
| `PUT /api/cape` | 圖檔本體 | 上傳披風（要通行證） |
| `DELETE /api/cape` | | 拿掉自己的披風（要通行證） |
| `POST /api/cape/wear` | `{hash}` | 穿上披風一覽裡的某一件（要通行證）。上傳的規矩照舊：GIF 只有成員能穿、不能超過自己的檔案上限、被封禁的不行。原本自己傳的那一件收著 |
| `POST /api/cape/restore` | | 復原（要通行證）：換回套用之前自己傳的那一件；原本沒有披風就是脫下 |
| `POST /api/cape/star` | `{hash, on}` | 把一覽上的某一件加進／移出我的最愛（要通行證） |
| `GET /api/capes/mine` | | 我的最愛與我用過的披風（要通行證） |
| `POST /api/cape/forget` | `{hash}` | 把一件從我的使用紀錄拿掉；現在穿著的不能拿（要通行證） |
| `POST /api/cape/public` | `{on}` | 我的披風要不要出現在披風一覽（要通行證）。上傳時也可以帶 `?public=0`／`1` |
| `GET /api/admin/reviews` | | 審核名單（開發者） |
| `POST /api/admin/review` | `{uuid, note?}` 或 `{hash}` | 列入審核：一個人，或傳那張圖的人（開發者） |
| `POST /api/admin/unreview` | `{uuid}` | 從審核名單移除，等審核的那一件直接放行（開發者） |
| `POST /api/admin/approve` | `{hash}` | 通過審核（開發者） |
| `GET /api/cape/<代碼>` | | 下載某個人的披風 |
| `GET /api/capes` | | 所有上傳的披風，相同的圖併成一筆 |
| `GET /api/status` | | 有幾個人、幾件披風 |
| `GET /api/admin/capes` | | 同上，多了每一件是誰傳的（開發者） |
| `POST /api/admin/remove` | `{id}` 或 `{hash}`，加 `ban` | 拿掉披風、要不要封禁（開發者） |
| `GET /api/admin/perks` | | 特別名單：誰在上面（開發者） |
| `POST /api/admin/perk` | `{uuid, note?}` | 加進特別名單：那個人上傳的待遇跟名單成員一樣（GIF、20 MB），但不在 credits.json 上、沒有葉子（開發者） |
| `POST /api/admin/unperk` | `{uuid}` | 從特別名單移除（開發者） |
| `GET /api/admin/bans` | | 封禁名單（開發者） |
| `POST /api/admin/unban` | `{uuid}` | 解除封禁（開發者） |
| `POST /api/admin/credits` | `{section, name, mc, color}` | 替名單加一個人：開一個 PR（開發者） |
| `GET /api/team` | | 名單上的開發者、贊助者、貢獻者（名字、遊戲 ID、葉子顏色），加上貢獻公會（名字、標籤、圖的版本）。公開，首頁畫團隊用 |
| `GET /api/guild/<代碼>/<icon或banner>` | | 貢獻公會的圖示／旗幟（公開） |
| `GET /api/admin/guilds` | | 貢獻公會的名單，含誰加的（開發者） |
| `POST /api/admin/guild` | `{name, tag?}` | 加一個貢獻公會：`name` 要跟 Wynncraft 上的一樣（開發者） |
| `PUT /api/admin/guild/<代碼>/<icon或banner>` | 圖檔本體 | 上傳那個公會的圖示／旗幟：PNG、JPEG、WebP，512 KB 以內（開發者） |
| `POST /api/admin/unguild` | `{slug}` | 移除一個貢獻公會，連同它的圖（開發者） |
| `GET /api/admin/staff` | | 管理員名單（**只有開發者**） |
| `POST /api/admin/staff` | `{who, note?}` | 加一個管理員：`who` 是 Minecraft ID、Minecraft UUID 或 Discord 使用者 ID（**只有開發者**） |
| `POST /api/admin/unstaff` | `{key}` | 移除一個管理員（**只有開發者**） |

上面寫「開發者」的那幾條，管理員也做得到；寫「只有開發者」的不行。

## 它記什麼

- **記的**：玩家的 UUID（當成鍵）、要不要顯示披風、徽章的選擇（葉子要不要彩色、要展示哪幾個身分）、哪一天報到的；上傳的披風圖檔，連同上傳的人的帳號名（管理的人要知道是誰傳的）；被封禁的人（UUID、帳號名、時間、誰封的）。十四天沒再報到就從名單上消失；圖檔留著，回來報到就又出現。
- **網站帳號**：Discord 的代號與顯示名稱；綁了遊戲 ID 的話再加 UUID、遊戲名字與綁定時間。不拿 email、不拿伺服器清單，Discord 給的存取憑證用完就丟。
- **不記的**：IP、所在的世界、任何遊戲內容。
- **公開的名單與一覽不放 UUID 與帳號名**：放的是 UUID 雜湊出來的代碼。誰傳了哪一件只有開發者看得到。

## 身分怎麼驗

要證明的是「這個遊戲帳號是本人」。

**不能用「加入伺服器」那一套**（模組向 Mojang 說「我要加入 X」，伺服器再問 Mojang「他加入了嗎」）：Mojang 把 Cloudflare Worker 發出去的請求一律擋掉（403），那個問題從這裡永遠問不到答案。第一版就是這樣寫的，部署之後所有人都登不進來。

現在驗的是 Mojang **已經簽好**的東西，全程不必連它：

1. **玩家金鑰憑證**。1.19 起每個正版帳號都有一對 Mojang 發的金鑰（聊天簽章用的），公鑰連同 UUID 與到期時間由 Mojang 簽過（SHA1withRSA，內容是 UUID 16 個位元組＋到期時間 8 個位元組＋公鑰）。私鑰要帳號的登入憑證才拿得到。
2. **答題**。登記站出一道題（`/api/challenge`，五分鐘內有效），模組用那把私鑰簽 `wynnchayuan.com login\n<題目>\n<UUID>`（SHA256withRSA）。簽得出來＝手上有私鑰＝是本人；開頭綁住網域，這份簽名拿去別處沒有用。
3. **名字**。憑證裡沒有名字，另外驗個人檔案的 `textures` 屬性——也是 Mojang 簽的，裡面有 UUID、名字與時間。這份資料是公開的（誰都抓得到任何人的），所以它只回答「這個 UUID 叫什麼名字」，不能拿來證明身分。

模組送來的沒有一樣是祕密：公鑰、簽章、公開的個人檔案。**私鑰與登入憑證都留在玩家那裡。** 驗過之後發一張七天的通行證（HMAC 簽的，密鑰第一次用到時自己產生、存在 KV）；網頁的那一張只有一天。

離線帳號沒有憑證，驗不過。

### Mojang 的公鑰

Worker 自己抓不到（同一個原因），所以釘在 `mojang-keys.js`。Mojang 換公鑰的話登入會全部失敗：

```bash
node tools/presence/update-mojang-keys.mjs --check
```

不一樣就不帶 `--check` 重跑一次、重新部署。來不及部署時可以先設 `MOJANG_KEYS`（`https://api.minecraftservices.com/publickeys` 回的那個 JSON 整份貼上）：

```bash
npx wrangler secret put MOJANG_KEYS
```

## 誰可以上傳

`wrangler.toml` 的 `UPLOADS`：

- `tiered`（預設）：驗過身分的人都能傳 **PNG**；**GIF** 只有 `credits.json` 上有遊戲 ID 的成員（開發者、贊助者、貢獻者）。
- `listed`：只有名單上的成員能傳。
- `anyone`：驗過身分的人都能傳，PNG、GIF 都可以。
- `off`：不收。

圖的規則跟模組的 `CapeRules` 同一套：只收照範本畫的（22×17、44×34、88×68、176×136），檔案一般玩家 1 MB 以內、名單上的成員 20 MB 以內，GIF 最多 120 格。上傳成功會回尺寸，一覽與帳號頁都看得到每一件多大。見 `capes/README.md`。

相同的圖只存一份：圖檔照內容存（`blob:<SHA-256>`），每個人的披風那一筆（`cape:<代碼>`）只記它指到哪一張。沒有人用的圖在換圖、刪除時順手清掉。

## 管理

**不可以上傳的內容：政治、不雅或色情、違反法規、種族歧視或仇恨。**

誰有管理權：`credits.json` 開發者那一區的人。每個請求都會重新查一次名單，所以從名單上拿掉就立刻沒有權限。

**管理員**是開發者在 `/admin` 另外加的人：做得到下面這些管理（刪除、封禁與解除、特別名單、新增成員），但不在 `credits.json` 上——團隊名單沒有他、名字前面沒有葉子、上傳的待遇也跟一般玩家一樣——而且**不能新增或移除管理員**。加的時候填 Minecraft ID、Minecraft UUID 或 Discord 使用者 ID 其中一種：

- 照 Minecraft ID（名字）加的，那個人登入一次就改記成 UUID；名字之後改掉、被別人拿走也不會把權限帶走。
- 照 Discord 使用者 ID 加的，那個人只用 Discord 登入、還沒綁遊戲 ID 也進得了管理頁。
- 存在 KV 的 `staff:u:<UUID>`、`staff:n:<名字>`、`staff:d:<Discord ID>`；移除之後馬上沒有權限。

- **刪除、封禁**：在 `/capes` 點開一件披風，開發者看得到是誰傳的，還有「刪除這件披風」與「刪除並封禁上傳的人」。刪除是照圖刪：所有用同一張圖的人一起拿掉。
- **封禁名單**：`/admin`。誰、什麼時候、誰封的，可以解除。
- **貢獻公會**：`/admin`。先加公會的名字（要跟 Wynncraft 上的一模一樣，首頁會連到 `wynncraft.com/stats/guild/<名字>`）與標籤，再各上傳一張圖示與旗幟。圖在瀏覽器裡先縮小才傳（圖示最長邊 256、旗幟 512）。整份名單存在 KV 的 `guilds` 一筆，圖在 `gimg:<代碼>:icon`／`gimg:<代碼>:banner`。首頁最多晚五分鐘看到（`/api/team` 讓瀏覽器存五分鐘）。
- **套用來的披風**：玩家在披風一覽「穿上」別人的披風時，自己那一筆只是指到同一張圖，並把原本自己傳的那一件記在 `p`。照圖刪除（上面那一條）會連別人收著準備復原的那一份一起拿掉，刪了就穿不回來。
- **不公開**：玩家在帳號頁關掉「公開在披風一覽」，他那一筆就不出現在一覽上、別人不能穿也不能加最愛；遊戲裡別的玩家照樣看得到。管理的人在一覽看得到，標著「不公開」。
- **審核名單**（比封禁輕）：`/admin` 加 UUID，或在披風一覽點開一件按「上傳的人之後都要審核」。名單上的人照樣可以傳，但之後傳的披風標成等審核（`r`）：一覽與遊戲裡的名單都不給別人看，圖要帶雜湊（`?k=`）才拿得到。管理的人在一覽的「待審核」點開、按「通過審核」；通過過的雜湊記在他的 `review:<UUID>` 裡，再穿同一張不必重審。現在身上那一件不受影響。
- **使用紀錄與最愛**：`hist:<代碼>:<雜湊>`（一個人最多 8 件，留著的圖算有人在用）、`fav:<UUID>`、`stars`（每張圖幾個人加了最愛）。照圖刪除會把所有人的紀錄與人數一起清掉。
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

用放在記憶體裡的假資料跑同一支 Worker，塞了 `capes/templates` 的示範披風與一筆封禁。真的登入這裡沒有接，改成 `/dev-login?as=dev`（當成開發者）、`/dev-login?as=user`（當成一般玩家）、`/dev-login?as=discord`（剛用 Discord 註冊、還沒綁）；`/dev-link?code=XXXXXXXX` 假裝遊戲裡打了 `/wcy link`。

## 測試

```bash
node tools/presence/worker.test.mjs
```
