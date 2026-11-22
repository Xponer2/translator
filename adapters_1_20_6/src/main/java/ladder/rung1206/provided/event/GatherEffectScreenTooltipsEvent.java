/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.Event;

/**
 * NeoForge 21's {@code GatherEffectScreenTooltipsEvent} (a P row): the lines of an effect's tooltip in an inventory's
 * effect list, which listeners may change. Posted from 20.6's effect list (the adapters' mixin.EffectTooltipMixin).
 */
public class GatherEffectScreenTooltipsEvent extends Event {
    protected final EffectRenderingInventoryScreen<?> screen;
    protected final MobEffectInstance effectInst;
    protected final List<Component> tooltip;

    public GatherEffectScreenTooltipsEvent(EffectRenderingInventoryScreen<?> screen, MobEffectInstance effect, List<Component> tooltip) {
        this.screen = screen;
        this.effectInst = effect;
        this.tooltip = new ArrayList<>(tooltip);
    }

    public EffectRenderingInventoryScreen<?> getScreen() {
        return this.screen;
    }

    public MobEffectInstance getEffectInstance() {
        return this.effectInst;
    }

    public List<Component> getTooltip() {
        return this.tooltip;
    }
}
