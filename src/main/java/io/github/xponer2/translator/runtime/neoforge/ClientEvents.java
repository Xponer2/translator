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
	}

	public static void setup() {
		MinecraftForge.EVENT_BUS.post(new RegisterKeyMappingsEvent());
		KeyMapping.registerLeftovers();
		MinecraftForge.EVENT_BUS.post(new RegisterParticleProvidersEvent());
	}

	public static final class Forwarder {
		@SubscribeEvent
		public void onKey(net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent event) {
			int key = Keyboard.getEventKey();
			int action = Keyboard.getEventKeyState() ? (Keyboard.isRepeatEvent() ? 2 : 1) : 0;
			MinecraftForge.EVENT_BUS.post(new Key(KeyMapping.toGlfw(key), action));
		}

		/** The Controls screen counts every key category: one without a registered key leaves an empty line it crashes on. */
		@SubscribeEvent
		public void onGuiOpen(net.minecraftforge.client.event.GuiOpenEvent event) {
			if (event.getGui() instanceof net.minecraft.client.gui.GuiControls) {
				KeyMapping.pruneCategories();
			}
		}
	}

	public static class RegisterKeyMappingsEvent extends Event {
		public void register(KeyBinding key) {
			ClientRegistry.registerKeyBinding(key);
		}
	}

	public static class RegisterParticleProvidersEvent extends Event {
		public void registerSpriteSet(Object type, Object provider) {
			ForgeBus.LOGGER.debug("Particle provider for {} not used (mod particles are not drawn yet)", type);
		}

		public void registerSpecial(Object type, Object provider) {
			this.registerSpriteSet(type, provider);
		}

		public void registerSprite(Object type, Object provider) {
			this.registerSpriteSet(type, provider);
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
