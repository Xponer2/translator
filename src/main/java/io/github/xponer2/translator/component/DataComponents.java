package io.github.xponer2.translator.component;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.registry.MappedRegistry;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Codecs;
import io.github.xponer2.translator.util.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The vanilla component types the mods use (1.21 {@code net.minecraft.core.component.DataComponents}), registered
 * under their 1.21 ids. They keep their values where 1.12.2 keeps that data, so vanilla code sees them:
 * {@link #CUSTOM_DATA} is the stack's NBT (without {@link StackComponents#TAG}), {@link #UNBREAKABLE} the
 * "Unbreakable" tag. As defaults of an item ({@code ItemProperties#component}) they only exist for
 * {@link StackComponents}; the item itself has to act on them.
 */
public final class DataComponents {
    static final List<DataComponentType<?>> WITH_STORAGE = new ArrayList<>();

    public static final DataComponentType<NBTTagCompound> CUSTOM_DATA = register("custom_data", DataComponentType.<NBTTagCompound>builder()
            .persistent(Codecs.COMPOUND_TAG).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).storage(new CustomDataStorage()).build());
    public static final DataComponentType<Unbreakable> UNBREAKABLE = register("unbreakable", DataComponentType.<Unbreakable>builder()
            .persistent(Unbreakable.CODEC).networkSynchronized(Unbreakable.STREAM_CODEC).storage(new UnbreakableStorage()).build());

    private DataComponents() { }

    private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
        var registry = BuiltInRegistries.byKey(Registries.DATA_COMPONENT_TYPE);
        if (registry instanceof MappedRegistry) {
            ((MappedRegistry<DataComponentType<?>>) registry).register(new ResourceLocation("minecraft", name), type);
        }
        if (type.storage() != null) {
            WITH_STORAGE.add(type);
        }
        return type;
    }

    static List<DataComponentType<?>> withStorage() {
        return Collections.unmodifiableList(WITH_STORAGE);
    }

    /**
     * The NBT of the stack, without the components tag.
     */
    private static final class CustomDataStorage implements StackStorage<NBTTagCompound> {
        @Nullable
        @Override
        public NBTTagCompound get(ItemStack stack) {
            var tag = stack.getTagCompound();
            if (tag == null) {
                return null;
            }
            var copy = tag.copy();
            copy.removeTag(StackComponents.TAG);
            return copy.isEmpty() ? null : copy;
        }

        @Override
        public void set(ItemStack stack, @Nullable NBTTagCompound value) {
            var old = stack.getTagCompound();
            var tag = value == null ? new NBTTagCompound() : value.copy();
            if (old != null && old.hasKey(StackComponents.TAG, Constants.NBT.TAG_COMPOUND)) {
                tag.setTag(StackComponents.TAG, old.getTag(StackComponents.TAG));
            }
            stack.setTagCompound(tag.isEmpty() ? null : tag);
        }
    }

    /**
     * The vanilla "Unbreakable" tag; hidden in the tooltip by bit 4 of "HideFlags".
     */
    private static final class UnbreakableStorage implements StackStorage<Unbreakable> {
        private static final String UNBREAKABLE = "Unbreakable";
        private static final String HIDE_FLAGS = "HideFlags";
        private static final int HIDE_UNBREAKABLE = 4;

        @Nullable
        @Override
        public Unbreakable get(ItemStack stack) {
            var tag = stack.getTagCompound();
            if (tag == null || !tag.getBoolean(UNBREAKABLE)) {
                return null;
            }
            return new Unbreakable((tag.getInteger(HIDE_FLAGS) & HIDE_UNBREAKABLE) == 0);
        }

        @Override
        public void set(ItemStack stack, @Nullable Unbreakable value) {
            var tag = stack.getTagCompound();
            if (value == null) {
                if (tag != null) {
                    tag.removeTag(UNBREAKABLE);
                    if (tag.isEmpty()) {
                        stack.setTagCompound(null);
                    }
                }
                return;
            }
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            tag.setBoolean(UNBREAKABLE, true);
            int flags = tag.getInteger(HIDE_FLAGS);
            flags = value.showInTooltip() ? flags & ~HIDE_UNBREAKABLE : flags | HIDE_UNBREAKABLE;
            if (flags == 0) {
                tag.removeTag(HIDE_FLAGS);
            } else {
                tag.setInteger(HIDE_FLAGS, flags);
            }
        }
    }
}
