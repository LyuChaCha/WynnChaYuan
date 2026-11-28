# 簡中：任務對話重讀時發現的問題

整理日期 2026-11-28。這是繁中把任務對話（`quest/`、`secret/` 共 203 個任務）照英文原文與故事前後文逐句重讀時，順便對照同一句簡中譯文記下的問題。
語料裡的簡中譯文我們沒有動，由簡中團隊決定怎麼改；處理完把那一條刪掉即可。

「建議改法」只在答案很明確時才寫，用的是簡中語料自己已經在用的譯名，不是從繁中轉過來的。沒寫的請團隊自己決定。
「鍵」是 zh_cn 檔案裡的條目鍵。

共 36 條：意思錯 22 條、名詞錯（跟簡中語料自己的多數用法不一致）14 條、錯開一行 0 條。另外附上簡中沒翻、繁中有翻的句數統計。

其中有 9 條是同一類：原文用單數 they 指一個人（多半是玩家），簡中寫成「他们」，讀起來像一群人。這在語料裡很常見，這裡只列上下文明確、讀了會誤會的句子。

---

## 一、意思錯

### 1. A Hunter's Calling#141　quest/a-hunter-s-calling.json
- 英文原文: IT WAS A GOOD THING I HAD BACKUP CORES PREPARED. MY SARCASTIC STATEMENTS MOST LIKELY TRICKED THE HUMAN INTO THINKING THEY DESTROYED ME FULLY.
- 簡中現在: 幸好我准备了备用核心。我那些讽刺的发言多半让那个人类以为他们把我彻底摧毁了。
- 問題: they 是單數，指前面那個人類（玩家）；「以为他们把我彻底摧毁了」讀成另一群人。
- 建議改法: 幸好我准备了备用核心。我那些讽刺的发言多半让那个人类以为自己把我彻底摧毁了。

### 2. A Journey Beyond#015　quest/a-journey-beyond.json
- 英文原文: They're our fourth. What were you saying about the Admiral?
- 簡中現在: 他们是我们的第四个人。你刚才说上将怎么了？
- 問題: They're our fourth 是「第四個人」，單數，指玩家；「他们是我们的第四个人」人數對不上。同任務 #000、#012 的「他们」也是同一個問題。
- 建議改法: 这位就是我们的第四个人。你刚才说上将怎么了？

### 3. A Journey Home#034　quest/a-journey-home.json
- 英文原文: But, they're an expert, so I think your business will have to wait. Really, really sorry about all this!
- 簡中現在: 不过，他们可是专家，所以我想你的事得先等等了。真的、真的很抱歉！
- 問題: they're an expert 是單數，指專家 Paxon 一人；「他们可是专家」讀成好幾個專家。同任務 #031「跟他们谈谈」也一樣。
- 建議改法: 不过，人家可是专家，所以我想你的事得先等等了。真的、真的很抱歉！

### 4. A Journey Home#096　quest/a-journey-home.json
- 英文原文: Wait, who's that? Why are they coming from the border??
- 簡中現在: 等等，那是谁？他们为什么从边境那边过来？？
- 問題: 新兵看到的是從邊境逆向走來的玩家一個人，they 是單數；「他们」讀成一群人。

### 5. Aldorei's Secret Part II#222　quest/aldorei-s-secret-part-ii.json
- 英文原文: Here you go!
- 簡中現在: 开始了！
- 問題: Olon 把點好的酒遞給玩家，Here you go! 是「給你」，簡中譯成「开始了！」。
- 建議改法: 来，给你！

### 6. Aldorei's Secret Part II#241　quest/aldorei-s-secret-part-ii.json
- 英文原文: {~}BP - {~}BP (before portal)To reunite with her lost sister. May the light bring her this gift for her devotion.
- 簡中現在: {~}BP - {~}BP (传送门开启前)为了与她失散的妹妹重逢。愿光为她的虔诚带来这份恩赐。
- 問題: her lost sister 沒交代長幼，簡中寫成「妹妹」。

