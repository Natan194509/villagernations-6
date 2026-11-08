package com.example.villagernations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Geradores das estruturas físicas. Cada edifício vira uma lista de blocos relativos ao canto mínimo
 * da pegada (x, z a partir de 0; y = 0 é o piso). A lista é ordenada de baixo para cima, então a obra
 * "sobe" conforme o progresso.
 */
public final class Estruturas {
    public record Bloco(int x, int y, int z, BlockState estado) {}

    private record Paleta(Block piso, Block parede, Block pilar, Block janela, Block porta,
                          Block laje, Block escada, Block cumeeira) {}

    private enum Teto { PLANO, DUAS_AGUAS, AMEIAS }

    private static final Map<TipoEdificio, List<Bloco>> CACHE = new EnumMap<>(TipoEdificio.class);

    private Estruturas() {}

    public static synchronized List<Bloco> gerar(TipoEdificio tipo) {
        List<Bloco> lista = CACHE.get(tipo);
        if (lista == null) {
            lista = construir(tipo);
            lista.sort(Comparator.comparingInt(Bloco::y));
            CACHE.put(tipo, lista);
        }
        return lista;
    }

    /** Altura total da estrutura (maior y + 1). */
    public static int alturaReal(TipoEdificio tipo) {
        int max = 0;
        for (Bloco b : gerar(tipo)) max = Math.max(max, b.y());
        return max + 1;
    }

    // ------------------------------------------------------------ utilidades

    private static void set(List<Bloco> l, int x, int y, int z, BlockState s) {
        l.add(new Bloco(x, y, z, s));
    }

    private static void set(List<Bloco> l, int x, int y, int z, Block b) {
        l.add(new Bloco(x, y, z, b.defaultBlockState()));
    }

    private static void porta(List<Bloco> l, int x, int z, Block porta) {
        set(l, x, 1, z, porta.defaultBlockState());
        set(l, x, 2, z, porta.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void camas(List<Bloco> l, int w, int quantidade, int y) {
        for (int i = 0; i < quantidade; i++) {
            int x = 1 + i * 2;
            if (x > w - 2) break;
            set(l, x, y, 1, Blocks.RED_BED.defaultBlockState()
                    .setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT));
            set(l, x, y, 2, Blocks.RED_BED.defaultBlockState()
                    .setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD));
        }
    }

