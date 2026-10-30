
import { MORE_TEXT } from './site-text.js';
import { DEMO } from './demo-data.js';
import { FONTS } from './demo-fonts.js';
import { TEXTURES } from './demo-textures.js';
import { CAPE_TEMPLATES } from './cape-templates.js';

const STYLE = String.raw`
  :root {
    --ground: #0f1512; --panel: #161e19; --panel-2: #1c2620; --line: #2a382f;
    --ink: #e9efe9; --ink-2: #a3b3a7; --leaf: #7fd08a; --leaf-deep: #2f7d46;
    --gold: #d8b765; --cyan: #5bd8e8; --danger: #e08a8a;
    --out: cubic-bezier(0.23, 1, 0.32, 1); --arrive: cubic-bezier(0.16, 1, 0.3, 1); --move: cubic-bezier(0.77, 0, 0.175, 1);
    --fast: 140ms; --base: 200ms;
    --exit: cubic-bezier(0.32, 0, 0.67, 0);
  }
  * { box-sizing: border-box; }
  .skip { position: absolute; left: 12px; top: -60px; z-index: 20; padding: 8px 14px; border-radius: 9px; background: var(--leaf); color: #0c1a10; font-weight: 600; font-size: 14px; }
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
    font: 15px/1.7 "Segoe UI", "Microsoft JhengHei", "PingFang TC", "Noto Sans TC", system-ui, sans-serif;
    background-image: radial-gradient(1100px 520px at 50% -180px, #1d3324 0%, transparent 70%);
    background-repeat: no-repeat;
  }
  a { color: var(--leaf); text-decoration: none; text-underline-offset: 3px; }
  a:hover { text-decoration: underline; }
  a:focus-visible, button:focus-visible, input:focus-visible, select:focus-visible {
    outline: 2px solid var(--leaf); outline-offset: 2px;
  }
  .top { border-bottom: 1px solid var(--line); background: #0f1512e6; position: sticky; top: 0; z-index: 5; backdrop-filter: blur(8px); view-transition-name: top; }
  .top .in { position: relative; z-index: 1; max-width: 1120px; margin: 0 auto; padding: 12px 20px; display: flex; flex-wrap: nowrap; align-items: center; gap: 18px; }
  .hn-leaves { position: absolute; left: 0; top: 0; z-index: 0; width: 100%; height: 61px; pointer-events: none; image-rendering: pixelated; opacity: .72; }
  .brand { display: flex; align-items: center; gap: 10px; color: var(--ink); font-weight: 650; font-size: 17px; letter-spacing: .02em; flex: none; }
  .brand:hover { text-decoration: none; }
  .brand span { color: var(--leaf); display: flex; transition: color 300ms ease; }
  .brand svg { width: 26px; height: 26px; transform-origin: 25% 85%; transition: transform 520ms var(--out); }
  nav { display: flex; gap: 4px; flex-wrap: wrap; }
  nav a { padding: 6px 12px; border-radius: 8px; color: var(--ink-2); font-size: 14px; transition: color var(--fast) ease, background-color var(--fast) ease; }
  nav a:hover { color: var(--ink); background: var(--panel-2); text-decoration: none; }
  nav a[aria-current="page"] { color: var(--ink); background: var(--panel-2); }
  .hn { flex: 1 1 0; min-width: 0; overflow: hidden; display: block; }
  .hn.hn-live { overflow: visible; }
  .hn-list { display: flex; flex-wrap: nowrap; gap: 4px; margin: 0; padding: 0; list-style: none; }
  .hn-item { flex: none; position: relative; }
  .hn-item.hn-out { display: none; }
  .hn-link { display: inline-flex; align-items: center; gap: 4px; padding: 6px 12px; border: 0; border-radius: 8px; background: none; color: var(--ink-2); font: inherit; font-size: 14px; line-height: 1.7; white-space: nowrap; cursor: pointer; transition: color 150ms ease, background-color 150ms ease, transform var(--fast) var(--out); }
  .hn-link:active, .hn-icon:active, .hn-avatar:active, .hn-plink:active { transform: scale(.97); }
  .hn-link:hover { color: var(--ink); background: var(--panel-2); text-decoration: none; }
  .hn-link[aria-current="page"], .hn-link.hn-cur, .hn-trig[aria-expanded="true"] { color: var(--ink); background: var(--panel-2); }
  .hn-link.hn-cur { box-shadow: inset 0 -2px 0 var(--leaf-deep); }
  .hn-trig .ico, .hn-avatar > .ico, #get > .ico:last-child, #hn-globe > .ico:last-child { width: 14px; height: 14px; transition: transform var(--base) var(--out); }
  .hn-trig[aria-expanded="true"] .ico, .hn-avatar[aria-expanded="true"] > .ico, #get[aria-expanded="true"] > .ico:last-child, #hn-globe[aria-expanded="true"] > .ico:last-child { transform: rotate(180deg); }
  .hn .menu { left: 0; right: auto; width: auto; min-width: 190px; transform-origin: top left; }
  .tools { margin-left: auto; display: flex; align-items: center; gap: 8px; flex: none; }
  .ico { width: 16px; height: 16px; flex: none; }
  .btn.small { padding: 5px 11px; font-size: 13px; border-radius: 8px; gap: 6px; }
  .hn-icon { display: inline-flex; align-items: center; justify-content: center; gap: 2px; min-width: 34px; height: 34px; padding: 0 8px; border: 1px solid transparent; border-radius: 9px; background: transparent; color: var(--ink-2); font: inherit; cursor: pointer; transition: color 150ms ease, background-color 150ms ease, border-color 150ms ease, transform var(--fast) var(--out); }
  .hn-icon:hover { text-decoration: none; }
  .hn-icon > .ico { width: 18px; height: 18px; }
  #hn-globe > .ico:last-child { width: 13px; height: 13px; }
  .hn-icon[aria-expanded="true"] { color: var(--ink); background: var(--panel-2); }
  .hn-tip { position: relative; }
  .hn-tip::after {
    content: attr(data-tip); position: absolute; top: calc(100% + 8px); left: 50%; z-index: 7; padding: 3px 9px;
    border: 1px solid var(--line); border-radius: 7px; background: var(--panel-2); color: var(--ink); font-size: 12px; line-height: 1.6; white-space: nowrap;
    opacity: 0; pointer-events: none; transform: translate(-50%, -3px); transition: opacity var(--fast) ease, transform var(--fast) var(--out);
  }
  .hn-tip:focus-visible::after { opacity: 1; transform: translate(-50%, 0); transition-delay: 0ms; }
  .hn-tip[aria-expanded="true"]::after { opacity: 0 !important; transition-delay: 0ms !important; }
  .hn-sheen { position: relative; overflow: hidden; isolation: isolate; }
  .hn-sheen::after { content: ""; position: absolute; top: -40%; bottom: -40%; left: 0; width: 38%; pointer-events: none;
    background: linear-gradient(100deg, #fff0 0%, #ffffff8c 50%, #fff0 100%); transform: translateX(-190%) skewX(-18deg); }
  .hn-guest { display: flex; align-items: center; gap: 6px; }
  .hn-avatar { display: inline-flex; align-items: center; gap: 4px; height: 34px; padding: 3px 7px 3px 3px; border: 1px solid var(--line); border-radius: 9px; background: var(--panel); color: var(--ink-2); font: inherit; cursor: pointer; transition: border-color 150ms ease, color 150ms ease, transform var(--fast) var(--out); }
  .hn-avatar[aria-expanded="true"], .hn-avatar.hn-cur { border-color: var(--leaf-deep); color: var(--ink); }
  .hn-ava { width: 26px; height: 26px; border-radius: 6px; image-rendering: pixelated; flex: none; }
  .hn-ava-fb { display: inline-flex; align-items: center; justify-content: center; background: var(--panel-2); color: var(--leaf); }
  .hn-ava-fb .ico { width: 16px; height: 16px; }
  .hn-menu.hn-narrow { width: 200px; }
  .hn-acct { width: 240px; }
  .hn-acct-head { display: flex; align-items: center; gap: 10px; padding: 6px 10px 10px; margin-bottom: 4px; border-bottom: 1px solid var(--line); }
  .hn-acct-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--ink); font-weight: 600; font-size: 14px; }
  .hn-langs { display: grid; }
  .hn-sublangs { margin: 2px 0 2px 26px; padding-left: 6px; border-left: 1px solid var(--line); }
  .hn-mi { display: flex; align-items: center; gap: 10px; width: 100%; padding: 8px 10px; border: 0; border-radius: 8px; background: none; color: var(--ink); font: inherit; font-size: 14px; text-align: left; cursor: pointer; transition: background-color var(--fast) ease; }
  .menu > a.hn-mi { justify-content: flex-start; }
  .hn-mi .ico { color: var(--ink-2); }
  .hn-mi-val { margin-left: auto; color: var(--ink-2); font-size: 13px; }
  .hn-mi[aria-expanded] > .ico:last-child { width: 14px; height: 14px; transition: transform var(--fast) var(--out); }
  .hn-mi[aria-expanded="true"] > .ico:last-child { transform: rotate(180deg); }
  .hn-sep { height: 1px; margin: 6px 4px; border: 0; background: var(--line); }
  .hn-mhead { margin: 6px 0 2px; padding: 4px 10px 0; color: var(--ink-2); font-size: 12px; }
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
  .hn-phead { margin: 4px 0 4px; padding: 0 10px; color: var(--ink-2); font-size: 12px; letter-spacing: .04em; }
  .hn-plink { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 44px; padding: 8px 10px; border: 0; border-radius: 9px; background: none; color: var(--ink); font: inherit; font-size: 15px; text-align: left; cursor: pointer; transition: background-color 150ms ease, transform var(--fast) var(--out); }
  .hn-plink:hover { text-decoration: none; }
  .hn-plink .ico { color: var(--ink-2); }
  .hn-plink[aria-current="page"] { background: var(--panel-2); box-shadow: inset 3px 0 0 var(--leaf); }
  .hn-plink.hn-sub { padding-left: 22px; }
  .hn-pnote { margin: 4px 10px 2px; color: var(--ink-2); font-size: 12px; line-height: 1.55; }
  .hn-pme { display: flex; align-items: center; gap: 10px; margin: 2px 0 4px; padding: 0 10px; color: var(--ink); font-weight: 600; min-width: 0; }
  .hn-pme span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .hn-plangs { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 2px; }
  .hn-plangs > a { display: flex; align-items: center; justify-content: space-between; min-height: 44px; padding: 8px 10px; border-radius: 9px; color: var(--ink); }
  .hn-plangs > a:hover { text-decoration: none; }
  .hn-pgrid { display: grid; gap: 2px; }
  .hn-psignin { justify-self: start; margin: 2px 10px 4px; }
  .pop { position: relative; }
  .menu {
    position: absolute; right: 0; top: calc(100% + 8px); z-index: 6; width: 252px; padding: 6px;
    border: 1px solid var(--line); border-radius: 12px; background: var(--panel); box-shadow: 0 18px 44px #000a;
    opacity: 0; visibility: hidden; transform: translateY(-6px) scale(.96); transform-origin: top right;
    transition: opacity 120ms var(--exit), transform 120ms var(--exit), visibility 0s linear 120ms;
  }
  .menu.open { opacity: 1; visibility: visible; transform: none; transition: opacity 180ms var(--out), transform 200ms var(--out), visibility 0s; }
  .menu > *, .hn-panel > * { opacity: 0; transform: translateY(-3px); transition: opacity 0s linear 120ms, transform 0s linear 120ms, background-color 150ms ease; }
  .menu.open > *, .hn-panel.open > * { opacity: 1; transform: none;
    transition: opacity 180ms var(--out) calc(var(--i, 0) * 18ms), transform 220ms var(--out) calc(var(--i, 0) * 18ms), background-color 150ms ease; }
  .menu > a, .menu .hn-langs > a { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 8px 10px; border-radius: 8px; color: var(--ink); font-size: 14px; }
  .menu .hn-langs > a { transition: background-color 150ms ease; }
  .menu > a:hover, .menu .hn-langs > a:hover { text-decoration: none; }
  .menu > a .ico { color: var(--ink-2); }
  .menu > p:not(.hn-mhead) { margin: 6px 0 0; padding: 9px 10px 5px; border-top: 1px solid var(--line); color: var(--ink-2); font-size: 12px; line-height: 1.55; }
  select.lang { width: auto; font: inherit; font-size: 13px; padding: 5px 8px; border-radius: 8px; border: 1px solid var(--line); background: var(--panel); color: var(--ink-2); cursor: pointer; transition: color var(--fast) ease, border-color var(--fast) ease; }
  select.lang:hover { color: var(--ink); border-color: var(--leaf-deep); }
  select.pick { width: auto; font: inherit; font-size: 13px; padding: 5px 8px; border-radius: 8px; border: 1px solid var(--line); background: var(--panel); color: var(--ink); cursor: pointer; transition: border-color var(--fast) ease; }
  select.pick:hover { border-color: var(--leaf-deep); }
  :lang(zh-Hans) body { font-family: "Segoe UI", "Microsoft YaHei", "PingFang SC", "Noto Sans SC", system-ui, sans-serif; }
  :lang(ja) body { font-family: "Segoe UI", "Yu Gothic UI", "Hiragino Sans", "Noto Sans JP", "Meiryo", system-ui, sans-serif; }
  :lang(ko) body { font-family: "Segoe UI", "Malgun Gothic", "Apple SD Gothic Neo", "Noto Sans KR", system-ui, sans-serif; }
  main { flex: 1; width: 100%; max-width: 1120px; margin: 0 auto; padding: 44px 20px 72px; }
  h1 { margin: 0; font-size: 28px; font-weight: 650; letter-spacing: .02em; text-wrap: balance; }
  h2 { margin: 56px 0 14px; font-size: 19px; font-weight: 650; }
  .lede { margin: 6px 0 0; color: var(--ink-2); max-width: 62ch; text-wrap: pretty; }
  .policy { max-width: 72ch; line-height: 1.75; }
  .policy-date { margin: 8px 0 28px; color: var(--ink-2); font-size: 13px; }
  .policy h2 { margin: 36px 0 10px; font-size: 17px; }
  .policy p, .policy li { text-wrap: pretty; }
  .policy ul { margin: 8px 0; padding-left: 1.3em; display: grid; gap: 4px; }
  .policy b { color: var(--ink); font-weight: 600; }
  .panel { padding: 16px 18px; border: 1px solid var(--line); border-radius: 12px; background: var(--panel); }
  .tr-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 14px; margin: 20px 0 6px; }
  .tr-bar label { margin: 0; }
  .tr-role a { color: var(--leaf); }
  .tr-tabs { display: flex; flex-wrap: wrap; gap: 4px; margin: 16px 0; border-bottom: 1px solid var(--line); }
  .tr-tabs button { font: inherit; font-size: 15px; color: var(--ink-2); background: none; border: 0; border-bottom: 2px solid transparent; padding: 8px 12px; cursor: pointer; transition: color var(--fast) ease; }
  .tr-tabs button[aria-selected="true"] { color: var(--leaf); border-bottom-color: var(--leaf); }
  .tr-tabs button:focus-visible { outline: 2px solid var(--leaf); outline-offset: 2px; }
  .tr-count { color: var(--gold); font-variant-numeric: tabular-nums; }
  .tr-search { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 8px; }
  .tr-search input[type="search"] { flex: 1 1 280px; min-width: 0; font: inherit; padding: 8px 10px; border-radius: 8px; color: var(--ink); border: 1px solid var(--line); background: #0f1512; }
  .tr-search input[type="search"]:focus { border-color: var(--leaf-deep); outline: none; }
  .tr-check { display: inline-flex; align-items: center; gap: 6px; margin: 0; color: var(--ink-2); font-size: 14px; }
  .tr-list { display: grid; gap: 10px; }
  .tr-row { padding: 12px 14px; border: 1px solid var(--line); border-radius: 10px; background: var(--panel); min-width: 0; }
  .tr-head { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 8px; margin-bottom: 6px; }
  .tr-head a { color: var(--leaf); font-size: 13px; }
  .tr-chip { font-size: 12px; padding: 1px 8px; border-radius: 5px; background: var(--panel-2); color: var(--ink-2); overflow-wrap: anywhere; }
  .tr-chip.leaf { color: var(--leaf); background: #7fd08a1f; }
  .tr-chip.gold { color: var(--gold); background: #d8b7651f; }
  .tr-chip.bad { color: var(--danger); background: #e08a8a1f; }
  .tr-src { color: var(--ink-2); font-size: 14px; overflow-wrap: anywhere; }
  .tr-dst { color: var(--ink); margin-top: 4px; overflow-wrap: anywhere; }
  .tr-dst.empty { color: var(--ink-2); font-style: italic; }
  .tr-actions { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 10px; }
  .tr-actions input[type="text"] { flex: 1 1 200px; min-width: 0; width: auto; }
  .tr-more { margin-top: 10px; display: grid; gap: 10px; }
  .tr-more:empty { display: none; }
  .tr-editor { display: grid; gap: 4px; }
  .bot-state { list-style: none; margin: 0 0 4px; padding: 0; display: grid; gap: 4px; font-size: 14px; overflow-wrap: anywhere; }
  .bot-state .ok { color: var(--leaf); }
  .bot-state .bad { color: var(--danger); }
  .gv { display: grid; gap: 8px; margin-top: 6px; padding: 10px 12px; border-radius: 10px; background: #0b100d; box-shadow: inset 0 0 0 1px var(--line); }
  .gv-head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; }
  .gv-title { font-size: 13px; color: var(--ink-2); letter-spacing: .04em; }
  .gv-cols { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 12px; }
  .gv-col { margin: 0; display: grid; gap: 6px; align-content: start; min-width: 0; }
  .gv-col figcaption { font-size: 12px; color: var(--ink-2); }
  .gv-tip { position: relative; isolation: isolate; padding: 14px 14px 12px; color: #e8e8e8; font-size: 15px; line-height: 1.5; text-shadow: 1px 1px 0 #3a3a3a; overflow-x: auto; }
  .gv-tip::before { content: ""; position: absolute; inset: 0; z-index: -1; border: solid transparent; border-width: 12px 10px; border-image: var(--t-tip-plain) 12 10 fill / 12px 10px; image-rendering: pixelated; }
  .gv-line { white-space: pre-wrap; min-height: 1.5em; overflow-wrap: anywhere; }
  .gv-num { border-bottom: 1px dashed currentColor; font-variant-numeric: tabular-nums; }
  .gv-num sup { margin-left: 2px; font-size: 10px; line-height: 0; color: var(--leaf); text-shadow: none; }
  .gv-num.bad sup { color: var(--danger); }
  .gv-ph { text-decoration: underline dotted #ffffff70; text-underline-offset: 3px; }
  .gv-icon { display: inline-block; width: .8em; height: .8em; margin: 0 2px; vertical-align: -.05em; background: #777; box-shadow: inset -2px -2px 0 #4c4c4c; }
  .gv-note { margin: 0; font-size: 13px; }
  .gv-tools { margin-top: 2px; }
  .gv-tools .btn { font-family: Consolas, "Cascadia Mono", monospace; font-size: 13px; padding: 4px 9px; }
  .tr-text { width: 100%; font: inherit; padding: 8px 10px; border-radius: 8px; color: var(--ink); border: 1px solid var(--line); background: #0f1512; resize: vertical; }
  .tr-text:focus { border-color: var(--leaf-deep); outline: none; }
  .tr-prop { display: grid; gap: 8px; padding-top: 10px; border-top: 1px solid var(--line); }
  .tr-item { padding: 8px 10px; border-radius: 8px; background: var(--panel-2); }
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
  .fb-meta { display: flex; flex-wrap: wrap; gap: 4px 14px; margin: 4px 0 0; color: var(--ink-2); font-size: 13px; }
  .fb-meta a { color: var(--leaf); }
  .fb-meta .bad { color: var(--danger); }
  .tr-item.won, .tr-opt.won { box-shadow: inset 0 0 0 1px var(--leaf-deep); }
  .tr-foot { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 6px; }
  .tr-votes { color: var(--gold); font-size: 13px; font-variant-numeric: tabular-nums; }
  .tr-term { margin: 2px 0 6px; font-size: 18px; font-weight: 650; overflow-wrap: anywhere; }
  .tr-opt { position: relative; display: flex; flex-wrap: wrap; gap: 8px; align-items: center; padding: 8px 10px; border-radius: 8px; background: var(--panel-2); margin-top: 6px; overflow: hidden; }
  .tr-opt > * { position: relative; }
  .tr-opt > .tr-bar-fill { position: absolute; inset: 0 auto 0 0; background: #7fd08a14; pointer-events: none; }
  .tr-opt-text { flex: 1 1 auto; font-weight: 600; overflow-wrap: anywhere; }
  .tr-newpoll { margin-bottom: 14px; display: grid; gap: 4px; }
  .tr-langs { display: flex; flex-wrap: wrap; gap: 6px 14px; margin: 12px 0 0; padding: 0; border: 0; }
  .tr-langs legend { font-size: 13px; color: var(--ink-2); margin-bottom: 6px; padding: 0; }
  .lt-state { margin: 12px 0 0; padding: 10px 12px; border: 1px solid var(--line); border-radius: 10px; background: var(--panel-2); font-size: 13px; }
  .lt-state p { margin: 0; }
  .lt-state p + p { margin-top: 4px; color: var(--ink-2); }
  .lt-state.warn { border-color: var(--gold); }
  .tr-langs[disabled] { opacity: .5; }
  .lt-table { min-width: 680px; }
  .lt-table .tr-chip { white-space: nowrap; }
  .tr-langs label { display: inline-flex; align-items: center; gap: 6px; margin: 0; color: var(--ink); font-size: 14px; }
  .note { color: var(--ink-2); font-size: 14px; }
  .note b { color: var(--gold); font-weight: 600; }
  .btn {
    display: inline-flex; align-items: center; gap: 8px; font: inherit; font-size: 14px; cursor: pointer;
    padding: 8px 16px; border-radius: 9px; border: 1px solid var(--line); background: var(--panel-2); color: var(--ink);
    transition: transform var(--fast) var(--out), background-color 160ms ease, border-color 160ms ease, color 160ms ease, opacity 160ms ease;
  }
  .btn:hover { text-decoration: none; }
  .btn:active { transform: scale(.97); }
  .btn.primary { background: var(--leaf); border-color: var(--leaf); color: #0c1a10; font-weight: 600; }
  .btn.danger { border-color: #5a2b2b; background: #2e1a1a; color: var(--danger); }
  .btn[disabled] { opacity: .6; cursor: progress; }
  .btn[disabled]:active { transform: none; }
  /* 懸停只給有滑鼠的裝置：觸控點一下會卡在懸停的樣子 */
  @media (hover: hover) and (pointer: fine) {
    .btn:hover { border-color: var(--leaf-deep); }
    .btn.primary:hover { background: #93dba0; border-color: #93dba0; }
    .btn.danger:hover { background: #3d2020; border-color: #5a2b2b; }
    tbody tr:hover td { background: #ffffff06; }
    .hn-icon:hover, .hn-avatar:hover { color: var(--ink); border-color: var(--leaf-deep); }
    .hn-heart:hover { color: #f2a7b6; background: var(--panel-2); border-color: transparent; }
    .hn-tip:hover::after { opacity: 1; transform: translate(-50%, 0); transition-delay: 300ms; }
    .hn-sheen:hover::after { transform: translateX(340%) skewX(-18deg); transition: transform 680ms var(--out); }
    .brand:hover span { color: #a9e8b2; }
    .brand:hover svg { transform: rotate(-12deg) scale(1.06); }
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
  label { display: block; font-size: 13px; color: var(--ink-2); margin: 0 0 4px; }
  input[type="text"], select {
    width: 100%; font: inherit; padding: 8px 10px; border-radius: 8px; color: var(--ink);
    border: 1px solid var(--line); background: #0f1512; transition: border-color var(--fast) ease;
  }
  input[type="text"]:focus { border-color: var(--leaf-deep); }
  input[type="file"] { font: inherit; font-size: 14px; color: var(--ink-2); max-width: 100%; }
  input[type="file"]::file-selector-button {
    font: inherit; font-size: 14px; cursor: pointer; margin-right: 10px; padding: 7px 14px; border-radius: 9px;
    border: 1px solid var(--line); background: var(--panel-2); color: var(--ink);
    transition: border-color 160ms ease;
  }
  td { transition: background-color var(--fast) ease; }
  .msg { margin: 10px 0 0; font-size: 14px; min-height: 1.4em; }
  .msg.ok { color: var(--leaf); } .msg.bad { color: var(--danger); }
  table { width: 100%; border-collapse: collapse; font-size: 14px; }
  th { text-align: left; font-weight: 600; color: var(--ink-2); font-size: 13px; padding: 8px 10px; border-bottom: 1px solid var(--line); }
  td { padding: 9px 10px; border-bottom: 1px solid var(--line); vertical-align: middle; font-variant-numeric: tabular-nums; }
  td img { width: 22px; height: 22px; border-radius: 4px; vertical-align: middle; margin-right: 8px; image-rendering: pixelated; }
  .scroll { overflow-x: auto; }
  code { font-family: Consolas, "Cascadia Mono", monospace; font-size: 13px; color: var(--ink-2); }
  footer { border-top: 1px solid var(--line); color: var(--ink-2); font-size: 13px; background: #0c110e; }
  footer .in { max-width: 1120px; margin: 0 auto; padding: 40px 20px 26px; }
  .foot { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(0, 3fr); gap: 32px 56px; }
  .foot-brand p { margin: 10px 0 0; max-width: 34ch; text-wrap: pretty; }
  .foot-cols { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 26px 20px; }
  .foot-cols h2 { margin: 0 0 10px; font-size: 13px; font-weight: 650; color: var(--ink); }
  .foot-cols ul { margin: 0; padding: 0; list-style: none; display: grid; gap: 7px; }
  .foot-cols a { color: var(--ink-2); transition: color var(--fast) ease; }
  .copy { font: inherit; color: var(--ink-2); background: none; border: 0; padding: 0; cursor: pointer; text-align: left; transition: color var(--fast) ease; }
  .legal { display: flex; flex-wrap: wrap; gap: 4px 24px; justify-content: space-between; margin-top: 34px; padding-top: 18px; border-top: 1px solid var(--line); font-size: 12px; }
  [hidden] { display: none !important; }

  .hero { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.04fr); gap: 48px; align-items: center; padding: 14px 0 8px; }
  .hero h1 { font-size: clamp(34px, 4.3vw, 50px); line-height: 1.14; letter-spacing: -.012em; text-wrap: balance; }
  .hero .lede { font-size: 16px; }
  .hero-screen { min-width: 0; animation: settle 720ms var(--arrive) 80ms both; }
  .hero-screen .screen { display: block; aspect-ratio: 480 / 470; border: 1px solid var(--line); border-radius: 14px; box-shadow: 0 34px 70px -24px #000000e6, 0 0 0 6px #ffffff07; transition: border-color var(--base) ease, transform var(--base) var(--out); }
  .hero-cap { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: 4px 16px; margin: 12px 4px 0; font-size: 13px; color: var(--ink-2); }
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
  .chip { padding: 3px 11px; border: 1px solid var(--line); border-radius: 999px; font: inherit; font-size: 13px; color: var(--ink-2); background: var(--panel); cursor: pointer; transition: color var(--fast) ease, border-color var(--fast) ease, background-color var(--fast) ease, transform var(--fast) var(--out); }
  .chip:active { transform: scale(.96); }
  .chip[aria-pressed="true"] { color: #0c1a10; background: var(--leaf); border-color: var(--leaf); }
  .features { display: grid; gap: 0 32px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .features > div { padding: 16px 0 20px; border-top: 1px solid var(--line); }
  .features h3 { margin: 0 0 4px; font-size: 15px; font-weight: 650; }
  .features p { margin: 0; color: var(--ink-2); font-size: 14px; max-width: 38ch; }
  .more { margin: 14px 0 0; font-size: 14px; }
  .more a { display: inline-flex; align-items: center; gap: 6px; }
  .more .ico { transition: transform var(--fast) var(--out); }
  .team { margin-top: 20px; border-bottom: 1px solid var(--line); }
  .band { display: grid; grid-template-columns: 190px minmax(0, 1fr); gap: 14px 28px; padding: 22px 0; border-top: 1px solid var(--line); }
  .band-head { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 2px 10px; align-content: start; }
  .band-head > span { grid-row: span 2; margin-top: 1px; display: flex; }
  .band-head svg { width: 24px; height: 24px; }
  .band-head h3 { margin: 0; font-size: 16px; font-weight: 650; }
  .band-head p { margin: 0; grid-column: 2; color: var(--ink-2); font-size: 13px; line-height: 1.55; }
  .lineup { margin: 0; padding: 0; list-style: none; display: grid; grid-template-columns: repeat(auto-fill, minmax(236px, 1fr)); gap: 8px 12px; align-content: start; min-height: 58px; }
  .fig { min-width: 0; animation: fade 320ms ease both; animation-delay: calc(var(--i, 0) * 25ms); }
  .who-row, .guild {
    display: flex; align-items: center; gap: 12px; min-width: 0; border: 1px solid var(--line); background: var(--panel); color: var(--ink);
    transition: border-color var(--fast) ease, background-color var(--fast) ease, transform var(--fast) var(--out);
  }
  .who-row { padding: 8px 12px; border-radius: 10px; }
  a.who-row:active, .guild:active { transform: scale(.985); }
  .who-row img, .who-row .blank { width: 40px; height: 40px; flex: none; border-radius: 6px; background: var(--panel-2); }
  .who-row img { image-rendering: pixelated; opacity: 0; transition: opacity 240ms ease; }
  .who-row img.in, .guild img.in { opacity: 1; }
  .who-row .blank { display: grid; place-items: center; color: var(--line); }
  .who-row .blank svg { width: 22px; height: 22px; }
  .who-row b { min-width: 0; font-size: 14px; font-weight: 650; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .who-row .mc { min-width: 0; margin-left: auto; padding-left: 6px; font-size: 13px; color: var(--ink-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .who-row > svg { width: 18px; height: 18px; flex: none; }
  .who-row b:only-of-type { flex: 1; }
  .who-row b + svg { margin-left: auto; }
  .who-txt { display: flex; flex-direction: column; flex: 1; min-width: 0; line-height: 1.45; }
  .who-top { display: flex; align-items: baseline; gap: 6px; min-width: 0; }
  .who-tags { min-width: 0; font-size: 12px; color: var(--ink-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .who-tags .tl { color: var(--leaf); }
  .team-pick { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
  .lineup.guilds { grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); }
  .guild { padding: 10px 14px 10px 12px; border-radius: 12px; }
  .guild img { opacity: 0; transition: opacity 240ms ease; }
  .g-banner { width: 28px; height: 56px; flex: none; object-fit: contain; image-rendering: pixelated; filter: drop-shadow(0 4px 8px #0009); }
  .g-icon { width: 56px; height: 56px; flex: none; border-radius: 12px; object-fit: contain; background: #f4f8f6; }
  span.g-icon { display: grid; place-items: center; background: var(--panel-2); color: var(--ink-2); font-size: 13px; font-weight: 650; letter-spacing: .04em; }
  .g-name { display: flex; align-items: baseline; gap: 7px; min-width: 0; flex: 1; }
  .g-name b { min-width: 0; font-size: 16px; font-weight: 650; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .g-name span { flex: none; font-size: 14px; font-weight: 600; color: var(--leaf); font-variant-numeric: tabular-nums; }
  .guild .ico, .who-row .ico { color: var(--ink-2); transition: color var(--fast) ease, transform var(--fast) var(--out); }
  .everyone { display: flex; align-items: center; gap: 10px; margin: 16px 0 0; color: var(--ink-2); font-size: 14px; }
  .everyone svg { width: 20px; height: 20px; }
  .everyone b { margin-right: .7em; color: var(--ink); font-weight: 600; }

  .play { margin-top: 26px; border: 1px solid var(--line); border-radius: 14px; background: var(--panel); overflow: hidden; }
  .play-bar { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 10px 18px; padding: 12px 14px; border-bottom: 1px solid var(--line); }
  .play-lang { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 13px; color: var(--ink-2); }
  .seg { position: relative; display: inline-flex; flex-wrap: wrap; gap: 2px; padding: 3px; border: 1px solid var(--line); border-radius: 10px; background: var(--ground); }
  .seg .pill { position: absolute; left: 0; top: 0; border-radius: 7px; background: var(--panel-2); box-shadow: inset 0 0 0 1px var(--line); pointer-events: none; transition: transform 220ms var(--out), width 220ms var(--out), height 220ms var(--out), opacity var(--fast) ease; }
  .seg button { position: relative; }
  .seg.live button[aria-pressed="true"] { background: none; box-shadow: none; }
  .seg button { font: inherit; font-size: 13px; padding: 5px 12px; border: 0; border-radius: 7px; background: none; color: var(--ink-2); cursor: pointer; transition: color var(--fast) ease, background-color var(--fast) ease, transform var(--fast) var(--out); }
  .seg button:active { transform: scale(.97); }
  .seg button[aria-pressed="true"] { background: var(--panel-2); color: var(--ink); box-shadow: inset 0 0 0 1px var(--line); }
  .play-body { display: grid; }
  .play-view { border-bottom: 1px solid var(--line); background: #0a0f0c; }
  .play-set { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 32px; align-content: start; padding: 0 16px; overflow: hidden; }
  .set-row { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px 14px; min-height: 52px; margin-top: -1px; padding: 9px 0; border-top: 1px solid var(--line); font-size: 14px; transition: opacity var(--base) ease; }
  .set-row > span { font-weight: 600; }
  .set-row.dim { opacity: .45; }
  .switch { position: relative; width: 40px; height: 22px; flex: none; padding: 0; border: 1px solid var(--line); border-radius: 999px; background: var(--ground); cursor: pointer; transition: background-color var(--base) ease, border-color var(--base) ease; }
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
  .demo-foot { display: flex; flex-wrap: wrap; gap: 4px 16px; align-items: baseline; margin: 0; padding: 12px 14px 13px; border-top: 1px solid var(--line); font-size: 14px; min-height: 3.2em; }
  .demo-foot .hint { flex-basis: 100%; }
  .wearbox { display: grid; gap: 8px; }
  .spec { display: grid; grid-template-columns: 170px minmax(0, 1fr); gap: 8px 44px; align-items: start; margin-top: 48px; }
  .spec-nav { position: sticky; top: 78px; display: grid; gap: 2px; font-size: 14px; }
  .spec-nav a { padding: 5px 10px; border-radius: 8px; color: var(--ink-2); transition: color var(--fast) ease, background-color var(--fast) ease; }
  .spec-nav a[aria-current="true"] { color: var(--ink); background: var(--panel-2); }
  .spec section + section { margin-top: 48px; }
  .spec h2 { margin: 0 0 12px; }
  .spec .note { margin: 14px 0 0; max-width: 68ch; }
  .cards { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
  .cards.three { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .fcard {
    display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 14px; align-items: start; padding: 14px 16px 15px;
    border: 1px solid var(--line); border-radius: 12px; background: var(--panel);
    transition: border-color var(--base) ease, background-color var(--base) ease;
  }
  .fcard h3 { margin: 1px 0 3px; font-size: 15px; font-weight: 650; }
  .fcard p { margin: 0; color: var(--ink-2); font-size: 14px; text-wrap: pretty; }
  .fcard code { font-size: 13px; }
  .glyph { position: relative; display: block; width: 40px; height: 40px; border-radius: 9px; background: #0c120f; box-shadow: inset 0 0 0 1px var(--line); color: var(--leaf); }
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
  .fcard .show { display: flex; align-items: center; gap: 10px; min-height: 56px; padding: 8px 12px; border-radius: 9px; background: #0c120f; box-shadow: inset 0 0 0 1px var(--line); }
  .fcard .show svg { width: 26px; height: 26px; }
  .capemini { width: 30px; aspect-ratio: 10 / 16; overflow: hidden; border-radius: 2px; box-shadow: 0 0 0 1px #0008; }
  .capemini img { display: block; width: 220%; max-width: none; margin: -10% 0 0 -10%; image-rendering: pixelated; }
  .fcard.mod h3 { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 8px; }
  .fcard.mod .state { margin-left: 0; }
  .tpl { display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr); gap: 22px; align-items: center; padding: 18px; border: 1px solid var(--leaf-deep); border-radius: 14px; background: linear-gradient(180deg, #17241c, var(--panel) 70%); }
  .tpl .guide { display: block; width: 100%; height: auto; border-radius: 8px; box-shadow: 0 0 0 1px var(--line); }
  .tpl h2 { margin: 0 0 6px; }
  .tpl p { margin: 0 0 12px; color: var(--ink-2); font-size: 14px; }
  .tpl .label { margin: 0 0 8px; color: var(--ink); font-size: 13px; font-weight: 600; }
  .tpl-sizes { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
  .tpl-sizes .btn { padding: 7px 12px; font-variant-numeric: tabular-nums; }
  .tpl-sizes small { font-size: 11px; opacity: .78; }
  .tpl .more-links { display: flex; flex-wrap: wrap; gap: 6px 18px; font-size: 14px; }
  .make { margin: 20px 0 4px; }
  .make > summary { display: flex; align-items: center; gap: 14px; padding: 12px 16px; border: 1px solid var(--leaf-deep); border-radius: 12px; background: #17241c; cursor: pointer; list-style: none; transition: background-color var(--fast) ease, transform var(--fast) var(--out); }
  .make > summary::-webkit-details-marker { display: none; }
  .make > summary:active { transform: scale(.995); }
  .make > summary img { width: 44px; height: 34px; flex: none; image-rendering: pixelated; border-radius: 3px; box-shadow: 0 0 0 1px #0008; }
  .make > summary b { display: block; font-size: 15px; font-weight: 650; }
  .make > summary .sub { display: block; color: var(--ink-2); font-size: 13px; }
  .make > summary .btn { margin-left: auto; flex: none; pointer-events: none; }
  .make > summary .btn svg { transition: transform var(--base) var(--out); }
  .make[open] > summary { border-radius: 12px 12px 0 0; border-bottom-color: transparent; }
  .make[open] > summary .btn svg { transform: rotate(180deg); }
  .make[open] > .tpl { border-top: 0; border-radius: 0 0 14px 14px; background: linear-gradient(180deg, #17241c, var(--panel) 60%); }
  .state { display: inline-block; margin-left: 6px; padding: 0 8px; border: 1px solid var(--line); border-radius: 999px; font-size: 11px; font-weight: 600; line-height: 1.7; color: var(--ink-2); vertical-align: 1px; white-space: nowrap; }
  .state.need { color: var(--gold); border-color: #5c4d27; }
  .plain { margin: 10px 0 0; padding-left: 1.2em; color: var(--ink-2); }
  .plain li { margin: 4px 0; } .plain b { color: var(--ink); font-weight: 600; }
  .split { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 14px; }

  .bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 18px; margin: 22px 0 14px; }
  .count { font-variant-numeric: tabular-nums; color: var(--ink-2); }
  .count b { color: var(--ink); font-weight: 600; }
  .grid { display: grid; gap: 18px; grid-template-columns: repeat(auto-fill, minmax(168px, 1fr)); }
  .card {
    position: relative; display: flex; flex-direction: column;
    background: var(--panel); border: 1px solid var(--line); border-radius: 12px; overflow: hidden;
    transition: transform 180ms var(--out), border-color 180ms ease, box-shadow 180ms ease;
    animation: rise 360ms var(--arrive) both; animation-delay: calc(var(--i, 0) * 35ms);
  }
  .card .open { display: flex; flex-direction: column; width: 100%; padding: 0; border: 0; background: none; color: inherit; font: inherit; text-align: left; cursor: pointer; }
  .card .open:focus-visible { outline-offset: -3px; }
  .card:has(.open:active) { transform: scale(.98); }
  .star {
    position: absolute; top: 8px; right: 8px; display: grid; place-items: center; width: 32px; height: 32px; padding: 0;
    border: 0; border-radius: 9px; background: #0b0f0d8c; color: var(--ink-2); cursor: pointer;
    transition: color var(--fast) ease, background-color var(--fast) ease, transform var(--fast) var(--out);
  }
  .star svg, .stars svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linejoin: round; }
  .star[aria-pressed="true"] { color: var(--gold); }
  .star[aria-pressed="true"] svg { fill: currentColor; }
  .star:active { transform: scale(.88); }
  .meta .stars { display: inline-flex; align-items: center; gap: 3px; color: var(--gold); font-variant-numeric: tabular-nums; }
  .meta .stars svg { width: 12px; height: 12px; fill: currentColor; stroke: none; }
  .tag.off { background: none; color: var(--ink-2); box-shadow: inset 0 0 0 1px var(--line); }
  .tag.wait { background: #5c4d27; color: var(--gold); }
  .tag.mine { background: var(--leaf-deep); color: #fff; }
  .sortby { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 13px; color: var(--ink-2); }
  .filters { margin: 0 0 14px; }
  .set-line { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--line); }
  .set-line b { display: block; font-weight: 600; font-size: 14px; }
  .set-line .hint { margin-top: 2px; }
  @media (hover: hover) and (pointer: fine) {
    .card:hover { transform: translateY(-3px); border-color: var(--leaf-deep); box-shadow: 0 10px 26px #0008; }
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
  .meta { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 43px; padding: 10px 12px 12px; font-size: 13px; color: var(--ink-2); white-space: nowrap; }
  .tag { padding: 1px 7px; border-radius: 999px; font-size: 11px; font-weight: 600; letter-spacing: .06em; color: var(--ground); background: var(--ink-2); }
  .tag.gif { background: var(--gold); }
  .meta .who { min-width: 0; overflow: hidden; text-overflow: ellipsis; font-variant-numeric: tabular-nums; }
  .meta .who.idle { color: var(--gold); }
  .meta .stars { flex: none; margin-left: auto; }
  .notice { margin: 0 0 16px; padding: 10px 14px; border: 1px solid #5c4d27; border-radius: 10px; background: #2a241266; color: var(--gold); font-size: 14px; }
  .empty { grid-column: 1 / -1; padding: 64px 0; text-align: center; color: var(--ink-2); }
  dialog {
    width: min(92vw, 780px); padding: 0; color: var(--ink); background: var(--panel); border: 1px solid var(--line); border-radius: 14px; box-shadow: 0 30px 80px #000c;
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
  .linkcode { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin: 10px 0; }
  .linkcode code { font-size: 18px; color: var(--ink); background: #0f1512; border: 1px solid var(--line); border-radius: 8px; padding: 8px 14px; user-select: all; }
  .linkcode code.share-url { flex: 1 1 220px; min-width: 0; font-size: 12px; padding: 7px 10px; word-break: break-all; }
  .vis-line { flex-direction: column; align-items: stretch; gap: 8px; }
  .vis-line .seg { align-self: flex-start; }
  .wipe-panel { margin-top: 28px; max-width: 720px; border-color: #5a2b2b; }
  #wipe { width: min(92vw, 560px); }
  .wipe-box { padding: 22px 24px; }
  .wipe-box h2 { margin: 0 0 10px; font-size: 18px; }
  .wipe-box p, .wipe-box li { color: var(--ink-2); font-size: 14px; line-height: 1.55; }
  .wipe-box ul { margin: 6px 0 12px; padding-left: 1.2em; }
  .wipe-box b { color: var(--ink); }
  .wipe-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; margin-top: 8px; }
  .form { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); align-items: end; }
  .finder { position: relative; }
  .finder input { padding-right: 32px; cursor: pointer; }
  .finder input:focus { cursor: text; }
  .finder::after { content: ''; position: absolute; right: 13px; bottom: 17px; width: 7px; height: 7px; border-right: 2px solid var(--ink-2); border-bottom: 2px solid var(--ink-2); transform: rotate(45deg); pointer-events: none; transition: transform var(--fast) var(--out); }
  .finder.open::after { transform: translateY(4px) rotate(-135deg); }
  .finder-hits { position: absolute; left: 0; right: 0; min-width: min(360px, calc(100vw - 32px)); top: calc(100% + 4px); z-index: 4; margin: 0; padding: 4px; list-style: none; max-height: 320px; overflow-y: auto; border: 1px solid var(--line); border-radius: 10px; background: var(--panel); box-shadow: 0 18px 44px #000a; }
  .finder-hits li { display: flex; align-items: center; gap: 10px; padding: 7px 8px; border-radius: 7px; cursor: pointer; transition: background-color var(--fast) ease; }
  .finder-hits li:hover, .finder-hits li[aria-selected="true"] { background: var(--panel-2); }
  .finder-hits li.taken, .finder-hits li.none { cursor: default; background: none; }
  .finder-hits li.taken { opacity: .55; }
  .finder-hits li.none { color: var(--ink-2); font-size: 13px; }
  .finder-hits li.count { cursor: default; background: none; padding: 4px 8px 6px; color: var(--ink-2); font-size: 12px; }
  .finder-hits img { width: 28px; height: 28px; border-radius: 5px; image-rendering: pixelated; flex: none; }
  .finder-hits div { min-width: 0; }
  .finder-hits b { display: block; font-weight: 600; color: var(--ink); }
  .finder-hits small { display: block; color: var(--ink-2); font-size: 12px; overflow-wrap: anywhere; }
  .drop { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 12px; padding: 14px; border: 1px dashed var(--line); border-radius: 10px; transition: border-color var(--fast) ease, background-color var(--fast) ease; }
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
  @media (prefers-reduced-motion: reduce) {
    html { scroll-behavior: auto; }
    .btn:active, .card:has(.open:active), .card:hover, .card:hover:has(.open:active), .star:active { transform: none; }
    .hero > div > *, .hero-screen, .card, .fig { animation-name: fade; animation-duration: 200ms; animation-delay: 0ms; }
    .chip:active, .hero-screen .screen:hover { transform: none; }
    .more a:hover .ico, .seg button:active, a.who-row:active, .guild:active, a.who-row:hover .ico, .guild:hover .ico { transform: none; }
    .menu, .menu.open { transform: none; }
    .hn-tip::after, .hn-tip:hover::after, .hn-tip:focus-visible::after { transform: translate(-50%, 0); }
    .hn-trig .ico, .hn-avatar > .ico, #get > .ico:last-child, #hn-globe > .ico:last-child, .hn-mi[aria-expanded] > .ico:last-child { transition: none; }
    .hn-panel, .hn-panel.open, .menu > *, .menu.open > *, .hn-panel > *, .hn-panel.open > * { transform: none; }
    .menu.open > *, .hn-panel.open > * { transition-delay: 0ms; }
    .hn-link:active, .hn-icon:active, .hn-avatar:active, .hn-plink:active, .brand:hover svg { transform: none; }
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

const LEAF = String.raw`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 21c.5-4.5 2.5-8 7-10"/><path d="M9 18c6.2 0 10.5-3.3 11-12V4h-4C7 4 4 8 4 13c0 1 0 3 2 5h3z"/></svg>`;

