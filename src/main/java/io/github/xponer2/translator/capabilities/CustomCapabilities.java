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
package io.github.xponer2.translator.capabilities;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

/**
 * The capabilities a mod makes itself (1.21 {@code ItemCapability.create}, Curios' item and entity capabilities): a
 * 1.12.2 Forge capability for each name. 1.21 saves no capability data, so nothing is saved for it either.
 */
final class CustomCapabilities {
	private static final Map<ResourceLocation, Capability<?>> FORGE = new ConcurrentHashMap<>();
	/** The 1.21 capability objects by kind and name: the same name gives the same object, as on NeoForge. */
	static final Map<String, Object> CREATED = new ConcurrentHashMap<>();

	private static final Capability.IStorage<Object> NO_STORAGE = new Capability.IStorage<Object>() {
		@Override
		public NBTBase writeNBT(Capability<Object> capability, Object instance, EnumFacing side) {
			return null;
		}

		@Override
		public void readNBT(Capability<Object> capability, Object instance, EnumFacing side, NBTBase nbt) {
		}
	};

	private CustomCapabilities() {
	}

	@SuppressWarnings("unchecked")
	static <T> Capability<T> forge(ResourceLocation name) {
		return (Capability<T>) FORGE.computeIfAbsent(name, CustomCapabilities::make);
	}

	/** Forge 1.12.2 makes capabilities for the classes registered with it; this one is made for its name. */
	private static Capability<?> make(ResourceLocation name) {
		try {
			var constructor = Capability.class.getDeclaredConstructor(String.class, Capability.IStorage.class, Callable.class);
			constructor.setAccessible(true);
			Callable<Object> factory = () -> null;
			return constructor.newInstance(name.toString(), NO_STORAGE, factory);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Can't make the capability " + name, e);
		}
	}
}
