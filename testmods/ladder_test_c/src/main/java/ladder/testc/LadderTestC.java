/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ladder.testc.Payloads.Bag;
import ladder.testc.Payloads.Gift;
import ladder.testc.Payloads.Net;
import ladder.testc.Payloads.Ping;
import ladder.testc.Payloads.Pong;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Made for NeoForge 1.21.1: network payloads. The client (ClientSide) sends a ping, a gift and a network-thread payload
 * to the server; the server answers each through another way of sending (reply, to the player, to the dimension, to
 * all, to the trackers, near a point, to a chunk's trackers, two at once) and sends the gift back changed. The client
 * writes what arrived and on which thread to {@code -Dladder.test.c.out}.
 */
@Mod("ladder_test_c")
public final class LadderTestC {
    static final String MODID = "ladder_test_c";

    public LadderTestC(IEventBus modBus) {
        modBus.addListener(LadderTestC::register);
        Armour.register(modBus);
        Recipes.register(modBus);
        PackFinders.register(modBus);
        SpawnProbe.register(modBus);
        if (FMLEnvironment.dist.isClient()) {
            ClientSide.init();
            GuiClient.register(modBus);
        }
    }

    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(Ping.TYPE, Ping.CODEC, LadderTestC::onPing);
        registrar.playBidirectional(Gift.TYPE, Gift.CODEC, LadderTestC::onGift);
        registrar.playToClient(Pong.TYPE, Pong.CODEC, (pong, context) -> ClientSide.onPong(pong, context));
        registrar.playToClient(Bag.TYPE, Bag.CODEC, (bag, context) -> ClientSide.onBag(bag, context));
        // the configuration codec registered first: the play send must still use the play one
        registrar.configurationToClient(Payloads.Phase.TYPE, Payloads.Phase.CONFIGURATION_CODEC, (phase, context) -> {
        });
        registrar.playToClient(Payloads.Phase.TYPE, Payloads.Phase.PLAY_CODEC, (phase, context) -> ClientSide.onPhase(phase, context));
        registrar.executesOn(HandlerThread.NETWORK).playToServer(Net.TYPE, Net.CODEC, LadderTestC::onNet);
    }

    static void onPing(Ping ping, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        boolean main = player.server.isSameThread();
        context.reply(new Pong("reply " + ping, ping.n(), main));
        SpawnProbe.spawnNear(player);
        context.reply(new Bag(List.of("a", "b", "c"), Set.of("only"), Map.of("x", 1, "y", 300), Optional.of(ping.uuid()), null, 77));
        ServerLevel level = player.serverLevel();
        PacketDistributor.sendToPlayer(player, new Pong("toPlayer", 1, main));
        PacketDistributor.sendToPlayer(player, new Payloads.Phase(5));
        PacketDistributor.sendToPlayersInDimension(level, new Pong("inDimension", 2, main));
        PacketDistributor.sendToAllPlayers(new Pong("allPlayers", 3, main));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new Pong("trackingAndSelf", 4, main));
        // a player doesn't track itself: nothing arrives
        PacketDistributor.sendToPlayersTrackingEntity(player, new Pong("trackingOnly", 5, main));
        PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 32, new Pong("near", 6, main));
        // the excluded player gets nothing
        PacketDistributor.sendToPlayersNear(level, player, player.getX(), player.getY(), player.getZ(), 32, new Pong("nearExcluded", 7, main));
        PacketDistributor.sendToPlayersTrackingChunk(level, player.chunkPosition(), new Pong("trackingChunk", 8, main));
        PacketDistributor.sendToPlayer(player, new Pong("pairFirst", 9, main), new Pong("pairSecond", 10, main));
    }

    static String describe(Gift gift) {
        return gift.stack().getCount() + "x" + BuiltInRegistries.ITEM.getKey(gift.stack().getItem()) + " named '" + gift.stack().getHoverName().getString()
                + "' note '" + gift.note().getString() + "' (" + gift.note().getStyle().getColor() + ") block " + BuiltInRegistries.BLOCK.getKey(gift.block());
    }

    static void onGift(Gift gift, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            ClientSide.onGift(gift, context);
            return;
        }
        ServerPlayer player = (ServerPlayer) context.player();
        boolean main = player.server.isSameThread();
        context.reply(new Pong("gift " + describe(gift), gift.stack().getCount(), main));
        var back = gift.stack().copyWithCount(gift.stack().getCount() + 1);
        PacketDistributor.sendToPlayer(player, new Gift(back, Component.literal("back: " + gift.note().getString()), Blocks.GOLD_BLOCK));
    }

    static void onNet(Net net, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        boolean handlerMain = player.server.isSameThread();
        context.enqueueWork(() -> PacketDistributor.sendToPlayer(player,
                new Pong("net " + net.n() + " handlerOnMain=" + handlerMain, net.n(), player.server.isSameThread())));
    }
}
