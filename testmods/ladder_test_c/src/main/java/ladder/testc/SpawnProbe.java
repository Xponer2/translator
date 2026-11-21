/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * An entity of the mod's own whose spawn packet is made with its tracker (1.21's getAddEntityPacket(serverEntity)): the
 * server spawns one near the player, the tracker pairs it with the client, the client sees it.
 */
final class SpawnProbe extends Entity {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, LadderTestC.MODID);
    static final DeferredHolder<EntityType<?>, EntityType<SpawnProbe>> TYPE = ENTITIES.register("spawn_probe",
            () -> EntityType.Builder.<SpawnProbe>of(SpawnProbe::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).build("spawn_probe"));

    /** what the tracker passed when it asked for the packet ("-" before it asked) */
    static volatile String pairedWith = "-";

    SpawnProbe(EntityType<? extends SpawnProbe> type, Level level) {
        super(type, level);
    }

    static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    static void spawnNear(ServerPlayer player) {
        SpawnProbe probe = TYPE.get().create(player.serverLevel());
        probe.setPos(player.getX() + 2.0, player.getY(), player.getZ());
        player.serverLevel().addFreshEntity(probe);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        pairedWith = serverEntity == null ? "null" : "tracker";
        return super.getAddEntityPacket(serverEntity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
