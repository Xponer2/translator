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

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * The NeoForge 1.21 server lifecycle events (about to start, starting, started, stopping, stopped), posted from the
 * 1.12.2 FML server events. Mods load their world and server configs there (MrCrayfish's Framework: Refurbished
 * Furniture's block entities read them when they are made).
 */
public final class ServerEvents {
	private ServerEvents() {
	}

	/** Posts the event on the game bus; a mod's handler that fails is logged, the server goes on starting. */
	public static void post(Event event) {
		try {
			MinecraftForge.EVENT_BUS.post(event);
		} catch (RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(event.getClass().getSimpleName(), e);
		}
	}

	public abstract static class ServerLifecycleEvent extends Event {
		private final MinecraftServer server;

		protected ServerLifecycleEvent(MinecraftServer server) {
			this.server = server;
		}

		public MinecraftServer getServer() {
			return this.server;
		}
	}

	public static class ServerAboutToStartEvent extends ServerLifecycleEvent {
		public ServerAboutToStartEvent(MinecraftServer server) {
			super(server);
		}
	}

	public static class ServerStartingEvent extends ServerLifecycleEvent {
		public ServerStartingEvent(MinecraftServer server) {
			super(server);
		}
	}

	public static class ServerStartedEvent extends ServerLifecycleEvent {
		public ServerStartedEvent(MinecraftServer server) {
			super(server);
		}
	}

	public static class ServerStoppingEvent extends ServerLifecycleEvent {
		public ServerStoppingEvent(MinecraftServer server) {
			super(server);
		}
	}

	public static class ServerStoppedEvent extends ServerLifecycleEvent {
		public ServerStoppedEvent(MinecraftServer server) {
			super(server);
		}
	}
}
