/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;

/** NeoForge 21.1's client-only additions on 1.21 (loaded only on a client) */
public final class ClientAdapters {
    private ClientAdapters() {
    }

    /** {@code IKeyMappingExtension.getDisplayName()}: 21.1's default, the mapping's name translated */
    public static Component getDisplayName(IKeyMappingExtension self) {
        return Component.translatable(((KeyMapping) self).getName());
    }

    /** {@code QuadLighter.computeLightingForQuad(vertices, isShade)} of 21.1: 21.0's reads only a quad's vertices and shade */
    public static void computeLightingForQuad(net.neoforged.neoforge.client.model.lighting.QuadLighter lighter, int[] vertices, boolean isShade) {
        lighter.computeLightingForQuad(new net.minecraft.client.renderer.block.model.BakedQuad(vertices, -1, net.minecraft.core.Direction.UP, null, isShade));
    }

    /** NeoForge 21.1's {@code RegisterClientExtensionsEvent.is...Registered}: 21.0 keeps the same maps, package-private */
    private static boolean registered(String field, Object key) {
        try {
            java.lang.reflect.Field f = Class.forName("net.neoforged.neoforge.client.extensions.common.ClientExtensionsManager").getDeclaredField(field);
            f.setAccessible(true);
            return ((java.util.Map<?, ?>) f.get(null)).containsKey(key);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("NeoForge 21.0's ClientExtensionsManager." + field, e);
        }
    }

    public static boolean isBlockRegistered(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event, net.minecraft.world.level.block.Block block) {
        return registered("BLOCK_EXTENSIONS", block);
    }

    public static boolean isItemRegistered(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event, net.minecraft.world.item.Item item) {
        return registered("ITEM_EXTENSIONS", item);
    }

    public static boolean isMobEffectRegistered(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event, net.minecraft.world.effect.MobEffect effect) {
        return registered("MOB_EFFECT_EXTENSIONS", effect);
    }

    public static boolean isFluidTypeRegistered(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event, net.neoforged.neoforge.fluids.FluidType type) {
        return registered("FLUID_TYPE_EXTENSIONS", type);
    }
}
