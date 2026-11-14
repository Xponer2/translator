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

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge 1.21 client events for original 1.21.1 mods: key mappings (posted at the Translator's client setup),
 * particle providers (accepted; a mod's own particles are not drawn yet) and key input (from the 1.12.2 input
 * event, with GLFW key codes).
 */
@SideOnly(Side.CLIENT)
public final class ClientEvents {
	private ClientEvents() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new Forwarder());
		io.github.xponer2.translator.client.ScreenEvents.register();
	}

	public static void setup() {
		MinecraftForge.EVENT_BUS.post(new RegisterKeyMappingsEvent());
		KeyMapping.registerLeftovers();
		postParticleProviders();
	}

	private static boolean particleProvidersPosted;

	/**
	 * The mods' particle providers, once: before the first atlas stitch (their sprite sets' textures go into the blocks
	 * atlas) or at setup, whichever comes first.
	 */
	public static synchronized void postParticleProviders() {
		if (!particleProvidersPosted) {
			particleProvidersPosted = true;
			MinecraftForge.EVENT_BUS.post(new RegisterParticleProvidersEvent());
		}
	}

	public static final class Forwarder {
		@SubscribeEvent
		public void onKey(net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent event) {
			int key = Keyboard.getEventKey();
			int action = Keyboard.getEventKeyState() ? (Keyboard.isRepeatEvent() ? 2 : 1) : 0;
			MinecraftForge.EVENT_BUS.post(new Key(KeyMapping.toGlfw(key), action));
		}

		/** The connection the mods were told about (LoggingIn posted for it), null when none. */
		@javax.annotation.Nullable
		private Object loggedIn;

		/**
		 * NeoForge's {@code ClientPlayerNetworkEvent.LoggingIn} and {@code RecipesUpdatedEvent} when the client player
		 * first joins a world of a connection: JEI starts on these two (it never started: no item list, no recipes), and
		 * other mods set up their client state on login. Posted once per connection (a respawn or a dimension change
		 * makes a new player that joins again).
		 */
		@SubscribeEvent
		public void onJoin(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
			if (!event.getWorld().isRemote || event.getEntity() != mc.player || mc.getConnection() == null) {
				return;
			}
			Object connection = mc.getConnection().getNetworkManager();
			if (connection == this.loggedIn) {
				return;
			}
			this.loggedIn = connection;
			post("net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent$LoggingIn", mc.playerController, mc.player, connection);
			post("net.neoforged.neoforge.client.event.RecipesUpdatedEvent", io.github.xponer2.translator.recipe.RecipeManager.get());
		}

		/** NeoForge's {@code ClientPlayerNetworkEvent.LoggingOut} when the client leaves the server. */
		@SubscribeEvent
		public void onLeave(net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
			Object connection = this.loggedIn;
			if (connection == null) {
				return;
			}
			this.loggedIn = null;
			mc.addScheduledTask(() -> post("net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent$LoggingOut", mc.playerController, mc.player,
					connection));
		}

		/** The Controls screen counts every key category: one without a registered key leaves an empty line it crashes on. */
		@SubscribeEvent
		public void onGuiOpen(net.minecraftforge.client.event.GuiOpenEvent event) {
			if (event.getGui() instanceof net.minecraft.client.gui.GuiControls) {
				KeyMapping.pruneCategories();
			}
		}
	}

	/** A carried NeoForge event made with the constructor that takes these arguments (nulls allowed), posted; failures logged once. */
	private static void post(String eventClass, Object... args) {
		try {
			Class<?> type = Class.forName(eventClass, true, net.minecraft.launchwrapper.Launch.classLoader);
			for (java.lang.reflect.Constructor<?> constructor : type.getConstructors()) {
				Class<?>[] params = constructor.getParameterTypes();
				if (params.length != args.length) {
					continue;
				}
				boolean fits = true;
				for (int i = 0; i < params.length && fits; i++) {
					fits = args[i] == null ? !params[i].isPrimitive() : params[i].isInstance(args[i]);
				}
				if (fits) {
					MinecraftForge.EVENT_BUS.post((Event) constructor.newInstance(args));
					return;
				}
			}
			io.github.xponer2.translator.runtime.RuntimeStubs.missing("the client event " + eventClass + " (no constructor for its arguments)");
		} catch (java.lang.reflect.InvocationTargetException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the client event " + eventClass, e.getCause());
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the client event " + eventClass, e);
		}
	}

	public static class RegisterKeyMappingsEvent extends Event {
		public void register(KeyBinding key) {
			ClientRegistry.registerKeyBinding(key);
		}
	}

	/** NeoForge's particle providers of the mods' particle types (drawn by {@link io.github.xponer2.translator.client.particle.ParticleProviders}). */
	public static class RegisterParticleProvidersEvent extends Event {
		/** A provider made with the type's sprite set (a SpriteParticleRegistration). */
		public void registerSpriteSet(Object type, Object registration) {
			io.github.xponer2.translator.client.particle.ParticleProviders.registerSpriteSet(type, registration);
		}

		/** A provider of particles without sprites (or with their own). */
		public void registerSpecial(Object type, Object provider) {
			io.github.xponer2.translator.client.particle.ParticleProviders.registerSpecial(type, provider);
		}

		/** A provider of one-sprite particles: the sprite picked from the type's set. */
		public void registerSprite(Object type, Object provider) {
			io.github.xponer2.translator.client.particle.ParticleProviders.registerSprite(type, provider);
		}
	}

	/** NeoForge {@code InputEvent.Key}: a key pressed (action 1), repeated (2) or released (0), GLFW codes. */
	public static class Key extends Event {
		private final int key;
		private final int action;

		public Key(int key, int action) {
			this.key = key;
			this.action = action;
		}

		public int getKey() {
			return this.key;
		}

		public int getScanCode() {
			return 0;
		}

		public int getAction() {
			return this.action;
		}

		public int getModifiers() {
			return 0;
		}
	}
}
