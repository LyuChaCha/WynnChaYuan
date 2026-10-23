# 第十一章　人物誌與地名誌

> 劇透提醒：這是全書的索引，會直接寫出幾乎所有主線與支線的身分揭曉，包括 Fruma 女王的真身、Aledar 的結局、WynnExcavation 與冒牌將軍的真相、《遺失的那一塊》與《真面目》的反轉。還沒玩完的話，只查你要的那一列就好。

## 一、這份索引怎麼用

這一章不講故事，是給譯者**查名字**用的：看到一個人名、地名或組織名，想知道「他是誰、語料現在怎麼寫、在哪幾個任務說話、男的女的、哪裡容易翻錯」，就來這裡找。

- **人物誌**（第二節）依地區與陣營分 17 組，每組一張表。資料來自第一到第十章的人物表與正文，再對上語料：「出現的任務」是從 `zh_tw/quest/*.json`、`zh_tw/secret/*.json` 的 speaker 欄位統計的（括號是句數，最多列五個任務），「繁中寫法」取自 `npc.json`／`label.json` 的名牌。
- **地名誌**（第三節）收地名、地區、界域、勢力與種族。
- **易錯處總表**（第四節）把散在各列的陷阱集中起來：一人多名、同形異義、speaker 欄位的特殊寫法、名字裡的變位字。
- **附錄**是語料裡其他有台詞的具名 NPC，用腳本整理，只列名字、寫法、任務。
- 「章節」欄是連結，指向提到這個名字的各章；因為是用英文名比對的，偶爾會連到同名的另一個人或物品，以表裡的「身分」為準。

### 名字怎麼寫：語料的慣例

整理下來，語料對名字有幾條很一致的慣例，新譯文照著寫就不會出錯：

1. **人名、地名、地區名留英文。** 第一次出現可以在旁邊加說明，但不要音譯（不寫「拉莉」「卡戎」）。地區名照「發現地區：」那一行。例外是有意義的代號與稱號，見第 4 條。
2. **頭銜分兩種位置。** 軍階、官職、敬稱放名字**後面**：Aegis 上將、Kymer 隊長、Birch 長老、Two 統領、Picard 博士、Hart 偵探、Van 中尉、Effal 招募官、Graken 將軍、Alvin 鎮長、Hurlay 爵士。職業、身分、處境放名字**前面**：農夫 Cevalus、挖掘員 Placardus、礦工 Linton、叛徒 Amadel、主教 Kaideus、學徒 Arkech、乘客 Nancy、店主 Gibbs、代表 Ernold、廚師 Hamsey、酋長 Paku、巫師 Markque、治療師 Nami、皇家顧問 Carlos。Captain 在船上譯「船長」（Ackbar、Olof、Redbeard），在軍隊譯「隊長」。Old 譯「老」（老 Siwel、老 Caritat），但 Old Man Martyn、Sensei Miyagi、Mother Aris 整串留原文。
3. **代名詞。** GLOSSARY 的原則是生物用「他」，有三種例外已經定案：神祇用「祂」（Orphion、Saphanis，含原文大寫的 He／Him）；有明確性別的角色照原文（Ozoth 用「她」）；Gikyo 照原文，別人稱他們（they）、自稱它（it）。Fruma 女王講過去的自己用大寫 It，譯「它」。
4. **代號與稱號翻成中文。** 綻放之刃的花名（泣血薔薇、暮色花園……）、Sui 的化名「水草」、Syndra 的「母獅」、統領的稱號（晨星、永動之風）都翻；但它們在 speaker 欄位仍是英文。
5. **speaker 欄位不要動。** speaker、src 是照遊戲抄的比對資料，就算遊戲自己拼錯（Recruiter Ollisy）也照留，只在譯文改正。
6. **名字裡藏著變位字或雙關的，一定留原文**，不然梗就消失了（Dren＝Dern、Nilrem＝Merlin，見 4.4）。

## 二、人物誌

欄位：**英文名**（原文；speaker 欄位或名牌的寫法）｜**繁中寫法**（語料目前的寫法；留英文就寫英文）｜**身分與陣營**｜**出現的任務**（有台詞的任務，括號是語料句數；沒有台詞的寫出處）｜**章節**｜**代名詞與性別**｜**易錯處**。代名詞標「（語料）」的是從原文 he／she 統計來的，只有一兩處的不列。

