/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The test's payloads, each with another way of making its stream codec. */
final class Payloads {
    private Payloads() {
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("ladder_test_c", path);
    }

    /** Client -> server: plain values, a list and an optional (StreamCodec.composite of ByteBufCodecs). */
    record Ping(int n, String text, BlockPos pos, UUID uuid, List<Integer> numbers, Optional<ResourceLocation> tag) implements CustomPacketPayload {
        static final Type<Ping> TYPE = new Type<>(id("ping"));
        static final StreamCodec<ByteBuf, Ping> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Ping::n,
                ByteBufCodecs.STRING_UTF8, Ping::text,
                BlockPos.STREAM_CODEC, Ping::pos,
                UUIDUtil.STREAM_CODEC, Ping::uuid,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Ping::numbers,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Ping::tag,
                Ping::new);

        @Override
        public Type<Ping> type() {
            return TYPE;
        }
    }

    /** Both ways: registry-aware values (an item stack with a name, text, a block by its registry id). */
    record Gift(ItemStack stack, Component note, Block block) implements CustomPacketPayload {
        static final Type<Gift> TYPE = new Type<>(id("gift"));
        static final StreamCodec<RegistryFriendlyByteBuf, Gift> CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Gift::stack,
                ComponentSerialization.STREAM_CODEC, Gift::note,
                ByteBufCodecs.registry(Registries.BLOCK), Gift::block,
                Gift::new);

        @Override
        public Type<Gift> type() {
            return TYPE;
        }
    }

    /** Server -> client: the answers, written by hand (CustomPacketPayload.codec of a write method and a constructor). */
    record Pong(String what, int n, boolean mainThread) implements CustomPacketPayload {
        static final Type<Pong> TYPE = new Type<>(id("pong"));
        static final StreamCodec<FriendlyByteBuf, Pong> CODEC = CustomPacketPayload.codec(Pong::write, Pong::new);

        Pong(FriendlyByteBuf buffer) {
            this(buffer.readUtf(), buffer.readVarInt(), buffer.readBoolean());
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(this.what);
            buffer.writeVarInt(this.n);
            buffer.writeBoolean(this.mainThread);
        }

        @Override
        public Type<Pong> type() {
            return TYPE;
        }
    }

    /**
     * Server -> client: collections, a map, an optional and nullables written with FriendlyByteBuf's own helpers, which
     * take stream codecs since 1.20.5 (the static nullable ones too).
     */
    record Bag(List<String> names, Set<String> tags, Map<String, Integer> counts, Optional<UUID> owner, @Nullable String maybe,
            @Nullable Integer maybeStatic) implements CustomPacketPayload, Writes {
        static final Type<Bag> TYPE = new Type<>(id("bag"));
        // written through the mod's own interface: its write is the same family as Bag's
        static final StreamCodec<FriendlyByteBuf, Bag> CODEC = CustomPacketPayload.codec((bag, buffer) -> ((Writes) bag).write(buffer), Bag::new);

        Bag(FriendlyByteBuf buffer) {
            this(buffer.readList(ByteBufCodecs.STRING_UTF8), buffer.readCollection(LinkedHashSet::new, ByteBufCodecs.STRING_UTF8),
                    buffer.readMap(size -> new TreeMap<>(), ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), buffer.readOptional(UUIDUtil.STREAM_CODEC),
                    buffer.readNullable(ByteBufCodecs.STRING_UTF8), FriendlyByteBuf.readNullable(buffer, ByteBufCodecs.VAR_INT));
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeCollection(this.names, ByteBufCodecs.STRING_UTF8);
            buffer.writeCollection(this.tags, ByteBufCodecs.STRING_UTF8);
            buffer.writeMap(this.counts, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);
            buffer.writeOptional(this.owner, UUIDUtil.STREAM_CODEC);
            buffer.writeNullable(this.maybe, ByteBufCodecs.STRING_UTF8);
            FriendlyByteBuf.writeNullable(buffer, this.maybeStatic, ByteBufCodecs.VAR_INT);
        }

        @Override
        public Type<Bag> type() {
            return TYPE;
        }
    }

    /** The mod's own interface of things that write themselves (1.20.4's CustomPacketPayload has a method like it). */
    interface Writes {
        void write(FriendlyByteBuf buffer);
    }

    /**
     * Server -> client: one id with a codec for the configuration phase and another for play; sent in play, the play
     * codec writes it (the configuration one would make the play reader read something else).
     */
    record Phase(int n) implements CustomPacketPayload {
        static final Type<Phase> TYPE = new Type<>(id("phase"));
        static final StreamCodec<FriendlyByteBuf, Phase> CONFIGURATION_CODEC = StreamCodec.of(
                (buffer, phase) -> {
                    buffer.writeUtf("configuration");
                    buffer.writeVarInt(phase.n() + 1000);
                },
                buffer -> {
                    buffer.readUtf();
                    return new Phase(buffer.readVarInt() - 1000);
                });
        static final StreamCodec<RegistryFriendlyByteBuf, Phase> PLAY_CODEC = ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(Phase::new, Phase::n);

        @Override
        public Type<Phase> type() {
            return TYPE;
        }
    }

    /** Client -> server, handled on the network thread: its work goes to the main thread with enqueueWork. */
    record Net(int n) implements CustomPacketPayload {
        static final Type<Net> TYPE = new Type<>(id("net"));
        static final StreamCodec<ByteBuf, Net> CODEC = ByteBufCodecs.VAR_INT.map(Net::new, Net::n);

        @Override
        public Type<Net> type() {
            return TYPE;
        }
    }
}
