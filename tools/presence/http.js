export const ID = /^[0-9a-f]{24}$/;

export const UUID = /^[0-9a-f]{32}$/;

export const NAME = /^[A-Za-z0-9_]{1,16}$/;

const BASE64 = /^[A-Za-z0-9+/]+={0,2}$/;

const MAX_BODY = 512;

const JSON_HEADERS = { 'content-type': 'application/json; charset=utf-8' };

export function json(body, status = 200, extra = {}) {
  return new Response(JSON.stringify(body), { status, headers: { ...JSON_HEADERS, ...extra } });
}

export function refuse(why, status = 400) {
  return json({ ok: false, why }, status);
}

export function day(now) {
  return Math.floor(now / 86400000);
}

export function hex(buffer) {
  return [...new Uint8Array(buffer)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

export function redirect(location, cookie) {
  const headers = { location, 'cache-control': 'no-store', 'referrer-policy': 'no-referrer' };
  if (cookie) headers['set-cookie'] = cookie;
  return new Response(null, { status: 302, headers });
}

export async function readBounded(request, limit) {
  if (Number(request.headers.get('content-length') || 0) > limit) return null;
  if (!request.body) return new Uint8Array(0);
  const reader = request.body.getReader();
  const chunks = [];
  let total = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.length;
    if (total > limit) {
      await reader.cancel();
      return null;
    }
    chunks.push(value);
  }
  const bytes = new Uint8Array(total);
  let at = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, at);
    at += chunk.length;
  }
  return bytes;
}

const MAX_BYTES_PER_CHAR = 4;

export async function readJson(request, limit = MAX_BODY) {
  if (Number(request.headers.get('content-length') || 0) > limit) return undefined;
  try {
    const bytes = await readBounded(request, limit * MAX_BYTES_PER_CHAR);
    if (!bytes) return undefined;
    const text = new TextDecoder().decode(bytes);
    if (text.length > limit) return undefined;
    const body = JSON.parse(text);
    return body && typeof body === 'object' && !Array.isArray(body) ? body : null;
  } catch (e) {
    return null;
  }
}

export function base64Bytes(text) {
  return Uint8Array.from(atob(text), (c) => c.charCodeAt(0));
}

export function fromBase64(text, max) {
  if (typeof text !== 'string' || !text || text.length > max || !BASE64.test(text)) return null;
  try {
    return base64Bytes(text);
  } catch (e) {
    return null;
  }
}

export function timingSafeEqual(expected, actual) {
  if (typeof expected !== 'string' || typeof actual !== 'string' || expected.length !== actual.length) return false;
  let diff = 0;
  for (let i = 0; i < expected.length; i++) diff |= expected.charCodeAt(i) ^ actual.charCodeAt(i);
  return diff === 0;
}

export function plainUuid(text) {
  const plain = String(text == null ? '' : text).trim().toLowerCase().replace(/-/g, '');
  return UUID.test(plain) ? plain : null;
}

export const HASH = /^[0-9a-f]{64}$/;

export const botFmt = (s, values) => String(s).replace(/\{(\w+)\}/g, (all, k) => (k in values ? String(values[k]) : all));

export const clip = (s, n) => (String(s).length > n ? String(s).slice(0, n - 1) + '…' : String(s));
