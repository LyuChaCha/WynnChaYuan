// 在本機開一覽頁來看：node tools/presence/dev.mjs [埠號]
//
// 用一個放在記憶體裡的假 KV 跑同一支 worker，先塞幾件 capes/templates 的示範披風。
// 只給改頁面的時候看樣子用——登入、上傳那些要連 Mojang，這裡沒有接。
// 加上 ?mod 就當成有管理權（刪除的按鈕會出來，按下去會回 403，因為憑證是假的）。
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import worker from './worker.js';

const here = path.dirname(fileURLToPath(import.meta.url));
const templates = path.join(here, '..', '..', 'capes', 'templates');
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

// 示範資料：每個檔當成一、兩個人上傳的
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
      metadata: { t: type, v: Date.now() - n * 86400000, s: bytes.length, u: 'f'.repeat(32), h: hash },
    });
  }
  n++;
}

const port = Number(process.argv[2] || 8799);
http.createServer(async (req, res) => {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const request = new Request('http://localhost:' + port + req.url, {
    method: req.method,
    headers: req.headers,
    body: ['GET', 'HEAD'].includes(req.method) ? undefined : Buffer.concat(chunks),
  });
  const answer = await worker.fetch(request, { CORPUS: kv });
  const headers = Object.fromEntries(answer.headers);
  res.writeHead(answer.status, headers);
  res.end(Buffer.from(await answer.arrayBuffer()));
}).listen(port, () => console.log('披風一覽：http://localhost:' + port + '/'));
