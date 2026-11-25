/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.List;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.rung1206.provided.SessionSearchTrees;
import net.minecraft.client.Minecraft;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;

/**
 * 1.21's search trees on 1.20.6's search registry: the creative tabs' name trees take their text by 1.21's
 * SessionSearchTrees.getTooltipLines (the same lines 1.20.6 takes: a mod's point there applies, Jade's), and the recipe
 * book's tree is the session's (1.21's, built by its reloader; ClientPacketListenerRecipesMixin updates it).
 */
@Mixin(value = Minecraft.class, priority = 500)
abstract class MinecraftSearchTreesMixin {
    @Shadow
    @Final
    private SearchRegistry searchRegistry;

    @Inject(method = "createSearchTrees", at = @At("TAIL"))
    private void ladder$sessionSearchTrees(CallbackInfo ci) {
        for (SearchRegistry.Key<ItemStack> key : CreativeModeTabSearchRegistry.getNameSearchKeys().values()) {
            this.searchRegistry.register(key, items -> new FullTextSearchTree<>(
                    stack -> SessionSearchTrees.getTooltipLines(Stream.of(stack), Item.TooltipContext.EMPTY, TooltipFlag.Default.NORMAL.asCreative()),
                    stack -> Stream.of(BuiltInRegistries.ITEM.getKey(stack.getItem())), items));
        }
        this.searchRegistry.register(SearchRegistry.RECIPE_COLLECTIONS, collections -> SessionSearchTrees.recipeTree());
        this.searchRegistry.populate(SearchRegistry.RECIPE_COLLECTIONS, List.of());
    }
}
