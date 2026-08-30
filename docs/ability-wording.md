# 技能敘述：用詞與語料對不上的地方

由 `tools/` 之外的一次性腳本產出，**純報告，沒有動任何檔案**。

## 這是什麼

技能敘述裡帶顏色的專有名詞，顏色是拿**原文的字面**到譯文裡找的。
語料裡那個詞有譯法、而譯文用了**另一個說法**時，字面對不上，那一段就掉回底色。

篩選條件是「原文裡後面緊跟著 `{#}` 圖示」——那是資源／狀態名詞的固定寫法，
一定帶顏色。純粹的句子用詞差異不在這裡。

已經排除掉程式能自動接起來的情況（複數、所有格、語料查得到的中文，見 v1.99.85）。

## `Corruption`（1 處）

語料裡**沒有**這個詞。

- `warrior` WARRIOR-P5-R05-C02
  - 原文：While Corrupted {#}, hitting an ⏎ enemy with your Main Attack ⏎ will add +{~} Corruption {#}, ⏎ per second. ⏎ (Powder Specials always add +{~})
  - 譯文：處於 Corrupted {#} 狀態時， ⏎ 普通攻擊命中敵人會依攻擊速度以每秒 +{~}  ⏎ 的比例提升 Corrupted {#}。 ⏎ (Powder Specials 總是提升 +{~} )


---

# 附錄：一般用詞

不影響顏色，是**用詞一致性**的問題——同一個東西在不同職業叫不同名字，
讀起來會以為是兩種機制。

判準：語料裡這個英文詞有譯法、原文用了它、而譯文既沒保留英文也沒用那個譯法。

## `Area of Effect` → 語料作「技能範圍」（7 處）

- `mage` MAGE-P1-R00-C04
  - 原文：{#} Area of Effect: {~} Blocks (Circle-Shaped)
  - 譯文：{#} 作用範圍: {~} 格 (圓形)
- `mage` MAGE-P4-R02-C05
  - 原文：{#} Meteor Area of Effect: {~} Blocks (Circle-Shaped)
  - 譯文：{#} Meteor作用範圍: {~} 格 (圓形)
- …另外 5 處

## `Walk Speed` → 語料作「移動速度」（7 處）

- `mage` MAGE-P6-R04-C04
  - 原文：{#} Effect: +{~} Walk Speed to Allies (per {~}s, +{~} Max)
  - 譯文：{#} 效果: {~1} 友軍移速增加 (每 {~2} 秒，上限 {~3})
- `shaman` SHAMAN-P4-R01-C03
  - 原文：ÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀÀ -{~} Walk Speed to Self
  - 譯文：{~} 自身移速降低
- …另外 5 處

## `Range` → 語料作「施放範圍」（5 處）

- `mage` MAGE-P1-R00-C04
  - 原文：{#} Range: {~} Blocks
  - 譯文：{#} 範圍: {~} 格
- `mage` MAGE-P4-R03-C08
  - 原文：{#} Drift Range: {~} Blocks
  - 譯文：{#} 漂移範圍: {~} 格
- …另外 3 處

## `to Allies` → 語料作「給友軍」（5 處）

- `mage` MAGE-P5-R01-C02
  - 原文：{#} Effect: {~} Damage Bonus (⚔) to Allies
  - 譯文：{#} 效果: {~} 友軍傷害加成 (⚔)
- `mage` MAGE-P6-R04-C04
  - 原文：{#} Effect: +{~} Walk Speed to Allies (per {~}s, +{~} Max)
  - 譯文：{#} 效果: {~1} 友軍移速增加 (每 {~2} 秒，上限 {~3})
- …另外 3 處

## `to Enemies` → 語料作「給敵人」（5 處）

- `mage` MAGE-P6-R04-C02
  - 原文：{#} Effect: +{~} Slowness ({#}) to Enemies
  - 譯文：{#} 效果: 對敵人 +{~} 緩速({#})
- `mage` MAGE-P7-R01-C02
  - 原文：{#} Effect: -{~} Damage Bonus ({#}) to Enemies
  - 譯文：{#} 效果: {~} 敵人傷害降低 ({#})
- …另外 3 處

## `Total Damage` → 語料作「總傷害」（5 處）

- `mage` MAGE-P9-R02-C01
  - 原文：{#} Total Damage: {~} (of your DPS, per Wisp)
  - 譯文：{#} 靈體傷害: {~} (你的每秒傷害)
- `mage` MAGE-P9-R02-C07
  - 原文：{#} Total Damage: {~} (of your DPS, Fire)
  - 譯文：{#} 炎蛟傷害: {~} (你的每秒傷害，)
- …另外 3 處

## `second` → 語料作「秒」（4 處）

- `assassin` ASSASSIN-P1-R04-C04
  - 原文：Spin Attack will activate twice, with a / larger area of effect on the second spin.
  - 譯文：Spin Attack 會啟動兩次，第二次旋轉的作用範圍更大。
- `assassin` ASSASSIN-P8-R02-C03
  - 原文：Your second hit after casting / Vanish will deal +{~} damage.
  - 譯文：施放 Vanish 後的第二次命中會額外造成 +{~} 傷害。
- …另外 2 處

## `ability` → 語料作「技能」（2 處）

- `mage` MAGE-P1-R04-C04
  - 原文：Drastically increase the / speed of your Meteor ability.
  - 譯文：大幅增加隕石的速度。
- `warrior` WARRIOR-P4-R02-C01
  - 原文：While Corrupted {#}, you lose the ability / to heal, and every {~} health you lose will / grant you +{~} Corrupted {#}.
  - 譯文：處於 Corrupted {#} 狀態下，你將無法獲得治療， / 且每失去 {~1} 生命， / 就會 +{~2} Corrupted {#}。

## `Spell Damage` → 語料作「法術傷害」（2 處）

- `mage` MAGE-P2-R01-C06
  - 原文：For every {~} or {~} Raw Spell Damage you have / from items, gain +{~}/{~}s Mana Regen (Max {~}/{~}s)
  - 譯文：從道具中獲得的每 {~} 或 {~} 點技能傷害， / 額外獲得 {~}/{~}s 魔力回復。(最高 {~}/{~}s)
- `mage` MAGE-P5-R03-C08
  - 原文：For every {~}/{~}s Lifesteal you have / from items, gain +{~} Spell Damage. (Max {~})
  - 譯文：從道具中獲得的每 {~}/{~}s 生命偷取， / 額外獲得 {~} 技能傷害。(最高 {~})

## `Damage.` → 語料作「傷害。」（2 處）

- `mage` MAGE-P4-R03-C04
  - 原文：For every +{~} Distortion {#}, the tornado gains / +{~} {#} Area of Effect and +{~} {#} Damage.
  - 譯文：每有 {~1} 層 Distortion {#}，龍捲風的 / {#} 作用範圍 增加 {~2}，{#} 傷害 增加 {~3}。
- `mage` MAGE-P5-R03-C03
  - 原文：For every +{~} Distortion {#}, Vacuokinesis / deals +{~} {#} Damage.
  - 譯文：每有 {~1} 層 Distortion {#}，Vacuokinesis / 的 {#} 傷害 增加 {~2}。

## `Slowness (` → 語料作「緩速 (」（2 處）

- `mage` MAGE-P6-R04-C02
  - 原文：{#} Effect: +{~} Slowness ({#}) to Enemies
  - 譯文：{#} 效果: 對敵人 +{~} 緩速({#})
- `shaman` SHAMAN-P4-R04-C03
  - 原文：{#} Heretic: +{~} Slowness ({#}) to Enemies
  - 譯文：{#} 蒼逆: {~} 敵人移速降低 ({#})

## `Thunder` → 語料作「雷屬性」（2 處）

- `mage` MAGE-P9-R02-C07
  - 原文：{#} Total Damage: {~} (of your DPS, Thunder)
  - 譯文：{#} 霆蛟傷害: {~} (你的每秒傷害)
- `shaman` SHAMAN-P9-R03-C04
  - 原文：When your Totem lands, it summons / a massive Thundercloud that zaps enemies / every {~}s, stunning them for {~}s.
  - 譯文：當你的圖騰落地時，它將召喚巨大的閃電雲，每 {~} 秒對敵人造成傷害，並使其眩暈 {~} 秒。

