/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import java.util.List;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21's search trees of a client's session ({@code ClientPacketListener.searchTrees()}) on 1.20.6, whose trees are the
 * client's own (Minecraft's search registry): the creative tab's names and tags, the recipe book's collections. Filling
 * one fills the client's.
 */
public final class SessionSearchTrees {
    private static final SessionSearchTrees CLIENT = new SessionSearchTrees();

    private SessionSearchTrees() {
    }

    /** 1.21's {@code connection.searchTrees()}: the client's trees (one client, one session) */
    public static SessionSearchTrees of(ClientPacketListener connection) {
        return CLIENT;
    }

    public void updateCreativeTooltips(HolderLookup.Provider registries, List<ItemStack> items) {
        Minecraft.getInstance().populateSearchTree(SearchRegistry.CREATIVE_NAMES, items);
    }

    public void updateCreativeTags(List<ItemStack> items) {
        Minecraft.getInstance().populateSearchTree(SearchRegistry.CREATIVE_TAGS, items);
    }

    /** (1.21's private overload, a mod's invoker: the tree of that key filled) */
    public void updateCreativeTags(List<ItemStack> items, SearchRegistry.Key<ItemStack> key) {
        Minecraft.getInstance().populateSearchTree(key, items);
    }

    public void updateRecipes(ClientRecipeBook book, RegistryAccess.Frozen registries) {
        Minecraft.getInstance().populateSearchTree(SearchRegistry.RECIPE_COLLECTIONS, book.getCollections());
    }

    public SearchTree<ItemStack> creativeNameSearch() {
        return Minecraft.getInstance().getSearchTree(SearchRegistry.CREATIVE_NAMES);
    }

    public SearchTree<ItemStack> creativeTagSearch() {
        return Minecraft.getInstance().getSearchTree(SearchRegistry.CREATIVE_TAGS);
    }

    public SearchTree<RecipeCollection> recipes() {
        return Minecraft.getInstance().getSearchTree(SearchRegistry.RECIPE_COLLECTIONS);
    }
}
