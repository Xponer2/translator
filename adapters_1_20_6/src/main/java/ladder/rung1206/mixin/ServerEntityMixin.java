/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.rung1206.entity.AddEntityPacketApi;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;

/** The tracker asks a converted mod entity for its spawn packet with itself (1.21's getAddEntityPacket(serverEntity)). */
@Mixin(ServerEntity.class)
abstract class ServerEntityMixin {
    @Redirect(method = "sendPairingData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getAddEntityPacket()Lnet/minecraft/network/protocol/Packet;"))
    private Packet<ClientGamePacketListener> ladder$withTracker(Entity entity) {
        return AddEntityPacketApi.packet(entity, (ServerEntity) (Object) this);
    }
}
