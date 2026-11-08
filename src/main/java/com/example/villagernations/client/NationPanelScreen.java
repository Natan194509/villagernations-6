package com.example.villagernations.client;

import java.util.ArrayList;
import java.util.List;

import com.example.villagernations.Era;
import com.example.villagernations.Habitante;
import com.example.villagernations.NationLogic;
import com.example.villagernations.Ordem;
import com.example.villagernations.Profissao;
import com.example.villagernations.Rules;
import com.example.villagernations.TipoEdificio;
import com.example.villagernations.net.ActionPayload;
import com.example.villagernations.net.SyncPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Painel de administração com três abas: Geral, População e Construções. */
public class NationPanelScreen extends Screen {
    private static final int W = 360;
    private static final int H = 232;
    private static final int BRANCO = 0xFFFFFFFF;
    private static final int CINZA = 0xFFAAAAAA;
    private static final int OURO = 0xFFFFD54F;
    private static final int POR_PAGINA_OBRAS = 5;

    /** Lembram a aba e a página entre aberturas do painel. */
    private static int abaAtual = 0;
    private static int paginaObras = 0;

    private int px;
    private int py;

    public NationPanelScreen() {
        super(Component.literal("Painel da Vila"));
    }

    private static void enviar(int acao, int valor) {
        PacketDistributor.sendToServer(new ActionPayload(acao, valor));
    }

    private static SyncPayload.HabLinha linha(int row) {
        SyncPayload s = ClientNationState.dados;
        if (s == null || row >= s.habitantes().size()) return null;
        return s.habitantes().get(row);
    }

    /** Edifícios já liberados na era atual. */
    private static List<TipoEdificio> disponiveis() {
        SyncPayload s = ClientNationState.dados;
        int era = s == null ? 0 : s.era();
        List<TipoEdificio> lista = new ArrayList<>();
        for (TipoEdificio t : TipoEdificio.values()) {
            if (t.eraMinima() <= era) lista.add(t);
        }
        return lista;
    }

