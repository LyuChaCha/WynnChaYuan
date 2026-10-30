import { CAPE_TEMPLATES } from './cape-templates.js';
import { CURSEFORGE, ICON, ISSUES, KOFI, LEAF, RELEASES, REPO } from './site-constants.js';
import { LANGS, LANG_NAMES, tagOf } from './site-strings.js';
import { FONT_CSS, STYLE, TEX_CSS } from './site-styles.js';
import { jsonData, readJsonData, scriptFile } from './site-scripts.js';

const COMMON = String.raw`
  var TOKEN_KEY = 'wcy.token';
  (function () {
    var found = location.hash.match(/t=([^&]+)/);
    if (!found) return;
    try { localStorage.setItem(TOKEN_KEY, found[1]); } catch (e) {}
    history.replaceState(null, '', location.pathname);
  })();
  function revealOnScroll(selector) {
    if (!('IntersectionObserver' in window) || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    var fold = window.innerHeight * 0.92;
    var nodes = Array.prototype.filter.call(document.querySelectorAll(selector), function (node) {
      return node.getBoundingClientRect().top > fold;
    });
    var watch = new IntersectionObserver(function (entries) {
      var arriving = entries.filter(function (entry) { return entry.isIntersecting; }).map(function (entry) { return entry.target; });
      arriving.sort(function (a, b) {
        var p = a.getBoundingClientRect(), q = b.getBoundingClientRect();
        return (p.top - q.top) || (p.left - q.left);
      });
      arriving.forEach(function (node, i) {
        watch.unobserve(node);
        node.style.setProperty('--d', Math.min(i * 70, 420) + 'ms');
        node.classList.add('rv-on');
        setTimeout(function () {
          node.classList.remove('rv', 'rv-on');
          node.style.removeProperty('--d');
        }, 1200 + Math.min(i * 70, 420));
      });
    }, { rootMargin: '0px 0px -8% 0px' });
    nodes.forEach(function (node) {
      node.classList.add('rv');
      watch.observe(node);
    });
  }
  function token() { try { return localStorage.getItem(TOKEN_KEY) || ''; } catch (e) { return ''; } }
  function signOut() { try { localStorage.removeItem(TOKEN_KEY); } catch (e) {} location.reload(); }
  function api(path, options) {
    options = options || {};
    options.headers = options.headers || {};
    if (token()) options.headers.authorization = 'Bearer ' + token();
    return fetch('/api' + path, options);
  }
  /* 用 minotar：有些帳號在別家只畫得出預設的 Steve */
  function head(name, size) { return 'https://minotar.net/helm/' + encodeURIComponent(name) + '/' + (size || 32) + '.png'; }
  function figure(name, width) { return 'https://minotar.net/armor/body/' + encodeURIComponent(name) + '/' + width + '.png'; }
  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }
  function fmt(text, values) {
    return text.replace(/\{(\w+)\}/g, function (all, key) { return key in values ? values[key] : all; });
  }
  function bytes(n) { return n >= 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB'; }
  var calm = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  function flash(node) {
    if (!node.animate) return;
    node.animate(calm ? [{ opacity: 0 }, { opacity: 1 }]
      : [{ opacity: 0, transform: 'translateY(4px)' }, { opacity: 1, transform: 'none' }],
      { duration: 200, easing: 'cubic-bezier(0.23, 1, 0.32, 1)' });
  }
  function slide(seg) {
    var on = seg.querySelector('button[aria-pressed="true"]:not([hidden])');
    var pill = seg.querySelector('.pill');
    var fresh = !pill;
    if (fresh) {
      pill = el('i', 'pill');
      seg.prepend(pill);
      seg.classList.add('live');
    }
    if (!on || !on.offsetWidth) { pill.style.opacity = '0'; return; }
    if (fresh || pill.style.opacity === '0') pill.style.transition = 'none';
    pill.style.opacity = '1';
    pill.style.width = on.offsetWidth + 'px';
    pill.style.height = on.offsetHeight + 'px';
    pill.style.transform = 'translate(' + on.offsetLeft + 'px, ' + on.offsetTop + 'px)';
    if (pill.style.transition === 'none') {
      pill.getBoundingClientRect();        /* 先讓瀏覽器算好位置再開過場，不然會從左上角滑進來 */
      pill.style.transition = '';
    }
  }
  function slideAll() { Array.prototype.forEach.call(document.querySelectorAll('.seg'), slide); }
  document.addEventListener('click', function (e) {
    var seg = e.target.closest && e.target.closest('.seg');
    if (seg) setTimeout(function () { slide(seg); }, 0);
  });
  window.addEventListener('resize', slideAll);
  window.addEventListener('load', slideAll);
  function blip(button, text) {
    if (button.dataset.label) return;
    button.dataset.label = button.textContent;
    button.style.minWidth = button.offsetWidth + 'px';
    button.textContent = text;
    setTimeout(function () {
      button.textContent = button.dataset.label;
      delete button.dataset.label;
      button.style.minWidth = '';
    }, 1400);
  }
  var menus = (function () {
    var open = null;
    function entries(menu) {
      return Array.prototype.filter.call(menu.querySelectorAll('[role^="menuitem"]'), function (node) {
        return node.getClientRects().length > 0;
      });
    }
    function close(back) {
      if (!open) return;
      var was = open;
      open = null;
      was.menu.classList.remove('open');
      was.trigger.setAttribute('aria-expanded', 'false');
      if (back) was.trigger.focus();
    }
    function show(trigger, menu, focus) {
      if (!open || open.trigger !== trigger) {
        close(false);
        menu.classList.add('open');
        trigger.setAttribute('aria-expanded', 'true');
        open = { trigger: trigger, menu: menu };
      }
      if (!focus) return;
      var list = entries(menu);
      var pick = focus === 'last' ? list[list.length - 1] : list[0];
      if (pick) pick.focus();
    }
    function bind(trigger) {
      var menu = document.getElementById(trigger.getAttribute('aria-controls'));
      if (!menu) return;
      var listy = menu.getAttribute('role') === 'menu';
      if (listy) {
        Array.prototype.forEach.call(menu.querySelectorAll('a:not([role]), button:not([role])'), function (node) {
          node.setAttribute('role', 'menuitem');
        });
      }
      number(menu);
      trigger.addEventListener('click', function (e) {
        if (open && open.trigger === trigger) close(false);
        else show(trigger, menu, listy && e.detail === 0 ? 'first' : null);
      });
      if (!listy) return;
      trigger.addEventListener('keydown', function (e) {
        if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;
        e.preventDefault();
        show(trigger, menu, e.key === 'ArrowUp' ? 'last' : 'first');
      });
      menu.addEventListener('keydown', function (e) {
        var list = entries(menu);
        var at = list.indexOf(document.activeElement);
        var to = e.key === 'ArrowDown' ? at + 1 : e.key === 'ArrowUp' ? (at < 0 ? list.length - 1 : at - 1)
          : e.key === 'Home' ? 0 : e.key === 'End' ? list.length - 1 : null;
        if (to === null || !list.length) return;
        e.preventDefault();
        list[(to + list.length) % list.length].focus();
      });
    }
    function number(menu) {
      Array.prototype.forEach.call(menu.children, function (child, i) { child.style.setProperty('--i', Math.min(i, 10)); });
    }
    function outside(target) { return open && !open.trigger.contains(target) && !open.menu.contains(target); }
    document.addEventListener('click', function (e) {
      if (outside(e.target)) close(false);
      else if (open && open.menu.contains(e.target) && e.target.closest('a')) close(false);
    });
    document.addEventListener('focusin', function (e) { if (outside(e.target)) close(false); });
    document.addEventListener('keydown', function (e) {
      if (e.key !== 'Escape' || !open) return;
      e.preventDefault();
      close(true);
    });
    Array.prototype.forEach.call(document.querySelectorAll('.top [aria-haspopup="true"], #hn-burger'), bind);
    return {
      close: close,
      number: number,
      closeIn: function (box) { if (open && box.contains(open.trigger)) close(false); },
    };
  })();
  if (window.matchMedia) {
    var phone = window.matchMedia('(max-width: 720px)');
    var folded = function () { menus.close(false); };
    if (phone.addEventListener) phone.addEventListener('change', folded);
    else if (phone.addListener) phone.addListener(folded);
  }
  document.getElementById('hn-lang-toggle').addEventListener('click', function () {
    var on = this.getAttribute('aria-expanded') !== 'true';
    this.setAttribute('aria-expanded', on ? 'true' : 'false');
    document.getElementById(this.getAttribute('aria-controls')).hidden = !on;
  });
  document.addEventListener('click', function (e) {
    var link = e.target.closest && e.target.closest('a[data-lang]');
    if (!link || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey || e.button) return;
    e.preventDefault();
    document.cookie = 'lang=' + link.dataset.lang + '; path=/; max-age=31536000; samesite=lax';
    location.replace(location.pathname);
  });
  var fitNav = (function () {
    var nav = document.getElementById('hn');
    var list = nav.querySelector('.hn-list');
    var more = document.getElementById('hn-more');
    var moreBtn = document.getElementById('hn-more-btn');
    var moreMenu = document.getElementById('hn-more-menu');
    var last = null;
    function fit(force) {
      var cells = Array.prototype.filter.call(list.children, function (li) { return li !== more; });
      cells.forEach(function (li) { li.classList.remove('hn-out'); });
      more.hidden = false;
      var room = nav.clientWidth;
      var gap = parseFloat(getComputedStyle(list).columnGap) || 0;
      var size = function (li) { return li.getBoundingClientRect().width + gap; };
      var spare = size(more);
      var live = cells.filter(function (li) { return !li.hidden; });
      var need = live.reduce(function (sum, li) { return sum + size(li); }, -gap);
      var out = [];
      if (room > 0 && need > room + 0.5) {
        room -= spare;
        live.map(function (li, i) { return { li: li, i: i }; })
          .sort(function (a, b) { return (a.li.dataset.pri - b.li.dataset.pri) || (b.i - a.i); })
          .some(function (one) {
            if (need <= room + 0.5) return true;
            need -= size(one.li);
            out.push(one.li);
            return false;
          });
      }
      more.hidden = out.length === 0;
      out.forEach(function (li) { li.classList.add('hn-out'); });
      nav.classList.add('hn-live');
      var sig = out.map(function (li) { return li.dataset.key; }).join();
      if (sig === last && !force) return;
      last = sig;
      menus.closeIn(nav);
      moreMenu.replaceChildren();
      moreBtn.classList.remove('hn-cur');
      live.forEach(function (li) {
        if (out.indexOf(li) < 0) return;
        var trig = li.querySelector('.hn-trig');
        if (trig) {
          var headline = el('p', 'hn-mhead', trig.textContent);
          headline.setAttribute('role', 'none');
          moreMenu.append(headline);
        }
        Array.prototype.forEach.call(trig ? li.querySelectorAll('.hn-menu a') : [li.querySelector('a')], function (a) {
          var copy = a.cloneNode(true);
          copy.className = trig ? 'hn-msub' : '';
          copy.setAttribute('role', 'menuitem');
          moreMenu.append(copy);
          if (a.getAttribute('aria-current') === 'page' && !a.hidden) moreBtn.classList.add('hn-cur');
        });
      });
      menus.number(moreMenu);
    }
    fit();
    if (window.ResizeObserver) new ResizeObserver(function () { fit(); }).observe(nav);
    else window.addEventListener('resize', function () { fit(); });
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(function () { fit(); });
    return fit;
  })();
  /* Discord 沒有用名字開個人頁的網址：按一下複製名字 */
  document.getElementById('dc-copy').onclick = function () {
    var button = this;
    if (!navigator.clipboard) return;
    navigator.clipboard.writeText('LyuChaCha').then(function () { blip(button, L.copied); });
  };
  var whoami = !token() ? Promise.resolve(null) : api('/me').then(function (r) {
    if (r.status === 401) { try { localStorage.removeItem(TOKEN_KEY); } catch (e) {} return null; }
    return r.ok ? r.json() : null;
  }).catch(function () { return null; });
  function headerAccount(me) {
    var on = Boolean(me);
    ['hn-guest', 'hn-p-guest'].forEach(function (id) { document.getElementById(id).hidden = on; });
    ['hn-user', 'hn-p-user'].forEach(function (id) { document.getElementById(id).hidden = !on; });
    Array.prototype.forEach.call(document.querySelectorAll('.top [data-need]'), function (node) {
      node.hidden = !(me && me !== true && me[node.dataset.need]);
    });
    if (me && me !== true) {
      var name = me.linked === false ? ((me.discord && me.discord.name) || 'Discord') : me.name;
      Array.prototype.forEach.call(document.querySelectorAll('.top [data-me-name]'), function (node) { node.textContent = name; });
      var avatar = document.getElementById('hn-avatar');
      avatar.setAttribute('aria-label', avatar.dataset.label + ': ' + name);
      avatar.title = name;
      if (me.linked !== false) {
        Array.prototype.forEach.call(document.querySelectorAll('.top img.hn-ava'), function (img) {
          img.onload = function () { img.hidden = false; img.nextElementSibling.hidden = true; };
          img.src = head(me.name, 52);
        });
      }
    }
    fitNav(true);
  }
  Array.prototype.forEach.call(document.querySelectorAll('[data-signout]'), function (button) { button.onclick = signOut; });
  if (token()) headerAccount(true);
  whoami.then(headerAccount);
  (function () {
    var canvas = document.getElementById('hn-leaves');
    if (!canvas || !canvas.getContext) return;
    var ctx = canvas.getContext('2d');
    var calmQuery = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : null;
    function off() { return Boolean(calmQuery && calmQuery.matches); }
    var PX = 2;
    var UP = {
      full: ['..o..', '.oLo.', 'oLLGo', 'oLVGo', 'oGVGo', '.oVo.', '..s..'],
      half: ['..o..', '.oLo.', '.oVo.', '.oVo.', '.oVo.', '..o..', '..s..'],
      edge: ['.....', '..o..', '..o..', '..o..', '..o..', '..o..', '..s..'],
    };
    var DIAG = {
      full: ['....oo', '...oLo', '..oLVo', '.oLVGo', '.oVGo.', 'so....'],
      half: ['.....o', '....oo', '...oVo', '..oVo.', '.oVo..', 'so....'],
      edge: ['......', '....o.', '...o..', '..o...', '.o....', 's.....'],
    };
    var SETS = [
      { o: [62, 104, 40], G: [110, 166, 66], L: [160, 206, 96], V: [80, 130, 50], s: [112, 86, 54] },
      { o: [44, 110, 64], G: [79, 160, 96], L: [127, 208, 138], V: [52, 124, 72], s: [112, 86, 54] },
      { o: [40, 98, 70], G: [70, 150, 100], L: [118, 196, 140], V: [50, 118, 80], s: [112, 86, 54] },
    ];
    var FLIP = [['full', false], ['half', false], ['edge', false], ['half', true], ['full', true]];
    var sprites = {};
    function rgb(c) { return 'rgb(' + c[0] + ',' + c[1] + ',' + c[2] + ')'; }
    function dim(c) { return [Math.round(c[0] * 0.7), Math.round(c[1] * 0.7), Math.round(c[2] * 0.7)]; }
    function sprite(set, shape, back, idx) {
      var key = set + shape + back + idx;
      if (sprites[key]) return sprites[key];
      var rows = (idx % 2 ? DIAG : UP)[shape];
      var h = rows.length, w = rows[0].length;
      var pts = [];
      rows.forEach(function (row, y) {
        for (var x = 0; x < w; x++) if (row.charAt(x) !== '.') pts.push([x, y, row.charAt(x)]);
      });
      var c = SETS[set];
      var paint = back ? { o: c.o, G: dim(c.G), L: dim(c.G), V: dim(c.V), s: c.s } : c;
      if (back) {
        pts = pts.map(function (p) { return idx % 2 ? [w - 1 - p[1], h - 1 - p[0], p[2]] : [w - 1 - p[0], p[1], p[2]]; });
      }
      for (var turn = 0; turn < (idx >> 1); turn++) {
        var hh = h;
        pts = pts.map(function (p) { return [hh - 1 - p[1], p[0], p[2]]; });
        h = w;
        w = hh;
      }
      var img = document.createElement('canvas');
      img.width = w;
      img.height = h;
      var g = img.getContext('2d');
      pts.forEach(function (p) {
        g.fillStyle = rgb(paint[p[2]]);
        g.fillRect(p[0], p[1], 1, 1);
      });
      sprites[key] = img;
      return img;
    }
    var W = 0, H = 0, leaves = [], gusts = [], raf = 0, timer = 0, last = 0;
    function size() {
      var bar = canvas.parentNode.querySelector('.in');
      /* 整數倍才不會一列寬一列窄；寬度往下取整，免得撐出橫向捲動 */
      W = Math.floor(canvas.parentNode.clientWidth / PX);
      H = Math.floor((bar ? bar.offsetHeight : 60) / PX);
      canvas.style.width = W * PX + 'px';
      canvas.style.height = H * PX + 'px';
      canvas.width = W;
      canvas.height = H;
    }
    function wind(x) {
      var total = 0;
      gusts.forEach(function (g) {
        var d = g.x - x;
        if (d < -12) return;
        var rise = Math.min(1, (d + 12) / 20);
        var fall = Math.exp(-Math.max(0, d - 8) / 70);
        total = Math.max(total, rise * fall * g.k);
      });
      return total;
    }
    function rand(a, b) { return a + Math.random() * (b - a); }
    function addLeaf(fromTop) {
      if (leaves.length >= 6) return;
      leaves.push({
        x: fromTop ? rand(W * 0.05, W * 0.55) : rand(-26, -8), y0: fromTop ? -8 : rand(3, Math.max(4, H - 10)),
        base: rand(9, 16), fall: fromTop ? rand(3, 5) : rand(-0.4, 0.9), amp: rand(1.2, 2.6), f: rand(0.35, 0.6),
        ph: rand(0, 6.3), t: 0, set: Math.floor(Math.random() * SETS.length), dir: Math.random() < 0.5 ? 1 : -1,
        o0: Math.floor(Math.random() * 8), off: Math.random(), flip: 0, g: 0,
      });
    }
    function blow() {
      if (document.hidden || off() || !W) return;
      var k = rand(0.65, 1);
      gusts.push({ x: -30, v: rand(150, 190), k: k, wisps: [0, 1].map(function () {
        return { dy: rand(4, Math.max(5, H - 4)), lag: rand(0, 26), len: rand(28, 52), amp: rand(0.8, 1.8), ph: rand(0, 6.3) };
      }) });
      addLeaf(Math.random() < 0.3);
      if (Math.random() < 0.45) addLeaf(false);
      wake();
    }
    function step(dt) {
      gusts.forEach(function (g) { g.x += g.v * dt; });
      gusts = gusts.filter(function (g) { return g.x - 90 < W + 40; });
      leaves.forEach(function (l) {
        l.g = wind(l.x);
        l.t += dt;
        l.x += (l.base + 52 * l.g) * dt;
        l.flip += (0.5 + 3.6 * l.g) * dt;
      });
      leaves = leaves.filter(function (l) { return l.x < W + 12 && l.y0 + l.fall * l.t < H + 12; });
    }
    function draw() {
      ctx.clearRect(0, 0, W, H);
      ctx.fillStyle = '#e8f8de';
      gusts.forEach(function (g) {
        var k = (g.x + 30) / (W + 160);
        var fade = Math.max(0, Math.min(1, k / 0.12, (1 - k) / 0.18));
        g.wisps.forEach(function (w) {
          var head = g.x - w.lag;
          for (var i = 0; i < w.len; i += 1) {
            var a = 0.2 * fade * Math.pow(1 - i / w.len, 1.5);
            if (a < 0.02) continue;
            ctx.globalAlpha = a;
            ctx.fillRect(Math.floor(head - i), Math.floor(w.dy + w.amp * Math.sin((head - i) / 13 + w.ph)), 1, 1);
          }
        });
      });
      ctx.globalAlpha = 1;
      leaves.forEach(function (l) {
        var spin = l.flip + l.off;
        var turn = Math.floor(spin), frac = spin - turn;
        var stage = frac < 0.66 ? 0 : Math.min(4, Math.floor((frac - 0.66) / 0.34 * 5));
        var back = (turn % 2 === 1) !== FLIP[stage][1];
        var idx = (((l.o0 + l.dir * turn) % 8) + 8) % 8;
        var img = sprite(l.set, FLIP[stage][0], back, idx);
        var swing = Math.sin(2 * Math.PI * 2 * l.f * l.t + l.ph);
        var x = l.x + 2 * swing;
        var y = l.y0 + l.fall * l.t + l.amp * Math.sin(2 * Math.PI * l.f * l.t + l.ph) + Math.abs(swing) - 3 * l.g;
        ctx.drawImage(img, Math.round(x - img.width / 2), Math.round(y - img.height / 2));
      });
      ctx.globalAlpha = 1;
    }
    function frame(now) {
      var dt = Math.min(0.05, Math.max(0, (now - last) / 1000));
      last = now;
      step(dt);
      draw();
      raf = leaves.length || gusts.length ? requestAnimationFrame(frame) : 0;
      if (!raf) ctx.clearRect(0, 0, W, H);
    }
    function wake() {
      if (raf || document.hidden) return;
      last = performance.now();
      raf = requestAnimationFrame(frame);
    }
    function schedule(ms) {
      clearTimeout(timer);
      timer = setTimeout(function () {
        blow();
        schedule(rand(5500, 10500));
      }, ms);
    }
    function stop() {
      clearTimeout(timer);
      cancelAnimationFrame(raf);
      raf = 0;
    }
    function start(first) {
      if (off()) return;
      schedule(first ? rand(1500, 3000) : rand(2500, 5000));
      if (leaves.length || gusts.length) wake();
    }
    document.addEventListener('visibilitychange', function () {
      if (document.hidden) stop();
      else start(false);
    });
    if (calmQuery) {
      var changed = function () {
        stop();
        leaves = [];
        gusts = [];
        ctx.clearRect(0, 0, W, H);
        start(false);
      };
      if (calmQuery.addEventListener) calmQuery.addEventListener('change', changed);
      else if (calmQuery.addListener) calmQuery.addListener(changed);
    }
    var resized = function () { size(); if (!raf) ctx.clearRect(0, 0, W, H); };
    if (window.ResizeObserver) new ResizeObserver(resized).observe(canvas.parentNode.querySelector('.in') || canvas.parentNode);
    else window.addEventListener('resize', resized);
    size();
    canvas.hnBlow = blow;
    start(true);
  })();
`;

