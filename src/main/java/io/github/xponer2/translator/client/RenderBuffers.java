/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

/**
 * 1.21 {@code Minecraft.renderBuffers()}: the buffers a renderer draws into, which are the Translator's one
 * immediate buffer source on 1.12.2.
 */
public final class RenderBuffers {
    public static final RenderBuffers INSTANCE = new RenderBuffers();

    private RenderBuffers() {
    }

    public MultiBufferSource bufferSource() {
        return MultiBufferSource.INSTANCE;
    }

    public MultiBufferSource crumblingBufferSource() {
        return MultiBufferSource.INSTANCE;
    }

    /** The buffers of glowing entities' outlines (1.12.2 draws the outlines itself). */
    public OutlineBufferSource outlineBufferSource() {
        return OutlineBufferSource.INSTANCE;
    }
}
