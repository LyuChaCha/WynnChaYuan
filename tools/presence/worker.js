/**
 * WynnChaYuan 的網站與登記站（wynnchayuan.com）。
 *
 * 網頁（內容在 site.js）：
 *   /          首頁
 *   /capes     披風一覽
 *   /account   帳號：登入狀態、自己的披風、上傳
 *   /feedback  回饋與建議（要用 Discord 登入；譯文建議自動開 GitHub Issue）
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
 *   POST   /api/feedback       {cat, msg, where?, lang?}   送一則回饋（要用 Discord 登入）；譯文建議另外開 GitHub Issue
 *   GET    /api/admin/feedback?cat=&st=      回饋列表，新的在前（開發者與管理員）
 *   POST   /api/admin/feedback/status {id, st}   標成已處理（done）或重新開啟（open）
 *   POST   /api/admin/feedback/delete {id}       刪掉一則（垃圾訊息）
 *   POST   /api/admin/feedback/issue  {id}       譯文建議的 Issue 上次沒開成：再開一次
 *   GET    /font/px-<代號>.ttf       預覽畫面用的點陣字（demo-fonts.js）
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
import { capePreview, figurePreview } from './cape-preview.js';

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
  // 每一種失敗帶一個原因碼：帳號頁照樣只說「沒有成功」，但看網址或 wrangler tail 就知道卡在哪一步
  const failed = (reason) => {
    console.log('discord sign-in failed: ' + reason);
    return redirect(url.origin + '/account#e=discord&r=' + reason, oauthCookie('', 0));
  };
  if (!discordOn(env)) return failed('off');
  const code = url.searchParams.get('code') || '';
  const parts = (url.searchParams.get('state') || '').split('.');
  const cookie = new RegExp('(?:^|;\\s*)' + OAUTH_COOKIE + '=([0-9a-f]{32}(?:[0-9a-f]{32})?)(?:;|$)')
    .exec(request.headers.get('cookie') || '');
  if (!code || code.length > 512) return failed(url.searchParams.get('error') === 'access_denied' ? 'denied' : 'code');
  if (parts.length !== 3 || !cookie || cookie[1] !== parts[1]) return failed('cookie');
  if (!/^\d{1,16}$/.test(parts[0]) || now - Number(parts[0]) > 600000 || Number(parts[0]) > now + 60000) return failed('expired');
  const expected = await sign(env, 'oauth.' + parts[0] + '.' + parts[1]);
  if (expected !== parts[2]) return failed('state');
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
    if (token.status !== 200) {
      // Discord 的錯誤名（invalid_client、invalid_grant…）不含任何金鑰，可以照原樣帶回來
      const why = await token.json().then((body) => String(body.error || ''), () => '');
      return failed('token-' + token.status + (/^[a-z_]{1,40}$/.test(why) ? '-' + why : ''));
    }
    const access = (await token.json()).access_token;
    if (typeof access !== 'string' || !access) return failed('token-empty');
    const me = await fetch(DISCORD_API + '/users/@me', { headers: { authorization: 'Bearer ' + access } });
    if (me.status !== 200) return failed('me-' + me.status);
    profile = await me.json();
  } catch (e) {
    return failed('network');
  }
  const id = profile && typeof profile.id === 'string' ? profile.id : '';
  if (!DISCORD_ID.test(id)) return failed('profile');
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
 * 與傳過披風的人。開發者與管理員都能用（管理員名單、譯者名單選人）。回 {people: [{discord, dname, mc, uuid}]}：沒帶 q 是全部、照名字排；
 * 帶了 q 只回對得上的，對得越準越前面。網頁展開選單時抓一次全部，篩選在瀏覽器裡做。
 */
async function handlePeople(request, env, now) {
  // 管理員也能用：譯者名單也要選人（2026-10-16）
  if (!await moderator(request, env, now)) return no('forbidden', 403);
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
  await dropPreviews(env, hash);
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
    await dropPreviews(env, hash);
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

// ---------------------------------------------------------------- 協作翻譯
//
// 使用者 2026-10-16：「像 Crowdin 一樣可以提交翻譯（校稿）」「不同團隊對不同語言有不同意見，所以採取投票」。
// 選了：做在自己的網站、術語投票與整句提案兩個一起、各語言團隊投票、管理員定案。
//
// 誰能做什麼
//   看：任何人（瀏覽語料、看提案與票數）
//   提案、投票：那個語言的譯者（管理頁指派，KV tl:<鍵>），以及開發者、管理員
//   定案：開發者、管理員。整句提案定案＝替倉庫開 PR（同一個語言同一天的放在同一個 PR）；
//         術語投票定案只記下結果，套到整個語料由維護的人來做（一個詞散在幾百條裡，不能自動換）
//
// KV
//   tl:<u:UUID｜d:Discord 代號｜n:名字> → {langs, n 備註, t, by, m 遊戲名字, dn Discord 名字}
//   tp:<語言>:<編號> → 整句提案 {lang, file, key, src, base, items:[{id, text, by, name, t, votes}], status, done}
//   tq:<語言>:<編號> → 術語投票 {lang, term, note, by, name, t, options:[…], status, decided}
//   兩種都把「列表要看的東西」放 metadata，列表只要 list 不必一筆一筆讀。
//
// 語料本身不放 KV：GitHub Action 每次語料有變動就重建一份精簡索引（tools/build-translate-index.py），
// 推到 translate-index 分支；這裡代讀、快取，網頁照它搜尋。

export const TR_LANGS = ['zh_tw', 'zh_cn', 'ja_jp', 'ko_kr', 'ru_ru', 'es_es', 'tr_tr'];
const TR_INDEX = 'https://raw.githubusercontent.com/' + 'LyuChaCha/WynnChaYuan' + '/translate-index/';
const TR_TEXT_MAX = 2000;
const TR_BODY = 16384;
const TR_FILE = /^(?:[a-z0-9-]+\/)?[a-z0-9_-]+\.json$/;
const TR_KEY = /^tl:(?:u:[0-9a-f]{32}|d:\d{17,20}|n:[a-z0-9_]{1,16})$/;
const TR_ID = /^[0-9a-f]{12,24}$/;

/**
 * 提案的譯文跟原文的佔位符對不對得上。跟 tools/validate.py 同一套規矩的精簡版，
 * 擋在送出的當下，不必等 PR 的檢查才紅。回傳問題代碼，沒問題回 null。
 *
 * @param base 現在的譯文（顏色標記只有譯文有，原文沒有，所以拿它比）
 */
export function placeholderProblem(src, text, base) {
  if (typeof text !== 'string' || !text.trim()) return 'empty';
  if (text.length > TR_TEXT_MAX) return 'too long';
  // {#}、{p}、{u} 沒有編號，照順序填：順序要跟原文一樣。{u}（玩家名字）可以少、不能多。
  const seq = (s) => String(s).match(/\{[#pu]\}/g) || [];
  const want = seq(src);
  const got = seq(text);
  const noName = (list) => list.filter((t) => t !== '{u}').join('');
  if (noName(want) !== noName(got)) return 'placeholders';
  let at = 0;
  for (const t of got) {
    while (at < want.length && want[at] !== t) at++;
    if (at === want.length) return 'placeholders';
    at++;
  }
  const nums = (s) => String(s).match(/\{~\d*\}/g) || [];
  if (nums(src).length !== nums(text).length) return 'numbers';
  const named = nums(text).filter((n) => n !== '{~}');
  if (named.length && named.length !== nums(text).length) return 'numbering';
  if (named.length) {
    const want = named.map((n) => Number(n.slice(2, -1))).sort((a, b) => a - b);
    if (want.some((n, i) => n !== i + 1)) return 'numbering';
  }
  const colours = (s) => (String(s).match(/\{c\d+\}|\{\/\}/g) || []).sort().join(',');
  if (base && base.trim() && colours(base) !== colours(text)) return 'colours';
  if (String(src).split('\n').length !== text.split('\n').length) return 'lines';
  return null;
}

/**
 * 改語料裡的一句：扁平檔照原文找那一行換值；任務檔照條目編號找那一條的 dst。
 * 逐行改，不重 dump（整檔重寫會讓 git 把整個檔當成改過）。找不到回 null。
 */
export function replaceTranslation(text, key, src, value) {
  const nl = text.includes('\r\n') ? '\r\n' : '\n';
  const lines = text.split(nl);
  const encoded = JSON.stringify(value);
  if (key == null) {
    const head = JSON.stringify(src);
    for (let i = 0; i < lines.length; i++) {
      const m = /^(\s*)("(?:[^"\\]|\\.)*")(\s*:\s*)("(?:[^"\\]|\\.)*")(,?\s*)$/.exec(lines[i]);
      if (m && m[2] === head) {
        lines[i] = m[1] + m[2] + m[3] + encoded + m[5];
        return lines.join(nl);
      }
    }
    return null;
  }
  const opener = JSON.stringify(key) + ': {';
  let inside = false;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (!inside) {
      if (line === opener) inside = true;
      continue;
    }
    if (line.startsWith('}')) return null;
    const m = /^(\s*"dst":\s*)("(?:[^"\\]|\\.)*")(,?\s*)$/.exec(lines[i]);
    if (m) {
      lines[i] = m[1] + encoded + m[3];
      return lines.join(nl);
    }
  }
  return null;
}

/** 投票與提案用的身分。綁了 Discord 的遊戲帳號只算一個人，不管從哪一邊登入。 */
async function trIdentity(request, env, now) {
  const user = await who(request, env, now);
  if (user) {
    const discord = user.discord || await env.CORPUS.get('l:' + user.uuid);
    return { id: 'u:' + user.uuid, name: user.name, uuid: user.uuid, discord };
  }
  const session = await discordSession(request, env, now);
  if (!session) return null;
  const linked = UUID.test(session.account.u || '') ? session.account.u : null;
  return { id: linked ? 'u:' + linked : 'd:' + session.id, name: session.account.m || session.account.n || session.id,
    uuid: linked, discord: session.id };
}

/** 這個人是哪幾個語言的譯者、能不能定案。照名字加的第一次對上就改記成 UUID（跟管理員名單一樣）。 */
async function trRoles(request, env, now, me) {
  const langs = new Set();
  const read = async (key) => {
    const record = parseRecord(await env.CORPUS.get(key));
    (Array.isArray(record.langs) ? record.langs : []).forEach((l) => TR_LANGS.includes(l) && langs.add(l));
    return record;
  };
  if (me.uuid) await read('tl:u:' + me.uuid);
  if (me.discord) await read('tl:d:' + me.discord);
  if (me.uuid && NAME.test(me.name || '')) {
    const byName = await env.CORPUS.get('tl:n:' + me.name.toLowerCase());
    if (byName) {
      const record = parseRecord(byName);
      (record.langs || []).forEach((l) => TR_LANGS.includes(l) && langs.add(l));
      const kept = parseRecord(await env.CORPUS.get('tl:u:' + me.uuid));
      const merged = { ...record, ...kept, langs: [...new Set([...(kept.langs || []), ...(record.langs || [])])], m: me.name };
      await env.CORPUS.put('tl:u:' + me.uuid, JSON.stringify(merged));
      await env.CORPUS.delete('tl:n:' + me.name.toLowerCase());
    }
  }
  const mine = await authority(request, env, now);
  return { langs, decide: Boolean(mine), by: mine ? mine.by : me.name };
}

async function trWho(request, env, now) {
  const me = await trIdentity(request, env, now);
  if (!me) return null;
  return { ...me, ...(await trRoles(request, env, now, me)) };
}

const canWrite = (me, lang) => Boolean(me) && (me.decide || me.langs.has(lang));

async function trId(text) {
  return hex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))).slice(0, 16);
}

