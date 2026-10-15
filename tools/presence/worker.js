/**
 * WynnChaYuan 的網站與登記站（wynnchayuan.com）。
 *
 * 網頁（內容在 site.js）：
 *   /          首頁
 *   /capes     披風一覽
 *   /account   帳號：登入狀態、自己的披風、上傳
 *   /admin     管理（開發者）：封禁名單、新增成員
 *
 * API（模組與網頁都用）：
 *   POST   /api/login          {name, serverId}   向 Mojang 確認身分，發一張通行證（模組用）
 *   POST   /api/web-link                          拿一條替瀏覽器登入的網址（要通行證）
 *   GET    /api/me                                我是誰、有沒有披風、是不是開發者（要通行證）
 *   POST   /api/hello          {cape}             報到（要通行證）
 *   GET    /api/list                              名單：有裝的人、有上傳披風的人、關掉披風的人
 *   PUT    /api/cape           圖檔本體            上傳披風（要通行證）
 *   DELETE /api/cape                              拿掉自己上傳的披風（要通行證）
 *   GET    /api/cape/<代碼>                        下載某個人的披風
 *   GET    /api/capes                             所有上傳的披風（相同的圖併成一筆）
 *   GET    /api/status                            有幾個人、幾件披風
 *   GET    /api/admin/capes                       同上，多了每一件是誰傳的（開發者）
 *   POST   /api/admin/remove   {id|hash, ban}     拿掉披風、要不要順便禁止再傳（開發者）
 *   GET    /api/admin/perks                       特別名單（開發者）
 *   POST   /api/admin/perk     {uuid, note?}      加進特別名單（開發者）
 *   POST   /api/admin/unperk   {uuid}             從特別名單移除（開發者）
 *   GET    /api/admin/bans                        封禁名單（開發者）
 *   POST   /api/admin/unban    {uuid}             解除封禁（開發者）
 *   POST   /api/admin/credits  {section, name, mc, color}   替名單加一個人：開一個 PR（開發者）
 *   GET    /api/team                  名單上的開發者、贊助者、貢獻者，加上貢獻公會（首頁畫團隊用，公開）
 *   POST   /api/cape/wear      {hash} 穿上披風一覽裡的某一件（原本那一件收著）
 *   POST   /api/cape/restore          復原：換回套用之前自己的那一件；原本沒有就脫下
 *   POST   /api/cape/star      {hash, on}    把一覽上的某一件加進（或移出）我的最愛
 *   POST   /api/cape/public    {on}          我的披風要不要出現在披風一覽
 *   GET    /api/capes/mine            我的最愛與我用過的披風（要通行證）；已經移除的順手清掉並回報幾件
 *   GET    /api/idle/<雜湊>           沒有人穿、還在倒數的那一張圖（公開）
 *   POST   /api/cape/forget    {hash}        把一件從我的使用紀錄拿掉
 *   GET    /api/admin/reviews         審核名單（開發者）
 *   POST   /api/admin/review   {uuid, note?} 或 {hash}   列入審核：之後上傳的披風要先通過才給別人看（開發者）
 *   POST   /api/admin/unreview {uuid}        從審核名單移除（開發者）
 *   POST   /api/admin/approve  {hash}        通過審核：那張圖等審核的都放行（開發者）
 *   GET    /api/guild/<代碼>/<icon|banner>   貢獻公會的圖示／旗幟（公開）
 *   GET    /api/admin/guilds          貢獻公會的名單（開發者）
 *   POST   /api/admin/guild    {name, tag?}  加一個貢獻公會（開發者）
 *   PUT    /api/admin/guild/<代碼>/<icon|banner>   上傳那個公會的圖示／旗幟（開發者）
 *   POST   /api/admin/unguild  {slug}        移除一個貢獻公會（開發者）
 *   GET    /api/admin/staff           管理員名單（只有開發者）
 *   POST   /api/admin/staff    {who, note}   加一個管理員：Minecraft ID、UUID 或 Discord 代號（只有開發者）
 *   POST   /api/admin/unstaff  {key}         移除一個管理員（只有開發者）
 *   GET    /font/px-<代號>.ttf        預覽畫面用的點陣字（demo-fonts.js）
 *   GET    /tex/<檔名>                預覽畫面用的材質（demo-textures.js）
 *   GET    /template/<檔名>           披風範本、說明圖與示範（cape-templates.js）
 *   （排程）每天一次                  沒有人穿滿七天的披風刪掉（tidy）
 *
 * 一個 KV（綁定叫 CORPUS，沿用語料收集站那一個），沒有別的儲存。
 *
 * 它記什麼：玩家的 UUID（當成鍵）、要不要顯示披風、哪一天報到的；上傳的披風圖檔，
 * 連同上傳的人的帳號名（管理的人要知道是誰傳的）；被封禁的人。
 * 十四天沒再報到就從名單上消失（圖檔留著，回來報到就又出現）。
 * 它不記：IP、所在的世界、任何遊戲內容。
 *
 * 對外公開的名單與一覽不放 UUID 與帳號名，放的是 UUID 雜湊出來的代碼。
 *
 * 身分驗證用的是 Minecraft 多人伺服器自己那一套：模組向 Mojang 說「我要加入 <隨機字串>」，
 * 我們再問 Mojang「這個帳號剛剛是不是加入了 <隨機字串>」。模組的登入憑證不會經過這裡。
 * 網頁沒辦法自己做這件事，所以網頁的登入是<b>從遊戲裡開</b>：模組拿通行證換一條
 * 一天有效的網址，瀏覽器開了就是登入的。
 *
 * 部署方式見同一個資料夾的 README.md。
 */

import { LANGS, PATHS, pickLang, render, renderLost, tagOf } from './site.js';
import { FONTS } from './demo-fonts.js';
import { TEXTURES } from './demo-textures.js';
import { CAPE_TEMPLATES } from './cape-templates.js';
import { CERT_KEYS, PROFILE_KEYS } from './mojang-keys.js';
import { APPLE_TOUCH, FAVICON, ICON_192 } from './icons.js';

/** 公開代碼的樣子：24 個小寫十六進位字元。 */
const ID = /^[0-9a-f]{24}$/;
const UUID = /^[0-9a-f]{32}$/;
const NAME = /^[A-Za-z0-9_]{1,16}$/;
const BASE64 = /^[A-Za-z0-9+/]+={0,2}$/;

/** 登入要簽的那串字的開頭：綁住網域，這份簽章拿去別的地方沒有用。 */
export const LOGIN_PREFIX = 'wynnchayuan.com login\n';

/** 出的題目幾分鐘內要答。 */
const CHALLENGE_TTL_MS = 5 * 60 * 1000;

/** 個人檔案那份 Mojang 簽章最舊可以多舊。遊戲是啟動時抓的，開著幾天都有可能。 */
const PROFILE_MAX_AGE_MS = 30 * 24 * 3600 * 1000;

/** 登入那一包的上限：兩把公鑰的簽章加上個人檔案，三、四 KB。 */
const MAX_LOGIN_BODY = 12 * 1024;

/** 多久沒報到就從名單上消失。 */
const TTL_SECONDS = 14 * 24 * 3600;

/** 名單最多幾分鐘重算一次。 */
const LIST_MAX_AGE_MS = 10 * 60 * 1000;

/** 通行證的效期。過期了模組會自己重新驗一次。 */
const TOKEN_TTL_MS = 7 * 24 * 3600 * 1000;

/** 名單裡最多收幾個人。 */
const MAX_USERS = 50000;

/** JSON body 的上限。 */
const MAX_BODY = 512;

/** 披風圖檔的上限：名單上的成員（開發者、贊助者、貢獻者）。 */
export const MAX_CAPE_BYTES = 20 * 1024 * 1024;

/** 一般玩家的上限（使用者 2026-10-12：「一般使用者只能上傳 1mb」）。 */
export const MAX_PLAIN_CAPE_BYTES = 1024 * 1024;

/** GIF 最多幾格。 */
export const MAX_CAPE_FRAMES = 120;

/** 同一個人多久才能再傳一次。 */
const UPLOAD_EVERY_MS = 20 * 1000;

/**
 * 收哪幾種尺寸——就是 capes/templates 的範本：22×17 格的 1、2、4、8 倍
 * （左邊 1 ＋ 正面 10 ＋ 右邊 1 ＋ 背面 10 寬，上下緣 1 ＋ 面 16 高）。
 * 不照範本畫的圖不收，免得什麼圖都往上傳。
 */
// 22×17 格的 1、2、4、8、16、32、48 倍。最大那一張 1056×816（使用者 2026-10-12：「最多支援到 1000 左右」）。
// 遊戲裡最多用到 16 倍（352×272），再大的模組會自己縮；大的主要是網站上看得清楚。
export const CAPE_SIZES = [[22, 17], [44, 34], [88, 68], [176, 136], [352, 272], [704, 544], [1056, 816]];

const CREDITS_URL = 'https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/'
  + 'src/main/resources/assets/wynnchayuan/credits.json';
const CREDITS_MAX_AGE_MS = 10 * 60 * 1000;

const JSON_HEADERS = { 'content-type': 'application/json; charset=utf-8' };

function json(body, status = 200, extra = {}) {
  return new Response(JSON.stringify(body), { status, headers: { ...JSON_HEADERS, ...extra } });
}

function no(why, status = 400) {
  return json({ ok: false, why }, status);
}

function day(now) {
  return Math.floor(now / 86400000);
}

