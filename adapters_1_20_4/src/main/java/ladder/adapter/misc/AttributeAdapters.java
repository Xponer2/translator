/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.misc;

import java.util.UUID;
import java.util.function.IntFunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 1.20.5's attribute modifier is a record (id, name, amount, operation) with codecs of its own, and its operation has
 * an id and new names (add_value, add_multiplied_base, add_multiplied_total); 1.20.4's are a class with getters and an
 * enum with toValue and the old names (addition, multiply_base, multiply_total). The record's accessors go to the
 * getters; the codecs are 1.20.6's, over 1.20.4's modifier (reading the old operation names too).
 */
public final class AttributeAdapters {
    private static final String[] NAMES = {"add_value", "add_multiplied_base", "add_multiplied_total"};
    private static final String[] OLD_NAMES = {"addition", "multiply_base", "multiply_total"};

    /** 1.20.6's Operation.CODEC: by name. */
    private static final Codec<AttributeModifier.Operation> OPERATION_CODEC = Codec.STRING.comapFlatMap(name -> {
        for (int id = 0; id < NAMES.length; id++) {
            if (NAMES[id].equals(name) || OLD_NAMES[id].equals(name)) {
                return DataResult.success(AttributeModifier.Operation.fromValue(id));
            }
        }
        return DataResult.error(() -> "Unknown attribute modifier operation: " + name);
    }, operation -> NAMES[operation.toValue()]);

    /** 1.20.6's Operation.BY_ID: an id out of range is the first operation (ByIdMap.OutOfBoundsStrategy.ZERO). */
    private static final IntFunction<AttributeModifier.Operation> OPERATION_BY_ID =
            id -> AttributeModifier.Operation.fromValue(id >= 0 && id < NAMES.length ? id : 0);

    /** 1.20.6's Operation.STREAM_CODEC: the id as a var int. */
    private static final StreamCodec<ByteBuf, AttributeModifier.Operation> OPERATION_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AttributeModifier.Operation decode(ByteBuf buf) {
            return OPERATION_BY_ID.apply(friendly(buf).readVarInt());
        }

        @Override
        public void encode(ByteBuf buf, AttributeModifier.Operation operation) {
            friendly(buf).writeVarInt(operation.toValue());
        }
    };

    /** 1.20.6's AttributeModifier.MAP_CODEC: uuid, name, amount, operation. */
    private static final MapCodec<AttributeModifier> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(AttributeModifier::getId),
            Codec.STRING.fieldOf("name").forGetter(AttributeAdapters::name),
            Codec.DOUBLE.fieldOf("amount").forGetter(AttributeModifier::getAmount),
            OPERATION_CODEC.fieldOf("operation").forGetter(AttributeModifier::getOperation)).apply(i, AttributeModifier::new));

    private static final Codec<AttributeModifier> CODEC = MAP_CODEC.codec();

    /** 1.20.6's AttributeModifier.STREAM_CODEC: the uuid, the name, the amount, the operation's id. */
    private static final StreamCodec<ByteBuf, AttributeModifier> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AttributeModifier decode(ByteBuf buf) {
            FriendlyByteBuf in = friendly(buf);
            UUID id = in.readUUID();
            String name = in.readUtf();
            double amount = in.readDouble();
            int operation = in.readVarInt();
            if (operation < 0 || operation >= NAMES.length) {
                throw new DecoderException("Unknown attribute modifier operation: " + operation);
            }
            return new AttributeModifier(id, name, amount, AttributeModifier.Operation.fromValue(operation));
        }

        @Override
        public void encode(ByteBuf buf, AttributeModifier modifier) {
            FriendlyByteBuf out = friendly(buf);
            out.writeUUID(modifier.getId());
            out.writeUtf(name(modifier));
            out.writeDouble(modifier.getAmount());
            out.writeVarInt(modifier.getOperation().toValue());
        }
    };

    private AttributeAdapters() {
    }

    private static FriendlyByteBuf friendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buf);
    }

    /** The modifier's name: 1.20.4 keeps it private and gives it only in its save. */
    private static String name(AttributeModifier modifier) {
        return modifier.save().getString("Name");
    }

    /** AttributeModifier.Operation.id(): the operation's number (0 add, 1 multiply base, 2 multiply total). */
    public static int operationId(AttributeModifier.Operation operation) {
        return operation.toValue();
    }

    public static Codec<AttributeModifier.Operation> operationCodec() {
        return OPERATION_CODEC;
    }

    public static IntFunction<AttributeModifier.Operation> operationById() {
        return OPERATION_BY_ID;
    }

    public static StreamCodec<ByteBuf, AttributeModifier.Operation> operationStreamCodec() {
        return OPERATION_STREAM_CODEC;
    }

    /** AttributeModifier.id() (a UUID until 1.21). */
    public static UUID id(AttributeModifier modifier) {
        return modifier.getId();
    }

    /** AttributeModifier.name(). */
    public static String modifierName(AttributeModifier modifier) {
        return name(modifier);
    }

    /** AttributeModifier.amount(). */
    public static double amount(AttributeModifier modifier) {
        return modifier.getAmount();
    }

    /** AttributeModifier.operation(). */
    public static AttributeModifier.Operation operation(AttributeModifier modifier) {
        return modifier.getOperation();
    }

    public static MapCodec<AttributeModifier> mapCodec() {
        return MAP_CODEC;
    }

    public static Codec<AttributeModifier> codec() {
        return CODEC;
    }

    public static StreamCodec<ByteBuf, AttributeModifier> streamCodec() {
        return STREAM_CODEC;
    }

    /**
     * EquipmentSlot.BODY, 1.20.5's slot of an animal's armour: 1.20.4 keeps a horse's armour in its CHEST slot
     * (Horse.setArmor), so a mod's BODY is the slot 1.20.4 has for it. A switch over the slots in carried code sees
     * BODY's case for CHEST then: EquipmentSlotGroup.bySlot(CHEST) is the BODY group, whose test is the same.
     */
    public static EquipmentSlot bodySlot() {
        return EquipmentSlot.CHEST;
    }

    // 1.20.5's attributes NeoForge 20.4 has as its own (the same bases): block and entity interaction range (reach), step
    // height, gravity

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> blockInteractionRange() {
        return net.neoforged.neoforge.common.NeoForgeMod.BLOCK_REACH;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> entityInteractionRange() {
        return net.neoforged.neoforge.common.NeoForgeMod.ENTITY_REACH;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> stepHeight() {
        return net.neoforged.neoforge.common.NeoForgeMod.STEP_HEIGHT;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> gravity() {
        return net.neoforged.neoforge.common.NeoForgeMod.ENTITY_GRAVITY;
    }

    /** 1.20.5's {@code player.blockInteractionRange()}: NeoForge 20.4's block reach */
    public static double blockInteractionRange(net.minecraft.world.entity.player.Player player) {
        return player.getBlockReach();
    }

    /** 1.20.5's {@code player.entityInteractionRange()}: NeoForge 20.4's entity reach */
    public static double entityInteractionRange(net.minecraft.world.entity.player.Player player) {
        return player.getEntityReach();
    }
}
