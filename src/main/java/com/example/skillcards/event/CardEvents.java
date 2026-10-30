package com.example.skillcards.event;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.card.impl.BenEngCard;
import com.example.skillcards.card.impl.BizhangCard;
import com.example.skillcards.card.impl.DiaoSheCard;
import com.example.skillcards.card.impl.DuochongCard;
import com.example.skillcards.card.impl.FaLunCard;
import com.example.skillcards.systems.GlobalEffects;
import com.example.skillcards.card.impl.ChiLinCard;
import com.example.skillcards.card.impl.DaFuNiCard;
import com.example.skillcards.card.impl.DiaoYuCard;
import com.example.skillcards.card.impl.GuoZaiCard;
import com.example.skillcards.card.impl.HuskarCard;
import com.example.skillcards.card.impl.PaoXieCard;
import com.example.skillcards.card.impl.KuangwuCard;
import com.example.skillcards.card.impl.FeiXueCard;
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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

    private static final Map<UUID, Long> FULL_BOW_SHOTS = new java.util.HashMap<>(); // 吊射：满蓄力弓射击标记
    private static final Map<UUID, Long> LAST_FX_SENT = new java.util.HashMap<>();  // HUD：上次发送的层数包

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
            logger.info("[SkillCards][冒烟测试] 已注册卡牌物品 {}/{}", count, ModItems.all().size());
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
        tickGlowSuppress(now);
        tickRedGlow(server, now);
        tickFrost(now);
        tickRewind(now);
        tickHuskar(now);
        tickDiaoyu(now);
        tickQinglong(now);
        tickGuozai(now);
        tickPassives(server, now);
        GlobalEffects.tickScorch(now);
        GlobalEffects.tickDance(now);
        tickBizhang(now);
        tickFalun(server, now);
        tickXushiHeartbeat(server, now);
        tickGongshiFx(server, now);
        tickRainZones(server, now);
        tickFalunFx(now);

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

    /** 艾丝缇的赐福：内敛粒子 + 传送前一瞬炸开 + 到期传送（死亡/下线取消）。 */
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

    // ==================== 被动卡 / 全局系统 ====================

    /** 每 0.5 秒重算玩家背包中的被动卡集合（含副手）。 */
    private static void tickPassives(MinecraftServer server, long now) {
        if (now % 10 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            java.util.Set<Card> found = new HashSet<>();
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (stack.getItem() instanceof SkillCardItem cardItem && cardItem.card().isPassive()) {
                    found.add(cardItem.card());
                }
            }
            ItemStack offhandStack = player.getItemInHand(InteractionHand.OFF_HAND);
            if (offhandStack.getItem() instanceof SkillCardItem offhandCard && offhandCard.card().isPassive()) {
                found.add(offhandCard.card());
            }
            // 饰品栏（Curios）中的被动卡同样生效
            com.example.skillcards.compat.CuriosBridge.collectPassives(player, found);
            ActiveStates.setPassives(player.getUUID(), found);
            // 法轮：未装备（背包/副手/饰品栏都没有）时清空适应记录，重新装备后重新适应
            if (!found.contains(Card.FALUN)) {
                ActiveStates.falunFirst().remove(player.getUUID());
                ActiveStates.falunAdapted().remove(player.getUUID());
            }
            syncGlobalFx(player);
        }
    }

    /** 全局效果层数同步：变化才发包（HUD 用）。 */
    private static void syncGlobalFx(ServerPlayer player) {
        int dance = GlobalEffects.danceLayers(player);
        int kuangjian = KuangwuCard.kuangjianLayers(player);
        int scorch = GlobalEffects.scorchLayers(player);
        long packed = ((long) dance << 20) | ((long) kuangjian << 10) | scorch;
        Long last = LAST_FX_SENT.get(player.getUUID());
        if (last != null && last == packed) {
            return;
        }
        LAST_FX_SENT.put(player.getUUID(), packed);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new com.example.skillcards.network.GlobalFxPayload(dance, kuangjian, scorch));
    }

    /** 攻势：正在移动且拥有迅捷时，脚下零散白色粒子。 */
    private static void tickGongshiFx(MinecraftServer server, long now) {
        if (now % 8 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ActiveStates.hasPassive(player.getUUID(), Card.GONGSHI) || !player.hasEffect(MobEffects.SPEED)) {
                continue;
            }
            Vec3 delta = player.getDeltaMovement();
            if (delta.x * delta.x + delta.z * delta.z > 0.004) {
                player.level().sendParticles(CardFx.WHITE,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.6,
                    player.getY() + 0.1,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.6,
                    2, 0.1, 0.05, 0.1, 0.01);
            }
        }
    }

    /** 恶煞：每个负面效果提供 1 级生命恢复。 */
    private static void tickEsha(MinecraftServer server, long now) {
        if (now % 40 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ActiveStates.hasPassive(player.getUUID(), Card.ESHA)) {
                continue;
            }
            int harmful = 0;
            for (var instance : player.getActiveEffects()) {
                if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    harmful++;
                }
            }
            if (harmful > 0) {
                // ambient=true 不刷粒子，visible=true 让图标显示（之前 visible=false 导致看不出效果）
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, harmful - 1, true, true));
                if (CardConfig.DEBUG_LOGGING && now % 200 == 0) {
                    com.mojang.logging.LogUtils.getLogger().info(
                        "[SkillCards][恶煞] {} 当前 {} 个负面效果 → 生命恢复 {} 级",
                        player.getName().getString(), harmful, harmful);
                }
            }
        }
    }


    /** 蓄势：技能就绪时以 0.3s/0.8s 交替节奏向持有者播放心跳气泡音。 */
    private static void tickXushiHeartbeat(MinecraftServer server, long now) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ActiveStates.hasPassive(player.getUUID(), Card.XUSHI)) {
                continue;
            }
            Long last = ActiveStates.lastMelee(player.getUUID());
            boolean armed = last == null || now - last >= CardConfig.XUSHI_IDLE_TICKS;
            var sound = ActiveStates.xushiSound().get(player.getUUID());
            if (!armed) {
                ActiveStates.xushiSound().remove(player.getUUID());
                continue;
            }
            if (sound == null) {
                // 刚进入"就绪"：右手小爆发 + 开始心跳
                Vec3 look = player.getLookAngle().normalize();
                Vec3 hand = player.position().add(look.scale(0.45))
                    .add(-look.z * 0.3, 1.2, look.x * 0.3);
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(CardFx.WHITE, hand.x, hand.y, hand.z, 8, 0.15, 0.1, 0.15, 0.02);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        hand.x, hand.y, hand.z, 5, 0.1, 0.08, 0.1, 0.02);
                }
                ActiveStates.xushiSound().put(player.getUUID(), new ActiveStates.XushiBeat(now + CardConfig.XUSHI_SOUND_MIN_TICKS, false));
                continue;
            }
            if (now >= sound.until()) {
                player.playSound(SoundEvents.BUBBLE_POP, 0.8F, sound.alternate() ? 1.2F : 0.9F);
                Vec3 look = player.getLookAngle().normalize();
                Vec3 hand = player.position().add(look.scale(0.45))
                    .add(-look.z * 0.3, 1.2, look.x * 0.3);
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(CardFx.WHITE, hand.x, hand.y, hand.z, 3, 0.1, 0.06, 0.1, 0.02);
                }
                ActiveStates.xushiSound().put(player.getUUID(),
                    sound.alternate() ? new ActiveStates.XushiBeat(now + CardConfig.XUSHI_SOUND_MAX_TICKS, false)
                        : new ActiveStates.XushiBeat(now + CardConfig.XUSHI_SOUND_MIN_TICKS, true));
            }
        }
    }

    /** 法轮：负面效果持续追踪；满 30 秒达成适应并播放音波粒子。 */
    private static void tickFalun(MinecraftServer server, long now) {
        if (now % 20 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ActiveStates.hasPassive(player.getUUID(), Card.FALUN)) {
                continue;
            }
            var firstSeen = ActiveStates.falunFirst().computeIfAbsent(player.getUUID(), k -> new java.util.HashMap<>());
            var adapted = ActiveStates.falunAdapted(player.getUUID());
            java.util.Set<Holder<MobEffect>> active = new java.util.HashSet<>();
            for (var instance : player.getActiveEffects()) {
                if (instance.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
                    continue;
                }
                Holder<MobEffect> effect = instance.getEffect();
                active.add(effect);
                firstSeen.putIfAbsent(effect, now);
                if (now - firstSeen.get(effect) >= CardConfig.FALUN_ADAPT_TICKS && adapted.add(effect)) {
                    // 达成适应：立刻移除当前持续中的该负面效果（此后新的施加也会被 Applicable 拦截）
                    player.removeEffect(effect);
                    FaLunCard.adaptFx(player);
                    ActiveStates.falunFx().put(player.getUUID(), now + 20);
                    if (CardConfig.DEBUG_LOGGING) {
                        com.mojang.logging.LogUtils.getLogger().info(
                            "[SkillCards][法轮] {} 已适应负面效果 {}（此后免疫）",
                            player.getName().getString(), effect.value().getDescriptionId());
                    }
                }
                // 已适应的效果：持续移除（保证"不会再受到该效果影响"）
                if (adapted.contains(effect) && player.hasEffect(effect)) {
                    player.removeEffect(effect);
                }
            }
            // 尚未适应的效果中断后重置计时；已适应的持久保留（重新施加直接免疫）
            firstSeen.keySet().removeIf(effect -> !active.contains(effect) && !adapted.contains(effect));
        }
    }

    /** 法轮适应粒子的短窗口播放。 */
    private static void tickFalunFx(long now) {
        var iterator = ActiveStates.falunFx().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            if (now % 4 == 0) {
                ServerPlayer player = ActiveStates.player(entry.getKey());
                if (player != null) {
                    player.level().sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM,
                        player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.2,
                        player.getEyeY() + 0.4,
                        player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.2,
                        1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    /** 臂章：6 秒后移除吸收并返还生命。 */
    private static void tickBizhang(long now) {
        var iterator = ActiveStates.bizhangs().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now < entry.getValue().until()) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player != null) {
                BizhangCard.restore(player, entry.getValue().deducted());
            }
        }
    }

    /** 吊射：窗口结算 + 箭雨区推进。 */
    private static void tickRainZones(MinecraftServer server, long now) {
        ActiveStates.diaosheWindows().entrySet().removeIf(entry -> now >= entry.getValue());
        var iterator = ActiveStates.rainZones().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ActiveStates.RainZone zone = entry.getValue();
            if (now >= zone.until()) {
                iterator.remove();
                continue;
            }
            if (!(server.getLevel(zone.dimension()) instanceof ServerLevel level)) {
                iterator.remove();
                continue;
            }
            if (now % CardConfig.DIAOSHE_RAIN_INTERVAL_TICKS == 0) {
                DiaoSheCard.tickRain(level, zone);
            }
        }
    }

    // ==================== 新卡事件监听 ====================

    /** 多重箭：玩家射出的箭进入世界时克隆 2 支带散布的箭（跳过雨箭与克隆箭——它们没有武器来源）。 */
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Arrow arrow)) {
            return;
        }
        // 只认"由武器射出"的箭：雨箭/克隆箭没有武器来源，天然跳过，也不会递归
        if (arrow.getWeaponItem() == null
            || !(arrow.getOwner() instanceof ServerPlayer shooter)) {
            return;
        }
        if (!ActiveStates.hasPassive(shooter.getUUID(), Card.DUOCHONG)) {
            return;
        }
        DuochongCard.spawnClones(shooter, arrow);
    }

    /** 遁走之术：获得隐身时强化移动（不覆盖更高的已有药效）。 */
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !ActiveStates.hasPassive(player.getUUID(), Card.DUNZOU)) {
            return;
        }
        var instance = event.getEffectInstance();
        if (instance != null && instance.getEffect().equals(MobEffects.INVISIBILITY)) {
            int dur = Math.max(instance.getDuration(), 20 * 30);
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, dur, 2));
        }
    }

    /** 法轮：已适应的负面效果不再施加。 */
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !ActiveStates.hasPassive(player.getUUID(), Card.FALUN)) {
            return;
        }
        var instance = event.getEffectInstance();
        if (instance == null || !FaLunCard.isHarmful(instance.getEffect())) {
            return;
        }
        var firstSeen = ActiveStates.falunFirst().get(player.getUUID());
        if (firstSeen != null) {
            Long seen = firstSeen.get(instance.getEffect());
            if (seen != null && ActiveStates.now() - seen >= CardConfig.FALUN_ADAPT_TICKS) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        }
    }

    /** 吊射：满蓄力弓射击标记（供落点判定）。 */
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getLevel().isClientSide() || !event.hasAmmo()) {
            return;
        }
        if (ActiveStates.diaosheActive(event.getEntity().getUUID(), ActiveStates.now())
            && event.getCharge() >= 20) {
            FULL_BOW_SHOTS.put(event.getEntity().getUUID(), ActiveStates.now() + 40);
        }
    }

    /** 吊射：箭矢命中方块（落点）或命中实体（实体所在地）都触发箭雨；一根箭只触发一场。 */
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide()
            || !(event.getProjectile() instanceof Arrow arrow)
            || !(arrow.getOwner() instanceof ServerPlayer shooter)) {
            return;
        }
        if (DiaoSheCard.alreadyTriggered(arrow)) {
            return;
        }
        long now = ActiveStates.now();
        boolean weaponArrow = arrow.getWeaponItem() != null;
        boolean cloneArrow = Boolean.TRUE.equals(arrow.getData(com.example.skillcards.data.CardState.DUOCHONG_CLONE));
        Vec3 center = arrow.position();
        if (event.getRayTraceResult().getType() == HitResult.Type.ENTITY
            && event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
            center = entityHit.getEntity().position(); // 命中实体：以实体当时的所在地为目标
        }
        if (weaponArrow) {
            // 普通武器箭：弩或满蓄力弓触发
            boolean crossbow = arrow.getWeaponItem().is(net.minecraft.world.item.Items.CROSSBOW);
            Long bowUntil = FULL_BOW_SHOTS.get(shooter.getUUID());
            boolean fullBow = bowUntil != null && now < bowUntil;
            if (!ActiveStates.diaosheActive(shooter.getUUID(), now) || !(crossbow || fullBow)) {
                return;
            }
        } else if (cloneArrow) {
            // 多重箭×吊射联动：克隆箭每根都是一次箭雨落点
            if (!ActiveStates.diaosheActive(shooter.getUUID(), now)
                || !ActiveStates.hasPassive(shooter.getUUID(), Card.DUOCHONG)) {
                return;
            }
        } else {
            return; // 雨箭等其他箭不触发
        }
        DiaoSheCard.markTriggered(arrow);
        DiaoSheCard.spawnRain(shooter, center);
    }

    // ==================== 盲点：发光压制 ====================

    /** 逐刻压制自己身上的发光（Manhunt 每 40 刻重挂，需持续移除）；结束后由 Manhunt 刷新自然恢复。 */
    private static void tickGlowSuppress(long now) {
        var iterator = ActiveStates.glowSuppress().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null || now >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            if (player.hasEffect(MobEffects.GLOWING)) {
                player.removeEffect(MobEffects.GLOWING);
            }
        }
    }

    /** 本能模式：红色发光到期回收队伍；发光被提前移除（牛奶）时同样回收。 */
    private static void tickRedGlow(MinecraftServer server, long now) {
        var iterator = ActiveStates.redGlows().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            boolean expired = now >= entry.getValue();
            boolean glowGone = player == null || !player.hasEffect(MobEffects.GLOWING);
            if (expired || glowGone) {
                iterator.remove();
                BenEngCard.clearRedGlow(server, entry.getKey());
            }
        }
    }

    /** 霜凪：冻结血条拉满、冰晶轮廓与每秒 0.5 点冻结伤害。 */
    private static void tickFrost(long now) {
        var iterator = ActiveStates.frosts().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!(findEntity(entry.getKey()) instanceof LivingEntity target) || !target.isAlive()) {
                iterator.remove();
                continue;
            }
            if (now >= entry.getValue().expiry()) {
                iterator.remove();
                continue;
            }
            if (target.level() instanceof ServerLevel level) {
                if (now % 5 == 0) {
                    target.setTicksFrozen(CardConfig.SHUANGNI_FROZEN_TICKS);
                    CardFx.frostBox(level, target.getBoundingBox().inflate(0.15));
                }
                if (now % 20 == 0) {
                    target.hurtServer(level, level.damageSources().freeze(),
                        CardConfig.SHUANGNI_DAMAGE_PER_SECOND);
                }
            }
        }
    }

    /** 隐秘跑鞋：8 秒后自动回溯（死亡/下线/重生的记录作废）。 */
    private static void tickRewind(long now) {
        var iterator = ActiveStates.rewinds().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ActiveStates.RewindMark mark = entry.getValue();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null || player.isDeadOrDying() || player.getId() != mark.entityId()) {
                iterator.remove();
                continue;
            }
            long remaining = mark.expiry() - now;
            if (remaining == 2) {
                CardFx.rewindSwirl(player.level(), player.getX(), player.getY() + 1.0, player.getZ());
                continue;
            }
            if (remaining <= 0) {
                iterator.remove();
                PaoXieCard.rewind(player, mark);
            }
        }
    }

    /** 哈斯卡：档位增益刷新 + 火焰环绕 + 到期移除攻速。 */
    private static void tickHuskar(long now) {
        var iterator = ActiveStates.huskars().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (now >= entry.getValue()) {
                iterator.remove();
                HuskarCard.expire(player);
                continue;
            }
            if (now % CardConfig.HUSKAR_SWEEP_INTERVAL_TICKS == 0) {
                HuskarCard.refreshTier(player);
            }
            if (now % CardConfig.ORBIT_PARTICLE_INTERVAL_TICKS == 0) {
                CardFx.orbitStyle(player.level(), player, "huskar");
            }
        }
    }

    /** 钓鱼翁：环绕粒子。 */
    private static void tickDiaoyu(long now) {
        var iterator = ActiveStates.diaoyus().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null || now >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            if (now % CardConfig.ORBIT_PARTICLE_INTERVAL_TICKS == 0) {
                CardFx.orbitStyle(player.level(), player, "diaoyu");
            }
        }
    }

    /** 青龙形态：环绕粒子 + 到期回收（摔落减免随之结束）。 */
    private static void tickQinglong(long now) {
        var iterator = ActiveStates.qinglongs().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null || now >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            if (now % CardConfig.ORBIT_PARTICLE_INTERVAL_TICKS == 0) {
                CardFx.orbitStyle(player.level(), player, "qinglong");
            }
        }
    }

    /** 过载运转：蒸汽环绕 + 到期结算透支。 */
    private static void tickGuozai(long now) {
        var iterator = ActiveStates.guozais().entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = ActiveStates.player(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (now >= entry.getValue()) {
                iterator.remove();
                GuoZaiCard.expire(player);
                continue;
            }
            if (now % CardConfig.ORBIT_PARTICLE_INTERVAL_TICKS == 0) {
                CardFx.orbitStyle(player.level(), player, "guozai");
            }
        }
    }

    // ==================== 神罚：伤害覆写 ====================

    /** 有神罚标记的玩家的下一次攻击（近战/投射物）固定造成 20 点伤害，命中后标记消耗。 */
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        // 青龙形态：摔落伤害减半
        if (event.getSource().is(DamageTypes.FALL) && event.getEntity() instanceof ServerPlayer qinglongPlayer) {
            Long qinglongUntil = ActiveStates.qinglongs().get(qinglongPlayer.getUUID());
            if (qinglongUntil != null && ActiveStates.now() < qinglongUntil) {
                event.setNewDamage(event.getNewDamage() * CardConfig.QINGLONG_FALL_REDUCTION);
            }
        }

        // 吊射：雨箭固定 6 点伤害（下落加速会让 速度×基础伤害 超标）
        if (event.getSource().getDirectEntity() instanceof Arrow rainArrow
            && Boolean.TRUE.equals(rainArrow.getData(com.example.skillcards.data.CardState.RAIN_ARROW))) {
            event.setNewDamage((float) CardConfig.DIAOSHE_ARROW_DAMAGE);
        }

        // 灼烧：着火目标受到的火焰伤害每层 +1 点
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && event.getEntity().isOnFire()) {
            int scorchLayers = GlobalEffects.scorchLayers(event.getEntity());
            if (scorchLayers > 0) {
                if (CardConfig.DEBUG_LOGGING) {
                    com.mojang.logging.LogUtils.getLogger().info(
                        "[SkillCards][灼烧] {} 火焰伤害 {} + {} 层 = {}",
                        event.getEntity().getName().getString(), event.getNewDamage(),
                        scorchLayers, event.getNewDamage() + scorchLayers);
                }
                event.setNewDamage(event.getNewDamage() + scorchLayers);
            }
        }

        // 攻击方增益结算
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        java.util.Set<Card> passives = ActiveStates.passives(attacker.getUUID());
        DamageSource damageSource = event.getSource();
        Entity direct = damageSource.getDirectEntity();
        boolean melee = direct != null && direct == damageSource.getEntity() && direct instanceof LivingEntity;

        double damage = event.getNewDamage();
        double additive = 0.0;
        double multiplier = 1.0;

        // 攻势：正在移动且拥有迅捷 → 每级 +2 点
        if (passives.contains(Card.GONGSHI) && attacker.hasEffect(MobEffects.SPEED)) {
            Vec3 delta = attacker.getDeltaMovement();
            if (delta.x * delta.x + delta.z * delta.z > 0.004) {
                additive += CardConfig.GONGSHI_DAMAGE_PER_LEVEL
                    * (attacker.getEffect(MobEffects.SPEED).getAmplifier() + 1);
            }
        }
        // 圆满：生命高于 80% → +2 点
        if (passives.contains(Card.YUANMAN)
            && attacker.getHealth() > attacker.getMaxHealth() * CardConfig.YUANMAN_HEALTH_RATIO) {
            additive += CardConfig.YUANMAN_DAMAGE_BONUS;
        }
        // 哨兵：投射物命中低处的目标 → +2 点
        if (passives.contains(Card.SHAOBING) && direct instanceof Projectile
            && event.getEntity().getY() < attacker.getY()) {
            additive += CardConfig.SHAOBING_DAMAGE_BONUS;
        }
        if (melee) {
            // 狂剑：每层 +1 点
            additive += KuangwuCard.kuangjianLayers(attacker);
            // 重压：跳跃攻击暴击倍率 1.5 → 2.0
            if (passives.contains(Card.ZHONGYA) && !attacker.onGround() && attacker.fallDistance > 0) {
                multiplier *= CardConfig.ZHONGYA_CRIT_MULTIPLIER / 1.5;
                if (event.getEntity().level() instanceof ServerLevel spLevel) {
                    spLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WARPED_SPORE,
                        event.getEntity().getX(), event.getEntity().getY() + 1.0, event.getEntity().getZ(),
                        12, 0.4, 0.3, 0.4, 0.02);
                }
            }
            // 蓄势：20 秒未造成近战伤害 → 翻倍
            if (passives.contains(Card.XUSHI)) {
                Long last = ActiveStates.lastMelee(attacker.getUUID());
                if (last == null || ActiveStates.now() - last >= CardConfig.XUSHI_IDLE_TICKS) {
                    multiplier *= CardConfig.XUSHI_MULTIPLIER;
                    ActiveStates.setLastMelee(attacker.getUUID(), ActiveStates.now());
                }
            }
            // 背刺：攻击背对自身的实体 → 1.5 倍
            if (passives.contains(Card.BEISHI) && event.getEntity() instanceof LivingEntity victim) {
                Vec3 look = victim.getLookAngle();
                Vec3 toAttacker = attacker.position().subtract(victim.position());
                if (look.x * toAttacker.x + look.z * toAttacker.z < 0) {
                    multiplier *= CardConfig.BEISHI_MULTIPLIER;
                }
            }
        }

        // 神罚：下一次攻击固定 20 点
        if (ActiveStates.consumePunish(attacker.getUUID())) {
            damage = CardConfig.SHENFA_DAMAGE;
        }

        event.setNewDamage((float) ((damage + additive) * multiplier));
    }

    // ==================== 魔兔：分裂 / AI / 目标过滤 ====================

    /** 兔子攻击命中（实际造成伤害）后分裂为两只，受同批次上限约束。 */
    public static void onDamagePost(LivingDamageEvent.Post event) {
        // 哈斯卡之狂战：攻击命中扣血
        if (event.getSource().getEntity() instanceof ServerPlayer huskarAttacker
            && event.getInflictedDamage() > 0) {
            Long huskarUntil = ActiveStates.huskars().get(huskarAttacker.getUUID());
            if (huskarUntil != null && ActiveStates.now() < huskarUntil) {
                HuskarCard.payCost(huskarAttacker);
            }
        }
        DamageSource damageSource = event.getSource();
        boolean meleeHit = damageSource.getEntity() instanceof ServerPlayer
            && damageSource.getDirectEntity() == damageSource.getEntity()
            && event.getInflictedDamage() > 0;
        // 蓄势：记录近战命中时刻
        if (meleeHit) {
            ActiveStates.setLastMelee(((ServerPlayer) damageSource.getEntity()).getUUID(), ActiveStates.now());
        }
        // 狂舞：近战命中叠剑舞并转换狂剑；鬼人：命中再叠 1 层
        if (meleeHit && damageSource.getEntity() instanceof ServerPlayer dancer) {
            if (ActiveStates.hasPassive(dancer.getUUID(), Card.KUANGWU)) {
                KuangwuCard.onMeleeHit(dancer);
            }
            if (ActiveStates.guiren(dancer.getUUID()) != null) {
                GlobalEffects.addDance(dancer, 1);
            }
        }
        // 沸血之矛：命中扣血 + 点燃 + 灼烧
        if (meleeHit && damageSource.getEntity() instanceof ServerPlayer feixueAttacker
            && ActiveStates.hasPassive(feixueAttacker.getUUID(), Card.FEIXUE)
            && event.getEntity() instanceof LivingEntity feixueTarget) {
            FeiXueCard.payAndIgnite(feixueAttacker, feixueTarget);
        }
        // 荆棘：受到近战攻击反弹 2 点（荆棘来源不再反弹，防止互相刷）
        if (event.getEntity() instanceof ServerPlayer thornsVictim
            && ActiveStates.hasPassive(thornsVictim.getUUID(), Card.ZHENJI)
            && !damageSource.is(DamageTypes.THORNS)
            && damageSource.getDirectEntity() instanceof LivingEntity meleeAttacker
            && meleeAttacker != thornsVictim
            && thornsVictim.distanceToSqr(meleeAttacker) <= 16.0
            && thornsVictim.level() instanceof ServerLevel thornsLevel) {
            meleeAttacker.hurtServer(thornsLevel, thornsLevel.damageSources().thorns(thornsVictim),
                CardConfig.ZHENJI_REFLECT_DAMAGE);
        }
        if (!(event.getSource().getDirectEntity() instanceof Rabbit rabbit)
            || !(rabbit.level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getInflictedDamage() <= 0) {
            return;
        }
        // 魔兔命中附加 3 秒饥饿
        if (rabbit.getData(CardState.BUNNY_MARK) != null && event.getEntity().isAlive()) {
            event.getEntity().addEffect(new MobEffectInstance(
                MobEffects.HUNGER, CardConfig.DAFUNI_HUNGER_TICKS, 0), rabbit);
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
        LAST_FX_SENT.remove(event.getEntity().getUUID());
    }

}
