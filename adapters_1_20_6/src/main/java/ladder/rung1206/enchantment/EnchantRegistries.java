/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.enchantment;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The registries 1.21 added for its data-driven enchantments (effect component types, entity / location / value effect
 * types, level based value types, provider types), made on 1.20.6 with their 1.21 ids (synced), and the keys of the data
 * registries {@code enchantment_provider} and {@code advancement}. The F rows give them to the mods; the 1.21 classes
 * that register into them come down as carried code.
 */
public final class EnchantRegistries {
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_EFFECT_COMPONENT_TYPE_KEY = ResourceKey.createRegistryKey(new ResourceLocation("enchantment_effect_component_type"));
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_ENTITY_EFFECT_TYPE_KEY = ResourceKey.createRegistryKey(new ResourceLocation("enchantment_entity_effect_type"));
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_LEVEL_BASED_VALUE_TYPE_KEY = ResourceKey.createRegistryKey(new ResourceLocation("enchantment_level_based_value_type"));
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE_KEY = ResourceKey.createRegistryKey(new ResourceLocation("enchantment_location_based_effect_type"));
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_PROVIDER_TYPE_KEY = ResourceKey.createRegistryKey(new ResourceLocation("enchantment_provider_type"));
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_VALUE_EFFECT_TYPE_KEY = ResourceKey.createRegistryKey(new ResourceLocation("enchantment_value_effect_type"));
    public static final ResourceKey<Registry<Object>> ENCHANTMENT_PROVIDER_KEY = ResourceKey.createRegistryKey(new ResourceLocation("enchantment_provider"));
    public static final ResourceKey<Registry<Object>> ADVANCEMENT_KEY = ResourceKey.createRegistryKey(new ResourceLocation("advancement"));

    static Registry<Object> ENCHANTMENT_EFFECT_COMPONENT_TYPE;
    static Registry<Object> ENCHANTMENT_ENTITY_EFFECT_TYPE;
    static Registry<Object> ENCHANTMENT_LEVEL_BASED_VALUE_TYPE;
    static Registry<Object> ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE;
    static Registry<Object> ENCHANTMENT_PROVIDER_TYPE;
    static Registry<Object> ENCHANTMENT_VALUE_EFFECT_TYPE;

