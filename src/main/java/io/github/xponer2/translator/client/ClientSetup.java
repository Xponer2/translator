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

import io.github.xponer2.translator.menu.MenuScreens;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client part of the Translator's mod events: a class of its own, so the dedicated server never loads (or verifies
 * a method that names) a client class.
 */
@SideOnly(Side.CLIENT)
public final class ClientSetup {
	private ClientSetup() { }

	public static void preInit() {
		MinecraftForge.EVENT_BUS.register(new ItemTooltipEvent.Forwarder());
		MinecraftForge.EVENT_BUS.register(new ItemModels.Registrar());
		MinecraftForge.EVENT_BUS.register(new ModelEvent.Forwarder());
		io.github.xponer2.translator.client.model.GeometryLoaders.INSTANCE.install();
		MinecraftForge.EVENT_BUS.register(new RegisterColorHandlersEvent.Forwarder());
		MinecraftForge.EVENT_BUS.register(new RegisterClientExtensionsEvent.HandRenderer());
		MinecraftForge.EVENT_BUS.register(new AtlasTextures());
		MinecraftForge.EVENT_BUS.register(new SingleVariantModels());
		MinecraftForge.EVENT_BUS.register(new CopperTextures());
		MinecraftForge.EVENT_BUS.register(new ExtensionsBeforeStitch());
		MinecraftForge.EVENT_BUS.register(new PoseStack.FrameEnd());
		// the signs of the 1.21.1 mods (their block entities): board, wood texture, text
		SignRenderers.register();
	}

	// renderers and client extensions of the mods (subscribed in pre-init)
	public static void init() {
		MinecraftForge.EVENT_BUS.post(new EntityRenderersEvent.RegisterLayerDefinitions());
		MinecraftForge.EVENT_BUS.post(new EntityRenderersEvent.RegisterRenderers());
		MinecraftForge.EVENT_BUS.post(new EntityRenderersEvent.CreateSkullModels());
		MinecraftForge.EVENT_BUS.post(new EntityRenderersEvent.AddLayers());
		postClientExtensions();
	}

	private static boolean extensionsPosted;

	/**
	 * The mods' client extensions, once: before the first atlas stitch (the textures of their fluids go into the atlas
	 * and their fluid buckets bake with them; posted in init they came after both, and a fluid bucket showed water).
	 */
	static synchronized void postClientExtensions() {
		if (!extensionsPosted) {
			extensionsPosted = true;
			MinecraftForge.EVENT_BUS.post(new RegisterClientExtensionsEvent());
		}
	}

	public static final class ExtensionsBeforeStitch {
		@net.minecraftforge.fml.common.eventhandler.SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST)
		public void onStitch(net.minecraftforge.client.event.TextureStitchEvent.Pre event) {
			postClientExtensions();
		}
	}

	// the mods subscribed to RegisterMenuScreensEvent in pre-init or init
	public static void postInit() {
		MenuScreens.registerAll();
	}
}
