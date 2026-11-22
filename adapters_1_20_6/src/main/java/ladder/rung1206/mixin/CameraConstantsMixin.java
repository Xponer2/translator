/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.Camera;

/**
 * 1.21's camera directions (a mod's mixin shadows them): added to the class as its own constants. (A mod's read of one
 * takes the F row's getter: a mixin can add only private static fields.)
 */
@Mixin(value = Camera.class, priority = 500)
abstract class CameraConstantsMixin {
    private static final Vector3f FORWARDS = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f LEFT = new Vector3f(-1.0F, 0.0F, 0.0F);
}
