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
