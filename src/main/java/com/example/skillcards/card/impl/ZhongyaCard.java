package com.example.skillcards.card.impl;

import net.minecraft.server.level.ServerPlayer;

/**
 * 重压（被动）：跳跃攻击造成暴击的伤害倍率由 1.5 提升至 2.0（见 CardEvents#onDamagePre）；
 * 触发时从受击方散射受重力影响的 warped_spore 粒子。
 */
public final class ZhongyaCard {
    private ZhongyaCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }
}
