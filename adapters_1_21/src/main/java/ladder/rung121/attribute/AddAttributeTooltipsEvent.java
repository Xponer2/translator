/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.attribute;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.Event;

/**
 * NeoForge 21.1's {@code AddAttributeTooltipsEvent} on 1.21: posted on the game's bus after an item's attribute lines
 * (AttributeUtil), for listeners to add their own attribute-related lines.
 */
public class AddAttributeTooltipsEvent extends Event {
    protected final ItemStack stack;
    protected final Consumer<Component> tooltip;
    protected final AttributeTooltipContext ctx;

    public AddAttributeTooltipsEvent(ItemStack stack, Consumer<Component> tooltip, AttributeTooltipContext ctx) {
        this.stack = stack;
        this.tooltip = tooltip;
        this.ctx = ctx;
    }

    public AttributeTooltipContext getContext() {
        return this.ctx;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public void addTooltipLines(Component... comps) {
        for (Component comp : comps) {
            this.tooltip.accept(comp);
        }
    }

    /** whether the stack shows its attribute modifiers (its component's own flag) */
    public boolean shouldShow() {
        return this.stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).showInTooltip();
    }
}
