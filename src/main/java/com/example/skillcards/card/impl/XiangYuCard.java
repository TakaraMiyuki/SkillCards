package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** 响玉：使用后自己获得生命恢复4，持续8秒；迸发微弱绿色粒子（强化：绿玉螺旋 + 白色玉尘）。 */
public final class XiangYuCard {
    private XiangYuCard() {}

    public static boolean activate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
            CardConfig.XIANGYU_DURATION_TICKS, CardConfig.XIANGYU_AMPLIFIER));
        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        // 绿玉迸发 + 白色玉尘
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.COMPOSTER, 80, 0.8, 0.35);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.HAPPY_VILLAGER, 30, 0.6, 0.25);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.END_ROD, 30, 0.6, 0.2);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.WHITE, 20, 0.5, 0.15);
        // 螺旋上升的绿玉环
        for (double h = 0.1; h <= 2.0; h += 0.35) {
            double r = Math.max(0.25, 0.95 - h * 0.32);
            CardFx.ring(level, front.x, front.y + h - 1.0, front.z, r, 10, ParticleTypes.COMPOSTER);
        }
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME);
        CardFx.announce(player, "发动", Card.XIANGYU);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.XIANGYU,
            ActiveStates.now() + CardConfig.XIANGYU_DURATION_TICKS);
        return true;
    }
}
