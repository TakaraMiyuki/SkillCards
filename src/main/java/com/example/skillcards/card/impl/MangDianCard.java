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
 * 盲点：干扰猎人的追踪指南（60 秒内罗盘持续乱指），自己获得隐身（30 秒）；
 * 迸发微弱白色粒子。罗盘干扰通过 Manhunt 的 CompassManager.targetDecorator 钩子实现；
 * 无 manhunt 时仅隐身。
 */
public final class MangDianCard {
    private MangDianCard() {}

    public static boolean activate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
            CardConfig.MANGDIAN_INVISIBILITY_DURATION_TICKS, 0));
        ActiveStates.setJam(player.getUUID(), ActiveStates.now() + CardConfig.MANGDIAN_JAM_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.MANGDIAN,
            ActiveStates.now() + CardConfig.MANGDIAN_JAM_DURATION_TICKS);

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        // 白色粒子（加强）+ 隐迹云雾
        CardFx.burst(level, front.x, front.y, front.z, CardFx.WHITE, 60, 0.9, 0.15);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.CLOUD, 30, 0.7, 0.08);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE);
        CardFx.announce(player, "发动", Card.MANGDIAN);
        return true;
    }
}
