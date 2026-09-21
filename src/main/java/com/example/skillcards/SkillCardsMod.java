package com.example.skillcards;

import com.example.skillcards.data.CardState;
import com.example.skillcards.event.CardEvents;
import com.example.skillcards.registry.ModItems;
import com.example.skillcards.registry.ModTabs;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(SkillCardsMod.MODID)
public final class SkillCardsMod {
    public static final String MODID = "skillcards";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SkillCardsMod(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModTabs.TABS.register(modEventBus);
        CardState.ATTACHMENTS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(CardEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(CardEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(CardEvents::onServerStopping);
        NeoForge.EVENT_BUS.addListener(CardEvents::onDamagePre);
        NeoForge.EVENT_BUS.addListener(CardEvents::onDamagePost);
        NeoForge.EVENT_BUS.addListener(CardEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(CardEvents::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(CardEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CardEvents::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(CardEvents::onDimensionChange);
        NeoForge.EVENT_BUS.addListener(CardEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(com.example.skillcards.command.SkillCardsCommand::onRegisterCommands);

        LOGGER.info("[SkillCards] 技能卡模组初始化完成（14 张卡）");
    }
}
