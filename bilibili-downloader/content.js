// 在 B站影片頁注入「下載」按鈕與面板（Shadow DOM 隔離樣式）。
(function () {
  if (window.__bdlInjected) return;
  window.__bdlInjected = true;
  const { parseVideoUrl, getVideoInfo, getPlayUrl } = window.bdlApi;

  const QN_NAMES = {
    127: '8K', 126: '杜比視界', 125: 'HDR', 120: '4K', 116: '1080P 60幀', 112: '1080P 高碼率',
    80: '1080P', 74: '720P 60幀', 64: '720P', 32: '480P', 16: '360P', 6: '240P',
  };
  const AUDIO_NAMES = { 30216: '64K', 30232: '132K', 30280: '192K', 30250: '杜比全景聲', 30251: 'Hi-Res 無損' };

  const host = document.createElement('div');
  host.id = 'bdl-host';
  const root = host.attachShadow({ mode: 'open' });
  root.innerHTML = `
<style>
  :host { all: initial; }
  * { box-sizing: border-box; font-family: system-ui, "PingFang TC", "Microsoft JhengHei", sans-serif; }
  .fab { position: fixed; right: 20px; bottom: 80px; z-index: 2147483646; padding: 10px 16px; border: 0;
    border-radius: 999px; background: #fb7299; color: #fff; font-size: 14px; cursor: pointer;
    box-shadow: 0 4px 12px rgba(0,0,0,.2); }
  .fab:hover { background: #fc8bab; }
  .panel { position: fixed; right: 20px; bottom: 130px; z-index: 2147483647; width: 380px; max-height: 70vh;
    overflow: auto; background: #fff; color: #18191c; border-radius: 12px; padding: 14px;
    box-shadow: 0 8px 30px rgba(0,0,0,.25); font-size: 13px; display: none; }
  .panel.open { display: block; }
  .head { display: flex; justify-content: space-between; align-items: start; gap: 8px; }
  .title { font-weight: 600; font-size: 14px; line-height: 1.4; word-break: break-all; }
  .x { border: 0; background: none; font-size: 18px; cursor: pointer; color: #999; }
  h3 { font-size: 13px; margin: 14px 0 6px; color: #61666d; }
  select { width: 100%; padding: 6px; border: 1px solid #e3e5e7; border-radius: 6px; font-size: 13px; }
  .row { display: flex; justify-content: space-between; align-items: center; gap: 8px; padding: 6px 0;
    border-bottom: 1px solid #f1f2f3; }
  .row small { color: #9499a0; }
  button.dl { flex: none; padding: 4px 10px; border: 1px solid #fb7299; background: #fff; color: #fb7299;
    border-radius: 6px; cursor: pointer; font-size: 12px; }
  button.dl:hover { background: #fb7299; color: #fff; }
  button.dl:disabled { opacity: .7; cursor: default; min-width: 52px; }
  .msg { margin-top: 10px; color: #61666d; font-size: 12px; }
  .err { color: #e5484d; }
  code { display: block; background: #f6f7f8; padding: 6px; border-radius: 6px; font-size: 11px;
    word-break: break-all; font-family: ui-monospace, monospace; margin-top: 4px; user-select: all; }
</style>
<button class="fab" title="下載這部影片">⬇ 下載</button>
<div class="panel">
  <div class="head"><div class="title"></div><button class="x" title="關閉">×</button></div>
  <div class="body"></div>
  <div class="msg"></div>
</div>`;
  document.documentElement.appendChild(host);

  const $ = (s) => root.querySelector(s);
  const panel = $('.panel');
  const body = $('.body');
  const msg = $('.msg');
  const el = (tag, props = {}, ...kids) => {
    const n = Object.assign(document.createElement(tag), props);
    n.append(...kids);
    return n;
  };
  const setMsg = (text, isErr = false) => {
    msg.textContent = text;
    msg.className = 'msg' + (isErr ? ' err' : '');
  };

  function safeName(s) {
    return (s || 'video').replace(/[\\/:*?"<>|~\u0000-\u001f]/g, '_').replace(/^[.\s]+|[.\s]+$/g, '').slice(0, 120) || 'video';
  }

  // 優先選非 PCDN（mcdn）的網址，較穩定
  function pickUrl(item) {
    const all = [item.baseUrl || item.base_url, ...(item.backupUrl || item.backup_url || [])].filter(Boolean);
    return all.find((u) => !/mcdn\.bilivideo|:\d{4,5}\//.test(u)) || all[0];
  }

  function fmtSize(bytes) {
    if (!bytes) return '';
    return bytes > 1 << 30 ? (bytes / (1 << 30)).toFixed(2) + ' GB' : (bytes / (1 << 20)).toFixed(1) + ' MB';
  }

  function codecName(c = '') {
    if (c.startsWith('avc')) return 'H.264';
    if (c.startsWith('hev') || c.startsWith('hvc')) return 'H.265';
    if (c.startsWith('av01')) return 'AV1';
    return c;
  }

  // 在頁面內 fetch：請求自動帶上 bilibili.com 的 Referer（CDN 需要），
  // 下載完後轉成 Blob 存檔。
  async function download(url, filename, btn) {
    const label = btn.textContent;
    btn.disabled = true;
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const total = +res.headers.get('content-length') || 0;
      const reader = res.body.getReader();
      const chunks = [];
      let got = 0;
      for (;;) {
        const { done, value } = await reader.read();
        if (done) break;
        chunks.push(value);
        got += value.length;
        btn.textContent = total ? Math.floor((got / total) * 100) + '%' : fmtSize(got);
      }
      const blobUrl = URL.createObjectURL(new Blob(chunks, { type: 'application/octet-stream' }));
      const a = el('a', { href: blobUrl, download: filename });
      root.append(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000);
      setMsg('完成：' + filename);
      btn.textContent = '✓';
    } catch (e) {
      setMsg('下載失敗：' + e.message, true);
      btn.textContent = label;
    } finally {
      btn.disabled = false;
    }
  }

  function dlButton(label, url, filename) {
    const b = el('button', { className: 'dl', textContent: label });
    b.onclick = () => {
      setMsg('下載中：' + filename);
      download(url, filename, b);
    };
    return b;
  }

  let info = null;

  async function renderPage(pageNo) {
    const pg = info.pages.find((x) => x.page === pageNo) || info.pages[0];
    const base = safeName(info.pages.length > 1 ? `${info.title} P${pg.page} ${pg.part}` : info.title);
    const content = el('div');
    body.querySelector('.streams')?.remove();
    content.className = 'streams';
    body.append(content);
    setMsg('讀取播放地址中…');

    const [dash, mp4] = await Promise.all([
      getPlayUrl({ bvid: info.bvid, cid: pg.cid, qn: 127, fnval: 4048 }),
      getPlayUrl({ bvid: info.bvid, cid: pg.cid, qn: 80, fnval: 1 }).catch(() => null),
    ]);

    // 1) 有聲音的完整 MP4（畫質通常最高 720P/1080P）
    const durl = mp4?.durl?.[0];
    if (durl) {
      content.append(el('h3', { textContent: '完整 MP4（含聲音，直接可播）' }));
      const q = QN_NAMES[mp4.quality] || mp4.quality;
      content.append(
        el('div', { className: 'row' },
          el('span', {}, `${q} `, el('small', { textContent: fmtSize(durl.size) })),
          dlButton('下載', pickUrl({ baseUrl: durl.url, backupUrl: durl.backup_url }), `${base}.mp4`))
      );
    }

    // 2) DASH：影像與音訊分開，可選最高畫質
    const d = dash?.dash;
    if (d) {
      content.append(el('h3', { textContent: '高畫質（影像、音訊分開下載）' }));
      const videos = [...(d.video || [])].sort((a, b) => b.id - a.id || b.bandwidth - a.bandwidth);
      for (const v of videos) {
        const q = QN_NAMES[v.id] || v.id;
        content.append(
          el('div', { className: 'row' },
            el('span', {}, `${q} `, el('small', { textContent: `${codecName(v.codecs)} · ${v.width}×${v.height}` })),
            dlButton('影像', pickUrl(v), `${base} [${q} ${codecName(v.codecs)}].video.mp4`))
        );
      }
      const audios = [...(d.audio || [])];
      if (d.dolby?.audio) audios.push(...d.dolby.audio);
      if (d.flac?.audio) audios.push(d.flac.audio);
      audios.sort((a, b) => b.bandwidth - a.bandwidth);
      for (const a of audios) {
        const q = AUDIO_NAMES[a.id] || `${Math.round(a.bandwidth / 1000)}K`;
        const ext = a.id === 30251 ? 'flac.m4a' : 'm4a';
        content.append(
          el('div', { className: 'row' },
            el('span', {}, `音訊 ${q}`),
            dlButton('音訊', pickUrl(a), `${base} [${q}].audio.${ext}`))
        );
      }
      content.append(
        el('div', { className: 'msg' },
          '分開下載後，可用 ffmpeg 合併（不重新編碼，幾秒完成）：',
          el('code', { textContent: `ffmpeg -i "影像.video.mp4" -i "音訊.audio.m4a" -c copy "${base}.mp4"` }))
      );
    }

    if (!durl && !d) throw new Error('沒有可用的播放地址（可能需要登入或大會員）');
    const vip = dash?.accept_quality?.filter((x) => !d?.video?.some((v) => v.id === x)) || [];
    setMsg(vip.length ? `部分畫質（${vip.map((x) => QN_NAMES[x] || x).join('、')}）需登入或大會員才能取得。` : '');
  }

  async function open() {
    panel.classList.add('open');
    body.textContent = '';
    $('.title').textContent = '讀取中…';
    setMsg('');
    try {
      const parsed = parseVideoUrl(location.href); // B站是單頁應用，每次開啟都重新讀網址
      if (!parsed) throw new Error('無法辨識影片網址');
      info = await getVideoInfo(parsed);
      $('.title').textContent = info.title;
      if (info.pages.length > 1) {
        body.append(el('h3', { textContent: '選擇分P' }));
        const sel = el('select');
        for (const pg of info.pages) sel.append(el('option', { value: pg.page, textContent: `P${pg.page} ${pg.part}` }));
        sel.value = String(parsed.p);
        sel.onchange = () => renderPage(+sel.value).catch((e) => setMsg(e.message, true));
        body.append(sel);
      }
      await renderPage(parsed.p);
    } catch (e) {
      setMsg('錯誤：' + e.message, true);
    }
  }

  $('.fab').onclick = () => (panel.classList.contains('open') ? panel.classList.remove('open') : open());
  $('.x').onclick = () => panel.classList.remove('open');
  chrome.runtime.onMessage.addListener((m) => {
    if (m?.type === 'openPanel') open();
  });
})();
