/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.rung121.client.ClientItemApi;
import ladder.rung121.client.ClientItemDefaults;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * NeoForge 21.1's scope texture and helmet overlay hooks on 1.21: a converted mod's client extensions that override them
 * give the spyglass overlay's texture of the item in use, and draw the overlay of the helmet worn (where 21.1 does:
 * looking in first person, not scoping, no carved pumpkin on).
 */
@Mixin(Gui.class)
public abstract class GuiOverlayMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @ModifyArg(method = "renderSpyglassOverlay", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V"), index = 0)
    private ResourceLocation ladder$scopeTexture(ResourceLocation texture) {
        LocalPlayer player = this.minecraft.player;
        ItemStack using = player == null ? ItemStack.EMPTY : player.getUseItem();
        ClientItemApi api = ClientItemDefaults.own(using, "getScopeOverlayTexture", ItemStack.class);
        return api != null ? api.getScopeOverlayTexture(using) : texture;
    }

    @Inject(method = "renderCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getTicksFrozen()I"))
    private void ladder$helmetOverlay(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;
        if (player == null || !this.minecraft.options.getCameraType().isFirstPerson() || player.isScoping()) {
            return;
        }
        ItemStack helmet = player.getInventory().getArmor(3);
        if (helmet.is(Blocks.CARVED_PUMPKIN.asItem())) {
            return;
        }
        ClientItemApi api = ClientItemDefaults.own(helmet, "renderHelmetOverlay", ItemStack.class, net.minecraft.world.entity.player.Player.class,
                GuiGraphics.class, DeltaTracker.class);
        if (api != null) {
            api.renderHelmetOverlay(helmet, player, graphics, deltaTracker);
        }
    }
}
