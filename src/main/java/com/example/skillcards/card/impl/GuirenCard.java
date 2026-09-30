package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import com.example.skillcards.systems.GlobalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * 鬼人：立刻获得 12 层剑舞（全局效果）；30 秒内每次近战攻击命中再叠加 1 层剑舞（上限 20），
 * 每层剑舞提升 3% 近战攻击速度；红色粒子持续缠绕。
 */
public final class GuirenCard {
    private GuirenCard() {}

    public static boolean activate(ServerPlayer player) {
        GlobalEffects.addDance(player, CardConfig.GUIREN_INSTANT_LAYERS);
        ActiveStates.setGuiren(player.getUUID(), ActiveStates.now() + CardConfig.GUIREN_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.GUIREN,
            ActiveStates.now() + CardConfig.GUIREN_DURATION_TICKS);

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.RED, 70, 1.0, 0.4);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.PINK, 40, 0.8, 0.3);
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN);
        CardFx.announce(player, "发动", Card.GUIREN);
        return true;
    }
}
