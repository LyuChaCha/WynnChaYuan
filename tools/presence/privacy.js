import { DISCORD_ID, account, discordSession, publicId, who } from './auth.js';
import { allHistory, discard, favsOf, idleMap, starMap, sweep } from './capes.js';
import { HASH, UUID, hex, json, no } from './http.js';
import { parseRecord } from './kv.js';
import { dropFromList, storedList } from './presence.js';
import { pollMeta, trMeta } from './translate.js';

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

export async function noteParticipant(env, owner, key, before) {
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

export async function handleDeleteMe(request, env, now) {
  const user = await who(request, env, now);
  const session = user ? null : await discordSession(request, env, now);
  if (!user && !session) return no('login first', 401);
  const discord = user ? user.discord || await env.CORPUS.get('l:' + user.uuid) : session.id;
  const subject = { uuid: user ? user.uuid : null, discord: discord && DISCORD_ID.test(discord) ? discord : null };
  const outcome = await eraseSubject(env, subject, now);
  if (!outcome.done) return json({ ok: false, pending: true }, 202, { 'cache-control': 'no-store' });
  return json({ ok: true }, 200, { 'cache-control': 'no-store' });
}
