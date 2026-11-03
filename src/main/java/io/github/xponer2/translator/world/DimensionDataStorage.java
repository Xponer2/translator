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

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.RegistryAccess;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * The saved data of a level (1.21 {@code DimensionDataStorage}, neoforge-1.21.1-runtime.tsv): the 1.12.2 per-world
 * {@link MapStorage} of that dimension (data/<name>.dat in the dimension's folder), saved with the world when dirty.
 */
public final class DimensionDataStorage {
	private final MapStorage storage;

	private DimensionDataStorage(MapStorage storage) {
		this.storage = storage;
	}

	public static DimensionDataStorage of(World world) {
		return new DimensionDataStorage(world.getPerWorldStorage());
	}

	/** The data of that name: loaded, read from its file by the factory, or new (then saved when dirty). */
	@SuppressWarnings("unchecked")
	public <T extends SavedData> T computeIfAbsent(SavedData.Factory<T> factory, String name) {
		T data = this.get(factory, name);
		if (data == null) {
			data = factory.constructor().get();
			this.set(name, data);
		}
		return data;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T extends SavedData> T get(SavedData.Factory<T> factory, String name) {
		WorldSavedData loaded = this.storage.getOrLoadData(Raw.class, name);
		if (loaded instanceof SavedData) {
			return (T) loaded;
		}
		if (!(loaded instanceof Raw raw) || raw.tag == null) {
			return null;
		}
		T data = factory.deserializer().apply(raw.tag, RegistryAccess.INSTANCE);
		if (data != null) {
			this.set(name, data);
		}
		return data;
	}

	public void set(String name, SavedData data) {
		data.rename(name);
		this.storage.setData(name, data);
	}

	public void save() {
		this.storage.saveAllData();
	}

	/** The tag of a data file, read before the mod's deserializer makes the data of it. */
	public static final class Raw extends WorldSavedData {
		@Nullable
		NBTTagCompound tag;

		public Raw(String name) {
			super(name);
		}

		@Override
		public void readFromNBT(NBTTagCompound tag) {
			this.tag = tag;
		}

		@Override
		public NBTTagCompound writeToNBT(NBTTagCompound tag) {
			return this.tag == null ? tag : this.tag.copy();
		}
	}
}
