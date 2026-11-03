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
package io.github.xponer2.translator.world;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.HolderLookup;
import io.github.xponer2.translator.registry.RegistryAccess;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Data a mod saves with a world (1.21 {@code SavedData}, neoforge-1.21.1-supers.tsv): a 1.12.2 {@link WorldSavedData}
 * that the 1.21 code builds without a name (the {@link DimensionDataStorage} names it) and saves with
 * {@code save(tag, registries)} (renamed {@code writeToNBT} by the remap table). It is read by the mod's own
 * deserializer ({@link Factory}), never by {@link #readFromNBT}.
 */
public abstract class SavedData extends WorldSavedData {
	private static final java.lang.reflect.Field NAME = nameField();

	public SavedData() {
		super("translator_saved_data");
	}

	private static java.lang.reflect.Field nameField() {
		for (var field : WorldSavedData.class.getDeclaredFields()) {
			if (field.getType() == String.class) {
				field.setAccessible(true);
				return field;
			}
		}
		throw new IllegalStateException("WorldSavedData has no name field");
	}

	/** The file name the data is saved under (data/<name>.dat). */
	void rename(String name) {
		try {
			NAME.set(this, name);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	/** The 1.21 save: the mod's override (renamed writeToNBT by the remap table, or still save). */
	public NBTTagCompound writeToNBT(NBTTagCompound tag, HolderLookup.Provider registries) {
		return this.save(tag, registries);
	}

	public NBTTagCompound save(NBTTagCompound tag, HolderLookup.Provider registries) {
		return tag;
	}

	@Override
	public final NBTTagCompound writeToNBT(NBTTagCompound tag) {
		return this.writeToNBT(tag, RegistryAccess.INSTANCE);
	}

	/** The data is read by its factory's deserializer when it is loaded (DimensionDataStorage). */
	@Override
	public void readFromNBT(NBTTagCompound tag) {
	}

	public void setDirty() {
		this.markDirty();
	}

	/**
	 * How a kind of saved data is made: new, or read from its tag (1.21 {@code SavedData.Factory}, a record).
	 */
	public static final class Factory<T extends SavedData> {
		private final Supplier<T> constructor;
		private final BiFunction<NBTTagCompound, HolderLookup.Provider, T> deserializer;
		@Nullable
		private final Object type;

		public Factory(Supplier<T> constructor, BiFunction<NBTTagCompound, HolderLookup.Provider, T> deserializer, @Nullable Object type) {
			this.constructor = constructor;
			this.deserializer = deserializer;
			this.type = type;
		}

		public Factory(Supplier<T> constructor, BiFunction<NBTTagCompound, HolderLookup.Provider, T> deserializer) {
			this(constructor, deserializer, null);
		}

		public Supplier<T> constructor() {
			return this.constructor;
		}

		public BiFunction<NBTTagCompound, HolderLookup.Provider, T> deserializer() {
			return this.deserializer;
		}

		@Nullable
		public Object type() {
			return this.type;
		}
	}
}
