// 在本機開網站來看：node tools/presence/dev.mjs [埠號]
//
// 用一個放在記憶體裡的假 KV 跑同一支 worker，先塞幾件 capes/templates 的示範披風與一筆封禁。
// 只給改頁面的時候看樣子用——真的登入要連 Mojang，這裡沒有接，改成：
//   /dev-login?as=dev    當成名單上的開發者登入（名字用 credits.json 的第一位開發者）
//   /dev-login?as=user   當成一般玩家登入
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import worker, { issueWebToken } from './worker.js';

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
const env = { CORPUS: kv };

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
data.set('ban:' + '9'.repeat(32), {
  value: JSON.stringify({ n: 'Herobrine', t: Date.now() - 3 * 86400000, by: developer }), metadata: null });

const port = Number(process.argv[2] || 8799);
http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost:' + port);
  if (url.pathname === '/dev-login') {
    const dev = url.searchParams.get('as') !== 'user';
    const token = await issueWebToken(env, (dev ? 'a' : 'b').repeat(32), dev ? developer : 'Steve', Date.now());
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
