import { hex } from './http.js';

export async function listAll(store, prefix, { limit = 1000, more } = {}) {
  const keys = [];
  let cursor;
  do {
    const page = await store.list({ prefix, limit, cursor });
    keys.push(...page.keys);
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor && (!more || more(keys)));
  return keys;
}

export function parseStored(raw) {
  try {
    const value = JSON.parse(raw);
    return value && typeof value === 'object' ? value : null;
  } catch (e) {
    return null;
  }
}

export function parseRecord(raw) {
  try {
    const value = JSON.parse(raw || '{}');
    return value && typeof value === 'object' ? value : {};
  } catch (e) {
    return {};
  }
}

export async function quietPut(store, key, value, options, ctx) {
  const write = store.put(key, value, options).catch((error) => {
    console.error(JSON.stringify({ event: 'kv write failed', key, error: String(error?.message ?? error) }));
  });
  if (ctx?.waitUntil) ctx.waitUntil(write);
  else await write;
}

const SCAN_PAGE_SIZE = 200;

export class OperationBudgetExhausted extends Error {}

export function meteredStore(store, budget) {
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

export async function scanPrefix(store, state, prefix, visit) {
  for (;;) {
    const page = await store.list({ prefix, limit: SCAN_PAGE_SIZE, cursor: state.cursor || undefined });
    for (const key of page.keys) {
      if (state.after && key.name <= state.after) continue;
      await visit(key);
      state.after = key.name;
    }
    if (page.list_complete || !page.cursor) return;
    state.cursor = page.cursor;
  }
}

let slotSequence = 0;

export async function claimSlot(store, prefix, { windowMs, limit }) {
  slotSequence += 1;
  const claimedAt = Date.now();
  const stamp = claimedAt + '.' + String(slotSequence).padStart(8, '0') + hex(crypto.getRandomValues(new Uint8Array(4)));
  const key = prefix + stamp;
  await store.put(key, '', { expirationTtl: Math.max(60, Math.ceil(windowMs / 1000)) });
  const recent = (await listAll(store, prefix))
    .map((entry) => entry.name.slice(prefix.length))
    .filter((name) => claimedAt - Number(name.split('.')[0]) < windowMs)
    .sort((a, b) => Number(a.split('.')[0]) - Number(b.split('.')[0]) || a.localeCompare(b));
  const rank = recent.indexOf(stamp);
  if (rank >= 0 && rank < limit) return { granted: true, key };
  await store.delete(key);
  return { granted: false, oldest: Number((recent[0] || stamp).split('.')[0]) };
}
