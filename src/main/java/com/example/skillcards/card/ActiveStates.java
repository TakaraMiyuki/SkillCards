package com.example.skillcards.card;

import com.example.skillcards.registry.Card;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 服务端内存中的临时卡牌状态（神罚标记 / 罗盘干扰 / 蓄力 / 飞行 / 诅咒的到期时间）。
 * 以主世界 gameTime 为时钟；重登后自身增益类状态失效（赤鳞的永久加成除外，见 {@code CardState}）。
 */
public final class ActiveStates {
    private ActiveStates() {}

    private static MinecraftServer server;
    private static final Map<UUID, Long> PUNISH = new HashMap<>();      // 神罚：下次攻击强化，UUID=使用者
    private static final Map<UUID, Long> FLIGHT = new HashMap<>();      // 海王之翼：飞行到期
    private static final Map<UUID, Long> CURSE = new HashMap<>();       // 诅咒化身：最大生命减半到期
    private static final Map<UUID, Long> SACRED = new HashMap<>();      // 圣树化身：环绕粒子时长
    private static final Map<UUID, Long> JAM = new HashMap<>();         // 盲点：猎人罗盘乱指到期
    private static final Map<UUID, Long> WARP_CHARGE = new HashMap<>(); // 亚丝缇：蓄力完成时刻
    private static final List<PendingStrike> SECOND_STRIKES = new ArrayList<>(); // 麒麟：待补的第二道雷
    private static final Map<UUID, Map<Card, Long>> END_HINTS = new HashMap<>(); // 到期时提示"效果结束"
    private static final Map<UUID, Set<Card>> COOLING = new HashMap<>(); // 冷却完毕提醒的记忆集
    private static final Map<UUID, SmokeZone> SMOKE_ZONES = new HashMap<>(); // 纱幕：原地黑灰烟雾团

    /** 纱幕释放的黑灰烟雾团（原地固定，半径/高/时长见 CardConfig）。 */
    public record SmokeZone(ResourceKey<Level> dimension, double x, double y, double z, long expiry) {}

    /** 麒麟第二道雷的定时任务。 */
    public record PendingStrike(UUID target, long fireTime) {}

    public static void bind(MinecraftServer s) {
        server = s;
        clear();
    }

    public static void clear() {
        PUNISH.clear();
        FLIGHT.clear();
        CURSE.clear();
        SACRED.clear();
        JAM.clear();
        WARP_CHARGE.clear();
        SECOND_STRIKES.clear();
        END_HINTS.clear();
        COOLING.clear();
        SMOKE_ZONES.clear();
    }

    /** 清除全部烟雾团（管理员指令用），返回清除数量。 */
    public static int clearSmokeZones() {
        int count = SMOKE_ZONES.size();
        SMOKE_ZONES.clear();
        return count;
    }

    /** 清除单个玩家的全部卡牌状态（管理员指令用）。 */
    public static void clearFor(UUID id) {
        PUNISH.remove(id);
        FLIGHT.remove(id);
        CURSE.remove(id);
        SACRED.remove(id);
        JAM.remove(id);
        WARP_CHARGE.remove(id);
        END_HINTS.remove(id);
        COOLING.remove(id);
    }

    public static MinecraftServer server() {
        return server;
    }

    public static long now() {
        return server == null ? 0L : server.overworld().getGameTime();
    }

    // ==================== 通用 map 操作 ====================

    private static Long get(Map<UUID, Long> map, UUID id) {
        Long until = map.get(id);
        if (until == null) {
            return null;
        }
        if (now() >= until) {
            map.remove(id);
            return null;
        }
        return until;
    }

    private static void set(Map<UUID, Long> map, UUID id, long until) {
        map.put(id, until);
    }

    private static void remove(Map<UUID, Long> map, UUID id) {
        map.remove(id);
    }

    // ==================== 神罚 ====================

    public static void setPunish(UUID user, long until) {
        set(PUNISH, user, until);
    }

    /** 有神罚标记则消费掉并返回 true。 */
    public static boolean consumePunish(UUID user) {
        if (get(PUNISH, user) != null) {
            remove(PUNISH, user);
            cancelEndHint(user, Card.SHENFA); // 命中即消耗，不再提示"效果结束"
            return true;
        }
        return false;
    }

