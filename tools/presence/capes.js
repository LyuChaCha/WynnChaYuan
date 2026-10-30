import { capePreview, figurePreview } from './cape-preview.js';
import { moderator } from './admin.js';
import { publicId, who } from './auth.js';
import { credits } from './credits.js';
import { HASH, ID, UUID, hex, json, no, readJson } from './http.js';
import { OperationBudgetExhausted, claimSlot, listAll, meteredStore, parseRecord, quietPut, scanPrefix } from './kv.js';
import { remember } from './presence.js';
import { ownerEntry } from './privacy.js';

export const MAX_CAPE_BYTES = 20 * 1024 * 1024;

export const MAX_PLAIN_CAPE_BYTES = 1024 * 1024;

export const MAX_CAPE_FRAMES = 120;

const UPLOAD_EVERY_MS = 20 * 1000;

export const CAPE_SIZES = [[22, 17], [44, 34], [88, 68], [176, 136], [352, 272], [704, 544], [1056, 816]];

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

export async function hasPerk(env, uuid) {
  return Boolean(await env.CORPUS.get('perk:' + uuid));
}

export async function isMember(env, user, now) {
  if (await hasPerk(env, user.uuid)) return true;
  const list = await credits(now);
  return Boolean(list && list.names.has(user.name.toLowerCase()));
}

