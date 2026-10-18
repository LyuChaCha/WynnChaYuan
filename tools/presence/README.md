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
| `site.js` | 五張網頁與「找不到這一頁」（共用樣式、頁首、頁尾），中文與英文 |
| `mojang-keys.js` | 釘住的 Mojang 公鑰（`update-mojang-keys.mjs` 產生的） |
| `lang/*.json`、`site-text.js` | 其他語言的字，與 `build-site-text.mjs` 併出來給 Worker 讀的那一份 |
| `demo-data.js` | 功能頁即時預覽用的字：選項的名稱與說明（模組設定畫面的語言檔）、場景裡的譯文（語料）。`python tools/presence/build-demo.py` 產生，`--check` 比對；那幾條語料或語言檔改了要重跑 |
| `demo-fonts.js` | 預覽畫面用的點陣字：從模組附的 Fusion Pixel 10px（SIL OFL 1.1，授權全文在 `assets/wynnchayuan/font/ofl-fusion.txt`）裁出場景裡出現的那幾百個字，四份共 75 KB 左右。同一支 `build-demo.py` 產生（要裝 fontTools；`--check` 不必裝，只看涵蓋的字夠不夠）。Worker 當成 `/font/px-<代號>.ttf` 送出去 |
| `textures/`、`demo-textures.js` | 預覽畫面用的材質：遊戲裡真的那幾張（物品說明的框、背包、對話框與頭像、生命與魔力、快捷列、通用的小圖示）、沒穿裝備的格子裡那幾個影子、各稀有度的說明框與徽記、官方的物品圖示、兩張實機截圖當背景（都在 Detlas 一帶，跟預覽裡 22 等的人物同一個等級；街上那一張是真的 NPC，頭上的名牌從截圖上拿掉、改由網頁畫，才能跟著語言換）、背包裡那個人的皮膚（`skin.png`，使用者自己的；網頁用 CSS 的 3D 把它貼成方塊人，會看著滑鼠）。**這些圖是 Wynncraft 的（© Wynncraft），不在這個倉庫的授權範圍裡**，只用在網站的預覽上、不進模組的 jar；頁尾有一句說明。`python tools/presence/build-textures.py` 把 `textures/` 打包成 `demo-textures.js`（`--check` 比對）；要重做材質本身：`--pack` 從資源包重組（玩過 Wynncraft 的客戶端 `downloads/` 底下有快取；那個 zip 被動過手腳，腳本自己解）、`--icons` 重抓官方 CDN 的物品圖示、`--shots <資料夾>` 重做背景（資料夾裡要有 `world.png` 與 `street.png`）、`--skin <玩家名字>` 重抓皮膚。Worker 當成 `/tex/<檔名>` 送出去 |
| `cape-templates.js` | 披風範本每一種尺寸、兩份說明圖（中文、英文）與一張會動的示範，從 `capes/templates/` 打包的。`node tools/presence/build-cape-templates.mjs`（`--check` 比對）；範本改了要重跑。Worker 當成 `/template/<檔名>` 送出去，披風一覽與帳號頁的下載鈕用它 |
| `cape-preview.js` | 披風的預覽圖（`/api/cape-preview/<雜湊>`，Discord 機器人的 `/cape` 用）：跟披風一覽的卡片一樣只露出背面、像素放大、墊上卡片的深綠底。Worker 沒有 canvas，PNG 與 GIF 的解碼、編碼都在這支裡（不靠套件；GIF 逐格切、每格自己的調色盤） |
| `icons.js` | 網站的圖示（`make-icons.py` 從 `logo/2026` 縮出來的；`/favicon.png`、`/icon-192.png`、`/apple-touch-icon.png`） |
| `worker.test.mjs` | `node tools/presence/worker.test.mjs` |
| `dev.mjs` | 在本機用假資料開網站來看 |

## 網頁

