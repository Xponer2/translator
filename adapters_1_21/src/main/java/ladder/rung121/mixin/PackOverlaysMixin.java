/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;

/**
 * NeoForge 21.1's {@code neoforge:overlays} pack metadata (overlays only NeoForge applies, beside the vanilla
 * {@code overlays}): NeoForge 21.0 reads the vanilla section alone, so a 1.21.1 pack's NeoForge overlays would be left out. The
 * pack's overlays are both sections' (as 21.1 reads them).
 */
@Mixin(Pack.class)
abstract class PackOverlaysMixin {
    @Redirect(method = "readPackMetadata", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/PackResources;getMetadataSection(Lnet/minecraft/server/packs/metadata/MetadataSectionSerializer;)Ljava/lang/Object;"))
    private static Object ladder$neoforgeOverlays(PackResources resources, MetadataSectionSerializer<?> type) throws java.io.IOException {
        Object section = resources.getMetadataSection(type);
        if (type != OverlayMetadataSection.TYPE) {
            return section;
        }
        OverlayMetadataSection neoforge = resources.getMetadataSection(ladder.rung121.Overlays.NEOFORGE_TYPE);
        if (neoforge == null) {
            return section;
        }
        List<OverlayMetadataSection.OverlayEntry> all = new ArrayList<>();
        if (section != null) {
            all.addAll(((OverlayMetadataSection) section).overlays());
        }
        all.addAll(neoforge.overlays());
        return new OverlayMetadataSection(all);
    }
}
