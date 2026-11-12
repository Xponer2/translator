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
public class MultiBufferSource {
    /** Every buffer source made: they share the 1.12.2 Tessellator. */
    private static final java.util.Set<MultiBufferSource> SOURCES = java.util.Collections.synchronizedSet(
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));

    public static final MultiBufferSource INSTANCE = new MultiBufferSource();

    private MultiBufferSource() {
        SOURCES.add(this);
    }

    /**
     * 1.21 {@code new MultiBufferSource.BufferSource(sharedBuffer, fixedBuffers)}: a mod's own buffer source (its subclass
     * wraps the buffers: Patchouli's ghost multiblocks, GuideMe's scene export), batches of its own drawn when it ends
     * them. As a final class it failed to load and the mods' classes with it.
     */
    protected MultiBufferSource(@javax.annotation.Nullable Object sharedBuffer, @javax.annotation.Nullable java.util.Map<?, ?> fixedBuffers) {
        SOURCES.add(this);
    }

    private final java.util.Map<RenderType, BufferVertexConsumer> consumers = new java.util.LinkedHashMap<>();

    public VertexConsumer getBuffer(RenderType type) {
        return this.consumers.computeIfAbsent(type, BufferVertexConsumer::new);
    }

    /** Draws what the consumers got (the renderer adapters call it after each render, 1.21 endBatch). */
    /** Draws a render type's batch: every batch is drawn (they are immediate here). */
    public void endBatch(RenderType type) {
        this.endBatch();
    }

    public void endLastBatch() {
        this.endBatch();
    }

    public void endBatch() {
        for (var consumer : this.consumers.values()) {
            try {
                consumer.flush();
            } catch (RuntimeException e) {
                // a batch that can't be drawn doesn't take the frame down: dropped, the Tessellator left clean
                io.github.xponer2.translator.runtime.RuntimeStubs.failed("MultiBufferSource.endBatch " + consumer.type(), e);
                this.abandon();
                return;
            }
        }
    }

    /** Draws the other started batches: they share the 1.12.2 Tessellator with the one starting. */
    void flushOthers(VertexConsumer starting) {
        for (var consumer : this.consumers.values()) {
            if (consumer != starting) {
                consumer.flush();
            }
        }
    }

    /** The other started batches of every buffer source (a mod's own ones too) drawn: the Tessellator is one. */
    static void flushAllOthers(VertexConsumer starting) {
        MultiBufferSource[] sources;
        synchronized (SOURCES) {
            sources = SOURCES.toArray(new MultiBufferSource[0]);
        }
        for (MultiBufferSource source : sources) {
            source.flushOthers(starting);
        }
    }

    /** Every batch dropped and the shared Tessellator finished and emptied. */
    private void abandon() {
        for (var consumer : this.consumers.values()) {
            consumer.abandon();
        }
        var buffer = net.minecraft.client.renderer.Tessellator.getInstance().getBuffer();
        if (BufferVertexConsumer.drawing(buffer)) {
            buffer.finishDrawing();
        }
        buffer.reset();
    }

    // the name table maps getBuffer to this 1.12.2 name
    public VertexConsumer getVertexBufferByLayer(RenderType type) {
        return this.getBuffer(type);
    }
}
