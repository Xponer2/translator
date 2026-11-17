/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.util;

import net.minecraft.util.EnumActionResult;

/**
 * 1.21 result of using an item on a block (Block#useItemOn). The converter's Block#onBlockActivated override falls
 * back to useWithoutItem for {@link #PASS_TO_DEFAULT_BLOCK_INTERACTION}.
 */
public enum ItemInteractionResult {
	SUCCESS,
	CONSUME,
	CONSUME_PARTIAL,
	PASS_TO_DEFAULT_BLOCK_INTERACTION,
	SKIP_DEFAULT_BLOCK_INTERACTION,
	FAIL;

	public boolean consumesAction() {
		return this == SUCCESS || this == CONSUME || this == CONSUME_PARTIAL;
	}

	public EnumActionResult result() {
		return switch (this) {
			case SUCCESS, CONSUME, CONSUME_PARTIAL -> EnumActionResult.SUCCESS;
			case FAIL -> EnumActionResult.FAIL;
			default -> EnumActionResult.PASS;
		};
	}

	public static ItemInteractionResult sidedSuccess(boolean clientSide) {
		return clientSide ? SUCCESS : CONSUME;
	}
}