function trMeta(record) {
  const votes = Math.max(0, ...record.items.map((it) => it.votes.length));
  return { f: record.file, s: String(record.src).slice(0, 120), n: record.items.length, v: votes,
    st: record.status, t: record.updated || record.t };
}

function pollMeta(record) {
  const votes = record.options.reduce((sum, o) => sum + o.votes.length, 0);
  return { s: String(record.term).slice(0, 80), n: record.options.length, v: votes, st: record.status, t: record.updated || record.t };
}

async function trList(env, prefix) {
  const out = [];
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix, limit: 1000, cursor });
    for (const k of page.keys) out.push({ id: k.name.slice(prefix.length), ...(k.metadata || {}) });
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  return out.sort((a, b) => (b.t || 0) - (a.t || 0));
}

const trLang = (value) => (TR_LANGS.includes(value) ? value : null);

/** 我是誰、是哪幾個語言的譯者、能不能定案。沒登入也回（網頁照這個決定按鈕要不要出現）。 */
async function handleTrMe(request, env, now) {
  const me = await trWho(request, env, now);
  return json(me ? { signedIn: true, id: me.id, name: me.name, langs: [...me.langs], decide: me.decide }
    : { signedIn: false, langs: [], decide: false }, 200, { 'cache-control': 'no-store' });
}

/** 某個語言的語料索引：從 translate-index 分支代讀，快取十分鐘。 */
async function handleTrIndex(request, env, lang, ctx) {
  if (!trLang(lang)) return no('bad lang', 404);
  const cache = typeof caches !== 'undefined' ? caches.default : null;
  const cacheKey = new Request('https://wynnchayuan.com/tr-index/' + lang);
  if (cache) {
    const hit = await cache.match(cacheKey);
    if (hit) return hit;
  }
  let upstream;
  try {
    upstream = await fetch(TR_INDEX + lang + '.json', { cf: { cacheTtl: 600 } });
  } catch (e) {
    return no('index unreachable', 502);
  }
  if (upstream.status !== 200) return no('index ' + upstream.status, 502);
  const response = new Response(upstream.body, {
    headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'public, max-age=600' },
  });
  if (cache) {
    const copy = response.clone();
    if (ctx && ctx.waitUntil) ctx.waitUntil(cache.put(cacheKey, copy));
    else await cache.put(cacheKey, copy);
  }
  return response;
}

async function handleTrProps(request, env) {
  const url = new URL(request.url);
  const lang = trLang(url.searchParams.get('lang'));
  if (!lang) return no('bad lang');
  return json({ props: await trList(env, 'tp:' + lang + ':') }, 200, { 'cache-control': 'no-store' });
}

async function handleTrProp(request, env) {
  const url = new URL(request.url);
  const lang = trLang(url.searchParams.get('lang'));
  const id = url.searchParams.get('id') || '';
  if (!lang || !TR_ID.test(id)) return no('bad request');
  const raw = await env.CORPUS.get('tp:' + lang + ':' + id);
  return raw ? json(JSON.parse(raw), 200, { 'cache-control': 'no-store' }) : no('not found', 404);
}

/** 提一個譯法。同一句已經有人提過同樣的字，就當成投那一票。 */
async function handleTrPropose(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me) return no('login first', 401);
  const body = await readJson(request, TR_BODY);
  if (!body) return no('bad request');
  const lang = trLang(body.lang);
  if (!lang) return no('bad lang');
  if (!canWrite(me, lang)) return no('not a translator', 403);
  const file = String(body.file || '');
  const key = body.key == null ? null : String(body.key);
  const src = String(body.src || '');
  const base = typeof body.base === 'string' ? body.base : '';
  const text = typeof body.text === 'string' ? body.text.replace(/\r\n/g, '\n') : '';
  if (!TR_FILE.test(file) || !src || src.length > 4000 || (key != null && key.length > 200)) return no('bad request');
  const problem = placeholderProblem(src, text, base);
  if (problem) return no(problem);
  const id = await trId(file + '\n' + (key == null ? src : key));
  const storeKey = 'tp:' + lang + ':' + id;
  const record = parseRecord(await env.CORPUS.get(storeKey));
  if (!record.items) Object.assign(record, { lang, file, key, src, base, items: [], status: 'open', t: now });
  if (record.status !== 'open') {
    record.status = 'open';
    delete record.done;
  }
  for (const it of record.items) it.votes = it.votes.filter((v) => v !== me.id);
  const same = record.items.find((it) => it.text === text);
  if (same) {
    same.votes.push(me.id);
  } else {
    if (record.items.length >= 20) return no('too many');
    record.items.push({ id: (await trId(text + now)).slice(0, 12), text, by: me.id, name: me.name, t: now, votes: [me.id] });
  }
  record.updated = now;
  await env.CORPUS.put(storeKey, JSON.stringify(record), { metadata: trMeta(record) });
  return json({ ok: true, id, prop: record });
}

/** 投一票；再按一次收回。一句話一個人只能投一個譯法。 */
async function handleTrVote(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me) return no('login first', 401);
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang || !TR_ID.test(String(body.id || ''))) return no('bad request');
  if (!canWrite(me, lang)) return no('not a translator', 403);
  const storeKey = 'tp:' + lang + ':' + body.id;
  const raw = await env.CORPUS.get(storeKey);
  if (!raw) return no('not found', 404);
  const record = JSON.parse(raw);
  if (record.status !== 'open') return no('closed', 409);
  const target = record.items.find((it) => it.id === body.item);
  if (!target) return no('not found', 404);
  const had = target.votes.includes(me.id);
  for (const it of record.items) it.votes = it.votes.filter((v) => v !== me.id);
  if (!had) target.votes.push(me.id);
  record.updated = now;
  await env.CORPUS.put(storeKey, JSON.stringify(record), { metadata: trMeta(record) });
  return json({ ok: true, prop: record });
}

/**
 * 定案：把這個譯法寫進語料。同一個語言同一天的放進同一個分支與 PR（translate/<語言>-<日期>），
 * 不然一天幾十個 PR 會塞爆合併的佇列。任務檔的句子連同合併檔一起改，PR 的檢查才不會紅。
 */