const TEMPLATES = 'https://github.com/LyuChaCha/WynnChaYuan/tree/main/capes/templates';
const ISSUES = 'https://github.com/LyuChaCha/WynnChaYuan/issues';
const SIZES = '22×17, 44×34, 88×68, 176×136, 352×272, 704×544, 1056×816';
const REPO = 'https://github.com/LyuChaCha/WynnChaYuan';
const CURSEFORGE = 'https://www.curseforge.com/minecraft/mc-mods/wynnchayuan';
const RELEASES = REPO + '/releases/latest';
const KOFI = 'https://ko-fi.com/lyuchacha';

const stroke = (paths) => '<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" '
  + 'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + paths + '</svg>';
const ICON = {
  get: stroke('<path d="M12 4v11"/><path d="m7 11 5 5 5-5"/><path d="M5 20h14"/>'),
  heart: stroke('<path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7a4.3 4.3 0 0 1 7.5 2.8C19.5 15.4 12 20 12 20z"/>'),
  down: stroke('<path d="m7 10 5 5 5-5"/>'),
  out: stroke('<path d="M8 16 16 8"/><path d="M9.5 8H16v6.5"/>'),
  go: stroke('<path d="M5 12h14"/><path d="m13 6 6 6-6 6"/>'),
  flag: stroke('<path d="M6 21V4"/><path d="M6 5h12l-2.5 4 2.5 4H6"/>'),
  exit: stroke('<path d="M9 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h3"/><path d="m15 16 4-4-4-4"/><path d="M19 12H9"/>'),
  globe: stroke('<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17"/><path d="M12 3.5c2.3 2.5 3.4 5.3 3.4 8.5s-1.1 6-3.4 8.5c-2.3-2.5-3.4-5.3-3.4-8.5s1.1-6 3.4-8.5z"/>'),
  menu: stroke('<path d="M4 7h16"/><path d="M4 12h16"/><path d="M4 17h16"/>'),
  user: stroke('<circle cx="12" cy="8.5" r="3.5"/><path d="M5 20c.8-3.6 3.6-5.5 7-5.5s6.2 1.9 7 5.5"/>'),
  check: stroke('<path d="m5 12.5 4.5 4.5L19 7.5"/>'),
  account: stroke('<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="10" r="2.8"/><path d="M7 17.6c1.1-1.9 2.9-2.9 5-2.9s3.9 1 5 2.9"/>'),
  admin: stroke('<path d="M12 3.5 19 6v5.5c0 4.2-3 7.6-7 9-4-1.4-7-4.8-7-9V6z"/><path d="m9 12 2 2 4-4"/>'),
};


