package com.example.villagernations;

/** Profissões da versão 0.1. Quem precisa de vaga só trabalha se existir um edifício com vaga livre. */
public enum Profissao {
    LIVRE("Coletor", "comida", 1, false),
    AGRICULTOR("Agricultor", "comida", 4, true),
    LENHADOR("Lenhador", "madeira", 3, true),
    MINEIRO("Mineiro", "pedra", 2, true),
    CONSTRUTOR("Construtor", "obras", 0, false);

    private final String nome;
    private final String recurso;
    private final int producao;
    private final boolean precisaVaga;

    Profissao(String nome, String recurso, int producao, boolean precisaVaga) {
        this.nome = nome;
        this.recurso = recurso;
        this.producao = producao;
        this.precisaVaga = precisaVaga;
    }

    public String nome() { return nome; }
    public String recurso() { return recurso; }
    public int producao() { return producao; }
    public boolean precisaVaga() { return precisaVaga; }

    public Profissao proxima() {
        Profissao[] todas = values();
        return todas[(ordinal() + 1) % todas.length];
    }

    public static Profissao porIndice(int i) {
        Profissao[] todas = values();
        return todas[Math.max(0, Math.min(i, todas.length - 1))];
    }
}
