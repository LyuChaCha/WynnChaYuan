/**
 * wynnchayuan.com 的網頁：首頁、功能、披風一覽、帳號、管理。中文與英文各一份。
 *
 * 五頁共用一份樣式、頁首與頁尾，各自一段程式。全部是靜態的字串，資料都跟 /api 要。
 *
 * 語言：頁面的樣板用 %%鍵%% 標位置，render() 填進去。繁體中文與英文的字在下面的 TEXT
 * （這兩份是正本，改字先改這裡）；其他語言在 lang/<代碼>.json，由 build-site-text.mjs
 * 併成 site-text.js。用哪一種由 worker 決定（訪客選過的那個 cookie，沒選過就看瀏覽器的
 * 語言）；頁首右邊可以切換。加一種語言就是在 lang/ 加一份、在 LANG_NAMES 加一個名字。
 *
 * 登入：網站沒辦法自己確認 Minecraft 帳號，所以是<b>從遊戲裡開</b>——模組拿通行證換一條
 * 網址（#t=…），這裡把它收進 localStorage、從網址列上拿掉，之後每個 API 請求帶著它。
 *
 * 頁面裡的程式刻意不用樣板字串（反引號、錢字號加大括號）：整頁是包在樣板字串裡的。
 */

import { MORE_TEXT } from './site-text.js';
import { DEMO } from './demo-data.js';

