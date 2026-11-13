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

import net.minecraft.entity.item.EntityBoat;

/**
 * 1.21 {@code Boat.Type} members 1.12.2's boat types lack (R row): none of 1.12.2's six woods is a raft. Without it
 * the whole of 1.21's {@code LayerDefinitions.createRoots()} (every vanilla entity model, the boats' among them) was
 * left empty, and a mod renderer using a vanilla model (Iron's Spells' catacombs zombie, its magehunter) had none.
 */
public final class BoatTypeBridges {
	private BoatTypeBridges() {
	}

	public static boolean isRaft(EntityBoat.Type type) {
		return false;
	}

	/** 1.21 {@code boatType.getPlanks()}: the planks block (1.12.2 has one, its wood a property). */
	public static net.minecraft.block.Block getPlanks(net.minecraft.entity.item.EntityBoat.Type type) {
		return net.minecraft.init.Blocks.PLANKS;
	}
}
