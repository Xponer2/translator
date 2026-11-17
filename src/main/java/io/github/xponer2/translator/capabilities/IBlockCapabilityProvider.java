/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.capabilities;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** NeoForge {@code IBlockCapabilityProvider}: a block's capability from its world, position, state and block entity. */
@FunctionalInterface
public interface IBlockCapabilityProvider<T, C> {
	@Nullable
	T getCapability(World level, BlockPos pos, IBlockState state, @Nullable TileEntity blockEntity, C context);
}