const STYLE = String.raw`
  :root {
    --ground: #0f1512; --panel: #161e19; --panel-2: #1c2620; --line: #2a382f;
    --ink: #e9efe9; --ink-2: #a3b3a7; --leaf: #7fd08a; --leaf-deep: #2f7d46;
    --gold: #d8b765; --cyan: #5bd8e8; --danger: #e08a8a;
    /* 動作的節奏：進場用 --arrive（先快後慢），按壓與懸停用 --out；回饋 140ms、狀態 200ms */
    --out: cubic-bezier(0.23, 1, 0.32, 1); --arrive: cubic-bezier(0.16, 1, 0.3, 1); --move: cubic-bezier(0.77, 0, 0.175, 1);
    --fast: 140ms; --base: 200ms;
  }
  * { box-sizing: border-box; }
  /* 換頁：內容淡過去，頁首留在原地不閃。很短——換頁是常做的事，動畫不該讓人等 */
  @view-transition { navigation: auto; }
  ::view-transition-old(root), ::view-transition-new(root) { animation-duration: 150ms; animation-timing-function: ease; }
  /* 披風一覽換排序、換篩選：每張卡滑到新的位置（在畫面上移動的東西用兩頭慢的曲線） */
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
  /* 放不下的時候（字比較長的語言、登入後多了名字與「管理」）右邊那一組整組換到下一列，選單本身不折行 */
  .top .in { max-width: 1120px; margin: 0 auto; padding: 12px 20px; display: flex; flex-wrap: wrap; align-items: center; gap: 8px 22px; }
  .brand { display: flex; align-items: center; gap: 10px; color: var(--ink); font-weight: 650; font-size: 17px; letter-spacing: .02em; }
  .brand:hover { text-decoration: none; }
  .brand span { color: var(--leaf); display: flex; }
  .brand svg { width: 26px; height: 26px; }
  nav { display: flex; gap: 4px; flex-wrap: wrap; }
  .top nav { flex-wrap: nowrap; white-space: nowrap; }
  nav a { padding: 6px 12px; border-radius: 8px; color: var(--ink-2); font-size: 14px; transition: color var(--fast) ease, background-color var(--fast) ease; }
  nav a:hover { color: var(--ink); background: var(--panel-2); text-decoration: none; }
  nav a[aria-current="page"] { color: var(--ink); background: var(--panel-2); }
  .tools { margin-left: auto; display: flex; align-items: center; gap: 12px; }
  .ico { width: 16px; height: 16px; flex: none; }
  .btn.small { padding: 5px 11px; font-size: 13px; border-radius: 8px; gap: 6px; }
  /* 頁首右邊：贊助（Ko-fi）與下載。下載按了往下開一張小選單，三個地方挑一個 */
  .kofi { display: inline-flex; align-items: center; gap: 6px; padding: 6px 10px; border-radius: 8px; color: var(--ink-2); font-size: 14px; transition: color var(--fast) ease, background-color var(--fast) ease; }
  .kofi:hover { text-decoration: none; }
  .pop { position: relative; }
  .menu {
    position: absolute; right: 0; top: calc(100% + 8px); z-index: 6; width: 252px; padding: 6px;
    border: 1px solid var(--line); border-radius: 12px; background: var(--panel); box-shadow: 0 18px 44px #000a;
    opacity: 0; visibility: hidden; transform: translateY(-4px) scale(.97); transform-origin: top right;
    transition: opacity var(--fast) ease, transform var(--fast) var(--out), visibility 0s linear var(--fast);
  }
  .menu.open { opacity: 1; visibility: visible; transform: none; transition: opacity var(--base) ease, transform var(--base) var(--arrive), visibility 0s; }
  .menu > a { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 8px 10px; border-radius: 8px; color: var(--ink); font-size: 14px; transition: background-color var(--fast) ease; }
  .menu > a:hover { text-decoration: none; }
  .menu > a .ico { color: var(--ink-2); }
  .menu p { margin: 6px 0 0; padding: 9px 10px 5px; border-top: 1px solid var(--line); color: var(--ink-2); font-size: 12px; line-height: 1.55; }
  select.lang { width: auto; font: inherit; font-size: 13px; padding: 5px 8px; border-radius: 8px; border: 1px solid var(--line); background: var(--panel); color: var(--ink-2); cursor: pointer; transition: color var(--fast) ease, border-color var(--fast) ease; }
  select.lang:hover { color: var(--ink); border-color: var(--leaf-deep); }
  select.pick { width: auto; font: inherit; font-size: 13px; padding: 5px 8px; border-radius: 8px; border: 1px solid var(--line); background: var(--panel); color: var(--ink); cursor: pointer; transition: border-color var(--fast) ease; }
  select.pick:hover { border-color: var(--leaf-deep); }
  /* 同一個漢字在各語言的寫法不一樣：照頁面的語言挑字型，不要一律拿繁體中文的字型去畫 */
  :lang(zh-Hans) body { font-family: "Segoe UI", "Microsoft YaHei", "PingFang SC", "Noto Sans SC", system-ui, sans-serif; }
  :lang(ja) body { font-family: "Segoe UI", "Yu Gothic UI", "Hiragino Sans", "Noto Sans JP", "Meiryo", system-ui, sans-serif; }
  :lang(ko) body { font-family: "Segoe UI", "Malgun Gothic", "Apple SD Gothic Neo", "Noto Sans KR", system-ui, sans-serif; }
  .me { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--ink-2); }
  .me img { width: 22px; height: 22px; border-radius: 4px; image-rendering: pixelated; }
  main { flex: 1; width: 100%; max-width: 1120px; margin: 0 auto; padding: 44px 20px 72px; }
  h1 { margin: 0; font-size: 28px; font-weight: 650; letter-spacing: .02em; text-wrap: balance; }
  /* 大段之間留寬一點、標題貼著自己的內容：寬與緊的差距才看得出哪幾塊是一組 */
  h2 { margin: 56px 0 14px; font-size: 19px; font-weight: 650; }
  .lede { margin: 6px 0 0; color: var(--ink-2); max-width: 62ch; text-wrap: pretty; }
  .panel { padding: 16px 18px; border: 1px solid var(--line); border-radius: 12px; background: var(--panel); }
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
  /* 懸停只給真的有滑鼠的裝置：觸控螢幕點一下會卡在懸停的樣子 */
  @media (hover: hover) and (pointer: fine) {
    .btn:hover { border-color: var(--leaf-deep); }
    .btn.primary:hover { background: #93dba0; border-color: #93dba0; }
    .btn.danger:hover { background: #3d2020; border-color: #5a2b2b; }
    tbody tr:hover td { background: #ffffff06; }
    .kofi:hover { color: #f2a7b6; background: var(--panel-2); }
    .menu > a:hover { background: var(--panel-2); }
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

  .hero { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 36px; align-items: center; padding: 18px 0 8px; }
  .hero h1 { font-size: 40px; line-height: 1.25; }
  .hero .lede { font-size: 16px; }
  .hero img { width: 100%; max-width: 380px; height: auto; justify-self: center; filter: drop-shadow(0 18px 40px #000a); }
  .hero > div > * { animation: rise 520ms var(--arrive) both; }
  .hero > div > :nth-child(2) { animation-delay: 60ms; }
  .hero > div > :nth-child(3) { animation-delay: 120ms; }
  .hero > div > :nth-child(4) { animation-delay: 180ms; }
  .hero img { animation: settle 720ms var(--arrive) 80ms both; }
  @keyframes rise { from { opacity: 0; transform: translateY(10px); } }
  @keyframes settle { from { opacity: 0; transform: scale(.94) rotate(-4deg); } }
  @keyframes fade { from { opacity: 0; } }
  .cta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 22px; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 18px; }
  .chip { padding: 3px 11px; border: 1px solid var(--line); border-radius: 999px; font-size: 13px; color: var(--ink-2); background: var(--panel); }
  .features { display: grid; gap: 0 32px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .features > div { padding: 16px 0 20px; border-top: 1px solid var(--line); }
  .features h3 { margin: 0 0 4px; font-size: 15px; font-weight: 650; }
  .features p { margin: 0; color: var(--ink-2); font-size: 14px; max-width: 38ch; }
  .more { margin: 14px 0 0; font-size: 14px; }
  .more a { display: inline-flex; align-items: center; gap: 6px; }
  .more .ico { transition: transform var(--fast) var(--out); }
  /* 團隊：三個身分各一段，一個人一列（頭像、名字、遊戲 ID、葉子）；點下去是他在 Wynncraft 的頁面 */
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
  /* 貢獻公會：比一個人大一號——旗幟、圖示、名字與標籤 */
  .lineup.guilds { grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); }
  .guild { padding: 10px 14px 10px 12px; border-radius: 12px; }
  .guild img { opacity: 0; transition: opacity 240ms ease; }
  .g-banner { width: 28px; height: 56px; flex: none; object-fit: contain; image-rendering: pixelated; filter: drop-shadow(0 4px 8px #0009); }
  .g-icon { width: 56px; height: 56px; flex: none; border-radius: 12px; object-fit: contain; background: #f4f8f6; }
  span.g-icon { display: grid; place-items: center; background: var(--panel-2); color: var(--ink-2); font-size: 13px; font-weight: 650; letter-spacing: .04em; }
  .g-name { display: grid; min-width: 0; flex: 1; }
  .g-name b { font-size: 16px; font-weight: 650; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .g-name span { font-size: 13px; color: var(--ink-2); }
  .guild .ico, .who-row .ico { color: var(--ink-2); transition: color var(--fast) ease, transform var(--fast) var(--out); }
  .everyone { display: flex; align-items: center; gap: 10px; margin: 16px 0 0; color: var(--ink-2); font-size: 14px; }
  .everyone svg { width: 20px; height: 20px; }
  .everyone b { margin-right: .7em; color: var(--ink); font-weight: 600; }

  /* 功能頁：最上面是即時預覽——照遊戲裡設定畫面右邊那一塊做，左邊幾個真的選項、右邊一張小場景 */
  .play { margin-top: 26px; border: 1px solid var(--line); border-radius: 14px; background: var(--panel); overflow: hidden; }
  .play-bar { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 10px 18px; padding: 12px 14px; border-bottom: 1px solid var(--line); }
  .play-lang { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 13px; color: var(--ink-2); }
  .seg { position: relative; display: inline-flex; flex-wrap: wrap; gap: 2px; padding: 3px; border: 1px solid var(--line); border-radius: 10px; background: var(--ground); }
  /* 選到的那一顆底下的那一塊：換一顆就滑過去（程式放的，見 slide()）。沒有程式的時候照舊是按鈕自己的底色 */
  .seg .pill { position: absolute; left: 0; top: 0; border-radius: 7px; background: var(--panel-2); box-shadow: inset 0 0 0 1px var(--line); pointer-events: none; transition: transform 220ms var(--out), width 220ms var(--out), height 220ms var(--out), opacity var(--fast) ease; }
  .seg button { position: relative; }
  .seg.live button[aria-pressed="true"] { background: none; box-shadow: none; }
  .seg button { font: inherit; font-size: 13px; padding: 5px 12px; border: 0; border-radius: 7px; background: none; color: var(--ink-2); cursor: pointer; transition: color var(--fast) ease, background-color var(--fast) ease, transform var(--fast) var(--out); }
  .seg button:active { transform: scale(.97); }
  .seg button[aria-pressed="true"] { background: var(--panel-2); color: var(--ink); box-shadow: inset 0 0 0 1px var(--line); }
  .play-body { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr); }
  .play-set { display: grid; align-content: start; padding: 6px 16px 10px; }
  .set-row { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px 14px; min-height: 52px; padding: 9px 0; border-bottom: 1px solid var(--line); font-size: 14px; transition: opacity var(--base) ease; }
  .set-row:last-child { border-bottom: 0; }
  .set-row > span { font-weight: 600; }
  .set-row.dim { opacity: .45; }
  .switch { position: relative; width: 40px; height: 22px; flex: none; padding: 0; border: 1px solid var(--line); border-radius: 999px; background: var(--ground); cursor: pointer; transition: background-color var(--base) ease, border-color var(--base) ease; }
  .switch::after { content: ""; position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; border-radius: 50%; background: var(--ink-2); transition: transform var(--base) var(--out), background-color var(--base) ease; }
  .switch[aria-checked="true"] { background: var(--leaf-deep); border-color: var(--leaf-deep); }
  .switch[aria-checked="true"]::after { transform: translateX(18px); background: #fff; }
  .range { display: flex; align-items: center; gap: 10px; }
  .range input { width: 150px; }
  .range output { min-width: 6ch; text-align: right; font-variant-numeric: tabular-nums; color: var(--ink-2); }
  .play-view { padding: 12px 14px 14px; border-left: 1px solid var(--line); background: #0d1310; }
  .play-view h3 { display: flex; align-items: center; gap: 8px; margin: 0 0 10px; font-size: 12px; font-weight: 600; color: var(--ink-2); }
  .play-view h3::before { content: ""; width: 6px; height: 6px; border-radius: 50%; background: var(--leaf); }
  /* 場景：很淡的格線讓人一眼看出這是示意圖。裡面每一塊的位置照遊戲裡的擺 */
  .pstage {
    position: relative; height: 360px; overflow: hidden; border: 1px solid var(--line); border-radius: 10px; font-size: 14px; line-height: 1.45; color: #fff;
    background-color: #0a0f0d; background-size: 24px 24px;
    background-image: linear-gradient(#ffffff0d 1px, transparent 1px), linear-gradient(90deg, #ffffff0d 1px, transparent 1px);
  }
  .pstage > * { animation: fade 180ms ease both; }
  .pstage.still > * { animation: none; }
  .gbox { padding: 5px 9px 6px; border: 1px solid var(--leaf); background: #0a1018f2; color: var(--ink); box-shadow: 0 0 0 3px #7fd08a1c; white-space: nowrap; max-width: 100%; overflow: hidden; text-overflow: ellipsis; }
  .tip { position: relative; padding: 5px 9px 6px; border: 1px solid #4b2a8a; background: #160a22f2; white-space: nowrap; }
  .tip > div:nth-of-type(2), .tipside > div:nth-of-type(2) { color: #b8c0cc; }
  .tip > div:nth-of-type(3), .tipside > div:nth-of-type(3) { color: #f08a8a; }
  .tip > div:nth-of-type(4), .tipside > div:nth-of-type(4) { color: #7fc8e8; }
  .tipside > div:first-child { color: #fff; }
  .tip > svg { position: absolute; left: -11px; top: -13px; width: 14px; height: 18px; filter: drop-shadow(1px 1px 0 #000a); }
  .duo { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; padding: 0 12px; font-size: 16px; }
  .gapmark { position: relative; align-self: stretch; flex: none; transition: width 160ms var(--out); }
  .gapmark::before { content: ""; position: absolute; left: 0; right: 0; top: 50%; height: 7px; margin-top: -58px; border: solid var(--leaf); border-width: 0 1px; background: linear-gradient(var(--leaf), var(--leaf)) center / 100% 1px no-repeat; }
  .gapmark span { position: absolute; left: 50%; top: 50%; margin-top: -82px; transform: translateX(-50%); color: var(--leaf); font-size: 12px; white-space: nowrap; }
  .stack { position: absolute; inset: 14px; display: flex; flex-direction: column; justify-content: flex-end; align-items: flex-start; gap: 8px; }
  .stack .gbox { white-space: normal; }
  .stack .right { align-self: flex-end; }
  .dlgbox { align-self: stretch; padding: 8px 12px 10px; border: 2px solid #7a5c2e; background: #22180ef2; color: #f1e3c4; }
  .dlgbox b { display: block; font-weight: 600; color: #f5c56b; }
  .dlgbox p { margin: 0 0 4px; min-height: 2.9em; }
  .dlgbox .choice { color: #c9b38a; }
  .w-boss { position: absolute; top: 10px; left: 50%; transform: translateX(-50%); text-align: center; color: #f0a0a0; white-space: nowrap; }
  .w-boss i { display: block; width: 130px; height: 4px; margin: 3px auto 0; background: linear-gradient(90deg, #c0392b 62%, #3a1414 62%); }
  .w-track { position: absolute; top: 46px; right: 10px; max-width: 52%; display: grid; justify-items: end; gap: 4px; }
  .w-track .box { padding: 4px 8px 5px; border: 1px solid #ffffff22; background: #080c12b8; white-space: nowrap; max-width: 100%; overflow: hidden; text-overflow: ellipsis; }
  .w-track .box.framed { border-color: var(--leaf); }
  .w-track .box div:first-child { color: #f5c56b; }
  .w-track .box div:last-child { color: #8fd694; }
  .w-npc { position: absolute; left: 14px; top: 112px; display: flex; align-items: flex-start; gap: 6px; max-width: 64%; }
  .w-npc .plate { padding: 1px 6px 2px; background: #00000080; color: #fff; white-space: nowrap; }
  .w-npc .man { display: grid; justify-items: center; gap: 4px; }
  .w-npc .man::after { content: ""; width: 18px; height: 42px; background: linear-gradient(#b98b5e 0 18px, #4c6a8c 18px); clip-path: polygon(0 0, 100% 0, 100% 18px, 86% 18px, 86% 100%, 14% 100%, 14% 18px, 0 18px); }
  .w-title { position: absolute; left: 0; right: 0; top: 214px; text-align: center; color: #f5c56b; font-size: 18px; font-weight: 600; text-shadow: 1px 1px 0 #000a; pointer-events: none; }
  .w-board { position: absolute; right: 10px; top: 144px; max-width: 62%; padding: 3px 8px 4px; background: #00000066; color: #b8c0cc; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .w-board div:first-child { color: #f5c56b; }
  .w-chat { position: absolute; left: 10px; bottom: 52px; max-width: 66%; padding: 3px 8px 4px; background: #00000066; color: #b8c0cc; }
  .w-chat div { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .w-chat .ours { color: var(--leaf); }
  .w-held { position: absolute; left: 0; right: 0; bottom: 8px; display: grid; justify-items: center; gap: 3px; }
  .w-held i { display: flex; gap: 2px; }
  .w-held u { width: 16px; height: 16px; border: 1px solid #55606a; background: #00000080; }
  .w-held u:first-child { border-color: #e6edf5; }
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
  .rows { margin: 0; border-bottom: 1px solid var(--line); }
  .rows > div { display: grid; grid-template-columns: 210px minmax(0, 1fr); gap: 2px 24px; padding: 13px 0; border-top: 1px solid var(--line); }
  .rows dt { font-weight: 650; }
  .rows dd { margin: 0; color: var(--ink-2); max-width: 64ch; }
  .state { display: inline-block; margin-left: 6px; padding: 0 8px; border: 1px solid var(--line); border-radius: 999px; font-size: 11px; font-weight: 600; line-height: 1.7; color: var(--ink-2); vertical-align: 1px; white-space: nowrap; }
  .state.need { color: var(--gold); border-color: #5c4d27; }
  .plain { margin: 10px 0 0; padding-left: 1.2em; color: var(--ink-2); }
  .plain li { margin: 4px 0; } .plain b { color: var(--ink); font-weight: 600; }
  .split { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 14px; }

  .bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 18px; margin: 22px 0 14px; }
  .count { font-variant-numeric: tabular-nums; color: var(--ink-2); }
  .count b { color: var(--ink); font-weight: 600; }
  .grid { display: grid; gap: 18px; grid-template-columns: repeat(auto-fill, minmax(168px, 1fr)); }
  /* 一張卡：整張是「點開來看」的按鈕，右上角另外一顆星星（加進我的最愛）——按鈕不能包按鈕，所以是兩顆並排在同一個框裡 */
  .card {
    position: relative; display: flex; flex-direction: column;
    background: var(--panel); border: 1px solid var(--line); border-radius: 12px; overflow: hidden;
    transition: transform 180ms var(--out), border-color 180ms ease, box-shadow 180ms ease;
    /* 一覽載進來的時候一張接一張出現；--i 是第幾張，最多排到第 10 張，後面的一起出來 */
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
  /* 還在跟伺服器要資料的時候：先放幾張同樣大小的空卡，版面不會跳 */
  .card.skel { cursor: default; animation: pulse 1.4s ease-in-out infinite; }
  .card.skel .stage { height: 190px; }
  .card.skel .meta { height: 43px; }
  @keyframes pulse { 50% { opacity: .55; } }
  .stage { display: grid; place-items: center; padding: 20px 0 16px; background: linear-gradient(180deg, #1b2a21 0%, #131b16 100%); }
  .face { width: 96px; aspect-ratio: 10 / 16; overflow: hidden; border-radius: 3px; box-shadow: 0 8px 18px #000a, 0 0 0 1px #0006; background: #0b0f0d; }
  .face img { display: block; width: 220%; max-width: none; margin: -10% 0 0 -10%; image-rendering: pixelated; opacity: 0; transition: opacity 240ms ease; }
  .face img.in { opacity: 1; }
  .meta { display: flex; align-items: center; gap: 8px; padding: 10px 12px 12px; font-size: 13px; color: var(--ink-2); }
  .tag { padding: 1px 7px; border-radius: 999px; font-size: 11px; font-weight: 600; letter-spacing: .06em; color: var(--ground); background: var(--ink-2); }
  .tag.gif { background: var(--gold); }
  .meta .who { margin-left: auto; font-variant-numeric: tabular-nums; }
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
  .form { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); align-items: end; }
  /* 上傳：可以把圖檔拖進來。拖到上面的時候框會亮起來 */
  .drop { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 12px; padding: 14px; border: 1px dashed var(--line); border-radius: 10px; transition: border-color var(--fast) ease, background-color var(--fast) ease; }
  .drop.over { border-color: var(--leaf); background: #7fd08a12; }
  .drop .hint { flex-basis: 100%; }
  @media (max-width: 1240px) {
    .kofi span { display: none; }
  }
  @media (max-width: 940px) {
    .foot { grid-template-columns: 1fr; }
    .features { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    /* 窄一點的畫面：頁首拆成幾列（標誌與工具、選單），所以不黏在上面，免得佔掉半個畫面 */
    .top { position: static; }
    .top .in { gap: 8px 10px; flex-wrap: wrap; }
    .top nav { order: 3; flex-basis: 100%; flex-wrap: nowrap; overflow-x: auto; scrollbar-width: none; margin: 0 -8px; }
    .top nav a { white-space: nowrap; }
    .tools { gap: 8px; }
  }
  @media (max-width: 720px) {
    .hero, .account { grid-template-columns: 1fr; }
    .hero h1 { font-size: 30px; }
    .view { grid-template-columns: 1fr; }
    canvas.model { height: 320px; }
    .side { border-left: 0; border-top: 1px solid var(--line); }
    .me a { max-width: 96px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .menu { left: 0; right: auto; transform-origin: top left; }
    .foot-cols { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .band { grid-template-columns: 1fr; }
    .lineup, .lineup.guilds { grid-template-columns: 1fr; }
    .features { grid-template-columns: 1fr; }
    .play-body { grid-template-columns: 1fr; }
    .play-view { order: -1; border-left: 0; border-bottom: 1px solid var(--line); }
    .pstage { height: 350px; }
    .range input { width: 120px; }
    .spec { grid-template-columns: 1fr; margin-top: 36px; }
    .spec-nav { position: static; display: flex; flex-wrap: wrap; gap: 4px; margin: 0 -10px 10px; }
    .rows > div { grid-template-columns: 1fr; }
  }
  /* 不想看動來動去的人：位移與縮放都拿掉，只留淡入與顏色（回饋還是看得出來） */
  @media (prefers-reduced-motion: reduce) {
    html { scroll-behavior: auto; }
    .btn:active, .card:has(.open:active), .card:hover, .card:hover:has(.open:active), .star:active { transform: none; }
    .hero > div > *, .hero img, .card, .fig { animation-name: fade; animation-duration: 200ms; animation-delay: 0ms; }
    .more a:hover .ico, .seg button:active, a.who-row:active, .guild:active, a.who-row:hover .ico, .guild:hover .ico { transform: none; }
    .menu, .menu.open { transform: none; }
    .pstage > * { animation: none; }
    .gapmark, .switch::after { transition: none; }
    .card.skel { animation: none; }
    dialog, dialog[open] { transform: none; }
    .seg .pill { transition: none; }
    @view-transition { navigation: none; }
    @starting-style { dialog[open] { transform: none; } }
  }
`;

