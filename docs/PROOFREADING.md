# 校稿清單

**待校稿 42 個檔、14,674 句。** 已校稿 0 個。

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
| Off the Rails | 752 | [`off-the-rails.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/off-the-rails.json) |
| Echoes of Change | 734 | [`echoes-of-change.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/echoes-of-change.json) |
| Ensemble of Hope | 726 | [`ensemble-of-hope.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/ensemble-of-hope.json) |
| The Strong Survive | 643 | [`the-strong-survive.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-strong-survive.json) |
| Aldorei's Secret Part II | 633 | [`aldorei-s-secret-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-ii.json) |
| Apotheosis (Quest) | 616 | [`apotheosis-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/apotheosis-quest.json) |
| Overture to Despair | 616 | [`overture-to-despair.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/overture-to-despair.json) |
| The Price of Ingenuity | 589 | [`the-price-of-ingenuity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-price-of-ingenuity.json) |
| The Feathers Fly Part II | 577 | [`the-feathers-fly-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-feathers-fly-part-ii.json) |
| The Missing Piece | 556 | [`the-missing-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-missing-piece.json) |
| Celebrations in Smoke | 540 | [`celebrations-in-smoke.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/celebrations-in-smoke.json) |
| True Colours | 522 | [`true-colours.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/true-colours.json) |
| The Cursed One | 521 | [`the-cursed-one.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-cursed-one.json) |
| Solidarity of Steel | 517 | [`solidarity-of-steel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/solidarity-of-steel.json) |
| All Roads to Peace | 505 | [`all-roads-to-peace.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/all-roads-to-peace.json) |
| Hollow Serenity | 445 | [`hollow-serenity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hollow-serenity.json) |
| The Canary Calls | 332 | [`the-canary-calls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canary-calls.json) |
| The Breaking Point | 318 | [`the-breaking-point.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-breaking-point.json) |
| Shrouded in Mist | 309 | [`shrouded-in-mist.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shrouded-in-mist.json) |
| Fantastic Voyage | 306 | [`fantastic-voyage.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fantastic-voyage.json) |
| Revelations in Fall | 306 | [`revelations-in-fall.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/revelations-in-fall.json) |
| A Hunter's Calling | 304 | [`a-hunter-s-calling.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-hunter-s-calling.json) |
| The Order of the Grook | 245 | [`the-order-of-the-grook.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-order-of-the-grook.json) |
| The Hero of Gavel | 242 | [`the-hero-of-gavel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hero-of-gavel.json) |
| The Feathers Fly Part I | 237 | [`the-feathers-fly-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-feathers-fly-part-i.json) |
| Through the Pipes | 210 | [`through-the-pipes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/through-the-pipes.json) |
| A Headless History | 206 | [`a-headless-history.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-headless-history.json) |
| Forbidden Prison (Quest) | 203 | [`forbidden-prison-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/forbidden-prison-quest.json) |
| dungeon.json | 195 | [`dungeon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/dungeon.json) |
| Beyond the Grave | 185 | [`beyond-the-grave.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beyond-the-grave.json) |
| Cowfusion | 183 | [`cowfusion.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cowfusion.json) |
| Recipe For Disaster | 181 | [`recipe-for-disaster.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recipe-for-disaster.json) |
| Dwarves and Doguns Part I | 180 | [`dwarves-and-doguns-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-i.json) |
| WynnExcavation Site D | 143 | [`wynnexcavation-site-d.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-d.json) |
| The Scarred Springs | 136 | [`the-scarred-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-scarred-springs.json) |
| The Mercenary | 134 | [`the-mercenary.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-mercenary.json) |
| Memory Paranoia | 127 | [`memory-paranoia.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/memory-paranoia.json) |
| Rise of the Quartron | 118 | [`rise-of-the-quartron.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/rise-of-the-quartron.json) |
| Undersupply | 110 | [`undersupply.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/undersupply.json) |
| Mixed Feelings | 97 | [`mixed-feelings.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mixed-feelings.json) |
| From the Bottom | 92 | [`from-the-bottom.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-bottom.json) |
| Bob's Lost Soul | 83 | [`bob-s-lost-soul.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/bob-s-lost-soul.json) |

