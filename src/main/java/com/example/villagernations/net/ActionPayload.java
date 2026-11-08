package com.example.villagernations.net;

import com.example.villagernations.NationsMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cliente -> servidor: uma ação feita no painel ou pelas teclas. */
public record ActionPayload(int acao, int valor) implements CustomPacketPayload {
    public static final int PROF_MAIS = 1;
    public static final int PROF_MENOS = 2;
    public static final int IMPOSTO_MAIS = 3;
    public static final int IMPOSTO_MENOS = 4;
    public static final int RECRUTAR = 5;
    public static final int SALARIO_MAIS = 6;
    public static final int SALARIO_MENOS = 7;
    public static final int AVANCAR = 8;
    /** Posiciona o edifício (valor = tipo) onde o jogador está mirando. */
    public static final int CONSTRUIR_AQUI = 10;
    public static final int CANCELAR_OBRA = 11;
    public static final int HAB_PROF = 12;
    public static final int HAB_ORDEM = 13;
    public static final int PAGINA = 14;

    public static final CustomPacketPayload.Type<ActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NationsMod.MODID, "action"));

    public static final StreamCodec<FriendlyByteBuf, ActionPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.acao());
                buf.writeVarInt(p.valor());
            },
            buf -> new ActionPayload(buf.readVarInt(), buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