export const TPL = (name) => '/template/' + name + '?v=' + CAPE_TEMPLATES[name].v;

export const NAV_ITEMS = [
  { key: 'home', href: '/', label: 'nav.home', priority: 100 },
  { key: 'features', href: '/features', label: 'nav.features', priority: 80 },
  { key: 'capes', href: '/capes', label: 'nav.capes', priority: 90 },
  { key: 'translate', href: '/translate', label: 'nav.translate', priority: 70 },
  { key: 'feedback', href: '/feedback', label: 'nav.feedback', priority: 60 },
];

export const NAV_GROUPS = {};

export const ACCOUNT_ITEMS = [
  { key: 'account', href: '/account', label: 'top.myaccount', icon: 'account' },
  { key: 'admin', href: '/admin', label: 'nav.admin', icon: 'admin', role: 'staff' },
];

const DOWNLOADS = [['GitHub Releases', RELEASES], ['CurseForge', CURSEFORGE]];

const gate = (item) => (item && item.role ? ' data-need="' + item.role + '" hidden' : '');

const here = (href, active) => (href === active ? ' aria-current="page"' : '');

function navRows(items, groups) {
  const rows = [];
  const seen = new Set();
  for (const item of items) {
    if (!item.group) {
      rows.push({ item });
      continue;
    }
    if (seen.has(item.group)) continue;
    seen.add(item.group);
    const members = items.filter((other) => other.group === item.group);
    const group = groups[item.group] || {};
    rows.push({
      group: { key: item.group, label: group.label || item.group, role: group.role,
        priority: group.priority ?? Math.max(...members.map((m) => m.priority)) },
      members,
    });
  }
  return rows;
}

