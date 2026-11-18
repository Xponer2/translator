/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Item tags 1.20.5 added that the components use, with their contents for 1.20.4 (data/minecraft/tags/items in the
 * adapters: the tag has the 1.21 items 1.20.4 has).
 */
public final class ComponentTags {
    private static final TagKey<Item> DYEABLE = TagKey.create(Registries.ITEM, new ResourceLocation("dyeable"));

    private ComponentTags() {
    }

    /** {@code ItemTags.DYEABLE}: what a dye can colour (leather armour, leather horse armour). */
    public static TagKey<Item> dyeable() {
        return DYEABLE;
    }
}
