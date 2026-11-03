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

import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.util.Registries;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21 {@code Enchantments}: keys of the vanilla enchantments (1.12.2's class has the enchantments themselves). A
 * GETSTATIC of a key that 1.12.2's Enchantments has with another type reads here.
 */
public final class EnchantmentKeys {
	public static final ResourceKey<Enchantment> PROTECTION = key("protection");
	public static final ResourceKey<Enchantment> FIRE_PROTECTION = key("fire_protection");
	public static final ResourceKey<Enchantment> FEATHER_FALLING = key("feather_falling");
	public static final ResourceKey<Enchantment> BLAST_PROTECTION = key("blast_protection");
	public static final ResourceKey<Enchantment> PROJECTILE_PROTECTION = key("projectile_protection");
	public static final ResourceKey<Enchantment> RESPIRATION = key("respiration");
	public static final ResourceKey<Enchantment> AQUA_AFFINITY = key("aqua_affinity");
	public static final ResourceKey<Enchantment> THORNS = key("thorns");
	public static final ResourceKey<Enchantment> DEPTH_STRIDER = key("depth_strider");
	public static final ResourceKey<Enchantment> FROST_WALKER = key("frost_walker");
	public static final ResourceKey<Enchantment> BINDING_CURSE = key("binding_curse");
	public static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");
	public static final ResourceKey<Enchantment> SMITE = key("smite");
	public static final ResourceKey<Enchantment> BANE_OF_ARTHROPODS = key("bane_of_arthropods");
	public static final ResourceKey<Enchantment> KNOCKBACK = key("knockback");
	public static final ResourceKey<Enchantment> FIRE_ASPECT = key("fire_aspect");
	public static final ResourceKey<Enchantment> LOOTING = key("looting");
	public static final ResourceKey<Enchantment> SWEEPING_EDGE = key("sweeping");
	public static final ResourceKey<Enchantment> EFFICIENCY = key("efficiency");
	public static final ResourceKey<Enchantment> SILK_TOUCH = key("silk_touch");
	public static final ResourceKey<Enchantment> UNBREAKING = key("unbreaking");
	public static final ResourceKey<Enchantment> FORTUNE = key("fortune");
	public static final ResourceKey<Enchantment> POWER = key("power");
	public static final ResourceKey<Enchantment> PUNCH = key("punch");
	public static final ResourceKey<Enchantment> FLAME = key("flame");
	public static final ResourceKey<Enchantment> INFINITY = key("infinity");
	public static final ResourceKey<Enchantment> LUCK_OF_THE_SEA = key("luck_of_the_sea");
	public static final ResourceKey<Enchantment> LURE = key("lure");
	public static final ResourceKey<Enchantment> MENDING = key("mending");
	public static final ResourceKey<Enchantment> VANISHING_CURSE = key("vanishing_curse");

	private EnchantmentKeys() {
	}

	private static ResourceKey<Enchantment> key(String name) {
		return ResourceKey.create(Registries.ENCHANTMENT, new ResourceLocation("minecraft", name));
	}
}
