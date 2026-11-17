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
 * Minecraft (ladder.adapter.render.RenderAdapters.TIMER); where 1.20.4 passes only a partial tick, a tracker of that
 * partial tick whose deltas are the client timer's.
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

    /** the client's timer, set by the adapters on the client (none on a server: no frames there) */
    final class Client {
        public static volatile DeltaTracker timer;

        private Client() {
        }
    }

    /** a partial tick 1.20.4 passes; the deltas are the client timer's */
    record PartialTick(float partialTick) implements DeltaTracker {
        @Override
        public float getGameTimeDeltaTicks() {
            DeltaTracker timer = Client.timer;
            return timer == null ? 1.0F : timer.getGameTimeDeltaTicks();
        }

        @Override
        public float getGameTimeDeltaPartialTick(boolean runsNormally) {
            return this.partialTick;
        }

        @Override
        public float getRealtimeDeltaTicks() {
            DeltaTracker timer = Client.timer;
            return timer == null ? 1.0F : timer.getRealtimeDeltaTicks();
        }
    }
}
