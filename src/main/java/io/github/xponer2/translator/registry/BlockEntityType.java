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
package io.github.xponer2.translator.registry;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The type of a tile entity (1.21 {@code BlockEntityType}), registered in {@code Registries.BLOCK_ENTITY_TYPE}. When
 * it is registered it registers its tile entity class with 1.12.2 under the same id (1.12.2 creates tile entities
 * from NBT through that class and its no-arg constructor).
 */
public final class BlockEntityType<T extends TileEntity> implements RegistrationCallback {
    private final BlockEntitySupplier<? extends T> factory;
    private final Set<Block> validBlocks;
    private Class<? extends TileEntity> tileClass;

    private BlockEntityType(BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks) {
        this.factory = factory;
        this.validBlocks = validBlocks;
    }

    @Nullable
    public T create(BlockPos pos, IBlockState state) {
        var tile = this.factory.create(pos, state);
        if (tile != null) {
            tile.setPos(pos.toImmutable());
        }
        return tile;
    }

    public boolean isValid(IBlockState state) {
        return this.validBlocks.contains(state.getBlock());
    }

    public Set<Block> getValidBlocks() {
        return this.validBlocks;
    }

    @Nullable
    public Class<? extends TileEntity> getTileClass() {
        return this.tileClass;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRegistered(ResourceLocation id) {
        if (this.validBlocks.isEmpty()) {
            throw new IllegalStateException("Block entity type " + id + " has no blocks");
        }
        var sample = this.factory.create(BlockPos.ORIGIN, this.validBlocks.iterator().next().getDefaultState());
        this.tileClass = sample.getClass();
        GameRegistry.registerTileEntity(this.tileClass, id);
    }

    @FunctionalInterface
    public interface BlockEntitySupplier<T extends TileEntity> {
        T create(BlockPos pos, IBlockState state);
    }

    public static final class Builder<T extends TileEntity> {
        private final BlockEntitySupplier<? extends T> factory;
        private final Set<Block> validBlocks;

        private Builder(BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks) {
            this.factory = factory;
            this.validBlocks = validBlocks;
        }

        public static <T extends TileEntity> Builder<T> of(BlockEntitySupplier<? extends T> factory, Block... validBlocks) {
            return new Builder<>(factory, Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(validBlocks))));
        }

        /**
         * The argument is the 1.21 data fixer type (always null in the mods), ignored.
         */
        public BlockEntityType<T> build(@Nullable Object dataType) {
            return new BlockEntityType<>(this.factory, this.validBlocks);
        }
    }
}
