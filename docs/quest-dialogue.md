# 任務對話認領清單

**22,671 / 22,844 句已翻（99.2%），共 160 個任務，還沒有人動的有 0 個。**

**一個任務一個檔案**，放在 [`translations/quest/`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/)。接了哪個任務就開哪個檔，不會跟別人在同一個檔案裡打架。

> 改完之後跑一次 `python tools/quest-bundle.py`——模組讀的是合併後的`quest-dialogue.json`，那是產生物，不要直接改。

每一條長這樣——`quest`、`stage`、`speaker` 是給你看上下文用的，**不用翻**：

```json
"Cook Assistant#004": {
  "src": "Unfortunately, a Grook took my last cake, and I ran out of ingredients!",
  "dst": "",
  "quest": "Cook Assistant",
  "stage": "Stage 1",
  "speaker": "The Cook"
}
```

> 對話取自 [Wynncraft wiki](https://wynncraft.wiki.gg/)，跟遊戲裡**不保證逐字相同**。翻的時候發現不一樣，以遊戲裡的為準，直接改 `src`。

---

## 認領方式

**動手前先講一聲**（[開一則 issue](https://github.com/LyuChaCha/WynnChaYuan/issues) 或直接找 LyuChaCha），說你要接哪個任務。
兩個人同時翻同一個任務，合併時會互相覆蓋。

## 全部任務

「台詞」是 NPC 講的話，其餘是任務目標（顯示在追蹤器上）。

| 任務 | 檔案 | 句數 | 台詞 | 角色 | 進度 |
|---|---|---|---|---|---|
| A Grave Mistake | [`a-grave-mistake.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-grave-mistake.json) | 19 | 17 | 5 | ✅ |
| A Headless History | [`a-headless-history.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-headless-history.json) | 203 | 192 | 7 | 99% |
| A Hunter's Calling | [`a-hunter-s-calling.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-hunter-s-calling.json) | 253 | 233 | 24 | ✅ |
| A Journey Beyond | [`a-journey-beyond.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-beyond.json) | 242 | 224 | 7 | ✅ |
| A Journey Further | [`a-journey-further.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-further.json) | 88 | 77 | 2 | ✅ |
| A Journey Home | [`a-journey-home.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-home.json) | 131 | 114 | 11 | ✅ |
| A Marauder's Dues | [`a-marauder-s-dues.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-marauder-s-dues.json) | 68 | 62 | 4 | ✅ |
| A New Beginning | [`a-new-beginning.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-new-beginning.json) | 323 | 312 | 13 | ✅ |
| A Sandy Scandal | [`a-sandy-scandal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-sandy-scandal.json) | 92 | 76 | 7 | ✅ |
| Acquiring Credentials | [`acquiring-credentials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/acquiring-credentials.json) | 123 | 112 | 18 | 76% |
| Aldorei's Secret Part I | [`aldorei-s-secret-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-i.json) | 95 | 84 | 14 | 88% |
| Aldorei's Secret Part II | [`aldorei-s-secret-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-ii.json) | 591 | 561 | 23 | ✅ |
| All Roads to Peace | [`all-roads-to-peace.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/all-roads-to-peace.json) | 484 | 471 | 38 | ✅ |
| An Iron Heart Part I | [`an-iron-heart-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-i.json) | 24 | 20 | 3 | ✅ |
| An Iron Heart Part II | [`an-iron-heart-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-ii.json) | 66 | 58 | 7 | ✅ |
| Apotheosis (Quest) | [`apotheosis-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/apotheosis-quest.json) | 616 | 597 | 26 | ✅ |
| Arachnids' Ascent | [`arachnids-ascent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/arachnids-ascent.json) | 111 | 105 | 7 | ✅ |
| Beneath the Depths | [`beneath-the-depths.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beneath-the-depths.json) | 47 | 41 | 3 | ✅ |
| Beyond the Grave | [`beyond-the-grave.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beyond-the-grave.json) | 181 | 169 | 3 | ✅ |
| Blazing Retribution | [`blazing-retribution.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/blazing-retribution.json) | 51 | 44 | 6 | 84% |
| Bob's Lost Soul | [`bob-s-lost-soul.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/bob-s-lost-soul.json) | 89 | 79 | 6 | 98% |
| Brothers Return | [`brothers-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/brothers-return.json) | 100 | 86 | 7 | 99% |
| Burning Bonds | [`burning-bonds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/burning-bonds.json) | 103 | 96 | 2 | ✅ |
| Canyon Condor | [`canyon-condor.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/canyon-condor.json) | 48 | 42 | 4 | ✅ |
| Celebrations in Smoke | [`celebrations-in-smoke.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/celebrations-in-smoke.json) | 530 | 511 | 14 | ✅ |
| Clearing the Camps | [`clearing-the-camps.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/clearing-the-camps.json) | 18 | 15 | 1 | 94% |
| Cluck Cluck | [`cluck-cluck.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cluck-cluck.json) | 13 | 11 | 1 | ✅ |
| Cook Assistant | [`cook-assistant.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cook-assistant.json) | 37 | 32 | 2 | ✅ |
| Corrupted Betrayal | [`corrupted-betrayal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/corrupted-betrayal.json) | 69 | 63 | 4 | ✅ |
| Cowfusion | [`cowfusion.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cowfusion.json) | 169 | 154 | 5 | ✅ |
| Creeper Infiltration | [`creeper-infiltration.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/creeper-infiltration.json) | 47 | 42 | 2 | ✅ |
| Crop Failure | [`crop-failure.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/crop-failure.json) | 52 | 48 | 3 | 98% |
| Dearly Departed | [`dearly-departed.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dearly-departed.json) | 57 | 53 | 2 | ✅ |
| Death Whistle (Quest) | [`death-whistle-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/death-whistle-quest.json) | 34 | 28 | 4 | 97% |
| Deja Vu | [`deja-vu.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/deja-vu.json) | 89 | 81 | 2 | ✅ |
| Desperate Metal | [`desperate-metal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/desperate-metal.json) | 48 | 42 | 7 | ✅ |
| Dwarves and Doguns Part I | [`dwarves-and-doguns-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-i.json) | 180 | 160 | 19 | ✅ |
| Dwarves and Doguns Part II | [`dwarves-and-doguns-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-ii.json) | 90 | 76 | 6 | ✅ |
| Dwarves and Doguns Part III | [`dwarves-and-doguns-part-iii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iii.json) | 86 | 72 | 5 | ✅ |
| Dwarves and Doguns Part IV | [`dwarves-and-doguns-part-iv.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iv.json) | 70 | 58 | 7 | ✅ |
| Dwelling Walls | [`dwelling-walls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwelling-walls.json) | 18 | 14 | 1 | 94% |
| Echoes of Change | [`echoes-of-change.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/echoes-of-change.json) | 720 | 691 | 26 | ✅ |
| Elemental Exercise | [`elemental-exercise.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/elemental-exercise.json) | 83 | 74 | 4 | 93% |
| Ensemble of Hope | [`ensemble-of-hope.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/ensemble-of-hope.json) | 726 | 705 | 34 | ✅ |
| Enter the Dojo | [`enter-the-dojo.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enter-the-dojo.json) | 115 | 105 | 2 | ✅ |
| Enzan's Brother | [`enzan-s-brother.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enzan-s-brother.json) | 30 | 27 | 2 | 97% |
| Fallen Delivery | [`fallen-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fallen-delivery.json) | 65 | 55 | 5 | ✅ |
| Fallen Factory | [`fallen-factory.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fallen-factory.json) | 58 | 58 | 2 | ✅ |
| Fantastic Voyage | [`fantastic-voyage.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fantastic-voyage.json) | 306 | 285 | 5 | ✅ |
| Fate of the Fallen | [`fate-of-the-fallen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fate-of-the-fallen.json) | 88 | 79 | 5 | ✅ |
| Finding the Light | [`finding-the-light.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/finding-the-light.json) | 156 | 140 | 3 | ✅ |
| Flight in Distress | [`flight-in-distress.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/flight-in-distress.json) | 112 | 94 | 23 | ✅ |
| Forbidden Prison (Quest) | [`forbidden-prison-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/forbidden-prison-quest.json) | 207 | 192 | 15 | ✅ |
| From the Bottom | [`from-the-bottom.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-bottom.json) | 89 | 81 | 8 | ✅ |
| From the Mountains | [`from-the-mountains.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-mountains.json) | 73 | 64 | 2 | ✅ |
| Frost Bite | [`frost-bite.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/frost-bite.json) | 88 | 79 | 2 | ✅ |
| General's Orders | [`general-s-orders.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/general-s-orders.json) | 138 | 98 | 10 | ✅ |
| Grand Youth | [`grand-youth.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/grand-youth.json) | 39 | 33 | 6 | ✅ |
| Green Gloop | [`green-gloop.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/green-gloop.json) | 37 | 33 | 2 | 89% |
| Haven Antiquity | [`haven-antiquity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/haven-antiquity.json) | 62 | 54 | 5 | ✅ |
| Heart of Llevigar | [`heart-of-llevigar.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/heart-of-llevigar.json) | 42 | 33 | 3 | 88% |
| Hollow Serenity | [`hollow-serenity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hollow-serenity.json) | 431 | 412 | 27 | ✅ |
| Hunger of the Gerts Part I | [`hunger-of-the-gerts-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-i.json) | 111 | 97 | 8 | ✅ |
| Hunger of the Gerts Part II | [`hunger-of-the-gerts-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-ii.json) | 74 | 63 | 7 | ✅ |
| Ice Nations | [`ice-nations.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/ice-nations.json) | 30 | 26 | 4 | 97% |
| Infested Plants | [`infested-plants.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/infested-plants.json) | 60 | 55 | 4 | 98% |
| Jungle Fever | [`jungle-fever.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/jungle-fever.json) | 37 | 31 | 3 | ✅ |
| King's Recruit | [`king-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/king-s-recruit.json) | 128 | 108 | 8 | ✅ |
| Kingdom of Sand | [`kingdom-of-sand.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/kingdom-of-sand.json) | 72 | 63 | 8 | 99% |
| Lava Springs | [`lava-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lava-springs.json) | 46 | 39 | 4 | ✅ |
| Lazarus Pit (Quest) | [`lazarus-pit-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lazarus-pit-quest.json) | 92 | 73 | 14 | ✅ |
| Lexdale Witch Trials | [`lexdale-witch-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lexdale-witch-trials.json) | 39 | 32 | 5 | ✅ |
| Lost Royalty | [`lost-royalty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-royalty.json) | 32 | 26 | 2 | ✅ |
| Lost Soles | [`lost-soles.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-soles.json) | 70 | 61 | 7 | ✅ |
| Lost Tower | [`lost-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-tower.json) | 9 | 6 | 1 | ✅ |
| Lost in the Jungle | [`lost-in-the-jungle.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-in-the-jungle.json) | 34 | 29 | 3 | 97% |
| Maltic's Well | [`maltic-s-well.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/maltic-s-well.json) | 27 | 25 | 3 | 93% |
| Master Piece | [`master-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/master-piece.json) | 58 | 51 | 1 | ✅ |
| Meaningful Holiday | [`meaningful-holiday.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/meaningful-holiday.json) | 122 | 107 | 10 | 93% |
| Memory Paranoia | [`memory-paranoia.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/memory-paranoia.json) | 127 | 104 | 13 | 98% |
| Misadventure on the Sea | [`misadventure-on-the-sea.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/misadventure-on-the-sea.json) | 199 | 191 | 7 | 91% |
| Mixed Feelings | [`mixed-feelings.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mixed-feelings.json) | 97 | 77 | 4 | ✅ |
| Murder Mystery | [`murder-mystery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/murder-mystery.json) | 77 | 71 | 11 | ✅ |
| Mushroom Man | [`mushroom-man.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mushroom-man.json) | 85 | 80 | 6 | 96% |
| Off the Rails | [`off-the-rails.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/off-the-rails.json) | 752 | 744 | 12 | ✅ |
| One Thousand Meters Under | [`one-thousand-meters-under.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/one-thousand-meters-under.json) | 102 | 85 | 10 | ✅ |
| Out of my Mind | [`out-of-my-mind.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/out-of-my-mind.json) | 61 | 55 | 4 | ✅ |
| Overture to Despair | [`overture-to-despair.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/overture-to-despair.json) | 602 | 582 | 26 | ✅ |
| Pirate's Trove | [`pirate-s-trove.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pirate-s-trove.json) | 24 | 19 | 1 | ✅ |
| Pit of the Dead (Quest) | [`pit-of-the-dead-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pit-of-the-dead-quest.json) | 11 | 9 | 1 | ✅ |
| Point of No Return | [`point-of-no-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/point-of-no-return.json) | 53 | 49 | 3 | ✅ |
| Potion Making | [`potion-making.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/potion-making.json) | 33 | 29 | 3 | ✅ |
| Purple and Blue | [`purple-and-blue.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/purple-and-blue.json) | 81 | 73 | 3 | ✅ |
| Queen's Recruit | [`queen-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/queen-s-recruit.json) | 621 | 595 | 20 | ✅ |
| Recipe For Disaster | [`recipe-for-disaster.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recipe-for-disaster.json) | 167 | 146 | 10 | ✅ |
| Reclaiming the House | [`reclaiming-the-house.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reclaiming-the-house.json) | 67 | 55 | 4 | ✅ |
| Recover the Past | [`recover-the-past.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recover-the-past.json) | 302 | 286 | 11 | ✅ |
| Redbeard's Booty | [`redbeard-s-booty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/redbeard-s-booty.json) | 45 | 39 | 5 | 98% |
| Reincarnation | [`reincarnation.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reincarnation.json) | 36 | 32 | 3 | 92% |
| Revelations in Fall | [`revelations-in-fall.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/revelations-in-fall.json) | 284 | 271 | 10 | ✅ |
| Rise of the Quartron | [`rise-of-the-quartron.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/rise-of-the-quartron.json) | 118 | 108 | 8 | 92% |
| Rogue Wyrmling | [`rogue-wyrmling.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/rogue-wyrmling.json) | 1 | 1 | 0 | ✅ |
| Royal Trials | [`royal-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/royal-trials.json) | 83 | 66 | 7 | ✅ |
| Shattered Minds | [`shattered-minds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shattered-minds.json) | 66 | 60 | 12 | ✅ |
| Shrouded in Mist | [`shrouded-in-mist.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shrouded-in-mist.json) | 301 | 285 | 14 | ✅ |
| Skittering Spiders | [`skittering-spiders.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/skittering-spiders.json) | 8 | 8 | 0 | ✅ |
| Solidarity of Steel | [`solidarity-of-steel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/solidarity-of-steel.json) | 513 | 491 | 17 | ✅ |
| Stable Story | [`stable-story.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/stable-story.json) | 35 | 33 | 1 | 97% |
| Star Thief | [`star-thief.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/star-thief.json) | 24 | 17 | 3 | 96% |
| Supply and Delivery | [`supply-and-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/supply-and-delivery.json) | 67 | 61 | 4 | ✅ |
| Taking the Tower | [`taking-the-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taking-the-tower.json) | 51 | 44 | 3 | ✅ |
| Taproot | [`taproot.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taproot.json) | 114 | 103 | 4 | ✅ |
| Temple of the Legends (Quest) | [`temple-of-the-legends-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/temple-of-the-legends-quest.json) | 137 | 124 | 5 | 99% |
| Tempo Town Trouble | [`tempo-town-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tempo-town-trouble.json) | 58 | 53 | 4 | ✅ |
| The Bigger Picture | [`the-bigger-picture.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-bigger-picture.json) | 25 | 18 | 1 | ✅ |
| The Breaking Point | [`the-breaking-point.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-breaking-point.json) | 310 | 291 | 19 | ✅ |
| The Canary Calls | [`the-canary-calls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canary-calls.json) | 323 | 314 | 9 | ✅ |
| The Canyon Guides | [`the-canyon-guides.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canyon-guides.json) | 71 | 65 | 2 | ✅ |
| The Corrupted Village | [`the-corrupted-village.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-corrupted-village.json) | 71 | 61 | 3 | ✅ |
| The Cursed One | [`the-cursed-one.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-cursed-one.json) | 490 | 474 | 20 | ✅ |
| The Dark Descent | [`the-dark-descent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-dark-descent.json) | 72 | 66 | 5 | ✅ |
| The Envoy Part I | [`the-envoy-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-i.json) | 104 | 93 | 7 | ✅ |
| The Envoy Part II | [`the-envoy-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-ii.json) | 101 | 90 | 12 | ✅ |
| The Feathers Fly Part I | [`the-feathers-fly-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-feathers-fly-part-i.json) | 226 | 214 | 6 | ✅ |
| The Feathers Fly Part II | [`the-feathers-fly-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-feathers-fly-part-ii.json) | 499 | 486 | 19 | ✅ |
| The Hero of Gavel | [`the-hero-of-gavel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hero-of-gavel.json) | 193 | 175 | 11 | ✅ |
| The Hidden City | [`the-hidden-city.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hidden-city.json) | 75 | 59 | 13 | ✅ |
| The House of Twain (Quest) | [`the-house-of-twain-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-house-of-twain-quest.json) | 16 | 14 | 2 | ✅ |
| The Lost | [`the-lost.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-lost.json) | 57 | 44 | 4 | ✅ |
| The Maiden Tower | [`the-maiden-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-maiden-tower.json) | 50 | 46 | 4 | 74% |
| The Mercenary | [`the-mercenary.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-mercenary.json) | 133 | 124 | 8 | 98% |
| The Missing Piece | [`the-missing-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-missing-piece.json) | 549 | 496 | 15 | ✅ |
| The Olmic Rune | [`the-olmic-rune.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-olmic-rune.json) | 13 | 8 | 0 | ✅ |
| The Order of the Grook | [`the-order-of-the-grook.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-order-of-the-grook.json) | 245 | 233 | 11 | 99% |
| The Passage (Quest) | [`the-passage-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-passage-quest.json) | 32 | 30 | 4 | 94% |
| The Price of Ingenuity | [`the-price-of-ingenuity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-price-of-ingenuity.json) | 585 | 566 | 29 | ✅ |
| The Qira Hive (Quest) | [`the-qira-hive-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-qira-hive-quest.json) | 60 | 60 | 7 | ✅ |
| The Realm of Light (Quest) | [`the-realm-of-light-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-realm-of-light-quest.json) | 114 | 101 | 1 | ✅ |
| The Scarred Springs | [`the-scarred-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-scarred-springs.json) | 135 | 122 | 10 | ✅ |
| The Sewers of Ragni | [`the-sewers-of-ragni.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-sewers-of-ragni.json) | 36 | 30 | 1 | ✅ |
| The Shadow of the Beast | [`the-shadow-of-the-beast.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-shadow-of-the-beast.json) | 90 | 78 | 4 | ✅ |
| The Strong Survive | [`the-strong-survive.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-strong-survive.json) | 642 | 609 | 43 | ✅ |
| The Thanos Depository | [`the-thanos-depository.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-thanos-depository.json) | 60 | 51 | 2 | ✅ |
| The Ultimate Weapon | [`the-ultimate-weapon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-ultimate-weapon.json) | 77 | 65 | 8 | ✅ |
| The Worm Holes | [`the-worm-holes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-worm-holes.json) | 96 | 90 | 4 | 99% |
| Through the Pipes | [`through-the-pipes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/through-the-pipes.json) | 210 | 197 | 2 | ✅ |
| Tower of Ascension (Quest) | [`tower-of-ascension-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tower-of-ascension-quest.json) | 21 | 21 | 3 | 95% |
| Tribal Aggression | [`tribal-aggression.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tribal-aggression.json) | 29 | 25 | 3 | 86% |
| Troubled Tribesmen | [`troubled-tribesmen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/troubled-tribesmen.json) | 38 | 32 | 6 | ✅ |
| True Colours | [`true-colours.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/true-colours.json) | 510 | 491 | 11 | ✅ |
| Tunnel Trouble | [`tunnel-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tunnel-trouble.json) | 68 | 57 | 6 | ✅ |
| Underice | [`underice.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underice.json) | 63 | 50 | 7 | 94% |
| Undersupply | [`undersupply.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/undersupply.json) | 110 | 105 | 10 | ✅ |
| Underwater | [`underwater.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underwater.json) | 26 | 21 | 2 | 96% |
| Wrath of the Mummy | [`wrath-of-the-mummy.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wrath-of-the-mummy.json) | 38 | 32 | 5 | ✅ |
| WynnExcavation Site A | [`wynnexcavation-site-a.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-a.json) | 32 | 26 | 2 | 97% |
| WynnExcavation Site B | [`wynnexcavation-site-b.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-b.json) | 29 | 24 | 4 | ✅ |
| WynnExcavation Site C | [`wynnexcavation-site-c.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-c.json) | 61 | 52 | 6 | 98% |
| WynnExcavation Site D | [`wynnexcavation-site-d.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-d.json) | 136 | 107 | 15 | 99% |
| Zhight Island (Quest) | [`zhight-island-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/zhight-island-quest.json) | 60 | 55 | 7 | 85% |

---

這份清單由 `tools/quest-index.py` 產生，翻譯有進度就重跑一次。
