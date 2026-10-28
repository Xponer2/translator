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
	private final RegistryEvent.Register<?> forge;

	public RegisterEvent(RegistryEvent.Register<?> forge) {
		this.forge = forge;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T> void register(ResourceKey<? extends Registry<T>> key, Consumer<RegisterHelper<T>> consumer) {
		var registry = BuiltInRegistries.byKey(key);
		if (registry instanceof ForgeRegistryWrapper wrapper && wrapper.forge() == this.forge.getRegistry()) {
			IForgeRegistry forgeRegistry = this.forge.getRegistry();
			consumer.accept((id, value) -> register(forgeRegistry, id, (IForgeRegistryEntry) value));
		}
	}

	public <T> void register(ResourceKey<? extends Registry<T>> key, ResourceLocation id, Supplier<T> value) {
		this.register(key, helper -> helper.register(id, value.get()));
	}

	public ResourceKey<? extends Registry<?>> getRegistryKey() {
		throw new UnsupportedOperationException("1.12.2 registry events have no registry key");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void register(IForgeRegistry registry, ResourceLocation id, IForgeRegistryEntry value) {
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
	}

	@FunctionalInterface
	public interface RegisterHelper<T> {
		void register(ResourceLocation id, T value);

		default void register(ResourceKey<T> key, T value) {
			this.register(key.location(), value);
		}
	}

	public static final class Forwarder {
		// raw event type: a parameterized one would only get the events of that type (1.12.2 filters generic events)
		@SuppressWarnings("rawtypes")
		@SubscribeEvent(priority = EventPriority.NORMAL)
		public void onRegister(RegistryEvent.Register event) {
			MinecraftForge.EVENT_BUS.post(new RegisterEvent(event));
		}
	}
}
