# 語料收集站

模組收集到的「還沒翻到的句子」可以分享回來，讓語料不必靠一個人跑遍全圖。
這個資料夾是收集站本體：一支 Cloudflare Worker，加一個 KV。

跑在免費額度裡綽綽有餘 —— 每天十萬次請求，這條路一天大概用掉幾百次。

## 它存什麼、不存什麼

**存的**：遊戲裡的英文原文、它出現在哪一類畫面（`dialogue/`、`gui/`…）、
看過幾次、送出者的模組版本。

**不存的**：帳號名、UUID、座標、世界、IP。限流用的是「雜湊過的 IP 前 16 碼」，
而且一小時就過期 —— 它只能回答「這個來源這小時送了幾次」，回答不了「是誰」。

**不收的**：公會、隊伍、喊話、私訊。那些是別人打的字，翻譯價值趨近於零，
而風險是把陌生人的話散到公開倉庫裡。模組端 `CorpusUpload#shareable`、
Worker 的 `acceptable`、以及 `tools/import-captured.py` 的 `NAMED`
三個地方各擋一次 —— 這種東西漏一次就收不回來。

## 部署

需要一個 Cloudflare 帳號（免費）與 `npx`。

```bash
cd tools/collector
npx wrangler login
npx wrangler kv namespace create CORPUS
```

最後一行會印出一個 id，填進 `wrangler.toml` 的 `[[kv_namespaces]].id`。

接著設定 Action 來撈資料時用的密語（隨便一串長亂碼都行）：

```bash
npx wrangler secret put DRAIN_TOKEN
```

然後部署：

```bash
npx wrangler deploy
```

部署完會印出網址，像 `https://wynnchayuan-corpus.你的名字.workers.dev`。

## 接起來

還有三件事：

1. **倉庫根目錄的 `collect.json`**：把 `endpoint` 填成
   `https://.../submit`，`enabled` 改成 `true`。
   模組每 30 分鐘讀一次這個檔，所以之後要暫停收集只要把 `enabled` 改回
   `false` —— 不必重新發 jar，也不必玩家重開遊戲。

2. **倉庫的 Secrets**（Settings → Secrets and variables → Actions）：
   - `COLLECTOR_URL` = Worker 的網址（不含路徑）
   - `COLLECTOR_TOKEN` = 上面那個 `DRAIN_TOKEN`

3. 沒了。`.github/workflows/collect-inbox.yml` 每天會自己撈一次、
   分類進語料、開一個 PR 等人看過。

## 手動撈一次

```bash
curl -H "Authorization: Bearer $DRAIN_TOKEN" https://.../drain > inbox.json
python tools/import-captured.py inbox.json --write
```

不加 `--write` 就只是預覽，什麼都不會動。
