package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.compat.ManhuntHook;
import com.example.skillcards.registry.Card;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 绽放：弹开周围的目标，自己获得生命提升5（10秒）、伤害吸收5（30秒）；粉色+绿色粒子从玩家身上炸开。 */
public final class ZhanFangCard {
    private ZhanFangCard() {}

    public static boolean activate(ServerPlayer player) {
        for (LivingEntity target : ManhuntHook.targetsAround(player, CardConfig.ZHANFANG_KNOCKBACK_RADIUS)) {
            Vec3 away = target.position().subtract(player.position());
            double horizontal = Math.sqrt(away.x * away.x + away.z * away.z);
            double scale = horizontal < 0.01
                ? CardConfig.ZHANFANG_KNOCKBACK_HORIZONTAL
                : CardConfig.ZHANFANG_KNOCKBACK_HORIZONTAL / horizontal;
            target.push(away.x * scale, CardConfig.ZHANFANG_KNOCKBACK_VERTICAL, away.z * scale);
            target.hurtMarked = true; // 立即同步速度到客户端
        }
        player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST,
            CardConfig.ZHANFANG_HEALTH_BOOST_DURATION_TICKS, CardConfig.ZHANFANG_HEALTH_BOOST_AMPLIFIER));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
            CardConfig.ZHANFANG_ABSORPTION_DURATION_TICKS, CardConfig.ZHANFANG_ABSORPTION_AMPLIFIER));

        ServerLevel level = player.level();
        // 粉色（樱花瓣）+ 绿色（骨粉）粒子从玩家身上炸开：胸口高度为爆心，球状向外迸发
        double bx = player.getX(), by = player.getY() + 0.9, bz = player.getZ();
        CardFx.burst(level, bx, by, bz, ParticleTypes.CHERRY_LEAVES, 90, 1.6, 0.55);
        CardFx.burst(level, bx, by, bz, ParticleTypes.COMPOSTER, 90, 1.6, 0.55);
        CardFx.burst(level, bx, by, bz, ParticleTypes.END_ROD, 30, 1.2, 0.4);
        // 由身体向外扩张的能量环
        for (double radius : new double[] {0.8, 1.6, 2.6, 3.8}) {
            CardFx.ring(level, bx, by, bz, radius, 22, ParticleTypes.CHERRY_LEAVES);
            CardFx.ring(level, bx, by, bz, radius, 22, ParticleTypes.COMPOSTER);
        }
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST);
        CardFx.announce(player, "发动", Card.ZHANFANG);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.ZHANFANG,
            ActiveStates.now() + CardConfig.ZHANFANG_ABSORPTION_DURATION_TICKS);
        return true;
    }
}
