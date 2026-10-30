
import { LANGS, PATHS, pickLang, render, renderLost, tagOf } from './site.js';
import { FONTS } from './demo-fonts.js';
import { TEXTURES } from './demo-textures.js';
import { CAPE_TEMPLATES } from './cape-templates.js';
import { CERT_KEYS, PROFILE_KEYS } from './mojang-keys.js';
import { APPLE_TOUCH, FAVICON, ICON_192 } from './icons.js';
import { capePreview, figurePreview } from './cape-preview.js';

const ID = /^[0-9a-f]{24}$/;
const UUID = /^[0-9a-f]{32}$/;
const NAME = /^[A-Za-z0-9_]{1,16}$/;
const BASE64 = /^[A-Za-z0-9+/]+={0,2}$/;

export const LOGIN_PREFIX = 'wynnchayuan.com login\n';

const CHALLENGE_TTL_MS = 5 * 60 * 1000;

const PROFILE_MAX_AGE_MS = 30 * 24 * 3600 * 1000;

const MAX_LOGIN_BODY = 12 * 1024;

const TTL_SECONDS = 14 * 24 * 3600;

const LIST_MAX_AGE_MS = 10 * 60 * 1000;

const TOKEN_TTL_MS = 7 * 24 * 3600 * 1000;

const MAX_USERS = 50000;

const MAX_BODY = 512;

export const MAX_CAPE_BYTES = 20 * 1024 * 1024;

export const MAX_PLAIN_CAPE_BYTES = 1024 * 1024;

export const MAX_CAPE_FRAMES = 120;

const UPLOAD_EVERY_MS = 20 * 1000;

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

export async function publicId(uuid) {
  const data = new TextEncoder().encode('wynnchayuan:' + uuid);
  return hex(await crypto.subtle.digest('SHA-256', data)).slice(0, 24);
}


let cachedSecret = null;

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

const WEB_TTL_MS = 24 * 3600 * 1000;

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

async function who(request, env, now) {
  const header = request.headers.get('authorization') || '';
  if (!header.startsWith('Bearer ')) return null;
  const token = header.slice(7);
  if (token.startsWith('dc.')) {
    const session = await discordSession(request, env, now);
    const linked = session && session.account;
    return linked && UUID.test(linked.u || '') && NAME.test(linked.m || '')
      ? { uuid: linked.u, name: linked.m, web: true, discord: session.id, discordName: linked.n || '' }
      : null;
  }
  return token.startsWith('web.') ? readWebToken(env, token, now) : readToken(env, token, now);
}


const DISCORD_API = 'https://discord.com/api';
const DISCORD_ID = /^\d{5,25}$/;

const SESSION_TTL_MS = 30 * 24 * 3600 * 1000;

const LINK_TTL_SECONDS = 600;

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

async function handleDiscordStart(request, env, now) {
  const url = new URL(request.url);
  const origin = url.origin;
  if (!discordOn(env)) return redirect(origin + '/account#e=discord-off');
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
  const intent = parts[1].slice(32);
  if (INTENT.test(intent)) {
    try {
      const wanted = JSON.parse(await env.CORPUS.get('intent:' + intent) || 'null');
      if (wanted && UUID.test(wanted.u || '') && NAME.test(wanted.m || '')) {
        await bind(env, id, wanted.u, wanted.m, now);
      }
    } catch (e) {
    }
    await env.CORPUS.delete('intent:' + intent);
  }
  // 通行證放在 # 後面：不會送到任何伺服器
  return redirect(url.origin + '/account#t=' + await issueDiscordToken(env, id, now), oauthCookie('', 0));
}

async function handleLinkStart(request, env, now) {
  const session = await discordSession(request, env, now);
  if (!session) return no('login first', 401);
  const bytes = crypto.getRandomValues(new Uint8Array(8));
  let code = '';
  for (const b of bytes) code += LINK_ALPHABET[b % LINK_ALPHABET.length];
  await env.CORPUS.put('link:' + code, session.id, { expirationTtl: LINK_TTL_SECONDS });
  return json({ ok: true, code, minutes: LINK_TTL_SECONDS / 60 });
}

async function handleLink(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
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

// Mojang 擋掉 Worker 的請求（403）：不問 Mojang，改驗它簽好的玩家金鑰憑證與個人檔案

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

function mojangKeys(env, name) {
  if (env.MOJANG_KEYS) {
    try {
      const all = JSON.parse(env.MOJANG_KEYS);
      const list = (all[name] || []).map((k) => (typeof k === 'string' ? k : k && k.publicKey))
        .filter((k) => typeof k === 'string');
      if (list.length) return list;
    } catch (e) {
    }
  }
  return name === 'playerCertificateKeys' ? CERT_KEYS : PROFILE_KEYS;
}

async function signedBy(keys, hash, signature, data) {
  for (const spki of keys) {
    try {
      const key = await crypto.subtle.importKey('spki', fromBase64(spki, 4096),
        { name: 'RSASSA-PKCS1-v1_5', hash }, false, ['verify']);
      if (await crypto.subtle.verify('RSASSA-PKCS1-v1_5', key, signature, data)) return true;
    } catch (e) {
    }
  }
  return false;
}

export function certificatePayload(uuid, expiresAt, publicKey) {
  const out = new Uint8Array(24 + publicKey.length);
  for (let i = 0; i < 16; i++) out[i] = parseInt(uuid.slice(i * 2, i * 2 + 2), 16);
  new DataView(out.buffer).setBigUint64(16, BigInt(expiresAt));
  out.set(publicKey, 24);
  return out;
}

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

async function profileName(env, textures, uuid, now) {
  if (!textures || typeof textures !== 'object') return null;
  const signature = fromBase64(textures.signature, 1024);
  if (!signature || typeof textures.value !== 'string' || textures.value.length > 6000) return null;
  const value = fromBase64(textures.value, 6000);
  if (!value) return null;
  const signed = new TextEncoder().encode(textures.value);
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
  await refreshLinkedName(env, body.uuid, name);
  return json({ ok: true, token: await issueToken(env, body.uuid, name, now), id: await publicId(body.uuid),
    member: await isMember(env, { uuid: body.uuid, name }, now) });
}


export function needsWrite(existing, next) {
  if (!existing) return true;
  return existing.c !== next.c || existing.d !== next.d || existing.v !== next.v
    || (existing.r || 0) !== (next.r || 0) || existing.s !== next.s;
}

const ROLE = /^[a-z]{1,24}$/;

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

async function remember(env, uuid, wantsCape, now, prefs) {
  const id = await publicId(uuid);
  const key = 'u:' + uuid;
  const have = await env.CORPUS.getWithMetadata(key);
  const cape = await env.CORPUS.getWithMetadata('cape:' + id, { type: 'stream' });
  const version = cape && cape.metadata && !cape.metadata.r ? cape.metadata.v : 0;
  if (cape && cape.value && cape.value.cancel) cape.value.cancel();
  const next = { i: id, c: wantsCape ? 1 : 0, d: day(now), v: version };
  const before = have && have.metadata;
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

export function dropFromList(list, id) {
  const rainbows = list.rainbow || [];
  const roles = list.show || {};
  if (!list.users.includes(id) && !(id in list.capes) && !list.hidden.includes(id)
      && !rainbows.includes(id) && !(id in roles)) {
    return null;
  }
  const capes = { ...list.capes };
  delete capes[id];
  const show = { ...roles };
  delete show[id];
  return {
    ...list,
    users: list.users.filter((one) => one !== id),
    capes,
    hidden: list.hidden.filter((one) => one !== id),
    rainbow: rainbows.filter((one) => one !== id),
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
  if (!list) return;
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
  return 0;
}

let cachedCredits = null;

const COLOUR = /^#[0-9a-fA-F]{6}$/;

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
      const teams = knownCodes(member.teams, TR_LANGS);
      if (teams.length) entry.teams = teams;
      const areas = knownCodes(member.areas, TEAM_AREAS);
      if (areas.length) entry.areas = areas;
      members.push(entry);
    }
    team.push({ role: found[1], color: COLOUR.test(section.color || '') ? section.color : null, members });
  }
  return team;
}

async function handleTeam(env, now) {
  const list = await credits(now);
  if (!list) return no('try later', 503);
  const guilds = (await guildList(env, now)).map((guild) => ({
    slug: guild.slug, name: guild.name, tag: guild.tag || '', icon: guild.i || 0, banner: guild.b || 0 }));
  return json({ team: list.team, guilds }, 200, { 'cache-control': 'public, max-age=300' });
}


const GUILD_NAME = /^[A-Za-z](?:[A-Za-z ]{1,28})[A-Za-z]$/;
const GUILD_TAG = /^[A-Za-z]{3,4}$/;
const GUILD_SLUG = /^[a-z](?:[a-z-]{1,28})[a-z]$/;

