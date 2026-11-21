/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Arguments of 1.20.5's level rendering made from 1.20.4's (the A rows' plans). 1.20.5 passes the camera's rotation as
 * a frustum matrix where 1.20.4 passes a PoseStack holding that rotation (GameRenderer.renderLevel turns its pose stack
 * by the camera's pitch and yaw before LevelRenderer.renderLevel; 1.20.5 makes the same rotation from the camera's
 * quaternion and makes a fresh PoseStack of it where it still needs one).
 */
public final class RenderArgs {
    private RenderArgs() {
    }

    /** the frustum matrix of a level render: the pose stack's current pose (the matrix itself, as 1.20.5 passes its own) */
    public static Matrix4f frustum(PoseStack poseStack) {
        return poseStack.last().pose();
    }

    /** the frame's partial tick (1.21's render methods take it, 1.20.4's of the same name don't) */
    public static float partialTick() {
        return net.minecraft.client.Minecraft.getInstance().getPartialTick();
    }
}