async function handleTrAccept(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me || !me.decide) return no('forbidden', 403);
  if (!env.GITHUB_TOKEN) return no('github not configured', 503);
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang || !TR_ID.test(String(body.id || ''))) return no('bad request');
  const storeKey = 'tp:' + lang + ':' + body.id;
  const raw = await env.CORPUS.get(storeKey);
  if (!raw) return no('not found', 404);
  const record = JSON.parse(raw);
  const item = record.items.find((it) => it.id === body.item);
  if (!item) return no('not found', 404);
  const github = githubClient(env);
  try {
    const day = new Date(now).toISOString().slice(0, 10).replace(/-/g, '');
    const branch = 'translate/' + lang.replace('_', '-') + '-' + day;
    let created = false;
    const ref = await github('/git/ref/heads/' + branch);
    if (ref.status === 404) {
      const head = await github('/git/ref/heads/main');
      if (head.status !== 200) return no('github ' + head.status, 502);
      const made = await github('/git/refs', {
        method: 'POST', body: JSON.stringify({ ref: 'refs/heads/' + branch, sha: (await head.json()).object.sha }),
      });
      if (made.status !== 201) return no('github ' + made.status, 502);
      created = true;
    } else if (ref.status !== 200) {
      return no('github ' + ref.status, 502);
    }
    const root = 'src/main/resources/assets/wynnchayuan/translations/' + lang + '/';
    const targets = [record.file];
    if (record.key != null && record.file.startsWith('quest/')) targets.push('quest-dialogue.json');
    if (record.key != null && record.file.startsWith('secret/')) targets.push('secret-dialogue.json');
    const short = record.src.replace(/\s+/g, ' ').slice(0, 50);
    for (const name of targets) {
      const file = await github('/contents/' + root + name + '?ref=' + encodeURIComponent(branch));
      if (file.status !== 200) return no('github ' + file.status, 502);
      const current = await file.json();
      const changed = replaceTranslation(base64ToUtf8(current.content), record.key, record.src, item.text);
      if (changed == null) return no('line not found', 409);
      const put = await github('/contents/' + root + name, {
        method: 'PUT',
        body: JSON.stringify({ message: '譯文（' + lang + '）：' + short, content: utf8ToBase64(changed),
          sha: current.sha, branch }),
      });
      if (put.status !== 200 && put.status !== 201) return no('github ' + put.status, 502);
    }
    let url = '';
    if (created) {
      const pull = await github('/pulls', {
        method: 'POST',
        body: JSON.stringify({
          title: '協作翻譯：' + lang + '（' + day + '）',
          head: branch, base: 'main',
          body: '網站「協作翻譯」頁定案的譯文。同一個語言同一天定案的都會加進這個 PR。\n\n'
            + '第一條由 ' + me.by + ' 定案。每一條的原文、提案人與票數記在網站上。',
        }),
      });
      if (pull.status !== 201) return no('github ' + pull.status, 502);
      url = (await pull.json()).html_url;
    } else {
      const open = await github('/pulls?head=' + encodeURIComponent('LyuChaCha:' + branch) + '&state=open');
      const list = open.status === 200 ? await open.json() : [];
      url = Array.isArray(list) && list[0] ? list[0].html_url : '';
    }
    record.status = 'accepted';
    record.done = { item: item.id, by: me.by, t: now, pr: url };
    record.updated = now;
    await env.CORPUS.put(storeKey, JSON.stringify(record), { metadata: trMeta(record) });
    return json({ ok: true, url, prop: record });
  } catch (e) {
    return no('github unreachable', 502);
  }
}

async function handleTrReject(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me || !me.decide) return no('forbidden', 403);
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang || !TR_ID.test(String(body.id || ''))) return no('bad request');
  const storeKey = 'tp:' + lang + ':' + body.id;
  const raw = await env.CORPUS.get(storeKey);
  if (!raw) return no('not found', 404);
  const record = JSON.parse(raw);
  record.status = 'rejected';
  record.done = { by: me.by, t: now };
  record.updated = now;
  await env.CORPUS.put(storeKey, JSON.stringify(record), { metadata: trMeta(record) });
  return json({ ok: true, prop: record });
}

async function handleTrPolls(request, env) {
  const lang = trLang(new URL(request.url).searchParams.get('lang'));
  if (!lang) return no('bad lang');
  return json({ polls: await trList(env, 'tq:' + lang + ':') }, 200, { 'cache-control': 'no-store' });
}

async function handleTrPoll(request, env) {
  const url = new URL(request.url);
  const lang = trLang(url.searchParams.get('lang'));
  const id = url.searchParams.get('id') || '';
  if (!lang || !TR_ID.test(id)) return no('bad request');
  const raw = await env.CORPUS.get('tq:' + lang + ':' + id);
  return raw ? json(JSON.parse(raw), 200, { 'cache-control': 'no-store' }) : no('not found', 404);
}

const cleanOption = (text) => (typeof text === 'string' ? text.trim().replace(/\s+/g, ' ') : '');

/** 開一個術語投票：英文、說明、幾個候選（可以之後再加）。 */
async function handleTrPollOpen(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me) return no('login first', 401);
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang) return no('bad lang');
  if (!canWrite(me, lang)) return no('not a translator', 403);
  const term = cleanOption(body.term);
  const note = typeof body.note === 'string' ? body.note.trim().slice(0, 500) : '';
  if (!term || term.length > 80) return no('bad term');
  const options = [...new Set((Array.isArray(body.options) ? body.options : []).map(cleanOption).filter(Boolean))];
  if (options.some((o) => o.length > 80) || options.length > 10) return no('bad option');
  const id = (await trId(lang + term + now)).slice(0, 12);
  const record = { lang, term, note, by: me.id, name: me.name, t: now, status: 'open', options: [] };
  for (const text of options) {
    record.options.push({ id: (await trId(text + now)).slice(0, 12), text, by: me.id, name: me.name, t: now, votes: [] });
  }
  await env.CORPUS.put('tq:' + lang + ':' + id, JSON.stringify(record), { metadata: pollMeta(record) });
  return json({ ok: true, id, poll: record });
}

async function pollFor(request, env, now, needDecide) {
  const me = await trWho(request, env, now);
  if (!me) return { error: no('login first', 401) };
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang || !TR_ID.test(String(body.id || ''))) return { error: no('bad request') };
  if (needDecide ? !me.decide : !canWrite(me, lang)) return { error: no(needDecide ? 'forbidden' : 'not a translator', 403) };
  const storeKey = 'tq:' + lang + ':' + body.id;
  const raw = await env.CORPUS.get(storeKey);
  if (!raw) return { error: no('not found', 404) };
  return { me, body, storeKey, record: JSON.parse(raw) };
}

async function savePoll(env, got, now) {
  got.record.updated = now;
  await env.CORPUS.put(got.storeKey, JSON.stringify(got.record), { metadata: pollMeta(got.record) });
  return json({ ok: true, poll: got.record });
}

/** 加一個候選。已經有一樣的字就當成投它。 */
async function handleTrPollOption(request, env, now) {
  const got = await pollFor(request, env, now, false);
  if (got.error) return got.error;
  if (got.record.status !== 'open') return no('closed', 409);
  const text = cleanOption(got.body.text);
  if (!text || text.length > 80) return no('bad option');
  for (const o of got.record.options) o.votes = o.votes.filter((v) => v !== got.me.id);
  const same = got.record.options.find((o) => o.text === text);
  if (same) same.votes.push(got.me.id);
  else {
    if (got.record.options.length >= 12) return no('too many');
    got.record.options.push({ id: (await trId(text + now)).slice(0, 12), text, by: got.me.id, name: got.me.name, t: now,
      votes: [got.me.id] });
  }
  return savePoll(env, got, now);
}

/** 投票；一個人一票，投別的就移過去，再按一次收回。 */
async function handleTrPollVote(request, env, now) {
  const got = await pollFor(request, env, now, false);
  if (got.error) return got.error;
  if (got.record.status !== 'open') return no('closed', 409);
  const target = got.record.options.find((o) => o.id === got.body.option);
  if (!target) return no('not found', 404);
  const had = target.votes.includes(got.me.id);
  for (const o of got.record.options) o.votes = o.votes.filter((v) => v !== got.me.id);
  if (!had) target.votes.push(got.me.id);
  return savePoll(env, got, now);
}

/** 定案：記下採用哪一個。套到整個語料由維護的人做。 */
async function handleTrPollDecide(request, env, now) {
  const got = await pollFor(request, env, now, true);
  if (got.error) return got.error;
  const target = got.record.options.find((o) => o.id === got.body.option);
  if (!target) return no('not found', 404);
  got.record.status = 'decided';
  got.record.decided = { option: target.id, text: target.text, by: got.me.by, t: now };
  return savePoll(env, got, now);
}

async function handleTrPollReopen(request, env, now) {
  const got = await pollFor(request, env, now, true);
  if (got.error) return got.error;
  got.record.status = 'open';
  delete got.record.decided;
  return savePoll(env, got, now);
}

// ---- 譯者名單（管理頁）