export const MAX_GUILD_IMAGE = 512 * 1024;

const MAX_GUILDS = 24;

export function guildSlug(name) {
  const raw = String(name == null ? '' : name).trim().replace(/\s+/g, ' ');
  return GUILD_NAME.test(raw) ? raw.toLowerCase().replace(/ /g, '-') : null;
}

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
  }
  return cachedCredits;
}

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

async function hasPerk(env, uuid) {
  return Boolean(await env.CORPUS.get('perk:' + uuid));
}

async function isMember(env, user, now) {
  if (await hasPerk(env, user.uuid)) return true;
  const list = await credits(now);
  return Boolean(list && list.names.has(user.name.toLowerCase()));
}

async function capeLimit(env, user, now) {
  return await isMember(env, user, now) ? MAX_CAPE_BYTES : MAX_PLAIN_CAPE_BYTES;
}

export function plainUuid(text) {
  const plain = String(text == null ? '' : text).trim().toLowerCase().replace(/-/g, '');
  return UUID.test(plain) ? plain : null;
}

async function isDeveloper(name, now) {
  const list = await credits(now);
  return Boolean(list && list.developers.has(name.toLowerCase()));
}


const DISCORD_SNOWFLAKE = /^\d{17,20}$/;

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

async function authority(request, env, now) {
  const header = request.headers.get('authorization') || '';
  if (env.ADMIN_TOKEN && header === 'Bearer ' + env.ADMIN_TOKEN) return { by: 'admin', developer: true };
  const user = await who(request, env, now);
  if (user) {
    if (await isDeveloper(user.name, now)) return { by: user.name, developer: true };
    return await isStaff(env, user) ? { by: user.name, developer: false } : null;
  }
  const session = await discordSession(request, env, now);
  if (session && (await env.CORPUS.get('staff:d:' + session.id)
      || (session.account.u && await env.CORPUS.get('staff:u:' + session.account.u)))) {
    return { by: 'Discord: ' + (session.account.n || session.id), developer: false };
  }
  return null;
}

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
  if (key.startsWith('n:')) record.m = String(body.who).trim();
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

async function handlePeople(request, env, now) {
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
      // 舊帳號沒有 metadata：讀一次本體
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

export const BURST_COUNT = 3;
export const BURST_WINDOW_MS = 3600 * 1000;
export const BURST_LOCK_MS = 12 * 3600 * 1000;

export function burstLock(log, now) {
  return log && log.until > now ? log.until : 0;
}

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
  const limit = await capeLimit(env, user, now);
  if (bytes.length > limit) return json({ ok: false, why: 'too large', limit }, 413);
  const id = await publicId(user.uuid);
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (have && have.metadata && now - have.metadata.v < UPLOAD_EVERY_MS) {
    return no('slow down', 429);
  }
  if (have && have.metadata && (!have.metadata.w || have.metadata.p) && await reviewOf(env, user.uuid)) {
    return no('review one', 403);
  }
  const hash = hex(await crypto.subtle.digest('SHA-256', bytes));
  const blob = await env.CORPUS.getWithMetadata('blob:' + hash, { type: 'stream' });
  if (blob && blob.value && blob.value.cancel) blob.value.cancel();
  if (!blob || !blob.metadata) {
    await env.CORPUS.put('blob:' + hash, bytes, { metadata: { t: found.type, s: bytes.length } });
  }
  const metadata = { t: found.type, v: now, c: now, s: bytes.length, u: user.uuid, n: user.name, h: hash,
    x: found.width, y: found.height };
  const vis = askedVisibility(new URL(request.url).searchParams)
    || (have && have.metadata ? visibility(have.metadata) : 'public');
  if (vis !== 'public') metadata.vis = vis;
  const pending = await needsReview(env, user.uuid, hash);
  if (pending) metadata.r = 1;
  await env.CORPUS.put('cape:' + id, '', { metadata });
  await env.CORPUS.put('upl:' + user.uuid, JSON.stringify(burstNote(log, now)),
    { expirationTtl: Math.ceil((BURST_LOCK_MS + BURST_WINDOW_MS) / 1000) });
  await noteHistory(env, id, metadata, now);
  await revive(env, hash);
  if (have && have.metadata && have.metadata.h !== hash) await sweep(env, have.metadata.h, have.metadata, now);
  if (have && have.metadata && have.metadata.p && have.metadata.p.h !== hash) {
    await sweep(env, have.metadata.p.h, { ...have.metadata.p, vis: visibility(have.metadata) }, now);
  }
  const mine = await env.CORPUS.getWithMetadata('u:' + user.uuid);
  await remember(env, user.uuid, !(mine && mine.metadata && mine.metadata.c === 0), now);
  return json({ ok: true, id, v: now, ...found, pending });
}


const HASH = /^[0-9a-f]{64}$/;

const HISTORY_MAX = 8;

const FAVS_MAX = 200;

export const VISIBILITY = ['public', 'unlisted', 'private'];

export function visibility(meta) {
  if (!meta) return 'public';
  if (meta.vis === 'unlisted' || meta.vis === 'private') return meta.vis;
  return meta.q ? 'unlisted' : 'public';
}

export function askedVisibility(params) {
  const vis = params.get('vis');
  if (VISIBILITY.includes(vis)) return vis;
  const pub = params.get('public');
  return pub === '0' ? 'unlisted' : pub === '1' ? 'public' : null;
}

function withVisibility(meta, vis) {
  const { q, vis: old, ...rest } = meta;
  if (vis !== 'public') rest.vis = vis;
  return rest;
}

export function groupVisibility(wearers) {
  if (wearers.some(listed)) return 'public';
  if (wearers.some((cape) => visibility(cape) !== 'private')) return 'unlisted';
  return 'private';
}

const idleVisibility = (one) => (!one.q ? 'public' : one.vis === 'private' ? 'private' : 'unlisted');

const linkable = (one) => !one.q || one.vis === 'unlisted';

const listed = (cape) => visibility(cape) === 'public' && !cape.r && !cape.lk;

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

async function handleStar(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const body = await readJson(request);
  const hash = body && typeof body.hash === 'string' ? body.hash : '';
  if (!HASH.test(hash) || typeof body.on !== 'boolean') return no('bad request');
  const capes = await allCapes(env);
  const idle = await idleMap(env);
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
    const alive = new Set([...capes.map((cape) => cape.h), ...Object.keys(idle)]);
    for (const key of Object.keys(map)) {
      if (!alive.has(key) || !map[key]) delete map[key];
    }
    await env.CORPUS.put('stars', JSON.stringify(map));
  }
  return json({ ok: true, on: body.on, stars: Number(map[hash]) || 0 });
}

async function handleMine(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const id = await publicId(user.uuid);
  const alive = new Set(Object.keys(await purge(env, now)));
  for (const cape of await allCapes(env)) {
    alive.add(cape.h);
    if (cape.p && cape.id === id) alive.add(cape.p.h);
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

async function handlePublic(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const body = await readJson(request);
  const vis = !body ? null : VISIBILITY.includes(body.vis) ? body.vis
    : typeof body.on === 'boolean' ? (body.on ? 'public' : 'unlisted') : null;
  if (!vis) return no('bad request');
  const id = await publicId(user.uuid);
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (!have || !have.metadata) return no('no cape', 404);
  await env.CORPUS.put('cape:' + id, '', { metadata: withVisibility(have.metadata, vis) });
  return json({ ok: true, vis, public: vis === 'public' });
}

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
  await release(env, await publicId(uuid), uuid, now);
  return json({ ok: true });
}

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


const WEAR_EVERY_MS = 5 * 1000;

async function handleWear(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const body = await readJson(request);
  const hash = body && typeof body.hash === 'string' ? body.hash : '';
  if (!/^[0-9a-f]{64}$/.test(hash)) return no('bad request');
  const id = await publicId(user.uuid);
  const everyone = await allCapes(env);
  const idleNow = await idleMap(env);
  let onShow = everyone.find((cape) => cape.h === hash && listed(cape));
  if (!onShow) {
    const waiting = idleNow[hash];
    if (waiting && !waiting.q) onShow = { ...waiting, h: hash };
  }
  let linked = null;
  if (!onShow) {
    const shared = everyone.find((cape) => cape.h === hash && !cape.r && visibility(cape) !== 'private');
    if (shared) linked = shared;
    else if (idleNow[hash] && linkable(idleNow[hash])) linked = { ...idleNow[hash], h: hash };
  }
  const past = onShow || linked ? null : await env.CORPUS.getWithMetadata('hist:' + id + ':' + hash);
  let source = onShow || linked || (past && past.metadata ? { ...past.metadata, h: hash } : null);
  if (source && !onShow && !linked) {
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
  if (mine && visibility(mine) !== 'public') metadata.vis = visibility(mine);
  if (linked) metadata.lk = 1;
  const pending = !onShow && !linked && await needsReview(env, user.uuid, hash);
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
  if (visibility(mine) !== 'public') metadata.vis = visibility(mine);
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

// idle 整筆覆寫，同時寫入會互蓋：tidy 每天把漏記的圖補回去

const IDLE_KEEP_MS = 7 * 24 * 3600 * 1000;

async function idleMap(env) {
  return parseRecord(await env.CORPUS.get('idle'));
}

const inUse = (capes, hash) => capes.some((cape) => cape.h === hash || (cape.p && cape.p.h === hash));

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
  const vis = !from ? 'private' : from.lk && visibility(from) === 'public' ? 'unlisted' : visibility(from);
  if (!from || vis !== 'public' || from.r) entry.q = 1;
  if (from && !from.r && vis !== 'public') entry.vis = vis;
  idle[hash] = entry;
  await env.CORPUS.put('idle', JSON.stringify(idle));
}

async function revive(env, hash) {
  const idle = await idleMap(env);
  if (!idle[hash]) return;
  delete idle[hash];
  await env.CORPUS.put('idle', JSON.stringify(idle));
}

async function discard(env, hash) {
  if (!hash || inUse(await allCapes(env), hash)) return false;
  await env.CORPUS.delete('blob:' + hash);
  await dropPreviews(env, hash);
  await revive(env, hash);
  return true;
}

async function purge(env, now) {
  const idle = await idleMap(env);
  const due = Object.keys(idle).filter((hash) => now - (Number(idle[hash] && idle[hash].at) || 0) >= IDLE_KEEP_MS);
  if (!due.length) return idle;
  const capes = await allCapes(env);
  const stars = await starMap(env);
  let starred = false;
  for (const hash of due) {
    delete idle[hash];
    if (inUse(capes, hash)) continue;
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
      'cache-control': 'public, max-age=3600',
      'x-content-type-options': 'nosniff',
    },
  });
}

