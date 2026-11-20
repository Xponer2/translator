/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.attribute;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * NeoForge 21.1's {@code AttributeTooltipContext} on 1.21: an item's tooltip context with the player looking at it and
 * the tooltip flag, as the attribute tooltip hooks pass it.
 */
public interface AttributeTooltipContext extends Item.TooltipContext {
    @Nullable
    Player player();

    TooltipFlag flag();

    static AttributeTooltipContext of(@Nullable Player player, Item.TooltipContext itemContext, TooltipFlag flag) {
        return new AttributeTooltipContext() {
            @Override
            public HolderLookup.Provider registries() {
                return itemContext.registries();
            }

            @Override
            public float tickRate() {
                return itemContext.tickRate();
            }

            @Override
            public MapItemSavedData mapData(MapId id) {
                return itemContext.mapData(id);
            }

            @Override
            public Level level() {
                return itemContext.level();
            }

            @Override
            public Player player() {
                return player;
            }

            @Override
            public TooltipFlag flag() {
                return flag;
            }
        };
    }
}
