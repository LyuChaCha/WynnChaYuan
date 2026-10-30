import { authority, moderator, staffKey } from './admin.js';
import { account, discordSession, publicId, requestUser } from './auth.js';
import { TEAM_AREAS, addMemberTeams, commitTeams, creditTeamsOf, credits, exactCodes, mergeLangTeam, setPersonTeams } from './credits.js';
import { GitHubError, githubRepository, markdownText } from './github.js';
import { NAME, UUID, hex, json, refuse, readJson } from './http.js';
import { listAll, parseRecord, parseStored } from './kv.js';
import { noteParticipant } from './privacy.js';

export const TR_LANGS = ['zh_tw', 'zh_cn', 'ja_jp', 'ko_kr', 'ru_ru', 'es_es', 'tr_tr'];

const TR_INDEX = 'https://raw.githubusercontent.com/' + 'LyuChaCha/WynnChaYuan' + '/translate-index/';

const TR_TEXT_MAX = 2000;

export const TR_BODY = 16384;

const TR_FILE = /^(?:[a-z0-9-]+\/)?[a-z0-9_-]+\.json$/;

const TR_KEY = /^tl:(?:u:[0-9a-f]{32}|d:\d{17,20}|n:[a-z0-9_]{1,16})$/;

const TR_ID = /^[0-9a-f]{12,24}$/;

export function placeholderProblem(src, text, base) {
  if (typeof text !== 'string' || !text.trim()) return 'empty';
  if (text.length > TR_TEXT_MAX) return 'too long';
  const seq = (s) => String(s).match(/\{[#pu]\}/g) || [];
  const want = seq(src);
  const got = seq(text);
  const noName = (list) => list.filter((t) => t !== '{u}').join('');
  if (noName(want) !== noName(got)) return 'placeholders';
  let at = 0;
  for (const t of got) {
    while (at < want.length && want[at] !== t) at++;
    if (at === want.length) return 'placeholders';
    at++;
  }
  const nums = (s) => String(s).match(/\{~\d*\}/g) || [];
  if (nums(src).length !== nums(text).length) return 'numbers';
  const named = nums(text).filter((n) => n !== '{~}');
  if (named.length && named.length !== nums(text).length) return 'numbering';
  if (named.length) {
    const want = named.map((n) => Number(n.slice(2, -1))).sort((a, b) => a - b);
    if (want.some((n, i) => n !== i + 1)) return 'numbering';
  }
  const colours = (s) => (String(s).match(/\{c\d+\}|\{\/\}/g) || []).sort().join(',');
  if (base && base.trim() && colours(base) !== colours(text)) return 'colours';
  if (String(src).split('\n').length !== text.split('\n').length) return 'lines';
  return null;
}

/** 逐行改，不重 dump：整檔重寫會讓 git 把整個檔當成改過。 */
export function replaceTranslation(text, key, src, value) {
  const nl = text.includes('\r\n') ? '\r\n' : '\n';
  const lines = text.split(nl);
  const encoded = JSON.stringify(value);
  if (key == null) {
    const head = JSON.stringify(src);
    for (let i = 0; i < lines.length; i++) {
      const m = /^(\s*)("(?:[^"\\]|\\.)*")(\s*:\s*)("(?:[^"\\]|\\.)*")(,?\s*)$/.exec(lines[i]);
      if (m && m[2] === head) {
        lines[i] = m[1] + m[2] + m[3] + encoded + m[5];
        return lines.join(nl);
      }
    }
    return null;
  }
  const opener = JSON.stringify(key) + ': {';
  let inside = false;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (!inside) {
      if (line === opener) inside = true;
      continue;
    }
    if (line.startsWith('}')) return null;
    const m = /^(\s*"dst":\s*)("(?:[^"\\]|\\.)*")(,?\s*)$/.exec(lines[i]);
    if (m) {
      lines[i] = m[1] + encoded + m[3];
      return lines.join(nl);
    }
  }
  return null;
}

async function trIdentity(request, env, now) {
  const user = await requestUser(request, env, now);
  if (user) {
    const discord = user.discord || await env.CORPUS.get('l:' + user.uuid);
    return { id: 'u:' + user.uuid, name: user.name, uuid: user.uuid, discord };
  }
  const session = await discordSession(request, env, now);
  if (!session) return null;
  const linked = UUID.test(session.account.u || '') ? session.account.u : null;
  return { id: linked ? 'u:' + linked : 'd:' + session.id, name: session.account.m || session.account.n || session.id,
    uuid: linked, discord: session.id };
}

async function trRoles(request, env, now, me) {
  const langs = new Set();
  const read = async (key) => {
    const record = parseRecord(await env.CORPUS.get(key));
    (Array.isArray(record.langs) ? record.langs : []).forEach((l) => TR_LANGS.includes(l) && langs.add(l));
    return record;
  };
  if (me.uuid) await read('tl:u:' + me.uuid);
  if (me.discord) await read('tl:d:' + me.discord);
  if (me.uuid && NAME.test(me.name || '')) {
    const byName = await env.CORPUS.get('tl:n:' + me.name.toLowerCase());
    if (byName) {
      const record = parseRecord(byName);
      (record.langs || []).forEach((l) => TR_LANGS.includes(l) && langs.add(l));
      const kept = parseRecord(await env.CORPUS.get('tl:u:' + me.uuid));
      const merged = { ...record, ...kept, langs: [...new Set([...(kept.langs || []), ...(record.langs || [])])], m: me.name };
      await env.CORPUS.put('tl:u:' + me.uuid, JSON.stringify(merged));
      await env.CORPUS.delete('tl:n:' + me.name.toLowerCase());
    }
  }
  const mine = await authority(request, env, now);
  return { langs, decide: Boolean(mine), by: mine ? mine.by : me.name };
}

async function trWho(request, env, now) {
  const me = await trIdentity(request, env, now);
  if (!me) return null;
  return { ...me, ...(await trRoles(request, env, now, me)) };
}

const canWrite = (me, lang) => Boolean(me) && (me.decide || me.langs.has(lang));

async function trId(text) {
  return hex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))).slice(0, 16);
}

