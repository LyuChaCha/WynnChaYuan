import { moderator } from './admin.js';
import { DISCORD_ID, account, discordSession, requestUser } from './auth.js';
import { LANG_LABEL } from './discord-bot.js';
import { githubClient } from './github.js';
import { clip, hex, json, refuse, readJson } from './http.js';
import { claimSlot, parseRecord } from './kv.js';
import { ownerEntry } from './privacy.js';
import { TR_BODY, TR_LANGS, trList } from './translate.js';

export const FEEDBACK_CATS = ['translation', 'content', 'website', 'feature', 'other'];

export const FEEDBACK_MAX = 2000;

export const FEEDBACK_MIN = 3;

export const FEEDBACK_WHERE_MAX = 200;

export const FEEDBACK_PER_HOUR = 5;

export const FEEDBACK_KEEP_DAYS = 180;

const FEEDBACK_ID = /^[0-9a-f]{16}$/;

const FEEDBACK_CAT_ZH = { translation: '譯文建議', content: '內容建議', website: '網站建議', feature: '功能建議', other: '其他' };

const FEEDBACK_LABEL = 'translation';

export function feedbackCheck(input) {
  if (!input || typeof input !== 'object') return { why: 'bad request' };
  const cat = String(input.cat || '');
  if (!FEEDBACK_CATS.includes(cat)) return { why: 'bad category' };
  const maintainCapes = (s) => String(s).replace(/\r\n?/g, '\n').replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/g, '');
  const msg = typeof input.msg === 'string' ? maintainCapes(input.msg).trim() : '';
  if (msg.length < FEEDBACK_MIN) return { why: 'too short' };
  if (msg.length > FEEDBACK_MAX) return { why: 'too long' };
  const where = typeof input.where === 'string' ? maintainCapes(input.where).replace(/\s+/g, ' ').trim() : '';
  if (where.length > FEEDBACK_WHERE_MAX) return { why: 'where too long' };
  const lang = TR_LANGS.includes(input.lang) ? input.lang : null;
  return { entry: { cat, msg, where, lang } };
}

async function feedbackPerson(request, env, now) {
  const session = await discordSession(request, env, now);
  if (session) return { id: session.id, name: session.account.n || session.id };
  const user = await requestUser(request, env, now);
  if (!user) return null;
  const discord = user.discord || await env.CORPUS.get('l:' + user.uuid);
  const found = discord ? await account(env, discord) : null;
  return found ? { id: discord, name: found.n || discord } : { error: 'discord first' };
}

const FEEDBACK_WINDOW_MS = 3600 * 1000;

async function feedbackGate(env, id, now) {
  const slot = await claimSlot(env.CORPUS, 'fbr:' + id + ':', { windowMs: FEEDBACK_WINDOW_MS, limit: FEEDBACK_PER_HOUR });
  return slot.granted ? {} : { until: slot.oldest + FEEDBACK_WINDOW_MS };
}

function feedbackMeta(record) {
  const meta = { c: record.cat, st: record.st, t: record.t, n: clip(record.name, 40),
    s: clip(record.msg.replace(/\s+/g, ' '), 100) };
  if (record.issue) meta.i = record.issue.url;
  if (record.issueError) meta.e = record.issueError;
  return meta;
}

async function saveFeedback(env, record) {
  const options = { metadata: feedbackMeta(record) };
  if (record.st === 'done') options.expirationTtl = FEEDBACK_KEEP_DAYS * 86400;
  await env.CORPUS.put('fb:' + record.id, JSON.stringify(record), options);
  if (DISCORD_ID.test(String(record.uid || ''))) {
    await env.CORPUS.put(ownerEntry('d:' + record.uid, 'fb:' + record.id), '',
      options.expirationTtl ? { expirationTtl: options.expirationTtl } : undefined);
  }
}

