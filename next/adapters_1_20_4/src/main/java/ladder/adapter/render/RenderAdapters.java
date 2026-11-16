/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package ladder.adapter.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import ladder.adapter.provided.DeltaTracker;
import ladder.adapter.provided.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.util.FastColor;

/**
 * 1.21's vertex API on 1.20.4 (receiver first). 1.21 begins a buffer with {@code Tesselator.begin}, ends each vertex
 * by the next {@code addVertex} and builds a {@code MeshData}; 1.20.4 has one builder per tesselator, needs
 * {@code endVertex()} after each vertex and ends into a {@code RenderedBuffer}. A vertex begun here is kept and
 * written whole later (see the vertices below; mixin.BufferBuilderMixin writes them before a buffer ends).
 */
public final class RenderAdapters {
    private RenderAdapters() {
    }

    // ------------------------------------------------------------------ the frame's time

    /** the client's own DeltaTracker: 1.20.4's timer values through its Minecraft */
    private static final DeltaTracker TIMER = new DeltaTracker() {
        @Override
        public float getGameTimeDeltaTicks() {
            return Minecraft.getInstance().getDeltaFrameTime();
        }

        @Override
        public float getGameTimeDeltaPartialTick(boolean runsNormally) {
            // 1.20.4's frame time is the paused partial tick while paused, as 1.21's timer gives it
            return Minecraft.getInstance().getFrameTime();
        }

        @Override
        public float getRealtimeDeltaTicks() {
            return Minecraft.getInstance().getDeltaFrameTime();
        }
    };

    /** 1.21's {@code Minecraft.getTimer()} */
    public static DeltaTracker getTimer(Minecraft minecraft) {
        return TIMER;
    }

    /** 1.21's {@code getPartialTick()} of the render events, which 1.20.4 gives as a float */
    public static DeltaTracker getPartialTick(RenderLevelStageEvent event) {
        return new DeltaTracker.PartialTick(event.getPartialTick());
    }

    public static DeltaTracker getPartialTick(RenderGuiEvent event) {
        return new DeltaTracker.PartialTick(event.getPartialTick());
    }

    // ------------------------------------------------------------------ buffers

    /** 1.21's {@code Tesselator.begin(mode, format)}: the tesselator's builder, begun */
    public static BufferBuilder begin(Tesselator tesselator, VertexFormat.Mode mode, VertexFormat format) {
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(mode, format);
        return builder;
    }

    /** 1.21's {@code BufferBuilder.buildOrThrow()}: an empty buffer is an error, as there */
    public static MeshData buildOrThrow(BufferBuilder builder) {
        MeshData mesh = build(builder);
        if (mesh == null) {
            throw new IllegalStateException("BufferBuilder was empty");
        }
        return mesh;
    }

    /** 1.21's {@code BufferBuilder.build()}: null when nothing was drawn */
    public static MeshData build(BufferBuilder builder) {
        BufferBuilder.RenderedBuffer buffer = builder.endOrDiscardIfEmpty();
        return buffer == null ? null : new MeshData(buffer);
    }

    /** 1.21's {@code BufferUploader.drawWithShader(MeshData)} */
    public static void drawWithShader(MeshData mesh) {
        BufferUploader.drawWithShader(mesh.buffer());
    }

    /** 1.21's {@code BufferUploader.draw(MeshData)} */
    public static void draw(MeshData mesh) {
        BufferUploader.draw(mesh.buffer());
    }

    /** 1.21's {@code VertexBuffer.upload(MeshData)} */
    public static void upload(VertexBuffer buffer, MeshData mesh) {
        buffer.upload(mesh.buffer());
    }

    // ------------------------------------------------------------------ vertices
    //
    // 1.21 sets a vertex's attributes in any order and writes it when the next one begins (or the buffer is built);
    // 1.20.4 takes them in its format's order and needs endVertex(). So a vertex begun here is kept, its attributes
    // gathered, and written whole (in the format's order, endVertex() after) when the next one begins on that
    // consumer, before the game writes a whole vertex, and before any buffer ends (a wrapper's vertex may go into it).

    /** a begun vertex's attributes */
    private static final class Vertex {
        float x, y, z;
        int r = 255, g = 255, b = 255, a = 255;
        float u, v;
        int u1, v1, u2, v2;
        float nx, ny = 1.0F, nz;
        boolean color, uv, uv1, uv2, normal;
    }

    /** consumer -> its begun vertex (identity: a consumer is its own key) */
    private static final Map<VertexConsumer, Vertex> PENDING = Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    /** writes the consumer's begun vertex, if any */
    public static void endVertex(VertexConsumer consumer) {
        Vertex vertex = PENDING.remove(consumer);
        if (vertex != null) {
            write(consumer, vertex);
        }
    }

