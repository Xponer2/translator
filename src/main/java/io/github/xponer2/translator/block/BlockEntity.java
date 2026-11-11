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

	/** The state it was made for (1.21 keeps it): what getBlockState gives before the block entity is in a world. */
	@Nullable
	private IBlockState blockState;

	public BlockEntity(BlockEntityType<?> type, BlockPos pos, @Nullable IBlockState state) {
		this.type = type;
		this.pos = pos.toImmutable();
		this.blockState = state;
	}

	public BlockEntityType<?> getType() {
		return this.type;
	}

	public IBlockState getBlockState() {
		if (this.world != null) {
			return this.world.getBlockState(this.pos);
		}
		if (this.blockState != null) {
			return this.blockState;
		}
		// made without a state (1.12.2 loading a saved block entity): its block's, else air
		return this.blockType != null ? this.blockType.getDefaultState() : net.minecraft.init.Blocks.AIR.getDefaultState();
	}

	/** 1.21 {@code setBlockState}: the state it is for (a block that changed its state). */
	public void setBlockState(IBlockState state) {
		this.blockState = state;
	}

	// 1.21 save/load

	protected void loadAdditional(NBTTagCompound tag, HolderLookup.Provider registries) { }

	protected void saveAdditional(NBTTagCompound tag, HolderLookup.Provider registries) { }

	/** 1.21 {@code saveCustomOnly}: what the block entity saves, without its id and position. */
	public NBTTagCompound saveCustomOnly(HolderLookup.Provider registries) {
		NBTTagCompound tag = new NBTTagCompound();
		this.saveAdditional(tag, registries);
		return tag;
	}

	/** 1.21 {@code loadCustomOnly}: loads what saveCustomOnly gave (Storage Drawers keeps it in a drawer item). */
	public void loadCustomOnly(NBTTagCompound tag, HolderLookup.Provider registries) {
		this.loadAdditional(tag, registries);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		// 1.21 tags without the position (update tags from saveWithoutMetadata, update packets) keep it: 1.12.2 would
		// move the block entity to 0,0,0, and lookups by its position (capabilities, menus) would find nothing
		BlockPos pos = this.pos;
		super.readFromNBT(tag);
		if (!tag.hasKey("x") || !tag.hasKey("y") || !tag.hasKey("z")) {
			this.pos = pos;
		}
		try {
			this.loadAdditional(tag, RegistryAccess.INSTANCE);
		} catch (RuntimeException | LinkageError e) {
			// a mod's load failing stops the chunk from loading (the world doesn't open): logged, the rest of it loads
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".loadAdditional", e);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		if (TileEntity.getKey(this.getClass()) == null) {
			// a class its type didn't register (its sample was another class): 1.12.2 can't save it otherwise
			var id = this.type == null ? null
					: io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.util.Registries.BLOCK_ENTITY_TYPE)
							.getKey(this.type);
			if (id == null) {
				// a type the registry doesn't name (registered through a mod's own helper: BOP's anomaly): its block's id
				IBlockState state = this.getBlockState();
				id = state == null ? null : state.getBlock().getRegistryName();
			}
			if (id != null) {
				net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(this.getClass(), id);
			}
		}
		super.writeToNBT(tag);
		try {
			this.saveAdditional(tag, RegistryAccess.INSTANCE);
		} catch (RuntimeException | LinkageError e) {
			// a mod's save failing crashed the server (the world saved or sent to a player): logged, what it wrote is kept
			// (Supplementaries' sign posts, whose text 1.12.2 couldn't resolve)
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".saveAdditional", e);
		}
		return tag;
	}

	// NeoForge loads the tag of an update packet (1.12.2 ignores it)
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		this.onDataPacket(net, packet, RegistryAccess.INSTANCE);
	}

	/** 1.21 signature, what original 1.21.1 mods override. */
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet, HolderLookup.Provider registries) {
		this.readFromNBT(packet.getNbtCompound());
	}

	/** 1.21 {@code getUpdateTag(registries)}; the 1.12.2 one calls it. */
	public NBTTagCompound getUpdateTag(HolderLookup.Provider registries) {
		return super.getUpdateTag();
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.getUpdateTag(RegistryAccess.INSTANCE);
	}

	/** 1.21 {@code handleUpdateTag(tag, registries)}; the 1.12.2 one calls it. */
	public void handleUpdateTag(NBTTagCompound tag, HolderLookup.Provider registries) {
		super.handleUpdateTag(tag);
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		this.handleUpdateTag(tag, RegistryAccess.INSTANCE);
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
			try {
				this.ticker.tick(this.world, this.pos, this.getBlockState(), this);
			} catch (RuntimeException | LinkageError e) {
				// a part of the 1.21 game the Translator lacks (Create's advancements, when a player placed the block):
				// logged once, where it would stop the server; the block entity ticks on
				io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".tick", e);
			}
		}
	}

	// 1.21 setChanged
	/** The components a placed item gives (1.21): 1.12.2 block items set the block entity's NBT instead. */
	protected void applyImplicitComponents(Object input) {
	}

	protected void collectImplicitComponents(io.github.xponer2.translator.component.DataComponentMap.Builder builder) {
	}

	public void removeComponentsFromTag(NBTTagCompound tag) {
	}

	/**
	 * 1.21 {@code BlockEntity.parseCustomNameSafe}: a custom name saved as text JSON, null when it can't be read
	 * (Storage Drawers' drawers with a name, when loaded).
	 */
	@Nullable
	public static net.minecraft.util.text.ITextComponent parseCustomNameSafe(String json, HolderLookup.Provider registries) {
		try {
			return net.minecraft.util.text.ITextComponent.Serializer.jsonToComponent(json);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** The 1.21 name of onChunkUnload. */
	public void onChunkUnloaded() {
	}

	@Override
	public void onChunkUnload() {
		this.onChunkUnloaded();
	}

	public void setChanged() {
		this.markDirty();
	}

	/** NeoForge {@code getModelData()}: what this block entity tells its block's model (a mod's override). */
	public io.github.xponer2.translator.client.ModelData getModelData() {
		return io.github.xponer2.translator.client.ModelData.EMPTY;
	}

	/** NeoForge {@code requestModelDataUpdate()}: the block drawn again, with the new data. */
	public void requestModelDataUpdate() {
		if (this.world != null && this.world.isRemote) {
			this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
		}
	}
}
