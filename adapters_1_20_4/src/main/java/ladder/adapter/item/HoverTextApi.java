/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import java.util.List;

import ladder.adapter.provided.TooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.20.5's {@code appendHoverText(stack, TooltipContext, lines, flag)} of items and blocks. 1.20.4's call sites
 * ({@code ItemStack.getTooltipLines}, {@code BlockItem.appendHoverText}) call a converted mod's override.
 */
public interface HoverTextApi {
    default void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    }
}
