/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.loot;

import java.util.Collection;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 1.20.5's {@code ReloadableServerRegistries.Holder} (the server's loot tables, predicates and modifiers, reloaded with
 * the data packs) over 1.20.4's LootDataManager, which keeps the same data by location.
 */
public class ReloadableServerRegistries {
    /** 1.20.5's {@code Registries.LOOT_TABLE}: the key loot tables are named by (1.20.4 has no such registry, only its data) */
    public static final ResourceKey<Registry<LootTable>> LOOT_TABLE = ResourceKey.createRegistryKey(new ResourceLocation("loot_table"));
    public static final ResourceKey<Registry<net.minecraft.world.level.storage.loot.predicates.LootItemCondition>> PREDICATE =
            ResourceKey.createRegistryKey(new ResourceLocation("predicate"));
    public static final ResourceKey<Registry<net.minecraft.world.level.storage.loot.functions.LootItemFunction>> ITEM_MODIFIER =
            ResourceKey.createRegistryKey(new ResourceLocation("item_modifier"));

    public static ResourceKey<Registry<LootTable>> lootTableRegistry() {
        return LOOT_TABLE;
    }

    public static ResourceKey<Registry<net.minecraft.world.level.storage.loot.predicates.LootItemCondition>> predicateRegistry() {
        return PREDICATE;
    }

    public static ResourceKey<Registry<net.minecraft.world.level.storage.loot.functions.LootItemFunction>> itemModifierRegistry() {
        return ITEM_MODIFIER;
    }

    /** 1.20.5's {@code ReloadableServerRegistries.Holder}: the server's loot data */
    public static class Holder {
        private final MinecraftServer server;

        public Holder(MinecraftServer server) {
            this.server = server;
        }

        /** MinecraftServer.reloadableRegistries() */
        public static Holder of(MinecraftServer server) {
            return new Holder(server);
        }


        private LootDataManager data() {
            return this.server.getLootData();
        }

        /** the loot table of that key, or the empty one (as 1.20.5 gives it) */
        public LootTable getLootTable(ResourceKey<LootTable> key) {
            return this.data().getLootTable(key.location());
        }

        /** the ids of the loot data of one kind (tables, predicates, item modifiers) */
        public Collection<ResourceLocation> getKeys(ResourceKey<? extends Registry<?>> registry) {
            LootDataType<?> type = registry.equals(PREDICATE) ? LootDataType.PREDICATE : registry.equals(ITEM_MODIFIER) ? LootDataType.MODIFIER : LootDataType.TABLE;
            return this.data().getKeys(type);
        }
    }
}
