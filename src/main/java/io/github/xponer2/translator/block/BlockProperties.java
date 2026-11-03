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
package io.github.xponer2.translator.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * The properties of a block (1.21 {@code BlockBehaviour.Properties}). A 1.12.2 block gets its material and map color
 * in the constructor ({@link #material()}, {@link #mapColor()}), the rest from {@link #applyTo(Block, Consumer)}:
 * <pre>
 * super(properties.material(), properties.mapColor());
 * properties.applyTo(this, this::setSoundType);
 * </pre>
 * 1.21 has no materials; the material decides in 1.12.2 whether a tool is needed for drops, so it follows
 * {@link #requiresCorrectToolForDrops()} unless set with {@link #material(Material)}.
 */
public class BlockProperties {
    // the fields 1.21 BlockBehaviour.Properties has under the same name and type are public: mods read and write
    // them directly (access transformers; Twilight Forest copies destroyTime), a private one is an IllegalAccessError
    private Material material;
    private MapColor mapColor;
    public SoundType soundType = SoundType.STONE;
    public float destroyTime;
    public float explosionResistance;
    private int lightLevel;
    public java.util.function.ToIntFunction<IBlockState> lightEmission;
    @Nullable
    public net.minecraft.block.material.EnumPushReaction pushReaction;
    public boolean requiresCorrectToolForDrops;
    private boolean randomTicks;
    public boolean hasCollision = true;
    public boolean canOcclude = true;
    private boolean noLootTable;
    /**
     * 1.21 {@code BlockBehaviour.Properties.drops} (the loot table key): Registrate resets it by reflection for every
     * block it makes (ObfuscationReflectionHelper by name), so it has to exist. 1.12.2 blocks drop by their own code.
     */
    @SuppressWarnings("unused")
    private Object drops;
    private StateArgumentPredicate<Object> isValidSpawn;
    private StatePredicate isRedstoneConductor;
    private StatePredicate isSuffocating;
    private StatePredicate isViewBlocking;

    protected BlockProperties() { }

    public static BlockProperties of() {
        return new BlockProperties();
    }

    /**
     * The properties of an existing 1.12.2 block (material, map color, hardness, resistance, sound, light).
     */
    @SuppressWarnings("deprecation")
    public static BlockProperties ofFullCopy(Block block) {
        if (block == null || block == net.minecraft.init.Blocks.AIR) {
            // a 1.21 block 1.12.2 doesn't have (Blocks.BARREL): the default properties
            return of();
        }
        var state = block.getDefaultState();
        var properties = new BlockProperties();
        properties.material = state.getMaterial();
        properties.mapColor = block.getMapColor(state, null, null);
        properties.soundType = block.getSoundType();
        properties.destroyTime = block.getBlockHardness(state, null, null);
        // 1.12.2 keeps 3x the given resistance and uses a fifth of that, 1.21 uses the given value
        properties.explosionResistance = block.getExplosionResistance(null);
        properties.lightLevel = state.getLightValue();
        properties.randomTicks = block.getTickRandomly();
        properties.requiresCorrectToolForDrops = !properties.material.isToolNotRequired();
        properties.hasCollision = state.getMaterial().blocksMovement();
        properties.canOcclude = state.isOpaqueCube();
        return properties;
    }

    /** 1.21 {@code ofLegacyCopy}: like {@link #ofFullCopy} (1.12.2 has no newer properties to leave out). */
    public static BlockProperties ofLegacyCopy(Block block) {
        return ofFullCopy(block);
    }

    /** 1.21 {@code lootFrom}: drops like another block. 1.12.2 blocks drop by their own code: the block keeps its own. */
    public BlockProperties lootFrom(java.util.function.Supplier<?> block) {
        return this;
    }

    public BlockProperties material(Material material) {
        this.material = material;
        return this;
    }

    /** The note a note block plays on this block (1.21): 1.12.2 note blocks ask the material. */
    public BlockProperties instrument(Object instrument) {
        return this;
    }

    /** A map color by state (logs: top and side): the default state's. */
    public BlockProperties mapColor(java.util.function.Function<net.minecraft.block.state.IBlockState, MapColor> mapColor) {
        this.mapColorFunction = mapColor;
        return this;
    }

    @javax.annotation.Nullable
    private java.util.function.Function<net.minecraft.block.state.IBlockState, MapColor> mapColorFunction;

    public BlockProperties mapColor(net.minecraft.item.EnumDyeColor color) {
        return this.mapColor(MapColor.getBlockColor(color));
    }

    /** How slippery the block is (ice 0.98): the 1.12.2 block's slipperiness. */
    public BlockProperties friction(float friction) {
        this.friction = friction;
        return this;
    }

    public float friction = 0.6F;

    public float friction() {
        return this.friction;
    }

    // 1.21 properties 1.12.2 blocks don't have: taken, not applied
    public BlockProperties speedFactor(float factor) {
        return this;
    }

    public BlockProperties jumpFactor(float factor) {
        return this;
    }

    public BlockProperties emissiveRendering(Object predicate) {
        return this;
    }

    public BlockProperties hasPostProcess(Object predicate) {
        return this;
    }

    /** Random offsets of plants in a block (1.21): 1.12.2 offsets by the block's own offset type. */
    public BlockProperties offsetType(Object offsetType) {
        return this;
    }

    public BlockProperties requiredFeatures(Object... features) {
        return this;
    }

    public BlockProperties dropsLike(net.minecraft.block.Block block) {
        return this;
    }

    public BlockProperties overrideLootTable(Object lootTable) {
        return this;
    }

    public BlockProperties overrideDescription(String description) {
        return this;
    }

    /** The block of a fluid (1.21 liquid()): no collision, replaceable. */
    public BlockProperties liquid() {
        return this.noCollission().replaceable();
    }

    public BlockProperties mapColor(MapColor mapColor) {
        this.mapColor = mapColor;
        return this;
    }

    public BlockProperties sound(SoundType soundType) {
        // a 1.21 sound type can miss sounds 1.12.2 doesn't have (null): placing the block would stop the game
        this.soundType = io.github.xponer2.translator.runtime.redirect.SoundFallback.complete(soundType);
        return this;
    }

    public BlockProperties strength(float destroyTime, float explosionResistance) {
        this.destroyTime = destroyTime;
        this.explosionResistance = Math.max(0.0F, explosionResistance);
        return this;
    }

    public BlockProperties strength(float strength) {
        return this.strength(strength, strength);
    }

    public BlockProperties destroyTime(float destroyTime) {
        this.destroyTime = destroyTime;
        return this;
    }

    public BlockProperties explosionResistance(float explosionResistance) {
        this.explosionResistance = Math.max(0.0F, explosionResistance);
        return this;
    }

    public BlockProperties instabreak() {
        return this.strength(0.0F);
    }

    /** 1.21 form: the light of each state (1.12.2 blocks have one light level; the default state's is used). */
    public BlockProperties lightLevel(java.util.function.ToIntFunction<IBlockState> lightEmission) {
        this.lightEmission = lightEmission;
        return this;
    }

    public BlockProperties pushReaction(net.minecraft.block.material.EnumPushReaction pushReaction) {
        this.pushReaction = pushReaction;
        return this;
    }

    @Nullable
    public net.minecraft.block.material.EnumPushReaction pushReaction() {
        return this.pushReaction;
    }

    /** 1.21 properties 1.12.2 blocks have no counterpart for: accepted, no effect. */
    public BlockProperties replaceable() {
        return this;
    }

    public BlockProperties noTerrainParticles() {
        return this;
    }

    public BlockProperties dynamicShape() {
        return this;
    }

    public BlockProperties forceSolidOn() {
        return this;
    }

    public BlockProperties forceSolidOff() {
        return this;
    }

    public BlockProperties ignitedByLava() {
        return this;
    }

    public BlockProperties air() {
        return this.noCollission().noLootTable();
    }

    public BlockProperties lightLevel(int lightLevel) {
        this.lightLevel = lightLevel;
        return this;
    }

    public BlockProperties requiresCorrectToolForDrops() {
        this.requiresCorrectToolForDrops = true;
        return this;
    }

    public BlockProperties randomTicks() {
        this.randomTicks = true;
        return this;
    }

    public BlockProperties noCollission() {
        this.hasCollision = false;
        this.canOcclude = false;
        return this;
    }

    public BlockProperties noOcclusion() {
        this.canOcclude = false;
        return this;
    }

    public BlockProperties noLootTable() {
        this.noLootTable = true;
        return this;
    }

    @SuppressWarnings("unchecked")
    public BlockProperties isValidSpawn(StateArgumentPredicate<?> predicate) {
        this.isValidSpawn = (StateArgumentPredicate<Object>) predicate;
        return this;
    }

    public BlockProperties isRedstoneConductor(StatePredicate predicate) {
        this.isRedstoneConductor = predicate;
        return this;
    }

    public BlockProperties isSuffocating(StatePredicate predicate) {
        this.isSuffocating = predicate;
        return this;
    }

    public BlockProperties isViewBlocking(StatePredicate predicate) {
        this.isViewBlocking = predicate;
        return this;
    }

    /**
     * The 1.12.2 material: the one set, else rock (pickaxe needed) for blocks that need a tool for drops, else one
     * that fits the sound.
     */
    public Material material() {
        if (this.material != null) {
            return this.material;
        }
        if (this.requiresCorrectToolForDrops) {
            return this.soundType == SoundType.METAL || this.soundType == SoundType.ANVIL ? Material.IRON : Material.ROCK;
        }
        if (this.soundType == SoundType.GLASS) {
            return Material.GLASS;
        }
        if (this.soundType == SoundType.WOOD || this.soundType == SoundType.LADDER) {
            return Material.WOOD;
        }
        if (this.soundType == SoundType.PLANT) {
            return this.hasCollision ? Material.GOURD : Material.PLANTS;
        }
        if (this.soundType == SoundType.SAND) {
            return Material.SAND;
        }
        if (this.soundType == SoundType.CLOTH) {
            return Material.CLOTH;
        }
        return Material.GROUND;
    }

    public MapColor mapColor() {
        return this.mapColor != null ? this.mapColor : this.material().getMaterialMapColor();
    }

    /**
     * Sets hardness, resistance, light and random ticks; the sound type through the block's protected setter.
     */
    public void applyTo(Block block, Consumer<SoundType> soundTypeSetter) {
        block.setDefaultSlipperiness(this.friction);
        block.setHardness(this.destroyTime);
        block.setResistance(this.explosionResistance * 5.0F / 3.0F);
        int light = this.lightEmission != null ? this.lightEmission.applyAsInt(block.getDefaultState()) : this.lightLevel;
        block.setLightLevel(light / 15.0F);
        block.setTickRandomly(this.randomTicks);
        soundTypeSetter.accept(this.soundType);
    }

    public SoundType soundType() {
        return this.soundType;
    }

    public boolean needsCorrectToolForDrops() {
        return this.requiresCorrectToolForDrops;
    }

    public boolean hasCollision() {
        return this.hasCollision;
    }

    public boolean canOcclude() {
        return this.canOcclude;
    }

    public boolean hasLootTable() {
        return !this.noLootTable;
    }

    public boolean isValidSpawn(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable Object entityType) {
        return this.isValidSpawn == null || this.isValidSpawn.test(state, world, pos, entityType);
    }

    @Nullable
    public Boolean isRedstoneConductor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.isRedstoneConductor == null ? null : this.isRedstoneConductor.test(state, world, pos);
    }

    @Nullable
    public Boolean isSuffocating(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.isSuffocating == null ? null : this.isSuffocating.test(state, world, pos);
    }

    @Nullable
    public Boolean isViewBlocking(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.isViewBlocking == null ? null : this.isViewBlocking.test(state, world, pos);
    }

    @FunctionalInterface
    public interface StatePredicate {
        boolean test(IBlockState state, IBlockAccess world, BlockPos pos);
    }

    @FunctionalInterface
    public interface StateArgumentPredicate<A> {
        boolean test(IBlockState state, IBlockAccess world, BlockPos pos, A argument);
    }
}
