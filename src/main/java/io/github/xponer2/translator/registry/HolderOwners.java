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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.HolderOwner;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21's holder owners (the carried {@code HolderOwner}): the registry a holder belongs to, for the codecs that check a
 * holder can be written where they are (RegistryFixedCodec: Moonlight's soft fluid stacks, so every Supplementaries jar,
 * goblet and faucet failed to save). 1.12.2 has one set of registries: every holder belongs to every owner. Helper of
 * Registry, Registry.Reference and Holder (R lines), loaded only by the mods' code, after the 1.21.1 API is carried.
 */
public final class HolderOwners {
	private static final Map<ResourceLocation, Owner> OWNERS = new ConcurrentHashMap<>();
	private static final Owner ANY = new Owner(null);

	private HolderOwners() {
	}

	/** The owner of a registry (one per registry key). */
	public static HolderOwner<?> of(@javax.annotation.Nullable ResourceKey<?> registry) {
		return registry == null ? ANY : OWNERS.computeIfAbsent(registry.location(), Owner::new);
	}

	/** 1.21 {@code Registry.holderOwner()}. */
	public static HolderOwner<?> holderOwner(Registry<?> registry) {
		return of(registry.key());
	}

	/** 1.21 {@code Holder.Reference.owner} (the field): the owner of the registry the holder is of. */
	public static HolderOwner<?> owner(Registry.Reference<?> reference) {
		ResourceKey<?> key = reference.key();
		return key == null ? ANY : of(ResourceKey.createRegistryKey(key.registry()));
	}

	/** 1.21 {@code Holder.canSerializeIn(owner)}: one set of registries, so always. */
	public static boolean canSerializeIn(Holder<?> holder, HolderOwner<?> owner) {
		return true;
	}

	/** An owner: canSerializeIn (the interface's default) holds for itself; holders don't ask it here. */
	static final class Owner implements HolderOwner<Object> {
		@javax.annotation.Nullable
		private final ResourceLocation registry;

		Owner(@javax.annotation.Nullable ResourceLocation registry) {
			this.registry = registry;
		}

		@Override
		public String toString() {
			return "HolderOwner[" + this.registry + "]";
		}
	}
}
