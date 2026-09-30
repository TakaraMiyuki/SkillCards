package com.example.skillcards.card.impl;

import net.minecraft.server.level.ServerPlayer;

/**
 * 哨兵（被动）：投掷攻击或射击命中低处的敌人时，攻击造成的伤害额外增加 2 点。
 */
public final class ShaobingCard {
    private ShaobingCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }
}
