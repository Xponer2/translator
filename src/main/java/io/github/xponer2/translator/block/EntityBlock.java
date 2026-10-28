package io.github.xponer2.translator.block;

import io.github.xponer2.translator.registry.BlockEntityType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * A block with a tile entity (1.21 {@code EntityBlock}). A 1.12.2 block also has to answer {@code hasTileEntity} and
 * {@code createTileEntity} (Cucumber's BaseTileEntityBlock does).
 */
public interface EntityBlock {
    @Nullable
    TileEntity newBlockEntity(BlockPos pos, IBlockState state);

    @Nullable
    default <T extends TileEntity> BlockEntityTicker<T> getTicker(World level, IBlockState state, BlockEntityType<T> type) {
        return null;
    }
}
