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
package io.github.xponer2.translator.runtime.redirect;

import java.io.InputStream;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The 1.21 vanilla items 1.12.2 doesn't have (Items.BUNDLE, the copper items, ...), as plain items of their 1.21
 * names, in no creative tab: {@link FlattenedItems} finds them, so a mod's list of vanilla items has their ids and a
 * stack of one saves and loads (null, they made empty stacks and crashed whatever asked for their id).
 */
public final class VanillaPlaceholders {
	private static final java.util.Map<String, Item> PLACEHOLDERS = new java.util.concurrent.ConcurrentHashMap<>();

	/** The placeholder item of a 1.21 name, made on first use (before the items are registered) and registered as itself. */
	static Item placeholder(String name) {
		return PLACEHOLDERS.computeIfAbsent(name, key -> {
			var id = new ResourceLocation(key);
			return new Item().setRegistryName(id).setTranslationKey(id.getNamespace() + "." + id.getPath());
		});
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onItems(RegistryEvent.Register<Item> event) {
		var fields = flattenedFields();
		int added = 0;
		for (String name : new java.util.LinkedHashSet<>(fields.values())) {
			var id = new ResourceLocation(name);
			if (Item.REGISTRY.containsKey(id) || event.getRegistry().containsKey(id)) {
				continue;
			}
			event.getRegistry().register(placeholder(name));
			added++;
		}
		// FlattenedItems may have been read already (a mod's class initializer): its fields of these were null
		int filled = 0;
		for (var entry : fields.entrySet()) {
			try {
				var field = FlattenedItems.class.getDeclaredField(entry.getKey());
				if (field.get(null) == null) {
					Item item = Item.getByNameOrId(entry.getValue());
					if (item != null) {
						setStatic(field, item);
						filled++;
					}
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				org.apache.logging.log4j.LogManager.getLogger("Translator").warn("Can't set FlattenedItems.{}: {}", entry.getKey(), e.toString());
			}
		}
		org.apache.logging.log4j.LogManager.getLogger("Translator").info("{} 1.21 vanilla items 1.12.2 lacks added as plain items ({} filled in late)",
				added, filled);
	}

	private static void setStatic(java.lang.reflect.Field field, Object value) throws ReflectiveOperationException {
		field.setAccessible(true);
		var modifiers = java.lang.reflect.Field.class.getDeclaredField("modifiers");
		modifiers.setAccessible(true);
		modifiers.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
		field.set(null, value);
	}

	/**
	 * FlattenedItems' fields and the item names they look up, read from its bytecode (initializing it first would look
	 * them up before these items exist).
	 */
	private static java.util.Map<String, String> flattenedFields() {
		var fields = new java.util.LinkedHashMap<String, String>();
		try (InputStream in = VanillaPlaceholders.class.getResourceAsStream("FlattenedItems.class")) {
			var node = new xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode();
			new xyz.wagyourtail.jvmdg.shade.asm.ClassReader(in).accept(node, 0);
			for (var method : node.methods) {
				if (!"<clinit>".equals(method.name)) {
					continue;
				}
				String name = null;
				for (var insn : method.instructions) {
					if (insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode ldc && ldc.cst instanceof String text
							&& text.startsWith("minecraft:")) {
						name = text;
					} else if (insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.FieldInsnNode put && name != null
							&& put.getOpcode() == xyz.wagyourtail.jvmdg.shade.asm.Opcodes.PUTSTATIC) {
						fields.put(put.name, name);
						name = null;
					}
				}
			}
		} catch (Exception e) {
			org.apache.logging.log4j.LogManager.getLogger("Translator").warn("Can't read the vanilla item names: {}", e.toString());
		}
		return fields;
	}
}