### 7. Beyond the Grave#024　quest/beyond-the-grave.json
- 英文原文: IF I HAD BOUGHT ANY FLOUR THIS MORNING, I WOULD HAVE OFFERED A BISCUIT FOR DYING. I'M TOLD I MAKE THE BEST CHOCOLATE CHIP ONES.
- 簡中現在: 要是我今天早上带了面粉，就能请你吃块饼干，送你上路。听说我做的巧克力豆饼干是最好吃的。
- 問題: 原文 IF I HAD BOUGHT ANY FLOUR 是「買了麵粉」，簡中「带了面粉」把 bought 讀成 brought。
- 建議改法: 要是我今天早上买了面粉

### 8. Desperate Metal#038　quest/desperate-metal.json
- 英文原文: Request additional orders from L.H. Defend.
- 簡中現在: 向 L.H. Defend 请求追加指令。
- 問題: 原文是兩道指令：向 L.H.（燈塔）請求追加指令，然後 Defend（同 #040 句尾的 Defend.）；簡中把 Defend 併進名字，像是有個叫「L.H. Defend」的對象。
- 建議改法: 向 L.H. 请求追加指令。进入防御。

### 9. Misadventure on the Sea#184　quest/misadventure-on-the-sea.json
- 英文原文: No, but they did a great job in the hold! Some gunpowder went missing though.
- 簡中現在: 没有，不过他们在货舱干得很好！只是少了一些火药。
- 問題: they 是單數，指 Honip 問起的那個士兵（玩家）；「他们」讀成一群人。
- 建議改法: 没有，不过那家伙在货舱干得很好！只是少了一些火药。

### 10. Mixed Feelings#028　quest/mixed-feelings.json
- 英文原文: It’s THEM! Someone call the authorities! It’s the one who set my house on fire.
- 簡中現在: 就是他们！快叫当局来！就是这个人放火烧了我的房子。
- 問題: It's THEM 是單數，指玩家（下一句「就是这个人放火」）；「就是他们」讀成一群人。
- 建議改法: 就是那家伙！快叫当局来！就是这个人放火烧了我的房子。

### 11. Mixed Feelings#061　quest/mixed-feelings.json
- 英文原文: No? Didn't think so. I have no time for this. You served your purpose, even if you did try and redeem your barbaric behaviour... Kill them!
- 簡中現在: 不知道？我就知道。我没时间陪你耗。你已经发挥了你的作用，就算你试着弥补你那野蛮的行为……杀了他们！
- 問題: Rensa 下令殺的是眼前的玩家，they 是單數；「杀了他们」讀成一群人。

### 12. Off the Rails#736　quest/off-the-rails.json
- 英文原文: The troubled people of Fruma shall wake to the fact that their rulers are not infallible gods; they shall at last understand that they alone bear the bridles of their own destiny...!
- 簡中現在: Fruma 苦难的人民将会醒悟，他们的统治者并非无所不能的神；他们终将明白，唯有他们自己握着命运的缰绳……！
- 問題: infallible 是「不會犯錯」，不是「无所不能」；Cemor 要說的是統治者也會犯錯。
- 建議改法: 并非永不犯错的神

### 13. Off the Rails#981　quest/off-the-rails.json
- 英文原文: "Zhirao Sleep Solution: A beverage for hard workers."
- 簡中現在: “Zhirao 助眠饮：献给辛勤劳动者的饮品。”
- 問題: 這是提神飲料（下一句說提供五小時的清醒），「助眠饮」意思正好相反；簡中選項 c101 與 misc.json 都留原文 Zhirao Sleep Solution。
- 建議改法: “Zhirao Sleep Solution：献给辛勤劳动者的饮品。”

### 14. Out of my Mind#026　quest/out-of-my-mind.json
- 英文原文: Find the hidden door next to the Undergrowth Ruins outside {p}
- 簡中現在: 在 {p} 外的灌木废墟附近，找到大门旁边的暗门
- 問題: 原文沒有大門，簡中目標多出「大门旁边」，會讓玩家去找一扇大門。
- 建議改法: 在 {p} 外的灌木废墟旁边找到那扇暗门

### 15. Queen's Recruit#637　quest/queen-s-recruit.json
- 英文原文: Before you leave. You need to tell him what happened.
- 簡中現在: 趁他还没走。你得告诉他发生了什么。
- 問題: Before you leave 是玩家提醒自己「在你離開之前」就要說，簡中「趁他还没走」把要離開的人換成了對方。
- 建議改法: 在你离开之前。你得告诉他发生了什么。

