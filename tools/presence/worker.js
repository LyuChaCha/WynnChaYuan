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
 *   GET    /api/admin/bans                        封禁名單（開發者）
 *   POST   /api/admin/unban    {uuid}             解除封禁（開發者）
 *   POST   /api/admin/credits  {section, name, mc, color}   替名單加一個人：開一個 PR（開發者）
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

import { LANGS, PATHS, pickLang, render, tagOf } from './site.js';
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
      await env.CORPUS.put('d:' + before, JSON.stringify(other));
    }
  }
  if (record.u && record.u !== uuid) await env.CORPUS.delete('l:' + record.u);
  record.u = uuid;
  record.m = name;
  record.t = now;
  await env.CORPUS.put('d:' + id, JSON.stringify(record));
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
  await env.CORPUS.put('d:' + id, JSON.stringify(record));
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
    await env.CORPUS.put('d:' + session.id, JSON.stringify(record));
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
    await env.CORPUS.put('d:' + id, JSON.stringify(record));
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
  return json({ ok: true, token: await issueToken(env, body.uuid, name, now), id: await publicId(body.uuid) });
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
  const version = cape && cape.metadata ? cape.metadata.v : 0;
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
      for (const section of (await answer.json()).sections || []) {
        const dev = typeof section.key === 'string' && section.key.endsWith('.developers');
        for (const member of section.members || []) {
          if (typeof member.mc !== 'string' || !member.mc) continue;
          names.add(member.mc.toLowerCase());
          if (dev) developers.add(member.mc.toLowerCase());
        }
      }
      cachedCredits = { t: now, names, developers };
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
  const list = await credits(now);
  if (!list) return 'try later';
  if (list.names.has(user.name.toLowerCase())) return null;
  return mode === 'tiered' ? 'gif members only' : 'not listed';
}

/** 這個人的披風最大可以多大。 */
async function capeLimit(name, now) {
  const list = await credits(now);
  return list && list.names.has(name.toLowerCase()) ? MAX_CAPE_BYTES : MAX_PLAIN_CAPE_BYTES;
}

/** 這個人是不是開發者（可以拿掉別人的披風）。 */
async function isDeveloper(name, now) {
  const list = await credits(now);
  return Boolean(list && list.developers.has(name.toLowerCase()));
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
  // 一般玩家 1 MB，名單上的成員 20 MB。名單讀不到的時候當成一般玩家。
  const limit = await capeLimit(user.name, now);
  if (bytes.length > limit) return json({ ok: false, why: 'too large', limit }, 413);
  const id = await publicId(user.uuid);
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (have && have.metadata && now - have.metadata.v < UPLOAD_EVERY_MS) {
    return no('slow down', 429);
  }
  // 圖檔照內容存（blob:<雜湊>），披風那一筆只記它指到哪一張——
  // 好幾個人傳同一張圖，只存一份。
  const hash = hex(await crypto.subtle.digest('SHA-256', bytes));
  const blob = await env.CORPUS.getWithMetadata('blob:' + hash, { type: 'stream' });
  if (blob && blob.value && blob.value.cancel) blob.value.cancel();
  if (!blob || !blob.metadata) {
    await env.CORPUS.put('blob:' + hash, bytes, { metadata: { t: found.type, s: bytes.length } });
  }
  await env.CORPUS.put('cape:' + id, '', {
    metadata: { t: found.type, v: now, s: bytes.length, u: user.uuid, n: user.name, h: hash,
      x: found.width, y: found.height },
  });
  if (have && have.metadata && have.metadata.h !== hash) await sweep(env, have.metadata.h);
  const mine = await env.CORPUS.getWithMetadata('u:' + user.uuid);
  await remember(env, user.uuid, !(mine && mine.metadata && mine.metadata.c === 0), now);
  return json({ ok: true, id, v: now, ...found });
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

/** 這張圖沒有人在用了就刪掉。 */
async function sweep(env, hash) {
  if (!hash) return;
  if ((await allCapes(env)).some((cape) => cape.h === hash)) return;
  await env.CORPUS.delete('blob:' + hash);
}

/** 拿掉一個人的披風：那一筆、沒人用的圖、名單上的記號。回傳那個人的 UUID。 */
async function dropCape(env, id, now) {
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (!have || !have.metadata) return null;
  await env.CORPUS.delete('cape:' + id);
  await sweep(env, have.metadata.h);
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
      group = { h: cape.h, t: cape.t, s: cape.s, v: 0, ids: [] };
      if (cape.x && cape.y) {
        group.x = cape.x;                       // 早期上傳的沒有記尺寸，網頁會自己量
        group.y = cape.y;
      }
      byHash.set(cape.h, group);
    }
    group.ids.push(cape.id);
    group.v = Math.max(group.v, cape.v || 0);
  }
  return [...byHash.values()].sort((a, b) => b.v - a.v);
}

async function handleCapes(env) {
  return json({ capes: groupCapes(await allCapes(env)) }, 200, { 'cache-control': 'no-store' });
}