const storedProposal = (raw) => {
  const record = parseStored(raw);
  return Array.isArray(record?.items) ? record : null;
};

const storedPoll = (raw) => {
  const record = parseStored(raw);
  return Array.isArray(record?.options) ? record : null;
};

export function trMeta(record) {
  const votes = Math.max(0, ...record.items.map((it) => it.votes.length));
  return { f: record.file, s: String(record.src).slice(0, 120), n: record.items.length, v: votes,
    st: record.status, t: record.updated || record.t };
}

export function pollMeta(record) {
  const votes = record.options.reduce((sum, o) => sum + o.votes.length, 0);
  return { s: String(record.term).slice(0, 80), n: record.options.length, v: votes, st: record.status, t: record.updated || record.t };
}

export async function trList(env, prefix) {
  const out = [];
  for (const k of await listAll(env.CORPUS, prefix)) out.push({ id: k.name.slice(prefix.length), ...(k.metadata || {}) });
  return out.sort((a, b) => (b.t || 0) - (a.t || 0));
}

const trLang = (value) => (TR_LANGS.includes(value) ? value : null);

async function handleTrMe(request, env, now) {
  const me = await trWho(request, env, now);
  return json(me ? { signedIn: true, id: me.id, name: me.name, langs: [...me.langs], decide: me.decide }
    : { signedIn: false, langs: [], decide: false }, 200, { 'cache-control': 'no-store' });
}

export async function handleTrIndex(request, env, lang, ctx) {
  if (!trLang(lang)) return refuse('bad lang', 404);
  const cache = typeof caches !== 'undefined' ? caches.default : null;
  const cacheKey = new Request('https://wynnchayuan.com/tr-index/' + lang);
  if (cache) {
    const hit = await cache.match(cacheKey);
    if (hit) return hit;
  }
  let upstream;
  try {
    upstream = await fetch(TR_INDEX + lang + '.json', { cf: { cacheTtl: 600 } });
  } catch (e) {
    return refuse('index unreachable', 502);
  }
  if (upstream.status !== 200) return refuse('index ' + upstream.status, 502);
  const response = new Response(upstream.body, {
    headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'public, max-age=600' },
  });
  if (cache) {
    const copy = response.clone();
    if (ctx && ctx.waitUntil) ctx.waitUntil(cache.put(cacheKey, copy));
    else await cache.put(cacheKey, copy);
  }
  return response;
}

