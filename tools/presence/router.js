import { LANGS, PATHS, pickLang, render, renderLost } from './site.js';
import { handleAdminCapes, handleAdminRemove, handleApprove, handleBans, handlePeople, handlePerkAdd, handlePerkRemove, handlePerks, handleReviewAdd, handleReviewRemove, handleReviews, handleStaff, handleStaffAdd, handleStaffRemove, handleUnban } from './admin.js';
import { ICONS, font, icon, page, texture } from './assets.js';
import { discordOn, handleChallenge, handleDiscordCallback, handleDiscordStart, handleLink, handleLinkIntent, handleLinkStart, handleLogin, handleUnlink, handleWebLink } from './auth.js';
import { handleCape, handleCapeLink, handleCapePreview, handleCapes, handleForget, handleIdle, handleMine, handlePublic, handleRemove, handleRestore, handleStar, handleUpload, handleWear, tidy } from './capes.js';
import { GUILD_SLUG, handleAddCredit, handleCreditTeams, handleGuildAdd, handleGuildImage, handleGuildPicture, handleGuildRemove, handleGuilds, handleTeam } from './credits.js';
import { handleDiscordInfo, handleDiscordInteraction, handleDiscordRegister } from './discord-bot.js';
import { handleFeedback, handleFeedbackDelete, handleFeedbackIssue, handleFeedbackList, handleFeedbackStatus } from './feedback.js';
import { json, no } from './http.js';
import { handleHello, handleList, handleMe, storedList } from './presence.js';
import { handleDeleteMe, indexOwners } from './privacy.js';
import { handleLangTeam, handleLangTeamSet, handleTrIndex, handleTranslatorAdd, handleTranslatorRemove, handleTranslators, scrubTr, trRoute } from './translate.js';

