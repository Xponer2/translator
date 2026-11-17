/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.registry;

import net.minecraft.util.ResourceLocation;

/**
 * An object of a mapped registry that has to register itself somewhere in 1.12.2 too (a block entity type registers
 * its tile entity class); called by {@link DeferredRegister} when the object is registered.
 */
public interface RegistrationCallback {
    void onRegistered(ResourceLocation id);
}
