import { DEMO } from './demo-data.js';
import { CURSEFORGE, ICON, LEAF, RELEASES } from './site-constants.js';
import { TPL, layout, leaf } from './site-layout.js';
import { STAGE_JS, STAGE_PARTS } from './site-stage.js';
import { TEXT, attribute, filled, plain } from './site-strings.js';
import { TEX_URL } from './site-styles.js';
import { jsonData, readJsonData, scriptFile } from './site-scripts.js';

const REVEAL_HOME = '#main > h2, #main > .lede, .features > div, #main > .more, .split > *, .band, .everyone, #main > .panel, #main > .cta';

const REVEAL_FEATURES = 'section[id] > h2, .cards > .fcard';

const HOME = String.raw`
  <section class="hero">
    <div>
      <h1>%%home.h1%%</h1>
      <p class="lede">%%home.lede%%</p>
      <div class="cta">
        <a class="btn primary" href="https://github.com/LyuChaCha/WynnChaYuan/releases/latest">%%home.github%%</a>
        <a class="btn" href="https://www.curseforge.com/minecraft/mc-mods/wynnchayuan">CurseForge</a>
      </div>
      <div class="chips" role="group" aria-label="%%home.langs%%">
        <button type="button" class="chip" data-lang="zh_tw" lang="zh-Hant">繁體中文</button><button type="button" class="chip" data-lang="zh_cn" lang="zh-Hans">简体中文</button><button type="button" class="chip" data-lang="ja_jp" lang="ja">日本語</button>
        <button type="button" class="chip" data-lang="ko_kr" lang="ko">한국어</button><button type="button" class="chip" data-lang="ru_ru" lang="ru">Русский</button><button type="button" class="chip" data-lang="es_es" lang="es">Español</button><button type="button" class="chip" data-lang="tr_tr" lang="tr">Türkçe</button>
      </div>
    </div>
    <div class="hero-screen">
      <a class="screen" id="hero-stage" href="/features" aria-label="%%home.try%%"></a>
      <p class="hero-cap"><span>%%home.live%%</span><a href="/features">%%home.try%%` + ICON.go + String.raw`</a></p>
    </div>
  </section>
  ` + STAGE_PARTS + String.raw`

  <h2>%%home.what%%</h2>
  <div class="features">
    <div><h3>%%home.f1%%</h3><p>%%home.f1b%%</p></div>
    <div><h3>%%home.f2%%</h3><p>%%home.f2b%%</p></div>
    <div><h3>%%home.f3%%</h3><p>%%home.f3b%%</p></div>
    <div><h3>%%home.f4%%</h3><p>%%home.f4b%%</p></div>
    <div><h3>%%home.f5%%</h3><p>%%home.f5b%%</p></div>
    <div><h3>%%home.f6%%</h3><p>%%home.f6b%%</p></div>
  </div>
  <p class="more"><a href="/features">%%home.morefeatures%%` + ICON.go + String.raw`</a></p>

  <h2 id="install">%%home.install%%</h2>
  <div class="split">
    <div class="panel">
      <p style="margin:0">%%home.need%%</p>
      <p class="note" style="margin:8px 0 0">%%home.how%%</p>
    </div>
    <div class="panel note">%%home.about%%</div>
  </div>

  <h2 id="team">%%home.team%%</h2>
  <p class="lede">%%home.teamlede%%</p>
  <template id="leaf">` + LEAF + String.raw`</template>
  <template id="out">` + ICON.out + String.raw`</template>
  <div class="team-pick" id="team-pick" role="group" aria-label="%%home.teampick%%" hidden></div>
  <div class="team" id="team-list" aria-busy="true">
    <section class="band" id="band-developers" data-role="developers">
      <div class="band-head">` + leaf('#FFD24A') + String.raw`<h3>%%home.dev%%</h3><p>%%home.devb%%</p></div>
      <ul class="lineup"></ul>
    </section>
    <section class="band" id="band-sponsors" data-role="sponsors">
      <div class="band-head">` + leaf('#5BD8E8') + String.raw`<h3>%%home.sponsor%%</h3><p>%%home.sponsorb%%</p></div>
      <ul class="lineup"></ul>
    </section>
    <section class="band" id="band-contributors" data-role="contributors">
      <div class="band-head">` + leaf('#5BD86F') + String.raw`<h3>%%home.contrib%%</h3><p>%%home.contribb%%</p></div>
      <ul class="lineup"></ul>
    </section>
    <section class="band" id="band-guilds" data-role="guilds" hidden>
      <div class="band-head"><span style="color:var(--ink)">` + ICON.flag + String.raw`</span><h3>%%home.guilds%%</h3><p>%%home.guildsb%%</p></div>
      <ul class="lineup guilds"></ul>
    </section>
  </div>
  <p class="note" id="team-fail" hidden>%%home.teamfail%%</p>
  <p class="everyone">` + leaf('#A8A8A8') + String.raw`<span><b>%%home.user%%</b>%%home.userb%%</span></p>

  <h2 id="sponsoring">%%home.support%%</h2>
  <div class="panel">
    <p style="margin:0">%%home.free%%</p>
    <ul class="plain">
      <li>%%home.sup1%%</li>
      <li>%%home.sup2%%</li>
      <li>%%home.sup3%%</li>
    </ul>
  </div>
  <div class="cta">
    <a class="btn" href="/capes">%%home.seecapes%%</a>
    <a class="btn" href="https://ko-fi.com/lyuchacha">%%home.donate%%</a>
    <a class="btn" href="https://github.com/LyuChaCha/WynnChaYuan/blob/main/CONTRIBUTING.md">%%home.join%%</a>
  </div>
`;

const HOME_JS = jsonData('demo-data', '%%demo%%') + scriptFile('home', 'var D = ' + readJsonData('demo-data') + ';' + STAGE_JS + String.raw`
  (function () {
    var host = document.getElementById('hero-stage');
    var screen = makeStage(host, D);
    var order = ['zh_tw', 'ja_jp', 'ko_kr', 'ru_ru', 'es_es', 'tr_tr', 'zh_cn'];
    var scenes = ['items', 'world', 'dialogue'];
    var chips = Array.prototype.slice.call(document.querySelectorAll('.chips .chip'));
    var state = {};
    Object.keys(STAGE_DEFAULTS).forEach(function (key) { state[key] = STAGE_DEFAULTS[key]; });
    var at = 0;
    var scene = 0;
    var picked = false;
    Object.keys(STAGE_TAGS).forEach(function (code) {
      if (STAGE_TAGS[code] === document.documentElement.lang) at = order.indexOf(code);
    });
    function show(typed) {
      chips.forEach(function (chip) { chip.setAttribute('aria-pressed', chip.dataset.lang === order[at] ? 'true' : 'false'); });
      screen.draw(scenes[scene], state, order[at], typed);
    }
    chips.forEach(function (chip) {
      chip.onclick = function () { at = order.indexOf(chip.dataset.lang); picked = true; show(true); };
    });
    show(false);
    if (calm) return;
    var tick = 0;
    setInterval(function () {
      if (document.hidden) return;
      tick++;
      if (!picked) at = (at + 1) % order.length;
      if (picked || tick % 2 === 0) scene = (scene + 1) % scenes.length;
      show(true);
    }, 3200);
  })();
  (function () {
    var ROLES = { developers: 1, sponsors: 1, contributors: 1 };
    var TEAM_NAMES = { zh_tw: '繁體中文', zh_cn: '简体中文', ja_jp: '日本語', ko_kr: '한국어', ru_ru: 'Русский', es_es: 'Español', tr_tr: 'Türkçe' };
    var present = {};
    var guildsShown = false;
    function pickTeam(code) {
      document.querySelectorAll('#team-pick .chip').forEach(function (chip) {
        chip.setAttribute('aria-pressed', chip.dataset.team === code ? 'true' : 'false');
      });
      Object.keys(ROLES).forEach(function (role) {
        var band = document.getElementById('band-' + role);
        var shown = 0;
        band.querySelectorAll('.lineup > .fig').forEach(function (item) {
          var hit = !code || (' ' + item.dataset.teams + ' ').indexOf(' ' + code + ' ') >= 0;
          item.hidden = !hit;
          if (hit) shown++;
        });
        band.hidden = shown === 0;
      });
      document.getElementById('band-guilds').hidden = !guildsShown || Boolean(code);
    }
    function buildPick() {
      var codes = Object.keys(TEAM_NAMES).filter(function (code) { return present[code]; });
      var host = document.getElementById('team-pick');
      host.replaceChildren();
      host.hidden = codes.length === 0;
      if (!codes.length) return;
      [''].concat(codes).forEach(function (code) {
        var chip = el('button', 'chip', code ? TEAM_NAMES[code] : L.teamAll);
        chip.type = 'button';
        chip.dataset.team = code;
        if (code) chip.lang = STAGE_TAGS[code] || '';
        chip.onclick = function () {
          pickTeam(code);
          var link = new URL(location.href);
          if (code) link.searchParams.set('team', code);
          else link.searchParams.delete('team');
          history.replaceState(history.state, '', link.pathname + link.search + link.hash);
        };
        host.append(chip);
      });
      var asked = new URLSearchParams(location.search).get('team') || '';
      pickTeam(present[asked] ? asked : '');
    }
    var shape = document.getElementById('leaf').content.firstElementChild;
    var arrow = document.getElementById('out').content.firstElementChild;
    function settle(img) {
      img.onload = function () { img.classList.add('in'); };
      img.onerror = function () { img.classList.add('in'); };
    }
    function outside(link, where) {
      link.href = where;
      link.target = '_blank';
      link.rel = 'noopener';
    }
    fetch('/api/team').then(function (r) {
      if (!r.ok) throw new Error('team');
      return r.json();
    }).then(function (data) {
      data.team.forEach(function (section) {
        if (!ROLES[section.role]) return;
        var band = document.getElementById('band-' + section.role);
        var list = band.querySelector('.lineup');
        list.replaceChildren();
        band.hidden = section.members.length === 0;
        section.members.forEach(function (member, i) {
          var item = el('li', 'fig');
          item.style.setProperty('--i', Math.min(i, 14));
          var row = el(member.mc ? 'a' : 'span', 'who-row');
          if (member.mc) {
            outside(row, 'https://wynncraft.com/stats/player/' + encodeURIComponent(member.mc));
            row.title = fmt(L.onWynn, { n: member.mc });
            var img = el('img');
            img.alt = '';
            img.width = 40;
            img.height = 40;
            img.decoding = 'async';
            settle(img);
            img.src = head(member.mc, 40);
            row.append(img);
          } else {
            var blank = el('span', 'blank');
            blank.append(shape.cloneNode(true));
            row.append(blank);
          }
          var teams = member.teams || [];
          var areas = member.areas || [];
          item.dataset.teams = teams.join(' ');
          teams.forEach(function (code) { present[code] = true; });
          if (teams.length || areas.length) {
            var txt = el('span', 'who-txt');
            var tags = el('span', 'who-tags');
            var words = [];
            teams.forEach(function (code) {
              var tl = el('span', 'tl', TEAM_NAMES[code] || code);
              tl.lang = STAGE_TAGS[code] || '';
              if (words.length) tags.append(' · ');
              tags.append(tl);
              words.push(TEAM_NAMES[code] || code);
            });
            areas.forEach(function (code) {
              var word = L.teamAreas[code] || code;
              tags.append((words.length ? ' · ' : '') + word);
              words.push(word);
            });
            tags.title = words.join(' · ');
            var top = el('span', 'who-top');
            top.append(el('b', '', member.name));
            if (member.mc && member.mc !== member.name) top.append(el('span', 'mc', member.mc));
            txt.append(top, tags);
            row.append(txt);
          } else {
            row.append(el('b', '', member.name));
            if (member.mc && member.mc !== member.name) row.append(el('span', 'mc', member.mc));
          }
          var leaf = shape.cloneNode(true);
          leaf.style.color = member.color || section.color || '';
          row.append(leaf);
          item.append(row);
          list.append(item);
        });
      });
      var guilds = data.guilds || [];
      var band = document.getElementById('band-guilds');
      var list = band.querySelector('.lineup');
      list.replaceChildren();
      band.hidden = guilds.length === 0;
      guilds.forEach(function (guild, i) {
        var item = el('li', 'fig');
        item.style.setProperty('--i', Math.min(i, 14));
        var link = el('a', 'guild');
        outside(link, 'https://wynncraft.com/stats/guild/' + encodeURIComponent(guild.name));
        link.title = fmt(L.onWynn, { n: guild.name });
        if (guild.banner) {
          var banner = el('img', 'g-banner');
          banner.alt = '';
          settle(banner);
          banner.src = '/api/guild/' + guild.slug + '/banner?v=' + guild.banner;
          link.append(banner);
        }
        if (guild.icon) {
          var icon = el('img', 'g-icon');
          icon.alt = '';
          settle(icon);
          icon.src = '/api/guild/' + guild.slug + '/icon?v=' + guild.icon;
          link.append(icon);
        } else {
          link.append(el('span', 'g-icon', guild.tag || guild.name.slice(0, 3).toUpperCase()));
        }
        var name = el('span', 'g-name');
        if (guild.tag) name.append(el('span', '', '[' + guild.tag + ']'));
        name.append(el('b', '', guild.name));
        link.append(name, arrow.cloneNode(true));
        item.append(link);
        list.append(item);
      });
      guildsShown = guilds.length > 0;
      buildPick();
      document.getElementById('team-list').removeAttribute('aria-busy');
    }).catch(function () {
      document.getElementById('team-list').removeAttribute('aria-busy');
      document.getElementById('team-fail').hidden = false;
    });
  })();
`);

const card = (key, at, eg) => '<article class="fcard"><i class="glyph" style="--r:' + at[0] + ';--c:' + at[1]
  + '" aria-hidden="true"></i><div><h3>%%feat.' + key + '%%</h3><p>%%feat.' + key + 'b%%</p>'
  + (eg ? '<p class="eg" data-eg="' + eg + '" aria-hidden="true"></p>' : '') + '</div></article>';

const mod = (name, slug, state, key) => '<article class="fcard mod"><i class="glyph" style="--r:2;--c:11" aria-hidden="true"></i>'
  + '<div><h3><a href="https://modrinth.com/mod/' + slug + '">' + name + '</a>'
  + '<span class="state' + (state === 'required' ? ' need' : '') + '">%%feat.' + state + '%%</span></h3><p>%%feat.' + key + 'b%%</p></div></article>';

const TEMPLATE_SIZES = [[22, 17], [44, 34], [88, 68], [176, 136], [352, 272], [704, 544], [1056, 816]];

const TEMPLATE_BLOCK = '<div class="tpl"><img class="guide" src="%%tpl.guide%%" alt="%%tpl.alt%%" width="776" height="502" loading="lazy">'
  + '<div><h2>%%tpl.h%%</h2><p>%%tpl.p%%</p><p class="label">%%tpl.pick%%</p><div class="tpl-sizes">'
  + TEMPLATE_SIZES.map(([w, h]) => {
    const name = 'template-' + w + 'x' + h + '.png';
    const best = w === 88;
    return '<a class="btn' + (best ? ' primary' : '') + '" href="' + TPL(name) + '" download="wynnchayuan-cape-' + name + '">'
      + ICON.get + w + '×' + h + (best ? ' <small>%%tpl.rec%%</small>' : '') + '</a>';
  }).join('')
  + '</div><p>%%tpl.note%%</p><div class="more-links"><a href="' + TPL('example-88x68.gif')
  + '" download="wynnchayuan-cape-example-88x68.gif">%%tpl.example%%</a><a href="/account">%%tpl.upload%%</a></div></div></div>';

const FEATURES = String.raw`
  <h1>%%features.h1%%</h1>
  <p class="lede">%%features.lede%%</p>

  <section class="play" aria-label="%%demo.label%%">
    <div class="play-bar">
      <div class="seg" id="play-tabs" role="group" aria-label="%%play.pages%%">
        <button type="button" data-tab="items" aria-pressed="true"></button>
        <button type="button" data-tab="dialogue" aria-pressed="false"></button>
        <button type="button" data-tab="world" aria-pressed="false"></button>
      </div>
      <label class="play-lang" for="play-lang"><span id="play-lang-name"></span>
        <select class="lang" id="play-lang">
          <option value="zh_tw" lang="zh-Hant">繁體中文</option><option value="zh_cn" lang="zh-Hans">简体中文</option>
          <option value="ja_jp" lang="ja">日本語</option><option value="ko_kr" lang="ko">한국어</option>
          <option value="ru_ru" lang="ru">Русский</option><option value="es_es" lang="es">Español</option>
          <option value="tr_tr" lang="tr">Türkçe</option>
        </select>
      </label>
    </div>
    <div class="play-body">
      <div class="play-view"><div class="screen" id="play-stage" aria-hidden="true"></div></div>
      <div class="play-set" id="play-set"></div>
    </div>
    <p class="demo-foot"><span id="play-hint" role="status"></span><span class="hint">%%play.note%%</span></p>
  </section>
  ` + STAGE_PARTS + String.raw`

  <div class="spec">
    <nav class="spec-nav" aria-label="%%features.nav%%">
      <a href="#translate">%%features.t%%</a>
      <a href="#tools">%%features.u%%</a>
      <a href="#social">%%features.s%%</a>
      <a href="#compat">%%features.c%%</a>
    </nav>
    <div>
      <section id="translate">
        <h2>%%features.t%%</h2>
        <div class="cards">
          ` + [card('t1', [0, 8], 'Main Attack Damage'), card('t2', [0, 3], 'Ability Points'),
    card('t3', [2, 15], 'Well, what are we waiting for?'), card('t4', [1, 13], 'World Event'),
    card('t5', [2, 9], 'Weapon Merchant'), card('t6', [3, 0], 'The trade has been completed!'),
    card('t7', [3, 9], 'Trade Market'), card('t8', [1, 8], 'Daily Objective:'),
    card('t9', [3, 12], 'Secret Discovery'), card('t10', [3, 3], 'Level Up!')].join('\n          ') + String.raw`
        </div>
        <p class="note">%%features.tnote%%</p>
      </section>
      <section id="tools">
        <h2>%%features.u%%</h2>
        <div class="cards">
          ` + [card('u1', [2, 7]), card('u2', [3, 6]), card('u3', [3, 4]), card('u4', [0, 12]),
    card('u5', [4, 12]), card('u6', [2, 11]), card('u7', [5, 4]), card('u8', [2, 8])].join('\n          ') + String.raw`
        </div>
      </section>
      <section id="social">
        <h2>%%features.s%%</h2>
        <div class="cards three">
          <article class="fcard tall"><div class="show" aria-hidden="true">` + leaf('#FFD24A') + leaf('#5BD8E8') + leaf('#5BD86F') + leaf('#A8A8A8') + String.raw`</div>
            <div><h3>%%feat.s1%%</h3><p>%%feat.s1b%%</p></div></article>
          <article class="fcard tall"><div class="show" aria-hidden="true"><span class="capemini"><img src="` + TPL('example-88x68.gif') + String.raw`" alt="" loading="lazy"></span><span class="capemini"><img src="` + TPL('template-88x68.png') + String.raw`" alt="" loading="lazy"></span></div>
            <div><h3>%%feat.s2%%</h3><p>%%feat.s2b%%</p></div></article>
          <article class="fcard tall"><div class="show" aria-hidden="true"><i class="glyph" style="--r:2;--c:14;box-shadow:none;background:none"></i></div>
            <div><h3>%%feat.s3%%</h3><p>%%feat.s3b%%</p></div></article>
        </div>
      </section>
      <section id="compat">
        <h2>%%features.c%%</h2>
        <div class="cards">
          ` + [mod('Wynntils 4.2+', 'wynntils', 'required', 'c1'), mod('Fabric API', 'fabric-api', 'required', 'c2'),
    mod('Mod Menu', 'modmenu', 'optional', 'c3'), mod('WynnMarketSearch', 'wynnmarketsearch', 'optional', 'c4'),
    mod('Wynnventory', 'wynnventory', 'optional', 'c5'), mod('WynnMod', 'wynnmod', 'compatible', 'c6')].join('\n          ') + String.raw`
        </div>
        <p class="note">%%features.cnote%%</p>
        <div class="cta">
          <a class="btn primary" href="` + RELEASES + String.raw`">%%home.github%%</a>
          <a class="btn" href="` + CURSEFORGE + String.raw`">CurseForge</a>
          <a class="btn" href="/#install">%%features.install%%</a>
        </div>
      </section>
    </div>
  </div>
`;

