package com.example.villagernations;

/** Regras simples compartilhadas entre servidor e cliente. */
public final class Rules {
    private Rules() {}

    public static String humor(int felicidade) {
        if (felicidade >= 85) return "Eufórico";
        if (felicidade >= 65) return "Contente";
        if (felicidade >= 45) return "Tranquilo";
        if (felicidade >= 25) return "Insatisfeito";
        return "Revoltado";
    }

    public static int corHumor(int felicidade) {
        if (felicidade >= 65) return 0xFF66BB6A;
        if (felicidade >= 45) return 0xFFFFD54F;
        if (felicidade >= 25) return 0xFFFF9800;
        return 0xFFF44336;
    }

    /** Produção em porcentagem conforme a felicidade: 50% (revoltados) a 110% (eufóricos). */
    public static int multiplicador(int felicidade) {
        return 50 + felicidade * 6 / 10;
    }

    /** Quanto cada 100 villagers consomem de comida e de água por ciclo. */
    public static int consumoPercent(int dificuldade) {
        return switch (dificuldade) {
            case 0 -> 30;
            case 2 -> 75;
            default -> 50;
        };
    }

    public static String nomeDificuldade(int dificuldade) {
        return switch (dificuldade) {
            case 0 -> "Fácil";
            case 2 -> "Difícil";
            default -> "Normal";
        };
    }
}
