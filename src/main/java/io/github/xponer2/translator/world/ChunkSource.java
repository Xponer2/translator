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

import net.minecraft.launchwrapper.Launch;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * 1.21 {@code ChunkSource} as the class a mod's own chunk source extends (the chunks of a world the mod makes itself: a
 * scene, a schematic): a 1.12.2 chunk provider whose chunks are the ones the mod's 1.21 {@code getChunk(x, z, status,
 * load)} gives. 1.21's ChunkSource is 1.12.2's IChunkProvider in the tables, an interface, which no class can extend.
 */
public abstract class ChunkSource implements IChunkProvider {
	@Nullable
	private Method getChunk;
	private boolean looked;
	@Nullable
	private static Object fullStatus;

	protected ChunkSource() {
	}

	/** The world the chunks are of, when the mod's source says ({@code getLevel()}). */
	@Nullable
	World level() {
		try {
			Object level = this.getClass().getMethod("getLevel").invoke(this);
			return level instanceof World world ? world : null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	/** The mod's 1.21 {@code getChunk(x, z, status, load)} (its status is a 1.21 class: found by its shape). */
	@Nullable
	private Chunk modChunk(int x, int z, boolean load) {
		if (!this.looked) {
			this.looked = true;
			for (Class<?> type = this.getClass(); type != null && type != ChunkSource.class && this.getChunk == null; type = type.getSuperclass()) {
				for (Method method : type.getDeclaredMethods()) {
					Class<?>[] parameters = method.getParameterTypes();
					if ("getChunk".equals(method.getName()) && parameters.length == 4 && parameters[0] == int.class && parameters[1] == int.class
							&& parameters[3] == boolean.class) {
						method.setAccessible(true);
						this.getChunk = method;
						break;
					}
				}
			}
		}
		if (this.getChunk == null) {
			return null;
		}
		try {
			if (fullStatus == null) {
				try {
					fullStatus = Class.forName("net.minecraft.world.level.chunk.status.ChunkStatus", true, Launch.classLoader).getField("FULL").get(null);
				} catch (ReflectiveOperationException | LinkageError e) {
					fullStatus = Boolean.FALSE;
				}
			}
			Object status = this.getChunk.getParameterTypes()[2].isInstance(fullStatus) ? fullStatus : null;
			return this.getChunk.invoke(this, x, z, status, load) instanceof Chunk chunk ? chunk : null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	// --- 1.21 methods a mod calls on its source or overrides ----------------------------------------------------------

	@Nullable
	public Chunk getChunk(int x, int z, boolean load) {
		return this.modChunk(x, z, load);
	}

	@Nullable
	public Chunk getChunkNow(int x, int z) {
		return this.getChunk(x, z, false);
	}

	public boolean hasChunk(int x, int z) {
		return this.chunkExists(x, z);
	}

	/** 1.21 {@code hasChunk} by its 1.12.2 name (the tables rename it so: a mod's override of it is this one). */
	public boolean chunkExists(int x, int z) {
		return this.getChunk(x, z, false) != null;
	}

	public int getLoadedChunksCount() {
		return this.getLoadedChunkCount();
	}

	/** 1.21 {@code getLoadedChunksCount} by its 1.12.2 name (the tables rename it so). */
	public int getLoadedChunkCount() {
		return 0;
	}

	public String gatherStats() {
		return this.getClass().getSimpleName();
	}

	public void setSpawnSettings(boolean hostile, boolean peaceful) {
	}

	public void close() {
	}

	// --- the chunk provider of 1.12.2 ---------------------------------------------------------------------------------

	@Nullable
	@Override
	public Chunk getLoadedChunk(int x, int z) {
		return this.getChunk(x, z, false);
	}

	@Override
	public Chunk provideChunk(int x, int z) {
		return this.getChunk(x, z, true);
	}

	@Override
	public boolean tick() {
		return false;
	}

	@Override
	public String makeString() {
		return this.gatherStats();
	}

	@Override
	public boolean isChunkGeneratedAt(int x, int z) {
		return this.chunkExists(x, z);
	}
}
