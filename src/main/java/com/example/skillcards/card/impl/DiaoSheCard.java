package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.Vec3;

import net.minecraft.util.RandomSource;

/**
 * 吊射：发动后 30 秒内，弩或完全蓄力的弓射出的箭矢落点处会持续落下箭雨——
 * 以落点为中心 3×3、上方 20 格、每支箭固定 6 点伤害，每场箭雨持续 3 秒。
 */
public final class DiaoSheCard {
    private DiaoSheCard() {}

    /** 已触发过箭雨的箭（一根箭最多触发一场：命中实体后落地不再重复）。 */
    private static final java.util.Set<java.util.UUID> TRIGGERED = new java.util.HashSet<>();

    public static boolean alreadyTriggered(Arrow arrow) {
        return TRIGGERED.contains(arrow.getUUID());
    }

    public static void markTriggered(Arrow arrow) {
        TRIGGERED.add(arrow.getUUID());
        if (TRIGGERED.size() > 200) { // 粗略防泄漏
            TRIGGERED.clear();
        }
    }

    public static boolean activate(ServerPlayer player) {
        ActiveStates.setDiaosheWindow(player.getUUID(), ActiveStates.now() + CardConfig.DIAOSHE_WINDOW_TICKS);
        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.YELLOW, 50, 0.9, 0.3);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.PINK, 40, 0.8, 0.3);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END);
        CardFx.announce(player, "发动", Card.DIAOSHE);
        return true;
    }

    /** 箭矢落点触发一场箭雨（由 ProjectileImpactEvent 调用）。 */
    public static void spawnRain(ServerPlayer shooter, Vec3 impact) {
        ActiveStates.addRainZone(new ActiveStates.RainZone(shooter.level().dimension(),
            impact.x, impact.y, impact.z, ActiveStates.now() + CardConfig.DIAOSHE_RAIN_DURATION_TICKS,
            shooter.getUUID()));
    }

    /** 箭雨推进（由每刻扫描调用）：每 4 刻从上空 20 格降下 1 支固定 6 点伤害的箭。 */
    public static void tickRain(ServerLevel level, ActiveStates.RainZone zone) {
        ServerPlayer owner = ActiveStates.player(zone.owner());
        RandomSource random = level.getRandom();
        double x = zone.x() + (random.nextDouble() * 2 - 1) * CardConfig.DIAOSHE_HALF_AREA;
        double z = zone.z() + (random.nextDouble() * 2 - 1) * CardConfig.DIAOSHE_HALF_AREA;
        double y = zone.y() + CardConfig.DIAOSHE_RAIN_HEIGHT;
        Arrow arrow = new Arrow(EntityTypes.ARROW, level);
        if (owner != null) {
            arrow.setOwner(owner);
        }
        arrow.snapTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
        arrow.setDeltaMovement((random.nextDouble() - 0.5) * 0.1, -1.5, (random.nextDouble() - 0.5) * 0.1);
        arrow.setBaseDamage((float) CardConfig.DIAOSHE_ARROW_DAMAGE);
        arrow.setData(com.example.skillcards.data.CardState.RAIN_ARROW, true);
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(arrow);
    }
}
