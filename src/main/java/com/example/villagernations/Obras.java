package com.example.villagernations;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Operações no mundo: validar local, preparar o terreno, erguer blocos e demolir. */
public final class Obras {
    /** Raio do território da vila, em blocos, a partir do Centro da Nação. */
    public static final int RAIO_VILA = 80;

    private Obras() {}

    private static boolean sobrepoe(int ax, int az, int aw, int ad, int bx, int bz, int bw, int bd) {
        return ax < bx + bw && ax + aw > bx && az < bz + bd && az + ad > bz;
    }

    /** Devolve uma mensagem de erro, ou null se o local é válido. */
    public static String validar(ServerLevel level, NationData d, TipoEdificio t, BlockPos o) {
        int w = t.largura();
        int dd = t.profundidade();

        double dx = (o.getX() + w / 2.0) - d.center.getX();
        double dz = (o.getZ() + dd / 2.0) - d.center.getZ();
        if (dx * dx + dz * dz > (double) RAIO_VILA * RAIO_VILA) {
            return "Fora do território da vila (raio de " + RAIO_VILA + " blocos).";
        }
        if (sobrepoe(o.getX(), o.getZ(), w, dd, d.center.getX() - 2, d.center.getZ() - 2, 5, 5)) {
            return "Muito perto do Centro da Nação.";
        }
        for (Edificio e : d.edificios) {
            if (sobrepoe(o.getX(), o.getZ(), w, dd,
                    e.pos.getX() - 1, e.pos.getZ() - 1, e.tipo.largura() + 2, e.tipo.profundidade() + 2)) {
                return "Já existe uma construção neste local.";
            }
        }

        int[][] cantos = { { 0, 0 }, { w - 1, 0 }, { 0, dd - 1 }, { w - 1, dd - 1 } };
        for (int[] c : cantos) {
            BlockPos p = new BlockPos(o.getX() + c[0], o.getY(), o.getZ() + c[1]);
            if (!level.isLoaded(p)) return "Essa área ainda não está carregada. Aproxime a câmera.";
            int h = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p).getY();
            if (Math.abs(h - (o.getY() + 1)) > 4) {
                return "Terreno irregular demais (ou com árvores altas). Escolha um local mais plano.";
            }
        }
        return null;
    }

    /** Nivela o terreno: aterra por baixo e limpa o volume acima do piso. */
    public static void preparar(ServerLevel level, Edificio e) {
        TipoEdificio t = e.tipo;
        BlockPos o = e.pos;
        int altura = Estruturas.alturaReal(t) + 2;
        BlockState ar = Blocks.AIR.defaultBlockState();
        BlockState terra = Blocks.DIRT.defaultBlockState();

        for (int x = 0; x < t.largura(); x++) {
            for (int z = 0; z < t.profundidade(); z++) {
                for (int y = -1; y >= -8; y--) {
                    BlockPos p = o.offset(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || s.canBeReplaced()) level.setBlock(p, terra, 2);
                    else break;
                }
                for (int y = 0; y <= altura; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, ar, 2);
                }
            }
        }
        e.preparado = true;
    }

    /** Ergue os blocos da estrutura até o índice alvo (já respeitando o que foi colocado antes). */
    public static void colocarAte(ServerLevel level, Edificio e, int alvo) {
        List<Estruturas.Bloco> lista = Estruturas.gerar(e.tipo);
        int fim = Math.min(alvo, lista.size());
        for (int i = e.colocados; i < fim; i++) {
            Estruturas.Bloco b = lista.get(i);
            level.setBlock(e.pos.offset(b.x(), b.y(), b.z()), b.estado(), 2);
        }
        e.colocados = Math.max(e.colocados, fim);
    }

    /** Limpa a obra (usada ao cancelar). */
    public static void demolir(ServerLevel level, Edificio e) {
        if (!level.isLoaded(e.pos)) return;
        int altura = Estruturas.alturaReal(e.tipo) + 2;
        BlockState ar = Blocks.AIR.defaultBlockState();
        for (int x = 0; x < e.tipo.largura(); x++) {
            for (int z = 0; z < e.tipo.profundidade(); z++) {
                for (int y = 0; y <= altura; y++) {
                    BlockPos p = e.pos.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, ar, 2);
                }
            }
        }
    }
}
