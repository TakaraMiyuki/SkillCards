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
 * 赤鳞之跃动的永久加成（跨死亡、跨重登）；魔兔的归属与到期标记（保证重启后仍能正确清理）。
 */
public final class CardState {
    private CardState() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SkillCardsMod.MODID);

    /** 赤鳞之跃动的使用次数（永久生效：copyOnDeath 保证死亡后仍保留）。 */
    public static final Supplier<AttachmentType<Integer>> CRIMSON_USES =
        ATTACHMENTS.register("crimson_uses", () -> AttachmentType.builder(() -> 0)
            .serialize(Codec.INT.fieldOf("crimson_uses"))
            .copyOnDeath()
            .build());

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
