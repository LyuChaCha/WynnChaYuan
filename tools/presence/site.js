/**
 * wynnchayuan.com 的網頁：首頁、披風一覽、帳號、管理。
 *
 * 四頁共用一份樣式、頁首與頁尾，各自一段程式。全部是靜態的字串，資料都跟 /api 要。
 *
 * 登入：網站沒辦法自己確認 Minecraft 帳號，所以是<b>從遊戲裡開</b>——模組拿通行證換一條
 * 網址（#t=…），這裡把它收進 localStorage、從網址列上拿掉，之後每個 API 請求帶著它。
 *
 * 頁面裡的程式刻意不用樣板字串（反引號、錢字號加大括號）：整頁是包在樣板字串裡的。
 */

const STYLE = String.raw`
  :root {
    --ground: #0f1512; --panel: #161e19; --panel-2: #1c2620; --line: #2a382f;
    --ink: #e9efe9; --ink-2: #a3b3a7; --leaf: #7fd08a; --leaf-deep: #2f7d46;
    --gold: #d8b765; --cyan: #5bd8e8; --danger: #e08a8a;
  }
  * { box-sizing: border-box; }
  html { scroll-behavior: smooth; }
  body {
    margin: 0; min-height: 100vh; display: flex; flex-direction: column;
    background: var(--ground); color: var(--ink);
    font: 15px/1.7 "Segoe UI", "Microsoft JhengHei", "PingFang TC", "Noto Sans TC", system-ui, sans-serif;
    background-image: radial-gradient(1100px 520px at 50% -180px, #1d3324 0%, transparent 70%);
    background-repeat: no-repeat;
  }
  a { color: var(--leaf); text-decoration: none; }
  a:hover { text-decoration: underline; }
  a:focus-visible, button:focus-visible, input:focus-visible, select:focus-visible {
    outline: 2px solid var(--leaf); outline-offset: 2px;
  }
  .top { border-bottom: 1px solid var(--line); background: #0f1512e6; position: sticky; top: 0; z-index: 5; backdrop-filter: blur(8px); }
  .top .in { max-width: 1120px; margin: 0 auto; padding: 12px 20px; display: flex; align-items: center; gap: 22px; }
  .brand { display: flex; align-items: center; gap: 10px; color: var(--ink); font-weight: 650; font-size: 17px; letter-spacing: .02em; }
  .brand:hover { text-decoration: none; }
  .brand svg { width: 26px; height: 26px; }
  nav { display: flex; gap: 4px; flex-wrap: wrap; }
  nav a { padding: 6px 12px; border-radius: 8px; color: var(--ink-2); font-size: 14px; }
  nav a:hover { color: var(--ink); background: var(--panel-2); text-decoration: none; }
  nav a[aria-current="page"] { color: var(--ink); background: var(--panel-2); }
  .me { margin-left: auto; display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--ink-2); }
  .me img { width: 22px; height: 22px; border-radius: 4px; image-rendering: pixelated; }
  main { flex: 1; width: 100%; max-width: 1120px; margin: 0 auto; padding: 44px 20px 72px; }
  h1 { margin: 0; font-size: 28px; font-weight: 650; letter-spacing: .02em; text-wrap: balance; }
  h2 { margin: 44px 0 14px; font-size: 19px; font-weight: 650; }
  .lede { margin: 6px 0 0; color: var(--ink-2); max-width: 62ch; }
  .panel { padding: 16px 18px; border: 1px solid var(--line); border-radius: 12px; background: var(--panel); }
  .note { color: var(--ink-2); font-size: 14px; }
  .note b { color: var(--gold); font-weight: 600; }
  .btn {
    display: inline-flex; align-items: center; gap: 8px; font: inherit; font-size: 14px; cursor: pointer;
    padding: 8px 16px; border-radius: 9px; border: 1px solid var(--line); background: var(--panel-2); color: var(--ink);
  }
  .btn:hover { border-color: var(--leaf-deep); text-decoration: none; }
  .btn.primary { background: var(--leaf); border-color: var(--leaf); color: #0c1a10; font-weight: 600; }
  .btn.primary:hover { background: #93dba0; }
  .btn.danger { border-color: #5a2b2b; background: #2e1a1a; color: var(--danger); }
  .btn.danger:hover { background: #3d2020; }
  .btn[disabled] { opacity: .5; cursor: default; }
  label { display: block; font-size: 13px; color: var(--ink-2); margin: 0 0 4px; }
  input[type="text"], select {
    width: 100%; font: inherit; padding: 8px 10px; border-radius: 8px; color: var(--ink);
    border: 1px solid var(--line); background: #0f1512;
  }
  .msg { margin: 10px 0 0; font-size: 14px; min-height: 1.4em; }
  .msg.ok { color: var(--leaf); } .msg.bad { color: var(--danger); }
  table { width: 100%; border-collapse: collapse; font-size: 14px; }
  th { text-align: left; font-weight: 600; color: var(--ink-2); font-size: 13px; padding: 8px 10px; border-bottom: 1px solid var(--line); }
  td { padding: 9px 10px; border-bottom: 1px solid var(--line); vertical-align: middle; font-variant-numeric: tabular-nums; }
  td img { width: 22px; height: 22px; border-radius: 4px; vertical-align: middle; margin-right: 8px; image-rendering: pixelated; }
  .scroll { overflow-x: auto; }
  code { font-family: Consolas, "Cascadia Mono", monospace; font-size: 13px; color: var(--ink-2); }
  footer { border-top: 1px solid var(--line); color: var(--ink-2); font-size: 13px; }
  footer .in { max-width: 1120px; margin: 0 auto; padding: 22px 20px 28px; display: flex; flex-wrap: wrap; gap: 4px 24px; justify-content: space-between; }
  [hidden] { display: none !important; }

  /* 首頁 */
  .hero { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 36px; align-items: center; padding: 18px 0 8px; }
  .hero h1 { font-size: 40px; line-height: 1.25; }
  .hero .lede { font-size: 16px; }
  .hero img { width: 100%; max-width: 380px; justify-self: center; filter: drop-shadow(0 18px 40px #000a); }
  .cta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 22px; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 18px; }
  .chip { padding: 3px 11px; border: 1px solid var(--line); border-radius: 999px; font-size: 13px; color: var(--ink-2); background: var(--panel); }
  .features { display: grid; gap: 14px; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); }
  .features h3 { margin: 0 0 4px; font-size: 15px; font-weight: 650; }
  .features p { margin: 0; color: var(--ink-2); font-size: 14px; }
  .roles { display: grid; gap: 14px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); }
  .role { display: flex; gap: 12px; align-items: flex-start; }
  .role svg { flex: none; width: 26px; height: 26px; margin-top: 2px; }
  .role b { display: block; font-weight: 650; }
  .role span { color: var(--ink-2); font-size: 14px; }
  .split { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 14px; }

  /* 披風 */
  .bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 18px; margin: 22px 0 14px; }
  .count { font-variant-numeric: tabular-nums; color: var(--ink-2); }
  .count b { color: var(--ink); font-weight: 600; }
  .grid { display: grid; gap: 18px; grid-template-columns: repeat(auto-fill, minmax(168px, 1fr)); }
  .card {
    display: flex; flex-direction: column; padding: 0; text-align: left; color: inherit; font: inherit;
    background: var(--panel); border: 1px solid var(--line); border-radius: 12px; overflow: hidden;
    cursor: pointer; transition: transform .16s ease, border-color .16s ease, box-shadow .16s ease;
  }
  .card:hover { transform: translateY(-3px); border-color: var(--leaf-deep); box-shadow: 0 10px 26px #0008; }
  .stage { display: grid; place-items: center; padding: 20px 0 16px; background: linear-gradient(180deg, #1b2a21 0%, #131b16 100%); }
  .face { width: 96px; aspect-ratio: 10 / 16; overflow: hidden; border-radius: 3px; box-shadow: 0 8px 18px #000a, 0 0 0 1px #0006; background: #0b0f0d; }
  .face img { display: block; width: 220%; max-width: none; margin: -10% 0 0 -10%; image-rendering: pixelated; }
  .meta { display: flex; align-items: center; gap: 8px; padding: 10px 12px 12px; font-size: 13px; color: var(--ink-2); }
  .tag { padding: 1px 7px; border-radius: 999px; font-size: 11px; font-weight: 600; letter-spacing: .06em; color: var(--ground); background: var(--ink-2); }
  .tag.gif { background: var(--gold); }
  .meta .who { margin-left: auto; font-variant-numeric: tabular-nums; }
  .empty { grid-column: 1 / -1; padding: 64px 0; text-align: center; color: var(--ink-2); }
  dialog { width: min(92vw, 780px); padding: 0; color: var(--ink); background: var(--panel); border: 1px solid var(--line); border-radius: 14px; box-shadow: 0 30px 80px #000c; }
  dialog::backdrop { background: #070a08cc; }
  .view { display: grid; grid-template-columns: minmax(0, 1fr) 260px; background: linear-gradient(180deg, #1d2e24, #121915); }
  .view .side { background: var(--panel); }
  canvas.model { display: block; width: 100%; height: 440px; align-self: center; cursor: grab; }
  canvas.model:active { cursor: grabbing; }
  .side { padding: 18px; border-left: 1px solid var(--line); display: flex; flex-direction: column; gap: 12px; min-width: 0; }
  .side h2 { margin: 0; font-size: 16px; }
  .side dl { margin: 0; display: grid; grid-template-columns: auto 1fr; gap: 4px 12px; font-size: 13px; }
  .side dt { color: var(--ink-2); } .side dd { margin: 0; font-variant-numeric: tabular-nums; }
  .flat { width: 100%; image-rendering: pixelated; border-radius: 4px; background: #0b0f0d; }
  .owners { margin: 0; padding: 0; list-style: none; font-size: 13px; max-height: 120px; overflow: auto; }
  .owners li { display: flex; align-items: center; gap: 8px; padding: 3px 0; }
  .owners img { width: 18px; height: 18px; border-radius: 3px; image-rendering: pixelated; }
  .hint { color: var(--ink-2); font-size: 12px; margin: 0; }
  .spacer { flex: 1; }
  .account { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18px; align-items: start; }
  .who-am-i { display: flex; align-items: center; gap: 14px; }
  .who-am-i img { width: 48px; height: 48px; border-radius: 8px; image-rendering: pixelated; }
  .who-am-i b { font-size: 18px; }
  .steps { margin: 10px 0 0; padding-left: 1.3em; color: var(--ink-2); }
  .steps li { margin: 4px 0; } .steps b { color: var(--ink); font-weight: 600; }
  .form { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); align-items: end; }
  @media (max-width: 720px) {
    .hero, .account { grid-template-columns: 1fr; }
    .hero h1 { font-size: 30px; }
    .view { grid-template-columns: 1fr; }
    canvas.model { height: 320px; }
    .side { border-left: 0; border-top: 1px solid var(--line); }
    .top .in { gap: 10px; flex-wrap: wrap; }
  }
  @media (prefers-reduced-motion: reduce) { .card { transition: none; } .card:hover { transform: none; } html { scroll-behavior: auto; } }
`;

