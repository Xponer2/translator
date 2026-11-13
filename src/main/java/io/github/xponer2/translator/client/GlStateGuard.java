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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The GL state around a 1.21.1 mod's render listener (HUD, screen, world): what it was before, put back after. 1.21
 * code sets its render state and leaves it (1.21 sets everything before each draw); 1.12.2 code expects what it left.
 * JourneyMap's minimap left the texture matrix, blend and depth changed, and everything drawn after it on the frame
 * broke (the chat in fragments, the HUD). The 1.12.2 GlStateManager caches its state: each switch is set through it
 * twice (the other way, then the saved way), so the cache and GL agree again even after raw GL calls.
 */
@SideOnly(Side.CLIENT)
public final class GlStateGuard {
	private static final FloatBuffer COLOR = BufferUtils.createFloatBuffer(16);
	private static final ByteBuffer MASK = BufferUtils.createByteBuffer(16);
	// one guard runs at a time (nested ones run their body as it is): its saved matrices
	private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
	private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
	private static final FloatBuffer TEXTURE = BufferUtils.createFloatBuffer(16);
	private static int nesting;

	private GlStateGuard() {
	}

	public static void run(Runnable body) {
		if (nesting > 0 || !Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
			body.run();
			return;
		}
		nesting++;
		try {
			guarded(body);
		} finally {
			nesting--;
		}
	}

	private static void guarded(Runnable body) {
		int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
		// the matrices by value, not pushed: the texture and projection stacks may hold only 2 (a mod pushing too overflowed)
		int modelview = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
		int projection = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
		int texture = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
		FloatBuffer modelviewMatrix = matrix(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
		FloatBuffer projectionMatrix = matrix(GL11.GL_PROJECTION_MATRIX, PROJECTION);
		FloatBuffer textureMatrix = matrix(GL11.GL_TEXTURE_MATRIX, TEXTURE);

		boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
		boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
		boolean rescale = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
		boolean colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
		boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
		boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		boolean texture2d = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
		int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		int blendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
		int blendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);
		boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
		int alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
		float alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
		int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
		COLOR.clear();
		GL11.glGetFloat(GL11.GL_CURRENT_COLOR, COLOR);
		float red = COLOR.get(0);
		float green = COLOR.get(1);
		float blue = COLOR.get(2);
		float alphaColor = COLOR.get(3);
		MASK.clear();
		GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, MASK);
		boolean maskRed = MASK.get(0) != 0;
		boolean maskGreen = MASK.get(1) != 0;
		boolean maskBlue = MASK.get(2) != 0;
		boolean maskAlpha = MASK.get(3) != 0;
		int poses = PoseStack.depth();
		try {
			body.run();
		} finally {
			// the GL stacks come back to their depth below: so does the count of the 1.21 poses pushed (a handler that
			// failed after pushPose left it one up, and the frame's end then popped a matrix of 1.12.2's own: stack
			// underflows every frame, the world's entities drawn off their place, only their shadows seen)
			PoseStack.resetDepth(poses);
			restore(GL11.GL_TEXTURE, GL11.GL_TEXTURE_STACK_DEPTH, texture, textureMatrix);
			restore(GL11.GL_PROJECTION, GL11.GL_PROJECTION_STACK_DEPTH, projection, projectionMatrix);
			restore(GL11.GL_MODELVIEW, GL11.GL_MODELVIEW_STACK_DEPTH, modelview, modelviewMatrix);
			GlStateManager.matrixMode(mode);

			GlStateManager.setActiveTexture(activeTexture == GL13.GL_TEXTURE0 ? GL13.GL_TEXTURE1 : GL13.GL_TEXTURE0);
			GlStateManager.setActiveTexture(activeTexture);
			set(texture2d, GlStateManager::enableTexture2D, GlStateManager::disableTexture2D);
			GlStateManager.bindTexture(boundTexture == 0 ? net.minecraft.client.renderer.texture.TextureUtil.MISSING_TEXTURE.getGlTextureId() : 0);
			GlStateManager.bindTexture(boundTexture);
			set(blend, GlStateManager::enableBlend, GlStateManager::disableBlend);
			set(depth, GlStateManager::enableDepth, GlStateManager::disableDepth);
			set(alpha, GlStateManager::enableAlpha, GlStateManager::disableAlpha);
			set(cull, GlStateManager::enableCull, GlStateManager::disableCull);
			set(lighting, GlStateManager::enableLighting, GlStateManager::disableLighting);
			set(fog, GlStateManager::enableFog, GlStateManager::disableFog);
			set(rescale, GlStateManager::enableRescaleNormal, GlStateManager::disableRescaleNormal);
			set(colorMaterial, GlStateManager::enableColorMaterial, GlStateManager::disableColorMaterial);
			if (scissor) {
				GL11.glEnable(GL11.GL_SCISSOR_TEST);
			} else {
				GL11.glDisable(GL11.GL_SCISSOR_TEST);
			}
			if (stencil) {
				GL11.glEnable(GL11.GL_STENCIL_TEST);
			} else {
				GL11.glDisable(GL11.GL_STENCIL_TEST);
			}
			GlStateManager.blendFunc(blendSrc == GL11.GL_ONE ? GL11.GL_ZERO : GL11.GL_ONE, blendDst == GL11.GL_ONE ? GL11.GL_ZERO : GL11.GL_ONE);
			GlStateManager.blendFunc(blendSrc, blendDst);
			GlStateManager.depthMask(!depthMask);
			GlStateManager.depthMask(depthMask);
			GlStateManager.depthFunc(depthFunc == GL11.GL_LEQUAL ? GL11.GL_LESS : GL11.GL_LEQUAL);
			GlStateManager.depthFunc(depthFunc);
			GlStateManager.alphaFunc(alphaFunc == GL11.GL_GREATER ? GL11.GL_ALWAYS : GL11.GL_GREATER, alphaRef);
			GlStateManager.alphaFunc(alphaFunc, alphaRef);
			GlStateManager.shadeModel(shadeModel == GL11.GL_SMOOTH ? GL11.GL_FLAT : GL11.GL_SMOOTH);
			GlStateManager.shadeModel(shadeModel);
			GlStateManager.colorMask(!maskRed, !maskGreen, !maskBlue, !maskAlpha);
			GlStateManager.colorMask(maskRed, maskGreen, maskBlue, maskAlpha);
			GlStateManager.color(red == 0.5F ? 0.25F : 0.5F, green, blue, alphaColor);
			GlStateManager.color(red, green, blue, alphaColor);
		}
	}

	private static FloatBuffer matrix(int which, FloatBuffer values) {
		values.clear();
		GL11.glGetFloat(which, values);
		values.rewind();
		return values;
	}

	/** Back to the depth before (what the mod pushed and left is popped), then the matrix it had. */
	private static void restore(int mode, int depthQuery, int depthBefore, FloatBuffer values) {
		GL11.glMatrixMode(mode);
		for (int depth = GL11.glGetInteger(depthQuery); depth > depthBefore; depth--) {
			GL11.glPopMatrix();
		}
		values.rewind();
		GL11.glLoadMatrix(values);
	}

	private static void set(boolean enabled, Runnable enable, Runnable disable) {
		if (enabled) {
			disable.run();
			enable.run();
		} else {
			enable.run();
			disable.run();
		}
	}
}
