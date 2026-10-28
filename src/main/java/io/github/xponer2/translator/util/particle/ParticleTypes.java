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
	public static final ParticleOptions SMOKE = new ParticleOptions(EnumParticleTypes.SMOKE_NORMAL);
	public static final ParticleOptions LARGE_SMOKE = new ParticleOptions(EnumParticleTypes.SMOKE_LARGE);
	public static final ParticleOptions FLAME = new ParticleOptions(EnumParticleTypes.FLAME);
	public static final ParticleOptions SOUL_FIRE_FLAME = new ParticleOptions(EnumParticleTypes.FLAME);
	public static final ParticleOptions HAPPY_VILLAGER = new ParticleOptions(EnumParticleTypes.VILLAGER_HAPPY);
	public static final ParticleOptions ANGRY_VILLAGER = new ParticleOptions(EnumParticleTypes.VILLAGER_ANGRY);
	public static final ParticleOptions RAIN = new ParticleOptions(EnumParticleTypes.WATER_DROP);
	public static final ParticleOptions SPLASH = new ParticleOptions(EnumParticleTypes.WATER_SPLASH);
	public static final ParticleOptions BUBBLE = new ParticleOptions(EnumParticleTypes.WATER_BUBBLE);
	public static final ParticleOptions ENCHANT = new ParticleOptions(EnumParticleTypes.ENCHANTMENT_TABLE);
	public static final ParticleOptions PORTAL = new ParticleOptions(EnumParticleTypes.PORTAL);
	public static final ParticleOptions END_ROD = new ParticleOptions(EnumParticleTypes.END_ROD);
	public static final ParticleOptions WITCH = new ParticleOptions(EnumParticleTypes.SPELL_WITCH);
	public static final ParticleOptions EFFECT = new ParticleOptions(EnumParticleTypes.SPELL);
	public static final ParticleOptions CRIT = new ParticleOptions(EnumParticleTypes.CRIT);
	public static final ParticleOptions CLOUD = new ParticleOptions(EnumParticleTypes.CLOUD);
	public static final ParticleOptions HEART = new ParticleOptions(EnumParticleTypes.HEART);
	public static final ParticleOptions EXPLOSION = new ParticleOptions(EnumParticleTypes.EXPLOSION_NORMAL);
	public static final ParticleOptions LAVA = new ParticleOptions(EnumParticleTypes.LAVA);
	public static final ParticleOptions NOTE = new ParticleOptions(EnumParticleTypes.NOTE);
	/** The item particle needs the item: {@link ItemParticleOption}. */
	public static final EnumParticleTypes ITEM = EnumParticleTypes.ITEM_CRACK;

	private ParticleTypes() {
	}
}
