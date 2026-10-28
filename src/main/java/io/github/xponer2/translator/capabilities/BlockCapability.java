package io.github.xponer2.translator.capabilities;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.capabilities.Capability;

/**
 * 1.21 block capability (with a context, usually the side) on a 1.12.2 Forge capability. Providers registered in
 * {@link RegisterCapabilitiesEvent} are attached to the tile entities of their type (see {@link CapabilityAttacher}).
 */
public final class BlockCapability<T, C> {
	private final Supplier<Capability<T>> capability;

	BlockCapability(Supplier<Capability<T>> capability) {
		this.capability = capability;
	}

	/** The Forge capability; null until Forge injected it (pre-init). */
	@Nullable
	public Capability<T> forge() {
		return this.capability.get();
	}

	/** 1.21 level.getCapability(capability, pos, side), for tile entities. */
	@Nullable
	public T getCapability(IBlockAccess level, BlockPos pos, @Nullable C context) {
		var cap = this.forge();
		var tile = level.getTileEntity(pos);
		if (cap == null || tile == null) {
			return null;
		}

		var side = context instanceof EnumFacing facing ? facing : null;
		return tile.hasCapability(cap, side) ? tile.getCapability(cap, side) : null;
	}

	@Nullable
	public T getCapability(TileEntity tile, @Nullable C context) {
		var cap = this.forge();
		var side = context instanceof EnumFacing facing ? facing : null;
		return cap != null && tile.hasCapability(cap, side) ? tile.getCapability(cap, side) : null;
	}
}