const FEATURES_JS = jsonData('demo-data', '%%demo%%') + scriptFile('features', 'var D = ' + readJsonData('demo-data') + ';' + STAGE_JS + String.raw`
  (function () {
    var ui = D.ui;
    var TAGS = STAGE_TAGS;
    var PAGES = {
      items: [
        { key: 'tooltip', label: 'items.tooltip', options: ['mode.panel', 'mode.replace', 'mode.off'] },
        { key: 'names', label: 'items.names', options: ['mode.on', 'items.names.both', 'mode.off'] },
        { key: 'gap', label: 'panel.gap', range: true }
      ],
      dialogue: [
        { key: 'dialogue', label: 'dialogue.mode', options: ['mode.box', 'mode.replace', 'mode.off'] },
        { key: 'choices', label: 'dialogue.choices', options: ['mode.box', 'mode.replace', 'mode.off'] }
      ],
      world: [
        { key: 'nametag', label: 'world.nametag', options: ['mode.lookat', 'mode.replace', 'mode.off'] },
        { key: 'chat', label: 'world.chat', options: ['mode.replace', 'mode.both', 'mode.off'] },
        { key: 'tracker', label: 'world.tracker', options: ['mode.replace', 'mode.panel', 'mode.off'] },
        { key: 'titles', label: 'world.titles', toggle: true },
        { key: 'bossbar', label: 'world.bossbar', toggle: true },
        { key: 'scoreboard', label: 'world.scoreboard', toggle: true },
        { key: 'held', label: 'world.helditem', toggle: true }
      ]
    };
    var state = { tooltip: 0, names: 2, gap: 12, dialogue: 1, choices: 1, nametag: 1, chat: 1, tracker: 0,
      titles: true, bossbar: true, scoreboard: true, held: true };
    var tab = 'items';
    var lang = 'zh_tw';
    var tabs = Array.prototype.slice.call(document.querySelectorAll('#play-tabs button'));
    var picker = document.getElementById('play-lang');
    var set = document.getElementById('play-set');
    var stage = document.getElementById('play-stage');
    var hintNode = document.getElementById('play-hint');
    var updaters = [];
    Object.keys(TAGS).forEach(function (code) { if (TAGS[code] === document.documentElement.lang) lang = code; });
    picker.value = lang;
    document.getElementById('play-lang-name').textContent = ui['data.language'];
    tabs.forEach(function (button) { button.textContent = ui['tab.' + button.dataset.tab]; });

    var screen = makeStage(stage, D);
    function draw(typed) { screen.draw(tab, state, lang, typed); }
    function explain(row) {
      var text = row ? ui[row.label + '.hint'] : L.playStart;
      if (hintNode.textContent === text) return;
      hintNode.textContent = text;
      flash(hintNode);
    }
    function changed(row, typed, quiet) {
      updaters.forEach(function (update) { update(); });
      explain(row);
      screen.quiet(quiet);
      draw(typed);
    }
    function build() {
      set.replaceChildren();
      updaters = [];
      PAGES[tab].forEach(function (row) {
        var line = el('div', 'set-row');
        line.append(el('span', '', ui[row.label]));
        if (row.options) {
          var seg = el('div', 'seg');
          seg.setAttribute('role', 'group');
          seg.setAttribute('aria-label', ui[row.label]);
          var buttons = row.options.map(function (key, i) {
            var button = el('button', '', ui[key]);
            button.type = 'button';
            button.onclick = function () { state[row.key] = i; changed(row, true); };
            seg.append(button);
            return button;
          });
          updaters.push(function () {
            buttons.forEach(function (button, i) { button.setAttribute('aria-pressed', state[row.key] === i ? 'true' : 'false'); });
          });
          line.append(seg);
        } else if (row.toggle) {
          var toggle = el('button', 'switch');
          toggle.type = 'button';
          toggle.setAttribute('role', 'switch');
          toggle.setAttribute('aria-label', ui[row.label]);
          toggle.onclick = function () { state[row.key] = !state[row.key]; changed(row, false); };
          updaters.push(function () { toggle.setAttribute('aria-checked', state[row.key] ? 'true' : 'false'); });
          line.append(toggle);
        } else {
          var wrap = el('div', 'range');
          var input = el('input');
          input.type = 'range';
          input.min = 12;
          input.max = 200;
          input.value = state[row.key];
          input.setAttribute('aria-label', ui[row.label]);
          var output = el('output', '', state[row.key] + ' px');
          input.oninput = function () { state[row.key] = Number(input.value); output.textContent = input.value + ' px'; changed(row, false, true); };
          updaters.push(function () { line.classList.toggle('dim', state.tooltip !== 0); });
          wrap.append(input, output);
          line.append(wrap);
        }
        set.append(line);
      });
      updaters.forEach(function (update) { update(); });
      slideAll();
    }
    tabs.forEach(function (button) {
      button.onclick = function () {
        tab = button.dataset.tab;
        tabs.forEach(function (other) { other.setAttribute('aria-pressed', other === button ? 'true' : 'false'); });
        build();
        explain(null);
        draw(true);
      };
    });
    function examples() {
      Array.prototype.forEach.call(document.querySelectorAll('[data-eg]'), function (node) {
        var src = node.dataset.eg;
        var out = el('span', 'tr', (D.text[lang][src] || src).replace(/\{c\d+\}/g, ''));
        out.lang = TAGS[lang];
        node.replaceChildren(el('span', '', src), out);
      });
    }
    picker.onchange = function () { lang = picker.value; draw(true); examples(); };
    build();
    explain(null);
    draw(false);
    examples();

    var links = Array.prototype.slice.call(document.querySelectorAll('.spec-nav a'));
    function idOf(link) { return link.getAttribute('href').slice(1); }
    if ('IntersectionObserver' in window) {
      var seen = {};
      var watch = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) { seen[entry.target.id] = entry.isIntersecting; });
        var current = links.filter(function (link) { return seen[idOf(link)]; })[0];
        if (!current) return;
        links.forEach(function (link) {
          if (link === current) link.setAttribute('aria-current', 'true');
          else link.removeAttribute('aria-current');
        });
      }, { rootMargin: '-90px 0px -55% 0px' });
      links.forEach(function (link) {
        var section = document.getElementById(idOf(link));
        if (section) watch.observe(section);
      });
    }
  })();
`);

const VIEWER = String.raw`
  function mannequin() {
    var c = document.createElement('canvas');
    c.width = 64; c.height = 64;
    var g = c.getContext('2d');
    g.fillStyle = '#8f9a93'; g.fillRect(0, 0, 64, 64);
    g.fillStyle = '#a4aea7'; g.fillRect(0, 0, 32, 16);
    g.fillStyle = '#7d8881'; g.fillRect(0, 16, 16, 16); g.fillRect(16, 48, 16, 16);
    return c;
  }
  function capeCanvas(source, width) {
    var k = Math.max(1, Math.round(width / 22));
    var c = document.createElement('canvas');
    c.width = 64 * k; c.height = 32 * k;
    var g = c.getContext('2d');
    g.imageSmoothingEnabled = false;
    g.drawImage(source, 0, 0, 22 * k, 17 * k);
    return c;
  }
  var SKIN_KEY = 'wcy-cape-skin';
  function skinMode() { try { return localStorage.getItem(SKIN_KEY) === 'plain' ? 'plain' : 'skin'; } catch (e) { return 'skin'; } }
  function setSkinMode(mode) { try { localStorage.setItem(SKIN_KEY, mode); } catch (e) {} }
  function dress(viewer, name) {
    if (skinMode() === 'plain' || !name) { viewer.loadSkin(mannequin()); return; }
    var asked = viewer.dressed = name;
    Promise.resolve(viewer.loadSkin('https://minotar.net/skin/' + encodeURIComponent(name), { model: 'auto-detect' }))
      .catch(function () { if (viewer.dressed === asked) viewer.loadSkin(mannequin()); });
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
    /* 轉外面那一層：走路動畫每一格都把人本身轉回正面 */
    viewer.playerWrapper.rotation.y = Math.PI + 0.5;
    if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      viewer.animation = new skinview3d.WalkingAnimation();
      viewer.animation.speed = 0.55;
    }
    return viewer;
  }
  var REST = Math.PI + 0.5;
  function reveal(viewer) {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    viewer.turning = (viewer.turning || 0) + 1;
    var mine = viewer.turning;
    var started = performance.now();
    function step(now) {
      if (viewer.turning !== mine) return;
      var t = Math.min(1, (now - started) / 900);
      var eased = 1 - Math.pow(1 - t, 4);
      viewer.playerWrapper.rotation.y = REST + (1 - eased) * 1.15;
      if (t < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  }
  function wear(viewer, source, isGif) {
    var mine = ++playing;
    var image = new Image();
    image.onload = function () {
      if (mine !== playing) return;
      viewer.loadCape(capeCanvas(image, image.naturalWidth));
      reveal(viewer);
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

const STAR = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3.6 2.6 5.3 5.8.8-4.2 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.2-4.1 5.8-.8z"/></svg>';

const CAPES = String.raw`
  <h1>%%capes.h1%%</h1>
  <p class="lede">%%capes.lede%%</p>
  <details class="make" id="template">
    <summary><img src="` + TPL('template-88x68.png') + String.raw`" alt="" width="44" height="34">
      <span><b>%%capes.make%%</b><span class="sub">%%capes.makeb%%</span></span>
      <span class="btn primary small">%%tpl.h%%` + ICON.down + String.raw`</span></summary>
    ` + TEMPLATE_BLOCK + String.raw`
  </details>
  <div class="bar">
    <span class="count" id="count"></span>
    <label class="sortby" for="kind">%%capes.f.format%%
      <select class="pick" id="kind">
        <option value="all">%%capes.f.all%%</option>
        <option value="png">PNG</option>
        <option value="gif">GIF</option>
      </select>
    </label>
    <label class="sortby" for="scope" id="scope-box" hidden>%%capes.f.show%%
      <select class="pick" id="scope">
        <option value="all">%%capes.f.all%%</option>
        <option value="fav">%%capes.f.fav%%</option>
        <option value="hist">%%capes.f.hist%%</option>
        <option value="unlisted">%%capes.f.hidden%%</option>
        <option value="private">%%capes.f.private%%</option>
        <option value="pending">%%capes.f.pending%%</option>
      </select>
    </label>
    <label class="sortby" for="sort">%%capes.sort%%
      <select class="pick" id="sort">
        <option value="new">%%capes.s.new%%</option>
        <option value="old">%%capes.s.old%%</option>
        <option value="worn">%%capes.s.worn%%</option>
        <option value="stars">%%capes.s.stars%%</option>
        <option value="recent">%%capes.s.recent%%</option>
      </select>
    </label>
    <a class="btn" href="/account" style="margin-left:auto">%%capes.mine%%</a>
  </div>
  <p class="notice" id="gone" role="status" hidden></p>
  <div class="grid" id="grid"></div>
  <p class="note" style="margin:28px 0 0;max-width:72ch">%%rules%% %%capes.keep%%</p>
  <template id="star">` + STAR + String.raw`</template>

  <dialog id="viewer" aria-labelledby="viewer-title">
    <div class="view">
      <canvas class="model" id="model" width="520" height="440"></canvas>
      <div class="side">
        <h2 id="viewer-title">%%view.title%%</h2>
        <img class="flat" id="flat" alt="%%view.flat%%">
        <dl>
          <dt>%%view.type%%</dt><dd id="d-type"></dd>
          <dt>%%view.dim%%</dt><dd id="d-dim"></dd>
          <dt>%%view.size%%</dt><dd id="d-size"></dd>
          <dt>%%view.users%%</dt><dd id="d-users"></dd>
          <dt>%%view.stars%%</dt><dd id="d-stars"></dd>
          <dt>%%view.date%%</dt><dd id="d-date"></dd>
        </dl>
        <p class="hint" id="idle-note" hidden style="color:var(--gold)"></p>
        <ul class="owners" id="owners" hidden></ul>
        <p class="hint" id="hint"></p>
        <div class="seg" id="skinpick" role="group">
          <button type="button" data-s="skin" aria-pressed="true"></button>
          <button type="button" data-s="plain" aria-pressed="false"></button>
        </div>
        <div class="wearbox" id="wearbox" hidden>
          <p class="hint" id="wear-note" hidden></p>
          <button class="btn primary" id="wear">%%view.wear%%</button>
          <button class="btn" id="unwear" hidden></button>
          <button class="btn" id="fav"></button>
          <button class="btn" id="forget" hidden>%%view.forget%%</button>
        </div>
        <button class="btn" id="share" type="button" hidden>%%view.share%%</button>
        <p class="hint" id="wear-signin" hidden>%%view.signin%%</p>
        <p class="msg" id="wear-msg" role="status" style="margin:0"></p>
        <div class="spacer"></div>
        <button class="btn" id="approve" hidden>%%view.approve%%</button>
        <button class="btn" id="review" hidden>%%view.review%%</button>
        <button class="btn danger" id="del" hidden>%%view.del%%</button>
        <button class="btn danger" id="ban" hidden>%%view.ban%%</button>
        <button class="btn" id="close">%%view.close%%</button>
      </div>
    </div>
  </dialog>
