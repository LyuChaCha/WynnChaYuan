# 第十章　Lootrun、世界事件與小型故事

> **劇透提醒**：本章劇透 Lootrun 七個營地的挑戰內容、全部世界事件（含毀滅前奏與湮滅）、角色任務 Afelis 與 Angie 的結局、四個節慶的劇情（含英雄節祕密房間裡四兄弟的真相），以及《不得安息》《國王的新兵》《蛛群登巔》《傭兵》《木乃伊之怒》《峽谷兀鷲》《冰封諸國》《Maltic 的水井》等舊任務首領的身分。

---

## 一、概述

前九章談的是「有任務名的故事」。這一章收的是另外三類**沒有任務日誌、卻帶著劇情**的內容：

1. **Lootrun**（2.0.3 加入）：Silverbull 貿易公司的 Lootrun 部門在七個高等地區設營地，玩家在計時內連續挑戰洞窟與戰鬥點。玩法是刷寶，但每個挑戰點都蓋在一段既有的故事上——Olm 的墓場、Avos 的亡魂、Ahms 碎裂後的虛空、Fruma 的宣傳海報。
2. **世界事件**（2.1 加入）：地圖上隨機開始的波次戰。一半是舊任務首領的「重戰」，另一半用一行說明文字（語料已收）替一個地區補上設定；其中**毀滅前奏**是全伺服器的大型事件，首領湮滅（Annihilation）有完整的背景。
3. **小型故事**：沒有任務名的**角色任務**（Afelis、Angie）、迷你任務告示裡的地方小事，以及**四個節慶**——節慶表面上是限時活動，底下卻藏著一條跨年度的主線：四位主持人其實是一家四兄弟。

這些內容大多**語料沒收台詞**（只收了名稱與說明文字），下文標「本章轉述」「本章自譯」的部分請在正式翻譯前以 capture 核對。

