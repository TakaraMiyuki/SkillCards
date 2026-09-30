package com.example.skillcards;

/**
 * 全部可调数值。改动后重新构建 jar 即可生效。
 * 冷却时间 / 持续时间 / 半径 / 伤害等与《卡牌一览》对应。
 */
public final class CardConfig {
    private CardConfig() {}

    // ==================== 响玉：生命恢复4 x 8秒 ====================
    public static final int XIANGYU_DURATION_TICKS = 8 * 20;
    public static final int XIANGYU_AMPLIFIER = 3; // 等级4

    // ==================== 纱幕：周围30格猎人缓慢2 x 10秒 + 失明 x 10秒 ====================
    public static final int SHAMU_RADIUS = 30;
    public static final int SHAMU_SLOWNESS_DURATION_TICKS = 10 * 20;
    public static final int SHAMU_SLOWNESS_AMPLIFIER = 1; // 等级2
    public static final int SHAMU_BLINDNESS_DURATION_TICKS = 10 * 20;

    // ==================== 绽放：弹开周围猎人 + 生命提升5 x 10秒 + 伤害吸收5 x 30秒 ====================
    public static final double ZHANFANG_KNOCKBACK_RADIUS = 5.0;
    public static final double ZHANFANG_KNOCKBACK_HORIZONTAL = 1.6; // 击飞约5~6格
    public static final double ZHANFANG_KNOCKBACK_VERTICAL = 0.5;
    public static final int ZHANFANG_ABSORPTION_DURATION_TICKS = 30 * 20;
    public static final int ZHANFANG_ABSORPTION_AMPLIFIER = 4; // 等级5

    // ==================== 神罚：下一次攻击造成20点伤害，持续30秒 ====================
    public static final float SHENFA_DAMAGE = 20.0F;
    public static final int SHENFA_DURATION_TICKS = 20 * 20;

    // ==================== 虹星：目视方向冲刺约7格 + 速度4 x 5秒 ====================
    public static final double HONGXING_DASH_POWER = 1.2; // 初速（格/刻），空中阻力下约合12格
    public static final int HONGXING_SPEED_DURATION_TICKS = 5 * 20;
    public static final int HONGXING_SPEED_AMPLIFIER = 3; // 等级4

    // ==================== 达芙妮之灾厄：20只魔兔，1血/1伤，攻击后分裂，120秒 ====================
    public static final int DAFUNI_BUNNY_COUNT = 20;
    public static final double DAFUNI_BUNNY_MAX_HEALTH = 1.0;  // 一滴血
    public static final double DAFUNI_BUNNY_ATTACK_DAMAGE = 1.0; // 攻击1点
    public static final double DAFUNI_AGGRO_RANGE = 16.0;      // 兔子索敌半径
    public static final int DAFUNI_CAST_LIMIT = 60;            // 同批次存活上限（防指数爆炸）
    public static final int DAFUNI_LIFETIME_TICKS = 120 * 20;
    public static final double DAFUNI_SPAWN_MIN_RADIUS = 2.0;
    public static final double DAFUNI_SPAWN_MAX_RADIUS = 5.0;
    public static final double DAFUNI_SPLIT_COUNT_RADIUS = 64.0; // 分裂前统计同批次存活兔的扫描半径

    // ==================== 麒麟：精准雷击周围10格的猎人，每个猎人两道，间隔1秒 ====================
    public static final double QILIN_RADIUS = 10.0;
    public static final int QILIN_STRIKE_COUNT = 2;             // 每个目标两道雷
    public static final int QILIN_STRIKE_INTERVAL_TICKS = 10;   // 每道间隔0.5秒

    // ==================== 三世秘传：范围内每个猎人各被偷一组 ====================
    public static final double SANSHI_RADIUS = 300.0;
    public static final boolean SANSHI_EXCLUDE_CORE_ITEMS = true; // 不偷 Manhunt 罗盘等核心道具

    // ==================== 盲点：猎人罗盘乱指60秒 + 自身隐身30秒 ====================
    public static final int MANGDIAN_JAM_DURATION_TICKS = 60 * 20;
    public static final int MANGDIAN_INVISIBILITY_DURATION_TICKS = 30 * 20;
    public static final int MANGDIAN_JAM_RANGE = 1000; // 乱指的假坐标偏移半径（格）

    // ==================== 海王之翼：飞翔20秒，到期给3秒缓降 ====================
    public static final int HAIWANG_DURATION_TICKS = 20 * 20;
    public static final int HAIWANG_SLOW_FALLING_TICKS = 3 * 20;

    // ==================== 艾丝缇的赐福：蓄力5秒后随机传送(200,300)格 ====================
    public static final int YASITI_CHARGE_TICKS = 5 * 20;
    public static final double YASITI_MIN_DISTANCE = 201.0;
    public static final double YASITI_MAX_DISTANCE = 299.0; // 表格要求大于200小于300
    public static final double YASITI_CHARGE_SLOW_RATIO = -0.6; // 蓄力期间移速降低60%
    public static final int YASITI_FIND_RETRIES = 8; // 安全落点重试次数