async function handleTranslators(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const out = [];
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'tl:', limit: 1000, cursor });
    for (const k of page.keys) {
      const record = parseRecord(await env.CORPUS.get(k.name));
      const key = k.name.slice(3);
      out.push({ key, kind: key[0], id: key.slice(2), langs: record.langs || [], name: record.m || null,
        discord: record.dn || null, note: record.n || '', t: record.t || null, by: record.by || null });
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  out.sort((a, b) => (b.t || 0) - (a.t || 0));
  return json({ translators: out }, 200, { 'cache-control': 'no-store' });
}

/** 加一位譯者，或改他負責的語言（同一個人再送一次就是覆寫）。 */
async function handleTranslatorAdd(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  const body = await readJson(request, TR_BODY);
  if (!body) return no('bad request');
  const key = staffKey(body.who);
  if (!key) return no('bad who');
  const langs = [...new Set((Array.isArray(body.langs) ? body.langs : []).filter((l) => TR_LANGS.includes(l)))];
  if (!langs.length) return no('bad langs');
  const note = typeof body.note === 'string' ? body.note.trim() : '';
  if (note.length > 60) return no('bad note');
  const record = { langs, n: note, t: now, by };
  if (key.startsWith('n:')) record.m = String(body.who).trim();
  const linked = key.startsWith('d:') ? key.slice(2) : key.startsWith('u:') ? await env.CORPUS.get('l:' + key.slice(2)) : null;
  const known = linked ? await account(env, linked) : null;
  if (known) {
    if (known.n) record.dn = known.n;
    if (known.m && (key.startsWith('d:') || known.u === key.slice(2))) record.m = known.m;
  }
  if (key.startsWith('u:') && !record.m) {
    const cape = await env.CORPUS.getWithMetadata('cape:' + await publicId(key.slice(2)));
    if (cape && cape.metadata && NAME.test(cape.metadata.n || '')) record.m = cape.metadata.n;
  }
  await env.CORPUS.put('tl:' + key, JSON.stringify(record));
  return json({ ok: true, key });
}

async function handleTranslatorRemove(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  const key = body && typeof body.key === 'string' ? 'tl:' + body.key : '';
  if (!TR_KEY.test(key)) return no('bad request');
  await env.CORPUS.delete(key);
  return json({ ok: true });
}

function githubClient(env) {
  return (path, init = {}) => fetch('https://api.github.com/repos/' + REPO + path, {
    ...init,
    headers: {
      authorization: 'Bearer ' + env.GITHUB_TOKEN,
      accept: 'application/vnd.github+json',
      'user-agent': 'wynnchayuan-site',
      'content-type': 'application/json',
    },
  });
}

// ---------------------------------------------------------------- 回饋（/feedback 與機器人的 /回饋）
//
// 使用者 2026-10-17：網站加一頁回饋，分成譯文建議、內容建議、網站建議、功能建議、其他；
// 要用 Discord 登入（記代號與顯示名稱），一個人一小時只能送幾則；管理頁看得到、標已處理、
// 重新開啟、刪掉垃圾訊息。譯文建議自動在 GitHub 開 Issue（內文只放顯示名稱，不放代號），
// GitHub 失敗也不能把回饋弄丟。機器人的 /回饋 進同一個地方。
//
// KV
//   fb:<編號> → {id, cat, msg, where, lang, uid Discord 代號, name 顯示名稱, via web|bot, t, st open|done,
//               done {by, t}, issue {url, n}, issueError}；列表要看的放 metadata（c, st, t, n, s 摘要, i, e）
//   fbr:<Discord 代號> → 最近一小時送出的時間（限流用，一小時後自己消失）
//   標成已處理的留 FEEDBACK_KEEP_DAYS 天就讓 KV 自己刪掉；還沒處理的留到處理或刪除為止。

export const FEEDBACK_CATS = ['translation', 'content', 'website', 'feature', 'other'];
export const FEEDBACK_MAX = 2000;
const FEEDBACK_MIN = 3;
const FEEDBACK_WHERE_MAX = 200;
export const FEEDBACK_PER_HOUR = 5;
export const FEEDBACK_KEEP_DAYS = 180;
const FEEDBACK_ID = /^[0-9a-f]{16}$/;
const FEEDBACK_CAT_ZH = { translation: '譯文建議', content: '內容建議', website: '網站建議', feature: '功能建議', other: '其他' };
/** 開 Issue 時貼的標籤：跟倉庫的「提交翻譯」範本（.github/ISSUE_TEMPLATE/translation.md）同一個。 */
const FEEDBACK_LABEL = 'translation';

/** 送來的回饋對不對：回 {entry}，不對回 {why}。純函式，網頁與機器人共用。 */
export function feedbackCheck(input) {
  if (!input || typeof input !== 'object') return { why: 'bad request' };
  const cat = String(input.cat || '');
  if (!FEEDBACK_CATS.includes(cat)) return { why: 'bad category' };
  // 控制字元拿掉（換行與 tab 留著），換行統一成 \n
  const tidy = (s) => String(s).replace(/\r\n?/g, '\n').replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/g, '');
  const msg = typeof input.msg === 'string' ? tidy(input.msg).trim() : '';
  if (msg.length < FEEDBACK_MIN) return { why: 'too short' };
  if (msg.length > FEEDBACK_MAX) return { why: 'too long' };
  const where = typeof input.where === 'string' ? tidy(input.where).replace(/\s+/g, ' ').trim() : '';
  if (where.length > FEEDBACK_WHERE_MAX) return { why: 'where too long' };
  const lang = TR_LANGS.includes(input.lang) ? input.lang : null;
  return { entry: { cat, msg, where, lang } };
}

/**
 * 送回饋的人：{id Discord 代號, name 顯示名稱}。網站帳號（Discord）登入的直接認；從遊戲裡開的網頁
 * 要綁了 Discord 才算（回 {error: 'discord first'}）；都沒有回 null。
 */
async function feedbackPerson(request, env, now) {
  const session = await discordSession(request, env, now);
  if (session) return { id: session.id, name: session.account.n || session.id };
  const user = await who(request, env, now);
  if (!user) return null;
  const discord = user.discord || await env.CORPUS.get('l:' + user.uuid);
  const found = discord ? await account(env, discord) : null;
  return found ? { id: discord, name: found.n || discord } : { error: 'discord first' };
}

/** 一個人一小時最多 FEEDBACK_PER_HOUR 則。滿了回 {until 哪時候可以再送}，沒滿回 {log 這一小時送過的時間}。 */
async function feedbackGate(env, id, now) {
  let log = [];
  try {
    const raw = JSON.parse(await env.CORPUS.get('fbr:' + id) || '[]');
    if (Array.isArray(raw)) log = raw.filter((t) => typeof t === 'number' && now - t < 3600000).sort((a, b) => a - b);
  } catch (e) {
    log = [];
  }
  return log.length >= FEEDBACK_PER_HOUR ? { until: log[0] + 3600000 } : { log };
}

function feedbackMeta(record) {
  const meta = { c: record.cat, st: record.st, t: record.t, n: clip(record.name, 40),
    s: clip(record.msg.replace(/\s+/g, ' '), 100) };
  if (record.issue) meta.i = record.issue.url;
  if (record.issueError) meta.e = record.issueError;
  return meta;
}

async function saveFeedback(env, record) {
  const options = { metadata: feedbackMeta(record) };
  if (record.st === 'done') options.expirationTtl = FEEDBACK_KEEP_DAYS * 86400;
  await env.CORPUS.put('fb:' + record.id, JSON.stringify(record), options);
}

