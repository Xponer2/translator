/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import java.lang.reflect.Field;
import java.util.List;

import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

/**
 * 1.21's SectionCompiler (a section's meshes) is 1.20.6's RebuildTask.compile (a W row): a mixin's handler of 1.21's
 * compile gets its values from the task (A rows), its shadows of the compiler's renderers the game's (F rows).
 */
public final class SectionAdapters {
    /** the task's render section (the inner class's outer instance) */
    private static volatile Field section;

    private SectionAdapters() {
    }

    /** the section's position (1.21 passes it; 1.20.6's task has its render section's origin) */
    public static SectionPos origin(SectionRenderDispatcher.RenderSection.RebuildTask task) {
        Field f = section;
        if (f == null) {
            for (Class<?> c = task.getClass(); c != null && f == null; c = c.getSuperclass()) {
                for (Field x : c.getDeclaredFields()) {
                    if (x.getType() == SectionRenderDispatcher.RenderSection.class) {
                        x.setAccessible(true);
                        f = x;
                        break;
                    }
                }
            }
            if (f == null) {
                throw new IllegalStateException("no render section in " + task.getClass().getName());
            }
            section = f;
        }
        try {
            return SectionPos.of(((SectionRenderDispatcher.RenderSection) f.get(task)).getOrigin());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** the section's copy of the level (1.20.6: the task's region) */
    public static RenderChunkRegion region(SectionRenderDispatcher.RenderSection.RebuildTask task) {
        return task.region;
    }

    /** the translucent layer's sorting: by the distance to the camera (1.20.6 passes the camera's position to the section) */
    public static VertexSorting sorting(float x, float y, float z) {
        return VertexSorting.byDistance(x, y, z);
    }

    /** NeoForge's additional section renderers (1.20.6: the task's) */
    public static List<AddSectionGeometryEvent.AdditionalSectionRenderer> renderers(SectionRenderDispatcher.RenderSection.RebuildTask task) {
        return task.additionalRenderers;
    }

    /** the compiler's block renderer (1.20.6's task takes the game's) */
    public static BlockRenderDispatcher blockRenderer(SectionRenderDispatcher.RenderSection.RebuildTask task) {
        return Minecraft.getInstance().getBlockRenderer();
    }

    /** the compiler's block entity renderer (the game's) */
    public static BlockEntityRenderDispatcher blockEntityRenderer(SectionRenderDispatcher.RenderSection.RebuildTask task) {
        return Minecraft.getInstance().getBlockEntityRenderDispatcher();
    }
}
