import { authority } from './admin.js';
import { DISCORD_ID } from './auth.js';
import { FIGURE_STYLE, allCapes, handleCapePreview, listed } from './capes.js';
import { FEEDBACK_MAX, FEEDBACK_MIN, FEEDBACK_PER_HOUR, FEEDBACK_WHERE_MAX, submitFeedback } from './feedback.js';
import { NAME, UUID, botFmt, clip, json, no } from './http.js';
import { storedList } from './presence.js';
import { TR_LANGS, handleTrIndex, trList } from './translate.js';

const DISCORD_V10 = 'https://discord.com/api/v10';

const SITE = 'https://wynnchayuan.com';

const BOT_TEXT = {
  zh: {
    status: '網站狀態', users: '裝了模組的玩家', capes: '穿著披風', pending: '待審核的披風',
    props: '協作翻譯：投票中的提案', polls: '協作翻譯：投票中的術語', none: '沒有',
    capeOf: '{name} 的披風', noCape: '找不到 {name} 的披風（沒上傳過，或現在沒穿）',
    privateCape: '{name} 的披風設成不公開。', pendingCape: '{name} 的披風還在審核中。', badName: '這不像 Minecraft 的名字。',
    kind: '格式', since: '換上的時間', gallery: '披風一覽',
    found: '「{q}」找到 {n} 條，列出前 {shown} 條', notFound: '「{q}」在 {lang} 的語料裡找不到。', tooShort: '關鍵字至少兩個字。',
    untranslated: '（還沒翻）', indexFail: '語料讀不到，晚點再試。', more: '在網站上看更多', unknown: '不認得這個指令。',
    fbThanks: '收到了，謝謝你的回饋！團隊會在網站的管理頁看到它。', fbIssue: '譯文建議已經在 GitHub 開了 Issue：{url}',
    fbIssueFail: '（這次沒能在 GitHub 開 Issue，回饋已經存好，管理員會再補開。）',
    fbWhy: { 'bad category': '分類不對。', 'too short': '內容太短了，多寫幾個字吧。', 'too long': '內容太長了（最多 2000 字）。',
      'where too long': '「位置」太長了（最多 200 字）。', 'slow down': '一小時內最多送 {n} 則，請 {t}再試。', 'no user': '認不出你是誰，請再試一次。' },
  },
  cn: {
    status: '网站状态', users: '装了模组的玩家', capes: '穿着披风', pending: '待审核的披风',
    props: '协作翻译：投票中的提案', polls: '协作翻译：投票中的术语', none: '没有',
    capeOf: '{name} 的披风', noCape: '找不到 {name} 的披风（没上传过，或现在没穿）',
    privateCape: '{name} 的披风设为不公开。', pendingCape: '{name} 的披风还在审核中。', badName: '这不像 Minecraft 的名字。',
    kind: '格式', since: '换上的时间', gallery: '披风一览',
    found: '「{q}」找到 {n} 条，列出前 {shown} 条', notFound: '「{q}」在 {lang} 的语料里找不到。', tooShort: '关键字至少两个字。',
    untranslated: '（还没翻）', indexFail: '语料读不到，稍后再试。', more: '在网站上看更多', unknown: '不认识这个指令。',
    fbThanks: '收到了，谢谢你的反馈！团队会在网站的管理页看到它。', fbIssue: '译文建议已经在 GitHub 开了 Issue：{url}',
    fbIssueFail: '（这次没能在 GitHub 开 Issue，反馈已经保存，管理员会再补开。）',
    fbWhy: { 'bad category': '分类不对。', 'too short': '内容太短了，多写几个字吧。', 'too long': '内容太长了（最多 2000 字）。',
      'where too long': '“位置”太长了（最多 200 字）。', 'slow down': '一小时内最多发送 {n} 条，请 {t}再试。', 'no user': '认不出你是谁，请再试一次。' },
  },
  en: {
    status: 'Website status', users: 'Players with the mod', capes: 'Wearing a cape', pending: 'Capes awaiting review',
    props: 'Translate: suggestions open for voting', polls: 'Translate: terms open for voting', none: 'none',
    capeOf: "{name}'s cape", noCape: 'No cape found for {name} (never uploaded, or not wearing one now).',
    privateCape: "{name}'s cape is not public.", pendingCape: "{name}'s cape is still awaiting review.",
    badName: 'That does not look like a Minecraft name.',
    kind: 'Format', since: 'Worn since', gallery: 'Cape gallery',
    found: '{n} lines match "{q}", showing the first {shown}', notFound: 'Nothing matches "{q}" in {lang}.',
    tooShort: 'Use at least two characters.', untranslated: '(not translated yet)', indexFail: 'Could not load the translations. Try again later.',
    more: 'See more on the website', unknown: 'Unknown command.',
    fbThanks: 'Got it, thanks for the feedback! The team will see it on the website.', fbIssue: 'Your translation suggestion is now a GitHub issue: {url}',
    fbIssueFail: '(Could not open the GitHub issue this time. Your feedback is saved and an admin will open it later.)',
    fbWhy: { 'bad category': 'Unknown category.', 'too short': 'That is too short. Please add a few more words.',
      'too long': 'That is too long (2000 characters at most).', 'where too long': '"Where" is too long (200 characters at most).',
      'slow down': 'You can send {n} per hour. Try again {t}.', 'no user': 'Could not tell who you are. Please try again.' },
  },
};

