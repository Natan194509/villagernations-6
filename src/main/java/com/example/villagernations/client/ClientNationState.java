package com.example.villagernations.client;

import com.example.villagernations.net.SyncPayload;

/** Último estado da vila recebido do servidor, mais a construção selecionada pelo jogador. */
public final class ClientNationState {
    public static SyncPayload dados = null;
    public static long ultimoSync = 0;
    public static String aviso = "";
    public static long avisoTempo = 0;
    /** Tipo de edifício selecionado para posicionar (-1 = nenhum). */
    public static int selecionado = -1;

    private ClientNationState() {}

    public static boolean ativo() {
        return dados != null && System.currentTimeMillis() - ultimoSync < 6000;
    }

    public static boolean avisoRecente() {
        return !aviso.isEmpty() && System.currentTimeMillis() - avisoTempo < 6000;
    }

    public static void atualizar(SyncPayload p) {
        dados = p;
        ultimoSync = System.currentTimeMillis();
        if (!p.aviso().isEmpty()) {
            aviso = p.aviso();
            avisoTempo = ultimoSync;
        }
    }
}