export function navMarkup(active, items = NAV_ITEMS, groups = NAV_GROUPS) {
  const cells = navRows(items, groups).map((row) => {
    if (row.item) {
      return '<li class="hn-item" data-key="' + row.item.key + '" data-pri="' + row.item.priority + '"' + gate(row.item) + '>'
        + '<a class="hn-link" href="' + row.item.href + '"' + here(row.item.href, active) + '>%%' + row.item.label + '%%</a></li>';
    }
    const id = 'hn-g-' + row.group.key;
    const inside = row.members.some((m) => m.href === active);
    return '<li class="hn-item hn-group" data-key="' + row.group.key + '" data-pri="' + row.group.priority + '"' + gate(row.group) + '>'
      + '<button class="hn-link hn-trig' + (inside ? ' hn-cur' : '') + '" type="button" aria-haspopup="true" aria-expanded="false" aria-controls="' + id + '">'
      + '<span>%%' + row.group.label + '%%</span>' + ICON.down + '</button>'
      + '<div class="menu hn-menu" id="' + id + '" role="menu">'
      + row.members.map((m) => '<a href="' + m.href + '" role="menuitem"' + here(m.href, active) + gate(m) + '>%%' + m.label + '%%</a>').join('')
      + '</div></li>';
  });
  cells.push('<li class="hn-item hn-more" id="hn-more" hidden>'
    + '<button class="hn-link hn-trig" id="hn-more-btn" type="button" aria-haspopup="true" aria-expanded="false" aria-controls="hn-more-menu">'
    + '<span>%%top.more%%</span>' + ICON.down + '</button>'
    + '<div class="menu hn-menu" id="hn-more-menu" role="menu"></div></li>');
  return '<ul class="hn-list">' + cells.join('') + '</ul>';
}

