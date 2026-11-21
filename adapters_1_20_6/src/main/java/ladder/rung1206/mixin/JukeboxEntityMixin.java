/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.rung1206.jukebox.Jukebox;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The jukebox takes and plays an item with 1.21's jukebox_playable as a music disc, and stops it after its song's length. */
@Mixin(JukeboxBlockEntity.class)
abstract class JukeboxEntityMixin {
    @Shadow
    private long tickCount;
    @Shadow
    private long recordStartedTick;

    @Shadow
    public abstract ItemStack getTheItem();

    @Shadow
    public abstract boolean isRecordPlaying();

    @Shadow
    public abstract void stopPlaying();

    @Redirect(method = {"setTheItem", "canPlaceItem"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean ladder$disc(ItemStack stack, TagKey<Item> tag) {
        return stack.is(tag) || Jukebox.playable(stack);
    }

    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"))
    private void ladder$songEnds(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        ItemStack item = this.getTheItem();
        if (this.isRecordPlaying() && !(item.getItem() instanceof RecordItem)) {
            Jukebox.song(level.registryAccess(), item).ifPresent(song -> {
                if (song.value().hasFinished(this.tickCount - this.recordStartedTick)) {
                    this.stopPlaying();
                }
            });
        }
    }
}
