/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

/**
 * NeoForge 21.1's armour pieces are drawn with the entity's animation values (HumanoidArmorLayer.renderArmorPiece with
 * limb swing, limb swing amount, partial tick, age in ticks, head yaw and pitch, for IClientItemExtensions'
 * setupModelAnimations); 1.20.4 draws them without. The layer's render keeps them here while it draws (on the render
 * thread), for the A row of a mod's mixin into the piece; out of it they are 0, as 1.21.1's piece without them passes.
 */
public final class ArmorArgs {
    private static final float[] VALUES = new float[6];

    private ArmorArgs() {
    }

    public static void set(float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        VALUES[0] = limbSwing;
        VALUES[1] = limbSwingAmount;
        VALUES[2] = partialTick;
        VALUES[3] = ageInTicks;
        VALUES[4] = netHeadYaw;
        VALUES[5] = headPitch;
    }

    public static void clear() {
        java.util.Arrays.fill(VALUES, 0.0F);
    }

    public static float limbSwing() {
        return VALUES[0];
    }

    public static float limbSwingAmount() {
        return VALUES[1];
    }

    public static float partialTick() {
        return VALUES[2];
    }

    public static float ageInTicks() {
        return VALUES[3];
    }

    public static float netHeadYaw() {
        return VALUES[4];
    }

    public static float headPitch() {
        return VALUES[5];
    }
}