export const LANG_LABEL = { zh_tw: '繁體中文', zh_cn: '简体中文', ja_jp: '日本語', ko_kr: '한국어', ru_ru: 'Русский', es_es: 'Español', tr_tr: 'Türkçe' };

function botLocale(locale) {
  const l = String(locale || '');
  if (l === 'zh-CN') return { text: BOT_TEXT.cn, corpus: 'zh_cn' };
  if (l.startsWith('zh')) return { text: BOT_TEXT.zh, corpus: 'zh_tw' };
  const corpus = { ja: 'ja_jp', ko: 'ko_kr', ru: 'ru_ru', 'es-ES': 'es_es', 'es-419': 'es_es', tr: 'tr_tr' }[l] || 'zh_tw';
  return { text: BOT_TEXT.en, corpus };
}

const FEEDBACK_CHOICES = [
  ['translation', 'Translation suggestion', '譯文建議', '译文建议'],
  ['content', 'Content suggestion', '內容建議', '内容建议'],
  ['website', 'Website suggestion', '網站建議', '网站建议'],
  ['feature', 'Feature suggestion', '功能建議', '功能建议'],
  ['other', 'Other', '其他', '其他'],
];

export const DISCORD_COMMANDS = [
  {
    name: 'status', type: 1,
    description: 'WynnChaYuan website status',
    name_localizations: { 'zh-TW': '狀態', 'zh-CN': '状态' },
    description_localizations: { 'zh-TW': '網站狀態：玩家、披風、待審核、協作翻譯', 'zh-CN': '网站状态：玩家、披风、待审核、协作翻译' },
  },
  {
    name: 'cape', type: 1,
    description: "Show a player's WynnChaYuan cape",
    name_localizations: { 'zh-TW': '披風', 'zh-CN': '披风' },
    description_localizations: { 'zh-TW': '看某位玩家現在的 WynnChaYuan 披風', 'zh-CN': '看某位玩家现在的 WynnChaYuan 披风' },
    options: [{
      type: 3, name: 'player', required: true, max_length: 16,
      description: 'Minecraft name',
      name_localizations: { 'zh-TW': '玩家', 'zh-CN': '玩家' },
      description_localizations: { 'zh-TW': 'Minecraft 名字', 'zh-CN': 'Minecraft 名字' },
    }],
  },
  {
    name: 'translate', type: 1,
    description: 'Search the WynnChaYuan translations',
    name_localizations: { 'zh-TW': '翻譯', 'zh-CN': '翻译' },
    description_localizations: { 'zh-TW': '搜尋 WynnChaYuan 的語料（原文或譯文）', 'zh-CN': '搜索 WynnChaYuan 的语料（原文或译文）' },
    options: [
      {
        type: 3, name: 'text', required: true, min_length: 2, max_length: 100,
        description: 'Words in the English source or the translation',
        name_localizations: { 'zh-TW': '關鍵字', 'zh-CN': '关键字' },
        description_localizations: { 'zh-TW': '英文原文或譯文裡的字', 'zh-CN': '英文原文或译文里的字' },
      },
      {
        type: 3, name: 'language', required: false,
        description: 'Which translation (default: follows your Discord language)',
        name_localizations: { 'zh-TW': '語言', 'zh-CN': '语言' },
        description_localizations: { 'zh-TW': '哪一種語言的譯文（預設跟著 Discord 的語言）', 'zh-CN': '哪一种语言的译文（默认跟着 Discord 的语言）' },
        choices: Object.entries(LANG_LABEL).map(([value, name]) => ({ name, value })),
      },
    ],
  },
  {
    name: 'feedback', type: 1,
    description: 'Send feedback or a suggestion to the WynnChaYuan team',
    name_localizations: { 'zh-TW': '回饋', 'zh-CN': '反馈' },
    description_localizations: { 'zh-TW': '給 WynnChaYuan 團隊的回饋或建議', 'zh-CN': '给 WynnChaYuan 团队的反馈或建议' },
    options: [
      {
        type: 3, name: 'category', required: true,
        description: 'What kind of feedback',
        name_localizations: { 'zh-TW': '分類', 'zh-CN': '分类' },
        description_localizations: { 'zh-TW': '哪一種回饋', 'zh-CN': '哪一种反馈' },
        choices: FEEDBACK_CHOICES.map(([value, name, tw, cn]) => ({ name, value, name_localizations: { 'zh-TW': tw, 'zh-CN': cn } })),
      },
      {
        type: 3, name: 'message', required: true, min_length: FEEDBACK_MIN, max_length: FEEDBACK_MAX,
        description: 'Your feedback',
        name_localizations: { 'zh-TW': '內容', 'zh-CN': '内容' },
        description_localizations: { 'zh-TW': '想說的話', 'zh-CN': '想说的话' },
      },
      {
        type: 3, name: 'where', required: false, max_length: FEEDBACK_WHERE_MAX,
        description: 'Where: which text, screen or quest (helps a lot for translation suggestions)',
        name_localizations: { 'zh-TW': '位置', 'zh-CN': '位置' },
        description_localizations: { 'zh-TW': '在哪裡：哪一句、哪個畫面或任務（譯文建議特別需要）', 'zh-CN': '在哪里：哪一句、哪个画面或任务（译文建议特别需要）' },
      },
      {
        type: 3, name: 'language', required: false,
        description: 'Which translation it is about',
        name_localizations: { 'zh-TW': '語言', 'zh-CN': '语言' },
        description_localizations: { 'zh-TW': '跟哪一種語言的譯文有關', 'zh-CN': '跟哪一种语言的译文有关' },
        choices: Object.entries(LANG_LABEL).map(([value, name]) => ({ name, value })),
      },
    ],
  },
];

