# 簡中：機械檢查找到的問題

整理日期 2026-11-28。這份是拿程式對 zh_cn 語料跑十種「以前試過、真的抓得到錯」的比對，每一條都人工看過才放進來。
語料裡的簡中譯文我們沒有動，由簡中團隊決定怎麼改；處理完把那一列刪掉（或把「完成」欄的 ☐ 改成 ☑）即可。

「建議」欄只在答案很明確時才寫，而且用的是簡中語料自己已經在用的譯名，不是從繁中轉過來的。沒寫的請團隊自己決定。

## 總覽

| # | 檢查 | 條數 | 在遊戲裡會怎樣 |
|---|---|---|---|
| 1 | 同一個鍵、原文跟繁中不一樣 | 0（另 110 條已在 [stale-sources.md](stale-sources.md)） | 原文對不上，畫面顯示英文 |
| 2 | 字型畫不出來 | 0 | 整句退回英文，沒有任何訊號 |
| 3 | 佔位符數量、順序、空格 | 1 | 數字填到錯的位置 |
| 4 | 同一個英文名字有兩種譯法 | 8 組（13 條） | 同一個東西在不同畫面叫不同名字 |
| 5 | 一個譯名對兩個不同英文詞（撞名） | 10 組 | 兩個不同的技能／詞條在畫面上同名，玩家分不出來 |
| 6 | 框架裡的任務名跟正本不同 | 3 組（7 條） | 任務名在提示、對話裡是第二種寫法或英文 |
| 7 | 名字翻了一半 | 1 條（另附 1 則觀察） | 同一個道具名有的地方中文、有的地方英文 |
| 8 | 數字不見、問號對不上 | 2 條（候選） | 語氣或資訊少了一截 |
| 9 | 繁體字混進簡中（CI 沒擋到的） | 0 | — |
| 10 | 簡中空白、繁中有譯文 | 按檔統計 | 畫面顯示英文 |

**建議先處理**：第 5 類（撞名，技能樹上兩格同名最容易讓玩家搞混）、第 4 類（大多是一兩個字之差，改起來很快），以及第 10 類裡的探索點名與任務對話。

---

## 1. 同一個鍵、原文跟繁中不一樣

逐鍵比對所有巢狀檔（ability/、quest/、secret/、scoped/、gear-*.json 等）的 src。找到的 110 條**全部**都已經列在 [stale-sources.md](stale-sources.md)，這裡不重複列。

## 2. 字型畫不出來

對話的就地取代有一道守門：譯文裡只要有一個字不在對話字型裡，整句就退回英文，畫面上看起來只是「這句沒翻」。

- 對話譯文（quest/、secret/、dialogue-choice.json）逐字對 `font/dialogue/zh_cn/coverage.txt`：**0 條**。
- /class 選單的流派名（scoped/archetype.json）對內附的 Fusion Pixel 簡體字型：**0 條**。
- 簡中用到、但任何原文與繁中都沒用過的符號：只有全形冒號「：」（238 條）。字型畫得出來，不算缺字，這裡只是提一下（繁中已統一用半形冒號，簡中要不要統一由團隊決定）。
- 介面的平滑字型（F6 設定畫面）沒收到的字會退回原版點陣字，不會變方框，所以不必掃。

## 3. 佔位符

`{~}` 沒有編號時，遊戲照出現順序填數字。譯文把兩個數字的位置對調卻沒加編號，數字就會填反。

`{#}`、`{p}`、`{u}`、`{~}` 的個數不符：0 條（validate 已在擋）。佔位符直接貼著漢字、但原文那個位置有空格的：0 條。

| 完成 | 檔案 | 鍵 | 原文 | 目前譯文 | 問題 | 建議 |
|---|---|---|---|---|---|---|
| ☐ | quest/hollow-serenity.json | Hollow Serenity#052 | ...You know what? Neither do you! You've said, like, {~} sentences in the past {~} minutes! | ……你知道吗？你也没说什么啊！你在过去 {~} 分钟里大概只说了 {~} 句话！ | 原文先句數後分鐘，譯文先分鐘後句數，兩個數字會填反 | ……你在过去 {~2} 分钟里大概只说了 {~1} 句话！ |

## 4. 同一個英文名字有兩種譯法

同一個名字散在好幾個不同的鍵（卡片、名牌、清單、獎勵列），validate 只比同一個鍵，所以看不出來。下表「多數」是簡中語料自己的票數。

