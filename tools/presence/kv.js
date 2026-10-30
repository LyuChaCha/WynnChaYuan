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
