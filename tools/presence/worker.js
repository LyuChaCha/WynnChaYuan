/**
 * WynnChaYuan 的登記站：誰有裝、誰的披風要給別人看、玩家上傳的披風。
 *
 *   POST   /login        {name, serverId}   向 Mojang 確認這個人真的是他說的那個帳號，發一張通行證
 *   POST   /hello        {cape}             報到（要通行證）
 *   GET    /list                            名單：有裝的人、有上傳披風的人、關掉披風的人
 *   PUT    /cape         圖檔本體            上傳披風（要通行證）
 *   DELETE /cape                            拿掉自己上傳的披風（要通行證）
 *   GET    /cape/<代碼>                      下載某個人的披風
 *   GET    /capes                           所有上傳的披風（相同的圖併成一筆）
 *   GET    /  與  /gallery                  披風一覽（網頁）
 *   GET    /status                          有幾個人、幾件披風
 *   POST   /mod-link                        開發者拿一條帶著管理權的一覽網址（要通行證）
 *   POST   /admin/remove {id|hash, ban}     拿掉披風、要不要順便禁止再傳（開發者，或 ADMIN_TOKEN）
 *
 * 一個 KV（綁定叫 CORPUS，沿用語料收集站那一個），沒有別的儲存。
 *
 * 它記什麼：玩家的 UUID（當成鍵）、要不要顯示披風、哪一天報到的、上傳的披風圖檔。
 * 十四天沒再報到就從名單上消失（圖檔留著，回來報到就又出現）。
 * 它不記：IP、所在的世界、任何遊戲內容。帳號名只在驗證與「是不是名單上的人」那一刻用到。
 *
 * 對外公開的名單不放 UUID，放的是 UUID 雜湊出來的代碼——模組把畫面上那個人的 UUID
 * 算成代碼再去名單裡找。
 *
 * 身分驗證用的是 Minecraft 多人伺服器自己那一套：模組向 Mojang 說「我要加入 <隨機字串>」，
 * 我們再問 Mojang「這個帳號剛剛是不是加入了 <隨機字串>」。模組的登入憑證不會經過這裡。
 *
 * 部署方式見同一個資料夾的 README.md。
 */

import { GALLERY } from './gallery.js';

/** 公開代碼的樣子：24 個小寫十六進位字元。 */
const ID = /^[0-9a-f]{24}$/;
const UUID = /^[0-9a-f]{32}$/;
const NAME = /^[A-Za-z0-9_]{1,16}$/;
const SERVER_ID = /^[0-9a-f]{40}$/;

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

/** 披風圖檔的上限。 */
export const MAX_CAPE_BYTES = 20 * 1024 * 1024;

/** GIF 最多幾格。 */
export const MAX_CAPE_FRAMES = 120;

/** 同一個人多久才能再傳一次。 */
const UPLOAD_EVERY_MS = 20 * 1000;

/**
 * 收哪幾種尺寸——就是 capes/templates 的範本：22×17 格的 1、2、4、8 倍
 * （左邊 1 ＋ 正面 10 ＋ 右邊 1 ＋ 背面 10 寬，上下緣 1 ＋ 面 16 高）。
 * 不照範本畫的圖不收，免得什麼圖都往上傳。
 */
export const CAPE_SIZES = [[22, 17], [44, 34], [88, 68], [176, 136]];

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

async function who(request, env, now) {
  const header = request.headers.get('authorization') || '';
  return header.startsWith('Bearer ') ? readToken(env, header.slice(7), now) : null;
}

async function readJson(request) {
  const length = Number(request.headers.get('content-length') || 0);
  if (length > MAX_BODY) return undefined;
  try {
    const text = await request.text();
    if (text.length > MAX_BODY) return undefined;
    const body = JSON.parse(text);
    return body && typeof body === 'object' && !Array.isArray(body) ? body : null;
  } catch (e) {
    return null;
  }
}

