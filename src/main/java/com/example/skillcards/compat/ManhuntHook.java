package com.example.skillcards.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * 与 Manhunt 的联动入口。所有对 manhunt 类的引用都隔离在 {@link ManhuntBridge} 中，
 * 只有在 manhunt 已加载时才会触碰该类，保证本模组可脱离 manhunt 独立加载。
 *
 * 目标规则：
 * - manhunt 已加载：对"猎人生效/锁定"的技能只以猎人为目标。
 * - manhunt 未加载：回退为范围内所有存活敌对生物（{@link Enemy}）。
 */
public final class ManhuntHook {
    private ManhuntHook() {}

    private static boolean checked;
    private static boolean loaded;

    public static boolean available() {
        if (!checked) {
            loaded = ModList.get().isLoaded("manhunt");
            checked = true;
        }
        return loaded;
    }

    /** 提示语中的目标称呼（"猎人" / "敌对生物"）。 */
    public static String targetNoun() {
        return available() ? "猎人" : "敌对生物";
    }

    public static boolean isHunter(ServerPlayer player) {
        return available() && ManhuntBridge.isHunter(player);
    }

    /**
     * 技能目标：范围内、存活、非旁观、排除自己。
     * manhunt 模式下为猎人玩家；回退模式下为敌对生物。
     */
    public static List<LivingEntity> targetsAround(ServerPlayer center, double radius) {
        if (available()) {
            return List.copyOf(ManhuntBridge.huntersAround(center, radius));
        }
        AABB box = new AABB(center.position(), center.position()).inflate(radius);
        return new ArrayList<>(center.level().getEntitiesOfClass(LivingEntity.class, box,
            entity -> entity != center && entity.isAlive() && entity instanceof Enemy));
    }

    /** 实体是否为该技能的合法目标（魔兔索敌与目标过滤用）。 */
    public static boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (available()) {
            return entity instanceof ServerPlayer player && ManhuntBridge.isHunter(player);
        }
        return entity instanceof Enemy;
    }

    /** 是否为罗盘等核心道具（三世秘传默认不偷）。 */
    public static boolean isCoreItem(net.minecraft.world.item.ItemStack stack) {
        return available() && ManhuntBridge.isCoreItem(stack);
    }

    public static void onServerStarted(MinecraftServer server) {
        if (available()) {
            ManhuntBridge.installCompassDecorator(server);
        }
    }

    /** 罗盘干扰装饰器是否已安装（冒烟测试用）。 */
    public static boolean compassDecoratorInstalled() {
        return available() && ManhuntBridge.decoratorInstalled();
    }

    public static void onServerStopping() {
        if (available()) {
            ManhuntBridge.removeCompassDecorator();
        }
    }
}
