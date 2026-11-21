/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.attachment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import io.netty.buffer.Unpooled;
import ladder.adapter.mixin.AttachmentChunkMapAccess;
import ladder.adapter.mixin.AttachmentTrackedEntityAccess;
import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * NeoForge 21.1's attachment syncing on NeoForge 20.4, which syncs none. An attachment type built with {@code sync(...)}
 * (the X rows) keeps its handler here; its value goes to the clients that see the holder when they start seeing it (a
 * chunk sent with its block entities, an entity tracked, the player itself on joining, respawning or changing level:
 * AttachmentEvents) and on {@code syncData} (an X row), as 21.1 sends it. The client sets what it reads
 * (SyncAttachmentsPayload, a 20.4 payload).
 */
public final class AttachmentSync {
    /** a builder's handler, until it builds its type */
    private static final Map<AttachmentType.Builder<?>, AttachmentSyncHandler<?>> BUILDING = new WeakHashMap<>();
    /** the synced types and their handlers */
    static final Map<AttachmentType<?>, AttachmentSyncHandler<?>> SYNCED = new ConcurrentHashMap<>();

    private AttachmentSync() {
    }

    // ---------------------------------------------------------------- the builder (X rows)

    public static <T> AttachmentType.Builder<T> sync(AttachmentType.Builder<T> builder, AttachmentSyncHandler<T> handler) {
        synchronized (BUILDING) {
            BUILDING.put(builder, handler);
        }
        return builder;
    }

    public static <T> AttachmentType.Builder<T> sync(AttachmentType.Builder<T> builder, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return sync(builder, (holder, to) -> true, codec);
    }