const LEAF = String.raw`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 21c.5-4.5 2.5-8 7-10"/><path d="M9 18c6.2 0 10.5-3.3 11-12V4h-4C7 4 4 8 4 13c0 1 0 3 2 5h3z"/></svg>`;

/** 每一頁都有的程式：收下登入的網址、帶著通行證問 API、頁首右邊那一格。 */
const COMMON = String.raw`
  var TOKEN_KEY = 'wcy.token';
  (function () {
    var found = location.hash.match(/t=([^&]+)/);
    if (!found) return;
    try { localStorage.setItem(TOKEN_KEY, found[1]); } catch (e) {}
    history.replaceState(null, '', location.pathname);     /* 通行證不留在網址列上 */
  })();
  function token() { try { return localStorage.getItem(TOKEN_KEY) || ''; } catch (e) { return ''; } }
  function signOut() { try { localStorage.removeItem(TOKEN_KEY); } catch (e) {} location.reload(); }
  function api(path, options) {
    options = options || {};
    options.headers = options.headers || {};
    if (token()) options.headers.authorization = 'Bearer ' + token();
    return fetch('/api' + path, options);
  }
  function head(name, size) { return 'https://mc-heads.net/avatar/' + encodeURIComponent(name) + '/' + (size || 32); }
  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }
  function bytes(n) { return n >= 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB'; }
  /* 我是誰。沒登入、或通行證過期了，就是 null。 */
  var whoami = !token() ? Promise.resolve(null) : api('/me').then(function (r) {
    if (r.status === 401) { try { localStorage.removeItem(TOKEN_KEY); } catch (e) {} return null; }
    return r.ok ? r.json() : null;
  }).catch(function () { return null; });
  whoami.then(function (me) {
    var slot = document.getElementById('me');
    slot.replaceChildren();
    if (!me) {
      var link = el('a', '', '登入');
      link.href = '/account';
      slot.append(link);
      return;
    }
    var img = el('img');
    img.alt = '';
    img.src = head(me.name, 44);
    var name = el('a', '', me.name);
    name.href = '/account';
    slot.append(img, name);
    if (me.developer) document.getElementById('nav-admin').hidden = false;
  });
`;

