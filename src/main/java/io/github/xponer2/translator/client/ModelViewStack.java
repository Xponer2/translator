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

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code RenderSystem.setProjectionMatrix} and {@code getModelViewStack()} (a JOML stack) as 1.12.2's GL matrices
 * (X lines of the runtime table). They went to the carried RenderSystem's own copies, which 1.12.2 never draws with:
 * JourneyMap sets a projection in window pixels for its maps, kept the GUI one, and drew every map tile twice as large
 * and off the screen.
 */
@SideOnly(Side.CLIENT)
public final class ModelViewStack {
	private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(16);
	private static final Stack STACK = new Stack();

	private ModelViewStack() {
	}

	/** 1.21 {@code RenderSystem.setProjectionMatrix(matrix, sorting)}: the GL projection. */
	public static void setProjectionMatrix(Matrix4f matrix, Object sorting) {
		BUFFER.clear();
		matrix.get(BUFFER);
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GL11.glLoadMatrix(BUFFER);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
	}

	/**
	 * 1.21 {@code RenderSystem.getModelViewStack()}: a stack holding the GL model view matrix when a mod takes it up,
	 * loaded into GL by {@link #applyModelViewMatrix()}.
	 */
	public static Matrix4fStack getModelViewStack() {
		if (STACK.depth == 0) {
			BUFFER.clear();
			GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, BUFFER);
			STACK.set(BUFFER);
		}
		STACK.active = true;
		return STACK;
	}

	/** 1.21 {@code RenderSystem.applyModelViewMatrix()}: the stack as the GL model view, when a mod changed it. */
	public static void applyModelViewMatrix() {
		if (!STACK.active) {
			return;
		}
		BUFFER.clear();
		STACK.get(BUFFER);
		GL11.glLoadMatrix(BUFFER);
		if (STACK.depth == 0) {
			STACK.active = false;
		}
	}

	private static final class Stack extends Matrix4fStack {
		int depth;
		boolean active;

		Stack() {
			super(32);
		}

		@Override
		public Matrix4fStack pushMatrix() {
			this.depth++;
			return super.pushMatrix();
		}

		@Override
		public Matrix4fStack popMatrix() {
			this.depth = Math.max(0, this.depth - 1);
			return super.popMatrix();
		}
	}
}
