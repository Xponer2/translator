/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;

/**
 * 1.21's {@code addOptions()} of the game's own options screens on 1.20.6 (added to each class, as 1.21 has it there).
 * 1.20.6 fills a screen's list in its init, in the same calls 1.21's addOptions makes: those calls are held and made
 * again, in their order, in addOptions, which runs where 1.21 runs it (before the title and footer, OptionsSubScreen.init).
 * The mods' injectors into 1.21's addOptions (at its addBig and addSmall calls, at its end) land there.
 */
final class OptionsScreenSites {
    private OptionsScreenSites() {
    }

    private static final String ADD_BIG = "Lnet/minecraft/client/gui/components/OptionsList;addBig(Lnet/minecraft/client/OptionInstance;)V";
    private static final String ADD_SMALL = "Lnet/minecraft/client/gui/components/OptionsList;addSmall([Lnet/minecraft/client/OptionInstance;)V";
    private static final String ADD_WIDGETS = "Lnet/minecraft/client/gui/components/OptionsList;addSmall(Lnet/minecraft/client/gui/components/AbstractWidget;Lnet/minecraft/client/gui/components/AbstractWidget;)V";
    private static final String SUPER_INIT = "Lnet/minecraft/client/gui/screens/OptionsSubScreen;init()V";

    @Mixin(value = VideoSettingsScreen.class, priority = 500)
    abstract static class Video {
        @Shadow
        private OptionsList list;
        @Unique
        private OptionInstance<?> ladder$big0;
        @Unique
        private OptionInstance<?> ladder$big1;
        @Unique
        private OptionInstance<?>[] ladder$small0;

        @Redirect(method = "init", at = @At(value = "INVOKE", target = ADD_BIG, ordinal = 0))
        private void ladder$big0(OptionsList list, OptionInstance<?> option) {
            this.ladder$big0 = option;
        }

        @Redirect(method = "init", at = @At(value = "INVOKE", target = ADD_BIG, ordinal = 1))
        private void ladder$big1(OptionsList list, OptionInstance<?> option) {
            this.ladder$big1 = option;
        }

        @Redirect(method = "init", at = @At(value = "INVOKE", target = ADD_SMALL, ordinal = 0))
        private void ladder$small0(OptionsList list, OptionInstance<?>[] options) {
            this.ladder$small0 = options;
        }

        protected void addOptions() {
            if (this.ladder$big0 != null) {
                this.list.addBig(this.ladder$big0);
            }
            if (this.ladder$big1 != null) {
                this.list.addBig(this.ladder$big1);
            }
            if (this.ladder$small0 != null) {
                this.list.addSmall(this.ladder$small0);
            }
        }

        @Inject(method = "init", at = @At(value = "INVOKE", target = SUPER_INIT))
        private void ladder$addOptions(CallbackInfo ci) {
            this.addOptions();
        }
    }

    @Mixin(value = SoundOptionsScreen.class, priority = 500)
    abstract static class Sound {
        @Shadow
        private OptionsList list;
        @Unique
        private OptionInstance<?> ladder$big0;
        @Unique
        private OptionInstance<?> ladder$big1;
        @Unique
        private OptionInstance<?>[] ladder$small0;
        @Unique
        private OptionInstance<?>[] ladder$small1;

        @Redirect(method = "init", at = @At(value = "INVOKE", target = ADD_BIG, ordinal = 0))
        private void ladder$big0(OptionsList list, OptionInstance<?> option) {
            this.ladder$big0 = option;
        }

        @Redirect(method = "init", at = @At(value = "INVOKE", target = ADD_BIG, ordinal = 1))
        private void ladder$big1(OptionsList list, OptionInstance<?> option) {
            this.ladder$big1 = option;
        }

        @Redirect(method = "init", at = @At(value = "INVOKE", target = ADD_SMALL, ordinal = 0))
        private void ladder$small0(OptionsList list, OptionInstance<?>[] options) {
            this.ladder$small0 = options;
        }

        @Redirect(method = "init", at = @At(value = "INVOKE", target = ADD_SMALL, ordinal = 1))
        private void ladder$small1(OptionsList list, OptionInstance<?>[] options) {
            this.ladder$small1 = options;
        }

        protected void addOptions() {
            if (this.ladder$big0 != null) {
                this.list.addBig(this.ladder$big0);
            }
            if (this.ladder$small0 != null) {
                this.list.addSmall(this.ladder$small0);
            }
            if (this.ladder$big1 != null) {
                this.list.addBig(this.ladder$big1);
            }
            if (this.ladder$small1 != null) {
                this.list.addSmall(this.ladder$small1);
            }
        }

        @Inject(method = "init", at = @At(value = "INVOKE", target = SUPER_INIT))
        private void ladder$addOptions(CallbackInfo ci) {
            this.addOptions();
        }
    }

    @Mixin(value = ControlsScreen.class, priority = 500)
    abstract static class Controls {
        @Shadow
        private OptionsList list;
        @Unique
        private AbstractWidget ladder$left;
        @Unique
        private AbstractWidget ladder$right;
        @Unique
        private OptionInstance<?>[] ladder$small0;

        @Redirect(method = "init", at = @At(value = "INVOKE", target = ADD_WIDGETS, ordinal = 0))
        private void ladder$widgets(OptionsList list, AbstractWidget left, AbstractWidget right) {
            this.ladder$left = left;
            this.ladder$right = right;
        }

        @Redirect(method = "init", at = @At(value = "INVOKE", target = ADD_SMALL, ordinal = 0))
        private void ladder$small0(OptionsList list, OptionInstance<?>[] options) {
            this.ladder$small0 = options;
        }

        protected void addOptions() {
            if (this.ladder$left != null) {
                this.list.addSmall(this.ladder$left, this.ladder$right);
            }
            if (this.ladder$small0 != null) {
                this.list.addSmall(this.ladder$small0);
            }
        }

        @Inject(method = "init", at = @At(value = "INVOKE", target = SUPER_INIT))
        private void ladder$addOptions(CallbackInfo ci) {
            this.addOptions();
        }
    }
}
