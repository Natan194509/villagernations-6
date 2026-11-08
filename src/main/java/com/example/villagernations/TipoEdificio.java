package com.example.villagernations;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo de edifícios, liberados por era (da Primitiva à Moderna).
 * trabalho = unidades de obra necessárias. largura (eixo X) e profundidade (eixo Z) = pegada no terreno.
 */
public enum TipoEdificio {
    // Era Primitiva
    CABANA("Cabana", 0, 15, 0, 5, 3, null, 0, 0, 0, 0, 5, 5, Forma.CASA),
    FAZENDA("Fazenda", 0, 20, 0, 8, 0, Profissao.AGRICULTOR, 3, 0, 0, 0, 9, 9, Forma.FAZENDA),
    SERRARIA("Posto de Lenhadores", 0, 15, 5, 6, 0, Profissao.LENHADOR, 3, 0, 0, 0, 5, 7, Forma.GALPAO),
    PEDREIRA("Pedreira", 0, 20, 0, 8, 0, Profissao.MINEIRO, 3, 0, 0, 0, 7, 7, Forma.PEDREIRA),
    DEPOSITO("Depósito", 0, 30, 10, 8, 0, null, 0, 300, 0, 0, 5, 7, Forma.GALPAO),
    // Era Antiga
    CASA_PEDRA("Casa de Pedra", 1, 20, 25, 8, 6, null, 0, 0, 0, 0, 7, 7, Forma.CASA),
    // Era Medieval
    CASA_MEDIEVAL("Casa Medieval", 2, 30, 30, 10, 8, null, 0, 0, 0, 0, 7, 9, Forma.CASA),
    MERCADO("Mercado", 2, 40, 20, 10, 0, null, 0, 0, 4, 0, 9, 7, Forma.MERCADO),
    TAVERNA("Taverna", 2, 45, 25, 10, 0, null, 0, 0, 0, 5, 9, 9, Forma.CASA),
    TORRE("Torre de Vigia", 2, 20, 40, 10, 0, null, 0, 0, 0, 3, 5, 5, Forma.TORRE),
    // Renascimento
    BIBLIOTECA("Biblioteca", 3, 50, 50, 12, 0, null, 0, 0, 0, 6, 9, 9, Forma.CASA),
    MANSAO("Casa Renascentista", 3, 40, 50, 12, 10, null, 0, 0, 0, 0, 9, 9, Forma.CASA),
    // Industrial
    FABRICA("Fábrica", 4, 80, 80, 16, 0, null, 0, 0, 12, 0, 11, 9, Forma.FABRICA),
    CORTICO("Prédio de Tijolos", 4, 60, 80, 14, 16, null, 0, 0, 0, 0, 9, 9, Forma.PREDIO),
    // Moderna
    PREDIO("Prédio Moderno", 5, 100, 120, 18, 28, null, 0, 0, 0, 0, 11, 11, Forma.PREDIO),
    HOSPITAL("Hospital", 5, 90, 110, 18, 0, null, 0, 0, 0, 10, 13, 11, Forma.CASA);

    private final String nome;
    private final int eraMinima;
    private final int madeira;
    private final int pedra;
    private final int trabalho;
    private final int moradia;
    private final Profissao profissao;
    private final int vagas;
    private final int armazenamento;
    private final int renda;
    private final int bonusFeliz;
    private final int largura;
    private final int profundidade;
    private final Forma forma;

    TipoEdificio(String nome, int eraMinima, int madeira, int pedra, int trabalho, int moradia,
                 Profissao profissao, int vagas, int armazenamento, int renda, int bonusFeliz,
                 int largura, int profundidade, Forma forma) {
        this.nome = nome;
        this.eraMinima = eraMinima;
        this.madeira = madeira;
        this.pedra = pedra;
        this.trabalho = trabalho;
        this.moradia = moradia;
        this.profissao = profissao;
        this.vagas = vagas;
        this.armazenamento = armazenamento;
        this.renda = renda;
        this.bonusFeliz = bonusFeliz;
        this.largura = largura;
        this.profundidade = profundidade;
        this.forma = forma;
    }

    public String nome() { return nome; }
    public int eraMinima() { return eraMinima; }
    public int madeira() { return madeira; }
    public int pedra() { return pedra; }
    public int trabalho() { return trabalho; }
    public int moradia() { return moradia; }
    public Profissao profissao() { return profissao; }
    public int vagas() { return vagas; }
    public int armazenamento() { return armazenamento; }
    public int renda() { return renda; }
    public int bonusFeliz() { return bonusFeliz; }
    public int largura() { return largura; }
    public int profundidade() { return profundidade; }
    public Forma forma() { return forma; }

    public String efeito() {
        List<String> partes = new ArrayList<>();
        if (moradia > 0) partes.add("+" + moradia + " moradia");
        if (profissao != null) partes.add(vagas + " vagas de " + profissao.nome().toLowerCase());
        if (armazenamento > 0) partes.add("+" + armazenamento + " armazém");
        if (renda > 0) partes.add("+" + renda + " esm/min");
        if (bonusFeliz > 0) partes.add("+" + bonusFeliz + " felicidade");
        return String.join(", ", partes);
    }

    public static TipoEdificio porIndice(int i) {
        TipoEdificio[] todos = values();
        return todos[Math.max(0, Math.min(i, todos.length - 1))];
    }
}
