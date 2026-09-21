package com.example.skillcards.registry;

import com.example.skillcards.SkillCardsMod;
import com.example.skillcards.item.SkillCardItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 14 张卡牌物品注册。
 */
public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkillCardsMod.MODID);

    private static final List<DeferredItem<SkillCardItem>> ALL = new ArrayList<>();
    private static final Map<Card, DeferredItem<SkillCardItem>> BY_CARD = new EnumMap<>(Card.class);

    public static final DeferredItem<SkillCardItem> XIANGYU = register(Card.XIANGYU);
    public static final DeferredItem<SkillCardItem> SHAMU = register(Card.SHAMU);
    public static final DeferredItem<SkillCardItem> ZHANFANG = register(Card.ZHANFANG);
    public static final DeferredItem<SkillCardItem> SHENFA = register(Card.SHENFA);
    public static final DeferredItem<SkillCardItem> HONGXING = register(Card.HONGXING);
    public static final DeferredItem<SkillCardItem> DAFUNI = register(Card.DAFUNI);
    public static final DeferredItem<SkillCardItem> QILIN = register(Card.QILIN);
    public static final DeferredItem<SkillCardItem> SANSHI = register(Card.SANSHI);
    public static final DeferredItem<SkillCardItem> MANGDIAN = register(Card.MANGDIAN);
    public static final DeferredItem<SkillCardItem> HAIWANG = register(Card.HAIWANG);
    public static final DeferredItem<SkillCardItem> YASITI = register(Card.YASITI);
    public static final DeferredItem<SkillCardItem> ZUZHOU = register(Card.ZUZHOU);
    public static final DeferredItem<SkillCardItem> SHENGSHU = register(Card.SHENGSHU);
    public static final DeferredItem<SkillCardItem> CHILIN = register(Card.CHILIN);

    private static DeferredItem<SkillCardItem> register(Card card) {
        DeferredItem<SkillCardItem> item = ITEMS.registerItem(card.id(),
            properties -> new SkillCardItem(card, properties.stacksTo(1)));
        ALL.add(item);
        BY_CARD.put(card, item);
        return item;
    }

    public static List<DeferredItem<SkillCardItem>> all() {
        return List.copyOf(ALL);
    }

    public static SkillCardItem itemOf(Card card) {
        return BY_CARD.get(card).get();
    }
}
