/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.recipe;

import ladder.carried_1_20_6.net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21's smithing input made outside the adapters' mixins: a mixin's code must not name a carried class's constructor
 * (Mixin loads it while attaching, and the carried jar has it only when a converted mod needs it).
 */
public final class SmithingInputs {
    private SmithingInputs() {
    }

    public static SmithingRecipeInput of(ItemStack template, ItemStack base, ItemStack addition) {
        return new SmithingRecipeInput(template, base, addition);
    }
}
