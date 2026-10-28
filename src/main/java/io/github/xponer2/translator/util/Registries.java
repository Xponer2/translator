package io.github.xponer2.translator.util;

import io.github.xponer2.translator.component.DataComponentType;
import io.github.xponer2.translator.item.ArmorMaterialData;
import io.github.xponer2.translator.item.CreativeModeTab;
import io.github.xponer2.translator.recipe.RecipeSerializer;
import io.github.xponer2.translator.recipe.RecipeType;
import io.github.xponer2.translator.registry.BlockEntityType;
import io.github.xponer2.translator.registry.EntityType;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

/**
 * Keys of the registries (1.21 {@code net.minecraft.core.registries.Registries}), same ids as on 1.21. The registries
 * themselves are in {@link BuiltInRegistries}.
 */
public final class Registries {
    public static final ResourceKey<Registry<Item>> ITEM = create("item");
    public static final ResourceKey<Registry<Block>> BLOCK = create("block");
    public static final ResourceKey<Registry<Enchantment>> ENCHANTMENT = create("enchantment");
    public static final ResourceKey<Registry<SoundEvent>> SOUND_EVENT = create("sound_event");
    // 1.21 MobEffect
    public static final ResourceKey<Registry<Potion>> MOB_EFFECT = create("mob_effect");
    // the Forge entity registry under 1.21 ids (EntityTypeRegistry)
    public static final ResourceKey<Registry<EntityType<?>>> ENTITY_TYPE = create("entity_type");
    // registries 1.12.2 doesn't have (MappedRegistry)
    public static final ResourceKey<Registry<io.github.xponer2.translator.menu.MenuType<?>>> MENU = create("menu");
    public static final ResourceKey<Registry<DataComponentType<?>>> DATA_COMPONENT_TYPE = create("data_component_type");
    public static final ResourceKey<Registry<BlockEntityType<?>>> BLOCK_ENTITY_TYPE = create("block_entity_type");
    public static final ResourceKey<Registry<RecipeType<?>>> RECIPE_TYPE = create("recipe_type");
    public static final ResourceKey<Registry<ArmorMaterialData>> ARMOR_MATERIAL = create("armor_material");
    public static final ResourceKey<Registry<CreativeModeTab>> CREATIVE_MODE_TAB = create("creative_mode_tab");
    public static final ResourceKey<Registry<RecipeSerializer<?>>> RECIPE_SERIALIZER = create("recipe_serializer");

    private Registries() { }

    private static <T> ResourceKey<Registry<T>> create(String name) {
        return ResourceKey.createRegistryKey(new ResourceLocation("minecraft", name));
    }
}