async function dropCape(env, id, now) {
  const have = await env.CORPUS.getWithMetadata('cape:' + id);
  if (!have || !have.metadata) return null;
  await env.CORPUS.delete('cape:' + id);
  await sweep(env, have.metadata.h, have.metadata, now);
  if (have.metadata.p) await sweep(env, have.metadata.p.h, { ...have.metadata.p, vis: visibility(have.metadata) }, now);
  const uuid = have.metadata.u;
  if (uuid && UUID.test(uuid)) {
    const mine = await env.CORPUS.getWithMetadata('u:' + uuid);
    if (mine && mine.metadata) await remember(env, uuid, mine.metadata.c !== 0, now);
  }
  return uuid || null;
}

export function groupCapes(capes) {
  const byHash = new Map();
  for (const cape of capes) {
    let group = byHash.get(cape.h);
    if (!group) {
      group = { h: cape.h, t: cape.t, s: cape.s, v: 0, c: 0, ids: [] };
      if (cape.x && cape.y) {
        group.x = cape.x;                       // 早期上傳的沒有記尺寸
        group.y = cape.y;
      }
      byHash.set(cape.h, group);
    }
    group.ids.push(cape.id);
    group.v = Math.max(group.v, cape.v || 0);
    // 早期的沒記 c：拿換上的時間頂著
    const first = cape.c || cape.v || 0;
    if (first && (!group.c || first < group.c)) group.c = first;
  }
  return [...byHash.values()].sort((a, b) => b.v - a.v);
}

function idleGroup(hash, one) {
  const group = { h: hash, t: one.t, s: one.s, v: one.at, c: one.c || one.at, ids: [], idle: one.at };
  if (one.x && one.y) {
    group.x = one.x;
    group.y = one.y;
  }
  return group;
}

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

async function handleCapeLink(request, env, now, hash) {
  if (!HASH.test(hash)) return no('not found', 404);
  const user = await who(request, env, now);
  const viewer = user ? await publicId(user.uuid) : null;
  const staff = Boolean(await moderator(request, env, now));
  const wearers = (await allCapes(env)).filter((cape) => cape.h === hash);
  const shown = staff ? wearers
    : wearers.filter((cape) => cape.id === viewer || (!cape.r && visibility(cape) !== 'private'));
  let group = groupCapes(shown)[0] || null;
  if (group) {
    group.vis = groupVisibility(shown);
    group.pending = shown.some((cape) => cape.r);
    group.hidden = !shown.some(listed);
    if (shown.some((cape) => cape.id === viewer)) group.own = true;
  } else {
    const one = (await purge(env, now))[hash];
    if (one && (staff || linkable(one))) {
      group = { ...idleGroup(hash, one), vis: idleVisibility(one), pending: false, hidden: Boolean(one.q) };
    }
  }
  if (!group) return no('not found', 404);
  group.stars = Number((await starMap(env))[hash]) || 0;
  return json({ cape: group, keep: IDLE_KEEP_MS, now }, 200, { 'cache-control': 'no-store' });
}

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
      'cache-control': key ? 'private, max-age=86400, immutable' : 'public, max-age=86400, immutable',
      'x-content-type-options': 'nosniff',
    },
  });
}

async function handleWebLink(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const token = await issueWebToken(env, user.uuid, user.name, now);
  return json({ ok: true, url: new URL(request.url).origin + '/capes#t=' + token });
}

async function handleMe(request, env, now) {
  const user = await who(request, env, now);
  if (!user) {
    const session = await discordSession(request, env, now);
    if (!session) return no('login first', 401);
    return json({ ok: true, linked: false, discord: { name: session.account.n || '' },
      staff: Boolean(await env.CORPUS.get('staff:d:' + session.id)) });
  }
  const id = await publicId(user.uuid);
  const cape = await env.CORPUS.getWithMetadata('cape:' + id);
  const list = await credits(now);
  const name = user.name.toLowerCase();
  const member = Boolean(list && list.names.has(name)) || await hasPerk(env, user.uuid);
  const developer = Boolean(list && list.developers.has(name));
  return json({
    ok: true,
    linked: true,
    discord: user.discord ? { name: user.discordName } : null,
    name: user.name,
    id,
    developer,
    staff: developer || await isStaff(env, user),
    member,
    banned: Boolean(await env.CORPUS.get('ban:' + user.uuid)),
    uploads: env.UPLOADS || 'tiered',
    limit: member ? MAX_CAPE_BYTES : MAX_PLAIN_CAPE_BYTES,
    cape: cape && cape.metadata ? { t: cape.metadata.t, v: cape.metadata.v, s: cape.metadata.s,
      x: cape.metadata.x || 0, y: cape.metadata.y || 0, h: cape.metadata.h,
      worn: Boolean(cape.metadata.w), back: Boolean(cape.metadata.w && cape.metadata.p),
      public: visibility(cape.metadata) === 'public', vis: visibility(cape.metadata), listed: listed(cape.metadata),
      pending: Boolean(cape.metadata.r) } : null,
    review: Boolean(await env.CORPUS.get('review:' + user.uuid)),
  });
}

export async function goneCode(key) {
  return hex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode('gone:' + key))).slice(0, 8);
}

export function anonymise(record, ids, code) {
  const mark = 'x:' + code;
  let changed = false;
  const hide = (entry) => {
    if (!entry || !ids.includes(entry.by)) return;
    entry.by = mark;
    entry.name = null;
    entry.gone = code;
    changed = true;
  };
  const votes = (entry) => {
    if (!Array.isArray(entry.votes) || !entry.votes.some((v) => ids.includes(v))) return;
    entry.votes = [...new Set(entry.votes.map((v) => (ids.includes(v) ? mark : v)))];
    changed = true;
  };
  hide(record);
  for (const entry of [...(record.items || []), ...(record.options || [])]) {
    hide(entry);
    votes(entry);
  }
  if (changed) record.ghosts = { ...(record.ghosts || {}), [code]: ids };
  return changed;
}

export const ERASE_OPS_PER_REQUEST = 600;
export const OWNER_INDEX_OPS_PER_RUN = 500;
const ERASE_JOB_VERSION = 1;
const ERASE_JOB_TTL_SECONDS = 30 * 86400;
const ERASE_PAGE_SIZE = 200;
export const OWNER_INDEX_READY = 'own-index:ready';
const OWNER_INDEX_STATE = 'own-index:state';
const OWNED_PREFIXES = ['tp:', 'tq:', 'fb:', 'prev:'];
const PARTICIPANT = /^(?:u:[0-9a-f]{32}|d:\d{5,25})$/;

class OperationBudgetExhausted extends Error {}

