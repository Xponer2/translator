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

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.Holder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.21 {@code PoiManager}: the points of interest of a level (villagers' beds and workstations, nether portals, a mod's
 * own). 1.12.2 keeps none: a level's manager finds nothing and drops what is added. It was a carried 1.21 class the
 * level had none of ({@code level.getPoiManager()} gave null): Create's contraptions, AE2's spatial storage, the
 * Aether's portals and Iron's Spells' village goals crashed on it. The calls with 1.21's {@code Occupancy} (a carried
 * enum) are redirected to the static ones below.
 */
public final class PoiManager {
	/** The one manager of all levels. */
	public static final PoiManager NONE = new PoiManager();

	private PoiManager() {
	}

	public void add(BlockPos pos, @Nullable Holder<?> type) {
	}

	public void remove(BlockPos pos) {
	}

	/** The POI section there (none). */
	public Optional<Object> getOrLoad(long section) {
		return Optional.empty();
	}

	public Optional<Holder<?>> getType(BlockPos pos) {
		return Optional.empty();
	}

	public boolean exists(BlockPos pos, @Nullable Predicate<?> type) {
		return false;
	}

	public void checkConsistencyWithBlocks(@Nullable Object sectionPos, @Nullable Object section) {
	}

	// --- with the carried Occupancy (redirected here) ------------------------------------------------------------------

	public static void ensureLoadedAndValid(PoiManager manager, @Nullable World level, BlockPos pos, int radius) {
	}

	public static Stream<Object> getInRange(PoiManager manager, @Nullable Predicate<?> type, BlockPos pos, int distance, @Nullable Object occupancy) {
		return Stream.empty();
	}

	public static Stream<Object> getInSquare(PoiManager manager, @Nullable Predicate<?> type, BlockPos pos, int distance, @Nullable Object occupancy) {
		return Stream.empty();
	}

	public static Stream<Object> getInChunk(PoiManager manager, @Nullable Predicate<?> type, @Nullable Object chunkPos, @Nullable Object occupancy) {
		return Stream.empty();
	}

	public static Optional<BlockPos> findClosest(PoiManager manager, @Nullable Predicate<?> type, BlockPos pos, int distance, @Nullable Object occupancy) {
		return Optional.empty();
	}

	public static Optional<BlockPos> find(PoiManager manager, @Nullable Predicate<?> type, @Nullable Predicate<?> posFilter, BlockPos pos, int distance,
			@Nullable Object occupancy) {
		return Optional.empty();
	}

	public static Stream<BlockPos> findAll(PoiManager manager, @Nullable Predicate<?> type, @Nullable Predicate<?> posFilter, BlockPos pos, int distance,
			@Nullable Object occupancy) {
		return Stream.empty();
	}

	public static long getCountInRange(PoiManager manager, @Nullable Predicate<?> type, BlockPos pos, int distance, @Nullable Object occupancy) {
		return 0L;
	}

	public static boolean existsAtPosition(PoiManager manager, @Nullable Object type, BlockPos pos) {
		return false;
	}
}
