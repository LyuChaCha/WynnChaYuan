package com.wynnchayuan.render;

import com.wynntils.core.text.StyledText;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LabelAimTest {

    public static void main(String[] args) {
        Vec3 at = new Vec3(100, 70, 200);
        AABB marker = new AABB(at.x, at.y, at.z, at.x, at.y, at.z);

        AABB one = LookAtTranslator.labelBox(marker, at, 1);
        check("一行：區塊往上蓋到字的位置（頂端 " + one.maxY + "）",
              one.maxY > 70.5);
        check("一行：實體本身還是瞄得到（底 " + one.minY + "）", one.minY <= 70.0);

        AABB four = LookAtTranslator.labelBox(marker, at, 4);
        check("四行：區塊比一行高（實際 " + four.maxY + " vs " + one.maxY + "）",
              four.maxY > one.maxY);

        double topLine = 70.0 + 0.5 + 3 * 0.28;
        check("★ 四行的最上面那一行瞄得到（頂端 " + four.maxY + "、該行在 "
                      + topLine + "）",
              four.maxY >= topLine);

        AABB old = marker.inflate(0.5);
        check("★ 舊的寫法確實蓋不到最上面那一行（舊頂端 " + old.maxY + "）",
              old.maxY < topLine);

        Vec3 centre = LookAtTranslator.labelCentre(marker, at, 4);
        check("中心點在字那一塊裡（實際 " + centre.y + "）",
              centre.y > 70.5 && centre.y < four.maxY);
        check("中心點的水平位置就是實體的",
              Math.abs(centre.x - at.x) < 1e-9 && Math.abs(centre.z - at.z) < 1e-9);

        AABB body = new AABB(99.7, 70, 199.7, 100.3, 71.8, 200.3);
        AABB npc = LookAtTranslator.labelBox(body, at, 1);
        check("有身體的 NPC：身體也在可瞄範圍裡（底 " + npc.minY + "）",
              npc.minY <= 70.0 && npc.maxY > 71.8);

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

}