`;

const CAPES_JS = SKINVIEW + scriptFile('capes', VIEWER + String.raw`
  var grid = document.getElementById('grid');
  var dialog = document.getElementById('viewer');
  var starShape = document.getElementById('star').content.firstElementChild;
  var current = null;
  var viewer = null;
  var developer = false;
  var visitor = null;
  var all = [];
  var favs = {};
  var past = [];
  var sort = 'new';
  var kind = 'all';
  var scope = 'all';
  var keep = 7 * 86400000;
  var serverNow = Date.now();
  document.getElementById('count').textContent = L.loading;
  function member() { return Boolean(visitor && visitor.linked !== false); }
  function url(cape) {
    if (cape.idle) return '/api/idle/' + cape.h;
    if (!cape.ids.length) return '/api/cape/' + visitor.id + '?k=' + cape.h;
    return '/api/cape/' + cape.ids[0] + '?v=' + cape.v + (developer || cape.own ? '&k=' + cape.h : '');
  }
  (function () {
    for (var i = 0; i < 10; i++) {
      var blank = el('div', 'card skel');
      blank.setAttribute('aria-hidden', 'true');
      blank.append(el('div', 'stage'), el('div', 'meta'));
      grid.append(blank);
    }
  })();

  function tell(text, good) {
    var msg = document.getElementById('wear-msg');
    msg.className = 'msg ' + (good ? 'ok' : 'bad');
    msg.textContent = text;
    if (text) flash(msg);
  }
  function send(path, body) {
    return api(path, { method: 'POST', body: JSON.stringify(body), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (answer) {
          answer.status = r.status;
          answer.good = r.ok;
          return answer;
        });
      });
  }

  function daysLeft(cape) { return Math.max(1, Math.ceil((cape.idle + keep - serverNow) / 86400000)); }

  function listNow() {
    function format(cape) { return kind === 'all' || cape.t === kind; }
    if (scope === 'hist') {
      return past.map(function (one) {
        var found = all.filter(function (cape) { return cape.h === one.h; })[0];
        return found || { h: one.h, t: one.t, s: one.s, x: one.x, y: one.y, v: one.at, c: one.at, ids: [], stars: 0, gone: true };
      }).filter(format);
    }
    var out = all.filter(function (cape) {
      if (!format(cape)) return false;
      if (scope === 'fav') return favs[cape.h];
      if (scope === 'unlisted' || scope === 'private') return cape.vis === scope && !cape.pending;
      if (scope === 'pending') return cape.pending;
      return true;
    });
    var by = {
      'new': function (a, b) { return (b.c || 0) - (a.c || 0); },
      old: function (a, b) { return (a.c || 0) - (b.c || 0); },
      worn: function (a, b) { return b.ids.length - a.ids.length || (b.c || 0) - (a.c || 0); },
      stars: function (a, b) { return (b.stars || 0) - (a.stars || 0) || b.ids.length - a.ids.length || (b.c || 0) - (a.c || 0); },
      recent: function (a, b) { return (b.v || 0) - (a.v || 0); }
    };
    return out.sort(by[sort] || by['new']);
  }

  function starLabel(button, cape) {
    var on = Boolean(favs[cape.h]);
    button.setAttribute('aria-pressed', on ? 'true' : 'false');
    button.setAttribute('aria-label', on ? L.favRemove : L.favAdd);
    button.title = on ? L.favRemove : L.favAdd;
  }
  function toggleStar(cape, button) {
    var on = !favs[cape.h];
    send('/cape/star', { hash: cape.h, on: on }).then(function (answer) {
      if (!answer.good) { if (dialog.open) tell(L.why[answer.why] || fmt(L.failed, { s: answer.status }), false); return; }
      if (answer.on) favs[cape.h] = true; else delete favs[cape.h];
      cape.stars = answer.stars;
      if (button && button.animate && !calm && answer.on) {
        button.animate([{ transform: 'scale(1)' }, { transform: 'scale(1.28)' }, { transform: 'scale(1)' }],
          { duration: 260, easing: 'cubic-bezier(0.23, 1, 0.32, 1)' });
      }
      render(true);
      if (current === cape) favState();
    }).catch(function () {});
  }

  function render(still) {
    var items = listNow();
    var count = document.getElementById('count');
    var parts = L.count.split('{n}');
    count.replaceChildren(parts[0], el('b', '', String(items.length)), parts[1] || '');
    grid.replaceChildren();
    if (!items.length) {
      grid.append(el('div', 'empty', scope === 'fav' && kind === 'all' ? L.emptyFav : scope === 'hist' && kind === 'all' ? L.emptyHist : all.length ? L.emptyFilter : L.empty));
    }
    items.forEach(function (cape, index) {
      var card = el('div', 'card');
      if (still) card.style.animation = 'none';
      else card.style.setProperty('--i', Math.min(index, 10));
      if (items.length <= 60) card.style.viewTransitionName = 'c' + cape.h.slice(0, 16);
      var open = el('button', 'open');
      open.type = 'button';
      open.setAttribute('aria-label', fmt(L.card, { t: cape.t.toUpperCase(), n: cape.ids.length }));
      var stage = el('div', 'stage');
      var face = el('div', 'face');
      var img = el('img');
      img.loading = 'lazy';
      img.alt = '';
      img.onload = function () { this.classList.add('in'); };
      img.onerror = function () { this.classList.add('in'); };
      img.src = url(cape);
      face.append(img);
      stage.append(face);
      var chips = el('div', 'marks');
      if (cape.t === 'gif') chips.append(el('span', 'tag gif', 'GIF'));
      if (member() && visitor.cape && visitor.cape.h === cape.h) chips.append(el('span', 'tag mine', L.tagMine));
      if (cape.pending) chips.append(el('span', 'tag wait', L.tagPending));
      else if (cape.hidden) chips.append(el('span', 'tag off', L.vis[cape.vis] || L.vis.unlisted));
      if (chips.firstChild) stage.append(chips);
      var meta = el('div', 'meta');
      if (cape.idle) {
        var left = daysLeft(cape);
        meta.append(el('span', 'who idle', left > 1 ? fmt(L.idleLeft, { n: left }) : L.idleLast));
      } else {
        meta.append(el('span', 'who', cape.ids.length ? fmt(L.wearing, { n: cape.ids.length }) : ''));
      }
      if (cape.stars) {
        var stars = el('span', 'stars');
        stars.append(starShape.cloneNode(true), String(cape.stars));
        meta.append(stars);
      }
      open.append(stage, meta);
      open.onclick = function () { show(cape); };
      card.append(open);
      if (member() && !cape.gone && !cape.hidden) {
        var star = el('button', 'star');
        star.type = 'button';
        star.append(starShape.cloneNode(true));
        starLabel(star, cape);
        star.onclick = function () { toggleStar(cape, star); };
        card.append(star);
      }
      grid.append(card);
    });
  }

  function favState() {
    var cape = current;
    var button = document.getElementById('fav');
    document.getElementById('d-stars').textContent = fmt(L.users, { n: (cape && cape.stars) || 0 });
    button.hidden = !(cape && member() && !cape.gone && !cape.hidden);
    if (!cape) return;
    button.textContent = favs[cape.h] ? L.favRemove : L.favAdd;
  }
  function wearState() {
    var cape = current;
    var can = Boolean(member() && !visitor.banned);
    document.getElementById('wearbox').hidden = !can;
    document.getElementById('wear-signin').hidden = member();
    if (!can || !cape) return;
    var mine = visitor.cape;
    var on = Boolean(mine && mine.h === cape.h);
    var note = document.getElementById('wear-note');
    note.hidden = !on;
    note.textContent = L.wearingThis;
    document.getElementById('wear').hidden = on;
    var back = document.getElementById('unwear');
    back.hidden = !(on && mine.worn);
    back.textContent = mine && mine.back ? L.restoreOwn : L.takeOff;
    document.getElementById('forget').hidden = !(scope === 'hist' && !on);
  }
  function show(cape) {
    current = cape;
    document.getElementById('flat').src = url(cape);
    document.getElementById('d-type').textContent = cape.t.toUpperCase();
    document.getElementById('d-size').textContent = bytes(cape.s);
    var dim = document.getElementById('d-dim');
    dim.textContent = cape.x ? cape.x + '×' + cape.y : '';
    document.getElementById('flat').onload = function () {
      if (!cape.x) dim.textContent = this.naturalWidth + '×' + this.naturalHeight;
    };
    document.getElementById('d-users').textContent = fmt(L.users, { n: cape.ids.length });
    var idleNote = document.getElementById('idle-note');
    idleNote.hidden = !cape.idle;
    if (cape.idle) idleNote.textContent = daysLeft(cape) > 1 ? fmt(L.idleNote, { n: daysLeft(cape) }) : L.idleNoteLast;
    document.getElementById('d-date').textContent = new Date(cape.c || cape.v).toLocaleDateString();
    document.getElementById('del').hidden = !developer || cape.gone;
    document.getElementById('ban').hidden = !developer || cape.gone;
    document.getElementById('review').hidden = !developer || cape.gone;
    document.getElementById('approve').hidden = !(developer && cape.pending);
    document.getElementById('share').hidden = Boolean(cape.gone || cape.pending || cape.vis === 'private');
    tell('', true);
    wearState();
    favState();
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
      var marks = [owner.pending ? L.tagPending : '', owner.vis && owner.vis !== 'public' ? L.vis[owner.vis] : '', owner.worn ? L.tagWorn : '']
        .filter(function (mark) { return mark; });
      li.append(el('span', '', (owner.name || owner.uuid) + (marks.length ? ' (' + marks.join(', ') + ')' : '')));
      owners.append(li);
    });
    dialog.showModal();
    var hint = document.getElementById('hint');
    if (!viewer) viewer = makeViewer(document.getElementById('model'));
    document.getElementById('skinpick').hidden = !viewer;
    if (!viewer) { hint.textContent = L.no3d; return; }
    hint.textContent = L.drag;
    pickSkin();
    slideAll();
    wear(viewer, url(cape), cape.t === 'gif');
  }
  function wearer() { return member() && visitor.name ? visitor.name : 'Green_teaTW'; }
  var skinButtons = Array.prototype.slice.call(document.querySelectorAll('#skinpick button'));
  document.getElementById('skinpick').setAttribute('aria-label', L.skinPick);
  skinButtons[1].textContent = L.skinPlain;
  function pickSkin() {
    var mode = skinMode();
    skinButtons[0].textContent = wearer();
    skinButtons.forEach(function (button) { button.setAttribute('aria-pressed', button.dataset.s === mode ? 'true' : 'false'); });
    if (viewer) dress(viewer, wearer());
  }
  skinButtons.forEach(function (button) {
    button.onclick = function () { setSkinMode(button.dataset.s); pickSkin(); };
  });
  function shut() { stopCape(); current = null; if (dialog.open) dialog.close(); }
  document.getElementById('close').onclick = shut;
  dialog.addEventListener('close', stopCape);
  dialog.addEventListener('click', function (e) { if (e.target === dialog) shut(); });

  function wearCall(path, body, done) {
    var buttons = [document.getElementById('wear'), document.getElementById('unwear')];
    buttons.forEach(function (button) { button.disabled = true; });
    send(path, body).then(function (answer) {
      if (!answer.good) { tell(L.why[answer.why] || fmt(L.failed, { s: answer.status }), false); return null; }
      return api('/me').then(function (again) { return again.ok ? again.json() : visitor; }).then(function (me) {
        visitor = me;
        done(answer);
        return load(true);
      }).then(wearState);
    }).catch(function () { tell(L.offline, false); })
      .then(function () { buttons.forEach(function (button) { button.disabled = false; }); });
  }
  document.getElementById('wear').onclick = function () {
    if (!current) return;
    wearCall('/cape/wear', { hash: current.h }, function (answer) {
      tell(answer.pending ? L.wornPending : answer.back ? L.wornKept : L.wornOk, true);
    });
  };
  document.getElementById('unwear').onclick = function () {
    wearCall('/cape/restore', {}, function (answer) { tell(answer.cape ? L.restored : L.tookOff, true); });
  };
  document.getElementById('fav').onclick = function () { if (current) toggleStar(current, null); };
  document.getElementById('share').onclick = function () {
    var button = this;
    if (!current || !navigator.clipboard) return;
    navigator.clipboard.writeText(location.origin + '/capes?c=' + current.h)
      .then(function () { blip(button, L.copied); }).catch(function () {});
  };
  function openLinked() {
    var asked = new URLSearchParams(location.search).get('c') || '';
    if (!/^[0-9a-f]{64}$/.test(asked)) return;
    var found = all.filter(function (cape) { return cape.h === asked; })[0];
    if (found) { show(found); return; }
    api('/capes/' + asked).then(function (r) { return r.ok ? r.json() : null; }).then(function (got) {
      if (!got || !got.cape) {
        var gone = document.getElementById('gone');
        gone.textContent = L.linkGone;
        gone.hidden = false;
        return;
      }
      show(got.cape);
    }).catch(function () {});
  }
  document.getElementById('forget').onclick = function () {
    var cape = current;
    if (!cape || !confirm(L.confirmForget)) return;
    send('/cape/forget', { hash: cape.h }).then(function (answer) {
      if (!answer.good) { tell(L.why[answer.why] || fmt(L.failed, { s: answer.status }), false); return; }
      shut();
      load(true);
    }).catch(function () { tell(L.offline, false); });
  };

  function remove(ban) {
    var cape = current;
    if (!cape) return;
    if (!confirm(fmt(ban ? L.confirmBan : L.confirmDel, { n: cape.ids.length }))) return;
    var body = { hash: cape.h };
    if (ban) body.ban = true;
    send('/admin/remove', body).then(function (answer) {
      if (answer.good) { shut(); load(true); }
      else alert(fmt(L.expired, { s: answer.status }));
    });
  }
  document.getElementById('del').onclick = function () { remove(false); };
  document.getElementById('ban').onclick = function () { remove(true); };
  document.getElementById('approve').onclick = function () {
    var cape = current;
    if (!cape) return;
    send('/admin/approve', { hash: cape.h }).then(function (answer) {
      if (!answer.good) { tell(fmt(L.failed, { s: answer.status }), false); return; }
      tell(L.approved, true);
      document.getElementById('approve').hidden = true;
      load(true);
    });
  };
  document.getElementById('review').onclick = function () {
    var cape = current;
    if (!cape || !confirm(L.confirmReview)) return;
    send('/admin/review', { hash: cape.h }).then(function (answer) {
      if (!answer.good) { tell(fmt(L.failed, { s: answer.status }), false); return; }
      tell(answer.added ? fmt(L.reviewed, { n: answer.added }) : L.reviewedNone, true);
    });
  };

  if (location.hash === '#template') document.getElementById('template').open = true;

  function reorder() {
    if (!document.startViewTransition || calm || document.visibilityState === 'hidden') { render(false); return; }
    document.startViewTransition(function () { render(true); });
  }
  document.getElementById('kind').onchange = function () { kind = this.value; reorder(); };
  document.getElementById('scope').onchange = function () {
    scope = this.value;
    document.getElementById('sort').disabled = scope === 'hist';
    reorder();
  };
  document.getElementById('sort').onchange = function () { sort = this.value; reorder(); };

  function load(still) {
    var list = api(developer ? '/admin/capes' : '/capes').then(function (r) { return r.json(); });
    var mine = member() ? api('/capes/mine').then(function (r) { return r.ok ? r.json() : null; }).catch(function () { return null; })
      : Promise.resolve(null);
    return Promise.all([list, mine]).then(function (got) {
      all = got[0].capes;
      if (got[0].keep) keep = got[0].keep;
      if (got[0].now) serverNow = got[0].now;
      if (got[1]) {
        favs = {};
        got[1].favs.forEach(function (hash) { favs[hash] = true; });
        past = got[1].history;
        if (got[1].removed) {
          var gone = document.getElementById('gone');
          gone.textContent = fmt(L.removedNote, { n: got[1].removed });
          gone.hidden = false;
        }
      }
      all.forEach(function (cape) { if (member() && visitor.cape && visitor.cape.h === cape.h) cape.own = true; });
      if (current) {
        var again = all.filter(function (cape) { return cape.h === current.h; })[0];
        if (again) { current = again; favState(); }
      }
      render(still);
    });
  }
  whoami.then(function (me) {
    developer = Boolean(me && me.staff);
    visitor = me;
    /* option 的 hidden 有的瀏覽器不理：直接拿掉 */
    var scopes = document.getElementById('scope');
    Array.prototype.slice.call(scopes.options).forEach(function (option) {
      var f = option.value;
      if (((f === 'fav' || f === 'hist') && !member()) || ((f === 'unlisted' || f === 'private' || f === 'pending') && !developer)) option.remove();
    });
    document.getElementById('scope-box').hidden = scopes.options.length < 2;
    slideAll();
    return load(false).then(openLinked);
  }).catch(function () { document.getElementById('count').textContent = L.readFail; });
`);

const ACCOUNT = String.raw`
  <h1>%%account.h1%%</h1>
  <p class="notice" id="wiped" role="status" hidden>%%account.del.done%%</p>
  <div id="out" hidden>
    <p class="lede">%%account.lede%%</p>
    <div class="split" style="margin-top:22px">
      <div class="panel">
        <b>%%account.discord.h%%</b>
        <p class="note" style="margin:8px 0 14px">%%account.discord.p%%</p>
        <a class="btn primary" id="discord" href="/auth/discord" hidden>%%account.discord.btn%%</a>
        <p class="note" id="discord-off" hidden style="margin:0">%%account.discord.off%%</p>
        <p class="msg bad" id="oauth-error" hidden>%%account.discord.err%%</p>
      </div>
      <div class="panel">
        <b>%%account.how%%</b>
        <ol class="steps">
          <li>%%account.s1%%</li>
          <li>%%account.s2%%</li>
          <li>%%account.s3%%</li>
        </ol>
        <p class="note" style="margin:10px 0 0">%%account.expire%%</p>
      </div>
    </div>
  </div>
  <div id="link" hidden>
    <div class="panel" style="margin-top:22px;max-width:680px">
      <b id="dc-name"></b>
      <h2 style="margin:14px 0 6px">%%account.link.h%%</h2>
      <p class="note" style="margin:0 0 12px">%%account.link.p%%</p>
      <button class="btn primary" id="getcode">%%account.link.get%%</button>
      <div id="codebox" hidden>
        <p class="note" style="margin:0">%%account.link.type%%</p>
        <div class="linkcode"><code id="cmd"></code><button class="btn" id="copy">%%account.link.copy%%</button></div>
        <p class="note" style="margin:0">%%account.link.wait%%</p>
      </div>
      <p class="msg" id="linkmsg" role="status"></p>
      <div style="margin-top:8px"><button class="btn" id="signout2">%%account.signout%%</button></div>
    </div>
  </div>
  <div id="in" hidden>
    <div class="account" style="margin-top:22px">
      <div class="panel">
        <div class="who-am-i"><img id="avatar" alt=""><div><b id="name"></b><div class="note" id="roles"></div></div></div>
        <p class="note" id="dc-line" hidden style="margin:12px 0 0"></p>
        <p class="note" id="dc-hint" hidden style="margin:12px 0 0">%%account.discord.hint%%</p>
        <p class="note" id="banned" hidden style="color:var(--danger);margin:12px 0 0">%%account.banned%%</p>
        <div style="margin-top:16px;display:flex;flex-wrap:wrap;gap:8px">
          <button class="btn" id="signout">%%account.signout%%</button>
          <button class="btn" id="unlink" hidden>%%account.unlink%%</button>
          <button class="btn" id="discord2" hidden>%%account.discord.bind%%</button>
        </div>
      </div>
      <div class="panel">
        <b>%%account.mine%%</b>
        <p class="note" id="nocape" style="margin:8px 0 0">%%account.none%%</p>
        <canvas class="model" id="mine" width="400" height="320" hidden style="height:320px;border-radius:8px;margin-top:10px"></canvas>
        <p class="hint" id="minehint" hidden style="margin-top:6px"></p>
        <p class="note" id="worn" hidden style="margin:10px 0 0">%%account.worn%%</p>
        <p class="note" id="pending" hidden style="margin:10px 0 0;color:var(--gold)">%%account.pending%%</p>
        <p class="note" id="review" hidden style="margin:10px 0 0">%%account.review%%</p>
        <div class="set-line vis-line">
          <span><b id="vis-label">%%account.vis%%</b><span class="hint" id="vis-hint"></span></span>
          <div class="seg" id="vis" role="group" aria-labelledby="vis-label">
            <button type="button" data-v="public" aria-pressed="true">%%account.vis.public%%</button>
            <button type="button" data-v="unlisted" aria-pressed="false">%%account.vis.unlisted%%</button>
            <button type="button" data-v="private" aria-pressed="false">%%account.vis.private%%</button>
          </div>
          <p class="hint" style="margin:0">%%account.vis.game%%</p>
        </div>
        <div id="sharebox" hidden>
          <p class="note" style="margin:12px 0 0">%%account.share%%</p>
          <div class="linkcode"><code class="share-url" id="share-url"></code><button class="btn" id="share-copy">%%account.link.copy%%</button></div>
        </div>
        <div style="margin-top:12px;display:flex;flex-wrap:wrap;gap:8px">
          <button class="btn" id="restore" hidden></button>
          <button class="btn danger" id="remove" hidden>%%account.remove%%</button>
        </div>
      </div>
    </div>
    <h2 id="template">%%account.upload%%</h2>
    ` + TEMPLATE_BLOCK.replace('<a href="/account">%%tpl.upload%%</a>', '') + String.raw`
    <div class="panel" style="max-width:720px;margin-top:14px">
      <p class="note" style="margin:0 0 12px">%%account.spec%%</p>
      <p class="note" style="margin:0 0 12px">%%rules%%</p>
      <div class="drop" id="drop">
        <input type="file" id="file" accept="image/png,image/gif" aria-label="%%account.pick%%">
        <button class="btn primary" id="upload">%%account.send%%</button>
        <p class="hint">%%account.drop%%</p>
      </div>
      <p class="msg" id="msg" role="status"></p>
    </div>
  </div>
  <div class="panel wipe-panel" id="wipe-panel" hidden>
    <b>%%account.del.h%%</b>
    <p class="note" style="margin:8px 0 14px">%%account.del.p%%</p>
    <button class="btn danger" id="wipe-open" type="button">%%account.del.btn%%</button>
  </div>
  <dialog id="wipe" aria-labelledby="wipe-title">
    <div class="wipe-box">
      <h2 id="wipe-title">%%account.del.h%%</h2>
      %%account.del.list%%
      <p class="msg" id="wipe-msg" role="status"></p>
      <div class="wipe-actions">
        <button class="btn" id="wipe-no" type="button">%%account.del.cancel%%</button>
        <button class="btn danger" id="wipe-go" type="button">%%account.del.go%%</button>
      </div>
    </div>
  </dialog>
