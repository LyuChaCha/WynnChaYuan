package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Wynncraft 的位置字型 -&gt; 我們自己那一份同位置、但畫得出中日韓字的。
 *
 * <h2>為什麼要一個位置一份</h2>
 * 「畫在畫面的哪個高度」是烘進字型 {@code ascent} 的，不是用座標排的。
 * {@code /class} 畫面整個是<b>一行</b> action bar，十幾個 HUD 元件各自靠自己那
 * 一份字型的 ascent 被推到該去的位置；三個職業原型的名字與簡介各有一份
 *（ascent {@code -26 / -58 / -90} 與 {@code -35 / -67 / -99}），配錯就是整行
 * 跑到別的高度去。對應的 {@code shift_y = 7 - ascent}，字型檔是從資源包量出來產的。
 *
 * <h2>為什麼不能讓它退回預設字型</h2>
 * 那幾份 Wynncraft 點陣字型裡沒有中日韓字。譯文塞進去，Minecraft 會自己退回
 * 預設字型——字看得見，可是 ascent 一起換掉（那一份是 -90，預設是 7，差 97px），
 * 於是「神射手」孤零零掉到畫面左下角（使用者 2026-10-05 的截圖）。
 * 只有 Archer 與 Assassin 中（它們的原型名剛好跟
 * {@code gear-weapon.json} 的裝備名撞到才被翻），所以看起來像隨機。
 *
 * <h2>高度不是 ascent 給的，是 shader 搬的</h2>
 * {@code 7 - ascent} 只對提示列（{@code bottom_middle}，-48 → 55）成立。原型名
 * 與簡介的字型 ascent 是 -26／-58／-90 與 -35／-67／-99，照算會落在畫面底部，
 * 而 Wynncraft 畫出來在左側中間——差了整整 (W/2, H/2)。2026-10-05 讀資源包的
 * {@code shaders/include/text.glsl} 才看懂：{@code screenAnchor()} 讀<b>目前綁定的
 * 字型圖集</b>在 (0,0)／(6,8) 那個像素，顏色等於某個標記值就把整個頂點搬到
 * 九宮格的某一角（48 → CENTER_LEFT、49 → TOP_MIDDLE……）。那幾份字型裡的
 * {@code U+0001}（{@link #MARKER}）是一個 256 px 高的標記字，字串裡永遠排在
 * 文字前面，所以第一個被烘進圖集、落在 (0,0)，整張圖集都帶著標記色。
 *
 * <p>我們的 TTF 字烘在<b>自己那份字型的圖集</b>裡，沒有標記，所以不會被搬——
 * 譯文全落在 action bar 自己的高度（使用者 2026-10-05 的截圖）。解法不是改 shift，
 * 是讓 {@code U+0001} 也用我們的字型畫（它 {@code reference} 原字型，畫出來一樣），
 * 而且要排在譯文前面：我們圖集的 (0,0) 就也是標記了。見
 * {@code ActionBarListener#columnSwap} 與 {@code #column}。
 *
 * <p>配不到、或那一份沒隨 jar 出貨時回 {@code null}，呼叫端照原本的做法走。
 */
public final class PairedFont {

    private PairedFont() {
    }

    /** 定位標記字 U+0001；見類別說明。 */
    public static final String MARKER = String.valueOf((char) 1);

    /**
     * 讓標記字成為這一份字型<b>第一個</b>烘進圖集的字。
     *
     * <p>shader 讀的是圖集 (0,0)，所以標記必須比任何中文字先烘。畫的順序是對的
     * （標記片段排在譯文前面），但 {@code Font.width()} 也會烘字：
     * {@code ActionBarListener#owed} 量譯文寬度時，簡介的譯文已經帶著標記
     * （先量到標記，沒事），原型名的標記卻是另一個獨立片段、不在被量的那一段裡，
     * 於是中文先被烘到 (0,0)，標記排到後面——實機就是「簡介有搬、名字沒搬」
     * （2026-10-05）。每一段進來先量一次標記，一個字的寬度，便宜。
     *
     * <p>不快取「已經暖過」：資源重載（F3+T、切語言）會把圖集清掉。
     */
    public static void warm(FontDescription ours) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.font != null) {
                mc.font.width(net.minecraft.network.chat.Component.literal(MARKER)
                        .setStyle(Style.EMPTY.withFont(ours)));
            }
        } catch (Throwable t) {
            // 量不到就算了，畫的順序本來就對
        }
    }

    /** {@code hud/selector/default/center_left/<0-2>/<display_name|description>} */
    private static final java.util.regex.Pattern SELECTOR_SLOT =
            java.util.regex.Pattern.compile(
                    "hud/selector/default/center_left/([0-2])/"
                            + "(display_name|description)\\b");

    /** 這一段原本的字型對應到我們哪一份；只有檔名，沒有命名空間。 */
    public static String slot(String font) {
        if (font == null) {
            return null;
        }
        if (font.contains("hud/selector/default/bottom_middle")) {
            return "selector_bottom";
        }
        java.util.regex.Matcher m = SELECTOR_SLOT.matcher(font);
        if (!m.find()) {
            return null;
        }
        return ("display_name".equals(m.group(2)) ? "selector_name_"
                                                  : "selector_desc_") + m.group(1);
    }

    /** 那一份字型檔真的在 jar 裡嗎。問一次就記起來，action bar 每幀都走。 */
    private static final java.util.Map<String, Boolean> FONTS =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean shipped(String lang, String name) {
        return FONTS.computeIfAbsent(lang + "/" + name, key -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return false;             // 測試環境沒有資源管理員，維持原樣
            }
            Identifier id = Identifier.fromNamespaceAndPath(
                    WynnChaYuan.MOD_ID, "font/actionbar/" + key + ".json");
            return mc.getResourceManager().getResource(id).isPresent();
        });
    }

    /**
     * 這一段該改用我們的哪一份字型。
     *
     * @return 對應的字型；這一段不在我們顧的位置上、或那一份沒出貨時回 {@code null}
     */
    public static FontDescription forStyle(Style style) {
        if (style == null) {
            return null;
        }
        String name = slot(nameOf(style));
        if (name == null) {
            return null;
        }
        String lang = WynnChaYuan.language();
        if (!shipped(lang, name)) {
            return null;
        }
        return new FontDescription.Resource(Identifier.fromNamespaceAndPath(
                WynnChaYuan.MOD_ID, "actionbar/" + lang + "/" + name));
    }

    /**
     * 這一段是不是 {@code /class} 選單那種<b>絕對定位</b>的 HUD 元件。
     *
     * <h2>為什麼這裡不能翻</h2>
     * 那個畫面的每一格長成 {@code [跳 +36] \u0001 文字 [退回 -97]}，退回的大小
     * 是 Wynncraft 照<b>英文</b>的寬度烘好的，整格的淨寬是固定值。文字一變短，
     * 這一格的淨寬就變了，後面所有元件跟著走位。
     *
     * <p>實機的證據很乾淨：warrior／mage／shaman 一個字都沒被翻，版面完美；
     * archer 與 assassin 各有一個原型名（{@code Sharpshooter}、{@code Acrobat}）
     * 剛好跟 {@code gear-weapon.json} 的<b>裝備名</b>撞到而被翻掉，那兩個職業
     * 就散掉。使用者最早回報的「有時候 class 的內容會跑掉」就是這個，
     * 跟任何新增的語料無關（2026-10-05）。
     *
     * <h2>試過、而且都失敗的做法</h2>
     * 不是寬度的問題，也不是字型的問題——下面四種組合實機都試過：
     *
     * <ul>
     *   <li>全英文、不動 —— <b>完美</b></li>
     *   <li>有翻譯 + 不補償 + 預設字型 —— 壞</li>
     *   <li>有翻譯 + 補償推給下一格的跳 + 配對字型 —— 壞</li>
     *   <li>有翻譯 + 補償補在自己的退回 + 配對字型 —— 壞</li>
     * </ul>
     *
     * <p>壞掉的樣子一致：右邊的角色卡整張不見、文字全部落到畫面底部。
     * 共同因素只剩「那幾格的文字被動過」。在弄懂真正的定位機制之前不要
     * 再試了，翻了比沒翻更糟。
     *
     * <h2>最容易看漏的那一格</h2>
     * 畫面正上方的 {@code Create a Character} 在 {@code top_middle}，<b>不在
     * center_left 底下</b>。守門一開始只擋 {@code center_left/}，它照翻不誤，
     * 一翻就把後面整串推掉 54px（寬 90 -&gt; 36），而使用者看到的症狀跟
     * 原型名被翻時一模一樣——我因此盯錯對象好幾輪。範圍要是整個
     * {@code hud/selector/default/}，不是其中某幾格。
     */
    public static boolean absolutelyPositioned(Style style) {
        // bottom_middle 一度以為可以翻——它看起來是流動置中的提示列。但實機
        // 證明不行：三欄翻完、寬度補償就地補好、整行總寬也守住（差 0 px），
        // 排在提示列<b>後面</b>的元件仍然會被推開幾十像素（使用者 2026-10-05
        // 的截圖，Fallen 那一列的圖示壓在名字上，另外兩列正常——因為它們的
        // 圖示排在提示列前面）。
        //
        // 量到的寬度跟實際的繪製推進量之間還有落差，而那不是再多試幾輪就能
        // 收斂的。整個 selector 一律不翻。
        return isSelector(style)
                && !nameOf(style).contains("hud/selector/default/bottom_middle");
    }

    /**
     * 這一段是不是 {@code /class}／角色選擇那個 HUD 的任何一格（<b>含</b>提示列）。
     *
     * <p>這種行不能交給 {@code LineTranslator}：每一格的跳／退回位移會被當成
     * 可調的欄距重排（負的夾成 0、同一段只留最後一個），整行 39 px 變 1714 px、
     * 全部畫到畫面外（2026-10-05 的 {@code actionbar-columns-2.txt} 譯文側）。
     * 它們只走 {@code ActionBarListener#columnSwap}。
     */
    public static boolean isSelector(Style style) {
        return nameOf(style).contains("hud/selector/default/");
    }

    /** 這一段現在用的是不是我們自己備的那幾份之一。 */
    public static boolean isOurs(Style style) {
        return nameOf(style).contains(WynnChaYuan.MOD_ID + ":actionbar/");
    }

    public static String nameOf(Style style) {
        return style == null || style.getFont() == null
                ? "" : String.valueOf(style.getFont());
    }
}
