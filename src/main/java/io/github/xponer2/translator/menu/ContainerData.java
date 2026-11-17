/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.menu;

/**
 * Integers a container syncs to its screen (1.21 {@code ContainerData}): progress, fuel, energy, ...
 */
public interface ContainerData {
    int get(int index);

    void set(int index, int value);

    int getCount();
}
