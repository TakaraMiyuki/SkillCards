package com.example.skillcards.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.fml.ModList;

/**
 * 与"飞翔 Flying"模组（flying_enchant）的软联动：海王之翼使用它注册的
 * {@code flying_enchant:flying} 状态效果（疾跑中按跳跃键向前上方突进）。
 * 模组未加载时返回 null，海王之翼提示无法发动且不进冷却。
 */
public final class FlyingHook {
    private FlyingHook() {}

    public static final Identifier EFFECT_ID = Identifier.fromNamespaceAndPath("flying_enchant", "flying");

    private static boolean checked;
    private static boolean loaded;

    public static boolean available() {
        if (!checked) {
            loaded = ModList.get().isLoaded("flying_enchant");
            checked = true;
        }
        return loaded;
    }

    /** 解析飞翔状态效果的 Holder；模组未加载或注册表查不到时返回 null。 */
    public static Holder<MobEffect> effect() {
        if (!available()) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.get(EFFECT_ID).orElse(null);
    }
}
