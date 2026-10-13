/**
 * wynnchayuan.com 的網頁：首頁、披風一覽、帳號、管理。中文與英文各一份。
 *
 * 四頁共用一份樣式、頁首與頁尾，各自一段程式。全部是靜態的字串，資料都跟 /api 要。
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
  .brand span { color: var(--leaf); display: flex; }
  .brand svg { width: 26px; height: 26px; }
  nav { display: flex; gap: 4px; flex-wrap: wrap; }
  nav a { padding: 6px 12px; border-radius: 8px; color: var(--ink-2); font-size: 14px; }
  nav a:hover { color: var(--ink); background: var(--panel-2); text-decoration: none; }
  nav a[aria-current="page"] { color: var(--ink); background: var(--panel-2); }
  .tools { margin-left: auto; display: flex; align-items: center; gap: 14px; }
  select.lang { width: auto; font: inherit; font-size: 13px; padding: 5px 8px; border-radius: 8px; border: 1px solid var(--line); background: var(--panel); color: var(--ink-2); cursor: pointer; }
  select.lang:hover { color: var(--ink); border-color: var(--leaf-deep); }
  /* 同一個漢字在各語言的寫法不一樣：照頁面的語言挑字型，不要一律拿繁體中文的字型去畫 */
  :lang(zh-Hans) body { font-family: "Segoe UI", "Microsoft YaHei", "PingFang SC", "Noto Sans SC", system-ui, sans-serif; }
  :lang(ja) body { font-family: "Segoe UI", "Yu Gothic UI", "Hiragino Sans", "Noto Sans JP", "Meiryo", system-ui, sans-serif; }
  :lang(ko) body { font-family: "Segoe UI", "Malgun Gothic", "Apple SD Gothic Neo", "Noto Sans KR", system-ui, sans-serif; }
  .me { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--ink-2); }
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

  .hero { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 36px; align-items: center; padding: 18px 0 8px; }
  .hero h1 { font-size: 40px; line-height: 1.25; }
  .hero .lede { font-size: 16px; }
  .hero img { width: 100%; max-width: 380px; height: auto; justify-self: center; filter: drop-shadow(0 18px 40px #000a); }
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
  .role span span { color: var(--ink-2); font-size: 14px; }
  .plain { margin: 10px 0 0; padding-left: 1.2em; color: var(--ink-2); }
  .plain li { margin: 4px 0; } .plain b { color: var(--ink); font-weight: 600; }
  .split { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 14px; }

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
  .linkcode { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin: 10px 0; }
  .linkcode code { font-size: 18px; color: var(--ink); background: #0f1512; border: 1px solid var(--line); border-radius: 8px; padding: 8px 14px; user-select: all; }
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

const LOGO = 'https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/logo/2026/wynnchayuan-logo-transparent-on-dark-1024.png';
const TEMPLATES = 'https://github.com/LyuChaCha/WynnChaYuan/tree/main/capes/templates';
const ISSUES = 'https://github.com/LyuChaCha/WynnChaYuan/issues';
const SIZES = '22×17, 44×34, 88×68, 176×136, 352×272, 704×544, 1056×816';

// ------------------------------------------------------------------ 字

export const TEXT = {
  zh: {
    html: 'zh-Hant',
    'nav.home': '首頁', 'nav.capes': '披風', 'nav.account': '帳號', 'nav.admin': '管理', 'nav.label': '主選單',
    'lang.label': '語言',

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
    'home.badges': '徽章與披風',
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
    'capes.lede': 'WynnChaYuan 玩家上傳的披風。點一件可以轉著看。',
    'capes.mine': '上傳我的披風',
    'rules': '<b>不可以上傳：</b>政治、不雅或色情、違反法規、種族歧視或仇恨的內容。違規的披風會被移除，上傳的人可能不能再傳。',
    'view.title': '披風', 'view.flat': '披風的範本圖：左邊正面、右邊背面',
    'view.type': '格式', 'view.dim': '尺寸', 'view.size': '檔案大小', 'view.users': '使用', 'view.date': '上傳',
    'view.del': '刪除這件披風', 'view.ban': '刪除並封禁上傳的人', 'view.close': '關閉',

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
    'account.upload': '上傳披風',
    'account.spec': '要照<a href="' + TEMPLATES + '">範本</a>畫：' + SIZES + '。檔案大小：一般玩家 1 MB 以內，開發者、贊助者、貢獻者 20 MB 以內。PNG 大家都能傳；GIF（最多 120 格）只有開發者、贊助者、貢獻者能傳。',
    'account.pick': '選一張披風的圖', 'account.send': '上傳',

    'admin.title': '管理 - WynnChaYuan',
    'admin.h1': '管理',
    'admin.gate': '這一頁只有名單上的開發者能用。請從遊戲裡登入。',
    'admin.lede': '披風的刪除與封禁在<a href="/capes">披風一覽</a>：點開一件，就看得到是誰傳的。',
    'admin.bans': '封禁名單',
    'admin.player': '玩家', 'admin.when': '封禁的時間', 'admin.by': '誰封的',
    'admin.nobans': '目前沒有人被封禁。',
    'admin.add': '新增成員',
    'admin.addnote': '送出之後會在 GitHub 開一個 PR 改 <code>credits.json</code>，合併之後玩家下次同步名單就生效（名牌的葉子、傳 GIF 披風的資格）。',
    'admin.section': '身分', 'admin.name': '顯示的名字', 'admin.mc': 'Minecraft ID', 'admin.color': '葉子顏色（可以不填）',
    'admin.contributors': '貢獻者', 'admin.sponsors': '贊助者', 'admin.developers': '開發者',
    'admin.send': '開 PR',
    'admin.perks': '特別名單',
    'admin.perksnote': '名單上的人可以上傳 GIF 披風、檔案上限 20 MB，跟開發者、贊助者、貢獻者一樣，但不會出現在貢獻者名單上，名字前面也不會多一片葉子。輸入他的 Minecraft UUID，有沒有連字號都可以；加了馬上生效。',
    'admin.uuid': 'Minecraft UUID', 'admin.note': '備註（可以不填）', 'admin.grant': '加入',
    'admin.noperks': '名單是空的。', 'admin.pwhen': '加入的時間', 'admin.pby': '誰加的',

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
      },
      noName: '（不知道名字）', lift: '解除', confirmLift: '解除 {who} 的封禁？', liftFail: '沒有成功（{s}）',
      revoke: '移除', confirmRevoke: '把 {who} 從特別名單移除？', perkDone: '加好了',
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
    'nav.home': 'Home', 'nav.capes': 'Capes', 'nav.account': 'Account', 'nav.admin': 'Admin', 'nav.label': 'Main menu',
    'lang.label': 'Language',

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
    'home.badges': 'Badges and capes',
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
    'capes.lede': 'Capes uploaded by WynnChaYuan players. Click one to turn it around.',
    'capes.mine': 'Upload my cape',
    'rules': '<b>Not allowed:</b> political, obscene or sexual, unlawful, racist or hateful content. Capes that break the rules are removed, and the uploader may lose the ability to upload.',
    'view.title': 'Cape', 'view.flat': 'The cape template image: front on the left, back on the right',
    'view.type': 'Format', 'view.dim': 'Dimensions', 'view.size': 'File size', 'view.users': 'Players', 'view.date': 'Uploaded',
    'view.del': 'Delete this cape', 'view.ban': 'Delete and ban the uploaders', 'view.close': 'Close',

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
    'account.upload': 'Upload a cape',
    'account.spec': 'Draw it on the <a href="' + TEMPLATES + '">template</a>: ' + SIZES + '. File size: up to 1 MB for regular players, 20 MB for developers, sponsors and contributors. Anyone can upload a PNG; GIFs (up to 120 frames) are for developers, sponsors and contributors.',
    'account.pick': 'Choose a cape image', 'account.send': 'Upload',

    'admin.title': 'Admin - WynnChaYuan',
    'admin.h1': 'Admin',
    'admin.gate': 'This page is for listed developers only. Please sign in from the game.',
    'admin.lede': 'Deleting capes and banning uploaders happens in the <a href="/capes">cape gallery</a>: open a cape to see who uploaded it.',
    'admin.bans': 'Bans',
    'admin.player': 'Player', 'admin.when': 'Banned at', 'admin.by': 'Banned by',
    'admin.nobans': 'Nobody is banned.',
    'admin.add': 'Add a member',
    'admin.addnote': 'This opens a pull request on GitHub that edits <code>credits.json</code>. Once merged, it takes effect the next time players sync the list (name-tag leaf, permission to upload GIF capes).',
    'admin.section': 'Role', 'admin.name': 'Display name', 'admin.mc': 'Minecraft ID', 'admin.color': 'Leaf colour (optional)',
    'admin.contributors': 'Contributor', 'admin.sponsors': 'Sponsor', 'admin.developers': 'Developer',
    'admin.send': 'Open PR',
    'admin.perks': 'Special list',
    'admin.perksnote': 'People on this list can upload GIF capes and files up to 20 MB, the same as developers, sponsors and contributors, but they are not shown on the credits list and get no leaf before their name. Enter their Minecraft UUID, with or without hyphens. It takes effect immediately.',
    'admin.uuid': 'Minecraft UUID', 'admin.note': 'Note (optional)', 'admin.grant': 'Add',
    'admin.noperks': 'The list is empty.', 'admin.pwhen': 'Added at', 'admin.pby': 'Added by',

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
      },
      noName: '(name unknown)', lift: 'Unban', confirmLift: 'Lift the ban on {who}?', liftFail: 'That did not work ({s})',
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
  function head(name, size) { return 'https://mc-heads.net/avatar/' + encodeURIComponent(name) + '/' + (size || 32); }
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

function layout(lang, title, active, body, script) {
  const nav = [['/', 'nav.home'], ['/capes', 'nav.capes'], ['/account', 'nav.account']]
    .map(([href, key]) => '<a href="' + href + '"' + (href === active ? ' aria-current="page"' : '')
      + '>%%' + key + '%%</a>').join('');
  const switcher = LANGS.map((code) => '<option value="' + code + '"' + (code === lang ? ' selected' : '')
    + ' lang="' + tagOf(code) + '">' + LANG_NAMES[code] + '</option>').join('');
  return '<!doctype html>\n<html lang="%%html%%">\n<head>\n<meta charset="utf-8">\n'
    + '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
    + '<meta name="color-scheme" content="dark">\n'
    + '<meta name="theme-color" content="#0f1512">\n'
    + '<link rel="icon" type="image/png" href="/favicon.png">\n'
    + '<link rel="apple-touch-icon" href="/apple-touch-icon.png">\n'
    + '<meta property="og:site_name" content="WynnChaYuan">\n'
    + '<meta property="og:image" content="https://wynnchayuan.com/icon-192.png">\n'
    + '<title>' + title + '</title>\n<style>' + STYLE + '</style>\n</head>\n<body>\n'
    + '<div class="top"><div class="in">'
    + '<a class="brand" href="/"><span>' + LEAF + '</span>WynnChaYuan</a>'
    + '<nav aria-label="%%nav.label%%">' + nav
    + '<a href="/admin" id="nav-admin" hidden' + (active === '/admin' ? ' aria-current="page"' : '')
    + '>%%nav.admin%%</a></nav>'
    + '<div class="tools"><select class="lang" id="lang" aria-label="%%lang.label%%">' + switcher + '</select>'
    + '<div class="me" id="me"></div></div>'
    + '</div></div>\n<main>' + body + '</main>\n'
    + '<footer><div class="in"><span>© 2026 WynnChaYuan - All rights reserved.</span>'
    + '<span>Designed by LyuChaCha</span></div></footer>\n'
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
    <div class="panel"><h3>%%home.f1%%</h3><p>%%home.f1b%%</p></div>
    <div class="panel"><h3>%%home.f2%%</h3><p>%%home.f2b%%</p></div>
    <div class="panel"><h3>%%home.f3%%</h3><p>%%home.f3b%%</p></div>
    <div class="panel"><h3>%%home.f4%%</h3><p>%%home.f4b%%</p></div>
    <div class="panel"><h3>%%home.f5%%</h3><p>%%home.f5b%%</p></div>
    <div class="panel"><h3>%%home.f6%%</h3><p>%%home.f6b%%</p></div>
  </div>

  <h2>%%home.install%%</h2>
  <div class="split">
    <div class="panel">
      <p style="margin:0">%%home.need%%</p>
      <p class="note" style="margin:8px 0 0">%%home.how%%</p>
    </div>
    <div class="panel note">%%home.about%%</div>
  </div>

  <h2>%%home.badges%%</h2>
  <div class="roles">
    <div class="panel role">` + leaf('#FFD24A') + String.raw`<span><b>%%home.dev%%</b><span>%%home.devb%%</span></span></div>
    <div class="panel role">` + leaf('#5BD8E8') + String.raw`<span><b>%%home.sponsor%%</b><span>%%home.sponsorb%%</span></span></div>
    <div class="panel role">` + leaf('#5BD86F') + String.raw`<span><b>%%home.contrib%%</b><span>%%home.contribb%%</span></span></div>
    <div class="panel role">` + leaf('#A8A8A8') + String.raw`<span><b>%%home.user%%</b><span>%%home.userb%%</span></span></div>
  </div>
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

const CAPES = String.raw`
  <h1>%%capes.h1%%</h1>
  <p class="lede">%%capes.lede%%</p>
  <div class="bar">
    <span class="count" id="count"></span>
    <a class="btn" href="/account">%%capes.mine%%</a>
  </div>
  <p class="panel note">%%rules%%</p>
  <div class="grid" id="grid" style="margin-top:22px"></div>

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
          <dt>%%view.date%%</dt><dd id="d-date"></dd>
        </dl>
        <ul class="owners" id="owners" hidden></ul>
        <p class="hint" id="hint"></p>
        <div class="spacer"></div>
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
  var current = null;
  var viewer = null;
  var developer = false;
  document.getElementById('count').textContent = L.loading;
  function url(cape) { return '/api/cape/' + cape.ids[0] + '?v=' + cape.v; }

  function open(cape) {
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
    if (!viewer) { hint.textContent = L.no3d; return; }
    hint.textContent = L.drag;
    wear(viewer, url(cape), cape.t === 'gif');
  }
  function shut() { stopCape(); current = null; if (dialog.open) dialog.close(); }
  document.getElementById('close').onclick = shut;
  dialog.addEventListener('close', stopCape);
  dialog.addEventListener('click', function (e) { if (e.target === dialog) shut(); });

  function remove(ban) {
    var cape = current;
    if (!cape) return;
    if (!confirm(fmt(ban ? L.confirmBan : L.confirmDel, { n: cape.ids.length }))) return;
    var body = { hash: cape.h };
    if (ban) body.ban = true;
    api('/admin/remove', { method: 'POST', body: JSON.stringify(body), headers: { 'content-type': 'application/json' } })
      .then(function (r) {
        if (r.ok) { shut(); load(); }
        else alert(fmt(L.expired, { s: r.status }));
      });
  }
  document.getElementById('del').onclick = function () { remove(false); };
  document.getElementById('ban').onclick = function () { remove(true); };

  function load() {
    return api(developer ? '/admin/capes' : '/capes').then(function (r) { return r.json(); }).then(function (data) {
      var count = document.getElementById('count');
      var parts = L.count.split('{n}');
      count.replaceChildren(parts[0], el('b', '', String(data.capes.length)), parts[1] || '');
      grid.replaceChildren();
      if (!data.capes.length) grid.append(el('div', 'empty', L.empty));
      data.capes.forEach(function (cape) {
        var card = el('button', 'card');
        card.type = 'button';
        card.setAttribute('aria-label', fmt(L.card, { t: cape.t.toUpperCase(), n: cape.ids.length }));
        var stage = el('div', 'stage');
        var face = el('div', 'face');
        var img = el('img');
        img.loading = 'lazy';
        img.alt = '';
        img.src = url(cape);
        face.append(img);
        stage.append(face);
        var meta = el('div', 'meta');
        meta.append(el('span', 'tag ' + cape.t, cape.t.toUpperCase()), el('span', 'who', fmt(L.wearing, { n: cape.ids.length })));
        card.append(stage, meta);
        card.onclick = function () { open(cape); };
        grid.append(card);
      });
    });
  }
  whoami.then(function (me) {
    developer = Boolean(me && me.developer);
    return load();
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
        <div style="margin-top:12px"><button class="btn danger" id="remove" hidden>%%account.remove%%</button></div>
      </div>
    </div>
    <h2>%%account.upload%%</h2>
    <div class="panel" style="max-width:720px">
      <p class="note" style="margin:0 0 12px">%%account.spec%%</p>
      <p class="note" style="margin:0 0 12px">%%rules%%</p>
      <input type="file" id="file" accept="image/png,image/gif" aria-label="%%account.pick%%">
      <button class="btn primary" id="upload" style="margin-left:8px">%%account.send%%</button>
      <p class="msg" id="msg" role="status"></p>
    </div>
  </div>
`;

const ACCOUNT_JS = SKINVIEW + '<script>' + VIEWER + String.raw`
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
    if (!viewer) { hint.textContent = L.mineNo3d; canvas.hidden = true; return; }
    hint.textContent = fmt(L.mineHint, { t: me.cape.t.toUpperCase(), s: bytes(me.cape.s),
      d: me.cape.x ? me.cape.x + '×' + me.cape.y + ' · ' : '' });
    wear(viewer, '/api/cape/' + me.id + '?v=' + me.cape.v, me.cape.t === 'gif');
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
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(function () {
          linkMsg.className = 'msg ok';
          linkMsg.textContent = L.copied;
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
      else say(fmt(L.picked, { w: w, h: h, s: bytes(file.size) }), true);
    };
    probe.onerror = function () { URL.revokeObjectURL(address); say(L.why['bad format'], false); };
    probe.src = address;
  };
  document.getElementById('unlink').onclick = function () {
    if (!confirm(L.confirmUnlink)) return;
    api('/unlink', { method: 'POST', body: '{}', headers: { 'content-type': 'application/json' } })
      .then(function () { location.reload(); });
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
    say(L.uploading, true);
    api('/cape', { method: 'PUT', body: file, headers: { 'content-type': file.type || 'application/octet-stream' } })
      .then(function (r) {
        return r.json().catch(function () { return {}; }).then(function (body) {
          if (r.ok) { say(fmt(L.uploaded, { w: body.width, h: body.height }), true); refresh(); }
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
  whoami.then(function (me) {
    if (!me || !me.developer) return;
    document.getElementById('gate').hidden = true;
    document.getElementById('panel').hidden = false;
    loadBans();
    loadPerks();
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
  '/': ['home.title', HOME, ''],
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
export function render(path, lang) {
  if (!Object.hasOwn(PAGES, path)) return null;
  const code = Object.hasOwn(TEXT, lang) ? lang : 'en';
  const key = code + ' ' + path;
  if (!built.has(key)) {
    const text = filled(code);
    const [title, body, script] = PAGES[path];
    const html = layout(code, '%%' + title + '%%', path, body, script)
      .replace(/%%([\w.]+)%%/g, (all, name) => {
        if (name === 'js') return JSON.stringify(text.js).replace(/</g, '\\u003c');
        return Object.hasOwn(text, name) ? text[name] : all;
      });
    built.set(key, html);
  }
  return built.get(key);
}

/** 這個語言的字，缺的拿英文補：新加的字還沒翻到的語言不會冒出一個 %%鍵%%。 */
function filled(code) {
  const own = TEXT[code];
  if (code === 'en') return own;
  const base = TEXT.en;
  const js = { ...base.js, ...(own.js || {}) };
  js.why = { ...base.js.why, ...((own.js && own.js.why) || {}) };
  js.credit = { ...base.js.credit, ...((own.js && own.js.credit) || {}) };
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
