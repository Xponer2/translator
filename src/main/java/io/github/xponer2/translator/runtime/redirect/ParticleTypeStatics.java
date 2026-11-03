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

import io.github.xponer2.translator.runtime.mc.SimpleParticleType;
import io.github.xponer2.translator.runtime.mc.VanillaParticleType;

import net.minecraft.util.EnumParticleTypes;

/**
 * 1.21 {@code ParticleTypes} as a 1.21.1 jar reads them: the simple particle types (the Translator's ParticleTypes
 * has them as particle options, for the source ports), each spawning the nearest 1.12.2 particle.
 */
public final class ParticleTypeStatics {
	public static final SimpleParticleType SMOKE = of(EnumParticleTypes.SMOKE_NORMAL);
	public static final SimpleParticleType LARGE_SMOKE = of(EnumParticleTypes.SMOKE_LARGE);
	public static final SimpleParticleType FLAME = of(EnumParticleTypes.FLAME);
	public static final SimpleParticleType SOUL_FIRE_FLAME = of(EnumParticleTypes.FLAME);
	public static final SimpleParticleType HAPPY_VILLAGER = of(EnumParticleTypes.VILLAGER_HAPPY);
	public static final SimpleParticleType ANGRY_VILLAGER = of(EnumParticleTypes.VILLAGER_ANGRY);
	public static final SimpleParticleType RAIN = of(EnumParticleTypes.WATER_DROP);
	public static final SimpleParticleType SPLASH = of(EnumParticleTypes.WATER_SPLASH);
	public static final SimpleParticleType BUBBLE = of(EnumParticleTypes.WATER_BUBBLE);
	public static final SimpleParticleType FISHING = of(EnumParticleTypes.WATER_WAKE);
	public static final SimpleParticleType UNDERWATER = of(EnumParticleTypes.SUSPENDED);
	public static final SimpleParticleType ENCHANT = of(EnumParticleTypes.ENCHANTMENT_TABLE);
	public static final SimpleParticleType PORTAL = of(EnumParticleTypes.PORTAL);
	public static final SimpleParticleType END_ROD = of(EnumParticleTypes.END_ROD);
	public static final SimpleParticleType WITCH = of(EnumParticleTypes.SPELL_WITCH);
	public static final SimpleParticleType EFFECT = of(EnumParticleTypes.SPELL);
	public static final SimpleParticleType INSTANT_EFFECT = of(EnumParticleTypes.SPELL_INSTANT);
	public static final SimpleParticleType CRIT = of(EnumParticleTypes.CRIT);
	public static final SimpleParticleType ENCHANTED_HIT = of(EnumParticleTypes.CRIT_MAGIC);
	public static final SimpleParticleType CLOUD = of(EnumParticleTypes.CLOUD);
	public static final SimpleParticleType POOF = of(EnumParticleTypes.EXPLOSION_NORMAL);
	public static final SimpleParticleType HEART = of(EnumParticleTypes.HEART);
	public static final SimpleParticleType EXPLOSION = of(EnumParticleTypes.EXPLOSION_LARGE);
	public static final SimpleParticleType EXPLOSION_EMITTER = of(EnumParticleTypes.EXPLOSION_HUGE);
	public static final SimpleParticleType FIREWORK = of(EnumParticleTypes.FIREWORKS_SPARK);
	public static final SimpleParticleType LAVA = of(EnumParticleTypes.LAVA);
	public static final SimpleParticleType NOTE = of(EnumParticleTypes.NOTE);
	public static final SimpleParticleType DRIPPING_WATER = of(EnumParticleTypes.DRIP_WATER);
	public static final SimpleParticleType DRIPPING_LAVA = of(EnumParticleTypes.DRIP_LAVA);
	public static final SimpleParticleType MYCELIUM = of(EnumParticleTypes.TOWN_AURA);
	public static final SimpleParticleType ITEM_SLIME = of(EnumParticleTypes.SLIME);
	public static final SimpleParticleType ITEM_SNOWBALL = of(EnumParticleTypes.SNOWBALL);
	public static final SimpleParticleType DAMAGE_INDICATOR = of(EnumParticleTypes.DAMAGE_INDICATOR);
	public static final SimpleParticleType SWEEP_ATTACK = of(EnumParticleTypes.SWEEP_ATTACK);
	public static final SimpleParticleType TOTEM_OF_UNDYING = of(EnumParticleTypes.TOTEM);
	public static final SimpleParticleType SPIT = of(EnumParticleTypes.SPIT);
	public static final SimpleParticleType DRAGON_BREATH = of(EnumParticleTypes.DRAGON_BREATH);
	public static final SimpleParticleType ELDER_GUARDIAN = of(EnumParticleTypes.MOB_APPEARANCE);

