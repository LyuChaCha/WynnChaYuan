# 專有名詞對照表

> 這裡講**怎麼翻**。要先弄懂**是什麼**，看 [世界觀與主線](docs/world-and-story.md)。


為了譯名的標準話語一致性，專有名詞翻譯時請遵循下列表格翻譯，碰到不確定的詞就查這裡，**不要自己另外想一個**。
這份表由 `tools/validate.py` 自動檢查：譯文檔裡把表中的詞翻成別的說法，
送 PR 時 CI 會提出警告。**要改某個詞的譯法，先改這裡**，讓所有人一起跟著改。
另外，請善用 Ctrl+F 查詢譯名。(我真的不知道該怎麼分類救我)

## 製作與採集

| 原文 | 譯文 |
|---|---|
| Tailoring | 裁縫 |
| Jeweling | 珠寶 |
| Weaponsmithing | 鍛兵 |
| Armouring | 鑄甲 |
| Woodworking | 木工 |
| Cooking | 烹飪 |
| Alchemism | 鍊金 |
| Scribing | 抄寫 |
|  |  |
| Fishing | 釣魚 |
| Woodcutting | 伐木 |
| Mining | 採礦 |
| Farming | 農耕 |

## 職業內容相關

職業名一律「**主職/造型**」，中間用半形斜線。

| 原文 | 譯文 |
|---|---|
| Mage/Dark Wizard | 法師/闇導士 |
| Archer/Hunter | 弓手/獵人 |
| Warrior/Knight | 戰士/騎士 |
| Assassin/Ninja | 刺客/忍者 |
| Shaman/Skyseer | 薩滿/觀星者 |
| Spears | 矛 |
| Daggers | 匕首 |
| Bows | 弓 |
| Wands | 魔杖 |
| Reliks | 法器 |

職業基礎技能翻譯如下：(待補)
| 順序 | 法師 | 弓手 | 戰士 | 刺客 | 薩滿 |
|---|---|---|---|---|---|
| 1st | 治療 | 箭矢風暴 | 重擊 | 迴刃斬 | 圖騰 |
| 2nd | 傳送 | 逃脫 | 衝鋒 | 騰躍行 | Haul |
| 3rd | 隕石 | 箭矢炸彈 | 上劈 | 連環斬 | Aura |
| 4th | 冰蛇 | 箭矢護盾 | 戰吼 | 煙霧彈 | Uproot |

## 攻擊速度

| 原文 | 譯文 |
|---|---|
| Super Fast | 超快 |
| Very Fast | 很快 |
| Fast | 快速 |
| Normal | 普通 |
| Slow | 緩慢 |
| Very Slow | 很慢 |
| Super Slow | 超慢 |

## 遊戲系統

| 原文 | 譯文 | 說明 |
|---|---|---|
| Crafting Level | 製作等級 | |
| Combat Level | 戰鬥等級 | |
| Class Type | 職業類型 | |
| Material | 原料 | 製作用的原料，和 Ingreident 一詞相對 |
| Ingredient | 素材 | 製作用的素材，和 Material 一詞相對 |
| Ingredient Pouch | 素材袋 | |
| Emerald Pouch | 綠寶石袋 | |
| Mastery Tome | 精通書卷 | Tome 為書卷 |
| Liquid Emeralds | 液態綠寶石 | |
| Item Identifier | 物品鑑定師 | NPC 職業，不是「鑑定道具」 |
| Lootrun | Lootrun | |
| Dungeon | 地城 |  |
| Raid | 討伐戰 | 此指後期需要 4 人加入的副本。**一般語意的 raid／assault 仍用「突襲」**——任務對話裡的「突襲一座腐化據點」、「獸人突襲」不是這個 |
| Scale | 適性 | 此指裝備詞條經加權計算後得出評估該物品是否良好的值 |

## 詞條相關：
Wynncraft 中運用詞條的部分很多，遂記錄之：

### Skill Point

| 原文 | 譯文 | 備註 |
|---|---|---|
| Skill Point | 屬性點 | Ability Point 為技能點，兩者務必分清楚 |
| Strength | 力量 | |
| Dexterity | 靈巧 | |
| Intelligence | 智慧 | |
| Defense | 防禦 | |
| Agility | 敏捷 | |

### 攻擊、防禦屬性相關

Wynncraft 中的攻擊詞條事實上是種組合式的詞條。
其以 屬性 + 作用方式 + Damage + 作用乘區(Raw/%) 組合而成，前兩區可為空、Damage 一律譯為傷害。
舉例而言，Elemental Spell Damage % 應翻譯作「元素法術傷害百分比」。
另外，防禦屬性亦遵循下列原則，如 Elemental Defence 譯為 「元素防禦」。

