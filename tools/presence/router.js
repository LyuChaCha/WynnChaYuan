import { LANGS, PATHS, pickLang, render, renderLost } from './site.js';
import { handleAdminCapes, handleAdminRemove, handleApprove, handleBans, handlePeople, handlePerkAdd, handlePerkRemove, handlePerks, handleReviewAdd, handleReviewRemove, handleReviews, handleStaff, handleStaffAdd, handleStaffRemove, handleUnban } from './admin.js';
import { ICONS, font, icon, page, script, texture } from './assets.js';
import { handleChallenge, handleDiscordCallback, handleDiscordStart, handleLink, handleLinkIntent, handleLinkStart, handleLogin, handleUnlink, handleWebLink } from './auth.js';
import { handleCape, handleCapeLink, handleCapePreview, handleCapes, handleForget, handleIdle, handleMine, handlePublic, handleRemove, handleRestore, handleStar, handleUpload, handleWear, recountStars, tidy } from './capes.js';
import { GUILD_SLUG, handleAddCredit, handleCreditTeams, handleGuildAdd, handleGuildImage, handleGuildPicture, handleGuildRemove, handleGuilds, handleTeam } from './credits.js';
import { handleDiscordInfo, handleDiscordInteraction, handleDiscordRegister } from './discord-bot.js';
import { handleFeedback, handleFeedbackDelete, handleFeedbackIssue, handleFeedbackList, handleFeedbackStatus } from './feedback.js';
import { json, no } from './http.js';
import { handleHello, handleList, handleMe, handleStatus } from './presence.js';
import { handleDeleteMe, indexOwners } from './privacy.js';
import { handleLangTeam, handleLangTeamSet, handleTrIndex, handleTranslatorAdd, handleTranslatorRemove, handleTranslators, scrubTr, trRoute } from './translate.js';

const withRequest = (handler) => ({ request, env, now }) => handler(request, env, now);

const API_ROUTES = new Map([
  ['POST /challenge', ({ env, now }) => handleChallenge(env, now)],
  ['POST /login', withRequest(handleLogin)],
  ['POST /web-link', withRequest(handleWebLink)],
  ['POST /link/start', withRequest(handleLinkStart)],
  ['POST /link/intent', withRequest(handleLinkIntent)],
  ['POST /link', withRequest(handleLink)],
  ['POST /unlink', withRequest(handleUnlink)],
  ['GET /me', withRequest(handleMe)],
  ['POST /me/delete', withRequest(handleDeleteMe)],
  ['POST /hello', withRequest(handleHello)],
  ['GET /list', ({ env, now, ctx }) => handleList(env, now, ctx)],
  ['PUT /cape', withRequest(handleUpload)],
  ['DELETE /cape', withRequest(handleRemove)],
  ['GET /capes', ({ env, now }) => handleCapes(env, now)],
  ['GET /admin/capes', withRequest(handleAdminCapes)],
  ['POST /admin/remove', withRequest(handleAdminRemove)],
  ['GET /admin/perks', withRequest(handlePerks)],
  ['POST /admin/perk', withRequest(handlePerkAdd)],
  ['POST /admin/unperk', withRequest(handlePerkRemove)],
  ['GET /admin/bans', withRequest(handleBans)],
  ['POST /admin/unban', withRequest(handleUnban)],
  ['POST /admin/credits', withRequest(handleAddCredit)],
  ['POST /admin/credits/teams', withRequest(handleCreditTeams)],
  ['GET /admin/staff', withRequest(handleStaff)],
  ['POST /admin/staff', withRequest(handleStaffAdd)],
  ['POST /admin/unstaff', withRequest(handleStaffRemove)],
  ['GET /admin/people', withRequest(handlePeople)],
  ['GET /admin/translators', withRequest(handleTranslators)],
  ['POST /admin/translator', withRequest(handleTranslatorAdd)],
  ['POST /admin/untranslator', withRequest(handleTranslatorRemove)],
  ['GET /admin/langteam', withRequest(handleLangTeam)],
  ['POST /admin/langteam', withRequest(handleLangTeamSet)],
  ['POST /feedback', withRequest(handleFeedback)],
  ['GET /admin/feedback', withRequest(handleFeedbackList)],
  ['POST /admin/feedback/status', withRequest(handleFeedbackStatus)],
  ['POST /admin/feedback/delete', withRequest(handleFeedbackDelete)],
  ['POST /admin/feedback/issue', withRequest(handleFeedbackIssue)],
  ['POST /discord/interactions', ({ request, env, ctx }) => handleDiscordInteraction(request, env, ctx)],
  ['GET /admin/discord', withRequest(handleDiscordInfo)],
  ['POST /admin/discord/register', withRequest(handleDiscordRegister)],
  ['GET /team', ({ env, now }) => handleTeam(env, now)],
  ['POST /cape/wear', withRequest(handleWear)],
  ['POST /cape/restore', withRequest(handleRestore)],
  ['POST /cape/star', withRequest(handleStar)],
  ['POST /cape/public', withRequest(handlePublic)],
  ['POST /cape/visibility', withRequest(handlePublic)],
  ['POST /cape/forget', withRequest(handleForget)],
  ['GET /capes/mine', withRequest(handleMine)],
  ['GET /admin/reviews', withRequest(handleReviews)],
  ['POST /admin/review', withRequest(handleReviewAdd)],
  ['POST /admin/unreview', withRequest(handleReviewRemove)],
  ['POST /admin/approve', withRequest(handleApprove)],
  ['GET /admin/guilds', withRequest(handleGuilds)],
  ['POST /admin/guild', withRequest(handleGuildAdd)],
  ['POST /admin/unguild', withRequest(handleGuildRemove)],
  ['GET /status', ({ env }) => handleStatus(env)],
]);

