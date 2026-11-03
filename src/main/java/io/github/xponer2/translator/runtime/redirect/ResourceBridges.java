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
package io.github.xponer2.translator.runtime.redirect;

import java.lang.reflect.Method;

import io.github.xponer2.translator.resources.ResourceManager;
import io.github.xponer2.translator.runtime.RuntimeStubs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

/**
 * The client's 1.21 {@code ReloadableResourceManager} (= the Translator's client resource manager, R line):
 * {@code registerReloadListener} hooks a 1.21 listener into the 1.12.2 resource reloads. 1.12.2 runs a new listener
 * once right away, as the 1.21 game runs them at its first resource load (map mods build their HUD there, before a
 * world exists). Client only.
 */
public final class ResourceBridges {
	private ResourceBridges() {
	}

	/** The listener (a 1.21 ResourceManagerReloadListener, carried) as a 1.12.2 one: its onResourceManagerReload(ResourceManager). */
	public static void registerReloadListener(ResourceManager.Client manager, Object listener) {
		Method reload = find(listener.getClass());
		if (reload == null) {
			// a PreparableReloadListener with only the asynchronous reload(...) 1.12.2 has no stages for
			RuntimeStubs.missing("ReloadableResourceManager.registerReloadListener (" + listener.getClass().getName() + ": no onResourceManagerReload)");
			return;
		}
		IResourceManagerReloadListener hook = resources -> {
			try {
				reload.invoke(listener, ResourceManager.client());
			} catch (ReflectiveOperationException | RuntimeException e) {
				RuntimeStubs.failed(listener.getClass().getName() + ".onResourceManagerReload", e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e);
			}
		};
		((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(hook);
	}

	private static Method find(Class<?> type) {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			for (Method method : c.getMethods()) {
				if (method.getName().equals("onResourceManagerReload") && method.getParameterCount() == 1
						&& method.getParameterTypes()[0].isAssignableFrom(ResourceManager.Client.class)) {
					method.setAccessible(true);
					return method;
				}
			}
		}
		return null;
	}
}
