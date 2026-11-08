package com.example.villagernations.client;

import com.example.villagernations.NationsMod;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Registro das teclas e da camada do HUD (só no cliente). */
@EventBusSubscriber(modid = NationsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final KeyMapping ABRIR_PAINEL = new KeyMapping(
            "key.villagernations.painel", InputConstants.KEY_N, "key.categories.villagernations");
    public static final KeyMapping CONSTRUIR = new KeyMapping(
            "key.villagernations.construir", InputConstants.KEY_B, "key.categories.villagernations");
    public static final KeyMapping CANCELAR = new KeyMapping(
            "key.villagernations.cancelar", InputConstants.KEY_V, "key.categories.villagernations");

    private ClientModEvents() {}

    @SubscribeEvent
    public static void teclas(RegisterKeyMappingsEvent event) {
        event.register(ABRIR_PAINEL);
        event.register(CONSTRUIR);
        event.register(CANCELAR);
    }

    @SubscribeEvent
    public static void camadas(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(NationsMod.MODID, "hud"), NationHud::render);
    }
}
