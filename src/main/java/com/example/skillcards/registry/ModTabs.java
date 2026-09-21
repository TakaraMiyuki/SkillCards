package com.example.skillcards.registry;

import com.example.skillcards.SkillCardsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 创造模式物品栏："技能卡"标签页（按稀有度顺序排列全部 14 张卡）。
 */
public final class ModTabs {
    private ModTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkillCardsMod.MODID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.skillcards"))
        .icon(() -> new ItemStack(ModItems.XIANGYU.get()))
        .displayItems((parameters, output) ->
            ModItems.all().forEach(item -> output.accept(item.get())))
        .build());
}
