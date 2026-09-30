package com.example.skillcards.compat;

import com.example.skillcards.item.SkillCardItem;
import com.example.skillcards.registry.Card;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashSet;
import java.util.Set;

/**
 * 与 Curios 的联动：被动卡可放入饰品栏（任意饰品槽，含 manhunt 提供的 skill_passive 槽）。
 * 只有在 curios 已加载时才会触碰 curios 类，保证本模组可脱离 curios 独立加载。
 */
public final class CuriosBridge {
    private CuriosBridge() {}

    private static boolean checked;
    private static boolean loaded;

    public static boolean available() {
        if (!checked) {
            loaded = ModList.get().isLoaded("curios");
            checked = true;
        }
        return loaded;
    }

    /** 收集玩家饰品栏中的全部被动卡（追加到 found 集合）。 */
    public static void collectPassives(ServerPlayer player, Set<Card> found) {
        if (!available()) {
            return;
        }
        var handlerOpt = CuriosApi.getCuriosInventory(player);
        if (handlerOpt.isEmpty()) {
            return;
        }
        for (var entry : handlerOpt.get().getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.getItem() instanceof SkillCardItem cardItem && cardItem.card().isPassive()) {
                    found.add(cardItem.card());
                }
            }
        }
    }

    /** 调试用/统计：玩家饰品栏中的全部技能卡物品 id。 */
    public static Set<String> equippedSkillCardIds(ServerPlayer player) {
        Set<String> ids = new HashSet<>();
        if (!available()) {
            return ids;
        }
        var handlerOpt = CuriosApi.getCuriosInventory(player);
        if (handlerOpt.isEmpty()) {
            return ids;
        }
        for (var entry : handlerOpt.get().getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.getItem() instanceof SkillCardItem) {
                    ids.add(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem()).toString());
                }
            }
        }
        return ids;
    }
}
