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
package io.github.xponer2.translator.world;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * The 1.21 structure templates ({@code data/<ns>/structure/<path>.nbt} of the mods' jars and of Minecraft 1.21.1), as
 * the carried 1.21 StructureTemplate: what 1.21's StructureTemplateManager gives the mods' structure pieces and template
 * pools ({@code manager.getOrCreate(id)}). The manager the mods hold is 1.12.2's TemplateManager (a handle); 1.12.2's
 * own templates (assets/<ns>/structures) are another thing. A template's blocks are read by their 1.21 names
 * ({@link BlockStateNbt}). Loaded once per id; a missing one is an empty template, as on 1.21.
 */
public final class StructureTemplates {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	private static final Map<ResourceLocation, Object> LOADED = new ConcurrentHashMap<>();
	private static final Object MISSING = new Object();

	private StructureTemplates() {
	}

	/** 1.21 {@code getOrCreate(id)}: the template, an empty one when there is none. */
	public static StructureTemplate getOrCreate(ResourceLocation id) {
		return get(id).orElseGet(StructureTemplates::empty);
	}

	/** 1.21 {@code get(id)}: the template, empty when there is none. */
	public static Optional<StructureTemplate> get(ResourceLocation id) {
		Object template = LOADED.computeIfAbsent(id, StructureTemplates::load);
		return template == MISSING ? Optional.empty() : Optional.of((StructureTemplate) template);
	}

	/** 1.21 {@code readStructure(tag)}: a template from its saved form. */
	public static StructureTemplate read(NBTTagCompound tag) {
		StructureTemplate template = empty();
		fixPalette(tag);
		try {
			template.getClass().getMethod("load", io.github.xponer2.translator.registry.Registry.class, NBTTagCompound.class).invoke(template, null, tag);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			LOGGER.warn("Couldn't read a 1.21 structure template: {}", e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e);
		}
		return template;
	}

	private static Object load(ResourceLocation id) {
		var resources = io.github.xponer2.translator.resources.ServerResourceManager.get();
		// 1.21's folder, then the one of before 1.21 (templates a mod still keeps there)
		var resource = resources.getResource(new ResourceLocation(id.getNamespace(), "structure/" + id.getPath() + ".nbt"));
		if (!resource.isPresent()) {
			resource = resources.getResource(new ResourceLocation(id.getNamespace(), "structures/" + id.getPath() + ".nbt"));
		}
		if (!resource.isPresent()) {
			LOGGER.warn("No 1.21 structure template {}", id);
			return MISSING;
		}
		try (InputStream in = resource.get().open()) {
			return read(CompressedStreamTools.readCompressed(in));
		} catch (java.io.IOException | RuntimeException e) {
			LOGGER.warn("Couldn't read the 1.21 structure template {}: {}", id, e.toString());
			return MISSING;
		}
	}

	private static StructureTemplate empty() {
		try {
			return (StructureTemplate) Class.forName("net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate", true, Launch.classLoader)
					.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("No carried 1.21 StructureTemplate", e);
		}
	}

	/**
	 * Names a template saved before 1.21 has for blocks 1.21 renamed (1.21's DataFixer updates them when it loads one;
	 * 1.12.2 has none for 1.13+ data): short grass was {@code minecraft:grass} up to 1.20.2, and is 1.12.2's grass block.
	 */
	private static void fixPalette(NBTTagCompound tag) {
		int version = tag.hasKey("DataVersion", 99) ? tag.getInteger("DataVersion") : 0;
		if (version >= 3698) {
			return;
		}
		fix(tag.getTagList("palette", 10));
		NBTTagList palettes = tag.getTagList("palettes", 9);
		for (int i = 0; i < palettes.tagCount(); i++) {
			var one = palettes.get(i);
			if (one instanceof NBTTagList list) {
				fix(list);
			}
		}
	}

	private static void fix(NBTTagList palette) {
		for (int i = 0; i < palette.tagCount(); i++) {
			NBTTagCompound entry = palette.getCompoundTagAt(i);
			String name = entry.getString("Name");
			if ("minecraft:grass".equals(name)) {
				entry.setString("Name", "minecraft:short_grass");
			}
		}
	}

	/**
	 * The 1.21 jigsaw block (the carried JigsawBlock, as {@code minecraft:jigsaw}): 1.12.2 has none, so the jigsaw blocks of
	 * the templates of the mods' template pools (Iron's Spells' towers, villages of the mods) read as air, no piece found
	 * where to join the next, and {@code Blocks.JIGSAW} was a stand-in nothing matched. Never placed in the world: the
	 * pools replace it by its final state.
	 */
	public static final class Jigsaw {
		@net.minecraftforge.fml.common.eventhandler.SubscribeEvent
		public void onBlocks(net.minecraftforge.event.RegistryEvent.Register<net.minecraft.block.Block> event) {
			ResourceLocation id = new ResourceLocation("minecraft", "jigsaw");
			if (event.getRegistry().containsKey(id)) {
				return;
			}
			try {
				Class<?> type = Class.forName("net.minecraft.world.level.block.JigsawBlock", true, Launch.classLoader);
				var block = (net.minecraft.block.Block) type.getConstructor(io.github.xponer2.translator.block.BlockProperties.class)
						.newInstance(io.github.xponer2.translator.block.BlockProperties.of().noLootTable());
				block.setRegistryName(id);
				event.getRegistry().register(block);
				// read before the blocks were registered (a mod's class initializer): the stand-in, set to the block now
				var field = io.github.xponer2.translator.runtime.redirect.FlattenedBlocks.class.getDeclaredField("JIGSAW");
				if (field.get(null) != block) {
					var modifiers = java.lang.reflect.Field.class.getDeclaredField("modifiers");
					modifiers.setAccessible(true);
					modifiers.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
					field.setAccessible(true);
					field.set(null, block);
				}
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				LOGGER.warn("No 1.21 jigsaw block: the mods' template pools won't join their pieces ({})", e.toString());
			}
		}
	}
}
