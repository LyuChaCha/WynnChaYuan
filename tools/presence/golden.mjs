// node tools/presence/golden.mjs [write|check] [--strip-comments] [--file <path>] [--worker <path>]
import fs from 'node:fs';
import path from 'node:path';
import nodeCrypto from 'node:crypto';
import { fileURLToPath, pathToFileURL } from 'node:url';

const FIXED_NOW = Date.UTC(2026, 9, 30, 12, 0, 0);
const RealDate = Date;
class FixedDate extends RealDate {
  constructor(...args) {
    super(...(args.length ? args : [FIXED_NOW]));
  }

  static now() {
    return FIXED_NOW;
  }
}
globalThis.Date = FixedDate;

let seed = 0x9e3779b9;
function nextByte() {
  seed ^= seed << 13;
  seed ^= seed >>> 17;
  seed ^= seed << 5;
  return (seed >>> 0) & 0xff;
}
Object.defineProperty(globalThis.crypto, 'getRandomValues', {
  configurable: true,
  value: (array) => {
    const bytes = new Uint8Array(array.buffer, array.byteOffset, array.byteLength);
    for (let i = 0; i < bytes.length; i++) bytes[i] = nextByte();
    return array;
  },
});
Object.defineProperty(globalThis.crypto, 'randomUUID', {
  configurable: true,
  value: () => [...globalThis.crypto.getRandomValues(new Uint8Array(16))].map((b) => b.toString(16).padStart(2, '0')).join(''),
});
Math.random = () => nextByte() / 256;

const here = path.dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const mode = args.includes('check') ? 'check' : 'write';
const stripComments = args.includes('--strip-comments');
const fileArg = args.indexOf('--file');
const workerArg = args.indexOf('--worker');
const workerUrl = workerArg >= 0 ? pathToFileURL(path.resolve(args[workerArg + 1])).href : './worker.js';
const target = fileArg >= 0 ? path.resolve(args[fileArg + 1]) : path.join(here, 'golden', stripComments ? 'snapshot.stripped.json' : 'snapshot.json');

const ED25519_SEED = Buffer.alloc(32, 7);
const discordKey = nodeCrypto.createPrivateKey({
  key: Buffer.concat([Buffer.from('302e020100300506032b657004220420', 'hex'), ED25519_SEED]), format: 'der', type: 'pkcs8',
});
const DISCORD_PUBLIC_KEY = nodeCrypto.createPublicKey(discordKey).export({ format: 'der', type: 'spki' }).subarray(-32).toString('hex');

const { ROOT, DEVELOPER, DEVELOPER_UUID, demoCredits, memoryKv, seedDemo } = await import('./dev-data.mjs');
const worker = (await import(workerUrl)).default;
const { issueToken, issueWebToken, issueDiscordToken, publicId } = await import('./worker-api.js');
const { LANGS, PATHS } = await import('./site.js');
const { capePreview, figurePreview } = await import('./cape-preview.js');

const sha = (bytes) => nodeCrypto.createHash('sha256').update(bytes).digest('hex');

function withoutComments(text) {
  let out = '';
  let i = 0;
  let quote = null;
  while (i < text.length) {
    const c = text[i];
    const next = text[i + 1];
    if (quote) {
      out += c;
      if (c === '\\') { out += next || ''; i += 2; continue; }
      if (c === quote || (quote !== '`' && c === '\n')) quote = null;
      i++;
      continue;
    }
    if (c === '<' && text.startsWith('<!--', i)) {
      const end = text.indexOf('-->', i + 4);
      i = end < 0 ? text.length : end + 3;
      continue;
    }
    if (c === '/' && next === '*') {
      const end = text.indexOf('*/', i + 2);
      i = end < 0 ? text.length : end + 2;
      out += ' ';
      continue;
    }
    if (c === '/' && next === '/' && !/[:\\]$/.test(out) && !/https?:$/.test(out)) {
      const end = text.indexOf('\n', i);
      i = end < 0 ? text.length : end;
      continue;
    }
    if (c === '"' || c === "'" || c === '`') quote = c;
    out += c;
    i++;
  }
  return out.replace(/\s+/g, ' ');
}

function textual(type) {
  return /json|text\/|javascript|css/.test(type || '');
}

