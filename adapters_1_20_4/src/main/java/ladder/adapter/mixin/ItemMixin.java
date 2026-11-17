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
