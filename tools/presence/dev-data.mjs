import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { publicId } from './worker.js';

const here = path.dirname(fileURLToPath(import.meta.url));
export const ROOT = path.join(here, '..', '..');
const templates = path.join(ROOT, 'capes', 'templates');
const creditsFile = path.join(ROOT, 'src/main/resources/assets/wynnchayuan/credits.json');

export function memoryKv(data = new Map()) {
  return {
    data,
    async get(key) { return data.has(key) ? data.get(key).value : null; },
    async getWithMetadata(key) {
      return data.has(key) ? { ...data.get(key) } : { value: null, metadata: null };
    },
    async put(key, value, options = {}) { data.set(key, { value, metadata: options.metadata || null }); },
    async delete(key) { data.delete(key); },
    async list({ prefix }) {
      const keys = [...data.keys()].filter((k) => k.startsWith(prefix))
        .map((name) => ({ name, metadata: data.get(name).metadata }));
      return { keys, list_complete: true };
    },
  };
}

export function demoCredits(demoTeams) {
  const doc = JSON.parse(fs.readFileSync(creditsFile, 'utf8'));
  if (!demoTeams) return doc;
  const demo = [
    ['Steve', ['zh_tw'], ['translation', 'proofreading']], ['Alex', ['ja_jp', 'zh_tw'], ['translation']],
    ['Sakura_Demo', ['ja_jp'], ['translation', 'testing']], ['MinJun_Demo', ['ko_kr'], ['translation']],
    ['Ivan_Demo', ['ru_ru'], ['translation', 'proofreading']], ['Lucia_Demo', ['es_es'], ['proofreading']],
    ['Emre_Demo', ['tr_tr'], ['translation', 'capture']], ['Xiao_Demo', ['zh_cn'], ['translation', 'community']],
    ['Tester_Demo', [], ['capture', 'testing']], ['Plain_Demo', [], []]];
  for (const section of doc.sections) {
    if (section.key.endsWith('.developers')) {
      section.members.forEach((member) => Object.assign(member, { teams: ['zh_tw'], areas: ['code', 'website', 'translation'] }));
    }
    if (section.key.endsWith('.contributors')) {
      section.members = demo.map(([mc, teams, areas]) => {
        const member = { name: mc, mc };
        if (teams.length) member.teams = teams;
        if (areas.length) member.areas = areas;
        return member;
      });
    }
  }
  return doc;
}

export const DEVELOPER = JSON.parse(fs.readFileSync(creditsFile, 'utf8'))
  .sections.find((s) => s.key.endsWith('.developers')).members[0].mc;

