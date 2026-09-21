package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** 虹星：向目视方向发动一次冲刺（约7格），自己获得速度4（5秒）；蓝色+粉色粒子。 */
public final class HongXingCard {
    private HongXingCard() {}

    public static boolean activate(ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.scale(CardConfig.HONGXING_DASH_POWER));
        player.applyPostImpulseGraceTime(10);
        player.resetFallDistance();
        player.setIgnoreFallDamageFromCurrentImpulse(true, player.position());
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        player.addEffect(new MobEffectInstance(MobEffects.SPEED,
            CardConfig.HONGXING_SPEED_DURATION_TICKS, CardConfig.HONGXING_SPEED_AMPLIFIER));

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        // 蓝+粉粒子（沿冲刺方向拖尾迸发）
        CardFx.burst(level, front.x, front.y, front.z, CardFx.LIGHT_BLUE, 45, 0.8, 0.35);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.PINK, 45, 0.8, 0.35);
        for (double d = 0.8; d <= 3.2; d += 0.8) {
            CardFx.ring(level, front.x - look.x * d, front.y, front.z - look.z * d, 0.6, 8, ParticleTypes.END_ROD);
        }
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST);
        CardFx.announce(player, "发动", Card.HONGXING);
        return true;
    }
}
