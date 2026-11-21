/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;

/**
 * NeoForge 21's {@code IConfigScreenFactory} (a mod's config screen from the mod list, made for its container); 20.6's
 * takes the Minecraft. Registered through {@code ModContainer.registerExtensionPoint}, it becomes 20.6's
 * ({@link ladder.rung1206.fml.FmlAdapters}).
 */
@FunctionalInterface
public interface IConfigScreenFactory extends IExtensionPoint {
    Screen createScreen(ModContainer container, Screen modListScreen);
}
