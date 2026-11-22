/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.level.Level;

/**
 * NeoForge 21's day time speed of a level on 1.20.6: the time it advances per tick (a fraction kept between ticks); unset,
 * 1.20.6's one per tick. The server's level advances by it (mixin.DayTimeMixin); the clients follow the server's time.
 */
public final class DayTime {
    private static final Map<Level, float[]> SPEED = new WeakHashMap<>();

    private DayTime() {
    }

    private static float[] of(Level level) {
        synchronized (SPEED) {
            return SPEED.computeIfAbsent(level, l -> new float[] {-1.0F, 0.0F});
        }
    }

    public static float getDayTimePerTick(Level level) {
        return of(level)[0];
    }

    public static void setDayTimePerTick(Level level, float perTick) {
        of(level)[0] = perTick;
    }

    public static float getDayTimeFraction(Level level) {
        return of(level)[1];
    }

    public static void setDayTimeFraction(Level level, float fraction) {
        of(level)[1] = fraction;
    }

    /** how far the day time moves this tick (1 unless a speed is set) */
    public static long advance(Level level) {
        float[] s = of(level);
        if (s[0] < 0.0F) {
            return 1L;
        }
        float total = s[1] + s[0];
        long whole = (long) total;
        s[1] = total - whole;
        return whole;
    }
}
