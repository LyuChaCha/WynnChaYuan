package com.wynnchayuan.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

// 用 MixinService 讀位元組：Class.forName 提前載入會套不到 mixin
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
                    + "，跳過接到它上面的那一塊（沒裝那個模組或它改版了；其餘功能照常）");
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
