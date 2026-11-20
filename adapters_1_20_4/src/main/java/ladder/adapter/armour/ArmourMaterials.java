/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.armour;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ladder.carried.net.minecraft.world.item.ArmorMaterial;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The game's own armour materials as 1.20.5 has them: records registered under minecraft: in the armour material
 * registry, made of 1.20.4's ArmorMaterials (the same numbers) with 1.20.5's body armour defense and texture layers;
 * {@code ArmorMaterials.IRON} and the others are their holders (the same object every time, as 1.21's). Armour items
 * here: their 1.20.5 material ({@code getMaterial()}) is the record's holder.
 */
public final class ArmourMaterials {
    private static final Map<String, Holder<ArmorMaterial>> HOLDERS = new HashMap<>();
    /** the records of 1.20.4 mods' materials, one holder per material */
    private static final Map<net.minecraft.world.item.ArmorMaterial, Holder<ArmorMaterial>> OTHERS = new java.util.IdentityHashMap<>();

    private ArmourMaterials() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(RegisterEvent.class, event -> event.register(ArmourAdapters.KEY, helper -> {
            helper.register(id("leather"), of(ArmorMaterials.LEATHER, 3, List.of(layer("leather", "", true), layer("leather", "_overlay", false))));
            helper.register(id("chainmail"), of(ArmorMaterials.CHAIN, 4, List.of(layer("chainmail"))));
            helper.register(id("iron"), of(ArmorMaterials.IRON, 5, List.of(layer("iron"))));
            helper.register(id("gold"), of(ArmorMaterials.GOLD, 7, List.of(layer("gold"))));
            helper.register(id("diamond"), of(ArmorMaterials.DIAMOND, 11, List.of(layer("diamond"))));
            helper.register(id("turtle"), of(ArmorMaterials.TURTLE, 5, List.of(layer("turtle"))));
            helper.register(id("netherite"), of(ArmorMaterials.NETHERITE, 11, List.of(layer("netherite"))));
            // 1.20.5's wolf armour material: 1.20.4 has neither the scute nor the sound
            Map<ArmorItem.Type, Integer> armadillo = defense(3, 6, 8, 3, 11);
            helper.register(id("armadillo"), new ArmorMaterial(armadillo, 10, sound(SoundEvents.ARMOR_EQUIP_GENERIC), () -> Ingredient.EMPTY,
                    List.of(layer("armadillo")), 0.0F, 0.0F));
        }));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation("minecraft", name);
    }

    private static ArmorMaterial.Layer layer(String name) {
        return new ArmorMaterial.Layer(id(name));
    }

    private static ArmorMaterial.Layer layer(String name, String suffix, boolean dyeable) {
        return new ArmorMaterial.Layer(id(name), suffix, dyeable);
    }

    private static Holder<SoundEvent> sound(SoundEvent sound) {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
    }

    private static Map<ArmorItem.Type, Integer> defense(int boots, int leggings, int chestplate, int helmet, int body) {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmourAdapters.bodyType(), body);
        return defense;
    }

    /** a 1.20.4 material as the 1.20.5 record: its numbers, the given body defense and layers */
    private static ArmorMaterial of(net.minecraft.world.item.ArmorMaterial old, int body, List<ArmorMaterial.Layer> layers) {
        Map<ArmorItem.Type, Integer> defense = defense(old.getDefenseForType(ArmorItem.Type.BOOTS), old.getDefenseForType(ArmorItem.Type.LEGGINGS),
                old.getDefenseForType(ArmorItem.Type.CHESTPLATE), old.getDefenseForType(ArmorItem.Type.HELMET), body);
        return new ArmorMaterial(defense, old.getEnchantmentValue(), sound(old.getEquipSound()), old::getRepairIngredient, layers, old.getToughness(),
                old.getKnockbackResistance());
    }

    /** a 1.20.4 material no record was made for (a 1.20.4 mod's): the same numbers, its name as the texture's */
    static ArmorMaterial record(net.minecraft.world.item.ArmorMaterial old) {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            if (type != ArmourAdapters.bodyType()) {
                defense.put(type, old.getDefenseForType(type));
            }
        }
        String name = old.getName();
        ResourceLocation asset = name.contains(":") ? new ResourceLocation(name) : id(name);
        return new ArmorMaterial(defense, old.getEnchantmentValue(), sound(old.getEquipSound()), old::getRepairIngredient,
                List.of(new ArmorMaterial.Layer(asset)), old.getToughness(), old.getKnockbackResistance());
    }

    /** the holder of the game's material of that name (1.20.5's {@code ArmorMaterials.X}): one object per name */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized Holder<ArmorMaterial> holder(String name) {
        // once registered, the registry's own reference (as 1.21's ArmorMaterials.X are): equal to any holder of it
        // the registry or data gives; before that a holder bound to it when it is registered
        Holder<ArmorMaterial> early = HOLDERS.computeIfAbsent(name, n -> DeferredHolder.create(ArmourAdapters.typedKey(), id(n)));
        if (early instanceof DeferredHolder<?, ?> deferred && deferred.isBound()) {
            java.util.Optional<?> reference = ArmourAdapters.registry().getHolder(
                    net.minecraft.resources.ResourceKey.create((net.minecraft.resources.ResourceKey) ArmourAdapters.KEY, id(name)));
            if (reference.isPresent()) {
                return (Holder<ArmorMaterial>) reference.get();
            }
        }
        return early;
    }

    /** the holder of a 1.20.4 ArmorMaterials constant (its 1.20.5 name) */
    static Holder<ArmorMaterial> holder(ArmorMaterials vanilla) {
        return holder(switch (vanilla) {
            case LEATHER -> "leather";
            case CHAIN -> "chainmail";
            case IRON -> "iron";
            case GOLD -> "gold";
            case DIAMOND -> "diamond";
            case TURTLE -> "turtle";
            case NETHERITE -> "netherite";
        });
    }

    public static Holder<ArmorMaterial> leather() {
        return holder("leather");
    }

    public static Holder<ArmorMaterial> chain() {
        return holder("chainmail");
    }

    public static Holder<ArmorMaterial> iron() {
        return holder("iron");
    }

    public static Holder<ArmorMaterial> gold() {
        return holder("gold");
    }

    public static Holder<ArmorMaterial> diamond() {
        return holder("diamond");
    }

    public static Holder<ArmorMaterial> turtle() {
        return holder("turtle");
    }

    public static Holder<ArmorMaterial> netherite() {
        return holder("netherite");
    }

    public static Holder<ArmorMaterial> armadillo() {
        return holder("armadillo");
    }

    private static final java.lang.reflect.Field ASSET_NAME;

    static {
        try {
            ASSET_NAME = ArmorMaterial.Layer.class.getDeclaredField("assetName");
            ASSET_NAME.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** a layer's asset name (1.21 keeps it private: its textures are made of it) */
    static ResourceLocation assetName(ArmorMaterial.Layer layer) {
        try {
            return (ResourceLocation) ASSET_NAME.get(layer);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- armour items

    /**
     * 1.20.5's {@code new ArmorItem(material holder, type, properties)} (and a subclass's super call, a K row): 1.20.4's
     * constructor takes a material, made of the holder.
     */
    public static net.minecraft.world.item.ArmorMaterial material(Holder<ArmorMaterial> holder, ArmorItem.Type type, Item.Properties properties) {
        return new RecordMaterial(holder);
    }

    /** 1.20.5's {@code ArmorItem.getMaterial()}: the material's holder */
    public static Holder<ArmorMaterial> getMaterial(ArmorItem item) {
        net.minecraft.world.item.ArmorMaterial material = item.getMaterial();
        if (material instanceof RecordMaterial own) {
            return own.holder;
        }
        if (material instanceof ArmorMaterials vanilla) {
            return holder(vanilla);
        }
        synchronized (OTHERS) {
            return OTHERS.computeIfAbsent(material, m -> Holder.direct(record(m)));
        }
    }
}
