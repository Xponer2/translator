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