function hex(buffer) {
  return [...new Uint8Array(buffer)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

/** UUID 的公開代碼：SHA-256("wynnchayuan:" + 沒有連字號的小寫 UUID) 的前 12 個位元組。 */
export async function publicId(uuid) {
  const data = new TextEncoder().encode('wynnchayuan:' + uuid);
  return hex(await crypto.subtle.digest('SHA-256', data)).slice(0, 24);
}

// ---------------------------------------------------------------- 通行證

let cachedSecret = null;

/** 簽通行證用的密鑰。第一次用到時自己產生一把，存在 KV 裡。 */
async function secret(env) {
  if (cachedSecret) return cachedSecret;
  let value = await env.CORPUS.get('auth:secret');
  if (!value) {
    value = hex(crypto.getRandomValues(new Uint8Array(32)));
    await env.CORPUS.put('auth:secret', value);
  }
  cachedSecret = value;
  return value;
}

async function sign(env, text) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(await secret(env)),
    { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return hex(await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(text)));
}

export async function issueToken(env, uuid, name, now) {
  const body = uuid + '.' + name + '.' + (now + TOKEN_TTL_MS);
  return body + '.' + await sign(env, body);
}

/** 通行證對的話回 {uuid, name}，不對或過期回 null。 */
export async function readToken(env, token, now) {
  if (typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 4) return null;
  const [uuid, name, expires, signature] = parts;
  if (!UUID.test(uuid) || !NAME.test(name) || !/^\d{1,16}$/.test(expires)) return null;
  if (Number(expires) < now) return null;
  const expected = await sign(env, uuid + '.' + name + '.' + expires);
  if (expected.length !== signature.length) return null;
  let diff = 0;
  for (let i = 0; i < expected.length; i++) diff |= expected.charCodeAt(i) ^ signature.charCodeAt(i);
  return diff === 0 ? { uuid, name } : null;
}

/** 網頁的通行證多久過期。從遊戲裡再開一次就有新的。 */
const WEB_TTL_MS = 24 * 3600 * 1000;

/** 給瀏覽器用的通行證：前面多一個 web，效期短。網址會留在瀏覽紀錄裡，所以不發長的。 */
export async function issueWebToken(env, uuid, name, now) {
  const body = 'web.' + uuid + '.' + name + '.' + (now + WEB_TTL_MS);
  return body + '.' + await sign(env, body);
}

async function readWebToken(env, token, now) {
  const parts = token.split('.');
  if (parts.length !== 5 || parts[0] !== 'web') return null;
  const [, uuid, name, expires, signature] = parts;
  if (!UUID.test(uuid) || !NAME.test(name) || !/^\d{1,16}$/.test(expires)) return null;
  if (Number(expires) < now) return null;
  const expected = await sign(env, 'web.' + uuid + '.' + name + '.' + expires);
  if (expected.length !== signature.length) return null;
  let diff = 0;
  for (let i = 0; i < expected.length; i++) diff |= expected.charCodeAt(i) ^ signature.charCodeAt(i);
  return diff === 0 ? { uuid, name, web: true } : null;
}

/** 這個請求是誰發的：模組的通行證或網頁的通行證都認。 */
async function who(request, env, now) {
  const header = request.headers.get('authorization') || '';
  if (!header.startsWith('Bearer ')) return null;
  const token = header.slice(7);
  if (token.startsWith('dc.')) {
    // 網站帳號：綁了遊戲 ID 才算「是某個玩家」。沒綁的只有 /me 與綁定那幾條路認得（discordSession）。
    const session = await discordSession(request, env, now);
    const linked = session && session.account;
    return linked && UUID.test(linked.u || '') && NAME.test(linked.m || '')
      ? { uuid: linked.u, name: linked.m, web: true, discord: session.id, discordName: linked.n || '' }
      : null;
  }
  return token.startsWith('web.') ? readWebToken(env, token, now) : readToken(env, token, now);
}

// ---------------------------------------------------------------- 網站帳號（Discord）
//
// 使用者 2026-10-12：「像 Wynntils 一樣先註冊，然後再綁定遊戲 ID」「或許可以用 Discord 登入」。
//
//   1. 註冊／登入：Discord 的 OAuth（只要 identify：代號與顯示名稱）。
//   2. 綁定：網站給一組綁定碼，玩家在遊戲裡打 /wcy link <碼>。模組帶著它自己的通行證
//      （那一張是驗過 Mojang 憑證才發的）把碼送來，這裡就知道「這個 Discord 帳號＝這個遊戲帳號」。
//
// Discord 只證明 Discord 帳號；「遊戲帳號是本人」靠的還是第 2 步模組那張通行證。
// 所以綁定一定要從遊戲裡來——在網頁上自己填遊戲 ID 是不算數的。
//
// KV：d:<Discord 代號> → {n 顯示名稱, u 遊戲 UUID, m 遊戲名字, t 綁定時間}；
//     l:<UUID> → Discord 代號；link:<綁定碼> → Discord 代號（十分鐘）。

const DISCORD_API = 'https://discord.com/api';
const DISCORD_ID = /^\d{5,25}$/;

/** 網站帳號登入一次管多久。 */
const SESSION_TTL_MS = 30 * 24 * 3600 * 1000;

/** 綁定碼幾秒內要用掉。 */
const LINK_TTL_SECONDS = 600;

/** 綁定碼用的字：拿掉長得像的 0／O、1／I。 */
const LINK_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const LINK_CODE = /^[A-HJ-NP-Z2-9]{8}$/;

function discordOn(env) {
  return Boolean(env.DISCORD_CLIENT_ID && env.DISCORD_CLIENT_SECRET);
}

export async function issueDiscordToken(env, id, now) {
  const body = 'dc.' + id + '.' + (now + SESSION_TTL_MS);
  return body + '.' + await sign(env, body);
}

async function readDiscordToken(env, token, now) {
  const parts = token.split('.');
  if (parts.length !== 4 || parts[0] !== 'dc') return null;
  const [, id, expires, signature] = parts;
  if (!DISCORD_ID.test(id) || !/^\d{1,16}$/.test(expires) || Number(expires) < now) return null;
  const expected = await sign(env, 'dc.' + id + '.' + expires);
  if (expected.length !== signature.length) return null;
  let diff = 0;
  for (let i = 0; i < expected.length; i++) diff |= expected.charCodeAt(i) ^ signature.charCodeAt(i);
  return diff === 0 ? id : null;
}

/** 網站帳號存回去。名字與綁的遊戲帳號也放進 metadata，管理頁搜尋時只要列出來、不必一筆一筆讀。 */
async function saveAccount(env, id, record) {
  const metadata = {};
  if (record.n) metadata.n = record.n;
  if (record.m) metadata.m = record.m;
  if (record.u) metadata.u = record.u;
  await env.CORPUS.put('d:' + id, JSON.stringify(record), { metadata });
}

async function account(env, id) {
  try {
    const raw = await env.CORPUS.get('d:' + id);
    const value = raw ? JSON.parse(raw) : null;
    return value && typeof value === 'object' ? value : null;
  } catch (e) {
    return null;
  }
}

/** 這個請求帶著的網站帳號：{id, account}。沒登入、過期、帳號不在了都是 null。 */
async function discordSession(request, env, now) {
  const header = request.headers.get('authorization') || '';
  if (!header.startsWith('Bearer dc.')) return null;
  const id = await readDiscordToken(env, header.slice(7), now);
  if (!id) return null;
  const found = await account(env, id);
  return found ? { id, account: found } : null;
}

function redirect(location, cookie) {
  const headers = { location, 'cache-control': 'no-store', 'referrer-policy': 'no-referrer' };
  if (cookie) headers['set-cookie'] = cookie;
  return new Response(null, { status: 302, headers });
}

const OAUTH_COOKIE = 'wcy_oauth';
const oauthCookie = (value, age) => OAUTH_COOKIE + '=' + value
  + '; Path=/auth; Max-Age=' + age + '; HttpOnly; Secure; SameSite=Lax';

const INTENT = /^[0-9a-f]{32}$/;
const INTENT_COOKIE = 'wcy_intent';

/** 綁定：一個遊戲帳號只綁一個網站帳號，反過來也是，兩邊原本綁著別的都先解掉。 */
async function bind(env, id, uuid, name, now) {
  const record = await account(env, id) || {};
  const before = await env.CORPUS.get('l:' + uuid);
  if (before && before !== id) {
    const other = await account(env, before);
    if (other) {
      delete other.u;
      delete other.m;
      delete other.t;
      await saveAccount(env, before, other);
    }
  }
  if (record.u && record.u !== uuid) await env.CORPUS.delete('l:' + record.u);
  record.u = uuid;
  record.m = name;
  record.t = now;
  await saveAccount(env, id, record);
  await env.CORPUS.put('l:' + uuid, id);
  return record;
}

/**
 * 已經以遊戲帳號登入的人（從遊戲開的網頁）要綁 Discord：記下「接下來登入的那個 Discord 帳號
 * 綁到這個遊戲帳號」，十分鐘內有效，用完就丟。
 *
 * <p>這張單子另外用 cookie 綁在開單的瀏覽器上：別人把他開的單子連結塞給你、你去 Discord
 * 登入的話，你的 Discord 就會被綁到他的遊戲帳號——沒有那個 cookie 的瀏覽器不認這張單子。
 */
async function handleLinkIntent(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  if (user.discord) return no('already linked', 409);
  if (!discordOn(env)) return no('discord off', 503);
  const id = hex(crypto.getRandomValues(new Uint8Array(16)));
  await env.CORPUS.put('intent:' + id, JSON.stringify({ u: user.uuid, m: user.name }),
    { expirationTtl: LINK_TTL_SECONDS });
  return json({ ok: true, url: '/auth/discord?intent=' + id }, 200, {
    'cache-control': 'no-store',
    'set-cookie': INTENT_COOKIE + '=' + id + '; Path=/auth; Max-Age=600; HttpOnly; Secure; SameSite=Lax',
  });
}

/** 去 Discord 登入。state 自己簽、再用 cookie 綁在這個瀏覽器上，別人拿不走也塞不進來。 */
async function handleDiscordStart(request, env, now) {
  const url = new URL(request.url);
  const origin = url.origin;
  if (!discordOn(env)) return redirect(origin + '/account#e=discord-off');
  // 帶著綁定單的：單子要是這個瀏覽器自己開的（cookie 對得上）才帶進 state
  const asked = url.searchParams.get('intent') || '';
  const mine = new RegExp('(?:^|;\\s*)' + INTENT_COOKIE + '=([0-9a-f]{32})')
    .exec(request.headers.get('cookie') || '');
  const intent = INTENT.test(asked) && mine && mine[1] === asked ? asked : '';
  const nonce = hex(crypto.getRandomValues(new Uint8Array(16))) + intent;
  const state = now + '.' + nonce + '.' + await sign(env, 'oauth.' + now + '.' + nonce);
  const to = new URL('https://discord.com/oauth2/authorize');
  to.searchParams.set('client_id', env.DISCORD_CLIENT_ID);
  to.searchParams.set('response_type', 'code');
  to.searchParams.set('redirect_uri', origin + '/auth/discord/callback');
  to.searchParams.set('scope', 'identify');
  to.searchParams.set('state', state);
  return redirect(to.toString(), oauthCookie(nonce, 600));
}

async function handleDiscordCallback(request, env, now) {
  const url = new URL(request.url);
  const failed = redirect(url.origin + '/account#e=discord', oauthCookie('', 0));
  if (!discordOn(env)) return failed;
  const code = url.searchParams.get('code') || '';
  const parts = (url.searchParams.get('state') || '').split('.');
  const cookie = new RegExp('(?:^|;\\s*)' + OAUTH_COOKIE + '=([0-9a-f]{32}(?:[0-9a-f]{32})?)(?:;|$)')
    .exec(request.headers.get('cookie') || '');
  if (!code || code.length > 512 || parts.length !== 3 || !cookie || cookie[1] !== parts[1]) return failed;
  if (!/^\d{1,16}$/.test(parts[0]) || now - Number(parts[0]) > 600000 || Number(parts[0]) > now + 60000) return failed;
  const expected = await sign(env, 'oauth.' + parts[0] + '.' + parts[1]);
  if (expected !== parts[2]) return failed;
  let profile = null;
  try {
    const token = await fetch(DISCORD_API + '/oauth2/token', {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        client_id: env.DISCORD_CLIENT_ID,
        client_secret: env.DISCORD_CLIENT_SECRET,
        grant_type: 'authorization_code',
        code,
        redirect_uri: url.origin + '/auth/discord/callback',
      }).toString(),
    });
    if (token.status !== 200) return failed;
    const access = (await token.json()).access_token;
    if (typeof access !== 'string' || !access) return failed;
    const me = await fetch(DISCORD_API + '/users/@me', { headers: { authorization: 'Bearer ' + access } });
    if (me.status !== 200) return failed;
    profile = await me.json();
  } catch (e) {
    return failed;
  }
  const id = profile && typeof profile.id === 'string' ? profile.id : '';
  if (!DISCORD_ID.test(id)) return failed;
  const shown = String(profile.global_name || profile.username || '').slice(0, 64);
  const record = await account(env, id) || {};
  record.n = shown;
  await saveAccount(env, id, record);
  // state 後半帶著綁定單：這個人是從「已經以遊戲帳號登入」的網頁來的，登入完直接綁
  const intent = parts[1].slice(32);
  if (INTENT.test(intent)) {
    try {
      const wanted = JSON.parse(await env.CORPUS.get('intent:' + intent) || 'null');
      if (wanted && UUID.test(wanted.u || '') && NAME.test(wanted.m || '')) {
        await bind(env, id, wanted.u, wanted.m, now);
      }
    } catch (e) {
      // 單子壞了或過期了：照樣登入，只是沒綁
    }
    await env.CORPUS.delete('intent:' + intent);
  }
  // 憑證放在 # 後面：那一段不會送到任何伺服器，網頁收進 localStorage 之後就從網址列拿掉
  return redirect(url.origin + '/account#t=' + await issueDiscordToken(env, id, now), oauthCookie('', 0));
}

/** 網站帳號要一組綁定碼。 */
async function handleLinkStart(request, env, now) {
  const session = await discordSession(request, env, now);
  if (!session) return no('login first', 401);
  const bytes = crypto.getRandomValues(new Uint8Array(8));
  let code = '';
  for (const b of bytes) code += LINK_ALPHABET[b % LINK_ALPHABET.length];
  await env.CORPUS.put('link:' + code, session.id, { expirationTtl: LINK_TTL_SECONDS });
  return json({ ok: true, code, minutes: LINK_TTL_SECONDS / 60 });
}

/** 遊戲裡打 /wcy link <碼>：模組帶著它的通行證把碼送來。 */
async function handleLink(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  // 要的是「遊戲帳號是本人」的證明。Discord 那一張本身就是綁定的結果，不能拿來再綁別人。
  if (user.discord) return no('use the game', 403);
  const body = await readJson(request);
  if (body === undefined) return no('too large', 413);
  const code = body && typeof body.code === 'string' ? body.code.trim().toUpperCase() : '';
  if (!LINK_CODE.test(code)) return no('bad code');
  const id = await env.CORPUS.get('link:' + code);
  if (!id || !DISCORD_ID.test(id)) return no('unknown code', 404);
  const record = await bind(env, id, user.uuid, user.name, now);
  await env.CORPUS.delete('link:' + code);
  return json({ ok: true, discord: record.n || '' });
}

/** 網站帳號解除綁定。 */
async function handleUnlink(request, env, now) {
  const session = await discordSession(request, env, now);
  if (!session) return no('login first', 401);
  const record = session.account;
  if (record.u) {
    await env.CORPUS.delete('l:' + record.u);
    delete record.u;
    delete record.m;
    delete record.t;
    await saveAccount(env, session.id, record);
  }
  return json({ ok: true });
}

/** 遊戲登入時順手：這個帳號有綁網站帳號的話，把記著的名字換成 Mojang 剛簽的那一個。 */
async function refreshLinkedName(env, uuid, name) {
  const id = await env.CORPUS.get('l:' + uuid);
  if (!id) return;
  const record = await account(env, id);
  if (record && record.u === uuid && record.m !== name) {
    record.m = name;
    await saveAccount(env, id, record);
  }
}

async function readJson(request, limit = MAX_BODY) {
  const length = Number(request.headers.get('content-length') || 0);
  if (length > limit) return undefined;
  try {
    const text = await request.text();
    if (text.length > limit) return undefined;
    const body = JSON.parse(text);
    return body && typeof body === 'object' && !Array.isArray(body) ? body : null;
  } catch (e) {
    return null;
  }
}

// ---------------------------------------------------------------- 登入
//
// 要證明的是「這個遊戲帳號是本人」。原本的做法是 Minecraft 多人伺服器那一套
// （模組向 Mojang 說「我要加入 X」，這裡再問 Mojang「他加入 X 了嗎」），但 Mojang 把
// Cloudflare Worker 發出去的請求一律擋掉（403），那個問題永遠問不到答案——
// 部署之後所有人都登不進來（使用者 2026-10-12）。
//
// 現在不問 Mojang，改驗它<b>已經簽好</b>的東西：
//
//   1. 玩家金鑰憑證。1.19 起每個正版帳號都有一對 Mojang 發的金鑰（聊天簽章用的），
//      公鑰連同 UUID 與到期時間由 Mojang 簽過。私鑰要帳號的登入憑證才拿得到。
//   2. 這裡出一道題（challenge），模組用那把私鑰簽。簽得出來＝手上有私鑰＝是本人。
//   3. 名字不在憑證裡，另外驗個人檔案的 textures 屬性——那也是 Mojang 簽的，
//      裡面有 UUID、名字與時間。這份資料是公開的（誰都抓得到任何人的），所以它只用來
//      回答「這個 UUID 叫什麼名字」，不能拿來證明身分；身分靠的是上面兩步。
//
// 模組送來的沒有一樣是祕密：公鑰、簽章、公開的個人檔案。私鑰與登入憑證都留在玩家那裡。

function fromBase64(text, max) {
  if (typeof text !== 'string' || !text || text.length > max || !BASE64.test(text)) return null;
  try {
    const raw = atob(text);
    const out = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
    return out;
  } catch (e) {
    return null;
  }
}

/**
 * Mojang 的公鑰。預設是釘在 mojang-keys.js 的那一份；MOJANG_KEYS（publickeys 那個
 * JSON 的樣子，值可以是字串或 {publicKey}）蓋過它——公鑰換了、來不及重新部署時用。
 */
function mojangKeys(env, name) {
  if (env.MOJANG_KEYS) {
    try {
      const all = JSON.parse(env.MOJANG_KEYS);
      const list = (all[name] || []).map((k) => (typeof k === 'string' ? k : k && k.publicKey))
        .filter((k) => typeof k === 'string');
      if (list.length) return list;
    } catch (e) {
      // 寫壞了就用釘住的那一份
    }
  }
  return name === 'playerCertificateKeys' ? CERT_KEYS : PROFILE_KEYS;
}

/** 這份簽章是不是其中一把公鑰簽的。 */
async function signedBy(keys, hash, signature, data) {
  for (const spki of keys) {
    try {
      const key = await crypto.subtle.importKey('spki', fromBase64(spki, 4096),
        { name: 'RSASSA-PKCS1-v1_5', hash }, false, ['verify']);
      if (await crypto.subtle.verify('RSASSA-PKCS1-v1_5', key, signature, data)) return true;
    } catch (e) {
      // 這一把讀不進來就試下一把
    }
  }
  return false;
}

/** Mojang 簽憑證時簽的內容：UUID 16 個位元組、到期時間（毫秒）8 個位元組、公鑰本體。 */
export function certificatePayload(uuid, expiresAt, publicKey) {
  const out = new Uint8Array(24 + publicKey.length);
  for (let i = 0; i < 16; i++) out[i] = parseInt(uuid.slice(i * 2, i * 2 + 2), 16);
  new DataView(out.buffer).setBigUint64(16, BigInt(expiresAt));
  out.set(publicKey, 24);
  return out;
}

/** 出一道題：時間、一段亂數，加上我們自己的簽章（不必記在哪裡）。 */
export async function issueChallenge(env, now) {
  const body = now + '.' + hex(crypto.getRandomValues(new Uint8Array(16)));
  return body + '.' + await sign(env, 'challenge.' + body);
}