function meteredStore(store, budget) {
  const spend = () => {
    if (budget.left <= 0) throw new OperationBudgetExhausted('kv operation budget exhausted');
    budget.left -= 1;
  };
  return {
    get: (...args) => { spend(); return store.get(...args); },
    getWithMetadata: (...args) => { spend(); return store.getWithMetadata(...args); },
    put: (...args) => { spend(); return store.put(...args); },
    delete: (...args) => { spend(); return store.delete(...args); },
    list: (...args) => { spend(); return store.list(...args); },
  };
}

export const ownerEntry = (owner, key) => 'own:' + owner + ':' + key;

async function noteParticipant(env, owner, key, before) {
  if (!PARTICIPANT.test(owner || '')) return;
  if (typeof before === 'string' && before.includes('"' + owner + '"')) return;
  await env.CORPUS.put(ownerEntry(owner, key), '');
}

export function participantsOf(record) {
  const found = new Set();
  const add = (value) => {
    if (typeof value === 'string' && PARTICIPANT.test(value)) found.add(value);
  };
  add(record.by);
  for (const entry of [...(Array.isArray(record.items) ? record.items : []), ...(Array.isArray(record.options) ? record.options : [])]) {
    if (!entry || typeof entry !== 'object') continue;
    add(entry.by);
    if (Array.isArray(entry.votes)) entry.votes.forEach(add);
  }
  return found;
}

async function scanPrefix(store, state, prefix, visit) {
  for (;;) {
    const page = await store.list({ prefix, limit: ERASE_PAGE_SIZE, cursor: state.cursor || undefined });
    for (const key of page.keys) {
      if (state.after && key.name <= state.after) continue;
      await visit(key);
      state.after = key.name;
    }
    if (page.list_complete || !page.cursor) return;
    state.cursor = page.cursor;
  }
}

async function ownersOfStored(store, key) {
  if (key.name.startsWith('prev:')) {
    const parts = key.name.split(':');
    return parts.length === 4 && UUID.test(parts[2]) ? ['u:' + parts[2]] : [];
  }
  const record = parseRecord(await store.get(key.name));
  if (key.name.startsWith('fb:')) return DISCORD_ID.test(String(record.uid || '')) ? ['d:' + record.uid] : [];
  return [...participantsOf(record)];
}

export async function indexOwners(env, now) {
  if (await env.CORPUS.get(OWNER_INDEX_READY)) return { done: true };
  const state = parseRecord(await env.CORPUS.get(OWNER_INDEX_STATE));
  state.prefixAt = Number.isInteger(state.prefixAt) ? state.prefixAt : 0;
  const store = meteredStore(env.CORPUS, { left: OWNER_INDEX_OPS_PER_RUN });
  try {
    while (state.prefixAt < OWNED_PREFIXES.length) {
      await scanPrefix(store, state, OWNED_PREFIXES[state.prefixAt], async (key) => {
        const options = key.expiration ? { expiration: key.expiration } : undefined;
        for (const owner of await ownersOfStored(store, key)) await store.put(ownerEntry(owner, key.name), '', options);
      });
      state.prefixAt += 1;
      state.cursor = null;
      state.after = '';
    }
  } catch (e) {
    if (!(e instanceof OperationBudgetExhausted)) throw e;
    await env.CORPUS.put(OWNER_INDEX_STATE, JSON.stringify(state));
    return { done: false };
  }
  await env.CORPUS.put(OWNER_INDEX_READY, String(now));
  await env.CORPUS.delete(OWNER_INDEX_STATE);
  return { done: true };
}

async function eraseOwned(store, key, job) {
  if (key.startsWith('tp:') || key.startsWith('tq:')) {
    const raw = await store.get(key);
    if (!raw) return;
    const record = parseRecord(raw);
    if (anonymise(record, job.owners, job.code)) {
      await store.put(key, JSON.stringify(record), { metadata: key.startsWith('tp:') ? trMeta(record) : pollMeta(record) });
    }
  } else if (key.startsWith('fb:')) {
    if (job.discord && parseRecord(await store.get(key)).uid === job.discord) await store.delete(key);
  } else if (key.startsWith('prev:')) {
    if (job.uuid && key.split(':')[2] === job.uuid) await store.delete(key);
  }
}

async function eraseIndexed(store, env, job) {
  while (job.ownerAt < job.owners.length) {
    const prefix = 'own:' + job.owners[job.ownerAt] + ':';
    await scanPrefix(store, job.scan, prefix, async (key) => {
      await eraseOwned(store, key.name.slice(prefix.length), job);
      await store.delete(key.name);
    });
    job.ownerAt += 1;
    job.scan = {};
  }
}

async function eraseUnindexed(store, env, job) {
  if (await env.CORPUS.get(OWNER_INDEX_READY)) return;
  const prefixes = ['tp:', 'tq:', ...(job.discord ? ['fb:'] : []), ...(job.uuid ? ['prev:'] : [])];
  while (job.prefixAt < prefixes.length) {
    await scanPrefix(store, job.scan, prefixes[job.prefixAt], (key) => eraseOwned(store, key.name, job));
    job.prefixAt += 1;
    job.scan = {};
  }
}

async function planPlayerErase(store, env, job) {
  if (!job.uuid || job.plan) return;
  const id = await publicId(job.uuid);
  const history = await allHistory(env, 'hist:' + id + ':');
  const have = await store.getWithMetadata('cape:' + id);
  const mine = have?.metadata && typeof have.metadata.h === 'string' ? have.metadata : null;
  const own = new Set();
  if (mine && !mine.w) own.add(mine.h);
  if (mine?.p && HASH.test(mine.p.h || '')) own.add(mine.p.h);
  const idle = await idleMap(env);
  const unshared = [...new Set(history.map((one) => one.h))].filter((hash) => idle[hash] && !own.has(hash));
  job.plan = { id, history: history.map((one) => one.key), mine, own: [...own], unshared,
    favs: (await favsOf(env, job.uuid)).h };
}

async function keepSharedImages(store, env, job) {
  if (!job.plan || !job.plan.unshared.length) return;
  const mine = 'hist:' + job.plan.id + ':';
  await scanPrefix(store, job.scan, 'hist:', (key) => {
    if (key.name.startsWith(mine)) return;
    const parts = key.name.split(':');
    if (parts.length === 3) job.plan.unshared = job.plan.unshared.filter((hash) => hash !== parts[2]);
  });
  job.scan = {};
}

async function erasePlayer(store, env, job, now, saveJob) {
  const plan = job.plan;
  if (!plan) return;
  const done = new Set(job.done);
  const finish = (part) => {
    done.add(part);
    job.done = [...done];
  };
  if (!done.has('presence')) {
    await store.delete('u:' + job.uuid);
    const list = await storedList(env);
    const trimmed = list && dropFromList(list, plan.id);
    if (trimmed) await store.put('presence:list', JSON.stringify(trimmed));
    finish('presence');
  }
  if (!done.has('history')) {
    for (const key of plan.history) await store.delete(key);
    finish('history');
  }
  if (!done.has('cape')) {
    if (plan.mine) {
      await store.delete('cape:' + plan.id);
      if (plan.mine.w) await sweep(env, plan.mine.h, plan.mine, now);
    }
    finish('cape');
  }
  for (const hash of [...plan.own, ...plan.unshared]) {
    if (job.discarded.includes(hash)) continue;
    if (await discard(env, hash)) job.gone.push(hash);
    job.discarded.push(hash);
  }
  if (!done.has('stars')) {
    const stars = await starMap(env);
    let changed = false;
    for (const hash of plan.favs) {
      if (!(hash in stars)) continue;
      stars[hash] = Math.max(0, (Number(stars[hash]) || 0) - 1);
      if (!stars[hash]) delete stars[hash];
      changed = true;
    }
    for (const hash of job.gone) {
      if (!(hash in stars)) continue;
      delete stars[hash];
      changed = true;
    }
    if (changed) await store.put('stars', JSON.stringify(stars));
    finish('stars');
    await saveJob();
  }
  if (!done.has('favs')) {
    await store.delete('fav:' + job.uuid);
    finish('favs');
  }
}

async function eraseAccounts(store, env, job) {
  if (job.discord) {
    const record = await account(env, job.discord);
    if (record && UUID.test(record.u || '') && await store.get('l:' + record.u) === job.discord) {
      await store.delete('l:' + record.u);
    }
  }
  if (job.uuid) await store.delete('l:' + job.uuid);
  if (job.discord) await store.delete('d:' + job.discord);
}

const ERASE_STEPS = [eraseIndexed, eraseUnindexed, planPlayerErase, keepSharedImages, erasePlayer, eraseAccounts];

