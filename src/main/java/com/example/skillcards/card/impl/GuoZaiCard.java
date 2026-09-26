package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
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

/**
 * 过载运转：速度3 + 攻击速度 +30%（30 秒，蒸汽环绕），
 * 结束后陷入"透支"：缓慢1（10 秒）。
 */
public final class GuoZaiCard {
    private GuoZaiCard() {}

    public static final Identifier ATTACK_SPEED_ID =
        Identifier.fromNamespaceAndPath("skillcards", "guozai_attack_speed");

    public static boolean activate(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,
            CardConfig.GUOZAI_DURATION_TICKS, CardConfig.GUOZAI_SPEED_AMPLIFIER));
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_ID);
            attackSpeed.addTransientModifier(new AttributeModifier(ATTACK_SPEED_ID,
                CardConfig.GUOZAI_ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        ActiveStates.setGuozai(player.getUUID(), ActiveStates.now() + CardConfig.GUOZAI_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.GUOZAI,
            ActiveStates.now() + CardConfig.GUOZAI_DURATION_TICKS);

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.YELLOW, 60, 0.9, 0.3);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypesHolder.CLOUD, 40, 0.7, 0.15);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_CHARGE);
        CardFx.announce(player, "发动", Card.GUOZAI);
        return true;
    }

    /** 30 秒结束：移除攻速加成并施加透支缓慢（结束提示由 ActiveStates 统一播报）。 */
    public static void expire(ServerPlayer player) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_ID);
        }
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
            CardConfig.GUOZAI_AFTER_SLOWNESS_TICKS, 0));
        if (player.level() instanceof ServerLevel level) {
            Vec3 front = CardFx.frontPos(player);
            CardFx.burst(level, front.x, front.y, front.z, ParticleTypesHolder.CLOUD, 30, 0.6, 0.08);
        }
    }

    private static final class ParticleTypesHolder {
        static final net.minecraft.core.particles.SimpleParticleType CLOUD =
            net.minecraft.core.particles.ParticleTypes.CLOUD;
    }
}
