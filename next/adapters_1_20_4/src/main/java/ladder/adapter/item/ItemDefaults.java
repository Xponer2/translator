/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import java.util.List;

import ladder.adapter.provided.TooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The newer versions' item methods for a converted mod item's {@code super} call into the game's own class. */
public final class ItemDefaults {
    private ItemDefaults() {
    }

    public static void appendHoverText(Item self, ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    }

    /** 1.20.5's {@code BlockItem.appendHoverText}: the block's lines (through 1.20.4's block method). */
    public static void blockItemAppendHoverText(BlockItem self, ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var block = self.getBlock();
        if (block instanceof HoverTextApi api && ladder.adapter.Dispatch.declares(block.getClass(), "appendHoverText", ItemStack.class,
                TooltipContext.class, List.class, TooltipFlag.class)) {
            api.appendHoverText(stack, context, lines, flag);
        } else {
            block.appendHoverText(stack, context.level(), lines, flag);
        }
    }
}
