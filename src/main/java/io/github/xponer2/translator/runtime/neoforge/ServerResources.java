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
package io.github.xponer2.translator.runtime.neoforge;

/**
 * 1.21 {@code ReloadableServerResources}: what the server's data reload gives the mods (its registries, its recipes).
 * {@link AddReloadListenerEvent#getServerResources()}: Moonlight makes its listeners with its registry lookup (null, and
 * its handler threw inside the event: the listeners of every mod after it were lost).
 */
public final class ServerResources {
	public static final ServerResources INSTANCE = new ServerResources();

	private ServerResources() {
	}

	public io.github.xponer2.translator.registry.HolderLookup.Provider getRegistryLookup() {
		return io.github.xponer2.translator.registry.RegistryAccess.INSTANCE;
	}

	public io.github.xponer2.translator.recipe.RecipeManager getRecipeManager() {
		return io.github.xponer2.translator.recipe.RecipeManager.get();
	}
}