`;

const ACCOUNT_JS = SKINVIEW + scriptFile('account', VIEWER + String.raw`
  var msg = document.getElementById('msg');
  function say(text, ok) { msg.textContent = text; msg.className = 'msg ' + (ok ? 'ok' : 'bad'); flash(msg); }
  var viewer = null;
  var myName = null;
  function ready(canvas) {
    if (!viewer) {
      viewer = makeViewer(canvas);
      if (viewer) dress(viewer, myName);
    }
    return viewer;
  }
  var trying = null;
  function tryOn(file, w, h) {
    var canvas = document.getElementById('mine');
    var hint = document.getElementById('minehint');
    canvas.hidden = false;
    if (!ready(canvas)) { canvas.hidden = true; return; }
    if (trying) URL.revokeObjectURL(trying);
    trying = URL.createObjectURL(file);
    document.getElementById('nocape').hidden = true;
    hint.hidden = false;
    hint.textContent = fmt(L.previewHint, { w: w, h: h });
    wear(viewer, trying, file.type === 'image/gif');
  }

  var vis = 'public';
  var lastMe = null;
  function setVis(value) {
    vis = value;
    Array.prototype.forEach.call(document.querySelectorAll('#vis button'), function (button) {
      button.setAttribute('aria-pressed', button.dataset.v === value ? 'true' : 'false');
    });
    document.getElementById('vis-hint').textContent = L.visHint[value];
    slide(document.getElementById('vis'));
  }
  function shareState(me) {
    var open = Boolean(me && me.cape && !me.cape.pending && vis !== 'private');
    document.getElementById('sharebox').hidden = !open;
    if (open) document.getElementById('share-url').textContent = location.origin + '/capes?c=' + me.cape.h;
  }
  function show(me) {
    lastMe = me;
    if (me.name && me.name !== myName) { myName = me.name; if (viewer) dress(viewer, myName); }
    document.getElementById('nocape').hidden = Boolean(me.cape);
    setVis(me.cape ? me.cape.vis || (me.cape.public === false ? 'unlisted' : 'public') : vis);
    shareState(me);
    document.getElementById('pending').hidden = !(me.cape && me.cape.pending);
    document.getElementById('review').hidden = !me.review;
    document.getElementById('remove').hidden = !me.cape;
    var worn = Boolean(me.cape && me.cape.worn);
    document.getElementById('worn').hidden = !worn;
    document.getElementById('restore').hidden = !worn;
    document.getElementById('restore').textContent = me.cape && me.cape.back ? L.restoreOwn : L.takeOff;
    var canvas = document.getElementById('mine');
    var hint = document.getElementById('minehint');
    canvas.hidden = !me.cape;
    hint.hidden = !me.cape;
    if (trying) { URL.revokeObjectURL(trying); trying = null; }
    if (!me.cape) { stopCape(); return; }
    if (!ready(canvas)) { hint.textContent = L.mineNo3d; canvas.hidden = true; return; }
    hint.textContent = fmt(L.mineHint, { t: me.cape.t.toUpperCase(), s: bytes(me.cape.s),
      d: me.cape.x ? me.cape.x + '×' + me.cape.y + ' · ' : '' });
    wear(viewer, '/api/cape/' + me.id + '?v=' + me.cape.v + '&k=' + me.cape.h, me.cape.t === 'gif');
  }
  function refresh() {
    return api('/me').then(function (r) { return r.ok ? r.json() : null; }).then(function (me) { if (me) show(me); });
  }

  var discordOn = fetch('/api/status').then(function (r) { return r.json(); })
    .then(function (status) { return Boolean(status.discord); }).catch(function () { return false; });
  var linkMsg = document.getElementById('linkmsg');
  var limit = 20 * 1024 * 1024;
  var SIZES_OK = [[22, 17], [44, 34], [88, 68], [176, 136], [352, 272], [704, 544], [1056, 816]];
  var watching = null;

  function linking(me) {
    document.getElementById('dc-name').textContent = fmt(L.discordAs, { n: (me.discord && me.discord.name) || '' });
    document.getElementById('getcode').onclick = function () {
      linkMsg.textContent = '';
      api('/link/start', { method: 'POST', body: '{}', headers: { 'content-type': 'application/json' } })
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (answer) {
          if (!answer || !answer.code) { linkMsg.className = 'msg bad'; linkMsg.textContent = L.codeFail; return; }
          document.getElementById('cmd').textContent = '/wcy link ' + answer.code;
          document.getElementById('codebox').hidden = false;
          if (watching) clearInterval(watching);
          watching = setInterval(function () {
            api('/me').then(function (r) { return r.ok ? r.json() : null; })
              .then(function (now) { if (now && now.linked) location.reload(); });
          }, 3000);
        }).catch(function () { linkMsg.className = 'msg bad'; linkMsg.textContent = L.offline; });
    };
    document.getElementById('copy').onclick = function () {
      var text = document.getElementById('cmd').textContent;
      var button = this;
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(function () {
          blip(button, L.copied);
        }).catch(function () {});
      }
    };
    document.getElementById('signout2').onclick = signOut;
  }

  whoami.then(function (me) {
    if (!me) {
      document.getElementById('out').hidden = false;
      if (location.hash === '#wiped') {
        document.getElementById('wiped').hidden = false;
        history.replaceState(null, '', location.pathname + location.search);
        window.scrollTo(0, 0);
      }
      var oauthFail = /e=discord(?:&r=([a-z0-9_-]{1,60}))?$/.exec(location.hash);
      var oauthError = document.getElementById('oauth-error');
      oauthError.hidden = !oauthFail;
      if (oauthFail && oauthFail[1]) oauthError.textContent += ' (' + oauthFail[1] + ')';
      discordOn.then(function (on) {
        document.getElementById('discord').hidden = !on;
        document.getElementById('discord-off').hidden = on;
      });
      return;
    }
    document.getElementById('wipe-panel').hidden = false;
    if (me.linked === false) {
      document.getElementById('link').hidden = false;
      linking(me);
      return;
    }
    document.getElementById('in').hidden = false;
    if (me.limit) limit = me.limit;
    if (me.discord) {
      var line = document.getElementById('dc-line');
      line.hidden = false;
      line.textContent = fmt(L.discordAs, { n: me.discord.name || '' });
      document.getElementById('unlink').hidden = false;
    } else {
      discordOn.then(function (on) {
        document.getElementById('dc-hint').hidden = !on;
        document.getElementById('discord2').hidden = !on;
      });
    }
    document.getElementById('avatar').src = head(me.name, 96);
    document.getElementById('name').textContent = me.name;
    document.getElementById('roles').textContent = me.developer ? L.roleDev : (me.member ? L.roleMember : L.roleUser);
    document.getElementById('banned').hidden = !me.banned;
    show(me);
  });
  document.getElementById('signout').onclick = signOut;
  document.getElementById('discord2').onclick = function () {
    api('/link/intent', { method: 'POST', body: '{}', headers: { 'content-type': 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (answer) {
        if (answer && String(answer.url).indexOf('/auth/discord?intent=') === 0) location.href = answer.url;
        else say(L.bindFail, false);
      }).catch(function () { say(L.offline, false); });
  };
  document.getElementById('file').onchange = function () {
    var file = this.files[0];
    if (!file) return;
    var probe = new Image();
    var address = URL.createObjectURL(file);
    probe.onload = function () {
      var w = probe.naturalWidth;
      var h = probe.naturalHeight;
      URL.revokeObjectURL(address);
      var fits = SIZES_OK.some(function (size) { return size[0] === w && size[1] === h; });
      if (!fits) say(fmt(L.pickedBad, { w: w, h: h }), false);
      else if (file.size > limit) say(L.why['too large'], false);
      else { say(fmt(L.picked, { w: w, h: h, s: bytes(file.size) }), true); tryOn(file, w, h); }
    };
    probe.onerror = function () { URL.revokeObjectURL(address); say(L.why['bad format'], false); };
    probe.src = address;
  };
  (function () {
    var drop = document.getElementById('drop');
    var input = document.getElementById('file');
    var depth = 0;
    function has(e) { return e.dataTransfer && Array.prototype.indexOf.call(e.dataTransfer.types || [], 'Files') >= 0; }
    drop.addEventListener('dragenter', function (e) { if (!has(e)) return; e.preventDefault(); depth++; drop.classList.add('over'); });
    drop.addEventListener('dragover', function (e) { if (has(e)) e.preventDefault(); });
    drop.addEventListener('dragleave', function () { depth = Math.max(0, depth - 1); if (!depth) drop.classList.remove('over'); });
    drop.addEventListener('drop', function (e) {
      if (!has(e)) return;
      e.preventDefault();
      depth = 0;
      drop.classList.remove('over');
      if (!e.dataTransfer.files.length) return;
      input.files = e.dataTransfer.files;
      input.onchange();
    });
  })();
  document.getElementById('unlink').onclick = function () {
    if (!confirm(L.confirmUnlink)) return;
    api('/unlink', { method: 'POST', body: '{}', headers: { 'content-type': 'application/json' } })
      .then(function () { location.reload(); });
  };
  Array.prototype.forEach.call(document.querySelectorAll('#vis button'), function (button) {
    button.onclick = function () {
      var before = vis;
      var want = button.dataset.v;
      if (want === before) return;
      setVis(want);
      if (!lastMe || !lastMe.cape) { shareState(lastMe); return; }
      api('/cape/visibility', { method: 'POST', body: JSON.stringify({ vis: want }), headers: { 'content-type': 'application/json' } })
        .then(function (r) {
          if (!r.ok) throw new Error('vis');
          lastMe.cape.vis = want;
          shareState(lastMe);
        }).catch(function () { setVis(before); shareState(lastMe); say(L.tryLater, false); });
    };
  });
  document.getElementById('share-copy').onclick = function () {
    var button = this;
    if (navigator.clipboard) {
      navigator.clipboard.writeText(document.getElementById('share-url').textContent)
        .then(function () { blip(button, L.copied); }).catch(function () {});
    }
  };
  var wipe = document.getElementById('wipe');
  document.getElementById('wipe-open').onclick = function () {
    document.getElementById('wipe-msg').textContent = '';
    wipe.showModal();
  };
  document.getElementById('wipe-no').onclick = function () { wipe.close(); };
  wipe.addEventListener('click', function (e) { if (e.target === wipe) wipe.close(); });
  document.getElementById('wipe-go').onclick = function () {
    var button = this;
    button.disabled = true;
    button.setAttribute('aria-busy', 'true');
    var erase = function (attempt) {
      return api('/me/delete', { method: 'POST', body: '{}', headers: { 'content-type': 'application/json' } }).then(function (r) {
        if (r.status === 202 && attempt < 30) {
          return new Promise(function (resolve) { setTimeout(resolve, 1000); }).then(function () { return erase(attempt + 1); });
        }
        if (r.status !== 200) throw new Error('delete');
      });
    };
    erase(0).then(function () {
      try { localStorage.removeItem(TOKEN_KEY); } catch (e) {}
      history.replaceState(null, '', '/account#wiped');
      location.reload();
    }).catch(function () {
      button.disabled = false;
      button.removeAttribute('aria-busy');
      var out = document.getElementById('wipe-msg');
      out.className = 'msg bad';
      out.textContent = L.wipeFail;
      flash(out);
    });
  };
  document.getElementById('restore').onclick = function () {
    api('/cape/restore', { method: 'POST', body: '{}', headers: { 'content-type': 'application/json' } }).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (answer) {
        if (r.ok) { say(answer.cape ? L.restored : L.tookOff, true); refresh(); }
        else say(L.why[answer.why] || L.tryLater, false);
      });
    }).catch(function () { say(L.offline, false); });
  };
  document.getElementById('remove').onclick = function () {
    if (!confirm(L.confirmRemove)) return;
    api('/cape', { method: 'DELETE' }).then(function (r) {
      if (r.ok) { say(L.removed, true); refresh(); } else say(L.tryLater, false);
    });
  };
  document.getElementById('upload').onclick = function () {
    var file = document.getElementById('file').files[0];
    if (!file) { say(L.pickFirst, false); return; }
    if (file.size > limit) { say(L.why['too large'], false); return; }
    var button = this;
    button.disabled = true;
    button.setAttribute('aria-busy', 'true');
    function idle() { button.disabled = false; button.removeAttribute('aria-busy'); }
    say(L.uploading, true);
    api('/cape?vis=' + vis, { method: 'PUT', body: file, headers: { 'content-type': file.type || 'application/octet-stream' } })
      .then(function (r) {
        idle();
        return r.json().catch(function () { return {}; }).then(function (body) {
          if (r.ok) { say(fmt(body.pending ? L.uploadedPending : L.uploaded, { w: body.width, h: body.height }), true); refresh(); }
          else if (body.why === 'cooldown' && body.until) say(fmt(L.why.cooldown, { t: new Date(body.until).toLocaleString(document.documentElement.lang) }), false);
          else say(L.why[body.why] || fmt(L.failed, { s: r.status }), false);
        });
      }).catch(function () { say(L.offline, false); });
  };
`);

const FEEDBACK_CAT_OPTIONS = ['translation', 'content', 'website', 'feature', 'other']
  .map((c) => '<option value="' + c + '">%%fb.cat.' + c + '%%</option>').join('');

const FEEDBACK = String.raw`
  <h1>%%fb.h1%%</h1>
  <p class="lede">%%fb.lede%%</p>
  <p class="panel fb-gate" id="fb-gate">%%fb.gate%%</p>
  <p class="panel fb-gate" id="fb-gatelink" hidden>%%fb.gatelink%%</p>
  <form class="panel fb-form" id="fb-form" hidden novalidate>
    <p class="note fb-as" id="fb-as" hidden></p>
    <div class="form">
      <div><label for="fb-cat">%%fb.cat%%</label><select id="fb-cat">` + FEEDBACK_CAT_OPTIONS + String.raw`</select></div>
      <div id="fb-langbox" hidden><label for="fb-lang">%%fb.lang%%</label><select id="fb-lang"></select></div>
    </div>
    <label for="fb-text">%%fb.msg%%</label>
    <textarea id="fb-text" class="tr-text" rows="7" maxlength="2000" required aria-describedby="fb-count"></textarea>
    <p class="hint fb-count" id="fb-count" aria-live="polite"></p>
    <label for="fb-where" id="fb-wherelabel">%%fb.where%%</label>
    <input type="text" id="fb-where" maxlength="200" autocomplete="off" aria-describedby="fb-wherehint">
    <p class="hint" id="fb-wherehint">%%fb.wherehint%%</p>
    <div class="tr-actions" style="margin-top:16px"><button class="btn primary" id="fb-send" type="submit">%%fb.send%%</button></div>
    <p class="msg fb-out" id="fb-out" role="status"></p>
  </form>
  <div class="fb-notes">
    <p class="note">%%fb.note%%</p>
    <p class="note">%%fb.bugs%%</p>
  </div>
`;

const FEEDBACK_JS = scriptFile('feedback', String.raw`
  var F = L.fb;
  var MAX = 2000;
  var form = document.getElementById('fb-form');
  var gate = document.getElementById('fb-gate');
  var cat = document.getElementById('fb-cat');
  var langBox = document.getElementById('fb-langbox');
  var langSel = document.getElementById('fb-lang');
  var text = document.getElementById('fb-text');
  var count = document.getElementById('fb-count');
  var where = document.getElementById('fb-where');
  var out = document.getElementById('fb-out');
  var send = document.getElementById('fb-send');
  var SITE_TO = { zh: 'zh_tw', 'zh-cn': 'zh_cn', ja: 'ja_jp', ko: 'ko_kr', ru: 'ru_ru', es: 'es_es', tr: 'tr_tr' };
  var none = el('option', '', F.langNone);
  none.value = '';
  langSel.append(none);
  ['zh_tw', 'zh_cn', 'ja_jp', 'ko_kr', 'ru_ru', 'es_es', 'tr_tr'].forEach(function (code) {
    var o = el('option', '', L.tr.langs[code] || code);
    o.value = code;
    langSel.append(o);
  });
  var pageLang = document.documentElement.lang.toLowerCase();
  langSel.value = SITE_TO[pageLang] || SITE_TO[pageLang.split('-')[0]] || (pageLang === 'zh-hans' ? 'zh_cn' : '');
  var asked = new URLSearchParams(location.search).get('cat');
  if (asked && cat.querySelector('option[value="' + asked.replace(/[^a-z]/g, '') + '"]')) cat.value = asked;
  function shape() {
    var tr = cat.value === 'translation';
    langBox.hidden = !tr;
    document.getElementById('fb-wherelabel').textContent = tr ? F.whereTr : F.whereAny;
    where.placeholder = tr ? F.wherePhTr : F.wherePh;
  }
  cat.onchange = shape;
  shape();
  function counter() {
    count.textContent = fmt(F.count, { n: text.value.length, max: MAX });
    count.classList.toggle('bad', text.value.length > MAX);
  }
  text.addEventListener('input', counter);
  counter();
  function say(ok, message) {
    out.className = 'msg fb-out ' + (ok ? 'ok' : 'bad');
    out.textContent = message;
    flash(out);
  }
  whoami.then(function (me) {
    if (!me) return;
    gate.hidden = true;
    form.hidden = false;
    var name = me.discord && me.discord.name;
    if (name) {
      var as = document.getElementById('fb-as');
      as.textContent = fmt(F.as, { n: name });
      as.hidden = false;
    }
  });
  form.onsubmit = function (e) {
    e.preventDefault();
    if (text.value.trim().length < 3) { say(false, F.why['too short']); text.focus(); return; }
    var tr = cat.value === 'translation';
    send.disabled = true;
    say(true, L.sending);
    api('/feedback', {
      method: 'POST',
      body: JSON.stringify({ cat: cat.value, msg: text.value, where: where.value, lang: tr ? langSel.value : '' }),
      headers: { 'content-type': 'application/json' }
    }).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (b) {
        send.disabled = false;
        if (!r.ok) {
          if (b.why === 'discord first' || b.why === 'login first') {
            form.hidden = true;
            document.getElementById(b.why === 'discord first' ? 'fb-gatelink' : 'fb-gate').hidden = false;
            return;
          }
          if (b.why === 'slow down') { say(false, fmt(F.why['slow down'], { t: new Date(b.until).toLocaleTimeString() })); return; }
          say(false, F.why[b.why] || fmt(L.failed, { s: b.why || r.status }));
          return;
        }
        say(true, F.done);
        if (b.issue && String(b.issue).indexOf('https://github.com/') === 0) {
          var link = el('a', '', F.issueLink);
          link.href = b.issue;
          link.rel = 'noopener';
          out.append(' ', link);
        } else if (b.issueFailed) {
          out.append(' ' + F.issueFail);
        }
        text.value = '';
        where.value = '';
        counter();
      });
    }).catch(function () { send.disabled = false; say(false, L.offline); });
  };
