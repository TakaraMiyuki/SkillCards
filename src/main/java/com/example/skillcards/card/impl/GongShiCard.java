package com.example.skillcards.card.impl;

import net.minecraft.server.level.ServerPlayer;

/**
 * 攻势（被动）：移动速度加成越高，攻击力越高——
 * 每拥有 x 级迅捷效果，攻击造成的伤害额外提升 2x 点（需正在移动且有迅捷）。
 * 伤害加成见 CardEvents#onDamagePre；脚下白色粒子见 tickGongshiFx。
 */
public final class GongShiCard {
    private GongShiCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }
}
