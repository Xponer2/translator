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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Made for NeoForge 1.20.4 (not converted): the twin of ladder_test_e (1.21.1) with 1.20.4's own API, the stack's NBT.
 * Next to the converted test mod B it changes the stack B left in its chest (an enchantment, a key of the stack's tag,
 * a new name) and reports what it finds after a restart. Its results and B's must be those of the 1.21.1 twin.
 */
@Mod("ladder_test_e")
public final class LadderTestE {
    static final BlockPos CHEST = new BlockPos(8, 140, 8);

    public LadderTestE() {
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
            if (!(level.getBlockEntity(CHEST) instanceof ChestBlockEntity chest)) {
                results.put("error", "no chest of test mod B");
            } else {
                ItemStack stack = chest.getItem(0);
                CompoundTag tag = stack.getTag();
                if (tag == null || !tag.contains("e_mark")) {
                    results.put("run", "first");
                    stack.enchant(Enchantments.SHARPNESS, 2);
                    stack.getOrCreateTag().putInt("e_mark", 1);
                    stack.setHoverName(Component.literal("Renamed by E"));
                    chest.setChanged();
                } else {
                    results.put("run", "again");
                }
                CompoundTag now = stack.getOrCreateTag();
                results.put("name", stack.getHoverName().getString());
                results.put("k", String.valueOf(now.getInt("k")));
                results.put("eMark", String.valueOf(now.getInt("e_mark")));
                results.put("sharpness", String.valueOf(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack)));
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
