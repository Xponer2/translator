/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
public abstract class ItemStackMixin implements ladder.adapter.component.DataComponentHolder {
    /** a 1.20.4 stack is a 1.20.5 DataComponentHolder (casts, instanceof, calls through the interface) */
    @Override
    public ladder.adapter.component.DataComponentMap getComponents() {
        return ladder.adapter.component.ComponentStore.getComponents((ItemStack) (Object) this);
    }

    /** the custom name an item's properties give (1.20.5's Item.Properties.component(CUSTOM_NAME, ...)) shows */
    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void ladder$defaultName(CallbackInfoReturnable<Component> cir) {
        Component name = ladder.adapter.component.ComponentStore.defaultName((ItemStack) (Object) this);
        if (name != null) {
            cir.setReturnValue(name);
        }
    }

    /** 1.20.5's enchantment_glint_override (the stack's, the item's properties', the game's items' own) decides the glint */
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void ladder$glint(CallbackInfoReturnable<Boolean> cir) {
        Boolean glint = ladder.adapter.component.ComponentStore.glintOverride((ItemStack) (Object) this);
        if (glint != null) {
            cir.setReturnValue(glint);
        }
    }

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
