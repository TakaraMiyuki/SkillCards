package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import com.example.skillcards.systems.GlobalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * 燃火：向目视方向半径 3 格的扇形区域释放火焰——
 * 被击中的敌人获得 10 秒着火并叠加 1 层灼烧；
 * 若敌人已有灼烧，先清空所有灼烧并按每层 6 点转换为火焰伤害，再叠加 1 层。
 */
public final class RanhuoCard {
    private RanhuoCard() {}

    public static boolean activate(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle().normalize();
        int hits = 0;
        for (var target : level.getEntities(player, player.getBoundingBox().inflate(CardConfig.RANHUO_RADIUS + 1),
                e -> e instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()
                    && e != player && e.distanceToSqr(player) <= CardConfig.RANHUO_RADIUS * CardConfig.RANHUO_RADIUS)) {
            Vec3 to = target.position().subtract(player.position());
            double horizontal = Math.sqrt(to.x * to.x + to.z * to.z);
            double dot = horizontal < 0.01 ? 1.0
                : (to.x / horizontal * look.x + to.z / horizontal * look.z);
            if (dot < 0.35) { // 扇形半角约 70°
                continue;
            }
            var living = (net.minecraft.world.entity.LivingEntity) target;
            int layers = GlobalEffects.scorchLayers(living);
            float convert = layers * CardConfig.RANHUO_SCORCH_CONVERT_DAMAGE;
            if (convert > 0) {
                GlobalEffects.addScorch(living, -layers); // 清空
                living.hurtServer(level, player.damageSources().playerAttack(player), convert);
            }
            living.igniteForSeconds(CardConfig.RANHUO_FIRE_SECONDS);
            GlobalEffects.addScorch(living, 1);
            hits++;
        }
        // 大量团状火焰粒子（身前锥形）
        Vec3 front = CardFx.frontPos(player);
        for (int i = 0; i < 60; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            double dist = player.getRandom().nextDouble() * CardConfig.RANHUO_RADIUS;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                front.x + Math.cos(angle) * dist, front.y + player.getRandom().nextDouble() * 1.2,
                front.z + Math.sin(angle) * dist, 1, 0.0, 0.02, 0.0, 0.02);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME,
                front.x + Math.cos(angle) * dist, front.y + player.getRandom().nextDouble() * 1.2,
                front.z + Math.sin(angle) * dist, 1, 0.0, 0.02, 0.0, 0.02);
        }
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT);
        if (hits > 0) {
            CardFx.announce(player, "发动", Card.RANHUO);
        } else {
            CardFx.hint(player, "扇形范围内没有目标");
            return false;
        }
        return true;
    }
}