function layout(title, active, body, script) {
  const nav = [['/', '首頁'], ['/capes', '披風'], ['/account', '帳號']]
    .map(([href, label]) => '<a href="' + href + '"' + (href === active ? ' aria-current="page"' : '') + '>' + label + '</a>')
    .join('');
  return '<!doctype html>\n<html lang="zh-Hant">\n<head>\n<meta charset="utf-8">\n'
    + '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
    + '<meta name="color-scheme" content="dark">\n'
    + '<title>' + title + '</title>\n<style>' + STYLE + '</style>\n</head>\n<body>\n'
    + '<div class="top"><div class="in">'
    + '<a class="brand" href="/" style="color:var(--ink)"><span style="color:var(--leaf);display:flex">' + LEAF + '</span>WynnChaYuan</a>'
    + '<nav aria-label="主選單">' + nav
    + '<a href="/admin" id="nav-admin" hidden' + (active === '/admin' ? ' aria-current="page"' : '') + '>管理</a></nav>'
    + '<div class="me" id="me"></div>'
    + '</div></div>\n<main>' + body + '</main>\n'
    + '<footer><div class="in"><span>© 2026 WynnChaYuan - All rights reserved.</span>'
    + '<span>Designed by LyuChaCha</span></div></footer>\n'
    + '<script>' + COMMON + '</script>\n' + (script || '') + '\n</body>\n</html>';
}

// ------------------------------------------------------------------ 首頁

const RAW = 'https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/logo/2026/';

