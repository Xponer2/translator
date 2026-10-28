package io.github.xponer2.translator.network;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The 1.12.2 side of the payloads: one FML channel per namespace of the payload ids (the mod id, cut to the 20
 * characters a channel name may have). A packet holds the payload id, then what its stream codec writes. Received
 * payloads are decoded on the network thread and handled on the main thread, like on NeoForge.
 */
public final class NetworkChannels {
    private static final Logger LOGGER = LogManager.getLogger("Cucumber Network");
    private static final int MAX_CHANNEL_NAME_LENGTH = 20;
    private static final Map<ResourceLocation, Registration<?>> PAYLOADS = new ConcurrentHashMap<>();
    private static final Map<String, FMLEventChannel> CHANNELS = new ConcurrentHashMap<>();

    private NetworkChannels() { }

    static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                         @Nullable IPayloadHandler<T> clientHandler, @Nullable IPayloadHandler<T> serverHandler) {
        if (PAYLOADS.putIfAbsent(type.id(), new Registration<>(codec, clientHandler, serverHandler)) != null) {
            throw new IllegalStateException("Payload " + type.id() + " is registered twice");
        }
        channel(type.id().getNamespace());
    }

    private static String channelName(String namespace) {
        return namespace.length() <= MAX_CHANNEL_NAME_LENGTH ? namespace : namespace.substring(0, MAX_CHANNEL_NAME_LENGTH);
    }

    private static FMLEventChannel channel(String namespace) {
        return CHANNELS.computeIfAbsent(channelName(namespace), name -> {
            var channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(name);
            channel.register(new Listener());
            return channel;
        });
    }

    static FMLEventChannel channel(CustomPacketPayload payload) {
        return channel(payload.type().id().getNamespace());
    }

    /**
     * A new packet of a payload; {@code toClient} tells the direction, which must have been registered.
     */
    @SuppressWarnings("unchecked")
    static FMLProxyPacket packet(CustomPacketPayload payload, boolean toClient) {
        var id = payload.type().id();
        var registration = (Registration<CustomPacketPayload>) PAYLOADS.get(id);
        if (registration == null) {
            throw new IllegalStateException("Payload " + id + " isn't registered");
        }
        if ((toClient ? registration.clientHandler : registration.serverHandler) == null) {
            throw new IllegalStateException("Payload " + id + " isn't registered to be sent to the " + (toClient ? "client" : "server"));
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer());
        buffer.writeResourceLocation(id);
        registration.codec.encode(buffer, payload);
        return new FMLProxyPacket(buffer, channelName(id.getNamespace()));
    }

    @Nullable
    private static Received decode(FMLProxyPacket packet) {
        var buffer = RegistryFriendlyByteBuf.wrap(packet.payload());
        var id = buffer.readResourceLocation();
        var registration = PAYLOADS.get(id);
        if (registration == null) {
            LOGGER.warn("Received unknown payload {}", id);
            return null;
        }
        return new Received(id, registration, registration.codec.decode(buffer));
    }

    @SuppressWarnings("unchecked")
    static <T extends CustomPacketPayload> void handle(IPayloadHandler<T> handler, CustomPacketPayload payload, IPayloadContext context) {
        try {
            handler.handle((T) payload, context);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to handle payload {}", payload.type().id(), e);
        }
    }

    /**
     * Receives the packets of the channels; must be public for the event bus.
     */
    public static final class Listener {
        @SubscribeEvent
        public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
            var received = decode(event.getPacket());
            if (received == null) {
                return;
            }
            if (received.registration.serverHandler == null) {
                LOGGER.warn("Received payload {} on the server, but it is only sent to clients", received.id);
                return;
            }
            EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
            var server = player.getServer();
            if (server == null) {
                return;
            }
            var context = new Context(() -> player, server, payload -> PacketDistributor.sendToPlayer(player, payload));
            server.addScheduledTask(() -> handle(received.registration.serverHandler, received.payload, context));
        }

        @SubscribeEvent
        public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
            var received = decode(event.getPacket());
            if (received == null) {
                return;
            }
            if (received.registration.clientHandler == null) {
                LOGGER.warn("Received payload {} on the client, but it is only sent to the server", received.id);
                return;
            }
            ClientPayloads.handle(received.registration.clientHandler, received.payload);
        }
    }

    private static final class Registration<T extends CustomPacketPayload> {
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec;
        @Nullable
        final IPayloadHandler<T> clientHandler;
        @Nullable
        final IPayloadHandler<T> serverHandler;

        Registration(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, @Nullable IPayloadHandler<T> clientHandler,
                     @Nullable IPayloadHandler<T> serverHandler) {
            this.codec = codec;
            this.clientHandler = clientHandler;
            this.serverHandler = serverHandler;
        }
    }

    private static final class Received {
        final ResourceLocation id;
        final Registration<?> registration;
        final CustomPacketPayload payload;

        Received(ResourceLocation id, Registration<?> registration, CustomPacketPayload payload) {
            this.id = id;
            this.registration = registration;
            this.payload = payload;
        }
    }

    static final class Context implements IPayloadContext {
        private final Supplier<EntityPlayer> player;
        private final IThreadListener thread;
        private final Consumer<CustomPacketPayload> reply;

        Context(Supplier<EntityPlayer> player, IThreadListener thread, Consumer<CustomPacketPayload> reply) {
            this.player = player;
            this.thread = thread;
            this.reply = reply;
        }

        @Override
        public EntityPlayer player() {
            return this.player.get();
        }

        @Override
        public CompletableFuture<Void> enqueueWork(Runnable task) {
            return this.enqueueWork(() -> {
                task.run();
                return null;
            });
        }

        @Override
        public <T> CompletableFuture<T> enqueueWork(Supplier<T> task) {
            var future = new CompletableFuture<T>();
            Runnable run = () -> {
                try {
                    future.complete(task.get());
                } catch (RuntimeException e) {
                    LOGGER.error("Failed to run the work of a payload", e);
                    future.completeExceptionally(e);
                }
            };
            if (this.thread.isCallingFromMinecraftThread()) {
                run.run();
            } else {
                this.thread.addScheduledTask(run);
            }
            return future;
        }

        @Override
        public void reply(CustomPacketPayload payload) {
            this.reply.accept(payload);
        }
    }
}