async function challengeFresh(env, challenge, now) {
  if (typeof challenge !== 'string' || challenge.length > 128) return false;
  const parts = challenge.split('.');
  if (parts.length !== 3 || !/^\d{1,16}$/.test(parts[0]) || !/^[0-9a-f]{32}$/.test(parts[1])) return false;
  const issued = Number(parts[0]);
  if (issued > now + 60000 || now - issued > CHALLENGE_TTL_MS) return false;
  const expected = await sign(env, 'challenge.' + parts[0] + '.' + parts[1]);
  if (expected.length !== parts[2].length) return false;
  let diff = 0;
  for (let i = 0; i < expected.length; i++) diff |= expected.charCodeAt(i) ^ parts[2].charCodeAt(i);
  return diff === 0;
}

async function handleChallenge(env, now) {
  return json({ ok: true, challenge: await issueChallenge(env, now) }, 200, { 'cache-control': 'no-store' });
}

/** 個人檔案那份屬性裡的名字：簽章對、UUID 對、不太舊才算數。 */
async function profileName(env, textures, uuid, now) {
  if (!textures || typeof textures !== 'object') return null;
  const signature = fromBase64(textures.signature, 1024);
  if (!signature || typeof textures.value !== 'string' || textures.value.length > 6000) return null;
  const value = fromBase64(textures.value, 6000);
  if (!value) return null;
  const signed = new TextEncoder().encode(textures.value);     // 簽的是那串 base64 本身
  if (!await signedBy(mojangKeys(env, 'profilePropertyKeys'), 'SHA-1', signature, signed)) return null;
  try {
    const profile = JSON.parse(new TextDecoder().decode(value));
    const id = String(profile.profileId || '').replace(/-/g, '').toLowerCase();
    const age = now - Number(profile.timestamp);
    if (id !== uuid || !NAME.test(profile.profileName || '')) return null;
    if (!(age <= PROFILE_MAX_AGE_MS && age >= -86400000)) return null;
    return profile.profileName;
  } catch (e) {
    return null;
  }
}

async function handleLogin(request, env, now) {
  const body = await readJson(request, MAX_LOGIN_BODY);
  if (body === undefined) return no('too large', 413);
  if (!body || typeof body.uuid !== 'string' || !UUID.test(body.uuid)
      || !Number.isSafeInteger(body.expiresAt) || body.expiresAt <= 0) {
    return no('bad login');
  }
  const publicKey = fromBase64(body.publicKey, 1024);
  const keySignature = fromBase64(body.keySignature, 1024);
  const signature = fromBase64(body.signature, 1024);
  if (!publicKey || !keySignature || !signature || typeof body.challenge !== 'string') {
    return no('bad login');
  }
  // 先驗自己出的題：亂送的請求在這裡就擋掉，不必算 RSA
  if (!await challengeFresh(env, body.challenge, now)) return no('stale challenge', 403);
  if (body.expiresAt <= now) return no('certificate expired', 403);
  if (!await signedBy(mojangKeys(env, 'playerCertificateKeys'), 'SHA-1', keySignature,
    certificatePayload(body.uuid, body.expiresAt, publicKey))) {
    return no('bad certificate', 403);
  }
  let answered = false;
  try {
    const key = await crypto.subtle.importKey('spki', publicKey,
      { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['verify']);
    answered = await crypto.subtle.verify('RSASSA-PKCS1-v1_5', key, signature,
      new TextEncoder().encode(LOGIN_PREFIX + body.challenge + '\n' + body.uuid));
  } catch (e) {
    answered = false;
  }
  if (!answered) return no('bad signature', 403);
  const name = await profileName(env, body.textures, body.uuid, now);
  if (!name) return no('bad profile', 403);
  // 管理權是照名字認的：綁著網站帳號的話，那邊記的名字不可以比這裡舊
  await refreshLinkedName(env, body.uuid, name);
  // member：模組在送出之前先擋 GIF 與 1 MB，它要知道這個人是不是有成員的待遇
  // （credits.json 它自己看得到，特別名單只有這裡知道）
  return json({ ok: true, token: await issueToken(env, body.uuid, name, now), id: await publicId(body.uuid),
    member: await isMember(env, { uuid: body.uuid, name }, now) });
}

// ---------------------------------------------------------------- 報到與名單

/** 這一次報到要不要寫進 KV：沒登記過、設定變了、披風換了、或上次是昨天以前。 */
export function needsWrite(existing, next) {
  if (!existing) return true;
  return existing.c !== next.c || existing.d !== next.d || existing.v !== next.v
    || (existing.r || 0) !== (next.r || 0) || existing.s !== next.s;
}

const ROLE = /^[a-z]{1,24}$/;

/**
 * 報到時帶的徽章選擇（使用者 2026-10-12：「我有開啟全彩模式，他看我也只是金色而已」）。
 *
 * 模組的「我的徽章」原本只存在自己的電腦上，別人看到的是 credits.json 登記的那一份。
 * 現在跟著報到送來：rainbow（葉子用彩色）、show（要展示哪幾個身分的葉子）。
 * 這裡只管記與發；「彩色是開發者才有的」「只能展示自己真的有的身分」由看的人那一邊
 * 對著 credits.json 判斷——那份名單是公開的，亂送也畫不出不屬於他的東西。
 *
 * @returns undefined 沒帶（舊版模組，或從別的路來的）；null 格式不對；否則 {r, s}
 */
export function badgePrefs(body) {
  if (!('rainbow' in body) && !('show' in body)) return undefined;
  if ('rainbow' in body && typeof body.rainbow !== 'boolean') return null;
  let shown;
  if ('show' in body) {
    if (!Array.isArray(body.show) || body.show.length > 8) return null;
    if (!body.show.every((role) => typeof role === 'string' && ROLE.test(role))) return null;
    shown = [...new Set(body.show)].sort().join(',');
  }
  return { r: body.rainbow === true ? 1 : 0, s: shown };
}

async function handleHello(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const body = await readJson(request);
  if (body === undefined) return no('too large', 413);
  if (!body || ('cape' in body && typeof body.cape !== 'boolean')) return no('bad hello');
  const prefs = badgePrefs(body);
  if (prefs === null) return no('bad hello');
  const record = await remember(env, user.uuid, body.cape !== false, now, prefs);
  return json({ ok: true, id: record.i, cape: record.v > 0 });
}

/** 把一個人的現況寫進 KV（有變才寫），順手改進現成的名單。回傳他那一筆。 */
async function remember(env, uuid, wantsCape, now, prefs) {
  const id = await publicId(uuid);
  const key = 'u:' + uuid;
  const have = await env.CORPUS.getWithMetadata(key);
  const cape = await env.CORPUS.getWithMetadata('cape:' + id, { type: 'stream' });
  // 等審核的披風不給別人看：名單上當成沒有
  const version = cape && cape.metadata && !cape.metadata.r ? cape.metadata.v : 0;
  if (cape && cape.value && cape.value.cancel) cape.value.cancel();
  const next = { i: id, c: wantsCape ? 1 : 0, d: day(now), v: version };
  const before = have && have.metadata;
  // 徽章的選擇：這一次有帶就用帶來的，沒帶（上傳披風那幾條路）就留著原本的
  const rainbow = prefs ? prefs.r : (before && before.r) || 0;
  const shown = prefs ? prefs.s : before && before.s;
  if (rainbow) next.r = 1;
  if (typeof shown === 'string') next.s = shown;
  if (needsWrite(before, next)) {
    await env.CORPUS.put(key, '', { expirationTtl: TTL_SECONDS, metadata: next });
    if (!before || before.c !== next.c || before.v !== next.v
        || (before.r || 0) !== (next.r || 0) || before.s !== next.s) {
      await patchList(env, next);
    }
  }
  return next;
}

/** 名單套上一個人的現況之後的樣子；沒有變就回 null。純函式。 */
export function applyRecord(list, record) {
  const known = list.users.includes(record.i);
  const shown = record.c === 1 && record.v > 0 ? record.v : 0;
  const hidden = record.c !== 1;
  const wasShown = list.capes[record.i] || 0;
  const wasHidden = list.hidden.includes(record.i);
  const rainbows = list.rainbow || [];
  const roles = list.show || {};
  const rainbow = record.r === 1;
  const wasRainbow = rainbows.includes(record.i);
  const picked = typeof record.s === 'string' ? record.s : undefined;
  if (known && shown === wasShown && hidden === wasHidden && rainbow === wasRainbow
      && picked === roles[record.i]) {
    return null;
  }
  const capes = { ...list.capes };
  if (shown) capes[record.i] = shown; else delete capes[record.i];
  const show = { ...roles };
  if (picked === undefined) delete show[record.i]; else show[record.i] = picked;
  return {
    t: list.t,
    users: known ? list.users : [...list.users, record.i],
    capes,
    hidden: hidden ? (wasHidden ? list.hidden : [...list.hidden, record.i])
      : list.hidden.filter((id) => id !== record.i),
    rainbow: rainbow ? (wasRainbow ? rainbows : [...rainbows, record.i])
      : rainbows.filter((id) => id !== record.i),
    show,
  };
}

function validList(list) {
  return list && typeof list.t === 'number' && Array.isArray(list.users)
    && list.capes && typeof list.capes === 'object' && Array.isArray(list.hidden);
}

async function storedList(env) {
  try {
    const list = JSON.parse((await env.CORPUS.get('presence:list')) || 'null');
    return validList(list) ? list : null;
  } catch (e) {
    return null;
  }
}

async function patchList(env, record) {
  const list = await storedList(env);
  if (!list) return;                       // 還沒算過，等 /list 來算
  const patched = applyRecord(list, record);
  if (patched) await env.CORPUS.put('presence:list', JSON.stringify(patched));
}

async function buildList(env, now) {
  const users = [];
  const capes = {};
  const hidden = [];
  const rainbow = [];
  const show = {};
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'u:', limit: 1000, cursor });
    for (const k of page.keys) {
      const m = k.metadata;
      if (!m || !ID.test(m.i)) continue;
      users.push(m.i);
      if (m.c !== 1) hidden.push(m.i);
      else if (m.v > 0) capes[m.i] = m.v;
      if (m.r === 1) rainbow.push(m.i);
      if (typeof m.s === 'string') show[m.i] = m.s;
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor && users.length < MAX_USERS);
  return { t: now, users, capes, hidden, rainbow, show };
}

async function handleList(env, now) {
  let list = await storedList(env);
  if (!list || now - list.t > LIST_MAX_AGE_MS) {
    list = await buildList(env, now);
    await env.CORPUS.put('presence:list', JSON.stringify(list));
  }
  return json(list, 200, { 'cache-control': 'public, max-age=60' });
}

// ---------------------------------------------------------------- 披風

/**
 * 這個檔是不是一件收得下的披風。
 *
 * @returns {{type: 'png'|'gif', width: number, height: number, frames: number}|{why: string}}
 */
export function inspectCape(bytes) {
  if (bytes.length > MAX_CAPE_BYTES) return { why: 'too large' };
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  let type;
  let width;
  let height;
  let frames = 1;
  const png = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
  if (bytes.length >= 24 && png.every((b, i) => bytes[i] === b)
      && String.fromCharCode(bytes[12], bytes[13], bytes[14], bytes[15]) === 'IHDR') {
    type = 'png';
    width = view.getUint32(16);
    height = view.getUint32(20);
  } else if (bytes.length >= 13 && bytes[0] === 0x47 && bytes[1] === 0x49 && bytes[2] === 0x46
      && bytes[3] === 0x38 && (bytes[4] === 0x37 || bytes[4] === 0x39) && bytes[5] === 0x61) {
    type = 'gif';
    width = view.getUint16(6, true);
    height = view.getUint16(8, true);
    frames = gifFrames(bytes);
    if (frames < 1) return { why: 'bad format' };
    if (frames > MAX_CAPE_FRAMES) return { why: 'too many frames' };
  } else {
    return { why: 'bad format' };
  }
  if (!CAPE_SIZES.some(([w, h]) => w === width && h === height)) return { why: 'bad size' };
  return { type, width, height, frames };
}

/** GIF 有幾格：只走區塊、不解壓。檔案不完整回 0。 */
export function gifFrames(bytes) {
  let pos = 13;
  const flags = bytes[10];
  if (flags & 0x80) pos += 3 * (1 << ((flags & 7) + 1));
  let frames = 0;
  const skip = () => {
    while (pos < bytes.length) {
      const size = bytes[pos++];
      if (size === 0) return true;
      pos += size;
    }
    return false;
  };
  while (pos < bytes.length) {
    const block = bytes[pos++];
    if (block === 0x3b) return frames;
    if (block === 0x21) {
      pos++;
      if (!skip()) return 0;
    } else if (block === 0x2c) {
      if (pos + 9 > bytes.length) return 0;
      const local = bytes[pos + 8];
      pos += 9;
      if (local & 0x80) pos += 3 * (1 << ((local & 7) + 1));
      pos++;
      if (!skip()) return 0;
      frames++;
      if (frames > MAX_CAPE_FRAMES) return frames;
    } else {
      return 0;
    }
  }
  return 0;                                // 沒有檔尾：下載到一半的檔
}

let cachedCredits = null;

const COLOUR = /^#[0-9a-fA-F]{6}$/;

/**
 * 首頁「團隊」那一段要畫的：開發者、贊助者、貢獻者三區，各自的葉子顏色與成員。
 * 只留畫得出來的欄位（名字、遊戲 ID、自訂的葉子顏色）；資料來源那一區不是人，不放。
 * 純函式，測試直接餵。
 */
export function teamOf(sections) {
  const team = [];
  for (const section of Array.isArray(sections) ? sections : []) {
    const found = /\.(developers|sponsors|contributors)$/.exec(String(section && section.key));
    if (!found || !Array.isArray(section.members)) continue;
    const members = [];
    for (const member of section.members) {
      if (!member || typeof member.name !== 'string' || !member.name.trim()) continue;
      const entry = { name: member.name.trim().slice(0, 40) };
      if (typeof member.mc === 'string' && NAME.test(member.mc)) entry.mc = member.mc;
      if (typeof member.color === 'string' && COLOUR.test(member.color)) entry.color = member.color;
      members.push(entry);
    }
    team.push({ role: found[1], color: COLOUR.test(section.color || '') ? section.color : null, members });
  }
  return team;
}

/** 名單上的人，加上貢獻公會。抓不到名單（GitHub 沒回）就請訪客等一下再來。 */
async function handleTeam(env, now) {
  const list = await credits(now);
  if (!list) return no('try later', 503);
  const guilds = (await guildList(env, now)).map((guild) => ({
    slug: guild.slug, name: guild.name, tag: guild.tag || '', icon: guild.i || 0, banner: guild.b || 0 }));
  return json({ team: list.team, guilds }, 200, { 'cache-control': 'public, max-age=300' });
}

