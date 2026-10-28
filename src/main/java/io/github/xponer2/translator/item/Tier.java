package io.github.xponer2.translator.item;

import io.github.xponer2.translator.util.TagKey;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;

/**
 * A tool tier (1.21 {@code net.minecraft.world.item.Tier}). {@link #toolMaterial()} is the matching 1.12.2 tool
 * material for the vanilla tool classes; its harvest level comes from {@link #getIncorrectBlocksForDrops()}
 * (see {@link Tiers#harvestLevel(TagKey)}).
 */
public interface Tier {
    int getUses();

    float getSpeed();

    float getAttackDamageBonus();

    TagKey<Block> getIncorrectBlocksForDrops();

    int getEnchantmentValue();

    Ingredient getRepairIngredient();

    default Item.ToolMaterial toolMaterial() {
        return Tiers.toolMaterial(this);
    }

    default int harvestLevel() {
        return Tiers.harvestLevel(this.getIncorrectBlocksForDrops());
    }
}
