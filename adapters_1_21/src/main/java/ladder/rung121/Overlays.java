/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.metadata.MetadataSectionType;

/** NeoForge 21.1's {@code OverlayMetadataSection.NEOFORGE_TYPE} (an F row): the {@code neoforge:overlays} pack section. */
public final class Overlays {
    public static final MetadataSectionType<OverlayMetadataSection> NEOFORGE_TYPE = MetadataSectionType.fromCodec("neoforge:overlays",
            RecordCodecBuilder.create(i -> i.group(OverlayMetadataSection.OverlayEntry.CODEC.listOf().fieldOf("entries").forGetter(OverlayMetadataSection::overlays))
                    .apply(i, OverlayMetadataSection::new)));

    private Overlays() {
    }

    public static MetadataSectionType<OverlayMetadataSection> neoforgeType() {
        return NEOFORGE_TYPE;
    }
}
