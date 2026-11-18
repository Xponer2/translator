/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.teste;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Made for NeoForge 1.21.1, the reference twin of ladder_test_e_1204 (the same steps with 1.20.4's own API): it takes
 * the stack test mod B left in its chest, changes it its own way (an enchantment, a custom data key, a new name) on the
 * first run, and on the next run reports what it finds. Natively on 1.21.1 this is the expected result; the 1.20.4 twin
 * runs next to the converted test mod B, and both mods' results must be the same as here.
 */
@Mod("ladder_test_e")
public final class LadderTestE {
    static final BlockPos CHEST = new BlockPos(8, 140, 8);

    public LadderTestE(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onStarted(ServerStartedEvent event) {
        String out = System.getProperty("ladder.test.e.out");
        if (out == null) {
            return;
        }
        Map<String, String> results = new TreeMap<>();
        try {
            ServerLevel level = event.getServer().overworld();
            Holder<Enchantment> sharpness = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);
            if (!(level.getBlockEntity(CHEST) instanceof ChestBlockEntity chest)) {
                results.put("error", "no chest of test mod B");
            } else {
                ItemStack stack = chest.getItem(0);
                CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                if (!custom.copyTag().contains("e_mark")) {
                    results.put("run", "first");
                    stack.enchant(sharpness, 2);
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("e_mark", 1));
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed by E"));
                    chest.setChanged();
                } else {
                    results.put("run", "again");
                }
                results.put("name", stack.getHoverName().getString());
                results.put("k", String.valueOf(custom.copyTag().getInt("k")));
                results.put("eMark", String.valueOf(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("e_mark")));
                results.put("sharpness", String.valueOf(EnchantmentHelper.getItemEnchantmentLevel(sharpness, stack)));
                results.put("item", stack.getItem().toString() + " x" + stack.getCount());
            }
        } catch (RuntimeException | LinkageError e) {
            results.put("error", e + (e.getCause() == null ? "" : " / " + e.getCause()));
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
