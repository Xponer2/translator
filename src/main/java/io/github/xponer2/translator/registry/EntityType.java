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

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A type of entity (1.21 {@code net.minecraft.world.entity.EntityType}): a 1.12.2 {@link EntityEntry} under its 1.21
 * id. The registry is {@link EntityTypeRegistry} ({@code BuiltInRegistries.ENTITY_TYPE}).
 */
public final class EntityType<T extends Entity> {
    private static final Map<EntityEntry, EntityType<?>> TYPES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EntityType<?>> MISSING = new ConcurrentHashMap<>();

    /** The registration, null for a 1.21 vanilla type 1.12.2 has nothing like (no entity is of it). */
    @Nullable
    private final EntityEntry entry;
    @Nullable
    private final ResourceLocation missing;

    private EntityType(EntityEntry entry) {
        this.entry = entry;
        this.missing = null;
    }

    private EntityType(ResourceLocation missing) {
        this.entry = null;
        this.missing = missing;
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
        return this.entry == null ? null : (T) this.entry.newInstance(level);
    }

    public String getDescriptionId() {
        return this.entry == null ? "entity." + this.missing.getNamespace() + "." + this.missing.getPath() : "entity." + this.entry.getName() + ".name";
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

    /** What kind of creature it is (spawning caps): the 1.12.2 class's kind. */
    public net.minecraft.entity.EnumCreatureType getCategory() {
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
        return this.entry != null;
    }

    public boolean fireImmune() {
        return false;
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