### 16. Shrouded in Mist#318　quest/shrouded-in-mist.json
- 英文原文: The sweeper did no damage to the monitor...
- 簡中現在: 清扫机完全没伤到监视器……
- 問題: the sweeper 是任務道具掃帚（Frail Sweeper），簡中譯成「清扫机」，讀成機器了。
- 建議改法: 扫帚完全没伤到监视器……

### 17. The Feathers Fly Part II#533　quest/the-feathers-fly-part-ii.json
- 英文原文: How do you not?! They're the one who helped deal with the Factory!
- 簡中現在: 你们怎么会不认识？！就是他们帮忙解决了工厂那件事！
- 問題: They're the one（單數 the one）指玩家一人；「就是他们」讀成一群人。同任務 #532、#569、#606、#1400 的「认识他们」也一樣。
- 建議改法: 你怎么会不认识？！就是这个人帮忙解决了工厂那件事！

### 18. The Feathers Fly Part II#1114　quest/the-feathers-fly-part-ii.json
- 英文原文: They unconscious?
- 簡中現在: 他们昏过去了？
- 問題: Efena 問的是倒在瓦礫下的玩家一人，They unconscious 的 they 是單數；「他们昏过去了」讀成好幾個人。

### 19. The Mercenary#093　quest/the-mercenary.json
- 英文原文: I always preferred his brother anyway. Much quieter.
- 簡中現在: 反正我一直比较喜欢他哥哥。安静多了。
- 問題: his brother 沒交代長幼，簡中寫成「哥哥」。

### 20. The Price of Ingenuity#127　quest/the-price-of-ingenuity.json
- 英文原文: You'd be even harder to deal with if you did, bro. But on that subject- Quill said something about you all practicing... what was the word he used... "magic?"
- 簡中現在: 要是你真有，那就更难搞了，哥。不过说到这个——Quill 提到你们在练习……他用的词是什么来着……“魔法？”
- 問題: 說話的是三兄弟的老大 Yulie，對象是老么 Yuman，bro 在這裡是叫弟弟；「哥」長幼反了（同檔 #534 Yuman 叫 Yulie「哥」才對）。
- 建議改法: 要是你真有，那就更难搞了，老弟。不过说到这个——Quill 提到你们在练习……他用的词是什么来着……“魔法？”

### 21. The Scarred Springs#078　quest/the-scarred-springs.json
- 英文原文: Okay, one sec...and...here! Remember to check them for where to go!
- 簡中現在: 好，等一下……然后……给你！记得看内容手册找要去哪里！
- 問題: them 指 Maripe 剛重寫給玩家的筆記（#076、#077），簡中寫成「内容手册」，對象錯了。
- 建議改法: 记得看笔记找要去哪里！

### 22. True Colours#033　quest/true-colours.json
- 英文原文: A shame it's inescapable, or I'd love to pay a visit. It's true, what they say- there is no escaping its pull once it gets a hold of you!
- 簡中現在: 可惜它无法逃脱，不然我还真想去参观一下。人家说得没错——一旦被它抓住，就再也逃不开它的引力！
- 問題: A shame it's inescapable 是「進去就逃不出來（所以不想去）」，簡中「可惜它无法逃脱」變成監獄自己逃不掉。
- 建議改法: 可惜进去就逃不出来，不然我还真想去参观一下。

---

## 二、名詞錯

### 23. A Hunter's Calling#106　quest/a-hunter-s-calling.json
- 英文原文: This decay's made me terribly sick. I still have a family to feed... Any amount will help.
- 簡中現在: 这衰败让我病得很重。我还有一家人要养……多少都有帮助。
- 問題: This decay 指 Olux 底下那場 Gavel 的黯蝕；簡中語料其他 the decay 都作「黯蚀」，這句作「衰败」。
- 建議改法: 这黯蚀让我病得很重。我还有一家人要养……多少都有帮助。

