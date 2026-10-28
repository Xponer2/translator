/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.block;

import io.github.xponer2.translator.registry.BlockEntityType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * A block with a tile entity (1.21 {@code EntityBlock}). A 1.12.2 block also has to answer {@code hasTileEntity} and
 * {@code createTileEntity}: the converter adds them to the classes implementing this. The ticker is asked for by the
 * tile entity ({@link BlockEntity}).
 */
public interface EntityBlock {
    @Nullable
    TileEntity newBlockEntity(BlockPos pos, IBlockState state);

    @Nullable
    default <T extends TileEntity> BlockEntityTicker<T> getTicker(World level, IBlockState state, BlockEntityType<T> type) {
        return null;
    }
}