const HOME = layout('WynnChaYuan - Wynncraft 的多語言翻譯模組', '/', String.raw`
  <section class="hero">
    <div>
      <h1>把 Wynncraft 翻成你的語言</h1>
      <p class="lede">WynnChaYuan 是 Wynncraft 的多語言翻譯模組（Fabric 1.21.11）。原文保留，譯文顯示在旁邊，或直接寫進原本的位置——任務、對話、物品與介面都看得懂。</p>
      <div class="cta">
        <a class="btn primary" href="https://modrinth.com/mod/wynnchayuan">從 Modrinth 下載</a>
        <a class="btn" href="https://www.curseforge.com/minecraft/mc-mods/wynnchayuan">CurseForge</a>
        <a class="btn" href="https://github.com/LyuChaCha/WynnChaYuan/releases/latest">GitHub</a>
      </div>
      <div class="chips" aria-label="支援的語言">
        <span class="chip">繁體中文</span><span class="chip">简体中文</span><span class="chip">日本語</span>
        <span class="chip">한국어</span><span class="chip">Русский</span><span class="chip">Español</span><span class="chip">Türkçe</span>
      </div>
    </div>
    <img src="` + RAW + String.raw`wynnchayuan-logo-transparent-on-dark-1024.png" alt="WynnChaYuan 的標誌：一片葉子形狀的對話框" width="380" height="380">
  </section>

  <h2>翻了什麼</h2>
  <div class="features">
    <div class="panel"><h3>物品與裝備</h3><p>裝備敘述、屬性、Major ID。旁邊另開面板，或寫進原本的說明框。</p></div>
    <div class="panel"><h3>任務與對話</h3><p>寫進 Wynncraft 自己的對話框，逐字打出的節奏跟原文同步；任務追蹤也有。</p></div>
    <div class="panel"><h3>技能樹</h3><p>五個職業的節點、說明與流派。</p></div>
    <div class="panel"><h3>聊天與提示</h3><p>伺服器發的訊息：任務完成、獎勵、進出區域。玩家的發言不翻。</p></div>
    <div class="panel"><h3>選單與介面</h3><p>交易市場、公會、商城，還有 Lootrun、討伐戰、地城的面板。</p></div>
    <div class="panel"><h3>譯文自動更新</h3><p>新的翻譯合併之後，下次進遊戲就生效，不必更新模組。</p></div>
  </div>

  <h2>安裝</h2>
  <div class="split">
    <div class="panel">
      <p style="margin:0">需要 <a href="https://modrinth.com/mod/wynntils">Wynntils</a> 4.2 以上與 <a href="https://modrinth.com/mod/fabric-api">Fabric API</a>。純客戶端，伺服器不需要裝。</p>
      <p class="note" style="margin:8px 0 0">把 jar 放進 mods 資料夾，進遊戲按 F6 開設定。</p>
    </div>
    <div class="panel note">
      <b>關於譯文。</b>部分譯文由人工輸入再經 AI 轉出，一小部分直接由 AI 翻譯，大部分基礎內容經過人工校稿。其他語言目前多半是圍繞著繁體中文再做 AI 翻譯，可能有錯譯與用詞不一致。遇到問題請到 <a href="https://github.com/LyuChaCha/WynnChaYuan/issues">GitHub Issues</a> 回報。
    </div>
  </div>

  <h2>徽章與披風</h2>
  <div class="roles">
    <div class="panel role"><span style="color:#FFD24A;display:flex">` + LEAF + String.raw`</span><div><b>開發者</b><span>協助程式開發與維護。</span></div></div>
    <div class="panel role"><span style="color:#5BD8E8;display:flex">` + LEAF + String.raw`</span><div><b>贊助者</b><span>單次 20 USD 或每月 2 USD 以上。</span></div></div>
    <div class="panel role"><span style="color:#5BD86F;display:flex">` + LEAF + String.raw`</span><div><b>貢獻者</b><span>替自己的語言翻譯、修正錯誤，或提供程式上的實質協助。</span></div></div>
    <div class="panel role"><span style="color:#A8A8A8;display:flex">` + LEAF + String.raw`</span><div><b>裝了模組的人</b><span>名字前面是一片灰色的葉子。</span></div></div>
  </div>
  <div class="cta">
    <a class="btn" href="/capes">看看大家的披風</a>
    <a class="btn" href="https://ko-fi.com/lyuchacha">贊助</a>
    <a class="btn" href="https://github.com/LyuChaCha/WynnChaYuan/blob/main/CONTRIBUTING.md">怎麼參與</a>
  </div>
`);

// ------------------------------------------------------------------ 披風一覽

