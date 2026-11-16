/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package ladder.adapter.fml;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

/** FML 4's {@code ModContainer.registerConfig} on FML 2 (1.20.4): the container's config added as FML 2 makes it. */
public final class FmlAdapters {
    private FmlAdapters() {
    }

    public static void registerConfig(ModContainer container, ModConfig.Type type, IConfigSpec<?> spec) {
        container.addConfig(new ModConfig(type, spec, container));
    }

    public static void registerConfig(ModContainer container, ModConfig.Type type, IConfigSpec<?> spec, String fileName) {
        container.addConfig(new ModConfig(type, spec, container, fileName));
    }
}
