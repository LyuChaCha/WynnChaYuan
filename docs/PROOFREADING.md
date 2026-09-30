# 校稿清單

**待校稿 44 個檔、16,035 句。** 已校稿 0 個。

> 這份清單是**產生物**，跑 `python tools/proofread.py` 重產。不要手改。

AI 翻得快，但快不等於對。語氣、雙關、角色口癖這些東西機器容易翻得
**通順但不對**——而通順反而更難被發現。所以每一份 AI 譯文都該由人再看一遍。

---

## 怎麼標記校稿完畢

打開那個檔，把 `_meta.review.proofread` 從 `false` 改成**你的名字與日期**：

```json
"review": {
  "translator": "claude",
  "proofread": "SCPNightsky 2026-08-30"
}
```

然後重跑 `python tools/proofread.py`（或直接讓 CI 跑）。

**改到一半也沒關係**——`proofread` 只要還是 `false`，它就會一直留在待辦上。
覺得某一條譯得不對就直接改，不需要問過我；這份清單的用意就是讓人有最後一票。

---

## 待校稿

| 內容 | 句數 | 檔案 |
|---|---:|---|
| The Feathers Fly Part II | 1,286 | [`the-feathers-fly-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-feathers-fly-part-ii.json) |
| Off the Rails | 791 | [`off-the-rails.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/off-the-rails.json) |
| Ensemble of Hope | 765 | [`ensemble-of-hope.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/ensemble-of-hope.json) |
| Echoes of Change | 740 | [`echoes-of-change.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/echoes-of-change.json) |
| Aldorei's Secret Part II | 729 | [`aldorei-s-secret-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-ii.json) |
| Apotheosis (Quest) | 708 | [`apotheosis-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/apotheosis-quest.json) |
| The Strong Survive | 660 | [`the-strong-survive.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-strong-survive.json) |
| Overture to Despair | 658 | [`overture-to-despair.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/overture-to-despair.json) |
| The Missing Piece | 615 | [`the-missing-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-missing-piece.json) |
| The Price of Ingenuity | 599 | [`the-price-of-ingenuity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-price-of-ingenuity.json) |
| True Colours | 570 | [`true-colours.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/true-colours.json) |
| Celebrations in Smoke | 560 | [`celebrations-in-smoke.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/celebrations-in-smoke.json) |
| All Roads to Peace | 539 | [`all-roads-to-peace.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/all-roads-to-peace.json) |
| Solidarity of Steel | 525 | [`solidarity-of-steel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/solidarity-of-steel.json) |
| The Cursed One | 503 | [`the-cursed-one.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-cursed-one.json) |
| Hollow Serenity | 493 | [`hollow-serenity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hollow-serenity.json) |
| The Canary Calls | 351 | [`the-canary-calls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canary-calls.json) |
| Shrouded in Mist | 314 | [`shrouded-in-mist.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shrouded-in-mist.json) |
| Revelations in Fall | 309 | [`revelations-in-fall.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/revelations-in-fall.json) |
| The Breaking Point | 304 | [`the-breaking-point.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-breaking-point.json) |
| Fantastic Voyage | 283 | [`fantastic-voyage.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fantastic-voyage.json) |
| A Hunter's Calling | 271 | [`a-hunter-s-calling.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-hunter-s-calling.json) |
| The Order of the Grook | 260 | [`the-order-of-the-grook.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-order-of-the-grook.json) |
| Cowfusion | 224 | [`cowfusion.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cowfusion.json) |
| Through the Pipes | 223 | [`through-the-pipes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/through-the-pipes.json) |
| Forbidden Prison (Quest) | 215 | [`forbidden-prison-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/forbidden-prison-quest.json) |
| A Headless History | 210 | [`a-headless-history.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-headless-history.json) |
| The Feathers Fly Part I | 210 | [`the-feathers-fly-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-feathers-fly-part-i.json) |
| The Hero of Gavel | 196 | [`the-hero-of-gavel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hero-of-gavel.json) |
| Beyond the Grave | 192 | [`beyond-the-grave.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beyond-the-grave.json) |
| Temple of the Legends (Quest) | 180 | [`temple-of-the-legends-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/temple-of-the-legends-quest.json) |
| The Mercenary | 155 | [`the-mercenary.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-mercenary.json) |
| Dwarves and Doguns Part I | 146 | [`dwarves-and-doguns-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-i.json) |
| Memory Paranoia | 140 | [`memory-paranoia.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/memory-paranoia.json) |
| Recipe For Disaster | 138 | [`recipe-for-disaster.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recipe-for-disaster.json) |
| WynnExcavation Site D | 138 | [`wynnexcavation-site-d.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-d.json) |
| The Scarred Springs | 137 | [`the-scarred-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-scarred-springs.json) |
| Undersupply | 125 | [`undersupply.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/undersupply.json) |
| Rise of the Quartron | 117 | [`rise-of-the-quartron.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/rise-of-the-quartron.json) |
| dungeon.json | 105 | [`dungeon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/dungeon.json) |
| The Worm Holes | 97 | [`the-worm-holes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-worm-holes.json) |
| From the Bottom | 91 | [`from-the-bottom.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-bottom.json) |
| Bob's Lost Soul | 87 | [`bob-s-lost-soul.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/bob-s-lost-soul.json) |
| Mixed Feelings | 76 | [`mixed-feelings.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mixed-feelings.json) |

## 還在翻，先別校

翻完之前校稿會白做工——這裡列出來只是讓你知道它們存在。

| 內容 | 進度 | 檔案 |
|---|---:|---|
| raid.json | 422 / 430 | [`raid.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/raid.json) |