// ---------------------------------------------------------------- 貢獻公會
//
// 使用者 2026-10-13：「新增貢獻者公會（該團體提供巨大幫助或成員之間協助），希望可以放上
// WYNNCRAFT 的旗幟與 icon（我們自己上傳）」。
//
// 在管理頁加：公會的名字（連到 wynncraft.com/stats/guild/<名字>）、標籤，再各上傳一張圖示與旗幟。
// KV：guilds → 整份名單（JSON 陣列：{slug, name, tag, t, by, i 圖示的版本, b 旗幟的版本}）。
// 名單放在同一筆裡：首頁每開一次都要讀，一次 get 比一次 list 便宜得多。
// 圖在 gimg:<代碼>:icon、gimg:<代碼>:banner，metadata 記它是哪一種圖。

/** Wynncraft 的公會名：英文字母與空格，3 到 30 個字。 */
const GUILD_NAME = /^[A-Za-z](?:[A-Za-z ]{1,28})[A-Za-z]$/;
const GUILD_TAG = /^[A-Za-z]{3,4}$/;
const GUILD_SLUG = /^[a-z](?:[a-z-]{1,28})[a-z]$/;

/** 公會的圖最大可以多大（網頁上傳之前會先縮小，正常不會碰到）。 */
export const MAX_GUILD_IMAGE = 512 * 1024;

/** 最多登記幾個貢獻公會。 */
const MAX_GUILDS = 24;

/** 公會名在這裡的代碼：小寫、空格換成連字號；不像公會名回 null。 */
export function guildSlug(name) {
  const raw = String(name == null ? '' : name).trim().replace(/\s+/g, ' ');
  return GUILD_NAME.test(raw) ? raw.toLowerCase().replace(/ /g, '-') : null;
}

/** 這些位元組是哪一種圖：只認 PNG、JPEG、WebP；別的回 null。 */
export function imageType(bytes) {
  const b = bytes;
  if (b.length > 8 && b[0] === 0x89 && b[1] === 0x50 && b[2] === 0x4e && b[3] === 0x47) return 'image/png';
  if (b.length > 3 && b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff) return 'image/jpeg';
  if (b.length > 12 && b[0] === 0x52 && b[1] === 0x49 && b[2] === 0x46 && b[3] === 0x46
    && b[8] === 0x57 && b[9] === 0x45 && b[10] === 0x42 && b[11] === 0x50) return 'image/webp';
  return null;
}

let cachedGuilds = null;

async function guildList(env, now) {
  if (cachedGuilds && cachedGuilds.kv === env.CORPUS && now - cachedGuilds.t < 60 * 1000) return cachedGuilds.list;
  let list = [];
  try {
    const parsed = JSON.parse((await env.CORPUS.get('guilds')) || '[]');
    if (Array.isArray(parsed)) {
      list = parsed.filter((g) => g && typeof g.slug === 'string' && GUILD_SLUG.test(g.slug) && typeof g.name === 'string');
    }
  } catch (e) {
    list = [];
  }
  cachedGuilds = { t: now, kv: env.CORPUS, list };
  return list;
}

async function saveGuilds(env, list, now) {
  await env.CORPUS.put('guilds', JSON.stringify(list));
  cachedGuilds = { t: now, kv: env.CORPUS, list };
}

/** 寫入之前重讀一次：名單是整筆覆寫的，不能拿一分鐘前的那一份去改。 */
async function freshGuilds(env, now) {
  cachedGuilds = null;
  return [...await guildList(env, now)];
}

async function handleGuilds(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  return json({ guilds: await freshGuilds(env, now) }, 200, { 'cache-control': 'no-store' });
}

async function handleGuildAdd(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  const body = await readJson(request);
  if (!body) return no('bad request');
  const slug = guildSlug(body.name);
  if (!slug) return no('bad name');
  const tag = body.tag == null ? '' : String(body.tag).trim();
  if (tag && !GUILD_TAG.test(tag)) return no('bad tag');
  const list = await freshGuilds(env, now);
  if (list.some((guild) => guild.slug === slug)) return no('already', 409);
  if (list.length >= MAX_GUILDS) return no('too many', 409);
  list.push({ slug, name: String(body.name).trim().replace(/\s+/g, ' '), tag, t: now, by, i: 0, b: 0 });
  await saveGuilds(env, list, now);
  return json({ ok: true, slug });
}

async function handleGuildImage(request, env, now, slug, kind) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  if (Number(request.headers.get('content-length') || 0) > MAX_GUILD_IMAGE) return no('too large', 413);
  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length > MAX_GUILD_IMAGE) return no('too large', 413);
  const type = imageType(bytes);
  if (!type) return no('bad image');
  const list = await freshGuilds(env, now);
  const guild = list.find((one) => one.slug === slug);
  if (!guild) return no('no such guild', 404);
  await env.CORPUS.put('gimg:' + slug + ':' + kind, bytes, { metadata: { t: type } });
  guild[kind === 'icon' ? 'i' : 'b'] = now;
  await saveGuilds(env, list, now);
  return json({ ok: true, v: now });
}

async function handleGuildRemove(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  const slug = body && typeof body.slug === 'string' ? body.slug : '';
  if (!GUILD_SLUG.test(slug)) return no('bad request');
  const list = await freshGuilds(env, now);
  await saveGuilds(env, list.filter((guild) => guild.slug !== slug), now);
  await env.CORPUS.delete('gimg:' + slug + ':icon');
  await env.CORPUS.delete('gimg:' + slug + ':banner');
  return json({ ok: true });
}

/** 公會的圖示／旗幟。網址帶著版本（?v=），所以可以放心讓瀏覽器存久一點。 */
async function handleGuildPicture(env, slug, kind) {
  const found = await env.CORPUS.getWithMetadata('gimg:' + slug + ':' + kind, { type: 'arrayBuffer' });
  if (!found || !found.value || !found.metadata) return no('not found', 404);
  return new Response(found.value, {
    headers: {
      'content-type': found.metadata.t,
      'cache-control': 'public, max-age=86400',
      'x-content-type-options': 'nosniff',
      'content-security-policy': "default-src 'none'",
    },
  });
}

/**
 * credits.json 上有遊戲 ID 的人（小寫）：names 是所有成員（開發者、贊助者、貢獻者），
 * developers 是其中的開發者。抓不到就回上一次的；從來沒抓到過回 null。
 */
async function credits(now) {
  if (cachedCredits && now - cachedCredits.t < CREDITS_MAX_AGE_MS) return cachedCredits;
  try {
    const answer = await fetch(CREDITS_URL, { cf: { cacheTtl: 300 } });
    if (answer.status === 200) {
      const names = new Set();
      const developers = new Set();
      const sections = (await answer.json()).sections || [];
      for (const section of sections) {
        const dev = typeof section.key === 'string' && section.key.endsWith('.developers');
        for (const member of section.members || []) {
          if (typeof member.mc !== 'string' || !member.mc) continue;
          names.add(member.mc.toLowerCase());
          if (dev) developers.add(member.mc.toLowerCase());
        }
      }
      cachedCredits = { t: now, names, developers, team: teamOf(sections) };
    }
  } catch (e) {
    // 用舊的
  }
  return cachedCredits;
}

/**
 * 這個人能不能上傳這種檔。UPLOADS：
 *   tiered（預設）：驗過身分的人都能傳 PNG；GIF 只有名單上的成員（開發者、贊助者、貢獻者）
 *   listed：只有名單上的成員能傳
 *   anyone：驗過身分的人都能傳，PNG、GIF 都可以
 *   off：不收
 */
async function mayUpload(env, user, now, type) {
  const mode = env.UPLOADS || 'tiered';
  if (mode === 'off') return 'uploads off';
  if (await env.CORPUS.get('ban:' + user.uuid)) return 'banned';
  if (mode === 'anyone' || (mode === 'tiered' && type !== 'gif')) return null;
  if (await hasPerk(env, user.uuid)) return null;
  const list = await credits(now);
  if (!list) return 'try later';
  if (list.names.has(user.name.toLowerCase())) return null;
  return mode === 'tiered' ? 'gif members only' : 'not listed';
}

/**
 * 特別名單（使用者 2026-10-13：「新增類似贊助者但不顯示的，我們自己輸入 minecraft uuid
 * 讓他們可以使用 gif 和 20mb」）。
 *
 * 開發者在管理頁加的 UUID。上面的人上傳的待遇跟名單成員一樣（GIF、20 MB），
 * 但不在 credits.json 裡——沒有葉子、不在貢獻者名單上、公開的 /api/list 也看不出來。
 * 存在 KV 的 perk:<uuid>，內容是 {n 備註, t 什麼時候加的, by 誰加的}。
 */
async function hasPerk(env, uuid) {
  return Boolean(await env.CORPUS.get('perk:' + uuid));
}

/** 上傳的待遇算不算成員：credits.json 上有名字，或在特別名單上。 */
async function isMember(env, user, now) {
  if (await hasPerk(env, user.uuid)) return true;
  const list = await credits(now);
  return Boolean(list && list.names.has(user.name.toLowerCase()));
}

/** 這個人的披風最大可以多大。 */
async function capeLimit(env, user, now) {
  return await isMember(env, user, now) ? MAX_CAPE_BYTES : MAX_PLAIN_CAPE_BYTES;
}

/** UUID 寫成 32 個十六進位字元（去掉連字號、轉小寫）；不是 UUID 回 null。 */
export function plainUuid(text) {
  const plain = String(text == null ? '' : text).trim().toLowerCase().replace(/-/g, '');
  return UUID.test(plain) ? plain : null;
}

/** 這個人是不是開發者（可以拿掉別人的披風）。 */
async function isDeveloper(name, now) {
  const list = await credits(now);
  return Boolean(list && list.developers.has(name.toLowerCase()));
}

// ---------------------------------------------------------------- 管理員
//
// 使用者 2026-10-13：「新增管理員（需要 discord 或 minecraft id 方便綁定），可以移除披風和
// 網站開發者的那些功能，區別是不展示而已（只是不能移除管理員、新增）」。
//
// 管理員做得到開發者在網站上做的事（刪披風、封禁與解除、特別名單、替名單開 PR），但不在
// credits.json 裡——沒有葉子、不在團隊名單上——也不能動管理員名單本身。由開發者在管理頁加。
//
// KV：staff:u:<UUID>、staff:n:<遊戲名字，小寫>、staff:d:<Discord 代號> → {n 備註, t, by, m 遊戲名字}。
// 用名字加的那一種只是「還不知道 UUID」：那個人登入一次就改記成 UUID（名字可以改，改掉的
// 名字會被別人拿走，UUID 不會）。

const DISCORD_SNOWFLAKE = /^\d{17,20}$/;

/**
 * 開發者填的那一格是哪一種：'u:<UUID>'、'd:<Discord 代號>'、'n:<名字>'；看不懂回 null。
 * 三種不會混：UUID 是 32 個十六進位字元、Discord 代號是 17 到 20 位數字，遊戲名字最多 16 個字。
 */
export function staffKey(text) {
  const raw = String(text == null ? '' : text).trim();
  const uuid = plainUuid(raw);
  if (uuid) return 'u:' + uuid;
  if (DISCORD_SNOWFLAKE.test(raw)) return 'd:' + raw;
  return NAME.test(raw) ? 'n:' + raw.toLowerCase() : null;
}

function parseRecord(raw) {
  try {
    const value = JSON.parse(raw || '{}');
    return value && typeof value === 'object' ? value : {};
  } catch (e) {
    return {};
  }
}

/** 這個玩家是不是管理員：照 UUID、照他綁的 Discord、照名字（第一次對上就改記成 UUID）。 */
async function isStaff(env, user) {
  if (await env.CORPUS.get('staff:u:' + user.uuid)) return true;
  const discord = user.discord || await env.CORPUS.get('l:' + user.uuid);
  if (discord && await env.CORPUS.get('staff:d:' + discord)) return true;
  const byName = await env.CORPUS.get('staff:n:' + user.name.toLowerCase());
  if (!byName) return false;
  await env.CORPUS.put('staff:u:' + user.uuid, JSON.stringify({ ...parseRecord(byName), m: user.name }));
  await env.CORPUS.delete('staff:n:' + user.name.toLowerCase());
  return true;
}

/**
 * 這個請求在網站上的權限：{by 是誰（寫進紀錄用）, developer 是不是開發者}；沒有權限回 null。
 * 開發者＝本人在名單上的開發者那一區（每次都重新查名單），或帶著 ADMIN_TOKEN。
 * 管理員＝在 staff:* 裡；只用 Discord 登入、還沒綁遊戲 ID 的人也可以是（照 Discord 代號加的）。
 */
async function authority(request, env, now) {
  const header = request.headers.get('authorization') || '';
  if (env.ADMIN_TOKEN && header === 'Bearer ' + env.ADMIN_TOKEN) return { by: 'admin', developer: true };
  const user = await who(request, env, now);
  if (user) {
    if (await isDeveloper(user.name, now)) return { by: user.name, developer: true };
    return await isStaff(env, user) ? { by: user.name, developer: false } : null;
  }
  const session = await discordSession(request, env, now);
  // 照遊戲帳號加的管理員，用綁著那個遊戲帳號的 Discord 登入網站也算（管理頁的搜尋兩邊都有的人記 UUID）
  if (session && (await env.CORPUS.get('staff:d:' + session.id)
      || (session.account.u && await env.CORPUS.get('staff:u:' + session.account.u)))) {
    return { by: 'Discord: ' + (session.account.n || session.id), developer: false };
  }
  return null;
}

/** 管理員名單。只有開發者看得到。 */
async function handleStaff(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  const staff = [];
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'staff:', limit: 1000, cursor });
    for (const k of page.keys) {
      const record = parseRecord(await env.CORPUS.get(k.name));
      const key = k.name.slice(6);
      staff.push({ key, kind: key[0], id: key.slice(2), name: record.m || null, discord: record.dn || null,
        note: record.n || '', t: record.t || null, by: record.by || null });
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  staff.sort((a, b) => (b.t || 0) - (a.t || 0));
  return json({ staff }, 200, { 'cache-control': 'no-store' });
}

async function handleStaffAdd(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  const body = await readJson(request);
  if (!body) return no('bad request');
  const key = staffKey(body.who);
  if (!key) return no('bad who');
  const note = body.note == null ? '' : body.note;
  if (typeof note !== 'string' || note.trim().length > 60) return no('bad note');
  if (await env.CORPUS.get('staff:' + key)) return no('already', 409);
  const record = { n: note.trim(), t: now, by: mine.by };
  if (key.startsWith('n:')) record.m = String(body.who).trim();      // 名字照他打的大小寫記著，名單上好認
  // 照 UUID 或 Discord 加的：網站帳號認得這個人的話，把名字一起記著，名單上不必只看一串代號
  const linked = key.startsWith('d:') ? key.slice(2)
    : key.startsWith('u:') ? await env.CORPUS.get('l:' + key.slice(2)) : null;
  const known = linked ? await account(env, linked) : null;
  if (known) {
    if (known.n) record.dn = known.n;
    if (known.m && (key.startsWith('d:') || known.u === key.slice(2))) record.m = known.m;
  }
  if (key.startsWith('u:') && !record.m) {
    const cape = await env.CORPUS.getWithMetadata('cape:' + await publicId(key.slice(2)));
    if (cape && cape.metadata && NAME.test(cape.metadata.n || '')) record.m = cape.metadata.n;
  }
  await env.CORPUS.put('staff:' + key, JSON.stringify(record));
  return json({ ok: true, key });
}

