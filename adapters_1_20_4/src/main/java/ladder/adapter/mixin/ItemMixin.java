/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.adapter.component.ItemDefaults;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * An item takes the component values its properties gathered (1.20.5's {@code Item.Properties.component}); a stack's
 * own rarity (1.20.5's rarity component set on the stack) is its rarity, raised by enchantments as 1.20.4 raises the
 * item's.
 */
@Mixin(Item.class)
abstract class ItemMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ladder$components(Item.Properties properties, CallbackInfo ci) {
        ItemDefaults.bind((Item) (Object) this, properties);
    }

    @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
    private void ladder$stackRarity(ItemStack stack, CallbackInfoReturnable<Rarity> cir) {
        Rarity own = ladder.adapter.component.ComponentStore.stackRarity(stack);
        if (own != null) {
            cir.setReturnValue(!stack.isEnchanted() ? own : switch (own) {
                case COMMON, UNCOMMON -> Rarity.RARE;
                case RARE -> Rarity.EPIC;
                default -> own;
            });
        }
    }

}
