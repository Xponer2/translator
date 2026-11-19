/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.event;

import java.util.HashMap;
import java.util.Map;

import ladder.adapter.provided.DeltaTracker;
import ladder.adapter.provided.GuiLayer;
import ladder.adapter.provided.event.RegisterGuiLayersEvent;
import ladder.adapter.provided.event.RenderGuiLayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 1.21's HUD layers on 1.20.4's GUI overlays (client only): the registration event is posted to every mod's bus from
 * 1.20.4's, the render events from 1.20.4's. The vanilla layers' names: 1.21 joined the camera overlays (vignette,
 * spyglass, helmet, frostbite, portal) into one and renamed others.
 */
public final class GuiLayerBridge {
    private static final Map<String, String> LOWER = new HashMap<>();
    private static final Map<String, String> UPPER = new HashMap<>();

    static {
        String[][] names = {{"camera_overlays", "vignette"}, {"jump_meter", "jump_bar"}, {"vehicle_health", "mount_health"},
                {"selected_item_name", "item_name"}, {"spectator_tooltip", "item_name"}, {"experience_level", "experience_bar"},
                {"effects", "potion_icons"}, {"boss_overlay", "boss_event_progress"}, {"sleep_overlay", "sleep_fade"},
                {"debug_overlay", "debug_screen"}, {"scoreboard_sidebar", "scoreboard"}, {"overlay_message", "record_overlay"},
                {"title", "title_text"}, {"chat", "chat_panel"}, {"tab_list", "player_list"}, {"subtitle_overlay", "subtitles"},
                {"saved_indicator", "chat_panel"}};
        for (String[] n : names) {
            LOWER.put(n[0], n[1]);
            UPPER.putIfAbsent(n[1], n[0]);
        }
        for (String camera : new String[] {"spyglass", "helmet", "frostbite", "portal"}) {
            UPPER.put(camera, "camera_overlays");
        }
        // 1.20.4's experience bar is 1.21's of the same name (1.21's experience_level only comes down onto it)
        UPPER.put("experience_bar", "experience_bar");
    }

    private GuiLayerBridge() {
    }

    /** a 1.21 vanilla layer's id as 1.20.4's overlay id (a mod's own ids are the same) */
    public static ResourceLocation lower(ResourceLocation id) {
        String name = id.getNamespace().equals("minecraft") ? LOWER.get(id.getPath()) : null;
        return name == null ? id : new ResourceLocation("minecraft", name);
    }

    /** a 1.20.4 overlay id as 1.21's layer id */
    public static ResourceLocation upper(ResourceLocation id) {
        String name = id.getNamespace().equals("minecraft") ? UPPER.get(id.getPath()) : null;
        return name == null ? id : new ResourceLocation("minecraft", name);
    }

    public static IGuiOverlay overlay(GuiLayer layer) {
        return (gui, graphics, partialTick, width, height) -> layer.render(graphics, new DeltaTracker.PartialTick(partialTick));
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(GuiLayerBridge::registerLayers);
        NeoForge.EVENT_BUS.addListener(GuiLayerBridge::pre);
        NeoForge.EVENT_BUS.addListener(GuiLayerBridge::post);
    }

    private static void registerLayers(RegisterGuiOverlaysEvent event) {
        RegisterGuiLayersEvent layers = new RegisterGuiLayersEvent(event);
        ModList.get().forEachModContainer((id, container) -> {
            if (container.getEventBus() != null && !id.equals("ladder_adapters")) {
                container.getEventBus().post(layers);
            }
        });
    }

    private static void pre(RenderGuiOverlayEvent.Pre event) {
        if (NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Pre(event)).isCanceled()) {
            event.setCanceled(true);
        }
    }

    private static void post(RenderGuiOverlayEvent.Post event) {
        NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Post(event));
    }
}
