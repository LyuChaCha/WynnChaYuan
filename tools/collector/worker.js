/**
 * 語料收集站。
 *
 * 玩家那邊打開「分享給翻譯團隊」之後，模組會把「沒翻到的英文原文」POST 到
 * /submit；每晚的 GitHub Action 用 /drain 把累積的撈走，跑 import-captured.py
 * 分類進語料，再開一個 PR 等人看過再合併。
 *
 * 這支刻意做得很笨：一個 KV、兩個路由、沒有資料庫、沒有帳號。
 * 它存的東西全部是「遊戲裡的英文字串」，沒有任何一欄跟玩家有關——
 * 沒有帳號、沒有 UUID、沒有 IP（限流用的是雜湊過的 IP，而且只留一小時）。
 *
 * 部署方式見同一個資料夾的 README.md。
 */

/** 一次最多收幾條。模組那邊也有同樣的上限，兩邊都擋。 */
const MAX_ITEMS = 100;

/** 單條字串的長度上限。正常的一句台詞不會超過這個。 */
const MAX_LEN = 600;

/** 整包 body 的上限，避免有人拿它當免費儲存空間。 */
const MAX_BODY = 128 * 1024;

/** 同一個來源一小時最多送幾次。 */
const RATE_PER_HOUR = 60;

/** 佇列裡最多堆幾條；滿了就不再收，等 Action 撈走。 */
const MAX_QUEUE = 20000;

/** 允許的來源分類。跟模組端 CorpusUpload#shareable 是同一份名單。 */
const ALLOWED = ['dialogue/', 'gui/', 'tooltip/', 'npc/', 'label/'];

/**
 * 明顯夾帶個資的形狀。
 *
 * 這裡只是粗篩——真正完整的那一份在 tools/import-captured.py 的 NAMED，
 * 進倉庫前會再跑一次。放在這裡的理由是「不該存的東西連存都不要存」。
 */
const LOOKS_PERSONAL = [
  /\[[A-Za-z0-9]{2,4}\]/,          // 公會標籤
  /\bControlled by\b/,
  /\bCrafted by\b/,
  /Guild:\s/,
  /'s (party|guild|island|house|status)\b/i,
  /[A-Za-z0-9]*_[A-Za-z0-9_]{2,}/, // 帳號名裡的底線
  /\shas given you\s/,
  /would like to trade/,

  // ---- 聊天室折行之後留下的半截句子 ----
  //
  // chat/INFO 是允許的來源，而玩家名字最常就是從這裡漏出去的。下面幾條
  // 都是實際在 captured.json 裡撿到的，句型是遊戲寫死的、主詞永遠是別人：
  //
  //   {#} {#}JC grindeando has thrown a
  //   {#} JC grindeando Loot Bomb has expired! …
  //   {#} {#}Thank JC grindeando
  //   {~}jimmy's Totem of Tales
  //   {#}{#}ChangJenChief has died
  //
  // 比對的是<b>句型</b>不是名字：名字沒有形狀可以認（被折行截斷、
  // 被數字佔位符吃掉半截、或伺服器根本沒把那個人放進分頁清單）。
  /\shas thrown a/,
  /Loot Bomb has expired/,
  /['\u2019]s Totem of Tales/,
  // 行首一個詞加句型才算，不然劇情裡的「The king has died」也會被擋掉。
  /^(?:\{#\}\s*)*\S+ has died/m,
  // 前面至少要有一個符號佔位符，任務對話裡的「Thank ...」不帶那個。
  /^(?:\{#\}\s*)+Thank /m,

  // ---- 其餘已知會夾帶別人 ID 的廣播 ----
  // 這幾類在過去的語料裡都真的漏進來過，見 PlayerDataFilter 的 MARKERS。
  /\sshouts:/,
  /has logged into server/,
  /\shas chosen the\s/,
  /\shas placed a (mob|gathering) totem/,
  /from their crate!/,
  /Party Finder: Hey\s/,
  /\sis now (online|offline)/,
];

async function sha256(text) {
  const data = new TextEncoder().encode(text);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return [...new Uint8Array(digest)]
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8' },
  });
}

/** 這一條可以收嗎。收不了就整條丟掉，不回報是哪一條——沒有必要。 */
function acceptable(item) {
  if (!item || typeof item.src !== 'string') return false;
  const src = item.src.trim();
  if (!src || src.length > MAX_LEN) return false;
  if (!/[A-Za-z]/.test(src)) return false;          // 沒有字母 = 純符號
  const ctx = typeof item.ctx === 'string' ? item.ctx : '';
  const ok = ALLOWED.some((p) => ctx.startsWith(p)) || ctx === 'chat/INFO';
  if (!ok) return false;
  return !LOOKS_PERSONAL.some((re) => re.test(src));
}

async function rateLimited(env, request) {
  const ip = request.headers.get('cf-connecting-ip') || 'unknown';
  // IP 本身不存：存的是雜湊，而且一小時就過期。
  const key = 'r:' + (await sha256(ip)).slice(0, 16);
  const now = Number((await env.CORPUS.get(key)) || 0);
  if (now >= RATE_PER_HOUR) return true;
  await env.CORPUS.put(key, String(now + 1), { expirationTtl: 3600 });
  return false;
}

async function submit(request, env) {
  const length = Number(request.headers.get('content-length') || 0);
  if (length > MAX_BODY) return json({ error: 'too large' }, 413);
  if (await rateLimited(env, request)) return json({ error: 'slow down' }, 429);

  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: 'bad json' }, 400);
  }
  const items = Array.isArray(body.items) ? body.items.slice(0, MAX_ITEMS) : [];
  if (!items.length) return json({ accepted: 0, skipped: 0 });

  const queued = Number((await env.CORPUS.get('meta:count')) || 0);
  if (queued >= MAX_QUEUE) return json({ error: 'queue full' }, 503);

  let accepted = 0;
  let skipped = 0;
  for (const item of items) {
    if (!acceptable(item)) {
      skipped++;
      continue;
    }
    const key = 'q:' + (await sha256(item.src.trim())).slice(0, 24);
    if (await env.CORPUS.get(key)) {
      skipped++;                    // 別人已經送過同一句了
      continue;
    }
    await env.CORPUS.put(
      key,
      JSON.stringify({
        src: item.src.trim(),
        role: typeof item.role === 'string' ? item.role.slice(0, 16) : 'desc',
        domain: typeof item.domain === 'string' ? item.domain.slice(0, 24) : '',
        ctx: typeof item.ctx === 'string' ? item.ctx.slice(0, 96) : '',
        seen: Number.isFinite(item.seen) ? Math.min(item.seen, 9999) : 1,
        v: typeof body.v === 'string' ? body.v.slice(0, 16) : '',
      }),
      // 三十天沒人來撈就自己過期。撈不走的資料沒有留著的意義。
      { expirationTtl: 60 * 60 * 24 * 30 },
    );
    accepted++;
  }
  await env.CORPUS.put('meta:count', String(queued + accepted));
  return json({ accepted, skipped });
}