/** 立體檢視：一個灰色的人偶穿著披風。披風一覽與帳號那一頁都用。 */
const VIEWER = String.raw`
  /* 人偶的皮膚：沒有臉的灰色木偶，免得跟披風搶戲。 */
  function mannequin() {
    var c = document.createElement('canvas');
    c.width = 64; c.height = 64;
    var g = c.getContext('2d');
    g.fillStyle = '#8f9a93'; g.fillRect(0, 0, 64, 64);
    g.fillStyle = '#a4aea7'; g.fillRect(0, 0, 32, 16);
    g.fillStyle = '#7d8881'; g.fillRect(0, 16, 16, 16); g.fillRect(16, 48, 16, 16);
    return c;
  }
  /* 範本（22×17 格）放回 Minecraft 披風貼圖（64×32 格）的左上角。 */
  function capeCanvas(source, width) {
    var k = Math.max(1, Math.round(width / 22));
    var c = document.createElement('canvas');
    c.width = 64 * k; c.height = 32 * k;
    var g = c.getContext('2d');
    g.imageSmoothingEnabled = false;
    g.drawImage(source, 0, 0, 22 * k, 17 * k);
    return c;
  }
  var playing = 0;
  function stopCape() { playing++; }
  function makeViewer(canvas) {
    if (!window.skinview3d) return null;
    var viewer = new skinview3d.SkinViewer({ canvas: canvas, width: canvas.clientWidth, height: canvas.clientHeight, skin: mannequin() });
    viewer.controls.enableZoom = false;
    viewer.controls.enablePan = false;
    viewer.fov = 36;
    viewer.zoom = 0.86;
    /* 背對著，稍微側一點。要轉外面那一層：走路的動畫每一格都會把人本身轉回正面 */
    viewer.playerWrapper.rotation.y = Math.PI + 0.5;
    if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      viewer.animation = new skinview3d.WalkingAnimation();
      viewer.animation.speed = 0.55;
    }
    return viewer;
  }
  /* 把一件披風穿到人偶身上。GIF 一格一格換：瀏覽器有 ImageDecoder 就動，沒有就停在第一格。 */
  function wear(viewer, source, isGif) {
    var mine = ++playing;
    var image = new Image();
    image.onload = function () {
      if (mine !== playing) return;
      viewer.loadCape(capeCanvas(image, image.naturalWidth));
      if (!isGif || !('ImageDecoder' in window)) return;
      fetch(source).then(function (r) { return r.arrayBuffer(); }).then(function (data) {
        var decoder = new ImageDecoder({ data: data, type: 'image/gif' });
        return decoder.tracks.ready.then(function () {
          var count = decoder.tracks.selectedTrack.frameCount;
          var index = 0;
          function next() {
            if (mine !== playing) { decoder.close(); return; }
            decoder.decode({ frameIndex: index }).then(function (result) {
              var frame = result.image;
              if (mine !== playing) { frame.close(); decoder.close(); return; }
              viewer.loadCape(capeCanvas(frame, frame.displayWidth));
              var wait = Math.max(20, (frame.duration || 100000) / 1000);
              frame.close();
              index = (index + 1) % count;
              setTimeout(next, wait);
            }).catch(function () {});
          }
          next();
        });
      }).catch(function () {});
    };
    image.src = source;
  }
`;

const SKINVIEW = '<script src="https://cdn.jsdelivr.net/npm/skinview3d@3.4.1/bundles/skinview3d.bundle.js"></script>';

