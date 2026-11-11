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

import net.minecraft.client.renderer.block.model.ItemTransformVec3f;

/**
 * Static stand-ins for the 1.21 item display transform ({@code ItemTransform}, 1.12.2's {@link ItemTransformVec3f}):
 * its vectors are JOML ones in 1.21 and LWJGL ones in 1.12.2 (neoforge-1.21.1-redirects.tsv). Client only.
 */
public final class ModelBridges {
	private ModelBridges() {
	}

	/** 1.21 {@code new ItemTransform(rotation, translation, scale)} (Sophisticated Storage's barrel item transforms). */
	public static ItemTransformVec3f newItemTransformVec3f(org.joml.Vector3f rotation, org.joml.Vector3f translation, org.joml.Vector3f scale) {
		return new ItemTransformVec3f(lwjgl(rotation), lwjgl(translation), lwjgl(scale));
	}

	/** NeoForge's {@code new ItemTransform(rotation, translation, scale, rightRotation)}: the right rotation left out. */
	public static ItemTransformVec3f newItemTransformVec3f(org.joml.Vector3f rotation, org.joml.Vector3f translation, org.joml.Vector3f scale,
			org.joml.Vector3f rightRotation) {
		return newItemTransformVec3f(rotation, translation, scale);
	}

	// the record's fields, read by renderers that apply a transform themselves (Iron's Spells' staffs, display items)

	public static org.joml.Vector3f rotation(ItemTransformVec3f transform) {
		return joml(transform.rotation);
	}

	public static org.joml.Vector3f translation(ItemTransformVec3f transform) {
		return joml(transform.translation);
	}

	public static org.joml.Vector3f scale(ItemTransformVec3f transform) {
		return joml(transform.scale);
	}

	private static org.lwjgl.util.vector.Vector3f lwjgl(org.joml.Vector3f vector) {
		return vector == null ? new org.lwjgl.util.vector.Vector3f() : new org.lwjgl.util.vector.Vector3f(vector.x, vector.y, vector.z);
	}

	private static org.joml.Vector3f joml(org.lwjgl.util.vector.Vector3f vector) {
		return vector == null ? new org.joml.Vector3f() : new org.joml.Vector3f(vector.x, vector.y, vector.z);
	}
}