| | |
|---|---|
| `/` | 首頁：模組是什麼、下載、團隊（開發者、贊助者、貢獻者照 `credits.json` 畫成名冊，一個人一列，點下去是他在 Wynncraft 的頁面；下面是管理頁登記的貢獻公會，有旗幟與圖示）、關於贊助 |
| `/features` | 功能：最上面是即時預覽（照遊戲裡設定畫面右邊那一塊做）：上面一整面「遊戲畫面」——後面是實機截圖，前面是遊戲裡真的那幾張材質（物品說明照現在的版面：徽記、標籤、傷害、需求、詞條；背包；對話框與頭像；生命、魔力與快捷列），位置照實機截圖擺，字是遊戲裡那一套點陣字；物品那一面背包裡有三樣東西（骨弓、無瑕橡木弓、腐爛血肉——數值是官方 API 的，版面照實機截圖）可以用滑鼠指、點或用鍵盤移過去，說明跟著換；下面幾列是真的選項，一動畫面就跟著變。物品／對話／世界與聊天三頁、七種譯文語言。首頁最上面那一塊用的是同一份（`site.js` 的 `makeStage`），自己輪著換語言與場景，下面照分類列出翻譯範圍、工具、葉子與披風、相容的模組——每一項一張卡，前面一顆遊戲通用的圖示；翻譯範圍那十張各附一句預覽裡的例子（原文在上、譯文在下，跟著預覽選的語言換） |
| `/capes` | 披風一覽。最上面一條「做自己的披風」，按了展開**披風範本**那一塊（說明圖、每一種尺寸的下載鈕、會動的示範；帳號頁上傳的地方也有同一塊）。每張卡片是披風的正面；點開是一個穿著它的人偶，可以拖曳旋轉（立體的部分用 jsDelivr 上的 skinview3d，載不到就只看平面的圖）。可以排序（最新、最早、最多人使用、人氣、最近有人穿上）與篩選（PNG／GIF／我的最愛／我用過的）；登入的人可以**穿上**、**復原**、按星星加進**我的最愛**。管理的人看得到全部，多「不公開」「待審核」兩個篩選 |
| `/account` | 帳號：登入狀態、自己的披風、上傳與移除 |
| `/admin` | 管理（開發者與管理員）：回饋（最上面）、封禁名單、特別名單、貢獻公會、新增成員、譯者名單；管理員名單只有開發者看得到 |
| `/translate` | 協作翻譯：選一個語言，搜尋或瀏覽語料（原文、譯文、檔名），對任一句提新譯法；整句提案與術語投票兩個分頁。那個語言的譯者（管理頁指派）可以提案與投票，開發者與管理員定案。整句定案＝替倉庫開 PR（同一個語言同一天的放同一個 PR，任務檔的句子連合併檔一起改）；術語定案只記下結果，套到整份語料由維護的人做。送出前照 `placeholderProblem` 檢查佔位符（`{#}{p}{u}` 順序、`{~}` 數量與編號、顏色標記、行數），網頁與 Worker 同一套。編輯時有「實機預覽」：{~} 帶入模擬數值、標出接的是原文第幾個數字，{p}／{u}／{#}／顏色標記照遊戲的樣子畫；有插入佔位符與「幫我編號」（{~} 依序改成 {~1}、{~2}…）的按鈕 |
| `/feedback` | 回饋與建議（使用者 2026-10-17）：分類（譯文建議、內容建議、網站建議、功能建議、其他）、想說的話（必填，最多 2000 字）、可以不填的「在哪裡」（最多 200 字；譯文建議時特別請他填，另外多問一個語言）。要用 Discord 登入（從遊戲開的網頁要綁了 Discord 才行），一個人一小時最多 5 則（跟機器人的 `/回饋` 共用）。**譯文建議會自動在 GitHub 開 Issue**（見下面〈回饋〉）；其他分類只留在網站上，管理頁看得到。頁首與頁尾（聯絡那一欄）都有連結；`?cat=translation` 可以先選好分類 |
| `/privacy`、`/terms` | 隱私政策、服務條款（Discord 應用程式驗證要的兩個連結）。內容照下面的〈它記什麼〉與倉庫 README 的「會送出的資料」，那兩處改了這裡要跟 |
| 其他網址 | 404，但是一頁有頁首頁尾、給幾條回去的路的網頁（照訪客的語言）；`/api/` 底下與不是 GET 的照舊回 JSON |

每一頁的頁首見下面〈頁首〉；頁尾有下載點、GitHub 與兩份授權、聯絡方式，以及「與 Wynncraft、Mojang、Microsoft 沒有關係」那一行。玩家的頭像與全身圖用 [minotar](https://minotar.net)（照名字畫，不必先查 UUID）。

### 頁首

使用者 2026-10-17 選的 A 案：永遠只有一列。

- **左邊**：標誌，然後是公開的頁面（首頁、功能、披風、協作翻譯、回饋）。
- **右邊**依序是：
  - 贊助：只有愛心圖示，滑過或用鍵盤停上去會浮出「贊助」。
  - 綠色的「下載 ▾」：Modrinth、CurseForge、GitHub Releases。
  - 帳號那一塊：
    - 登入之後是頭像，按了是帳號選單：名字、我的帳號、管理（只有 staff）、語言（就地展開八種）、登出。
    - 沒登入是「登入」加一顆地球（語言選單）。
- **放不下的時候**（priority+）：程式量選單那一段剩多寬，從順位小的開始收進最後的「更多 ▾」。
  - 「更多」只有真的收了東西才出現。
  - 這一頁收在裡面的話，「更多」本身亮起來。
  - 視窗變寬變窄、登入後頭像換上來、字型載好都會重新量。
- **手機（720px 以下）**：只留標誌、愛心與漢堡選單。面板裡從上到下是選單、下載、帳號、語言。
- **鍵盤**：
  - 選單按鈕帶 `aria-expanded`／`aria-haspopup`。Enter、空白鍵、↓ 打開並停在第一項。
  - 選單裡 ↑↓ 移動、Home／End 到頭尾。Esc 關掉，焦點回到按鈕。
  - 點外面或焦點離開也會關。一次只開一張。
- **動作**（使用者 2026-10-17 要「更順、加一點巧思」）：
  - 選單打開 180–200ms：從按鈕那一角長出來，淡入、從 0.96 放大、往下幾 px。關掉 120ms，收得比開得快。
  - 裡面一項接一項淡進來，相隔 18ms，只在打開時。
  - 手機面板蓋在頁面上，從上面滑下來。
  - 都是 transition、只動 transform 與 opacity，開到一半再關會從當下的位置接著走。曲線是 `--out`（打開）與 `--exit`（關掉）。
  - 箭頭打開時轉過來；按下去縮到 0.97。
  - 下載鈕滑上去時一道斜光掃過一次，不循環。
  - 地球與愛心是同一種浮出提示。
- **飄過的葉子**：跟 README 橫幅同一套像素葉子（桌面 `discord-banner/leafart.py`、`animate4.py`）。
  - 每隔 5–10 秒來一陣風，帶一兩片葉子從左邊飄過頁首。風鋒到了葉子才被吹快、翻得快。
  - 轉向只用 90 度的倍數加鏡射，像素永遠方正（使用者不要任意角度的旋轉）。
  - 畫在 `.top` 裡一張小畫布上（`#hn-leaves`，一格像素＝2px），在字的後面、`pointer-events: none`。
  - 畫面上沒東西就不跑 `requestAnimationFrame`；分頁藏起來就停。
  - 系統設定「減少動態」時完全不出現，光也拿掉。
  - 畫布元素上有 `hnBlow()` 可以馬上吹一陣，測試與錄影用。

選單是資料，都在 `site.js` 開頭「頁首」那一段：

| | |
|---|---|
| `NAV_ITEMS` | 上面那一排：`{ key, href, label, priority, group?, role? }` |
| `NAV_GROUPS` | 群組：`{ <group>: { label, priority?, role? } }` |
| `ACCOUNT_ITEMS` | 頭像選單的連結：同樣的欄位，多一個 `icon`（`ICON` 的名字） |

- **加一頁**：
  1. 在 `NAV_ITEMS` 加一行，例如 `{ key: 'guide', href: '/guide', label: 'nav.guide', priority: 50 }`。
  2. `label` 是字的鍵：繁中與英文寫在 `site.js` 的 `TEXT`，其他六種寫在 `lang/*.json`，再跑 `build-site-text.mjs`。少一種測試會擋。
  3. 頁面本身照舊加在 `PAGES`。
  4. `priority` 決定窄的時候誰先收進「更多」，數字小的先收。現在是首頁 100、披風 90、功能 80、協作翻譯 70、回饋 60。
- **只給某些人看**：加 `role: 'staff'`。值是 `/api/me` 回的欄位，例如 `staff`、`developer`、`member`。
  - 畫出來先是 `hidden`、帶著 `data-need`，程式問過 `/api/me` 才打開。沒登入一律看不到。
  - 這只是畫面；真正的權限照舊由 API 自己檢查。
- **收成一個下拉選單**：幾頁寫上同一個 `group`，在 `NAV_GROUPS` 寫它的名字。例如
  1. 寫上 `community: { label: 'nav.community', priority: 65 }`。
  2. 協作翻譯與回饋那兩行加上 `group: 'community'`。
  3. 頁首就多一個「社群 ▾」，放在組裡第一頁原本的位置。
  - 群組沒寫 `priority` 就用組裡最大的那一個。
  - 收進「更多」時，群組變成一個小標題加底下的幾頁；手機面板也一樣。
  - 現在沒有要分組的，`NAV_GROUPS` 是空的。

樣式與 id 一律用 `hn-` 開頭（加之前 grep 過沒有撞名）。測試在 `worker.test.mjs` 的〈頁首〉那一段：

- 清單怎麼畫、群組與身分限定。
- 沒登入與登入的樣子：拿頁面上的 `headerAccount` 在假的 document 上跑。
- 三份語言連結。

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

登入不需要 Bot、不需要其他 scope。設好不必重新部署，`/api/status` 的 `discord` 會變成 `true`。

#### Discord 機器人（斜線指令）

使用者 2026-10-16：「用 DC 看一些東西，例如當前使用者披風、網站狀態，或查看譯文」。跟登入用同一個 Discord 應用程式。
指令用 HTTP 送到 `/api/discord/interactions`（Interactions Endpoint），Worker 直接回，不需要常駐的機器人主機；它只收得到斜線指令，讀不到聊天。

| 指令 | 繁中名稱 | 回什麼 |
|---|---|---|
| `/status` | `/狀態` | 裝了模組的人數、穿披風的人數、待審核的披風、各語言投票中的提案與術語 |
| `/cape player:<名字>` | `/披風` | 那個人現在的披風，圖是那個人的 skin 穿著披風、從背後斜看的立體人物（角度跟網頁的 3D 檢視一樣）（skin 從 minotar 拿，拿不到畫灰色人偶；GIF 披風會動）。先回「處理中」，預覽圖算好存進 KV 才補上，Discord 來抓圖時不必現算。不公開、等審核的不給圖 |
| `/translate text:<關鍵字> [language]` | `/翻譯` | 原文或譯文含有那個字的前五條；語言預設跟著 Discord 的介面語言 |
| `/feedback category:<分類> message:<內容> [where] [language]` | `/回饋`（簡中 `/反馈`） | 送一則回饋，跟網站的回饋頁進同一個地方、共用限流；Discord 已經告訴我們是誰，不必另外登入。回覆只給本人看（ephemeral）。譯文建議要開 GitHub Issue，先回「處理中」再補上 Issue 網址；沒帶 `language` 就照 Discord 的介面語言（英文介面當成沒指定） |

回覆的語言跟著使用者的 Discord 介面（繁中、簡中、其他給英文）。`/translate` 要讀一份八九 MB 的索引，先回「處理中」再補上結果。

1. Developer Portal → **General Information**：**Public Key** 寫在 `wrangler.toml` 的 `DISCORD_PUBLIC_KEY`（公開的值）。同一頁的 **Terms of Service URL**、**Privacy Policy URL** 填 `https://wynnchayuan.com/terms`、`https://wynnchayuan.com/privacy`。
2. **Bot** → **Reset Token**，在這個資料夾設成 secret（**不要貼在任何地方**）：

   ```bash
   npx wrangler secret put DISCORD_BOT_TOKEN
   ```

3. 部署之後，**General Information** 的 **Interactions Endpoint URL** 填 `https://wynnchayuan.com/api/discord/interactions`，按存檔（Discord 會先打一次 PING 驗簽名，過了才存得進去）。
4. 管理頁的「Discord 機器人」按「註冊指令」（只有開發者看得到）；指令改了要再按一次（2026-10-17 加了 `/回饋`，部署之後要再按一次才會出現）。同一處有邀請連結（`applications.commands` + `bot`，不要任何權限）。

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
| `GET /api/capes/mine` | | 我的最愛與我用過的披風（要通行證）。已經被移除的會順手清掉，`removed` 是這一次清了幾件 |
| `GET /api/idle/<雜湊>` | | 沒有人穿、還在倒數的那一張圖（公開；披風一覽上「沒人穿」的卡用） |
| `POST /api/cape/forget` | `{hash}` | 把一件從我的使用紀錄拿掉；現在穿著的不能拿（要通行證）。只是少一筆紀錄，不影響那張圖留多久 |
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
| `GET /api/admin/people?q=` | | 已註冊的人（網站帳號 `d:*` 與傳過披風的人）；沒帶 `q` 回全部，帶了照 Discord 名稱、Minecraft ID、UUID、Discord 使用者 ID 篩；管理頁選人的下拉選單用（開發者與管理員） |
| `GET /api/admin/translators` | | 譯者名單（開發者與管理員） |
| `POST /api/admin/translator` | `{who, langs, note?}` | 加一位譯者，或改他負責的語言（同一個人再送一次就是覆寫）；`langs` 是語料的語言代號（zh_tw…tr_tr） |
| `POST /api/admin/untranslator` | `{key}` | 移除一位譯者 |
| `GET /api/cape-preview/<雜湊>[?u=<UUID>]` | | 披風的預覽圖（PNG 或 GIF）。沒帶 `u`：只露出披風背面（跟一覽的卡片一樣），存在 KV 的 `prev:<雜湊>`；帶了 `u`：那個人的 skin 穿著披風、從背後斜看，存 `prev:<雜湊>:<UUID>:<畫法>` 三天（skin 會換；換了畫法鍵跟著換，Discord 的圖片網址也帶 `&v=<畫法>`）。圖被刪掉時全部一起刪；雜湊就是鑰匙 |
| `POST /api/feedback` | `{cat, msg, where?, lang?}` | 送一則回饋（要用 Discord 登入，或從遊戲開、綁了 Discord 的網頁；沒登入 401 `login first`、沒綁 401 `discord first`）。`cat` 是 `translation`／`content`／`website`／`feature`／`other`；`lang` 是語料的語言代號，可以不帶。一小時滿 5 則回 429 `slow down` 與 `until`。回 `{id, issue, issueFailed}` |
| `GET /api/admin/feedback?cat=&st=` | | 回饋列表，新的在前，照分類與狀態（`open`／`done`）篩；一次最多 200 則，另回 `total` 與兩種狀態的數量（開發者與管理員） |
| `POST /api/admin/feedback/status` | `{id, st}` | 標成已處理（`done`，記下誰、什麼時候）或重新開啟（`open`）（開發者與管理員） |
| `POST /api/admin/feedback/delete` | `{id}` | 刪掉一則（垃圾訊息）；已經開的 Issue 留在 GitHub（開發者與管理員） |
| `POST /api/admin/feedback/issue` | `{id}` | 譯文建議的 Issue 上次沒開成：再開一次（開發者與管理員） |
| `POST /api/discord/interactions` | Discord 送來的 | 斜線指令；驗 Ed25519 簽名（`DISCORD_PUBLIC_KEY`），驗不過回 401 |
| `GET /api/admin/discord` | | 機器人的設定齊了沒有、Interactions Endpoint、邀請連結（**只有開發者**） |
| `POST /api/admin/discord/register` | | 把指令註冊到 Discord（全域、整批覆寫；要 `DISCORD_BOT_TOKEN`；**只有開發者**） |
| `GET /api/tr/me` | | 我是誰、是哪幾個語言的譯者、能不能定案（沒登入也回） |
| `GET /api/tr/index/<語言>` | | 那個語言的語料索引：從 `translate-index` 分支代讀，快取十分鐘 |
| `GET /api/tr/props?lang=`／`GET /api/tr/prop?lang=&id=` | | 整句提案的列表（metadata）／一筆的內容 |
| `POST /api/tr/propose` | `{lang, file, key, src, base, text}` | 提一個譯法（譯者）；同一句已經有人提過一樣的字就當成投它 |
| `POST /api/tr/vote` | `{lang, id, item}` | 投一票，再按一次收回；一句一個人一票（譯者） |
| `POST /api/tr/accept`／`POST /api/tr/reject` | `{lang, id, item}`／`{lang, id}` | 定案：寫進 `translate/<語言>-<日期>` 分支並開 PR／駁回（開發者與管理員；要 `GITHUB_TOKEN`） |
| `GET /api/tr/polls?lang=`／`GET /api/tr/poll?lang=&id=` | | 術語投票的列表／一筆的內容 |
| `POST /api/tr/poll` | `{lang, term, note?, options?}` | 開一個術語投票（譯者） |
| `POST /api/tr/poll/option`／`/vote` | `{lang, id, text}`／`{lang, id, option}` | 加候選（等於投它）／投票，一個人一票（譯者） |
| `POST /api/tr/poll/decide`／`/reopen` | `{lang, id, option}`／`{lang, id}` | 術語定案／重新開放（開發者與管理員） |

上面寫「開發者」的那幾條，管理員也做得到；寫「只有開發者」的不行。

## 它記什麼

- **記的**：玩家的 UUID（當成鍵）、要不要顯示披風、徽章的選擇（葉子要不要彩色、要展示哪幾個身分）、哪一天報到的；上傳的披風圖檔，連同上傳的人的帳號名（管理的人要知道是誰傳的）；被封禁的人（UUID、帳號名、時間、誰封的）。十四天沒再報到就從名單上消失；圖檔留著，回來報到就又出現。
- **網站帳號**：Discord 的代號與顯示名稱；綁了遊戲 ID 的話再加 UUID、遊戲名字與綁定時間。不拿 email、不拿伺服器清單，Discord 給的存取憑證用完就丟。
- **回饋**：Discord 的代號與顯示名稱、分類、內容（含「在哪裡」與語言）、時間、從網站還是機器人送的；限流用的送出時間（一小時後消失）。只有開發者與管理員看得到；譯文建議另外變成公開的 GitHub Issue，上面只有顯示名稱。標成已處理的 180 天後自動刪除，還沒處理的留到處理或刪除。
- **不記的**：IP、所在的世界、任何遊戲內容。
- **公開的名單與一覽不放 UUID 與帳號名**：放的是 UUID 雜湊出來的代碼。誰傳了哪一件只有開發者看得到。

## 回饋

使用者 2026-10-17 決定：網站一頁 `/feedback`、機器人一個 `/回饋`，送進同一個地方（KV `fb:*`）；要用 Discord 登入；一個人一小時最多 5 則（`FEEDBACK_PER_HOUR`）。

- **譯文建議**自動在倉庫開 Issue（`POST /repos/…/issues`，用同一把 `GITHUB_TOKEN`）：標題是「譯文建議：」加內容的前 60 個字；內文是內容、在哪裡、語言與 **Discord 顯示名稱**（放在行內程式碼裡，不會 @ 到人），**不放 Discord 代號**；內容放在程式碼區塊裡，圍欄比內容裡最長的一串反引號多一個。加 `translation` 標籤（跟 `.github/ISSUE_TEMPLATE/translation.md` 同一個；倉庫裡還沒有這個標籤的話，GitHub 會在第一次用到時自己建）。
- **先存再開 Issue**：GitHub 失敗（沒設 token、權限不夠、連不上）回饋照樣存著，原因記在 `issueError`，管理頁標紅、可以按「再開一次 Issue」。
- 其他分類只留在網站上。
- 管理頁最上面一塊：新的在前，照分類與狀態篩；標成已處理（180 天後 KV 自己刪掉）、重新開啟、刪除。開發者與管理員都看得到，看得到 Discord 代號。

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

相同的圖只存一份：圖檔照內容存（`blob:<SHA-256>`），每個人的披風那一筆（`cape:<代碼>`）只記它指到哪一張。

**沒有人穿的披風留七天**（使用者 2026-10-13：防止占用伺服器空間）。最後一個人換掉、脫下之後，那張圖記進閒置清單（KV 的 `idle` 這一筆：`{雜湊: {t, s, x, y, c, at, q}}`），圖先留著。這七天裡它照樣在披風一覽上，卡片標著「沒人穿・剩 N 天」，誰都可以再穿——一穿上就不再倒數。期滿沒有人穿就刪圖；加了星號的也一樣。

- 有人收著它準備復原（套用別人的披風時，自己原本那一件）算在用，不進清單。使用紀錄與最愛**不算**在用。
- 最後一個穿的人設了不公開、或那一件還在等審核：清單上記 `q`，一覽上不出現，只有本人從自己的紀錄穿得回去；管理的人在一覽看得到。
- 管理的人移除的不留，馬上刪。
- 加了星號或用過的披風被移除了：那個人下次開披風一覽時，`/api/capes/mine` 回 `removed`，頁面上說一聲「有 N 件已經移除」，只說一次。
- 期滿的在有人看披風一覽時順手刪；另外每天一次的整理（`wrangler.toml` 的 `[triggers]`，UTC 20:41）會刪掉期滿的，並把「沒有人在用、清單上也沒有」的圖補記進去（不公開，從那一刻起算七天）——改規則之前只被使用紀錄留住的舊圖就是這樣清掉的。

## 管理

**不可以上傳的內容：政治、不雅或色情、違反法規、種族歧視或仇恨；廣告（商業宣傳、宣傳其他伺服器、真錢交易例如用現金買賣 LE、現實生活的聯絡方式——公會宣傳不算）；以及其他違反 Wynncraft 規章的內容。也不可以濫用上傳。**

上傳的限制（`worker.js` 的 `handleUpload`）：同一個人 20 秒內不能連傳；一小時內傳滿 3 次就鎖 12 小時（KV `upl:<UUID>`，`burstNote`／`burstLock`，回 429 `cooldown` 與 `until`）；在審核名單上的人已經有一件自己傳的披風（等審核的也算，從一覽穿別人的不算）就回 403 `review one`，要先移除。

誰有管理權：`credits.json` 開發者那一區的人。每個請求都會重新查一次名單，所以從名單上拿掉就立刻沒有權限。

**管理員**是開發者在 `/admin` 另外加的人：做得到下面這些管理（刪除、封禁與解除、特別名單、新增成員），但不在 `credits.json` 上——團隊名單沒有他、名字前面沒有葉子、上傳的待遇也跟一般玩家一樣——而且**不能新增或移除管理員**。加的時候填 Minecraft ID、Minecraft UUID 或 Discord 使用者 ID 其中一種：

- 照 Minecraft ID（名字）加的，那個人登入一次就改記成 UUID；名字之後改掉、被別人拿走也不會把權限帶走。
- 照 Discord 使用者 ID 加的，那個人只用 Discord 登入、還沒綁遊戲 ID 也進得了管理頁。
- 存在 KV 的 `staff:u:<UUID>`、`staff:n:<名字>`、`staff:d:<Discord ID>`；移除之後馬上沒有權限。
- 回饋：`fb:<編號>`（16 個十六進位字元）→ `{id, cat, msg, where, lang, uid, name, via, t, st, done, issue, issueError}`，列表要看的放 metadata（`c` 分類、`st`、`t`、`n` 名字、`s` 摘要、`i` Issue 網址、`e` 失敗原因）；標成已處理時加上 180 天的 `expirationTtl`，重新開啟就拿掉。限流 `fbr:<Discord 代號>` → 最近一小時送出的時間（`expirationTtl` 一小時）。
- 協作翻譯：譯者 `tl:<u:|d:|n:>…` → `{langs, n, t, by, m, dn}`；整句提案 `tp:<語言>:<編號>`、術語投票 `tq:<語言>:<編號>`，列表要看的放 metadata。
  語料索引不放 KV：`.github/workflows/translate-index.yml` 在語料有變動時跑 `tools/build-translate-index.py`，整個蓋掉 `translate-index` 分支。
  本機的 `dev.mjs` 第一次用到時自己跑那支產生器（要有 python）。
- 從搜尋選人時，有綁遊戲帳號的記 `staff:u:<UUID>`；用綁著那個遊戲帳號的 Discord 登入網站也算管理員。網站帳號 `d:<Discord ID>` 的 metadata 放著名字與綁的帳號（`n`、`m`、`u`），搜尋只要列鍵；改版前存的沒有 metadata，下次登入就補上，搜尋時先讀本體。

- **刪除、封禁**：在 `/capes` 點開一件披風，開發者看得到是誰傳的，還有「刪除這件披風」與「刪除並封禁上傳的人」。刪除是照圖刪：所有用同一張圖的人一起拿掉。
- **封禁名單**：`/admin`。誰、什麼時候、誰封的，可以解除。
- **貢獻公會**：`/admin`。先加公會的名字（要跟 Wynncraft 上的一模一樣，首頁會連到 `wynncraft.com/stats/guild/<名字>`）與標籤，再各上傳一張圖示與旗幟。圖在瀏覽器裡先縮小才傳（圖示最長邊 256、旗幟 512）。整份名單存在 KV 的 `guilds` 一筆，圖在 `gimg:<代碼>:icon`／`gimg:<代碼>:banner`。首頁最多晚五分鐘看到（`/api/team` 讓瀏覽器存五分鐘）。
- **套用來的披風**：玩家在披風一覽「穿上」別人的披風時，自己那一筆只是指到同一張圖，並把原本自己傳的那一件記在 `p`。照圖刪除（上面那一條）會連別人收著準備復原的那一份一起拿掉，刪了就穿不回來。
- **不公開**：玩家在帳號頁關掉「公開在披風一覽」，他那一筆就不出現在一覽上、別人不能穿也不能加最愛；遊戲裡別的玩家照樣看得到。管理的人在一覽看得到，標著「不公開」。
- **審核名單**（比封禁輕）：`/admin` 加 UUID，或在披風一覽點開一件按「上傳的人之後都要審核」。名單上的人照樣可以傳，但之後傳的披風標成等審核（`r`）：一覽與遊戲裡的名單都不給別人看，圖要帶雜湊（`?k=`）才拿得到。管理的人在一覽的「待審核」點開、按「通過審核」；通過過的雜湊記在他的 `review:<UUID>` 裡，再穿同一張不必重審。現在身上那一件不受影響。
- **使用紀錄與最愛**：`hist:<代碼>:<雜湊>`（一個人最多 8 件；只是紀錄，不會把圖留住）、`fav:<UUID>`、`stars`（每張圖幾個人加了最愛）。照圖刪除會把所有人的紀錄與人數一起清掉。
- **新增成員**：`/admin` 的表單。送出之後在倉庫開一個分支、改 `credits.json`、開 PR；合併還是照倉庫的規矩來（檢查要過、要有人按）。要先設好 `GITHUB_TOKEN`：

  ```bash
  npx wrangler secret put GITHUB_TOKEN
  ```

  用 fine-grained token，只給這個倉庫，權限 **Contents: Read and write**、**Pull requests: Read and write** 與 **Issues: Read and write**（回饋的譯文建議開 Issue 用；少了這一項 Issue 會開不成，回饋照樣存著，管理頁標出 `github 403`）。沒設的話表單會說還沒設定，其他功能不受影響。

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

要一起上去的檔案：`worker.js`、`site.js`、`site-text.js`、`demo-data.js`、`demo-fonts.js`、`demo-textures.js`、`cape-templates.js`、`cape-preview.js`、`mojang-keys.js`、`icons.js`、`wrangler.toml`。`wrangler.toml` 裡有每天一次的排程（`[triggers]`），部署時會一起設好。

## 在本機看網站

```bash
node tools/presence/dev.mjs
```

用放在記憶體裡的假資料跑同一支 Worker，塞了 `capes/templates` 的示範披風與一筆封禁。真的登入這裡沒有接，改成 `/dev-login?as=dev`（當成開發者）、`/dev-login?as=user`（當成一般玩家）、`/dev-login?as=discord`（剛用 Discord 註冊、還沒綁）；`/dev-link?code=XXXXXXXX` 假裝遊戲裡打了 `/wcy link`。

## 測試

```bash
node tools/presence/worker.test.mjs
```
