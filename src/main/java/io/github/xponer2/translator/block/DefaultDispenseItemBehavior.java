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
import net.minecraft.dispenser.IPosition;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * 1.21 {@code DefaultDispenseItemBehavior} (neoforge-1.21.1-runtime.tsv): the 1.12.2 one, its hooks (dispenseStack,
 * playDispenseSound, spawnDispenseParticles) calling the 1.21 ones a mod overrides (execute, playSound, playAnimation),
 * which default to the 1.12.2 behavior (drop the item, click, smoke).
 */
public class DefaultDispenseItemBehavior extends BehaviorDefaultDispenseItem implements DispenseItemBehavior {
	@Override
	public ItemStack dispense(BlockSource source, ItemStack stack) {
		return this.dispense(source.source(), stack);
	}

	protected ItemStack execute(BlockSource source, ItemStack stack) {
		return super.dispenseStack(source.source(), stack);
	}

	protected void playSound(BlockSource source) {
		super.playDispenseSound(source.source());
	}

	protected void playAnimation(BlockSource source, EnumFacing facing) {
		super.spawnDispenseParticles(source.source(), facing);
	}

	public static void spawnItem(World level, ItemStack stack, int speed, EnumFacing facing, IPosition position) {
		BehaviorDefaultDispenseItem.doDispense(level, stack, speed, facing, position);
	}

	/** 1.21 overload: the position is a Vec3d here (1.21 Position). */
	public static void spawnItem(World level, ItemStack stack, int speed, EnumFacing facing, net.minecraft.util.math.Vec3d position) {
		BehaviorDefaultDispenseItem.doDispense(level, stack, speed, facing, new net.minecraft.dispenser.PositionImpl(position.x, position.y, position.z));
	}

	@Override
	protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
		return this.execute(new BlockSource(source), stack);
	}

	@Override
	protected void playDispenseSound(IBlockSource source) {
		this.playSound(new BlockSource(source));
	}

	@Override
	protected void spawnDispenseParticles(IBlockSource source, EnumFacing facing) {
		this.playAnimation(new BlockSource(source), facing);
	}
}
