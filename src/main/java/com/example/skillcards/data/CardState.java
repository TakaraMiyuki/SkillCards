package com.example.skillcards.data;

import com.example.skillcards.SkillCardsMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 玩家/实体的持久化附加数据。
 * 魔兔的归属与到期标记（保证重启后仍能正确清理）。
 */
public final class CardState {
    private CardState() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SkillCardsMod.MODID);

    /** 多重箭：克隆箭标记（用于吊射联动与雨箭识别）。 */
    public static final Supplier<AttachmentType<Boolean>> DUOCHONG_CLONE =
        ATTACHMENTS.register("duochong_clone", () -> AttachmentType.builder(() -> false).build());

    /** 吊射：雨箭标记（伤害固定 6 点，不受下落加速影响）。 */
    public static final Supplier<AttachmentType<Boolean>> RAIN_ARROW =
        ATTACHMENTS.register("rain_arrow", () -> AttachmentType.builder(() -> false).build());

    /** 魔兔标记：归属者、施法批次与到期时间。 */
    public record BunnyMark(UUID owner, UUID castId, long expiry) {
        public static final MapCodec<BunnyMark> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(BunnyMark::owner),
            UUIDUtil.CODEC.fieldOf("cast_id").forGetter(BunnyMark::castId),
            Codec.LONG.fieldOf("expiry").forGetter(BunnyMark::expiry)
        ).apply(instance, BunnyMark::new));
    }

    public static final Supplier<AttachmentType<BunnyMark>> BUNNY_MARK =
        ATTACHMENTS.register("bunny_mark", () -> AttachmentType.<BunnyMark>builder(() -> null)
            .serialize(BunnyMark.MAP_CODEC)
            .build());
}
