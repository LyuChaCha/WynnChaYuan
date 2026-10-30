// 在本機開網站來看：node tools/presence/dev.mjs [埠號] [公會圖示.png] [公會旗幟.png]
//
// 用一個放在記憶體裡的假 KV 跑同一支 worker，先塞幾件 capes/templates 的示範披風與一筆封禁。
// 只給改頁面的時候看樣子用——真的登入要連 Mojang，這裡沒有接，改成：
//   /dev-login?as=dev    當成名單上的開發者登入（名字用 credits.json 的第一位開發者）
//   /dev-login?as=user   當成一般玩家登入
//   /dev-login?as=staff  當成管理員登入（不在名單上，但管理頁開得起來）
//   /dev-login?as=discord  當成剛用 Discord 註冊、還沒綁遊戲 ID 的人登入
//   /dev-link?code=XXXXXXXX  假裝遊戲裡打了 /wcy link（當成一般玩家 Steve 送的）
//
// 協作翻譯頁的語料索引：正式站從 translate-index 分支讀，這裡第一次用到時在本機跑
// tools/build-translate-index.py 產生（要有 python）。Alex 是日文與繁中的譯者，已經有一個提案與一個術語投票。
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import os from 'node:os';
import worker, { issueDiscordToken, issueToken, issueWebToken } from './worker.js';
import { demoCredits, memoryKv, seedDemo } from './dev-data.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const data = new Map();
const kv = memoryKv(data);
const env = { CORPUS: kv, DISCORD_CLIENT_ID: 'dev', DISCORD_CLIENT_SECRET: 'dev' };

// 協作翻譯的語料索引：攔下 Worker 去 translate-index 分支讀的那一次，改從本機產生
const realFetch = globalThis.fetch;
let indexDir = null;
globalThis.fetch = async (url, init) => {
  const u = String(typeof url === 'string' ? url : url.url);
  if (u.endsWith('/main/src/main/resources/assets/wynnchayuan/credits.json')) {
    return new Response(JSON.stringify(demoCredits(process.env.DEMO_TEAMS)), { status: 200 });
  }
  const m = /\/translate-index\/([a-z_]+\.json)$/.exec(u);
  if (!m) return realFetch(url, init);
  if (!indexDir) {
    indexDir = fs.mkdtempSync(path.join(os.tmpdir(), 'wcy-tindex-'));
    execFileSync(process.env.PYTHON || 'python', [path.join(here, '..', 'build-translate-index.py'), indexDir], { stdio: 'ignore' });
  }
  const file = path.join(indexDir, m[1]);
  return fs.existsSync(file) ? new Response(fs.readFileSync(file), { status: 200 }) : new Response('{}', { status: 404 });
};

const { developer } = await seedDemo(data, { guildIcon: process.argv[3], guildBanner: process.argv[4], demoTeams: process.env.DEMO_TEAMS });

const port = Number(process.argv[2] || 8799);
http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost:' + port);
  if (url.pathname === '/dev-login' && url.searchParams.get('as') === 'discord') {
    data.set('d:123456789012345678', { value: JSON.stringify({ n: 'Dev 的 Discord' }), metadata: null });
    res.writeHead(302, { location: '/account#t=' + await issueDiscordToken(env, '123456789012345678', Date.now()) });
    res.end();
    return;
  }
  if (url.pathname === '/dev-link') {
    const game = await issueToken(env, 'b'.repeat(32), 'Steve', Date.now());
    const answer = await worker.fetch(new Request('http://localhost/api/link', {
      method: 'POST', body: JSON.stringify({ code: url.searchParams.get('code') || '' }),
      headers: { authorization: 'Bearer ' + game },
    }), env);
    res.writeHead(answer.status, { 'content-type': 'application/json; charset=utf-8' });
    res.end(await answer.text());
    return;
  }
  if (url.pathname === '/dev-login') {
    const as = url.searchParams.get('as');
    const [uuid, name] = as === 'user' ? ['b'.repeat(32), 'Steve']
      : as === 'staff' ? ['c'.repeat(32), 'Alex'] : ['a'.repeat(32), developer];
    const token = await issueWebToken(env, uuid, name, Date.now());
    const to = /^\/[a-z]*$/.test(url.searchParams.get('to') || '') ? url.searchParams.get('to') : '/account';
    res.writeHead(302, { location: to + '#t=' + token });
    res.end();
    return;
  }
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const request = new Request(url, {
    method: req.method,
    headers: req.headers,
    body: ['GET', 'HEAD'].includes(req.method) ? undefined : Buffer.concat(chunks),
  });
  const answer = await worker.fetch(request, env);
  res.writeHead(answer.status, Object.fromEntries(answer.headers));
  res.end(Buffer.from(await answer.arrayBuffer()));
}).listen(port, () => console.log('WynnChaYuan 網站：http://localhost:' + port + '/'));
