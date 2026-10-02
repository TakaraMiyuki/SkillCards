package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 莫尔迪基安的臂章：临时扣除 80% 当前生命值，期间内获得等价的伤害吸收，持续 6 秒；
 * 效果结束后返还扣除的生命值。金色粒子发动、灰色粒子结束。
 */
public final class BizhangCard {
    private BizhangCard() {}

    public static final Identifier MAX_ABSORPTION_ID =
        Identifier.fromNamespaceAndPath("skillcards", "bizhang_max_absorption");

    public static boolean activate(ServerPlayer player) {
        float health = player.getHealth();
        float deducted = health * (float) CardConfig.BIZHANG_RATIO;
        if (deducted <= 0.5F) {
            CardFx.hint(player, "生命值过低，无法转化");
            return false;
        }
        player.setHealth(health - deducted);
        // 26.2 吸收值受 MAX_ABSORPTION 属性上限钳制（基础 0），必须先抬高上限
        AttributeInstance maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption != null) {
            maxAbsorption.removeModifier(MAX_ABSORPTION_ID);
            maxAbsorption.addTransientModifier(new AttributeModifier(MAX_ABSORPTION_ID,
                deducted, AttributeModifier.Operation.ADD_VALUE));
        }
        player.setAbsorptionAmount(deducted);
        ActiveStates.setBizhang(player.getUUID(), new ActiveStates.Bracer(deducted,
            ActiveStates.now() + CardConfig.BIZHANG_DURATION_TICKS));
        ActiveStates.scheduleEndHint(player.getUUID(), Card.BIZHANG,
            ActiveStates.now() + CardConfig.BIZHANG_DURATION_TICKS);

        ServerLevel level = player.level();
        CardFx.burst(level, CardFx.frontPos(player).x, CardFx.frontPos(player).y,
            CardFx.frontPos(player).z, CardFx.YELLOW, 70, 1.0, 0.35);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE);
        CardFx.announce(player, "发动", Card.BIZHANG);
        return true;
    }

    /** 6 秒结束：移除上限修饰符（吸收值随之归零）并返还扣除的生命值（由每刻扫描调用）。 */
    public static void restore(ServerPlayer player, float deducted) {
        AttributeInstance maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption != null) {
            maxAbsorption.removeModifier(MAX_ABSORPTION_ID);
        }
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + deducted));
        if (player.level() instanceof ServerLevel level) {
            CardFx.burst(level, CardFx.frontPos(player).x, CardFx.frontPos(player).y,
                CardFx.frontPos(player).z, CardFx.GRAY_SMOKE, 40, 0.8, 0.15);
        }
    }
}
