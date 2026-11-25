/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * 1.21 draws a dying dragon's rays in its own {@code renderRays(pose, completion, consumer)}, called from render; 1.20.6
 * draws them inline in render. The renderer gets renderRays (1.20.6's rays: the same random, rotations and vertices) and
 * render calls it where 1.20.6's loop began (after the rays' translate); the inline loop runs no rounds. The mods' mixins
 * aim at renderRays (EuphoriaPatcher's shader hooks).
 */
@Mixin(value = EnderDragonRenderer.class, priority = 500)
abstract class EnderDragonRaysMixin {
    @Shadow
    private static void vertex01(VertexConsumer consumer, Matrix4f pose, int alpha) {
        throw new AssertionError();
    }

    @Shadow
    private static void vertex2(VertexConsumer consumer, Matrix4f pose, float length, float width) {
        throw new AssertionError();
    }

    @Shadow
    private static void vertex3(VertexConsumer consumer, Matrix4f pose, float length, float width) {
        throw new AssertionError();
    }

    @Shadow
    private static void vertex4(VertexConsumer consumer, Matrix4f pose, float length, float width) {
        throw new AssertionError();
    }

    /** where 1.20.6's rays began (the third translate of render: the rays' own place): 1.21's renderRays */
    @Inject(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 2, shift = At.Shift.AFTER))
    private void ladder$renderRays(EnderDragon dragon, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light,
            CallbackInfo ci) {
        renderRays(pose, ((float) dragon.dragonDeathTime + partialTicks) / 200.0F, buffers.getBuffer(RenderType.lightning()));
    }

    /** 1.20.6's inline loop of rays runs no rounds (renderRays drew them): render's only 60.0F is that loop's bound */
    @ModifyConstant(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            constant = @Constant(floatValue = 60.0F, ordinal = 0))
    private float ladder$noInlineRays(float rays) {
        return 0.0F;
    }

    /** 1.21's renderRays: 1.20.6's rays of a dying dragon (its completion 0..1) */
    private static void renderRays(PoseStack pose, float completion, VertexConsumer consumer) {
        pose.pushPose();
        float fade = Math.min(completion > 0.8F ? (completion - 0.8F) / 0.2F : 0.0F, 1.0F);
        RandomSource random = RandomSource.create(432L);
        for (int i = 0; (float) i < (completion + completion * completion) / 2.0F * 60.0F; i++) {
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + completion * 90.0F));
            float length = random.nextFloat() * 20.0F + 5.0F + fade * 10.0F;
            float width = random.nextFloat() * 2.0F + 1.0F + fade * 2.0F;
            Matrix4f matrix = pose.last().pose();
            int alpha = (int) (255.0F * (1.0F - fade));
            vertex01(consumer, matrix, alpha);
            vertex2(consumer, matrix, length, width);
            vertex3(consumer, matrix, length, width);
            vertex01(consumer, matrix, alpha);
            vertex3(consumer, matrix, length, width);
            vertex4(consumer, matrix, length, width);
            vertex01(consumer, matrix, alpha);
            vertex4(consumer, matrix, length, width);
            vertex2(consumer, matrix, length, width);
        }
        pose.popPose();
    }
}