const CAPES = layout('披風一覽 - WynnChaYuan', '/capes', String.raw`
  <h1>披風一覽</h1>
  <p class="lede">WynnChaYuan 玩家上傳的披風。點一件可以轉著看。</p>
  <div class="bar">
    <span class="count" id="count">讀取中…</span>
    <a class="btn" href="/account">上傳我的披風</a>
  </div>
  <p class="panel note"><b>不可以上傳：</b>政治、不雅或色情、違反法規、種族歧視或仇恨的內容。違規的披風會被移除，上傳的人可能不能再傳。</p>
  <div class="grid" id="grid" style="margin-top:22px"></div>

  <dialog id="viewer" aria-labelledby="viewer-title">
    <div class="view">
      <canvas class="model" id="model" width="520" height="440"></canvas>
      <div class="side">
        <h2 id="viewer-title">披風</h2>
        <img class="flat" id="flat" alt="披風的範本圖：左邊正面、右邊背面">
        <dl>
          <dt>格式</dt><dd id="d-type"></dd>
          <dt>大小</dt><dd id="d-size"></dd>
          <dt>使用</dt><dd id="d-users"></dd>
          <dt>上傳</dt><dd id="d-date"></dd>
        </dl>
        <ul class="owners" id="owners" hidden></ul>
        <p class="hint" id="hint">拖曳可以旋轉。</p>
        <div class="spacer"></div>
        <button class="btn danger" id="del" hidden>刪除這件披風</button>
        <button class="btn danger" id="ban" hidden>刪除並封禁上傳的人</button>
        <button class="btn" id="close">關閉</button>
      </div>
    </div>
  </dialog>
`, SKINVIEW + '<script>' + VIEWER + String.raw`
  var grid = document.getElementById('grid');
  var dialog = document.getElementById('viewer');
  var current = null;
  var viewer = null;
  var developer = false;
  function url(cape) { return '/api/cape/' + cape.ids[0] + '?v=' + cape.v; }

  function open(cape) {
    current = cape;
    document.getElementById('flat').src = url(cape);
    document.getElementById('d-type').textContent = cape.t.toUpperCase();
    document.getElementById('d-size').textContent = bytes(cape.s);
    document.getElementById('d-users').textContent = cape.ids.length + ' 人';
    document.getElementById('d-date').textContent = new Date(cape.v).toLocaleDateString();
    document.getElementById('del').hidden = !developer;
    document.getElementById('ban').hidden = !developer;
    var owners = document.getElementById('owners');
    owners.replaceChildren();
    owners.hidden = !(developer && cape.owners);
    (cape.owners || []).forEach(function (owner) {
      var li = el('li');
      if (owner.name) {
        var img = el('img');
        img.alt = '';
        img.src = head(owner.name, 36);
        li.append(img);
      }
      li.append(el('span', '', owner.name || owner.uuid));
      owners.append(li);
    });
    dialog.showModal();
    var hint = document.getElementById('hint');
    if (!viewer) viewer = makeViewer(document.getElementById('model'));
    if (!viewer) { hint.textContent = '立體檢視載不起來，只能看平面的圖。'; return; }
    hint.textContent = '拖曳可以旋轉。';
    wear(viewer, url(cape), cape.t === 'gif');
  }
  function shut() { stopCape(); current = null; if (dialog.open) dialog.close(); }
  document.getElementById('close').onclick = shut;
  dialog.addEventListener('close', stopCape);
  dialog.addEventListener('click', function (e) { if (e.target === dialog) shut(); });

  function remove(ban) {
    var cape = current;
    if (!cape) return;
    var text = ban ? '刪除這件披風，並封禁用它的 ' + cape.ids.length + ' 個人（之後不能再上傳）？'
                   : '刪除這件披風（' + cape.ids.length + ' 個人在用）？';
    if (!confirm(text)) return;
    var body = { hash: cape.h };
    if (ban) body.ban = true;
    api('/admin/remove', { method: 'POST', body: JSON.stringify(body), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        if (r.ok) { shut(); load(); }
        else alert('沒有成功（' + r.status + '）。登入一天過期，請從遊戲裡重新開一次。');
      });
  }
  document.getElementById('del').onclick = function () { remove(false); };
  document.getElementById('ban').onclick = function () { remove(true); };

  function load() {
    return api(developer ? '/admin/capes' : '/capes').then(function (r) { return r.json(); }).then(function (data) {
      var count = document.getElementById('count');
      count.replaceChildren('共 ', el('b', '', String(data.capes.length)), ' 件（相同的圖算一件）');
      grid.replaceChildren();
      if (!data.capes.length) grid.append(el('div', 'empty', '還沒有人上傳披風。'));
      data.capes.forEach(function (cape) {
        var card = el('button', 'card');
        card.type = 'button';
        card.setAttribute('aria-label', cape.t.toUpperCase() + ' 披風，' + cape.ids.length + ' 人使用');
        var stage = el('div', 'stage');
        var face = el('div', 'face');
        var img = el('img');
        img.loading = 'lazy';
        img.alt = '';
        img.src = url(cape);
        face.append(img);
        stage.append(face);
        var meta = el('div', 'meta');
        meta.append(el('span', 'tag ' + cape.t, cape.t.toUpperCase()), el('span', 'who', cape.ids.length + ' 人'));
        card.append(stage, meta);
        card.onclick = function () { open(cape); };
        grid.append(card);
      });
    });
  }
  whoami.then(function (me) {
    developer = Boolean(me && me.developer);
    return load();
  }).catch(function () { document.getElementById('count').textContent = '讀不到清單，請重新整理。'; });
` + '</script>');

// ------------------------------------------------------------------ 帳號

