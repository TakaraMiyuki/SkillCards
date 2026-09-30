package com.example.skillcards.card.impl;

import net.minecraft.server.level.ServerPlayer;

/**
 * 恶煞（被动）：每拥有 x 个负面效果，就获得 x 级生命恢复（见 tickEsha）。
 */
public final class EShaCard {
    private EShaCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }
}
