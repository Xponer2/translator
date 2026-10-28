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
    private Material material;
    private MapColor mapColor;
    private SoundType soundType = SoundType.STONE;
    private float destroyTime;
    private float explosionResistance;
    private int lightLevel;
    private boolean requiresCorrectToolForDrops;
    private boolean randomTicks;
    private boolean hasCollision = true;
    private boolean canOcclude = true;
    private boolean noLootTable;
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

    public BlockProperties material(Material material) {
        this.material = material;
        return this;
    }

    public BlockProperties mapColor(MapColor mapColor) {
        this.mapColor = mapColor;
        return this;
    }

    public BlockProperties sound(SoundType soundType) {
        this.soundType = soundType;
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
        block.setHardness(this.destroyTime);
        block.setResistance(this.explosionResistance * 5.0F / 3.0F);
        block.setLightLevel(this.lightLevel / 15.0F);
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