`);

const ADMIN = String.raw`
  <h1>%%admin.h1%%</h1>
  <p class="lede" id="gate">%%admin.gate%%</p>
  <div id="panel" hidden>
    <p class="lede">%%admin.lede%%</p>

    <h2>%%admin.feedback%%</h2>
    <div class="panel">
      <p class="note" style="margin:0 0 14px">%%admin.feedbacknote%%</p>
      <div class="tr-search">
        <select class="pick" id="fbf-cat" aria-label="%%fb.cat%%"><option value="">%%admin.fball%%</option>` + FEEDBACK_CAT_OPTIONS + String.raw`</select>
        <select class="pick" id="fbf-st" aria-label="%%admin.fbst%%"><option value="open">%%admin.fbopen%%</option><option value="done">%%admin.fbdone%%</option><option value="">%%admin.fball%%</option></select>
        <span class="note" id="fbf-counts"></span>
      </div>
    </div>
    <div id="fbf-list" class="tr-list" style="margin-top:10px"></div>
    <p class="note" id="nofeedback" hidden style="margin:10px">%%admin.nofeedback%%</p>

    <h2>%%admin.bans%%</h2>
    <div class="panel scroll">
      <table>
        <thead><tr><th>%%admin.player%%</th><th>UUID</th><th>%%admin.when%%</th><th>%%admin.by%%</th><th></th></tr></thead>
        <tbody id="bans"></tbody>
      </table>
      <p class="note" id="nobans" hidden style="margin:10px">%%admin.nobans%%</p>
    </div>

    <h2>%%admin.reviews%%</h2>
    <div class="panel">
      <p class="note" style="margin:0 0 14px">%%admin.reviewsnote%%</p>
      <div class="form">
        <div><label for="r-uuid">%%admin.uuid%%</label><input type="text" id="r-uuid" maxlength="36" placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" autocomplete="off" spellcheck="false"></div>
        <div><label for="r-note">%%admin.note%%</label><input type="text" id="r-note" maxlength="60" autocomplete="off"></div>
        <div><button class="btn primary" id="r-send">%%admin.grant%%</button></div>
      </div>
      <p class="msg" id="r-msg" role="status"></p>
    </div>
    <div class="panel scroll">
      <table>
        <thead><tr><th>%%admin.player%%</th><th>UUID</th><th>%%admin.note%%</th><th>%%admin.pwhen%%</th><th>%%admin.pby%%</th><th></th></tr></thead>
        <tbody id="reviews"></tbody>
      </table>
      <p class="note" id="noreviews" hidden style="margin:10px">%%admin.noreviews%%</p>
    </div>

    <h2>%%admin.perks%%</h2>
    <div class="panel">
      <p class="note" style="margin:0 0 14px">%%admin.perksnote%%</p>
      <div class="form">
        <div><label for="p-uuid">%%admin.uuid%%</label><input type="text" id="p-uuid" maxlength="36" placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" autocomplete="off" spellcheck="false"></div>
        <div><label for="p-note">%%admin.note%%</label><input type="text" id="p-note" maxlength="60" autocomplete="off"></div>
        <div><button class="btn primary" id="p-send">%%admin.grant%%</button></div>
      </div>
      <p class="msg" id="p-msg" role="status"></p>
    </div>
    <div class="panel scroll">
      <table>
        <thead><tr><th>UUID</th><th>%%admin.note%%</th><th>%%admin.pwhen%%</th><th>%%admin.pby%%</th><th></th></tr></thead>
        <tbody id="perks"></tbody>
      </table>
      <p class="note" id="noperks" hidden style="margin:10px">%%admin.noperks%%</p>
    </div>

    <h2>%%admin.add%%</h2>
    <div class="panel">
      <p class="note" style="margin:0 0 14px">%%admin.addnote%%</p>
      <div class="form">
        <div><label for="c-section">%%admin.section%%</label>
          <select id="c-section"><option value="contributors">%%admin.contributors%%</option><option value="sponsors">%%admin.sponsors%%</option><option value="developers">%%admin.developers%%</option></select></div>
        <div><label for="c-name">%%admin.name%%</label><input type="text" id="c-name" maxlength="40" autocomplete="off"></div>
        <div><label for="c-mc">%%admin.mc%%</label><input type="text" id="c-mc" maxlength="16" autocomplete="off" spellcheck="false"></div>
        <div><label for="c-color">%%admin.color%%</label><input type="text" id="c-color" maxlength="7" placeholder="#RRGGBB" autocomplete="off" spellcheck="false"></div>
        <div><button class="btn primary" id="c-send">%%admin.send%%</button></div>
      </div>
      <p class="hint" id="c-teams" hidden></p>
      <p class="msg" id="c-msg" role="status"></p>
    </div>

    <h2 id="langteam">%%admin.lt%%</h2>
    <div class="panel">
      <p class="note" style="margin:0 0 14px">%%admin.ltnote%%</p>
      <div class="form">
        <div class="finder"><label for="t-who">%%admin.who%%</label><input type="text" id="t-who" maxlength="40" autocomplete="off" spellcheck="false" placeholder="%%admin.findph%%" role="combobox" aria-autocomplete="list" aria-expanded="false" aria-controls="t-hits"><ul class="finder-hits" id="t-hits" role="listbox" hidden></ul></div>
        <div><label for="t-note">%%admin.note%%</label><input type="text" id="t-note" maxlength="60" autocomplete="off"></div>
      </div>
      <p class="hint" id="t-pick" style="margin-top:8px"></p>
      <div class="lt-state" id="lt-state" hidden></div>
      <fieldset class="tr-langs" id="t-langs"><legend>%%admin.ltlangs%%</legend></fieldset>
      <fieldset class="tr-langs" id="lt-areas"><legend>%%admin.tareas%%</legend></fieldset>
      <div class="tr-actions"><button class="btn primary" id="lt-save" type="button" disabled>%%admin.ltsave%%</button><button class="btn" id="lt-join" type="button" hidden>%%admin.ltjoin%%</button></div>
      <p class="msg" id="t-msg" role="status"></p>
    </div>
    <div class="panel scroll">
      <table class="lt-table">
        <thead><tr><th>%%admin.tmember%%</th><th>%%admin.section%%</th><th>%%admin.ltrights%%</th><th>%%admin.ltteams%%</th><th>%%admin.tareas%%</th><th></th></tr></thead>
        <tbody id="langteam-rows"></tbody>
      </table>
      <p class="note" id="nolangteam" hidden style="margin:10px">%%admin.nolt%%</p>
    </div>

    <h2>%%admin.guilds%%</h2>
    <div class="panel">
      <p class="note" style="margin:0 0 14px">%%admin.guildsnote%%</p>
      <div class="form">
        <div><label for="g-name">%%admin.gname%%</label><input type="text" id="g-name" maxlength="30" autocomplete="off" spellcheck="false"></div>
        <div><label for="g-tag">%%admin.gtag%%</label><input type="text" id="g-tag" maxlength="4" autocomplete="off" spellcheck="false"></div>
        <div><button class="btn primary" id="g-send">%%admin.grant%%</button></div>
      </div>
      <p class="msg" id="g-msg" role="status"></p>
      <input type="file" id="g-file" accept="image/png,image/jpeg,image/webp" hidden>
    </div>
    <div class="panel scroll">
      <table>
        <thead><tr><th>%%admin.gpics%%</th><th>%%admin.guild%%</th><th>%%admin.pwhen%%</th><th>%%admin.pby%%</th><th></th></tr></thead>
        <tbody id="guilds"></tbody>
      </table>
      <p class="note" id="noguilds" hidden style="margin:10px">%%admin.noguilds%%</p>
    </div>

    <div id="staff-block" hidden>
      <h2>%%admin.staff%%</h2>
      <div class="panel">
        <p class="note" style="margin:0 0 8px">%%admin.staffnote%%</p>
        <p class="note" style="margin:0 0 14px">%%admin.staffhow%%</p>
        <div class="form">
          <div class="finder"><label for="s-who">%%admin.who%%</label><input type="text" id="s-who" maxlength="40" autocomplete="off" spellcheck="false" placeholder="%%admin.findph%%" role="combobox" aria-autocomplete="list" aria-expanded="false" aria-controls="s-hits"><ul class="finder-hits" id="s-hits" role="listbox" hidden></ul></div>
          <div><label for="s-note">%%admin.note%%</label><input type="text" id="s-note" maxlength="60" autocomplete="off"></div>
          <div><button class="btn primary" id="s-send">%%admin.grant%%</button></div>
        </div>
        <p class="hint" id="s-pick" style="margin-top:8px"></p>
        <p class="msg" id="s-msg" role="status"></p>
      </div>
      <div class="panel scroll">
        <table>
          <thead><tr><th>%%admin.kind%%</th><th>%%admin.id%%</th><th>%%admin.player%%</th><th>%%admin.note%%</th><th>%%admin.pwhen%%</th><th>%%admin.pby%%</th><th></th></tr></thead>
          <tbody id="staff"></tbody>
        </table>
        <p class="note" id="nostaff" hidden style="margin:10px">%%admin.nostaff%%</p>
      </div>
    </div>

    <div id="bot-block" hidden>
      <h2>%%admin.bot%%</h2>
      <div class="panel">
        <p class="note" style="margin:0 0 12px">%%admin.botnote%%</p>
        <ul class="bot-state" id="bot-state"></ul>
        <div class="tr-actions"><button class="btn primary" id="bot-reg" type="button" disabled>%%admin.botreg%%</button><a class="btn" id="bot-invite" hidden rel="noopener">%%admin.botinvite%%</a></div>
        <p class="msg" id="bot-msg" role="status"></p>
      </div>
    </div>
  </div>
