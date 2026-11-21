/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import ladder.rung1206.provided.VertexFormatBuilder;

/**
 * 1.21's vertex format elements (constants of VertexFormatElement, a record) on 1.20.6's (DefaultVertexFormat's
 * ELEMENT_ constants, getters), and its formats' builder and element offsets.
 */
public final class VertexFormats {
    private VertexFormats() {
    }

    public static VertexFormatElement position() {
        return DefaultVertexFormat.ELEMENT_POSITION;
    }

    public static VertexFormatElement color() {
        return DefaultVertexFormat.ELEMENT_COLOR;
    }

    public static VertexFormatElement uv0() {
        return DefaultVertexFormat.ELEMENT_UV0;
    }

    public static VertexFormatElement uv1() {
        return DefaultVertexFormat.ELEMENT_UV1;
    }

    public static VertexFormatElement uv2() {
        return DefaultVertexFormat.ELEMENT_UV2;
    }

    public static VertexFormatElement normal() {
        return DefaultVertexFormat.ELEMENT_NORMAL;
    }

    public static VertexFormatElement.Usage usage(VertexFormatElement element) {
        return element.getUsage();
    }

    public static VertexFormatElement.Type type(VertexFormatElement element) {
        return element.getType();
    }

    public static int count(VertexFormatElement element) {
        return element.getCount();
    }

    public static int index(VertexFormatElement element) {
        return element.getIndex();
    }

    public static int byteSize(VertexFormatElement element) {
        return element.getByteSize();
    }

    /** 1.21's {@code format.getOffset(element)}: the offset of the element's place in the format (-1 when it has none) */
    public static int getOffset(VertexFormat format, VertexFormatElement element) {
        int index = format.getElements().indexOf(element);
        return index < 0 ? -1 : format.getOffset(index);
    }

    /** 1.21's {@code VertexFormat.builder()} */
    public static VertexFormatBuilder builder() {
        return new VertexFormatBuilder();
    }
}
