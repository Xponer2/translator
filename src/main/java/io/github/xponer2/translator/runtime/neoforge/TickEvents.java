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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The NeoForge 1.21 tick events (ServerTickEvent, ClientTickEvent, PlayerTickEvent, LevelTickEvent, each with Pre
 * and Post), posted from the 1.12.2 Forge tick events (phase START / END).
 */
public final class TickEvents {
	private TickEvents() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new Forwarder());
	}

	public static final class Forwarder {
		@SubscribeEvent
		public void onServer(TickEvent.ServerTickEvent event) {
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
			MinecraftForge.EVENT_BUS.post(event.phase == TickEvent.Phase.START ? new ServerTickEvent.Pre(server) : new ServerTickEvent.Post(server));
		}

		@SubscribeEvent
		public void onClient(TickEvent.ClientTickEvent event) {
			MinecraftForge.EVENT_BUS.post(event.phase == TickEvent.Phase.START ? new ClientTickEvent.Pre() : new ClientTickEvent.Post());
		}

		@SubscribeEvent
		public void onPlayer(TickEvent.PlayerTickEvent event) {
			MinecraftForge.EVENT_BUS.post(event.phase == TickEvent.Phase.START ? new PlayerTickEvent.Pre(event.player) : new PlayerTickEvent.Post(event.player));
		}

		@SubscribeEvent
		public void onLevel(TickEvent.WorldTickEvent event) {
			MinecraftForge.EVENT_BUS.post(event.phase == TickEvent.Phase.START ? new LevelTickEvent.Pre(event.world) : new LevelTickEvent.Post(event.world));
		}
	}

	public abstract static class ServerTickEvent extends Event {
		private final MinecraftServer server;

		protected ServerTickEvent(MinecraftServer server) {
			this.server = server;
		}

		public MinecraftServer getServer() {
			return this.server;
		}

		public boolean hasTime() {
			return true;
		}

		public static class Pre extends ServerTickEvent {
			public Pre(MinecraftServer server) {
				super(server);
			}
		}

		public static class Post extends ServerTickEvent {
			public Post(MinecraftServer server) {
				super(server);
			}
		}
	}

	public abstract static class ClientTickEvent extends Event {
		public static class Pre extends ClientTickEvent {
		}

		public static class Post extends ClientTickEvent {
		}
	}

	public abstract static class PlayerTickEvent extends Event {
		private final EntityPlayer player;

		protected PlayerTickEvent(EntityPlayer player) {
			this.player = player;
		}

		public EntityPlayer getEntity() {
			return this.player;
		}

		public static class Pre extends PlayerTickEvent {
			public Pre(EntityPlayer player) {
				super(player);
			}
		}

		public static class Post extends PlayerTickEvent {
			public Post(EntityPlayer player) {
				super(player);
			}
		}
	}

	public abstract static class LevelTickEvent extends Event {
		private final World level;

		protected LevelTickEvent(World level) {
			this.level = level;
		}

		public World getLevel() {
			return this.level;
		}

		public boolean hasTime() {
			return true;
		}

		public static class Pre extends LevelTickEvent {
			public Pre(World level) {
				super(level);
			}
		}

		public static class Post extends LevelTickEvent {
			public Post(World level) {
				super(level);
			}
		}
	}
}
