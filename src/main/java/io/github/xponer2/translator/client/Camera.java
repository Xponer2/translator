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

	/** A camera at a place, turned as the game's (1.12.2 draws particles relative to the view entity's position). */
	public static Camera at(double x, double y, double z) {
		Camera camera = new Camera();
		camera.ownPosition = new Vec3d(x, y, z);
		return camera;
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
		if (this.ownPosition != null) {
			return this.ownPosition;
		}
		Entity entity = this.getEntity();
		return entity == null ? Vec3d.ZERO : ActiveRenderInfo.projectViewFromEntity(entity, Minecraft.getMinecraft().getRenderPartialTicks());
	}

	public BlockPos getBlockPosition() {
		return new BlockPos(this.getPosition());
	}

	public float getYRot() {
		if (this.ownRotation != null) {
			return this.ownRotation[0];
		}
		Entity entity = this.getEntity();
		if (entity == null) {
			return 0;
		}
		float pt = Minecraft.getMinecraft().getRenderPartialTicks();
		float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * pt;
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 2 ? yaw + 180 : yaw;
	}

	public float getXRot() {
		if (this.ownRotation != null) {
			return this.ownRotation[1];
		}
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

	// --- a mod's own camera (Moonlight's dummy camera, Supplementaries' cannon view): what it sets, it reads ---------

	@Nullable
	private Vec3d ownPosition;
	@Nullable
	private float[] ownRotation;

	/** 1.21 {@code setPosition(position)}: this camera's own place from now on (the game's camera keeps following the view). */
	protected void setPosition(Vec3d position) {
		this.ownPosition = position;
	}

	protected void setPosition(double x, double y, double z) {
		this.setPosition(new Vec3d(x, y, z));
	}

	/** 1.21 {@code setRotation(yRot, xRot)}: this camera's own turn from now on. */
	protected void setRotation(float yRot, float xRot) {
		this.ownRotation = new float[] {yRot, xRot};
	}

	/** NeoForge's {@code setRotation(yRot, xRot, roll)}: the roll isn't kept. */
	public void setRotation(float yRot, float xRot, float roll) {
		this.setRotation(yRot, xRot);
	}

	/** 1.21 {@code move(forwards, up, left)}: along this camera's look, up and left vectors. */
	protected void move(float forwards, float up, float left) {
		org.joml.Vector3f look = this.getLookVector();
		org.joml.Vector3f upVector = this.getUpVector();
		org.joml.Vector3f leftVector = this.getLeftVector();
		Vec3d position = this.getPosition();
		this.setPosition(position.x + look.x * forwards + upVector.x * up + leftVector.x * left,
				position.y + look.y * forwards + upVector.y * up + leftVector.y * left,
				position.z + look.z * forwards + upVector.z * up + leftVector.z * left);
	}

	/** 1.21 {@code getMaxZoom(distance)}: the distance the view can back off before a block; 1.12.2's clip is its renderer's. */
	protected float getMaxZoom(float distance) {
		return distance;
	}

	/**
	 * 1.21 {@code getFluidInCamera()}: the FogType (carried, by name) of the block at the eyes: lava, water or none
	 * (the Aether's and the mods' fog handlers ask for it).
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <R> R getFluidInCamera() {
		String type = "NONE";
		Entity entity = this.getEntity();
		if (entity != null && entity.world != null) {
			net.minecraft.block.material.Material material = ActiveRenderInfo.getBlockStateAtEntityViewpoint(entity.world, entity,
					Minecraft.getMinecraft().getRenderPartialTicks()).getMaterial();
			type = material == net.minecraft.block.material.Material.LAVA ? "LAVA" : material == net.minecraft.block.material.Material.WATER ? "WATER" : "NONE";
		}
		try {
			Class enumClass = Class.forName("net.minecraft.world.level.material.FogType", true, Camera.class.getClassLoader());
			return (R) Enum.valueOf(enumClass, type);
		} catch (ClassNotFoundException | RuntimeException e) {
			return null;
		}
	}

	/**
	 * 1.21 {@code getNearPlane()}: the view's near plane, 0.05 ahead, as wide and high as the field of view there
	 * (Twilight Forest finds the hand of the first person view on it for its scepters' beams).
	 */
	public NearPlane getNearPlane() {
		Minecraft minecraft = Minecraft.getMinecraft();
		double aspect = (double) minecraft.displayWidth / Math.max(1, minecraft.displayHeight);
		double halfHeight = Math.tan(minecraft.gameSettings.fovSetting * (Math.PI / 180.0) / 2.0) * 0.05F;
		double halfWidth = halfHeight * aspect;
		org.joml.Vector3f look = this.getLookVector();
		org.joml.Vector3f left = this.getLeftVector();
		org.joml.Vector3f up = this.getUpVector();
		return new NearPlane(new Vec3d(look.x, look.y, look.z).scale(0.05F), new Vec3d(left.x, left.y, left.z).scale(halfWidth),
				new Vec3d(up.x, up.y, up.z).scale(halfHeight));
	}

	/** 1.21 {@code Camera.NearPlane}: the near plane by its center, half width (left) and half height (up). */
	public static class NearPlane {
		private final Vec3d forward;
		private final Vec3d left;
		private final Vec3d up;

		NearPlane(Vec3d forward, Vec3d left, Vec3d up) {
			this.forward = forward;
			this.left = left;
			this.up = up;
		}

		public Vec3d getTopLeft() {
			return this.forward.add(this.up).add(this.left);
		}

		public Vec3d getTopRight() {
			return this.forward.add(this.up).subtract(this.left);
		}

		public Vec3d getBottomLeft() {
			return this.forward.subtract(this.up).add(this.left);
		}

		public Vec3d getBottomRight() {
			return this.forward.subtract(this.up).subtract(this.left);
		}

		/** The point at (x, y) of the plane, -1 to 1 each way (left is -1). */
		public Vec3d getPointOnPlane(float x, float y) {
			return this.forward.add(this.up.scale(y)).subtract(this.left.scale(x));
		}
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
