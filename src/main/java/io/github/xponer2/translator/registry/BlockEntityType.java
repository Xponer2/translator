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
 * from NBT through that class and its no-arg constructor). Not final: mods extend it (SuperMartijn642's Core Lib's
 * BaseBlockEntityType, and with it every block entity of his mods, couldn't load).
 */
public class BlockEntityType<T extends TileEntity> implements RegistrationCallback {
    /** The types whose sample couldn't be made when they were registered: their class is registered later. */
    private static final java.util.List<BlockEntityType<?>> PENDING = new java.util.ArrayList<>();

    /** 1.21 vanilla sign types: 1.12.2's signs are its own; a mod's sign block entity made through them gets none. */
    @Nullable
    public static BlockEntityType<?> SIGN;
    @Nullable
    public static BlockEntityType<?> HANGING_SIGN;

    private final BlockEntitySupplier<? extends T> factory;
    private final Set<Block> validBlocks;
    private Class<? extends TileEntity> tileClass;
    private ResourceLocation id;
    /** A vanilla 1.21 type of a block 1.12.2 hasn't: any block may hold one (the mods' own vaults, pots...). */
    private boolean anyBlock;
    /** Its carried 1.21 block entity class, registered for the saved ones before a world loads. */
    @Nullable
    private String carriedClass;

    /** The vanilla 1.21 types whose block entities are carried 1.21 classes (their blocks are new in 1.21). */
    private static final java.util.List<BlockEntityType<?>> CARRIED = new java.util.ArrayList<>();

    // the vanilla 1.21 types of blocks 1.12.2 hasn't: the carried block entity classes name them. They were null, so
    // such a block entity had no type and was saved under its block's id (Iron's Spells' bone vault, a VaultBlockEntity,
    // was lost when the world was opened again)
    public static final BlockEntityType<TileEntity> VAULT = carried("vault", "vault.VaultBlockEntity");
    public static final BlockEntityType<TileEntity> TRIAL_SPAWNER = carried("trial_spawner", "TrialSpawnerBlockEntity");
    public static final BlockEntityType<TileEntity> CRAFTER = carried("crafter", "CrafterBlockEntity");
    public static final BlockEntityType<TileEntity> DECORATED_POT = carried("decorated_pot", "DecoratedPotBlockEntity");
    public static final BlockEntityType<TileEntity> BRUSHABLE_BLOCK = carried("brushable_block", "BrushableBlockEntity");
    public static final BlockEntityType<TileEntity> CHISELED_BOOKSHELF = carried("chiseled_bookshelf", "ChiseledBookShelfBlockEntity");
    public static final BlockEntityType<TileEntity> SCULK_SENSOR = carried("sculk_sensor", "SculkSensorBlockEntity");
    public static final BlockEntityType<TileEntity> CALIBRATED_SCULK_SENSOR = carried("calibrated_sculk_sensor", "CalibratedSculkSensorBlockEntity");
    public static final BlockEntityType<TileEntity> SCULK_CATALYST = carried("sculk_catalyst", "SculkCatalystBlockEntity");
    public static final BlockEntityType<TileEntity> SCULK_SHRIEKER = carried("sculk_shrieker", "SculkShriekerBlockEntity");
    public static final BlockEntityType<TileEntity> CAMPFIRE = carried("campfire", "CampfireBlockEntity");
    public static final BlockEntityType<TileEntity> BEEHIVE = carried("beehive", "BeehiveBlockEntity");
    public static final BlockEntityType<TileEntity> LECTERN = carried("lectern", "LecternBlockEntity");
    public static final BlockEntityType<TileEntity> BELL = carried("bell", "BellBlockEntity");
    public static final BlockEntityType<TileEntity> BARREL = carried("barrel", "BarrelBlockEntity");
    public static final BlockEntityType<TileEntity> SMOKER = carried("smoker", "SmokerBlockEntity");
    public static final BlockEntityType<TileEntity> BLAST_FURNACE = carried("blast_furnace", "BlastFurnaceBlockEntity");
    public static final BlockEntityType<TileEntity> CONDUIT = carried("conduit", "ConduitBlockEntity");
    public static final BlockEntityType<TileEntity> JIGSAW = carried("jigsaw", "JigsawBlockEntity");

    private static BlockEntityType<TileEntity> carried(String name, String className) {
        var type = new BlockEntityType<TileEntity>((pos, state) -> {
            throw new UnsupportedOperationException("Translator: a 1.21 " + name + " block entity is made by its block");
        }, java.util.Collections.emptySet());
        type.id = new ResourceLocation(name);
        type.anyBlock = true;
        type.carriedClass = "net.minecraft.world.level.block.entity." + className;
        CARRIED.add(type);
        return type;
    }

    /** The type's id (the one its block entities are saved under), null before it is registered. */
    @Nullable
    public ResourceLocation getId() {
        return this.id;
    }

    private BlockEntityType(BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks) {
        this.factory = factory;
        this.validBlocks = validBlocks;
    }

    /**
     * 1.21 {@code new BlockEntityType(factory, validBlocks, dataType)} (the data fixer type, unused): Refined Storage
     * makes its types this way, not through the builder; without it none of its block entities were made.
     */
    public BlockEntityType(BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks, @Nullable com.mojang.datafixers.types.Type<?> dataType) {
        this(factory, validBlocks);
    }

    /**
     * A 1.21 vanilla type over 1.12.2's tile entity of that kind (VanillaBlockEntityTypes): its id and class are known,
     * its blocks a set the mods may add theirs to (Twilight Forest's chests join CHEST).
     */
    static <T extends TileEntity> BlockEntityType<T> vanilla(String path, Class<T> type, Block... blocks) {
        BlockEntityType<T> made = new BlockEntityType<>((pos, state) -> {
            try {
                return type.getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }, new java.util.HashSet<>(java.util.Arrays.asList(blocks)));
        made.id = new ResourceLocation("minecraft", path);
        made.tileClass = type;
        return made;
    }

    @Nullable
    public T create(BlockPos pos, IBlockState state) {
        var tile = this.factory.create(pos, state);
        if (tile != null) {
            tile.setPos(pos.toImmutable());
            if (this.tileClass == null && this.id != null) {
                this.registerClass(tile.getClass());
            }
        }
        return tile;
    }

    public boolean isValid(IBlockState state) {
        return this.anyBlock || this.validBlocks.contains(state.getBlock());
    }

    /** Whether the block entity is one of this type (1.21 {@code blockEntity.getType() == type}). */
    public boolean is(@Nullable TileEntity tile) {
        if (tile == null) {
            return false;
        }
        if (tile instanceof io.github.xponer2.translator.block.BlockEntity facade && facade.getType() != null) {
            return facade.getType() == this;
        }
        if (tile instanceof io.github.xponer2.translator.runtime.mc.SignBlockEntity sign && sign.getType() != null) {
            return sign.getType() == this;
        }
        return this.tileClass != null && this.tileClass == tile.getClass();
    }

    /** 1.21 {@code getBlockEntity(level, pos)}: the block entity there when it is of this type, else null. */
    @Nullable
    @SuppressWarnings("unchecked")
    public T getBlockEntity(net.minecraft.world.IBlockAccess level, BlockPos pos) {
        TileEntity tile = level.getTileEntity(pos);
        return this.is(tile) ? (T) tile : null;
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
        this.id = id;
        try {
            this.registerClass(this.sample().getClass());
        } catch (RuntimeException | LinkageError e) {
            // a block entity that reads a server config when it is made (Refurbished Furniture's sinks and mailboxes:
            // Framework links those when the server starts): the type is there, its class is registered when the
            // server is about to start or when one is first made. NeoForge makes none at registration.
            synchronized (PENDING) {
                PENDING.add(this);
            }
            org.apache.logging.log4j.LogManager.getLogger("Translator Runtime").info("Block entity {}: no sample yet ({}), its class is registered later", id, e.toString());
        }
    }

    private T sample() {
        return this.factory.create(BlockPos.ORIGIN, this.validBlocks.iterator().next().getDefaultState());
    }

    @SuppressWarnings("deprecation")
    private void registerClass(Class<? extends TileEntity> type) {
        this.tileClass = type;
        if (TileEntity.getKey(type) == null) {
            GameRegistry.registerTileEntity(type, this.id);
        }
    }

    /**
     * The classes of the types whose sample failed, made again once the mods set up their server (before the worlds
     * load, whose saved block entities need them).
     */
    public static void registerPending() {
        // the carried vanilla ones first: a saved one is made by its class when its chunk loads
        for (BlockEntityType<?> type : CARRIED) {
            if (type.tileClass != null) {
                continue;
            }
            try {
                Class<?> found = Class.forName(type.carriedClass, true, net.minecraft.launchwrapper.Launch.classLoader);
                if (TileEntity.class.isAssignableFrom(found)) {
                    type.registerClass(found.asSubclass(TileEntity.class));
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                // not carried (a mapped class, or no player's 1.21.1 to carry it from): nothing to register
            }
        }
        java.util.List<BlockEntityType<?>> pending;
        synchronized (PENDING) {
            pending = new java.util.ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (BlockEntityType<?> type : pending) {
            if (type.tileClass != null) {
                continue;
            }
            try {
                type.registerClass(type.sample().getClass());
            } catch (RuntimeException | LinkageError e) {
                io.github.xponer2.translator.runtime.RuntimeStubs.failed("the block entity " + type.id + " (made for its class)", e);
            }
        }
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
        /** 1.21 signature (the data fixer type, unused). */
        public BlockEntityType<T> build(@Nullable com.mojang.datafixers.types.Type<?> dataType) {
            return this.build((Object) dataType);
        }

        public BlockEntityType<T> build(@Nullable Object dataType) {
            return new BlockEntityType<>(this.factory, this.validBlocks);
        }
    }
}
