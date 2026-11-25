/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import ladder.rung1206.enchantment.AddedEnchantmentHelper;
import ladder.rung1206.enchantment.EnchantHelpers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21's mending call in ExperienceOrb.repairPlayerItems: getRandomItemWith(REPAIR_WITH_XP, player, ItemStack::isDamaged)
 * of EnchantmentHelper (the shape patch's), its find the first local; the repair itself 1.20.6's. The mods' mixins aim
 * at that call (Quark's predicate, Clumps' captured find).
 */
@Mixin(value = ExperienceOrb.class, priority = 500)
abstract class ExperienceOrbRepairMixin {
    @Shadow
    private int durabilityToXp(int durability) {
        throw new AssertionError();
    }

    // an overwrite, not an injector: the mods' points are the call 1.21 makes here (1.20.6 calls the Enchantment form)
    /**
     * @author ladder
     * @reason 1.21's call shape of mending (an effect component, an optional find), 1.20.6's repair
     */
    @Overwrite
    private int repairPlayerItems(Player player, int value) {
        Optional<Object> found = AddedEnchantmentHelper.getRandomItemWith(EnchantHelpers.repairWithXp(), player, ItemStack::isDamaged);
        if (found.isPresent()) {
            ItemStack stack = EnchantHelpers.stackOf(found.get());
            int repair = Math.min((int) ((float) value * stack.getXpRepairRatio()), stack.getDamageValue());
            stack.setDamageValue(stack.getDamageValue() - repair);
            int rest = value - this.durabilityToXp(repair);
            return rest > 0 ? this.repairPlayerItems(player, rest) : 0;
        }
        return value;
    }
}