/**
 * 把佇列撈走。只有拿著 DRAIN_TOKEN 的人叫得動——那是 GitHub Action 的 secret。
 *
 * 撈完<b>不</b>立刻刪：先回傳，等 Action 真的開好 PR 再呼叫 /ack 把那批刪掉。
 * 中間 Action 掛掉的話，下一次還撈得到同一批，不會平白掉資料。
 */
async function drain(request, env) {
  const auth = request.headers.get('authorization') || '';
  if (!env.DRAIN_TOKEN || auth !== 'Bearer ' + env.DRAIN_TOKEN) {
    return json({ error: 'nope' }, 401);
  }
  const listed = await env.CORPUS.list({ prefix: 'q:', limit: 1000 });
  const entries = {};
  const keys = [];
  let seq = 0;
  for (const k of listed.keys) {
    const raw = await env.CORPUS.get(k.name);
    if (!raw) continue;
    const one = JSON.parse(raw);
    entries[String(seq).padStart(4, '0')] = {
      src: one.src,
      dst: '',
      role: one.role,
      domain: one.domain,
      ctx: one.ctx,
      seen: one.seen,
      seq,
    };
    keys.push(k.name);
    seq++;
  }
  // 刻意長得跟 captured.json 一模一樣：Action 那邊就能原封不動餵給
  // tools/import-captured.py，不必為了這條路另外寫一支匯入工具。
  return json({
    _meta: { count: seq, note: '由收集站彙整，格式同 captured.json' },
    entries,
    _keys: keys,
  });
}

/** Action 開完 PR 之後回來說「這批處理好了」，這裡才真的刪。 */
async function ack(request, env) {
  const auth = request.headers.get('authorization') || '';
  if (!env.DRAIN_TOKEN || auth !== 'Bearer ' + env.DRAIN_TOKEN) {
    return json({ error: 'nope' }, 401);
  }
  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: 'bad json' }, 400);
  }
  const keys = Array.isArray(body.keys) ? body.keys.slice(0, 1000) : [];
  for (const k of keys) {
    if (typeof k === 'string' && k.startsWith('q:')) await env.CORPUS.delete(k);
  }
  const queued = Number((await env.CORPUS.get('meta:count')) || 0);
  await env.CORPUS.put('meta:count', String(Math.max(0, queued - keys.length)));
  return json({ deleted: keys.length });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === 'POST' && url.pathname === '/submit') {
      return submit(request, env);
    }
    if (request.method === 'GET' && url.pathname === '/drain') {
      return drain(request, env);
    }
    if (request.method === 'POST' && url.pathname === '/ack') {
      return ack(request, env);
    }
    if (request.method === 'GET' && url.pathname === '/') {
      return json({
        what: 'WynnChaYuan 語料收集站',
        queued: Number((await env.CORPUS.get('meta:count')) || 0),
      });
    }
    return json({ error: 'not found' }, 404);
  },
};
