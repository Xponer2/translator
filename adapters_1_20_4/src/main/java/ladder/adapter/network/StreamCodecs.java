/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import java.util.Optional;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The {@code STREAM_CODEC} fields 1.20.5 added to classes 1.20.4 already has (a class that exists below can't be
 * carried, so its new static fields come from here). The simple ones write the same bytes as 1.21.1's; items,
 * ingredients and text use 1.20.4's own network forms (both ends of a converted game are 1.20.4).
 */
public final class StreamCodecs {
    private StreamCodecs() {
    }

    private static FriendlyByteBuf friendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buf);
    }

    /** 1.21: {@code ByteBufCodecs.STRING_UTF8.map(ResourceLocation::parse, ResourceLocation::toString)}. */
    private static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = new StreamCodec<>() {
        @Override
        public ResourceLocation decode(ByteBuf buf) {
            return new ResourceLocation(friendly(buf).readUtf(32767));
        }

        @Override
        public void encode(ByteBuf buf, ResourceLocation id) {
            friendly(buf).writeUtf(id.toString(), 32767);
        }
    };

    /** 1.21: two longs (most, then least significant bits). */
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buf) {
            return friendly(buf).readUUID();
        }

        @Override
        public void encode(ByteBuf buf, UUID uuid) {
            friendly(buf).writeUUID(uuid);
        }
    };

    /** 1.21: the position packed in a long. */
    private static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = new StreamCodec<>() {
        @Override
        public BlockPos decode(ByteBuf buf) {
            return friendly(buf).readBlockPos();
        }

        @Override
        public void encode(ByteBuf buf, BlockPos pos) {
            friendly(buf).writeBlockPos(pos);
        }
    };

    /** 1.21: {@code ByteBufCodecs.idMapper(Direction::from3DDataValue, Direction::get3DDataValue)}, a var int. */
    private static final StreamCodec<ByteBuf, Direction> DIRECTION = new StreamCodec<>() {
        @Override
        public Direction decode(ByteBuf buf) {
            return Direction.from3DDataValue(friendly(buf).readVarInt());
        }

        @Override
        public void encode(ByteBuf buf, Direction direction) {
            friendly(buf).writeVarInt(direction.get3DDataValue());
        }
    };

    public static StreamCodec<ByteBuf, ResourceLocation> resourceLocation() {
        return RESOURCE_LOCATION;
    }

    public static StreamCodec<ByteBuf, UUID> uuid() {
        return UUID_CODEC;
    }

    public static StreamCodec<ByteBuf, BlockPos> blockPos() {
        return BLOCK_POS;
    }

    public static StreamCodec<ByteBuf, Direction> direction() {
        return DIRECTION;
    }

    /** 1.21's {@code ItemStack.OPTIONAL_STREAM_CODEC}: any stack, the empty one too (1.20.4's item form: id, count, NBT). */
    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_ITEM_STACK = new StreamCodec<>() {
        @Override
        public ItemStack decode(RegistryFriendlyByteBuf buf) {
            return buf.readItem();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ItemStack stack) {
            buf.writeItem(stack);
        }
    };

    /** 1.21's {@code ItemStack.STREAM_CODEC}: as the optional one, but an empty stack is an error both ways. */
    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK = new StreamCodec<>() {
        @Override
        public ItemStack decode(RegistryFriendlyByteBuf buf) {
            ItemStack stack = buf.readItem();
            if (stack.isEmpty()) {
                throw new DecoderException("Empty ItemStack not allowed");
            }
            return stack;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ItemStack stack) {
            if (stack.isEmpty()) {
                throw new EncoderException("Empty ItemStack not allowed");
            }
            buf.writeItem(stack);
        }
    };

    /** 1.21's {@code Ingredient.CONTENTS_STREAM_CODEC}: 1.20.4's {@code toNetwork} / {@code fromNetwork}. */
    private static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> INGREDIENT = new StreamCodec<>() {
        @Override
        public Ingredient decode(RegistryFriendlyByteBuf buf) {
            return Ingredient.fromNetwork(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, Ingredient ingredient) {
            ingredient.toNetwork(buf);
        }
    };

    /** 1.21's {@code ComponentSerialization} text codecs (with or without registries): 1.20.4's network text (NBT). */
    private static final StreamCodec<ByteBuf, Component> COMPONENT = new StreamCodec<>() {
        @Override
        public Component decode(ByteBuf buf) {
            return friendly(buf).readComponent();
        }

        @Override
        public void encode(ByteBuf buf, Component component) {
            friendly(buf).writeComponent(component);
        }
    };

    /** 1.21's {@code ComponentSerialization.OPTIONAL_STREAM_CODEC}: a presence flag, then the text. */
    private static final StreamCodec<ByteBuf, Optional<Component>> OPTIONAL_COMPONENT = new StreamCodec<>() {
        @Override
        public Optional<Component> decode(ByteBuf buf) {
            return buf.readBoolean() ? Optional.of(friendly(buf).readComponent()) : Optional.empty();
        }

        @Override
        public void encode(ByteBuf buf, Optional<Component> component) {
            buf.writeBoolean(component.isPresent());
            component.ifPresent(c -> friendly(buf).writeComponent(c));
        }
    };

    public static StreamCodec<RegistryFriendlyByteBuf, ItemStack> itemStack() {
        return ITEM_STACK;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ItemStack> optionalItemStack() {
        return OPTIONAL_ITEM_STACK;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, Ingredient> ingredient() {
        return INGREDIENT;
    }

    public static StreamCodec<ByteBuf, Component> component() {
        return COMPONENT;
    }

    public static StreamCodec<ByteBuf, Optional<Component>> optionalComponent() {
        return OPTIONAL_COMPONENT;
    }
}
