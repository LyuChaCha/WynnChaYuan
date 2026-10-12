// node tools/presence/worker.test.mjs
import worker, {
  publicId, issueToken, readToken, needsWrite, applyRecord, inspectCape, gifFrames,
  MAX_CAPE_BYTES, MAX_CAPE_FRAMES,
} from './worker.js';

let failures = 0;
function check(what, ok) {
  console.log((ok ? '  ok   ' : '  FAIL ') + what);
  if (!ok) failures++;
}

/** 假的 KV。 */
function fakeKv() {
  const data = new Map();
  const ops = { get: 0, put: 0, list: 0 };
  return {
    ops,
    data,
    async get(key) {
      ops.get++;
      return data.has(key) ? data.get(key).value : null;
    },
    async getWithMetadata(key) {
      ops.get++;
      return data.has(key)
        ? { value: data.get(key).value, metadata: data.get(key).metadata }
        : { value: null, metadata: null };
    },
    async put(key, value, options = {}) {
      ops.put++;
      data.set(key, { value, metadata: options.metadata || null, ttl: options.expirationTtl });
    },
    async delete(key) {
      data.delete(key);
    },
    async list({ prefix }) {
      ops.list++;
      const keys = [...data.keys()].filter((k) => k.startsWith(prefix)).sort()
        .map((name) => ({ name, metadata: data.get(name).metadata }));
      return { keys, list_complete: true };
    },
  };
}

// ---- 假的 Mojang 與 GitHub ----
const ALICE = { id: 'a'.repeat(32), name: 'Alice_Dev' };          // 名單上的人
const BOB = { id: 'b'.repeat(32), name: 'Bob' };                  // 不在名單上
const joined = new Map();                                         // serverId → 帳號
let mojangDown = false;
globalThis.fetch = async (url) => {
  const u = new URL(typeof url === 'string' ? url : url.url);
  if (u.hostname === 'sessionserver.mojang.com') {
    if (mojangDown) throw new Error('down');
    const profile = joined.get(u.searchParams.get('serverId'));
    return profile && profile.name === u.searchParams.get('username')
      ? new Response(JSON.stringify(profile), { status: 200 })
      : new Response(null, { status: 204 });
  }
  if (u.hostname === 'raw.githubusercontent.com') {
    return new Response(JSON.stringify({
      sections: [{ role: 'dev', members: [{ name: 'A', mc: 'Alice_Dev' }, { name: 'data' }] }],
    }), { status: 200 });
  }
  throw new Error('unexpected fetch ' + u);
};

const post = (path, body, token) => new Request('https://x.test' + path, {
  method: 'POST',
  body: typeof body === 'string' ? body : JSON.stringify(body),
  headers: token ? { authorization: 'Bearer ' + token } : {},
});
const get = (path) => new Request('https://x.test' + path);
const put = (path, bytes, token) => new Request('https://x.test' + path, {
  method: 'PUT', body: bytes, headers: token ? { authorization: 'Bearer ' + token } : {},
});
const del = (path, token) => new Request('https://x.test' + path, {
  method: 'DELETE', headers: token ? { authorization: 'Bearer ' + token } : {},
});

// ---- 測試用的圖 ----
function png(width, height) {
  const bytes = new Uint8Array(64);
  bytes.set([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52]);
  new DataView(bytes.buffer).setUint32(16, width);
  new DataView(bytes.buffer).setUint32(20, height);
  return bytes;
}
function gif(width, height, frames, finished = true) {
  const out = [0x47, 0x49, 0x46, 0x38, 0x39, 0x61, width & 255, width >> 8, height & 255, height >> 8,
    0x80, 0, 0, 0, 0, 0, 255, 255, 255];                              // 兩色的調色盤
  for (let i = 0; i < frames; i++) {
    out.push(0x21, 0xf9, 4, 0, 10, 0, 0, 0);                         // 停留時間
    out.push(0x2c, 0, 0, 0, 0, width & 255, width >> 8, height & 255, height >> 8, 0);
    out.push(2, 2, 0x4c, 0x01, 0);                                   // LZW 資料
  }
  if (finished) out.push(0x3b);
  return new Uint8Array(out);
}

const env = { CORPUS: fakeKv() };
const NOW = Date.now();

// ---- 代碼與通行證 ----
const aliceId = await publicId(ALICE.id);
check('代碼是 24 個十六進位字元，而且不是 UUID 的一部分',
  /^[0-9a-f]{24}$/.test(aliceId) && !ALICE.id.includes(aliceId.slice(0, 8)));
// 模組那邊（PresenceTest）釘的是同一個值：兩邊算的不一樣就互相看不到
check('★ 演算法跟模組一樣', aliceId === '3dd509c0fcb112e29c04ee34');
check('同一個 UUID 永遠同一個代碼，不同的人不同', aliceId === await publicId(ALICE.id)
  && aliceId !== await publicId(BOB.id));
