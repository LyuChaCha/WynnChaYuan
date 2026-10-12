/**
 * 披風一覽的網頁。一張靜態的頁面：跟 /capes 要清單，圖直接用 /cape/<代碼>。
 *
 * - 每張卡片是披風的正面（從範本裡裁出來的那一塊），GIF 會動。
 * - 點一張卡片，開一個可以拖曳旋轉的立體檢視：一個人偶穿著那件披風。
 *   立體的部分用 skinview3d（從 jsDelivr 載入）；載不到的話就只看得到平面的圖。
 * - 網址帶著管理憑證（#k=…，開發者從遊戲裡開）的話，多出刪除的按鈕。
 *
 * 頁面裡的程式刻意不用樣板字串——整頁是包在 worker 的一個樣板字串裡送出去的。
 */
export const GALLERY = String.raw`<!doctype html>
<html lang="zh-Hant">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="dark">
<title>WynnChaYuan 披風一覽</title>
<style>
  :root {
    --ground: #0f1512;
    --panel: #161e19;
    --panel-2: #1c2620;
    --line: #2a382f;
    --ink: #e9efe9;
    --ink-2: #a3b3a7;
    --leaf: #7fd08a;
    --leaf-deep: #2f7d46;
    --gold: #d8b765;
    --danger: #e08a8a;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0; background: var(--ground); color: var(--ink);
    font: 15px/1.65 "Segoe UI", "Microsoft JhengHei", "PingFang TC", "Noto Sans TC", system-ui, sans-serif;
    background-image: radial-gradient(1100px 520px at 50% -160px, #1d3324 0%, transparent 70%);
    background-repeat: no-repeat;
  }
  main { max-width: 1120px; margin: 0 auto; padding: 44px 20px 72px; }
  header { display: flex; align-items: center; gap: 16px; }
  header svg { flex: none; width: 46px; height: 46px; }
  h1 { margin: 0; font-size: 26px; font-weight: 650; letter-spacing: .02em; }
  .sub { margin: 2px 0 0; color: var(--ink-2); font-size: 14px; }
  .bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 18px; margin: 26px 0 14px; }
  .count { font-variant-numeric: tabular-nums; color: var(--ink-2); }
  .count b { color: var(--ink); font-weight: 600; }
  .mod { color: var(--leaf); font-size: 14px; }
  .rules {
    margin: 0 0 26px; padding: 12px 16px; border: 1px solid var(--line); border-radius: 10px;
    background: var(--panel); color: var(--ink-2); font-size: 14px;
  }
  .rules b { color: var(--gold); font-weight: 600; }
  .grid { display: grid; gap: 18px; grid-template-columns: repeat(auto-fill, minmax(168px, 1fr)); }
  .card {
    display: flex; flex-direction: column; padding: 0; text-align: left; color: inherit; font: inherit;
    background: var(--panel); border: 1px solid var(--line); border-radius: 12px; overflow: hidden;
    cursor: pointer; transition: transform .16s ease, border-color .16s ease, box-shadow .16s ease;
  }
  .card:hover { transform: translateY(-3px); border-color: var(--leaf-deep); box-shadow: 0 10px 26px #0008; }
  .card:focus-visible { outline: 2px solid var(--leaf); outline-offset: 2px; }
  .stage {
    display: grid; place-items: center; padding: 20px 0 16px;
    background: linear-gradient(180deg, #1b2a21 0%, #131b16 100%);
  }
  .face {
    width: 96px; aspect-ratio: 10 / 16; overflow: hidden; border-radius: 3px;
    box-shadow: 0 8px 18px #000a, 0 0 0 1px #0006; background: #0b0f0d;
  }
  .face img { display: block; width: 220%; max-width: none; margin: -10% 0 0 -10%; image-rendering: pixelated; }
  .meta { display: flex; align-items: center; gap: 8px; padding: 10px 12px 12px; font-size: 13px; color: var(--ink-2); }
  .tag {
    padding: 1px 7px; border-radius: 999px; font-size: 11px; font-weight: 600; letter-spacing: .06em;
    color: var(--ground); background: var(--ink-2);
  }
  .tag.gif { background: var(--gold); }
  .meta .who { margin-left: auto; font-variant-numeric: tabular-nums; }
  .empty { grid-column: 1 / -1; padding: 64px 0; text-align: center; color: var(--ink-2); }
  dialog {
    width: min(92vw, 760px); padding: 0; color: var(--ink); background: var(--panel);
    border: 1px solid var(--line); border-radius: 14px; box-shadow: 0 30px 80px #000c;
  }
  dialog::backdrop { background: #070a08cc; }
  .view { display: grid; grid-template-columns: minmax(0, 1fr) 240px; }
  .view canvas#model { display: block; width: 100%; height: 440px; background: linear-gradient(180deg, #1d2e24, #121915); cursor: grab; }
  .view canvas#model:active { cursor: grabbing; }
  .side { padding: 18px; border-left: 1px solid var(--line); display: flex; flex-direction: column; gap: 12px; }
  .side h2 { margin: 0; font-size: 16px; font-weight: 600; }
  .side dl { margin: 0; display: grid; grid-template-columns: auto 1fr; gap: 4px 12px; font-size: 13px; }
  .side dt { color: var(--ink-2); }
  .side dd { margin: 0; font-variant-numeric: tabular-nums; }
  .flat { width: 100%; image-rendering: pixelated; border-radius: 4px; background: #0b0f0d; }
  .hint { color: var(--ink-2); font-size: 12px; }
  .side .spacer { flex: 1; }
  button.act {
    font: inherit; font-size: 13px; padding: 7px 12px; border-radius: 8px; cursor: pointer;
    border: 1px solid var(--line); background: var(--panel-2); color: var(--ink);
  }
  button.act:hover { border-color: var(--leaf-deep); }
  button.act:focus-visible { outline: 2px solid var(--leaf); outline-offset: 2px; }
  button.act.danger { border-color: #5a2b2b; background: #2e1a1a; color: var(--danger); }
  button.act.danger:hover { background: #3d2020; }
  @media (max-width: 640px) {
    .view { grid-template-columns: 1fr; }
    .view canvas#model { height: 320px; }
    .side { border-left: 0; border-top: 1px solid var(--line); }
  }
  @media (prefers-reduced-motion: reduce) { .card { transition: none; } .card:hover { transform: none; } }
</style>
</head>
<body>
<main>
  <header>
    <svg viewBox="0 0 24 24" fill="none" stroke="#7fd08a" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M5 21c.5-4.5 2.5-8 7-10"/><path d="M9 18c6.2 0 10.5-3.3 11-12V4h-4C7 4 4 8 4 13c0 1 0 3 2 5h3z"/>
    </svg>
    <div>
      <h1>披風一覽</h1>
      <p class="sub">WynnChaYuan 玩家上傳的披風。點一件可以轉著看。</p>
    </div>
  </header>
  <div class="bar">
    <span class="count" id="count">讀取中…</span>
    <span class="mod" id="mod" hidden>你有管理權：點開披風可以刪除。</span>
  </div>
  <p class="rules"><b>不可以上傳：</b>政治、不雅或色情、違反法規、種族歧視或仇恨的內容。違規的披風會被移除，上傳的人可能不能再傳。</p>
  <div class="grid" id="grid"></div>
</main>

<dialog id="viewer" aria-labelledby="viewer-title">
  <div class="view">
    <canvas id="model" width="520" height="440"></canvas>
    <div class="side">
      <h2 id="viewer-title">披風</h2>
      <img class="flat" id="flat" alt="披風的範本圖：左邊正面、右邊背面">
      <dl>
        <dt>格式</dt><dd id="d-type"></dd>
        <dt>大小</dt><dd id="d-size"></dd>
        <dt>使用</dt><dd id="d-users"></dd>
        <dt>上傳</dt><dd id="d-date"></dd>
      </dl>
      <p class="hint" id="hint">拖曳可以旋轉。</p>
      <div class="spacer"></div>
      <button class="act danger" id="del" hidden>刪除這件披風</button>
      <button class="act danger" id="ban" hidden>刪除並禁止再傳</button>
      <button class="act" id="close">關閉</button>
    </div>
  </div>
</dialog>

<script src="https://cdn.jsdelivr.net/npm/skinview3d@3.4.1/bundles/skinview3d.bundle.js" defer></script>
<script>
  var key = (location.hash.match(/k=([^&]+)/) || [])[1] || '';
  var grid = document.getElementById('grid');
  var dialog = document.getElementById('viewer');
  var current = null;
  var viewer = null;
  var playing = 0;

  if (key) document.getElementById('mod').hidden = false;

  function size(n) {
    return n >= 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB';
  }
  function url(cape) { return '/cape/' + cape.ids[0] + '?v=' + cape.v; }

  /* 人偶的皮膚：沒有臉的灰色木偶，免得跟披風搶戲。 */
  function mannequin() {
    var c = document.createElement('canvas');
    c.width = 64; c.height = 64;
    var g = c.getContext('2d');
    g.fillStyle = '#8f9a93'; g.fillRect(0, 0, 64, 64);
    g.fillStyle = '#a4aea7'; g.fillRect(0, 0, 32, 16);        /* 頭 */
    g.fillStyle = '#7d8881'; g.fillRect(0, 16, 16, 16); g.fillRect(16, 48, 16, 16);   /* 腿 */
    return c;
  }

  /* 範本（22×17 格）放回 Minecraft 披風貼圖（64×32 格）的左上角。 */
  function capeCanvas(source, width, height) {
    var k = Math.max(1, Math.round(width / 22));
    var c = document.createElement('canvas');
    c.width = 64 * k; c.height = 32 * k;
    var g = c.getContext('2d');
    g.imageSmoothingEnabled = false;
    g.drawImage(source, 0, 0, 22 * k, 17 * k);
    return c;
  }

  function stop() { playing++; }

  /* GIF 一格一格換：瀏覽器有 ImageDecoder 就動，沒有就停在第一格。 */
  function play(cape, image) {
    var mine = ++playing;
    viewer.loadCape(capeCanvas(image, image.naturalWidth, image.naturalHeight));
    if (cape.t !== 'gif' || !('ImageDecoder' in window)) return;
    fetch(url(cape)).then(function (r) { return r.arrayBuffer(); }).then(function (data) {
      var decoder = new ImageDecoder({ data: data, type: 'image/gif' });
      return decoder.tracks.ready.then(function () {
        var count = decoder.tracks.selectedTrack.frameCount;
        var index = 0;
        function next() {
          if (mine !== playing) { decoder.close(); return; }
          decoder.decode({ frameIndex: index }).then(function (result) {
            if (mine !== playing) { result.image.close(); decoder.close(); return; }
            var frame = result.image;
            viewer.loadCape(capeCanvas(frame, frame.displayWidth, frame.displayHeight));
            var wait = Math.max(20, (frame.duration || 100000) / 1000);
            frame.close();
            index = (index + 1) % count;
            setTimeout(next, wait);
          }).catch(function () {});
        }
        next();
      });
    }).catch(function () {});
  }

  function open(cape) {
    current = cape;
    document.getElementById('flat').src = url(cape);
    document.getElementById('d-type').textContent = cape.t.toUpperCase();
    document.getElementById('d-size').textContent = size(cape.s);
    document.getElementById('d-users').textContent = cape.ids.length + ' 人';
    document.getElementById('d-date').textContent = new Date(cape.v).toLocaleDateString();
    document.getElementById('del').hidden = !key;
    document.getElementById('ban').hidden = !key;
    dialog.showModal();
    var hint = document.getElementById('hint');
    if (!window.skinview3d) {
      hint.textContent = '立體檢視載不起來，只能看平面的圖。';
      return;
    }
    hint.textContent = '拖曳可以旋轉。';
    if (!viewer) {
      var canvas = document.getElementById('model');
      viewer = new skinview3d.SkinViewer({ canvas: canvas, width: canvas.clientWidth, height: canvas.clientHeight, skin: mannequin() });
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
    }
    var image = new Image();
    image.onload = function () { if (current === cape) play(cape, image); };
    image.src = url(cape);
  }

  function shut() { stop(); current = null; if (dialog.open) dialog.close(); }
  document.getElementById('close').onclick = shut;
  dialog.addEventListener('close', stop);
  dialog.addEventListener('click', function (e) { if (e.target === dialog) shut(); });

  function remove(ban) {
    var cape = current;
    if (!cape) return;
    var text = ban ? '刪除這件披風，並禁止用它的 ' + cape.ids.length + ' 個人再上傳？'
                   : '刪除這件披風（' + cape.ids.length + ' 個人在用）？';
    if (!confirm(text)) return;
    var body = { hash: cape.h };
    if (ban) body.ban = true;
    fetch('/admin/remove', {
      method: 'POST', body: JSON.stringify(body),
      headers: { authorization: 'Bearer ' + key, 'content-type': 'application/json' }
    }).then(function (r) {
      if (r.ok) { shut(); load(); }
      else alert('沒有成功（' + r.status + '）。管理權兩個小時過期，請從遊戲裡重新開一次。');
    });
  }
  document.getElementById('del').onclick = function () { remove(false); };
  document.getElementById('ban').onclick = function () { remove(true); };

  function load() {
    return fetch('/capes').then(function (r) { return r.json(); }).then(function (data) {
      var count = document.getElementById('count');
      count.textContent = '';
      var b = document.createElement('b');
      b.textContent = data.capes.length;
      count.append('共 ', b, ' 件（相同的圖算一件）');
      grid.replaceChildren();
      if (!data.capes.length) {
        var empty = document.createElement('div');
        empty.className = 'empty';
        empty.textContent = '還沒有人上傳披風。';
        grid.append(empty);
      }
      data.capes.forEach(function (cape) {
        var card = document.createElement('button');
        card.className = 'card';
        card.type = 'button';
        card.setAttribute('aria-label', cape.t.toUpperCase() + ' 披風，' + cape.ids.length + ' 人使用');
        var stage = document.createElement('div');
        stage.className = 'stage';
        var face = document.createElement('div');
        face.className = 'face';
        var img = document.createElement('img');
        img.loading = 'lazy';
        img.alt = '';
        img.src = url(cape);
        face.append(img);
        stage.append(face);
        var meta = document.createElement('div');
        meta.className = 'meta';
        var tag = document.createElement('span');
        tag.className = 'tag ' + cape.t;
        tag.textContent = cape.t.toUpperCase();
        var who = document.createElement('span');
        who.className = 'who';
        who.textContent = cape.ids.length + ' 人';
        meta.append(tag, who);
        card.append(stage, meta);
        card.onclick = function () { open(cape); };
        grid.append(card);
      });
    });
  }
  load().catch(function () { document.getElementById('count').textContent = '讀不到清單，請重新整理。'; });
</script>
</body>
</html>`;
