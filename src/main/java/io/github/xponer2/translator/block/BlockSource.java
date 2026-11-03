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

import javax.annotation.Nullable;

import net.minecraft.block.BlockSourceImpl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

/**
 * 1.21 dispenser source (a record there) on the 1.12.2 IBlockSource.
 */
public final class BlockSource {
	private final IBlockSource source;

	public BlockSource(IBlockSource source) {
		this.source = source;
	}

	/** 1.21 {@code new BlockSource(level, pos, state, blockEntity)}: the 1.12.2 source of that block. */
	public BlockSource(WorldServer level, BlockPos pos, IBlockState state, Object blockEntity) {
		this(new BlockSourceImpl(level, pos));
	}

	public WorldServer level() {
		return (WorldServer) this.source.getWorld();
	}

	public BlockPos pos() {
		return this.source.getBlockPos();
	}

	public IBlockState state() {
		return this.source.getBlockState();
	}

	@Nullable
	public TileEntity blockEntity() {
		return this.source.getBlockTileEntity();
	}

	public Vec3d center() {
		return new Vec3d(this.source.getX(), this.source.getY(), this.source.getZ());
	}

	public IBlockSource source() {
		return this.source;
	}
}
