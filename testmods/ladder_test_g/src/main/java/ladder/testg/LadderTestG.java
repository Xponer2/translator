/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Test mod G, made for NeoForge 1.21.1: NeoForge's fluid ingredients (a recipe's sized fluid ingredient, as Create's
 * filling and mixing recipes have them). Its results go beside test mod B's (test-g.json).
 */
@Mod("ladder_test_g")
public final class LadderTestG {
    public LadderTestG(IEventBus modBus) {
        // (before test mod B writes its results, which the test runs wait for)
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, false, ServerStartedEvent.class, this::onStarted);
    }

    private void onStarted(ServerStartedEvent event) {
        String b = System.getProperty("ladder.test.b.out");
        if (b == null) {
            return;
        }
        Map<String, String> results = new TreeMap<>();
        try {
            SizedFluidIngredient water = SizedFluidIngredient.FLAT_CODEC.parse(JsonOps.INSTANCE,
                    JsonParser.parseString("{\"type\":\"neoforge:single\",\"amount\":500,\"fluid\":\"minecraft:water\"}")).getOrThrow();
            SizedFluidIngredient lava = SizedFluidIngredient.FLAT_CODEC.parse(JsonOps.INSTANCE,
                    JsonParser.parseString("{\"type\":\"neoforge:tag\",\"amount\":100,\"tag\":\"minecraft:lava\"}")).getOrThrow();
            results.put("fluidTypes", String.valueOf(NeoForgeRegistries.FLUID_INGREDIENT_TYPES.keySet().size()));
            results.put("single", water.amount() + "/" + water.test(new FluidStack(Fluids.WATER, 1000)) + "/" + water.test(new FluidStack(Fluids.LAVA, 1000)));
            results.put("tag", lava.amount() + "/" + lava.test(new FluidStack(Fluids.LAVA, 1000)) + "/" + lava.test(new FluidStack(Fluids.WATER, 50)));
        } catch (RuntimeException | LinkageError e) {
            results.put("error", e.toString());
        }
        // the mod's NeoForge overlays: one always applied, one whose condition (a mod not loaded) leaves it out
        net.minecraft.world.item.ItemStack stick = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);
        results.put("overlays", stick.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ladder_test_g", "on"))) + "/"
                + stick.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ladder_test_g", "off"))));
        try {
            Files.writeString(Path.of(b).resolveSibling("test-g.json"), new GsonBuilder().setPrettyPrinting().create().toJson(results),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