	// 1.21 particles 1.12.2 doesn't have: the 1.12.2 particle that looks most alike
	public static final SimpleParticleType CAMPFIRE_COSY_SMOKE = of(EnumParticleTypes.SMOKE_LARGE);
	public static final SimpleParticleType CAMPFIRE_SIGNAL_SMOKE = of(EnumParticleTypes.SMOKE_LARGE);
	public static final SimpleParticleType BUBBLE_POP = of(EnumParticleTypes.WATER_BUBBLE);
	public static final SimpleParticleType BUBBLE_COLUMN_UP = of(EnumParticleTypes.WATER_BUBBLE);
	public static final SimpleParticleType CURRENT_DOWN = of(EnumParticleTypes.WATER_BUBBLE);
	public static final SimpleParticleType ELECTRIC_SPARK = of(EnumParticleTypes.CRIT_MAGIC);
	public static final SimpleParticleType FLASH = of(EnumParticleTypes.FIREWORKS_SPARK);
	public static final SimpleParticleType COMPOSTER = of(EnumParticleTypes.VILLAGER_HAPPY);
	public static final SimpleParticleType WHITE_ASH = of(EnumParticleTypes.TOWN_AURA);
	public static final SimpleParticleType ASH = of(EnumParticleTypes.TOWN_AURA);
	public static final SimpleParticleType CRIMSON_SPORE = of(EnumParticleTypes.TOWN_AURA);
	public static final SimpleParticleType WARPED_SPORE = of(EnumParticleTypes.TOWN_AURA);
	public static final SimpleParticleType SPORE_BLOSSOM_AIR = of(EnumParticleTypes.TOWN_AURA);
	public static final SimpleParticleType FALLING_WATER = of(EnumParticleTypes.DRIP_WATER);
	public static final SimpleParticleType FALLING_LAVA = of(EnumParticleTypes.DRIP_LAVA);
	public static final SimpleParticleType LANDING_LAVA = of(EnumParticleTypes.DRIP_LAVA);
	public static final SimpleParticleType DRIPPING_HONEY = of(EnumParticleTypes.DRIP_LAVA);
	public static final SimpleParticleType SOUL = of(EnumParticleTypes.SMOKE_NORMAL);
	public static final SimpleParticleType REVERSE_PORTAL = of(EnumParticleTypes.PORTAL);
	public static final SimpleParticleType SMALL_FLAME = of(EnumParticleTypes.FLAME);
	public static final SimpleParticleType SNOWFLAKE = of(EnumParticleTypes.SNOW_SHOVEL);
	public static final SimpleParticleType SQUID_INK = of(EnumParticleTypes.SMOKE_NORMAL);
	public static final SimpleParticleType GLOW = of(EnumParticleTypes.END_ROD);
	public static final SimpleParticleType WAX_ON = of(EnumParticleTypes.VILLAGER_HAPPY);
	public static final SimpleParticleType WAX_OFF = of(EnumParticleTypes.VILLAGER_HAPPY);
	public static final SimpleParticleType SCRAPE = of(EnumParticleTypes.CRIT);
	public static final SimpleParticleType NAUTILUS = of(EnumParticleTypes.ENCHANTMENT_TABLE);
	public static final SimpleParticleType DOLPHIN = of(EnumParticleTypes.WATER_WAKE);
	public static final SimpleParticleType SNEEZE = of(EnumParticleTypes.SLIME);
	public static final SimpleParticleType CHERRY_LEAVES = of(EnumParticleTypes.VILLAGER_HAPPY);
	public static final SimpleParticleType SCULK_SOUL = of(EnumParticleTypes.SMOKE_NORMAL);
	public static final SimpleParticleType SONIC_BOOM = of(EnumParticleTypes.EXPLOSION_LARGE);
	public static final SimpleParticleType GUST = of(EnumParticleTypes.CLOUD);
	public static final SimpleParticleType SMALL_GUST = of(EnumParticleTypes.CLOUD);
	public static final SimpleParticleType TRIAL_SPAWNER_DETECTED_PLAYER = of(EnumParticleTypes.FLAME);
	public static final SimpleParticleType EGG_CRACK = of(EnumParticleTypes.ITEM_CRACK);

	// particles with options: the options carry the state, item or color
	public static final io.github.xponer2.translator.runtime.mc.ParticleType<?> BLOCK = new io.github.xponer2.translator.runtime.mc.VanillaOptionType(
			EnumParticleTypes.BLOCK_CRACK);
	public static final io.github.xponer2.translator.runtime.mc.ParticleType<?> BLOCK_MARKER = new io.github.xponer2.translator.runtime.mc.VanillaOptionType(
			EnumParticleTypes.BLOCK_DUST);
	public static final io.github.xponer2.translator.runtime.mc.ParticleType<?> DUST_PILLAR = new io.github.xponer2.translator.runtime.mc.VanillaOptionType(
			EnumParticleTypes.BLOCK_DUST);
	public static final io.github.xponer2.translator.runtime.mc.ParticleType<?> FALLING_DUST = new io.github.xponer2.translator.runtime.mc.VanillaOptionType(
			EnumParticleTypes.FALLING_DUST);
	public static final io.github.xponer2.translator.runtime.mc.ParticleType<?> ITEM = new io.github.xponer2.translator.runtime.mc.VanillaOptionType(
			EnumParticleTypes.ITEM_CRACK);
	public static final io.github.xponer2.translator.runtime.mc.ParticleType<?> ENTITY_EFFECT = new io.github.xponer2.translator.runtime.mc.VanillaOptionType(
			EnumParticleTypes.SPELL_MOB);
	public static final io.github.xponer2.translator.runtime.mc.ParticleType<?> DUST = new io.github.xponer2.translator.runtime.mc.VanillaOptionType(
			EnumParticleTypes.REDSTONE);

	private ParticleTypeStatics() {
	}

	private static SimpleParticleType of(EnumParticleTypes legacy) {
		return new VanillaParticleType(legacy);
	}
}
