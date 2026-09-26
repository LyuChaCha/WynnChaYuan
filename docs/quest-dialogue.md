# 任務對話認領清單

**23,754 / 23,754 句已翻（100.0%），共 159 個任務，還沒有人動的有 0 個。**

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
| A Grave Mistake | [`a-grave-mistake.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-grave-mistake.json) | 21 | 19 | 5 | ✅ |
| A Headless History | [`a-headless-history.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-headless-history.json) | 210 | 197 | 7 | ✅ |
| A Hunter's Calling | [`a-hunter-s-calling.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-hunter-s-calling.json) | 271 | 242 | 24 | ✅ |
| A Journey Beyond | [`a-journey-beyond.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-beyond.json) | 248 | 230 | 7 | ✅ |
| A Journey Further | [`a-journey-further.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-further.json) | 89 | 77 | 2 | ✅ |
| A Journey Home | [`a-journey-home.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-journey-home.json) | 133 | 114 | 11 | ✅ |
| A Marauder's Dues | [`a-marauder-s-dues.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-marauder-s-dues.json) | 73 | 66 | 4 | ✅ |
| A New Beginning | [`a-new-beginning.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-new-beginning.json) | 359 | 346 | 13 | ✅ |
| A Sandy Scandal | [`a-sandy-scandal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/a-sandy-scandal.json) | 100 | 81 | 7 | ✅ |
| Acquiring Credentials | [`acquiring-credentials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/acquiring-credentials.json) | 92 | 81 | 10 | ✅ |
| Aldorei's Secret Part I | [`aldorei-s-secret-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-i.json) | 71 | 60 | 8 | ✅ |
| Aldorei's Secret Part II | [`aldorei-s-secret-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/aldorei-s-secret-part-ii.json) | 600 | 569 | 17 | ✅ |
| All Roads to Peace | [`all-roads-to-peace.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/all-roads-to-peace.json) | 402 | 389 | 38 | ✅ |
| An Iron Heart Part I | [`an-iron-heart-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-i.json) | 30 | 26 | 3 | ✅ |
| An Iron Heart Part II | [`an-iron-heart-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/an-iron-heart-part-ii.json) | 67 | 59 | 7 | ✅ |
| Apotheosis (Quest) | [`apotheosis-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/apotheosis-quest.json) | 708 | 689 | 26 | ✅ |
| Arachnids' Ascent | [`arachnids-ascent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/arachnids-ascent.json) | 130 | 124 | 7 | ✅ |
| Beneath the Depths | [`beneath-the-depths.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beneath-the-depths.json) | 49 | 43 | 3 | ✅ |
| Beyond the Grave | [`beyond-the-grave.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/beyond-the-grave.json) | 186 | 180 | 3 | ✅ |
| Blazing Retribution | [`blazing-retribution.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/blazing-retribution.json) | 46 | 39 | 4 | ✅ |
| Bob's Lost Soul | [`bob-s-lost-soul.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/bob-s-lost-soul.json) | 87 | 77 | 5 | ✅ |
| Brothers Return | [`brothers-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/brothers-return.json) | 109 | 95 | 7 | ✅ |
| Burning Bonds | [`burning-bonds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/burning-bonds.json) | 104 | 97 | 2 | ✅ |
| Canyon Condor | [`canyon-condor.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/canyon-condor.json) | 47 | 41 | 3 | ✅ |
| Celebrations in Smoke | [`celebrations-in-smoke.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/celebrations-in-smoke.json) | 560 | 541 | 14 | ✅ |
| Clearing the Camps | [`clearing-the-camps.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/clearing-the-camps.json) | 23 | 20 | 1 | ✅ |
| Cluck Cluck | [`cluck-cluck.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cluck-cluck.json) | 13 | 11 | 1 | ✅ |
| Cook Assistant | [`cook-assistant.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cook-assistant.json) | 38 | 33 | 2 | ✅ |
| Corrupted Betrayal | [`corrupted-betrayal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/corrupted-betrayal.json) | 73 | 67 | 4 | ✅ |
| Cowfusion | [`cowfusion.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/cowfusion.json) | 224 | 207 | 5 | ✅ |
| Creeper Infiltration | [`creeper-infiltration.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/creeper-infiltration.json) | 53 | 47 | 1 | ✅ |
| Crop Failure | [`crop-failure.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/crop-failure.json) | 67 | 62 | 2 | ✅ |
| Dearly Departed | [`dearly-departed.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dearly-departed.json) | 64 | 58 | 2 | ✅ |
| Death Whistle (Quest) | [`death-whistle-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/death-whistle-quest.json) | 40 | 34 | 3 | ✅ |
| Deja Vu | [`deja-vu.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/deja-vu.json) | 90 | 82 | 2 | ✅ |
| Desperate Metal | [`desperate-metal.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/desperate-metal.json) | 37 | 32 | 4 | ✅ |
| Dwarves and Doguns Part I | [`dwarves-and-doguns-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-i.json) | 146 | 131 | 18 | ✅ |
| Dwarves and Doguns Part II | [`dwarves-and-doguns-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-ii.json) | 87 | 75 | 6 | ✅ |
| Dwarves and Doguns Part III | [`dwarves-and-doguns-part-iii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iii.json) | 80 | 70 | 5 | ✅ |
| Dwarves and Doguns Part IV | [`dwarves-and-doguns-part-iv.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwarves-and-doguns-part-iv.json) | 67 | 59 | 7 | ✅ |
| Dwelling Walls | [`dwelling-walls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/dwelling-walls.json) | 18 | 14 | 1 | ✅ |
| Echoes of Change | [`echoes-of-change.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/echoes-of-change.json) | 740 | 711 | 26 | ✅ |
| Elemental Exercise | [`elemental-exercise.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/elemental-exercise.json) | 83 | 74 | 4 | ✅ |
| Ensemble of Hope | [`ensemble-of-hope.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/ensemble-of-hope.json) | 765 | 743 | 34 | ✅ |
| Enter the Dojo | [`enter-the-dojo.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enter-the-dojo.json) | 36 | 28 | 1 | ✅ |
| Enzan's Brother | [`enzan-s-brother.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/enzan-s-brother.json) | 31 | 28 | 2 | ✅ |
| Fallen Delivery | [`fallen-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fallen-delivery.json) | 65 | 55 | 5 | ✅ |
| Fantastic Voyage | [`fantastic-voyage.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fantastic-voyage.json) | 283 | 264 | 5 | ✅ |
| Fate of the Fallen | [`fate-of-the-fallen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/fate-of-the-fallen.json) | 93 | 84 | 5 | ✅ |
| Finding the Light | [`finding-the-light.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/finding-the-light.json) | 162 | 146 | 3 | ✅ |
| Flight in Distress | [`flight-in-distress.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/flight-in-distress.json) | 103 | 85 | 21 | ✅ |
| Forbidden Prison (Quest) | [`forbidden-prison-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/forbidden-prison-quest.json) | 120 | 104 | 10 | ✅ |
| From the Bottom | [`from-the-bottom.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-bottom.json) | 91 | 82 | 8 | ✅ |
| From the Mountains | [`from-the-mountains.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/from-the-mountains.json) | 63 | 54 | 2 | ✅ |
| Frost Bite | [`frost-bite.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/frost-bite.json) | 92 | 83 | 2 | ✅ |
| General's Orders | [`general-s-orders.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/general-s-orders.json) | 114 | 102 | 10 | ✅ |
| Grand Youth | [`grand-youth.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/grand-youth.json) | 39 | 33 | 4 | ✅ |
| Green Gloop | [`green-gloop.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/green-gloop.json) | 35 | 31 | 2 | ✅ |
| Haven Antiquity | [`haven-antiquity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/haven-antiquity.json) | 54 | 46 | 3 | ✅ |
| Heart of Llevigar | [`heart-of-llevigar.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/heart-of-llevigar.json) | 41 | 32 | 3 | ✅ |
| Hollow Serenity | [`hollow-serenity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hollow-serenity.json) | 295 | 276 | 16 | ✅ |
| Hunger of the Gerts Part I | [`hunger-of-the-gerts-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-i.json) | 113 | 99 | 8 | ✅ |
| Hunger of the Gerts Part II | [`hunger-of-the-gerts-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/hunger-of-the-gerts-part-ii.json) | 76 | 65 | 7 | ✅ |
| Ice Nations | [`ice-nations.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/ice-nations.json) | 28 | 24 | 2 | ✅ |
| Infested Plants | [`infested-plants.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/infested-plants.json) | 62 | 57 | 4 | ✅ |
| Jungle Fever | [`jungle-fever.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/jungle-fever.json) | 40 | 34 | 3 | ✅ |
| King's Recruit | [`king-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/king-s-recruit.json) | 131 | 109 | 8 | ✅ |
| Kingdom of Sand | [`kingdom-of-sand.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/kingdom-of-sand.json) | 75 | 66 | 8 | ✅ |
| Lava Springs | [`lava-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lava-springs.json) | 50 | 42 | 3 | ✅ |
| Lazarus Pit (Quest) | [`lazarus-pit-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lazarus-pit-quest.json) | 78 | 59 | 7 | ✅ |
| Lexdale Witch Trials | [`lexdale-witch-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lexdale-witch-trials.json) | 34 | 26 | 3 | ✅ |
| Lost Royalty | [`lost-royalty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-royalty.json) | 26 | 20 | 2 | ✅ |
| Lost Soles | [`lost-soles.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-soles.json) | 63 | 53 | 7 | ✅ |
| Lost Tower | [`lost-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-tower.json) | 12 | 8 | 1 | ✅ |
| Lost in the Jungle | [`lost-in-the-jungle.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/lost-in-the-jungle.json) | 34 | 29 | 3 | ✅ |
| Maltic's Well | [`maltic-s-well.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/maltic-s-well.json) | 29 | 27 | 3 | ✅ |
| Master Piece | [`master-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/master-piece.json) | 59 | 52 | 1 | ✅ |
| Meaningful Holiday | [`meaningful-holiday.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/meaningful-holiday.json) | 124 | 109 | 10 | ✅ |
| Memory Paranoia | [`memory-paranoia.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/memory-paranoia.json) | 105 | 80 | 13 | ✅ |
| Misadventure on the Sea | [`misadventure-on-the-sea.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/misadventure-on-the-sea.json) | 194 | 186 | 6 | ✅ |
| Mixed Feelings | [`mixed-feelings.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mixed-feelings.json) | 76 | 67 | 4 | ✅ |
| Murder Mystery | [`murder-mystery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/murder-mystery.json) | 58 | 52 | 5 | ✅ |
| Mushroom Man | [`mushroom-man.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/mushroom-man.json) | 91 | 85 | 6 | ✅ |
| Off the Rails | [`off-the-rails.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/off-the-rails.json) | 791 | 783 | 12 | ✅ |
| One Thousand Meters Under | [`one-thousand-meters-under.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/one-thousand-meters-under.json) | 105 | 86 | 10 | ✅ |
| Out of my Mind | [`out-of-my-mind.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/out-of-my-mind.json) | 74 | 67 | 4 | ✅ |
| Overture to Despair | [`overture-to-despair.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/overture-to-despair.json) | 658 | 637 | 26 | ✅ |
| Pirate's Trove | [`pirate-s-trove.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pirate-s-trove.json) | 27 | 22 | 1 | ✅ |
| Pit of the Dead (Quest) | [`pit-of-the-dead-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/pit-of-the-dead-quest.json) | 14 | 11 | 1 | ✅ |
| Point of No Return | [`point-of-no-return.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/point-of-no-return.json) | 63 | 51 | 3 | ✅ |
| Potion Making | [`potion-making.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/potion-making.json) | 36 | 32 | 2 | ✅ |
| Purple and Blue | [`purple-and-blue.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/purple-and-blue.json) | 84 | 76 | 3 | ✅ |
| Queen's Recruit | [`queen-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/queen-s-recruit.json) | 695 | 669 | 20 | ✅ |
| Recipe For Disaster | [`recipe-for-disaster.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recipe-for-disaster.json) | 138 | 130 | 10 | ✅ |
| Reclaiming the House | [`reclaiming-the-house.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reclaiming-the-house.json) | 70 | 58 | 4 | ✅ |
| Recover the Past | [`recover-the-past.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/recover-the-past.json) | 348 | 332 | 11 | ✅ |
| Redbeard's Booty | [`redbeard-s-booty.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/redbeard-s-booty.json) | 47 | 41 | 4 | ✅ |
| Reincarnation | [`reincarnation.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/reincarnation.json) | 33 | 29 | 2 | ✅ |
| Revelations in Fall | [`revelations-in-fall.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/revelations-in-fall.json) | 309 | 296 | 10 | ✅ |
| Rise of the Quartron | [`rise-of-the-quartron.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/rise-of-the-quartron.json) | 117 | 107 | 7 | ✅ |
| Rogue Wyrmling | [`rogue-wyrmling.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/rogue-wyrmling.json) | 1 | 1 | 0 | ✅ |
| Royal Trials | [`royal-trials.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/royal-trials.json) | 85 | 66 | 7 | ✅ |
| Shattered Minds | [`shattered-minds.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shattered-minds.json) | 51 | 45 | 7 | ✅ |
| Shrouded in Mist | [`shrouded-in-mist.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/shrouded-in-mist.json) | 314 | 296 | 14 | ✅ |
| Skittering Spiders | [`skittering-spiders.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/skittering-spiders.json) | 8 | 8 | 0 | ✅ |
| Solidarity of Steel | [`solidarity-of-steel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/solidarity-of-steel.json) | 525 | 503 | 17 | ✅ |
| Stable Story | [`stable-story.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/stable-story.json) | 38 | 36 | 1 | ✅ |
| Star Thief | [`star-thief.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/star-thief.json) | 24 | 17 | 3 | ✅ |
| Supply and Delivery | [`supply-and-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/supply-and-delivery.json) | 87 | 81 | 3 | ✅ |
| Taking the Tower | [`taking-the-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taking-the-tower.json) | 59 | 52 | 3 | ✅ |
| Taproot | [`taproot.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/taproot.json) | 116 | 104 | 4 | ✅ |
| Temple of the Legends (Quest) | [`temple-of-the-legends-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/temple-of-the-legends-quest.json) | 137 | 124 | 4 | ✅ |
| Tempo Town Trouble | [`tempo-town-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tempo-town-trouble.json) | 63 | 58 | 4 | ✅ |
| The Bigger Picture | [`the-bigger-picture.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-bigger-picture.json) | 25 | 18 | 1 | ✅ |
| The Breaking Point | [`the-breaking-point.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-breaking-point.json) | 304 | 285 | 20 | ✅ |
| The Canary Calls | [`the-canary-calls.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canary-calls.json) | 220 | 211 | 9 | ✅ |
| The Canyon Guides | [`the-canyon-guides.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-canyon-guides.json) | 63 | 56 | 2 | ✅ |
| The Corrupted Village | [`the-corrupted-village.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-corrupted-village.json) | 71 | 61 | 3 | ✅ |
| The Cursed One | [`the-cursed-one.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-cursed-one.json) | 503 | 485 | 20 | ✅ |
| The Dark Descent | [`the-dark-descent.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-dark-descent.json) | 81 | 75 | 5 | ✅ |
| The Envoy Part I | [`the-envoy-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-i.json) | 121 | 110 | 7 | ✅ |
| The Envoy Part II | [`the-envoy-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-envoy-part-ii.json) | 117 | 104 | 12 | ✅ |
| The Feathers Fly Part I | [`the-feathers-fly-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-feathers-fly-part-i.json) | 210 | 198 | 6 | ✅ |
| The Feathers Fly Part II | [`the-feathers-fly-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-feathers-fly-part-ii.json) | 1286 | 1272 | 19 | ✅ |
| The Hero of Gavel | [`the-hero-of-gavel.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hero-of-gavel.json) | 196 | 177 | 11 | ✅ |
| The Hidden City | [`the-hidden-city.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-hidden-city.json) | 75 | 59 | 13 | ✅ |
| The House of Twain (Quest) | [`the-house-of-twain-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-house-of-twain-quest.json) | 16 | 14 | 2 | ✅ |
| The Lost | [`the-lost.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-lost.json) | 48 | 45 | 4 | ✅ |
| The Maiden Tower | [`the-maiden-tower.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-maiden-tower.json) | 43 | 39 | 2 | ✅ |
| The Mercenary | [`the-mercenary.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-mercenary.json) | 155 | 145 | 8 | ✅ |
| The Missing Piece | [`the-missing-piece.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-missing-piece.json) | 615 | 546 | 15 | ✅ |
| The Olmic Rune | [`the-olmic-rune.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-olmic-rune.json) | 15 | 8 | 0 | ✅ |
| The Order of the Grook | [`the-order-of-the-grook.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-order-of-the-grook.json) | 260 | 248 | 9 | ✅ |
| The Passage (Quest) | [`the-passage-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-passage-quest.json) | 29 | 26 | 2 | ✅ |
| The Price of Ingenuity | [`the-price-of-ingenuity.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-price-of-ingenuity.json) | 599 | 580 | 29 | ✅ |
| The Qira Hive (Quest) | [`the-qira-hive-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-qira-hive-quest.json) | 63 | 63 | 7 | ✅ |
| The Realm of Light (Quest) | [`the-realm-of-light-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-realm-of-light-quest.json) | 116 | 103 | 1 | ✅ |
| The Scarred Springs | [`the-scarred-springs.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-scarred-springs.json) | 137 | 124 | 10 | ✅ |
| The Sewers of Ragni | [`the-sewers-of-ragni.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-sewers-of-ragni.json) | 54 | 48 | 1 | ✅ |
| The Shadow of the Beast | [`the-shadow-of-the-beast.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-shadow-of-the-beast.json) | 87 | 75 | 4 | ✅ |
| The Strong Survive | [`the-strong-survive.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-strong-survive.json) | 660 | 627 | 43 | ✅ |
| The Thanos Depository | [`the-thanos-depository.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-thanos-depository.json) | 51 | 42 | 2 | ✅ |
| The Ultimate Weapon | [`the-ultimate-weapon.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-ultimate-weapon.json) | 77 | 65 | 8 | ✅ |
| The Worm Holes | [`the-worm-holes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/the-worm-holes.json) | 97 | 91 | 3 | ✅ |
| Through the Pipes | [`through-the-pipes.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/through-the-pipes.json) | 223 | 210 | 2 | ✅ |
| Tower of Ascension (Quest) | [`tower-of-ascension-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tower-of-ascension-quest.json) | 20 | 20 | 2 | ✅ |
| Tribal Aggression | [`tribal-aggression.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tribal-aggression.json) | 29 | 25 | 2 | ✅ |
| Troubled Tribesmen | [`troubled-tribesmen.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/troubled-tribesmen.json) | 26 | 20 | 3 | ✅ |
| True Colours | [`true-colours.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/true-colours.json) | 570 | 551 | 11 | ✅ |
| Tunnel Trouble | [`tunnel-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/tunnel-trouble.json) | 71 | 60 | 6 | ✅ |
| Underice | [`underice.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underice.json) | 64 | 50 | 5 | ✅ |
| Undersupply | [`undersupply.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/undersupply.json) | 125 | 120 | 10 | ✅ |
| Underwater | [`underwater.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/underwater.json) | 26 | 21 | 2 | ✅ |
| Wrath of the Mummy | [`wrath-of-the-mummy.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wrath-of-the-mummy.json) | 40 | 33 | 3 | ✅ |
| WynnExcavation Site A | [`wynnexcavation-site-a.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-a.json) | 33 | 27 | 2 | ✅ |
| WynnExcavation Site B | [`wynnexcavation-site-b.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-b.json) | 29 | 24 | 4 | ✅ |
| WynnExcavation Site C | [`wynnexcavation-site-c.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-c.json) | 62 | 53 | 5 | ✅ |
| WynnExcavation Site D | [`wynnexcavation-site-d.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/wynnexcavation-site-d.json) | 98 | 69 | 15 | ✅ |
| Zhight Island (Quest) | [`zhight-island-quest.json`](../src/main/resources/assets/wynnchayuan/translations/zh_tw/quest/zhight-island-quest.json) | 52 | 47 | 4 | ✅ |

---

這份清單由 `tools/quest-index.py` 產生，翻譯有進度就重跑一次。
