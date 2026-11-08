package com.example.villagernations;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;

/** Ficha de um habitante. Os dados vivem aqui, não em uma entidade do mundo. */
public class Habitante {
    public UUID id = UUID.randomUUID();
    /** Número curto e único dentro da vila, usado pela interface. */
    public int seq = 0;
    public String nome = "";
    public int idade = 20;
    public int saude = 100;
    public int fome = 100;
    public int energia = 100;
    public int felicidade = 70;
    public Profissao profissao = Profissao.LIVRE;
    public Ordem ordem = Ordem.AUTO;
    public int xp = 0;
    /** Última tarefa executada (ver nomeTarefa). */
    public int tarefa = 0;

    public int nivel() {
        return Math.min(4, xp / 30);
    }

    public static String nomeTarefa(int t) {
        return switch (t) {
            case 0 -> "Coletando";
            case 1 -> "Trabalhando";
            case 2 -> "Construindo";
            case 3 -> "Descansando";
            case 4 -> "Sem vaga";
            default -> "Ocioso";
        };
    }

    public CompoundTag salvar() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putInt("seq", seq);
        t.putString("nome", nome);
        t.putInt("idade", idade);
        t.putInt("saude", saude);
        t.putInt("fome", fome);
        t.putInt("energia", energia);
        t.putInt("felicidade", felicidade);
        t.putInt("profissao", profissao.ordinal());
        t.putInt("ordem", ordem.ordinal());
        t.putInt("xp", xp);
        t.putInt("tarefa", tarefa);
        return t;
    }

    public static Habitante carregar(CompoundTag t) {
        Habitante h = new Habitante();
        if (t.hasUUID("id")) h.id = t.getUUID("id");
        h.seq = t.getInt("seq");
        h.nome = t.getString("nome");
        h.idade = t.getInt("idade");
        h.saude = t.getInt("saude");
        h.fome = t.getInt("fome");
        h.energia = t.getInt("energia");
        h.felicidade = t.getInt("felicidade");
        h.profissao = Profissao.porIndice(t.getInt("profissao"));
        h.ordem = Ordem.porIndice(t.getInt("ordem"));
        h.xp = t.getInt("xp");
        h.tarefa = t.getInt("tarefa");
        return h;
    }
}
