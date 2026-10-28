package io.github.xponer2.translator.component;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.StreamCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;

/**
 * The value of {@link DataComponents#UNBREAKABLE} (1.21 {@code net.minecraft.world.item.component.Unbreakable}).
 */
public final class Unbreakable {
    public static final Codec<Unbreakable> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(Unbreakable::showInTooltip)
    ).apply(instance, Unbreakable::new));
    public static final StreamCodec<ByteBuf, Unbreakable> STREAM_CODEC = ByteBufCodecs.BOOL.map(Unbreakable::new, Unbreakable::showInTooltip);

    private final boolean showInTooltip;

    public Unbreakable(boolean showInTooltip) {
        this.showInTooltip = showInTooltip;
    }

    public boolean showInTooltip() {
        return this.showInTooltip;
    }

    public Unbreakable withTooltip(boolean showInTooltip) {
        return new Unbreakable(showInTooltip);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Unbreakable && ((Unbreakable) o).showInTooltip == this.showInTooltip;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(this.showInTooltip);
    }

    @Override
    public String toString() {
        return "Unbreakable[showInTooltip=" + this.showInTooltip + "]";
    }
}
