package com.example.skillcards.compat;

import com.example.manhunt.item.CompassManager;
import com.example.manhunt.item.ManhuntItems;
import com.example.manhunt.game.TeamUtil;
import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 对 manhunt 模组的直接引用层：只有 ManhuntHook 确认 manhunt 已加载后才会触碰本类。
 */
final class ManhuntBridge {
    private ManhuntBridge() {}

    static boolean isHunter(ServerPlayer player) {
        return TeamUtil.isHunter(player);
    }

    static List<ServerPlayer> huntersAround(ServerPlayer center, double radius) {  // 返回值实为 LivingEntity 子集
        MinecraftServer server = center.level().getServer();
        if (server == null) {
            return List.of();
        }
        List<ServerPlayer> out = new ArrayList<>();
        double radiusSq = radius * radius;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == center || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (!TeamUtil.isHunter(player)) {
                continue;
            }
            if (player.distanceToSqr(center) > radiusSq) {
                continue;
            }
            out.add(player);
        }
        return out;
    }

    static boolean isCoreItem(ItemStack stack) {
        if (!CardConfig.SANSHI_EXCLUDE_CORE_ITEMS) {
            return false;
        }
        return stack.is(ManhuntItems.TRACKING_COMPASS.get())
            || stack.is(ManhuntItems.CHECKPOINT_COMPASS.get());
    }

    /** 安装罗盘指向装饰器：被"盲点"干扰的猎人，其追踪罗盘每秒指向一个随机假坐标。 */
    static void installCompassDecorator(MinecraftServer server) {
        CompassManager.targetDecorator = (holder, original) -> {
            if (!ActiveStates.isJammed(holder)) {
                return null;
            }
            RandomSource random = RandomSource.create();
            int dx = random.nextInt(-CardConfig.MANGDIAN_JAM_RANGE, CardConfig.MANGDIAN_JAM_RANGE + 1);
            int dz = random.nextInt(-CardConfig.MANGDIAN_JAM_RANGE, CardConfig.MANGDIAN_JAM_RANGE + 1);
            return GlobalPos.of(original.dimension(), original.pos().offset(dx, 0, dz));
        };
    }

    static void removeCompassDecorator() {
        CompassManager.targetDecorator = null;
    }

    static boolean decoratorInstalled() {
        return CompassManager.targetDecorator != null;
    }
}