---

屬性：
| 原文 | 譯文 | 備註 |
|---|---|---|
| Neutral | 無屬性 | 此僅於詞條，若技能樹中出現 Neutral Damage 則應譯作基礎傷害 |
| Elemental | 元素 | |
| Earth | 地 | |
| Thunder | 雷 | |
| Water | 水 | |
| Fire | 火 | |
| Air | 風 | |

---

作用方式：
| 原文 | 譯文 | 備註 |
|---|---|---|
| Spell | 法術 |  |
| Main Attack | 普攻 |  |

---

作用乘區：
| 原文 | 譯文 | 備註 |
|---|---|---|
| % | 百分比 |  |
| Raw | 值 |  |

---

不屬於該分類方法的完整詞條展示於下：
| 原文 | 譯文 | 備註 |
|---|---|---|
| Attack Speed | 攻擊速度 | |
| Main Attack Range | 普攻距離 | |
| Knockback | 擊退效果 | |
| Critical Damage Bonus | 暴擊傷害百分比 | 此詞條沒有 Raw，故置於此 |

### 生命與魔力

| 原文 | 譯文 | 備註 |
|---|---|---|
| Health | 生命 |  |
| Raw Health Regen | 生命回復 |  |
| Health Regen % | 生命回復百分比 | 遵循上部分的原則 |
| Life Steal |  生命竊取 |  |
| Healing Efficiency | 治療效率 |  |
| Mana Regen | 魔力回復 |  |
| Mana Steal | 魔力竊取 |  |
| Max Mana | 最大魔力 |  |
| Spell Cost % | 魔力消耗百分比 | 若為序數則譯作第X技能(X表小寫中文)，若為技能則參閱上文技能處 |
| Spell Cost Raw | 魔力消耗 | 同上 |

### 被動詞條

| 原文 | 譯文 | 備註 |
|---|---|---|
| Exploding | 爆炸 |  |
| Poison | 中毒 |  |
| Thorns | 近戰反傷 |  |
| Reflection | 遠程反傷 |  |

### 移動相關

| 原文 | 譯文 | 備註 |
|---|---|---|
| Walk Speed | 移動速度 |  |
| Sprint | 耐力 |  |
| Sprint Regen | 耐力回復 |  |
| Jump Height | 跳躍高度 |  |

### 經驗與採集

| 原文 | 譯文 | 備註 |
|---|---|---|
| Loot (Bonus) | 寶物加成 |  |
| Loot Quality | 寶物品質 |  |
| Stealing | 竊取綠寶石 |  |
| XP Bonus | 經驗加成 |  |
| Gather XP Bonus | 採集經驗 |  |
| Gather Speed | 採集速度 |  |

## 其他

| 原文 | 譯文 | 備註 |
|---|---|---|
| Part x | 第X部 | X 用中文小寫(一、二、三)，且與主要任務名字空一格半形空格|

---

## 世界觀相關

| 原文 | 譯文 | 備註 |
|---|---|---|
| Decay | 黯蝕 | 指 Gavel 那場災厄。一般語意的腐朽／枯萎（`Decaying Heart`、`fallen into decay`）與機制上的層數衰減（`Distortion decays`）不適用 |
| Corruption | 腐敗 | **未確定** |
|  |  |

## Fruma 篇（主線）

`Queen's Recruit` 之後的主線都會用到這幾個詞，先在這裡定下來。

