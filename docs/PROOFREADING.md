# 校稿清單

**待校稿 45 個檔、15,001 句。** 已校稿 0 個。

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
| Echoes of Change | 725 | [`echoes-of-change.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/echoes-of-change.json) |
| Apotheosis (Quest) | 709 | [`apotheosis-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/apotheosis-quest.json) |
| The Strong Survive | 660 | [`the-strong-survive.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-strong-survive.json) |
| Overture to Despair | 603 | [`overture-to-despair.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/overture-to-despair.json) |
| Aldorei's Secret Part II | 600 | [`aldorei-s-secret-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-ii.json) |
| The Price of Ingenuity | 584 | [`the-price-of-ingenuity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-price-of-ingenuity.json) |
| The Missing Piece | 582 | [`the-missing-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-missing-piece.json) |
| True Colours | 541 | [`true-colours.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/true-colours.json) |
| Celebrations in Smoke | 528 | [`celebrations-in-smoke.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/celebrations-in-smoke.json) |
| Solidarity of Steel | 508 | [`solidarity-of-steel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/solidarity-of-steel.json) |
| The Cursed One | 497 | [`the-cursed-one.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-cursed-one.json) |
| All Roads to Peace | 402 | [`all-roads-to-peace.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/all-roads-to-peace.json) |
| raid.json | 348 | [`raid.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/raid.json) |
| Revelations in Fall | 304 | [`revelations-in-fall.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/revelations-in-fall.json) |
| Shrouded in Mist | 299 | [`shrouded-in-mist.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shrouded-in-mist.json) |
| Hollow Serenity | 295 | [`hollow-serenity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hollow-serenity.json) |
| Fantastic Voyage | 283 | [`fantastic-voyage.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fantastic-voyage.json) |
| The Breaking Point | 281 | [`the-breaking-point.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-breaking-point.json) |
| A Hunter's Calling | 241 | [`a-hunter-s-calling.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-hunter-s-calling.json) |
| Cowfusion | 223 | [`cowfusion.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cowfusion.json) |
| The Canary Calls | 220 | [`the-canary-calls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canary-calls.json) |
| The Order of the Grook | 217 | [`the-order-of-the-grook.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-order-of-the-grook.json) |
| The Feathers Fly Part I | 210 | [`the-feathers-fly-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-feathers-fly-part-i.json) |
| The Hero of Gavel | 196 | [`the-hero-of-gavel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hero-of-gavel.json) |
| Through the Pipes | 191 | [`through-the-pipes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/through-the-pipes.json) |
| Beyond the Grave | 186 | [`beyond-the-grave.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beyond-the-grave.json) |
| A Headless History | 178 | [`a-headless-history.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-headless-history.json) |
| The Mercenary | 141 | [`the-mercenary.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-mercenary.json) |
| Recipe For Disaster | 139 | [`recipe-for-disaster.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recipe-for-disaster.json) |
| Temple of the Legends (Quest) | 137 | [`temple-of-the-legends-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/temple-of-the-legends-quest.json) |
| The Scarred Springs | 137 | [`the-scarred-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-scarred-springs.json) |
| Undersupply | 125 | [`undersupply.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/undersupply.json) |
| Dwarves and Doguns Part I | 122 | [`dwarves-and-doguns-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-i.json) |
| Forbidden Prison (Quest) | 117 | [`forbidden-prison-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/forbidden-prison-quest.json) |
| Memory Paranoia | 105 | [`memory-paranoia.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/memory-paranoia.json) |
| WynnExcavation Site D | 97 | [`wynnexcavation-site-d.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-d.json) |
| From the Bottom | 90 | [`from-the-bottom.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-bottom.json) |
| Rise of the Quartron | 86 | [`rise-of-the-quartron.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/rise-of-the-quartron.json) |
| The Worm Holes | 85 | [`the-worm-holes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-worm-holes.json) |
| Mixed Feelings | 76 | [`mixed-feelings.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mixed-feelings.json) |
| Bob's Lost Soul | 75 | [`bob-s-lost-soul.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/bob-s-lost-soul.json) |
| dungeon.json | 16 | [`dungeon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/dungeon.json) |

## 未標記譯者

沒有 `_meta.review` 的檔。**這裡不代表已經校過**——
只代表沒有人記錄過它是誰翻的。多半是團隊自己翻的、或早期沒有這個欄位。

<details><summary>展開（151 個檔）</summary>

| 內容 | 句數 | 檔案 |
|---|---:|---|
| gear-weapon.json | 2,769 | [`gear-weapon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gear-weapon.json) |
| gear-armour.json | 2,466 | [`gear-armour.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gear-armour.json) |
| npc.json | 1,877 | [`npc.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/npc.json) |
| misc.json | 1,876 | [`misc.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/misc.json) |
| gear-accessory.json | 1,144 | [`gear-accessory.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gear-accessory.json) |
| gui.json | 1,011 | [`gui.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gui.json) |
| ingredient.json | 969 | [`ingredient.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ingredient.json) |
| lootrun.json | 959 | [`lootrun.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/lootrun.json) |
| Queen's Recruit | 673 | [`queen-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/queen-s-recruit.json) |
| major-id.json | 322 | [`major-id.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/major-id.json) |
| label.json | 314 | [`label.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/label.json) |
| A New Beginning | 289 | [`a-new-beginning.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-new-beginning.json) |
| ui-labels.json | 288 | [`ui-labels.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ui-labels.json) |
| Recover the Past | 264 | [`recover-the-past.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recover-the-past.json) |
| ability-labels.json | 249 | [`ability-labels.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability-labels.json) |
| shaman.json | 243 | [`shaman.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/shaman.json) |
| A Journey Beyond | 243 | [`a-journey-beyond.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-beyond.json) |
| mage.json | 236 | [`mage.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/mage.json) |
| assassin.json | 234 | [`assassin.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/assassin.json) |
| warrior.json | 233 | [`warrior.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/warrior.json) |
| archer.json | 231 | [`archer.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/archer.json) |
| aspect-desc.json | 209 | [`aspect-desc.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/aspect-desc.json) |
| tome.json | 156 | [`tome.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/tome.json) |
| quest-name.json | 154 | [`quest-name.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest-name.json) |
| material.json | 136 | [`material.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/material.json) |
| Misadventure on the Sea | 132 | [`misadventure-on-the-sea.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/misadventure-on-the-sea.json) |
| aspect.json | 128 | [`aspect.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/aspect.json) |
| King's Recruit | 127 | [`king-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/king-s-recruit.json) |
| A Journey Home | 119 | [`a-journey-home.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-home.json) |
| guild.json | 118 | [`guild.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/guild.json) |
| discovery.json | 117 | [`discovery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/discovery.json) |
| General's Orders | 114 | [`general-s-orders.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/general-s-orders.json) |
| Hunger of the Gerts Part I | 112 | [`hunger-of-the-gerts-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-i.json) |
| Arachnids' Ascent | 111 | [`arachnids-ascent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/arachnids-ascent.json) |
| Meaningful Holiday | 108 | [`meaningful-holiday.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/meaningful-holiday.json) |
| One Thousand Meters Under | 105 | [`one-thousand-meters-under.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/one-thousand-meters-under.json) |
| Flight in Distress | 103 | [`flight-in-distress.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/flight-in-distress.json) |
| Taproot | 103 | [`taproot.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taproot.json) |
| Finding the Light | 99 | [`finding-the-light.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/finding-the-light.json) |
| quest-ui.json | 96 | [`quest-ui.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest-ui.json) |
| The Envoy Part I | 94 | [`the-envoy-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-i.json) |
| Brothers Return | 93 | [`brothers-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/brothers-return.json) |
| quest.json | 93 | [`quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest.json) |
| Acquiring Credentials | 92 | [`acquiring-credentials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/acquiring-credentials.json) |
| The Realm of Light (Quest) | 92 | [`the-realm-of-light-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-realm-of-light-quest.json) |
| ability-terms.json | 91 | [`ability-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability-terms.json) |
| Burning Bonds | 89 | [`burning-bonds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/burning-bonds.json) |
| Deja Vu | 87 | [`deja-vu.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/deja-vu.json) |
| The Shadow of the Beast | 87 | [`the-shadow-of-the-beast.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-shadow-of-the-beast.json) |
| A Journey Further | 85 | [`a-journey-further.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-further.json) |
| Royal Trials | 85 | [`royal-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/royal-trials.json) |
| Purple and Blue | 84 | [`purple-and-blue.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/purple-and-blue.json) |
| The Envoy Part II | 84 | [`the-envoy-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-ii.json) |
| Dwarves and Doguns Part II | 80 | [`dwarves-and-doguns-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-ii.json) |
| Supply and Delivery | 77 | [`supply-and-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/supply-and-delivery.json) |
| Hunger of the Gerts Part II | 76 | [`hunger-of-the-gerts-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-ii.json) |
| Lazarus Pit (Quest) | 76 | [`lazarus-pit-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lazarus-pit-quest.json) |
| The Ultimate Weapon | 76 | [`the-ultimate-weapon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-ultimate-weapon.json) |
| Dwarves and Doguns Part III | 75 | [`dwarves-and-doguns-part-iii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iii.json) |
| Mushroom Man | 75 | [`mushroom-man.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mushroom-man.json) |
| The Hidden City | 75 | [`the-hidden-city.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hidden-city.json) |
| shared.json | 74 | [`shared.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/shared.json) |
| Aldorei's Secret Part I | 71 | [`aldorei-s-secret-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-i.json) |
| Fate of the Fallen | 70 | [`fate-of-the-fallen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fate-of-the-fallen.json) |
| Frost Bite | 69 | [`frost-bite.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/frost-bite.json) |
| The Legend of Bob | 68 | [`the-legend-of-bob.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret/the-legend-of-bob.json) |
| secret-dialogue.json | 68 | [`secret-dialogue.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret-dialogue.json) |
| A Sandy Scandal | 66 | [`a-sandy-scandal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-sandy-scandal.json) |
| Infested Plants | 66 | [`infested-plants.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/infested-plants.json) |
| Dwarves and Doguns Part IV | 65 | [`dwarves-and-doguns-part-iv.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iv.json) |
| Elemental Exercise | 65 | [`elemental-exercise.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/elemental-exercise.json) |
| Fallen Delivery | 65 | [`fallen-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fallen-delivery.json) |
| From the Mountains | 63 | [`from-the-mountains.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-mountains.json) |
| The Canyon Guides | 63 | [`the-canyon-guides.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canyon-guides.json) |
| The Qira Hive (Quest) | 63 | [`the-qira-hive-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-qira-hive-quest.json) |
| Tunnel Trouble | 63 | [`tunnel-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tunnel-trouble.json) |
| An Iron Heart Part II | 61 | [`an-iron-heart-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-ii.json) |
| Lost Soles | 61 | [`lost-soles.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-soles.json) |
| Kingdom of Sand | 60 | [`kingdom-of-sand.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/kingdom-of-sand.json) |
| Out of my Mind | 60 | [`out-of-my-mind.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/out-of-my-mind.json) |
| Reclaiming the House | 59 | [`reclaiming-the-house.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reclaiming-the-house.json) |
| A Marauder's Dues | 58 | [`a-marauder-s-dues.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-marauder-s-dues.json) |
| Dearly Departed | 58 | [`dearly-departed.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dearly-departed.json) |
| Murder Mystery | 57 | [`murder-mystery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/murder-mystery.json) |
| Tempo Town Trouble | 57 | [`tempo-town-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tempo-town-trouble.json) |
| Underice | 56 | [`underice.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underice.json) |
| The Corrupted Village | 55 | [`the-corrupted-village.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-corrupted-village.json) |
| Haven Antiquity | 54 | [`haven-antiquity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/haven-antiquity.json) |
| Corrupted Betrayal | 51 | [`corrupted-betrayal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/corrupted-betrayal.json) |
| Shattered Minds | 51 | [`shattered-minds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shattered-minds.json) |
| Zhight Island (Quest) | 50 | [`zhight-island-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/zhight-island-quest.json) |
| The Dark Descent | 49 | [`the-dark-descent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-dark-descent.json) |
| The Lost | 48 | [`the-lost.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-lost.json) |
| Creeper Infiltration | 44 | [`creeper-infiltration.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/creeper-infiltration.json) |
| Crop Failure | 44 | [`crop-failure.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/crop-failure.json) |
| Taking the Tower | 44 | [`taking-the-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taking-the-tower.json) |
| Beneath the Depths | 42 | [`beneath-the-depths.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beneath-the-depths.json) |
| Blazing Retribution | 42 | [`blazing-retribution.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/blazing-retribution.json) |
| Canyon Condor | 42 | [`canyon-condor.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/canyon-condor.json) |
| Point of No Return | 42 | [`point-of-no-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/point-of-no-return.json) |
| Redbeard's Booty | 42 | [`redbeard-s-booty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/redbeard-s-booty.json) |
| The Thanos Depository | 42 | [`the-thanos-depository.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-thanos-depository.json) |
| WynnExcavation Site C | 42 | [`wynnexcavation-site-c.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-c.json) |
| Lava Springs | 41 | [`lava-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lava-springs.json) |
| Grand Youth | 39 | [`grand-youth.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/grand-youth.json) |
| Master Piece | 38 | [`master-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/master-piece.json) |
| Desperate Metal | 37 | [`desperate-metal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/desperate-metal.json) |
| The Maiden Tower | 36 | [`the-maiden-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-maiden-tower.json) |
| Enter the Dojo | 34 | [`enter-the-dojo.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enter-the-dojo.json) |
| Lexdale Witch Trials | 33 | [`lexdale-witch-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lexdale-witch-trials.json) |
| Reincarnation | 33 | [`reincarnation.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reincarnation.json) |
| Wrath of the Mummy | 33 | [`wrath-of-the-mummy.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wrath-of-the-mummy.json) |
| Green Gloop | 32 | [`green-gloop.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/green-gloop.json) |
| Jungle Fever | 32 | [`jungle-fever.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/jungle-fever.json) |
| Lost in the Jungle | 32 | [`lost-in-the-jungle.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-in-the-jungle.json) |
| Death Whistle (Quest) | 31 | [`death-whistle-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/death-whistle-quest.json) |
| Heart of Llevigar | 31 | [`heart-of-llevigar.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/heart-of-llevigar.json) |
| Potion Making | 31 | [`potion-making.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/potion-making.json) |
| The Sewers of Ragni | 31 | [`the-sewers-of-ragni.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-sewers-of-ragni.json) |
| wynntils.json | 31 | [`wynntils.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/wynntils.json) |
| WynnExcavation Site A | 28 | [`wynnexcavation-site-a.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-a.json) |
| WynnExcavation Site B | 28 | [`wynnexcavation-site-b.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-b.json) |
| Ice Nations | 26 | [`ice-nations.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/ice-nations.json) |
| Lost Royalty | 26 | [`lost-royalty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-royalty.json) |
| Troubled Tribesmen | 26 | [`troubled-tribesmen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/troubled-tribesmen.json) |
| Enzan's Brother | 25 | [`enzan-s-brother.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enzan-s-brother.json) |
| The Bigger Picture | 25 | [`the-bigger-picture.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-bigger-picture.json) |
| An Iron Heart Part I | 24 | [`an-iron-heart-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-i.json) |
| The Passage (Quest) | 24 | [`the-passage-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-passage-quest.json) |
| Tribal Aggression | 24 | [`tribal-aggression.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tribal-aggression.json) |
| Underwater | 24 | [`underwater.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underwater.json) |
| Maltic's Well | 23 | [`maltic-s-well.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/maltic-s-well.json) |
| Star Thief | 22 | [`star-thief.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/star-thief.json) |
| Cook Assistant | 20 | [`cook-assistant.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cook-assistant.json) |
| Stable Story | 19 | [`stable-story.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/stable-story.json) |
| Tower of Ascension (Quest) | 18 | [`tower-of-ascension-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tower-of-ascension-quest.json) |
| Dwelling Walls | 17 | [`dwelling-walls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwelling-walls.json) |
| A Grave Mistake | 16 | [`a-grave-mistake.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-grave-mistake.json) |
| Pirate's Trove | 16 | [`pirate-s-trove.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pirate-s-trove.json) |
| The House of Twain (Quest) | 15 | [`the-house-of-twain-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-house-of-twain-quest.json) |
| Clearing the Camps | 14 | [`clearing-the-camps.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/clearing-the-camps.json) |
| profession-terms.json | 12 | [`profession-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/profession-terms.json) |
| Pit of the Dead (Quest) | 12 | [`pit-of-the-dead-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pit-of-the-dead-quest.json) |
| Cluck Cluck | 10 | [`cluck-cluck.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cluck-cluck.json) |
| Lost Tower | 10 | [`lost-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-tower.json) |
| The Olmic Rune | 7 | [`the-olmic-rune.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-olmic-rune.json) |
| charm.json | 5 | [`charm.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/charm.json) |
| dialogue-choice.json | 3 | [`dialogue-choice.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/dialogue-choice.json) |
| major-id-terms.json | 3 | [`major-id-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/major-id-terms.json) |
| chat-terms.json | 2 | [`chat-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/chat-terms.json) |
| Skittering Spiders | 2 | [`skittering-spiders.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/skittering-spiders.json) |

</details>

---

## 散在共用檔裡的條目

`misc.json`、`gui.json` 這類共用檔裡也有 AI 加的條目，但它們跟團隊翻的
混在同一個檔，沒辦法用檔層級標記。那些逐批記在 [CHANGELOG](../CHANGELOG.md) 裡——
每一則都寫著加了什麼、為什麼那樣譯。要校那部分請從 CHANGELOG 往回看。

