package com.example.skillcards.registry;

import com.example.skillcards.card.impl.ChiLinCard;
import com.example.skillcards.card.impl.DaFuNiCard;
import com.example.skillcards.card.impl.HaiWangCard;
import com.example.skillcards.card.impl.HongXingCard;
import com.example.skillcards.card.impl.MangDianCard;
import com.example.skillcards.card.impl.QiLinCard;
import com.example.skillcards.card.impl.SanShiCard;
import com.example.skillcards.card.impl.ShaMuCard;
import com.example.skillcards.card.impl.ShenFaCard;
import com.example.skillcards.card.impl.ShengShuCard;
import com.example.skillcards.card.impl.XiangYuCard;
import com.example.skillcards.card.impl.YaSiTiCard;
import com.example.skillcards.card.impl.ZhanFangCard;
import com.example.skillcards.card.impl.ZuZhouCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

/**
 * 14 张技能卡：物品 id、稀有度、冷却（秒）与发动逻辑的映射。
 * 发动逻辑返回 false 表示"无效发动"（如范围内没有猎人），此时不进入冷却。
 */
public enum Card {
    XIANGYU("xiangyu", Grade.COMMON, 180, XiangYuCard::activate),
    SHAMU("shamu", Grade.COMMON, 180, ShaMuCard::activate),
    ZHANFANG("zhanfang", Grade.COMMON, 180, ZhanFangCard::activate),
    SHENFA("shenfa", Grade.COMMON, 180, ShenFaCard::activate),
    HONGXING("hongxing", Grade.COMMON, 120, HongXingCard::activate),

    DAFUNI("dafuni", Grade.RARE, 480, DaFuNiCard::activate),
    QILIN("qilin", Grade.RARE, 180, QiLinCard::activate),
    SANSHI("sanshi", Grade.RARE, 300, SanShiCard::activate),
    MANGDIAN("mangdian", Grade.RARE, 300, MangDianCard::activate),

    HAIWANG("haiwang", Grade.RAINBOW, 120, HaiWangCard::activate),
    YASITI("yasiti", Grade.RARE, 300, YaSiTiCard::activate),

    ZUZHOU("zuzhou", Grade.BLACK, 300, ZuZhouCard::activate),
    SHENGSHU("shengshu", Grade.BLACK, 300, ShengShuCard::activate),
    CHILIN("chilin", Grade.BLACK, 300, ChiLinCard::activate);

    private final String id;
    private final Grade grade;
    private final int cooldownSeconds;
    private final Function<ServerPlayer, Boolean> action;

    Card(String id, Grade grade, int cooldownSeconds, Function<ServerPlayer, Boolean> action) {
        this.id = id;
        this.grade = grade;
        this.cooldownSeconds = cooldownSeconds;
        this.action = action;
    }

    public String id() {
        return id;
    }

    public Grade grade() {
        return grade;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public int cooldownTicks() {
        return cooldownSeconds * 20;
    }

    public boolean activate(ServerPlayer player) {
        return action.apply(player);
    }

    /** 卡牌品级：决定名称颜色与物品稀有度。 */
    public enum Grade {
        COMMON("common", 0xE8E8E8, Rarity.COMMON),
        RARE("rare", 0x55FFFF, Rarity.RARE),
        RAINBOW("rainbow", 0xFF55FF, Rarity.EPIC),
        BLACK("black", 0xAA0000, Rarity.EPIC);

        private final String id;
        private final int color;
        private final Rarity itemRarity;

        Grade(String id, int color, Rarity itemRarity) {
            this.id = id;
            this.color = color;
            this.itemRarity = itemRarity;
        }

        public String id() {
            return id;
        }

        /** 名称颜色（RGB）。 */
        public int color() {
            return color;
        }

        public Rarity itemRarity() {
            return itemRarity;
        }

        public String translationKey() {
            return "card.skillcards.grade." + id;
        }
    }
}