| 原文 | 譯文 | 備註 |
|---|---|---|
| Sovereign | 統領 | **(待討論)** |
| Her Majesty | 女王陛下 | 說話者名稱用全稱，句中稱呼用「陛下」 |
| Gendarme Soldier | 憲兵 | `Gendarme` 本來就是憲兵，需與其做區分。|
| Gendarme Commander | 憲兵指揮官 | |
| Aelumia Guard | Aelumia 衛兵 | |
| the Cursed | 受詛者 | **(待討論)** |
| blight | 災厄 | Fruma 對牆外世界的稱呼 **(待討論)** |
| Representative | 接待人員 | |
| Seaskipper Captain | 渡船船長 | **(待討論)** |
| the Morning Star | 晨星 | Majin 的稱號 **(待討論)** |
| the Adjustor | 調律器 | Fruma 抹除記憶用的裝置 **(待確定、討論)** |
| Inquisitor | 審訊官 | 執行抹除的人、ARG 中去外界偵查訊息的人 **(待確定、討論)** |
| memory restorification orb | 記憶復原化球 | Picard 自己造的字（`Restorification` 不是英文），中文也要留一點造字感，別修成通順的「記憶復原球」**(待討論)** |
| Royal Captain / Royal Guard | 皇家隊長／皇家衛兵 | Fruma 的王室部隊 |
| House Thalas / Nasin | Thalas 家 / Nasin 家 |  |
| the Cursed One | 受詛者 | 同 `the Cursed`。 **(待討論)** |
| Leaves in the Wind | 風中之葉 | Syndra 的反抗組織。 **(待討論)** |
| young leaf | 小葉子 | Mora 對新成員的稱呼。 **(待討論)** |
| identification papers | 身分文件 |  |
| passport | 通行證 | Syndra 弄來的那份，跟 `papers` 分開 |
| the Driver | （未定，暫留原文） | 檔案裡「由 the Driver 帶往本地」。像職稱又像代號，**待討論** |
| the Canopy | 樹冠 | Fruma 的另一支反抗組織，住在巨樹上，內部以梟／鴿互稱。與 `Leaves in the Wind` 同一種命名法。 **(待討論)** |
| Steel Feather | 鋼羽 | Quill 那一支。 **(待討論)** |
| changeling | 換形者 | 語料裡本來就這樣譯，補進表裡 |
| talisman | 護符 | 語料裡本來就這樣譯，補進表裡 |
| Gravity's Monolith | 重力方尖碑 | Deraj 監獄的別稱，敘述裡當普通名詞用，不是地名 **(待討論)** |
| Warden | 典獄長 | Deraj 監獄的 Oru。與 boss 稱號 `Warden of Wisdom`（智慧的看守者）分開 **(待討論)** |
| recruit（Fruma 軍方） | 新兵 | Tericen 訓練營。公會階級的 `Recruit` 是「新成員」，兩者不同 **(待討論)** |
| Recruiter（Fruma 軍方） | 招募官 | 公會階級的 `Recruiter` 是「招募者」（見 `gui.json`），兩者不同 |
| Instructor | 教官 |  |
| Commander | 指揮官 | 與既有的「憲兵指揮官」一致 |
| Captain | 隊長 | 與既有的「皇家隊長」一致 |
| Wings division | 羽翼部隊 | 權限最高的那一支，憲兵屬於它 **(待討論)** |
| Constable division | 警備部隊 | 權限高於地區部隊、低於羽翼部隊 **(待討論)** |
| estoc | 刺劍 | Tericen 的木製訓練武器 |
| the Subway | 地鐵 | Fruma 的地下運輸系統 |
| Queen's Line | 女王線 | 地鐵的路線名 |
| Industrial Sector Loop | 工業區環線 | 通勤用的那條，與女王線分開 |
| carriage（列車） | 車廂 | 馬拉的 `carriage` 仍是「馬車」，兩者不同 |
| attendant | 車勤員 | 列車上的服務人員 |
| Ticket Collector | 驗票員 | |
| Marshal | 元帥 | Majin 的軍職，在將軍之上 |
| General | 將軍 | 每支部隊各有一位 |
| Highlander | 高地人 | Highlands 的居民 |
| Aelumia University | Aelumia 大學 | 分為秩序、水晶、繁花、星辰四個學院 |
| Blossom House | 繁花學院 | 大學裡的學院，`House` 在此不是家族 |
| Blossoming Blades | 綻放之刃 | 反抗組織，Cemor 所屬 **(待討論)** |
| Toxic Thistle | 劇毒薊 | Cemor 的代號 **(待討論)** |
| Penitence District | （保留原文） | 不在 places.json 裡，所以不會變成 {p}。其他區都在表上，這像是漏了 **(待確認)** |
| Spymaster | 諜報總管 | 與陛下一起主持「那個計畫」的人 **(待討論)** |
| charm | 符咒 | Nasin 家那枚讓 Zhiraok 復活的。與 `talisman`（護符）分開 **(待討論)** |
| blaster | 爆能槍 | Yulie 的武器 |
| Syndra the Lioness | 母獅 Syndra | 女王對她的稱呼 **(待討論)** |
**請注意，這裡每一個單字都待討論**

---

## 一律保留原文

翻了會跟其他玩家對不上話，或社群本來就講英文。

- **地名**（Ragni、Detlas、Troms…）——用 `{p}` 佔位符自動處理，共 137 個
- **裝備與物品名稱**——專有名詞，翻了對不上 wiki 與交易市場
- **Lootrun**、**Guild** 等社群通用詞（`Raid` 不在此列，見上面的對照表）


## 想改某個詞？

送 PR 改這個檔案，說明為什麼。改完之後**同時把相關譯文檔一起改掉**，
否則 CI 會警告表與實際譯文不一致。