async function describeBody(response) {
  const type = response.headers.get('content-type') || '';
  const bytes = Buffer.from(await response.arrayBuffer());
  if (type.includes('json')) {
    const text = bytes.toString('utf8');
    return text.length <= 6000 ? { json: text } : { sha: sha(bytes), length: bytes.length };
  }
  if (stripComments && textual(type)) {
    const plain = withoutComments(bytes.toString('utf8'));
    return { sha: sha(plain), normalised: true };
  }
  return { sha: sha(bytes), length: bytes.length };
}

function describeHeaders(headers) {
  const out = {};
  for (const [name, value] of [...headers].sort(([a], [b]) => a.localeCompare(b))) {
    out[name] = value;
  }
  return out;
}

function valueDigest(entry) {
  const value = entry.value;
  const bytes = typeof value === 'string' ? Buffer.from(value, 'utf8') : Buffer.from(value instanceof ArrayBuffer ? new Uint8Array(value) : value);
  return { sha: sha(bytes), metadata: entry.metadata == null ? null : JSON.stringify(entry.metadata) };
}

function kvDiff(before, after) {
  const changes = {};
  for (const key of [...new Set([...before.keys(), ...after.keys()])].sort()) {
    const a = before.has(key) ? valueDigest(before.get(key)) : null;
    const b = after.has(key) ? valueDigest(after.get(key)) : null;
    if (JSON.stringify(a) !== JSON.stringify(b)) changes[key] = b;
  }
  return changes;
}

const outbound = [];
const repoFile = (rel) => {
  try {
    return fs.readFileSync(path.join(ROOT, rel));
  } catch (e) {
    return null;
  }
};
const indexFixture = JSON.stringify({ files: ['raid.json', 'npc.json'], rows: [
  [0, null, 'Choose a buff or go to the exit', '選一個增益，或前往出口'], [1, null, 'Weapon Merchant', '武器商人']] });
const fakeSkin = repoFile('tools/presence/textures/skin.png');
globalThis.fetch = async (input, init = {}) => {
  const url = new URL(typeof input === 'string' ? input : input.url);
  const method = init.method || (typeof input === 'string' ? 'GET' : input.method) || 'GET';
  const body = init.body == null ? '' : typeof init.body === 'string' ? init.body : '[binary]';
  outbound.push({ method, url: url.origin + url.pathname + url.search, body: body.length > 400 ? 'sha:' + sha(body) : body });
  const answer = (value, status = 200) => new Response(typeof value === 'string' ? value : JSON.stringify(value), { status });
  if (url.hostname === 'api.github.com') {
    const route = url.pathname.replace(/^\/repos\/[^/]+\/[^/]+/, '');
    if (method === 'GET' && route.startsWith('/contents/')) {
      const file = repoFile(decodeURIComponent(route.slice('/contents/'.length)));
      return file ? answer({ content: file.toString('base64'), sha: 'filesha' }) : answer({}, 404);
    }
    if (route === '/git/ref/heads/main') return answer({ object: { sha: 'mainsha' } });
    if (route.startsWith('/git/ref/heads/')) return answer({}, 404);
    if (route === '/git/refs') return answer({}, 201);
    if (method === 'GET' && route === '/pulls') return answer([]);
    if (method === 'POST' && route === '/pulls') return answer({ html_url: 'https://github.com/LyuChaCha/WynnChaYuan/pull/1' }, 201);
    if (method === 'POST' && route === '/issues') return answer({ html_url: 'https://github.com/LyuChaCha/WynnChaYuan/issues/2', number: 2 }, 201);
    if (method === 'PUT') return answer({}, 200);
    if (method === 'DELETE') return answer('', 204);
    return answer({}, 404);
  }
  if (url.hostname === 'discord.com') {
    if (url.pathname === '/api/oauth2/token') return answer({ access_token: 'access' });
    if (url.pathname === '/api/users/@me') return answer({ id: '444444444444444444', username: 'golden_dc', global_name: 'Golden' });
    if (url.pathname === '/api/v10/applications/@me') {
      return answer({ id: 'client1', name: 'WynnChaYuan', bot: { username: 'WynnChaYuan' }, verify_key: DISCORD_PUBLIC_KEY,
        interactions_endpoint_url: 'https://wynnchayuan.com/api/discord/interactions' });
    }
    if (url.pathname === '/api/v10/users/@me/guilds') return answer([{ name: 'YuChaYuan' }]);
    if (url.pathname.endsWith('/commands') && method === 'GET') return answer([{ name: 'status' }]);
    if (url.pathname.endsWith('/commands') && method === 'PUT') return answer(JSON.parse(init.body).map((c) => ({ id: '1', name: c.name })));
    if (url.pathname.startsWith('/api/v10/webhooks/')) return answer({});
    return answer({}, 404);
  }
  if (url.hostname === 'raw.githubusercontent.com') {
    if (url.pathname.includes('/translate-index/')) return answer(indexFixture);
    if (url.pathname.endsWith('/credits.json')) return answer(demoCredits(false));
    return answer({}, 404);
  }
  if (url.hostname === 'minotar.net') return fakeSkin ? new Response(fakeSkin, { status: 200 }) : answer('', 404);
  return answer('unreachable', 599);
};

