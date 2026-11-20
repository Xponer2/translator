/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.rung121.attribute.AttributeTooltipContext;
import ladder.rung121.attribute.AttributeUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An item's attribute lines the NeoForge 21.1 way (AttributeUtil, with its two events) where 1.21 makes its own: the
 * tooltip's context and flag, which 1.21's attribute section isn't given, are kept while its tooltip is made.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    private static final ThreadLocal<Deque<Object[]>> LADDER$TOOLTIP = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "getTooltipLines", at = @At("HEAD"))
    private void ladder$tooltipStart(Item.TooltipContext context, @Nullable Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        LADDER$TOOLTIP.get().push(new Object[] {context, flag});
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void ladder$tooltipEnd(Item.TooltipContext context, @Nullable Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        LADDER$TOOLTIP.get().poll();
    }

    @Inject(method = "addAttributeTooltips", at = @At("HEAD"), cancellable = true)
    private void ladder$attributeTooltips(Consumer<Component> tooltip, @Nullable Player player, CallbackInfo ci) {
        Object[] made = LADDER$TOOLTIP.get().peek();
        if (made == null) {
            return;
        }
        AttributeUtil.addAttributeTooltips((ItemStack) (Object) this, tooltip,
                AttributeTooltipContext.of(player, (Item.TooltipContext) made[0], (TooltipFlag) made[1]));
        ci.cancel();
    }
}
