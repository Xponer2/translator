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

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * What sends a menu's contents to its player's client (1.21 {@code ContainerSynchronizer}): the whole menu when it
 * opens, then each slot, the carried stack and each data slot that changed. A menu gets one from its player
 * ({@link AbstractContainerMenu#setSynchronizer}, {@link PlayerSynchronizer}); a mod may wrap it (Sophisticated Core's
 * synchronizer sends stacks of more than 255 items by its own packets, 1.12.2's write the count as a byte).
 */
public interface ContainerSynchronizer {
	void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> items, ItemStack carried, int[] data);

	void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack);

	void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack);

	void sendDataChange(AbstractContainerMenu menu, int id, int value);
}
