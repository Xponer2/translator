/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.rung121.client.DimensionTransitionScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The screen shown while a level loads is the one NeoForge 21.1's DimensionTransitionScreenManager picks for the
 * dimensions entered and left (21.0 always makes a ReceivingLevelScreen): the dimension left is remembered on a respawn
 * packet (none on login, as 21.1 passes).
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Unique
    @Nullable
    private ResourceKey<Level> ladder$leaving;

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void ladder$remember(ClientboundRespawnPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        this.ladder$leaving = player == null ? null : player.level().dimension();
    }

    @Redirect(method = "startWaitingForNewLevel", at = @At(value = "NEW",
            target = "(Ljava/util/function/BooleanSupplier;Lnet/minecraft/client/gui/screens/ReceivingLevelScreen$Reason;)Lnet/minecraft/client/gui/screens/ReceivingLevelScreen;"))
    private ReceivingLevelScreen ladder$transitionScreen(BooleanSupplier ready, ReceivingLevelScreen.Reason reason, LocalPlayer player, ClientLevel level,
            ReceivingLevelScreen.Reason sameReason) {
        ResourceKey<Level> from = this.ladder$leaving;
        this.ladder$leaving = null;
        return DimensionTransitionScreenManager.getScreen(from == null ? null : level.dimension(), from).create(ready, reason);
    }
}