async function handleLogin(request, env, now) {
  const body = await readJson(request);
  if (body === undefined) return no('too large', 413);
  if (!body || typeof body.name !== 'string' || !NAME.test(body.name)
      || typeof body.serverId !== 'string' || !SERVER_ID.test(body.serverId)) {
    return no('bad login');
  }
  let profile = null;
  try {
    const answer = await fetch('https://sessionserver.mojang.com/session/minecraft/hasJoined'
      + '?username=' + encodeURIComponent(body.name) + '&serverId=' + body.serverId);
    if (answer.status === 200) profile = await answer.json();
  } catch (e) {
    return no('mojang unreachable', 502);
  }
  const uuid = profile && typeof profile.id === 'string' ? profile.id.toLowerCase() : '';
  const name = profile && typeof profile.name === 'string' ? profile.name : '';
  if (!UUID.test(uuid) || !NAME.test(name)) return no('not verified', 403);
  return json({ ok: true, token: await issueToken(env, uuid, name, now), id: await publicId(uuid) });
}

// ---------------------------------------------------------------- 報到與名單

/** 這一次報到要不要寫進 KV：沒登記過、設定變了、披風換了、或上次是昨天以前。 */
export function needsWrite(existing, next) {
  if (!existing) return true;
  return existing.c !== next.c || existing.d !== next.d || existing.v !== next.v;
}

async function handleHello(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const body = await readJson(request);
  if (body === undefined) return no('too large', 413);
  if (!body || ('cape' in body && typeof body.cape !== 'boolean')) return no('bad hello');
  const record = await remember(env, user.uuid, body.cape !== false, now);
  return json({ ok: true, id: record.i, cape: record.v > 0 });
}

