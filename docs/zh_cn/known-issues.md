# zh_cn 同錯清單

這是繁中重讀任務以外的文字（misc、npc、quest.json、探索點、洞窟、label、quest-ui）時改掉的錯，簡中同一句看起來也有同樣問題的清單，都還沒改。

建議改法的用詞照簡中語料自己的慣例（例如大碎裂、树冠会、城塞、击杀！），不是從繁中轉過來的。共 34 條：前 17 條是意思錯，後 17 條是簡中語料自己的譯名不一致或格式殘留。

2026-11-28 複查：34 條在 main 上都還是原樣，沒有已處理的。

---

## 一、意思錯

### 1. B01-0024　misc.json
- 英文原文: Inside, there are walls and walls of unfamiliar text, flanked by incomprehensible graphs.
- 簡中現在: 里面是一面又一面写满陌生文字的墙，两旁还有看不懂的图表。
- 問題: walls and walls of text 是「大段大段的文字」的慣用語，不是真的牆。
- 建議改法: 里面是一大片又一大片陌生的文字，两旁还有看不懂的图表。

### 2. B01-0041　misc.json
- 英文原文: A magic pearl, by nature of being from the {p}. Could be good for charmcrafting.
- 簡中現在: 一颗魔法珍珠，天生就来自 {p}。或许适合用来制作护符。
- 問題: 原文是「因為來自 {p} 所以帶有魔力」，譯文把因果讀成「天生就來自」。
- 建議改法: 一颗珍珠，因为来自 {p} 而天生带有魔力。或许适合用来制作护符。

### 3. B01-0657　misc.json
- 英文原文: ...and allows one to vent their frustrations."
- 簡中現在: ……但这能让人发泄一下怨气。”
- 問題: 原文是 ...and，承接上一句再補一個好處，不是轉折，「但」用錯。
- 建議改法: ……也能让人发泄一下怨气。”

### 4. B02-0444　misc.json
- 英文原文: Likes to mind his own business.
- 簡中現在: 喜欢管好自己的事。
- 問題: mind his own business 是「不管閒事」的慣用語，照字面翻讀起來像在叫別人少管閒事。
- 建議改法: 不爱多管闲事。

### 5. B02-0613　misc.json
- 英文原文: [{~}/{~}] Seaskipper Captain: Have you heard tales about Captain Redbeard? Back when I was young, he terrorized the seas!
- 簡中現在: [{~}/{~}] 渡船船长: 你听说过红胡子船长的故事吗？我年轻那会儿，他可是称霸四海！
- 問題: terrorized the seas 是在海上四處作惡、令人聞之色變，「稱霸」只剩稱王的意思，丟了海盜為害的語感。
- 建議改法: [{~}/{~}] 渡船船长: 你听说过红胡子船长的故事吗？我年轻那会儿，他可是横行四海！

### 6. B03-0142　misc.json
- 英文原文: Foreign chanting is a constant ambience here.
- 簡中現在: 这里总是回荡着异域的吟唱声。
- 問題: foreign 指聽不懂的陌生吟唱，不是「異域」；同一句在 discovery.json〈Tribal Sanctuary〉的兩個版本都寫「陌生的吟唱声」。
- 建議改法: 这里总是回荡着陌生的吟唱声。

### 7. B04-0694　misc.json
- 英文原文: Trick or treat, bane or boon. Pick your candy, pick your poison. The choice is yours.
- 簡中現在: 不给糖就捣蛋，是祸也是福。挑你的糖果，挑你的毒药。选择权在你。
- 問題: bane or boon 是二選一（是禍是福），「是禍也是福」變成兩者皆是。
- 建議改法: 不给糖就捣蛋，是祸是福。挑你的糖果，挑你的毒药。选择权在你。

### 8. B04-0742　misc.json
- 英文原文: Trick or treat, bane or boon.
- 簡中現在: 不给糖就捣蛋，是祸也是福。
- 問題: 同上一條。
- 建議改法: 不给糖就捣蛋，是祸是福。

