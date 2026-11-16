/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package ladder.adapter.provided;

/**
 * 1.21's {@code DeltaTracker} (the frame's time: delta ticks, partial tick). On 1.20.4 the client's own comes from its
 * Minecraft (ladder.adapter.render.RenderAdapters.timer); a fixed one is made where 1.20.4 passes a partial tick.
 */
public interface DeltaTracker {
    DeltaTracker ZERO = new DefaultValue(0.0F);
    DeltaTracker ONE = new DefaultValue(1.0F);

    float getGameTimeDeltaTicks();

    float getGameTimeDeltaPartialTick(boolean runsNormally);

    float getRealtimeDeltaTicks();

    /** 1.21's {@code DeltaTracker.DefaultValue}: the same value for all three */
    class DefaultValue implements DeltaTracker {
        private final float value;

        public DefaultValue(float value) {
            this.value = value;
        }

        @Override
        public float getGameTimeDeltaTicks() {
            return this.value;
        }

        @Override
        public float getGameTimeDeltaPartialTick(boolean runsNormally) {
            return this.value;
        }

        @Override
        public float getRealtimeDeltaTicks() {
            return this.value;
        }
    }

    /** where 1.20.4 passes the partial tick only: that partial tick, deltas of one tick */
    record PartialTick(float partialTick) implements DeltaTracker {
        @Override
        public float getGameTimeDeltaTicks() {
            return 1.0F;
        }

        @Override
        public float getGameTimeDeltaPartialTick(boolean runsNormally) {
            return this.partialTick;
        }

        @Override
        public float getRealtimeDeltaTicks() {
            return 1.0F;
        }
    }
}
