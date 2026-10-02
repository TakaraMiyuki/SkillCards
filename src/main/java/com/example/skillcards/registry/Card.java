package com.example.skillcards.registry;

import com.example.skillcards.card.impl.BenEngCard;
import com.example.skillcards.card.impl.BeiShiCard;
import com.example.skillcards.card.impl.BizhangCard;
import com.example.skillcards.card.impl.ChiLinCard;
import com.example.skillcards.card.impl.DaFuNiCard;
import com.example.skillcards.card.impl.DiaoYuCard;
import com.example.skillcards.card.impl.QiLinCard;
import com.example.skillcards.card.impl.DiaoSheCard;
import com.example.skillcards.card.impl.DunZouCard;
import com.example.skillcards.card.impl.DuochongCard;
import com.example.skillcards.card.impl.EShaCard;
import com.example.skillcards.card.impl.FaLunCard;
import com.example.skillcards.card.impl.FeiXueCard;
import com.example.skillcards.card.impl.GongShiCard;
import com.example.skillcards.card.impl.GuirenCard;
import com.example.skillcards.card.impl.GuoZaiCard;
import com.example.skillcards.card.impl.HaiWangCard;
import com.example.skillcards.card.impl.HongXingCard;
import com.example.skillcards.card.impl.HuskarCard;
import com.example.skillcards.card.impl.KuangwuCard;
import com.example.skillcards.card.impl.MangDianCard;
import com.example.skillcards.card.impl.PaoXieCard;
import com.example.skillcards.card.impl.QingLongCard;
import com.example.skillcards.card.impl.RanhuoCard;
import com.example.skillcards.card.impl.SanShiCard;
import com.example.skillcards.card.impl.ShaMuCard;
import com.example.skillcards.card.impl.ShaobingCard;
import com.example.skillcards.card.impl.ShenFaCard;
import com.example.skillcards.card.impl.ShengShuCard;
import com.example.skillcards.card.impl.ShuangNiCard;
import com.example.skillcards.card.impl.XiangYuCard;
import com.example.skillcards.card.impl.XushiCard;
import com.example.skillcards.card.impl.YaSiTiCard;
import com.example.skillcards.card.impl.YuanManCard;
import com.example.skillcards.card.impl.ZhanFangCard;
import com.example.skillcards.card.impl.ZhenjiCard;
import com.example.skillcards.card.impl.ZhongyaCard;
import com.example.skillcards.card.impl.ZuZhouCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

/**
 * 技能卡：物品 id、稀有度、类型（主动/被动）、冷却（秒）与发动逻辑。
 * 主动卡需要手持右键发动；被动卡放入背包即自动生效（冷却 0）。
 * 发动逻辑返回 false 表示"无效发动"，主动卡此时不进入冷却。
 */
public enum Card {
    // ==================== 主动卡 ====================
    XIANGYU("xiangyu", Grade.COMMON, Type.ACTIVE, 120, XiangYuCard::activate),
    SHAMU("shamu", Grade.COMMON, Type.ACTIVE, 160, ShaMuCard::activate),
    ZHANFANG("zhanfang", Grade.COMMON, Type.ACTIVE, 80, ZhanFangCard::activate),
    SHENFA("shenfa", Grade.COMMON, Type.ACTIVE, 100, ShenFaCard::activate),
    HONGXING("hongxing", Grade.COMMON, Type.ACTIVE, 60, HongXingCard::activate),

    DAFUNI("dafuni", Grade.RARE, Type.ACTIVE, 300, DaFuNiCard::activate),
    QILIN("qilin", Grade.RARE, Type.ACTIVE, 100, QiLinCard::activate),
    SANSHI("sanshi", Grade.RARE, Type.ACTIVE, 120, SanShiCard::activate),
    MANGDIAN("mangdian", Grade.RARE, Type.ACTIVE, 180, MangDianCard::activate),
    YASITI("yasiti", Grade.RARE, Type.ACTIVE, 240, YaSiTiCard::activate),
    BENENG("beneng", Grade.RARE, Type.ACTIVE, 100, BenEngCard::activate),
    SHUANGNI("shuangni", Grade.COMMON, Type.ACTIVE, 160, ShuangNiCard::activate),
    PAOXIE("paoxie", Grade.RARE, Type.ACTIVE, 100, PaoXieCard::activate),

