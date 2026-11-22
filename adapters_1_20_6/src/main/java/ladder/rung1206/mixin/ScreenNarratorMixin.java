/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;

/**
 * 1.21's narrator button of any screen and its update when the narrator's status changes (1.20.6 has them on its simple
 * options screens only): added to the class, a converted mod's screen sets and updates its own.
 */
@Mixin(value = Screen.class, priority = 500)
abstract class ScreenNarratorMixin {
    @Shadow
    protected Minecraft minecraft;

    protected CycleButton<NarratorStatus> narratorButton;

    @Shadow
    public abstract void narrationEnabled();

    public void updateNarratorStatus(boolean narratorEnabled) {
        if (narratorEnabled) {
            this.narrationEnabled();
        }
        if (this.narratorButton != null && this.minecraft != null) {
            this.narratorButton.setValue(this.minecraft.options.narrator().get());
        }
    }
}
