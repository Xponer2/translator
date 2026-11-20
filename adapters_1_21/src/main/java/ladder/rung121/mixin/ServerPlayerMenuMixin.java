/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import java.util.OptionalInt;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.rung121.MenuDataApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A converted mod's menu provider that writes client data (NeoForge 21.1's writeClientSideData): 21.0's openMenu gets a
 * writer that writes it for the menu just made, then the opener's own data, as 21.1 sends them.
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMenuMixin {
    @Unique
    @Nullable
    private AbstractContainerMenu ladder$opening;
    @Unique
    private boolean ladder$wrapping;

    @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;", at = @At("HEAD"),
            cancellable = true)
    private void ladder$clientSideData(@Nullable MenuProvider provider, @Nullable Consumer<RegistryFriendlyByteBuf> writer,
            CallbackInfoReturnable<OptionalInt> cir) {
        if (this.ladder$wrapping || !(provider instanceof MenuDataApi data)) {
            return;
        }
        this.ladder$wrapping = true;
        try {
            cir.setReturnValue(((ServerPlayer) (Object) this).openMenu(provider, buffer -> {
                data.writeClientSideData(this.ladder$opening, buffer);
                if (writer != null) {
                    writer.accept(buffer);
                }
            }));
        } finally {
            this.ladder$wrapping = false;
            this.ladder$opening = null;
        }
    }

    @ModifyVariable(method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;", at = @At("STORE"),
            ordinal = 0)
    private AbstractContainerMenu ladder$menuMade(AbstractContainerMenu menu) {
        this.ladder$opening = menu;
        return menu;
    }
}