const base = new Map();
base.set('auth:secret', { value: 'f'.repeat(64), metadata: null });
await seedDemo(base, {});
const STEVE = 'b'.repeat(32);
const ALEX = 'c'.repeat(32);
const DEV = DEVELOPER_UUID;
const LINKED = 'd'.repeat(32);
const tokens = {
  steve: await issueToken({ CORPUS: memoryKv(base) }, STEVE, 'Steve', FIXED_NOW),
  steveWeb: await issueWebToken({ CORPUS: memoryKv(base) }, STEVE, 'Steve', FIXED_NOW),
  alex: await issueWebToken({ CORPUS: memoryKv(base) }, ALEX, 'Alex', FIXED_NOW),
  dev: await issueWebToken({ CORPUS: memoryKv(base) }, DEV, DEVELOPER, FIXED_NOW),
  devGame: await issueToken({ CORPUS: memoryKv(base) }, DEV, DEVELOPER, FIXED_NOW),
  linkedDc: await issueDiscordToken({ CORPUS: memoryKv(base) }, '222222222222222222', FIXED_NOW),
  loneDc: await issueDiscordToken({ CORPUS: memoryKv(base) }, '333333333333333333', FIXED_NOW),
  admin: 'golden-admin-token',
  expired: await issueToken({ CORPUS: memoryKv(base) }, STEVE, 'Steve', FIXED_NOW - 8 * 86400000),
};
const capes = [...base.entries()].filter(([k]) => k.startsWith('cape:')).map(([k, v]) => ({ id: k.slice(5), ...v.metadata }));
const hashes = [...new Set(capes.map((c) => c.h))].sort();
const idleHashes = Object.keys(JSON.parse(base.get('idle').value)).sort();
const steveId = await publicId(STEVE);
const someCape = capes.find((c) => c.vis !== 'private' && !c.r && !c.q) || capes[0];
const pngTemplate = new Uint8Array(fs.readFileSync(path.join(ROOT, 'capes', 'templates', 'example-88x68.png')));
const gifTemplate = new Uint8Array(fs.readFileSync(path.join(ROOT, 'capes', 'templates', 'example-88x68.gif')));

function signedInteraction(payload, tamper) {
  const body = JSON.stringify(payload);
  const timestamp = String(Math.floor(FIXED_NOW / 1000));
  const signature = nodeCrypto.sign(null, Buffer.from(timestamp + body), discordKey).toString('hex');
  return { body, headers: { 'x-signature-ed25519': tamper ? signature.replace(/^./, (c) => (c === 'a' ? 'b' : 'a')) : signature,
    'x-signature-timestamp': timestamp, 'content-type': 'application/json' } };
}
const command = (name, options = [], locale = 'zh-TW') => ({ type: 2, id: 'i1', application_id: 'client1', token: 'tok', locale,
  member: { user: { id: '222222222222222222', username: 'steve_dc' } }, data: { name, options } });

const C = [];
const add = (name, method, p, options = {}) => C.push({ name, method, path: p, ...options });
const api = (name, method, p, options = {}) => add(name, method, '/api' + p, options);

