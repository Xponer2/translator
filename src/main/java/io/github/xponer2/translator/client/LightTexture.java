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
package io.github.xponer2.translator.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.DimensionType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code LightTexture}: the light map of the game renderer, which 1.12.2's entity renderer keeps
 * ({@code gameRenderer.lightTexture()} is this one). Mods turn it on around their own drawing (Twilight Forest's weather,
 * Ars Nouveau, Moonlight) and pack light values with its statics. Client only.
 */
@SideOnly(Side.CLIENT)
public final class LightTexture {
	public static final LightTexture INSTANCE = new LightTexture();
	public static final int FULL_BRIGHT = 15728880;
	public static final int FULL_SKY = 15728640;
	public static final int FULL_BLOCK = 240;

	private LightTexture() {
	}

	/** Block light and sky light (0 to 15) as one packed light value, the layout 1.12.2 uses too. */
	public static int pack(int block, int sky) {
		return block << 4 | sky << 20;
	}

	public static int block(int packed) {
		return (packed & 0xFFFF) >> 4;
	}

	public static int sky(int packed) {
		return packed >> 20 & 0xFFFF;
	}

	/** 1.21 {@code lightCoordsWithEmission(packed, emission)}: the block light raised to at least the emission. */
	public static int lightCoordsWithEmission(int packed, int emission) {
		if (emission == 0) {
			return packed;
		}
		return pack(Math.max(block(packed), emission), sky(packed));
	}

	/** The brightness of a light level in a dimension (1.21's curve; 1.12.2's dimensions have no ambient light but the nether's). */
	public static float getBrightness(@Nullable Object dimension, int level) {
		float fraction = level / 15.0F;
		float brightness = fraction / (4.0F - 3.0F * fraction);
		float ambient = dimension == DimensionType.NETHER ? 0.1F : 0.0F;
		return ambient + (1.0F - ambient) * brightness;
	}

	public void turnOnLightLayer() {
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
	}

	public void turnOffLightLayer() {
		Minecraft.getMinecraft().entityRenderer.disableLightmap();
	}

	/** 1.21's darkness effect (the Warden's): 1.12.2 has none. */
	public float getDarknessGamma(float partialTick) {
		return 0.0F;
	}

	public float calculateDarknessScale(Object entity, float gamma, float partialTick) {
		return 0.0F;
	}

	/** 1.21 {@code tick()} / {@code updateLightTexture}: 1.12.2's entity renderer updates its light map itself. */
	public void tick() {
	}

	public void updateLightTexture(float partialTick) {
	}
}