| 完成 | 檔案 | 鍵（原文） | 目前譯文 | 問題 | 建議 |
|---|---|---|---|---|---|
| ☐ | misc.json | Mainhand Scale{#}[{~}] | 主要词缀{#}[{~}] | 另一條 `Mainhand Scale{#} [{~}]` 寫「主手词缀」，對應的 Offhand 兩條都是「副手词缀」 | 主手词缀{#}[{~}] |
| ☐ | npc.json | {#}{#}<br><br>+{~} {#}Defence<br>+{~} Healing Efficiency<br>+{~} Thorns<br><br> You chose this Buff! | ……<br>+{~} 反伤<br>…… | Thorns 在 raid.json 同一張增益卡的四個版本與 misc.json 的詞條列（`- Thorns: +{~}`、`[+{~} Thorns for {~} seconds]`）都是「近战反伤」，只有這張寫「反伤」 | 把「反伤」換成「近战反伤」 |
| ☐ | npc.json | Salmon<br>✔ Ⓚ Fishing Lv. Min: {~}<br><br>{#} Left-Click for Oil<br>{#} Right-Click for Meat | ……<br>{#} 右键取鱼 | 其他 7 種魚（鲈鱼、鳟鱼、锦鲤……）都是「右键取鱼肉」 | 右键取鱼肉 |
| ☐ | npc.json | Carp<br>（同上） | ……<br>{#} 右键取鱼 | 同上 | 右键取鱼肉 |
| ☐ | npc.json | +{~} Ⓙ Farming XP [{~}]<br>+{~} Rye Grains | +{~} Ⓙ 农耕经验 [{~}]<br>+{~} 黑麦谷物 | 同一張卡的 `[x{~}]` 版本、「已完成购买」與精炼那條都是「黑麦谷粒」（3 對 1） | 黑麦谷粒 |
| ☐ | npc.json | {#}<br>Party Finder<br>Find Player groups for | {#}<br>组队查找器<br>寻找玩家队伍： | 其他三塊名牌與聊天訊息都是「组队查找」 | 组队查找 |
| ☐ | npc.json | {#}{#}<br>Collect White Lilies<br><br>Required Level: {~} | {#}{#}<br>采集白百合<br><br>需求等级: {~} | 迷你任務名：misc.json 三條（清單、`[Mini-Quest]` 卡片、「+ 新任务」）寫「收集白百合」 | 收集白百合 |
| ☐ | quest/overture-to-despair.json | Overture to Despair#007（Mini-Quest Started: Collect White Lilies） | 迷你任务开始: 采集白百合 | 同上 | 迷你任务开始: 收集白百合 |
| ☐ | npc.json | {#}{#}<br>Collect Stackpeak Eggs<br><br>Required Level: {~} | {#}{#}<br>收集叠峰鹰蛋<br><br>需求等级: {~} | Stackpeak 三種寫法：「叠峰鹰蛋」（這條、quest.json 一條台詞）、「Stackpeak 蛋」（misc.json 清單、卡片、「+ 新任务」、quest.json 的「迷你任务开始」共 4 條）、mini-quest.json 的目標列整個留英文 `[{~} Stackpeak Eggs]` | 請團隊選定一種 |
| ☐ | quest.json | Those pies go fast, … [{~} Stackpeak Eggs] by my tally. | ……照我算要 [{~} 叠峰鹰蛋]。 | 同上 | — |
| ☐ | mini-quest.json | Bring [{~} Stackpeak Eggs] to the Slaying Post at [-{~}, {~}, -{~}] | 到击杀告示上交 [{~} Stackpeak Eggs]，坐标 …… | 同上（目標列留英文） | — |
| ☐ | npc.json | Right-Click to<br>Get the Potion | 右键<br>拿取药水 | 同一句名牌的另一個版本（多一行 À）寫「领取药水」 | 兩條統一成同一種 |
| ☐ | npc.json | Right-Click to<br>Get the Potion<br>À | 右键<br>领取药水<br>À | 同上 | — |

註：Mainhand Scale 那一組裡，`{#}` 後面有沒有空格是兩個不同的鍵，兩條都在語料裡，所以要一起對齊。

## 5. 一個譯名對兩個不同英文詞（撞名）

兩個不同的英文名字寫成同一個中文。技能樹的格子上只有名字，兩格同名玩家就分不出來；validate 只看「同一句原文的兩種譯法」，看不到這種反方向的錯。
判準：下表每一組，繁中、日、韓、俄、西、德、法、葡八個語言裡至少有七個把兩邊分開寫。

| 完成 | 譯名 | 英文 A（檔案） | 英文 B（檔案） | 說明 |
|---|---|---|---|---|
| ☐ | 光环 | Halo（ability/mage.json，法師技能） | Aura（ability/shaman.json，薩滿技能） | 連帶 gui.json「Unlock Halo ability」與 misc.json「Unlock Aura ability」都是「解锁光环技能」，兩行一模一樣 |
| ☐ | 回响 | Echo（ability/assassin.json） | Reverberance（ability/warrior.json） | 兩個職業的不同技能 |
| ☐ | 风暴 | Windstorm（ability/archer.json） | Tempest（ability/warrior.json） | 兩個職業的不同技能 |
| ☐ | 斥退 | Repel（ability/archer.json） | Repulse（ability/shaman.json） | 兩個職業的不同技能 |
| ☐ | 闪耀 | Shine（ability/warrior.json，戰士技能） | Shining（ability-terms.json，法師技能給隊友的狀態） | 不同職業、不同機制 |
| ☐ | 毒雾 | Noxious Haze（ability/assassin.json，技能名） | Noxious（ability-terms.json，該技能給的狀態） | 技能名與它給的狀態同名（敘述寫「都会获得毒雾 {#}」）；其他語言都把技能與狀態分開寫，影響較小，算候選 |
| ☐ | 压制 | Pressure（ability/warrior.json） | Overwhelm（major-id.json） | 技能與 Major ID 同名 |
| ☐ | 净化 | Purification（ability/mage.json） | Lustrate（major-id.json） | 技能與 Major ID 同名 |
| ☐ | 位移 | Displacement（ability/mage.json） | Displace（major-id.json） | 技能與 Major ID 同名 |
| ☐ | 孤注一掷 | All In（lootrun.json） | Desperate Measures（major-id.json） | Lootrun 項目與 Major ID 同名 |

改哪一邊由團隊決定；可以參考的順序是：先看哪個譯名已經在別的條目裡被另一個英文詞「佔住」，被佔的那個就改另一邊。

## 6. 框架裡的任務名

拿 `quest-name.json` 的簡中譯名當正本，掃所有含這個任務名的條目（`[Quest Req: X]`、`+ New Quest [X]`、台詞、說明），譯文裡找不到正本的列出來。
已列在 known-issues.md 第 16 條的 Marauder's Dues 不重複列。

| 完成 | 檔案 | 鍵（原文） | 目前譯文 | 問題 | 建議 |
|---|---|---|---|---|---|
| ☐ | misc.json | {#} [{~}/{~}] Kelight: Only the bravest adventurers who<br>{#} succeed in my quest can enter the Temple of the Legends. | ……<br>{#} {c4}才能进入传说神殿。 | 正本「传奇神殿」（語料 22 處），這條寫「传说神殿」 | 把「传说神殿」換成「传奇神殿」 |
| ☐ | misc.json | The Temple of the Legends' soldiers undergo the most rigorous training in all of Wynn, and the results speak for themselves. | 传说神殿的士兵受的是全 Wynn 最严苛的训练，成果如何不必多说。 | 同上 | 传奇神殿的士兵…… |
| ☐ | discovery.json | An arena for those who wish to truly test their mettle after being admitted into the Temple of the Legends, where fierce training takes place. | 一座竞技场，专为获准进入 Temple of the Legends 后、…… | 任務名留英文 | 把 Temple of the Legends 換成「传奇神殿」 |
| ☐ | discovery.json | Area Discovered: Arena of the Legends (+{~} XP)<br>（同上） | 发现区域: Arena of the Legends (+{~} XP)<br>一座竞技场，专为获准进入 Temple of the Legends 后、…… | 同上；前面的探索點名照慣例不動 | 只換句子裡的 Temple of the Legends |
| ☐ | misc.json | - +Access to The Passage | - +可进入幽径 | 任務名與地點都叫「通道」（`[任务需求: 通道]`、通道守卫「进入这条通道」），獎勵列卻寫「幽径」 | - +可进入通道 |
| ☐ | misc.json | {#}- +Access to The Passage | {#}- +可进入幽径 | 同上 | {#}- +可进入通道 |
| ☐ | quest/lost-soles.json | Lost Soles#014（Bring back Abysso Galoshes, received during the quest Beneath the Depths.） | 带回 Abysso Galoshes，它是在任务 Beneath the Depths 中获得的。 | 任務名留英文；正本「深渊之下」 | 把 Beneath the Depths 換成「深渊之下」 |

## 7. 名字翻了一半

同一個名字在簡中已經翻過的條目比留英文的多，剩下那幾條還是英文。

| 完成 | 檔案 | 鍵（原文） | 目前譯文 | 問題 | 建議 |
|---|---|---|---|---|---|
| ☐ | quest/cluck-cluck.json | Cluck Cluck#012（Bring back [{~} Cluckles' Favourite Feather] to Nohno.） | 把 [{~} Cluckles' Favourite Feather] 带回给 Nohno。 | misc.json 的道具名與 Reincarnation 的三條都是「Cluckles 最爱的羽毛」 | 把 [{~} Cluckles 最爱的羽毛] 带回给 Nohno。 |

觀察（不算錯）：major-id.json 的說明裡提到屬性名時一律留英文（Health Regen、Life Steal、Mana Regen、Mana Steal、Healing Efficiency 共 14 處），而其他檔都翻成「生命恢复」「法力恢复」「治疗效率」。整檔一致，看起來是刻意的；如果不是，可以照其他檔的譯名換。

## 8. 數字不見、問號對不上

原文有阿拉伯數字而譯文找不到（國字也算）：0 條。下面兩條是「原文是問句、譯文沒有問號」裡最可疑的，屬於候選，請團隊判斷。

| 完成 | 檔案 | 鍵 | 原文 | 目前譯文 | 問題 |
|---|---|---|---|---|---|
| ☐ | quest/the-canary-calls.json | The Canary Calls#041 | OW! Bloody hell! Those are some freakishly sharp claws! What are you playing at?! | 哎哟！该死！这爪子也太尖了！你在搞什么—— | 原文是說完的質問（?!），譯文變成話被打斷 |
| ☐ | quest/ensemble-of-hope.json | Ensemble of Hope#719 | And... I want to be like that, y'know? Confident enough to do that sorta thing. | 而……我也想变成那样，你懂吧！有足够的自信去做那种事。 | y'know? 是問句語氣，譯文寫成驚嘆號 |

## 9. 繁體字混進簡中

自己算字表（繁中出現 50 次以上、簡中 3 次以下的字），再排掉 `tools/check-zh-cn.py` 已經在 CI 擋的：**0 條**（命中的「喔」「橘」兩個都是正常的簡體用字）。

## 10. 簡中空白、繁中有譯文

同一句原文，繁中有譯文、簡中 dst 是空的，遊戲裡會顯示英文。只按檔案統計（2026-11-28 的 main）。

| 類別 | 檔案 | 條數 |
|---|---|---|
| 任務對話 | quest/（各任務檔合計） | 199 |
| 任務對話 | secret/（其中 the-royal-university's-library.json 141、wynn-plains-monument.json 21、ruins-of-detlas.json 20、shadowy-dealings.json 20） | 262 |
| 任務對話 | quest.json | 80 |
| 任務對話 | dialogue-choice.json | 35 |
| 名牌 | npc.json | 7,228 |
| 名牌 | label.json | 284 |
| 地點 | discovery-name.json（探索點名，307 條全空；繁中已改成翻譯，簡中要不要翻由團隊決定） | 307 |
| 介面 | quest-ui.json | 99 |
| 介面 | dungeon.json | 97 |
| 介面 | gui.json | 67 |
| 介面 | scoped/（幾乎都是 scoped/gear.json） | 58 |
| 介面 | raid.json、quest-name.json、lootrun.json、mini-quest.json、guild.json、wynntils.json | 14、11、4、4、2、2 |
| 物品名 | ingredient.json、material.json（不受「翻譯物品名稱」開關管，一律顯示） | 971、136 |
| 物品名 | gear-*.json、tome.json、aspect.json、charm.json（受開關管，預設關） | 6,283、160、128、5 |
| 其他 | misc.json | 3,890 |
| 其他 | cosmetics.json | 647 |
| 其他 | ability/、major-id.json、aspect-desc.json、ability-terms.json、discovery.json、unsorted.json | 56、35、14、6、6、3 |

玩家最常看到的是任務對話、名牌與探索點名；素材名（ingredient／material）不受開關影響，空著就一定是英文。