add('home bare', 'GET', '/');
add('home cookie lang', 'GET', '/', { headers: { cookie: 'lang=ja' } });
add('home accept-language', 'GET', '/', { headers: { 'accept-language': 'ko-KR,ko;q=0.9' } });
add('head home', 'HEAD', '/');
add('head privacy', 'HEAD', '/privacy');
add('head missing', 'HEAD', '/nope');
add('lost page', 'GET', '/nope', { headers: { 'accept-language': 'en' } });
add('lost post', 'POST', '/nope');
add('gallery redirect', 'GET', '/gallery');
for (const icon of ['/favicon.png', '/favicon.ico', '/apple-touch-icon.png', '/icon-192.png']) add('icon ' + icon, 'GET', icon);
add('font', 'GET', '/font/px-tc.ttf');
add('font missing', 'GET', '/font/px-xx.ttf');
add('texture', 'GET', '/tex/hud.png');
add('texture missing', 'GET', '/tex/nope.png');
add('template png', 'GET', '/template/template-88x68.png');
add('template gif', 'GET', '/template/example-88x68.gif');
add('template readme', 'GET', '/template/README.md');
add('discord start', 'GET', '/auth/discord');
add('discord start with intent', 'GET', '/auth/discord?intent=' + 'ab'.repeat(16));
add('discord callback without cookie', 'GET', '/auth/discord/callback?code=x&state=y');
add('discord callback denied', 'GET', '/auth/discord/callback?error=access_denied');

