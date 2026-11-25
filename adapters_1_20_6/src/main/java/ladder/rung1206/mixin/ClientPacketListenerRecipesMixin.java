/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import ladder.rung1206.provided.SessionSearchTrees;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.searchtree.SearchRegistry;

/**
 * 1.21's recipe update fills the session's recipe tree (searchTrees().updateRecipes(book, registries)); 1.20.6 fills the
 * client's registry tree, which is the session's here (MinecraftSearchTreesMixin): the update goes to the session.
 */
@Mixin(value = ClientPacketListener.class, priority = 500)
abstract class ClientPacketListenerRecipesMixin {
    @WrapOperation(method = "handleUpdateRecipes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;populateSearchTree(Lnet/minecraft/client/searchtree/SearchRegistry$Key;Ljava/util/List;)V"))
    private void ladder$sessionRecipes(Minecraft minecraft, SearchRegistry.Key<?> key, List<?> collections, Operation<Void> original) {
        if (key != SearchRegistry.RECIPE_COLLECTIONS || minecraft.player == null) {
            original.call(minecraft, key, collections);
            return;
        }
        SessionSearchTrees.client().updateRecipes(minecraft.player.getRecipeBook(), ((ClientPacketListener) (Object) this).registryAccess());
    }
}
