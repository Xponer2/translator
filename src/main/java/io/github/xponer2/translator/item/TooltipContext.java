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
package io.github.xponer2.translator.item;

import io.github.xponer2.translator.registry.HolderLookup;
import io.github.xponer2.translator.registry.RegistryAccess;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Where a tooltip is shown (1.21 {@code Item.TooltipContext}). 1.12.2 items get {@code addInformation(stack, world,
 * List<String>, flag)}; Cucumber's base items override it and call the 1.21 method {@code appendHoverText(stack,
 * context, List<ITextComponent>, flag)} through {@link #addInformation}.
 */
public final class TooltipContext {
    public static final TooltipContext EMPTY = new TooltipContext(null);

    @Nullable
    private final World level;

    private TooltipContext(@Nullable World level) {
        this.level = level;
    }

    public static TooltipContext of(@Nullable World level) {
        return level == null ? EMPTY : new TooltipContext(level);
    }

    @Nullable
    public World level() {
        return this.level;
    }

    @Nullable
    public HolderLookup.Provider registries() {
        return RegistryAccess.INSTANCE;
    }

    /**
     * Runs the 1.21 tooltip method of an item and adds its lines to the 1.12.2 tooltip.
     */
    public static void addInformation(HoverText item, ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        var lines = new ArrayList<ITextComponent>();
        item.appendHoverText(stack, of(world), lines, flag);
        for (var line : lines) {
            tooltip.add(line.getFormattedText());
        }
    }

    /**
     * An item with the 1.21 tooltip method.
     */
    @FunctionalInterface
    public interface HoverText {
        void appendHoverText(ItemStack stack, TooltipContext context, List<ITextComponent> tooltip, ITooltipFlag flag);
    }
}
