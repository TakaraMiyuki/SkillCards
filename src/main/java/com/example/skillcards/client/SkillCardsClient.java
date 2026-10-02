package com.example.skillcards.client;

import com.example.skillcards.SkillCardsMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * 仅客户端入口：注册全局效果 HUD 图层。
 * 26.2 的客户端专用注册写法（@Mod dist = CLIENT），专用服务端不会加载本类。
 */
@Mod(value = SkillCardsMod.MODID, dist = Dist.CLIENT)
public final class SkillCardsClient {
    // HUD 图层由 ClientFx 上的 @EventBusSubscriber 自动注册；
    // 本类仅作为 26.2 客户端专用入口存在——不要在这里再手动 addListener，
    // 否则同一图层 id 会被注册两次（RegisterGuiLayersEvent 抛 IllegalArgumentException 崩溃）。
    public SkillCardsClient(IEventBus modEventBus) {
    }
}