/** 搜尋字串跟這個人對得上的程度：0 對不上、1 包含、2 開頭相同、3 完全相同。 */
export function personScore(person, q) {
  let best = 0;
  const bare = q.replace(/-/g, '');
  for (const [field, text] of [['dname', q], ['mc', q], ['discord', q], ['uuid', bare]]) {
    const value = String(person[field] || '').toLowerCase();
    if (!value || !text) continue;
    const score = value === text ? 3 : value.startsWith(text) ? 2 : value.includes(text) ? 1 : 0;
    if (score > best) best = score;
  }
  return best;
}

export const PEOPLE_MAX = 1000;

/**
 * 管理頁「新增管理員」的下拉選單：網站帳號（用 Discord 登入過的，綁了遊戲帳號就連名字與 UUID）
 * 與傳過披風的人。只有開發者。回 {people: [{discord, dname, mc, uuid}]}：沒帶 q 是全部、照名字排；
 * 帶了 q 只回對得上的，對得越準越前面。網頁展開選單時抓一次全部，篩選在瀏覽器裡做。
 */
async function handlePeople(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  const q = (new URL(request.url).searchParams.get('q') || '').trim().toLowerCase();
  if (q.length > 40) return json({ people: [] }, 200, { 'cache-control': 'no-store' });
  const byUuid = new Map();
  const people = [];
  let cursor;
  let reads = 0;
  do {
    const page = await env.CORPUS.list({ prefix: 'd:', limit: 1000, cursor });
    for (const k of page.keys) {
      const id = k.name.slice(2);
      if (!DISCORD_ID.test(id)) continue;
      let m = k.metadata;
      // 這次改版之前存的帳號沒有 metadata：讀一次本體（下次登入就會補上）
      if (!m && reads < 400) {
        reads++;
        m = await account(env, id);
      }
      if (!m) continue;
      const person = { discord: id, dname: m.n || null, mc: m.m || null, uuid: UUID.test(m.u || '') ? m.u : null };
      people.push(person);
      if (person.uuid) byUuid.set(person.uuid, person);
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  for (const cape of await allCapes(env)) {
    if (!UUID.test(cape.u || '') || byUuid.has(cape.u)) continue;
    const person = { discord: null, dname: null, mc: NAME.test(cape.n || '') ? cape.n : null, uuid: cape.u };
    byUuid.set(cape.u, person);
    people.push(person);
  }
  const label = (p) => String(p.mc || p.dname || p.discord).toLowerCase();
  const hits = people.map((p) => [q ? personScore(p, q) : 1, p]).filter(([score]) => score > 0)
    .sort((a, b) => b[0] - a[0] || label(a[1]).localeCompare(label(b[1])))
    .slice(0, PEOPLE_MAX).map(([, p]) => p);
  return json({ people: hits }, 200, { 'cache-control': 'no-store' });
}

async function handleStaffRemove(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  const body = await readJson(request);
  const key = body && typeof body.key === 'string' ? body.key : '';
  if (!/^(?:u:[0-9a-f]{32}|d:\d{17,20}|n:[a-z0-9_]{1,16})$/.test(key)) return no('bad request');
  await env.CORPUS.delete('staff:' + key);
  return json({ ok: true });
}

// 上傳冷卻（使用者 2026-10-14：「1 小時內若上傳 3 個披風以上，就需要等待 12 小時才能再傳一個」）。
// KV upl:<UUID> → {t [最近一小時內每次上傳的時間], until 鎖到什麼時候}。12 小時多一點就自己過期。
export const BURST_COUNT = 3;
export const BURST_WINDOW_MS = 3600 * 1000;
export const BURST_LOCK_MS = 12 * 3600 * 1000;

/** 還在冷卻就回鎖到什麼時候，不然回 0。 */
export function burstLock(log, now) {
  return log && log.until > now ? log.until : 0;
}

/** 記一次上傳：一小時內湊滿 BURST_COUNT 次就鎖 12 小時。 */
export function burstNote(log, now) {
  const recent = (log && Array.isArray(log.t) ? log.t : []).filter((t) => now - t < BURST_WINDOW_MS);
  recent.push(now);
  const next = { t: recent };
  if (recent.length >= BURST_COUNT) next.until = now + BURST_LOCK_MS;
  return next;
}

async function handleUpload(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  if (Number(request.headers.get('content-length') || 0) > MAX_CAPE_BYTES) {
    return no('too large', 413);
  }
  const bytes = new Uint8Array(await request.arrayBuffer());
  const found = inspectCape(bytes);
  if (found.why) return no(found.why, found.why === 'too large' ? 413 : 400);
  const refusal = await mayUpload(env, user, now, found.type);
  if (refusal) return no(refusal, 403);
  const log = parseRecord(await env.CORPUS.get('upl:' + user.uuid));
  const until = burstLock(log, now);
  if (until) return json({ ok: false, why: 'cooldown', until }, 429);
  // 一般玩家 1 MB，名單上的成員 20 MB。名單讀不到的時候當成一般玩家。
  const limit = await capeLimit(env, user, now);
  if (bytes.length > limit) return json({ ok: false, why: 'too large', limit }, 413);
  const id = await publicId(user.uuid);
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (have && have.metadata && now - have.metadata.v < UPLOAD_EVERY_MS) {
    return no('slow down', 429);
  }
  // 審核名單上的人只能有一件自己傳的披風（等審核的也算；使用者 2026-10-14）。
  // 從一覽穿別人的那一件不算，但收著的自己那件（p）算。要換就先移除。
  if (have && have.metadata && (!have.metadata.w || have.metadata.p) && await reviewOf(env, user.uuid)) {
    return no('review one', 403);
  }
  // 圖檔照內容存（blob:<雜湊>），披風那一筆只記它指到哪一張——
  // 好幾個人傳同一張圖，只存一份。
  const hash = hex(await crypto.subtle.digest('SHA-256', bytes));
  const blob = await env.CORPUS.getWithMetadata('blob:' + hash, { type: 'stream' });
  if (blob && blob.value && blob.value.cancel) blob.value.cancel();
  if (!blob || !blob.metadata) {
    await env.CORPUS.put('blob:' + hash, bytes, { metadata: { t: found.type, s: bytes.length } });
  }
  const metadata = { t: found.type, v: now, c: now, s: bytes.length, u: user.uuid, n: user.name, h: hash,
    x: found.width, y: found.height };
  // 要不要出現在一覽：這一次有說（?public=0／1）就照這一次，沒說（從遊戲裡傳的）照原本的
  const asked = new URL(request.url).searchParams.get('public');
  if (asked === '0' || (asked !== '1' && have && have.metadata && have.metadata.q)) metadata.q = 1;
  // 在審核名單上的人：這張圖沒通過過，就先不給別人看
  const pending = await needsReview(env, user.uuid, hash);
  if (pending) metadata.r = 1;
  await env.CORPUS.put('cape:' + id, '', { metadata });
  await env.CORPUS.put('upl:' + user.uuid, JSON.stringify(burstNote(log, now)),
    { expirationTtl: Math.ceil((BURST_LOCK_MS + BURST_WINDOW_MS) / 1000) });
  await noteHistory(env, id, metadata, now);
  await revive(env, hash);
  if (have && have.metadata && have.metadata.h !== hash) await sweep(env, have.metadata.h, have.metadata, now);
  // 自己傳了新的：先前從一覽套用時收著的那一件就不必留了
  if (have && have.metadata && have.metadata.p && have.metadata.p.h !== hash) {
    await sweep(env, have.metadata.p.h, { ...have.metadata.p, q: have.metadata.q }, now);
  }
  const mine = await env.CORPUS.getWithMetadata('u:' + user.uuid);
  await remember(env, user.uuid, !(mine && mine.metadata && mine.metadata.c === 0), now);
  return json({ ok: true, id, v: now, ...found, pending });
}

// ---------------------------------------------------------------- 一覽：公開與否、審核、使用紀錄、最愛
//
// 使用者 2026-10-13：「披風麻煩多幾個篩選（使用人數、新增時間、人氣、上傳時間；新增愛心或星星，
// 類似我的最愛）和使用紀錄；使用者可以開關是否要公開披風（管理員可以看到所有披風，包含不公開）；
// 還有審核制度（輕度懲處）：管理員可以新增使用者進去，他之後上傳的披風都要審核」。
//
// 披風那一筆（cape:<代碼>）的 metadata 多了：
//   c  這張圖最早是什麼時候傳的（穿別人的那一件就抄它的）——排序用
//   q  1＝不公開：不出現在一覽上、別人不能穿；遊戲裡別的玩家照樣看得到
//   r  1＝等審核：一覽與遊戲裡都不給別人看，通過了才算數
//
// 其他的鍵：
//   hist:<代碼>:<雜湊>  我用過的披風（metadata：{t, s, x, y, at}），一個人最多留 HISTORY_MAX 件。
//                      只是一筆紀錄，不會把圖留住：那張圖沒有人穿滿七天就刪，紀錄跟著清掉
//   idle                沒有人穿的披風與它從什麼時候開始沒人穿（見「沒有人穿的披風：留七天」）
//   fav:<UUID>          我的最愛：{t 上一次改的時間, h [雜湊…]}
//   stars               每張圖有幾個人加了最愛：{雜湊: 人數}。放同一筆，一覽讀一次就夠
//   review:<UUID>       審核名單：{n 名字, note, t, by, ok [通過過的雜湊…]}

const HASH = /^[0-9a-f]{64}$/;

/** 一個人的使用紀錄最多留幾件。 */
const HISTORY_MAX = 8;

/** 我的最愛最多幾件。 */
const FAVS_MAX = 200;

/** 一覽上看得到的：公開，而且不是等審核的。 */
const listed = (cape) => !cape.q && !cape.r;

/** 所有人的使用紀錄（只有鍵：誰、哪張圖）。sweep 與管理的人照圖刪除的時候用。 */
async function allHistory(env, prefix = 'hist:') {
  const out = [];
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix, limit: 1000, cursor });
    for (const k of page.keys) {
      const parts = k.name.split(':');
      if (parts.length === 3 && HASH.test(parts[2])) out.push({ key: k.name, id: parts[1], h: parts[2], ...(k.metadata || {}) });
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  return out;
}

/** 記一筆「我用過這一件」；超過上限就把最舊的拿掉。 */
async function noteHistory(env, id, cape, now) {
  await env.CORPUS.put('hist:' + id + ':' + cape.h, '', {
    metadata: { t: cape.t, s: cape.s, x: cape.x || 0, y: cape.y || 0, at: now },
  });
  const mine = (await allHistory(env, 'hist:' + id + ':')).sort((a, b) => (b.at || 0) - (a.at || 0));
  for (const old of mine.slice(HISTORY_MAX)) await env.CORPUS.delete(old.key);
}

async function reviewOf(env, uuid) {
  const raw = await env.CORPUS.get('review:' + uuid);
  return raw ? parseRecord(raw) : null;
}

/** 這個人穿這張圖要不要先審核：他在審核名單上，而且這張圖沒通過過。 */
async function needsReview(env, uuid, hash) {
  const record = await reviewOf(env, uuid);
  return Boolean(record && !(Array.isArray(record.ok) && record.ok.includes(hash)));
}

async function favsOf(env, uuid) {
  const record = parseRecord(await env.CORPUS.get('fav:' + uuid));
  return { t: record.t || 0, h: Array.isArray(record.h) ? record.h.filter((h) => HASH.test(h)) : [] };
}

async function starMap(env) {
  const map = parseRecord(await env.CORPUS.get('stars'));
  return map && typeof map === 'object' ? map : {};
}

/** 加進／移出我的最愛。 */
async function handleStar(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const body = await readJson(request);
  const hash = body && typeof body.hash === 'string' ? body.hash : '';
  if (!HASH.test(hash) || typeof body.on !== 'boolean') return no('bad request');
  const capes = await allCapes(env);
  const idle = await idleMap(env);
  // 只能把一覽上看得到的加進來（沒有人穿、還在倒數的也算）；移出去的話什麼時候都可以（那一件可能已經不在了）
  if (body.on && !capes.some((cape) => cape.h === hash && listed(cape)) && !(idle[hash] && !idle[hash].q)) {
    return no('no such cape', 404);
  }
  const favs = await favsOf(env, user.uuid);
  if (now - favs.t < 1000) return no('slow down', 429);
  const has = favs.h.includes(hash);
  const map = await starMap(env);
  if (body.on !== has) {
    if (body.on && favs.h.length >= FAVS_MAX) return no('too many', 409);
    const next = body.on ? [hash, ...favs.h] : favs.h.filter((h) => h !== hash);
    await env.CORPUS.put('fav:' + user.uuid, JSON.stringify({ t: now, h: next }));
    map[hash] = Math.max(0, (Number(map[hash]) || 0) + (body.on ? 1 : -1));
    // 順手把已經不在的圖從人數表拿掉
    const alive = new Set([...capes.map((cape) => cape.h), ...Object.keys(idle)]);
    for (const key of Object.keys(map)) {
      if (!alive.has(key) || !map[key]) delete map[key];
    }
    await env.CORPUS.put('stars', JSON.stringify(map));
  }
  return json({ ok: true, on: body.on, stars: Number(map[hash]) || 0 });
}

/**
 * 我的最愛與我用過的披風。
 *
 * <p>順手清掉已經不在的：沒有人穿滿七天的圖會被刪掉，紀錄與最愛裡指到它的那幾筆留著也沒用。
 * removed 是這一次清了幾件（兩邊都有的算一件），網頁拿它告訴使用者「有幾件披風已經移除」。
 */
async function handleMine(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const id = await publicId(user.uuid);
  const alive = new Set(Object.keys(await purge(env, now)));   // 還在倒數的也算還在
  for (const cape of await allCapes(env)) {
    alive.add(cape.h);
    if (cape.p && cape.id === id) alive.add(cape.p.h);        // 自己收著準備復原的那一件
  }
  const gone = new Set();
  const history = [];
  for (const one of (await allHistory(env, 'hist:' + id + ':')).sort((a, b) => (b.at || 0) - (a.at || 0))) {
    if (!alive.has(one.h)) {
      gone.add(one.h);
      await env.CORPUS.delete(one.key);
      continue;
    }
    history.push({ h: one.h, t: one.t, s: one.s, x: one.x || 0, y: one.y || 0, at: one.at || 0 });
  }
  const favs = await favsOf(env, user.uuid);
  const kept = favs.h.filter((hash) => alive.has(hash));
  if (kept.length !== favs.h.length) {
    favs.h.forEach((hash) => { if (!alive.has(hash)) gone.add(hash); });
    await env.CORPUS.put('fav:' + user.uuid, JSON.stringify({ t: favs.t, h: kept }));
  }
  return json({ ok: true, id, favs: kept, history, removed: gone.size }, 200, { 'cache-control': 'no-store' });
}

/** 把一件從我的使用紀錄拿掉（那張圖沒別人用就真的刪了）。現在穿著的那一件不能拿。 */
async function handleForget(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const body = await readJson(request);
  const hash = body && typeof body.hash === 'string' ? body.hash : '';
  if (!HASH.test(hash)) return no('bad request');
  const id = await publicId(user.uuid);
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (have && have.metadata && (have.metadata.h === hash || (have.metadata.p && have.metadata.p.h === hash))) {
    return no('in use', 409);
  }
  await env.CORPUS.delete('hist:' + id + ':' + hash);
  return json({ ok: true });
}

/** 我的披風要不要出現在一覽上。 */
async function handlePublic(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const body = await readJson(request);
  if (!body || typeof body.on !== 'boolean') return no('bad request');
  const id = await publicId(user.uuid);
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (!have || !have.metadata) return no('no cape', 404);
  const { q: before, ...rest } = have.metadata;
  if (!body.on) rest.q = 1;
  await env.CORPUS.put('cape:' + id, '', { metadata: rest });
  return json({ ok: true, public: body.on });
}

/** 審核名單。 */
async function handleReviews(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const reviews = [];
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'review:', limit: 1000, cursor });
    for (const k of page.keys) {
      const record = parseRecord(await env.CORPUS.get(k.name));
      reviews.push({ uuid: k.name.slice(7), name: record.n || null, note: record.note || '',
        t: record.t || null, by: record.by || null });
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  reviews.sort((a, b) => (b.t || 0) - (a.t || 0));
  return json({ reviews }, 200, { 'cache-control': 'no-store' });
}