    // ==================== 诅咒化身：力量4 x 60秒，期间最大生命值-50% ====================
    public static final int ZUZHOU_DURATION_TICKS = 60 * 20;
    public static final int ZUZHOU_STRENGTH_AMPLIFIER = 3; // 等级4
    public static final double ZUZHOU_HEALTH_MULTIPLIER = -0.5;

    // ==================== 圣树化身：生命恢复3 x 60秒 + 虚弱2 x 60秒 ====================
    public static final int SHENGSHU_DURATION_TICKS = 60 * 20;
    public static final int SHENGSHU_REGENERATION_AMPLIFIER = 2; // 等级3
    public static final int SHENGSHU_WEAKNESS_AMPLIFIER = 0; // 等级1

    // ==================== 赤鳞之跃动：分段扣血20点 + 30秒五强化 ====================
    /** 赤鳞之跃动：发动扣除的生命值（现值不足则扣至最后 2 点）。 */
    public static final float CHILIN_HEALTH_COST = 20.0F;
    /** 赤鳞之跃动：扣血保留的最低生命值。 */
    public static final float CHILIN_MIN_HEALTH = 2.0F;
    /** 赤鳞之跃动：每段扣血量（分段掉血营造连续受击感）。 */
    public static final float CHILIN_DRAIN_CHUNK_HP = 2.0F;
    /** 赤鳞之跃动：分段扣血的间隔（刻）。 */
    public static final int CHILIN_DRAIN_INTERVAL_TICKS = 2;
    /** 赤鳞之跃动：强化持续时间（秒）。 */
    public static final int CHILIN_BUFF_SECONDS = 30;

    // ==================== 纱幕：原地黑灰烟雾团 ====================
    public static final double SHAMU_SMOKE_RADIUS = 6.0;       // 烟雾团半径（格）
    public static final double SHAMU_SMOKE_HEIGHT = 4.0;       // 烟雾团高度（格）
    public static final int SHAMU_SMOKE_DURATION_TICKS = 30 * 20;
    public static final int SHAMU_SMOKE_WAVE_INTERVAL_TICKS = 3; // 粒子生成波次间隔

    // ==================== 达芙妮：魔兔移速与攻击 ====================
    public static final double DAFUNI_BUNNY_MOVEMENT_SPEED = 0.45; // 滞空跳跃移速（配合牵引逼近疾跑）
    /** 魔兔每刻向目标施加的牵引加速度（水平；地面摩擦下稳定在 DAFUNI_MAX_SPEED 附近）。 */
    public static final double DAFUNI_PULL_ACCEL = 0.06;
    /** 魔兔牵引后的水平速度上限（格/刻，约 6 m/s > 玩家疾跑 5.6 m/s）。 */
    public static final double DAFUNI_MAX_SPEED = 0.30;
    /** 魔兔牵引的贴身停止半径（格，贴脸后不再加速）。 */
    public static final double DAFUNI_PULL_STOP_DIST = 2.0;
    /** 魔兔攻击命中附加的饥饿时长（刻，3 秒）。 */
    public static final int DAFUNI_HUNGER_TICKS = 60;

    // ==================== 本能模式：红色发光洞察 ====================
    public static final double BENENG_RADIUS = 200.0;
    public static final int BENENG_GLOW_DURATION_TICKS = 60 * 20;
    public static final int BENENG_SPEED_DURATION_TICKS = 10 * 20;

    // ==================== 霜凪：冰封最近的一个猎人 ====================
    public static final double SHUANGNI_RADIUS = 20.0;
    public static final int SHUANGNI_DURATION_TICKS = 10 * 20;
    public static final int SHUANGNI_SLOWNESS_AMPLIFIER = 3; // 等级4
    public static final int SHUANGNI_FATIGUE_AMPLIFIER = 2;  // 等级3
    public static final float SHUANGNI_DAMAGE_PER_SECOND = 0.5F;
    public static final int SHUANGNI_FROZEN_TICKS = 140; // 冻结血条满档

    // ==================== 隐秘跑鞋：8秒回溯 ====================
    public static final int PAOXIE_REWIND_TICKS = 8 * 20;

    // ==================== 哈斯卡之狂战 ====================
    public static final int HUSKAR_DURATION_TICKS = 60 * 20;
    public static final float HUSKAR_ATTACK_COST = 2.0F;   // 每次攻击命中损失的生命
    public static final float HUSKAR_MIN_HEALTH = 1.0F;    // 卖血下限
    public static final int HUSKAR_SWEEP_INTERVAL_TICKS = 10;

    // ==================== 钓鱼翁 ====================
    public static final int DIAOYU_DURATION_TICKS = 10 * 20;
    public static final int DIAOYU_RESISTANCE_AMPLIFIER = 3; // 等级4
    public static final int DIAOYU_SLOWNESS_AMPLIFIER = 2;   // 等级3