async function handleTrProps(request, env) {
  const url = new URL(request.url);
  const lang = trLang(url.searchParams.get('lang'));
  if (!lang) return refuse('bad lang');
  return json({ props: await trList(env, 'tp:' + lang + ':') }, 200, { 'cache-control': 'no-store' });
}

async function handleTrProp(request, env) {
  const url = new URL(request.url);
  const lang = trLang(url.searchParams.get('lang'));
  const id = url.searchParams.get('id') || '';
  if (!lang || !TR_ID.test(id)) return refuse('bad request');
  const record = storedProposal(await env.CORPUS.get('tp:' + lang + ':' + id));
  return record ? json(record, 200, { 'cache-control': 'no-store' }) : refuse('not found', 404);
}

async function handleTrPropose(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me) return refuse('login first', 401);
  const body = await readJson(request, TR_BODY);
  if (!body) return refuse('bad request');
  const lang = trLang(body.lang);
  if (!lang) return refuse('bad lang');
  if (!canWrite(me, lang)) return refuse('not a translator', 403);
  const file = String(body.file || '');
  const key = body.key == null ? null : String(body.key);
  const src = String(body.src || '');
  const base = typeof body.base === 'string' ? body.base : '';
  const text = typeof body.text === 'string' ? body.text.replace(/\r\n/g, '\n') : '';
  if (!TR_FILE.test(file) || !src || src.length > 4000 || (key != null && key.length > 200)) return refuse('bad request');
  const problem = placeholderProblem(src, text, base);
  if (problem) return refuse(problem);
  const id = await trId(file + '\n' + (key == null ? src : key));
  const storeKey = 'tp:' + lang + ':' + id;
  const before = await env.CORPUS.get(storeKey);
  const record = parseRecord(before);
  if (!record.items) Object.assign(record, { lang, file, key, src, base, items: [], status: 'open', t: now });
  if (record.status !== 'open') {
    record.status = 'open';
    delete record.done;
  }
  for (const it of record.items) it.votes = it.votes.filter((v) => v !== me.id);
  const same = record.items.find((it) => it.text === text);
  if (same) {
    same.votes.push(me.id);
  } else {
    if (record.items.length >= 20) return refuse('too many');
    record.items.push({ id: (await trId(text + now)).slice(0, 12), text, by: me.id, name: me.name, t: now, votes: [me.id] });
  }
  record.updated = now;
  await env.CORPUS.put(storeKey, JSON.stringify(record), { metadata: trMeta(record) });
  await noteParticipant(env, me.id, storeKey, before);
  return json({ ok: true, id, prop: record });
}

async function handleTrVote(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me) return refuse('login first', 401);
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang || !TR_ID.test(String(body.id || ''))) return refuse('bad request');
  if (!canWrite(me, lang)) return refuse('not a translator', 403);
  const storeKey = 'tp:' + lang + ':' + body.id;
  const raw = await env.CORPUS.get(storeKey);
  const record = storedProposal(raw);
  if (!record) return refuse('not found', 404);
  if (record.status !== 'open') return refuse('closed', 409);
  const target = record.items.find((it) => it.id === body.item);
  if (!target) return refuse('not found', 404);
  const had = target.votes.includes(me.id);
  for (const it of record.items) it.votes = it.votes.filter((v) => v !== me.id);
  if (!had) target.votes.push(me.id);
  record.updated = now;
  await env.CORPUS.put(storeKey, JSON.stringify(record), { metadata: trMeta(record) });
  await noteParticipant(env, me.id, storeKey, raw);
  return json({ ok: true, prop: record });
}