### 24. Arachnids' Ascent#051　quest/arachnids-ascent.json
- 英文原文: Alright. We'll fight through the spiders in the mountain, then. Captain, Aledar, good luck!
- 簡中現在: 好。那我们就杀穿山里的蜘蛛。上尉、Aledar，祝你们好运！
- 問題: Captain Enduyn 在本任務其他句子都作「队长」，只有這句寫「上尉」。
- 建議改法: 队长、Aledar，祝你们好运！

### 25. Dwarves and Doguns Part I#080　quest/dwarves-and-doguns-part-i.json
- 英文原文: We...are those who...want peace. I am son...of the Dogun Chief... I have...been banished.
- 簡中現在: 我们……是那些……想要和平的人。我是……Dogun 族长的儿子……我已经……被放逐了。
- 問題: Dogun 的首領 Paku 在簡中多作「酋长」（名牌、《临界点》#134、#271、#272 等），這句寫「族长」。
- 建議改法: 我是……Dogun 酋长的儿子……

### 26. Dwarves and Doguns Part I#110　quest/dwarves-and-doguns-part-i.json
- 英文原文: We're going to present this to the Dogun chief as tribute. We need to tidy it up.
- 簡中現在: 我们要把它当作贡品献给 Dogun 族长。得先把它整理干净。
- 問題: 同上一條，Dogun 的首領簡中多作「酋长」。
- 建議改法: 我们要把它当作贡品献给 Dogun 酋长。

### 27. Enter the Dojo#099　quest/enter-the-dojo.json
- 英文原文: ...there's a letter here, too. The script is very ornate...this is addressed from the Mistress of the Hive herself, only a few months ago.
- 簡中現在: ……这里还有一封信。字迹非常华丽……寄信人正是蜂巢的女主人本人，就在几个月前。
- 問題: the Mistress of the Hive 在簡中 label.json、misc.json 作「蜂巢的主母」（10-24 繁簡一起定案），這句作「蜂巢的女主人」。
- 建議改法: 寄信人正是蜂巢的主母本人

### 28. Finding the Light#100　quest/finding-the-light.json
- 英文原文: They just...walked up, after a thousand years of my putting my entire life into trying to save this province, and do everything I couldn't, in the span of weeks at most!
- 簡中現在: 他们就这么……走过来，在我把整个人生都投入到拯救这个省份整整一千年之后，最多不过几个星期，就做到了我做不到的一切！
- 問題: this province 簡中語料多作「行省」，這句寫「省份」。
- 建議改法: 在我把整个人生都投入到拯救这个行省整整一千年之后

### 29. Lost Soles#014　quest/lost-soles.json
- 英文原文: Bring back Abysso Galoshes, received during the quest Beneath the Depths.
- 簡中現在: 带回 Abysso Galoshes，它是在任务 Beneath the Depths 中获得的。
- 問題: 任務名 Beneath the Depths 在簡中 quest-name.json 作「深渊之下」，這句任務目標留了英文。
- 建議改法: 带回 Abysso Galoshes，它是在任务“深渊之下”中获得的。

### 30. Mushroom Man#076　quest/mushroom-man.json
- 英文原文: ...so, Captain, what do you think? We'll need to start training soldiers more quickly, but I hope that won't be a problem.
- 簡中現在: ……那么，上尉，你怎么看？我们得加快训练士兵的速度，希望这不会是个问题。
- 問題: Captain Ragon 在本任務 #072、#080 與其他任務都作「Ragon 队长」，這句寫成「上尉」。
- 建議改法: ……那么，队长，你怎么看？

### 31. Overture to Despair#683　quest/overture-to-despair.json
- 英文原文: You tell her about Orphion. About the Parasite latched on, eating through his radiance. About the struggle to remove it.
- 簡中現在: 你告诉她 Orphion 的事。告诉她那只寄生虫紧紧吸附，啃噬祂的光辉。告诉她为了除掉它的苦战。
- 問題: the Parasite 是光之領域裡附在 Orphion 身上的首領，簡中首領訊息作「寄生体」，這句寫「寄生虫」。
- 建議改法: 告诉她紧紧吸附在祂身上的寄生体，啃噬祂的光辉。

