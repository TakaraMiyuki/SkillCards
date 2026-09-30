package com.example.skillcards.card.impl;

import net.minecraft.server.level.ServerPlayer;

/**
 * 背刺（被动）：攻击玩家时，若攻击对象背对自身，则伤害提升至 1.5 倍。
 */
public final class BeiShiCard {
    private BeiShiCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }
}
