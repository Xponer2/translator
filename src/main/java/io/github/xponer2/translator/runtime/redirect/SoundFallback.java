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

import net.minecraft.block.SoundType;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundEvent;

/**
 * A 1.21 sound 1.12.2 doesn't have (SoundEvents.LANTERN_PLACE, ...): the 1.12.2 sound of the same kind, so a block
 * places, breaks and steps with a sound instead of a null that stops the game (PositionedSoundRecord). Used by the
 * converter for a missing SoundEvents field, and for the sound types of blocks.
 */
public final class SoundFallback {
	private SoundFallback() {
	}

	/** By the name of the 1.21 SoundEvents field: its last part says what kind of sound it is. */
	public static SoundEvent byName(String field) {
		String name = field.toUpperCase(java.util.Locale.ROOT);
		if (name.endsWith("_PLACE")) {
			return SoundEvents.BLOCK_STONE_PLACE;
		}
		if (name.endsWith("_BREAK")) {
			return SoundEvents.BLOCK_STONE_BREAK;
		}
		if (name.endsWith("_STEP")) {
			return SoundEvents.BLOCK_STONE_STEP;
		}
		if (name.endsWith("_HIT")) {
			return SoundEvents.BLOCK_STONE_HIT;
		}
		if (name.endsWith("_FALL")) {
			return SoundEvents.BLOCK_STONE_FALL;
		}
		return SoundEvents.UI_BUTTON_CLICK;
	}

	/** The holder of a sound (1.21 SoundEvents fields typed Holder or Holder.Reference). */
	public static io.github.xponer2.translator.registry.Registry.Reference<SoundEvent> reference(SoundEvent sound) {
		return io.github.xponer2.translator.util.BuiltInRegistries.SOUND_EVENT.createIntrusiveHolder(sound);
	}

	/** A sound type with every sound there: the missing ones (null) from stone. */
	public static SoundType complete(SoundType type) {
		if (type == null) {
			return SoundType.STONE;
		}
		if (type.getBreakSound() != null && type.getStepSound() != null && type.getPlaceSound() != null && type.getHitSound() != null
				&& type.getFallSound() != null) {
			return type;
		}
		SoundType stone = SoundType.STONE;
		return new SoundType(type.getVolume(), type.getPitch(), or(type.getBreakSound(), stone.getBreakSound()), or(type.getStepSound(), stone.getStepSound()),
				or(type.getPlaceSound(), stone.getPlaceSound()), or(type.getHitSound(), stone.getHitSound()), or(type.getFallSound(), stone.getFallSound()));
	}

	private static SoundEvent or(SoundEvent sound, SoundEvent fallback) {
		return sound != null ? sound : fallback;
	}
}
