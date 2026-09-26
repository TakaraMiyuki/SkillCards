package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** 钓鱼翁：大幅增强抗性（抗性提升4），但移动迟缓（缓慢3），持续10秒；浅蓝+白+灰粒子环绕。 */
public final class DiaoYuCard {
    private DiaoYuCard() {}

    public static boolean activate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
            CardConfig.DIAOYU_DURATION_TICKS, CardConfig.DIAOYU_RESISTANCE_AMPLIFIER));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
            CardConfig.DIAOYU_DURATION_TICKS, CardConfig.DIAOYU_SLOWNESS_AMPLIFIER));
        ActiveStates.setDiaoyu(player.getUUID(), ActiveStates.now() + CardConfig.DIAOYU_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.DIAOYU,
            ActiveStates.now() + CardConfig.DIAOYU_DURATION_TICKS);

        ServerLevel level = player.level();
        CardFx.burst(level, CardFx.frontPos(player).x, CardFx.frontPos(player).y,
            CardFx.frontPos(player).z, CardFx.LIGHT_BLUE, 40, 0.8, 0.2);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE);
        CardFx.announce(player, "发动", Card.DIAOYU);
        return true;
    }
}
