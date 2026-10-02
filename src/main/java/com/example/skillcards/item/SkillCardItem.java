package com.example.skillcards.item;

import com.example.skillcards.card.CardFx;
import com.example.skillcards.registry.Card;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * 技能卡物品：手持右键发动，成功后进入冷却（图标转灰）。
 * 发动失败（如范围内没有目标）不进入冷却，仅提示。
 *
 * tooltip：常驻显示 稀有度 / 效果简介 / 冷却时间；按住 Shift 追加展开"使用后效果"。
 */
public class SkillCardItem extends Item {
    private final Card card;

    public SkillCardItem(Card card, Properties properties) {
        super(properties);
        this.card = card;
    }

    public Card card() {
        return card;
    }

    /** 所有技能卡附带附魔光效。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (card().isPassive()) {
            CardFx.passiveHint(serverPlayer);
            return InteractionResult.FAIL;
        }
        boolean used = card().activate(serverPlayer);
        // 亚丝缇的赐福：传送成功才进入冷却（蓄力阶段失败可原价重试）
        if (used && !card().deferredCooldown()) {
            serverPlayer.getCooldowns().addCooldown(stack, card().cooldownTicks());
        }
        return used ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public Component getName(ItemStack stack) {
        return colored(Component.translatable(stack.getItem().getDescriptionId()), card.grade().color());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        String base = stack.getItem().getDescriptionId();
        tooltip.accept(colored(Component.translatable(card.grade().translationKey()), card.grade().color()));
        tooltip.accept(colored(Component.translatable(base + ".brief"), 0xFFFFFF));
        if (card().isPassive()) {
            tooltip.accept(colored(Component.translatable("item.skillcards.passive_hint"), 0x77DD77));
        } else {
            tooltip.accept(colored(Component.translatable("item.skillcards.cooldown", cooldownText()), 0x777777));
        }
        if (shiftDown()) {
            tooltip.accept(Component.empty());
            tooltip.accept(colored(Component.translatable(base + ".desc"), 0xAAAAAA));
        for (String global : card().linkedGlobals()) {
            tooltip.accept(colored(Component.translatable("global.skillcards." + global), 0x7FDFFF));
        }
        } else {
            tooltip.accept(colored(Component.translatable("item.skillcards.shift_hint"), 0x555555));
        }
    }

    /** 成功后才结算冷却的卡（延迟结算型）。 */
    public static void applyCooldown(ServerPlayer player, com.example.skillcards.registry.Card card) {
        player.getCooldowns().addCooldown(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                com.example.skillcards.registry.ModItems.itemOf(card)),
            card.cooldownTicks());
    }

    private String cooldownText() {
        int seconds = card().cooldownSeconds();
        return seconds % 60 == 0 ? (seconds / 60) + " 分钟" : seconds + " 秒";
    }

    /** appendHoverText 仅在客户端渲染时调用，此处查询物理键盘的 Shift 状态。 */
    private static boolean shiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static MutableComponent colored(MutableComponent component, int rgb) {
        return component.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }
}