    // ==================== 青龙形态 ====================
    public static final int QINGLONG_DURATION_TICKS = 30 * 20;
    public static final int QINGLONG_JUMP_AMPLIFIER = 5;  // 等级6
    public static final int QINGLONG_SPEED_AMPLIFIER = 2; // 等级3
    public static final float QINGLONG_FALL_REDUCTION = 0.5F; // 落地伤害减半

    // ==================== 过载运转 ====================
    public static final int GUOZAI_DURATION_TICKS = 30 * 20;
    public static final int GUOZAI_SPEED_AMPLIFIER = 2;        // 等级3
    public static final double GUOZAI_ATTACK_SPEED_BONUS = 0.30; // 攻速+30%
    public static final int GUOZAI_AFTER_SLOWNESS_TICKS = 10 * 20;

    // ==================== 全局效果：灼烧 ====================
    public static final int SCORCH_MAX_LAYERS = 5;
    public static final int SCORCH_STACK_INTERVAL_TICKS = 60;      // 着火时每3秒自动+1层
    public static final int SCORCH_WINDOW_AFTER_FIRE_TICKS = 60;   // 火熄灭后再存续3秒

    // ==================== 全局效果：剑舞 ====================
    public static final int DANCE_MAX_LAYERS = 20;
    public static final double DANCE_SPEED_PER_LAYER = 0.03;       // 每层+3%近战攻速
    public static final int DANCE_DURATION_TICKS = 60;             // 每次叠加后存续3秒

    // ==================== 被动卡 ====================
    public static final int GONGSHI_DAMAGE_PER_LEVEL = 2;  // 攻势：每级迅捷+2点
    public static final float ZHENJI_REFLECT_DAMAGE = 2.0F; // 荆棘：反弹伤害
    public static final double ZHONGYA_CRIT_MULTIPLIER = 2.0; // 重压：跳劈倍率 1.5→2.0
    public static final int XUSHI_IDLE_TICKS = 20 * 20;      // 蓄势：20秒未近战
    public static final double XUSHI_MULTIPLIER = 2.0;       // 蓄势：下一击翻倍
    public static final int XUSHI_SOUND_MIN_TICKS = 6;       // 蓄势心跳：0.3秒
    public static final int XUSHI_SOUND_MAX_TICKS = 16;      // 蓄势心跳：0.8秒
    public static final double YUANMAN_HEALTH_RATIO = 0.8;   // 圆满：生命>80%
    public static final int YUANMAN_DAMAGE_BONUS = 2;        // 圆满：+2点
    public static final int SHAOBING_DAMAGE_BONUS = 2;       // 哨兵：低处目标+2点
    public static final double BEISHI_MULTIPLIER = 1.5;      // 背刺：1.5倍
    public static final int DUOCHONG_EXTRA_ARROWS = 2;       // 多重箭：额外2支（共3支）
    public static final double DUOCHONG_SPREAD = 0.08;       // 多重箭散布强度
    public static final int FALUN_ADAPT_TICKS = 30 * 20;     // 法轮：持续30秒后免疫

    // ==================== 鬼人 / 沸血之矛 / 狂舞 ====================
    public static final int GUIREN_DURATION_TICKS = 30 * 20;
    public static final int GUIREN_INSTANT_LAYERS = 12;
    public static final int FEIXUE_FIRE_SECONDS = 4;
    public static final int KUANGWU_CLEAR_TICKS = 30;        // 1.5秒没叠剑舞清空狂剑
    public static final int KUANGWU_DANCE_PER_CONVERT = 4;   // 每4层剑舞换1层狂剑

    // ==================== 燃火 / 臂章 / 吊射 ====================
    public static final double RANHUO_RADIUS = 3.0;
    public static final int RANHUO_FIRE_SECONDS = 10;
    public static final int RANHUO_SCORCH_CONVERT_DAMAGE = 6; // 每层灼烧转换的火焰伤害
    public static final double BIZHANG_RATIO = 0.8;          // 臂章：扣除80%生命
    public static final int BIZHANG_DURATION_TICKS = 6 * 20;
    public static final int DIAOSHE_WINDOW_TICKS = 30 * 20;  // 吊射：30秒发动窗口
    public static final int DIAOSHE_RAIN_DURATION_TICKS = 3 * 20;
    public static final int DIAOSHE_RAIN_INTERVAL_TICKS = 4;
    public static final double DIAOSHE_ARROW_DAMAGE = 6.0;
    public static final int DIAOSHE_RAIN_HEIGHT = 20;
    public static final double DIAOSHE_HALF_AREA = 1.0;      // 3×3 半宽1格

    // ==================== 通用 ====================
    public static final double PARTICLE_FRONT_OFFSET = 0.5; // 全体粒子释放位置向玩家身前偏移（半身位）

    public static final int ORBIT_PARTICLE_INTERVAL_TICKS = 10; // 环绕粒子的刷新间隔
    public static final int PUNISH_MARKER_INTERVAL_TICKS = 10; // 神罚头顶标记的刷新间隔
}
