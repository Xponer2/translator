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
import java.util.function.Supplier;

import io.github.xponer2.translator.util.BuiltInRegistries;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IContextSetter;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * NeoForge registration event: posted on the Forge event bus for every 1.12.2 registry event (see {@link Forwarder},
 * registered in pre-init). {@link #register} runs only for the registry being registered now. A context setter like
 * the Forge event: each handler runs with its own mod active, so registry names get no prefix warnings.
 */
public class RegisterEvent extends Event implements IContextSetter {
	@javax.annotation.Nullable
	private final RegistryEvent.Register<?> forge;
	/** The registry of this event when it is one 1.12.2 has no event for (a mod's own: AE2's key types). */
	@javax.annotation.Nullable
	private final MappedRegistry<?> mapped;

	public RegisterEvent(RegistryEvent.Register<?> forge) {
		this.forge = forge;
		this.mapped = null;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private RegisterEvent(IForgeRegistry<?> registry) {
		this((RegistryEvent.Register) new RegistryEvent.Register(net.minecraftforge.registries.RegistryManager.ACTIVE.getName((IForgeRegistry) registry), (IForgeRegistry) registry));
	}

	private RegisterEvent(MappedRegistry<?> mapped) {
		this.forge = null;
		this.mapped = mapped;
	}

	/** FML's event bus makes one to find the listeners. */
	public RegisterEvent() {
		this.forge = null;
		this.mapped = null;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T> void register(ResourceKey<? extends Registry<T>> key, Consumer<RegisterHelper<T>> consumer) {
		var registry = BuiltInRegistries.byKey(key);
		if (this.mapped != null) {
			if (registry == this.mapped) {
				MappedRegistry target = this.mapped;
				consumer.accept((id, value) -> {
					if (value == null) {
						nullEntry(id);
					} else if (!target.containsKey(id)) {
						target.register(id, value);
					}
				});
			}
			return;
		}
		if (registry instanceof ForgeRegistryWrapper wrapper && wrapper.forge() == this.forge.getRegistry()) {
			IForgeRegistry forgeRegistry = this.forge.getRegistry();
			consumer.accept((id, value) -> {
				if (value != null) {
					register(forgeRegistry, id, (IForgeRegistryEntry) value);
				} else {
					nullEntry(id);
				}
			});
		} else if (registry instanceof MappedRegistry mapped && this.forge.getRegistry().getRegistrySuperType() == net.minecraft.item.Item.class) {
			// a registry 1.12.2 has no event for (menus, recipe serializers, creative tabs, components): filled once,
			// in the item event like the DeferredRegisters of those registries
			consumer.accept((id, value) -> {
				if (value == null) {
					nullEntry(id);
				} else if (!mapped.containsKey(id)) {
					mapped.register(id, value);
				}
			});
		}
	}

	public <T> void register(ResourceKey<? extends Registry<T>> key, ResourceLocation id, Supplier<T> value) {
		this.register(key, helper -> {
			T made;
			try {
				made = value.get();
			} catch (RuntimeException | LinkageError e) {
				if (!io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(id.getNamespace())) {
					throw e;
				}
				// Registrate calls this per entry and would end its loop on the rethrown error (create:cart_assembler)
				io.github.xponer2.translator.runtime.RuntimeMods.skipped(id, e);
				return;
			}
			helper.register(id, made);
		});
	}

	/** The 1.21 registry this event registers (the 1.12.2 registries: items, blocks, potions, ...). */
	public ResourceKey<? extends Registry<?>> getRegistryKey() {
		if (this.mapped != null) {
			return this.mapped.key();
		}
		var key = BuiltInRegistries.keyOf(this.forge.getRegistry());
		if (key != null) {
			return key;
		}
		// a 1.12.2 registry no 1.21 registry wraps: its 1.21 name (a mod compares it with Registries.X and skips it)
		ResourceLocation name = this.forge.getName();
		String path = LEGACY_NAMES.getOrDefault(name.getPath(), name.getPath());
		return ResourceKey.createRegistryKey(new ResourceLocation(name.getNamespace(), path));
	}

	/** 1.12.2 Forge registry names -> 1.21 registry names. */
	private static final java.util.Map<String, String> LEGACY_NAMES = new java.util.HashMap<>();

	static {
		LEGACY_NAMES.put("blocks", "block");
		LEGACY_NAMES.put("items", "item");
		LEGACY_NAMES.put("potions", "mob_effect");
		LEGACY_NAMES.put("biomes", "worldgen/biome");
		LEGACY_NAMES.put("soundevents", "sound_event");
		LEGACY_NAMES.put("potiontypes", "potion");
		LEGACY_NAMES.put("enchantments", "enchantment");
		LEGACY_NAMES.put("entities", "entity_type");
		LEGACY_NAMES.put("villagerprofessions", "villager_profession");
		LEGACY_NAMES.put("recipes", "recipe");
	}

	/** The registry this event registers. */
	@SuppressWarnings("unchecked")
	public Registry<?> getRegistry() {
		return BuiltInRegistries.byKey((ResourceKey<Registry<Object>>) (ResourceKey<?>) this.getRegistryKey());
	}

	@javax.annotation.Nullable
	@SuppressWarnings("unchecked")
	public <T> Registry<T> getRegistry(ResourceKey<? extends Registry<T>> key) {
		return key.equals(this.getRegistryKey()) ? (Registry<T>) BuiltInRegistries.byKey(key) : null;
	}

	/** An original 1.21.1 mod's entry its code couldn't make (a part the Translator lacks returned null): reported. */
	private static void nullEntry(ResourceLocation id) {
		if (io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(id.getNamespace())) {
			io.github.xponer2.translator.runtime.RuntimeMods.skipped(id, new IllegalStateException("the entry is null"));
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void register(IForgeRegistry registry, ResourceLocation id, IForgeRegistryEntry value) {
		boolean lenient = io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(id.getNamespace());
		if (lenient && value instanceof net.minecraft.item.ItemBlock blockItem && blockItem.getBlock() == null) {
			// its block couldn't be made (a missing 1.21 part): the block item alone would fail at every use
			io.github.xponer2.translator.runtime.RuntimeMods.skipped(id, new IllegalStateException("its block is missing"));
			return;
		}
		try {
			if (value.getRegistryName() == null) {
				value.setRegistryName(id);
			}

			var key = id.getNamespace() + "." + id.getPath();
			if (value instanceof Item item && "item.null".equals(item.getTranslationKey())) {
				item.setTranslationKey(key);
			} else if (value instanceof Block block && "tile.null".equals(block.getTranslationKey())) {
				block.setTranslationKey(key);
			}

			registry.register(value);
			if (value instanceof Item item) {
				io.github.xponer2.translator.client.ItemModels.track(item);
			}
		} catch (RuntimeException | LinkageError e) {
			if (!lenient) {
				throw e;
			}
			// an original 1.21.1 mod registers in one loop: one entry that fails doesn't end it (BoP's block items)
			io.github.xponer2.translator.runtime.RuntimeMods.skipped(id, e);
		}
	}

	@FunctionalInterface
	public interface RegisterHelper<T> {
		void register(ResourceLocation id, T value);

		default void register(ResourceKey<T> key, T value) {
			this.register(key.location(), value);
		}
	}

	public static final class Forwarder {
		/** The loading screen's bar of the registry event going on: a step per DeferredRegister (the mod's id). */
		@javax.annotation.Nullable
		private static io.github.xponer2.translator.util.LoadingBar bar;

		@SubscribeEvent(priority = EventPriority.HIGHEST)
		@SuppressWarnings("rawtypes")
		public void onRegisterStart(RegistryEvent.Register event) {
			int count = DeferredRegister.countFor(event.getRegistry());
			if (count > 0 && bar == null) {
				bar = io.github.xponer2.translator.util.LoadingBar.push("Translator: registering " + event.getName().getPath(), count);
			}
		}

		@SubscribeEvent(priority = EventPriority.LOWEST)
		@SuppressWarnings("rawtypes")
		public void onRegisterEnd(RegistryEvent.Register event) {
			if (bar != null) {
				bar.close();
				bar = null;
			}
			if (event.getRegistry().getRegistrySuperType() == net.minecraft.item.Item.class) {
				BrokenBlockItems.fix(event.getRegistry());
			}
		}

		static void step(String namespace) {
			if (bar != null) {
				bar.step(namespace);
			}
		}

		// raw event type: a parameterized one would only get the events of that type (1.12.2 filters generic events)
		@SuppressWarnings("rawtypes")
		/** The registries posted early, in the block event: their own 1.12.2 events come later and are not posted again. */
		private final java.util.Set<IForgeRegistry<?>> early = new java.util.HashSet<>();

		@SubscribeEvent(priority = EventPriority.NORMAL)
		public void onRegister(RegistryEvent.Register event) {
			if (this.early.contains(event.getRegistry())) {
				return;
			}
			if (event.getRegistry().getRegistrySuperType() == Block.class) {
				// 1.21 fills sounds and mob effects before the blocks (a block's constructor takes its sounds: Framework,
				// Refurbished); 1.12.2 posts them after the items
				for (IForgeRegistry<?> registry : new IForgeRegistry<?>[] { net.minecraftforge.fml.common.registry.ForgeRegistries.SOUND_EVENTS,
						net.minecraftforge.fml.common.registry.ForgeRegistries.POTIONS }) {
					this.early.add(registry);
					try {
						MinecraftForge.EVENT_BUS.post(new RegisterEvent(registry));
					} catch (RuntimeException e) {
						org.apache.logging.log4j.LogManager.getLogger("Translator").warn("Early RegisterEvent for {} failed: {}", registry.getRegistrySuperType().getSimpleName(), e.toString());
					}
				}
			}
			MinecraftForge.EVENT_BUS.post(new RegisterEvent(event));
			if (event.getRegistry().getRegistrySuperType() == Item.class) {
				// the registries 1.12.2 has no event for (the mods' own, block entity types, menus, ...): an event each,
				// as NeoForge posts, then the bake (their callbacks); DeferredRegisters fill them after this (LOWEST)
				for (MappedRegistry<?> registry : BuiltInRegistries.mappedRegistries()) {
					try {
						MinecraftForge.EVENT_BUS.post(new RegisterEvent(registry));
					} catch (RuntimeException e) {
						org.apache.logging.log4j.LogManager.getLogger("Translator").warn("RegisterEvent for {} failed: {}", registry.key().location(), e.toString());
					}
				}
				RegistryCallbacks.bakeAll();
			}
		}

		/** After the DeferredRegisters of those registries were filled (LOWEST): baked again with their entries. */
		@SubscribeEvent(priority = EventPriority.LOWEST)
		public void onRegisterLate(RegistryEvent.Register event) {
			if (event.getRegistry().getRegistrySuperType() == Item.class) {
				RegistryCallbacks.bakeAll();
			}
		}
	}
}