const LEAF = String.raw`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 21c.5-4.5 2.5-8 7-10"/><path d="M9 18c6.2 0 10.5-3.3 11-12V4h-4C7 4 4 8 4 13c0 1 0 3 2 5h3z"/></svg>`;

const LOGO = 'https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/logo/2026/wynnchayuan-logo-transparent-on-dark-1024.png';
const TEMPLATES = 'https://github.com/LyuChaCha/WynnChaYuan/tree/main/capes/templates';
const ISSUES = 'https://github.com/LyuChaCha/WynnChaYuan/issues';
const SIZES = '22×17, 44×34, 88×68, 176×136, 352×272, 704×544, 1056×816';
const REPO = 'https://github.com/LyuChaCha/WynnChaYuan';
const MODRINTH = 'https://modrinth.com/mod/wynnchayuan';
const CURSEFORGE = 'https://www.curseforge.com/minecraft/mc-mods/wynnchayuan';
const RELEASES = REPO + '/releases/latest';
const KOFI = 'https://ko-fi.com/lyuchacha';

/** 小圖示：跟葉子同一種畫法（線條、圓頭、1.6 的粗細）。 */
const stroke = (paths) => '<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" '
  + 'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + paths + '</svg>';
const ICON = {
  get: stroke('<path d="M12 4v11"/><path d="m7 11 5 5 5-5"/><path d="M5 20h14"/>'),
  heart: stroke('<path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7a4.3 4.3 0 0 1 7.5 2.8C19.5 15.4 12 20 12 20z"/>'),
  down: stroke('<path d="m7 10 5 5 5-5"/>'),
  out: stroke('<path d="M8 16 16 8"/><path d="M9.5 8H16v6.5"/>'),
  go: stroke('<path d="M5 12h14"/><path d="m13 6 6 6-6 6"/>'),
  flag: stroke('<path d="M6 21V4"/><path d="M6 5h12l-2.5 4 2.5 4H6"/>'),
};

// ------------------------------------------------------------------ 字

