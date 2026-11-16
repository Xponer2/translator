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