api('challenge', 'POST', '/challenge');
api('login bad body', 'POST', '/login', { body: '{"uuid":"nope"}' });
api('login stale challenge', 'POST', '/login', { body: JSON.stringify({ uuid: STEVE, expiresAt: FIXED_NOW + 1, publicKey: 'AAAA', keySignature: 'AAAA', signature: 'AAAA', challenge: 'x' }) });
api('web-link anon', 'POST', '/web-link');
api('web-link steve', 'POST', '/web-link', { token: tokens.steve });
api('web-link expired', 'POST', '/web-link', { token: tokens.expired });
api('link start discord', 'POST', '/link/start', { token: tokens.loneDc });
api('link start anon', 'POST', '/link/start');
api('link intent', 'POST', '/link/intent', { token: tokens.steve });
api('link bad code', 'POST', '/link', { token: tokens.steve, body: '{"code":"ZZZZZZZZ"}' });
api('link anon', 'POST', '/link', { body: '{"code":"ZZZZZZZZ"}' });
api('unlink linked', 'POST', '/unlink', { token: tokens.linkedDc });
api('unlink anon', 'POST', '/unlink');
for (const who of ['steve', 'steveWeb', 'alex', 'dev', 'linkedDc', 'loneDc']) api('me ' + who, 'GET', '/me', { token: tokens[who] });
api('me anon', 'GET', '/me');
api('delete me anon', 'POST', '/me/delete', { body: '{}' });
api('delete me steve', 'POST', '/me/delete', { token: tokens.steve, body: '{}' });
api('delete me lone discord', 'POST', '/me/delete', { token: tokens.loneDc, body: '{}' });
api('delete me linked discord', 'POST', '/me/delete', { token: tokens.linkedDc, body: '{}' });
api('hello steve', 'POST', '/hello', { token: tokens.steve, body: '{"cape":true,"rainbow":true,"show":["dev"]}' });
api('hello web token', 'POST', '/hello', { token: tokens.steveWeb, body: '{"cape":true}' });
api('hello bad json', 'POST', '/hello', { token: tokens.steve, body: '{' });
api('list', 'GET', '/list');
api('status', 'GET', '/status');
api('team', 'GET', '/team');
api('upload anon', 'PUT', '/cape', { binary: pngTemplate });
api('upload steve png', 'PUT', '/cape', { token: tokens.steve, binary: pngTemplate });
api('upload dev gif', 'PUT', '/cape', { token: tokens.devGame, binary: gifTemplate });
api('upload garbage', 'PUT', '/cape', { token: tokens.steve, binary: new Uint8Array([1, 2, 3]) });
api('remove cape steve', 'DELETE', '/cape', { token: tokens.steve });
api('capes', 'GET', '/capes');
api('capes mine steve', 'GET', '/capes/mine', { token: tokens.steveWeb });
api('capes mine anon', 'GET', '/capes/mine');
for (const hash of hashes.slice(0, 3)) api('cape link ' + hash.slice(0, 8), 'GET', '/capes/' + hash, { token: tokens.steveWeb });
api('cape link bad', 'GET', '/capes/zzz');
api('cape by id', 'GET', '/cape/' + someCape.id);
api('cape by id with key', 'GET', '/cape/' + someCape.id + '?k=' + someCape.h);
api('cape by id missing', 'GET', '/cape/' + '0'.repeat(24));
api('cape steve unlisted', 'GET', '/cape/' + steveId);
for (const hash of idleHashes) api('idle ' + hash.slice(0, 8), 'GET', '/idle/' + hash);
api('idle bad', 'GET', '/idle/nothex');
api('wear', 'POST', '/cape/wear', { token: tokens.steveWeb, body: JSON.stringify({ hash: someCape.h }) });
api('wear idle', 'POST', '/cape/wear', { token: tokens.alex, body: JSON.stringify({ hash: idleHashes[0] }) });
api('wear anon', 'POST', '/cape/wear', { body: JSON.stringify({ hash: someCape.h }) });
api('restore', 'POST', '/cape/restore', { token: tokens.steveWeb, body: '{}' });
api('star on', 'POST', '/cape/star', { token: tokens.steveWeb, body: JSON.stringify({ hash: someCape.h, on: true }) });
api('star off', 'POST', '/cape/star', { token: tokens.steveWeb, body: JSON.stringify({ hash: someCape.h, on: false }) });
api('public on', 'POST', '/cape/public', { token: tokens.steveWeb, body: '{"on":true}' });
api('visibility private', 'POST', '/cape/visibility', { token: tokens.steveWeb, body: '{"vis":"private"}' });
api('visibility bad', 'POST', '/cape/visibility', { token: tokens.steveWeb, body: '{"vis":"nope"}' });
api('forget', 'POST', '/cape/forget', { token: tokens.steveWeb, body: JSON.stringify({ hash: someCape.h }) });
for (const [who, token] of [['dev', tokens.dev], ['staff', tokens.alex], ['admin', tokens.admin], ['steve', tokens.steveWeb], ['anon', null]]) {
  for (const p of ['/admin/capes', '/admin/perks', '/admin/bans', '/admin/staff', '/admin/translators', '/admin/langteam',
    '/admin/reviews', '/admin/guilds', '/admin/feedback', '/admin/discord', '/admin/people?q=ste']) {
    api(p + ' ' + who, 'GET', p, { token });
  }
}
api('admin feedback filtered', 'GET', '/admin/feedback?cat=translation&st=open', { token: tokens.dev });
api('admin remove by hash', 'POST', '/admin/remove', { token: tokens.dev, body: JSON.stringify({ hash: hashes[0], ban: true }) });
api('admin remove by id', 'POST', '/admin/remove', { token: tokens.alex, body: JSON.stringify({ id: someCape.id }) });
api('admin remove anon', 'POST', '/admin/remove', { body: JSON.stringify({ id: someCape.id }) });
api('perk add', 'POST', '/admin/perk', { token: tokens.dev, body: JSON.stringify({ uuid: STEVE, note: 'golden' }) });
api('perk remove', 'POST', '/admin/unperk', { token: tokens.dev, body: JSON.stringify({ uuid: STEVE }) });
api('unban', 'POST', '/admin/unban', { token: tokens.dev, body: JSON.stringify({ uuid: '9'.repeat(32) }) });
api('credits add', 'POST', '/admin/credits', { token: tokens.dev, body: JSON.stringify({ section: 'contributors', name: 'Golden', mc: 'Golden_Mc', color: '#55ff55' }) });
api('credits add bad', 'POST', '/admin/credits', { token: tokens.dev, body: '{"section":"x"}' });
api('credits add staff', 'POST', '/admin/credits', { token: tokens.alex, body: JSON.stringify({ section: 'contributors', name: 'Golden', mc: 'Golden_Mc' }) });
api('credits teams', 'POST', '/admin/credits/teams', { token: tokens.dev, body: JSON.stringify({ mc: DEVELOPER, teams: ['zh_tw'], areas: ['code'] }) });
api('staff add', 'POST', '/admin/staff', { token: tokens.dev, body: JSON.stringify({ who: 'Notch2', note: 'n' }) });
api('staff add by staff', 'POST', '/admin/staff', { token: tokens.alex, body: JSON.stringify({ who: 'Notch2' }) });
api('staff remove', 'POST', '/admin/unstaff', { token: tokens.dev, body: JSON.stringify({ key: 'n:notch' }) });
api('translator add', 'POST', '/admin/translator', { token: tokens.dev, body: JSON.stringify({ who: 'Steve', langs: ['zh_tw'], note: 'x' }) });
api('translator remove', 'POST', '/admin/untranslator', { token: tokens.dev, body: JSON.stringify({ key: 'u:' + ALEX }) });
api('langteam set', 'POST', '/admin/langteam', { token: tokens.dev, body: JSON.stringify({ who: 'Alex', langs: ['ja_jp'], areas: ['translation'], note: 'jp' }) });
api('langteam bad', 'POST', '/admin/langteam', { token: tokens.dev, body: JSON.stringify({ who: 'Alex', langs: ['xx'] }) });
api('review add uuid', 'POST', '/admin/review', { token: tokens.dev, body: JSON.stringify({ uuid: STEVE, note: 'r' }) });
api('review add hash', 'POST', '/admin/review', { token: tokens.dev, body: JSON.stringify({ hash: someCape.h }) });
api('review remove', 'POST', '/admin/unreview', { token: tokens.dev, body: JSON.stringify({ uuid: '0'.repeat(32) }) });
api('approve', 'POST', '/admin/approve', { token: tokens.dev, body: JSON.stringify({ hash: someCape.h }) });
api('guild add', 'POST', '/admin/guild', { token: tokens.dev, body: JSON.stringify({ name: 'Golden Guild', tag: 'GLD' }) });
api('guild add bad', 'POST', '/admin/guild', { token: tokens.dev, body: JSON.stringify({ name: '1' }) });
api('guild remove', 'POST', '/admin/unguild', { token: tokens.dev, body: JSON.stringify({ slug: 'yuchayuan' }) });
api('guild image upload', 'PUT', '/admin/guild/yuchayuan/icon', { token: tokens.dev, binary: pngTemplate });
api('guild image missing', 'GET', '/guild/yuchayuan/icon');
api('guild image bad slug', 'GET', '/guild/zz/icon');
api('tr me anon', 'GET', '/tr/me');
api('tr me alex', 'GET', '/tr/me', { token: tokens.alex });
api('tr index', 'GET', '/tr/index/zh_tw');
api('tr index bad', 'GET', '/tr/index/xx_xx');
api('tr props', 'GET', '/tr/props?lang=zh_tw');
api('tr props bad', 'GET', '/tr/props?lang=xx');
api('tr prop', 'GET', '/tr/prop?lang=zh_tw&id=0123456789abcdef');
api('tr prop staff', 'GET', '/tr/prop?lang=zh_tw&id=0123456789abcdef', { token: tokens.dev });
api('tr prop missing', 'GET', '/tr/prop?lang=zh_tw&id=ffffffffffffffff');
api('tr propose', 'POST', '/tr/propose', { token: tokens.alex, body: JSON.stringify({ lang: 'zh_tw', file: 'npc.json', src: 'Weapon Merchant', base: '武器商人', text: '兵器商人' }) });
api('tr propose not translator', 'POST', '/tr/propose', { token: tokens.steveWeb, body: JSON.stringify({ lang: 'zh_tw', file: 'npc.json', src: 'A', text: 'B' }) });
api('tr propose placeholder', 'POST', '/tr/propose', { token: tokens.alex, body: JSON.stringify({ lang: 'zh_tw', file: 'npc.json', src: 'Hi {u}', text: '嗨' }) });
api('tr vote', 'POST', '/tr/vote', { token: tokens.alex, body: JSON.stringify({ lang: 'zh_tw', id: '0123456789abcdef', item: 'b1b2c3d4e5f6' }) });
api('tr accept', 'POST', '/tr/accept', { token: tokens.dev, body: JSON.stringify({ lang: 'zh_tw', id: '0123456789abcdef', item: 'a1b2c3d4e5f6' }) });
api('tr accept translator', 'POST', '/tr/accept', { token: tokens.alex, body: JSON.stringify({ lang: 'zh_tw', id: '0123456789abcdef', item: 'a1b2c3d4e5f6' }) });
api('tr reject', 'POST', '/tr/reject', { token: tokens.dev, body: JSON.stringify({ lang: 'zh_tw', id: '0123456789abcdef' }) });
api('tr polls', 'GET', '/tr/polls?lang=zh_tw');
api('tr poll', 'GET', '/tr/poll?lang=zh_tw&id=fedcba987654');
api('tr poll open', 'POST', '/tr/poll', { token: tokens.alex, body: JSON.stringify({ lang: 'zh_tw', term: 'Golden Term', note: 'n', options: ['金', '黃金'] }) });
api('tr poll option', 'POST', '/tr/poll/option', { token: tokens.alex, body: JSON.stringify({ lang: 'zh_tw', id: 'fedcba987654', text: '弱化敵人' }) });
api('tr poll vote', 'POST', '/tr/poll/vote', { token: tokens.alex, body: JSON.stringify({ lang: 'zh_tw', id: 'fedcba987654', option: 'c1c2c3c4c5c6' }) });
api('tr poll decide', 'POST', '/tr/poll/decide', { token: tokens.dev, body: JSON.stringify({ lang: 'zh_tw', id: 'fedcba987654', option: 'c1c2c3c4c5c6' }) });
api('tr poll reopen', 'POST', '/tr/poll/reopen', { token: tokens.dev, body: JSON.stringify({ lang: 'zh_tw', id: 'fedcba987654' }) });
api('tr unknown', 'GET', '/tr/nothing');
api('feedback anon', 'POST', '/feedback', { body: JSON.stringify({ cat: 'other', msg: 'hello there' }) });
api('feedback game without discord', 'POST', '/feedback', { token: tokens.steveWeb, body: JSON.stringify({ cat: 'other', msg: 'hello there' }) });
api('feedback discord', 'POST', '/feedback', { token: tokens.loneDc, body: JSON.stringify({ cat: 'website', msg: 'hello there', where: '/capes', lang: 'zh' }) });
api('feedback translation', 'POST', '/feedback', { token: tokens.linkedDc, body: JSON.stringify({ cat: 'translation', msg: 'fix this line', where: 'npc' }) });
api('feedback too short', 'POST', '/feedback', { token: tokens.loneDc, body: JSON.stringify({ cat: 'other', msg: 'x' }) });
api('feedback status missing', 'POST', '/admin/feedback/status', { token: tokens.dev, body: JSON.stringify({ id: '0'.repeat(16), st: 'done' }) });
api('feedback delete missing', 'POST', '/admin/feedback/delete', { token: tokens.dev, body: JSON.stringify({ id: '0'.repeat(16) }) });
api('feedback issue missing', 'POST', '/admin/feedback/issue', { token: tokens.dev, body: JSON.stringify({ id: '0'.repeat(16) }) });
api('discord register', 'POST', '/admin/discord/register', { token: tokens.dev, body: '{}' });
api('discord register staff', 'POST', '/admin/discord/register', { token: tokens.alex, body: '{}' });
api('interaction bad signature', 'POST', '/discord/interactions', { signed: [{ type: 1 }, true] });
api('interaction ping', 'POST', '/discord/interactions', { signed: [{ type: 1 }] });
api('interaction status', 'POST', '/discord/interactions', { signed: [command('status')] });
api('interaction status en', 'POST', '/discord/interactions', { signed: [command('status', [], 'en-US')] });
api('interaction cape', 'POST', '/discord/interactions', { signed: [command('cape', [{ name: 'name', value: 'Steve' }])] });
api('interaction translate', 'POST', '/discord/interactions', { signed: [command('translate', [{ name: 'text', value: 'Weapon Merchant' }, { name: 'lang', value: 'zh_tw' }])] });
api('interaction feedback', 'POST', '/discord/interactions', { signed: [command('feedback', [{ name: 'category', value: 'other' }, { name: 'message', value: 'from the bot' }])] });
api('interaction feedback translation', 'POST', '/discord/interactions', { signed: [command('feedback', [{ name: 'category', value: 'translation' }, { name: 'message', value: 'from the bot' }])] });
api('interaction component', 'POST', '/discord/interactions', { signed: [{ type: 3 }] });
for (const hash of hashes.slice(0, 2)) {
  api('preview ' + hash.slice(0, 8), 'GET', '/cape-preview/' + hash);
  api('preview figure not wearing ' + hash.slice(0, 8), 'GET', '/cape-preview/' + hash + '?u=' + STEVE);
}
api('preview figure wearing', 'GET', '/cape-preview/' + capes.find((c) => c.id === steveId).h + '?u=' + STEVE);
api('preview bad', 'GET', '/cape-preview/zz');
api('unknown api', 'GET', '/nothing');
api('unknown api post', 'POST', '/nothing');