export const TEXT = {
  zh: {
    html: 'zh-Hant',
    'nav.home': '首頁', 'nav.features': '功能', 'nav.capes': '披風', 'nav.account': '帳號', 'nav.admin': '管理', 'nav.label': '主選單',
    'lang.label': '語言',
    'top.get': '下載', 'top.sponsor': '贊助',
    'top.getnote': '需要 Wynntils 與 Fabric API，jar 放進 mods 資料夾就好。<a href="/#install">安裝說明</a>',
    'foot.label': '頁尾',
    'foot.tagline': 'Wynncraft 的多語言翻譯模組。原文留著，譯文就在旁邊。',
    'foot.get': '下載', 'foot.project': '專案', 'foot.support': '支持', 'foot.contact': '聯絡',
    'foot.changelog': '更新日誌', 'foot.license': '程式授權：AGPL-3.0', 'foot.datalicense': '譯文授權：CC BY-NC-SA 4.0',
    'foot.issues': '回報問題', 'foot.contribute': '怎麼參與', 'foot.team': '團隊', 'foot.sponsoring': '關於贊助',
    'foot.kofi': '在 Ko-fi 贊助', 'foot.sitedev': '網站開發者：LyuChaCha', 'foot.discord': 'Discord：LyuChaCha',
    'foot.copy': '複製 Discord 名稱',
    'foot.unofficial': '玩家自製的模組，與 Wynncraft、Mojang、Microsoft 沒有關係。',

    'home.title': 'WynnChaYuan - Wynncraft 的多語言翻譯模組',
    'home.h1': '把 Wynncraft 翻成你的語言',
    'home.lede': 'WynnChaYuan 是 Wynncraft 的多語言翻譯模組（Fabric 1.21.11）。原文保留，譯文顯示在旁邊，或直接寫進原本的位置——任務、對話、物品與介面都看得懂。',
    'home.modrinth': '從 Modrinth 下載',
    'home.langs': '支援的語言',
    'home.logo': 'WynnChaYuan 的標誌：一片葉子形狀的對話框',
    'home.what': '翻了什麼',
    'home.f1': '物品與裝備', 'home.f1b': '裝備敘述、屬性、Major ID。旁邊另開面板，或寫進原本的說明框。',
    'home.f2': '任務與對話', 'home.f2b': '寫進 Wynncraft 自己的對話框，逐字打出的節奏跟原文同步；任務追蹤也有。',
    'home.f3': '技能樹', 'home.f3b': '五個職業的節點、說明與流派。',
    'home.f4': '聊天與提示', 'home.f4b': '伺服器發的訊息：任務完成、獎勵、進出區域。玩家的發言不翻。',
    'home.f5': '選單與介面', 'home.f5b': '交易市場、公會、商城，還有 Lootrun、討伐戰、地城的面板。',
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
    'capes.sort': '排序', 'capes.filter': '篩選',
    'capes.s.new': '最新上傳', 'capes.s.old': '最早上傳', 'capes.s.worn': '最多人使用', 'capes.s.stars': '人氣最高', 'capes.s.recent': '最近有人穿上',
    'capes.f.all': '全部', 'capes.f.fav': '我的最愛', 'capes.f.hist': '我用過的', 'capes.f.hidden': '不公開', 'capes.f.pending': '待審核',
    'rules': '<b>不可以上傳：</b>政治、不雅或色情、違反法規、種族歧視或仇恨的內容。違規的披風會被移除，上傳的人可能不能再傳。',
    'view.title': '披風', 'view.flat': '披風的範本圖：左邊正面、右邊背面',
    'view.type': '格式', 'view.dim': '尺寸', 'view.size': '檔案大小', 'view.users': '使用', 'view.date': '上傳',
    'view.stars': '加入最愛', 'view.approve': '通過審核', 'view.review': '上傳的人之後都要審核', 'view.forget': '從我的紀錄移除',
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
    'account.s2': '到「<b>徽章與披風</b>」那一頁。',
    'account.s3': '按「<b>披風一覽</b>」旁邊的「打開」。瀏覽器開起來的時候就是登入的。',
    'account.expire': '登入一天有效，過期了從遊戲裡再開一次。離線帳號沒辦法登入。',
    'account.banned': '這個帳號不能上傳披風。',
    'account.signout': '登出這個瀏覽器',
    'account.mine': '我的披風', 'account.none': '還沒有上傳。', 'account.remove': '移除我的披風',
    'account.worn': '這一件是從披風一覽套用的。',
    'account.public': '公開在披風一覽',
    'account.publichint': '關掉之後，別人在披風一覽上看不到你的披風，也不能穿上它；遊戲裡其他玩家照樣看得到你穿著。',
    'account.pending': '這件披風正在等審核，通過之前別人看不到。',
    'account.review': '你上傳的披風目前要先經過審核，通過之後別人才看得到。',
    'account.upload': '上傳披風',
    'account.spec': '要照<a href="' + TEMPLATES + '">範本</a>畫：' + SIZES + '。檔案大小：一般玩家 1 MB 以內，開發者、贊助者、貢獻者 20 MB 以內。PNG 大家都能傳；GIF（最多 120 格）只有開發者、贊助者、貢獻者能傳。',
    'account.pick': '選一張披風的圖', 'account.send': '上傳',

    'admin.title': '管理 - WynnChaYuan',
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
    'admin.staffhow': '填他的 Minecraft ID、Minecraft UUID，或 Discord 使用者 ID（17 到 20 位數字：在 Discord 開「開發者模式」，對他按右鍵選「複製使用者 ID」）。用 Minecraft ID 加的，等他登入一次就會改記成 UUID。加了馬上生效。',
    'admin.who': 'Minecraft ID、UUID 或 Discord 使用者 ID',
    'admin.kind': '類型', 'admin.id': '代號', 'admin.nostaff': '還沒有管理員。',
    'admin.guilds': '貢獻公會',
    'admin.guildsnote': '會出現在首頁「團隊」那一段，點下去連到那個公會在 Wynncraft 的頁面。先加名字（要跟 Wynncraft 上的一模一樣），再各上傳一張圖示與旗幟。圖會先縮小再傳，PNG、JPEG、WebP 都可以。',
    'admin.gname': '公會名稱', 'admin.gtag': '標籤（可以不填）', 'admin.gpics': '圖示與旗幟', 'admin.guild': '公會',
    'admin.noguilds': '還沒有登記公會。',

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
    'feat.t8': 'Lootrun、討伐戰、地城', 'feat.t8b': '使命、賜福、信標、Aspect、Gambit，還有結算與戰利品的面板。',
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
    'feat.s3': '會送出的資料', 'feat.s3b': '為了上面兩項，模組在你進 Wynncraft 的時候向 wynnchayuan.com 報到一次：UUID、公開的個人檔案，以及披風與徽章的設定。不送任何遊戲內容。<a href="' + REPO + '#readme">完整的說明在 README</a>',
    'feat.c1b': '物品、任務、聊天的資料都從它來；它自己的畫面也會翻。',
    'feat.c2b': '跟 Wynntils 一樣是前置。',
    'feat.c3b': '模組清單上多一顆設定按鈕。',
    'feat.c4b': '它的搜尋面板打譯名也找得到，結果在英文名旁邊附上譯名。',
    'feat.c5b': '它的價格框、獎勵畫面與設定畫面也翻成譯文；按住 Shift 看原文。',
    'feat.c6b': '它用來判斷討伐戰結果的那幾句話保留英文，連勝才不會算錯。',

    js: {
      login: '登入', loading: '讀取中…',
      count: '共 {n} 件（相同的圖算一件）', empty: '還沒有人上傳披風。', users: '{n} 人', wearing: '{n} 人',
      card: '{t} 披風，{n} 人使用',
      drag: '拖曳可以旋轉。', no3d: '立體檢視載不起來，只能看平面的圖。',
      confirmDel: '刪除這件披風（{n} 個人在用）？',
      confirmBan: '刪除這件披風，並封禁用它的 {n} 個人（之後不能再上傳）？',
      expired: '沒有成功（{s}）。登入一天過期，請從遊戲裡重新開一次。',
      readFail: '讀不到清單，請重新整理。',
      roleDev: '開發者', roleMember: '名單上的成員', roleUser: '裝了模組的玩家',
      mineHint: '{t} · {d}{s} · 拖曳可以旋轉', mineNo3d: '立體檢視載不起來。',
      discordAs: 'Discord：{n}', codeFail: '拿不到綁定碼，請再試一次', copied: '已複製',
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
        'login first': '登入過期了，請從遊戲裡重新開一次',
        'no such cape': '這件披風已經不在了，請重新整理',
        'nothing to restore': '沒有可以復原的披風',
        'in use': '現在穿著的那一件不能從紀錄移除',
        'too many': '我的最愛已經滿了（最多 200 件）',
        'no cape': '你還沒有披風',
      },
      favAdd: '加入我的最愛', favRemove: '從我的最愛移除',
      emptyFav: '還沒有加入最愛的披風。點披風右上角的星星就能加進來。', emptyHist: '還沒有使用紀錄。', emptyFilter: '沒有符合的披風。',
      tagPrivate: '不公開', tagPending: '待審核', tagMine: '我穿的', tagWorn: '套用',
      confirmReview: '把上傳這件披風的人列入審核名單？之後他們傳的披風要先通過審核，別人才看得到。',
      reviewed: '已列入審核名單（{n} 人）', reviewedNone: '上傳的人已經在審核名單上了', approved: '已通過審核',
      confirmForget: '把這一件從你的使用紀錄移除？', forgot: '已從紀錄移除',
      uploadedPending: '披風上傳好了（{w}×{h}）。要等審核通過，別人才會看到',
      wornPending: '穿上了，但要等審核通過別人才會看到。',
      confirmUnreview: '把 {who} 從審核名單移除？等審核的披風會直接放行。',
      onWynn: '在 Wynncraft 看 {n} 的資料',
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
      sending: '送出中…', prDone: 'PR 開好了', prFail: '沒有成功（{s}）',
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
    'top.get': 'Download', 'top.sponsor': 'Sponsor',
    'top.getnote': 'Needs Wynntils and Fabric API; the jar goes in your mods folder. <a href="/#install">How to install</a>',
    'foot.label': 'Footer',
    'foot.tagline': 'A multilingual translation mod for Wynncraft. The original text stays; the translation sits beside it.',
    'foot.get': 'Download', 'foot.project': 'Project', 'foot.support': 'Support', 'foot.contact': 'Contact',
    'foot.changelog': 'Changelog', 'foot.license': 'Code licence: AGPL-3.0', 'foot.datalicense': 'Translation licence: CC BY-NC-SA 4.0',
    'foot.issues': 'Report a problem', 'foot.contribute': 'How to contribute', 'foot.team': 'The team', 'foot.sponsoring': 'About sponsoring',
    'foot.kofi': 'Sponsor on Ko-fi', 'foot.sitedev': 'Website developer: LyuChaCha', 'foot.discord': 'Discord: LyuChaCha',
    'foot.copy': 'Copy the Discord name',
    'foot.unofficial': 'A fan-made mod. Not affiliated with Wynncraft, Mojang or Microsoft.',

    'home.title': 'WynnChaYuan - Wynncraft in your language',
    'home.h1': 'Wynncraft, in your language',
    'home.lede': 'WynnChaYuan is a translation mod for Wynncraft (Fabric 1.21.11). The original text stays; the translation appears next to it or is written into the same spot - quests, dialogue, items and menus.',
    'home.modrinth': 'Download on Modrinth',
    'home.langs': 'Supported languages',
    'home.logo': 'The WynnChaYuan logo: a speech bubble shaped like a leaf',
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
    'capes.sort': 'Sort by', 'capes.filter': 'Filter',
    'capes.s.new': 'Newest', 'capes.s.old': 'Oldest', 'capes.s.worn': 'Most worn', 'capes.s.stars': 'Most popular', 'capes.s.recent': 'Recently put on',
    'capes.f.all': 'All', 'capes.f.fav': 'My favourites', 'capes.f.hist': 'Worn before', 'capes.f.hidden': 'Not public', 'capes.f.pending': 'Awaiting review',
    'rules': '<b>Not allowed:</b> political, obscene or sexual, unlawful, racist or hateful content. Capes that break the rules are removed, and the uploader may lose the ability to upload.',
    'view.title': 'Cape', 'view.flat': 'The cape template image: front on the left, back on the right',
    'view.type': 'Format', 'view.dim': 'Dimensions', 'view.size': 'File size', 'view.users': 'Players', 'view.date': 'Uploaded',
    'view.stars': 'Favourited by', 'view.approve': 'Approve', 'view.review': 'Review the uploaders’ future capes', 'view.forget': 'Remove from my history',
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
    'account.s2': 'Go to the <b>Badge &amp; cape</b> page.',
    'account.s3': 'Press <b>Open</b> next to <b>Cape gallery</b>. Your browser opens already signed in.',
    'account.expire': 'A sign-in lasts one day; open it from the game again when it expires. Offline accounts cannot sign in.',
    'account.banned': 'This account cannot upload capes.',
    'account.signout': 'Sign out of this browser',
    'account.mine': 'My cape', 'account.none': 'Nothing uploaded yet.', 'account.remove': 'Remove my cape',
    'account.worn': 'This one was picked from the cape gallery.',
    'account.public': 'Show in the cape gallery',
    'account.publichint': 'When this is off, others cannot see your cape in the gallery or wear it; other players still see you wearing it in game.',
    'account.pending': 'This cape is awaiting review. Nobody else can see it until it is approved.',
    'account.review': 'Capes you upload are reviewed first for now; others see them once they are approved.',
    'account.upload': 'Upload a cape',
    'account.spec': 'Draw it on the <a href="' + TEMPLATES + '">template</a>: ' + SIZES + '. File size: up to 1 MB for regular players, 20 MB for developers, sponsors and contributors. Anyone can upload a PNG; GIFs (up to 120 frames) are for developers, sponsors and contributors.',
    'account.pick': 'Choose a cape image', 'account.send': 'Upload',

    'admin.title': 'Admin - WynnChaYuan',
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
    'admin.staffhow': 'Enter their Minecraft ID, Minecraft UUID, or Discord user ID (17 to 20 digits: turn on Developer Mode in Discord, right-click them and choose “Copy User ID”). An entry added by Minecraft ID is switched to the UUID the first time they sign in. It takes effect at once.',
    'admin.who': 'Minecraft ID, UUID or Discord user ID',
    'admin.kind': 'Type', 'admin.id': 'ID', 'admin.nostaff': 'No moderators yet.',
    'admin.guilds': 'Contributing guilds',
    'admin.guildsnote': 'These appear in the team section of the home page and link to the guild’s page on Wynncraft. Add the name first (exactly as it is on Wynncraft), then upload an icon and a banner. Images are shrunk before upload; PNG, JPEG and WebP all work.',
    'admin.gname': 'Guild name', 'admin.gtag': 'Tag (optional)', 'admin.gpics': 'Icon and banner', 'admin.guild': 'Guild',
    'admin.noguilds': 'No guilds yet.',

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
    'feat.s3': 'What is sent', 'feat.s3b': 'For the two features above, the mod checks in with wynnchayuan.com once when you join Wynncraft: your UUID, your public profile, and your cape and badge settings. No game content is sent. <a href="' + REPO + '#readme">The README has the full details</a>',
    'feat.c1b': 'Item, quest and chat data all come from it; its own screens are translated too.',
    'feat.c2b': 'A dependency, like Wynntils.',
    'feat.c3b': 'Adds a settings button to the mod list.',
    'feat.c4b': 'Its search panel accepts translated names, and shows them next to the English ones in the results.',
    'feat.c5b': 'Its price box, reward screens and settings are translated too; hold Shift to see the original.',
    'feat.c6b': 'The few lines it reads to tell how a raid ended stay in English, so your streak is counted right.',

    js: {
      login: 'Sign in', loading: 'Loading…',
      count: '{n} capes (identical images count once)', empty: 'Nobody has uploaded a cape yet.', users: '{n}', wearing: '{n} wearing',
      card: '{t} cape, worn by {n}',
      drag: 'Drag to rotate.', no3d: 'The 3D view could not load; only the flat image is shown.',
      confirmDel: 'Delete this cape (worn by {n})?',
      confirmBan: 'Delete this cape and ban the {n} players wearing it from uploading?',
      expired: 'That did not work ({s}). A sign-in lasts one day - open the site from the game again.',
      readFail: 'Could not load the list. Please refresh.',
      roleDev: 'Developer', roleMember: 'Listed member', roleUser: 'Player with the mod',
      mineHint: '{t} · {d}{s} · drag to rotate', mineNo3d: 'The 3D view could not load.',
      discordAs: 'Discord: {n}', codeFail: 'Could not get a link code - please try again', copied: 'Copied',
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
        'login first': 'Your sign-in expired - open the site from the game again',
        'no such cape': 'This cape is no longer here. Please refresh',
        'nothing to restore': 'There is no cape to switch back to',
        'in use': 'You cannot remove the cape you are wearing from your history',
        'too many': 'Your favourites are full (200 at most)',
        'no cape': 'You do not have a cape yet',
      },
      favAdd: 'Add to my favourites', favRemove: 'Remove from my favourites',
      emptyFav: 'No favourites yet. Click the star on a cape to add it.', emptyHist: 'Nothing worn yet.', emptyFilter: 'No capes match.',
      tagPrivate: 'Not public', tagPending: 'In review', tagMine: 'Wearing', tagWorn: 'picked',
      confirmReview: 'Put the uploaders of this cape on the review list? Capes they upload from now on must be approved before others can see them.',
      reviewed: 'Added to the review list ({n})', reviewedNone: 'The uploaders are already on the review list', approved: 'Approved',
      confirmForget: 'Remove this one from your history?', forgot: 'Removed from your history',
      uploadedPending: 'Cape uploaded ({w}×{h}). Others will see it once it has been approved',
      wornPending: 'You are wearing it now, but others will only see it once it has been approved.',
      confirmUnreview: 'Remove {who} from the review list? A cape awaiting review will be let through.',
      onWynn: 'See {n} on Wynncraft',
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
      sending: 'Sending…', prDone: 'Pull request opened', prFail: 'That did not work ({s})',
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

/** 語言選單上每一種語言叫什麼（用它自己的話寫），也是選單的順序。 */
const LANG_NAMES = {
  zh: '繁體中文', 'zh-cn': '简体中文', en: 'English', ja: '日本語', ko: '한국어',
  ru: 'Русский', es: 'Español', tr: 'Türkçe',
};

/** 有哪幾種語言：有名字、也真的有字的那幾種。 */
export const LANGS = Object.keys(LANG_NAMES).filter((code) => Object.hasOwn(TEXT, code));

/** 這個語言的頁面寫在 html 上的標籤（zh-Hant、ja…）。 */
export function tagOf(lang) {
  return Object.hasOwn(TEXT, lang) && TEXT[lang].html ? TEXT[lang].html : 'en';
}

// ------------------------------------------------------------------ 每一頁都有的程式

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
  /* 玩家的頭像與全身圖。用 minotar：有些帳號在別家只畫得出預設的 Steve */
  function head(name, size) { return 'https://minotar.net/helm/' + encodeURIComponent(name) + '/' + (size || 32) + '.png'; }
  function figure(name, width) { return 'https://minotar.net/armor/body/' + encodeURIComponent(name) + '/' + width + '.png'; }
  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }
  /* 把 {n} 這種位置換成值。 */
  function fmt(text, values) {
    return text.replace(/\{(\w+)\}/g, function (all, key) { return key in values ? values[key] : all; });
  }
  function bytes(n) { return n >= 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB'; }
  var calm = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  /* 訊息換了字：淡進來（順便浮上來一點），眼睛才知道這裡變了 */
  function flash(node) {
    if (!node.animate) return;
    node.animate(calm ? [{ opacity: 0 }, { opacity: 1 }]
      : [{ opacity: 0, transform: 'translateY(4px)' }, { opacity: 1, transform: 'none' }],
      { duration: 200, easing: 'cubic-bezier(0.23, 1, 0.32, 1)' });
  }
  /* 分段選項底下那一塊：滑到選到的那一顆底下。第一次直接放好，之後才用滑的 */
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
      pill.getBoundingClientRect();        /* 先讓瀏覽器把位置算好，再把過場開回來，才不會從左上角滑進來 */
      pill.style.transition = '';
    }
  }
  function slideAll() { Array.prototype.forEach.call(document.querySelectorAll('.seg'), slide); }
  /* 按了哪一組就更新那一組（等那一顆自己的程式先把選到的換好）；視窗變寬變窄、字型載好也要重新量 */
  document.addEventListener('click', function (e) {
    var seg = e.target.closest && e.target.closest('.seg');
    if (seg) setTimeout(function () { slide(seg); }, 0);
  });
  window.addEventListener('resize', slideAll);
  window.addEventListener('load', slideAll);
  /* 按鈕暫時換一個字（「已複製」），過一下換回來。寬度先定住，字換了按鈕不會縮 */
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
  /* 頁首的下載選單：按一下開、再按一下關；點別的地方、按 Esc 也關 */
  (function () {
    var button = document.getElementById('get');
    var menu = document.getElementById('get-menu');
    function set(open) {
      menu.classList.toggle('open', open);
      button.setAttribute('aria-expanded', open ? 'true' : 'false');
    }
    button.onclick = function () { set(!menu.classList.contains('open')); };
    document.addEventListener('click', function (e) {
      if (!button.contains(e.target) && !menu.contains(e.target)) set(false);
    });
    document.addEventListener('keydown', function (e) {
      if (e.key !== 'Escape' || !menu.classList.contains('open')) return;
      set(false);
      button.focus();
    });
  })();
  /* 頁尾的 Discord 名稱：按一下複製（Discord 沒有「用名字開個人頁」的網址） */
  document.getElementById('dc-copy').onclick = function () {
    var button = this;
    if (!navigator.clipboard) return;
    navigator.clipboard.writeText('LyuChaCha').then(function () { blip(button, L.copied); });
  };
  /* 換語言：記在 cookie 裡（伺服器照它挑頁面），重新載入。 */
  document.getElementById('lang').onchange = function () {
    document.cookie = 'lang=' + this.value + '; path=/; max-age=31536000; samesite=lax';
    location.replace(location.pathname);     /* 網址上的 ?lang= 比 cookie 優先，要拿掉 */
  };
  /* 我是誰。沒登入、或通行證過期了，就是 null。 */
  var whoami = !token() ? Promise.resolve(null) : api('/me').then(function (r) {
    if (r.status === 401) { try { localStorage.removeItem(TOKEN_KEY); } catch (e) {} return null; }
    return r.ok ? r.json() : null;
  }).catch(function () { return null; });
  whoami.then(function (me) {
    var slot = document.getElementById('me');
    slot.replaceChildren();
    if (!me) {
      var link = el('a', '', L.login);
      link.href = '/account';
      slot.append(link);
      return;
    }
    if (me.linked === false) {              /* 用 Discord 登入了，還沒綁遊戲 ID */
      var pending = el('a', '', (me.discord && me.discord.name) || 'Discord');
      pending.href = '/account';
      slot.append(pending);
      if (me.staff) document.getElementById('nav-admin').hidden = false;
      return;
    }
    var img = el('img');
    img.alt = '';
    img.src = head(me.name, 44);
    var name = el('a', '', me.name);
    name.href = '/account';
    slot.append(img, name);
    if (me.staff) document.getElementById('nav-admin').hidden = false;
  });
