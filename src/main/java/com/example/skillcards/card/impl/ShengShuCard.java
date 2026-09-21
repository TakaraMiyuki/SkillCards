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

/**
 * 圣树化身：获得持久的恢复能力（生命恢复3，60秒），但期间内虚弱2（60秒）；
 * 持续环绕绿色+黄色+白色粒子。
 */
public final class ShengShuCard {
    private ShengShuCard() {}

    public static boolean activate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
            CardConfig.SHENGSHU_DURATION_TICKS, CardConfig.SHENGSHU_REGENERATION_AMPLIFIER));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
            CardConfig.SHENGSHU_DURATION_TICKS, CardConfig.SHENGSHU_WEAKNESS_AMPLIFIER));
        ActiveStates.setSacred(player.getUUID(), ActiveStates.now() + CardConfig.SHENGSHU_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.SHENGSHU,
            ActiveStates.now() + CardConfig.SHENGSHU_DURATION_TICKS);

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        CardFx.burst(level, front.x, front.y, front.z,
            ParticleTypes.COMPOSTER, 50, 0.9, 0.25);
        CardFx.burst(level, front.x, front.y, front.z,
            ParticleTypes.HAPPY_VILLAGER, 40, 0.9, 0.2);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE);
        CardFx.announce(player, "发动", Card.SHENGSHU);
        return true;
    }
}
