// node tools/presence/worker.test.mjs
import worker from './worker.js';
import {
  publicId, issueToken, issueWebToken, readToken, needsWrite, applyRecord, badgePrefs, plainUuid, inspectCape, gifFrames, groupCapes,
  teamOf, staffKey, guildSlug, imageType, MAX_GUILD_IMAGE,
  addMember, setMemberTeams, addMemberTeams, setPersonTeams, mergeLangTeam, teamsInput, TEAM_AREAS, TR_LANGS, certificatePayload, issueChallenge, issueDiscordToken, LOGIN_PREFIX,
  MAX_CAPE_BYTES, MAX_CAPE_FRAMES, burstLock, burstNote, BURST_LOCK_MS, BURST_WINDOW_MS, personScore, placeholderProblem, replaceTranslation,
  feedbackCheck, feedbackIssue, FEEDBACK_PER_HOUR, FEEDBACK_KEEP_DAYS, DISCORD_COMMANDS,
  visibility, askedVisibility, VISIBILITY, dropFromList, goneCode, anonymise,
  indexOwners, ownerEntry, participantsOf, OWNER_INDEX_READY,
} from './worker-api.js';
import { CERT_KEYS, PROFILE_KEYS } from './mojang-keys.js';
import { decodePng, decodeGif, encodePng, encodeGif, layout } from './cape-preview.js';

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
    async list({ prefix = '', limit = 1000, cursor } = {}) {
      ops.list++;
      const all = [...data.keys()].filter((k) => k.startsWith(prefix)).sort();
      const start = cursor ? all.findIndex((k) => k > cursor) : 0;
      const slice = start < 0 ? [] : all.slice(start, start + limit);
      const keys = slice.map((name) => ({ name, metadata: data.get(name).metadata }));
      const complete = start < 0 || start + limit >= all.length;
      return complete ? { keys, list_complete: true } : { keys, list_complete: false, cursor: slice[slice.length - 1] };
    },
  };
}

// ---- 假的 Mojang 與 GitHub ----
const ALICE = { id: 'a'.repeat(32), name: 'Alice_Dev' };          // 名單上的人
const BOB = { id: 'b'.repeat(32), name: 'Bob' };                  // 不在名單上
let githubFile = '';
const discordCalls = [];
let discordAppKey = '';
let fakeSkin = null;
let discordUser = { id: '111111111111111111', username: 'alice_dc', global_name: 'Alice 的 Discord' };
const githubCalls = [];
let githubIssueStatus = 201;
const githubBranches = new Set();
let githubOpenPulls = [];         // 回饋開 Issue 時 GitHub 回什麼（測失敗用）
let githubPullStatus = 201;
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
    if (path.startsWith('/git/ref/heads/') && githubBranches.has(path.slice('/git/ref/heads/'.length))) {
      return answer({ object: { sha: 'branchsha' } });
    }
    if (call.method === 'GET' && path.startsWith('/pulls?')) return answer(githubOpenPulls);
    if (path === '/git/refs') return answer({}, 201);
    if (path === '/issues' && call.method === 'POST') {
      return githubIssueStatus === 201
        ? answer({ html_url: 'https://github.com/LyuChaCha/WynnChaYuan/issues/77', number: 77 }, 201)
        : answer({ message: 'nope' }, githubIssueStatus);
    }
    if (call.method === 'PUT') return answer({}, 200);
    if (path === '/pulls') {
      if (githubPullStatus === 'no url') return answer({ number: 1 }, 201);
      return githubPullStatus === 201 ? answer({ html_url: 'https://github.com/LyuChaCha/WynnChaYuan/pull/9999' }, 201) : answer({ message: 'nope' }, githubPullStatus);
    }
    return answer({}, 404);
  }
  // 沒有 Mojang 這一支：Worker 連不到它（403），登入不可以再去問。問了就在下面丟例外。
  if (u.hostname === 'discord.com') {
    discordCalls.push({ path: u.pathname, method: init.method || 'GET', body: String(init.body || ''),
      auth: (init.headers || {}).authorization });
    if (u.pathname.startsWith('/api/v10/webhooks/')) return new Response('{}', { status: 200 });
    if (u.pathname === '/api/v10/applications/@me') {
      return new Response(JSON.stringify({ id: 'client1', name: 'WynnChaYuan', bot: { username: 'WynnChaYuan' },
        verify_key: discordAppKey, interactions_endpoint_url: 'https://wynnchayuan.com/api/discord/interactions' }), { status: 200 });
    }
    if (u.pathname === '/api/v10/applications/client1/commands' && (init.method || 'GET') === 'GET') {
      return new Response(JSON.stringify([{ name: 'status' }, { name: 'cape' }]), { status: 200 });
    }
    if (u.pathname === '/api/v10/users/@me/guilds') return new Response(JSON.stringify([{ name: 'YuChaYuan' }]), { status: 200 });
    if (u.pathname.startsWith('/api/v10/applications/') && init.method === 'PUT') {
      return new Response(JSON.stringify(JSON.parse(init.body).map((c) => ({ id: '1', name: c.name }))), { status: 200 });
    }
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
  if (u.hostname === 'minotar.net' && u.pathname.startsWith('/skin/')) {
    return fakeSkin ? new Response(fakeSkin, { status: 200 }) : new Response('', { status: 404 });
  }
  if (u.hostname === 'raw.githubusercontent.com' && u.pathname.includes('/translate-index/')) {
    return new Response(JSON.stringify({ files: ['npc.json', 'quest/q.json'], rows: [
      [0, null, 'Hello {u}!', '你好，{u}！'], [1, 'Q#001', 'Hello there, traveller.', ''], [1, 'Q#002', 'Bye', '再見']] }), { status: 200 });
  }
  if (u.hostname === 'raw.githubusercontent.com') {
    return new Response(JSON.stringify({
      sections: [
        { role: 'dev', key: 'credits.role.developers', members: [{ name: 'A', mc: 'Alice_Dev', teams: ['ja_jp', 'zh_tw', 'xx_xx'], areas: ['code', 'bogus'] }, { name: 'data' }] },
        { role: 'sponsor', key: 'credits.role.sponsors', members: [{ name: 'C', mc: 'Carol' }] }],
    }), { status: 200 });
  }
  throw new Error('unexpected fetch ' + u);
};

/** 網頁的路徑照原樣，其餘的都是 API。 */
const at = (path) => 'https://x.test' + (['/', '/features', '/capes', '/account', '/admin', '/translate', '/feedback', '/privacy', '/terms'].includes(path) ? path : '/api' + path);
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
/** 「過了很久」：上傳冷卻的紀錄一起清掉（測試裡同一個人常常連傳好幾件）。 */
const forgetBursts = () => { for (const k of [...env.CORPUS.data.keys()]) if (k.startsWith('upl:')) env.CORPUS.data.delete(k); };
const NOW = Date.now();
const ENGLISH_NAMES = { zh_cn: ['Bony Bow', 'Flawless Oak Bow', 'Rotten Flesh', 'Zombie {#}{#}', 'Weapon Merchant', 'Trade Market'] };
const englishOnPurpose = (code, src) => (ENGLISH_NAMES[code] || []).includes(src);

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
check('★ 回來時沒有那個 cookie（別人把他的登入連結塞給你）：不認', r.headers.get('location') === 'https://x.test/account#e=discord&r=cookie');
r = await oauthBack('code=goodcode&state=' + state, 'wcy_oauth=' + 'f'.repeat(32));
check('★ cookie 對不上 state：不認', r.headers.get('location').endsWith('#e=discord&r=cookie'));
r = await oauthBack('code=goodcode&state=' + state.replace(/\.[0-9a-f]{64}$/, '.' + '0'.repeat(64)), oauthCookie);
check('★ state 的簽章是假的：不認', r.headers.get('location').endsWith('#e=discord&r=state'));
r = await oauthBack('code=wrong&state=' + state, oauthCookie);
check('Discord 不認這個 code：不認，原因碼帶著 Discord 的狀態', /#e=discord&r=token-\d{3}/.test(r.headers.get('location')));
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
const pushBack = (id) => { env.CORPUS.data.get('cape:' + id).metadata.v -= 60000; forgetBursts(); };
pushBack(aliceId);
r = await worker.fetch(put('/cape', png(22, 17), aliceToken), env);
check('她改傳跟他一樣的圖', r.status === 200);
const histOf = (id) => [...env.CORPUS.data.keys()].filter((k) => k.startsWith('hist:' + id + ':')).map((k) => k.split(':')[2]);
// 沒有人穿的披風留七天（使用者 2026-10-13）：閒置清單與「把時間快轉」
const idleNow = () => JSON.parse((env.CORPUS.data.get('idle') || { value: '{}' }).value || '{}');
const expire = (hash) => {
  const map = idleNow();
  for (const h of Object.keys(map)) if (!hash || h === hash) map[h].at -= 8 * 86400000;
  env.CORPUS.data.set('idle', { value: JSON.stringify(map), metadata: null });
};
check('★ 兩個人同一張圖只存一份；她原本那張沒有人穿了，先留著倒數（實際 ' + blobs().length + ' 份）',
  blobs().length === 2 && histOf(aliceId).length === 2 && Object.keys(idleNow()).length === 1);
{
  // 她也可以自己從紀錄拿掉；那只是少一筆紀錄，圖照樣等七天
  const wearing = env.CORPUS.data.get('cape:' + aliceId).metadata.h;
  const stale = histOf(aliceId).find((h) => h !== wearing);
  r = await worker.fetch(post('/cape/forget', { hash: wearing }, aliceToken), env);
  check('現在穿著的那一件不能從紀錄拿掉', r.status === 409 && (await r.json()).why === 'in use');
  check('沒登入不能動紀錄', (await worker.fetch(post('/cape/forget', { hash: stale }), env)).status === 401);
  r = await worker.fetch(post('/cape/forget', { hash: stale }, aliceToken), env);
  check('★ 從紀錄拿掉：只是少一筆紀錄', r.status === 200 && blobs().length === 2 && histOf(aliceId).length === 1);
  const seen = await (await worker.fetch(api('/capes'), env)).json();
  const waiting = seen.capes.find((c) => c.h === stale);
  check('★ 沒有人穿的那一張還在一覽上：0 個人、記著從什麼時候開始、伺服器說留七天', seen.capes.length === 2
    && Boolean(waiting) && waiting.ids.length === 0 && waiting.idle > 0 && seen.keep === 7 * 86400000 && seen.now >= waiting.idle);
  check('★ 它的圖照雜湊拿得到；不在清單上的雜湊拿不到', (await worker.fetch(get('/idle/' + stale), env)).status === 200
    && (await worker.fetch(get('/idle/' + wearing), env)).status === 404
    && (await worker.fetch(get('/idle/nope'), env)).status === 404);
  expire(stale);
  check('★ 滿七天沒有人穿：一覽上沒有了，圖也刪了', (await (await worker.fetch(api('/capes'), env)).json()).capes.length === 1
    && blobs().length === 1 && Object.keys(idleNow()).length === 0
    && (await worker.fetch(get('/idle/' + stale), env)).status === 404);
}
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
const { LANGS, PATHS, TEXT, pickLang, render } = await import('./site.js');
const inLang = (path, headers) => worker.fetch(new Request('https://x.test' + path, { headers }), env);
const pages = {};
const english = {};
const served = {};
const withScripts = async (html) => {
  let out = html;
  for (const m of [...html.matchAll(/<script src="(\/js\/[^"]+)"><\/script>/g)]) {
    if (!served[m[1]]) {
      const answer = await worker.fetch(new Request('https://x.test' + m[1]), env);
      served[m[1]] = { answer, code: await answer.text() };
    }
    out = out.replace(m[0], () => '<script>' + served[m[1]].code + '</script>');
  }
  return out;
};
const rawPages = {};
for (const path of ['/', '/features', '/capes', '/account', '/admin', '/translate', '/feedback', '/privacy', '/terms']) {
  english[path] = await withScripts(await (await inLang(path, { cookie: 'lang=en' })).text());
  r = await inLang(path, { cookie: 'lang=zh' });
  rawPages[path] = await r.text();
  pages[path] = await withScripts(rawPages[path]);
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
// ---- ★ 連結預覽（Discord 貼連結出來的那一塊）與互動的底線 ----
{
  const bare = await (await worker.fetch(new Request('https://x.test/'), env)).text();
  const meta = (html, name) => (html.match(new RegExp('<meta (?:name|property)="' + name + '" content="([^"]*)"')) || [])[1] || '';
  check('★ 什麼語言都沒說的訪客（抓預覽的機器人）：介紹是中文與英文各一句',
    /翻譯模組/.test(meta(bare, 'og:description')) && /translation mod/.test(meta(bare, 'og:description'))
    && meta(bare, 'description') === meta(bare, 'og:description'));
  check('預覽有標題、網址、類型，圖是我們的圖示', meta(bare, 'og:title').includes('WynnChaYuan')
    && meta(bare, 'og:url') === 'https://wynnchayuan.com/' && meta(bare, 'og:type') === 'website'
    && meta(bare, 'og:image') === 'https://wynnchayuan.com/icon-192.png' && meta(bare, 'twitter:card') === 'summary');
  check('★ 有說語言的訪客：介紹只有那一種語言', /翻譯模組/.test(meta(pages['/'], 'og:description'))
    && !/translation mod/.test(meta(pages['/'], 'og:description'))
    && /translation mod/.test(meta(english['/'], 'og:description')) && !/翻譯/.test(meta(english['/'], 'og:description')));
  check('介紹裡沒有標籤、也沒有會把屬性弄斷的引號', LANGS.every((lang) => {
    const text = (render('/', lang).match(/<meta property="og:description" content="([^"]*)"/) || [])[1] || '';
    return text.length > 40 && !/[<>]/.test(text);
  }));
  check('每一頁的預覽網址是自己那一頁', meta(pages['/capes'], 'og:url') === 'https://wynnchayuan.com/capes');
  const css = pages['/'];
  check('★ 動作有給「不想看動來動去」的人一條路，懸停只給有滑鼠的裝置',
    css.includes('@media (prefers-reduced-motion: reduce)') && css.includes('@media (hover: hover) and (pointer: fine)'));
  check('沒有 transition: all、沒有從 0 放大、沒有 ease-in', !/transition:\s*all/.test(css) && !/scale\(0\)/.test(css)
    && !/ease-in[^-]/.test(css));
  check('上傳那一塊可以拖放，也有試穿的說明', pages['/account'].includes('id="drop"')
    && LANGS.every((lang) => TEXT[lang]['account.drop'] && TEXT[lang].js.previewHint.includes('{w}')));
}
check('★ 披風一覽上寫著不可以上傳什麼', ['廣告', '公會宣傳', '濫用', '政治', '不雅', '違反法規', '種族歧視'].every((w) => pages['/capes'].includes(w)));
check('★ 英文版的披風一覽也寫著不可以上傳什麼',
  ['political', 'obscene', 'unlawful', 'racist'].every((w) => english['/capes'].includes(w)));
check('英文版的帳號頁用的是遊戲裡英文介面的字',
  ['F6', 'Features', 'Cape gallery'].every((w) => english['/account'].includes(w)));
check('★ 開關拿掉了：網站不再叫人去確認它開著', LANGS.every((lang) => !/Let others know|讓別人知道|让别人知道/.test(TEXT[lang]['account.s2'])));

// ---- ★ 網站的語言 ----
const WANTED = ['zh', 'zh-cn', 'en', 'ja', 'ko', 'ru', 'es', 'tr'];
check('★ 八種語言都在，選單照這個順序（實際 ' + LANGS.join() + '）', LANGS.join() === WANTED.join()
  && PATHS.join() === '/,/features,/capes,/account,/admin,/translate,/feedback,/privacy,/terms');
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
    && home.includes('data-lang="' + lang + '" role="menuitemradio" aria-checked="true"'));
  for (const path of PATHS) {
    const html = await (await inLang(path + '?lang=' + lang, {})).text();
    check(lang + ' 的 ' + path + ' 沒有會被樣板字串吃掉的寫法、也沒有漏掉的鍵',
      !html.includes(String.fromCharCode(96)) && !html.includes('$' + '{') && !/%%[\w.]+%%/.test(html));
  }
}
check('html 上的語言標籤各不相同', new Set(LANGS.map((l) => TEXT[l].html)).size === LANGS.length);
// 看得到的字：拿掉程式與樣式、標籤；語言選單與首頁的語言清單本來就是各寫各的
const visible = (html) => html.replace(/<script[\s\S]*?<\/script>/g, ' ').replace(/<style[\s\S]*?<\/style>/g, ' ')
  .replace(/<button type="button" class="chip"[^>]*>[^<]*<\/button>/g, ' ').replace(/<select class="lang"[\s\S]*?<\/select>/g, ' ').replace(/<div class="hn-langs[^"]*"[\s\S]*?<\/div>/g, ' ')
  .replace(/<div class="seg" id="demo-langs"[\s\S]*?<\/div>/g, ' ')        // 功能頁示範的語言鈕：各寫各的名字
  .replace(/<[^>]+>/g, ' ');