const translateRoute = async ({ request, env, now, route }) => scrubTr(request, env, now, await trRoute(route, request, env, now));

const API_PREFIXES = [
  ['GET /tr/index/', ({ request, env, ctx }, rest) => handleTrIndex(request, env, rest, ctx)],
  ['GET /tr/', translateRoute],
  ['POST /tr/', translateRoute],
  ['GET /capes/', ({ request, env, now }, rest) => handleCapeLink(request, env, now, rest)],
  ['GET /cape-preview/', ({ env, url }, rest) => handleCapePreview(env, rest, url.searchParams.get('u'))],
  ['GET /idle/', ({ env }, rest) => handleIdle(env, rest)],
  ['GET /cape/', ({ env, url }, rest) => handleCape(env, rest, url.searchParams.get('k'))],
];

const GUILD_PICTURE = /^(GET|PUT) \/(admin\/)?guild\/([a-z-]{3,30})\/(icon|banner)$/;

function guildPictureRoute(call) {
  const picture = GUILD_PICTURE.exec(call.route);
  if (!picture || !GUILD_SLUG.test(picture[3])) return null;
  if (picture[1] === 'PUT' && picture[2]) return handleGuildImage(call.request, call.env, call.now, picture[3], picture[4]);
  if (picture[1] === 'GET' && !picture[2]) return handleGuildPicture(call.env, picture[3], picture[4]);
  return null;
}

function routeApi(call) {
  const exact = API_ROUTES.get(call.route);
  if (exact) return exact(call);
  const picture = guildPictureRoute(call);
  if (picture) return picture;
  for (const [prefix, handler] of API_PREFIXES) {
    if (call.route.startsWith(prefix)) return handler(call, call.route.slice(prefix.length));
  }
  return no('not found', 404);
}

function languageOf(request, url) {
  const asked = url.searchParams.get('lang');
  if (LANGS.includes(asked)) return { lang: asked, bare: false };
  const cookie = request.headers.get('cookie');
  const accept = request.headers.get('accept-language');
  return { lang: pickLang(cookie, accept), bare: !accept && !/(?:^|;\s*)lang=/.test(cookie || '') };
}

function routeSite(call) {
  const { request, env, now, url, path } = call;
  const get = request.method === 'GET';
  if (get && PATHS.includes(path)) {
    const { lang, bare } = languageOf(request, url);
    return page(render(path, lang, bare), lang);
  }
  if (get && Object.hasOwn(ICONS, path)) return icon(ICONS[path]);
  if (get && path.startsWith('/font/')) return font(path);
  if (get && path.startsWith('/js/')) return script(path);
  if (get && (path.startsWith('/tex/') || path.startsWith('/template/'))) return texture(path);
  if (get && path === '/auth/discord') return handleDiscordStart(request, env, now);
  if (get && path === '/auth/discord/callback') return handleDiscordCallback(request, env, now);
  if (get && path === '/gallery') return Response.redirect(url.origin + '/capes', 302);
  if (!get) return no('not found', 404);
  const lang = pickLang(request.headers.get('cookie'), request.headers.get('accept-language'));
  return page(renderLost(lang), lang, 404);
}

async function dispatch(request, env, ctx) {
  const url = new URL(request.url);
  const path = url.pathname;
  // Discord 檢查條款與隱私政策網址時送 HEAD：跟 GET 一樣回，不帶內容
  if (request.method === 'HEAD' && !path.startsWith('/api/')) {
    const full = await site.fetch(new Request(request.url, { method: 'GET', headers: request.headers }), env, ctx);
    return new Response(null, { status: full.status, headers: full.headers });
  }
  const call = { request, env, ctx, url, path, now: Date.now() };
  if (!path.startsWith('/api/')) return routeSite(call);
  call.route = request.method + ' ' + path.slice(4);
  return routeApi(call);
}

function logFailure(event, detail, error) {
  console.error(JSON.stringify({ event, ...detail, error: error instanceof Error ? error.stack || error.message : String(error) }));
}

const site = {
  async scheduled(event, env) {
    for (const [task, run] of [['tidy', tidy], ['indexOwners', indexOwners], ['recountStars', recountStars]]) {
      try {
        await run(env, Date.now());
      } catch (error) {
        logFailure('scheduled failed', { task }, error);
      }
    }
  },
  async fetch(request, env, ctx) {
    try {
      return await dispatch(request, env, ctx);
    } catch (error) {
      logFailure('request failed', { method: request.method, path: new URL(request.url).pathname }, error);
      return json({ ok: false, why: 'server error' }, 500, { 'cache-control': 'no-store' });
    }
  },
};

export default site;
