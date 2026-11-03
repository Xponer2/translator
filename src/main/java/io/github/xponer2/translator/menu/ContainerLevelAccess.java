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
package io.github.xponer2.translator.menu;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** 1.21 {@code ContainerLevelAccess}: the world and position a menu belongs to (or none, on the client). */
public interface ContainerLevelAccess {
	ContainerLevelAccess NULL = new ContainerLevelAccess() {
		@Override
		public <T> Optional<T> evaluate(BiFunction<World, BlockPos, T> function) {
			return Optional.empty();
		}
	};

	static ContainerLevelAccess create(World level, BlockPos pos) {
		return new ContainerLevelAccess() {
			@Override
			public <T> Optional<T> evaluate(BiFunction<World, BlockPos, T> function) {
				return Optional.ofNullable(function.apply(level, pos));
			}
		};
	}

	<T> Optional<T> evaluate(BiFunction<World, BlockPos, T> function);

	default <T> T evaluate(BiFunction<World, BlockPos, T> function, T defaultValue) {
		return this.evaluate(function).orElse(defaultValue);
	}

	default void execute(BiConsumer<World, BlockPos> action) {
		this.evaluate((level, pos) -> {
			action.accept(level, pos);
			return Optional.empty();
		});
	}
}
