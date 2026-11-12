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

import net.minecraftforge.fml.common.thread.SidedThreadGroup;
import net.minecraftforge.fml.relauncher.Side;

/**
 * NeoForge's sided thread groups on Forge 1.12.2's (the table maps the classes: the server thread runs in Forge's SERVER
 * group): {@code SidedThreadGroup.getSide()} is NeoForge's LogicalSide (carried with the API), Forge's returns a Side.
 * EffectiveSide.get() of the carried NeoForge reads it (Mekanism, Create, JourneyMap, Modular Routers).
 */
public final class ThreadSides {
	private ThreadSides() {
	}

	/** The group's side as NeoForge's LogicalSide. */
	public static Object getSide(SidedThreadGroup group) {
		return logicalSide(group.getSide() == Side.SERVER ? "SERVER" : "CLIENT");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object logicalSide(String name) {
		try {
			return Enum.valueOf((Class) Class.forName("net.neoforged.fml.LogicalSide", true, net.minecraft.launchwrapper.Launch.classLoader), name);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
