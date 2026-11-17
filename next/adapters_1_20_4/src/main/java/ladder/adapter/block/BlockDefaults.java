/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.block;

import java.util.List;

import javax.annotation.Nullable;

import ladder.adapter.provided.TooltipContext;
import ladder.carried.net.minecraft.world.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The newer versions' {@code BlockBehaviour} / {@code Block} methods as they are there, for a converted mod block's
 * {@code super} call into the game's own class (which doesn't have them on 1.20.4). Receiver first.
 */
public final class BlockDefaults {
    private BlockDefaults() {
    }

    public static ItemInteractionResult useItemOn(BlockBehaviour self, ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static InteractionResult useWithoutItem(BlockBehaviour self, BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    public static void appendHoverText(Block self, ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    }

    /** 1.20.5's {@code BlockBehaviour.isPathfindable}: by the collision shape (and water for boats). */
    public static boolean isPathfindable(@Nullable BlockBehaviour self, BlockState state, PathComputationType type) {
        return switch (type) {
            case LAND, AIR -> !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            case WATER -> state.getFluidState().is(FluidTags.WATER);
        };
    }
}