`;

function layout(lang, title, active, body, script) {
  const nav = [['/', 'nav.home'], ['/features', 'nav.features'], ['/capes', 'nav.capes'], ['/account', 'nav.account']]
    .map(([href, key]) => '<a href="' + href + '"' + (href === active ? ' aria-current="page"' : '')
      + '>%%' + key + '%%</a>').join('');
  const switcher = LANGS.map((code) => '<option value="' + code + '"' + (code === lang ? ' selected' : '')
    + ' lang="' + tagOf(code) + '">' + LANG_NAMES[code] + '</option>').join('');
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
    + '<title>' + title + '</title>\n<style>' + STYLE + '</style>\n</head>\n<body>\n'
    + '<div class="top"><div class="in">'
    + '<a class="brand" href="/"><span>' + LEAF + '</span>WynnChaYuan</a>'
    + '<nav aria-label="%%nav.label%%">' + nav
    + '<a href="/admin" id="nav-admin" hidden' + (active === '/admin' ? ' aria-current="page"' : '')
    + '>%%nav.admin%%</a></nav>'
    + '<div class="tools">'
    + '<a class="kofi" href="' + KOFI + '" aria-label="%%foot.kofi%%">' + ICON.heart + '<span>%%top.sponsor%%</span></a>'
    + '<div class="pop"><button class="btn primary small" id="get" type="button" aria-expanded="false" aria-controls="get-menu">'
    + ICON.get + '%%top.get%%' + ICON.down + '</button>'
    + '<div class="menu" id="get-menu">'
    + '<a href="' + MODRINTH + '">Modrinth' + ICON.out + '</a>'
    + '<a href="' + CURSEFORGE + '">CurseForge' + ICON.out + '</a>'
    + '<a href="' + RELEASES + '">GitHub Releases' + ICON.out + '</a>'
    + '<p>%%top.getnote%%</p></div></div>'
    + '<select class="lang" id="lang" aria-label="%%lang.label%%">' + switcher + '</select>'
    + '<div class="me" id="me"></div></div>'
    + '</div></div>\n<main>' + body + '</main>\n'
    + '<footer><div class="in"><div class="foot">'
    + '<div class="foot-brand"><a class="brand" href="/"><span>' + LEAF + '</span>WynnChaYuan</a>'
    + '<p>%%foot.tagline%%</p></div>'
    + '<div class="foot-cols" role="navigation" aria-label="%%foot.label%%">'
    + column('foot.get', [to(MODRINTH, 'Modrinth'), to(CURSEFORGE, 'CurseForge'), to(RELEASES, 'GitHub Releases'),
      to(REPO + '/blob/main/CHANGELOG.md', '%%foot.changelog%%')])
    + column('foot.project', [to(REPO, 'GitHub'), to(REPO + '/blob/main/LICENSE', '%%foot.license%%'),
      to(REPO + '/blob/main/LICENSE-DATA', '%%foot.datalicense%%'), to(ISSUES, '%%foot.issues%%')])
    + column('foot.support', [to(KOFI, '%%foot.kofi%%'), to('/#sponsoring', '%%foot.sponsoring%%'),
      to('/#team', '%%foot.team%%'), to(REPO + '/blob/main/CONTRIBUTING.md', '%%foot.contribute%%')])
    + column('foot.contact', ['%%foot.sitedev%%',
      '<button class="copy" id="dc-copy" type="button" title="%%foot.copy%%">%%foot.discord%%</button>'])
    + '</div></div>'
    + '<div class="legal"><span>© 2026 WynnChaYuan - All rights reserved.</span>'
    + '<span>%%foot.unofficial%%</span>'
    + '<span>Designed by LyuChaCha</span></div>'
    + '</div></footer>\n'
    + '<script>var L = %%js%%;' + COMMON + '</script>\n' + (script || '') + '\n</body>\n</html>';
}

// ------------------------------------------------------------------ 首頁

const leaf = (colour) => '<span style="color:' + colour + ';display:flex">' + LEAF + '</span>';

const HOME = String.raw`
  <section class="hero">
    <div>
      <h1>%%home.h1%%</h1>
      <p class="lede">%%home.lede%%</p>
      <div class="cta">
        <a class="btn primary" href="https://modrinth.com/mod/wynnchayuan">%%home.modrinth%%</a>
        <a class="btn" href="https://www.curseforge.com/minecraft/mc-mods/wynnchayuan">CurseForge</a>
        <a class="btn" href="https://github.com/LyuChaCha/WynnChaYuan/releases/latest">GitHub</a>
      </div>
      <div class="chips" aria-label="%%home.langs%%">
        <span class="chip">繁體中文</span><span class="chip">简体中文</span><span class="chip">日本語</span>
        <span class="chip">한국어</span><span class="chip">Русский</span><span class="chip">Español</span><span class="chip">Türkçe</span>
      </div>
    </div>
    <img src="` + LOGO + String.raw`" alt="%%home.logo%%" width="380" height="380">
  </section>

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

/**
 * 首頁的團隊：名單跟 /api/team 要（倉庫裡那份 credits.json，加上管理頁登記的貢獻公會）。
 * 一個人一列，點下去是他在 Wynncraft 的頁面；公會那一段多了旗幟與圖示。
 */
const HOME_JS = '<script>' + String.raw`
  (function () {
    var ROLES = { developers: 1, sponsors: 1, contributors: 1 };
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
            var blank = el('span', 'blank');        /* 沒登記遊戲 ID 的人：一片葉子佔著頭像的位置 */
            blank.append(shape.cloneNode(true));
            row.append(blank);
          }
          row.append(el('b', '', member.name));
          /* 名單上寫的名字跟遊戲 ID 不一樣，才把遊戲 ID 另外寫出來 */
          if (member.mc && member.mc !== member.name) row.append(el('span', 'mc', member.mc));
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
        name.append(el('b', '', guild.name));
        if (guild.tag) name.append(el('span', '', '[' + guild.tag + ']'));
        link.append(name, arrow.cloneNode(true));
        item.append(link);
        list.append(item);
      });
      document.getElementById('team-list').removeAttribute('aria-busy');
    }).catch(function () {
      document.getElementById('team-list').removeAttribute('aria-busy');
      document.getElementById('team-fail').hidden = false;
    });
  })();
` + '</script>';

// ------------------------------------------------------------------ 功能

const rows = (prefix, n) => Array.from({ length: n }, (unused, i) => '<div><dt>%%feat.' + prefix + (i + 1)
  + '%%</dt><dd>%%feat.' + prefix + (i + 1) + 'b%%</dd></div>').join('\n          ');
const mod = (name, slug, state, key) => '<div><dt><a href="https://modrinth.com/mod/' + slug + '">' + name + '</a>'
  + '<span class="state' + (state === 'required' ? ' need' : '') + '">%%feat.' + state + '%%</span></dt><dd>%%feat.' + key + 'b%%</dd></div>';

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
      <div class="play-set" id="play-set"></div>
      <div class="play-view">
        <h3 id="play-title"></h3>
        <div class="pstage" id="play-stage" aria-hidden="true"></div>
      </div>
    </div>
    <p class="demo-foot"><span id="play-hint" role="status"></span><span class="hint">%%play.note%%</span></p>
    <template id="cursor"><svg viewBox="0 0 14 18" aria-hidden="true"><path d="M1 1v13l3.5-3 2.4 5.2 2-.9L6.5 10H11z" fill="#fff" stroke="#000" stroke-width="1"/></svg></template>
  </section>

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
        <dl class="rows">
          ` + rows('t', 10) + String.raw`
        </dl>
        <p class="note">%%features.tnote%%</p>
      </section>
      <section id="tools">
        <h2>%%features.u%%</h2>
        <dl class="rows">
          ` + rows('u', 8) + String.raw`
        </dl>
      </section>
      <section id="social">
        <h2>%%features.s%%</h2>
        <dl class="rows">
          ` + rows('s', 3) + String.raw`
        </dl>
      </section>
      <section id="compat">
        <h2>%%features.c%%</h2>
        <dl class="rows">
          ` + [mod('Wynntils 4.2+', 'wynntils', 'required', 'c1'), mod('Fabric API', 'fabric-api', 'required', 'c2'),
    mod('Mod Menu', 'modmenu', 'optional', 'c3'), mod('WynnMarketSearch', 'wynnmarketsearch', 'optional', 'c4'),
    mod('Wynnventory', 'wynnventory', 'optional', 'c5'), mod('WynnMod', 'wynnmod', 'compatible', 'c6')].join('\n          ') + String.raw`
        </dl>
        <p class="note">%%features.cnote%%</p>
        <div class="cta">
          <a class="btn primary" href="` + MODRINTH + String.raw`">%%home.modrinth%%</a>
          <a class="btn" href="` + CURSEFORGE + String.raw`">CurseForge</a>
          <a class="btn" href="` + RELEASES + String.raw`">GitHub</a>
          <a class="btn" href="/#install">%%features.install%%</a>
        </div>
      </section>
    </div>
  </div>
