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
import net.minecraft.world.phys.Vec3;

/**
 * 青龙形态：跳跃提升5 + 速度3（30 秒），期间蓝金色粒子紧密环绕，
 * 摔落伤害减半（配合高跳落地）。
 */
public final class QingLongCard {
    private QingLongCard() {}

    public static boolean activate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,
            CardConfig.QINGLONG_DURATION_TICKS, CardConfig.QINGLONG_JUMP_AMPLIFIER));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,
            CardConfig.QINGLONG_DURATION_TICKS, CardConfig.QINGLONG_SPEED_AMPLIFIER));
        ActiveStates.setQinglong(player.getUUID(), ActiveStates.now() + CardConfig.QINGLONG_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.QINGLONG,
            ActiveStates.now() + CardConfig.QINGLONG_DURATION_TICKS);

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.LIGHT_BLUE, 70, 0.9, 0.35);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.YELLOW, 50, 0.8, 0.3);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_FLAP);
        CardFx.announce(player, "发动", Card.QINGLONG);
        return true;
    }
}
