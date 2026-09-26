package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 隐秘跑鞋：使用后记录 8 秒的行踪（位置与生命值），8 秒后自动回到发动时的位置，
 * 生命值也随之回溯（状态效果不回溯）。蓄力期间无任何粒子表现，
 * 回溯前一瞬与落点各出现蓝色+金色回旋特效。
 */
public final class PaoXieCard {
    private PaoXieCard() {}

    public static boolean activate(ServerPlayer player) {
        if (ActiveStates.rewinds().containsKey(player.getUUID())) {
            CardFx.hint(player, "行踪记录中");
            return false;
        }
        Vec3 pos = player.position();
        ActiveStates.setRewind(player.getUUID(), new ActiveStates.RewindMark(
            player.level().dimension(), pos, player.getYRot(), player.getXRot(),
            player.getHealth(), player.getId(),
            ActiveStates.now() + CardConfig.PAOXIE_REWIND_TICKS));
        CardFx.announce(player, "发动", Card.PAOXIE);
        return true;
    }

    /** 回溯结算（由每刻扫描调用）。 */
    public static void rewind(ServerPlayer player, ActiveStates.RewindMark mark) {
        ServerLevel level = player.level();
        if (player.level().dimension() != mark.dimension()) {
            CardFx.hint(player, "回溯失败：已跨维度");
            return;
        }
        CardFx.rewindSwirl(level, player.getX(), player.getY() + 1.0, player.getZ());
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT);
        player.teleportTo(mark.pos().x, mark.pos().y, mark.pos().z);
        player.resetFallDistance();
        player.setHealth(Math.min(mark.health(), player.getMaxHealth()));
        player.removeEffect(MobEffects.POISON);
        CardFx.rewindSwirl(level, mark.pos().x, mark.pos().y + 1.0, mark.pos().z);
        CardFx.sound(level, mark.pos().x, mark.pos().y, mark.pos().z, SoundEvents.ENDERMAN_TELEPORT);
    }
}