## 還在翻，先別校

翻完之前校稿會白做工——這裡列出來只是讓你知道它們存在。

| 內容 | 進度 | 檔案 |
|---|---:|---|
| Temple of the Legends (Quest) | 144 / 145 | [`temple-of-the-legends-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/temple-of-the-legends-quest.json) |
| The Worm Holes | 95 / 96 | [`the-worm-holes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-worm-holes.json) |
| raid.json | 625 / 633 | [`raid.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/raid.json) |

## 未標記譯者

沒有 `_meta.review` 的檔。**這裡不代表已經校過**——
只代表沒有人記錄過它是誰翻的。多半是團隊自己翻的、或早期沒有這個欄位。

<details><summary>展開（164 個檔）</summary>

| 內容 | 句數 | 檔案 |
|---|---:|---|
| misc.json | 15,343 | [`misc.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/misc.json) |
| npc.json | 9,446 | [`npc.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/npc.json) |
| gear-weapon.json | 2,805 | [`gear-weapon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gear-weapon.json) |
| gear-armour.json | 2,483 | [`gear-armour.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gear-armour.json) |
| lootrun.json | 1,627 | [`lootrun.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/lootrun.json) |
| quest.json | 1,517 | [`quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest.json) |
| gui.json | 1,226 | [`gui.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gui.json) |
| gear-accessory.json | 1,149 | [`gear-accessory.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/gear-accessory.json) |
| ingredient.json | 975 | [`ingredient.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ingredient.json) |
| Queen's Recruit | 712 | [`queen-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/queen-s-recruit.json) |
| label.json | 699 | [`label.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/label.json) |
| discovery.json | 529 | [`discovery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/discovery.json) |
| A New Beginning | 371 | [`a-new-beginning.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-new-beginning.json) |
| major-id.json | 331 | [`major-id.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/major-id.json) |
| Recover the Past | 308 | [`recover-the-past.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recover-the-past.json) |
| discovery-name.json | 301 | [`discovery-name.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/discovery-name.json) |
| shaman.json | 295 | [`shaman.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/shaman.json) |
| ui-labels.json | 293 | [`ui-labels.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ui-labels.json) |
| ability-labels.json | 249 | [`ability-labels.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability-labels.json) |
| A Journey Beyond | 249 | [`a-journey-beyond.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-beyond.json) |
| mage.json | 245 | [`mage.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/mage.json) |
| quest-ui.json | 240 | [`quest-ui.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest-ui.json) |
| warrior.json | 236 | [`warrior.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/warrior.json) |
| assassin.json | 235 | [`assassin.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/assassin.json) |
| archer.json | 234 | [`archer.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/archer.json) |
| aspect-desc.json | 220 | [`aspect-desc.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/aspect-desc.json) |
| cave.json | 203 | [`cave.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/cave.json) |
| Misadventure on the Sea | 196 | [`misadventure-on-the-sea.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/misadventure-on-the-sea.json) |
| secret-dialogue.json | 190 | [`secret-dialogue.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret-dialogue.json) |
| King's Recruit | 167 | [`king-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/king-s-recruit.json) |
| Finding the Light | 161 | [`finding-the-light.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/finding-the-light.json) |
| tome.json | 156 | [`tome.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/tome.json) |
| quest-name.json | 155 | [`quest-name.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest-name.json) |
| guild.json | 154 | [`guild.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/guild.json) |
| A Journey Home | 154 | [`a-journey-home.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-home.json) |
| General's Orders | 139 | [`general-s-orders.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/general-s-orders.json) |
| material.json | 136 | [`material.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/material.json) |
| aspect.json | 131 | [`aspect.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/aspect.json) |
| wynnventory.json | 131 | [`wynnventory.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/scoped/wynnventory.json) |
| Hunger of the Gerts Part I | 130 | [`hunger-of-the-gerts-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-i.json) |
| Enter the Dojo | 126 | [`enter-the-dojo.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enter-the-dojo.json) |
| One Thousand Meters Under | 125 | [`one-thousand-meters-under.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/one-thousand-meters-under.json) |
| Acquiring Credentials | 123 | [`acquiring-credentials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/acquiring-credentials.json) |
| Meaningful Holiday | 120 | [`meaningful-holiday.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/meaningful-holiday.json) |
| The Realm of Light (Quest) | 116 | [`the-realm-of-light-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-realm-of-light-quest.json) |
| Taproot | 114 | [`taproot.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taproot.json) |
| Arachnids' Ascent | 113 | [`arachnids-ascent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/arachnids-ascent.json) |
| Flight in Distress | 110 | [`flight-in-distress.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/flight-in-distress.json) |
| The Envoy Part II | 105 | [`the-envoy-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-ii.json) |
| The Envoy Part I | 104 | [`the-envoy-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-i.json) |
| Brothers Return | 103 | [`brothers-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/brothers-return.json) |
| Burning Bonds | 103 | [`burning-bonds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/burning-bonds.json) |
| Purple and Blue | 103 | [`purple-and-blue.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/purple-and-blue.json) |
| ability-terms.json | 101 | [`ability-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability-terms.json) |
| Royal Trials | 100 | [`royal-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/royal-trials.json) |
| A Journey Further | 97 | [`a-journey-further.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-further.json) |
| Aldorei's Secret Part I | 93 | [`aldorei-s-secret-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-i.json) |
| Lazarus Pit (Quest) | 93 | [`lazarus-pit-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lazarus-pit-quest.json) |
| The Hidden City | 93 | [`the-hidden-city.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hidden-city.json) |
| A Sandy Scandal | 92 | [`a-sandy-scandal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-sandy-scandal.json) |
| Dwarves and Doguns Part II | 91 | [`dwarves-and-doguns-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-ii.json) |
| Hunger of the Gerts Part II | 91 | [`hunger-of-the-gerts-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-ii.json) |
| The Shadow of the Beast | 90 | [`the-shadow-of-the-beast.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-shadow-of-the-beast.json) |
| dialogue-choice.json | 89 | [`dialogue-choice.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/dialogue-choice.json) |
| Deja Vu | 89 | [`deja-vu.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/deja-vu.json) |
| Dwarves and Doguns Part III | 87 | [`dwarves-and-doguns-part-iii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iii.json) |
| Mushroom Man | 87 | [`mushroom-man.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mushroom-man.json) |
| The Qira Hive (Quest) | 87 | [`the-qira-hive-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-qira-hive-quest.json) |
| Fate of the Fallen | 84 | [`fate-of-the-fallen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fate-of-the-fallen.json) |
| The Ultimate Weapon | 84 | [`the-ultimate-weapon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-ultimate-weapon.json) |
| Elemental Exercise | 81 | [`elemental-exercise.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/elemental-exercise.json) |
| Murder Mystery | 77 | [`murder-mystery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/murder-mystery.json) |
| shared.json | 74 | [`shared.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/ability/shared.json) |
| Frost Bite | 74 | [`frost-bite.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/frost-bite.json) |
| The Canyon Guides | 74 | [`the-canyon-guides.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canyon-guides.json) |
| An Iron Heart Part II | 72 | [`an-iron-heart-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-ii.json) |
| Corrupted Betrayal | 72 | [`corrupted-betrayal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/corrupted-betrayal.json) |
| Kingdom of Sand | 72 | [`kingdom-of-sand.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/kingdom-of-sand.json) |
| The Corrupted Village | 71 | [`the-corrupted-village.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-corrupted-village.json) |
| A Marauder's Dues | 70 | [`a-marauder-s-dues.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-marauder-s-dues.json) |
| Dwarves and Doguns Part IV | 70 | [`dwarves-and-doguns-part-iv.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iv.json) |
| Fallen Delivery | 70 | [`fallen-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fallen-delivery.json) |
| Lost Soles | 70 | [`lost-soles.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-soles.json) |
| Tunnel Trouble | 68 | [`tunnel-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tunnel-trouble.json) |
| A Croaking Cryptid | 68 | [`a-croaking-cryptid.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret/a-croaking-cryptid.json) |
| The Legend of Bob | 68 | [`the-legend-of-bob.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret/the-legend-of-bob.json) |
| mini-quest.json | 67 | [`mini-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/mini-quest.json) |
| From the Mountains | 67 | [`from-the-mountains.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-mountains.json) |
| Reclaiming the House | 67 | [`reclaiming-the-house.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reclaiming-the-house.json) |
| Shattered Minds | 66 | [`shattered-minds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shattered-minds.json) |
| Supply and Delivery | 63 | [`supply-and-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/supply-and-delivery.json) |
| Underice | 63 | [`underice.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underice.json) |
| The Thanos Depository | 62 | [`the-thanos-depository.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-thanos-depository.json) |
| Infested Plants | 61 | [`infested-plants.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/infested-plants.json) |
| Out of my Mind | 61 | [`out-of-my-mind.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/out-of-my-mind.json) |
| WynnExcavation Site C | 61 | [`wynnexcavation-site-c.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-c.json) |
| Haven Antiquity | 59 | [`haven-antiquity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/haven-antiquity.json) |
| The Dark Descent | 59 | [`the-dark-descent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-dark-descent.json) |
| Master Piece | 58 | [`master-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/master-piece.json) |
| Tempo Town Trouble | 58 | [`tempo-town-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tempo-town-trouble.json) |
| Dearly Departed | 57 | [`dearly-departed.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dearly-departed.json) |
| The Lost | 57 | [`the-lost.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-lost.json) |
| gear.json | 57 | [`gear.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/scoped/gear.json) |
| Point of No Return | 56 | [`point-of-no-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/point-of-no-return.json) |
| Crop Failure | 55 | [`crop-failure.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/crop-failure.json) |
| Taking the Tower | 53 | [`taking-the-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taking-the-tower.json) |
| Zhight Island (Quest) | 53 | [`zhight-island-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/zhight-island-quest.json) |
| The Maiden Tower | 50 | [`the-maiden-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-maiden-tower.json) |
| Desperate Metal | 48 | [`desperate-metal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/desperate-metal.json) |
| Creeper Infiltration | 47 | [`creeper-infiltration.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/creeper-infiltration.json) |
| Lava Springs | 46 | [`lava-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lava-springs.json) |
| Blazing Retribution | 45 | [`blazing-retribution.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/blazing-retribution.json) |
| Redbeard's Booty | 45 | [`redbeard-s-booty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/redbeard-s-booty.json) |
| Beneath the Depths | 44 | [`beneath-the-depths.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beneath-the-depths.json) |
| Canyon Condor | 44 | [`canyon-condor.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/canyon-condor.json) |
| Heart of Llevigar | 41 | [`heart-of-llevigar.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/heart-of-llevigar.json) |
| Grand Youth | 40 | [`grand-youth.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/grand-youth.json) |
| Lexdale Witch Trials | 39 | [`lexdale-witch-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lexdale-witch-trials.json) |
| The Sewers of Ragni | 39 | [`the-sewers-of-ragni.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-sewers-of-ragni.json) |
| wynntils.json | 39 | [`wynntils.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/wynntils.json) |
| Troubled Tribesmen | 38 | [`troubled-tribesmen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/troubled-tribesmen.json) |
| unsorted.json | 38 | [`unsorted.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/unsorted.json) |
| Jungle Fever | 37 | [`jungle-fever.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/jungle-fever.json) |
| Stable Story | 37 | [`stable-story.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/stable-story.json) |
| Reincarnation | 36 | [`reincarnation.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reincarnation.json) |
| Wrath of the Mummy | 35 | [`wrath-of-the-mummy.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wrath-of-the-mummy.json) |
| Death Whistle (Quest) | 34 | [`death-whistle-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/death-whistle-quest.json) |
| Green Gloop | 34 | [`green-gloop.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/green-gloop.json) |
| Potion Making | 34 | [`potion-making.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/potion-making.json) |
| Lost in the Jungle | 33 | [`lost-in-the-jungle.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-in-the-jungle.json) |
| Lost Royalty | 32 | [`lost-royalty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-royalty.json) |
| WynnExcavation Site A | 32 | [`wynnexcavation-site-a.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-a.json) |
| Maex Black Market | 31 | [`maex-black-market.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret/maex-black-market.json) |
| Ice Nations | 30 | [`ice-nations.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/ice-nations.json) |
| Star Thief | 30 | [`star-thief.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/star-thief.json) |
| The Passage (Quest) | 29 | [`the-passage-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-passage-quest.json) |
| Tribal Aggression | 29 | [`tribal-aggression.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tribal-aggression.json) |
| WynnExcavation Site B | 29 | [`wynnexcavation-site-b.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-b.json) |
| The Bigger Picture | 27 | [`the-bigger-picture.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-bigger-picture.json) |
| Enzan's Brother | 26 | [`enzan-s-brother.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enzan-s-brother.json) |
| Underwater | 26 | [`underwater.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underwater.json) |
| An Iron Heart Part I | 25 | [`an-iron-heart-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-i.json) |
| Maltic's Well | 25 | [`maltic-s-well.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/maltic-s-well.json) |
| Cook Assistant | 22 | [`cook-assistant.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cook-assistant.json) |
| label.json | 22 | [`label.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/scoped/label.json) |
| Tower of Ascension (Quest) | 21 | [`tower-of-ascension-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tower-of-ascension-quest.json) |
| Pirate's Trove | 20 | [`pirate-s-trove.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pirate-s-trove.json) |
| A Grave Mistake | 19 | [`a-grave-mistake.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-grave-mistake.json) |
| Solace in Pain | 18 | [`solace-in-pain.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret/solace-in-pain.json) |
| Dwelling Walls | 17 | [`dwelling-walls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwelling-walls.json) |
| The House of Twain (Quest) | 16 | [`the-house-of-twain-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-house-of-twain-quest.json) |
| The Olmic Rune | 16 | [`the-olmic-rune.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-olmic-rune.json) |
| Clearing the Camps | 15 | [`clearing-the-camps.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/clearing-the-camps.json) |
| Cluck Cluck | 15 | [`cluck-cluck.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cluck-cluck.json) |
| archetype.json | 15 | [`archetype.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/scoped/archetype.json) |
| profession-terms.json | 12 | [`profession-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/profession-terms.json) |
| Pit of the Dead (Quest) | 11 | [`pit-of-the-dead-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pit-of-the-dead-quest.json) |
| Lost Tower | 9 | [`lost-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-tower.json) |
| charm.json | 5 | [`charm.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/charm.json) |
| Ruler of the Skies | 5 | [`ruler-of-the-skies.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/secret/ruler-of-the-skies.json) |
| major-id-terms.json | 3 | [`major-id-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/major-id-terms.json) |
| bossbar.json | 3 | [`bossbar.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/scoped/bossbar.json) |
| chat-terms.json | 2 | [`chat-terms.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/chat-terms.json) |
| name.json | 1 | [`name.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/scoped/name.json) |

</details>

---

## 散在共用檔裡的條目

`misc.json`、`gui.json` 這類共用檔裡也有 AI 加的條目，但它們跟團隊翻的
混在同一個檔，沒辦法用檔層級標記。那些逐批記在 [CHANGELOG](../CHANGELOG.md) 裡——
每一則都寫著加了什麼、為什麼那樣譯。要校那部分請從 CHANGELOG 往回看。

