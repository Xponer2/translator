/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.client.searchtree.RefreshableSearchTree;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.21's search trees of a client's session ({@code ClientPacketListener.searchTrees()}) on 1.20.6. The creative tab's
 * trees are the client's own (Minecraft's search registry), their item text by 1.21's getTooltipLines (the adapters'
 * MinecraftSearchTreesMixin builds 1.20.6's name trees through it, so the mods' points there apply); the recipe book's
 * tree is 1.21's (built off the render thread by its reloader, read on use), which 1.20.6's recipe book searches (the
 * same mixin gives it this tree, ClientPacketListenerRecipesMixin updates it). Reloaders run again after a language
 * change (1.20.6's search registry refreshes on a resource reload).
 */
public final class SessionSearchTrees {
    private static final SessionSearchTrees CLIENT = new SessionSearchTrees();

    private final Map<SearchRegistry.Key<?>, Runnable> reloaders = new IdentityHashMap<>();
    private CompletableFuture<SearchTree<RecipeCollection>> recipeSearch = CompletableFuture.completedFuture(RefreshableSearchTree.empty());

    private SessionSearchTrees() {
    }

    /** 1.21's {@code connection.searchTrees()}: the client's trees (one client, one session) */
    public static SessionSearchTrees of(ClientPacketListener connection) {
        return CLIENT;
    }

    /** the client's trees (the adapters' own callers) */
    public static SessionSearchTrees client() {
        return CLIENT;
    }

    /** 1.21's register(key, reloader): run now, and again after a language change */
    public void register(SearchRegistry.Key<?> key, Runnable reloader) {
        reloader.run();
        this.reloaders.put(key, reloader);
    }

    /** 1.21's rebuildAfterLanguageChange(): every reloader again */
    public void rebuildAfterLanguageChange() {
        for (Runnable reloader : List.copyOf(this.reloaders.values())) {
            reloader.run();
        }
    }

    /** 1.21's getTooltipLines(items, context, flag): the items' tooltip lines as search text */
    public static Stream<String> getTooltipLines(Stream<ItemStack> items, Item.TooltipContext context, TooltipFlag flag) {
        return items.flatMap(stack -> stack.getTooltipLines(context, null, flag).stream())
                .map(line -> ChatFormatting.stripFormatting(line.getString()).trim())
                .filter(line -> !line.isEmpty());
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

    /** 1.21's updateRecipes(book, registries): the recipe tree's reloader */
    public void updateRecipes(ClientRecipeBook book, RegistryAccess.Frozen registries) {
        this.register(SearchRegistry.RECIPE_COLLECTIONS, () -> this.lambda$updateRecipes$8(book, registries));
    }

    /** 1.21's reloader of the recipe tree: the book's collections by their results' text, built off the render thread */
    private void lambda$updateRecipes$8(ClientRecipeBook book, RegistryAccess.Frozen registries) {
        List<RecipeCollection> collections = book.getCollections();
        Item.TooltipContext context = Item.TooltipContext.of(registries);
        TooltipFlag flag = TooltipFlag.Default.NORMAL;
        this.recipeSearch = CompletableFuture.supplyAsync(() -> {
            FullTextSearchTree<RecipeCollection> tree = new FullTextSearchTree<>(
                    collection -> getTooltipLines(collection.getRecipes().stream().map(recipe -> recipe.value().getResultItem(registries)), context, flag),
                    collection -> collection.getRecipes().stream().map(recipe -> BuiltInRegistries.ITEM.getKey(recipe.value().getResultItem(registries).getItem())),
                    collections);
            tree.refresh(); // 1.20.6's tree is built by its refresh (1.21's by its constructor)
            return tree;
        }, Util.backgroundExecutor());
    }

    /** 1.21's recipes(): the whole tree, waited for (never a part of one) */
    public SearchTree<RecipeCollection> recipes() {
        return this.recipeSearch.join();
    }

    public SearchTree<ItemStack> creativeNameSearch() {
        return Minecraft.getInstance().getSearchTree(SearchRegistry.CREATIVE_NAMES);
    }

    public SearchTree<ItemStack> creativeTagSearch() {
        return Minecraft.getInstance().getSearchTree(SearchRegistry.CREATIVE_TAGS);
    }

    /** the recipe book's tree in 1.20.6's search registry: a search of this session's recipe tree; a refresh rebuilds */
    public static RefreshableSearchTree<RecipeCollection> recipeTree() {
        return new RefreshableSearchTree<>() {
            @Override
            public List<RecipeCollection> search(String query) {
                return CLIENT.recipes().search(query);
            }

            @Override
            public void refresh() {
                CLIENT.rebuildAfterLanguageChange();
            }
        };
    }
}
