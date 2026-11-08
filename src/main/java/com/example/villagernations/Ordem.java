package com.example.villagernations;

/** Ordens que o jogador pode dar a um habitante. */
public enum Ordem {
    AUTO("Automático"),
    CONSTRUIR("Construir"),
    DESCANSAR("Descansar");

    private final String nome;

    Ordem(String nome) { this.nome = nome; }

    public String nome() { return nome; }

    public Ordem proxima() {
        Ordem[] todas = values();
        return todas[(ordinal() + 1) % todas.length];
    }

    public static Ordem porIndice(int i) {
        Ordem[] todas = values();
        return todas[Math.max(0, Math.min(i, todas.length - 1))];
    }
}
