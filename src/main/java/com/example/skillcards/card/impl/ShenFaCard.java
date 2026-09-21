package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/** 神罚：下一次攻击将造成20点伤害（10颗心），持续30秒；头顶出现红色十字+红色光圈。 */
public final class ShenFaCard {
    private ShenFaCard() {}

    public static boolean activate(ServerPlayer player) {
        ActiveStates.setPunish(player.getUUID(), ActiveStates.now() + CardConfig.SHENFA_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.SHENFA,
            ActiveStates.now() + CardConfig.SHENFA_DURATION_TICKS);
        CardFx.punishMarker(player.level(), player);
        CardFx.sound(player.level(), player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT);
        CardFx.announce(player, "发动", Card.SHENFA);
        return true;
    }
}