const token = await issueToken(env, ALICE.id, ALICE.name, NOW);
const read = await readToken(env, token, NOW);
check('通行證讀得回 UUID 與名字', read && read.uuid === ALICE.id && read.name === ALICE.name);
check('★ 過期的通行證不認', await readToken(env, token, NOW + 8 * 24 * 3600 * 1000) === null);
const forged = token.replace(ALICE.id, BOB.id);
check('★ 把 UUID 換成別人的：簽章對不上，不認', await readToken(env, forged, NOW) === null);
check('亂寫的通行證不認', await readToken(env, 'x.y.z', NOW) === null
  && await readToken(env, null, NOW) === null);

// ---- 登入：要 Mojang 說是 ----
const sid = 'c'.repeat(40);
let r = await worker.fetch(post('/login', { name: ALICE.name, serverId: sid }), env);
check('★ 沒有向 Mojang 加入過：不發通行證', r.status === 403);
joined.set(sid, ALICE);
r = await worker.fetch(post('/login', { name: ALICE.name, serverId: sid }), env);
let body = await r.json();
check('Mojang 說是：發通行證，回公開代碼', r.status === 200 && body.id === aliceId && body.token);
const aliceToken = body.token;
r = await worker.fetch(post('/login', { name: BOB.name, serverId: sid }), env);
check('★ 拿別人的 serverId 冒名：不發', r.status === 403);
r = await worker.fetch(post('/login', { name: 'bad name!', serverId: sid }), env);
check('怪名字、怪 serverId 回 400', r.status === 400
  && (await worker.fetch(post('/login', { name: 'Bob', serverId: 'nope' }), env)).status === 400);
mojangDown = true;
r = await worker.fetch(post('/login', { name: ALICE.name, serverId: sid }), env);
check('Mojang 連不上回 502，不是當成通過', r.status === 502);
mojangDown = false;
const bobSid = 'd'.repeat(40);
joined.set(bobSid, BOB);
const bobToken = (await (await worker.fetch(
  post('/login', { name: BOB.name, serverId: bobSid }), env)).json()).token;
const bobId = await publicId(BOB.id);

// ---- 報到與名單 ----
r = await worker.fetch(post('/hello', { cape: true }), env);
check('★ 沒有通行證不能報到', r.status === 401);
r = await worker.fetch(post('/hello', { cape: true }, aliceToken), env);
check('報到成功', r.status === 200);
check('★ 鍵是 UUID，值是空的', env.CORPUS.data.has('u:' + ALICE.id)
  && env.CORPUS.data.get('u:' + ALICE.id).value === '');
check('十四天過期', env.CORPUS.data.get('u:' + ALICE.id).ttl === 14 * 24 * 3600);
const puts = env.CORPUS.ops.put;
await worker.fetch(post('/hello', { cape: true }, aliceToken), env);
check('同一天再報到：不寫 KV', env.CORPUS.ops.put === puts);
r = await worker.fetch(get('/list'), env);
let list = await r.json();
check('名單上有她；沒上傳披風所以 capes 是空的', list.users.includes(aliceId)
  && Object.keys(list.capes).length === 0 && list.hidden.length === 0);
check('★ 名單裡沒有 UUID 也沒有名字', !JSON.stringify(list).includes(ALICE.id)
  && !JSON.stringify(list).includes(ALICE.name));
