package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.Vec3;

/**
 * 多重箭（被动）：使用弓或弩射击时，射出三倍的箭矢（额外 2 支，小范围散布）。
 * 由 CardEvents#onArrowJoin（EntityJoinLevelEvent）触发，避免递归的克隆箭会
 * 携带 DISALLOWED 拾取标记。
 */
public final class DuochongCard {
    private DuochongCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }

    /** 为原箭生成 2 支带散布的复制箭。 */
    public static void spawnClones(ServerPlayer shooter, Arrow original) {
        if (!(original.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 origin = original.position();
        Vec3 velocity = original.getDeltaMovement();
        for (int i = 0; i < CardConfig.DUOCHONG_EXTRA_ARROWS; i++) {
            // 复制原箭的拾取物品栈（含药水箭矢的 POTION_CONTENTS）→ 克隆同为药水箭
            Arrow clone = new Arrow(level, shooter, original.getPickupItemStackOrigin(), null);
            clone.setData(com.example.skillcards.data.CardState.DUOCHONG_CLONE, true);
            clone.setOwner(shooter);
            clone.snapTo(origin.x, origin.y, origin.z, original.getYRot(), original.getXRot());
            // 绕 Y 轴 ±8° 旋转出散布
            double angle = (i == 0 ? 1 : -1) * CardConfig.DUOCHONG_SPREAD;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            clone.setDeltaMovement(velocity.x * cos - velocity.z * sin, velocity.y,
                velocity.x * sin + velocity.z * cos);
            // 26.2 的 AbstractArrow 没有 baseDamage 读取器；玩家箭基础伤害固定 2.0
            clone.setBaseDamage(2.0);
            clone.pickup = AbstractArrow.Pickup.DISALLOWED;
            level.addFreshEntity(clone);
        }
    }
}