function eraseJob(stored, subject, code, now) {
  const owners = [...(subject.uuid ? ['u:' + subject.uuid] : []), ...(subject.discord ? ['d:' + subject.discord] : [])];
  const fresh = { v: ERASE_JOB_VERSION, owners, uuid: subject.uuid, discord: subject.discord, code, step: 0,
    ownerAt: 0, prefixAt: 0, scan: {}, plan: null, done: [], discarded: [], gone: [], t: now };
  if (stored.v !== ERASE_JOB_VERSION || stored.uuid !== subject.uuid) return fresh;
  if (stored.discord === subject.discord) return stored;
  return { ...fresh, plan: stored.plan, done: stored.done, discarded: stored.discarded, gone: stored.gone };
}

export async function eraseSubject(env, subject, now) {
  const jobKey = 'erase:' + (subject.uuid ? 'u:' + subject.uuid : 'd:' + subject.discord);
  const code = await goneCode(subject.uuid || subject.discord);
  const job = eraseJob(parseRecord(await env.CORPUS.get(jobKey)), subject, code, now);
  const store = meteredStore(env.CORPUS, { left: ERASE_OPS_PER_REQUEST });
  const metered = { ...env, CORPUS: store };
  const saveJob = () => env.CORPUS.put(jobKey, JSON.stringify(job), { expirationTtl: ERASE_JOB_TTL_SECONDS });
  try {
    while (job.step < ERASE_STEPS.length) {
      await ERASE_STEPS[job.step](store, metered, job, now, saveJob);
      job.step += 1;
      job.scan = {};
    }
  } catch (e) {
    await saveJob();
    if (e instanceof OperationBudgetExhausted) return { done: false };
    throw e;
  }
  await env.CORPUS.delete(jobKey);
  return { done: true };
}

async function handleDeleteMe(request, env, now) {
  const user = await who(request, env, now);
  const session = user ? null : await discordSession(request, env, now);
  if (!user && !session) return no('login first', 401);
  const discord = user ? user.discord || await env.CORPUS.get('l:' + user.uuid) : session.id;
  const subject = { uuid: user ? user.uuid : null, discord: discord && DISCORD_ID.test(discord) ? discord : null };
  const outcome = await eraseSubject(env, subject, now);
  if (!outcome.done) return json({ ok: false, pending: true }, 202, { 'cache-control': 'no-store' });
  return json({ ok: true }, 200, { 'cache-control': 'no-store' });
}

async function moderator(request, env, now) {
  const mine = await authority(request, env, now);
  return mine ? mine.by : null;
}

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
    for (const cape of capes) {
      if (!cape.p || cape.p.h !== body.hash || cape.h === body.hash) continue;
      const { id: owner, p: gone, ...rest } = cape;
      await env.CORPUS.put('cape:' + owner, '', { metadata: rest });
    }
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
    await env.CORPUS.delete('hist:' + cape.id + ':' + cape.h);
    const uuid = await dropCape(env, cape.id, now);
    if (!uuid) continue;
    removed++;
    if (body.ban === true) {
      await env.CORPUS.put('ban:' + uuid, JSON.stringify({ n: cape.n || null, t: now, by }));
    }
    await discard(env, cape.h);
    if (cape.p) await discard(env, cape.p.h);
  }
  let gone = false;
  if (typeof body.hash === 'string') {
    gone = Boolean((await idleMap(env))[body.hash]);
    await discard(env, body.hash);
  }
  return json({ ok: true, removed: removed || (gone ? 1 : 0), banned: body.ban === true });
}

async function handleAdminCapes(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const capes = await allCapes(env);
  const groups = groupCapes(capes);
  const stars = await starMap(env);
  for (const group of groups) {
    const wearers = capes.filter((cape) => cape.h === group.h);
    group.owners = wearers.map((cape) => ({ id: cape.id, name: cape.n || null, uuid: cape.u, v: cape.v,
      vis: visibility(cape), linked: Boolean(cape.lk), pending: Boolean(cape.r), worn: Boolean(cape.w) }));
    group.stars = Number(stars[group.h]) || 0;
    group.hidden = wearers.every((cape) => !listed(cape));
    group.vis = groupVisibility(wearers);
    group.pending = wearers.some((cape) => cape.r);
  }
  const idle = await purge(env, now);
  for (const hash of Object.keys(idle)) {
    if (capes.some((cape) => cape.h === hash)) continue;
    groups.push({ ...idleGroup(hash, idle[hash]), owners: [], stars: Number(stars[hash]) || 0,
      hidden: Boolean(idle[hash].q), vis: idleVisibility(idle[hash]), pending: false });
  }
  return json({ capes: groups, keep: IDLE_KEEP_MS, now }, 200, { 'cache-control': 'no-store' });
}

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


const REPO = 'LyuChaCha/WynnChaYuan';
const CREDITS_PATH = 'src/main/resources/assets/wynnchayuan/credits.json';

const SECTIONS = { developers: '開發者', sponsors: '贊助者', contributors: '貢獻者' };

export const TEAM_AREAS = ['translation', 'proofreading', 'capture', 'testing', 'code', 'design', 'website', 'community'];

function knownCodes(value, allowed) {
  return Array.isArray(value) ? allowed.filter((code) => value.includes(code)) : [];
}

function exactCodes(value, allowed) {
  if (!Array.isArray(value) || value.length > allowed.length * 2) return null;
  if (!value.every((code) => typeof code === 'string' && allowed.includes(code))) return null;
  return knownCodes(value, allowed);
}

export function teamsInput(entry) {
  if (!entry || typeof entry !== 'object') return { why: 'bad request' };
  if (!Object.hasOwn(SECTIONS, entry.section)) return { why: 'bad section' };
  const who = typeof entry.who === 'string' ? entry.who.trim() : '';
  if (!who || who.length > 40) return { why: 'bad who' };
  const teams = exactCodes(entry.teams, TR_LANGS);
  if (!teams) return { why: 'bad team' };
  const areas = exactCodes(entry.areas, TEAM_AREAS);
  if (!areas) return { why: 'bad area' };
  return { section: entry.section, who, teams, areas };
}

export function setMemberTeams(text, entry) {
  const input = teamsInput(entry);
  if (input.why) return input;
  let doc;
  try {
    doc = JSON.parse(text);
  } catch (e) {
    return { why: 'bad credits' };
  }
  const target = (Array.isArray(doc.sections) ? doc.sections : []).find((item) => item && typeof item.key === 'string'
    && item.key.endsWith('.' + input.section));
  if (!target || !Array.isArray(target.members)) return { why: 'bad credits' };
  const lower = input.who.toLowerCase();
  const member = target.members.find((one) => one && typeof one.mc === 'string' && one.mc.toLowerCase() === lower)
    || target.members.find((one) => one && !one.mc && one.name === input.who);
  if (!member) return { why: 'not found' };
  const held = (field) => JSON.stringify(field in member ? member[field] : []);
  if (held('teams') === JSON.stringify(input.teams) && held('areas') === JSON.stringify(input.areas)) {
    return { why: 'unchanged' };
  }
  if (input.teams.length) member.teams = input.teams;
  else delete member.teams;
  if (input.areas.length) member.areas = input.areas;
  else delete member.areas;
  return { text: JSON.stringify(doc, null, 2) + '\n', name: String(member.name || input.who) };
}

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
  const teams = entry.teams === undefined ? [] : exactCodes(entry.teams, TR_LANGS);
  if (!teams) return { why: 'bad team' };
  const areas = entry.areas === undefined ? [] : exactCodes(entry.areas, TEAM_AREAS);
  if (!areas) return { why: 'bad area' };
  if (teams.length) member.teams = teams;
  if (areas.length) member.areas = areas;
  target.members.push(member);
  // 照倉庫的格式（兩格縮排、結尾換行）寫回去，diff 才只有新增的行
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

const TEAMS_WHY_STATUS = { unchanged: 409, 'not found': 404 };

export function addMemberTeams(text, mc, langs) {
  if (typeof mc !== 'string' || !NAME.test(mc)) return { why: 'bad who' };
  const adding = knownCodes(langs, TR_LANGS);
  if (!adding.length) return { why: 'bad team' };
  let doc;
  try {
    doc = JSON.parse(text);
  } catch (e) {
    return { why: 'bad credits' };
  }
  const lower = mc.toLowerCase();
  let found = null;
  let changed = false;
  for (const section of Array.isArray(doc.sections) ? doc.sections : []) {
    if (!section || !/\.(developers|sponsors|contributors)$/.test(String(section.key)) || !Array.isArray(section.members)) continue;
    for (const member of section.members) {
      if (!member || typeof member.mc !== 'string' || member.mc.toLowerCase() !== lower) continue;
      found = found || member;
      const had = Array.isArray(member.teams) ? member.teams : [];
      const next = knownCodes([...had, ...adding], TR_LANGS);
      if (JSON.stringify(next) === JSON.stringify(had)) continue;
      member.teams = next;
      changed = true;
    }
  }
  if (!found) return { why: 'not found' };
  if (!changed) return { why: 'unchanged' };
  return { text: JSON.stringify(doc, null, 2) + '\n', name: String(found.name || mc) };
}

