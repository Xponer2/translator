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
