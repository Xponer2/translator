/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 1.21.1's block entity members on 1.21 (the X and O rows' adapters) */
public final class BlockEntityAdapters {
    private BlockEntityAdapters() {
    }

    /** {@code blockEntity.isValidBlockState(state)}: the mod block entity's own override, else its type's check */
    public static boolean isValidBlockState(BlockEntity blockEntity, BlockState state) {
        if (blockEntity instanceof ValidStateApi api && Dispatch.declares(blockEntity.getClass(), "isValidBlockState", BlockState.class)) {
            return api.isValidBlockState(state);
        }
        return blockEntity.getType().isValid(state);
    }

    /** a mod block entity's {@code super.isValidBlockState(state)} */
    public static boolean superValidBlockState(BlockEntity blockEntity, BlockState state) {
        return blockEntity.getType().isValid(state);
    }
}
