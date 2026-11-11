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

import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * 1.21 {@code ChunkSource} (what {@code level.getChunkSource()} gives on either side) on 1.12.2's IChunkProvider, which
 * the client's and the server's chunk providers both are. It was the server's provider only: on the client
 * {@code level.getChunkSource()} gave null, and a mod that looked there for its chunks found none (JourneyMap mapped
 * nothing; 17 of the mods tried here ask for it).
 */
public final class ChunkSources {
	@Nullable
	private static Field clientWorld;

	private ChunkSources() {
	}

	/** The world of a chunk provider (the client's keeps it private). */
	@Nullable
	public static World getLevel(@Nullable IChunkProvider provider) {
		if (provider instanceof ChunkProviderServer server) {
			return server.world;
		}
		if (provider instanceof ChunkSource source) {
			return source.level();
		}
		if (provider == null || !provider.getClass().getName().endsWith("ChunkProviderClient")) {
			return null;
		}
		try {
			if (clientWorld == null) {
				clientWorld = ReflectionHelper.findField(provider.getClass(), "world", "field_73235_d");
			}
			return (World) clientWorld.get(provider);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	/** 1.21 {@code getChunk(x, z, load)}: the loaded chunk, or loaded (generated) when asked. */
	@Nullable
	public static Chunk getChunk(IChunkProvider provider, int x, int z, boolean load) {
		Chunk chunk = provider.getLoadedChunk(x, z);
		return chunk != null || !load ? chunk : provider.provideChunk(x, z);
	}

	/** 1.21 {@code getChunk(x, z, status, load)}: a 1.12.2 chunk is whole or not there. */
	@Nullable
	public static Chunk getChunk(IChunkProvider provider, int x, int z, @Nullable Object status, boolean load) {
		return getChunk(provider, x, z, load);
	}

	/** 1.21 {@code getChunkNow(x, z)}: the chunk when it is loaded, else null. */
	@Nullable
	public static Chunk getChunkNow(IChunkProvider provider, int x, int z) {
		return provider.getLoadedChunk(x, z);
	}

	@Nullable
	public static Chunk getChunkForLighting(IChunkProvider provider, int x, int z) {
		return provider.getLoadedChunk(x, z);
	}

	public static boolean hasChunk(IChunkProvider provider, int x, int z) {
		return provider.getLoadedChunk(x, z) != null;
	}

	/** 1.21 {@code getLightEngine()}: the light engine of the provider's world. */
	@Nullable
	public static LevelLightEngine getLightEngine(IChunkProvider provider) {
		World world = getLevel(provider);
		return world == null ? null : new LevelLightEngine(world);
	}

	/** 1.21 {@code hasChunk} by its 1.12.2 name (the tables rename it to the server provider's). */
	public static boolean chunkExists(IChunkProvider provider, int x, int z) {
		return provider.getLoadedChunk(x, z) != null;
	}

	/** 1.21 {@code getLoadedChunksCount} by its 1.12.2 name (the tables rename it to the server provider's). */
	public static int getLoadedChunkCount(IChunkProvider provider) {
		return getLoadedChunksCount(provider);
	}

	public static int getLoadedChunksCount(IChunkProvider provider) {
		return provider instanceof ChunkProviderServer server ? server.getLoadedChunkCount() : 0;
	}

	public static void tick(IChunkProvider provider, @Nullable BooleanSupplier hasTime, boolean tickChunks) {
		provider.tick();
	}

	public static void setSpawnSettings(IChunkProvider provider, boolean hostile, boolean peaceful) {
	}

	public static void updateChunkForced(IChunkProvider provider, @Nullable Object pos, boolean add) {
	}

	public static void onLightUpdate(IChunkProvider provider, @Nullable Object layer, @Nullable Object section) {
	}
}
