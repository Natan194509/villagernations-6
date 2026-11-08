package com.example.villagernations.client;

import com.example.villagernations.NationsMod;
import com.example.villagernations.net.ActionPayload;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Teclas do jogo: abrir o painel (N), posicionar a construção (B) e cancelar a seleção (V). */
@EventBusSubscriber(modid = NationsMod.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        while (ClientModEvents.ABRIR_PAINEL.consumeClick()) {
            if (mc.player != null && mc.screen == null && ClientNationState.ativo()) {
                mc.setScreen(new NationPanelScreen());
            }
        }
        while (ClientModEvents.CONSTRUIR.consumeClick()) {
            if (mc.player != null && mc.screen == null && ClientNationState.ativo()
                    && ClientNationState.selecionado >= 0) {
                PacketDistributor.sendToServer(
                        new ActionPayload(ActionPayload.CONSTRUIR_AQUI, ClientNationState.selecionado));
            }
        }
        while (ClientModEvents.CANCELAR.consumeClick()) {
            ClientNationState.selecionado = -1;
        }
    }
}
