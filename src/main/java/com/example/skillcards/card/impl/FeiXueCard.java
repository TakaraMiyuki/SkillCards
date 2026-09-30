package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.systems.GlobalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 沸血之矛（被动）：每次攻击命中时损失 2 点生命值，敌人获得 4 秒着火；
 * 若敌人没有灼烧状态，还会叠加一层灼烧（全局效果）。
 */
public final class FeiXueCard {
    private FeiXueCard() {}

    public static boolean activate(ServerPlayer player) {
        return false;
    }

    /** 攻击命中结算（由 CardEvents#onDamagePost 调用）。 */
    public static void payAndIgnite(ServerPlayer attacker, net.minecraft.world.entity.LivingEntity target) {
        float health = attacker.getHealth();
        if (health > 1.0F) {
            attacker.setHealth(Math.max(1.0F, health - 2.0F));
        }
        target.igniteForSeconds(CardConfig.FEIXUE_FIRE_SECONDS);
        if (GlobalEffects.scorchLayers(target) == 0) {
            GlobalEffects.addScorch(target, 1);
        }
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(CardFx.RED, target.getX(), target.getY() + 1.0, target.getZ(),
                10, 0.3, 0.4, 0.3, 0.05);
        }
    }
}
