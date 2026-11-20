/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.attribute;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * NeoForge 21.1's {@code GatherSkippedAttributeTooltipsEvent} on 1.21: posted on the game's bus before an item's
 * attribute lines are made (AttributeUtil); listeners hide modifiers by id, whole slot groups, or all of them.
 */
public class GatherSkippedAttributeTooltipsEvent extends Event {
    protected final ItemStack stack;
    protected final AttributeTooltipContext ctx;
    private final Set<ResourceLocation> skippedIds = new HashSet<>();
    private final Set<EquipmentSlotGroup> skippedGroups = new HashSet<>();
    private boolean skipAll;

    public GatherSkippedAttributeTooltipsEvent(ItemStack stack, AttributeTooltipContext ctx) {
        this.stack = stack;
        this.ctx = ctx;
    }

    public AttributeTooltipContext getContext() {
        return this.ctx;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public void skipId(ResourceLocation id) {
        this.skippedIds.add(id);
    }

    public void skipGroup(EquipmentSlotGroup group) {
        this.skippedGroups.add(group);
    }

    public boolean isSkipped(ResourceLocation id) {
        return this.skippedIds.contains(id);
    }

    public boolean isSkipped(EquipmentSlotGroup group) {
        return this.skippedGroups.contains(group);
    }

    public void setSkipAll(boolean skip) {
        this.skipAll = skip;
    }

    public boolean isSkippingAll() {
        return this.skipAll;
    }

    protected Set<ResourceLocation> getSkippedIds() {
        return this.skippedIds;
    }

    protected Set<EquipmentSlotGroup> getSkippedGroups() {
        return this.skippedGroups;
    }
}