const cjk = /[㐀-鿿＀-￯　-〿]/;
for (const path of PATHS) {
  const left = visible(english[path]).match(cjk);
  check('★ 英文版的 ' + path + ' 看得到的字裡沒有中文' + (left ? '（' + left[0] + '）' : ''), !left);
  const strings = (english[path].match(/<script type="application\/json" id="site-text">(.*?)<\/script>/s) || [])[1] || '';
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
check('語言選單八種都在、選著的是現在這一種（頁首的地球與頭像選單；詳細的在〈頁首〉那一段）',
  english['/'].includes('<div class="menu hn-menu hn-narrow" id="hn-globe-menu" role="menu">') && english['/'].includes('data-lang="en" role="menuitemradio" aria-checked="true"')
  && pages['/'].includes('data-lang="zh" role="menuitemradio" aria-checked="true"') && WANTED.every((l) => english['/'].includes('<a href="?lang=' + l + '"'))
  && ['繁體中文', '简体中文', 'English', '日本語', '한국어', 'Русский', 'Español', 'Türkçe'].every((n) => english['/'].includes('>' + n)));
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

check('首頁有下載的連結：主要按鈕是 GitHub Releases、其次 CurseForge',
  /<a class="btn primary" href="https:\/\/github\.com\/LyuChaCha\/WynnChaYuan\/releases\/latest">從 GitHub 下載<\/a>/.test(pages['/'])
  && /<a class="btn primary" href="https:\/\/github\.com\/LyuChaCha\/WynnChaYuan\/releases\/latest">Download on GitHub<\/a>/.test(english['/'])
  && pages['/'].includes('https://www.curseforge.com/minecraft/mc-mods/wynnchayuan'));
check('★ 沒有任何一頁連到我們在 Modrinth 的專案頁（2026-10 起不在 Modrinth 上架）',
  PATHS.every((path) => !pages[path].includes('modrinth.com/mod/wynnchayuan') && !english[path].includes('modrinth.com/mod/wynnchayuan')));
check('帳號頁講了怎麼從遊戲裡登入', pages['/account'].includes('F6') && pages['/account'].includes('披風一覽'));
check('★ 網頁只准跑自己的程式與 jsDelivr 的立體檢視：不准頁面裡的內嵌程式',
  (r.headers.get('content-security-policy') || '').includes("script-src 'self' https://cdn.jsdelivr.net;")
  && !/script-src[^;]*unsafe-inline/.test(r.headers.get('content-security-policy') || ''));
check('★ 每一頁都沒有內嵌程式：只有外部檔案與 JSON 資料', Object.values(rawPages).every((html) =>
  [...html.matchAll(/<script([^>]*)>/g)].every((m) => / src="/.test(m[1]) || / type="application\/json"/.test(m[1]))));
check('★ 外部程式檔：JavaScript、可以長期快取、網址帶內容的雜湊', Object.entries(served).length >= 9
  && Object.entries(served).every(([path, { answer }]) => answer.status === 200 && /^\/js\/[a-z-]+\.[0-9a-f]{16}\.js$/.test(path)
    && answer.headers.get('content-type').startsWith('text/javascript') && answer.headers.get('cache-control').includes('immutable')
    && answer.headers.get('x-content-type-options') === 'nosniff'));
check('不認得的程式檔 404', (await worker.fetch(new Request('https://x.test/js/nope.0000000000000000.js'), env)).status === 404);
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

const TEAMED = JSON.stringify({
  _note: 'x',
  sections: [
    { role: '開發者', key: 'credits.role.developers', color: '#FFD24A', members: [{ name: 'A', mc: 'Alice_Dev', cape: 'a.gif' }] },
    { role: '貢獻者', key: 'credits.role.contributors', color: '#5BD86F', members: [{ name: '小明', mc: 'Ming_01' }, { name: '沒有 ID' }] },
  ],
}, null, 2) + '\n';
let teamed = setMemberTeams(TEAMED, { section: 'contributors', who: 'ming_01', teams: ['ja_jp', 'zh_tw', 'ja_jp'], areas: ['testing', 'translation'] });
doc = JSON.parse(teamed.text);
check('★ teams／areas 寫進那個人（遊戲 ID 不分大小寫），照固定順序、去掉重複', JSON.stringify(doc.sections[1].members[0])
  === JSON.stringify({ name: '小明', mc: 'Ming_01', teams: ['zh_tw', 'ja_jp'], areas: ['translation', 'testing'] }) && teamed.name === '小明');
check('★ 別人一個字都沒動；全部取消勾選就把兩欄拿掉，檔案回到原樣',
  JSON.stringify(doc.sections[0]) === JSON.stringify(JSON.parse(TEAMED).sections[0])
  && setMemberTeams(teamed.text, { section: 'contributors', who: 'Ming_01', teams: [], areas: [] }).text === TEAMED);
check('只改其中一欄，另一欄不會多出空陣列', !('teams' in JSON.parse(setMemberTeams(TEAMED,
  { section: 'contributors', who: 'Ming_01', teams: [], areas: ['code'] }).text).sections[1].members[0]));
check('沒有遊戲 ID 的成員照名字找', JSON.parse(setMemberTeams(TEAMED,
  { section: 'contributors', who: '沒有 ID', teams: ['ko_kr'], areas: [] }).text).sections[1].members[1].teams.join() === 'ko_kr');
const whyTeams = (entry) => setMemberTeams(TEAMED, { section: 'contributors', who: 'Ming_01', teams: [], areas: [], ...entry }).why;
check('★ 不認得的語言、項目、區、人都擋，什麼都沒改也擋', whyTeams({ teams: ['xx_xx'] }) === 'bad team'
  && whyTeams({ teams: 'zh_tw' }) === 'bad team' && whyTeams({ teams: [1] }) === 'bad team'
  && whyTeams({ areas: ['hack'] }) === 'bad area' && whyTeams({ areas: null }) === 'bad area'
  && whyTeams({ section: 'sources' }) === 'bad section' && whyTeams({ who: '  ' }) === 'bad who'
  && whyTeams({ who: 'x'.repeat(41) }) === 'bad who' && whyTeams({ who: 'Nobody', areas: ['code'] }) === 'not found'
  && whyTeams({}) === 'unchanged' && setMemberTeams('{', { section: 'contributors', who: 'a', teams: [], areas: [] }).why === 'bad credits');
check('同一個人要在那一區才找得到', whyTeams({ section: 'developers', teams: ['zh_tw'] }) === 'not found');
check('語言就是協作翻譯那七種，項目八種', TR_LANGS.length === 7 && TEAM_AREAS.length === 8
  && teamsInput({ section: 'sponsors', who: 'x', teams: TR_LANGS, areas: TEAM_AREAS }).teams.length === 7);

const teamEntry = { section: 'contributors', who: 'Ming_01', teams: ['zh_tw'], areas: ['translation'] };
const teamPost = (entry, token, extra = {}) => worker.fetch(post('/admin/credits/teams', entry, token), { CORPUS: env.CORPUS, ...extra });
check('★ 一般玩家不能改團隊', (await teamPost(teamEntry, bobWeb, { GITHUB_TOKEN: 't' })).status === 403
  && (await teamPost(teamEntry, undefined, { GITHUB_TOKEN: 't' })).status === 403);
r = await teamPost(teamEntry, aliceWeb);
check('沒設 GITHUB_TOKEN：說清楚是沒設定', r.status === 503 && (await r.json()).why === 'github not configured');
githubCalls.length = 0;
r = await teamPost({ ...teamEntry, teams: ['en_us'] }, aliceWeb, { GITHUB_TOKEN: 't' });
check('★ 送來的東西不對：還沒碰 GitHub 就擋掉', r.status === 400 && (await r.json()).why === 'bad team' && githubCalls.length === 0);
githubFile = TEAMED;
r = await teamPost(teamEntry, aliceWeb, { GITHUB_TOKEN: 'secret-token' });
body = await r.json();
const teamBranch = githubCalls.find((c) => c.method === 'PUT').body.branch;
check('★ 開發者送出：開好當天的分支與 PR', r.status === 200 && body.url === 'https://github.com/LyuChaCha/WynnChaYuan/pull/9999'
  && body.added === false && /^credits\/teams-\d{8}$/.test(teamBranch));
check('★ 照順序：查當天的分支、從 main 讀檔、讀 main、開分支、寫檔、開 PR（實際 ' + githubCalls.map((c) => c.method + ' ' + c.path.split('?')[0]).join('、') + '）',
  githubCalls.length === 6 && githubCalls[0].path === '/git/ref/heads/' + teamBranch
  && githubCalls[1].path.endsWith('?ref=main') && githubCalls[2].path === '/git/ref/heads/main'
  && githubCalls[3].method === 'POST' && githubCalls[3].body.ref === 'refs/heads/' + teamBranch
  && githubCalls[4].method === 'PUT' && githubCalls[5].path === '/pulls' && githubCalls.every((c) => c.auth === 'Bearer secret-token'));
const teamWritten = JSON.parse(Buffer.from(githubCalls[4].body.content, 'base64').toString('utf8'));
check('★ 寫回去的檔：那個人多了 teams 與 areas，其他人照舊', JSON.stringify(teamWritten.sections[1].members[0])
  === JSON.stringify({ name: '小明', mc: 'Ming_01', teams: ['zh_tw'], areas: ['translation'] })
  && teamWritten.sections[0].members[0].cape === 'a.gif' && githubCalls[4].body.message.includes('小明'));
check('PR 開到 main，說明寫了是誰送的，沒有多餘的署名行', githubCalls[5].body.base === 'main' && githubCalls[5].body.head === teamBranch
  && githubCalls[5].body.body.includes('Alice_Dev') && !/Generated with|Co-Authored-By/.test(githubCalls[5].body.body));
githubBranches.add(teamBranch);
githubOpenPulls = [{ html_url: 'https://github.com/LyuChaCha/WynnChaYuan/pull/42' }];
githubCalls.length = 0;
r = await teamPost({ ...teamEntry, who: '沒有 ID', teams: ['ja_jp'] }, aliceWeb, { GITHUB_TOKEN: 't' });
body = await r.json();
check('★ 同一天第二筆：加進同一個分支與 PR，不再開新的', r.status === 200 && body.added === true
  && body.url === 'https://github.com/LyuChaCha/WynnChaYuan/pull/42'
  && githubCalls.some((c) => c.path.startsWith('/contents/') && c.path.endsWith('?ref=' + encodeURIComponent(teamBranch)))
  && githubCalls.find((c) => c.method === 'PUT').body.branch === teamBranch
  && !githubCalls.some((c) => c.method === 'POST'));
githubOpenPulls = [];
githubCalls.length = 0;
r = await teamPost(teamEntry, aliceWeb, { GITHUB_TOKEN: 't' });
body = await r.json();
const lateBranch = (githubCalls.find((c) => c.method === 'PUT') || { body: {} }).body.branch || '';
check('★ 當天的 PR 已經合併或關掉：另開一個分支與 PR，不寫進舊分支', r.status === 200 && body.added === false
  && lateBranch.startsWith(teamBranch + '-') && githubCalls.some((c) => c.path === '/pulls' && c.method === 'POST'));
githubBranches.clear();
r = await teamPost({ ...teamEntry, teams: [], areas: [] }, aliceWeb, { GITHUB_TOKEN: 't' });
check('沒有要改的：409，不開分支', r.status === 409 && (await r.json()).why === 'unchanged');
r = await teamPost({ ...teamEntry, who: 'Nobody' }, aliceWeb, { GITHUB_TOKEN: 't' });
check('名單上沒有這個人：404', r.status === 404 && (await r.json()).why === 'not found');

// ---- 自己拿掉 ----
pushBack(bobId);
await worker.fetch(put('/cape', png(22, 17), bobToken), env);
r = await worker.fetch(del('/cape', bobToken), env);
list = await (await worker.fetch(get('/list'), env)).json();
check('自己拿掉披風：那一筆與名單都沒了；圖先留著倒數', r.status === 200
  && !env.CORPUS.data.has('cape:' + bobId) && !(bobId in list.capes) && histOf(bobId).length > 0
  && blobs().length === 2 && Object.keys(idleNow()).length === 2);
{
  // 每天一次的整理：期滿的刪掉；沒有人在用、清單上也沒有的圖補記進去（不公開），再等七天
  const stray = 'blob:' + 'ab'.repeat(32);
  env.CORPUS.data.set(stray, { value: new Uint8Array([1, 2, 3]), metadata: { t: 'png', s: 3 } });
  await worker.scheduled({}, env);
  check('★ 整理：還沒滿七天的不動，漏網的圖補記進清單（不公開）', blobs().length === 3
    && idleNow()['ab'.repeat(32)].q === 1
    && !(await (await worker.fetch(api('/capes'), env)).json()).capes.some((c) => c.h === 'ab'.repeat(32)));
  expire();
  await worker.scheduled({}, env);
  check('★ 整理：滿七天的全部刪掉', blobs().length === 0 && Object.keys(idleNow()).length === 0);
  r = await worker.fetch(get('/capes/mine', bobToken), env);
  check('★ 他的使用紀錄跟著清掉，告訴他移除了幾件', (await r.json()).removed === 2 && histOf(bobId).length === 0);
}

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

// ---- ★ 頁首、頁尾、首頁的團隊、功能頁 ----
for (const path of PATHS) {
  const html = pages[path];
  check(path + ' 頁首有下載選單（GitHub Releases、CurseForge）與贊助', html.includes('id="get-menu"')
    && ['https://www.curseforge.com/minecraft/mc-mods/wynnchayuan',
      'https://github.com/LyuChaCha/WynnChaYuan/releases/latest', 'https://ko-fi.com/lyuchacha']
      .every((link) => html.split('<main>')[0].includes(link)));
  const foot = html.split('<footer>')[1] || '';
  check('★ ' + path + ' 頁尾有 GitHub、兩份授權、兩個下載點（GitHub Releases 在前）、Ko-fi、聯絡方式',
    ['href="https://github.com/LyuChaCha/WynnChaYuan"', '/blob/main/LICENSE"', '/blob/main/LICENSE-DATA"',
      'github.com/LyuChaCha/WynnChaYuan/releases/latest', 'curseforge.com/minecraft/mc-mods/wynnchayuan', 'ko-fi.com/lyuchacha',
      '網站開發者：LyuChaCha', 'Discord：LyuChaCha', 'AGPL-3.0', 'CC BY-NC-SA 4.0'].every((w) => foot.includes(w))
    && foot.indexOf('/releases/latest') < foot.indexOf('curseforge.com/minecraft/mc-mods/wynnchayuan')
    && !foot.includes('modrinth.com/mod/wynnchayuan'));
  check('★ ' + path + ' 頁尾寫著跟 Wynncraft 官方沒有關係', /Wynncraft、Mojang、Microsoft 沒有關係/.test(foot)
    && /Not affiliated with Wynncraft/.test(english[path].split('<footer>')[1]));
}
check('★ 選單多了「功能」', pages['/'].includes('<a class="hn-link" href="/features">功能</a>')
  && english['/features'].includes('<a class="hn-link" href="/features" aria-current="page">Features</a>'));

// ---- ★ 頁首（使用者 2026-10-17 選的 A 案）：上面只有公開的頁面，右邊是贊助、下載、頭像（沒登入是登入＋語言） ----
{
  const { NAV_ITEMS, NAV_GROUPS, ACCOUNT_ITEMS, navMarkup, panelNavMarkup, langLinks, tagOf } = await import('./site.js');
  const top = (html) => html.split('<body>')[1].split('<main id="main">')[0];
  const cells = (html) => [...html.matchAll(/<li class="hn-item[^"]*"([^>]*)>/g)].map((m) => m[1]);
  check('★ 選單清單只有五個公開的頁面，照這個順序', NAV_ITEMS.map((i) => i.key).join() === 'home,features,capes,translate,feedback'
    && NAV_ITEMS.every((i) => !i.role && !i.group && Number.isFinite(i.priority) && TEXT.zh[i.label] && TEXT.en[i.label])
    && Object.keys(NAV_GROUPS).length === 0);
  const flat = navMarkup('/capes');
  check('★ navMarkup：一頁一格、帶著順位，最後一格是藏著的「更多」', cells(flat).length === NAV_ITEMS.length + 1
    && NAV_ITEMS.every((i) => flat.includes('data-key="' + i.key + '" data-pri="' + i.priority + '"'))
    && /<li class="hn-item hn-more" id="hn-more" hidden><button class="hn-link hn-trig" id="hn-more-btn" type="button" aria-haspopup="true" aria-expanded="false" aria-controls="hn-more-menu">/.test(flat)
    && flat.includes('<div class="menu hn-menu" id="hn-more-menu" role="menu"></div>'));
  check('navMarkup：這一頁的那一格標著 aria-current，其他的沒有', flat.includes('<a class="hn-link" href="/capes" aria-current="page">')
    && (flat.match(/aria-current/g) || []).length === 1 && !navMarkup('/account').includes('aria-current'));
  check('★ 每一頁的頁首與手機面板都照這份清單畫', PATHS.every((path) => NAV_ITEMS.every((i) =>
    (top(pages[path]).match(new RegExp('class="hn-(?:p)?link" href="' + i.href + '"', 'g')) || []).length === 2)));
  // 群組：今天沒有，但機制要在。拿一份假的清單試
  const items = [...NAV_ITEMS.slice(0, 3), { key: 'translate', href: '/translate', label: 'nav.translate', priority: 70, group: 'community' },
    { key: 'feedback', href: '/feedback', label: 'nav.feedback', priority: 60, group: 'community' },
    { key: 'secret', href: '/secret', label: 'nav.secret', priority: 10, role: 'developer' }];
  const grouped = navMarkup('/feedback', items, { community: { label: 'nav.community', priority: 65 } });
  check('★ 群組：同一組的幾頁收成一個下拉選單，放在第一頁的位置，順位照群組的', cells(grouped).length === 6
    && /<li class="hn-item hn-group" data-key="community" data-pri="65"><button class="hn-link hn-trig hn-cur" type="button" aria-haspopup="true" aria-expanded="false" aria-controls="hn-g-community"><span>%%nav.community%%<\/span>/.test(grouped)
    && grouped.indexOf('data-key="capes"') < grouped.indexOf('data-key="community"')
    && grouped.indexOf('data-key="community"') < grouped.indexOf('data-key="secret"')
    && /id="hn-g-community" role="menu"><a href="\/translate" role="menuitem">%%nav.translate%%<\/a><a href="\/feedback" role="menuitem" aria-current="page">%%nav.feedback%%<\/a><\/div>/.test(grouped));
  check('群組沒寫順位：用組裡最大的那一個', navMarkup('/', items, { community: { label: 'nav.community' } }).includes('data-key="community" data-pri="70"'));
  const panelGrouped = panelNavMarkup('/feedback', items, { community: { label: 'nav.community' } });
  check('群組在手機面板裡是一個小標題加底下的幾頁', panelGrouped.includes('<div class="hn-pgroup"><p class="hn-phead">%%nav.community%%</p><a class="hn-plink hn-sub" href="/translate">')
    && panelGrouped.includes('<a class="hn-plink hn-sub" href="/feedback" aria-current="page">'));
  check('★ 身分限定：清單上寫了 role，畫出來就是藏著的、帶著 data-need', grouped.includes('data-key="secret" data-pri="10" data-need="developer" hidden>')
    && panelGrouped.includes('href="/secret" data-need="developer" hidden>'));
  check('★ 頭像選單：我的帳號、管理（只有 staff），清單一樣是資料', ACCOUNT_ITEMS.map((i) => i.key + ':' + (i.role || '')).join() === 'account:,admin:staff');
  for (const path of PATHS) {
    const head = top(pages[path]);
    const admin = [...head.matchAll(/<a[^>]*href="\/admin"[^>]*>/g)].map((m) => m[0]);
    check('★ ' + path + '：管理不在上面那一排，在頭像選單與手機面板裡，一律先藏著等 /api/me', admin.length === 2
      && admin.every((tag) => tag.includes('data-need="staff" hidden')) && !/<li class="hn-item[^>]*><a class="hn-link" href="\/admin"/.test(head));
    check(path + '：右邊的順序是贊助、下載、帳號（沒登入／登入）、漢堡選單', head.indexOf('hn-heart') > head.indexOf('id="hn"')
      && head.indexOf('hn-heart') < head.indexOf('id="get"') && head.indexOf('id="get"') < head.indexOf('id="hn-guest"')
      && head.indexOf('id="hn-guest"') < head.indexOf('id="hn-user"') && head.indexOf('id="hn-user"') < head.indexOf('id="hn-burger"'));
    check(path + '：舊的語言下拉、名字、登出鈕都不在了', !head.includes('<select') && !head.includes('id="me"') && !head.includes('id="nav-admin"'));
  }
  const zhTop = top(pages['/']);
  check('★ 贊助是一顆只有愛心的按鈕：有說明給螢幕閱讀器、滑過浮出「贊助」', zhTop.includes('<a class="hn-icon hn-heart hn-tip" href="https://ko-fi.com/lyuchacha" aria-label="在 Ko-fi 贊助" data-tip="贊助">')
    && top(english['/']).includes('aria-label="Sponsor on Ko-fi" data-tip="Sponsor"'));
  check('★ 下載還是那顆綠色的鈕，GitHub Releases 排第一、CurseForge 其次、沒有 Modrinth', /<button class="btn primary small hn-sheen" id="get" type="button" aria-haspopup="true" aria-expanded="false" aria-controls="get-menu">/.test(zhTop)
    && ['GitHub Releases', 'CurseForge'].every((n) => zhTop.includes('role="menuitem">' + n))
    && zhTop.indexOf('role="menuitem">GitHub Releases') < zhTop.indexOf('role="menuitem">CurseForge')
    && !zhTop.includes('role="menuitem">Modrinth'));
  check('★ 沒登入的樣子：登入＋地球（語言），頭像那一塊藏著', /<div class="hn-guest hn-desk" id="hn-guest"><a class="btn small hn-signin" href="\/account">登入<\/a>/.test(zhTop)
    && zhTop.includes('<button class="hn-icon hn-tip" id="hn-globe" type="button" aria-label="語言" data-tip="語言" aria-haspopup="true" aria-expanded="false" aria-controls="hn-globe-menu">')
    && zhTop.includes('<div class="pop hn-user hn-desk" id="hn-user" hidden>') && zhTop.includes('<div class="hn-pgrid" id="hn-p-user" hidden>')
    && !/id="hn-guest"[^>]*hidden/.test(zhTop) && !/id="hn-p-guest"[^>]*hidden/.test(zhTop));
  check('★ 頭像選單：名字、我的帳號、管理、語言、分隔線、登出，照這個順序', (() => {
    const menu = zhTop.slice(zhTop.indexOf('id="hn-acct-menu"'), zhTop.indexOf('id="hn-burger"'));
    const at = ['data-me-name', '我的帳號', '>管理<', 'id="hn-lang-toggle"', 'class="hn-sep" role="separator"', '登出'].map((w) => menu.indexOf(w));
    return at.every((n, i) => n > 0 && (i === 0 || n > at[i - 1])) && menu.includes('aria-controls="hn-acct-langs"');
  })());
  check('★ 這一頁在頭像選單裡（帳號、管理）：頭像亮著', top(pages['/account']).includes('<button class="hn-avatar hn-cur"')
    && top(pages['/admin']).includes('<button class="hn-avatar hn-cur"') && !zhTop.includes('hn-avatar hn-cur')
    && /<a role="menuitem" class="hn-mi" href="\/admin" aria-current="page" data-need="staff" hidden>/.test(top(pages['/admin'])));
  check('手機：漢堡選單打開面板（選單、下載、帳號、語言）', zhTop.includes('<button class="hn-icon hn-burger" id="hn-burger" type="button" aria-label="選單" aria-expanded="false" aria-controls="hn-panel">')
    && (() => {
      const panel = zhTop.slice(zhTop.indexOf('<div class="hn-panel" id="hn-panel">'));
      const at = ['class="hn-pnav"', '>下載<', 'id="hn-p-guest"', '>語言<'].map((w) => panel.indexOf(w));
      return at.every((n, i) => n > 0 && (i === 0 || n > at[i - 1]));
    })());
  // 語言：八種都在三個地方（地球、頭像選單、手機面板），連結是 ?lang=，程式攔下來寫 cookie
  for (const [lang, html] of [['zh', pages['/']], ['en', english['/']], ['ja', await (await inLang('/capes?lang=ja', {})).text()]]) {
    const head = top(html);
    check('★ ' + lang + '：語言連結八種各三份，現在這一種在選單裡打勾、在面板裡標成目前的',
      WANTED.every((code) => (head.match(new RegExp('<a href="\\?lang=' + code + '" hreflang="' + tagOf(code) + '" lang="' + tagOf(code) + '" data-lang="' + code + '"', 'g')) || []).length === 3)
      && (head.match(new RegExp('data-lang="' + lang + '" role="menuitemradio" aria-checked="true"', 'g')) || []).length === 2
      && (head.match(/aria-checked="true"/g) || []).length === 2 && (head.match(/aria-checked="false"/g) || []).length === 14
      && head.includes('data-lang="' + lang + '" aria-current="true">')
      && ['繁體中文', '简体中文', 'English', '日本語', '한국어', 'Русский', 'Español', 'Türkçe'].every((n) => (head.match(new RegExp('>' + n + '(?:<svg|</a>)', 'g')) || []).length === 3));
  }
  check('langLinks：在選單裡是 menuitemradio，在面板裡是一般連結', langLinks('ko', true).includes('data-lang="ko" role="menuitemradio" aria-checked="true">한국어<svg')
    && !langLinks('ko', false).includes('role=') && langLinks('ko', false).includes('data-lang="ko" aria-current="true">'));
  check('★ 換語言還是寫 lang 這個 cookie、拿掉網址上的 ?lang=', pages['/'].includes("document.cookie = 'lang=' + link.dataset.lang + '; path=/; max-age=31536000; samesite=lax';")
    && pages['/'].includes('location.replace(location.pathname);'));
  check('★ 新的字八種語言都有', ['top.signin', 'top.more', 'top.myaccount', 'top.account', 'top.menu'].every((k) => LANGS.every((l) => TEXT[l][k]))
    && new Set(LANGS.map((l) => TEXT[l]['top.more'])).size >= 6);

  // 頭像那一塊的程式：拿頁面上那一段 headerAccount 出來，在假的 document 上跑（沒登入、還在問、一般玩家、管理員、只有 Discord）
  const sandbox = (html) => {
    const head = top(html);
    const from = html.indexOf('function headerAccount(me) {');
    const end = html.indexOf('    fitNav(true);\n  }', from) + '    fitNav(true);\n  }'.length;
    const byId = {};
    for (const m of head.matchAll(/<(\w+)([^>]*\sid="([^"]+)"[^>]*)>/g)) {
      byId[m[3]] = { hidden: /\shidden(?=\s|$)/.test(m[2]), attrs: {}, title: '',
        dataset: { label: (m[2].match(/data-label="([^"]*)"/) || [])[1] }, setAttribute(k, v) { this.attrs[k] = v; } };
    }
    const needs = [...head.matchAll(/<(\w+)([^>]*\sdata-need="(\w+)"[^>]*)>/g)].map((m) => ({ hidden: true, dataset: { need: m[3] } }));
    const names = [...head.matchAll(/data-me-name/g)].map(() => ({ textContent: '' }));
    const imgs = [...head.matchAll(/<img class="hn-ava"/g)].map(() => ({ hidden: true, src: '', nextElementSibling: { hidden: false } }));
    const document = {
      getElementById: (id) => byId[id],
      querySelectorAll: (sel) => (sel.includes('data-need') ? needs : sel.includes('data-me-name') ? names : sel.includes('img.hn-ava') ? imgs : []),
    };
    const state = { fits: 0 };
    state.run = new Function('document', 'fitNav', 'head', html.slice(from, end) + '\nreturn headerAccount;')(
      document, () => state.fits++, (name, size) => 'https://minotar.net/helm/' + name + '/' + size + '.png');
    return Object.assign(state, { byId, needs, names, imgs });
  };
  const shown = (s) => ['hn-guest', 'hn-p-guest', 'hn-user', 'hn-p-user'].filter((id) => !s.byId[id].hidden).join();
  let s = sandbox(pages['/']);
  check('★ 頁上有 headerAccount、找得到四塊與管理', typeof s.run === 'function'
    && ['hn-guest', 'hn-p-guest', 'hn-user', 'hn-p-user', 'hn-avatar'].every((id) => s.byId[id])
    && s.needs.length === 2 && s.names.length === 2 && s.imgs.length === 3);
  s.run(null);
  check('★ 沒登入：登入與地球在、頭像不在、管理藏著', shown(s) === 'hn-guest,hn-p-guest' && s.needs.every((n) => n.hidden) && s.fits === 1);
  s = sandbox(pages['/']);
  s.run(true);
  check('有通行證、還在問 /api/me：先換成頭像，管理還是藏著', shown(s) === 'hn-user,hn-p-user' && s.needs.every((n) => n.hidden)
    && s.names.every((n) => n.textContent === ''));
  s.run({ linked: true, name: 'Bob', staff: false });
  check('★ 一般玩家：頭像與名字換上來，管理藏著', shown(s) === 'hn-user,hn-p-user' && s.needs.every((n) => n.hidden)
    && s.names.every((n) => n.textContent === 'Bob') && s.imgs.every((img) => img.src === 'https://minotar.net/helm/Bob/52.png')
    && s.byId['hn-avatar'].attrs['aria-label'] === '帳號選單: Bob' && s.byId['hn-avatar'].title === 'Bob');
  s.run({ linked: true, name: 'Alice_Dev', staff: true, developer: true });
  check('★ 開發者與管理員（staff）：管理打開', s.needs.every((n) => !n.hidden));
  s = sandbox(english['/admin']);
  s.run({ linked: false, discord: { name: 'Sayla' }, staff: true });
  check('★ 只有 Discord、還沒綁的管理員：名字用 Discord 的、沒有頭像圖、管理一樣打開', shown(s) === 'hn-user,hn-p-user'
    && s.names.every((n) => n.textContent === 'Sayla') && s.imgs.every((img) => img.src === '') && s.needs.every((n) => !n.hidden)
    && s.byId['hn-avatar'].attrs['aria-label'] === 'Account menu: Sayla');
  s.run(null);
  check('通行證過期（/api/me 回 null）：換回登入', shown(s) === 'hn-guest,hn-p-guest' && s.needs.every((n) => n.hidden));

  // 動作與葉子（使用者 2026-10-17：「更順、更流暢，加一點巧思，像小光澤或飄過的葉子」）
  const css = (pages['/'].match(/<style>([\s\S]*?)<\/style>/) || [])[1] || '';
  const calm = (css.match(/@media \(prefers-reduced-motion: reduce\) \{([\s\S]*?)\n  \}/) || [])[1] || '';
  check('★ 選單打開 180–200ms、關掉 120ms，從按鈕那一角長出來，只動 transform 與 opacity',
    /\.menu \{[^}]*transform: translateY\(-6px\) scale\(\.96\); transform-origin: top right;[^}]*transition: opacity 120ms var\(--exit\), transform 120ms var\(--exit\)/.test(css)
    && /\.menu\.open \{ opacity: 1; visibility: visible; transform: none; transition: opacity 180ms var\(--out\), transform 200ms var\(--out\)/.test(css)
    && css.includes('.hn .menu { left: 0; right: auto; width: auto; min-width: 190px; transform-origin: top left; }'));
  check('★ 選單裡一項接一項淡進來（只在打開時、相隔 18ms），序號是程式填的',
    css.includes('calc(var(--i, 0) * 18ms)') && pages['/'].includes("child.style.setProperty('--i', Math.min(i, 10))")
    && pages['/'].includes('menus.number(moreMenu);'));
  check('手機面板是滑下來的（transition，不是一下子冒出來）', /\.hn-panel\.open \{ opacity: 1; visibility: visible; transform: none; transition: opacity 200ms var\(--out\), transform 260ms var\(--out\)/.test(css)
    && !css.includes('hn-drop'));
  check('★ 下載鈕的光只在滑上去時掃一次、不循環', /\.hn-sheen:hover::after \{ transform: translateX\(340%\) skewX\(-18deg\); transition: transform 680ms var\(--out\); \}/.test(css)
    && !/\.hn-sheen[^{]*\{[^}]*animation/.test(css) && !/\.hn-sheen::after \{[^}]*transition/.test(css));
  check('★ 頁首的葉子：畫布在字的後面、不擋滑鼠、像素放大不糊', zhTop.includes('<div class="top"><canvas class="hn-leaves" id="hn-leaves" aria-hidden="true"></canvas><div class="in">')
    && /\.hn-leaves \{[^}]*z-index: 0;[^}]*pointer-events: none; image-rendering: pixelated;/.test(css) && css.includes('.top .in { position: relative; z-index: 1;'));
  check('★ 減少動態：葉子與光都不出現，程式也不跑', /\.hn-sheen::after, \.hn-leaves \{ display: none; \}/.test(calm)
    && pages['/'].includes("window.matchMedia('(prefers-reduced-motion: reduce)')") && pages['/'].includes('if (off()) return;'));
  check('★ 葉子：分頁藏起來就停、沒東西在畫面上就不跑 rAF、轉向只用 90 度的倍數（沒有任意角度的旋轉）',
    pages['/'].includes("document.addEventListener('visibilitychange', function () {\n      if (document.hidden) stop();")
    && pages['/'].includes('raf = leaves.length || gusts.length ? requestAnimationFrame(frame) : 0;')
    && !/ctx\.rotate|\.rotate\(/.test(pages['/'].slice(pages['/'].indexOf('頁首飄過的葉子'))));
  check('地球也用自己的浮出提示，選單開著不出來', css.includes('.hn-tip[aria-expanded="true"]::after { opacity: 0 !important;'));
  // 葉子的程式本身：拿頁面上那一段在假的瀏覽器裡跑（減少動態、分頁藏起來、正常三種）
  const leafRun = (calmOn, hidden) => {
    const html = pages['/'];
    const from = html.indexOf('  (function () {\n    var canvas = document.getElementById(\'hn-leaves\');');
    const end = html.indexOf('\n  })();\n', from) + '\n  })();\n'.length;
    const calls = { timers: 0, frames: 0, listeners: [] };
    const ctx2d = { clearRect() {}, fillRect() {}, drawImage() {}, globalAlpha: 1, fillStyle: '' };
    const bar = { offsetHeight: 61 };
    const canvas = { getContext: () => ctx2d, style: {}, width: 0, height: 0,
      parentNode: { clientWidth: 1266, querySelector: () => bar } };
    const doc = {
      hidden,
      getElementById: (id) => (id === 'hn-leaves' ? canvas : null),
      createElement: () => ({ width: 0, height: 0, getContext: () => ctx2d }),
      addEventListener: (type) => calls.listeners.push(type),
    };
    const win = { matchMedia: () => ({ matches: calmOn, addEventListener() {} }), addEventListener() {} };
    new Function('document', 'window', 'setTimeout', 'clearTimeout', 'requestAnimationFrame', 'cancelAnimationFrame', 'performance', html.slice(from, end))(
      doc, win, () => ++calls.timers, () => {}, () => ++calls.frames, () => {}, { now: () => 0 });
    return { calls, canvas };
  };
  let run = leafRun(true, false);
  run.canvas.hnBlow();
  check('★ 減少動態：葉子的程式不排任何計時、吹了也不動', run.calls.timers === 0 && run.calls.frames === 0);
  run = leafRun(false, true);
  run.canvas.hnBlow();
  check('★ 分頁藏著：吹了也不動（不跑 requestAnimationFrame）', run.calls.frames === 0 && run.calls.listeners.includes('visibilitychange'));
  run = leafRun(false, false);
  check('正常：排一陣風，畫布照頁首量好大小（一格像素＝2px）', run.calls.timers === 1 && run.calls.frames === 0
    && run.canvas.width === 633 && run.canvas.height === 30 && run.canvas.style.width === '1266px' && run.canvas.style.height === '60px');
  run.canvas.hnBlow();
  check('正常：吹一陣才開始跑 requestAnimationFrame', run.calls.frames === 1);
  check('按下去縮一點、箭頭打開時轉過來', css.includes('.hn-link:active, .hn-icon:active, .hn-avatar:active, .hn-plink:active { transform: scale(.97); }')
    && css.includes('#hn-globe[aria-expanded="true"] > .ico:last-child { transform: rotate(180deg); }'));
}
check('★ 首頁有團隊那一段：三個身分各一區，名單跟 /api/team 要', ['band-developers', 'band-sponsors', 'band-contributors']
  .every((id) => pages['/'].includes('id="' + id + '"')) && pages['/'].includes("fetch('/api/team')")
  && pages['/'].includes('id="team"') && pages['/'].includes('id="install"') && pages['/'].includes('id="sponsoring"'));
check('★ 頭像從 minotar 來，網頁的安全規則也放行它（不再用 mc-heads）',
  pages['/'].includes('https://minotar.net/helm/') && !pages['/'].includes('mc-heads.net')
  && (await inLang('/', {})).headers.get('content-security-policy').includes('https://minotar.net'));
check('★ 團隊的每個人、每個公會都連到 Wynncraft 上他自己的頁面，另開分頁',
  pages['/'].includes("'https://wynncraft.com/stats/player/' + encodeURIComponent(member.mc)")
  && pages['/'].includes("'https://wynncraft.com/stats/guild/' + encodeURIComponent(guild.name)")
  && pages['/'].includes("link.rel = 'noopener'") && pages['/'].includes('id="band-guilds"'));
// 功能頁的即時預覽：選項的名稱與場景裡的譯文都是從模組的語言檔與語料產生的（demo-data.js）
{
  const { DEMO } = await import('./demo-data.js');
  check('★ 面板間距：滑桿 12–200，預覽照遊戲比例（一格 .1em、量的是兩個框線之間），太寬就整張縮小', pages['/features'].includes('input.min = 12;')
    && pages['/features'].includes("gap.style.width = Math.max(0, state.gap - insetLeft - FRAME_INSET.plain) / 10 + 'em'")
    && pages['/features'].includes('function zoomToFit()') && !pages['/features'].includes('state.gap * (wide ? 0.25 : 0.06)')
    && DEMO.ui.zh['panel.gap.hint'].includes('12–200'));
  check('★ 八種語言的隱私政策與帳號頁都改成「刪除我的資料」按鈕與「恢復上傳」，不再提舊的開關', LANGS.every((lang) => {
    const text = TEXT[lang]['privacy.body'] + TEXT[lang]['account.del.list'];
    return !/不上傳我的資料|不上传我的数据|Don't upload my data|自分のデータを送らない|내 데이터 보내지 않기|Не отправлять мои данные|No subir mis datos|Verilerimi gönderme/.test(text)
      && /恢復上傳|恢复上传|Resume uploads|アップロードを再開|업로드 다시 시작|Возобновить отправку|Reanudar la subida|Yüklemeyi sürdür/.test(text);
  }));
  check('★ 服務條款講了第一次會顯示使用須知；功能頁講了同意連線功能才會報到', LANGS.every((lang) => /使用須知|使用须知|Before you play|ご利用にあたって|이용 안내|Перед игрой|Antes de jugar|Oynamadan önce/.test(TEXT[lang]['terms.body'])
    && /同意連線功能|同意联网功能|agree to online features|オンライン機能に同意|온라인 기능에 동의|согласились на сетевые функции|aceptes las funciones en línea|çevrimiçi özelliklere izin/.test(TEXT[lang]['feat.s3b'])));

  const embedded = (html) => JSON.parse((html.match(/<script type="application\/json" id="demo-data">(\{.*?\})<\/script>/s) || [])[1] || '{}');
  const zh = embedded(pages['/features']);
  const en = embedded(english['/features']);
  check('★ 功能頁有即時預覽：三個分頁、七種譯文語言，分四段', ['items', 'dialogue', 'world']
    .every((tab) => pages['/features'].includes('data-tab="' + tab + '"'))
    && (pages['/features'].match(/<option value="[a-z]{2}_[a-z]{2}"/g) || []).length === 7
    && ['id="translate"', 'id="tools"', 'id="social"', 'id="compat"'].every((w) => pages['/features'].includes(w)));
  check('★ 預覽的選項名稱照這一頁的語言（就是遊戲裡設定畫面的字）', zh.ui['items.tooltip'] === '物品翻譯'
    && zh.ui['mode.replace'] === '就地取代' && en.ui['items.tooltip'] === 'Item translation'
    && en.ui['mode.replace'] === 'Replace in place');
  check('★ 預覽裡的譯文七種語言都帶著，每一條都有字（簡中刻意留英文的名稱除外）', Object.keys(zh.text).length === 7
    && Object.entries(zh.text).every(([code, lang]) => Object.keys(lang).length === 34
      && Object.entries(lang).every(([src, v]) => v.length > 0 || englishOnPurpose(code, src)))
    && zh.text.zh_tw['Weapon Merchant'] === '武器商人' && zh.text.ja_jp.Yes === 'はい');
  // 預覽是一整面「遊戲畫面」：功能頁與首頁最上面用同一份程式，字是遊戲裡那一套點陣字
  {
    const { FONTS } = await import('./demo-fonts.js');
    check('★ 功能頁與首頁都有那一面遊戲畫面（同一份 makeStage）', ['/features', '/'].every((p) =>
      pages[p].includes('function makeStage(') && pages[p].includes('id="stage-parts"') && pages[p].includes('class="screen"'))
      && pages['/'].includes('id="hero-stage"') && pages['/features'].includes('id="play-stage"')
      && !pages['/capes'].includes('function makeStage('));
    check('首頁的語言是七顆按鈕，各自標著要換成哪一種譯文', (pages['/'].match(/class="chip" data-lang="[a-z]{2}_[a-z]{2}"/g) || []).length === 7
      && Object.keys(embedded(pages['/']).text).length === 7);
    check('四份點陣字都宣告了，網址帶著內容的雜湊', Object.keys(FONTS).join() === 'tc,sc,ja,ko'
      && Object.keys(FONTS).every((code) => pages['/features'].includes('/font/px-' + code + '.ttf?v=' + FONTS[code].v)));
    // 場景裡每一個字，對應的那一份字型都要有（勾與叉是網頁自己畫的）
    const need = { tc: ['zh_tw', 'ru_ru', 'es_es', 'tr_tr'], sc: ['zh_cn'], ja: ['ja_jp'], ko: ['ko_kr'] };
    const DRAWN = '✔✖✓\n';                 // 勾與叉是網頁畫的；換行是名牌那一段的分行，不是字
    const missing = [];
    for (const code of Object.keys(need)) {
      for (const lang of need[code]) {
        for (const value of Object.values(DEMO.text[lang])) {
          for (const ch of value) if (!DRAWN.includes(ch) && !FONTS[code].covers.includes(ch)) missing.push(code + ':' + ch);
        }
      }
      for (const src of Object.keys(DEMO.text.zh_tw)) {
        for (const ch of src) if (!DRAWN.includes(ch) && !FONTS[code].covers.includes(ch)) missing.push(code + ':' + ch);
      }
    }
    check('★ 場景裡的每一個字，字型裡都有' + (missing.length ? '（缺 ' + [...new Set(missing)].join(' ') + '）' : ''), missing.length === 0);
    r = await worker.fetch(new Request('https://x.test/font/px-tc.ttf?v=' + FONTS.tc.v), env);
    const bytes = new Uint8Array(await r.arrayBuffer());
    check('★ 字型拿得到：是 TrueType、可以長期快取', r.status === 200 && r.headers.get('content-type') === 'font/ttf'
      && r.headers.get('cache-control').includes('immutable') && bytes.length > 5000
      && bytes[0] === 0 && bytes[1] === 1 && bytes[2] === 0 && bytes[3] === 0);
    check('裁出來的字型留著版權與授權（OFL 的要求）', new TextDecoder('utf-16be').decode(bytes).includes('TakWolf')
      || new TextDecoder('latin1').decode(bytes).includes('TakWolf'));
    check('沒有這一份的字型：404', (await worker.fetch(new Request('https://x.test/font/px-xx.ttf'), env)).status === 404
      && (await worker.fetch(new Request('https://x.test/font/../worker.js'), env)).status === 404);
  }
  // 預覽用的是遊戲的材質（demo-textures.js），披風範本從自己的網址下載（cape-templates.js）
  {
    const { TEXTURES } = await import('./demo-textures.js');
    const { CAPE_TEMPLATES } = await import('./cape-templates.js');
    const need = ['tip-rare.png', 'tip-normal.png', 'tip-ingredient.png', 'tip-plain.png', 'emblem-bow.png', 'emblem-ingredient.png',
      'req.png', 'mark.png', 'divider.png', 'slots.png', 'item-oakbow.png', 'item-flesh.png', 'item-pouch.png',
      'prof-weaponsmithing.png', 'prof-woodworking.png',
      'dialogue.png', 'choice.png', 'cursor.png', 'hud.png', 'inventory.png', 'glyphs.png', 'item-bow.png',
      'bg-world.webp', 'bg-street.webp', 'skin.png'];
    check('★ 預覽要的材質都在', need.every((name) => Object.hasOwn(TEXTURES, name)));
    check('★ 每一張材質頁面上都取得到（樣式的 --t-*、程式的 D.tex），網址帶著雜湊', Object.keys(TEXTURES).every((name) =>
      pages['/features'].includes('--t-' + name.replace(/\.\w+$/, '') + ': url(/tex/' + name + '?v=' + TEXTURES[name].v + ')'))
      && Object.keys(embedded(pages['/features']).tex).length === Object.keys(TEXTURES).length
      && embedded(pages['/']).tex['hud.png'] === '/tex/hud.png?v=' + TEXTURES['hud.png'].v);
    r = await worker.fetch(new Request('https://x.test/tex/hud.png?v=' + TEXTURES['hud.png'].v), env);
    let bytes = new Uint8Array(await r.arrayBuffer());
    check('★ 材質拿得到：是 PNG、可以長期快取', r.status === 200 && r.headers.get('content-type') === 'image/png'
      && r.headers.get('cache-control').includes('immutable') && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47);
    r = await worker.fetch(new Request('https://x.test/tex/bg-world.webp'), env);
    check('背景是 WebP', r.status === 200 && r.headers.get('content-type') === 'image/webp');
    check('沒有的材質：404', (await worker.fetch(new Request('https://x.test/tex/nope.png'), env)).status === 404
      && (await worker.fetch(new Request('https://x.test/tex/__proto__'), env)).status === 404);
    check('★ 預覽不再自己畫風景與物品（程式裡沒有那幾支了）', !pages['/features'].includes('paintScene')
      && !pages['/features'].includes('paintIcon') && pages['/features'].includes("emblem: ['emblem-bow.png'"));
    check('★ 頁尾寫了材質是 Wynncraft 的', pages['/'].includes(TEXT.zh['foot.assets']) && english['/'].includes(TEXT.en['foot.assets']));
    // 背包裡的人、對話框與快捷列的上下、街上的 NPC 與怪物的等級（使用者 2026-10-14 看了預覽之後要的）
    const skin = new Uint8Array(await (await worker.fetch(new Request('https://x.test/tex/skin.png'), env)).arrayBuffer());
    check('★ 背包裡有一個人：皮膚是 64×64 的 PNG，貼在方塊上、會看著滑鼠', skin[1] === 0x50 && skin[19] === 64 && skin[23] === 64
      && pages['/features'].includes('--t-skin: url(/tex/skin.png?v=') && pages['/features'].includes('makeDoll(3.5)')
      && pages['/features'].includes("addEventListener('pointermove'") && pages['/'].includes('makeDoll(3.5)'));
    check('★ 人站在人物格裡面：超出格子的部分裁掉（轉頭時頭不會蓋到木框上）', /\.dollclip \{[^}]*overflow: hidden/.test(pages['/features'])
      && pages['/features'].includes('dollClip.append(dollBox)'));
    {
      // 背包裡可以指的幾樣東西：每一樣的每一行字，七種語言的語料都要有（不然那一行會留英文）
      const js = pages['/features'];
      const table = js.slice(js.indexOf('var ITEMS = {'), js.indexOf('var ITEM_ORDER'));
      const used = [...table.matchAll(/'((?:[A-Z{]|\{#\})[^']*)'/g)].map((m) => m[1])
        .filter((s) => !/^[A-Z★]+$/.test(s) && !/\.png$/.test(s));
      const also = ['{~} DPS', '{#}{~} Crafting Level', '{#}Can be used in recipes for', '{~} to {~}'];
      const missing = [];
      for (const src of [...used, ...also]) {
        for (const [code, lang] of Object.entries(DEMO.text)) if (!lang[src] && !englishOnPurpose(code, src)) missing.push(code + ':' + src);
      }
      check('★ 背包裡三樣東西可以指（滑鼠、點、鍵盤），每一行字七種語言都有' + (missing.length ? '（缺 ' + missing.join('、') + '）' : ''),
        missing.length === 0 && used.includes('Flawless Oak Bow') && used.includes('Rotten Flesh') && used.includes('Bony Bow')
        && js.includes("var ITEM_ORDER = ['bony', 'oak', 'flesh']") && js.includes("button.addEventListener('pointerenter', pick)")
        && js.includes("button.addEventListener('focus', pick)") && js.includes("el('button', 'slot')"));
      check('★ 沒穿裝備的格子有影子：四件防具、兩個戒指、手環、項鍊', (js.slice(js.indexOf('function drawItems'), js.indexOf('function drawBar')).match(/\[\d+, \d+, \d\]/g) || []).length === 8
        && js.includes('var(--t-slots)'));
      check('★ 說明框的字離框的外緣 12 格（遊戲的規格），物品那一面的框照內容的寬度、有上下限', /\n  \.tip \{[^}]*padding: 1\.2em 1\.2em/.test(js)
        && /\.duo \.tip \{[^}]*width: max-content; min-width: [\d.]+em; max-width: [\d.]+em/.test(js));
      check('★ 首頁與功能頁往下捲有淡進來的動畫；減少動態的設定下不做、一開始就看得到的不動', pages['/'].includes('<script>revealOnScroll("#main > h2')
        && pages['/features'].includes('<script>revealOnScroll("section[id] > h2, .cards > .fcard")</script>')
        && js.includes("matchMedia('(prefers-reduced-motion: reduce)').matches) return;") && js.includes('return node.getBoundingClientRect().top > fold;')
        && !pages['/capes'].includes('<script>revealOnScroll('));
      check('★ 人物的等級是六角形上描邊的白字，不會被怪物等級那顆灰框的樣式蓋到', /\.hudbar \.lv \{[^}]*color: #fff/.test(js)
        && !/\.scr \.lv\b/.test(js) && js.includes('.scr .mlv {'));
    }
    check('★ 街上的人是截圖裡真的 NPC：不再自己畫一個，名牌的兩行七種語言都有', !pages['/features'].includes('paintFigure')
      && pages['/features'].includes('var(--t-bg-street)')
      && Object.values(DEMO.text).every((lang) => {
        const lines = (lang['{#}\nParty Finder\nFind a party of queue for a raid'] || '').split('\n');
        return lines.length === 3 && lines[0] === '{#}' && lines[1] && lines[2];
      }));
    check('★ 怪物的等級是名字後面的小方框，跟人物的等級是同一帶的', pages['/features'].includes("el('b', 'mlv', 'LV 13')")
      && !pages['/features'].includes('[Lv. 3]') && Object.entries(DEMO.text).every(([code, lang]) => /\S \{#\}\{#\}$/.test(lang['Zombie {#}{#}'] || '') || englishOnPurpose(code, 'Zombie {#}{#}')));
    {
      const js = pages['/features'];
      const dialogue = js.slice(js.indexOf('function drawDialogue'), js.indexOf('function drawWorld'));
      check('★ 對話：快捷列畫在對話框之後（在它上面），對話框整個在那一排的上方', dialogue.indexOf('hud.append(stack)') > 0
        && dialogue.indexOf('drawBar(null)') > dialogue.indexOf('hud.append(stack)')
        && /\.dlg \{[^}]*bottom: 96px/.test(js) && /\.option \{[^}]*line-height: 22px/.test(js));
    }

    // 披風範本：披風一覽與帳號頁都有一塊，每一種尺寸一顆下載鈕
    const sizes = ['22x17', '44x34', '88x68', '176x136', '352x272', '704x544', '1056x816'];
    check('★ 範本每一種尺寸、兩份說明圖與一張示範都打包了', sizes.every((s) => Object.hasOwn(CAPE_TEMPLATES, 'template-' + s + '.png'))
      && ['guide.png', 'guide.en.png', 'example-88x68.gif'].every((n) => Object.hasOwn(CAPE_TEMPLATES, n)));
    check('★ 披風一覽與帳號頁都有範本那一塊：七顆下載鈕，檔名看得出是什麼', ['/capes', '/account'].every((p) =>
      sizes.every((s) => pages[p].includes('href="/template/template-' + s + '.png?v=' + CAPE_TEMPLATES['template-' + s + '.png'].v
        + '" download="wynnchayuan-cape-template-' + s + '.png"'))
      && pages[p].includes('class="tpl"') && pages[p].includes(TEXT.zh['tpl.h'])));
    check('披風一覽那一塊平常收著（details），帶著 #template 進來會打開', pages['/capes'].includes('<details class="make" id="template">')
      && pages['/capes'].includes("location.hash === '#template'"));
    check('★ 說明圖：中文的頁面用中文那一張，其他語言用英文那一張', pages['/capes'].includes('/template/guide.png?v=')
      && english['/capes'].includes('/template/guide.en.png?v=')
      && (await (await inLang('/capes?lang=zh-cn', {})).text()).includes('/template/guide.png?v=')
      && (await (await inLang('/capes?lang=ja', {})).text()).includes('/template/guide.en.png?v='));
    r = await worker.fetch(new Request('https://x.test/template/template-88x68.png'), env);
    bytes = new Uint8Array(await r.arrayBuffer());
    check('★ 範本下載得到：就是 capes/templates/ 裡那一張', r.status === 200 && r.headers.get('content-type') === 'image/png'
      && Buffer.from(bytes).equals((await import('node:fs')).readFileSync(new URL('../../capes/templates/template-88x68.png', import.meta.url))));
    check('示範是 GIF；沒有的檔 404', (await worker.fetch(new Request('https://x.test/template/example-88x68.gif'), env))
      .headers.get('content-type') === 'image/gif'
      && (await worker.fetch(new Request('https://x.test/template/README.md'), env)).status === 404);
    // 功能頁下半：每一項一張卡，翻譯範圍那十張各有一句例子
    check('★ 功能頁的卡片：翻譯範圍十張都有例句，例句在預覽的資料裡找得到', (pages['/features'].match(/data-eg="/g) || []).length === 10
      && [...pages['/features'].matchAll(/data-eg="([^"]+)"/g)].every((m) => Object.hasOwn(DEMO.text.zh_tw, m[1])
        && Object.entries(DEMO.text).every(([code, lang]) => lang[m[1]] || englishOnPurpose(code, m[1]))));
  }
  // 找不到這一頁：一頁像樣的網頁，照訪客的語言
  {
    r = await worker.fetch(new Request('https://x.test/no-such-page', { headers: { 'accept-language': 'zh-TW' } }), env);
    const lost = await r.text();
    check('★ 找不到的網址：404，但是一頁有頁首與回去的路的網頁', r.status === 404
      && r.headers.get('content-type').startsWith('text/html') && lost.includes('class="lost"')
      && lost.includes('找不到這一頁') && lost.includes('href="/features"') && lost.includes('class="brand"')
      && !/%%[\w.]+%%/.test(lost));
    r = await worker.fetch(new Request('https://x.test/no-such-page', { headers: { 'accept-language': 'en-US' } }), env);
    check('英文的訪客看到英文的', (await r.text()).includes('This page does not exist'));
    check('不是 GET、或是 /api 底下的照舊回 JSON', (await worker.fetch(new Request('https://x.test/nope', { method: 'POST' }), env))
      .headers.get('content-type').includes('json')
      && (await worker.fetch(api('/nope'), env)).headers.get('content-type').includes('json'));
    check('★ 每一頁第一個可以聚焦的是「跳到主要內容」', PATHS.every((p) => pages[p].includes('<a class="skip" href="#main">')
      && pages[p].includes('<main id="main">')));
  }
  check('預覽用到的每一個選項，八種語言都有名稱與說明', Object.keys(DEMO.ui).join() === 'zh,zh-cn,en,ja,ko,ru,es,tr'
    && Object.values(DEMO.ui).every((lang) => Object.values(lang).every((v) => typeof v === 'string' && v.length > 0)
      && ['items.tooltip', 'world.tracker', 'dialogue.choices'].every((k) => lang[k] && lang[k + '.hint'])));
  check('數字的位置（{~}、{~2}）每一種語言都填得回去', Object.values(DEMO.text).every((lang) =>
    (lang['- Loot Chests T{~}+: {~}/{~}'].match(/\{~\d?\}/g) || []).length === 3
    && (lang['- Mobs slain: {~}/{~}'].match(/\{~\d?\}/g) || []).length === 2));
  check('英文版的功能頁：預覽以外看得到的字裡沒有中文', !/[㐀-鿿]/.test(english['/features']
    .replace(/<script[\s\S]*?<\/script>/g, ' ').replace(/<select class="lang"[\s\S]*?<\/select>/g, ' ').replace(/<div class="hn-langs[^"]*"[\s\S]*?<\/div>/g, ' ').replace(/<style[\s\S]*?<\/style>/g, ' ')
    .replace(/<[^>]+>/g, ' ')));
}
check('功能頁的列都填了字（27 列）', (pages['/features'].match(/class="fcard/g) || []).length === 27
  && !/<dd>\s*<\/dd>/.test(pages['/features']));
check('teamOf：只留三個身分，欄位只有名字、遊戲 ID、顏色', JSON.stringify(teamOf([
  { key: 'credits.role.developers', color: '#FFD24A', members: [{ name: ' A ', mc: 'Alice_Dev', rainbow: true, cape: 'x.gif' }] },
  { key: 'credits.role.sponsors', color: 'red', members: [{ name: 'C', mc: 'bad name!', color: '#123abc' }, { mc: 'NoName' }] },
  { key: 'credits.role.sources', members: [{ name: 'Wynncraft' }] },
  { key: 'credits.role.contributors', members: 'oops' }]))
  === JSON.stringify([{ role: 'developers', color: '#FFD24A', members: [{ name: 'A', mc: 'Alice_Dev' }] },
    { role: 'sponsors', color: null, members: [{ name: 'C', color: '#123abc' }] }]) && teamOf(null).length === 0);
r = await worker.fetch(api('/team'), env);
body = await r.json();
check('★ /api/team：公開、可以快取，照名單回開發者與贊助者', r.status === 200
  && /max-age=\d+/.test(r.headers.get('cache-control')) && body.team.map((s) => s.role).join() === 'developers,sponsors'
  && body.team[0].members.some((m) => m.mc === 'Alice_Dev') && body.team[1].members[0].mc === 'Carol');
check('★ /api/team 帶著翻譯團隊與貢獻項目：只留認得的，照固定順序',
  JSON.stringify(body.team[0].members[0].teams) === '["zh_tw","ja_jp"]' && JSON.stringify(body.team[0].members[0].areas) === '["code"]'
  && !('teams' in body.team[1].members[0]) && !('areas' in body.team[1].members[0]));
check('★ teamOf：teams 與 areas 不是陣列或是空的就不帶這兩欄', JSON.stringify(teamOf([{ key: 'credits.role.contributors', members: [
  { name: 'P', teams: ['ja_jp', 'zh_tw', 'xx', 3, 'ja_jp'], areas: 'code' }, { name: 'Q', teams: [], areas: ['testing', 'nope'] }] }]))
  === JSON.stringify([{ role: 'contributors', color: null, members: [{ name: 'P', teams: ['zh_tw', 'ja_jp'] }, { name: 'Q', areas: ['testing'] }] }]));

// ---- ★ 管理員：開發者加的，做得到網站上的管理，但不在名單上、也動不了管理員名單 ----
check('staffKey：UUID、Discord 代號、遊戲名字分得出來', staffKey(dashed(BOB.id).toUpperCase()) === 'u:' + BOB.id
  && staffKey(' 123456789012345678 ') === 'd:123456789012345678' && staffKey('Bob_99') === 'n:bob_99'
  && staffKey('1234567890123456') === 'n:1234567890123456' && staffKey('bad name!') === null
  && staffKey('') === null && staffKey(null) === null && staffKey('123456789012345678901') === null);
const bobNow = await issueWebToken(env, BOB.id, BOB.name, NOW);
const staffPost = (path, bodyValue, token) => worker.fetch(post(path, bodyValue, token), env);
check('★ 一般玩家：管理員名單看不到、加不了、移除不了', (await worker.fetch(get('/admin/staff', bobNow), env)).status === 403
  && (await staffPost('/admin/staff', { who: BOB.name }, bobNow)).status === 403
  && (await staffPost('/admin/unstaff', { key: 'u:' + BOB.id }, bobNow)).status === 403
  && (await worker.fetch(get('/admin/staff'), env)).status === 403);
me = await (await worker.fetch(get('/me', bobNow), env)).json();
check('一般玩家的 /me：不是管理員', me.staff === false && me.developer === false);
check('開發者的 /me：管理頁開得起來', (await (await worker.fetch(get('/me', aliceWeb), env)).json()).staff === true);
r = await staffPost('/admin/staff', { who: 'not a name!' }, aliceWeb);
check('看不懂的代號：擋掉', r.status === 400 && (await r.json()).why === 'bad who');
r = await staffPost('/admin/staff', { who: BOB.name, note: 'x'.repeat(61) }, aliceWeb);
check('備註太長：擋掉', r.status === 400 && (await r.json()).why === 'bad note');
r = await staffPost('/admin/staff', { who: BOB.name, note: ' 幫忙看披風 ' }, aliceWeb);
check('★ 開發者照遊戲名字加一個管理員', r.status === 200 && (await r.json()).key === 'n:bob'
  && env.CORPUS.data.has('staff:n:bob'));
r = await staffPost('/admin/staff', { who: 'BOB' }, aliceWeb);
check('重複加：說已經在了', r.status === 409 && (await r.json()).why === 'already');
me = await (await worker.fetch(get('/me', bobNow), env)).json();
check('★ 他登入之後是管理員，但不是開發者、不是成員（沒有葉子、沒有上傳的待遇）',
  me.staff === true && me.developer === false && me.member === false);
check('★ 照名字加的，他一登入就改記成 UUID（名字之後被別人拿走也沒用）',
  env.CORPUS.data.has('staff:u:' + BOB.id) && !env.CORPUS.data.has('staff:n:bob')
  && JSON.parse(env.CORPUS.data.get('staff:u:' + BOB.id).value).m === BOB.name);
const impostor = await issueWebToken(env, 'e'.repeat(32), BOB.name, NOW);
check('★ 另一個帳號改成同一個名字：不是管理員',
  (await (await worker.fetch(get('/me', impostor), env)).json()).staff === false
  && (await worker.fetch(get('/admin/bans', impostor), env)).status === 403);
check('★ 管理員進得了管理：披風、封禁、特別名單', (await worker.fetch(get('/admin/capes', bobNow), env)).status === 200
  && (await worker.fetch(get('/admin/bans', bobNow), env)).status === 200
  && (await worker.fetch(get('/admin/perks', bobNow), env)).status === 200);
githubFile = TEAMED;
r = await worker.fetch(post('/admin/credits/teams', { section: 'contributors', who: 'Ming_01', teams: ['ko_kr'], areas: [] }, bobNow),
  { ...env, GITHUB_TOKEN: 't' });
check('★ 管理員也能改翻譯團隊與貢獻項目（跟新增成員一樣）', r.status === 200 && (await r.json()).ok === true);
r = await staffPost('/admin/perk', { uuid: 'f'.repeat(32), note: '管理員加的' }, bobNow);
check('管理員加的特別名單，紀錄上寫的是他', r.status === 200
  && JSON.parse(env.CORPUS.data.get('perk:' + 'f'.repeat(32)).value).by === BOB.name);
await staffPost('/admin/unperk', { uuid: 'f'.repeat(32) }, bobNow);
check('★ 管理員動不了管理員名單：看不到、加不了、移除不了（連自己都不行）',
  (await worker.fetch(get('/admin/staff', bobNow), env)).status === 403
  && (await staffPost('/admin/staff', { who: 'Carol' }, bobNow)).status === 403
  && (await staffPost('/admin/unstaff', { key: 'u:' + BOB.id }, bobNow)).status === 403
  && env.CORPUS.data.has('staff:u:' + BOB.id));
// 照 Discord 代號加的：只用 Discord 登入、還沒綁遊戲 ID 的人也能管理
const DAVE = '444444444444444444';
env.CORPUS.data.set('d:' + DAVE, { value: JSON.stringify({ n: 'Dave 的 Discord' }), metadata: null });
const daveDc = await issueDiscordToken(env, DAVE, NOW);
check('還沒加之前：Discord 帳號進不了管理', (await dc('/admin/bans', daveDc)).status === 403
  && (await (await dc('/me', daveDc)).json()).staff === false);
r = await staffPost('/admin/staff', { who: DAVE, note: 'DC' }, aliceWeb);
check('★ 開發者照 Discord 代號加一個管理員', r.status === 200 && env.CORPUS.data.has('staff:d:' + DAVE));
me = await (await dc('/me', daveDc)).json();
check('★ 只用 Discord 登入（沒綁遊戲 ID）的管理員：進得了管理，動不了管理員名單',
  me.linked === false && me.staff === true && (await dc('/admin/bans', daveDc)).status === 200
  && (await dc('/admin/staff', daveDc)).status === 403);
r = await worker.fetch(get('/admin/staff', aliceWeb), env);
const staffList = (await r.json()).staff;
check('★ 開發者看得到名單：類型、代號、名字、備註、誰加的', r.status === 200 && staffList.length === 2
  && staffList.some((s) => s.kind === 'u' && s.id === BOB.id && s.name === BOB.name && s.note === '幫忙看披風' && s.by === ALICE.name)
  && staffList.some((s) => s.kind === 'd' && s.id === DAVE && s.key === 'd:' + DAVE)
  && r.headers.get('cache-control') === 'no-store');
check('★ 公開的資料裡看不出誰是管理員', !(await (await worker.fetch(api('/list'), env)).text()).includes('staff')
  && !JSON.stringify((await (await worker.fetch(api('/team'), env)).json())).includes(BOB.name));
check('亂寫的鍵：移除擋掉', (await staffPost('/admin/unstaff', { key: 'perk:' + BOB.id }, aliceWeb)).status === 400
  && (await staffPost('/admin/unstaff', {}, aliceWeb)).status === 400);
r = await staffPost('/admin/unstaff', { key: 'u:' + BOB.id }, aliceWeb);
await staffPost('/admin/unstaff', { key: 'd:' + DAVE }, aliceWeb);
check('★ 開發者移除之後，兩個人都馬上沒有權限', r.status === 200
  && (await worker.fetch(get('/admin/bans', bobNow), env)).status === 403
  && (await dc('/admin/bans', daveDc)).status === 403
  && (await (await worker.fetch(get('/me', bobNow), env)).json()).staff === false);
// ---- ★ 新增管理員改成搜尋已註冊的人（使用者 2026-10-15：一格塞不下三種代號） ----
{
  const ERIC = '412892319889489924';
  const ERIC_UUID = 'facc3ddab70a40dba028fe723bb53c88';
  const OLD = '555555555555555555';
  env.CORPUS.data.set('d:' + ERIC, { value: JSON.stringify({ n: 'n02sayla', m: 'eric18960', u: ERIC_UUID, t: NOW }),
    metadata: { n: 'n02sayla', m: 'eric18960', u: ERIC_UUID } });
  env.CORPUS.data.set('l:' + ERIC_UUID, { value: ERIC, metadata: null });
  env.CORPUS.data.set('d:' + OLD, { value: JSON.stringify({ n: 'OldTimer' }), metadata: null });   // 改版前存的，沒有 metadata
  const capeKey = 'cape:' + await publicId('c'.repeat(32));
  const hadCape = env.CORPUS.data.get(capeKey);
  env.CORPUS.data.set(capeKey, { value: '', metadata: { h: 'f'.repeat(64), u: 'c'.repeat(32), n: 'CapeOnly', v: NOW } });
  const people = async (q, token = aliceWeb) => {
    const res = await worker.fetch(get('/admin/people?q=' + encodeURIComponent(q), token), env);
    return { status: res.status, list: res.status === 200 ? (await res.json()).people : null };
  };
  const eric = (out) => out.list && out.list[0] && out.list[0].discord === ERIC && out.list[0].mc === 'eric18960'
    && out.list[0].uuid === ERIC_UUID && out.list[0].dname === 'n02sayla';
  check('★ 搜尋：Discord 名、Minecraft ID、UUID（帶連字號）、Discord ID 都找得到同一個人',
    eric(await people('n02')) && eric(await people('ERIC189')) && eric(await people('facc3dda-b70a'))
    && eric(await people('41289231988')));
  check('搜尋：改版前沒有 metadata 的帳號也找得到', (await people('oldtim')).list.some((p) => p.discord === OLD && p.dname === 'OldTimer'));
  check('搜尋：只傳過披風、沒有網站帳號的人也找得到', (await people('capeonly')).list.some((p) => p.uuid === 'c'.repeat(32) && p.discord === null));
  check('搜尋：完全相同的排在包含的前面', personScore({ mc: 'eric' }, 'eric') > personScore({ mc: 'eric18960' }, 'eric')
    && personScore({ mc: 'eric18960' }, 'eric') > personScore({ dname: 'xeric' }, 'eric') && personScore({ mc: 'bob' }, 'eric') === 0);
  const all = await people('');
  check('★ 沒帶字：回全部已註冊的人（下拉選單用），照名字排',
    ['n02sayla', 'OldTimer', 'CapeOnly'].every((n) => all.list.some((p) => p.dname === n || p.mc === n))
    && all.list.map((p) => (p.mc || p.dname || p.discord).toLowerCase()).every((n, i, a) => i === 0 || a[i - 1].localeCompare(n) <= 0));
  check('搜尋：一個字也篩得出來；一般玩家不能搜', eric(await people('n0'))
    && (await people('eric', bobNow)).status === 403);
  r = await staffPost('/admin/staff', { who: ERIC_UUID }, aliceWeb);
  const saved = JSON.parse(env.CORPUS.data.get('staff:u:' + ERIC_UUID).value);
  check('★ 選了之後照 UUID 加，名單上記著遊戲名字與 Discord 名字', r.status === 200 && saved.m === 'eric18960' && saved.dn === 'n02sayla'
    && (await (await worker.fetch(get('/admin/staff', aliceWeb), env)).json()).staff.some((s) => s.discord === 'n02sayla'));
  const ericDc = await issueDiscordToken(env, ERIC, NOW);
  check('★ 照 UUID 加的管理員，用綁著的 Discord 登入網站也有權限', (await dc('/admin/bans', ericDc)).status === 200);
  await staffPost('/admin/unstaff', { key: 'u:' + ERIC_UUID }, aliceWeb);
  check('移除之後 Discord 那邊也馬上沒有權限', (await dc('/admin/bans', ericDc)).status === 403);
  for (const k of ['d:' + ERIC, 'l:' + ERIC_UUID, 'd:' + OLD]) env.CORPUS.data.delete(k);
  if (hadCape) env.CORPUS.data.set(capeKey, hadCape); else env.CORPUS.data.delete(capeKey);
}
check('管理頁：管理員那一塊預設藏著，開發者才打開', pages['/admin'].includes('<div id="staff-block" hidden>')
  && pages['/admin'].includes('if (me.developer) {') && pages['/admin'].includes('if (!me || !me.staff) return;'));

// ---- ★ 套用一覽上的披風，與復原 ----
{
  const person = async (letter, name) => {
    const uuid = letter.repeat(32);
    return { uuid, name, id: await publicId(uuid), token: await issueWebToken(env, uuid, name, NOW) };
  };
  const wren = await person('1', 'Wren');
  const finch = await person('2', 'Finch');
  const robin = await person('3', 'Robin');
  const lark = await person('4', 'Lark');
  const dove = await person('5', 'Dove');
  const carol = await person('6', 'Carol');                  // 名單上的贊助者：傳得了 GIF
  const sha = async (bytes) => [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes))]
    .map((b) => b.toString(16).padStart(2, '0')).join('');
  const OWN = await sha(png(44, 34));
  const A = await sha(png(88, 68));
  const B = await sha(png(176, 136));
  const G = await sha(gif(22, 17, 2));
  const upload = (who, bytes) => worker.fetch(put('/cape', bytes, who.token), env);
  const wear = (who, hash) => worker.fetch(post('/cape/wear', { hash }, who.token), env);
  const restore = (who) => worker.fetch(post('/cape/restore', {}, who.token), env);
  const mine = async (who) => (await (await worker.fetch(get('/me', who.token), env)).json()).cape;
  const later = (who) => { env.CORPUS.data.get('cape:' + who.id).metadata.v -= 60000; forgetBursts(); };
  const has = (hash) => env.CORPUS.data.has('blob:' + hash);
  check('四個人各傳一件', (await upload(wren, png(44, 34))).status === 200 && (await upload(finch, png(88, 68))).status === 200
    && (await upload(robin, png(176, 136))).status === 200 && (await upload(carol, gif(22, 17, 2))).status === 200);
  check('沒登入不能套用；亂寫的雜湊擋掉', (await worker.fetch(post('/cape/wear', { hash: A }), env)).status === 401
    && (await wear(wren, 'xyz')).status === 400 && (await wear(wren, 'f'.repeat(64))).status === 404);
  r = await wear(wren, A);
  check('★ 剛傳完馬上換：請他等一下', r.status === 429 && (await r.json()).why === 'slow down');
  later(wren);
  r = await wear(wren, A);
  body = await r.json();
  let cape = await mine(wren);
  check('★ 套用別人的披風：自己那一筆指到同一張圖，原本那一件收著', r.status === 200 && body.back === true
    && cape.h === A && cape.worn === true && cape.back === true && has(OWN) && has(A));
  let listed = (await (await worker.fetch(api('/capes'), env)).json()).capes;
  check('★ 一覽上那一件多一個人穿；收著的那一件不在一覽上', listed.find((c) => c.h === A).ids.length === 2
    && !listed.some((c) => c.h === OWN));
  check('★ 別人收著的那一張不能套用（一覽上沒有的都不行）', (await wear(finch, OWN)).status === 404);
  check('已經穿著的那一件再按一次：沒事', (await (await wear(wren, A)).json()).same === true);
  later(wren);
  r = await wear(wren, G);
  check('★ GIF 只有成員穿得了：套用不是繞過規矩的路', r.status === 403 && (await r.json()).why === 'gif members only'
    && (await mine(wren)).h === A);
  r = await wear(wren, B);
  cape = await mine(wren);
  check('★ 再換一件：收著的還是自己傳的那一件', r.status === 200 && cape.h === B && cape.back === true
    && env.CORPUS.data.get('cape:' + wren.id).metadata.p.h === OWN && has(OWN));
  r = await restore(wren);
  cape = await mine(wren);
  check('★ 復原：換回自己原本那一件，別人的圖還在', r.status === 200 && (await r.json()).cape === true
    && cape.h === OWN && cape.worn === false && cape.back === false && has(A) && has(B)
    && env.CORPUS.data.get('cape:' + wren.id).metadata.p === undefined);
  r = await restore(wren);
  check('沒有套用過：沒得復原', r.status === 409 && (await r.json()).why === 'nothing to restore');
  r = await wear(lark, A);
  check('★ 原本沒有披風的人也能套用', r.status === 200 && (await r.json()).back === false && (await mine(lark)).worn === true);
  r = await restore(lark);
  check('★ 他復原就是脫下', r.status === 200 && (await r.json()).cape === false && (await mine(lark)) === null
    && !env.CORPUS.data.has('cape:' + lark.id) && has(A));
  // 自己傳新的：收著的那一件不必留
  await wear(lark, A);
  later(lark);
  check('★ 套用之後自己又傳一件：不再是套用來的', (await upload(lark, png(352, 272))).status === 200
    && (await mine(lark)).worn === false && env.CORPUS.data.get('cape:' + lark.id).metadata.p === undefined);
  // 管理的人照圖刪：有人收著那張圖準備復原的，也要一起拿掉
  later(wren);
  await wear(wren, A);
  check('另一個人傳了同一張圖', (await upload(dove, png(44, 34))).status === 200 && (await mine(wren)).back === true);
  r = await worker.fetch(post('/admin/remove', { hash: OWN }, aliceWeb), env);
  check('★ 照圖刪掉之後：圖沒了，收著它的人也復原不回去', r.status === 200 && !has(OWN)
    && (await mine(wren)).back === false && (await mine(dove)) === null);
  r = await restore(wren);
  check('他復原就變成脫下', r.status === 200 && (await r.json()).cape === false && (await mine(wren)) === null);
  env.CORPUS.data.set('ban:' + robin.uuid, { value: JSON.stringify({ t: NOW }), metadata: null });
  later(robin);
  check('★ 被封禁的人不能套用', (await wear(robin, A)).status === 403);
  env.CORPUS.data.delete('ban:' + robin.uuid);
  check('★ 披風一覽點開有「穿上這件」與復原，沒登入的看到的是去帳號頁的連結', pages['/capes'].includes('id="wear"')
    && pages['/capes'].includes('id="unwear"') && pages['/capes'].includes("'/cape/wear'")
    && pages['/capes'].includes("'/cape/restore'") && pages['/capes'].includes('id="wear-signin"')
    && pages['/account'].includes('id="restore"'));
}

// ---- ★ 貢獻公會：管理頁加名字、上傳圖示與旗幟，首頁的團隊那一段畫出來 ----
{
  check('guildSlug：英文字母與空格，3 到 30 個字', guildSlug(' Yu  Cha Yuan ') === 'yu-cha-yuan' && guildSlug('YuChaYuan') === 'yuchayuan'
    && guildSlug('ab') === null && guildSlug('Bad_Name1') === null && guildSlug('x'.repeat(31)) === null && guildSlug(null) === null);
  const jpeg = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0, 0, 0, 0]);
  const webp = new Uint8Array([0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50, 0, 0]);
  check('imageType：只認 PNG、JPEG、WebP', imageType(png(8, 8)) === 'image/png' && imageType(jpeg) === 'image/jpeg'
    && imageType(webp) === 'image/webp' && imageType(gif(8, 8, 1)) === null && imageType(new Uint8Array(4)) === null);
  const visitor = await issueWebToken(env, '7'.repeat(32), 'Heron', NOW);
  const helper = await issueWebToken(env, '8'.repeat(32), 'Stork', NOW);
  env.CORPUS.data.set('staff:u:' + '8'.repeat(32), { value: '{}', metadata: null });      // 一位管理員
  const send = (path, value, token) => worker.fetch(post(path, value, token), env);
  const picture = (slug, kind, bytes, token) => worker.fetch(put('/admin/guild/' + slug + '/' + kind, bytes, token), env);
  check('★ 一般玩家動不了公會名單', (await worker.fetch(get('/admin/guilds', visitor), env)).status === 403
    && (await send('/admin/guild', { name: 'YuChaYuan' }, visitor)).status === 403
    && (await picture('yuchayuan', 'icon', png(8, 8), visitor)).status === 403
    && (await send('/admin/unguild', { slug: 'yuchayuan' }, visitor)).status === 403);
  r = await send('/admin/guild', { name: 'Bad_Name1' }, aliceWeb);
  check('名字不對：擋掉', r.status === 400 && (await r.json()).why === 'bad name');
  r = await send('/admin/guild', { name: 'YuChaYuan', tag: 'TOOLONG' }, aliceWeb);
  check('標籤不對：擋掉', r.status === 400 && (await r.json()).why === 'bad tag');
  r = await send('/admin/guild', { name: 'YuChaYuan', tag: 'YCY' }, aliceWeb);
  check('★ 開發者加一個公會', r.status === 200 && (await r.json()).slug === 'yuchayuan');
  r = await send('/admin/guild', { name: 'yuchayuan' }, aliceWeb);
  check('重複加：說已經登記了', r.status === 409 && (await r.json()).why === 'already');
  check('★ 管理員也加得了', (await send('/admin/guild', { name: 'Tea Garden' }, helper)).status === 200);
  check('還沒傳圖：圖的網址是 404', (await worker.fetch(api('/guild/yuchayuan/icon'), env)).status === 404);
  r = await picture('yuchayuan', 'icon', gif(8, 8, 1), aliceWeb);
  check('★ 不是圖（或不認的格式）：擋掉', r.status === 400 && (await r.json()).why === 'bad image');
  const huge = new Uint8Array(MAX_GUILD_IMAGE + 1);
  huge.set(png(8, 8));
  r = await picture('yuchayuan', 'icon', huge, aliceWeb);
  check('太大的圖：擋掉', r.status === 413 && (await r.json()).why === 'too large');
  check('沒登記的公會傳不了圖', (await picture('nobody-here', 'icon', png(8, 8), aliceWeb)).status === 404);
  check('★ 傳圖示與旗幟', (await picture('yuchayuan', 'icon', png(64, 64), aliceWeb)).status === 200
    && (await picture('yuchayuan', 'banner', jpeg, aliceWeb)).status === 200);
  r = await worker.fetch(api('/guild/yuchayuan/banner'), env);
  check('★ 圖拿得到：種類照內容、不讓瀏覽器亂猜', r.status === 200 && r.headers.get('content-type') === 'image/jpeg'
    && r.headers.get('x-content-type-options') === 'nosniff' && new Uint8Array(await r.arrayBuffer()).length === jpeg.length);
  r = await worker.fetch(api('/team'), env);
  body = await r.json();
  check('★ /api/team 帶著公會：名字、標籤、圖的版本；沒有「誰加的」', r.status === 200 && body.guilds.length === 2
    && body.guilds[0].name === 'YuChaYuan' && body.guilds[0].tag === 'YCY' && body.guilds[0].icon > 0 && body.guilds[0].banner > 0
    && body.guilds[1].name === 'Tea Garden' && body.guilds[1].icon === 0 && !JSON.stringify(body.guilds).includes(ALICE.name));
  r = await worker.fetch(get('/admin/guilds', aliceWeb), env);
  check('管理的人看得到是誰加的', (await r.json()).guilds.map((g) => g.by).join() === ALICE.name + ',Stork');
  check('亂寫的代碼：移除擋掉', (await send('/admin/unguild', { slug: '../x' }, aliceWeb)).status === 400);
  r = await send('/admin/unguild', { slug: 'yuchayuan' }, aliceWeb);
  body = await (await worker.fetch(api('/team'), env)).json();
  check('★ 移除之後：名單上沒有了，圖也刪了', r.status === 200 && body.guilds.map((g) => g.slug).join() === 'tea-garden'
    && (await worker.fetch(api('/guild/yuchayuan/icon'), env)).status === 404
    && !env.CORPUS.data.has('gimg:yuchayuan:banner'));
  await send('/admin/unguild', { slug: 'tea-garden' }, aliceWeb);
  env.CORPUS.data.delete('staff:u:' + '8'.repeat(32));
  check('管理頁有公會那一塊（名字、標籤、挑檔案）', ['id="g-name"', 'id="g-tag"', 'id="g-file"', 'id="guilds"']
    .every((w) => pages['/admin'].includes(w)) && pages['/admin'].includes('accept="image/png,image/jpeg,image/webp"'));
}

// ---- ★ 披風一覽：排序用的資料、最愛、使用紀錄、公開與否、審核 ----
{
  const person = async (letter, name) => {
    const uuid = letter.repeat(32);
    return { uuid, name, id: await publicId(uuid), token: await issueWebToken(env, uuid, name, NOW) };
  };
  const ivy = await person('d', 'Ivy');
  const oak = await person('9', 'Oak');
  const elm = await person('0', 'Elm');
  const sha = async (bytes) => [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes))]
    .map((b) => b.toString(16).padStart(2, '0')).join('');
  // 這一段要的是「別人都沒有的圖」：同樣的尺寸，在沒用到的位元組上做記號，雜湊就不同
  const mark = (bytes, n) => { const out = bytes.slice(); out[40] = n; return out; };
  const P1 = mark(png(704, 544), 1);
  const P2 = mark(png(1056, 816), 2);
  const P3 = mark(png(22, 17), 3);
  const H1 = await sha(P1);
  const H2 = await sha(P2);
  const H3 = await sha(P3);
  const send = (path, value, who) => worker.fetch(post(path, value, who && who.token), env);
  const upload = (who, bytes, query = '') => worker.fetch(new Request('https://x.test/api/cape' + query, {
    method: 'PUT', body: bytes, headers: { authorization: 'Bearer ' + who.token } }), env);
  const gallery = async () => (await (await worker.fetch(api('/capes'), env)).json()).capes;
  const meta = (who) => env.CORPUS.data.get('cape:' + who.id).metadata;
  const mine = async (who) => (await (await worker.fetch(get('/me', who.token), env)).json());
  const roster = async () => (await (await worker.fetch(get('/list'), env)).json()).capes;
  const later = (who) => { meta(who).v -= 60000; forgetBursts(); };
  const hist = (who) => [...env.CORPUS.data.keys()].filter((k) => k.startsWith('hist:' + who.id + ':')).map((k) => k.split(':')[2]);
  const picture = (who, key) => worker.fetch(new Request('https://x.test/api/cape/' + who.id + (key ? '?k=' + key : '')), env);

  // 排序用的資料
  check('兩個人各傳一件', (await upload(ivy, P1)).status === 200 && (await upload(oak, P2)).status === 200);
  let list = await gallery();
  let g1 = list.find((c) => c.h === H1);
  check('★ 一覽的每一筆有：幾個人穿、最早什麼時候傳的、最近一次換上、幾個人加了最愛',
    g1.ids.length === 1 && g1.c > 0 && g1.v >= g1.c && g1.stars === 0);
  meta(ivy).c -= 86400000;                          // 她那一件是昨天傳的
  check('groupCapes：最早上傳的時間取最小的那一個；沒記的拿換上的時間頂著', (() => {
    const out = groupCapes([{ id: 'a', h: 'x', t: 'png', s: 1, v: 9, c: 5 }, { id: 'b', h: 'x', t: 'png', s: 1, v: 7, c: 3 },
      { id: 'c', h: 'y', t: 'png', s: 1, v: 4 }]);
    return out.find((g) => g.h === 'x').c === 3 && out.find((g) => g.h === 'y').c === 4;
  })());

  // 最愛
  check('沒登入不能加最愛；亂寫的擋掉', (await send('/cape/star', { hash: H1, on: true })).status === 401
    && (await send('/cape/star', { hash: 'nope', on: true }, oak)).status === 400
    && (await send('/cape/star', { hash: H1 }, oak)).status === 400);
  check('一覽上沒有的不能加', (await send('/cape/star', { hash: 'e'.repeat(64), on: true }, oak)).status === 404);
  r = await send('/cape/star', { hash: H1, on: true }, oak);
  body = await r.json();
  check('★ 加進我的最愛：那一件的人數加一', r.status === 200 && body.on === true && body.stars === 1
    && (await gallery()).find((c) => c.h === H1).stars === 1);
  r = await send('/cape/star', { hash: H1, on: true }, oak);
  check('連按：請他等一下', r.status === 429);
  const cool = (who) => { const row = env.CORPUS.data.get('fav:' + who.uuid); row.value = JSON.stringify({ ...JSON.parse(row.value), t: 0 }); };
  cool(oak);
  r = await send('/cape/star', { hash: H1, on: true }, oak);
  check('★ 已經加過的再加一次：人數不會多算', r.status === 200 && (await r.json()).stars === 1);
  check('另一個人也加：兩個人', (await (await send('/cape/star', { hash: H1, on: true }, elm)).json()).stars === 2);
  r = await worker.fetch(get('/capes/mine', oak.token), env);
  body = await r.json();
  check('★ 我的最愛與使用紀錄只有本人拿得到', r.status === 200 && body.favs.join() === H1 && body.history.length === 1
    && body.history[0].h === H2 && (await worker.fetch(get('/capes/mine'), env)).status === 401);
  cool(oak);
  r = await send('/cape/star', { hash: H1, on: false }, oak);
  check('★ 移出最愛：人數減一', r.status === 200 && (await r.json()).stars === 1
    && (await (await worker.fetch(get('/capes/mine', oak.token), env)).json()).favs.length === 0);

  // 使用紀錄：穿過的記著。他自己那一件收著等復原，所以圖還在
  later(oak);
  check('他穿上她那一件', (await send('/cape/wear', { hash: H1 }, oak)).status === 200);
  body = await (await worker.fetch(get('/capes/mine', oak.token), env)).json();
  check('★ 兩件都在他的使用紀錄裡，新的在前；沒有東西被移除', body.history.map((one) => one.h).join() === H1 + ',' + H2
    && body.removed === 0 && env.CORPUS.data.has('blob:' + H2));
  check('★ 收著等復原的那一件，本人照雜湊拿得到；別人拿不到', (await picture(oak, H2)).status === 200
    && (await picture(oak, 'e'.repeat(64))).status === 404 && (await picture(ivy, H2)).status === 404);
  // 沒有人穿的披風留七天，期滿才刪——加了最愛的也一樣（使用者 2026-10-13）
  later(oak);
  check('自己再傳第三件', (await upload(oak, P3)).status === 200);
  let waiting = (await gallery()).find((c) => c.h === H2);
  check('★ 他最早那一件沒有人穿了：留在一覽上倒數，圖還在', Boolean(waiting) && waiting.ids.length === 0 && waiting.idle > 0
    && waiting.x === 1056 && env.CORPUS.data.has('blob:' + H2) && (await worker.fetch(get('/idle/' + H2), env)).status === 200);
  cool(elm);
  check('★ 倒數中的照樣可以加最愛', (await (await send('/cape/star', { hash: H2, on: true }, elm)).json()).stars === 1
    && (await gallery()).find((c) => c.h === H2).stars === 1);
  later(oak);
  r = await send('/cape/wear', { hash: H2 }, oak);
  check('★ 有人再穿上：不再倒數', r.status === 200 && meta(oak).h === H2 && !(H2 in idleNow())
    && (await gallery()).find((c) => c.h === H2).ids.length === 1 && !('idle' in (await gallery()).find((c) => c.h === H2)));
  check('復原：換回自己傳的第三件，最早那一件重新開始倒數', (await send('/cape/restore', {}, oak)).status === 200
    && meta(oak).h === H3 && H2 in idleNow());
  expire(H2);
  check('★ 滿七天沒有人穿：一覽上沒有，圖也刪了（就算在紀錄與別人的最愛裡），人數表也清掉', !(await gallery()).some((c) => c.h === H2)
    && !env.CORPUS.data.has('blob:' + H2) && hist(oak).includes(H2)
    && !(H2 in JSON.parse(env.CORPUS.data.get('stars').value)));
  check('★ 刪掉的圖誰都拿不到、也穿不回去', (await picture(oak, H2)).status === 404
    && (await worker.fetch(get('/idle/' + H2), env)).status === 404
    && (await send('/cape/wear', { hash: H2 }, elm)).status === 404
    && (await send('/cape/wear', { hash: H2 }, oak)).status === 404 && meta(oak).h !== H2);
  body = await (await worker.fetch(get('/capes/mine', oak.token), env)).json();
  check('★ 看「我的」的時候告訴他有一件已經移除，紀錄跟著清掉', body.removed === 1
    && !body.history.some((one) => one.h === H2) && !hist(oak).includes(H2));
  check('只說一次：再看就沒有了', (await (await worker.fetch(get('/capes/mine', oak.token), env)).json()).removed === 0);
  body = await (await worker.fetch(get('/capes/mine', elm.token), env)).json();
  check('★ 加了最愛的人也被告知，最愛裡清掉那一件、還在的留著', body.removed === 1 && body.favs.join() === H1
    && JSON.parse(env.CORPUS.data.get('fav:' + elm.uuid).value).h.join() === H1);
  cool(elm);
  await send('/cape/star', { hash: H1, on: false }, elm);
  // 不公開的披風沒有人穿了：一樣留七天，但不出現在一覽上；本人從紀錄穿得回去，管理的人看得到、可以馬上刪
  {
    const P9 = mark(png(88, 68), 9);
    const H9 = await sha(P9);
    later(oak);
    check('他傳一件不公開的，再換回第三件', (await upload(oak, P9, '?public=0')).status === 200 && meta(oak).vis === 'unlisted' && !('q' in meta(oak)));
    later(oak);
    await upload(oak, P3, '?public=1');
    check('★ 不公開的那一件：清單上記著不公開，一覽上沒有，別人不能加最愛', idleNow()[H9].q === 1
      && idleNow()[H9].vis === 'unlisted' && !(await gallery()).some((c) => c.h === H9)
      && (await send('/cape/star', { hash: H9, on: true }, ivy)).status === 404);
    const seenByStaff = (await (await worker.fetch(get('/admin/capes', aliceWeb), env)).json()).capes.find((c) => c.h === H9);
    check('★ 管理的人看得到它：沒有人穿、不公開、在倒數', Boolean(seenByStaff) && seenByStaff.hidden === true
      && seenByStaff.owners.length === 0 && seenByStaff.idle > 0);
    later(oak);
    check('★ 本人從自己的紀錄穿得回去', (await send('/cape/wear', { hash: H9 }, oak)).status === 200 && meta(oak).h === H9
      && !(H9 in idleNow()));
    check('復原之後又開始倒數', (await send('/cape/restore', {}, oak)).status === 200 && H9 in idleNow());
    r = await send('/admin/remove', { hash: H9 }, { token: aliceWeb });
    check('★ 管理的人照圖刪：不必等七天', r.status === 200 && (await r.json()).removed === 1
      && !env.CORPUS.data.has('blob:' + H9) && !(H9 in idleNow()));
  }
  later(oak);
  r = await send('/cape/wear', { hash: H1 }, oak);
  check('還在一覽上的照樣從紀錄穿得回去', r.status === 200 && meta(oak).h === H1);
  // 紀錄有上限：超過的把最舊的拿掉
  for (let i = 0; i < 9; i++) {
    env.CORPUS.data.set('hist:' + elm.id + ':' + String(i).repeat(64), { value: '', metadata: { t: 'png', s: 1, at: i + 1 } });
  }
  await upload(elm, mark(png(88, 68), 11));
  check('★ 使用紀錄最多留 8 件，舊的先走', hist(elm).length === 8 && !hist(elm).includes('0'.repeat(64))
    && !hist(elm).includes('1'.repeat(64)) && hist(elm).includes('8'.repeat(64)));

  // 公開與否
  check('沒登入不能改；沒有披風的人改不了', (await send('/cape/public', { on: false })).status === 401
    && (await send('/cape/public', { on: false }, await person('e', 'Fir'))).status === 404);
  r = await send('/cape/public', { on: false }, ivy);
  check('★ 設成不公開：一覽上看不到她那一件（別人穿著同一張的還在）', r.status === 200 && (await r.json()).public === false
    && (await mine(ivy)).cape.public === false && !(await gallery()).some((c) => c.ids.includes(ivy.id)));
  check('★ 不公開不影響遊戲裡：名單上她還是有披風', ivy.id in (await roster()));
  check('★ 管理的人看得到全部，標著誰不公開', await (async () => {
    const seen = (await (await worker.fetch(get('/admin/capes', aliceWeb), env)).json()).capes;
    const owner = seen.flatMap((c) => c.owners).find((o) => o.id === ivy.id);
    return owner && owner.vis === 'unlisted' && seen.every((c) => typeof c.hidden === 'boolean' && typeof c.pending === 'boolean');
  })());
  check('★ 只有她一個人穿的不公開披風：別人不能加最愛', await (async () => {
    later(ivy);
    await upload(ivy, mark(png(352, 272), 12));
    const lone = meta(ivy).h;
    return meta(ivy).vis === 'unlisted' && (await send('/cape/star', { hash: lone, on: true }, elm)).status === 404;
  })());
  later(ivy);
  check('★ 上傳的時候說要公開：照這一次的', (await upload(ivy, mark(png(176, 136), 13), '?public=1')).status === 200 && !meta(ivy).q && !meta(ivy).vis
    && (await gallery()).some((c) => c.ids.includes(ivy.id)));
  later(ivy);
  check('上傳的時候說不公開', (await upload(ivy, mark(png(44, 34), 14), '?public=0')).status === 200 && meta(ivy).vis === 'unlisted');
  check('改回公開', (await send('/cape/public', { on: true }, ivy)).status === 200 && !meta(ivy).vis);

  // 審核
  check('★ 一般玩家動不了審核名單', (await worker.fetch(get('/admin/reviews', elm.token), env)).status === 403
    && (await send('/admin/review', { uuid: elm.uuid }, elm)).status === 403
    && (await send('/admin/unreview', { uuid: elm.uuid }, elm)).status === 403
    && (await send('/admin/approve', { hash: H1 }, elm)).status === 403);
  check('UUID 不對、備註太長：擋掉', (await send('/admin/review', { uuid: 'Elm' }, { token: aliceWeb })).status === 400
    && (await send('/admin/review', { uuid: elm.uuid, note: 'x'.repeat(61) }, { token: aliceWeb })).status === 400);
  const before = meta(elm).h;
  r = await send('/admin/review', { uuid: elm.uuid, note: '測試' }, { token: aliceWeb });
  check('★ 列入審核：他現在身上那一件不受影響', r.status === 200 && (await r.json()).added === 1
    && !meta(elm).r && elm.id in (await roster()) && (await mine(elm)).review === true);
  check('重複列入：說已經在了', (await send('/admin/review', { uuid: elm.uuid }, { token: aliceWeb })).status === 409);
  later(elm);
  r = await upload(elm, mark(png(352, 272), 15));
  check('★ 審核名單上的人已經有一件：要先移除才能再傳', r.status === 403 && (await r.json()).why === 'review one');
  await worker.fetch(del('/cape', elm.token), env);
  forgetBursts();
  r = await upload(elm, mark(png(352, 272), 15));
  body = await r.json();
  const WAIT = meta(elm).h;
  check('★ 之後傳的要等審核：傳得上去，回覆說在等', r.status === 200 && body.pending === true && meta(elm).r === 1
    && (await mine(elm)).cape.pending === true);
  check('★ 等審核的披風：一覽沒有、遊戲裡的名單上也沒有', !(await gallery()).some((c) => c.h === WAIT) && !(elm.id in (await roster())));
  check('★ 等審核的圖：不知道雜湊的人拿不到，本人與管理的人（帶雜湊）拿得到', (await picture(elm)).status === 404
    && (await picture(elm, 'e'.repeat(64))).status === 404 && (await picture(elm, WAIT)).status === 200);
  check('別人不能穿等審核的那一件', (await send('/cape/wear', { hash: WAIT }, oak)).status === 404);
  const seen = (await (await worker.fetch(get('/admin/capes', aliceWeb), env)).json()).capes.find((c) => c.h === WAIT);
  check('★ 管理的人看得到它，標著待審核', Boolean(seen) && seen.pending === true && seen.owners[0].pending === true);
  r = await send('/admin/approve', { hash: WAIT }, { token: aliceWeb });
  check('★ 通過審核：別人看得到了', r.status === 200 && (await r.json()).approved === 1 && !meta(elm).r
    && (await gallery()).some((c) => c.h === WAIT) && elm.id in (await roster()));
  later(elm);
  // 換穿一覽上別人的一件（自己那一件收著等復原），再穿回來
  await send('/cape/wear', { hash: (await gallery()).find((c) => c.h !== WAIT && c.h !== before).h }, elm);
  check('換穿別人的：自己傳的那一件收著', meta(elm).h !== WAIT && meta(elm).p && meta(elm).p.h === WAIT);
  later(elm);
  r = await send('/cape/wear', { hash: WAIT }, elm);
  check('★ 通過過的那一張，他之後再穿不必再審', r.status === 200 && (await r.json()).pending === false && !meta(elm).r);
  later(elm);
  await worker.fetch(del('/cape', elm.token), env);
  forgetBursts();
  await upload(elm, mark(png(1056, 816), 16));
  check('又傳一張新的：還是要等', meta(elm).r === 1 && !(elm.id in (await roster())));
  r = await worker.fetch(get('/admin/reviews', aliceWeb), env);
  body = await r.json();
  check('名單看得到是誰加的、備註', body.reviews.some((one) => one.uuid === elm.uuid && one.note === '測試' && one.by === ALICE.name));
  r = await send('/admin/unreview', { uuid: elm.uuid }, { token: aliceWeb });
  check('★ 從審核名單移除：等著的那一件直接放行', r.status === 200 && !meta(elm).r && elm.id in (await roster())
    && (await mine(elm)).review === false);
  // 照圖列入：傳那張圖的人；穿別人那一件的不算
  later(oak);
  await send('/cape/wear', { hash: meta(ivy).h }, oak);
  r = await send('/admin/review', { hash: meta(ivy).h }, { token: aliceWeb });
  check('★ 照圖列入審核：只算上傳的人，套用的人不算', r.status === 200 && (await r.json()).added === 1
    && env.CORPUS.data.has('review:' + ivy.uuid) && !env.CORPUS.data.has('review:' + oak.uuid));
  await send('/admin/unreview', { uuid: ivy.uuid }, { token: aliceWeb });
  // 管理的人刪掉某個人的披風：他的使用紀錄裡也不留
  r = await send('/admin/remove', { hash: meta(elm).h }, { token: aliceWeb });
  check('★ 照圖刪除：誰的使用紀錄裡都不留，最愛的人數也清掉', r.status === 200
    && !hist(elm).includes(await sha(mark(png(1056, 816), 16))) && !env.CORPUS.data.has('cape:' + elm.id));

  // 上傳冷卻（使用者 2026-10-14：一小時內傳 3 件，之後要等 12 小時）
  check('★ 冷卻：一小時內第三次上傳之後鎖 12 小時；隔超過一小時的不算', (() => {
    const t0 = 1_000_000_000_000;
    let log = burstNote(null, t0);
    log = burstNote(log, t0 + 60_000);
    const two = burstLock(log, t0 + 120_000) === 0;
    log = burstNote(log, t0 + 120_000);
    const locked = burstLock(log, t0 + 130_000) === t0 + 120_000 + BURST_LOCK_MS;
    const free = burstLock(log, t0 + 120_000 + BURST_LOCK_MS + 1) === 0;
    let spread = burstNote(null, t0);
    spread = burstNote(spread, t0 + BURST_WINDOW_MS + 1);
    spread = burstNote(spread, t0 + BURST_WINDOW_MS + 2);
    return two && locked && free && burstLock(spread, t0 + BURST_WINDOW_MS + 3) === 0;
  })());
  forgetBursts();
  env.CORPUS.data.delete('review:' + oak.uuid);
  const C1 = mark(png(88, 68), 41);
  const C2 = mark(png(88, 68), 42);
  const C3 = mark(png(88, 68), 43);
  const C4 = mark(png(88, 68), 44);
  const step = () => { meta(oak).v -= 60000; };          // 只跳過 20 秒的間隔，不清冷卻
  step();
  let ok = (await upload(oak, C1)).status === 200;
  step();
  ok = ok && (await upload(oak, C2)).status === 200;
  step();
  ok = ok && (await upload(oak, C3)).status === 200;
  step();
  r = await upload(oak, C4);
  body = await r.json();
  check('★ 一小時內傳了 3 件：第 4 件要等 12 小時，回答裡有解鎖的時間',
    ok && r.status === 429 && body.why === 'cooldown' && body.until > Date.now() + BURST_LOCK_MS - 60_000);
  forgetBursts();
  step();
  check('冷卻過了就能再傳', (await upload(oak, C4)).status === 200);

  // 審核名單上的人只能有一件自己傳的披風（等審核的也算）
  forgetBursts();
  env.CORPUS.data.set('review:' + oak.uuid, { value: JSON.stringify({ n: 'Oak', note: '', t: 1, by: 'x', ok: [] }), metadata: null });
  step();
  r = await upload(oak, C1);
  check('★ 審核名單上的人已經有一件：不能再傳', r.status === 403 && (await r.json()).why === 'review one');
  check('移除之後可以傳一件（等審核）', (await worker.fetch(del('/cape', oak.token), env)).status === 200
    && await (async () => { forgetBursts(); const a = await upload(oak, C2); const b = await a.json(); return a.status === 200 && b.pending === true; })());
  step();
  check('★ 等審核的那一件也算：不能再傳第二件', (await (await upload(oak, C3)).json()).why === 'review one');
  env.CORPUS.data.delete('review:' + oak.uuid);
  forgetBursts();

  check('★ 披風一覽有排序、篩選與星星；管理的人多三個篩選', ['id="sort"', 'value="worn"', 'value="stars"', 'value="old"',
    'id="kind"', 'id="scope"', 'value="fav"', 'value="hist"', 'value="unlisted"', 'value="private"', 'value="pending"', 'id="skinpick"',
    'id="fav"', 'id="approve"', 'id="review"', 'id="forget"']
    .every((w) => pages['/capes'].includes(w)) && pages['/capes'].includes("'/cape/star'") && pages['/capes'].includes("'/capes/mine'"));
  check('★ 帳號頁有公開程度的選擇；管理頁有審核名單', pages['/account'].includes('id="vis"') && pages['/account'].includes("'/cape/visibility'")
    && pages['/account'].includes('id="pending"') && pages['/admin'].includes('id="reviews"') && pages['/admin'].includes("'/admin/review'"));
}

{
  const person = async (pair, name) => {
    const uuid = pair.repeat(16);
    return { uuid, name, id: await publicId(uuid), token: await issueWebToken(env, uuid, name, NOW) };
  };
  const fern = await person('f1', 'Fern');
  const moss = await person('f2', 'Moss');
  const sha = async (bytes) => [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes))]
    .map((b) => b.toString(16).padStart(2, '0')).join('');
  const mark = (bytes, n) => { const out = bytes.slice(); out[40] = n; out[41] = 7; return out; };
  const send = (path, value, who) => worker.fetch(post(path, value, who && who.token), env);
  const upload = (who, bytes, query = '') => worker.fetch(new Request('https://x.test/api/cape' + query, {
    method: 'PUT', body: bytes, headers: { authorization: 'Bearer ' + who.token } }), env);
  const gallery = async () => (await (await worker.fetch(api('/capes'), env)).json()).capes;
  const meta = (who) => env.CORPUS.data.get('cape:' + who.id).metadata;
  const mine = async (who) => (await (await worker.fetch(get('/me', who.token), env)).json());
  const roster = async () => (await (await worker.fetch(get('/list'), env)).json()).capes;
  const link = (hash, who) => worker.fetch(get('/capes/' + hash, who && who.token), env);
  const later = (who) => { meta(who).v -= 60000; forgetBursts(); };
  const staff = { token: aliceWeb };

  check('visibility：舊的 q 讀成不公開，新的 vis 優先，認不得的當公開', visibility(null) === 'public'
    && visibility({}) === 'public' && visibility({ q: 1 }) === 'unlisted' && visibility({ q: 1, vis: 'private' }) === 'private'
    && visibility({ vis: 'bogus' }) === 'public' && VISIBILITY.join() === 'public,unlisted,private');
  check('askedVisibility：?vis= 優先，舊的 ?public=0／1 照舊，沒說回 null', askedVisibility(new URLSearchParams('vis=private&public=1')) === 'private'
    && askedVisibility(new URLSearchParams('public=0')) === 'unlisted' && askedVisibility(new URLSearchParams('public=1')) === 'public'
    && askedVisibility(new URLSearchParams('vis=nope')) === null && askedVisibility(new URLSearchParams('')) === null);

  forgetBursts();
  const A = mark(png(88, 68), 61);
  const HA = await sha(A);
  check('上傳時選私人', (await upload(fern, A, '?vis=private')).status === 200 && meta(fern).vis === 'private');
  check('★ 私人：一覽沒有；別人拿到連結也看不到；不能穿、不能加最愛', !(await gallery()).some((c) => c.h === HA)
    && (await link(HA)).status === 404 && (await link(HA, moss)).status === 404
    && (await send('/cape/wear', { hash: HA }, moss)).status === 404
    && (await send('/cape/star', { hash: HA, on: true }, moss)).status === 404);
  r = await link(HA, fern);
  body = await r.json();
  check('★ 私人：本人用連結看得到，標著是自己的', r.status === 200 && body.cape.own === true && body.cape.vis === 'private'
    && body.cape.ids.join() === fern.id);
  const staffSeen = (await (await worker.fetch(get('/admin/capes', aliceWeb), env)).json()).capes.find((c) => c.h === HA);
  check('★ 管理的人看得到私人的那一件，整筆與每個人都標著私人', Boolean(staffSeen) && staffSeen.vis === 'private'
    && staffSeen.owners[0].vis === 'private' && staffSeen.hidden === true && (await link(HA, staff)).status === 200);
  check('★ 私人只管網站：遊戲裡的名單照樣有她的披風', fern.id in (await roster()));
  body = await mine(fern);
  check('/api/me 說是私人', body.cape.vis === 'private' && body.cape.public === false);

  r = await send('/cape/visibility', { vis: 'unlisted' }, fern);
  check('改成不公開', r.status === 200 && (await r.json()).vis === 'unlisted' && meta(fern).vis === 'unlisted');
  r = await link(HA);
  body = await r.json();
  check('★ 不公開：一覽沒有，拿到連結的人（沒登入也可以）看得到', !(await gallery()).some((c) => c.h === HA)
    && r.status === 200 && body.cape.vis === 'unlisted' && body.cape.ids.join() === fern.id && !body.cape.own
    && body.cape.hidden === true);
  r = await send('/cape/wear', { hash: HA }, moss);
  check('★ 不公開：拿到連結的人可以穿上；他那一筆記著是從連結來的（lk），一覽上照樣沒有這件', r.status === 200
    && meta(moss).h === HA && meta(moss).lk === 1 && !meta(moss).vis && (await mine(moss)).cape.listed === false
    && !(await gallery()).some((c) => c.h === HA) && (await link(HA)).status === 200);
  check('★ 管理的人看得到他是從連結穿的，整筆仍標不公開', await (async () => {
    const seen = (await (await worker.fetch(get('/admin/capes', aliceWeb), env)).json()).capes.find((c) => c.h === HA);
    return Boolean(seen) && seen.vis === 'unlisted' && seen.hidden === true
      && seen.owners.some((o) => o.id === moss.id && o.linked === true);
  })());
  check('不公開的照樣不能加最愛', (await send('/cape/star', { hash: HA, on: true }, moss)).status === 404);
  later(moss);
  check('他脫下：一覽上照樣沒有', (await send('/cape/restore', {}, moss)).status === 200 && !env.CORPUS.data.has('cape:' + moss.id)
    && !(await gallery()).some((c) => c.h === HA));
  check('亂寫的連結、沒有這件：404', (await link('nope')).status === 404 && (await link('f'.repeat(64))).status === 404);
  check('不認得的程度擋掉；沒登入不能改', (await send('/cape/visibility', { vis: 'secret' }, fern)).status === 400
    && (await send('/cape/visibility', { vis: 'public' })).status === 401);

  const row = env.CORPUS.data.get('cape:' + fern.id);
  const { vis: dropped, ...legacy } = row.metadata;
  row.metadata = { ...legacy, q: 1 };
  r = await link(HA);
  check('★ 舊資料（q＝1）讀成不公開：一覽沒有、連結看得到、/api/me 說不公開', !(await gallery()).some((c) => c.h === HA)
    && r.status === 200 && (await r.json()).cape.vis === 'unlisted' && (await mine(fern)).cape.vis === 'unlisted');
  check('★ 改過一次就換成新的寫法：q 拿掉、記 vis', (await send('/cape/visibility', { vis: 'private' }, fern)).status === 200
    && !('q' in meta(fern)) && meta(fern).vis === 'private');
  later(fern);
  check('★ 沒帶程度的上傳（從遊戲傳的）照原本的', (await upload(fern, mark(png(88, 68), 62))).status === 200 && meta(fern).vis === 'private');
  later(fern);
  await upload(fern, A);

  forgetBursts();
  const B = mark(png(88, 68), 63);
  const HB = await sha(B);
  check('他傳一件公開的', (await upload(moss, B, '?vis=public')).status === 200 && !meta(moss).vis
    && (await gallery()).some((c) => c.h === HB));
  later(fern);
  r = await send('/cape/wear', { hash: HB }, fern);
  check('★ 穿別人公開的那一件：她自己的程度不變（還是私人），一覽上那一筆只算公開的人', r.status === 200
    && meta(fern).vis === 'private' && meta(fern).p.h === HA
    && (await gallery()).find((c) => c.h === HB).ids.join() === moss.id);
  later(fern);
  check('★ 復原：換回自己那一件，程度照舊', (await send('/cape/restore', {}, fern)).status === 200
    && meta(fern).h === HA && meta(fern).vis === 'private');

  later(fern);
  const C = mark(png(88, 68), 64);
  const HC = await sha(C);
  check('她換一件新的：私人的那一件沒人穿了，開始倒數', (await upload(fern, C)).status === 200 && HA in idleNow()
    && idleNow()[HA].q === 1 && idleNow()[HA].vis === 'private');
  check('★ 倒數中的私人披風：拿到連結也看不到，管理的人看得到', (await link(HA)).status === 404
    && (await link(HA, staff)).status === 200);
  await send('/cape/visibility', { vis: 'unlisted' }, fern);
  later(fern);
  await upload(fern, A);
  check('★ 倒數中的不公開披風：拿到連結看得到（標著不公開、沒有人穿）', idleNow()[HC].vis === 'unlisted'
    && await (async () => { const got = await link(HC); const one = (await got.json()).cape; return got.status === 200 && one.vis === 'unlisted' && one.idle > 0 && one.ids.length === 0; })());
  check('★ 舊的開關（/cape/public）照樣能用：關掉＝不公開，打開＝公開',
    (await send('/cape/public', { on: true }, fern)).status === 200 && !meta(fern).vis
    && (await send('/cape/public', { on: false }, fern)).status === 200 && meta(fern).vis === 'unlisted');
  check('★ 帳號頁有三種程度與分享連結；披風一覽的管理篩選分成不公開與私人，有複製連結',
    ['id="vis"', 'data-v="public"', 'data-v="unlisted"', 'data-v="private"', 'id="sharebox"', "'/cape/visibility'"]
      .every((w) => pages['/account'].includes(w))
    && ['value="unlisted"', 'value="private"', 'id="share"', "'/capes/' + "].every((w) => pages['/capes'].includes(w))
    && !pages['/capes'].includes('value="hidden"'));
}

{
  const sha = async (bytes) => [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes))]
    .map((b) => b.toString(16).padStart(2, '0')).join('');
  const mark = (bytes, n) => { const out = bytes.slice(); out[40] = n; out[41] = 9; return out; };
  const RUE = { uuid: 'f3'.repeat(16), name: 'Rue' };
  const SAGE = { uuid: 'f4'.repeat(16), name: 'Sage' };
  const rueId = await publicId(RUE.uuid);
  const sageId = await publicId(SAGE.uuid);
  const rueGame = await issueToken(env, RUE.uuid, RUE.name, NOW);
  const rueWeb = await issueWebToken(env, RUE.uuid, RUE.name, NOW);
  const sageWeb = await issueWebToken(env, SAGE.uuid, SAGE.name, NOW);
  const THYME = { uuid: 'f5'.repeat(16), name: 'Thyme' };
  const thymeId = await publicId(THYME.uuid);
  const thymeWeb = await issueWebToken(env, THYME.uuid, THYME.name, NOW);
  const RUE_DC = '466666666666666666';
  const OTHER_DC = '477777777777777777';
  const upload = (token, bytes) => worker.fetch(new Request('https://x.test/api/cape', {
    method: 'PUT', body: bytes, headers: { authorization: 'Bearer ' + token } }), env);
  const send = (path, value, token) => worker.fetch(post(path, value, token), env);
  const later = (id) => { const row = env.CORPUS.data.get('cape:' + id); if (row) row.metadata.v -= 60000; forgetBursts(); };
  const has = (key) => env.CORPUS.data.has(key);
  const starsNow = () => JSON.parse((env.CORPUS.data.get('stars') || { value: '{}' }).value);

  check('沒登入不能刪', (await send('/me/delete', {})).status === 401);
  env.CORPUS.data.delete('own-index:ready');

  forgetBursts();
  const R1 = mark(png(88, 68), 71);
  const R2 = mark(png(88, 68), 72);
  const R3 = mark(png(88, 68), 73);
  const S1 = mark(png(88, 68), 74);
  const [H1, H2, H3, HS] = [await sha(R1), await sha(R2), await sha(R3), await sha(S1)];
  await send('/hello', { cape: true, rainbow: true, show: ['dev'] }, rueGame);
  await upload(rueGame, R1);
  later(rueId);
  await upload(sageWeb, S1);
  check('別人穿上她的第一件', (await send('/cape/wear', { hash: H1 }, thymeWeb)).status === 200);
  later(rueId);
  await upload(rueGame, R2);
  later(rueId);
  await upload(rueGame, R3);
  check('她的第二件沒有人穿了，在倒數', H2 in idleNow());
  later(rueId);
  check('她穿上別人的一件（自己的第三件收著）', (await send('/cape/wear', { hash: HS }, rueWeb)).status === 200
    && env.CORPUS.data.get('cape:' + rueId).metadata.p.h === H3);
  check('她把別人那一件加進最愛', (await (await send('/cape/star', { hash: HS, on: true }, rueWeb)).json()).stars >= 1);
  const starsBefore = starsNow()[HS];
  env.CORPUS.data.set('d:' + RUE_DC, { value: JSON.stringify({ n: 'Rue DC', u: RUE.uuid, m: RUE.name, t: NOW }), metadata: { n: 'Rue DC', u: RUE.uuid, m: RUE.name } });
  env.CORPUS.data.set('l:' + RUE.uuid, { value: RUE_DC, metadata: null });
  env.CORPUS.data.set('fb:' + 'c1'.repeat(8), { value: JSON.stringify({ id: 'c1'.repeat(8), uid: RUE_DC, name: 'Rue DC', msg: 'hello there', cat: 'other', st: 'open' }), metadata: { c: 'other' } });
  env.CORPUS.data.set('fb:' + 'c2'.repeat(8), { value: JSON.stringify({ id: 'c2'.repeat(8), uid: OTHER_DC, name: 'Other', msg: 'not hers', cat: 'other', st: 'open' }), metadata: { c: 'other' } });
  env.CORPUS.data.set('prev:' + HS + ':' + RUE.uuid + ':angled', { value: new Uint8Array([1]), metadata: { t: 'png' } });
  env.CORPUS.data.set('prev:' + HS, { value: new Uint8Array([1]), metadata: { t: 'png' } });
  for (const kept of ['ban:', 'review:', 'perk:', 'staff:u:', 'tl:u:']) {
    env.CORPUS.data.set(kept + RUE.uuid, { value: JSON.stringify({ n: RUE.name, t: NOW }), metadata: null });
  }
  let roster = await (await worker.fetch(get('/list'), env)).json();
  check('刪之前：名單上有她、葉子是彩色的', roster.users.includes(rueId) && roster.rainbow.includes(rueId));

  const PROP = 'tp:zh_tw:' + 'ab12'.repeat(4);
  const POLL = 'tq:zh_tw:' + 'cd34'.repeat(3);
  const OTHER_ID = 'u:' + 'f6'.repeat(16);
  env.CORPUS.data.set(PROP, { value: JSON.stringify({ lang: 'zh_tw', file: 'npc.json', key: null, src: 'Hi', base: '', status: 'open', t: NOW,
    items: [{ id: 'i1', text: '嗨', by: 'u:' + RUE.uuid, name: 'Rue', t: NOW, votes: ['u:' + RUE.uuid, OTHER_ID] },
      { id: 'i2', text: '你好', by: OTHER_ID, name: 'Other', t: NOW, votes: ['d:' + RUE_DC] }] }), metadata: { st: 'open' } });
  env.CORPUS.data.set(POLL, { value: JSON.stringify({ lang: 'zh_tw', term: 'Hi', by: 'd:' + RUE_DC, name: 'Rue DC', t: NOW, status: 'open',
    options: [{ id: 'o1', text: '嗨', by: 'd:' + RUE_DC, name: 'Rue DC', t: NOW, votes: ['d:' + RUE_DC] }] }), metadata: { st: 'open' } });
  r = await send('/me/delete', {}, rueGame);
  body = await r.json();
  check('★ 用模組的通行證刪：回 {"ok":true}', r.status === 200 && body.ok === true && Object.keys(body).join() === 'ok');
  roster = await (await worker.fetch(get('/list'), env)).json();
  check('★ 名單上馬上就沒有她（有裝的人、披風、彩色、徽章都清掉）', !roster.users.includes(rueId) && !(rueId in roster.capes)
    && !roster.rainbow.includes(rueId) && !(rueId in (roster.show || {})) && !has('u:' + RUE.uuid));
  check('★ 她的披風那一筆、使用紀錄、最愛都刪了', !has('cape:' + rueId) && !has('fav:' + RUE.uuid)
    && ![...env.CORPUS.data.keys()].some((k) => k.startsWith('hist:' + rueId + ':')));
  check('★ 只有她用的圖馬上刪（收著的第三件、倒數中的第二件），不必等七天', !has('blob:' + H3) && !has('blob:' + H2)
    && !(H2 in idleNow()) && !(H3 in idleNow()));
  check('★ 別人正在穿的圖留著（她傳的第一件、她穿的那件）', has('blob:' + H1) && has('blob:' + HS)
    && env.CORPUS.data.get('cape:' + thymeId).metadata.h === H1);
  check('★ 最愛的人數扣掉她那一票', (starsNow()[HS] || 0) === starsBefore - 1);
  check('★ 她的 skin 畫的預覽刪了，只有披風的那份留著', !has('prev:' + HS + ':' + RUE.uuid + ':angled') && has('prev:' + HS));
  check('★ 網站帳號與綁定一起刪；她送的回饋刪了，別人的留著', !has('d:' + RUE_DC) && !has('l:' + RUE.uuid)
    && !has('fb:' + 'c1'.repeat(8)) && has('fb:' + 'c2'.repeat(8)));
  check('★ 管理用的紀錄不給自己刪：封禁、審核、特別名單、管理員、譯者', ['ban:', 'review:', 'perk:', 'staff:u:', 'tl:u:']
    .every((kept) => has(kept + RUE.uuid)));
  {
    const code = await goneCode(RUE.uuid);
    const prop = JSON.parse(env.CORPUS.data.get(PROP).value);
    const poll = JSON.parse(env.CORPUS.data.get(POLL).value);
    check('★ 協作翻譯的提案與投票留著，名字拿掉、改記同一個代碼；別人的不動', /^[0-9a-f]{8}$/.test(code)
      && prop.items[0].name === null && prop.items[0].gone === code && prop.items[0].by === 'x:' + code && prop.items[0].text === '嗨'
      && prop.items[0].votes.join() === 'x:' + code + ',' + OTHER_ID && prop.items[1].name === 'Other' && prop.items[1].votes.join() === 'x:' + code
      && poll.name === null && poll.gone === code && poll.options[0].gone === code && poll.options[0].votes.join() === 'x:' + code
      && !JSON.stringify(prop.items).includes(RUE.uuid) && !JSON.stringify(poll.options).includes(RUE_DC));
    check('★ 帳號代號留在紀錄上（ghosts），刪除者是誰查得到', prop.ghosts[code].join() === 'u:' + RUE.uuid + ',d:' + RUE_DC
      && poll.ghosts[code].includes('d:' + RUE_DC) && env.CORPUS.data.get(PROP).metadata.n === 2);
    const publicView = await (await worker.fetch(get('/tr/prop?lang=zh_tw&id=' + 'ab12'.repeat(4)), env)).text();
    const staffView = await (await worker.fetch(get('/tr/prop?lang=zh_tw&id=' + 'ab12'.repeat(4), aliceWeb), env)).json();
    const pollView = await (await worker.fetch(get('/tr/poll?lang=zh_tw&id=' + 'cd34'.repeat(3)), env)).text();
    check('★ 一般人看不到帳號代號；開發者與管理員看得到', !publicView.includes('ghosts') && !publicView.includes(RUE.uuid)
      && publicView.includes(code) && staffView.ghosts[code].includes('u:' + RUE.uuid) && !pollView.includes(RUE_DC));
    check('anonymise：沒有這個人的紀錄不動', anonymise({ items: [{ by: 'u:x', votes: ['u:y'] }] }, ['u:z'], 'abcd1234') === false);
    check('★ 協作翻譯頁把刪除者畫成「已刪除的使用者 #代碼」', pages['/translate'].includes('whoOf(it, prop)')
      && TEXT.zh.js.tr.gone === '已刪除的使用者 #{id}' && LANGS.every((l) => TEXT[l].js.tr.gone.includes('#{id}')));
    env.CORPUS.data.delete(PROP);
    env.CORPUS.data.delete(POLL);
  }
  check('★ 刪完再報到就是一個新的人（模組關掉開關就不會再報到）', (await send('/hello', { cape: true }, rueGame)).status === 200
    && has('u:' + RUE.uuid));
  for (const kept of ['ban:', 'review:', 'perk:', 'staff:u:', 'tl:u:', 'u:']) env.CORPUS.data.delete(kept + RUE.uuid);
  env.CORPUS.data.delete('fb:' + 'c2'.repeat(8));

  const lone = '488888888888888888';
  env.CORPUS.data.set('d:' + lone, { value: JSON.stringify({ n: 'Only DC' }), metadata: { n: 'Only DC' } });
  r = await send('/me/delete', {}, await issueDiscordToken(env, lone, NOW));
  check('★ 只用 Discord 登入、還沒綁遊戲帳號：網站帳號刪掉', r.status === 200 && (await r.json()).ok === true && !has('d:' + lone));
  check('刪掉之後那張通行證就沒用了', (await worker.fetch(get('/me', await issueDiscordToken(env, lone, NOW)), env)).status === 401);
  check('★ 帳號頁有刪除資料的按鈕與確認視窗', ['id="wipe"', 'id="wipe-open"', 'id="wipe-go"', "'/me/delete'"]
    .every((w) => pages['/account'].includes(w)));
  check('★ 隱私政策講了怎麼刪、留多久、同意與不賣資料（中英）',
    ['/account', '7', '14', '180'].every((w) => pages['/privacy'].includes(w) && english['/privacy'].includes(w))
    && english['/privacy'].includes('consent') && english['/privacy'].includes('never sell')
    && english['/privacy'].includes('Deleted user #code') && pages['/privacy'].includes('已刪除的使用者 #代碼'));
  check('★ 服務條款八種語言都講了可以刪資料、連到隱私政策', LANGS.every((lang) => TEXT[lang]['terms.body'].includes('href="/privacy"')));
  check('dropFromList：每一欄都拿掉；本來就沒有的回 null', (() => {
    const list = { t: 1, users: ['a', 'b'], capes: { a: 3 }, hidden: ['a'], rainbow: ['a'], show: { a: 'dev' } };
    const out = dropFromList(list, 'a');
    return out.users.join() === 'b' && !('a' in out.capes) && !out.hidden.length && !out.rainbow.length
      && !('a' in out.show) && out.t === 1 && dropFromList(out, 'a') === null && list.users.length === 2;
  })());
}

// ---- ★ 協作翻譯（使用者 2026-10-16：像 Crowdin 一樣提交、投票，管理員定案） ----
{
  const trPost = (path, bodyValue, token) => worker.fetch(post(path, bodyValue, token), env);
  const trGet = (path, token) => worker.fetch(get(path, token), env);
  const hadToken = env.GITHUB_TOKEN;
  env.GITHUB_TOKEN = 'ghtok';

  check('佔位符：照原文的 {#}{p}{u} 順序、{~} 數量、行數；顏色標記照現在的譯文',
    placeholderProblem('Talk to {u} at {p}', '到 {p} 找 {u}', '') === 'placeholders'
    && placeholderProblem('{#}Kill {~} mobs', '{#}擊殺 {~} 隻怪物', '') === null
    && placeholderProblem('{#}Kill {~} mobs in {~}s', '{#}在 {~2} 秒內擊殺 {~1} 隻怪物', '') === null
    && placeholderProblem('{#}Kill {~} mobs in {~}s', '{#}在 {~2} 秒內擊殺 {~} 隻怪物', '') === 'numbering'
    && placeholderProblem('Kill {~} mobs', '擊殺怪物', '') === 'numbers'
    && placeholderProblem('A\nB', '甲乙', '') === 'lines'
    && placeholderProblem('Kill it', '{c1}殺{/}掉', '{c1}擊殺{/}') === null
    && placeholderProblem('Kill it', '殺掉', '{c1}擊殺{/}') === 'colours'
    && placeholderProblem('Hi {u}', '嗨', '') === null && placeholderProblem('{u} gave {u} a {#}', '{u} 給了 {#}', '') === null
    && placeholderProblem('x', '   ', '') === 'empty');

  const flatFile = '{\r\n "_note": "x",\r\n "Kill {~} mobs": "擊殺 {~} 隻怪物",\r\n "Hello": "哈囉"\r\n}\r\n';
  const replaced = replaceTranslation(flatFile, null, 'Hello', '你好');
  check('改扁平檔：只換那一行的值，換行照原檔', replaced === flatFile.replace('"哈囉"', '"你好"')
    && replaceTranslation(flatFile, null, 'Nope', 'x') === null);
  const workFile = '{\n "entries": {\n  "Q#001": {\n   "src": "Hi",\n   "dst": "嗨",\n   "quest": "Q"\n  },\n'
    + '  "Q#002": {\n   "src": "Bye",\n   "dst": "",\n   "quest": "Q"\n  }\n }\n}\n';
  check('改任務檔：照條目編號換 dst，別的條目不動', replaceTranslation(workFile, 'Q#002', 'Bye', '再見')
    === workFile.replace('"dst": ""', '"dst": "再見"') && replaceTranslation(workFile, 'Q#009', 'x', 'y') === null);

  let r = await trGet('/tr/me', bobNow);
  let me = await r.json();
  check('★ 一般玩家：看得到、不是譯者、不能定案', me.signedIn === true && me.langs.length === 0 && me.decide === false);
  check('沒登入也回（網頁照這個決定按鈕）', (await (await trGet('/tr/me')).json()).signedIn === false);
  const sample = { lang: 'ja_jp', file: 'npc.json', key: null, src: 'Hello {u}!', base: 'こんにちは、{u}！', text: 'やあ、{u}！' };
  check('★ 不是譯者不能提案', (await trPost('/tr/propose', sample, bobNow)).status === 403);

  check('一般玩家不能指派譯者', (await trPost('/admin/translator', { who: BOB.id, langs: ['ja_jp'] }, bobNow)).status === 403);
  r = await trPost('/admin/translator', { who: dashed(BOB.id), langs: ['ja_jp', 'xx_yy'], note: '日文組' }, aliceWeb);
  check('★ 管理的人指派譯者（只收認得的語言）', r.status === 200
    && JSON.parse(env.CORPUS.data.get('tl:u:' + BOB.id).value).langs.join() === 'ja_jp');
  check('沒有語言不收', (await trPost('/admin/translator', { who: 'Carol', langs: [] }, aliceWeb)).status === 400);
  me = await (await trGet('/tr/me', bobNow)).json();
  check('指派之後 /tr/me 看得到語言', me.langs.join() === 'ja_jp' && me.decide === false);

  check('★ 佔位符不對，送出當下就擋', (await (await trPost('/tr/propose', { ...sample, text: 'やあ、{u}{u}！' }, bobNow)).json()).why === 'placeholders');
  check('★ 只能提自己負責的語言', (await trPost('/tr/propose', { ...sample, lang: 'zh_tw' }, bobNow)).status === 403);
  r = await trPost('/tr/propose', sample, bobNow);
  const made = await r.json();
  check('★ 譯者提案：建立、自己先投一票', r.status === 200 && made.prop.items.length === 1
    && made.prop.items[0].votes.length === 1 && made.prop.items[0].name === BOB.name);
  r = await trPost('/tr/propose', { ...sample, text: 'どうも、{u}！' }, aliceWeb);
  let prop = (await r.json()).prop;
  check('開發者也能提，同一句變兩個譯法', r.status === 200 && prop.items.length === 2);
  r = await trPost('/tr/propose', { ...sample, text: 'やあ、{u}！' }, aliceWeb);
  prop = (await r.json()).prop;
  check('★ 提了一樣的字就當成投那一票，而且一個人只留一票', prop.items.length === 2
    && prop.items[0].votes.length === 2 && prop.items[1].votes.length === 0);
  r = await trPost('/tr/vote', { lang: 'ja_jp', id: made.id, item: prop.items[0].id }, aliceWeb);
  prop = (await r.json()).prop;
  check('再按一次收回', prop.items[0].votes.length === 1);
  const listed = (await (await trGet('/tr/props?lang=ja_jp')).json()).props;
  check('列表不用一筆一筆讀（metadata 有檔名、原文、票數）', listed.length === 1 && listed[0].f === 'npc.json'
    && listed[0].s === 'Hello {u}!' && listed[0].st === 'open');

  check('★ 譯者不能定案', (await trPost('/tr/accept', { lang: 'ja_jp', id: made.id, item: prop.items[0].id }, bobNow)).status === 403);
  githubCalls.length = 0;
  githubFile = '{\n "Hello {u}!": "こんにちは、{u}！"\n}\n';
  r = await trPost('/tr/accept', { lang: 'ja_jp', id: made.id, item: prop.items[0].id }, aliceWeb);
  const accepted = await r.json();
  const putCall = githubCalls.find((c) => c.method === 'PUT');
  check('★ 管理的人定案：開分支、改那一行、開 PR', r.status === 200 && accepted.url.endsWith('/pull/9999')
    && accepted.prop.status === 'accepted'
    && githubCalls.some((c) => c.path === '/git/refs' && c.body.ref.startsWith('refs/heads/translate/ja-jp-'))
    && putCall && putCall.path.endsWith('/translations/ja_jp/npc.json')
    && Buffer.from(putCall.body.content, 'base64').toString('utf8').includes('"Hello {u}!": "やあ、{u}！"'));

  githubCalls.length = 0;
  githubFile = workFile;
  await trPost('/admin/translator', { who: dashed(BOB.id), langs: ['ja_jp', 'zh_tw'] }, aliceWeb);
  r = await trPost('/tr/propose', { lang: 'zh_tw', file: 'quest/q.json', key: 'Q#002', src: 'Bye', base: '', text: '再見' }, bobNow);
  const quest = await r.json();
  r = await trPost('/tr/accept', { lang: 'zh_tw', id: quest.id, item: quest.prop.items[0].id }, aliceWeb);
  const puts = githubCalls.filter((c) => c.method === 'PUT').map((c) => c.path);
  check('★ 任務檔的句子連合併檔一起改（PR 的檢查才不會紅）', r.status === 200 && puts.length === 2
    && puts.some((p) => p.endsWith('/zh_tw/quest/q.json')) && puts.some((p) => p.endsWith('/zh_tw/quest-dialogue.json')));
  {
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    const dayBranch = 'translate/ja-jp-' + today;
    const proposeJa = async (src) => {
      githubFile = '{\n "' + src + '": ""\n}\n';
      const made = await (await trPost('/tr/propose', { lang: 'ja_jp', file: 'npc.json', src, base: '', text: '訳' + src }, bobNow)).json();
      return made;
    };
    const accept = (made) => trPost('/tr/accept', { lang: 'ja_jp', id: made.id, item: made.prop.items[0].id }, aliceWeb);
    const stored = (made) => JSON.parse(env.CORPUS.data.get('tp:ja_jp:' + made.id).value);

    githubBranches.add(dayBranch);
    githubOpenPulls = [];
    githubCalls.length = 0;
    let made = await proposeJa('Merged already');
    r = await accept(made);
    let answer = await r.json();
    const branchMade = githubCalls.find((c) => c.path === '/git/refs');
    const pullMade = githubCalls.find((c) => c.method === 'POST' && c.path === '/pulls');
    const putTo = githubCalls.find((c) => c.method === 'PUT');
    check('★ 當天的分支已經合併或關掉：開新的分支與 PR，不提交到舊分支', r.status === 200
      && branchMade && branchMade.body.ref.startsWith('refs/heads/' + dayBranch + '-')
      && putTo && putTo.body.branch === pullMade.body.head && pullMade.body.head !== dayBranch
      && answer.url.endsWith('/pull/9999') && stored(made).done.pr.endsWith('/pull/9999'));
    check('查開著的 PR 用倉庫設定的擁有者', githubCalls.some((c) => c.path.startsWith('/pulls?head=' + encodeURIComponent('LyuChaCha:' + dayBranch))));

    githubOpenPulls = [{ html_url: 'https://github.com/LyuChaCha/WynnChaYuan/pull/4242' }];
    githubCalls.length = 0;
    made = await proposeJa('Still open');
    r = await accept(made);
    answer = await r.json();
    check('★ 當天的 PR 還開著：加進同一個分支，不另開 PR', r.status === 200 && answer.url.endsWith('/pull/4242')
      && !githubCalls.some((c) => c.method === 'POST' && (c.path === '/pulls' || c.path === '/git/refs'))
      && githubCalls.find((c) => c.method === 'PUT').body.branch === dayBranch);

    githubOpenPulls = [];
    githubPullStatus = 'no url';
    made = await proposeJa('No link');
    r = await accept(made);
    check('★ 拿不到 PR 網址就不標成已定案', r.status === 502 && stored(made).status === 'open' && !stored(made).done);
    githubPullStatus = 201;

    githubBranches.delete(dayBranch);
    githubOpenPulls = [];
    githubPullStatus = 422;
    made = await proposeJa('Pull fails');
    r = await accept(made);
    check('★ 開 PR 失敗就不標成已定案', r.status === 502 && stored(made).status === 'open');
    githubPullStatus = 201;
  }

  check('改過指派：重送一次就覆寫語言', JSON.parse(env.CORPUS.data.get('tl:u:' + BOB.id).value).langs.join() === 'ja_jp,zh_tw');

  r = await trPost('/tr/poll', { lang: 'ja_jp', term: 'Weaken Enemy', note: 'Lootrun の加持', options: ['敵弱体化', '敵を弱体化'] }, bobNow);
  const poll = await r.json();
  check('★ 譯者開術語投票', r.status === 200 && poll.poll.options.length === 2 && poll.poll.status === 'open');
  check('不是那個語言的譯者不能開', (await trPost('/tr/poll', { lang: 'ru_ru', term: 'X' }, bobNow)).status === 403);
  let p = (await (await trPost('/tr/poll/vote', { lang: 'ja_jp', id: poll.id, option: poll.poll.options[0].id }, bobNow)).json()).poll;
  p = (await (await trPost('/tr/poll/vote', { lang: 'ja_jp', id: poll.id, option: poll.poll.options[1].id }, bobNow)).json()).poll;
  check('★ 一人一票：投別的就移過去', p.options[0].votes.length === 0 && p.options[1].votes.length === 1);
  p = (await (await trPost('/tr/poll/option', { lang: 'ja_jp', id: poll.id, text: '弱体化' }, aliceWeb)).json()).poll;
  check('加候選＝投它', p.options.length === 3 && p.options[2].votes.length === 1);
  check('★ 譯者不能定案術語', (await trPost('/tr/poll/decide', { lang: 'ja_jp', id: poll.id, option: p.options[1].id }, bobNow)).status === 403);
  p = (await (await trPost('/tr/poll/decide', { lang: 'ja_jp', id: poll.id, option: p.options[1].id }, aliceWeb)).json()).poll;
  check('★ 管理的人定案術語，記下採用哪一個', p.status === 'decided' && p.decided.text === '敵を弱体化');
  check('定案之後不能再投', (await trPost('/tr/poll/vote', { lang: 'ja_jp', id: poll.id, option: p.options[0].id }, bobNow)).status === 409);
  const polls = (await (await trGet('/tr/polls?lang=ja_jp')).json()).polls;
  check('投票列表照 metadata', polls.length === 1 && polls[0].s === 'Weaken Enemy' && polls[0].st === 'decided');

  r = await trGet('/tr/index/zh_tw');
  check('語料索引從 translate-index 分支代讀', r.status === 200 && (r.headers.get('content-type') || '').includes('json'));
  check('不認得的語言不讀', (await trGet('/tr/index/xx_yy')).status === 404);

  const people = (await (await trGet('/admin/translators', aliceWeb)).json()).translators;
  check('管理頁看得到譯者名單', people.some((t) => t.key === 'u:' + BOB.id && t.langs.includes('zh_tw') && t.by === ALICE.name));
  check('一般玩家看不到', (await trGet('/admin/translators', bobNow)).status === 403);
  await trPost('/admin/untranslator', { key: 'u:' + BOB.id }, aliceWeb);
  check('移除之後馬上不能提案', (await trPost('/tr/propose', sample, bobNow)).status === 403);
  {
    const LINKED = JSON.stringify({ sections: [
      { role: '開發者', key: 'credits.role.developers', members: [{ name: 'A', mc: 'Alice_Dev', teams: ['zh_tw'] }] },
      { role: '贊助者', key: 'credits.role.sponsors', members: [{ name: 'C', mc: 'Carol' }] },
      { role: '貢獻者', key: 'credits.role.contributors', members: [{ name: 'C', mc: 'Carol', areas: ['testing'] }] },
      { role: '資料來源', key: 'credits.role.sources', members: [{ name: 'Carol 的資料', mc: 'Carol' }] }] }, null, 2) + '\n';
    const added = addMemberTeams(LINKED, 'carol', ['ko_kr', 'ja_jp', 'xx_yy']);
    const linkedDoc = JSON.parse(added.text);
    check('★ addMemberTeams：名冊上同一個人的每一列都加上語言，照固定順序，項目與資料來源不動',
      linkedDoc.sections[1].members[0].teams.join() === 'ja_jp,ko_kr' && linkedDoc.sections[2].members[0].teams.join() === 'ja_jp,ko_kr'
      && linkedDoc.sections[2].members[0].areas.join() === 'testing' && !('teams' in linkedDoc.sections[3].members[0]));
    check('addMemberTeams：已經有的語言不重複，只加沒有的；都有了算沒改', addMemberTeams(LINKED, 'Alice_Dev', ['ja_jp']).text.includes('"ja_jp"')
      && JSON.parse(addMemberTeams(LINKED, 'Alice_Dev', ['ja_jp']).text).sections[0].members[0].teams.join() === 'zh_tw,ja_jp'
      && addMemberTeams(LINKED, 'Alice_Dev', ['zh_tw']).why === 'unchanged');
    check('addMemberTeams：不在名冊、怪名字、沒有認得的語言都不改', addMemberTeams(LINKED, 'Nobody', ['ja_jp']).why === 'not found'
      && addMemberTeams(LINKED, 'bad name!', ['ja_jp']).why === 'bad who' && addMemberTeams(LINKED, 'Carol', ['xx_yy']).why === 'bad team');

    const tlEnv = { ...env, GITHUB_TOKEN: 't' };
    const tlPost = (body, token, which = tlEnv) => worker.fetch(post(body.key ? '/admin/untranslator' : '/admin/translator', body, token), which);
    githubFile = LINKED;
    githubBranches.clear();
    githubOpenPulls = [];
    githubCalls.length = 0;
    let res = await tlPost({ who: 'Carol', langs: ['ko_kr', 'ja_jp'] }, aliceWeb);
    let out = await res.json();
    const linkPut = githubCalls.find((c) => c.method === 'PUT');
    const linkDoc = linkPut ? JSON.parse(Buffer.from(linkPut.body.content, 'base64').toString('utf8')) : null;
    check('★ 指派名冊上的人當譯者：語言一起加進他的翻譯團隊，開當天的 PR', res.status === 200 && out.ok === true
      && out.team && out.team.url === 'https://github.com/LyuChaCha/WynnChaYuan/pull/9999' && out.team.added === false
      && linkDoc && linkDoc.sections[1].members[0].teams.join() === 'ja_jp,ko_kr' && /^credits\/teams-\d{8}$/.test(linkPut.body.branch)
      && env.CORPUS.data.has('tl:n:carol'));
    githubBranches.add(linkPut ? linkPut.body.branch : '');
    githubOpenPulls = [{ html_url: 'https://github.com/LyuChaCha/WynnChaYuan/pull/42' }];
    githubCalls.length = 0;
    out = await (await tlPost({ who: 'Carol', langs: ['ru_ru'] }, aliceWeb)).json();
    check('★ 同一天再指派：加進同一個 PR', out.team && out.team.added === true && out.team.url.endsWith('/pull/42')
      && !githubCalls.some((c) => c.method === 'POST'));
    githubCalls.length = 0;
    out = await (await tlPost({ who: 'Alice_Dev', langs: ['ja_jp'] }, aliceWeb)).json();
    check('團隊裡已經有那個語言：不碰 GitHub，回他現在的團隊', out.ok === true && out.team && out.team.teams.join() === 'zh_tw,ja_jp'
      && githubCalls.length === 0);
    out = await (await tlPost({ who: 'Dave_X', langs: ['ja_jp'] }, aliceWeb)).json();
    check('★ 不在名冊上的人：照樣是譯者，不動 credits.json', out.ok === true && out.team === null && githubCalls.length === 0
      && env.CORPUS.data.has('tl:n:dave_x'));
    res = await tlPost({ who: 'Carol', langs: ['es_es'] }, aliceWeb, { ...env, GITHUB_TOKEN: undefined });
    out = await res.json();
    check('★ 沒設 GITHUB_TOKEN：譯者照樣加好，只說團隊沒改成', res.status === 200 && out.ok === true
      && out.team.why === 'github not configured' && JSON.parse(env.CORPUS.data.get('tl:n:carol').value).langs.join() === 'es_es');
    check('一般玩家照樣不能指派（也不會碰到名冊）', (await tlPost({ who: 'Carol', langs: ['ja_jp'] }, bobNow)).status === 403
      && githubCalls.length === 0);
    const listed = (await (await worker.fetch(get('/admin/translators', aliceWeb), tlEnv)).json()).translators;
    const row = (name) => listed.find((t) => t.name === name);
    check('★ 譯者名單帶著名冊上的翻譯團隊；不在名冊上的是 null', row('Carol') && Array.isArray(row('Carol').team)
      && row('Alice_Dev').team.join() === 'zh_tw,ja_jp' && row('Dave_X').team === null);
    githubCalls.length = 0;
    res = await tlPost({ key: 'n:carol' }, aliceWeb);
    check('★ 移除譯者不會動到翻譯團隊', res.status === 200 && !env.CORPUS.data.has('tl:n:carol') && githubCalls.length === 0);
    await tlPost({ key: 'n:alice_dev' }, aliceWeb);
    await tlPost({ key: 'n:dave_x' }, aliceWeb);
    githubBranches.clear();
    githubOpenPulls = [];
  }
  {
    const merged = mergeLangTeam([
      { role: 'developers', members: [{ name: 'A', mc: 'Alice_Dev', teams: ['zh_tw'] }] },
      { role: 'sponsors', members: [{ name: 'C', mc: 'Carol' }] },
      { role: 'contributors', members: [{ name: 'C', mc: 'Carol', teams: ['ja_jp'], areas: ['testing'] }, { name: 'NoId' }] }], [
      { key: 'u:' + '1'.repeat(32), name: 'carol', langs: ['ko_kr'], note: '韓文組', t: 5, by: 'A' },
      { key: 'n:alice_dev', name: 'Alice_Dev', langs: ['zh_tw'], t: 3 },
      { key: 'd:123456789012345678', name: null, discord: 'Dee', langs: ['es_es'] },
      { key: 'n:solo', name: 'Solo', langs: ['tr_tr', 'ja_jp'] }]);
    const who = (name) => merged.find((p) => p.name === name);
    check('★ mergeLangTeam：名冊與譯者照遊戲 ID 併成一人一列（不分大小寫，多個身分算一人）', merged.length === 5
      && who('C').roles.join() === 'sponsors,contributors' && who('C').rights.keys[0] === 'u:' + '1'.repeat(32)
      && who('C').teams.join() === 'ja_jp' && who('C').areas.join() === 'testing' && who('C').rights.note === '韓文組');
    check('★ mergeLangTeam：兩邊都有又對不上才標不一致；只有一邊的不算', who('C').mismatch === true && who('A').mismatch === false
      && who('NoId').rights === null && who('NoId').mismatch === false && who('Solo').listed === false && who('Solo').mismatch === false);
    check('mergeLangTeam：不在名冊的譯者照樣列出；只有 Discord 的沒有遊戲 ID', who('Dee').listed === false && who('Dee').mc === null
      && who('Dee').rights.langs.join() === 'es_es' && who('Solo').mc === 'Solo' && who('Solo').rights.langs.join() === 'ja_jp,tr_tr');

    const UNI = JSON.stringify({ sections: [
      { role: '開發者', key: 'credits.role.developers', members: [{ name: 'A', mc: 'Alice_Dev', teams: ['zh_tw'] }] },
      { role: '贊助者', key: 'credits.role.sponsors', members: [{ name: 'C', mc: 'Carol' }] },
      { role: '貢獻者', key: 'credits.role.contributors', members: [{ name: 'C', mc: 'Carol', teams: ['ja_jp'], areas: ['testing'] }] }] }, null, 2) + '\n';
    const cleared = JSON.parse(setPersonTeams(UNI, 'carol', [], null).text);
    check('★ setPersonTeams：同一個人的每一列一起改；null 的那一欄不動；清空就拿掉', !('teams' in cleared.sections[2].members[0])
      && cleared.sections[2].members[0].areas.join() === 'testing' && !('teams' in cleared.sections[1].members[0])
      && JSON.parse(setPersonTeams(UNI, 'Carol', ['ko_kr'], ['code']).text).sections[1].members[0].areas.join() === 'code'
      && setPersonTeams(UNI, 'Alice_Dev', ['zh_tw'], null).why === 'unchanged' && setPersonTeams(UNI, 'Nobody', [], null).why === 'not found');
    const joined = JSON.parse(addMember(CREDITS, { section: 'contributors', name: 'J', mc: 'Join_1', teams: ['ja_jp', 'zh_tw'], areas: ['code'] }).text);
    check('★ 新增成員可以一起帶翻譯團隊與貢獻項目（「加進名冊」用）', JSON.stringify(joined.sections[1].members[0])
      === JSON.stringify({ name: 'J', mc: 'Join_1', teams: ['zh_tw', 'ja_jp'], areas: ['code'] })
      && addMember(CREDITS, { section: 'contributors', name: 'J', mc: 'Join_1', teams: ['xx'] }).why === 'bad team');

    const ltEnv = { ...env, GITHUB_TOKEN: 't' };
    const ltPost = (body, token, which = ltEnv) => worker.fetch(post('/admin/langteam', body, token), which);
    const tl = (key) => (env.CORPUS.data.has('tl:' + key) ? JSON.parse(env.CORPUS.data.get('tl:' + key).value) : null);
    const putOf = () => {
      const put = githubCalls.find((c) => c.method === 'PUT');
      return put ? JSON.parse(Buffer.from(put.body.content, 'base64').toString('utf8')) : null;
    };
    check('★ 一般玩家看不到也改不了翻譯團隊', (await worker.fetch(get('/admin/langteam', bobNow), ltEnv)).status === 403
      && (await ltPost({ who: 'Carol', langs: ['ja_jp'] }, bobNow)).status === 403);
    env.CORPUS.data.set('tl:n:solo_tl', { value: JSON.stringify({ langs: ['tr_tr'], m: 'Solo_TL', t: 1, by: 'A' }), metadata: null });
    const listing = await (await worker.fetch(get('/admin/langteam', aliceWeb), ltEnv)).json();
    check('★ GET /admin/langteam：名冊的人與譯者併在一起', listing.credits === true
      && listing.people.some((p) => p.mc === 'Alice_Dev' && p.listed && p.teams.join() === 'zh_tw,ja_jp')
      && listing.people.some((p) => p.mc === 'Solo_TL' && !p.listed && p.rights.langs.join() === 'tr_tr'));

    githubFile = UNI;
    githubBranches.clear();
    githubOpenPulls = [];
    githubCalls.length = 0;
    let res = await ltPost({ who: 'Carol', langs: ['ko_kr', 'ja_jp'], note: '日韓' }, aliceWeb);
    let out = await res.json();
    let doc3 = putOf();
    check('★ 勾語言：馬上給譯者權限，名冊的翻譯團隊放進當天的 PR（同一個人的每一列都改）', res.status === 200 && out.listed === true
      && tl('n:carol').langs.join() === 'ja_jp,ko_kr' && tl('n:carol').n === '日韓' && out.rights.keys.join() === 'n:carol'
      && out.team.url.endsWith('/pull/9999') && doc3.sections[1].members[0].teams.join() === 'ja_jp,ko_kr'
      && doc3.sections[2].members[0].teams.join() === 'ja_jp,ko_kr' && doc3.sections[2].members[0].areas.join() === 'testing');
    env.CORPUS.data.delete('tl:n:carol');
    env.CORPUS.data.set('tl:u:' + '2'.repeat(32), { value: JSON.stringify({ langs: ['ja_jp'], m: 'Carol', n: '舊備註', t: 1, by: 'A' }), metadata: null });
    githubCalls.length = 0;
    res = await ltPost({ who: 'carol', langs: ['ja_jp', 'zh_cn'] }, aliceWeb);
    check('★ 已經有照 UUID 存的譯者：改那一筆，不另外多存一筆；沒送備註就留著原本的', res.status === 200 && tl('u:' + '2'.repeat(32)).langs.join() === 'zh_cn,ja_jp'
      && tl('u:' + '2'.repeat(32)).m === 'Carol' && tl('u:' + '2'.repeat(32)).n === '舊備註' && !tl('n:carol'));
    githubCalls.length = 0;
    res = await ltPost({ who: 'Carol', langs: [] }, aliceWeb);
    out = await res.json();
    doc3 = putOf();
    check('★ 拿掉全部語言：譯者權限與名冊的團隊一起拿掉', res.status === 200 && !tl('u:' + '2'.repeat(32)) && out.rights === null
      && doc3 && !('teams' in doc3.sections[2].members[0]) && doc3.sections[2].members[0].areas.join() === 'testing');
    env.CORPUS.data.set('tl:n:carol', { value: JSON.stringify({ langs: ['ko_kr'], m: 'Carol', t: 1, by: 'A' }), metadata: null });
    githubCalls.length = 0;
    res = await ltPost({ who: 'Carol', areas: ['code', 'testing'] }, aliceWeb);
    doc3 = putOf();
    check('★ 只改貢獻項目：譯者權限與團隊都不動', res.status === 200 && tl('n:carol').langs.join() === 'ko_kr'
      && doc3.sections[2].members[0].areas.join() === 'testing,code' && doc3.sections[2].members[0].teams.join() === 'ja_jp');
    githubCalls.length = 0;
    res = await ltPost({ who: 'Dave_Y', langs: ['es_es'] }, aliceWeb);
    out = await res.json();
    check('★ 不在名冊上的人：只給譯者權限，不碰 GitHub', res.status === 200 && out.listed === false && out.team === null
      && tl('n:dave_y').langs.join() === 'es_es' && githubCalls.length === 0);
    check('不在名冊上的人不能登記貢獻項目', (await (await ltPost({ who: 'Dave_Y', areas: ['code'] }, aliceWeb)).json()).why === 'not listed');
    res = await ltPost({ who: 'Carol', langs: ['ru_ru'] }, aliceWeb, { ...env, GITHUB_TOKEN: undefined });
    out = await res.json();
    check('★ 沒設 GITHUB_TOKEN：譯者權限照樣改好，只說名冊沒改成', res.status === 200 && tl('n:carol').langs.join() === 'ru_ru'
      && out.team.why === 'github not configured');
    check('★ 送來的東西不對都擋', (await (await ltPost({ who: 'Carol', langs: ['xx_yy'] }, aliceWeb)).json()).why === 'bad team'
      && (await (await ltPost({ who: 'Carol', areas: 'code' }, aliceWeb)).json()).why === 'bad area'
      && (await (await ltPost({ who: 'Carol' }, aliceWeb)).json()).why === 'bad request'
      && (await (await ltPost({ who: 'bad name!', langs: [] }, aliceWeb)).json()).why === 'bad who'
      && (await (await ltPost({ who: 'Carol', langs: [], note: 'x'.repeat(61) }, aliceWeb)).json()).why === 'bad note');
    for (const key of ['n:carol', 'n:dave_y', 'n:solo_tl']) env.CORPUS.data.delete('tl:' + key);
    githubBranches.clear();
    githubOpenPulls = [];
  }
  // ---- Discord 機器人：簽名、三個指令、註冊 ----
  {
    const pair = await crypto.subtle.generateKey({ name: 'Ed25519' }, true, ['sign', 'verify']);
    const raw = new Uint8Array(await crypto.subtle.exportKey('raw', pair.publicKey));
    env.DISCORD_PUBLIC_KEY = Buffer.from(raw).toString('hex');
    const signed = async (body, tamper) => {
      const text = JSON.stringify(body);
      const ts = String(Math.floor(Date.now() / 1000));
      const sig = Buffer.from(await crypto.subtle.sign('Ed25519', pair.privateKey, new TextEncoder().encode(ts + text))).toString('hex');
      return new Request('https://x.test/api/discord/interactions', {
        method: 'POST', body: tamper ? text.replace('1', '2') : text,
        headers: { 'x-signature-ed25519': sig, 'x-signature-timestamp': ts },
      });
    };
    const command = (name, options = [], locale = 'zh-TW') => ({
      type: 2, application_id: 'app1', token: 'tok1', locale, data: { name, options },
    });
    check('★ 簽名不對就 401（Discord 會拿壞簽名來測）', (await worker.fetch(await signed({ type: 1 }, true), env)).status === 401);
    check('沒有簽名也 401', (await worker.fetch(new Request('https://x.test/api/discord/interactions', { method: 'POST', body: '{"type":1}' }), env)).status === 401);
    r = await worker.fetch(await signed({ type: 1 }), env);
    check('★ PING 回 PONG', r.status === 200 && (await r.json()).type === 1);

    r = await worker.fetch(await signed(command('status')), env);
    let reply = await r.json();
    check('★ /status 回網站狀態（繁中）', reply.type === 4 && reply.data.embeds[0].title === '網站狀態'
      && reply.data.embeds[0].fields.length === 5);
    r = await worker.fetch(await signed(command('status', [], 'en-US')), env);
    check('英文介面回英文', (await r.json()).data.embeds[0].title === 'Website status');

    const capeId = 'c'.repeat(24);
    await env.CORPUS.put('cape:' + capeId, '', { metadata: { h: 'd'.repeat(64), t: 'png', n: 'Dottie_9', v: 1760000000000 } });
    r = await worker.fetch(await signed(command('cape', [{ name: 'player', value: 'dottie_9' }])), env);
    reply = await r.json();
    check('★ /cape 不分大小寫找到人，附披風的預覽圖（跟一覽的卡片一樣）', reply.data.embeds && reply.data.embeds[0].title.includes('Dottie_9')
      && reply.data.embeds[0].image.url.endsWith('/api/cape-preview/' + 'd'.repeat(64)));
    await env.CORPUS.put('cape:' + capeId, '', { metadata: { h: 'd'.repeat(64), t: 'png', n: 'Dottie_9', v: 1, q: 1 } });
    reply = await (await worker.fetch(await signed(command('cape', [{ name: 'player', value: 'Dottie_9' }])), env)).json();
    check('★ 不公開的披風不給圖', !reply.data.embeds && reply.data.content.includes('不公開'));
    await env.CORPUS.put('cape:' + capeId, '', { metadata: { h: 'd'.repeat(64), t: 'png', n: 'Dottie_9', v: 1, r: 1 } });
    reply = await (await worker.fetch(await signed(command('cape', [{ name: 'player', value: 'Dottie_9' }])), env)).json();
    check('★ 等審核的披風不給圖', !reply.data.embeds && reply.data.content.includes('審核'));

    // ---- 披風預覽圖：切背面、放大、墊底；PNG 與 GIF 都要能解、能編 ----
    {
      // 22×17 的範本：背面那塊（1,1)–(11,17) 塗紅，其他塗藍
      const W = 22, H = 17, rgb = new Uint8Array(W * H * 3);
      for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
        const o = (y * W + x) * 3;
        const face = x >= 1 && x < 11 && y >= 1;
        rgb[o] = face ? 220 : 20; rgb[o + 1] = 30; rgb[o + 2] = face ? 30 : 220;
      }
      const png = await encodePng(W, H, rgb);
      const back = await decodePng(png);
      check('PNG 編了再解回來一模一樣', back.width === W && back.height === H && back.rgba[(5 * W + 5) * 4] === 220 && back.rgba[3] === 255);
      const frames = [0, 1, 2].map((k) => ({ rgb: rgb.map((v, i) => (i % 3 === 1 ? 30 + k * 40 : v)), delay: 7 }));
      const gif = encodeGif(W, H, frames);
      const g = decodeGif(gif);
      check('GIF 編了再解回來：格數、延遲、顏色都對', g.frames.length === 3 && g.frames[2].delay === 7
        && g.frames[2].rgba[(5 * W + 5) * 4 + 1] === 110 && g.frames[0].rgba[(5 * W + 5) * 4] === 220);

      const hash = 'e'.repeat(64);
      await env.CORPUS.put('blob:' + hash, png, { metadata: { t: 'png', s: png.length } });
      r = await worker.fetch(api('/cape-preview/' + hash), env);
      const shot = await decodePng(new Uint8Array(await r.arrayBuffer()));
      const box = layout(W, H, 512);
      const at = (x, y) => [...shot.rgba.subarray((y * shot.width + x) * 4, (y * shot.width + x) * 4 + 3)];
      check('★ 預覽圖：只露出背面（中間是紅的），兩邊是卡片的深綠底', r.status === 200
        && r.headers.get('content-type') === 'image/png' && shot.width === box.width && shot.height === box.height
        && at(shot.width >> 1, shot.height >> 1)[0] === 220 && at(2, shot.height >> 1)[1] < 50 && at(2, shot.height >> 1)[2] < 50);
      check('小張的像素圖用整數倍放大', Number.isInteger(box.h / 16) && box.h >= 320);
      check('★ 算過一次就存起來（prev:<雜湊>）', env.CORPUS.data.has('prev:' + hash)
        && (await worker.fetch(api('/cape-preview/' + hash), env)).status === 200);
      check('沒有這張圖 404、雜湊不對 404', (await worker.fetch(api('/cape-preview/' + 'f'.repeat(64)), env)).status === 404
        && (await worker.fetch(api('/cape-preview/xyz'), env)).status === 404);

      const ghash = 'a1'.repeat(32);
      await env.CORPUS.put('blob:' + ghash, gif, { metadata: { t: 'gif', s: gif.length } });
      r = await worker.fetch(api('/cape-preview/' + ghash), env);
      const moving = decodeGif(new Uint8Array(await r.arrayBuffer()));
      check('★ GIF 披風的預覽也會動（每一格都切）', r.headers.get('content-type') === 'image/gif' && moving.frames.length === 3);

      await env.CORPUS.put('cape:' + capeId, '', { metadata: { h: hash, t: 'png', n: 'Dottie_9', v: 1 } });
      env.CORPUS.data.delete('prev:' + hash);
      const waits = [];
      discordCalls.length = 0;
      r = await worker.fetch(await signed(command('cape', [{ name: 'player', value: 'Dottie_9' }])), env, { waitUntil: (job) => waits.push(job) });
      check('★ /cape 先回「處理中」', (await r.json()).type === 5);
      await Promise.all(waits);
      const sent = JSON.parse(discordCalls.find((c) => c.method === 'PATCH').body);
      check('★ 補上結果之前，預覽圖已經算好存起來（Discord 來抓時不必現算）', env.CORPUS.data.has('prev:' + hash)
        && sent.embeds[0].image.url.endsWith('/api/cape-preview/' + hash));
      // ---- 人物預覽：skin 穿著披風，正面＋背面 ----
      // skin：頭正面塗紅、頭背面塗藍、身體正面塗綠（其餘透明）
      const sk = new Uint8Array(64 * 64 * 3);
      const paintSkin = (x0, y0, w, h, c) => {
        for (let y = y0; y < y0 + h; y++) for (let x = x0; x < x0 + w; x++) sk.set(c, (y * 64 + x) * 3);
      };
      paintSkin(8, 8, 8, 8, [200, 0, 0]);
      paintSkin(24, 8, 8, 8, [0, 0, 200]);
      paintSkin(20, 20, 8, 12, [0, 200, 0]);
      // 測試用的 skin 沒有透明度，外層（帽子、外套）也要塗成同色，不然整片黑會蓋住
      paintSkin(40, 8, 8, 8, [200, 0, 0]);
      paintSkin(56, 8, 8, 8, [0, 0, 200]);
      paintSkin(20, 36, 8, 12, [0, 200, 0]);
      fakeSkin = await encodePng(64, 64, sk);
      const who = 'c'.repeat(32);
      r = await worker.fetch(api('/cape-preview/' + hash + '?u=' + who), env);
      const fig = await decodePng(new Uint8Array(await r.arrayBuffer()));
      // 從背後斜看（跟網頁的 3D 檢視同一個角度）：看得到頭的背面（藍）與披風外側（紅），
      // 頭的正面與身體正面（綠）在另一邊，看不到。顏色有打光，只比色相。
      let blue = 0, red = 0, green = 0;
      for (let p = 0; p < fig.width * fig.height; p++) {
        const [cr, cg, cb] = fig.rgba.subarray(p * 4, p * 4 + 3);
        if (cb > 120 && cr < 30 && cg < 30) blue++;
        if (cr > 120 && cg < 30 && cb < 30) red++;
        if (cg > 120 && cr < 30 && cb < 30) green++;
      }
      check('★ 人物預覽：從背後斜看，看得到頭的背面與披風的外側', r.status === 200 && fig.height === 480
        && blue > 2000 && red > 5000);
      check('★ 人物預覽：身體正面在另一邊，看不到', green === 0);
      check('人物那份另外存（prev:<雜湊>:<UUID>:<畫法>，三天）', env.CORPUS.data.has('prev:' + hash + ':' + who + ':angled')
        && env.CORPUS.data.get('prev:' + hash + ':' + who + ':angled').ttl === 3 * 86400);
      fakeSkin = null;
      r = await worker.fetch(api('/cape-preview/' + hash + '?u=' + 'd'.repeat(32)), env);
      check('抓不到 skin 就畫灰色人偶，不會失敗', r.status === 200 && r.headers.get('content-type') === 'image/png');
      r = await worker.fetch(api('/cape-preview/' + ghash + '?u=' + who), env);
      check('★ GIF 披風的人物預覽也會動', r.headers.get('content-type') === 'image/gif'
        && decodeGif(new Uint8Array(await r.arrayBuffer())).frames.length === 3);
      await env.CORPUS.put('cape:' + capeId, '', { metadata: { h: hash, t: 'png', n: 'Dottie_9', v: 1, u: who } });
      reply = await (await worker.fetch(await signed(command('cape', [{ name: 'player', value: 'Dottie_9' }])), env)).json();
      check('★ /cape 的圖是那個人的人物預覽（帶 UUID）', reply.data.embeds[0].image.url.endsWith('/api/cape-preview/' + hash + '?u=' + who + '&v=angled'));
      for (const k of [...env.CORPUS.data.keys()]) if (k.startsWith('prev:')) env.CORPUS.data.delete(k);
      for (const k of ['blob:' + hash, 'blob:' + ghash]) env.CORPUS.data.delete(k);
    }
    env.CORPUS.data.delete('cape:' + capeId);
    reply = await (await worker.fetch(await signed(command('cape', [{ name: 'player', value: 'no such!' }])), env)).json();
    check('不像名字的只回給本人看', reply.data.flags === 64);

    const waits = [];
    const ctx = { waitUntil: (job) => waits.push(job) };
    discordCalls.length = 0;
    r = await worker.fetch(await signed(command('translate', [{ name: 'text', value: 'hello' }])), env, ctx);
    check('★ /translate 先回「處理中」', (await r.json()).type === 5);
    await Promise.all(waits);
    const patch = discordCalls.find((c) => c.method === 'PATCH');
    const sent = patch ? JSON.parse(patch.body) : null;
    check('★ 查完補上結果：原文、譯文都比對，沒翻的標出來', patch && patch.path === '/api/v10/webhooks/app1/tok1/messages/@original'
      && sent.embeds[0].fields.length === 2 && sent.embeds[0].fields.some((f) => f.value.startsWith('你好，{u}！'))
      && sent.embeds[0].fields.some((f) => f.value.startsWith('（還沒翻）')));
    waits.length = 0;
    discordCalls.length = 0;
    await worker.fetch(await signed(command('translate', [{ name: 'text', value: '再見' }, { name: 'language', value: 'zh_tw' }])), env, ctx);
    await Promise.all(waits);
    check('用譯文也查得到', JSON.parse(discordCalls.find((c) => c.method === 'PATCH').body).embeds[0].fields[0].name === 'Bye');

    // ---- /回饋（使用者 2026-10-17）：跟網站的回饋頁進同一個地方 ----
    {
      const GUS = '333333333333333333';
      const gus = { id: GUS, username: 'gus', global_name: 'Gus 阿古' };
      const fbCommand = (options, locale = 'zh-TW', who = { member: { user: gus } }) => ({
        type: 2, application_id: 'app1', token: 'tok1', locale, guild_id: '1', ...who, data: { name: 'feedback', options },
      });
      const opt = (cat, msg, extra = []) => [{ name: 'category', value: cat }, { name: 'message', value: msg }, ...extra];
      const mine = () => [...env.CORPUS.data.entries()].filter(([k]) => k.startsWith('fb:'))
        .map(([, v]) => JSON.parse(v.value)).filter((x) => x.uid === GUS);
      const def = DISCORD_COMMANDS.find((c) => c.name === 'feedback');
      check('★ /回饋 的定義：繁中叫「回饋」、簡中「反馈」；分類五選一、內容必填、位置與語言可以不填', def
        && def.name_localizations['zh-TW'] === '回饋' && def.name_localizations['zh-CN'] === '反馈'
        && def.options.map((o) => o.name + (o.required ? '!' : '?')).join() === 'category!,message!,where?,language?'
        && def.options[0].choices.map((c) => c.value).join() === 'translation,content,website,feature,other'
        && def.options[0].choices[0].name_localizations['zh-TW'] === '譯文建議');

      r = await worker.fetch(await signed(fbCommand(opt('feature', '想要 /cape 也能看別人的徽章'))), env);
      const fbReply = await r.json();
      check('★ /回饋（不是譯文建議）：直接回，只給本人看', r.status === 200 && fbReply.type === 4
        && fbReply.data.flags === 64 && fbReply.data.content.includes('收到了'));
      check('★ /回饋 存進同一個地方：Discord 代號、顯示名稱、來自機器人', mine().length === 1 && mine()[0].name === 'Gus 阿古'
        && mine()[0].via === 'bot' && mine()[0].cat === 'feature' && mine()[0].st === 'open');
      check('★ /回饋 的簽名不對：401，什麼都不存',
        (await worker.fetch(await signed(fbCommand(opt('other', 'forged message')), true), env)).status === 401 && mine().length === 1);

      const fbWaits = [];
      discordCalls.length = 0;
      githubCalls.length = 0;
      r = await worker.fetch(await signed(fbCommand(opt('translation', 'Gambit 的譯名前後不一致', [{ name: 'where', value: 'Lootrun 結算畫面' }]), 'ja')),
        env, { waitUntil: (job) => fbWaits.push(job) });
      const deferred = await r.json();
      check('★ /回饋 的譯文建議要開 Issue：先回「處理中」，而且只給本人看', deferred.type === 5 && deferred.data.flags === 64);
      await Promise.all(fbWaits);
      const patched = JSON.parse((discordCalls.find((c) => c.method === 'PATCH') || { body: '{}' }).body);
      const issueCall = githubCalls.find((c) => c.path === '/issues');
      check('★ 補上的回覆附 Issue 網址；Issue 只放顯示名稱、不放代號；語言跟著 Discord 介面（日文）',
        String(patched.content).includes('/issues/77') && issueCall && issueCall.body.body.includes('Gus 阿古')
        && !issueCall.body.body.includes(GUS) && !issueCall.body.title.includes(GUS) && issueCall.body.body.includes('日本語')
        && issueCall.body.body.includes('Lootrun 結算畫面') && issueCall.body.body.includes('Discord 機器人'));
      check('Issue 網址記在那一筆', mine().some((x) => x.cat === 'translation' && x.issue && x.issue.url.endsWith('/issues/77')));

      r = await worker.fetch(await signed(fbCommand(opt('other', 'ok'))), env);
      check('/回饋 太短：只回給本人看', (await r.json()).data.content.includes('太短'));
      for (let i = mine().length; i < FEEDBACK_PER_HOUR; i++) {
        await worker.fetch(await signed(fbCommand(opt('other', '再一則 ' + i), 'zh-TW', { user: gus })), env);
      }
      check('私訊裡（user 不在 member 底下）也認得是誰', mine().length === FEEDBACK_PER_HOUR);
      r = await worker.fetch(await signed(fbCommand(opt('other', '第六則了'))), env);
      const limited = await r.json();
      check('★ /回饋 的限流：滿了只回給本人看，說幾點可以再送', limited.data.flags === 64 && limited.data.content.includes('<t:')
        && limited.data.content.includes(String(FEEDBACK_PER_HOUR)) && mine().length === FEEDBACK_PER_HOUR);
      for (const k of [...env.CORPUS.data.keys()]) if (k.startsWith('fb:') || k.startsWith('fbr:')) env.CORPUS.data.delete(k);
    }

    check('★ 註冊指令只有開發者', (await trPost('/admin/discord/register', {}, bobNow)).status === 403);
    check('沒設 Bot Token 就說還沒設定', (await trPost('/admin/discord/register', {}, aliceWeb)).status === 503);
    env.DISCORD_BOT_TOKEN = 'bot-secret';
    const hadClient = env.DISCORD_CLIENT_ID;
    env.DISCORD_CLIENT_ID = env.DISCORD_CLIENT_ID || 'client1';
    discordCalls.length = 0;
    r = await trPost('/admin/discord/register', {}, aliceWeb);
    const put = discordCalls.find((c) => c.method === 'PUT');
    check('★ 註冊：PUT 全部指令、帶 Bot Token', r.status === 200 && put && put.auth === 'Bot bot-secret'
      && JSON.parse(put.body).map((c) => c.name).join() === 'status,cape,translate,feedback');
    discordAppKey = env.DISCORD_PUBLIC_KEY;
    const info = await (await trGet('/admin/discord', aliceWeb)).json();
    check('管理頁拿得到邀請連結與設定狀態', info.token && info.publicKey && info.invite.includes('scope=applications.commands+bot'));
    check('★ 管理頁直接問 Discord：應用程式、金鑰、指令、伺服器', info.check && info.check.app === 'WynnChaYuan'
      && info.check.sameKey && info.check.commands.join() === 'status,cape' && info.check.guilds.join() === 'YuChaYuan'
      && info.check.sameClient === (env.DISCORD_CLIENT_ID === 'client1'));
    discordAppKey = 'f'.repeat(64);
    check('金鑰對不上就標出來', (await (await trGet('/admin/discord', aliceWeb)).json()).check.sameKey === false);
    delete env.DISCORD_BOT_TOKEN;
    if (hadClient === undefined) delete env.DISCORD_CLIENT_ID;
    else env.DISCORD_CLIENT_ID = hadClient;
  }

  for (const k of [...env.CORPUS.data.keys()]) if (k.startsWith('tp:') || k.startsWith('tq:')) env.CORPUS.data.delete(k);
  githubCalls.length = 0;
  githubFile = CREDITS;
  env.GITHUB_TOKEN = hadToken;
}

// ---- ★ 回饋（使用者 2026-10-17：/feedback，要用 Discord 登入；譯文建議自動開 GitHub Issue） ----
{
  // /feedback 同時是網頁與 API 的名字：這裡一律打 /api 底下的
  const fbPost = (path, bodyValue, token) => worker.fetch(new Request('https://x.test/api' + path, {
    method: 'POST', body: typeof bodyValue === 'string' ? bodyValue : JSON.stringify(bodyValue),
    headers: token ? { authorization: 'Bearer ' + token } : {},
  }), env);
  const fbGet = (path, token) => worker.fetch(new Request('https://x.test/api' + path, {
    headers: token ? { authorization: 'Bearer ' + token } : {},
  }), env);
  const hadToken = env.GITHUB_TOKEN;
  env.GITHUB_TOKEN = 'ghtok';
  const FAY = '222222222222222222';
  await env.CORPUS.put('d:' + FAY, JSON.stringify({ n: 'Fay 小花' }));
  const fay = await issueDiscordToken(env, FAY, NOW);
  const frank = await issueWebToken(env, 'f'.repeat(32), 'Frank', NOW);      // 從遊戲開的網頁，沒綁 Discord
  const good = { cat: 'website', msg: '披風一覽在手機上排得太擠了', where: '/capes' };
  const kv = (id) => env.CORPUS.data.get('fb:' + id);
  const value = (id) => JSON.parse(kv(id).value);

  r = await fbPost('/feedback', good);
  check('★ 回饋要登入：沒登入 401', r.status === 401 && (await r.json()).why === 'login first');
  r = await fbPost('/feedback', good, frank);
  check('★ 從遊戲開的網頁、沒綁 Discord：401，叫他先綁', r.status === 401 && (await r.json()).why === 'discord first');

  const tidy = feedbackCheck({ cat: 'other', msg: ' hi\r\nthere\u0007 ', where: ' a \n  b ', lang: 'xx_yy' });
  check('★ feedbackCheck：分類、長短、位置的長度', feedbackCheck({ cat: 'nope', msg: 'hello' }).why === 'bad category'
    && feedbackCheck({ cat: 'other', msg: '  a  ' }).why === 'too short'
    && feedbackCheck({ cat: 'other', msg: 'x'.repeat(2001) }).why === 'too long'
    && feedbackCheck({ cat: 'other', msg: 'x'.repeat(2000) }).entry.msg.length === 2000
    && feedbackCheck({ cat: 'other', msg: 'hello', where: 'w'.repeat(201) }).why === 'where too long'
    && feedbackCheck({ cat: 'other', msg: 42 }).why === 'too short' && feedbackCheck(null).why === 'bad request');
  check('feedbackCheck：換行統一、控制字元拿掉、位置收成一行、不認得的語言當成沒填', tidy.entry.msg === 'hi\nthere'
    && tidy.entry.where === 'a b' && tidy.entry.lang === null
    && feedbackCheck({ cat: 'translation', msg: 'abc', lang: 'ja_jp' }).entry.lang === 'ja_jp');
  r = await fbPost('/feedback', { ...good, cat: 'bogus' }, fay);
  check('★ 網頁送來的也驗：分類不對 400', r.status === 400 && (await r.json()).why === 'bad category');
  check('太短、太長、不是 JSON 都 400', (await fbPost('/feedback', { ...good, msg: 'a' }, fay)).status === 400
    && (await fbPost('/feedback', { ...good, msg: 'x'.repeat(2001) }, fay)).status === 400
    && (await fbPost('/feedback', 'not json', fay)).status === 400);

  githubCalls.length = 0;
  r = await fbPost('/feedback', good, fay);
  let made = await r.json();
  const websiteId = made.id;
  const stored = value(websiteId);
  check('★ 存下來：Discord 代號、顯示名稱、分類、內容、位置、來自網站、待處理', r.status === 200 && made.ok
    && stored.uid === FAY && stored.name === 'Fay 小花' && stored.cat === 'website' && stored.msg === good.msg
    && stored.where === '/capes' && stored.via === 'web' && stored.st === 'open' && kv(websiteId).ttl === undefined);
  check('列表要看的放在 metadata', kv(websiteId).metadata.c === 'website' && kv(websiteId).metadata.st === 'open'
    && kv(websiteId).metadata.n === 'Fay 小花' && kv(websiteId).metadata.s === good.msg);
  check('★ 不是譯文建議：不碰 GitHub', made.issue === null && !githubCalls.some((c) => c.path === '/issues'));

  githubCalls.length = 0;
  r = await fbPost('/feedback', { cat: 'translation', msg: '「Weaken Enemy」應該翻成削弱敵傷', where: 'Lootrun 加持', lang: 'zh_tw' }, fay);
  made = await r.json();
  const trId = made.id;
  const issueCall = githubCalls.find((c) => c.path === '/issues');
  check('★ 譯文建議：開 GitHub Issue（用同一把 token、加 translation 標籤），網址記在那一筆', r.status === 200
    && issueCall && issueCall.method === 'POST' && issueCall.auth === 'Bearer ghtok' && issueCall.body.labels.join() === 'translation'
    && issueCall.body.title.startsWith('譯文建議：') && issueCall.body.title.includes('Weaken Enemy')
    && made.issue === 'https://github.com/LyuChaCha/WynnChaYuan/issues/77'
    && value(trId).issue.url === made.issue && value(trId).issue.n === 77 && kv(trId).metadata.i === made.issue);
  check('★ Issue 內文：內容、位置、語言、顯示名稱都在；Discord 代號不在', issueCall.body.body.includes('削弱敵傷')
    && issueCall.body.body.includes('Lootrun 加持') && issueCall.body.body.includes('繁體中文')
    && issueCall.body.body.includes('Fay 小花') && !issueCall.body.body.includes(FAY) && !issueCall.body.title.includes(FAY));
  const tricky = feedbackIssue({ id: 'x', cat: 'translation', msg: 'a ``` b @someone', where: '', name: 'n`a\nme', via: 'web', lang: null });
  check('feedbackIssue：內容裡的反引號關不掉程式碼區塊，名字裡的反引號拿掉', tricky.body.includes('````\na ``` b @someone\n````')
    && tricky.body.includes('`name`') && tricky.body.includes('沒有指定') && !tricky.body.includes('**位置**'));

  for (let i = 2; i < FEEDBACK_PER_HOUR; i++) await fbPost('/feedback', { cat: 'other', msg: '第 ' + i + ' 則回饋' }, fay);
  r = await fbPost('/feedback', good, fay);
  const slow = await r.json();
  check('★ 限流：一小時 ' + FEEDBACK_PER_HOUR + ' 則，再送就 429，告訴他哪時候可以再送', r.status === 429 && slow.why === 'slow down'
    && slow.until > Date.now() && slow.until <= Date.now() + 3600000);
  check('驗證沒過的不算一則；限流紀錄一小時後自己消失', JSON.parse(env.CORPUS.data.get('fbr:' + FAY).value).length === FEEDBACK_PER_HOUR
    && env.CORPUS.data.get('fbr:' + FAY).ttl === 3600);
  env.CORPUS.data.get('fbr:' + FAY).value = JSON.stringify(JSON.parse(env.CORPUS.data.get('fbr:' + FAY).value).map((t) => t - 3600001));
  check('★ 一小時前送的不算：又可以送了', (await fbPost('/feedback', { cat: 'content', msg: '希望翻譯 Raid 的結算畫面' }, fay)).status === 200);

  githubIssueStatus = 500;
  githubCalls.length = 0;
  r = await fbPost('/feedback', { cat: 'translation', msg: 'GitHub 掛了也要留著', lang: 'ja_jp' }, fay);
  made = await r.json();
  const failedId = made.id;
  check('★ GitHub 失敗：回饋照樣存著，記下原因給管理頁看', r.status === 200 && made.issueFailed === true
    && githubCalls.some((c) => c.path === '/issues') && value(failedId).issueError === 'github 500' && !value(failedId).issue
    && kv(failedId).metadata.e === 'github 500' && value(failedId).msg === 'GitHub 掛了也要留著');
  delete env.GITHUB_TOKEN;
  env.CORPUS.data.delete('fbr:' + FAY);
  r = await fbPost('/feedback', { cat: 'translation', msg: '沒設 token 也要留著' }, fay);
  made = await r.json();
  check('沒設 GITHUB_TOKEN：一樣存著，原因是還沒設定', r.status === 200 && value(made.id).issueError === 'github not configured');
  env.CORPUS.data.delete('fb:' + made.id);
  env.GITHUB_TOKEN = 'ghtok';
  githubIssueStatus = 201;
  // 讓「新的在前」看得出來：失敗那一則挪到最新
  kv(failedId).metadata.t = Date.now() + 5000;

  check('★ 管理：一般人、沒登入都看不到回饋列表', (await fbGet('/admin/feedback', fay)).status === 403
    && (await fbGet('/admin/feedback')).status === 403 && (await fbGet('/admin/feedback', bobNow)).status === 403);
  check('★ 管理：一般人不能標已處理、不能刪、不能重開 Issue',
    (await fbPost('/admin/feedback/status', { id: failedId, st: 'done' }, fay)).status === 403
    && (await fbPost('/admin/feedback/delete', { id: websiteId }, bobNow)).status === 403
    && (await fbPost('/admin/feedback/issue', { id: failedId }, fay)).status === 403 && kv(websiteId) && value(failedId).st === 'open');
  r = await fbGet('/admin/feedback', aliceWeb);
  let list = await r.json();
  check('★ 開發者看得到全部，新的在前', r.status === 200 && list.total === FEEDBACK_PER_HOUR + 2
    && list.items[0].id === failedId && list.items.every((it, i, all) => i === 0 || all[i - 1].t >= it.t || i === 1)
    && list.counts.open === FEEDBACK_PER_HOUR + 2 && list.counts.done === 0);
  check('列表裡有 Issue 網址與失敗原因', list.items.some((it) => it.id === trId && it.issue.url.endsWith('/issues/77'))
    && list.items.some((it) => it.id === failedId && it.issueError === 'github 500'));
  list = await (await fbGet('/admin/feedback?cat=translation', aliceWeb)).json();
  check('★ 照分類篩', list.items.length === 2 && list.items.every((it) => it.cat === 'translation'));

  r = await fbPost('/admin/feedback/status', { id: websiteId, st: 'done' }, aliceWeb);
  const doneItem = (await r.json()).item;
  check('★ 標成已處理：記下誰、什麼時候；KV ' + FEEDBACK_KEEP_DAYS + ' 天後自己刪', r.status === 200 && doneItem.st === 'done'
    && doneItem.done.by === ALICE.name && kv(websiteId).ttl === FEEDBACK_KEEP_DAYS * 86400 && kv(websiteId).metadata.st === 'done');
  list = await (await fbGet('/admin/feedback?st=done', aliceWeb)).json();
  check('★ 照狀態篩', list.items.length === 1 && list.items[0].id === websiteId && list.counts.done === 1
    && (await (await fbGet('/admin/feedback?st=open', aliceWeb)).json()).items.every((it) => it.st === 'open'));
  r = await fbPost('/admin/feedback/status', { id: websiteId, st: 'open' }, aliceWeb);
  check('★ 重新開啟：拿掉處理紀錄，不再過期', r.status === 200 && !value(websiteId).done && value(websiteId).st === 'open'
    && kv(websiteId).ttl === undefined);
  check('狀態不對、編號不對 400，找不到 404', (await fbPost('/admin/feedback/status', { id: websiteId, st: 'maybe' }, aliceWeb)).status === 400
    && (await fbPost('/admin/feedback/status', { id: 'zz', st: 'done' }, aliceWeb)).status === 400
    && (await fbPost('/admin/feedback/status', { id: '0'.repeat(16), st: 'done' }, aliceWeb)).status === 404);

  githubCalls.length = 0;
  r = await fbPost('/admin/feedback/issue', { id: failedId }, aliceWeb);
  const retried = await r.json();
  check('★ Issue 沒開成的：管理頁再開一次，成功就清掉失敗原因', r.status === 200 && retried.item.issue.url.endsWith('/issues/77')
    && !value(failedId).issueError && githubCalls.filter((c) => c.path === '/issues').length === 1);
  check('不是譯文建議的不開 Issue', (await fbPost('/admin/feedback/issue', { id: websiteId }, aliceWeb)).status === 409);

  await env.CORPUS.put('staff:d:' + FAY, JSON.stringify({ n: '測試', t: NOW, by: 'test' }));
  check('★ 管理員（照 Discord 加的）也看得到、也能處理', (await fbGet('/admin/feedback', fay)).status === 200
    && (await fbPost('/admin/feedback/status', { id: trId, st: 'done' }, fay)).status === 200
    && value(trId).done.by === 'Discord: Fay 小花');
  env.CORPUS.data.delete('staff:d:' + FAY);

  r = await fbPost('/admin/feedback/delete', { id: websiteId }, aliceWeb);
  check('★ 刪掉垃圾訊息', r.status === 200 && !kv(websiteId));
  check('刪過的再刪 404', (await fbPost('/admin/feedback/delete', { id: websiteId }, aliceWeb)).status === 404);

  check('★ 回饋頁：分類、內容、位置、語言與送出；頁首與頁尾都連得到；管理頁有回饋列表',
    ['id="fb-form"', 'id="fb-cat"', 'value="translation"', 'value="other"', 'id="fb-text"', 'id="fb-where"', 'id="fb-lang"', "'/feedback'"]
      .every((w) => pages['/feedback'].includes(w))
    && pages['/'].includes('<a href="/feedback">') && english['/terms'].includes('<a href="/feedback">')
    && ['id="fbf-list"', 'id="fbf-cat"', 'id="fbf-st"', "'/admin/feedback/status'", "'/admin/feedback/delete'", "'/admin/feedback/issue'"]
      .every((w) => pages['/admin'].includes(w)));
  check('★ 隱私政策八種語言都講了回饋（會變成公開的 Issue、保留多久）與怎麼刪資料', LANGS.every((lang) => {
    const body = TEXT[lang]['privacy.body'];
    return body.split('<h2>').length === 12 && body.includes('GitHub') && body.includes('180') && /issue/i.test(body)
      && body.includes('/api/me/delete') && body.includes('href="/account"') && body.includes('version.json');
  }));

  for (const k of [...env.CORPUS.data.keys()]) if (k.startsWith('fb:') || k.startsWith('fbr:')) env.CORPUS.data.delete(k);
  env.CORPUS.data.delete('d:' + FAY);
  env.GITHUB_TOKEN = hadToken;
}

{
  const KV_LIMIT = 1000;
  const limitedKv = () => {
    const base = fakeKv();
    let used = 0;
    let peak = 0;
    const spend = () => {
      used += 1;
      peak = Math.max(peak, used);
      if (used > KV_LIMIT) throw new Error('Too many API requests by single Worker invocation.');
    };
    const store = { data: base.data, ops: base.ops, reset: () => { used = 0; }, peak: () => peak };
    for (const name of ['get', 'getWithMetadata', 'put', 'delete', 'list']) {
      store[name] = async (...args) => { spend(); return base[name](...args); };
    }
    return store;
  };
  const big = { CORPUS: limitedKv() };
  const call = async (path, value, token) => {
    big.CORPUS.reset();
    try {
      return await worker.fetch(post(path, value, token), big);
    } catch (e) {
      return new Response(JSON.stringify({ ok: false, threw: e.message }), { status: 500 });
    }
  };
  const IVY = { uuid: 'e7'.repeat(16), name: 'Ivy' };
  const IVY_DC = '499999999999999999';
  const ivyGame = await issueToken(big, IVY.uuid, IVY.name, NOW);
  const OTHER = 'u:' + 'e8'.repeat(16);
  const PROPS = 1500;
  const propKey = (n) => 'tp:zh_tw:' + n.toString(16).padStart(16, '0');
  for (let n = 0; n < PROPS; n++) {
    big.CORPUS.data.set(propKey(n), { value: JSON.stringify({ lang: 'zh_tw', file: 'npc.json', key: null, src: 'L' + n, base: '',
      status: 'open', t: NOW, items: [{ id: 'i' + n, text: '譯' + n, by: n % 3 ? OTHER : 'u:' + IVY.uuid, name: n % 3 ? 'Other' : 'Ivy', t: NOW,
        votes: n % 2 ? ['u:' + IVY.uuid, OTHER] : [OTHER] }] }), metadata: { st: 'open' } });
  }
  for (let n = 0; n < 300; n++) {
    const id = n.toString(16).padStart(16, '0');
    big.CORPUS.data.set('fb:' + id, { value: JSON.stringify({ id, uid: n % 2 ? IVY_DC : '400000000000000000', name: 'x', msg: 'm', cat: 'other', st: 'open' }),
      metadata: { c: 'other' } });
  }
  big.CORPUS.data.set('d:' + IVY_DC, { value: JSON.stringify({ n: 'Ivy DC', u: IVY.uuid, m: IVY.name, t: NOW }), metadata: { n: 'Ivy DC' } });
  big.CORPUS.data.set('l:' + IVY.uuid, { value: IVY_DC, metadata: null });
  big.CORPUS.data.set('u:' + IVY.uuid, { value: '', metadata: { i: await publicId(IVY.uuid), c: 1, d: 1 } });
  const ivyLeft = () => [...big.CORPUS.data.entries()].filter(([k, v]) => k.startsWith('tp:') && v.value.includes(IVY.uuid)
    && !JSON.parse(v.value).ghosts).length;
  const statuses = [];
  let finished = null;
  for (let round = 0; round < 40 && !finished; round++) {
    const r = await call('/me/delete', {}, ivyGame);
    const answer = await r.json();
    statuses.push(r.status);
    if (r.status === 200) finished = answer;
    else if (!(r.status === 202 && answer.pending === true && answer.ok === false)) break;
  }
  check('★ 刪資料：資料多到一次刪不完時回 202 pending，分好幾次接著刪', statuses[0] === 202 && statuses.length > 1
    && statuses.slice(0, -1).every((s) => s === 202));
  check('★ 刪資料：每一次請求都在 Cloudflare 的 1,000 次 KV 操作之內', big.CORPUS.peak() <= KV_LIMIT);
  check('★ 刪資料：全部刪完才回 {"ok":true}', Boolean(finished) && finished.ok === true && ivyLeft() === 0
    && ![...big.CORPUS.data.keys()].some((k) => k.startsWith('fb:') && big.CORPUS.data.get(k).value.includes(IVY_DC))
    && [...big.CORPUS.data.keys()].filter((k) => k.startsWith('fb:')).length === 150
    && !big.CORPUS.data.has('d:' + IVY_DC) && !big.CORPUS.data.has('l:' + IVY.uuid) && !big.CORPUS.data.has('u:' + IVY.uuid)
    && ![...big.CORPUS.data.keys()].some((k) => k.startsWith('erase:')));
  check('★ 刪資料：別人的提案只拿掉她的票，內容不動', (() => {
    const one = JSON.parse(big.CORPUS.data.get(propKey(1)).value);
    const untouched = JSON.parse(big.CORPUS.data.get(propKey(2)).value);
    return one.items[0].by === OTHER && one.items[0].votes.length === 2 && !one.items[0].votes.includes('u:' + IVY.uuid)
      && !untouched.ghosts && untouched.items[0].votes.join() === OTHER;
  })());

  const fresh = { CORPUS: limitedKv() };
  for (let n = 0; n < 40; n++) {
    fresh.CORPUS.data.set(propKey(n), { value: JSON.stringify({ lang: 'zh_tw', items: [{ by: n % 2 ? OTHER : 'u:' + IVY.uuid, votes: ['d:' + IVY_DC] }] }),
      metadata: null });
  }
  fresh.CORPUS.data.set('fb:' + 'a'.repeat(16), { value: JSON.stringify({ uid: IVY_DC }), metadata: null });
  fresh.CORPUS.data.set('prev:' + 'c'.repeat(64) + ':' + IVY.uuid + ':angled', { value: '', metadata: null });
  fresh.CORPUS.data.set('prev:' + 'c'.repeat(64), { value: '', metadata: null });
  let runs = 0;
  let indexed;
  do {
    fresh.CORPUS.reset();
    indexed = await indexOwners(fresh, NOW);
    runs++;
  } while (!indexed.done && runs < 20);
  check('★ 每天的排程替舊資料補上「誰參與了哪一筆」的索引，補完才標成可以只看索引', indexed.done
    && fresh.CORPUS.data.has(OWNER_INDEX_READY) && fresh.CORPUS.data.has(ownerEntry('u:' + IVY.uuid, propKey(0)))
    && !fresh.CORPUS.data.has(ownerEntry('u:' + IVY.uuid, propKey(1))) && fresh.CORPUS.data.has(ownerEntry('d:' + IVY_DC, propKey(1)))
    && fresh.CORPUS.data.has(ownerEntry('d:' + IVY_DC, 'fb:' + 'a'.repeat(16)))
    && fresh.CORPUS.data.has(ownerEntry('u:' + IVY.uuid, 'prev:' + 'c'.repeat(64) + ':' + IVY.uuid + ':angled'))
    && [...fresh.CORPUS.data.keys()].filter((k) => k.startsWith('own:')).length === 40 * 2 + 1 + 1);
  check('participantsOf：只收帳號格式的 by 與票', [...participantsOf({ by: 'u:' + 'a'.repeat(32), items: [{ by: 'x:1234', votes: ['d:123456', 'n:bob'] }],
    options: [null] })].join() === 'u:' + 'a'.repeat(32) + ',d:123456');

  const indexedEnv = { CORPUS: limitedKv() };
  indexedEnv.CORPUS.data.set(OWNER_INDEX_READY, { value: String(NOW), metadata: null });
  for (let n = 0; n < 3000; n++) indexedEnv.CORPUS.data.set(propKey(n), { value: '{"items":[]}', metadata: null });
  const ivyDc = await issueDiscordToken(indexedEnv, IVY_DC, NOW);
  indexedEnv.CORPUS.data.set('d:' + IVY_DC, { value: JSON.stringify({ n: 'Ivy DC' }), metadata: { n: 'Ivy DC' } });
  indexedEnv.CORPUS.data.set('tl:d:' + IVY_DC, { value: JSON.stringify({ langs: ['zh_tw'] }), metadata: null });
  indexedEnv.CORPUS.reset();
  const proposed = await worker.fetch(post('/tr/propose', { lang: 'zh_tw', file: 'npc.json', src: 'Hello', text: '哈囉' }, ivyDc), indexedEnv);
  const propId = (await proposed.json()).id;
  check('★ 提案時記下「她參與了這一筆」', proposed.status === 200
    && indexedEnv.CORPUS.data.has(ownerEntry('d:' + IVY_DC, 'tp:zh_tw:' + propId)));
  indexedEnv.CORPUS.data.delete('tl:d:' + IVY_DC);
  const listsBefore = indexedEnv.CORPUS.ops.list;
  indexedEnv.CORPUS.reset();
  const gone = await worker.fetch(post('/me/delete', {}, ivyDc), indexedEnv);
  const prop = JSON.parse(indexedEnv.CORPUS.data.get('tp:zh_tw:' + propId).value);
  check('★ 索引補完之後只照索引刪，不必掃整個協作翻譯', gone.status === 200 && (await gone.json()).ok === true
    && prop.items[0].name === null && !indexedEnv.CORPUS.data.has('d:' + IVY_DC)
    && !indexedEnv.CORPUS.data.has(ownerEntry('d:' + IVY_DC, 'tp:zh_tw:' + propId))
    && indexedEnv.CORPUS.ops.list - listsBefore <= 3);
}

{
  const broken = { CORPUS: { ...fakeKv(), list: async () => { throw new Error('KV list exploded: secret detail'); },
    get: async () => null, getWithMetadata: async () => ({ value: null, metadata: null }) } };
  const logged = [];
  const realError = console.error;
  console.error = (...args) => logged.push(args.join(' '));
  let answer;
  try {
    answer = await worker.fetch(new Request('https://x.test/api/list'), broken);
  } catch (e) {
    answer = null;
  } finally {
    console.error = realError;
  }
  const text = answer ? await answer.text() : '';
  check('★ 沒接住的錯誤回 JSON 500，不是 Cloudflare 的錯誤頁', answer && answer.status === 500
    && answer.headers.get('content-type').startsWith('application/json') && JSON.parse(text).why === 'server error');
  check('★ 錯誤內容不外洩，但記在 log（含路徑）', !text.includes('exploded') && logged.length === 1
    && logged[0].includes('request failed') && logged[0].includes('/api/list') && logged[0].includes('KV list exploded'));
  const page = await worker.fetch(new Request('https://x.test/', { headers: { 'accept-language': 'en' } }), broken);
  check('網頁本身不靠 KV，照樣出得來', page.status === 200);
  const cronLogs = [];
  console.error = (...args) => cronLogs.push(args.join(' '));
  try {
    await worker.scheduled({}, broken);
  } finally {
    console.error = realError;
  }
  check('★ 每天的排程：一件失敗記下來，另一件照樣跑', cronLogs.some((line) => line.includes('scheduled failed') && line.includes('tidy')));
}

{
  const { listAll } = await import('./kv.js');
  const { timingSafeEqual, base64Bytes } = await import('./http.js');
  const store = fakeKv();
  for (let n = 0; n < 2500; n++) store.data.set('x:' + String(n).padStart(4, '0'), { value: '', metadata: { n } });
  const all = await listAll(store, 'x:');
  const lists = store.ops.list;
  const firstPages = await listAll(store, 'x:', { more: (keys) => keys.length < 1500 });
  check('listAll：分頁讀完整份；more 說不要了就停在那一頁', all.length === 2500 && lists === 3
    && all[2499].metadata.n === 2499 && firstPages.length === 2000);
  check('timingSafeEqual 與 base64Bytes', timingSafeEqual('abc', 'abc') && !timingSafeEqual('abc', 'abd')
    && !timingSafeEqual('abc', 'ab') && !timingSafeEqual(null, 'a') && base64Bytes('AQID').join() === '1,2,3');
}

{
  const { recountStars } = await import('./capes.js');
  const HOT = ['presence:list', 'idle', 'stars'];
  const hot = { CORPUS: fakeKv() };
  const realPut = hot.CORPUS.put.bind(hot.CORPUS);
  let refuse = false;
  hot.CORPUS.put = async (key, value, options) => {
    if (refuse && HOT.includes(key)) throw new Error('KV PUT failed: 429 Too Many Requests');
    return realPut(key, value, options);
  };
  const hash = 'ab'.repeat(32);
  const PAM = { uuid: 'e1'.repeat(16), name: 'Pam' };
  const pamGame = await issueToken(hot, PAM.uuid, PAM.name, NOW);
  const pamWeb = await issueWebToken(hot, PAM.uuid, PAM.name, NOW);
  hot.CORPUS.data.set('presence:list', { value: JSON.stringify({ t: Date.now() - 3600000, users: [], capes: {}, hidden: [], rainbow: [], show: {} }), metadata: null });
  hot.CORPUS.data.set('idle', { value: JSON.stringify({ [hash]: { t: 'png', s: 10, at: Date.now() } }), metadata: null });
  hot.CORPUS.data.set('blob:' + hash, { value: new Uint8Array([1]), metadata: { t: 'png', s: 1 } });
  const logged = [];
  const realError = console.error;
  console.error = (...args) => logged.push(args.join(' '));
  refuse = true;
  const hello = await worker.fetch(post('/hello', { cape: true }, pamGame), hot);
  const star = await worker.fetch(post('/cape/star', { hash, on: true }, pamWeb), hot);
  const listed = await worker.fetch(get('/list'), hot);
  refuse = false;
  console.error = realError;
  check('★ 名單、閒置清單、星星那幾個熱門的鍵寫不進去（429）：請求照樣成功，記在 log', hello.status === 200 && star.status === 200
    && (await star.json()).on === true && listed.status === 200 && logged.some((line) => line.includes('kv write failed') && line.includes('presence:list'))
    && logged.some((line) => line.includes('"stars"')));
  check('★ 寫不進去也沒有弄丟真正的資料：報到紀錄與我的最愛都在', hot.CORPUS.data.has('u:' + PAM.uuid)
    && JSON.parse(hot.CORPUS.data.get('fav:' + PAM.uuid).value).h.includes(hash));
  await worker.scheduled({}, hot);
  check('★ 每天的排程照最愛重算星星，修好沒寫進去或被蓋掉的人數', JSON.parse(hot.CORPUS.data.get('stars').value)[hash] === 1
    && !hot.CORPUS.data.has('stars:recount'));
  hot.CORPUS.data.set('stars', { value: JSON.stringify({ [hash]: 9, ['cd'.repeat(32)]: 2 }), metadata: null });
  for (let n = 0; n < 700; n++) {
    hot.CORPUS.data.set('fav:' + n.toString(16).padStart(32, '0'), { value: JSON.stringify({ t: 1, h: [hash, 'cd'.repeat(32)] }), metadata: null });
  }
  let rounds = 0;
  let outcome;
  do {
    outcome = await recountStars(hot);
    rounds++;
  } while (!outcome.done && rounds < 10);
  check('★ 最愛太多一次讀不完：分幾天接著算，算完才換掉人數；已經不在的圖不算', rounds > 1 && outcome.done
    && JSON.parse(hot.CORPUS.data.get('stars').value)[hash] === 701 && !(('cd'.repeat(32)) in JSON.parse(hot.CORPUS.data.get('stars').value))
    && !hot.CORPUS.data.has('stars:recount'));
}

check('★ 協作翻譯頁：搜尋、提案、投票、術語；管理頁的翻譯團隊管譯者權限', ['id="tr-q"', 'id="tr-file"', 'id="tr-blank"', 'id="tp-props"', 'id="tp-terms"', "'/tr/propose'", "'/tr/vote'", "'/tr/poll/vote'"]
  .every((w) => pages['/translate'].includes(w)) && pages['/admin'].includes('id="langteam-rows"') && pages['/admin'].includes("'/admin/langteam'")
  && pages['/'].includes('href="/translate"'));
check('★ 首頁有照翻譯團隊篩選的那一排；管理頁的譯者名單與翻譯團隊併成一節', pages['/'].includes('id="team-pick"')
  && pages['/'].includes('who-tags') && pages['/'].includes("get('team')") && pages['/admin'].includes('id="lt-save"')
  && !pages['/admin'].includes('id="translators"') && !pages['/admin'].includes('id="mteams"')
  && pages['/admin'].includes('id="c-teams"') && english['/admin'].includes('Translation teams'));
check('★ 八種語言都有貢獻項目、「全部」與翻譯團隊那一節的字', LANGS.every((code) => TEXT[code] && TEXT[code].js.teamAreas
  && TEAM_AREAS.every((area) => TEXT[code].js.teamAreas[area]) && TEXT[code].js.teamAll && TEXT[code]['home.teampick']
  && TEXT[code]['admin.lt'] && TEXT[code]['admin.ltnote'] && TEXT[code].js.lt && TEXT[code].js.lt.confirmDrop.includes('{langs}')
  && TEXT[code].js.lt.teamFail.includes('{s}') && ['developers', 'sponsors', 'contributors'].every((role) => TEXT[code].js.lt.roles[role])
  && ['bad who', 'bad team', 'bad area', 'not listed', 'bad note'].every((why) => TEXT[code].js.lt.why[why])));
check('★ 首頁與管理頁的程式都是合法的 JavaScript', ['/', '/admin'].every((path) => [...pages[path].matchAll(/<script>([\s\S]*?)<\/script>/g)]
  .every((m) => { try { new Function(m[1]); return true; } catch (e) { console.log(path, e.message); return false; } })));
for (const path of ['/privacy', '/terms', '/', '/favicon.png']) {
  r = await worker.fetch(new Request('https://x.test' + path, { method: 'HEAD' }), env);
  check('★ HEAD ' + path + ' 跟 GET 一樣是 200、不帶內容（Discord 檢查網址用 HEAD）', r.status === 200 && (await r.text()) === '');
}
check('HEAD 不認得的頁面一樣 404', (await worker.fetch(new Request('https://x.test/nope', { method: 'HEAD' }), env)).status === 404);
r = await worker.fetch(get('/status'), env);
check('/api/status 回人數與上傳模式', JSON.stringify(await r.json()).includes('"uploads":"tiered"'));

console.log(failures === 0 ? '登記站：全部通過' : '登記站：' + failures + ' 項失敗');
process.exit(failures === 0 ? 0 : 1);
