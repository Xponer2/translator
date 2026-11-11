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

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.runtime.redirect.WorldBridges;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * 1.21 {@code BiomeManager}: the biomes of a level by block position, over its 4x4x4 noise biomes. Here over a 1.12.2
 * world's biomes (by block), or a mod's own 1.21 biome source ({@code NoiseBiomeSource}, a carried interface, called by
 * its {@code getNoiseBiome(x, y, z)}). It was a carried 1.21 class no level had ({@code level.getBiomeManager()} gave
 * null): Mekanism's solar neutron activator and the Aether's crystal islands crashed on it.
 */
public final class BiomeManager {
	/** A mod level's own manager (a virtual world's override of getBiomeManager()), else none. */
	private static final ClassValue<Method> OWN = new ClassValue<Method>() {
		@Override
		protected Method computeValue(Class<?> type) {
			try {
				Method method = type.getMethod("getBiomeManager");
				return method.getReturnType() == BiomeManager.class && !method.getDeclaringClass().getName().startsWith("net.minecraft.") ? method : null;
			} catch (NoSuchMethodException | SecurityException e) {
				return null;
			}
		}
	};

	@Nullable
	private final Object source;
	/** 1.21's field (Twilight Forest's aurora reads it). */
	public final long biomeZoomSeed;

	private BiomeManager(@Nullable Object source, long seed) {
		this.source = source;
		this.biomeZoomSeed = seed;
	}

	/** The manager of a world: the mod's own when its level class overrides getBiomeManager(), else over its biomes. */
	public static BiomeManager of(World world) {
		Method own = OWN.get(world.getClass());
		if (own != null) {
			try {
				Object manager = own.invoke(world);
				if (manager instanceof BiomeManager) {
					return (BiomeManager) manager;
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed("BiomeManager: " + world.getClass().getName() + ".getBiomeManager()", e);
			}
		}
		return new BiomeManager(world, world.getSeed());
	}

	/** 1.21 {@code new BiomeManager(source, seed)} (a mod's biome source). */
	public static BiomeManager newBiomeManager(@Nullable Object source, long seed) {
		return new BiomeManager(source, seed);
	}

	/** 1.21 {@code BiomeManager.obfuscateSeed(seed)}: the seed as the clients get it. */
	public static long obfuscateSeed(long seed) {
		return com.google.common.hash.Hashing.sha256().hashLong(seed).asLong();
	}

	/** 1.21 {@code withDifferentSource(source)}. */
	public static BiomeManager withDifferentSource(BiomeManager manager, @Nullable Object source) {
		return new BiomeManager(source, manager.biomeZoomSeed);
	}

	public Holder<Biome> getBiome(BlockPos pos) {
		if (this.source instanceof World) {
			return WorldBridges.getBiome((World) this.source, pos);
		}
		return this.getNoiseBiomeAtQuart(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
	}

	public Holder<Biome> getNoiseBiomeAtPosition(BlockPos pos) {
		return this.getNoiseBiomeAtQuart(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
	}

	public Holder<Biome> getNoiseBiomeAtPosition(double x, double y, double z) {
		return this.getNoiseBiomeAtQuart((int) Math.floor(x) >> 2, (int) Math.floor(y) >> 2, (int) Math.floor(z) >> 2);
	}

	@SuppressWarnings("unchecked")
	public Holder<Biome> getNoiseBiomeAtQuart(int x, int y, int z) {
		if (this.source instanceof World) {
			return WorldBridges.getNoiseBiome((World) this.source, x, y, z);
		}
		if (this.source != null) {
			try {
				Method method = this.source.getClass().getMethod("getNoiseBiome", int.class, int.class, int.class);
				method.setAccessible(true);
				return (Holder<Biome>) method.invoke(this.source, x, y, z);
			} catch (ReflectiveOperationException | RuntimeException e) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed("BiomeManager: the biome source " + this.source.getClass().getName(), e);
			}
		}
		return Holder.direct(net.minecraft.init.Biomes.PLAINS);
	}
}