## 未標記譯者

沒有 `_meta.review` 的檔。**這裡不代表已經校過**——
只代表沒有人記錄過它是誰翻的。多半是團隊自己翻的、或早期沒有這個欄位。

<details><summary>展開（159 個檔）</summary>

| 內容 | 句數 | 檔案 |
|---|---:|---|
| misc.json | 11,773 | [`misc.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/misc.json) |
| npc.json | 6,293 | [`npc.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/npc.json) |
| gear-weapon.json | 2,772 | [`gear-weapon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gear-weapon.json) |
| gear-armour.json | 2,466 | [`gear-armour.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gear-armour.json) |
| gear-accessory.json | 1,144 | [`gear-accessory.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gear-accessory.json) |
| gui.json | 1,111 | [`gui.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gui.json) |
| lootrun.json | 1,084 | [`lootrun.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/lootrun.json) |
| ingredient.json | 969 | [`ingredient.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ingredient.json) |
| quest.json | 761 | [`quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest.json) |
| discovery.json | 721 | [`discovery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/discovery.json) |
| Queen's Recruit | 695 | [`queen-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/queen-s-recruit.json) |
| quest-ui.json | 395 | [`quest-ui.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest-ui.json) |
| shaman.json | 363 | [`shaman.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/shaman.json) |
| A New Beginning | 359 | [`a-new-beginning.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-new-beginning.json) |
| Recover the Past | 348 | [`recover-the-past.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recover-the-past.json) |
| major-id.json | 331 | [`major-id.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/major-id.json) |
| label.json | 321 | [`label.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/label.json) |
| mage.json | 320 | [`mage.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/mage.json) |
| discovery-name.json | 295 | [`discovery-name.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/discovery-name.json) |
| ui-labels.json | 293 | [`ui-labels.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ui-labels.json) |
| warrior.json | 278 | [`warrior.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/warrior.json) |
| archer.json | 270 | [`archer.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/archer.json) |
| assassin.json | 268 | [`assassin.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/assassin.json) |
| ability-labels.json | 249 | [`ability-labels.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability-labels.json) |
| A Journey Beyond | 248 | [`a-journey-beyond.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-beyond.json) |
| mini-quest.json | 216 | [`mini-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/mini-quest.json) |
| aspect-desc.json | 209 | [`aspect-desc.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/aspect-desc.json) |
| cave.json | 209 | [`cave.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/cave.json) |
| Misadventure on the Sea | 194 | [`misadventure-on-the-sea.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/misadventure-on-the-sea.json) |
| Finding the Light | 162 | [`finding-the-light.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/finding-the-light.json) |
| tome.json | 156 | [`tome.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/tome.json) |
| quest-name.json | 154 | [`quest-name.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest-name.json) |
| guild.json | 152 | [`guild.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/guild.json) |
| General's Orders | 144 | [`general-s-orders.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/general-s-orders.json) |
| material.json | 136 | [`material.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/material.json) |
| A Journey Home | 133 | [`a-journey-home.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-home.json) |
| King's Recruit | 131 | [`king-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/king-s-recruit.json) |
| Arachnids' Ascent | 130 | [`arachnids-ascent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/arachnids-ascent.json) |
| aspect.json | 128 | [`aspect.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/aspect.json) |
| Meaningful Holiday | 124 | [`meaningful-holiday.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/meaningful-holiday.json) |
| The Envoy Part I | 121 | [`the-envoy-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-i.json) |
| The Envoy Part II | 117 | [`the-envoy-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-ii.json) |
| Taproot | 116 | [`taproot.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taproot.json) |
| The Realm of Light (Quest) | 116 | [`the-realm-of-light-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-realm-of-light-quest.json) |
| Hunger of the Gerts Part I | 113 | [`hunger-of-the-gerts-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-i.json) |
| Brothers Return | 109 | [`brothers-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/brothers-return.json) |
| One Thousand Meters Under | 105 | [`one-thousand-meters-under.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/one-thousand-meters-under.json) |
| Burning Bonds | 104 | [`burning-bonds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/burning-bonds.json) |
| Flight in Distress | 103 | [`flight-in-distress.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/flight-in-distress.json) |
| ability-terms.json | 100 | [`ability-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability-terms.json) |
| A Sandy Scandal | 100 | [`a-sandy-scandal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-sandy-scandal.json) |
| Lazarus Pit (Quest) | 98 | [`lazarus-pit-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lazarus-pit-quest.json) |
| The Shadow of the Beast | 98 | [`the-shadow-of-the-beast.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-shadow-of-the-beast.json) |
| Fate of the Fallen | 93 | [`fate-of-the-fallen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fate-of-the-fallen.json) |
| Acquiring Credentials | 92 | [`acquiring-credentials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/acquiring-credentials.json) |
| Frost Bite | 92 | [`frost-bite.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/frost-bite.json) |
| Mushroom Man | 91 | [`mushroom-man.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mushroom-man.json) |
| Deja Vu | 90 | [`deja-vu.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/deja-vu.json) |
| A Journey Further | 89 | [`a-journey-further.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-further.json) |
| Dwarves and Doguns Part II | 87 | [`dwarves-and-doguns-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-ii.json) |
| Supply and Delivery | 87 | [`supply-and-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/supply-and-delivery.json) |
| The Canyon Guides | 87 | [`the-canyon-guides.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canyon-guides.json) |
| Aldorei's Secret Part I | 86 | [`aldorei-s-secret-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-i.json) |
| Royal Trials | 85 | [`royal-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/royal-trials.json) |
| From the Mountains | 84 | [`from-the-mountains.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-mountains.json) |
| Purple and Blue | 84 | [`purple-and-blue.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/purple-and-blue.json) |
| Elemental Exercise | 83 | [`elemental-exercise.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/elemental-exercise.json) |
| The Dark Descent | 81 | [`the-dark-descent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-dark-descent.json) |
| Dwarves and Doguns Part III | 80 | [`dwarves-and-doguns-part-iii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iii.json) |
| Murder Mystery | 80 | [`murder-mystery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/murder-mystery.json) |
| The Ultimate Weapon | 77 | [`the-ultimate-weapon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-ultimate-weapon.json) |
| Hunger of the Gerts Part II | 76 | [`hunger-of-the-gerts-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-ii.json) |
| Kingdom of Sand | 75 | [`kingdom-of-sand.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/kingdom-of-sand.json) |
| Lost Soles | 75 | [`lost-soles.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-soles.json) |
| The Hidden City | 75 | [`the-hidden-city.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hidden-city.json) |
| shared.json | 74 | [`shared.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/shared.json) |
| A Marauder's Dues | 74 | [`a-marauder-s-dues.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-marauder-s-dues.json) |
| Out of my Mind | 74 | [`out-of-my-mind.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/out-of-my-mind.json) |
| Corrupted Betrayal | 73 | [`corrupted-betrayal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/corrupted-betrayal.json) |
| The Corrupted Village | 71 | [`the-corrupted-village.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-corrupted-village.json) |
| Tunnel Trouble | 71 | [`tunnel-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tunnel-trouble.json) |
| Reclaiming the House | 70 | [`reclaiming-the-house.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reclaiming-the-house.json) |
| The Legend of Bob | 68 | [`the-legend-of-bob.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret/the-legend-of-bob.json) |
| secret-dialogue.json | 68 | [`secret-dialogue.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret-dialogue.json) |
| An Iron Heart Part II | 67 | [`an-iron-heart-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-ii.json) |
| Crop Failure | 67 | [`crop-failure.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/crop-failure.json) |
| Dwarves and Doguns Part IV | 67 | [`dwarves-and-doguns-part-iv.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iv.json) |
| Shattered Minds | 66 | [`shattered-minds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shattered-minds.json) |
| Fallen Delivery | 65 | [`fallen-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fallen-delivery.json) |
| Dearly Departed | 64 | [`dearly-departed.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dearly-departed.json) |
| Underice | 64 | [`underice.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underice.json) |
| Point of No Return | 63 | [`point-of-no-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/point-of-no-return.json) |
| Tempo Town Trouble | 63 | [`tempo-town-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tempo-town-trouble.json) |
| The Qira Hive (Quest) | 63 | [`the-qira-hive-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-qira-hive-quest.json) |
| Haven Antiquity | 62 | [`haven-antiquity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/haven-antiquity.json) |
| Infested Plants | 62 | [`infested-plants.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/infested-plants.json) |
| WynnExcavation Site C | 62 | [`wynnexcavation-site-c.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-c.json) |
| The Thanos Depository | 60 | [`the-thanos-depository.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-thanos-depository.json) |
| Master Piece | 59 | [`master-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/master-piece.json) |
| Taking the Tower | 59 | [`taking-the-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taking-the-tower.json) |
| The Lost | 59 | [`the-lost.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-lost.json) |
| The Sewers of Ragni | 54 | [`the-sewers-of-ragni.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-sewers-of-ragni.json) |
| Creeper Infiltration | 53 | [`creeper-infiltration.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/creeper-infiltration.json) |
| Zhight Island (Quest) | 52 | [`zhight-island-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/zhight-island-quest.json) |
| Lava Springs | 50 | [`lava-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lava-springs.json) |
| Beneath the Depths | 49 | [`beneath-the-depths.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beneath-the-depths.json) |
| Lexdale Witch Trials | 48 | [`lexdale-witch-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lexdale-witch-trials.json) |
| Canyon Condor | 47 | [`canyon-condor.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/canyon-condor.json) |
| Redbeard's Booty | 47 | [`redbeard-s-booty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/redbeard-s-booty.json) |
| Blazing Retribution | 46 | [`blazing-retribution.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/blazing-retribution.json) |
| The Maiden Tower | 43 | [`the-maiden-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-maiden-tower.json) |
| Troubled Tribesmen | 43 | [`troubled-tribesmen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/troubled-tribesmen.json) |
| Grand Youth | 42 | [`grand-youth.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/grand-youth.json) |
| Heart of Llevigar | 41 | [`heart-of-llevigar.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/heart-of-llevigar.json) |
| Death Whistle (Quest) | 40 | [`death-whistle-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/death-whistle-quest.json) |
| Jungle Fever | 40 | [`jungle-fever.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/jungle-fever.json) |
| Wrath of the Mummy | 40 | [`wrath-of-the-mummy.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wrath-of-the-mummy.json) |
| Cook Assistant | 38 | [`cook-assistant.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cook-assistant.json) |
| Stable Story | 38 | [`stable-story.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/stable-story.json) |
| wynntils.json | 38 | [`wynntils.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/wynntils.json) |
| Desperate Metal | 37 | [`desperate-metal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/desperate-metal.json) |
| Enter the Dojo | 36 | [`enter-the-dojo.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enter-the-dojo.json) |
| Potion Making | 36 | [`potion-making.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/potion-making.json) |
| Green Gloop | 35 | [`green-gloop.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/green-gloop.json) |
| unsorted.json | 35 | [`unsorted.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/unsorted.json) |
| Lost in the Jungle | 34 | [`lost-in-the-jungle.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-in-the-jungle.json) |
| Reincarnation | 33 | [`reincarnation.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reincarnation.json) |
| WynnExcavation Site A | 33 | [`wynnexcavation-site-a.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-a.json) |
| Lost Royalty | 32 | [`lost-royalty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-royalty.json) |
| Enzan's Brother | 31 | [`enzan-s-brother.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enzan-s-brother.json) |
| An Iron Heart Part I | 30 | [`an-iron-heart-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-i.json) |
| Maltic's Well | 29 | [`maltic-s-well.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/maltic-s-well.json) |
| The Passage (Quest) | 29 | [`the-passage-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-passage-quest.json) |
| Tribal Aggression | 29 | [`tribal-aggression.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tribal-aggression.json) |
| WynnExcavation Site B | 29 | [`wynnexcavation-site-b.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-b.json) |
| Ice Nations | 28 | [`ice-nations.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/ice-nations.json) |
| Pirate's Trove | 27 | [`pirate-s-trove.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pirate-s-trove.json) |
| Underwater | 26 | [`underwater.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underwater.json) |
| The Bigger Picture | 25 | [`the-bigger-picture.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-bigger-picture.json) |
| Star Thief | 24 | [`star-thief.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/star-thief.json) |
| Clearing the Camps | 23 | [`clearing-the-camps.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/clearing-the-camps.json) |
| A Grave Mistake | 21 | [`a-grave-mistake.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-grave-mistake.json) |
| Tower of Ascension (Quest) | 21 | [`tower-of-ascension-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tower-of-ascension-quest.json) |
| dialogue-choice.json | 20 | [`dialogue-choice.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/dialogue-choice.json) |
| Dwelling Walls | 18 | [`dwelling-walls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwelling-walls.json) |
| The House of Twain (Quest) | 16 | [`the-house-of-twain-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-house-of-twain-quest.json) |
| The Olmic Rune | 15 | [`the-olmic-rune.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-olmic-rune.json) |
| Pit of the Dead (Quest) | 14 | [`pit-of-the-dead-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pit-of-the-dead-quest.json) |
| Cluck Cluck | 13 | [`cluck-cluck.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cluck-cluck.json) |
| profession-terms.json | 12 | [`profession-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/profession-terms.json) |
| Lost Tower | 12 | [`lost-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-tower.json) |
| Skittering Spiders | 8 | [`skittering-spiders.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/skittering-spiders.json) |
| label.json | 7 | [`label.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/scoped/label.json) |
| charm.json | 5 | [`charm.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/charm.json) |
| major-id-terms.json | 3 | [`major-id-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/major-id-terms.json) |
| bossbar.json | 3 | [`bossbar.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/scoped/bossbar.json) |
| chat-terms.json | 2 | [`chat-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/chat-terms.json) |
| Rogue Wyrmling | 1 | [`rogue-wyrmling.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/rogue-wyrmling.json) |
| name.json | 1 | [`name.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/scoped/name.json) |

</details>

---

## 散在共用檔裡的條目

`misc.json`、`gui.json` 這類共用檔裡也有 AI 加的條目，但它們跟團隊翻的
混在同一個檔，沒辦法用檔層級標記。那些逐批記在 [CHANGELOG](../CHANGELOG.md) 裡——
每一則都寫著加了什麼、為什麼那樣譯。要校那部分請從 CHANGELOG 往回看。

