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
package io.github.xponer2.translator.client;

/**
 * Rotation axes (1.21 {@code com.mojang.math.Axis}); a rotation is applied to the GL matrix by {@link PoseStack#mulPose}.
 */
public final class Axis {
    public static final Axis XN = new Axis(-1, 0, 0);
    public static final Axis XP = new Axis(1, 0, 0);
    public static final Axis YN = new Axis(0, -1, 0);
    public static final Axis YP = new Axis(0, 1, 0);
    public static final Axis ZN = new Axis(0, 0, -1);
    public static final Axis ZP = new Axis(0, 0, 1);

    private final float x;
    private final float y;
    private final float z;

    private Axis(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Rotation rotationDegrees(float degrees) {
        return new Rotation(degrees, this.x, this.y, this.z);
    }

    public Rotation rotation(float radians) {
        return this.rotationDegrees((float) Math.toDegrees(radians));
    }

    public static final class Rotation {
        private final float degrees;
        private final float x;
        private final float y;
        private final float z;

        Rotation(float degrees, float x, float y, float z) {
            this.degrees = degrees;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float degrees() {
            return this.degrees;
        }

        public float x() {
            return this.x;
        }

        public float y() {
            return this.y;
        }

        public float z() {
            return this.z;
        }
    }
}
