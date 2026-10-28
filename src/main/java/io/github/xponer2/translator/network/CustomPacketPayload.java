/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.network;

import net.minecraft.util.ResourceLocation;

/**
 * A packet of a mod (1.21 {@code net.minecraft.network.protocol.common.custom.CustomPacketPayload}): registered with
 * a {@link PayloadRegistrar}, sent with {@link PacketDistributor}.
 */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    static <T extends CustomPacketPayload> Type<T> createType(String id) {
        return new Type<>(io.github.xponer2.translator.util.ResourceLocations.parse(id));
    }

    /**
     * The id of a payload type; its namespace picks the network channel ({@link NetworkChannels}).
     */
    final class Type<T extends CustomPacketPayload> {
        private final ResourceLocation id;

        public Type(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return this.id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Type && ((Type<?>) o).id.equals(this.id);
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        @Override
        public String toString() {
            return "Type[" + this.id + "]";
        }
    }
}
