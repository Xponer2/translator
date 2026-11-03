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

import io.github.xponer2.translator.block.BlockSource;
import io.github.xponer2.translator.block.DispenseItemBehavior;
import io.github.xponer2.translator.util.ItemLike;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IPosition;
import net.minecraft.init.Items;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

/**
 * The 1.21 dispenser calls (DispenserBlock, ArmorItem) on the 1.12.2 dispenser registry
 * (neoforge-1.21.1-redirects.tsv).
 */
public final class DispenserBridges {
	/** 1.21 {@code ArmorItem.DISPENSE_ITEM_BEHAVIOR}: what a dispenser does with armor (puts it on the entity in front). */
	public static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = (source, stack) -> BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY
			.getObject(Items.IRON_CHESTPLATE).dispense(source.source(), stack);

	private DispenserBridges() {
	}

	public static void registerBehavior(Object item, IBehaviorDispenseItem behavior) {
		BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(ItemLike.item(item), behavior);
	}

	/** The point in front of the dispenser (a 1.21 Position, a Vec3d here). */
	public static Vec3d getDispensePosition(BlockSource source) {
		IPosition position = BlockDispenser.getDispensePosition(source.source());
		return new Vec3d(position.getX(), position.getY(), position.getZ());
	}

	public static Vec3d getDispensePosition(BlockSource source, double offset) {
		EnumFacing facing = source.state().getValue(BlockDispenser.FACING);
		return source.center().add(offset * facing.getXOffset(), offset * facing.getYOffset(), offset * facing.getZOffset());
	}
}
