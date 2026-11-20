/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import ladder.adapter.Dispatch;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.extensions.IItemExtension;

/**
 * 1.21.1's supportsEnchantment on 1.20.4: the default (an enchanted book takes any, else the enchantment's own check),
 * a mod item's override asked where 1.21.1 asks it (the anvil, enchant_randomly, /enchant: 1.20.4 asks
 * {@code Enchantment.canEnchant} there: AnvilEnchantMixin, EnchantRandomlyMixin, EnchantCommandMixin), and the calls of {@code ItemStack.supportsEnchantment}.
 */
public final class EnchantSupport {
    /** inside the enchantment's own check (the default asks it): the item's override isn't asked again */
    private static final ThreadLocal<Boolean> OWN_CHECK = ThreadLocal.withInitial(() -> false);

    private EnchantSupport() {
    }

    /** 1.21.1's default: {@code stack.is(Items.ENCHANTED_BOOK) || enchantment.value().isSupportedItem(stack)} */
    public static boolean byDefault(ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            return true;
        }
        boolean outer = OWN_CHECK.get();
        OWN_CHECK.set(true);
        try {
            return enchantment.value().canEnchant(stack);
        } finally {
            OWN_CHECK.set(outer);
        }
    }

    /** the O row's defaults: a mod item's {@code super.supportsEnchantment(stack, enchantment)} */
    public static boolean supportsEnchantment(IItemExtension self, ItemStack stack, Holder<Enchantment> enchantment) {
        return byDefault(stack, enchantment);
    }

    /** {@code ItemStack.supportsEnchantment(enchantment)}: the item's override, else the default */
    public static boolean stackSupportsEnchantment(net.neoforged.neoforge.common.extensions.IItemStackExtension self, Holder<Enchantment> enchantment) {
        ItemStack stack = (ItemStack) self;
        Boolean asked = ask(stack, enchantment.value());
        return asked != null ? asked : byDefault(stack, enchantment);
    }

    /**
     * What a converted mod item's own supportsEnchantment says of the enchantment on the stack, or null when the item
     * has none (or the enchantment's own check is asking).
     */
    public static Boolean ask(ItemStack stack, Enchantment enchantment) {
        if (OWN_CHECK.get() || !(stack.getItem() instanceof EnchantSupportApi api)
                || !Dispatch.declares(stack.getItem().getClass(), "supportsEnchantment", ItemStack.class, Holder.class)) {
            return null;
        }
        return api.supportsEnchantment(stack, BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment));
    }

    /** a 1.20.4 call site's {@code enchantment.canEnchant(stack)}, where 1.21.1 asks {@code stack.supportsEnchantment} */
    public static boolean canEnchantThere(Enchantment enchantment, ItemStack stack) {
        Boolean asked = ask(stack, enchantment);
        return asked != null ? asked : enchantment.canEnchant(stack);
    }

    /** {@code item.supportsEnchantment(stack, enchantment)} called through the game's class: the override, else the default */
    public static boolean itemSupportsEnchantment(IItemExtension self, ItemStack stack, Holder<Enchantment> enchantment) {
        if (self instanceof EnchantSupportApi api && Dispatch.declares(self.getClass(), "supportsEnchantment", ItemStack.class, Holder.class)) {
            return api.supportsEnchantment(stack, enchantment);
        }
        return byDefault(stack, enchantment);
    }
}
