// 在本機開網站來看：node tools/presence/dev.mjs [埠號] [公會圖示.png] [公會旗幟.png]
//
// 用一個放在記憶體裡的假 KV 跑同一支 worker，先塞幾件 capes/templates 的示範披風與一筆封禁。
// 只給改頁面的時候看樣子用——真的登入要連 Mojang，這裡沒有接，改成：
//   /dev-login?as=dev    當成名單上的開發者登入（名字用 credits.json 的第一位開發者）
//   /dev-login?as=user   當成一般玩家登入
//   /dev-login?as=staff  當成管理員登入（不在名單上，但管理頁開得起來）
//   /dev-login?as=discord  當成剛用 Discord 註冊、還沒綁遊戲 ID 的人登入
//   /dev-link?code=XXXXXXXX  假裝遊戲裡打了 /wcy link（當成一般玩家 Steve 送的）
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import worker, { issueDiscordToken, issueToken, issueWebToken } from './worker.js';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(here, '..', '..');
const templates = path.join(root, 'capes', 'templates');
const data = new Map();
const kv = {
  async get(key) { return data.has(key) ? data.get(key).value : null; },
  async getWithMetadata(key) {
    return data.has(key) ? { ...data.get(key) } : { value: null, metadata: null };
  },
  async put(key, value, options = {}) { data.set(key, { value, metadata: options.metadata || null }); },
  async delete(key) { data.delete(key); },
  async list({ prefix }) {
    const keys = [...data.keys()].filter((k) => k.startsWith(prefix))
      .map((name) => ({ name, metadata: data.get(name).metadata }));
    return { keys, list_complete: true };
  },
};
const env = { CORPUS: kv, DISCORD_CLIENT_ID: 'dev', DISCORD_CLIENT_SECRET: 'dev' };

const credits = JSON.parse(fs.readFileSync(
  path.join(root, 'src/main/resources/assets/wynnchayuan/credits.json'), 'utf8'));
const developer = credits.sections.find((s) => s.key.endsWith('.developers')).members[0].mc;
const people = [developer, 'Steve', 'Alex', 'Notch'];

// 示範資料：每個檔當成一到三個人上傳的
let n = 0;
for (const file of fs.readdirSync(templates).filter((f) => /^(example|template)-.*\.(gif|png)$/.test(f))) {
  const bytes = new Uint8Array(fs.readFileSync(path.join(templates, file)));
  const hash = crypto.createHash('sha256').update(bytes).digest('hex');
  const type = file.endsWith('.gif') ? 'gif' : 'png';
  data.set('blob:' + hash, { value: bytes, metadata: { t: type, s: bytes.length } });
  for (let i = 0; i <= n % 3; i++) {
    const id = crypto.createHash('sha256').update(file + i).digest('hex').slice(0, 24);
    data.set('cape:' + id, {
      value: '',
      metadata: { t: type, v: Date.now() - n * 86400000, s: bytes.length, u: String(i).repeat(32),
        n: people[(n + i) % people.length], h: hash },
    });
  }
  n++;
}
// 披風一覽的幾種狀態：幾件有人加了最愛、一件不公開、一件在等審核，審核名單上一個人
{
  const seeded = [...data.keys()].filter((k) => k.startsWith('cape:')).map((k) => data.get(k).metadata);
  const stars = {};
  seeded.forEach((cape, i) => {
    cape.c = cape.v - (i % 5) * 3600000;
    if (i % 3 === 0) stars[cape.h] = 1 + (i % 4);
  });
  const alone = seeded.filter((cape) => seeded.filter((other) => other.h === cape.h).length === 1);
  if (alone[0]) alone[0].q = 1;
  if (alone[1]) alone[1].r = 1;
  data.set('stars', { value: JSON.stringify(stars), metadata: null });
  // 沒有人穿的披風留七天：兩件已經沒有人穿，一件還剩四天、一件是最後一天
  const idle = {};
  [[alone[2], 3], [alone[3], 6.5]].forEach(([cape, days]) => {
    if (!cape) return;
    const key = [...data.keys()].find((k) => k.startsWith('cape:') && data.get(k).metadata === cape);
    data.delete(key);
    idle[cape.h] = { t: cape.t, s: cape.s, c: cape.c, at: Date.now() - days * 86400000 };
  });
  data.set('idle', { value: JSON.stringify(idle), metadata: null });
  data.set('review:' + '0'.repeat(32), {
    value: JSON.stringify({ n: people[1], note: '示範', t: Date.now() - 5400000, by: developer, ok: [] }), metadata: null });
}
data.set('ban:' + '9'.repeat(32), {
  value: JSON.stringify({ n: 'Herobrine', t: Date.now() - 3 * 86400000, by: developer }), metadata: null });

// 一位管理員（照 UUID 加的）與一位還沒登入過的（照名字加的）
data.set('staff:u:' + 'c'.repeat(32), {
  value: JSON.stringify({ n: '幫忙看披風', t: Date.now() - 86400000, by: developer, m: 'Alex' }), metadata: null });
data.set('staff:n:notch', { value: JSON.stringify({ n: '', t: Date.now() - 3600000, by: developer }), metadata: null });

// 一個貢獻公會。圖示與旗幟不在倉庫裡（是公會自己的圖）：指令列有給檔案就用，沒給就只有名字
const guild = { slug: 'yuchayuan', name: 'YuChaYuan', tag: 'YCY', t: Date.now() - 7200000, by: developer, i: 0, b: 0 };
[['icon', 'i', process.argv[3]], ['banner', 'b', process.argv[4]]].forEach(([kind, field, file]) => {
  if (!file || !fs.existsSync(file)) return;
  data.set('gimg:yuchayuan:' + kind, { value: new Uint8Array(fs.readFileSync(file)), metadata: { t: 'image/png' } });
  guild[field] = Date.now();
});
data.set('guilds', { value: JSON.stringify([guild]), metadata: null });

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
    res.writeHead(302, { location: '/account#t=' + token });
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
