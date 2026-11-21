/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import ladder.rung121.Overlays;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.neoforge.resource.ResourcePackLoader;

/**
 * A mod's resource pack metadata read the way NeoForge 21.1 reads it: the vanilla overlays and the
 * {@code neoforge:overlays} ones, joined into one list ({@code List.copyOf}). The 1.21.1 mods' injectors into this
 * method expect that shape (fusion adds its overrides folder at the copy).
 */
@Mixin(value = ResourcePackLoader.class, remap = false)
abstract class ResourcePackLoaderMixin {
    @Shadow
    @Final
    public static MetadataSectionType<PackMetadataSection> OPTIONAL_FORMAT;

    /**
     * @author ladder
     * @reason NeoForge 21.1's overlays of mod packs and its method shape
     */
    @Overwrite
    private static Pack.Metadata readMeta(PackType type, PackLocationInfo location, Pack.ResourcesSupplier supplier) throws IOException {
        int version = SharedConstants.getCurrentVersion().getPackVersion(type);
        try (PackResources resources = supplier.openPrimary(location)) {
            PackMetadataSection meta = resources.getMetadataSection(OPTIONAL_FORMAT);
            FeatureFlagSet flags = Optional.ofNullable(resources.getMetadataSection(FeatureFlagsMetadataSection.TYPE)).map(FeatureFlagsMetadataSection::flags)
                    .orElse(FeatureFlagSet.of());
            List<String> vanilla = Optional.ofNullable(resources.getMetadataSection(OverlayMetadataSection.TYPE)).map(o -> o.overlaysForVersion(version))
                    .orElse(List.of());
            List<String> neoforge = Optional.ofNullable(resources.getMetadataSection(Overlays.NEOFORGE_TYPE)).map(o -> o.overlaysForVersion(version))
                    .orElse(List.of());
            List<String> overlays = new ArrayList<>(vanilla);
            overlays.addAll(neoforge);
            overlays = List.copyOf(overlays);
            if (meta == null) {
                return new Pack.Metadata(location.title(), PackCompatibility.COMPATIBLE, flags, overlays, resources.isHidden());
            }
            PackCompatibility compatibility = meta.packFormat() == -1 && meta.supportedFormats().isEmpty() ? PackCompatibility.COMPATIBLE
                    : PackCompatibility.forVersion(Pack.getDeclaredPackVersions(location.id(), meta), version);
            return new Pack.Metadata(meta.description(), compatibility, flags, overlays, resources.isHidden());
        }
    }
}
