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

import java.util.List;

/**
 * 纱幕：在自己周围释放黑雾，给予周围20格的目标缓慢2（2秒）、失明（10秒）；
 * 并在原地留下一团黑灰色的烟雾（半径6格、高4格，持续30秒）。
 */
public final class ShaMuCard {
    private ShaMuCard() {}

    public static boolean activate(ServerPlayer player) {
        List<LivingEntity> targets = ManhuntHook.targetsAround(player, CardConfig.SHAMU_RADIUS);
        if (targets.isEmpty()) {
            CardFx.hint(player, "周围 " + (int) CardConfig.SHAMU_RADIUS + " 格内没有" + ManhuntHook.targetNoun());
            return false;
        }
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                CardConfig.SHAMU_SLOWNESS_DURATION_TICKS, CardConfig.SHAMU_SLOWNESS_AMPLIFIER));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                CardConfig.SHAMU_BLINDNESS_DURATION_TICKS, 0));
        }
        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        // 黑灰烟雾扩散（加强：五层大烟环 + 大团烟雾）
        for (double radius : new double[] {1.5, 4.0, 8.0, 13.0, 18.0}) {
            CardFx.ring(level, front.x, front.y, front.z, radius,
                Math.max(12, (int) (radius * 2.2)), ParticleTypes.LARGE_SMOKE);
        }
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.LARGE_SMOKE, 60, 1.5, 0.12);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.SMOKE, 90, 1.8, 0.15);
        // 原地黑灰烟雾团：半径6格、高4格、持续30秒（由每刻扫描渲染）
        ActiveStates.addSmokeZone(player.level().dimension(),
            player.getX(), player.getY(), player.getZ(),
            ActiveStates.now() + CardConfig.SHAMU_SMOKE_DURATION_TICKS);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE);
        CardFx.announce(player, "发动", Card.SHAMU);
        return true;
    }
}