function hexBytes(hex) {
  const out = new Uint8Array(hex.length / 2);
  for (let i = 0; i < out.length; i++) out[i] = parseInt(hex.substr(i * 2, 2), 16);
  return out;
}

export async function verifyDiscord(publicKey, signature, timestamp, body) {
  if (!/^[0-9a-f]{64}$/i.test(publicKey || '') || !/^[0-9a-f]{128}$/i.test(signature || '') || !timestamp) return false;
  try {
    const key = await crypto.subtle.importKey('raw', hexBytes(publicKey), { name: 'Ed25519' }, false, ['verify']);
    return await crypto.subtle.verify('Ed25519', key, hexBytes(signature), new TextEncoder().encode(timestamp + body));
  } catch (e) {
    return false;
  }
}

const parsedIndex = new Map();

async function corpusIndex(env, lang, ctx) {
  const hit = parsedIndex.get(lang);
  if (hit && Date.now() - hit.t < 600000) return hit.data;
  const res = await handleTrIndex(null, env, lang, ctx);
  if (res.status !== 200) return null;
  const data = await res.json();
  parsedIndex.set(lang, { t: Date.now(), data });
  return data;
}

async function botStatus(env, text) {
  const list = (await storedList(env)) || { users: [], capes: {} };
  const capes = await allCapes(env);
  const pending = capes.filter((c) => c.r).length;
  const openBy = async (prefix) => {
    const parts = [];
    for (const lang of TR_LANGS) {
      const n = (await trList(env, prefix + lang + ':')).filter((p) => p.st === 'open').length;
      if (n) parts.push(LANG_LABEL[lang] + ' ' + n);
    }
    return parts.length ? parts.join('、') : text.none;
  };
  return {
    embeds: [{
      title: text.status, url: SITE, color: 0x7fd08a,
      fields: [
        { name: text.users, value: String(list.users.length), inline: true },
        { name: text.capes, value: String(Object.keys(list.capes).length), inline: true },
        { name: text.pending, value: String(pending), inline: true },
        { name: text.props, value: await openBy('tp:') },
        { name: text.polls, value: await openBy('tq:') },
      ],
    }],
  };
}

