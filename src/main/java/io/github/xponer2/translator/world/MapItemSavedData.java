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

import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.runtime.redirect.ApiBridges;

import net.minecraft.world.storage.MapData;

/**
 * The 1.21 {@code MapItemSavedData} a mod's map data extends (E line; the class itself stays 1.12.2's MapData):
 * its 1.21 constructor (center, scale, tracking, dimension). Twilight Forest's magic, maze and ore maps had none, so
 * none could be made. The data is named when it is stored ({@code level.setMapData}, as 1.12.2's map_N).
 */
public class MapItemSavedData extends MapData {
	/** 1.21's locked flag (a map locked in a cartography table); 1.12.2 has none. */
	public boolean locked;

	public MapItemSavedData(int x, int z, byte scale, boolean trackingPosition, boolean unlimitedTracking, boolean locked,
			ResourceKey<?> dimension) {
		super("map_new");
		this.xCenter = x;
		this.zCenter = z;
		this.scale = scale;
		this.trackingPosition = trackingPosition;
		this.unlimitedTracking = unlimitedTracking;
		this.locked = locked;
		this.dimension = ApiBridges.dimensionId(dimension);
	}
}
