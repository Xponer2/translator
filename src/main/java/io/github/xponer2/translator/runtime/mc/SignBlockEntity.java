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
package io.github.xponer2.translator.runtime.mc;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.BlockEntityType;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;

/**
 * 1.21 {@code SignBlockEntity} (and the hanging sign's, a subclass): 1.12.2's sign block entity, made with the 1.21
 * constructors (a mod's sign block entity calls super(type, pos, state): 1.12.2's has none, so no mod sign had its block
 * entity and none was drawn). Its text is 1.12.2's; {@code client.SignRenderers} draws it with the wood's texture.
 */
public class SignBlockEntity extends TileEntitySign {
	@Nullable
	private final BlockEntityType<?> type;

	/** 1.12.2 makes a saved one with this. */
	public SignBlockEntity() {
		this.type = null;
	}

	public SignBlockEntity(BlockPos pos, IBlockState state) {
		this(null, pos, state);
	}

	public SignBlockEntity(@Nullable BlockEntityType<?> type, BlockPos pos, IBlockState state) {
		this.type = type;
		this.setPos(pos.toImmutable());
	}

	@Nullable
	public BlockEntityType<?> getType() {
		return this.type;
	}
}
