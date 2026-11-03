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

    // the game's worker pools (1.21): daemon threads of the Translator, shared by the mods
    private static final java.util.concurrent.ExecutorService BACKGROUND = pool("Translator Worker", Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    private static final java.util.concurrent.ExecutorService IO = pool("Translator IO", 4);

    private static java.util.concurrent.ExecutorService pool(String name, int threads) {
        var counter = new java.util.concurrent.atomic.AtomicInteger();
        return java.util.concurrent.Executors.newFixedThreadPool(threads, task -> {
            Thread thread = new Thread(task, name + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public static java.util.concurrent.ExecutorService backgroundExecutor() {
        return BACKGROUND;
    }

    public static java.util.concurrent.ExecutorService ioPool() {
        return IO;
    }

    public static java.util.concurrent.ExecutorService nonCriticalIoPool() {
        return IO;
    }

    public static long getMillis() {
        return System.nanoTime() / 1000000L;
    }

    public static long getNanos() {
        return System.nanoTime();
    }

    public static long getEpochMillis() {
        return System.currentTimeMillis();
    }
}
