/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.effect;

import java.util.function.BiConsumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 1.20.5's effect instance API on 1.20.4's: the effect is a Holder<MobEffect> above, the MobEffect itself below.
 * <p>
 * The codecs: 1.20.4 saves an instance with the same field names 1.21's codec uses (id, amplifier, duration, ambient,
 * show_particles, show_icon, hidden_effect) and the same defaults, so the codec is 1.20.4's own save and load over a
 * compound of any format (NBT, or JSON in data files); the stream codec sends that compound.
 */
public final class EffectAdapters {
    private static final Codec<MobEffectInstance> CODEC = CompoundTag.CODEC.comapFlatMap(tag -> {
        MobEffectInstance effect = load(tag);
        return effect != null ? DataResult.success(effect) : DataResult.error(() -> "Unknown mob effect: " + tag.getString("id"));
    }, effect -> effect.save(new CompoundTag()));

    private static final StreamCodec<RegistryFriendlyByteBuf, MobEffectInstance> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MobEffectInstance decode(RegistryFriendlyByteBuf buf) {
            CompoundTag tag = buf.readNbt();
            MobEffectInstance effect = tag == null ? null : load(tag);
            if (effect == null) {
                throw new io.netty.handler.codec.DecoderException("Unknown mob effect: " + tag);
            }
            return effect;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, MobEffectInstance effect) {
            buf.writeNbt(effect.save(new CompoundTag()));
        }
    };

    private EffectAdapters() {
    }

    /**
     * 1.20.4's MobEffectInstance.load, with the amplifier read unsigned: 1.20.4 saves it as a byte and loads a negative
     * one as 0, while 1.20.5's amplifiers go up to 255 (the hidden effect is loaded the same way).
     */
    public static MobEffectInstance load(CompoundTag tag) {
        MobEffectInstance effect = MobEffectInstance.load(tag);
        if (effect == null || tag.getByte("amplifier") >= 0) {
            return effect;
        }
        MobEffectInstance hidden = tag.contains("hidden_effect", net.minecraft.nbt.Tag.TAG_COMPOUND) ? load(tag.getCompound("hidden_effect")) : null;
        MobEffectInstance unsigned = new MobEffectInstance(effect.getEffect(), effect.getDuration(), tag.getByte("amplifier") & 255, effect.isAmbient(),
                effect.isVisible(), effect.showIcon(), hidden, effect.getFactorData());
        // the cures the loaded one has (saved ones, or none), not the defaults the constructor fills in
        unsigned.getCures().clear();
        unsigned.getCures().addAll(effect.getCures());
        return unsigned;
    }

    public static Codec<MobEffectInstance> codec() {
        return CODEC;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, MobEffectInstance> streamCodec() {
        return STREAM_CODEC;
    }

    /**
     * new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon, hiddenEffect): 1.20.4's
     * constructor of these takes the effect's factor data too, made as its shorter constructors make it.
     */
    public static MobEffectInstance create(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible,
            boolean showIcon, MobEffectInstance hiddenEffect) {
        MobEffect value = effect.value();
        return new MobEffectInstance(value, duration, amplifier, ambient, visible, showIcon, hiddenEffect, value.createFactorData());
    }

    /** new MobEffectInstance(effect) and its shorter forms: 1.20.4's constructors of the effect itself */
    public static MobEffectInstance create(Holder<MobEffect> effect) {
        return new MobEffectInstance(effect.value());
    }

    public static MobEffectInstance create(Holder<MobEffect> effect, int duration) {
        return new MobEffectInstance(effect.value(), duration);
    }

    public static MobEffectInstance create(Holder<MobEffect> effect, int duration, int amplifier) {
        return new MobEffectInstance(effect.value(), duration, amplifier);
    }

    public static MobEffectInstance create(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible) {
        return new MobEffectInstance(effect.value(), duration, amplifier, ambient, visible);
    }

    public static MobEffectInstance create(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        return new MobEffectInstance(effect.value(), duration, amplifier, ambient, visible, showIcon);
    }

    /** MobEffectInstance.is(effect): whether the instance is of that effect. */
    public static boolean is(MobEffectInstance instance, Holder<MobEffect> effect) {
        return instance.getEffect() == effect.value();
    }

    /**
     * MobEffect.createModifiers(amplifier, output): each attribute the effect changes, with its modifier at that
     * amplifier, as 1.20.4's templates make them (the effect's addAttributeModifiers adds the same ones).
     */
    public static void createModifiers(MobEffect effect, int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> output) {
        effect.getAttributeModifiers().forEach((attribute, template) ->
                output.accept(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), template.create(amplifier)));
    }
}