export const TEXT = {
  zh: {
    html: 'zh-Hant',
    'nav.home': '首頁', 'nav.features': '功能', 'nav.capes': '披風', 'nav.account': '帳號', 'nav.admin': '管理', 'nav.label': '主選單',
    'lang.label': '語言',
    'top.get': '下載', 'top.sponsor': '贊助', 'top.signout': '登出',
    'top.signin': '登入', 'top.more': '更多', 'top.myaccount': '我的帳號', 'top.account': '帳號選單', 'top.menu': '選單',
    'top.getnote': '需要 Wynntils 與 Fabric API，jar 放進 mods 資料夾就好。<a href="/#install">安裝說明</a>',
    'foot.label': '頁尾',
    'foot.tagline': 'Wynncraft 的多語言翻譯模組。原文留著，譯文就在旁邊。',
    'foot.privacy': '隱私政策', 'foot.terms': '服務條款',
    'privacy.title': '隱私政策 - WynnChaYuan', 'privacy.h1': '隱私政策',
    'terms.title': '服務條款 - WynnChaYuan', 'terms.h1': '服務條款',
    'legal.updated': "生效日期：2026 年 10 月 30 日",
    'privacy.body': "<p>這份說明講的是 WynnChaYuan 的三個部分會碰到哪些資料：Minecraft 模組 WynnChaYuan、這個網站 wynnchayuan.com，以及 Discord 機器人。WynnChaYuan 是玩家做的非官方專案，由 WynnChaYuan 團隊（聯絡人 LyuChaCha）維護。</p><h2>重點</h2><ul><li>只記功能需要的東西，也只留到用不到為止。我們<b>絕不販賣</b>你的資料，也不拿去做廣告或行銷。</li><li>模組第一次啟動時會先問你同不同意連線。不同意的話，檢查更新與遊戲內更新、從 GitHub 同步譯文與貢獻者名單、報到、葉子與徽章、披風、網站登入與 /wcy link 都不會開；翻譯照樣能用（模組內附的譯文檔，以及之前同步過的檔案）。之後可以在 F6「功能」→「隱私與連線」的「連線功能」更改。接著會讓你選翻譯要怎麼顯示：「推薦設定」、「自訂」或「保留目前的設定」。</li><li>你隨時可以自己刪除資料：在<a href=\"/account\">帳號頁</a>，或在模組裡按 F6「功能」→「隱私與連線」的「刪除我的資料」（見下面〈刪除你的資料〉）。</li><li>我們的程式不記 IP、所在的世界、聊天或任何遊戲內容。</li></ul><h2>模組的每一項連線功能送出什麼</h2><ul><li><b>檢查更新</b>：從 GitHub（或它在 jsDelivr 的鏡像）下載 <code>version.json</code>，看有沒有新版。除了一般的網路請求，不送任何關於你的東西。「直接更新」只有你按了才會從 GitHub Releases 下載新版。</li><li><b>譯文同步</b>：從 GitHub（或 jsDelivr）下載較新的譯文檔與貢獻者名單。只下載，不送任何關於你的東西。</li><li><b>報到（葉子、徽章、誰有裝模組）</b>：你進 Wynncraft 時，向 wynnchayuan.com 送出你的 Minecraft UUID、Mojang 簽過的公開個人檔案（名字與外觀）、用來證明帳號是本人的公鑰與簽章、要不要顯示披風，以及徽章的設定（葉子要不要彩色、展示哪幾個身分）。私鑰與登入憑證不會離開遊戲。</li><li><b>披風</b>：上傳時把你選的圖送到 wynnchayuan.com。要顯示披風時，從 wynnchayuan.com 下載其他玩家的披風、從 GitHub（或 jsDelivr）下載團隊的披風；下載不送任何關於你的東西。</li><li><b>網站登入與綁定</b>：從 F6 開披風一覽時，模組拿它的通行證換一條一天有效的登入網址；<code>/wcy link</code> 送出網站給你的綁定碼。</li><li><b>不會自己送出</b>：遊戲裡的文字。「匯出未翻譯字串」產生的檔案留在你的電腦上，要不要交給我們由你決定。</li></ul><h2>網站帳號、協作翻譯、回饋與機器人</h2><ul><li><b>網站帳號</b>：Discord 登入只要 <code>identify</code> 權限。我們記下 Discord 的使用者代號與顯示名稱；不拿 email、不拿你加入的伺服器，Discord 給的存取憑證用完就丟。在遊戲裡打 <code>/wcy link</code> 綁定之後，帳號會多記遊戲的 UUID、名字與綁定時間。</li><li><b>協作翻譯</b>：提案、投票與術語投票會記下你的顯示名稱與代號、時間、你寫的內容，其他人看得到。定案的譯文會寫進公開的 GitHub 倉庫（CC BY-NC-SA 4.0），留在它的歷史裡。</li><li><b>回饋</b>：記下你的 Discord 使用者代號與顯示名稱、分類、你寫的內容（含「在哪裡」與語言）、時間，以及從網站還是機器人送的。只有開發者與管理員看得到。譯文建議另外會變成公開的 GitHub Issue，上面只有你的顯示名稱（不含使用者代號）。</li><li><b>Discord 機器人</b>：只收到你打的斜線指令，以及 Discord 一定會送的欄位（你的使用者代號、伺服器與頻道的代號、介面語言）。它讀不到聊天；除了用 /回饋 送出的回饋之外什麼都不留。</li></ul><h2>誰看得到你的披風</h2><ul><li><b>公開</b>：每個人都能在披風一覽看到。</li><li><b>不公開</b>：不在披風一覽上；只有你和拿到連結的人看得到，拿到連結的人也可以穿上它（這不會讓它出現在一覽上）。</li><li><b>私人</b>：網站上只有你自己看得到。</li><li>不管選哪一種，管理的人都看得到每一件披風；遊戲裡裝了模組的其他玩家也照樣看得到你穿著的披風。一覽上不會出現你的 UUID 或名字，只有從 UUID 雜湊出來的代碼；誰傳了哪一件只有管理的人看得到。你的使用紀錄與最愛只有你自己看得到。</li></ul><h2>存在哪裡</h2><p>伺服器記的東西都存在 Cloudflare Workers 的 KV。Cloudflare 為了安全與網路運作會處理連線資訊（包含 IP），依它自己的隱私政策；我們的程式不存這些。</p><h2>保留多久</h2><ul><li>報到紀錄：最後一次報到之後 14 天。</li><li>你的披風與它的設定：留到你移除或刪除資料為止。沒有人穿的披風留 7 天後刪除；管理的人移除的馬上刪。</li><li>使用紀錄（最多 8 件）與最愛：留到你拿掉、那件披風被刪除，或你刪除資料為止。</li><li>最近幾次上傳的時間（擋短時間大量上傳）：最多 13 小時。</li><li>通行證：遊戲的 7 天，從遊戲開的網頁 1 天，Discord 登入 30 天。綁定碼 10 分鐘。</li><li>網站帳號與綁定：留到你刪除資料或解除綁定為止。</li><li>回饋：標成已處理之後 180 天刪除；還沒處理的留到處理或刪除為止。擋濫發用的送出時間 1 小時。</li><li>替 Discord 機器人畫的「你的外觀穿著披風」預覽：3 天。</li><li>封禁與審核紀錄：留到管理的人解除為止。</li></ul><h2>刪除你的資料</h2><p>在<a href=\"/account\">帳號頁</a>按「刪除我的資料」，或在模組裡按 F6「功能」→「隱私與連線」的「刪除我的資料」（要按兩次才會送出）。兩邊送的是同一個請求（<code>POST /api/me/delete</code>），馬上開始刪；資料多的時候會分幾次刪完，帳號頁會自己接著送。模組那一邊還會停止報到、葉子與披風的上傳，每次啟動都重送，直到伺服器回覆全部刪完，就算你沒有同意連線功能也照樣送出。之後想再上傳，按同一處的「恢復上傳」。</p><ul><li><b>會刪掉</b>：報到紀錄（馬上從玩家名單上消失）、你的披風與設定、只有你用的圖（馬上刪，不等 7 天）、使用紀錄與最愛、網站帳號與 Discord 綁定、你送過的回饋，以及用你的外觀畫的披風預覽。</li><li><b>會留著，以及原因</b>：封禁與審核紀錄、管理的人維護的名單（特別名單、管理員、譯者），刪資料不能拿來撤銷管理的決定；其他玩家還穿著的圖；最近幾次上傳的時間（最多 13 小時），刪資料不能拿來跳過上傳冷卻；你在協作翻譯的提案與投票，那是公開專案的一部分：上面的名字會改成「已刪除的使用者 #代碼」（代碼從你的帳號算出來，同一個人的紀錄才分得出來），你的遊戲 UUID 或 Discord 代號留在那些紀錄上、只有開發者與管理員看得到，用來記錄是誰貢獻的；已經定案寫進倉庫歷史的譯文不會改，GitHub 上的 Issue 也留著。</li><li>只在網站上刪除的話，模組下次進 Wynncraft 會再報到一次；不想要的話，在模組裡按「刪除我的資料」，或關掉「連線功能」。</li></ul><h2>Cookie 與瀏覽器裡存的東西</h2><ul><li><code>lang</code>：記住你選的網站語言</li><li><code>wcy_oauth</code>、<code>wcy_intent</code>：Discord 登入時的驗證，只在登入的那幾分鐘有用</li><li>瀏覽器的 <code>localStorage</code>：登入的通行證與畫面上的小設定</li></ul><p>沒有廣告，也沒有分析或追蹤用的 Cookie。</p><h2>第三方服務</h2><ul><li><b>Cloudflare</b>：放網站與資料</li><li><b>Discord</b>：登入與機器人</li><li><b>GitHub</b>：原始碼、譯文、更新檔、協作翻譯的 PR，以及譯文建議的 Issue</li><li><b>jsDelivr</b>：模組下載 GitHub 檔案時的鏡像，以及瀏覽器載入的披風 3D 預覽程式</li><li><b>minotar.net</b>：網頁上的玩家頭像由你的瀏覽器直接向它讀取；Discord 機器人的披風預覽由網站向它讀取那位玩家的外觀</li></ul><h2>年齡</h2><p>使用這些服務需要符合 Minecraft 與 Discord 對年齡的要求。</p><h2>修改</h2><p>這份說明改了會更新這一頁與上面的日期；重大的改動會在 Discord 或更新日誌公告。</p>",
    'terms.body': "<p>使用 WynnChaYuan 模組、wynnchayuan.com 網站或 Discord 機器人，就表示你同意以下條款。不同意的話請不要使用。</p><h2>非官方專案</h2><p>WynnChaYuan 是玩家做的非官方專案，跟 Wynncraft、Mojang、Microsoft、Discord 都沒有關係，也沒有得到它們的認可。網站上的遊戲材質與畫面屬於 Wynncraft 及各自的權利人。</p><h2>授權</h2><p>模組的程式碼以 AGPL-3.0 授權；譯文與語料以 CC BY-NC-SA 4.0 授權。詳見 GitHub 倉庫的 LICENSE 與 LICENSE-DATA。</p><h2>帳號</h2><ul><li>一個網站帳號綁一個遊戲帳號，綁定一定要從遊戲裡做</li><li>不可以冒用別人的帳號、繞過身分驗證，或把通行證交給別人使用</li><li>違反條款的帳號可能被停用</li><li>你隨時可以在帳號頁刪除自己的帳號與資料；哪些會留著，見<a href=\"/privacy\">隱私政策</a></li></ul><h2>披風的規範</h2><p>披風會被遊戲裡的其他玩家看到；網站上誰看得到，照你選的公開、不公開或私人。上傳的圖不可以包含：</p><ul><li>政治宣傳</li><li>色情、猥褻或血腥的內容</li><li>違法的內容，或侵害他人著作權、商標的圖</li><li>種族歧視、仇恨或騷擾他人的內容</li><li>廣告（宣傳公會除外）</li></ul><p>管理的人可以審核、移除違規的披風，或封禁上傳的人，不另行通知。圖的權利仍是你的；上傳即表示你同意我們保存它、展示給遊戲裡的其他玩家，並照你選的公開程度展示給網站訪客。</p><h2>協作翻譯</h2><ul><li>提交的譯文必須是你有權提供的內容，並同意以 CC BY-NC-SA 4.0 授權</li><li>請照術語表與各語言團隊的決定翻譯；開發者與管理員有最後的決定權</li><li>不要灌水、刷票或提交無關的內容</li></ul><h2>禁止的行為</h2><ul><li>大量或自動化地呼叫網站的 API，或干擾服務運作</li><li>嘗試取得不屬於你的資料或權限</li><li>濫用 Discord 機器人</li></ul><h2>贊助</h2><p>透過 Ko-fi 的贊助是自願的支持。贊助者的回饋（例如 GIF 披風）可能隨專案調整。</p><h2>遊戲與第三方</h2><p>模組只改變你畫面上顯示的文字與外觀，不改變遊戲玩法。使用任何模組都請自行遵守 Wynncraft 與 Minecraft 的規則。第一次帶著模組進 Wynncraft 時會顯示「使用須知」（勾選「不再自動顯示」之前，每次啟動都會顯示）：譯文只在你自己的畫面上，跟其他玩家交流時請用原文。</p><h2>不提供保證</h2><p>這些服務照現況提供，可能有錯誤、中斷或停止服務。在法律允許的範圍內，我們不對使用這些服務造成的損失負責。</p><h2>修改與終止</h2><p>我們可能修改這份條款，或停止部分甚至全部服務。修改後會更新這一頁與上面的日期；之後繼續使用，即表示同意新的條款。</p><h2>聯絡</h2><p>有問題請在 Discord 聯絡 <b>LyuChaCha</b>，或在 GitHub 開 Issue。</p>",
    'foot.get': '下載', 'foot.project': '專案', 'foot.support': '支持', 'foot.contact': '聯絡',
    'foot.changelog': '更新日誌', 'foot.license': '程式授權：AGPL-3.0', 'foot.datalicense': '譯文授權：CC BY-NC-SA 4.0',
    'foot.issues': '回報問題', 'foot.contribute': '怎麼參與', 'foot.team': '團隊', 'foot.sponsoring': '關於贊助',
    'foot.kofi': '在 Ko-fi 贊助', 'foot.sitedev': '網站開發者：LyuChaCha', 'foot.discord': 'Discord：LyuChaCha',
    'foot.copy': '複製 Discord 名稱',
    'foot.unofficial': '玩家自製的模組，與 Wynncraft、Mojang、Microsoft 沒有關係。',
    'foot.assets': '預覽裡的遊戲材質與圖示屬於 Wynncraft。',

    'home.title': 'WynnChaYuan - Wynncraft 的多語言翻譯模組',
    'home.h1': '把 Wynncraft 翻成你的語言',
    'home.lede': 'WynnChaYuan 是 Wynncraft 的多語言翻譯模組（Fabric 1.21.11）。原文保留，譯文顯示在旁邊，或直接寫進原本的位置——任務、對話、物品與介面都看得懂。',
    'home.github': '從 GitHub 下載',
    'home.langs': '支援的語言',
    'home.what': '翻了什麼',
    'home.f1': '物品與裝備', 'home.f1b': '裝備敘述、屬性、Major ID。旁邊另開面板，或寫進原本的說明框。',
    'home.f2': '任務與對話', 'home.f2b': '寫進 Wynncraft 自己的對話框，逐字打出的節奏跟原文同步；任務追蹤也有。',
    'home.f3': '技能樹', 'home.f3b': '五個職業的節點、說明與流派。',
    'home.f4': '聊天與提示', 'home.f4b': '伺服器發的訊息：任務完成、獎勵、進出區域。玩家的發言不翻。',
    'home.f5': '選單與介面', 'home.f5b': '交易市場、公會、商城，還有掠奪長征、討伐戰、地城的面板。',
    'home.f6': '譯文自動更新', 'home.f6b': '新的翻譯合併之後，下次進遊戲就生效，不必更新模組。',
    'home.install': '安裝',
    'home.need': '需要 <a href="https://modrinth.com/mod/wynntils">Wynntils</a> 4.2 以上與 <a href="https://modrinth.com/mod/fabric-api">Fabric API</a>。純客戶端，伺服器不需要裝。',
    'home.how': '把 jar 放進 mods 資料夾，進遊戲按 F6 開設定。',
    'home.about': '<b>關於譯文。</b>部分譯文由人工輸入再經 AI 轉出，一小部分直接由 AI 翻譯，大部分基礎內容經過人工校稿。其他語言目前多半是圍繞著繁體中文再做 AI 翻譯，可能有錯譯與用詞不一致。遇到問題請到 <a href="' + ISSUES + '">GitHub Issues</a> 回報。',
    'home.morefeatures': '看全部功能',
    'home.team': '團隊',
    'home.teamlede': '做出 WynnChaYuan 的人。葉子的顏色代表身分，跟遊戲裡名字前面那一片一樣；點一下可以看他在 Wynncraft 的資料。',
    'home.guilds': '貢獻公會', 'home.guildsb': '對模組提供巨大幫助，或成員之間互相協助的公會。',
    'home.teamfail': '名單現在讀不到，請重新整理。',
    'home.teampick': '依翻譯團隊篩選',
    'home.dev': '開發者', 'home.devb': '協助程式開發與維護。',
    'home.sponsor': '贊助者', 'home.sponsorb': '單次 20 USD 或每月 2 USD 以上。',
    'home.contrib': '貢獻者', 'home.contribb': '替自己的語言翻譯、修正錯誤，或提供程式上的實質協助。',
    'home.user': '裝了模組的人', 'home.userb': '名字前面是一片灰色的葉子。',
    'home.support': '關於贊助',
    'home.free': '<b>翻譯免費，也會一直免費。</b>所有譯文與翻譯功能都由團隊無償提供，不存在「付費才有翻譯」這回事。贊助完全出於自願：',
    'home.sup1': '<b>不贊助，翻譯品質不會打折。</b>每個人拿到的譯文、更新與修正都是同一份，不會因為沒贊助而變少或變慢。',
    'home.sup2': '<b>贊助者多的只有趣味性的東西：</b>名字前面的青藍色葉子、列在名單上，以及動態（GIF）披風與比較大的檔案上限。',
    'home.sup3': '<b>贊助者的意見會優先採納：</b>回報與建議我們會優先看、優先處理。',
    'home.seecapes': '看看大家的披風', 'home.donate': '贊助', 'home.join': '怎麼參與',

    'capes.title': '披風一覽 - WynnChaYuan',
    'capes.h1': '披風一覽',
    'capes.lede': 'WynnChaYuan 玩家上傳的披風。點一件可以轉著看，登入之後還能直接穿上。',
    'capes.mine': '上傳我的披風',
    'capes.sort': '排序', 'capes.filter': '篩選', 'capes.f.format': '格式', 'capes.f.show': '顯示',
    'capes.s.new': '最新上傳', 'capes.s.old': '最早上傳', 'capes.s.worn': '最多人使用', 'capes.s.stars': '人氣最高', 'capes.s.recent': '最近有人穿上',
    'capes.f.all': '全部', 'capes.f.fav': '我的最愛', 'capes.f.hist': '我用過的', 'capes.f.hidden': "不公開", 'capes.f.pending': '待審核', 'capes.f.private': "私人",
    'rules': '<b>不可以上傳：</b>政治、不雅或色情、違反法規、種族歧視或仇恨的內容；廣告（商業宣傳、宣傳其他伺服器、真錢交易例如用現金買賣 LE、現實生活的聯絡方式；公會宣傳不算）；以及其他違反 Wynncraft 規章的內容。也不可以濫用上傳（例如短時間內大量上傳）。違規的披風會被移除，上傳的人可能不能再傳。',
    'capes.keep': '沒有人穿的披風會保留 7 天；期滿還是沒有人穿就會移除，加了星號的也一樣。',
    'skip': '跳到主要內容',
    'capes.make': '做自己的披風', 'capes.makeb': '下載範本、畫上你的圖案、上傳，三步就好。',
    'tpl.h': '披風範本',
    'tpl.p': '披風要照範本畫。下載一張範本，把藍色（正面）、紅色（背面）與青色（邊框）的地方換成你的圖案，存成 PNG 或 GIF 再上傳。',
    'tpl.pick': '下載範本（選一個尺寸）', 'tpl.rec': '建議',
    'tpl.note': '尺寸越大畫得越細，檔案也越大。不確定就用 88×68。',
    'tpl.example': '下載會動的示範（GIF）', 'tpl.upload': '畫好了？到帳號頁上傳',
    'tpl.alt': '披風範本的說明圖：哪一塊是正面、背面與邊框',
    'lost.title': '找不到這一頁 - WynnChaYuan', 'lost.h1': '找不到這一頁',
    'lost.p': '網址可能打錯了，或是這一頁已經搬走。', 'lost.home': '回首頁',
    'home.live': '遊戲裡實際的樣子：原文留著，譯文在旁邊。', 'home.try': '自己調調看',
    'view.title': '披風', 'view.flat': '披風的範本圖：左邊正面、右邊背面',
    'view.type': '格式', 'view.dim': '尺寸', 'view.size': '檔案大小', 'view.users': '使用', 'view.date': '上傳',
    'view.stars': '加入最愛', 'view.approve': '通過審核', 'view.review': '上傳的人之後都要審核', 'view.forget': '從我的紀錄移除', 'view.share': "複製連結",
    'view.del': '刪除這件披風', 'view.ban': '刪除並封禁上傳的人', 'view.close': '關閉',
    'view.wear': '穿上這件披風', 'view.signin': '登入並綁定遊戲 ID 之後，就能穿上這一件。<a href="/account">前往帳號</a>',

    'account.title': '帳號 - WynnChaYuan',
    'account.h1': '帳號',
    'account.lede': '先用 Discord 註冊，再到遊戲裡綁定你的遊戲 ID；或是直接從遊戲裡開這個網站。',
    'account.discord.h': '用 Discord 註冊／登入',
    'account.discord.p': '網站帳號用 Discord 登入，我們只讀你的 Discord 名稱。登入之後到遊戲裡綁定遊戲 ID，就能上傳披風；之後不必開遊戲也登得進來。',
    'account.discord.btn': '用 Discord 登入',
    'account.discord.off': '這個網站還沒有開放 Discord 登入，請先從遊戲裡開。',
    'account.discord.err': 'Discord 登入沒有成功，請再試一次。',
    'account.discord.hint': '綁定 Discord 之後，不必開遊戲也能用 Discord 登入這個網站。',
    'account.discord.bind': '綁定 Discord',
    'account.link.h': '綁定遊戲 ID',
    'account.link.p': '網站沒辦法自己確認遊戲帳號是不是你的，所以綁定要從遊戲裡做：先拿一組綁定碼，再到遊戲裡輸入。',
    'account.link.get': '取得綁定碼',
    'account.link.type': '進 Wynncraft，在聊天欄輸入這一行（十分鐘內有效，要裝 WynnChaYuan）：',
    'account.link.copy': '複製',
    'account.link.wait': '輸入之後這一頁會自己更新。',
    'account.unlink': '解除 Discord 綁定',
    'account.how': '或是從遊戲裡直接開',
    'account.s1': '進 Wynncraft，按 <b>F6</b> 開 WynnChaYuan 的設定。',
    'account.s2': '到「<b>功能</b>」那一頁。',
    'account.s3': '按「<b>披風一覽</b>」旁邊的「打開」。瀏覽器開起來的時候就是登入的。',
    'account.expire': '登入一天有效，過期了從遊戲裡再開一次。離線帳號沒辦法登入。',
    'account.banned': '這個帳號不能上傳披風。',
    'account.signout': '登出這個瀏覽器',
    'account.mine': '我的披風', 'account.none': '還沒有上傳。', 'account.remove': '移除我的披風',
    'account.worn': '這一件是從披風一覽套用的。',
    'account.vis': "誰看得到這件披風",
    'account.vis.public': "公開",
    'account.vis.unlisted': "不公開",
    'account.vis.private': "私人",
    'account.vis.game': "這只影響網站：遊戲裡裝了模組的其他玩家照樣看得到你穿著它。",
    'account.share': "分享連結：拿到這條連結的人就看得到這件披風。",
    'account.del.h': "刪除我的資料",
    'account.del.p': "把你在伺服器上的資料全部刪掉：報到紀錄、披風、使用紀錄與最愛、網站帳號與綁定、送過的回饋。封禁與審核紀錄會留著，詳見<a href=\"/privacy\">隱私政策</a>。",
    'account.del.btn': "刪除我的資料",
    'account.del.list': "<p>確定要刪除嗎？刪了就不能復原。</p><ul><li><b>會刪掉</b>：報到紀錄、你的披風與設定、只有你用的圖、使用紀錄與最愛、網站帳號與 Discord 綁定、你送過的回饋。</li><li><b>會留著</b>：封禁與審核紀錄、其他玩家還穿著的圖、最近幾次上傳的時間（最多 13 小時）、協作翻譯的提案與投票（名字改成「已刪除的使用者 #代碼」，帳號代號只有管理的人看得到）。</li><li>模組下次進 Wynncraft 會再報到一次；不想要的話，在模組的 F6「功能」→「隱私與連線」按「刪除我的資料」，它會一併停止報到與上傳（之後可以按「恢復上傳」）。</li></ul>",
    'account.del.cancel': "取消",
    'account.del.go': "確定刪除",
    'account.del.done': "你的資料已經刪除，這個瀏覽器也登出了。",
    'account.pending': '這件披風正在等審核，通過之前別人看不到。',
    'account.review': '你上傳的披風目前要先經過審核，通過之後別人才看得到。審核期間只能有一件上傳的披風（等審核的也算）；要換一件，先移除現在這件。',
    'account.upload': '上傳披風',
    'account.spec': '要照<a href="' + TEMPLATES + '">範本</a>畫：' + SIZES + '。檔案大小：一般玩家 1 MB 以內，開發者、贊助者、貢獻者 20 MB 以內。PNG 大家都能傳；GIF（最多 120 格）只有開發者、贊助者、貢獻者能傳。一小時內上傳 3 次以上，要等 12 小時才能再傳。',
    'account.pick': '選一張披風的圖', 'account.send': '上傳',

    'admin.title': '管理 - WynnChaYuan',
    'nav.translate': '協作翻譯', 'tr.title': '協作翻譯 - WynnChaYuan', 'tr.h1': '協作翻譯',
    'tr.lede': '搜尋你在遊戲裡看到的句子，提出更好的譯法，或對有爭議的術語投票。各語言的譯者可以提案與投票，管理員定案後會送進語料。',
    'tr.lang': '語言', 'tr.tab.browse': '瀏覽與提案', 'tr.tab.props': '整句提案', 'tr.tab.terms': '術語投票',
    'tr.q': '搜尋原文或譯文（至少兩個字）', 'tr.allfiles': '所有檔案', 'tr.blank': '只看還沒翻的',
    'tr.props.note': '每一句的提案與票數。管理員採用的句子，同一個語言同一天的會放進同一個 PR。',
    'tr.terms.note': '同一個詞有不同譯法時在這裡投票。定案之後，由維護的人把它套到整份語料。',
    'tr.st.open': '投票中', 'tr.st.accepted': '已採用', 'tr.st.rejected': '已駁回',
    'tr.np.term': '英文術語', 'tr.np.opts': '候選譯法（用逗號分開）', 'tr.np.note': '說明（可以不填）：出現在哪裡、為什麼有不同意見', 'tr.np.send': '開始投票',
    'admin.h1': '管理',
    'admin.gate': '這一頁只有開發者與管理員能用，請先登入。',
    'admin.lede': '披風的刪除與封禁在<a href="/capes">披風一覽</a>：點開一件，就看得到是誰傳的。',
    'admin.bans': '封禁名單',
    'admin.player': '玩家', 'admin.when': '封禁的時間', 'admin.by': '誰封的',
    'admin.nobans': '目前沒有人被封禁。',
    'admin.add': '新增成員',
    'admin.addnote': '送出之後會在 GitHub 開一個 PR 改 <code>credits.json</code>，合併之後玩家下次同步名單就生效（名牌的葉子、傳 GIF 披風的資格）。',
    'admin.section': '身分', 'admin.name': '顯示的名字', 'admin.mc': 'Minecraft ID', 'admin.color': '葉子顏色（可以不填）',
    'admin.contributors': '貢獻者', 'admin.sponsors': '贊助者', 'admin.developers': '開發者',
    'admin.send': '開 PR',
    'admin.lt': '翻譯團隊', 'admin.ltnote': '一個人負責哪些語言，在這裡一次管好。勾一個語言等於兩件事：他在「協作翻譯」頁可以替那個語言提案與投票（馬上生效），也列進名冊（<code>credits.json</code>）那個語言的翻譯團隊（放進當天的 PR，合併之後首頁跟著變）。拿掉語言兩邊一起拿掉。不在名冊上的譯者只有權限，按「加進名冊」才能登記團隊與貢獻項目；名冊上還沒有權限的人只列出團隊。兩邊對不上的會標「不一致」，選一邊為準再儲存，不會自動同步。', 'admin.ltlangs': '語言（譯者權限與翻譯團隊）', 'admin.ltsave': '儲存', 'admin.ltjoin': '加進名冊', 'admin.ltrights': '譯者權限', 'admin.ltteams': '名冊的翻譯團隊', 'admin.nolt': '還沒有成員或譯者。',
    'admin.tmember': '成員', 'admin.tareas': '貢獻項目',
    'account.drop': '也可以把圖檔拖到這裡。尺寸對的話，上面的人偶會先穿給你看。',
    'admin.reviews': '審核名單',
    'admin.reviewsnote': '比封禁輕的處置：名單上的人還是可以上傳，但之後傳的披風要先由管理的人在披風一覽按「通過審核」，別人才看得到（一覽與遊戲裡都是）。他現在身上那一件不受影響。等審核的披風在披風一覽的「待審核」。輸入他的 Minecraft UUID；也可以在披風一覽點開一件，直接把上傳的人列進來。',
    'admin.noreviews': '沒有人在審核名單上。',
    'admin.perks': '特別名單',
    'admin.perksnote': '名單上的人可以上傳 GIF 披風、檔案上限 20 MB，跟開發者、贊助者、貢獻者一樣，但不會出現在貢獻者名單上，名字前面也不會多一片葉子。輸入他的 Minecraft UUID，有沒有連字號都可以；加了馬上生效。',
    'admin.uuid': 'Minecraft UUID', 'admin.note': '備註（可以不填）', 'admin.grant': '加入',
    'admin.noperks': '名單是空的。', 'admin.pwhen': '加入的時間', 'admin.pby': '誰加的',
    'admin.staff': '管理員',
    'admin.staffnote': '管理員可以做開發者在這個網站上做的事：刪除披風、封禁與解除封禁、管理特別名單、替名單開 PR。差別是不會出現在團隊名單上、名字前面沒有葉子，也不能新增或移除管理員。',
    'admin.staffhow': '點開下拉選單，從已註冊成員（登入過網站或傳過披風的人）裡選一位；可以打 Discord 名稱、Minecraft ID、UUID 或 Discord 使用者 ID 篩選。有綁遊戲帳號的會記成 UUID，用遊戲或 Discord 登入都有權限。還沒註冊的人也可以直接打完整的 Minecraft ID 或 Discord 使用者 ID 再按加入，用 Minecraft ID 加的等他登入一次就會改記成 UUID。加了馬上生效。',
    'admin.who': '選擇成員', 'admin.findph': '點一下展開，或打字篩選…',
    'admin.kind': '類型', 'admin.id': '代號', 'admin.nostaff': '還沒有管理員。',
    'admin.bot': 'Discord 機器人', 'admin.botreg': '註冊指令', 'admin.botinvite': '邀請到伺服器',
    'admin.botnote': '斜線指令 /status、/cape、/translate、/feedback（繁中介面是 /回饋）由這個網站回應。部署之後，到 Discord Developer Portal 把下面的 Interactions Endpoint URL 填進去，再按「註冊指令」；指令改了要再按一次。Bot Token 用 <code>wrangler secret put DISCORD_BOT_TOKEN</code> 設定，不要貼在任何網頁上。',
    'admin.guilds': '貢獻公會',
    'admin.guildsnote': '會出現在首頁「團隊」那一段，點下去連到那個公會在 Wynncraft 的頁面。先加名字（要跟 Wynncraft 上的一模一樣），再各上傳一張圖示與旗幟。圖會先縮小再傳，PNG、JPEG、WebP 都可以。',
    'admin.gname': '公會名稱', 'admin.gtag': '標籤（可以不填）', 'admin.gpics': '圖示與旗幟', 'admin.guild': '公會',
    'admin.noguilds': '還沒有登記公會。',
    'admin.feedback': '回饋',
    'admin.feedbacknote': '網站的回饋頁與 Discord 的 /回饋 送來的都在這裡，新的在最上面。處理完按「標成已處理」，已處理的 180 天後自動刪除；垃圾訊息直接刪除。譯文建議會自動在 GitHub 開 Issue，沒開成的會標出原因，可以按「再開一次 Issue」。',
    'admin.fbst': '狀態', 'admin.fball': '全部', 'admin.fbopen': '待處理', 'admin.fbdone': '已處理', 'admin.nofeedback': '沒有符合的回饋。',

    'nav.feedback': '回饋', 'foot.feedback': '回饋與建議',
    'fb.title': '回饋與建議 - WynnChaYuan', 'fb.h1': '回饋與建議',
    'fb.lede': '譯文哪裡怪怪的、想多翻哪些內容、網站哪裡不好用、想要什麼功能，都可以告訴我們。也可以在 Discord 用機器人的 <code>/回饋</code>。',
    'fb.gate': '送出回饋要先用 Discord 登入，我們才知道是誰寫的。<a href="/account">前往帳號頁登入</a>',
    'fb.gatelink': '你是從遊戲裡開這個網站的，還沒綁 Discord。到帳號頁按「綁定 Discord」，或改用 Discord 登入，就能送出回饋。<a href="/account">前往帳號頁</a>',
    'fb.cat': '分類', 'fb.cat.translation': '譯文建議', 'fb.cat.content': '內容建議', 'fb.cat.website': '網站建議',
    'fb.cat.feature': '功能建議', 'fb.cat.other': '其他',
    'fb.lang': '哪一種語言的譯文', 'fb.msg': '想說的話', 'fb.where': '在哪裡（可以不填）',
    'fb.wherehint': '例如任務或 NPC 的名字、哪個畫面、哪一頁，或直接貼上那一句英文原文。',
    'fb.send': '送出',
    'fb.note': '一個人一小時最多送 5 則。<b>譯文建議會自動在 GitHub 開一個公開的 Issue</b>，上面會顯示你的 Discord 顯示名稱（不會顯示使用者代號）；其他分類只有開發者與管理員看得到。詳見<a href="/privacy">隱私政策</a>。',
    'fb.bugs': '遇到當機、畫面壞掉這類程式錯誤，請到 <a href="' + ISSUES + '">GitHub Issues</a> 回報，附上 log 與截圖會比較快找到原因。',

    'features.title': '功能 - WynnChaYuan',
    'features.h1': '功能',
    'features.lede': 'WynnChaYuan 把 Wynncraft 畫面上的英文換成你的語言，原文要不要留由你決定。先動動看下面的預覽，再往下是翻了哪些地方、附帶哪些工具。',
    'demo.label': '即時預覽',
    'play.pages': '設定的分類',
    'play.note': '左邊就是遊戲裡設定畫面的選項（在聊天輸入 <code>/wcy</code> 打開），預覽裡的譯文直接取自模組的語料。',
    'features.nav': '這一頁的內容',
    'features.t': '翻譯範圍', 'features.u': '附帶的工具', 'features.s': '葉子與披風', 'features.c': '相容的模組',
    'features.tnote': '裝備名稱預設保留原文：那是專有名詞，交易市場與 wiki 都用英文。想看譯名可以在設定裡打開。',
    'features.cnote': '沒列在上面的模組多半也能一起用：WynnChaYuan 只在你的電腦上換畫面上的字，伺服器不需要裝。',
    'features.install': '安裝說明',
    'feat.required': '必要', 'feat.optional': '選裝', 'feat.compatible': '相容',
    'feat.t1': '物品說明', 'feat.t1b': '裝備敘述、屬性、Major ID。旁邊另開面板，或寫進原本的說明框。',
    'feat.t2': '技能樹', 'feat.t2b': '五個職業的節點、說明與流派。',
    'feat.t3': '任務對話與選項', 'feat.t3b': '寫進 Wynncraft 自己的對話框，框、名牌、頭像都不動，逐字打出的節奏跟原文同步；也可以另開小框。',
    'feat.t4': '任務追蹤', 'feat.t4b': '獨立的小框，抬頭照實際追蹤的東西顯示：任務、世界事件、洞窟、討伐戰。',
    'feat.t5': 'NPC 名牌與漂浮字', 'feat.t5b': '看著它的時候跳出小框，或就地取代；工作站與互動提示也算。',
    'feat.t6': '聊天訊息', 'feat.t6b': '伺服器發的訊息：任務完成、獎勵、進出區域。分欄的訊息保持對齊；玩家的發言不翻。',
    'feat.t7': '選單與介面', 'feat.t7b': '交易市場、公會、商城等畫面。',
    'feat.t8': '掠奪長征、討伐戰、地城', 'feat.t8b': '使命、加持、信標、Aspect、Gambit，還有結算與戰利品的面板。',
    'feat.t9': '探索點與祕密發現', 'feat.t9b': '名稱、說明與故事。',
    'feat.t10': '畫面中央的大字', 'feat.t10b': '標題與副標題。',
    'feat.u1': '譯文自動更新', 'feat.u1b': '新的翻譯合併之後，下次進遊戲就生效，不必更新模組。離線時用上次抓到的那一份。',
    'feat.u2': '市集搜尋', 'feat.u2b': '在交易市場用你的語言搜尋，送出前自動換回英文原名；打字的時候會列出候選。',
    'feat.u3': '名稱要不要翻', 'feat.u3b': '任務、洞窟、探索點這些內容的名稱，可以選譯名、譯名加原文，或保留原文。',
    'feat.u4': '輔助語言', 'feat.u4b': '你的語言還沒翻到的句子，可以改顯示另一種語言，或留著原文。',
    'feat.u5': '面板隨你擺', 'feat.u5b': '小框都能拖曳定位；對話、選項、任務追蹤還能拉右下角改大小。',
    'feat.u6': '複製聊天與譯文截圖', 'feat.u6b': '列出最近的聊天訊息，點一則就複製；翻譯面板可以存成圖片。回報問題的時候很方便。',
    'feat.u7': '遊戲內更新', 'feat.u7b': '有新版的時候，在更新說明的畫面按一下就下載，關遊戲時自動換上。',
    'feat.u8': '設定', 'feat.u8b': '在聊天輸入 <code>/wcy</code> 或從 Mod Menu 打開。滑鼠移到項目上有說明，右邊的預覽會跟著你調的值變。',
    'feat.s1': '名字前面的葉子', 'feat.s1b': '裝了模組的玩家名字前面都有一片葉子，彼此看得到：開發者金色、贊助者青藍色、貢獻者綠色，其他人灰色。',
    'feat.s2': '披風', 'feat.s2b': '上傳自己的披風，其他裝了模組的玩家看得到；在披風一覽看到喜歡的，也可以直接穿上，隨時能復原。PNG 大家都能用；GIF 給開發者、贊助者、貢獻者。<a href="/capes">看看大家的披風</a>',
    'feat.s3': '會送出的資料', 'feat.s3b': '為了上面兩項，你同意連線功能之後，模組才會在你進 Wynncraft 的時候向 wynnchayuan.com 報到一次：UUID、公開的個人檔案，以及披風與徽章的設定。不送任何遊戲內容。<a href="' + REPO + '#readme">完整的說明在 README</a>',
    'feat.c1b': '物品、任務、聊天的資料都從它來；它自己的畫面也會翻。',
    'feat.c2b': '跟 Wynntils 一樣是前置。',
    'feat.c3b': '模組清單上多一顆設定按鈕。',
    'feat.c4b': '它的搜尋面板打譯名也找得到，結果在英文名旁邊附上譯名。',
    'feat.c5b': '它的價格框、獎勵畫面與設定畫面也翻成譯文；按住 Shift 看原文。',
    'feat.c6b': '它用來判斷討伐戰結果的那幾句話保留英文，連勝才不會算錯。',

    js: {
      loading: '讀取中…',
      count: '共 {n} 件（相同的圖算一件）', empty: '還沒有人上傳披風。', users: '{n} 人', wearing: '{n} 人',
      card: '{t} 披風，{n} 人使用',
      drag: '拖曳可以旋轉。', no3d: '立體檢視載不起來，只能看平面的圖。',
      skinPick: '人物的外觀', skinPlain: '無外觀',
      confirmDel: '刪除這件披風（{n} 個人在用）？',
      confirmBan: '刪除這件披風，並封禁用它的 {n} 個人（之後不能再上傳）？',
      expired: '沒有成功（{s}）。登入一天過期，請從遊戲裡重新開一次。',
      readFail: '讀不到清單，請重新整理。',
      roleDev: '開發者', roleMember: '名單上的成員', roleUser: '裝了模組的玩家',
      mineHint: '{t} · {d}{s} · 拖曳可以旋轉', mineNo3d: '立體檢視載不起來。',
      discordAs: 'Discord：{n}', codeFail: '拿不到綁定碼，請再試一次', copied: '已複製', vis: { public: "公開", unlisted: "不公開", private: "私人" }, visHint: { public: "每個人都能在披風一覽看到它，也能穿上、加進最愛。", unlisted: "不會出現在披風一覽上，只有你和拿到連結的人看得到；拿到連結的人也能穿上，但不能加進最愛。", private: "網站上只有你自己看得到（管理的人除外）。" }, linkGone: "找不到這件披風：可能已經移除，或擁有者把它設成私人。", wipeFail: "沒有刪除成功，請稍後再試。",
      confirmUnlink: '解除這個遊戲 ID 與 Discord 的綁定？',
      confirmRemove: '移除你上傳的披風？', removed: '披風拿掉了', tryLater: '沒有成功，等一下再試',
      pickFirst: '先選一張圖', uploading: '上傳中…', uploaded: '披風上傳好了（{w}×{h}），別人幾分鐘內就會看到',
      picked: '這張圖是 {w}×{h}，{s}', pickedBad: '這張圖是 {w}×{h}，不是範本的尺寸（' + SIZES + '）',
      bindFail: '沒有成功，請再試一次',
      failed: '沒有成功（{s}），等一下再試', offline: '連不上，等一下再試',
      why: {
        'too large': '檔案太大了（一般玩家 1 MB；開發者、贊助者、貢獻者 20 MB）',
        'bad size': '尺寸不對：要照範本畫，' + SIZES + ' 其中一種',
        'bad format': '這不是 PNG 或 GIF，或是檔案壞了',
        'too many frames': 'GIF 的格數太多了（最多 120 格）',
        'gif members only': 'GIF 只有開發者、贊助者、貢獻者能上傳；PNG 大家都可以',
        'not listed': '目前只開放名單上的成員上傳',
        banned: '這個帳號不能上傳披風',
        'uploads off': '現在沒有開放上傳',
        'slow down': '剛剛才傳過，等一下再試',
        cooldown: '一小時內已經傳了 3 件，{t} 之後才能再傳',
        'review one': '審核期間只能有一件上傳的披風（等審核的也算）；要換一件，先移除現在這件。',
        'login first': '登入過期了，請從遊戲裡重新開一次',
        'no such cape': '這件披風已經不在了，請重新整理',
        'nothing to restore': '沒有可以復原的披風',
        'in use': '現在穿著的那一件不能從紀錄移除',
        'too many': '我的最愛已經滿了（最多 200 件）',
        'no cape': '你還沒有披風',
      },
      favAdd: '加入我的最愛', favRemove: '從我的最愛移除',
      emptyFav: '還沒有加入最愛的披風。點披風右上角的星星就能加進來。', emptyHist: '還沒有使用紀錄。', emptyFilter: '沒有符合的披風。',
      tagPending: '待審核', tagMine: '我穿的', tagWorn: '套用',
      idleLeft: '沒人穿・剩 {n} 天', idleLast: '沒人穿・最後一天',
      idleNote: '現在沒有人穿這一件。{n} 天內沒有人穿上的話，它就會被移除。',
      idleNoteLast: '現在沒有人穿這一件。今天之內沒有人穿上的話，它就會被移除。',
      removedNote: '你加了星號或用過的披風裡，有 {n} 件已經移除了（超過 7 天沒有人穿）。',
      confirmReview: '把上傳這件披風的人列入審核名單？之後他們傳的披風要先通過審核，別人才看得到。',
      reviewed: '已列入審核名單（{n} 人）', reviewedNone: '上傳的人已經在審核名單上了', approved: '已通過審核',
      confirmForget: '把這一件從你的使用紀錄移除？', forgot: '已從紀錄移除',
      uploadedPending: '披風上傳好了（{w}×{h}）。要等審核通過，別人才會看到',
      wornPending: '穿上了，但要等審核通過別人才會看到。',
      confirmUnreview: '把 {who} 從審核名單移除？等審核的披風會直接放行。',
      onWynn: '在 Wynncraft 看 {n} 的資料',
      lt: { roles: { developers: '開發者', sponsors: '贊助者', contributors: '貢獻者' }, inSync: '譯者權限與名冊的翻譯團隊一致。', mismatch: '譯者權限與名冊的翻譯團隊對不上：選一邊為準（或直接勾選），再按「儲存」。', rightsNow: '譯者權限：{s}', teamsNow: '名冊的翻譯團隊：{s}', useRights: '以譯者權限為準', useTeams: '以名冊團隊為準', noRights: '在名冊上，還沒有譯者權限。改了語言再儲存，就會同時給權限。', grantTeams: '照名冊團隊給權限', notListed: '不在名冊上：只有譯者權限。要登記翻譯團隊與貢獻項目，先按「加進名冊」。', discordOnly: '只用 Discord 登記的譯者，沒有遊戲 ID，加不進名冊。', fresh: '還不在名單上：儲存之後，他勾選的語言就有譯者權限。', notListedChip: '不在名冊', mismatchChip: '不一致', pending: '等 PR 合併', edit: '編輯', confirmDrop: '拿掉 {who} 的 {langs}？譯者權限馬上收回，名冊的翻譯團隊放進今天的 PR。', saved: '存好了', teamPr: '名冊的修改放進 PR', teamSame: '名冊本來就是這樣', teamFail: '名冊沒改成（{s}）', joinHint: '會一併登記翻譯團隊：{s}', pick: '先選一位成員', why: { 'bad who': '看不懂這個人：填遊戲 ID、UUID 或 Discord 使用者 ID', 'bad team': '語言不對', 'bad area': '貢獻項目不對', 'not listed': '不在名冊上，不能登記貢獻項目', 'bad note': '備註太長了（最多 60 個字）', 'try later': '名單暫時讀不到，等一下再試', 'bad request': '沒有要改的', forbidden: '登入過期了，或你沒有管理權限' } },
      teamAll: '全部', teamAreas: { translation: '翻譯', proofreading: '校對', capture: '資料收集', testing: '測試', code: '程式', design: '美術設計', website: '網站', community: '社群管理' },
      wearingThis: '你正在穿這一件。', restoreOwn: '復原成我原本的披風', takeOff: '脫下這件披風',
      wornOk: '穿上了，其他玩家幾分鐘內就會看到。', wornKept: '穿上了，其他玩家幾分鐘內就會看到。你原本那一件還留著，隨時可以復原。',
      restored: '已經換回你原本的披風。', tookOff: '已經脫下。',
      playStart: '動一下左邊的選項，右邊的預覽會跟著變。',
      upIcon: '上傳圖示', upBanner: '上傳旗幟', picDone: '圖換好了', confirmUnguild: '把 {who} 從貢獻公會移除？',
      guild: {
        'bad name': '公會名稱只能是英文字母與空格，3 到 30 個字',
        'bad tag': '標籤是 3 到 4 個英文字母',
        already: '這個公會已經登記了',
        'bad image': '這不是 PNG、JPEG 或 WebP 的圖',
        'too large': '圖太大了（最多 512 KB）',
        'too many': '登記的公會太多了',
        'no such guild': '找不到這個公會',
      },
      previewHint: '試穿中：{w}×{h}，還沒上傳。',
      noName: '（不知道名字）', lift: '解除', confirmLift: '解除 {who} 的封禁？', liftFail: '沒有成功（{s}）',
      revoke: '移除', confirmRevoke: '把 {who} 從特別名單移除？', perkDone: '加好了',
      confirmUnstaff: '把 {who} 從管理員移除？', notYet: '還沒登入過',
      tr: {
        gone: "已刪除的使用者 #{id}",
        langs: { zh_tw: '繁體中文', zh_cn: '简体中文', ja_jp: '日本語', ko_kr: '한국어', ru_ru: 'Русский', es_es: 'Español', tr_tr: 'Türkçe' },
        st: { open: '投票中', accepted: '已採用', rejected: '已駁回' },
        signin: '登入後才能提案與投票', roleDecide: '管理員：可以定案', roleTranslator: '譯者：可以提案與投票',
        roleViewer: '目前只能瀏覽；要提案請找管理員把你加進這個語言的譯者', loading: '讀取中…', indexFail: '語料讀不到，晚點再試一次',
        hint: '共 {n} 條。輸入關鍵字、選一個檔，或勾「只看還沒翻的」開始。', found: '找到 {n} 條，顯示前 {shown} 條',
        untranslated: '（還沒翻）', propose: '提出譯法', view: '看提案', openProps: '{n} 個提案',
        notTranslator: '你還不是這個語言的譯者，只能看。', signinToPropose: '登入後才能提案。',
        yours: '你的譯法', send: '送出', looksOk: '佔位符都對得上', by: '{who} 提出', votes: '{n} 票', vote: '投這個',
        voted: '已投（再按一次收回）', accept: '採用', reject: '駁回', prLink: '看 PR', none: '目前沒有。', seeItems: '看 {n} 個譯法',
        noPolls: '還沒有人開投票。', decided: '已定案', decide: '定案', addOption: '加一個候選', add: '加入', reopen: '重新開放',
        opened: '開好了', failed: '沒有成功（{s}）',
        preview: '實機預覽', pvSrc: '原文', pvDst: '譯文', reroll: '換一組數字', insert: '插入',
        pvNote: '數字是模擬的，旁邊的小字是它接的是原文第幾個數字；{p} 換成地名、{u} 換成名字，灰色方塊是圖示。',
        autonum: '幫我編號', autonumHint: '把譯文裡的 {~} 依序改成 {~1}、{~2}…，再照語序搬動',
        pvOver: '原文只有 {n} 個數字',
        why: {
          empty: '譯文是空的', 'too long': '太長了（最多 2000 字）',
          placeholders: '{#}、{p}、{u} 的數量或順序跟原文不一樣（{u} 可以少，不能多）', numbers: '{~} 的數量跟原文不一樣',
          numbering: '{~1}、{~2} 的編號不完整，或跟沒編號的 {~} 混在一起', colours: '顏色標記 {c1}…{/} 跟現在的譯文不一樣',
          lines: '行數跟原文不一樣', 'not a translator': '你不是這個語言的譯者', 'login first': '請先登入', closed: '這一項已經結束了',
          forbidden: '只有管理員可以定案', 'bad term': '要填英文術語', 'bad option': '候選太長或太多', 'too many': '候選太多了',
          'line not found': '語料裡找不到這一句（可能剛被改過），重新整理再試', 'github not configured': '網站還沒設定 GitHub',
          'github unreachable': '連不到 GitHub，晚點再試',
        },
      },
      fb: {
        langNone: '（沒有指定）', whereTr: '在哪裡：哪一句、哪個畫面或任務（建議填）', whereAny: '在哪裡（可以不填）',
        wherePhTr: '例如：Detlas 的鐵匠說的第一句話', wherePh: '例如：披風一覽、F6 設定的「對話」那一頁',
        count: '{n} / {max} 字', as: '以 Discord「{n}」的身分送出', done: '收到了，謝謝你的回饋！',
        issueLink: '看 GitHub 上的 Issue', issueFail: '這次沒能在 GitHub 開 Issue；回饋已經存好，管理員會再補開。',
        why: {
          'bad category': '請選一個分類', 'too short': '多寫幾個字吧（至少 3 個字）', 'too long': '太長了（最多 2000 字）',
          'where too long': '「在哪裡」太長了（最多 200 字）', 'slow down': '一小時內最多送 5 則，{t} 之後可以再送',
          'bad request': '送出的內容看不懂，請重新整理再試',
        },
        cats: { translation: '譯文建議', content: '內容建議', website: '網站建議', feature: '功能建議', other: '其他' },
        st: { open: '待處理', done: '已處理' },
        via: { web: '網站', bot: 'Discord /回饋' },
        counts: '待處理 {open} 則・已處理 {done} 則', where: '在哪裡：{s}', lang: '語言：{s}', uid: 'Discord 使用者代號：{s}',
        adminIssue: 'GitHub Issue #{n}', issueErr: 'Issue 沒開成（{s}）', handled: '{who} 在 {t} 標成已處理',
        markDone: '標成已處理', reopen: '重新開啟', retry: '再開一次 Issue', del: '刪除',
        confirmDel: '刪除這則回饋？刪了就找不回來（已經開的 GitHub Issue 不會跟著關）。',
      },
      findNone: '沒有找到已註冊的人。直接按「加入」會照你打的字加。', isStaff: '已經是管理員', chosen: '已選：{who}',
      findCount: '{n} 位（已註冊 {all} 位）',
      staff: {
        'bad who': '看不懂這一格：要填 Minecraft ID、UUID，或 Discord 使用者 ID（17 到 20 位數字）',
        'bad note': '備註最多 60 個字',
        already: '這個人已經是管理員了',
      },
      perk: {
        'bad uuid': 'UUID 要是 32 個十六進位字元（有沒有連字號都可以）',
        'bad note': '備註最多 60 個字',
        already: '這個 UUID 已經在名單上了',
      },
      sending: '送出中…', prDone: 'PR 開好了', prFail: '沒有成功（{s}）', botOk: '註冊好了：{s}',
      credit: {
        'bad name': '顯示的名字不能空白，也不能超過 40 個字',
        'bad mc': 'Minecraft ID 只能是英數與底線，最多 16 個字',
        'bad color': '顏色要寫成 #RRGGBB',
        'bad section': '身分不對',
        'already listed': '這個 Minecraft ID 已經在那一區了',
        'github not configured': '還沒設定 GITHUB_TOKEN，沒辦法開 PR（見 tools/presence/README.md）',
        forbidden: '登入過期了，或你不在開發者名單上',
      },
    },
  },

  en: {
    html: 'en',
    'nav.home': 'Home', 'nav.features': 'Features', 'nav.capes': 'Capes', 'nav.account': 'Account', 'nav.admin': 'Admin', 'nav.label': 'Main menu',
    'lang.label': 'Language',
    'top.get': 'Download', 'top.sponsor': 'Sponsor', 'top.signout': 'Sign out',
    'top.signin': 'Sign in', 'top.more': 'More', 'top.myaccount': 'My account', 'top.account': 'Account menu', 'top.menu': 'Menu',
    'top.getnote': 'Needs Wynntils and Fabric API; the jar goes in your mods folder. <a href="/#install">How to install</a>',
    'foot.label': 'Footer',
    'foot.tagline': 'A multilingual translation mod for Wynncraft. The original text stays; the translation sits beside it.',
    'foot.privacy': 'Privacy policy', 'foot.terms': 'Terms of service',
    'privacy.title': 'Privacy policy - WynnChaYuan', 'privacy.h1': 'Privacy policy',
    'terms.title': 'Terms of service - WynnChaYuan', 'terms.h1': 'Terms of service',
    'legal.updated': "Effective 30 October 2026",
    'privacy.body': "<p>This policy explains what data WynnChaYuan handles in its three parts: the WynnChaYuan Minecraft mod, this website (wynnchayuan.com) and the Discord bot. WynnChaYuan is an unofficial fan project maintained by the WynnChaYuan team (contact: LyuChaCha).</p><h2>In short</h2><ul><li>We only keep what a feature needs to work, and only for as long as it needs it. We <b>never sell</b> your data or share it for advertising or marketing.</li><li>The mod asks for your consent the first time it starts. Without consent, the update check and in-game update, translation sync from GitHub (and the contributor list), check-in, leaves and badges, capes, website sign-in and /wcy link all stay off; translation still works from the files bundled with the mod and any files synced before. You can change this later under F6 → \"Features\" → \"Privacy and online features\" → \"Online features\". It then asks how translations should appear: \"Recommended\", \"Customise\" or \"Keep my current settings\".</li><li>You can delete your data yourself at any time: on the <a href=\"/account\">account page</a> or with \"Delete my data\" under F6 → \"Features\" → \"Privacy and online features\" in the mod (see \"Deleting your data\" below).</li><li>Our code does not record IP addresses, the world you are in, chat or any other game content.</li></ul><h2>What each online feature of the mod sends</h2><ul><li><b>Update check</b>: downloads <code>version.json</code> from GitHub (or its jsDelivr mirror) to see whether there is a new version. Nothing about you is sent beyond an ordinary web request. \"Update now\" downloads the new version from GitHub Releases only when you press it.</li><li><b>Translation sync</b>: downloads newer translation files and the contributor list from GitHub (or jsDelivr). It only downloads and sends nothing about you.</li><li><b>Check-in (leaves, badges and who has the mod)</b>: when you join Wynncraft, the mod sends wynnchayuan.com your Minecraft UUID, your public profile as signed by Mojang (name and skin), a public key and signature proving the account is yours, whether to show your cape, and your badge settings (coloured leaf, which roles to show). Your private key and login credentials never leave the game.</li><li><b>Capes</b>: uploading sends the image you chose to wynnchayuan.com. To show capes, the mod downloads other players' capes from wynnchayuan.com and the team capes from GitHub (or jsDelivr); downloading sends nothing about you.</li><li><b>Website sign-in and linking</b>: opening the cape gallery from F6 exchanges the mod's pass for a sign-in link valid for one day; <code>/wcy link</code> sends the link code the website gave you.</li><li><b>Never sent on its own</b>: in-game text. Files made by \"Export untranslated strings\" stay on your computer; sending them to us is up to you.</li></ul><h2>Website account, Translate, feedback and the bot</h2><ul><li><b>Website account</b>: Discord sign-in uses only the <code>identify</code> scope. We keep your Discord user ID and display name; we do not read your email or your server list, and the access token Discord gives us is thrown away after use. Once you link your game account with <code>/wcy link</code>, the account also stores your game UUID, name and the time you linked.</li><li><b>Translate</b>: suggestions, votes and term polls store your display name and ID, the time and what you wrote. Others can see them. Accepted translations are written to the public GitHub repository (CC BY-NC-SA 4.0) and stay in its history.</li><li><b>Feedback</b>: we store your Discord user ID and display name, the category, what you wrote (including \"where\" and the language), the time and whether it came from the website or the bot. Only developers and moderators can see it. Translation suggestions also become a public GitHub issue that shows your display name (never your user ID).</li><li><b>Discord bot</b>: it only receives the slash commands you use, plus fields Discord always sends (your user ID, the server and channel IDs and your client language). It cannot read chat, and keeps nothing except feedback sent with /feedback.</li></ul><h2>Who can see your cape</h2><ul><li><b>Public</b>: anyone can see it in the cape gallery.</li><li><b>Unlisted</b>: not in the gallery; only you and people you give its link to can see it, and they can wear it too (that does not put it in the gallery).</li><li><b>Private</b>: only you can see it on the website.</li><li>Whatever you choose, moderators can see every cape, and other players with the mod still see the cape you wear in game. The gallery never shows your UUID or name, only a code hashed from the UUID; only moderators can see who uploaded which cape. Your cape history and favourites are visible only to you.</li></ul><h2>Where it is stored</h2><p>Everything our server keeps is stored in Cloudflare Workers KV. Cloudflare processes connection data (including IP addresses) for security and network operation under its own privacy policy; our code does not store it.</p><h2>How long we keep it</h2><ul><li>Check-in record: 14 days after your last check-in.</li><li>Your cape and its settings: until you remove it or delete your data. A cape nobody wears any more is kept for 7 days, then deleted; capes removed by moderators are deleted at once.</li><li>Cape history (up to 8) and favourites: until you remove them, the cape is deleted, or you delete your data.</li><li>Times of your recent uploads (to stop bursts of uploads): at most 13 hours.</li><li>Passes: 7 days from the game, 1 day for the website opened from the game, 30 days for Discord sign-in. Link codes: 10 minutes.</li><li>Website account and link: until you delete your data or unlink.</li><li>Feedback: deleted 180 days after it is marked handled; open feedback stays until it is handled or deleted. Send times kept for rate limiting: 1 hour.</li><li>Previews of a cape on your skin, made for the Discord bot: 3 days.</li><li>Ban and review records: until a moderator lifts them.</li></ul><h2>Deleting your data</h2><p>Press \"Delete my data\" on the <a href=\"/account\">account page</a>, or under F6 → \"Features\" → \"Privacy and online features\" in the mod (press it twice to confirm). Both send the same request (<code>POST /api/me/delete</code>) and deletion starts immediately; if there is a lot of data it finishes over several requests, which the account page keeps sending on its own. The mod also stops check-in, leaves and cape uploads, and resends the request at every launch until the server confirms everything is deleted, even if you never agreed to online features. To upload again, press \"Resume uploads\" in the same place.</p><ul><li><b>Deleted</b>: your check-in record (you drop off the player list at once), your cape and its settings, images only you used (straight away, not after 7 days), your cape history and favourites, your website account and Discord link, feedback you sent, and cape previews made with your skin.</li><li><b>Kept, and why</b>: ban and review records and the lists moderators manage (special perks, moderators, translators), so deleting cannot undo a moderation decision; images other players are still wearing; the times of your recent uploads (at most 13 hours), so deleting cannot skip the upload cooldown; and your Translate suggestions and votes, which are part of the public project: your name on them is replaced with \"Deleted user #code\" (a short code derived from your account, so entries by the same person stay distinguishable), and your game UUID or Discord ID stays on those entries, visible only to developers and moderators, as a record of who contributed. Translations already accepted into the repository history are not changed, and GitHub issues stay on GitHub.</li><li>If you delete only on the website, the mod checks in again the next time you join Wynncraft. To stop that, press \"Delete my data\" or turn off \"Online features\" in the mod.</li></ul><h2>Cookies and browser storage</h2><ul><li><code>lang</code>: remembers the website language you chose</li><li><code>wcy_oauth</code>, <code>wcy_intent</code>: protect the Discord sign-in and only matter for the few minutes it takes</li><li>Browser <code>localStorage</code>: your sign-in pass and small display preferences</li></ul><p>There are no ads and no analytics or tracking cookies.</p><h2>Third-party services</h2><ul><li><b>Cloudflare</b>: hosts the website and its data</li><li><b>Discord</b>: sign-in and the bot</li><li><b>GitHub</b>: source code, translations, update files, Translate pull requests and issues from translation suggestions</li><li><b>jsDelivr</b>: a mirror of the GitHub files the mod downloads, and the 3D cape viewer your browser loads</li><li><b>minotar.net</b>: player heads on the website are loaded by your browser directly from it; for the Discord bot's cape preview, the website fetches that player's skin from it</li></ul><h2>Age</h2><p>You need to meet the age requirements of Minecraft and Discord to use these services.</p><h2>Changes</h2><p>If this policy changes, we update this page and the date above. Significant changes are announced on Discord or in the changelog.</p>",
    'terms.body': "<p>By using the WynnChaYuan mod, the wynnchayuan.com website or the Discord bot, you agree to these terms. If you do not agree, please do not use them.</p><h2>Unofficial project</h2><p>WynnChaYuan is an unofficial fan project. It is not affiliated with or endorsed by Wynncraft, Mojang, Microsoft or Discord. Game textures and screens on this website belong to Wynncraft and their respective owners.</p><h2>Licences</h2><p>The mod's code is licensed under AGPL-3.0; translations and corpus data under CC BY-NC-SA 4.0. See LICENSE and LICENSE-DATA in the GitHub repository.</p><h2>Accounts</h2><ul><li>One website account links to one game account, and linking must be done from inside the game</li><li>Do not use someone else's account, get around identity checks, or let others use your pass</li><li>Accounts that break these terms may be suspended</li><li>You can delete your account and data at any time on the account page; see the <a href=\"/privacy\">privacy policy</a> for what is kept</li></ul><h2>Cape rules</h2><p>Capes are seen by other players in game, and on the website by whoever the visibility you choose (public, unlisted or private) allows. Uploaded images must not contain:</p><ul><li>Political propaganda</li><li>Sexual, obscene or gory content</li><li>Illegal content, or images that infringe copyrights or trademarks</li><li>Racist, hateful or harassing content</li><li>Advertising (promoting a guild is fine)</li></ul><p>Moderators may review or remove capes that break these rules, or ban the uploader, without notice. You keep the rights to your image; by uploading it you let us store it, show it to other players in game, and show it to website visitors as your chosen visibility allows.</p><h2>Translate</h2><ul><li>Only submit translations you have the right to provide, and agree to license them under CC BY-NC-SA 4.0</li><li>Follow the glossary and your language team's decisions; developers and admins have the final say</li><li>No spam, vote stuffing or unrelated content</li></ul><h2>Not allowed</h2><ul><li>Calling the website's API in bulk or automatically, or disrupting the service</li><li>Trying to access data or permissions that are not yours</li><li>Abusing the Discord bot</li></ul><h2>Sponsoring</h2><p>Sponsoring through Ko-fi is voluntary support. Sponsor perks (such as GIF capes) may change as the project evolves.</p><h2>The game and third parties</h2><p>The mod only changes the text and appearance shown on your own screen; it does not change gameplay. You are responsible for following the rules of Wynncraft and Minecraft when using any mod. The first time you join Wynncraft with the mod, it shows a usage notice (\"Before you play\"), and again at every launch until you tick \"Don't show this again\": translations appear only on your own screen, so use the original English terms when talking with other players.</p><h2>No warranty</h2><p>These services are provided as is and may have errors, interruptions or be discontinued. To the extent permitted by law, we are not liable for any loss arising from using them.</p><h2>Changes and termination</h2><p>We may change these terms or stop some or all of the services. Changes are posted on this page with an updated date; continuing to use the services means you accept the new terms.</p><h2>Contact</h2><p>For questions, contact <b>LyuChaCha</b> on Discord or open a GitHub issue.</p>",
    'foot.get': 'Download', 'foot.project': 'Project', 'foot.support': 'Support', 'foot.contact': 'Contact',
    'foot.changelog': 'Changelog', 'foot.license': 'Code licence: AGPL-3.0', 'foot.datalicense': 'Translation licence: CC BY-NC-SA 4.0',
    'foot.issues': 'Report a problem', 'foot.contribute': 'How to contribute', 'foot.team': 'The team', 'foot.sponsoring': 'About sponsoring',
    'foot.kofi': 'Sponsor on Ko-fi', 'foot.sitedev': 'Website developer: LyuChaCha', 'foot.discord': 'Discord: LyuChaCha',
    'foot.copy': 'Copy the Discord name',
    'foot.unofficial': 'A fan-made mod. Not affiliated with Wynncraft, Mojang or Microsoft.',
    'foot.assets': 'Game textures and icons shown in the previews belong to Wynncraft.',

    'home.title': 'WynnChaYuan - Wynncraft in your language',
    'home.h1': 'Wynncraft, in your language',
    'home.lede': 'WynnChaYuan is a translation mod for Wynncraft (Fabric 1.21.11). The original text stays; the translation appears next to it or is written into the same spot - quests, dialogue, items and menus.',
    'home.github': 'Download on GitHub',
    'home.langs': 'Supported languages',
    'home.what': 'What gets translated',
    'home.f1': 'Items and gear', 'home.f1b': 'Lore, stats and Major IDs, in a side panel or inside the original tooltip.',
    'home.f2': 'Quests and dialogue', 'home.f2b': 'Written into Wynncraft’s own dialogue box, typed out in step with the original. The quest tracker too.',
    'home.f3': 'Ability trees', 'home.f3b': 'Nodes, descriptions and archetypes for all five classes.',
    'home.f4': 'Chat and notices', 'home.f4b': 'Server messages: quest completion, rewards, entering areas. What players say is left alone.',
    'home.f5': 'Menus and screens', 'home.f5b': 'Trade market, guild and store, plus the lootrun, raid and dungeon panels.',
    'home.f6': 'Translations update themselves', 'home.f6b': 'New translations take effect the next time you start the game. No mod update needed.',
    'home.install': 'Install',
    'home.need': 'Requires <a href="https://modrinth.com/mod/wynntils">Wynntils</a> 4.2 or newer and <a href="https://modrinth.com/mod/fabric-api">Fabric API</a>. Client side only; nothing to install on the server.',
    'home.how': 'Drop the jar into your mods folder, then press F6 in game for settings.',
    'home.about': '<b>About the translations.</b> Some text was written by people and passed through AI, a small part is AI translation, and most of the core content has been proofread by hand. Languages other than Traditional Chinese are mostly AI translations built from it, so expect some errors and inconsistent wording. Please report problems on <a href="' + ISSUES + '">GitHub Issues</a>.',
    'home.morefeatures': 'See every feature',
    'home.team': 'The team',
    'home.teamlede': 'The people behind WynnChaYuan. The colour of the leaf shows the role, the same leaf you see before their name in game; click someone to see their Wynncraft profile.',
    'home.guilds': 'Contributing guilds', 'home.guildsb': 'Guilds that have given the mod a great deal of help, or whose members help one another with it.',
    'home.teamfail': 'The list could not be loaded. Please refresh.',
    'home.teampick': 'Filter by translation team',
    'home.dev': 'Developers', 'home.devb': 'Help build and maintain the mod.',
    'home.sponsor': 'Sponsors', 'home.sponsorb': 'A one-off 20 USD or 2 USD a month and up.',
    'home.contrib': 'Contributors', 'home.contribb': 'Translate for their language, fix mistakes, or help with the code.',
    'home.user': 'Everyone with the mod', 'home.userb': 'A grey leaf before their name.',
    'home.support': 'About sponsoring',
    'home.free': '<b>The translations are free, and will stay free.</b> Every translation and every translation feature is provided by the team at no cost; nothing is locked behind payment. Sponsoring is entirely voluntary:',
    'home.sup1': '<b>Not sponsoring never lowers translation quality.</b> Everyone gets the same translations, updates and fixes - no fewer and no later.',
    'home.sup2': '<b>Sponsors only get fun extras:</b> the cyan leaf before their name, a place on the list, and animated (GIF) capes with a larger file limit.',
    'home.sup3': '<b>Sponsors\u2019 feedback is given priority:</b> we look at and act on their reports and suggestions first.',
    'home.seecapes': 'Browse the capes', 'home.donate': 'Sponsor', 'home.join': 'How to contribute',

    'capes.title': 'Cape gallery - WynnChaYuan',
    'capes.h1': 'Cape gallery',
    'capes.lede': 'Capes uploaded by WynnChaYuan players. Click one to turn it around; once signed in, you can also wear it.',
    'capes.mine': 'Upload my cape',
    'capes.sort': 'Sort by', 'capes.filter': 'Filter', 'capes.f.format': 'Format', 'capes.f.show': 'Show',
    'capes.s.new': 'Newest', 'capes.s.old': 'Oldest', 'capes.s.worn': 'Most worn', 'capes.s.stars': 'Most popular', 'capes.s.recent': 'Recently put on',
    'capes.f.all': 'All', 'capes.f.fav': 'My favourites', 'capes.f.hist': 'Worn before', 'capes.f.hidden': "Unlisted", 'capes.f.pending': 'Awaiting review', 'capes.f.private': "Private",
    'rules': '<b>Not allowed:</b> political, obscene or sexual, unlawful, racist or hateful content; advertising (commercial promotion, promoting other servers, real-money trading such as selling LE for cash, real-life contact details; guild promotion is fine); and anything else against Wynncraft’s rules. Do not abuse uploads either (for example, uploading many capes in a short time). Capes that break the rules are removed, and the uploader may lose the ability to upload.',
    'capes.keep': 'A cape nobody is wearing is kept for 7 days. If no one has put it on by then it is removed, starred or not.',
    'skip': 'Skip to content',
    'capes.make': 'Make your own cape', 'capes.makeb': 'Download the template, draw your design, upload. Three steps.',
    'tpl.h': 'Cape template',
    'tpl.p': 'Capes are drawn on a template. Download one, replace the blue (front), red (back) and cyan (border) areas with your design, save it as a PNG or GIF, then upload it.',
    'tpl.pick': 'Download a template (pick a size)', 'tpl.rec': 'recommended',
    'tpl.note': 'Bigger sizes allow finer detail but make bigger files. If unsure, use 88×68.',
    'tpl.example': 'Download an animated example (GIF)', 'tpl.upload': 'Finished? Upload it on the Account page',
    'tpl.alt': 'Diagram of the cape template: which areas are the front, the back and the border',
    'lost.title': 'Page not found - WynnChaYuan', 'lost.h1': 'This page does not exist',
    'lost.p': 'The address may be mistyped, or the page has moved.', 'lost.home': 'Back to home',
    'home.live': 'How it looks in game: the original stays, the translation sits beside it.', 'home.try': 'Try the options',
    'view.title': 'Cape', 'view.flat': 'The cape template image: front on the left, back on the right',
    'view.type': 'Format', 'view.dim': 'Dimensions', 'view.size': 'File size', 'view.users': 'Players', 'view.date': 'Uploaded',
    'view.stars': 'Favourited by', 'view.approve': 'Approve', 'view.review': 'Review the uploaders’ future capes', 'view.forget': 'Remove from my history', 'view.share': "Copy link",
    'view.del': 'Delete this cape', 'view.ban': 'Delete and ban the uploaders', 'view.close': 'Close',
    'view.wear': 'Wear this cape', 'view.signin': 'Sign in and link your game ID to wear this one. <a href="/account">Go to Account</a>',

    'account.title': 'Account - WynnChaYuan',
    'account.h1': 'Account',
    'account.lede': 'Register with Discord, then link your game ID from inside the game - or simply open this site from the game.',
    'account.discord.h': 'Register / sign in with Discord',
    'account.discord.p': 'Site accounts sign in with Discord; we only read your Discord name. After signing in, link your game ID from inside the game to upload capes. From then on you can sign in without starting the game.',
    'account.discord.btn': 'Sign in with Discord',
    'account.discord.off': 'Discord sign-in is not enabled on this site yet. Please open the site from the game.',
    'account.discord.err': 'Discord sign-in did not work. Please try again.',
    'account.discord.hint': 'Link Discord and you can sign in to this site with Discord, without starting the game.',
    'account.discord.bind': 'Link Discord',
    'account.link.h': 'Link your game ID',
    'account.link.p': 'This site cannot verify by itself that a game account is yours, so linking is done from inside the game: get a link code here, then enter it in game.',
    'account.link.get': 'Get a link code',
    'account.link.type': 'Join Wynncraft and type this line in chat (valid for ten minutes; WynnChaYuan must be installed):',
    'account.link.copy': 'Copy',
    'account.link.wait': 'This page updates by itself once you have entered it.',
    'account.unlink': 'Unlink Discord',
    'account.how': 'Or open it straight from the game',
    'account.s1': 'Join Wynncraft and press <b>F6</b> to open the WynnChaYuan settings.',
    'account.s2': 'Go to the <b>Features</b> page.',
    'account.s3': 'Press <b>Open</b> next to <b>Cape gallery</b>. Your browser opens already signed in.',
    'account.expire': 'A sign-in lasts one day; open it from the game again when it expires. Offline accounts cannot sign in.',
    'account.banned': 'This account cannot upload capes.',
    'account.signout': 'Sign out of this browser',
    'account.mine': 'My cape', 'account.none': 'Nothing uploaded yet.', 'account.remove': 'Remove my cape',
    'account.worn': 'This one was picked from the cape gallery.',
    'account.vis': "Who can see this cape",
    'account.vis.public': "Public",
    'account.vis.unlisted': "Unlisted",
    'account.vis.private': "Private",
    'account.vis.game': "This only affects the website: other players with the mod still see you wearing it in game.",
    'account.share': "Share link: anyone with this link can see this cape.",
    'account.del.h': "Delete my data",
    'account.del.p': "Removes all your data from our server: check-in record, cape, cape history and favourites, website account and link, and feedback you sent. Ban and review records are kept; see the <a href=\"/privacy\">privacy policy</a>.",
    'account.del.btn': "Delete my data",
    'account.del.list': "<p>Are you sure? This cannot be undone.</p><ul><li><b>Deleted</b>: your check-in record, your cape and its settings, images only you used, your cape history and favourites, your website account and Discord link, and feedback you sent.</li><li><b>Kept</b>: ban and review records, images other players are still wearing, the times of your recent uploads (at most 13 hours), and your Translate suggestions and votes (shown as \"Deleted user #code\"; your ID is visible only to moderators).</li><li>The mod checks in again the next time you join Wynncraft. To stop that, press \"Delete my data\" under F6 → \"Features\" → \"Privacy and online features\" in the mod; it also stops check-in and uploads (press \"Resume uploads\" to start again).</li></ul>",
    'account.del.cancel': "Cancel",
    'account.del.go': "Delete everything",
    'account.del.done': "Your data has been deleted and this browser is signed out.",
    'account.pending': 'This cape is awaiting review. Nobody else can see it until it is approved.',
    'account.review': 'Capes you upload are reviewed first for now; others see them once they are approved. While under review you can have only one uploaded cape, pending ones included; to change it, remove the current one first.',
    'account.upload': 'Upload a cape',
    'account.spec': 'Draw it on the <a href="' + TEMPLATES + '">template</a>: ' + SIZES + '. File size: up to 1 MB for regular players, 20 MB for developers, sponsors and contributors. Anyone can upload a PNG; GIFs (up to 120 frames) are for developers, sponsors and contributors. If you upload 3 or more capes within an hour, you have to wait 12 hours before uploading again.',
    'account.pick': 'Choose a cape image', 'account.send': 'Upload',

    'admin.title': 'Admin - WynnChaYuan',
    'nav.translate': 'Translate', 'tr.title': 'Translate - WynnChaYuan', 'tr.h1': 'Translate together',
    'tr.lede': 'Search for a line you saw in game, suggest a better translation, or vote on a disputed term. Translators of each language can suggest and vote; once a moderator decides, it goes into the translations.',
    'tr.lang': 'Language', 'tr.tab.browse': 'Browse and suggest', 'tr.tab.props': 'Suggestions', 'tr.tab.terms': 'Term votes',
    'tr.q': 'Search the source or the translation (two characters or more)', 'tr.allfiles': 'All files', 'tr.blank': 'Untranslated only',
    'tr.props.note': 'Suggestions and votes for each line. Lines a moderator accepts on the same day for the same language go into one pull request.',
    'tr.terms.note': 'Vote here when a term has more than one translation. After a decision, maintainers apply it across the translations.',
    'tr.st.open': 'Voting', 'tr.st.accepted': 'Accepted', 'tr.st.rejected': 'Rejected',
    'tr.np.term': 'English term', 'tr.np.opts': 'Candidate translations (comma-separated)', 'tr.np.note': 'Note (optional): where it appears and what the disagreement is', 'tr.np.send': 'Start the vote',
    'admin.h1': 'Admin',
    'admin.gate': 'This page is for developers and moderators only. Please sign in first.',
    'admin.lede': 'Deleting capes and banning uploaders happens in the <a href="/capes">cape gallery</a>: open a cape to see who uploaded it.',
    'admin.bans': 'Bans',
    'admin.player': 'Player', 'admin.when': 'Banned at', 'admin.by': 'Banned by',
    'admin.nobans': 'Nobody is banned.',
    'admin.add': 'Add a member',
    'admin.addnote': 'This opens a pull request on GitHub that edits <code>credits.json</code>. Once merged, it takes effect the next time players sync the list (name-tag leaf, permission to upload GIF capes).',
    'admin.section': 'Role', 'admin.name': 'Display name', 'admin.mc': 'Minecraft ID', 'admin.color': 'Leaf colour (optional)',
    'admin.contributors': 'Contributor', 'admin.sponsors': 'Sponsor', 'admin.developers': 'Developer',
    'admin.send': 'Open PR',
    'admin.lt': 'Translation teams', 'admin.ltnote': 'Manage which languages a person works on, all in one place. Ticking a language does two things: they can suggest and vote for that language on the Translate page (at once), and they are listed in that language’s translation team in the credits (<code>credits.json</code>, in that day’s pull request; the home page follows once it is merged). Removing a language removes both. Translators who are not in the credits only have the rights; press “Add to credits” before recording teams and contributions. Credits members without translator rights only show their teams. Where the two disagree the row is marked “Mismatch”: pick a side and save; nothing is synced automatically.', 'admin.ltlangs': 'Languages (translator rights and teams)', 'admin.ltsave': 'Save', 'admin.ltjoin': 'Add to credits', 'admin.ltrights': 'Translator rights', 'admin.ltteams': 'Teams in the credits', 'admin.nolt': 'No members or translators yet.',
    'admin.tmember': 'Member', 'admin.tareas': 'Contributions',
    'account.drop': 'You can also drop the image here. If the size is right, the model above tries it on first.',
    'admin.reviews': 'Review list',
    'admin.reviewsnote': 'A lighter measure than a ban: people on this list can still upload, but a cape they upload later is shown to others (in the gallery and in game) only after a moderator clicks “Approve” in the cape gallery. The cape they are wearing now is not affected. Capes awaiting review are under “Awaiting review” in the gallery. Enter their Minecraft UUID; you can also open a cape in the gallery and add its uploaders from there.',
    'admin.noreviews': 'Nobody is on the review list.',
    'admin.perks': 'Special list',
    'admin.perksnote': 'People on this list can upload GIF capes and files up to 20 MB, the same as developers, sponsors and contributors, but they are not shown on the credits list and get no leaf before their name. Enter their Minecraft UUID, with or without hyphens. It takes effect immediately.',
    'admin.uuid': 'Minecraft UUID', 'admin.note': 'Note (optional)', 'admin.grant': 'Add',
    'admin.noperks': 'The list is empty.', 'admin.pwhen': 'Added at', 'admin.pby': 'Added by',
    'admin.staff': 'Moderators',
    'admin.staffnote': 'Moderators can do what developers do on this website: remove capes, ban and unban, manage the special list, and open pull requests for the credits. The difference: they are not shown on the team list, get no leaf before their name, and cannot add or remove moderators.',
    'admin.staffhow': 'Open the drop-down and pick one of the registered members (people who have signed in to the website or uploaded a cape); type a Discord name, Minecraft ID, UUID or Discord user ID to filter. Someone with a linked game account is saved by UUID, so signing in with either the game or Discord works. For someone who has not registered yet, type their full Minecraft ID or Discord user ID and press Add; an entry added by Minecraft ID is switched to the UUID the first time they sign in. It takes effect at once.',
    'admin.who': 'Choose a member', 'admin.findph': 'Click to open, or type to filter…',
    'admin.kind': 'Type', 'admin.id': 'ID', 'admin.nostaff': 'No moderators yet.',
    'admin.bot': 'Discord bot', 'admin.botreg': 'Register commands', 'admin.botinvite': 'Invite to a server',
    'admin.botnote': 'This website answers the slash commands /status, /cape, /translate and /feedback. After deploying, paste the Interactions Endpoint URL below into the Discord Developer Portal, then press “Register commands”; press it again whenever the commands change. Set the bot token with <code>wrangler secret put DISCORD_BOT_TOKEN</code> and never paste it into any web page.',
    'admin.guilds': 'Contributing guilds',
    'admin.guildsnote': 'These appear in the team section of the home page and link to the guild’s page on Wynncraft. Add the name first (exactly as it is on Wynncraft), then upload an icon and a banner. Images are shrunk before upload; PNG, JPEG and WebP all work.',
    'admin.gname': 'Guild name', 'admin.gtag': 'Tag (optional)', 'admin.gpics': 'Icon and banner', 'admin.guild': 'Guild',
    'admin.noguilds': 'No guilds yet.',
    'admin.feedback': 'Feedback',
    'admin.feedbacknote': 'Everything sent from the feedback page and the Discord /feedback command lands here, newest first. Press “Mark handled” when you are done; handled feedback is deleted automatically after 180 days. Delete spam outright. Translation suggestions open a GitHub issue automatically; if that failed, the reason is shown and you can press “Open issue again”.',
    'admin.fbst': 'Status', 'admin.fball': 'All', 'admin.fbopen': 'Open', 'admin.fbdone': 'Handled', 'admin.nofeedback': 'No feedback matches.',

    'nav.feedback': 'Feedback', 'foot.feedback': 'Feedback and ideas',
    'fb.title': 'Feedback - WynnChaYuan', 'fb.h1': 'Feedback and suggestions',
    'fb.lede': 'Tell us about a translation that reads wrong, content you would like translated, something awkward on the website, or a feature you want. You can also use the bot’s <code>/feedback</code> command on Discord.',
    'fb.gate': 'Sign in with Discord to send feedback, so we know who wrote it. <a href="/account">Sign in on the account page</a>',
    'fb.gatelink': 'You opened this site from the game and have not linked Discord yet. Press “Link Discord” on the account page, or sign in with Discord, to send feedback. <a href="/account">Go to the account page</a>',
    'fb.cat': 'Category', 'fb.cat.translation': 'Translation suggestion', 'fb.cat.content': 'Content suggestion',
    'fb.cat.website': 'Website suggestion', 'fb.cat.feature': 'Feature suggestion', 'fb.cat.other': 'Other',
    'fb.lang': 'Which translation', 'fb.msg': 'Your feedback', 'fb.where': 'Where (optional)',
    'fb.wherehint': 'For example a quest or NPC name, a screen or a page, or paste the English line itself.',
    'fb.send': 'Send',
    'fb.note': 'You can send up to 5 per hour. <b>Translation suggestions automatically become a public GitHub issue</b> that shows your Discord display name (never your user ID); other categories are only seen by developers and moderators. See the <a href="/privacy">privacy policy</a>.',
    'fb.bugs': 'For crashes, broken screens and other bugs, please report on <a href="' + ISSUES + '">GitHub Issues</a> with your log and a screenshot; that finds the cause faster.',

    'features.title': 'Features - WynnChaYuan',
    'features.h1': 'Features',
    'features.lede': 'WynnChaYuan puts the English on your Wynncraft screen into your language, and whether the original stays is up to you. Play with the preview below first; further down is what gets translated and what else comes with it.',
    'demo.label': 'Live preview',
    'play.pages': 'Settings pages',
    'play.note': 'The options on the left are the ones in the mod’s settings screen (type <code>/wcy</code> in chat to open it); the translations in the preview come straight from the mod’s corpus.',
    'features.nav': 'On this page',
    'features.t': 'What gets translated', 'features.u': 'Tools that come with it', 'features.s': 'Leaves and capes', 'features.c': 'Works with',
    'features.tnote': 'Gear names stay in English by default: they are proper nouns, and the trade market and the wiki use the English ones. You can turn translated names on in the settings.',
    'features.cnote': 'Mods not listed here usually work too: WynnChaYuan only changes the text on your own screen, and the server does not need it.',
    'features.install': 'How to install',
    'feat.required': 'Required', 'feat.optional': 'Optional', 'feat.compatible': 'Compatible',
    'feat.t1': 'Item tooltips', 'feat.t1b': 'Gear lore, stats and Major IDs. In a panel beside the tooltip, or written into the tooltip itself.',
    'feat.t2': 'Ability trees', 'feat.t2b': 'Nodes, descriptions and archetypes for all five classes.',
    'feat.t3': 'Quest dialogue and choices', 'feat.t3b': 'Written into Wynncraft’s own dialogue box, with the frame, nameplate and portrait kept and the text typed out in step with the original; or shown in a separate box.',
    'feat.t4': 'Quest tracker', 'feat.t4b': 'Its own box; the heading says what is being tracked: a quest, a world event, a cave, a raid.',
    'feat.t5': 'NPC nameplates and floating text', 'feat.t5b': 'A box while you look at one, or replaced in place. Crafting stations and interaction prompts are included.',
    'feat.t6': 'Chat messages', 'feat.t6b': 'Messages from the server: quest completions, rewards, entering and leaving areas. Column layouts stay aligned; what players say is never translated.',
    'feat.t7': 'Menus and interfaces', 'feat.t7b': 'The trade market, guild, store and other screens.',
    'feat.t8': 'Lootruns, raids, dungeons', 'feat.t8b': 'Missions, boons, beacons, aspects and gambits, plus the end-of-run and loot panels.',
    'feat.t9': 'Discoveries and secret discoveries', 'feat.t9b': 'Names, descriptions and stories.',
    'feat.t10': 'Title text', 'feat.t10b': 'The big title and subtitle in the middle of the screen.',
    'feat.u1': 'Translations update themselves', 'feat.u1b': 'Once a new translation is merged it shows up the next time you start the game, with no mod update. Offline, the last copy you downloaded is used.',
    'feat.u2': 'Market search', 'feat.u2b': 'Search the trade market in your language; the English name is what gets sent. Matches are listed as you type.',
    'feat.u3': 'Names your way', 'feat.u3b': 'Names of quests, caves, discoveries and other content can be shown translated, translated with the original, or left in the original.',
    'feat.u4': 'Fallback language', 'feat.u4b': 'For lines your language does not have yet, show another language instead, or keep the original.',
    'feat.u5': 'Panels where you want them', 'feat.u5b': 'Every box can be dragged into place; dialogue, choices and the tracker can also be resized from the corner.',
    'feat.u6': 'Copy chat and panel screenshots', 'feat.u6b': 'List recent chat messages and click one to copy it; save the translation panel as an image. Handy when you report a problem.',
    'feat.u7': 'Update from inside the game', 'feat.u7b': 'When a new version is out, one click on the release notes screen downloads it, and it is swapped in when you close the game.',
    'feat.u8': 'Settings', 'feat.u8b': 'Type <code>/wcy</code> in chat, or open them from Mod Menu. Hover an option for an explanation; the preview on the right follows the values you set.',
    'feat.s1': 'A leaf before the name', 'feat.s1b': 'Everyone with the mod has a leaf before their name, and they see each other’s: gold for developers, cyan for sponsors, green for contributors, grey for everyone else.',
    'feat.s2': 'Capes', 'feat.s2b': 'Upload your own cape and other players with the mod will see it; if you like one in the gallery you can wear it too, and switch back whenever you want. Anyone can use a PNG; GIFs are for developers, sponsors and contributors. <a href="/capes">Browse the capes</a>',
    'feat.s3': 'What is sent', 'feat.s3b': 'For the two features above, and only after you agree to online features, the mod checks in with wynnchayuan.com once when you join Wynncraft: your UUID, your public profile, and your cape and badge settings. No game content is sent. <a href="' + REPO + '#readme">The README has the full details</a>',
    'feat.c1b': 'Item, quest and chat data all come from it; its own screens are translated too.',
    'feat.c2b': 'A dependency, like Wynntils.',
    'feat.c3b': 'Adds a settings button to the mod list.',
    'feat.c4b': 'Its search panel accepts translated names, and shows them next to the English ones in the results.',
    'feat.c5b': 'Its price box, reward screens and settings are translated too; hold Shift to see the original.',
    'feat.c6b': 'The few lines it reads to tell how a raid ended stay in English, so your streak is counted right.',

    js: {
      loading: 'Loading…',
      count: '{n} capes (identical images count once)', empty: 'Nobody has uploaded a cape yet.', users: '{n}', wearing: '{n} wearing',
      card: '{t} cape, worn by {n}',
      drag: 'Drag to rotate.', no3d: 'The 3D view could not load; only the flat image is shown.',
      skinPick: 'Player look', skinPlain: 'No skin',
      confirmDel: 'Delete this cape (worn by {n})?',
      confirmBan: 'Delete this cape and ban the {n} players wearing it from uploading?',
      expired: 'That did not work ({s}). A sign-in lasts one day - open the site from the game again.',
      readFail: 'Could not load the list. Please refresh.',
      roleDev: 'Developer', roleMember: 'Listed member', roleUser: 'Player with the mod',
      mineHint: '{t} · {d}{s} · drag to rotate', mineNo3d: 'The 3D view could not load.',
      discordAs: 'Discord: {n}', codeFail: 'Could not get a link code - please try again', copied: 'Copied', vis: { public: "Public", unlisted: "Unlisted", private: "Private" }, visHint: { public: "Everyone can see it in the cape gallery, wear it and add it to their favourites.", unlisted: "Not shown in the gallery; only you and people with the link can see it. People with the link can wear it, but not add it to favourites.", private: "Only you can see it on the website (apart from moderators)." }, linkGone: "This cape could not be found: it may have been removed, or its owner made it private.", wipeFail: "Could not delete your data - please try again later.",
      confirmUnlink: 'Unlink this game ID from Discord?',
      confirmRemove: 'Remove the cape you uploaded?', removed: 'Cape removed', tryLater: 'That did not work - try again later',
      pickFirst: 'Choose an image first', uploading: 'Uploading…', uploaded: 'Cape uploaded ({w}×{h}) - others will see it within a few minutes',
      picked: 'This image is {w}×{h}, {s}', pickedBad: 'This image is {w}×{h}, which is not a template size (' + SIZES + ')',
      bindFail: 'That did not work - please try again',
      failed: 'That did not work ({s}) - try again later', offline: 'Could not connect - try again later',
      why: {
        'too large': 'The file is too large (1 MB for regular players; 20 MB for developers, sponsors and contributors)',
        'bad size': 'Wrong size: draw it on the template - ' + SIZES,
        'bad format': 'This is not a PNG or GIF, or the file is damaged',
        'too many frames': 'The GIF has too many frames (120 at most)',
        'gif members only': 'GIFs are for developers, sponsors and contributors; anyone can upload a PNG',
        'not listed': 'Uploads are open to listed members only for now',
        banned: 'This account cannot upload capes',
        'uploads off': 'Uploads are closed right now',
        'slow down': 'You just uploaded - try again in a moment',
        cooldown: 'You uploaded 3 capes within an hour. You can upload again after {t}',
        'review one': 'While under review you can have only one uploaded cape, pending ones included; to change it, remove the current one first.',
        'login first': 'Your sign-in expired - open the site from the game again',
        'no such cape': 'This cape is no longer here. Please refresh',
        'nothing to restore': 'There is no cape to switch back to',
        'in use': 'You cannot remove the cape you are wearing from your history',
        'too many': 'Your favourites are full (200 at most)',
        'no cape': 'You do not have a cape yet',
      },
      favAdd: 'Add to my favourites', favRemove: 'Remove from my favourites',
      emptyFav: 'No favourites yet. Click the star on a cape to add it.', emptyHist: 'Nothing worn yet.', emptyFilter: 'No capes match.',
      tagPending: 'In review', tagMine: 'Wearing', tagWorn: 'picked',
      idleLeft: 'Unworn · {n} days left', idleLast: 'Unworn · last day',
      idleNote: 'Nobody is wearing this one right now. It will be removed unless someone puts it on within {n} days.',
      idleNoteLast: 'Nobody is wearing this one right now. It will be removed unless someone puts it on today.',
      removedNote: '{n} of the capes you starred or wore have been removed (nobody wore them for 7 days).',
      confirmReview: 'Put the uploaders of this cape on the review list? Capes they upload from now on must be approved before others can see them.',
      reviewed: 'Added to the review list ({n})', reviewedNone: 'The uploaders are already on the review list', approved: 'Approved',
      confirmForget: 'Remove this one from your history?', forgot: 'Removed from your history',
      uploadedPending: 'Cape uploaded ({w}×{h}). Others will see it once it has been approved',
      wornPending: 'You are wearing it now, but others will only see it once it has been approved.',
      confirmUnreview: 'Remove {who} from the review list? A cape awaiting review will be let through.',
      onWynn: 'See {n} on Wynncraft',
      lt: { roles: { developers: 'Developer', sponsors: 'Sponsor', contributors: 'Contributor' }, inSync: 'Translator rights and credits teams match.', mismatch: 'Translator rights and credits teams do not match: pick a side (or tick the boxes yourself), then press “Save”.', rightsNow: 'Translator rights: {s}', teamsNow: 'Teams in the credits: {s}', useRights: 'Use translator rights', useTeams: 'Use credits teams', noRights: 'In the credits, but no translator rights yet. Change the languages and save to grant rights as well.', grantTeams: 'Grant rights for these teams', notListed: 'Not in the credits: translator rights only. Press “Add to credits” to record teams and contributions.', discordOnly: 'Added by Discord only, with no game ID, so they cannot be added to the credits.', fresh: 'Not on the list yet: once you save, they get translator rights for the ticked languages.', notListedChip: 'Not in credits', mismatchChip: 'Mismatch', pending: 'waiting for the PR', edit: 'Edit', confirmDrop: 'Remove {langs} from {who}? Translator rights are taken away at once; the credits change goes into today’s pull request.', saved: 'Saved', teamPr: 'credits change added to a pull request', teamSame: 'the credits already say this', teamFail: 'the credits were not changed ({s})', joinHint: 'Their translation teams will be recorded too: {s}', pick: 'Choose a member first', why: { 'bad who': 'Who is that? Enter a game ID, UUID or Discord user ID', 'bad team': 'Unknown language', 'bad area': 'Unknown contribution', 'not listed': 'Not in the credits, so contributions cannot be recorded', 'bad note': 'The note is too long (60 characters at most)', 'try later': 'The credits list cannot be read right now; try again shortly', 'bad request': 'Nothing to change', forbidden: 'Your sign-in expired, or you cannot manage this site' } },
      teamAll: 'All', teamAreas: { translation: 'Translation', proofreading: 'Proofreading', capture: 'Data collection', testing: 'Testing', code: 'Code', design: 'Art and design', website: 'Website', community: 'Community' },
      wearingThis: 'You are wearing this one.', restoreOwn: 'Switch back to my own cape', takeOff: 'Take this cape off',
      wornOk: 'You are wearing it now. Other players will see it within a few minutes.', wornKept: 'You are wearing it now. Other players will see it within a few minutes. Your own cape is kept, and you can switch back any time.',
      restored: 'Your own cape is back.', tookOff: 'The cape is off.',
      playStart: 'Change an option on the left and the preview on the right follows.',
      upIcon: 'Upload icon', upBanner: 'Upload banner', picDone: 'Image updated', confirmUnguild: 'Remove {who} from the contributing guilds?',
      guild: {
        'bad name': 'A guild name is letters and spaces only, 3 to 30 characters',
        'bad tag': 'A tag is 3 or 4 letters',
        already: 'This guild is already listed',
        'bad image': 'That is not a PNG, JPEG or WebP image',
        'too large': 'The image is too large (512 KB at most)',
        'too many': 'Too many guilds are listed',
        'no such guild': 'That guild is not on the list',
      },
      previewHint: 'Trying it on: {w}×{h}, not uploaded yet.',
      noName: '(name unknown)', lift: 'Unban', confirmLift: 'Lift the ban on {who}?', liftFail: 'That did not work ({s})',
      confirmUnstaff: 'Remove {who} from the moderators?', notYet: 'Has not signed in yet',
      tr: {
        gone: "Deleted user #{id}",
        langs: { zh_tw: 'Traditional Chinese', zh_cn: 'Simplified Chinese', ja_jp: 'Japanese', ko_kr: 'Korean', ru_ru: 'Russian', es_es: 'Spanish', tr_tr: 'Turkish' },
        st: { open: 'Voting', accepted: 'Accepted', rejected: 'Rejected' },
        signin: 'Sign in to suggest and vote', roleDecide: 'Moderator: can decide', roleTranslator: 'Translator: can suggest and vote',
        roleViewer: 'View only. Ask a moderator to add you as a translator for this language', loading: 'Loading…',
        indexFail: 'Could not load the translations. Try again later.',
        hint: '{n} lines. Type a keyword, pick a file, or tick “Untranslated only” to start.', found: '{n} found, showing the first {shown}',
        untranslated: '(not translated yet)', propose: 'Suggest', view: 'See suggestions', openProps: '{n} suggestions',
        notTranslator: 'You are not a translator for this language yet, so you can only look.', signinToPropose: 'Sign in to suggest.',
        yours: 'Your translation', send: 'Send', looksOk: 'Placeholders match', by: 'by {who}', votes: '{n} votes', vote: 'Vote',
        voted: 'Voted (press again to take it back)', accept: 'Accept', reject: 'Reject', prLink: 'Pull request', none: 'Nothing here yet.',
        seeItems: 'See {n} translations', noPolls: 'No votes have been started yet.', decided: 'Decided', decide: 'Decide',
        addOption: 'Add a candidate', add: 'Add', reopen: 'Reopen', opened: 'Started', failed: 'That did not work ({s})',
        preview: 'In-game preview', pvSrc: 'Source', pvDst: 'Translation', reroll: 'Other numbers', insert: 'Insert',
        pvNote: 'The numbers are made up; the small digit beside each one shows which number of the source it takes. {p} becomes a place, {u} a name, and grey squares are icons.',
        autonum: 'Number them', autonumHint: 'Turns each {~} in your translation into {~1}, {~2}… in order, so you can move them around',
        pvOver: 'The source only has {n} numbers',
        why: {
          empty: 'The translation is empty', 'too long': 'Too long (2000 characters at most)',
          placeholders: '{#}, {p} or {u} differ from the source in number or order ({u} may be dropped, not added)',
          numbers: 'The number of {~} differs from the source', numbering: '{~1}, {~2} numbering is incomplete or mixed with plain {~}',
          colours: 'Colour markers {c1}…{/} differ from the current translation', lines: 'The number of lines differs from the source',
          'not a translator': 'You are not a translator for this language', 'login first': 'Please sign in first',
          closed: 'This one is already closed', forbidden: 'Only moderators can decide', 'bad term': 'Enter the English term',
          'bad option': 'A candidate is too long or there are too many', 'too many': 'Too many candidates',
          'line not found': 'This line was not found (it may have just changed). Reload and try again',
          'github not configured': 'GitHub is not set up for the website', 'github unreachable': 'Could not reach GitHub. Try again later',
        },
      },
      fb: {
        langNone: '(not specified)', whereTr: 'Where: which line, screen or quest (recommended)', whereAny: 'Where (optional)',
        wherePhTr: 'e.g. the first line the Detlas blacksmith says', wherePh: 'e.g. the cape gallery, or the Dialogue page in the F6 settings',
        count: '{n} / {max}', as: 'Sending as Discord “{n}”', done: 'Got it, thank you for the feedback!',
        issueLink: 'See the issue on GitHub', issueFail: 'The GitHub issue could not be opened this time; your feedback is saved and a moderator will open it later.',
        why: {
          'bad category': 'Pick a category', 'too short': 'Please write a little more (at least 3 characters)',
          'too long': 'That is too long (2000 characters at most)', 'where too long': '“Where” is too long (200 characters at most)',
          'slow down': 'You can send 5 per hour; you can send again after {t}', 'bad request': 'That could not be read; reload the page and try again',
        },
        cats: { translation: 'Translation suggestion', content: 'Content suggestion', website: 'Website suggestion', feature: 'Feature suggestion', other: 'Other' },
        st: { open: 'Open', done: 'Handled' },
        via: { web: 'Website', bot: 'Discord /feedback' },
        counts: '{open} open · {done} handled', where: 'Where: {s}', lang: 'Language: {s}', uid: 'Discord user ID: {s}',
        adminIssue: 'GitHub issue #{n}', issueErr: 'Issue not opened ({s})', handled: 'Marked handled by {who} on {t}',
        markDone: 'Mark handled', reopen: 'Reopen', retry: 'Open issue again', del: 'Delete',
        confirmDel: 'Delete this feedback? It cannot be recovered (an issue already opened on GitHub stays open).',
      },
      findNone: 'No registered member matches. Pressing “Add” uses exactly what you typed.', isStaff: 'Already a moderator', chosen: 'Selected: {who}',
      findCount: '{n} shown ({all} registered)',
      staff: {
        'bad who': 'That is not a Minecraft ID, a UUID, or a Discord user ID (17 to 20 digits)',
        'bad note': 'The note can be 60 characters at most',
        already: 'This person is already a moderator',
      },
      revoke: 'Remove', confirmRevoke: 'Remove {who} from the special list?', perkDone: 'Added',
      perk: {
        'bad uuid': 'A UUID is 32 hexadecimal characters, with or without hyphens',
        'bad note': 'The note can be 60 characters at most',
        already: 'This UUID is already on the list',
      },
      sending: 'Sending…', prDone: 'Pull request opened', prFail: 'That did not work ({s})', botOk: 'Registered: {s}',
      credit: {
        'bad name': 'The display name cannot be empty or longer than 40 characters',
        'bad mc': 'A Minecraft ID is letters, digits and underscores, 16 at most',
        'bad color': 'Write the colour as #RRGGBB',
        'bad section': 'Unknown role',
        'already listed': 'That Minecraft ID is already in this section',
        'github not configured': 'GITHUB_TOKEN is not set, so no pull request can be opened (see tools/presence/README.md)',
        forbidden: 'Your sign-in expired, or you are not on the developer list',
      },
    },
  },
};

