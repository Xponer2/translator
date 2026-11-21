/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.FastColor;

/**
 * 1.20.5's {@code ColorParticleOption} (a P row): particle options of one ARGB colour, for a mod's own particle type
 * (1.20.4's own coloured particles are simple ones). Its codecs are 1.20.5's in shape: {"color": argb} and the int.
 */
public class ColorParticleOption implements ParticleOptions {
    private final ParticleType<ColorParticleOption> type;
    private final int color;

    public static MapCodec<ColorParticleOption> codec(ParticleType<ColorParticleOption> type) {
        return Codec.INT.xmap(color -> new ColorParticleOption(type, color), option -> option.color).fieldOf("color");
    }

    public static StreamCodec<? super ByteBuf, ColorParticleOption> streamCodec(ParticleType<ColorParticleOption> type) {
        return StreamCodec.of((buf, option) -> buf.writeInt(option.color), buf -> new ColorParticleOption(type, buf.readInt()));
    }

    private ColorParticleOption(ParticleType<ColorParticleOption> type, int color) {
        this.type = type;
        this.color = color;
    }

    @Override
    public ParticleType<ColorParticleOption> getType() {
        return this.type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(this.color);
    }

    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(this.type) + " " + this.color;
    }

    public float getRed() {
        return FastColor.ARGB32.red(this.color) / 255.0F;
    }

    public float getGreen() {
        return FastColor.ARGB32.green(this.color) / 255.0F;
    }

    public float getBlue() {
        return FastColor.ARGB32.blue(this.color) / 255.0F;
    }

    public float getAlpha() {
        return FastColor.ARGB32.alpha(this.color) / 255.0F;
    }

    public static ColorParticleOption create(ParticleType<ColorParticleOption> type, int color) {
        return new ColorParticleOption(type, color);
    }

    public static ColorParticleOption create(ParticleType<ColorParticleOption> type, float red, float green, float blue) {
        return create(type, FastColor.ARGB32.color(255, (int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F)));
    }
}
