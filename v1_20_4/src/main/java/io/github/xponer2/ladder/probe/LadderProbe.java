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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Behaviour probes of converted mods on the real 1.20.4, run on a dedicated server when {@code -Dladder.probe.out=<file>}
 * is set: the results go to that JSON file and the server stops.
 *
 * <p>Alternate Current (1.21.1, converted): the same redstone line is built with the mod on and off. The power levels
 * must be vanilla's (15 down to 0, all 0 once the source is gone) both times; the mod's own code must be in the level
 * (its mixin interface, a wire handler) and its updates must differ from vanilla's (fewer neighbour notifications), or
 * its code path didn't run.</p>
 */
@Mod("ladder_probe")
public final class LadderProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private int notifications;

    public LadderProbe(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(new LadderProbeClient());
        }
    }

    @SubscribeEvent
    public void onNotify(BlockEvent.NeighborNotifyEvent event) {
        this.notifications++;
    }

    @SubscribeEvent
    public void onStarted(ServerStartedEvent event) {
        String out = System.getProperty("ladder.probe.out");
        if (out == null) {
            return;
        }
        MinecraftServer server = event.getServer();
        JsonObject root = new JsonObject();
        try {
            root.add("alternate_current", this.alternateCurrent(server.overworld()));
        } catch (Exception | LinkageError e) {
            LOGGER.error("Probe failed", e);
            root.addProperty("error", e.toString());
        }
        try {
            root.add("block_use", this.blockUse(server.overworld()));
        } catch (Exception | LinkageError e) {
            LOGGER.error("Block use probe failed", e);
            root.addProperty("blockUseError", e.toString());
        }
        String facts = System.getProperty("ladder.facts.out");
        if (facts != null) {
            // the converted mods' facts, as the 1.21.1 reference records them
            try {
                Files.writeString(Path.of(facts), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                        .toJson(LadderFacts.record(server)), StandardCharsets.UTF_8);
            } catch (IOException | RuntimeException | LinkageError e) {
                LOGGER.error("Facts failed", e);
            }
        }
        String data = System.getProperty("ladder.data.out");
        if (data != null) {
            // whether every data file of the converted mods loaded
            try {
                Files.writeString(Path.of(data), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                        .toJson(LadderData.record(server)), StandardCharsets.UTF_8);
            } catch (IOException | RuntimeException | LinkageError e) {
                LOGGER.error("Data check failed", e);
            }
        }
        try {
            Files.writeString(Path.of(out), new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
            LOGGER.info("Probe results written to {}", out);
        } catch (IOException e) {
            LOGGER.error("Couldn't write the probe results", e);
        }
        server.halt(false);
    }

    private JsonObject alternateCurrent(ServerLevel level) throws Exception {
        JsonObject result = new JsonObject();
        Class<?> mod;
        try {
            mod = Class.forName("alternate.current.AlternateCurrentMod");
        } catch (ClassNotFoundException e) {
            result.addProperty("present", false);
            return result;
        }
        result.addProperty("present", true);
        Class<?> levelInterface = Class.forName("alternate.current.interfaces.mixin.IServerLevel");
        result.addProperty("levelHasMixinInterface", levelInterface.isInstance(level));
        Object handler = null;
        if (levelInterface.isInstance(level)) {
            Method get = levelInterface.getMethod("alternate_current$getWireHandler");
            handler = get.invoke(level);
        }
        result.addProperty("wireHandler", handler == null ? null : handler.getClass().getName());
        Field on = mod.getField("on");
        boolean was = on.getBoolean(null);
        JsonObject modes = new JsonObject();
        List<Integer> notified = new ArrayList<>();
        boolean powersOk = true;
        for (boolean enabled : new boolean[] {true, false}) {
            on.setBoolean(null, enabled);
            JsonObject run = this.line(level, enabled ? 0 : 8);
            modes.add(enabled ? "on" : "off", run);
            notified.add(run.get("notificationsPowering").getAsInt() + run.get("notificationsUnpowering").getAsInt());
            powersOk &= run.get("powersAsVanilla").getAsBoolean();
        }
        on.setBoolean(null, was);
        result.add("modes", modes);
        boolean codeRan = !notified.get(0).equals(notified.get(1));
        result.addProperty("modCodePathRan", codeRan);
        result.addProperty("pass", levelInterface.isInstance(level) && handler != null && powersOk && codeRan);
        return result;
    }

    /**
     * 1.20.5's useItemOn of a converted mod's block reached through 1.20.4's {@code BlockStateBase.use} (the adapters):
     * Macaw's shutter opens and closes on a right click with an empty hand, and stays as it is with its own item in
     * hand (useItemOn passes to the default interaction, which a shutter hasn't).
     */
    private JsonObject blockUse(ServerLevel level) {
        JsonObject result = new JsonObject();
        var id = new net.minecraft.resources.ResourceLocation("mcwwindows", "acacia_shutter");
        if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)) {
            result.addProperty("present", false);
            return result;
        }
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
        BlockPos pos = new BlockPos(4, 100, 16);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, block.defaultBlockState(), 3);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.NORTH, pos, false);
        var open = net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN;
        boolean before = level.getBlockState(pos).getValue(open);
        var first = level.getBlockState(pos).use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        boolean afterFirst = level.getBlockState(pos).getValue(open);
        level.getBlockState(pos).use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        boolean afterSecond = level.getBlockState(pos).getValue(open);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(block.asItem()));
        var withItem = level.getBlockState(pos).use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        boolean afterItem = level.getBlockState(pos).getValue(open);
        result.addProperty("present", true);
        result.addProperty("firstResult", first.toString());
        result.addProperty("openBefore", before);
        result.addProperty("openAfterFirstClick", afterFirst);
        result.addProperty("openAfterSecondClick", afterSecond);
        result.addProperty("withOwnItemResult", withItem.toString());
        result.addProperty("openAfterClickWithOwnItem", afterItem);
        result.addProperty("pass", !before && afterFirst && !afterSecond && !afterItem && first.consumesAction() && !withItem.consumesAction());
        return result;
    }

    /** A redstone block at x=0 and wire at x=1..16 on a stone floor, at row z: the powers after placing and removing it. */
    private JsonObject line(ServerLevel level, int z) {
        int y = 100;
        for (int x = -1; x <= 18; x++) {
            level.setBlock(new BlockPos(x, y - 1, z), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        for (int x = 1; x <= 16; x++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
        }
        JsonObject run = new JsonObject();
        this.notifications = 0;
        level.setBlock(new BlockPos(0, y, z), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        JsonArray powered = this.powers(level, y, z);
        run.addProperty("notificationsPowering", this.notifications);
        this.notifications = 0;
        level.setBlock(new BlockPos(0, y, z), Blocks.AIR.defaultBlockState(), 3);
        JsonArray unpowered = this.powers(level, y, z);
        run.addProperty("notificationsUnpowering", this.notifications);
        run.add("powered", powered);
        run.add("unpowered", unpowered);
        boolean ok = true;
        for (int i = 0; i < 16; i++) {
            ok &= powered.get(i).getAsInt() == 15 - i && unpowered.get(i).getAsInt() == 0;
        }
        run.addProperty("powersAsVanilla", ok);
        return run;
    }

    private JsonArray powers(ServerLevel level, int y, int z) {
        JsonArray powers = new JsonArray();
        for (int x = 1; x <= 16; x++) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            powers.add(state.hasProperty(RedStoneWireBlock.POWER) ? state.getValue(RedStoneWireBlock.POWER) : -1);
        }
        return powers;
    }
}
