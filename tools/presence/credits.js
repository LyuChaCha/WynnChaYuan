import { moderator } from './admin.js';
import { REPO, base64ToUtf8, githubClient, utf8ToBase64 } from './github.js';
import { NAME, json, no, readJson } from './http.js';
import { TR_LANGS } from './translate.js';

const CREDITS_URL = 'https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/'
  + 'src/main/resources/assets/wynnchayuan/credits.json';

const CREDITS_MAX_AGE_MS = 10 * 60 * 1000;

let cachedCredits = null;

const COLOUR = /^#[0-9a-fA-F]{6}$/;

export function teamOf(sections) {
  const team = [];
  for (const section of Array.isArray(sections) ? sections : []) {
    const found = /\.(developers|sponsors|contributors)$/.exec(String(section && section.key));
    if (!found || !Array.isArray(section.members)) continue;
    const members = [];
    for (const member of section.members) {
      if (!member || typeof member.name !== 'string' || !member.name.trim()) continue;
      const entry = { name: member.name.trim().slice(0, 40) };
      if (typeof member.mc === 'string' && NAME.test(member.mc)) entry.mc = member.mc;
      if (typeof member.color === 'string' && COLOUR.test(member.color)) entry.color = member.color;
      const teams = knownCodes(member.teams, TR_LANGS);
      if (teams.length) entry.teams = teams;
      const areas = knownCodes(member.areas, TEAM_AREAS);
      if (areas.length) entry.areas = areas;
      members.push(entry);
    }
    team.push({ role: found[1], color: COLOUR.test(section.color || '') ? section.color : null, members });
  }
  return team;
}

export async function handleTeam(env, now) {
  const list = await credits(now);
  if (!list) return no('try later', 503);
  const guilds = (await guildList(env, now)).map((guild) => ({
    slug: guild.slug, name: guild.name, tag: guild.tag || '', icon: guild.i || 0, banner: guild.b || 0 }));
  return json({ team: list.team, guilds }, 200, { 'cache-control': 'public, max-age=300' });
}

const GUILD_NAME = /^[A-Za-z](?:[A-Za-z ]{1,28})[A-Za-z]$/;

const GUILD_TAG = /^[A-Za-z]{3,4}$/;

export const GUILD_SLUG = /^[a-z](?:[a-z-]{1,28})[a-z]$/;

export const MAX_GUILD_IMAGE = 512 * 1024;

const MAX_GUILDS = 24;

export function guildSlug(name) {
  const raw = String(name == null ? '' : name).trim().replace(/\s+/g, ' ');
  return GUILD_NAME.test(raw) ? raw.toLowerCase().replace(/ /g, '-') : null;
}

export function imageType(bytes) {
  const b = bytes;
  if (b.length > 8 && b[0] === 0x89 && b[1] === 0x50 && b[2] === 0x4e && b[3] === 0x47) return 'image/png';
  if (b.length > 3 && b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff) return 'image/jpeg';
  if (b.length > 12 && b[0] === 0x52 && b[1] === 0x49 && b[2] === 0x46 && b[3] === 0x46
    && b[8] === 0x57 && b[9] === 0x45 && b[10] === 0x42 && b[11] === 0x50) return 'image/webp';
  return null;
}

let cachedGuilds = null;

async function guildList(env, now) {
  if (cachedGuilds && cachedGuilds.kv === env.CORPUS && now - cachedGuilds.t < 60 * 1000) return cachedGuilds.list;
  let list = [];
  try {
    const parsed = JSON.parse((await env.CORPUS.get('guilds')) || '[]');
    if (Array.isArray(parsed)) {
      list = parsed.filter((g) => g && typeof g.slug === 'string' && GUILD_SLUG.test(g.slug) && typeof g.name === 'string');
    }
  } catch (e) {
    list = [];
  }
  cachedGuilds = { t: now, kv: env.CORPUS, list };
  return list;
}

