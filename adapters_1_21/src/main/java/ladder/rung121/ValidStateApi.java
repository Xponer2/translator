/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.21.1's {@code BlockEntity.isValidBlockState(state)} (1.21.1 checks it when a block entity is made or its state set;
 * 1.21 checks nothing). A converted mod's override gets this interface, so a call through a BlockEntity reaches it
 * (BlockEntityAdapters).
 */
public interface ValidStateApi {
    default boolean isValidBlockState(BlockState state) {
        return ((BlockEntity) this).getType().isValid(state);
    }
}
