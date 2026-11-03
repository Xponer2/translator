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

import java.nio.FloatBuffer;

import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;

import io.github.xponer2.translator.client.Axis;
import io.github.xponer2.translator.client.PoseStack;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The 1.21 render math (JOML quaternions and matrices) on the Translator's PoseStack, which is the 1.12.2 GL matrix
 * stack (runtime, neoforge-1.21.1-redirects.tsv): {@code poseStack.mulPose(Axis.YP.rotationDegrees(90))} rotates the
 * GL matrix. A Pose (PoseStack.Pose is the PoseStack here) has identity matrices: the GL matrix already transforms
 * every vertex. JOML is the player's Minecraft 1.21.1 library, there whenever 1.21.1 mods are.
 */
@SideOnly(Side.CLIENT)
public final class RenderBridges {
	private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

	private RenderBridges() {
	}

	// --- Axis --------------------------------------------------------------------------------------------------------

	public static Quaternionf rotationDegrees(Axis axis, float degrees) {
		Axis.Rotation rotation = axis.rotationDegrees(degrees);
		return new Quaternionf().rotationAxis((float) Math.toRadians(rotation.degrees()), rotation.x(), rotation.y(), rotation.z());
	}

	public static Quaternionf rotation(Axis axis, float radians) {
		Axis.Rotation rotation = axis.rotation(radians);
		return new Quaternionf().rotationAxis((float) Math.toRadians(rotation.degrees()), rotation.x(), rotation.y(), rotation.z());
	}

	// --- PoseStack ---------------------------------------------------------------------------------------------------

	public static void mulPose(PoseStack stack, Quaternionf rotation) {
		var angle = new AxisAngle4f(rotation);
		if (angle.angle != 0F) {
			GlStateManager.rotate((float) Math.toDegrees(angle.angle), angle.x, angle.y, angle.z);
		}
	}

	public static void mulPose(PoseStack stack, Matrix4f matrix) {
		synchronized (MATRIX) {
			MATRIX.clear();
			matrix.get(MATRIX);
			MATRIX.rewind();
			GlStateManager.multMatrix(MATRIX);
		}
	}

	public static void rotateAround(PoseStack stack, Quaternionf rotation, float x, float y, float z) {
		GlStateManager.translate(x, y, z);
		mulPose(stack, rotation);
		GlStateManager.translate(-x, -y, -z);
	}

	public static boolean clear(PoseStack stack) {
		return true;
	}

	/** {@code last().pose()}: identity, the GL matrix transforms the vertices. */
	public static Matrix4f pose(PoseStack stack) {
		return new Matrix4f();
	}

	public static Matrix3f normal(PoseStack stack) {
		return new Matrix3f();
	}

	public static PoseStack copy(PoseStack stack) {
		return stack;
	}
}
