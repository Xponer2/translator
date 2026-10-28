package io.github.xponer2.translator;

import io.github.xponer2.translator.capabilities.CapabilityAttacher;
import io.github.xponer2.translator.client.ItemTooltipEvent;
import io.github.xponer2.translator.menu.MenuScreens;
import io.github.xponer2.translator.menu.Menus;
import io.github.xponer2.translator.network.RegisterPayloadHandlersEvent;
import io.github.xponer2.translator.recipe.RecipeLoader;
import io.github.xponer2.translator.registry.RegisterEvent;
import io.github.xponer2.translator.util.ModList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The 1.21.1 / NeoForge API on 1.12.2 for ported mods. Loads before them (they require it), so its events are posted
 * after the mods subscribed in their pre-init.
 */
@Mod(modid = Translator.MOD_ID, name = TranslatorBuild.MOD_NAME, version = TranslatorBuild.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public final class Translator {
	public static final String MOD_ID = "translator";
	// constant, so the mods built on the Translator can require this version in their @Mod
	public static final String VERSION = TranslatorBuild.VERSION;
	public static final Logger LOGGER = LogManager.getLogger("Translator");
	private static java.util.Set<String> portedMods;

	/** The mods built on the Translator (they require it): their resources follow the 1.21 rules. */
	public static boolean isPortedMod(String modId) {
		if (portedMods == null) {
			var mods = new java.util.HashSet<String>();
			for (var mod : net.minecraftforge.fml.common.Loader.instance().getActiveModList()) {
				for (var version : mod.getRequirements()) {
					if (MOD_ID.equals(version.getLabel())) {
						mods.add(mod.getModId());
					}
				}
			}
			portedMods = mods;
		}
		return portedMods.contains(modId);
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		ModList.setScanData(event.getAsmData());
		MinecraftForge.EVENT_BUS.register(new RegisterEvent.Forwarder());
		Menus.init();
		MinecraftForge.EVENT_BUS.register(new CapabilityAttacher());
		MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.item.DataEnchantments());
		if (event.getSide().isClient()) {
			MinecraftForge.EVENT_BUS.register(new ItemTooltipEvent.Forwarder());
			MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.client.ItemModels.Registrar());
			MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.client.ModelEvent.Forwarder());
			MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.client.RegisterColorHandlersEvent.Forwarder());
			MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.client.RegisterClientExtensionsEvent.HandRenderer());
			MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.client.AtlasTextures());
			MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.client.SingleVariantModels());
		}
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		// the mods registered their payload and capability handlers on the event bus in pre-init
		RegisterPayloadHandlersEvent.fire();
		CapabilityAttacher.registerAll();
		if (event.getSide().isClient()) {
			// renderers and client extensions of the mods (subscribed in pre-init)
			MinecraftForge.EVENT_BUS.post(new io.github.xponer2.translator.client.EntityRenderersEvent.RegisterRenderers());
			MinecraftForge.EVENT_BUS.post(new io.github.xponer2.translator.client.RegisterClientExtensionsEvent());
		}
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		// tags and recipes of the 1.21 data, once every mod has registered its items and OreDictionary entries
		RecipeLoader.load();
		if (event.getSide().isClient()) {
			// the mods subscribed to RegisterMenuScreensEvent in pre-init or init
			MenuScreens.registerAll();
		}
	}
}
