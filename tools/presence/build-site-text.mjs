/**
 * 把 lang/<代碼>.json 併成 site-text.js（Worker 直接 import，不必在執行時讀檔）。
 *
 *   node tools/presence/build-site-text.mjs           重寫 site-text.js
 *   node tools/presence/build-site-text.mjs --check   跟現在的檔案比，不一樣就以 1 結束
 *
 * 繁體中文與英文的字在 site.js 的 TEXT（那兩份是正本）；其他語言一個檔一種，鍵跟英文那一份
 * 一樣。改了 lang/ 底下的檔要重跑這一支，再部署。少翻的鍵網頁會拿英文補。
 */
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const dir = fileURLToPath(new URL('./lang/', import.meta.url));
const target = fileURLToPath(new URL('./site-text.js', import.meta.url));

const all = {};
for (const file of readdirSync(dir).filter((name) => name.endsWith('.json')).sort()) {
  const code = file.slice(0, -5);
  const text = JSON.parse(readFileSync(dir + file, 'utf8').replace(/^﻿/, ''));
  if (!text || typeof text !== 'object' || typeof text.html !== 'string' || !text.js) {
    console.error(file + '：不像一份網站的字（要有 html 與 js）');
    process.exit(2);
  }
  all[code] = text;
}
const out = '/**\n * 網站其他語言的字。這個檔是 build-site-text.mjs 從 lang/*.json 產生的，不要手改。\n */\n'
  + 'export const MORE_TEXT = ' + JSON.stringify(all, null, 1) + ';\n';

if (process.argv.includes('--check')) {
  let have = '';
  try {
    have = readFileSync(target, 'utf8').replace(/\r\n/g, '\n');
  } catch (e) {
    // 沒有檔案就是不一樣
  }
  if (have !== out) {
    console.error('site-text.js 跟 lang/*.json 不一樣了：重跑 node tools/presence/build-site-text.mjs');
    process.exit(1);
  }
  console.log('site-text.js 是最新的（' + Object.keys(all).join('、') + '）。');
} else {
  writeFileSync(target, out);
  console.log('寫好了 site-text.js：' + (Object.keys(all).join('、') || '（還沒有其他語言）'));
}