出處：wiki〈[Lootrunning](https://wynncraft.wiki.gg/wiki/Lootrunning)〉〈[World Events](https://wynncraft.wiki.gg/wiki/World_Events)〉〈[Festival of the Heroes](https://wynncraft.wiki.gg/wiki/Festival_of_the_Heroes)〉；語料 `lootrun.json`、`misc.json`、`quest.json`

---

## 二、Lootrun

### 1. Silverbull 的 Lootrun 部門

Lootrun 的入口在 Cinfras 東邊的 **Silverbull Company Headquarters**——前身是 Gavel 王室的避暑行宮（第八章 §一）。北翼的 **Lootrun 主管**（Lootrun Chief）替七十五級以上的冒險者免費入會；入會後，各營地的 **Lootrun 大師**（Lootrun Master）負責驗裝備、開跑。Lootrun 部門是 Silverbull 的第一個「集團」（Syndicate），第二個是討伐戰部門（2.1）。

總部大廳的職員會招呼你：「你可以報名加入各個部門，享有公司替你談來的各種優惠。」「直接走進我身後左邊的房間，找我們的 Lootrun 主管談談。他很快就會幫你安排好！」（語料，但歸錯檔，見文末語料問題 1）。主管本人入會後只說一句「看來你已經是我部門的人了！**目前**我沒什麼能為你做的了」（本章轉述）——「目前」在 wiki 抄錄裡是斜體，像在暗示部門以後還有別的事（推測；總部頂樓有一間進不去的祕密辦公室，wiki 附有截圖）。

部門的十六個階級從 Rookie 一路到 Grandmaster（語料只收了最高階「Lootrun 宗師」），每升一級解鎖一種信標或一項特權。

出處：wiki〈[Lootrun Chief](https://wynncraft.wiki.gg/wiki/Lootrun_Chief)〉〈[Silverbull Company Headquarters](https://wynncraft.wiki.gg/wiki/Silverbull_Company_Headquarters)〉〈[Silverbull Syndicates](https://wynncraft.wiki.gg/wiki/Silverbull_Syndicates)〉；語料 `npc.json`、`gui.json`、`quest/aldorei-s-secret-part-i.json` #000–#008

### 2. 信標、加持、怪物強化：機制背後的命名

術語照 2026-10-14／15 的定案：Beacon＝信標、Boon＝加持、Curse＝怪物強化、Radiant＝精英、Mission＝使命、Trial＝試煉、End Reward Pulls＝結算抽數、Reroll＝結算重抽數、Sacrifice＝結算獻祭數（第七章、`lootrun.json`）。

**信標的顏色是一套語言**：藍＝加持、紫＝怪物強化換抽數、黃＝飛天寶箱、綠＝加時、灰＝使命、緋紅（Crimson）＝試煉、彩虹＝之後都是璀璨信標。這套顏色在故事上沒有明說，但幾個名字露了口風：

- **使命「Orphion 的恩典」**（Orphion's Grace，語料）：加持效果提高五成。整套系統裡只有這一項用神祇命名，而它強化的正是藍色信標帶來的加持——設計上像是把加持當成光的恩賜、把紫色的怪物強化當成它的反面（推測）。
- **使命「國王的宮廷」**（King's Court，語料）：往信標池多加一個**緋紅**信標。King's Court＋Crimson，幾乎可以確定是在玩英國樂團 King Crimson 的專輯《In the Court of the Crimson King》（推測，wiki 未說明）。中文「國王的宮廷」保住了字面，雙關無法保留，可以接受。
- **使命「紫色恐懼症」**（Porphyrophobia）：希臘文 porphyra（紫）＋恐懼症，效果正是「看到紫色信標就多一層怪物強化」。語料直譯成「紫色恐懼症」，意思到了。
- **使命「靛藍路線」**（Route Indigo）：藍與紫信標被遮住、但一定是大幅強化——靛藍正是藍與紫之間的顏色。
- **使命「頑固音型」**（Ostinato）：音樂術語「不斷重複的音型」，效果是**重複**拿同一種加持就給抽數。語料用了正式的樂理譯名，雙關在。同一批（2.2.1）加入的「一切美好」（Beleza Pura，葡萄牙文「純粹的美」，也是巴西歌曲名）、「備用節拍」（Backup Beat）、「安魂曲」（Requiem，效果是讓怪物強化暫時失效＝讓死者安息）也都帶著音樂味（推測是同一位設計者的命名習慣）。
- **試煉 Imperitia**：拉丁文「笨拙、不熟練」，效果是每完成一場挑戰法術就多耗五點魔力。語料留原文，建議譯「生疏」（本章建議）。

試煉的名字則像一份「賭徒與英雄的罪」清單：傲慢（Hubris，十場內死亡就結束）、孤注一擲、嗜賭之獸、終極獻祭、刺激成癮（語料）。

出處：wiki〈[Lootrunning](https://wynncraft.wiki.gg/wiki/Lootrunning)〉；語料 `lootrun.json`、`misc.json`

### 3. 七個營地

每個營地要先完成該區的任務與大部分洞窟才能開；洞窟本身就是「探洞」（Spelunk）挑戰，其餘是殲滅、獵殺目標、摧毀、防守四類。下面只列**帶劇情的**挑戰點（挑戰名語料都沒收，括號內是本章暫譯）。

#### Canyon of the Lost 遠足（南）　Lv. 85

營地在 Thesead 外，前置任務之一是《不得安息》（第三章）。

- **The Armory（軍械庫）**：峽谷底下一座「早已消失的民族」留下、無人看管的古老鍛造場（wiki），守衛是 Leytech 石像鬼與 Gigas 魔像。是誰的鍛造場，遊戲沒說；跟巨像與「遠古者」（Ancient Ones）的遺跡同屬一層（推測）。
- **Colossal Synapse（巨像突觸）**：在峽谷巨像西邊的拱門下，目標是「巨像突觸」（語料名牌）。整個峽谷的巨像碎片都用**神經解剖學**命名——軸突（Axon）、突觸、髓鞘（Myelin Sheath）、Monumyelin、Thalamold（視丘）——好像巨像是一顆散落在地底的大腦。翻譯時這組名字要成套（語料已有「瑪瑙軸突」「巨像突觸」）。
- **Sorcerer's Rave（術士狂歡）**：在 Dramele 的塔底下，對手是「三位一體狂徒」（語料）。
- **Farmer's Harvest（農夫的收成）**：地點是「Krolton 被殺的地方」附近——也就是《不得安息》玩家打塌洞穴、壓死 Krolton 換回自己火花的那一帶（第三章）。
- **Elemental Spring（元素之泉）**：防守 Bantisu 氣之神殿的圖騰。
- **Absorbing Slime（吞噬史萊姆）**：一名村民被困在巨大的史萊姆裡。

#### 橫越 Corkus　Lv. 90

前置是《使者 第一部／第二部》（第四章）。Corkus 的挑戰點最有故事性：

- **Shackles of the Past（過往的枷鎖）**：摧毀一座巨大的圖騰——它把**死去的 Avos 與 Corkus 人的靈魂一起錨住**，讓他們無法往生。打掉後偶爾會出現一群 Avos 的亡魂，點其中一個，它只說一句「謝謝你」就消失（本章自譯）。人類與 Avos 打了一輩子仗，死後卻被同一根柱子鎖在一起。
- **Armistice Sapling（停戰樹苗）**：防守一棵「停戰」之名的樹苗五十秒，燃燒人（Burning Men，語料「燃燒人」）會專挑樹苗攻擊。Corkus 與 Avos 的和解是《使者》的主題，樹苗象徵什麼不言可喻（推測）。
- **Communication Monopole（通訊單極塔）**：地點跟世界事件「除役的戰爭機械」相同（見 §三-5）。
- 其他：Fort Corkia 的首領、海盜收費站的「商務機器人」（Commodroid，語料）、Builder-Bot 中樞的建築師機器人。

#### Molten Heights 健行　Lv. 95

前置是《矮人與 Doguns 第四部》。

- **Draconic Graveyard（龍之墓場）**：在 Freezing Heights 的冰亞龍骨骸旁，敵人全是死去的龍族亡魂。
- **Rare Dragon Bone（稀有龍骨）**：一群貪心的矮人想把龍骨砸碎拿走，玩家要守住它——矮人的貪婪是第四章 Rodoroc 篇的老主題。
- **Supply Wagon（補給馬車）**：Azer 想搶 Rodoroc 附近的一輛補給馬車。
- **Shadow Altar（暗影祭壇）**：殺 Byrnic 小鬼祭司取得儀式石，才見得到「暗影矮人」（語料）。

#### Sky Islands 探索　Lv. 96

部分挑戰在雲上、太空或虛空裡。

- **Astraulus' Comet（Astraulus 的彗星）**：走近 Astraulus 塔的岩石時，塔主會罵你：「嘿！又是你！滾出去，這些月鹽是我的！」「回來幹嘛！？滾，害蟲！」或「你又來了？好啊！那就回星星上去吧！」（本章自譯）然後把你射上太空，對手是「星隙之暗」（Darkness Between The Stars，語料）。
- **Wybel Satellite（Wybel 衛星）**：玩家掉到下層的 Wybel 島，一架 **UFW**（不明飛行 Wybel）趕來保護它的衛星，事後放下彈跳床送你上去。wiki 指出這架 UFW 其實就是**橘色 Wybel**——第四章那位陰謀論老師堅稱「外面還有第十個 Wybel」，答案在這裡。
- **Kandon-Beda Beanstalk（Kandon-Beda 豆莖）**：沿著〈雲端之上〉那根豆莖爬上天，背景裡可以看見古龍 **Farcor** 在飛（第四章）。
- **Debris of Furcation（分岔殘骸）**：從瀑布掉進虛空島。Furcation（分叉）與 Fracturing（碎裂）同源，指 Ahms 碎裂後掉進虛空的殘片（推測）。
- 其他：Colossus Islet 的「碎裂哨兵」、Raiders' Plaza 的天空劫掠者、Phantasmic Field 的悲傷（Sorrow）、懺悔與悔恨之魂（Eidolon of Contrition／Regret）。

#### Silent Expanse 遠征　Lv. 101

前置是《逐深之旅》（第五章）。四個區塊——Olm 古城、眼球森林、毒廢之地、虛空谷——各有自己的設定，wiki 的冷知識欄也特別多：

- **Graveyard of the Thousand（千人墓場）**：在虛空洞裡，其實是一座 **Olm 的墓場**——除了首領「骸骨怪物」，其餘亡者生前都是 Olm，牆上還嵌著一具巨大的 Olm 骨骸。不在 Lootrun 時想進去，會被告知「一股看不見的力量阻止你進入這個虛空洞，你該去找別的虛空洞」（本章自譯）。
- **False Respite（虛假的安歇）**：目標是**典範者**（Exemplar，語料）。設計者 Deusphage 說它是「冒牌的神」：原本是從無名異常體那片群島漂出來的畸形怪物，靠信徒（迷途的眼族）的供奉長大，信徒把自己的身體獻上、成了它的一部分，「久而久之它長得越來越像它的信徒」。名字取自《Darkest Dungeon II》。
- **Forlorn Library（被遺棄的圖書館）**：防守一堆古書。獻火者（Immolatist）專燒書，毀書者（Biblioclast）與執意無知者攻擊玩家（語料名牌）；wiki 說毀書者的祭司面具底下是被黑暗汙染的 Olm。
- **毒廢之地的雙關**：Lesionary＝病灶（lesion）＋軍團兵（legionary）；Terra-Toma＝大地（terra）＋畸胎瘤（teratoma），呼應「整片毒廢之地是一頭融進大地的活物」；喘息者（Respirator，語料）是被改造來替毒廢之地「呼吸」的礦工屍體，肺脹到撐破肋骨。
- **Melting Architecture（融化的建築）**：防守「方尖碑的殘跡」（Vestige of the Obelisk）——Lutho 那座方尖碑的碎片（第五章 §四，推測）。
- **The Feedhole（餵食孔）**：極罕見的隨機事件，地面裂開一張嘴把玩家吞進肉壁洞穴，要打斷四條「磨損的神經」才逃得出來。
- 其他：Olm 古城的人偶戲（Puppetry Performance，目標是幕後的操偶師）、眼之尖塔、Exiled Garden 上空盤旋的「培育者」（The Cultivator）、掌燈者、凝結／凝聚／凝集之物（語料）。

#### Fruma 突襲（西）Lv. 110／（東）Lv. 115

前置分別是《一葉知秋》與《真面目》（第六章）。Fruma 的挑戰點幾乎都在講**政權與人民**：

- **Propaganda Vandal（塗鴉宣傳的人）**（西）：防守一個正在塗改大型宣傳海報的居民，攻擊他的是 Industrial District 的衛兵。玩家在這一關是替「破壞宣傳品的人」擋子彈。
- **Thug Ambush、Pesticide Ventilation、Railway Furnace**（西）：Roughworks 的暴徒、噴毒氣的通風口、有警報與砲台的鐵路熔爐（wiki：致敬《ULTRAKILL》）。
- **Autumnal Ruins（秋之遺跡）**（西）：穿過 Auburn Forest 東側的天然石拱，進入被遺忘的神殿，目標是一隻**名字用古 Fruma 文寫成**的靈狐。
- **Canopy Ambush（樹冠伏擊）**（東）：Mistwoods 的樹冠族把玩家**誤認成換形者**而發動攻擊——第六章「猜疑會吃掉一個社群」的又一個註腳。
- **Queen's Statue、Aelumia Gatekeepers**（東）：摧毀女王雕像、擊退守城的羽翼部隊。
- **Aldwell Invasion（Aldwell 入侵）**（東）：防守 Aldwell 的橋，攻擊方是 Mistwoods 衛隊。
- **Sleeping Fox（沉睡的狐狸）**（東）：保護一隻睡著的狐狸。

Fruma 的兩個營地是唯一會開出 **Ek 符文**的地方；Ek 是第八章 The Forgery 熔合實驗裡的三個「符文之詞」之一。

出處：wiki〈[Canyon of the Lost Excursion (South)](https://wynncraft.wiki.gg/wiki/Canyon_of_the_Lost_Excursion_(South))〉〈[The Corkus Traversal](https://wynncraft.wiki.gg/wiki/The_Corkus_Traversal)〉〈[Molten Heights Hike](https://wynncraft.wiki.gg/wiki/Molten_Heights_Hike)〉〈[Sky Islands Exploration](https://wynncraft.wiki.gg/wiki/Sky_Islands_Exploration)〉〈[Silent Expanse Expedition](https://wynncraft.wiki.gg/wiki/Silent_Expanse_Expedition)〉〈[The Fruma Foray (West)](https://wynncraft.wiki.gg/wiki/The_Fruma_Foray_(West))〉〈[The Fruma Foray (East)](https://wynncraft.wiki.gg/wiki/The_Fruma_Foray_(East))〉〈[Exemplar](https://wynncraft.wiki.gg/wiki/Exemplar)〉；語料 `discovery-name.json`（營地名）、`npc.json`

---

## 三、世界事件

### 1. 制度

2.1「重燃的世界」加入。事件在固定地點倒數開始，參加的隊伍有自己的副本，怪物等級跟著隊上最高等的人走。每個地區同時只會有一個事件。內容書裡每個事件都有一行說明文字，語料大多已收（`quest.json`、`misc.json`），是本節最主要的引文來源。

### 2. 湮滅與毀滅前奏

**毀滅前奏**（Prelude to Annihilation）是唯一的「大型世界事件」，約每三天一次，地點在 Roots of Corruption 的腐敗傳送門前。開始前全伺服器會看到：

> 傳送門湧出充滿憎恨的回音。Wynn 正面臨湮滅。（語料）

原文 Wynn faces Annihilation 一語雙關：既是「面臨毀滅」，也是「面對湮滅這頭魔物」。中文「湮滅」同時是首領名與動詞，雙關恰好保住了。事件說明則是：「某種來自彼端的東西帶著對一切生命的憎恨，穿過傳送門咆哮而來。湮滅正降臨物質界。」（語料）

**湮滅是誰。** 戰爭領域（Realm of War）的惡魔，等級 250。wiki 整理的年表：

| 年代 | 事件 |
|---|---|
| 約 900 AP | 第一次有「那種巨獸」試圖穿過傳送門，據說 **Bob** 獨力把它逼回去 |
| 985 AP 冬 | Bob 死後第二年，Roots 一帶地震，Elkurn 最先察覺；湮滅破門而出，被 Detlas 的 **Aegis 上將**、Nemract 的 **Takan 指揮官**、Troms 的 **Eveni 將軍**率各城聯軍擊退，死傷慘重 |
| 1000 AP | 湮滅開始反覆嘗試出門，也就是現在的毀滅前奏 |

烽火王宮討伐戰（第八章）裡，戰爭領域的背景中可以看到**好幾頭**湮滅的剪影——所以 Bob、Aegis 與玩家打的是不是同一頭，wiki 也說不準。

**Aegis 的回憶**（本章轉述）。在 Detlas 兵營問起這場仗，Aegis 先抱怨「Ragon 又在對新兵講那個故事了？」，然後說：985 年冬天，英雄剛走一年，腐敗卻異常安靜，直到 Roots 開始地震。「這一次沒有他來救我們，所以我們得自己救自己。」戰場上士兵與小惡魔肉搏，砲兵轟擊巨獸，弓兵射出不穩定石英削成的箭。他說起大家掛在嘴上的那句「勇敢的 Aegis 絕不屈服！」，接著承認「那幾乎是真的。那一仗，我差一點就……」——他看見一團**尖叫的烈焰越長越大、比太陽還亮**，「那麼多人……燒成灰」，「我能把 Takan 一起拖到掩體後面，簡直是奇蹟」。最後砲兵把那團火打了下來。「我們把它推回傳送門……但沒有殺死它。我不確定我們**能不能**殺死它。」「我只希望它別在我有生之年回來。我已經辦過太多場追悼會了。」

那團「尖叫的烈焰」就是湮滅的終極技能**新日**（New Sun）：一顆越長越大的光球，長滿之前沒打掉，場上所有人立刻死亡。玩家在毀滅前奏裡親身重演了 Aegis 的惡夢。打倒湮滅時，它會**伸手掙扎著被拖回傳送門**——不是死，是被推回去，跟 Aegis 說的一樣。

戰利品「腐敗寶匣」的說明把話說滿了：「毀滅暫時被擋下了，但事情不會就此結束。來自戰爭荒原深處的攻勢，永無止盡。」（語料；原文 Annihilation 大寫，語料這裡譯成「毀滅」，見語料問題 7）

死亡訊息也自成一套（wiki 收八條）：「{u} 成了戰爭的又一名犧牲者」（本章自譯）「業火焚燒著 {u} 的屍體」（語料）「{u} 難逃滅絕的命運」（語料）「{u} 的靈魂熄滅了」（語料）「{u} 被湮滅了」——最後一句原文 was annihilated 也是雙關，譯「殲滅」會斷掉與首領名的連結，宜用「湮滅」。

出處：wiki〈[Annihilation](https://wynncraft.wiki.gg/wiki/Annihilation)〉〈[Prelude to Annihilation](https://wynncraft.wiki.gg/wiki/Prelude_to_Annihilation)〉〈[Admiral Aegis](https://wynncraft.wiki.gg/wiki/Admiral_Aegis)〉〈[Corrupted Cache](https://wynncraft.wiki.gg/wiki/Corrupted_Cache)〉；語料 `misc.json`、`npc.json`

### 3. 腐敗軍的七個軍階：「逼近的」系列

七個「逼近的 X」（Encroaching）事件是一組，首領是腐敗大軍的**軍官**，軍階由低到高沿著玩家的冒險路線排開。說明文字都用同一個句型「腐敗大軍集結，要……」（語料）：

| 事件（語料） | 地點 | 首領（語料） | 說明（語料） |
|---|---|---|---|
| 逼近的烈焰 | Nivla Woods | 地獄副官 | 要把森林化為灰燼 |
| 逼近的毀滅 | Detlas 郊區 | 地獄中尉 | 要把戰場據為己有 |
| 逼近的悲苦 | 古 Nemract | 地獄上尉 | 要為自己的征服狂歡 |
| 逼近的復生 | Almuj 沙漠 | 地獄少校 | 要把死者從灼熱的沙裡拖出來 |
| 逼近的燎原 | Rymek Mesa | 地獄上校 | 要用戰爭在大地上劃下傷疤 |
| 逼近的消融 | Nesaak 凍原 | 地獄將軍 | 要解開 Theorick 的法術，再往深處推進 |
| 逼近的絕滅 | Dernel 叢林 | 地獄上將 | 要終結一切生命 |

兩點值得注意：「消融」那條點名 **Theorick 的法術**——Nesaak 的冰封是 Theorick Twain 的魔法（第一章 §4），腐敗想把它融掉；前兩個事件的首領同時是《元素演練》裡腐敗召喚師的重戰。這張表也說明腐敗不是一群無腦的怪物，而是**有編制的軍隊**——跟 Aegis 說的「無腦的腐敗」形成對照（推測是刻意的）。

出處：wiki〈[Encroaching Blaze](https://wynncraft.wiki.gg/wiki/Encroaching_Blaze)〉〈[Encroaching Ablation](https://wynncraft.wiki.gg/wiki/Encroaching_Ablation)〉〈[Encroaching Extinction](https://wynncraft.wiki.gg/wiki/Encroaching_Extinction)〉等七頁；語料 `misc.json`、`quest.json`、`npc.json`

### 4. 舊任務首領的歸來

一大半世界事件是 wiki 所謂的「重戰」：把任務首領換個名字再打一次。說明文字常是這些首領的後日談：

| 事件（語料） | 重戰的首領 | 原任務 | 說明文字（語料）與註記 |
|---|---|---|---|
| 襲擊將至 | 殭屍蠻兵（對應該任務的腐敗巨人） | 《國王的新兵》 | 「對 Wynn 的士兵來說，不死大軍的圍攻算不上罕見。」 |
| 亡靈法陣 | 飄移死神 | 《Ragni 的下水道》的骷髏死神 | 「腐敗在 Wynn 的出現，把曾經莊嚴的墓園變成了腐爛的大軍。」（本章自譯，語料未收） |
| 亡者歸來 | Sayleros 那位兄弟的歸來 | 《Grave Digger》（2.2.2 移除，改版為《歸來的兄長》） | 「在 Wynn，真正的死亡很罕見——腐敗不允許任何人真的了結。」第九章已指出「Sayleros 兄弟」的歧義，名牌改作「Sayleros 那位兄弟」 |
| 血痂石墓／失禮的葬儀 | 血痂木乃伊／受詛木乃伊 | 《木乃伊之怒》 | 「沒能讓死者安息，往往換來他們陰森的歸來。」 |
| 失敗的狩獵 | 暴怒的雞蛇獸 | 《峽谷兀鷲》 | 「財富與虛榮的誘惑，引來了莽撞之徒……」 |
| 海盜混戰 | 腐爛船長 | 《冰封諸國》 | 「這些海盜的全盛時期早已過去，但他們蹣跚的殘骸仍聽命於那位腐爛船長的吼叫。」 |
| 困獸埋伏 | 荒蕪女巫 | 《Maltic 的水井》的井中女巫 | 「沼澤裡有人目擊到一名女巫，正玩弄著遠超出她掌控的力量。」 |
| 燃燒的記憶 | ???，大橋剋星 | 首次過 Great Bridge | 「腐敗把最強大的資產牢牢攥在手裡。」大橋剋星生前是 Twain 家的火之子 **Rickeo**（第一章 §4），死後被腐敗一次次喚回 |
| 蛛群竄動 | 育巢母蛛 | 《蘑菇人》（事件常駐） | 「一大群蜘蛛在 Nivla Woods 深處的蘑菇叢間築起了巢。」 |

「腐敗不允許任何人真的了結」這句，跟《不得安息》死神說的「那股禍患只在火花熄滅之後才對肉體下手」是同一件事的兩面（第三章）：靈魂走了，身體被腐敗拿去用。大橋剋星就是最極端的例子——連身為大法師的力量都一併被留用。

出處：wiki 各事件頁、〈[Bane of the Great Bridge](https://wynncraft.wiki.gg/wiki/Bane_of_the_Great_Bridge)〉〈[Sayleros' Brother](https://wynncraft.wiki.gg/wiki/Sayleros%27_Brother)〉；語料 `quest.json`、`misc.json`、`npc.json`

### 5. 用一行字讀世界：各地區的事件

其餘事件的說明文字，常常補上任務沒講的設定。依地區摘要（事件名照語料，說明引語料譯文）：

**Wynn 與 Gavel**

- **犬群埋伏**（Nesaak）：「有些傳說把 Bob 的死歸咎於 Nesaak 凍原上的野狼。許多人質疑這則故事的真假……」——對照第一章〈狼之神殿〉與 985 AP 出現在雪地的 Bob 之墓。官方用「傳說」二字，沒有證實。
- **汙濁的海岸**（Pirate Bay）：「腐敗不能渡過大片水域」是迷思，但真相是它**很難**越過海洋（wiki 原文，語料未收）——這解釋了海島與 Corkus 為什麼相對安全。
- **遭棄的哨衛**（Cinfras 東南的廢墟平原）：「Gavel 那些古老的守護者如今已分不清敵我。有人說它們是最早那批人的士兵，在創造者死去很久之後，仍在打同一場仗。」事件就在一顆遠古巨人頭像旁。
- **永劫之源**（Time Valley）：「失落的古代之物與不可能存在的未來匯聚於同一點……」（第一章 §9）。
- **光之使者／界域抗原**（Light Forest／光之領域）：「隨著黯蝕逼近，光的守衛者自行擔下了責任，要把一切外來之物從他們的土地上清除。」「這個領域的免疫反應，把一切不熟悉的東西都視為該清除的病。」——光把玩家當病原體，正是第二、五章「光不等於善」的主題。
- **脫韁幼龍**（Olux）：「一隻 Grootslang 的幼獸受黯蝕折磨，脫離了族群。」對照第八章 Grootslang 之巢（幼龍發狂是因為體內的 Dern 蟎）。
- **彼方來訪**（Kander Forest 的 House of Talor）：「黑暗的使徒試圖囚禁受詛者，把他們的靈魂縛在這個世界上。」首領「Kandrekk 織魂者」。Kandrekk 是一整個術士派系（名牌另有「Kandrekk 大幽魂」「Kandrekk 縛靈者」，洞窟說明稱「Kandrekk 術士……想看世界燃燒」），第九章魂墨胸甲的「大幽魂」就是其中之一。

**峽谷、海洋與天空**

- **巨像扎根**：「巨像的碎片扎根於大地，在它所俯瞰的崩解土地上四處肆虐。」
- **犬吠鬧場**（Ruff & Tumble）：峽谷不斷移動造成落石，挖出「沉睡在土裡的古老石頭生物」。設計者說 Tuffer 是當地的狼，被埋就冬眠數百上千年；首領凶暴 Tuffer 可能睡了數十萬年。事件名「犬吠」抓到了 Ruff（狗叫聲）的一半雙關，rough-and-tumble（混戰）的另一半則落在「鬧場」。
- **孤島**（Lonely Islet）：「浪濤平息之前，這片海洋多年無人踏足。」首領叫「風暴的殘餘」——海是後來才平靜的（第四章海洋篇的背景）。
- **Ahms 紀念碑／破碎的巢穴**：古老雕像「熬過了時間與碎裂」；大地被剖開、留下通往虛空的傷口後，Ahms 原生的陸地物種大多滅絕，如今是鳥類的天下（說明語料未收，本章轉述）。
- **除役的戰爭機械**（Corkus）：「那些為戰爭而造的舊機器，被 Antikythera 挪作他用，投入它那場號稱『和平』的行動。」引號是原文就有的——淪陷工廠的 Antikythera 自認是在維和（第八章）。

**Silent Expanse**

- **難解的指引之星**：「放棄一切理智，臣服於那些正刮擦著這個世界的不可名狀之物。」首領名是 Zalgo 亂碼的「某個東西」（Something）。
- **失落紀念碑**（虛空谷的黑色方尖碑旁）：「在山谷裡坐得夠久，你就會看著現實被一點一點啃食殆盡……直到什麼都不剩。」
- **異界展演**：藏在虛空谷一扇門後，**必須先打過詭異瞭望台**才進得去。「世界上那些被無饜的飢餓撕開的傷口，成了看不見的通道……最瘋狂的人，卻在這種難以言喻的存在交融中看見了美。」「無饜的飢餓」指的是撕開虛空洞的那股力量（第五章 §十三，推測）；八個首領裡包括「衰弱的至大者」——無名異常體那位至大者的殘影。

**Fruma**（全部要先完成《一葉知秋》或《希望的合奏》）

- **過勞工會**（Karoshi＝日文「過勞死」）：「在永不停轉的生產齒輪下，許多工人漸漸看不見自己……眼裡只剩下手上那份工作。」首領 Padrone（義大利文「工頭、老闆」）。
- **生化綻放**：「Roughworks 的污染讓水道裡的藻類、黏液與腐敗殘渣聚成了會移動的群落。清理它既噁心又非做不可……」
- **皇家鍊金師**（Aelumia 城塞）：「Fruman 政權急於向城塞的民眾展示自己的技術成就，好讓人民不會被魔法所誘惑。」——Fruma 禁魔的統治術，一句話講完（第六章）。
- **巡邏士兵**：巡邏隊「既保護民眾，也看管他們」（語料把 keep the population safe and in check 譯得很好）。
- **宮廷衛隊**：「在那種場合裡當個叛亂者，必死無疑。」**堡壘營房**：「最危險的敵人，就是願意為信念赴死的人。」
- **沼澤爭端**（Frog Bog）：首領就叫「青蛙」，搭配三名受詛的棄民——第七章〈呱鳴怪談〉的那片沼澤。

出處：wiki〈[World Events](https://wynncraft.wiki.gg/wiki/World_Events)〉及各事件頁（〈[Ruff & Tumble](https://wynncraft.wiki.gg/wiki/Ruff_%26_Tumble)〉〈[Tainted Shoreline](https://wynncraft.wiki.gg/wiki/Tainted_Shoreline)〉〈[Otherworldly Exhibition](https://wynncraft.wiki.gg/wiki/Otherworldly_Exhibition)〉等）；語料 `quest.json`、`misc.json`

---

## 四、小型故事：角色任務與迷你任務

### 1. Afelis（角色任務，Lv. 1–70）

**角色任務**（Character Quest）沒有任務日誌，只是一個會跟著玩家在各城市出現的 NPC。Afelis 在 2.1.1 加入，是遊戲裡**最令人心碎**的小故事之一。以下全為本章轉述（語料只收了第一句「嘿！你也是剛來這裡的吧？我是 Afelis，你是……？」）。

- **Ragni**：Afelis 跟玩家一樣是剛到的新兵，**也想不起來到這裡之前的事**（「只有一種模糊的感覺……」），但很樂觀：「Wynn 就是我開始累積值得記住的回憶的地方！」
- **Detlas**：見過 Ragon 隊長，崇拜 Aegis 上將，轉述了湮滅的故事（「如果它真的出來，整片大陸就完了」）。提到一位讓自己印象深刻的女子：「她經歷了那麼多，卻還是笑著幫助別人。」
- **Nemract**：讀了古 Nemract 的歷史，敬佩倖存者「拒絕讓他們奮鬥過的一切被奪走」，準備前往 Gavel。
- **Llevigar**：在 Ragni 士兵的營地，對 Kymer 隊長對付獸人的強硬手段有意見。
- **Bucie**：聽說獸人與軍隊達成和解（《All Roads to Peace》）而鬆了一口氣，心思已經飛到被黯蝕包圍的 Gelibord。
- **Gelibord**：「我治不好森林，也抹不去苦難。我只能……陪著他們。」叫玩家離開，「我不想連你也失去」。一個孩子跑來喊「Afelis！媽媽需要人幫忙搬柴！」——Afelis 似乎已經成了某個家庭的一份子。
- **結局**（需完成《Lazarus 之坑》）：Afelis 在橋上獨自擋住黯蝕的野獸（Tiontaith Beast），身上已經負傷，要玩家把三個孩子帶回城：「別管我……我們之後再見，好嗎？」回頭時橋上只剩一行字：「人已經不在了。」一小時後，鎮民在山丘上立了紀念碑：「紀念 Afelis。願你的光為我們照亮通往更好明天的路。」Gelibord 的神父交給玩家 **Afelis 的墜盒**。

墜盒的 lore（語料）：「Afelis 隨身帶著的一只金製小盒。裡面是一張照片，Afelis 在 Detlas 的廣場上擁抱著一名女子。盒蓋上刻著『A+E』。」——這名女子很可能就是 Detlas 那位「笑著幫助別人」的人，Gelibord 那個喊「媽媽」的孩子或許是她的孩子（推測；E 是誰，遊戲沒說）。

**翻譯注意**：wiki 註明 Afelis 是**非二元性別**角色，原文全程用 they。語料目前的作法是避開代名詞（「Afelis 隨身帶著……」），建議沿用；非用不可時比照 Gikyo 用「他們」，不要寫「他」或「她」。Afelis 胸前的花會隨劇情逐漸凋謝，最後一戰時染成紅色。

出處：wiki〈[Afelis](https://wynncraft.wiki.gg/wiki/Afelis)〉〈[Afelis's Locket](https://wynncraft.wiki.gg/wiki/Afelis%27s_Locket)〉；語料 `npc.json`、`gear-accessory.json` #0007

### 2. Angie（角色任務，Fruma）

Hyloch 出身、在 Aelumia 大學念書的畫家，正在旅行「描繪女王陛下賜予的一切美景」（本章轉述）。

- **Auburn Forest**：她在池塘邊寫生，說接下來要去「**農業區**」看「起伏的山丘、豐饒的田野、晴朗的天空」，還請玩家當模特兒。
- **Timasca 附近**：她發現所謂的「農業區」其實就是 **Industrial District**——「我原本期待一望無際的麥田、山坡上可愛的小屋、照料土地的和藹老人……不是這樣。」「為什麼要告訴我們這些謊話？女王陛下知道嗎？……不，她當然知道。」說完立刻收口：「我相信女王陛下自有道理。」
- 玩家護送她回車站，遇上 Roughworks 的混混：「喲，看看誰來了。兩個穿得光鮮的有錢人跑來我們的車站？來看動物喔？」打退之後，Angie 搭上回 Highlands 的火車，送給玩家 32 片 Liff 葉。

這個小故事把第六章的階級結構濃縮成兩次對話：Highlands 的人從小被告知工業區是田園，工人則把他們看成來動物園參觀的遊客。

出處：wiki〈[Angie](https://wynncraft.wiki.gg/wiki/Angie)〉；語料 `npc.json`

### 3. 迷你任務的告示

第七章 §6.6 已說明迷你任務「沒有故事也沒有謎題」。不過 Fruma 的幾張在地告示，本身就是一段世界觀（語料已收）：

- **討伐 Hedoro**：「女王陛下政府的委託：Industrial District 的全體居民請注意。Hedoro 問題已經失控……繼去年宣布的淨水計畫之後，我們將推行全新的 Hedoro 清除行動。」wiki 收的後文還寫著獎勵是「一週房租打九五折」——政府要居民拿命去清下水道，酬勞是少付一點房租。
- **收集 Stackpeak 蛋**：Zhiraok 統領替 Espren 的麵包師徵蛋，因為 Auburn 慶典快到了——統領說話還是一貫的數學腔（「照我算」）。Auburn 慶典就是《絕望的序曲》裡 Sui 回憶「大家一起笑著」的那場（第六章）。
- **收集白百合**（本章轉述）：Jayba 忘了週年紀念日的花，又不想弄髒衣服，「尤其現在還有那些**國家公敵**的傳聞」——Highlands 的日常裡，叛軍只是一則讓人不想出門的八卦。
- **獵殺長毛犀**（本章轉述）：首都的海關人員抱怨「從首都外面進來的貨物損壞增加，耽誤了進出首都的**監控**」——他在乎的不是商人的安危，是檢查流程。
- **收集 Liff 葉**：Liff 獵人 Russel 被「大熊座」（Ursa Major）弄傷了（語料譯名，見語料問題 9）。

出處：wiki〈[Slay Hedoro](https://wynncraft.wiki.gg/wiki/Slay_Hedoro)〉〈[Collect Stackpeak Eggs](https://wynncraft.wiki.gg/wiki/Collect_Stackpeak_Eggs)〉〈[Collect White Lilies](https://wynncraft.wiki.gg/wiki/Collect_White_Lilies)〉〈[Hunt Woolly Rhinos](https://wynncraft.wiki.gg/wiki/Hunt_Woolly_Rhinos)〉〈[Collect Liff Leaves](https://wynncraft.wiki.gg/wiki/Collect_Liff_Leaves)〉；語料 `quest.json`

---

## 五、節慶活動：四兄弟的故事

### 1. 四個節慶

節慶都在 Detlas 舉行，一年一輪：

| 節慶（語料） | 季節 | Detlas 變成 | 主持人 | 主要活動 |
|---|---|---|---|---|
| 英雄節（Festival of the Heroes） | 春 | Festive Detlas | **Aster**（說書人） | The Claim to Heroism（在 Talespun Citadel） |
| 營火節（Festival of the Bonfire） | 夏 | Bonfire Detlas | **營火看守人**（Bonfire Keeper，舊名 Bonfire Tender） | 爐火狩獵（語料）、The Kindling |
| 靈魂節（Festival of the Spirits） | 秋 | Spooky Detlas | **使者**（The Envoy）、嚮導、先知（後兩者語料） | 靈魂引渡（Guiding of the Spirits）、驅逐彼界（語料） |
| 暴雪節（Festival of the Blizzard） | 冬 | Snowy Detlas | **旅人**（The Traveler） | Blizzard Brawl、Wynnter Fair |

營火節是 2021 年最早的一個；每年第一次登入時畫面會出現一句「又到了一年的這個時候嗎？」，前面接季節各自的開場（營火節「火光照亮了大地……」、暴雪節「你感到一陣突來的寒風……」、靈魂節「死寂的夜裡，亡靈升起……」、英雄節「書頁翻開了一個全新的故事……」，本章自譯）。

### 2. 四兄弟的真相（英雄節 2023、2024）

**這是一條跨年度的主線，四位主持人是親兄弟。** Aster 是長子；三個弟弟就是營火看守人、使者與旅人。

**2023 年**：英雄節的 Detlas 東南角有一扇神祕的門，拿英雄節徽記就能打開，裡面是黑夜版的 Talespun Citadel，Aster 獨自坐在塔底（本章轉述）：

> 父母早逝，身為長子的 Aster 拉拔三個弟弟長大，一家人在郊區過得很快樂。他們小時候常跑去看 Bob 一眼。但其中一個弟弟——Aster 稱他為「看守人」——一心想證明自己，打聽到一座**能賜予偉大力量的祭壇**，瞞著 Aster 帶另外兩個弟弟去舉行儀式。Aster 發現他們失蹤，請人施法追蹤，趕到時儀式正在進行；看守人看到哥哥一驚，**儀式出了錯**，三人當場消失。祭壇的力量「沒有別人可以附著」，便附到了 Aster 身上。

從此 Aster 擁有把故事變成現實的力量——Talespun Citadel（「由故事紡成的城塞」）就是祭壇照著他從小對英雄的憧憬長出來的地方，「只在我允許時才存在」。他一次又一次想把弟弟們變回來，每次都差一點：「他們回來了，卻沒有名字。只是故事裡的角色。」——**看守人、聲音（the Voice）、流浪者**，也就是營火看守人、使者與旅人。他的魔法「會把我的意圖塑造成最適合成為好故事的樣子」，這一次就把他們綁在了 Wynn 的節慶上。玩家答應替他邀請三個弟弟來明年的英雄節。

**2024 年**：門上多了三道鎖，Aster 被自己的城塞鎖在門外。三道鎖各對應一個弟弟（營火、煙、火花），解開的謎題就是「Festive Detlas 之謎」；最後一道線索指向 **Isles of Fiction**——海上那片「憑空出現」、空氣裡滿是奇異魔法的群島（語料探索點），wiki 推測就是 Aster 的力量造出來的，島中央還有他為弟弟們立的紀念碑。

門開之後，三兄弟在城塞地下等著。營火看守人說「時候到了，故事已經走到我們可以……」，使者打斷他：「別再講什麼故事了！」——三人中只有營火看守人記得完整的經過；旅人的記憶被一陣「霧」吞噬，使者則不肯相信「自己的角色是被故事安排出來的」。使者當面指責：「那天的事全是你的錯。你就是非當英雄不可。」營火看守人承認：「……是。但我們早就走過那個錯了。」

Aster 下來後，旅人用他「花掉大半清醒」記住的唯一一句話安慰哥哥：「**那不是你的錯**。」使者說：「你可以哀悼，哥哥。但總有一天你得放下。」最後，營火看守人說出最殘酷也最溫柔的一段：

> 「我不是他了。他的野心讓他走向了終點——聽了那麼多英雄的故事，他為了成為英雄什麼都願意做。我只是他剩下的部分。」「我們都變了，你不也一樣嗎？成長就是改變，而我們總會在過程中失去一部分的自己。那個人也許不在了，你的悲傷是真的。但我們現在都在這裡，這樣不夠嗎？」（本章自譯）

他邀 Aster 在營火再次照亮這座城時來看他，「讓我們一起哀悼那份失去」。Aster 送給玩家神話法器 **Aster 的呼喚**（語料「Aster 的呼喚」）。

**伏筆與細節**：

- 英雄節的主題是「英雄」，而悲劇的起因正是「想當英雄」。Aster 為節慶選的三位英雄是 **Bob**、守護 Troms 的 **Tisaun**（第九章）、Gavel 家喻戶曉的 **Siegfried**；投稿裡還有人提名 Aegis 上將，以及「General Skien，真是什麼人都有」（第一章）。
- 2021 年暴雪節，旅人的「隱藏台詞」早就埋好了：給他看營火節與靈魂節的徽記後，他說「你提到兩個人，但還有第三個……不管發生了什麼，**那不是他的錯**。你得讓他知道。」——三年後才兌現（本章轉述）。
- 旅人說每年冬天他都會「像發條一樣」出現在 Detlas 城門外；暴雪節結束時，他會在一陣**黑煙**中消失（wiki）。
- Aster 稱使者為「聲音」，大概是因為他在靈魂節替亡者與彼界傳話（推測）。
- 英雄節的武器名多取自戲劇術語：Hamartia（悲劇英雄的致命缺陷）、Anagnorisis（真相大白）、Catharsis（淨化）、Prologue／Epilogue（wiki）。其中 **Hamartia** 正是解開「聲音之鎖」用的那把劍——四兄弟的悲劇就是一個標準的希臘悲劇（推測）。

出處：wiki〈[Aster](https://wynncraft.wiki.gg/wiki/Aster)〉〈[Bonfire Keeper](https://wynncraft.wiki.gg/wiki/Bonfire_Keeper)〉〈[The Envoy](https://wynncraft.wiki.gg/wiki/The_Envoy)〉〈[The Traveler](https://wynncraft.wiki.gg/wiki/The_Traveler)〉〈[Festival of the Heroes Mystery](https://wynncraft.wiki.gg/wiki/Festive_Detlas_Mystery)〉〈[Talespun Citadel](https://wynncraft.wiki.gg/wiki/Talespun_Citadel)〉；語料 `misc.json`（Aster 的呼喚、Isles of Fiction）

### 3. 靈魂節：彼界的使者

靈魂節的設定是：每年的**靈潮**讓生死之間的帷幕變薄，有的亡魂找不到路去「彼界」（The Beyond，語料），有的趁機逃回人世。使者與嚮導受「某位很有權勢的人」委託，請玩家把**迷途之魂**帶回 Detlas 中央的大坑、把**懸賞靈魂**驅逐回去（本章轉述）。

使者說老闆「有些工作在他那裡做不了……他忙著處理文書」，嚮導補一句「我不確定他**會不會**失望」。玩家在《不得安息》見過一位審閱紀錄、抱怨「靈魂越過我所能觸及之處」就會失去自己一部分的**死神**（第三章）——使者的老闆幾乎可以確定就是他（推測，遊戲沒有點名）。嚮導則說自己「曾經處在它們的位置」，所以不希望逃魂得逞；他也是唯一見過 Aster 魔法、並刻意不讓使者知道「自己的角色是被捏造出來的」的人：「他現在很滿足……我喜歡我們三個現在的樣子。」

**懸賞靈魂的評語**。每驅逐一個懸賞靈魂，使者與嚮導會評論幾句，每一句都像在指涉遊戲裡的某個人物，但都沒有點名（本章轉述；對應誰皆為推測，譯者翻這幾段時要保留含糊）：

| 靈魂 | 評語大意 | 可能的指涉（推測） |
|---|---|---|
| The Wanderer（流浪者） | 「捨棄凡人之身不是可以輕率的事。把同樣的事強加在別人身上，不給選擇……」 | 讓自己與他人不死的術士 |
| The Terror（恐懼） | 一件物品被賦予了生命，「生者的意志就是要延續」 | 被賦靈的器物 |
| The Taken（被奪者） | 「連安息的完整都不被允許……碎成片段，被偷走」 | 虛空或 Dern 奪走的靈魂（Sky 營地的虛空島就有名為 Taken 的怪物） |
| The Keeper（看守者） | 被困太久，忘了一切，只記得「曾經能撫平他們病痛的那個人的臉」 | 某位醫者的病人們 |
| The Executioner（處刑人） | 「她很快又會不見……一個**死不了**的靈魂。為了力量刻意把自己的靈魂掏空」 | 掏空靈魂換取力量的女性人物 |
| The Stalker（潛伏者） | 躲在陰影裡，「要是讓它逃了，我們再也找不到」 | — |
| The Hollowed（被掏空者） | 「它本來可以變成任何東西……卻被綁死在這個形狀裡」 | 換形者（第六章，「隨變化的念頭而動」） |

使者在「處刑人」那段突然激動起來（「到底是什麼讓人寧可掏空自己的靈魂去換力量！」），嚮導冷冷地說「這已經不是在說我們的目標了」——他說的其實是看守人弟弟（推測）。另外，**這張表的 The Keeper 與 Aster 口中的「看守人」是兩回事**，翻譯時不要讓兩者共用譯名而被誤讀。

出處：wiki〈[Festival of the Spirits](https://wynncraft.wiki.gg/wiki/Festival_of_the_Spirits)〉〈[The Envoy](https://wynncraft.wiki.gg/wiki/The_Envoy)〉〈[The Guide](https://wynncraft.wiki.gg/wiki/The_Guide)〉；語料 `label.json`（嚮導、先知）、`misc.json`（靈魂節、驅逐彼界）

### 4. Siegfried 上台

英雄節唯一一場「特別演出」由 **Siegfried** 本人登台。他講完「英雄要保護需要保護的人」，突然宣布「人群裡混進了一頭怪物！現身吧！」，演了一場打怪秀，然後說「多虧我，偉大的 Siegfried 在這裡保護了你們！」（本章轉述）。

第三章《Gavel 的英雄》已經揭露：Siegfried 從來不只一個人，後期的 Siegfried 只是 Siegfried 基金會雇來的演員，冒險故事的「危險」都是布景。拿這段對照，那頭「混進人群的怪物」多半也是安排好的（推測）——英雄節本身就是一場講英雄故事的演出，Siegfried 恰好是「被故事製造出來的英雄」的最佳範例，跟 Aster 的力量形成呼應。

出處：wiki〈[Aster](https://wynncraft.wiki.gg/wiki/Aster)〉（Special Performance）、〈[Siegfried](https://wynncraft.wiki.gg/wiki/Siegfried)〉

### 5. 暴雪節與營火節的細節

- **暴雪節的邀請**：玩家替旅人「找回記憶」或發邀請函，把各地任務的 NPC 請來 Detlas 過節（Enzan、Graken 將軍……）。陰謀家 Worid 的「禮物」在介面上被加了引號，暗示他又在騙人（wiki）。
- **節慶武器上的刻字**（古 Wynn 語／高階 Gavel 語，wiki）：營火節的 Bob 之杖刻「ETHE」、暴雪節的 Bob 之矛刻「FIER」、靈魂節的 Bob 之匕首刻「CHAK」、英雄節的 Bob 之弓刻「MAEL」——四個節慶分別紀念 Bob 的四位師父（第一章 §6：刺客 Chak、弓手 Mael、法師 Ethe、戰士 Fier）。Tasim 的長柄刀刻「ALLY」（盟友），Aledar 的匕首刻「WYNN」，Theorick 的法杖刻「T, IB」（Theorick、Ice Barrows）。
- **營火節的 NPC**：完成相關任務後，Ope、Orikal、Zuett、Garull、Mastro、Korzim，以及**完成《神化》後的 Tasim** 都會來營火邊。

出處：wiki〈[Festival of the Blizzard](https://wynncraft.wiki.gg/wiki/Festival_of_the_Blizzard)〉〈[Festival of the Bonfire](https://wynncraft.wiki.gg/wiki/Festival_of_the_Bonfire)〉〈[Festival of the Spirits](https://wynncraft.wiki.gg/wiki/Festival_of_the_Spirits)〉〈[Festival of the Heroes](https://wynncraft.wiki.gg/wiki/Festival_of_the_Heroes)〉

---

## 六、主要人物

| 名字 | 身分 | 在本章的作用 | 出現於 |
|---|---|---|---|
| Lootrun 主管／Lootrun 大師 | Silverbull Lootrun 部門 | 入會與開跑 | 總部、各營地 |
| Astraulus | Sky Islands 的法師 | 把玩家射上太空 | 挑戰 Astraulus' Comet |
| 典範者（Exemplar） | 虛空裡的冒牌神 | 吞噬信徒的身體長大 | 挑戰 False Respite |
| 湮滅（Annihilation） | 戰爭領域的惡魔 | 900 AP、985 AP、1000 AP 三度破門 | 毀滅前奏 |
| Aegis 上將 | Detlas 軍隊首領 | 985 AP 擊退湮滅、救下 Takan | Detlas 兵營 |
| Takan、Eveni | Nemract 指揮官、Troms 將軍 | 985 AP 聯軍將領 | Aegis 的回憶 |
| Rickeo Twain／大橋剋星 | Twain 家火之子，被腐敗喚回 | 燃燒的記憶 | Great Bridge |
| Afelis | 新兵（非二元） | 從 Ragni 一路走到 Gelibord，為保護孩子而死 | 角色任務 |
| Angie | Highlands 畫家 | 發現「農業區」是謊言 | 角色任務 |
| Aster | 說書人，四兄弟的長子 | 擁有把故事變成現實的力量 | 英雄節、營火節 |
| 營火看守人（Bonfire Keeper） | 次子之一，悲劇的起因 | 唯一記得一切的人 | 營火節、Talespun Citadel |
| 使者（The Envoy） | 「聲音」，替彼界工作 | 指責看守人，勸 Aster 放下 | 靈魂節 |
| 旅人（The Traveler） | 「流浪者」，記憶被霧吞噬 | 「那不是你的錯」 | 暴雪節 |
| 嚮導（The Guide） | 使者的同伴，曾是逃魂 | 對使者隱瞞真相 | 靈魂節 |
| Siegfried | Gavel 的「英雄」 | 英雄節的特別演出 | 英雄節 |

---

## 七、伏筆與暗示

- **Aegis 救了 Takan，又判了他弟弟死刑** → Aegis 說 985 年他把 Takan 拖到掩體後面才活下來；同一段對話裡他也說起被 Almuj 的 Amerigo 叫去，處決了**Takan 在 Almuj 當軍官的弟弟**（通匪罪），「我很好奇 Takan 會怎麼反應」——答案在第一章《傭兵》：Takan 叛變。
- **「我已經辦過太多場追悼會了」** → 第五章《彼界之旅》開頭 Aledar 說找到了 Aegis，「但他不太……高興」。一位不想再寫陣亡通知的上將，對一支自殺式的祕密任務不會高興（推測）。
- **英雄節的英雄名單** → 投稿裡的 Aegis 與 Skien 被刷掉；Aegis 自己說「勇敢的 Aegis 絕不屈服」那句口號「幾乎是真的」。英雄是被說出來的，這是整個英雄節的主題。
- **旅人 2021 年的「那不是他的錯」** → 2024 年英雄節的結局。
- **使者的老闆「忙著處理文書」** → 《不得安息》的死神（推測）。
- **Afelis 的失憶** → 跟玩家一樣「想不起來到這裡之前的事」，是第一章「被洗掉記憶的新兵」設定的又一個例子；Afelis 的故事等於是「如果玩家不是主角」的版本。
- **Angie 的「農業區」** → Fruma 的宣傳讓上層民眾以為工業區是田園；Fruma 突襲的「塗鴉宣傳的人」挑戰則站在反宣傳那一邊。
- **巨像碎片的神經名** → 峽谷巨像是一具巨大的「腦」，與第三章「巨像身世」對讀。
- **腐敗的軍階** → 腐敗大軍有副官到上將的完整編制，與「無腦的腐敗」的印象相反。
- **汙濁的海岸：腐敗難以渡海** → 解釋了 Corkus、海島與 Fruma 為何相對未受腐敗波及（推測）。

---

## 八、祕密與隱藏內容

- **Festive Detlas 之謎**（英雄節 2024）：三道鎖、三條線索（點燃四座火盆、拔出 Hamartia、拼地圖到機場的裂縫看「過去的幻象」、到 Isles of Fiction 取鑰匙）。門開時浮現古 Wynn 語「城塞開啟」「故事已經講完」，之後 Nohno 會在門外送一隻「毫不起眼的雞玩偶」（第三章，Nohno 迷上雞）。
- **英雄節 2023 的門**：用英雄節徽記開門，見 Aster。
- **Isles of Fiction**：Aster 為弟弟們立的紀念碑在中央島頂（wiki 冷知識）。
- **Snow Storm Detlas／靈魂節之謎**：暴雪節銀行附近有一根「奇怪的拐杖糖」；靈魂節 Detlas 地下有一隻「巨大的騙子老鼠」，牠甚至附身過懸賞靈魂的隊伍，讓使者與嚮導找不到牠的紀錄，只留下一塊起司。
- **Lootrun 的罕見事件**：毒廢之地的餵食孔、眼球隧道的「膿液之眼」、Olm 古城偶爾來助陣的「薄暮 Kantyr」（語料名牌）、Colossus Islet 的巨像面容。
- **烽火王宮背景的湮滅群**：第八章討伐戰的戰爭領域背景裡有好幾頭湮滅。
- **湮滅的模型**：設計者 Deusphage 說它的貼圖參考了生鴕鳥絞肉，肌肉纖維裡藏著「HELL」字樣（wiki）。

---

## 九、本章新出現的譯名

語料沒有的詞，**皆未定案**：

| 原文 | 本章用的譯名 | 說明 |
|---|---|---|
| Lootrun Division ranks：Rookie／Assistant／Sentinel／Elite／Admiral／Commander／Master | 新手／助理／哨衛／精銳／上將／指揮官／大師 | Elite 不要用「精英」，會撞 Radiant＝精英；Master 與 NPC「Lootrun 大師」同字，可接受 |
| Spelunk（挑戰類型） | 探洞 | 其餘：殲滅（Slay Mobs）、獵殺目標、摧毀、防守 |
| Imperitia（試煉） | 生疏 | 語料留原文 |
| 挑戰點名（The Armory、Shackles of the Past、Armistice Sapling、Graveyard of the Thousand、False Respite、Forlorn Library、Propaganda Vandal、Canopy Ambush 等） | 軍械庫、過往的枷鎖、停戰樹苗、千人墓場、虛假的安歇、被遺棄的圖書館、塗鴉宣傳的人、樹冠伏擊 | 語料未收，遊戲內是否顯示需 capture 確認 |
| Freed Spirit | 獲釋的亡魂 | 只說「謝謝你」 |
| Character Quest | 角色任務 | |
| Aster's title: the Storyteller | 說書人 | |
| Bonfire Keeper／Bonfire Tender | 營火看守人 | 避免用「守護者」（撞峽谷巨像的 Guardian 與 Keepers＝守秘人） |
| The Envoy（靈魂節 NPC） | 使者 | **撞任務《使者》（The Envoy Part I／II）**；若要區分可作「冥使」，待討論 |
| The Traveler | 旅人 | Aster 稱他 the Wanderer＝流浪者（照 GLOSSARY） |
| the Keeper／the Voice／the Wanderer（Aster 的稱呼） | 看守人／聲音／流浪者 | 與懸賞靈魂 The Keeper／The Wanderer 是不同的對象 |
| Talespun Citadel | Talespun 城塞（意為「由故事紡成的城塞」） | Citadel 語料有「城塞」「堡壘」「Citadel」三種寫法 |
| Spirit Tide | 靈潮 | |
| Lost Souls／Bounty Spirits | 迷途之魂／懸賞靈魂 | |
| Festive Detlas Mystery | Festive Detlas 之謎 | |
| New Sun | 新日 | 湮滅的終極技能 |
| Corrupted Cache | 腐敗寶匣 | 語料只收了 lore |

---

## 十、出處清單

- wiki（Lootrun）：[Lootrunning](https://wynncraft.wiki.gg/wiki/Lootrunning)、[Lootrun Chief](https://wynncraft.wiki.gg/wiki/Lootrun_Chief)、[Silverbull Company Headquarters](https://wynncraft.wiki.gg/wiki/Silverbull_Company_Headquarters)、[Silverbull Syndicates](https://wynncraft.wiki.gg/wiki/Silverbull_Syndicates)、[Canyon of the Lost Excursion (South)](https://wynncraft.wiki.gg/wiki/Canyon_of_the_Lost_Excursion_(South))、[The Corkus Traversal](https://wynncraft.wiki.gg/wiki/The_Corkus_Traversal)、[Molten Heights Hike](https://wynncraft.wiki.gg/wiki/Molten_Heights_Hike)、[Sky Islands Exploration](https://wynncraft.wiki.gg/wiki/Sky_Islands_Exploration)、[Silent Expanse Expedition](https://wynncraft.wiki.gg/wiki/Silent_Expanse_Expedition)、[The Fruma Foray (West)](https://wynncraft.wiki.gg/wiki/The_Fruma_Foray_(West))、[The Fruma Foray (East)](https://wynncraft.wiki.gg/wiki/The_Fruma_Foray_(East))、[Exemplar](https://wynncraft.wiki.gg/wiki/Exemplar)、[The Cultivator](https://wynncraft.wiki.gg/wiki/The_Cultivator)
- wiki（世界事件）：[World Events](https://wynncraft.wiki.gg/wiki/World_Events) 與其分類下 68 個事件頁、[Annihilation](https://wynncraft.wiki.gg/wiki/Annihilation)、[Prelude to Annihilation](https://wynncraft.wiki.gg/wiki/Prelude_to_Annihilation)、[Admiral Aegis](https://wynncraft.wiki.gg/wiki/Admiral_Aegis)、[Corrupted Cache](https://wynncraft.wiki.gg/wiki/Corrupted_Cache)、[Bane of the Great Bridge](https://wynncraft.wiki.gg/wiki/Bane_of_the_Great_Bridge)、[Sayleros' Brother](https://wynncraft.wiki.gg/wiki/Sayleros%27_Brother)
- wiki（小型故事）：[Afelis](https://wynncraft.wiki.gg/wiki/Afelis)、[Afelis's Locket](https://wynncraft.wiki.gg/wiki/Afelis%27s_Locket)、[Angie](https://wynncraft.wiki.gg/wiki/Angie)、[Slay Hedoro](https://wynncraft.wiki.gg/wiki/Slay_Hedoro)、[Collect Stackpeak Eggs](https://wynncraft.wiki.gg/wiki/Collect_Stackpeak_Eggs)、[Collect White Lilies](https://wynncraft.wiki.gg/wiki/Collect_White_Lilies)、[Hunt Woolly Rhinos](https://wynncraft.wiki.gg/wiki/Hunt_Woolly_Rhinos)、[Hunt Banshee Stags](https://wynncraft.wiki.gg/wiki/Hunt_Banshee_Stags)、[Collect Liff Leaves](https://wynncraft.wiki.gg/wiki/Collect_Liff_Leaves)、[Gather Abyssal Matter](https://wynncraft.wiki.gg/wiki/Gather_Abyssal_Matter)
- wiki（節慶）：[Festival of the Heroes](https://wynncraft.wiki.gg/wiki/Festival_of_the_Heroes)、[Festival of the Bonfire](https://wynncraft.wiki.gg/wiki/Festival_of_the_Bonfire)、[Festival of the Spirits](https://wynncraft.wiki.gg/wiki/Festival_of_the_Spirits)、[Festival of the Blizzard](https://wynncraft.wiki.gg/wiki/Festival_of_the_Blizzard)、[Aster](https://wynncraft.wiki.gg/wiki/Aster)、[Bonfire Keeper](https://wynncraft.wiki.gg/wiki/Bonfire_Keeper)、[The Envoy](https://wynncraft.wiki.gg/wiki/The_Envoy)、[The Traveler](https://wynncraft.wiki.gg/wiki/The_Traveler)、[The Guide](https://wynncraft.wiki.gg/wiki/The_Guide)、[Festival of the Heroes Mystery](https://wynncraft.wiki.gg/wiki/Festive_Detlas_Mystery)、[Talespun Citadel](https://wynncraft.wiki.gg/wiki/Talespun_Citadel)、[Siegfried](https://wynncraft.wiki.gg/wiki/Siegfried)
- wiki 上**不存在**的頁：Mini-Quests（迷你任務改從各任務頁與 Quests 頁取材）、Lootrun Master、Canyon of the Lost Excursion (North)（北線尚未推出）、Kandrekk Soulweaver、Astraulus、Infernal Adjutant 等事件首領的獨立頁
- 語料：`lootrun.json`、`misc.json`（事件名、湮滅、腐敗寶匣、節慶名）、`quest.json`（事件說明、迷你任務告示）、`npc.json`（名牌）、`label.json`、`discovery-name.json`、`discovery.json`、`gear-accessory.json`、`quest/aldorei-s-secret-part-i.json`、`quest/cook-assistant.json`
- 本書其他章：第一章（Bob 的四位師父、Twain 家、Takan）、第三章（《不得安息》的死神、《Gavel 的英雄》）、第四章（Wybel、Farcor、《使者》）、第五章（Silent Expanse、《彼界之旅》）、第六章（Fruma 的階級與宣傳）、第七章（迷你任務、〈呱鳴怪談〉）、第八章（Silverbull 總部、烽火王宮）、第九章（Bloodbath、魂墨胸甲、Sayleros 兄弟）

---

## 本章發現的語料問題

沒有動語料，只記錄。

| # | 檔案／條目 | 問題 | 建議 |
|---|---|---|---|
| 1 | `quest/aldorei-s-secret-part-i.json` #000–#008 | 九句全是 Silverbull 總部的台詞（「加入我們的討伐戰部門」「找我們的 Lootrun 主管談談」「祝你往後的討伐戰一切順利」等），#004 還標著 speaker「Silverbull 階級專家」，跟《Aldorei 的祕密 第一部》無關；任務內容從 #009 才開始 | 搬到 quest.json（不屬任何任務的台詞），七個語言一起，比照 #1373 的作法 **已處理（#1411）** |
| 2 | `quest/cook-assistant.json` #021 | speaker Afelis 的「我真的很想做點有意義的事」是 Afelis 在 Ragni 的角色任務台詞，不是《廚師助理》 | 搬到 npc.json 或 quest.json **已處理（#1411）** |
| 3 | `npc.json`「Burning Iris」 | 譯「燃燒鳶尾」（花）；這是湮滅「戰爭之眼」召喚的眼球怪，與「惡魔凝視」成對，iris 指**虹膜** | 改「燃燒虹膜」或「燃燒之瞳」 **已處理（#1411）** |
| 4 | `npc.json`「Padrone」 | 譯「教父」；首領出現在「過勞工會」，padrone 是義大利文「工頭、老闆」，「教父」會讀成黑手黨 | 改「工頭」 **已處理（#1411）**：「工頭」已是 Foreman 的譯名，改「老闆」 |
| 5 | `quest.json`「From the decay of the earth…」（不安的遭遇說明） | decay 譯「腐朽」；wiki 原文連到 [[Decay]]，同組的光之使者、脫韁幼龍都譯「黯蝕」 | 改「從大地的黯蝕裡……」 **已處理（#1411）** |
| 6 | `misc.json`「Player was annihilated.」 | 字面的 Player 被當成原文收進來，應為 {u}；was annihilated 是首領名雙關，譯「殲滅」斷了連結 | 改 {u} 模板，譯「{u} 被湮滅了」 **已處理（#1411）** |
| 7 | `misc.json` 腐敗寶匣 lore「Annihilation has been averted…」 | 大寫的 Annihilation 譯「毀滅」，與首領名「湮滅」不一致（README 待查 #14 已把武器 lore 改成「湮滅」） | 改「湮滅暫時被擋下了」 **已處理（#1411）** |
| 8 | `misc.json`「Absolution Sheep fell to inevitable extinction.」 | 帶具體名字的死亡訊息，dst 空白；應是 capture 時把某個名字整句收進來 | 刪除（已有 {u} 版本） **已處理（#1411）** |
| 9 | `npc.json`／`quest.json`「Ursa Major」 | 譯「大熊座」，讀起來是星座；這是 Auburn Forest 的巨熊（另有 Ursa Minor 小熊） | 「大熊」，或保留雙關另議 |
| 10 | Woolly Rhino | 迷你任務名「長毛犀」、名牌與 label「長毛犀牛」、任務目標留英文「Woolly Rhino」 | 統一 **已處理（#1411）** |
| 11 | Syndicate | `misc.json`「集團書卷欄位」與「聯合會！請前往 Silverbull」兩種譯法 | 統一（建議「集團」） |
| 12 | Apiary Hive（世界事件） | 譯「蜂巢」，撞 10-24 定案的 Qira 的 the Hive＝蜂巢 | 事件名改「蜂房」或「養蜂巢」 |
| 13 | Citadel（Aelumia） | `discovery.json` 留「Citadel」、皇家鍊金師說明作「城塞」、事件名「堡壘營房」 | 統一（ja 已定「城塞」） |
| 14 | `misc.json` 任務名 Brothers Return「歸來的兄長」 | 譯名假定 Sayleros 的兄弟是**哥哥**；原文與 wiki 都沒交代長幼，名牌也刻意寫成「Sayleros 那位兄弟」（第九章已討論歧義） | 確認劇情；不確定就改「兄弟歸來」 |
| 15 | Visit from Beyond／The Beyond | 事件名「彼方來訪」、靈魂節的 The Beyond「彼界」；兩者指的不是同一處（前者是黑暗），目前不同譯法反而正確，**記錄以免被統一錯** | 不改 |