async function commitTeams(env, by, now, change) {
  if (!env.GITHUB_TOKEN) return { why: 'github not configured', status: 503 };
  const github = githubClient(env);
  try {
    const day = new Date(now).toISOString().slice(0, 10).replace(/-/g, '');
    let branch = 'credits/teams-' + day;
    let url = '';
    const ref = await github('/git/ref/heads/' + branch);
    if (ref.status === 200) {
      const open = await github('/pulls?head=' + encodeURIComponent(REPO.split('/')[0] + ':' + branch) + '&state=open');
      const list = open.status === 200 ? await open.json() : [];
      if (Array.isArray(list) && list[0]) url = String(list[0].html_url || '');
      else branch += '-' + now;
    } else if (ref.status !== 404) {
      return { why: 'github ' + ref.status, status: 502 };
    }
    const reuse = Boolean(url);
    const file = await github('/contents/' + CREDITS_PATH + '?ref=' + encodeURIComponent(reuse ? branch : 'main'));
    if (file.status !== 200) return { why: 'github ' + file.status, status: 502 };
    const current = await file.json();
    const changed = change(base64ToUtf8(current.content));
    if (changed.why) return { why: changed.why, status: TEAMS_WHY_STATUS[changed.why] || 400 };
    if (!reuse) {
      const head = await github('/git/ref/heads/main');
      if (head.status !== 200) return { why: 'github ' + head.status, status: 502 };
      const made = await github('/git/refs', {
        method: 'POST', body: JSON.stringify({ ref: 'refs/heads/' + branch, sha: (await head.json()).object.sha }),
      });
      if (made.status !== 201) return { why: 'github ' + made.status, status: 502 };
    }
    const message = '名單：' + changed.name + ' 的翻譯團隊與貢獻項目';
    const put = await github('/contents/' + CREDITS_PATH, {
      method: 'PUT',
      body: JSON.stringify({ message, content: utf8ToBase64(changed.text), sha: current.sha, branch }),
    });
    if (put.status !== 200 && put.status !== 201) return { why: 'github ' + put.status, status: 502 };
    if (!reuse) {
      const pull = await github('/pulls', {
        method: 'POST',
        body: JSON.stringify({
          title: '名單：翻譯團隊與貢獻項目（' + day + '）',
          head: branch,
          base: 'main',
          body: '從網站的管理頁送出，第一筆由 ' + by + ' 送出。同一天在管理頁改的翻譯團隊與貢獻項目（包括指派譯者時一併加上的團隊）都會加進這個 PR。\n\n'
            + '改的是 `credits.json` 成員的 `teams` 與 `areas` 兩欄。合併之後，首頁的團隊名冊最多十分鐘內跟著更新。',
        }),
      });
      if (pull.status !== 201) return { why: 'github ' + pull.status, status: 502 };
      url = String((await pull.json()).html_url || '');
    }
    return { url, added: reuse };
  } catch (e) {
    return { why: 'github unreachable', status: 502 };
  }
}

async function handleCreditTeams(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  if (!env.GITHUB_TOKEN) return no('github not configured', 503);
  const body = await readJson(request);
  if (!body) return no('bad request');
  const input = teamsInput(body);
  if (input.why) return no(input.why);
  const done = await commitTeams(env, by, now, (text) => setMemberTeams(text, input));
  if (done.why) return no(done.why, done.status);
  return json({ ok: true, url: done.url, added: done.added });
}

function creditTeamsOf(list, mc) {
  if (!list || typeof mc !== 'string' || !mc) return null;
  const lower = mc.toLowerCase();
  let hit = false;
  const teams = new Set();
  for (const section of list.team) {
    for (const member of section.members) {
      if (!member.mc || member.mc.toLowerCase() !== lower) continue;
      hit = true;
      for (const code of member.teams || []) teams.add(code);
    }
  }
  return hit ? TR_LANGS.filter((code) => teams.has(code)) : null;
}


export const TR_LANGS = ['zh_tw', 'zh_cn', 'ja_jp', 'ko_kr', 'ru_ru', 'es_es', 'tr_tr'];
const TR_INDEX = 'https://raw.githubusercontent.com/' + 'LyuChaCha/WynnChaYuan' + '/translate-index/';
const TR_TEXT_MAX = 2000;
const TR_BODY = 16384;
const TR_FILE = /^(?:[a-z0-9-]+\/)?[a-z0-9_-]+\.json$/;
const TR_KEY = /^tl:(?:u:[0-9a-f]{32}|d:\d{17,20}|n:[a-z0-9_]{1,16})$/;
const TR_ID = /^[0-9a-f]{12,24}$/;

export function placeholderProblem(src, text, base) {
  if (typeof text !== 'string' || !text.trim()) return 'empty';
  if (text.length > TR_TEXT_MAX) return 'too long';
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

/** 逐行改，不重 dump：整檔重寫會讓 git 把整個檔當成改過。 */
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

async function handleTrMe(request, env, now) {
  const me = await trWho(request, env, now);
  return json(me ? { signedIn: true, id: me.id, name: me.name, langs: [...me.langs], decide: me.decide }
    : { signedIn: false, langs: [], decide: false }, 200, { 'cache-control': 'no-store' });
}

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
  const before = await env.CORPUS.get(storeKey);
  const record = parseRecord(before);
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
  await noteParticipant(env, me.id, storeKey, before);
  return json({ ok: true, id, prop: record });
}

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
  await noteParticipant(env, me.id, storeKey, raw);
  return json({ ok: true, prop: record });
}

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
    let branch = 'translate/' + lang.replace('_', '-') + '-' + day;
    let url = '';
    const ref = await github('/git/ref/heads/' + branch);
    if (ref.status === 200) {
      const open = await github('/pulls?head=' + encodeURIComponent(REPO.split('/')[0] + ':' + branch) + '&state=open');
      const list = open.status === 200 ? await open.json() : [];
      if (Array.isArray(list) && list[0]) url = String(list[0].html_url || '');
      else branch += '-' + now;
    } else if (ref.status !== 404) {
      return no('github ' + ref.status, 502);
    }
    const reuse = Boolean(url);
    if (!reuse) {
      const head = await github('/git/ref/heads/main');
      if (head.status !== 200) return no('github ' + head.status, 502);
      const made = await github('/git/refs', {
        method: 'POST', body: JSON.stringify({ ref: 'refs/heads/' + branch, sha: (await head.json()).object.sha }),
      });
      if (made.status !== 201) return no('github ' + made.status, 502);
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
    if (!reuse) {
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
      url = String((await pull.json()).html_url || '');
    }
    if (!url) return no('github pull request missing', 502);
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
  await noteParticipant(env, me.id, 'tq:' + lang + ':' + id, null);
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
  return { me, body, storeKey, raw, record: JSON.parse(raw) };
}

async function savePoll(env, got, now) {
  got.record.updated = now;
  await env.CORPUS.put(got.storeKey, JSON.stringify(got.record), { metadata: pollMeta(got.record) });
  return json({ ok: true, poll: got.record });
}

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
  await noteParticipant(env, got.me.id, got.storeKey, got.raw);
  return savePoll(env, got, now);
}

async function handleTrPollVote(request, env, now) {
  const got = await pollFor(request, env, now, false);
  if (got.error) return got.error;
  if (got.record.status !== 'open') return no('closed', 409);
  const target = got.record.options.find((o) => o.id === got.body.option);
  if (!target) return no('not found', 404);
  const had = target.votes.includes(got.me.id);
  for (const o of got.record.options) o.votes = o.votes.filter((v) => v !== got.me.id);
  if (!had) target.votes.push(got.me.id);
  await noteParticipant(env, got.me.id, got.storeKey, got.raw);
  return savePoll(env, got, now);
}

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


