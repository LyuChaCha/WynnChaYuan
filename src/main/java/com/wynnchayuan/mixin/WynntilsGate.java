package com.wynnchayuan.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/**
 * 只有目標類別真的在的時候才套用 mixin。
 *
 * <h2>為什麼需要這一道</h2>
 * 這兩個 mixin 打的是<b>另一個模組</b>（Wynntils）的類別。目標類別不見時，
 * mixin 預設的反應是讓遊戲<b>開不起來</b>——對一個「翻譯輔助」來說那是
 * 最糟的失敗方式：玩家只是想看中文，結果連遊戲都進不去。
 *
 * <p>Wynntils 改版把類別搬家、或哪天 fabric.mod.json 的相依放寬，
 * 都會走到這條路。查得到就套、查不到就跳過，其餘功能照常。
 *
 * <p>查的方法是<b>讀位元組</b>（{@code MixinService} 的 bytecode provider），
 * 不是 {@code Class.forName}——後者會把目標類別提前載入，而提前載入的類別
 * <b>套不到 mixin</b>，等於自己把要修的東西弄壞。
 */
public final class WynntilsGate implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider()
                    .getClassNode(targetClassName.replace('.', '/'));
            return node != null;
        } catch (Throwable t) {
            System.out.println("[WynnChaYuan] 找不到 " + targetClassName
                    + "，跳過就地取代那一塊（其餘功能照常）");
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}
