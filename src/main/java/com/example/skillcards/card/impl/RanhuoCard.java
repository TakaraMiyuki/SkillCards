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
        // 3（宽）×4（长）矩形区域：沿目视方向前方 4 格、左右各 1.5 格、高度差 ±2 格
        for (var target : level.getEntities(player, player.getBoundingBox().inflate(6),
                e -> e instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive() && e != player)) {
            Vec3 to = target.position().subtract(player.position());
            double forward = to.x * look.x + to.z * look.z;
            double perpX = to.x - look.x * forward, perpZ = to.z - look.z * forward;
            if (forward < 0 || forward > CardConfig.RANHUO_LENGTH
                || perpX * perpX + perpZ * perpZ > CardConfig.RANHUO_HALF_WIDTH * CardConfig.RANHUO_HALF_WIDTH
                || Math.abs(to.y) > 2) {
                continue;
            }
            var living = (net.minecraft.world.entity.LivingEntity) target;
            int layers = GlobalEffects.scorchLayers(living);
            float convert = layers * CardConfig.RANHUO_SCORCH_CONVERT_DAMAGE;
            if (convert > 0) {
                GlobalEffects.addScorch(living, -layers); // 清空
                // 灼烧清空后的转换结算（数值上等同于火焰伤害；用 playerAttack 保留击杀归属与战利品）
                boolean applied = living.hurtServer(level, player.damageSources().playerAttack(player), convert);
                if (CardConfig.DEBUG_LOGGING) {
                    com.mojang.logging.LogUtils.getLogger().info(
                        "[SkillCards][燃火] {} 灼烧转化：{} 层 × 6 = {} 点伤害，实际结算:{}",
                        living.getName().getString(), layers, convert, applied);
                }
            }
            living.igniteForSeconds(CardConfig.RANHUO_FIRE_SECONDS);
            GlobalEffects.addScorch(living, 1);
            hits++;
        }
        // 3×4 矩形区域内铺满火焰粒子（宽3：左右各1.5格；长4：前方0~4格）
        for (int len = 0; len <= 8; len++) {
            double f = len * 0.5;
            for (int wid = -3; wid <= 3; wid++) {
                double p = wid * 0.5;
                double x = player.getX() + look.x * f + -look.z * p;
                double z = player.getZ() + look.z * f + look.x * p;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    x, player.getY() + 0.3, z, 2, 0.15, 0.25, 0.15, 0.01);
            }
        }
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT);
        CardFx.announce(player, "发动", Card.RANHUO);
        if (hits == 0) {
            CardFx.hint(player, "扇形范围内没有目标");
        }
        return true;
    }
}