/**
 * 列入審核：{uuid} 是一個人；{hash} 是傳那張圖的人（穿別人那一件的不算——圖不是他傳的）。
 * 現在身上那一件不動：審核管的是「之後」傳的。
 */
async function handleReviewAdd(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  const body = await readJson(request);
  if (!body) return no('bad request');
  const note = body.note == null ? '' : body.note;
  if (typeof note !== 'string' || note.trim().length > 60) return no('bad note');
  const people = [];
  if (typeof body.hash === 'string' && HASH.test(body.hash)) {
    for (const cape of await allCapes(env)) {
      if (cape.h === body.hash && !cape.w && UUID.test(cape.u || '')) people.push({ uuid: cape.u, name: cape.n || null });
    }
  } else {
    const uuid = plainUuid(body.uuid);
    if (!uuid) return no('bad uuid');
    people.push({ uuid, name: null });
  }
  let added = 0;
  for (const person of people) {
    if (await env.CORPUS.get('review:' + person.uuid)) continue;
    await env.CORPUS.put('review:' + person.uuid, JSON.stringify({ n: person.name, note: note.trim(), t: now, by, ok: [] }));
    added++;
  }
  if (!added && people.length === 1 && !body.hash) return no('already', 409);
  return json({ ok: true, added });
}

/** 把一個人的披風那一筆（cape:<代碼>）的「等審核」拿掉，名單也跟著更新。 */
async function release(env, id, uuid, now) {
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (!have || !have.metadata || !have.metadata.r) return false;
  const { r: gone, ...rest } = have.metadata;
  await env.CORPUS.put('cape:' + id, '', { metadata: rest });
  const shown = await env.CORPUS.getWithMetadata('u:' + uuid);
  if (shown && shown.metadata) await remember(env, uuid, shown.metadata.c !== 0, now);
  return true;
}

async function handleReviewRemove(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  const uuid = body ? plainUuid(body.uuid) : null;
  if (!uuid) return no('bad request');
  await env.CORPUS.delete('review:' + uuid);
  await release(env, await publicId(uuid), uuid, now);      // 不必審核了：等著的那一件直接放行
  return json({ ok: true });
}

/** 通過審核：那張圖等審核的都放行，並記在那些人的「通過過」裡（之後再穿同一張不必再審）。 */
async function handleApprove(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  const hash = body && typeof body.hash === 'string' ? body.hash : '';
  if (!HASH.test(hash)) return no('bad request');
  let approved = 0;
  for (const cape of await allCapes(env)) {
    if (cape.h !== hash || !cape.r || !UUID.test(cape.u || '')) continue;
    const record = await reviewOf(env, cape.u);
    if (record) {
      const passed = Array.isArray(record.ok) ? record.ok.filter((h) => h !== hash) : [];
      record.ok = [hash, ...passed].slice(0, 30);
      await env.CORPUS.put('review:' + cape.u, JSON.stringify(record));
    }
    if (await release(env, cape.id, cape.u, now)) approved++;
  }
  return json({ ok: true, approved });
}

// ---------------------------------------------------------------- 套用一覽上的披風
//
// 使用者 2026-10-13：「希望點下去可以讓使用者自由選擇要不要套用該披風（以及復原）」。
//
// 套用＝自己那一筆改成指到同一張圖（圖本來就是照內容存的，不必再傳一次）。上傳的規矩
// 照舊：GIF 只有成員能穿、檔案不能超過自己的上限、被封禁的人不行——不然套用就成了繞過
// 那些規矩的路。
//
// 復原：套用的時候把原本「自己傳的那一件」收在 metadata 的 p（{t, s, h, x, y}），記號 w
// 表示現在這件是套用來的。一路換好幾件，收著的始終是自己傳的那一件；復原就換回它，
// 原本沒有披風就脫下。收著的圖還算「有人在用」，sweep 不會刪掉。

/** 套用之後隔多久才能再換一件。 */
const WEAR_EVERY_MS = 5 * 1000;

async function handleWear(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const body = await readJson(request);
  const hash = body && typeof body.hash === 'string' ? body.hash : '';
  if (!/^[0-9a-f]{64}$/.test(hash)) return no('bad request');
  // 只能套用一覽上看得到的（公開、通過審核的），或自己用過的；別人收著準備復原的那一張不算
  const id = await publicId(user.uuid);
  let onShow = (await allCapes(env)).find((cape) => cape.h === hash && listed(cape));
  if (!onShow) {
    // 沒有人穿、還在倒數的那幾件也在一覽上
    const waiting = (await idleMap(env))[hash];
    if (waiting && !waiting.q) onShow = { ...waiting, h: hash };
  }
  const past = onShow ? null : await env.CORPUS.getWithMetadata('hist:' + id + ':' + hash);
  let source = onShow || (past && past.metadata ? { ...past.metadata, h: hash } : null);
  if (source && !onShow) {
    // 從自己的紀錄穿回去：那張圖要還在（沒有人穿滿七天的圖會被刪掉）
    const blob = await env.CORPUS.getWithMetadata('blob:' + hash, { type: 'stream' });
    if (blob && blob.value && blob.value.cancel) blob.value.cancel();
    if (!blob || !blob.metadata) source = null;
  }
  if (!source) return no('no such cape', 404);
  const refusal = await mayUpload(env, user, now, source.t);
  if (refusal) return no(refusal, 403);
  const limit = await capeLimit(env, user, now);
  if ((source.s || 0) > limit) return json({ ok: false, why: 'too large', limit }, 413);
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  const mine = have && have.metadata ? have.metadata : null;
  if (mine && mine.h === hash) return json({ ok: true, id, v: mine.v, same: true });
  if (mine && now - mine.v < WEAR_EVERY_MS) return no('slow down', 429);
  const keep = !mine ? null : mine.w ? mine.p || null
    : { t: mine.t, s: mine.s, h: mine.h, x: mine.x || 0, y: mine.y || 0, c: mine.c || mine.v };
  const metadata = { t: source.t, v: now, c: source.c || source.v || source.at || now, s: source.s,
    u: user.uuid, n: user.name, h: hash, w: 1 };
  if (source.x && source.y) {
    metadata.x = source.x;
    metadata.y = source.y;
  }
  if (keep) metadata.p = keep;
  if (mine && mine.q) metadata.q = 1;                        // 公開與否照他原本的
  // 一覽上看得到的本來就給大家看了；從自己的紀錄穿回去的，在審核名單上的人照樣要審
  const pending = !onShow && await needsReview(env, user.uuid, hash);
  if (pending) metadata.r = 1;
  await env.CORPUS.put('cape:' + id, '', { metadata });
  await noteHistory(env, id, metadata, now);
  await revive(env, hash);
  if (mine) await sweep(env, mine.h, mine, now);
  const shown = await env.CORPUS.getWithMetadata('u:' + user.uuid);
  await remember(env, user.uuid, !(shown && shown.metadata && shown.metadata.c === 0), now);
  return json({ ok: true, id, v: now, t: source.t, back: Boolean(keep), pending });
}

async function handleRestore(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  if (await env.CORPUS.get('ban:' + user.uuid)) return no('banned', 403);
  const id = await publicId(user.uuid);
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  const mine = have && have.metadata ? have.metadata : null;
  if (!mine || !mine.w) return no('nothing to restore', 409);
  const kept = mine.p && typeof mine.p.h === 'string' ? mine.p : null;
  // 收著的那張圖要還在（管理的人可能把它刪了）；不在就當成原本沒有披風
  const blob = kept ? await env.CORPUS.getWithMetadata('blob:' + kept.h, { type: 'stream' }) : null;
  if (blob && blob.value && blob.value.cancel) blob.value.cancel();
  if (!kept || !blob || !blob.metadata) {
    await dropCape(env, id, now);
    return json({ ok: true, cape: false });
  }
  const metadata = { t: kept.t, v: now, c: kept.c || now, s: kept.s, u: user.uuid, n: user.name, h: kept.h };
  if (kept.x && kept.y) {
    metadata.x = kept.x;
    metadata.y = kept.y;
  }
  if (mine.q) metadata.q = 1;
  if (await needsReview(env, user.uuid, kept.h)) metadata.r = 1;
  await env.CORPUS.put('cape:' + id, '', { metadata });
  await noteHistory(env, id, metadata, now);
  await sweep(env, mine.h, mine, now);
  const shown = await env.CORPUS.getWithMetadata('u:' + user.uuid);
  await remember(env, user.uuid, !(shown && shown.metadata && shown.metadata.c === 0), now);
  return json({ ok: true, cape: true, v: now });
}

async function handleRemove(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const id = await publicId(user.uuid);
  await dropCape(env, id, now);
  return json({ ok: true });
}

/** 所有上傳的披風（披風那一筆的 metadata）。 */
async function allCapes(env) {
  const out = [];
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'cape:', limit: 1000, cursor });
    for (const k of page.keys) {
      if (k.metadata && typeof k.metadata.h === 'string') {
        out.push({ id: k.name.slice(5), ...k.metadata });
      }
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  return out;
}

// ---------------------------------------------------------------- 沒有人穿的披風：留七天
//
// 使用者 2026-10-13：「披風部分如果沒人使用保留 7 天，7 天後沒人使用就移除，防止占用伺服器空間」
// 「包含加星號（的也一樣），可以告知使用者披風已移除」。
//
// 最後一個人換掉、脫下之後，那張圖記進閒置清單（idle 這一筆：{雜湊: {t, s, x, y, c, at, q}}），
// 圖本身先留著。這七天裡它照樣在披風一覽上（標著幾天後移除），誰都可以再穿——一穿上就從
// 清單拿掉、不再倒數。期滿沒有人穿就刪圖。
//
//   - 有人收著它準備復原（p）算在用，不進清單——一個人只收一件。
//   - 使用紀錄與最愛<b>不算</b>在用：加了星號的一樣期滿就刪。紀錄與最愛裡指到的圖不見了，
//     /api/capes/mine 會順手清掉並回報清了幾件，網頁告訴他「有幾件披風已經移除」。
//   - 最後一個穿的人設了不公開、或那一件還在等審核：清單上記 q，一覽上不出現，
//     只有本人從自己的紀錄穿得回去。
//   - 管理的人移除的不留，馬上刪（discard）。
//
// 清單放同一筆，讀一次就夠（不必 list）。兩個人同時動到它，後寫的會蓋掉先寫的那一筆——
// 所以每天的整理（tidy，見 scheduled）會把「沒人在用、清單上也沒有」的圖補記回去，
// 不會有圖永遠留在那裡。

/** 沒有人穿的披風留多久才刪。 */
const IDLE_KEEP_MS = 7 * 24 * 3600 * 1000;

async function idleMap(env) {
  return parseRecord(await env.CORPUS.get('idle'));
}

const inUse = (capes, hash) => capes.some((cape) => cape.h === hash || (cape.p && cape.p.h === hash));

/**
 * 這張圖沒有人在用了：記進閒置清單，開始倒數。已經在清單上的不重算。
 *
 * @param from 原本指到它的那一筆（要它的格式、尺寸、最早上傳的時間，以及公開與否）
 */
async function sweep(env, hash, from, now) {
  if (!hash) return;
  if (inUse(await allCapes(env), hash)) return;
  const idle = await idleMap(env);
  if (idle[hash]) return;
  const blob = await env.CORPUS.getWithMetadata('blob:' + hash, { type: 'stream' });
  if (blob && blob.value && blob.value.cancel) blob.value.cancel();
  if (!blob || !blob.metadata) return;
  const entry = { t: (from && from.t) || blob.metadata.t, s: (from && from.s) || blob.metadata.s, at: now };
  if (from && from.x && from.y) {
    entry.x = from.x;
    entry.y = from.y;
  }
  if (from && (from.c || from.v)) entry.c = from.c || from.v;
  if (!from || from.q || from.r) entry.q = 1;
  idle[hash] = entry;
  await env.CORPUS.put('idle', JSON.stringify(idle));
}

/** 有人穿上了：不再倒數。 */
async function revive(env, hash) {
  const idle = await idleMap(env);
  if (!idle[hash]) return;
  delete idle[hash];
  await env.CORPUS.put('idle', JSON.stringify(idle));
}

/** 馬上刪掉這張圖（管理的人移除的）。還有人在用就不動。 */
async function discard(env, hash) {
  if (!hash || inUse(await allCapes(env), hash)) return;
  await env.CORPUS.delete('blob:' + hash);
  await revive(env, hash);
}

/** 期滿的刪掉。回傳整理過的閒置清單。 */
async function purge(env, now) {
  const idle = await idleMap(env);
  const due = Object.keys(idle).filter((hash) => now - (Number(idle[hash] && idle[hash].at) || 0) >= IDLE_KEEP_MS);
  if (!due.length) return idle;
  const capes = await allCapes(env);
  const stars = await starMap(env);
  let starred = false;
  for (const hash of due) {
    delete idle[hash];
    if (inUse(capes, hash)) continue;                       // 清單上漏拿掉的：有人在穿就不刪
    await env.CORPUS.delete('blob:' + hash);
    if (hash in stars) {
      delete stars[hash];
      starred = true;
    }
  }
  await env.CORPUS.put('idle', JSON.stringify(idle));
  if (starred) await env.CORPUS.put('stars', JSON.stringify(stars));
  return idle;
}

