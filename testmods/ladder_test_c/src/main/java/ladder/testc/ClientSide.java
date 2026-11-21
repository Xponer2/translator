/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import ladder.testc.Payloads.Gift;
import ladder.testc.Payloads.Net;
import ladder.testc.Payloads.Ping;
import ladder.testc.Payloads.Pong;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The client half: sends the payloads once in a world, records the answers, writes them. */
final class ClientSide {
    private static final int SEND = 20, WRITE = 100;
    private static final Map<String, String> RESULTS = new TreeMap<>();
    private static int ticks;
    private static boolean titleSeen;
    private static boolean written;

    private ClientSide() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener(ClientSide::tick);
        if (Boolean.getBoolean("ladder.test.c.drive")) {
            NeoForge.EVENT_BUS.addListener(ClientSide::screen);
        }
    }

    /** Natively (and on a target without the probe) the test enters a fresh world itself, or connects to a server. */
    private static void screen(ScreenEvent.Init.Post event) {
        if (titleSeen || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        titleSeen = true;
        Minecraft minecraft = Minecraft.getInstance();
        String connect = System.getProperty("ladder.test.c.connect", "");
        if (!connect.isEmpty()) {
            // onto a dedicated server instead (a real connection: the payloads' codecs run)
            minecraft.tell(() -> net.minecraft.client.gui.screens.ConnectScreen.startConnecting(event.getScreen(), minecraft,
                    net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(connect),
                    new net.minecraft.client.multiplayer.ServerData("ladder", connect, net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null));
            return;
        }
        String name = "ladder-test-c-" + System.currentTimeMillis();
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
                WorldDataConfiguration.DEFAULT);
        minecraft.tell(() -> minecraft.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(20240101L, true, false),
                WorldPresets::createNormalWorldDimensions, event.getScreen()));
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        ticks++;
        if (ticks == SEND) {
            PacketDistributor.sendToServer(new Ping(42, "héllo ✓", new BlockPos(-5, 64, 300000), new UUID(0x0123456789abcdefL, 0xfedcba9876543210L),
                    List.of(1, 300, -7), Optional.of(Payloads.id("tag"))));
            ItemStack stack = new ItemStack(Items.DIAMOND, 3);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Shiny"));
            PacketDistributor.sendToServer(new Gift(stack, Component.literal("for you").withStyle(ChatFormatting.GOLD), Blocks.OAK_PLANKS));
            PacketDistributor.sendToServer(new Net(7));
        } else if (ticks == SEND + 10) {
            // armour drawn: a 1.20.5 material's layers (dyed, suffixed, inner model, an item's own texture), the game's own
            ItemStack dyed = new ItemStack(Armour.HELMET.get());
            dyed.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0x3366CC, true));
            RESULTS.put("armourDrawn", ArmourClient.drawn(dyed, new ItemStack(Armour.LEGGINGS.get()), new ItemStack(Armour.BOOTS.get())));
            RESULTS.put("armourDrawnVanilla", ArmourClient.drawn(new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.IRON_LEGGINGS),
                    new ItemStack(Items.DIAMOND_CHESTPLATE)));
        } else if (ticks == SEND + 15) {
            RecipeChecks.run(RESULTS);
        } else if (ticks == SEND + 20) {
            PackChecks.run(RESULTS);
        } else if (ticks == SEND + 22) {
            ShapeChecks.run(RESULTS);
        } else if (ticks == WRITE - 5) {
            GuiClient.record(RESULTS);
            boolean seen = false;
            for (net.minecraft.world.entity.Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
                seen |= entity.getType() == SpawnProbe.TYPE.get();
            }
            RESULTS.put("spawnProbe", SpawnProbe.pairedWith + "/" + seen);
        } else if (ticks >= WRITE && !written && PackChecks.done()) {
            // (after the resource reload the pack check started has ended)
            written = true;
            write();
            if (Boolean.getBoolean("ladder.test.c.drive")) {
                minecraft.stop();
            }
        }
    }

    static void onPong(Pong pong, IPayloadContext context) {
        String key = pong.what().split(" ")[0];
        RESULTS.put("pong." + key, pong.what() + " | n=" + pong.n() + " serverMain=" + pong.mainThread() + " clientMain="
                + Minecraft.getInstance().isSameThread() + " flow=" + context.flow());
        RESULTS.merge("count." + key, "1", (a, b) -> String.valueOf(Integer.parseInt(a) + 1));
    }

    static void onBag(Payloads.Bag bag, IPayloadContext context) {
        RESULTS.put("bag", bag + " | " + bag.counts().getClass().getSimpleName() + " " + bag.tags().getClass().getSimpleName() + " clientMain="
                + Minecraft.getInstance().isSameThread());
    }

    static void onPhase(Payloads.Phase phase, IPayloadContext context) {
        RESULTS.put("phase", "n=" + phase.n() + " flow=" + context.flow());
    }

    static void onGift(Gift gift, IPayloadContext context) {
        RESULTS.put("giftBack", LadderTestC.describe(gift) + " | clientMain=" + Minecraft.getInstance().isSameThread());
    }

    private static void write() {
        String out = System.getProperty("ladder.test.c.out");
        if (out == null) {
            return;
        }
        JsonObject json = new JsonObject();
        RESULTS.forEach(json::addProperty);
        try {
            Files.writeString(Path.of(out), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
