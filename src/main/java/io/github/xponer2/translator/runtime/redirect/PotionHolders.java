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

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.util.Registries;

import net.minecraft.init.PotionTypes;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21 {@code Potions}: holders of the vanilla potions 1.12.2 has (its PotionTypes has the potions themselves, under
 * the same names). A GETSTATIC of a 1.21 potion reads here (9 mods: brewing recipes, potion items, tipped arrows). The
 * 1.21-only potions (turtle master, slow falling, wind charged, ...) stay missing.
 */
public final class PotionHolders {
	public static final Holder<PotionType> EMPTY = holder(PotionTypes.EMPTY);
	public static final Holder<PotionType> WATER = holder(PotionTypes.WATER);
	public static final Holder<PotionType> MUNDANE = holder(PotionTypes.MUNDANE);
	public static final Holder<PotionType> THICK = holder(PotionTypes.THICK);
	public static final Holder<PotionType> AWKWARD = holder(PotionTypes.AWKWARD);
	public static final Holder<PotionType> NIGHT_VISION = holder(PotionTypes.NIGHT_VISION);
	public static final Holder<PotionType> LONG_NIGHT_VISION = holder(PotionTypes.LONG_NIGHT_VISION);
	public static final Holder<PotionType> INVISIBILITY = holder(PotionTypes.INVISIBILITY);
	public static final Holder<PotionType> LONG_INVISIBILITY = holder(PotionTypes.LONG_INVISIBILITY);
	public static final Holder<PotionType> LEAPING = holder(PotionTypes.LEAPING);
	public static final Holder<PotionType> LONG_LEAPING = holder(PotionTypes.LONG_LEAPING);
	public static final Holder<PotionType> STRONG_LEAPING = holder(PotionTypes.STRONG_LEAPING);
	public static final Holder<PotionType> FIRE_RESISTANCE = holder(PotionTypes.FIRE_RESISTANCE);
	public static final Holder<PotionType> LONG_FIRE_RESISTANCE = holder(PotionTypes.LONG_FIRE_RESISTANCE);
	public static final Holder<PotionType> SWIFTNESS = holder(PotionTypes.SWIFTNESS);
	public static final Holder<PotionType> LONG_SWIFTNESS = holder(PotionTypes.LONG_SWIFTNESS);
	public static final Holder<PotionType> STRONG_SWIFTNESS = holder(PotionTypes.STRONG_SWIFTNESS);
	public static final Holder<PotionType> SLOWNESS = holder(PotionTypes.SLOWNESS);
	public static final Holder<PotionType> LONG_SLOWNESS = holder(PotionTypes.LONG_SLOWNESS);
	public static final Holder<PotionType> WATER_BREATHING = holder(PotionTypes.WATER_BREATHING);
	public static final Holder<PotionType> LONG_WATER_BREATHING = holder(PotionTypes.LONG_WATER_BREATHING);
	public static final Holder<PotionType> HEALING = holder(PotionTypes.HEALING);
	public static final Holder<PotionType> STRONG_HEALING = holder(PotionTypes.STRONG_HEALING);
	public static final Holder<PotionType> HARMING = holder(PotionTypes.HARMING);
	public static final Holder<PotionType> STRONG_HARMING = holder(PotionTypes.STRONG_HARMING);
	public static final Holder<PotionType> POISON = holder(PotionTypes.POISON);
	public static final Holder<PotionType> LONG_POISON = holder(PotionTypes.LONG_POISON);
	public static final Holder<PotionType> STRONG_POISON = holder(PotionTypes.STRONG_POISON);
	public static final Holder<PotionType> REGENERATION = holder(PotionTypes.REGENERATION);
	public static final Holder<PotionType> LONG_REGENERATION = holder(PotionTypes.LONG_REGENERATION);
	public static final Holder<PotionType> STRONG_REGENERATION = holder(PotionTypes.STRONG_REGENERATION);
	public static final Holder<PotionType> STRENGTH = holder(PotionTypes.STRENGTH);
	public static final Holder<PotionType> LONG_STRENGTH = holder(PotionTypes.LONG_STRENGTH);
	public static final Holder<PotionType> STRONG_STRENGTH = holder(PotionTypes.STRONG_STRENGTH);
	public static final Holder<PotionType> WEAKNESS = holder(PotionTypes.WEAKNESS);
	public static final Holder<PotionType> LONG_WEAKNESS = holder(PotionTypes.LONG_WEAKNESS);
	public static final Holder<PotionType> LUCK = holder(PotionType.getPotionTypeForName("luck"));

	private PotionHolders() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Holder<PotionType> holder(PotionType potion) {
		ResourceLocation id = PotionType.REGISTRY.getNameForObject(potion);
		ResourceKey<PotionType> key = id == null ? null : (ResourceKey<PotionType>) (ResourceKey) ResourceKey.create((ResourceKey) Registries.POTION, id);
		return key == null ? Holder.direct(potion) : new Registry.Reference<>(key, potion);
	}
}
