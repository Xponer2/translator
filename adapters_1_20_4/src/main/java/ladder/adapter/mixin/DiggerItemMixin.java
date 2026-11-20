/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.adapter.item.IncorrectBlocksApi;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A digger of a converted mod's tier (1.20.5: the tier's incorrect blocks tag) drops what 1.21's does: the blocks of
 * its kind, unless in the tier's incorrect tag. Vanilla tiers keep 1.20.4's check.
 */
@Mixin(DiggerItem.class)
abstract class DiggerItemMixin extends TieredItem {
    @Shadow
    @Final
    private TagKey<Block> blocks;

    private DiggerItemMixin() {
        super(null, null);
    }

    @Inject(method = "isCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("HEAD"), cancellable = true)
    private void ladder$incorrectTag(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (this.getTier() instanceof IncorrectBlocksApi tier) {
            cir.setReturnValue(!state.is(tier.getIncorrectBlocksForDrops()) && state.is(this.blocks));
        }
    }

    @Inject(method = "isCorrectToolForDrops(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("HEAD"),
            cancellable = true)
    private void ladder$incorrectTagStack(ItemStack stack, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        this.ladder$incorrectTag(state, cir);
    }
}
