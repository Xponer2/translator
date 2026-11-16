/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
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
