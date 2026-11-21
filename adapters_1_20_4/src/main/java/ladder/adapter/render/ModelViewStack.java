/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * 1.20.5's {@code RenderSystem.getModelViewStack()} (a JOML matrix stack, its current matrix the model view) on 1.20.4,
 * whose model view is the top of a pose stack (an X row). The stack handed to the mods and the game's are kept the
 * same at each {@code applyModelViewMatrix} (mixin.ModelViewApplyMixin): what a mod changed since the last time goes to
 * the game's, else the game's own changes come to the mods' (its pushed matrices stay its own).
 */
public final class ModelViewStack {
    private static final Matrix4fStack STACK = new Matrix4fStack(32);
    /** the matrix both had at the last sync */
    private static final Matrix4f SEEN = new Matrix4f();
    private static boolean handed;

    private ModelViewStack() {
    }

    public static Matrix4fStack get() {
        RenderSystem.assertOnRenderThread();
        if (!handed) {
            handed = true;
            take();
        } else if (SEEN.equals(STACK, 0.0F)) {
            // unchanged by the mods: the game's may have moved on
            take();
        }
        return STACK;
    }

    /** before the game applies its model view */
    public static void beforeApply() {
        if (!handed) {
            return;
        }
        if (SEEN.equals(STACK, 0.0F)) {
            take();
        } else {
            game().set(STACK);
            SEEN.set(STACK);
        }
    }

    private static void take() {
        STACK.set(game());
        SEEN.set(STACK);
    }

    private static Matrix4f game() {
        return RenderSystem.getModelViewStack().last().pose();
    }
}
