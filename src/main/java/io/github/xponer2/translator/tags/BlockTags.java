/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.tags;

import io.github.xponer2.translator.util.Registries;
import io.github.xponer2.translator.util.TagKey;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

/**
 * Block tag keys (1.21 {@code BlockTags}); contents in {@link TagManager}.
 */
public final class BlockTags {
    public static final TagKey<Block> CROPS = vanilla("crops");

    private BlockTags() { }

    public static TagKey<Block> create(ResourceLocation location) {
        return TagKey.create(Registries.BLOCK, location);
    }

    private static TagKey<Block> vanilla(String path) {
        return create(new ResourceLocation("minecraft", path));
    }
}
