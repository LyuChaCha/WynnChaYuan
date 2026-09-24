package com.wynnchayuan.capture;

import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 擋掉夾帶玩家個資的系統訊息。
 *
 * <h2>為什麼需要</h2>
 * 聊天的 {@code INFO} 類型不只有介面字串，也混著一堆<b>執行期才產生、內含玩家資料</b>
 * 的通知。實測收集到的內容包括：
 *
 * <pre>
 *   Green_teaTW's friends (43): Haagen_Dazs69, WD69, GreenTEA6666, ...   ← 完整好友名單
 *   PoorChaCha {#} shouts: 晚安                                           ← 別人打的字
 *   eric18960 has logged into server AS12 as a Knight
 *   [!] Congratulations to I Tonk you Bonk for reaching level 110
 *   你死在了 [-781, 89, -5563]                                            ← 座標
 * </pre>
 *
 * 這些東西有兩個問題：<b>不是可翻譯的內容</b>（玩家名字是變數，不是文案），
 * 而且 {@code captured.json} 是要拿給大家一起翻譯用的，
 * 把別人的名字和自己的好友名單寫進去再分享出去並不妥當。
 *
 * <h2>做法與限制</h2>
 * 用結構性特徵比對，再加上本機玩家名稱。這是<b>啟發式的，不保證滴水不漏</b>——
 * 遊戲隨時可能新增別種通知格式。所以寧可誤擋也不要漏放：
 * 漏掉幾句可翻譯的系統訊息，代價遠小於把別人的個資寫進共享檔案。
 */
public final class PlayerDataFilter {

    /**
     * 一看就知道帶玩家資料的訊息特徵。
     *
     * <p>刻意用「片語」而不是完整比對，因為前後常常還接著別的東西。
     */
    private static final List<String> MARKERS = List.of(
            " shouts:",                 // 玩家喊話
            "has logged into server",   // 上線廣播
            "has left the game",
            " left the game",       // 沒有 has 的版本，一樣夾帶玩家名
            "'s friends (",             // 好友名單標題
            "[Server: ",                // 好友名單條目
            "Congratulations to",       // 別人的成就
            " has joined your party",
            " has invited you",
            " sent you a friend request",
            " from their crate!",       // 開箱廣播，夾帶開箱者的玩家名
            // 「某某人丟了經驗炸彈」，夾帶玩家名。
            //
            // 原本比對的是後面<b>帶空白</b>的 " has thrown a "，而聊天室會把
            // 這句折行，剛好折在 a 後面：收到的是
            // 「{#} {#}JC grindeando has thrown a」——結尾沒有那個空白，
            // 整句穿了過去。折行位置是遊戲決定的，不能當作比對的一部分。
            " has thrown a",
            // 同一則廣播的其餘幾片，名字散在不同片上：
            // 「{#} JC grindeando Loot Bomb has expired! …」
            " Loot Bomb has expired",
            // 炸彈不只 Loot 一種：Combat Experience、Profession Speed…
            // 每一種到期都是同一句廣播，主詞一樣是丟炸彈的人。
            // 「{#} hp giving stick Combat Experience Bomb has expired!」就漏進過收件匣。
            " Bomb has expired",
            // 「PoorChaCha has placed a mob totem in {p} at …」與
            // 「You are inside of PoorChaCha's mob totem.」——石碑是誰放的
            // 就寫誰的名字，而且句子裡還有座標。實機的 captured.json 裡
            // 這兩句都漏進來過。
            // 「某某人的故事石碑」。玩家名開頭是數字時，數字會先被抽成
            // {~} 佔位符，剩下的半截名字（{~}jimmy）就不再等於名單上那個 ID，
            // 線上名單那條路整個失效。認後面那個物件名比認名字穩。
            "'s Totem of Tales",
            "\u2019s Totem of Tales",
            // 「RedHeartCCC's Mob Totem has run out」。跟下面那句 mob totem
            // 是兩則不同的廣播——這一則大寫、句尾也不一樣，下面那條比對不到。
            "'s Mob Totem",
            "’s Mob Totem",
            // 住宅島的訪客通知：「Elaina Lover is visiting this island. Say hi!」
            // 與「You have invited Elaina Lover into your island.」
            " is visiting this island",
            " into your island",
            // 無名異常體的死亡廣播。跟其他討伐戰死法同一類，動詞不同而已。
            " had their existence effaced",
            // 討伐戰的下降預告，主詞是隊友的帳號名。
            " is preparing to descend",
            // 各地區還有自己的死法，一種一句模板，主詞永遠是別人。
            // `was reduced to ashes` 要連 was 一起認——「has been almost
            // entirely reduced to ashes」是地景敘述，那句該收。
            " became another casualty of War",
            " was entirely corrupted",
            " was reduced to ashes",
            " was instantly evaporated",
            " ceased to be anything human",
            " was buried beneath earth",
            " made a fatal mistake",
            "Cleansing fire melted ",
            // 一般地圖上的死亡快訊，一種怪一句俏皮的模板，主詞是死掉的玩家。
            " fell victim to ",
            " was clobbered by ",
            " was bashed into paste by ",
            " was impaled by ",
            " was silenced by ",
            " was stomped by ",
            " was weaker than ",
            "took a shortcut to death's door",
            "There's only ashes where ",
            "-- oops, other way around",
            // 2026-09-14 收件匣又冒出的六種死法，跟 tools/check-leaks.py 同一批。
            " failed to evade ",
            " was shot down by ",
            " was de-animated by ",
            " became swiss cheese",
            " to thank for their death",
            " got a new skull piercing",
            // 升等廣播，主詞是別人的帳號名。
            " is now combat level",
            // 公會大廳／住宅島的招牌：撇號前面是玩家或公會取的名字。
            "'s HQ",
            "’s HQ",
            // 玩家打的公會招人廣告：「TW Guild ! DM Owner or Chiefs」。
            "DM Owner",
            "DM Chiefs",
            " has placed a mob totem",
            " has placed a gathering totem",
            " mob totem. Get your own",
            " gathering totem. Get your own",
            " of your XP to ",          // 「將 X% 經驗貢獻給某公會」，夾帶公會名
            " has just logged in",      // 上線通知，夾帶玩家名
            " has just logged out",
            " is now online",
            " is now offline",
            // 領地名牌：「Kandon Ridge / Controlled by Paladins United [Lv. 32]」。
            // 公會名稱跟玩家名稱一樣是別人的資料，不該進共享語料；而且那一行
            // 每塊領地、每次易主都不一樣，收進來也永遠不會有人翻。
            //
            // 先前會漏掉，是因為這條走<b>名牌</b>那條路，而名牌只擋得住
            // 「長得像帳號名」的東西——公會名沒有底線，整條穿了過去。
            // 一次 Lootrun 就收進 83 塊別人的領地。
            "Controlled by ",
            // 討伐戰的增益選擇：「Watari has chosen the Elder III buff!」——
            // 前面那個是隊友的 ID。一場討伐戰就收進 80 條。
            " has chosen the ",
            // 討伐戰裡的治療與補益：「King gave you [+240 ❤]」、
            //「IHateRaid has given you 2 resistance and 2 strength.」
            // 句型是遊戲的模板，但主詞永遠是隊友的 ID。
            " gave you [+",
            " has given you ",
            // 「Party Finder: Hey MorphCascade, over here!」——後面接的是收訊者本人。
            "Party Finder: Hey ",
            // 市集篩選「- Name Contains: 某某」——冒號後面是玩家自己打的搜尋字。
            "Name Contains: "
    );

    /**
     * 討伐戰的死亡訊息，例如 {@code Watari was purified by Orphion.}、
     * {@code KFA was crushed between the Wyrmling's jaws.}
     *
     * <p>每座討伐戰各有一套講究的死法文案，句子本身是遊戲寫死的，
     * 但<b>主詞永遠是隊友的 ID</b>。一場團隊跑下來就收進三十幾條，
     * 主詞每場都不一樣，收進來也永遠不會有人翻。
     *
     * <p>用「行首 + 一個詞 + 特定句型」而不是單純的片語比對，是因為
     * {@code passed away} 這種說法在任務對話裡完全可能出現——
     * 那是該收的內容，不能一起擋掉。
     */
    private static final Pattern RAID_DEATH = Pattern.compile(
            "(^|(?:\\{#}\\s*)+)\\S+ (passed away"
                    + "|was purified by"
                    + "|was drained of"
                    + "|lost their color to"
                    + "|was crushed b"          // beneath the Wyrmling / between the jaws
                    + "|had their existence effaced"
                    + "|has been overtaken"   // NOL：隊友被寄生體吞沒
                    + "|has been crystallized"  // NOL：隊友被結晶封住
                    + "|was minced to bits by"  // 地底之巢：被幼龍絞碎
                    + "|began to glow and then faded"
                    + "|has reconnected"
                    // 一般地圖上的死亡快訊，走的是<b>標題</b>那條路：
                    // 「{#}{#}ChangJenChief has died」。前綴是連著的兩個符號、
                    // 中間沒有空白，所以先前那個 "{#} " 的前綴對不上。
                    + "|has died)"
                    + "|['’]s existence was redacted",
            Pattern.MULTILINE);

    /**
     * 廣播裡那句「謝謝某某」，例如 {@code {#} {#}Thank JC grindeando}。
     *
     * <p>這是寶物炸彈廣播折出來的其中一片，整片只有句首兩個字是遊戲的，
     * 後面就是別人的 ID。要求前面至少有一個符號佔位符，才不會誤傷
     * 任務對話裡的「Thank ...」——那些不帶符號。
     */
    private static final Pattern THANK_SOMEONE =
            Pattern.compile("(?m)^(?:\\{#}\\s*)+Thank ");

    /**
     * 行首一個字黏著數字、空一格接小寫動詞：「Sparkl{~} failed to evade …」、
     * 「zhanhua{~} was de-animated by …」。
     *
     * <p>帳號名尾巴的數字收集時變成 {@code {~}}。遊戲自己的句子不會拿
     * 「單字{~}」當主語開頭，所以不必一種死法一種死法地追——新的死法
     * 只要主詞帶數字就擋得住。跟 {@code tools/check-leaks.py} 的同一條規則。
     */
    private static final Pattern ACCOUNT_LEAD =
            Pattern.compile("(?m)^(?:\\{#}\\s*)*[A-Za-z]{2,}\\{~}\\s+[a-z]");

    /**
     * 隊伍計分板上的一名隊友，例如 {@code - [||{~}||] PoorChaC [{~}]}、{@code - {~}jimmy}。
     *
     * <h2>為什麼要認形狀</h2>
     * 那一欄的名字被欄寬<b>截斷</b>，名字裡的數字又先被抽成 {@code {~}}——
     * 「駝峰」「底線」那些字形判準全部落空，一次組隊就收進 41 條隊友名字，
     * 出現次數還排在整份 captured.json 的最前面。
     *
     * <p>{@link #mentionsOnlinePlayerLoose} 問伺服器要名單，那條路準，但要求
     * 當下真的連著線。這一條認的是<b>那一列的排版</b>：破折號開頭，接一條
     * {@code [||…||]} 的血條、或是直接接一個以佔位符起頭的半截名字。
     * 遊戲自己的介面文字不會長這樣，離線重跑檢查時也照樣擋得住。
     *
     * <p>半截名字那一路要求<b>整列只有那一個詞</b>：量過整份語料，放寬成
     * 「開頭是 {@code -} 再接 {@code {~}} 開頭的字」會連「{@code - {~}x 松木板}」
     * 那種數量列一起擋掉，一百多條。數量列後面還接著東西，隊友那一列沒有。
     */
    private static final Pattern PARTY_ROW = Pattern.compile(
            "(?m)^\\s*-\\s*(\\[\\|+|\\{~}[A-Za-z_][A-Za-z0-9_]*\\s*$)");

    /**
     * 交易、公會倉庫與隊伍收送的紀錄，那一列中間有個箭頭：
     *
     * <pre>
     *   {~}ay_joker → PoorChaCha: {~} Emeralds
     *   PoorChaCha → {~}yue: a Guild Tome
     *   → PoorChaCha {~}x {p} Teleportation Scroll [{~}/{~}] (Everyone)
     *   → RRRRAcher [AS{~}/Hunter]
     * </pre>
     *
     * <p>箭頭的一邊（常常是兩邊）就是玩家 ID。這幾種寫法各不相同，一句一句
     * 追不完；共通的是<b>那個箭頭</b>加上至少一個佔位符——遊戲的介面文字
     * 不會這樣寫。量過整份語料，七萬多條有譯文的條目<b>一條都不會</b>被擋到。
     */
    private static final Pattern TRADE_LINE =
            Pattern.compile("(?m)^[^\\n]*→[^\\n]*\\{[#~pu]\\d?}[^\\n]*$"
                    + "|(?m)^[^\\n]*\\{[#~pu]\\d?}[^\\n]*→[^\\n]*$");

    /** 見 {@link #carriesPlayerData}：折行處連同後面補上的符號，併回一個空白。 */
    private static final Pattern UNWRAP =
            Pattern.compile("[ \\t]*\\n(?:[ \\t]*\\{#}[ \\t]*)*");

    /** 座標，例如 {@code [-781, 89, -5563]}。 */
    private static final Pattern COORDS =
            Pattern.compile("\\[-?\\d+,\\s*-?\\d+,\\s*-?\\d+]");

    /**
     * 玩家攤位的名牌，例如 {@code PoorChaCha's Shop}。
     *
     * <p>攤位名牌<b>整段都是玩家內容</b>：名稱是玩家 ID，下面那行是玩家自己
     * 打的招牌字（實際收到的有中文、有梗圖字串）。這種東西既不該進語料，
     * 也不該出現在別人的 captured.json 裡。
     *
     * <p>比對整段而不是只看第一行，因為模組拿到的是<b>整塊名牌</b>——
     * 招牌字跟名稱在同一筆。
     */
    private static final Pattern PLAYER_SHOP =
            Pattern.compile("['’]s Shop(\\n|$)");

    /**
     * 隊伍搜尋裡的隊伍名與隊長，例如 {@code Oscar123's Party}、{@code Leader: LphMe}。
     *
     * <p>兩者都直接帶著玩家 ID。隊伍簡介本身也是玩家自己打的字，不該進共享語料——
     * 一次隊伍搜尋就收進 50 幾條。
     */
    private static final Pattern PARTY_OWNER =
            Pattern.compile("['’]s Party(\\n|$)|^Leader: ", Pattern.MULTILINE);

    /**
     * 公會清單的一列，例如 {@code - RabbitHouse [Maya]}、{@code - Death Star [LIVE]}。
     *
     * <h2>為什麼靠形狀擋得住，靠名字擋不住</h2>
     * 公會名是玩家自己取的，什麼樣子都有——{@code Blank}、{@code Death Star}、
     * {@code ChinaNumberOne}。{@link #looksAccountNamed} 那套「底線或駝峰」的判準
     * 對它們幾乎全部失效：實機開一次公會選單就漏了七個。
     *
     * <p>但<b>那一列的形狀</b>是固定的：可能有的破折號開頭，結尾是方括號包起來
     * 的二到四碼公會標籤。Wynncraft 的標籤長度就是這個範圍，而遊戲本身的介面
     * 文字不會長成「某某 [ABCD]」——所以擋形狀既準又不會誤傷。
     *
     * <p>不限定大寫：實機收到的標籤有 {@code [Maya]}、{@code [tmxt]}。
     *
     * <p>量過整份語料：53,274 條有譯文的條目裡只有兩條會被擋到
     * （{@code - Lootrunning Grandmaster [MAX]} 與 {@code - Raiding Grandmaster [MAX]}）。
     * 那兩條<b>早就翻好了</b>，而這道濾網只管收集不管翻譯，所以擋到也沒有損失。
     *
     * <h2>內容書的卡片類型要放行</h2>
     * 上面那句「遊戲本身的介面文字不會長成『某某 [ABCD]』」說得太滿了：
     * 內容書的洞窟卡標題正是 {@code The Barracks [Cave]}——名字加上四碼的
     * 類型，跟公會清單那一列一模一樣。
     *
     * <p>實機跑過一輪之後，{@code cards.json} 收到 531 張卡：任務、迷你任務、
     * 祕密發現、世界發現、首領祭壇全都在，唯獨 {@code [Cave]} 一張都沒有。
     * 那幾張卡明明滑過（{@code tooltip-partial-1..6} 全是洞窟），是被這一條
     * 整類擋掉的——{@code [Quest]} 五碼、{@code [Mini-Quest]} 十碼，都長到
     * 碰不著這個上限，只有洞窟剛好落在二到四碼裡面。
     *
     * <p>所以把遊戲自己的類型標籤挑掉。只列<b>四碼以內</b>的那幾個，
     * 再長的本來就不會走到這裡。公會如果剛好取了這幾個字當標籤會穿過去，
     * 但那是一個標籤的代價，比整類洞窟卡收不到小得多。
     */
    private static final Pattern GUILD_TAG = Pattern.compile(
            "^\\s*-?\\s*[^\\[\\]\\n]*\\S[^\\[\\]\\n]*"
                    + "\\[(?!(?:Cave|Raid)])[A-Za-z0-9]{2,4}]\\s*$",
            Pattern.MULTILINE);

    /**
     * 公會的領地全像投影與成員名牌上那一行 {@code < ... >}。
     *
     * <pre>
     *   YuChaYuan            ← 公會名，別人的資料
     *   &lt; Season {~} - Platinum &gt;
     * </pre>
     *
     * <p>公會名本身沒有形狀可以認（沒有底線、沒有駝峰，跟一般英文詞一樣），
     * {@code looksAccountNamed} 那條路擋不住；而 {@code GUILD_TAG} 要的是
     * 方括號裡的縮寫，這種寫法也沒有。於是整塊穿了過去——實測一份
     * captured.json 裡有三種（{@code < Radiant >}、{@code < Traders >}、
     * {@code < Season {~} - Platinum >}），全部掛在同一個公會名下面。
     *
     * <p>擋掉的是<b>整塊名牌</b>，連那一行本來可翻的「賽季」一起。這是刻意的：
     * 那一行永遠跟著公會名出現，收得到的話公會名也一定跟著進來。
     * 量過整份語料，有譯文的三萬條裡<b>一條都不會</b>被擋到。
     */
    /**
     * 這一整份 tooltip 是不是<b>組隊尋找</b>清單上的一張隊伍卡。
     *
     * <h2>為什麼要看整份而不是看那一行</h2>
     * 隊伍名是玩家自己打的字——{@code Batcave}、{@code Gikyu Boss Fight :(}、
     * {@code Scrapyard dxp totems}。這種字沒有形狀可以認，跟一般的介面標題
     * 長得一模一樣，{@link #looksAccountNamed} 與 {@link #GUILD_TAG} 兩條路
     * 都擋不住——實機開一次組隊清單就漏了三個進 captured.json。
     *
     * <p>但「這是一張隊伍卡」是<b>整份</b> tooltip 才看得出來的：卡片上一定有
     * 一行伺服器世界（{@code World: NA{~}}）。那一行是遊戲自己排的，玩家改不動。
     * 認出卡片之後，把<b>標題那一行</b>整個丟掉就行——底下的
     * 「這個隊伍所在的世界已滿」那些是正常的介面字，照收。
     *
     * <p>代價是遇到隊伍卡時少收一個標題。那個標題本來就不該收。
     */
    public static boolean isPartyCard(java.util.List<String> templates) {
        if (templates == null) {
            return false;
        }
        for (String line : templates) {
            if (line != null && PARTY_WORLD.matcher(line.strip()).matches()) {
                return true;
            }
        }
        return false;
    }

    /** 見 {@link #isPartyCard}：隊伍卡上那一行伺服器世界。 */
    private static final Pattern PARTY_WORLD =
            Pattern.compile("^World: [A-Z]{2}\\{~}$");

    private static final Pattern GUILD_HOLOGRAM =
            Pattern.compile("(?m)^\\s*<[^<>\\n]{1,40}>\\s*$");

    /**
     * 中日韓文字。
     *
     * <h2>為什麼「出現中文」就該擋</h2>
     * Wynncraft 的原文<b>全部是英文</b>。收集到的字串裡出現中文，只有兩種可能，
     * 兩種都不該進語料：
     *
     * <ul>
     *   <li><b>玩家自己打的字</b>——攤位招牌、名牌上的留言。那是別人的內容，
     *       不是遊戲文案。</li>
     *   <li><b>我們自己的譯文繞回來了</b>——就地取代模式下畫面上的 tooltip
     *       已經是中文，再收集一次就會把譯文當成原文記下來。實際收到的
     *       {@code captured.json} 裡就有「- 等級 {~} [...]」這種條目。</li>
     * </ul>
     */
    /**
     * 玩家自製物品的製作者署名，例如整整一行只有 {@code by eric18960}。
     *
     * <p>玩家做出來的裝備會在 lore 最後掛上做的人是誰。那一行永遠是別人的 ID，
     * 翻不了也不該進共享語料——實際收到的 {@code npc.json} 裡就混進了兩筆。
     *
     * <p>另一種寫法是<b>整句</b>掛在同一行、前面還帶著材質包符號：
     * {@code {#}Crafted by Bunnub}。先前只擋得住單獨一行的 {@code by X}，
     * 這種就漏了進來——實測一份 captured.json 裡有三筆不同的玩家 ID。
     */
    private static final Pattern CRAFTED_BY =
            Pattern.compile("(?m)^(?:\\{#\\})*\\s*(?:Crafted )?by \\S+\\s*$");

    /**
     * 立牌底下那一行署名，名字不只一個字：「Cats' Alleyway / by Late Night Cats」。
     *
     * <p>{@link #CRAFTED_BY} 只認一個字的名字，玩家或公會取的名字常常有空白，
     * 整塊就穿了過去——收件匣的檢查擋下過這一筆。跟 {@code tools/check-leaks.py}
     * 的 SIGN_SHAPES 用同一條規則：行首小寫 by、後面接大寫開頭。
     */
    private static final Pattern SIGNED_BY =
            Pattern.compile("(?m)^(?:\\{#\\})*\\s*by [A-Z]");

    /**
     * 經驗共享通知後面掛的那個人是誰，例如：
     *
     * <pre>
     *   [+120 Combat XP]
     *   [eric18960]
     * </pre>
     *
     * <p>只擋「後面緊跟著一行括號名字」的情況——單獨的經驗提示是漂浮字，
     * 那個要翻譯，不能一起擋掉。
     */
    private static final Pattern XP_SHARE_TARGET =
            Pattern.compile("Combat XP]\\s*\\n\\s*\\[");

    private static final Pattern CJK = Pattern.compile(
            "[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff\\uf900-\\ufaff]");

    private PlayerDataFilter() {}

    /** 這段訊息是否夾帶玩家資料、不該被記錄。 */
    public static boolean carriesPlayerData(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        // 聊天室會把長廣播折行，折點後面還補上符號：「… from their\n{#} crate!」。
        // 片語要跨過折行比對，不然整則開箱廣播連同玩家名一起穿過去——
        // 實機的 captured.json 裡就收進了兩筆。
        String unwrapped = UNWRAP.matcher(text).replaceAll(" ");
        for (String marker : MARKERS) {
            if (text.contains(marker) || unwrapped.contains(marker)) {
                return true;
            }
        }
        if (COORDS.matcher(text).find() || PLAYER_SHOP.matcher(text).find()
                || PARTY_OWNER.matcher(text).find()
                || GUILD_TAG.matcher(text).find()
                || RAID_DEATH.matcher(text).find()
                || CRAFTED_BY.matcher(text).find()
                || SIGNED_BY.matcher(text).find()
                || ACCOUNT_LEAD.matcher(text).find()
                || XP_SHARE_TARGET.matcher(text).find()
                || GUILD_HOLOGRAM.matcher(text).find()
                || PARTY_ROW.matcher(text).find()
                || TRADE_LINE.matcher(text).find()
                || THANK_SOMEONE.matcher(text).find()
                || CJK.matcher(text).find()) {
            return true;
        }
        // 帳號名<b>與暱稱</b>都要擋。暱稱是玩家自己取的，跟公會名一樣不能進
        // 共享語料——而它跟帳號名毫無關係，只比對帳號名等於沒擋。見 SelfNames。
        if (SelfNames.find(text) != null) {
            return true;
        }
        return mentionsOnlinePlayer(text);
    }

    /** 短到會誤中一般英文字的名字就不比對了，例如三個字母的 ID。 */
    private static final int MIN_ONLINE_NAME = 4;

    /**
     * 這段文字裡出現了<b>目前線上任何一個玩家</b>的名字嗎。
     *
     * <h2>為什麼需要這一條</h2>
     * 先前只擋得住<b>自己</b>的名字（{@code {u}} 佔位符也只認自己），別人的
     * 一律穿過去。實測一份貢獻者回傳的語料裡混進了這些名牌：
     *
     * <pre>
     *   Critar's Totem        Netzuko's Puppet       Netzuko's Effigy
     *   nunot's Totem         Noxy_OwO's Rubber Duck
     * </pre>
     *
     * 那些是隊友技能生成的實體，名字前綴是<b>施放者的帳號名</b>。
     *
     * <p>用「長得像不像帳號名」去猜是不管用的：{@code Netzuko}、{@code Critar}
     * 跟 {@code Orphion}、{@code Grook} 這些設定裡的名字在結構上分不出來。
     * 但遊戲自己知道誰在線上——照著那份名單比對，就精準得多：會出現在你附近
     * 名牌上的名字，本來就是同一個世界裡的人。
     *
     * <p>設定裡的名字剛好跟某個線上玩家同名時會誤擋。那是可以接受的——
     * 照這個類別的原則，漏掉一句可翻譯的字，代價遠小於把別人的 ID 寫進共享檔案。
     */
    private static boolean mentionsOnlinePlayer(String text) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getConnection() == null) {
                return false;
            }
            for (net.minecraft.client.multiplayer.PlayerInfo info
                    : mc.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().name();
                if (name != null && name.length() >= MIN_ONLINE_NAME
                        && text.contains(name)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            // 還沒連上線、或 API 換了：這只是額外一層，拿不到不影響上面的比對
        }
        return false;
    }

    /**
     * 這個<b>名牌</b>的名字看起來是玩家自己取的嗎。
     *
     * <h2>為什麼名牌要另外一條</h2>
     * {@link #mentionsOnlinePlayer} 擋得住「名字前綴是施放者」的召喚物，但擋不住
     * 玩家<b>自己命名</b>的寵物、坐騎與飾品——那些名字跟帳號名無關。同一份語料裡
     * 混進來的有：
     *
     * <pre>
     *   epoch    Morrowind    Stardew    Poro    HellRevenger
     *   NexusRolly Love       Catgirl ring        oily femboy choker
     * </pre>
     *
     * <p>Wynncraft 自己的名字一律是<b>標題大小寫</b>（{@code Grootslang Wyrmling}、
     * {@code Voracious Octiped}）。所以判準是「不符合標題大小寫」：開頭小寫、
     * 含數字或底線、或是字中間冒出大寫（{@code NexusRolly}、{@code HellRevenger}）。
     *
     * <p>只看第一行——後面幾行是血條與狀態，本來就不是名字。
     *
     * <p>這是<b>啟發式</b>的，會漏掉剛好符合標題大小寫的自訂名（{@code Morrowind}
     * 就是）。它補的是 {@link #mentionsOnlinePlayer} 之外那一截，不是取代它。
     */
    /**
     * 這一行是不是<b>單純一個帳號名</b>，例如 {@code PoorChaCha}、
     * {@code YuChaYuan [YCY]}、{@code - [106] YuanYoIn}。
     *
     * <h2>跟 {@link #looksPlayerNamed} 的分工</h2>
     * 那一支是給<b>名牌</b>用的，收得比較寬——連「開頭小寫」都算數，因為玩家的
     * 寵物、坐騎常取那種名字。介面文字不能用那麼寬的判準：介面裡滿是被折行折出來的
     * 小寫續行（「to confirm」「offering better chances」），整份語料量過去會誤擋
     * 62 條。
     *
     * <p>所以這裡只留<b>強訊號</b>：底線、或「小寫緊接大寫」的駝峰。再加一道
     * 「整行看起來像個名字」（三個詞以內、結尾不是句讀），把任務對話那種長句排除掉。
     * 量過整份語料：52,921 條有譯文的條目裡只有 1 條會被擋到。
     *
     * <h2>大寫必須是 ASCII</h2>
     * 地城鑰匙長 {@code UnderworldÀÀÀCrypt Key} 這樣——中間那幾個 {@code À} 是
     * 對齊字元。不限定 ASCII 的話「d 接 À」會被當成駝峰，把鑰匙全部擋掉。
     * Minecraft 的帳號名本來就只有 {@code [A-Za-z0-9_]}。
     *
     * <h2>擋掉的代價很小</h2>
     * 這道濾網只管<b>收集</b>，不管翻譯——擋掉的東西是「不會再進 captured.json」，
     * 已經翻好的照常顯示。所以偶爾誤擋一個 Wynntils 的統計名（{@code HprRaw Scale}）
     * 沒有損失。
     */
    public static boolean looksAccountNamed(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        String name = template.split("\\n", 2)[0]
                .replaceAll("\\{[#~pu]\\d?}", " ")
                .trim();
        if (name.length() < MIN_PLATE_NAME) {
            return false;
        }
        char tail = name.charAt(name.length() - 1);
        if (tail == '.' || tail == '!' || tail == '?' || tail == ',' || tail == ':') {
            return false;                          // 像句子，不像名字
        }
        String[] words = name.replaceAll("\\[[^\\]]*\\]?", " ").trim().split("\\s+");
        if (words.length > MAX_PLATE_WORDS) {
            return false;
        }
        for (String word : words) {
            if (word.indexOf('_') >= 0) {
                return true;
            }
            for (int i = 1; i < word.length(); i++) {
                char c = word.charAt(i);
                char prev = word.charAt(i - 1);
                if (c >= 'A' && c <= 'Z' && prev >= 'a' && prev <= 'z') {
                    return true;                   // PoorChaCha、YuChaYuan
                }
            }
        }
        return false;
    }

    /**
     * 這一行提到了<b>線上玩家</b>的名字嗎——比 {@link #mentionsOnlinePlayer} 寬的那一版。
     *
     * <h2>跟上面那一支的分工</h2>
     * 上面那支要求名字<b>原封不動</b>出現在文字裡，所以只擋得住完整的名字。
     * 這一支連截斷的、數字被抽掉的都擋，代價是設定裡的名字剛好撞到線上玩家時
     * 會誤擋——所以只用在<b>本來就會出現玩家名</b>的地方（計分板、漂浮名牌），
     * 不放進 {@link #carriesPlayerData} 那條通用的路。
     *
     * <h2>為什麼要問伺服器而不是看字形</h2>
     * 隊伍計分板那一欄長這樣：{@code - [||{~}||] PoorChaC [{~}]}。名字裡的數字
     * 先被抽成 {@code {~}}，名字本身又被欄寬截斷，於是「駝峰」「底線」那些字形
     * 判準全部落空——實機的 captured.json 裡一次收進 41 條隊友名字，出現次數
     * 還排在最前面。寵物名（{@code Tomzd{~}'s Bird}）同理。
     *
     * <p>名單是現成的：分頁列上就有這個世界的玩家。把名單的名字做<b>同樣的</b>
     * 數字遮罩再比，截斷的情況就用前綴對——{@code PoorChaC} 是
     * {@code PoorChaCha} 的前綴。
     *
     * <p>四個字元以上才比：再短就會誤擋一般的字（{@code Bob}、{@code Ice}）。
     * 拿不到名單時（測試、還沒進伺服器）回傳 {@code false}，交給其他判準。
     */
    public static boolean mentionsOnlinePlayerLoose(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        java.util.List<String> names = onlineNames();
        if (names.isEmpty()) {
            return false;
        }
        java.util.regex.Matcher m = WORDS.matcher(template);
        while (m.find()) {
            String token = m.group();
            if (token.length() < MIN_NAME_MATCH) {
                continue;
            }
            for (String name : names) {
                if (name.length() < MIN_NAME_MATCH) {
                    continue;
                }
                // 截斷的名字是名單那個的前綴；帶後綴的（'s Bird）反過來
                if (name.regionMatches(true, 0, token, 0, Math.min(name.length(), token.length()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 分頁列上的玩家名，數字換成 {@code {~}} 之後的樣子。 */
    private static java.util.List<String> onlineNames() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getConnection() == null) {
                return List.of();
            }
            java.util.List<String> out = new java.util.ArrayList<>();
            for (var entry : mc.getConnection().getListedOnlinePlayers()) {
                String name = entry.getProfile().name();
                if (name != null && !name.isBlank()) {
                    out.add(name.replaceAll("\\d+", ""));
                }
            }
            return out;
        } catch (Throwable t) {
            return List.of();                      // 名單拿不到就交給其他判準
        }
    }

    /** 見 {@link #mentionsOnlinePlayer}：名字至少要這麼長才拿去比。 */
    private static final int MIN_NAME_MATCH = 4;

    /** 一段連續的英數與底線，就是一個可能的名字。 */
    private static final Pattern WORDS = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public static boolean looksPlayerNamed(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        // 佔位符換成<b>空白</b>而不是直接刪掉。直接刪的話血條那種
        // 「{~}k{#}{~}k{#}{~}k」會黏成 kkk，看起來就像一個開頭小寫的名字。
        String name = template.split("\\n", 2)[0]
                .replaceAll("\\{[#~pu]\\d?}", " ")
                .trim();
        if (name.length() < MIN_PLATE_NAME) {
            return false;
        }
        for (String word : name.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (word.indexOf('_') >= 0) {
                return true;                       // 帳號名才有底線
            }
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (Character.isDigit(c)) {
                    return true;
                }
                // 小寫後面緊跟著大寫：NexusRolly、HellRevenger、OwO
                if (i > 0 && Character.isUpperCase(c)
                        && Character.isLowerCase(word.charAt(i - 1))) {
                    return true;
                }
            }
        }

        // 開頭小寫：epoch、oily femboy choker、nunot's Totem。
        //
        // 這一條只適用於<b>名字</b>，所以還要再兩道：
        //
        // <ul>
        //   <li>整段都是單字母的不算——那是血條的單位（k、m），不是名字</li>
        //   <li>太長或帶句讀的不算——那是多行提示被折出來的續行，
        //       像「on the Trade Market!」「untradable, and quest items」</li>
        // </ul>
        //
        // 等級標籤不算在字數裡：玩家名牌長「heal kitty [Lv 106]」這樣，
        // 把 [Lv 106] 算進去就會超過字數上限而漏掉。
        if (!Character.isLowerCase(name.charAt(0))) {
            return false;
        }
        char tail = name.charAt(name.length() - 1);
        if (tail == '.' || tail == '!' || tail == '?' || tail == ',' || tail == ':') {
            return false;                          // 像句子，不像名字
        }
        String[] words = name.replaceAll("\\[[^\\]]*\\]?", " ").trim().split("\\s+");
        boolean real = false;
        for (String word : words) {
            if (word.length() >= 2) {
                real = true;                       // 有一個真的字，不只是單位字母
                break;
            }
        }
        return real && words.length <= MAX_PLATE_WORDS;
    }

    /** 剝掉佔位符之後短於這個長度的，不是名字。 */
    private static final int MIN_PLATE_NAME = 3;

    /** 開頭小寫又超過這麼多個字的，比較像被折出來的句子而不是名字。 */
    private static final int MAX_PLATE_WORDS = 3;

    /**
     * 本機玩家名稱。取不到就回傳 null——這只是額外的一層防護，
     * 拿不到不影響上面的特徵比對。
     */
    private static String localPlayerName() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc == null || mc.getUser() == null ? null : mc.getUser().getName();
        } catch (Throwable t) {
            return null;   // 初始化階段可能還沒有 user，不值得為此中斷收集
        }
    }
}