    private static int totalPaginasObras() {
        return Math.max(1, (disponiveis().size() + POR_PAGINA_OBRAS - 1) / POR_PAGINA_OBRAS);
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;

        String[] nomes = { "Geral", "População", "Construções" };
        for (int i = 0; i < nomes.length; i++) {
            final int aba = i;
            Button tab = Button.builder(Component.literal(nomes[i]), btn -> {
                abaAtual = aba;
                rebuildWidgets();
            }).bounds(px + 10 + i * 86, py + 22, 82, 16).build();
            tab.active = aba != abaAtual;
            addRenderableWidget(tab);
        }

        if (abaAtual == 0) iniciarGeral();
        else if (abaAtual == 1) iniciarPopulacao();
        else iniciarObras();

        int by = py + H - 26;
        int bw = 78;
        int bx = px + 12;
        addRenderableWidget(Button.builder(Component.literal("Recrutar +1"), b -> enviar(ActionPayload.RECRUTAR, 1))
                .bounds(bx, by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Recrutar +5"), b -> enviar(ActionPayload.RECRUTAR, 5))
                .bounds(bx + (bw + 8), by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Avançar era"), b -> enviar(ActionPayload.AVANCAR, 0))
                .bounds(bx + 2 * (bw + 8), by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> onClose())
                .bounds(bx + 3 * (bw + 8), by, bw, 20).build());
    }

    private void maisMenos(int y, int acaoMenos, int valorMenos, int acaoMais, int valorMais) {
        addRenderableWidget(Button.builder(Component.literal("-"), b -> enviar(acaoMenos, valorMenos))
                .bounds(px + 296, y, 18, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> enviar(acaoMais, valorMais))
                .bounds(px + 320, y, 18, 16).build());
    }

    private void iniciarGeral() {
        Profissao[] ps = { Profissao.AGRICULTOR, Profissao.LENHADOR, Profissao.MINEIRO, Profissao.CONSTRUTOR };
        for (int i = 0; i < ps.length; i++) {
            int idx = ps[i].ordinal();
            maisMenos(py + 46 + i * 20, ActionPayload.PROF_MENOS, idx, ActionPayload.PROF_MAIS, idx);
        }
        maisMenos(py + 46 + 4 * 20, ActionPayload.IMPOSTO_MENOS, 0, ActionPayload.IMPOSTO_MAIS, 0);
        maisMenos(py + 46 + 5 * 20, ActionPayload.SALARIO_MENOS, 0, ActionPayload.SALARIO_MAIS, 0);
    }

    private void iniciarPopulacao() {
        for (int r = 0; r < NationLogic.POR_PAGINA; r++) {
            final int row = r;
            int ry = py + 46 + r * 22;
            addRenderableWidget(Button.builder(Component.literal("Prof."), b -> {
                SyncPayload.HabLinha h = linha(row);
                if (h != null) enviar(ActionPayload.HAB_PROF, h.seq());
            }).bounds(px + 250, ry + 2, 44, 18).build());
            addRenderableWidget(Button.builder(Component.literal("Ordem"), b -> {
                SyncPayload.HabLinha h = linha(row);
                if (h != null) enviar(ActionPayload.HAB_ORDEM, h.seq());
            }).bounds(px + 298, ry + 2, 50, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            SyncPayload s = ClientNationState.dados;
            if (s != null) enviar(ActionPayload.PAGINA, Math.max(0, s.pagina() - 1));
        }).bounds(px + 10, py + 180, 18, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            SyncPayload s = ClientNationState.dados;
            if (s != null) enviar(ActionPayload.PAGINA, s.pagina() + 1);
        }).bounds(px + 32, py + 180, 18, 16).build());
    }

    private void iniciarObras() {
        for (int r = 0; r < POR_PAGINA_OBRAS; r++) {
            final int row = r;
            addRenderableWidget(Button.builder(Component.literal("Selecionar"), b -> {
                List<TipoEdificio> lista = disponiveis();
                int i = paginaObras * POR_PAGINA_OBRAS + row;
                if (i < lista.size()) {
                    ClientNationState.selecionado = lista.get(i).ordinal();
                    onClose();
                }
            }).bounds(px + 280, py + 46 + r * 22 + 2, 70, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Cancelar última obra"), b -> enviar(ActionPayload.CANCELAR_OBRA, 0))
                .bounds(px + 10, py + 172, 130, 16).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> paginaObras = Math.max(0, paginaObras - 1))
                .bounds(px + 190, py + 172, 18, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                b -> paginaObras = Math.min(totalPaginasObras() - 1, paginaObras + 1))
                .bounds(px + 212, py + 172, 18, 16).build());
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Sem fundo borrado: dá para ver o mapa atrás do painel. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientModEvents.ABRIR_PAINEL.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SyncPayload s = ClientNationState.dados;
        g.fill(px - 2, py - 2, px + W + 2, py + H + 2, 0xFF3C6E47);
        g.fill(px, py, px + W, py + H, 0xF0141A22);

        if (s != null) {
            Era era = Era.porIndice(s.era());
            g.drawCenteredString(this.font, s.nome() + " - Era " + era.nome(), px + W / 2, py + 8, OURO);
            if (abaAtual == 0) desenharGeral(g, s, era);
            else if (abaAtual == 1) desenharPopulacao(g, s);
            else desenharObras(g, s);

            String rodape;
            int cor = CINZA;
            if (ClientNationState.avisoRecente()) {
                rodape = ClientNationState.aviso;
                cor = 0xFFFFE082;
            } else if (abaAtual == 0) {
                rodape = "Imposto/salário em esmeraldas por habitante por minuto.";
            } else if (abaAtual == 1) {
                rodape = "F fome | E energia | S saúde | H felicidade | Nv nível";
            } else {
                rodape = "Selecione, feche o painel, mire no chão e aperte B. V cancela.";
            }
            g.drawCenteredString(this.font, rodape, px + W / 2, py + 194, cor);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void desenharGeral(GuiGraphics g, SyncPayload s, Era era) {
        int lx = px + 10;
        int liquido = s.income() - s.expenses();
        g.drawString(this.font, "Habitantes: " + s.pop() + "/" + s.popMax() + " | Moradia: " + s.moradia(), lx, py + 46,
                s.pop() > s.moradia() ? 0xFFFFCC80 : BRANCO);
        g.drawString(this.font, "Esmeraldas: " + s.emeralds() + " (" + (liquido >= 0 ? "+" : "") + liquido + "/min)",
                lx, py + 58, liquido >= 0 ? 0xFFA5D6A7 : 0xFFEF9A9A);
        g.drawString(this.font, "Comida " + s.food() + "  Madeira " + s.wood() + "  Pedra " + s.stone()
                + " (máx. " + s.armazem() + ")", lx, py + 70, BRANCO);

        g.drawString(this.font, "Fome", lx, py + 86, BRANCO);
        NationHud.barra(g, lx + 50, py + 87, 100, 7, s.mediaFome());
        g.drawString(this.font, "Saúde", lx, py + 98, BRANCO);
        NationHud.barra(g, lx + 50, py + 99, 100, 7, s.mediaSaude());
        g.drawString(this.font, "Feliz", lx, py + 110, BRANCO);
        NationHud.barra(g, lx + 50, py + 111, 100, 7, s.mediaFeliz());
        g.drawString(this.font, "Humor: ", lx, py + 124, CINZA);
        g.drawString(this.font, Rules.humor(s.mediaFeliz()), lx + 38, py + 124, Rules.corHumor(s.mediaFeliz()));

        if (era.temProxima()) {
            g.drawString(this.font, "Próxima era: " + era.proxima().nome(), lx, py + 138, OURO);
            g.drawString(this.font, "Mad." + era.custo() + " Ped." + era.custo() + " Esm." + era.custoEsmeraldas()
                    + " Hab." + era.populacaoMinima() + " Edif." + era.edificiosMinimos(), lx, py + 150, CINZA);
        } else {
            g.drawString(this.font, "Era Futurista alcançada!", lx, py + 138, OURO);
        }

        List<String> hist = s.historico();
        int primeiro = Math.max(0, hist.size() - 3);
        for (int i = primeiro; i < hist.size(); i++) {
            g.drawString(this.font, hist.get(i), lx, py + 166 + (i - primeiro) * 10, CINZA);
        }

        int rx = px + 190;
        g.fill(px + 182, py + 44, px + 183, py + 176, 0xFF2E3B46);
        Profissao[] ps = { Profissao.AGRICULTOR, Profissao.LENHADOR, Profissao.MINEIRO, Profissao.CONSTRUTOR };
        for (int i = 0; i < ps.length; i++) {
            int p = ps[i].ordinal();
            int ry = py + 46 + i * 20;
            String vagas = ps[i].precisaVaga() ? s.profCount()[p] + "/" + s.vagas()[p] : String.valueOf(s.profCount()[p]);
            boolean semVaga = ps[i].precisaVaga() && s.profCount()[p] > s.vagas()[p];
            g.drawString(this.font, ps[i].nome() + " " + vagas, rx, ry + 4, semVaga ? 0xFFFFCC80 : BRANCO);
        }
        g.drawString(this.font, "Imposto: " + s.tax(), rx, py + 46 + 4 * 20 + 4, BRANCO);
        g.drawString(this.font, "Salário: " + s.salario(), rx, py + 46 + 5 * 20 + 4, BRANCO);
        g.drawString(this.font, "Livres: " + s.profCount()[Profissao.LIVRE.ordinal()], rx, py + 46 + 6 * 20 + 4, CINZA);
    }

    private void desenharPopulacao(GuiGraphics g, SyncPayload s) {
        for (int r = 0; r < s.habitantes().size(); r++) {
            SyncPayload.HabLinha h = s.habitantes().get(r);
            int ry = py + 46 + r * 22;
            g.drawString(this.font, h.nome() + " | " + Profissao.porIndice(h.prof()).nome()
                    + " | " + Ordem.porIndice(h.ordem()).nome(), px + 10, ry + 2, BRANCO);
            g.drawString(this.font, Habitante.nomeTarefa(h.tarefa()) + "  F" + h.fome() + " E" + h.energia()
                    + " S" + h.saude() + " H" + h.feliz() + " Nv" + h.nivel(), px + 10, ry + 12, CINZA);
        }
        g.drawString(this.font, "Página " + (s.pagina() + 1) + "/" + s.totalPaginas(), px + 56, py + 184, CINZA);
    }

    private void desenharObras(GuiGraphics g, SyncPayload s) {
        List<TipoEdificio> lista = disponiveis();
        int totalPag = totalPaginasObras();
        if (paginaObras >= totalPag) paginaObras = totalPag - 1;

        for (int r = 0; r < POR_PAGINA_OBRAS; r++) {
            int i = paginaObras * POR_PAGINA_OBRAS + r;
            if (i >= lista.size()) break;
            TipoEdificio t = lista.get(i);
            int ry = py + 46 + r * 22;
            boolean selecionado = ClientNationState.selecionado == t.ordinal();
            int emObra = s.edObra()[t.ordinal()];
            String obra = emObra > 0 ? " (+" + emObra + " em obra)" : "";
            g.drawString(this.font, t.nome() + ": " + s.edConcluidos()[t.ordinal()] + obra, px + 10, ry + 2,
                    selecionado ? 0xFF81C784 : BRANCO);
            g.drawString(this.font, "Mad." + t.madeira() + " Ped." + t.pedra() + " | " + t.efeito(),
                    px + 10, ry + 12, CINZA);
        }

        String atual = s.projetoTipo() < 0
                ? "Nenhuma obra em andamento."
                : "Obra atual: " + TipoEdificio.porIndice(s.projetoTipo()).nome() + " (" + s.projetoPct() + "%)";
        g.drawString(this.font, atual, px + 10, py + 158, OURO);
        g.drawString(this.font, "Pág. " + (paginaObras + 1) + "/" + totalPag + " | Construtores: "
                + s.profCount()[Profissao.CONSTRUTOR.ordinal()], px + 236, py + 176, CINZA);
    }
}
