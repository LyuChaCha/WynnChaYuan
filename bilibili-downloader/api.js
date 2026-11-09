// B站 API 存取。在 www.bilibili.com 頁面中執行，會帶上使用者自己的登入 cookie，
// 因此能取得的畫質與使用者帳號權限相同（不會繞過付費/大會員限制）。
(function (g) {
  const API = 'https://api.bilibili.com';

  const MIXIN_KEY_ENC_TAB = [
    46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39,
    12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63,
    57, 62, 11, 36, 20, 34, 44, 52,
  ];

  async function getJson(url) {
    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }

  let mixinKeyCache = null;
  async function getMixinKey() {
    if (mixinKeyCache) return mixinKeyCache;
    const nav = await getJson(`${API}/x/web-interface/nav`);
    const img = nav?.data?.wbi_img;
    if (!img) throw new Error('取得 WBI 金鑰失敗');
    const name = (u) => u.slice(u.lastIndexOf('/') + 1).split('.')[0];
    const raw = name(img.img_url) + name(img.sub_url);
    mixinKeyCache = MIXIN_KEY_ENC_TAB.map((i) => raw[i]).join('').slice(0, 32);
    return mixinKeyCache;
  }

  function signWbi(params, mixinKey) {
    const p = { ...params, wts: Math.round(Date.now() / 1000) };
    const query = Object.keys(p)
      .sort()
      .map((k) => `${encodeURIComponent(k)}=${encodeURIComponent(String(p[k]).replace(/[!'()*]/g, ''))}`)
      .join('&');
    return `${query}&w_rid=${g.bdlMd5(query + mixinKey)}`;
  }

  /** 從網址解析 bvid / aid 與分P */
  function parseVideoUrl(href) {
    const u = new URL(href);
    const bv = u.pathname.match(/\/video\/(BV[0-9A-Za-z]{10})/);
    const av = u.pathname.match(/\/video\/av(\d+)/i);
    const p = parseInt(u.searchParams.get('p') || '1', 10) || 1;
    if (bv) return { bvid: bv[1], p };
    if (av) return { aid: av[1], p };
    return null;
  }

  /** 影片資訊：標題、分P列表 */
  async function getVideoInfo({ bvid, aid }) {
    const q = bvid ? `bvid=${bvid}` : `aid=${aid}`;
    const j = await getJson(`${API}/x/web-interface/view?${q}`);
    if (j.code !== 0) throw new Error(j.message || `view API 錯誤 ${j.code}`);
    const d = j.data;
    return {
      bvid: d.bvid,
      aid: d.aid,
      title: d.title,
      owner: d.owner?.name,
      pages: (d.pages || []).map((pg) => ({ page: pg.page, cid: pg.cid, part: pg.part, duration: pg.duration })),
    };
  }

  /** 取得播放地址。先試 WBI 簽名介面，失敗時改用舊介面。 */
  async function getPlayUrl({ bvid, cid, qn = 127, fnval = 4048 }) {
    const params = { bvid, cid, qn, fnval, fnver: 0, fourk: 1 };
    let lastErr;
    try {
      const key = await getMixinKey();
      const j = await getJson(`${API}/x/player/wbi/playurl?${signWbi(params, key)}`);
      if (j.code === 0) return j.data;
      lastErr = new Error(j.message || `playurl 錯誤 ${j.code}`);
    } catch (e) {
      lastErr = e;
    }
    const qs = new URLSearchParams(Object.entries(params).map(([k, v]) => [k, String(v)]));
    const j = await getJson(`${API}/x/player/playurl?${qs}`);
    if (j.code === 0) return j.data;
    throw new Error(j.message || lastErr?.message || `playurl 錯誤 ${j.code}`);
  }

  g.bdlApi = { parseVideoUrl, getVideoInfo, getPlayUrl, signWbi };
})(typeof globalThis !== 'undefined' ? globalThis : this);
