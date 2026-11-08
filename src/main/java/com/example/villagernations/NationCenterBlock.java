package com.example.villagernations;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Centro da Nação. O primeiro clique com a mão vazia abre a tela de fundação da vila.
 * Depois de fundada, o jogador fica no Modo Prefeito e usa o painel (tecla N).
 */
public class NationCenterBlock extends Block {
    public NationCenterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.sidedSuccess(false);
        }
        if (serverLevel.dimension() != Level.OVERWORLD) {
            NationLogic.msg(player, "O Centro da Nação só funciona no Overworld.");
            return InteractionResult.sidedSuccess(false);
        }

        NationData data = NationData.get(serverLevel.getServer());
        if (data.center == null) {
            NationLogic.pedirConfiguracao(serverPlayer, pos);
            return InteractionResult.sidedSuccess(false);
        }

        if (!data.center.equals(pos)) {
            if (serverLevel.getBlockState(data.center).is(this)) {
                BlockPos c = data.center;
                NationLogic.msg(player, "Sua vila já tem um Centro em " + c.getX() + ", " + c.getY() + ", " + c.getZ() + ".");
                return InteractionResult.sidedSuccess(false);
            }
            data.center = pos.immutable();
            data.setDirty();
            NationLogic.msg(player, "O Centro da Nação foi movido para este bloco.");
        }

        NationLogic.entrarModoPrefeito(serverPlayer, data, true);
        NationLogic.abrirPainel(serverPlayer, data);
        return InteractionResult.sidedSuccess(false);
    }
}
