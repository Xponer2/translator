/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided.event;

import ladder.adapter.event.GuiLayerBridge;
import ladder.adapter.provided.DeltaTracker;
import ladder.adapter.provided.GuiLayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;

/** NeoForge 20.5's {@code RenderGuiLayerEvent} over 1.20.4's RenderGuiOverlayEvent (the layer's 1.21 name). */
public abstract class RenderGuiLayerEvent extends Event {
    final RenderGuiOverlayEvent overlay;

    RenderGuiLayerEvent(RenderGuiOverlayEvent overlay) {
        this.overlay = overlay;
    }

    public ResourceLocation getName() {
        return GuiLayerBridge.upper(this.overlay.getOverlay().id());
    }

    public GuiLayer getLayer() {
        return (graphics, partialTick) -> this.overlay.getOverlay().overlay().render(net.minecraft.client.Minecraft.getInstance().gui instanceof
                net.neoforged.neoforge.client.gui.overlay.ExtendedGui gui ? gui : null, graphics, partialTick.getGameTimeDeltaPartialTick(false), graphics.guiWidth(), graphics.guiHeight());
    }

    public GuiGraphics getGuiGraphics() {
        return this.overlay.getGuiGraphics();
    }

    /** 1.21's frame time (the class is provided at every step, so a mod's call keeps 1.21's DeltaTracker form) */
    public DeltaTracker getPartialTick() {
        return new DeltaTracker.PartialTick(this.overlay.getPartialTick());
    }

    public static class Pre extends RenderGuiLayerEvent implements ICancellableEvent {
        public Pre(RenderGuiOverlayEvent.Pre overlay) {
            super(overlay);
        }
    }

    public static class Post extends RenderGuiLayerEvent {
        public Post(RenderGuiOverlayEvent.Post overlay) {
            super(overlay);
        }
    }
}
