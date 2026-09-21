package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 亚丝缇的赐福：末影粒子以玩家身前为中心向内敛聚蓄力 5 秒（可移动、受击不打断，但移速显著降低），
 * 传送发动前一瞬向外炸开，随后随机传送到大于 200 格、小于 300 格的安全地表。
 */
public final class YaSiTiCard {
    private YaSiTiCard() {}

    public static final Identifier CHARGE_SLOW_ID =
        Identifier.fromNamespaceAndPath("skillcards", "yasiti_charge_slow");

    public static boolean activate(ServerPlayer player) {
        if (ActiveStates.warpCharge(player.getUUID()) != null) {
            CardFx.hint(player, "已经在蓄力中");
            return false;
        }
        ActiveStates.setWarpCharge(player.getUUID(), ActiveStates.now() + CardConfig.YASITI_CHARGE_TICKS);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(CHARGE_SLOW_ID);
            speed.addTransientModifier(new AttributeModifier(CHARGE_SLOW_ID,
                CardConfig.YASITI_CHARGE_SLOW_RATIO, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        CardFx.sound(player.level(), player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE);
        CardFx.announce(player, "发动", Card.YASITI);
        return true;
    }

    /** 蓄力期间的向内敛聚粒子（由每刻扫描调用），收敛中心在玩家身前半个身位。 */
    public static void chargingParticles(ServerLevel level, ServerPlayer player, long tickInCharge) {
        Vec3 front = CardFx.frontPos(player);
        double phase = tickInCharge * 0.45;
        // 双层球面内敛：大半径慢相 + 小半径快相
        CardFx.inwardCollapse(level, front.x, front.y, front.z, 8, 2.2, phase);
        CardFx.inwardCollapse(level, front.x, front.y + 0.2, front.z, 6, 1.4, -phase * 1.6);
    }

    /** 蓄力最后一瞬的向外炸开（传送发动前）。 */
    public static void preTeleportBurst(ServerLevel level, ServerPlayer player) {
        Vec3 front = CardFx.frontPos(player);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.PORTAL, 120, 1.8, 1.1);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.REVERSE_PORTAL, 60, 1.4, 0.8);
    }

    /** 蓄力完成：随机方向 (200,300) 格内找安全地表传送。返回是否成功。 */
    public static boolean teleport(ServerPlayer player) {
        ServerLevel level = player.level();
        RandomSource random = level.getRandom();
        Vec3 origin = player.position();
        for (int attempt = 0; attempt < CardConfig.YASITI_FIND_RETRIES; attempt++) {
            double dist = CardConfig.YASITI_MIN_DISTANCE
                + random.nextDouble() * (CardConfig.YASITI_MAX_DISTANCE - CardConfig.YASITI_MIN_DISTANCE);
            double angle = random.nextDouble() * Math.PI * 2;
            int x = (int) Math.floor(origin.x + Math.cos(angle) * dist);
            int z = (int) Math.floor(origin.z + Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= level.getMinY()) {
                continue; // 区块未加载或虚空
            }
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockState groundState = level.getBlockState(ground);
            if (!groundState.blocksMotion()) {
                continue; // 岩浆 / 水 / 空气等不安全地面
            }
            if (!level.getBlockState(ground.above()).isAir() || !level.getBlockState(ground.above(2)).isAir()) {
                continue; // 头顶被埋
            }
            // 出发点粒子迸发
            Vec3 front = CardFx.frontPos(player);
            CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.PORTAL, 60, 0.8, 0.6);
            CardFx.sound(level, front.x, front.y, front.z, SoundEvents.ENDERMAN_TELEPORT);

            player.teleportTo(level, x + 0.5, y, z + 0.5, java.util.Set.of(),
                player.getYRot(), player.getXRot(), false);

            // 落点粒子迸发
            CardFx.burst(level, x + 0.5, y + 1.0, z + 0.5, ParticleTypes.PORTAL, 60, 0.8, 0.6);
            CardFx.burst(level, x + 0.5, y + 1.0, z + 0.5, ParticleTypes.REVERSE_PORTAL, 30, 0.5, 0.4);
            CardFx.sound(level, x + 0.5, y, z + 0.5, SoundEvents.ENDERMAN_TELEPORT);
            return true;
        }
        CardFx.hint(player, "找不到安全的落点，传送失败");
        return false;
    }

    public static void removeChargeSlow(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(CHARGE_SLOW_ID);
        }
    }
}
