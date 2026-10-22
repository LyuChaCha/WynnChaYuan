# 第七章　祕密與隱藏內容：探索點、祕密故事、計時賽與隱藏任務

> **劇透提醒**：本章劇透全世界 121 個隱藏探索點（含 12 個終極探索點）的內容、語料 `secret/` 收的五個祕密故事（《Bob 的傳說》〈Maex 黑市〉〈天空的統治者〉〈痛中之慰〉〈呱鳴怪談〉）、任務《???》與已移除的舊版《???》、兩個已移除的 Craftmas 任務、光之領域系列的改版史；輕度劇透《轉世重生》《Bob 的失落之魂》《美妙旅程》《歸鄉之旅》《獵手的呼喚》《Twain 宅邸》。

---

## 一、概述

前六章照地區講故事；這一章換個角度，當成一本**尋寶指南**：哪些東西藏在地圖背面、怎麼打開、打開之後看到的那段歷史在整個世界裡佔什麼位置。

Wynncraft 的「祕密」大致有五種：

1. **隱藏探索點**（Secret Discovery）：全世界 17 區、121 個。每個都是一段被藏起來的歷史，常常要解謎、帶道具或兩人合作才進得去。每區最後一個是**終極探索點**（Ultimate Discovery），要先找齊同區其他探索點才會開。
2. **祕密故事**：走進某些探索點會播放的逐句文字或過場。語料把其中五段單獨收在 `zh_tw/secret/`。
3. **計時賽**（Time Trial）：騎坐騎跑固定賽道的單人挑戰，12 條。
4. **隱藏與移除的任務**：任務清單上只顯示「???」的任務、舊版禁止討論的《???》、節慶限定後被拿掉的 Craftmas 任務。
5. **書、信與彩蛋**：祕密圖書館的套書、Bob 的封緘信、Builder 留下的玩笑。

讀這一章最重要的一件事：**探索點彼此在對話**。Ragni 的書提到「其他和它一樣的圖書館」，Relos 的書說「世界各地還藏著更多」；Bak'al 在紀念碑裡提起 Twain 家；Farcor 送你的書要拿回 Llevigar 去還——遊戲在用探索點把四個行省縫成一張網。第三節按時間把這張網攤開，第六節再逐一列出入口。

