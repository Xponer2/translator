/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.armour;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * 1.20.5's armour on 1.20.4. An armour material is a record in a registry there ({@code Registries.ARMOR_MATERIAL}, held
 * by a Holder), an interface the ArmorMaterials enum implements here: the record is carried, the registry is a NeoForge
 * registry of the same name made here (before the items: an armour item reads its material when it is made), and an
 * armour item gets a 1.20.4 material made of the record ({@link RecordMaterial}). ArmorItem.Type's BODY (horse and wolf
 * armour) is a fifth constant here, worn in the chest slot as EquipmentSlot.BODY is (ArmourTypeMixin).
 */
public final class ArmourAdapters {
    public static final ResourceKey<Registry<Object>> KEY = ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "armor_material"));
    private static Registry<Object> registry;

    /** BODY, appended to ArmorItem.Type when the enum is made (ArmourTypeMixin) */
    public static ArmorItem.Type body;

    private static final TagKey<Item> HEAD_ARMOR = TagKey.create(Registries.ITEM, new ResourceLocation("head_armor"));
    private static final TagKey<Item> CHEST_ARMOR = TagKey.create(Registries.ITEM, new ResourceLocation("chest_armor"));
    private static final TagKey<Item> LEG_ARMOR = TagKey.create(Registries.ITEM, new ResourceLocation("leg_armor"));
    private static final TagKey<Item> FOOT_ARMOR = TagKey.create(Registries.ITEM, new ResourceLocation("foot_armor"));

    private ArmourAdapters() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(NewRegistryEvent.class, event -> registry = event.create(new RegistryBuilder<>(KEY).sync(true)));
        ArmourMaterials.register(modBus);
    }

    /** 1.20.5's {@code Registries.ARMOR_MATERIAL} */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ResourceKey key() {
        return KEY;
    }

    /** the registry key with the record's type (holders of the game's materials) */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static ResourceKey<Registry<ladder.carried.net.minecraft.world.item.ArmorMaterial>> typedKey() {
        return (ResourceKey) KEY;
    }

    /** 1.20.5's {@code BuiltInRegistries.ARMOR_MATERIAL} */
    @SuppressWarnings("rawtypes")
    public static Registry registry() {
        if (registry == null) {
            throw new IllegalStateException("the armour material registry is made with the game's registries; it was asked for earlier");
        }
        return registry;
    }

    /** 1.20.5's {@code ArmorItem.Type.BODY} */
    public static ArmorItem.Type bodyType() {
        if (body == null) {
            ArmorItem.Type.values();
        }
        return body;
    }

    /** 1.20.5's {@code ArmorItem.Type.getDurability(multiplier)}: the type's base durability times the multiplier */
    public static int durability(ArmorItem.Type type, int multiplier) {
        int base;
        if (type == body) {
            base = 16;
        } else {
            base = switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
                default -> 16;
            };
        }
        return base * multiplier;
    }

    /** 1.20.5's {@code SoundEvents.WOLF_ARMOR_BREAK} (wolf armour's break sound): 1.20.4 has none, an item's break sound */
    public static net.minecraft.sounds.SoundEvent wolfArmorBreak() {
        return net.minecraft.sounds.SoundEvents.ITEM_BREAK;
    }

    // 1.20.5's armour item tags (their 1.21 contents are in the adapters' data)
    public static TagKey<Item> headArmor() {
        return HEAD_ARMOR;
    }

    public static TagKey<Item> chestArmor() {
        return CHEST_ARMOR;
    }

    public static TagKey<Item> legArmor() {
        return LEG_ARMOR;
    }

    public static TagKey<Item> footArmor() {
        return FOOT_ARMOR;
    }
}