export function panelNavMarkup(active, items = NAV_ITEMS, groups = NAV_GROUPS) {
  const link = (item, sub) => '<a class="hn-plink' + (sub ? ' hn-sub' : '') + '" href="' + item.href + '"'
    + here(item.href, active) + gate(item) + '>%%' + item.label + '%%</a>';
  return '<nav class="hn-pnav" aria-label="%%nav.label%%">' + navRows(items, groups).map((row) => (row.item ? link(row.item)
    : '<div class="hn-pgroup"' + gate(row.group) + '><p class="hn-phead">%%' + row.group.label + '%%</p>'
      + row.members.map((m) => link(m, true)).join('') + '</div>')).join('') + '</nav>';
}

export function langLinks(lang, menu) {
  return LANGS.map((code) => '<a href="?lang=' + code + '" hreflang="' + tagOf(code) + '" lang="' + tagOf(code) + '" data-lang="' + code + '"'
    + (menu ? ' role="menuitemradio" aria-checked="' + (code === lang) + '"' : (code === lang ? ' aria-current="true"' : ''))
    + '>' + LANG_NAMES[code] + (code === lang ? ICON.check : '') + '</a>').join('');
}

function accountLinks(active, menu) {
  return ACCOUNT_ITEMS.map((item) => '<a' + (menu ? ' role="menuitem" class="hn-mi"' : ' class="hn-plink"') + ' href="' + item.href + '"'
    + here(item.href, active) + gate(item) + '>' + (menu && item.icon ? ICON[item.icon] : '') + '<span>%%' + item.label + '%%</span></a>').join('');
}