`;

/**
 * 功能頁的即時預覽：照遊戲裡設定畫面右邊那一塊做（client/ui/Preview.java）。
 * 左邊幾個真的選項，右邊一張小場景，選項一動場景就跟著變。
 * 選項的名稱與說明、場景裡的譯文都在 D 裡——build-demo.py 從模組的語言檔與語料產生的。
 */
const FEATURES_JS = '<script>var D = %%demo%%;' + String.raw`
  (function () {
    var ui = D.ui;
    var TAGS = { zh_tw: 'zh-Hant', zh_cn: 'zh-Hans', ja_jp: 'ja', ko_kr: 'ko', ru_ru: 'ru', es_es: 'es', tr_tr: 'tr' };
    var LINE = 'Well, what are we waiting for?';
    var CHOICES = ['Yes', 'Goodbye.'];
    /* 每一頁有哪幾列。options 是三段的選項（照遊戲裡的順序），toggle 是開關，range 是滑桿 */
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
    /* 跟模組的預設值一樣 */
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
    /* 一開始用這一頁的語言；英文版的頁面沒有對應的譯文，先給繁體中文 */
    Object.keys(TAGS).forEach(function (code) { if (TAGS[code] === document.documentElement.lang) lang = code; });
    picker.value = lang;
    document.getElementById('play-lang-name').textContent = ui['data.language'];
    document.getElementById('play-title').textContent = ui['preview.title'];
    tabs.forEach(function (button) { button.textContent = ui['tab.' + button.dataset.tab]; });

    function T(src) { return D.text[lang][src] || src; }
    /* 一塊字。translated 的那些標上語言：同一個漢字各語言的寫法不一樣，字型要跟著換 */
    function bit(tag, className, text, translated) {
      var node = el(tag, className, text);
      if (translated) node.lang = TAGS[lang];
      return node;
    }
    /* 把數字填回 {~}、{~2} 這種位置：譯文的語序不同時位置是有編號的 */
    function fill(template, values) {
      var next = 0;
      return template.replace(/\{~(\d*)\}/g, function (all, n) {
        var at = n ? Number(n) - 1 : next++;
        return at < values.length ? values[at] : '0';
      });
    }
    var turn = 0;
    /* 逐字打出來，跟遊戲裡的對話框一樣。中途又動了別的就停掉這一輪 */
    function type(node, text) {
      var mine = ++turn;
      var chars = Array.from(text);
      var shown = 0;
      if (calm) { node.textContent = text; return; }
      node.textContent = '';
      (function step() {
        if (mine !== turn) return;
        shown++;
        node.textContent = chars.slice(0, shown).join('');
        if (shown < chars.length) setTimeout(step, 26);
      })();
    }

    /* 物品：滑鼠旁邊的說明框，與我們另開的面板 */
    function drawItems() {
      var translatedName = T('Bony Bow');
      var name = state.names === 0 ? translatedName : state.names === 1 ? translatedName + ' (Bony Bow)' : 'Bony Bow';
      var rows = ['Durability', 'Health', 'Mana'];
      var panel = state.tooltip === 0;
      var replace = state.tooltip === 1;
      var duo = el('div', 'duo');
      var tip = el('div', 'tip');
      tip.append(document.getElementById('cursor').content.firstElementChild.cloneNode(true));
      tip.append(bit('div', '', replace ? name : 'Bony Bow', replace && state.names !== 2));
      rows.forEach(function (row) { tip.append(bit('div', '', replace ? T(row) : row, replace)); });
      duo.append(tip);
      if (panel) {
        var gap = el('div', 'gapmark');
        gap.style.width = Math.round(state.gap * 0.5) + 'px';      /* 間距照比例縮：200 像素要塞得進這張小圖 */
        gap.append(el('span', '', state.gap + ' px'));
        var side = el('div', 'gbox tipside');
        side.append(bit('div', '', name, state.names !== 2));
        rows.forEach(function (row) { side.append(bit('div', '', T(row), true)); });
        duo.append(gap, side);
      }
      stage.append(duo);
    }

    /* 對話：底下是遊戲自己的對話框，另開小框的時候譯文在它上面 */
    function drawDialogue(typed) {
      var stack = el('div', 'stack');
      if (state.choices === 0) {
        var choiceBox = el('div', 'gbox right');
        CHOICES.forEach(function (choice, i) { choiceBox.append(bit('div', '', '[' + (i + 1) + '] ' + T(choice), true)); });
        stack.append(choiceBox);
      }
      if (state.dialogue === 0) stack.append(bit('div', 'gbox', T(LINE), true));
      var box = el('div', 'dlgbox');
      box.append(el('b', '', 'Aledar'));
      var line = bit('p', '', '', state.dialogue === 1);
      box.append(line);
      CHOICES.forEach(function (choice, i) {
        box.append(bit('div', 'choice', '[' + (i + 1) + '] ' + (state.choices === 1 ? T(choice) : choice), state.choices === 1));
      });
      stack.append(box);
      stage.append(stack);
      if (state.dialogue === 1 && typed) type(line, T(LINE));
      else { turn++; line.textContent = state.dialogue === 1 ? T(LINE) : LINE; }
    }

    /* 世界與聊天：Boss 血條在頂端、任務追蹤在右上、NPC 在左邊、聊天在左下、手上那件的名字在快捷列上面 */
    function drawWorld() {
      var boss = bit('div', 'w-boss', state.bossbar ? T('Zombie') : 'Zombie', state.bossbar);
      boss.append(el('i'));
      stage.append(boss);

      var track = el('div', 'w-track');
      var inPlace = state.tracker === 0;
      var box = el('div', 'box');
      box.append(bit('div', '', inPlace ? T('World Event') : 'World Event', inPlace),
        bit('div', '', inPlace ? T('Click To Track') : 'Click To Track', inPlace));
      track.append(box);
      if (state.tracker === 1) track.append(bit('div', 'gbox', T('World Event'), true));
      stage.append(track);

      var npc = el('div', 'w-npc');
      var man = el('div', 'man');
      man.append(bit('div', 'plate', state.nametag === 1 ? T('Weapon Merchant') : 'Weapon Merchant', state.nametag === 1));
      npc.append(man);
      if (state.nametag === 0) npc.append(bit('div', 'gbox', T('Weapon Merchant'), true));
      stage.append(npc);

      stage.append(bit('div', 'w-title', state.titles ? T('Level Up!') : 'Level Up!', state.titles));

      var on = state.scoreboard;
      var board = el('div', 'w-board');
      board.append(bit('div', '', on ? T('Daily Objective:') : 'Daily Objective:', on));
      [['- Loot Chests T{~}+: {~}/{~}', ['2', '3', '5']], ['- Mobs slain: {~}/{~}', ['42', '100']]].forEach(function (row) {
        board.append(bit('div', '', fill(on ? T(row[0]) : row[0], row[1]), on));
      });
      stage.append(board);

      var chat = el('div', 'w-chat');
      var said = 'The trade has been completed!';
      if (state.chat !== 0) chat.append(el('div', '', said));
      if (state.chat !== 2) chat.append(bit('div', state.chat === 1 ? 'ours' : '', T(said), true));
      stage.append(chat);

      var held = el('div', 'w-held');
      held.append(bit('div', '', state.held ? T('Bony Bow') : 'Bony Bow', state.held));
      var bar = el('i');
      for (var i = 0; i < 4; i++) bar.append(el('u'));
      held.append(bar);
      stage.append(held);
    }

    function draw(typed) {
      stage.replaceChildren();
      if (tab === 'items') drawItems();
      else if (tab === 'dialogue') drawDialogue(typed);
      else drawWorld();
    }
    function explain(row) {
      var text = row ? ui[row.label + '.hint'] : L.playStart;
      if (hintNode.textContent === text) return;
      hintNode.textContent = text;
      flash(hintNode);
    }
    /* quiet：拖滑桿的時候一路重畫，不要每畫一次就淡入一次 */
    function changed(row, typed, quiet) {
      updaters.forEach(function (update) { update(); });
      explain(row);
      stage.classList.toggle('still', Boolean(quiet));
      draw(typed);
    }
    /* 這一頁的幾列。每一列自己記得怎麼照 state 更新外觀，按了不必整塊重畫（焦點才不會跑掉） */
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
          input.min = 0;
          input.max = 200;
          input.value = state[row.key];
          input.setAttribute('aria-label', ui[row.label]);
          var output = el('output', '', state[row.key] + ' px');
          input.oninput = function () { state[row.key] = Number(input.value); output.textContent = input.value + ' px'; changed(row, false, true); };
          /* 間距只有「另開面板」的時候有東西可以調：別的模式這一列淡掉 */
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
    picker.onchange = function () { lang = picker.value; draw(true); };
    build();
    explain(null);
    draw(false);

    /* 左邊的目錄：捲到哪一段，哪一項就亮起來 */
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
  /* 亮相：人偶從側面轉回背對著的位置，披風跟著甩進視線。每穿上一件新的做一次。 */
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
  /* 把一件披風穿到人偶身上。GIF 一格一格換：瀏覽器有 ImageDecoder 就動，沒有就停在第一格。 */
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
  <div class="bar">
    <span class="count" id="count"></span>
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
  <div class="seg filters" id="filters" role="group" aria-label="%%capes.filter%%">
    <button type="button" data-f="all" aria-pressed="true">%%capes.f.all%%</button>
    <button type="button" data-f="png" aria-pressed="false">PNG</button>
    <button type="button" data-f="gif" aria-pressed="false">GIF</button>
    <button type="button" data-f="fav" aria-pressed="false" hidden>%%capes.f.fav%%</button>
    <button type="button" data-f="hist" aria-pressed="false" hidden>%%capes.f.hist%%</button>
    <button type="button" data-f="hidden" aria-pressed="false" hidden>%%capes.f.hidden%%</button>
    <button type="button" data-f="pending" aria-pressed="false" hidden>%%capes.f.pending%%</button>
  </div>
  <div class="grid" id="grid"></div>
  <p class="note" style="margin:28px 0 0;max-width:72ch">%%rules%%</p>
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
        <ul class="owners" id="owners" hidden></ul>
        <p class="hint" id="hint"></p>
        <div class="wearbox" id="wearbox" hidden>
          <p class="hint" id="wear-note" hidden></p>
          <button class="btn primary" id="wear">%%view.wear%%</button>
          <button class="btn" id="unwear" hidden></button>
          <button class="btn" id="fav"></button>
          <button class="btn" id="forget" hidden>%%view.forget%%</button>
        </div>
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
  var visitor = null;                    /* 登入的人（/api/me 回的那一份）；沒登入是 null */
  var all = [];                          /* 伺服器回的那一份：一張圖一筆 */
  var favs = {};                         /* 我的最愛：{雜湊: true} */
  var past = [];                         /* 我用過的（新的在前） */
  var sort = 'new';
  var filter = 'all';
  document.getElementById('count').textContent = L.loading;
  function member() { return Boolean(visitor && visitor.linked !== false); }
  /* 圖的網址。平常拿「穿著它的某個人」那一件；我用過、但現在沒人穿的，照雜湊從自己的紀錄拿。
     管理的人多帶雜湊：等審核的那幾件要它才拿得到 */
  function url(cape) {
    if (!cape.ids.length) return '/api/cape/' + visitor.id + '?k=' + cape.h;
    return '/api/cape/' + cape.ids[0] + '?v=' + cape.v + (developer || cape.own ? '&k=' + cape.h : '');
  }
  /* 還在載入：先放幾張空卡佔位置，資料到了整批換掉 */
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

  /* ---- 這一頁現在該列哪幾件：照篩選挑、照排序排 ---- */
  function listNow() {
    if (filter === 'hist') {
      /* 我用過的：照用的時間排。一覽上還有的拿一覽那一筆（才有人數與星星），沒有的自己拼一筆 */
      return past.map(function (one) {
        var found = all.filter(function (cape) { return cape.h === one.h; })[0];
        return found || { h: one.h, t: one.t, s: one.s, x: one.x, y: one.y, v: one.at, c: one.at, ids: [], stars: 0, gone: true };
      });
    }
    var out = all.filter(function (cape) {
      if (filter === 'png' || filter === 'gif') return cape.t === filter;
      if (filter === 'fav') return favs[cape.h];
      if (filter === 'hidden') return cape.hidden && !cape.pending;
      if (filter === 'pending') return cape.pending;
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
      /* 星星亮起來的時候彈一下 */
      if (button && button.animate && !calm && answer.on) {
        button.animate([{ transform: 'scale(1)' }, { transform: 'scale(1.28)' }, { transform: 'scale(1)' }],
          { duration: 260, easing: 'cubic-bezier(0.23, 1, 0.32, 1)' });
      }
      render(true);
      if (current === cape) favState();
    }).catch(function () {});
  }

  /* still：只是星星或人數變了，卡片不要重新一張一張進場 */
  function render(still) {
    var items = listNow();
    var count = document.getElementById('count');
    var parts = L.count.split('{n}');
    count.replaceChildren(parts[0], el('b', '', String(items.length)), parts[1] || '');
    grid.replaceChildren();
    if (!items.length) {
      grid.append(el('div', 'empty', filter === 'fav' ? L.emptyFav : filter === 'hist' ? L.emptyHist : all.length ? L.emptyFilter : L.empty));
    }
    items.forEach(function (cape, index) {
      var card = el('div', 'card');
      if (still) card.style.animation = 'none';
      else card.style.setProperty('--i', Math.min(index, 10));
      /* 每張卡有自己的名字：換排序的時候瀏覽器才知道哪一張移到哪裡。太多張就不取了（一次動太多反而亂） */
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
      var meta = el('div', 'meta');
      meta.append(el('span', 'tag ' + cape.t, cape.t.toUpperCase()));
      if (member() && visitor.cape && visitor.cape.h === cape.h) meta.append(el('span', 'tag mine', L.tagMine));
      if (cape.pending) meta.append(el('span', 'tag wait', L.tagPending));
      else if (cape.hidden) meta.append(el('span', 'tag off', L.tagPrivate));
      if (cape.stars) {
        var stars = el('span', 'stars');
        stars.append(starShape.cloneNode(true), String(cape.stars));
        meta.append(stars);
      }
      if (cape.ids.length) meta.append(el('span', 'who', fmt(L.wearing, { n: cape.ids.length })));
      open.append(stage, meta);
      open.onclick = function () { show(cape); };
      card.append(open);
      /* 星星：登入並綁定的人才有；一覽上看不到的那些（我用過但已經沒人穿、不公開、待審核）不能加 */
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

  /* ---- 點開一件 ---- */
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
    /* 我用過、現在沒穿的那幾件可以從紀錄拿掉 */
    document.getElementById('forget').hidden = !(filter === 'hist' && !on);
  }
  function show(cape) {
    current = cape;
    document.getElementById('flat').src = url(cape);
    document.getElementById('d-type').textContent = cape.t.toUpperCase();
    document.getElementById('d-size').textContent = bytes(cape.s);
    /* 尺寸：有記的用記的；早期上傳的沒記，等那張圖載進來自己量 */
    var dim = document.getElementById('d-dim');
    dim.textContent = cape.x ? cape.x + '×' + cape.y : '';
    document.getElementById('flat').onload = function () {
      if (!cape.x) dim.textContent = this.naturalWidth + '×' + this.naturalHeight;
    };
    document.getElementById('d-users').textContent = fmt(L.users, { n: cape.ids.length });
    document.getElementById('d-date').textContent = new Date(cape.c || cape.v).toLocaleDateString();
    document.getElementById('del').hidden = !developer || cape.gone;
    document.getElementById('ban').hidden = !developer || cape.gone;
    document.getElementById('review').hidden = !developer || cape.gone;
    document.getElementById('approve').hidden = !(developer && cape.pending);
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
      /* 管理的人看得到每個人的狀態：不公開、待審核、是套用來的（圖不是他傳的） */
      var marks = [owner.pending ? L.tagPending : '', owner.private ? L.tagPrivate : '', owner.worn ? L.tagWorn : '']
        .filter(function (mark) { return mark; });
      li.append(el('span', '', (owner.name || owner.uuid) + (marks.length ? ' (' + marks.join(', ') + ')' : '')));
      owners.append(li);
    });
    dialog.showModal();
    var hint = document.getElementById('hint');
    if (!viewer) viewer = makeViewer(document.getElementById('model'));
    if (!viewer) { hint.textContent = L.no3d; return; }
    hint.textContent = L.drag;
    wear(viewer, url(cape), cape.t === 'gif');
  }
  function shut() { stopCape(); current = null; if (dialog.open) dialog.close(); }
  document.getElementById('close').onclick = shut;
  dialog.addEventListener('close', stopCape);
  dialog.addEventListener('click', function (e) { if (e.target === dialog) shut(); });

  /* 穿上這一件／復原。能不能穿由伺服器決定（GIF 只有成員能穿、檔案上限、封禁），這裡只管畫面 */
  function wearCall(path, body, done) {
    var buttons = [document.getElementById('wear'), document.getElementById('unwear')];
    buttons.forEach(function (button) { button.disabled = true; });
    send(path, body).then(function (answer) {
      if (!answer.good) { tell(L.why[answer.why] || fmt(L.failed, { s: answer.status }), false); return null; }
      /* 換了一件：重新問一次「我是誰」，再把一覽更新（人數、我用過的） */
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
  document.getElementById('forget').onclick = function () {
    var cape = current;
    if (!cape || !confirm(L.confirmForget)) return;
    send('/cape/forget', { hash: cape.h }).then(function (answer) {
      if (!answer.good) { tell(L.why[answer.why] || fmt(L.failed, { s: answer.status }), false); return; }
      shut();
      load(true);
    }).catch(function () { tell(L.offline, false); });
  };

  /* ---- 管理 ---- */
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

  /* ---- 排序與篩選 ---- */
  /* 換排序、換篩選：留下來的卡滑到新的位置，新來的淡進來。瀏覽器不支援、或不想看動來動去的人就直接換 */
  function reorder() {
    if (!document.startViewTransition || calm || document.visibilityState === 'hidden') { render(false); return; }
    document.startViewTransition(function () { render(true); });
  }
  var filters = Array.prototype.slice.call(document.querySelectorAll('#filters button'));
  filters.forEach(function (button) {
    button.onclick = function () {
      filter = button.dataset.f;
      filters.forEach(function (other) { other.setAttribute('aria-pressed', other === button ? 'true' : 'false'); });
      /* 「我用過的」照用的時間排，排序在那裡沒有用 */
      document.getElementById('sort').disabled = filter === 'hist';
      reorder();
    };
  });
  document.getElementById('sort').onchange = function () { sort = this.value; reorder(); };

  function load(still) {
    var list = api(developer ? '/admin/capes' : '/capes').then(function (r) { return r.json(); });
    var mine = member() ? api('/capes/mine').then(function (r) { return r.ok ? r.json() : null; }).catch(function () { return null; })
      : Promise.resolve(null);
    return Promise.all([list, mine]).then(function (got) {
      all = got[0].capes;
      if (got[1]) {
        favs = {};
        got[1].favs.forEach(function (hash) { favs[hash] = true; });
        past = got[1].history;
      }
      /* 自己那一件：在等審核、或不公開的時候一覽上沒有它，圖要帶著雜湊才拿得到 */
      all.forEach(function (cape) { if (member() && visitor.cape && visitor.cape.h === cape.h) cape.own = true; });
      /* 點開著的那一件換成新的那一筆（人數、星星會變） */
      if (current) {
        var again = all.filter(function (cape) { return cape.h === current.h; })[0];
        if (again) { current = again; favState(); }
      }
      render(still);
    });
  }
  whoami.then(function (me) {
    developer = Boolean(me && me.staff);       /* 開發者或管理員：看得到全部（含不公開、待審核）與誰傳的，能刪除與封禁 */
    visitor = me;
    filters.forEach(function (button) {
      var f = button.dataset.f;
      if (f === 'fav' || f === 'hist') button.hidden = !member();
      if (f === 'hidden' || f === 'pending') button.hidden = !developer;
    });
    slideAll();
    return load(false);
  }).catch(function () { document.getElementById('count').textContent = L.readFail; });
` + '</script>';

// ------------------------------------------------------------------ 帳號

const ACCOUNT = String.raw`
  <h1>%%account.h1%%</h1>
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
        <div class="set-line">
          <span><b id="public-label">%%account.public%%</b><span class="hint">%%account.publichint%%</span></span>
          <button class="switch" id="public" type="button" role="switch" aria-checked="true" aria-labelledby="public-label"></button>
        </div>
        <div style="margin-top:12px;display:flex;flex-wrap:wrap;gap:8px">
          <button class="btn" id="restore" hidden></button>
          <button class="btn danger" id="remove" hidden>%%account.remove%%</button>
        </div>
      </div>
    </div>
    <h2>%%account.upload%%</h2>
    <div class="panel" style="max-width:720px">
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
`;

const ACCOUNT_JS = SKINVIEW + '<script>' + VIEWER + String.raw`
  var msg = document.getElementById('msg');
  function say(text, ok) { msg.textContent = text; msg.className = 'msg ' + (ok ? 'ok' : 'bad'); flash(msg); }
  var viewer = null;
  var trying = null;                       /* 正在試穿的那張圖的網址（還沒上傳） */
  /* 選了一張尺寸對的圖：先穿到人偶身上看看，不必傳了才知道長怎樣 */
  function tryOn(file, w, h) {
    var canvas = document.getElementById('mine');
    var hint = document.getElementById('minehint');
    canvas.hidden = false;
    if (!viewer) viewer = makeViewer(canvas);
    if (!viewer) { canvas.hidden = true; return; }
    if (trying) URL.revokeObjectURL(trying);
    trying = URL.createObjectURL(file);
    document.getElementById('nocape').hidden = true;
    hint.hidden = false;
    hint.textContent = fmt(L.previewHint, { w: w, h: h });
    wear(viewer, trying, file.type === 'image/gif');
  }

  var shared = true;                     /* 要不要出現在披風一覽。還沒有披風的時候先記在這裡，上傳時帶去 */
  function show(me) {
    document.getElementById('nocape').hidden = Boolean(me.cape);
    if (me.cape) shared = me.cape.public !== false;
    document.getElementById('public').setAttribute('aria-checked', shared ? 'true' : 'false');
    document.getElementById('pending').hidden = !(me.cape && me.cape.pending);
    document.getElementById('review').hidden = !me.review;
    document.getElementById('remove').hidden = !me.cape;
    /* 這一件是從一覽套用的：多一顆「復原」，換回自己原本那一件（原本沒有的話就是脫下） */
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
    if (!viewer) viewer = makeViewer(canvas);
    if (!viewer) { hint.textContent = L.mineNo3d; canvas.hidden = true; return; }
    hint.textContent = fmt(L.mineHint, { t: me.cape.t.toUpperCase(), s: bytes(me.cape.s),
      d: me.cape.x ? me.cape.x + '×' + me.cape.y + ' · ' : '' });
    /* 帶著雜湊：等審核的那一件只有知道雜湊的人（本人、管理的人）拿得到 */
    wear(viewer, '/api/cape/' + me.id + '?v=' + me.cape.v + '&k=' + me.cape.h, me.cape.t === 'gif');
  }
  function refresh() {
    return api('/me').then(function (r) { return r.ok ? r.json() : null; }).then(function (me) { if (me) show(me); });
  }

  /* 網站有沒有開 Discord 登入：有才把按鈕露出來 */
  var discordOn = fetch('/api/status').then(function (r) { return r.json(); })
    .then(function (status) { return Boolean(status.discord); }).catch(function () { return false; });
  var linkMsg = document.getElementById('linkmsg');
  var limit = 20 * 1024 * 1024;          /* 檔案上限：登入之後換成這個人自己的（一般玩家 1 MB） */
  var SIZES_OK = [[22, 17], [44, 34], [88, 68], [176, 136], [352, 272], [704, 544], [1056, 816]];
  var watching = null;

  /* 用 Discord 登入了、還沒綁：要一組綁定碼，然後等遊戲那邊把它送回來 */
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
      document.getElementById('oauth-error').hidden = !/e=discord$/.test(location.hash);
      discordOn.then(function (on) {
        document.getElementById('discord').hidden = !on;
        document.getElementById('discord-off').hidden = on;
      });
      return;
    }
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
      /* 從遊戲開的：提醒可以另外用 Discord 註冊，之後不開遊戲也登得進來 */
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
  /* 一鍵綁定：這一頁已經是驗過的遊戲帳號，去 Discord 登入回來就綁好了 */
  document.getElementById('discord2').onclick = function () {
    api('/link/intent', { method: 'POST', body: '{}', headers: { 'content-type': 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (answer) {
        if (answer && String(answer.url).indexOf('/auth/discord?intent=') === 0) location.href = answer.url;
        else say(L.bindFail, false);
      }).catch(function () { say(L.offline, false); });
  };
  /* 選了圖就先說它多大：尺寸不對的話不必等傳完才知道 */
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
  /* 把圖檔拖進上傳那一塊：框亮起來，放開就等於選了那個檔 */
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
  document.getElementById('public').onclick = function () {
    var button = this;
    var want = button.getAttribute('aria-checked') !== 'true';
    shared = want;
    button.setAttribute('aria-checked', want ? 'true' : 'false');
    /* 有披風就馬上改那一件；還沒有的話先記著，等上傳的時候一起送 */
    api('/me').then(function (r) { return r.ok ? r.json() : null; }).then(function (me) {
      if (!me || !me.cape) return;
      return api('/cape/public', { method: 'POST', body: JSON.stringify({ on: want }), headers: { 'content-type': 'application/json' } })
        .then(function (r) { if (!r.ok) throw new Error('public'); });
    }).catch(function () {
      shared = !want;
      button.setAttribute('aria-checked', !want ? 'true' : 'false');
      say(L.tryLater, false);
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
    button.disabled = true;                 /* 傳的時候按鈕看得出來在忙，也不會被連按 */
    button.setAttribute('aria-busy', 'true');
    function idle() { button.disabled = false; button.removeAttribute('aria-busy'); }
    say(L.uploading, true);
    api('/cape?public=' + (shared ? '1' : '0'), { method: 'PUT', body: file, headers: { 'content-type': file.type || 'application/octet-stream' } })
      .then(function (r) {
        idle();
        return r.json().catch(function () { return {}; }).then(function (body) {
          if (r.ok) { say(fmt(body.pending ? L.uploadedPending : L.uploaded, { w: body.width, h: body.height }), true); refresh(); }
          else say(L.why[body.why] || fmt(L.failed, { s: r.status }), false);
        });
      }).catch(function () { say(L.offline, false); });
  };
` + '</script>';

// ------------------------------------------------------------------ 管理

const ADMIN = String.raw`
  <h1>%%admin.h1%%</h1>
  <p class="lede" id="gate">%%admin.gate%%</p>
  <div id="panel" hidden>
    <p class="lede">%%admin.lede%%</p>

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
      <p class="msg" id="c-msg" role="status"></p>
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
          <div><label for="s-who">%%admin.who%%</label><input type="text" id="s-who" maxlength="36" autocomplete="off" spellcheck="false"></div>
          <div><label for="s-note">%%admin.note%%</label><input type="text" id="s-note" maxlength="60" autocomplete="off"></div>
          <div><button class="btn primary" id="s-send">%%admin.grant%%</button></div>
        </div>
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
  /* 審核名單：名單上的人之後傳的披風要先通過 */
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
  /* 貢獻公會：名字與標籤先加，圖示與旗幟各傳一張 */
  var gmsg = document.getElementById('g-msg');
  function gsay(text, good) { gmsg.className = 'msg ' + (good ? 'ok' : 'bad'); gmsg.textContent = text; flash(gmsg); }
  var picking = null;                    /* 正在替哪個公會挑哪一張圖 */
  /* 圖先縮到 max 以內再傳：標誌動不動就是一千多像素，網頁上只畫幾十像素 */
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
        var link = el('a', '', guild.name + (guild.tag ? ' [' + guild.tag + ']' : ''));
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

  /* 管理員名單：只有開發者看得到、動得了 */
  var KIND = { u: 'Minecraft UUID', n: 'Minecraft ID', d: 'Discord' };
  function loadStaff() {
    return api('/admin/staff').then(function (r) { return r.json(); }).then(function (data) {
      var body = document.getElementById('staff');
      body.replaceChildren();
      document.getElementById('nostaff').hidden = data.staff.length > 0;
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
        } else {
          who.append(one.kind === 'u' ? L.notYet : '—');
        }
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
  document.getElementById('s-send').onclick = function () {
    var msg = document.getElementById('s-msg');
    var entry = { who: document.getElementById('s-who').value.trim(), note: document.getElementById('s-note').value.trim() };
    msg.className = 'msg ok';
    msg.textContent = L.sending;
    api('/admin/staff', { method: 'POST', body: JSON.stringify(entry), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          if (r.ok) {
            msg.className = 'msg ok';
            msg.textContent = L.perkDone;
            document.getElementById('s-who').value = '';
            document.getElementById('s-note').value = '';
            loadStaff();
          } else {
            msg.className = 'msg bad';
            msg.textContent = L.staff[body.why] || fmt(L.prFail, { s: body.why || r.status });
          }
        });
      }).catch(function () { msg.className = 'msg bad'; msg.textContent = L.offline; });
  };
  whoami.then(function (me) {
    if (!me || !me.staff) return;
    document.getElementById('gate').hidden = true;
    document.getElementById('panel').hidden = false;
    loadBans();
    loadReviews();
    loadPerks();
    loadGuilds();
    if (me.developer) {
      document.getElementById('staff-block').hidden = false;
      loadStaff();
    }
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
    msg.textContent = L.sending;
    api('/admin/credits', { method: 'POST', body: JSON.stringify(entry), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          msg.replaceChildren();
          if (r.ok) {
            msg.className = 'msg ok';
            msg.append(L.prDone);
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

/** 每一頁：標題的鍵、內容、程式。 */
const PAGES = {
  '/': ['home.title', HOME, HOME_JS],
  '/features': ['features.title', FEATURES, FEATURES_JS],
  '/capes': ['capes.title', CAPES, CAPES_JS],
  '/account': ['account.title', ACCOUNT, ACCOUNT_JS],
  '/admin': ['admin.title', ADMIN, ADMIN_JS],
};

/** 網站有哪幾頁。 */
export const PATHS = Object.keys(PAGES);

const built = new Map();

/**
 * 某一頁的 HTML。
 *
 * @param path 路徑
 * @param lang 語言（LANGS 之一；別的當成英文）
 * @returns 沒有這一頁回 null
 */
export function render(path, lang, bare) {
  if (!Object.hasOwn(PAGES, path)) return null;
  const code = Object.hasOwn(TEXT, lang) ? lang : 'en';
  const key = code + ' ' + path + (bare ? ' bare' : '');
  if (!built.has(key)) {
    const text = { ...filled(code) };
    // 連結預覽的介紹：首頁那一段開場白。bare 是「什麼語言都沒說」的訪客——多半是
    // Discord、LINE 這類抓預覽的機器人——給它中文與英文各一句，兩邊的人貼出去都看得懂。
    text['meta.desc'] = attribute(bare ? plain(TEXT.zh['home.lede']) + ' ' + plain(TEXT.en['home.lede'])
      : plain(text['home.lede']));
    const [title, body, script] = PAGES[path];
    const html = layout(code, '%%' + title + '%%', path, body, script)
      .replace(/%%([\w.]+)%%/g, (all, name) => {
        if (name === 'js') return JSON.stringify(text.js).replace(/</g, '\\u003c');
        // 功能頁的即時預覽：選項的名稱照這一頁的語言，場景裡的譯文七種都帶著（頁面上可以換）
        if (name === 'demo') return JSON.stringify({ ui: DEMO.ui[code], text: DEMO.text }).replace(/</g, '\\u003c');
        return Object.hasOwn(text, name) ? text[name] : all;
      });
    built.set(key, html);
  }
  return built.get(key);
}

/** 拿掉標籤，只留字。 */
function plain(html) {
  return String(html).replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/\s+/g, ' ').trim();
}

/** 可以放進屬性值裡的字。 */
function attribute(text) {
  return text.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
}

/** 這個語言的字，缺的拿英文補：新加的字還沒翻到的語言不會冒出一個 %%鍵%%。 */
function filled(code) {
  const own = TEXT[code];
  if (code === 'en') return own;
  const base = TEXT.en;
  const js = { ...base.js, ...(own.js || {}) };
  js.why = { ...base.js.why, ...((own.js && own.js.why) || {}) };
  js.credit = { ...base.js.credit, ...((own.js && own.js.credit) || {}) };
  js.staff = { ...base.js.staff, ...((own.js && own.js.staff) || {}) };
  js.guild = { ...base.js.guild, ...((own.js && own.js.guild) || {}) };
  return { ...base, ...own, js };
}

/** 瀏覽器說的語言標籤（已經轉小寫）對到我們有的哪一種；沒有回 null。 */
function fromTag(tag) {
  if (tag === 'zh' || tag.startsWith('zh-')) {
    // 簡體：zh-CN、zh-SG、zh-MY、zh-Hans…；其餘（台灣、香港、澳門、只寫 zh 的）給繁體
    const simplified = /^zh-(?:cn|sg|my|hans)\b/.test(tag) && LANGS.includes('zh-cn');
    return simplified ? 'zh-cn' : 'zh';
  }
  const primary = tag.split('-')[0];
  return primary !== 'zh' && LANGS.includes(primary) ? primary : null;
}

/**
 * 訪客要看哪一種語言：選過的（cookie）優先，沒選過看瀏覽器的語言，再沒有就英文。
 *
 * @param cookie Cookie 標頭
 * @param accept Accept-Language 標頭
 */
export function pickLang(cookie, accept) {
  const chosen = /(?:^|;\s*)lang=([a-z-]+)/.exec(cookie || '');
  if (chosen && LANGS.includes(chosen[1])) return chosen[1];
  for (const part of String(accept || '').toLowerCase().split(',')) {
    const hit = fromTag(part.split(';')[0].trim());
    if (hit) return hit;
  }
  return 'en';
}
