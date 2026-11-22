/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import java.lang.reflect.Field;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge 21.1 registers its fluid ingredient types (neoforge:single, tag, components, compound, difference,
 * intersection, empty); 21.0 makes the same register but never puts it on the mod bus, so a recipe with a fluid
 * ingredient (Create's filling, mixing, compacting) fails to load. The adapters put it on their bus.
 */
public final class FluidIngredientTypes {
    private FluidIngredientTypes() {
    }

    public static void register(IEventBus modBus) {
        try {
            Field types = NeoForgeMod.class.getDeclaredField("FLUID_INGREDIENT_TYPES");
            types.setAccessible(true);
            DeferredRegister<?> register = (DeferredRegister<?>) types.get(null);
            Field registered = DeferredRegister.class.getDeclaredField("registeredEventBus");
            registered.setAccessible(true);
            if (!registered.getBoolean(register)) {
                register.register(modBus);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // (a NeoForge without the register: nothing to put on the bus)
            com.mojang.logging.LogUtils.getLogger().warn("Ladder: NeoForge's fluid ingredient types not registered: {}", e.toString());
        }
    }
}
