/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;

/**
 * 1.21's {@code Entity.getAddEntityPacket(serverEntity)} (the tracker that sends it): a converted mod entity that
 * overrides it gets this interface (an O row); 1.20.4's tracker asks {@code getAddEntityPacket()}, and the adapters'
 * mixin (mixin.ServerEntityMixin) asks the override with the tracker.
 */
public interface AddEntityPacketApi {
    /** (every mod entity gets the interface: one that doesn't override answers as the game) */
    default Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return ((Entity) this).getAddEntityPacket();
    }

    /** the game's own packet (a super call): 1.20.4's, without the tracker */
    static Packet<ClientGamePacketListener> getAddEntityPacket(Entity self, ServerEntity serverEntity) {
        return self.getAddEntityPacket();
    }

    /** {@code entity.getAddEntityPacket(serverEntity)} on any entity */
    static Packet<ClientGamePacketListener> packet(Entity entity, ServerEntity serverEntity) {
        return entity instanceof AddEntityPacketApi own ? own.getAddEntityPacket(serverEntity) : entity.getAddEntityPacket();
    }

    /** 1.21's {@code new ClientboundAddEntityPacket(entity, serverEntity)}: what the tracker last sent, as 1.21 makes it */
    static ClientboundAddEntityPacket create(Entity entity, ServerEntity serverEntity) {
        return create(entity, serverEntity, 0);
    }

    /** 1.21's {@code new ClientboundAddEntityPacket(entity, serverEntity, data)} */
    static ClientboundAddEntityPacket create(Entity entity, ServerEntity serverEntity, int data) {
        net.minecraft.world.phys.Vec3 base = getPositionBase(serverEntity);
        return new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(), base.x(), base.y(), base.z(), getLastSentXRot(serverEntity),
                getLastSentYRot(serverEntity), entity.getType(), data, getLastSentMovement(serverEntity), getLastSentYHeadRot(serverEntity));
    }

    /** 1.21's {@code serverEntity.getPositionBase()} */
    static net.minecraft.world.phys.Vec3 getPositionBase(ServerEntity serverEntity) {
        return ((ladder.adapter.mixin.VecDeltaCodecAccess) ((ladder.adapter.mixin.ServerEntityAccess) serverEntity).ladder$positionCodec()).ladder$base();
    }

    static float getLastSentYRot(ServerEntity serverEntity) {
        return unpack(((ladder.adapter.mixin.ServerEntityAccess) serverEntity).ladder$yRot());
    }

    static float getLastSentXRot(ServerEntity serverEntity) {
        return unpack(((ladder.adapter.mixin.ServerEntityAccess) serverEntity).ladder$xRot());
    }

    static float getLastSentYHeadRot(ServerEntity serverEntity) {
        return unpack(((ladder.adapter.mixin.ServerEntityAccess) serverEntity).ladder$yHeadRot());
    }

    static net.minecraft.world.phys.Vec3 getLastSentMovement(ServerEntity serverEntity) {
        net.minecraft.world.phys.Vec3 movement = ((ladder.adapter.mixin.ServerEntityAccess) serverEntity).ladder$movement();
        return movement == null ? net.minecraft.world.phys.Vec3.ZERO : movement;
    }

    /** a rotation the tracker keeps packed into a byte's 256 steps */
    private static float unpack(int packed) {
        return (byte) packed * 360.0F / 256.0F;
    }
}
