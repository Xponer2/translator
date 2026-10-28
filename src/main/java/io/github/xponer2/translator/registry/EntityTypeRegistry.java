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

import io.github.xponer2.translator.util.Registries;
import com.google.common.collect.ImmutableMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The entity types by 1.21 id, backed by the 1.12.2 Forge entity registry. Vanilla entities that were renamed
 * after 1.12.2 are found under both ids and keyed by the 1.21 one.
 */
public final class EntityTypeRegistry implements Registry<EntityType<?>> {
    /** 1.12.2 path -> 1.21 path of the vanilla entities whose id changed */
    private static final Map<String, String> RENAMED = ImmutableMap.<String, String>builder()
            .put("zombie_pigman", "zombified_piglin")
            .put("villager_golem", "iron_golem")
            .put("snowman", "snow_golem")
            .put("evocation_illager", "evoker")
            .put("vindication_illager", "vindicator")
            .put("illusion_illager", "illusioner")
            .put("evocation_fangs", "evoker_fangs")
            .put("ender_crystal", "end_crystal")
            .put("xp_orb", "experience_orb")
            .put("xp_bottle", "experience_bottle")
            .put("commandblock_minecart", "command_block_minecart")
            .put("fireworks_rocket", "firework_rocket")
            .put("eye_of_ender_signal", "eye_of_ender")
            .build();
    private static final Map<String, String> LEGACY = RENAMED.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    @Override
    public ResourceKey<? extends Registry<EntityType<?>>> key() {
        return Registries.ENTITY_TYPE;
    }

    @Nullable
    @Override
    public ResourceLocation getKey(EntityType<?> value) {
        return EntityType.getKey(value);
    }

    @Nullable
    @Override
    public EntityType<?> get(@Nullable ResourceLocation location) {
        if (location == null) {
            return null;
        }
        var legacy = toLegacy(location);
        if (!ForgeRegistries.ENTITIES.containsKey(legacy)) {
            // a similar 1.12.2 entity (only this way: keys stay the real ones)
            var alias = Aliases.get(Registries.ENTITY_TYPE.location(), location);
            legacy = alias != null ? toLegacy(alias) : legacy;
        }
        return ForgeRegistries.ENTITIES.containsKey(legacy) ? EntityType.of(ForgeRegistries.ENTITIES.getValue(legacy)) : null;
    }

    @Override
    public boolean containsKey(ResourceLocation location) {
        return ForgeRegistries.ENTITIES.containsKey(toLegacy(location));
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return ForgeRegistries.ENTITIES.getKeys().stream().map(EntityTypeRegistry::toModern).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>>> entrySet() {
        var entries = new LinkedHashSet<Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>>>();
        for (var entry : ForgeRegistries.ENTITIES.getEntries()) {
            entries.add(new AbstractMap.SimpleImmutableEntry<>(ResourceKey.create(Registries.ENTITY_TYPE, toModern(entry.getKey())),
                    EntityType.of(entry.getValue())));
        }
        return entries;
    }

    @Override
    public Iterator<EntityType<?>> iterator() {
        return ForgeRegistries.ENTITIES.getValuesCollection().stream().<EntityType<?>>map(EntityType::of).iterator();
    }

    @Override
    public String toString() {
        return "EntityTypeRegistry";
    }

    static ResourceLocation toModern(ResourceLocation legacy) {
        if ("minecraft".equals(legacy.getNamespace()) && RENAMED.containsKey(legacy.getPath())) {
            return new ResourceLocation("minecraft", RENAMED.get(legacy.getPath()));
        }
        return legacy;
    }

    static ResourceLocation toLegacy(ResourceLocation modern) {
        if ("minecraft".equals(modern.getNamespace()) && LEGACY.containsKey(modern.getPath())) {
            return new ResourceLocation("minecraft", LEGACY.get(modern.getPath()));
        }
        return modern;
    }
}
