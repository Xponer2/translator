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

    private final EntityEntry entry;

    private EntityType(EntityEntry entry) {
        this.entry = entry;
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
        return EntityTypeRegistry.toModern(type.entry.getRegistryName());
    }

    public EntityEntry entry() {
        return this.entry;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends T> getEntityClass() {
        return (Class<? extends T>) this.entry.getEntityClass();
    }

    /**
     * A new entity of this type in a world (not spawned yet), null if it can't be created.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public T create(World level) {
        return (T) this.entry.newInstance(level);
    }

    public String getDescriptionId() {
        return "entity." + this.entry.getName() + ".name";
    }

    public ITextComponent getDescription() {
        return new TextComponentTranslation(this.getDescriptionId());
    }

    public boolean is(EntityType<?> type) {
        return this == type;
    }

    @Override
    public String toString() {
        var id = getKey(this);
        return id != null ? id.toString() : String.valueOf(this.entry.getRegistryName());
    }

    /**
     * The 1.12.2 id of an entity (for code that needs it, e.g. spawn eggs).
     */
    @Nullable
    public static ResourceLocation legacyKey(Entity entity) {
        return EntityList.getKey(entity);
    }
}
