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

import io.github.xponer2.translator.registry.BlockEntityType;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.math.BlockPos;

/**
 * 1.21 {@code SkullBlockEntity} for the mods' heads that extend it (Supplementaries' enderman and spider heads): 1.12.2's
 * skull, made with the 1.21 constructors (1.12.2's takes nothing: the mod's constructor couldn't run and its heads had
 * no block entity). The E line: a vanilla skull is still 1.12.2's.
 */
public class SkullBlockEntity extends TileEntitySkull {
	@Nullable
	private final BlockEntityType<?> type;

	/** 1.12.2 makes a saved one with this. */
	public SkullBlockEntity() {
		this.type = null;
	}

	public SkullBlockEntity(BlockPos pos, IBlockState state) {
		this(null, pos, state);
	}

	public SkullBlockEntity(@Nullable BlockEntityType<?> type, BlockPos pos, IBlockState state) {
		this.type = type;
		this.setPos(pos.toImmutable());
	}

	@Nullable
	public BlockEntityType<?> getType() {
		return this.type;
	}

	/**
	 * 1.21 {@code getOwnerProfile()}: the skull's player as the carried ResolvableProfile (1.12.2 keeps the GameProfile).
	 * Twilight Forest's skull candles keep their head's owner when broken, picked and placed.
	 */
	@Nullable
	public net.minecraft.world.item.component.ResolvableProfile getOwnerProfile() {
		com.mojang.authlib.GameProfile profile = this.getPlayerProfile();
		if (profile == null) {
			return null;
		}
		try {
			return (net.minecraft.world.item.component.ResolvableProfile) Class.forName("net.minecraft.world.item.component.ResolvableProfile", true,
					net.minecraft.launchwrapper.Launch.classLoader).getConstructor(com.mojang.authlib.GameProfile.class).newInstance(profile);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** 1.21 {@code setOwner(profile)}: the carried ResolvableProfile's GameProfile as the skull's player. */
	public void setOwner(@Nullable net.minecraft.world.item.component.ResolvableProfile owner) {
		com.mojang.authlib.GameProfile profile = null;
		if (owner != null) {
			try {
				Object found = owner.getClass().getMethod("gameProfile").invoke(owner);
				profile = found instanceof com.mojang.authlib.GameProfile game ? game : null;
			} catch (ReflectiveOperationException | RuntimeException e) {
				profile = null;
			}
		}
		this.setPlayerProfile(profile);
	}

	/** Saved with its class's id; a class no type registered gets the type's id (1.12.2 threw "missing a mapping"). */
	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.nbt.NBTTagCompound writeToNBT(net.minecraft.nbt.NBTTagCompound tag) {
		if (net.minecraft.tileentity.TileEntity.getKey(this.getClass()) == null) {
			net.minecraft.util.ResourceLocation id = this.type == null ? null
					: io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.util.Registries.BLOCK_ENTITY_TYPE).getKey(this.type);
			if (id == null) {
				id = new net.minecraft.util.ResourceLocation("translator", "skull_" + this.getClass().getName().toLowerCase(java.util.Locale.ROOT).replace('.', '_').replace('$', '_'));
			}
			net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(this.getClass(), id);
		}
		super.writeToNBT(tag);
		try {
			this.saveAdditional(tag, io.github.xponer2.translator.registry.RegistryAccess.INSTANCE);
		} catch (RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".saveAdditional", e);
		}
		return tag;
	}

	/** 1.12.2 reads the skull; then the mod's 1.21 loadAdditional (its own data). */
	@Override
	public void readFromNBT(net.minecraft.nbt.NBTTagCompound tag) {
		super.readFromNBT(tag);
		try {
			this.loadAdditional(tag, io.github.xponer2.translator.registry.RegistryAccess.INSTANCE);
		} catch (RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".loadAdditional", e);
		}
	}

	/** 1.21 {@code loadAdditional(tag, registries)}: the skull's own data is 1.12.2's (read before); a mod's override adds its own. */
	protected void loadAdditional(net.minecraft.nbt.NBTTagCompound tag, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
	}

	/** 1.21 {@code saveAdditional(tag, registries)}: as loadAdditional. */
	protected void saveAdditional(net.minecraft.nbt.NBTTagCompound tag, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
	}

	/** 1.21 {@code isValidBlockState(state)}: any state (1.12.2 doesn't tie block entities to blocks). */
	public boolean isValidBlockState(IBlockState state) {
		return true;
	}

	/** 1.21 {@code new SkullBlockEntity(pos, state)} of a vanilla head: 1.12.2's skull there. */
	public static TileEntitySkull newTileEntitySkull(BlockPos pos, IBlockState state) {
		TileEntitySkull skull = new TileEntitySkull();
		skull.setPos(pos.toImmutable());
		return skull;
	}
}