`;

const ADMIN_JS = scriptFile('admin', String.raw`
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
        who.append(ban.name || L.noName);
        var uuid = el('td');
        uuid.append(el('code', '', ban.uuid));
        var lift = el('button', 'btn', L.lift);
        lift.onclick = function () {
          if (!confirm(fmt(L.confirmLift, { who: ban.name || ban.uuid }))) return;
          api('/admin/unban', { method: 'POST', body: JSON.stringify({ uuid: ban.uuid }), headers: { 'content-type': 'application/json' } })
            .then(function (r) { if (r.ok) loadBans(); else alert(fmt(L.liftFail, { s: r.status })); });
        };
        var action = el('td');
        action.append(lift);
        row.append(who, uuid, el('td', '', ban.t ? new Date(ban.t).toLocaleString() : '—'), el('td', '', ban.by || '—'), action);
        body.append(row);
      });
    });
  }
  function loadReviews() {
    return api('/admin/reviews').then(function (r) { return r.json(); }).then(function (data) {
      var body = document.getElementById('reviews');
      body.replaceChildren();
      document.getElementById('noreviews').hidden = data.reviews.length > 0;
      data.reviews.forEach(function (one) {
        var row = el('tr');
        var who = el('td');
        if (one.name) {
          var img = el('img');
          img.alt = '';
          img.src = head(one.name, 44);
          who.append(img, one.name);
        } else {
          who.append('—');
        }
        var uuid = el('td');
        uuid.append(el('code', '', one.uuid));
        var drop = el('button', 'btn', L.revoke);
        drop.onclick = function () {
          if (!confirm(fmt(L.confirmUnreview, { who: one.name || one.uuid }))) return;
          api('/admin/unreview', { method: 'POST', body: JSON.stringify({ uuid: one.uuid }), headers: { 'content-type': 'application/json' } })
            .then(function (r) { if (r.ok) loadReviews(); else alert(fmt(L.liftFail, { s: r.status })); });
        };
        var action = el('td');
        action.append(drop);
        row.append(who, uuid, el('td', '', one.note || '—'), el('td', '', one.t ? new Date(one.t).toLocaleString() : '—'), el('td', '', one.by || '—'), action);
        body.append(row);
      });
    });
  }
  document.getElementById('r-send').onclick = function () {
    var msg = document.getElementById('r-msg');
    var entry = { uuid: document.getElementById('r-uuid').value.trim(), note: document.getElementById('r-note').value.trim() };
    msg.className = 'msg ok';
    msg.textContent = L.sending;
    api('/admin/review', { method: 'POST', body: JSON.stringify(entry), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          if (r.ok) {
            msg.className = 'msg ok';
            msg.textContent = L.perkDone;
            document.getElementById('r-uuid').value = '';
            document.getElementById('r-note').value = '';
            loadReviews();
          } else {
            msg.className = 'msg bad';
            msg.textContent = L.perk[body.why] || fmt(L.prFail, { s: body.why || r.status });
          }
        });
      }).catch(function () { msg.className = 'msg bad'; msg.textContent = L.offline; });
  };
  function loadPerks() {
    return api('/admin/perks').then(function (r) { return r.json(); }).then(function (data) {
      var body = document.getElementById('perks');
      body.replaceChildren();
      document.getElementById('noperks').hidden = data.perks.length > 0;
      data.perks.forEach(function (perk) {
        var row = el('tr');
        var uuid = el('td');
        uuid.append(el('code', '', perk.uuid));
        var drop = el('button', 'btn', L.revoke);
        drop.onclick = function () {
          if (!confirm(fmt(L.confirmRevoke, { who: perk.note || perk.uuid }))) return;
          api('/admin/unperk', { method: 'POST', body: JSON.stringify({ uuid: perk.uuid }), headers: { 'content-type': 'application/json' } })
            .then(function (r) { if (r.ok) loadPerks(); else alert(fmt(L.liftFail, { s: r.status })); });
        };
        var action = el('td');
        action.append(drop);
        row.append(uuid, el('td', '', perk.note || '—'), el('td', '', perk.t ? new Date(perk.t).toLocaleString() : '—'), el('td', '', perk.by || '—'), action);
        body.append(row);
      });
    });
  }
  document.getElementById('p-send').onclick = function () {
    var msg = document.getElementById('p-msg');
    var entry = { uuid: document.getElementById('p-uuid').value.trim(), note: document.getElementById('p-note').value.trim() };
    msg.className = 'msg ok';
    msg.textContent = L.sending;
    api('/admin/perk', { method: 'POST', body: JSON.stringify(entry), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          if (r.ok) {
            msg.className = 'msg ok';
            msg.textContent = L.perkDone;
            document.getElementById('p-uuid').value = '';
            document.getElementById('p-note').value = '';
            loadPerks();
          } else {
            msg.className = 'msg bad';
            msg.textContent = L.perk[body.why] || fmt(L.prFail, { s: body.why || r.status });
          }
        });
      }).catch(function () { msg.className = 'msg bad'; msg.textContent = L.offline; });
  };
  var gmsg = document.getElementById('g-msg');
  function gsay(text, good) { gmsg.className = 'msg ' + (good ? 'ok' : 'bad'); gmsg.textContent = text; flash(gmsg); }
  var picking = null;
  function shrink(file, max, done) {
    var probe = new Image();
    var address = URL.createObjectURL(file);
    probe.onload = function () {
      var w = probe.naturalWidth;
      var h = probe.naturalHeight;
      var k = Math.min(1, max / Math.max(w, h));
      if (k === 1 && file.size <= 300000) { URL.revokeObjectURL(address); done(file); return; }
      var canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(w * k));
      canvas.height = Math.max(1, Math.round(h * k));
      canvas.getContext('2d').drawImage(probe, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(address);
      canvas.toBlob(function (blob) { done(blob); }, 'image/png');
    };
    probe.onerror = function () { URL.revokeObjectURL(address); done(null); };
    probe.src = address;
  }
  function loadGuilds() {
    return api('/admin/guilds').then(function (r) { return r.json(); }).then(function (data) {
      var body = document.getElementById('guilds');
      body.replaceChildren();
      document.getElementById('noguilds').hidden = data.guilds.length > 0;
      data.guilds.forEach(function (guild) {
        var row = el('tr');
        var pics = el('td');
        [['icon', guild.i], ['banner', guild.b]].forEach(function (pair) {
          if (!pair[1]) return;
          var img = el('img');
          img.alt = '';
          img.style.width = 'auto';
          img.style.height = '36px';
          img.style.imageRendering = pair[0] === 'banner' ? 'pixelated' : 'auto';
          img.src = '/api/guild/' + guild.slug + '/' + pair[0] + '?v=' + pair[1];
          pics.append(img);
        });
        if (!guild.i && !guild.b) pics.append('—');
        var name = el('td');
        var link = el('a', '', (guild.tag ? '[' + guild.tag + '] ' : '') + guild.name);
        link.href = 'https://wynncraft.com/stats/guild/' + encodeURIComponent(guild.name);
        link.target = '_blank';
        link.rel = 'noopener';
        name.append(link);
        var action = el('td');
        action.style.whiteSpace = 'nowrap';
        [['icon', L.upIcon], ['banner', L.upBanner]].forEach(function (pair) {
          var button = el('button', 'btn small', pair[1]);
          button.style.marginRight = '6px';
          button.onclick = function () {
            picking = { slug: guild.slug, kind: pair[0] };
            var input = document.getElementById('g-file');
            input.value = '';
            input.click();
          };
          action.append(button);
        });
        var drop = el('button', 'btn small', L.revoke);
        drop.onclick = function () {
          if (!confirm(fmt(L.confirmUnguild, { who: guild.name }))) return;
          api('/admin/unguild', { method: 'POST', body: JSON.stringify({ slug: guild.slug }), headers: { 'content-type': 'application/json' } })
            .then(function (r) { if (r.ok) loadGuilds(); else alert(fmt(L.liftFail, { s: r.status })); });
        };
        action.append(drop);
        row.append(pics, name, el('td', '', guild.t ? new Date(guild.t).toLocaleString() : '—'), el('td', '', guild.by || '—'), action);
        body.append(row);
      });
    });
  }
  document.getElementById('g-file').onchange = function () {
    var file = this.files[0];
    var target = picking;
    if (!file || !target) return;
    gsay(L.sending, true);
    shrink(file, target.kind === 'icon' ? 256 : 512, function (blob) {
      if (!blob) { gsay(L.guild['bad image'], false); return; }
      api('/admin/guild/' + target.slug + '/' + target.kind, { method: 'PUT', body: blob, headers: { 'content-type': blob.type || 'image/png' } })
        .then(function (r) {
          return r.json().catch(function () { return {}; }).then(function (answer) {
            if (r.ok) { gsay(L.picDone, true); loadGuilds(); }
            else gsay(L.guild[answer.why] || fmt(L.prFail, { s: answer.why || r.status }), false);
          });
        }).catch(function () { gsay(L.offline, false); });
    });
  };
  document.getElementById('g-send').onclick = function () {
    var entry = { name: document.getElementById('g-name').value.trim(), tag: document.getElementById('g-tag').value.trim() };
    gsay(L.sending, true);
    api('/admin/guild', { method: 'POST', body: JSON.stringify(entry), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (answer) {
          if (r.ok) {
            gsay(L.perkDone, true);
            document.getElementById('g-name').value = '';
            document.getElementById('g-tag').value = '';
            loadGuilds();
          } else gsay(L.guild[answer.why] || fmt(L.prFail, { s: answer.why || r.status }), false);
        });
      }).catch(function () { gsay(L.offline, false); });
  };

  var KIND = { u: 'Minecraft UUID', n: 'Minecraft ID', d: 'Discord' };
  var staffKeys = {};
  function loadStaff() {
    return api('/admin/staff').then(function (r) { return r.json(); }).then(function (data) {
      var body = document.getElementById('staff');
      body.replaceChildren();
      document.getElementById('nostaff').hidden = data.staff.length > 0;
      staffKeys = {};
      data.staff.forEach(function (one) { staffKeys[one.key] = true; });
      data.staff.forEach(function (one) {
        var row = el('tr');
        var id = el('td');
        id.append(el('code', '', one.id));
        var who = el('td');
        var known = one.name || (one.kind === 'n' ? one.id : '');
        if (known) {
          var img = el('img');
          img.alt = '';
          img.src = head(known, 44);
          who.append(img, known);
        } else if (!one.discord) {
          who.append(one.kind === 'u' ? L.notYet : '—');
        }
        if (one.discord) who.append(el('small', 'hint', (known ? ' · ' : '') + 'Discord ' + one.discord));
        var drop = el('button', 'btn', L.revoke);
        drop.onclick = function () {
          if (!confirm(fmt(L.confirmUnstaff, { who: one.name || one.note || one.id }))) return;
          api('/admin/unstaff', { method: 'POST', body: JSON.stringify({ key: one.key }), headers: { 'content-type': 'application/json' } })
            .then(function (r) { if (r.ok) loadStaff(); else alert(fmt(L.liftFail, { s: r.status })); });
        };
        var action = el('td');
        action.append(drop);
        row.append(el('td', '', KIND[one.kind] || one.kind), id, who, el('td', '', one.note || '—'),
          el('td', '', one.t ? new Date(one.t).toLocaleString() : '—'), el('td', '', one.by || '—'), action);
        body.append(row);
      });
    });
  }
  var everyone = null;
  function dashedId(u) { return [u.slice(0, 8), u.slice(8, 12), u.slice(12, 16), u.slice(16, 20), u.slice(20)].join('-'); }
  function personName(p) { return p.mc || p.dname || p.discord; }
  function personLines(p) {
    var lines = [];
    if (p.discord) lines.push('Discord ' + (p.dname ? p.dname + ' (' + p.discord + ')' : p.discord));
    if (p.uuid) lines.push((p.mc ? 'Minecraft ' + p.mc + ' · ' : '') + dashedId(p.uuid));
    return lines;
  }
  function personScore(p, q) {
    var best = 0, bare = q.replace(/-/g, '');
    [[p.dname, q], [p.mc, q], [p.discord, q], [p.uuid, bare]].forEach(function (pair) {
      var value = String(pair[0] || '').toLowerCase(), text = pair[1];
      if (!value || !text) return;
      var score = value === text ? 3 : value.indexOf(text) === 0 ? 2 : value.indexOf(text) >= 0 ? 1 : 0;
      if (score > best) best = score;
    });
    return best;
  }
  function loadPeople() {
    if (!everyone) {
      everyone = api('/admin/people').then(function (r) { return r.ok ? r.json() : { people: [] }; })
        .then(function (data) { return data.people || []; })
        .catch(function () { everyone = null; return []; });
    }
    return everyone;
  }
  function memberPicker(prefix, taken, takenLabel, onPick) {
  var finder = document.getElementById(prefix + '-who');
  var hits = document.getElementById(prefix + '-hits');
  var pickNote = document.getElementById(prefix + '-pick');
  var picked = null, found = [], active = -1;
  function personTaken(p) { return taken(p); }
  function openHits() {
    if (!hits.children.length) { hits.replaceChildren(el('li', 'none', L.loading)); hits.hidden = false; }
    loadPeople().then(function (all) {
      if (document.activeElement !== finder) return;
      var q = picked ? '' : finder.value.trim().toLowerCase();
      var list = !q ? all : all.map(function (p) { return [personScore(p, q), p]; })
        .filter(function (pair) { return pair[0] > 0; })
        .sort(function (a, b) { return b[0] - a[0]; })
        .map(function (pair) { return pair[1]; });
      showHits(list, all.length);
    });
  }
  function closeHits() {
    hits.hidden = true;
    finder.parentNode.classList.remove('open');
    finder.setAttribute('aria-expanded', 'false');
    finder.removeAttribute('aria-activedescendant');
    active = -1;
  }
  function mark(i) {
    active = i;
    found.forEach(function (p, n) {
      var li = document.getElementById(prefix + '-hit-' + n);
      if (li) li.setAttribute('aria-selected', n === i ? 'true' : 'false');
    });
    var li = document.getElementById(prefix + '-hit-' + i);
    if (li) { finder.setAttribute('aria-activedescendant', li.id); li.scrollIntoView({ block: 'nearest' }); }
  }
  function choose(p) {
    if (!p || personTaken(p)) return;
    picked = p;
    finder.value = personName(p);
    pickNote.textContent = fmt(L.chosen, { who: personLines(p).join(' · ') });
    closeHits();
    if (onPick) onPick(p);
  }
  function showHits(list, total) {
    found = list;
    hits.replaceChildren();
    if (!list.length) hits.append(el('li', 'none', L.findNone));
    else hits.append(el('li', 'count', fmt(L.findCount, { n: list.length, all: total })));
    list.forEach(function (p, i) {
      var li = el('li', personTaken(p) ? 'taken' : '');
      li.id = prefix + '-hit-' + i;
      li.setAttribute('role', 'option');
      li.setAttribute('aria-selected', 'false');
      if (personTaken(p)) li.setAttribute('aria-disabled', 'true');
      var img = el('img');
      img.alt = '';
      img.src = head(p.mc || p.uuid || 'MHF_Steve', 56);
      var text = el('div');
      text.append(el('b', '', personName(p)));
      personLines(p).forEach(function (line) { text.append(el('small', '', line)); });
      if (personTaken(p)) text.append(el('small', '', takenLabel));
      li.append(img, text);
      /* mousedown：搶在輸入框失焦、清單收起來之前 */
      li.addEventListener('mousedown', function (e) { e.preventDefault(); choose(p); });
      hits.append(li);
    });
    hits.hidden = false;
    finder.parentNode.classList.add('open');
    finder.setAttribute('aria-expanded', 'true');
    active = -1;
  }
  finder.addEventListener('focus', openHits);
  finder.addEventListener('click', function () { if (hits.hidden) openHits(); });
  finder.addEventListener('input', function () {
    picked = null;
    pickNote.textContent = '';
    if (onPick) onPick(null);
    openHits();
  });
  finder.addEventListener('keydown', function (e) {
    if (hits.hidden && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) { e.preventDefault(); openHits(); return; }
    if (hits.hidden || !found.length) { if (e.key === 'Escape') closeHits(); return; }
    if (e.key === 'ArrowDown') { e.preventDefault(); mark(active < 0 ? 0 : (active + 1) % found.length); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); mark(active <= 0 ? found.length - 1 : active - 1); }
    else if (e.key === 'Enter' && active >= 0) { e.preventDefault(); choose(found[active]); }
    else if (e.key === 'Escape') closeHits();
  });
  finder.addEventListener('blur', closeHits);
  return {
    who: function () { return picked ? (picked.uuid || picked.discord) : finder.value.trim(); },
    clear: function () { finder.value = ''; picked = null; pickNote.textContent = ''; },
    set: function (text) { finder.value = text; picked = null; pickNote.textContent = ''; }
  };
  }
  var staffPick = memberPicker('s', function (p) {
    return Boolean((p.uuid && staffKeys['u:' + p.uuid]) || (p.discord && staffKeys['d:' + p.discord]));
  }, L.isStaff);
  document.getElementById('s-send').onclick = function () {
    var msg = document.getElementById('s-msg');
    var entry = { who: staffPick.who(), note: document.getElementById('s-note').value.trim() };
    msg.className = 'msg ok';
    msg.textContent = L.sending;
    api('/admin/staff', { method: 'POST', body: JSON.stringify(entry), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          if (r.ok) {
            msg.className = 'msg ok';
            msg.textContent = L.perkDone;
            staffPick.clear();
            document.getElementById('s-note').value = '';
            loadStaff();
          } else {
            msg.className = 'msg bad';
            msg.textContent = L.staff[body.why] || fmt(L.prFail, { s: body.why || r.status });
          }
        });
      }).catch(function () { msg.className = 'msg bad'; msg.textContent = L.offline; });
  };
  var FB = L.fb;
  function fbPost(path, body) {
    return api(path, { method: 'POST', body: JSON.stringify(body), headers: { 'content-type': 'application/json' } })
      .then(function (r) { return r.json().catch(function () { return {}; }).then(function (b) { b.okay = r.ok; b.status = r.status; return b; }); });
  }
  function fbChip(text, cls) { return el('span', 'tr-chip' + (cls ? ' ' + cls : ''), text); }
  function loadFeedback() {
    var q = '?cat=' + encodeURIComponent(document.getElementById('fbf-cat').value)
      + '&st=' + encodeURIComponent(document.getElementById('fbf-st').value);
    return api('/admin/feedback' + q).then(function (r) { return r.ok ? r.json() : null; }).then(function (data) {
      var list = document.getElementById('fbf-list');
      list.replaceChildren();
      if (!data) return;
      document.getElementById('fbf-counts').textContent = fmt(FB.counts, { open: data.counts.open, done: data.counts.done });
      document.getElementById('nofeedback').hidden = data.items.length > 0;
      data.items.forEach(function (item) { list.append(feedbackCard(item)); });
    });
  }
  function feedbackCard(item) {
    var card = el('article', 'tr-row');
    var head = el('div', 'tr-head');
    head.append(fbChip(FB.cats[item.cat] || item.cat, item.cat === 'translation' ? 'leaf' : ''),
      fbChip(FB.st[item.st] || item.st, item.st === 'done' ? '' : 'gold'),
      el('span', 'note', item.name + ' · ' + new Date(item.t).toLocaleString() + ' · ' + (FB.via[item.via] || item.via)));
    card.append(head, el('p', 'fb-body', item.msg));
    var meta = el('p', 'fb-meta');
    if (item.where) meta.append(el('span', '', fmt(FB.where, { s: item.where })));
    if (item.lang) meta.append(el('span', '', fmt(FB.lang, { s: L.tr.langs[item.lang] || item.lang })));
    meta.append(el('span', '', fmt(FB.uid, { s: item.uid })));
    if (item.issue && String(item.issue.url).indexOf('https://github.com/') === 0) {
      var link = el('a', '', fmt(FB.adminIssue, { n: item.issue.n || '' }));
      link.href = item.issue.url;
      link.rel = 'noopener';
      meta.append(link);
    } else if (item.issueError) {
      meta.append(el('span', 'bad', fmt(FB.issueErr, { s: item.issueError })));
    }
    if (item.done) meta.append(el('span', '', fmt(FB.handled, { who: item.done.by, t: new Date(item.done.t).toLocaleString() })));
    card.append(meta);
    var bar = el('div', 'tr-actions');
    var flip = el('button', 'btn small' + (item.st === 'done' ? '' : ' primary'), item.st === 'done' ? FB.reopen : FB.markDone);
    flip.type = 'button';
    flip.onclick = function () {
      flip.disabled = true;
      fbPost('/admin/feedback/status', { id: item.id, st: item.st === 'done' ? 'open' : 'done' }).then(function (b) {
        if (b.okay) loadFeedback(); else { flip.disabled = false; alert(fmt(L.liftFail, { s: b.why || b.status })); }
      });
    };
    bar.append(flip);
    if (item.cat === 'translation' && !item.issue) {
      var again = el('button', 'btn small', FB.retry);
      again.type = 'button';
      again.onclick = function () {
        again.disabled = true;
        fbPost('/admin/feedback/issue', { id: item.id }).then(function (b) {
          if (b.item) card.replaceWith(feedbackCard(b.item));
          if (!b.okay) alert(fmt(FB.issueErr, { s: b.why || b.status }));
        });
      };
      bar.append(again);
    }
    var drop = el('button', 'btn small', FB.del);
    drop.type = 'button';
    drop.onclick = function () {
      if (!confirm(FB.confirmDel)) return;
      fbPost('/admin/feedback/delete', { id: item.id }).then(function (b) {
        if (b.okay) loadFeedback(); else alert(fmt(L.liftFail, { s: b.why || b.status }));
      });
    };
    bar.append(drop);
    card.append(bar);
    return card;
  }
  document.getElementById('fbf-cat').onchange = loadFeedback;
  document.getElementById('fbf-st').onchange = loadFeedback;
  whoami.then(function (me) {
    if (!me || !me.staff) return;
    document.getElementById('gate').hidden = true;
    document.getElementById('panel').hidden = false;
    loadFeedback();
    loadBans();
    loadReviews();
    loadPerks();
    loadGuilds();
    loadLangTeam();
    if (me.developer) {
      document.getElementById('staff-block').hidden = false;
      loadStaff();
      document.getElementById('bot-block').hidden = false;
      loadBot();
    }
  });
  function loadBot() {
    api('/admin/discord').then(function (r) { return r.ok ? r.json() : null; }).then(function (info) {
      if (!info) return;
      var list = document.getElementById('bot-state');
      list.replaceChildren();
      [['Public Key', info.publicKey], ['Bot Token', info.token], ['Client ID', info.clientId]].forEach(function (pair) {
        list.append(el('li', pair[1] ? 'ok' : 'bad', (pair[1] ? '\u2713 ' : '\u2717 ') + pair[0]));
      });
      var end = el('li', '', 'Interactions Endpoint URL: ');
      end.append(el('code', '', info.endpoint));
      list.append(end);
      var c = info.check;
      if (c && c.error) list.append(el('li', 'bad', '\u2717 Discord: ' + c.error));
      if (c && !c.error) {
        var line = function (ok, text) { list.append(el('li', ok === null ? '' : ok ? 'ok' : 'bad', (ok === null ? '' : ok ? '\u2713 ' : '\u2717 ') + text)); };
        line(null, 'Application: ' + c.app + ' (' + c.appId + ')' + (c.bot ? ' / Bot: ' + c.bot : ''));
        line(c.sameClient, 'Client ID = Application ID');
        line(c.sameKey, 'Public Key = Application');
        line(c.endpointSet === info.endpoint, 'Portal endpoint: ' + (c.endpointSet || '-'));
        line(Boolean(c.commands && c.commands.length), 'Commands: ' + (c.commands && c.commands.length ? c.commands.join(', ') : '-'));
        line(Boolean(c.guilds && c.guilds.length), 'Servers: ' + (c.guilds && c.guilds.length ? c.guilds.join(', ') : '-'));
      }
      var invite = document.getElementById('bot-invite');
      invite.hidden = !info.invite;
      if (info.invite) invite.href = info.invite;
      document.getElementById('bot-reg').disabled = !(info.token && info.clientId);
    });
  }
  document.getElementById('bot-reg').onclick = function () {
    var msg = document.getElementById('bot-msg');
    msg.className = 'msg ok';
    msg.textContent = L.sending;
    api('/admin/discord/register', { method: 'POST', body: '{}', headers: { 'content-type': 'application/json' } }).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (b) {
        msg.className = 'msg ' + (r.ok ? 'ok' : 'bad');
        msg.textContent = r.ok ? fmt(L.botOk, { s: (b.commands || []).join(', ') }) : fmt(L.prFail, { s: b.why || r.status });
      });
    });
  };
  var LT = L.lt;
  var TEAM_CODES = ['zh_tw', 'zh_cn', 'ja_jp', 'ko_kr', 'ru_ru', 'es_es', 'tr_tr'];
  var AREA_CODES = ['translation', 'proofreading', 'capture', 'testing', 'code', 'design', 'website', 'community'];
  var ltPeople = [];
  var ltSent = {};
  var ltCur = null;
  var ltTouched = false;
  var ltAreasStart = '';
  var joinTeams = null;
  var joinFor = '';
  function ltBoxes(host, prefix, codes, names, onChange) {
    codes.forEach(function (code) {
      var label = el('label');
      var box = el('input');
      box.type = 'checkbox';
      box.value = code;
      box.id = prefix + code;
      box.onchange = onChange;
      label.append(box, ' ' + (names[code] || code));
      host.append(label);
    });
  }
  ltBoxes(document.getElementById('t-langs'), 't-lang-', TEAM_CODES, L.tr.langs, function () { ltTouched = true; ltDirty(); });
  ltBoxes(document.getElementById('lt-areas'), 'lt-area-', AREA_CODES, L.teamAreas, function () { ltDirty(); });
  function ltTicked(prefix, codes) {
    return codes.filter(function (code) { return document.getElementById(prefix + code).checked; });
  }
  function ltSet(prefix, codes, list) {
    codes.forEach(function (code) { document.getElementById(prefix + code).checked = list.indexOf(code) >= 0; });
  }
  function ltWords(list, names) {
    return (list || []).map(function (code) { return (names || L.tr.langs)[code] || code; }).join('、') || '—';
  }
  function ltWho(person) {
    if (person.mc) return person.mc;
    var key = person.rights && person.rights.keys[0];
    return key ? key.slice(2) : '';
  }
  function ltFind(text) {
    var q = String(text || '').trim().toLowerCase();
    if (!q) return null;
    return ltPeople.find(function (p) { return p.mc && p.mc.toLowerCase() === q; })
      || ltPeople.find(function (p) { return p.rights && p.rights.keys.some(function (k) { return k.slice(2) === q; }); }) || null;
  }
  var ltPick = memberPicker('t', function () { return false; }, '', function (p) {
    ltShow(p ? (ltFind(p.mc) || ltFind(p.uuid) || ltFind(p.discord)) : ltFind(document.getElementById('t-who').value));
  });
  function ltDirty() {
    var areasMoved = Boolean(ltCur && ltCur.listed) && ltTicked('lt-area-', AREA_CODES).join() !== ltAreasStart;
    document.getElementById('lt-save').disabled = !ltPick.who() || !(ltTouched || areasMoved);
  }
  function ltButton(text, langs) {
    var button = el('button', 'btn small', text);
    button.type = 'button';
    button.onclick = function () {
      ltSet('t-lang-', TEAM_CODES, langs);
      ltTouched = true;
      ltDirty();
    };
    return button;
  }
  function ltShow(person) {
    ltCur = person;
    ltTouched = false;
    var rights = person && person.rights ? person.rights.langs : [];
    var start = person ? (person.rights ? rights : person.teams) : [];
    ltSet('t-lang-', TEAM_CODES, start);
    ltSet('lt-area-', AREA_CODES, person ? person.areas : []);
    ltAreasStart = person ? person.areas.join() : '';
    document.getElementById('lt-areas').disabled = !(person && person.listed);
    document.getElementById('t-note').value = person && person.rights ? person.rights.note || '' : '';
    var state = document.getElementById('lt-state');
    state.replaceChildren();
    state.className = 'lt-state';
    var join = document.getElementById('lt-join');
    join.hidden = !(person && !person.listed && person.mc);
    var typed = document.getElementById('t-who').value.trim();
    state.hidden = !person && !typed;
    if (!person) {
      if (typed) state.append(el('p', '', LT.fresh));
      ltDirty();
      return;
    }
    var lines = el('p', '');
    if (person.rights) lines.append(fmt(LT.rightsNow, { s: ltWords(rights) }));
    if (person.listed) {
      if (person.rights) lines.append(el('br'));
      lines.append(fmt(LT.teamsNow, { s: ltWords(person.teams) }));
    }
    var says = el('p', '');
    var bar = el('div', 'tr-actions');
    if (!person.listed) {
      says.textContent = person.mc ? LT.notListed : LT.discordOnly;
    } else if (!person.rights) {
      says.textContent = LT.noRights;
      if (person.teams.length) bar.append(ltButton(LT.grantTeams, person.teams));
    } else if (person.mismatch) {
      state.className = 'lt-state warn';
      says.textContent = LT.mismatch;
      bar.append(ltButton(LT.useRights, rights), ltButton(LT.useTeams, person.teams));
    } else {
      says.textContent = LT.inSync;
    }
    state.append(lines, says);
    if (bar.children.length) state.append(bar);
    ltDirty();
  }
  function loadLangTeam() {
    return api('/admin/langteam').then(function (r) { return r.ok ? r.json() : null; }).then(function (data) {
      if (!data) return;
      ltPeople = data.people.map(function (person) {
        var sent = person.mc && ltSent[person.mc.toLowerCase()];
        if (sent) {
          if (sent.teams) person.teams = sent.teams;
          if (sent.areas) person.areas = sent.areas;
          person.pending = true;
          person.mismatch = Boolean(person.rights) && person.rights.langs.join() !== person.teams.join();
        }
        return person;
      });
      ltPeople.sort(function (a, b) { return (b.mismatch ? 1 : 0) - (a.mismatch ? 1 : 0); });
      var body = document.getElementById('langteam-rows');
      body.replaceChildren();
      document.getElementById('nolangteam').hidden = ltPeople.length > 0;
      ltPeople.forEach(function (person) {
        var row = el('tr');
        var who = el('td');
        if (person.mc) {
          var img = el('img');
          img.alt = '';
          img.src = head(person.mc, 44);
          who.append(img);
        }
        who.append(person.name);
        if (person.rights && person.rights.discord && person.rights.discord !== person.name) who.append(el('small', 'hint', ' · Discord ' + person.rights.discord));
        if (person.mismatch) who.append(' ', el('span', 'tr-chip gold', LT.mismatchChip));
        var roles = el('td');
        if (person.listed) roles.textContent = person.roles.map(function (role) { return LT.roles[role] || role; }).join('、');
        else roles.append(el('span', 'tr-chip', LT.notListedChip));
        var teams = el('td', '', person.listed ? ltWords(person.teams) : '—');
        if (person.pending) teams.append(el('br'), el('small', 'hint', LT.pending));
        var edit = el('button', 'btn', LT.edit);
        edit.type = 'button';
        edit.onclick = function () {
          ltPick.set(ltWho(person));
          document.getElementById('t-msg').textContent = '';
          ltShow(person);
          document.getElementById('langteam').scrollIntoView();
        };
        var action = el('td');
        action.append(edit);
        row.append(who, roles, el('td', '', person.rights ? ltWords(person.rights.langs) : '—'), teams,
          el('td', '', person.listed ? ltWords(person.areas, L.teamAreas) : '—'), action);
        body.append(row);
      });
      if (ltCur) ltShow(ltFind(ltWho(ltCur)));
    });
  }
  document.getElementById('lt-save').onclick = function () {
    var msg = document.getElementById('t-msg');
    var who = ltPick.who();
    if (!who) { msg.className = 'msg bad'; msg.textContent = LT.pick; return; }
    var entry = { who: who, note: document.getElementById('t-note').value.trim() };
    var langs = ltTicked('t-lang-', TEAM_CODES);
    if (ltTouched) entry.langs = langs;
    var areas = ltTicked('lt-area-', AREA_CODES);
    if (ltCur && ltCur.listed && areas.join() !== ltAreasStart) entry.areas = areas;
    if (entry.langs && ltCur) {
      var had = TEAM_CODES.filter(function (code) {
        return (ltCur.rights ? ltCur.rights.langs : []).indexOf(code) >= 0 || (ltCur.listed && ltCur.teams.indexOf(code) >= 0);
      });
      var dropped = had.filter(function (code) { return langs.indexOf(code) < 0; });
      if (dropped.length && !confirm(fmt(LT.confirmDrop, { who: ltCur.name, langs: ltWords(dropped) }))) return;
    }
    var save = document.getElementById('lt-save');
    save.disabled = true;
    msg.className = 'msg ok';
    msg.textContent = L.sending;
    api('/admin/langteam', { method: 'POST', body: JSON.stringify(entry), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          msg.replaceChildren();
          if (!r.ok) {
            msg.className = 'msg bad';
            msg.textContent = LT.why[body.why] || L.credit[body.why] || fmt(L.prFail, { s: body.why || r.status });
            ltDirty();
            return;
          }
          msg.className = 'msg ok';
          msg.append(LT.saved);
          var team = body.team;
          if (team && team.url) {
            ltSent[String(body.mc).toLowerCase()] = { teams: entry.langs, areas: entry.areas };
            msg.append(' · ' + LT.teamPr + ': ');
            if (String(team.url).indexOf('https://github.com/') === 0) {
              var link = el('a', '', team.url);
              link.href = team.url;
              link.rel = 'noopener';
              msg.append(link);
            }
          } else if (team && team.unchanged) {
            msg.append(' · ' + LT.teamSame);
          } else if (team && team.why) {
            msg.className = 'msg bad';
            msg.append(' · ' + fmt(LT.teamFail, { s: L.credit[team.why] || team.why }));
          }
          loadLangTeam();
        });
      }).catch(function () { msg.className = 'msg bad'; msg.textContent = L.offline; ltDirty(); });
  };
  document.getElementById('lt-join').onclick = function () {
    if (!ltCur || !ltCur.mc) return;
    document.getElementById('c-section').value = 'contributors';
    document.getElementById('c-name').value = ltCur.name;
    document.getElementById('c-mc').value = ltCur.mc;
    joinTeams = ltCur.rights ? ltCur.rights.langs : [];
    joinFor = ltCur.mc.toLowerCase();
    var hint = document.getElementById('c-teams');
    hint.textContent = fmt(LT.joinHint, { s: ltWords(joinTeams) });
    hint.hidden = !joinTeams.length;
    document.getElementById('c-name').scrollIntoView({ block: 'center' });
    document.getElementById('c-name').focus();
  };
  document.getElementById('c-mc').addEventListener('input', function () {
    if (this.value.trim().toLowerCase() === joinFor) return;
    joinTeams = null;
    joinFor = '';
    document.getElementById('c-teams').hidden = true;
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
    if (joinTeams && joinTeams.length && entry.mc.toLowerCase() === joinFor) entry.teams = joinTeams;
    msg.className = 'msg ok';
    msg.textContent = L.sending;
    api('/admin/credits', { method: 'POST', body: JSON.stringify(entry), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          msg.replaceChildren();
          if (r.ok) {
            msg.className = 'msg ok';
            msg.append(L.prDone);
            joinTeams = null;
            joinFor = '';
            document.getElementById('c-teams').hidden = true;
            if (String(body.url).indexOf('https://github.com/') === 0) {
              var link = el('a', '', body.url);
              link.href = body.url;
              msg.append(': ', link);
            }
          } else {
            msg.className = 'msg bad';
            msg.textContent = L.credit[body.why] || fmt(L.prFail, { s: body.why || r.status });
          }
        });
      }).catch(function () { msg.className = 'msg bad'; msg.textContent = L.offline; });
  };
