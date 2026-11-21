/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

import java.lang.reflect.Field;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * NeoForge 21's {@code RegisterClientExtensionsEvent} (a P row): the client extensions of items, blocks, fluid types and
 * effects, registered from outside. 20.4 asks each object's own initializeClient once and keeps the answer in a field of
 * the object; the event sets that field. Posted on the mod buses when the client's hooks start
 * (mixin.ClientExtensionsMixin), before any extension is asked for.
 */
public class RegisterClientExtensionsEvent extends Event implements IModBusEvent {
    public void registerItem(IClientItemExtensions extensions, Item... items) {
        for (Item item : items) {
            set(Item.class, "renderProperties", item, extensions);
        }
    }

    public void registerBlock(IClientBlockExtensions extensions, Block... blocks) {
        for (Block block : blocks) {
            set(Block.class, "renderProperties", block, extensions);
        }
    }

    public void registerFluidType(IClientFluidTypeExtensions extensions, FluidType... types) {
        for (FluidType type : types) {
            set(FluidType.class, "renderProperties", type, extensions);
        }
    }

    public void registerMobEffect(IClientMobEffectExtensions extensions, MobEffect... effects) {
        for (MobEffect effect : effects) {
            set(MobEffect.class, "effectRenderer", effect, extensions);
        }
    }

    private static void set(Class<?> owner, String field, Object target, Object value) {
        try {
            Field f = owner.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no " + owner.getSimpleName() + "." + field + " for client extensions", e);
        }
    }
}
