/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.DimensionTransitionScreenManager;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The client half: a screen for the menu, a transition screen for the nether; the results written after a while in a world. */
final class ClientSide {
    private static final DimensionTransitionScreenManager.ReceivingLevelScreenFactory NETHER_SCREEN = ReceivingLevelScreen::new;
    private static int ticks;
    private static boolean written;

    private ClientSide() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(LadderTestF.DATA_MENU.get(), DataScreen::new));
        modBus.addListener(RegisterDimensionTransitionScreenEvent.class, event -> event.registerIncomingEffect(Level.NETHER, NETHER_SCREEN));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || written || ++ticks < 80) {
            return;
        }
        written = true;
        Map<String, String> results = new TreeMap<>();
        results.put("menuClientData", LadderTestF.clientMenu);
        results.put("transitionNether", String.valueOf(DimensionTransitionScreenManager.getScreen(Level.NETHER, Level.OVERWORLD) == NETHER_SCREEN));
        results.put("transitionEnd", String.valueOf(DimensionTransitionScreenManager.getScreen(Level.END, Level.OVERWORLD) == NETHER_SCREEN));
        String out = System.getProperty("ladder.test.f.out");
        if (out != null) {
            JsonObject json = new JsonObject();
            results.forEach(json::addProperty);
            try {
                Files.writeString(Path.of(out), new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (minecraft.screen instanceof DataScreen) {
            minecraft.player.closeContainer();
        }
    }

    static final class DataScreen extends AbstractContainerScreen<LadderTestF.DataMenu> {
        DataScreen(LadderTestF.DataMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        }
    }
}
