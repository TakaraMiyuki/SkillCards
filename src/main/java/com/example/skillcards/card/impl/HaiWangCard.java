package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.compat.FlyingHook;
import com.example.skillcards.registry.Card;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * 海王之翼：获得"飞翔"模组（flying_enchant）的状态效果（疾跑中按跳跃键向前上方突进），持续20秒；
 * 蓝色+白色粒子持续环绕到效果结束；到期收回并给3秒缓降。
 * 通过注册表软引用 flying_enchant:flying，不与飞翔模组产生编译期依赖。
 */
public final class HaiWangCard {
    private HaiWangCard() {}

    public static boolean activate(ServerPlayer player) {
        Holder<MobEffect> flying = FlyingHook.effect();
        if (flying == null) {
            CardFx.hint(player, "未安装飞翔模组（flying_enchant），无法发动海王之翼");
            return false;
        }
        player.addEffect(new MobEffectInstance(flying, CardConfig.HAIWANG_DURATION_TICKS, 0));
        ActiveStates.setFlight(player.getUUID(), ActiveStates.now() + CardConfig.HAIWANG_DURATION_TICKS);
        ActiveStates.scheduleEndHint(player.getUUID(), Card.HAIWANG,
            ActiveStates.now() + CardConfig.HAIWANG_DURATION_TICKS);

        ServerLevel level = player.level();
        Vec3 front = CardFx.frontPos(player);
        CardFx.burst(level, front.x, front.y, front.z, CardFx.LIGHT_BLUE, 50, 0.9, 0.3);
        CardFx.burst(level, front.x, front.y, front.z, ParticleTypes.END_ROD, 40, 0.8, 0.12);
        // 海洋音效：三叉戟激流（海王的武器破水声）
        CardFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_1);
        CardFx.announce(player, "发动", Card.HAIWANG);
        return true;
    }

    /** 到期处理：给 3 秒缓降（结束提示由 ActiveStates 统一播报；效果被提前移除时同样触发）。 */
    public static void expire(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, CardConfig.HAIWANG_SLOW_FALLING_TICKS, 0));
    }
}
