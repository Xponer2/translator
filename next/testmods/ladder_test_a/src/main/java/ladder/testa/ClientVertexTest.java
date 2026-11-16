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
package ladder.testa;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * 1.21's vertex API on the client: quads written with addVertex and its setters into the tesselator's builder, built
 * into MeshData; the vertex count, index count and the bytes written go to {@code -Dladder.test.client.out}. The same
 * on the real 1.21.1 and converted on 1.20.4 = the vertex adapters work.
 */
public final class ClientVertexTest {
    private boolean done;

    @SubscribeEvent
    public void onScreen(ScreenEvent.Init.Post event) {
        String out = System.getProperty("ladder.test.client.out");
        if (out == null || this.done || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        this.done = true;
        Map<String, String> results = new TreeMap<>();
        try {
            Tesselator tesselator = Tesselator.getInstance();
            // two quads, position + colour; the last vertex is ended by the build
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int q = 0; q < 2; q++) {
                builder.addVertex(q, 0, 0).setColor(255, 0, 0, 255);
                builder.addVertex(q + 1, 0, 0).setColor(0, 255, 0, 255);
                builder.addVertex(q + 1, 1, 0).setColor(0xFF0000FF);
                builder.addVertex(q, 1, 0.5F).setColor(1.0F, 1.0F, 1.0F, 0.5F);
            }
            MeshData mesh = builder.buildOrThrow();
            results.put("quadVertices", String.valueOf(mesh.drawState().vertexCount()));
            results.put("quadIndices", String.valueOf(mesh.drawState().indexCount()));
            results.put("quadBytes", hex(mesh.vertexBuffer()));
            mesh.close();
            // a transformed vertex, the position through a pose
            PoseStack pose = new PoseStack();
            pose.translate(10, 20, 30);
            pose.scale(2, 2, 2);
            builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int v = 0; v < 4; v++) {
                builder.addVertex(pose.last(), v, v * 2, 0).setUv(v * 0.25F, 1 - v * 0.25F).setColor(0x80402010);
            }
            mesh = builder.buildOrThrow();
            results.put("poseBytes", hex(mesh.vertexBuffer()));
            mesh.close();
            // attributes in another order than the format's (colour before uv in POSITION_TEX_COLOR)
            builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int v = 0; v < 4; v++) {
                builder.addVertex(v, 0, 0).setColor(10 * v, 20, 30, 255).setUv(v * 0.5F, 0.25F);
            }
            mesh = builder.buildOrThrow();
            results.put("orderBytes", hex(mesh.vertexBuffer()));
            mesh.close();
            // through a wrapper over two builders: each gets the four vertices
            Tesselator second = new Tesselator(4096);
            BufferBuilder a = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            BufferBuilder b = second.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            com.mojang.blaze3d.vertex.VertexConsumer both = com.mojang.blaze3d.vertex.VertexMultiConsumer.create(a, b);
            for (int v = 0; v < 4; v++) {
                both.addVertex(v, v, 0).setColor(0xFF112233);
            }
            MeshData meshA = a.buildOrThrow();
            MeshData meshB = b.buildOrThrow();
            results.put("wrapperVertices", meshA.drawState().vertexCount() + "+" + meshB.drawState().vertexCount());
            results.put("wrapperBytes", hex(meshA.vertexBuffer()) + "/" + hex(meshB.vertexBuffer()));
            meshA.close();
            meshB.close();
            // a normal through a pose scaled unevenly
            PoseStack scaled = new PoseStack();
            scaled.scale(2, 1, 1);
            builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            for (int v = 0; v < 4; v++) {
                builder.addVertex(v, 0, 0).setColor(-1).setNormal(scaled.last(), 1, 1, 0);
            }
            mesh = builder.buildOrThrow();
            results.put("normalBytes", hex(mesh.vertexBuffer()));
            mesh.close();
            // nothing written: buildOrThrow fails
            builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            try {
                builder.buildOrThrow().close();
                results.put("emptyBuildOrThrow", "built");
            } catch (IllegalStateException e) {
                results.put("emptyBuildOrThrow", e.getMessage());
            }
            // the frame's time (1.21's DeltaTracker): a partial tick within a tick
            float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            results.put("timerPartialInRange", String.valueOf(partial >= 0.0F && partial <= 1.0F));
            results.put("timerFixedOne", String.valueOf(net.minecraft.client.DeltaTracker.ONE.getGameTimeDeltaPartialTick(false)));
            // nothing written: build() gives null
            builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            MeshData empty = builder.build();
            results.put("emptyBuild", String.valueOf(empty == null));
            if (empty != null) {
                empty.close();
            }
        } catch (RuntimeException | LinkageError e) {
            results.put("error", e + (e.getCause() == null ? "" : " / " + e.getCause()));
        }
        StringBuilder json = new StringBuilder("{\n");
        results.forEach((k, v) -> json.append("  \"").append(k).append("\": \"").append(v.replace("\"", "'")).append("\",\n"));
        json.setLength(json.length() - 2);
        json.append("\n}\n");
        try {
            Files.writeString(Path.of(out), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Minecraft.getInstance().stop();
    }

    private static String hex(ByteBuffer buffer) {
        StringBuilder b = new StringBuilder();
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            b.append(String.format("%02x", buffer.get(i)));
        }
        return b.toString();
    }
}
