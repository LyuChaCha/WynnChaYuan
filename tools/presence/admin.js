import { DISCORD_ID, account, discordSession, publicId, who } from './auth.js';
import { IDLE_KEEP_MS, allCapes, allHistory, discard, dropCape, groupCapes, groupVisibility, idleGroup, idleMap, idleVisibility, listed, purge, reviewOf, starMap, visibility } from './capes.js';
import { isDeveloper } from './credits.js';
import { HASH, ID, NAME, UUID, json, no, plainUuid, readJson } from './http.js';
import { listAll, parseRecord, quietPut } from './kv.js';
import { remember } from './presence.js';

const DISCORD_SNOWFLAKE = /^\d{17,20}$/;

export function staffKey(text) {
  const raw = String(text == null ? '' : text).trim();
  const uuid = plainUuid(raw);
  if (uuid) return 'u:' + uuid;
  if (DISCORD_SNOWFLAKE.test(raw)) return 'd:' + raw;
  return NAME.test(raw) ? 'n:' + raw.toLowerCase() : null;
}

export async function isStaff(env, user) {
  if (await env.CORPUS.get('staff:u:' + user.uuid)) return true;
  const discord = user.discord || await env.CORPUS.get('l:' + user.uuid);
  if (discord && await env.CORPUS.get('staff:d:' + discord)) return true;
  const byName = await env.CORPUS.get('staff:n:' + user.name.toLowerCase());
  if (!byName) return false;
  await env.CORPUS.put('staff:u:' + user.uuid, JSON.stringify({ ...parseRecord(byName), m: user.name }));
  await env.CORPUS.delete('staff:n:' + user.name.toLowerCase());
  return true;
}

export async function authority(request, env, now) {
  const header = request.headers.get('authorization') || '';
  if (env.ADMIN_TOKEN && header === 'Bearer ' + env.ADMIN_TOKEN) return { by: 'admin', developer: true };
  const user = await who(request, env, now);
  if (user) {
    if (await isDeveloper(user, now)) return { by: user.name, developer: true };
    return await isStaff(env, user) ? { by: user.name, developer: false } : null;
  }
  const session = await discordSession(request, env, now);
  if (session && (await env.CORPUS.get('staff:d:' + session.id)
      || (session.account.u && await env.CORPUS.get('staff:u:' + session.account.u)))) {
    return { by: 'Discord: ' + (session.account.n || session.id), developer: false };
  }
  return null;
}

export async function handleStaff(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  const staff = [];
  for (const k of await listAll(env.CORPUS, 'staff:')) {
    const record = parseRecord(await env.CORPUS.get(k.name));
    const key = k.name.slice(6);
    staff.push({ key, kind: key[0], id: key.slice(2), name: record.m || null, discord: record.dn || null,
      note: record.n || '', t: record.t || null, by: record.by || null });
  }
  staff.sort((a, b) => (b.t || 0) - (a.t || 0));
  return json({ staff }, 200, { 'cache-control': 'no-store' });
}

export async function handleStaffAdd(request, env, now) {
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

export async function handlePeople(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const q = (new URL(request.url).searchParams.get('q') || '').trim().toLowerCase();
  if (q.length > 40) return json({ people: [] }, 200, { 'cache-control': 'no-store' });
  const byUuid = new Map();
  const people = [];
  let reads = 0;
  for (const k of await listAll(env.CORPUS, 'd:')) {
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

export async function handleStaffRemove(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  const body = await readJson(request);
  const key = body && typeof body.key === 'string' ? body.key : '';
  if (!/^(?:u:[0-9a-f]{32}|d:\d{17,20}|n:[a-z0-9_]{1,16})$/.test(key)) return no('bad request');
  await env.CORPUS.delete('staff:' + key);
  return json({ ok: true });
}

export async function handleReviews(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const reviews = [];
  for (const k of await listAll(env.CORPUS, 'review:')) {
    const record = parseRecord(await env.CORPUS.get(k.name));
    reviews.push({ uuid: k.name.slice(7), name: record.n || null, note: record.note || '',
      t: record.t || null, by: record.by || null });
  }
  reviews.sort((a, b) => (b.t || 0) - (a.t || 0));
  return json({ reviews }, 200, { 'cache-control': 'no-store' });
}

export async function handleReviewAdd(request, env, now) {
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

export async function handleReviewRemove(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  const uuid = body ? plainUuid(body.uuid) : null;
  if (!uuid) return no('bad request');
  await env.CORPUS.delete('review:' + uuid);
  await release(env, await publicId(uuid), uuid, now);
  return json({ ok: true });
}

export async function handleApprove(request, env, now) {
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

export async function moderator(request, env, now) {
  const mine = await authority(request, env, now);
  return mine ? mine.by : null;
}

export async function handleAdminRemove(request, env, now) {
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
      await quietPut(env.CORPUS, 'stars', JSON.stringify(stars));
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

export async function handleAdminCapes(request, env, now) {
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

export async function handlePerks(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const perks = [];
  for (const k of await listAll(env.CORPUS, 'perk:')) {
    let record = {};
    try {
      record = JSON.parse((await env.CORPUS.get(k.name)) || '{}');
    } catch (e) {
      record = {};
    }
    if (typeof record !== 'object' || record === null) record = {};
    perks.push({ uuid: k.name.slice(5), note: record.n || '', t: record.t || null, by: record.by || null });
  }
  perks.sort((a, b) => (b.t || 0) - (a.t || 0));
  return json({ perks }, 200, { 'cache-control': 'no-store' });
}

export async function handlePerkAdd(request, env, now) {
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

export async function handlePerkRemove(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  const uuid = body ? plainUuid(body.uuid) : null;
  if (!uuid) return no('bad request');
  await env.CORPUS.delete('perk:' + uuid);
  return json({ ok: true });
}

export async function handleBans(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const bans = [];
  for (const k of await listAll(env.CORPUS, 'ban:')) {
    let record = {};
    try {
      record = JSON.parse((await env.CORPUS.get(k.name)) || '{}');
    } catch (e) {
      record = {};                     // 舊格式只存了時間
    }
    if (typeof record !== 'object' || record === null) record = {};
    bans.push({ uuid: k.name.slice(4), name: record.n || null, t: record.t || null, by: record.by || null });
  }
  bans.sort((a, b) => (b.t || 0) - (a.t || 0));
  return json({ bans }, 200, { 'cache-control': 'no-store' });
}

export async function handleUnban(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  if (!body || typeof body.uuid !== 'string' || !UUID.test(body.uuid)) return no('bad request');
  await env.CORPUS.delete('ban:' + body.uuid);
  return json({ ok: true });
}
