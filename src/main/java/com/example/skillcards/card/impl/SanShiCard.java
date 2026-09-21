package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.compat.ManhuntHook;
import com.example.skillcards.registry.Card;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 三世秘传：范围内每个目标各被随机偷走一组物品。
 * 猎人（manhunt 模式）偷整包（快捷栏/背包/副手/盔甲）；敌对生物（回退模式）偷手持与盔甲。
 * 偷到的物品进入自己背包（满了掉在脚边）。范围内没有目标不进入冷却。
 */
public final class SanShiCard {
    private SanShiCard() {}

    private record Slot(ItemStack stack, Runnable remove) {}

    public static boolean activate(ServerPlayer player) {
        List<LivingEntity> victims = ManhuntHook.targetsAround(player, CardConfig.SANSHI_RADIUS);
        if (victims.isEmpty()) {
            CardFx.hint(player, "周围 " + (int) CardConfig.SANSHI_RADIUS + " 格内没有" + ManhuntHook.targetNoun());
            return false;
        }
        RandomSource random = player.getRandom();
        int stolenTotal = 0;
        for (LivingEntity victim : victims) {
            ItemStack stolen = victim instanceof ServerPlayer serverVictim
                ? stealFromPlayer(player, serverVictim, random)
                : stealFromMob(player, victim, random);
            if (stolen == null) {
                continue;
            }
            stolenTotal++;
            if (victim instanceof ServerPlayer online) {
                online.sendSystemMessage(Component.literal(
                    "§5[三世秘传] §f你的 §e" + stolen.getHoverName().getString() + " §f被偷走了！"), true);
            }
        }
        if (stolenTotal == 0) {
            CardFx.hint(player, ManhuntHook.targetNoun() + "身上没有可偷的物品");
            return false;
        }
        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        // 紫色粒子（加强）+ 传送门星尘
        CardFx.burst(level, front.x, front.y, front.z, CardFx.PURPLE, 80, 1.0, 0.3);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.PORTAL, 50, 1.0, 0.25);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT);
        CardFx.announce(player, "发动", Card.SANSHI);
        return true;
    }

    /** 偷到的物品进自己背包（满了掉脚边）。 */
    private static ItemStack finishSteal(ServerPlayer player, ItemStack stack, Runnable remove) {
        ItemStack stolen = stack.copy();
        remove.run();
        if (!player.getInventory().add(stolen)) {
            player.drop(stolen, false);
        }
        return stolen;
    }

    /** 猎人玩家：主物品栏（快捷栏+背包）+ 副手 + 盔甲。 */
    private static ItemStack stealFromPlayer(ServerPlayer player, ServerPlayer victim, RandomSource random) {
        List<Slot> candidates = new ArrayList<>();
        var items = victim.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && !ManhuntHook.isCoreItem(stack)) {
                int index = i;
                candidates.add(new Slot(stack, () -> items.set(index, ItemStack.EMPTY)));
            }
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.OFFHAND, EquipmentSlot.FEET,
            EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack stack = victim.getItemBySlot(slot);
            if (!stack.isEmpty() && !ManhuntHook.isCoreItem(stack)) {
                candidates.add(new Slot(stack, () -> victim.setItemSlot(slot, ItemStack.EMPTY)));
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Slot chosen = candidates.get(random.nextInt(candidates.size()));
        return finishSteal(player, chosen.stack(), chosen.remove());
    }

    /** 敌对生物：主手 / 副手 / 盔甲（多数空手，无候选则跳过该目标）。 */
    private static ItemStack stealFromMob(ServerPlayer player, LivingEntity victim, RandomSource random) {
        List<Slot> candidates = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack stack = victim.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                candidates.add(new Slot(stack, () -> victim.setItemSlot(slot, ItemStack.EMPTY)));
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Slot chosen = candidates.get(random.nextInt(candidates.size()));
        return finishSteal(player, chosen.stack(), chosen.remove());
    }
}
