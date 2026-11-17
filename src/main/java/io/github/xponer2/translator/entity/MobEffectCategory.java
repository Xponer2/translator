/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.entity;

/** The kind of an effect (1.21 {@code MobEffectCategory}, neoforge-1.21.1-runtime.tsv). */
public enum MobEffectCategory {
	BENEFICIAL,
	HARMFUL,
	NEUTRAL;

	public net.minecraft.util.text.TextFormatting getTooltipFormatting() {
		return this == HARMFUL ? net.minecraft.util.text.TextFormatting.RED : net.minecraft.util.text.TextFormatting.BLUE;
	}

	public static MobEffectCategory of(net.minecraft.potion.Potion potion) {
		return potion.isBadEffect() ? HARMFUL : potion.isBeneficial() ? BENEFICIAL : NEUTRAL;
	}
}
