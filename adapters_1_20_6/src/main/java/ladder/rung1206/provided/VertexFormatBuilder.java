/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * 1.21's {@code VertexFormat.Builder} (a P row): the named elements in order, made into 1.20.6's VertexFormat (an ordered
 * map of names to elements). Padding is 1.20.6's one-byte padding element, as many as the bytes.
 */
public final class VertexFormatBuilder {
    private final ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
    private int paddings;

    public VertexFormatBuilder add(String name, VertexFormatElement element) {
        this.elements.put(name, element);
        return this;
    }

    public VertexFormatBuilder padding(int bytes) {
        for (int i = 0; i < bytes; i++) {
            this.elements.put("Padding" + this.paddings++, DefaultVertexFormat.ELEMENT_PADDING);
        }
        return this;
    }

    public VertexFormat build() {
        return new VertexFormat(this.elements.buildOrThrow());
    }
}
