/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.adapter.item.EnchantSupport;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** enchant_randomly loot asks a converted mod item's supportsEnchantment (NeoForge 21.1) where 1.20.4 asks the enchantment */
@Mixin(net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyMixin {
    @Redirect(method = "lambda$run$5", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean ladder$supportsEnchantment(Enchantment enchantment, ItemStack stack) {
        return EnchantSupport.canEnchantThere(enchantment, stack);
    }
}
