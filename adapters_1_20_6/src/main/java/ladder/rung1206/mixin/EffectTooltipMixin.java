/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;

import ladder.rung1206.provided.event.GatherEffectScreenTooltipsEvent;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge 21's effect tooltip event: the lines of the hovered effect's tooltip, gathered before 20.6 draws them. */
@Mixin(EffectRenderingInventoryScreen.class)
abstract class EffectTooltipMixin {
    @ModifyArg(method = "renderEffects", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V"))
    private List<Component> ladder$gather(List<Component> tooltip, @Local(ordinal = 0) MobEffectInstance effect) {
        GatherEffectScreenTooltipsEvent event = new GatherEffectScreenTooltipsEvent((EffectRenderingInventoryScreen<?>) (Object) this, effect, tooltip);
        NeoForge.EVENT_BUS.post(event);
        return event.getTooltip();
    }
}
