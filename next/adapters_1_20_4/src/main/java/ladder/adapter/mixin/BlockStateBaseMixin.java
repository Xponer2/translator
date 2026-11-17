/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.adapter.Dispatch;
import ladder.adapter.block.BlockUseApi;
import ladder.adapter.block.PathfindableApi;
import ladder.carried.net.minecraft.world.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.20.4's block call sites with 1.20.5's split methods of a converted mod's block: {@code use} is
 * {@code useItemOn}, then {@code useWithoutItem} for the main hand (as 1.20.5's {@code ServerPlayerGameMode} calls
 * them); {@code isPathfindable} without level and position. A block that doesn't override them keeps 1.20.4's own.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow
    public abstract Block getBlock();

    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void ladder$use(Level level, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> result) {
        Block block = this.getBlock();
        if (!(block instanceof BlockUseApi api)) {
            return;
        }
        boolean withItem = Dispatch.declares(block.getClass(), "useItemOn", ItemStack.class, BlockState.class, Level.class, BlockPos.class,
                Player.class, InteractionHand.class, BlockHitResult.class);
        boolean withoutItem = Dispatch.declares(block.getClass(), "useWithoutItem", BlockState.class, Level.class, BlockPos.class, Player.class,
                BlockHitResult.class);
        if (!withItem && !withoutItem) {
            return;
        }
        BlockState state = this.asState();
        BlockPos pos = hit.getBlockPos();
        ItemInteractionResult itemResult = withItem ? api.useItemOn(player.getItemInHand(hand), state, level, pos, player, hand, hit)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (itemResult.consumesAction()) {
            result.setReturnValue(itemResult.result());
            return;
        }
        if (itemResult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == InteractionHand.MAIN_HAND) {
            if (withoutItem) {
                result.setReturnValue(api.useWithoutItem(state, level, pos, player, hit));
            }
            // else the game's own use() of the block's class runs (its 1.20.5 useWithoutItem)
            return;
        }
        result.setReturnValue(itemResult.result());
    }

    @Inject(method = "isPathfindable", at = @At("HEAD"), cancellable = true)
    private void ladder$isPathfindable(BlockGetter getter, BlockPos pos, PathComputationType type, CallbackInfoReturnable<Boolean> result) {
        Block block = this.getBlock();
        if (block instanceof PathfindableApi api
                && Dispatch.declares(block.getClass(), "isPathfindable", BlockState.class, PathComputationType.class)) {
            result.setReturnValue(api.isPathfindable(this.asState(), type));
        }
    }
}
