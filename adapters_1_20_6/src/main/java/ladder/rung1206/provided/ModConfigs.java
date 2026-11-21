/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import java.util.List;
import java.util.Set;

import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;

/** FML 4's {@code ModConfigs} (a P row): the registered configs, from FML 3's config tracker. */
public final class ModConfigs {
    private ModConfigs() {
    }

    public static Set<ModConfig> getConfigSet(ModConfig.Type type) {
        return ConfigTracker.INSTANCE.configSets().get(type);
    }

    public static List<ModConfig> getModConfigs(String modId) {
        return ConfigTracker.INSTANCE.fileMap().values().stream().filter(c -> c.getModId().equals(modId)).distinct().toList();
    }

    public static List<String> getConfigFileNames(String modId, ModConfig.Type type) {
        return getModConfigs(modId).stream().filter(c -> c.getType() == type).map(ModConfig::getFileName).toList();
    }
}
