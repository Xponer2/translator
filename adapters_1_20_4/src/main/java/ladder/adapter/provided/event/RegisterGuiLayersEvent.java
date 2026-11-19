/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided.event;

import ladder.adapter.event.GuiLayerBridge;
import ladder.adapter.provided.GuiLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;

/**
 * NeoForge 20.5's {@code RegisterGuiLayersEvent} over 1.20.4's RegisterGuiOverlaysEvent: each layer becomes an overlay
 * of the same id, placed by the 1.20.4 names of 1.21's vanilla layers (GuiLayerBridge).
 */
public class RegisterGuiLayersEvent extends Event implements IModBusEvent {
    private final RegisterGuiOverlaysEvent overlays;

    public RegisterGuiLayersEvent(RegisterGuiOverlaysEvent overlays) {
        this.overlays = overlays;
    }

    public void registerBelowAll(ResourceLocation id, GuiLayer layer) {
        this.overlays.registerBelowAll(id, GuiLayerBridge.overlay(layer));
    }

    public void registerBelow(ResourceLocation other, ResourceLocation id, GuiLayer layer) {
        this.overlays.registerBelow(GuiLayerBridge.lower(other), id, GuiLayerBridge.overlay(layer));
    }

    public void registerAbove(ResourceLocation other, ResourceLocation id, GuiLayer layer) {
        this.overlays.registerAbove(GuiLayerBridge.lower(other), id, GuiLayerBridge.overlay(layer));
    }

    public void registerAboveAll(ResourceLocation id, GuiLayer layer) {
        this.overlays.registerAboveAll(id, GuiLayerBridge.overlay(layer));
    }
}
