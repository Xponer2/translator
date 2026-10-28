package io.github.xponer2.translator.util;

import java.util.function.Consumer;

/**
 * 1.21 {@code net.minecraft.Util} helpers the mods use.
 */
public final class Util {
    private Util() { }

    public static <T> T make(T object, Consumer<? super T> initializer) {
        initializer.accept(object);
        return object;
    }
}
