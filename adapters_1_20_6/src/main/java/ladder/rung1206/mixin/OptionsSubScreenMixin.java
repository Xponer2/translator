/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 1.21's options sub screen on 1.20.6 (the members are added to the class): its list of options ({@code list}), made in
 * {@code addContents()}, filled by the screen's {@code addOptions()}, sized with the layout. Only a screen that has
 * 1.21's addOptions or addContents (a converted mod's) gets them; 1.20.6's own screens build theirs as before.
 */
@Mixin(OptionsSubScreen.class)
abstract class OptionsSubScreenMixin extends Screen {
    @Shadow
    @Final
    public HeaderAndFooterLayout layout;

    protected OptionsList list;

    protected OptionsSubScreenMixin(Component title) {
        super(title);
    }

    protected void addContents() {
        this.list = this.layout.addToContents(new OptionsList(this.minecraft, this.width, this.layout.getContentHeight(), (OptionsSubScreen) (Object) this));
        this.addOptions();
    }

    protected void addOptions() {
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/OptionsSubScreen;addTitle()V", shift = At.Shift.AFTER))
    private void ladder$contents(CallbackInfo ci) {
        if (ladder$upper(this.getClass())) {
            this.addContents();
        }
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void ladder$listSize(CallbackInfo ci) {
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    /** whether a class below this one declares 1.21's addOptions() or addContents() */
    private static boolean ladder$upper(Class<?> type) {
        for (Class<?> c = type; c != null && c != OptionsSubScreen.class; c = c.getSuperclass()) {
            if (c.getName().startsWith("net.minecraft.")) {
                // the game's own screens (their addOptions is the adapters', OptionsScreenSites): built as 1.20.6 does
                continue;
            }
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                if ((m.getName().equals("addOptions") || m.getName().equals("addContents")) && m.getParameterCount() == 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