### 32. Shrouded in Mist#165　quest/shrouded-in-mist.json
- 英文原文: Phew. Getting up here really wasn't the easiest... the Red Fox back in Auburn really, ha, pales in comparison.
- 簡中現在: 呼。爬上来真的不太容易……Auburn 的赤狐跟这比，哈，简直不值一提。
- 問題: Red Fox（Auburn 的樹頂咖啡館）簡中多作「红狐」（《一叶知秋》6 處），這句與《绝望的序曲》#229 作「赤狐」。
- 建議改法: 呼。爬上来真的不太容易……Auburn 的红狐跟这比，哈，简直不值一提。

### 33. The Cursed One#403　quest/the-cursed-one.json
- 英文原文: We have also imported the faaaamed Heather Beer from the rich highlands.
- 簡中現在: 我们还从富饶的高地进口了大——名鼎鼎的石南啤酒。
- 問題: Heather Beer 在簡中 misc.json 與同一個攤位的 #404 都留原文，這句譯成「石南啤酒」，同一樣東西兩種說法。
- 建議改法: 我们还从富饶的高地进口了大——名鼎鼎的 Heather Beer。

### 34. The Mercenary#081　quest/the-mercenary.json
- 英文原文: Step aside, Shadow Spears. I'd like to deal with these dogs myself.
- 簡中現在: 让开，暗影之矛。我要亲自对付这两条走狗。
- 問題: Shadow Spears 在簡中洞窟名與名牌都留英文，這句譯成「暗影之矛」。
- 建議改法: 让开，Shadow Spears。我要亲自对付这两条走狗。

### 35. The Strong Survive#743　quest/the-strong-survive.json
- 英文原文: It's a book about a variety of famous Fruman military leaders.
- 簡中現在: 这是一本介绍多位著名 Fruman 军事将领的书。
- 問題: Fruman 在簡中多寫成「Fruma 军队／Fruma 士兵」（同任務 #011、#066），這句留了英文形容詞 Fruman。
- 建議改法: 这是一本介绍多位著名 Fruma 军事将领的书。

### 36. The Thanos Depository#034　quest/the-thanos-depository.json
- 英文原文: New Quest Started: The {p} Depository
- 簡中現在: 新任务开始: {p} 仓库
- 問題: 簡中任務名作「{p} 储藏库」（misc.json），本任務的開始提示與 #025、#028、#031、#039–#042 卻作「仓库」，任務內外兩種說法。
- 建議改法: 新任务开始: {p} 储藏库

---

## 四、簡中沒翻、繁中有翻的句子（只算數量）

簡中空白 461 句，另有 34 句在簡中找不到同一句原文的條目（多半是原文已更新，簡中還留著舊原文，見 [stale-sources.md](stale-sources.md)）。依檔案排列（只列 5 句以上的）：

| 檔案 | 空白 | 沒有條目 |
|---|---:|---:|
| `secret/the-royal-university's-library.json` | 141 | 0 |
| `quest/apotheosis-quest.json` | 29 | 8 |
| `quest/the-qira-hive-quest.json` | 27 | 0 |
| `secret/wynn-plains-monument.json` | 21 | 0 |
| `secret/ruins-of-detlas.json` | 20 | 0 |
| `secret/shadowy-dealings.json` | 20 | 0 |
| `quest/elemental-exercise.json` | 10 | 4 |
| `quest/the-feathers-fly-part-ii.json` | 11 | 3 |
| `secret/headless-not-homeless.json` | 11 | 0 |
| `secret/temple-ruin.json` | 11 | 0 |
| `quest/taking-the-tower.json` | 5 | 4 |
| `secret/water-of-the-past.json` | 9 | 0 |
| `secret/lost-farmers.json` | 8 | 0 |
| `quest/misadventure-on-the-sea.json` | 5 | 2 |
| `quest/black-road-runaround.json` | 5 | 0 |
| `quest/meaningful-holiday.json` | 0 | 5 |
| `quest/the-cursed-one.json` | 3 | 2 |
| `quest/the-missing-piece.json` | 5 | 0 |
| `quest/through-the-pipes.json` | 5 | 0 |
| `secret/hallowed-ground.json` | 5 | 0 |
| 其他 63 個檔 | 合計 116 句 | |
