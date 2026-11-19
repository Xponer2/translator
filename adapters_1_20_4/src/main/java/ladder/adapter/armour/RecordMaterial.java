/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.armour;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A 1.20.4 armour material made of a 1.20.5 one (a record in the armour material registry, held by its holder): what
 * 1.20.4's armour item reads of its material (defense and toughness for its modifiers, enchantability, the equip sound,
 * the repair ingredient, the texture's name) comes from the record, read when asked (a mod's own holder is bound once
 * the registry is filled). Durability is not the material's in 1.20.5 (the item's properties have it): 0 here, so
 * 1.20.4's default durability leaves the properties' value.
 */
public final class RecordMaterial implements ArmorMaterial {
    final Holder<ladder.carried.net.minecraft.world.item.ArmorMaterial> holder;

    RecordMaterial(Holder<ladder.carried.net.minecraft.world.item.ArmorMaterial> holder) {
        this.holder = holder;
    }

    /** the record's texture layers (drawn one by one: ArmourLayerMixin) */
    public java.util.List<ladder.carried.net.minecraft.world.item.ArmorMaterial.Layer> layers() {
        return this.record().layers();
    }

    private ladder.carried.net.minecraft.world.item.ArmorMaterial record() {
        return this.holder.value();
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return 0;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.record().getDefense(type);
    }

    @Override
    public int getEnchantmentValue() {
        return this.record().enchantmentValue();
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.record().equipSound().value();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.record().repairIngredient().get();
    }

    /** the first layer's asset name, as 1.20.5 names the texture (namespace:path: 1.20.4 makes the same file of it) */
    @Override
    public String getName() {
        var layers = this.record().layers();
        if (!layers.isEmpty()) {
            return ArmourMaterials.assetName(layers.get(0)).toString();
        }
        return this.holder.unwrapKey().map(key -> key.location().toString()).orElse("unnamed");
    }

    @Override
    public float getToughness() {
        return this.record().toughness();
    }

    @Override
    public float getKnockbackResistance() {
        return this.record().knockbackResistance();
    }

    @Override
    public String toString() {
        return "RecordMaterial[" + this.getName() + "]";
    }
}
