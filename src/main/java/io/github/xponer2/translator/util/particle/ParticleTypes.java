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
	/** 1.21 {@code STREAM_CODEC}: any particle with its type ({@link ParticleCodec}). */
	public static final io.github.xponer2.translator.network.StreamCodec<io.github.xponer2.translator.network.RegistryFriendlyByteBuf, ParticleOptions> STREAM_CODEC =
			io.github.xponer2.translator.network.StreamCodec.of(ParticleCodec::encode, ParticleCodec::decode);
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

	/**
	 * 1.21 {@code ParticleTypes.CODEC}: a particle by its 1.12.2 name (flame, smoke, ...); a mod's own particle type has
	 * no 1.12.2 name and reads back as an error. Twilight Forest's sinister spawner saves its particles with it (the
	 * field was null and saving the spawner crashed the game).
	 */
	public static final com.mojang.serialization.Codec<ParticleOptions> CODEC = com.mojang.serialization.Codec.STRING.comapFlatMap(name -> {
		EnumParticleTypes type = EnumParticleTypes.getByName(name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name);
		return type != null ? com.mojang.serialization.DataResult.success(new LegacyParticleOptions(type))
				: com.mojang.serialization.DataResult.error("Unknown particle on 1.12.2: " + name);
	}, options -> options instanceof LegacyParticleOptions legacy ? "minecraft:" + legacy.getType().getParticleName()
			: "translator:" + options.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT));

	private ParticleTypes() {
	}
}
