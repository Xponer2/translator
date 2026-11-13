/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.item;

import io.github.xponer2.translator.registry.RegistrationCallback;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A creative tab (1.21 {@code CreativeModeTab}, registered in {@code Registries.CREATIVE_MODE_TAB}). When registered
 * it creates the 1.12.2 tab, labeled with the id's namespace (its name is the lang key {@code itemGroup.<label>}), lists
 * the items of its generator and puts those items into the tab (JEI and the search tab find items by their tab).
 * Mods extend it (NeoForge's protected constructor from a builder: SuperMartijn642's CreativeItemGroup, owo's
 * OwoItemGroup).
 */
public class CreativeModeTab implements RegistrationCallback {
    private final ITextComponent title;
    private final Supplier<ItemStack> icon;
    /**
     * The 1.21 field name: a mod's subclass sets it after the constructor (SuperMartijn642's CreativeItemGroup gives
     * its own item list so; its constructor was skipped without the field, and with it every tab of those mods).
     */
    protected DisplayItemsGenerator displayItemsGenerator;
    /** 1.21 {@code background} (a tab's background texture, SuperMartijn642 sets it): 1.12.2 draws its own. */
    @Nullable
    protected ResourceLocation background;
    @Nullable
    private CreativeTabs vanilla;

    protected CreativeModeTab(Builder builder) {
        this.title = builder.title;
        this.icon = builder.icon;
        this.displayItemsGenerator = builder.generator;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ITextComponent getDisplayName() {
        return this.title;
    }

    @Nullable
    public CreativeTabs vanilla() {
        return this.vanilla;
    }

    /** 1.21 {@code getDisplayItems()}: the items the tab shows, made when asked. */
    public java.util.Collection<ItemStack> getDisplayItems() {
        return this.displayItems();
    }

    private List<ItemStack> displayItems() {
        var stacks = new ArrayList<ItemStack>();
        try {
            this.displayItemsGenerator.accept(ItemDisplayParameters.INSTANCE, (stack, visibility) -> {
                // 1.12.2 has no separate search tab list: an item only for the search tab isn't in the tab
                if (stack != null && !stack.isEmpty() && visibility != TabVisibility.SEARCH_TAB_ONLY) {
                    stacks.add(stack);
                }
            });
        } catch (RuntimeException | LinkageError e) {
            // a tab of an original 1.21.1 mod whose list can't be made completely: what was added so far
            io.github.xponer2.translator.runtime.RuntimeMods.skipped("creative tab " + this.title.getUnformattedText(), e);
        }
        return stacks;
    }

    /** 1.21: the tab's list is made when asked here (getDisplayItems), nothing to build ahead. */
    public void buildContents(ItemDisplayParameters parameters) {
    }

    /** 1.21 {@code shouldDisplay()}: whether the tab shows (JEI names an item's tabs by it); 1.12.2 shows every tab. */
    public boolean shouldDisplay() {
        return true;
    }

    public boolean contains(ItemStack stack) {
        for (ItemStack shown : this.displayItems()) {
            if (ItemStack.areItemsEqual(shown, stack)) {
                return true;
            }
        }
        return false;
    }

    public java.util.Collection<ItemStack> getSearchTabDisplayItems() {
        return this.displayItems();
    }

    public Type getType() {
        return Type.CATEGORY;
    }

    /** 1.21 {@code CreativeModeTab.Type}: a mod's tab is a category. */
    public enum Type {
        CATEGORY,
        INVENTORY,
        HOTBAR,
        SEARCH
    }

    @Override
    public void onRegistered(ResourceLocation id) {
        var tab = this;
        this.vanilla = new CreativeTabs(id.getNamespace()) {
            @Override
            @SideOnly(Side.CLIENT)
            public ItemStack createIcon() {
                try {
                    ItemStack icon = tab.icon.get();
                    if (icon != null && !icon.isEmpty()) {
                        return icon;
                    }
                } catch (RuntimeException | LinkageError e) {
                    // the icon of an original 1.21.1 mod's tab is an item the Translator couldn't make: the tab's first item
                }
                List<ItemStack> items = tab.displayItems();
                return items.isEmpty() ? new ItemStack(net.minecraft.init.Items.PAPER) : items.get(0);
            }

            @Override
            @SideOnly(Side.CLIENT)
            public void displayAllRelevantItems(NonNullList<ItemStack> items) {
                items.addAll(tab.displayItems());
            }
        };
        List<ItemStack> items = this.displayItems();
        for (var stack : items) {
            Item item = stack.getItem();
            if (item.getCreativeTab() == null) {
                item.setCreativeTab(this.vanilla);
                // a block item's tab is its block's (1.12.2 ItemBlock.getCreativeTab): without it the item was in no tab,
                // and in no recipe either (an ingredient lists its items by their tabs)
                if (item instanceof net.minecraft.item.ItemBlock blockItem && blockItem.getBlock() != null
                        && blockItem.getBlock().getCreativeTab() == null) {
                    blockItem.getBlock().setCreativeTab(this.vanilla);
                }
            }
        }
        LOGGER.info("Creative tab {}: {} items", id, items.size());
    }

    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");

    @FunctionalInterface
    public interface DisplayItemsGenerator {
        void accept(ItemDisplayParameters parameters, Output output);
    }

    /**
     * 1.21 {@code CreativeModeTab.Output}: as in 1.21 the item with its visibility is the method to implement, the
     * others come to it (Cucumber's tab lists implement only that one: Mystical Agriculture's and Iron Jetpacks' tabs
     * were empty, the one-item method was missing).
     */
    @FunctionalInterface
    public interface Output {
        void accept(ItemStack stack, TabVisibility visibility);

        default void accept(ItemStack stack) {
            this.accept(stack, TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        default void accept(net.minecraft.item.Item item) {
            this.accept(new ItemStack(item));
        }

        default void accept(io.github.xponer2.translator.util.ItemLike item) {
            this.accept(new ItemStack(io.github.xponer2.translator.util.ItemLike.item(item)));
        }

        default void accept(io.github.xponer2.translator.util.ItemLike item, TabVisibility visibility) {
            this.accept(new ItemStack(io.github.xponer2.translator.util.ItemLike.item(item)), visibility);
        }

        default void acceptAll(java.util.Collection<ItemStack> stacks) {
            for (ItemStack stack : stacks) {
                this.accept(stack);
            }
        }

        default void acceptAll(java.util.Collection<ItemStack> stacks, TabVisibility visibility) {
            for (ItemStack stack : stacks) {
                this.accept(stack, visibility);
            }
        }
    }

    /** 1.21 {@code CreativeModeTab.TabVisibility}: where an item of the tab shows (the tab, the search tab or both). */
    public enum TabVisibility {
        PARENT_AND_SEARCH_TABS,
        PARENT_TAB_ONLY,
        SEARCH_TAB_ONLY
    }

    /**
     * 1.21 feature flags and permissions; 1.12.2 shows every item.
     */
    public static final class ItemDisplayParameters {
        public static final ItemDisplayParameters INSTANCE = new ItemDisplayParameters();

        private ItemDisplayParameters() { }

        public boolean hasPermissions() {
            return true;
        }

        public io.github.xponer2.translator.registry.HolderLookup.Provider holders() {
            return io.github.xponer2.translator.registry.RegistryAccess.INSTANCE;
        }

        public io.github.xponer2.translator.util.FeatureFlagSet enabledFeatures() {
            return io.github.xponer2.translator.util.FeatureFlagSet.of();
        }
    }

    public static final class Builder {
        public Builder() {
        }

        /** 1.21 {@code new CreativeModeTab.Builder(row, column)}: 1.12.2 places the tabs itself. */
        public Builder(Object row, int column) {
        }

        private ITextComponent title;
        private Supplier<ItemStack> icon = () -> ItemStack.EMPTY;
        private DisplayItemsGenerator generator = (parameters, output) -> { };

        public Builder title(ITextComponent title) {
            this.title = title;
            return this;
        }

        public Builder icon(Supplier<ItemStack> icon) {
            this.icon = icon;
            return this;
        }

        public Builder displayItems(DisplayItemsGenerator generator) {
            this.generator = generator;
            return this;
        }

        // 1.21 tab placement and look: 1.12.2 orders tabs by registration and draws them all alike
        @SafeVarargs
        public final Builder withTabsBefore(io.github.xponer2.translator.registry.ResourceKey<CreativeModeTab>... tabs) {
            return this;
        }

        @SafeVarargs
        public final Builder withTabsAfter(io.github.xponer2.translator.registry.ResourceKey<CreativeModeTab>... tabs) {
            return this;
        }

        /** NeoForge {@code withTabsBefore(ids...)}: by the tabs' ids. */
        public Builder withTabsBefore(ResourceLocation... tabs) {
            return this;
        }

        public Builder withTabsAfter(ResourceLocation... tabs) {
            return this;
        }

        public Builder withSearchBar() {
            return this;
        }

        public Builder withSearchBar(int width) {
            return this;
        }

        public Builder hideTitle() {
            return this;
        }

        public Builder noScrollBar() {
            return this;
        }

        public Builder alignedRight() {
            return this;
        }

        public CreativeModeTab build() {
            return new CreativeModeTab(this);
        }
    }
}
