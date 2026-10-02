package com.example.skillcards.network;

import com.example.skillcards.SkillCardsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：同步玩家当前的全局效果层数（剑舞/狂剑/灼烧），供 HUD 小字显示。
 */
public record GlobalFxPayload(int dance, int kuangjian, int scorch) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GlobalFxPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SkillCardsMod.MODID, "global_fx"));

    public static final StreamCodec<FriendlyByteBuf, GlobalFxPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, GlobalFxPayload::dance,
        ByteBufCodecs.VAR_INT, GlobalFxPayload::kuangjian,
        ByteBufCodecs.VAR_INT, GlobalFxPayload::scorch,
        GlobalFxPayload::new);

    @Override
    public CustomPacketPayload.Type<GlobalFxPayload> type() {
        return TYPE;
    }
}
