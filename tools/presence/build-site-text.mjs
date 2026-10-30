// node tools/presence/build-site-text.mjs [--check]
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
