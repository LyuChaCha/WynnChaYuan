import { FONTS } from './demo-fonts.js';
import { UI_FONTS } from './site-fonts.js';
import { TEXTURES } from './demo-textures.js';

export const STYLE = String.raw`
  :root {
    --ground: #0f1512; --panel: #161e19; --panel-2: #1c2620; --line: #2a382f;
    --ink: #edf3ee; --ink-2: #b4c2b8; --leaf: #7fd08a; --leaf-deep: #2f7d46;
    --gold: #d8b765; --cyan: #5bd8e8; --danger: #e08a8a;
    --out: cubic-bezier(0.23, 1, 0.32, 1); --arrive: cubic-bezier(0.16, 1, 0.3, 1); --move: cubic-bezier(0.77, 0, 0.175, 1);
    --fast: 140ms; --base: 200ms;
    --exit: cubic-bezier(0.32, 0, 0.67, 0);
  }
  * { box-sizing: border-box; }
  .skip { position: absolute; left: 12px; top: -60px; z-index: 20; padding: 8px 14px; border-radius: 3px; background: var(--leaf); color: #0c1a10; font-weight: 400; font-size: 20px; }
  .skip:focus { top: 10px; text-decoration: none; }
  body::after {
    content: ""; position: fixed; inset: 0; z-index: 30; pointer-events: none; opacity: .05; mix-blend-mode: overlay;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='160' height='160' filter='url(%23n)'/%3E%3C/svg%3E");
  }
  .lost { display: grid; justify-items: start; gap: 6px; padding: 8vh 0 6vh; }
  .lost .code { font-family: PxTC, ui-monospace, monospace; font-size: 80px; line-height: 1; color: var(--leaf); text-shadow: 5px 5px 0 #0a2413; }
  .lost h1 { margin-top: 18px; }
  @view-transition { navigation: auto; }
  ::view-transition-old(root), ::view-transition-new(root) { animation-duration: 150ms; animation-timing-function: ease; }
  ::view-transition-group(*) { animation-duration: 320ms; animation-timing-function: var(--move); }
  ::view-transition-group(root), ::view-transition-group(top) { animation-duration: 150ms; }
  html { scroll-behavior: smooth; scroll-padding-top: 76px; scrollbar-color: #2f4236 var(--ground); accent-color: var(--leaf); }
  ::selection { background: var(--leaf-deep); color: #fff; }
  input, textarea { caret-color: var(--leaf); }
  body {
    margin: 0; min-height: 100vh; display: flex; flex-direction: column;
    background: var(--ground); color: var(--ink);
    font: 400 20px/32px WcyPx, "Segoe UI", system-ui, sans-serif; letter-spacing: 0; -webkit-text-size-adjust: 100%;
    -webkit-font-smoothing: none; font-smooth: never; text-rendering: optimizeSpeed; font-synthesis: none; font-variant-ligatures: none;
    background-image: radial-gradient(1100px 520px at 50% -180px, #1d3324 0%, transparent 70%);
    background-repeat: no-repeat;
  }
  a { color: var(--leaf); text-decoration: none; text-underline-offset: 3px; }
  a:hover { text-decoration: underline; }
  a:focus-visible, button:focus-visible, input:focus-visible, select:focus-visible, textarea:focus-visible, summary:focus-visible {
    outline: 2px solid var(--leaf); outline-offset: 2px;
  }
  .top { border-bottom: 1px solid var(--line); background: #0f1512e6; position: sticky; top: 0; z-index: 5; backdrop-filter: blur(8px); view-transition-name: top; }
  .top .in { position: relative; z-index: 1; max-width: 1120px; margin: 0 auto; padding: 12px 20px; display: flex; flex-wrap: nowrap; align-items: center; gap: 18px; }
  .hn-leaves { position: absolute; left: 0; top: 0; z-index: 0; width: 100%; height: 61px; pointer-events: none; image-rendering: pixelated; opacity: .72; }
  .brand { display: flex; align-items: center; gap: 10px; color: var(--ink); font-weight: 400; font-size: 20px; letter-spacing: 0; flex: none; }
  .brand:hover { text-decoration: none; }
  .brand .logo { width: 30px; height: 30px; flex: none; transform-origin: 30% 85%; transition: transform 520ms var(--out); }
  nav { display: flex; gap: 4px; flex-wrap: wrap; }
  nav a { padding: 6px 12px; border-radius: 3px; color: var(--ink-2); font-size: 20px; transition: color var(--fast) ease, background-color var(--fast) ease; }
  nav a:hover { color: var(--ink); background: var(--panel-2); text-decoration: none; }
  nav a[aria-current="page"] { color: var(--ink); background: var(--panel-2); }
  .hn { flex: 1 1 0; min-width: 0; overflow: hidden; display: block; }
  .hn.hn-live { overflow: visible; }
  .hn-list { display: flex; flex-wrap: nowrap; gap: 4px; margin: 0; padding: 0; list-style: none; }
  .hn-item { flex: none; position: relative; }
  .hn-item.hn-out { display: none; }
  .hn-link { display: inline-flex; align-items: center; gap: 4px; padding: 6px 12px; border: 0; border-radius: 3px; background: none; color: var(--ink-2); font: inherit; font-size: 20px; line-height: 32px; white-space: nowrap; cursor: pointer; transition: color 150ms ease, background-color 150ms ease, transform var(--fast) var(--out); }
  .hn-link:active, .hn-icon:active, .hn-avatar:active, .hn-plink:active { transform: scale(.97); }
  .hn-link:hover { color: var(--ink); background: var(--panel-2); text-decoration: none; }
  .hn-link[aria-current="page"], .hn-link.hn-cur, .hn-trig[aria-expanded="true"] { color: var(--ink); background: var(--panel-2); }
  .hn-link.hn-cur { box-shadow: inset 0 -2px 0 var(--leaf-deep); }
  .hn-trig .ico, .hn-avatar > .ico, #get > .ico:last-child, #hn-globe > .ico:last-child { width: 14px; height: 14px; transition: transform var(--base) var(--out); }
  .hn-trig[aria-expanded="true"] .ico, .hn-avatar[aria-expanded="true"] > .ico, #get[aria-expanded="true"] > .ico:last-child, #hn-globe[aria-expanded="true"] > .ico:last-child { transform: rotate(180deg); }
  .hn .menu { left: 0; right: auto; width: auto; min-width: 190px; transform-origin: top left; }
  .tools { margin-left: auto; display: flex; align-items: center; gap: 8px; flex: none; }
  .ico { width: 16px; height: 16px; flex: none; }
  .btn.small { padding: 5px 11px; font-size: 20px; border-radius: 3px; gap: 6px; }
  .hn-icon { display: inline-flex; align-items: center; justify-content: center; gap: 2px; min-width: 34px; height: 34px; padding: 0 8px; border: 1px solid transparent; border-radius: 3px; background: transparent; color: var(--ink-2); font: inherit; cursor: pointer; transition: color 150ms ease, background-color 150ms ease, border-color 150ms ease, transform var(--fast) var(--out); }
  .hn-icon:hover { text-decoration: none; }
  .hn-icon > .ico { width: 18px; height: 18px; }
  #hn-globe > .ico:last-child { width: 13px; height: 13px; }
  .hn-icon[aria-expanded="true"] { color: var(--ink); background: var(--panel-2); }
  .hn-tip { position: relative; }
  .hn-tip::after {
    content: attr(data-tip); position: absolute; top: calc(100% + 8px); left: 50%; z-index: 7; padding: 3px 9px;
    border: 1px solid var(--line); border-radius: 3px; background: var(--panel-2); color: var(--ink); font-size: 20px; line-height: 32px; white-space: nowrap;
    opacity: 0; pointer-events: none; transform: translate(-50%, -3px); transition: opacity var(--fast) ease, transform var(--fast) var(--out);
  }
  .hn-tip:focus-visible::after { opacity: 1; transform: translate(-50%, 0); transition-delay: 0ms; }
  .hn-tip[aria-expanded="true"]::after { opacity: 0 !important; transition-delay: 0ms !important; }
  .hn-sheen { position: relative; overflow: hidden; isolation: isolate; }
  .hn-sheen::after { content: ""; position: absolute; top: -40%; bottom: -40%; left: 0; width: 38%; pointer-events: none;
    background: linear-gradient(100deg, #fff0 0%, #ffffff8c 50%, #fff0 100%); transform: translateX(-190%) skewX(-18deg); }
  .hn-guest { display: flex; align-items: center; gap: 6px; }
  .hn-avatar { display: inline-flex; align-items: center; gap: 4px; height: 34px; padding: 3px 7px 3px 3px; border: 1px solid var(--line); border-radius: 3px; background: var(--panel); color: var(--ink-2); font: inherit; cursor: pointer; transition: border-color 150ms ease, color 150ms ease, transform var(--fast) var(--out); }
  .hn-avatar[aria-expanded="true"], .hn-avatar.hn-cur { border-color: var(--leaf-deep); color: var(--ink); }
  .hn-ava { width: 26px; height: 26px; border-radius: 6px; image-rendering: pixelated; flex: none; }
  .hn-ava-fb { display: inline-flex; align-items: center; justify-content: center; background: var(--panel-2); color: var(--leaf); }
  .hn-ava-fb .ico { width: 16px; height: 16px; }
  .hn-menu.hn-narrow { width: 200px; }
  .hn-acct { width: 240px; }
  .hn-acct-head { display: flex; align-items: center; gap: 10px; padding: 6px 10px 10px; margin-bottom: 4px; border-bottom: 1px solid var(--line); }
  .hn-acct-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--ink); font-weight: 400; font-size: 20px; }
  .hn-langs { display: grid; }
  .hn-sublangs { margin: 2px 0 2px 26px; padding-left: 6px; border-left: 1px solid var(--line); }
  .hn-mi { display: flex; align-items: center; gap: 10px; width: 100%; padding: 8px 10px; border: 0; border-radius: 3px; background: none; color: var(--ink); font: inherit; font-size: 20px; text-align: left; cursor: pointer; transition: background-color var(--fast) ease; }
  .menu > a.hn-mi { justify-content: flex-start; }
  .hn-mi .ico { color: var(--ink-2); }
  .hn-mi-val { margin-left: auto; color: var(--ink-2); font-size: 20px; }
  .hn-mi[aria-expanded] > .ico:last-child { width: 14px; height: 14px; transition: transform var(--fast) var(--out); }
  .hn-mi[aria-expanded="true"] > .ico:last-child { transform: rotate(180deg); }
  .hn-sep { height: 1px; margin: 6px 4px; border: 0; background: var(--line); }
  .hn-mhead { margin: 6px 0 2px; padding: 4px 10px 0; color: var(--ink-2); font-size: 20px; }
  .menu > .hn-mhead:first-child { margin-top: 0; padding-top: 2px; }
  .menu > a.hn-msub { padding-left: 18px; }
  .menu a[aria-current="page"] { background: var(--panel-2); box-shadow: inset 2px 0 0 var(--leaf); }
  .menu a:focus-visible, .hn-mi:focus-visible { outline-offset: -2px; background: var(--panel-2); }
  .hn-langs > a[aria-checked="true"], .hn-langs > a[aria-current="true"] { color: var(--leaf); }
  .hn-langs > a .ico { color: var(--leaf); }
  .hn-burger { display: none; }
  .hn-panel { display: none; }
  .hn-pnav, .hn-psec { max-width: 1120px; margin: 0 auto; padding: 10px 16px; display: grid; gap: 2px; }
  .hn-psec { border-top: 1px solid var(--line); }
  .hn-pnav { padding-top: 12px; }
  .hn-phead { margin: 4px 0 4px; padding: 0 10px; color: var(--ink-2); font-size: 20px; letter-spacing: 0; }
  .hn-plink { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 44px; padding: 8px 10px; border: 0; border-radius: 3px; background: none; color: var(--ink); font: inherit; font-size: 20px; text-align: left; cursor: pointer; transition: background-color 150ms ease, transform var(--fast) var(--out); }
  .hn-plink:hover { text-decoration: none; }
  .hn-plink .ico { color: var(--ink-2); }
  .hn-plink[aria-current="page"] { background: var(--panel-2); box-shadow: inset 3px 0 0 var(--leaf); }
  .hn-plink.hn-sub { padding-left: 22px; }
  .hn-pnote { margin: 4px 10px 2px; color: var(--ink-2); font-size: 20px; line-height: 32px; }
  .hn-pme { display: flex; align-items: center; gap: 10px; margin: 2px 0 4px; padding: 0 10px; color: var(--ink); font-weight: 400; min-width: 0; }
  .hn-pme span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .hn-plangs { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 2px; }
  .hn-plangs > a { display: flex; align-items: center; justify-content: space-between; min-height: 44px; padding: 8px 10px; border-radius: 3px; color: var(--ink); }
  .hn-plangs > a:hover { text-decoration: none; }
  .hn-pgrid { display: grid; gap: 2px; }
  .hn-psignin { justify-self: start; margin: 2px 10px 4px; }
  .pop { position: relative; }
  .menu {
    position: absolute; right: 0; top: calc(100% + 8px); z-index: 6; width: 252px; padding: 6px;
    border: 1px solid var(--line); border-radius: 6px; background: var(--panel); box-shadow: 0 18px 44px #000a;
    opacity: 0; visibility: hidden; transform: translateY(-6px) scale(.96); transform-origin: top right;
    transition: opacity 120ms var(--exit), transform 120ms var(--exit), visibility 0s linear 120ms;
  }
  .menu.open { opacity: 1; visibility: visible; transform: none; transition: opacity 180ms var(--out), transform 200ms var(--out), visibility 0s; }
  .menu > *, .hn-panel > * { opacity: 0; transform: translateY(-3px); transition: opacity 0s linear 120ms, transform 0s linear 120ms, background-color 150ms ease; }
  .menu.open > *, .hn-panel.open > * { opacity: 1; transform: none;
    transition: opacity 180ms var(--out) calc(var(--i, 0) * 18ms), transform 220ms var(--out) calc(var(--i, 0) * 18ms), background-color 150ms ease; }
  .menu > a, .menu .hn-langs > a { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 8px 10px; border-radius: 3px; color: var(--ink); font-size: 20px; }
  .menu .hn-langs > a { transition: background-color 150ms ease; }
  .menu > a:hover, .menu .hn-langs > a:hover { text-decoration: none; }
  .menu > a .ico { color: var(--ink-2); }
  .menu > p:not(.hn-mhead) { margin: 6px 0 0; padding: 9px 10px 5px; border-top: 1px solid var(--line); color: var(--ink-2); font-size: 20px; line-height: 32px; }
  select.lang { width: auto; font: inherit; font-size: 20px; padding: 5px 8px; border-radius: 3px; border: 1px solid var(--line); background: var(--panel); color: var(--ink-2); cursor: pointer; transition: color var(--fast) ease, border-color var(--fast) ease; }
  select.lang:hover { color: var(--ink); border-color: var(--leaf-deep); }
  select.pick { width: auto; font: inherit; font-size: 20px; padding: 5px 8px; border-radius: 3px; border: 1px solid var(--line); background: var(--panel); color: var(--ink); cursor: pointer; transition: border-color var(--fast) ease; }
  select.pick:hover { border-color: var(--leaf-deep); }
  :lang(zh-Hant) body { font-family: WcyPx, "Microsoft JhengHei UI", "PingFang TC", "Noto Sans TC", sans-serif; }
  :lang(zh-Hans) body { font-family: WcyPx, "Microsoft YaHei UI", "PingFang SC", "Noto Sans SC", sans-serif; }
  :lang(ja) body { font-family: WcyPx, "Yu Gothic UI", "Hiragino Sans", "Noto Sans JP", sans-serif; }
  :lang(ko) body { font-family: WcyPx, "Malgun Gothic", "Apple SD Gothic Neo", "Noto Sans KR", sans-serif; }
  main { flex: 1; width: 100%; max-width: 1120px; margin: 0 auto; padding: 44px 20px 72px; }
  h1 { margin: 0; font-size: 30px; font-weight: 400; line-height: 28px; letter-spacing: 0; text-wrap: balance; }
  h2 { margin: 56px 0 14px; font-size: 30px; font-weight: 400; line-height: 28px; letter-spacing: 0; text-wrap: balance; }
  .lede { margin: 6px 0 0; color: var(--ink-2); max-width: 62ch; text-wrap: pretty; }
  h1 + .lede { margin-top: 10px; }
  .lede:lang(zh), .lede:lang(ja), .lede:lang(ko) { max-width: 40em; }
  .policy { max-width: 72ch; line-height: 32px; }
  .policy-date { margin: 8px 0 28px; color: var(--ink-2); font-size: 20px; }
  .policy h2 { margin: 36px 0 10px; font-size: 20px; }
  .policy p, .policy li { text-wrap: pretty; }
  .policy ul { margin: 8px 0; padding-left: 1.3em; display: grid; gap: 4px; }
  .policy b { color: var(--ink); font-weight: 400; }
  .panel { padding: 16px 18px; border: 1px solid var(--line); border-radius: 6px; background: var(--panel); }
  .tr-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 14px; margin: 20px 0 6px; }
  .tr-bar label { margin: 0; }
  .tr-role a { color: var(--leaf); }
  .tr-tabs { display: flex; flex-wrap: wrap; gap: 4px; margin: 16px 0; border-bottom: 1px solid var(--line); }
  .tr-tabs button { font: inherit; font-size: 20px; color: var(--ink-2); background: none; border: 0; border-bottom: 2px solid transparent; padding: 8px 12px; cursor: pointer; transition: color var(--fast) ease; }
  .tr-tabs button[aria-selected="true"] { color: var(--leaf); border-bottom-color: var(--leaf); }
  .tr-tabs button:focus-visible { outline: 2px solid var(--leaf); outline-offset: 2px; }
  .tr-count { display: inline-block; min-width: 1.7em; margin-left: 6px; padding: 0 6px; border-radius: 3px; background: #d8b7651f; color: var(--gold); font-size: 10px; line-height: 14px; text-align: center; vertical-align: 1px; font-variant-numeric: tabular-nums; }
  .tr-count:empty { display: none; }
  .tr-search { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 8px; }
  .tr-search input[type="search"] { flex: 1 1 280px; min-width: 0; font: inherit; padding: 8px 10px; border-radius: 3px; color: var(--ink); border: 1px solid var(--line); background: #0f1512; }
  .tr-check { display: inline-flex; align-items: center; gap: 6px; margin: 0; color: var(--ink-2); font-size: 20px; }
  .tr-list { display: grid; gap: 10px; }
  .tr-row { padding: 12px 14px; border: 1px solid var(--line); border-radius: 6px; background: var(--panel); min-width: 0; }
  .tr-head { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 8px; margin-bottom: 6px; }
  .tr-head a { color: var(--leaf); font-size: 20px; }
  .tr-chip { font-size: 10px; padding: 1px 8px; border-radius: 3px; background: var(--panel-2); color: var(--ink-2); overflow-wrap: anywhere; }
  .tr-chip.leaf { color: var(--leaf); background: #7fd08a1f; }
  .tr-chip.gold { color: var(--gold); background: #d8b7651f; }
  .tr-chip.bad { color: var(--danger); background: #e08a8a1f; }
  .tr-src { color: var(--ink-2); font-size: 20px; overflow-wrap: anywhere; }
  .tr-dst { color: var(--ink); margin-top: 4px; overflow-wrap: anywhere; }
  .tr-dst.empty { color: var(--ink-2); }
  .tr-actions { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 10px; }
  .tr-actions input[type="text"] { flex: 1 1 200px; min-width: 0; width: auto; }
  .tr-more { margin-top: 10px; display: grid; gap: 10px; }
  .tr-more:empty { display: none; }
  .tr-editor { display: grid; gap: 4px; }
  .bot-state { list-style: none; margin: 0 0 4px; padding: 0; display: grid; gap: 4px; font-size: 20px; overflow-wrap: anywhere; }
  .bot-state .ok { color: var(--leaf); }
  .bot-state .bad { color: var(--danger); }
  .gv { display: grid; gap: 8px; margin-top: 6px; padding: 10px 12px; border-radius: 3px; background: #0b100d; box-shadow: inset 0 0 0 1px var(--line); }
  .gv-head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; }
  .gv-title { font-size: 20px; color: var(--ink-2); letter-spacing: 0; }
  .gv-cols { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 12px; }
  .gv-col { margin: 0; display: grid; gap: 6px; align-content: start; min-width: 0; }
  .gv-col figcaption { font-size: 20px; color: var(--ink-2); }
  .gv-tip { position: relative; isolation: isolate; padding: 14px 14px 12px; color: #e8e8e8; font-size: 15px; line-height: 1.5; text-shadow: 1px 1px 0 #3a3a3a; overflow-x: auto; }
  .gv-tip::before { content: ""; position: absolute; inset: 0; z-index: -1; border: solid transparent; border-width: 12px 10px; border-image: var(--t-tip-plain) 12 10 fill / 12px 10px; image-rendering: pixelated; }
  .gv-line { white-space: pre-wrap; min-height: 1.5em; overflow-wrap: anywhere; }
  .gv-num { border-bottom: 1px dashed currentColor; font-variant-numeric: tabular-nums; }
  .gv-num sup { margin-left: 2px; font-size: 10px; line-height: 0; color: var(--leaf); text-shadow: none; }
  .gv-num.bad sup { color: var(--danger); }
  .gv-ph { text-decoration: underline dotted #ffffff70; text-underline-offset: 3px; }
  .gv-icon { display: inline-block; width: .8em; height: .8em; margin: 0 2px; vertical-align: -.05em; background: #777; box-shadow: inset -2px -2px 0 #4c4c4c; }
  .gv-note { margin: 0; font-size: 20px; }
  .gv-tools { margin-top: 2px; }
  .gv-tools .btn { font-family: Consolas, "Cascadia Mono", monospace; font-size: 14px; padding: 4px 9px; }
  .tr-text { width: 100%; font: inherit; padding: 8px 10px; border-radius: 3px; color: var(--ink); border: 1px solid var(--line); background: #0f1512; resize: vertical; }
  .tr-prop { display: grid; gap: 8px; padding-top: 10px; border-top: 1px solid var(--line); }
  .tr-item { padding: 8px 10px; border-radius: 3px; background: var(--panel-2); }
  .fb-form { max-width: 720px; margin-top: 20px; }
  .fb-form > label { margin-top: 16px; }
  .fb-form > label:first-child { margin-top: 0; }
  .fb-form .form { align-items: start; }
  .fb-form .hint { margin-top: 4px; }
  .fb-count { text-align: right; font-variant-numeric: tabular-nums; }
  .fb-count.bad { color: var(--danger); }
  .fb-gate { max-width: 720px; margin-top: 20px; }
  .fb-gate a, .fb-out a { color: var(--leaf); }
  .fb-as { margin: 0 0 14px; }
  .fb-notes { max-width: 720px; margin-top: 18px; display: grid; gap: 6px; }
  .fb-notes p { margin: 0; }
  .fb-body { margin: 6px 0; white-space: pre-wrap; overflow-wrap: anywhere; color: var(--ink); }
  .fb-meta { display: flex; flex-wrap: wrap; gap: 4px 14px; margin: 4px 0 0; color: var(--ink-2); font-size: 20px; }
  .fb-meta a { color: var(--leaf); }
  .fb-meta .bad { color: var(--danger); }
  .tr-item.won, .tr-opt.won { box-shadow: inset 0 0 0 1px var(--leaf-deep); }
  .tr-foot { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 6px; }
  .tr-votes { color: var(--gold); font-size: 20px; font-variant-numeric: tabular-nums; }
  .tr-term { margin: 2px 0 6px; font-size: 20px; font-weight: 400; overflow-wrap: anywhere; }
  .tr-opt { position: relative; display: flex; flex-wrap: wrap; gap: 8px; align-items: center; padding: 8px 10px; border-radius: 3px; background: var(--panel-2); margin-top: 6px; overflow: hidden; }
  .tr-opt > * { position: relative; }
  .tr-opt > .tr-bar-fill { position: absolute; inset: 0 auto 0 0; background: #7fd08a14; pointer-events: none; }
  .tr-opt-text { flex: 1 1 auto; font-weight: 400; overflow-wrap: anywhere; }
  .tr-newpoll { margin-bottom: 14px; display: grid; gap: 4px; }
  .tr-langs { display: flex; flex-wrap: wrap; gap: 6px 14px; margin: 12px 0 0; padding: 0; border: 0; }
  .tr-langs legend { font-size: 20px; color: var(--ink-2); margin-bottom: 6px; padding: 0; }
  .lt-state { margin: 12px 0 0; padding: 10px 12px; border: 1px solid var(--line); border-radius: 6px; background: var(--panel-2); font-size: 20px; }
  .lt-state p { margin: 0; }
  .lt-state p + p { margin-top: 4px; color: var(--ink-2); }
  .lt-state.warn { border-color: var(--gold); }
  .tr-langs[disabled] { opacity: .5; }
  .lt-table { min-width: 680px; }
  .lt-table .tr-chip { white-space: nowrap; }
  .tr-langs label { display: inline-flex; align-items: center; gap: 6px; margin: 0; color: var(--ink); font-size: 20px; }
  .note { color: var(--ink-2); font-size: 20px; }
  .note b { color: var(--gold); font-weight: 400; }
  .btn {
    display: inline-flex; align-items: center; gap: 8px; font: inherit; font-size: 20px; cursor: pointer;
    padding: 8px 16px; border-radius: 3px; border: 1px solid var(--line); background: var(--panel-2); color: var(--ink);
    transition: transform var(--fast) var(--out), background-color 160ms ease, border-color 160ms ease, color 160ms ease, opacity 160ms ease;
  }
  .btn:hover { text-decoration: none; }
  .btn:active { transform: scale(.97); }
  .btn.primary { background: var(--leaf); border-color: var(--leaf); color: #0c1a10; font-weight: 400; }
  .btn.danger { border-color: #5a2b2b; background: #2e1a1a; color: var(--danger); }
  .btn[disabled] { opacity: .6; cursor: progress; }
  .btn[disabled]:active { transform: none; }
  /* 懸停只給有滑鼠的裝置：觸控點一下會卡在懸停的樣子 */
  @media (hover: hover) and (pointer: fine) {
    .btn:hover { border-color: var(--leaf-deep); background-color: #223029; }
    input[type="file"]::file-selector-button:hover { border-color: var(--leaf-deep); background-color: #223029; }
    .tr-tabs button[aria-selected="false"]:hover { color: var(--ink); }
    .btn.primary:hover { background: #93dba0; border-color: #93dba0; }
    .btn.danger:hover { background: #3d2020; border-color: #5a2b2b; }
    tbody tr:hover td { background: #ffffff06; }
    .hn-icon:hover, .hn-avatar:hover { color: var(--ink); border-color: var(--leaf-deep); }
    .hn-heart:hover { color: var(--gold); background: var(--panel-2); border-color: transparent; }
    .hn-tip:hover::after { opacity: 1; transform: translate(-50%, 0); transition-delay: 300ms; }
    .hn-sheen:hover::after { transform: translateX(340%) skewX(-18deg); transition: transform 680ms var(--out); }
    .brand:hover .logo { transform: rotate(-8deg) scale(1.04); }
    .hn-mi:hover, .hn-plink:hover, .hn-plangs > a:hover { background: var(--panel-2); }
    .fcard:hover { border-color: var(--leaf-deep); background: #18221c; }
    .make > summary:hover { background: #1b2c21; }
    .chip:hover { border-color: var(--leaf-deep); color: var(--ink); }
    .chip[aria-pressed="true"]:hover { color: #0c1a10; }
    .hero-screen .screen:hover { border-color: var(--leaf-deep); transform: translateY(-2px); }
    .menu > a:hover, .menu .hn-langs > a:hover { background: var(--panel-2); }
    .foot-cols a:hover, .copy:hover { color: var(--ink); text-decoration: none; }
    .more a:hover .ico { transform: translateX(3px); }
    .seg button:hover { color: var(--ink); }
    .spec-nav a:hover { color: var(--ink); text-decoration: none; }
    a.who-row:hover, .guild:hover { border-color: var(--leaf-deep); background: var(--panel-2); text-decoration: none; }
    a.who-row:hover .ico, .guild:hover .ico { color: var(--leaf); transform: translate(2px, -2px); }
    .switch:hover { border-color: var(--leaf-deep); }
  }
  label { display: block; font-size: 20px; color: var(--ink-2); margin: 0 0 4px; }
  input[type="text"], select {
    width: 100%; font: inherit; padding: 8px 10px; border-radius: 3px; color: var(--ink);
    border: 1px solid var(--line); background: #0f1512; transition: border-color var(--fast) ease;
  }
  input[type="text"], input[type="search"], textarea { transition: border-color var(--fast) ease, box-shadow var(--fast) ease; }
  input[type="text"]:focus, input[type="search"]:focus, textarea:focus { border-color: var(--leaf); outline: 2px solid transparent; outline-offset: 0; box-shadow: 0 0 0 3px #7fd08a2e; }
  :where(.policy, .lede, .note, .hint, .notice, .steps, .plain, .fb-gate, .panel > p) a:not(.btn) {
    text-decoration: underline; text-decoration-color: #7fd08a59; text-decoration-thickness: 1px; text-underline-offset: 3px;
    transition: text-decoration-color var(--fast) ease;
  }
  :where(.policy, .lede, .note, .hint, .notice, .steps, .plain, .fb-gate, .panel > p) a:not(.btn):hover { text-decoration-color: currentColor; }
  input[type="file"] { font: inherit; font-size: 20px; color: var(--ink-2); max-width: 100%; }
  input[type="file"]::file-selector-button {
    font: inherit; font-size: 20px; cursor: pointer; margin-right: 10px; padding: 7px 14px; border-radius: 3px;
    border: 1px solid var(--line); background: var(--panel-2); color: var(--ink);
    transition: border-color 160ms ease, background-color 160ms ease;
  }
  td { transition: background-color var(--fast) ease; }
  .msg { margin: 10px 0 0; font-size: 20px; min-height: 1.4em; }
  .msg.ok { color: var(--leaf); } .msg.bad { color: var(--danger); }
  table { width: 100%; border-collapse: collapse; font-size: 20px; }
  th { text-align: left; font-weight: 400; color: var(--ink-2); font-size: 20px; padding: 8px 10px; border-bottom: 1px solid var(--line); }
  td { padding: 9px 10px; border-bottom: 1px solid var(--line); vertical-align: middle; font-variant-numeric: tabular-nums; }
  td img { width: 22px; height: 22px; border-radius: 4px; vertical-align: middle; margin-right: 8px; image-rendering: pixelated; }
  .scroll { overflow-x: auto; }
  code { font-family: Consolas, "Cascadia Mono", monospace; font-size: 14px; color: var(--ink-2); }
  footer { border-top: 1px solid var(--line); color: var(--ink-2); font-size: 20px; background: #0c110e; }
  footer .in { max-width: 1120px; margin: 0 auto; padding: 40px 20px 26px; }
  .foot { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(0, 3fr); gap: 32px 56px; }
  .foot-brand p { margin: 10px 0 0; max-width: 34ch; text-wrap: pretty; }
  .foot-cols { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 26px 20px; }
  .foot-cols h2 { margin: 0 0 10px; font-size: 20px; font-weight: 400; color: var(--ink); }
  .foot-cols ul { margin: 0; padding: 0; list-style: none; display: grid; gap: 7px; }
  .foot-cols a { color: var(--ink-2); transition: color var(--fast) ease; }
  .copy { font: inherit; color: var(--ink-2); background: none; border: 0; padding: 0; cursor: pointer; text-align: left; transition: color var(--fast) ease; }
  .legal { display: flex; flex-wrap: wrap; gap: 4px 24px; justify-content: space-between; margin-top: 34px; padding-top: 18px; border-top: 1px solid var(--line); font-size: 10px; }
  [hidden] { display: none !important; }

  .hero { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.04fr); gap: 48px; align-items: center; padding: 14px 0 8px; }
  .hero h1 { font-size: clamp(34px, 4.3vw, 50px); line-height: 28px; letter-spacing: 0; text-wrap: balance; }
  .hero .lede { font-size: 20px; }
  .hero h1 + .lede { margin-top: 16px; }
  .hero-screen { min-width: 0; animation: settle 720ms var(--arrive) 80ms both; }
  .hero-screen .screen { display: block; aspect-ratio: 480 / 470; border: 1px solid var(--line); border-radius: 6px; box-shadow: 0 34px 70px -24px #000000e6, 0 0 0 6px #ffffff07; transition: border-color var(--base) ease, transform var(--base) var(--out); }
  .hero-cap { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: 4px 16px; margin: 12px 4px 0; font-size: 20px; color: var(--ink-2); }
  .hero-cap a { display: inline-flex; align-items: center; gap: 6px; white-space: nowrap; }
  .hero-cap svg { width: 14px; height: 14px; }
  .hero > div > * { animation: rise 520ms var(--arrive) both; }
  .hero > div > :nth-child(2) { animation-delay: 60ms; }
  .hero > div > :nth-child(3) { animation-delay: 120ms; }
  .hero > div > :nth-child(4) { animation-delay: 180ms; }
  @keyframes rise { from { opacity: 0; transform: translateY(10px); } }
  @keyframes settle { from { opacity: 0; transform: scale(.94) rotate(-4deg); } }
  @keyframes fade { from { opacity: 0; } }
  .cta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 22px; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 18px; }
  .chip { padding: 3px 11px; border: 1px solid var(--line); border-radius: 3px; font: inherit; font-size: 20px; color: var(--ink-2); background: var(--panel); cursor: pointer; transition: color var(--fast) ease, border-color var(--fast) ease, background-color var(--fast) ease, transform var(--fast) var(--out); }
  .chip:active { transform: scale(.96); }
  .chip[aria-pressed="true"] { color: #0c1a10; background: var(--leaf); border-color: var(--leaf); }
  .features { display: grid; gap: 0 32px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .features > div { padding: 16px 0 20px; border-top: 1px solid var(--line); }
  .features h3 { margin: 0 0 4px; font-size: 20px; font-weight: 400; }
  .features p { margin: 0; color: var(--ink-2); font-size: 20px; max-width: 38ch; }
  .more { margin: 14px 0 0; font-size: 20px; }
  .more a { display: inline-flex; align-items: center; gap: 6px; }
  .more .ico { transition: transform var(--fast) var(--out); }
  .team { margin-top: 20px; border-bottom: 1px solid var(--line); }
  .band { display: grid; grid-template-columns: 190px minmax(0, 1fr); gap: 14px 28px; padding: 22px 0; border-top: 1px solid var(--line); }
  .band-head { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 2px 10px; align-content: start; }
  .band-head > span { grid-row: span 2; margin-top: 1px; display: flex; }
  .band-head svg { width: 24px; height: 24px; }
  .band-head h3 { margin: 0; font-size: 20px; font-weight: 400; }
  .band-head p { margin: 0; grid-column: 2; color: var(--ink-2); font-size: 20px; line-height: 32px; }
  .lineup { margin: 0; padding: 0; list-style: none; display: grid; grid-template-columns: repeat(auto-fill, minmax(236px, 1fr)); gap: 8px 12px; align-content: start; min-height: 58px; }
  .fig { min-width: 0; animation: fade 320ms ease both; animation-delay: calc(var(--i, 0) * 25ms); }
  .who-row, .guild {
    display: flex; align-items: center; gap: 12px; min-width: 0; border: 1px solid var(--line); background: var(--panel); color: var(--ink);
    transition: border-color var(--fast) ease, background-color var(--fast) ease, transform var(--fast) var(--out);
  }
  .who-row { padding: 8px 12px; border-radius: 3px; }
  a.who-row:active, .guild:active { transform: scale(.985); }
  .who-row img, .who-row .blank { width: 40px; height: 40px; flex: none; border-radius: 6px; background: var(--panel-2); }
  .who-row img { image-rendering: pixelated; opacity: 0; transition: opacity 240ms ease; }
  .who-row img.in, .guild img.in { opacity: 1; }
  .who-row .blank { display: grid; place-items: center; color: var(--line); }
  .who-row .blank svg { width: 22px; height: 22px; }
  .who-row b { min-width: 0; font-size: 20px; font-weight: 400; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .who-row .mc { min-width: 0; margin-left: auto; padding-left: 6px; font-size: 20px; color: var(--ink-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .who-row > svg { width: 18px; height: 18px; flex: none; }
  .who-row b + svg { margin-left: auto; }
  .who-txt { display: flex; flex-direction: column; flex: 1; min-width: 0; line-height: 28px; }
  .who-top { display: flex; align-items: baseline; gap: 6px; min-width: 0; }
  .who-top > b, .who-row > b { flex: 0 0 auto; max-width: 100%; }
  .who-top > .mc, .who-row > .mc { flex: 1 1 0; text-align: right; }
  .who-tags { min-width: 0; font-size: 20px; color: var(--ink-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .who-tags .tl { color: var(--leaf); }
  .team-pick { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
  .lineup.guilds { grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); }
  .guild { padding: 10px 14px 10px 12px; border-radius: 6px; }
  .guild img { opacity: 0; transition: opacity 240ms ease; }
  .g-banner { width: 28px; height: 56px; flex: none; object-fit: contain; image-rendering: pixelated; filter: drop-shadow(0 4px 8px #0009); }
  .g-icon { width: 56px; height: 56px; flex: none; border-radius: 12px; object-fit: contain; background: #f4f8f6; }
  span.g-icon { display: grid; place-items: center; background: var(--panel-2); color: var(--ink-2); font-size: 20px; font-weight: 400; letter-spacing: 0; }
  .g-name { display: flex; align-items: baseline; gap: 7px; min-width: 0; flex: 1; }
  .g-name b { min-width: 0; font-size: 20px; font-weight: 400; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .g-name span { flex: none; font-size: 20px; font-weight: 400; color: var(--leaf); font-variant-numeric: tabular-nums; }
  .guild .ico, .who-row .ico { color: var(--ink-2); transition: color var(--fast) ease, transform var(--fast) var(--out); }
  .everyone { display: flex; align-items: center; gap: 10px; margin: 16px 0 0; color: var(--ink-2); font-size: 20px; }
  .everyone svg { width: 20px; height: 20px; }
  .everyone b { margin-right: .7em; color: var(--ink); font-weight: 400; }

  .play { margin-top: 26px; border: 1px solid var(--line); border-radius: 6px; background: var(--panel); overflow: hidden; }
  .play-bar { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 10px 18px; padding: 12px 14px; border-bottom: 1px solid var(--line); }
  .play-lang { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 20px; color: var(--ink-2); }
  .seg { position: relative; display: inline-flex; flex-wrap: wrap; gap: 2px; padding: 3px; border: 1px solid var(--line); border-radius: 3px; background: var(--ground); }
  .seg .pill { position: absolute; left: 0; top: 0; border-radius: 3px; background: var(--panel-2); box-shadow: inset 0 0 0 1px var(--line); pointer-events: none; transition: transform 220ms var(--out), width 220ms var(--out), height 220ms var(--out), opacity var(--fast) ease; }
  .seg button { position: relative; }
  .seg.live button[aria-pressed="true"] { background: none; box-shadow: none; }
  .seg button { font: inherit; font-size: 20px; padding: 5px 12px; border: 0; border-radius: 3px; background: none; color: var(--ink-2); cursor: pointer; transition: color var(--fast) ease, background-color var(--fast) ease, transform var(--fast) var(--out); }
  .seg button:active { transform: scale(.97); }
  .seg button[aria-pressed="true"] { background: var(--panel-2); color: var(--ink); box-shadow: inset 0 0 0 1px var(--line); }
  .play-body { display: grid; }
  .play-view { border-bottom: 1px solid var(--line); background: #0a0f0c; }
  .play-set { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 32px; align-content: start; padding: 0 16px; overflow: hidden; }
  .set-row { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px 14px; min-height: 52px; margin-top: -1px; padding: 9px 0; border-top: 1px solid var(--line); font-size: 20px; transition: opacity var(--base) ease; }
  .set-row > span { font-weight: 400; }
  .set-row.dim { opacity: .45; }
  .switch { position: relative; width: 40px; height: 22px; flex: none; padding: 0; border: 1px solid var(--line); border-radius: 3px; background: var(--ground); cursor: pointer; transition: background-color var(--base) ease, border-color var(--base) ease; }
  .switch::after { content: ""; position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; border-radius: 50%; background: var(--ink-2); transition: transform var(--base) var(--out), background-color var(--base) ease; }
  .switch[aria-checked="true"] { background: var(--leaf-deep); border-color: var(--leaf-deep); }
  .switch[aria-checked="true"]::after { transform: translateX(18px); background: #fff; }
  .range { display: flex; align-items: center; gap: 10px; }
  .range input { width: 150px; }
  .range output { min-width: 6ch; text-align: right; font-variant-numeric: tabular-nums; color: var(--ink-2); }

  .screen { position: relative; display: block; width: 100%; overflow: hidden; background: #0a0f0c; aspect-ratio: 960 / 480; }
  .scr {
    position: absolute; left: 0; top: 0; transform-origin: 0 0; overflow: hidden; width: 960px; height: 480px;
    color: #fff; font-family: PxTC, ui-monospace, monospace; font-size: 20px; line-height: 1.2; font-weight: 400;
    text-shadow: .1em .1em 0 #000000b8; user-select: none; -webkit-font-smoothing: none;
  }
  .scr :lang(zh-Hans) { font-family: PxSC, PxTC, monospace; }
  .scr :lang(ja) { font-family: PxJA, PxTC, monospace; }
  .scr :lang(ko) { font-family: PxKO, PxTC, monospace; }
  .scr b, .scr kbd { font-weight: 400; font-family: inherit; }
  .scr img, .scr canvas, .scr .hudbar, .scr .inv, .scr .dlgbox, .scr .option, .scr .tip::before, .scr i, .scr u { image-rendering: pixelated; }
  .scr .bg { position: absolute; inset: 0; background: var(--t-bg-world) center / cover no-repeat; }
  .scr[data-tab="world"] .bg { background-image: var(--t-bg-street); }
  .scr .hud { position: absolute; inset: 0; }
  .scr .hud > * { animation: fade 180ms ease both; }
  .scr.still .hud > * { animation: none; }
  .scr .dim { position: absolute; inset: 0; background: #0a0a0a99; }
  .c-y { color: #ffff55; } .c-g { color: #55ff55; } .c-gr { color: #aaaaaa; } .c-go { color: #ffaa00; } .c-m { color: #ff55ff; }
  .scr .soft { color: #aaaaaa; } .scr .dimmer { color: #6a6a6a; } .scr .good { color: #acfac6; } .scr .worse { color: #faacac; }
  .tip { position: relative; isolation: isolate; padding: 1.2em 1.2em 1.1em; white-space: nowrap; }
  .tip::before { content: ""; position: absolute; inset: 0; z-index: -1; border: solid transparent; border-width: 1.2em 1em; border-image: var(--t-tip-plain) 12 10 fill / 1.2em 1em; }
  .tip.f-rare::before { border-image-source: var(--t-tip-rare); }
  .tip.f-normal::before { border-image-source: var(--t-tip-normal); }
  .tip.f-ingredient::before { border-image-source: var(--t-tip-ingredient); }
  .duo .tip { width: max-content; min-width: 15em; max-width: 19.5em; font-size: 16px; }
  .tip-head { display: flex; align-items: center; gap: .5em; margin-bottom: .2em; }
  .tip .emblem { width: 3.2em; height: 3.2em; flex: none; }
  .tip .tname { color: #ff55ff; white-space: normal; }
  .tip.t-normal .tname { color: #fff; } .tip.t-ingredient .tname { color: #20aa20; }
  .tip .pills { display: flex; gap: .2em; margin-top: .2em; }
  .tip .pill { padding: 0 .35em; font-size: .6em; line-height: 1.5; letter-spacing: .06em; color: #1b1420; background: #e0e0e0; text-shadow: none; }
  .tip .pill.p-rare { background: #ff55ff; } .tip .pill.p-normal { background: #fff; }
  .tip .pill.p-stars { background: #555; color: #2e2e2e; letter-spacing: .12em; } .tip .pill.p-ingredient { background: #7dc97d; color: #12240f; }
  .tip .ico { width: 1.6em; height: 1.6em; margin: -.4em .1em -.4em -.25em; vertical-align: middle; }
  .tip .lead { margin-top: .15em; }
  .tip .lead b { margin-right: .12em; font-size: 1.5em; line-height: 1; }
  .tip.t-normal .lead b { color: #e0e0e0; } .tip.t-ingredient .lead b { color: #7dc97d; }
  .tip .jobs { display: flex; flex-wrap: wrap; gap: 0 .9em; margin-bottom: .35em; white-space: nowrap; }
  .tip .jobs .ico { margin-left: 0; }
  .tip .pager { display: flex; justify-content: center; align-items: center; gap: .55em; margin-top: .5em; }
  .tip .dot { width: .5em; height: .5em; background: #455449; clip-path: polygon(25% 0, 75% 0, 75% 25%, 100% 25%, 100% 75%, 75% 75%, 75% 100%, 25% 100%, 25% 75%, 0 75%, 0 25%, 25% 25%); }
  .tip .dot.on { background: #ffea80; }
  .tip kbd { margin-left: .3em; padding: 0 .35em; border-radius: 2px; font-size: .6em; line-height: 1.5; background: #ece6dd; color: #3a2a22; box-shadow: 0 2px 0 #9a8f84; text-shadow: none; }
  .tip .dmg .ico:not(:first-child) { margin-left: .5em; }
  .tip .divider { display: block; width: 11.2em; height: 1.6em; margin: -.08em auto; background: var(--t-divider) center / 100% 100% no-repeat; }
  .tip .reqs { display: flex; justify-content: space-between; margin: 0 .2em .2em; }
  .tip .req { display: grid; grid-template-columns: auto auto; justify-content: center; align-items: center; gap: 0 .2em; color: #a8a8a8; }
  .tip .req.on { color: #fff; }
  .tip .req i { grid-column: 1 / -1; width: 2.4em; height: 2.4em; background: var(--t-req) 0 0 / 12em 4.8em no-repeat; }
  .tip u { display: inline-block; width: 1em; height: 1em; margin-right: .3em; vertical-align: -.12em; text-decoration: none; background: var(--t-mark) 0 0 / 3em 1em no-repeat; }
  .tip u.ok { background-position: -1em 0; } .tip u.no { background-position: -2em 0; }
  .tip .req u { margin: 0; }
  .tip .kv { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 0 .8em; }
  .tip .kv span:last-child { margin-left: auto; text-align: right; }
  .inv { position: absolute; left: 5px; top: 56px; width: 374px; height: 350px; background: var(--t-inventory) 0 0 / 100% 100% no-repeat; }
  .scr .it { position: absolute; width: 32px; height: 32px; }
  .scr .it.smooth { image-rendering: auto; }
  .inv .hover { position: absolute; width: 32px; height: 32px; background: #ffffff59; pointer-events: none; }
  .inv .ghost { position: absolute; width: 32px; height: 32px; background: var(--t-slots) 0 0 / 224px 32px no-repeat; }
  .inv .slot { position: absolute; width: 32px; height: 32px; padding: 0; border: 0; background: none; cursor: pointer; font: inherit; color: inherit; text-shadow: inherit; }
  .inv .slot img { display: block; width: 32px; height: 32px; }
  .inv .slot img.smooth { image-rendering: auto; }
  .inv .slot:focus-visible { outline: 2px solid #fff; outline-offset: 0; }
  .inv .qty { position: absolute; right: -2px; bottom: -5px; font-size: 16px; line-height: 1; }
  .inv:not(.used) .slot[aria-pressed="false"]::after { content: ""; position: absolute; inset: 0; background: #fff; opacity: 0; animation: beckon 2.6s ease-in-out infinite; }
  .inv:not(.used) .slot[aria-pressed="false"] ~ .slot[aria-pressed="false"]::after { animation-delay: 1.3s; }
  @keyframes beckon { 0%, 55%, 100% { opacity: 0; } 28% { opacity: .3; } }
  .dollclip { position: absolute; left: 66px; top: 32px; width: 98px; height: 140px; overflow: hidden; pointer-events: none; }
  .dollbox { position: absolute; left: 21px; top: 16px; width: 56px; height: 112px; }
  .doll, .doll div { position: absolute; transform-style: preserve-3d; }
  .doll { inset: 0; }
  .doll .over { inset: 0; }
  .doll i { position: absolute; left: 50%; top: 50%; background-image: var(--t-skin); background-repeat: no-repeat; }
  .scr .cursor { position: absolute; width: 16px; height: 22px; filter: drop-shadow(1px 1px 0 #000a); }
  .duo { position: absolute; right: 5px; top: 34px; display: flex; align-items: flex-start; width: max-content; }
  .duo > * { flex: none; }
  .gapmark { position: relative; align-self: stretch; font-size: 16px; }
  .gapmark::before { content: ""; position: absolute; left: calc(-1 * var(--frame-l, 0px)); right: calc(-1 * var(--frame-r, 0px)); top: -12px; height: 8px; border: solid var(--leaf); border-width: 0 2px; background: linear-gradient(var(--leaf), var(--leaf)) center / 100% 2px no-repeat; }
  .gapmark span { position: absolute; left: 50%; top: -30px; transform: translateX(-50%); color: var(--leaf); font-size: 14px; line-height: 1; white-space: nowrap; }
  .hudbar { position: absolute; left: 50%; bottom: -6px; width: 464px; height: 110px; transform: translateX(-50%); background: var(--t-hud) 0 0 / 100% 100% no-repeat; }
  .hudbar .num { position: absolute; top: 14px; width: 144px; font-size: 16px; line-height: 1; text-align: center; white-space: nowrap; }
  .hudbar .hp { left: 30px; } .hudbar .mp { left: 288px; }
  .hudbar .lv { left: 208px; top: 25px; width: 48px; color: #fff; text-shadow: 2px 0 #3a2414, -2px 0 #3a2414, 0 2px #3a2414, 0 -2px #3a2414, 2px 2px #3a2414, -2px 2px #3a2414, 2px -2px #3a2414, -2px -2px #3a2414; }
  .held { position: absolute; left: 0; right: 0; bottom: 112px; text-align: center; white-space: nowrap; color: #ff55ff; }
  .boss { position: absolute; top: 10px; left: 50%; transform: translateX(-50%); text-align: center; white-space: nowrap; color: #ff5555; }
  .scr .mlv { display: inline-block; margin-left: .45em; padding: 0 .5em; border-radius: 4px; font-size: .7em; line-height: 1.45; letter-spacing: .1em; vertical-align: .14em; color: #fff; background: #555; box-shadow: 0 2px 0 #2c2c2c; text-shadow: none; }
  .boss i { display: block; width: 364px; height: 10px; margin: 4px auto 0; background: linear-gradient(90deg, #e0358f 62%, #4a1232 62%); box-shadow: 0 0 0 2px #000000b0; }
  .trk { position: absolute; top: 58px; right: 10px; display: grid; justify-items: end; gap: 8px; max-width: 46%; text-align: right; white-space: nowrap; }
  .trk .tip { text-align: left; }
  .board { position: absolute; right: 0; top: 172px; min-width: 210px; padding-bottom: 2px; background: #0000004d; color: #aaaaaa; white-space: nowrap; }
  .board.below { top: 206px; }
  .dlg-extra .tip, .trk .tip, .npc .look { padding: .8em 1em .7em; }
  .board div { padding: 0 12px 0 8px; }
  .board div:first-child { background: #00000066; color: #ffaa00; text-align: center; }
  .npc { position: absolute; left: 468px; top: 198px; width: 0; height: 0; }
  .npc .plate { position: absolute; left: 0; bottom: 0; display: grid; justify-items: center; width: max-content; transform: translateX(-50%); text-align: center; white-space: nowrap; }
  .npc .plate b { color: #ff55ff; }
  .npc .plate span { color: #aaaaaa; }
  .npc .look { position: absolute; right: 46px; top: 8px; width: max-content; max-width: 320px; white-space: normal; }
  .title { position: absolute; left: 0; right: 0; top: 62px; text-align: center; color: #ffaa00; font-size: 2em; text-shadow: .1em .1em 0 #3f2a00; pointer-events: none; }
  .chat { position: absolute; left: 6px; bottom: 120px; max-width: 420px; }
  .chat div { padding: 0 6px; background: #00000080; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .chat .ours { color: #9be7a5; }
  .dlg { position: absolute; left: 50%; bottom: 96px; display: flex; flex-direction: column; gap: 6px; width: 574px; transform: translateX(-50%); }
  .dlg-extra { display: flex; justify-content: space-between; align-items: flex-end; gap: 10px; padding-left: 128px; }
  .dlg-extra .tip { white-space: normal; }
  .dlg-extra .right { margin-left: auto; }
  .options { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; margin-bottom: -2px; }
  .option { position: relative; width: 288px; height: 32px; padding: 0 16px; line-height: 22px; white-space: nowrap; background: var(--t-choice) 0 0 / 288px 64px no-repeat; }
  .option.on { background-position: 0 -32px; }
  .option .pick { position: absolute; left: -30px; top: -4px; width: 32px; height: 32px; }
  .dlgbox { position: relative; height: 212px; background: var(--t-dialogue) 0 0 / 100% 100% no-repeat; }
  .dlgbox b { position: absolute; left: 128px; top: 6px; width: 224px; line-height: 28px; text-align: center; white-space: nowrap; }
  .dlgbox p { position: absolute; left: 142px; top: 48px; width: 404px; margin: 0; line-height: 24px; }
  .dlgbox .hint2 { position: absolute; left: 94px; top: 174px; display: flex; justify-content: center; align-items: center; gap: 8px; width: 448px; height: 36px; font-size: 16px; }
  .dlgbox kbd { padding: 0 6px; border-radius: 3px; background: #ece6dd; color: #3a2a22; line-height: 18px; box-shadow: 0 2px 0 #9a8f84; text-shadow: none; }
  .scr.narrow { height: 470px; }
  .scr.narrow .tip { font-size: 14px; }
  .scr.narrow .duo .tip, .scr.narrow .gapmark { font-size: 12px; }
  .scr.narrow .inv { left: 143px; top: 2px; transform: scale(.52); transform-origin: 0 0; }
  .scr.narrow .duo { left: 0; right: 0; top: 178px; width: auto; justify-content: center; }
  .scr.narrow .gapmark span { padding: 2px 5px; background: #000000b0; }
  .scr.narrow .boss i { width: 250px; }
  .scr.narrow .trk { top: 50px; right: 6px; max-width: none; }
  .scr.narrow .trk { grid-auto-flow: column; align-items: start; }
  .scr.narrow .trk .tip { padding: .6em .8em .5em; }
  .scr.narrow[data-tab="world"] .bg { background-position: -310px 0; background-size: auto 100%; }
  .scr.narrow .board { top: 138px; min-width: 0; font-size: 14px; }
  .scr.narrow .board.below { top: 138px; }
  .scr.narrow .npc { left: 150px; top: 194px; }
  .scr.narrow .npc .plate { max-width: 236px; font-size: 16px; white-space: normal; }
  .scr.narrow .npc .look { left: 40px; right: auto; top: 14px; max-width: 250px; }
  .scr.narrow .title { top: 96px; font-size: 1.7em; }
  .scr.narrow .chat { bottom: 112px; max-width: 300px; }
  .scr.narrow .held { display: none; }
  .scr.narrow .dlg { bottom: 92px; transform: translateX(-50%) scale(.8); transform-origin: 50% 100%; }
  .demo-foot { display: flex; flex-wrap: wrap; gap: 4px 16px; align-items: baseline; margin: 0; padding: 12px 14px 13px; border-top: 1px solid var(--line); font-size: 20px; min-height: 3.2em; }
  .demo-foot .hint { flex-basis: 100%; }
  .wearbox { display: grid; gap: 8px; }
  .spec { display: grid; grid-template-columns: 170px minmax(0, 1fr); gap: 8px 44px; align-items: start; margin-top: 48px; }
  .spec-nav { position: sticky; top: 78px; display: grid; gap: 2px; font-size: 20px; }
  .spec-nav a { padding: 5px 10px; border-radius: 3px; color: var(--ink-2); transition: color var(--fast) ease, background-color var(--fast) ease; }
  .spec-nav a[aria-current="true"] { color: var(--ink); background: var(--panel-2); }
  .spec section + section { margin-top: 48px; }
  .spec h2 { margin: 0 0 12px; }
  .spec .note { margin: 14px 0 0; max-width: 68ch; }
  .cards { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
  .cards.three { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .fcard {
    display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 14px; align-items: start; padding: 14px 16px 15px;
    border: 1px solid var(--line); border-radius: 6px; background: var(--panel);
    transition: border-color var(--base) ease, background-color var(--base) ease;
  }
  .fcard h3 { margin: 1px 0 3px; font-size: 20px; font-weight: 400; }
  .fcard p { margin: 0; color: var(--ink-2); font-size: 20px; text-wrap: pretty; }
  .fcard code { font-size: 20px; }
  .glyph { position: relative; display: block; width: 40px; height: 40px; border-radius: 3px; background: #0c120f; box-shadow: inset 0 0 0 1px var(--line); color: var(--leaf); }
  .rv { opacity: 0; transform: translateY(22px); }
  .rv.rv-on { opacity: 1; transform: none; transition: opacity 560ms var(--arrive), transform 700ms var(--arrive); transition-delay: var(--d, 0ms); }
  .fcard.rv .glyph { opacity: 0; transform: scale(.6) rotate(-12deg); }
  .fcard.rv.rv-on .glyph { opacity: 1; transform: none; transition: opacity 360ms var(--arrive), transform 620ms cubic-bezier(.34, 1.56, .64, 1); transition-delay: calc(var(--d, 0ms) + 220ms); }
  .glyph::after {
    content: ""; position: absolute; inset: 4px; background: currentColor; image-rendering: pixelated;
    -webkit-mask: var(--t-glyphs) calc(var(--c) * -32px) calc(var(--r) * -32px) / 512px 192px no-repeat;
    mask: var(--t-glyphs) calc(var(--c) * -32px) calc(var(--r) * -32px) / 512px 192px no-repeat;
  }
  .glyph.own { display: grid; place-items: center; }
  .glyph.own::after { content: none; }
  .glyph.own svg { width: 22px; height: 22px; }
  .eg { display: grid; margin-top: 10px !important; padding: 6px 10px 5px; border-radius: 7px; background: #0a0f0c; box-shadow: inset 0 0 0 1px #ffffff0a; font-family: PxTC, ui-monospace, monospace; font-size: 20px !important; line-height: 1.15; color: #8d9a91 !important; }
  .eg span { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .eg .tr { color: var(--leaf); }
  .eg :lang(zh-Hans) { font-family: PxSC, PxTC, monospace; }
  .eg :lang(ja) { font-family: PxJA, PxTC, monospace; }
  .eg :lang(ko) { font-family: PxKO, PxTC, monospace; }
  .fcard.tall { grid-template-columns: minmax(0, 1fr); gap: 12px; }
  .fcard .show { display: flex; align-items: center; gap: 10px; min-height: 56px; padding: 8px 12px; border-radius: 3px; background: #0c120f; box-shadow: inset 0 0 0 1px var(--line); }
  .fcard .show svg { width: 26px; height: 26px; }
  .capemini { width: 30px; aspect-ratio: 10 / 16; overflow: hidden; border-radius: 2px; box-shadow: 0 0 0 1px #0008; }
  .capemini img { display: block; width: 220%; max-width: none; margin: -10% 0 0 -10%; image-rendering: pixelated; }
  .fcard.mod h3 { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 8px; }
  .fcard.mod .state { margin-left: 0; }
  .tpl { display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr); gap: 22px; align-items: center; padding: 18px; border: 1px solid var(--leaf-deep); border-radius: 6px; background: linear-gradient(180deg, #17241c, var(--panel) 70%); }
  .tpl .guide { display: block; width: 100%; height: auto; border-radius: 3px; box-shadow: 0 0 0 1px var(--line); }
  .tpl h2 { margin: 0 0 6px; }
  .tpl p { margin: 0 0 12px; color: var(--ink-2); font-size: 20px; }
  .tpl .label { margin: 0 0 8px; color: var(--ink); font-size: 20px; font-weight: 400; }
  .tpl-sizes { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
  .tpl-sizes .btn { padding: 7px 12px; font-variant-numeric: tabular-nums; }
  .tpl-sizes small { font-size: 20px; opacity: .78; }
  .tpl .more-links { display: flex; flex-wrap: wrap; gap: 6px 18px; font-size: 20px; }
  .make { margin: 20px 0 4px; }
  .make > summary { display: flex; align-items: center; gap: 14px; padding: 12px 16px; border: 1px solid var(--leaf-deep); border-radius: 6px; background: #17241c; cursor: pointer; list-style: none; transition: background-color var(--fast) ease, transform var(--fast) var(--out); }
  .make > summary::-webkit-details-marker { display: none; }
  .make > summary:active { transform: scale(.995); }
  .make > summary img { width: 44px; height: 34px; flex: none; image-rendering: pixelated; border-radius: 3px; box-shadow: 0 0 0 1px #0008; }
  .make > summary b { display: block; font-size: 20px; font-weight: 400; }
  .make > summary .sub { display: block; color: var(--ink-2); font-size: 20px; }
  .make > summary .btn { margin-left: auto; flex: none; pointer-events: none; }
  .make > summary .btn svg { transition: transform var(--base) var(--out); }
  .make[open] > summary { border-radius: 6px 6px 0 0; border-bottom-color: transparent; }
  .make[open] > summary .btn svg { transform: rotate(180deg); }
  .make[open] > .tpl { border-top: 0; border-radius: 0 0 6px 6px; background: linear-gradient(180deg, #17241c, var(--panel) 60%); }
  .state { display: inline-block; margin-left: 6px; padding: 0 8px; border: 1px solid var(--line); border-radius: 3px; font-size: 10px; font-weight: 400; line-height: 14px; color: var(--ink-2); vertical-align: 1px; white-space: nowrap; }
  .state.need { color: var(--gold); border-color: #5c4d27; }
  .plain { margin: 10px 0 0; padding-left: 1.2em; color: var(--ink-2); }
  .plain li { margin: 4px 0; } .plain b { color: var(--ink); font-weight: 400; }
  .split { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 14px; }

  .bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 18px; margin: 22px 0 14px; }
  .count { font-variant-numeric: tabular-nums; color: var(--ink-2); }
  .count b { color: var(--ink); font-weight: 400; }
  .grid { display: grid; gap: 18px; grid-template-columns: repeat(auto-fill, minmax(168px, 1fr)); }
  .card {
    position: relative; display: flex; flex-direction: column;
    background: var(--panel); border: 1px solid var(--line); border-radius: 6px; overflow: hidden;
    transition: transform 180ms var(--out), border-color 180ms ease, box-shadow 180ms ease;
    animation: rise 360ms var(--arrive) both; animation-delay: calc(var(--i, 0) * 35ms);
  }
  .card .open { display: flex; flex-direction: column; width: 100%; padding: 0; border: 0; background: none; color: inherit; font: inherit; text-align: left; cursor: pointer; }
  .card .open:focus-visible { outline-offset: -3px; }
  .card:has(.open:active) { transform: scale(.98); }
  .star {
    position: absolute; top: 8px; right: 8px; display: grid; place-items: center; width: 32px; height: 32px; padding: 0;
    border: 0; border-radius: 3px; background: #0b0f0d8c; color: var(--ink-2); cursor: pointer;
    transition: color var(--fast) ease, background-color var(--fast) ease, transform var(--fast) var(--out);
  }
  .star svg { width: 22px; height: 22px; }
  .star[aria-pressed="true"] { color: var(--gold); }
  .star[aria-pressed="true"] { --pxf: currentColor; }
  .star:active { transform: scale(.88); }
  .meta .stars { display: inline-flex; align-items: center; gap: 3px; color: var(--gold); font-variant-numeric: tabular-nums; }
  .meta .stars svg { width: 11px; height: 11px; --pxf: currentColor; }
  .tag.off { background: none; color: var(--ink-2); box-shadow: inset 0 0 0 1px var(--line); }
  .tag.wait { background: #5c4d27; color: var(--gold); }
  .tag.mine { background: var(--leaf-deep); color: #fff; }
  .sortby { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 20px; color: var(--ink-2); }
  .filters { margin: 0 0 14px; }
  .set-line { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--line); }
  .set-line b { display: block; font-weight: 400; font-size: 20px; }
  .set-line .hint { margin-top: 2px; }
  @media (hover: hover) and (pointer: fine) {
    .card:hover { transform: translateY(-2px); border-color: var(--leaf-deep); box-shadow: 0 10px 24px -6px #000a; }
    .card:hover:has(.open:active) { transform: translateY(-1px) scale(.98); }
    .star:hover { color: var(--gold); background: #0b0f0dd9; }
  }
  .card.skel { cursor: default; animation: pulse 1.4s ease-in-out infinite; }
  .card.skel .stage { height: 190px; }
  .card.skel .meta { height: 43px; }
  @keyframes pulse { 50% { opacity: .55; } }
  .stage { position: relative; display: grid; place-items: center; padding: 20px 0 16px; background: linear-gradient(180deg, #1b2a21 0%, #131b16 100%); }
  .marks { position: absolute; top: 8px; left: 8px; right: 46px; display: flex; flex-wrap: wrap; gap: 4px; pointer-events: none; }
  .marks .tag { white-space: nowrap; box-shadow: 0 1px 5px #0009; }
  .marks .tag.off { background: #0b0f0dd9; color: var(--ink); }
  .face { width: 96px; aspect-ratio: 10 / 16; overflow: hidden; border-radius: 3px; box-shadow: 0 8px 18px #000a, 0 0 0 1px #0006; background: #0b0f0d; }
  .face img { display: block; width: 220%; max-width: none; margin: -10% 0 0 -10%; image-rendering: pixelated; opacity: 0; transition: opacity 240ms ease; }
  .face img.in { opacity: 1; }
  .meta { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 43px; padding: 10px 12px 12px; font-size: 20px; color: var(--ink-2); white-space: nowrap; }
  .tag { padding: 1px 7px; border-radius: 3px; font-size: 10px; font-weight: 400; letter-spacing: 0; color: var(--ground); background: var(--ink-2); }
  .tag.gif { background: var(--gold); }
  .meta .who { min-width: 0; overflow: hidden; text-overflow: ellipsis; font-variant-numeric: tabular-nums; }
  .meta .who.idle { color: var(--gold); }
  .meta .stars { flex: none; margin-left: auto; }
  .notice { margin: 0 0 16px; padding: 10px 14px; border: 1px solid #5c4d27; border-radius: 6px; background: #2a241266; color: var(--gold); font-size: 20px; }
  .grid > .empty { grid-column: 1 / -1; padding: 56px 16px; border: 1px dashed var(--line); border-radius: 6px; text-align: center; color: var(--ink-2); text-wrap: balance; }
  dialog {
    width: min(92vw, 780px); padding: 0; color: var(--ink); background: var(--panel); border: 1px solid var(--line); border-radius: 6px; box-shadow: 0 30px 80px #000c;
    opacity: 0; transform: scale(.96);
    transition: opacity 150ms ease, transform 150ms var(--out), overlay 150ms allow-discrete, display 150ms allow-discrete;
  }
  dialog[open] { opacity: 1; transform: none; transition-duration: 260ms; transition-timing-function: ease, var(--arrive), ease, ease; }
  @starting-style { dialog[open] { opacity: 0; transform: scale(.96); } }
  dialog::backdrop { background: #070a0800; transition: background-color 150ms ease, overlay 150ms allow-discrete, display 150ms allow-discrete; }
  dialog[open]::backdrop { background: #070a08cc; transition-duration: 260ms; }
  @starting-style { dialog[open]::backdrop { background: #070a0800; } }
  .view { display: grid; grid-template-columns: minmax(0, 1fr) 260px; background: linear-gradient(180deg, #1d2e24, #121915); }
  .view .side { background: var(--panel); }
  canvas.model { display: block; width: 100%; height: 440px; align-self: center; cursor: grab; }
  canvas.model:active { cursor: grabbing; }
  #skinpick { align-self: flex-start; }
  .side { padding: 18px; border-left: 1px solid var(--line); display: flex; flex-direction: column; gap: 12px; min-width: 0; }
  .side h2 { margin: 0; font-size: 20px; }
  .side dl { margin: 0; display: grid; grid-template-columns: auto 1fr; gap: 4px 12px; font-size: 20px; }
  .side dt { color: var(--ink-2); } .side dd { margin: 0; font-variant-numeric: tabular-nums; }
  .flat { width: 100%; image-rendering: pixelated; border-radius: 4px; background: #0b0f0d; }
  .owners { margin: 0; padding: 0; list-style: none; font-size: 20px; max-height: 120px; overflow: auto; }
  .owners li { display: flex; align-items: center; gap: 8px; padding: 3px 0; }
  .owners img { width: 18px; height: 18px; border-radius: 3px; image-rendering: pixelated; }
  .hint { color: var(--ink-2); font-size: 20px; margin: 0; }
  .spacer { flex: 1; }
  .account { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18px; align-items: start; }
  .who-am-i { display: flex; align-items: center; gap: 14px; }
  .who-am-i img { width: 48px; height: 48px; border-radius: 3px; image-rendering: pixelated; }
  .who-am-i b { font-size: 20px; }
  .steps { margin: 10px 0 0; padding-left: 1.3em; color: var(--ink-2); }
  .steps li { margin: 4px 0; } .steps b { color: var(--ink); font-weight: 400; }
  .linkcode { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin: 10px 0; }
  .linkcode code { font-size: 18px; color: var(--ink); background: #0f1512; border: 1px solid var(--line); border-radius: 3px; padding: 8px 14px; user-select: all; }
  .linkcode code.share-url { flex: 1 1 220px; min-width: 0; font-size: 13px; padding: 7px 10px; word-break: break-all; }
  .vis-line { flex-direction: column; align-items: stretch; gap: 8px; }
  .vis-line .seg { align-self: flex-start; }
  .wipe-panel { margin-top: 28px; border-color: #5a2b2b; }
  #wipe { width: min(92vw, 560px); }
  .wipe-box { padding: 22px 24px; }
  .wipe-box h2 { margin: 0 0 10px; font-size: 20px; }
  .wipe-box p, .wipe-box li { color: var(--ink-2); font-size: 20px; line-height: 32px; }
  .wipe-box ul { margin: 6px 0 12px; padding-left: 1.2em; }
  .wipe-box b { color: var(--ink); }
  .wipe-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; margin-top: 8px; }
  .form { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); align-items: end; }
  .finder { position: relative; }
  .finder input { padding-right: 32px; cursor: pointer; }
  .finder input:focus { cursor: text; }
  .finder::after { content: ''; position: absolute; right: 13px; bottom: 17px; width: 7px; height: 7px; border-right: 2px solid var(--ink-2); border-bottom: 2px solid var(--ink-2); transform: rotate(45deg); pointer-events: none; transition: transform var(--fast) var(--out); }
  .finder.open::after { transform: translateY(4px) rotate(-135deg); }
  .finder-hits { position: absolute; left: 0; right: 0; min-width: min(360px, calc(100vw - 32px)); top: calc(100% + 4px); z-index: 4; margin: 0; padding: 4px; list-style: none; max-height: 320px; overflow-y: auto; border: 1px solid var(--line); border-radius: 3px; background: var(--panel); box-shadow: 0 18px 44px #000a; }
  .finder-hits li { display: flex; align-items: center; gap: 10px; padding: 7px 8px; border-radius: 3px; cursor: pointer; transition: background-color var(--fast) ease; }
  .finder-hits li:hover, .finder-hits li[aria-selected="true"] { background: var(--panel-2); }
  .finder-hits li.taken, .finder-hits li.none { cursor: default; background: none; }
  .finder-hits li.taken { opacity: .55; }
  .finder-hits li.none { color: var(--ink-2); font-size: 20px; }
  .finder-hits li.count { cursor: default; background: none; padding: 4px 8px 6px; color: var(--ink-2); font-size: 20px; }
  .finder-hits img { width: 28px; height: 28px; border-radius: 5px; image-rendering: pixelated; flex: none; }
  .finder-hits div { min-width: 0; }
  .finder-hits b { display: block; font-weight: 400; color: var(--ink); }
  .finder-hits small { display: block; color: var(--ink-2); font-size: 20px; overflow-wrap: anywhere; }
  .drop { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 12px; padding: 14px; border: 1px dashed var(--line); border-radius: 6px; transition: border-color var(--fast) ease, background-color var(--fast) ease; }
  .drop.over { border-color: var(--leaf); background: #7fd08a12; }
  .drop .hint { flex-basis: 100%; }
  @media (max-width: 940px) {
    .foot { grid-template-columns: 1fr; }
    .features { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .cards.three { grid-template-columns: 1fr; }
    .top .in { gap: 12px; }
    .tools { gap: 6px; }
  }
  @media (max-width: 720px) {
    .hero, .account { grid-template-columns: 1fr; }
    .hero h1 { font-size: 30px; }
    .view { grid-template-columns: 1fr; }
    canvas.model { height: 320px; }
    .side { border-left: 0; border-top: 1px solid var(--line); }
    .top .in { padding: 10px 16px; }
    .hn, .hn-desk { display: none !important; }
    .hn-burger { display: inline-flex; }
    .hn-leaves { height: 55px; }
    .hn-panel {
      display: block; position: absolute; top: 100%; left: 0; right: 0; z-index: 4;
      max-height: calc(100vh - 56px); max-height: calc(100dvh - 56px); overflow-y: auto; overscroll-behavior: contain;
      border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); background: var(--ground); box-shadow: 0 28px 48px #000b;
      opacity: 0; visibility: hidden; transform: translateY(-10px); transform-origin: top center;
      transition: opacity 120ms var(--exit), transform 120ms var(--exit), visibility 0s linear 120ms;
    }
    .hn-panel.open { opacity: 1; visibility: visible; transform: none; transition: opacity 200ms var(--out), transform 260ms var(--out), visibility 0s; }
    .foot-cols { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .band { grid-template-columns: 1fr; }
    .lineup, .lineup.guilds { grid-template-columns: 1fr; }
    .features { grid-template-columns: 1fr; }
    .play-set { grid-template-columns: 1fr; }
    .range input { width: 120px; }
    .spec { grid-template-columns: 1fr; margin-top: 36px; }
    .spec-nav { position: static; display: flex; flex-wrap: wrap; gap: 4px; margin: 0 -10px 10px; }
    .cards, .cards.three { grid-template-columns: 1fr; }
    .tpl { grid-template-columns: 1fr; }
    .make > summary .sub { display: none; }
  }
  @media (max-width: 520px) {
    .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
    .grid .stage { padding: 16px 0 12px; }
    .grid .face { width: 84px; }
    .grid .meta { padding: 8px 10px 10px; }
    .grid .card.skel .stage { height: 162px; }
  }
  .scr, .eg { letter-spacing: 0; font-weight: 400; }
  .top { background: #0f1512d9; border-bottom-color: #ffffff0f; }
  .top .in, footer .in, .hn-pnav, .hn-psec { max-width: 1400px; }
  .top .in, footer .in { padding-left: var(--gutter); padding-right: var(--gutter); }
  .hn-link[aria-current="page"], .hn-link.hn-cur, .spec-nav a[aria-current="true"] { background: #7fd08a1a; box-shadow: inset 0 0 0 1px #7fd08a4d; color: var(--ink); }
  .hn-link, .hn-icon, .hn-avatar, .spec-nav a, .seg, .seg button, .seg .pill, select.pick, select.lang { border-radius: 3px; }
  .eg span { white-space: normal; overflow: visible; }
  #in > .panel { max-width: none !important; }
  .tr-tabs { flex-wrap: nowrap; overflow-x: auto; scrollbar-width: none; }
  .tr-tabs button { flex: none; white-space: nowrap; }
  .menu > a > span, .hn-plink > span { flex: 1; min-width: 0; }
  .menu > a > .ico:last-child, .hn-plink > .ico:last-child { width: 14px; height: 14px; color: var(--ink-2); }
  .btn > .ico { width: 17px; height: 17px; margin-left: -2px; }
  main { max-width: 1400px; padding: 64px var(--gutter) 120px; }
  h1 { font-size: 40px; line-height: 28px; letter-spacing: 0; }
  #main > h2 { margin: 112px 0 18px; font-size: 30px; line-height: 28px; letter-spacing: 0; }
  #main > h2 + .lede { margin-top: -4px; }
  .lede { color: #bccabf; }
  .hero { grid-template-columns: minmax(0, .8fr) minmax(0, 1.2fr); gap: 64px; padding: 8px 0 16px; }
  .hero h1 { font-size: clamp(40px, 4.8vw, 62px); line-height: 28px; letter-spacing: 0; font-weight: 400; word-break: keep-all; }
  .hero h1 + .lede { margin-top: 22px; font-size: 20px; line-height: 32px; }
  .hero-screen .screen { border-radius: 6px; border-color: #ffffff14; box-shadow: 0 40px 90px -30px #000, 0 0 0 1px #ffffff08; }
  .hero-cap { margin: 16px 0 0; }
  .chips { gap: 6px; margin-top: 26px; }
  .chip { background: transparent; border-color: #ffffff1c; }
  .cta { margin-top: 30px; gap: 12px; }
  .btn { border-radius: 3px; padding: 9px 18px; border-color: #ffffff17; font-weight: 400; }
  .btn.small { border-radius: 3px; padding: 5px 13px; }
  .btn.primary { box-shadow: 0 8px 22px -10px #7fd08acc, inset 0 1px 0 #ffffff59; }
  .btn.danger { border-color: #5a2b2b; }
  .tpl-sizes .btn { border-radius: 3px; }
  .features { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 64px; margin-top: 8px; }
  .features > div { padding: 24px 0 28px; border-top-color: #ffffff12; }
  .features .ico { display: block; width: 22px; height: 22px; margin-bottom: 14px; color: var(--leaf); }
  .features h3 { margin-bottom: 6px; font-size: 20px; }
  .features p { font-size: 20px; max-width: 46ch; }
  .split { gap: 48px; }
  .split > .panel, #main > h2 + .panel { padding: 22px 0 0; border: 0; border-top: 1px solid #ffffff14; border-radius: 0; background: none; box-shadow: none; }
  .panel { padding: 20px 22px; border-color: #ffffff10; border-radius: 6px; background: linear-gradient(180deg, #17201a, #131a16); box-shadow: inset 0 1px 0 #ffffff08; }
  .plain { padding-left: 1.1em; }
  .team { border-bottom-color: #ffffff10; }
  .band { grid-template-columns: 220px minmax(0, 1fr); gap: 16px 32px; padding: 30px 0; border-top-color: #ffffff10; }
  .lineup { grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 10px 14px; }
  .who-row, .guild { border-color: transparent; background: #ffffff05; border-radius: 6px; }
  .make > summary { border-color: #ffffff17; background: #ffffff05; border-radius: 6px; padding: 16px 20px; }
  .make[open] > summary { border-radius: 6px 6px 0 0; }
  .tpl { border-color: #ffffff17; border-radius: 6px; }
  .make[open] > .tpl { border-radius: 0 0 6px 6px; }
  .bar { margin: 40px 0 22px; }
  .grid { grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 28px 20px; }
  .card { background: none; border: 0; border-radius: 0; overflow: visible; }
  .card .stage { border-radius: 6px; padding: 28px 0 24px; background: radial-gradient(110% 90% at 50% 0%, #1f3328 0%, #131b16 75%); box-shadow: inset 0 0 0 1px #ffffff0f; transition: box-shadow 180ms ease; }
  .card .open:focus-visible { outline-offset: 2px; border-radius: 6px; }
  .card.skel .stage { height: 206px; }
  .meta { min-height: 0; padding: 12px 4px 0; }
  .card.skel .meta { height: 34px; }
  .star { top: 10px; right: 10px; }
  .marks { top: 10px; left: 10px; }
  .tr-row { border-color: #ffffff10; border-radius: 6px; background: #ffffff04; }
  .spec { grid-template-columns: 190px minmax(0, 1fr); gap: 8px 56px; margin-top: 72px; }
  .spec h2 { font-size: 30px; letter-spacing: 0; line-height: 28px; margin-bottom: 16px; }
  .spec section + section { margin-top: 88px; }
  .spec-nav { top: 84px; }
  .cards { gap: 14px; }
  .fcard { padding: 18px 20px; border-color: #ffffff10; border-radius: 6px; background: #ffffff04; }
  .fcard h3 { font-size: 20px; }
  .glyph { border-radius: 6px; background: #7fd08a14; box-shadow: none; }
  .fcard .show { border-radius: 6px; background: #ffffff05; box-shadow: none; }
  .play { border-color: #ffffff14; border-radius: 6px; }
  .notice { border-radius: 6px; }
  dialog { border-color: #ffffff14; border-radius: 6px; }
  footer { background: #0c110e; border-top-color: #ffffff0d; }
  footer .in { padding-top: 64px; padding-bottom: 28px; }
  .foot-cols h2 { color: var(--ink-2); font-weight: 400; letter-spacing: 0; }
  .legal { border-top-color: #ffffff0d; }
  .foot-cols li, .foot-brand p, .legal span { text-wrap: pretty; }
  @keyframes settle { from { opacity: 0; transform: translateY(12px) scale(.985); } }
  .rv { transform: translateY(12px); }
  .rv.rv-on { transition: opacity 480ms var(--arrive), transform 560ms var(--arrive); transition-delay: var(--d, 0ms); }
  .fcard.rv .glyph { transform: scale(.9); }
  .fcard.rv.rv-on .glyph { transition: opacity 320ms var(--arrive), transform 420ms var(--arrive); transition-delay: calc(var(--d, 0ms) + 120ms); }
  @media (hover: hover) and (pointer: fine) {
    .btn.primary:hover { box-shadow: 0 10px 28px -10px #7fd08ae6, inset 0 1px 0 #ffffff66; }
    .card:hover { box-shadow: none; }
    .card:hover .stage { box-shadow: inset 0 0 0 1px var(--leaf-deep), 0 14px 30px -14px #000c; }
    .who-row:hover, a.who-row:hover, .guild:hover { background: #ffffff0a; border-color: transparent; }
    .fcard:hover { border-color: #2f7d4699; background: #ffffff07; }
  }
  @media (max-width: 940px) {
    .spec { grid-template-columns: 160px minmax(0, 1fr); gap: 8px 32px; }
  }
  @media (max-width: 720px) {
    main { padding: 40px 16px 88px; }
    .top .in { padding-left: 16px; padding-right: 16px; }
    footer .in { padding: 48px 16px 24px; }
    h1 { font-size: 30px; }
    #main > h2 { margin-top: 80px; font-size: 30px; }
    .hero { grid-template-columns: 1fr; gap: 32px; }
    .hero h1 { font-size: 40px; }
    .features { grid-template-columns: 1fr; }
    .band { grid-template-columns: 1fr; gap: 12px; padding: 24px 0; }
    .split { gap: 28px; }
    .spec { grid-template-columns: 1fr; margin-top: 48px; }
    .spec section + section { margin-top: 64px; }
    .spec h2 { font-size: 30px; }
  }
  @media (max-width: 520px) {
    .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px 12px; }
    .grid .stage { padding: 18px 0 14px; }
    .grid .card.skel .stage { height: 167px; }
    .grid .meta { flex-wrap: wrap; white-space: normal; row-gap: 0; }
    .grid .meta .stars { margin-left: 0; }
    .make > summary { gap: 12px; padding: 14px 16px; }
    .make > summary img { display: none; }
    .grid .meta { padding: 10px 2px 0; }
  }
  h1 { font-size: 40px; line-height: 52px; }
  .hero h1 { font-size: 60px; line-height: 72px; font-weight: 400; letter-spacing: 0; }
  .hero h1 { overflow-wrap: break-word; }
  .hero h1:lang(ja) { font-size: 40px; line-height: 52px; }
  h2, #main > h2, .spec h2, .tpl h2, .policy h2 { font-size: 30px; line-height: 40px; }
  .side h2, .wipe-box h2, .foot-cols h2 { font-size: 20px; line-height: 28px; }
  h3, .features h3, .fcard h3, .band-head h3 { font-size: 20px; line-height: 28px; }
  b, strong, th { font-weight: 400; color: var(--ink); }
  .lede, .hero h1 + .lede { font-size: 20px; line-height: 32px; }
  .lede:lang(zh), .lede:lang(ja), .lede:lang(ko) { max-width: 36em; }
  .policy { max-width: 46em; line-height: 32px; }
  .policy:lang(zh), .policy:lang(ja), .policy:lang(ko) { max-width: 40em; }
  .btn { padding: 6px 18px; line-height: 28px; }
  .btn.small { padding: 2px 12px; line-height: 28px; }
  .chip { padding: 1px 12px; line-height: 28px; }
  .tag, .state, .tr-chip, .tr-count { padding-top: 2px; padding-bottom: 2px; }
  .who-tags { white-space: normal; overflow: visible; }
  .meta { flex-wrap: wrap; row-gap: 0; white-space: normal; }
  .meta .who { overflow: visible; }
  td .btn, th .btn, .tpl-sizes .btn { white-space: nowrap; }
  .btn { text-align: center; }
  select { max-width: 100%; min-width: 0; }
  #main :where(p, li, dd, dt, h1, h2, h3, label) { overflow-wrap: break-word; }
  .bot-state li, .bot-state code { word-break: break-all; }
  @media (min-width: 941px) { .foot { grid-template-columns: minmax(0, .8fr) minmax(0, 3.6fr); gap: 32px 40px; } }
  .foot-cols { gap: 26px 24px; }
  .foot-cols li { overflow-wrap: anywhere; }
  @media (max-width: 940px) { .foot { grid-template-columns: minmax(0, 1fr); } }
  .tr-src, .tr-dst, .tr-term, .tr-opt-text, .fb-body, .tr-item, .finder-hits small, input[type="text"], input[type="search"], textarea, .tr-text {
    font-family: "Segoe UI", system-ui, -apple-system, "Microsoft JhengHei UI", "Microsoft YaHei UI", "Yu Gothic UI", "Malgun Gothic", sans-serif;
    font-size: 16px; line-height: 26px; -webkit-font-smoothing: auto; font-smooth: auto; text-rendering: auto;
  }
  .tr-term { font-size: 20px; line-height: 30px; }
  :root { --pxa: var(--leaf); }
  .btn.primary, .chip[aria-pressed="true"] { --pxa: currentColor; }
  .ico.px { width: 22px; height: 22px; flex: none; shape-rendering: crispEdges; }
  .ico.px-down, .ico.px-out, .hn-trig .ico.px, #get > .ico.px:last-child, #hn-globe > .ico.px:last-child, .hn-avatar > .ico.px,
  .menu > a > .ico.px:last-child, .hn-plink > .ico.px:last-child, .hn-mi[aria-expanded] > .ico.px:last-child, .btn.small > .ico.px { width: 11px; height: 11px; }
  .btn > .ico.px { margin-left: -2px; }
  .hn-icon > .ico.px, .features .ico.px, .glyph.own svg.px, .fcard .show svg.px, .hn-ava-fb .ico.px { width: 22px; height: 22px; }
  @media (max-width: 720px) {
    h1, .hero h1 { font-size: 40px; line-height: 52px; }
    .hero h1:lang(ja) { font-size: 30px; line-height: 40px; }
    .hn-tip::after { content: none; }
    #main > h2, .spec h2, .policy h2 { font-size: 30px; line-height: 40px; }
    .who-top { flex-wrap: wrap; }
    .who-top > .mc { flex: 0 1 auto; margin-left: 0; padding-left: 0; text-align: left; }
  }
  :root { --gutter: clamp(16px, 4vw, 48px); }
  .hn-link[aria-current="page"], .hn-link.hn-cur, .spec-nav a[aria-current="true"] { box-shadow: inset 0 0 0 1px #7fd08a66; }
  .hero { grid-template-columns: minmax(0, .72fr) minmax(0, 1.28fr); gap: 72px; }
  .lineup { grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); }
  @media (min-width: 1200px) {
    .features { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0 56px; }
    .cards { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .cards.three { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .band { grid-template-columns: 240px minmax(0, 1fr); }
  }
  .policy, .tr-list, .fb-form, .fb-gate, .fb-notes { margin-right: auto; }
  .tr-list { max-width: 1120px; }
  .who-tags { word-break: keep-all; }
  .who-tag { white-space: nowrap; }
  @property --tone { syntax: '<color>'; inherits: true; initial-value: #7fd08a; }
  @property --tone-glow { syntax: '<color>'; inherits: true; initial-value: #22432c; }
  :root { --tone: #7fd08a; --tone-glow: #22432c; --pxa: var(--tone); transition: --tone 900ms ease, --tone-glow 900ms ease; }
  html[data-tone="moss"] { --tone: #b5cf6e; --tone-glow: #36421c; }
  html[data-tone="teal"] { --tone: #62cfb4; --tone-glow: #123f36; }
  html[data-tone="dusk"] { --tone: #e6b866; --tone-glow: #4a3312; }
  html, body { overflow-x: clip; }
  body { background-image: none; }
  body::before {
    content: ""; position: fixed; inset: 0; z-index: -1; pointer-events: none;
    background: radial-gradient(1200px 640px at 78% -12%, var(--tone-glow), transparent 70%),
      radial-gradient(900px 620px at -6% 108%, color-mix(in srgb, var(--tone-glow) 70%, transparent), transparent 70%);
  }
  [data-tone="forest"] { --sec: #7fd08a; }
  [data-tone="moss"] { --sec: #b5cf6e; }
  [data-tone="teal"] { --sec: #62cfb4; }
  [data-tone="dusk"] { --sec: #e6b866; }
  .hm-sec { position: relative; margin-top: 128px; }
  .hm-sec > h2 { margin: 0 0 28px; }
  .hm-sec > h2::after { content: ""; display: block; width: 40px; height: 4px; margin-top: 12px; background: var(--sec); }
  .hm-sec > h2 + .lede { margin: -12px 0 20px; }
  .hm-bento { display: grid; gap: 16px; grid-template-columns: repeat(4, minmax(0, 1fr)); grid-template-areas: "lead lead . ." "lead lead . ." "wide wide wide wide"; }
  .hm-tile { display: grid; align-content: start; gap: 8px; padding: 24px; border: 1px solid color-mix(in srgb, var(--sec) 22%, transparent); border-radius: 6px; background: #ffffff05; }
  .hm-tile > .ico { width: 22px; height: 22px; color: var(--sec); --pxa: var(--sec); }
  .hm-tile h3 { margin: 6px 0 0; }
  .hm-tile p { margin: 0; color: var(--ink-2); }
  .hm-lead { grid-area: lead; align-content: end; min-height: 380px; padding: 32px;
    background: linear-gradient(180deg, #0f151252 0%, #0f1512e6 62%, #0f1512 100%), var(--t-bg-world) center / cover no-repeat; }
  .hm-lead > .ico { width: 44px; height: 44px; }
  .hm-lead h3 { font-size: 20px; line-height: 28px; }
  .hm-lead p { max-width: 30em; color: var(--ink); }
  .hm-wide { grid-area: wide; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 20px;
    background: linear-gradient(90deg, color-mix(in srgb, var(--sec) 16%, transparent), transparent 72%); }
  .hm-wide h3 { margin: 0; }
  .hm-more { display: inline-flex; align-items: center; gap: 8px; white-space: nowrap; }
  .hm-install { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, .75fr); gap: 32px; align-items: start; }
  .hm-steps { margin: 0; padding: 0; list-style: none; display: grid; gap: 16px; }
  .hm-steps li { display: grid; grid-template-columns: 48px minmax(0, 1fr); gap: 16px; align-items: start; padding: 20px 24px;
    border: 1px solid color-mix(in srgb, var(--sec) 22%, transparent); border-radius: 6px; background: #ffffff05; }
  .hm-num { font-size: 40px; line-height: 52px; color: var(--sec); }
  .hm-steps p { margin: 10px 0 0; }
  .hm-note { margin: 0; padding: 20px 24px; border: 1px dashed #d8b76566; border-radius: 6px; }
  .hm-team .band { grid-template-columns: minmax(0, 1fr); gap: 16px; padding: 28px 0; }
  @media (min-width: 1001px) {
    .hm-team .team { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 3fr); column-gap: 32px; }
    .hm-team #band-contributors, .hm-team #band-guilds { grid-column: 1 / -1; }
  }
  .hm-team .lineup:not(.guilds) { grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
  .hm-team .lineup:not(.guilds) .who-row { position: relative; flex-direction: column; justify-content: flex-start; gap: 10px; height: 100%; padding: 20px 12px 16px; text-align: center; }
  .hm-team .lineup:not(.guilds) .who-row img, .hm-team .lineup:not(.guilds) .who-row .blank { width: 48px; height: 48px; }
  .hm-team .lineup:not(.guilds) .who-row > svg { position: absolute; top: 10px; right: 10px; margin: 0; }
  .hm-team .lineup:not(.guilds) .who-txt { align-items: center; }
  .hm-team .lineup:not(.guilds) .who-tags { text-wrap: balance; }
  .hm-team .lineup:not(.guilds) .who-top { flex-direction: column; align-items: center; gap: 0; }
  .hm-team .lineup:not(.guilds) .who-top > .mc, .hm-team .lineup:not(.guilds) .who-row > .mc { flex: none; margin: 0; padding: 0; text-align: center; }
  .hm-sponsor { padding: 80px 0; }
  .hm-sponsor::before { content: ""; position: absolute; inset: 0 calc(50% - 50vw); z-index: -1;
    background: linear-gradient(180deg, color-mix(in srgb, var(--sec) 9%, #0f1512), color-mix(in srgb, var(--sec) 3%, #0f1512));
    border-block: 1px solid color-mix(in srgb, var(--sec) 26%, transparent); }
  .hm-free { max-width: 46em; margin: 0; }
  .hm-points { margin: 28px 0 0; padding: 0; list-style: none; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; }
  .hm-points li { padding: 22px 24px; border: 1px solid color-mix(in srgb, var(--sec) 26%, transparent); border-radius: 6px; background: #0f151299; color: var(--ink-2); }
  .hm-points b { display: block; margin-bottom: 8px; color: var(--sec); }
  @media (max-width: 1000px) {
    .hm-bento { grid-template-columns: repeat(2, minmax(0, 1fr)); grid-template-areas: "lead lead" ". ." ". ." "wide wide"; }
    .hm-points { grid-template-columns: minmax(0, 1fr); }
  }
  @media (max-width: 720px) {
    .hero { grid-template-columns: minmax(0, 1fr); gap: 32px; }
    .hm-sec { margin-top: 88px; }
    .hm-bento { grid-template-columns: minmax(0, 1fr); grid-template-areas: none; }
    .hm-lead, .hm-wide { grid-area: auto; }
    .hm-lead { min-height: 300px; padding: 24px; }
    .hm-wide { grid-template-columns: auto minmax(0, 1fr); }
    .hm-more { grid-column: 1 / -1; }
    .hm-install { grid-template-columns: minmax(0, 1fr); }
    .hm-steps li { grid-template-columns: 32px minmax(0, 1fr); padding: 16px 18px; }
    .hm-num { font-size: 30px; line-height: 40px; }
    .hm-steps p { margin-top: 4px; }
    .hm-team .lineup:not(.guilds) { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .hm-sponsor { padding: 56px 0; }
  }
  @media (prefers-reduced-motion: reduce) {
    html { scroll-behavior: auto; }
    :root { transition: none; }
    .btn:active, .card:has(.open:active), .card:hover, .card:hover:has(.open:active), .star:active { transform: none; }
    .hero > div > *, .hero-screen, .card, .fig { animation-name: fade; animation-duration: 200ms; animation-delay: 0ms; }
    .chip:active, .hero-screen .screen:hover { transform: none; }
    .more a:hover .ico, .seg button:active, a.who-row:active, .guild:active, a.who-row:hover .ico, .guild:hover .ico { transform: none; }
    .menu, .menu.open { transform: none; }
    .hn-tip::after, .hn-tip:hover::after, .hn-tip:focus-visible::after { transform: translate(-50%, 0); }
    .hn-trig .ico, .hn-avatar > .ico, #get > .ico:last-child, #hn-globe > .ico:last-child, .hn-mi[aria-expanded] > .ico:last-child { transition: none; }
    .hn-panel, .hn-panel.open, .menu > *, .menu.open > *, .hn-panel > *, .hn-panel.open > * { transform: none; }
    .menu.open > *, .hn-panel.open > * { transition-delay: 0ms; }
    .hn-link:active, .hn-icon:active, .hn-avatar:active, .hn-plink:active, .brand:hover .logo { transform: none; }
    .hn-sheen::after, .hn-leaves { display: none; }
    .scr .hud > * { animation: none; }
    .inv .slot::after { animation: none !important; }
    .switch::after { transition: none; }
    .card.skel { animation: none; }
    dialog, dialog[open] { transform: none; }
    .seg .pill { transition: none; }
    @view-transition { navigation: none; }
    @starting-style { dialog[open] { transform: none; } }
  }
`;

const UI_SET = { zh: 'tc', 'zh-cn': 'sc', ja: 'ja', ko: 'ko' };

export const uiFontFace = (lang) => {
  const code = UI_SET[lang] || 'latin';
  return '\n  @font-face { font-family: WcyPx; src: url(/font/ui-' + code + '.woff?v=' + UI_FONTS[code].v
    + ') format("woff"); font-display: swap; }';
};

export const FONT_CSS = ''
  + Object.keys(FONTS).map((code) => '\n  @font-face { font-family: Px' + code.toUpperCase()
  + '; src: url(/font/px-' + code + '.ttf?v=' + FONTS[code].v + ') format("truetype"); font-display: swap; }').join('');

export const TEX_URL = Object.fromEntries(Object.keys(TEXTURES).map((name) =>
  [name, '/tex/' + name + '?v=' + TEXTURES[name].v]));

export const TEX_CSS = '\n  :root {' + Object.keys(TEX_URL).map((name) =>
  ' --t-' + name.replace(/\.\w+$/, '') + ': url(' + TEX_URL[name] + ');').join('') + ' }';
