package io.github.xponer2.translator.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Ticks a tile entity (1.21 {@code BlockEntityTicker}); 1.12.2 ticks {@code ITickable} tile entities, Cucumber's
 * BaseTileEntity asks its block for the ticker.
 */
@FunctionalInterface
public interface BlockEntityTicker<T extends TileEntity> {
    void tick(World level, BlockPos pos, IBlockState state, T blockEntity);
}
