package com.example.skillcards.item;

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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        boolean used = card().activate(serverPlayer);
        if (used) {
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
        tooltip.accept(colored(Component.translatable("item.skillcards.cooldown", cooldownText()), 0x777777));
        if (shiftDown()) {
            tooltip.accept(Component.empty());
            tooltip.accept(colored(Component.translatable(base + ".desc"), 0xAAAAAA));
        } else {
            tooltip.accept(colored(Component.translatable("item.skillcards.shift_hint"), 0x555555));
        }
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
