// node tools/presence/worker.test.mjs
import worker, {
  publicId, issueToken, readToken, needsWrite, applyRecord, inspectCape, gifFrames, groupCapes,
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
      sections: [
        { role: 'dev', key: 'credits.role.developers', members: [{ name: 'A', mc: 'Alice_Dev' }, { name: 'data' }] },
        { role: 'sponsor', key: 'credits.role.sponsors', members: [{ name: 'C', mc: 'Carol' }] }],
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
check('範本的四種尺寸都收', [[22, 17], [44, 34], [88, 68], [176, 136]]
  .every(([w, h]) => inspectCape(png(w, h)).type === 'png'));
check('★ 不是範本的尺寸不收（實際 ' + inspectCape(png(100, 100)).why + '）',
  inspectCape(png(100, 100)).why === 'bad size' && inspectCape(png(23, 17)).why === 'bad size'
  && inspectCape(png(66, 51)).why === 'bad size' && inspectCape(png(352, 272)).why === 'bad size');
check('★ 只畫一面的、或沒裁掉空白的完整版型都不收——要照範本', inspectCape(png(40, 64)).why === 'bad size'
  && inspectCape(png(64, 32)).why === 'bad size' && inspectCape(png(128, 64)).why === 'bad size'
  && inspectCape(gif(20, 32, 3)).why === 'bad size');
check('GIF 讀得出尺寸與格數', JSON.stringify(inspectCape(gif(44, 34, 5)))
  === JSON.stringify({ type: 'gif', width: 44, height: 34, frames: 5 }));
check('★ 格數太多不收', inspectCape(gif(22, 17, MAX_CAPE_FRAMES + 1)).why === 'too many frames');
check('★ 下載到一半的 GIF 不收', gifFrames(gif(22, 17, 3, false)) === 0
  && inspectCape(gif(22, 17, 3, false)).why === 'bad format');
check('上限是 20 MB', MAX_CAPE_BYTES === 20 * 1024 * 1024);
check('★ 太大的檔不收', inspectCape(new Uint8Array(MAX_CAPE_BYTES + 1)).why === 'too large');
check('不是圖的不收', inspectCape(new TextEncoder().encode('<html>hello</html>')).why === 'bad format');

// ---- 上傳：PNG 大家都能傳，GIF 只有名單上的成員 ----
r = await worker.fetch(put('/cape', gif(44, 34, 4)), env);
check('★ 沒有通行證不能上傳', r.status === 401);
r = await worker.fetch(put('/cape', gif(44, 34, 4), bobToken), env);
check('★ 不在名單上的人不能傳 GIF（預設）', r.status === 403
  && (await r.json()).why === 'gif members only');
r = await worker.fetch(put('/cape', png(33, 33), aliceToken), env);
check('尺寸不對回 400', r.status === 400 && (await r.json()).why === 'bad size');
r = await worker.fetch(put('/cape', gif(44, 34, 4), aliceToken), env);
body = await r.json();
check('名單上的人傳 GIF 成功', r.status === 200 && body.type === 'gif' && body.frames === 4);
r = await worker.fetch(get('/cape/' + aliceId), env);
const back = new Uint8Array(await r.arrayBuffer());
check('★ 下載回來一個位元組都沒變', r.status === 200 && back.length === gif(44, 34, 4).length
  && back.every((b, i) => b === gif(44, 34, 4)[i]) && r.headers.get('content-type') === 'image/gif');
list = await (await worker.fetch(get('/list'), env)).json();
check('★ 名單馬上看得到她的披風與版本', list.capes[aliceId] === body.v);
r = await worker.fetch(put('/cape', png(22, 17), aliceToken), env);
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

r = await worker.fetch(put('/cape', png(22, 17), bobToken), env);
check('★ 不在名單上的人可以傳 PNG（預設）', r.status === 200 && (await r.json()).type === 'png');
r = await worker.fetch(put('/cape', png(22, 17), bobToken), { CORPUS: env.CORPUS, UPLOADS: 'listed' });
check('UPLOADS=listed：不在名單上的人連 PNG 也不能傳', r.status === 403
  && (await r.json()).why === 'not listed');
r = await worker.fetch(put('/cape', png(22, 17), aliceToken), { CORPUS: env.CORPUS, UPLOADS: 'off' });
check('UPLOADS=off：誰都不能傳', r.status === 403 && (await r.json()).why === 'uploads off');

// ---- ★ 相同的圖只存一份 ----
const blobs = () => [...env.CORPUS.data.keys()].filter((k) => k.startsWith('blob:'));
check('現在兩個人、兩張不同的圖', blobs().length === 2);
// 把她那一筆的時間往前推，她才能馬上再傳
const pushBack = (id) => { env.CORPUS.data.get('cape:' + id).metadata.v -= 60000; };
pushBack(aliceId);
r = await worker.fetch(put('/cape', png(22, 17), aliceToken), env);
check('她改傳跟他一樣的圖', r.status === 200);
check('★ 兩個人同一張圖：只存一份，她原本那張沒人用了就刪掉（實際 ' + blobs().length + ' 份）',
  blobs().length === 1);
r = await worker.fetch(get('/capes'), env);
const capes = (await r.json()).capes;
check('★ 一覽把相同的圖併成一筆，列出兩個人', capes.length === 1 && capes[0].ids.length === 2
  && capes[0].ids.includes(aliceId) && capes[0].ids.includes(bobId) && capes[0].t === 'png');
check('★ 一覽裡沒有 UUID 也沒有名字', !JSON.stringify(capes).includes(ALICE.id)
  && !JSON.stringify(capes).includes(BOB.name));
check('兩個人都下載得到', (await worker.fetch(get('/cape/' + aliceId), env)).status === 200
  && (await worker.fetch(get('/cape/' + bobId), env)).status === 200);
check('groupCapes：新的排前面', groupCapes([
  { id: 'a', h: 'x', t: 'png', s: 1, v: 5 }, { id: 'b', h: 'y', t: 'gif', s: 2, v: 9 },
  { id: 'c', h: 'x', t: 'png', s: 1, v: 7 }]).map((g) => g.h + g.ids.length).join() === 'y1,x2');

// ---- 披風一覽頁 ----
r = await worker.fetch(get('/gallery'), env);
const page = await r.text();
check('一覽是一張網頁', r.status === 200 && r.headers.get('content-type').startsWith('text/html')
  && page.includes('披風一覽') && page.includes('/capes'));
check('首頁就是一覽', (await (await worker.fetch(get('/'), env)).text()) === page);
check('★ 一覽頁只准跑自己的程式與 jsDelivr 的立體檢視',
  (r.headers.get('content-security-policy') || '').includes("script-src 'unsafe-inline' https://cdn.jsdelivr.net"));
// 頁面是包在樣板字串裡送出去的：裡面不能有反引號或 \${
check('頁面裡沒有會被樣板字串吃掉的寫法', !page.includes(String.fromCharCode(96)) && !page.includes('$' + '{'));
check('★ 一覽上寫著不可以上傳什麼', ['政治', '不雅', '違反法規', '種族歧視'].every((w) => page.includes(w)));

// ---- ★ 開發者可以刪 ----
r = await worker.fetch(post('/admin/remove', { hash: capes[0].h }), env);
check('★ 沒有憑證不能刪', r.status === 403);
r = await worker.fetch(post('/admin/remove', { hash: capes[0].h }, bobToken), env);
check('★ 拿一般的通行證不能刪', r.status === 403);
r = await worker.fetch(post('/mod-link', {}, bobToken), env);
check('★ 不是開發者拿不到管理網址', r.status === 403);
r = await worker.fetch(post('/mod-link', {}, aliceToken), env);
const link = (await r.json()).url;
check('開發者拿得到管理網址，憑證在 # 後面', r.status === 200 && link.includes('/gallery#k=mod.'));
const modKey = link.split('#k=')[1];
r = await worker.fetch(post('/admin/remove', { hash: 'f'.repeat(64) }, modKey), env);
check('沒有這張圖：成功但什麼都沒刪', r.status === 200 && (await r.json()).removed === 0);
r = await worker.fetch(post('/admin/remove', { hash: capes[0].h }, modKey.replace(ALICE.id, BOB.id)), env);
check('★ 竄改過的管理憑證不認', r.status === 403);
r = await worker.fetch(post('/admin/remove', { id: bobId, ban: true }, modKey), env);
check('★ 開發者刪掉他的披風並禁止再傳', r.status === 200 && (await r.json()).removed === 1
  && !env.CORPUS.data.has('cape:' + bobId) && env.CORPUS.data.has('ban:' + BOB.id));
check('她還在用同一張圖，圖檔留著', blobs().length === 1);
list = await (await worker.fetch(get('/list'), env)).json();
check('名單上他的披風也沒了，她的還在', !(bobId in list.capes) && aliceId in list.capes);
r = await worker.fetch(put('/cape', png(22, 17), bobToken), env);
check('★ 被禁止的人不能再傳', r.status === 403 && (await r.json()).why === 'banned');
r = await worker.fetch(post('/admin/remove', { hash: capes[0].h }, modKey), env);
check('★ 照雜湊刪：所有用這張圖的人一起拿掉，圖檔也刪了', (await r.json()).removed === 1
  && blobs().length === 0 && !env.CORPUS.data.has('cape:' + aliceId));

// ADMIN_TOKEN 是另一條路（不在遊戲裡的時候用）
const admin = { CORPUS: env.CORPUS, ADMIN_TOKEN: 'sesame' };
r = await worker.fetch(post('/admin/remove', { id: bobId, ban: false }, 'wrong'), admin);
check('密語不對不行', r.status === 403);
env.CORPUS.data.set('cape:' + bobId, { value: '', metadata: { t: 'png', v: 1, s: 1, u: BOB.id, h: 'e'.repeat(64) } });
r = await worker.fetch(post('/admin/remove', { id: bobId, ban: false }, 'sesame'), admin);
check('ADMIN_TOKEN 也能刪，ban:false 是解除禁止', r.status === 200 && !env.CORPUS.data.has('ban:' + BOB.id));

// ---- 自己拿掉 ----
await worker.fetch(put('/cape', gif(22, 17, 2), aliceToken), env);
r = await worker.fetch(del('/cape', aliceToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('自己拿掉披風：那一筆、圖檔、名單都沒了', r.status === 200
  && !env.CORPUS.data.has('cape:' + aliceId) && !(aliceId in list.capes) && blobs().length === 0);

r = await worker.fetch(get('/status'), env);
check('/status 回人數與上傳模式', JSON.stringify(await r.json()).includes('"uploads":"tiered"'));
check('舊的語料路由不在了', (await worker.fetch(get('/submit'), env)).status === 404);

console.log(failures === 0 ? '登記站：全部通過' : '登記站：' + failures + ' 項失敗');
process.exit(failures === 0 ? 0 : 1);
