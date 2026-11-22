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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.DimensionTransitionScreenManager;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The client half: a screen for the menu, a transition screen for the nether; the results written after a while in a world. */
final class ClientSide {
    private static final DimensionTransitionScreenManager.ReceivingLevelScreenFactory NETHER_SCREEN = ReceivingLevelScreen::new;
    /** client extensions registered through NeoForge 21's RegisterClientExtensionsEvent */
    private static final IClientItemExtensions STICK_EXTENSIONS = new IClientItemExtensions() {
    };
    private static int ticks;
    private static boolean written;

    private ClientSide() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(LadderTestF.DATA_MENU.get(), DataScreen::new));
        modBus.addListener(RegisterDimensionTransitionScreenEvent.class, event -> event.registerIncomingEffect(Level.NETHER, NETHER_SCREEN));
        modBus.addListener(RegisterClientExtensionsEvent.class, event -> event.registerItem(STICK_EXTENSIONS, Items.STICK));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    /** the most client ticks the results wait for the synced attachments (a lagging server sets the pig's later) */
    private static final int WAIT_FOR_SYNC = 400;

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || written || ++ticks < 80) {
            return;
        }
        // the server changes the pig's attachment after its own 40 ticks: wait for both synced values and the opened menu (or the timeout)
        net.minecraft.world.entity.Entity syncedPig = minecraft.level.getEntity(LadderTestF.pigId);
        boolean synced = minecraft.player != null && Integer.valueOf(7).equals(minecraft.player.getData(LadderTestF.MARK))
                && syncedPig != null && Integer.valueOf(12).equals(syncedPig.getData(LadderTestF.MARK))
                // and the menu the server opened with its client data
                && !"-".equals(LadderTestF.clientMenu);
        if (!synced && ticks < WAIT_FOR_SYNC) {
            return;
        }
        written = true;
        Map<String, String> results = new TreeMap<>();
        results.put("menuClientData", LadderTestF.clientMenu);
        results.put("attachPlayer", String.valueOf(minecraft.player.getData(LadderTestF.MARK)));
        net.minecraft.world.entity.Entity pig = minecraft.level.getEntity(LadderTestF.pigId);
        results.put("attachPig", pig == null ? "none" : String.valueOf(pig.getData(LadderTestF.MARK)));
        results.put("transitionNether", String.valueOf(DimensionTransitionScreenManager.getScreen(Level.NETHER, Level.OVERWORLD) == NETHER_SCREEN));
        results.put("clientExtensions", String.valueOf(IClientItemExtensions.of(Items.STICK) == STICK_EXTENSIONS));
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
