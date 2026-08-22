# mage 技能樹：與翻譯團隊匯出檔的差異

由 `tools/import-ability-tree.py` 產生。

標「未自動套用」的請人工挑——硬套會張冠李戴，或是佔位符數量對不上讓整條失效。

## Heal（佔位符對不上，未自動套用）

- 原文：`Heals you and nearby allies in
a large area around you.
(When healing others, you can't heal
more than {~} of their max health)`
- 舊譯：治療你自己與周圍大範圍內的友軍。
(治療他人時，單次無法治療
超過其最大生命的 {~})
- 新譯：治癒你和你身邊的友軍。

## Water Mastery

- 原文：`Increase your base damage
from all Water attacks.`
- 舊譯：增加水屬性傷害。
- 新譯：增加風屬性傷害。

## Thunder Mastery

- 原文：`Increase your base damage
from all Thunder attacks.`
- 舊譯：增加雷屬性傷害。
- 新譯：增加地屬性傷害。

## Air Mastery

- 原文：`Increase your base damage
from all Air attacks.`
- 舊譯：增加風屬性傷害。
- 新譯：增加火屬性傷害。

## Fire Mastery

- 原文：`Increase your base damage
from all Fire attacks.`
- 舊譯：增加火屬性傷害。
- 新譯：增加雷屬性傷害。

## Earth Mastery

- 原文：`Increase your base damage
from all Earth attacks.`
- 舊譯：增加地屬性傷害。
- 新譯：增加水屬性傷害。

## Distortion（佔位符對不上，未自動套用）