async function translatorList(env, list) {
  const out = [];
  let cursor;
  do {
    const page = await env.CORPUS.list({ prefix: 'tl:', limit: 1000, cursor });
    for (const k of page.keys) {
      const record = parseRecord(await env.CORPUS.get(k.name));
      const key = k.name.slice(3);
      out.push({ key, kind: key[0], id: key.slice(2), langs: record.langs || [], name: record.m || null,
        team: creditTeamsOf(list, record.m),
        discord: record.dn || null, note: record.n || '', t: record.t || null, by: record.by || null });
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  return out.sort((a, b) => (b.t || 0) - (a.t || 0));
}

async function handleTranslators(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  return json({ translators: await translatorList(env, await credits(now)) }, 200, { 'cache-control': 'no-store' });
}

async function translatorIdentity(env, key, who) {
  const found = {};
  if (key.startsWith('n:')) found.m = String(who).trim();
  const linked = key.startsWith('d:') ? key.slice(2) : key.startsWith('u:') ? await env.CORPUS.get('l:' + key.slice(2)) : null;
  const known = linked ? await account(env, linked) : null;
  if (known) {
    if (known.n) found.dn = known.n;
    if (known.m && (key.startsWith('d:') || known.u === key.slice(2))) found.m = known.m;
  }
  if (key.startsWith('u:') && !found.m) {
    const cape = await env.CORPUS.getWithMetadata('cape:' + await publicId(key.slice(2)));
    if (cape && cape.metadata && NAME.test(cape.metadata.n || '')) found.m = cape.metadata.n;
  }
  return found;
}

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
  const record = { langs, n: note, t: now, by, ...(await translatorIdentity(env, key, body.who)) };
  await env.CORPUS.put('tl:' + key, JSON.stringify(record));
  const listed = creditTeamsOf(await credits(now), record.m);
  if (!listed) return json({ ok: true, key, team: null });
  if (langs.every((code) => listed.includes(code))) return json({ ok: true, key, team: { teams: listed } });
  const done = await commitTeams(env, by, now, (text) => addMemberTeams(text, record.m, langs));
  if (done.why && done.why !== 'unchanged') return json({ ok: true, key, team: { why: done.why } });
  return json({ ok: true, key, team: done.why ? { teams: listed } : { url: done.url, added: done.added } });
}

export function mergeLangTeam(team, translators) {
  const people = [];
  const byMc = new Map();
  const union = (a, b, all) => all.filter((code) => a.includes(code) || (Array.isArray(b) && b.includes(code)));
  for (const section of Array.isArray(team) ? team : []) {
    for (const member of Array.isArray(section.members) ? section.members : []) {
      const mc = typeof member.mc === 'string' && member.mc ? member.mc.toLowerCase() : null;
      let person = mc ? byMc.get(mc) : null;
      if (!person) {
        person = { name: member.name, mc: member.mc || null, roles: [], listed: true, teams: [], areas: [], rights: null };
        people.push(person);
        if (mc) byMc.set(mc, person);
      }
      person.roles.push(section.role);
      person.teams = union(person.teams, member.teams, TR_LANGS);
      person.areas = union(person.areas, member.areas, TEAM_AREAS);
    }
  }
  for (const one of Array.isArray(translators) ? translators : []) {
    const mc = typeof one.name === 'string' && one.name ? one.name.toLowerCase() : null;
    let person = mc ? byMc.get(mc) : null;
    if (!person) {
      person = { name: one.name || one.discord || one.id, mc: one.name || null, roles: [], listed: false, teams: [], areas: [], rights: null };
      people.push(person);
      if (mc) byMc.set(mc, person);
    }
    const rights = person.rights || { keys: [], langs: [], note: '', discord: null, t: null, by: null };
    rights.keys.push(one.key);
    rights.langs = union(rights.langs, one.langs, TR_LANGS);
    if (!rights.note && one.note) rights.note = one.note;
    if (!rights.discord && one.discord) rights.discord = one.discord;
    if ((one.t || 0) > (rights.t || 0)) {
      rights.t = one.t;
      rights.by = one.by || null;
    }
    person.rights = rights;
  }
  for (const person of people) {
    person.mismatch = Boolean(person.listed && person.rights && person.rights.langs.join() !== person.teams.join());
  }
  return people;
}

export function setPersonTeams(text, mc, teams, areas) {
  if (typeof mc !== 'string' || !NAME.test(mc)) return { why: 'bad who' };
  let doc;
  try {
    doc = JSON.parse(text);
  } catch (e) {
    return { why: 'bad credits' };
  }
  const before = JSON.stringify(doc);
  const lower = mc.toLowerCase();
  let found = null;
  for (const section of Array.isArray(doc.sections) ? doc.sections : []) {
    if (!section || !/\.(developers|sponsors|contributors)$/.test(String(section.key)) || !Array.isArray(section.members)) continue;
    for (const member of section.members) {
      if (!member || typeof member.mc !== 'string' || member.mc.toLowerCase() !== lower) continue;
      found = found || member;
      if (Array.isArray(teams)) {
        if (teams.length) member.teams = teams;
        else delete member.teams;
      }
      if (Array.isArray(areas)) {
        if (areas.length) member.areas = areas;
        else delete member.areas;
      }
    }
  }
  if (!found) return { why: 'not found' };
  if (JSON.stringify(doc) === before) return { why: 'unchanged' };
  return { text: JSON.stringify(doc, null, 2) + '\n', name: String(found.name || mc) };
}

async function handleLangTeam(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const list = await credits(now);
  const translators = await translatorList(env, list);
  return json({ people: mergeLangTeam(list ? list.team : [], translators), credits: Boolean(list) }, 200,
    { 'cache-control': 'no-store' });
}

async function handleLangTeamSet(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  const body = await readJson(request, TR_BODY);
  if (!body) return no('bad request');
  const key = staffKey(body.who);
  if (!key) return no('bad who');
  const langs = body.langs === undefined ? null : exactCodes(body.langs, TR_LANGS);
  if (body.langs !== undefined && !langs) return no('bad team');
  const areas = body.areas === undefined ? null : exactCodes(body.areas, TEAM_AREAS);
  if (body.areas !== undefined && !areas) return no('bad area');
  if (!langs && !areas) return no('bad request');
  const note = typeof body.note === 'string' ? body.note.trim() : null;
  if (note && note.length > 60) return no('bad note');
  const identity = await translatorIdentity(env, key, body.who);
  const mc = identity.m || null;
  const list = await credits(now);
  const listed = Boolean(mc && list && list.names.has(mc.toLowerCase()));
  if (areas && !listed) return no(list ? 'not listed' : 'try later', list ? 400 : 503);
  let rights = null;
  if (langs) {
    const matched = [];
    for (const one of await translatorList(env, null)) {
      if (one.key === key || (mc && one.name && one.name.toLowerCase() === mc.toLowerCase())) matched.push(one.key);
    }
    if (!langs.length) {
      for (const k of matched) await env.CORPUS.delete('tl:' + k);
    } else {
      const keys = matched.length ? matched : [key];
      for (const k of keys) {
        const old = parseRecord(await env.CORPUS.get('tl:' + k));
        const record = { ...old, ...identity, ...(old.m ? { m: old.m } : {}), langs, t: now, by };
        record.n = note === null ? (old.n || '') : note;
        await env.CORPUS.put('tl:' + k, JSON.stringify(record));
      }
      rights = { keys, langs };
    }
  }
  let team = null;
  if (listed) {
    const done = await commitTeams(env, by, now, (text) => setPersonTeams(text, mc, langs, areas));
    team = done.why === 'unchanged' ? { unchanged: true } : done.why ? { why: done.why } : { url: done.url, added: done.added };
  } else if (langs && mc && !list) {
    team = { why: 'try later' };
  }
  return json({ ok: true, mc, listed, rights, team });
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


export const FEEDBACK_CATS = ['translation', 'content', 'website', 'feature', 'other'];
export const FEEDBACK_MAX = 2000;
const FEEDBACK_MIN = 3;
const FEEDBACK_WHERE_MAX = 200;
export const FEEDBACK_PER_HOUR = 5;
export const FEEDBACK_KEEP_DAYS = 180;
const FEEDBACK_ID = /^[0-9a-f]{16}$/;
const FEEDBACK_CAT_ZH = { translation: '譯文建議', content: '內容建議', website: '網站建議', feature: '功能建議', other: '其他' };
const FEEDBACK_LABEL = 'translation';

export function feedbackCheck(input) {
  if (!input || typeof input !== 'object') return { why: 'bad request' };
  const cat = String(input.cat || '');
  if (!FEEDBACK_CATS.includes(cat)) return { why: 'bad category' };
  const tidy = (s) => String(s).replace(/\r\n?/g, '\n').replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/g, '');
  const msg = typeof input.msg === 'string' ? tidy(input.msg).trim() : '';
  if (msg.length < FEEDBACK_MIN) return { why: 'too short' };
  if (msg.length > FEEDBACK_MAX) return { why: 'too long' };
  const where = typeof input.where === 'string' ? tidy(input.where).replace(/\s+/g, ' ').trim() : '';
  if (where.length > FEEDBACK_WHERE_MAX) return { why: 'where too long' };
  const lang = TR_LANGS.includes(input.lang) ? input.lang : null;
  return { entry: { cat, msg, where, lang } };
}

async function feedbackPerson(request, env, now) {
  const session = await discordSession(request, env, now);
  if (session) return { id: session.id, name: session.account.n || session.id };
  const user = await who(request, env, now);
  if (!user) return null;
  const discord = user.discord || await env.CORPUS.get('l:' + user.uuid);
  const found = discord ? await account(env, discord) : null;
  return found ? { id: discord, name: found.n || discord } : { error: 'discord first' };
}

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
  if (DISCORD_ID.test(String(record.uid || ''))) {
    await env.CORPUS.put(ownerEntry('d:' + record.uid, 'fb:' + record.id), '',
      options.expirationTtl ? { expirationTtl: options.expirationTtl } : undefined);
  }
}

