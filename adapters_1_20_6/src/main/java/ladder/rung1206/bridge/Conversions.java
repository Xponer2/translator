/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The value conversions of the generated bridges: where a newer version takes or gives a {@code Holder<X>} and 1.20.4
 * an {@code X} (mob effects, attributes, game events, sounds, potions, enchantments). Written once; the converter makes
 * a bridge per changed member that calls these.
 */
public final class Conversions {
    /** value class -> the built-in registry that holds its values (null: none, a direct holder is made) */
    private static final Map<Class<?>, Registry<?>> REGISTRIES = new ConcurrentHashMap<>();
    private static final Registry<?> NONE = BuiltInRegistries.REGISTRY;

    private Conversions() {
    }

    /** {@code Holder<X>} -> {@code X} */
    @Nullable
    public static Object unwrap(@Nullable Holder<?> holder) {
        return holder == null ? null : holder.value();
    }

    /** {@code X} -> the registry's own {@code Holder<X>} for it (a reference, as the newer versions give), else a direct one */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Holder<?> holder(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        Registry registry = registryOf(value);
        if (registry != null) {
            return registry.wrapAsHolder(value);
        }
        return Holder.direct(value);
    }

    /** {@code X} -> its registry reference ({@code Holder.Reference<X>}), or null when it isn't registered */
    @Nullable
    public static Holder.Reference<?> reference(@Nullable Object value) {
        return holder(value) instanceof Holder.Reference<?> reference ? reference : null;
    }

    /** {@code Codec} -> {@code MapCodec}: the record codec's own map codec (1.20.5 gives the map codec where 1.20.4 the codec). */
    public static com.mojang.serialization.MapCodec<?> mapCodec(com.mojang.serialization.Codec<?> codec) {
        if (codec instanceof com.mojang.serialization.MapCodec.MapCodecCodec<?> map) {
            return map.codec();
        }
        throw new IllegalStateException("A codec that isn't a map codec where the newer version has one: " + codec);
    }

