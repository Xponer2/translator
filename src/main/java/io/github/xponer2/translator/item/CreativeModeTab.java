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
 */
public final class CreativeModeTab implements RegistrationCallback {
    private final ITextComponent title;
    private final Supplier<ItemStack> icon;
    private final DisplayItemsGenerator generator;
    @Nullable
    private CreativeTabs vanilla;

    private CreativeModeTab(Builder builder) {
        this.title = builder.title;
        this.icon = builder.icon;
        this.generator = builder.generator;
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

    public List<ItemStack> getDisplayItems() {
        var stacks = new ArrayList<ItemStack>();
        this.generator.accept(ItemDisplayParameters.INSTANCE, stacks::add);
        return stacks;
    }

    @Override
    public void onRegistered(ResourceLocation id) {
        var tab = this;
        this.vanilla = new CreativeTabs(id.getNamespace()) {
            @Override
            @SideOnly(Side.CLIENT)
            public ItemStack createIcon() {
                return tab.icon.get();
            }

            @Override
            @SideOnly(Side.CLIENT)
            public void displayAllRelevantItems(NonNullList<ItemStack> items) {
                items.addAll(tab.getDisplayItems());
            }
        };
        for (var stack : this.getDisplayItems()) {
            Item item = stack.getItem();
            if (item.getCreativeTab() == null) {
                item.setCreativeTab(this.vanilla);
            }
        }
    }

    @FunctionalInterface
    public interface DisplayItemsGenerator {
        void accept(ItemDisplayParameters parameters, Output output);
    }

    @FunctionalInterface
    public interface Output {
        void accept(ItemStack stack);

        default void accept(net.minecraft.item.Item item) {
            this.accept(new ItemStack(item));
        }

        default void accept(io.github.xponer2.translator.util.ItemLike item) {
            this.accept(new ItemStack(item.asItem()));
        }
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
    }

    public static final class Builder {
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

        public CreativeModeTab build() {
            return new CreativeModeTab(this);
        }
    }
}
