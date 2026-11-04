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
package io.github.xponer2.translator.client.model;

/** 1.21 {@code PartPose}: where a model part is and how it is turned (pixels, radians). */
public class PartPose {
	public static final PartPose ZERO = offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
	public final float x;
	public final float y;
	public final float z;
	public final float xRot;
	public final float yRot;
	public final float zRot;

	private PartPose(float x, float y, float z, float xRot, float yRot, float zRot) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.xRot = xRot;
		this.yRot = yRot;
		this.zRot = zRot;
	}

	public static PartPose offset(float x, float y, float z) {
		return offsetAndRotation(x, y, z, 0.0F, 0.0F, 0.0F);
	}

	public static PartPose rotation(float xRot, float yRot, float zRot) {
		return offsetAndRotation(0.0F, 0.0F, 0.0F, xRot, yRot, zRot);
	}

	public static PartPose offsetAndRotation(float x, float y, float z, float xRot, float yRot, float zRot) {
		return new PartPose(x, y, z, xRot, yRot, zRot);
	}
}