async function saveGuilds(env, list, now) {
  await env.CORPUS.put('guilds', JSON.stringify(list));
  cachedGuilds = { t: now, kv: env.CORPUS, list };
}

async function freshGuilds(env, now) {
  cachedGuilds = null;
  return [...await guildList(env, now)];
}

export async function handleGuilds(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  return json({ guilds: await freshGuilds(env, now) }, 200, { 'cache-control': 'no-store' });
}

export async function handleGuildAdd(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  const body = await readJson(request);
  if (!body) return no('bad request');
  const slug = guildSlug(body.name);
  if (!slug) return no('bad name');
  const tag = body.tag == null ? '' : String(body.tag).trim();
  if (tag && !GUILD_TAG.test(tag)) return no('bad tag');
  const list = await freshGuilds(env, now);
  if (list.some((guild) => guild.slug === slug)) return no('already', 409);
  if (list.length >= MAX_GUILDS) return no('too many', 409);
  list.push({ slug, name: String(body.name).trim().replace(/\s+/g, ' '), tag, t: now, by, i: 0, b: 0 });
  await saveGuilds(env, list, now);
  return json({ ok: true, slug });
}

export async function handleGuildImage(request, env, now, slug, kind) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  if (Number(request.headers.get('content-length') || 0) > MAX_GUILD_IMAGE) return no('too large', 413);
  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length > MAX_GUILD_IMAGE) return no('too large', 413);
  const type = imageType(bytes);
  if (!type) return no('bad image');
  const list = await freshGuilds(env, now);
  const guild = list.find((one) => one.slug === slug);
  if (!guild) return no('no such guild', 404);
  await env.CORPUS.put('gimg:' + slug + ':' + kind, bytes, { metadata: { t: type } });
  guild[kind === 'icon' ? 'i' : 'b'] = now;
  await saveGuilds(env, list, now);
  return json({ ok: true, v: now });
}

export async function handleGuildRemove(request, env, now) {
  if (!await moderator(request, env, now)) return no('forbidden', 403);
  const body = await readJson(request);
  const slug = body && typeof body.slug === 'string' ? body.slug : '';
  if (!GUILD_SLUG.test(slug)) return no('bad request');
  const list = await freshGuilds(env, now);
  await saveGuilds(env, list.filter((guild) => guild.slug !== slug), now);
  await env.CORPUS.delete('gimg:' + slug + ':icon');
  await env.CORPUS.delete('gimg:' + slug + ':banner');
  return json({ ok: true });
}

export async function handleGuildPicture(env, slug, kind) {
  const found = await env.CORPUS.getWithMetadata('gimg:' + slug + ':' + kind, { type: 'arrayBuffer' });
  if (!found || !found.value || !found.metadata) return no('not found', 404);
  return new Response(found.value, {
    headers: {
      'content-type': found.metadata.t,
      'cache-control': 'public, max-age=86400',
      'x-content-type-options': 'nosniff',
      'content-security-policy': "default-src 'none'",
    },
  });
}

export async function credits(now) {
  if (cachedCredits && now - cachedCredits.t < CREDITS_MAX_AGE_MS) return cachedCredits;
  try {
    const answer = await fetch(CREDITS_URL, { cf: { cacheTtl: 300 } });
    if (answer.status === 200) {
      const names = new Set();
      const developers = new Set();
      const sections = (await answer.json()).sections || [];
      for (const section of sections) {
        const dev = typeof section.key === 'string' && section.key.endsWith('.developers');
        for (const member of section.members || []) {
          if (typeof member.mc !== 'string' || !member.mc) continue;
          names.add(member.mc.toLowerCase());
          if (dev) developers.add(member.mc.toLowerCase());
        }
      }
      cachedCredits = { t: now, names, developers, team: teamOf(sections) };
    }
  } catch (e) {
  }
  return cachedCredits;
}