async function runCase(entry) {
  const data = structuredClone(base);
  const env = { CORPUS: memoryKv(data), DISCORD_CLIENT_ID: 'client1', DISCORD_CLIENT_SECRET: 'secret', GITHUB_TOKEN: 'gh-token',
    ADMIN_TOKEN: tokens.admin, DISCORD_PUBLIC_KEY, DISCORD_BOT_TOKEN: 'bot-token', UPLOADS: 'tiered' };
  const headers = { ...(entry.headers || {}) };
  if (entry.token) headers.authorization = 'Bearer ' + entry.token;
  let body = entry.body;
  if (entry.binary) body = entry.binary;
  if (entry.signed) {
    const made = signedInteraction(...entry.signed);
    body = made.body;
    Object.assign(headers, made.headers);
  }
  if (typeof body === 'string' && !headers['content-type']) headers['content-type'] = 'application/json';
  const waits = [];
  const ctx = { waitUntil: (promise) => waits.push(promise), passThroughOnException() {} };
  outbound.length = 0;
  let record;
  try {
    const response = await worker.fetch(new Request('https://wynnchayuan.com' + entry.path, { method: entry.method, headers, body }), env, ctx);
    record = { status: response.status, headers: describeHeaders(response.headers), body: await describeBody(response) };
  } catch (e) {
    record = { threw: String(e && e.message) };
  }
  await Promise.allSettled(waits);
  record.kv = kvDiff(base, data);
  record.fetch = outbound.splice(0);
  return record;
}

