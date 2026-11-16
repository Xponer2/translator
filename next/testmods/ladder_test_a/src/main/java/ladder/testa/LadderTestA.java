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
package ladder.testa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Made for NeoForge 1.21.1. At the server's start it uses mob effects and attributes through 1.20.5's Holder API (a
 * Holder field, a constructor and methods taking holders) and ResourceLocation's 1.21 factory, then writes what it
 * saw to {@code -Dladder.test.out}. The same results on the real 1.21.1 and converted on 1.20.4 = the bridges work.
 */
@Mod("ladder_test_a")
public final class LadderTestA {
    public LadderTestA(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onStarted(ServerStartedEvent event) {
        String out = System.getProperty("ladder.test.out");
        if (out == null) {
            return;
        }
        Map<String, String> results = new TreeMap<>();
        try {
            ServerLevel level = event.getServer().overworld();
            ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            stand.setPos(0.5, 100, 0.5);
            Holder<MobEffect> regeneration = MobEffects.REGENERATION;
            results.put("effectHolderKey", String.valueOf(regeneration.unwrapKey().map(k -> k.location().toString()).orElse("none")));
            boolean added = stand.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            results.put("addEffect", String.valueOf(added));
            results.put("hasRegeneration", String.valueOf(stand.hasEffect(MobEffects.REGENERATION)));
            results.put("hasPoison", String.valueOf(stand.hasEffect(MobEffects.POISON)));
            MobEffectInstance instance = stand.getEffect(MobEffects.REGENERATION);
            results.put("amplifier", String.valueOf(instance == null ? -1 : instance.getAmplifier()));
            results.put("duration", String.valueOf(instance == null ? -1 : instance.getDuration()));
            Holder<Attribute> maxHealth = Attributes.MAX_HEALTH;
            AttributeInstance health = stand.getAttribute(maxHealth);
            results.put("maxHealthBase", String.valueOf(health == null ? -1 : health.getBaseValue()));
            results.put("maxHealthValue", String.valueOf(stand.getAttributeValue(Attributes.MAX_HEALTH)));
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ladder_test_a", "probe");
            results.put("resourceLocation", id.toString());
            results.put("blockAtOrigin", String.valueOf(level.getBlockState(BlockPos.ZERO.above(64)).isAir()));
        } catch (RuntimeException | LinkageError e) {
            results.put("error", e.toString());
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
        event.getServer().halt(false);
    }
}