function fenced(text) {
  const runs = String(text).match(/`+/g) || [];
  const bar = '`'.repeat(Math.max(3, ...runs.map((run) => run.length + 1)));
  return bar + '\n' + text + '\n' + bar;
}

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

async function submitFeedback(env, person, input, via, now) {
  const checked = feedbackCheck(input);
  if (checked.why) return { why: checked.why, status: 400 };
  const gate = await feedbackGate(env, person.id, now);
  if (gate.until) return { why: 'slow down', status: 429, until: gate.until };
  const id = hex(crypto.getRandomValues(new Uint8Array(8)));
  const record = { id, ...checked.entry, uid: person.id, name: String(person.name).slice(0, 64), via, t: now, st: 'open' };
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

async function handleFeedbackDelete(request, env, now) {
  const got = await feedbackFor(request, env, now);
  if (got.error) return got.error;
  await env.CORPUS.delete('fb:' + got.record.id);
  if (DISCORD_ID.test(String(got.record.uid || ''))) await env.CORPUS.delete(ownerEntry('d:' + got.record.uid, 'fb:' + got.record.id));
  return json({ ok: true });
}

async function handleFeedbackIssue(request, env, now) {
  const got = await feedbackFor(request, env, now);
  if (got.error) return got.error;
  if (got.record.cat !== 'translation') return no('not a translation', 409);
  if (got.record.issue) return json({ ok: true, item: got.record });
  await fileFeedbackIssue(env, got.record);
  return got.record.issue ? json({ ok: true, item: got.record })
    : json({ ok: false, why: got.record.issueError, item: got.record }, 502);
}

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
      'content-security-policy': "default-src 'self'; "
        + "img-src 'self' data: blob: https://raw.githubusercontent.com https://minotar.net; "
        + "style-src 'unsafe-inline'; script-src 'unsafe-inline' https://cdn.jsdelivr.net; "
        + "connect-src 'self'; base-uri 'none'; form-action 'none'",
    },
  });
}

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

function texture(path) {
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

function botLocale(locale) {
  const l = String(locale || '');
  if (l === 'zh-CN') return { text: BOT_TEXT.cn, corpus: 'zh_cn' };
  if (l.startsWith('zh')) return { text: BOT_TEXT.zh, corpus: 'zh_tw' };
  const corpus = { ja: 'ja_jp', ko: 'ko_kr', ru: 'ru_ru', 'es-ES': 'es_es', 'es-419': 'es_es', tr: 'tr_tr' }[l] || 'zh_tw';
  return { text: BOT_TEXT.en, corpus };
}

const botFmt = (s, values) => String(s).replace(/\{(\w+)\}/g, (all, k) => (k in values ? String(values[k]) : all));
const clip = (s, n) => (String(s).length > n ? String(s).slice(0, n - 1) + '…' : String(s));

const FEEDBACK_CHOICES = [
  ['translation', 'Translation suggestion', '譯文建議', '译文建议'],
  ['content', 'Content suggestion', '內容建議', '内容建议'],
  ['website', 'Website suggestion', '網站建議', '网站建议'],
  ['feature', 'Feature suggestion', '功能建議', '功能建议'],
  ['other', 'Other', '其他', '其他'],
];

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

export async function verifyDiscord(publicKey, signature, timestamp, body) {
  if (!/^[0-9a-f]{64}$/i.test(publicKey || '') || !/^[0-9a-f]{128}$/i.test(signature || '') || !timestamp) return false;
  try {
    const key = await crypto.subtle.importKey('raw', hexBytes(publicKey), { name: 'Ed25519' }, false, ['verify']);
    return await crypto.subtle.verify('Ed25519', key, hexBytes(signature), new TextEncoder().encode(timestamp + body));
  } catch (e) {
    return false;
  }
}

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

const FIGURE_STYLE = 'angled';

async function handleCapePreview(env, hash, uuid) {
  if (!HASH.test(hash || '')) return no('not found', 404);
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
  if (who) await env.CORPUS.put(ownerEntry('u:' + who, key), '', { expirationTtl: 3 * 86400 });
  return new Response(made.bytes, { headers: headers(made.type) });
}

async function skinOf(uuid) {
  try {
    const res = await fetch('https://minotar.net/skin/' + uuid, { signal: AbortSignal.timeout(5000) });
    return res.status === 200 ? new Uint8Array(await res.arrayBuffer()) : null;
  } catch (e) {
    return null;
  }
}

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
  if (!listed(mine)) return { content: botFmt(text.privateCape, { name: mine.n }) };
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
    // 先算好預覽圖：Discord 拿到訊息才來抓圖，大張的 GIF 現算來不及
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

const LOCALE_CORPUS = { 'zh-TW': 'zh_tw', 'zh-CN': 'zh_cn', ja: 'ja_jp', ko: 'ko_kr', ru: 'ru_ru', 'es-ES': 'es_es', 'es-419': 'es_es', tr: 'tr_tr' };

async function botFeedback(env, text, interaction, option, now) {
  // 伺服器裡是 member.user，私訊裡是 user
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
  // Discord 只等三秒：慢的指令先回「處理中」再補上
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

async function handleDiscordInfo(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  const id = env.DISCORD_CLIENT_ID || '';
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

async function trRoute(route, request, env, now) {
  if (route === 'GET /tr/me') return handleTrMe(request, env, now);
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
  return no('not found', 404);
}

async function scrubTr(request, env, now, answer) {
  if (!(answer.headers.get('content-type') || '').includes('json')) return answer;
  const text = await answer.text();
  if (!text.includes('"ghosts"') || await moderator(request, env, now)) {
    return new Response(text, { status: answer.status, headers: answer.headers });
  }
  const body = JSON.parse(text);
  const strip = (value) => {
    if (Array.isArray(value)) value.forEach(strip);
    else if (value && typeof value === 'object') {
      delete value.ghosts;
      Object.values(value).forEach(strip);
    }
  };
  strip(body);
  return new Response(JSON.stringify(body), { status: answer.status, headers: answer.headers });
}

const site = {
  async scheduled(event, env) {
    await tidy(env, Date.now());
    await indexOwners(env, Date.now());
  },
  async fetch(request, env, ctx) {
    // Discord 檢查條款與隱私政策網址時送 HEAD：跟 GET 一樣回，不帶內容
    if (request.method === 'HEAD' && !new URL(request.url).pathname.startsWith('/api/')) {
      const full = await site.fetch(new Request(request.url, { method: 'GET', headers: request.headers }), env, ctx);
      return new Response(null, { status: full.status, headers: full.headers });
    }
    const url = new URL(request.url);
    const now = Date.now();
    const path = url.pathname;
    if (request.method === 'GET' && PATHS.includes(path)) {
      const asked = url.searchParams.get('lang');
      const lang = LANGS.includes(asked) ? asked
        : pickLang(request.headers.get('cookie'), request.headers.get('accept-language'));
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
    if (route === 'POST /me/delete') return handleDeleteMe(request, env, now);
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
    if (route === 'POST /admin/credits/teams') return handleCreditTeams(request, env, now);
    if (route === 'GET /admin/staff') return handleStaff(request, env, now);
    if (route === 'POST /admin/staff') return handleStaffAdd(request, env, now);
    if (route === 'POST /admin/unstaff') return handleStaffRemove(request, env, now);
    if (route === 'GET /admin/people') return handlePeople(request, env, now);
    if (route === 'GET /admin/translators') return handleTranslators(request, env, now);
    if (route === 'POST /admin/translator') return handleTranslatorAdd(request, env, now);
    if (route === 'POST /admin/untranslator') return handleTranslatorRemove(request, env, now);
    if (route === 'GET /admin/langteam') return handleLangTeam(request, env, now);
    if (route === 'POST /admin/langteam') return handleLangTeamSet(request, env, now);
    if (route.startsWith('GET /tr/index/')) return handleTrIndex(request, env, route.slice('GET /tr/index/'.length), ctx);
    if (route.startsWith('GET /tr/') || route.startsWith('POST /tr/')) {
      return scrubTr(request, env, now, await trRoute(route, request, env, now));
    }
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
    if (route === 'POST /cape/public' || route === 'POST /cape/visibility') return handlePublic(request, env, now);
    if (route === 'POST /cape/forget') return handleForget(request, env, now);
    if (route === 'GET /capes/mine') return handleMine(request, env, now);
    if (route.startsWith('GET /capes/')) return handleCapeLink(request, env, now, route.slice('GET /capes/'.length));
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
