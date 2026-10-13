// node tools/presence/worker.test.mjs
import worker, {
  publicId, issueToken, issueWebToken, readToken, needsWrite, applyRecord, badgePrefs, plainUuid, inspectCape, gifFrames, groupCapes,
  addMember, certificatePayload, issueChallenge, issueDiscordToken, LOGIN_PREFIX,
  MAX_CAPE_BYTES, MAX_CAPE_FRAMES,
} from './worker.js';
import { CERT_KEYS, PROFILE_KEYS } from './mojang-keys.js';

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
let githubFile = '';
const discordCalls = [];
let discordUser = { id: '111111111111111111', username: 'alice_dc', global_name: 'Alice 的 Discord' };
const githubCalls = [];
globalThis.fetch = async (url, init = {}) => {
  const u = new URL(typeof url === 'string' ? url : url.url);
  if (u.hostname === 'api.github.com') {
    const path = u.pathname.replace('/repos/LyuChaCha/WynnChaYuan', '') + u.search;
    const call = { method: init.method || 'GET', path, auth: (init.headers || {}).authorization,
      body: init.body ? JSON.parse(init.body) : null };
    githubCalls.push(call);
    const answer = (body, status = 200) => new Response(JSON.stringify(body), { status });
    if (call.method === 'GET' && path.startsWith('/contents/')) {
      return answer({ content: Buffer.from(githubFile, 'utf8').toString('base64'), sha: 'filesha' });
    }
    if (path === '/git/ref/heads/main') return answer({ object: { sha: 'mainsha' } });
    if (path === '/git/refs') return answer({}, 201);
    if (call.method === 'PUT') return answer({}, 200);
    if (path === '/pulls') return answer({ html_url: 'https://github.com/LyuChaCha/WynnChaYuan/pull/9999' }, 201);
    return answer({}, 404);
  }
  // 沒有 Mojang 這一支：Worker 連不到它（403），登入不可以再去問。問了就在下面丟例外。
  if (u.hostname === 'discord.com') {
    discordCalls.push({ path: u.pathname, body: String(init.body || ''), auth: (init.headers || {}).authorization });
    if (u.pathname === '/api/oauth2/token') {
      const form = new URLSearchParams(String(init.body));
      return form.get('code') === 'goodcode' && form.get('client_secret') === 'shh'
        && form.get('redirect_uri') === 'https://x.test/auth/discord/callback'
        ? new Response(JSON.stringify({ access_token: 'acc-' + discordUser.id }), { status: 200 })
        : new Response('{}', { status: 400 });
    }
    if (u.pathname === '/api/users/@me') {
      return (init.headers || {}).authorization === 'Bearer acc-' + discordUser.id
        ? new Response(JSON.stringify(discordUser), { status: 200 })
        : new Response('{}', { status: 401 });
    }
    return new Response('{}', { status: 404 });
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

/** 網頁的路徑照原樣，其餘的都是 API。 */
const at = (path) => 'https://x.test' + (['/', '/capes', '/account', '/admin'].includes(path) ? path : '/api' + path);
const post = (path, body, token) => new Request(at(path), {
  method: 'POST',
  body: typeof body === 'string' ? body : JSON.stringify(body),
  headers: token ? { authorization: 'Bearer ' + token } : {},
});
const get = (path, token) => new Request(at(path), {
  headers: token ? { authorization: 'Bearer ' + token } : {},
});
const api = (path) => new Request('https://x.test/api' + path);
const put = (path, bytes, token) => new Request(at(path), {
  method: 'PUT', body: bytes, headers: token ? { authorization: 'Bearer ' + token } : {},
});
const del = (path, token) => new Request(at(path), {
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

// ---- 登入：驗 Mojang 簽過的玩家金鑰憑證 ----
//
// 測試裡的「Mojang」是當場產生的兩把金鑰，用 MOJANG_KEYS 蓋過釘住的那一份。
const subtle = globalThis.crypto.subtle;
const b64 = (bytes) => Buffer.from(bytes).toString('base64');
const utf8 = (text) => new TextEncoder().encode(text);
const rsa = (hash) => subtle.generateKey({ name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048,
  publicExponent: new Uint8Array([1, 0, 1]), hash }, true, ['sign', 'verify']);
const spki = async (pair) => new Uint8Array(await subtle.exportKey('spki', pair.publicKey));
const rsaSign = async (pair, data) => new Uint8Array(await subtle.sign('RSASSA-PKCS1-v1_5', pair.privateKey, data));
const mojangCert = await rsa('SHA-1');
const mojangProfile = await rsa('SHA-1');
const notMojang = await rsa('SHA-1');
env.MOJANG_KEYS = JSON.stringify({
  playerCertificateKeys: [{ publicKey: b64(await spki(mojangCert)) }],
  profilePropertyKeys: [b64(await spki(mojangProfile))],
});

/** 一個玩家手上的東西：自己的金鑰，與 Mojang 替那把公鑰簽的憑證。 */
async function player(who, { signer = mojangCert, expiresAt = Date.now() + 86400000 } = {}) {
  const keys = await rsa('SHA-256');
  const der = await spki(keys);
  return { ...who, keys, der, expiresAt,
    keySignature: await rsaSign(signer, certificatePayload(who.id, expiresAt, der)) };
}
/** Mojang 簽的個人檔案（公開資料：誰都抓得到任何人的）。 */
async function texturesOf(who, { signer = mojangProfile, age = 3600000 } = {}) {
  const dashed = who.id.replace(/^(.{8})(.{4})(.{4})(.{4})(.{12})$/, '$1-$2-$3-$4-$5');
  const value = b64(utf8(JSON.stringify({ timestamp: Date.now() - age,
    profileId: who.dashes ? dashed : who.id, profileName: who.name, textures: {} })));
  return { value, signature: b64(await rsaSign(signer, utf8(value))) };
}
const newChallenge = async () => (await (await worker.fetch(post('/challenge', {}), env)).json()).challenge;
/** 模組登入時送的那一包。over 可以換掉任何一樣，用來試冒名。 */
async function loginBody(p, over = {}) {
  const challenge = over.challenge || await newChallenge();
  const uuid = over.uuid || p.id;
  const signature = await rsaSign(over.signWith || p.keys,
    utf8((over.prefix ?? LOGIN_PREFIX) + (over.signedChallenge || challenge) + '\n' + (over.signedUuid || uuid)));
  return { uuid, expiresAt: over.expiresAt || p.expiresAt, publicKey: b64(over.der || p.der),
    keySignature: b64(over.keySignature || p.keySignature), challenge, signature: b64(signature),
    textures: over.textures === undefined ? await texturesOf(p) : over.textures };
}
const login = async (p, over) => worker.fetch(post('/login', await loginBody(p, over)), env);

const alice = await player(ALICE);
const bob = await player(BOB);
let r = await worker.fetch(post('/challenge', {}), env);
let body = await r.json();
check('出題：一串「時間.亂數.簽章」，不快取', r.status === 200 && /^\d+\.[0-9a-f]{32}\.[0-9a-f]{64}$/.test(body.challenge)
  && r.headers.get('cache-control') === 'no-store' && body.challenge !== await newChallenge());

r = await login(alice);
body = await r.json();
check('★ 憑證是 Mojang 簽的、題目是本人簽的：發通行證，回公開代碼',
  r.status === 200 && body.id === aliceId && body.token);
const aliceToken = body.token;
const who = await readToken(env, aliceToken, Date.now());
check('★ 通行證上的名字是 Mojang 簽的那一個', who && who.uuid === ALICE.id && who.name === ALICE.name);
r = await login({ ...alice, dashes: true });
check('個人檔案裡的 UUID 帶連字號也認', r.status === 200);

// ★ 冒名的各種走法。別人的憑證與個人檔案都當成拿得到（公鑰本來就不是祕密）。
const why = async (response) => response.status + ' ' + (await response.json()).why;
check('★ 拿 Alice 的憑證、用自己的私鑰簽：不發', await why(await login(alice, { signWith: bob.keys })) === '403 bad signature');
check('★ 拿自己的憑證、UUID 寫 Alice：憑證對不上，不發',
  await why(await login(bob, { uuid: ALICE.id, textures: await texturesOf(ALICE) })) === '403 bad certificate');
const selfMade = await player(ALICE, { signer: notMojang });
check('★ 自己簽一張 Alice 的憑證（不是 Mojang 的公鑰）：不發', await why(await login(selfMade)) === '403 bad certificate');
check('★ 自己的憑證配上 Alice 的個人檔案（想掛她的名字拿管理權）：不發',
  await why(await login(bob, { textures: await texturesOf(ALICE) })) === '403 bad profile');
check('★ 個人檔案寫 Alice 的名字、自己簽（不是 Mojang 的公鑰）：不發',
  await why(await login(bob, { textures: await texturesOf({ id: BOB.id, name: ALICE.name }, { signer: notMojang }) })) === '403 bad profile');
check('個人檔案被改過一個字：簽章對不上，不發', await why(await login(bob, { textures: await (async () => {
  const t = await texturesOf(BOB);
  return { value: b64(utf8(Buffer.from(t.value, 'base64').toString().replace('Bob', 'Bib'))), signature: t.signature };
})() })) === '403 bad profile');
check('個人檔案太舊（兩個月前簽的）：不發', await why(await login(bob, { textures: await texturesOf(BOB, { age: 60 * 86400000 }) })) === '403 bad profile');
check('沒附個人檔案：不發（名字不可以用說的）', await why(await login(bob, { textures: null })) === '403 bad profile');
check('★ 簽的不是這一題（拿舊的簽章來重放）：不發',
  await why(await login(alice, { signedChallenge: await newChallenge() })) === '403 bad signature');
check('★ 簽的字沒有綁我們的網域（別的網站騙來的簽章）：不發',
  await why(await login(alice, { prefix: 'example.com login\n' })) === '403 bad signature');
check('★ 題目不是我們出的：不發', await why(await login(alice, { challenge: Date.now() + '.' + 'a'.repeat(32) + '.' + 'b'.repeat(64) })) === '403 stale challenge');
check('★ 題目過了五分鐘：不發', await why(await login(alice, { challenge: await issueChallenge(env, Date.now() - 6 * 60000) })) === '403 stale challenge');
const expired = await player(BOB, { expiresAt: Date.now() - 1000 });
check('憑證過期了：不發', await why(await login(expired)) === '403 certificate expired');
check('到期時間被改掉（想讓過期的憑證復活）：憑證對不上', await why(await login(expired, { expiresAt: Date.now() + 86400000 })) === '403 bad certificate');
r = await worker.fetch(post('/login', { uuid: 'nope', expiresAt: 1, publicKey: 'AA==', keySignature: 'AA==', signature: 'AA==', challenge: 'x' }), env);
check('怪的 UUID、缺欄位、不是 base64 都回 400', r.status === 400
  && (await worker.fetch(post('/login', { uuid: BOB.id }), env)).status === 400
  && (await worker.fetch(post('/login', { ...(await loginBody(bob)), publicKey: '不是 base64' }), env)).status === 400);
check('舊版模組送的那一種（name 與 serverId）回 400，不是當成通過',
  (await worker.fetch(post('/login', { name: ALICE.name, serverId: 'c'.repeat(40) }), env)).status === 400);
check('太大的一包回 413', (await worker.fetch(post('/login', { ...(await loginBody(bob)), pad: 'x'.repeat(13000) }), env)).status === 413);
// 釘住的那一份是真的 Mojang 公鑰：讀得進來、各兩把以內
const importable = async (list) => (await Promise.all(list.map((k) => subtle.importKey('spki', Buffer.from(k, 'base64'),
  { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-1' }, false, ['verify'])))).length;
check('釘住的 Mojang 公鑰讀得進來（憑證 ' + CERT_KEYS.length + ' 把、個人檔案 ' + PROFILE_KEYS.length + ' 把）',
  CERT_KEYS.length >= 1 && PROFILE_KEYS.length >= 1 && await importable(CERT_KEYS) === CERT_KEYS.length
  && await importable(PROFILE_KEYS) === PROFILE_KEYS.length);
const pinned = { CORPUS: env.CORPUS };
check('★ 沒設 MOJANG_KEYS 時用釘住的那一份：測試的假 Mojang 就不算數了',
  (await worker.fetch(post('/login', await loginBody(alice)), pinned)).status === 403);
check('MOJANG_KEYS 寫壞了：退回釘住的那一份，不是全部放行',
  (await worker.fetch(post('/login', await loginBody(alice)), { CORPUS: env.CORPUS, MOJANG_KEYS: '{oops' })).status === 403);
const bobToken = (await (await login(bob)).json()).token;
const bobId = await publicId(BOB.id);

// ---- ★ 網站帳號：用 Discord 註冊／登入，再從遊戲裡綁定遊戲 ID ----
const page = (path, headers = {}) => worker.fetch(new Request('https://x.test' + path, { headers, redirect: 'manual' }), env);
const dc = (path, token, bodyValue) => worker.fetch(new Request('https://x.test/api' + path, {
  method: bodyValue === undefined ? 'GET' : 'POST', body: bodyValue === undefined ? undefined : JSON.stringify(bodyValue),
  headers: token ? { authorization: 'Bearer ' + token } : {},
}), env);
r = await page('/auth/discord');
check('沒設 Discord 的時候：登入鈕帶回帳號頁，說還沒開放', r.status === 302 && r.headers.get('location') === 'https://x.test/account#e=discord-off');
check('/api/status 說 Discord 沒開', (await (await worker.fetch(api('/status'), env)).json()).discord === false);
env.DISCORD_CLIENT_ID = 'app123';
env.DISCORD_CLIENT_SECRET = 'shh';
check('設好之後 /api/status 說有開', (await (await worker.fetch(api('/status'), env)).json()).discord === true);
r = await page('/auth/discord');
const authorize = new URL(r.headers.get('location'));
const oauthCookie = (r.headers.get('set-cookie') || '').split(';')[0];
check('★ 去 Discord 登入：只要 identify，回來的網址是我們自己的', r.status === 302 && authorize.origin === 'https://discord.com'
  && authorize.searchParams.get('client_id') === 'app123' && authorize.searchParams.get('scope') === 'identify'
  && authorize.searchParams.get('redirect_uri') === 'https://x.test/auth/discord/callback'
  && authorize.searchParams.get('response_type') === 'code');
check('state 用 cookie 綁在這個瀏覽器上（HttpOnly、Secure）', /^wcy_oauth=[0-9a-f]{32}$/.test(oauthCookie)
  && /HttpOnly/i.test(r.headers.get('set-cookie')) && /Secure/i.test(r.headers.get('set-cookie')));
const state = authorize.searchParams.get('state');
const oauthBack = (query, cookie) => page('/auth/discord/callback?' + query, cookie ? { cookie } : {});
r = await oauthBack('code=goodcode&state=' + state, null);
check('★ 回來時沒有那個 cookie（別人把他的登入連結塞給你）：不認', r.headers.get('location') === 'https://x.test/account#e=discord');
r = await oauthBack('code=goodcode&state=' + state, 'wcy_oauth=' + 'f'.repeat(32));
check('★ cookie 對不上 state：不認', r.headers.get('location').endsWith('#e=discord'));
r = await oauthBack('code=goodcode&state=' + state.replace(/\.[0-9a-f]{64}$/, '.' + '0'.repeat(64)), oauthCookie);
check('★ state 的簽章是假的：不認', r.headers.get('location').endsWith('#e=discord'));
r = await oauthBack('code=wrong&state=' + state, oauthCookie);
check('Discord 不認這個 code：不認', r.headers.get('location').endsWith('#e=discord'));
r = await oauthBack('code=goodcode&state=' + state, oauthCookie);
const dcToken = (r.headers.get('location').match(/#t=(dc\.[^&]+)$/) || [])[1];
check('★ 都對：帶著網站帳號的憑證回帳號頁，cookie 清掉', r.status === 302 && r.headers.get('location').startsWith('https://x.test/account#t=dc.')
  && /Max-Age=0/.test(r.headers.get('set-cookie')));
check('換憑證時帶了 client_secret，拿使用者資料時帶了 Discord 給的那一張',
  discordCalls.some((c) => c.path === '/api/oauth2/token' && c.body.includes('client_secret=shh'))
  && discordCalls.some((c) => c.path === '/api/users/@me' && c.auth === 'Bearer acc-111111111111111111'));
r = await dc('/me', dcToken);
body = await r.json();
check('★ 登入了、還沒綁：認得 Discord 名稱，但還不是任何一個玩家', r.status === 200 && body.linked === false
  && body.discord.name === 'Alice 的 Discord' && body.name === undefined && body.developer === undefined);
check('★ 沒綁之前不能報到、不能上傳、不能進管理', (await dc('/hello', dcToken, { cape: true })).status === 401
  && (await worker.fetch(put('/cape', png(22, 17), dcToken), env)).status === 401
  && (await dc('/admin/bans', dcToken)).status === 403);
check('網站帳號的憑證過期就不認', (await dc('/me', await issueDiscordToken(env, '111111111111111111', Date.now() - 31 * 86400000))).status === 401);
check('沒註冊過的 Discord 代號（自己編一張憑證簽不出來；簽得出來也沒有帳號）不認',
  (await dc('/me', await issueDiscordToken(env, '999999999999999999', Date.now()))).status === 401);

check('要綁定碼得先登入網站帳號', (await dc('/link/start', null, {})).status === 401
  && (await dc('/link/start', aliceToken, {})).status === 401);
r = await dc('/link/start', dcToken, {});
body = await r.json();
const linkCode = body.code;
check('綁定碼是 8 個不容易看錯的字，十分鐘內有效', r.status === 200 && /^[A-HJ-NP-Z2-9]{8}$/.test(linkCode) && body.minutes === 10
  && env.CORPUS.data.get('link:' + linkCode).ttl === 600);
check('★ 綁定要遊戲的通行證：沒帶的、只有網站帳號的都不行', (await dc('/link', null, { code: linkCode })).status === 401
  && (await dc('/link', dcToken, { code: linkCode })).status === 401);
check('亂打的碼：格式不對 400、沒有這組 404', (await dc('/link', aliceToken, { code: 'nope' })).status === 400
  && (await dc('/link', aliceToken, { code: 'ZZZZZZZZ' })).status === 404);
r = await dc('/link', aliceToken, { code: linkCode.toLowerCase() + ' ' });
body = await r.json();
check('★ 遊戲裡送對的碼（大小寫、多打的空白都不計較）：綁起來了', r.status === 200 && body.discord === 'Alice 的 Discord');
check('★ 綁定碼只能用一次', (await dc('/link', bobToken, { code: linkCode })).status === 404);
r = await dc('/me', dcToken);
body = await r.json();
check('★ 之後用 Discord 登入就是那個玩家：名字、代碼、開發者身分都對', body.linked === true && body.name === ALICE.name
  && body.id === aliceId && body.developer === true && body.discord.name === 'Alice 的 Discord');
check('★ 用 Discord 登入的開發者進得了管理', (await dc('/admin/bans', dcToken)).status === 200);
check('★ 網站帳號的憑證不能拿來綁別的網站帳號（要從遊戲裡來）',
  (await dc('/link', dcToken, { code: (await (await dc('/link/start', dcToken, {})).json()).code })).status === 403);
r = await dc('/me', aliceToken);
check('從遊戲開的那一條路照舊：沒有 Discord 也是她', (await r.json()).discord === null);

// 名字是照 Mojang 簽的為準：她改名之後再從遊戲登入一次，網站帳號那邊跟著換
const renamed = await player({ id: ALICE.id, name: 'Alice_New' });
await login(renamed);
check('★ 遊戲帳號改名：網站帳號記的名字跟著換，舊名字的管理權沒了',
  JSON.parse(env.CORPUS.data.get('d:111111111111111111').value).m === 'Alice_New'
  && (await (await dc('/me', dcToken)).json()).developer === false);
await login(alice);                                                 // 改回來，後面的測試還要用她

// Bob 也拿同一個 Discord 帳號來綁：一個網站帳號只綁一個遊戲帳號
r = await dc('/link', bobToken, { code: (await (await dc('/link/start', dcToken, {})).json()).code });
check('★ 同一個網站帳號改綁別的遊戲帳號：舊的那一邊解掉', r.status === 200
  && (await (await dc('/me', dcToken)).json()).name === BOB.name
  && !env.CORPUS.data.has('l:' + ALICE.id) && env.CORPUS.data.get('l:' + BOB.id).value === '111111111111111111');
// 另一個 Discord 帳號來綁 Bob：一個遊戲帳號也只綁一個網站帳號
discordUser = { id: '222222222222222222', username: 'second' };
r = await page('/auth/discord');
const second = new URL(r.headers.get('location')).searchParams.get('state');
r = await oauthBack('code=goodcode&state=' + second, (r.headers.get('set-cookie') || '').split(';')[0]);
const dcSecond = r.headers.get('location').match(/#t=(dc\.[^&]+)$/)[1];
check('沒有顯示名稱的 Discord 帳號用使用者名稱', (await (await dc('/me', dcSecond)).json()).discord.name === 'second');
r = await dc('/link', bobToken, { code: (await (await dc('/link/start', dcSecond, {})).json()).code });
check('★ 同一個遊戲帳號改綁別的網站帳號：原本那個網站帳號變回沒綁', r.status === 200
  && (await (await dc('/me', dcToken)).json()).linked === false
  && (await (await dc('/me', dcSecond)).json()).name === BOB.name);
r = await dc('/unlink', dcSecond, {});
check('★ 解除綁定：網站帳號變回沒綁，遊戲那一邊的紀錄也清掉', r.status === 200
  && (await (await dc('/me', dcSecond)).json()).linked === false && !env.CORPUS.data.has('l:' + BOB.id));
check('解除綁定要登入', (await dc('/unlink', null, {})).status === 401);
// 綁回 Alice，後面網頁的測試會用到
await dc('/link', aliceToken, { code: (await (await dc('/link/start', dcToken, {})).json()).code });

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

// ---- ★ 徽章的選擇跟著報到走：彩色模式、要展示哪幾片 ----
check('沒帶徽章的選擇（舊版模組）：不動原本記的', badgePrefs({ cape: true }) === undefined);
check('帶了：彩色與要展示的身分', JSON.stringify(badgePrefs({ rainbow: true, show: ['sponsors', 'developers', 'sponsors'] }))
  === JSON.stringify({ r: 1, s: 'developers,sponsors' }));
check('只帶彩色：身分那一欄不動', JSON.stringify(badgePrefs({ rainbow: false })) === JSON.stringify({ r: 0 }));
check('★ 怪的不收：彩色不是布林、身分不是陣列、太多、怪字', badgePrefs({ rainbow: 'yes' }) === null
  && badgePrefs({ show: 'developers' }) === null && badgePrefs({ show: Array(9).fill('a') }) === null
  && badgePrefs({ show: ['Dev!'] }) === null && badgePrefs({ show: [1] }) === null);
check('彩色換了、展示的身分換了都要寫', needsWrite({ c: 1, d: 5, v: 9 }, { c: 1, d: 5, v: 9, r: 1 })
  && needsWrite({ c: 1, d: 5, v: 9, s: 'developers' }, { c: 1, d: 5, v: 9, s: 'developers,sponsors' })
  && !needsWrite({ c: 1, d: 5, v: 9, r: 1, s: 'developers' }, { c: 1, d: 5, v: 9, r: 1, s: 'developers' }));
{
  const before = { t: 1, users: ['a'], capes: {}, hidden: [] };          // 舊名單沒有那兩欄
  const after = applyRecord(before, { i: 'a', c: 1, v: 0, r: 1, s: 'developers' });
  check('★ 名單記下誰開了彩色、誰挑了哪幾片', after && after.rainbow.join() === 'a' && after.show.a === 'developers');
  check('沒變就不改名單', applyRecord(after, { i: 'a', c: 1, v: 0, r: 1, s: 'developers' }) === null);
  const off = applyRecord(after, { i: 'a', c: 1, v: 0 });
  check('關掉彩色、不再挑身分：兩欄都拿掉', off && off.rainbow.length === 0 && !('a' in off.show));
}
r = await worker.fetch(post('/hello', { cape: true, rainbow: true, show: ['developers'] }, aliceToken), env);
check('★ 報到帶著徽章的選擇', r.status === 200 && env.CORPUS.data.get('u:' + ALICE.id).metadata.r === 1
  && env.CORPUS.data.get('u:' + ALICE.id).metadata.s === 'developers');
list = await (await worker.fetch(get('/list'), env)).json();
check('★ 名單上看得到：她開了彩色、只展示開發者那一片', list.rainbow.includes(aliceId) && list.show[aliceId] === 'developers');
check('名單上還是沒有 UUID 與名字', !JSON.stringify(list).includes(ALICE.id) && !JSON.stringify(list).includes(ALICE.name));
r = await worker.fetch(post('/hello', { cape: true }, aliceToken), env);
check('舊版模組再報到（沒帶那兩欄）：原本的選擇留著', env.CORPUS.data.get('u:' + ALICE.id).metadata.r === 1);
r = await worker.fetch(post('/hello', { cape: true, rainbow: 'yes' }, aliceToken), env);
check('怪的報到回 400', r.status === 400
  && (await worker.fetch(post('/hello', { cape: true, show: 'developers' }, aliceToken), env)).status === 400);
r = await worker.fetch(post('/hello', { cape: true, rainbow: false, show: ['developers', 'sponsors'] }, aliceToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('★ 關掉彩色、多展示一片：名單跟著變', !list.rainbow.includes(aliceId) && list.show[aliceId] === 'developers,sponsors');
env.CORPUS.data.delete('presence:list');
list = await (await worker.fetch(get('/list'), env)).json();
check('整份重算出來也一樣', !list.rainbow.includes(aliceId) && list.show[aliceId] === 'developers,sponsors');

check('沒登記過要寫', needsWrite(null, { c: 1, d: 5, v: 0 }));
check('設定、日期、披風都一樣不必寫', !needsWrite({ c: 1, d: 5, v: 9 }, { c: 1, d: 5, v: 9 }));
check('披風換了要寫', needsWrite({ c: 1, d: 5, v: 9 }, { c: 1, d: 5, v: 10 }));
check('沒變的現況不改名單', applyRecord(
  { t: 1, users: ['x'], capes: { x: 3 }, hidden: [] }, { i: 'x', c: 1, v: 3 }) === null);

// ---- 圖檔的規則 ----
check('★ 範本的七種尺寸都收，最大 1056×816', [[22, 17], [44, 34], [88, 68], [176, 136], [352, 272], [704, 544], [1056, 816]]
  .every(([w, h]) => inspectCape(png(w, h)).type === 'png'));
check('★ 不是範本的尺寸不收（實際 ' + inspectCape(png(100, 100)).why + '）',
  inspectCape(png(100, 100)).why === 'bad size' && inspectCape(png(23, 17)).why === 'bad size'
  && inspectCape(png(66, 51)).why === 'bad size' && inspectCape(png(1408, 1088)).why === 'bad size'
  && inspectCape(png(990, 765)).why === 'bad size');
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
r = await worker.fetch(api('/capes'), env);
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

// ---- 網頁 ----
const { LANGS, PATHS, TEXT, pickLang } = await import('./site.js');
const inLang = (path, headers) => worker.fetch(new Request('https://x.test' + path, { headers }), env);
const pages = {};
const english = {};
for (const path of ['/', '/capes', '/account', '/admin']) {
  english[path] = await (await inLang(path, { cookie: 'lang=en' })).text();
  r = await inLang(path, { cookie: 'lang=zh' });
  pages[path] = await r.text();
  check(path + ' 兩種語言都填滿了，沒有漏掉的 %%鍵%%',
    !/%%[\w.]+%%/.test(pages[path]) && !/%%[\w.]+%%/.test(english[path]));
  check(path + ' 的 lang 屬性跟著語言走', pages[path].includes('<html lang="zh-Hant">')
    && english[path].includes('<html lang="en">'));
  check(path + ' 英文版也有頁尾，也沒有會被樣板字串吃掉的寫法',
    english[path].includes('Designed by LyuChaCha')
    && !english[path].includes(String.fromCharCode(96)) && !english[path].includes('$' + '{'));
  check(path + ' 是一張網頁', r.status === 200 && r.headers.get('content-type').startsWith('text/html')
    && pages[path].includes('<title>'));
  check(path + ' 有頁尾', pages[path].includes('© 2026 WynnChaYuan - All rights reserved.')
    && pages[path].includes('Designed by LyuChaCha'));
  // 頁面是包在樣板字串裡送出去的：裡面不能有反引號或錢字號加大括號
  check(path + ' 裡沒有會被樣板字串吃掉的寫法',
    !pages[path].includes(String.fromCharCode(96)) && !pages[path].includes('$' + '{'));
}
// 圖示：分頁上的、手機主畫面的；瀏覽器自己來要的 /favicon.ico 也有
for (const path of ['/favicon.ico', '/favicon.png', '/icon-192.png', '/apple-touch-icon.png']) {
  const answer = await worker.fetch(new Request('https://x.test' + path), env);
  const bytes = new Uint8Array(await answer.arrayBuffer());
  check(path + ' 是一張 PNG（' + bytes.length + ' 位元組）', answer.status === 200
    && answer.headers.get('content-type') === 'image/png' && bytes.length > 500
    && bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47
    && /max-age=\d+/.test(answer.headers.get('cache-control')));
}
check('★ 每一頁都掛了圖示', Object.values(pages).concat(Object.values(english)).every((html) =>
  html.includes('<link rel="icon" type="image/png" href="/favicon.png">')
  && html.includes('<link rel="apple-touch-icon" href="/apple-touch-icon.png">')));
check('★ 披風一覽上寫著不可以上傳什麼', ['政治', '不雅', '違反法規', '種族歧視'].every((w) => pages['/capes'].includes(w)));
check('★ 英文版的披風一覽也寫著不可以上傳什麼',
  ['political', 'obscene', 'unlawful', 'racist'].every((w) => english['/capes'].includes(w)));
check('英文版的帳號頁用的是遊戲裡英文介面的字',
  ['F6', 'Badge &amp; cape', 'Cape gallery'].every((w) => english['/account'].includes(w)));
check('★ 開關拿掉了：網站不再叫人去確認它開著', LANGS.every((lang) => !/Let others know|讓別人知道|让别人知道/.test(TEXT[lang]['account.s2'])));

// ---- ★ 網站的語言 ----
const WANTED = ['zh', 'zh-cn', 'en', 'ja', 'ko', 'ru', 'es', 'tr'];
check('★ 八種語言都在，選單照這個順序（實際 ' + LANGS.join() + '）', LANGS.join() === WANTED.join()
  && PATHS.join() === '/,/capes,/account,/admin');
const keysOf = (o, at) => Object.entries(o).flatMap(([k, v]) =>
  (v && typeof v === 'object' ? keysOf(v, at + k + '.') : [at + k]));
const valuesOf = (o) => Object.values(o).flatMap((v) => (v && typeof v === 'object' ? valuesOf(v) : [v]));
const wantedKeys = keysOf(TEXT.en, '').sort().join();
const marks = (text) => (String(text).match(/\{\w+\}|<[^>]+>/g) || []).sort().join(' ');
for (const lang of LANGS) {
  check('★ ' + lang + '：鍵跟英文一模一樣（少翻一條就是一塊英文）', keysOf(TEXT[lang], '').sort().join() === wantedKeys);
  check(lang + '：每一條都有字', valuesOf(TEXT[lang]).every((v) => typeof v === 'string' && v.trim().length > 0));
  // 網址、標籤、{n} 這種位置在每個語言都要原樣在：少一個 {n} 畫面上就少一個數字
  const broken = [];
  const walk = (en, own, at) => {
    for (const [k, v] of Object.entries(en)) {
      if (v && typeof v === 'object') walk(v, (own && own[k]) || {}, at + k + '.');
      else if (k !== 'html' && lang !== 'zh' && marks(v) !== marks(own ? own[k] : '')) broken.push(at + k);
    }
  };
  walk(TEXT.en, TEXT[lang], '');
  check('★ ' + lang + '：標籤、連結與 {n} 這類位置都跟英文一樣' + (broken.length ? '（' + broken.slice(0, 4).join('、') + '）' : ''),
    broken.length === 0);
  const home = await (await inLang('/?lang=' + lang, {})).text();
  check(lang + '：首頁填滿了、html 的 lang 是 ' + TEXT[lang].html, !/%%[\w.]+%%/.test(home)
    && home.includes('<html lang="' + TEXT[lang].html + '">')
    && home.includes('<option value="' + lang + '" selected'));
  for (const path of PATHS) {
    const html = await (await inLang(path + '?lang=' + lang, {})).text();
    check(lang + ' 的 ' + path + ' 沒有會被樣板字串吃掉的寫法、也沒有漏掉的鍵',
      !html.includes(String.fromCharCode(96)) && !html.includes('$' + '{') && !/%%[\w.]+%%/.test(html));
  }
}
check('html 上的語言標籤各不相同', new Set(LANGS.map((l) => TEXT[l].html)).size === LANGS.length);
// 看得到的字：拿掉程式與樣式、標籤；語言選單與首頁的語言清單本來就是各寫各的
const visible = (html) => html.replace(/<script[\s\S]*?<\/script>/g, ' ').replace(/<style[\s\S]*?<\/style>/g, ' ')
  .replace(/<span class="chip">[^<]*<\/span>/g, ' ').replace(/<select class="lang"[\s\S]*?<\/select>/g, ' ')
  .replace(/<[^>]+>/g, ' ');
const cjk = /[㐀-鿿＀-￯　-〿]/;
for (const path of PATHS) {
  const left = visible(english[path]).match(cjk);
  check('★ 英文版的 ' + path + ' 看得到的字裡沒有中文' + (left ? '（' + left[0] + '）' : ''), !left);
  const strings = (english[path].match(/var L = (.*);$/m) || [])[1] || '';
  check('英文版的 ' + path + ' 程式用的字也沒有中文', strings.length > 100 && !cjk.test(strings));
  check('中文版的 ' + path + ' 真的是中文', cjk.test(visible(pages[path])));
}
// 每一種語言真的是那一種語言：看得到的字裡有它自己的文字
const SCRIPT = { 'zh-cn': /[一-鿿]/, ja: /[぀-ヿ]/, ko: /[가-힯]/, ru: /[Ѐ-ӿ]/,
  es: /[áéíóúñ¿¡]/i, tr: /[ığşçöüİ]/ };
for (const [lang, own] of Object.entries(SCRIPT)) {
  const account = visible(await (await inLang('/account?lang=' + lang, {})).text());
  check('★ ' + lang + ' 的帳號頁真的是那個語言', own.test(account));
}
check('簡體不是繁體照搬：帳號頁沒有繁體才有的字', !/[帳號檔說這個們]/.test(
  visible(await (await inLang('/account?lang=zh-cn', {})).text())));
check('語言選單是下拉、八種都在、選著的是現在這一種',
  english['/'].includes('<select class="lang" id="lang"') && english['/'].includes('<option value="en" selected')
  && pages['/'].includes('<option value="zh" selected') && WANTED.every((l) => english['/'].includes('<option value="' + l + '"'))
  && ['繁體中文', '简体中文', 'English', '日本語', '한국어', 'Русский', 'Español', 'Türkçe'].every((n) => english['/'].includes('>' + n + '</option>')));
check('pickLang：選過的優先', pickLang('a=1; lang=zh', 'en-US,en;q=0.9') === 'zh'
  && pickLang('lang=en', 'zh-TW') === 'en' && pickLang('lang=zh-cn', 'en') === 'zh-cn' && pickLang('lang=ja', 'en') === 'ja');
check('pickLang：沒選過看瀏覽器，照它排的順序', pickLang('', 'zh-TW,zh;q=0.9,en;q=0.8') === 'zh'
  && pickLang(null, 'en-GB,zh;q=0.5') === 'en' && pickLang('', 'ja,zh-TW;q=0.8,en;q=0.5') === 'ja');
check('★ pickLang：簡體的瀏覽器給簡體，台灣、香港、只寫 zh 的給繁體', pickLang('', 'zh-CN,zh;q=0.9') === 'zh-cn'
  && pickLang('', 'zh-Hans-CN') === 'zh-cn' && pickLang('', 'zh-SG') === 'zh-cn'
  && pickLang('', 'zh-TW') === 'zh' && pickLang('', 'zh-HK') === 'zh' && pickLang('', 'zh-Hant') === 'zh' && pickLang('', 'zh') === 'zh');
check('★ pickLang：日、韓、俄、西、土各給各的', pickLang('', 'ja-JP') === 'ja' && pickLang('', 'ko-KR,ko;q=0.9') === 'ko'
  && pickLang('', 'ru') === 'ru' && pickLang('', 'es-MX,es;q=0.9') === 'es' && pickLang('', 'tr-TR') === 'tr');
check('pickLang：我們沒有的語言、什麼都沒有、亂填的 cookie 都給英文', pickLang('', 'fr-FR,de;q=0.9') === 'en'
  && pickLang(null, null) === 'en' && pickLang('lang=xx', '') === 'en' && pickLang('lang=__proto__', 'fr') === 'en'
  && pickLang('lang=constructor', '') === 'en');
check('pickLang：我們沒有的排前面，給他排得到的第一個我們有的', pickLang('', 'fr,de;q=0.9,ko;q=0.8,en;q=0.5') === 'ko');
r = await inLang('/capes', { 'accept-language': 'zh-TW,zh;q=0.9' });
check('★ 沒選過：繁體的瀏覽器拿到繁體', (await r.text()).includes('<html lang="zh-Hant">')
  && r.headers.get('content-language') === 'zh-Hant');
r = await inLang('/capes', { 'accept-language': 'tr-TR,tr;q=0.9' });
check('★ 沒選過：土耳其文的瀏覽器拿到土耳其文', (await r.text()).includes('<html lang="tr">')
  && r.headers.get('content-language') === 'tr');
r = await inLang('/capes', { 'accept-language': 'fr-FR,fr;q=0.9' });
check('★ 沒選過：我們沒有的語言拿到英文', (await r.text()).includes('<html lang="en">')
  && r.headers.get('content-language') === 'en');
r = await inLang('/capes', {});
check('什麼都沒帶：英文', (await r.text()).includes('<html lang="en">'));
check('回應說了它會隨 cookie 與語言變', /cookie/i.test(r.headers.get('vary')) && /accept-language/i.test(r.headers.get('vary')));
r = await inLang('/account?lang=zh', { cookie: 'lang=en', 'accept-language': 'en' });
check('網址上的 ?lang= 最優先（貼連結給人用）', (await r.text()).includes('<html lang="zh-Hant">'));
r = await inLang('/account?lang=klingon', { cookie: 'lang=en' });
check('?lang= 亂填的不理', (await r.text()).includes('<html lang="en">'));

check('首頁有下載的連結', pages['/'].includes('https://modrinth.com/mod/wynnchayuan')
  && pages['/'].includes('https://www.curseforge.com/minecraft/mc-mods/wynnchayuan'));
check('帳號頁講了怎麼從遊戲裡登入', pages['/account'].includes('F6') && pages['/account'].includes('披風一覽'));
check('★ 網頁只准跑自己的程式與 jsDelivr 的立體檢視',
  (r.headers.get('content-security-policy') || '').includes("script-src 'unsafe-inline' https://cdn.jsdelivr.net"));
r = await worker.fetch(new Request('https://x.test/gallery', { redirect: 'manual' }), env);
check('舊的 /gallery 轉到 /capes', r.status === 302 && r.headers.get('location').endsWith('/capes'));
check('沒有 /api 的舊路徑不在了', (await worker.fetch(new Request('https://x.test/list'), env)).status === 404
  && (await worker.fetch(new Request('https://x.test/submit'), env)).status === 404);

// ---- ★ 網頁的登入：從遊戲裡換一條網址 ----
r = await worker.fetch(post('/web-link', {}), env);
check('★ 沒有通行證換不到登入網址', r.status === 401);
r = await worker.fetch(post('/web-link', {}, bobToken), env);
const bobLink = (await r.json()).url;
check('換得到，通行證在 # 後面（不會送到伺服器）', r.status === 200 && bobLink.includes('/capes#t=web.'));
const bobWeb = bobLink.split('#t=')[1];
r = await worker.fetch(get('/me', bobWeb), env);
let me = await r.json();
check('★ 網頁的通行證認得出是誰；他不是開發者、不是成員', r.status === 200 && me.name === BOB.name
  && me.id === bobId && me.developer === false && me.member === false);
check('沒登入問我是誰：401', (await worker.fetch(get('/me'), env)).status === 401);
check('★ 把網頁通行證的 UUID 換成別人的：不認',
  (await worker.fetch(get('/me', bobWeb.replace(BOB.id, ALICE.id)), env)).status === 401);
check('★ 網頁的通行證一天就過期', await issueWebToken(env, BOB.id, BOB.name, NOW - 25 * 3600 * 1000)
  .then((old) => worker.fetch(get('/me', old), env)).then((x) => x.status === 401));
const aliceWeb = (await (await worker.fetch(post('/web-link', {}, aliceToken), env)).json()).url.split('#t=')[1];
me = await (await worker.fetch(get('/me', aliceWeb), env)).json();
check('她是開發者，/me 看得到她的披風', me.developer === true && me.member === true && me.cape && me.cape.t === 'png');

// ---- ★ 特別名單：開發者輸入 UUID，那個人就有成員的上傳待遇，但不在名單上、沒有葉子 ----
const dashed = (id) => [id.slice(0, 8), id.slice(8, 12), id.slice(12, 16), id.slice(16, 20), id.slice(20)].join('-');
check('UUID 有沒有連字號、大小寫都認；不是 UUID 的不認', plainUuid(dashed(BOB.id).toUpperCase()) === BOB.id
  && plainUuid(' ' + BOB.id + ' ') === BOB.id && plainUuid('Bob') === null && plainUuid(null) === null
  && plainUuid(BOB.id + '0') === null);
check('★ 不是開發者：看不到也加不了特別名單', (await worker.fetch(get('/admin/perks', bobWeb), env)).status === 403
  && (await worker.fetch(post('/admin/perk', { uuid: BOB.id }, bobWeb), env)).status === 403
  && (await worker.fetch(post('/admin/unperk', { uuid: BOB.id }, bobWeb), env)).status === 403
  && (await worker.fetch(get('/admin/perks'), env)).status === 403);
r = await worker.fetch(post('/admin/perk', { uuid: 'Bob', note: '' }, aliceWeb), env);
check('不是 UUID 回 400 bad uuid', r.status === 400 && (await r.json()).why === 'bad uuid');
r = await worker.fetch(post('/admin/perk', { uuid: BOB.id, note: 'x'.repeat(61) }, aliceWeb), env);
check('備註太長回 400 bad note', r.status === 400 && (await r.json()).why === 'bad note');
r = await worker.fetch(post('/admin/perk', { uuid: dashed(BOB.id), note: ' 朋友 ' }, aliceWeb), env);
check('★ 開發者加得進去（帶連字號的 UUID 也行）', r.status === 200 && (await r.json()).uuid === BOB.id
  && env.CORPUS.data.has('perk:' + BOB.id));
r = await worker.fetch(post('/admin/perk', { uuid: BOB.id }, aliceWeb), env);
check('已經在上面的不重複加', r.status === 409 && (await r.json()).why === 'already');
me = await (await worker.fetch(get('/me', bobWeb), env)).json();
check('★ 他現在有成員的待遇：20 MB，但不是開發者', me.member === true && me.developer === false
  && me.limit === MAX_CAPE_BYTES);
r = await worker.fetch(put('/cape', gif(44, 34, 4), bobToken), env);
const perkUpload = r.status === 200 ? {} : await r.json();
check('★ 他可以傳 GIF 了（實際 ' + r.status + ' ' + (perkUpload.why || '') + '）',
  r.status !== 403 && perkUpload.why !== 'gif members only');
check('★ 模組登入時知道他有這個待遇', (await (await login(bob)).json()).member === true
  && (await (await login(alice)).json()).member === true);
r = await worker.fetch(get('/admin/perks', aliceWeb), env);
const perkList = (await r.json()).perks;
check('★ 管理的人看得到名單：UUID、備註、誰加的', r.status === 200 && perkList.length === 1
  && perkList[0].uuid === BOB.id && perkList[0].note === '朋友' && perkList[0].by === ALICE.name
  && r.headers.get('cache-control') === 'no-store');
env.CORPUS.data.delete('presence:list');
const publicList = JSON.stringify(await (await worker.fetch(get('/list'), env)).json());
check('★ 公開的名單上看不出誰在特別名單', !publicList.includes('perk') && !publicList.includes(BOB.id));
r = await worker.fetch(post('/admin/unperk', { uuid: dashed(BOB.id) }, aliceWeb), env);
me = await (await worker.fetch(get('/me', bobWeb), env)).json();
check('★ 移除之後待遇就沒了', r.status === 200 && me.member === false && me.limit < MAX_CAPE_BYTES
  && (await (await login(bob)).json()).member === false
  && (await (await worker.fetch(get('/admin/perks', aliceWeb), env)).json()).perks.length === 0);

// ---- ★ 開發者：看得到是誰傳的 ----
r = await worker.fetch(get('/admin/capes', bobWeb), env);
check('★ 不是開發者看不到是誰傳的', r.status === 403);
r = await worker.fetch(get('/admin/capes', aliceWeb), env);
const adminCapes = (await r.json()).capes;
check('★ 開發者看得到每一件是誰傳的', r.status === 200 && adminCapes.length === 1
  && adminCapes[0].owners.map((o) => o.name).sort().join() === 'Alice_Dev,Bob');

// ---- ★ 開發者：刪除與封禁 ----
r = await worker.fetch(post('/admin/remove', { hash: capes[0].h }), env);
check('★ 沒有通行證不能刪', r.status === 403);
r = await worker.fetch(post('/admin/remove', { hash: capes[0].h }, bobWeb), env);
check('★ 不是開發者不能刪', r.status === 403);
r = await worker.fetch(post('/admin/remove', { hash: 'f'.repeat(64) }, aliceWeb), env);
check('沒有這張圖：成功但什麼都沒刪', r.status === 200 && (await r.json()).removed === 0);
r = await worker.fetch(post('/admin/remove', { id: bobId, ban: true }, aliceWeb), env);
check('★ 開發者刪掉他的披風並封禁', r.status === 200 && (await r.json()).removed === 1
  && !env.CORPUS.data.has('cape:' + bobId) && env.CORPUS.data.has('ban:' + BOB.id));
check('她還在用同一張圖，圖檔留著', blobs().length === 1);
list = await (await worker.fetch(get('/list'), env)).json();
check('名單上他的披風也沒了，她的還在', !(bobId in list.capes) && aliceId in list.capes);
r = await worker.fetch(put('/cape', png(22, 17), bobToken), env);
check('★ 被封禁的人不能再傳', r.status === 403 && (await r.json()).why === 'banned');
me = await (await worker.fetch(get('/me', bobWeb), env)).json();
check('他自己的帳號頁看得到被封禁', me.banned === true && me.cape === null);

// ---- ★ 封禁名單 ----
r = await worker.fetch(get('/admin/bans', bobWeb), env);
check('★ 不是開發者看不到封禁名單', r.status === 403);
r = await worker.fetch(get('/admin/bans', aliceWeb), env);
let bans = (await r.json()).bans;
check('★ 封禁名單：誰、什麼時候、誰封的', r.status === 200 && bans.length === 1 && bans[0].uuid === BOB.id
  && bans[0].name === 'Bob' && bans[0].by === 'Alice_Dev' && typeof bans[0].t === 'number');
env.CORPUS.data.set('ban:' + 'e'.repeat(32), { value: '1700000000000', metadata: null });   // 舊格式
bans = (await (await worker.fetch(get('/admin/bans', aliceWeb), env)).json()).bans;
check('舊格式的封禁（只存了時間）也列得出來', bans.length === 2 && bans.some((b) => b.name === null));
r = await worker.fetch(post('/admin/unban', { uuid: BOB.id }, bobWeb), env);
check('★ 不能自己替自己解除', r.status === 403);
r = await worker.fetch(post('/admin/unban', { uuid: BOB.id }, aliceWeb), env);
check('★ 開發者解除封禁，他又能傳了', r.status === 200 && !env.CORPUS.data.has('ban:' + BOB.id)
  && (await worker.fetch(put('/cape', png(44, 34), bobToken), env)).status === 200);
r = await worker.fetch(post('/admin/remove', { hash: capes[0].h }, aliceWeb), env);
check('★ 照雜湊刪：所有用這張圖的人一起拿掉，沒人用的圖檔也刪了', (await r.json()).removed === 1
  && !env.CORPUS.data.has('cape:' + aliceId) && blobs().length === 1);

// ADMIN_TOKEN 是另一條路（不在遊戲裡的時候用）
const admin = { CORPUS: env.CORPUS, ADMIN_TOKEN: 'sesame' };
check('密語不對不行', (await worker.fetch(get('/admin/bans', 'wrong'), admin)).status === 403);
check('ADMIN_TOKEN 也有管理權', (await worker.fetch(get('/admin/bans', 'sesame'), admin)).status === 200);

// ---- ★ 新增成員：開 PR ----
const CREDITS = JSON.stringify({
  _note: 'x',
  sections: [
    { role: '開發者', key: 'credits.role.developers', color: '#FFD24A', members: [{ name: 'A', mc: 'Alice_Dev' }] },
    { role: '貢獻者', key: 'credits.role.contributors', color: '#5BD86F', members: [] },
    { role: '資料來源', key: 'credits.role.sources', members: [{ name: 'Wynntils' }] },
  ],
}, null, 2) + '\n';
let added = addMember(CREDITS, { section: 'contributors', name: ' 小明 ', mc: 'Ming_01', color: '#abcdef' });
let doc = JSON.parse(added.text);
check('★ 加進對的那一區，名字去掉頭尾空白，顏色轉大寫', doc.sections[1].members.length === 1
  && JSON.stringify(doc.sections[1].members[0]) === JSON.stringify({ name: '小明', mc: 'Ming_01', color: '#ABCDEF' }));
doc.sections[1].members.pop();
check('★ 別的地方一個字都沒動：把那個人拿掉重寫一次，跟原本一模一樣',
  JSON.stringify(doc, null, 2) + '\n' === CREDITS && added.text.endsWith('}\n'));
check('不填顏色就沒有 color 這一欄', !('color' in JSON.parse(addMember(CREDITS,
  { section: 'sponsors'.replace('sponsors', 'contributors'), name: 'B', mc: 'Bee' }).text).sections[1].members[0]));
check('★ 已經在那一區的人不重複加（不分大小寫）',
  addMember(CREDITS, { section: 'developers', name: 'A2', mc: 'alice_dev' }).why === 'already listed');
check('★ 怪的 Minecraft ID、怪的區、怪的顏色、空的名字都不收',
  addMember(CREDITS, { section: 'contributors', name: 'x', mc: 'bad name!' }).why === 'bad mc'
  && addMember(CREDITS, { section: 'sources', name: 'x', mc: 'Okay' }).why === 'bad section'
  && addMember(CREDITS, { section: 'contributors', name: 'x', mc: 'Okay', color: 'red' }).why === 'bad color'
  && addMember(CREDITS, { section: 'contributors', name: '  ', mc: 'Okay' }).why === 'bad name'
  && addMember(CREDITS, { section: 'contributors', name: '<b>x</b>', mc: 'Okay' }).why === 'bad name');
check('沒有的區（credits.json 裡找不到）回 bad credits',
  addMember(CREDITS, { section: 'sponsors', name: 'x', mc: 'Okay' }).why === 'bad credits');

const entry = { section: 'contributors', name: '小明', mc: 'Ming_01' };
r = await worker.fetch(post('/admin/credits', entry, bobWeb), { CORPUS: env.CORPUS, GITHUB_TOKEN: 't' });
check('★ 不是開發者不能加人', r.status === 403);
r = await worker.fetch(post('/admin/credits', entry, aliceWeb), env);
check('沒設 GITHUB_TOKEN：說清楚是沒設定', r.status === 503 && (await r.json()).why === 'github not configured');
githubFile = CREDITS;
r = await worker.fetch(post('/admin/credits', entry, aliceWeb), { CORPUS: env.CORPUS, GITHUB_TOKEN: 'secret-token' });
body = await r.json();
check('★ 開發者送出：開好 PR，回 PR 的網址', r.status === 200 && body.url === 'https://github.com/LyuChaCha/WynnChaYuan/pull/9999');
check('★ 照順序：讀檔、讀 main、開分支、寫檔、開 PR（實際 ' + githubCalls.map((c) => c.method).join(' ') + '）',
  githubCalls.length === 5 && githubCalls[0].path.startsWith('/contents/') && githubCalls[1].path === '/git/ref/heads/main'
  && githubCalls[2].method === 'POST' && githubCalls[2].path === '/git/refs'
  && githubCalls[3].method === 'PUT' && githubCalls[4].path === '/pulls');
check('★ 每一個請求都帶著 token', githubCalls.every((c) => c.auth === 'Bearer secret-token'));
const written = JSON.parse(Buffer.from(githubCalls[3].body.content, 'base64').toString('utf8'));
check('★ 寫回去的檔多了那個人，中文沒有變亂碼', written.sections[1].members[0].name === '小明'
  && githubCalls[3].body.branch.startsWith('credits/add-ming_01-') && githubCalls[3].body.sha === 'filesha');
check('PR 是開到 main，說明裡寫了是誰送的', githubCalls[4].body.base === 'main'
  && githubCalls[4].body.body.includes('Alice_Dev') && githubCalls[4].body.title.includes('小明'));
r = await worker.fetch(post('/admin/credits', { section: 'developers', name: 'A', mc: 'Alice_Dev' }, aliceWeb),
  { CORPUS: env.CORPUS, GITHUB_TOKEN: 't' });
check('已經在名單上：409', r.status === 409 && (await r.json()).why === 'already listed');

// ---- 自己拿掉 ----
pushBack(bobId);
await worker.fetch(put('/cape', png(22, 17), bobToken), env);
r = await worker.fetch(del('/cape', bobToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('自己拿掉披風：那一筆、圖檔、名單都沒了', r.status === 200
  && !env.CORPUS.data.has('cape:' + bobId) && !(bobId in list.capes) && blobs().length === 0);

// ---- ★ 檔案上限分級：一般玩家 1 MB，名單上的成員 20 MB ----
const padded = (width, height, size) => {
  const out = new Uint8Array(size);
  out.set(png(width, height));
  return out;
};
r = await worker.fetch(put('/cape', padded(88, 68, 1024 * 1024 + 1), bobToken), env);
body = await r.json();
check('★ 一般玩家超過 1 MB：不收，而且說了上限是多少', r.status === 413 && body.why === 'too large' && body.limit === 1024 * 1024);
r = await worker.fetch(put('/cape', padded(88, 68, 1024 * 1024), bobToken), env);
body = await r.json();
check('一般玩家剛好 1 MB：收', r.status === 200);
check('★ 上傳成功時回尺寸（告訴人這件披風多大）', body.width === 88 && body.height === 68 && body.type === 'png');
r = await worker.fetch(get('/me', bobToken), env);
body = await r.json();
check('★ 帳號頁拿得到自己的上限與披風尺寸', body.limit === 1024 * 1024 && body.cape.x === 88 && body.cape.y === 68);
check('一覽裡每一件也有尺寸', (await (await worker.fetch(api('/capes'), env)).json()).capes
  .some((cape) => cape.x === 88 && cape.y === 68));
if (env.CORPUS.data.has('cape:' + aliceId)) pushBack(aliceId);
r = await worker.fetch(put('/cape', padded(176, 136, 3 * 1024 * 1024), aliceToken), env);
check('★ 名單上的成員超過 1 MB 照收（上限 20 MB）', r.status === 200
  && (await (await worker.fetch(get('/me', aliceToken), env)).json()).limit === MAX_CAPE_BYTES);
await worker.fetch(del('/cape', aliceToken), env);
pushBack(bobId);
await worker.fetch(del('/cape', bobToken), env);

// ---- ★ 一鍵綁定 Discord：從遊戲開的網頁（已經驗過遊戲帳號）直接綁 ----
check('要先以遊戲帳號登入才能開綁定單', (await dc('/link/intent', null, {})).status === 401);
check('已經綁了 Discord 的那種登入不必再開', (await dc('/link/intent', dcToken, {})).status === 409);
r = await dc('/link/intent', bobToken, {});
body = await r.json();
const intentCookie = (r.headers.get('set-cookie') || '').split(';')[0];
check('★ 開單：回要去的網址，單子用 cookie 綁在這個瀏覽器上', r.status === 200
  && /^\/auth\/discord\?intent=[0-9a-f]{32}$/.test(body.url) && /^wcy_intent=[0-9a-f]{32}$/.test(intentCookie)
  && /HttpOnly/i.test(r.headers.get('set-cookie')));
discordUser = { id: '333333333333333333', username: 'bob_dc', global_name: 'Bob 的 Discord' };
// 別人的瀏覽器（沒有那個 cookie）拿著同一張單子去登入：照樣登入，但不綁
r = await page(body.url);
let stateNow = new URL(r.headers.get('location')).searchParams.get('state');
r = await oauthBack('code=goodcode&state=' + stateNow, (r.headers.get('set-cookie') || '').split(';')[0]);
const stranger = r.headers.get('location').match(/#t=(dc\.[^&]+)$/)[1];
check('★ 單子不是這個瀏覽器開的：登入照常，但沒有被綁到別人的遊戲帳號',
  (await (await dc('/me', stranger)).json()).linked === false);
// 開單的那個瀏覽器
r = await page(body.url, { cookie: intentCookie });
stateNow = new URL(r.headers.get('location')).searchParams.get('state');
check('單子帶進 state（跟著 Discord 繞一圈回來）', stateNow.split('.')[1].length === 64);
r = await oauthBack('code=goodcode&state=' + stateNow, (r.headers.get('set-cookie') || '').split(';')[0]);
const bobDc = r.headers.get('location').match(/#t=(dc\.[^&]+)$/)[1];
body = await (await dc('/me', bobDc)).json();
check('★ 登入完就綁好了：不必打指令', body.linked === true && body.name === BOB.name && body.discord.name === 'Bob 的 Discord');
check('★ 單子用過就丟', [...env.CORPUS.data.keys()].filter((k) => k.startsWith('intent:')).length === 0);
await dc('/unlink', bobDc, {});

r = await worker.fetch(get('/status'), env);
check('/api/status 回人數與上傳模式', JSON.stringify(await r.json()).includes('"uploads":"tiered"'));

console.log(failures === 0 ? '登記站：全部通過' : '登記站：' + failures + ' 項失敗');
process.exit(failures === 0 ? 0 : 1);
