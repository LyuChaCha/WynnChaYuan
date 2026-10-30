import { isStaff } from './admin.js';
import { discordSession, publicId, who } from './auth.js';
import { MAX_CAPE_BYTES, MAX_PLAIN_CAPE_BYTES, hasPerk, listed, visibility } from './capes.js';
import { credits } from './credits.js';
import { ID, day, json, no, readJson } from './http.js';

const TTL_SECONDS = 14 * 24 * 3600;

const LIST_MAX_AGE_MS = 10 * 60 * 1000;

const MAX_USERS = 50000;

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

export async function handleHello(request, env, now) {
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

export async function remember(env, uuid, wantsCape, now, prefs) {
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

export async function storedList(env) {
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

export async function handleList(env, now) {
  let list = await storedList(env);
  if (!list || now - list.t > LIST_MAX_AGE_MS) {
    list = await buildList(env, now);
    await env.CORPUS.put('presence:list', JSON.stringify(list));
  }
  return json(list, 200, { 'cache-control': 'public, max-age=60' });
}

export async function handleMe(request, env, now) {
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
