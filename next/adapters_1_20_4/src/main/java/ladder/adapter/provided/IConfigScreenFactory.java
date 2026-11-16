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
