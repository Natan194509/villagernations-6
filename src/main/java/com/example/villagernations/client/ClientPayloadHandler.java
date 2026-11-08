package com.example.villagernations.client;

import com.example.villagernations.net.ScreenPayload;
import com.example.villagernations.net.SyncPayload;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Recebe no cliente o que o servidor enviou. */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void sync(SyncPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientNationState.atualizar(p));
    }

    public static void tela(ScreenPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (p.tipo() == 0) {
                mc.setScreen(new NationSetupScreen());
            } else {
                mc.setScreen(new NationPanelScreen());
            }
        });
    }
}