async function handleTrAccept(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me || !me.decide) return refuse('forbidden', 403);
  if (!env.GITHUB_TOKEN) return refuse('github not configured', 503);
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang || !TR_ID.test(String(body.id || ''))) return refuse('bad request');
  const storeKey = 'tp:' + lang + ':' + body.id;
  const record = storedProposal(await env.CORPUS.get(storeKey));
  if (!record) return refuse('not found', 404);
  const item = record.items.find((it) => it.id === body.item);
  if (!item) return refuse('not found', 404);
  const repo = githubRepository(env);
  let created = null;
  try {
    const day = new Date(now).toISOString().slice(0, 10).replace(/-/g, '');
    let branch = 'translate/' + lang.replace('_', '-') + '-' + day;
    const existing = await repo.openPullFor(branch);
    let url = existing.url;
    if (existing.exists && !url) branch += '-' + now;
    const reuse = Boolean(url);
    if (!reuse) {
      await repo.createBranch(branch);
      created = branch;
    }
    const root = 'src/main/resources/assets/wynnchayuan/translations/' + lang + '/';
    const targets = [record.file];
    if (record.key != null && record.file.startsWith('quest/')) targets.push('quest-dialogue.json');
    if (record.key != null && record.file.startsWith('secret/')) targets.push('secret-dialogue.json');
    const short = record.src.replace(/\s+/g, ' ').slice(0, 50);
    for (const name of targets) {
      const current = await repo.readFile(root + name, branch);
      const changed = replaceTranslation(current.text, record.key, record.src, item.text);
      if (changed == null) {
        if (created) await repo.deleteBranch(created);
        return refuse('line not found', 409);
      }
      await repo.writeFile(root + name, { message: '譯文（' + lang + '）：' + short, text: changed, sha: current.sha, branch });
    }
    if (!reuse) {
      url = String(await repo.openPull({
        title: '協作翻譯：' + lang + '（' + day + '）',
        head: branch,
        body: '網站「協作翻譯」頁定案的譯文。同一個語言同一天定案的都會加進這個 PR。\n\n'
          + '第一條由 ' + markdownText(me.by) + ' 定案。每一條的原文、提案人與票數記在網站上。',
      }) || '');
    }
    if (!url) return refuse('github pull request missing', 502);
    record.status = 'accepted';
    record.done = { item: item.id, by: me.by, t: now, pr: url };
    record.updated = now;
    await env.CORPUS.put(storeKey, JSON.stringify(record), { metadata: trMeta(record) });
    return json({ ok: true, url, prop: record });
  } catch (e) {
    if (created) await repo.deleteBranch(created);
    return refuse(e instanceof GitHubError ? e.message : 'github unreachable', 502);
  }
}

async function handleTrReject(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me || !me.decide) return refuse('forbidden', 403);
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang || !TR_ID.test(String(body.id || ''))) return refuse('bad request');
  const storeKey = 'tp:' + lang + ':' + body.id;
  const record = storedProposal(await env.CORPUS.get(storeKey));
  if (!record) return refuse('not found', 404);
  record.status = 'rejected';
  record.done = { by: me.by, t: now };
  record.updated = now;
  await env.CORPUS.put(storeKey, JSON.stringify(record), { metadata: trMeta(record) });
  return json({ ok: true, prop: record });
}

async function handleTrPolls(request, env) {
  const lang = trLang(new URL(request.url).searchParams.get('lang'));
  if (!lang) return refuse('bad lang');
  return json({ polls: await trList(env, 'tq:' + lang + ':') }, 200, { 'cache-control': 'no-store' });
}

async function handleTrPoll(request, env) {
  const url = new URL(request.url);
  const lang = trLang(url.searchParams.get('lang'));
  const id = url.searchParams.get('id') || '';
  if (!lang || !TR_ID.test(id)) return refuse('bad request');
  const record = storedPoll(await env.CORPUS.get('tq:' + lang + ':' + id));
  return record ? json(record, 200, { 'cache-control': 'no-store' }) : refuse('not found', 404);
}

const cleanOption = (text) => (typeof text === 'string' ? text.trim().replace(/\s+/g, ' ') : '');