Object.assign(TEXT, MORE_TEXT);

const LANG_NAMES = {
  zh: '繁體中文', 'zh-cn': '简体中文', en: 'English', ja: '日本語', ko: '한국어',
  ru: 'Русский', es: 'Español', tr: 'Türkçe',
};

export const LANGS = Object.keys(LANG_NAMES).filter((code) => Object.hasOwn(TEXT, code));

export function tagOf(lang) {
  return Object.hasOwn(TEXT, lang) && TEXT[lang].html ? TEXT[lang].html : 'en';
}


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

const FONT_CSS = Object.keys(FONTS).map((code) => '\n  @font-face { font-family: Px' + code.toUpperCase()
  + '; src: url(/font/px-' + code + '.ttf?v=' + FONTS[code].v + ') format("truetype"); font-display: swap; }').join('');

const TEX_URL = Object.fromEntries(Object.keys(TEXTURES).map((name) =>
  [name, '/tex/' + name + '?v=' + TEXTURES[name].v]));
const TEX_CSS = '\n  :root {' + Object.keys(TEX_URL).map((name) =>
  ' --t-' + name.replace(/\.\w+$/, '') + ': url(' + TEX_URL[name] + ');').join('') + ' }';

const TPL = (name) => '/template/' + name + '?v=' + CAPE_TEMPLATES[name].v;


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

