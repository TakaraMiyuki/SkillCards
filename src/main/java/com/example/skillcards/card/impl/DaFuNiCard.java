package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.compat.ManhuntHook;
import com.example.skillcards.data.CardState;
import com.example.skillcards.registry.Card;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 达芙妮之灾厄：召唤20只攻击目标的白色魔兔；
 * 每只兔子发动一次攻击会分裂为两只（同批次存活上限见 {@link CardConfig#DAFUNI_CAST_LIMIT}）；
 * 所有兔子持续120秒后消失。
 */
public final class DaFuNiCard {
    private DaFuNiCard() {}

    /** setVariant(EVIL) 会自动附加的攻击力修饰符（ minecraft:evil +5 ），需要移除后改写为 1 点。 */
    private static final Identifier EVIL_MODIFIER_ID = Identifier.withDefaultNamespace("evil");

    public static boolean activate(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        UUID castId = UUID.randomUUID();
        long expiry = ActiveStates.now() + CardConfig.DAFUNI_LIFETIME_TICKS;
        RandomSource random = level.getRandom();
        int spawned = 0;
        for (int i = 0; i < CardConfig.DAFUNI_BUNNY_COUNT; i++) {
            if (spawnBunny(level, player, castId, expiry, random, player.position())) {
                spawned++;
            }
        }
        if (spawned == 0) {
            CardFx.hint(player, "周围没有可用的生成位置");
            return false;
        }
        // 白+红粒子（加强）
        CardFx.burst(level, front.x, front.y, front.z, CardFx.WHITE, 70, 1.2, 0.35);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.RED, 50, 1.2, 0.35);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL);
        CardFx.announce(player, "发动", Card.DAFUNI);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.DAFUNI, expiry);
        return true;
    }

    /** 在以 owner 为圆心的圆环上找地表位置生成一只魔兔。 */
    static boolean spawnBunny(ServerLevel level, ServerPlayer owner, UUID castId, long expiry,
                              RandomSource random, Vec3 center) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = CardConfig.DAFUNI_SPAWN_MIN_RADIUS
            + random.nextDouble() * (CardConfig.DAFUNI_SPAWN_MAX_RADIUS - CardConfig.DAFUNI_SPAWN_MIN_RADIUS);
        int x = Mth.floor(center.x + Math.cos(angle) * dist);
        int z = Mth.floor(center.z + Math.sin(angle) * dist);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY()) {
            return false; // 区块未加载或虚空
        }
        Rabbit rabbit = new Rabbit(EntityTypes.RABBIT, level);
        rabbit.setComponent(DataComponents.RABBIT_VARIANT, Rabbit.Variant.EVIL); // 白色魔兔
        rabbit.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        applyBunnyStats(rabbit);
        rabbit.setData(CardState.BUNNY_MARK, new CardState.BunnyMark(owner.getUUID(), castId, expiry));
        level.addFreshEntity(rabbit);
        level.sendParticles(ParticleTypes.POOF, x + 0.5, y + 0.5, z + 0.5, 10, 0.25, 0.25, 0.25, 0.02);
        return true;
    }

    /** 每只兔：1 滴血、攻击 1 点。 */
    private static void applyBunnyStats(Rabbit rabbit) {
        AttributeInstance health = rabbit.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(CardConfig.DAFUNI_BUNNY_MAX_HEALTH);
            rabbit.setHealth((float) CardConfig.DAFUNI_BUNNY_MAX_HEALTH);
        }
        AttributeInstance attack = rabbit.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(EVIL_MODIFIER_ID); // 移除 evil 自带的 +5
            attack.setBaseValue(CardConfig.DAFUNI_BUNNY_ATTACK_DAMAGE);
        }
        AttributeInstance movement = rabbit.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.setBaseValue(CardConfig.DAFUNI_BUNNY_MOVEMENT_SPEED); // 接近玩家疾跑速度
        }
    }

    /** 统计同批次存活兔数量（含自己），用于分裂上限。 */
    static int countCastBunnies(ServerLevel level, Vec3 center, UUID castId) {
        double r = CardConfig.DAFUNI_SPLIT_COUNT_RADIUS;
        AABB box = new AABB(center.x - r, center.y - r, center.z - r,
            center.x + r, center.y + r, center.z + r);
        List<Rabbit> rabbits = level.getEntitiesOfClass(Rabbit.class, box,
            rabbit -> rabbit.isAlive() && rabbit.getData(CardState.BUNNY_MARK) != null);
        int count = 0;
        for (Rabbit rabbit : rabbits) {
            CardState.BunnyMark mark = rabbit.getData(CardState.BUNNY_MARK);
            if (mark != null && mark.castId().equals(castId)) {
                count++;
            }
        }
        return count;
    }

    /** 兔子周期逻辑（由 EntityTickEvent.Post 调用）：到期清除 + 只锁定合法目标（猎人/敌对生物）。 */
    public static void tick(ServerLevel level, Rabbit rabbit) {
        CardState.BunnyMark mark = rabbit.getData(CardState.BUNNY_MARK);
        if (mark == null) {
            return;
        }
        long now = ActiveStates.now();
        if (now >= mark.expiry()) {
            CardFx.poof(level, rabbit.getX(), rabbit.getY() + 0.3, rabbit.getZ());
            rabbit.discard();
            return;
        }
        if (rabbit.tickCount % 20 != 0) {
            return;
        }
        // 目标必须合法，否则清空并重新索敌
        LivingEntity target = rabbit.getTarget();
        if (ManhuntHook.isValidTarget(target)) {
            return;
        }
        rabbit.setTarget(null);
        double best = CardConfig.DAFUNI_AGGRO_RANGE * CardConfig.DAFUNI_AGGRO_RANGE;
        AABB box = new AABB(rabbit.position(), rabbit.position()).inflate(CardConfig.DAFUNI_AGGRO_RANGE);
        LivingEntity nearest = null;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box,
            entity -> entity.isAlive() && ManhuntHook.isValidTarget(entity))) {
            double distSq = rabbit.distanceToSqr(candidate);
            if (distSq <= best) {
                best = distSq;
                nearest = candidate;
            }
        }
        if (nearest != null) {
            rabbit.setTarget(nearest);
        }
    }

    /** 攻击命中后分裂为两只（原体保留 + 新增一只），受同批次上限约束。 */
    public static void trySplit(ServerLevel level, Rabbit rabbit) {
        CardState.BunnyMark mark = rabbit.getData(CardState.BUNNY_MARK);
        if (mark == null) {
            return;
        }
        long now = ActiveStates.now();
        if (now >= mark.expiry()) {
            return;
        }
        if (countCastBunnies(level, rabbit.position(), mark.castId()) >= CardConfig.DAFUNI_CAST_LIMIT) {
            return;
        }
        spawnClone(level, rabbit, mark, level.getRandom());
    }

    private static boolean spawnClone(ServerLevel level, Rabbit rabbit, CardState.BunnyMark mark, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 0.5 + random.nextDouble() * 1.0;
        int x = Mth.floor(rabbit.getX() + Math.cos(angle) * dist);
        int z = Mth.floor(rabbit.getZ() + Math.sin(angle) * dist);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY()) {
            return false;
        }
        Rabbit clone = new Rabbit(EntityTypes.RABBIT, level);
        clone.setComponent(DataComponents.RABBIT_VARIANT, Rabbit.Variant.EVIL);
        clone.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        applyBunnyStats(clone);
        clone.setData(CardState.BUNNY_MARK, mark);
        level.addFreshEntity(clone);
        level.sendParticles(ParticleTypes.POOF, x + 0.5, y + 0.5, z + 0.5, 10, 0.25, 0.25, 0.25, 0.02);
        return true;
    }

    /** 管理员指令：清除全部模组生成的魔兔（跨维度），返回清除数量。 */
    public static int clearAllBunnies(MinecraftServer server) {
        int cleared = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof Rabbit rabbit && rabbit.getData(CardState.BUNNY_MARK) != null) {
                    CardFx.poof(level, rabbit.getX(), rabbit.getY() + 0.3, rabbit.getZ());
                    rabbit.discard();
                    cleared++;
                }
            }
        }
        return cleared;
    }
}