async function handleTrPollOpen(request, env, now) {
  const me = await trWho(request, env, now);
  if (!me) return refuse('login first', 401);
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang) return refuse('bad lang');
  if (!canWrite(me, lang)) return refuse('not a translator', 403);
  const term = cleanOption(body.term);
  const note = typeof body.note === 'string' ? body.note.trim().slice(0, 500) : '';
  if (!term || term.length > 80) return refuse('bad term');
  const options = [...new Set((Array.isArray(body.options) ? body.options : []).map(cleanOption).filter(Boolean))];
  if (options.some((o) => o.length > 80) || options.length > 10) return refuse('bad option');
  const id = (await trId(lang + term + now)).slice(0, 12);
  const record = { lang, term, note, by: me.id, name: me.name, t: now, status: 'open', options: [] };
  for (const text of options) {
    record.options.push({ id: (await trId(text + now)).slice(0, 12), text, by: me.id, name: me.name, t: now, votes: [] });
  }
  await env.CORPUS.put('tq:' + lang + ':' + id, JSON.stringify(record), { metadata: pollMeta(record) });
  await noteParticipant(env, me.id, 'tq:' + lang + ':' + id, null);
  return json({ ok: true, id, poll: record });
}

async function pollFor(request, env, now, needDecide) {
  const me = await trWho(request, env, now);
  if (!me) return { error: refuse('login first', 401) };
  const body = await readJson(request, TR_BODY);
  const lang = trLang(body && body.lang);
  if (!lang || !TR_ID.test(String(body.id || ''))) return { error: refuse('bad request') };
  if (needDecide ? !me.decide : !canWrite(me, lang)) return { error: refuse(needDecide ? 'forbidden' : 'not a translator', 403) };
  const storeKey = 'tq:' + lang + ':' + body.id;
  const raw = await env.CORPUS.get(storeKey);
  const record = storedPoll(raw);
  if (!record) return { error: refuse('not found', 404) };
  return { me, body, storeKey, raw, record };
}

async function savePoll(env, got, now) {
  got.record.updated = now;
  await env.CORPUS.put(got.storeKey, JSON.stringify(got.record), { metadata: pollMeta(got.record) });
  return json({ ok: true, poll: got.record });
}

async function handleTrPollOption(request, env, now) {
  const got = await pollFor(request, env, now, false);
  if (got.error) return got.error;
  if (got.record.status !== 'open') return refuse('closed', 409);
  const text = cleanOption(got.body.text);
  if (!text || text.length > 80) return refuse('bad option');
  for (const o of got.record.options) o.votes = o.votes.filter((v) => v !== got.me.id);
  const same = got.record.options.find((o) => o.text === text);
  if (same) same.votes.push(got.me.id);
  else {
    if (got.record.options.length >= 12) return refuse('too many');
    got.record.options.push({ id: (await trId(text + now)).slice(0, 12), text, by: got.me.id, name: got.me.name, t: now,
      votes: [got.me.id] });
  }
  await noteParticipant(env, got.me.id, got.storeKey, got.raw);
  return savePoll(env, got, now);
}

async function handleTrPollVote(request, env, now) {
  const got = await pollFor(request, env, now, false);
  if (got.error) return got.error;
  if (got.record.status !== 'open') return refuse('closed', 409);
  const target = got.record.options.find((o) => o.id === got.body.option);
  if (!target) return refuse('not found', 404);
  const had = target.votes.includes(got.me.id);
  for (const o of got.record.options) o.votes = o.votes.filter((v) => v !== got.me.id);
  if (!had) target.votes.push(got.me.id);
  await noteParticipant(env, got.me.id, got.storeKey, got.raw);
  return savePoll(env, got, now);
}

async function handleTrPollDecide(request, env, now) {
  const got = await pollFor(request, env, now, true);
  if (got.error) return got.error;
  const target = got.record.options.find((o) => o.id === got.body.option);
  if (!target) return refuse('not found', 404);
  got.record.status = 'decided';
  got.record.decided = { option: target.id, text: target.text, by: got.me.by, t: now };
  return savePoll(env, got, now);
}

