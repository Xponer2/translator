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

import net.minecraft.client.renderer.GlStateManager;

/**
 * The 1.21 matrix stack on the 1.12.2 OpenGL matrix: every operation is applied to the current GL model view matrix
 * at once ({@code pushPose} = {@code pushMatrix}, ...). Client only.
 */
public final class PoseStack {
    public static final PoseStack INSTANCE = new PoseStack();

    public void pushPose() {
        GlStateManager.pushMatrix();
    }

    public void popPose() {
        GlStateManager.popMatrix();
    }

    public void translate(double x, double y, double z) {
        GlStateManager.translate(x, y, z);
    }

    public void translate(float x, float y, float z) {
        GlStateManager.translate(x, y, z);
    }

    public void scale(float x, float y, float z) {
        GlStateManager.scale(x, y, z);
    }

    public void mulPose(Axis.Rotation rotation) {
        GlStateManager.rotate(rotation.degrees(), rotation.x(), rotation.y(), rotation.z());
    }

    public void setIdentity() {
        GlStateManager.loadIdentity();
    }

    /**
     * 1.21 {@code last()}: the current matrix, which is the GL one here.
     */
    /** 1.21 Pose#pose (the matrix): the GL matrix is current anyway. */
    public PoseStack pose() {
        return this;
    }

    public PoseStack last() {
        return this;
    }
}
