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
package io.github.xponer2.translator.recipe.ingredient;

import io.github.xponer2.translator.component.DataComponentPatch;
import io.github.xponer2.translator.component.DataComponentType;
import io.github.xponer2.translator.component.StackComponents;
import io.github.xponer2.translator.util.Codecs;
import io.github.xponer2.translator.util.ItemIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Items with given component values (NeoForge {@code DataComponentIngredient}, type {@code neoforge:components}):
 * {@code {"items": id | "#tag" | [...], "components": {type: value}, "strict": false}}. Not strict: the stack has
 * these values (and maybe others); strict: its own values are exactly these.
 */
public class DataComponentIngredient implements ICustomIngredient {
    private static final Codec<List<String>> ITEMS = Codec.either(Codec.STRING, Codec.STRING.listOf())
            .xmap(either -> either.map(Collections::singletonList, list -> list), list -> com.mojang.datafixers.util.Either.right(list));
    public static final MapCodec<DataComponentIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ITEMS.fieldOf("items").forGetter(i -> i.items),
            DataComponentPatch.CODEC.fieldOf("components").forGetter(i -> i.components),
            Codec.BOOL.optionalFieldOf("strict", false).forGetter(i -> i.strict)
    ).apply(instance, DataComponentIngredient::new));

    private final List<String> items;
    private final DataComponentPatch components;
    private final boolean strict;
    @Nullable
    private ItemStack[] prototypes;

    public DataComponentIngredient(List<String> items, DataComponentPatch components, boolean strict) {
        this.items = items;
        this.components = components;
        this.strict = strict;
    }

    public static Ingredient of(boolean strict, ItemStack stack) {
        var id = ItemIds.id(stack);
        var items = id == null ? Collections.<String>emptyList() : Collections.singletonList(id.toString());
        return new DataComponentIngredient(items, StackComponents.getComponentsPatch(stack), strict).toVanilla();
    }

    public static <T> Ingredient of(boolean strict, DataComponentType<T> type, T value, Item... items) {
        var ids = new ArrayList<String>();
        for (var item : items) {
            ids.add(String.valueOf(item.getRegistryName()));
        }
        return new DataComponentIngredient(ids, DataComponentPatch.builder().set(type, value).build(), strict).toVanilla();
    }

    public List<String> items() {
        return this.items;
    }

    public DataComponentPatch components() {
        return this.components;
    }

    public boolean isStrict() {
        return this.strict;
    }

    @Override
    public boolean test(ItemStack stack) {
        boolean item = false;
        for (var prototype : this.prototypes()) {
            if (OreDictionary.itemMatches(prototype, stack, false)) {
                item = true;
                break;
            }
        }
        if (!item) {
            return false;
        }
        if (this.strict) {
            return StackComponents.getComponentsPatch(stack).equals(this.components);
        }
        for (var entry : this.components.entrySet()) {
            if (!Objects.equals(StackComponents.get(stack, entry.getKey()), entry.getValue().orElse(null))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(this.prototypes()).map(prototype -> {
            var stack = prototype.copy();
            if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE) {
                stack.setItemDamage(0);
            }
            StackComponents.applyComponents(stack, this.components);
            return stack;
        });
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return NeoForgeIngredientTypes.DATA_COMPONENT;
    }

    private ItemStack[] prototypes() {
        if (this.prototypes == null) {
            var stacks = new ArrayList<ItemStack>();
            for (var entry : this.items) {
                if (entry.startsWith("#")) {
                    stacks.addAll(Codecs.itemsOfTag(io.github.xponer2.translator.util.ResourceLocations.parse(entry.substring(1))));
                } else {
                    var stack = ItemIds.stack(io.github.xponer2.translator.util.ResourceLocations.parse(entry), 1);
                    if (!stack.isEmpty()) {
                        if (!stack.getHasSubtypes()) {
                            stack.setItemDamage(OreDictionary.WILDCARD_VALUE);
                        }
                        stacks.add(stack);
                    }
                }
            }
            this.prototypes = stacks.toArray(new ItemStack[0]);
        }
        return this.prototypes;
    }
}