async function handleCape(env, id) {
  if (!ID.test(id)) return no('not found', 404);
  const cape = await env.CORPUS.getWithMetadata('cape:' + id);
  if (!cape || !cape.metadata || !cape.metadata.h) return no('not found', 404);
  const have = await env.CORPUS.getWithMetadata('blob:' + cape.metadata.h, { type: 'arrayBuffer' });
  if (!have || !have.value) return no('not found', 404);
  have.metadata = cape.metadata;
  return new Response(have.value, {
    status: 200,
    headers: {
      'content-type': have.metadata.t === 'gif' ? 'image/gif' : 'image/png',
      // 網址帶著版本（?v=），同一個版本的內容不會變
      'cache-control': 'public, max-age=86400, immutable',
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
    return json({ ok: true, linked: false, discord: { name: session.account.n || '' } });
  }
  const id = await publicId(user.uuid);
  const cape = await env.CORPUS.getWithMetadata('cape:' + id);
  const list = await credits(now);
  const name = user.name.toLowerCase();
  return json({
    ok: true,
    linked: true,
    discord: user.discord ? { name: user.discordName } : null,
    name: user.name,
    id,
    developer: Boolean(list && list.developers.has(name)),
    member: Boolean(list && list.names.has(name)),
    banned: Boolean(await env.CORPUS.get('ban:' + user.uuid)),
    uploads: env.UPLOADS || 'tiered',
    limit: list && list.names.has(name) ? MAX_CAPE_BYTES : MAX_PLAIN_CAPE_BYTES,
    cape: cape && cape.metadata ? { t: cape.metadata.t, v: cape.metadata.v, s: cape.metadata.s,
      x: cape.metadata.x || 0, y: cape.metadata.y || 0 } : null,
  });
}

/**
 * 這個請求有沒有管理權。有的話回是誰（寫進紀錄用），沒有回 null。
 * 兩條路：本人在名單上是開發者（每次都重新查名單），或帶著 ADMIN_TOKEN。
 */
async function moderator(request, env, now) {
  const header = request.headers.get('authorization') || '';
  if (env.ADMIN_TOKEN && header === 'Bearer ' + env.ADMIN_TOKEN) return 'admin';
  const user = await who(request, env, now);
  return user && await isDeveloper(user.name, now) ? user.name : null;
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
  let removed = 0;
  for (const cape of targets) {
    const uuid = await dropCape(env, cape.id, now);
    if (!uuid) continue;
    removed++;
    if (body.ban === true) {
      await env.CORPUS.put('ban:' + uuid, JSON.stringify({ n: cape.n || null, t: now, by }));
    }
  }
  return json({ ok: true, removed, banned: body.ban === true });
}

/** 所有上傳的披風，連同每一件是誰傳的。只給管理的人看。 */
async function handleAdminCapes(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const capes = await allCapes(env);
  const groups = groupCapes(capes);
  for (const group of groups) {
    group.owners = capes.filter((cape) => cape.h === group.h)
      .map((cape) => ({ id: cape.id, name: cape.n || null, uuid: cape.u, v: cape.v }));
  }
  return json({ capes: groups }, 200, { 'cache-control': 'no-store' });
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

function page(html, lang) {
  return new Response(html, {
    headers: {
      'content-type': 'text/html; charset=utf-8',
      'content-language': tagOf(lang),
      'cache-control': 'no-store',
      vary: 'Cookie, Accept-Language',
      'referrer-policy': 'no-referrer',
      'x-content-type-options': 'nosniff',
      'x-frame-options': 'DENY',
      // 只跑自己的程式與 jsDelivr 上的立體檢視；圖從自己這裡、玩家頭像與倉庫的標誌來
      'content-security-policy': "default-src 'self'; "
        + "img-src 'self' data: blob: https://raw.githubusercontent.com https://mc-heads.net; "
        + "style-src 'unsafe-inline'; script-src 'unsafe-inline' https://cdn.jsdelivr.net; "
        + "connect-src 'self'; base-uri 'none'; form-action 'none'",
    },
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const now = Date.now();
    const path = url.pathname;
    if (request.method === 'GET' && PATHS.includes(path)) {
      // 語言：網址上指定的（?lang=en，給人貼連結用）> 訪客選過的 > 瀏覽器的語言 > 英文
      const asked = url.searchParams.get('lang');
      const lang = LANGS.includes(asked) ? asked
        : pickLang(request.headers.get('cookie'), request.headers.get('accept-language'));
      return page(render(path, lang), lang);
    }
    if (request.method === 'GET' && Object.hasOwn(ICONS, path)) return icon(ICONS[path]);
    if (request.method === 'GET' && path === '/auth/discord') return handleDiscordStart(request, env, now);
    if (request.method === 'GET' && path === '/auth/discord/callback') {
      return handleDiscordCallback(request, env, now);
    }
    if (request.method === 'GET' && path === '/gallery') {
      return Response.redirect(url.origin + '/capes', 302);
    }
    if (!path.startsWith('/api/')) return no('not found', 404);
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
    if (route === 'GET /capes') return handleCapes(env);
    if (route === 'GET /admin/capes') return handleAdminCapes(request, env, now);
    if (route === 'POST /admin/remove') return handleAdminRemove(request, env, now);
    if (route === 'GET /admin/bans') return handleBans(request, env, now);
    if (route === 'POST /admin/unban') return handleUnban(request, env, now);
    if (route === 'POST /admin/credits') return handleAddCredit(request, env, now);
    if (request.method === 'GET' && route.startsWith('GET /cape/')) {
      return handleCape(env, route.slice('GET /cape/'.length));
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