### 9. B05-0496　misc.json
- 英文原文: A festival attendee claims that they saw a group from the Kalkalrah clan with bottles of fuel bring it to the second floor of {p}. They were paid to stay silent about what they saw.
- 簡中現在: 一位庆典参加者声称，看到一群 Kalkalrah 族的人带着几瓶燃料上了 {c2}{p} 的二楼{/}。他们收了钱，对所见之事保持沉默。
- 問題: 收錢封口的是目擊的那位參加者（they 是單數），「他们」讀起來是 Kalkalrah 那群人。
- 建議改法: 一位庆典参加者声称，看到一群 Kalkalrah 族的人带着几瓶燃料上了 {c2}{p} 的二楼{/}。这位参加者收了钱，对所见之事保持沉默。

### 10. B05-0512　misc.json
- 英文原文: Arris says that while they were exiled for stealing a box of green tea, but they say they never committed committed the theft they were accused of.
- 簡中現在: Arris 说，他们虽然因为偷了一箱绿茶而被放逐，但他们从没犯下被指控的偷窃罪。
- 問題: 原文是被指控偷竊而放逐、本人否認；「因为偷了」等於直接說他偷了，跟後半句矛盾。
- 建議改法: Arris 说，他们虽然以偷了一箱绿茶的罪名被放逐，但他们从没犯下被指控的偷窃罪。

### 11. B08-0090　quest.json
- 英文原文: HOW LOVELY. YOU DESTROYED MY AUXILIARY MEMORY BANK. TOO BAD IT WAS ONLY AUXILIARY. I WILL STILL REMEMBER THIS. I WISH I COULD FORGET ABOUT YOU.
- 簡中現在: 真是可爱。你把我的辅助记忆库破坏了。……
- 問題: HOW LOVELY 是反諷的「真好啊」，照字面譯成「可愛」意思不對。
- 建議改法: 真是太好了。你把我的辅助记忆库破坏了。（後面不動）

### 12. B09-0304　quest.json
- 英文原文: Like, they think if they're just, just man enough and keep getting back up, they'll pull a turnabout? Uh, news flash? Doesn't work.
- 簡中現在: 就像，他们以为只要够爷们、一次又一次爬起来，就能逆转局势？呃，告诉你个新闻？没用的。
- 問題: news flash 是「拜託醒醒」的慣用語，照字面翻成「告訴你個新聞」讀不懂。
- 建議改法: 就像，他们以为只要够爷们、一次又一次爬起来，就能逆转局势？呃，醒醒吧？没用的。

### 13. B10-0005　discovery.json
- 英文原文: With the death of Marius Twain, the family fell apart, leaving Wynn's protectors alone... or worse.
- 簡中現在: 随着 Marius Twain 的死去，这个家族分崩离析，只留下 Wynn 的守护者孤身一人……甚至更糟。
- 問題: protectors 是複數（Marius 死後兄弟各奔東西），「孤身一人」把主詞變成單數。
- 建議改法: 随着 Marius Twain 的死去，这个家族分崩离析，Wynn 的守护者们从此孤立无援……甚至更糟。

### 14. B10-0030　discovery.json
- 英文原文: {p} was one of the cities attacked most often. The people would hide in the gutters and sewers until it was over. However, corrupt enemies were not without intelligence, and they were quickly discovered by the enemy.
- 簡中現在: ……然而腐化的敌人并非毫无智慧，他们很快就被敌人发现了。
- 問題: 被找到的是躲起來的居民；「他们」緊接在「腐化的敌人」後面，讀起來像敵人被敵人發現。
- 建議改法: ……然而腐化的敌人并非毫无智慧，躲藏的人们很快就被发现了。

### 15. B11-0014　discovery.json
- 英文原文: A strange aura emanating from a forgotten refuse within the Void has warped the gravity surrounding it, ...
- 簡中現在: The Void 里一处被遗忘的废墟散出诡异的气息，……
- 問題: refuse 是廢棄物、垃圾，不是廢墟；同一段的另外兩個版本已作「一堆被遗忘的废弃物」。
- 建議改法: The Void 里一堆被遗忘的废弃物散出诡异的气息，……（後面不動）

