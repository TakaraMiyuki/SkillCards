package com.example.skillcards.card.impl;

import net.minecraft.server.level.ServerPlayer;

/**
 * 荆棘（被动）：自身永久获得等同于荆棘 IV 的效果——
 * 受到近战攻击时，攻击者受到 2 点荆棘伤害（见 CardEvents#onDamagePost）。
 */
public final class ZhenjiCard {
    private ZhenjiCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }
}
