package io.github.xponer2.translator.item;

import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.EnumHelper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * An armor material (1.21 {@code ArmorMaterial}, registered in {@code Registries.ARMOR_MATERIAL}). Not named
 * ArmorMaterial: inside armor items that name is the inherited 1.12.2 {@code ItemArmor.ArmorMaterial}.
 * {@link #toolMaterial()}: the matching 1.12.2 {@code ItemArmor.ArmorMaterial} (EnumHelper), whose texture is the
 * first layer's asset: {@code <namespace>:textures/models/armor/<path>_layer_1.png}. The durability comes from the
 * item (1.21 {@code ArmorType#getDurability}), not from the material.
 */
public final class ArmorMaterialData {
    private final Map<ArmorType, Integer> defense;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final Supplier<Ingredient> repairIngredient;
    private final List<Layer> layers;
    private final float toughness;
    private final float knockbackResistance;
    private net.minecraft.item.ItemArmor.ArmorMaterial vanilla;

    public ArmorMaterialData(Map<ArmorType, Integer> defense, int enchantmentValue, SoundEvent equipSound, Supplier<Ingredient> repairIngredient,
                         List<Layer> layers, float toughness, float knockbackResistance) {
        this.defense = defense;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.repairIngredient = repairIngredient;
        this.layers = layers;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    public int getDefense(ArmorType type) {
        return this.defense.getOrDefault(type, 0);
    }

    public int enchantmentValue() {
        return this.enchantmentValue;
    }

    public SoundEvent equipSound() {
        return this.equipSound;
    }

    public Supplier<Ingredient> repairIngredient() {
        return this.repairIngredient;
    }

    public List<Layer> layers() {
        return this.layers;
    }

    public float toughness() {
        return this.toughness;
    }

    public float knockbackResistance() {
        return this.knockbackResistance;
    }

    public synchronized net.minecraft.item.ItemArmor.ArmorMaterial toolMaterial() {
        if (this.vanilla == null) {
            var asset = this.layers.isEmpty() ? new ResourceLocation("minecraft", "iron") : this.layers.get(0).assetName();
            // 1.12.2 order of the reductions: feet, legs, chest, head
            var reductions = new int[] { this.getDefense(ArmorType.BOOTS), this.getDefense(ArmorType.LEGGINGS),
                    this.getDefense(ArmorType.CHESTPLATE), this.getDefense(ArmorType.HELMET) };
            var name = (asset.getNamespace() + "_" + asset.getPath()).toUpperCase(Locale.ROOT);
            this.vanilla = EnumHelper.addArmorMaterial(name, asset.toString(), 0, reductions, this.enchantmentValue, this.equipSound, this.toughness);
        }
        return this.vanilla;
    }

    /**
     * A texture layer (1.21 {@code ArmorMaterial.Layer}); 1.12.2 armor has one texture per material.
     */
    public static final class Layer {
        private final ResourceLocation assetName;
        private final String suffix;
        private final boolean dyeable;

        public Layer(ResourceLocation assetName, String suffix, boolean dyeable) {
            this.assetName = assetName;
            this.suffix = suffix;
            this.dyeable = dyeable;
        }

        public Layer(ResourceLocation assetName) {
            this(assetName, "", false);
        }

        public ResourceLocation assetName() {
            return this.assetName;
        }

        public String suffix() {
            return this.suffix;
        }

        public boolean dyeable() {
            return this.dyeable;
        }
    }
}
