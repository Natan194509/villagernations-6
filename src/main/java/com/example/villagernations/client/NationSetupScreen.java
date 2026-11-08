package com.example.villagernations.client;

import com.example.villagernations.Rules;
import com.example.villagernations.net.StartPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Tela de configuração mostrada no primeiro clique no Centro da Nação. */
public class NationSetupScreen extends Screen {
    private static final int W = 250;
    private static final int H = 190;

    private String nome = "";
    private int villagers = 3;
    private int esmeraldas = 30;
    private int dificuldade = 1;
    private int px;
    private int py;

    public NationSetupScreen() {
        super(Component.literal("Fundar vila"));
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;

        EditBox campo = new EditBox(this.font, px + 12, py + 28, W - 24, 18, Component.literal("Nome da vila"));
        campo.setMaxLength(24);
        campo.setHint(Component.literal("Nome da vila"));
        campo.setValue(nome);
        campo.setResponder(texto -> nome = texto);
        addRenderableWidget(campo);

        linha(0, () -> villagers = Math.max(1, villagers - 1), () -> villagers = Math.min(10, villagers + 1));
        linha(1, () -> esmeraldas = Math.max(0, esmeraldas - 10), () -> esmeraldas = Math.min(500, esmeraldas + 10));
        linha(2, () -> dificuldade = Math.max(0, dificuldade - 1), () -> dificuldade = Math.min(2, dificuldade + 1));

        addRenderableWidget(Button.builder(Component.literal("Começar"), b -> {
            PacketDistributor.sendToServer(new StartPayload(nome, villagers, esmeraldas, dificuldade));
            onClose();
        }).bounds(px + 20, py + H - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> onClose())
                .bounds(px + 130, py + H - 30, 100, 20).build());
    }

    private void linha(int i, Runnable menos, Runnable mais) {
        int ry = py + 62 + i * 26;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> menos.run())
                .bounds(px + 150, ry, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> mais.run())
                .bounds(px + 220, ry, 18, 18).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(px - 2, py - 2, px + W + 2, py + H + 2, 0xFF3C6E47);
        g.fill(px, py, px + W, py + H, 0xF0141A22);
        g.drawCenteredString(this.font, "Fundar vila", px + W / 2, py + 10, 0xFFFFD54F);

        g.drawString(this.font, "Habitantes iniciais", px + 12, py + 67, 0xFFFFFFFF);
        g.drawCenteredString(this.font, String.valueOf(villagers), px + 194, py + 67, 0xFFFFFFFF);
        g.drawString(this.font, "Esmeraldas iniciais", px + 12, py + 93, 0xFFFFFFFF);
        g.drawCenteredString(this.font, String.valueOf(esmeraldas), px + 194, py + 93, 0xFFFFFFFF);
        g.drawString(this.font, "Dificuldade", px + 12, py + 119, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Rules.nomeDificuldade(dificuldade), px + 194, py + 119, 0xFFFFFFFF);

        g.drawCenteredString(this.font, "A dificuldade muda o consumo de comida.", px + W / 2, py + 144, 0xFFAAAAAA);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
