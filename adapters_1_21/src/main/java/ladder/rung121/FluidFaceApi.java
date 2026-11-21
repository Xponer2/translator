/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * NeoForge 21.1's {@code IBlockExtension.shouldHideAdjacentFluidFace(state, side, fluid)} (whether a block hides the face
 * of a fluid next to it): a converted mod block that overrides it gets this interface (an O row); the X rows of the calls
 * ask it ({@link MoreAdapters}).
 */
public interface FluidFaceApi {
    default boolean shouldHideAdjacentFluidFace(BlockState state, Direction side, FluidState fluid) {
        return shouldHideAdjacentFluidFace((Block) this, state, side, fluid);
    }

    /** 21.1's default (a super call): the block's own fluid hides the same fluid */
    static boolean shouldHideAdjacentFluidFace(Block self, BlockState state, Direction side, FluidState fluid) {
        return state.getFluidState().getType().isSame(fluid.getType());
    }
}
