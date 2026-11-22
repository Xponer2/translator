/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.enchantment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 1.21's {@code enchantment.effects()} on 1.20.6: a mod's data enchantment's effects, read from its file with 1.21's own
 * effect component types (carried, in the adapters' registry); 1.20.6's enchantments keep their effects in code (none
 * here). Read once the server's registries are there (the effects name sounds, damage types...).
 */
public final class EnchantEffects {
    private static final Map<Enchantment, DataComponentMap> READ = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ladder");

    private EnchantEffects() {
    }

    /** {@code enchantment.effects()} (an X row) */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DataComponentMap effects(Enchantment enchantment) {
        JsonObject json = DataEnchantments.EFFECTS.get(enchantment);
        if (json == null || EnchantRegistries.enchantmentEffectComponentType() == null) {
            return DataComponentMap.EMPTY;
        }
        DataComponentMap known = READ.get(enchantment);
        if (known != null) {
            return known;
        }
        RegistryAccess registries = ServerLifecycleHooks.getCurrentServer() != null ? ServerLifecycleHooks.getCurrentServer().registryAccess() : null;
        if (registries == null) {
            return DataComponentMap.EMPTY;
        }
        Codec<DataComponentType<?>> types = (Codec) EnchantRegistries.enchantmentEffectComponentType().byNameCodec();
        DataComponentMap read = EnchantLoot.makeCodec(types).parse(RegistryOps.create(JsonOps.INSTANCE, registries), json).resultOrPartial(
                message -> LOGGER.warn("the effects of an enchantment can't be read: {}", message)).orElse(DataComponentMap.EMPTY);
        READ.put(enchantment, read);
        return read;
    }

    /** {@code enchantment.getEffects(type)} (an X row): the list-valued effect, or none */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getEffects(Enchantment enchantment, DataComponentType<List<T>> type) {
        List<T> list = effects(enchantment).get(type);
        return list == null ? List.of() : list;
    }
}
