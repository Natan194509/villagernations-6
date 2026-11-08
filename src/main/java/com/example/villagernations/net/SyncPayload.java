package com.example.villagernations.net;

import java.util.ArrayList;
import java.util.List;

import com.example.villagernations.NationsMod;
import com.example.villagernations.Profissao;
import com.example.villagernations.TipoEdificio;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: resumo da vila, enviado a cada 2 segundos e após cada ação. */
public record SyncPayload(String nome, int era, int emeralds, int food, int wood, int stone, int armazem,
                          int pop, int popMax, int moradia, int mediaFome, int mediaSaude, int mediaFeliz,
                          int tax, int salario, int income, int expenses,
                          int[] profCount, int[] vagas, int[] edConcluidos, int[] edObra,
                          int projetoTipo, int projetoPct, int pagina, int totalPaginas,
                          List<HabLinha> habitantes, boolean prefeito, String aviso, List<String> historico)
        implements CustomPacketPayload {

    /** Uma linha da lista de habitantes (a página atual). */
    public record HabLinha(int seq, String nome, int prof, int ordem, int tarefa,
                           int fome, int energia, int saude, int feliz, int nivel) {}

    public static final CustomPacketPayload.Type<SyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NationsMod.MODID, "sync"));

    public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC =
            StreamCodec.of(SyncPayload::escrever, SyncPayload::ler);

    private static void escreverArray(FriendlyByteBuf buf, int[] a, int tamanho) {
        for (int i = 0; i < tamanho; i++) buf.writeVarInt(a[i]);
    }

    private static int[] lerArray(FriendlyByteBuf buf, int tamanho) {
        int[] a = new int[tamanho];
        for (int i = 0; i < tamanho; i++) a[i] = buf.readVarInt();
        return a;
    }

    private static void escrever(FriendlyByteBuf buf, SyncPayload p) {
        buf.writeUtf(p.nome, 32);
        buf.writeVarInt(p.era);
        buf.writeVarInt(p.emeralds);
        buf.writeVarInt(p.food);
        buf.writeVarInt(p.wood);
        buf.writeVarInt(p.stone);
        buf.writeVarInt(p.armazem);
        buf.writeVarInt(p.pop);
        buf.writeVarInt(p.popMax);
        buf.writeVarInt(p.moradia);
        buf.writeVarInt(p.mediaFome);
        buf.writeVarInt(p.mediaSaude);
        buf.writeVarInt(p.mediaFeliz);
        buf.writeVarInt(p.tax);
        buf.writeVarInt(p.salario);
        buf.writeVarInt(p.income);
        buf.writeVarInt(p.expenses);
        escreverArray(buf, p.profCount, Profissao.values().length);
        escreverArray(buf, p.vagas, Profissao.values().length);
        escreverArray(buf, p.edConcluidos, TipoEdificio.values().length);
        escreverArray(buf, p.edObra, TipoEdificio.values().length);
        buf.writeVarInt(p.projetoTipo + 1);
        buf.writeVarInt(p.projetoPct);
        buf.writeVarInt(p.pagina);
        buf.writeVarInt(p.totalPaginas);
        buf.writeVarInt(p.habitantes.size());
        for (HabLinha h : p.habitantes) {
            buf.writeVarInt(h.seq());
            buf.writeUtf(h.nome(), 32);
            buf.writeVarInt(h.prof());
            buf.writeVarInt(h.ordem());
            buf.writeVarInt(h.tarefa());
            buf.writeVarInt(h.fome());
            buf.writeVarInt(h.energia());
            buf.writeVarInt(h.saude());
            buf.writeVarInt(h.feliz());
            buf.writeVarInt(h.nivel());
        }
        buf.writeBoolean(p.prefeito);
        buf.writeUtf(p.aviso, 256);
        buf.writeVarInt(p.historico.size());
        for (String s : p.historico) buf.writeUtf(s, 128);
    }

    private static SyncPayload ler(FriendlyByteBuf buf) {
        String nome = buf.readUtf(32);
        int era = buf.readVarInt();
        int emeralds = buf.readVarInt();
        int food = buf.readVarInt();
        int wood = buf.readVarInt();
        int stone = buf.readVarInt();
        int armazem = buf.readVarInt();
        int pop = buf.readVarInt();
        int popMax = buf.readVarInt();
        int moradia = buf.readVarInt();
        int mediaFome = buf.readVarInt();
        int mediaSaude = buf.readVarInt();
        int mediaFeliz = buf.readVarInt();
        int tax = buf.readVarInt();
        int salario = buf.readVarInt();
        int income = buf.readVarInt();
        int expenses = buf.readVarInt();
        int[] profCount = lerArray(buf, Profissao.values().length);
        int[] vagas = lerArray(buf, Profissao.values().length);
        int[] edConcluidos = lerArray(buf, TipoEdificio.values().length);
        int[] edObra = lerArray(buf, TipoEdificio.values().length);
        int projetoTipo = buf.readVarInt() - 1;
        int projetoPct = buf.readVarInt();
        int pagina = buf.readVarInt();
        int totalPaginas = buf.readVarInt();
        int n = buf.readVarInt();
        List<HabLinha> habitantes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int seq = buf.readVarInt();
            String hn = buf.readUtf(32);
            int prof = buf.readVarInt();
            int ordem = buf.readVarInt();
            int tarefa = buf.readVarInt();
            int fome = buf.readVarInt();
            int energia = buf.readVarInt();
            int saude = buf.readVarInt();
            int feliz = buf.readVarInt();
            int nivel = buf.readVarInt();
            habitantes.add(new HabLinha(seq, hn, prof, ordem, tarefa, fome, energia, saude, feliz, nivel));
        }
        boolean prefeito = buf.readBoolean();
        String aviso = buf.readUtf(256);
        int nh = buf.readVarInt();
        List<String> historico = new ArrayList<>();
        for (int i = 0; i < nh; i++) historico.add(buf.readUtf(128));
        return new SyncPayload(nome, era, emeralds, food, wood, stone, armazem, pop, popMax, moradia,
                mediaFome, mediaSaude, mediaFeliz, tax, salario, income, expenses,
                profCount, vagas, edConcluidos, edObra, projetoTipo, projetoPct, pagina, totalPaginas,
                habitantes, prefeito, aviso, historico);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
