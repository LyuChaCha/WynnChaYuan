import { CERT_KEYS, PROFILE_KEYS } from './mojang-keys.js';
import { isMember } from './capes.js';
import { NAME, UUID, fromBase64, hex, json, no, readJson, redirect, timingSafeEqual } from './http.js';

export const LOGIN_PREFIX = 'wynnchayuan.com login\n';

const CHALLENGE_TTL_MS = 5 * 60 * 1000;

const PROFILE_MAX_AGE_MS = 30 * 24 * 3600 * 1000;

const MAX_LOGIN_BODY = 12 * 1024;

const TOKEN_TTL_MS = 7 * 24 * 3600 * 1000;

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
  return timingSafeEqual(expected, signature) ? { uuid, name } : null;
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
  return timingSafeEqual(expected, signature) ? { uuid, name, web: true } : null;
}

export async function who(request, env, now) {
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

export const DISCORD_ID = /^\d{5,25}$/;

const SESSION_TTL_MS = 30 * 24 * 3600 * 1000;

const LINK_TTL_SECONDS = 600;

const LINK_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

const LINK_CODE = /^[A-HJ-NP-Z2-9]{8}$/;

export function discordOn(env) {
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
  return timingSafeEqual(expected, signature) ? id : null;
}

async function saveAccount(env, id, record) {
  const metadata = {};
  if (record.n) metadata.n = record.n;
  if (record.m) metadata.m = record.m;
  if (record.u) metadata.u = record.u;
  await env.CORPUS.put('d:' + id, JSON.stringify(record), { metadata });
}

export async function account(env, id) {
  try {
    const raw = await env.CORPUS.get('d:' + id);
    const value = raw ? JSON.parse(raw) : null;
    return value && typeof value === 'object' ? value : null;
  } catch (e) {
    return null;
  }
}

export async function discordSession(request, env, now) {
  const header = request.headers.get('authorization') || '';
  if (!header.startsWith('Bearer dc.')) return null;
  const id = await readDiscordToken(env, header.slice(7), now);
  if (!id) return null;
  const found = await account(env, id);
  return found ? { id, account: found } : null;
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

export async function handleLinkIntent(request, env, now) {
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

export async function handleDiscordStart(request, env, now) {
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

export async function handleDiscordCallback(request, env, now) {
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

export async function handleLinkStart(request, env, now) {
  const session = await discordSession(request, env, now);
  if (!session) return no('login first', 401);
  const bytes = crypto.getRandomValues(new Uint8Array(8));
  let code = '';
  for (const b of bytes) code += LINK_ALPHABET[b % LINK_ALPHABET.length];
  await env.CORPUS.put('link:' + code, session.id, { expirationTtl: LINK_TTL_SECONDS });
  return json({ ok: true, code, minutes: LINK_TTL_SECONDS / 60 });
}

export async function handleLink(request, env, now) {
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

export async function handleUnlink(request, env, now) {
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

// Mojang 擋掉 Worker 的請求（403）：不問 Mojang，改驗它簽好的玩家金鑰憑證與個人檔案
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
  return timingSafeEqual(expected, parts[2]);
}

export async function handleChallenge(env, now) {
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

export async function handleLogin(request, env, now) {
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

export async function handleWebLink(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const token = await issueWebToken(env, user.uuid, user.name, now);
  return json({ ok: true, url: new URL(request.url).origin + '/capes#t=' + token });
}