async function handleTrPollReopen(request, env, now) {
  const got = await pollFor(request, env, now, true);
  if (got.error) return got.error;
  got.record.status = 'open';
  delete got.record.decided;
  return savePoll(env, got, now);
}

async function translatorList(env, list) {
  const out = [];
  for (const k of await listAll(env.CORPUS, 'tl:')) {
    const record = parseRecord(await env.CORPUS.get(k.name));
    const key = k.name.slice(3);
    out.push({ key, kind: key[0], id: key.slice(2), langs: record.langs || [], name: record.m || null,
      team: creditTeamsOf(list, record.m),
      discord: record.dn || null, note: record.n || '', t: record.t || null, by: record.by || null });
  }
  return out.sort((a, b) => (b.t || 0) - (a.t || 0));
}

export async function handleTranslators(request, env, now) {
  if (!await moderator(request, env, now)) return refuse('forbidden', 403);
  return json({ translators: await translatorList(env, await credits(now)) }, 200, { 'cache-control': 'no-store' });
}

async function translatorIdentity(env, key, who) {
  const found = {};
  if (key.startsWith('n:')) found.m = String(who).trim();
  const linked = key.startsWith('d:') ? key.slice(2) : key.startsWith('u:') ? await env.CORPUS.get('l:' + key.slice(2)) : null;
  const known = linked ? await account(env, linked) : null;
  if (known) {
    if (known.n) found.dn = known.n;
    if (known.m && (key.startsWith('d:') || known.u === key.slice(2))) found.m = known.m;
  }
  if (key.startsWith('u:') && !found.m) {
    const cape = await env.CORPUS.getWithMetadata('cape:' + await publicId(key.slice(2)));
    if (cape && cape.metadata && NAME.test(cape.metadata.n || '')) found.m = cape.metadata.n;
  }
  return found;
}

export async function handleTranslatorAdd(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return refuse('forbidden', 403);
  const body = await readJson(request, TR_BODY);
  if (!body) return refuse('bad request');
  const key = staffKey(body.who);
  if (!key) return refuse('bad who');
  const langs = [...new Set((Array.isArray(body.langs) ? body.langs : []).filter((l) => TR_LANGS.includes(l)))];
  if (!langs.length) return refuse('bad langs');
  const note = typeof body.note === 'string' ? body.note.trim() : '';
  if (note.length > 60) return refuse('bad note');
  const record = { langs, n: note, t: now, by, ...(await translatorIdentity(env, key, body.who)) };
  await env.CORPUS.put('tl:' + key, JSON.stringify(record));
  const listed = creditTeamsOf(await credits(now), record.m);
  if (!listed) return json({ ok: true, key, team: null });
  if (langs.every((code) => listed.includes(code))) return json({ ok: true, key, team: { teams: listed } });
  const done = await commitTeams(env, by, now, (text) => addMemberTeams(text, record.m, langs));
  if (done.why && done.why !== 'unchanged') return json({ ok: true, key, team: { why: done.why } });
  return json({ ok: true, key, team: done.why ? { teams: listed } : { url: done.url, added: done.added } });
}

export async function handleLangTeam(request, env, now) {
  if (!await moderator(request, env, now)) return refuse('forbidden', 403);
  const list = await credits(now);
  const translators = await translatorList(env, list);
  return json({ people: mergeLangTeam(list ? list.team : [], translators), credits: Boolean(list) }, 200,
    { 'cache-control': 'no-store' });
}