/**
 * 每天一次的整理：期滿的刪掉；沒有人在用、清單上也沒有的圖補記進去（從這一刻起算七天）。
 *
 * <p>後面那一種是漏網的：改成「留七天」之前只被使用紀錄留住的圖，或兩個人同時動到清單時
 * 被蓋掉的那一筆。補記的一律不公開——不知道它原本是不是公開的。
 */
export async function tidy(env, now) {
  const idle = await purge(env, now);
  const capes = await allCapes(env);
  let added = 0;
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'blob:', limit: 1000, cursor });
    for (const k of page.keys) {
      const hash = k.name.slice(5);
      if (!HASH.test(hash) || idle[hash] || inUse(capes, hash)) continue;
      idle[hash] = { t: (k.metadata && k.metadata.t) || 'png', s: (k.metadata && k.metadata.s) || 0, at: now, q: 1 };
      added++;
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  if (added) await env.CORPUS.put('idle', JSON.stringify(idle));
  return { idle: Object.keys(idle).length, added };
}

/** 閒置清單上某一張圖（披風一覽上「沒有人在穿」的那幾張卡用）。 */
async function handleIdle(env, hash) {
  if (!HASH.test(hash)) return no('not found', 404);
  const one = (await idleMap(env))[hash];
  if (!one) return no('not found', 404);
  const have = await env.CORPUS.getWithMetadata('blob:' + hash, { type: 'arrayBuffer' });
  if (!have || !have.value) return no('not found', 404);
  return new Response(have.value, {
    status: 200,
    headers: {
      'content-type': one.t === 'gif' ? 'image/gif' : 'image/png',
      // 這張圖幾天後就不在了，別讓瀏覽器記太久
      'cache-control': 'public, max-age=3600',
      'x-content-type-options': 'nosniff',
    },
  });
}

/** 拿掉一個人的披風：那一筆、名單上的記號；沒人用的圖開始倒數。回傳那個人的 UUID。 */
async function dropCape(env, id, now) {
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (!have || !have.metadata) return null;
  await env.CORPUS.delete('cape:' + id);
  await sweep(env, have.metadata.h, have.metadata, now);
  if (have.metadata.p) await sweep(env, have.metadata.p.h, { ...have.metadata.p, q: have.metadata.q }, now);
  const uuid = have.metadata.u;
  if (uuid && UUID.test(uuid)) {
    const mine = await env.CORPUS.getWithMetadata('u:' + uuid);
    if (mine && mine.metadata) await remember(env, uuid, mine.metadata.c !== 0, now);
  }
  return uuid || null;
}

/**
 * 相同的圖併成一筆：誰在用、最近一次是什麼時候傳的。新的排前面。純函式。
 */
export function groupCapes(capes) {
  const byHash = new Map();
  for (const cape of capes) {
    let group = byHash.get(cape.h);
    if (!group) {
      group = { h: cape.h, t: cape.t, s: cape.s, v: 0, c: 0, ids: [] };
      if (cape.x && cape.y) {
        group.x = cape.x;                       // 早期上傳的沒有記尺寸，網頁會自己量
        group.y = cape.y;
      }
      byHash.set(cape.h, group);
    }
    group.ids.push(cape.id);
    group.v = Math.max(group.v, cape.v || 0);
    // 這張圖最早是什麼時候傳的：早期的那幾筆沒記（c），拿它換上的時間頂著
    const first = cape.c || cape.v || 0;
    if (first && (!group.c || first < group.c)) group.c = first;
  }
  return [...byHash.values()].sort((a, b) => b.v - a.v);
}

/** 閒置清單上的一筆，寫成一覽上的一筆：沒有人穿（ids 是空的），idle 是從什麼時候開始沒人穿。 */
function idleGroup(hash, one) {
  const group = { h: hash, t: one.t, s: one.s, v: one.at, c: one.c || one.at, ids: [], idle: one.at };
  if (one.x && one.y) {
    group.x = one.x;
    group.y = one.y;
  }
  return group;
}

/**
 * 披風一覽：公開、通過審核的那些，相同的圖併成一筆，附上幾個人加了最愛。
 *
 * <p>沒有人穿、還在倒數的也在上面（idle 有值的那幾筆）；keep 是留多久、now 是伺服器的時間，
 * 網頁拿這兩個算「幾天後移除」，不靠訪客電腦的時鐘。
 */
async function handleCapes(env, now) {
  const capes = await allCapes(env);
  const groups = groupCapes(capes.filter(listed));
  const idle = await purge(env, now);
  for (const hash of Object.keys(idle)) {
    if (!idle[hash].q && !capes.some((cape) => cape.h === hash)) groups.push(idleGroup(hash, idle[hash]));
  }
  const stars = await starMap(env);
  for (const group of groups) group.stars = Number(stars[group.h]) || 0;
  return json({ capes: groups, keep: IDLE_KEEP_MS, now }, 200, { 'cache-control': 'no-store' });
}

/**
 * 某個人的披風。
 *
 * <p>平常給的是他現在穿的那一件。key（網址上的 ?k=<雜湊>）有兩個用途：等審核的那一件
 * 只有知道雜湊的人拿得到（本人與管理的人——雜湊從 /api/me 與管理用的一覽來）；
 * 以及拿他使用紀錄裡的某一件（紀錄也是本人才看得到雜湊）。
 */
async function handleCape(env, id, key) {
  if (!ID.test(id)) return no('not found', 404);
  const cape = await env.CORPUS.getWithMetadata('cape:' + id);
  const now = cape && cape.metadata && cape.metadata.h ? cape.metadata : null;
  let hash = null;
  let type = null;
  if (now && (!key || key === now.h) && (!now.r || key === now.h)) {
    hash = now.h;
    type = now.t;
  } else if (key && HASH.test(key)) {
    const past = await env.CORPUS.getWithMetadata('hist:' + id + ':' + key);
    if (past && past.metadata && (!now || now.h !== key)) {
      hash = key;
      type = past.metadata.t;
    }
  }
  if (!hash) return no('not found', 404);
  const have = await env.CORPUS.getWithMetadata('blob:' + hash, { type: 'arrayBuffer' });
  if (!have || !have.value) return no('not found', 404);
  return new Response(have.value, {
    status: 200,
    headers: {
      'content-type': type === 'gif' ? 'image/gif' : 'image/png',
      // 網址帶著版本（?v=）或雜湊（?k=），同一個網址的內容不會變
      'cache-control': key ? 'private, max-age=86400, immutable' : 'public, max-age=86400, immutable',
      'x-content-type-options': 'nosniff',
    },
  });
}

/** 替瀏覽器登入：拿模組的通行證換一條一天有效的網址。# 後面那一段不會送到任何伺服器。 */
async function handleWebLink(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const token = await issueWebToken(env, user.uuid, user.name, now);
  return json({ ok: true, url: new URL(request.url).origin + '/capes#t=' + token });
}

/** 我是誰。網頁拿這個決定畫面上要有什麼。 */
async function handleMe(request, env, now) {
  const user = await who(request, env, now);
  if (!user) {
    // 用 Discord 登入了、還沒綁遊戲 ID：認得是誰，但還不是任何一個玩家
    const session = await discordSession(request, env, now);
    if (!session) return no('login first', 401);
    return json({ ok: true, linked: false, discord: { name: session.account.n || '' },
      staff: Boolean(await env.CORPUS.get('staff:d:' + session.id)) });
  }
  const id = await publicId(user.uuid);
  const cape = await env.CORPUS.getWithMetadata('cape:' + id);
  const list = await credits(now);
  const name = user.name.toLowerCase();
  // 特別名單上的人也算成員（上傳的待遇一樣），但不是開發者、也不會有葉子
  const member = Boolean(list && list.names.has(name)) || await hasPerk(env, user.uuid);
  const developer = Boolean(list && list.developers.has(name));
  return json({
    ok: true,
    linked: true,
    discord: user.discord ? { name: user.discordName } : null,
    name: user.name,
    id,
    developer,
    // 管理頁開不開：開發者，或開發者加的管理員（不在名單上、沒有葉子）
    staff: developer || await isStaff(env, user),
    member,
    banned: Boolean(await env.CORPUS.get('ban:' + user.uuid)),
    uploads: env.UPLOADS || 'tiered',
    limit: member ? MAX_CAPE_BYTES : MAX_PLAIN_CAPE_BYTES,
    // h 是那張圖的雜湊（一覽上的那一件就是它）；worn 表示這件是從一覽套用的，back 表示復原會換回自己傳的那一件
    cape: cape && cape.metadata ? { t: cape.metadata.t, v: cape.metadata.v, s: cape.metadata.s,
      x: cape.metadata.x || 0, y: cape.metadata.y || 0, h: cape.metadata.h,
      worn: Boolean(cape.metadata.w), back: Boolean(cape.metadata.w && cape.metadata.p),
      // public：出現在披風一覽上；pending：在等審核，通過之前別人看不到
      public: !cape.metadata.q, pending: Boolean(cape.metadata.r) } : null,
    // 在審核名單上：之後上傳的披風要先通過
    review: Boolean(await env.CORPUS.get('review:' + user.uuid)),
  });
}

/**
 * 這個請求有沒有管理權（開發者或管理員）。有的話回是誰（寫進紀錄用），沒有回 null。
 * 只有開發者能做的事（動管理員名單）另外看 authority().developer。
 */
async function moderator(request, env, now) {
  const mine = await authority(request, env, now);
  return mine ? mine.by : null;
}

/**
 * 拿掉披風：{id} 是一個人的，{hash} 是所有用同一張圖的。ban 是 true 的話那些人之後不能再傳。
 */
async function handleAdminRemove(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  const body = await readJson(request);
  if (!body) return no('bad request');
  let targets;
  const capes = await allCapes(env);
  if (typeof body.id === 'string' && ID.test(body.id)) {
    targets = capes.filter((cape) => cape.id === body.id);
  } else if (typeof body.hash === 'string' && /^[0-9a-f]{64}$/.test(body.hash)) {
    targets = capes.filter((cape) => cape.h === body.hash);
  } else {
    return no('bad request');
  }
  if (typeof body.hash === 'string') {
    // 照圖刪的：有人把這張圖收著準備復原的，也一起拿掉——不然刪了還能被穿回來
    for (const cape of capes) {
      if (!cape.p || cape.p.h !== body.hash || cape.h === body.hash) continue;
      const { id: owner, p: gone, ...rest } = cape;
      await env.CORPUS.put('cape:' + owner, '', { metadata: rest });
    }
    // 留在別人使用紀錄裡的也拿掉，最愛的人數表也清掉這一張
    for (const one of await allHistory(env)) {
      if (one.h === body.hash) await env.CORPUS.delete(one.key);
    }
    const stars = await starMap(env);
    if (body.hash in stars) {
      delete stars[body.hash];
      await env.CORPUS.put('stars', JSON.stringify(stars));
    }
  }
  let removed = 0;
  for (const cape of targets) {
    await env.CORPUS.delete('hist:' + cape.id + ':' + cape.h);     // 他的使用紀錄裡也不留，不然穿得回來
    const uuid = await dropCape(env, cape.id, now);
    if (!uuid) continue;
    removed++;
    if (body.ban === true) {
      await env.CORPUS.put('ban:' + uuid, JSON.stringify({ n: cape.n || null, t: now, by }));
    }
    // 管理的人拿掉的不留七天：沒有別人在用就馬上刪
    await discard(env, cape.h);
    if (cape.p) await discard(env, cape.p.h);
  }
  let gone = false;
  if (typeof body.hash === 'string') {
    // 沒有人穿、還在倒數的那一張也可以照圖刪
    gone = Boolean((await idleMap(env))[body.hash]);
    await discard(env, body.hash);
  }
  return json({ ok: true, removed: removed || (gone ? 1 : 0), banned: body.ban === true });
}

/** 所有上傳的披風，連同每一件是誰傳的。只給管理的人看。 */
async function handleAdminCapes(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const capes = await allCapes(env);
  const groups = groupCapes(capes);
  const stars = await starMap(env);
  for (const group of groups) {
    const wearers = capes.filter((cape) => cape.h === group.h);
    group.owners = wearers.map((cape) => ({ id: cape.id, name: cape.n || null, uuid: cape.u, v: cape.v,
      private: Boolean(cape.q), pending: Boolean(cape.r), worn: Boolean(cape.w) }));
    group.stars = Number(stars[group.h]) || 0;
    // 管理的人看得到全部：沒有人公開的標「不公開」，有人在等審核的標「待審核」
    group.hidden = wearers.every((cape) => cape.q || cape.r);
    group.pending = wearers.some((cape) => cape.r);
  }
  // 沒有人穿、還在倒數的：管理的人全部看得到（原本不公開的也是），要刪可以馬上刪
  const idle = await purge(env, now);
  for (const hash of Object.keys(idle)) {
    if (capes.some((cape) => cape.h === hash)) continue;
    groups.push({ ...idleGroup(hash, idle[hash]), owners: [], stars: Number(stars[hash]) || 0,
      hidden: Boolean(idle[hash].q), pending: false });
  }
  return json({ capes: groups, keep: IDLE_KEEP_MS, now }, 200, { 'cache-control': 'no-store' });
}

/** 特別名單：誰在上面。只給管理的人看。 */
async function handlePerks(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const perks = [];
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'perk:', limit: 1000, cursor });
    for (const k of page.keys) {
      let record = {};
      try {
        record = JSON.parse((await env.CORPUS.get(k.name)) || '{}');
      } catch (e) {
        record = {};
      }
      if (typeof record !== 'object' || record === null) record = {};
      perks.push({ uuid: k.name.slice(5), note: record.n || '', t: record.t || null, by: record.by || null });
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  perks.sort((a, b) => (b.t || 0) - (a.t || 0));
  return json({ perks }, 200, { 'cache-control': 'no-store' });
}

async function handlePerkAdd(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  const body = await readJson(request);
  if (!body) return no('bad request');
  const uuid = plainUuid(body.uuid);
  if (!uuid) return no('bad uuid');
  const note = body.note == null ? '' : body.note;
  if (typeof note !== 'string' || note.trim().length > 60) return no('bad note');
  if (await env.CORPUS.get('perk:' + uuid)) return no('already', 409);
  await env.CORPUS.put('perk:' + uuid, JSON.stringify({ n: note.trim(), t: now, by }));
  return json({ ok: true, uuid });
}

async function handlePerkRemove(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  const uuid = body ? plainUuid(body.uuid) : null;
  if (!uuid) return no('bad request');
  await env.CORPUS.delete('perk:' + uuid);
  return json({ ok: true });
}

