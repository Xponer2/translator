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
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/**
 * Item tag keys (1.21 {@code ItemTags}); contents in {@link TagManager}.
 */
public final class ItemTags {
    public static final TagKey<Item> COMPASSES = vanilla("compasses");

    private ItemTags() { }

    public static TagKey<Item> create(ResourceLocation location) {
        return TagKey.create(Registries.ITEM, location);
    }

    private static TagKey<Item> vanilla(String path) {
        return create(new ResourceLocation("minecraft", path));
    }
}
