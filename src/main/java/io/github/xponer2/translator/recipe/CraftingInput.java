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
package io.github.xponer2.translator.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The grid of a crafting recipe (1.21 {@code CraftingInput}).
 */
public class CraftingInput implements RecipeInput {
    public static final CraftingInput EMPTY = new CraftingInput(0, 0, Collections.emptyList());

    private final int width;
    private final int height;
    private final List<ItemStack> items;
    private final int ingredientCount;

    protected CraftingInput(int width, int height, List<ItemStack> items) {
        this.width = width;
        this.height = height;
        this.items = items;
        int count = 0;
        for (var stack : items) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        this.ingredientCount = count;
    }

    public static CraftingInput of(int width, int height, List<ItemStack> items) {
        if (width == 0 || height == 0) {
            return EMPTY;
        }
        if (items.size() != width * height) {
            throw new IllegalArgumentException("Invalid crafting input: " + width + "x" + height + " with " + items.size() + " items");
        }
        return new CraftingInput(width, height, Collections.unmodifiableList(new ArrayList<>(items)));
    }

    /**
     * The 1.12.2 crafting grid, cut to the smallest rectangle around its items like 1.21 does.
     */
    public static CraftingInput of(InventoryCrafting inventory) {
        return ofPositioned(inventory).input();
    }

    /**
     * The cut grid and where it starts in the 1.12.2 grid (1.21 {@code CraftingInput.ofPositioned}).
     */
    public static Positioned ofPositioned(InventoryCrafting inventory) {
        int w = inventory.getWidth();
        int h = inventory.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!inventory.getStackInRowAndColumn(x, y).isEmpty()) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return new Positioned(EMPTY, 0, 0);
        }
        var items = new ArrayList<ItemStack>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                items.add(inventory.getStackInRowAndColumn(x, y));
            }
        }
        return new Positioned(of(maxX - minX + 1, maxY - minY + 1, items), minX, minY);
    }

    @Override
    public ItemStack getItem(int index) {
        return this.items.get(index);
    }

    public ItemStack getItem(int x, int y) {
        return this.items.get(x + y * this.width);
    }

    @Override
    public int size() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.ingredientCount == 0;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public List<ItemStack> items() {
        return this.items;
    }

    public int ingredientCount() {
        return this.ingredientCount;
    }

    public static final class Positioned {
        private final CraftingInput input;
        private final int left;
        private final int top;

        public Positioned(CraftingInput input, int left, int top) {
            this.input = input;
            this.left = left;
            this.top = top;
        }

        public CraftingInput input() {
            return this.input;
        }

        public int left() {
            return this.left;
        }

        public int top() {
            return this.top;
        }
    }
}
