package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.systems.GlobalEffects;
import net.minecraft.server.level.ServerPlayer;

/**
 * 狂舞（被动）：每次近战攻击命中敌人叠加 1 层剑舞（全局效果）；
 * 每拥有 4 层剑舞，转换为 1 层狂剑并清空所有剑舞；每层狂剑使近战伤害 +1 点；
 * 连续 1.5 秒没有叠加剑舞则清空所有狂剑。狂剑层数越多环绕的红色粒子越多。
 */
public final class KuangwuCard {
    private KuangwuCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }

    /** 近战命中时的叠层与转换（由 CardEvents#onDamagePost 调用）。 */
    public static void onMeleeHit(ServerPlayer player) {
        GlobalEffects.addDance(player, 1);
        var state = ActiveStates.kuangwuState().get(player.getUUID());
        long now = ActiveStates.now();
        if (state == null || now - state.lastStack() >= CardConfig.KUANGWU_CLEAR_TICKS) {
            ActiveStates.kuangwuState().remove(player.getUUID());
        }
        while (GlobalEffects.tryConvertDance(player, CardConfig.KUANGWU_DANCE_PER_CONVERT)) {
            var cur = ActiveStates.kuangwuState().getOrDefault(player.getUUID(),
                new GlobalEffects.Kuangwu(0, now));
            ActiveStates.kuangwuState().put(player.getUUID(),
                new GlobalEffects.Kuangwu(cur.layers() + 1, now));
        }
    }

    /** 当前狂剑层数（1.5 秒未叠剑舞即归零）。 */
    public static int kuangjianLayers(ServerPlayer player) {
        var state = ActiveStates.kuangwuState().get(player.getUUID());
        if (state == null || ActiveStates.now() - state.lastStack() >= CardConfig.KUANGWU_CLEAR_TICKS) {
            return 0;
        }
        return state.layers();
    }

    /** 狂剑环绕粒子：层数越多粒子越多（上限 4 层）。 */
    public static void orbitFx(ServerPlayer player) {
        int layers = Math.min(4, kuangjianLayers(player));
        for (int i = 0; i <= layers; i++) {
            CardFx.orbit(player.level(), player, CardFx.RED, 0.55 + i * 0.25, 0.02);
        }
    }
}
