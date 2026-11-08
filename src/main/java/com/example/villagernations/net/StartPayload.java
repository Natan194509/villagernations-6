package com.example.villagernations.net;

import com.example.villagernations.NationsMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cliente -> servidor: configurações escolhidas na tela inicial para fundar a vila. */
public record StartPayload(String nome, int villagers, int esmeraldas, int dificuldade)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StartPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NationsMod.MODID, "start"));

    public static final StreamCodec<FriendlyByteBuf, StartPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.nome(), 32);
                buf.writeVarInt(p.villagers());
                buf.writeVarInt(p.esmeraldas());
                buf.writeVarInt(p.dificuldade());
            },
            buf -> new StartPayload(buf.readUtf(32), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
