package com.example.villagernations;

/**
 * Eras do documento de design.
 * custo = madeira E pedra necessárias para avançar desta era para a próxima.
 * populacaoMinima e edificiosMinimos = requisitos de população e de infraestrutura concluída.
 * Avançar também custa esmeraldas (custo / 5).
 */
public enum Era {
    PRIMITIVA("Primitiva", 50, 4, 4),
    ANTIGA("Antiga", 120, 8, 7),
    MEDIEVAL("Medieval", 250, 12, 10),
    RENASCIMENTO("Renascimento", 400, 18, 14),
    INDUSTRIAL("Industrial", 700, 24, 18),
    MODERNA("Moderna", 1000, 32, 24),
    FUTURISTA("Futurista", 0, 0, 0);

    private final String nome;
    private final int custo;
    private final int populacaoMinima;
    private final int edificiosMinimos;

    Era(String nome, int custo, int populacaoMinima, int edificiosMinimos) {
        this.nome = nome;
        this.custo = custo;
        this.populacaoMinima = populacaoMinima;
        this.edificiosMinimos = edificiosMinimos;
    }

    public String nome() { return nome; }
    public int custo() { return custo; }
    public int custoEsmeraldas() { return custo / 5; }
    public int populacaoMinima() { return populacaoMinima; }
    public int edificiosMinimos() { return edificiosMinimos; }
    public boolean temProxima() { return this != FUTURISTA; }

    /** Limite de habitantes (mantém a simulação leve). */
    public int populacaoMaxima() { return 12 + 8 * ordinal(); }

    public Era proxima() {
        Era[] todas = values();
        return todas[Math.min(ordinal() + 1, todas.length - 1)];
    }

    public static Era porIndice(int i) {
        Era[] todas = values();
        return todas[Math.max(0, Math.min(i, todas.length - 1))];
    }
}