    private EnchantRegistries() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EnchantRegistries::create);
        modBus.addListener(EnchantRegistries::bootstrap);
    }

    /** a registry's 1.21 entries: the bootstrap of its carried 1.21 class (the B rows), when a mod brought it down */
    private static final java.util.Map<ResourceKey<?>, String> BOOTSTRAPS = java.util.Map.of(
            ENCHANTMENT_LEVEL_BASED_VALUE_TYPE_KEY, "net.minecraft.world.item.enchantment.LevelBasedValue",
            ENCHANTMENT_ENTITY_EFFECT_TYPE_KEY, "net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect",
            ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE_KEY, "net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect",
            ENCHANTMENT_VALUE_EFFECT_TYPE_KEY, "net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect",
            ENCHANTMENT_PROVIDER_TYPE_KEY, "net.minecraft.world.item.enchantment.providers.EnchantmentProviderTypes",
            ENCHANTMENT_EFFECT_COMPONENT_TYPE_KEY, "net.minecraft.world.item.enchantment.EnchantmentEffectComponents");

    private static void bootstrap(net.neoforged.neoforge.registries.RegisterEvent event) {
        String upper = BOOTSTRAPS.get(event.getRegistryKey());
        if (upper == null || event.getRegistry() == null) {
            return;
        }
        Class<?> carried;
        try {
            carried = Class.forName("ladder.carried_1_20_6." + upper, true, EnchantRegistries.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            // no converted mod uses it: nothing to register
            return;
        }
        for (java.lang.reflect.Method m : carried.getDeclaredMethods()) {
            if (m.getName().equals("bootstrap") && m.getParameterCount() == 1 && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                try {
                    m.setAccessible(true);
                    m.invoke(null, event.getRegistry());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("the 1.21 entries of " + event.getRegistryKey().location(), e);
                }
                return;
            }
        }
    }

    private static void create(NewRegistryEvent event) {
        ENCHANTMENT_EFFECT_COMPONENT_TYPE = event.create(new RegistryBuilder<>(ENCHANTMENT_EFFECT_COMPONENT_TYPE_KEY).sync(true));
        ENCHANTMENT_ENTITY_EFFECT_TYPE = event.create(new RegistryBuilder<>(ENCHANTMENT_ENTITY_EFFECT_TYPE_KEY).sync(true));
        ENCHANTMENT_LEVEL_BASED_VALUE_TYPE = event.create(new RegistryBuilder<>(ENCHANTMENT_LEVEL_BASED_VALUE_TYPE_KEY).sync(true));
        ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE = event.create(new RegistryBuilder<>(ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE_KEY).sync(true));
        ENCHANTMENT_PROVIDER_TYPE = event.create(new RegistryBuilder<>(ENCHANTMENT_PROVIDER_TYPE_KEY).sync(true));
        ENCHANTMENT_VALUE_EFFECT_TYPE = event.create(new RegistryBuilder<>(ENCHANTMENT_VALUE_EFFECT_TYPE_KEY).sync(true));
    }

    /** {@code Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE} (an F row) */
    public static ResourceKey<Registry<Object>> enchantmentEffectComponentTypeKey() {
        return ENCHANTMENT_EFFECT_COMPONENT_TYPE_KEY;
    }

    /** {@code BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE} (an F row) */
    public static Registry<Object> enchantmentEffectComponentType() {
        return ENCHANTMENT_EFFECT_COMPONENT_TYPE;
    }

    /** {@code Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE} (an F row) */
    public static ResourceKey<Registry<Object>> enchantmentEntityEffectTypeKey() {
        return ENCHANTMENT_ENTITY_EFFECT_TYPE_KEY;
    }

    /** {@code BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE} (an F row) */
    public static Registry<Object> enchantmentEntityEffectType() {
        return ENCHANTMENT_ENTITY_EFFECT_TYPE;
    }

    /** {@code Registries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE} (an F row) */
    public static ResourceKey<Registry<Object>> enchantmentLevelBasedValueTypeKey() {
        return ENCHANTMENT_LEVEL_BASED_VALUE_TYPE_KEY;
    }

    /** {@code BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE} (an F row) */
    public static Registry<Object> enchantmentLevelBasedValueType() {
        return ENCHANTMENT_LEVEL_BASED_VALUE_TYPE;
    }

    /** {@code Registries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE} (an F row) */
    public static ResourceKey<Registry<Object>> enchantmentLocationBasedEffectTypeKey() {
        return ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE_KEY;
    }

    /** {@code BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE} (an F row) */
    public static Registry<Object> enchantmentLocationBasedEffectType() {
        return ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE;
    }

    /** {@code Registries.ENCHANTMENT_PROVIDER_TYPE} (an F row) */
    public static ResourceKey<Registry<Object>> enchantmentProviderTypeKey() {
        return ENCHANTMENT_PROVIDER_TYPE_KEY;
    }

    /** {@code BuiltInRegistries.ENCHANTMENT_PROVIDER_TYPE} (an F row) */
    public static Registry<Object> enchantmentProviderType() {
        return ENCHANTMENT_PROVIDER_TYPE;
    }

    /** {@code Registries.ENCHANTMENT_VALUE_EFFECT_TYPE} (an F row) */
    public static ResourceKey<Registry<Object>> enchantmentValueEffectTypeKey() {
        return ENCHANTMENT_VALUE_EFFECT_TYPE_KEY;
    }

    /** {@code BuiltInRegistries.ENCHANTMENT_VALUE_EFFECT_TYPE} (an F row) */
    public static Registry<Object> enchantmentValueEffectType() {
        return ENCHANTMENT_VALUE_EFFECT_TYPE;
    }

    public static ResourceKey<Registry<Object>> enchantmentProviderKey() {
        return ENCHANTMENT_PROVIDER_KEY;
    }

    public static ResourceKey<Registry<Object>> advancementKey() {
        return ADVANCEMENT_KEY;
    }
}
