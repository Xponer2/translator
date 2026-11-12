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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A type of entity (1.21 {@code net.minecraft.world.entity.EntityType}): a 1.12.2 {@link EntityEntry} under its 1.21
 * id. The registry is {@link EntityTypeRegistry} ({@code BuiltInRegistries.ENTITY_TYPE}).
 * <p>
 * A 1.21.1 mod's own type is made with {@link Builder} ({@code EntityType.Builder.of(Moa::new, CREATURE).sized(..)
 * .build(..)}) and becomes a Forge entity registration when the mod registers it ({@link #register}): its factory makes
 * the entities, on the server and on the client (FML spawns them through the registration's factory).
 */
public final class EntityType<T extends Entity> {
    private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
    private static final Map<EntityEntry, EntityType<?>> TYPES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EntityType<?>> MISSING = new ConcurrentHashMap<>();
    /** The network ids of the mods' entity registrations (under the Translator's mod): the same order on both sides. */
    private static final AtomicInteger NETWORK_IDS = new AtomicInteger();

    /** The registration, null for a 1.21 vanilla type 1.12.2 has nothing like (no entity is of it), or a mod's type not registered yet. */
    @Nullable
    private volatile EntityEntry entry;
    @Nullable
    private final ResourceLocation missing;

    // a 1.21.1 mod's own type (null factory: a 1.12.2 one)
    @Nullable
    private final EntityFactory<T> factory;
    @Nullable
    private final net.minecraft.entity.EnumCreatureType category;
    private final float width;
    private final float height;
    private final boolean fireImmune;
    private final boolean summonable;
    private final int trackingRange;
    private final int updateInterval;
    private final boolean velocityUpdates;
    @Nullable
    private volatile ResourceLocation id;

    private EntityType(EntityEntry entry) {
        this.entry = entry;
        this.missing = null;
        this.factory = null;
        this.category = null;
        this.width = 0.6F;
        this.height = 1.8F;
        this.fireImmune = false;
        this.summonable = true;
        this.trackingRange = 5;
        this.updateInterval = 3;
        this.velocityUpdates = true;
    }

    private EntityType(ResourceLocation missing) {
        this.entry = null;
        this.missing = missing;
        this.factory = null;
        this.category = null;
        this.width = 0.6F;
        this.height = 1.8F;
        this.fireImmune = false;
        this.summonable = false;
        this.trackingRange = 5;
        this.updateInterval = 3;
        this.velocityUpdates = true;
    }

    private EntityType(Builder<T> builder) {
        this.entry = null;
        this.missing = null;
        this.factory = builder.factory;
        this.category = builder.category;
        this.width = builder.width;
        this.height = builder.height;
        this.fireImmune = builder.fireImmune;
        this.summonable = builder.summonable;
        this.trackingRange = builder.trackingRange;
        this.updateInterval = builder.updateInterval;
        this.velocityUpdates = builder.velocityUpdates;
    }

    /** 1.21 {@code EntityType.EntityFactory}: what makes an entity of a mod's type ({@code Moa::new}). */
    @FunctionalInterface
    public interface EntityFactory<T extends Entity> {
        T create(EntityType<T> type, World level);
    }

    /** 1.21 {@code EntityType.Builder}: a mod's entity type, its size, tracking and what it is immune to. */
    public static final class Builder<T extends Entity> {
        @Nullable
        private final EntityFactory<T> factory;
        @Nullable
        private final net.minecraft.entity.EnumCreatureType category;
        private float width = 0.6F;
        private float height = 1.8F;
        private boolean fireImmune;
        private boolean summonable = true;
        private int trackingRange = 5;
        private int updateInterval = 3;
        private boolean velocityUpdates = true;

        private Builder(@Nullable EntityFactory<T> factory, @Nullable net.minecraft.entity.EnumCreatureType category) {
            this.factory = factory;
            this.category = category;
        }

        public static <T extends Entity> Builder<T> of(EntityFactory<T> factory, @Nullable net.minecraft.entity.EnumCreatureType category) {
            return new Builder<>(factory, category);
        }

        /** A type no entity is made of by itself (a mod makes them with its own code). */
        public static <T extends Entity> Builder<T> createNothing(@Nullable net.minecraft.entity.EnumCreatureType category) {
            return new Builder<>(null, category);
        }

        public Builder<T> sized(float width, float height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder<T> fireImmune() {
            this.fireImmune = true;
            return this;
        }

        public Builder<T> noSummon() {
            this.summonable = false;
            return this;
        }

        public Builder<T> clientTrackingRange(int chunks) {
            this.trackingRange = chunks;
            return this;
        }

        public Builder<T> updateInterval(int ticks) {
            this.updateInterval = ticks;
            return this;
        }

        /** NeoForge {@code setTrackingRange}/{@code setUpdateInterval}/{@code setShouldReceiveVelocityUpdates}. */
        public Builder<T> setTrackingRange(int chunks) {
            return this.clientTrackingRange(chunks);
        }

        public Builder<T> setUpdateInterval(int ticks) {
            return this.updateInterval(ticks);
        }

        public Builder<T> setShouldReceiveVelocityUpdates(boolean velocityUpdates) {
            this.velocityUpdates = velocityUpdates;
            return this;
        }

        // what 1.12.2 has no place for: kept as the 1.21 builder's answer, nothing more
        public Builder<T> noSave() {
            return this;
        }

        public Builder<T> canSpawnFarFromPlayer() {
            return this;
        }

        public Builder<T> immuneTo(Object... blocks) {
            return this;
        }

        public Builder<T> eyeHeight(float height) {
            return this;
        }

        public Builder<T> spawnDimensionsScale(float scale) {
            return this;
        }

        public Builder<T> ridingOffset(float offset) {
            return this;
        }

        public Builder<T> nameTagOffset(float offset) {
            return this;
        }

        public Builder<T> passengerAttachments(float... offsets) {
            return this;
        }

        public Builder<T> passengerAttachments(Object... offsets) {
            return this;
        }

        public Builder<T> vehicleAttachment(Object offset) {
            return this;
        }

        public Builder<T> attach(Object attachment, float x, float y, float z) {
            return this;
        }

        public Builder<T> attach(Object attachment, Object offset) {
            return this;
        }

        public Builder<T> requiredFeatures(Object... features) {
            return this;
        }

        public Builder<T> setCustomClientFactory(Object factory) {
            return this;
        }

        public EntityType<T> build(String id) {
            return new EntityType<>(this);
        }
    }

    /**
     * A mod's type as a Forge entity registration under its id ({@link DeferredRegister} and {@link RegisterEvent} on
     * the entity registry event): null, logged, when it can't be one (no factory, or its entity class can't be told).
     */
    @Nullable
    static EntityEntry register(ResourceLocation id, EntityType<?> type) {
        if (type.entry != null) {
            return type.entry;
        }
        if (type.factory == null) {
            // a type made with createNothing: the mod spawns its entities itself, there is nothing to register
            type.id = id;
            MISSING.putIfAbsent(id, type);
            return null;
        }
        Class<? extends Entity> entityClass = entityClass(type);
        if (entityClass == null) {
            LOGGER.warn("Entity type {}: its entity class can't be told (its factory makes none without a world), not registered", id);
            return null;
        }
        var loader = net.minecraftforge.fml.common.Loader.instance();
        var previous = loader.activeModContainer();
        // the registration's mod is the active one: the Translator's (the 1.21.1 mods have no FML container)
        loader.setActiveModContainer(loader.getIndexedModList().get(io.github.xponer2.translator.Translator.MOD_ID));
        EntityEntry entry;
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            EntityType<Entity> self = (EntityType) type;
            entry = net.minecraftforge.fml.common.registry.EntityEntryBuilder.create()
                    .entity(entityClass)
                    .factory(world -> make(self, world))
                    .id(id, NETWORK_IDS.getAndIncrement())
                    .name(id.getNamespace() + "." + id.getPath())
                    .tracker(Math.max(1, type.trackingRange) * 16, Math.max(1, type.updateInterval), type.velocityUpdates)
                    .build();
        } finally {
            loader.setActiveModContainer(previous);
        }
        type.entry = entry;
        type.id = id;
        TYPES.put(entry, type);
        return entry;
    }

    /**
     * A mod's entity made by its 1.21 factory. One whose constructor fails (a linkage error is no Exception: the
     * natural spawning of 1.12.2 let it through and the server crashed — Iron's Spells' necromancer) is logged once and
     * taken out of the natural spawning of every biome; the call gets an exception the spawner and the commands catch.
     */
    private static Entity make(EntityType<Entity> type, net.minecraft.world.World world) {
        try {
            return type.factory.create(type, world);
        } catch (RuntimeException | LinkageError e) {
            if (BROKEN.add(type)) {
                io.github.xponer2.translator.runtime.RuntimeStubs.failed("making entity " + type.id, e);
                Class<? extends Entity> entityClass = type.entry == null ? null : type.entry.getEntityClass();
                if (entityClass != null) {
                    for (net.minecraft.world.biome.Biome biome : net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES) {
                        for (net.minecraft.entity.EnumCreatureType kind : net.minecraft.entity.EnumCreatureType.values()) {
                            biome.getSpawnableList(kind).removeIf(spawn -> spawn.entityClass == entityClass);
                        }
                    }
                }
            }
            throw new IllegalStateException("Entity " + type.id + " can't be made: " + e, e);
        }
    }

    private static final java.util.Set<EntityType<?>> BROKEN = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /**
     * The class of a mod type's entities: the entity class the factory names ({@code Moa::new} is a class whose constant
     * pool holds Moa), else that of an entity made without a world.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private static Class<? extends Entity> entityClass(EntityType<?> type) {
        Class<? extends Entity> named = namedBy(type.factory);
        if (named != null) {
            return named;
        }
        try {
            @SuppressWarnings({"rawtypes"})
            Entity sample = ((EntityFactory) type.factory).create(type, null);
            return sample == null ? null : sample.getClass();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** The one entity class in the constant pool of the factory's class (a method reference to a constructor), else null. */
    @Nullable
    @SuppressWarnings("unchecked")
    private static Class<? extends Entity> namedBy(Object factory) {
        try {
            Object access = Class.forName("sun.misc.SharedSecrets").getMethod("getJavaLangAccess").invoke(null);
            Object pool = Class.forName("sun.misc.JavaLangAccess").getMethod("getConstantPool", Class.class).invoke(access, factory.getClass());
            Class<?> poolClass = Class.forName("sun.reflect.ConstantPool");
            int size = (Integer) poolClass.getMethod("getSize").invoke(pool);
            java.lang.reflect.Method classAt = poolClass.getMethod("getClassAtIfLoaded", int.class);
            Class<? extends Entity> found = null;
            for (int i = 1; i < size; i++) {
                Object value;
                try {
                    value = classAt.invoke(pool, i);
                } catch (java.lang.reflect.InvocationTargetException notAClass) {
                    continue;
                }
                if (value instanceof Class<?> candidate && Entity.class.isAssignableFrom(candidate)
                        && !java.lang.reflect.Modifier.isAbstract(candidate.getModifiers())) {
                    if (found != null && found != candidate) {
                        return null;
                    }
                    found = (Class<? extends Entity>) candidate;
                }
            }
            return found;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /**
     * A 1.21 vanilla type by its field name ({@code EntityType.ZOMBIE}): the 1.12.2 entity of that id or a similar one,
     * else a type of its own that no entity has.
     */
    public static EntityType<?> vanilla(String field) {
        var id = new ResourceLocation("minecraft", field.toLowerCase(java.util.Locale.ROOT));
        EntityType<?> type = null;
        try {
            type = io.github.xponer2.translator.util.BuiltInRegistries.ENTITY_TYPE.get(id);
        } catch (RuntimeException e) {
            // the registry isn't there yet
        }
        return type != null ? type : MISSING.computeIfAbsent(id, EntityType::new);
    }

    /**
     * The type of a 1.12.2 entity registration.
     */
    public static EntityType<?> of(EntityEntry entry) {
        return TYPES.computeIfAbsent(entry, EntityType::new);
    }

    /**
     * The type of an entity, null for entities without a registration (players, ...).
     */
    @Nullable
    public static EntityType<?> of(Entity entity) {
        var entry = EntityRegistry.getEntry(entity.getClass());
        return entry == null ? null : of(entry);
    }

    @Nullable
    public static ResourceLocation getKey(EntityType<?> type) {
        if (type.factory != null || type.id != null) {
            return type.id;
        }
        return type.entry == null ? type.missing : EntityTypeRegistry.toModern(type.entry.getRegistryName());
    }

    public EntityEntry entry() {
        return this.entry;
    }

    /** 1.21 {@code getDefaultLootTable()}: {@code <namespace>:entities/<path>}, the loot table 1.21 gives the mob. */
    public ResourceKey<net.minecraft.world.storage.loot.LootTable> getDefaultLootTable() {
        var id = getKey(this);
        return ResourceKey.create(io.github.xponer2.translator.runtime.redirect.LootBridges.LOOT_TABLE, id == null
                ? new ResourceLocation("minecraft", "empty") : new ResourceLocation(id.getNamespace(), "entities/" + id.getPath()));
    }

    @SuppressWarnings("unchecked")
    public Class<? extends T> getEntityClass() {
        return this.entry == null ? (Class<? extends T>) Entity.class : (Class<? extends T>) this.entry.getEntityClass();
    }

    /**
     * A new entity of this type in a world (not spawned yet), null if it can't be created.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public T create(World level) {
        if (this.factory != null) {
            return this.factory.create(this, level);
        }
        return this.entry == null ? null : (T) this.entry.newInstance(level);
    }

    public String getDescriptionId() {
        var id = this.factory != null || this.id != null ? this.id : null;
        if (id != null) {
            // the 1.21 key, which the mod's language files name
            return "entity." + id.getNamespace() + "." + id.getPath();
        }
        if (this.entry == null) {
            return this.missing == null ? "entity.unknown" : "entity." + this.missing.getNamespace() + "." + this.missing.getPath();
        }
        return "entity." + this.entry.getName() + ".name";
    }

    /** 1.21 {@code getWidth()}/{@code getHeight()}: the size its builder gave (a 1.12.2 entity sets its own). */
    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    /**
     * 1.21 {@code getSpawnAABB(x, y, z)}: the box an entity of the type takes standing at that point (its size). Twilight
     * Forest's spawners check the room for a mob with it.
     */
    public net.minecraft.util.math.AxisAlignedBB getSpawnAABB(double x, double y, double z) {
        double half = this.width / 2.0;
        return new net.minecraft.util.math.AxisAlignedBB(x - half, y, z - half, x + half, y + this.height, z + half);
    }

    public ITextComponent getDescription() {
        return new TextComponentTranslation(this.getDescriptionId());
    }

    public boolean is(EntityType<?> type) {
        return this == type;
    }

    /** In an entity type tag of the mods' data (data/<ns>/tags/entity_type). */
    public boolean is(io.github.xponer2.translator.util.TagKey<?> tag) {
        var id = getKey(this);
        return id != null && io.github.xponer2.translator.tags.TagManager.ids("entity_type", tag.location()).contains(id);
    }

    /** What kind of creature it is (spawning caps): the mod type's category, else the 1.12.2 class's kind. */
    public net.minecraft.entity.EnumCreatureType getCategory() {
        if (this.category != null) {
            return this.category;
        }
        var type = this.getEntityClass();
        for (var category : net.minecraft.entity.EnumCreatureType.values()) {
            if (category.getCreatureClass().isAssignableFrom(type)) {
                return category;
            }
        }
        return net.minecraft.entity.EnumCreatureType.MONSTER;
    }

    public Registry.Reference<EntityType<?>> builtInRegistryHolder() {
        return io.github.xponer2.translator.util.BuiltInRegistries.ENTITY_TYPE.createIntrusiveHolder(this);
    }

    public boolean canSummon() {
        return this.entry != null && this.summonable;
    }

    public boolean fireImmune() {
        return this.fireImmune;
    }

    /** A new entity of this type at a position, spawned in the world (1.21 spawn eggs, summoning). */
    @Nullable
    public T spawn(net.minecraft.world.WorldServer level, @Nullable net.minecraft.item.ItemStack stack, @Nullable net.minecraft.entity.player.EntityPlayer player,
            net.minecraft.util.math.BlockPos pos, Object reason, boolean alignPosition, boolean invertY) {
        return this.spawn(level, pos, reason);
    }

    @Nullable
    public T spawn(net.minecraft.world.WorldServer level, net.minecraft.util.math.BlockPos pos, Object reason) {
        T entity = this.create(level);
        if (entity == null) {
            return null;
        }
        entity.setLocationAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.rand.nextFloat() * 360.0F, 0.0F);
        if (entity instanceof net.minecraft.entity.EntityLiving living) {
            living.onInitialSpawn(level.getDifficultyForLocation(pos), null);
        }
        level.spawnEntity(entity);
        return entity;
    }

    /**
     * 1.21 {@code create(level, configure, pos, reason, alignPosition, invertY)}: a new entity there, a mob set up for its
     * spawn reason (its finalizeSpawn), then the mod's configure; not in the world yet (Supplementaries' hat stands
     * and piñatas, placed by their items).
     */
    @Nullable
    public T create(net.minecraft.world.WorldServer level, @Nullable java.util.function.Consumer<T> configure, net.minecraft.util.math.BlockPos pos,
            @Nullable io.github.xponer2.translator.entity.MobSpawnType reason, boolean alignPosition, boolean invertY) {
        T entity = this.create(level);
        if (entity == null) {
            return null;
        }
        entity.setLocationAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, net.minecraft.util.math.MathHelper.wrapDegrees(level.rand.nextFloat() * 360.0F), 0.0F);
        if (entity instanceof net.minecraft.entity.EntityLiving living) {
            living.rotationYawHead = living.rotationYaw;
            living.renderYawOffset = living.rotationYaw;
            net.minecraft.world.DifficultyInstance difficulty = level.getDifficultyForLocation(pos);
            if (living instanceof io.github.xponer2.translator.entity.SpawnFinalizing finalizing) {
                finalizing.finalizeSpawn(level, difficulty, reason == null ? io.github.xponer2.translator.entity.MobSpawnType.NATURAL : reason, null);
            } else {
                living.onInitialSpawn(difficulty, null);
            }
        }
        if (configure != null) {
            configure.accept(entity);
        }
        return entity;
    }

    /** 1.21 {@code spawn(level, configure, pos, reason, alignPosition, invertY)}: made as {@link #create} and added to the world (the Aether's Moa eggs). */
    @Nullable
    public T spawn(net.minecraft.world.WorldServer level, @Nullable java.util.function.Consumer<T> configure, net.minecraft.util.math.BlockPos pos,
            @Nullable io.github.xponer2.translator.entity.MobSpawnType reason, boolean alignPosition, boolean invertY) {
        T entity = this.create(level, configure, pos, reason, alignPosition, invertY);
        if (entity != null) {
            level.spawnEntity(entity);
        }
        return entity;
    }

    /** 1.21 {@code spawn(level, stack, player, pos, reason, alignPosition, invertY)} (spawn eggs). */
    @Nullable
    public T spawn(net.minecraft.world.WorldServer level, @Nullable net.minecraft.item.ItemStack stack, @Nullable net.minecraft.entity.player.EntityPlayer player,
            net.minecraft.util.math.BlockPos pos, @Nullable io.github.xponer2.translator.entity.MobSpawnType reason, boolean alignPosition, boolean invertY) {
        return this.spawn(level, (java.util.function.Consumer<T>) null, pos, reason, alignPosition, invertY);
    }

    /** 1.21 {@code spawn(level, pos, reason)} (the Aether's incubator). */
    @Nullable
    public T spawn(net.minecraft.world.WorldServer level, net.minecraft.util.math.BlockPos pos, @Nullable io.github.xponer2.translator.entity.MobSpawnType reason) {
        return this.spawn(level, (java.util.function.Consumer<T>) null, pos, reason, false, false);
    }

    @Override
    public String toString() {
        var id = getKey(this);
        return id != null ? id.toString() : String.valueOf(this.entry == null ? this.missing : this.entry.getRegistryName());
    }

    /**
     * The 1.12.2 id of an entity (for code that needs it, e.g. spawn eggs).
     */
    @Nullable
    public static ResourceLocation legacyKey(Entity entity) {
        return EntityList.getKey(entity);
    }
}
