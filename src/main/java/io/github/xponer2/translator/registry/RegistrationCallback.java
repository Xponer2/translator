package io.github.xponer2.translator.registry;

import net.minecraft.util.ResourceLocation;

/**
 * An object of a mapped registry that has to register itself somewhere in 1.12.2 too (a block entity type registers
 * its tile entity class); called by {@link DeferredRegister} when the object is registered.
 */
public interface RegistrationCallback {
    void onRegistered(ResourceLocation id);
}
