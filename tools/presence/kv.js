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
