package com.example.skillcards.card;

import com.example.skillcards.CardConfig;
import com.example.skillcards.registry.Card;
import com.example.skillcards.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 粒子 / 音效 / 提示工具。全部使用原版粒子与音效近似还原《卡牌一览》的"使用后外观"。 */
public final class CardFx {
    private CardFx() {}

    // 常用颜色粒子（颜色为 RGB，DustParticleOptions(int color, float scale)）
    public static final DustParticleOptions RED = new DustParticleOptions(0xFF3333, 1.0F);
    public static final DustParticleOptions WHITE = new DustParticleOptions(0xF4F4F4, 1.0F);
    public static final DustParticleOptions LIGHT_BLUE = new DustParticleOptions(0x55AAFF, 1.0F);
    public static final DustParticleOptions DARK_BLUE = new DustParticleOptions(0x3355CC, 1.0F);
    public static final DustParticleOptions PINK = new DustParticleOptions(0xFF77AA, 1.0F);
    public static final DustParticleOptions PURPLE = new DustParticleOptions(0xAA00AA, 1.0F);
    public static final DustParticleOptions YELLOW = new DustParticleOptions(0xFFFF55, 1.0F);
    public static final DustParticleOptions GRAY_SMOKE = new DustParticleOptions(0x444444, 1.6F);

    /** 统一音效：效果结束 / 冷却完毕。 */
    public static final SoundEvent END_SOUND = net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE;
    public static final SoundEvent READY_SOUND = net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP;

    /**
     * 粒子释放基准点：玩家身前半个身位（水平朝向偏移，胸口高度）。
     * 全体粒子效果的位置统一用它，避免第一人称视角下被身体模型遮挡。
     */
    public static Vec3 frontPos(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        double len = Math.sqrt(look.x * look.x + look.z * look.z);
        if (len < 1.0e-4) { // 垂直俯仰时退化为原地
            return new Vec3(player.getX(), player.getY() + 1.0, player.getZ());
        }
        return new Vec3(player.getX() + look.x / len * CardConfig.PARTICLE_FRONT_OFFSET,
            player.getY() + 1.0,
            player.getZ() + look.z / len * CardConfig.PARTICLE_FRONT_OFFSET);
    }

    /** 从某点向四周迸发粒子。 */
    public static void burst(ServerLevel level, double x, double y, double z,
                             ParticleOptions type, int count, double spread, double speed) {
        level.sendParticles(type, x, y, z, count, spread, spread * 0.5, spread, speed);
    }

    /** 以 (x,y,z) 为中心生成一圈环形粒子。 */
    public static void ring(ServerLevel level, double x, double y, double z,
                            double radius, int points, ParticleOptions type) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            level.sendParticles(type, x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius,
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * 向内敛聚粒子（亚丝缇蓄力）：以 (cx,cy,cz) 为中心，在半径 radius 的球面上生成
     * PORTAL 粒子并向中心收拢（count=0 时偏移参数即速度向量，PortalParticle 会从
     * "生成点+偏移"回落到生成点）。
     */
    public static void inwardCollapse(ServerLevel level, double cx, double cy, double cz,
                                      int points, double radius, double phase) {
        for (int i = 0; i < points; i++) {
            double theta = phase + Math.PI * 2 * i / points;
            double dx = Math.cos(theta);
            double dz = Math.sin(theta);
            double dy = (i % 2 == 0) ? 0.7 : -0.5;
            level.sendParticles(ParticleTypes.PORTAL, cx, cy, cz, 0, dx, dy, dz, radius);
        }
    }

    /** 环绕粒子（由每刻扫描调用）：四点双环，环绕中心在玩家身前半个身位。 */
    public static void orbit(ServerLevel level, ServerPlayer player, ParticleOptions type, double radius, double speed) {
        Vec3 center = frontPos(player);
        double angle = (player.tickCount % 40) * (Math.PI / 20);
        for (int i = 0; i < 4; i++) {
            double a = angle + i * Math.PI / 2;
            double r = (i % 2 == 0) ? radius : radius * 0.65;
            double h = (i % 2 == 0) ? 0.0 : 0.45;
            level.sendParticles(type, center.x + Math.cos(a) * r, center.y + h,
                center.z + Math.sin(a) * r, 1, 0.0, 0.05, 0.0, speed);
        }
    }

    /** 头顶红色十字 + 光圈（神罚标记，位置在头顶，不随身前偏移）。 */
    public static void punishMarker(ServerLevel level, ServerPlayer player) {
        double y = player.getEyeY() + 0.6;
        DustParticleOptions dust = RED;
        for (double t = -0.45; t <= 0.45; t += 0.15) {
            level.sendParticles(dust, player.getX() + t, y, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(dust, player.getX(), y, player.getZ() + t, 1, 0.0, 0.0, 0.0, 0.0);
        }
        ring(level, player.getX(), y, player.getZ(), 0.65, 12, dust);
    }

    // ==================== 提示 ====================

    /** 行动栏提示（自由文本，多用于失败原因）。 */
    public static void hint(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§7[技能卡] §f" + message), true);
    }

    /** 标准提示："发动 / 效果结束 / 冷却完毕 + 品级色卡名"。 */
    public static void announce(ServerPlayer player, String action, Card card) {
        MutableComponent name = Component.translatable(ModItems.itemOf(card).getDescriptionId())
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(card.grade().color())));
        player.sendSystemMessage(Component.literal("§7[技能卡] §f" + action + " ").append(name), true);
    }

    // ==================== 音效 ====================

    public static void sound(Level level, double x, double y, double z, SoundEvent sound) {
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void sound(Level level, double x, double y, double z, Holder<SoundEvent> sound) {
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** 兔子消失的烟雾。 */
    public static void poof(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.POOF, x, y, z, 10, 0.25, 0.25, 0.25, 0.02);
    }
}