    public static <T> AttachmentType.Builder<T> sync(AttachmentType.Builder<T> builder, BiPredicate<IAttachmentHolder, ServerPlayer> sendToPlayer,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return sync(builder, new AttachmentSyncHandler<T>() {
            @Override
            public boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
                return sendToPlayer.test(holder, to);
            }

            @Override
            public void write(RegistryFriendlyByteBuf buf, T attachment, boolean initialSync) {
                codec.encode(buf, attachment);
            }

            @Override
            public T read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable T previousValue) {
                return codec.decode(buf);
            }
        });
    }

    /** the type a builder built (mixin.AttachmentBuilderMixin): a synced one when its builder had a handler */
    public static void built(AttachmentType.Builder<?> builder, AttachmentType<?> type) {
        AttachmentSyncHandler<?> handler;
        synchronized (BUILDING) {
            handler = BUILDING.remove(builder);
        }
        if (handler != null) {
            SYNCED.put(type, handler);
        }
    }

    // ---------------------------------------------------------------- syncData (X rows)

    public static void syncData(IAttachmentHolder holder, AttachmentType<?> type) {
        if (!SYNCED.containsKey(type)) {
            return;
        }
        switch (holder) {
            case BlockEntity blockEntity when blockEntity.getLevel() instanceof ServerLevel level ->
                    send(holder, Target.of(holder), List.of(type), level.getChunkSource().chunkMap.getPlayers(new ChunkPos(blockEntity.getBlockPos()), false));
            case LevelChunk chunk when chunk.getLevel() instanceof ServerLevel level ->
                    send(holder, Target.of(holder), List.of(type), level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false));
            case Entity entity when entity.level() instanceof ServerLevel level -> send(holder, Target.of(holder), List.of(type), trackers(level, entity));
            case ServerLevel level -> send(holder, Target.of(holder), List.of(type), level.players());
            default -> {
                // a client's holder: nothing to send
            }
        }
    }

    public static void syncData(IAttachmentHolder holder, Supplier<? extends AttachmentType<?>> type) {
        syncData(holder, type.get());
    }

    /** the players that track an entity (the chunk map keeps them on its tracked entity), and a player itself */
    private static List<ServerPlayer> trackers(ServerLevel level, Entity entity) {
        List<ServerPlayer> players = new ArrayList<>();
        Object tracked = ((AttachmentChunkMapAccess) level.getChunkSource().chunkMap).ladder$attachmentEntityMap().get(entity.getId());
        if (tracked != null) {
            for (ServerPlayerConnection connection : ((AttachmentTrackedEntityAccess) tracked).ladder$attachmentSeenBy()) {
                players.add(connection.getPlayer());
            }
        }
        if (entity instanceof ServerPlayer self && !players.contains(self)) {
            // players don't track themselves
            players.add(self);
        }
        return players;
    }

    // ---------------------------------------------------------------- the first sync of what a client starts seeing

    static void chunkSent(LevelChunk chunk, ServerPlayer player) {
        sendAll(chunk, player);
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            sendAll(blockEntity, player);
        }
    }

    static void self(ServerPlayer player) {
        sendAll(player, player);
        sendAll(player.serverLevel(), player);
    }

    /** every synced attachment the holder has, to one player (21.1's initial sync) */
    static void sendAll(IAttachmentHolder holder, ServerPlayer to) {
        if (SYNCED.isEmpty() || !holder.hasAttachments()) {
            return;
        }
        List<AttachmentType<?>> types = new ArrayList<>();
        for (AttachmentType<?> type : SYNCED.keySet()) {
            if (holder.hasData(type)) {
                types.add(type);
            }
        }
        if (!types.isEmpty()) {
            send(holder, Target.of(holder), types, List.of(to));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void send(IAttachmentHolder holder, Target target, List<AttachmentType<?>> types, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            List<ResourceLocation> ids = new ArrayList<>();
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.level().registryAccess());
            try {
                for (AttachmentType<?> type : types) {
                    AttachmentSyncHandler handler = SYNCED.get(type);
                    ResourceLocation id = NeoForgeRegistries.ATTACHMENT_TYPES.getKey(type);
                    if (handler == null || id == null || !handler.sendToPlayer(holder, player)) {
                        continue;
                    }
                    ids.add(id);
                    Object value = holder.getExistingData((AttachmentType) type).orElse(null);
                    buf.writeBoolean(value != null);
                    if (value != null) {
                        handler.write(buf, value, false);
                    }
                }
                if (!ids.isEmpty()) {
                    byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    PacketDistributor.PLAYER.with(player).send(new SyncAttachmentsPayload(target, ids, data));
                }
            } finally {
                buf.release();
            }
        }
    }

    /** where a holder is, for the client to find it */
    record Target(byte kind, int entity, BlockPos pos) {
        static final byte ENTITY = 0, BLOCK_ENTITY = 1, CHUNK = 2, LEVEL = 3;

        static Target of(IAttachmentHolder holder) {
            return switch (holder) {
                case Entity entity -> new Target(ENTITY, entity.getId(), BlockPos.ZERO);
                case BlockEntity blockEntity -> new Target(BLOCK_ENTITY, 0, blockEntity.getBlockPos());
                case LevelChunk chunk -> new Target(CHUNK, 0, chunk.getPos().getWorldPosition());
                case Level level -> new Target(LEVEL, 0, BlockPos.ZERO);
                default -> throw new UnsupportedOperationException("an attachment holder 21.1 doesn't sync: " + holder);
            };
        }

        @Nullable
        IAttachmentHolder find(Level level) {
            return switch (this.kind) {
                case ENTITY -> level.getEntity(this.entity);
                case BLOCK_ENTITY -> level.getBlockEntity(this.pos);
                case CHUNK -> level.getChunkSource().getChunkNow(this.pos.getX() >> 4, this.pos.getZ() >> 4);
                case LEVEL -> level;
                default -> null;
            };
        }
    }

    /** the client: each type's value read and set (or removed), as 21.1's handler reads it */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void receive(SyncAttachmentsPayload payload, Level level) {
        IAttachmentHolder holder = payload.target().find(level);
        if (holder == null) {
            return;
        }
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), level.registryAccess());
        try {
            for (ResourceLocation id : payload.types()) {
                AttachmentType type = NeoForgeRegistries.ATTACHMENT_TYPES.get(id);
                AttachmentSyncHandler handler = type == null ? null : SYNCED.get(type);
                if (handler == null) {
                    // the rest of the buffer is that type's: nothing more can be read
                    return;
                }
                if (buf.readBoolean()) {
                    Object previous = holder.getExistingData(type).orElse(null);
                    Object value = handler.read(holder, buf, previous);
                    if (value == null) {
                        holder.removeData(type);
                    } else {
                        holder.setData(type, value);
                    }
                } else {
                    holder.removeData(type);
                }
            }
        } finally {
            buf.release();
        }
    }
}
