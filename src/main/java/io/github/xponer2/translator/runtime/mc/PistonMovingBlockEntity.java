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
package io.github.xponer2.translator.runtime.mc;

import javax.annotation.Nullable;

import io.github.xponer2.translator.block.BlockEntityTicker;
import io.github.xponer2.translator.block.EntityBlock;
import io.github.xponer2.translator.registry.BlockEntityType;
import io.github.xponer2.translator.registry.HolderLookup;
import io.github.xponer2.translator.registry.RegistryAccess;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * 1.21 {@code PistonMovingBlockEntity} for the mods' moving block entities (1.12.2's TileEntityPiston): its 1.21
 * constructors, the fields the mods read (deathTicks, lastTicked), the 1.21 save and load, and the block's 1.21 ticker
 * in place of 1.12.2's piston update when the block has one. Supplementaries' pulley and slidy blocks had no block
 * entity at all (their constructors called 1.21 ones 1.12.2's hasn't).
 */
public class PistonMovingBlockEntity extends TileEntityPiston {
	@Nullable
	private final BlockEntityType<?> type;
	public int deathTicks;
	public long lastTicked;
	private boolean tickerResolved;
	@Nullable
	private BlockEntityTicker<TileEntity> ticker;

	/** 1.12.2 makes a saved one with this. */
	public PistonMovingBlockEntity() {
		this.type = null;
	}

	public PistonMovingBlockEntity(BlockPos pos, IBlockState state) {
		this.type = null;
		this.setPos(pos.toImmutable());
	}

	public PistonMovingBlockEntity(BlockPos pos, IBlockState state, IBlockState movedState, EnumFacing direction, boolean extending,
			boolean isSourcePiston) {
		super(movedState, direction, extending, isSourcePiston);
		this.type = null;
		this.setPos(pos.toImmutable());
	}

	@Nullable
	public BlockEntityType<?> getType() {
		return this.type;
	}

	/** 1.21 {@code isValidBlockState(state)}: any state (1.12.2 doesn't tie block entities to blocks). */
	public boolean isValidBlockState(IBlockState state) {
		return true;
	}

	/** 1.21 load: 1.12.2's piston data (what a mod's own load adds comes after its super call). */
	protected void loadAdditional(NBTTagCompound tag, HolderLookup.Provider registries) {
		super.readFromNBT(tag);
	}

	/** 1.21 save: 1.12.2 wrote the piston data already. */
	protected void saveAdditional(NBTTagCompound tag, HolderLookup.Provider registries) {
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		this.loadAdditional(tag, RegistryAccess.INSTANCE);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		this.saveAdditional(tag, RegistryAccess.INSTANCE);
		return tag;
	}

	/** The block's 1.21 ticker when it has one (the mod moves its block itself), else 1.12.2's piston. */
	@Override
	@SuppressWarnings("unchecked")
	public void update() {
		if (!this.tickerResolved) {
			this.tickerResolved = true;
			IBlockState state = this.world == null ? null : this.world.getBlockState(this.pos);
			// the mod's own type (its getType: the 1.21 constructors take none)
			BlockEntityType<?> own = this.getType();
			if (state != null && state.getBlock() instanceof EntityBlock block && own != null) {
				this.ticker = (BlockEntityTicker<TileEntity>) block.getTicker(this.world, state, (BlockEntityType<TileEntity>) own);
			}
		}
		if (this.ticker == null) {
			super.update();
			return;
		}
		try {
			this.ticker.tick(this.world, this.pos, this.world.getBlockState(this.pos), this);
		} catch (RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".tick", e);
		}
	}
}