const snapshot = { routes: {}, pages: {}, previews: {}, scheduled: null };
for (const entry of C) {
  const key = entry.method + ' ' + entry.path + ' :: ' + entry.name;
  if (snapshot.routes[key]) throw new Error('duplicate case ' + key);
  snapshot.routes[key] = await runCase(entry);
}
for (const p of PATHS) {
  for (const lang of LANGS) {
    const result = await runCase({ method: 'GET', path: p + '?lang=' + lang });
    snapshot.pages[p + ' ' + lang] = { status: result.status, headers: result.headers, body: result.body };
  }
}
const scriptPaths = new Set();
for (const p of PATHS) {
  const response = await worker.fetch(new Request('https://wynnchayuan.com' + p + '?lang=en'), { CORPUS: memoryKv(structuredClone(base)) }, { waitUntil() {} });
  for (const m of (await response.text()).matchAll(/<script src="(\/js\/[^"]+)"/g)) scriptPaths.add(m[1]);
}
for (const p of [...scriptPaths].sort()) snapshot.pages['script ' + p] = await runCase({ method: 'GET', path: p });
snapshot.previews.png = sha((await capePreview(pngTemplate, 'png')).bytes);
snapshot.previews.gif = sha((await capePreview(gifTemplate, 'gif')).bytes);
snapshot.previews.figure = sha((await figurePreview(pngTemplate, 'png', fakeSkin ? new Uint8Array(fakeSkin) : null)).bytes);
snapshot.previews.figureNoSkin = sha((await figurePreview(gifTemplate, 'gif', null)).bytes);
{
  const data = structuredClone(base);
  outbound.length = 0;
  await worker.scheduled({}, { CORPUS: memoryKv(data) }, { waitUntil() {} });
  snapshot.scheduled = { kv: kvDiff(base, data), fetch: outbound.splice(0) };
}

