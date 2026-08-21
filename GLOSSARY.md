# 專有名詞對照表

翻譯時碰到不確定的詞就查這裡，**不要自己另外想一個**。
同一個詞在不同檔案翻成兩種說法，玩家會以為是兩個不同的東西。

這份表由 `tools/validate.py` 自動檢查：譯文檔裡把表中的詞翻成別的說法，
送 PR 時 CI 會提出警告。**要改某個詞的譯法，先改這裡**，讓所有人一起跟著改。

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
| Mage/Dark Wizard | 法師/黑巫師 |
| Archer/Hunter | 弓手/獵人 |
| Warrior/Knight | 戰士/騎士 |
| Assassin/Ninja | 刺客/忍者 |
| Shaman/Skyseer | 薩滿/天視者 |
| Spears | 矛 |
| Daggers | 匕首 |
| Bows | 弓 |
| Wands | 魔杖 |
| Reliks | 法器 |

職業基礎技能翻譯如下：(待補)
| 順序 | 法師 | 弓手 | 戰士 | 刺客 | 薩滿 |
|---|---|---|---|---|---|
| 1st | Heal | Arrow Storm | Bash | Spin Attack | Totem |
| 2nd | Teleport | Escape | Charge | Dash | Haul |
| 3rd | Meteor | Arrow Bomb | Uppercut | Multihit | Aura |
| 4th | Ice Snake | Arrow Shield | War Scream | Smoke Bomb | Uproot |

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
| Lootrun | Lootrun | **不翻** |
| Dungeon | 地城 |  |
| Raid | 團隊副本 | 此指後期需要 4 人加入的副本，非指突襲 |
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

## 一律保留原文

翻了會跟其他玩家對不上話，或社群本來就講英文。

- **地名**（Ragni、Detlas、Troms…）——用 `{p}` 佔位符自動處理，共 137 個
- **裝備與物品名稱**——專有名詞，翻了對不上 wiki 與交易市場
- **技能名稱**——建議保留英文，真的需要就附註：`Meteor（隕石）`
- **Lootrun**、**Raid**、**Guild** 等社群通用詞

---

## 主線角色的語氣

翻對話時<b>語氣比字面重要</b>。同一句話，該角色會怎麼講？

| 角色 | 性格 | 語氣 |
|---|---|---|
| Aledar | 衝動、熱血、莽撞 | 短句、驚嘆號多，動不動就先衝出去（「那還等什麼？」） |
| Tasim | 謹慎、愛擔心、想得多 | 常在後面喊「等等」，句子有停頓與省略號 |
| King of Ragni | 威嚴、正式 | 書面語（「就此別過」「想必路上頗為驚險」） |
| Guard / Soldier / Lieutenant | 職務在身 | 乾脆、簡短，不寒暄 |
| The Cook | 碎念的抱怨鬼 | 一直在訴苦（「我真不敢相信我怎麼這麼倒楣」） |
| Nohno | 養雞養到走火入魔的島民 | 話多、自來熟，最後崩潰 |
| Merloni | 說故事的人 | 沉、有懸念 |
| Captain Kymer | 軍官 | 乾脆，帶軍中的隨口調侃 |

已翻的任務可以直接當範本：`King's Recruit`（開場，124 句）、`Cook Assistant`、
`Clearing the Camps`。

---

## 有雙關或典故的

Wynncraft 的任務名稱與台詞常常一語雙關。**能做出來就做出來**，做不出來就在
這裡註明原文在玩什麼——內容的語氣才接得上。

| 原文 | 譯文 | 原文在玩什麼 |
|---|---|---|
| A Grave Mistake | 致命的失足 | `grave` 同時是「嚴重的」與「墳墓」。玩家最後真的掉進一座墳墓，「失足」剛好兩層都接住 |
| Cluck Cluck | 咕咕咯咯 | 雞叫聲。中文的擬聲詞也是疊字，四個字比兩個字更像在學雞叫 |

> 破碎、殘缺的台詞（`.;I. ,told;.`）是**刻意**的，那是氣氛的一部分。
> 譯文要保留那種殘缺感，不要補成通順的句子。

---

## 想改某個詞？

送 PR 改這個檔案，說明為什麼。改完之後**同時把相關譯文檔一起改掉**，
否則 CI 會警告表與實際譯文不一致。