    /** Caixa com piso, paredes, janelas, porta, andares e telhado. */
    private static void caixa(List<Bloco> l, int w, int d, int andares, int altAndar, Paleta p,
                              boolean janelasGrandes, Teto teto) {
        int topo = andares * altAndar;

        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) set(l, x, 0, z, p.piso());
        }
        for (int a = 1; a < andares; a++) {
            for (int x = 1; x < w - 1; x++) {
                for (int z = 1; z < d - 1; z++) set(l, x, a * altAndar, z, p.piso());
            }
        }

        int portaX = w / 2;
        for (int y = 1; y <= topo; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    boolean borda = x == 0 || x == w - 1 || z == 0 || z == d - 1;
                    if (!borda) continue;
                    if (z == 0 && x == portaX && y <= 2) continue;
                    boolean canto = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                    Block bloco = p.parede();
                    if (canto) {
                        bloco = p.pilar();
                    } else {
                        int nivel = (y - 1) % altAndar;
                        boolean alturaJanela = janelasGrandes ? (nivel >= 1 && nivel <= altAndar - 2) : nivel == 1;
                        boolean posJanela = janelasGrandes ? ((x + z) % 2 == 0) : ((x + z) % 3 == 1);
                        boolean perto = z == 0 && Math.abs(x - portaX) <= 1 && y <= 2;
                        if (alturaJanela && posJanela && !perto) bloco = p.janela();
                    }
                    set(l, x, y, z, bloco);
                }
            }
        }
        porta(l, portaX, 0, p.porta());

        int base = topo + 1;
        switch (teto) {
            case PLANO -> {
                for (int x = 0; x < w; x++) {
                    for (int z = 0; z < d; z++) set(l, x, base, z, p.laje());
                }
            }
            case DUAS_AGUAS -> {
                BlockState esq = p.escada().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
                BlockState dir = p.escada().defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
                for (int k = 0; k * 2 < w; k++) {
                    int xl = k;
                    int xr = w - 1 - k;
                    int y = base + k;
                    for (int z = 0; z < d; z++) {
                        if (xl == xr) {
                            set(l, xl, y, z, p.cumeeira());
                        } else {
                            set(l, xl, y, z, esq);
                            set(l, xr, y, z, dir);
                            if (z == 0 || z == d - 1) {
                                for (int x = xl + 1; x < xr; x++) set(l, x, y, z, p.parede());
                            }
                        }
                    }
                }
            }
            case AMEIAS -> {
                for (int x = 1; x < w - 1; x++) {
                    for (int z = 1; z < d - 1; z++) set(l, x, topo, z, p.piso());
                }
                for (int x = 0; x < w; x++) {
                    for (int z = 0; z < d; z++) {
                        boolean borda = x == 0 || x == w - 1 || z == 0 || z == d - 1;
                        if (borda && (x + z) % 2 == 0) set(l, x, base, z, p.parede());
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------- formas

    private static List<Bloco> construir(TipoEdificio t) {
        List<Bloco> l = new ArrayList<>();
        int w = t.largura();
        int d = t.profundidade();

        switch (t) {
            case CABANA -> {
                caixa(l, w, d, 1, 4, new Paleta(Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.GLASS,
                        Blocks.OAK_DOOR, Blocks.OAK_SLAB, Blocks.OAK_STAIRS, Blocks.OAK_PLANKS), false, Teto.PLANO);
                camas(l, w, 2, 1);
                set(l, w - 2, 1, d - 2, Blocks.LANTERN);
            }
            case CASA_PEDRA -> {
                caixa(l, w, d, 1, 4, new Paleta(Blocks.OAK_PLANKS, Blocks.COBBLESTONE, Blocks.OAK_LOG, Blocks.GLASS,
                        Blocks.OAK_DOOR, Blocks.COBBLESTONE_SLAB, Blocks.OAK_STAIRS, Blocks.OAK_PLANKS), false, Teto.DUAS_AGUAS);
                camas(l, w, 3, 1);
                set(l, w - 2, 1, d - 2, Blocks.LANTERN);
            }
            case CASA_MEDIEVAL -> {
                caixa(l, w, d, 2, 4, new Paleta(Blocks.OAK_PLANKS, Blocks.WHITE_TERRACOTTA, Blocks.DARK_OAK_LOG, Blocks.GLASS,
                        Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS), false, Teto.DUAS_AGUAS);
                camas(l, w, 3, 1);
                camas(l, w, 3, 5);
                set(l, w - 2, 1, d - 2, Blocks.LANTERN);
            }
            case TAVERNA -> {
                caixa(l, w, d, 1, 4, new Paleta(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.GLASS,
                        Blocks.SPRUCE_DOOR, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS), false, Teto.DUAS_AGUAS);
                for (int x = 1; x < w - 1; x += 2) set(l, x, 1, d - 2, Blocks.BARREL);
                for (int x = 2; x < w - 2; x += 2) set(l, x, 1, 3, Blocks.OAK_SLAB);
                set(l, w / 2, 1, d - 3, Blocks.LANTERN);
            }
            case BIBLIOTECA -> {
                caixa(l, w, d, 1, 5, new Paleta(Blocks.OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.GLASS,
                        Blocks.DARK_OAK_DOOR, Blocks.STONE_BRICK_SLAB, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS), true, Teto.DUAS_AGUAS);
                for (int x = 1; x < w - 1; x++) {
                    set(l, x, 1, d - 2, Blocks.BOOKSHELF);
                    set(l, x, 2, d - 2, Blocks.BOOKSHELF);
                }
                set(l, w / 2, 1, d / 2, Blocks.LANTERN);
            }
            case MANSAO -> {
                caixa(l, w, d, 2, 4, new Paleta(Blocks.OAK_PLANKS, Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.GLASS,
                        Blocks.DARK_OAK_DOOR, Blocks.SANDSTONE_SLAB, Blocks.BRICK_STAIRS, Blocks.BRICKS), false, Teto.DUAS_AGUAS);
                camas(l, w, 4, 1);
                camas(l, w, 4, 5);
                set(l, w - 2, 1, d - 2, Blocks.LANTERN);
            }
            case HOSPITAL -> {
                caixa(l, w, d, 2, 4, new Paleta(Blocks.SMOOTH_STONE, Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.GLASS,
                        Blocks.IRON_DOOR, Blocks.SMOOTH_STONE_SLAB, Blocks.STONE_BRICK_STAIRS, Blocks.WHITE_CONCRETE), true, Teto.PLANO);
                camas(l, w, 5, 1);
                camas(l, w, 5, 5);
                // Cruz vermelha na fachada
                int cx = w / 2;
                set(l, cx, 6, 0, Blocks.RED_CONCRETE);
                set(l, cx, 7, 0, Blocks.RED_CONCRETE);
                set(l, cx, 8, 0, Blocks.RED_CONCRETE);
                set(l, cx - 1, 7, 0, Blocks.RED_CONCRETE);
                set(l, cx + 1, 7, 0, Blocks.RED_CONCRETE);
            }
            case CORTICO -> {
                caixa(l, w, d, 3, 4, new Paleta(Blocks.SPRUCE_PLANKS, Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.GLASS,
                        Blocks.OAK_DOOR, Blocks.STONE_BRICK_SLAB, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS), true, Teto.PLANO);
                camas(l, w, 3, 1);
                camas(l, w, 3, 5);
                camas(l, w, 3, 9);
            }
            case PREDIO -> {
                caixa(l, w, d, 4, 4, new Paleta(Blocks.SMOOTH_STONE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.GLASS,
                        Blocks.IRON_DOOR, Blocks.SMOOTH_STONE_SLAB, Blocks.STONE_BRICK_STAIRS, Blocks.GRAY_CONCRETE), true, Teto.PLANO);
                for (int a = 0; a < 4; a++) camas(l, w, 4, 1 + a * 4);
            }
            case FABRICA -> {
                caixa(l, w, d, 1, 7, new Paleta(Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.GLASS,
                        Blocks.IRON_DOOR, Blocks.STONE_SLAB, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS), true, Teto.PLANO);
                for (int y = 1; y <= 13; y++) set(l, w - 2, y, d - 2, Blocks.BRICKS);
                for (int x = 2; x < w - 2; x += 2) set(l, x, 1, d - 3, Blocks.BARREL);
            }
            case TORRE -> {
                caixa(l, w, d, 1, 11, new Paleta(Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.GLASS,
                        Blocks.OAK_DOOR, Blocks.STONE_BRICK_SLAB, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS), false, Teto.AMEIAS);
            }
            case DEPOSITO -> {
                caixa(l, w, d, 1, 4, new Paleta(Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.GLASS,
                        Blocks.OAK_DOOR, Blocks.OAK_SLAB, Blocks.OAK_STAIRS, Blocks.OAK_PLANKS), false, Teto.PLANO);
                for (int x = 1; x < w - 1; x++) set(l, x, 1, d - 2, Blocks.BARREL);
                set(l, 1, 2, d - 2, Blocks.BARREL);
                set(l, w - 2, 2, d - 2, Blocks.BARREL);
                set(l, 1, 1, d - 3, Blocks.HAY_BLOCK);
                set(l, w - 2, 1, d - 3, Blocks.HAY_BLOCK);
            }
            case SERRARIA -> galpaoAberto(l, w, d);
            case FAZENDA -> fazenda(l, w, d);
            case PEDREIRA -> pedreira(l, w, d);
            case MERCADO -> mercado(l, w, d);
        }
        return l;
    }

    private static void galpaoAberto(List<Bloco> l, int w, int d) {
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) set(l, x, 0, z, Blocks.OAK_PLANKS);
        }
        int[] zs = { 0, d / 2, d - 1 };
        for (int z : zs) {
            for (int y = 1; y <= 3; y++) {
                set(l, 0, y, z, Blocks.OAK_LOG);
                set(l, w - 1, y, z, Blocks.OAK_LOG);
            }
        }
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) set(l, x, 4, z, Blocks.OAK_SLAB);
        }
        set(l, w / 2, 1, d - 2, Blocks.CRAFTING_TABLE);
        set(l, 1, 1, 1, Blocks.OAK_LOG);
        set(l, 2, 1, 1, Blocks.OAK_LOG);
        set(l, 1, 2, 1, Blocks.OAK_LOG);
        set(l, w - 2, 1, 1, Blocks.OAK_LOG);
        set(l, w / 2, 3, d / 2, Blocks.LANTERN);
    }

    private static void fazenda(List<Bloco> l, int w, int d) {
        int cx = w / 2;
        int cz = d / 2;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                boolean borda = x == 0 || x == w - 1 || z == 0 || z == d - 1;
                if (borda) {
                    set(l, x, 0, z, Blocks.DIRT);
                    boolean entrada = x == cx && z == d - 1;
                    if (!entrada) set(l, x, 1, z, Blocks.OAK_FENCE);
                } else if (x == cx && z == cz) {
                    set(l, x, 0, z, Blocks.WATER);
                } else {
                    set(l, x, 0, z, Blocks.FARMLAND);
                    set(l, x, 1, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
                }
            }
        }
    }

    private static void pedreira(List<Bloco> l, int w, int d) {
        int cx = w / 2;
        int cz = d / 2;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                set(l, x, 0, z, Blocks.STONE);
                boolean area = Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1;
                int altura = area ? 0 : (x * 7 + z * 13) % 4;
                for (int y = 1; y <= altura; y++) {
                    Block b = (x + z + y) % 3 == 0 ? Blocks.COBBLESTONE : ((x * z + y) % 2 == 0 ? Blocks.STONE : Blocks.ANDESITE);
                    set(l, x, y, z, b);
                }
            }
        }
        set(l, cx, 1, cz, Blocks.STONECUTTER);
    }

    private static void mercado(List<Bloco> l, int w, int d) {
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) set(l, x, 0, z, Blocks.STONE_BRICKS);
        }
        int[] xs = { 0, w / 2, w - 1 };
        for (int x : xs) {
            for (int y = 1; y <= 3; y++) {
                set(l, x, y, 0, Blocks.OAK_FENCE);
                set(l, x, y, d - 1, Blocks.OAK_FENCE);
            }
        }
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                set(l, x, 4, z, (x / 2) % 2 == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL);
            }
        }
        for (int x = 2; x < w - 2; x++) set(l, x, 1, 2, Blocks.OAK_SLAB);
        set(l, 1, 1, d - 2, Blocks.BARREL);
        set(l, w - 2, 1, d - 2, Blocks.BARREL);
        set(l, w / 2, 1, d - 2, Blocks.HAY_BLOCK);
        set(l, w / 2, 2, 2, Blocks.LANTERN);
    }
}
