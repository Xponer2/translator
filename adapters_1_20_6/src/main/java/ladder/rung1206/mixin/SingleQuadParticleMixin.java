/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21's quad helpers of a single quad particle on 1.20.6 (the methods are added to the class): a mod particle draws its
 * quad with a rotation of its own through them, as on 1.21. 1.20.6's own render keeps its own drawing.
 */
@Mixin(SingleQuadParticle.class)
abstract class SingleQuadParticleMixin extends Particle {
    protected SingleQuadParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Shadow
    public abstract float getQuadSize(float partialTick);

    @Shadow
    protected abstract float getU0();

    @Shadow
    protected abstract float getU1();

    @Shadow
    protected abstract float getV0();

    @Shadow
    protected abstract float getV1();

    /** the quad at the particle's place this frame, seen from the camera */
    protected void renderRotatedQuad(VertexConsumer buffer, Camera camera, Quaternionf rotation, float partialTick) {
        Vec3 eye = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - eye.x());
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - eye.y());
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - eye.z());
        this.renderRotatedQuad(buffer, rotation, x, y, z, partialTick);
    }

    /** the quad's four corners around (x, y, z), turned by the rotation */
    protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf rotation, float x, float y, float z, float partialTick) {
        float size = this.getQuadSize(partialTick);
        float u0 = this.getU0(), u1 = this.getU1(), v0 = this.getV0(), v1 = this.getV1();
        int light = this.getLightColor(partialTick);
        this.renderVertex(buffer, rotation, x, y, z, 1.0F, -1.0F, size, u1, v1, light);
        this.renderVertex(buffer, rotation, x, y, z, 1.0F, 1.0F, size, u1, v0, light);
        this.renderVertex(buffer, rotation, x, y, z, -1.0F, 1.0F, size, u0, v0, light);
        this.renderVertex(buffer, rotation, x, y, z, -1.0F, -1.0F, size, u0, v1, light);
    }

    private void renderVertex(VertexConsumer buffer, Quaternionf rotation, float x, float y, float z, float cornerX, float cornerY, float size, float u,
            float v, int light) {
        Vector3f corner = new Vector3f(cornerX, cornerY, 0.0F).rotate(rotation).mul(size).add(x, y, z);
        buffer.vertex(corner.x(), corner.y(), corner.z()).uv(u, v).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }
}
