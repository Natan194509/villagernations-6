package com.example.villagernations;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Um edifício da vila. pos = canto mínimo da pegada, na altura do piso.
 * progresso é em centésimos de unidade de obra; colocados = quantos blocos da estrutura já foram erguidos.
 */
public class Edificio {
    public int id = 0;
    public TipoEdificio tipo = TipoEdificio.CABANA;
    public BlockPos pos = BlockPos.ZERO;
    public boolean concluido = false;
    public boolean preparado = false;
    public int progresso = 0;
    public int colocados = 0;

    public CompoundTag salvar() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putInt("tipo", tipo.ordinal());
        t.putLong("pos", pos.asLong());
        t.putBoolean("concluido", concluido);
        t.putBoolean("preparado", preparado);
        t.putInt("progresso", progresso);
        t.putInt("colocados", colocados);
        return t;
    }

    public static Edificio carregar(CompoundTag t) {
        Edificio e = new Edificio();
        e.id = t.getInt("id");
        e.tipo = TipoEdificio.porIndice(t.getInt("tipo"));
        if (t.contains("pos")) e.pos = BlockPos.of(t.getLong("pos"));
        e.concluido = t.getBoolean("concluido");
        e.preparado = t.getBoolean("preparado");
        e.progresso = t.getInt("progresso");
        e.colocados = t.getInt("colocados");
        return e;
    }
}
