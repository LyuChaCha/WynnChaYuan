# 專有名詞對照表

翻譯時碰到不確定的詞就查這裡，**不要自己另外想一個**。
同一個詞在不同檔案翻成兩種說法，玩家會以為是兩個不同的東西。

這份表由 `tools/validate.py` 自動檢查：譯文檔裡把表中的詞翻成別的說法，
送 PR 時 CI 會提出警告。**要改某個詞的譯法，先改這裡**，讓所有人一起跟著改。

新增詞條的門檻：**「照字面翻會翻錯」或「翻了反而看不懂」**。
`Health` → `生命` 這種一看就懂的不用寫進來，只會讓表變長沒人看。

---

## 容易翻錯的

這些照字面翻是錯的，Wynncraft 的用法跟一般英文不同。

此外，請注意若一個詞條有 % 則原樣加到後面，如 "Damage %" -> "傷害 %"
若有 Raw 則不翻，如 "Damage" -> "傷害"

| 原文 | 譯文 | 為什麼 |
|---|---|---|
| Reflection | 遠程反傷 | **不是**「反射」。這是被遠程攻擊時反彈傷害 |
| Thorns | 近戰反傷 | **不是**「荊棘」。與 Reflection 成對，這個是近戰 |
| Exploding | 爆炸 | 攻擊時造成範圍爆炸的機率，不是「正在爆炸」 |
| Scale | 適性 | Wynncraft 的裝備評分術語，不是「規模／比例」 |
| Loot(Bonus) | 寶物加成 | 影響掉落**掉落率**，不是掉落品質 |
| Sprint Regen | 體力回復 | 回復的是奔跑用的體力，不是生命 |
| Healing Efficiency | 治療效率 | 技能和 Major ID 的治療效率 |
| Stealing | 竊取綠寶石 | 正如其名 |

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
| Liquid Emeralds | 液態綠寶石 | Wynncraft 的高額貨幣單位 |
| Item Identifier | 物品鑑定師 | NPC 職業，不是「鑑定道具」 |
| Lootrun | Lootrun | **不翻**。社群通用說法，翻了反而對不上 |

## 製作職業

| 原文 | 譯文 |
|---|---|
| Tailoring | 裁縫 |
| Jeweling | 珠寶 |
| Weaponsmithing | 武器鍛造 |
| Armouring | 防具製作 |
| Woodworking | 木工 |
| Cooking | 烹飪 |
| Alchemism | 鍊金 |
| Scribing | 抄寫 |

## 採集職業

| 原文 | 譯文 |
|---|---|
| Fishing | 釣魚 |
| Woodcutting | 伐木 |
| Mining | 採礦 |
| Farming | 農耕 |

## 屬性與元素

| 原文 | 譯文 |
|---|---|
| Strength | 力量 |
| Dexterity | 靈巧 |
| Intelligence | 智慧 |
| Defence | 防禦 |
| Agility | 敏捷 |
| Air / Earth / Thunder / Water / Fire | 風／地／雷／水／火 |
| Elemental Defence(s) | 元素防禦 |

> 元素傷害統一寫成「**風屬性傷害**」而不是「風傷害」——
> 少了「屬性」兩字會跟「風」這個字本身混淆。

## 職業

職業名一律「**主職/造型**」，中間用半形斜線。

| 原文 | 譯文 |
|---|---|
| Mage/Dark Wizard | 法師/黑巫師 |
| Archer/Hunter | 弓手/獵人 |
| Warrior/Knight | 戰士/騎士 |
| Assassin/Ninja | 刺客/忍者 |
| Shaman/Skyseer | 薩滿/天視者 |

## 攻擊速度

固定用這七個，不要自己換詞。

| 原文 | 譯文 |
|---|---|
| Super Fast | 超快 |
| Very Fast | 很快 |
| Fast | 快速 |
| Normal | 普通 |
| Slow | 緩慢 |
| Very Slow | 很慢 |
| Super Slow | 超慢 |

---

## 一律保留原文的

翻了會跟其他玩家對不上話，或社群本來就講英文。

- **地名**（Ragni、Detlas、Troms…）——用 `{p}` 佔位符自動處理，共 137 個
- **裝備與物品名稱**——專有名詞，翻了對不上 wiki 與交易市場
- **技能名稱**——建議保留英文，真的需要就附註：`Meteor（隕石）`
- **Lootrun**、**Raid**、**Guild** 等社群通用詞

---

## 想改某個詞？

送 PR 改這個檔案，說明為什麼。改完之後**同時把相關譯文檔一起改掉**，
否則 CI 會警告表與實際譯文不一致。
