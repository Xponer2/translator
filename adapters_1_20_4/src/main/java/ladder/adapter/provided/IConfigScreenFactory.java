/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;

/**
 * NeoForge 20.5's {@code IConfigScreenFactory} (a mod's config screen from the mod list). Registered through
 * {@code ModContainer.registerExtensionPoint}, it becomes 1.20.4's {@code ConfigScreenHandler.ConfigScreenFactory}
 * ({@link ladder.adapter.fml.FmlAdapters}).
 */
@FunctionalInterface
@SuppressWarnings("rawtypes")
public interface IConfigScreenFactory extends IExtensionPoint {
    Screen createScreen(ModContainer container, Screen modListScreen);
}
