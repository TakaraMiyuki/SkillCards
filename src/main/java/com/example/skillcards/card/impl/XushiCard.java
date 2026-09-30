package com.example.skillcards.card.impl;

import net.minecraft.server.level.ServerPlayer;

/**
 * 蓄势（被动）：连续 20 秒没有造成任何近战伤害，下一次近战攻击伤害翻倍
 * （见 CardEvents#onDamagePre / tickXushiHeartbeat）。
 */
public final class XushiCard {
    private XushiCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }
}
