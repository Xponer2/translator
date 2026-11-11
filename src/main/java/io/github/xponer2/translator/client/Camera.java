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

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code Camera} (neoforge-1.21.1-runtime.tsv): the game's one camera, read from what 1.12.2 renders from (the
 * view entity, ActiveRenderInfo's offset for the third-person views) at the frame's partial tick.
 */
@SideOnly(Side.CLIENT)
public class Camera {
	private static final Camera MAIN = new Camera();

	/** Public as 1.21's: a mod's own camera extends it (Moonlight's DummyCamera didn't load). */
	public Camera() {
	}

	/** {@code GameRenderer.getMainCamera()}, {@code EntityRenderDispatcher.camera}, the render events' camera. */
	public static Camera main() {
		return MAIN;
	}

	@Nullable
	public Entity getEntity() {
		Minecraft mc = Minecraft.getMinecraft();
		return mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.player;
	}

	public boolean isInitialized() {
		return this.getEntity() != null;
	}

	public boolean isDetached() {
		return Minecraft.getMinecraft().gameSettings.thirdPersonView != 0;
	}

	public Vec3d getPosition() {
		Entity entity = this.getEntity();
		return entity == null ? Vec3d.ZERO : ActiveRenderInfo.projectViewFromEntity(entity, Minecraft.getMinecraft().getRenderPartialTicks());
	}

	public BlockPos getBlockPosition() {
		return new BlockPos(this.getPosition());
	}

	public float getYRot() {
		Entity entity = this.getEntity();
		if (entity == null) {
			return 0;
		}
		float pt = Minecraft.getMinecraft().getRenderPartialTicks();
		float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * pt;
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 2 ? yaw + 180 : yaw;
	}

	public float getXRot() {
		Entity entity = this.getEntity();
		if (entity == null) {
			return 0;
		}
		float pt = Minecraft.getMinecraft().getRenderPartialTicks();
		float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * pt;
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 2 ? -pitch : pitch;
	}

	public float getPartialTickTime() {
		return Minecraft.getMinecraft().getRenderPartialTicks();
	}

	/** As 1.21 sets it: yaw about Y (south is 0), then pitch about X. */
	public org.joml.Quaternionf rotation() {
		float rad = (float) (Math.PI / 180);
		return new org.joml.Quaternionf().rotationYXZ((float) Math.PI - this.getYRot() * rad, -this.getXRot() * rad, 0);
	}

	public org.joml.Vector3f getLookVector() {
		return new org.joml.Vector3f(0, 0, -1).rotate(this.rotation());
	}

	public org.joml.Vector3f getUpVector() {
		return new org.joml.Vector3f(0, 1, 0).rotate(this.rotation());
	}

	public org.joml.Vector3f getLeftVector() {
		return new org.joml.Vector3f(-1, 0, 0).rotate(this.rotation());
	}
}