### 2.1 神祇、界域巨獸與跨區的存在

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Anathema（Her Majesty） | 女王陛下（台詞與 speaker）／Anathema（首領名牌） | 戰爭的化身、戰爭領域的界域巨獸；200 AP 起以人類外殼統治 Fruma；腐敗的源頭 | 《神化》(51)、《女王的新兵》(46)、《變革迴響》(40)、《希望的合奏》(32)、《詛咒之人》(12) 等 7 個 | [01](01-wynn.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 她（女王）；講過去的自己用大寫 It，譯「它」 | 原文大寫 War 常指她或戰爭領域，不是一般的「戰爭」；speaker 欄位是 Her Majesty，不是名字；Fruma 篇揭曉前不要在譯文裡提前點破 |
| Orphion | Orphion | 光的化身、光之領域的界域巨獸，精靈信仰的神；被寄生體附身 | 《斬草除根》《尋光》《光之領域》、討伐戰 Orphion 的光之樞紐 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 祂（已定案 10-23，含原文大寫 He／Him） | 叫玩家「我的脈搏」（已統一，不寫「脈動」）；首領名牌「Orphion，消逝之光」；與 Saphanis 是雙生子 |
| Saphanis | Saphanis | 黑暗的界域巨獸，舊稱「Dern 野獸」；圍攻 Olm、挖出自己的眼睛交給 Bak'al | 探索點〈最後的時刻〉〈Silent Expanse 的祕密〉〈Ne du Valeos du Ellach〉 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 祂 | 2.2.0 才正式命名，舊資料寫 Dern Beast；名牌「Dern 野獸」另有一般怪物，別混 |
| 眼（The Eye） | 眼 | Saphanis 的眼睛，「Wynn 的監視者」；坐鎮詭異瞭望台 | 詭異瞭望台、《美妙旅程》 | [04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md) | 它 | 頭銜帶 overseer／watcher，《美妙旅程》#276「overseen from afar」要譯出「監視」；台詞是愛爾蘭語，留原文 |
| 寄生體（The Parasite） | 寄生體 | 從黑暗領域入侵 Gavel 的生物；黯蝕的源頭，後來附上 Orphion | 光之領域系列、Orphion 的光之樞紐 | [02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[08](08-dungeons-and-raids.md) | 它 | 首領名「寄生體」；泛指的小蟲才用「寄生蟲」；幼體 Hatchling＝幼雛，Dernmite＝Dern 蟎 |
| 無名者（The Nameless） | 無名者 | 來自 Dern 的異常存在；製造虛空洞，想逃出去 | 討伐戰無名異常體 | [05](05-silent-expanse-and-dern.md)、[08](08-dungeons-and-raids.md) | 它 | 討伐戰名「無名異常體」、首領「無名者」，兩個詞不同；名牌亂碼照留 |
| ???（獵手的呼喚） | ??? | 漂浮的無名黑色骷髏，自稱「我們」，「不是黑暗也不是光明」 | 《獵手的呼喚》 | [05](05-silent-expanse-and-dern.md) | 自稱「我們」 | speaker 一律是 ???，同一個 ??? 在其他任務指的是別人（語料共 341 句、橫跨幾十個任務）；外型像已移除的 Heliolux |
| Bak'al | Bak'al | 被腐敗的人類士兵，Anathema 的第一個後裔；率腐敗大軍八百年，後改投 Saphanis | 《獵手的呼喚》(27) | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | 自稱「我們」的段落照原文；「為了戰爭（大寫 War）的征服」指 Anathema；技能 Bak'al's Grasp 會改變 Anathema 的台詞 |
| 世界腐敗者（Corrupter of Worlds） | 世界腐敗者 | 第一個腐敗者；傳說是第一個走進傳送門的採礦隊長 | 《傳說神殿》 | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md) | — | 「礦工說」只是傳聞、後被 Anathema 否定，譯文的「據說」不能拿掉 |
| 死神（Death） | 死神 | 死亡的化身；揭露世界沙漏與玩家「不滅的火花」 | 《不得安息》(152) | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[07](07-secrets.md) | — | 〈死亡的警告〉全大寫是死神的語氣，排版要保留；Tower of Ascension 頂端「自稱死神」的是冒牌貨，別當成同一個 |
| Heliolux | Heliolux | 已移除的光之領域 NPC；1.20 以前由偽裝成村民的 Tiek 擔任 | （已移除） | [02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md) | — | 只在改版史與外型比對中出現，現行劇情沒有這個人 |

### 2.2 Wynn 行省：新兵、Detlas 與軍方

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Aledar | Aledar | 與玩家同梯的新兵，前風中之葉成員；Detlas 的士兵，在 Silent Expanse 主動被獻祭 | 《彼界之旅》(98)、《逐深之旅》(73)、《傭兵》(35)、《國王的新兵》(34)、《蛛群登巔》(31) 等 9 個 | [01](01-wynn.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 他的死是 Fruma 篇 Tasim 與玩家關係的核心；在《獵手的呼喚》是幻象 |
| Tasim | Tasim／Tasim 統領 | 與玩家同梯的新兵，天生會治療魔法的受詛者；後來成為最新的統領（祝福「意念」），最後倒戈 | 《女王的新兵》(215)、《真面目》(136)、《神化》(127)、《絕望的序曲》(49)、《國王的新兵》(46) 等 14 個 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | speaker 有 Tasim 與 Sovereign Tasim 兩種，同一個人；統領頭銜放名字後面 |
| Admiral Aegis | Aegis 上將 | Detlas 的上將，新兵的上司；985 AP 率軍擊退湮滅 | 《蘑菇人》(12)、《蛛群登巔》(7)、《元素演練》(5)、《獵手的呼喚》(3) | [01](01-wynn.md)、[05](05-silent-expanse-and-dern.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 物品 Admiral's Aegis 的 Aegis 是同一人，不是「神盾」 |
| Elphaba（Elphie） | Elphaba | Detlas 的軍官，《彼界之旅》的發起人；事先知道要獻祭 | 《彼界之旅》(49)、《奪回據點》(39) | [05](05-silent-expanse-and-dern.md) | 她 | 暱稱 Elphie 是同一人 |
| Lucio | Lucio | 魯莽的戰士；被執念者同化成「痴迷的執念者」 | 《彼界之旅》(31) | [05](05-silent-expanse-and-dern.md) | 他 | — |
| Dren | Dren | 鐵匠；說出「黑暗」是第六種元素 | 《彼界之旅》(17) | [05](05-silent-expanse-and-dern.md) | 他 | 名字是 Dern 的字母重排，譯文必須留原文才看得出來 |
| Caid | Caid | 被流放到 Wynn 的竊賊，記憶受試者；最後賠上自己的船救了城 | 《尋回過往》(53) | [01](01-wynn.md) | 他 | — |
| Reynauld | Reynauld | 被流放的失勢軍官，記憶受試者 | 《尋回過往》(53)、《女王的新兵》(21) | [01](01-wynn.md)、[06](06-fruma.md) | 他 | — |
| Dr. Picard | Picard 博士 | 研究記憶的博士，造出記憶復原化球，揭開 Fruma 的放逐制度 | 《女王的新兵》(58)、《尋回過往》(55) | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[06](06-fruma.md) | 他 | 玩家的記憶讓化球爆炸——「本來不存在的記憶」是玩家身世的伏筆 |
| Admiral Amerigo | Amerigo 上將 | Almuj 的上將 | 《傭兵》(17) | [01](01-wynn.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | — |
| Takan | Takan | 叛變的 Nemract 指揮官；985 AP 曾與 Aegis 一同擊退湮滅 | 《傭兵》(15)、《不得安息》(1) | [01](01-wynn.md)、[09](09-item-lore-index.md) | 他 | 物品 Ba'al's Betrayal 是他的舊名（Takan's Treachery 是改名後），不要譯成「巴力」 |
| Eveni | Eveni | 985 AP 與 Aegis、Takan 率聯軍擊退湮滅的將領 | 〈狼之神殿〉 | [01](01-wynn.md) | — | 只在年表出現 |
| Selium（Sovereign Selium） | Selium 統領 | Fruma 的統領；900 AP 來到 Ragni 訂下流放新兵的約定；主持 Hallowground 計畫，978 AP 推定死亡 | 《真面目》檔案櫃（語料未收） | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[06](06-fruma.md) | — | 計畫文件語料沒收，引用時標「本章自譯」 |
| Amadel | Amadel／叛徒 Amadel | WynnExcavation 的首領，假扮反抗軍首領，利用玩家集齊 Olm 水晶 | 《WynnExcavation D 號遺址》(19)、《WynnExcavation C 號遺址》(8)、《獵手的呼喚》(7) | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[05](05-silent-expanse-and-dern.md) | 他 | speaker 有五種寫法（WynnExcavation Leader／Traitor／Corrupted／Shadow），都是同一人；「叛徒」放名字前面 |
| Lykron／Flendar | 挖掘員 Lykron／Flendar | WynnExcavation 的挖掘員與部下，在 B 號遺址設局把玩家困進死路 | 《WynnExcavation B 號遺址》(12) | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md) | — | Lykron 講到「遺跡」時那一聲「呃……」是破綻 |
| Junes | Junes | 考古學家，帶玩家探索沙漠遺跡、取回皇帝之杖 | 《黃沙王朝》(17) | [01](01-wynn.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | — | 物品 lore 稱「Junes 博士」 |

### 2.3 Bob、四位大師與 Twain 家

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Bob（Robert） | Bob | Wynn 的英雄；854 AP 生於 Ragni 下水道，886 AP 擊退 Bak'al，984 AP 為封印黑暗生物而失蹤 | 《Bob 的傳說》《Bob 的失落之魂》《轉世重生》 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 本名 Robert；人類與 Olm 混血是 wiki〈The Olm〉的說法，〈Bob〉頁列為理論，譯文別寫成定論；傳說裡的數字被參數化成 {~}，不要改成概數 |
| Momo | Momo | Ragni 最窮的單親媽媽，收養並命名 Robert | 《Bob 的傳說》 | [01](01-wynn.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 她 | 她的話被印在凋零首的商品上（兩位母親的呼應） |
| Chak | Chak | 刺客大師，Bob 的第一位老師（匕首） | 《Bob 的傳說》 | [01](01-wynn.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | — |
| Mael Twain | Mael | 靈魂法師、最後一個 Twain；守著大宅與墓園，教 Bob 弓術 | 《致命的失足》(3) | [01](01-wynn.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 遺囑「給最後一位榮譽 Twai——」被撕掉，斷字要保留 |
| Ethe | Ethe | 法師大師（法杖），住 Nivla 樹上村 | 《Bob 的傳說》 | [01](01-wynn.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — | — |
| Fier | Fier | 戰士大師（矛），在 Troms | 《Bob 的傳說》 | [01](01-wynn.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | — | — |
| Nilrem | Nilrem | 巫師，Bob 最好的朋友；偽造 Bob 的死訊與屍體 | 探索點〈Nilrem 的封緘信〉 | [01](01-wynn.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | 他 | 名字倒過來是 Merlin，必須留原文 |
| Tarod | Tarod | Bob 的老友，收著 Bob 的胸甲 | 《Bob 的失落之魂》(20) | [01](01-wynn.md)、[09](09-item-lore-index.md) | 他 | — |
| Marius Twain | Marius | Twain 家創始者，收養天賦孤兒；570 AP 葬禮上家族決裂 | 〈Twain 兩斷〉〈Twain 家的殞落〉 | [01](01-wynn.md)、[07](07-secrets.md) | 他 | — |
| Theorick Twain | Theorick | 冰之法師；589 AP 進門失敗後凍結 Nesaak 森林，冰封墳塚首領 | 《墮落者的宿命》(31)、《獵手的呼喚》(10) | [01](01-wynn.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 「他死後敵魂佔據身體」與「他沒有死」兩說並存；fall 別譯成「戰死」；into being cold 是冰魔法與冷漠的雙關 |
| Rickeo Twain | Rickeo | 火之法師；死後成為 Great Bridge 的禍首 | 〈Twain 家的殞落〉 | [10](10-lootruns-events-and-mini-quests.md) | 他 | 物品 Immolation（焚祭）指他 |
| Dwendle Twain | Dwendle | 風之法師 | 〈Twain 家的殞落〉 | [01](01-wynn.md) | 他 | — |
| Cluckles／Mooington／Baab | Cluckles／Mooington／Baab | Bob 分出力量的三隻動物（雞、牛、羊） | 《轉世重生》 | [01](01-wynn.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md) | — | 動物名是造字，留原文 |
| Telvu | Telvu | 法師，帶玩家回到 589 AP 查明 Theorick 的真相 | 《墮落者的宿命》(21) | [01](01-wynn.md) | — | — |
| Jenprest | Jenprest | Ragni 的老人；「她在下面躺了好多年」暗指凋零首 | 《Ragni 的下水道》(27)、《女王的新兵》(21) | [01](01-wynn.md)、[06](06-fruma.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | — | — |
| Llevigar 檔案管理員（Llevigar Archivist） | Llevigar 檔案管理員 | Llevigar 大學圖書館員，記得 Bob 欠書不還 | 探索點〈Llevigar 的祕密圖書館〉 | [07](07-secrets.md) | — | — |

### 2.4 Ragni、Nemract、Troms、叢林與 Nesaak

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Nick | Nick | 自稱「沙漠的 Sanda Claus」，Santa 的兄弟 | 《佳節有情》(52) | [01](01-wynn.md) | 他 | Sanda Claus＝Sand＋Santa 的雙關 |
| Thomas | Thomas／叛徒 Thomas | Pigman Ravine 的村民，起初不信苦力怕存在 | 《苦力怕入侵》(38)、《WynnExcavation D 號遺址》(9) | [01](01-wynn.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | 《苦力怕入侵》的 Thomas 與「叛徒 Thomas」是否同一人要看任務 |
| Ope | Ope | Detlas 的騙子，耍了 Tasim | 《補給與運送》(27)、《蟲害叢生》(6) | [01](01-wynn.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | — |
| Bishop Kaideus | 主教 Kaideus | Nemract 牛神教會的主教，扣下死者遺囑 | 《歸來的兄長》(18) | [01](01-wynn.md) | — | 「主教」放名字前面；Bovemist＝牛神信徒（已定案 10-24）、Holy Bovine＝聖牛在上 |
| Alfe | Alfe | 新來的 Fruma 新兵、教會見習生，整理千年古籍 | 《歸來的兄長》(8) | [01](01-wynn.md) | — | — |
| King of Ragni | Ragni 國王 | Ragni 的國王；「兩股力量相撞，就會有戰爭」 | 《女王的新兵》(14)、《國王的新兵》(8)、《歸鄉之旅》(6) | [01](01-wynn.md) | 他 | speaker 有 King of Ragni 與 Ragni's King 兩種 |
| Slykaar | Slykaar | Iboju 出身的薩滿，以活人獻祭守護 Troms，被放逐後投向黑暗；叢林遺跡首領 | 《腐敗之叛》(16) | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | 「Bob 已經失蹤好幾年了」把時間點定在 984 AP 之後 |
| General Skien | Skien 將軍 | Troms 將軍，被部下背叛、戰敗流放，死後被腐敗喚回 | 《幽徑》（市民閒談）、Skien 島 | [01](01-wynn.md)、[09](09-item-lore-index.md) | 他 | 物品 Skien's Madness／Paranoia 是同一人 |
| King of Troms | Troms 國王 | Troms 的國王 | 《WynnExcavation D 號遺址》(9) | [01](01-wynn.md) | 他 | — |
| Ragamal | Ragamal | Nemract 下水道住民，戰時對士兵大吼 | 探索點〈木箱吊車〉 | [01](01-wynn.md)、[09](09-item-lore-index.md) | 他 | 物品 Putrid 的 lore 是他 |
| Harv | Harv | 戰時 Nemract 的居民，抱怨傀儡守衛「一半都鏽死了」 | 隱藏探索點 | [01](01-wynn.md) | 他 | — |
| Entamis | Entamis | 叢林部族的聖樹，被腐敗怪物 Aenara 的蟲卵侵蝕 | 《族人有難》(3) | [01](01-wynn.md) | 她（語料） | — |
| Aryn | Aryn | 被千年石中怨靈附身的少年 | 《迷失叢林》(10) | [01](01-wynn.md) | 他 | speaker 另有 Aryn?（附身狀態） |
| Eppo | Eppo | Nesaak 居民，代表民間對 Theorick 的怨恨 | 《噬骨寒霜》(54) | [01](01-wynn.md) | 他 | — |
| Lozendu Haon | Lozendu Haon | Iboju 前任酋長，名字是 Tahkashtel 語「焦黑的骨」 | 祭壇 Tribal Sanctuary、物品 Charred Bone | [09](09-item-lore-index.md) | 他 | 部族語是舊 Fruma 語的後代，名字留原文 |
| Tisaun Sodeta | Tisaun | Troms 王子、Sodeta 公會創立者 | 物品 Tisaun's Valor／Honour／Proof、Sodeta Boots | [09](09-item-lore-index.md) | 他 | 只在物品 lore 出現 |

### 2.5 沙漠、Time Valley 與古代人物

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Hashr | Hashr | 沙漠皇帝的第 26 子，688 BP 弒父奪杖，招來埋葬帝國的沙暴；風沙古墓首領 | 〈沙漠王室宅邸〉《黃沙王朝》、風沙古墓 | [01](01-wynn.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | 名字是阿拉伯文「放逐」；年代 688 BP（舊資料寫 100／1400 BP） |
| 沙漠皇帝（Desert Emperor） | 沙漠皇帝 | 被 Hashr 弒殺的皇帝，皇帝之杖的主人 | 沙漠探索點 | [01](01-wynn.md) | 他 | 物品 Emperor's Sceptre 統一為「皇帝之杖」 |
| Old Man Martyn | Old Man Martyn | Time Valley 的守護者，懂 Olm、認識 Bob，迴避「你是不是人類」 | 《既視感》(30)、《Tempo Town 風波》(12) | [01](01-wynn.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | 名字整串留原文（npc.json 也留） |
| Garoth | Garoth，聖殿的守護者 | 最後的 Olm 法師，把時失聖殿凍結在過去；舊版是 Elkurn 的人類科學家 | 時失聖殿、腐敗失落聖所 | [01](01-wynn.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | 新舊版是兩個不同設定：腐敗失落聖所仍是舊版 |
| Asher | Asher | Time Valley 居民，因 Olm 鏟子陷入時間悖論 | 《既視感》(48) | [01](01-wynn.md)、[09](09-item-lore-index.md) | 他 | 物品 Asher's Relic 是同一人 |
| Anya | Anya | Olm 的先知；預見滅亡，打造「純光之武器」手鐲 | 探索點〈Olm 的命運〉 | [05](05-silent-expanse-and-dern.md)、[09](09-item-lore-index.md) | 她 | — |
| Froner | Froner | Olm 的天文學家，城市被火球轟炸前最後對話的人 | 探索點〈最後的時刻〉 | [05](05-silent-expanse-and-dern.md) | — | — |
| Kaen／Adan | Kaen／Adan | 開 Anya 墓的兩把鑰匙的主人 | Silent Expanse 探索點 | [05](05-silent-expanse-and-dern.md) | — | — |
| Remikas | Remikas | Gavel 第一位國王，古代戰士的典範 | 宏大典藏、《踏入道場》 | [03](03-gavel-gylia-and-canyon.md)、[09](09-item-lore-index.md) | 他 | 物品 Remikas' Authority／Righteousness 是同一人 |
| 「受愛戴的」Marika（Marika the Admired） | 「受愛戴的」Marika | Gavel 女王；她死後子孫爆發表親之戰 | （年表） | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md) | 她 | 第二輪統一，不寫「受景仰的」 |
| Raemin | Raemin | 431 BP 處決對手、登基為 Gavel 女王，表親之戰結束 | （年表） | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md) | 她 | — |
| Theden II | Theden II | Gavel 末代國王，750 AP 起義時已死 | 宏大典藏 | [03](03-gavel-gylia-and-canyon.md) | 他 | — |
| Prometheus（Elder Prometheus） | Prometheus 長老 | 0 AP 時的 Aldorei 長老，誤讀預視、下令砍倒森林守護者 | 《Aldorei 的祕密 第二部》(23) | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[09](09-item-lore-index.md) | 他 | 物品 Prometheus's Vision 已改「Prometheus」，不譯「普羅米修斯」 |

### 2.6 Gavel 西部：精靈、光與黯蝕

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Lari | Lari | Aldorei 出身的精靈，Orphion 的受選者，活了一千年；最後追著 Bak'al 進入黑暗的傳送門 | 《光之領域》(79)、《蟲洞》(63)、《尋光》(61)、《無頭歷史》(56)、《斬草除根》(54) 等 9 個 | [02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | 她 | chosen＝受選者，Scion＝後裔，兩個英文詞不同；咒文是愛爾蘭語，留原文；DON'T YOU-＝你休想——（已定案 10-24） |
| Dullahan | Dullahan | 有預視能力的精靈，Lari 的同伴；被斬首、被復活後墮落成收魂者 | 《無頭歷史》(54) | [02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md) | 他 | 名字留原文，不譯「無頭騎士」；他是精靈，不是怪物種族 |
| 森林守護者 | 森林守護者 | 與光之領域相連的巨樹；原本那棵 0 AP 被砍倒，現在那棵在《尋光》被喚醒 | 《尋光》《Aldorei 的祕密 第二部》 | [02](02-gavel-light-and-decay.md) | — | 與峽谷巨像的「守護者」（Guardian）不同 |
| Elder Oak | Oak 長老 | Aldorei 守密六長老之一，主張封鎖到底 | 《Aldorei 的祕密 第二部》(45) | [02](02-gavel-light-and-decay.md) | — | npc.json 有其他長老的名牌，唯獨沒有 Elder Oak；`Elder Oak {#}{#}`＝「古橡」是另一隻怪物（同系列有「古霜」），不要拿來套 |
| Elder Birch | Birch 長老 | 守密六長老之一，與其他長老決裂後被留在峽谷 | 《Aldorei 的祕密 第二部》(66) | [02](02-gavel-light-and-decay.md) | — | 長老名是樹名但留原文，不譯「樺樹」 |
| Elder Alder／Willow／Elm／Hickory／Rowan／Hawthorn | Alder 長老／Willow 長老／Elm 長老／Hickory 長老／Rowan 長老／Hawthorn 長老 | Aldorei 長老；Alder 想改革 | 《Aldorei 的祕密 第二部》(28) | [02](02-gavel-light-and-decay.md) | — | 同上 |
| Olon | Olon | Efilim 的精靈，熱愛陰謀論；追查父母之死，揭開精靈永生的真相 | 《Aldorei 的祕密 第二部》(209)、《Aldorei 的祕密 第一部》(2) | [02](02-gavel-light-and-decay.md) | 他 | — |
| Korben | Korben | Aldorei 的精靈，為救中毒的父親偷闖禁地 | 《Aldorei 的祕密 第一部》(16) | [02](02-gavel-light-and-decay.md) | 他 | — |
| Mané | Mané | Aldorei 的精靈；「這世紀只有我一個新生的精靈」 | 《Aldorei 的祕密 第二部》(33) | [02](02-gavel-light-and-decay.md) | — | 重音字母 é 要保留 |
| Mona | Mona | Aldorei 的精靈，不准人問年紀 | 《Aldorei 的祕密 第二部》(29) | [02](02-gavel-light-and-decay.md) | — | — |
| Amber | Amber | Aldorei「Sol 使館皇家特勤隊」的線民 | 《Aldorei 的祕密 第二部》(43) | [02](02-gavel-light-and-decay.md) | 她（語料） | 名字與寶石 amber（琥珀）同形：物品名的 Amber 要譯，人名不譯 |
| Vaeli／Tyana | Vaeli／Tyana | 被拒於故鄉門外的精靈姊妹，建立 Efilim | （探索點） | [02](02-gavel-light-and-decay.md) | 她 | — |
| Dr. Urelix | Urelix 博士／Urelix 科學家 | Olux 的魔像研究者，以活人換魔像，供應 Wynn 的腐敗之戰 | 《鋼鐵之心 第二部》(18)、《獵手的呼喚》(14)、《斬草除根》(4) | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[05](05-silent-expanse-and-dern.md) | 他 | speaker 有三種（Dr.／Scientist／裸名），npc.json 也分「博士」「科學家」兩種，指同一人 |
| Daxe | Daxe | Olux 的魔像師 | 《鋼鐵之心 第二部》(14) | [02](02-gavel-light-and-decay.md) | 他 | 魔像的「冷」（cold）是貫穿兩部的關鍵字 |
| Madeleine／Theodore | Madeleine／Theodore | 被做成魔像的父女：第一部的魔像是女兒，第二部是父親 | 《鋼鐵之心》第一部、第二部 | [02](02-gavel-light-and-decay.md) | 她／他 | 魔像拼名字時的錯位字元（Ma[ERROine）語料照留，是對的 |
| Olux 鎮長 | Olux 鎮長 | 被財政逼到與 Urelix 交易，想揭發時被玩家奉 Orphion 之命殺死 | 《斬草除根》、探索點 Shadowy Dealings | [02](02-gavel-light-and-decay.md) | 他 | speaker 只寫 Mayor，跟其他任務的鎮長共用 |
| Garvan | Garvan | 科學家，第一個查出黯蝕成因的人；把寄生體植入 Eileen，之後在 Kander 遊蕩一百五十年（流浪者） | 《空洞的寧靜》(57) | [02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[09](09-item-lore-index.md) | 他 | 說話口吃；《空洞的寧靜》speaker 曾整段錯位，已修 |
| Katarin Faltach | Katarin | Faltach 家女主人，與 Garvan 私通、靈魂綁在寶石上 | 《空洞的寧靜》(51) | [02](02-gavel-light-and-decay.md) | 她 | — |
| Eileen Faltach | Eileen | Katarin 的女兒，被植入寄生體的王位繼承人；下落不明 | 《空洞的寧靜》(4) | [02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[09](09-item-lore-index.md) | 她 | — |
| Aki／Lokno | Aki／Lokno | 保皇派村民（殺死 Katarin）／她的曾孫 | 《空洞的寧靜》 | [02](02-gavel-light-and-decay.md) | 她／— | — |
| 典獄長（The Warden） | 典獄長 | Lexdale 監獄的典獄長，「沒有過去的人」，替署名 D 的人收集靈魂 | 《禁忌牢獄》(21) | [02](02-gavel-light-and-decay.md) | — | 與 Fruma 的典獄長 Oru 不同人 |
| Burtur | Burtur | Gelibord 居民，帶玩家查 Lazarus 之坑 | 《Lazarus 之坑》(23) | [02](02-gavel-light-and-decay.md) | — | — |
| Hans／Referick | Hans／Referick | Gelibord 外圍的鄰居；Referick 是陰謀論者，靈魂被 Dullahan 抓走 | 《無頭歷史》(31) | [02](02-gavel-light-and-decay.md) | 他 | — |
| Revan | Revan | 監獄墓園的亡魂 | 隱藏探索點 Hallowed Ground | [02](02-gavel-light-and-decay.md) | 他 | — |
| Bip | Bip | 講 Lexdale 女巫八卦的人 | 《空洞的寧靜》(16) | [02](02-gavel-light-and-decay.md) | — | — |
| Amira | Amira | Lexdale 居民，被誣為女巫 | 《Lexdale 女巫審判》(8) | [02](02-gavel-light-and-decay.md) | 她 | 任務名已統一「女巫審判」 |
| Atisun Caritat | Atisun | Caritat 家最後的後代 | 《記憶的猜忌》(19) | [02](02-gavel-light-and-decay.md) | — | Caritat 家的 speaker 有 Mr.／Mister／Mrs.／Caritat Senior 四種 |
| Likeru／Elder Berusia | Likeru／Berusia 長老 | Efilim 居民，藍帽菇實驗 | 《破碎的心智》(17) | [02](02-gavel-light-and-decay.md) | 她（Likeru，語料） | — |
| Voer | Voer | 異國植物商人，偷了 Lari 的書、賣錯花 | 《死亡之哨》(17) | [02](02-gavel-light-and-decay.md) | — | 推車裡的《精靈草藥學之書》內文語料沒收 |
| Kalrow | Kalrow | 業餘術士 | 探索點〈召喚師的下場〉 | [02](02-gavel-light-and-decay.md) | 他 | — |
| van Garrett | van Garrett | Gavel 財政官，駁回 Olux 救濟款 | 探索點 | [02](02-gavel-light-and-decay.md) | — | — |
| Arrai-Veretel | Arrai-Veretel | 西 Gavel 獸人與哥布林的共主，Centerworld 堡壘 | 《條條大路達自由》(45) | [02](02-gavel-light-and-decay.md) | 她（wiki） | Rai 是營地首領的頭銜、Arrai 是全體之首，連字號要保留；1.20 以前的首領叫 Gorgu |
| Rai-Poxper | Poxper | 獸人營地首領，被玩家所殺 | 《烈焰復仇》(4) | [02](02-gavel-light-and-decay.md) | 他 | 2.0 以前叫 Gregg'r（裝備 Hide of Greggr） |
| Elisu／Ceifko／Veltu | Elisu／Ceifko／Veltu | 三座獸人營地的酋長（Cliffhearth、Earthpit、Trunkstump） | 《清掃營地》 | [02](02-gavel-light-and-decay.md) | — | Veltu 是獸人卻統領哥布林營地 |
| Captain Kymer | Kymer 隊長 | Gavel 軍官，派玩家去清掃獸人營地 | 《女王的新兵》(25)、《清掃營地》(11) | [02](02-gavel-light-and-decay.md)、[06](06-fruma.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 名字是前建築師 Mekyr 的變位字 |
| Dodegar Bandysnoot | Dodegar | Light Forest 的哈比鑄劍師 | 《究極的武器》(34) | [02](02-gavel-light-and-decay.md) | 他 | 全名只在 wiki；npc 名牌是 Dodegar |
| Arnod | Arnod | 房子一直燒的老人，假牙掉了講話含糊 | 《山中來客》(7) | [02](02-gavel-light-and-decay.md) | 他 | 大舌頭寫法是刻意的 |
| Fiona | Fiona | 《Aldorei 的祕密 第一部》的植物學者（目錄裡有 Death Whistle 的那本書）；另外《少女之塔》裡淑女養的寄生蟲寵物也叫 Fiona | 《Aldorei 的祕密 第一部》(14) | [02](02-gavel-light-and-decay.md) | 她 | 兩個 Fiona 不是同一個：寵物那隻是《史瑞克》公主名的反向玩笑；日記裡原文 it→her，中文前作「他」後作「她」（見第二章 A11） |
| Duvale | Duvale | Llevigar Plains 的農夫，買到會說出 Madeleine 的「瑕疵」魔像 | 《鋼鐵之心 第一部》(13) | [02](02-gavel-light-and-decay.md) | 他 | — |
| Detective Hart | Hart 偵探 | Olux 的偵探，調查市民失蹤 | 《鋼鐵之心 第二部》(15) | [02](02-gavel-light-and-decay.md) | — | — |
| Detective Jackson／Piere | Jackson 偵探／Piere | Llevigar 的偵探／被燒了房子的農夫；用停火協議的漏洞請人類出手 | 《烈焰復仇》(29) | [02](02-gavel-light-and-decay.md) | 他 | — |
| 密探（Agent） | 密探（本書）／Agent（npc.json 名牌留英文） | 不明組織的隕石研究者，依村民律法收繳隕石 | 《竊星者》(12) | [02](02-gavel-light-and-decay.md) | — | 名牌 Agent 在 npc.json 沒有翻 |
| Caissop | Caissop | Olux 的盲眼畫家，靠玩家的「描述」畫出三隻假傳說生物 | 《大師的傑作》(30) | [02](02-gavel-light-and-decay.md) | 他 | Picasso 的變位字；興奮時冒西班牙語、法語，語料照留 |
| Sherk | Sherk | 獨居沼澤的食人魔，向塔上的淑女求愛 | 《少女之塔》(22) | [02](02-gavel-light-and-decay.md) | 他 | Shrek 的變位字；把 Wynn 聽成 Win，叫玩家「Win 人」 |
| Tolem／Dereg／Kroac | Tolem／Dereg／Kroac | 《巨獸之影》的人物 | 《巨獸之影》(59) | [02](02-gavel-light-and-decay.md) | — | Tolem 氣到口誤的那句照原文笑點譯 |
| Zenam | Zenam | Llevigar 發電廠的協調人 | 《Llevigar 之心》(10) | [02](02-gavel-light-and-decay.md) | 他 | 工程師把他叫成 Zeman、Zebra，是笑點，照譯 |

### 2.7 Gavel 中東部：Cinfras、Gylia、失落者峽谷與 Qira

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Norler | Norler | Gylia 民兵領袖，以腐肉散播疾病贏得 Gerten 戰爭，創立 Gylia Watch | 探索點〈Gylia Watch 圖書館〉 | [03](03-gavel-gylia-and-canyon.md) | 他 | — |
| Cikal | Cikal | Gylia Watch 指揮官，偷 Gert 物資辦派對 | 《Gerts 大饑荒 第一部》(18)、《Gerts 大饑荒 第二部》(6) | [03](03-gavel-gylia-and-canyon.md) | 他 | — |
| Winie | Winie | 新進的 Gylia 哨兵，唯一清醒的守衛 | 《Gerts 大饑荒 第二部》(15) | [03](03-gavel-gylia-and-canyon.md) | — | — |
| Snaga | Gert 族長 Snaga | Gert 族長 | 《Gerts 大饑荒 第一部》(4) | [03](03-gavel-gylia-and-canyon.md) | — | 族長的倒裝文法是刻意的；「族長」放名字前面 |
| Korun／Nikoler | Korun／Nikoler | Lake Gylia 的兩位隕石科學家，嘴上互相仇視 | 《紫與藍》(72) | [03](03-gavel-gylia-and-canyon.md) | 他 | Nikoler 的「三個頭腦」是隕石魔力的暗示 |
| Yahya／Nohno／Mehme | Yahya／Nohno／Mehme | 被宇宙魔法所害的三兄弟 | 《???》《蘑菇人》《咕咕咯咯》 | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 任務清單顯示 ???，跟《獵手的呼喚》的 ??? 是兩回事 |
| Sohso | Sohso | 三兄弟的四弟；去 Fruma 研究宇宙瘋狂，977 AP 被 Two 推薦進 Hallowground 計畫 | （年表、《真面目》檔案櫃） | [03](03-gavel-gylia-and-canyon.md)、[06](06-fruma.md)、[07](07-secrets.md) | 他 | — |
| Helmund | Helmund | Gavel 隕石研究員，提出紫藍碎片＝混沌與秩序 | 探索點 Gavel 挖掘場 | [03](03-gavel-gylia-and-canyon.md) | 他 | — |
| Lorias／Rohem／Eminar | Lorias 偵探／Rohem／Eminar | 偵探／兇手／死者（Cinfras 的賭債兇殺案） | 《謀殺疑雲》(32)、《空洞的寧靜》(1) | [03](03-gavel-gylia-and-canyon.md) | 他 | — |
| Gawrick | Gawrick | 瘋狂魔法發明家，傳送卷軸發明人 | 《墜落的貨運》(19) | [03](03-gavel-gylia-and-canyon.md)、[09](09-item-lore-index.md) | 他 | 裝備「乖離」敘述的主角 |
| Ernold | 代表 Ernold | 飛船公司代表，為保住飯碗擊落自家飛船 | 《墜落的貨運》(15) | [03](03-gavel-gylia-and-canyon.md) | 他 | 「代表」放名字前面 |
| Siegfried／Gurix | Siegfried／Gurix | 被製造出來的「Gavel 的英雄」／扮演他的演員 | 《Gavel 的英雄》(138) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | speaker 標 Siegfried 的台詞其實是演員在講；「很有 Corkian 味」是伏筆 |
| 天空劫掠者女王 | 天空劫掠者女王 | 天空海盜之首，被玩家所殺 | 《危難航程》(3) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md) | 她 | 天空劫掠者全員女性，玩家被叫「姑娘」 |
| Gana／Irlok | Gana／Irlok | Bantisu 僧侶，峽谷的引路人 | 《不得安息》(16)、《峽谷嚮導》(11) | [03](03-gavel-gylia-and-canyon.md) | — | — |
| Seluc | Seluc | 困在崖上、一路嫌棄人類的觀光客 | 《峽谷嚮導》(45) | [03](03-gavel-gylia-and-canyon.md) | 他 | — |
| Lecade | Lecade | Kitrios Barracks 將軍；被換形者冒名頂替 | 《將軍下令》(67) | [03](03-gavel-gylia-and-canyon.md)、[09](09-item-lore-index.md) | 他 | speaker 有 Lecade 1、Lecade 2 兩個，真假將軍並存；冒牌貨把 Gavel 說成「碎石」（繁簡已定案） |
| Qira | Qira | 蜂巢（the Hive）的女主人；與 Gavel 互不侵犯，拒治黯蝕，為「黑暗」備戰 | 《Qira 蜂巢》(18) | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 她 | the Hive＝蜂巢（已定案 10-24），本工作樹的語料還有 24 句留 Hive |
| Yansur | Yansur | Qira 的僕人，揭露 Qira 與 Gavel 的盟約 | 《Qira 蜂巢》(44) | [03](03-gavel-gylia-and-canyon.md) | — | — |
| Gale Nasin | Gale | Fruma 出身（Nasin 家），畢業前夕出逃；後來成為 Qira 風之部隊的首領，現為亡靈 | 《Qira 蜂巢》(2) | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[06](06-fruma.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | 她 | 與名牌 `Gale Dragonling`（疾風幼龍）、裝備 Gale Burst 等的 gale（疾風）同形，人名不譯 |
| Sensei Miyagi | Sensei Miyagi | 櫻花道場的老師，與 Qira 以物易物、下圍棋 | 《踏入道場》(35) | [03](03-gavel-gylia-and-canyon.md) | 他 | 整串留原文（npc.json 也留） |
| Ozoth | Ozoth | 倖存的巨龍，「無盡點火者」；在 Thanos 孵蛋，後讓出永焰 | 《Thanos 儲藏庫》《臨界點》 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[09](09-item-lore-index.md) | 她（已定案 10-22，GLOSSARY「生物用他」的例外） | Everflame＝永焰、Eternal Flame＝永恆之焰，原文本來就是兩個詞 |
| Coulmis／Atila | Coulmis／Atila | Thanos 的矮人，委託偷龍蛋 | 《Thanos 儲藏庫》(37) | [03](03-gavel-gylia-and-canyon.md) | — | — |
| Eoric／Ardulf | Eoric／Ardulf | 非法人類城 Eltom 的鎮長／他的混血之子 | 《隱匿之城》(13) | [03](03-gavel-gylia-and-canyon.md) | 他 | — |
| Norsten／Peli | Norsten／Peli | Thesead 路上的守門人（被 Dramele 用水晶球操縱）／他的爺爺 | 《掠奪者的買路錢》(29) | [03](03-gavel-gylia-and-canyon.md) | 他 | 罵人的話是 Dramele 借他的嘴說的 |
| Clight | Clight 工頭 | Thesead 煤礦工頭，主張毀掉巨像 | 《礦坑金絲雀》(53) | [03](03-gavel-gylia-and-canyon.md) | 他 | — |
| 金絲雀（Canary） | 金絲雀 | 《礦坑金絲雀》裡對玩家唸俳句的幽影，最後要與之戰鬥 | 《礦坑金絲雀》(106) | [03](03-gavel-gylia-and-canyon.md) | — | speaker 留英文 Canary，台詞與目標譯「金絲雀」；俳句的音節形式譯文不必硬湊 |
| Drale | Drale | Woc 的科學家，想找回飛船讓流亡者回家 | 《莫名其牛》(97)、《隧道之患》(15) | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md) | 他 | — |
| General Elix | Elix 將軍 | Gavel 將軍，守著巨像穩定器 | 《臨界點》(21) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md) | — | — |
| Kelight | Kelight | 傳說神殿的守門人，被沉默打敗 | 《傳說神殿》(52) | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[09](09-item-lore-index.md) | 他 | — |
| Rayshyroth／Garull | Rayshyroth／Garull | 傳說神殿的學者／工匠 | 《傳說神殿》(30) | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | — |
| Svin | Svin | 傳說神殿的守秘人，調查 Cockatrice 傳說 | 《峽谷兀鷲》(14) | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | — | Keepers＝守秘人（不是「守護者」，避免與 Guardian 撞） |
| Psilo | Psilo | Cinfras County 蘑菇洞裡的地精 | 《大處著眼》(19) | [03](03-gavel-gylia-and-canyon.md) | 他 | — |
| Dramele | Dramele | 巫師；2.1 以前叫 Volmor | 《掠奪者的買路錢》(12) | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 名字取自 Adramelech；飾品 Dramele's Flair 是他 |

### 2.8 海洋與群島

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Dalben | Dalben | Mage Island 法師議會成員，承認議會為封印深淵雨靴毀掉 Sarnfic 村 | 《深淵之下》(13) | [04](04-ocean-corkus-heights-sky.md)、[09](09-item-lore-index.md) | — | — |
| Javier | Javier | Pirate Cove 的老海盜 | 《深淵之下》(18) | [04](04-ocean-corkus-heights-sky.md) | — | — |
| Redbeard | Redbeard 船長 | 傳說中的第一位航海家、海盜；巨帆墳場首領 | 《Redbeard 的寶藏》、巨帆墳場 | [04](04-ocean-corkus-heights-sky.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | 不譯「紅鬍子」（物品已改，#1370）；Barbarossa 譯「紅鬍子」才是對的 |
| Hastor | Hastor | 另一位海盜船長，寶藏藏在 Llevigar | 探索點〈海盜藏金〉 | [04](04-ocean-corkus-heights-sky.md) | 他 | — |
| Marston／Karkun／Martim／Argus | Marston／Karkun／Martim／Argus | 叛變的海盜船員，投奔 Redbeard | 《Redbeard 的寶藏》(35) | [04](04-ocean-corkus-heights-sky.md)、[08](08-dungeons-and-raids.md) | 他 | — |
| Damiral | Damiral | 退休水手，拿謎語請玩家找 Hastor 的寶藏 | 《海盜藏金》(11) | [04](04-ocean-corkus-heights-sky.md) | — | — |
| Seasum | Seasum | Mage Island 的小孩，把入學信讓給玩家 | 《Grook 魔法學院》(11) | [04](04-ocean-corkus-heights-sky.md) | — | — |
| Old Siwel／Siwel | 老 Siwel／Siwel | Dead Island 的鍊金術士，詛咒全島、背負永生 | 《Haven 的往昔》(36) | [04](04-ocean-corkus-heights-sky.md)、[09](09-item-lore-index.md) | 他 | 「老」放名字前面；兩個 speaker 是同一人的不同時期 |
| Relend | Relend | Jofash 的押韻老船長；984 AP 載 Bob 去 Gateway Island | 《美妙旅程》(70) | [01](01-wynn.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | 他 | 全程押韻，譯文要押 |
| Honip | Honip | Nemract 酒吧裡的海盜，把玩家下藥抓去打雜 | 《海上驚魂》(84) | [01](01-wynn.md)、[04](04-ocean-corkus-heights-sky.md) | 他 | — |
| Grook（NPC） | Grook | 幾個任務裡的同名 NPC | 《禁忌牢獄》(3)、《Aldorei 的祕密 第二部》(1)、《謀殺疑雲》(1)、《Redbeard 的寶藏》(1) | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md) | — | Grook 同時是雞形鳥、感嘆詞（Oh my grook、Thank grook）與學院名；speaker 標 Grook 的那幾句不全是同一個角色 |
| Carvel | Carvel | 航海大師，風暴中弄丟的弓漂到 Selchar | 物品 Maelstrom、Carvel's Creation／Sight | [09](09-item-lore-index.md) | 他 | — |

### 2.9 Corkus

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Markus Corkus T. | Markus Corkus T. | 850 AP 帶 Fruma 叛逃者坐熱氣球出走，建立 Corkus | （年表） | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[06](06-fruma.md) | 他 | wiki 沒有個人頁 |
| Maxie | Maxie | 自卑的 Corkian 青年，成長為工程師、議員；Ava 的夥伴、Rhay 的丈夫 | 《羽毛紛飛 第二部》(47)、《使者 第一部》(40)、《使者 第二部》(36)、《獵手的呼喚》(18)、《臨界點》(14) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md) | 他 | 名字是 QA 職位 Imaxe 的變位字 |
| Ava | Ava | 被放逐的 Avo 發明家，融合自然魔法與電魔法 | 《羽毛紛飛 第二部》(267)、《羽毛紛飛 第一部》(153) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 她 | Avo 單數、Avos 複數 |
| Efena | Efena | Corkus 總統 | 《羽毛紛飛 第二部》(80) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md) | 她 | — |
| Beltrude | Beltrude | Corkus 代表，起初掩蓋工廠問題 | 《使者》 | [04](04-ocean-corkus-heights-sky.md) | — | speaker 可能只寫 Corkus Delegate（Corkus 代表） |
| Rhay | Rhay | Corkian 工程師，負責壓制峽谷巨像的穩定裝置；Maxie 的妻子 | 《臨界點》(60) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md) | 她 | 《臨界點》#313–315 曾誤標成 Rhay（實為 Ineos），已修 |
| Rensa | Rensa | Corkian 分離主義者首領，下藥陷害玩家後逃脫 | 《五味雜陳》(16) | [04](04-ocean-corkus-heights-sky.md) | — | 「最終敵人」「真正戰爭的前奏」是伏筆 |
| Nettik | Nettik | 燈塔主人，違法自製機甲 | 《鐵皮末路》(10) | [04](04-ocean-corkus-heights-sky.md) | — | — |
| Mech X | Mech X | 工廠的新機甲 | 《使者 第二部》(2)、《使者 第一部》(1) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md) | 它 | — |
| Avos 族長 | Avos 族長 | Avos 現任族長，放逐 Ava | 《羽毛紛飛 第一部》(51)、《獵手的呼喚》(6)、《使者 第二部》(6) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — | speaker 有三種寫法；Dogun 的 Chieftain 卻譯「酋長」，兩族用詞不同 |
| Cerid | Cerid | Corkus 的天才工程師，工廠與 Antikythera 的創造者 | 淪陷工廠 | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | — | 防火牆「歡迎，Cerid」 |
| Antikythera | Antikythera 超級電腦（名牌「Antikythera 超級電腦核心」） | 工廠的電魔法超級電腦，判定人類才是最大的威脅 | 《獵手的呼喚》(10) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 它 | 照指令行事，不是故障；用備用核心活到《獵手的呼喚》 |
| Wynnston | Wynnston | 淪陷工廠的「假首領」小狗，其實是 Cerid 的機甲 | 淪陷工廠 | [08](08-dungeons-and-raids.md) | 牠 | 名字來自前管理員 Marion 的柯基 |
| Sybil／Dwice | Sybil／Dwice | 《羽毛紛飛 第二部》的人物 | 《羽毛紛飛 第二部》(27) | [04](04-ocean-corkus-heights-sky.md) | — | — |

### 2.10 Molten Heights 與 Dogun

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Axelus | Axelus | 矮人王子，揭穿矮人的歷史謊言、創立聯盟；被 Paku 殺死 | 《山中來客》(47)、《矮人與 Doguns 第二部》(44)、《矮人與 Doguns 第一部》(40)、《矮人與 Doguns 第四部》(23) | [02](02-gavel-light-and-decay.md)、[04](04-ocean-corkus-heights-sky.md) | 他 | 《山中來客》（Gavel）裡那個不說身分的矮人就是他，是《矮人與 Doguns》的前傳；Royal Rumble 是王子說的雙關 |
| Draani 八世（King Draani） | Draani 國王 | Rodoroc 國王；明知真相仍維持謊言，兒子死後終結戰爭 | 《臨界點》(81)、《矮人與 Doguns 第四部》(14)、《矮人與 Doguns 第二部》(10) | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md) | 他 | speaker 另有 King of Rodoroc；「他叫我 Draani」回應「你不再是我的兒子」；戰時國王 Draani Thunderwill 是另一位 |
| Korzim | Korzim | Paku 之子，被放逐；聯盟成員 | 《矮人與 Doguns 第三部》(31)、《矮人與 Doguns 第一部》(9)、《臨界點》(9)、《矮人與 Doguns 第四部》(7) | [04](04-ocean-corkus-heights-sky.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | — |
| Paku | 酋長 Paku／Dogun 酋長 | Dogun 族長，召喚 Garaheth、殺死 Axelus；後與矮人立約 | 《臨界點》(16)、《矮人與 Doguns 第二部》(12)、《矮人與 Doguns 第四部》(8)、《矮人與 Doguns 第三部》(1) | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md) | 他 | 語料作「酋長」，本書第三、四章寫「族長」，兩者並存（見文末）；4000 BP 被造出 |
| Edula／Sanba | Edula／Sanba | Maex 的鐵匠與附魔師夫妻 | 《矮人與 Doguns 第三部》(37) | [04](04-ocean-corkus-heights-sky.md) | — | — |
| Ineos | Ineos | 逃離 Heights、被 Corkus 收留的 Dogun 學者 | 《臨界點》(41) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md) | 他 | Dogun 說話一字一頓用「……」 |
| Astal／Desint／Iscor | Astal 長老／Desint 長老／Iscor 長老 | Dogun 長老，被封入真冰七百年 | 《臨界點》(23) | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md) | — | 名牌在 label.json |
| Garaheth | Garaheth | Dogun 的神（火）；300 AP 被 Osseus 所殺 | 《矮人與 Doguns》 | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[09](09-item-lore-index.md) | — | 第三部 #057 原文拼成 Garahath，譯文照正確拼法 |
| Osseus | Osseus | 斬殺 Garaheth 的矮人 | （年表）、物品 Ignis | [04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | 他 | — |
| Algard | Algard 將軍 | 下令把投降的 Doguns 澆成石頭的矮人將軍 | 探索點〈開創者之墓〉 | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md) | 他 | 墓誌銘「無論敵友皆加以保護」是反諷 |
| Ockar | Ockar | 戰時的「無影之刃」 | 探索點 | [04](04-ocean-corkus-heights-sky.md) | — | — |
| Marock／Plario | Marock／Plario | 《矮人與 Doguns 第一部》的人物 | 《矮人與 Doguns 第一部》(24) | — | — | — |
| Burtan | Burtan 王 | 統治 Thanos 的矮人王 | （地區說明） | [03](03-gavel-gylia-and-canyon.md) | 他 | — |

### 2.11 Sky Islands 與虛空

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Farcor | Farcor | 離開 Gavel 的古龍；講述碎裂前的 Ahms，預言災難再臨 | 探索點〈雲端之上〉 | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 峽谷巨像飾品的「智者」、Quetzalcoatl 的「巨蛇」可能指他（推測） |
| Ynnos | Ynnos 科學家 | Ahmsord 的科學家，想讓浮島復原，害整隊人死在虛空 | 《一千米之下》(22) | [04](04-ocean-corkus-heights-sky.md) | 他 | 名字致敬《波特萊爾大遇險》的 Sonny |
| Celuuse／Rontaid | Celuuse／Rontaid | 碎裂的倖存者（虛空村民） | 《一千米之下》(24) | [04](04-ocean-corkus-heights-sky.md) | — | 拉長音「不——知道」是語氣，要保留 |
| Captain Olof | Olof 船長 | 《一千米之下》的人物 | 《一千米之下》(24) | — | — | npc.json 沒有這條名牌 |
| Arriah | Arriah 將軍 | 天空劫掠者將軍，573 AP 寫信給第 356 代女王 | 祕密故事〈天空的統治者〉 | [04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md) | 她 | — |
| Brie／Frank／Kale | Brie／Frank／Kale | 《禍從口入》的人物 | 《禍從口入》(69) | [04](04-ocean-corkus-heights-sky.md) | — | — |

### 2.12 Silent Expanse

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Lutho 保有靈魂的居民 | — | Lutho 最後一個沒有屈服的礦工，帶玩家經歷靈魂分離 | 《不歸之地》 | [05](05-silent-expanse-and-dern.md) | — | speaker 多半是 Lutho Citizen（Lutho 居民） |
| Player? | Player? | 玩家被分出來的另一半自己 | 《不歸之地》(22) | [05](05-silent-expanse-and-dern.md) | — | speaker 照留 Player?；另有 speaker「Player」用在別的任務 |
| 執念者（Obsessor） | 執念者 | Silent Expanse 的怪物，同化 Lucio | 《彼界之旅》(26) | [05](05-silent-expanse-and-dern.md) | 它 | — |
| 至大者（Greater One） | 至大者 | 《獵手的呼喚》裡的存在 | 《獵手的呼喚》 | [05](05-silent-expanse-and-dern.md) | — | label 已統一「衰弱的至大者」 |
| Grian | Grian | 主要劇情開發者（現實人物），稱 Dern 為「遙遠的黑暗之地」 | — | [05](05-silent-expanse-and-dern.md) | 他 | 不是遊戲角色 |

### 2.13 Fruma：王室與統領

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Sovereign Two | Two 統領 | 「第二位」、諜報總管（Spymaster）；用鏡子傳令、預知「可能性的碎片」；八百年前就在替女王做事 | 《希望的合奏》(52)、《絕望的序曲》(48)、《神化》(18) | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | — | 名字就是 Two，照留，不譯「二號」；稱 Tasim「同為後裔」 |
| Zhiraok Nasin | Zhiraok 統領 | Auburn 統領，「永動之風」；祝福讓他能復活；最後抗命 | 《變革迴響》(35)、《神化》(28)、《一葉知秋》(25)、《詛咒之人》(25)、《希望的合奏》(18) 等 10 個 | [03](03-gavel-gylia-and-canyon.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 數學口癖要保留（向量、收斂、因式分解）；Who the helix＝Who the hell |
| Majin Thalas | Majin 統領 | Highlands 統領、軍隊元帥，「晨星」；被玩家擊殺 | 《絕望的序曲》(60)、《女王的新兵》(26)、《神化》(20)、《希望的合奏》(16)、《強者生存》(9) | [06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | — |
| Soosu | Soosu 統領 | 工業區統領，農家出身的天才，帶來工業紀元；被罰「保有祝福、無限期任職」 | 《巧思的代價》(38)、《神化》(20)、《變革迴響》(10)、《希望的合奏》(10)、《鋼鐵同心》(2) | [06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 她 | 日記 NA130「事情發生了」＝飛升；speaker 另有一條 ??? (Sovereign Soosu) |
| Gikyo | Gikyo 統領 | Mistwoods 統領兼巡林官，換形者；虐殺 Mora，被 Sui 殺死 | 《真面目》(17) | [06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他們（別人稱 they）／它（自稱 it），已定案 10-23 | 叫其他統領用轄區名；巡林官＝Ranger（已定案）；《隱於迷霧》裡「Rurie」那段是 Gikyo 假扮的 |
| Oru | Oru 統領 | 最早的統領之一；Deraj（重力方尖碑）的典獄長 | （年表、《真面目》） | [06](06-fruma.md) | — | 典獄長日誌語料沒收 |
| Xima Thalas | Xima | Auburn 前統領，750 AP 遇刺，女王因此開閘淹 Auburn | （年表） | [06](06-fruma.md) | — | — |
| Gitephe Nasin | Gitephe Nasin | 最早的統領之一，Nasin 家；Highlands 的 Lake Gitephe 疑以其為名（推測） | （年表） | [06](06-fruma.md) | — | — |
| Caetal | Caetal | 最早的統領之一，帶頭燒死第一個換形者 Ophir | 探索點〈初代 Wicker 的火花〉 | [06](06-fruma.md) | — | — |
| Amastli | Amastli | 農業區前統領，880 AP 失蹤 | （年表） | [06](06-fruma.md) | — | — |
| Rieke | Rieke | Highlands 前任統領，會認真回覆市民建議 | 探索點〈屬於人民的統領〉 | [06](06-fruma.md) | — | — |
| Hyhet | Hyhet | 王室憲兵指揮官；女王倒下後信仰崩潰、出走 | 祭壇 Deserter's Refuge | [08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | — | — |
| 審訊官（Inquisitor） | 審訊官 | 王室的審訊官，稱 Syndra「母獅」 | 《遺失的那一塊》(23)、《尋回過往》(11) | [06](06-fruma.md) | — | — |
| 車夫（the Driver） | 車隊駕駛（speaker）／車夫（本書） | 新兵篷車的車夫，把一代代受詛者載去 Wynn | 《歸鄉之旅》(17)、《國王的新兵》(2) | [06](06-fruma.md) | 他 | npc.json 作「車隊駕駛」，本書第六章作「車夫」；「每次撞同一顆石頭」是重複流放路線的伏筆 |
| Tefris Ethprilash | Tefris Ethprilash | Fruma 的前獨裁者，石板〈論階序〉（8 BP）的作者 | 探索點 Architect 神龕 | [09](09-item-lore-index.md) | 他 | — |
| Prathash | Prathash | 開門前一年站在日後 Aelumia 台地上說話的女子 | 物品 Halcyon 神龕 | [09](09-item-lore-index.md) | 她 | — |

### 2.14 Fruma：風中之葉、鋼羽、樹冠與綻放之刃

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Syndra | Syndra | 風中之葉首領，前羽翼、憲兵，代號「母獅」；刺殺女王失敗被處決 | 《變革迴響》(120)、《詛咒之人》(106)、《嶄新的開始》(59)、《一葉知秋》(49)、《鋼鐵同心》(33) 等 7 個 | [06](06-fruma.md) | 她 | 《遺失的那一塊》裡 Elise 回憶的人名被換成 {u}，speaker 是亂碼（見文末） |
| Mora | Mora | Syndra 的副手、Sui 的哥哥；被 Gikyo 虐殺 | 《變革迴響》(40)、《詛咒之人》(37)、《一葉知秋》(33)、《巧思的代價》(31)、《鋼鐵同心》(23) 等 7 個 | [06](06-fruma.md) | 他 | — |
| Sui | Sui | 醫護兼廚師，天生的水系受詛者，「天生的織法者」 | 《絕望的序曲》(156)、《隱於迷霧》(119)、《真面目》(114)、《希望的合奏》(80)、《神化》(79) 等 11 個 | [06](06-fruma.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 她 | 說話結巴（um、I- I）要保留 |
| Rex | Rex | 神射手，Highlands 貴族 Athassa 家的逃家女孩；擬定弒神行動，失去一臂 | 《巧思的代價》(175)、《希望的合奏》(99)、《變革迴響》(54)、《詛咒之人》(54)、《絕望的序曲》(40) 等 10 個 | [06](06-fruma.md) | 她 | 哥哥是「無垠櫻桃」 |
| Zeph | Zeph | 情報員，口吃；第一個學會魔法（風）；與 Yuman 相戀 | 《詛咒之人》(135)、《鋼鐵同心》(83)、《嶄新的開始》(66)、《一葉知秋》(58)、《神化》(50) 等 9 個 | [06](06-fruma.md) | 他（語料） | — |
| Kaio | Kaio | 風中之葉的老人，冷眼預言悲劇 | 《嶄新的開始》(26)、《希望的合奏》(14)、《歸鄉之旅》(7)、《詛咒之人》(4)、《一葉知秋》(1) | [06](06-fruma.md) | — | — |
| Quill | Quill | 鋼羽首領，設計隔開 Roughworks 的城牆 | 《鋼鐵同心》(55)、《變革迴響》(47)、《希望的合奏》(22)、《巧思的代價》(18)、《神化》(12) | [06](06-fruma.md) | 他（語料） | — |
| Yulie／Yusanu／Yuman | Yulie／Yusanu／Yuman | 鋼羽三兄弟：雙面偵探／Soosu 的助手、破解共鳴織機／吉他手 | 《變革迴響》(159)、《巧思的代價》(157)、《鋼鐵同心》(148)、《希望的合奏》(108)、《真面目》(55) 等 7 個 | [06](06-fruma.md) | 他 | — |
| Elise | Elise | Roughworks 的退休女工，Syndra 的戀人，被抹去記憶 | 《遺失的那一塊》(225) | [06](06-fruma.md) | 她 | — |
| Azael | Azael | 樹冠首領，換形者；與 Gikyo 同批 | 《希望的合奏》(25)、《真面目》(17)、《隱於迷霧》(14)、《神化》(9) | [06](06-fruma.md)、[07](07-secrets.md) | 他 | — |
| Rurie／Fuhue／Cileon | Rurie／Fuhue／Cileon | 樹冠的梟、鹿、狐三族隊長 | 《真面目》(115)、《希望的合奏》(98)、《隱於迷霧》(89)、《神化》(66) | [06](06-fruma.md) | Rurie 他、Fuhue 她（語料） | 「從更高之地逃出來的蠢貨」是 Gikyo 在模仿 Rurie |
| Jiro | Jiro | 鋼羽的聯絡人；《變革迴響》前「最近有點反常」 | 《鋼鐵同心》(9)、《詛咒之人》(5)、《變革迴響》(3)、《巧思的代價》(2) | [06](06-fruma.md) | 他 | 遊戲沒明說內鬼是誰（推測為他） |
| Aihan | Aihan | Roughworks 的女工，沒達到配額被驅逐，被 Zeph 救下 | 《鋼鐵同心》(37) | [06](06-fruma.md) | 她 | 「我不知道受詛者也可以是好人」 |
| 泣血薔薇（Bleeding Rose） | 泣血薔薇 | 綻放之刃成員，唯一想行動的人 | 《希望的合奏》(24)、《神化》(10)、《絕望的序曲》(9) | [06](06-fruma.md) | — | 綻放之刃成員都用花名代號，譯文翻成中文；speaker 欄位留英文 |
| 暮色花園／緋紅匕首／荊棘之刃／暴烈石楠／枯萎山茶 | 暮色花園／緋紅匕首／荊棘之刃／暴烈石楠／枯萎山茶 | 綻放之刃的其他成員（Dusk Garden、Crimson Dagger、Thorn Blade、Violent Heather、Wilted Camellia） | 《絕望的序曲》(25)、《希望的合奏》(19)、《神化》(8) | [06](06-fruma.md) | — | 同上；Sui 混進去時化名「水草」（speaker 寫成帶引號的 "Water Grass"），玩家化名「影叢」（Shadow Bush） |
| Arnol | Arnol | 《強者生存》被羞辱的貴族子弟，後來加入樹冠 | 《強者生存》(110)、《希望的合奏》(17) | [06](06-fruma.md) | 他 | — |
| Adder | Adder | 羽翼部隊的新兵，把投降的同袍打成殘廢 | 《強者生存》(34) | [06](06-fruma.md) | 他 | — |
| Commander Ferreo | Ferreo 指揮官 | Fort Tericen 訓練營的指揮官，默許霸凌、流放 Effal | 《強者生存》(71) | [06](06-fruma.md)、[09](09-item-lore-index.md) | 他 | — |
| Recruiter Effal | Effal 招募官 | Fruma 的招募官；放走玩家後被以叛國罪流放 Wynn、抹去記憶 | 《強者生存》(53) | [06](06-fruma.md) | 他 | 職稱「招募官」放名字後面；在 Wynn 遇到時他已經不記得 |
| Recruiter Olissy | Olissy 招募官 | Fort Tericen 的招募官 | 《強者生存》(4) | — | 她 | 遊戲在 speaker 與一句台詞拼成 Ollisy，譯文照正確拼法 Olissy（已裁示 10-04）；speaker 欄位不要動 |
| Levi | Levi | Constables 部隊的將軍，在列車上被毒死 | 《脫軌》 | [06](06-fruma.md) | 他 | — |
| 脫軌的乘客（Demma、Maddison、Maury、Malic、Harvin、Telati、Flinn、Cemor） | （留英文） | 《脫軌》列車謀殺案的嫌疑人與相關人 | 《脫軌》(692) | [06](06-fruma.md) | — | 這八個人第六章沒有逐一介紹 |
| Burick／Cerisa／Arris | Burick／Cerisa／Arris | 《慶典成煙》裡被扣押的三名嫌疑人：孤獨的拾荒者／冷靜的逃兵／被冤枉偷茶的人 | 《慶典成煙》(249) | [06](06-fruma.md) | — | — |
| Abramod | Abramod | 《羈絆成灰》失去徽牌的老人，其實自己就是換形者 | 《羈絆成灰》(77) | [06](06-fruma.md) | 他 | 前半段的「弟弟被冒名」是他的謊話 |
| Fichu | Fichu | 替 Soosu 做禮物的工匠，禮物被浣熊叼進下水道 | 《穿越管道》(162) | [06](06-fruma.md) | — | — |
| Lanu／Linton／Mel | Lanu／Linton／Mel | Wynn 篇的老面孔（《傭兵》《腐敗之叛》），在《女王的新兵》與玩家同行回 Fruma | 《女王的新兵》(85)、《傭兵》(35)、《腐敗之叛》(20) | [06](06-fruma.md) | — | Lanu 那句「那股魔法……為什麼感覺很熟悉？」沒有下文，可能指腐敗（推測） |
| Ophir | Ophir | 第一個被燒死的換形者 | 探索點〈初代 Wicker 的火花〉 | [06](06-fruma.md) | — | — |
| Xanthos | Xanthos | 皇家紀元 200 年反抗軍的首領，過場裡被火球與粉紅尖刺貫穿 | 探索點〈受詛者的遺骸〉 | [06](06-fruma.md) | — | — |
| Fia | Fia | Auburn 苦艾酒唯一的釀酒人 | 《呱鳴怪談》(5) | [06](06-fruma.md)、[07](07-secrets.md) | — | 家傳寶是 50 KA 的瓶蓋 |
| Croaquin | Croaquin | Frog Bog 的人形巨蛙 | 《呱鳴怪談》(9) | [06](06-fruma.md)、[07](07-secrets.md) | 他 | 讀音「像 Joaquin，但是 CROAK」；crrrroakin'＝「他呱～～的」全文統一 |

### 2.15 地城與討伐戰首領

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| 凋零首（Witherhead） | 凋零首 | 被腐敗喚回的 Olm 女子；破敗下水道首領；wiki 說是 Bob 的生母 | 破敗下水道 | — | 她 | 物品已改「凋零首」（#1370）；頭銜「{p} 的黑死病」 |
| Arakadicus | Arakadicus | 蜘蛛女王，Nivla 蛛群的源頭 | 蟲蝕深坑、《蛛群登巔》 | [08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 她 | — |
| Charon | Charon | 不死軍閥，「靈魂看守者」 | 冥界墓穴、《深入幽暗》 | [01](01-wynn.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | 不寫「卡戎」（已改）；2.0.1 前頭銜是 Undead Ferryman |
| General Graken | Graken 將軍 | Nemract 民兵將領 | 《深入幽暗》(23) | [01](01-wynn.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 英式俚語（語料「老兄」） |
| Haros | Haros | 被逐出軍團的擺渡人 | 物品 Haros' Oar、Rotten Passage | [08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 他 | Banished Legion 與 Forsaken Legion（遭棄軍團）是兩個詞 |
| Hyhet（見 2.13） | Hyhet | 祭壇首領 | — | [08](08-dungeons-and-raids.md) | — | — |
| Grootslang 幼龍 | Grootslang 幼龍 | 被 Dern 蟎逼瘋的幼體 | 討伐戰 Grootslang 之巢 | [08](08-dungeons-and-raids.md) | 它 | — |
| 峽谷巨像（Canyon Colossus） | 峽谷巨像 | Gavel 的守護者，失控後成了毀滅者 | 討伐戰峽谷巨像、《臨界點》 | — | 它 | 1.20 前叫 Lost Colossus；暱稱 Archlad；討伐戰結局是安撫不是摧毀 |
| Ahms 巨像 | Ahms 巨像 | 0 AP 失控、造成碎裂的另一尊巨像 | 探索點 | [04](04-ocean-corkus-heights-sky.md) | 它 | — |

### 2.16 只在物品 lore 與探索點出現的人物

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Ensa | Ensa | 學者祭司，故鄉被圍時做祈禱墜飾 | 物品 Ensa's Faith／Absolution／Failure | [09](09-item-lore-index.md) | — | wiki 沒有人物頁 |
| Rycar | Rycar | 把酒瓶口做的戒指丟進 Gylia 湖的人 | 物品 Rycar's Elation 等 | [09](09-item-lore-index.md) | — | — |
| Ohms | Ohms | Gale Nasin 的愛人；他的魔像被黑暗力量反噬 | 物品 Ohms' Wish／Rage、Gale's Sight | [09](09-item-lore-index.md) | — | — |
| Lacrona | Lacrona | Detlas 裁縫，被 Nivla 蜘蛛吃掉 | 物品 Web Plate | [09](09-item-lore-index.md) | — | — |
| Pyeka | Pyeka | Alekin 草藥師，懷疑 Yahya 的病因 | 物品 Pyrexia | [09](09-item-lore-index.md) | — | — |
| Sayleros 與他的兄弟 | Sayleros | 墓碑上沒有正式名字的兄弟 | 《歸來的兄長》(13) | [09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 他 | 物品名「Sayleros 兄弟」有歧義（是 Sayleros 的兄弟） |

### 2.17 Lootrun、世界事件、角色任務與節慶

| 英文名 | 繁中寫法 | 身分與陣營 | 出現的任務 | 章節 | 代名詞與性別 | 易錯處 |
|---|---|---|---|---|---|---|
| Lootrun 主管／Lootrun 大師（Lootrun Chief／Lootrun Master） | Lootrun 主管／Lootrun 大師 | Silverbull Lootrun 部門的 NPC：主管在總部替冒險者入會，大師在各營地開跑 | Lootrun | [10](10-lootruns-events-and-mini-quests.md) | 他（主管，語料） | 部門階級的 Master（大師）與這位 NPC 同字，第十章認為可接受 |
| Astraulus（Astralus） | Astralus | Sky Islands 塔上的法師，搶月鹽、把玩家射上太空 | 《禍從口入》(5) | [10](10-lootruns-events-and-mini-quests.md) | — | 同一人三種拼法：wiki 與第十章 Astraulus、speaker Astralus、《禍從口入》原文 Astralaus' Astral House；語料譯文統一寫 Astralus |
| 典範者（Exemplar） | 典範者 | 虛空裡的冒牌神，靠信徒獻上的身體長大 | Lootrun 挑戰 False Respite | [10](10-lootruns-events-and-mini-quests.md) | 它 | — |
| 湮滅（Annihilation） | 湮滅（首領名）／毀滅前奏（世界事件名） | 戰爭領域的惡魔；900、985、1000 AP 三度破門 | 世界事件毀滅前奏 | [01](01-wynn.md)、[05](05-silent-expanse-and-dern.md)、[10](10-lootruns-events-and-mini-quests.md) | 它 | 事件名與首領名不同是刻意的；大寫 Annihilation 指首領時譯「湮滅」，不要寫「毀滅」 |
| 薄暮 Kantyr | 薄暮 Kantyr | Lootrun 時 Olm 古城偶爾來助陣的存在（名牌） | Silent Expanse 遠征 | [10](10-lootruns-events-and-mini-quests.md) | — | — |
| Afelis | Afelis | 角色任務的新兵，從 Ragni 走到 Gelibord，為保護孩子而死 | 角色任務（語料只收一句，誤放在《廚師助理》） | [10](10-lootruns-events-and-mini-quests.md) | 非二元，原文用 they；語料避開代名詞，非用不可時寫「他們」 | 墜盒 lore 的「A+E」中的 E 是誰遊戲沒說 |
| Angie | Angie | Hyloch 出身、Aelumia 大學的畫家；發現「農業區」是謊言 | 角色任務（Fruma） | [10](10-lootruns-events-and-mini-quests.md) | 她 | 「農業區」是 Fruma 的宣傳用語，工業區的舊名 |
| Aster | Aster | 說書人，四兄弟的長子，能把故事變成現實 | 英雄節、營火節 | [10](10-lootruns-events-and-mini-quests.md) | 他 | — |
| 營火看守人（Bonfire Keeper） | 營火看守人 | 四兄弟之一，悲劇的起因，唯一記得一切的人 | 營火節、Talespun Citadel | [10](10-lootruns-events-and-mini-quests.md) | — | — |
| 使者（The Envoy） | 使者 | 替彼界工作的「聲音」 | 靈魂節 | [10](10-lootruns-events-and-mini-quests.md) | — | 撞任務名《使者》（The Envoy Part I／II，Corkus）；第十章提議必要時作「冥使」，未定 |
| 旅人（The Traveler） | 旅人 | 記憶被霧吞噬的「流浪者」 | 暴雪節 | [10](10-lootruns-events-and-mini-quests.md) | — | Aster 稱他 the Wanderer（流浪者），與 Garvan 的「流浪者」同詞不同人；懸賞靈魂裡另有 The Wanderer |
| 嚮導（The Guide） | 嚮導 | 使者的同伴，曾是逃魂 | 靈魂節 | [10](10-lootruns-events-and-mini-quests.md) | — | — |

## 三、地名誌與組織、種族

欄位：**英文**｜**繁中寫法**（地區名照慣例留英文，照「發現地區：」那行；本書自己的寫法另外標明）｜**在哪、是什麼**｜**章節**｜**易錯處**。任務台詞裡的地名多半是佔位符 `{p}`，譯文看不到地名本身，所以語料裡「留英文」的證據主要來自探索點與名牌。

### 3.1 大區（行省）與世界結構

| 英文 | 繁中寫法 | 在哪、是什麼 | 章節 | 易錯處 |
|---|---|---|---|---|
| Wynn（Wynn Province） | Wynn（留英文） | 新兵抵達的行省；腐敗傳送門所在 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 「Wynn 的英雄」指 Bob 或玩家；遊戲名 Wynncraft 不要混 |
| Gavel | Gavel（留英文） | 海洋另一側的行省，精靈、村民、矮人等多族並存 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 冒牌將軍把它叫成「碎石」（Gravel，繁簡已定案在地化） |
| Fruma | Fruma（留英文） | 女王統治的王國；新兵的出身地 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 舊 Fruma（門開前的文明）與現在的王國要分清 |
| Ocean | 海洋 | Wynn 與 Gavel 之間的海域與群島 | [04](04-ocean-corkus-heights-sky.md) | 任務目標的 [Ocean Treasure] 譯「海洋寶藏」 |
| Corkus | Corkus（留英文） | 850 AP 由 Fruma 叛逃者在 Relos 島西南岸建立的電魔法國家 | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | Corkian＝Corkus 人，照用 |
| Molten Heights | Molten Heights（留英文） | Gavel 的火山高地，矮人與 Dogun 的地盤 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Sky Islands | Sky Islands（留英文） | 0 AP 碎裂後浮在空中的群島，下方是虛空 | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Silent Expanse | Silent Expanse（留英文） | 被 Dern 扭曲的荒原，Lutho 與詭異瞭望台所在 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 不寫「寂靜之境」；物品 lore 的 The Expanse 也指這裡，不是「曠野」 |
| 物質界（Physical Plane） | 物質界 | 我們所在的世界 | [05](05-silent-expanse-and-dern.md) | 《尋光》台詞另有「物質層」 |
| 影響層（Plane of Influence） | 影響層 | 物質界以外的層面 | [01](01-wynn.md)、[05](05-silent-expanse-and-dern.md) | — |
| 影響領域（Realms of Influence） | 影響領域 | 光、黑暗、戰爭三個領域的總稱，各有一道門 | [05](05-silent-expanse-and-dern.md) | — |
| 光之領域（Realm of Light） | 光之領域 | Orphion 的領域，門在 Gavel 的巨樹下 | [02](02-gavel-light-and-decay.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md) | 也是任務線名「光之領域 第一部～第五部」 |
| 黑暗領域／Dern（Realm of Darkness／Realm of Dern） | 黑暗領域／Dern | Saphanis 的領域，門在 Silent Expanse 盡頭 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 同一處兩個名字；與腐敗分開翻（「不是腐敗——寫得非常清楚」）；Dernel Jungle 的 Dern 是否同義不明 |
| 戰爭領域（Realm of War） | 戰爭領域 | Anathema 的領域，門是 Wynn 的腐敗傳送門 | [01](01-wynn.md)、[05](05-silent-expanse-and-dern.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| 腐敗傳送門（Corruption Portal） | 腐敗傳送門 | 0 AP 礦工在 Wynn 挖出的門 | [01](01-wynn.md) | 不寫「腐敗之門」 |
| 虛空（the Void） | 虛空 | Sky Islands 下方、碎裂造成的空洞 | [04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md) | 虛空洞（Void Hole）是無名者造的另一種東西 |

### 3.2 Wynn 行省

| 英文 | 繁中寫法 | 在哪、是什麼 | 章節 | 易錯處 |
|---|---|---|---|---|
| Ragni | Ragni | Wynn 最早的城鎮；門開前是 Fruma 殖民地 Sikshahrath／Ragshniv | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[06](06-fruma.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Detlas | Detlas | Wynn 的軍事重鎮，新兵的集結地 | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Wynn Plains | Wynn Plains | Ragni 與 Detlas 一帶的平原 | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[07](07-secrets.md) | — |
| Nivla Woods | Nivla Woods | Wynn 的森林，882 AP 被 Bak'al 大軍焚燒；蛛群出沒 | [01](01-wynn.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Almuj | Almuj | 沙漠的城市 | [01](01-wynn.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Mesa／Upper Mesa | Mesa／Upper Mesa（解鎖訊息四條作「方山高地」、一條留 Upper Mesa；怪物「方山亡命徒」） | 沙漠北方的台地 | [01](01-wynn.md) | 同一組訊息兩種寫法（見文末） |
| Rymek | Rymek | 沙漠的城鎮，Creden Tibus 的首府 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 名字取自前建築師 Mekyr |
| Nemract | Nemract | Wynn 西岸的港城；古 Nemract 在 0 AP 陷落 | [01](01-wynn.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Troms | Troms | 叢林中的王國 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Dernel Jungle | Dernel Jungle | Wynn 南方的叢林；Olm 逃難後發瘋的地方 | [01](01-wynn.md)、[05](05-silent-expanse-and-dern.md) | — |
| Nesaak | Nesaak | Wynn 北方的冰原，被 Theorick 凍結 | [01](01-wynn.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Lusuco | Lusuco | Wynn 的城鎮，祕密圖書館裡藏著《高貴的 Twain 家》 | [01](01-wynn.md)、[07](07-secrets.md) | — |
| Maltic | Maltic | 900 AP 村民建立的城鎮 | [01](01-wynn.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Elkurn | Elkurn | Wynn 的村落；舊版 Garoth 的出身 | [01](01-wynn.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Time Valley | Time Valley | Olm 建造、帶時間魔法的山谷 | [01](01-wynn.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | Temple of Time／Gate of Time 語料保留原文 |
| Tempo Town | Tempo Town | Time Valley 裡的小鎮 | [01](01-wynn.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | — |
| Pigman Ravine | Pigman Ravine | Tempo Town 附近的峽谷 | [01](01-wynn.md) | — |
| Great Bridge | Great Bridge | Wynn 的大橋；Rickeo Twain 死後在這裡作祟 | [01](01-wynn.md) | 名牌「大橋剋星」 |
| Abandoned Mines | Abandoned Mines | Wynn Plains 的廢礦，接到 Dern | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md) | — |
| Gateway Island | Gateway Island | 海上的小島，Bob 從這裡消失 | [01](01-wynn.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md) | — |

### 3.3 Gavel

| 英文 | 繁中寫法 | 在哪、是什麼 | 章節 | 易錯處 |
|---|---|---|---|---|
| Llevigar | Llevigar | Gavel 西岸的大城，有大學與祕密圖書館 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Gelibord | Gelibord | Gavel 北方的城鎮 | [02](02-gavel-light-and-decay.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Lexdale | Lexdale | Gavel 北方的城鎮與監獄 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[09](09-item-lore-index.md) | 任務名「Lexdale 女巫審判」 |
| Olux／Olux Swamp | Olux | 沼澤與城鎮，魔像研究的所在 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Kander Forest | Kander Forest | 黯蝕最深的森林 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Dark Forest | Dark Forest（本書偶作「黑暗森林」） | Gavel 北方的森林，Dullahan 的地盤 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[07](07-secrets.md) | — |
| Light Forest | Light Forest（本書偶作「光之森林」） | Gavel 南方的森林 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Aldorei | Aldorei | 精靈的家鄉（谷地與城鎮） | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Efilim | Efilim | 被放逐的精靈建立的鎮 | [02](02-gavel-light-and-decay.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | 名字是建築師 im4life1 的變位字 |
| Taproot | Taproot | 地名；任務名「斬草除根」 | [02](02-gavel-light-and-decay.md) | 地名留英文、任務名意譯 |
| Lazarus Pit | Lazarus 之坑 | 能讓屍體復活的坑 | — | 任務名同為「Lazarus 之坑」 |
| Centerworld | Centerworld | 獸人共主 Arrai-Veretel 的堡壘 | [02](02-gavel-light-and-decay.md) | — |
| Kitrios Barracks | Kitrios Barracks | Gavel 軍隊的軍營，Lecade 將軍指揮；冒牌將軍的舞台 | [03](03-gavel-gylia-and-canyon.md) | — |
| Cinfras | Cinfras | Gavel 中部的首都，Guild Hall 原是王宮 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 《Haven 的往昔》裡的 Cinfras 是 NPC 名，不是城市；名字是 Francis 的變位字 |
| Letvus | Letvus | 飛船基地 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[09](09-item-lore-index.md) | 有一條原文拼成 Levtus，譯文照 Letvus；名字是 Selvut 的變位字 |
| Gylia／Lake Gylia | Gylia | Gavel 中部的平原與湖，隕石墜落處 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[06](06-fruma.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | 遊戲有一條拼成 Gyila，譯文照 Gylia（已裁示） |
| Woc | Woc | 流亡者的聚落 | [03](03-gavel-gylia-and-canyon.md) | — |
| Canyon of the Lost | Canyon of the Lost（本書作「失落者峽谷」） | Gavel 東部的大峽谷 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | 語料多半是地名佔位符 {p}，譯文看不到 |
| Thesead | Thesead | 峽谷的煤礦城 | [03](03-gavel-gylia-and-canyon.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Thanos | Thanos | 峽谷外唯一的矮人城市 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | 名字是建築師 Nohats 的變位字；任務名「Thanos 儲藏庫」 |
| Eltom | Eltom | 非法的人類城鎮 | [03](03-gavel-gylia-and-canyon.md) | — |
| Bantisu | Bantisu | 峽谷的僧侶神殿（Bantisu 神殿、Bantisu Air Temple） | [03](03-gavel-gylia-and-canyon.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| 傳說神殿（Temple of the Legends） | 傳說神殿 | 峽谷的神殿 | [03](03-gavel-gylia-and-canyon.md) | — |
| Ahmsord | Ahmsord | Sky Islands 的主要城鎮，曾是天空劫掠者主基地 | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | — |
| Raiders' Port | Raiders' Port | 天空劫掠者退守的港口 | [04](04-ocean-corkus-heights-sky.md) | — |
| 蜂巢（the Hive） | 蜂巢（已定案 10-24；本工作樹的語料還有 24 句留 Hive） | Qira 的據點 | [09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 物品 lore 的「蜂巢女主人」已是中文 |

### 3.4 海洋、Corkus、Molten Heights 與 Silent Expanse

| 英文 | 繁中寫法 | 在哪、是什麼 | 章節 | 易錯處 |
|---|---|---|---|---|
| Pirate Cove | Pirate Cove（探索點與「發現地區」）／海盜灣（任務目標與台詞） | 海盜的島 | [04](04-ocean-corkus-heights-sky.md) | 同一地點兩種寫法（見文末） |
| Mage Island | Mage Island | 法師議會與 Grook 魔法學院所在 | [04](04-ocean-corkus-heights-sky.md) | — |
| Dead Island | Dead Island | Siwel 詛咒的島 | [04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md) | — |
| Jofash | Jofash | Relend 的港 | [04](04-ocean-corkus-heights-sky.md) | — |
| Selchar | Selchar | 970 AP 建的港島 | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | — |
| Sarnfic | Sarnfic | 被法師議會沉入海底的村子 | [04](04-ocean-corkus-heights-sky.md)、[09](09-item-lore-index.md) | — |
| Relos | Relos | Corkus 所在的島，有祕密圖書館 | [04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md) | — |
| Corkus City | Corkus City | Corkus 的首都 | [04](04-ocean-corkus-heights-sky.md) | — |
| Avos | Avos | Corkus 的原住民族與他們的城 | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 族名與地名同形 |
| Kandon-Beda | Kandon Beda（語料原文沒有連字號；本書第三、四章寫 Kandon-Beda） | 峽谷巨像穩定器所在 | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[10](10-lootruns-events-and-mini-quests.md) | 兩種拼法指同一處 |
| Rodoroc | Rodoroc | 矮人的首都 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Courag | Courag | Dogun 的舊首都，現為廢墟 | [04](04-ocean-corkus-heights-sky.md) | — |
| Maex | Maex | Molten Heights 的城鎮，有黑市 | [04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md) | — |
| Freezing Heights | Freezing Heights | 被冰亞龍凍結的區域 | [04](04-ocean-corkus-heights-sky.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Lutho | Lutho | Silent Expanse 的礦工聚落 | [05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 名字是祖魯語「什麼都沒有」 |
| Ruined Olmic City | Ruined Olmic City | Olm 的舊首都 | [05](05-silent-expanse-and-dern.md) | — |
| 詭異瞭望台（Eldritch Outlook） | 詭異瞭望台 | 眼所在的塔（地城） | [05](05-silent-expanse-and-dern.md)、[08](08-dungeons-and-raids.md) | — |
| Tower of Ascension | Tower of Ascension（探索點名作「登天之塔」） | 冒牌死神所在的塔 | [03](03-gavel-gylia-and-canyon.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | 說明文留原文、標題譯中文 |

### 3.5 Fruma

| 英文 | 繁中寫法 | 在哪、是什麼 | 章節 | 易錯處 |
|---|---|---|---|---|
| Aelumia | Aelumia | Fruma 的首都，皇家大學與王宮所在 | [06](06-fruma.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Auburn Forest | Auburn Forest | 永恆秋天的森林區 | [06](06-fruma.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Espren | Espren | Auburn 的城鎮 | [06](06-fruma.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Roughworks（the Roughworks） | Roughworks | 工業區（舊稱農業區） | [06](06-fruma.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Timasca | Timasca | 工業區的城市 | [06](06-fruma.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Fort Torann | Fort Torann | 建在舊神殿廢墟上的堡壘，Xima 當年坐鎮於此；玩家在此掙脫（「Torann 大屠殺」） | [06](06-fruma.md)、[07](07-secrets.md) | — |
| Mistwoods（the Mistwoods） | Mistwoods | 霧林；換形者與樹冠的地盤 | [06](06-fruma.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Aldwell | Aldwell | Mistwoods 的城鎮，柳枝人祭典 | [06](06-fruma.md)、[07](07-secrets.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Fort Hegea | Fort Hegea | 換形者被送去社會化、受訓的堡壘 | [06](06-fruma.md)、[07](07-secrets.md) | — |
| Highlands（the Highlands） | Highlands | 高地區 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[06](06-fruma.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Hyloch | Hyloch | Highlands 的首府 | [06](06-fruma.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Fort Tericen | Fort Tericen | Fruma 最大的訓練營 | [06](06-fruma.md)、[09](09-item-lore-index.md) | — |
| Citadel（Aelumia Citadel／the Citadel） | Citadel／城塞／堡壘（三種並存） | Aelumia 的高地城區 | [06](06-fruma.md)、[10](10-lootruns-events-and-mini-quests.md) | discovery.json 留 Citadel、皇家鍊金師說明作「城塞」、事件名「堡壘營房」；ja 已定「城塞」 |
| Deraj | Deraj | Wastelands（Penitence District）的監獄，人稱「重力方尖碑」 | [06](06-fruma.md) | — |
| Royal Dam | 皇家水壩（本書；語料未見） | 750 AP 女王開閘淹 Auburn 的水壩 | [06](06-fruma.md) | — |
| 烽火王宮（Wartorn Palace） | 烽火王宮 | Anathema 的討伐戰 | [06](06-fruma.md)、[08](08-dungeons-and-raids.md) | — |

### 3.6 勢力與組織

| 英文 | 繁中寫法 | 在哪、是什麼 | 章節 | 易錯處 |
|---|---|---|---|---|
| Gavel 王室 | Gavel 王室 | 統治 Gavel 千年、750 AP 被推翻的王朝 | [03](03-gavel-gylia-and-canyon.md) | 保皇派（royalists）見《空洞的寧靜》 |
| Gylia Watch | Gylia Watch（台詞）／Gylia 守望隊（名牌） | Gylia 的民兵組織 | [03](03-gavel-gylia-and-canyon.md)、[07](07-secrets.md) | 同一組織兩種寫法（見文末） |
| WynnExcavation | WynnExcavation | 挖掘公司，實為崇拜 Saphanis 的組織 | [01](01-wynn.md)、[02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | Site A～D＝A～D 號遺址（已統一） |
| Creden Tibus | Creden Tibus | 沙漠的盜匪團，守著皇帝之杖 | [01](01-wynn.md)、[07](07-secrets.md)、[09](09-item-lore-index.md) | — |
| 天空劫掠者（Skyraiders） | 天空劫掠者 | Sky Islands 的空中海盜，全員女性 | [04](04-ocean-corkus-heights-sky.md) | 已統一，不寫「空襲者」；任務道具 Skyraider Coin 也是 |
| 法師議會（Mage Council） | 法師議會 | Mage Island 的法師組織 | [04](04-ocean-corkus-heights-sky.md) | — |
| Grook 魔法學院（The Order of the Grook） | Grook 魔法學院 | Mage Island 的學院 | [04](04-ocean-corkus-heights-sky.md) | 已定案，台詞不寫「Grook 修會」 |
| 遭棄軍團（Forsaken Legion） | 遭棄軍團 | 古 Nemract 陷落後被 Charon 收編的守軍 | [08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md) | 已統一（名牌）；Banished Legion 是另一個詞 |
| Sodeta 公會（Sodeta Guild） | Sodeta 公會 | Tisaun 創立、944 AP 在峽谷全滅的公會 | [09](09-item-lore-index.md) | — |
| Silverbull Trading Company | Silverbull | 商會；總部是 Gavel 王室避暑行宮 | [08](08-dungeons-and-raids.md) | — |
| Shadow Spears | Shadow Spears | 盜匪組織 | [01](01-wynn.md) | 名牌「Shadow Spears 首領」 |
| 統領（Sovereigns） | 統領 | Fruma 女王之下的各區首長 | — | 不寫「主權者」 |
| 諜報總管（Spymaster） | 諜報總管 | Two 的職位 | — | — |
| 羽翼部隊（the Wings） | 羽翼部隊 | Fruma 的精銳部隊 | — | 物品 Light of The Wings 的「雙翼」應指這支部隊；in the wings 有雙關 |
| 憲兵（Gendarme） | 憲兵 | Fruma 的王室憲兵 | — | — |
| 警備部隊（Constables Division） | 警備部隊 | Fruma 的治安部隊 | [06](06-fruma.md) | — |
| 風中之葉（Leaves in the Wind） | 風中之葉 | Auburn 的反抗組織 | [06](06-fruma.md) | — |
| 鋼羽（The Steel Feather） | 鋼羽 | Roughworks 的反抗組織 | [06](06-fruma.md) | 地下站的配給票券＝Steel Feather 票券 |
| 樹冠（the Canopy） | 樹冠 | Mistwoods 的反抗組織（梟、狐、鹿三族） | [06](06-fruma.md) | — |
| 綻放之刃（Blossom of Blades） | 綻放之刃 | Highlands 的地下組織，成員用花名代號 | [06](06-fruma.md) | — |
| Kalkalrah 氏族 | Kalkalrah | 只認錢的傭兵氏族 | [06](06-fruma.md) | 就是《慶典成煙》的「安全小組」 |
| Athassa 家／Thalas 家／Nasin 家 | Athassa／Thalas／Nasin | Fruma 的貴族家族 | [06](06-fruma.md) | Nasins' Nadir＝Nasin 家的谷底 |
| 牛神教（Bovemism） | 牛神信徒（Bovemist） | 崇拜聖牛的宗教 | [01](01-wynn.md)、[05](05-silent-expanse-and-dern.md) | Bovemist 統一為「牛神信徒」（已定案 10-24） |

### 3.7 種族與族群

| 英文 | 繁中寫法 | 在哪、是什麼 | 章節 | 易錯處 |
|---|---|---|---|---|
| Olm | Olm | 門開前的古文明種族，會時間與宇宙魔法 | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 形容詞 Olmic 照「Olm 符文」 |
| 精靈（Elves） | 精靈 | Gavel 的長生種族 | — | — |
| 村民（Villagers） | 村民 | Gavel 的長鼻子種族 | [04](04-ocean-corkus-heights-sky.md)、[09](09-item-lore-index.md) | 「大鼻子騙子」是罵村民 |
| 矮人（Dwarves） | 矮人 | Molten Heights 的種族 | [04](04-ocean-corkus-heights-sky.md) | — |
| Dogun（Doguns） | Dogun | 峽谷巨像的石頭造成、Garaheth 的火賦予生命的種族 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 大寫 Dogun（小寫 dogun 已改）；複數照原文 Doguns |
| Corkian | Corkian | Corkus 人 | [01](01-wynn.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md) | — |
| Avo／Avos | Avo／Avos | Corkus 的鳥人原住民 | [03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[05](05-silent-expanse-and-dern.md)、[07](07-secrets.md)、[08](08-dungeons-and-raids.md)、[09](09-item-lore-index.md)、[10](10-lootruns-events-and-mini-quests.md) | 單數 Avo、複數 Avos |
| 獸人與哥布林（Orcs／Goblins／Greenskins） | 獸人／哥布林 | 西 Gavel 的綠皮族群 | [02](02-gavel-light-and-decay.md) | — |
| Gert | Gert | 被隕石魔力變異的村民後代 | [02](02-gavel-light-and-decay.md)、[03](03-gavel-gylia-and-canyon.md)、[04](04-ocean-corkus-heights-sky.md)、[09](09-item-lore-index.md) | 他們不知道自己原是村民 |
| 換形者（Changeling） | 換形者 | Fruma 荒地的變形種族 | [03](03-gavel-gylia-and-canyon.md)、[06](06-fruma.md)、[09](09-item-lore-index.md) | 簡中「易形者」（已定案）；物品 Changeling's Chestplate 舊譯「換生靈」 |
| 受詛者（Cursed Ones） | 受詛者 | Fruma 對天生有魔力者的稱呼 | — | — |
| Wybel | Wybel | 小型生物，對宇宙魔法有親近感 | [09](09-item-lore-index.md) | — |
| 冰亞龍（Ice Drake） | 冰亞龍 | Molten Heights 的龍 | [04](04-ocean-corkus-heights-sky.md) | 已統一；任務道具 [Ice Drake Scales] 留英文 |

### 3.8 Lootrun、世界事件與節慶的地點

| 英文 | 繁中寫法 | 在哪、是什麼 | 章節 | 易錯處 |
|---|---|---|---|---|
| Silverbull Company Headquarters | Silverbull 總部（本書） | Cinfras 東邊，Lootrun 的入口；前身是 Gavel 王室的避暑行宮 | [08](08-dungeons-and-raids.md)、[10](10-lootruns-events-and-mini-quests.md) | — |
| Lootrun 營地（Canyon of the Lost Excursion 等七個） | （營地名見第十章） | 各大區的 Lootrun 起點 | [10](10-lootruns-events-and-mini-quests.md) | 地區名照慣例留英文 |
| Colossus Islet | Colossus Islet | Sky Islands 的 Lootrun 地點，有碎裂哨兵與巨像面容 | [10](10-lootruns-events-and-mini-quests.md) | — |
| Festive Detlas | Festive Detlas | 節慶期間的 Detlas | [10](10-lootruns-events-and-mini-quests.md) | — |
| Talespun Citadel | Talespun 城塞（第十章暫定） | Aster 的故事長出來的城塞 | [10](10-lootruns-events-and-mini-quests.md) | Citadel 的譯法見 3.5 |
| Isles of Fiction | Isles of Fiction | 海上的群島，Aster 為弟弟們立的紀念碑在中央島頂 | [04](04-ocean-corkus-heights-sky.md)、[10](10-lootruns-events-and-mini-quests.md) | — |

## 四、易錯處總表

### 4.1 一人多名：同一個人在語料裡有幾種寫法

| 人物 | 出現的寫法 | 怎麼處理 |
|---|---|---|
| Anathema | speaker「Her Majesty」、台詞「女王陛下」、首領名牌「Anathema」、原文大寫的 War（指她或戰爭領域）、她講過去的自己用 It | 揭曉前只寫「女王陛下」；大寫 War 要看上下文譯成「戰爭」並讓讀者感覺得出是專有名詞；It 譯「它」 |
| Saphanis | 2.2.0 以前叫 Dern Beast（Dern 野獸），之後正式命名 Saphanis | 舊資料的「Dern 野獸」若指的是這位巨獸，與一般怪物「Dern 野獸」分開 |
| Bob | Bob、Robert、「Wynn 的英雄」 | 「Wynn 的英雄」也被 Anathema 拿來稱呼玩家，看說話對象 |
| Tasim | Tasim、Sovereign Tasim | 同一人；當上統領後寫「Tasim 統領」 |
| Soosu | Sovereign Soosu、Soosu、??? (Sovereign Soosu)；《巧思的代價》的日記條目以日期當 speaker（125 NA～130 NA、0 IA～45 IA） | 日記的 speaker 是年代，不是人名；NA130「事情發生了」就是她的飛升 |
| Sovereign Two | Two、「第二位」、諜報總管（Spymaster） | 名字就是 Two，不譯「二號」 |
| Amadel | Amadel、WynnExcavation Leader Amadel、Traitor Amadel、Corrupted Amadel、Shadow Amadel | 同一人；「叛徒」「腐敗的」「影之」放前面 |
| Urelix | Dr. Urelix、Scientist Urelix、Urelix | 同一人；npc.json 有「Urelix 博士」「Urelix 科學家」兩條名牌 |
| Lecade | Lecade、Lecade 1、Lecade 2 | 《將軍下令》真假將軍並存，翻之前先確認是哪一個在講話 |
| Siegfried | Siegfried、演員 Gurix、Siegfried Agent（探員）、Siegfried Fan（粉絲） | 標 Siegfried 的台詞其實是演員在講 |
| Elphaba | Elphaba、Elphie | 暱稱照原文 |
| Siwel | Old Siwel（老 Siwel）、Siwel | 同一人的不同時期 |
| Draani 八世 | King Draani（Draani 國王）、King of Rodoroc | 戰時國王 Draani Thunderwill 是另一位 |
| Ragni 國王 | King of Ragni、Ragni's King | 同一人 |
| Paku | Chieftain Paku（酋長 Paku）、Dogun Chieftain（Dogun 酋長） | 語料用「酋長」，本書第三、四章用「族長」，見第五節 |
| Avos 族長 | Avos Chief、Avos Chieftain、Chief Avos | 三種都譯「Avos 族長」 |
| Caritat 家 | Mr. Caritat、Mister Caritat、Mrs. Caritat（Caritat 夫人）、Caritat Senior（老 Caritat） | 至少兩代人，前兩個 speaker 沒有名牌，譯文照上下文 |
| Aryn | Aryn、Aryn? | 帶問號的是被附身的狀態 |
| Garoth | 新版是 Olm 法師，舊版（腐敗失落聖所仍在用）是 Elkurn 的人類科學家 | 兩個設定，別混用台詞 |
| Witherhead | 凋零首、頭銜「{p} 的黑死病」 | 物品已改「凋零首」 |
| Poxper | 2.0 以前叫 Gregg'r | 舊譯名以現名為準 |
| Dramele | 2.1 以前叫 Volmor | 同上 |
| Takan | 物品 Ba'al's Betrayal 是他的舊名 | 不譯「巴力」 |
| Sui | 化名 "Water Grass"（水草）；同場的玩家化名 Shadow Bush（影叢） | speaker 欄位寫成帶引號的 "Water Grass" |
| Syndra | 代號「母獅」；《遺失的那一塊》Elise 的回憶裡，她的名字被換成 {u}，speaker 是一串亂碼（45 句） | 亂碼是刻意的，譯文不要補回名字 |
| Gale | Gale、Gale Nasin | Qira 的風之部隊首領就是 Fruma 出逃的 Gale Nasin |
| 車夫 | speaker「Caravan Driver」（npc.json 作「車隊駕駛」），本書第六章作「車夫」 | 見第五節 |
| 死神 | 《不得安息》的死神；Tower of Ascension 頂端「自稱死神」的冒牌貨 | 兩個不同的存在 |

### 4.2 同形異義：同一個英文字，不是同一個東西

| 英文 | 意思一 | 意思二 | 備註 |
|---|---|---|---|
| Gale | 人名 Gale Nasin | 疾風（名牌 Gale Dragonling＝疾風幼龍、裝備 Gale Burst 等） | 人名不譯 |
| Amber | Aldorei 的線民 | 琥珀（裝備、素材） | 人名不譯 |
| Grook | 幾個任務裡的 NPC | 雞形鳥、感嘆詞（Oh my grook、Thank grook）、Grook 魔法學院 | 感嘆詞要意譯 |
| Avos | 族名（Avo 的複數） | Avos 城、Lost Avos City | — |
| Cinfras | Gavel 中部的城市 | 《Haven 的往昔》的 NPC 名 | — |
| Canary | 《礦坑金絲雀》的幽影（speaker 留 Canary） | 任務名、裝備「金絲雀之盔」 | 台詞譯「金絲雀」 |
| Death | 死神 | Death Whistle（死亡之哨）、一般名詞「死亡」 | — |
| Elder Oak | Aldorei 的 Oak 長老 | 怪物 `Elder Oak {#}{#}`（古橡，同系列有「古霜」） | 見第六節 |
| Guardian／Keepers／Protector | 峽谷巨像是 Dogun 的「守護者」（Guardian） | 傳說神殿的 Keepers＝守秘人；森林守護者是光之領域的巨樹 | 三者不要用同一個詞 |
| Fiona | 《少女之塔》的淑女 | 《Aldorei 的祕密 第一部》也有一個 Fiona | 是否同一人未明 |
| Thomas | 《苦力怕入侵》的村民 | 叛徒 Thomas | 看任務 |
| Old Man | 一般的「老人」（speaker） | Old Man Martyn（名字的一部分） | 後者整串留原文 |
| Bush | 《遺失的那一塊》裡會說話的灌木，自稱 Bush（台詞留英文，npc.json 名牌卻是「灌木」） | Shadow Bush（玩家在綻放之刃的化名「影叢」） | 見第六節 |
| ??? | 《獵手的呼喚》的黑色骷髏 | 其他五十幾個任務裡「還不知道名字的人」 | speaker 共 341 句 |
| Two | Sovereign Two 的名字 | 數字 | — |
| Totem | 薩滿的圖騰 | 召喚怪物的石碑 | 兩種譯法是刻意的，不要統一 |

### 4.3 speaker 欄位的特殊寫法

- `???`：341 句、橫跨五十幾個任務，指「畫面上還沒顯示名字的人」。只有《獵手的呼喚》的那一位是固定角色。另有 `????` 到 `?????????`（《Qira 蜂巢》《條條大路達自由》）。
- 亂碼 speaker（《遺失的那一塊》45 句）：是 Syndra 在 Elise 記憶裡被抹掉的名字。
- `Player`（《傷痕之泉》等）與 `Player?`（《不歸之地》，玩家的另一半）是兩回事。
- 年代當 speaker：《巧思的代價》的日記（125 NA～45 IA）。
- 帶引號的 speaker：`"Water Grass"`（Sui 的化名）、`"Unknown Cursed One"`（《嶄新的開始》裡的受詛者檔案，不是人在講話）。
- 物件當 speaker：Narrator（旁白）、Note／Notes（筆記）、Diary（日記）、Old Book（舊書）、Bulletin Board、Tattered Letter、Fortune Cookie、Strange Coin、Switch、First～Fourth Digit（《失魂落鞋》的密碼提示牌）。
- 群體當 speaker：Everyone、Group、Crowd、Festival Goers、Watching Orcs。

### 4.4 名字裡的變位字、雙關與致敬（一律留原文）

| 名字 | 藏了什麼 | 出處 |
|---|---|---|
| Dren | Dern 的字母重排 | 《彼界之旅》 |
| Nilrem | Merlin 倒寫 | 〈Nilrem 的封緘信〉 |
| Efilim | 前建築師帳號 im4life1 的變位字 | wiki |
| Cinfras | Francis（Salted 的名字）的變位字 | wiki |
| Letvus | GM Selvut283 的變位字 | wiki |
| Thanos | 建築師 Nohats 的變位字 | wiki |
| Maxie | QA 職位 Imaxe 的變位字 | wiki |
| Kymer／Rymek | 前建築師 Mekyr | wiki |
| Caissop | Picasso 的變位字（興奮時冒西班牙語、法語） | 《大師的傑作》 |
| Sherk | Shrek 的變位字（獨居沼澤的食人魔） | 第二章 |
| Ynnos | 致敬《波特萊爾大遇險》的 Sonny | 《一千米之下》 |
| Hashr | 阿拉伯文「放逐」 | 第一章 |
| Lutho | 祖魯語「什麼都沒有」 | 第五章 |
| Croaquin | 讀音「像 Joaquin，但是 CROAK」 | 〈呱鳴怪談〉 |
| Dramele | 取自《惡魔城》的 Adramelech | 第三章 |
| Wynnston | 前管理員 Marion 的柯基 Winston | 淪陷工廠 |
| Ta Xevade | tax evade（《逃稅入門》的作者） | Maex 黑市 |
| Sanda Claus | Sand＋Santa，Nick 自稱沙漠版聖誕老人 | 《佳節有情》 |
| Regular Humyn | 名牌「普通人累」，拿 human 開玩笑 | 《禍從口入》 |
| Pigasus | pig＋Pegasus，語料作「天馬豬」 | 《大師的傑作》 |

## 附錄　其他有台詞的具名 NPC（速查）

上面的表只收劇情上有份量的人物。下面是語料 speaker 欄位裡**其他有名字的說話者**，共 336 位，依「台詞最多的那個任務」在本書哪一章介紹來分組；職稱、路人（Guard、Citizen、Merchant 之類）不列。這一節是用腳本從語料整理的，身分欄位省略，要知道是誰請看任務那一章，或直接查語料。繁中寫法取自 npc.json／label.json 的名牌；沒有名牌的照 speaker 原文寫。

### 第一章範圍（Wynn）

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Achper | Achper | 《木乃伊之怒》(6) | — |
| Alem | Alem | 《致命的失足》(4) | — |
| Alfonse | Alfonse | 《腐敗的村莊》(7) | — |
| Ankou | Ankou | 《登天之塔》(17) | — |
| Ariodo | Ariodo | 《神智出走》(29) | — |
| Batelu | Batelu | 《轉世重生》(21) | — |
| Bylvis | Bylvis | 《隧道之患》(2) | — |
| Calamaro | Calamaro | 《冰層之下》(9) | — |
| Captain Enduyn | Enduyn 隊長 | 《蛛群登巔》(14) | — |
| Captain Fenor | Fenor 隊長 | 《隧道之患》(4) | — |
| Captain Ragon | Ragon 隊長 | 《元素演練》(30)、《蘑菇人》(1) | — |
| Caras | Caras | 《部族之爭》(10) | — |
| Chief Excavator Dranfor | 首席挖掘員 Dranfor | 《WynnExcavation C 號遺址》(5) | — |
| Colonel Gailard | Gailard 上校 | 《熔岩湧泉》(22) | — |
| Dr. Essren | Essren 博士 | 《藥水製作》(6) | — |
| Edwin | Edwin | 《尋回過往》(4) | — |
| Eluzterp | Eluzterp | 《綠色黏漿》(17) | — |
| Elvura | Elvura | 《佳節有情》(3) | — |
| Enkser | Enkser | 《馬失前蹄》(18) | — |
| Enzan | Enzan | 《Enzan 的兄弟》(5) | — |
| Erisk | Erisk | 《少年長老》(5) | — |
| Excavator Admin Uci | 挖掘隊管理員 Uci | 《WynnExcavation D 號遺址》(8) | — |
| Excavator Placardus | 挖掘員 Placardus | 《WynnExcavation C 號遺址》(16) | — |
| Excavator Sean | 挖掘員 Sean | 《WynnExcavation D 號遺址》(1) | — |
| Farmer Cevalus | 農夫 Cevalus | 《音容宛在》(28) | — |
| Farmer Kelri | 農夫 Kelri | 《音容宛在》(9) | — |
| Favian | Favian | 《部族之爭》(10) | — |
| Ferndor | Ferndor | 《失魂落鞋》(16) | — |
| Flodur | Flodur | 《佳節有情》(8) | — |
| Fredris | Fredris | 《冰層之下》(15) | — |
| Geo | Geo | 《黃沙王朝》(4) | — |
| Gernald | Gernald | 《冰層之下》(10) | — |
| Gracen | Gracen | 《迷失叢林》(14) | — |
| Grandon | Grandon | 《失魂落鞋》(28) | 他（語料） |
| Gregor | Gregor | 《熔岩湧泉》(4) | — |
| Guard Klerodor | Klerodor 守衛 | 《WynnExcavation C 號遺址》(3) | — |
| Haily | Haily | 《佳節有情》(4) | — |
| Ildan | Ildan | 《幽徑》(18) | — |
| Inrekei | Inrekei | 《族人有難》(8) | — |
| Isfurus | Isfurus | 《少年長老》(10) | — |
| Jorkin | Jorkin | 《傳說神殿》(12) | — |
| Ka L'argwit | Ka L'argwit | 《族人有難》(2) | — |
| Krattson | Krattson | 《族人有難》(6) | — |
| Kroken | Kroken | 《冰層之下》(2) | — |
| Laen | Laen | 《失落之塔》(6) | — |
| Leacen | Leacen | 《失魂落鞋》(5) | — |
| Leucsaa | Leucsaa | 《牆內有靈》(13) | — |
| Lieutenant Van | Van 中尉 | 《奪塔之役》(12) | 他（語料） |
| Margaret | Margaret | 《佳節有情》(3) | — |
| Markque The Wizard | 巫師 Markque | 《Bob 的失落之魂》(8) | — |
| Mayor Alvin | Alvin 鎮長 | 《補給與運送》(12)、《蘑菇人》(4)、《蟲害叢生》(2) | — |
| Miner Linton | 礦工 Linton | 《隧道之患》(26) | — |
| Monte | Monte | 《少年長老》(11) | — |
| Nami The Healer | 治療師 Nami | 《熔岩湧泉》(9) | — |
| Omango | Omango | 《水面之下》(11) | — |
| Orikal | Orikal | 《腐敗的村莊》(32) | — |
| Ormrod | Ormrod | 《木乃伊之怒》(11) | — |
| Prentiss | Prentiss | 《神智出走》(7) | — |
| Private Cob | Cob 二等兵 | 《蛛群登巔》(7) | — |
| Psern | Psern | 《墮落者的宿命》(7) | — |
| Reshad | Reshad | 《佳節有情》(4) | — |
| Rhudya | Rhudya | 《噬骨寒霜》(6) | — |
| Roy | Roy | 《腐敗的村莊》(6) | — |
| Royal Advisor Carlos | 皇家顧問 Carlos | 《WynnExcavation D 號遺址》(9) | — |
| Rynend | Rynend | 《Maltic 的水井》(8) | — |
| Sayrr | Sayrr | 《水面之下》(8) | 他（語料） |
| Scout Evelyn | 偵察兵 Evelyn | 《元素演練》(3) | — |
| Sergeant Klafson | Klafson 中士 | 《隧道之患》(3) | — |
| Shaman Inrekei | Inrekei 薩滿 | 《族人有難》(3) | — |
| Shopkeeper Gibbs | 店主 Gibbs | 《沙城疑雲》(5) | — |
| Squidward | Squidward | 《冰層之下》(8) | — |
| Tarek | Tarek | 《黃沙王朝》(11) | — |
| Therck | Therck | 《Enzan 的兄弟》(8) | — |
| Todd | Todd | 《神智出走》(8) | — |
| Traitor Buice | 叛徒 Buice | 《WynnExcavation D 號遺址》(1) | — |
| Traitor Jaynar | 叛徒 Jaynar | 《WynnExcavation D 號遺址》(1) | — |
| Twendle | Twendle | 《Twain 宅邸》(12) | — |
| Tyko | Tyko | 《Tempo Town 風波》(10) | — |
| Viola | Viola | 《神智出走》(5) | — |
| Wedyf | Wedyf | 《Bob 的失落之魂》(21) | — |
| Wirt | Wirt | 《幽徑》(3) | — |
| Worid | Worid | 《叢林熱》(22) | — |
| WynnExcavation Scientist Azure | WynnExcavation Scientist Azure | 《WynnExcavation D 號遺址》(1) | — |
| Yavlis | Yavlis | 《失落的王嗣》(10) | — |
| Yodbon | Yodbon | 《綠色黏漿》(11) | — |

### 第二章範圍（Gavel 西部）

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Alegdar | Alegdar | 《究極的武器》(6) | — |
| Alraune | Alraune | 《條條大路達自由》(6) | — |
| Ann | Ann | 《空洞的寧靜》(13) | — |
| Ann & Qunit | Ann & Qunit | 《空洞的寧靜》(1) | — |
| Aunoa | Aunoa | 《空洞的寧靜》(14) | — |
| Balegar | Balegar | 《究極的武器》(4) | — |
| Bitteliso | Bitteliso | 《條條大路達自由》(5) | — |
| Blueberry | Blueberry | 《條條大路達自由》(9) | — |
| Captain Goruca | Goruca 隊長 | 《條條大路達自由》(20) | — |
| Caritat Senior | 老 Caritat | 《記憶的猜忌》(8) | — |
| Creinu | Creinu | 《條條大路達自由》(8) | — |
| Damys | Damys | 《Aldorei 的祕密 第一部》(13) | — |
| Dew Riverbreeze | Dew Riverbreeze | 《Aldorei 的祕密 第二部》(1) | — |
| Durbeo | Durbeo | 《條條大路達自由》(23) | — |
| Erd | Erd | 《破碎的心智》(1) | — |
| Etemitoro | Etemitoro | 《條條大路達自由》(9) | — |
| Fruludir | Fruludir | 《條條大路達自由》(3) | — |
| Gogedar | Gogedar | 《究極的武器》(7) | — |
| Grapes | Grapes | 《條條大路達自由》(10) | — |
| Guard Bitteliso | Guard Bitteliso | 《條條大路達自由》(5) | — |
| Guard Serepe | Guard Serepe | 《條條大路達自由》(7) | — |
| Harvy | Harvy | 《Aldorei 的祕密 第二部》(5) | — |
| Jixy | Jixy | 《條條大路達自由》(11) | — |
| Kaetan | Kaetan | 《禁忌牢獄》(2) | — |
| Kreimu | Kreimu | 《條條大路達自由》(29) | — |
| Lomoster | Lomoster | 《條條大路達自由》(9) | — |
| Lord Plaatic the Kind | 仁慈的 Plaatic 大人 | 《Lazarus 之坑》(6) | — |
| Mango | Mango | 《條條大路達自由》(8) | — |
| Merloni | Merloni | 《亡者之坑》(9) | — |
| Miliyus | Miliyus | 《條條大路達自由》(9) | — |
| Mister Caritat | Mister Caritat | 《記憶的猜忌》(9) | — |
| Moln Cloudcatcher | Moln Cloudcatcher | 《Aldorei 的祕密 第二部》(1) | — |
| Mr. Caritat | Mr. Caritat | 《記憶的猜忌》(11) | — |
| Mrs. Caritat | Caritat 夫人 | 《記憶的猜忌》(3) | — |
| Natt Moonchaser | Natt Moonchaser | 《Aldorei 的祕密 第二部》(1) | — |
| Nunexetor | Nunexetor | 《條條大路達自由》(16) | — |
| Onluk | Onluk | 《條條大路達自由》(20) | — |
| Peaches | Peaches | 《條條大路達自由》(7) | — |
| Peleros | Peleros | 《條條大路達自由》(2) | — |
| Peloros | Peloros | 《條條大路達自由》(6) | — |
| Poclo Wabblebuff of Gelibord | Poclo Wabblebuff of Gelibord | 《Lazarus 之坑》(9) | — |
| Professor Eih | Eih 教授 | 《破碎的心智》(24) | — |
| Qunit | Qunit | 《空洞的寧靜》(9) | — |
| Ressinter | Ressinter | 《條條大路達自由》(4) | — |
| Reyoretrsed | Reyoretrsed | 《究極的武器》(5) | — |
| Richard | Richard | 《究極的武器》(3) | — |
| Rileen | Rileen | 《巨獸之影》(15) | — |
| Runo | Runo | 《空洞的寧靜》(17) | — |
| Silverbull Rank Expert | Silverbull 階級專家 | 《Aldorei 的祕密 第一部》(1) | — |
| Sir Pigglesworth the Thirty-Fifth | Pigglesworth 爵士三十五世 | 《Lazarus 之坑》(7) | — |
| Sol | Sol | 《Aldorei 的祕密 第二部》(21) | — |
| Steve | Steve | 《破碎的心智》(1) | — |
| Sylvar | Sylvar | 《Aldorei 的祕密 第一部》(2) | — |
| Teiren | Teiren | 《條條大路達自由》(10) | — |
| Timmy | Timmy | 《記憶的猜忌》(7) | — |
| Timothy | Timothy | 《記憶的猜忌》(4) | — |
| Togedar | Togedar | 《究極的武器》(5) | — |
| Uggword Pollywaggon the Murderous | Uggword Pollywaggon the Murderous | 《Lazarus 之坑》(1) | — |
| Ureietun | Ureietun | 《條條大路達自由》(8) | — |
| Vaenr | Vaenr | 《Aldorei 的祕密 第一部》(1) | — |
| Vera | Vera | 《空洞的寧靜》(22) | — |
| Xoeinbor | Xoeinbor | 《條條大路達自由》(11) | — |
| Yorman | Yorman | 《空洞的寧靜》(5) | — |
| Yunelsu | Yunelsu | 《條條大路達自由》(14) | — |
| Zuett | Zuett | 《條條大路達自由》(44) | — |

### 第三章範圍（Gavel 中東部）

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Alie | Alie | 《Gerts 大饑荒 第二部》(4) | — |
| Baron Axol | Axol 男爵 | 《危難航程》(1) | — |
| Bory | Bory | 《Gerts 大饑荒 第二部》(27) | — |
| Bricot | Bricot | 《墜落的貨運》(10) | — |
| Calo | Calo | 《危難航程》(4) | — |
| Captain Ackbar | Ackbar 船長 | 《危難航程》(29) | — |
| Colry | Colry | 《Gerts 大饑荒 第二部》(4) | — |
| Daffyd | Daffyd | 《礦坑金絲雀》(6) | 他（語料） |
| Dejol | Dejol | 《迷途之人》(12) | — |
| Doan | Doan | 《獲取護照》(35) | — |
| Dobile | Dobile | 《谷底買賣》(9) | — |
| Dogun Delg | Dogun Delg | 《臨界點》(1) | — |
| Dohstaj | Dohstaj | 《谷底買賣》(8) | — |
| Dorroc | Dorroc | 《谷底買賣》(9) | — |
| Edac | Edac | 《臨界點》(1) | — |
| Egpla | Egpla | 《王位試煉》(16) | — |
| Engine Instructions | Engine Instructions | 《危難航程》(3) | — |
| Engineer Eric | 工程師 Eric | 《危難航程》(13) | — |
| Fetu | Fetu | 《迷途之人》(9) | — |
| Gert Battle Chief | Gert 戰鬥族長 | 《Gerts 大饑荒 第一部》(3) | — |
| Gert Chief | Gert Chief | 《Gerts 大饑荒 第二部》(5) | — |
| Gordon | Gordon | 《迷途之人》(17) | — |
| Gylia Watchman | Gylia 哨兵 | 《Gerts 大饑荒 第一部》(2) | — |
| Hilan | Hilan | 《隱匿之城》(5) | — |
| Hrona | Hrona | 《王位試煉》(6) | — |
| Jankan | Jankan | 《峽谷兀鷲》(10) | — |
| Jitak | Jitak | 《Gerts 大饑荒 第一部》(22) | — |
| Lieutenant Gren | Gren 中尉 | 《將軍下令》(6) | — |
| Lodog | Lodog | 《谷底買賣》(12) | — |
| Lumilda | Lumilda | 《隱匿之城》(6) | — |
| Marden | Marden | 《墜落的貨運》(8) | — |
| Mastro | Mastro | 《谷底買賣》(12) | — |
| Micho | Micho | 《礦坑金絲雀》(1) | — |
| Nakoba | Nakoba | 《谷底買賣》(17) | — |
| Narder | Narder | 《隱匿之城》(15) | — |
| Nichye | Nichye | 《隱匿之城》(1) | — |
| Passenger Casula | 乘客 Casula | 《危難航程》(1) | — |
| Passenger Jeremy | 乘客 Jeremy | 《危難航程》(1) | — |
| Passenger John | 乘客 John | 《危難航程》(1) | — |
| Passenger Nancy | 乘客 Nancy | 《危難航程》(2) | — |
| Private Walder | Walder 二等兵 | 《臨界點》(5) | — |
| Ranol | Ranol | 《莫名其牛》(9) | — |
| Reder | Reder | 《隱匿之城》(1) | — |
| Siegfried Agent | Siegfried 探員 | 《Gavel 的英雄》(1) | — |
| Siegfried Fan | Siegfried 粉絲 | 《Gavel 的英雄》(9) | — |
| Siegfried Fan 1 | Siegfried Fan 1 | 《Gavel 的英雄》(2) | — |
| Siegfried Fan 2 | Siegfried Fan 2 | 《Gavel 的英雄》(3) | — |
| Sub-engineer Akot | 副工程師 Akot | 《危難航程》(1) | — |
| Sybeth | Sybeth | 《隱匿之城》(1) | — |
| Syni | Syni | 《谷底買賣》(8) | — |
| The Mother Drill | The Mother Drill | 《臨界點》(3) | — |
| Thesead's Mayor | Thesead's Mayor | 《隱匿之城》(6) | — |
| Tylas | Tylas | 《將軍下令》(8) | — |
| Valimare | Valimare | 《謀殺疑雲》(8) | — |
| Yobon | Yobon | 《谷底買賣》(8) | — |
| Yuge | Yuge | 《迷途之人》(7) | — |

### 第四章範圍（海洋、Corkus、Heights、Sky Islands）

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Arlene | Arlene | 《一千米之下》(4) | — |
| Arock | Arock | 《矮人與 Doguns 第一部》(2) | — |
| Babbi | Babbi | 《矮人與 Doguns 第一部》(3) | — |
| Barden | Barden | 《矮人與 Doguns 第一部》(8) | — |
| Bertil | Bertil | 《使者 第二部》(1) | — |
| Big Boss Mech-DM666 | Big Boss Mech-DM666 | 《鐵皮末路》(2) | — |
| C.S.S Wavebreaker Captain | C.S.S Wavebreaker 船長 | 《使者 第一部》(7) | — |
| Carl | Carl | 《羽毛紛飛 第二部》(5) | — |
| Chef Hamsey | 廚師 Hamsey | 《禍從口入》(41) | 他（語料） |
| Cidre | Cidre | 《Grook 魔法學院》(3) | — |
| Clomius | Clomius | 《羽毛紛飛 第二部》(4) | — |
| Corkus City Citizen | Corkus City Citizen | 《五味雜陳》(35) | — |
| Corkus Delegate | Corkus 代表 | 《使者 第一部》(24)、《使者 第二部》(3) | — |
| Corkus Jeweler | Corkus Jeweler | 《羽毛紛飛 第二部》(4) | — |
| Czytash | Czytash | 《Zhight 島》(12) | — |
| Dwarfi | Dwarfi | 《矮人與 Doguns 第一部》(2) | — |
| Fiery Mech-DM2 | Fiery Mech-DM2 | 《鐵皮末路》(2) | — |
| Gagly | Gagly | 《矮人與 Doguns 第一部》(4) | — |
| Grant | Grant | 《羽毛紛飛 第二部》(5) | — |
| Intelligence Mech-DM3 | Intelligence Mech-DM3 | 《鐵皮末路》(1) | — |
| Javen | Javen | 《五味雜陳》(16) | — |
| Jesp | Jesp | 《一千米之下》(6) | — |
| Jofash Clairvoyant | Jofash 預言者 | 《美妙旅程》(37) | — |
| Jofash Resident | Jofash 居民 | 《美妙旅程》(134) | — |
| Kantor | Kantor | 《一千米之下》(9) | — |
| Key Temple Dragon | Key Temple Dragon | 《使者 第二部》(1) | — |
| Lozeg | Lozeg | 《矮人與 Doguns 第一部》(4) | — |
| Mahou | Mahou | 《矮人與 Doguns 第一部》(1) | — |
| Manny | Manny | 《一千米之下》(8) | — |
| Mei | Mei | 《羽毛紛飛 第二部》(8) | — |
| Nodise | Nodise | 《羽毛紛飛 第二部》(9)、《臨界點》(3) | — |
| Olivin | Olivin | 《使者 第一部》(7) | — |
| Pens'r | Pens'r | 《矮人與 Doguns 第一部》(3) | — |
| Pensa | Pensa | 《Grook 魔法學院》(1) | — |
| Phief | Phief | 《Zhight 島》(9) | — |
| Phinas | Phinas | 《鐵皮末路》(21) | — |
| Reiva | Reiva | 《使者 第二部》(1) | — |
| S-63 Mech | S-63 Mech | 《使者 第二部》(2) | — |
| Security Bot N4T17 | Security Bot N4T17 | 《使者 第二部》(2) | — |
| Seth | Seth | 《海上驚魂》(7) | — |
| Sleeping Jofash Resident | Sleeping Jofash Resident | 《美妙旅程》(1) | — |
| Steam Mech-DM1 | Steam Mech-DM1 | 《鐵皮末路》(2) | — |
| Tasha | Tasha | 《羽毛紛飛 第二部》(1) | — |
| Tirt | Tirt | 《Zhight 島》(9) | — |
| Una | Una | 《Grook 魔法學院》(3) | — |
| Zhight | Zhight | 《Zhight 島》(15) | — |

### 第五章範圍（Silent Expanse）

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Commander Lilles | Commander Lilles | 《歸鄉之旅》(3) | — |
| Fruman Recruit | Fruma 新兵 | 《歸鄉之旅》(10) | — |
| Kohma | Kohma | 《獵手的呼喚》(9) | — |
| Kotham | Kotham | 《獵手的呼喚》(2) | — |
| Leni | Leni | 《獵手的呼喚》(2) | — |
| Lieutenant Tolv | Tolv 中尉 | 《歸鄉之旅》(11) | — |
| Paxon | Paxon | 《歸鄉之旅》(17) | — |
| Rodoroc Slum Citizen | Rodoroc Slum Citizen | 《獵手的呼喚》(4) | — |
| Rusty Bartender | Rusty 調酒師 | 《彼界之旅》(5) | — |

### 第六章範圍（Fruma）

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Barkeep Romu | Romu 酒保 | 《壩下缺水》(20) | — |
| Bulletin Board | Bulletin Board | 《慶典成煙》(5) | — |
| Butler Hayn | Hayn 管家 | 《壩下缺水》(13) | — |
| Carriage Driver | Carriage Driver | 《強者生存》(3) | — |
| Commander Balsura | Commander Balsura | 《壩下缺水》(6) | — |
| Cousullo | Cousullo | 《傷痕之泉》(11) | — |
| Denis | Denis | 《傷痕之泉》(2) | — |
| Dogi | Dogi | 《強者生存》(6) | — |
| Doyel | Doyel | 《壩下缺水》(2) | — |
| Espren Border Guard | Espren Border Guard | 《嶄新的開始》(12) | — |
| Espren City Guard | Espren City Guard | 《嶄新的開始》(6) | — |
| Estur | Estur | 《傷痕之泉》(2) | — |
| Feri | Feri | 《傷痕之泉》(3) | — |
| Fiscu | Fiscu | 《傷痕之泉》(2) | — |
| Fortune Cookie | Fortune Cookie | 《慶典成煙》(15) | — |
| Halbe | Halbe | 《強者生存》(11) | — |
| Holger | Holger | 《強者生存》(3) | — |
| Jamie | Jamie | 《隱於迷霧》(9) | — |
| Kabo | Kabo | 《詛咒之人》(8) | — |
| Katze | Katze | 《強者生存》(5) | — |
| Kerry | Kerry | 《強者生存》(7) | — |
| Kyren | Kyren | 《強者生存》(16) | — |
| Lishvil | Lishvil | 《羈絆成灰》(4) | — |
| Lynn | Lynn | 《強者生存》(13) | — |
| Madelen | Madelen | 《強者生存》(6) | — |
| Maripe | Maripe | 《傷痕之泉》(73) | 他（語料） |
| Marseled | Marseled | 《傷痕之泉》(33) | 她（語料） |
| Masah | Masah | 《隱於迷霧》(5) | — |
| Master Oshman | Oshman 大師 | 《壩下缺水》(16) | — |
| Matilda | Matilda | 《強者生存》(9) | — |
| Merchant Risch | Risch 商人 | 《壩下缺水》(17) | — |
| Mont | Mont | 《傷痕之泉》(6) | — |
| Mother Aris | Mother Aris | 《壩下缺水》(18) | — |
| Nerod | Nerod | 《傷痕之泉》(3) | — |
| Niren | Niren | 《強者生存》(20) | — |
| Nisen | Nisen | 《隱於迷霧》(6) | — |
| Nixeph Watchman | Nixeph Watchman | 《嶄新的開始》(3) | — |
| Perin | Perin | 《強者生存》(5) | — |
| Petur | Petur | 《強者生存》(2) | — |
| Priscilla | Priscilla | 《強者生存》(9) | — |
| Ragni Courier | Ragni Courier | 《女王的新兵》(6) | — |
| Recruiter Alena | Recruiter Alena | 《強者生存》(2) | — |
| Recruiter Ezero | Recruiter Ezero | 《強者生存》(6) | — |
| Recruiter Riva | Recruiter Riva | 《強者生存》(5) | — |
| Recruiter Strumon | Recruiter Strumon | 《強者生存》(10) | — |
| Robin | Robin | 《強者生存》(6) | — |
| Robyn | Robyn | 《強者生存》(8) | — |
| Rules of The Crown Jewel | Rules of The Crown Jewel | 《絕望的序曲》(1) | — |
| Ryde | Ryde | 《壩下缺水》(4) | — |
| Saldin | Saldin | 《強者生存》(9) | — |
| Shu | Shu | 《隱於迷霧》(5) | — |
| Sir Hurlay | Hurlay 爵士 | 《壩下缺水》(7) | — |
| Timaca Mason | Timaca Mason | 《鋼鐵同心》(2) | — |
| Timasca Mason | Timasca Mason | 《變革迴響》(3) | — |
| Vault Chief | Vault Chief | 《遺失的那一塊》(6) | — |
| Verdi | Verdi | 《強者生存》(3) | — |
| Wilton | Wilton | 《強者生存》(9) | — |
| Wings Commander Kaezu | Wings Commander Kaezu | 《希望的合奏》(1) | — |
| Xao | Xao | 《詛咒之人》(6)、《一葉知秋》(5) | — |
| Yvonne | Yvonne | 《強者生存》(12) | — |

### 第七章範圍（祕密故事）

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Dado | Dado | 《Quartron 崛起》(15) | 他（語料） |
| Harnort | Harnort | 《Quartron 崛起》(15) | — |
| Karoc Miner | Karoc 礦工 | 《Quartron 崛起》(4) | — |
| Manager Nasea | Nasea 經理 | 《Quartron 崛起》(1) | — |
| Nasea | Nasea | 《Quartron 崛起》(30) | — |
| Popo | Popo | 《Quartron 崛起》(6) | — |

### 第八章範圍（地城與討伐戰）

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Malo | Malo | 《蟲洞》(15) | — |

### 第九章範圍

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Excavator Vade | 挖掘員 Vade | 《WynnExcavation A 號遺址》(14) | — |
| Tesha | Tesha | 《WynnExcavation A 號遺址》(8) | — |

### 第十章範圍（Lootrun、世界事件與迷你任務）

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Adigard | Adigard | 《冰封諸國》(13) | 他（語料） |
| Apprentice Arkech | 學徒 Arkech | 《歸來的兄長》(1) | — |
| Drucksh | Drucksh | 《歸來的兄長》(27) | — |
| Hallfred | Hallfred | 《冰封諸國》(10) | — |

### 還沒有章節介紹的任務

| 英文（speaker） | 繁中寫法 | 出現的任務（句數） | 代名詞 |
|---|---|---|---|
| Anast | Anast | 《顆粒無收》(21)、《獵手的呼喚》(5) | — |
| Doubiss | Doubiss | 《顆粒無收》(17) | — |

## 五、本章新出現的譯名與本書內部的不一致

本章是索引，**沒有新造譯名**；表裡的寫法都取自語料或前十章。整理時發現本書各章與語料之間有幾處寫法不同，列在這裡，之後統一時一併處理：

| 原文 | 語料的寫法 | 本書的寫法 | 建議 |
|---|---|---|---|
| Chieftain（Dogun 的 Paku） | 酋長 Paku、Dogun 酋長（npc.json） | 族長（第三、四章） | 本書改照語料寫「酋長」；Avos 的 Chief／Chieftain 語料作「族長」，兩族不同是語料現況 |
| Caravan Driver／the Driver | 車隊駕駛（名牌）；台詞兩句留 Driver | 車夫（第六章） | 待定；第六章把「車夫」當角色名用，與名牌不一致 |
| Kandon Beda | Kandon Beda（原文本身沒有連字號） | Kandon-Beda（第三、四章） | 本書照原文 |
| Dark Forest／Light Forest | 地區名留英文 | 正文多留英文，README 目錄作「黑暗森林」「光之森林」 | 地區名照慣例留英文 |
| Canyon of the Lost | 地名多為佔位符，看不到譯文 | 失落者峽谷（第三章標題） | 待定；若照地區名慣例應留英文 |
| Royal Dam | 語料未見 | 皇家水壩（第六章） | 語料出現時再對 |

## 六、本章發現的語料問題

只記錄，**沒有改語料**。

| # | 檔案 | 條目 | 問題 | 建議 |
|---|---|---|---|---|
| C1 | quest-name.json | Apotheosis、Pit of the Dead、The Realm of Light | 三個任務檔的 `_meta.quest` 帶 `(Quest)` 後綴，quest-name.json 沒有對應的鍵（misc.json 倒有「神化 [任務]」「亡者之坑 [任務]」「光之領域 [任務]」）。追蹤器與對話開頭若用 quest-name.json 查，這三個名字會留英文 | 實機確認追蹤器顯示；缺的話補三個鍵 |
| C2 | quest/redbeard-s-booty.json、quest/beneath-the-depths.json | Redbeard's Booty#001、#003、#010、#012、#019、#039；Beneath the Depths#005、#039 | Pirate Cove 在任務目標與台詞作「海盜灣」，探索點與「發現地區」留 Pirate Cove | 照地區名慣例擇一（地區名留英文的話，任務裡改回 Pirate Cove） |
| C3 | misc.json | `Unlocks Upper Mesa - Cannot be started` | 同一組五條解鎖訊息，四條作「方山高地」，這一條留「Upper Mesa」 | 統一 |
| C4 | quest/hunger-of-the-gerts-*.json；npc.json | Gylia Watch | 台詞留英文 Gylia Watch，名牌（Gylia 守望隊見習生、守衛、騎士）作「Gylia 守望隊」 | 擇一；組織名可以翻，也可以照地區名慣例留英文 |
| C5 | discovery.json；discovery-name.json；quest-name.json | Tower of Ascension | 探索點名稱與任務名作「登天之塔」，兩條探索點說明文留 Tower of Ascension | 說明文改「登天之塔」 |
| C6 | 多個檔（Enter the Dojo、From the Bottom、The Qira Hive、quest.json、misc.json） | 24 句 | 10-24 裁示 the Hive＝蜂巢，本工作樹的語料還有 24 句留 Hive（含「Hive 商店」「Qira Hive」） | 若裁示的 PR 還沒合，合併後再掃一次 |
| C7 | quest/a-new-beginning.json；quest/a-journey-home.json | A New Beginning#053、A Journey Home#124 | the Driver 留英文（#053 連冠詞寫成「由 the Driver 帶進」），名牌 Caravan Driver 作「車隊駕駛」 | 決定 the Driver 的譯名後兩句一起改（見第五節） |
| C8 | quest/the-missing-piece.json；npc.json | The Missing Piece#101–104；`Bush` | 會說話的灌木自稱 Bush，台詞留英文（「我是 Bush」「Bush 我可太清楚她了」），npc.json 的名牌 `Bush` 卻是「灌木」 | 確認名牌 `Bush` 是不是這一位；是的話兩邊擇一 |
| C9 | npc.json | Elder Oak | 其他 Aldorei 長老（Willow、Rowan、Alder、Hawthorn、Birch、Elm、Hickory）都有「X 長老」名牌，唯獨沒有 Elder Oak；只有怪物 `Elder Oak {#}{#}`＝古橡 | 實機看 Oak 長老的名牌；有的話補「Oak 長老」 |
| C10 | npc.json | 綻放之刃的代號 | Bleeding Rose、Dusk Garden、Crimson Dagger、Thorn Blade、Violent Heather、Wilted Camellia 台詞裡都翻成中文，npc.json 沒有這幾條名牌 | 若實機頭上有名牌，會顯示英文；看過再補 |
| C11 | quest/recipe-for-disaster.json | speaker `Astralus`、src「Welcome to Astralaus' Astral House!」 | 同一個 Sky Islands 法師有三種拼法：speaker Astralus、原文 Astralaus、wiki 與第十章 Astraulus；譯文統一寫 Astralus（照 speaker） | 譯文維持一種即可；src 照遊戲保留 |
| C12 | npc.json；quest/star-thief.json | `Agent` | 名牌 Agent 留英文，任務目標作「密探」 | 名牌補「密探」 |

另外，第十章語料問題 #2（Afelis 的一句台詞誤放在 `quest/cook-assistant.json` #021）與本章人物表的 Afelis 一列相關，這裡不重複列。

## 七、出處清單

- 本書第一至第十章（各章人物表、正文與「本章新出現的譯名」）。
- 語料：`translations/zh_tw/quest/*.json`、`secret/*.json` 的 speaker 欄位（句數統計）；`npc.json`、`label.json`（名牌寫法）；`quest-name.json`（任務名）；`discovery.json`、`discovery-name.json`、`misc.json`（地名與組織名的寫法）。
- 用詞依據：README「全書統一用詞」、GLOSSARY.md、2026-10-24 使用者裁示的用詞（Hive＝蜂巢、Bovemist＝牛神信徒、腐敗者、冰亞龍等）。
- 人物的性別與代名詞：原文 he／she 的出現次數（只列一邊至少兩次、另一邊零次的），再對照各章正文；神祇、Ozoth、Gikyo 照已定案的規則。
- wiki（各章已引用的頁面，本章沒有另外抓）：[Timeline](https://wynncraft.wiki.gg/wiki/Timeline)、[Secret Discoveries](https://wynncraft.wiki.gg/wiki/Secret_Discoveries)，以及各章出處清單所列的人物與地點頁。
