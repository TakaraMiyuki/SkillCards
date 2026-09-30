package com.example.skillcards.card.impl;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 法轮（被动）：被一种负面药水效果持续影响超过 30 秒后，不再受到该效果影响
 * （同种效果不同等级视为同类；见 CardEvents#onEffectApplicable 与 tickFalun）。
 */
public final class FaLunCard {
    private FaLunCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }

    /** 适应达成时的头顶无规律音波粒子。 */
    public static void adaptFx(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            for (int i = 0; i < 6; i++) {
                double ox = (player.getRandom().nextDouble() - 0.5) * 1.2;
                double oy = player.getEyeY() + 0.4 + player.getRandom().nextDouble() * 0.4;
                double oz = (player.getRandom().nextDouble() - 0.5) * 1.2;
                level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX() + ox, oy, player.getZ() + oz,
                    1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    public static boolean isHarmful(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return effect.value().getCategory() == MobEffectCategory.HARMFUL;
    }
}
