/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.registries.IRegistryExtension;

/** NeoForge 21.1's small additions on 1.21 (the X rows' adapters, receiver first) */
public final class SmallAdapters {
    /** the registries the loot tables being loaded are read with (ReloadableServerRegistriesMixin keeps them) */
    public static volatile HolderLookup.Provider lootRegistries;

    private SmallAdapters() {
    }


    /** {@code IRegistryExtension.getKeyOrNull(element)}: its key, null when it isn't in the registry (a defaulted one too) */
    @SuppressWarnings("unchecked")
    public static <T> ResourceLocation getKeyOrNull(IRegistryExtension<T> self, T element) {
        return ((Registry<T>) self).getResourceKey(element).map(ResourceKey::location).orElse(null);
    }

    public static <T> T getExistingDataOrNull(IAttachmentHolder self, AttachmentType<T> type) {
        return self.getExistingData(type).orElse(null);
    }

    public static <T> T getExistingDataOrNull(IAttachmentHolder self, Supplier<AttachmentType<T>> type) {
        return self.getExistingData(type.get()).orElse(null);
    }

    /** {@code LootTableLoadEvent.getRegistries()}: the registries of the reload that reads the table */
    public static HolderLookup.Provider getRegistries(LootTableLoadEvent event) {
        return lootRegistries;
    }

    /**
     * {@code DeferredRegister.createDataComponents(registryKey, modid)} of 21.1: 21.0's takes only the data component
     * type registry (21.1 also takes the enchantment effect component types, which 1.21 can't register for mods)
     */
    public static net.neoforged.neoforge.registries.DeferredRegister.DataComponents createDataComponents(
            ResourceKey<? extends Registry<net.minecraft.core.component.DataComponentType<?>>> registryKey, String modid) {
        if (!registryKey.equals(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE)) {
            throw new UnsupportedOperationException("1.21 (NeoForge 21.0) registers mods' data components only in "
                    + net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE + ", not in " + registryKey);
        }
        return net.neoforged.neoforge.registries.DeferredRegister.createDataComponents(modid);
    }

    /** {@code ITagBuilderExtension.removeElement(id)} of 21.1: 21.0's with a source (only shown in logs) */
    public static net.minecraft.tags.TagBuilder removeElement(net.neoforged.neoforge.common.extensions.ITagBuilderExtension self, ResourceLocation id) {
        return self.removeElement(id, "unknown");
    }

    /** {@code ITagBuilderExtension.removeTag(id)} of 21.1 */
    public static net.minecraft.tags.TagBuilder removeTag(net.neoforged.neoforge.common.extensions.ITagBuilderExtension self, ResourceLocation id) {
        return self.removeTag(id, "unknown");
    }

    /** {@code CommonHooks.canMobEffectBeApplied(entity, effect, source)} of 21.1: 21.0's event has no source */
    public static boolean canMobEffectBeApplied(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.effect.MobEffectInstance effect,
            @javax.annotation.Nullable net.minecraft.world.entity.Entity source) {
        return net.neoforged.neoforge.common.CommonHooks.canMobEffectBeApplied(entity, effect);
    }
}