const ACCOUNT = layout('帳號 - WynnChaYuan', '/account', String.raw`
  <h1>帳號</h1>
  <div id="out" hidden>
    <p class="lede">網站沒辦法自己確認你的 Minecraft 帳號，所以登入是從遊戲裡開的。</p>
    <div class="panel" style="margin-top:22px;max-width:640px">
      <b>怎麼登入</b>
      <ol class="steps">
        <li>進 Wynncraft，按 <b>F6</b> 開 WynnChaYuan 的設定。</li>
        <li>到「<b>徽章與披風</b>」那一頁，確認「讓別人知道我有裝」開著。</li>
        <li>按「<b>披風一覽</b>」旁邊的「打開」。瀏覽器開起來的時候就是登入的。</li>
      </ol>
      <p class="note" style="margin:10px 0 0">登入一天有效，過期了從遊戲裡再開一次。離線帳號沒辦法登入。</p>
    </div>
  </div>
  <div id="in" hidden>
    <div class="account" style="margin-top:22px">
      <div class="panel">
        <div class="who-am-i"><img id="avatar" alt=""><div><b id="name"></b><div class="note" id="roles"></div></div></div>
        <p class="note" id="banned" hidden style="color:var(--danger);margin:12px 0 0">這個帳號不能上傳披風。</p>
        <div style="margin-top:16px"><button class="btn" id="signout">登出這個瀏覽器</button></div>
      </div>
      <div class="panel">
        <b>我的披風</b>
        <p class="note" id="nocape" style="margin:8px 0 0">還沒有上傳。</p>
        <canvas class="model" id="mine" width="400" height="320" hidden style="height:320px;border-radius:8px;margin-top:10px"></canvas>
        <p class="hint" id="minehint" hidden style="margin-top:6px"></p>
        <div style="margin-top:12px"><button class="btn danger" id="remove" hidden>移除我的披風</button></div>
      </div>
    </div>
    <h2>上傳披風</h2>
    <div class="panel" style="max-width:720px">
      <p class="note" style="margin:0 0 12px">要照<a href="https://github.com/LyuChaCha/WynnChaYuan/tree/main/capes/templates">範本</a>畫：22×17、44×34、88×68 或 176×136，檔案 20 MB 以內。PNG 大家都能傳；GIF（最多 120 格）只有開發者、贊助者、貢獻者能傳。</p>
      <p class="note" style="margin:0 0 12px"><b>不可以上傳：</b>政治、不雅或色情、違反法規、種族歧視或仇恨的內容。</p>
      <input type="file" id="file" accept="image/png,image/gif" aria-label="選一張披風的圖">
      <button class="btn primary" id="upload" style="margin-left:8px">上傳</button>
      <p class="msg" id="msg" role="status"></p>
    </div>
  </div>
`, SKINVIEW + '<script>' + VIEWER + String.raw`
  var WHY = {
    'too large': '檔案太大了（上限 20 MB）',
    'bad size': '尺寸不對：要照範本畫，22×17、44×34、88×68、176×136 其中一種',
    'bad format': '這不是 PNG 或 GIF，或是檔案壞了',
    'too many frames': 'GIF 的格數太多了（最多 120 格）',
    'gif members only': 'GIF 只有開發者、贊助者、貢獻者能上傳；PNG 大家都可以',
    'not listed': '目前只開放名單上的成員上傳',
    'banned': '這個帳號不能上傳披風',
    'uploads off': '現在沒有開放上傳',
    'slow down': '剛剛才傳過，等一下再試',
    'login first': '登入過期了，請從遊戲裡重新開一次'
  };
  var msg = document.getElementById('msg');
  function say(text, ok) { msg.textContent = text; msg.className = 'msg ' + (ok ? 'ok' : 'bad'); }
  var viewer = null;

  function show(me) {
    document.getElementById('nocape').hidden = Boolean(me.cape);
    document.getElementById('remove').hidden = !me.cape;
    var canvas = document.getElementById('mine');
    var hint = document.getElementById('minehint');
    canvas.hidden = !me.cape;
    hint.hidden = !me.cape;
    if (!me.cape) { stopCape(); return; }
    if (!viewer) viewer = makeViewer(canvas);
    if (!viewer) { hint.textContent = '立體檢視載不起來。'; canvas.hidden = true; return; }
    hint.textContent = me.cape.t.toUpperCase() + ' · ' + bytes(me.cape.s) + ' · 拖曳可以旋轉';
    wear(viewer, '/api/cape/' + me.id + '?v=' + me.cape.v, me.cape.t === 'gif');
  }
  function refresh() {
    return api('/me').then(function (r) { return r.ok ? r.json() : null; }).then(function (me) { if (me) show(me); });
  }

  whoami.then(function (me) {
    document.getElementById(me ? 'in' : 'out').hidden = false;
    if (!me) return;
    document.getElementById('avatar').src = head(me.name, 96);
    document.getElementById('name').textContent = me.name;
    document.getElementById('roles').textContent = me.developer ? '開發者' : (me.member ? '名單上的成員' : '裝了模組的玩家');
    document.getElementById('banned').hidden = !me.banned;
    show(me);
  });
  document.getElementById('signout').onclick = signOut;
  document.getElementById('remove').onclick = function () {
    if (!confirm('移除你上傳的披風？')) return;
    api('/cape', { method: 'DELETE' }).then(function (r) {
      if (r.ok) { say('披風拿掉了', true); refresh(); } else say('沒有成功，等一下再試', false);
    });
  };
  document.getElementById('upload').onclick = function () {
    var file = document.getElementById('file').files[0];
    if (!file) { say('先選一張圖', false); return; }
    if (file.size > 20 * 1024 * 1024) { say(WHY['too large'], false); return; }
    say('上傳中…', true);
    api('/cape', { method: 'PUT', body: file, headers: { 'content-type': file.type || 'application/octet-stream' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          if (r.ok) { say('披風上傳好了，別人幾分鐘內就會看到', true); refresh(); }
          else say(WHY[body.why] || '沒有成功（' + r.status + '），等一下再試', false);
        });
      }).catch(function () { say('連不上，等一下再試', false); });
  };
` + '</script>');

// ------------------------------------------------------------------ 管理