function header(lang, active) {
  const mine = ACCOUNT_ITEMS.some((item) => item.href === active);
  const avatar = '<img class="hn-ava" alt="" hidden><span class="hn-ava hn-ava-fb">' + ICON.user + '</span>';
  const downloads = DOWNLOADS.map(([name, href]) => '<a href="' + href + '" role="menuitem">' + name + ICON.out + '</a>').join('');
  return '<div class="top"><canvas class="hn-leaves" id="hn-leaves" aria-hidden="true"></canvas><div class="in">'
    + '<a class="brand" href="/"><span>' + LEAF + '</span>WynnChaYuan</a>'
    + '<nav class="hn" id="hn" aria-label="%%nav.label%%">' + navMarkup(active) + '</nav>'
    + '<div class="tools">'
    + '<a class="hn-icon hn-heart hn-tip" href="' + KOFI + '" aria-label="%%foot.kofi%%" data-tip="%%top.sponsor%%">' + ICON.heart + '</a>'
    + '<div class="pop hn-desk"><button class="btn primary small hn-sheen" id="get" type="button" aria-haspopup="true" aria-expanded="false" aria-controls="get-menu">'
    + ICON.get + '%%top.get%%' + ICON.down + '</button>'
    + '<div class="menu" id="get-menu" role="menu">' + downloads + '<p role="none">%%top.getnote%%</p></div></div>'
    + '<div class="hn-guest hn-desk" id="hn-guest">'
    + '<a class="btn small hn-signin" href="/account"' + here('/account', active) + '>%%top.signin%%</a>'
    + '<div class="pop"><button class="hn-icon hn-tip" id="hn-globe" type="button" aria-label="%%lang.label%%" data-tip="%%lang.label%%" aria-haspopup="true" aria-expanded="false" aria-controls="hn-globe-menu">'
    + ICON.globe + ICON.down + '</button>'
    + '<div class="menu hn-menu hn-narrow" id="hn-globe-menu" role="menu"><div class="hn-langs" role="group" aria-label="%%lang.label%%">'
    + langLinks(lang, true) + '</div></div></div></div>'
    + '<div class="pop hn-user hn-desk" id="hn-user" hidden>'
    + '<button class="hn-avatar' + (mine ? ' hn-cur' : '') + '" id="hn-avatar" type="button" aria-label="%%top.account%%" data-label="%%top.account%%" aria-haspopup="true" aria-expanded="false" aria-controls="hn-acct-menu">'
    + avatar + ICON.down + '</button>'
    + '<div class="menu hn-menu hn-acct" id="hn-acct-menu" role="menu">'
    + '<div class="hn-acct-head" role="none">' + avatar + '<span class="hn-acct-name" data-me-name></span></div>'
    + accountLinks(active, true)
    + '<button class="hn-mi" id="hn-lang-toggle" type="button" role="menuitem" aria-expanded="false" aria-controls="hn-acct-langs">'
    + ICON.globe + '<span>%%lang.label%%</span><span class="hn-mi-val">' + LANG_NAMES[lang] + '</span>' + ICON.down + '</button>'
    + '<div class="hn-langs hn-sublangs" id="hn-acct-langs" role="group" aria-label="%%lang.label%%" hidden>' + langLinks(lang, true) + '</div>'
    + '<hr class="hn-sep" role="separator">'
    + '<button class="hn-mi" type="button" role="menuitem" data-signout>' + ICON.exit + '<span>%%top.signout%%</span></button>'
    + '</div></div>'
    + '<button class="hn-icon hn-burger" id="hn-burger" type="button" aria-label="%%top.menu%%" aria-expanded="false" aria-controls="hn-panel">'
    + ICON.menu + '</button>'
    + '</div></div>'
    + '<div class="hn-panel" id="hn-panel">'
    + panelNavMarkup(active)
    + '<div class="hn-psec"><p class="hn-phead">%%top.get%%</p>'
    + DOWNLOADS.map(([name, href]) => '<a class="hn-plink" href="' + href + '">' + name + ICON.out + '</a>').join('')
    + '<p class="hn-pnote">%%top.getnote%%</p></div>'
    + '<div class="hn-psec"><p class="hn-phead">%%nav.account%%</p>'
    + '<div id="hn-p-guest"><a class="btn small hn-psignin" href="/account"' + here('/account', active) + '>%%top.signin%%</a></div>'
    + '<div class="hn-pgrid" id="hn-p-user" hidden><p class="hn-pme">' + avatar + '<span data-me-name></span></p>'
    + accountLinks(active, false)
    + '<button class="hn-plink" type="button" data-signout><span>%%top.signout%%</span>' + ICON.exit + '</button></div></div>'
    + '<div class="hn-psec"><p class="hn-phead">%%lang.label%%</p><div class="hn-langs hn-plangs">' + langLinks(lang, false) + '</div></div>'
    + '</div></div>';
}