    public static Map<UUID, Long> punish() {
        return PUNISH;
    }

    public static Map<UUID, Long> flights() {
        return FLIGHT;
    }

    public static Map<UUID, Long> curses() {
        return CURSE;
    }

    public static Map<UUID, Long> sacreds() {
        return SACRED;
    }

    public static Map<UUID, Long> warpCharges() {
        return WARP_CHARGE;
    }

    // ==================== 海王之翼 ====================

    public static void setFlight(UUID user, long until) {
        set(FLIGHT, user, until);
    }

    public static Long flight(UUID user) {
        return get(FLIGHT, user);
    }

    public static void clearFlight(UUID user) {
        remove(FLIGHT, user);
    }

    // ==================== 诅咒化身 ====================

    public static void setCurse(UUID user, long until) {
        set(CURSE, user, until);
    }

    public static Long curse(UUID user) {
        return get(CURSE, user);
    }

    public static void clearCurse(UUID user) {
        remove(CURSE, user);
    }

    // ==================== 圣树化身（环绕粒子） ====================

    public static void setSacred(UUID user, long until) {
        set(SACRED, user, until);
    }

    public static Long sacred(UUID user) {
        return get(SACRED, user);
    }

    // ==================== 盲点（罗盘干扰） ====================

    public static void setJam(UUID hunter, long until) {
        set(JAM, hunter, until);
    }

    public static boolean isJammed(UUID hunter) {
        return get(JAM, hunter) != null;
    }

    // ==================== 亚丝缇（蓄力） ====================

    public static void setWarpCharge(UUID user, long until) {
        set(WARP_CHARGE, user, until);
    }

    /** 返回 null 表示不在蓄力；返回"已到期"的值表示该传送了（调用方负责移除）。 */
    public static Long warpCharge(UUID user) {
        return WARP_CHARGE.get(user);
    }

    public static void clearWarpCharge(UUID user) {
        remove(WARP_CHARGE, user);
    }

    // ==================== 麒麟第二道雷 ====================

    public static void scheduleSecondStrike(UUID target, long fireTime) {
        SECOND_STRIKES.add(new PendingStrike(target, fireTime));
    }

    /** 取出到期的第二道雷任务（每刻由扫描消费）。 */
    public static List<PendingStrike> drainDueSecondStrikes() {
        if (SECOND_STRIKES.isEmpty()) {
            return List.of();
        }
        long now = now();
        List<PendingStrike> due = new ArrayList<>();
        SECOND_STRIKES.removeIf(task -> {
            if (task.fireTime() <= now) {
                due.add(task);
                return true;
            }
            return false;
        });
        return due;
    }

    // ==================== 纱幕（烟雾团） ====================

    public static void addSmokeZone(ResourceKey<Level> dimension, double x, double y, double z, long expiry) {
        if (SMOKE_ZONES.size() >= 12) { // 全服上限，防止叠加失控
            SMOKE_ZONES.clear();
        }
        SMOKE_ZONES.put(UUID.randomUUID(), new SmokeZone(dimension, x, y, z, expiry));
    }

    public static Map<UUID, SmokeZone> smokeZones() {
        return SMOKE_ZONES;
    }

    // ==================== 结束提示（有持续时间的卡） ====================

    /** 登记一张卡的效果结束提示（到期由每刻扫描播报）。 */
    public static void scheduleEndHint(UUID user, Card card, long until) {
        END_HINTS.computeIfAbsent(user, k -> new HashMap<>()).put(card, until);
    }

    public static void cancelEndHint(UUID user, Card card) {
        Map<Card, Long> hints = END_HINTS.get(user);
        if (hints != null) {
            hints.remove(card);
        }
    }

    public static Map<UUID, Map<Card, Long>> endHints() {
        return END_HINTS;
    }

    // ==================== 冷却完毕提醒（记忆集） ====================

    public static Set<Card> cooling(UUID id) {
        return COOLING.getOrDefault(id, Set.of());
    }

    public static void updateCooling(UUID id, Set<Card> current) {
        COOLING.put(id, new HashSet<>(current));
    }

    // ==================== 玩家解析 ====================

    public static ServerPlayer player(UUID id) {
        return server == null ? null : server.getPlayerList().getPlayer(id);
    }
}
