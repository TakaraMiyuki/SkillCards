package com.example.skillcards.client;

import com.example.skillcards.SkillCardsMod;
import com.example.skillcards.network.GlobalFxState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 客户端：全局效果 HUD——屏幕下沿一行小字，例如 ［剑舞］12/20 ［狂剑］2/5 ［灼烧］1/5。
 * 层数由服务端通过 GlobalFxPayload 周期同步，仅在有任意层数时渲染。
 */
@EventBusSubscriber(modid = SkillCardsMod.MODID, value = Dist.CLIENT)
public final class ClientFx {
    private ClientFx() {}

    @net.neoforged.bus.api.SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(SkillCardsMod.MODID, "global_fx"),
            (gui, deltaTracker) -> render(gui));
    }

    private static void render(net.minecraft.client.gui.GuiGraphicsExtractor gui) {
        int dance = GlobalFxState.dance();
        int kuangjian = GlobalFxState.kuangjian();
        int scorch = GlobalFxState.scorch();
        if (dance + kuangjian + scorch <= 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (dance > 0) {
            sb.append("§b［剑舞］§f").append(dance).append("/20").append("  ");
        }
        if (kuangjian > 0) {
            sb.append("§c［狂剑］§f").append(kuangjian).append("/5").append("  ");
        }
        if (scorch > 0) {
            sb.append("§6［灼烧］§f").append(scorch).append("/5");
        }
        var font = Minecraft.getInstance().font;
        Component line = net.minecraft.network.chat.Component.literal(sb.toString().trim());
        int x = (gui.guiWidth() - font.width(line)) / 2 + com.example.skillcards.CardConfig.HUD_X_OFFSET;
        int y = gui.guiHeight() - com.example.skillcards.CardConfig.HUD_Y_OFFSET;
        gui.text(font, line, x, y, 0xFFFFFFFF);
    }
}
