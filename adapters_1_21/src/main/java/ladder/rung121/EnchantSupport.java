/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 1.21.1's supportsEnchantment on 1.21: the default (an enchanted book takes any, else the enchantment's own check), a
 * mod item's override asked where 1.21.1 asks it (1.21 asks {@code Enchantment.canEnchant} there), and the calls.
 */
public final class EnchantSupport {
    private EnchantSupport() {
    }

    /** 1.21.1's default: {@code stack.is(Items.ENCHANTED_BOOK) || enchantment.value().isSupportedItem(stack)} */
    public static boolean byDefault(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.is(Items.ENCHANTED_BOOK) || enchantment.value().isSupportedItem(stack);
    }

    /** the O row's defaults: a mod item's {@code super.supportsEnchantment(stack, enchantment)} */
    public static boolean supportsEnchantment(IItemExtension self, ItemStack stack, Holder<Enchantment> enchantment) {
        return byDefault(stack, enchantment);
    }

    /** {@code item.supportsEnchantment(stack, enchantment)} through the game's class: the override, else the default */
    public static boolean itemSupportsEnchantment(IItemExtension self, ItemStack stack, Holder<Enchantment> enchantment) {
        if (self instanceof EnchantSupportApi api && Dispatch.declares(self.getClass(), "supportsEnchantment", ItemStack.class, Holder.class)) {
            return api.supportsEnchantment(stack, enchantment);
        }
        return byDefault(stack, enchantment);
    }

    /** {@code stack.supportsEnchantment(enchantment)}: the item's override, else the default */
    public static boolean stackSupportsEnchantment(IItemStackExtension self, Holder<Enchantment> enchantment) {
        ItemStack stack = (ItemStack) self;
        return itemSupportsEnchantment(stack.getItem(), stack, enchantment);
    }

    /** a 1.21 call site's {@code enchantment.canEnchant(stack)}, where 1.21.1 asks {@code stack.supportsEnchantment} */
    public static boolean canEnchantThere(Enchantment enchantment, ItemStack stack) {
        if (stack.getItem() instanceof EnchantSupportApi api
                && Dispatch.declares(stack.getItem().getClass(), "supportsEnchantment", ItemStack.class, Holder.class)) {
            Holder<Enchantment> holder = holder(enchantment);
            if (holder != null) {
                return api.supportsEnchantment(stack, holder);
            }
        }
        return enchantment.canEnchant(stack);
    }

    /** a shape-patched call site's {@code stack.supportsEnchantment(holder)}: the mod item's override, else 21.0's own check */
    public static boolean supportsHere(ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack.getItem() instanceof EnchantSupportApi api
                && Dispatch.declares(stack.getItem().getClass(), "supportsEnchantment", ItemStack.class, Holder.class)) {
            return api.supportsEnchantment(stack, enchantment);
        }
        return enchantment.value().canEnchant(stack);
    }

    /** the enchantment's registry holder, else a direct one (no registries yet) */
    public static Holder<Enchantment> holderOf(Enchantment enchantment) {
        Holder<Enchantment> holder = holder(enchantment);
        return holder != null ? holder : Holder.direct(enchantment);
    }

    /** the registry's holder of an enchantment (1.21's are data: the running server's registry, else the client's) */
    static Holder<Enchantment> holder(Enchantment enchantment) {
        RegistryAccess access = ServerLifecycleHooks.getCurrentServer() != null ? ServerLifecycleHooks.getCurrentServer().registryAccess()
                : FMLEnvironment.dist.isClient() ? ClientAccess.registries() : null;
        return access == null ? null : access.registryOrThrow(Registries.ENCHANTMENT).wrapAsHolder(enchantment);
    }
}
