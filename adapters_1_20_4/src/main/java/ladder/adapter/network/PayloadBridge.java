/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import java.util.function.Function;

import javax.annotation.Nullable;

import io.netty.buffer.Unpooled;
import io.netty.channel.local.LocalChannel;

import ladder.adapter.provided.network.RegisterPayloadHandlersEvent;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import ladder.carried.net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type;
import ladder.carried.net.neoforged.neoforge.network.registration.HandlerThread;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IConfigurationPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

/**
 * 1.20.5's payload registration on 20.4's: the new event is posted to every mod from 20.4's, and each registration
 * becomes 20.4's (a reader from the codec, the handlers per side, run on the thread 1.21 runs them on).
 */
public final class PayloadBridge {
    @Nullable
    private static volatile Function<String, IPayloadRegistrar> current;

    private PayloadBridge() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlerEvent.class, PayloadBridge::post);
    }

    private static void post(RegisterPayloadHandlerEvent event) {
        current = event::registrar;
        try {
            ModLoader.get().postEvent(new RegisterPayloadHandlersEvent(event::registrar));
        } finally {
            current = null;
        }
    }

    /** 20.4's registrars while the registration event runs (1.21's {@code new PayloadRegistrar(version)} needs them). */
    public static Function<String, IPayloadRegistrar> current() {
        Function<String, IPayloadRegistrar> registrars = current;
        if (registrars == null) {
            throw new IllegalStateException("Payloads can only be registered during RegisterPayloadHandlersEvent");
        }
        return registrars;
    }

    public static <T extends CustomPacketPayload> void play(IPayloadRegistrar registrar, CustomPacketPayload$Type<T> type, StreamCodec<?, T> codec,
            @Nullable IPayloadHandler<T> client, @Nullable IPayloadHandler<T> server, HandlerThread thread) {
        Payloads.register(type.id(), codec, true, true);
        registrar.play(type.id(), Payloads.reader(codec, true), handlers -> {
            if (client != null) {
                handlers.client(play(client, thread, codec));
            }
            if (server != null) {
                handlers.server(play(server, thread, codec));
            }
        });
    }

    public static <T extends CustomPacketPayload> void configuration(IPayloadRegistrar registrar, CustomPacketPayload$Type<T> type, StreamCodec<?, T> codec,
            @Nullable IPayloadHandler<T> client, @Nullable IPayloadHandler<T> server, HandlerThread thread) {
        Payloads.register(type.id(), codec, false, false);
        registrar.configuration(type.id(), Payloads.reader(codec, false), handlers -> {
            if (client != null) {
                handlers.client(configuration(client, thread, codec));
            }
            if (server != null) {
                handlers.server(configuration(server, thread, codec));
            }
        });
    }

    public static <T extends CustomPacketPayload> void common(IPayloadRegistrar registrar, CustomPacketPayload$Type<T> type, StreamCodec<?, T> codec,
            @Nullable IPayloadHandler<T> client, @Nullable IPayloadHandler<T> server, HandlerThread thread) {
        Payloads.register(type.id(), codec, false, true);
        Payloads.register(type.id(), codec, false, false);
        registrar.common(type.id(), Payloads.reader(codec, false), handlers -> {
            if (client != null) {
                handlers.client(common(client, thread, codec));
            }
            if (server != null) {
                handlers.server(common(server, thread, codec));
            }
        });
    }

    private static <T extends CustomPacketPayload> IPlayPayloadHandler<T> play(IPayloadHandler<T> handler, HandlerThread thread, StreamCodec<?, T> codec) {
        return (payload, context) -> {
            T received = received(payload, context, codec, true);
            run(thread, context, () -> handler.handle(received, context));
        };
    }

    private static <T extends CustomPacketPayload> IConfigurationPayloadHandler<T> configuration(IPayloadHandler<T> handler, HandlerThread thread,
            StreamCodec<?, T> codec) {
        return (payload, context) -> {
            T received = received(payload, context, codec, false);
            run(thread, context, () -> handler.handle(received, context));
        };
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> common(IPayloadHandler<T> handler, HandlerThread thread, StreamCodec<?, T> codec) {
        return (payload, context) -> {
            T received = received(payload, context, codec, false);
            run(thread, context, () -> handler.handle(received, context));
        };
    }

    /**
     * The payload the mod's handler gets. Over a network connection 20.4 decoded it already. In single player 20.4 hands
     * over the sender's own object (in a phase wrapper perhaps), where 1.21 encodes and decodes even there: the receiver
     * gets a fresh copy (its own collections, not the sender's), so it is made the same way, with this registration's
     * codec.
     */
    @SuppressWarnings("unchecked")
    private static <T> T received(T payload, IPayloadContext context, StreamCodec<?, T> codec, boolean registries) {
        Object sent = Payloads.unwrap(payload);
        if (context.channelHandlerContext() == null || !(context.channelHandlerContext().channel() instanceof LocalChannel)) {
            return (T) sent;
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new Payloads.Entry(codec, registries).encode(buffer, sent);
            return Payloads.reader(codec, registries).apply(buffer);
        } finally {
            buffer.release();
        }
    }

    /** 1.21 runs a handler on the game's main thread unless it was registered with {@code executesOn(NETWORK)}. */
    private static void run(HandlerThread thread, IPayloadContext context, Runnable task) {
        if (thread == HandlerThread.NETWORK) {
            task.run();
        } else {
            context.workHandler().execute(task);
        }
    }
}
