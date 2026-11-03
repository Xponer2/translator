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
}
