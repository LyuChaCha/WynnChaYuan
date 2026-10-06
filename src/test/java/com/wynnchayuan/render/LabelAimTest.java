package com.wynnchayuan.render;

import com.wynntils.core.text.StyledText;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 「注視時顯示」的小框要瞄<b>字畫在哪</b>，不是實體站在哪。
 *
 * <p>issue #1047 的第二輪：0.2.8_1 之後盔甲座那條路會遵守設定了，但玩家回報
 * 「要低頭看到字<b>下方</b>才跳得出小框，而最上面那一行怎麼看都跳不出來」。
 *
 * <p>原因是瞄準用的是 {@code entity.getBoundingBox()}。浮空字是**隱形盔甲座**，
 * 碰撞箱幾乎是個點，而 Minecraft 把名字畫在碰撞箱頂端<b>再往上 0.5 格</b>；
 * 多行的名字還會一路往上長。於是準心對著字的時候，射線打的是字底下一兩格的空氣。
 *
 * <p>這一支測的就是那段幾何：四行的浮空字，它的可瞄區塊要**蓋到最上面那一行**。
 */
public final class LabelAimTest {

    private static int failures = 0;

    public static void main(String[] args) {
        // 隱形盔甲座：碰撞箱幾乎是個點
        Vec3 at = new Vec3(100, 70, 200);
        AABB marker = new AABB(at.x, at.y, at.z, at.x, at.y, at.z);

        AABB one = LookAtTranslator.labelBox(marker, at, 1);
        check("一行：區塊往上蓋到字的位置（頂端 " + one.maxY + "）",
              one.maxY > 70.5);
        check("一行：實體本身還是瞄得到（底 " + one.minY + "）", one.minY <= 70.0);

        AABB four = LookAtTranslator.labelBox(marker, at, 4);
        check("四行：區塊比一行高（實際 " + four.maxY + " vs " + one.maxY + "）",
              four.maxY > one.maxY);

        // ★ 真正要守的那一條：最上面那一行在不在可瞄範圍裡。
        // 字從 70.5 開始往上長，四行就到 71.6 左右——舊的寫法（只有碰撞箱）
        // 頂多到 70.5，對著最上面那一行看永遠打不中。
        double topLine = 70.0 + 0.5 + 3 * 0.28;
        check("★ 四行的最上面那一行瞄得到（頂端 " + four.maxY + "、該行在 "
                      + topLine + "）",
              four.maxY >= topLine);

        AABB old = marker.inflate(0.5);
        check("★ 舊的寫法確實蓋不到最上面那一行（舊頂端 " + old.maxY + "）",
              old.maxY < topLine);

        // 中心點也要跟著抬：那是「離視線多遠」與「有沒有被牆擋住」用的點
        Vec3 centre = LookAtTranslator.labelCentre(marker, at, 4);
        check("中心點在字那一塊裡（實際 " + centre.y + "）",
              centre.y > 70.5 && centre.y < four.maxY);
        check("中心點的水平位置就是實體的",
              Math.abs(centre.x - at.x) < 1e-9 && Math.abs(centre.z - at.z) < 1e-9);

        // 有身體的 NPC：區塊要把身體也包進去，不然站在面前反而瞄不到
        AABB body = new AABB(99.7, 70, 199.7, 100.3, 71.8, 200.3);
        AABB npc = LookAtTranslator.labelBox(body, at, 1);
        check("有身體的 NPC：身體也在可瞄範圍裡（底 " + npc.minY + "）",
              npc.minY <= 70.0 && npc.maxY > 71.8);

        // 行數是從名字裡的換行數來的
        check("一行的名字算一行",
              LookAtTranslator.lineCount(StyledText.fromComponent(
                      Component.literal("Temporal Anchor"))) == 1);
        check("四行的名字算四行",
              LookAtTranslator.lineCount(StyledText.fromComponent(
                      Component.literal("PUZZLE!\nDestabilize the\nResearch\nLab!"))) == 4);
        check("沒有名字也算一行", LookAtTranslator.lineCount(null) == 1);

        System.out.println(failures == 0 ? "小框的瞄準範圍：全部通過"
                                         : "小框的瞄準範圍：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
