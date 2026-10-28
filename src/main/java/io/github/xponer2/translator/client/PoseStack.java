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