const site = {
  async scheduled(event, env) {
    await tidy(env, Date.now());
    await indexOwners(env, Date.now());
  },
  async fetch(request, env, ctx) {
    // Discord 檢查條款與隱私政策網址時送 HEAD：跟 GET 一樣回，不帶內容
    if (request.method === 'HEAD' && !new URL(request.url).pathname.startsWith('/api/')) {
      const full = await site.fetch(new Request(request.url, { method: 'GET', headers: request.headers }), env, ctx);
      return new Response(null, { status: full.status, headers: full.headers });
    }
    const url = new URL(request.url);
    const now = Date.now();
    const path = url.pathname;
    if (request.method === 'GET' && PATHS.includes(path)) {
      const asked = url.searchParams.get('lang');
      const lang = LANGS.includes(asked) ? asked
        : pickLang(request.headers.get('cookie'), request.headers.get('accept-language'));
      const bare = !LANGS.includes(asked) && !request.headers.get('accept-language')
        && !/(?:^|;\s*)lang=/.test(request.headers.get('cookie') || '');
      return page(render(path, lang, bare), lang);
    }
    if (request.method === 'GET' && Object.hasOwn(ICONS, path)) return icon(ICONS[path]);
    if (request.method === 'GET' && path.startsWith('/font/')) return font(path);
    if (request.method === 'GET' && (path.startsWith('/tex/') || path.startsWith('/template/'))) return texture(path);
    if (request.method === 'GET' && path === '/auth/discord') return handleDiscordStart(request, env, now);
    if (request.method === 'GET' && path === '/auth/discord/callback') {
      return handleDiscordCallback(request, env, now);
    }
    if (request.method === 'GET' && path === '/gallery') {
      return Response.redirect(url.origin + '/capes', 302);
    }
    if (!path.startsWith('/api/')) {
      if (request.method !== 'GET') return no('not found', 404);
      const lang = pickLang(request.headers.get('cookie'), request.headers.get('accept-language'));
      return page(renderLost(lang), lang, 404);
    }
    const route = request.method + ' ' + path.slice(4);
    if (route === 'POST /challenge') return handleChallenge(env, now);
    if (route === 'POST /login') return handleLogin(request, env, now);
    if (route === 'POST /web-link') return handleWebLink(request, env, now);
    if (route === 'POST /link/start') return handleLinkStart(request, env, now);
    if (route === 'POST /link/intent') return handleLinkIntent(request, env, now);
    if (route === 'POST /link') return handleLink(request, env, now);
    if (route === 'POST /unlink') return handleUnlink(request, env, now);
    if (route === 'GET /me') return handleMe(request, env, now);
    if (route === 'POST /me/delete') return handleDeleteMe(request, env, now);
    if (route === 'POST /hello') return handleHello(request, env, now);
    if (route === 'GET /list') return handleList(env, now);
    if (route === 'PUT /cape') return handleUpload(request, env, now);
    if (route === 'DELETE /cape') return handleRemove(request, env, now);
    if (route === 'GET /capes') return handleCapes(env, now);
    if (route === 'GET /admin/capes') return handleAdminCapes(request, env, now);
    if (route === 'POST /admin/remove') return handleAdminRemove(request, env, now);
    if (route === 'GET /admin/perks') return handlePerks(request, env, now);
    if (route === 'POST /admin/perk') return handlePerkAdd(request, env, now);
    if (route === 'POST /admin/unperk') return handlePerkRemove(request, env, now);
    if (route === 'GET /admin/bans') return handleBans(request, env, now);
    if (route === 'POST /admin/unban') return handleUnban(request, env, now);
    if (route === 'POST /admin/credits') return handleAddCredit(request, env, now);
    if (route === 'POST /admin/credits/teams') return handleCreditTeams(request, env, now);
    if (route === 'GET /admin/staff') return handleStaff(request, env, now);
    if (route === 'POST /admin/staff') return handleStaffAdd(request, env, now);
    if (route === 'POST /admin/unstaff') return handleStaffRemove(request, env, now);
    if (route === 'GET /admin/people') return handlePeople(request, env, now);
    if (route === 'GET /admin/translators') return handleTranslators(request, env, now);
    if (route === 'POST /admin/translator') return handleTranslatorAdd(request, env, now);
    if (route === 'POST /admin/untranslator') return handleTranslatorRemove(request, env, now);
    if (route === 'GET /admin/langteam') return handleLangTeam(request, env, now);
    if (route === 'POST /admin/langteam') return handleLangTeamSet(request, env, now);
    if (route.startsWith('GET /tr/index/')) return handleTrIndex(request, env, route.slice('GET /tr/index/'.length), ctx);
    if (route.startsWith('GET /tr/') || route.startsWith('POST /tr/')) {
      return scrubTr(request, env, now, await trRoute(route, request, env, now));
    }
    if (route === 'POST /feedback') return handleFeedback(request, env, now);
    if (route === 'GET /admin/feedback') return handleFeedbackList(request, env, now);
    if (route === 'POST /admin/feedback/status') return handleFeedbackStatus(request, env, now);
    if (route === 'POST /admin/feedback/delete') return handleFeedbackDelete(request, env, now);
    if (route === 'POST /admin/feedback/issue') return handleFeedbackIssue(request, env, now);
    if (route === 'POST /discord/interactions') return handleDiscordInteraction(request, env, ctx);
    if (route === 'GET /admin/discord') return handleDiscordInfo(request, env, now);
    if (route === 'POST /admin/discord/register') return handleDiscordRegister(request, env, now);
    if (route === 'GET /team') return handleTeam(env, now);
    if (route === 'POST /cape/wear') return handleWear(request, env, now);
    if (route === 'POST /cape/restore') return handleRestore(request, env, now);
    if (route === 'POST /cape/star') return handleStar(request, env, now);
    if (route === 'POST /cape/public' || route === 'POST /cape/visibility') return handlePublic(request, env, now);
    if (route === 'POST /cape/forget') return handleForget(request, env, now);
    if (route === 'GET /capes/mine') return handleMine(request, env, now);
    if (route.startsWith('GET /capes/')) return handleCapeLink(request, env, now, route.slice('GET /capes/'.length));
    if (route === 'GET /admin/reviews') return handleReviews(request, env, now);
    if (route === 'POST /admin/review') return handleReviewAdd(request, env, now);
    if (route === 'POST /admin/unreview') return handleReviewRemove(request, env, now);
    if (route === 'POST /admin/approve') return handleApprove(request, env, now);
    if (route === 'GET /admin/guilds') return handleGuilds(request, env, now);
    if (route === 'POST /admin/guild') return handleGuildAdd(request, env, now);
    if (route === 'POST /admin/unguild') return handleGuildRemove(request, env, now);
    {
      const picture = /^(GET|PUT) \/(admin\/)?guild\/([a-z-]{3,30})\/(icon|banner)$/.exec(route);
      if (picture && GUILD_SLUG.test(picture[3])) {
        if (picture[1] === 'PUT' && picture[2]) return handleGuildImage(request, env, now, picture[3], picture[4]);
        if (picture[1] === 'GET' && !picture[2]) return handleGuildPicture(env, picture[3], picture[4]);
      }
    }
    if (route.startsWith('GET /cape-preview/')) {
      return handleCapePreview(env, route.slice('GET /cape-preview/'.length), url.searchParams.get('u'));
    }
    if (request.method === 'GET' && route.startsWith('GET /idle/')) {
      return handleIdle(env, route.slice('GET /idle/'.length));
    }
    if (request.method === 'GET' && route.startsWith('GET /cape/')) {
      return handleCape(env, route.slice('GET /cape/'.length), url.searchParams.get('k'));
    }
    if (route === 'GET /status') {
      const list = await storedList(env);
      return json({
        what: 'WynnChaYuan',
        users: list ? list.users.length : 0,
        capes: list ? Object.keys(list.capes).length : 0,
        uploads: env.UPLOADS || 'tiered',
        discord: discordOn(env),
      });
    }
    return no('not found', 404);
  },
};

export default site;
