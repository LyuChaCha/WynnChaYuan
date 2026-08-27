package com.wynnchayuan.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 對話字型的位移值必須跟 Wynncraft 的 ascent 對得上。
 *
 * <h2>為什麼要釘住</h2>
 * Wynncraft 把「畫在第幾行」烘進了字型的 {@code ascent}。我們自己那六份字型
 * 靠 {@code shift} 把中日韓補回同一個高度，位移量正好是 {@code -(ascent - 7)}
 * ——7 是 {@code minecraft:default} 的 ascent。
 *
 * <p>這些數字來自伺服器資源包（見玩家回報的 {@code font-dump.txt}），
 * 不是算出來的。有人手改 JSON 卻改錯一個，畫面上只會看到「字有點偏」，
 * 追起來又是好幾輪。所以在這裡對死。
 *
 * <p>方向也一併釘住：MC 的 ttf provider 把 shift 交給 FreeType 時取了負號
 * （{@code deltaY = -shiftY}），而 FreeType 的 +y 朝上，所以正數往下。
 */
public final class DialogueFontTest {

    /** {@code minecraft:default} 的 ascent。位移都是相對它算的。 */
    private static final int DEFAULT_ASCENT = 7;

    /** 每一份字型對應的 Wynncraft ascent，抄自 font-dump.txt。 */
    private static final Map<String, Integer> ASCENT = new LinkedHashMap<>();

    static {
        ASCENT.put("body_0", 34);
        ASCENT.put("body_1", 22);
        ASCENT.put("body_2", 10);
        ASCENT.put("body_3", -2);
        ASCENT.put("nameplate", 50);
        ASCENT.put("control", -38);
    }

    private static final Path DIR = Path.of(
            "src/main/resources/assets/wynnchayuan/font/dialogue");

    private static int failures = 0;

    private DialogueFontTest() {}

    public static void main(String[] args) throws IOException {
        System.out.println("=== 對話字型 ===");

        Path ttf = DIR.resolveSibling("cubic_11.ttf");
        check("中日韓字型檔在", Files.isRegularFile(ttf));
        check("授權檔在（SIL OFL 隨模組散布必須附上）",
                Files.isRegularFile(DIR.resolveSibling("OFL.txt")));

        for (Map.Entry<String, Integer> row : ASCENT.entrySet()) {
            String name = row.getKey();
            Path file = DIR.resolve(name + ".json");
            if (!Files.isRegularFile(file)) {
                check(name + ".json 存在", false);
                continue;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8)
                    .replaceAll("\\s+", "");

            check(name + " 先參照 Wynncraft 自己的字型（ASCII 外觀與高度不能變）",
                    json.contains("\"type\":\"reference\""));
            check(name + " 有中日韓的 ttf",
                    json.contains("cubic_11.ttf"));
            // reference 必須排在 ttf 前面：MC 是前面的 provider 優先，
            // 顛倒過來 ASCII 就會被 Cubic 11 接手，外觀跟原文對不上
            check(name + " reference 排在 ttf 前面",
                    json.indexOf("\"reference\"") < json.indexOf("\"ttf\""));

            int want = -(row.getValue() - DEFAULT_ASCENT);
            check(name + " 位移是 " + want + "（ascent " + row.getValue() + " − 7）",
                    json.contains("\"shift\":[0," + want + "]"));
        }

        System.out.println(failures == 0
                ? "DialogueFont: 全部通過"
                : "DialogueFont: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
