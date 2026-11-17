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
package ladder.adapter.mixin;

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.adapter.Dispatch;
import ladder.adapter.item.HoverTextApi;
import ladder.adapter.provided.TooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** 1.20.4's block-item tooltip with 1.20.5's {@code appendHoverText(stack, TooltipContext, ...)} of a converted mod's block. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Redirect(method = "appendHoverText", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/BlockGetter;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void ladder$blockHoverText(Block block, ItemStack stack, @Nullable BlockGetter getter, List<Component> lines, TooltipFlag flag) {
        if (block instanceof HoverTextApi api
                && Dispatch.declares(block.getClass(), "appendHoverText", ItemStack.class, TooltipContext.class, List.class, TooltipFlag.class)) {
            api.appendHoverText(stack, TooltipContext.of(getter instanceof Level level ? level : null), lines, flag);
        } else {
            block.appendHoverText(stack, getter, lines, flag);
        }
    }
}
