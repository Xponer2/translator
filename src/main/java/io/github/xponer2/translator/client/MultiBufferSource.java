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
 * The 1.21 buffers by render type ({@code MultiBufferSource}). 1.12.2 draws immediately, so a buffer only tells the
 * drawing helpers which render state to use. Client only.
 */
public final class MultiBufferSource {
    public static final MultiBufferSource INSTANCE = new MultiBufferSource();

    private MultiBufferSource() { }

    private final java.util.Map<RenderType, VertexConsumer> consumers = new java.util.LinkedHashMap<>();

    public VertexConsumer getBuffer(RenderType type) {
        return this.consumers.computeIfAbsent(type, VertexConsumer::new);
    }

    /** Draws what the consumers got (the renderer adapters call it after each render, 1.21 endBatch). */
    public void endBatch() {
        for (var consumer : this.consumers.values()) {
            consumer.flush();
        }
    }

    // the name table maps getBuffer to this 1.12.2 name
    public VertexConsumer getVertexBufferByLayer(RenderType type) {
        return this.getBuffer(type);
    }
}
