/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import com.electronwill.nightconfig.core.CommentedConfig;

import net.neoforged.fml.config.ModConfig;

/**
 * FML 4's (NeoForge 21's) loaded config of a mod config on FML 3 (NeoForge 20.6), which keeps the config's data in the
 * ModConfig itself. (Its own class: the loaded config's interface is carried down only for the mods that use it; named
 * by its binary name, the outer IConfigSpec is the loader's own below.)
 */
public final class ConfigAdapters {
    private ConfigAdapters() {
    }

    /** 1.21's {@code config.getLoadedConfig()}: its data and saving (null while the config isn't loaded, as on 1.21) */
    public static ladder.carried_1_20_6.net.neoforged.fml.config.IConfigSpec$ILoadedConfig getLoadedConfig(ModConfig config) {
        CommentedConfig data = config.getConfigData();
        if (data == null) {
            return null;
        }
        return new ladder.carried_1_20_6.net.neoforged.fml.config.IConfigSpec$ILoadedConfig() {
            @Override
            public CommentedConfig config() {
                return data;
            }

            @Override
            public void save() {
                config.save();
            }
        };
    }
}