/** Markdown 的程式碼區塊：圍欄比內容裡最長的一串反引號多一個，使用者寫什麼都關不掉它（也不會 @ 到人）。 */
function fenced(text) {
  const runs = String(text).match(/`+/g) || [];
  const bar = '`'.repeat(Math.max(3, ...runs.map((run) => run.length + 1)));
  return bar + '\n' + text + '\n' + bar;
}

/** 譯文建議要開的 Issue：標題是分類加一小段內容；內文只放顯示名稱，不放 Discord 代號。純函式，測試直接餵。 */
export function feedbackIssue(record) {
  const kind = FEEDBACK_CAT_ZH[record.cat] || record.cat;
  const name = String(record.name || '').replace(/[`\r\n]/g, '').trim().slice(0, 64) || '—';
  const lines = [
    '從 ' + (record.via === 'bot' ? 'Discord 機器人的 /回饋' : 'wynnchayuan.com/feedback') + ' 送來的' + kind + '。', '',
    '**內容**', '', fenced(record.msg), '',
  ];
  if (record.where) lines.push('**位置**', '', fenced(record.where), '');
  lines.push('**語言**：' + (record.lang ? LANG_LABEL[record.lang] + '（`' + record.lang + '`）' : '沒有指定'));
  lines.push('**送出的人**：`' + name + '`（Discord 顯示名稱）');
  lines.push('', '網站上的編號 `' + record.id + '`；處理完請到網站的管理頁標成已處理。');
  return { title: kind + '：' + clip(record.msg.replace(/\s+/g, ' ').trim(), 60), body: lines.join('\n'), labels: [FEEDBACK_LABEL] };
}

/** 開 Issue。成功回 {url, n}，失敗回 {error}（回饋本身已經存好了，失敗只是記下來給管理頁看）。 */
async function openFeedbackIssue(env, record) {
  if (!env.GITHUB_TOKEN) return { error: 'github not configured' };
  try {
    const res = await githubClient(env)('/issues', { method: 'POST', body: JSON.stringify(feedbackIssue(record)) });
    if (res.status !== 201) return { error: 'github ' + res.status };
    const made = await res.json();
    return { url: String(made.html_url || ''), n: Number(made.number) || 0 };
  } catch (e) {
    return { error: 'github unreachable' };
  }
}

async function fileFeedbackIssue(env, record) {
  const made = await openFeedbackIssue(env, record);
  if (made.error) {
    record.issueError = made.error;
  } else {
    record.issue = { url: made.url, n: made.n };
    delete record.issueError;
  }
  await saveFeedback(env, record);
}

/** 網頁與機器人共用：檢查、限流、存下來；譯文建議再開 Issue。回 {record} 或 {why, status, until}。 */
async function submitFeedback(env, person, input, via, now) {
  const checked = feedbackCheck(input);
  if (checked.why) return { why: checked.why, status: 400 };
  const gate = await feedbackGate(env, person.id, now);
  if (gate.until) return { why: 'slow down', status: 429, until: gate.until };
  const id = hex(crypto.getRandomValues(new Uint8Array(8)));
  const record = { id, ...checked.entry, uid: person.id, name: String(person.name).slice(0, 64), via, t: now, st: 'open' };
  // 先存，再碰 GitHub：GitHub 怎麼失敗都不會把回饋弄丟
  await saveFeedback(env, record);
  await env.CORPUS.put('fbr:' + person.id, JSON.stringify([...gate.log, now]), { expirationTtl: 3600 });
  if (record.cat === 'translation') await fileFeedbackIssue(env, record);
  return { record };
}

async function handleFeedback(request, env, now) {
  const person = await feedbackPerson(request, env, now);
  if (!person) return no('login first', 401);
  if (person.error) return no(person.error, 401);
  const body = await readJson(request, TR_BODY);
  if (!body) return no('bad request');
  const got = await submitFeedback(env, person, body, 'web', now);
  if (got.why) return json({ ok: false, why: got.why, until: got.until }, got.status);
  const { record } = got;
  return json({ ok: true, id: record.id, issue: record.issue ? record.issue.url : null,
    issueFailed: Boolean(record.issueError) });
}

/** 管理頁的回饋列表：新的在前，可以照分類與狀態篩；一次最多 200 則。 */
async function handleFeedbackList(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const url = new URL(request.url);
  const cat = url.searchParams.get('cat') || '';
  const st = url.searchParams.get('st') || '';
  const all = await trList(env, 'fb:');
  const counts = { open: 0, done: 0 };
  for (const meta of all) counts[meta.st === 'done' ? 'done' : 'open']++;
  const picked = all.filter((meta) => (!cat || meta.c === cat) && (!st || meta.st === st));
  const items = [];
  for (const meta of picked.slice(0, 200)) {
    const record = parseRecord(await env.CORPUS.get('fb:' + meta.id));
    if (record.id) items.push(record);
  }
  return json({ items, total: picked.length, counts }, 200, { 'cache-control': 'no-store' });
}

async function feedbackFor(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return { error: no('forbidden', 403) };
  const body = await readJson(request);
  const id = body ? String(body.id || '') : '';
  if (!FEEDBACK_ID.test(id)) return { error: no('bad request') };
  const raw = await env.CORPUS.get('fb:' + id);
  if (!raw) return { error: no('not found', 404) };
  return { by, body, record: parseRecord(raw) };
}

/** 標成已處理（done）或重新開啟（open）。 */
async function handleFeedbackStatus(request, env, now) {
  const got = await feedbackFor(request, env, now);
  if (got.error) return got.error;
  if (!['open', 'done'].includes(got.body.st)) return no('bad request');
  got.record.st = got.body.st;
  if (got.body.st === 'done') got.record.done = { by: got.by, t: now };
  else delete got.record.done;
  await saveFeedback(env, got.record);
  return json({ ok: true, item: got.record });
}

/** 刪掉一則（垃圾訊息）。已經開了的 Issue 留在 GitHub 上，要關掉到那邊關。 */
async function handleFeedbackDelete(request, env, now) {
  const got = await feedbackFor(request, env, now);
  if (got.error) return got.error;
  await env.CORPUS.delete('fb:' + got.record.id);
  return json({ ok: true });
}

/** 譯文建議的 Issue 上次沒開成：再開一次。 */
async function handleFeedbackIssue(request, env, now) {
  const got = await feedbackFor(request, env, now);
  if (got.error) return got.error;
  if (got.record.cat !== 'translation') return no('not a translation', 409);
  if (got.record.issue) return json({ ok: true, item: got.record });
  await fileFeedbackIssue(env, got.record);
  return got.record.issue ? json({ ok: true, item: got.record })
    : json({ ok: false, why: got.record.issueError, item: got.record }, 502);
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

// ---------------------------------------------------------------- Discord 機器人（斜線指令）
//
// 使用者 2026-10-16：「worker 可以順便架 Discord bot 嗎」「看當前使用者披風、網站狀態，或查看譯文」。
// 用 Discord 的 Interactions：指令用 HTTP 送到 /api/discord/interactions，這裡回訊息，不需要常駐的機器人主機。
// 只回應斜線指令，讀不到聊天內容。
//
// 要設定的：wrangler.toml 的 DISCORD_PUBLIC_KEY（驗簽名用，公開的值）、secret DISCORD_BOT_TOKEN
// （註冊指令用，使用者自己 wrangler secret put）、Developer Portal 的 Interactions Endpoint URL。
// 指令在管理頁按「註冊 Discord 指令」送出去（POST /api/admin/discord/register，只有開發者）。

const DISCORD_V10 = 'https://discord.com/api/v10';
const SITE = 'https://wynnchayuan.com';

const BOT_TEXT = {
  zh: {
    status: '網站狀態', users: '裝了模組的玩家', capes: '穿著披風', pending: '待審核的披風',
    props: '協作翻譯：投票中的提案', polls: '協作翻譯：投票中的術語', none: '沒有',
    capeOf: '{name} 的披風', noCape: '找不到 {name} 的披風（沒上傳過，或現在沒穿）',
    privateCape: '{name} 的披風設成不公開。', pendingCape: '{name} 的披風還在審核中。', badName: '這不像 Minecraft 的名字。',
    kind: '格式', since: '換上的時間', gallery: '披風一覽',
    found: '「{q}」找到 {n} 條，列出前 {shown} 條', notFound: '「{q}」在 {lang} 的語料裡找不到。', tooShort: '關鍵字至少兩個字。',
    untranslated: '（還沒翻）', indexFail: '語料讀不到，晚點再試。', more: '在網站上看更多', unknown: '不認得這個指令。',
    fbThanks: '收到了，謝謝你的回饋！團隊會在網站的管理頁看到它。', fbIssue: '譯文建議已經在 GitHub 開了 Issue：{url}',
    fbIssueFail: '（這次沒能在 GitHub 開 Issue，回饋已經存好，管理員會再補開。）',
    fbWhy: { 'bad category': '分類不對。', 'too short': '內容太短了，多寫幾個字吧。', 'too long': '內容太長了（最多 2000 字）。',
      'where too long': '「位置」太長了（最多 200 字）。', 'slow down': '一小時內最多送 {n} 則，請 {t}再試。', 'no user': '認不出你是誰，請再試一次。' },
  },
  cn: {
    status: '网站状态', users: '装了模组的玩家', capes: '穿着披风', pending: '待审核的披风',
    props: '协作翻译：投票中的提案', polls: '协作翻译：投票中的术语', none: '没有',
    capeOf: '{name} 的披风', noCape: '找不到 {name} 的披风（没上传过，或现在没穿）',
    privateCape: '{name} 的披风设为不公开。', pendingCape: '{name} 的披风还在审核中。', badName: '这不像 Minecraft 的名字。',
    kind: '格式', since: '换上的时间', gallery: '披风一览',
    found: '「{q}」找到 {n} 条，列出前 {shown} 条', notFound: '「{q}」在 {lang} 的语料里找不到。', tooShort: '关键字至少两个字。',
    untranslated: '（还没翻）', indexFail: '语料读不到，稍后再试。', more: '在网站上看更多', unknown: '不认识这个指令。',
    fbThanks: '收到了，谢谢你的反馈！团队会在网站的管理页看到它。', fbIssue: '译文建议已经在 GitHub 开了 Issue：{url}',
    fbIssueFail: '（这次没能在 GitHub 开 Issue，反馈已经保存，管理员会再补开。）',
    fbWhy: { 'bad category': '分类不对。', 'too short': '内容太短了，多写几个字吧。', 'too long': '内容太长了（最多 2000 字）。',
      'where too long': '“位置”太长了（最多 200 字）。', 'slow down': '一小时内最多发送 {n} 条，请 {t}再试。', 'no user': '认不出你是谁，请再试一次。' },
  },
  en: {
    status: 'Website status', users: 'Players with the mod', capes: 'Wearing a cape', pending: 'Capes awaiting review',
    props: 'Translate: suggestions open for voting', polls: 'Translate: terms open for voting', none: 'none',
    capeOf: "{name}'s cape", noCape: 'No cape found for {name} (never uploaded, or not wearing one now).',
    privateCape: "{name}'s cape is not public.", pendingCape: "{name}'s cape is still awaiting review.",
    badName: 'That does not look like a Minecraft name.',
    kind: 'Format', since: 'Worn since', gallery: 'Cape gallery',
    found: '{n} lines match "{q}", showing the first {shown}', notFound: 'Nothing matches "{q}" in {lang}.',
    tooShort: 'Use at least two characters.', untranslated: '(not translated yet)', indexFail: 'Could not load the translations. Try again later.',
    more: 'See more on the website', unknown: 'Unknown command.',
    fbThanks: 'Got it, thanks for the feedback! The team will see it on the website.', fbIssue: 'Your translation suggestion is now a GitHub issue: {url}',
    fbIssueFail: '(Could not open the GitHub issue this time. Your feedback is saved and an admin will open it later.)',
    fbWhy: { 'bad category': 'Unknown category.', 'too short': 'That is too short. Please add a few more words.',
      'too long': 'That is too long (2000 characters at most).', 'where too long': '"Where" is too long (200 characters at most).',
      'slow down': 'You can send {n} per hour. Try again {t}.', 'no user': 'Could not tell who you are. Please try again.' },
  },
};
const LANG_LABEL = { zh_tw: '繁體中文', zh_cn: '简体中文', ja_jp: '日本語', ko_kr: '한국어', ru_ru: 'Русский', es_es: 'Español', tr_tr: 'Türkçe' };

/** Discord 的介面語言 → 回覆用哪一套字、預設查哪個語言的語料。 */
function botLocale(locale) {
  const l = String(locale || '');
  if (l === 'zh-CN') return { text: BOT_TEXT.cn, corpus: 'zh_cn' };
  if (l.startsWith('zh')) return { text: BOT_TEXT.zh, corpus: 'zh_tw' };
  const corpus = { ja: 'ja_jp', ko: 'ko_kr', ru: 'ru_ru', 'es-ES': 'es_es', 'es-419': 'es_es', tr: 'tr_tr' }[l] || 'zh_tw';
  return { text: BOT_TEXT.en, corpus };
}

const botFmt = (s, values) => String(s).replace(/\{(\w+)\}/g, (all, k) => (k in values ? String(values[k]) : all));
const clip = (s, n) => (String(s).length > n ? String(s).slice(0, n - 1) + '…' : String(s));

/** /回饋 的分類選項：值、英文、繁中、簡中。值跟網站的 FEEDBACK_CATS 一樣。 */
const FEEDBACK_CHOICES = [
  ['translation', 'Translation suggestion', '譯文建議', '译文建议'],
  ['content', 'Content suggestion', '內容建議', '内容建议'],
  ['website', 'Website suggestion', '網站建議', '网站建议'],
  ['feature', 'Feature suggestion', '功能建議', '功能建议'],
  ['other', 'Other', '其他', '其他'],
];

/** 指令的定義（註冊時原樣送出）。名稱用英文，繁簡中文另外給在地化的名字。 */
export const DISCORD_COMMANDS = [
  {
    name: 'status', type: 1,
    description: 'WynnChaYuan website status',
    name_localizations: { 'zh-TW': '狀態', 'zh-CN': '状态' },
    description_localizations: { 'zh-TW': '網站狀態：玩家、披風、待審核、協作翻譯', 'zh-CN': '网站状态：玩家、披风、待审核、协作翻译' },
  },
  {
    name: 'cape', type: 1,
    description: "Show a player's WynnChaYuan cape",
    name_localizations: { 'zh-TW': '披風', 'zh-CN': '披风' },
    description_localizations: { 'zh-TW': '看某位玩家現在的 WynnChaYuan 披風', 'zh-CN': '看某位玩家现在的 WynnChaYuan 披风' },
    options: [{
      type: 3, name: 'player', required: true, max_length: 16,
      description: 'Minecraft name',
      name_localizations: { 'zh-TW': '玩家', 'zh-CN': '玩家' },
      description_localizations: { 'zh-TW': 'Minecraft 名字', 'zh-CN': 'Minecraft 名字' },
    }],
  },
  {
    name: 'translate', type: 1,
    description: 'Search the WynnChaYuan translations',
    name_localizations: { 'zh-TW': '翻譯', 'zh-CN': '翻译' },
    description_localizations: { 'zh-TW': '搜尋 WynnChaYuan 的語料（原文或譯文）', 'zh-CN': '搜索 WynnChaYuan 的语料（原文或译文）' },
    options: [
      {
        type: 3, name: 'text', required: true, min_length: 2, max_length: 100,
        description: 'Words in the English source or the translation',
        name_localizations: { 'zh-TW': '關鍵字', 'zh-CN': '关键字' },
        description_localizations: { 'zh-TW': '英文原文或譯文裡的字', 'zh-CN': '英文原文或译文里的字' },
      },
      {
        type: 3, name: 'language', required: false,
        description: 'Which translation (default: follows your Discord language)',
        name_localizations: { 'zh-TW': '語言', 'zh-CN': '语言' },
        description_localizations: { 'zh-TW': '哪一種語言的譯文（預設跟著 Discord 的語言）', 'zh-CN': '哪一种语言的译文（默认跟着 Discord 的语言）' },
        choices: Object.entries(LANG_LABEL).map(([value, name]) => ({ name, value })),
      },
    ],
  },
  {
    // 使用者 2026-10-17：「/回饋」，跟網站的回饋頁進同一個地方；回覆只給本人看
    name: 'feedback', type: 1,
    description: 'Send feedback or a suggestion to the WynnChaYuan team',
    name_localizations: { 'zh-TW': '回饋', 'zh-CN': '反馈' },
    description_localizations: { 'zh-TW': '給 WynnChaYuan 團隊的回饋或建議', 'zh-CN': '给 WynnChaYuan 团队的反馈或建议' },
    options: [
      {
        type: 3, name: 'category', required: true,
        description: 'What kind of feedback',
        name_localizations: { 'zh-TW': '分類', 'zh-CN': '分类' },
        description_localizations: { 'zh-TW': '哪一種回饋', 'zh-CN': '哪一种反馈' },
        choices: FEEDBACK_CHOICES.map(([value, name, tw, cn]) => ({ name, value, name_localizations: { 'zh-TW': tw, 'zh-CN': cn } })),
      },
      {
        type: 3, name: 'message', required: true, min_length: FEEDBACK_MIN, max_length: FEEDBACK_MAX,
        description: 'Your feedback',
        name_localizations: { 'zh-TW': '內容', 'zh-CN': '内容' },
        description_localizations: { 'zh-TW': '想說的話', 'zh-CN': '想说的话' },
      },
      {
        type: 3, name: 'where', required: false, max_length: FEEDBACK_WHERE_MAX,
        description: 'Where: which text, screen or quest (helps a lot for translation suggestions)',
        name_localizations: { 'zh-TW': '位置', 'zh-CN': '位置' },
        description_localizations: { 'zh-TW': '在哪裡：哪一句、哪個畫面或任務（譯文建議特別需要）', 'zh-CN': '在哪里：哪一句、哪个画面或任务（译文建议特别需要）' },
      },
      {
        type: 3, name: 'language', required: false,
        description: 'Which translation it is about',
        name_localizations: { 'zh-TW': '語言', 'zh-CN': '语言' },
        description_localizations: { 'zh-TW': '跟哪一種語言的譯文有關', 'zh-CN': '跟哪一种语言的译文有关' },
        choices: Object.entries(LANG_LABEL).map(([value, name]) => ({ name, value })),
      },
    ],
  },
];

function hexBytes(hex) {
  const out = new Uint8Array(hex.length / 2);
  for (let i = 0; i < out.length; i++) out[i] = parseInt(hex.substr(i * 2, 2), 16);
  return out;
}

/** 這個請求是不是 Discord 送來的：Ed25519 簽名，簽的是時間戳加上原始內容。 */
export async function verifyDiscord(publicKey, signature, timestamp, body) {
  if (!/^[0-9a-f]{64}$/i.test(publicKey || '') || !/^[0-9a-f]{128}$/i.test(signature || '') || !timestamp) return false;
  try {
    const key = await crypto.subtle.importKey('raw', hexBytes(publicKey), { name: 'Ed25519' }, false, ['verify']);
    return await crypto.subtle.verify('Ed25519', key, hexBytes(signature), new TextEncoder().encode(timestamp + body));
  } catch (e) {
    return false;
  }
}

/** 語料索引解析過的版本，同一個執行個體裡留十分鐘（一份八九 MB，不要每次都重新解析）。 */
const parsedIndex = new Map();
async function corpusIndex(env, lang, ctx) {
  const hit = parsedIndex.get(lang);
  if (hit && Date.now() - hit.t < 600000) return hit.data;
  const res = await handleTrIndex(null, env, lang, ctx);
  if (res.status !== 200) return null;
  const data = await res.json();
  parsedIndex.set(lang, { t: Date.now(), data });
  return data;
}

async function botStatus(env, text) {
  const list = (await storedList(env)) || { users: [], capes: {} };
  const capes = await allCapes(env);
  const pending = capes.filter((c) => c.r).length;
  const openBy = async (prefix) => {
    const parts = [];
    for (const lang of TR_LANGS) {
      const n = (await trList(env, prefix + lang + ':')).filter((p) => p.st === 'open').length;
      if (n) parts.push(LANG_LABEL[lang] + ' ' + n);
    }
    return parts.length ? parts.join('、') : text.none;
  };
  return {
    embeds: [{
      title: text.status, url: SITE, color: 0x7fd08a,
      fields: [
        { name: text.users, value: String(list.users.length), inline: true },
        { name: text.capes, value: String(Object.keys(list.capes).length), inline: true },
        { name: text.pending, value: String(pending), inline: true },
        { name: text.props, value: await openBy('tp:') },
        { name: text.polls, value: await openBy('tq:') },
      ],
    }],
  };
}

/**
 * 披風的預覽圖（跟披風一覽的卡片一樣只露出背面，見 cape-preview.js）。照圖的雜湊存在 KV 的 prev:<雜湊>，
 * 同一張圖只算一次；圖被刪掉時一起刪。知道雜湊才拿得到（雜湊跟 /api/cape 的 ?k= 一樣是鑰匙）。
 */
/** 人物預覽的畫法版本。改了 figurePreview 的樣子就換一個：KV 的鍵與給 Discord 的網址都會跟著變（Discord 照網址快取圖片）。 */
const FIGURE_STYLE = 'angled';

async function handleCapePreview(env, hash, uuid) {
  if (!HASH.test(hash || '')) return no('not found', 404);
  // 帶了 ?u=<UUID>：畫成人物（那個人的 skin 穿著披風，從背後斜看，見 cape-preview.js 的 figurePreview）。
  // skin 會換，所以人物那份只留三天；只有披風背面的那份跟著圖本身，一直留著。
  // 人物那份的鍵帶畫法的版本（FIGURE_STYLE）：換了畫法，舊的那張不會再被拿出來。
  const who = UUID.test(uuid || '') ? uuid.replace(/-/g, '').toLowerCase() : null;
  const key = 'prev:' + hash + (who ? ':' + who + ':' + FIGURE_STYLE : '');
  const headers = (type) => ({
    'content-type': type === 'gif' ? 'image/gif' : 'image/png',
    'cache-control': who ? 'public, max-age=86400' : 'public, max-age=604800, immutable',
    'x-content-type-options': 'nosniff',
  });
  const kept = await env.CORPUS.getWithMetadata(key, { type: 'arrayBuffer' });
  if (kept && kept.value) return new Response(kept.value, { headers: headers(kept.metadata && kept.metadata.t) });
  const blob = await env.CORPUS.getWithMetadata('blob:' + hash, { type: 'arrayBuffer' });
  if (!blob || !blob.value) return no('not found', 404);
  const type = blob.metadata && blob.metadata.t === 'gif' ? 'gif' : 'png';
  let made = null;
  try {
    made = who ? await figurePreview(new Uint8Array(blob.value), type, await skinOf(who))
      : await capePreview(new Uint8Array(blob.value), type);
  } catch (e) {
    made = null;
  }
  if (!made) return no('cannot preview', 422);
  await env.CORPUS.put(key, made.bytes, who ? { metadata: { t: made.type }, expirationTtl: 3 * 86400 }
    : { metadata: { t: made.type } });
  return new Response(made.bytes, { headers: headers(made.type) });
}

/** 某個人的 skin（PNG）。跟網頁一樣向 minotar 拿；拿不到回 null，畫成灰色人偶。 */
async function skinOf(uuid) {
  try {
    const res = await fetch('https://minotar.net/skin/' + uuid, { signal: AbortSignal.timeout(5000) });
    return res.status === 200 ? new Uint8Array(await res.arrayBuffer()) : null;
  } catch (e) {
    return null;
  }
}

/** 圖刪掉時，它的預覽（披風背面那份與每個人的人物那份）一起刪。 */
async function dropPreviews(env, hash) {
  await env.CORPUS.delete('prev:' + hash);
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'prev:' + hash + ':', cursor });
    for (const k of page.keys) await env.CORPUS.delete(k.name);
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
}

async function botCape(env, text, name) {
  if (!NAME.test(name || '')) return { content: text.badName, flags: 64 };
  const capes = await allCapes(env);
  const mine = capes.find((c) => String(c.n || '').toLowerCase() === name.toLowerCase());
  if (!mine) return { content: botFmt(text.noCape, { name }) };
  if (mine.r) return { content: botFmt(text.pendingCape, { name: mine.n }) };
  if (mine.q) return { content: botFmt(text.privateCape, { name: mine.n }) };
  return {
    embeds: [{
      title: botFmt(text.capeOf, { name: mine.n }), url: SITE + '/capes', color: 0x7fd08a,
      image: { url: SITE + '/api/cape-preview/' + mine.h + (UUID.test(mine.u || '') ? '?u=' + mine.u.replace(/-/g, '') + '&v=' + FIGURE_STYLE : '') },
      thumbnail: { url: 'https://minotar.net/helm/' + encodeURIComponent(mine.n) + '/64.png' },
      fields: [
        { name: text.kind, value: mine.t === 'gif' ? 'GIF' : 'PNG', inline: true },
        { name: text.since, value: '<t:' + Math.floor((mine.v || mine.c || 0) / 1000) + ':R>', inline: true },
      ],
      footer: { text: text.gallery + ' · wynnchayuan.com/capes' },
    }],
  };
}

async function botTranslate(env, text, lang, query, ctx) {
  const q = String(query || '').trim().toLowerCase();
  if (q.length < 2) return { content: text.tooShort, flags: 64 };
  const data = await corpusIndex(env, lang, ctx);
  if (!data || !Array.isArray(data.rows)) return { content: text.indexFail };
  const hits = [];
  let total = 0;
  for (const row of data.rows) {
    if (!String(row[2]).toLowerCase().includes(q) && !String(row[3]).toLowerCase().includes(q)) continue;
    total++;
    if (hits.length < 5) hits.push(row);
  }
  if (!total) return { content: botFmt(text.notFound, { q: query, lang: LANG_LABEL[lang] }) };
  return {
    embeds: [{
      title: botFmt(text.found, { q: clip(query, 40), n: total, shown: hits.length }),
      url: SITE + '/translate', color: 0x7fd08a,
      fields: hits.map((row) => ({
        name: clip(String(row[2]).replace(/\n/g, ' / '), 250),
        value: clip((row[3] ? String(row[3]).replace(/\n/g, ' / ') : text.untranslated), 900)
          + '\n`' + clip(data.files[row[0]], 60) + '`',
      })),
      footer: { text: LANG_LABEL[lang] + ' · ' + text.more },
    }],
  };
}

async function botAnswer(env, interaction, ctx) {
  const { text, corpus } = botLocale(interaction.locale);
  const command = interaction.data || {};
  const option = (name) => ((command.options || []).find((o) => o.name === name) || {}).value;
  if (command.name === 'status') return botStatus(env, text);
  if (command.name === 'cape') {
    const answer = await botCape(env, text, option('player'));
    // 先把預覽圖算好存起來：Discord 拿到訊息後才來抓圖，現算的話大張的 GIF 可能來不及
    const image = answer.embeds && answer.embeds[0].image;
    if (image) {
      const at = new URL(image.url);
      await handleCapePreview(env, at.pathname.split('/').pop(), at.searchParams.get('u'));
    }
    return answer;
  }
  if (command.name === 'translate') {
    const lang = TR_LANGS.includes(option('language')) ? option('language') : corpus;
    return botTranslate(env, text, lang, option('text'), ctx);
  }
  if (command.name === 'feedback') return botFeedback(env, text, interaction, option, Date.now());
  return { content: text.unknown, flags: 64 };
}

/** Discord 的介面語言對到語料的哪一種；對不上回 null（英文介面不代表在講哪一種譯文）。 */
const LOCALE_CORPUS = { 'zh-TW': 'zh_tw', 'zh-CN': 'zh_cn', ja: 'ja_jp', ko: 'ko_kr', ru: 'ru_ru', 'es-ES': 'es_es', 'es-419': 'es_es', tr: 'tr_tr' };

/** /回饋：跟網站的回饋頁進同一個地方。Discord 已經告訴我們是誰，不必另外登入。回覆只給本人看。 */
async function botFeedback(env, text, interaction, option, now) {
  // 在伺服器裡是 member.user，私訊裡是 user
  const user = (interaction.member && interaction.member.user) || interaction.user || {};
  if (!DISCORD_ID.test(String(user.id || ''))) return { content: text.fbWhy['no user'], flags: 64 };
  const asked = option('language');
  const got = await submitFeedback(env, { id: user.id, name: String(user.global_name || user.username || user.id) }, {
    cat: option('category'), msg: option('message'), where: option('where') || '',
    lang: TR_LANGS.includes(asked) ? asked : LOCALE_CORPUS[interaction.locale] || null,
  }, 'bot', now);
  if (got.why) {
    const why = text.fbWhy[got.why] || text.unknown;
    return { content: botFmt(why, { n: FEEDBACK_PER_HOUR, t: got.until ? '<t:' + Math.ceil(got.until / 1000) + ':R>' : '' }), flags: 64 };
  }
  const { record } = got;
  const lines = [text.fbThanks];
  if (record.issue && record.issue.url) lines.push(botFmt(text.fbIssue, { url: record.issue.url }));
  else if (record.issueError) lines.push(text.fbIssueFail);
  return { content: lines.join('\n'), flags: 64 };
}

async function handleDiscordInteraction(request, env, ctx) {
  const body = await request.text();
  const ok = await verifyDiscord(env.DISCORD_PUBLIC_KEY, request.headers.get('x-signature-ed25519'),
    request.headers.get('x-signature-timestamp'), body);
  if (!ok) return new Response('bad signature', { status: 401 });
  let interaction;
  try {
    interaction = JSON.parse(body);
  } catch (e) {
    return no('bad request');
  }
  if (interaction.type === 1) return json({ type: 1 });
  if (interaction.type !== 2) return json({ type: 4, data: { content: '…', flags: 64 } });
  // 查語料要讀大索引、披風要先算預覽圖、譯文建議要開 GitHub Issue：先回「處理中」再補上
  // （Discord 只等三秒）。/回饋 的「處理中」只給本人看，補上的回覆也就只有本人看得到。
  const asked = interaction.data || {};
  const slowFeedback = asked.name === 'feedback'
    && (asked.options || []).some((o) => o.name === 'category' && o.value === 'translation');
  if ((['translate', 'cape'].includes(asked.name) || slowFeedback) && ctx && ctx.waitUntil) {
    ctx.waitUntil((async () => {
      let data;
      try {
        data = await botAnswer(env, interaction, ctx);
      } catch (e) {
        data = { content: botLocale(interaction.locale).text.indexFail };
      }
      await fetch(DISCORD_V10 + '/webhooks/' + interaction.application_id + '/' + interaction.token + '/messages/@original', {
        method: 'PATCH', headers: { 'content-type': 'application/json' }, body: JSON.stringify(data),
      });
    })());
    return json(asked.name === 'feedback' ? { type: 5, data: { flags: 64 } } : { type: 5 });
  }
  return json({ type: 4, data: await botAnswer(env, interaction, ctx) });
}

/** 機器人的設定齊了沒有、邀請連結（管理頁用；只有開發者）。 */
async function handleDiscordInfo(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  const id = env.DISCORD_CLIENT_ID || '';
  // 拿 Bot Token 直接問 Discord：Token 是哪個應用程式的、跟 Client ID 與 Public Key 對不對得上、
  // 註冊了哪些指令、機器人在哪些伺服器。「邀了卻找不到指令」時一眼看出是哪一環（使用者 2026-10-16）。
  let check = null;
  if (env.DISCORD_BOT_TOKEN) {
    const ask = async (path) => {
      try {
        const res = await fetch(DISCORD_V10 + path, { headers: { authorization: 'Bot ' + env.DISCORD_BOT_TOKEN } });
        return res.status === 200 ? await res.json() : { error: res.status };
      } catch (e) {
        return { error: 'unreachable' };
      }
    };
    const app = await ask('/applications/@me');
    check = { error: app.error || null };
    if (!app.error) {
      check.app = app.name;
      check.appId = app.id;
      check.bot = app.bot ? app.bot.username : null;
      check.sameClient = app.id === id;
      check.sameKey = String(app.verify_key || '').toLowerCase() === String(env.DISCORD_PUBLIC_KEY || '').toLowerCase();
      check.endpointSet = app.interactions_endpoint_url || null;
      const commands = await ask('/applications/' + app.id + '/commands');
      check.commands = Array.isArray(commands) ? commands.map((c) => c.name) : null;
      const guilds = await ask('/users/@me/guilds');
      check.guilds = Array.isArray(guilds) ? guilds.map((g) => g.name) : null;
    }
  }
  return json({
    publicKey: Boolean(env.DISCORD_PUBLIC_KEY), token: Boolean(env.DISCORD_BOT_TOKEN), clientId: Boolean(id),
    invite: id ? 'https://discord.com/oauth2/authorize?client_id=' + encodeURIComponent(id) + '&scope=applications.commands+bot&permissions=0' : '',
    endpoint: SITE + '/api/discord/interactions',
    check,
  }, 200, { 'cache-control': 'no-store' });
}

/** 把指令註冊到 Discord（全域指令；改了指令再按一次就會覆寫）。只有開發者。 */
async function handleDiscordRegister(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  if (!env.DISCORD_BOT_TOKEN || !env.DISCORD_CLIENT_ID) return no('discord bot not configured', 503);
  try {
    const res = await fetch(DISCORD_V10 + '/applications/' + env.DISCORD_CLIENT_ID + '/commands', {
      method: 'PUT',
      headers: { authorization: 'Bot ' + env.DISCORD_BOT_TOKEN, 'content-type': 'application/json' },
      body: JSON.stringify(DISCORD_COMMANDS),
    });
    if (res.status !== 200) return no('discord ' + res.status, 502);
    const list = await res.json();
    return json({ ok: true, commands: Array.isArray(list) ? list.map((c) => c.name) : [] });
  } catch (e) {
    return no('discord unreachable', 502);
  }
}

const site = {
  /** 每天一次（wrangler.toml 的 crons）：沒有人穿滿七天的披風刪掉。 */
  async scheduled(event, env) {
    await tidy(env, Date.now());
  },
  async fetch(request, env, ctx) {
    // HEAD：網頁、圖示、字型、材質跟 GET 一樣回，只是不帶內容。Discord 檢查應用程式的
    // 服務條款與隱私政策網址時送的是 HEAD，原本一律 404，被它當成不接受的網址（2026-10-16）。
    if (request.method === 'HEAD' && !new URL(request.url).pathname.startsWith('/api/')) {
      const full = await site.fetch(new Request(request.url, { method: 'GET', headers: request.headers }), env, ctx);
      return new Response(null, { status: full.status, headers: full.headers });
    }
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
    if (route === 'GET /admin/translators') return handleTranslators(request, env, now);
    if (route === 'POST /admin/translator') return handleTranslatorAdd(request, env, now);
    if (route === 'POST /admin/untranslator') return handleTranslatorRemove(request, env, now);
    if (route === 'GET /tr/me') return handleTrMe(request, env, now);
    if (route.startsWith('GET /tr/index/')) return handleTrIndex(request, env, route.slice('GET /tr/index/'.length), ctx);
    if (route === 'GET /tr/props') return handleTrProps(request, env);
    if (route === 'GET /tr/prop') return handleTrProp(request, env);
    if (route === 'POST /tr/propose') return handleTrPropose(request, env, now);
    if (route === 'POST /tr/vote') return handleTrVote(request, env, now);
    if (route === 'POST /tr/accept') return handleTrAccept(request, env, now);
    if (route === 'POST /tr/reject') return handleTrReject(request, env, now);
    if (route === 'GET /tr/polls') return handleTrPolls(request, env);
    if (route === 'GET /tr/poll') return handleTrPoll(request, env);
    if (route === 'POST /tr/poll') return handleTrPollOpen(request, env, now);
    if (route === 'POST /tr/poll/option') return handleTrPollOption(request, env, now);
    if (route === 'POST /tr/poll/vote') return handleTrPollVote(request, env, now);
    if (route === 'POST /tr/poll/decide') return handleTrPollDecide(request, env, now);
    if (route === 'POST /tr/poll/reopen') return handleTrPollReopen(request, env, now);
    if (route === 'POST /feedback') return handleFeedback(request, env, now);
    if (route === 'GET /admin/feedback') return handleFeedbackList(request, env, now);
    if (route === 'POST /admin/feedback/status') return handleFeedbackStatus(request, env, now);
    if (route === 'POST /admin/feedback/delete') return handleFeedbackDelete(request, env, now);
    if (route === 'POST /admin/feedback/issue') return handleFeedbackIssue(request, env, now);
    if (route === 'POST /discord/interactions') return handleDiscordInteraction(request, env, ctx);
    if (route === 'GET /admin/discord') return handleDiscordInfo(request, env, now);
    if (route === 'POST /admin/discord/register') return handleDiscordRegister(request, env, now);
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
    if (route.startsWith('GET /cape-preview/')) {
      return handleCapePreview(env, route.slice('GET /cape-preview/'.length), url.searchParams.get('u'));
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

export default site;
