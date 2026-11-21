/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.rung1206.jukebox.Jukebox;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * An item with 1.21's jukebox_playable goes into an empty jukebox as 1.21 puts it there (1.20.6's discs put themselves);
 * a comparator reads its song's output.
 */
@Mixin(JukeboxBlock.class)
abstract class JukeboxBlockMixin extends BaseEntityBlock {
    protected JukeboxBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(JukeboxBlock.HAS_RECORD) || stack.getItem() instanceof RecordItem || !Jukebox.playable(stack)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox) {
            ItemStack one = stack.copyWithCount(1);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            jukebox.setTheItem(one);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            player.awardStat(Stats.PLAY_RECORD);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Inject(method = "getAnalogOutputSignal", at = @At("HEAD"), cancellable = true)
    private void ladder$songOutput(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox && !(jukebox.getTheItem().getItem() instanceof RecordItem)) {
            Jukebox.song(level.registryAccess(), jukebox.getTheItem()).ifPresent(song -> cir.setReturnValue(song.value().comparatorOutput()));
        }
    }
}
