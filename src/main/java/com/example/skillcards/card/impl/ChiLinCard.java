package com.example.skillcards.card.impl;

import com.example.skillcards.CardConfig;
import com.example.skillcards.card.CardFx;
import com.example.skillcards.data.CardState;
import com.example.skillcards.registry.Card;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * 赤鳞之跃动：使用后永久降低2点饥饿值上限、永久提升4点生命值上限（2颗心），可叠加。
 * 通过持久化附加数据（Attachment，copyOnDeath）跨死亡与重登保留，并在登录/重生/换维度时重挂属性修饰符。
 */
public final class ChiLinCard {
    private ChiLinCard() {}

    public static final Identifier CRIMSON_HEALTH_ID =
        Identifier.fromNamespaceAndPath("skillcards", "chilin_health");

    public static boolean activate(ServerPlayer player) {
        int uses = player.getData(CardState.CRIMSON_USES) + 1;
        player.setData(CardState.CRIMSON_USES, uses);
        applyModifiers(player, uses);

        Vec3 front = CardFx.frontPos(player);
        CardFx.sound(player.level(), player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE);
        CardFx.burst(player.level(), front.x, front.y, front.z, CardFx.RED, 90, 1.3, 0.6);
        CardFx.burst(player.level(), front.x, front.y, front.z, ParticleTypes.FLAME, 40, 1.0, 0.3);
        CardFx.announce(player, "发动", Card.CHILIN);
        return true;
    }

    /** 按使用次数重挂生命上限修饰符（登录/重生/换维度/使用时调用）。 */
    public static void applyModifiers(ServerPlayer player, int uses) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(CRIMSON_HEALTH_ID);
        if (uses > 0) {
            health.addPermanentModifier(new AttributeModifier(CRIMSON_HEALTH_ID,
                CardConfig.CHILIN_HEALTH_BONUS_PER_USE * uses, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** 当前饥饿值上限：20 - 2 x 使用次数。 */
    public static int hungerCap(ServerPlayer player) {
        int uses = player.getData(CardState.CRIMSON_USES);
        int cap = 20 - CardConfig.CHILIN_HUNGER_CAP_REDUCTION_PER_USE * uses;
        return Math.max(cap, CardConfig.CHILIN_HUNGER_CAP_FLOOR);
    }

    /** 管理员指令：重置永久加成（使用次数清零 + 移除属性修饰符）。 */
    public static void reset(ServerPlayer player) {
        player.setData(CardState.CRIMSON_USES, 0);
        applyModifiers(player, 0);
    }
}
