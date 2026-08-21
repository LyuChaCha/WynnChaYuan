# 任務對話認領清單

**0 / 22,088 句已翻（0.0%），共 157 個任務，還沒有人動的有 157 個。**

**一個任務一個檔案**，放在 [`translations/quest/`](../src/main/resources/assets/wynnchayuan/translations/quest/)。接了哪個任務就開哪個檔，不會跟別人在同一個檔案裡打架。

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

## 適合第一次接的（20 句以內、還沒有人動）

| 任務 | 句數 |
|---|---|
| The Olmic Rune | 7 |
| Cluck Cluck | 10 |
| Lost Tower | 10 |
| Pit of the Dead (Quest) | 12 |
| Clearing the Camps | 14 |
| The House of Twain (Quest) | 15 |
| A Grave Mistake | 16 |
| Pirate's Trove | 16 |
| Cook Assistant | 17 |
| Dwelling Walls | 17 |
| Tower of Ascension (Quest) | 18 |
| Stable Story | 19 |
| Enzan's Brother | 20 |

---

## 全部任務

「台詞」是 NPC 講的話，其餘是任務目標（顯示在追蹤器上）。

| 任務 | 檔案 | 句數 | 台詞 | 角色 | 進度 |
|---|---|---|---|---|---|
| A Grave Mistake | [`a-grave-mistake.json`](../src/main/resources/assets/wynnchayuan/translations/quest/a-grave-mistake.json) | 16 | 14 | 5 | — |
| A Headless History | [`a-headless-history.json`](../src/main/resources/assets/wynnchayuan/translations/quest/a-headless-history.json) | 178 | 165 | 7 | — |
| A Hunter's Calling | [`a-hunter-s-calling.json`](../src/main/resources/assets/wynnchayuan/translations/quest/a-hunter-s-calling.json) | 241 | 212 | 24 | — |
| A Journey Beyond | [`a-journey-beyond.json`](../src/main/resources/assets/wynnchayuan/translations/quest/a-journey-beyond.json) | 243 | 225 | 7 | — |
| A Journey Further | [`a-journey-further.json`](../src/main/resources/assets/wynnchayuan/translations/quest/a-journey-further.json) | 85 | 73 | 2 | — |
| A Journey Home | [`a-journey-home.json`](../src/main/resources/assets/wynnchayuan/translations/quest/a-journey-home.json) | 119 | 100 | 11 | — |
| A Marauder's Dues | [`a-marauder-s-dues.json`](../src/main/resources/assets/wynnchayuan/translations/quest/a-marauder-s-dues.json) | 58 | 51 | 4 | — |
| A New Beginning | [`a-new-beginning.json`](../src/main/resources/assets/wynnchayuan/translations/quest/a-new-beginning.json) | 289 | 276 | 13 | — |
| A Sandy Scandal | [`a-sandy-scandal.json`](../src/main/resources/assets/wynnchayuan/translations/quest/a-sandy-scandal.json) | 66 | 47 | 7 | — |
| Acquiring Credentials | [`acquiring-credentials.json`](../src/main/resources/assets/wynnchayuan/translations/quest/acquiring-credentials.json) | 92 | 81 | 10 | — |
| Aldorei's Secret Part I | [`aldorei-s-secret-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/quest/aldorei-s-secret-part-i.json) | 71 | 60 | 8 | — |
| Aldorei's Secret Part II | [`aldorei-s-secret-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/quest/aldorei-s-secret-part-ii.json) | 600 | 569 | 17 | — |
| All Roads to Peace | [`all-roads-to-peace.json`](../src/main/resources/assets/wynnchayuan/translations/quest/all-roads-to-peace.json) | 418 | 405 | 39 | — |
| An Iron Heart Part I | [`an-iron-heart-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/quest/an-iron-heart-part-i.json) | 24 | 20 | 3 | — |
| An Iron Heart Part II | [`an-iron-heart-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/quest/an-iron-heart-part-ii.json) | 61 | 53 | 7 | — |
| Apotheosis (Quest) | [`apotheosis-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/apotheosis-quest.json) | 709 | 690 | 26 | — |
| Arachnids' Ascent | [`arachnids-ascent.json`](../src/main/resources/assets/wynnchayuan/translations/quest/arachnids-ascent.json) | 111 | 105 | 7 | — |
| Beneath the Depths | [`beneath-the-depths.json`](../src/main/resources/assets/wynnchayuan/translations/quest/beneath-the-depths.json) | 42 | 36 | 3 | — |
| Beyond the Grave | [`beyond-the-grave.json`](../src/main/resources/assets/wynnchayuan/translations/quest/beyond-the-grave.json) | 186 | 180 | 3 | — |
| Blazing Retribution | [`blazing-retribution.json`](../src/main/resources/assets/wynnchayuan/translations/quest/blazing-retribution.json) | 42 | 35 | 4 | — |
| Bob's Lost Soul | [`bob-s-lost-soul.json`](../src/main/resources/assets/wynnchayuan/translations/quest/bob-s-lost-soul.json) | 75 | 65 | 6 | — |
| Brothers Return | [`brothers-return.json`](../src/main/resources/assets/wynnchayuan/translations/quest/brothers-return.json) | 93 | 79 | 7 | — |
| Burning Bonds | [`burning-bonds.json`](../src/main/resources/assets/wynnchayuan/translations/quest/burning-bonds.json) | 89 | 82 | 2 | — |
| Canyon Condor | [`canyon-condor.json`](../src/main/resources/assets/wynnchayuan/translations/quest/canyon-condor.json) | 42 | 36 | 3 | — |
| Celebrations in Smoke | [`celebrations-in-smoke.json`](../src/main/resources/assets/wynnchayuan/translations/quest/celebrations-in-smoke.json) | 528 | 509 | 14 | — |
| Clearing the Camps | [`clearing-the-camps.json`](../src/main/resources/assets/wynnchayuan/translations/quest/clearing-the-camps.json) | 14 | 11 | 1 | — |
| Cluck Cluck | [`cluck-cluck.json`](../src/main/resources/assets/wynnchayuan/translations/quest/cluck-cluck.json) | 10 | 8 | 1 | — |
| Cook Assistant | [`cook-assistant.json`](../src/main/resources/assets/wynnchayuan/translations/quest/cook-assistant.json) | 17 | 12 | 1 | — |
| Corrupted Betrayal | [`corrupted-betrayal.json`](../src/main/resources/assets/wynnchayuan/translations/quest/corrupted-betrayal.json) | 51 | 45 | 4 | — |
| Cowfusion | [`cowfusion.json`](../src/main/resources/assets/wynnchayuan/translations/quest/cowfusion.json) | 223 | 206 | 5 | — |
| Creeper Infiltration | [`creeper-infiltration.json`](../src/main/resources/assets/wynnchayuan/translations/quest/creeper-infiltration.json) | 44 | 38 | 1 | — |
| Crop Failure | [`crop-failure.json`](../src/main/resources/assets/wynnchayuan/translations/quest/crop-failure.json) | 44 | 39 | 2 | — |
| Dearly Departed | [`dearly-departed.json`](../src/main/resources/assets/wynnchayuan/translations/quest/dearly-departed.json) | 45 | 39 | 2 | — |
| Death Whistle (Quest) | [`death-whistle-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/death-whistle-quest.json) | 31 | 25 | 3 | — |
| Deja Vu | [`deja-vu.json`](../src/main/resources/assets/wynnchayuan/translations/quest/deja-vu.json) | 87 | 79 | 2 | — |
| Desperate Metal | [`desperate-metal.json`](../src/main/resources/assets/wynnchayuan/translations/quest/desperate-metal.json) | 37 | 32 | 4 | — |
| Dwarves and Doguns Part I | [`dwarves-and-doguns-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/quest/dwarves-and-doguns-part-i.json) | 122 | 107 | 18 | — |
| Dwarves and Doguns Part II | [`dwarves-and-doguns-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/quest/dwarves-and-doguns-part-ii.json) | 80 | 68 | 6 | — |
| Dwarves and Doguns Part III | [`dwarves-and-doguns-part-iii.json`](../src/main/resources/assets/wynnchayuan/translations/quest/dwarves-and-doguns-part-iii.json) | 75 | 65 | 5 | — |
| Dwarves and Doguns Part IV | [`dwarves-and-doguns-part-iv.json`](../src/main/resources/assets/wynnchayuan/translations/quest/dwarves-and-doguns-part-iv.json) | 65 | 57 | 7 | — |
| Dwelling Walls | [`dwelling-walls.json`](../src/main/resources/assets/wynnchayuan/translations/quest/dwelling-walls.json) | 17 | 13 | 1 | — |
| Echoes of Change | [`echoes-of-change.json`](../src/main/resources/assets/wynnchayuan/translations/quest/echoes-of-change.json) | 725 | 696 | 26 | — |
| Elemental Exercise | [`elemental-exercise.json`](../src/main/resources/assets/wynnchayuan/translations/quest/elemental-exercise.json) | 65 | 56 | 4 | — |
| Ensemble of Hope | [`ensemble-of-hope.json`](../src/main/resources/assets/wynnchayuan/translations/quest/ensemble-of-hope.json) | 765 | 743 | 34 | — |
| Enter the Dojo | [`enter-the-dojo.json`](../src/main/resources/assets/wynnchayuan/translations/quest/enter-the-dojo.json) | 35 | 27 | 1 | — |
| Enzan's Brother | [`enzan-s-brother.json`](../src/main/resources/assets/wynnchayuan/translations/quest/enzan-s-brother.json) | 20 | 17 | 2 | — |
| Fallen Delivery | [`fallen-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/quest/fallen-delivery.json) | 65 | 55 | 5 | — |
| Fantastic Voyage | [`fantastic-voyage.json`](../src/main/resources/assets/wynnchayuan/translations/quest/fantastic-voyage.json) | 283 | 264 | 5 | — |
| Fate of the Fallen | [`fate-of-the-fallen.json`](../src/main/resources/assets/wynnchayuan/translations/quest/fate-of-the-fallen.json) | 70 | 61 | 5 | — |
| Finding the Light | [`finding-the-light.json`](../src/main/resources/assets/wynnchayuan/translations/quest/finding-the-light.json) | 99 | 83 | 3 | — |
| Flight in Distress | [`flight-in-distress.json`](../src/main/resources/assets/wynnchayuan/translations/quest/flight-in-distress.json) | 103 | 85 | 21 | — |
| Forbidden Prison (Quest) | [`forbidden-prison-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/forbidden-prison-quest.json) | 117 | 101 | 10 | — |
| From the Bottom | [`from-the-bottom.json`](../src/main/resources/assets/wynnchayuan/translations/quest/from-the-bottom.json) | 90 | 81 | 8 | — |
| From the Mountains | [`from-the-mountains.json`](../src/main/resources/assets/wynnchayuan/translations/quest/from-the-mountains.json) | 63 | 54 | 2 | — |
| Frost Bite | [`frost-bite.json`](../src/main/resources/assets/wynnchayuan/translations/quest/frost-bite.json) | 69 | 60 | 2 | — |
| General's Orders | [`general-s-orders.json`](../src/main/resources/assets/wynnchayuan/translations/quest/general-s-orders.json) | 114 | 102 | 10 | — |
| Grand Youth | [`grand-youth.json`](../src/main/resources/assets/wynnchayuan/translations/quest/grand-youth.json) | 39 | 33 | 4 | — |
| Green Gloop | [`green-gloop.json`](../src/main/resources/assets/wynnchayuan/translations/quest/green-gloop.json) | 32 | 28 | 2 | — |
| Haven Antiquity | [`haven-antiquity.json`](../src/main/resources/assets/wynnchayuan/translations/quest/haven-antiquity.json) | 54 | 46 | 3 | — |
| Heart of Llevigar | [`heart-of-llevigar.json`](../src/main/resources/assets/wynnchayuan/translations/quest/heart-of-llevigar.json) | 31 | 22 | 3 | — |
| Hollow Serenity | [`hollow-serenity.json`](../src/main/resources/assets/wynnchayuan/translations/quest/hollow-serenity.json) | 295 | 276 | 16 | — |
| Hunger of the Gerts Part I | [`hunger-of-the-gerts-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/quest/hunger-of-the-gerts-part-i.json) | 112 | 98 | 8 | — |
| Hunger of the Gerts Part II | [`hunger-of-the-gerts-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/quest/hunger-of-the-gerts-part-ii.json) | 76 | 65 | 7 | — |
| Ice Nations | [`ice-nations.json`](../src/main/resources/assets/wynnchayuan/translations/quest/ice-nations.json) | 26 | 22 | 2 | — |
| Infested Plants | [`infested-plants.json`](../src/main/resources/assets/wynnchayuan/translations/quest/infested-plants.json) | 33 | 28 | 4 | — |
| Jungle Fever | [`jungle-fever.json`](../src/main/resources/assets/wynnchayuan/translations/quest/jungle-fever.json) | 32 | 26 | 3 | — |
| King's Recruit | [`king-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/quest/king-s-recruit.json) | 124 | 102 | 8 | — |
| Kingdom of Sand | [`kingdom-of-sand.json`](../src/main/resources/assets/wynnchayuan/translations/quest/kingdom-of-sand.json) | 60 | 51 | 8 | — |
| Lava Springs | [`lava-springs.json`](../src/main/resources/assets/wynnchayuan/translations/quest/lava-springs.json) | 41 | 33 | 3 | — |
| Lazarus Pit (Quest) | [`lazarus-pit-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/lazarus-pit-quest.json) | 76 | 57 | 7 | — |
| Lexdale Witch Trials | [`lexdale-witch-trials.json`](../src/main/resources/assets/wynnchayuan/translations/quest/lexdale-witch-trials.json) | 33 | 25 | 3 | — |
| Lost Royalty | [`lost-royalty.json`](../src/main/resources/assets/wynnchayuan/translations/quest/lost-royalty.json) | 26 | 20 | 2 | — |
| Lost Soles | [`lost-soles.json`](../src/main/resources/assets/wynnchayuan/translations/quest/lost-soles.json) | 61 | 51 | 7 | — |
| Lost Tower | [`lost-tower.json`](../src/main/resources/assets/wynnchayuan/translations/quest/lost-tower.json) | 10 | 6 | 1 | — |
| Lost in the Jungle | [`lost-in-the-jungle.json`](../src/main/resources/assets/wynnchayuan/translations/quest/lost-in-the-jungle.json) | 32 | 27 | 3 | — |
| Maltic's Well | [`maltic-s-well.json`](../src/main/resources/assets/wynnchayuan/translations/quest/maltic-s-well.json) | 23 | 21 | 3 | — |
| Master Piece | [`master-piece.json`](../src/main/resources/assets/wynnchayuan/translations/quest/master-piece.json) | 38 | 31 | 1 | — |
| Meaningful Holiday | [`meaningful-holiday.json`](../src/main/resources/assets/wynnchayuan/translations/quest/meaningful-holiday.json) | 108 | 93 | 10 | — |
| Memory Paranoia | [`memory-paranoia.json`](../src/main/resources/assets/wynnchayuan/translations/quest/memory-paranoia.json) | 105 | 80 | 13 | — |
| Misadventure on the Sea | [`misadventure-on-the-sea.json`](../src/main/resources/assets/wynnchayuan/translations/quest/misadventure-on-the-sea.json) | 132 | 124 | 6 | — |
| Mixed Feelings | [`mixed-feelings.json`](../src/main/resources/assets/wynnchayuan/translations/quest/mixed-feelings.json) | 76 | 67 | 4 | — |
| Murder Mystery | [`murder-mystery.json`](../src/main/resources/assets/wynnchayuan/translations/quest/murder-mystery.json) | 57 | 51 | 5 | — |
| Mushroom Man | [`mushroom-man.json`](../src/main/resources/assets/wynnchayuan/translations/quest/mushroom-man.json) | 75 | 69 | 6 | — |
| Off the Rails | [`off-the-rails.json`](../src/main/resources/assets/wynnchayuan/translations/quest/off-the-rails.json) | 791 | 783 | 12 | — |
| One Thousand Meters Under | [`one-thousand-meters-under.json`](../src/main/resources/assets/wynnchayuan/translations/quest/one-thousand-meters-under.json) | 105 | 86 | 10 | — |
| Out of my Mind | [`out-of-my-mind.json`](../src/main/resources/assets/wynnchayuan/translations/quest/out-of-my-mind.json) | 60 | 54 | 4 | — |
| Overture to Despair | [`overture-to-despair.json`](../src/main/resources/assets/wynnchayuan/translations/quest/overture-to-despair.json) | 603 | 582 | 26 | — |
| Pirate's Trove | [`pirate-s-trove.json`](../src/main/resources/assets/wynnchayuan/translations/quest/pirate-s-trove.json) | 16 | 11 | 1 | — |
| Pit of the Dead (Quest) | [`pit-of-the-dead-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/pit-of-the-dead-quest.json) | 12 | 9 | 1 | — |
| Point of No Return | [`point-of-no-return.json`](../src/main/resources/assets/wynnchayuan/translations/quest/point-of-no-return.json) | 42 | 30 | 3 | — |
| Potion Making | [`potion-making.json`](../src/main/resources/assets/wynnchayuan/translations/quest/potion-making.json) | 31 | 27 | 2 | — |
| Purple and Blue | [`purple-and-blue.json`](../src/main/resources/assets/wynnchayuan/translations/quest/purple-and-blue.json) | 84 | 76 | 3 | — |
| Queen's Recruit | [`queen-s-recruit.json`](../src/main/resources/assets/wynnchayuan/translations/quest/queen-s-recruit.json) | 673 | 647 | 20 | — |
| Recipe For Disaster | [`recipe-for-disaster.json`](../src/main/resources/assets/wynnchayuan/translations/quest/recipe-for-disaster.json) | 139 | 130 | 10 | — |
| Reclaiming the House | [`reclaiming-the-house.json`](../src/main/resources/assets/wynnchayuan/translations/quest/reclaiming-the-house.json) | 59 | 47 | 4 | — |
| Recover the Past | [`recover-the-past.json`](../src/main/resources/assets/wynnchayuan/translations/quest/recover-the-past.json) | 264 | 248 | 11 | — |
| Redbeard's Booty | [`redbeard-s-booty.json`](../src/main/resources/assets/wynnchayuan/translations/quest/redbeard-s-booty.json) | 42 | 36 | 4 | — |
| Reincarnation | [`reincarnation.json`](../src/main/resources/assets/wynnchayuan/translations/quest/reincarnation.json) | 33 | 29 | 2 | — |
| Revelations in Fall | [`revelations-in-fall.json`](../src/main/resources/assets/wynnchayuan/translations/quest/revelations-in-fall.json) | 304 | 291 | 10 | — |
| Rise of the Quartron | [`rise-of-the-quartron.json`](../src/main/resources/assets/wynnchayuan/translations/quest/rise-of-the-quartron.json) | 86 | 76 | 7 | — |
| Royal Trials | [`royal-trials.json`](../src/main/resources/assets/wynnchayuan/translations/quest/royal-trials.json) | 85 | 66 | 7 | — |
| Shattered Minds | [`shattered-minds.json`](../src/main/resources/assets/wynnchayuan/translations/quest/shattered-minds.json) | 51 | 45 | 7 | — |
| Shrouded in Mist | [`shrouded-in-mist.json`](../src/main/resources/assets/wynnchayuan/translations/quest/shrouded-in-mist.json) | 299 | 281 | 14 | — |
| Solidarity of Steel | [`solidarity-of-steel.json`](../src/main/resources/assets/wynnchayuan/translations/quest/solidarity-of-steel.json) | 508 | 486 | 17 | — |
| Stable Story | [`stable-story.json`](../src/main/resources/assets/wynnchayuan/translations/quest/stable-story.json) | 19 | 17 | 1 | — |
| Star Thief | [`star-thief.json`](../src/main/resources/assets/wynnchayuan/translations/quest/star-thief.json) | 22 | 15 | 3 | — |
| Supply and Delivery | [`supply-and-delivery.json`](../src/main/resources/assets/wynnchayuan/translations/quest/supply-and-delivery.json) | 76 | 70 | 3 | — |
| Taking the Tower | [`taking-the-tower.json`](../src/main/resources/assets/wynnchayuan/translations/quest/taking-the-tower.json) | 43 | 36 | 3 | — |
| Taproot | [`taproot.json`](../src/main/resources/assets/wynnchayuan/translations/quest/taproot.json) | 103 | 91 | 4 | — |
| Temple of the Legends (Quest) | [`temple-of-the-legends-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/temple-of-the-legends-quest.json) | 137 | 124 | 4 | — |
| Tempo Town Trouble | [`tempo-town-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/quest/tempo-town-trouble.json) | 57 | 52 | 4 | — |
| The Bigger Picture | [`the-bigger-picture.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-bigger-picture.json) | 25 | 18 | 1 | — |
| The Breaking Point | [`the-breaking-point.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-breaking-point.json) | 281 | 262 | 20 | — |
| The Canary Calls | [`the-canary-calls.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-canary-calls.json) | 220 | 211 | 9 | — |
| The Canyon Guides | [`the-canyon-guides.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-canyon-guides.json) | 63 | 56 | 2 | — |
| The Corrupted Village | [`the-corrupted-village.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-corrupted-village.json) | 56 | 46 | 4 | — |
| The Cursed One | [`the-cursed-one.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-cursed-one.json) | 497 | 479 | 20 | — |
| The Dark Descent | [`the-dark-descent.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-dark-descent.json) | 49 | 43 | 5 | — |
| The Envoy Part I | [`the-envoy-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-envoy-part-i.json) | 94 | 83 | 7 | — |
| The Envoy Part II | [`the-envoy-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-envoy-part-ii.json) | 84 | 71 | 12 | — |
| The Feathers Fly Part I | [`the-feathers-fly-part-i.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-feathers-fly-part-i.json) | 210 | 198 | 6 | — |
| The Feathers Fly Part II | [`the-feathers-fly-part-ii.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-feathers-fly-part-ii.json) | 1286 | 1272 | 19 | — |
| The Hero of Gavel | [`the-hero-of-gavel.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-hero-of-gavel.json) | 196 | 177 | 11 | — |
| The Hidden City | [`the-hidden-city.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-hidden-city.json) | 75 | 59 | 13 | — |
| The House of Twain (Quest) | [`the-house-of-twain-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-house-of-twain-quest.json) | 15 | 13 | 2 | — |
| The Lost | [`the-lost.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-lost.json) | 48 | 45 | 4 | — |
| The Maiden Tower | [`the-maiden-tower.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-maiden-tower.json) | 36 | 32 | 2 | — |
| The Mercenary | [`the-mercenary.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-mercenary.json) | 141 | 131 | 8 | — |
| The Missing Piece | [`the-missing-piece.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-missing-piece.json) | 582 | 513 | 15 | — |
| The Olmic Rune | [`the-olmic-rune.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-olmic-rune.json) | 7 | 0 | 0 | — |
| The Order of the Grook | [`the-order-of-the-grook.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-order-of-the-grook.json) | 217 | 205 | 9 | — |
| The Passage (Quest) | [`the-passage-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-passage-quest.json) | 24 | 21 | 2 | — |
| The Price of Ingenuity | [`the-price-of-ingenuity.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-price-of-ingenuity.json) | 584 | 565 | 29 | — |
| The Qira Hive (Quest) | [`the-qira-hive-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-qira-hive-quest.json) | 63 | 63 | 7 | — |
| The Realm of Light (Quest) | [`the-realm-of-light-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-realm-of-light-quest.json) | 92 | 79 | 1 | — |
| The Scarred Springs | [`the-scarred-springs.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-scarred-springs.json) | 137 | 124 | 10 | — |
| The Sewers of Ragni | [`the-sewers-of-ragni.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-sewers-of-ragni.json) | 31 | 25 | 1 | — |
| The Shadow of the Beast | [`the-shadow-of-the-beast.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-shadow-of-the-beast.json) | 87 | 75 | 4 | — |
| The Strong Survive | [`the-strong-survive.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-strong-survive.json) | 665 | 632 | 47 | — |
| The Thanos Depository | [`the-thanos-depository.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-thanos-depository.json) | 42 | 33 | 2 | — |
| The Ultimate Weapon | [`the-ultimate-weapon.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-ultimate-weapon.json) | 76 | 64 | 8 | — |
| The Worm Holes | [`the-worm-holes.json`](../src/main/resources/assets/wynnchayuan/translations/quest/the-worm-holes.json) | 85 | 79 | 3 | — |
| Through the Pipes | [`through-the-pipes.json`](../src/main/resources/assets/wynnchayuan/translations/quest/through-the-pipes.json) | 191 | 178 | 2 | — |
| Tower of Ascension (Quest) | [`tower-of-ascension-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/tower-of-ascension-quest.json) | 18 | 18 | 2 | — |
| Tribal Aggression | [`tribal-aggression.json`](../src/main/resources/assets/wynnchayuan/translations/quest/tribal-aggression.json) | 24 | 20 | 2 | — |
| Troubled Tribesmen | [`troubled-tribesmen.json`](../src/main/resources/assets/wynnchayuan/translations/quest/troubled-tribesmen.json) | 26 | 20 | 3 | — |
| True Colours | [`true-colours.json`](../src/main/resources/assets/wynnchayuan/translations/quest/true-colours.json) | 541 | 522 | 11 | — |
| Tunnel Trouble | [`tunnel-trouble.json`](../src/main/resources/assets/wynnchayuan/translations/quest/tunnel-trouble.json) | 63 | 52 | 6 | — |
| Underice | [`underice.json`](../src/main/resources/assets/wynnchayuan/translations/quest/underice.json) | 56 | 42 | 5 | — |
| Undersupply | [`undersupply.json`](../src/main/resources/assets/wynnchayuan/translations/quest/undersupply.json) | 125 | 120 | 10 | — |
| Underwater | [`underwater.json`](../src/main/resources/assets/wynnchayuan/translations/quest/underwater.json) | 24 | 19 | 2 | — |
| Wrath of the Mummy | [`wrath-of-the-mummy.json`](../src/main/resources/assets/wynnchayuan/translations/quest/wrath-of-the-mummy.json) | 33 | 26 | 3 | — |
| WynnExcavation Site A | [`wynnexcavation-site-a.json`](../src/main/resources/assets/wynnchayuan/translations/quest/wynnexcavation-site-a.json) | 28 | 22 | 2 | — |
| WynnExcavation Site B | [`wynnexcavation-site-b.json`](../src/main/resources/assets/wynnchayuan/translations/quest/wynnexcavation-site-b.json) | 28 | 23 | 4 | — |
| WynnExcavation Site C | [`wynnexcavation-site-c.json`](../src/main/resources/assets/wynnchayuan/translations/quest/wynnexcavation-site-c.json) | 42 | 33 | 5 | — |
| WynnExcavation Site D | [`wynnexcavation-site-d.json`](../src/main/resources/assets/wynnchayuan/translations/quest/wynnexcavation-site-d.json) | 97 | 68 | 15 | — |
| Zhight Island (Quest) | [`zhight-island-quest.json`](../src/main/resources/assets/wynnchayuan/translations/quest/zhight-island-quest.json) | 50 | 45 | 4 | — |

---

這份清單由 `tools/quest-index.py` 產生，翻譯有進度就重跑一次。
