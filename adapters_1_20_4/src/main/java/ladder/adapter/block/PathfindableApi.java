/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

/**
 * 1.20.5's {@code isPathfindable(state, type)} (no level, no position). 1.20.4's {@code BlockStateBase.isPathfindable}
 * calls a converted mod block's override ({@link ladder.adapter.mixin.BlockStateBaseMixin}).
 */
public interface PathfindableApi {
    default boolean isPathfindable(BlockState state, PathComputationType type) {
        return BlockDefaults.isPathfindable(null, state, type);
    }
}
