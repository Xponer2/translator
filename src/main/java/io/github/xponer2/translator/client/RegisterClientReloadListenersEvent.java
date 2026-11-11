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
package io.github.xponer2.translator.client;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge {@code RegisterClientReloadListenersEvent}: the mods add their client resource listeners (model caches,
 * book and guide loaders, config screens' data) to the game's reloads. It wasn't posted at all: 18 mods' client
 * listeners never ran. Posted in init (ClientSetup); 1.12.2 runs a listener once as it is added, then on every reload.
 * Client only.
 */
@SideOnly(Side.CLIENT)
public class RegisterClientReloadListenersEvent extends Event {
	public RegisterClientReloadListenersEvent() {
	}

	/** NeoForge's {@code new RegisterClientReloadListenersEvent(resourceManager)} (its client hooks): the client's manager is the one. */
	public RegisterClientReloadListenersEvent(Object resourceManager) {
	}

	/** A 1.21 listener (PreparableReloadListener, carried) into the 1.12.2 reloads. */
	public void registerReloadListener(Object listener) {
		if (listener != null) {
			io.github.xponer2.translator.runtime.redirect.ResourceBridges.registerReloadListener(
					(io.github.xponer2.translator.resources.ResourceManager.Client) io.github.xponer2.translator.resources.ResourceManager.client(),
					listener);
		}
	}
}
