package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.compat.ManhuntHook;
import com.example.skillcards.registry.Card;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * 本能模式：洞察范围内猎人的动向——范围内所有猎人获得红色轮廓的发光（60 秒），
 * 自己获得速度 I（10 秒）；白色声呐粒子向外扩散 10 格。
 * 红色轮廓通过独立计分板队伍（skillcards_red_glow）实现，到期自动移出队伍。
 */
public final class BenEngCard {
    private BenEngCard() {}

    public static final String RED_GLOW_TEAM = "skillcards_red_glow";

    public static boolean activate(ServerPlayer player) {
        List<LivingEntity> targets = ManhuntHook.targetsAround(player, CardConfig.BENENG_RADIUS);
        if (targets.isEmpty()) {
            CardFx.hint(player, "周围 " + (int) CardConfig.BENENG_RADIUS + " 格内没有" + ManhuntHook.targetNoun());
            return false;
        }
        long now = ActiveStates.now();
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                CardConfig.BENENG_GLOW_DURATION_TICKS, 0));
            if (target instanceof ServerPlayer sp) {
                ActiveStates.setRedGlow(sp.getUUID(), now + CardConfig.BENENG_GLOW_DURATION_TICKS);
                applyRedTeam(sp);
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,
            CardConfig.BENENG_SPEED_DURATION_TICKS, 0));

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        CardFx.sonarRings(level, front.x, front.y, front.z);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BELL);
        CardFx.announce(player, "发动", Card.BENENG);
        return true;
    }

    private static void applyRedTeam(ServerPlayer player) {
        var scoreboard = player.level().getScoreboard();
        var team = scoreboard.getPlayerTeam(RED_GLOW_TEAM);
        if (team == null) {
            team = scoreboard.addPlayerTeam(RED_GLOW_TEAM);
            team.setColor(Optional.of(net.minecraft.world.scores.TeamColor.RED));
        }
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
    }

    /** 红色发光到期（或发光被移除）时把目标移出红色队伍（由每刻扫描调用）。 */
    public static void clearRedGlow(MinecraftServer server, java.util.UUID id) {
        ActiveStates.redGlows().remove(id);
        var team = server.getScoreboard().getPlayerTeam(RED_GLOW_TEAM);
        if (team != null && team.getPlayers().contains(id.toString())) {
            server.getScoreboard().removePlayerFromTeam(id.toString(), team);
        }
    }
}
