/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * NeoForge 21's QuadBakingVertexConsumer on 20.6's (the X rows of its constructor and bakeQuad): 21's is made empty and
 * gives its quad when asked; 20.6's hands each quad to the consumer it was made with, when its fourth vertex ends. The
 * consumer here keeps the last quad for bakeQuad, which first ends the begun vertex (1.21 ends a vertex by the next).
 */
public final class QuadBaking {
    private static final Map<QuadBakingVertexConsumer, BakedQuad[]> QUADS = Collections.synchronizedMap(new WeakHashMap<>());

    private QuadBaking() {
    }

    /** 21's {@code new QuadBakingVertexConsumer()} */
    public static QuadBakingVertexConsumer create() {
        BakedQuad[] last = new BakedQuad[1];
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer(quad -> last[0] = quad);
        QUADS.put(baker, last);
        return baker;
    }

    /** 21's {@code bakeQuad()}: the quad of the four vertices given since the last one */
    public static BakedQuad bakeQuad(QuadBakingVertexConsumer baker) {
        VertexAdapters.endVertex(baker);
        BakedQuad[] last = QUADS.get(baker);
        if (last == null || last[0] == null) {
            throw new IllegalStateException("bakeQuad: no complete quad (a baker made by 20.6's constructor hands its quads to its consumer)");
        }
        BakedQuad quad = last[0];
        last[0] = null;
        return quad;
    }
}
