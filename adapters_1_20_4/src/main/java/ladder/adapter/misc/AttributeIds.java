/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.misc;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * 1.21 names an attribute modifier by a ResourceLocation where 1.20.4 has a UUID and a name. A 1.21 id is the UUID made
 * of its text (the vanilla ids are the vanilla UUIDs: the base attack damage and speed, the armour per slot), and the
 * name is the id's text; a 1.20.4 modifier's id is the vanilla id of its UUID, else the id its name gives when the name
 * makes that UUID, else (as 1.21's own data fix names old modifiers) the UUID under the minecraft namespace.
 */
public final class AttributeIds {
    private static final Map<UUID, ResourceLocation> KNOWN_IDS = new HashMap<>();
    private static final Map<ResourceLocation, UUID> KNOWN_UUIDS = new HashMap<>();
    /** the ids made into UUIDs so far: their modifiers give them back whatever name 1.20.4 gave them */
    private static final Map<UUID, ResourceLocation> SEEN = new ConcurrentHashMap<>();

    static {
        known(uuidField(Item.class, "BASE_ATTACK_DAMAGE_UUID"), "base_attack_damage");
        known(uuidField(Item.class, "BASE_ATTACK_SPEED_UUID"), "base_attack_speed");
        try {
            Field field = ArmorItem.class.getDeclaredField("ARMOR_MODIFIER_UUID_PER_TYPE");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            EnumMap<ArmorItem.Type, UUID> perType = (EnumMap<ArmorItem.Type, UUID>) field.get(null);
            perType.forEach((type, uuid) -> known(uuid, "armor." + type.getName()));
        } catch (ReflectiveOperationException | RuntimeException e) {
            // no armour ids known: they are made of their text like any other
        }
        known(uuidField(net.minecraft.world.entity.LivingEntity.class, "SPEED_MODIFIER_SPRINTING_UUID"), "sprinting");
        // an effect's modifiers: minecraft:effect.<effect> (as 1.21's data fix names them)
        try {
            for (var entry : net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.entrySet()) {
                Field field = net.minecraft.world.effect.MobEffect.class.getDeclaredField("attributeModifiers");
                field.setAccessible(true);
                for (Object template : ((java.util.Map<?, ?>) field.get(entry.getValue())).values()) {
                    UUID uuid = templateUuid(template);
                    if (uuid != null && !KNOWN_IDS.containsKey(uuid)) {
                        known(uuid, "effect." + entry.getKey().location().getPath());
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // no effect ids known
        }
    }

    private AttributeIds() {
    }

    /** an effect's attribute template's UUID (a record's id, or a modifier's) */
    @Nullable
    private static UUID templateUuid(Object template) {
        if (template instanceof AttributeModifier modifier) {
            return idOf(modifier);
        }
        for (Field f : template.getClass().getDeclaredFields()) {
            if (f.getType() == UUID.class) {
                try {
                    f.setAccessible(true);
                    return (UUID) f.get(template);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    return null;
                }
            }
        }
        return null;
    }

    @Nullable
    private static UUID uuidField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return (UUID) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static void known(@Nullable UUID uuid, String path) {
        if (uuid != null) {
            ResourceLocation id = new ResourceLocation("minecraft", path);
            KNOWN_IDS.put(uuid, id);
            KNOWN_UUIDS.put(id, uuid);
        }
    }

    /** a 1.21 modifier id -> its 1.20.4 UUID */
    public static UUID uuid(ResourceLocation id) {
        UUID known = KNOWN_UUIDS.get(id);
        if (known != null) {
            return known;
        }
        if (id.getNamespace().equals("minecraft")) {
            // a lower modifier's own UUID named as 1.21's data fix names it (minecraft:<uuid>): that UUID
            try {
                return UUID.fromString(id.getPath());
            } catch (IllegalArgumentException e) {
                // a name, not a UUID
            }
        }
        UUID made = UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
        SEEN.putIfAbsent(made, id);
        return made;
    }

    /** a 1.20.4 modifier's UUID and name -> its 1.21 id */
    public static ResourceLocation id(UUID uuid, @Nullable String name) {
        ResourceLocation known = KNOWN_IDS.get(uuid);
        if (known != null) {
            return known;
        }
        ResourceLocation seen = SEEN.get(uuid);
        if (seen != null) {
            return seen;
        }
        ResourceLocation named = name == null ? null : ResourceLocation.tryParse(name);
        if (named != null && uuid(named).equals(uuid)) {
            return named;
        }
        return new ResourceLocation("minecraft", uuid.toString());
    }

    /** 1.21's {@code new AttributeModifier(id, amount, operation)} */
    public static AttributeModifier create(ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(uuid(id), id.toString(), amount, operation);
    }

    /** 1.21's {@code AttributeModifier.id()} */
    public static ResourceLocation id(AttributeModifier modifier) {
        return id(modifier.getId(), modifier.save().getString("Name"));
    }

    /** 1.21's {@code AttributeModifier.is(id)} */
    public static boolean is(AttributeModifier modifier, ResourceLocation id) {
        return modifier.getId().equals(uuid(id));
    }

    /** 1.21's {@code AttributeInstance.getModifier(id)} */
    @Nullable
    public static AttributeModifier getModifier(AttributeInstance instance, ResourceLocation id) {
        return instance.getModifier(uuid(id));
    }

    /** 1.21's {@code AttributeInstance.hasModifier(id)} */
    public static boolean hasModifier(AttributeInstance instance, ResourceLocation id) {
        return instance.getModifier(uuid(id)) != null;
    }

    /** 1.21's {@code AttributeInstance.removeModifier(id)}: whether there was one */
    public static boolean removeModifier(AttributeInstance instance, ResourceLocation id) {
        UUID uuid = uuid(id);
        boolean had = instance.getModifier(uuid) != null;
        instance.removeModifier(uuid);
        return had;
    }

    /** 1.21's {@code AttributeInstance.addOrReplacePermanentModifier(modifier)} */
    public static void addOrReplacePermanentModifier(AttributeInstance instance, AttributeModifier modifier) {
        instance.removeModifier(modifier.getId());
        instance.addPermanentModifier(modifier);
    }

    /** 1.21's {@code MobEffect.addAttributeModifier(attribute, id, amount, operation)} */
    public static MobEffect addAttributeModifier(MobEffect effect, Holder<Attribute> attribute, ResourceLocation id, double amount,
            AttributeModifier.Operation operation) {
        return effect.addAttributeModifier(attribute.value(), uuid(id).toString(), amount, operation);
    }

    /** 1.21's {@code Item.BASE_ATTACK_DAMAGE_ID} */
    public static ResourceLocation baseAttackDamageId() {
        return new ResourceLocation("minecraft", "base_attack_damage");
    }

    /** 1.21's {@code Item.BASE_ATTACK_SPEED_ID} */
    public static ResourceLocation baseAttackSpeedId() {
        return new ResourceLocation("minecraft", "base_attack_speed");
    }

    private static UUID idOf(AttributeModifier modifier) {
        return modifier.getId();
    }
}
