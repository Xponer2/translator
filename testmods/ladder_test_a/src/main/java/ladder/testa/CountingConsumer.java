/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testa;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** A vertex consumer of the mod's own (1.21's methods): it sums what the game writes into it. */
final class CountingConsumer implements VertexConsumer {
    int vertices;
    double positions;
    long colours;
    double uvs;
    long overlays;
    long lights;
    double normals;
    /** the quad's sprite: its texture coordinates are counted within it (the atlas differs between versions) */
    net.minecraft.client.renderer.texture.TextureAtlasSprite sprite;

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.vertices++;
        this.positions += x + 2 * y + 3 * z;
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        this.colours += r + 2L * g + 3L * b + 4L * a;
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        float su = this.sprite == null ? u : (u - this.sprite.getU0()) / (this.sprite.getU1() - this.sprite.getU0());
        float sv = this.sprite == null ? v : (v - this.sprite.getV0()) / (this.sprite.getV1() - this.sprite.getV0());
        this.uvs += su + 2 * sv;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.overlays += u + 2L * v;
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.lights += u + 2L * v;
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.normals += x + 2 * y + 3 * z;
        return this;
    }

    @Override
    public String toString() {
        return this.vertices + " " + Math.round(this.positions * 1000) + " " + this.colours + " " + Math.round(this.uvs * 1000) + " " + this.overlays + " "
                + this.lights + " " + Math.round(this.normals * 1000);
    }
}