export async function handleLangTeamSet(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return refuse('forbidden', 403);
  const body = await readJson(request, TR_BODY);
  if (!body) return refuse('bad request');
  const key = staffKey(body.who);
  if (!key) return refuse('bad who');
  const langs = body.langs === undefined ? null : exactCodes(body.langs, TR_LANGS);
  if (body.langs !== undefined && !langs) return refuse('bad team');
  const areas = body.areas === undefined ? null : exactCodes(body.areas, TEAM_AREAS);
  if (body.areas !== undefined && !areas) return refuse('bad area');
  if (!langs && !areas) return refuse('bad request');
  const note = typeof body.note === 'string' ? body.note.trim() : null;
  if (note && note.length > 60) return refuse('bad note');
  const identity = await translatorIdentity(env, key, body.who);
  const mc = identity.m || null;
  const list = await credits(now);
  const listed = Boolean(mc && list && list.names.has(mc.toLowerCase()));
  if (areas && !listed) return refuse(list ? 'not listed' : 'try later', list ? 400 : 503);
  let rights = null;
  if (langs) {
    const matched = [];
    for (const one of await translatorList(env, null)) {
      if (one.key === key || (mc && one.name && one.name.toLowerCase() === mc.toLowerCase())) matched.push(one.key);
    }
    if (!langs.length) {
      for (const k of matched) await env.CORPUS.delete('tl:' + k);
    } else {
      const keys = matched.length ? matched : [key];
      for (const k of keys) {
        const old = parseRecord(await env.CORPUS.get('tl:' + k));
        const record = { ...old, ...identity, ...(old.m ? { m: old.m } : {}), langs, t: now, by };
        record.n = note === null ? (old.n || '') : note;
        await env.CORPUS.put('tl:' + k, JSON.stringify(record));
      }
      rights = { keys, langs };
    }
  }
  let team = null;
  if (listed) {
    const done = await commitTeams(env, by, now, (text) => setPersonTeams(text, mc, langs, areas));
    team = done.why === 'unchanged' ? { unchanged: true } : done.why ? { why: done.why } : { url: done.url, added: done.added };
  } else if (langs && mc && !list) {
    team = { why: 'try later' };
  }
  return json({ ok: true, mc, listed, rights, team });
}

export async function handleTranslatorRemove(request, env, now) {
  if (!await moderator(request, env, now)) return refuse('forbidden', 403);
  const body = await readJson(request);
  const key = body && typeof body.key === 'string' ? 'tl:' + body.key : '';
  if (!TR_KEY.test(key)) return refuse('bad request');
  await env.CORPUS.delete(key);
  return json({ ok: true });
}

export async function trRoute(route, request, env, now) {
  if (route === 'GET /tr/me') return handleTrMe(request, env, now);
  if (route === 'GET /tr/props') return handleTrProps(request, env);
  if (route === 'GET /tr/prop') return handleTrProp(request, env);
  if (route === 'POST /tr/propose') return handleTrPropose(request, env, now);
  if (route === 'POST /tr/vote') return handleTrVote(request, env, now);
  if (route === 'POST /tr/accept') return handleTrAccept(request, env, now);
  if (route === 'POST /tr/reject') return handleTrReject(request, env, now);
  if (route === 'GET /tr/polls') return handleTrPolls(request, env);
  if (route === 'GET /tr/poll') return handleTrPoll(request, env);
  if (route === 'POST /tr/poll') return handleTrPollOpen(request, env, now);
  if (route === 'POST /tr/poll/option') return handleTrPollOption(request, env, now);
  if (route === 'POST /tr/poll/vote') return handleTrPollVote(request, env, now);
  if (route === 'POST /tr/poll/decide') return handleTrPollDecide(request, env, now);
  if (route === 'POST /tr/poll/reopen') return handleTrPollReopen(request, env, now);
  return refuse('not found', 404);
}

export async function scrubTr(request, env, now, answer) {
  if (!(answer.headers.get('content-type') || '').includes('json')) return answer;
  const text = await answer.text();
  if (!text.includes('"ghosts"') || await moderator(request, env, now)) {
    return new Response(text, { status: answer.status, headers: answer.headers });
  }
  const body = parseStored(text);
  if (!body) return new Response(text, { status: answer.status, headers: answer.headers });
  const strip = (value) => {
    if (Array.isArray(value)) value.forEach(strip);
    else if (value && typeof value === 'object') {
      delete value.ghosts;
      Object.values(value).forEach(strip);
    }
  };
  strip(body);
  return new Response(JSON.stringify(body), { status: answer.status, headers: answer.headers });
}
