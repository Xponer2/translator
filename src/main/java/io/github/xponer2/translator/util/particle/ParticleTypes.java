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
package io.github.xponer2.translator.util.particle;

import net.minecraft.util.EnumParticleTypes;

/**
 * 1.21 particle types that 1.12.2 has (nearest 1.12.2 particle where the name changed).
 */
public final class ParticleTypes {
	public static final ParticleOptions SMOKE = new LegacyParticleOptions(EnumParticleTypes.SMOKE_NORMAL);
	public static final ParticleOptions LARGE_SMOKE = new LegacyParticleOptions(EnumParticleTypes.SMOKE_LARGE);
	public static final ParticleOptions FLAME = new LegacyParticleOptions(EnumParticleTypes.FLAME);
	public static final ParticleOptions SOUL_FIRE_FLAME = new LegacyParticleOptions(EnumParticleTypes.FLAME);
	public static final ParticleOptions HAPPY_VILLAGER = new LegacyParticleOptions(EnumParticleTypes.VILLAGER_HAPPY);
	public static final ParticleOptions ANGRY_VILLAGER = new LegacyParticleOptions(EnumParticleTypes.VILLAGER_ANGRY);
	public static final ParticleOptions RAIN = new LegacyParticleOptions(EnumParticleTypes.WATER_DROP);
	public static final ParticleOptions SPLASH = new LegacyParticleOptions(EnumParticleTypes.WATER_SPLASH);
	public static final ParticleOptions BUBBLE = new LegacyParticleOptions(EnumParticleTypes.WATER_BUBBLE);
	public static final ParticleOptions ENCHANT = new LegacyParticleOptions(EnumParticleTypes.ENCHANTMENT_TABLE);
	public static final ParticleOptions PORTAL = new LegacyParticleOptions(EnumParticleTypes.PORTAL);
	public static final ParticleOptions END_ROD = new LegacyParticleOptions(EnumParticleTypes.END_ROD);
	public static final ParticleOptions WITCH = new LegacyParticleOptions(EnumParticleTypes.SPELL_WITCH);
	public static final ParticleOptions EFFECT = new LegacyParticleOptions(EnumParticleTypes.SPELL);
	public static final ParticleOptions CRIT = new LegacyParticleOptions(EnumParticleTypes.CRIT);
	public static final ParticleOptions CLOUD = new LegacyParticleOptions(EnumParticleTypes.CLOUD);
	public static final ParticleOptions HEART = new LegacyParticleOptions(EnumParticleTypes.HEART);
	public static final ParticleOptions EXPLOSION = new LegacyParticleOptions(EnumParticleTypes.EXPLOSION_NORMAL);
	public static final ParticleOptions LAVA = new LegacyParticleOptions(EnumParticleTypes.LAVA);
	public static final ParticleOptions NOTE = new LegacyParticleOptions(EnumParticleTypes.NOTE);
	/** The item particle needs the item: {@link ItemParticleOption}. */
	public static final EnumParticleTypes ITEM = EnumParticleTypes.ITEM_CRACK;

	private ParticleTypes() {
	}
}