async function botCape(env, text, name) {
  if (!NAME.test(name || '')) return { content: text.badName, flags: 64 };
  const capes = await allCapes(env);
  const mine = capes.find((c) => String(c.n || '').toLowerCase() === name.toLowerCase());
  if (!mine) return { content: botFmt(text.noCape, { name }) };
  if (mine.r) return { content: botFmt(text.pendingCape, { name: mine.n }) };
  if (!listed(mine)) return { content: botFmt(text.privateCape, { name: mine.n }) };
  return {
    embeds: [{
      title: botFmt(text.capeOf, { name: mine.n }), url: SITE + '/capes', color: 0x7fd08a,
      image: { url: SITE + '/api/cape-preview/' + mine.h + (UUID.test(mine.u || '') ? '?u=' + mine.u.replace(/-/g, '') + '&v=' + FIGURE_STYLE : '') },
      thumbnail: { url: 'https://minotar.net/helm/' + encodeURIComponent(mine.n) + '/64.png' },
      fields: [
        { name: text.kind, value: mine.t === 'gif' ? 'GIF' : 'PNG', inline: true },
        { name: text.since, value: '<t:' + Math.floor((mine.v || mine.c || 0) / 1000) + ':R>', inline: true },
      ],
      footer: { text: text.gallery + ' · wynnchayuan.com/capes' },
    }],
  };
}

async function botTranslate(env, text, lang, query, ctx) {
  const q = String(query || '').trim().toLowerCase();
  if (q.length < 2) return { content: text.tooShort, flags: 64 };
  const data = await corpusIndex(env, lang, ctx);
  if (!data || !Array.isArray(data.rows)) return { content: text.indexFail };
  const hits = [];
  let total = 0;
  for (const row of data.rows) {
    if (!String(row[2]).toLowerCase().includes(q) && !String(row[3]).toLowerCase().includes(q)) continue;
    total++;
    if (hits.length < 5) hits.push(row);
  }
  if (!total) return { content: botFmt(text.notFound, { q: query, lang: LANG_LABEL[lang] }) };
  return {
    embeds: [{
      title: botFmt(text.found, { q: clip(query, 40), n: total, shown: hits.length }),
      url: SITE + '/translate', color: 0x7fd08a,
      fields: hits.map((row) => ({
        name: clip(String(row[2]).replace(/\n/g, ' / '), 250),
        value: clip((row[3] ? String(row[3]).replace(/\n/g, ' / ') : text.untranslated), 900)
          + '\n`' + clip(data.files[row[0]], 60) + '`',
      })),
      footer: { text: LANG_LABEL[lang] + ' · ' + text.more },
    }],
  };
}

async function botAnswer(env, interaction, ctx) {
  const { text, corpus } = botLocale(interaction.locale);
  const command = interaction.data || {};
  const option = (name) => ((command.options || []).find((o) => o.name === name) || {}).value;
  if (command.name === 'status') return botStatus(env, text);
  if (command.name === 'cape') {
    const answer = await botCape(env, text, option('player'));
    // 先算好預覽圖：Discord 拿到訊息才來抓圖，大張的 GIF 現算來不及
    const image = answer.embeds && answer.embeds[0].image;
    if (image) {
      const at = new URL(image.url);
      await handleCapePreview(env, at.pathname.split('/').pop(), at.searchParams.get('u'));
    }
    return answer;
  }
  if (command.name === 'translate') {
    const lang = TR_LANGS.includes(option('language')) ? option('language') : corpus;
    return botTranslate(env, text, lang, option('text'), ctx);
  }
  if (command.name === 'feedback') return botFeedback(env, text, interaction, option, Date.now());
  return { content: text.unknown, flags: 64 };
}

const LOCALE_CORPUS = { 'zh-TW': 'zh_tw', 'zh-CN': 'zh_cn', ja: 'ja_jp', ko: 'ko_kr', ru: 'ru_ru', 'es-ES': 'es_es', 'es-419': 'es_es', tr: 'tr_tr' };

async function botFeedback(env, text, interaction, option, now) {
  // 伺服器裡是 member.user，私訊裡是 user
  const user = (interaction.member && interaction.member.user) || interaction.user || {};
  if (!DISCORD_ID.test(String(user.id || ''))) return { content: text.fbWhy['no user'], flags: 64 };
  const asked = option('language');
  const got = await submitFeedback(env, { id: user.id, name: String(user.global_name || user.username || user.id) }, {
    cat: option('category'), msg: option('message'), where: option('where') || '',
    lang: TR_LANGS.includes(asked) ? asked : LOCALE_CORPUS[interaction.locale] || null,
  }, 'bot', now);
  if (got.why) {
    const why = text.fbWhy[got.why] || text.unknown;
    return { content: botFmt(why, { n: FEEDBACK_PER_HOUR, t: got.until ? '<t:' + Math.ceil(got.until / 1000) + ':R>' : '' }), flags: 64 };
  }
  const { record } = got;
  const lines = [text.fbThanks];
  if (record.issue && record.issue.url) lines.push(botFmt(text.fbIssue, { url: record.issue.url }));
  else if (record.issueError) lines.push(text.fbIssueFail);
  return { content: lines.join('\n'), flags: 64 };
}

