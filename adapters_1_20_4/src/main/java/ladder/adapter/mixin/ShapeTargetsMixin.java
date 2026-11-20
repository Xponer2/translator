/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Empty: the classes the shape patches edit (shape-patches.tsv; ShapePlugin edits them in this mixin's pre-apply). The
 * converter checks that every patched class is listed here.
 */
@Mixin(targets = {"net.minecraft.client.gui.screens.packs.TransferableSelectionList$PackEntry", "net.minecraft.client.gui.screens.OptionsScreen"},
        priority = 1)
abstract class ShapeTargetsMixin {
}
