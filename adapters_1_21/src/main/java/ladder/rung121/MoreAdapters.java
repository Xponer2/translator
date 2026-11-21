/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import com.google.common.collect.BiMap;
import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/** More of NeoForge 21.1's additions on 1.21 (the X rows' adapters, receiver first). */
public final class MoreAdapters {
    /** the registry builders a mod asked for intrusive holders (mixin.RegistryBuilderMixin reads it) */
    static final Set<RegistryBuilder<?>> INTRUSIVE = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private MoreAdapters() {
    }

    /** {@code RegisterSpriteSourceTypesEvent.register(id, type)}: a type made by the mod, put where 21.0's own go */
    @SuppressWarnings("unchecked")
    public static void registerSpriteSourceType(net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent event, ResourceLocation id,
            net.minecraft.client.renderer.texture.atlas.SpriteSourceType type) {
        try {
            java.lang.reflect.Field field = event.getClass().getDeclaredField("types");
            field.setAccessible(true);
            BiMap<ResourceLocation, net.minecraft.client.renderer.texture.atlas.SpriteSourceType> types =
                    (BiMap<ResourceLocation, net.minecraft.client.renderer.texture.atlas.SpriteSourceType>) field.get(event);
            if (types.containsKey(id)) {
                throw new IllegalStateException("Duplicate sprite source type registration " + id);
            }
            types.put(id, type);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no sprite source types of RegisterSpriteSourceTypesEvent", e);
        }
    }

    /** {@code ConfigurationScreen.CRUMB_SEPARATOR}: the breadcrumb separator (its text is in the adapters' language file) */
    public static Component crumbSeparator() {
        return Component.translatable("neoforge.configuration.uitext.breadcrumb.separator").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    /** {@code RegistryBuilder.withIntrusiveHolders()}: the registry it creates gives its entries' holders to them */
    public static <T> RegistryBuilder<T> withIntrusiveHolders(RegistryBuilder<T> builder) {
        INTRUSIVE.add(builder);
        return builder;
    }

    /**
     * {@code DataPackRegistryEvent.NewRegistry.dataPackRegistry(key, codec, networkCodec, builder)}: 21.0's makes the
     * registry itself, the consumer's settings (callbacks of 21.1's registry builder) have no place there
     */
    public static <T> void dataPackRegistry(DataPackRegistryEvent.NewRegistry event, ResourceKey<Registry<T>> key, Codec<T> codec, Codec<T> networkCodec,
            Consumer<RegistryBuilder<T>> builder) {
        event.dataPackRegistry(key, codec, networkCodec);
    }

    /** {@code state.shouldHideAdjacentFluidFace(side, fluid)}: the block's own answer, else a fluid hides the same fluid */
    public static boolean shouldHideAdjacentFluidFace(BlockState state, Direction side, FluidState fluid) {
        if (state.getBlock() instanceof FluidFaceApi own) {
            return own.shouldHideAdjacentFluidFace(state, side, fluid);
        }
        return FluidFaceApi.shouldHideAdjacentFluidFace(state.getBlock(), state, side, fluid);
    }

    /** the same through NeoForge's interface */
    public static boolean shouldHideAdjacentFluidFace(net.neoforged.neoforge.common.extensions.IBlockStateExtension state, Direction side, FluidState fluid) {
        return shouldHideAdjacentFluidFace((BlockState) state, side, fluid);
    }

    public static boolean shouldHideAdjacentFluidFace(net.neoforged.neoforge.common.extensions.IBlockExtension block, BlockState state, Direction side,
            FluidState fluid) {
        return shouldHideAdjacentFluidFace(state, side, fluid);
    }

    /** {@code block.shouldHideAdjacentFluidFace(state, side, fluid)} */
    public static boolean shouldHideAdjacentFluidFace(net.minecraft.world.level.block.Block block, BlockState state, Direction side, FluidState fluid) {
        return shouldHideAdjacentFluidFace(state, side, fluid);
    }

    /** {@code item.canFitInsideContainerItems(stack)}: a mod item's override, else the item's own answer (21.1's default) */
    public static boolean canFitInsideContainerItems(Item item, ItemStack stack) {
        if (item instanceof ContainerFitApi own && Dispatch.declares(item.getClass(), "canFitInsideContainerItems", ItemStack.class)) {
            return own.canFitInsideContainerItems(stack);
        }
        return item.canFitInsideContainerItems();
    }

    /** the same through NeoForge's interface */
    public static boolean canFitInsideContainerItems(net.neoforged.neoforge.common.extensions.IItemExtension item, ItemStack stack) {
        return canFitInsideContainerItems((Item) item, stack);
    }

    /** {@code stack.canFitInsideContainerItems()} */
    public static boolean canFitInsideContainerItems(ItemStack stack) {
        return ContainerFitApi.fits(stack);
    }

    public static boolean canFitInsideContainerItems(net.neoforged.neoforge.common.extensions.IItemStackExtension stack) {
        return ContainerFitApi.fits((ItemStack) stack);
    }

    @SafeVarargs
    private static <T> T[] values(T[] into, Holder<? extends T>... holders) {
        T[] out = Arrays.copyOf(into, holders.length);
        for (int i = 0; i < holders.length; i++) {
            out[i] = holders[i].value();
        }
        return out;
    }

    /** {@code RegisterClientExtensionsEvent.registerItem(extensions, holders...)} */
    @SafeVarargs
    public static void registerItem(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event,
            net.neoforged.neoforge.client.extensions.common.IClientItemExtensions extensions, Holder<Item>... items) {
        event.registerItem(extensions, values(new Item[0], items));
    }

    @SafeVarargs
    public static void registerBlock(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event,
            net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions extensions, Holder<net.minecraft.world.level.block.Block>... blocks) {
        event.registerBlock(extensions, values(new net.minecraft.world.level.block.Block[0], blocks));
    }

    @SafeVarargs
    public static void registerMobEffect(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event,
            net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions extensions, Holder<net.minecraft.world.effect.MobEffect>... effects) {
        event.registerMobEffect(extensions, values(new net.minecraft.world.effect.MobEffect[0], effects));
    }

    @SafeVarargs
    public static void registerFluidType(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event,
            net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions extensions, Holder<net.neoforged.neoforge.fluids.FluidType>... types) {
        event.registerFluidType(extensions, values(new net.neoforged.neoforge.fluids.FluidType[0], types));
    }

    /** {@code FMLLoader.getBindings()} of FML 4.0.43: the one bindings provider of the game layer */
    public static net.neoforged.fml.IBindingsProvider getBindings() {
        java.util.List<java.util.ServiceLoader.Provider<net.neoforged.fml.IBindingsProvider>> providers = java.util.ServiceLoader
                .load(net.neoforged.fml.loading.FMLLoader.getGameLayer(), net.neoforged.fml.IBindingsProvider.class).stream().toList();
        if (providers.size() != 1) {
            throw new IllegalStateException("Could not find bindings provider");
        }
        return providers.get(0).get();
    }
}
