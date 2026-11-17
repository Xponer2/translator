/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.ladder.probe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.TickEvent;

/**
 * The client half of the probes ({@code -Dladder.probe.client.out=<file>}). When the title screen first opens, the
 * mods that loaded go to that JSON file. Then, with {@code -Dladder.probe.client.world=true}, a fresh creative world is
 * made and entered: after the player has been in it a while the inventory opens, a few frames are drawn, and the
 * results (the world reached, the screens, the frames) are written again; then the game closes. A mod that breaks
 * loading never reaches the title screen, one that breaks the world never gets the results written: the run's watchdog
 * sees no file (or one without {@code "world"}).
 */
public final class LadderProbeClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int OPEN_INVENTORY = 60, RECORD = 120;
    private boolean titleSeen;
    private int ticksInWorld, inventoryFrames, worldFrames;
    private final JsonObject root = new JsonObject();

    @SubscribeEvent
    public void onScreen(ScreenEvent.Init.Post event) {
        String out = System.getProperty("ladder.probe.client.out");
        // the title screen, or the accessibility onboarding screen of a game folder without options
        if (out == null || this.titleSeen || !(event.getScreen() instanceof TitleScreen
                || event.getScreen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)) {
            return;
        }
        this.titleSeen = true;
        this.root.addProperty("firstScreen", event.getScreen().getClass().getSimpleName());
        JsonArray mods = new JsonArray();
        ModList.get().getMods().forEach(info -> mods.add(info.getModId() + " " + info.getVersion()));
        this.root.add("mods", mods);
        Minecraft minecraft = Minecraft.getInstance();
        if (!Boolean.getBoolean("ladder.probe.client.world")) {
            this.write(out);
            minecraft.stop();
            return;
        }
        // into a fresh world: the converted mods' client code in a level (ticks, rendering, the inventory)
        String name = "ladder-probe-" + System.currentTimeMillis();
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
                WorldDataConfiguration.DEFAULT);
        minecraft.tell(() -> minecraft.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(20240101L, true, false),
                WorldPresets::createNormalWorldDimensions, event.getScreen()));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        String out = System.getProperty("ladder.probe.client.out");
        Minecraft minecraft = Minecraft.getInstance();
        if (out == null || event.phase != TickEvent.Phase.END || minecraft.level == null || minecraft.player == null) {
            return;
        }
        this.ticksInWorld++;
        if (this.ticksInWorld == OPEN_INVENTORY) {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        } else if (this.ticksInWorld == RECORD) {
            JsonObject world = new JsonObject();
            world.addProperty("dimension", minecraft.level.dimension().location().toString());
            world.addProperty("screen", minecraft.screen == null ? "none" : minecraft.screen.getClass().getName());
            world.addProperty("worldFrames", this.worldFrames);
            world.addProperty("inventoryFrames", this.inventoryFrames);
            world.addProperty("loadedChunks", minecraft.level.getChunkSource().getLoadedChunksCount());
            world.addProperty("entities", minecraft.level.getEntityCount());
            this.root.add("world", world);
            this.write(out);
            minecraft.setScreen(null);
            minecraft.stop();
        }
    }

    @SubscribeEvent
    public void onScreenDrawn(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
            this.inventoryFrames++;
        }
    }

    @SubscribeEvent
    public void onLevelDrawn(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY) {
            this.worldFrames++;
        }
    }

    private void write(String out) {
        try {
            Files.writeString(Path.of(out), new GsonBuilder().setPrettyPrinting().create().toJson(this.root), StandardCharsets.UTF_8);
            LOGGER.info("Client probe results written to {}", out);
        } catch (IOException e) {
            LOGGER.error("Couldn't write the client probe results", e);
        }
    }
}
