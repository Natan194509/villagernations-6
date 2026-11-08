package com.example.villagernations.net;

import com.example.villagernations.NationsMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: manda abrir uma tela. tipo 0 = configuração inicial, 1 = painel da nação. */
public record ScreenPayload(int tipo) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NationsMod.MODID, "screen"));

    public static final StreamCodec<FriendlyByteBuf, ScreenPayload> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeVarInt(p.tipo()),
            buf -> new ScreenPayload(buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
