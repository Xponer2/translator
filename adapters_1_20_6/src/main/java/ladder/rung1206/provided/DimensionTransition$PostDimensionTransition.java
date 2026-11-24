/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import net.minecraft.world.entity.Entity;

/**
 * 1.21's {@code DimensionTransition.PostDimensionTransition} (a P row): what runs on the entity that arrived. The
 * adapters' portal code uses it, so it is the adapters' own (a carried copy may be missing from a pack).
 */
@FunctionalInterface
public interface DimensionTransition$PostDimensionTransition {
    void onTransition(Entity entity);

    default DimensionTransition$PostDimensionTransition then(DimensionTransition$PostDimensionTransition next) {
        return entity -> {
            this.onTransition(entity);
            next.onTransition(entity);
        };
    }
}