出處：wiki〈[Secret Discoveries](https://wynncraft.wiki.gg/wiki/Secret_Discoveries)〉；語料 `discovery-name.json`、`discovery.json`、`secret/*.json`

---

## 二、時間順序：探索點拼出來的世界史

下表只列「靠祕密內容才知道」的事件，年代照 wiki 與語料。各事件的完整來龍去脈在第 01–06 章。

| 年代 | 事件 | 揭露它的祕密 |
|---|---|---|
| 開門前數千年 | 操縱時間的非人種族（Olm）離開平原，留下遺跡 | 亙古遺跡 |
| 兩千多年前 | 兩顆隕石墜入 Gylia Plains，坑積水成湖，Gylia 人稱之「天墜」 | Gylia 的大災變、隕石之印 |
| 開門前約 1500 年 | 沙漠帝國鼎盛；皇帝之杖被供奉於神殿 | 流沙、神殿遺跡 |
| （年代不明） | 皇帝的第二十六子 Hashr 弒父奪杖，沙暴之咒埋掉整個帝國 | 巨石坑、劍之神殿、沙漠王室宅邸 |
| 0 AP | 礦工挖出傳送門；同年 Ahms 的巨像失控，土地碎裂成 Sky Islands | 墜落的守護者 |
| 162 AP | Bak'al 毀掉一座農舍：「這一切，全是為了戰爭的征服大業」 | Bak'al 的毀滅 3 |
| 347 AP | Akias 屠城，只剩一人活著 | Bak'al 的毀滅 2 |
| 464 AP | Nivla 前哨擊退一波怪物後，Bak'al 親自到來 | Bak'al 的毀滅 1 |
| 570 AP | Marius Twain 下葬，四個養子女在墓前大打出手；Twain 家解體 | Twain 家的殞落 |
| 573 AP | 天空劫掠者的將軍寫信給第 356 代女王：矮人開通 Heights 之後空運沒落 | 天空的統治者 |
| 823 AP | Gale Nasin 逃出 Fruma，Nasin 家失勢 | 分裂的家族、痛中之慰 |
| 850 AP | Fruma 的發明家坐熱氣球出逃，成為 Corkus 第一批人類 | 意外的出走、Relos 的祕密圖書館 |
| 854 AP | Bak'al 的大軍攻破 Ragni，Bob 在下水道出生 | 英雄的起點、《Bob 的傳說》 |
| 882–886 AP | Nivla 瞭望塔一座座失守 | 守望者 |
| 886 AP | Ragni 之役：Bob 第一次擊退 Bak'al | Wynn Plains 紀念碑 |
| 約 900 AP | 村民渡海在 Maltic 落腳；Fruma 開始送失憶的新兵到 Wynn | Maltic 的往昔、Relos 的祕密圖書館 |
| 984 AP | Bob 把力量分給三隻寵物、託 Nilrem 偽造死訊，從 Gateway Island 消失 | Nilrem 的封緘信、狼之神殿 |
| 現代（約 1000 AP） | 寄生體纏上 Orphion；Dern 說「我會一直看著」 | Ne du Valeos du Ellach |

出處：wiki 各探索點頁（見出處清單）；語料 `discovery.json`

---

## 三、跨區的暗線

### 1. 祕密圖書館

四座「祕密圖書館」各收一套書，彼此的書簡互相點名：

| 圖書館 | 套書 | 一句話 |
|---|---|---|
| Ragni 的祕密圖書館 | 《Bob 的傳說》十二章 | Wynn 英雄的一生；說明說「這座圖書館，以及其他和它一樣的圖書館，來歷至今仍是個謎」 |
| Lusuco 的祕密圖書館 | 《高貴的 Twain 家》八章 | Marius 收養有天賦的孤兒，四大弟子 Rickeo（火）、Dwendle（風）、Theorick（冰）、Mael（通靈）；最後一頁被撕掉 |
| Llevigar 的祕密圖書館 | Llevigar 大學的歷史課本（Wynn、Gavel、Fruma、海洋史） | 第五章被撕：館員說「黯蝕出現之後，政府不喜歡談黑魔法」 |
| Relos 的祕密圖書館 | 《工業之島》八章 | Corkian 的來歷，與 Factory 的失控；「傳聞世界各地還藏著更多像這樣的圖書館」 |

另有四座非「祕密」系列但同樣藏書的地方：Gylia Watch 圖書館（勝者寫的 Gerten 戰爭史）、宏大典藏（Cinfras）、Aldwell 圖書館（Mistwoods 古史）、皇家大學的圖書館（Fruma 統領的肖像牆）。

**伏筆**：《高貴的 Twain 家》第七章說晚年的 Mael「在一座祕密圖書館裡度過餘生，學習要傳給那個值得的人的技藝」。祕密圖書館是誰蓋的，遊戲一直沒說；Mael 那座會不會就是其中之一，是社群常見的猜測（推測）。

出處：wiki〈[Ragni's Secret Library](https://wynncraft.wiki.gg/wiki/Ragni%27s_Secret_Library)〉〈[Lusuco's Secret Library](https://wynncraft.wiki.gg/wiki/Lusuco%27s_Secret_Library)〉〈[Llevigar's Secret Library](https://wynncraft.wiki.gg/wiki/Llevigar%27s_Secret_Library)〉〈[Relos' Secret Library](https://wynncraft.wiki.gg/wiki/Relos%27_Secret_Library)〉

### 2. Bak'al 的毀滅（一到三）

三個編號探索點的年代是**倒過來**的：1 號是 464 AP、2 號 347 AP、3 號 162 AP。玩家照等級從 1 號往 3 號走，等於一路往前翻 Bak'al 的過去，最後停在年代最早的那句台詞「這一切，全是為了戰爭的征服大業……」（原文 *All in the name of War's conquest*）——這是 Bak'al 在替「戰爭領域」效命的最早證據。2 號的 Akias 屠城裡，他下令燒光全城，要灰燼提醒世人「每一座城市都會落得同樣的下場」；Wynn Plains 紀念碑裡他卻被 Bob 打得撤退，「從此再也沒能踏進任何一座受 Bob 保護的城」。三個廢墟周圍徘徊的幽靈士兵（Phantom Soldier），是同一批死者的殘影。

wiki 冷知識：Akias 影射 1.12 版因違反 Minecraft EULA 而移除的 VIP 城鎮；唯一倖存者是至今還在 Llevigar 港的詐騙商人。

出處：wiki〈[Bak'al's Destruction 1](https://wynncraft.wiki.gg/wiki/Bak%27al%27s_Destruction_1)〉〈[2](https://wynncraft.wiki.gg/wiki/Bak%27al%27s_Destruction_2)〉〈[3](https://wynncraft.wiki.gg/wiki/Bak%27al%27s_Destruction_3)〉〈[Wynn Plains Monument](https://wynncraft.wiki.gg/wiki/Wynn_Plains_Monument)〉

### 3. Twain 家

Nesaak 凍原的十個探索點幾乎都在講同一家人：Theorick 的冰能壓制腐敗（探險家的腐敗研究）、他向居民收貢品（陶輪）、他與養父決裂（Twain 兩斷：「你幾乎什麼都沒教我！……Mael 每次一開口，只會讓我變得更蠢！」）、葬禮上兄弟相殘（Twain 家的殞落）。終極探索點最後，Marius 的亡魂對留下來的 Mael 說：「Twain 家在 Theorick 離開我們那天就結束了。不過你該留在這裡，有個人總有一天會需要你的幫助……」——wiki 認為那個人就是後來找上門的少年 Bob。（本小節的過場台詞語料未收，皆為本章依 wiki 原文意譯。）

這條線往外延伸到兩個地方：Wynn Plains 紀念碑裡 Bak'al 說「Twain 家也對我說過一樣的話，然後和他們一樣倒在我腳下」，Bob 回答「我從 Twain 家的錯誤裡學到了教訓」（本章意譯）；以及《Bob 的傳說》第六、七章，Bob 往南越過暴風雪，找到地圖上沒有的那座大宅——《高貴的 Twain 家》第二章說那座宅邸「藏在傳送門窺探不到的地方」。

出處：wiki〈[Torn in Twain](https://wynncraft.wiki.gg/wiki/Torn_in_Twain)〉〈[The Twains' Downfall](https://wynncraft.wiki.gg/wiki/The_Twains%27_Downfall)〉；第一章

### 4. Bob 留下的東西

Bob 的痕跡散落在四個行省：

- **出生**：英雄的起點（Ragni 下水道的無名女子）。
- **成長**：《Bob 的傳說》。
- **成名**：Wynn Plains 紀念碑（886 AP）。
- **假死**：狼之神殿（「據說他在此喪生於野狼之口，認識他的人對此存疑」）、Bob 之墓（985 AP 憑空出現）。
- **真相**：Nilrem 的封緘信（信文語料未收，以下為本章意譯）。十張殘片拼起來，是 Bob 寫給摯友 Nilrem 的訣別信：「我們的世界遠比想像中渺小……兩股龐大的力量在交戰，而我們這個存在的層面就是戰場。」他要用自己的生命力去封印「遠方一頭黑夜的生物」，請 Nilrem 藏好注入他力量的三隻動物同伴，「偽造我的死亡，造一具身體讓他們埋葬」。署名用的是本名 Robert。
- **足跡**：雲端之上（他在古龍 Farcor 那裡紮過營）、Llevigar 的祕密圖書館（館員說借走《財富或知識》的人「欠了十六年」，「雲端上的龍？聽起來確實像他會去的地方」）。從 984 AP 到現代約 1000 AP，正好十六年。

出處：wiki〈[Sealed Letters](https://wynncraft.wiki.gg/wiki/Sealed_Letters)〉〈[Bob](https://wynncraft.wiki.gg/wiki/Bob)〉〈[Llevigar's Secret Library](https://wynncraft.wiki.gg/wiki/Llevigar%27s_Secret_Library)〉

### 5. 那隻眼睛

Dern 的「眼」在四個區的探索點裡一路逼近：採礦作業（Wynn 南方邊界外潛伏著「比這個行省遭遇過的任何東西都危險」的黑暗）→ 異界的異變（世界與其他界域的連結被撕裂）→ Silent Expanse 的祕密（「那隻眼睛注視著每一個闖入者……以及每一次逃脫的嘗試」）→ Ne du Valeos du Ellach（Dern 對 Orphion 說「我的目光第一次穿透了帷幕……**我會一直看著。**」，本章意譯）。探索點名稱本身是古 Gavel 語，wiki 說意思是「界域之戰」；語料刻意不翻，讓它照原樣顯示。

出處：wiki〈[Ne du Valeos du Ellach](https://wynncraft.wiki.gg/wiki/Ne_du_Valeos_du_Ellach)〉；第五章

### 6. 巨像、隕石與龍

- **巨像**：墜落的守護者（Ahms 的巨像失控，土地碎成 Sky Islands）、荒蕪的神殿（崇拜巨像的文明）、巨像遺跡（「第二尊巨像」，來歷不明）、破損的守護者（峽谷巨像「並非一直是威脅」）。四條合起來暗示：巨像需要有人維護，沒人照顧就會失控（推測）。
- **隕石**：Gylia 的大災變、隕石之印、Gavel 挖掘場、宇宙水晶，以及任務《???》的四色隕石。宇宙魔法「最強、也最危險，尤其對心智」（《???》的實驗日誌）。
- **龍**：鎖鏈巨獸（矮人奴役龍族）、Thanos 軍械庫（屠龍兵器的鍛爐，兵器已不見）、雲端之上（碎裂後離開 Gavel 的古龍 Farcor）、Llevigar 圖書館的「古代睿智巨龍」藏書。

### 7. 換形者、Nasin 家與 Fruma 的另一面

Fruma 的 16 個探索點有三條互相咬合的線：Gale Nasin 的出逃（分裂的家族）→ Zhiraok 日記裡對 Gale 的恨（痛中之慰）；換形者 Azael 與 Gikyo（光與影、初代 Wicker 的火花）→ Croaquin 記得養大他的「怪人」（呱鳴怪談，wiki 推測是 Gikyo）；女王抹去的舊文化（沉沒的紀念碑、埋藏的記錄、受詛者的遺骸）。

### 8. 解碼器

兩個終極探索點會給你「翻譯機」：Wynn Plains 紀念碑給**古 Wynn 語轉寫表**（Ancient Wynnic Transcriber），能讀出 Bob 喊的招式名；Ne du Valeos du Ellach 給**高階 Gavel 語轉寫表**（High Gavellian Transcriber），能讀 Gavel 各地的古文字。祕密內容是在教你讀懂其他祕密內容。

出處：wiki〈[Wynn Plains Monument](https://wynncraft.wiki.gg/wiki/Wynn_Plains_Monument)〉〈[Ne du Valeos du Ellach](https://wynncraft.wiki.gg/wiki/Ne_du_Valeos_du_Ellach)〉

---

## 四、主要人物

| 名字 | 身分 | 在祕密內容中的作用 | 出現於 |
|---|---|---|---|
| Bob（Robert） | Wynn 的英雄 | 十二章傳說的主角；所有「假死」線索的中心 | Ragni 的祕密圖書館、紀念碑、封緘信 |
| Momo | Bob 的養母 | Ragni 最窮的單親媽媽，把地圖和黑色匕首交給他 | 《Bob 的傳說》 |
| Chak／Mael／Ethe／Fier | Bob 的四位大師 | 匕首、弓、法杖、矛；Mael 就是最後一個 Twain | 《Bob 的傳說》、《高貴的 Twain 家》 |
| Nilrem | 巫師，Bob 的摯友 | 替 Bob 偽造死亡；名字倒過來是 Merlin | 封緘信 |
| Bak'al | 腐敗的首領 | 三場「毀滅」與 Ragni 之役 | Bak'al 的毀滅、紀念碑 |
| Marius／Theorick／Mael 等 | Twain 家 | 守護 Wynn 的家族，因理念分裂 | Nesaak 凍原各探索點 |
| Croaquin | 人形巨蛙 | Frog Bog 的「怪談」本尊，受雇嚇人 | 呱鳴怪談 |
| Fia | Auburn 苦艾酒的釀酒人 | 交易的另一端 | 呱鳴怪談 |
| Zhiraok 統領 | Auburn 的統領，Nasin 家 | 日記主人 | 痛中之慰 |
| Arriah 將軍 | 天空劫掠者 | 寫報告給女王 | 天空的統治者 |
| 鳳凰盜獵者、陶藝店老闆 | Maex 的黑市成員 | 帶你進黑市 | Maex 黑市 |
| Farcor | 古龍 | 告訴 Bob 世界的真相 | 雲端之上 |
| Llevigar 檔案管理員 | Llevigar 大學圖書館員 | 記得 Bob 欠書不還 | Llevigar 的祕密圖書館 |
| 計時賽大師 | NPC | 開始坐騎計時賽 | 計時賽 |

---

## 五、伏筆與暗示

- **《Bob 的傳說》第十一章「三個同伴」**：「或許他該替他們熬一帖藥，延長他們的壽命」→ 封緘信裡「我注入了魔法的動物同伴」→ 《轉世重生》的 Cluckles、Mooington、Baab。傳說裡一句閒話，其實是整條 Bob 遺產線的鑰匙。
- **第十二章「一頭正在腐敗這個行省的巨獸」**：傳說的作者以為 Bob 是老了打不過；封緘信揭露他是自願去封印 Dern 的巨獸。傳說寫的是「官方版本」，信才是真相。
- **「是誰立的墓？或許是殺他的人？也可能是他某位大師的法術？」**：兩個都錯——是 Nilrem 照 Bob 的遺願做的（wiki）。作者的猜測本身就是誤導。
- **Bob 的灰色皮膚、無名的外地母親、活了一百三十歲**：傳說只說他「被另眼看待」。wiki 記載社群理論：他有 Olm 血統（推測，未證實）。
- **「Bob，你哭什麼？那裡根本沒有人啊……」**：四位大師在 Fier 身旁排成一列微笑，旁人卻看不見。Mael 能與亡者溝通，而《高貴的 Twain 家》說 Mael 最後「也走了」——這一幕暗示大師們當時可能已不在人世（推測）。
- **四件武器其實是同一把鑰匙**：第五章說「精通一項技藝，才會拿到下一位大師願意收下的那把鑰匙」；每次換師父，武器就變形一次（匕首→弓→法杖→矛）。鑰匙就是武器本身。
- **Zhiraok 的數學口癖**：〈痛中之慰〉裡他說「Reach your endpoint!」（語料：直接推到結論！）、「that's a greater value than my concerns」（那確實比我的顧慮重要多了）、「Who the helix」（哪個螺旋的傢伙）。日記裡他抱怨「腦中不停運算的東西一直漏進說話裡」——這是女王祝福的副作用。譯者要讓這些句子保留「算式腔」，不能修成普通口語。
- **Croaquin「從焦油坑裡爬出來第一眼看到的那個人」**：他以為人人都有這樣的「父母」。換形者的誕生方式，加上「看不出是爸爸還是媽媽」的養育者，wiki 推測是 Gikyo。
- **Croaquin 的雇主「戴著奇怪面具」**：每隔幾週來付酒、問他嚇走了哪些人——一個專門登記闖入沼澤者的情報網；wiki 推測是審訊官或 Two。
- **天空劫掠者的報告**：「自從那些矮人打通了 Heights，空中往來就更稀少了」——地面路線一通，靠劫掠空運維生的人就沒飯吃；這封信預告了 Ahmsord 的起源裡劫掠者撤離的結局。
- **Craftmas Chaos 的結局**：聖誕老人最後說「把那顆水晶給我，我會好好保管」。玩家在未來看到的，正是被水晶腐化、毀掉 Selchar 的聖誕老人——他收下水晶這一刻，就是悲劇的起點（推測，遊戲沒有明說因果）。
- **計時賽的地名雙關**：Tempo Town 的那條叫 Rush Around the Ruins（tempo＝節奏），Llevigar 的叫 Leg Day（健身梗「練腿日」），通往 Dern 的叫 Stygian Sprint（Styx＝冥河）。

---

## 六、祕密與隱藏內容

### 6.1 計時賽（Time Trials）

**是什麼**：單人挑戰，必須騎指定種類的坐騎（馬或飛龍），在預設賽道上依序通過所有檢查點，越快越好。找「計時賽大師」（語料：計時賽大師／開始一場坐騎計時賽）開始。每條賽道有四個評級：**B、A、S、S+**，每級各有時限（成績等於或低於該時限即達成）。B、A、S 三級各有獎勵；**S+ 沒有任何獎勵**，純粹是榮譽。另有「每日獎勵」（wiki 欄位名 Daily Bonus），內容是讓坐騎某項能力的**上限**提高——坐騎的每項能力都有「目前等級／上限／最大值」三個數字，上限決定能練到多高。是否每天只能領一次，wiki 沒有寫明（推測）。

| 賽道（原文） | 暫譯 | 建議等級 | 坐騎與前置 | 地點 | 長度／難度 | 時限 B／A／S／S+ | 特殊獎勵 | 每日獎勵 |
|---|---|---|---|---|---|---|---|---|
| Black Road Runaround | Black Road 兜圈跑 | 17 | 馬；《馬失前蹄》 | Ternaves | 短／易 | 2:00／1:20／1:00／0:40 | B：馬鞍（每帳號一次） | 速度上限 +1 |
| Fast-Paced Pilgrimage | 快步朝聖 | 23 | 馬；《歸來的兄長》 | Saint's Row | 中／中 | 2:00／1:30／1:10／0:45 | A：Bovine Charm | 能量上限 +1 |
| Rush Around the Ruins | 廢墟疾走 | 27 | 馬 | Tempo Town | 中／易 | 2:00／1:20／1:10／0:45 | A：Aberrant Clock | 操控上限 +1 |
| Dash Across the Dunes | 沙丘衝刺 | 31 | 馬 | Invaded Barracks | 中／易 | 2:30／1:25／1:15／0:50 | A：Mercenary's Mail | 動量上限 +1 |
| Llevigar Leg Day | Llevigar 練腿日 | 40 | 馬 | Llevigar Plains | 短／中 | 2:00／1:15／1:05／0:45 | A：馬蹄靴 | 操控上限 +2 |
| Hazardous Hike | 險途健行 | 80 | 馬；《Aldorei 的祕密 第二部》 | Aldorei Town | 中／中 | 2:00／1:30／1:20／0:55 | — | 韌性上限 +3 |
| South Canyon Competition | 南峽谷競速 | 86 | 飛龍；《峽谷嚮導》 | Bantisu Air Temple | 中／中 | 2:45／2:05／1:45／1:20 | B：飛龍韁繩（每帳號一次） | 訓練上限 +2、Powerup 上限 +2 |
| Bremmyn Brustle | （暫留原文） | 88 | 飛龍 | Bremmyn-Hierra | 短／中 | 2:00／1:30／1:20／1:00 | — | 速度上限 +3 |
| Sky-High Soar | 衝天翱翔 | 95 | 飛龍 | Ahmsord | 中／中 | 2:45／2:05／1:45／1:20 | — | 速度上限 +4 |
| Abyssal Aerobatics | 深淵特技飛行 | 97 | 飛龍；《一千米之下》 | Void Village | 中／中 | 2:30／2:00／1:50／1:20 | A：陰鬱玫瑰 ×8、Frying Pan、Large Metal Chunk、遺失的錢幣 | 高度上限 +5 |
| Stygian Sprint | 冥河衝刺（語料） | 103 | 馬；《逐深之旅》 | Portal to Dern | 長／中 | 3:00／2:15／2:05／1:50 | — | 高度上限 +5 |
| Autumnal Athletics | 秋日競技（語料） | 107 | 馬；《歸鄉之旅》 | Auburn Forest | 長／難 | 3:00／2:15／2:05／1:35 | A：無瑕 Liff 葉、熟成金果、珍愛傳家寶、精緻沼澤鱗 | 高度上限 +3、操控上限 +1 |

各級都另有經驗值與綠寶石（例如 Black Road Runaround：B 級 +340 XP、+24 綠寶石；A 級 +40 XP、+96 綠寶石；S 級 +346 綠寶石）。wiki 的數字有一處怪：前五條與最後兩條是 B 級給的經驗多、A 級少，Hazardous Hike 到 Abyssal Aerobatics 卻反過來（A 級是 B 級的四倍），可能是 wiki 記錯欄位，照錄不改。

**藏在獎勵裡的在地梗**：
- 每條的特殊獎勵都跟地方有關：朝聖路上的 Saint's Row 是崇拜聖牛的 Bovemist 教會所在，給的是 Bovine Charm（牛的護符）；時間錯亂的 Tempo Town 給 Aberrant Clock（失常的鐘）；駐軍被攻陷的 Invaded Barracks 給 Mercenary's Mail（傭兵的鎖甲）。
- Abyssal Aerobatics 在虛空裡，給的是陰鬱玫瑰（Voidgloom 的通貨）和一堆「掉進虛空的雜物」——平底鍋、大塊金屬、遺失的錢幣，正對應虛空裡「把各種墜落的殘骸都吸過來」的那片重力異常區。
- Autumnal Athletics 給的四樣都是 Auburn 的特產：Liff 葉（Auburn 人真正的貨幣，Fia 說綠寶石在這裡「跟泥土一樣不值錢」）、金果、沼澤鱗，還有一件「珍愛傳家寶」——讓人想起〈呱鳴怪談〉裡 Fia 遺失的傳家寶（推測，遊戲沒有連結兩者）。
- **兩條有語料的賽道說明**：Stygian Sprint——「{p} 是一條通往傳送門的蜿蜒道路。所以就跟所有道路一樣，最適合拿來競速了！」（往 Dern 的路被拿來飆車，是刻意的黑色幽默）；Autumnal Athletics——「{p} 雖然貧窮，卻是 Fruma 最美麗的地區之一。也難怪有人在這一帶開闢了健行步道。你沒時間健行，不過來場計時賽或許可以。」
- **給譯者**：十二個賽道名**全部是押頭韻**（Black Road Runaround、Fast-Paced Pilgrimage、Dash Across the Dunes、Sky-High Soar……）。中文保不住頭韻，建議改用四字格或疊音（例如「沙丘衝刺」「衝天翱翔」）維持「標語感」，並保留地名雙關（Leg Day 一定要翻出健身梗）。Bremmyn Brustle 的 Brustle 不是英文字（像 bustle／rustle 的混合），暫留原文。

出處：wiki〈[Time Trials](https://wynncraft.wiki.gg/wiki/Time_Trials)〉〈[Mounts](https://wynncraft.wiki.gg/wiki/Mounts)〉〈Bovine Charm〉〈Aberrant Clock〉〈Mercenary's Mail〉；語料 `misc.json`（賽道名與說明）、`npc.json`（計時賽大師）

### 6.2 隱藏探索點怎麼運作

- 每個角色每個探索點只能領一次，經驗值很高、而且**不吃經驗加成**。
- 有最低戰鬥等級；等級不夠時聊天欄會說「你似乎發現了有趣的東西，但要到戰鬥等級 X 才能登記這個發現」。
- 許多要帶道具、右鍵某個方塊或解謎。右鍵沒反應時，wiki 建議換人少的伺服器、走開 5–10 格再回來，或空手右鍵。
- 每區的**終極探索點**要先找齊同區其他探索點。Silent Expanse 另外要完成《彼界之旅》《逐深之旅》並通關一次詭異瞭望台。
- 探索點的敘述文字遊戲本身不送，是 Wynntils 去 wiki 抓來顯示的（語料 `discovery.json` 的說明）。
- 設計靈感來自《薩爾達傳說 曠野之息》的神廟（wiki 引官方推文）。

出處：wiki〈[Secret Discoveries](https://wynncraft.wiki.gg/wiki/Secret_Discoveries)〉

### 6.3 全索引

譯名照 `discovery-name.json`；「需求」欄的道具名語料有譯名的用譯名，其餘照原文；一句話摘自 `discovery.json` 的說明與 wiki 頁面。**粗體**為終極探索點。地區的細節故事見括號裡的章節。

#### Wynn Plains（Lv. 1–20，第一章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Ragni's Secret Library | Ragni 的祕密圖書館 | Bob 之淚 | 把淚丟進 Ragni 城堡壁爐，影子帶你走進畫裡的門；《Bob 的傳說》十二章 |
| Far from the Roots | 遠離根源 | — | 新手第一個探索點：「腐敗在看不見的地方蔓延。黑暗之中、深處之下、心念之間。」 |
| A Hero's Origin | 英雄的起點 | — | 854 AP 躲進下水道的人；Bob 出生的地方 |
| Watchmen | 守望者 | 四座塔都要到 | 882–886 AP 斥候的回聲，一路退到 Ragni 城下 |
| Boulder Breaker | 碎岩者 | — | 躲進地下避難、再也沒能出來的人 |
| Historical Maltic | Maltic 的往昔 | — | 村民在 Wynn 的第一個聚落，人類與村民同盟的起點 |
| Ruins of Detlas | Detlas 遺跡 | — | 舊 Detlas 的六本日記；今天的城是村民幫忙蓋的 |
| Water of the Past | 昔日之水 | — | 被軍隊拋下、終遭屠殺的 Ragni 農民 |
| Bak'al's Destruction 1 | Bak'al 的毀滅 1 | Corrupted Remains | 464 AP，守軍剛擊退一波，Bak'al 親自到來 |
| Bak'al's Destruction 2 | Bak'al 的毀滅 2 | — | 347 AP Akias 屠城，只剩一人 |
| Bak'al's Destruction 3 | Bak'al 的毀滅 3 | — | 162 AP 一座農舍，「這一切，全是為了戰爭的征服大業」 |
| Timeless Ruin | 亙古遺跡 | 神祕晶石 | 比傳送門更古老、會操縱時間的非人種族 |
| Beneath the Roots | 盤根之下 | — | 腐敗尖刺下的根系洞窟：「腐敗就像野草」 |
| Somewhere In Between | 兩者之間 | — | 豬人的由來：尖刺電擊了普通的豬 |
| Crate Crane | 木箱吊車 | — | Nemract 人躲進水溝，仍被找了出來 |
| Mining Operation | 採礦作業 | — | WynnExcavation 挖到比腐敗更危險的南方黑暗 |
| **Wynn Plains Monument** | Wynn Plains 紀念碑 | 其他 16 個 | 886 AP Ragni 之役；古 Wynn 語轉寫表 |

#### Almuj 沙漠（Lv. 25–30，第一章；考古學家 Junes 多半同行）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Quicksand | 流沙 | — | 1500 年前帝國的小圖書館，滿是皇帝的文獻 |
| Temple Ruin | 神殿遺跡 | Machete | 供奉皇帝之杖的神殿，屋頂埋在沙裡 |
| Boulder Pit | 巨石坑 | — | 皇帝第二十六子 Hashr 登場；坑底屍骨不屬於古帝國 |
| Temple of Swords | 劍之神殿 | Ancient Sword | Hashr 藏杖處，多年後成了盜匪窩 |
| Totem Pole | 圖騰柱 | Wooden Eye | 皇帝死後的沙暴把 Eagle Tribe 逼進祈禱洞窟 |
| Wrecking Ball | 拆除鐵球 | Dangling Elevator Rope | Creden Tibus 得杖後兩度搬藏的紙條 |
| Rymek Thief | Rymek 竊賊 | — | 盜匪是腐敗之戰中被逐出城者的後代 |
| **Desert Royal Residence** | 沙漠王室宅邸 | 其他 7 個 | Hashr 弒父，沙暴之咒降臨 |

#### Nesaak 凍原（Lv. 35–45，第一章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Explorer's Corruption Study | 探險家的腐敗研究 | — | Theorick 的冰只能「拖慢」腐敗 |
| Wolf Temple | 狼之神殿 | — | 「Bob 死於野狼」的紀念碑，認識他的人都不信 |
| Frozen Cave Hideout | 冰封洞穴藏身處 | — | Theorick 冰封 Nesaak 時，地下避難的家庭一起被凍住 |
| Nilrem's Sealed Letter | Nilrem 的封緘信 | Mysterious Key ×2 | 破解 Nilrem 的封印，讀 Bob 的訣別信 |
| A Deathly Warning | 死亡的警告 | — | 真正的死神寫給登天之塔冒牌貨的抗議信 |
| The Lone Telescope | 孤獨的望遠鏡 | — | 塔頂水晶「並非來自這個世界」 |
| Pottery Wheel | 陶輪 | — | Theorick 收貢品——而且很愛做陶 |
| Torn in Twain | Twain 兩斷 | — | Theorick 與養父 Marius 決裂 |
| Lusuco's Secret Library | Lusuco 的祕密圖書館 | Dynamite | 炸開冰牆；《高貴的 Twain 家》八章 |
| **The Twains' Downfall** | Twain 家的殞落 | 其他 9 個 | 570 AP 葬禮上的手足相殘 |

#### Llevigar Plains（Lv. 35–40，第二章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Teachings of Old | 古老的教誨 | — | 村民把魔法用在日常雜務的失傳技藝 |
| Abandoned Excavations | 廢棄的挖掘場 | Stone Shovel | 村民幾乎挖光行省的綠寶石，只好往海外找 |
| Unstable Experiments | 不穩定的實驗 | — | 石英吸能實驗愈用愈不穩；《Quartron 崛起》的伏筆 |
| At the Edge of Decay | 黯蝕的邊緣 | — | 北 Gavel 正在凋零，Llevigar 人愈來愈迷信 |
| Spider Ruins | 蜘蛛遺跡 | Woodcutting Lv. 1 | 村民擴張時抹去了大半古神殿 |
| Llevigar's Secret Library | Llevigar 的祕密圖書館 | 依序點三本書 | 大學的隱藏圖書館；Bob 欠書十六年 |
| **Llevigar's Last Hope** | Llevigar 最後的希望 | 其他 6 個 | 災禍接連，村民只能把希望寄託在別處 |

#### Olux Swamp（Lv. 45，第二章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Lost Farmers | 失蹤的農民 | — | 孤零零的農民一個個消失，沒人察覺 |
| Shrouded Lives | 諱莫如深 | — | 從祕密工廠回家的人絕口不提裡面的事 |
| Harmless Worms | 無害之蟲 | Explosive | 溫和的 Grootslang 被什麼東西逼上地面 |
| Decaying Mountains | 黯蝕之山 | — | 黯蝕逼山洞裡的生物往下遷 |
| Shadowy Dealings | 見不得光的交易 | Golem Command Chip | 守衛魔像救不了 Olux 人，只能鋌而走險 |
| Empty Vaults | 空蕩的金庫 | — | Olux 的金庫為了對抗黯蝕早已掏空 |
| **The Bane of Gavel** | Gavel 之禍 | 其他 6 個 | 森林底下，黯蝕的可怕真相 |

#### Dark Forest 與 Kander Forest（Lv. 55–70，第二章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Parasite Cocoon | 寄生體之繭 | Rusted Scythe | 與 Lari 一戰後，寄生體鑽進地底冬眠 |
| The Talor Crypt | Talor 墓穴 | — | 富人的墓穴被黯蝕的死者塞爆而棄置 |
| A Dividing Force | 分隔之力 | — | 兩座森林之間的河，是 Lari 一人撐起的防線 |
| Hallowed Ground | 神聖之地 | — | Lazarus 之坑的水讓死者以不同程度復活 |
| Headless, Not Homeless | 無頭亦有家 | 已開始《無頭歷史》 | Dullahan 復活後奪下富商莊園的記憶 |
| Summoner's Fate | 召喚師的下場 | — | 業餘術士在黯蝕裡練黑魔法……練得很糟 |
| Ways of the Wicked | 邪者之道 | Blood-Infused Jewel | Kander 底下的保皇派（《空洞的寧靜》） |
| **The Heart of the Decay** | 黯蝕之心 | 其他 7 個 | 寄生體的巢藏了兩百年，發現時已太遲 |

#### Light Forest（Lv. 65–70，第二章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Temple of Light | 光之神殿 | 白天 | 精靈向村民傳光之信仰失敗的遺跡 |
| Messengers from Beyond | 彼岸的使者 | 完成《破碎的心智》 | 光透過更高層次的實體與 Gavel 溝通 |
| Light Mushrooms | 光之蘑菇 | — | 形態多變、作用更難捉摸的蘑菇 |
| Sealed Away | 封印之地 | — | Great Stump 屏障之後；Lari 想重獲 Orphion 的眷顧 |
| Birth of Efilim | Efilim 的誕生 | Medicinal Herbs | 觸犯 Aldorei 律法的流放者建立 Efilim |
| Fracture in Reality | 現實的裂隙 | — | 符文合一，Forgery 破土而出，鎮住時空裂隙 |
| **Ne du Valeos du Ellach** | （刻意留原文） | 其他 6 個 | 古 Gavel 語「界域之戰」；寄生體纏上 Orphion |

#### Gylia Plains（Lv. 70，第三章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| The Grand Archive | 宏大典藏 | — | Cinfras 公會大廳的古籍館，「並非所有經卷都收進了這裡」 |
| The Great Barrier | 巨大屏障 | — | 峽谷永不停的地動擋住了寄生生物 |
| The Ancient Ones | 遠古存在 | — | 平原上的石像：是野獸還是石頭？ |
| Mark of the Meteors | 隕石之印 | — | 「天墜」至今影響著村民與 Gerts |
| Gavel Excavation | Gavel 挖掘場 | — | 收集隕石的公司，後來把重心轉向 Wynn |
| Gylia Watch Library | Gylia Watch 圖書館 | 完成《Gerts 大饑荒 第一部》 | 勝者寫的 Gerten 戰爭史：空投腐肉散播疫病 |
| **Gylia's Cataclysm** | Gylia 的大災變 | 其他 6 個 | 兩千年前兩顆隕石墜落成湖 |

#### Canyon of the Lost（Lv. 80，第三章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Canyon Housing | 峽谷聚居地 | — | 寧可住會移動的峽谷，也不要黯蝕 |
| Cosmic Crystals | 宇宙水晶 | — | 來歷人盡皆知，力量卻逼瘋了許多人 |
| Cracked Canyon | 龜裂的峽谷 | — | 每次地震裂縫都擴大，終將釀禍 |
| Thanos Armory | Thanos 軍械庫 | Lava Tank | 打造屠龍兵器的鍛爐，兵器已不見 |
| Loot of the Lost | 失落者的遺物 | — | 大地精把死者的遺物囤起來 |
| **The Broken Protector** | 破損的守護者 | 其他 5 個 | 峽谷巨像不是天生的威脅，有東西讓它失控 |

#### Corkus（Lv. 80–90，第四章；無終極探索點）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Relos' Secret Library | Relos 的祕密圖書館 | 魔電電池 | 《工業之島》八章：Corkian 的來歷與 Factory |
| Lost Avos City | 失落的 Avos 之城 | — | Avos 先祖的紀念碑，藏著他們的古老祕密 |
| Necklace Chamber | 項鍊密室 | Avia 金羽 | Avos 藏起的強大項鍊，直到今天才被找到 |

#### Molten Heights（Lv. 85，第四章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Subterranean Creation | 地底的造物 | — | Doguns 用魔法把洞窟鑿成城市 |
| Maex Black Market | Maex 黑市 | 精緻陶器 | 見 6.4 |
| Cultural Artefact | 文化遺物 | Fired Dogun Tablet | Dogun 的古老石板與〈Dogun 守則〉 |
| Sealed in Ice | 封於寒冰 | Mining Lv. 1 | Dogun 戰爭初期被冰亞龍封住的城 |
| Tomb of the Founders | 開創者之墓 | Kindled Branch | Algard 與 Osseus 之墓，墓誌銘是最大的反諷 |
| Chained Goliaths | 鎖鏈巨獸 | Dwarven Axe | 矮人奴役龍族，如今只剩一頭 |
| **The Doguns' Defeat** | Dogun 的敗亡 | 其他 6 個 | Courag 最後一戰：答應停戰後背信突襲 |

#### Sky Islands（Lv. 90–95，第四章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Otherworldly Origins | 異界的起源 | — | Wybel 是從太空掉下來的？ |
| Desolate Temples | 荒蕪的神殿 | — | 崇拜巨像的原生文明僅存的神殿 |
| The Lonely Abyss | 孤寂的深淵 | 完成《一千米之下》 | 被彈進虛空、漂流的小島 |
| Ahmsord's Origins | Ahmsord 的起源 | — | 劫掠者在村民登陸後放棄 Ahmsord |
| Colossal Ruins | 巨像遺跡 | — | 「第二尊巨像」的殘骸，來歷不明 |
| Rulers of the Skies | 天空的統治者 | — | 見 6.4 |
| Far Above the Clouds | 雲端之上 | Glowing Bean Sprouts | 爬上豆莖見古龍 Farcor；Bob 曾在此求教 |
| **The Fallen Protector** | 墜落的守護者 | 其他 7 個 | 0 AP 巨像失控，Ahms 碎裂墜入虛空 |

#### Silent Expanse（Lv. 100，第五章）

| 原文 | 繁中 | 需求 | 一句話 |
|---|---|---|---|
| Mining Cessation | 停工的礦坑 | Mining Lv. 10 | 礦工鑿穿進 Silent Expanse，把自己封死在裡面 |
| A Wynnic Excavation | Wynn 的挖掘場 | — | 岩壁裡不只有綠寶石 |
| Fate of the Olm | Olm 的命運 | Tomb Key、兩名玩家 | Olm 先知的光之手鐲，後人沒能取出 |
| The Final Moments | 最後的時刻 | — | 傳送門闖入後，一頭異界野獸圍城 |
| Otherworldly Occurrence | 異界的異變 | — | 世界與其他界域的連結被撕裂扭曲 |
| The Forest has Eyes | 森林有眼 | — | Lutho 每有一名礦工失去自我，樹上就長出一雙眼 |
| A Monument of Hope | 希望的紀念碑 | — | 光豎起巨石碑，護住 Lutho 礦工的肉身 |
| Toxic Bounce | 劇毒彈跳 | Explosive Heart | Dern 讓怪物與地景融為一體 |
| Space Between Realities | 現實之間 | — | 實體界與影響界之間的虛無 |
| **Silent Expanse's Secret** | Silent Expanse 的祕密 | 其他 9 個＋主線＋詭異瞭望台 | 那隻眼睛注視著所有闖入與逃脫 |

#### Fruma（Lv. 104–118，第六章；四區都沒有終極探索點）

| 原文 | 繁中 | 地區 | 需求 | 一句話 |
|---|---|---|---|---|
| Washed Away | 隨波而逝 | Auburn | — | Espren 的鐘聲曾是暗號，直到 250 年前被沖走 |
| Solace in Pain | 痛中之慰 | Auburn | 完成《歸鄉之旅》 | 見 6.4 |
| A Croaking Cryptid | 呱鳴怪談 | Auburn | Auburn 苦艾酒 | 見 6.4 |
| Remains of the Cursed | 受詛者的遺骸 | Auburn | Mysterious Weapon | 反抗軍用「職業」分工——玩家職業制的由來 |
| An Unexpected Departure | 意外的出走 | Roughworks | Rake | 逃離 Fruma 的發明家成了 Corkus 第一批人類 |
| Life Before Technology | 科技之前的生活 | Roughworks | Dulled Blade | 農業時代，一位天才少女的發明改變一切 |
| Hidden In Plain Sight | 近在眼前 | Roughworks | Seeds | 廢棄鐵道的新住民 |
| The Great Sap Siphon | 巨型汲液塔 | Roughworks | Old Scraper | 就連樹也只為製造而存在 |
| Light and Shadow | 光與影 | Mistwoods | 狐、梟、鹿三副面具 | 樹冠首領 Azael 是換形者，Gikyo 的手足 |
| Aldwell Library | Aldwell 圖書館 | Mistwoods | 四枚德行之樹花瓣 | 只有少數人知道的森林古史 |
| First Wicker's Spark | 初代 Wicker 的火花 | Mistwoods | — | Fort Hegea：懲罰各族，也懲罰換形者 |
| A People's Sovereign | 屬於人民的統領 | Highlands | 48 Emerald Blocks | Majin 上任前，統領會聽人民說話 |
| A House Divided | 分裂的家族 | Highlands | Rusty Key Ring | 約兩百年前 Gale Nasin 出逃，Nasin 家到今天才開始復原 |
| Sunken Monument | 沉沒的紀念碑 | Highlands | 3 Coiled Stones | 女王之前，Fruma 有過另一種文化 |
| Buried Records | 埋藏的記錄 | Highlands | Heavy Ice Block | Nii、Az、Tol、Uth、Ek：失傳的力量之詞 |
| The Royal University's Library | 皇家大學的圖書館 | Highlands | Library Permission Slip | 歷任統領的肖像牆——少了 Two |

出處：wiki〈[Secret Discoveries](https://wynncraft.wiki.gg/wiki/Secret_Discoveries)〉及各探索點頁（infobox 的 lore 欄）；語料 `discovery-name.json`、`discovery.json`

### 6.4 五個祕密故事

語料 `zh_tw/secret/` 收了五段故事。引號裡的台詞，語料有收的照語料譯文；語料沒收的（觸發過程中 NPC 的閒聊、Zhiraok 的日記內文、Croaquin 的大部分問答）由本章依 wiki 原文意譯，較長的段落另有註明。

#### 一、《Bob 的傳說》（Ragni 的祕密圖書館）

**怎麼觸發**：殭屍 Bob、Nesaak 森林的怪物或世界事件會掉「Bob 之淚」。到 Ragni 城堡的壁爐中央丟下一滴，會聽到「求知者，追隨我的腳步，便得智慧。」（語料），一道影子帶你去宴會廳那幅城堡畫，畫裡的門打開，就是掛著十二章書的走廊。等級 1 就能領。

**故事**：854 AP，Bak'al 的大軍攻破 Ragni，城裡的人逃進通往監獄的下水道——「人類所能造出最污穢的地方」。一名「從未說出自己名字、也沒說從哪裡來」的女子（「她肯定不是 Ragni 本地人」）在冰冷的地上生下男嬰後死去。孩子活了下來，被城裡最窮的單親媽媽 Momo 收養，取名 Robert。他膚色帶灰，總被另眼看待，在麵包坊送貨時順手摸東西。

866 AP，獸潮再度入城。十二歲的 Robert 衝回家，在地窖看見倒在血泊裡的 Momo——母子早在他八歲時就約好：萬一她被感染，要由他親手結束她的生命。Momo 臨死前塞給他一張沾血的地圖、一對刻著紫光符號的漆黑匕首。

地圖上畫著四個地方、四個數字：

1. 東方沙地（匕首）→ 刺客大師 **Chak**，五年。
2. 往南越過暴風雪、「任何地圖上都沒見過的角落」（弓）→ 弓術大師 **Mael**，五年。他的大宅住滿了來求療癒的亡靈，Robert 偷偷希望能和母親說上話，卻從沒說出口。
3. 小時候差點走不出來的蜘蛛森林（一道線）→ 魔法大師 **Ethe**，在樹上的村落，五年。「你的旅程已經走了一半，別讓挫折扭曲了你。」
4. 剛被重創的 Troms（矛）→ 長矛大師 **Fier**，站在火場裡對求救的人視而不見，五年。

每換一位大師，舊武器就在熟悉的光芒裡變成新武器。在 Troms，他第一次跟「不是嚴厲大師的普通人」相處，人們叫他 Bob——「不知道欸，我覺得叫你 Bob 聽起來比較順。」訓練結束那天，四位大師在 Fier 身旁排成一列對他微笑，身後卻有人問：「Bob，你哭什麼？那裡根本沒有人啊……」

此後他成了「Wynn 的專屬半神」，守了一個多世紀，從不把本事傳人，怕力量被用錯。他擔心萬一自己走了誰來接替——「肯定不會是那三個同伴，他深愛的那些動物。或許他該替他們熬一帖藥，延長他們的壽命」。984 AP 他消失了；傳說他聽說有一頭巨獸在腐敗這個行省，想去除掉它，「只是人已年邁，或許力有未逮」。一年後，他的墓出現在雪地裡。「或許有一天，會有人追隨他的腳步。」

**暗示怎麼讀**：
- 這是書中書。敘述者用「我猜」「恐怕」「我們的歷史」說話，是一位不知名的 Wynn 作者寫的**通俗傳記**，不是全知真相。第十二章對 Bob 死因的推測，被 Nilrem 的封緘信整個推翻（見第三節第 4 小節）。
- 「三個同伴」「延長壽命的藥」→ 他把力量注入了 Cluckles、Mooington、Baab（《轉世重生》）。
- 「那次他……算了，這個就別提了」（第八章，想到毒就想到的往事）是敘述者故意吊胃口，遊戲沒有交代是什麼。
- 第二站的大宅就是 Twain 宅邸，Mael 是最後一個 Twain；Marius 的亡魂早就叮囑 Mael 留下來等「總有一天會需要你的人」。
- 最後一句「或許有一天，會有人追隨他的腳步」是對玩家說的；入口那句「追隨我的腳步」也是。

**給譯者**：語料把年份與年齡都參數化成 `{~}`，原文分別是 854 AP、6 天、12 歲、8 歲、四位大師、每位五年、17 歲、27 歲、15 年、3 個同伴、984 AP。數字前後自洽：12 歲出發，三位大師之後 27 歲、離開文明 15 年；再跟 Fier 學五年是 32 歲，854＋32＝886 AP，正好是 Ragni 之役那年。翻譯時不要把這些數字改成概數。

出處：祕密故事《Bob 的傳說》（`secret/the-legend-of-bob.json`）；wiki〈[Ragni's Secret Library](https://wynncraft.wiki.gg/wiki/Ragni%27s_Secret_Library)〉〈[Bob](https://wynncraft.wiki.gg/wiki/Bob)〉〈[Bob's Tear](https://wynncraft.wiki.gg/wiki/Bob%27s_Tear)〉〈[Bob's Tomb](https://wynncraft.wiki.gg/wiki/Bob%27s_Tomb)〉

#### 二、〈Maex 黑市〉（Molten Heights）

**怎麼觸發**（Lv. 85）：在 Upper Molten Heights 找到一名鳳凰盜獵者，他偷了一顆鳳凰蛋。跟著他走，他會越來越火大：「你，退後，就當什麼都沒發生過」→「說真的，怪胎，你給我走開」→「給我滾遠一點！！」最後他躲進電梯，「一拳打在電梯內側的控制面板上」，留下一件陶器。撿到「精緻陶器」，拿去 Maex 的陶藝店。老闆本來說「我們的陶藝課已經額滿了」，看到陶器馬上改口：「其實，我剛想起來，我們還有一個空位……如果你想參加我們的 *咳*『陶藝課』，就從我身後那扇門進去。」

**黑市裡有什麼**：
- 黑暗工匠，賣在「七十三個司法管轄區都被列為禁品」的 Kirschblute（德文「櫻花」），「它的主人們往往會去……嗯，去他們不太開心的地方」。
- Dogun 商人，賣冰凍的活 Dogun：「把 Dogun 澆濕就會變成這樣。不過他們還活著。只是處在極致的痛苦之中。」倒岩漿就會解凍——「嗯，是會尖叫。嗯，其實兩者都有。」
- 迷幻藥販、器官商（「全都採收自我們的『志願者』」）、毒藥釀造大師（「俗話說得好……毒藥自己挑」）、「Gavel 最頂尖的刺客」。
- 盜獵者也在這裡擺攤，一看到你：「來買正宗的鳳凰蛋喔！正——宗——的！正——不，又是你！滾開！」
- 離開前老闆叮嚀：「要是被守衛發現，我們就完蛋了。」

**暗示怎麼讀**：探索點說明的關鍵句是「這些人多半只是想在**王室的緊縮政策**下活下去」——矮人社會表面光鮮，底層靠黑市過活。冰凍 Dogun 的生意則是 Dogun 戰爭的餘毒：戰敗的民族成了商品。迷幻藥販背後有一本《逃稅入門》（作者 Ta Xevade＝tax evade，wiki）。wiki 也注意到，玩家對迷幻藥販的回應選項比對器官商還兇。

**給譯者**：語料用「泥好，窩看泥」模擬黑暗工匠的德式口音，是好的處理；「pick your poison」本是「任君挑選」的慣用語，在毒藥攤字面成立，語料「毒藥自己挑」兩層都保住了。

出處：祕密故事〈Maex 黑市〉（`secret/maex-black-market.json`）；wiki〈[Maex Black Market](https://wynncraft.wiki.gg/wiki/Maex_Black_Market)〉

#### 三、〈天空的統治者〉（Sky Islands）

**怎麼觸發**（Lv. 90）：在天空劫掠者基地，沿藤蔓爬上塔基、走樓梯到飛船旁，右鍵粒子弄斷纜繩，讓飛船撞向牆壁；再從鷹架爬進撞出來的洞。wiki 索引寫作 Rulers of the Skies，條目頁是 Ruler of the Skies（單數），語料兩種都收，譯名同為「天空的統治者」。

**故事**：只有一封舊報告。

> 致: 第 356 代天空劫掠者女王，573 AP。
> 喂，女王陛下，很抱歉打擾您，但我們這邊的情況有點糟。
> 自從那些矮人打通了 Heights，空中往來就更稀少了。我們在這一帶的勢力正在衰退。
> 我們在自己的地盤已經撐不下去了，可能得把人馬撤到主基地去。先跟您說一聲。
> Arriah 將軍 敬上
> （〈天空的統治者〉；數字依 wiki 補回）

**暗示怎麼讀**：
- 「第 356 代」女王——劫掠者是個傳承極久的王朝，而且是女王制（遊戲裡也有「天空劫掠者女王」這個首領）。
- 劫掠者靠打劫空運為生；矮人打通 Molten Heights 的地面通道後，沒人需要冒險飛越群島，獵物就沒了。對照同區的〈Ahmsord 的起源〉：劫掠者在村民軍隊的壓力下放棄 Ahmsord，「一如其他前哨」。這封信是他們衰落的第一個徵兆。
- 將軍的口音很重（op'ned、ta、'ere），語料用「喂」「先跟您說一聲」維持粗人寫公文的反差；報告的格式卻很正式，這個落差就是笑點。

出處：祕密故事〈天空的統治者〉（`secret/ruler-of-the-skies.json`）；wiki〈[Ruler of the Skies](https://wynncraft.wiki.gg/wiki/Ruler_of_the_Skies)〉

#### 四、〈痛中之慰〉（Auburn Forest，Espren）

**怎麼觸發**（Lv. 104，要完成《歸鄉之旅》）：Espren 東側是 Zhiraok 統領宅邸的入口，有兩名衛兵、一位園丁、一位市民。先跟市民聊（「覺得被人盯著？」——兩個衛兵從來不動，「除非有人報案」）或跟園丁聊（「你那麼想聊天，怎麼不去報個案？」）。然後向衛兵**謊報**「我在鎮上看到受詛者了！」或「有人在嘲笑我們的女王」，兩名衛兵會一起離開。趁這段時間踩著長椅、沿樹梢的綠色粒子爬上陽台，進入 Zhiraok 的房間（背景音樂〈Solemn Oath〉）。

房裡有信、日記、兩座衣櫃。調查完再讀日記，「從頭讀」會讀到一半就聽見腳步聲——十秒內要躲進三格的衣櫃（五格的那座「全滿了」）。Zhiraok 進門：

> 有人嗎？Xao？我好像聽到樓上有人。……是我剛才把這個放在外面的嗎？……他絕不會把我的辦公室弄得這麼亂。……到底是哪個螺旋的傢伙在這裡？！
> （〈痛中之慰〉）

他翻遍房間，正要打開你藏身的衣櫃，衛兵衝進來報告「鎮上出現受詛者！太多起了，不像巧合」——就是你剛才的謊報。他說「直接推到結論！我自己也有一堆問題要解」，又改口「那確實比我的顧慮重要多了。好，跟著我行動」，離開。你這才能讀完日記。

**日記內容**（語料未收，以下為本章依 wiki 原文意譯）：
- 開頭：「你永遠不該忘記。一刻也不能忘記，你得到此生最大機會的那一天……陛下把你的靈魂拆開、重新排列，讓你走上通往完美的路。」那天他在「不朽的旁觀者」包圍下痛得幾乎昏厥，其他統領都無動於衷，「只有 Soosu 不一樣——那是憐憫嗎？」從此他腦後一直有一種「針刺感，像一個洞，另一頭有雷電在劈啪作響」，只要停下來就會被撕大。「痛苦值得換取進步。任何程度的痛苦，都值得任何程度的進步。」
- 隨機頁一（恨）：咒罵 Gale——「你、你、你、你。要是你從來沒存在過就好了。」她的出逃讓 Nasin 家失勢。
- 隨機頁二（說話）：他發現腦中不停的運算已經漏進說話、甚至漏進文字，「前幾天我只是想回答幾個孩子的問題，結果把他們徹底搞糊塗了。如果我只會說數字，要怎麼照顧人民的需求？」
- 隨機頁三（受詛者）：Fort Torann 被毀，他算不出錯在哪裡。「受祝福者能建設，受詛者總是破壞。差別在哪？」並寫下一句拉丁文 *Purificatus non consumptus*（淨化而非吞噬）。
- 隨機頁四（榮譽）：園丁一直「熱切地欣賞他」，讓他分心；「我希望人們因為我解決的問題而尊敬我，而不是因為長得好看」；「重點是讓我的名字有意義！要讓人把我的解法和 Nasin 這個姓氏連在一起。那才是終點（endpoint）。」

衣櫃躲過之後，書架上還有「更多日記……每一本的第一頁都是同一段」——他每天都重抄那段「你永遠不該忘記」。

**暗示怎麼讀**：
- 探索點名「痛中之慰」就是日記的核心：他把祝福的痛當成鞭策自己的東西。說明那句「對某些人而言……不過是些軼聞；但對另一些人來說，並非如此」，wiki 解讀為：Zhiraok、Majin 覺得值得，Soosu 痛恨，Gikyo 被逼瘋。
- 「endpoint」「greater value」「helix」「sequence」「terminated」——他滿嘴算式與程式語彙，正是日記說的副作用。語料「直接推到結論！」「哪個螺旋的傢伙」保留了一半；建議譯者在他的所有台詞裡維持這種「理工腔」。原文 *Who the helix* 是 *Who the hell* 的諧音，語料若改成「到底是哪個螺旋的鬼」可以同時保住咒罵感（建議）。
- 你用來引開衛兵的謊言（「受詛者出現了」）正好救了你——而 Zhiraok 對受詛者的焦慮，日記裡寫得清清楚楚。玩家本身就是受詛者，這層諷刺是刻意的。
- 園丁說「我可能是 Auburn 最幸運的人！你不覺得他很了不起嗎？而且還很好看！」對上日記第四頁，兩邊互相不知道對方的心事。

出處：祕密故事〈痛中之慰〉（`secret/solace-in-pain.json`）；wiki〈[Solace in Pain](https://wynncraft.wiki.gg/wiki/Solace_in_Pain)〉；第六章

#### 五、〈呱鳴怪談〉（Auburn Forest，Frog Bog）

**怎麼觸發**（Lv. 104，要完成《歸鄉之旅》）：

1. Frog Bog 有一扇門，門把是青蛙形狀。敲門，門上窺孔出現一隻橫瞳的黃眼睛：「有人在嗎？……怎樣？」選「……嗨？」再選「你看起來很失望……」。門後的「???」抱怨：「他們已經他呱～～的好幾百年沒付我該拿的那份了！」雇主平常用「一瓶瓶好喝的綠色液體」付他，最近一個月都沒來；要你去沼澤東邊找。
2. 如果你拿液態綠寶石給他：「『液化綠寶石』……你們這些人拿這個來『喝』？」
3. 往東找到唯一一家釀 Auburn 苦艾酒的 Fia。酒很難喝，「大概只有一個人固定來買，買很多」，所以她發了財，一瓶要五百片 Liff 葉。你問能不能付綠寶石，她大驚：「綠寶石不是一文不值嗎？！」最後她要你拿「沾泥的鏟子」幫她找家傳寶。
4. 拿著鏟子出門，地上會出現閃光點。挖出來的東西各有一句：酷石頭、舊靴子、一顆畫了笑臉的石頭（Fia 在背後拚命搖頭，你趕快埋回去）、寫著別人名字的項鍊……真正的家傳寶是一個綁了繩子的苦艾酒瓶蓋，刻著「Icsul 家族釀酒廠——50 KA」，是她家第一瓶苦艾酒的瓶蓋。「我們家做這行七百五十年了……可大部分客人喝完都很嫌棄。」
5. 拿苦艾酒回到門前：「喔？對，看起來就是這玩意兒，試了才知道！」「沒錯……進來吧，老兄。咱們聊聊。」

門後是「青蛙堡」（Fort Frog），一隻戴禮帽、抽雪茄、喝苦艾酒的人形巨蛙 Croaquin（讀音「像 Joaquin，但是 CROAK」）。可以問他（語料只收了其中幾句，其餘為本章依 wiki 意譯）：

- **你是什麼？**「我大概是隻青蛙吧？只是大很多，還會講人話。」他不知道自己為什麼跟其他青蛙不同，「他們又不會講人話，也不穿衣服。也不會抽雪茄。也不會喝……呃……這玩意兒叫啥來著？Auburn 苦艾酒？對。」
- **你從哪裡來？** 記不得童年，只記得被「一個怪人」養大，「看不出是爸爸還是媽媽」，「他們好像很以身為我的家長為榮」，旁人卻不太高興。追問「你有父母？」他理所當然地說：「就是你從焦油坑裡爬出來，第一眼看到的那個人嘛。」
- **「他們」是誰？** 每隔幾週有個「戴著奇怪面具的人」送酒來，問他嚇跑了哪些人。「別擔心，我不會跟他們講你的事。」
- **認識 Eih 教授嗎？**（完成《破碎的心智》才有）「戴高帽的蟾蜍……在彩虹森林……講一堆謎語？老兄，你那時是他呱～～的嗑嗨了還是怎樣？」「呃。對。」「呃。真尷尬。」
- **口袋在抖**（身上有會說話的蘑菇）：蘑菇大叫「你是不是有吸引怪東西的磁力？會說話的青蛙？戴帽子抽雪茄？」Croaquin 回嘴：「你也好意思說？香菇什麼時候會講話了？」「……說得好。」

**暗示怎麼讀**：
- 說明文說沼澤巨蛙「背後藏著一個誰也沒料到的真相」——真相不是怪物，而是一個被派來看門的、寂寞的、說不清自己是誰的生物。
- 「焦油坑」與「看不出性別、很驕傲的養育者」→ 換形者的誕生；wiki 推測養育者是 Gikyo（非二元性別）。Croaquin 很可能是某種換形者實驗或後代（推測）。
- 「戴面具的人」記錄所有接近沼澤的人 → wiki 推測是審訊官或 Two。沼澤巨蛙的傳說，可能是有人刻意維持的（推測）。
- Fia 的家傳寶標著「50 KA」，她說家族釀酒七百五十年：Fruma 曆的換算線索，可以拿來對照第一章的曆法表。
- Fia 的「Any stories?」選項：她小時候不聽家人警告闖進深沼，「全靠狐狸們的恩典才撿回一命」——狐狸是 Fruma 反抗勢力的象徵之一（Espren 的狐狸臉、樹冠的狐面具），這句可能不只是客套（推測）。

**給譯者**：Croaquin 的口頭禪 *crrrroakin'* 是 *f\*\*\*in'* 的蛙叫版髒話，語料用「他呱～～的」對應「他媽的」，諧音與粗口都到位，請全文統一。他的 *dey／dem／da*（they／them／the）口音，語料用「啥」「咱們」「老兄」的口語處理，沒有硬造錯字，是合理的取捨。

出處：祕密故事〈呱鳴怪談〉（`secret/a-croaking-cryptid.json`）；wiki〈[A Croaking Cryptid](https://wynncraft.wiki.gg/wiki/A_Croaking_Cryptid)〉；第六章

### 6.5 隱藏與移除的任務

**任務清單上只顯示「???」的任務**有兩個：

- **《???》**（Lv. 80，Gavel）：沒有任務指示，全靠世界裡的線索解。要三名玩家同時站在紅、藍、黃三顆隕石裡的壓力板上才會開始，第三段要四人。內容是 Yahya、Nohno、Mehme 三兄弟的宇宙魔法實驗室；終點是白色隕石裡的 Mehme：「歡迎來到我美妙的木頭小屋！只要你身上沒有蘑菇或雞，這裡就歡迎你。我討厭那些東西。只要我的家和我就好。……孤零零的。」（語料）實驗日誌說隕石的力量讓 Yahya 迷上蘑菇、Nohno 迷上雞，Nohno 擔心自己也會被影響；隱藏房間透露還有第四個兄弟 Sohso。開場聊天欄的 Zalgo 亂碼其實是兩條提示：「被遺忘的科學家要一件餐具」「瘋子最愛的食物藏在一個視角會變的地方」。詳見第三章。
- **《獵手的呼喚》**：起點是 Portal to Dern 前一個標著「???」的存在，清單上也顯示為 ???。詳見第五章。

**已移除或改版的任務**（wiki〈Quests〉的移除表）：

| 任務 | 時期 | 內容與去向 |
|---|---|---|
| 舊版《???》 | Gavel 更新～1.14.1 | 在遊戲內或網路上**洩漏任何資訊都會被封禁**，只准同階段的玩家組隊合作。收集三片隕石碎片、在三座天文台夜觀星座拼出密碼 1754、進白色隕石見 Mehme。1.14.1 改成現在的版本，才解除禁令。 |
| Craftmas Quest | 2013 年冬季限定，1.11 移除 | 聖誕老人一行在環遊世界準備過節的路上被岩漿史萊姆襲擊，燒掉了衣服和鬍子；Almuj 的助手要你帶皮革和紅色粉末來做新衣，聖誕老人就躲在助手的酒窖裡。獎勵「聖誕老人的褲子」——任務拿掉後，這條褲子成了收藏品。部分內容改寫進《佳節有情》。 |
| Craftmas Chaos | 2.0.3 移除 | 見下。部分內容併入 2024 年起的暴雪節（Festival of the Blizzard）。 |
| 光之領域前身（The Worm Holes、Taproot、The Headless Hunt、Finding The Light 的 1.19 版） | 1.20 取代 | Gavel Reborn 更新把四個各自獨立的 Gavel 任務重寫成《光之領域》系列 I–IV，再接第五部《光之領域》。之後系列名的「Realm of Light I – 」前綴又拿掉，現名為《蟲洞》《斬草除根》《無頭歷史》《尋光》《光之領域》（語料進度字串仍是「光之領域 第一部」到「第五部」）。所以「被移除的」是 1.19 的舊版，不是這條故事線本身。劇情見第二章。 |

**Craftmas Chaos 的故事**：Selchar 的 Tom 說聖誕老人三十年沒來過這座島。玩家找到住在糖果手杖島上的聖誕老人，他說馴鹿受不了 Selchar 周圍的邪惡氣息，借你壁爐傳送門回到過去。你在過去遇見十歲的 Tommy，跟著黑袍人進「Selchar 之心」的洞窟，打碎召喚師的腐敗遺物石。回程卻**多跳了六十年**：未來的 Selchar 只剩灰燼，老 Tom 說「兇手是我以前的偶像——聖誕老人」，他紅著眼、帶著體內長滿深紅水晶的精靈屠城。你殺了被腐化的聖誕老人，帶回破布為證。回到現在，聖誕老人說你遲到是因為「帶著那顆黑暗水晶穿越時間」，然後要你把水晶交給他保管——也就是說，你可能親手把未來的災難交到了他手上（推測）。「別告訴任何人你看到了什麼，知道未來是很危險的。」

出處：wiki〈[???](https://wynncraft.wiki.gg/wiki/%3F%3F%3F)〉〈[??? (1.14)](https://wynncraft.wiki.gg/wiki/%3F%3F%3F_(1.14))〉〈[Craftmas Quest](https://wynncraft.wiki.gg/wiki/Craftmas_Quest)〉〈[Craftmas Chaos](https://wynncraft.wiki.gg/wiki/Craftmas_Chaos)〉〈[Quests](https://wynncraft.wiki.gg/wiki/Quests)〉〈[Finding the Light](https://wynncraft.wiki.gg/wiki/Finding_the_Light)〉；語料 `quest-name.json`、`misc.json`

### 6.6 迷你任務

1.18 經濟更新加入的極短任務，wiki 記載目前 125 個，每隔幾級就有一個，大約十分鐘。分兩種：**討伐告示**（Slaying Post，要你帶回某種怪物的掉落物）與**採集告示**（Gathering Post，要你帶回某種原木、礦石、魚或作物）。只有一個階段，**沒有故事也沒有謎題**，所以本章不展開。實際上不一定要殺指定的怪：例如「獵殺哞菇」要的褐蘑菇，從任何掉落褐蘑菇的怪或交易取得都行。Fruma 另有「收集 Liff 葉」這類在地迷你任務。語料：任務名在 `misc.json`，告示說明在 `mini-quest.json`。

出處：wiki〈[Quests#Mini-Quests](https://wynncraft.wiki.gg/wiki/Quests#Mini-Quests)〉；語料 `mini-quest.json`、`misc.json`

### 6.7 書、信與彩蛋

- **封緘信的十張殘片**：原本藏在遺物神殿的頭盔、矛、靴子、法杖、胸甲、護腿、匕首、弓等遺物裡；神殿在 1.17 移除，1.19 起信又能取得。第七張的內容已知但拿不到，第十張至今無法取得。拼起來就是 Bob 的訣別信。《美妙旅程》在 Gateway Island 的帳篷裡留了一張「親愛的 Nilrem……」的未完成信，證明 Bob 到過那裡。
- **《財富或知識》**：在雲端之上從 Farcor 手中拿到，帶去 Llevigar 的祕密圖書館還，檔案管理員會現身：「欠了十六年……既然你肯定不是借書的那位，我就不罰你了。」可以問他 Gavel 的「表親之戰」（影射英國玫瑰戰爭）、Fruma 的統領制度（他只知道 900 AP 有位「Selium 統領」出使 Ragni），以及他最愛的書《Sweeney S. Greenville 漫長而痛苦的死亡》。這段是為了紀念已移除的任務《算命師》而做的。
- **wiki〈Easter Eggs〉列的小彩蛋**：Ragni 銀行三樓的祕密綠寶石房；Little Wood 小屋與「Gone to travel -Martyn <3」的牌子（致敬 Yogscast 的 Martyn）；Almuj 瀑布後的黑市商人；Ternaves 的 Bovemist 教堂裡有一顆被命名為「The」的凋零骷髏，原本五座教堂各有一顆，拼成「The Lord Got Lost In Memories」，其他教堂改版後只剩這一顆；Freezing Heights 的雪人；Llevigar 銀行旁寫著「I need a hug.」的告示牌。
- **其他章已介紹的彩蛋**：〈死亡的警告〉全大寫的死神來信、Nilrem＝Merlin 倒寫（第一章）；《???》的 Squiggles 客串、Cinfras／Thanos 等地名的變位字（第三章）；海洋的 Potato Island 與 Isles of Fiction（第四章）。

出處：wiki〈[Sealed Letters](https://wynncraft.wiki.gg/wiki/Sealed_Letters)〉〈[Llevigar's Secret Library](https://wynncraft.wiki.gg/wiki/Llevigar%27s_Secret_Library)〉〈[Easter Eggs](https://wynncraft.wiki.gg/wiki/Easter_Eggs)〉

---

## 七、本章新出現的譯名

| 原文 | 本章用語 | 備註 |
|---|---|---|
| Time Trial Master | 計時賽大師 | 語料已有（`npc.json`） |
| Rank B／A／S／S+ | B 級／A 級／S 級／S+ 級 | 照原文字母 |
| Daily Bonus（計時賽） | 每日獎勵 | 語料 Lootrun 的 Daily Bonus 已用此譯 |
| Speed／Energy／Handling／Acceleration／Toughness／Altitude／Training Limit | 速度／能量／操控／動量／韌性／高度／訓練上限 | 能力名照語料；Limit＝上限 |
| Powerup（坐騎） | （暫留原文） | 語料無 |
| Black Road Runaround | Black Road 兜圈跑 | 暫譯；runaround 也有「推託」之意 |
| Fast-Paced Pilgrimage | 快步朝聖 | 暫譯 |
| Rush Around the Ruins | 廢墟疾走 | 暫譯 |
| Dash Across the Dunes | 沙丘衝刺 | 暫譯 |
| Llevigar Leg Day | Llevigar 練腿日 | 暫譯，保留健身梗 |
| Hazardous Hike | 險途健行 | 暫譯 |
| South Canyon Competition | 南峽谷競速 | 暫譯 |
| Bremmyn Brustle | （暫留原文） | Brustle 是造字 |
| Sky-High Soar | 衝天翱翔 | 暫譯 |
| Abyssal Aerobatics | 深淵特技飛行 | 暫譯 |
| Ultimate Discovery | 終極探索點 | 與第一章一致 |
| Phantom Soldier | 幽靈士兵 | |
| Secret Library | 祕密圖書館 | 語料探索點名已用 |
| The Noble House of Twain | 《高貴的 Twain 家》 | 與第一章一致 |
| The Isle of Industry | 《工業之島》 | Relos 的套書 |
| Records of the Gerten War | 《Gerten 戰爭紀錄》 | Gylia Watch 圖書館 |
| Sealed Letters | 封緘信 | 照探索點名 |
| Llevigar Archivist | Llevigar 檔案管理員 | |
| Wars of the Cousins | 表親之戰 | |
| Fort Frog | 青蛙堡 | Croaquin 的住處 |
| Bog Frog | 沼澤巨蛙 | Fia 口中的傳說 |
| tar pits | 焦油坑 | 換形者誕生之處 |
| Prized Bottle Cap | 珍貴的瓶蓋 | Fia 的家傳寶 |
| Icsul Family Brewery | Icsul 家族釀酒廠 | |
| Corrupted Remains | （留原文） | 表格中的需求道具 |
| Purificatus non consumptus | 淨化而非吞噬 | Zhiraok 日記的拉丁文 |
| Solemn Oath | （留原文） | 曲名 |
| Craftmas Quest／Craftmas Chaos | （留原文） | 已移除的任務，語料無譯名；Craftmas 照語料留原文 |
| The Headless Hunt | （留原文） | 已移除的 1.19 任務 |
| The Fortuneteller | 《算命師》 | 已移除的任務 |

**語料內值得一提的既有譯名**：Secret Discovery＝隱藏探索點、Mini-Quest＝迷你任務、Slaying Post＝討伐告示、Gathering Post＝採集告示、Tower of Ascension＝登天之塔（`discovery.json` 裡則留原文）、changeling＝換形者（`discovery.json` 有兩條寫成「變形者」，第六章已提出）、Sovereign X＝X 統領。

**待使用者決定**：

1. 〈天空的統治者〉的檔名與 wiki 索引不同（Ruler／Rulers），語料兩種都對到同一個譯名，不影響顯示；只是記錄。
2. 〈痛中之慰〉#013「Who the helix」現譯「到底是哪個螺旋的傢伙」，諧音的咒罵感弱了一些，可考慮「到底是哪個螺旋的鬼」之類（建議，不擅改）。
3. 計時賽十個賽道名語料沒有，本章是暫譯；正式收進語料時可以再定。

---

## 八、出處清單

**語料**（`src/main/resources/assets/wynnchayuan/translations/zh_tw/`）
- `secret/the-legend-of-bob.json`、`secret/maex-black-market.json`、`secret/ruler-of-the-skies.json`、`secret/solace-in-pain.json`、`secret/a-croaking-cryptid.json`
- `discovery-name.json`、`discovery.json`、`misc.json`、`npc.json`、`mini-quest.json`、`quest-name.json`
- 專案文件：`GLOSSARY.md`、`docs/world-and-story.md`、本系列第 01–06 章

**wiki**（wynncraft.wiki.gg）
- 計時賽：[Time Trials](https://wynncraft.wiki.gg/wiki/Time_Trials)、[Mounts](https://wynncraft.wiki.gg/wiki/Mounts)
- 探索點總表：[Secret Discoveries](https://wynncraft.wiki.gg/wiki/Secret_Discoveries)，以及 121 個探索點各自的條目頁（infobox 的 lore 欄）
- 祕密故事：[Ragni's Secret Library](https://wynncraft.wiki.gg/wiki/Ragni%27s_Secret_Library)、[Maex Black Market](https://wynncraft.wiki.gg/wiki/Maex_Black_Market)、[Ruler of the Skies](https://wynncraft.wiki.gg/wiki/Ruler_of_the_Skies)、[Solace in Pain](https://wynncraft.wiki.gg/wiki/Solace_in_Pain)、[A Croaking Cryptid](https://wynncraft.wiki.gg/wiki/A_Croaking_Cryptid)
- 跨區暗線：[Bak'al's Destruction 1](https://wynncraft.wiki.gg/wiki/Bak%27al%27s_Destruction_1)、[2](https://wynncraft.wiki.gg/wiki/Bak%27al%27s_Destruction_2)、[3](https://wynncraft.wiki.gg/wiki/Bak%27al%27s_Destruction_3)、[Wynn Plains Monument](https://wynncraft.wiki.gg/wiki/Wynn_Plains_Monument)、[Watchmen](https://wynncraft.wiki.gg/wiki/Watchmen)、[Torn in Twain](https://wynncraft.wiki.gg/wiki/Torn_in_Twain)、[The Twains' Downfall](https://wynncraft.wiki.gg/wiki/The_Twains%27_Downfall)、[Lusuco's Secret Library](https://wynncraft.wiki.gg/wiki/Lusuco%27s_Secret_Library)、[Llevigar's Secret Library](https://wynncraft.wiki.gg/wiki/Llevigar%27s_Secret_Library)、[Relos' Secret Library](https://wynncraft.wiki.gg/wiki/Relos%27_Secret_Library)、[Gylia Watch Library](https://wynncraft.wiki.gg/wiki/Gylia_Watch_Library)、[Nilrem's Sealed Letter](https://wynncraft.wiki.gg/wiki/Nilrem%27s_Sealed_Letter)、[Sealed Letters](https://wynncraft.wiki.gg/wiki/Sealed_Letters)、[A Deathly Warning](https://wynncraft.wiki.gg/wiki/A_Deathly_Warning)、[Ne du Valeos du Ellach](https://wynncraft.wiki.gg/wiki/Ne_du_Valeos_du_Ellach)、[Bob](https://wynncraft.wiki.gg/wiki/Bob)、[Bob's Tear](https://wynncraft.wiki.gg/wiki/Bob%27s_Tear)、[Bob's Tomb](https://wynncraft.wiki.gg/wiki/Bob%27s_Tomb)
- 任務：[Quests](https://wynncraft.wiki.gg/wiki/Quests)、[???](https://wynncraft.wiki.gg/wiki/%3F%3F%3F)、[??? (1.14)](https://wynncraft.wiki.gg/wiki/%3F%3F%3F_(1.14))、[Craftmas Quest](https://wynncraft.wiki.gg/wiki/Craftmas_Quest)、[Craftmas Chaos](https://wynncraft.wiki.gg/wiki/Craftmas_Chaos)、[The Worm Holes](https://wynncraft.wiki.gg/wiki/The_Worm_Holes)、[Taproot](https://wynncraft.wiki.gg/wiki/Taproot)、[A Headless History](https://wynncraft.wiki.gg/wiki/A_Headless_History)、[Finding the Light](https://wynncraft.wiki.gg/wiki/Finding_the_Light)、[The Realm of Light (Location)](https://wynncraft.wiki.gg/wiki/The_Realm_of_Light_(Location))
- 彩蛋：[Easter Eggs](https://wynncraft.wiki.gg/wiki/Easter_Eggs)