`);

const legalPage = (name) => '<article class="policy"><h1>%%' + name + '.h1%%</h1>'
  + '<p class="policy-date">%%legal.updated%%</p>%%' + name + '.body%%</article>';

const LOST = String.raw`
  <section class="lost">
    <span class="code" aria-hidden="true">404</span>
    <h1>%%lost.h1%%</h1>
    <p class="lede">%%lost.p%%</p>
    <div class="cta">
      <a class="btn primary" href="/">%%lost.home%%</a>
      <a class="btn" href="/features">%%nav.features%%</a>
      <a class="btn" href="/capes">%%nav.capes%%</a>
    </div>
  </section>
`;

const TRANSLATE = String.raw`
  <h1>%%tr.h1%%</h1>
  <p class="lede">%%tr.lede%%</p>
  <div class="tr-bar">
    <label for="tr-lang">%%tr.lang%%</label>
    <select class="pick" id="tr-lang"></select>
    <span class="tr-role" id="tr-role"></span>
  </div>
  <div class="tr-tabs" role="tablist" aria-label="%%tr.h1%%">
    <button type="button" role="tab" id="tt-browse" aria-selected="true" aria-controls="tp-browse">%%tr.tab.browse%%</button>
    <button type="button" role="tab" id="tt-props" aria-selected="false" aria-controls="tp-props">%%tr.tab.props%%<span class="tr-count" id="tc-props"></span></button>
    <button type="button" role="tab" id="tt-terms" aria-selected="false" aria-controls="tp-terms">%%tr.tab.terms%%<span class="tr-count" id="tc-terms"></span></button>
  </div>

  <section id="tp-browse" role="tabpanel" aria-labelledby="tt-browse">
    <div class="tr-search">
      <input type="search" id="tr-q" placeholder="%%tr.q%%" autocomplete="off" spellcheck="false">
      <select class="pick" id="tr-file"><option value="">%%tr.allfiles%%</option></select>
      <label class="tr-check"><input type="checkbox" id="tr-blank"> %%tr.blank%%</label>
    </div>
    <p class="note" id="tr-status" role="status"></p>
    <div id="tr-rows" class="tr-list"></div>
  </section>

  <section id="tp-props" role="tabpanel" aria-labelledby="tt-props" hidden>
    <p class="note">%%tr.props.note%%</p>
    <div class="tr-search">
      <select class="pick" id="tr-pst">
        <option value="open">%%tr.st.open%%</option>
        <option value="accepted">%%tr.st.accepted%%</option>
        <option value="rejected">%%tr.st.rejected%%</option>
      </select>
    </div>
    <div id="tr-props" class="tr-list"></div>
  </section>

  <section id="tp-terms" role="tabpanel" aria-labelledby="tt-terms" hidden>
    <p class="note">%%tr.terms.note%%</p>
    <div class="panel tr-newpoll" id="tr-newpoll" hidden>
      <div class="form">
        <div><label for="np-term">%%tr.np.term%%</label><input type="text" id="np-term" maxlength="80" autocomplete="off" placeholder="Weaken Enemy"></div>
        <div><label for="np-opts">%%tr.np.opts%%</label><input type="text" id="np-opts" maxlength="400" autocomplete="off"></div>
      </div>
      <label for="np-note" style="margin-top:12px">%%tr.np.note%%</label>
      <textarea id="np-note" maxlength="500" rows="2" class="tr-text"></textarea>
      <div class="tr-actions"><button class="btn primary" id="np-send" type="button">%%tr.np.send%%</button><span class="msg" id="np-msg" role="status"></span></div>
    </div>
    <div id="tr-polls" class="tr-list"></div>
  </section>
