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
public class PoseStack {
    public static final PoseStack INSTANCE = new PoseStack();
    /**
     * Pushes not popped yet. Render code of a mod that fails between pushPose and popPose leaves its matrix on the GL
     * stack, which overflows after a few frames ("1283: Stack overflow", everything drawn shifted): the callers that
     * can see a failure pop back to their depth ({@link #restore}), the rest is popped at the end of the frame.
     */
    private static int depth;

    public void pushPose() {
        GlStateManager.pushMatrix();
        depth++;
    }

    public void popPose() {
        GlStateManager.popMatrix();
        if (depth > 0) {
            depth--;
        }
    }

    public static int depth() {
        return depth;
    }

    /** Pops the matrices pushed since the stack had that depth; how many there were. */
    public static int restore(int to) {
        int popped = 0;
        while (depth > to) {
            GlStateManager.popMatrix();
            depth--;
            popped++;
        }
        return popped;
    }

    /** At the end of every frame: what failed render code left pushed. */
    public static final class FrameEnd {
        private boolean reported;

        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onRenderTick(net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent event) {
            if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END || depth == 0) {
                return;
            }
            int popped = restore(0);
            if (!this.reported) {
                this.reported = true;
                org.apache.logging.log4j.LogManager.getLogger("Translator").warn(
                        "Render code of a 1.21.1 mod left {} matrices pushed (it failed before popPose): popped at the end of the frame", popped);
            }
        }
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

    /** 1.21 {@code mulPose(quaternion)} (billboards: {@code cameraOrientation()}): its axis and angle on the GL matrix. */
    public void mulPose(org.joml.Quaternionf rotation) {
        var axis = new org.joml.AxisAngle4f().set(rotation);
        if (axis.angle != 0.0F && (axis.x != 0.0F || axis.y != 0.0F || axis.z != 0.0F)) {
            GlStateManager.rotate((float) Math.toDegrees(axis.angle), axis.x, axis.y, axis.z);
        }
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
