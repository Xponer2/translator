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
package io.github.xponer2.translator.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import io.github.xponer2.translator.client.PoseStack;
import io.github.xponer2.translator.client.VertexConsumer;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code ModelPart}: a part of an entity model with its cubes and named children, baked from a mod's layer
 * definition. It is a 1.12.2 {@link ModelRenderer} (its position, rotation and visibility are the 1.12.2 fields), so
 * 1.12.2 models ({@code ModelBiped}'s arms and head) and renderers draw it too ({@link #render(float)}); the 1.21
 * renderers draw it with a pose stack and a vertex consumer.
 */
@SideOnly(Side.CLIENT)
public class ModelPart extends ModelRenderer {
	public static final float DEFAULT_SCALE = 1.0F;
	/** The 1.12.2 part needs a model to belong to; the 1.21 one doesn't. */
	private static final ModelBase NO_MODEL = new ModelBase() {
	};

	public float xScale = 1.0F;
	public float yScale = 1.0F;
	public float zScale = 1.0F;
	public boolean skipDraw;
	public final List<Cube> cubes;
	public final Map<String, ModelPart> children;
	private PartPose initialPose = PartPose.ZERO;

	public ModelPart(List<Cube> cubes, Map<String, ModelPart> children) {
		super(NO_MODEL);
		synchronized (NO_MODEL) {
			NO_MODEL.boxList.remove(this);
		}
		this.cubes = cubes;
		this.children = children;
	}

	/**
	 * The part of a model field: itself, or for a part of a 1.12.2 model (the vanilla player's head) a part without
	 * cubes with its pose, which 1.21 code reads to copy (copyFrom). What is set on it doesn't reach the vanilla part.
	 */
	public static ModelPart of(ModelRenderer renderer) {
		if (renderer == null || renderer instanceof ModelPart) {
			return (ModelPart) renderer;
		}
		var part = new ModelPart(java.util.Collections.emptyList(), java.util.Collections.emptyMap());
		part.rotationPointX = renderer.rotationPointX;
		part.rotationPointY = renderer.rotationPointY;
		part.rotationPointZ = renderer.rotationPointZ;
		part.rotateAngleX = renderer.rotateAngleX;
		part.rotateAngleY = renderer.rotateAngleY;
		part.rotateAngleZ = renderer.rotateAngleZ;
		part.showModel = renderer.showModel;
		return part;
	}

	public PartPose storePose() {
		return PartPose.offsetAndRotation(this.rotationPointX, this.rotationPointY, this.rotationPointZ, this.rotateAngleX, this.rotateAngleY,
				this.rotateAngleZ);
	}

	public PartPose getInitialPose() {
		return this.initialPose;
	}

	public void setInitialPose(PartPose pose) {
		this.initialPose = pose;
	}

	public void resetPose() {
		this.loadPose(this.initialPose);
	}

	public void loadPose(PartPose pose) {
		this.rotationPointX = pose.x;
		this.rotationPointY = pose.y;
		this.rotationPointZ = pose.z;
		this.rotateAngleX = pose.xRot;
		this.rotateAngleY = pose.yRot;
		this.rotateAngleZ = pose.zRot;
		this.xScale = 1.0F;
		this.yScale = 1.0F;
		this.zScale = 1.0F;
	}

	public void copyFrom(ModelPart other) {
		this.xScale = other.xScale;
		this.yScale = other.yScale;
		this.zScale = other.zScale;
		this.rotateAngleX = other.rotateAngleX;
		this.rotateAngleY = other.rotateAngleY;
		this.rotateAngleZ = other.rotateAngleZ;
		this.rotationPointX = other.rotationPointX;
		this.rotationPointY = other.rotationPointY;
		this.rotationPointZ = other.rotationPointZ;
	}

	public boolean hasChild(String name) {
		return this.children.containsKey(name);
	}

	public ModelPart getChild(String name) {
		ModelPart child = this.children.get(name);
		if (child == null) {
			throw new NoSuchElementException("Can't find part " + name);
		}
		return child;
	}

	public void setPos(float x, float y, float z) {
		this.rotationPointX = x;
		this.rotationPointY = y;
		this.rotationPointZ = z;
	}

	public void setRotation(float xRot, float yRot, float zRot) {
		this.rotateAngleX = xRot;
		this.rotateAngleY = yRot;
		this.rotateAngleZ = zRot;
	}

	// --- 1.21 drawing: a pose stack (the GL matrix) and a vertex consumer -----------------------------------------

	public void render(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
		this.render(poseStack, buffer, light, overlay, -1);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
		if (!this.showModel || this.isHidden || this.cubes.isEmpty() && this.children.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		try {
			this.translateAndRotate(poseStack);
			if (!this.skipDraw) {
				for (Cube cube : this.cubes) {
					cube.compile(poseStack, buffer, light, overlay, color);
				}
			}
			for (ModelPart child : this.children.values()) {
				child.render(poseStack, buffer, light, overlay, color);
			}
		} finally {
			poseStack.popPose();
		}
	}

	public void visit(PoseStack poseStack, Visitor visitor) {
		this.visit(poseStack, visitor, "");
	}

	private void visit(PoseStack poseStack, Visitor visitor, String path) {
		if (this.cubes.isEmpty() && this.children.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		try {
			this.translateAndRotate(poseStack);
			for (int i = 0; i < this.cubes.size(); i++) {
				visitor.visit(poseStack, path, i, this.cubes.get(i));
			}
			String prefix = path + "/";
			this.children.forEach((name, child) -> child.visit(poseStack, visitor, prefix + name));
		} finally {
			poseStack.popPose();
		}
	}

	public void translateAndRotate(PoseStack poseStack) {
		poseStack.translate(this.rotationPointX / 16.0F, this.rotationPointY / 16.0F, this.rotationPointZ / 16.0F);
		rotate();
		if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
			poseStack.scale(this.xScale, this.yScale, this.zScale);
		}
	}

	/** 1.21 rotates z, then y, then x (its quaternion rotationZYX), as 1.12.2's ModelRenderer does. */
	private void rotate() {
		if (this.rotateAngleZ != 0.0F) {
			GlStateManager.rotate(this.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
		}
		if (this.rotateAngleY != 0.0F) {
			GlStateManager.rotate(this.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
		}
		if (this.rotateAngleX != 0.0F) {
			GlStateManager.rotate(this.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
		}
	}

	public Cube getRandomCube(Random random) {
		return this.cubes.get(random.nextInt(this.cubes.size()));
	}

	public boolean isEmpty() {
		return this.cubes.isEmpty();
	}

	public void offsetPos(Vector3f offset) {
		this.rotationPointX += offset.x();
		this.rotationPointY += offset.y();
		this.rotationPointZ += offset.z();
	}

	public void offsetRotation(Vector3f offset) {
		this.rotateAngleX += offset.x();
		this.rotateAngleY += offset.y();
		this.rotateAngleZ += offset.z();
	}

	public void offsetScale(Vector3f offset) {
		this.xScale += offset.x();
		this.yScale += offset.y();
		this.zScale += offset.z();
	}

	public Stream<ModelPart> getAllParts() {
		return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(ModelPart::getAllParts));
	}

	// --- 1.12.2 drawing: what ModelBiped and the 1.12.2 renderers call --------------------------------------------

	@Override
	public void render(float scale) {
		if (!this.showModel || this.isHidden || this.cubes.isEmpty() && this.children.isEmpty()) {
			return;
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(this.offsetX, this.offsetY, this.offsetZ);
		GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
		this.rotate();
		if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
			GlStateManager.scale(this.xScale, this.yScale, this.zScale);
		}
		if (!this.skipDraw && !this.cubes.isEmpty()) {
			BufferBuilder buffer = Tessellator.getInstance().getBuffer();
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
			for (Cube cube : this.cubes) {
				cube.draw(buffer, scale);
			}
			Tessellator.getInstance().draw();
		}
		for (ModelPart child : this.children.values()) {
			child.render(scale);
		}
		GlStateManager.popMatrix();
	}

	@Override
	public void renderWithRotation(float scale) {
		this.render(scale);
	}

	/** 1.21 {@code ModelPart.Visitor}. */
	@FunctionalInterface
	public interface Visitor {
		void visit(PoseStack pose, String path, int index, Cube cube);
	}

	/** 1.21 {@code ModelPart.Cube}: a box of the part, its faces with their texture coordinates. */
	public static class Cube {
		public final Polygon[] polygons;
		public final float minX;
		public final float minY;
		public final float minZ;
		public final float maxX;
		public final float maxY;
		public final float maxZ;

		public Cube(int texU, int texV, float originX, float originY, float originZ, float width, float height, float depth, float growX,
				float growY, float growZ, boolean mirror, float texWidth, float texHeight, Set<EnumFacing> faces) {
			this.minX = originX;
			this.minY = originY;
			this.minZ = originZ;
			this.maxX = originX + width;
			this.maxY = originY + height;
			this.maxZ = originZ + depth;
			this.polygons = new Polygon[faces.size()];
			float x0 = originX - growX;
			float y0 = originY - growY;
			float z0 = originZ - growZ;
			float x1 = originX + width + growX;
			float y1 = originY + height + growY;
			float z1 = originZ + depth + growZ;
			if (mirror) {
				float swap = x1;
				x1 = x0;
				x0 = swap;
			}
			Vertex v0 = new Vertex(x0, y0, z0, 0.0F, 0.0F);
			Vertex v1 = new Vertex(x1, y0, z0, 0.0F, 8.0F);
			Vertex v2 = new Vertex(x1, y1, z0, 8.0F, 8.0F);
			Vertex v3 = new Vertex(x0, y1, z0, 8.0F, 0.0F);
			Vertex v4 = new Vertex(x0, y0, z1, 0.0F, 0.0F);
			Vertex v5 = new Vertex(x1, y0, z1, 0.0F, 8.0F);
			Vertex v6 = new Vertex(x1, y1, z1, 8.0F, 8.0F);
			Vertex v7 = new Vertex(x0, y1, z1, 8.0F, 0.0F);
			float u0 = texU;
			float u1 = texU + depth;
			float u2 = texU + depth + width;
			float u3 = texU + depth + width + width;
			float u4 = texU + depth + width + depth;
			float u5 = texU + depth + width + depth + width;
			float t0 = texV;
			float t1 = texV + depth;
			float t2 = texV + depth + height;
			int i = 0;
			if (faces.contains(EnumFacing.DOWN)) {
				this.polygons[i++] = new Polygon(new Vertex[] {v5, v4, v0, v1}, u1, t0, u2, t1, texWidth, texHeight, mirror, EnumFacing.DOWN);
			}
			if (faces.contains(EnumFacing.UP)) {
				this.polygons[i++] = new Polygon(new Vertex[] {v2, v3, v7, v6}, u2, t1, u3, t0, texWidth, texHeight, mirror, EnumFacing.UP);
			}
			if (faces.contains(EnumFacing.WEST)) {
				this.polygons[i++] = new Polygon(new Vertex[] {v0, v4, v7, v3}, u0, t1, u1, t2, texWidth, texHeight, mirror, EnumFacing.WEST);
			}
			if (faces.contains(EnumFacing.NORTH)) {
				this.polygons[i++] = new Polygon(new Vertex[] {v1, v0, v3, v2}, u1, t1, u2, t2, texWidth, texHeight, mirror, EnumFacing.NORTH);
			}
			if (faces.contains(EnumFacing.EAST)) {
				this.polygons[i++] = new Polygon(new Vertex[] {v5, v1, v2, v6}, u2, t1, u4, t2, texWidth, texHeight, mirror, EnumFacing.EAST);
			}
			if (faces.contains(EnumFacing.SOUTH)) {
				this.polygons[i] = new Polygon(new Vertex[] {v4, v5, v6, v7}, u4, t1, u5, t2, texWidth, texHeight, mirror, EnumFacing.SOUTH);
			}
		}

		/** Its faces into a 1.21 vertex consumer, in the current matrix (positions in pixels, as 1.21 has them). */
		public void compile(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
			for (Polygon polygon : this.polygons) {
				if (polygon == null) {
					continue;
				}
				for (Vertex vertex : polygon.vertices) {
					buffer.addVertex(vertex.pos.x() / 16.0F, vertex.pos.y() / 16.0F, vertex.pos.z() / 16.0F, color, vertex.u, vertex.v, overlay, light,
							polygon.normal.x(), polygon.normal.y(), polygon.normal.z());
				}
			}
		}

		/** Its faces into a 1.12.2 buffer of position, texture and normal (what ModelBox draws). */
		void draw(BufferBuilder buffer, float scale) {
			for (Polygon polygon : this.polygons) {
				if (polygon == null) {
					continue;
				}
				for (Vertex vertex : polygon.vertices) {
					buffer.pos(vertex.pos.x() * scale, vertex.pos.y() * scale, vertex.pos.z() * scale).tex(vertex.u, vertex.v)
							.normal(polygon.normal.x(), polygon.normal.y(), polygon.normal.z()).endVertex();
				}
			}
		}
	}

	/** 1.21 {@code ModelPart.Polygon}: a face, its four vertices with texture coordinates in the texture's size. */
	public static class Polygon {
		public final Vertex[] vertices;
		public final Vector3f normal;

		public Polygon(Vertex[] vertices, float u1, float v1, float u2, float v2, float texWidth, float texHeight, boolean mirror,
				EnumFacing direction) {
			vertices[0] = vertices[0].remap(u2 / texWidth, v1 / texHeight);
			vertices[1] = vertices[1].remap(u1 / texWidth, v1 / texHeight);
			vertices[2] = vertices[2].remap(u1 / texWidth, v2 / texHeight);
			vertices[3] = vertices[3].remap(u2 / texWidth, v2 / texHeight);
			if (mirror) {
				for (int i = 0, j = vertices.length - 1; i < j; i++, j--) {
					Vertex swap = vertices[i];
					vertices[i] = vertices[j];
					vertices[j] = swap;
				}
			}
			this.vertices = vertices;
			this.normal = new Vector3f(direction.getXOffset(), direction.getYOffset(), direction.getZOffset());
			if (mirror) {
				this.normal.mul(-1.0F, 1.0F, 1.0F);
			}
		}
	}

	/** 1.21 {@code ModelPart.Vertex}. */
	public static class Vertex {
		public final Vector3f pos;
		public final float u;
		public final float v;

		public Vertex(float x, float y, float z, float u, float v) {
			this(new Vector3f(x, y, z), u, v);
		}

		public Vertex(Vector3f pos, float u, float v) {
			this.pos = pos;
			this.u = u;
			this.v = v;
		}

		public Vertex remap(float u, float v) {
			return new Vertex(this.pos, u, v);
		}
	}

	/** Parts in a list, for 1.12.2 code that wants them so. */
	public List<ModelPart> childList() {
		return new ArrayList<>(this.children.values());
	}
}
