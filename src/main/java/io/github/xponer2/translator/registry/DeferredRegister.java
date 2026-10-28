package io.github.xponer2.translator.registry;

import io.github.xponer2.translator.util.BuiltInRegistries;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers the objects of a mod in one registry (1.21 NeoForge {@code DeferredRegister}).
 * <ul>
 * <li>Registries 1.12.2 has (items, blocks, ...): the objects are created and registered in Forge's
 * {@code RegistryEvent.Register} of that registry, like on 1.21. Items and blocks get the translation key
 * {@code <namespace>.<path>} (lang files: {@code item.<namespace>.<path>.name}, {@code tile.<namespace>.<path>.name}).</li>
 * <li>The other registries ({@link MappedRegistry}): an object is created on its first use, the rest after the
 * items were registered (block entity types need their blocks).</li>
 * </ul>
 * {@link #register(Object)} must be called while the mod is constructed or pre-initialized.
 */
public class DeferredRegister<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;
    private final Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private boolean registered;

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new DeferredRegister<>(registryKey, namespace);
    }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace) {
        return new DeferredRegister<>(registry.key(), namespace);
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier) {
        return this.register(name, id -> supplier.get());
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Function<ResourceLocation, ? extends I> factory) {
        if (this.registered && !(this.registry() instanceof MappedRegistry)) {
            throw new IllegalStateException("Cannot register new entries to " + this.registryKey.location() + " after register() was called");
        }
        var location = new ResourceLocation(this.namespace, name);
        DeferredHolder<T, I> holder = DeferredHolder.create(this.registryKey, location);
        if (this.entries.putIfAbsent(holder, () -> factory.apply(location)) != null) {
            throw new IllegalArgumentException("Duplicate registration " + location);
        }
        if (this.registry() instanceof MappedRegistry) {
            var mapped = (MappedRegistry<T>) this.registry();
            holder.setResolver(() -> this.resolve(mapped, holder));
        }
        return holder;
    }

    /**
     * Hooks the registration into the 1.12.2 registry events. The argument is the 1.21 mod event bus, it is ignored.
     */
    public void register(Object modEventBus) {
        if (this.registered) {
            throw new IllegalStateException("Already registered: " + this.registryKey.location() + " of " + this.namespace);
        }
        this.registered = true;
        MinecraftForge.EVENT_BUS.register(this);
    }

    public Collection<DeferredHolder<T, ? extends T>> getEntries() {
        return Collections.unmodifiableSet(this.entries.keySet());
    }

    public ResourceKey<? extends Registry<T>> getRegistryKey() {
        return this.registryKey;
    }

    public String getNamespace() {
        return this.namespace;
    }

    private Registry<T> registry() {
        return BuiltInRegistries.byKey(this.registryKey);
    }

    // raw event type: a parameterized one would only get the events of that type (1.12.2 filters generic events)
    @SuppressWarnings({"rawtypes", "unchecked"})
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRegister(RegistryEvent.Register event) {
        var registry = this.registry();
        if (registry instanceof ForgeRegistryWrapper wrapper && wrapper.forge() == event.getRegistry()) {
            this.registerAll((IForgeRegistry) event.getRegistry());
        }
    }

    // the entries of mapped registries not used yet, once all items exist
    @SuppressWarnings({"rawtypes", "unchecked"})
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRegisterLate(RegistryEvent.Register event) {
        if (this.registry() instanceof MappedRegistry mapped && event.getRegistry().getRegistrySuperType() == Item.class) {
            for (var holder : this.entries.keySet()) {
                this.resolve(mapped, (DeferredHolder) holder);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerAll(IForgeRegistry registry) {
        for (var entry : this.entries.entrySet()) {
            var holder = (DeferredHolder) entry.getKey();
            var value = (IForgeRegistryEntry) entry.getValue().get();
            var location = holder.getId();
            if (value.getRegistryName() == null) {
                value.setRegistryName(location);
            }
            var key = location.getNamespace() + "." + location.getPath();
            if (value instanceof Item item && "item.null".equals(item.getTranslationKey())) {
                item.setTranslationKey(key);
            } else if (value instanceof Block block && "tile.null".equals(block.getTranslationKey())) {
                block.setTranslationKey(key);
            }
            registry.register(value);
            if (value instanceof Item item) {
                io.github.xponer2.translator.client.ItemModels.track(item);
            }
            holder.bind(value);
        }
    }

    private <I extends T> void resolve(MappedRegistry<T> registry, DeferredHolder<T, I> holder) {
        if (holder.isBound()) {
            return;
        }
        @SuppressWarnings("unchecked")
        var value = (I) this.entries.get(holder).get();
        registry.register(holder.getId(), value);
        holder.bind(value);
        if (value instanceof RegistrationCallback callback) {
            callback.onRegistered(holder.getId());
        }
    }
}
