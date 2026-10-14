// 披風範本：把 capes/templates/ 裡的範本、說明圖與一張示範打包成 cape-templates.js，
// 網站才能從自己的網址（/template/<檔名>）給人下載。
//
//   node tools/presence/build-cape-templates.mjs           重寫 cape-templates.js
//   node tools/presence/build-cape-templates.mjs --check   跟現在的檔案比，不一樣就以 1 結束
//
// 使用者 2026-10-14：「披風範本希望可以更明顯一點，有使用者回饋找不太到」。原本只是帳號頁
// 一句話裡的連結，連到 GitHub 的資料夾；現在披風一覽與帳號頁都有一塊，按尺寸直接下載。
// capes/templates/ 裡的範本改了要重跑這一支。
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const source = path.join(here, '..', '..', 'capes', 'templates');
const target = path.join(here, 'cape-templates.js');
const TYPES = { '.png': 'image/png', '.gif': 'image/gif' };
// 範本每一種尺寸、兩份說明圖（中文、英文）、一張會動的示範
const wanted = (name) => /^template-\d+x\d+\.png$/.test(name) || name === 'guide.png' || name === 'guide.en.png'
  || name === 'example-88x68.gif';

const files = {};
for (const name of fs.readdirSync(source).filter(wanted).sort()) {
  const data = fs.readFileSync(path.join(source, name));
  files[name] = {
    type: TYPES[path.extname(name)],
    v: crypto.createHash('sha256').update(data).digest('hex').slice(0, 10),
    data: data.toString('base64'),
  };
}
const out = '/**\n * 披風範本、說明圖與示範（capes/templates/）。這個檔是 build-cape-templates.mjs 產生的，不要手改。\n */\n'
  + 'export const CAPE_TEMPLATES = ' + JSON.stringify(files, null, 1) + ';\n';

if (process.argv.includes('--check')) {
  const have = fs.existsSync(target) ? fs.readFileSync(target, 'utf8').replace(/\r\n/g, '\n') : '';
  if (have !== out) {
    console.error('cape-templates.js 跟 capes/templates/ 不一樣了：重跑 node tools/presence/build-cape-templates.mjs');
    process.exit(1);
  }
  console.log('cape-templates.js 是最新的。');
} else {
  fs.writeFileSync(target, out);
  console.log('寫好了 cape-templates.js：' + Object.keys(files).join('、'));
}
