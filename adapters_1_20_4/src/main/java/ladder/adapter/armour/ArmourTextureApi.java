/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.armour;

import ladder.carried.net.minecraft.world.item.ArmorMaterial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge 20.5's {@code getArmorTexture(stack, entity, slot, layer, innerModel)} (an item's own texture for a layer of
 * its 1.20.5 material, null for the layer's own). 1.20.4's armour layer asks a converted item that has it
 * ({@link ladder.adapter.mixin.ArmourLayerMixin}).
 */
public interface ArmourTextureApi {
    default ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return defaultTexture(null, stack, entity, slot, layer, innerModel);
    }

    /** the game's own answer (a super call): none, the layer's texture is drawn */
    static ResourceLocation defaultTexture(net.neoforged.neoforge.common.extensions.IItemExtension self, ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return null;
    }
}
