/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import ladder.rung1206.provided.Leashable;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21 draws the leash of any leashable entity in EntityRenderer (render, renderLeash, addVertexPair); 1.20.6 draws a
 * mob's in MobRenderer. The mods' mixins into 1.21's methods land here; MobRendererLeashMixin stops 1.20.6's own one.
 */
@Mixin(value = EntityRenderer.class, priority = 500)
abstract class EntityRendererLeashMixin<T extends Entity> {
    @Shadow
    @Final
    protected EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    protected abstract boolean shouldShowName(T entity);

    @Shadow
    protected abstract void renderNameTag(T entity, Component name, PoseStack pose, MultiBufferSource buffers, int light, float partialTick);

    @Shadow
    protected abstract int getBlockLightLevel(T entity, BlockPos pos);

    /**
     * @author ladder
     * @reason 1.21's render: the entity's leash first, then 20.6's name tag event (1.20.6 leaves the leash to MobRenderer)
     */
    @Overwrite
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity instanceof Leashable leashable) {
            Entity holder = leashable.getLeashHolder();
            if (holder != null) {
                this.renderLeash(entity, partialTick, pose, buffers, holder);
            }
        }
        var event = new net.neoforged.neoforge.client.event.RenderNameTagEvent(entity, entity.getDisplayName(),
                (EntityRenderer<?>) (Object) this, pose, buffers, light, partialTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        if (event.canRender().isTrue() || event.canRender().isDefault() && this.shouldShowName(entity)) {
            this.renderNameTag(entity, event.getContent(), pose, buffers, light, partialTick);
        }
    }

    /** 1.21's EntityRenderer.renderLeash: 1.20.6's MobRenderer.renderLeash for any entity */
    private <E extends Entity> void renderLeash(T entity, float partialTick, PoseStack pose, MultiBufferSource buffers, E holder) {
        pose.pushPose();
        Vec3 hold = holder.getRopeHoldPosition(partialTick);
        double angle = (double) (ladder.rung1206.render.LeashAdapters.getPreciseBodyRotation(entity, partialTick) * (float) (Math.PI / 180.0))
                + (Math.PI / 2);
        Vec3 offset = entity.getLeashOffset(partialTick);
        double dx = Math.cos(angle) * offset.z + Math.sin(angle) * offset.x;
        double dz = Math.sin(angle) * offset.z - Math.cos(angle) * offset.x;
        double x = Mth.lerp((double) partialTick, entity.xo, entity.getX()) + dx;
        double y = Mth.lerp((double) partialTick, entity.yo, entity.getY()) + offset.y;
        double z = Mth.lerp((double) partialTick, entity.zo, entity.getZ()) + dz;
        pose.translate(dx, offset.y, dz);
        float fx = (float) (hold.x - x);
        float fy = (float) (hold.y - y);
        float fz = (float) (hold.z - z);
        VertexConsumer consumer = buffers.getBuffer(RenderType.leash());
        Matrix4f matrix = pose.last().pose();
        float scale = Mth.invSqrt(fx * fx + fz * fz) * 0.025F / 2.0F;
        float sz = fz * scale;
        float sx = fx * scale;
        BlockPos entityPos = BlockPos.containing(entity.getEyePosition(partialTick));
        BlockPos holderPos = BlockPos.containing(holder.getEyePosition(partialTick));
        int entityBlock = this.getBlockLightLevel(entity, entityPos);
        int holderBlock = ((EntityRendererLightAccess) this.entityRenderDispatcher.getRenderer(holder)).ladder$blockLightLevel(holder, holderPos);
        int entitySky = entity.level().getBrightness(LightLayer.SKY, entityPos);
        int holderSky = entity.level().getBrightness(LightLayer.SKY, holderPos);
        for (int i = 0; i <= 24; i++) {
            addVertexPair(consumer, matrix, fx, fy, fz, entityBlock, holderBlock, entitySky, holderSky, 0.025F, 0.025F, sz, sx, i, false);
        }
        for (int i = 24; i >= 0; i--) {
            addVertexPair(consumer, matrix, fx, fy, fz, entityBlock, holderBlock, entitySky, holderSky, 0.025F, 0.0F, sz, sx, i, true);
        }
        pose.popPose();
    }

    /** 1.21's EntityRenderer.addVertexPair: 1.20.6's MobRenderer.addVertexPair */
    private static void addVertexPair(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, int entityBlock, int holderBlock,
            int entitySky, int holderSky, float height, float width, float sz, float sx, int step, boolean back) {
        float f = (float) step / 24.0F;
        int block = (int) Mth.lerp(f, (float) entityBlock, (float) holderBlock);
        int sky = (int) Mth.lerp(f, (float) entitySky, (float) holderSky);
        int light = LightTexture.pack(block, sky);
        float shade = step % 2 == (back ? 1 : 0) ? 0.7F : 1.0F;
        float r = 0.5F * shade;
        float g = 0.4F * shade;
        float b = 0.3F * shade;
        float px = x * f;
        float py = y > 0.0F ? y * f * f : y - y * (1.0F - f) * (1.0F - f);
        float pz = z * f;
        consumer.vertex(matrix, px - sz, py + width, pz + sx).color(r, g, b, 1.0F).uv2(light).endVertex();
        consumer.vertex(matrix, px + sz, py + height - width, pz - sx).color(r, g, b, 1.0F).uv2(light).endVertex();
    }
}