    /** {@code HolderLookup.Provider} -> {@code RegistryAccess}: itself when it is one, else the server's or the client's. */
    public static net.minecraft.core.RegistryAccess registryAccess(net.minecraft.core.HolderLookup.Provider provider) {
        if (provider instanceof net.minecraft.core.RegistryAccess access) {
            return access;
        }
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.registryAccess();
        }
        return ClientRegistries.access();
    }

    /** the current registries (the server's, else the client's): what 1.20.5 passes where 1.20.4 passes nothing */
    public static net.minecraft.core.RegistryAccess registries() {
        return registryAccess(null);
    }

    /** {@code ResourceKey<X>} -> {@code X}: the value of that key in its built-in registry (1.21's enchantment keys) */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object valueOfKey(@Nullable net.minecraft.resources.ResourceKey<?> key) {
        if (key == null) {
            return null;
        }
        Registry registry = BuiltInRegistries.REGISTRY.get(key.registry());
        return registry == null ? null : registry.get(key.location());
    }

    /** {@code X} -> {@code ResourceKey<X>}: its key in the built-in registry that holds it */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.resources.ResourceKey<?> keyOfValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        Registry registry = registryOf(value);
        return registry == null ? null : (net.minecraft.resources.ResourceKey<?>) registry.getResourceKey(value).orElse(null);
    }

    /** {@code ResourceKey<X>} -> {@code ResourceLocation} */
    @Nullable
    public static net.minecraft.resources.ResourceLocation location(@Nullable net.minecraft.resources.ResourceKey<?> key) {
        return key == null ? null : key.location();
    }

    /** the loot tables' registry key of 1.20.5 (1.20.4 names loot tables by location only) */
    public static final net.minecraft.resources.ResourceKey<? extends Registry<Object>> LOOT_TABLE =
            net.minecraft.resources.ResourceKey.createRegistryKey(new net.minecraft.resources.ResourceLocation("loot_table"));

    /** {@code ResourceLocation} -> {@code ResourceKey<LootTable>} (BuiltInLootTables, getLootTable()) */
    @Nullable
    public static net.minecraft.resources.ResourceKey<?> lootTableKey(@Nullable net.minecraft.resources.ResourceLocation location) {
        return location == null ? null : net.minecraft.resources.ResourceKey.create(LOOT_TABLE, location);
    }

    /** a partial tick -> 1.21's {@code DeltaTracker} of it */
    public static ladder.rung1206.provided.DeltaTracker deltaTracker(float partialTick) {
        return new ladder.rung1206.provided.DeltaTracker.PartialTick(partialTick);
    }

    /** {@code X} -> {@code Optional<X>} */
    public static java.util.Optional<?> optional(@Nullable Object value) {
        return java.util.Optional.ofNullable(value);
    }

    /** {@code Optional<X>} -> {@code X} (null when empty) */
    @Nullable
    public static Object orNull(@Nullable java.util.Optional<?> value) {
        return value == null ? null : value.orElse(null);
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Registry<?> registryOf(Object value) {
        // getResourceKey, not getKey: a defaulted registry's getKey gives its default key (minecraft:air) for any object
        Registry<?> found = REGISTRIES.computeIfAbsent(value.getClass(), type -> {
            for (Registry registry : BuiltInRegistries.REGISTRY) {
                if (registry.getResourceKey(value).isPresent()) {
                    return registry;
                }
            }
            return NONE;
        });
        if (found == NONE) {
            return null;
        }
        // a class's values can sit in one registry and not another value of it (two registries of one type): checked
        return ((Registry) found).getResourceKey(value).isPresent() ? found : null;
    }

    /** 1.21's ARGB colour int -> 1.20.6's red, green, blue, alpha floats (0..1) */
    public static float red(int color) {
        return (color >> 16 & 255) / 255.0F;
    }

    public static float green(int color) {
        return (color >> 8 & 255) / 255.0F;
    }

    public static float blue(int color) {
        return (color & 255) / 255.0F;
    }

    public static float alpha(int color) {
        return (color >>> 24) / 255.0F;
    }

    /** 1.20.6's four floats -> 1.21's ARGB int (FastColor.ARGB32.colorFromFloat: each channel floor(f * 255)) */
    public static int color(float red, float green, float blue, float alpha) {
        return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    private static int channel(float value) {
        return net.minecraft.util.Mth.floor(value * 255.0F) & 255;
    }

    /**
     * The callback a mixin handler of the upper method gets when the method's result changed between numbers (1.21's
     * float, 1.20.6's double): the lower callback's value as the upper number ("F", "D", "I"...).
     */
    public static org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<?> callback(
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<?> lower, String upper) {
        Object value = lower.getReturnValue();
        return new org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<>(lower.getId(), lower.isCancellable(),
                value instanceof Number n ? number(n, upper) : value);
    }

    /** what the handler set on the upper callback (a cancel with a value), on the lower one as the lower number */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void result(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<?> upper,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<?> lower, String type) {
        if (upper.isCancelled()) {
            Object value = upper.getReturnValue();
            ((org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable) lower).setReturnValue(
                    value instanceof Number n ? number(n, type) : value);
        }
    }

    private static Object number(Number n, String type) {
        return switch (type) {
            case "B" -> n.byteValue();
            case "S" -> n.shortValue();
            case "I" -> n.intValue();
            case "J" -> n.longValue();
            case "F" -> n.floatValue();
            default -> n.doubleValue();
        };
    }

    /** 1.21's MeshData of 1.20.6's rendered buffer (a mixin handler's value, converted up) */
    public static ladder.rung1206.provided.MeshData meshData(com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer buffer) {
        return buffer == null ? null : new ladder.rung1206.provided.MeshData(buffer);
    }

    /**
     * The Operation a @WrapOperation or @WrapMethod handler of the upper types calls: it takes the upper values and calls
     * the lower Operation with them converted down, its result converted up. The spec (the converter's): per value
     * "upperDesc>lowerDesc" or "" when the same, comma-terminated, then "|" and the result's.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static com.llamalad7.mixinextras.injector.wrapoperation.Operation<?> operation(
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<?> lower, String spec) {
        int bar = spec.lastIndexOf('|');
        String[] values = spec.substring(0, bar).split(",", -1);
        String result = spec.substring(bar + 1);
        return (com.llamalad7.mixinextras.injector.wrapoperation.Operation) args -> {
            Object[] down = args.clone();
            for (int i = 0; i < down.length && i < values.length; i++) {
                if (!values[i].isEmpty()) {
                    String[] t = values[i].split(">");
                    down[i] = convert(down[i], t[0], t[1]);
                }
            }
            Object value = lower.call(down);
            if (result.isEmpty()) {
                return value;
            }
            String[] t = result.split(">");
            return convert(value, t[1], t[0]);
        };
    }

    /** a value of one type as another (the pairs a mixin handler's values convert between) */
    public static Object convert(Object value, String from, String to) {
        if (value == null || from.equals(to)) {
            return value;
        }
        if (value instanceof Number n && to.length() == 1) {
            return switch (to) {
                case "B" -> n.byteValue();
                case "S" -> n.shortValue();
                case "I" -> n.intValue();
                case "J" -> n.longValue();
                case "F" -> n.floatValue();
                default -> n.doubleValue();
            };
        }
        if (value instanceof ladder.rung1206.provided.DeltaTracker tracker && to.equals("F")) {
            return tracker.getGameTimeDeltaPartialTick(true);
        }
        if (value instanceof Float partial && to.equals("Lladder/rung1206/provided/DeltaTracker;")) {
            return deltaTracker(partial);
        }
        if (value instanceof ladder.rung1206.provided.MeshData mesh && to.equals("Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;")) {
            return mesh.buffer();
        }
        if (value instanceof com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer buffer && to.equals("Lladder/rung1206/provided/MeshData;")) {
            return meshData(buffer);
        }
        if (value instanceof Holder<?> holder && !to.equals("Lnet/minecraft/core/Holder;") && !to.equals("Lnet/minecraft/core/Holder$Reference;")) {
            return holder.value();
        }
        if (!(value instanceof Holder<?>) && (to.equals("Lnet/minecraft/core/Holder;") || to.equals("Lnet/minecraft/core/Holder$Reference;"))) {
            return to.endsWith("Reference;") ? reference(value) : holder(value);
        }
        if (value instanceof net.minecraft.resources.ResourceKey<?> key && !to.equals("Lnet/minecraft/resources/ResourceKey;")) {
            return valueOfKey(key);
        }
        if (to.equals("Lnet/minecraft/resources/ResourceKey;")) {
            return keyOfValue(value);
        }
        return value;
    }
}
