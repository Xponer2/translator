package io.github.xponer2.translator.item;

import io.github.xponer2.translator.component.DataComponentType;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The properties of an item (1.21 {@code Item.Properties}). A 1.12.2 item applies them after its constructor:
 * <pre>
 * super();
 * properties.applyTo(this);
 * </pre>
 * Stack size, durability, repair and crafting remainder go to the 1.12.2 item; the rest (rarity, fire resistance,
 * default data components) stays here and is found again with {@link #of(Item)} / {@link #defaults(Item)}.
 */
public class ItemProperties {
    private static final Map<Item, ItemProperties> BY_ITEM = Collections.synchronizedMap(new IdentityHashMap<>());

    // null: not set, the item keeps its own value (1.12.2 tools take theirs from the tool material)
    @Nullable
    private Integer maxStackSize;
    @Nullable
    private Integer maxDamage;
    private boolean canRepair = true;
    @Nullable
    private Item craftingRemainingItem;
    private EnumRarity rarity = EnumRarity.COMMON;
    private boolean fireResistant;
    private final Map<DataComponentType<?>, Object> components = new LinkedHashMap<>();

    public ItemProperties stacksTo(int maxStackSize) {
        if (this.maxDamage != null && this.maxDamage > 0 && maxStackSize > 1) {
            throw new IllegalStateException("Unable to have damage AND stack.");
        }
        this.maxStackSize = maxStackSize;
        return this;
    }

    public ItemProperties durability(int maxDamage) {
        this.maxDamage = maxDamage;
        this.maxStackSize = 1;
        return this;
    }

    public ItemProperties setNoRepair() {
        this.canRepair = false;
        return this;
    }

    public ItemProperties craftRemainder(Item item) {
        this.craftingRemainingItem = item;
        return this;
    }

    public ItemProperties rarity(EnumRarity rarity) {
        this.rarity = rarity;
        return this;
    }

    public ItemProperties fireResistant() {
        this.fireResistant = true;
        return this;
    }

    /**
     * A default data component of the item, null removes it.
     */
    public <T> ItemProperties component(DataComponentType<T> type, @Nullable T value) {
        if (value == null) {
            this.components.remove(type);
        } else {
            this.components.put(type, value);
        }
        return this;
    }

    /**
     * NeoForge's form with a registry holder (resolved now: data component types are created on first use).
     */
    public <T> ItemProperties component(Supplier<? extends DataComponentType<T>> type, @Nullable T value) {
        return this.component(type.get(), value);
    }

    public void applyTo(Item item) {
        if (this.maxStackSize != null) {
            item.setMaxStackSize(this.maxStackSize);
        }
        if (this.maxDamage != null) {
            item.setMaxDamage(this.maxDamage);
        }
        if (!this.canRepair) {
            item.setNoRepair();
        }
        if (this.craftingRemainingItem != null) {
            item.setContainerItem(this.craftingRemainingItem);
        }
        BY_ITEM.put(item, this);
    }

    /**
     * The properties an item was made with, null for items that don't use them (vanilla, other mods).
     */
    @Nullable
    public static ItemProperties of(Item item) {
        return BY_ITEM.get(item);
    }

    /**
     * The default data components of an item, by component type (empty for items without properties).
     */
    public static Map<DataComponentType<?>, Object> defaults(Item item) {
        var properties = of(item);
        return properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(properties.components);
    }

    public EnumRarity rarity() {
        return this.rarity;
    }

    public boolean isFireResistant() {
        return this.fireResistant;
    }
}
