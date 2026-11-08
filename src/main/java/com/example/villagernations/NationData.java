package com.example.villagernations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Dados da vila, salvos com o mundo. Contém tudo: habitantes, edifícios, estoques e finanças. */
public class NationData extends SavedData {
    private static final String NOME_ARQUIVO = "villagernations_nation";
    /** Versão do formato salvo, para migrações futuras. */
    public static final int VERSAO_DADOS = 2;

    // Identidade
    public String nome = "";
    public UUID dono = null;
    public Era era = Era.PRIMITIVA;
    public BlockPos center = null;

    // Estoques e finanças
    public int food = 0;
    public int wood = 0;
    public int stone = 0;
    public int emeralds = 0;
    public int tax = 1;
    public int salario = 0;
    public int lastIncome = 0;
    public int lastExpenses = 0;

    // Configuração e contadores
    public int dificuldade = 1;
    public int ciclo = 0;
    public int proxSeq = 1;
    public int proxEdId = 1;
    public int visiveis = 0;

    public final List<Habitante> habitantes = new ArrayList<>();
    public final List<Edificio> edificios = new ArrayList<>();
    public final List<String> historico = new ArrayList<>();

    // Valores derivados (recalculados, não salvos)
    public int moradia = 0;
    public int armazem = NationLogic.BASE_ARMAZEM;
    public int concluidos = 0;
    /** Renda extra por minuto vinda de mercados e fábricas. */
    public int renda = 0;
    /** Bônus de felicidade vindo de tavernas, bibliotecas e hospitais. */
    public int bonusFeliz = 0;
    public int mediaFome = 100;
    public int mediaSaude = 100;
    public int mediaFeliz = 70;
    public final int[] vagas = new int[Profissao.values().length];
    public final int[] contagem = new int[Profissao.values().length];
    public final int[] edConcluidos = new int[TipoEdificio.values().length];
    public final int[] edObra = new int[TipoEdificio.values().length];

    public static NationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(NationData::new, NationData::load, null),
                NOME_ARQUIVO);
    }

    public static NationData load(CompoundTag tag, HolderLookup.Provider registries) {
        NationData d = new NationData();
        // Dados de versões antigas do mod (antes do documento de design) são descartados.
        if (!tag.contains("versao") || tag.getInt("versao") < VERSAO_DADOS) return d;

        d.nome = tag.getString("nome");
        if (tag.hasUUID("dono")) d.dono = tag.getUUID("dono");
        d.era = Era.porIndice(tag.getInt("era"));
        if (tag.contains("center")) d.center = BlockPos.of(tag.getLong("center"));

        d.food = tag.getInt("food");
        d.wood = tag.getInt("wood");
        d.stone = tag.getInt("stone");
        d.emeralds = tag.getInt("emeralds");
        d.tax = tag.getInt("tax");
        d.salario = tag.getInt("salario");
        d.lastIncome = tag.getInt("lastIncome");
        d.lastExpenses = tag.getInt("lastExpenses");

        d.dificuldade = tag.getInt("dificuldade");
        d.ciclo = tag.getInt("ciclo");
        d.proxSeq = Math.max(1, tag.getInt("proxSeq"));
        d.proxEdId = Math.max(1, tag.getInt("proxEdId"));
        d.visiveis = tag.getInt("visiveis");

        ListTag hs = tag.getList("habitantes", Tag.TAG_COMPOUND);
        for (int i = 0; i < hs.size(); i++) d.habitantes.add(Habitante.carregar(hs.getCompound(i)));
        ListTag es = tag.getList("edificios", Tag.TAG_COMPOUND);
        for (int i = 0; i < es.size(); i++) d.edificios.add(Edificio.carregar(es.getCompound(i)));
        ListTag hist = tag.getList("historico", Tag.TAG_STRING);
        for (int i = 0; i < hist.size(); i++) d.historico.add(hist.getString(i));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("versao", VERSAO_DADOS);
        tag.putString("nome", nome);
        if (dono != null) tag.putUUID("dono", dono);
        tag.putInt("era", era.ordinal());
        if (center != null) tag.putLong("center", center.asLong());

        tag.putInt("food", food);
        tag.putInt("wood", wood);
        tag.putInt("stone", stone);
        tag.putInt("emeralds", emeralds);
        tag.putInt("tax", tax);
        tag.putInt("salario", salario);
        tag.putInt("lastIncome", lastIncome);
        tag.putInt("lastExpenses", lastExpenses);

        tag.putInt("dificuldade", dificuldade);
        tag.putInt("ciclo", ciclo);
        tag.putInt("proxSeq", proxSeq);
        tag.putInt("proxEdId", proxEdId);
        tag.putInt("visiveis", visiveis);

        ListTag hs = new ListTag();
        for (Habitante h : habitantes) hs.add(h.salvar());
        tag.put("habitantes", hs);
        ListTag es = new ListTag();
        for (Edificio e : edificios) es.add(e.salvar());
        tag.put("edificios", es);
        ListTag hist = new ListTag();
        for (String s : historico) hist.add(StringTag.valueOf(s));
        tag.put("historico", hist);
        return tag;
    }
}