    HAIWANG("haiwang", Grade.RAINBOW, Type.ACTIVE, 60, HaiWangCard::activate),
    QINGLONG("qinglong", Grade.RAINBOW, Type.ACTIVE, 80, QingLongCard::activate),
    DIAOSHE("diaoshe", Grade.RAINBOW, Type.ACTIVE, 100, DiaoSheCard::activate),

    ZUZHOU("zuzhou", Grade.BLACK, Type.ACTIVE, 180, ZuZhouCard::activate),
    SHENGSHU("shengshu", Grade.BLACK, Type.ACTIVE, 180, ShengShuCard::activate),
    CHILIN("chilin", Grade.BLACK, Type.ACTIVE, 180, ChiLinCard::activate),
    HUSKAR("huskar", Grade.BLACK, Type.ACTIVE, 120, HuskarCard::activate),
    DIAOYU("diaoyu", Grade.BLACK, Type.ACTIVE, 100, DiaoYuCard::activate),
    GUOZAI("guozai", Grade.BLACK, Type.ACTIVE, 120, GuoZaiCard::activate),

    RANHUO("ranhuo", Grade.COMMON, Type.ACTIVE, 60, RanhuoCard::activate),
    GUIREN("guiren", Grade.COMMON, Type.ACTIVE, 120, GuirenCard::activate),
    BIZHANG("bizhang", Grade.RARE, Type.ACTIVE, 120, BizhangCard::activate),

    // ==================== 被动卡 ====================
    GONGSHI("gongshi", Grade.COMMON, Type.PASSIVE, 0, GongShiCard::activate),
    ZHENJI("zhenji", Grade.COMMON, Type.PASSIVE, 0, ZhenjiCard::activate),
    ZHONGYA("zhongya", Grade.COMMON, Type.PASSIVE, 0, ZhongyaCard::activate),
    XUSHI("xushi", Grade.COMMON, Type.PASSIVE, 0, XushiCard::activate),
    YUANMAN("yuanman", Grade.COMMON, Type.PASSIVE, 0, YuanManCard::activate),
    SHAOBING("shaobing", Grade.COMMON, Type.PASSIVE, 0, ShaobingCard::activate),
    ESHA("esha", Grade.COMMON, Type.PASSIVE, 0, EShaCard::activate),
    BEISHI("beishi", Grade.COMMON, Type.PASSIVE, 0, BeiShiCard::activate),

    DUOCHONG("duochong", Grade.RARE, Type.PASSIVE, 0, DuochongCard::activate),
    FALUN("falun", Grade.RARE, Type.PASSIVE, 0, FaLunCard::activate),
    DUNZOU("dunzou", Grade.RARE, Type.PASSIVE, 0, DunZouCard::activate),
    KUANGWU("kuangwu", Grade.RARE, Type.PASSIVE, 0, KuangwuCard::activate),

    FEIXUE("feixue", Grade.BLACK, Type.PASSIVE, 0, FeiXueCard::activate);

    private final String id;
    private final Grade grade;
    private final Type type;
    private final int cooldownSeconds;
    private final Function<ServerPlayer, Boolean> action;

    Card(String id, Grade grade, Type type, int cooldownSeconds, Function<ServerPlayer, Boolean> action) {
        this.id = id;
        this.grade = grade;
        this.type = type;
        this.cooldownSeconds = cooldownSeconds;
        this.action = action;
    }

    public String id() {
        return id;
    }

    public Grade grade() {
        return grade;
    }

    public Type type() {
        return type;
    }

    public boolean isPassive() {
        return type == Type.PASSIVE;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public int cooldownTicks() {
        return cooldownSeconds * 20;
    }

    /** 被动卡没有发动逻辑，activate 恒返回 false。 */
    public boolean activate(ServerPlayer player) {
        return action.apply(player);
    }

    /** 冷却是否延迟结算（如亚丝缇的赐福：传送成功才进冷却）。 */
    public boolean deferredCooldown() {
        return this == YASITI;
    }

    /** 关联的全局效果（tooltip 中单独展示其描述）。键见 lang：global.skillcards.* */
    public java.util.List<String> linkedGlobals() {
        return switch (this) {
            case KUANGWU -> java.util.List.of("dance", "kuangjian");
            case GUIREN -> java.util.List.of("dance");
            case QILIN, RANHUO, FEIXUE -> java.util.List.of("scorch");
            default -> java.util.List.of();
        };
    }

    /** 卡牌类型：主动（右键发动）/ 被动（背包内自动生效）。 */
    public enum Type {
        ACTIVE, PASSIVE
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