const ADMIN = layout('管理 - WynnChaYuan', '/admin', String.raw`
  <h1>管理</h1>
  <p class="lede" id="gate">這一頁只有名單上的開發者能用。請從遊戲裡登入。</p>
  <div id="panel" hidden>
    <p class="lede">披風的刪除與封禁在<a href="/capes">披風一覽</a>：點開一件，就看得到是誰傳的。</p>

    <h2>封禁名單</h2>
    <div class="panel scroll">
      <table>
        <thead><tr><th>玩家</th><th>UUID</th><th>封禁的時間</th><th>誰封的</th><th></th></tr></thead>
        <tbody id="bans"></tbody>
      </table>
      <p class="note" id="nobans" hidden style="margin:10px">目前沒有人被封禁。</p>
    </div>

    <h2>新增成員</h2>
    <div class="panel">
      <p class="note" style="margin:0 0 14px">送出之後會在 GitHub 開一個 PR 改 <code>credits.json</code>，合併之後玩家下次同步名單就生效（名牌的葉子、傳 GIF 披風的資格）。</p>
      <div class="form">
        <div><label for="c-section">身分</label>
          <select id="c-section"><option value="contributors">貢獻者</option><option value="sponsors">贊助者</option><option value="developers">開發者</option></select></div>
        <div><label for="c-name">顯示的名字</label><input type="text" id="c-name" maxlength="40" autocomplete="off"></div>
        <div><label for="c-mc">Minecraft ID</label><input type="text" id="c-mc" maxlength="16" autocomplete="off" spellcheck="false"></div>
        <div><label for="c-color">葉子顏色（可以不填）</label><input type="text" id="c-color" maxlength="7" placeholder="#RRGGBB" autocomplete="off" spellcheck="false"></div>
        <div><button class="btn primary" id="c-send">開 PR</button></div>
      </div>
      <p class="msg" id="c-msg" role="status"></p>
    </div>
  </div>
`, '<script>' + String.raw`
  var CREDIT_WHY = {
    'bad name': '顯示的名字不能空白，也不能超過 40 個字',
    'bad mc': 'Minecraft ID 只能是英數與底線，最多 16 個字',
    'bad color': '顏色要寫成 #RRGGBB',
    'bad section': '身分不對',
    'already listed': '這個 Minecraft ID 已經在那一區了',
    'github not configured': '還沒設定 GITHUB_TOKEN，沒辦法開 PR（見 tools/presence/README.md）',
    'forbidden': '登入過期了，或你不在開發者名單上'
  };
  function loadBans() {
    return api('/admin/bans').then(function (r) { return r.json(); }).then(function (data) {
      var body = document.getElementById('bans');
      body.replaceChildren();
      document.getElementById('nobans').hidden = data.bans.length > 0;
      data.bans.forEach(function (ban) {
        var row = el('tr');
        var who = el('td');
        if (ban.name) {
          var img = el('img');
          img.alt = '';
          img.src = head(ban.name, 44);
          who.append(img);
        }
        who.append(ban.name || '（不知道名字）');
        var uuid = el('td');
        uuid.append(el('code', '', ban.uuid));
        var lift = el('button', 'btn', '解除');
        lift.onclick = function () {
          if (!confirm('解除 ' + (ban.name || ban.uuid) + ' 的封禁？')) return;
          api('/admin/unban', { method: 'POST', body: JSON.stringify({ uuid: ban.uuid }), headers: { 'content-type': 'application/json' } })
            .then(function (r) { if (r.ok) loadBans(); else alert('沒有成功（' + r.status + '）'); });
        };
        var action = el('td');
        action.append(lift);
        row.append(who, uuid, el('td', '', ban.t ? new Date(ban.t).toLocaleString() : '—'), el('td', '', ban.by || '—'), action);
        body.append(row);
      });
    });
  }
  whoami.then(function (me) {
    if (!me || !me.developer) return;
    document.getElementById('gate').hidden = true;
    document.getElementById('panel').hidden = false;
    loadBans();
  });
  document.getElementById('c-send').onclick = function () {
    var msg = document.getElementById('c-msg');
    var entry = {
      section: document.getElementById('c-section').value,
      name: document.getElementById('c-name').value.trim(),
      mc: document.getElementById('c-mc').value.trim()
    };
    var color = document.getElementById('c-color').value.trim();
    if (color) entry.color = color;
    msg.className = 'msg ok';
    msg.textContent = '送出中…';
    api('/admin/credits', { method: 'POST', body: JSON.stringify(entry), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          msg.replaceChildren();
          if (r.ok) {
            msg.className = 'msg ok';
            if (String(body.url).indexOf('https://github.com/') === 0) {
              var link = el('a', '', body.url);
              link.href = body.url;
              msg.append('PR 開好了：', link);
            } else {
              msg.textContent = 'PR 開好了';
            }
          } else {
            msg.className = 'msg bad';
            msg.textContent = CREDIT_WHY[body.why] || '沒有成功（' + (body.why || r.status) + '）';
          }
        });
      }).catch(function () { msg.className = 'msg bad'; msg.textContent = '連不上，等一下再試'; });
  };
` + '</script>');

/** 路徑 → 那一頁的 HTML。 */
export const PAGES = { '/': HOME, '/capes': CAPES, '/account': ACCOUNT, '/admin': ADMIN };
