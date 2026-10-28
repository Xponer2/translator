package io.github.xponer2.translator.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * An item model property (1.21 {@code ItemPropertyFunction}); the client registers it as a 1.12.2
 * {@code IItemPropertyGetter} (which has no seed argument) with {@code Item#addPropertyOverride}.
 */
@FunctionalInterface
public interface ItemPropertyFunction {
    float call(ItemStack stack, @Nullable World level, @Nullable EntityLivingBase entity, int seed);
}
