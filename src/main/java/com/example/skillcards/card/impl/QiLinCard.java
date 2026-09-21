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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 麒麟：精准雷击两次周围10格的目标（每个目标两道，间隔1秒）；浅蓝色+深蓝色粒子。 */
public final class QiLinCard {
    private QiLinCard() {}

    public static boolean activate(ServerPlayer player) {
        List<LivingEntity> targets = ManhuntHook.targetsAround(player, CardConfig.QILIN_RADIUS);
        if (targets.isEmpty()) {
            CardFx.hint(player, "周围 " + (int) CardConfig.QILIN_RADIUS + " 格内没有" + ManhuntHook.targetNoun());
            return false;
        }
        for (LivingEntity target : targets) {
            strike(target);
            ActiveStates.scheduleSecondStrike(target.getUUID(),
                ActiveStates.now() + CardConfig.QILIN_SECOND_STRIKE_DELAY_TICKS);
        }
        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        // 浅蓝+深蓝粒子（加强：蓝尘翻倍 + 电光）
        CardFx.burst(level, front.x, front.y, front.z, CardFx.LIGHT_BLUE, 60, 1.0, 0.4);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.DARK_BLUE, 60, 1.0, 0.4);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.ELECTRIC_SPARK, 50, 1.0, 0.3);
        CardFx.announce(player, "发动", Card.QILIN);
        return true;
    }

    /** 在目标头顶精准落下一道雷（原版落雷，带伤害与火焰）。 */
    public static void strike(LivingEntity target) {
        if (!target.isAlive() || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }
        bolt.snapTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0.0F);
        level.addFreshEntity(bolt);
    }
}
