/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;

/**
 * 1.20.5's {@code Item.TooltipContext}, given to {@code appendHoverText} instead of the level: the registries, the tick
 * rate and (NeoForge) the level. Built from 1.20.4's level at the call site.
 */
public interface TooltipContext {
    TooltipContext EMPTY = new TooltipContext() {
        @Nullable
        @Override
        public HolderLookup.Provider registries() {
            return null;
        }

        @Override
        public float tickRate() {
            return 20.0F;
        }
    };

    @Nullable
    HolderLookup.Provider registries();

    float tickRate();

    /** NeoForge's addition: the level the tooltip is for, when there is one. */
    @Nullable
    default Level level() {
        return null;
    }

    static TooltipContext of(@Nullable Level level) {
        if (level == null) {
            return EMPTY;
        }
        return new TooltipContext() {
            @Override
            public HolderLookup.Provider registries() {
                return level.registryAccess();
            }

            @Override
            public float tickRate() {
                return level.tickRateManager().tickrate();
            }

            @Override
            public Level level() {
                return level;
            }
        };
    }

    static TooltipContext of(HolderLookup.Provider registries) {
        return new TooltipContext() {
            @Override
            public HolderLookup.Provider registries() {
                return registries;
            }

            @Override
            public float tickRate() {
                return 20.0F;
            }
        };
    }
}
