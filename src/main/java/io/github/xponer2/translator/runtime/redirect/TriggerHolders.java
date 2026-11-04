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
package io.github.xponer2.translator.runtime.redirect;

import net.minecraft.advancements.critereon.PlacedBlockTrigger;

/**
 * 1.21 {@code CriteriaTriggers} 1.12.2 lacks. ITEM_USED_ON_BLOCK (an item used on a block: waxing, cutting, feeding)
 * is an ItemUsedOnLocationTrigger like PLACED_BLOCK, which the tables give 1.12.2's PlacedBlockTrigger: here one no
 * advancement listens to, so the mods' call (in their item's useOn) runs and awards nothing instead of throwing.
 */
public final class TriggerHolders {
	public static final PlacedBlockTrigger ITEM_USED_ON_BLOCK = new PlacedBlockTrigger();

	private TriggerHolders() {
	}
}
