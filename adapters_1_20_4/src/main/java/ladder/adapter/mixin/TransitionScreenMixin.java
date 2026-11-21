/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.adapter.client.DimensionTransitionScreenManager;
import ladder.adapter.client.TransitionScreens;
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
 * dimensions entered and left (1.20.4 always makes its ReceivingLevelScreen): the dimension left is remembered on a
 * respawn packet (none on login, as 21.1 passes), the reason is 1.20.5's for those dimensions.
 */
@Mixin(ClientPacketListener.class)
abstract class TransitionScreenMixin {
    @Unique
    @Nullable
    private ResourceKey<Level> ladder$leaving;

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void ladder$remember(ClientboundRespawnPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        this.ladder$leaving = player == null ? null : player.level().dimension();
    }

    @Redirect(method = "startWaitingForNewLevel", at = @At(value = "NEW", target = "(Ljava/util/function/BooleanSupplier;)Lnet/minecraft/client/gui/screens/ReceivingLevelScreen;"))
    private ReceivingLevelScreen ladder$transitionScreen(BooleanSupplier ready, LocalPlayer player, ClientLevel level) {
        ResourceKey<Level> from = this.ladder$leaving;
        this.ladder$leaving = null;
        ResourceKey<Level> to = from == null ? null : level.dimension();
        return DimensionTransitionScreenManager.getScreen(to, from).create(ready, TransitionScreens.reason(to, from));
    }
}
