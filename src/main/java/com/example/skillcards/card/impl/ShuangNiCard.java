package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.compat.ManhuntHook;
import com.example.skillcards.registry.Card;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * 霜凪：冰封最近的一个猎人——缓慢4 + 挖掘疲劳3（10 秒），
 * 期间冻结血条拉满（陷入细雪的视觉效果）且每秒受到 0.5 点冻结伤害，
 * 周身呈现长方体浅蓝色冰晶轮廓。
 */
public final class ShuangNiCard {
    private ShuangNiCard() {}

    public static boolean activate(ServerPlayer player) {
        List<LivingEntity> targets = ManhuntHook.targetsAround(player, CardConfig.SHUANGNI_RADIUS);
        if (targets.isEmpty()) {
            CardFx.hint(player, "周围 " + (int) CardConfig.SHUANGNI_RADIUS + " 格内没有" + ManhuntHook.targetNoun());
            return false;
        }
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity target : targets) {
            double distSq = player.distanceToSqr(target);
            if (distSq < best) {
                best = distSq;
                nearest = target;
            }
        }
        nearest.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
            CardConfig.SHUANGNI_DURATION_TICKS, CardConfig.SHUANGNI_SLOWNESS_AMPLIFIER));
        nearest.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,
            CardConfig.SHUANGNI_DURATION_TICKS, CardConfig.SHUANGNI_FATIGUE_AMPLIFIER));
        ActiveStates.setFrost(nearest.getUUID(),
            new ActiveStates.FrostMark(ActiveStates.now() + CardConfig.SHUANGNI_DURATION_TICKS));

        ServerLevel level = player.level();
        CardFx.burst(level, nearest.getX(), nearest.getY() + 1.0, nearest.getZ(),
            CardFx.LIGHT_BLUE, 50, 0.7, 0.3);
        CardFx.burst(level, nearest.getX(), nearest.getY() + 1.0, nearest.getZ(),
            ParticleTypesHolder.SNOWFLAKE, 30, 0.6, 0.2);
        CardFx.sound(level, nearest.getX(), nearest.getY(), nearest.getZ(), SoundEvents.GLASS_PLACE);
        CardFx.announce(player, "发动", Card.SHUANGNI);
        return true;
    }

    private static final class ParticleTypesHolder {
        static final net.minecraft.core.particles.SimpleParticleType SNOWFLAKE =
            net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
    }
}
