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
package io.github.xponer2.translator.runtime;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.xponer2.translator.runtime.neoforge.ModContainer;

import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModMetadata;

/**
 * The 1.21.1 mods in Forge's mod list (the Mods screen, Loader.isModLoaded): one entry per mod the Translator found,
 * started or not; a mod that didn't start says so and why. They are listed only, FML doesn't load or send them.
 */
final class FmlMods {
	private FmlMods() {
	}

	/** Mod id -> why it didn't start (absent: started). */
	static final Map<String, String> FAILED = new LinkedHashMap<>();

	static void add(List<ModContainer> mods) {
		try {
			Loader loader = Loader.instance();
			Field modsField = Loader.class.getDeclaredField("mods");
			Field namedField = Loader.class.getDeclaredField("namedMods");
			modsField.setAccessible(true);
			namedField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var list = new ArrayList<>((List<net.minecraftforge.fml.common.ModContainer>) modsField.get(loader));
			@SuppressWarnings("unchecked")
			var named = new LinkedHashMap<>((Map<String, net.minecraftforge.fml.common.ModContainer>) namedField.get(loader));
			int fmlActive = loader.getActiveModList().size();
			var added = new ArrayList<net.minecraftforge.fml.common.ModContainer>();
			for (ModContainer mod : mods) {
				if (named.containsKey(mod.getModId())) {
					continue;
				}
				// FML opens every active mod's file (recipes, tags, resources): the mod's jar
				java.io.File jar = mod.getModInfo().getOwningFile().getFile().getFilePath().toFile();
				var container = new DummyModContainer(metadata(mod)) {
					@Override
					public java.io.File getSource() {
						return jar;
					}
				};
				list.add(container);
				named.put(mod.getModId(), container);
				added.add(container);
			}
			modsField.set(loader, ImmutableList.copyOf(list));
			namedField.set(loader, ImmutableMap.copyOf(named));
			active(loader, added);
			branding(fmlActive + (int) mods.stream().filter(mod -> !FAILED.containsKey(mod.getModId())).count());
		} catch (ReflectiveOperationException | RuntimeException e) {
			RuntimeMods.LOGGER.warn("Can't add the 1.21.1 mods to the mod list: {}", e.toString());
		}
	}

	/**
	 * The Mods screen lists FML's active mods: the 1.21.1 mods join that list, each with an empty event channel (FML
	 * sends the server lifecycle events to every active mod; theirs get the NeoForge events from the Translator).
	 */
	private static void active(Loader loader, List<net.minecraftforge.fml.common.ModContainer> added) throws ReflectiveOperationException {
		Field controllerField = Loader.class.getDeclaredField("modController");
		controllerField.setAccessible(true);
		Object controller = controllerField.get(loader);
		Field activeField = controller.getClass().getDeclaredField("activeModList");
		Field channelsField = controller.getClass().getDeclaredField("eventChannels");
		activeField.setAccessible(true);
		channelsField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var active = new ArrayList<>((List<net.minecraftforge.fml.common.ModContainer>) activeField.get(controller));
		@SuppressWarnings("unchecked")
		var channels = new LinkedHashMap<>((Map<String, com.google.common.eventbus.EventBus>) channelsField.get(controller));
		for (var container : added) {
			active.add(container);
			channels.putIfAbsent(container.getModId(), new com.google.common.eventbus.EventBus(container.getModId()));
		}
		activeField.set(controller, ImmutableList.copyOf(active));
		channelsField.set(controller, ImmutableMap.copyOf(channels));
	}

	/**
	 * The main menu's "N mods loaded, M mods active": FML counts only its own mods as active; the started 1.21.1 mods
	 * are counted too (only the text: FML's active list sends its lifecycle events, they aren't part of it).
	 */
	private static void branding(int active) throws ReflectiveOperationException {
		var handler = net.minecraftforge.fml.common.FMLCommonHandler.instance();
		Field brandings = handler.getClass().getDeclaredField("brandings");
		Field noMinecraft = handler.getClass().getDeclaredField("brandingsNoMC");
		brandings.setAccessible(true);
		noMinecraft.setAccessible(true);
		brandings.set(handler, null);
		handler.computeBranding();
		@SuppressWarnings("unchecked")
		var lines = new ArrayList<>((List<String>) brandings.get(handler));
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains(" mods active") || lines.get(i).contains(" mod active")) {
				lines.set(i, lines.get(i).replaceAll("\\d+ mods? active", active + (active == 1 ? " mod" : " mods") + " active"));
			}
		}
		var result = ImmutableList.copyOf(lines);
		brandings.set(handler, result);
		noMinecraft.set(handler, result.subList(1, result.size()));
	}

	private static ModMetadata metadata(ModContainer mod) {
		var meta = new ModMetadata();
		meta.modId = mod.getModId();
		String failure = FAILED.get(mod.getModId());
		meta.name = mod.getDisplayName() + (failure == null ? "" : " (not started)");
		meta.version = mod.getVersion();
		String description = mod.getModInfo().getDescription();
		meta.description = (description == null ? "" : description.trim() + "\n\n") + "NeoForge 1.21.1 mod, run by the Translator."
				+ (failure == null ? "" : "\nNot started: " + failure);
		meta.credits = "";
		meta.logoFile = "";
		meta.url = "";
		meta.autogenerated = false;
		return meta;
	}
}
