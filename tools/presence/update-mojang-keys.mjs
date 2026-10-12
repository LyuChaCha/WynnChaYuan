/**
 * Mojang 的公鑰：抓下來寫進 mojang-keys.js，或只檢查有沒有換過。
 *
 *   node tools/presence/update-mojang-keys.mjs           重寫 mojang-keys.js
 *   node tools/presence/update-mojang-keys.mjs --check   跟現在的檔案比，不一樣就以 1 結束
 *
 * 為什麼要釘在程式裡：登記站在 Cloudflare Worker 上，而 Mojang 把 Worker 發出去的請求
 * 一律擋掉（403），所以它沒辦法自己去抓。公鑰換了的話登入會全部失敗——重跑這一支、
 * 重新部署就好；來不及部署時也可以先用 wrangler 設 MOJANG_KEYS（見 README）。
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const SOURCE = 'https://api.minecraftservices.com/publickeys';
const target = fileURLToPath(new URL('./mojang-keys.js', import.meta.url));

const answer = await fetch(SOURCE);
if (!answer.ok) {
  console.error('抓不到 ' + SOURCE + '：' + answer.status);
  process.exit(2);
}
const keys = await answer.json();
const list = (name) => {
  const out = (keys[name] || []).map((k) => k.publicKey).filter((k) => typeof k === 'string');
  if (!out.length) {
    console.error('回應裡沒有 ' + name);
    process.exit(2);
  }
  return out;
};
const lines = (name) => list(name).map((k) => "  '" + k + "',\n").join('');
const text = '/**\n'
  + ' * Mojang 的公鑰（X.509 SPKI，base64）。這個檔是 update-mojang-keys.mjs 產生的，不要手改。\n'
  + ' *\n'
  + ' * CERT_KEYS：簽「玩家金鑰憑證」的（publickeys 的 playerCertificateKeys）。\n'
  + ' * PROFILE_KEYS：簽個人檔案屬性（textures）的（profilePropertyKeys）。\n'
  + ' * 兩種都是 SHA1withRSA。\n'
  + ' */\n'
  + 'export const CERT_KEYS = [\n' + lines('playerCertificateKeys') + '];\n\n'
  + 'export const PROFILE_KEYS = [\n' + lines('profilePropertyKeys') + '];\n';

if (process.argv.includes('--check')) {
  let have = '';
  try {
    have = readFileSync(target, 'utf8').replace(/\r\n/g, '\n');
  } catch (e) {
    // 沒有檔案就是不一樣
  }
  if (have !== text) {
    console.error('Mojang 的公鑰跟 mojang-keys.js 不一樣了：重跑這一支（不帶 --check）再部署。');
    process.exit(1);
  }
  console.log('Mojang 的公鑰沒有換。');
} else {
  writeFileSync(target, text);
  console.log('寫好了 ' + target);
}
