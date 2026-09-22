package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * 赤鳞之跃动：发动后快速损失 20 点生命值（分段掉血、保留最后 2 点，呈现连续受击感），
 * 获得 30 秒的速度 2 / 力量 2 / 夜视 / 急迫 3 / 跳跃提升 1；持续期间身上散发红色粒子。
 */
public final class ChiLinCard {
    private ChiLinCard() {}

    public static boolean activate(ServerPlayer player) {
        // 分段扣血：目标 = max(当前生命 - 20, 2)（扣至最后 2 点生命，不会致死）
        float health = player.getHealth();
        float target = Math.max(health - CardConfig.CHILIN_HEALTH_COST, CardConfig.CHILIN_MIN_HEALTH);
        if (target < health) {
            int chunks = net.minecraft.util.Mth.ceil((health - target) / CardConfig.CHILIN_DRAIN_CHUNK_HP);
            ActiveStates.scheduleCrimsonDrain(player.getUUID(), chunks, target);
        }

        // 30 秒强化：速度2 / 力量2 / 夜视 / 急迫3 / 跳跃提升1
        int dur = CardConfig.CHILIN_BUFF_SECONDS * 20;
        long now = ActiveStates.now();
        buff(player, MobEffects.SPEED, dur, 1);
        buff(player, MobEffects.STRENGTH, dur, 1);
        buff(player, MobEffects.NIGHT_VISION, dur, 0);
        buff(player, MobEffects.HASTE, dur, 2);
        buff(player, MobEffects.JUMP_BOOST, dur, 0);
        ActiveStates.setCrimsonAura(player.getUUID(), now + dur);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.CHILIN, now + dur);

        Vec3 front = CardFx.frontPos(player);
        CardFx.sound(player.level(), player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE);
        CardFx.burst(player.level(), front.x, front.y, front.z, CardFx.RED, 90, 1.3, 0.6);
        CardFx.burst(player.level(), front.x, front.y, front.z, ParticleTypes.FLAME, 40, 1.0, 0.3);
        CardFx.announce(player, "发动", Card.CHILIN);
        return true;
    }

    private static void buff(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int dur, int amp) {
        player.addEffect(new MobEffectInstance(effect, dur, amp), null);
    }

    /**
     * 单段扣血（CardEvents 每 2 刻调用一次）：直接扣减生命值并播放受击表现
     * （受击动画包 + 受击音效），返回是否仍高于目标（需要继续扣）。
     */
    public static boolean drainChunk(ServerPlayer player, float targetHealth) {
        float health = player.getHealth();
        if (health <= targetHealth + 0.01F || player.isDeadOrDying()) {
            return false;
        }
        player.setHealth(Math.max(targetHealth, health - CardConfig.CHILIN_DRAIN_CHUNK_HP));
        if (player.level() instanceof ServerLevel level) {
            level.broadcastDamageEvent(player, player.damageSources().magic());
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.7F, 0.9F + net.minecraft.util.RandomSource.create().nextFloat() * 0.2F);
        }
        return player.getHealth() > targetHealth + 0.01F;
    }

    /** 红色粒子光环（CardEvents 周期调用）。 */
    public static void spawnAura(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(CardFx.RED, player.getX(), player.getY() + 0.9, player.getZ(),
                5, 0.35, 0.6, 0.35, 0.01);
        }
    }

    /** 供 Manhunt 每局开始/结束结算：取消进行中的扣血与光环（强化效果由效果清除统一处理）。 */
    public static void reset(ServerPlayer player) {
        ActiveStates.clearCrimson(player.getUUID());
        ActiveStates.cancelEndHint(player.getUUID(), Card.CHILIN);
    }
}
