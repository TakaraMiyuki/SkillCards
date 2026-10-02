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
 * 哈斯卡之狂战：效果期间每次攻击命中损失 2 点生命值（不会致死），
 * 生命值越低强化越高——低于 80%/60%/40%/20% 逐档获得
 * 力量 1/1/2/3、抗性提升 无/无/1/2、攻击速度 +5%/+10%/+20%/+30%、生命恢复 1/1/2/3。
 * 持续 60 秒，红色与火焰粒子环绕躯干。
 */
public final class HuskarCard {
    private HuskarCard() {}

    public static final Identifier ATTACK_SPEED_ID =
        Identifier.fromNamespaceAndPath("skillcards", "huskar_attack_speed");

    /** 各档位：力量等级修正 / 抗性等级修正（-1 表示无）/ 生命恢复等级修正 / 攻速加成。 */
    private static final int[] STRENGTH_AMP = {0, 0, 1, 2};
    private static final int[] RESISTANCE_AMP = {-1, -1, 0, 1};
    private static final int[] REGENERATION_AMP = {0, 0, 1, 2};
    private static final double[] ATTACK_SPEED_BONUS = {0.05, 0.10, 0.20, 0.30};

    public static boolean activate(ServerPlayer player) {
        ActiveStates.setHuskar(player.getUUID(), ActiveStates.now() + CardConfig.HUSKAR_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.HUSKAR,
            ActiveStates.now() + CardConfig.HUSKAR_DURATION_TICKS);

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.RED, 70, 1.1, 0.4);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypesHolder.FLAME, 50, 0.9, 0.25);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN);
        CardFx.announce(player, "发动", Card.HUSKAR);
        return true;
    }

    /** 当前档位：0=无（≥80%），1/2/3 对应 <80%/<60%/<40%，4 档 <20% 时数组取第 4 位。 */
    public static int tier(ServerPlayer player) {
        double pct = player.getHealth() / player.getMaxHealth();
        if (pct < 0.20) return 4;
        if (pct < 0.40) return 3;
        if (pct < 0.60) return 2;
        if (pct < 0.80) return 1;
        return 0;
    }

    /** 每档位增益刷新（由每刻扫描周期调用）。 */
    public static void refreshTier(ServerPlayer player) {
        int tier = tier(player);
        if (tier >= 1) {
            int idx = tier - 1;
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,
                CardConfig.HUSKAR_SWEEP_INTERVAL_TICKS * 4, STRENGTH_AMP[idx], true, false));
            if (RESISTANCE_AMP[idx] >= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                    CardConfig.HUSKAR_SWEEP_INTERVAL_TICKS * 4, RESISTANCE_AMP[idx], true, false));
            }
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                CardConfig.HUSKAR_SWEEP_INTERVAL_TICKS * 4, REGENERATION_AMP[idx], true, false));
        }
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            double bonus = tier >= 1 ? ATTACK_SPEED_BONUS[tier - 1] : 0.0;
            attackSpeed.addOrUpdateTransientModifier(
                new AttributeModifier(ATTACK_SPEED_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** 攻击命中扣当前生命的 3%（由 LivingDamageEvent.Post 调用）。 */
    public static void payCost(ServerPlayer attacker) {
        float health = attacker.getHealth();
        if (health > CardConfig.HUSKAR_MIN_HEALTH) {
            float cost = Math.max(1.0F, health * (float) CardConfig.HUSKAR_ATTACK_COST_RATIO);
            attacker.setHealth(Math.max(CardConfig.HUSKAR_MIN_HEALTH, health - cost));
        }
        if (attacker.level() instanceof ServerLevel level) {
            level.sendParticles(CardFx.RED, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
                6, 0.3, 0.4, 0.3, 0.05);
        }
    }

    /** 效果结束：移除攻速修饰符（生命恢复/抗性等短时效果自然到期）。 */
    public static void expire(ServerPlayer player) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_ID);
        }
    }

    private static final class ParticleTypesHolder {
        static final net.minecraft.core.particles.SimpleParticleType FLAME =
            net.minecraft.core.particles.ParticleTypes.FLAME;
    }
}
