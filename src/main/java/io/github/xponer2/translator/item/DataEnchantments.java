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
package io.github.xponer2.translator.item;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.xponer2.translator.Translator;
import io.github.xponer2.translator.tags.TagManager;
import io.github.xponer2.translator.tags.ItemTags;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.21 data driven enchantments ({@code data/<namespace>/enchantment/*.json} of the ported mods) as 1.12.2
 * enchantments, registered in the enchantment registry event. Read: level range, costs, weight, slots and the item
 * tags. The effects aren't (the mods built on them have handlers instead); files with effects get a warning.
 */
public final class DataEnchantments {
	private static final Pattern FILE = Pattern.compile("([a-z0-9_.-]+)/enchantment/([a-z0-9_./-]+)\\.json");

	@SubscribeEvent
	public void onRegister(RegistryEvent.Register<Enchantment> event) {
		for (var mod : Loader.instance().getActiveModList()) {
			if (!Translator.isPortedMod(mod.getModId())) {
				continue;
			}

			CraftingHelper.findFiles(mod, "data", null, (root, file) -> {
				var m = FILE.matcher(root.relativize(file).toString().replace('\\', '/'));
				if (m.matches()) {
					var id = new ResourceLocation(m.group(1), m.group(2));
					// registered as the mod of the file (no registry name prefix warnings)
					var active = Loader.instance().activeModContainer();
					Loader.instance().setActiveModContainer(mod);
					try {
						var json = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
						event.getRegistry().register(new DataEnchantment(id, json).setRegistryName(id));
					} catch (IOException | RuntimeException e) {
						Translator.LOGGER.error("Couldn't load the enchantment {}", id, e);
					} finally {
						Loader.instance().setActiveModContainer(active);
					}
				}
				return true;
			}, true, true);
		}
	}

	private static final class DataEnchantment extends Enchantment {
		private final int maxLevel;
		private final int minBase, minPerLevel, maxBase, maxPerLevel;
		private final ResourceLocation supported;
		private final ResourceLocation primary;

		DataEnchantment(ResourceLocation id, JsonObject json) {
			super(rarity(json.get("weight").getAsInt()), type(tag(json, "primary_items", "supported_items")), slots(json));
			this.maxLevel = json.get("max_level").getAsInt();
			var min = json.getAsJsonObject("min_cost");
			var max = json.getAsJsonObject("max_cost");
			this.minBase = min.get("base").getAsInt();
			this.minPerLevel = min.get("per_level_above_first").getAsInt();
			this.maxBase = max.get("base").getAsInt();
			this.maxPerLevel = max.get("per_level_above_first").getAsInt();
			this.supported = tag(json, "supported_items", "supported_items");
			this.primary = tag(json, "primary_items", "supported_items");
			// the 1.21 key is the description, 1.12.2 adds "enchantment." to the name
			var key = json.getAsJsonObject("description").get("translate").getAsString();
			this.setName(key.startsWith("enchantment.") ? key.substring("enchantment.".length()) : key);
			if (json.has("effects")) {
				Translator.LOGGER.warn("The effects of the enchantment {} aren't supported", id);
			}
		}

		@Override
		public int getMaxLevel() {
			return this.maxLevel;
		}

		@Override
		public int getMinEnchantability(int level) {
			return this.minBase + this.minPerLevel * (level - 1);
		}

		@Override
		public int getMaxEnchantability(int level) {
			return this.maxBase + this.maxPerLevel * (level - 1);
		}

		@Override
		public boolean canApply(ItemStack stack) {
			return TagManager.is(stack, ItemTags.create(this.supported));
		}

		@Override
		public boolean canApplyAtEnchantingTable(ItemStack stack) {
			return TagManager.is(stack, ItemTags.create(this.primary));
		}

		private static ResourceLocation tag(JsonObject json, String key, String fallback) {
			JsonElement value = json.has(key) ? json.get(key) : json.get(fallback);
			var text = value.getAsString();
			return io.github.xponer2.translator.util.ResourceLocations.parse(text.startsWith("#") ? text.substring(1) : text);
		}

		// the 1.12.2 rarities have the weights 10, 5, 2 and 1
		private static Rarity rarity(int weight) {
			return weight >= 10 ? Rarity.COMMON : weight >= 5 ? Rarity.UNCOMMON : weight >= 2 ? Rarity.RARE : Rarity.VERY_RARE;
		}

		// the type only sorts the books into the creative tabs here: the item checks use the tags
		private static EnumEnchantmentType type(ResourceLocation tag) {
			return switch (tag.toString()) {
				case "minecraft:enchantable/sword", "minecraft:enchantable/sharp_weapon", "minecraft:enchantable/weapon" -> EnumEnchantmentType.WEAPON;
				case "minecraft:enchantable/mining", "minecraft:enchantable/mining_loot" -> EnumEnchantmentType.DIGGER;
				case "minecraft:enchantable/bow" -> EnumEnchantmentType.BOW;
				case "minecraft:enchantable/fishing" -> EnumEnchantmentType.FISHING_ROD;
				case "minecraft:enchantable/armor", "minecraft:enchantable/equippable" -> EnumEnchantmentType.ARMOR;
				case "minecraft:enchantable/head_armor" -> EnumEnchantmentType.ARMOR_HEAD;
				case "minecraft:enchantable/chest_armor" -> EnumEnchantmentType.ARMOR_CHEST;
				case "minecraft:enchantable/leg_armor" -> EnumEnchantmentType.ARMOR_LEGS;
				case "minecraft:enchantable/foot_armor" -> EnumEnchantmentType.ARMOR_FEET;
				case "minecraft:enchantable/durability" -> EnumEnchantmentType.BREAKABLE;
				default -> EnumEnchantmentType.ALL;
			};
		}

		private static EntityEquipmentSlot[] slots(JsonObject json) {
			var slots = new ArrayList<EntityEquipmentSlot>();
			for (var element : json.getAsJsonArray("slots")) {
				switch (element.getAsString()) {
					case "mainhand" -> slots.add(EntityEquipmentSlot.MAINHAND);
					case "offhand" -> slots.add(EntityEquipmentSlot.OFFHAND);
					case "hand" -> {
						slots.add(EntityEquipmentSlot.MAINHAND);
						slots.add(EntityEquipmentSlot.OFFHAND);
					}
					case "head" -> slots.add(EntityEquipmentSlot.HEAD);
					case "chest", "body" -> slots.add(EntityEquipmentSlot.CHEST);
					case "legs" -> slots.add(EntityEquipmentSlot.LEGS);
					case "feet" -> slots.add(EntityEquipmentSlot.FEET);
					case "armor" -> {
						slots.add(EntityEquipmentSlot.HEAD);
						slots.add(EntityEquipmentSlot.CHEST);
						slots.add(EntityEquipmentSlot.LEGS);
						slots.add(EntityEquipmentSlot.FEET);
					}
					default -> {
						// "any"
						for (var slot : EntityEquipmentSlot.values()) {
							slots.add(slot);
						}
					}
				}
			}
			return slots.stream().distinct().toArray(EntityEquipmentSlot[]::new);
		}
	}
}
