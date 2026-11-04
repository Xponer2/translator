/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.capabilities;

import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/**
 * NeoForge {@code BlockCapabilityCache}: the capability of a block next to a machine (pipes, cables), asked each time
 * on 1.12.2, where tile entities give their capabilities themselves (nothing to invalidate).
 */
public final class BlockCapabilityCache<T, C> {
	private final BlockCapability<T, C> capability;
	private final WorldServer level;
	private final BlockPos pos;
	private final C context;
	private final BooleanSupplier isValid;

	private BlockCapabilityCache(BlockCapability<T, C> capability, WorldServer level, BlockPos pos, C context, BooleanSupplier isValid) {
		this.capability = capability;
		this.level = level;
		this.pos = pos.toImmutable();
		this.context = context;
		this.isValid = isValid;
	}

	public static <T, C> BlockCapabilityCache<T, C> create(BlockCapability<T, C> capability, WorldServer level, BlockPos pos, C context) {
		return new BlockCapabilityCache<>(capability, level, pos, context, () -> true);
	}

	public static <T, C> BlockCapabilityCache<T, C> create(BlockCapability<T, C> capability, WorldServer level, BlockPos pos, C context,
			BooleanSupplier isValid, Runnable invalidationListener) {
		return new BlockCapabilityCache<>(capability, level, pos, context, isValid);
	}

	public WorldServer level() {
		return this.level;
	}

	public BlockPos pos() {
		return this.pos;
	}

	public C context() {
		return this.context;
	}

	@Nullable
	public T getCapability() {
		if (!this.isValid.getAsBoolean() || !this.level.isBlockLoaded(this.pos)) {
			return null;
		}
		return this.capability.getCapability(this.level, this.pos, this.context);
	}
}
