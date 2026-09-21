package com.example.skillcards.command;

import com.example.skillcards.card.ActiveStates;
import com.example.skillcards.card.impl.ChiLinCard;
import com.example.skillcards.card.impl.DaFuNiCard;
import com.example.skillcards.card.impl.YaSiTiCard;
import com.example.skillcards.card.impl.ZuZhouCard;
import com.example.skillcards.compat.FlyingHook;
import com.example.skillcards.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;

/**
 * 管理员指令（权限等级 2）：
 * /skillcards cleareffects [<targets>] — 清除全部技能效果（含最大生命值改写、永久加成、魔兔）
 * /skillcards refresh [<targets>]       — 立刻刷新背包内全部技能卡的冷却
 */
public final class SkillCardsCommand {
    private SkillCardsCommand() {}

    /** 卡牌会施加的药水效果（cleareffects 时移除；飞翔效果来自 flying_enchant）。 */
    private static final List<Holder<MobEffect>> CARD_EFFECTS = List.of(
        MobEffects.REGENERATION, MobEffects.SLOWNESS, MobEffects.BLINDNESS,
        MobEffects.HEALTH_BOOST, MobEffects.ABSORPTION, MobEffects.SPEED,
        MobEffects.STRENGTH, MobEffects.WEAKNESS, MobEffects.INVISIBILITY, MobEffects.SLOW_FALLING);

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skillcards")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("cleareffects")
                .executes(context -> clearEffects(context.getSource(),
                    java.util.Collections.singleton(context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(context -> clearEffects(context.getSource(),
                        EntityArgument.getPlayers(context, "targets")))))
            .then(Commands.literal("refresh")
                .executes(context -> refreshCooldowns(context.getSource(),
                    java.util.Collections.singleton(context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(context -> refreshCooldowns(context.getSource(),
                        EntityArgument.getPlayers(context, "targets"))))));
    }

    private static int clearEffects(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int bunnies = DaFuNiCard.clearAllBunnies(source.getServer());
        int cleared = 0;
        for (ServerPlayer target : targets) {
            // 内存状态（神罚/飞行/诅咒/圣树/干扰/蓄力/结束提示/冷却记忆）
            ActiveStates.clearFor(target.getUUID());
            // 蓄力减速 + 诅咒最大生命减半
            YaSiTiCard.removeChargeSlow(target);
            ZuZhouCard.expire(target);
            // 赤鳞永久加成（最大生命值改写 + 饥饿上限）
            ChiLinCard.reset(target);
            // 卡牌施加的药水效果
            for (Holder<MobEffect> effect : CARD_EFFECTS) {
                target.removeEffect(effect);
            }
            Holder<MobEffect> flying = FlyingHook.effect();
            if (flying != null) {
                target.removeEffect(flying);
            }
            cleared++;
        }
        final int fCleared = cleared;
        final int fBunnies = bunnies;
        source.sendSuccess(() -> Component.literal(
            "§7[技能卡] §f已清除 " + fCleared + " 名玩家的全部技能效果（魔兔清除 " + fBunnies + " 只）"), true);
        return cleared;
    }

    private static int refreshCooldowns(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int refreshed = 0;
        for (ServerPlayer target : targets) {
            for (var item : ModItems.all()) {
                target.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(item.get()));
            }
            ActiveStates.clearFor(target.getUUID()); // 清冷却记忆集，防止误报"冷却完毕"
            refreshed++;
        }
        final int fRefreshed = refreshed;
        source.sendSuccess(() -> Component.literal(
            "§7[技能卡] §f已刷新 " + fRefreshed + " 名玩家的技能卡冷却"), true);
        return refreshed;
    }
}