export async function seedDemo(data, { guildIcon, guildBanner, demoTeams } = {}) {
  const developer = DEVELOPER;
  const people = [developer, 'Steve', 'Alex', 'Notch'];

  // 示範資料：每個檔當成一到三個人上傳的
  let n = 0;
  for (const file of fs.readdirSync(templates).filter((f) => /^(example|template)-.*\.(gif|png)$/.test(f)).sort()) {
    const bytes = new Uint8Array(fs.readFileSync(path.join(templates, file)));
    const hash = crypto.createHash('sha256').update(bytes).digest('hex');
    const type = file.endsWith('.gif') ? 'gif' : 'png';
    data.set('blob:' + hash, { value: bytes, metadata: { t: type, s: bytes.length } });
    for (let i = 0; i <= n % 3; i++) {
      const id = crypto.createHash('sha256').update(file + i).digest('hex').slice(0, 24);
      data.set('cape:' + id, {
        value: '',
        metadata: { t: type, v: Date.now() - n * 86400000, s: bytes.length, u: String(i).repeat(32),
          n: people[(n + i) % people.length], h: hash },
      });
    }
    n++;
  }
  // 披風一覽的幾種狀態：幾件有人加了最愛、一件不公開、一件在等審核，審核名單上一個人
  {
    const seeded = [...data.keys()].filter((k) => k.startsWith('cape:')).map((k) => data.get(k).metadata);
    const stars = {};
    seeded.forEach((cape, i) => {
      cape.c = cape.v - (i % 5) * 3600000;
      if (i % 3 === 0) stars[cape.h] = 1 + (i % 4);
    });
    const alone = seeded.filter((cape) => seeded.filter((other) => other.h === cape.h).length === 1);
    if (alone[0]) alone[0].q = 1;
    if (alone[1]) alone[1].r = 1;
    const pairs = seeded.filter((cape) => seeded.filter((other) => other.h === cape.h).length === 2);
    if (pairs[0]) pairs.filter((cape) => cape.h === pairs[0].h).forEach((cape) => { cape.vis = 'private'; });
    const steve = alone.pop();
    if (steve && alone.length >= 3) {
      steve.vis = 'unlisted';
      steve.u = 'b'.repeat(32);
      steve.n = 'Steve';
      const was = [...data.keys()].find((k) => k.startsWith('cape:') && data.get(k).metadata === steve);
      data.set('cape:' + await publicId('b'.repeat(32)), data.get(was));
      data.delete(was);
    }
    data.set('stars', { value: JSON.stringify(stars), metadata: null });
    // 沒有人穿的披風留七天：兩件已經沒有人穿，一件還剩四天、一件是最後一天
    const idle = {};
    [[alone[2], 3], [alone[3], 6.5]].forEach(([cape, days]) => {
      if (!cape) return;
      const key = [...data.keys()].find((k) => k.startsWith('cape:') && data.get(k).metadata === cape);
      data.delete(key);
      idle[cape.h] = { t: cape.t, s: cape.s, c: cape.c, at: Date.now() - days * 86400000 };
    });
    data.set('idle', { value: JSON.stringify(idle), metadata: null });
    data.set('review:' + '0'.repeat(32), {
      value: JSON.stringify({ n: people[1], note: '示範', t: Date.now() - 5400000, by: developer, ok: [] }), metadata: null });
  }
  data.set('ban:' + '9'.repeat(32), {
    value: JSON.stringify({ n: 'Herobrine', t: Date.now() - 3 * 86400000, by: developer }), metadata: null });

  // 一位管理員（照 UUID 加的）與一位還沒登入過的（照名字加的）
  data.set('staff:u:' + 'c'.repeat(32), {
    value: JSON.stringify({ n: '幫忙看披風', t: Date.now() - 86400000, by: developer, m: 'Alex' }), metadata: null });
  data.set('staff:n:notch', { value: JSON.stringify({ n: '', t: Date.now() - 3600000, by: developer }), metadata: null });

  // 一位譯者（Alex：日文與繁中）、一個提案、一個術語投票
  data.set('tl:u:' + 'c'.repeat(32), { value: JSON.stringify({ langs: ['ja_jp', 'zh_tw'], n: '日文組', t: Date.now() - 7200000, by: developer, m: 'Alex' }), metadata: null });
  if (demoTeams) {
    data.set('tl:n:sakura_demo', { value: JSON.stringify({ langs: ['zh_tw', 'ja_jp'], n: '', t: Date.now() - 3600000, by: developer, m: 'Sakura_Demo' }), metadata: null });
    data.set('tl:n:outsider_demo', { value: JSON.stringify({ langs: ['ko_kr'], n: '韓文組', t: Date.now() - 1800000, by: developer, m: 'Outsider_Demo' }), metadata: null });
    data.set('tl:d:555555555555555555', { value: JSON.stringify({ langs: ['es_es'], n: '', t: Date.now() - 900000, by: developer, dn: 'lucia_dc' }), metadata: null });
  }
  {
    const prop = { lang: 'zh_tw', file: 'raid.json', key: null, src: 'Choose a buff or go to the exit', base: '選一個增益，或前往出口',
      items: [
        { id: 'a1b2c3d4e5f6', text: '選一個增益，或走向出口', by: 'u:' + 'c'.repeat(32), name: 'Alex', t: Date.now() - 3600000, votes: ['u:' + 'c'.repeat(32)] },
        { id: 'b1b2c3d4e5f6', text: '挑一個增益，或前往出口', by: 'u:' + 'a'.repeat(32), name: developer, t: Date.now() - 1800000, votes: [] }],
      status: 'open', t: Date.now() - 3600000, updated: Date.now() - 1800000 };
    data.set('tp:zh_tw:0123456789abcdef', { value: JSON.stringify(prop), metadata: { f: prop.file, s: prop.src, n: 2, v: 1, st: 'open', t: prop.updated } });
    const poll = { lang: 'zh_tw', term: 'Weaken Enemy', note: 'Lootrun 加持與討伐戰增益裡的屬性', by: 'u:' + 'c'.repeat(32), name: 'Alex',
      t: Date.now() - 5400000, status: 'open', options: [
        { id: 'c1c2c3c4c5c6', text: '削弱敵傷', by: 'u:' + 'a'.repeat(32), name: developer, t: Date.now() - 5400000, votes: ['u:' + 'a'.repeat(32)] },
        { id: 'd1d2d3d4d5d6', text: '削弱敵人', by: 'u:' + 'c'.repeat(32), name: 'Alex', t: Date.now() - 5000000, votes: ['u:' + 'c'.repeat(32)] }] };
    data.set('tq:zh_tw:fedcba987654', { value: JSON.stringify(poll), metadata: { s: poll.term, n: 2, v: 2, st: 'open', t: poll.t } });
  }

  // 已註冊的人（管理頁「新增管理員」搜尋得到）：一位綁了遊戲帳號，一位只有 Discord（改版前存的，沒有 metadata）
  data.set('d:222222222222222222', {
    value: JSON.stringify({ n: 'steve_dc', m: 'Steve', u: 'd'.repeat(32), t: Date.now() - 86400000 }),
    metadata: { n: 'steve_dc', m: 'Steve', u: 'd'.repeat(32) } });
  data.set('l:' + 'd'.repeat(32), { value: '222222222222222222', metadata: null });
  data.set('d:333333333333333333', { value: JSON.stringify({ n: 'Sayla' }), metadata: null });

  // 一個貢獻公會。圖示與旗幟不在倉庫裡（是公會自己的圖）：有給檔案就用，沒給就只有名字
  const guild = { slug: 'yuchayuan', name: 'YuChaYuan', tag: 'YCY', t: Date.now() - 7200000, by: developer, i: 0, b: 0 };
  [['icon', 'i', guildIcon], ['banner', 'b', guildBanner]].forEach(([kind, field, file]) => {
    if (!file || !fs.existsSync(file)) return;
    data.set('gimg:yuchayuan:' + kind, { value: new Uint8Array(fs.readFileSync(file)), metadata: { t: 'image/png' } });
    guild[field] = Date.now();
  });
  data.set('guilds', { value: JSON.stringify([guild]), metadata: null });
  return { developer, people };
}
