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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1.20.4's tooltip call site with 1.20.5's {@code appendHoverText(stack, TooltipContext, ...)} of a converted mod's item. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Redirect(method = "getTooltipLines", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void ladder$appendHoverText(Item item, ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        if (item instanceof HoverTextApi api
                && Dispatch.declares(item.getClass(), "appendHoverText", ItemStack.class, TooltipContext.class, List.class, TooltipFlag.class)) {
            api.appendHoverText(stack, TooltipContext.of(level), lines, flag);
        } else {
            item.appendHoverText(stack, level, lines, flag);
        }
    }
}