async function capeLimit(env, user, now) {
  return await isMember(env, user, now) ? MAX_CAPE_BYTES : MAX_PLAIN_CAPE_BYTES;
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

export async function handleUpload(request, env, now) {
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
  const slot = await claimSlot(env.CORPUS, 'upl:claim:' + user.uuid + ':', { windowMs: UPLOAD_EVERY_MS, limit: 1 });
  if (!slot.granted) return no('slow down', 429);
  try {
    return await storeUpload(request, env, now, { user, bytes, found, log, id, have });
  } finally {
    await env.CORPUS.delete(slot.key);
  }
}

async function storeUpload(request, env, now, { user, bytes, found, log, id, have }) {
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

export const idleVisibility = (one) => (!one.q ? 'public' : one.vis === 'private' ? 'private' : 'unlisted');

const linkable = (one) => !one.q || one.vis === 'unlisted';

export const listed = (cape) => visibility(cape) === 'public' && !cape.r && !cape.lk;

export async function allHistory(env, prefix = 'hist:') {
  const out = [];
  for (const k of await listAll(env.CORPUS, prefix)) {
    const parts = k.name.split(':');
    if (parts.length === 3 && HASH.test(parts[2])) out.push({ key: k.name, id: parts[1], h: parts[2], ...(k.metadata || {}) });
  }
  return out;
}

async function noteHistory(env, id, cape, now) {
  await env.CORPUS.put('hist:' + id + ':' + cape.h, '', {
    metadata: { t: cape.t, s: cape.s, x: cape.x || 0, y: cape.y || 0, at: now },
  });
  const mine = (await allHistory(env, 'hist:' + id + ':')).sort((a, b) => (b.at || 0) - (a.at || 0));
  for (const old of mine.slice(HISTORY_MAX)) await env.CORPUS.delete(old.key);
}

export async function reviewOf(env, uuid) {
  const raw = await env.CORPUS.get('review:' + uuid);
  return raw ? parseRecord(raw) : null;
}

async function needsReview(env, uuid, hash) {
  const record = await reviewOf(env, uuid);
  return Boolean(record && !(Array.isArray(record.ok) && record.ok.includes(hash)));
}

export async function favsOf(env, uuid) {
  const record = parseRecord(await env.CORPUS.get('fav:' + uuid));
  return { t: record.t || 0, h: Array.isArray(record.h) ? record.h.filter((h) => HASH.test(h)) : [] };
}

export async function starMap(env) {
  const map = parseRecord(await env.CORPUS.get('stars'));
  return map && typeof map === 'object' ? map : {};
}

export async function handleStar(request, env, now) {
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
    await quietPut(env.CORPUS, 'stars', JSON.stringify(map));
  }
  return json({ ok: true, on: body.on, stars: Number(map[hash]) || 0 });
}

export async function handleMine(request, env, now) {
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

export async function handleForget(request, env, now) {
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

export async function handlePublic(request, env, now) {
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

const WEAR_EVERY_MS = 5 * 1000;

export async function handleWear(request, env, now) {
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

export async function handleRestore(request, env, now) {
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

export async function handleRemove(request, env, now) {
  const user = await who(request, env, now);
  if (!user) return no('login first', 401);
  const id = await publicId(user.uuid);
  await dropCape(env, id, now);
  return json({ ok: true });
}

export async function allCapes(env) {
  const out = [];
  for (const k of await listAll(env.CORPUS, 'cape:')) {
    if (k.metadata && typeof k.metadata.h === 'string') {
      out.push({ id: k.name.slice(5), ...k.metadata });
    }
  }
  return out;
}

// idle 整筆覆寫，同時寫入會互蓋：tidy 每天把漏記的圖補回去

export const IDLE_KEEP_MS = 7 * 24 * 3600 * 1000;

export async function idleMap(env) {
  return parseRecord(await env.CORPUS.get('idle'));
}

const inUse = (capes, hash) => capes.some((cape) => cape.h === hash || (cape.p && cape.p.h === hash));

export async function sweep(env, hash, from, now) {
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
  await quietPut(env.CORPUS, 'idle', JSON.stringify(idle));
}

async function revive(env, hash) {
  const idle = await idleMap(env);
  if (!idle[hash]) return;
  delete idle[hash];
  await quietPut(env.CORPUS, 'idle', JSON.stringify(idle));
}

export async function discard(env, hash) {
  if (!hash || inUse(await allCapes(env), hash)) return false;
  await env.CORPUS.delete('blob:' + hash);
  await dropPreviews(env, hash);
  await revive(env, hash);
  return true;
}

export async function purge(env, now) {
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
  await quietPut(env.CORPUS, 'idle', JSON.stringify(idle));
  if (starred) await quietPut(env.CORPUS, 'stars', JSON.stringify(stars));
  return idle;
}

export async function tidy(env, now) {
  const idle = await purge(env, now);
  const capes = await allCapes(env);
  let added = 0;
  for (const k of await listAll(env.CORPUS, 'blob:')) {
    const hash = k.name.slice(5);
    if (!HASH.test(hash) || idle[hash] || inUse(capes, hash)) continue;
    idle[hash] = { t: (k.metadata && k.metadata.t) || 'png', s: (k.metadata && k.metadata.s) || 0, at: now, q: 1 };
    added++;
  }
  if (added) await quietPut(env.CORPUS, 'idle', JSON.stringify(idle));
  return { idle: Object.keys(idle).length, added };
}

export async function handleIdle(env, hash) {
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

export async function dropCape(env, id, now) {
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

export function idleGroup(hash, one) {
  const group = { h: hash, t: one.t, s: one.s, v: one.at, c: one.c || one.at, ids: [], idle: one.at };
  if (one.x && one.y) {
    group.x = one.x;
    group.y = one.y;
  }
  return group;
}

export async function handleCapes(env, now) {
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

export async function handleCapeLink(request, env, now, hash) {
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

export async function handleCape(env, id, key) {
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

export const FIGURE_STYLE = 'angled';

async function wearing(env, uuid, hash) {
  if (!uuid) return false;
  const worn = await env.CORPUS.getWithMetadata('cape:' + await publicId(uuid));
  return worn?.metadata?.h === hash;
}

export async function handleCapePreview(env, hash, uuid) {
  if (!HASH.test(hash || '')) return no('not found', 404);
  const who = UUID.test(uuid || '') ? uuid.replace(/-/g, '').toLowerCase() : null;
  if (uuid && !await wearing(env, who, hash)) return no('not found', 404);
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
  for (const k of await listAll(env.CORPUS, 'prev:' + hash + ':')) await env.CORPUS.delete(k.name);
}

const STAR_RECOUNT_OPS = 300;
const STAR_RECOUNT_STATE = 'stars:recount';

export async function recountStars(env) {
  const state = parseRecord(await env.CORPUS.get(STAR_RECOUNT_STATE));
  state.counts = state.counts && typeof state.counts === 'object' ? state.counts : {};
  const store = meteredStore(env.CORPUS, { left: STAR_RECOUNT_OPS });
  try {
    await scanPrefix(store, state, 'fav:', async (key) => {
      const favs = await favsOf({ CORPUS: store }, key.name.slice('fav:'.length));
      for (const hash of favs.h) state.counts[hash] = (state.counts[hash] || 0) + 1;
    });
  } catch (error) {
    if (!(error instanceof OperationBudgetExhausted)) throw error;
    await env.CORPUS.put(STAR_RECOUNT_STATE, JSON.stringify(state));
    return { done: false };
  }
  const alive = new Set([...(await allCapes(env)).map((cape) => cape.h), ...Object.keys(await idleMap(env))]);
  const counted = Object.fromEntries(Object.entries(state.counts).filter(([hash]) => alive.has(hash)));
  const stored = await starMap(env);
  const same = Object.keys(counted).length === Object.keys(stored).length
    && Object.entries(counted).every(([hash, count]) => stored[hash] === count);
  if (!same) await quietPut(env.CORPUS, 'stars', JSON.stringify(counted));
  await env.CORPUS.delete(STAR_RECOUNT_STATE);
  return { done: true, changed: !same };
}
