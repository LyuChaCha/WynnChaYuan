# 專有名詞對照表

> 這裡講**怎麼翻**。要先弄懂**是什麼**，看 [世界觀與主線](docs/world-and-story.md)。

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

## 世界觀相關

| 原文 | 譯文 | 備註 |
|---|---|---|
| Decay | 衰朽 | **未確定** |
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
**請注意，這裡每一個單字都待討論**

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
| Mel | 鄉下口音、爽朗 | 原文大量吞音（`'fraid`、`y'were`、`'round`）。中文用口語詞與語尾助詞（「啊」「嘛」「欸」）去對應，不要用注音或錯字 |
| Linton | 藏不住話、少根筋 | 想到什麼講什麼，常常講到一半自己剎車（「呃」「當我沒說」） |
| Lanu | 冷靜、分析型 | 句子完整、不帶情緒，常補一句觀察 |
| Jenprest | 老兵、帶土氣 | 原文吞音（`o'`、`n'`、`t'`），中文用「欸」「哎唷」「難辦啊」這種口氣 |
| Dr. Picard | 亢奮的學者 | 語速快、驚嘆號和破折號多，講到一半自己插自己的話 |
| Reynauld | 話少、疲憊 | 短句，常以「嗯」開頭，情緒壓在字面底下 |
| Her Majesty | 從容、居高臨下 | 書面語、長句，從不提高音量。愈平靜愈可怕 |
| Sovereign Majin | 浮誇、瞧不起人 | 華麗辭藻加尖刻嘲笑，愛用「嗯？」反問把人架住 |
| Sovereign Zhiraok Nasin | 暴躁、滿嘴數學 | 見下方雙關表——**他的每一句話都要帶一個數學詞**，這是角色本身 |
| Syndra | 沉穩、帶詩意 | 長句、從容，用自然的比喻（風、葉、根）。是領袖，不是說教者 |
| Zeph | 嚴重口吃 | 原文用連字號重複字首（`th-there`、`w-wondering`）。中文用<b>重複首字加頓號</b>：「你、你在這裡啊」。每句都要有，但別多到讀不下去 |
| Rex | 沒耐性、防備心重 | 短句、反問、常常話說一半就「算了」。她的刺是怕，不是壞 |
| Mora | 溫厚的長輩 | 有點文氣，笑聲寫成「呵呵」，稱新人「小葉子」 |
| Sui | 怯生生 | 句子短、常有「呃」，說完自己又收回去。Mora 的妹妹 |
| Dr. Picard | 亢奮的學者 | 已列於上。他叫玩家「助手」，那是他一廂情願封的 |
| Caid | 嘴硬、心虛 | 講話跳、愛辯解，被戳破就轉移話題。他的船是他唯一的驕傲 |
| Reynauld | 話少、疲憊 | 已列於上。他在 Fruma 是收賄的軍官，這件事解釋了他為什麼那麼冷 |
| Yahya | 嚴重口吃 | 與 Zeph 同一套處理：重複首字加頓號。他怕蜘蛛怕到說不出完整句子 |
| Ope | 油腔滑調 | 一直在推銷、一直在賴帳。但他是 Alvin 鎮長的外甥，母親被蜘蛛害死 |
| Mayor Alvin | 溫和、疲憊的長輩 | 講 Ope 的時候要留住那份護短又無奈 |
| Captain Ragon | 教官 | 條理分明、講解式的句子。他是在上課 |
| Admiral Aegis | 上將 | 威嚴但不擺架子，會誇人 |
| Kaio | 看透了的老人 | 鄉音重、字句吞得多（`ain't`、`'n`、`nabbin'`），偶爾帶髒字。他講 Aledar 那一段是整個任務的重心 |
**請注意，這裡每一個語氣都未驗證準確性**


已翻的任務可以直接當範本：`King's Recruit`（開場，124 句）、`Queen's Recruit`（Fruma 篇，673 句）、`A New Beginning`（289 句）、`Recover the Past`（264 句）、`Cook Assistant`、
`Clearing the Camps`。
**註記：不要**

---

## 有雙關或典故的

Wynncraft 的任務名稱與台詞常常一語雙關。**能做出來就做出來**，做不出來就在
這裡註明原文在玩什麼——內容的語氣才接得上。

| 原文 | 譯文 | 原文在玩什麼 |
|---|---|---|
| A Grave Mistake | 致命的失足 | `grave` 同時是「嚴重的」與「墳墓」。玩家最後真的掉進一座墳墓，「失足」剛好兩層都接住 |
| Cluck Cluck | 咕咕咯咯 | 雞叫聲。中文的擬聲詞也是疊字，四個字比兩個字更像在學雞叫 |
| Queen's Recruit | 女王的新兵 | 對照開場的 `King's Recruit`。當年是國王徵召你，這次換成女王——而這次的「徵召」是什麼意思，正是整個任務的問題 |
| expected value | 期望值 | Zhiraok 講的是「新兵的價值」，用的是統計學的詞 |
| you ignorant integrand | 你這個無知的被積函數 | 罵人的話直接用微積分名詞。中文照搬，突兀正是效果 |
| your timing was golden | 你這時機抓得很黃金比例 | `golden` 是黃金比例。譯文得把「比例」補出來，不然梗就沒了 |
| trying to subtract them | 想把他們減掉 | 講「把人弄走」用減法。不要譯成「除掉」，那是另一個運算 |
| a miscalculation | 一次誤算 | 不要譯成「失策」 |
| how much more irrational can we get | 我們還能有多無理 | `irrational` 同時是「不理性」與「無理數」。中文的「無理」剛好兩層都接住 |
| calculate yourself out of dead last | 先把自己從倒數第一算出來 | Majin 反過來拿數學嗆 Zhiraok |
| crunch the numbers | 算那些數字 | Zhiraok 講「處理公務」用的說法 |
| how could that have factored out | 怎麼會因式分解成這樣 | 講「事情怎麼會變這樣」 |
| fill the gaps | 把缺項補上 | 不要譯成「補足空白」，那不是數學詞 |
| one positive in a sea of negatives | 一堆負數裡總算有一個正的 | 正負號的雙關 |
| a toddler could calculate that | 連學步的小孩都算得出來 | |
| I'd never bet on your odds | 絕不會押你的賠率 | 機率 |
| keep it constant | 保持常數 | 他的道別語。不要譯成「保持穩定」 |
| you interrupt my sequence | 你打斷我的數列 | 「打斷我做事」 |

> 破碎、殘缺的台詞（`.;I. ,told;.`）是**刻意**的，那是氣氛的一部分。
> 譯文要保留那種殘缺感，不要補成通順的句子。

註記：這邊要多想，不要直接丟上去。

---

## 想改某個詞？

送 PR 改這個檔案，說明為什麼。改完之後**同時把相關譯文檔一起改掉**，
否則 CI 會警告表與實際譯文不一致。
