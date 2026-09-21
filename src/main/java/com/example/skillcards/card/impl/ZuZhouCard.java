package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** 诅咒化身：获得力量4（60秒）；期间内最大生命值减少50%；持续环绕诅咒般的火焰粒子。 */
public final class ZuZhouCard {
    private ZuZhouCard() {}

    public static final Identifier CURSE_HEALTH_ID =
        Identifier.fromNamespaceAndPath("skillcards", "zuzhou_health");

    public static boolean activate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,
            CardConfig.ZUZHOU_DURATION_TICKS, CardConfig.ZUZHOU_STRENGTH_AMPLIFIER));
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(CURSE_HEALTH_ID);
            health.addTransientModifier(new AttributeModifier(CURSE_HEALTH_ID,
                CardConfig.ZUZHOU_HEALTH_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        ActiveStates.setCurse(player.getUUID(), ActiveStates.now() + CardConfig.ZUZHOU_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.ZUZHOU,
            ActiveStates.now() + CardConfig.ZUZHOU_DURATION_TICKS);

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        CardFx.burst(level, front.x, front.y, front.z,
            ParticleTypes.SOUL_FIRE_FLAME, 50, 0.9, 0.25);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN);
        CardFx.announce(player, "发动", Card.ZUZHOU);
        return true;
    }

    /** 到期处理：解除最大生命值减半（结束提示由 ActiveStates 统一播报）。 */
    public static void expire(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(CURSE_HEALTH_ID);
        }
    }
}
