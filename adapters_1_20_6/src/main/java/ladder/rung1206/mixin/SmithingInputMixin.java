/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;

import ladder.carried_1_20_6.net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;

/**
 * 1.21's {@code SmithingMenu.createRecipeInput()} on 1.20.6 (added to the class, as 1.21 has it): the template, base and
 * addition slots as a smithing input (mods shadow it to look the recipe up themselves).
 */
@Mixin(value = SmithingMenu.class, priority = 500)
abstract class SmithingInputMixin extends ItemCombinerMenu {
    SmithingInputMixin(MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access) {
        super(type, id, inventory, access);
    }

    private SmithingRecipeInput createRecipeInput() {
        return ladder.rung1206.recipe.SmithingInputs.of(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2));
    }
}
