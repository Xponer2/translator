/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
    private final net.neoforged.fml.ModContainer container;

    public LadderTestA(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        this.container = container;
        NeoForge.EVENT_BUS.register(this);
        container.registerExtensionPoint(ProbePoint.class, new ProbePoint("point"));
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(new ClientVertexTest());
        }
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
            // a branch inside a bridged constructor's arguments (stack map frames with the new object in them)
            boolean longer = level.getGameTime() >= 0;
            MobEffectInstance speed = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, longer ? 300 : 100, longer ? 2 : 0);
            stand.addEffect(speed);
            results.put("ternaryDuration", String.valueOf(speed.getDuration()));
            // method references of bridged members: bound, unbound, a constructor
            java.util.function.Predicate<Holder<MobEffect>> has = stand::hasEffect;
            results.put("boundRefHas", String.valueOf(has.test(MobEffects.MOVEMENT_SPEED)));
            java.util.function.BiPredicate<ArmorStand, Holder<MobEffect>> hasOn = ArmorStand::hasEffect;
            results.put("unboundRefHas", String.valueOf(hasOn.test(stand, MobEffects.POISON)));
            java.util.function.BiFunction<Holder<MobEffect>, Integer, MobEffectInstance> make = MobEffectInstance::new;
            results.put("constructorRefDuration", String.valueOf(make.apply(MobEffects.LUCK, 77).getDuration()));
            // an override of a bridged method (with a super call): the mod's calls, and the game's own lower call
            ProbeStand probe = new ProbeStand(EntityType.ARMOR_STAND, level);
            probe.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            results.put("overrideLuck", String.valueOf(probe.hasEffect(MobEffects.LUCK)));
            results.put("overrideSuperRegeneration", String.valueOf(probe.hasEffect(MobEffects.REGENERATION)));
            results.put("overrideSuperPoison", String.valueOf(probe.hasEffect(MobEffects.POISON)));
            net.minecraft.world.entity.LivingEntity asLiving = probe;
            results.put("overrideThroughBase", String.valueOf(asLiving.hasEffect(MobEffects.LUCK)));
            // two levels of overrides, the lower one's super call going to the upper mod class
            ProbeStand2 probe2 = new ProbeStand2(EntityType.ARMOR_STAND, level);
            results.put("override2Unluck", String.valueOf(probe2.hasEffect(MobEffects.UNLUCK)));
            results.put("override2Luck", String.valueOf(probe2.hasEffect(MobEffects.LUCK)));
            results.put("override2Poison", String.valueOf(probe2.hasEffect(MobEffects.POISON)));
            // an extension point of an ordinary class, read back
            results.put("extensionPoint", this.container.getCustomExtension(ProbePoint.class).map(p -> p.value).orElse("none"));
            MixinTest.run(stand, results);
            DfuTest.run(results);
            SmallApiTest.run(results);
            SmallApiTest.blockEntity(level, results);
            // what another mod (test mod B) handed over in a chest, read by its component type's id (no code of B's)
            if (level.getBlockEntity(new BlockPos(8, 140, 8)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                net.minecraft.world.item.ItemStack handed = chest.getItem(0);
                net.minecraft.core.component.DataComponentType<?> charge = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                        .get(ResourceLocation.fromNamespaceAndPath("ladder_test_b", "charge"));
                results.put("handedCharge", charge == null ? "no type" : String.valueOf(handed.get(charge)));
                results.put("handedName", handed.getHoverName().getString());
                results.put("handedCustom", String.valueOf(handed.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)));
            } else {
                results.put("handedCharge", "no chest");
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ladder_test_a", "probe");
            results.put("resourceLocation", id.toString());
            results.put("blockAboveTerrain", String.valueOf(level.getBlockState(BlockPos.ZERO.above(300)).isAir()));
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
        event.getServer().halt(false);
    }
}
