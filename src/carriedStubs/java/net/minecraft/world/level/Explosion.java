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
package net.minecraft.world.level;

/** Compile-time stand-in of 1.21's Explosion: only its BlockInteraction enum is carried (the class is 1.12.2's). */
public class Explosion {
	public enum BlockInteraction {
		KEEP,
		DESTROY,
		DESTROY_WITH_DECAY,
		TRIGGER_BLOCK
	}
}