- 原文：`Each Distortion {#} gives +{~} Raw Damage
and decays at a rate of -{~} per second,
increasing as you gain Distortion {#}.`
- 舊譯：每一層 Distortion {#} 提供 +{~1} 傷害值，
並以每秒 -{~2} 的速度衰減;
Distortion {#} 疊得愈高，衰減得愈快。
- 新譯：每一層 扭曲 §#c267f7≈ 提供玩家 1 傷害，玩家每秒失去 1 扭曲 §#c267f7≈，此數字隨著玩家身上的 扭曲 §#c267f7≈ 層數提升。

## Distortion（佔位符對不上，未自動套用）

- 原文：`Hitting an enemy with Etheric Slash grants up
to +{~} Distortion {#} per {~}s.`
- 舊譯：用 Etheric Slash 命中敵人時，每 {~2} 秒
最多獲得 +{~1} 層 Distortion {#} .
- 新譯：使用裂界斬命中敵人會給予自身 扭曲 §#c267f7≈，其層數取決於與上次命中時的間隔，每 0.2 秒一層，最高一次獲得 20 層。

## Thunderstorm（佔位符對不上，未自動套用）

- 原文：`After casting Meteor, generate a lightning
strike near the point of impact that
adds +{~} Mana to your Mana Bank ✺
for each aggressive enemy you hit.`
- 舊譯：施放 Meteor 之後，會在落點附近
產生一道閃電;你每命中一個具敵意的敵人，
就為你的 Mana Bank ✺ 增加 +{~} 點魔力。
- 新譯：使用隕石時會在目標處打雷，
命中敵方時，魔力儲庫 ✺ 增加 8 點魔力。

## Astral Fragmentation（佔位符對不上，未自動套用）

- 原文：`Ophanim draws light from the
stars, dealing additional damage.`
- 舊譯：Ophanim 會從星辰汲取光芒，
造成額外傷害。
- 新譯：奧法尼姆吸收星光的力量，造成額外傷害。
☀ Meteor Area of Effect: 6 Blocks (Circle-Shaped)

## Ophanim（佔位符對不上，未自動套用）

- 原文：`When casting Meteor, instead summon
{~} orbs of light with {~} Health that will
attack when you use your Main Attack.`
- 舊譯：施放 Meteor 時改為召喚 {~1} 顆
擁有 {~2} 點生命的光球;當你使用普攻時，
它們會跟著攻擊。
- 新譯：使用 2 顆血量為 200 的光球取代你的隕石，每當你進行普通攻擊時，光球將會追加攻擊。
當光球命中目標時，其失去 20% 血量，可以使用治癒回復其血量。

## Ophanim（配不出對應的段落）

- 原文：`When they damage an enemy, they lose {~}
of their Health. They can be healed back.`
- 舊譯：它們每對敵人造成一次傷害，就會失去 {~}
的生命。它們可以被治療回來。
- 新譯：使用 2 顆血量為 200 的光球取代你的隕石，每當你進行普通攻擊時，光球將會追加攻擊。 ⏎ 當光球命中目標時，其失去 20% 血量，可以使用治癒回復其血量。

## Arcane Transfer（佔位符對不上，未自動套用）

- 原文：`Heal will now transfer the contents of your
Mana Bank ✺ into usable Mana instead of healing.`
- 舊譯：Heal 不再治療，改為把你 Mana Bank ✺ 裡
的存量轉換成可用的魔力。
- 新譯：治癒不再回復血量，但會提取 魔力儲庫 ✺ 的魔力至玩家身上。

## Arcane Transfer（佔位符對不上，未自動套用）

- 原文：`Meteor, Ice Snake, and Etheric Slash will add +{~} Mana to
a Mana Bank ✺ for every aggressive enemy you hit.
Frozen Tornado and Meteor Shower will add +{~} Mana.`
- 舊譯：Meteor、Ice Snake 與 Etheric Slash 每命中一個具敵意的敵人，
就為 Mana Bank ✺ 增加 +{~1} 點魔力。
Frozen Tornado 與 Meteor Shower 則增加 +{~2} 點魔力。
- 新譯：隕石、冰蛇和裂界斬命中敵對目標時，魔力儲庫 ✺ 增加 13 點魔力。
冰凍龍捲風和流星雨增加 4 點魔力。

## Interweave（佔位符對不上，未自動套用）

- 原文：`Hitting an ally with a Lightweaver orb
consumes the orb and applies Shining {#}.`
- 舊譯：用 Lightweaver 的光球命中友軍時，
會消耗該光球並施加 Shining {#}.
- 新譯：使用織光的光球命中友軍時，消耗該光球並給予該友軍 閃耀 §#e1dca4✨狀態。
擁有 閃耀 §#e1dca4✨狀態的友軍可無視距離接受治癒，
並對附近的敵方造成織光的光球傷害。

## Interweave（配不出對應的段落）

- 原文：`Shining {#} players can be healed from
any distance, and they deal the orb's
damage to nearby enemies.`
- 舊譯：處於 Shining {#} 的玩家可以在
任意距離被治療，並且會對周圍敵人
造成該光球的傷害。
- 新譯：使用織光的光球命中友軍時，消耗該光球並給予該友軍 閃耀 §#e1dca4✨狀態。 ⏎ 擁有 閃耀 §#e1dca4✨狀態的友軍可無視距離接受治癒， ⏎ 並對附近的敵方造成織光的光球傷害。

## Displacement（配不出對應的段落）

- 原文：`You're teleported to its location upon recasting it.`
- 舊譯：再次施放時，你會被傳送到虛影的位置。
- 新譯：蹲下使用傳送將會在原地留下一個虛影。 ⏎ 再度使用此技能時，傳送至虛影的位置。

## Frozen Tornado（佔位符對不上，未自動套用）

- 原文：`For every +{~}{#}Distortion {#}, the tornado gains
+{~}{#}{#}{#}Area of Effect{#}and +{~}{#}{#}{#}Damage.`
- 舊譯：每 +{~1}{#}層 Distortion {#},龍捲風的
{#}{#}{#}技能範圍{#}增加 +{~2},{#}{#}{#}傷害增加 +{~3}.
- 新譯：每一層 扭曲 §#c267f7≈，會使龍捲風
增加 0.025 ☀ 作用範圍 和 1.1% ⚔ 傷害。
⌛ Tornado Duration: 5s

## Pyrokinesis（佔位符對不上，未自動套用）

- 原文：`When your Mana Bank ✺ reaches {~},
your Main Attack will explode
upon impact with enemies.`
- 舊譯：當你的 Mana Bank ✺ 達到 {~} 時，
你的普攻會在命中敵人時
引發爆炸。
- 新譯：當你的 魔力儲庫 ✺ 有超過 30 魔力時，
你的普通攻擊將會爆炸，造成範圍傷害。

## Warp Blast（佔位符對不上，未自動套用）

- 原文：`Hitting an enemy with Main Attack grants up
to +{~} Distortion {#} per {~}s.`
- 舊譯：用普攻命中敵人時，每 {~2} 秒
最多獲得 +{~1} 層 Distortion {#} .
- 新譯：使用 普通攻擊 命中敵人會給予自身 扭曲 §#c267f7≈，其層數取決於與上次命中時的間隔，每 0.5 秒一層，最高一次獲得 8 層。

## Orphion's Pulse（佔位符對不上，未自動套用）

- 原文：`Heal becomes slower and stronger,
pulsing {~} more times for greater healing.`
- 舊譯：Heal 變得更慢也更強，
多脈動 {~} 次以提供更高的治療量。
- 新譯：你的治癒變得更緩慢但更強大，且會重複施放兩次。
⌚ Delay: 1.5s (per pulse)

## Arcane Restoration（佔位符對不上，未自動套用）

- 原文：`Pyrokinesis will add +{~} Mana every
{~}s to your Mana Bank ✺ when
hitting an aggressive enemy.`
- 舊譯：使用馭火命中敵對目標時，魔力儲庫 ✺ 每 {~} 秒增加 {~} 點魔力。
- 新譯：使用馭火命中敵對目標時，魔力儲庫 ✺ 每 0.25 秒增加 1 點魔力。

## Meteor Shower（佔位符對不上，未自動套用）

- 原文：`Casting Meteor summons an additional
meteor near the target location for
every {~} Distortion {#} you have.
(Max {~} Meteors, only one Meteor
Shower can be active at the same time.)`
- 舊譯：施放 Meteor 時，你每擁有 {~1} 層
Distortion {#} ,就在目標位置附近
額外召喚一顆隕石。
(最多 {~2} 顆隕石，同時只能有一組
Meteor Shower 生效。)
- 新譯：每 20 層 扭曲 §#c267f7≈ 會在你使用隕石的目標處附近額外召喚一顆隕石。

## Meteor Shower（佔位符對不上，未自動套用）

- 原文：`Meteor and Astral Fragmentation become
smaller, dealing -{~} {#} Damage and
losing -{~} {#} Area of Effect.`
- 舊譯：Meteor 與 Astral Fragmentation 會變小，
{#} 傷害減少 -{~1},
{#} 技能範圍減少 -{~2}.
- 新譯：隕石和星碎將變得更小，
降低 20% ⚔ 傷害 及 20% ☀ 作用範圍。
☀ Target Radius: 1 Blocks (Circle-Shaped)

## Chaos Explosion（佔位符對不上，未自動套用）

- 原文：`When your Mana Bank ✺ reaches {~},
casting Arcane Transfer will rapidly unleash
the last {~} spells you've cast in order.`
- 舊譯：當你的 Mana Bank ✺ 達到 {~1} 時，
施放 Arcane Transfer 會依序快速
重新釋放你最後施放的 {~2} 個法術。
- 新譯：當你的魔力儲庫 ✺有超過 120 點魔力時，
使用祕法回流將會依照原順序快速施放前 3 個攻擊法術。

## Crystallize（佔位符對不上，未自動套用）

- 原文：`Casting Ice Snake shatters
enemies with {~} Crystallized {#},
damaging and slowing them.`
- 舊譯：施放 Ice Snake 會震碎帶有
{~} 層 Crystallized {#} 的敵人，
對其造成傷害並使其緩速。
- 新譯：在其身上疊加 7 結晶化 §j💎。
對已疊加 100 結晶化 §j💎 的目標使用冰蛇
會消耗 結晶化 §j💎 使其碎裂，造成傷害並緩速目標。
若 5 秒內未疊加 結晶化 §j💎，
將會每秒降低 5 層直至歸零。

## Crystallize（配不出對應的段落）

- 原文：`Crystallized {#} starts decaying at
a rate of -{~} every second if
none is applied for {~}s.`
- 舊譯：若 {~2} 秒內沒有再施加，
Crystallized {#} 會開始以每秒 -{~1}
的速度衰減。
- 新譯：使用織光命中敵方目標時， ⏎ 在其身上疊加 7 結晶化 §j💎。 ⏎ 對已疊加 100 結晶化 §j💎 的目標使用冰蛇 ⏎ 會消耗 結晶化 §j💎 使其碎裂，造成傷害並緩速目標。 ⏎ 若 5 秒內未疊加 結晶化 §j💎， ⏎ 將會每秒降低 5 層直至歸零。

## Crystallize（佔位符對不上，未自動套用）

- 原文：`Lightweaver applies +{~} Crystallized {#}
to enemies it hits.`
- 舊譯：Lightweaver 命中的敵人會被施加
+{~} 層 Crystallized {#}.
- 新譯：使用織光命中敵方目標時，

## Vacuokinesis（佔位符對不上，未自動套用）

- 原文：`For every +{~}{#}Distortion {#}, Vacuokinesis
deals +{~}{#}{#}{#}Damage.`
- 舊譯：每 +{~1}{#}層 Distortion {#},Vacuokinesis
的{#}{#}{#}傷害增加 +{~2}.
- 新譯：每一層 扭曲 §#c267f7≈，會使馭虛增加 0.8% ⚔傷害。

## Vacuokinesis（配不出對應的段落）

- 原文：`Your Main Attack is imbued with vacuum,
dealing additional damage.`
- 舊譯：你的普攻被灌注了真空之力，
造成額外傷害。
- 新譯：你的普通攻擊附帶了真空之力，造成額外傷害。 ⏎ 每一層 扭曲 §#c267f7≈，會使馭虛增加 0.8% ⚔傷害。

## Dimensional Tear（佔位符對不上，未自動套用）

- 原文：`Hitting an enemy with Etheric Slash tears
open a dimensional rift between you
and your destination.`
- 舊譯：用 Etheric Slash 命中敵人時，會在你
與目的地之間撕開一道次元裂隙。
- 新譯：使用裂界斬命中敵人時，會在你與目的地之間撕開一道次元裂隙。
當你位於裂隙中時，
裂隙每 0.4 秒造成一次傷害，

## Dimensional Tear（佔位符對不上，未自動套用）

- 原文：`The rift damages enemies every {~}s, increases
your Distortion {#} gain by +{~} and reduces
damage taken while standing in it.`
- 舊譯：裂隙每 {~1}s 對敵人造成一次傷害，
讓你的 Distortion {#} 累積速度 +{~2},
並降低站在其中時受到的傷害。
- 新譯：降低你承受的傷害並使你的 扭曲 §#c267f7≈ 層數獲取量提高 25%。
此效果在離開裂隙後會持續 3秒。

## Dimensional Tear（配不出對應的段落）

- 原文：`The rift's benefits and damage linger
for {~}s upon leaving its area.`
- 舊譯：離開裂隙範圍後，它的增益與傷害
仍會殘留 {~}s.
- 新譯：使用裂界斬命中敵人時，會在你與目的地之間撕開一道次元裂隙。 ⏎ 當你位於裂隙中時， ⏎ 裂隙每 0.4 秒造成一次傷害， ⏎ 降低你承受的傷害並使你的 扭曲 §#c267f7≈ 層數獲取量提高 25%。 ⏎ 此效果在離開裂隙後會持續 3秒。

## Arcane Power（佔位符對不上，未自動套用）

- 原文：`Meteor, Ice Snake, and Etheric Slash will add +{~} Mana to
your Mana Bank ✺ for each aggressive enemy you hit.
Frozen Tornado and Meteor Shower will add +{~} Mana.`
- 舊譯：Meteor、Ice Snake 與 Etheric Slash 每命中一個具敵意的敵人，
就為你的 Mana Bank ✺ 增加 +{~1} 點魔力。
Frozen Tornado 與 Meteor Shower 則增加 +{~2} 點魔力。
- 新譯：隕石、冰蛇和裂界斬命中敵對目標時，魔力儲庫 ✺ 增加 8 點魔力。
冰凍龍捲風和流星雨增加 4 點魔力。

## Time Dilation（佔位符對不上，未自動套用）

- 原文：`Creates an area of effect when sprinting
that increases the walk speed of all
allies the longer they run in it.
(You lose {~} Walk Speed Bonus every second
once you stop running)`
- 舊譯：衝刺時產生一個作用區域，
區域內的所有友軍在其中跑得愈久，
移動速度加成就愈高。
(一旦停止跑動，你每秒會失去
{~} 的移動速度加成)
- 新譯：在你衝刺時創造一個領域，增加其中所有友軍的移動速度。

## Sunflare（佔位符對不上，未自動套用）

- 原文：`While Sunflare is active, you will restore
health and mana to all nearby allies, and
your Ophanim orbs will attack constantly.`
- 舊譯：Sunflare 生效期間，你會為周圍所有友軍
回復生命與魔力，而且你的
Ophanim 光球會持續攻擊。
- 新譯：且奧法尼姆會自動攻擊。
✺ Mana Regen: 5% per second (of your max mana)

## Freezing Sigil（配不出對應的段落）

- 原文：`ÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀ Knockback Immunity to Allies`
- 舊譯：ÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀ 給友軍擊退免疫
- 新譯：冰蛇會在地上創造冰霜印記， ⏎ 每 0.8 秒造成一次傷害並緩速目標。 ⏎ 在冰霜印記上的友軍不會被擊退。

## Accelerated Strike（配不出對應的段落）

- 原文：`Time Dilation increases your Main Attack damage
for every {~} Walk Speed you gain from it.`
- 舊譯：你每從 Time Dilation 獲得 {~} 移動速度，
Time Dilation 就提升你的普攻傷害。
- 新譯：時流偏移在提供移動速度的同時增加你的普通攻擊傷害。 ⏎ 使用普通攻擊命中敵人能使移動速度不因停止衝刺而降低，持續 2 秒。

## Influx Shift（佔位符對不上，未自動套用）

- 原文：`Increase the effects gained from Distortion {#} by +{~}.`
- 舊譯：將 Distortion {#} 帶來的效果提升 +{~}.
- 新譯：扭曲 §#c267f7≈ 層數的獲取量提高 20% 並使其給予的效果增加 25%。

## Devitalize（佔位符對不上，未自動套用）

- 原文：`You gain +{~} Distortion {#} for every enemy weakened.
(Up to +{~})`
- 舊譯：每有一個敵人被虛弱，你就獲得 +{~1} 層 Distortion {#} .
(最多 +{~2})
- 新譯：每弱化一名敵人能給予自身 5 扭曲 §#c267f7≈。(最高 15 層)

## Riftbound（佔位符對不上，未自動套用）

- 原文：`While Riftbound, your Dimensional Tear follows you,
reaches further and your Distortion {#} stops decaying.`
- 舊譯：處於 Riftbound 狀態時，你的 Dimensional Tear 會跟著你移動、
延伸得更遠，而且你的 Distortion {#} 不再衰減。
- 新譯：當你處於裂隙繫身狀態時，你的次元裂隙會跟著你，增加其範圍並停止你的 扭曲 §#c267f7≈ 層數降低。

## Judrajim（配不出對應的段落）

- 原文：`Mana cost and damage increase by
{~} every time it deals damage.`
- 舊譯：每造成一次傷害，它的魔力消耗
與傷害就提升 {~}.
- 新譯：當你的 魔力儲庫 ✺ 有超過 140 點魔力時， ⏎ 蹲下並施放祕法回流會召喚出一顆電球， ⏎ 每 0.25 秒對附近敵人造成一次傷害並消耗 2 點魔力。 ⏎ 電球的傷害和魔力消耗隨著每一次命中提高 20%。

## Diffraction（佔位符對不上，未自動套用）

- 原文：`Ophanim also applies +{~} Crystallized {#}.`
- 舊譯：Ophanim 也會施加 +{~} 層 Crystallized {#}.
- 新譯：當目標死亡時，其身上的 結晶化 §j💎 擴散至附近敵方身上。
奧法尼姆也會疊加 2 結晶化 §j💎。

## Diffraction（配不出對應的段落）

- 原文：`When an enemy dies, leftover Crystallized {#}
is transferred to other nearby enemies.`
- 舊譯：敵人死亡時，殘餘的 Crystallized {#}
會轉移到附近其他敵人身上。
- 新譯：當目標死亡時，其身上的 結晶化 §j💎 擴散至附近敵方身上。 ⏎ 奧法尼姆也會疊加 2 結晶化 §j💎。

## Time Vortex（佔位符對不上，未自動套用）

- 原文：`Casting Frozen Tornado inside your Dimensional
Tear infuses it with otherworldly energies,
turning it into a Time Vortex.`
- 舊譯：在你的 Dimensional Tear 內施放 Frozen Tornado,
會為它灌注異界能量，
使其轉變為 Time Vortex.
- 新譯：在次元裂隙中施放冰凍龍捲風會使其融入異界的力量，
將其轉變為時空渦流。
時空渦流造成傷害的速度提升 25%，持續更久，

## Time Vortex（佔位符對不上，未自動套用）

- 原文：`Time Vortex deals damage {~} faster, lasts
longer and grants +{~} Distortion {#} every
time it deals damage to an enemy.`
- 舊譯：Time Vortex 造成傷害的速度加快 {~1}、持續時間
更長，而且每次對敵人造成傷害時
都提供 +{~2} 層 Distortion {#} .
- 新譯：且每一次造成傷害時給予自身 1.5 扭曲 §#c267f7≈。

## Portal to the Beyond（佔位符對不上，未自動套用）

- 原文：`The Riftspawn shoots homing distorting
blasts to nearby enemies every {~}s,
dealing damage and increasing your
Distortion {#} by +{~}.`
- 舊譯：Riftspawn 每 {~1}s 會朝附近敵人
射出追蹤的扭曲彈，造成傷害
並使你的 Distortion {#} 增加 +{~2}.
- 新譯：裂隙之裔 每 0.5 秒會朝著附近的敵人射出追蹤扭曲炮，
造成傷害並給予玩家 1 扭曲 §#c267f7≈。

## Paradox（佔位符對不上，未自動套用）

- 原文：`For every +{~} Distortion {#}, the
mana is refunded -{~}s faster.`
- 舊譯：每 +{~1} 層 Distortion {#},
魔力返還的速度就加快 -{~2}s.
- 新譯：每一層 扭曲 §#c267f7≈ 會使魔力補回的速度加快 0.02 秒。

## Paradox（配不出對應的段落）

- 原文：`You send {~} of the damage you take
to the future, dealing it over {~}s and
borrow mana from the past, refunding
{~} of your spent mana over {~}s.`
- 舊譯：你會把所受傷害的 {~1} 送往未來，
在 {~2} 秒內分次承受;並向過去借取魔力，
在 {~4} 秒內返還你已消耗魔力的 {~3}.
- 新譯：將你所承受的 15% 傷害平分到未來的 8 秒中， ⏎ 借用你過去的魔力，你消耗的 25% 魔力將在未來的 10 秒內補回。 ⏎ 每一層 扭曲 §#c267f7≈ 會使魔力補回的速度加快 0.02 秒。

## Dawn（配不出對應的段落）

- 原文：`Activate abilities or heal to charge
your Ultimate Meter {#} to {~}`
- 舊譯：發動技能或進行治療，
將你的終極計量表 {#} 充能至 {~}
- 新譯：引導 Orphion 的力量把太陽召喚到你的上方。 ⏎ 太陽會召喚大量自玩家處向外擴散的陽焰， ⏎ 治癒接觸到的友軍，並對目標造成傷害。 ⏎ Activate abilities or heal to charge ⏎ your Ultimate Meter ⚡ to 100%.

## Dawn（配不出對應的段落）

- 原文：`Channel the power of Orphion and summon
the sun itself above you.`
- 舊譯：引導 Orphion 的力量，
在你的上空召來太陽本身。
- 新譯：引導 Orphion 的力量把太陽召喚到你的上方。 ⏎ 太陽會召喚大量自玩家處向外擴散的陽焰， ⏎ 治癒接觸到的友軍，並對目標造成傷害。 ⏎ Activate abilities or heal to charge ⏎ your Ultimate Meter ⚡ to 100%.

## Dawn（配不出對應的段落）

- 原文：`The Sun will summon solar wisps that move
outwards, damaging all enemies and healing
all allies in their path.`
- 舊譯：太陽會召出向外移動的日光靈體，
對路徑上的所有敵人造成傷害，
並治療路徑上的所有友軍。
- 新譯：引導 Orphion 的力量把太陽召喚到你的上方。 ⏎ 太陽會召喚大量自玩家處向外擴散的陽焰， ⏎ 治癒接觸到的友軍，並對目標造成傷害。 ⏎ Activate abilities or heal to charge ⏎ your Ultimate Meter ⚡ to 100%.

## Gravitational Collapse（佔位符對不上，未自動套用）

- 原文：`The Dimensional Tear collapses when it expires,
dealing massive damage to nearby enemies.`
- 舊譯：Dimensional Tear 到期時會塌縮，
對附近敵人造成巨量傷害。
- 新譯：你的次元裂隙發生超載，體積增大並將所有目標拉入其中。
超載狀態下，
次元裂隙的傷害增加 400%，
當次元裂隙消失時，將會坍縮並對附近的敵人造成大量傷害。
Activate abilities to charge
your Ultimate Meter ⚡ to 100%.

## Gravitational Collapse（佔位符對不上，未自動套用）

- 原文：`While it is overloaded, it deals +{~} increased
damage, turns your Teleport into a deadly ray
that fires off {~} times in quick succession,
provides flight, and prevents mana drain.`
- 舊譯：過載期間，它造成的傷害提升 +{~1},
並把你的 Teleport 變成一道致命光束、
短時間內連射 {~2} 次，
同時提供飛行並防止魔力流失。
- 新譯：傳送會取代為快速發射 5 次的致命雷射，
玩家將處於飛行狀態，
且魔力不會消耗。

## Gravitational Collapse（配不出對應的段落）

- 原文：`Your Dimensional Tear overloads, growing
in size and damage while pulling all enemies in.`
- 舊譯：你的 Dimensional Tear 會過載，
體積與傷害不斷增長，並將所有敵人拉入其中。
- 新譯：你的次元裂隙發生超載，體積增大並將所有目標拉入其中。 ⏎ 超載狀態下， ⏎ 次元裂隙的傷害增加 400%， ⏎ 傳送會取代為快速發射 5 次的致命雷射， ⏎ 玩家將處於飛行狀態， ⏎ 且魔力不會消耗。 ⏎ 當次元裂隙消失時，將會坍縮並對附近的敵人造成大量傷害。 ⏎ Activate abilities to charge ⏎ your Ultimate Meter ⚡ to 100%.

## Tangled Origin（配不出對應的段落）

- 原文：`Summon a Thunder Serpent and a Fire
Serpent, which will attack nearby enemies.`
- 舊譯：召喚一條雷之蛇與一條火之蛇，
它們會攻擊附近的敵人。
- 新譯：召喚 霆蛟 與 炎蛟 ，對附近的敵人造成傷害。 ⏎ 霆蛟 每 1 秒對至多 4 名敵人發出閃電，使命中目標 失衡 ⚡。 ⏎ 炎蛟 每 3 秒引爆 失衡 ⚡ 的目標並消耗該狀態。 ⏎ Activate abilities to charge ⏎ your Ultimate Meter ⚡ to 100%.

## Tangled Origin（配不出對應的段落）

- 原文：`The Fire Serpent fires at Unstable {#}
targets every {~}s, gaining the damage bonus.`
- 舊譯：火之蛇每 {~}s 會朝帶有 Unstable {#}
的目標開火，並獲得該傷害加成。
- 新譯：召喚 霆蛟 與 炎蛟 ，對附近的敵人造成傷害。 ⏎ 霆蛟 每 1 秒對至多 4 名敵人發出閃電，使命中目標 失衡 ⚡。 ⏎ 炎蛟 每 3 秒引爆 失衡 ⚡ 的目標並消耗該狀態。 ⏎ Activate abilities to charge ⏎ your Ultimate Meter ⚡ to 100%.

## Tangled Origin（配不出對應的段落）

- 原文：`The Thunder Serpent strikes up to
{~} enemies with arcing lightning
every {~}s, making them Unstable {#}.`
- 舊譯：雷之蛇每 {~2} 秒會以弧狀閃電
擊中最多 {~1} 個敵人，
使他們進入 Unstable {#}.
- 新譯：召喚 霆蛟 與 炎蛟 ，對附近的敵人造成傷害。 ⏎ 霆蛟 每 1 秒對至多 4 名敵人發出閃電，使命中目標 失衡 ⚡。 ⏎ 炎蛟 每 3 秒引爆 失衡 ⚡ 的目標並消耗該狀態。 ⏎ Activate abilities to charge ⏎ your Ultimate Meter ⚡ to 100%.

