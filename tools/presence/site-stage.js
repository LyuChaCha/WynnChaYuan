export const STAGE_PARTS = '<template id="stage-parts">'
  + '<svg data-part="cursor" viewBox="0 0 14 18" aria-hidden="true"><path d="M1 1v13l3.5-3 2.4 5.2 2-.9L6.5 10H11z" fill="#fff" stroke="#000" stroke-width="1"/></svg>'
  + '</template>';

export const STAGE_JS = String.raw`
  var STAGE_TAGS = { zh_tw: 'zh-Hant', zh_cn: 'zh-Hans', ja_jp: 'ja', ko_kr: 'ko', ru_ru: 'ru', es_es: 'es', tr_tr: 'tr' };
  var STAGE_DEFAULTS = { tooltip: 0, names: 1, gap: 12, dialogue: 1, choices: 1, nametag: 1, chat: 1, tracker: 0,
    titles: true, bossbar: true, scoreboard: true, held: true };

  var DOLL_PARTS = [
    ['head', 0, 0, 8, 8, 8, 32, 0, 4, 0, .5],
    ['body', 16, 16, 8, 12, 4, 16, 32, 4, 8, .25],
    ['armR', 40, 16, 4, 12, 4, 40, 32, 0, 8, .25],
    ['armL', 32, 48, 4, 12, 4, 48, 48, 12, 8, .25],
    ['legR', 0, 16, 4, 12, 4, 0, 32, 4, 20, .25],
    ['legL', 16, 48, 4, 12, 4, 0, 48, 8, 20, .25]
  ];
  function makeDoll(unit) {
    var doll = el('div', 'doll');
    var parts = {};
    function face(u, v, w, h, how, shade) {
      var one = el('i', '');
      one.style.cssText = 'width:' + (w * unit) + 'px;height:' + (h * unit) + 'px;margin:' + (-h * unit / 2) + 'px 0 0 ' + (-w * unit / 2) + 'px;'
        + 'background-size:' + (64 * unit) + 'px ' + (64 * unit) + 'px;background-position:' + (-u * unit) + 'px ' + (-v * unit) + 'px;'
        + 'transform:' + how + ';' + (shade < 1 ? 'filter:brightness(' + shade + ');' : '');
      return one;
    }
    function faces(into, u, v, w, h, d) {
      into.append(
        face(u + d, v + d, w, h, 'translateZ(' + (d * unit / 2) + 'px)', 1),
        face(u + d + w + d, v + d, w, h, 'rotateY(180deg) translateZ(' + (d * unit / 2) + 'px)', .62),
        face(u, v + d, d, h, 'rotateY(-90deg) translateZ(' + (w * unit / 2) + 'px)', .8),
        face(u + d + w, v + d, d, h, 'rotateY(90deg) translateZ(' + (w * unit / 2) + 'px)', .8),
        face(u + d, v, w, d, 'rotateX(90deg) translateZ(' + (h * unit / 2) + 'px)', 1),
        face(u + d + w, v, w, d, 'rotateX(-90deg) translateZ(' + (h * unit / 2) + 'px)', .55));
    }
    DOLL_PARTS.forEach(function (p) {
      var w = p[3], h = p[4], d = p[5], grow = p[10];
      var part = el('div', '');
      part.style.cssText = 'left:' + (p[8] * unit) + 'px;top:' + (p[9] * unit) + 'px;width:' + (w * unit) + 'px;height:' + (h * unit) + 'px;';
      faces(part, p[1], p[2], w, h, d);
      var over = el('div', 'over');
      over.style.transform = 'scale3d(' + ((w + 2 * grow) / w) + ',' + ((h + 2 * grow) / h) + ',' + ((d + 2 * grow) / d) + ')';
      faces(over, p[6], p[7], w, h, d);
      part.append(over);
      doll.append(part);
      parts[p[0]] = part;
    });
    parts.head.style.transformOrigin = '50% 100%';
    function look(yaw, pitch) {
      doll.style.transform = 'rotateX(' + (pitch * .4).toFixed(2) + 'deg) rotateY(' + (yaw / 2).toFixed(2) + 'deg)';
      parts.head.style.transform = 'rotateY(' + (yaw / 2).toFixed(2) + 'deg) rotateX(' + (pitch * .6).toFixed(2) + 'deg)';
    }
    look(26, -6);
    return { node: doll, look: look };
  }

  function makeStage(host, D) {
    var scr = el('div', 'scr');
    var bg = el('div', 'bg');
    var hud = el('div', 'hud');
    scr.append(bg, hud);
    host.replaceChildren(scr);
    var wide = true;
    var last = null;
    var turn = 0;
    var first = true;
    var hush = false;
    var lang = 'zh_tw';
    function T(src) { return (D.text[lang][src] || src).replace(/\{c\d+\}/g, ''); }
    function bit(tag, className, text, translated) {
      var node = el(tag, className, text);
      if (translated) node.lang = STAGE_TAGS[lang];
      return node;
    }
    function fill(template, values) {
      var next = 0;
      return template.replace(/\{~(\d*)\}/g, function (all, n) {
        var at = n ? Number(n) - 1 : next++;
        return at < values.length ? values[at] : '0';
      });
    }
    function pic(name, className) {
      var img = el('img', className);
      img.alt = '';
      img.draggable = false;
      img.src = D.tex[name];
      return img;
    }
    function item(name, x, y) {
      var img = pic('item-' + name + '.png', /^(bow|spear|wand|dagger|relik)$/.test(name) ? 'it smooth' : 'it');
      img.style.left = x + 'px';
      img.style.top = y + 'px';
      return img;
    }
    var doll = null;
    var dollBox = el('div', 'dollbox');
    var dollClip = el('div', 'dollclip');
    dollClip.append(dollBox);
    var aimAt = null;
    function aim() {
      var at = aimAt;
      aimAt = null;
      if (!at || !doll || !dollClip.isConnected) return;
      var box = dollClip.getBoundingClientRect();
      if (!box.width) return;
      var scale = box.width / 98;
      var dx = (at[0] - (box.left + box.width / 2)) / scale;
      var dy = (at[1] - (box.top + box.height / 2)) / scale;
      doll.look(Math.atan(dx / 80) * 40, -Math.atan(dy / 80) * 20);
    }
    window.addEventListener('pointermove', function (event) {
      var waiting = aimAt;
      aimAt = [event.clientX, event.clientY];
      if (!waiting) requestAnimationFrame(aim);
    }, { passive: true });
    function type(node, text) {
      var mine = ++turn;
      var chars = Array.from(text);
      var shown = 0;
      if (calm) { node.textContent = text; return; }
      node.textContent = '';
      (function step() {
        if (mine !== turn || !node.isConnected) return;
        shown++;
        node.textContent = chars.slice(0, shown).join('');
        if (shown < chars.length) setTimeout(step, 26);
      })();
    }

    var ITEMS = {
      bony: { name: 'Bony Bow', icon: 'bow', slot: [0, 3], tier: 'rare', pills: [['RARE', 'p-rare'], ['BOW', '']],
        emblem: ['emblem-bow.png', 3.2], dps: '89', speed: ['{#} Fast ({~} hits/s)', '2.5'],
        damage: [['ico-neutral.png', '15-36'], ['ico-air.png', '8-12']], skills: [0, 0, 0, 0, 8],
        needs: [['{#} Class Type{#}Archer/Hunter', [], true], ['{#} Combat Level{#}{~}', ['22'], true]],
        ids: [['Agility', '+6', 'ico-air.png'], ['Main Attack Damage', '+8%']], pages: 3 },
      oak: { name: 'Flawless Oak Bow', icon: 'oakbow', slot: [6, 0], tier: 'normal', pills: [['NORMAL', 'p-normal'], ['BOW', '']],
        emblem: ['emblem-bow.png', 3.2], dps: '74', speed: ['{#} Slow ({~} hits/s)', '1.5'],
        damage: [['ico-neutral.png', '41-58']],
        needs: [['{#} Class Type{#}Archer/Hunter', [], true], ['{#} Combat Level{#}{~}', ['28'], false]], pages: 3 },
      flesh: { name: 'Rotten Flesh', icon: 'flesh', qty: '7', slot: [7, 1], tier: 'ingredient',
        pills: [['★★★', 'p-stars'], ['INGREDIENT', 'p-ingredient']],
        emblem: ['emblem-ingredient.png', 2.6], level: '1',
        jobs: [['prof-weaponsmithing.png', 'Weaponsmithing'], ['prof-woodworking.png', 'Woodworking']],
        ids: [['Durability', '-17'], ['Main Attack Damage%', ['+2%', '+4%']]] }
    };
    var ITEM_ORDER = ['bony', 'oak', 'flesh'];
    var picked = 'bony';
    var touched = false;

    function tipBox(frame, item, name, nameTranslated, translated) {
      var box = el('div', 'tip ' + frame + ' t-' + item.tier);
      function word(src) { return translated ? T(src) : src; }
      function line(className, text) { return bit('div', className, text, translated); }
      function lead(src, value) {
        var row = line('lead', '');
        var parts = word(src).replace('{#}', '').split(/\{~\d*\}/);
        row.append(parts[0], el('b', '', value), parts.slice(1).join(''));
        return row;
      }

      var head = el('div', 'tip-head');
      var emblem = pic(item.emblem[0], 'emblem');
      emblem.style.width = emblem.style.height = item.emblem[1] + 'em';
      head.append(emblem);
      var title = el('div', '');
      title.append(bit('div', 'tname', name, nameTranslated));
      var pills = el('div', 'pills');
      item.pills.forEach(function (pill) { pills.append(el('b', 'pill ' + pill[1], pill[0])); });
      title.append(pills);
      head.append(title);
      box.append(head);

      if (item.dps) {
        box.append(lead('{~} DPS', item.dps));
        var speed = line('row soft', '');
        var said = fill(word(item.speed[0]), [item.speed[1]]).replace('{#}', '').replace(/^\s+/, '');
        var cut = said.search(/[(（]/);
        speed.append(pic('ico-speed.png', 'ico'), cut > 0 ? said.slice(0, cut) : said);
        if (cut > 0) speed.append(el('span', 'dimmer', said.slice(cut)));
        box.append(speed);
        var damage = el('div', 'row soft dmg');
        item.damage.forEach(function (one) { damage.append(pic(one[0], 'ico'), one[1]); });
        box.append(damage);
      }
      if (item.level) {
        box.append(lead('{#}{~} Crafting Level', item.level));
        box.append(line('soft', word('{#}Can be used in recipes for').replace('{#}', '')));
        var jobs = line('row soft jobs', '');
        item.jobs.forEach(function (job) {
          var one = el('span', '');
          one.append(pic(job[0], 'ico'), word(job[1]));
          jobs.append(one);
        });
        box.append(jobs);
      }
      box.append(el('i', 'divider'));

      if (item.skills) {
        var reqs = el('div', 'reqs');
        item.skills.forEach(function (need, i) {
          var one = el('span', 'req' + (need ? ' on' : ''));
          var sprite = el('i', '');
          sprite.style.backgroundPosition = (-i * 2.4) + 'em ' + (need ? '0' : '-2.4em');
          one.append(sprite, el('u', need ? 'ok' : ''), String(need));
          reqs.append(one);
        });
        box.append(reqs);
      }
      (item.needs || []).forEach(function (need) {
        var parts = word(need[0]).split('{#}');
        var row = el('div', 'kv');
        var label = bit('span', '', '', translated);
        label.append(el('u', need[2] ? 'ok' : 'no'), (parts[1] || '').trim());
        row.append(label, bit('span', 'soft', fill((parts[2] || '').trim(), need[1]), translated));
        box.append(row);
      });
      if (item.ids) {
        if (item.needs) box.append(el('i', 'divider'));
        item.ids.forEach(function (id) {
          var row = el('div', 'kv');
          var label = bit('span', '', '', translated);
          if (id[2]) label.append(pic(id[2], 'ico'));
          label.append(word(id[0]));
          var value = el('span', '');
          function number(text) { return el('span', text.charAt(0) === '-' ? 'worse' : 'good', text); }
          if (typeof id[1] === 'string') value.append(number(id[1]));
          else {
            var between = word('{~} to {~}').split(/\{~\d*\}/);
            value.append(number(id[1][0]), bit('span', 'dimmer', between[1] || ' ', translated), number(id[1][1]));
          }
          row.append(label, value);
          box.append(row);
        });
      }
      if (item.pages) {
        var pager = el('div', 'pager');
        for (var p = 0; p < item.pages; p++) pager.append(el('span', 'dot' + (p ? '' : ' on')));
        pager.append(el('kbd', '', 'F'));
        box.append(pager);
      }
      return box;
    }

    var ITEM_EDGE = 361;
    var FRAME_INSET = { rare: 4, normal: 2, ingredient: 5, plain: 2 };
    var NARROW_INV_WIDTH = 374 * .52;
    function drawItems(state) {
      hud.append(el('div', 'dim'));
      var inv = el('div', touched ? 'inv used' : 'inv');
      function slot(column, row) { return [(15 + 18 * column) * 2, (row === 3 ? 150 : 92 + 18 * row) * 2]; }
      function put(node, at) {
        node.style.left = at[0] + 'px';
        node.style.top = at[1] + 'px';
        return node;
      }
      [[15, 16, 0], [15, 34, 1], [15, 52, 2], [15, 70, 3], [15, 92, 4], [33, 92, 4], [51, 92, 5], [69, 92, 6]].forEach(function (g) {
        var ghost = put(el('i', 'ghost'), [g[0] * 2, g[1] * 2]);
        ghost.style.backgroundPosition = (-g[2] * 32) + 'px 0';
        inv.append(ghost);
      });
      if (!doll) { doll = makeDoll(3.5); dollBox.append(doll.node); }
      inv.append(dollClip);
      inv.append(put(pic('item-pouch.png', 'it'), slot(4, 0)), put(pic('item-book.png', 'it'), slot(8, 3)));
      var glow = el('i', 'hover');
      inv.append(glow);
      var buttons = {};
      ITEM_ORDER.forEach(function (key) {
        var item = ITEMS[key];
        var button = put(el('button', 'slot'), slot(item.slot[0], item.slot[1]));
        button.type = 'button';
        button.setAttribute('aria-label', T(item.name) === item.name ? item.name : T(item.name) + ' (' + item.name + ')');
        button.append(pic('item-' + item.icon + '.png', item.icon === 'bow' ? 'smooth' : ''));
        if (item.qty) button.append(el('b', 'qty', item.qty));
        function pick() {
          var first = !touched;
          touched = true;
          inv.classList.add('used');
          if (picked !== key || first) { picked = key; show(); }
        }
        button.addEventListener('pointerenter', pick);
        button.addEventListener('focus', pick);
        button.addEventListener('click', pick);
        buttons[key] = button;
        inv.append(button);
      });
      var cursor = document.getElementById('stage-parts').content.querySelector('[data-part="cursor"]').cloneNode(true);
      cursor.classList.add('cursor');
      inv.append(cursor);
      var duo = el('div', 'duo');
      hud.append(inv, duo);

      function show() {
        var item = ITEMS[picked];
        var at = slot(item.slot[0], item.slot[1]);
        put(glow, at);
        put(cursor, [at[0] + 16, at[1] + 14]);
        cursor.style.display = touched ? 'none' : '';
        Object.keys(buttons).forEach(function (key) { buttons[key].setAttribute('aria-pressed', String(key === picked)); });
        var translatedName = T(item.name);
        var name = state.names === 0 ? translatedName : state.names === 1 ? translatedName + ' (' + item.name + ')' : item.name;
        var panel = state.tooltip === 0;
        var replace = state.tooltip === 1;
        duo.replaceChildren(tipBox('f-' + item.tier, item, replace ? name : item.name, replace && state.names !== 2, replace));
        if (panel) {
          var gap = el('div', 'gapmark');
          var insetLeft = FRAME_INSET[item.tier] || 0;
          gap.style.width = Math.max(0, state.gap - insetLeft - FRAME_INSET.plain) / 10 + 'em';
          gap.style.setProperty('--frame-l', insetLeft / 10 + 'em');
          gap.style.setProperty('--frame-r', FRAME_INSET.plain / 10 + 'em');
          gap.append(el('span', '', state.gap + ' px'));
          duo.append(gap, tipBox('ours', item, name, state.names !== 2, true));
        }
        zoomToFit();
      }
      function zoomToFit() {
        hud.removeAttribute('style');
        inv.style.left = '';
        if (!duo.isConnected) return;
        var W = wide ? 960 : 480;
        var H = wide ? 480 : 470;
        var content = Array.prototype.reduce.call(duo.children, function (sum, child) { return sum + child.offsetWidth; }, 0);
        var need = wide ? ITEM_EDGE + content + 5 : content + 8;
        if (need <= W) return;
        var k = W / need;
        hud.style.cssText = 'right:auto;bottom:auto;width:' + W / k + 'px;height:' + H / k + 'px;transform:scale(' + k + ');transform-origin:0 0';
        if (!wide) inv.style.left = (W / k - NARROW_INV_WIDTH) / 2 + 'px';
      }
      show();
      if (document.fonts && document.fonts.status !== 'loaded') document.fonts.ready.then(zoomToFit);
    }

    function drawBar(heldTranslated) {
      if (heldTranslated !== null) {
        hud.append(bit('div', 'held', heldTranslated ? T('Bony Bow') : 'Bony Bow', heldTranslated));
      }
      var bar = el('div', 'hudbar');
      bar.append(el('span', 'num hp', '420 / 436'), el('span', 'num lv', '22'), el('span', 'num mp', '62 / 62'));
      [['bow', 0], ['heal', 1], ['mana', 2], ['book', 8]].forEach(function (one) {
        bar.append(item(one[0], 56 + 40 * one[1], 66));
      });
      hud.append(bar);
    }

    function drawDialogue(state, typed) {
      var LINE = 'Well, what are we waiting for?';
      var CHOICES = ['Yes', 'Goodbye.'];
      var stack = el('div', 'dlg');
      var extra = el('div', 'dlg-extra');
      if (state.dialogue === 0) extra.append(bit('div', 'tip ours say', T(LINE), true));
      if (state.choices === 0) {
        var choiceBox = el('div', 'tip ours right');
        CHOICES.forEach(function (choice) { choiceBox.append(bit('div', '', T(choice), true)); });
        extra.append(choiceBox);
      }
      var options = el('div', 'options');
      CHOICES.forEach(function (choice, i) {
        var bar = bit('div', 'option' + (i === 0 ? ' on' : ''), state.choices === 1 ? T(choice) : choice, state.choices === 1);
        if (i === 0) bar.prepend(pic('cursor.png', 'pick'));
        options.append(bar);
      });
      var box = el('div', 'dlgbox');
      box.append(el('b', '', 'Aledar'));
      var line = bit('p', '', '', state.dialogue === 1);
      box.append(line);
      var hint = el('div', 'hint2');
      var on = state.dialogue === 1;
      hint.append(el('kbd', '', 'SHIFT'), bit('span', '', on ? T('to continue') : 'to continue', on));
      box.append(hint);
      stack.append(extra, options, box);
      hud.append(stack);
      drawBar(null);
      if (state.dialogue === 1 && typed) type(line, T(LINE));
      else { turn++; line.textContent = state.dialogue === 1 ? T(LINE) : LINE; }
    }

    function drawWorld(state) {
      var MOB = 'Zombie {#}{#}';
      var boss = bit('div', 'boss', (state.bossbar ? T(MOB) : MOB).replace(/\s*\{#\}/g, ''), state.bossbar);
      boss.append(el('b', 'mlv', 'LV 13'), el('i'));
      hud.append(boss);

      var track = el('div', 'trk');
      var inPlace = state.tracker === 0;
      var box = el('div', 'box');
      box.append(bit('div', 'c-go', inPlace ? T('World Event') : 'World Event', inPlace),
        bit('div', 'c-g', inPlace ? T('Click To Track') : 'Click To Track', inPlace));
      track.append(box);
      if (state.tracker === 1) {
        var side = el('div', 'tip ours');
        side.append(bit('div', 'c-go', T('World Event'), true), bit('div', 'c-g', T('Click To Track'), true));
        track.append(side);
      }
      hud.append(track);

      var on = state.scoreboard;
      var board = el('div', state.tracker === 1 ? 'board below' : 'board');
      board.append(bit('div', '', on ? T('Daily Objective:') : 'Daily Objective:', on));
      [['- Loot Chests T{~}+: {~}/{~}', ['2', '3', '5']], ['- Mobs slain: {~}/{~}', ['42', '100']]].forEach(function (row) {
        board.append(bit('div', '', fill(on ? T(row[0]) : row[0], row[1]), on));
      });
      hud.append(board);

      var NPC = '{#}\nParty Finder\nFind a party of queue for a raid';
      function plateLines(translated) {
        var lines = (translated ? T(NPC) : NPC).split('\n');
        return [lines[1] || '', lines.slice(2).join(' ')];
      }
      var npc = el('div', 'npc');
      var inPlace = state.nametag === 1;
      var shown = plateLines(inPlace);
      var plate = el('div', 'plate');
      plate.append(bit('b', '', shown[0], inPlace), bit('span', '', shown[1], inPlace));
      npc.append(plate);
      if (state.nametag === 0) {
        var ours = plateLines(true);
        var look = el('div', 'tip ours look');
        look.append(bit('div', 'c-m', ours[0], true), bit('div', 'c-gr', ours[1], true));
        npc.append(look);
      }
      hud.append(npc);

      hud.append(bit('div', 'title', state.titles ? T('Level Up!') : 'Level Up!', state.titles));

      var chat = el('div', 'chat');
      var said = 'The trade has been completed!';
      if (state.chat !== 0) chat.append(el('div', '', said));
      if (state.chat !== 2) chat.append(bit('div', state.chat === 1 ? 'ours' : '', T(said), true));
      hud.append(chat);

      drawBar(state.held);
    }

    function draw(tab, state, language, typed) {
      last = [tab, state, language];
      lang = language;
      scr.dataset.tab = tab;
      scr.classList.toggle('still', first || hush);
      first = false;
      hud.replaceChildren();
      hud.removeAttribute('style');
      if (tab === 'items') drawItems(state);
      else if (tab === 'dialogue') drawDialogue(state, typed);
      else drawWorld(state);
    }
    function fit() {
      var width = host.clientWidth;
      if (!width) return;
      var nowWide = width >= 640;
      var W = nowWide ? 960 : 480;
      var H = nowWide ? 480 : 470;
      scr.classList.toggle('narrow', !nowWide);
      scr.style.width = W + 'px';
      scr.style.height = H + 'px';
      scr.style.transform = 'scale(' + (width / W) + ')';
      host.style.aspectRatio = W + ' / ' + H;        /* 高度交給 aspect-ratio：自己寫高度的話寬會反過來跟著高縮 */
      if (nowWide !== wide) {
        wide = nowWide;
        if (last) {
          var was = hush;
          hush = true;
          draw(last[0], last[1], last[2], false);
          hush = was;
        }
      }
    }
    wide = host.clientWidth >= 640 || !host.clientWidth;
    fit();
    if ('ResizeObserver' in window) new ResizeObserver(fit).observe(host);
    else window.addEventListener('resize', fit);
    return { draw: draw, quiet: function (on) { hush = Boolean(on); } };
  }
`;