### 16. B01-0715　misc.json
- 英文原文: {#} You can't go through this gate yet. [Quest Req: A ⏎ {#} Marauder's Dues]
- 簡中現在: {#} 你还不能通过这道门。[任务需求: A ⏎ {#} Marauder's Dues]
- 問題: 任務名漏譯；quest-name.json 與「新任务」那行都作「劫匪的过路费」。
- 建議改法: {#} 你还不能通过这道门。[任务需求: 劫匪的 ⏎ {#} 过路费]（換行數照舊）

### 17. B11-0013　discovery.json
- 英文原文: Home to one of the most beautiful gardens in Fruma, this royal bathhouse is off-limits to everyone but its staff, the Queen, and her Sovereigns - and as such, is guarded quite heavily. ...
- 簡中現在: 这座王室浴场拥有 Fruma 最美的庭园之一，除了工作人员、女王与她的 Sovereign 之外一律不得进入——……
- 問題: Sovereign 漏譯；同一段的另外兩個版本與任務對話都作「统领」。
- 建議改法: ……除了工作人员、女王及其统领之外一律不得进入——……（其餘不動）

---

## 二、簡中語料自己的譯名不一致、格式殘留

### 18. B03-0291　misc.json
- 英文原文: [Cave Completed] ⏎ Fire & Water ⏎ … ⏎ - +{~} {#} Water Powder I ⏎ - +{~} {#} Fire Powder I
- 簡中現在: … ⏎ - +{~} {#} Water Powder I ⏎ - +{~} {#} 火元素粉末 I
- 問題: 同一列的火粉末已譯，水粉末漏譯；語料粉末一律作「X元素粉末」。
- 建議改法: 把 Water Powder I 換成「水元素粉末 I」，其餘不動。

### 19. B03-0295　misc.json
- 英文原文: [Cave Completed] ⏎ Winery Infestation ⏎ … ⏎ - +{~} {#} Earth Powder II
- 簡中現在: … ⏎ - +{~} {#} Earth Powder II
- 問題: 粉末漏譯；語料別處作「地元素粉末 II」。
- 建議改法: 把 Earth Powder II 換成「地元素粉末 II」，其餘不動。

### 20. B05-0604　misc.json
- 英文原文: Some archaic sculptures and effigies have resisted both time and The Fracturing.
- 簡中現在: 有些古老的雕塑与塑像，既熬过了岁月，也熬过了大断裂。
- 問題: The Fracturing 簡中語料多數作「大碎裂」（探索點、《临界点》），只有這條和下一條寫「大断裂」。
- 建議改法: 有些古老的雕塑与塑像，既熬过了岁月，也熬过了大碎裂。

### 21. B11-0025　discovery.json
- 英文原文: Left behind by the Fracturing, this intriguing ruin is thought to have been part of a second colossus - though it is currently unknown how this one came to be destroyed.
- 簡中現在: 这处引人好奇的遗迹是大断裂留下来的，……
- 問題: 同上；同一段的另一個版本作「大碎裂」。
- 建議改法: 这处引人好奇的遗迹是大碎裂留下来的，……（其餘不動）

### 22. B09-0436　quest.json
- 英文原文: A strand. Brittle, fraying, but... it is there. Colourless, transparent... but it. Is. There.
- 簡中現在: 一缕细丝。……
- 問題: 這裡的 strand 是構成現實的線，簡中《Apotheosis》《Ensemble of Hope》一律作「丝线」。
- 建議改法: 一缕丝线。……（其餘不動）

### 23. B09-0557　discovery.json
- 英文原文: The leader of the Canopy is a Changeling and was part of the original group of Changelings discovered alongside Sovereign Gikyo. ...
- 簡中現在: Canopy 的首领是一名易形者，……
- 問題: 組織名 the Canopy 在簡中語料作「树冠会」（54 處），探索點說明裡留英文的只有這幾條。
- 建議改法: 树冠会的首领是一名易形者，……（其餘不動）

### 24. B10-0387　discovery.json
- 英文原文: With little government assistance, the people of the {p} depends on various clans ... One such clan is The Canopy.
- 簡中現在: ……The Canopy 就是其中一个氏族。
- 問題: 同上。
- 建議改法: ……树冠会就是其中一个氏族。

### 25. B10-0388　discovery.json
- 英文原文: （同上，depend 版本）
- 簡中現在: ……The Canopy 就是其中一个氏族。
- 問題: 同上。
- 建議改法: ……树冠会就是其中一个氏族。

### 26. B10-0389　discovery.json
- 英文原文: Area Discovered: The Canopy's Hideout (+{~} XP) ⏎ With little government assistance, ... One such clan is The Canopy.
- 簡中現在: 发现区域: The Canopy's Hideout (+{~} XP) ⏎ ……The Canopy 就是其中一个氏族。
- 問題: 同上；前面的探索點名 The Canopy's Hideout 照慣例不動。
- 建議改法: 只把句尾的 The Canopy 換成「树冠会」。

### 27. B10-0055　discovery.json
- 英文原文: The Aelumia University's library is home to a vast wealth of information. ...
- 簡中現在: Aelumia University 的图书馆藏有极为丰富的信息。……
- 問題: 簡中 misc 與《Off the Rails》都作「Aelumia 大学」，探索點這三條留英文。
- 建議改法: Aelumia 大学的图书馆藏有极为丰富的信息。……（其餘不動）

### 28. B11-0007　discovery.json
- 英文原文: Colloquially known as Aelumia University, it is the most prestigious educational institution in Fruma. ...
- 簡中現在: 俗称 Aelumia University，这是 Fruma 最有声望的教育机构。……
- 問題: 同上。
- 建議改法: 俗称 Aelumia 大学，这是 Fruma 最有声望的教育机构。……

### 29. B11-0008　discovery.json
- 英文原文: Area Discovered: The Royal University of Aelumia (+{~} XP) ⏎ Colloquially known as Aelumia University, ...
- 簡中現在: 发现区域: The Royal University of Aelumia (+{~} XP) ⏎ 俗称 Aelumia University，……
- 問題: 同上；前面的探索點名不動。
- 建議改法: 只把「俗称 Aelumia University」換成「俗称 Aelumia 大学」。

### 30. B10-0285　discovery.json
- 英文原文: {p} has served as a beacon in the Canyon of The Lost. The Air Monks help guide people who have gotten lost.
- 簡中現在: ……Air Monks 会帮助迷路的人找到方向。
- 問題: 《The Order of the Grook》已把 Air Monks 譯作「风僧」，探索點留英文。
- 建議改法: ……风僧会帮助迷路的人找到方向。

### 31. B10-0286　discovery.json
- 英文原文: Area Discovered: {p} (+{~} XP) ⏎ （同上）
- 簡中現在: 发现区域: {p} (+{~} XP) ⏎ ……Air Monks 会帮助迷路的人找到方向。
- 問題: 同上。
- 建議改法: ……风僧会帮助迷路的人找到方向。

### 32. B12-0581　cave.json
- 英文原文: Old ruins carved into the side of the tableland Aelumia Citadel rests upon at [-{~}, {~}, -{~}] have become the spawning ground for various unsavory creatures and foliage.
- 簡中現在: 凿在 Aelumia Citadel 所倚台地侧面的古老遗迹，……
- 問題: Citadel 在簡中語料作「城塞」（城塞营房、城塞地下墓穴、探索點），只有這句留英文。
- 建議改法: 凿在 Aelumia 城塞所倚台地侧面的古老遗迹，……（其餘不動）

### 33. B12-0228　label.json
- 英文原文: Locked Loot Chest [✫✫✫✫] ⏎ SLAY! Defeat a Subterranean Overlord
- 簡中現在: 上锁的战利品宝箱 [✫✫✫✫] ⏎ 猎杀！击败 地底霸主
- 問題: SLAY! 簡中其他寶箱一律作「击杀！」（138 條，「猎杀！」只有 2 條）；「击败」後面多一個空格，是組字殘留。
- 建議改法: 上锁的战利品宝箱 [✫✫✫✫] ⏎ 击杀！击败地底霸主

### 34. B12-0818　quest-ui.json
- 英文原文: Identify your helmet at the Item Identifier at [-{~}, {~}, -{~}] and equip it in your helmet slot
- 簡中現在: 到 [-{~1}, {~2}, -{~3}] 的 物品鉴定师 鉴定你的头盔，并装备到头盔槽位
- 問題: 「物品鉴定师」前後多兩個空格，是組字殘留。
- 建議改法: 到 [-{~1}, {~2}, -{~3}] 的物品鉴定师那里鉴定你的头盔，并装备到头盔槽位