const lists = env.CORPUS.ops.list;
await worker.fetch(post('/hello', { cape: true }, bobToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('★ 新來的人馬上在名單上，而且沒有重算', list.users.includes(bobId)
  && env.CORPUS.ops.list === lists);
await worker.fetch(post('/hello', { cape: false }, bobToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('★ 關掉披風的人列在 hidden', list.hidden.includes(bobId) && list.users.includes(bobId));
await worker.fetch(post('/hello', { cape: true }, bobToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('再打開就不在 hidden 了', !list.hidden.includes(bobId));

check('沒登記過要寫', needsWrite(null, { c: 1, d: 5, v: 0 }));
check('設定、日期、披風都一樣不必寫', !needsWrite({ c: 1, d: 5, v: 9 }, { c: 1, d: 5, v: 9 }));
check('披風換了要寫', needsWrite({ c: 1, d: 5, v: 9 }, { c: 1, d: 5, v: 10 }));
check('沒變的現況不改名單', applyRecord(
  { t: 1, users: ['x'], capes: { x: 3 }, hidden: [] }, { i: 'x', c: 1, v: 3 }) === null);

// ---- 圖檔的規則 ----
check('一面的四種尺寸都收', [[20, 32], [40, 64], [80, 128], [160, 256]]
  .every(([w, h]) => inspectCape(png(w, h)).type === 'png'));
check('完整版型的三種尺寸都收', [[64, 32], [128, 64], [256, 128]]
  .every(([w, h]) => inspectCape(png(w, h)).type === 'png'));
check('★ 別的尺寸不收（實際 ' + inspectCape(png(100, 100)).why + '）',
  inspectCape(png(100, 100)).why === 'bad size' && inspectCape(png(41, 64)).why === 'bad size');
check('GIF 讀得出尺寸與格數', JSON.stringify(inspectCape(gif(40, 64, 5)))
  === JSON.stringify({ type: 'gif', width: 40, height: 64, frames: 5 }));
check('完整版型的 GIF 也收（正面、背面、邊框自己畫）', inspectCape(gif(128, 64, 3)).frames === 3);
check('★ 格數太多不收', inspectCape(gif(40, 64, MAX_CAPE_FRAMES + 1)).why === 'too many frames');
check('★ 下載到一半的 GIF 不收', gifFrames(gif(40, 64, 3, false)) === 0
  && inspectCape(gif(40, 64, 3, false)).why === 'bad format');
check('★ 太大的檔不收', inspectCape(new Uint8Array(MAX_CAPE_BYTES + 1)).why === 'too large');
check('不是圖的不收', inspectCape(new TextEncoder().encode('<html>hello</html>')).why === 'bad format');

// ---- 上傳 ----
r = await worker.fetch(put('/cape', gif(40, 64, 4)), env);
check('★ 沒有通行證不能上傳', r.status === 401);
r = await worker.fetch(put('/cape', gif(40, 64, 4), bobToken), env);
check('★ 不在名單上的人不能上傳（預設）', r.status === 403 && (await r.json()).why === 'not listed');
r = await worker.fetch(put('/cape', png(33, 33), aliceToken), env);
check('尺寸不對回 400', r.status === 400 && (await r.json()).why === 'bad size');
r = await worker.fetch(put('/cape', gif(40, 64, 4), aliceToken), env);
body = await r.json();
check('名單上的人上傳成功', r.status === 200 && body.type === 'gif' && body.frames === 4);
r = await worker.fetch(get('/cape/' + aliceId), env);
const back = new Uint8Array(await r.arrayBuffer());
check('★ 下載回來一個位元組都沒變', r.status === 200 && back.length === gif(40, 64, 4).length
  && back.every((b, i) => b === gif(40, 64, 4)[i]) && r.headers.get('content-type') === 'image/gif');
list = await (await worker.fetch(get('/list'), env)).json();
check('★ 名單馬上看得到她的披風與版本', list.capes[aliceId] === body.v);
r = await worker.fetch(put('/cape', png(40, 64), aliceToken), env);
check('連續上傳要等一下', r.status === 429);
await worker.fetch(post('/hello', { cape: false }, aliceToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('★ 她關掉披風：名單上沒有她的披風，圖檔還在', !(aliceId in list.capes)
  && list.hidden.includes(aliceId) && env.CORPUS.data.has('cape:' + aliceId));
await worker.fetch(post('/hello', { cape: true }, aliceToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('再打開：披風回來了', list.capes[aliceId] === body.v);
check('沒有的披風回 404', (await worker.fetch(get('/cape/' + bobId), env)).status === 404
  && (await worker.fetch(get('/cape/../x'), env)).status === 404);

// 開放給所有人
const open = { CORPUS: env.CORPUS, UPLOADS: 'anyone' };
r = await worker.fetch(put('/cape', png(20, 32), bobToken), open);
check('UPLOADS=anyone：不在名單上的人也能傳', r.status === 200);
r = await worker.fetch(put('/cape', png(20, 32), aliceToken), { CORPUS: env.CORPUS, UPLOADS: 'off' });
check('UPLOADS=off：誰都不能傳', r.status === 403 && (await r.json()).why === 'uploads off');

// ---- 管理 ----
r = await worker.fetch(post('/admin/remove', { id: bobId, ban: true }), open);
check('★ 沒設 ADMIN_TOKEN：管理路由不開', r.status === 403);
const admin = { CORPUS: env.CORPUS, UPLOADS: 'anyone', ADMIN_TOKEN: 'sesame' };
r = await worker.fetch(post('/admin/remove', { id: bobId, ban: true }, 'wrong'), admin);
check('密語不對不行', r.status === 403);
r = await worker.fetch(post('/admin/remove', { id: bobId, ban: true }, 'sesame'), admin);
check('拿掉他的披風', r.status === 200 && !env.CORPUS.data.has('cape:' + bobId));
list = await (await worker.fetch(get('/list'), admin)).json();
check('名單上也沒有了', !(bobId in list.capes));
env.CORPUS.data.delete('cape:' + bobId);
r = await worker.fetch(put('/cape', png(20, 32), bobToken), admin);
check('★ 被禁止的人不能再傳', r.status === 403 && (await r.json()).why === 'banned');

// ---- 自己拿掉 ----
r = await worker.fetch(del('/cape', aliceToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('自己拿掉披風：圖檔與名單都沒了', r.status === 200
  && !env.CORPUS.data.has('cape:' + aliceId) && !(aliceId in list.capes));

r = await worker.fetch(get('/'), env);
check('首頁回人數', (await r.json()).users === 2);
check('舊的語料路由不在了', (await worker.fetch(get('/submit'), env)).status === 404);

console.log(failures === 0 ? '登記站：全部通過' : '登記站：' + failures + ' 項失敗');
process.exit(failures === 0 ? 0 : 1);
