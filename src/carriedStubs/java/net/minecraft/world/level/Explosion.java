/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
