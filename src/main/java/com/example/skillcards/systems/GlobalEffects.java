package com.example.skillcards.systems;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 跨卡牌的全局效果系统（服务端内存态，不同卡牌来源的同名效果互相叠加）。
 *
 * 灼烧：每层使目标着火时受到的火焰伤害 +1 点；着火状态下每 3 秒自动 +1 层（上限 5 层），
 *       层数存在于着火期间及火熄灭后的 3 秒。
 * 剑舞：每层使玩家近战攻击速度 +3%；每次叠加后存续 3 秒（上限 20 层）。
 */
public final class GlobalEffects {
    private GlobalEffects() {}

    private static void log(String format, Object... args) {
        if (CardConfig.DEBUG_LOGGING) {
            com.mojang.logging.LogUtils.getLogger().info("[SkillCards][灼烧] " + format, args);
        }
    }

    public static final Identifier DANCE_ATTR_ID =
        Identifier.fromNamespaceAndPath("skillcards", "sword_dance");

    // ==================== 灼烧 ====================

    public record Scorch(int layers, long nextStackTick, long windowEnd) {}

    /** 为目标叠加 n 层灼烧（同一来源一次一批）。 */
    public static void addScorch(LivingEntity target, int layers) {
        if (layers <= 0) {
            return;
        }
        long now = ActiveStates.now();
        var map = ActiveStates.scorch();
        Scorch cur = map.get(target.getUUID());
        int newLayers = Math.max(0, Math.min(CardConfig.SCORCH_MAX_LAYERS, (cur == null ? 0 : cur.layers()) + layers));
        if (newLayers == 0) {
            map.remove(target.getUUID());
            log("清除 {} 的灼烧（原 {} 层）", target.getName().getString(), cur == null ? 0 : cur.layers());
            return;
        }
        long nextStack = cur == null ? now + CardConfig.SCORCH_STACK_INTERVAL_TICKS : cur.nextStackTick();
        map.put(target.getUUID(), new Scorch(newLayers, nextStack, now + CardConfig.SCORCH_WINDOW_AFTER_FIRE_TICKS));
        log("{} 灼烧 {}→{} 层（下次自动叠层:{} 刻后）", target.getName().getString(),
            cur == null ? 0 : cur.layers(), newLayers, nextStack - now);
    }

    /** 目标当前灼烧层数（仅当目标正在着火时提供增伤）。 */
    public static int scorchLayers(LivingEntity target) {
        Scorch cur = ActiveStates.scorch().get(target.getUUID());
        return cur == null ? 0 : cur.layers();
    }

    /** 灼烧推进：着火时每 3 秒自动 +1 层并续窗；火熄灭 3 秒后清除。 */
    public static void tickScorch(long now) {
        MinecraftServer server = ActiveStates.server();
        if (server == null) {
            return;
        }
        var iterator = ActiveStates.scorch().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            LivingEntity entity = findEntity(server, entry.getKey());
            Scorch scorch = entry.getValue();
            if (entity == null || now >= scorch.windowEnd()) {
                iterator.remove();
                log("灼烧到期移除（实体在线:{}）", entity != null);
                continue;
            }
            if (entity.isOnFire() && now >= scorch.nextStackTick()) {
                int layers = Math.min(CardConfig.SCORCH_MAX_LAYERS, scorch.layers() + 1);
                entry.setValue(new Scorch(layers, now + CardConfig.SCORCH_STACK_INTERVAL_TICKS,
                    now + CardConfig.SCORCH_WINDOW_AFTER_FIRE_TICKS));
                log("{} 着火自动叠层 → {} 层", entity.getName().getString(), layers);
            }
            // 灼烧可见化：层数越多，着火实体身上的火苗越密
            if (entity.isOnFire() && scorch.layers() > 0 && now % 10 == 0
                && entity.level() instanceof ServerLevel scorchLevel) {
                scorchLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                    scorch.layers() * 3, 0.3, 0.4, 0.3, 0.01);
            }
        }
    }

    private static LivingEntity findEntity(MinecraftServer server, UUID id) {
        for (var level : server.getAllLevels()) {
            if (level.getEntity(id) instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    // ==================== 剑舞 ====================

    public record Dance(int layers, long nextDecayTick) {}

    /** 狂剑层数与最近一次剑舞叠加时刻。 */
    public record Kuangwu(int layers, long lastStack) {}

    /** 为玩家叠加 n 层剑舞（上限 20，刷新存续时间），并同步攻速属性。 */
    public static void addDance(ServerPlayer player, int layers) {
        if (layers <= 0) {
            return;
        }
        long now = ActiveStates.now();
        var map = ActiveStates.swordDance();
        Dance cur = map.get(player.getUUID());
        int newLayers = Math.min(CardConfig.DANCE_MAX_LAYERS, (cur == null ? 0 : cur.layers()) + layers);
        map.put(player.getUUID(), new Dance(newLayers, now + CardConfig.DANCE_DECAY_INTERVAL_TICKS));
        updateDanceAttr(player, newLayers);
    }

    public static int danceLayers(ServerPlayer player) {
        Dance cur = ActiveStates.swordDance().get(player.getUUID());
        return cur == null ? 0 : cur.layers();
    }

    /** 狂舞转换：消耗 per 层剑舞换 1 层狂剑，成功返回 true。 */
    public static boolean tryConvertDance(ServerPlayer player, int per) {
        var map = ActiveStates.swordDance();
        Dance cur = map.get(player.getUUID());
        if (cur != null && cur.layers() >= per) {
            map.put(player.getUUID(), new Dance(cur.layers() - per, cur.nextDecayTick()));
            updateDanceAttr(player, cur.layers() - per);
            return true;
        }
        return false;
    }

    /** 剑舞推进：每 3 秒未再次叠加只掉 1 层；归零后移除。 */
    public static void tickDance(long now) {
        var iterator = ActiveStates.swordDance().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Dance dance = entry.getValue();
            if (now < dance.nextDecayTick()) {
                continue;
            }
            int layers = Math.max(0, dance.layers() - 1);
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (layers == 0) {
                iterator.remove();
                if (player != null) {
                    updateDanceAttr(player, 0);
                }
            } else {
                entry.setValue(new Dance(layers, now + CardConfig.DANCE_DECAY_INTERVAL_TICKS));
                if (player != null) {
                    updateDanceAttr(player, layers);
                }
            }
        }
    }

    private static void updateDanceAttr(ServerPlayer player, int layers) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }
        if (layers <= 0) {
            attackSpeed.removeModifier(DANCE_ATTR_ID);
        } else {
            attackSpeed.addOrUpdateTransientModifier(new AttributeModifier(DANCE_ATTR_ID,
                CardConfig.DANCE_SPEED_PER_LAYER * layers, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