    /** writes every begun vertex (a buffer ends: wrappers' vertices may go into it) */
    public static void endAll() {
        List<Map.Entry<VertexConsumer, Vertex>> all;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) {
                return;
            }
            all = new ArrayList<>(PENDING.entrySet());
            PENDING.clear();
        }
        for (Map.Entry<VertexConsumer, Vertex> e : all) {
            write(e.getKey(), e.getValue());
        }
    }

    /** a builder begins a new batch: a vertex begun before it is gone */
    public static void forget(BufferBuilder builder) {
        PENDING.remove(builder);
    }

    private static void write(VertexConsumer consumer, Vertex vertex) {
        if (consumer instanceof PendingVertex builder && builder.ladder$format() != null) {
            for (VertexFormatElement element : builder.ladder$format().getElements()) {
                writeElement(consumer, vertex, element.getUsage(), element.getIndex());
            }
        } else {
            // a wrapper: it passes each attribute to the consumers under it, in the game's usual order
            writeElement(consumer, vertex, VertexFormatElement.Usage.POSITION, 0);
            writeElement(consumer, vertex, VertexFormatElement.Usage.COLOR, 0);
            writeElement(consumer, vertex, VertexFormatElement.Usage.UV, 0);
            writeElement(consumer, vertex, VertexFormatElement.Usage.UV, 1);
            writeElement(consumer, vertex, VertexFormatElement.Usage.UV, 2);
            writeElement(consumer, vertex, VertexFormatElement.Usage.NORMAL, 0);
        }
        consumer.endVertex();
    }

    private static void writeElement(VertexConsumer consumer, Vertex vertex, VertexFormatElement.Usage usage, int index) {
        if (usage == VertexFormatElement.Usage.POSITION) {
            consumer.vertex(vertex.x, vertex.y, vertex.z);
        } else if (usage == VertexFormatElement.Usage.COLOR) {
            consumer.color(vertex.r, vertex.g, vertex.b, vertex.a);
        } else if (usage == VertexFormatElement.Usage.UV && index == 0) {
            consumer.uv(vertex.u, vertex.v);
        } else if (usage == VertexFormatElement.Usage.UV && index == 1) {
            consumer.overlayCoords(vertex.u1, vertex.v1);
        } else if (usage == VertexFormatElement.Usage.UV && index == 2) {
            consumer.uv2(vertex.u2, vertex.v2);
        } else if (usage == VertexFormatElement.Usage.NORMAL) {
            consumer.normal(vertex.nx, vertex.ny, vertex.nz);
        }
    }

    /** the consumer's begun vertex; null when none (an attribute set outside 1.21's protocol goes straight through) */
    private static Vertex begun(VertexConsumer consumer) {
        return PENDING.get(consumer);
    }

    /** 1.21's {@code addVertex(x, y, z)}: writes the vertex before, begins one */
    public static VertexConsumer addVertex(VertexConsumer consumer, float x, float y, float z) {
        endVertex(consumer);
        Vertex vertex = new Vertex();
        vertex.x = x;
        vertex.y = y;
        vertex.z = z;
        PENDING.put(consumer, vertex);
        return consumer;
    }

    /** {@code BufferBuilder.addVertex(x, y, z)} (the builder's own declaration) */
    public static VertexConsumer addVertex(BufferBuilder builder, float x, float y, float z) {
        return addVertex((VertexConsumer) builder, x, y, z);
    }

    public static VertexConsumer addVertex(VertexConsumer consumer, Vector3f position) {
        return addVertex(consumer, position.x(), position.y(), position.z());
    }

    public static VertexConsumer addVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z) {
        Vector3f p = pose.transformPosition(x, y, z, new Vector3f());
        return addVertex(consumer, p.x(), p.y(), p.z());
    }

    public static VertexConsumer addVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z) {
        return addVertex(consumer, pose.pose(), x, y, z);
    }

    public static VertexConsumer addVertex(VertexConsumer consumer, PoseStack.Pose pose, Vector3f position) {
        return addVertex(consumer, pose.pose(), position.x(), position.y(), position.z());
    }

    /** 1.21's whole vertex: 1.20.4's with the colour as four floats (it ends itself) */
    public static void addVertex(VertexConsumer consumer, float x, float y, float z, int color, float u, float v, int overlay, int light,
            float nx, float ny, float nz) {
        endVertex(consumer);
        consumer.vertex(x, y, z, FastColor.ARGB32.red(color) / 255.0F, FastColor.ARGB32.green(color) / 255.0F,
                FastColor.ARGB32.blue(color) / 255.0F, FastColor.ARGB32.alpha(color) / 255.0F, u, v, overlay, light, nx, ny, nz);
    }

    public static void addVertex(BufferBuilder builder, float x, float y, float z, int color, float u, float v, int overlay, int light,
            float nx, float ny, float nz) {
        addVertex((VertexConsumer) builder, x, y, z, color, u, v, overlay, light, nx, ny, nz);
    }

    /** 1.21's {@code setColor(r, g, b, a)} */
    public static VertexConsumer setColor(VertexConsumer consumer, int r, int g, int b, int a) {
        Vertex vertex = begun(consumer);
        if (vertex == null) {
            return consumer.color(r, g, b, a);
        }
        vertex.r = r;
        vertex.g = g;
        vertex.b = b;
        vertex.a = a;
        vertex.color = true;
        return consumer;
    }

    public static VertexConsumer setColor(BufferBuilder builder, int r, int g, int b, int a) {
        return setColor((VertexConsumer) builder, r, g, b, a);
    }

    public static VertexConsumer setColor(VertexConsumer consumer, float r, float g, float b, float a) {
        return setColor(consumer, (int) (r * 255.0F), (int) (g * 255.0F), (int) (b * 255.0F), (int) (a * 255.0F));
    }

    /** 1.21's {@code setColor(argb)} */
    public static VertexConsumer setColor(VertexConsumer consumer, int argb) {
        return setColor(consumer, FastColor.ARGB32.red(argb), FastColor.ARGB32.green(argb), FastColor.ARGB32.blue(argb), FastColor.ARGB32.alpha(argb));
    }

    public static VertexConsumer setColor(BufferBuilder builder, int argb) {
        return setColor((VertexConsumer) builder, argb);
    }

    /** 1.21's {@code setWhiteAlpha(alpha)} */
    public static VertexConsumer setWhiteAlpha(VertexConsumer consumer, int alpha) {
        return setColor(consumer, 255, 255, 255, alpha);
    }

    /** 1.21's {@code setUv(u, v)} */
    public static VertexConsumer setUv(VertexConsumer consumer, float u, float v) {
        Vertex vertex = begun(consumer);
        if (vertex == null) {
            return consumer.uv(u, v);
        }
        vertex.u = u;
        vertex.v = v;
        vertex.uv = true;
        return consumer;
    }

    public static VertexConsumer setUv(BufferBuilder builder, float u, float v) {
        return setUv((VertexConsumer) builder, u, v);
    }

    /** 1.21's {@code setUv1(u, v)} (the overlay coordinates) */
    public static VertexConsumer setUv1(VertexConsumer consumer, int u, int v) {
        Vertex vertex = begun(consumer);
        if (vertex == null) {
            return consumer.overlayCoords(u, v);
        }
        vertex.u1 = u;
        vertex.v1 = v;
        vertex.uv1 = true;
        return consumer;
    }

    public static VertexConsumer setUv1(BufferBuilder builder, int u, int v) {
        return setUv1((VertexConsumer) builder, u, v);
    }

    /** 1.21's {@code setOverlay(packed)} */
    public static VertexConsumer setOverlay(VertexConsumer consumer, int overlay) {
        return setUv1(consumer, overlay & 0xFFFF, overlay >> 16 & 0xFFFF);
    }

    public static VertexConsumer setOverlay(BufferBuilder builder, int overlay) {
        return setOverlay((VertexConsumer) builder, overlay);
    }

    /** 1.21's {@code setUv2(u, v)} (the light coordinates) */
    public static VertexConsumer setUv2(VertexConsumer consumer, int u, int v) {
        Vertex vertex = begun(consumer);
        if (vertex == null) {
            return consumer.uv2(u, v);
        }
        vertex.u2 = u;
        vertex.v2 = v;
        vertex.uv2 = true;
        return consumer;
    }

    public static VertexConsumer setUv2(BufferBuilder builder, int u, int v) {
        return setUv2((VertexConsumer) builder, u, v);
    }

    /** 1.21's {@code setLight(packed)} */
    public static VertexConsumer setLight(VertexConsumer consumer, int light) {
        return setUv2(consumer, light & 0xFFFF, light >> 16 & 0xFFFF);
    }

    public static VertexConsumer setLight(BufferBuilder builder, int light) {
        return setLight((VertexConsumer) builder, light);
    }

    /** 1.21's {@code setNormal(x, y, z)} */
    public static VertexConsumer setNormal(VertexConsumer consumer, float x, float y, float z) {
        Vertex vertex = begun(consumer);
        if (vertex == null) {
            return consumer.normal(x, y, z);
        }
        vertex.nx = x;
        vertex.ny = y;
        vertex.nz = z;
        vertex.normal = true;
        return consumer;
    }

    public static VertexConsumer setNormal(BufferBuilder builder, float x, float y, float z) {
        return setNormal((VertexConsumer) builder, x, y, z);
    }

    /**
     * 1.21's {@code setNormal(pose, x, y, z)}: through the pose's normal matrix, normalized (1.21 normalizes where the
     * pose's normals aren't trusted, after a non-uniform scale; a unit normal stays as it is)
     */
    public static VertexConsumer setNormal(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z) {
        Vector3f n = pose.normal().transform(x, y, z, new Vector3f());
        if (n.lengthSquared() > 0.0F) {
            n.normalize();
        }
        return setNormal(consumer, n.x(), n.y(), n.z());
    }

    /** 1.21's {@code putBulkData(pose, quad, r, g, b, a, light, overlay)}: 1.20.4's with alpha */
    public static void putBulkData(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a, int light,
            int overlay) {
        endVertex(consumer);
        consumer.putBulkData(pose, quad, new float[] {1.0F, 1.0F, 1.0F, 1.0F}, r, g, b, a, new int[] {light, light, light, light}, overlay, false);
    }
}
