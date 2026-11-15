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
     * them. As a final class it failed to load and the mods' classes with it. Public: the API's own
     * {@code MultiBufferSource.immediate(buffer)} makes one too (Ars Nouveau's GeckoLib renderers in their static
     * fields: an IllegalAccessError left the Starbuncle, the familiars and the animated blocks without a renderer).
     */
    public MultiBufferSource(@javax.annotation.Nullable Object sharedBuffer, @javax.annotation.Nullable java.util.Map<?, ?> fixedBuffers) {
        SOURCES.add(this);
    }

    /**
     * The consumer of each render type asked for. Mods make render types as they draw (JourneyMap's minimap: a new one
     * for every tile and label of every frame): kept here, they were 28,000 after five minutes, and every draw went
     * through all of them (the game at a few frames a second, worse the longer it ran, better with F3 hiding the map).
     * The ones without a started batch are let go when there are many; a draw looks at the started ones only.
     */
    private final java.util.Map<RenderType, BufferVertexConsumer> consumers = new java.util.LinkedHashMap<>();
    /** The consumers with a batch started since they were last drawn. */
    private final java.util.Set<BufferVertexConsumer> active = new java.util.LinkedHashSet<>();
    private static final int KEPT = 256;

    public VertexConsumer getBuffer(RenderType type) {
        var consumer = this.consumers.get(type);
        if (consumer == null) {
            if (this.consumers.size() >= KEPT) {
                this.consumers.values().removeIf(idle -> !this.active.contains(idle));
            }
            consumer = new BufferVertexConsumer(type, this);
            this.consumers.put(type, consumer);
        }
        return consumer;
    }

    /** How many render type consumers the shared source keeps (the self-test's play check: they must not pile up). */
    public static int kept() {
        return INSTANCE.consumers.size();
    }

    /** A consumer of this source started a batch (one that was let go and is written to again too). */
    void started(BufferVertexConsumer consumer) {
        this.active.add(consumer);
    }

    private BufferVertexConsumer[] takeActive(@javax.annotation.Nullable VertexConsumer except) {
        var taken = new java.util.ArrayList<BufferVertexConsumer>(this.active.size());
        for (var consumer : this.active) {
            if (consumer != except) {
                taken.add(consumer);
            }
        }
        this.active.removeAll(taken);
        return taken.toArray(new BufferVertexConsumer[0]);
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
        if (this.active.isEmpty()) {
            return;
        }
        for (var consumer : this.takeActive(null)) {
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
        if (this.active.isEmpty() || this.active.size() == 1 && this.active.contains(starting)) {
            return;
        }
        for (var consumer : this.takeActive(starting)) {
            consumer.flush();
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
        for (var consumer : this.takeActive(null)) {
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
