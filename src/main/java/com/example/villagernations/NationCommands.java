package com.example.villagernations;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Atalhos por comando. O jogo é feito pelo painel (N) e pela tecla de construção (B). */
public final class NationCommands {
    private NationCommands() {}

    public static void registrar(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("nacao")
                .then(Commands.literal("status").executes(NationCommands::status))
                .then(Commands.literal("painel").executes(NationCommands::painel))
                .then(Commands.literal("prefeito").executes(NationCommands::prefeito))
                .then(Commands.literal("sair").executes(NationCommands::sair)));
    }

    private static NationData nacao(ServerPlayer p) {
        NationData data = NationData.get(p.server);
        if (data.center == null) {
            NationLogic.msg(p, "Você ainda não fundou uma vila. Coloque o Centro da Nação e clique nele com a mão vazia.");
            return null;
        }
        return data;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        NationData data = nacao(p);
        if (data == null) return 0;
        NationLogic.mostrarStatus(p, data);
        return 1;
    }

    private static int painel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        NationData data = nacao(p);
        if (data == null) return 0;
        NationLogic.abrirPainel(p, data);
        return 1;
    }

    private static int prefeito(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        NationData data = nacao(p);
        if (data == null) return 0;
        NationLogic.entrarModoPrefeito(p, data, true);
        return 1;
    }

    private static int sair(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        NationData data = nacao(p);
        if (data == null) return 0;
        NationLogic.sairModoPrefeito(p, data);
        return 1;
    }
}
