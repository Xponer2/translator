/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import ladder.rung1206.provided.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** 1.21's frame time on 1.20.6: the client's own DeltaTracker from 1.20.6's timer, a partial tick where 1.20.6 passes one */
public final class TimeAdapters {
    private TimeAdapters() {
    }

    /** the client's own DeltaTracker: 1.20.6's timer values through its Minecraft */
    public static final DeltaTracker TIMER = new DeltaTracker() {
        @Override
        public float getGameTimeDeltaTicks() {
            return Minecraft.getInstance().getDeltaFrameTime();
        }

        @Override
        public float getGameTimeDeltaPartialTick(boolean runsNormally) {
            // NeoForge's getPartialTick is the paused one while the game is paused, as 1.21's timer gives it
            return Minecraft.getInstance().getPartialTick();
        }

        @Override
        public float getRealtimeDeltaTicks() {
            return Minecraft.getInstance().getDeltaFrameTime();
        }
    };

    /** 1.21's {@code Minecraft.getTimer()} */
    public static DeltaTracker getTimer(Minecraft minecraft) {
        return TIMER;
    }

    /** 1.21's {@code getPartialTick()} of the render events, which 1.20.6 gives as a float (the deltas: the timer's) */
    public static DeltaTracker getPartialTick(RenderLevelStageEvent event) {
        return new DeltaTracker.PartialTick(event.getPartialTick());
    }

    public static DeltaTracker getPartialTick(RenderGuiEvent event) {
        return new DeltaTracker.PartialTick(event.getPartialTick());
    }

    /** NeoForge 21's {@code getPartialTick()} of the GUI layer and frame events (20.6: a float) */
    public static ladder.rung1206.provided.DeltaTracker getPartialTick(net.neoforged.neoforge.client.event.RenderGuiLayerEvent event) {
        return new ladder.rung1206.provided.DeltaTracker.PartialTick(event.getPartialTick());
    }

    public static ladder.rung1206.provided.DeltaTracker getPartialTick(net.neoforged.neoforge.client.event.RenderFrameEvent event) {
        return new ladder.rung1206.provided.DeltaTracker.PartialTick(event.getPartialTick());
    }

    /** the partial tick 1.20.6's GameRenderer.render and renderLevel take, of 1.21's DeltaTracker (D rows) */
    public static float partialTick(DeltaTracker tracker) {
        return tracker.getGameTimeDeltaPartialTick(true);
    }

    /** the frame's start 1.20.6's GameRenderer.render and renderLevel take (1.21's take none) */
    public static long nanos() {
        return net.minecraft.Util.getNanos();
    }
}
