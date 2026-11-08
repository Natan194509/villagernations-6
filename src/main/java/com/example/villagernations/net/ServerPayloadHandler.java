package com.example.villagernations.net;

import com.example.villagernations.NationLogic;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Recebe no servidor o que o jogador clicou no painel. */
public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void acao(ActionPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            NationLogic.executarAcao(sp, p.acao(), p.valor());
        }
    }

    public static void iniciar(StartPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            NationLogic.iniciarNacao(sp, p.nome(), p.villagers(), p.esmeraldas(), p.dificuldade());
        }
    }
}