export async function isDeveloper(name, now) {
  const list = await credits(now);
  return Boolean(list && list.developers.has(name.toLowerCase()));
}

const CREDITS_PATH = 'src/main/resources/assets/wynnchayuan/credits.json';

const SECTIONS = { developers: '開發者', sponsors: '贊助者', contributors: '貢獻者' };

export const TEAM_AREAS = ['translation', 'proofreading', 'capture', 'testing', 'code', 'design', 'website', 'community'];

function knownCodes(value, allowed) {
  return Array.isArray(value) ? allowed.filter((code) => value.includes(code)) : [];
}

export function exactCodes(value, allowed) {
  if (!Array.isArray(value) || value.length > allowed.length * 2) return null;
  if (!value.every((code) => typeof code === 'string' && allowed.includes(code))) return null;
  return knownCodes(value, allowed);
}

export function teamsInput(entry) {
  if (!entry || typeof entry !== 'object') return { why: 'bad request' };
  if (!Object.hasOwn(SECTIONS, entry.section)) return { why: 'bad section' };
  const who = typeof entry.who === 'string' ? entry.who.trim() : '';
  if (!who || who.length > 40) return { why: 'bad who' };
  const teams = exactCodes(entry.teams, TR_LANGS);
  if (!teams) return { why: 'bad team' };
  const areas = exactCodes(entry.areas, TEAM_AREAS);
  if (!areas) return { why: 'bad area' };
  return { section: entry.section, who, teams, areas };
}

export function setMemberTeams(text, entry) {
  const input = teamsInput(entry);
  if (input.why) return input;
  let doc;
  try {
    doc = JSON.parse(text);
  } catch (e) {
    return { why: 'bad credits' };
  }
  const target = (Array.isArray(doc.sections) ? doc.sections : []).find((item) => item && typeof item.key === 'string'
    && item.key.endsWith('.' + input.section));
  if (!target || !Array.isArray(target.members)) return { why: 'bad credits' };
  const lower = input.who.toLowerCase();
  const member = target.members.find((one) => one && typeof one.mc === 'string' && one.mc.toLowerCase() === lower)
    || target.members.find((one) => one && !one.mc && one.name === input.who);
  if (!member) return { why: 'not found' };
  const held = (field) => JSON.stringify(field in member ? member[field] : []);
  if (held('teams') === JSON.stringify(input.teams) && held('areas') === JSON.stringify(input.areas)) {
    return { why: 'unchanged' };
  }
  if (input.teams.length) member.teams = input.teams;
  else delete member.teams;
  if (input.areas.length) member.areas = input.areas;
  else delete member.areas;
  return { text: JSON.stringify(doc, null, 2) + '\n', name: String(member.name || input.who) };
}

