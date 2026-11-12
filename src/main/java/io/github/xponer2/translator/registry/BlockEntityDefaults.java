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
package io.github.xponer2.translator.registry;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

/**
 * What the no-argument constructor the converter gives a mod's block entity passes on (1.12.2 makes a saved block
 * entity with it, then reads its data): the type registered for its class and that type's block's default state. With a
 * null state a block entity that reads its state when made threw (Iron's Spells' portal frame), and one made with its
 * type only (Iron's Spells' statues) had no such constructor: both were lost when their chunk loaded.
 */
public final class BlockEntityDefaults {
	private static final ClassValue<BlockEntityType<?>> TYPES = new ClassValue<BlockEntityType<?>>() {
		@Override
		protected BlockEntityType<?> computeValue(Class<?> tile) {
			for (Object value : io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.util.Registries.BLOCK_ENTITY_TYPE)) {
				if (value instanceof BlockEntityType<?> type && type.getTileClass() == tile) {
					return type;
				}
			}
			return null;
		}
	};

	private BlockEntityDefaults() {
	}

	/** The type registered for the block entity class (its class is known once the type made one), else null. */
	@Nullable
	public static BlockEntityType<?> type(Class<?> tile) {
		BlockEntityType<?> type = TYPES.get(tile);
		if (type == null) {
			// not known when first asked (the type's class registered later): asked again next time
			TYPES.remove(tile);
		}
		return type;
	}

	/**
	 * 1.12.2's {@code TileEntity.create} (a saved block entity, made when its chunk loads) makes one with the class's
	 * no-argument constructor (the coremod's TileEntityCreate calls this instead): a class without one is made by the
	 * factory of its registered type at the origin with its block's default state, then reads its data as before. The
	 * converter gives a no-argument constructor only to the (pos, state) and (type, pos, state) shapes: every Refined
	 * Storage block entity (a controller takes its own controller type) was skipped and lost on every reload.
	 */
	public static Object newInstance(Class<?> tile) throws InstantiationException, IllegalAccessException {
		try {
			return tile.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			BlockEntityType<?> type = type(tile);
			IBlockState state = state(tile);
			Object made = type == null || state == null ? null : type.create(net.minecraft.util.math.BlockPos.ORIGIN, state);
			if (made == null) {
				throw e;
			}
			return made;
		}
	}

	/** The default state of the type's first block, else null. */
	@Nullable
	public static IBlockState state(Class<?> tile) {
		BlockEntityType<?> type = type(tile);
		if (type == null) {
			return null;
		}
		for (Block block : type.getValidBlocks()) {
			return block.getDefaultState();
		}
		return null;
	}
}