export async function handleDiscordInteraction(request, env, ctx) {
  const body = await request.text();
  const ok = await verifyDiscord(env.DISCORD_PUBLIC_KEY, request.headers.get('x-signature-ed25519'),
    request.headers.get('x-signature-timestamp'), body);
  if (!ok) return new Response('bad signature', { status: 401 });
  let interaction;
  try {
    interaction = JSON.parse(body);
  } catch (e) {
    return no('bad request');
  }
  if (interaction.type === 1) return json({ type: 1 });
  if (interaction.type !== 2) return json({ type: 4, data: { content: '…', flags: 64 } });
  // Discord 只等三秒：慢的指令先回「處理中」再補上
  const asked = interaction.data || {};
  const slowFeedback = asked.name === 'feedback'
    && (asked.options || []).some((o) => o.name === 'category' && o.value === 'translation');
  if ((['translate', 'cape'].includes(asked.name) || slowFeedback) && ctx && ctx.waitUntil) {
    ctx.waitUntil((async () => {
      let data;
      try {
        data = await botAnswer(env, interaction, ctx);
      } catch (e) {
        data = { content: botLocale(interaction.locale).text.indexFail };
      }
      await fetch(DISCORD_V10 + '/webhooks/' + interaction.application_id + '/' + interaction.token + '/messages/@original', {
        method: 'PATCH', headers: { 'content-type': 'application/json' }, body: JSON.stringify(data),
      });
    })());
    return json(asked.name === 'feedback' ? { type: 5, data: { flags: 64 } } : { type: 5 });
  }
  return json({ type: 4, data: await botAnswer(env, interaction, ctx) });
}

export async function handleDiscordInfo(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  const id = env.DISCORD_CLIENT_ID || '';
  let check = null;
  if (env.DISCORD_BOT_TOKEN) {
    const ask = async (path) => {
      try {
        const res = await fetch(DISCORD_V10 + path, { headers: { authorization: 'Bot ' + env.DISCORD_BOT_TOKEN } });
        return res.status === 200 ? await res.json() : { error: res.status };
      } catch (e) {
        return { error: 'unreachable' };
      }
    };
    const app = await ask('/applications/@me');
    check = { error: app.error || null };
    if (!app.error) {
      check.app = app.name;
      check.appId = app.id;
      check.bot = app.bot ? app.bot.username : null;
      check.sameClient = app.id === id;
      check.sameKey = String(app.verify_key || '').toLowerCase() === String(env.DISCORD_PUBLIC_KEY || '').toLowerCase();
      check.endpointSet = app.interactions_endpoint_url || null;
      const commands = await ask('/applications/' + app.id + '/commands');
      check.commands = Array.isArray(commands) ? commands.map((c) => c.name) : null;
      const guilds = await ask('/users/@me/guilds');
      check.guilds = Array.isArray(guilds) ? guilds.map((g) => g.name) : null;
    }
  }
  return json({
    publicKey: Boolean(env.DISCORD_PUBLIC_KEY), token: Boolean(env.DISCORD_BOT_TOKEN), clientId: Boolean(id),
    invite: id ? 'https://discord.com/oauth2/authorize?client_id=' + encodeURIComponent(id) + '&scope=applications.commands+bot&permissions=0' : '',
    endpoint: SITE + '/api/discord/interactions',
    check,
  }, 200, { 'cache-control': 'no-store' });
}

export async function handleDiscordRegister(request, env, now) {
  const mine = await authority(request, env, now);
  if (!mine || !mine.developer) return no('forbidden', 403);
  if (!env.DISCORD_BOT_TOKEN || !env.DISCORD_CLIENT_ID) return no('discord bot not configured', 503);
  try {
    const res = await fetch(DISCORD_V10 + '/applications/' + env.DISCORD_CLIENT_ID + '/commands', {
      method: 'PUT',
      headers: { authorization: 'Bot ' + env.DISCORD_BOT_TOKEN, 'content-type': 'application/json' },
      body: JSON.stringify(DISCORD_COMMANDS),
    });
    if (res.status !== 200) return no('discord ' + res.status, 502);
    const list = await res.json();
    return json({ ok: true, commands: Array.isArray(list) ? list.map((c) => c.name) : [] });
  } catch (e) {
    return no('discord unreachable', 502);
  }
}