const COMMON_SCRIPT = scriptFile('common', 'var L = ' + readJsonData('site-text') + ';' + COMMON);

export function layout(lang, title, active, body, script) {
  const column = (heading, links) => '<div><h2>%%' + heading + '%%</h2><ul>'
    + links.map((link) => '<li>' + link + '</li>').join('') + '</ul></div>';
  const to = (href, label) => '<a href="' + href + '">' + label + '</a>';
  return '<!doctype html>\n<html lang="%%html%%">\n<head>\n<meta charset="utf-8">\n'
    + '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
    + '<meta name="color-scheme" content="dark">\n'
    + '<meta name="theme-color" content="#0f1512">\n'
    + '<link rel="icon" type="image/png" href="/favicon.png">\n'
    + '<link rel="apple-touch-icon" href="/apple-touch-icon.png">\n'
    + '<meta name="description" content="%%meta.desc%%">\n'
    + '<meta property="og:site_name" content="WynnChaYuan">\n'
    + '<meta property="og:type" content="website">\n'
    + '<meta property="og:url" content="https://wynnchayuan.com' + (active === '/' ? '/' : active) + '">\n'
    + '<meta property="og:title" content="' + title + '">\n'
    + '<meta property="og:description" content="%%meta.desc%%">\n'
    + '<meta property="og:image" content="https://wynnchayuan.com/icon-192.png">\n'
    + '<meta name="twitter:card" content="summary">\n'
    + '<title>' + title + '</title>\n<style>' + STYLE + FONT_CSS + TEX_CSS + '</style>\n</head>\n<body>\n'
    + '<a class="skip" href="#main">%%skip%%</a>'
    + header(lang, active)
    + '\n<main id="main">' + body + '</main>\n'
    + '<footer><div class="in"><div class="foot">'
    + '<div class="foot-brand"><a class="brand" href="/"><span>' + LEAF + '</span>WynnChaYuan</a>'
    + '<p>%%foot.tagline%%</p></div>'
    + '<div class="foot-cols" role="navigation" aria-label="%%foot.label%%">'
    + column('foot.get', [to(RELEASES, 'GitHub Releases'), to(CURSEFORGE, 'CurseForge'),
      to(REPO + '/blob/main/CHANGELOG.md', '%%foot.changelog%%')])
    + column('foot.project', [to(REPO, 'GitHub'), to(REPO + '/blob/main/LICENSE', '%%foot.license%%'),
      to(REPO + '/blob/main/LICENSE-DATA', '%%foot.datalicense%%'), to(ISSUES, '%%foot.issues%%'),
      to('/privacy', '%%foot.privacy%%'), to('/terms', '%%foot.terms%%')])
    + column('foot.support', [to(KOFI, '%%foot.kofi%%'), to('/#sponsoring', '%%foot.sponsoring%%'),
      to('/#team', '%%foot.team%%'), to(REPO + '/blob/main/CONTRIBUTING.md', '%%foot.contribute%%')])
    + column('foot.contact', [to('/feedback', '%%foot.feedback%%'), '%%foot.sitedev%%',
      '<button class="copy" id="dc-copy" type="button" title="%%foot.copy%%">%%foot.discord%%</button>'])
    + '</div></div>'
    + '<div class="legal"><span>© 2026 WynnChaYuan - All rights reserved.</span>'
    + '<span>%%foot.unofficial%% %%foot.assets%%</span>'
    + '<span>Designed by LyuChaCha</span></div>'
    + '</div></footer>\n'
    + jsonData('site-text', '%%js%%') + COMMON_SCRIPT + '\n' + (script || '') + '\n</body>\n</html>';
}

export const leaf = (colour) => '<span style="color:' + colour + ';display:flex">' + LEAF + '</span>';