function fenced(text) {
  const runs = String(text).match(/`+/g) || [];
  const bar = '`'.repeat(Math.max(3, ...runs.map((run) => run.length + 1)));
  return bar + '\n' + text + '\n' + bar;
}

export function feedbackIssue(record) {
  const kind = FEEDBACK_CAT_ZH[record.cat] || record.cat;
  const name = String(record.name || '').replace(/[`\r\n]/g, '').trim().slice(0, 64) || '—';
  const lines = [
    '從 ' + (record.via === 'bot' ? 'Discord 機器人的 /回饋' : 'wynnchayuan.com/feedback') + ' 送來的' + kind + '。', '',
    '**內容**', '', fenced(record.msg), '',
  ];
  if (record.where) lines.push('**位置**', '', fenced(record.where), '');
  lines.push('**語言**：' + (record.lang ? LANG_LABEL[record.lang] + '（`' + record.lang + '`）' : '沒有指定'));
  lines.push('**送出的人**：`' + name + '`（Discord 顯示名稱）');
  lines.push('', '網站上的編號 `' + record.id + '`；處理完請到網站的管理頁標成已處理。');
  return { title: kind + '：' + clip(record.msg.replace(/\s+/g, ' ').trim(), 60), body: lines.join('\n'), labels: [FEEDBACK_LABEL] };
}

async function openFeedbackIssue(env, record) {
  if (!env.GITHUB_TOKEN) return { error: 'github not configured' };
  try {
    const res = await githubClient(env)('/issues', { method: 'POST', body: JSON.stringify(feedbackIssue(record)) });
    if (res.status !== 201) return { error: 'github ' + res.status };
    const made = await res.json();
    return { url: String(made.html_url || ''), n: Number(made.number) || 0 };
  } catch (e) {
    return { error: 'github unreachable' };
  }
}

async function fileFeedbackIssue(env, record) {
  const made = await openFeedbackIssue(env, record);
  if (made.error) {
    record.issueError = made.error;
  } else {
    record.issue = { url: made.url, n: made.n };
    delete record.issueError;
  }
  await saveFeedback(env, record);
}

export async function submitFeedback(env, person, input, via, now) {
  const checked = feedbackCheck(input);
  if (checked.why) return { why: checked.why, status: 400 };
  const gate = await feedbackGate(env, person.id, now);
  if (gate.until) return { why: 'slow down', status: 429, until: gate.until };
  const id = hex(crypto.getRandomValues(new Uint8Array(8)));
  const record = { id, ...checked.entry, uid: person.id, name: String(person.name).slice(0, 64), via, t: now, st: 'open' };
  await saveFeedback(env, record);
  if (record.cat === 'translation') await fileFeedbackIssue(env, record);
  return { record };
}

export async function handleFeedback(request, env, now) {
  const person = await feedbackPerson(request, env, now);
  if (!person) return refuse('login first', 401);
  if (person.error) return refuse(person.error, 401);
  const body = await readJson(request, TR_BODY);
  if (!body) return refuse('bad request');
  const got = await submitFeedback(env, person, body, 'web', now);
  if (got.why) return json({ ok: false, why: got.why, until: got.until }, got.status);
  const { record } = got;
  return json({ ok: true, id: record.id, issue: record.issue ? record.issue.url : null,
    issueFailed: Boolean(record.issueError) });
}

export async function handleFeedbackList(request, env, now) {
  if (!await moderator(request, env, now)) return refuse('forbidden', 403);
  const url = new URL(request.url);
  const cat = url.searchParams.get('cat') || '';
  const st = url.searchParams.get('st') || '';
  const all = await trList(env, 'fb:');
  const counts = { open: 0, done: 0 };
  for (const meta of all) counts[meta.st === 'done' ? 'done' : 'open']++;
  const picked = all.filter((meta) => (!cat || meta.c === cat) && (!st || meta.st === st));
  const items = [];
  for (const meta of picked.slice(0, 200)) {
    const record = parseRecord(await env.CORPUS.get('fb:' + meta.id));
    if (record.id) items.push(record);
  }
  return json({ items, total: picked.length, counts }, 200, { 'cache-control': 'no-store' });
}

async function feedbackFor(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return { error: refuse('forbidden', 403) };
  const body = await readJson(request);
  const id = body ? String(body.id || '') : '';
  if (!FEEDBACK_ID.test(id)) return { error: refuse('bad request') };
  const raw = await env.CORPUS.get('fb:' + id);
  if (!raw) return { error: refuse('not found', 404) };
  return { by, body, record: parseRecord(raw) };
}

export async function handleFeedbackStatus(request, env, now) {
  const got = await feedbackFor(request, env, now);
  if (got.error) return got.error;
  if (!['open', 'done'].includes(got.body.st)) return refuse('bad request');
  got.record.st = got.body.st;
  if (got.body.st === 'done') got.record.done = { by: got.by, t: now };
  else delete got.record.done;
  await saveFeedback(env, got.record);
  return json({ ok: true, item: got.record });
}

export async function handleFeedbackDelete(request, env, now) {
  const got = await feedbackFor(request, env, now);
  if (got.error) return got.error;
  await env.CORPUS.delete('fb:' + got.record.id);
  if (DISCORD_ID.test(String(got.record.uid || ''))) await env.CORPUS.delete(ownerEntry('d:' + got.record.uid, 'fb:' + got.record.id));
  return json({ ok: true });
}

export async function handleFeedbackIssue(request, env, now) {
  const got = await feedbackFor(request, env, now);
  if (got.error) return got.error;
  if (got.record.cat !== 'translation') return refuse('not a translation', 409);
  if (got.record.issue) return json({ ok: true, item: got.record });
  await fileFeedbackIssue(env, got.record);
  return got.record.issue ? json({ ok: true, item: got.record })
    : json({ ok: false, why: got.record.issueError, item: got.record }, 502);
}
