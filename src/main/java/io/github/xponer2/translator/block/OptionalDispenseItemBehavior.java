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
package io.github.xponer2.translator.block;

import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;

/**
 * 1.21 dispenser behavior that may fail (then the dispenser plays the fail sound): subclasses implement
 * {@link #execute} and call {@link #setSuccess}.
 */
public abstract class OptionalDispenseItemBehavior extends BehaviorDefaultDispenseItem {
	private boolean success = true;

	public boolean isSuccess() {
		return this.success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	protected abstract ItemStack execute(BlockSource source, ItemStack stack);

	@Override
	protected final ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
		return this.execute(new BlockSource(source), stack);
	}

	@Override
	protected void playDispenseSound(IBlockSource source) {
		source.getWorld().playEvent(this.success ? 1000 : 1001, source.getBlockPos(), 0);
	}
}
