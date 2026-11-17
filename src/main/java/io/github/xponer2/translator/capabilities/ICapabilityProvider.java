/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.capabilities;

import javax.annotation.Nullable;

/**
 * 1.21 capability provider: the capability of an object (tile entity, stack) for a context (side or nothing).
 */
@FunctionalInterface
public interface ICapabilityProvider<O, C, T> {
	@Nullable
	T getCapability(O object, @Nullable C context);
}
