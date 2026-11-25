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

    // ---------------------------------------------------------------- 1.21's element ids (a record with an id there)

    /** 1.21's registered elements by id (vanilla's six first, as 1.21 registers them; a mod's by register) */
    private static final VertexFormatElement[] BY_ID = new VertexFormatElement[32];
    private static final java.util.Map<VertexFormatElement, Integer> IDS = new java.util.IdentityHashMap<>();
    /** 1.20.6's padding element (1.21 has none): an id of its own that byId never gives (so no mod registers it) */
    private static final int PADDING_ID = 31;

    static {
        VertexFormatElement[] vanilla = {DefaultVertexFormat.ELEMENT_POSITION, DefaultVertexFormat.ELEMENT_COLOR, DefaultVertexFormat.ELEMENT_UV0,
                DefaultVertexFormat.ELEMENT_UV1, DefaultVertexFormat.ELEMENT_UV2, DefaultVertexFormat.ELEMENT_NORMAL};
        for (int id = 0; id < vanilla.length; id++) {
            BY_ID[id] = vanilla[id];
            IDS.put(vanilla[id], id);
        }
        IDS.put(DefaultVertexFormat.ELEMENT_PADDING, PADDING_ID);
    }

    /** 1.21's {@code VertexFormatElement.register(id, index, type, usage, count)} */
    public static synchronized VertexFormatElement register(int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        VertexFormatElement element = create(id, index, type, usage, count);
        if (BY_ID[id] != null) {
            throw new IllegalArgumentException("Duplicate element registration for: " + id);
        }
        BY_ID[id] = element;
        return element;
    }

    /** 1.21's {@code new VertexFormatElement(id, index, type, usage, count)} */
    public static synchronized VertexFormatElement create(int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        if (id < 0 || id >= BY_ID.length) {
            throw new IllegalArgumentException("Element ID must be in range [0; " + BY_ID.length + ")");
        }
        VertexFormatElement element = new VertexFormatElement(index, type, usage, count);
        IDS.put(element, id);
        return element;
    }

    /** 1.21's {@code VertexFormatElement.byId(id)} (null: none registered) */
    @javax.annotation.Nullable
    public static synchronized VertexFormatElement byId(int id) {
        return BY_ID[id];
    }

    /** 1.21's {@code element.id()}: its registered id; an element 1.20.6's code made gets a free one from the top */
    public static synchronized int id(VertexFormatElement element) {
        Integer id = IDS.get(element);
        if (id != null) {
            return id;
        }
        for (int free = PADDING_ID - 1; free >= 0; free--) {
            if (BY_ID[free] == null && !IDS.containsValue(free)) {
                IDS.put(element, free);
                return free;
            }
        }
        throw new IllegalStateException("no free vertex format element id for " + element);
    }

    /** 1.21's {@code element.mask()} */
    public static int mask(VertexFormatElement element) {
        return 1 << id(element);
    }

    /** 1.21's {@code VertexFormatElement.elementsFromMask(mask)}: the registered elements of the mask */
    public static synchronized java.util.stream.Stream<VertexFormatElement> elementsFromMask(int mask) {
        java.util.List<VertexFormatElement> found = new java.util.ArrayList<>();
        for (int id = 0; id < BY_ID.length; id++) {
            if (BY_ID[id] != null && (mask & 1 << id) != 0) {
                found.add(BY_ID[id]);
            }
        }
        return found.stream();
    }
}
