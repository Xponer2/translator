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
package io.github.xponer2.translator.registry;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * NeoForge {@code DataPackRegistryEvent} (neoforge-1.21.1-runtime.tsv). Nobody posted it, so the registries the mods
 * fill from their data files stayed empty: Moonlight's soft fluids, so every Supplementaries jar, goblet and faucet failed
 * ("Failed to get object from registry: moonlight:soft_fluid / moonlight:empty").
 */
public abstract class DataPackRegistryEvent extends Event {
	/**
	 * NeoForge {@code DataPackRegistryEvent.NewRegistry}: posted after {@link NewRegistryEvent}. A registry named here is
	 * filled from the files {@code data/<namespace>/<registry dir>/<path>.json} of the 1.21.1 mods
	 * ({@link DataPackRegistries}).
	 */
	public static final class NewRegistry extends DataPackRegistryEvent {
		public <T> void dataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
			this.dataPackRegistry(key, codec, null);
		}

		public <T> void dataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> networkCodec) {
			this.dataPackRegistry(key, codec, networkCodec, builder -> { });
		}

		/** The network codec isn't needed: both sides read the same files from the mods' jars. */
		public <T> void dataPackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> networkCodec,
				@Nullable Consumer<RegistryBuilder<T>> consumer) {
			DataPackRegistries.add(key, codec);
		}
	}
}
