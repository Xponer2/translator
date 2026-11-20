/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.fml;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * FML 4's config entry points on FML 2's: a config is made and tracked by ModConfig's own constructor there, and the mod
 * container keeps it (what 1.21's ConfigTracker.registerConfig does in one call).
 */
public final class ConfigAdapters {
    private ConfigAdapters() {
    }

    /** ConfigTracker.registerConfig(type, spec, container, fileName) */
    public static ModConfig registerConfig(ConfigTracker tracker, ModConfig.Type type, IConfigSpec<?> spec, ModContainer container, String fileName) {
        ModConfig config = new ModConfig(type, spec, container, fileName);
        container.addConfig(config);
        return config;
    }

    /** ConfigTracker.registerConfig(type, spec, container): the file named by the mod and the type, as both versions name it */
    public static ModConfig registerConfig(ConfigTracker tracker, ModConfig.Type type, IConfigSpec<?> spec, ModContainer container) {
        ModConfig config = new ModConfig(type, spec, container);
        container.addConfig(config);
        return config;
    }

    /** ConfigTracker.acceptSyncedConfig(config, bytes): the server's values of a synced config */
    public static void acceptSyncedConfig(ConfigTracker tracker, ModConfig config, byte[] bytes) {
        config.acceptSyncedConfig(bytes);
    }

    /** the static form of ConfigTracker.acceptSyncedConfig(config, bytes) */
    public static void acceptSyncedConfig(ModConfig config, byte[] bytes) {
        config.acceptSyncedConfig(bytes);
    }

    /** ModLoader.hasErrors() */
    public static boolean hasErrors() {
        return !ModLoader.isLoadingStateValid();
    }

    /**
     * ModConfigSpec.Builder.defineListAllowEmpty(path, default, newElement, validator): the new element supplier is for
     * 1.21's config screen (a row added to the list); FML 2's builder keeps the rest.
     */
    public static <T> ModConfigSpec.ConfigValue<List<? extends T>> defineListAllowEmpty(ModConfigSpec.Builder builder, String path,
            Supplier<List<? extends T>> defaultValue, Supplier<T> newElement, Predicate<Object> validator) {
        return builder.defineListAllowEmpty(path, defaultValue, validator);
    }
}