export function addMember(text, entry) {
  if (!entry || typeof entry !== 'object') return { why: 'bad request' };
  const { section, name, mc, color } = entry;
  if (!Object.hasOwn(SECTIONS, section)) return { why: 'bad section' };
  if (typeof name !== 'string' || !name.trim() || name.length > 40 || /[\u0000-\u001f<>]/.test(name)) {
    return { why: 'bad name' };
  }
  if (typeof mc !== 'string' || !NAME.test(mc)) return { why: 'bad mc' };
  if (color !== undefined && color !== '' && (typeof color !== 'string' || !/^#[0-9A-Fa-f]{6}$/.test(color))) {
    return { why: 'bad color' };
  }
  let doc;
  try {
    doc = JSON.parse(text);
  } catch (e) {
    return { why: 'bad credits' };
  }
  const target = (doc.sections || []).find((item) => typeof item.key === 'string'
    && item.key.endsWith('.' + section));
  if (!target || !Array.isArray(target.members)) return { why: 'bad credits' };
  if (target.members.some((member) => typeof member.mc === 'string'
      && member.mc.toLowerCase() === mc.toLowerCase())) {
    return { why: 'already listed' };
  }
  const member = { name: name.trim(), mc };
  if (color) member.color = color.toUpperCase();
  const teams = entry.teams === undefined ? [] : exactCodes(entry.teams, TR_LANGS);
  if (!teams) return { why: 'bad team' };
  const areas = entry.areas === undefined ? [] : exactCodes(entry.areas, TEAM_AREAS);
  if (!areas) return { why: 'bad area' };
  if (teams.length) member.teams = teams;
  if (areas.length) member.areas = areas;
  target.members.push(member);
  // 照倉庫的格式（兩格縮排、結尾換行）寫回去，diff 才只有新增的行
  return { text: JSON.stringify(doc, null, 2) + '\n' };
}

export async function handleAddCredit(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  if (!env.GITHUB_TOKEN) return no('github not configured', 503);
  let entry;
  try {
    entry = JSON.parse(await request.text());
  } catch (e) {
    return no('bad request');
  }
  const github = (path, init = {}) => fetch('https://api.github.com/repos/' + REPO + path, {
    ...init,
    headers: {
      authorization: 'Bearer ' + env.GITHUB_TOKEN,
      accept: 'application/vnd.github+json',
      'user-agent': 'wynnchayuan-site',
      'content-type': 'application/json',
    },
  });
  try {
    const file = await github('/contents/' + CREDITS_PATH + '?ref=main');
    if (file.status !== 200) return no('github ' + file.status, 502);
    const current = await file.json();
    const changed = addMember(base64ToUtf8(current.content), entry);
    if (changed.why) return no(changed.why, changed.why === 'already listed' ? 409 : 400);
    const head = await github('/git/ref/heads/main');
    if (head.status !== 200) return no('github ' + head.status, 502);
    const branch = 'credits/add-' + entry.mc.toLowerCase() + '-' + now;
    const made = await github('/git/refs', {
      method: 'POST',
      body: JSON.stringify({ ref: 'refs/heads/' + branch, sha: (await head.json()).object.sha }),
    });
    if (made.status !== 201) return no('github ' + made.status, 502);
    const title = '名單：' + SECTIONS[entry.section] + '加入 ' + entry.name.trim() + '（' + entry.mc + '）';
    const commit = await github('/contents/' + CREDITS_PATH, {
      method: 'PUT',
      body: JSON.stringify({ message: title, content: utf8ToBase64(changed.text), sha: current.sha, branch }),
    });
    if (commit.status !== 200 && commit.status !== 201) return no('github ' + commit.status, 502);
    const pull = await github('/pulls', {
      method: 'POST',
      body: JSON.stringify({
        title,
        head: branch,
        base: 'main',
        body: '從網站的管理頁送出，送出的人：' + by + '。\n\n'
          + '在 `credits.json` 的「' + SECTIONS[entry.section] + '」加一個成員。合併之後，名牌上的葉子與'
          + '披風的資格會在玩家下次同步名單時生效。',
      }),
    });
    if (pull.status !== 201) return no('github ' + pull.status, 502);
    return json({ ok: true, url: (await pull.json()).html_url });
  } catch (e) {
    return no('github unreachable', 502);
  }
}

const TEAMS_WHY_STATUS = { unchanged: 409, 'not found': 404 };

export function addMemberTeams(text, mc, langs) {
  if (typeof mc !== 'string' || !NAME.test(mc)) return { why: 'bad who' };
  const adding = knownCodes(langs, TR_LANGS);
  if (!adding.length) return { why: 'bad team' };
  let doc;
  try {
    doc = JSON.parse(text);
  } catch (e) {
    return { why: 'bad credits' };
  }
  const lower = mc.toLowerCase();
  let found = null;
  let changed = false;
  for (const section of Array.isArray(doc.sections) ? doc.sections : []) {
    if (!section || !/\.(developers|sponsors|contributors)$/.test(String(section.key)) || !Array.isArray(section.members)) continue;
    for (const member of section.members) {
      if (!member || typeof member.mc !== 'string' || member.mc.toLowerCase() !== lower) continue;
      found = found || member;
      const had = Array.isArray(member.teams) ? member.teams : [];
      const next = knownCodes([...had, ...adding], TR_LANGS);
      if (JSON.stringify(next) === JSON.stringify(had)) continue;
      member.teams = next;
      changed = true;
    }
  }
  if (!found) return { why: 'not found' };
  if (!changed) return { why: 'unchanged' };
  return { text: JSON.stringify(doc, null, 2) + '\n', name: String(found.name || mc) };
}

export async function commitTeams(env, by, now, change) {
  if (!env.GITHUB_TOKEN) return { why: 'github not configured', status: 503 };
  const github = githubClient(env);
  try {
    const day = new Date(now).toISOString().slice(0, 10).replace(/-/g, '');
    let branch = 'credits/teams-' + day;
    let url = '';
    const ref = await github('/git/ref/heads/' + branch);
    if (ref.status === 200) {
      const open = await github('/pulls?head=' + encodeURIComponent(REPO.split('/')[0] + ':' + branch) + '&state=open');
      const list = open.status === 200 ? await open.json() : [];
      if (Array.isArray(list) && list[0]) url = String(list[0].html_url || '');
      else branch += '-' + now;
    } else if (ref.status !== 404) {
      return { why: 'github ' + ref.status, status: 502 };
    }
    const reuse = Boolean(url);
    const file = await github('/contents/' + CREDITS_PATH + '?ref=' + encodeURIComponent(reuse ? branch : 'main'));
    if (file.status !== 200) return { why: 'github ' + file.status, status: 502 };
    const current = await file.json();
    const changed = change(base64ToUtf8(current.content));
    if (changed.why) return { why: changed.why, status: TEAMS_WHY_STATUS[changed.why] || 400 };
    if (!reuse) {
      const head = await github('/git/ref/heads/main');
      if (head.status !== 200) return { why: 'github ' + head.status, status: 502 };
      const made = await github('/git/refs', {
        method: 'POST', body: JSON.stringify({ ref: 'refs/heads/' + branch, sha: (await head.json()).object.sha }),
      });
      if (made.status !== 201) return { why: 'github ' + made.status, status: 502 };
    }
    const message = '名單：' + changed.name + ' 的翻譯團隊與貢獻項目';
    const put = await github('/contents/' + CREDITS_PATH, {
      method: 'PUT',
      body: JSON.stringify({ message, content: utf8ToBase64(changed.text), sha: current.sha, branch }),
    });
    if (put.status !== 200 && put.status !== 201) return { why: 'github ' + put.status, status: 502 };
    if (!reuse) {
      const pull = await github('/pulls', {
        method: 'POST',
        body: JSON.stringify({
          title: '名單：翻譯團隊與貢獻項目（' + day + '）',
          head: branch,
          base: 'main',
          body: '從網站的管理頁送出，第一筆由 ' + by + ' 送出。同一天在管理頁改的翻譯團隊與貢獻項目（包括指派譯者時一併加上的團隊）都會加進這個 PR。\n\n'
            + '改的是 `credits.json` 成員的 `teams` 與 `areas` 兩欄。合併之後，首頁的團隊名冊最多十分鐘內跟著更新。',
        }),
      });
      if (pull.status !== 201) return { why: 'github ' + pull.status, status: 502 };
      url = String((await pull.json()).html_url || '');
    }
    return { url, added: reuse };
  } catch (e) {
    return { why: 'github unreachable', status: 502 };
  }
}

export async function handleCreditTeams(request, env, now) {
  const by = await moderator(request, env, now);
  if (!by) return no('forbidden', 403);
  if (!env.GITHUB_TOKEN) return no('github not configured', 503);
  const body = await readJson(request);
  if (!body) return no('bad request');
  const input = teamsInput(body);
  if (input.why) return no(input.why);
  const done = await commitTeams(env, by, now, (text) => setMemberTeams(text, input));
  if (done.why) return no(done.why, done.status);
  return json({ ok: true, url: done.url, added: done.added });
}

export function creditTeamsOf(list, mc) {
  if (!list || typeof mc !== 'string' || !mc) return null;
  const lower = mc.toLowerCase();
  let hit = false;
  const teams = new Set();
  for (const section of list.team) {
    for (const member of section.members) {
      if (!member.mc || member.mc.toLowerCase() !== lower) continue;
      hit = true;
      for (const code of member.teams || []) teams.add(code);
    }
  }
  return hit ? TR_LANGS.filter((code) => teams.has(code)) : null;
}

export function mergeLangTeam(team, translators) {
  const people = [];
  const byMc = new Map();
  const union = (a, b, all) => all.filter((code) => a.includes(code) || (Array.isArray(b) && b.includes(code)));
  for (const section of Array.isArray(team) ? team : []) {
    for (const member of Array.isArray(section.members) ? section.members : []) {
      const mc = typeof member.mc === 'string' && member.mc ? member.mc.toLowerCase() : null;
      let person = mc ? byMc.get(mc) : null;
      if (!person) {
        person = { name: member.name, mc: member.mc || null, roles: [], listed: true, teams: [], areas: [], rights: null };
        people.push(person);
        if (mc) byMc.set(mc, person);
      }
      person.roles.push(section.role);
      person.teams = union(person.teams, member.teams, TR_LANGS);
      person.areas = union(person.areas, member.areas, TEAM_AREAS);
    }
  }
  for (const one of Array.isArray(translators) ? translators : []) {
    const mc = typeof one.name === 'string' && one.name ? one.name.toLowerCase() : null;
    let person = mc ? byMc.get(mc) : null;
    if (!person) {
      person = { name: one.name || one.discord || one.id, mc: one.name || null, roles: [], listed: false, teams: [], areas: [], rights: null };
      people.push(person);
      if (mc) byMc.set(mc, person);
    }
    const rights = person.rights || { keys: [], langs: [], note: '', discord: null, t: null, by: null };
    rights.keys.push(one.key);
    rights.langs = union(rights.langs, one.langs, TR_LANGS);
    if (!rights.note && one.note) rights.note = one.note;
    if (!rights.discord && one.discord) rights.discord = one.discord;
    if ((one.t || 0) > (rights.t || 0)) {
      rights.t = one.t;
      rights.by = one.by || null;
    }
    person.rights = rights;
  }
  for (const person of people) {
    person.mismatch = Boolean(person.listed && person.rights && person.rights.langs.join() !== person.teams.join());
  }
  return people;
}

export function setPersonTeams(text, mc, teams, areas) {
  if (typeof mc !== 'string' || !NAME.test(mc)) return { why: 'bad who' };
  let doc;
  try {
    doc = JSON.parse(text);
  } catch (e) {
    return { why: 'bad credits' };
  }
  const before = JSON.stringify(doc);
  const lower = mc.toLowerCase();
  let found = null;
  for (const section of Array.isArray(doc.sections) ? doc.sections : []) {
    if (!section || !/\.(developers|sponsors|contributors)$/.test(String(section.key)) || !Array.isArray(section.members)) continue;
    for (const member of section.members) {
      if (!member || typeof member.mc !== 'string' || member.mc.toLowerCase() !== lower) continue;
      found = found || member;
      if (Array.isArray(teams)) {
        if (teams.length) member.teams = teams;
        else delete member.teams;
      }
      if (Array.isArray(areas)) {
        if (areas.length) member.areas = areas;
        else delete member.areas;
      }
    }
  }
  if (!found) return { why: 'not found' };
  if (JSON.stringify(doc) === before) return { why: 'unchanged' };
  return { text: JSON.stringify(doc, null, 2) + '\n', name: String(found.name || mc) };
}
