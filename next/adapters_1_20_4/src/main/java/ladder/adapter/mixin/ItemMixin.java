/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.adapter.component.ItemDefaults;
import net.minecraft.world.item.Item;

/** An item takes the component values its properties gathered (1.20.5's {@code Item.Properties.component}). */
@Mixin(Item.class)
abstract class ItemMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ladder$components(Item.Properties properties, CallbackInfo ci) {
        ItemDefaults.bind((Item) (Object) this, properties);
    }
}
