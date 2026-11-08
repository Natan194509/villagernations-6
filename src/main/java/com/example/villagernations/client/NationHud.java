package com.example.villagernations.client;

import java.util.ArrayList;
import java.util.List;

import com.example.villagernations.Era;
import com.example.villagernations.Profissao;
import com.example.villagernations.Rules;
import com.example.villagernations.TipoEdificio;
import com.example.villagernations.net.SyncPayload;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** HUD da vila: recursos no topo, condição dos habitantes e alertas. */
public final class NationHud {
    private static ItemStack[] icones;

    private NationHud() {}

    private record Seg(int icone, String texto, int cor) {}

    public static void barra(GuiGraphics g, int x, int y, int w, int h, int valor) {
        int v = Math.max(0, Math.min(100, valor));
        int cor = v > 60 ? 0xFF4CAF50 : v > 30 ? 0xFFFFC107 : 0xFFF44336;
        g.fill(x, y, x + w, y + h, 0xFF2A2A2A);
        g.fill(x, y, x + (w * v / 100), y + h, cor);
    }

    /** Devolve o alerta mais importante no momento, ou vazio. */
    public static String alerta(SyncPayload s) {
        int semVaga = 0;
        for (Profissao p : Profissao.values()) {
            if (p.precisaVaga()) {
                semVaga += Math.max(0, s.profCount()[p.ordinal()] - s.vagas()[p.ordinal()]);
            }
        }
        if (s.mediaFome() < 30) return "Fome! Construa fazendas e designe agricultores.";
        if (s.pop() > s.moradia()) return "Faltam casas: " + s.pop() + " habitantes para " + s.moradia() + " de moradia.";
        if (semVaga > 0) return semVaga + " trabalhador(es) sem vaga. Construa o edifício da profissão.";
        if (s.mediaFeliz() < 30) return "Povo descontente! Reduza o imposto ou pague salários.";
        if (s.emeralds() == 0 && s.expenses() > s.income()) return "Tesouro vazio! Aumente o imposto.";
        return "";
    }

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !ClientNationState.ativo()) return;

        SyncPayload s = ClientNationState.dados;
        Font f = mc.font;
        if (icones == null) {
            icones = new ItemStack[] {
                    new ItemStack(Items.EMERALD), new ItemStack(Items.WHEAT), new ItemStack(Items.OAK_LOG),
                    new ItemStack(Items.COBBLESTONE), new ItemStack(Items.BELL)
            };
        }

        int liquido = s.income() - s.expenses();
        List<Seg> segs = new ArrayList<>();
        segs.add(new Seg(0, s.emeralds() + " (" + (liquido >= 0 ? "+" : "") + liquido + ")",
                liquido >= 0 ? 0xFFFFFFFF : 0xFFFF8A80));
        segs.add(new Seg(1, String.valueOf(s.food()), 0xFFFFFFFF));
        segs.add(new Seg(2, String.valueOf(s.wood()), 0xFFFFFFFF));
        segs.add(new Seg(3, String.valueOf(s.stone()), 0xFFFFFFFF));
        segs.add(new Seg(4, s.pop() + "/" + s.popMax(), 0xFFFFFFFF));

        String titulo = s.nome() + " - " + Era.porIndice(s.era()).nome();
        int largura = f.width(titulo) + 14;
        for (Seg seg : segs) largura += 16 + 3 + f.width(seg.texto()) + 14;
        largura -= 14;

        int x0 = (g.guiWidth() - largura) / 2;
        int y0 = 4;
        g.fill(x0 - 8, y0 - 2, x0 + largura + 8, y0 + 20, 0xB0101820);
        g.fill(x0 - 8, y0 + 20, x0 + largura + 8, y0 + 21, 0xFF4CAF50);

        int x = x0;
        g.drawString(f, titulo, x, y0 + 6, 0xFFFFD54F);
        x += f.width(titulo) + 14;
        for (Seg seg : segs) {
            g.renderItem(icones[seg.icone()], x, y0 + 1);
            x += 16 + 3;
            g.drawString(f, seg.texto(), x, y0 + 6, seg.cor());
            x += f.width(seg.texto()) + 14;
        }

        // Condição dos habitantes
        int px = 6, py = 28;
        g.fill(px, py, px + 126, py + 52, 0xA0101820);
        g.drawString(f, "Fome", px + 4, py + 4, 0xFFFFFFFF);
        barra(g, px + 50, py + 5, 72, 7, s.mediaFome());
        g.drawString(f, "Saúde", px + 4, py + 16, 0xFFFFFFFF);
        barra(g, px + 50, py + 17, 72, 7, s.mediaSaude());
        g.drawString(f, "Feliz", px + 4, py + 28, 0xFFFFFFFF);
        barra(g, px + 50, py + 29, 72, 7, s.mediaFeliz());
        g.drawString(f, "Humor:", px + 4, py + 40, 0xFFAAAAAA);
        g.drawString(f, Rules.humor(s.mediaFeliz()), px + 40, py + 40, Rules.corHumor(s.mediaFeliz()));

        // Alerta
        String alerta = alerta(s);
        int ay = py + 58;
        if (!alerta.isEmpty()) {
            g.fill(px, ay, px + f.width(alerta) + 8, ay + 14, 0xB0401010);
            g.drawString(f, alerta, px + 4, ay + 3, 0xFFFFCDD2);
        }

        // Aviso da última ação
        if (ClientNationState.avisoRecente()) {
            String av = ClientNationState.aviso;
            int ax = (g.guiWidth() - f.width(av)) / 2;
            g.fill(ax - 4, 28, ax + f.width(av) + 4, 42, 0xB0101820);
            g.drawString(f, av, ax, 31, 0xFFFFE082);
        }

        // Construção selecionada
        if (ClientNationState.selecionado >= 0) {
            String sel = "Construindo: " + TipoEdificio.porIndice(ClientNationState.selecionado).nome()
                    + " - mire no chão e aperte [" + ClientModEvents.CONSTRUIR.getTranslatedKeyMessage().getString()
                    + "]. [" + ClientModEvents.CANCELAR.getTranslatedKeyMessage().getString() + "] cancela.";
            int sx = (g.guiWidth() - f.width(sel)) / 2;
            int sy = g.guiHeight() - 48;
            g.fill(sx - 6, sy - 3, sx + f.width(sel) + 6, sy + 12, 0xC0101820);
            g.drawString(f, sel, sx, sy, 0xFF81C784);
        }

        String dica = "[" + ClientModEvents.ABRIR_PAINEL.getTranslatedKeyMessage().getString() + "] Painel";
        g.drawString(f, dica, g.guiWidth() - f.width(dica) - 6, 28, 0xFFB0BEC5);
    }
}
