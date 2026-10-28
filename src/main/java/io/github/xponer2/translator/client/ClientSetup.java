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
		MinecraftForge.EVENT_BUS.register(new RegisterColorHandlersEvent.Forwarder());
		MinecraftForge.EVENT_BUS.register(new RegisterClientExtensionsEvent.HandRenderer());
		MinecraftForge.EVENT_BUS.register(new AtlasTextures());
		MinecraftForge.EVENT_BUS.register(new SingleVariantModels());
		MinecraftForge.EVENT_BUS.register(new CopperTextures());
	}

	// renderers and client extensions of the mods (subscribed in pre-init)
	public static void init() {
		MinecraftForge.EVENT_BUS.post(new EntityRenderersEvent.RegisterRenderers());
		MinecraftForge.EVENT_BUS.post(new RegisterClientExtensionsEvent());
	}

	// the mods subscribed to RegisterMenuScreensEvent in pre-init or init
	public static void postInit() {
		MenuScreens.registerAll();
	}
}