/** 把一個人的現況寫進 KV（有變才寫），順手改進現成的名單。回傳他那一筆。 */
async function remember(env, uuid, wantsCape, now) {
  const id = await publicId(uuid);
  const key = 'u:' + uuid;
  const have = await env.CORPUS.getWithMetadata(key);
  const cape = await env.CORPUS.getWithMetadata('cape:' + id, { type: 'stream' });
  const version = cape && cape.metadata ? cape.metadata.v : 0;
  if (cape && cape.value && cape.value.cancel) cape.value.cancel();
  const next = { i: id, c: wantsCape ? 1 : 0, d: day(now), v: version };
  const before = have && have.metadata;
  if (needsWrite(before, next)) {
    await env.CORPUS.put(key, '', { expirationTtl: TTL_SECONDS, metadata: next });
    if (!before || before.c !== next.c || before.v !== next.v) {
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
  if (known && shown === wasShown && hidden === wasHidden) return null;
  const capes = { ...list.capes };
  if (shown) capes[record.i] = shown; else delete capes[record.i];
  return {
    t: list.t,
    users: known ? list.users : [...list.users, record.i],
    capes,
    hidden: hidden ? (wasHidden ? list.hidden : [...list.hidden, record.i])
      : list.hidden.filter((id) => id !== record.i),
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
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'u:', limit: 1000, cursor });
    for (const k of page.keys) {
      const m = k.metadata;
      if (!m || !ID.test(m.i)) continue;
      users.push(m.i);
      if (m.c !== 1) hidden.push(m.i);
      else if (m.v > 0) capes[m.i] = m.v;
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor && users.length < MAX_USERS);
  return { t: now, users, capes, hidden };
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
    metadata: { t: found.type, v: now, s: bytes.length, u: user.uuid, h: hash },
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

/** 管理權的效期：開發者從遊戲裡開一覽頁，兩個小時內可以刪。 */
const MOD_TTL_MS = 2 * 3600 * 1000;

/** 開發者拿一條帶著管理權的一覽網址。網址 # 後面那一段不會送到任何伺服器，只有那一頁讀得到。 */
async function handleModLink(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  if (!await isDeveloper(user.name, now)) return no('not a developer', 403);
  const body = 'mod.' + user.uuid + '.' + user.name + '.' + (now + MOD_TTL_MS);
  const key = body + '.' + await sign(env, body);
  return json({ ok: true, url: new URL(request.url).origin + '/gallery#k=' + key });
}

/** 這個請求有沒有管理權：ADMIN_TOKEN，或一張還沒過期、而且本人還是開發者的管理憑證。 */
async function mayModerate(request, env, now) {
  const header = request.headers.get('authorization') || '';
  if (!header.startsWith('Bearer ')) return false;
  const given = header.slice(7);
  if (env.ADMIN_TOKEN && given === env.ADMIN_TOKEN) return true;
  const parts = given.split('.');
  if (parts.length !== 5 || parts[0] !== 'mod') return false;
  const [, uuid, name, expires, signature] = parts;
  if (!UUID.test(uuid) || !NAME.test(name) || !/^\d{1,16}$/.test(expires)) return false;
  if (Number(expires) < now) return false;
  const expected = await sign(env, 'mod.' + uuid + '.' + name + '.' + expires);
  if (expected.length !== signature.length) return false;
  let diff = 0;
  for (let i = 0; i < expected.length; i++) diff |= expected.charCodeAt(i) ^ signature.charCodeAt(i);
  return diff === 0 && isDeveloper(name, now);
}

/**
 * 拿掉披風：{id} 是一個人的，{hash} 是所有用同一張圖的。ban 是 true 的話那些人之後不能再傳，
 * false 是解除。
 */
async function handleAdminRemove(request, env, now) {
  if (!await mayModerate(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  if (!body) return no('bad request');
  let ids;
  if (typeof body.id === 'string' && ID.test(body.id)) {
    ids = [body.id];
  } else if (typeof body.hash === 'string' && /^[0-9a-f]{64}$/.test(body.hash)) {
    ids = (await allCapes(env)).filter((cape) => cape.h === body.hash).map((cape) => cape.id);
  } else {
    return no('bad request');
  }
  let removed = 0;
  for (const id of ids) {
    const uuid = await dropCape(env, id, now);
    if (!uuid) continue;
    removed++;
    if (body.ban === true) await env.CORPUS.put('ban:' + uuid, String(now));
    if (body.ban === false) await env.CORPUS.delete('ban:' + uuid);
  }
  return json({ ok: true, removed, banned: body.ban === true });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const now = Date.now();
    const route = request.method + ' ' + url.pathname;
    if (route === 'POST /login') return handleLogin(request, env, now);
    if (route === 'POST /hello') return handleHello(request, env, now);
    if (route === 'GET /list') return handleList(env, now);
    if (route === 'PUT /cape') return handleUpload(request, env, now);
    if (route === 'DELETE /cape') return handleRemove(request, env, now);
    if (route === 'POST /admin/remove') return handleAdminRemove(request, env, now);
    if (route === 'POST /mod-link') return handleModLink(request, env, now);
    if (route === 'GET /capes') return handleCapes(env);
    if (route === 'GET /gallery' || route === 'GET /') {
      return new Response(GALLERY, {
        headers: {
          'content-type': 'text/html; charset=utf-8',
          'cache-control': 'no-store',
          'referrer-policy': 'no-referrer',
          'x-content-type-options': 'nosniff',
          'x-frame-options': 'DENY',
          // 這一頁只跑自己的程式與 jsDelivr 上的立體檢視；圖只從自己這裡來
          'content-security-policy': "default-src 'self'; img-src 'self' data: blob:; "
            + "style-src 'unsafe-inline'; script-src 'unsafe-inline' https://cdn.jsdelivr.net; "
            + "connect-src 'self'; base-uri 'none'; form-action 'none'",
        },
      });
    }
    if (request.method === 'GET' && url.pathname.startsWith('/cape/')) {
      return handleCape(env, url.pathname.slice('/cape/'.length));
    }
    if (route === 'GET /status') {
      const list = await storedList(env);
      return json({
        what: 'WynnChaYuan presence',
        users: list ? list.users.length : 0,
        capes: list ? Object.keys(list.capes).length : 0,
        uploads: env.UPLOADS || 'tiered',
      });
    }
    return no('not found', 404);
  },
};
