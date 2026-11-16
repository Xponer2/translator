/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * The client half of the probes: when the title screen first opens ({@code -Dladder.probe.client.out=<file>}), the
 * mods that loaded and the screen reached go to that JSON file, and the game closes. A mod that breaks loading never
 * reaches the title screen (FML shows its error screen instead): the run's watchdog sees no file.
 */
public final class LadderProbeClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean done;

    @SubscribeEvent
    public void onScreen(ScreenEvent.Init.Post event) {
        String out = System.getProperty("ladder.probe.client.out");
        // the title screen, or the accessibility onboarding screen of a game folder without options
        if (out == null || this.done || !(event.getScreen() instanceof TitleScreen
                || event.getScreen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)) {
            return;
        }
        this.done = true;
        JsonObject root = new JsonObject();
        root.addProperty("firstScreen", event.getScreen().getClass().getSimpleName());
        JsonArray mods = new JsonArray();
        ModList.get().getMods().forEach(info -> mods.add(info.getModId() + " " + info.getVersion()));
        root.add("mods", mods);
        try {
            Files.writeString(Path.of(out), new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
            LOGGER.info("Client probe results written to {}", out);
        } catch (IOException e) {
            LOGGER.error("Couldn't write the client probe results", e);
        }
        Minecraft.getInstance().stop();
    }
}
