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
    /** The registry's event has registered the entries: later ones would never be registered (NeoForge throws then). */
    private boolean seenRegisterEvent;

    // public: the shadow of NeoForge's DeferredRegister (another package) makes them (createItems, createBlocks)
    public DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
        ALL.add(this);
    }

    /** Every DeferredRegister made: how many a registry event goes through (the loading screen's bar). */
    private static final java.util.List<DeferredRegister<?>> ALL = Collections.synchronizedList(new java.util.ArrayList<>());

    static int countFor(IForgeRegistry<?> forge) {
        int count = 0;
        synchronized (ALL) {
            for (DeferredRegister<?> register : ALL) {
                if (register.registry() instanceof ForgeRegistryWrapper<?> wrapper && wrapper.forge() == forge) {
                    count++;
                }
            }
        }
        return count;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new DeferredRegister<>(registryKey, namespace);
    }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace) {
        return new DeferredRegister<>(registry.key(), namespace);
    }

    /** NeoForge {@code DeferredRegister.createDataComponents}. */
    public static DataComponents createDataComponents(String namespace) {
        return new DataComponents(io.github.xponer2.translator.util.Registries.DATA_COMPONENT_TYPE, namespace);
    }

    public static DataComponents createDataComponents(ResourceKey<? extends Registry<io.github.xponer2.translator.component.DataComponentType<?>>> key,
            String namespace) {
        return new DataComponents(key, namespace);
    }

    /** NeoForge {@code DeferredRegister.DataComponents}: data component types made from a builder. */
    public static class DataComponents extends DeferredRegister<io.github.xponer2.translator.component.DataComponentType<?>> {
        protected DataComponents(ResourceKey<? extends Registry<io.github.xponer2.translator.component.DataComponentType<?>>> key, String namespace) {
            super(key, namespace);
        }

        public <D> DeferredHolder<io.github.xponer2.translator.component.DataComponentType<?>, io.github.xponer2.translator.component.DataComponentType<D>> registerComponentType(
                String name, java.util.function.UnaryOperator<io.github.xponer2.translator.component.DataComponentType.Builder<D>> builder) {
            return this.register(name, () -> builder.apply(io.github.xponer2.translator.component.DataComponentType.builder()).build());
        }
    }

    /** An old id of an entry (worlds saved with it load the entry): 1.12.2 remaps missing ids itself, nothing kept. */
    public void addAlias(ResourceLocation from, ResourceLocation to) {
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier) {
        return this.register(name, id -> supplier.get());
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Function<ResourceLocation, ? extends I> factory) {
        // like NeoForge: entries may be added after register(bus) (Twilight Forest's block items, made when TFBlocks
        // loads), until the registry event has run
        if (this.seenRegisterEvent && !(this.registry() instanceof MappedRegistry)) {
            throw new IllegalStateException("Cannot register new entries to " + this.registryKey.location() + " after its registry event has run");
        }
        var location = new ResourceLocation(this.namespace, name);
        DeferredHolder<T, I> holder = this.createHolder(this.registryKey, location);
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
     * NeoForge {@code createHolder}: the holder of a new entry. NeoForge's DeferredRegister.Items and Blocks (carried
     * from 1.21.1) override it to make DeferredItem / DeferredBlock holders.
     */
    protected <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation key) {
        return DeferredHolder.create(registryKey, key);
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

    /** The same for an original 1.21.1 mod (runtime): its mod event bus. */
    public void register(io.github.xponer2.translator.runtime.neoforge.IEventBus modEventBus) {
        this.register((Object) modEventBus);
    }

    /**
     * NeoForge {@code makeRegistry(consumer)}: the mod's own registry of this register's key, made with the builder now
     * (a library that keeps its own registries: Moonlight's villager trade types; without it the registry was null and
     * the mods registering into it failed while they were constructed: Supplementaries).
     */
    public Registry<T> makeRegistry(java.util.function.Consumer<RegistryBuilder<T>> consumer) {
        var builder = new RegistryBuilder<T>(this.registryKey);
        if (consumer != null) {
            consumer.accept(builder);
        }
        return builder.create();
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

    /** null for a register without a registry key (a key from a class the Translator can't load yet): it registers nothing. */
    @javax.annotation.Nullable
    private Registry<T> registry() {
        return this.registryKey == null ? null : BuiltInRegistries.byKey(this.registryKey);
    }

    // raw event type: a parameterized one would only get the events of that type (1.12.2 filters generic events)
    @SuppressWarnings({"rawtypes", "unchecked"})
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRegister(RegistryEvent.Register event) {
        var registry = this.registry();
        if (registry instanceof EntityTypeRegistry) {
            // a mod's entity types: each a Forge entity registration (EntityType.register), bound to the mod's type. 1.21
            // registers them before the items (a spawn egg's constructor takes its type), 1.12.2 after: in the block event
            Class<?> type = event.getRegistry().getRegistrySuperType();
            if (!this.registeredEarly && (type == Block.class || type == net.minecraftforge.fml.common.registry.EntityEntry.class)) {
                this.registeredEarly = true;
                this.registerEntityTypes(net.minecraftforge.fml.common.registry.ForgeRegistries.ENTITIES);
            }
            return;
        }
        if (!(registry instanceof ForgeRegistryWrapper wrapper) || this.registeredEarly) {
            return;
        }
        if (wrapper.forge() == event.getRegistry()) {
            this.registerAll((IForgeRegistry) event.getRegistry());
        } else if (event.getRegistry().getRegistrySuperType() == Block.class && isBeforeBlocks(wrapper.forge())) {
            // 1.21 fills these before the blocks (a block's constructor takes its sounds), 1.12.2 after the items
            this.registeredEarly = true;
            this.registerAll(wrapper.forge());
        }
    }

    private boolean registeredEarly;

    private static boolean isBeforeBlocks(IForgeRegistry<?> registry) {
        Class<?> type = registry.getRegistrySuperType();
        return type == net.minecraft.util.SoundEvent.class || type == net.minecraft.potion.Potion.class;
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
        this.seenRegisterEvent = true;
        RegisterEvent.Forwarder.step(this.namespace);
        boolean lenient = io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(this.namespace);
        for (var entry : this.entries.entrySet()) {
            var holder = (DeferredHolder) entry.getKey();
            IForgeRegistryEntry value;
            try {
                value = (IForgeRegistryEntry) entry.getValue().get();
            } catch (RuntimeException | LinkageError e) {
                if (!lenient) {
                    throw e;
                }
                // an original 1.21.1 mod (runtime): one entry the Translator can't make doesn't stop the game
                io.github.xponer2.translator.runtime.RuntimeMods.skipped(holder.getId(), e);
                continue;
            }
            var location = holder.getId();
            if (value == null && lenient) {
                // its supplier can't run on 1.12.2 yet (a stubbed constructor of the mod returns nothing)
                io.github.xponer2.translator.runtime.RuntimeMods.skipped(location, new IllegalStateException("the entry is null"));
                continue;
            }
            try {
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
            } catch (RuntimeException | LinkageError e) {
                if (!lenient) {
                    throw e;
                }
                io.github.xponer2.translator.runtime.RuntimeMods.skipped(location, e);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerEntityTypes(IForgeRegistry registry) {
        this.seenRegisterEvent = true;
        RegisterEvent.Forwarder.step(this.namespace);
        boolean lenient = io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(this.namespace);
        for (var entry : this.entries.entrySet()) {
            var holder = (DeferredHolder) entry.getKey();
            try {
                Object value = entry.getValue().get();
                if (!(value instanceof EntityType<?> type)) {
                    io.github.xponer2.translator.runtime.RuntimeMods.skipped(holder.getId(), new IllegalStateException("the entry is null"));
                    continue;
                }
                var forgeEntry = EntityType.register(holder.getId(), type);
                if (forgeEntry != null) {
                    registry.register(forgeEntry);
                }
                holder.bind(type);
            } catch (RuntimeException | LinkageError e) {
                if (!lenient) {
                    throw e;
                }
                io.github.xponer2.translator.runtime.RuntimeMods.skipped(holder.getId(), e);
            }
        }
    }

    private <I extends T> void resolve(MappedRegistry<T> registry, DeferredHolder<T, I> holder) {
        if (holder.isBound()) {
            return;
        }
        I value;
        try {
            @SuppressWarnings("unchecked")
            var made = (I) this.entries.get(holder).get();
            value = made;
        } catch (RuntimeException | LinkageError e) {
            if (!io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(this.namespace)) {
                throw e;
            }
            io.github.xponer2.translator.runtime.RuntimeMods.skipped(holder.getId(), e);
            return;
        }
        try {
            registry.register(holder.getId(), value);
        } catch (RuntimeException e) {
            // an original 1.21.1 mod's entry the registry refuses (null, or the same object as another entry because
            // what made it fell back to a shared default): skipped, the game goes on
            if (!io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(this.namespace)) {
                throw e;
            }
            io.github.xponer2.translator.runtime.RuntimeMods.skipped(holder.getId(), e);
            return;
        }
        holder.bind(value);
        // its callback once the holder is bound (a block entity's constructor reads the holder of its type)
        RegistrationCallback callback = registry.takePending(holder.getId());
        if (callback != null) {
            MappedRegistry.runCallback(holder.getId(), callback);
        }
    }
}
