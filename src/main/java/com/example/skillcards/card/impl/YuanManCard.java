package com.example.skillcards.card.impl;

import net.minecraft.server.level.ServerPlayer;

/**
 * 圆满（被动）：生命值高于最大生命值的 80% 时，攻击造成的伤害额外提升 2 点。
 */
public final class YuanManCard {
    private YuanManCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }
}
