package com.example.skillcards.card.impl;

import net.minecraft.server.level.ServerPlayer;

/**
 * 遁走之术（被动）：获得隐身效果时，获得 2 级迅捷与 3 级跳跃提升（30 秒）。
 * 由 CardEvents#onEffectAdded（MobEffectEvent.Added）触发；不与更高的同类药效叠加
 * （原版 addEffect 对已有更强效果不做覆盖）。
 */
public final class DunZouCard {
    private DunZouCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }
}
