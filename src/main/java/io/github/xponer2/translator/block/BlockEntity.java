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

import io.github.xponer2.translator.registry.BlockEntityType;
import io.github.xponer2.translator.registry.HolderLookup;
import io.github.xponer2.translator.registry.RegistryAccess;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The 1.21 block entity on a 1.12.2 tile entity: subclasses keep the 1.21 constructor {@code (type, pos, state)}
 * (plus the no-arg one 1.12.2 needs to load them) and the 1.21 {@code loadAdditional}/{@code saveAdditional}; this
 * class maps the 1.12.2 NBT and tick methods to them. Ticking: the ticker of the block ({@link EntityBlock#getTicker}),
 * asked for on the first tick. The converter makes {@code extends BlockEntity} of the ported code extend this.
 */
public class BlockEntity extends TileEntity implements ITickable {
	private final BlockEntityType<?> type;
	@Nullable
	private BlockEntityTicker<TileEntity> ticker;
	private boolean tickerResolved;

	public BlockEntity(BlockEntityType<?> type, BlockPos pos, @Nullable IBlockState state) {
		this.type = type;
		this.pos = pos.toImmutable();
	}

	public BlockEntityType<?> getType() {
		return this.type;
	}

	public IBlockState getBlockState() {
		return this.world != null ? this.world.getBlockState(this.pos) : this.getBlockType().getDefaultState();
	}

	// 1.21 save/load

	protected void loadAdditional(NBTTagCompound tag, HolderLookup.Provider registries) { }

	protected void saveAdditional(NBTTagCompound tag, HolderLookup.Provider registries) { }

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		this.loadAdditional(tag, RegistryAccess.INSTANCE);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		this.saveAdditional(tag, RegistryAccess.INSTANCE);
		return tag;
	}

	// NeoForge loads the tag of an update packet (1.12.2 ignores it)
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		this.readFromNBT(packet.getNbtCompound());
	}

	// a state change of the same block (facing, running) keeps the block entity, like on 1.21
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void update() {
		if (!this.tickerResolved) {
			this.tickerResolved = true;
			var state = this.getBlockState();
			if (state.getBlock() instanceof EntityBlock block) {
				this.ticker = (BlockEntityTicker<TileEntity>) block.getTicker(this.world, state, (BlockEntityType<TileEntity>) this.type);
			}
		}
		if (this.ticker != null) {
			this.ticker.tick(this.world, this.pos, this.getBlockState(), this);
		}
	}

	// 1.21 setChanged
	public void setChanged() {
		this.markDirty();
	}
}
