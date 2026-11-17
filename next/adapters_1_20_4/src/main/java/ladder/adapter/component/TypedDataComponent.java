/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

/** 1.20.5's {@code TypedDataComponent}: a component type with its value. */
public record TypedDataComponent<T>(DataComponentType<T> type, T value) {
}
