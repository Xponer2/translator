/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