/** 封禁名單。 */
async function handleBans(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const bans = [];
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'ban:', limit: 1000, cursor });
    for (const k of page.keys) {
      let record = {};
      try {
        record = JSON.parse((await env.CORPUS.get(k.name)) || '{}');
      } catch (e) {
        record = {};                     // 舊格式只存了時間
      }
      if (typeof record !== 'object' || record === null) record = {};
      bans.push({ uuid: k.name.slice(4), name: record.n || null, t: record.t || null, by: record.by || null });
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  bans.sort((a, b) => (b.t || 0) - (a.t || 0));
  return json({ bans }, 200, { 'cache-control': 'no-store' });
}

async function handleUnban(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  if (!body || typeof body.uuid !== 'string' || !UUID.test(body.uuid)) return no('bad request');
  await env.CORPUS.delete('ban:' + body.uuid);
  return json({ ok: true });
}

// ---------------------------------------------------------------- 名單：替 credits.json 加一個人

const REPO = 'LyuChaCha/WynnChaYuan';
const CREDITS_PATH = 'src/main/resources/assets/wynnchayuan/credits.json';

/** 可以加到哪幾區。鍵是 credits.json 裡那一區 key 的最後一段。 */
const SECTIONS = { developers: '開發者', sponsors: '贊助者', contributors: '貢獻者' };

/**
 * 加一個成員之後的 credits.json。純函式，測試直接餵。
 *
 * @returns {{text: string}|{why: string}}
 */
export function addMember(text, entry) {
  if (!entry || typeof entry !== 'object') return { why: 'bad request' };
  const { section, name, mc, color } = entry;
  if (!Object.hasOwn(SECTIONS, section)) return { why: 'bad section' };
  if (typeof name !== 'string' || !name.trim() || name.length > 40 || /[\u0000-\u001f<>]/.test(name)) {
    return { why: 'bad name' };
  }
  if (typeof mc !== 'string' || !NAME.test(mc)) return { why: 'bad mc' };
  if (color !== undefined && color !== '' && (typeof color !== 'string' || !/^#[0-9A-Fa-f]{6}$/.test(color))) {
    return { why: 'bad color' };
  }
  let doc;
  try {
    doc = JSON.parse(text);
  } catch (e) {
    return { why: 'bad credits' };
  }
  const target = (doc.sections || []).find((item) => typeof item.key === 'string'
    && item.key.endsWith('.' + section));
  if (!target || !Array.isArray(target.members)) return { why: 'bad credits' };
  if (target.members.some((member) => typeof member.mc === 'string'
      && member.mc.toLowerCase() === mc.toLowerCase())) {
    return { why: 'already listed' };
  }
  const member = { name: name.trim(), mc };
  if (color) member.color = color.toUpperCase();
  target.members.push(member);
  // 倉庫裡那一份就是兩格縮排、結尾一個換行；照這樣寫回去，diff 只有新增的那幾行
  return { text: JSON.stringify(doc, null, 2) + '\n' };
}

function utf8ToBase64(text) {
  let binary = '';
  for (const byte of new TextEncoder().encode(text)) binary += String.fromCharCode(byte);
  return btoa(binary);
}

function base64ToUtf8(base64) {
  const binary = atob(base64.replace(/\s/g, ''));
  return new TextDecoder().decode(Uint8Array.from(binary, (c) => c.charCodeAt(0)));
}

/**
 * 替名單加一個人：在倉庫開一個分支、改 credits.json、開 PR。合併還是照倉庫的規矩來
 * （檢查要過、要有人按）——這裡只是省掉手改 JSON。
 * 要先設好 GITHUB_TOKEN（能寫這個倉庫的內容與 PR 的 token）。
 */
async function handleAddCredit(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  if (!env.GITHUB_TOKEN) return no('github not configured', 503);
  let entry;
  try {
    entry = JSON.parse(await request.text());
  } catch (e) {
    return no('bad request');
  }
  const github = (path, init = {}) => fetch('https://api.github.com/repos/' + REPO + path, {
    ...init,
    headers: {
      authorization: 'Bearer ' + env.GITHUB_TOKEN,
      accept: 'application/vnd.github+json',
      'user-agent': 'wynnchayuan-site',
      'content-type': 'application/json',
    },
  });
  try {
    const file = await github('/contents/' + CREDITS_PATH + '?ref=main');
    if (file.status !== 200) return no('github ' + file.status, 502);
    const current = await file.json();
    const changed = addMember(base64ToUtf8(current.content), entry);
    if (changed.why) return no(changed.why, changed.why === 'already listed' ? 409 : 400);
    const head = await github('/git/ref/heads/main');
    if (head.status !== 200) return no('github ' + head.status, 502);
    const branch = 'credits/add-' + entry.mc.toLowerCase() + '-' + now;
    const made = await github('/git/refs', {
      method: 'POST',
      body: JSON.stringify({ ref: 'refs/heads/' + branch, sha: (await head.json()).object.sha }),
    });
    if (made.status !== 201) return no('github ' + made.status, 502);
    const title = '名單：' + SECTIONS[entry.section] + '加入 ' + entry.name.trim() + '（' + entry.mc + '）';
    const commit = await github('/contents/' + CREDITS_PATH, {
      method: 'PUT',
      body: JSON.stringify({ message: title, content: utf8ToBase64(changed.text), sha: current.sha, branch }),
    });
    if (commit.status !== 200 && commit.status !== 201) return no('github ' + commit.status, 502);
    const pull = await github('/pulls', {
      method: 'POST',
      body: JSON.stringify({
        title,
        head: branch,
        base: 'main',
        body: '從網站的管理頁送出，送出的人：' + by + '。\n\n'
          + '在 `credits.json` 的「' + SECTIONS[entry.section] + '」加一個成員。合併之後，名牌上的葉子與'
          + '披風的資格會在玩家下次同步名單時生效。',
      }),
    });
    if (pull.status !== 201) return no('github ' + pull.status, 502);
    return json({ ok: true, url: (await pull.json()).html_url });
  } catch (e) {
    return no('github unreachable', 502);
  }
}

/** 網站的圖示。/favicon.ico 是瀏覽器沒人叫也會自己來要的那一個，給它同一張 PNG。 */
const ICONS = {
  '/favicon.ico': FAVICON,
  '/favicon.png': FAVICON,
  '/icon-192.png': ICON_192,
  '/apple-touch-icon.png': APPLE_TOUCH,
};

function icon(base64) {
  const raw = atob(base64);
  const bytes = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
  return new Response(bytes, {
    headers: {
      'content-type': 'image/png',
      'cache-control': 'public, max-age=86400',
      'x-content-type-options': 'nosniff',
    },
  });
}

function page(html, lang, status = 200) {
  return new Response(html, {
    status,
    headers: {
      'content-type': 'text/html; charset=utf-8',
      'content-language': tagOf(lang),
      'cache-control': 'no-store',
      vary: 'Cookie, Accept-Language',
      'referrer-policy': 'no-referrer',
      'x-content-type-options': 'nosniff',
      'x-frame-options': 'DENY',
      // 只跑自己的程式與 jsDelivr 上的立體檢視；圖從自己這裡、玩家的頭像與全身圖（minotar）、倉庫的標誌來
      'content-security-policy': "default-src 'self'; "
        + "img-src 'self' data: blob: https://raw.githubusercontent.com https://minotar.net; "
        + "style-src 'unsafe-inline'; script-src 'unsafe-inline' https://cdn.jsdelivr.net; "
        + "connect-src 'self'; base-uri 'none'; form-action 'none'",
    },
  });
}

/**
 * 預覽畫面用的點陣字（demo-fonts.js：從模組附的 Fusion Pixel 裁出來的幾百個字）。
 * 網址帶著內容的雜湊（?v=），所以可以放心讓瀏覽器記一年。
 */
function font(path) {
  const found = /^\/font\/px-([a-z]{2})\.ttf$/.exec(path);
  const one = found && Object.hasOwn(FONTS, found[1]) ? FONTS[found[1]] : null;
  if (!one) return no('not found', 404);
  const bytes = Uint8Array.from(atob(one.data), (c) => c.charCodeAt(0));
  return new Response(bytes, {
    status: 200,
    headers: {
      'content-type': 'font/ttf',
      'cache-control': 'public, max-age=31536000, immutable',
      'x-content-type-options': 'nosniff',
    },
  });
}

/**
 * 預覽畫面用的材質（demo-textures.js：Wynncraft 的介面貼圖、物品圖示與實機截圖，© Wynncraft）。
 * 網址帶著內容的雜湊，所以可以長期快取。
 */
function texture(path) {
  // /tex/ 是預覽的材質，/template/ 是披風範本（cape-templates.js）
  const from = path.startsWith('/template/') ? CAPE_TEMPLATES : TEXTURES;
  const name = path.slice(path.indexOf('/', 1) + 1);
  const one = Object.hasOwn(from, name) ? from[name] : null;
  if (!one) return no('not found', 404);
  const bytes = Uint8Array.from(atob(one.data), (c) => c.charCodeAt(0));
  return new Response(bytes, {
    status: 200,
    headers: {
      'content-type': one.type,
      'cache-control': 'public, max-age=31536000, immutable',
      'x-content-type-options': 'nosniff',
    },
  });
}

export default {
  /** 每天一次（wrangler.toml 的 crons）：沒有人穿滿七天的披風刪掉。 */
  async scheduled(event, env) {
    await tidy(env, Date.now());
  },
  async fetch(request, env) {
    const url = new URL(request.url);
    const now = Date.now();
    const path = url.pathname;
    if (request.method === 'GET' && PATHS.includes(path)) {
      // 語言：網址上指定的（?lang=en，給人貼連結用）> 訪客選過的 > 瀏覽器的語言 > 英文
      const asked = url.searchParams.get('lang');
      const lang = LANGS.includes(asked) ? asked
        : pickLang(request.headers.get('cookie'), request.headers.get('accept-language'));
      // 什麼語言都沒說的訪客（沒有 ?lang、沒選過、瀏覽器也沒報）多半是抓連結預覽的機器人
      const bare = !LANGS.includes(asked) && !request.headers.get('accept-language')
        && !/(?:^|;\s*)lang=/.test(request.headers.get('cookie') || '');
      return page(render(path, lang, bare), lang);
    }
    if (request.method === 'GET' && Object.hasOwn(ICONS, path)) return icon(ICONS[path]);
    if (request.method === 'GET' && path.startsWith('/font/')) return font(path);
    if (request.method === 'GET' && (path.startsWith('/tex/') || path.startsWith('/template/'))) return texture(path);
    if (request.method === 'GET' && path === '/auth/discord') return handleDiscordStart(request, env, now);
    if (request.method === 'GET' && path === '/auth/discord/callback') {
      return handleDiscordCallback(request, env, now);
    }
    if (request.method === 'GET' && path === '/gallery') {
      return Response.redirect(url.origin + '/capes', 302);
    }
    if (!path.startsWith('/api/')) {
      // 找不到這一頁：給人看的是一頁有頁首頁尾的網頁（照他的語言），不是一行 JSON
      if (request.method !== 'GET') return no('not found', 404);
      const lang = pickLang(request.headers.get('cookie'), request.headers.get('accept-language'));
      return page(renderLost(lang), lang, 404);
    }
    const route = request.method + ' ' + path.slice(4);
    if (route === 'POST /challenge') return handleChallenge(env, now);
    if (route === 'POST /login') return handleLogin(request, env, now);
    if (route === 'POST /web-link') return handleWebLink(request, env, now);
    if (route === 'POST /link/start') return handleLinkStart(request, env, now);
    if (route === 'POST /link/intent') return handleLinkIntent(request, env, now);
    if (route === 'POST /link') return handleLink(request, env, now);
    if (route === 'POST /unlink') return handleUnlink(request, env, now);
    if (route === 'GET /me') return handleMe(request, env, now);
    if (route === 'POST /hello') return handleHello(request, env, now);
    if (route === 'GET /list') return handleList(env, now);
    if (route === 'PUT /cape') return handleUpload(request, env, now);
    if (route === 'DELETE /cape') return handleRemove(request, env, now);
    if (route === 'GET /capes') return handleCapes(env, now);
    if (route === 'GET /admin/capes') return handleAdminCapes(request, env, now);
    if (route === 'POST /admin/remove') return handleAdminRemove(request, env, now);
    if (route === 'GET /admin/perks') return handlePerks(request, env, now);
    if (route === 'POST /admin/perk') return handlePerkAdd(request, env, now);
    if (route === 'POST /admin/unperk') return handlePerkRemove(request, env, now);
    if (route === 'GET /admin/bans') return handleBans(request, env, now);
    if (route === 'POST /admin/unban') return handleUnban(request, env, now);
    if (route === 'POST /admin/credits') return handleAddCredit(request, env, now);
    if (route === 'GET /admin/staff') return handleStaff(request, env, now);
    if (route === 'POST /admin/staff') return handleStaffAdd(request, env, now);
    if (route === 'POST /admin/unstaff') return handleStaffRemove(request, env, now);
    if (route === 'GET /admin/people') return handlePeople(request, env, now);
    if (route === 'GET /team') return handleTeam(env, now);
    if (route === 'POST /cape/wear') return handleWear(request, env, now);
    if (route === 'POST /cape/restore') return handleRestore(request, env, now);
    if (route === 'POST /cape/star') return handleStar(request, env, now);
    if (route === 'POST /cape/public') return handlePublic(request, env, now);
    if (route === 'POST /cape/forget') return handleForget(request, env, now);
    if (route === 'GET /capes/mine') return handleMine(request, env, now);
    if (route === 'GET /admin/reviews') return handleReviews(request, env, now);
    if (route === 'POST /admin/review') return handleReviewAdd(request, env, now);
    if (route === 'POST /admin/unreview') return handleReviewRemove(request, env, now);
    if (route === 'POST /admin/approve') return handleApprove(request, env, now);
    if (route === 'GET /admin/guilds') return handleGuilds(request, env, now);
    if (route === 'POST /admin/guild') return handleGuildAdd(request, env, now);
    if (route === 'POST /admin/unguild') return handleGuildRemove(request, env, now);
    {
      const picture = /^(GET|PUT) \/(admin\/)?guild\/([a-z-]{3,30})\/(icon|banner)$/.exec(route);
      if (picture && GUILD_SLUG.test(picture[3])) {
        if (picture[1] === 'PUT' && picture[2]) return handleGuildImage(request, env, now, picture[3], picture[4]);
        if (picture[1] === 'GET' && !picture[2]) return handleGuildPicture(env, picture[3], picture[4]);
      }
    }
    if (request.method === 'GET' && route.startsWith('GET /idle/')) {
      return handleIdle(env, route.slice('GET /idle/'.length));
    }
    if (request.method === 'GET' && route.startsWith('GET /cape/')) {
      return handleCape(env, route.slice('GET /cape/'.length), url.searchParams.get('k'));
    }
    if (route === 'GET /status') {
      const list = await storedList(env);
      return json({
        what: 'WynnChaYuan',
        users: list ? list.users.length : 0,
        capes: list ? Object.keys(list.capes).length : 0,
        uploads: env.UPLOADS || 'tiered',
        discord: discordOn(env),
      });
    }
    return no('not found', 404);
  },
};
