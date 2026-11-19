/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Armour of a 1.20.5 material for the client check ({@link ArmourClient}): a dyeable layer and a suffixed one, the inner
 * model's for leggings, an item's own texture hook.
 */
final class Armour {
    static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, LadderTestC.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LadderTestC.MODID);

    static final DeferredHolder<ArmorMaterial, ArmorMaterial> PLATE = MATERIALS.register("plate", () -> new ArmorMaterial(
            net.minecraft.Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.BOOTS, 1);
                m.put(ArmorItem.Type.LEGGINGS, 2);
                m.put(ArmorItem.Type.CHESTPLATE, 3);
                m.put(ArmorItem.Type.HELMET, 1);
                m.put(ArmorItem.Type.BODY, 3);
            }), 9, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(LadderTestC.MODID, "plate"), "", true),
                    new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(LadderTestC.MODID, "plate"), "_overlay", false)),
            0.0F, 0.0F));
    static final DeferredHolder<Item, ArmorItem> HELMET = ITEMS.register("plate_helmet",
            () -> new ArmorItem(PLATE, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(5))));
    static final DeferredHolder<Item, ArmorItem> LEGGINGS = ITEMS.register("plate_leggings",
            () -> new ArmorItem(PLATE, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(5))));
    static final DeferredHolder<Item, ArmorItem> BOOTS = ITEMS.register("plate_boots", () -> new OwnTextureBoots(new Item.Properties()));

    /** boots with a texture of their own for the first layer (NeoForge's armour texture hook) */
    static final class OwnTextureBoots extends ArmorItem {
        OwnTextureBoots(Item.Properties properties) {
            super(PLATE, ArmorItem.Type.BOOTS, properties);
        }

        @Override
        public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
            return layer.dyeable() ? ResourceLocation.fromNamespaceAndPath(LadderTestC.MODID, "textures/models/armor/own_boots.png") : null;
        }
    }

    private Armour() {
    }

    static void register(IEventBus modBus) {
        MATERIALS.register(modBus);
        ITEMS.register(modBus);
    }

}
