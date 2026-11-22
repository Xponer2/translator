/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * 1.21's step of chunk generation, as far as 1.20.6 has it: the status it makes and how far from its chunk it may write
 * blocks (1.20.6's region keeps both itself). A world generation region's {@code generatingStep} (an F row).
 */
public final class ChunkStep {
    private final ChunkStatus targetStatus;
    private final int blockStateWriteRadius;

    private ChunkStep(ChunkStatus targetStatus, int blockStateWriteRadius) {
        this.targetStatus = targetStatus;
        this.blockStateWriteRadius = blockStateWriteRadius;
    }

    /** 1.21's {@code region.generatingStep} (the region's own status and write radius; the access transformer opens them) */
    public static ChunkStep generatingStep(WorldGenRegion region) {
        return new ChunkStep(region.generatingStatus, region.writeRadiusCutoff);
    }

    public ChunkStatus targetStatus() {
        return this.targetStatus;
    }

    public int blockStateWriteRadius() {
        return this.blockStateWriteRadius;
    }
}
