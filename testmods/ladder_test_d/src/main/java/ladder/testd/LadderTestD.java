/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testd;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Made for NeoForge 1.21.1: its mixins (ladder.testd.mixin) hook game methods whose signatures changed below and one
 * that didn't; the server start drives each hooked method once and records what the hooks saw, with the game's own
 * outcome next to it. Results go to {@code -Dladder.test.d.out}.
 */
@Mod("ladder_test_d")
public final class LadderTestD {
    static final Map<String, String> RESULTS = new TreeMap<>();
    static final BlockPos HOPPER = new BlockPos(20, 150, 20);

    public LadderTestD(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    /** A hook's record: how many times it ran and what it saw last. */
    public static synchronized void record(String hook, String seen) {
        String count = RESULTS.getOrDefault(hook + ".count", "0");
        RESULTS.put(hook + ".count", String.valueOf(Integer.parseInt(count) + 1));
        RESULTS.put(hook + ".seen", seen);
    }

    @SubscribeEvent
    public void onStarted(ServerStartedEvent event) {
        String out = System.getProperty("ladder.test.d.out");
        if (out == null) {
            return;
        }
        Map<String, String> results = new TreeMap<>();
        try {
            ServerLevel level = event.getServer().overworld();
            // eat (a parameter dropped below) and heal (unchanged)
            Zombie zombie = EntityType.ZOMBIE.create(level);
            zombie.moveTo(20.5, 160, 24.5, 0, 0);
            ItemStack left = zombie.eat(level, new ItemStack(Items.BREAD, 3));
            results.put("eat.left", left.getCount() + " " + left.getItem());
            zombie.setHealth(5);
            zombie.heal(2.5F);
            results.put("heal.health", String.valueOf(zombie.getHealth()));
            // a hopper pushing into a chest below it (a parameter added below; a call inside retyped); fresh blocks, so a
            // world kept from an earlier run (a hopper on its transfer cooldown, a chest already filled) changes nothing
            level.setBlock(HOPPER, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(HOPPER.below(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(HOPPER.below(), Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(HOPPER, Blocks.HOPPER.defaultBlockState(), 3);
            BlockState state = level.getBlockState(HOPPER);
            HopperBlockEntity hopper = (HopperBlockEntity) level.getBlockEntity(HOPPER);
            hopper.setItem(0, new ItemStack(Items.DIAMOND, 2));
            HopperBlockEntity.pushItemsTick(level, HOPPER, state, hopper);
            ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(HOPPER.below());
            results.put("hopper.chest", chest.getItem(0).getCount() + " " + chest.getItem(0).getItem());
            results.put("hopper.left", hopper.getItem(0).getCount() + " " + hopper.getItem(0).getItem());
            // an explosion that breaks nothing (a parameter whose type changed below)
            level.explode(null, 20.5, 170, 30.5, 1.5F, Level.ExplosionInteraction.NONE);
        } catch (Throwable e) {
            results.put("error", e + (e.getCause() == null ? "" : " / " + e.getCause()));
        }
        synchronized (LadderTestD.class) {
            results.putAll(RESULTS);
        }
        StringBuilder json = new StringBuilder("{\n");
        results.forEach((k, v) -> json.append("  \"").append(k).append("\": \"").append(v.replace("\"", "'")).append("\",\n"));
        json.setLength(json.length() - 2);
        json.append("\n}\n");
        try {
            Files.writeString(Path.of(out), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
