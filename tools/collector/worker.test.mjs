/**
 * 收集站那一道濾網。
 *
 * 跑法：`node tools/collector/worker.test.js`（不需要 wrangler，也不連網）。
 *
 * 這裡測的是 worker.js 的 `acceptable`——三道濾網的第二道。第一道在模組端
 * （`CorpusShareTest`），第三道在 `tools/import-captured.py`（`--selftest`）。
 * 三道各自獨立，所以三邊都要有自己的測試。
 */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const source = readFileSync(join(here, 'worker.js'), 'utf8');

// worker.js 是給 Cloudflare 跑的模組，acceptable 沒有匯出。與其為了測試改動
// 正式檔的形狀，不如把它連同依賴的常數一起取出來評估——測的仍然是同一份程式碼。
const slice = source.slice(
  source.indexOf('/** 一次最多收幾條'),
  source.indexOf('async function sha256'),
) + source.slice(
  source.indexOf('/** 這一條可以收嗎'),
  source.indexOf('async function rateLimited'),
);
const acceptable = new Function(slice + '; return acceptable;')();

let failures = 0;

function check(what, ok) {
  console.log(`  [${ok ? 'PASS' : 'FAIL'}] ${what}`);
  if (!ok) failures++;
}

const yes = (what, src, ctx) => check(`可以收：${what}`, acceptable({ src, ctx }));
const no = (what, src, ctx) => check(`★ 不可收：${what}`, !acceptable({ src, ctx }));

yes('任務對話', 'Good luck in there, recruits!', "dialogue/King's Recruit");
yes('介面文字', 'Available Points', 'gui/line');
yes('物品說明', "This item's power has been sealed.", 'tooltip/lore');
yes('伺服器公告', '{#} Found a bug? Use /bug to report it!', 'chat/INFO');
yes('NPC 名牌', 'Otium', 'label/floating');

no('公會頻道', 'anyone want to do a raid', 'chat/GUILD');
no('隊伍頻道', 'im at the bank', 'chat/PARTY');
no('喊話', 'WTS mythic cheap', 'chat/SHOUT');
no('私訊', 'hey are you there', 'chat/PRIVATE');
no('沒見過的來源', 'whatever', 'somewhere/else');

no('公會標籤', '- Cloud Tavern [CTRN]', 'gui/line');
no('領地控制', 'Controlled by Paladins United', 'label/floating');
no('製作者署名', 'Crafted by SomeOne', 'tooltip/lore');
no('帳號名的底線', 'Green_teaTW', 'label/floating');
no('公會欄位', '- Guild: Cloud Tavern', 'gui/line');
no('收禮通知', 'SomeOne has given you an item', 'gui/line');

// ★ 這一輪跑世界事件回傳的語料裡漏掉的四類。三道濾網都要各自擋得住。
no('丟炸彈的人（折行截斷）', '{#} {#}JC grindeando has thrown a', 'chat/INFO');
no('炸彈過期通知', '{#} JC grindeando Loot Bomb has expired!', 'chat/INFO');
no('謝謝某某', '{#} {#}Thank JC grindeando', 'chat/INFO');
no('石碑的主人', "{~}jimmy's Totem of Tales", 'label/floating');
no('死亡快訊', '{#}{#}ChangJenChief has died', 'chat/INFO');
no('討伐戰的增益選擇', '{#} Watari has chosen the Elder III buff!', 'chat/INFO');
no('石碑廣播', '{#} PoorChaCha has placed a mob totem in {p}', 'chat/INFO');
no('開箱廣播', '{#} userUwU has gotten a Spear from their crate!', 'chat/INFO');
no('上線廣播', 'eric18960 has logged into server AS12', 'chat/INFO');
no('喊話', '{#} PoorChaCha shouts: hello', 'chat/INFO');
no('組隊搜尋', '{#} Party Finder: Hey MorphCascade, over here!', 'chat/INFO');

// ★ 反方向：句型比對最容易連坐到旁邊的正常句子。
yes('沒有所有格的圖騰提示',
    '{#} You are gaining effects from a Totem of Tales!', 'chat/INFO');
yes('劇情裡的死亡', 'The king has died in his sleep.', "dialogue/X");
yes('對話裡的道謝', 'Thank you for your help!', "dialogue/X");
yes('世界事件公告',
    '{#} The Lonely Islet World Event starts in {~}m {~}s!', 'chat/INFO');
yes('未進場提示', '{#} You did not enter the event radius in time', 'chat/INFO');

no('空字串', '', 'dialogue/X');
no('純符號', '{#}{#}', 'gui/line');
no('太長', 'x'.repeat(601) + ' word', 'dialogue/X');
no('沒有 src', undefined, 'dialogue/X');
no('src 不是字串', 12345, 'dialogue/X');

console.log(failures === 0 ? 'Worker: 全部通過' : `Worker: ${failures} 項失敗`);
if (failures > 0) process.exit(1);