function layout(lang, title, active, body, script) {
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
    + '<script>var L = %%js%%;' + COMMON + '</script>\n' + (script || '') + '\n</body>\n</html>';
}


const leaf = (colour) => '<span style="color:' + colour + ';display:flex">' + LEAF + '</span>';

const STAGE_PARTS = '<template id="stage-parts">'
  + '<svg data-part="cursor" viewBox="0 0 14 18" aria-hidden="true"><path d="M1 1v13l3.5-3 2.4 5.2 2-.9L6.5 10H11z" fill="#fff" stroke="#000" stroke-width="1"/></svg>'
  + '</template>';

const STAGE_JS = String.raw`
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

const HOME_JS = '<script>var D = %%demo%%;' + STAGE_JS + String.raw`
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
` + '</script>';


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

const FEATURES_JS = '<script>var D = %%demo%%;' + STAGE_JS + String.raw`
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
` + '</script>';


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

const CAPES_JS = SKINVIEW + '<script>' + VIEWER + String.raw`
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
` + '</script>';


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

const ACCOUNT_JS = SKINVIEW + '<script>' + VIEWER + String.raw`
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
` + '</script>';


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

const FEEDBACK_JS = '<script>' + String.raw`
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
` + '</script>';

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

const ADMIN_JS = '<script>' + String.raw`
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
` + '</script>';

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

const TRANSLATE_JS = '<script>' + String.raw`
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
` + '</script>';

const PAGES = {
  '/': ['home.title', HOME, HOME_JS + '<script>revealOnScroll(' + JSON.stringify(REVEAL_HOME) + ')</script>'],
  '/features': ['features.title', FEATURES, FEATURES_JS + '<script>revealOnScroll(' + JSON.stringify(REVEAL_FEATURES) + ')</script>'],
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

function plain(html) {
  return String(html).replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/\s+/g, ' ').trim();
}

function attribute(text) {
  return text.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
}

function filled(code) {
  const own = TEXT[code];
  if (code === 'en') return own;
  const base = TEXT.en;
  const js = { ...base.js, ...(own.js || {}) };
  js.why = { ...base.js.why, ...((own.js && own.js.why) || {}) };
  js.credit = { ...base.js.credit, ...((own.js && own.js.credit) || {}) };
  js.staff = { ...base.js.staff, ...((own.js && own.js.staff) || {}) };
  js.guild = { ...base.js.guild, ...((own.js && own.js.guild) || {}) };
  js.tr = { ...base.js.tr, ...((own.js && own.js.tr) || {}) };
  js.tr.why = { ...base.js.tr.why, ...((own.js && own.js.tr && own.js.tr.why) || {}) };
  js.tr.st = { ...base.js.tr.st, ...((own.js && own.js.tr && own.js.tr.st) || {}) };
  js.tr.langs = { ...base.js.tr.langs, ...((own.js && own.js.tr && own.js.tr.langs) || {}) };
  js.translator = { ...base.js.translator, ...((own.js && own.js.translator) || {}) };
  js.fb = { ...base.js.fb, ...((own.js && own.js.fb) || {}) };
  for (const part of ['why', 'cats', 'st', 'via']) {
    js.fb[part] = { ...base.js.fb[part], ...((own.js && own.js.fb && own.js.fb[part]) || {}) };
  }
  return { ...base, ...own, js };
}

function fromTag(tag) {
  if (tag === 'zh' || tag.startsWith('zh-')) {
    const simplified = /^zh-(?:cn|sg|my|hans)\b/.test(tag) && LANGS.includes('zh-cn');
    return simplified ? 'zh-cn' : 'zh';
  }
  const primary = tag.split('-')[0];
  return primary !== 'zh' && LANGS.includes(primary) ? primary : null;
}

export function pickLang(cookie, accept) {
  const chosen = /(?:^|;\s*)lang=([a-z-]+)/.exec(cookie || '');
  if (chosen && LANGS.includes(chosen[1])) return chosen[1];
  for (const part of String(accept || '').toLowerCase().split(',')) {
    const hit = fromTag(part.split(';')[0].trim());
    if (hit) return hit;
  }
  return 'en';
}
