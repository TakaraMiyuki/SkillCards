package com.example.skillcards.event;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.card.impl.ChiLinCard;
import com.example.skillcards.card.impl.DaFuNiCard;
import com.example.skillcards.card.impl.HaiWangCard;
import com.example.skillcards.card.impl.QiLinCard;
import com.example.skillcards.card.impl.YaSiTiCard;
import com.example.skillcards.card.impl.ZuZhouCard;
import com.example.skillcards.compat.ManhuntHook;
import com.example.skillcards.data.CardState;
import com.example.skillcards.item.SkillCardItem;
import com.example.skillcards.registry.Card;
import com.example.skillcards.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 全部事件监听与每刻扫描（到期处理 / 环绕粒子 / 兔子 AI / 冷却提醒 / 永久属性重挂）。 */
public final class CardEvents {
    private CardEvents() {}

    // ==================== 生命周期 ====================

    public static void onServerStarted(ServerStartedEvent event) {
        ActiveStates.bind(event.getServer());
        ManhuntHook.onServerStarted(event.getServer());
        com.example.skillcards.SkillCardsMod.LOGGER.info("[SkillCards] 已就绪（manhunt 联动: {}）",
            ManhuntHook.available());
        if ("1".equals(System.getenv("SKILLCARDS_SMOKETEST"))) {
            runSmokeTest(event.getServer());
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        ManhuntHook.onServerStopping();
        ActiveStates.clear();
    }

    /** 无玩家冒烟测试（环境变量 SKILLCARDS_SMOKETEST=1 时启用）：校验注册并自动关服。 */
    private static void runSmokeTest(MinecraftServer server) {
        var logger = com.example.skillcards.SkillCardsMod.LOGGER;
        try {
            int count = 0;
            for (var item : ModItems.all()) {
                if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.get()) == null) {
                    logger.error("[SkillCards][冒烟测试] 物品未注册: {}", item.getId());
                } else {
                    count++;
                }
            }
            logger.info("[SkillCards][冒烟测试] 已注册卡牌物品 {}/14", count);
            logger.info("[SkillCards][冒烟测试] manhunt 联动: {}，罗盘干扰钩子: {}",
                ManhuntHook.available() ? "已加载" : "未加载",
                ManhuntHook.compassDecoratorInstalled() ? "已安装" : "未安装");
            logger.info("[SkillCards][冒烟测试] 飞翔联动: {}，flying_enchant:flying 效果: {}",
                com.example.skillcards.compat.FlyingHook.available() ? "已加载" : "未加载",
                com.example.skillcards.compat.FlyingHook.effect() != null ? "已找到" : "未找到");
        } catch (Exception e) {
            logger.error("[SkillCards][冒烟测试] 执行失败", e);
        } finally {
            server.halt(false);
        }
    }

    // ==================== 每刻扫描 ====================

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = ActiveStates.now();

        tickPunish(server, now);
        tickFlight(now);
        tickCurse(now);
        tickSacred(now);
        tickWarpCharge(now);
        tickCrimson(server, now);
        tickEndHints(now);
        tickCooldownReady(server, now);
        tickSmokeZones(server, now);

        // 麒麟：补第二道雷（跨维度解析目标）
        for (ActiveStates.PendingStrike task : ActiveStates.drainDueSecondStrikes()) {
            if (findEntity(task.target()) instanceof LivingEntity target && target.isAlive()) {
                QiLinCard.strike(target);
            }
        }
    }

    /** 跨维度按 UUID 查找实体。 */
    private static LivingEntity findEntity(UUID id) {
        MinecraftServer server = ActiveStates.server();
        if (server == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(id) instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    /** 神罚：头顶标记粒子（到期清理；结束提示由 END_HINTS 统一播报）。 */
    private static void tickPunish(MinecraftServer server, long now) {
        var iterator = ActiveStates.punish().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null || now >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            if (now % CardConfig.PUNISH_MARKER_INTERVAL_TICKS == 0) {
                CardFx.punishMarker(player.level(), player);
            }
        }
    }

    /** 海王之翼：环绕粒子 + 到期收回并给缓降（飞翔效果被提前移除时同样触发）。 */
    private static void tickFlight(long now) {
        var iterator = ActiveStates.flights().entrySet().iterator();
        Holder<MobEffect> flying = com.example.skillcards.compat.FlyingHook.effect();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            boolean timedOut = now >= entry.getValue();
            boolean effectGone = flying != null && !player.hasEffect(flying);
            if (timedOut || effectGone) {
                iterator.remove();
                ActiveStates.cancelEndHint(entry.getKey(), Card.HAIWANG);
                HaiWangCard.expire(player);
                continue;
            }
            if (now % CardConfig.ORBIT_PARTICLE_INTERVAL_TICKS == 0) {
                ServerLevel level = player.level();
                CardFx.orbit(level, player, CardFx.LIGHT_BLUE, 1.1, 0.02);
                CardFx.orbit(level, player, ParticleTypes.END_ROD, 0.9, 0.02);
            }
        }
    }

    /** 诅咒化身：环绕诅咒火焰 + 到期解除最大生命减半。 */
    private static void tickCurse(long now) {
        var iterator = ActiveStates.curses().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (now >= entry.getValue()) {
                iterator.remove();
                ZuZhouCard.expire(player);
                continue;
            }
            if (now % CardConfig.ORBIT_PARTICLE_INTERVAL_TICKS == 0) {
                ServerLevel level = player.level();
                CardFx.orbit(level, player, ParticleTypes.SOUL_FIRE_FLAME, 0.9, 0.02);
                CardFx.orbit(level, player, ParticleTypes.SMALL_FLAME, 1.2, 0.02);
            }
        }
    }

    /** 圣树化身：环绕绿色+黄色+白色粒子。 */
    private static void tickSacred(long now) {
        var iterator = ActiveStates.sacreds().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null || now >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            if (now % CardConfig.ORBIT_PARTICLE_INTERVAL_TICKS == 0) {
                ServerLevel level = player.level();
                CardFx.orbit(level, player, ParticleTypes.COMPOSTER, 1.0, 0.02);
                CardFx.orbit(level, player, CardFx.YELLOW, 1.2, 0.02);
                CardFx.orbit(level, player, ParticleTypes.END_ROD, 1.4, 0.02);
            }
        }
    }

    /** 亚丝缇的赐福：内敛粒子 + 传送前一瞬炸开 + 到期传送（死亡/下线取消）。 */
    private static void tickWarpCharge(long now) {
        var iterator = ActiveStates.warpCharges().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null || !player.isAlive()) {
                iterator.remove();
                if (player != null) {
                    YaSiTiCard.removeChargeSlow(player);
                }
                continue;
            }
            long remaining = entry.getValue() - now;
            if (remaining <= 0) {
                iterator.remove();
                YaSiTiCard.removeChargeSlow(player);
                YaSiTiCard.teleport(player);
                continue;
            }
            if (remaining <= 5) {
                // 传送发动前一瞬炸开
                YaSiTiCard.preTeleportBurst(player.level(), player);
                if (remaining == 5) {
                    CardFx.sound(player.level(), player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE);
                }
            } else {
                YaSiTiCard.chargingParticles(player.level(), player, CardConfig.YASITI_CHARGE_TICKS - remaining);
            }
        }
    }

    /** 赤鳞之跃动：分段扣血推进 + 红色粒子光环。 */
    private static void tickCrimson(MinecraftServer server, long now) {
        var drains = ActiveStates.crimsonDrains();
        var iterator = drains.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ActiveStates.CrimsonDrain task = entry.getValue();
            if (now < task.nextTick()) {
                continue;
            }
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null || player.isDeadOrDying()) {
                iterator.remove();
                continue;
            }
            boolean more = ChiLinCard.drainChunk(player, task.targetHealth());
            if (more && task.chunksLeft() > 1) {
                drains.put(entry.getKey(), new ActiveStates.CrimsonDrain(
                    task.chunksLeft() - 1, task.targetHealth(), now + CardConfig.CHILIN_DRAIN_INTERVAL_TICKS));
            } else {
                iterator.remove();
            }
        }

        if (now % 10 == 0) {
            var auras = ActiveStates.crimsonAuras();
            var auraIterator = auras.entrySet().iterator();
            while (auraIterator.hasNext()) {
                var entry = auraIterator.next();
                if (now >= entry.getValue()) {
                    auraIterator.remove();
                    continue;
                }
                ServerPlayer player = ActiveStates.player(entry.getKey());
                if (player == null) {
                    auraIterator.remove();
                    continue;
                }
                ChiLinCard.spawnAura(player);
            }
        }
    }

    /** 结束提示：到期播报"效果结束：卡名"+ 音效。 */
    private static void tickEndHints(long now) {
        var iterator = ActiveStates.endHints().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Map<Card, Long>> entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            var hints = entry.getValue().entrySet().iterator();
            while (hints.hasNext()) {
                Map.Entry<Card, Long> hint = hints.next();
                if (now >= hint.getValue()) {
                    hints.remove();
                    CardFx.announce(player, "效果结束", hint.getKey());
                    CardFx.sound(player.level(), player.getX(), player.getY(), player.getZ(), CardFx.END_SOUND);
                }
            }
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    /** 冷却完毕提醒：扫描背包内技能卡的冷却状态，与记忆集对比后播报。 */
    private static void tickCooldownReady(MinecraftServer server, long now) {
        if (now % 10 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // current = 背包中仍在冷却的卡；present = 背包中存在的所有技能卡。
            // 与 Manhunt 技能栏联动：卡可被切出背包存入虚拟技能库——离开背包的卡不播报
            // "冷却完毕"（否则切卡瞬间会误报），回到背包且冷却结束才播报。
            Set<Card> current = new HashSet<>();
            Set<Card> present = new HashSet<>();
            var items = player.getInventory().getNonEquipmentItems();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (stack.getItem() instanceof SkillCardItem cardItem) {
                    present.add(cardItem.card());
                    if (player.getCooldowns().isOnCooldown(stack)) {
                        current.add(cardItem.card());
                    }
                }
            }
            ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (offhand.getItem() instanceof SkillCardItem offhandCard) {
                present.add(offhandCard.card());
                if (player.getCooldowns().isOnCooldown(offhand)) {
                    current.add(offhandCard.card());
                }
            }
            Set<Card> previous = ActiveStates.cooling(player.getUUID());
            for (Card card : previous) {
                if (!current.contains(card) && present.contains(card)) {
                    CardFx.announce(player, "冷却完毕", card);
                    CardFx.sound(player.level(), player.getX(), player.getY(), player.getZ(), CardFx.READY_SOUND);
                }
            }
            ActiveStates.updateCooling(player.getUUID(), current);
        }
    }

    /** 纱幕：原地黑灰烟雾团（半径6格、高4格、持续30秒），每 3 刻生成一波粒子。 */
    private static void tickSmokeZones(MinecraftServer server, long now) {
        var iterator = ActiveStates.smokeZones().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ActiveStates.SmokeZone zone = entry.getValue();
            if (now >= zone.expiry()) {
                iterator.remove();
                continue;
            }
            ServerLevel level = server.getLevel(zone.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }
            if (now % CardConfig.SHAMU_SMOKE_WAVE_INTERVAL_TICKS != 0) {
                continue;
            }
            RandomSource random = level.getRandom();
            // 三种烟雾粒子铺满烟雾区，形成完全遮挡视野的烟墙
            ParticleOptions[] types = {
                ParticleTypes.CAMPFIRE_COSY_SMOKE, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, ParticleTypes.GUST
            };
            for (int i = 0; i < 26; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = Math.sqrt(random.nextDouble()) * CardConfig.SHAMU_SMOKE_RADIUS;
                double x = zone.x() + Math.cos(angle) * dist;
                double z = zone.z() + Math.sin(angle) * dist;
                double y = zone.y() + random.nextDouble() * CardConfig.SHAMU_SMOKE_HEIGHT;
                ParticleOptions type = types[random.nextInt(types.length)];
                level.sendParticles(type, x, y, z, 1, 0.05, 0.04, 0.05, 0.01);
            }
            if (now % 10 == 0) { // 地面灰尘环，标出烟雾范围
                CardFx.ring(level, zone.x(), zone.y() + 0.15, zone.z(),
                    CardConfig.SHAMU_SMOKE_RADIUS, 26, CardFx.GRAY_SMOKE);
            }
        }
    }

    // ==================== 神罚：伤害覆写 ====================

    /** 有神罚标记的玩家的下一次攻击（近战/投射物）固定造成 20 点伤害，命中后标记消耗。 */
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (!ActiveStates.consumePunish(attacker.getUUID())) {
            return;
        }
        event.setNewDamage(CardConfig.SHENFA_DAMAGE);
    }

    // ==================== 魔兔：分裂 / AI / 目标过滤 ====================

    /** 兔子攻击命中（实际造成伤害）后分裂为两只，受同批次上限约束。 */
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof Rabbit rabbit)
            || !(rabbit.level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getInflictedDamage() <= 0) {
            return;
        }
        DaFuNiCard.trySplit(level, rabbit);
    }

    /** 兔子每刻逻辑：到期清除 + 每秒重新索敌。 */
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Rabbit rabbit)
            || !(rabbit.level() instanceof ServerLevel level)) {
            return;
        }
        DaFuNiCard.tick(level, rabbit);
    }

    /** 兔子试图锁定非法目标（非猎人/非敌对生物，含使用者本人、逃生者、其他生物）时直接拦截。 */
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Rabbit rabbit) || rabbit.level().isClientSide()) {
            return;
        }
        if (rabbit.getData(CardState.BUNNY_MARK) == null) {
            return;
        }
        if (ManhuntHook.isValidTarget(event.getNewAboutToBeSetTarget())) {
            return;
        }
        event.setCanceled(true);
    }

    // ==================== 赤鳞：永久属性重挂 / 登出清理 ====================

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    }

    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ActiveStates.clearFor(event.getEntity().getUUID());
    }

}
