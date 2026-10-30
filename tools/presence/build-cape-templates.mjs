// node tools/presence/build-cape-templates.mjs [--check]
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const source = path.join(here, '..', '..', 'capes', 'templates');
const target = path.join(here, 'cape-templates.js');
const TYPES = { '.png': 'image/png', '.gif': 'image/gif' };
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