`;

const TRANSLATE_JS = scriptFile('translate', String.raw`
  var T = L.tr;
  var LANG_KEY = 'wcy.trlang';
  var CODES = ['zh_tw', 'zh_cn', 'ja_jp', 'ko_kr', 'ru_ru', 'es_es', 'tr_tr'];
  var SITE_TO = { zh: 'zh_tw', 'zh-cn': 'zh_cn', ja: 'ja_jp', ko: 'ko_kr', ru: 'ru_ru', es: 'es_es', tr: 'tr_tr' };
  var langSel = document.getElementById('tr-lang');
  var state = { lang: '', me: { signedIn: false, langs: [], decide: false, id: '' }, index: null, props: [], polls: [], byId: {} };
  CODES.forEach(function (c) {
    var o = el('option', '', T.langs[c] || c);
    o.value = c;
    langSel.append(o);
  });
  function saved() { try { return localStorage.getItem(LANG_KEY) || ''; } catch (e) { return ''; } }
  state.lang = CODES.indexOf(saved()) >= 0 ? saved() : (SITE_TO[document.documentElement.lang.toLowerCase()] || SITE_TO[document.documentElement.lang.toLowerCase().split('-')[0]] || 'zh_tw');
  langSel.value = state.lang;
  function post(path, body) {
    return api(path, { method: 'POST', body: JSON.stringify(body), headers: { 'content-type': 'application/json' } })
      .then(function (r) { return r.json().catch(function () { return {}; }).then(function (b) { b.status = r.status; b.okay = r.ok; return b; }); });
  }
  function canWrite() { return state.me.decide || state.me.langs.indexOf(state.lang) >= 0; }
  function why(code) { return T.why[code] || fmt(T.failed, { s: code }); }

  function problem(src, text, base) {
    if (!text || !text.trim()) return 'empty';
    if (text.length > 2000) return 'too long';
    var seq = function (s) { return String(s).match(/\{[#pu]\}/g) || []; };
    var want = seq(src), got = seq(text);
    var noName = function (list) { return list.filter(function (t) { return t !== '{u}'; }).join(''); };
    if (noName(want) !== noName(got)) return 'placeholders';
    var at = 0;
    for (var i = 0; i < got.length; i++) {
      while (at < want.length && want[at] !== got[i]) at++;
      if (at === want.length) return 'placeholders';
      at++;
    }
    var nums = function (s) { return String(s).match(/\{~\d*\}/g) || []; };
    if (nums(src).length !== nums(text).length) return 'numbers';
    var named = nums(text).filter(function (n) { return n !== '{~}'; });
    if (named.length && named.length !== nums(text).length) return 'numbering';
    if (named.length) {
      var order = named.map(function (n) { return Number(n.slice(2, -1)); }).sort(function (a, b) { return a - b; });
      if (order.some(function (n, i) { return n !== i + 1; })) return 'numbering';
    }
    var colours = function (s) { return (String(s).match(/\{c\d+\}|\{\/\}/g) || []).sort().join(','); };
    if (base && base.trim() && colours(base) !== colours(text)) return 'colours';
    if (String(src).split('\n').length !== text.split('\n').length) return 'lines';
    return null;
  }
  function propId(file, key, src) {
    var data = new TextEncoder().encode(file + '\n' + (key == null ? src : key));
    return crypto.subtle.digest('SHA-256', data).then(function (buf) {
      return Array.prototype.map.call(new Uint8Array(buf), function (b) { return ('0' + b.toString(16)).slice(-2); }).join('').slice(0, 16);
    });
  }
  function textBlock(text, cls) {
    var b = el('div', cls);
    String(text).split('\n').forEach(function (line, i) { if (i) b.append(el('br')); b.append(line); });
    return b;
  }
  function chip(text, cls) { return el('span', 'tr-chip' + (cls ? ' ' + cls : ''), text); }

  var SAMPLES = [[12, 3, 250, 45, 8, 1500, 7, 30, 2, 64], [1, 1, 1, 1, 1, 1, 1, 1, 1, 1], [1234, 56, 7, 890, 2, 33, 4, 5, 6, 9]];
  var sampleSet = 0;
  var SLOT_COLOURS = ['#ffaa00', '#55ffff', '#55ff55', '#ff55ff', '#ffff55', '#ff5555'];
  function numberCount(text) { return (String(text).match(/\{~\d*\}/g) || []).length; }
  function gameText(text, limit) {
    var values = SAMPLES[sampleSet];
    var out = el('div', 'gv-text');
    var order = 0, colour = '';
    String(text).split('\n').forEach(function (line) {
      var row = el('div', 'gv-line');
      line.split(/(\{(?:~\d*|[#pu\/]|c\d+|w\d+)\})/).forEach(function (part, i) {
        if (!part) return;
        var span = null;
        if (i % 2 === 0) span = el('span', '', part);
        else if (part.charAt(1) === '~') {
          var n = part === '{~}' ? ++order : Number(part.slice(2, -1));
          span = el('span', 'gv-num' + (n > limit ? ' bad' : ''), String(values[(n - 1) % values.length]));
          var sup = el('sup', '', String(n));
          if (n > limit) sup.title = fmt(T.pvOver, { n: limit });
          span.append(sup);
        } else if (part === '{p}') span = el('span', 'gv-ph', 'Detlas');
        else if (part === '{u}') span = el('span', 'gv-ph', 'Aledar');
        else if (part === '{#}') { span = el('i', 'gv-icon'); span.title = '{#}'; }
        else if (part === '{/}') colour = '';
        else if (part.charAt(1) === 'c') colour = SLOT_COLOURS[(Number(part.slice(2, -1)) - 1) % SLOT_COLOURS.length];
        if (span) {
          if (colour) span.style.color = colour;
          row.append(span);
        }
      });
      if (!row.childNodes.length) row.append('\u00a0');
      out.append(row);
    });
    return out;
  }
  function preview(src, current) {
    var box = el('div', 'gv');
    var head = el('div', 'gv-head');
    head.append(el('span', 'gv-title', T.preview));
    var limit = numberCount(src);
    if (limit) {
      var again = el('button', 'btn small', T.reroll);
      again.type = 'button';
      again.onclick = function () { sampleSet = (sampleSet + 1) % SAMPLES.length; draw(); };
      head.append(again);
    }
    var cols = el('div', 'gv-cols');
    var left = el('figure', 'gv-col'), right = el('figure', 'gv-col');
    cols.append(left, right);
    function tip(text) { var t = el('div', 'gv-tip'); t.append(gameText(text, limit)); return t; }
    function draw() {
      left.replaceChildren(el('figcaption', '', T.pvSrc), tip(src));
      var text = current();
      right.replaceChildren(el('figcaption', '', T.pvDst), text && text.trim() ? tip(text) : el('p', 'note', T.untranslated));
    }
    box.append(head, cols);
    if (/\{(?:~\d*|[#pu])\}/.test(src)) box.append(el('p', 'note gv-note', T.pvNote));
    draw();
    box.redraw = draw;
    return box;
  }
  function insertAt(area, text) {
    var from = area.selectionStart, to = area.selectionEnd;
    area.value = area.value.slice(0, from) + text + area.value.slice(to);
    area.selectionStart = area.selectionEnd = from + text.length;
  }

  var tabs = ['browse', 'props', 'terms'];
  function show(name) {
    tabs.forEach(function (t) {
      document.getElementById('tt-' + t).setAttribute('aria-selected', t === name ? 'true' : 'false');
      document.getElementById('tp-' + t).hidden = t !== name;
    });
    if (name === 'props') drawProps();
    if (name === 'terms') drawPolls();
  }
  tabs.forEach(function (t) { document.getElementById('tt-' + t).onclick = function () { show(t); }; });

  function drawRole() {
    var role = document.getElementById('tr-role');
    role.replaceChildren();
    if (!state.me.signedIn) {
      var a = el('a', '', T.signin);
      a.href = '/account';
      role.append(a);
    } else if (state.me.decide) role.append(chip(T.roleDecide, 'gold'));
    else if (canWrite()) role.append(chip(T.roleTranslator, 'leaf'));
    else role.append(el('span', 'note', T.roleViewer));
    document.getElementById('tr-newpoll').hidden = !canWrite();
  }

  function loadIndex() {
    var status = document.getElementById('tr-status');
    status.textContent = T.loading;
    state.index = null;
    document.getElementById('tr-rows').replaceChildren();
    var lang = state.lang;
    return api('/tr/index/' + lang).then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(function (data) {
        if (lang !== state.lang) return;
        data.low = data.rows.map(function (row) { return (row[2] + '\n' + row[3]).toLowerCase(); });
        state.index = data;
        var files = document.getElementById('tr-file');
        files.replaceChildren(files.options[0]);
        data.files.forEach(function (f, i) { var o = el('option', '', f); o.value = String(i); files.append(o); });
        search();
      }).catch(function () { status.textContent = T.indexFail; });
  }
  var timer = 0;
  function search() {
    var data = state.index;
    if (!data) return;
    var q = document.getElementById('tr-q').value.trim().toLowerCase();
    var file = document.getElementById('tr-file').value;
    var blank = document.getElementById('tr-blank').checked;
    var status = document.getElementById('tr-status');
    var box = document.getElementById('tr-rows');
    if (q.length < 2 && file === '' && !blank) {
      status.textContent = fmt(T.hint, { n: data.rows.length.toLocaleString() });
      box.replaceChildren();
      return;
    }
    var hits = [];
    for (var i = 0; i < data.rows.length; i++) {
      var row = data.rows[i];
      if (file !== '' && String(row[0]) !== file) continue;
      if (blank && row[3]) continue;
      if (q.length >= 2 && data.low[i].indexOf(q) < 0) continue;
      hits.push(row);
    }
    status.textContent = fmt(T.found, { n: hits.length.toLocaleString(), shown: Math.min(hits.length, 100) });
    box.replaceChildren();
    hits.slice(0, 100).forEach(function (row) { box.append(rowCard(row)); });
  }
  ['tr-q', 'tr-file', 'tr-blank'].forEach(function (id) {
    document.getElementById(id).addEventListener(id === 'tr-q' ? 'input' : 'change', function () {
      clearTimeout(timer);
      timer = setTimeout(search, 180);
    });
  });

  function rowCard(row) {
    var file = state.index.files[row[0]], key = row[1], src = row[2], dst = row[3], who = row[4];
    var card = el('article', 'tr-row');
    var head = el('div', 'tr-head');
    head.append(chip(file));
    if (who) head.append(chip(who, 'soft'));
    var badge = chip('', 'leaf');
    badge.hidden = true;
    head.append(badge);
    card.append(head, textBlock(src, 'tr-src'));
    card.append(dst ? textBlock(dst, 'tr-dst') : el('div', 'tr-dst empty', T.untranslated));
    var actions = el('div', 'tr-actions');
    var open = el('button', 'btn small', canWrite() ? T.propose : T.view);
    open.type = 'button';
    actions.append(open);
    card.append(actions);
    var more = el('div', 'tr-more');
    more.hidden = true;
    card.append(more);
    propId(file, key, src).then(function (id) {
      var meta = state.byId[id];
      if (meta && meta.st === 'open') { badge.hidden = false; badge.textContent = fmt(T.openProps, { n: meta.n }); }
      open.onclick = function () {
        if (!more.hidden) { more.hidden = true; return; }
        more.hidden = false;
        more.replaceChildren(el('p', 'note', T.loading));
        api('/tr/prop?lang=' + state.lang + '&id=' + id).then(function (r) { return r.ok ? r.json() : null; }).then(function (prop) {
          more.replaceChildren();
          if (prop) more.append(propBody(prop, id));
          if (canWrite() && (!prop || prop.status === 'open')) more.append(editor({ file: file, key: key, src: src, base: dst }, more, id));
          else more.append(preview(src, function () { return dst; }));
          if (!prop && !canWrite()) more.append(el('p', 'note', state.me.signedIn ? T.notTranslator : T.signinToPropose));
        });
      };
    });
    return card;
  }

  function editor(entry, holder, id) {
    var box = el('div', 'tr-editor');
    var label = el('label', '', T.yours);
    var area = el('textarea', 'tr-text');
    area.setAttribute('aria-label', T.yours);
    area.rows = Math.min(8, entry.src.split('\n').length + 1);
    area.value = entry.base || '';
    var check = el('p', 'msg');
    var send = el('button', 'btn primary small', T.send);
    send.type = 'button';
    var pv = preview(entry.src, function () { return area.value; });
    var tools = el('div', 'tr-actions gv-tools');
    var count = numberCount(entry.src);
    var tokens = [];
    for (var i = 1; i <= count; i++) tokens.push('{~' + i + '}');
    ['{p}', '{u}', '{#}'].forEach(function (t) { if (entry.src.indexOf(t) >= 0) tokens.push(t); });
    if (tokens.length) tools.append(el('span', 'note', T.insert));
    tokens.forEach(function (t) {
      var b = el('button', 'btn small', t);
      b.type = 'button';
      b.onclick = function () { insertAt(area, t); live(); area.focus(); };
      tools.append(b);
    });
    var number = el('button', 'btn small', T.autonum);
    number.type = 'button';
    number.title = T.autonumHint;
    number.onclick = function () {
      var n = 0;
      area.value = area.value.replace(/\{~\}/g, function () { return '{~' + (++n) + '}'; });
      live();
      area.focus();
    };
    tools.append(number);
    function live() {
      var p = problem(entry.src, area.value, entry.base);
      check.className = 'msg ' + (p ? 'bad' : 'ok');
      check.textContent = p ? why(p) : T.looksOk;
      send.disabled = Boolean(p) || area.value === entry.base;
      number.hidden = count < 2 || !/\{~\}/.test(area.value);
      tools.hidden = !tokens.length && number.hidden;
      pv.redraw();
    }
    area.addEventListener('input', live);
    live();
    send.onclick = function () {
      send.disabled = true;
      post('/tr/propose', { lang: state.lang, file: entry.file, key: entry.key, src: entry.src, base: entry.base, text: area.value })
        .then(function (b) {
          if (!b.okay) { check.className = 'msg bad'; check.textContent = why(b.why); send.disabled = false; return; }
          state.byId[b.id] = { st: 'open', n: b.prop.items.length };
          holder.replaceChildren(propBody(b.prop, b.id));
          loadProps();
        });
    };
    box.append(label, area, tools, check, pv, el('div', 'tr-actions'));
    box.lastChild.append(send);
    return box;
  }

  function propBody(prop, id) {
    var box = el('div', 'tr-prop');
    var stat = { open: T.st.open, accepted: T.st.accepted, rejected: T.st.rejected }[prop.status] || prop.status;
    var head = el('div', 'tr-head');
    head.append(chip(stat, prop.status === 'accepted' ? 'leaf' : prop.status === 'rejected' ? 'bad' : 'gold'));
    if (prop.done && prop.done.pr) {
      var link = el('a', '', T.prLink);
      link.href = prop.done.pr;
      link.rel = 'noopener';
      head.append(link);
    }
    box.append(head);
    var items = prop.items.slice().sort(function (a, b) { return b.votes.length - a.votes.length; });
    items.forEach(function (it) {
      var row = el('div', 'tr-item' + (prop.done && prop.done.item === it.id ? ' won' : ''));
      row.append(textBlock(it.text, 'tr-dst'));
      var foot = el('div', 'tr-foot');
      foot.append(el('span', 'note', fmt(T.by, { who: whoOf(it, prop) })));
      var votes = el('span', 'tr-votes', fmt(T.votes, { n: it.votes.length }));
      foot.append(votes);
      if (prop.status === 'open' && canWrite()) {
        var mine = it.votes.indexOf(state.me.id) >= 0;
        var v = el('button', 'btn small' + (mine ? ' primary' : ''), mine ? T.voted : T.vote);
        v.type = 'button';
        v.onclick = function () {
          post('/tr/vote', { lang: state.lang, id: id, item: it.id }).then(function (b) {
            if (b.okay) box.replaceWith(propBody(b.prop, id)); else alert(why(b.why));
          });
        };
        foot.append(v);
      }
      if (prop.status === 'open' && state.me.decide) {
        var ok = el('button', 'btn small', T.accept);
        ok.type = 'button';
        ok.onclick = function () {
          ok.disabled = true;
          post('/tr/accept', { lang: state.lang, id: id, item: it.id }).then(function (b) {
            if (b.okay) { box.replaceWith(propBody(b.prop, id)); refreshCounts(); } else { ok.disabled = false; alert(why(b.why)); }
          });
        };
        foot.append(ok);
      }
      row.append(foot);
      box.append(row);
    });
    if (prop.status === 'open' && state.me.decide) {
      var no = el('button', 'btn small', T.reject);
      no.type = 'button';
      no.onclick = function () {
        post('/tr/reject', { lang: state.lang, id: id }).then(function (b) {
          if (b.okay) { box.replaceWith(propBody(b.prop, id)); refreshCounts(); } else alert(why(b.why));
        });
      };
      var bar = el('div', 'tr-actions');
      bar.append(no);
      box.append(bar);
    }
    return box;
  }

  function loadProps() {
    return api('/tr/props?lang=' + state.lang).then(function (r) { return r.ok ? r.json() : { props: [] }; }).then(function (data) {
      state.props = data.props || [];
      state.byId = {};
      state.props.forEach(function (p) { state.byId[p.id] = p; });
      refreshCounts();
    });
  }
  function refreshCounts() {
    var open = state.props.filter(function (p) { return p.st === 'open'; }).length;
    document.getElementById('tc-props').textContent = open ? ' ' + open : '';
    var polls = state.polls.filter(function (p) { return p.st === 'open'; }).length;
    document.getElementById('tc-terms').textContent = polls ? ' ' + polls : '';
  }
  document.getElementById('tr-pst').onchange = function () { drawProps(); };
  function drawProps() {
    var want = document.getElementById('tr-pst').value;
    var box = document.getElementById('tr-props');
    box.replaceChildren();
    var list = state.props.filter(function (p) { return p.st === want; });
    if (!list.length) { box.append(el('p', 'note', T.none)); return; }
    list.forEach(function (meta) {
      var card = el('article', 'tr-row');
      var head = el('div', 'tr-head');
      head.append(chip(meta.f), chip(fmt(T.votes, { n: meta.v }), 'soft'));
      card.append(head, textBlock(meta.s, 'tr-src'));
      var more = el('div', 'tr-more');
      var open = el('button', 'btn small', fmt(T.seeItems, { n: meta.n }));
      open.type = 'button';
      open.onclick = function () {
        if (more.childNodes.length) { more.replaceChildren(); return; }
        api('/tr/prop?lang=' + state.lang + '&id=' + meta.id).then(function (r) { return r.json(); }).then(function (prop) {
          more.replaceChildren(textBlock(prop.base || T.untranslated, 'tr-dst' + (prop.base ? '' : ' empty')), propBody(prop, meta.id));
        });
      };
      var bar = el('div', 'tr-actions');
      bar.append(open);
      card.append(bar, more);
      box.append(card);
    });
  }

  function loadPolls() {
    return api('/tr/polls?lang=' + state.lang).then(function (r) { return r.ok ? r.json() : { polls: [] }; }).then(function (data) {
      state.polls = data.polls || [];
      refreshCounts();
    });
  }
  function drawPolls() {
    var box = document.getElementById('tr-polls');
    box.replaceChildren();
    if (!state.polls.length) { box.append(el('p', 'note', T.noPolls)); return; }
    state.polls.forEach(function (meta) {
      var card = el('article', 'tr-row');
      card.append(el('p', 'note', T.loading));
      box.append(card);
      api('/tr/poll?lang=' + state.lang + '&id=' + meta.id).then(function (r) { return r.json(); }).then(function (poll) {
        card.replaceWith(pollCard(poll, meta.id));
      });
    });
  }
  function whoOf(entry, record) {
    if (!entry.gone) return entry.name;
    var ids = record.ghosts && record.ghosts[entry.gone];
    return fmt(T.gone, { id: entry.gone }) + (ids ? ' (' + ids.join(', ') + ')' : '');
  }
  function pollCard(poll, id) {
    var card = el('article', 'tr-row tr-poll');
    var head = el('div', 'tr-head');
    head.append(chip(poll.status === 'decided' ? T.decided : T.st.open, poll.status === 'decided' ? 'leaf' : 'gold'));
    head.append(el('span', 'note', fmt(T.by, { who: whoOf(poll, poll) })));
    card.append(head, el('h3', 'tr-term', poll.term));
    if (poll.note) card.append(textBlock(poll.note, 'note'));
    var total = poll.options.reduce(function (s, o) { return s + o.votes.length; }, 0);
    poll.options.slice().sort(function (a, b) { return b.votes.length - a.votes.length; }).forEach(function (o) {
      var won = poll.decided && poll.decided.option === o.id;
      var row = el('div', 'tr-opt' + (won ? ' won' : ''));
      var bar = el('span', 'tr-bar-fill');
      bar.style.width = (total ? Math.round(o.votes.length * 100 / total) : 0) + '%';
      var label = el('span', 'tr-opt-text', o.text);
      var count = el('span', 'tr-votes', fmt(T.votes, { n: o.votes.length }));
      row.append(bar, label, count);
      if (poll.status === 'open' && canWrite()) {
        var mine = o.votes.indexOf(state.me.id) >= 0;
        var v = el('button', 'btn small' + (mine ? ' primary' : ''), mine ? T.voted : T.vote);
        v.type = 'button';
        v.onclick = function () {
          post('/tr/poll/vote', { lang: state.lang, id: id, option: o.id }).then(function (b) {
            if (b.okay) card.replaceWith(pollCard(b.poll, id)); else alert(why(b.why));
          });
        };
        row.append(v);
      }
      if (poll.status === 'open' && state.me.decide) {
        var d = el('button', 'btn small', T.decide);
        d.type = 'button';
        d.onclick = function () {
          post('/tr/poll/decide', { lang: state.lang, id: id, option: o.id }).then(function (b) {
            if (b.okay) { card.replaceWith(pollCard(b.poll, id)); loadPolls(); } else alert(why(b.why));
          });
        };
        row.append(d);
      }
      card.append(row);
    });
    if (poll.status === 'open' && canWrite()) {
      var add = el('div', 'tr-actions');
      var input = el('input');
      input.type = 'text';
      input.maxLength = 80;
      input.placeholder = T.addOption;
      var go = el('button', 'btn small', T.add);
      go.type = 'button';
      go.onclick = function () {
        if (!input.value.trim()) return;
        post('/tr/poll/option', { lang: state.lang, id: id, text: input.value }).then(function (b) {
          if (b.okay) card.replaceWith(pollCard(b.poll, id)); else alert(why(b.why));
        });
      };
      add.append(input, go);
      card.append(add);
    }
    if (poll.status === 'decided' && state.me.decide) {
      var again = el('button', 'btn small', T.reopen);
      again.type = 'button';
      again.onclick = function () {
        post('/tr/poll/reopen', { lang: state.lang, id: id }).then(function (b) {
          if (b.okay) { card.replaceWith(pollCard(b.poll, id)); loadPolls(); }
        });
      };
      var bar2 = el('div', 'tr-actions');
      bar2.append(again);
      card.append(bar2);
    }
    return card;
  }
  document.getElementById('np-send').onclick = function () {
    var msg = document.getElementById('np-msg');
    var opts = document.getElementById('np-opts').value.split(/[,，、]/).map(function (s) { return s.trim(); }).filter(Boolean);
    post('/tr/poll', { lang: state.lang, term: document.getElementById('np-term').value, note: document.getElementById('np-note').value, options: opts })
      .then(function (b) {
        if (!b.okay) { msg.className = 'msg bad'; msg.textContent = why(b.why); return; }
        msg.className = 'msg ok';
        msg.textContent = T.opened;
        ['np-term', 'np-opts', 'np-note'].forEach(function (x) { document.getElementById(x).value = ''; });
        loadPolls().then(drawPolls);
      });
  };

  function switchLang() {
    state.lang = langSel.value;
    try { localStorage.setItem(LANG_KEY, state.lang); } catch (e) {}
    drawRole();
    Promise.all([loadProps(), loadPolls()]).then(function () {
      if (!document.getElementById('tp-props').hidden) drawProps();
      if (!document.getElementById('tp-terms').hidden) drawPolls();
    });
    loadIndex();
  }
  langSel.onchange = switchLang;
  api('/tr/me').then(function (r) { return r.json(); }).catch(function () { return {}; }).then(function (me) {
    state.me = { signedIn: Boolean(me.signedIn), langs: me.langs || [], decide: Boolean(me.decide), id: me.id || '' };
    switchLang();
  });
`);

const PAGES = {
  '/': ['home.title', HOME, HOME_JS + scriptFile('home-reveal', 'revealOnScroll(' + JSON.stringify(REVEAL_HOME) + ')')],
  '/features': ['features.title', FEATURES, FEATURES_JS + scriptFile('features-reveal', 'revealOnScroll(' + JSON.stringify(REVEAL_FEATURES) + ')')],
  '/capes': ['capes.title', CAPES, CAPES_JS],
  '/account': ['account.title', ACCOUNT, ACCOUNT_JS],
  '/admin': ['admin.title', ADMIN, ADMIN_JS],
  '/translate': ['tr.title', TRANSLATE, TRANSLATE_JS],
  '/feedback': ['fb.title', FEEDBACK, FEEDBACK_JS],
  '/privacy': ['privacy.title', legalPage('privacy'), ''],
  '/terms': ['terms.title', legalPage('terms'), ''],
};

export const PATHS = Object.keys(PAGES);

const built = new Map();

export function renderLost(lang) {
  const code = Object.hasOwn(TEXT, lang) ? lang : 'en';
  const key = code + ' 404';
  if (!built.has(key)) {
    const text = { ...filled(code) };
    text['meta.desc'] = attribute(plain(text['lost.p']));
    built.set(key, layout(code, '%%lost.title%%', '', LOST, '')
      .replace(/%%([\w.]+)%%/g, (all, name) => {
        if (name === 'js') return JSON.stringify(text.js).replace(/</g, '\\u003c');
        return Object.hasOwn(text, name) ? text[name] : all;
      }));
  }
  return built.get(key);
}

export function render(path, lang, bare) {
  if (!Object.hasOwn(PAGES, path)) return null;
  const code = Object.hasOwn(TEXT, lang) ? lang : 'en';
  const key = code + ' ' + path + (bare ? ' bare' : '');
  if (!built.has(key)) {
    const text = { ...filled(code) };
    // 什麼語言都沒說的多半是抓連結預覽的機器人：給中英各一句
    text['meta.desc'] = attribute(bare ? plain(TEXT.zh['home.lede']) + ' ' + plain(TEXT.en['home.lede'])
      : plain(text['home.lede']));
    text['tpl.guide'] = TPL(code.startsWith('zh') ? 'guide.png' : 'guide.en.png');
    const [title, body, script] = PAGES[path];
    const html = layout(code, '%%' + title + '%%', path, body, script)
      .replace(/%%([\w.]+)%%/g, (all, name) => {
        if (name === 'js') return JSON.stringify(text.js).replace(/</g, '\\u003c');
        if (name === 'demo') return JSON.stringify({ ui: DEMO.ui[code], text: DEMO.text, tex: TEX_URL }).replace(/</g, '\\u003c');
        return Object.hasOwn(text, name) ? text[name] : all;
      });
    built.set(key, html);
  }
  return built.get(key);
}
