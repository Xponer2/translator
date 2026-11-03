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
package io.github.xponer2.translator;

import io.github.xponer2.translator.capabilities.CapabilityAttacher;
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
		return portedMods.contains(modId) || io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(modId);
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		ModList.setScanData(event.getAsmData());
		// more than 4096 blocks: before any registry event
		io.github.xponer2.translator.world.BlockIds.raiseLimit();
		if (!net.minecraftforge.fml.common.Loader.isModLoaded("jeid") && !net.minecraftforge.fml.common.Loader.isModLoaded("reid")) {
			MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.world.BlockIds.Saving());
		}
		io.github.xponer2.translator.content.Copper.readConfig(new net.minecraftforge.common.config.Configuration(event.getSuggestedConfigurationFile()));
		MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.content.Copper.Registrar());
		MinecraftForge.EVENT_BUS.register(new RegisterEvent.Forwarder());
		MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.runtime.redirect.VanillaPlaceholders());
		MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.runtime.ChunkUnloadGuard());
		Menus.init();
		MinecraftForge.EVENT_BUS.register(new CapabilityAttacher());
		MinecraftForge.EVENT_BUS.register(new io.github.xponer2.translator.item.DataEnchantments());
		if (event.getSide().isClient()) {
			io.github.xponer2.translator.client.ClientSetup.preInit();
		}
		// original NeoForge 1.21.1 jars in mods/: converted and started now, before the registry events
		io.github.xponer2.translator.runtime.RuntimeMods.load(event.getModConfigurationDirectory().getParentFile());
	}

	private static boolean setUp;

	/**
	 * Payload handlers, capabilities, the registries baked, configs, then NeoForge's setup events. NeoForge sets the
	 * mods up before the models load (Patchouli's model event waits until its common setup has read the books: on 1.12.2,
	 * where the models load between pre-init and init, the game hung): on the client this runs with the 1.12.2 model
	 * registry event at the end of pre-init, on a server at init.
	 */
	public static synchronized void setupMods() {
		if (setUp) {
			return;
		}
		setUp = true;
		// the mods registered their payload and capability handlers on the event bus in pre-init
		RegisterPayloadHandlersEvent.fire();
		CapabilityAttacher.registerAll();
		// every registry filled (DeferredRegisters too): baked once more, as NeoForge does after registration
		io.github.xponer2.translator.registry.RegistryCallbacks.bakeAll();
		io.github.xponer2.translator.runtime.neoforge.ConfigLoader.loadServerConfigs();
		io.github.xponer2.translator.runtime.RuntimeMods.setup();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		setupMods();
		if (event.getSide().isClient()) {
			io.github.xponer2.translator.client.ClientSetup.init();
		}
	}

	@Mod.EventHandler
	public void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
		event.registerServerCommand(new io.github.xponer2.translator.selftest.SelfTest.Command());
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		io.github.xponer2.translator.runtime.RuntimeMods.complete();
		// whose copper minecraft:copper_ingot is, then the tags and recipes of the 1.21 data (every mod has registered
		// its items and OreDictionary entries)
		try (var bar = io.github.xponer2.translator.util.LoadingBar.push("Translator: copper, tags and recipes", 2)) {
			bar.step("copper");
			io.github.xponer2.translator.content.Copper.setup();
			bar.step("tags and recipes of the 1.21 data");
			RecipeLoader.load();
		}
		if (event.getSide().isClient()) {
			io.github.xponer2.translator.client.ClientSetup.postInit();
			// -Dtranslator.autotest=<world>: the self-test on its own, then the game quits
			io.github.xponer2.translator.selftest.AutoTest.init();
		}
	}
}