const text = JSON.stringify(snapshot, null, 1) + '\n';
if (mode === 'write') {
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, text);
  console.log('golden: wrote ' + path.relative(process.cwd(), target) + ' (' + Object.keys(snapshot.routes).length + ' routes, '
    + Object.keys(snapshot.pages).length + ' pages)');
} else {
  const have = JSON.parse(fs.readFileSync(target, 'utf8'));
  const differences = [];
  const compare = (section) => {
    const keys = new Set([...Object.keys(have[section] || {}), ...Object.keys(snapshot[section] || {})]);
    for (const key of keys) {
      const a = JSON.stringify(have[section][key]);
      const b = JSON.stringify(snapshot[section][key]);
      if (a !== b) differences.push(section + ' :: ' + key + '\n  was ' + (a || 'missing').slice(0, 600) + '\n  now ' + (b || 'missing').slice(0, 600));
    }
  };
  ['routes', 'pages', 'previews'].forEach(compare);
  if (JSON.stringify(have.scheduled) !== JSON.stringify(snapshot.scheduled)) differences.push('scheduled differs');
  if (differences.length) {
    console.log(differences.join('\n'));
    console.log('golden: ' + differences.length + ' differences');
    process.exit(1);
  }
  console.log('golden: identical (' + Object.keys(snapshot.routes).length + ' routes, ' + Object.keys(snapshot.pages).length + ' pages, previews, scheduled)');
}
