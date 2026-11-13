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
package io.github.xponer2.translator.runtime.redirect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.ITransformation;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The 1.21 unbaked block model on 1.12.2's (R rows of ModelBlock, BlockPart, BlockPartFace and BlockPartRotation):
 * {@code blockModel.bake(baker, sprites, state)} bakes its elements with 1.12.2's face baker (the mods' own model
 * loaders bake their parts with it: Mekanism's, Sophisticated Storage's barrels, Twilight Forest's, Supplementaries',
 * Fusion's), NeoForge's {@code customData} of a model, and the 1.21 records of an element, its faces and its rotation
 * (1.21's from/to are JOML vectors, 1.12.2's LWJGL ones).
 */
@SideOnly(Side.CLIENT)
public final class ModelBakeBridges {
	private static final FaceBakery FACE_BAKERY = new FaceBakery();
	private static final Map<ModelBlock, Object> CUSTOM_DATA = Collections.synchronizedMap(new WeakHashMap<>());
	@Nullable
	private static Constructor<?> material;
	@Nullable
	private static Constructor<?> customData;

	private ModelBakeBridges() {
	}

	// --- baking ------------------------------------------------------------------------------------------------------

	/** 1.21 {@code blockModel.bake(baker, sprites, state)}. */
	public static IBakedModel bake(ModelBlock model, @Nullable Object baker, @Nullable Function<Object, TextureAtlasSprite> sprites, @Nullable Object state) {
		return bake(model, baker, model, sprites, state, true);
	}

	/**
	 * 1.21 {@code blockModel.bake(baker, owner, sprites, state, gui3d)}: each face of each element baked with the
	 * state's rotation (1.12.2's ModelBakery does the same for its own models), the textures resolved through the
	 * model's parents and taken from the mod's sprite getter (the blocks atlas when it has none).
	 */
	public static IBakedModel bake(ModelBlock model, @Nullable Object baker, @Nullable ModelBlock owner, @Nullable Function<Object, TextureAtlasSprite> sprites,
			@Nullable Object state, boolean gui3d) {
		ITransformation rotation = transformation(state);
		boolean uvLocked = uvLocked(state);
		ModelBlock textures = owner != null ? owner : model;
		SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(textures, textures.createOverrides()).setTexture(sprite(textures, "particle", sprites));
		for (BlockPart part : model.getElements()) {
			for (Map.Entry<EnumFacing, BlockPartFace> entry : part.mapFaces.entrySet()) {
				BlockPartFace face = entry.getValue();
				TextureAtlasSprite sprite = sprite(textures, face.texture, sprites);
				BakedQuad quad = FACE_BAKERY.makeBakedQuad(part.positionFrom, part.positionTo, face, sprite, entry.getKey(), rotation, part.partRotation, uvLocked,
						part.shade);
				if (face.cullFace == null) {
					builder.addGeneralQuad(quad);
				} else {
					builder.addFaceQuad(rotation.rotate(face.cullFace), quad);
				}
			}
		}
		return builder.makeBakedModel();
	}

	/** The sprite of a texture name ("#side" resolved through the parents), the missing one when there is none. */
	private static TextureAtlasSprite sprite(ModelBlock model, String name, @Nullable Function<Object, TextureAtlasSprite> sprites) {
		TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
		String location = model.resolveTextureName(name);
		if (location == null || location.isEmpty() || location.startsWith("#") || "missingno".equals(location)) {
			return atlas.getMissingSprite();
		}
		ResourceLocation texture = new ResourceLocation(location);
		if (sprites != null) {
			try {
				TextureAtlasSprite sprite = sprites.apply(newMaterial(TextureMap.LOCATION_BLOCKS_TEXTURE, texture));
				if (sprite != null) {
					return sprite;
				}
			} catch (RuntimeException | LinkageError e) {
				// the atlas's own below
			}
		}
		return atlas.getAtlasSprite(texture.toString());
	}

	/** A 1.21 Material (carried) of an atlas and a texture: what the mods' sprite getters take. */
	public static Object newMaterial(ResourceLocation atlas, ResourceLocation texture) {
		try {
			if (material == null) {
				material = Class.forName("net.minecraft.client.resources.model.Material", true, ModelBakeBridges.class.getClassLoader())
						.getConstructor(ResourceLocation.class, ResourceLocation.class);
			}
			return material.newInstance(atlas, texture);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("no 1.21 Material", e);
		}
	}

	/**
	 * The rotation of a 1.21 ModelState: a 1.12.2 one (BlockModelRotation is ModelRotation) as it is, else its 1.21
	 * transformation's matrix as Forge's; none when it gives nothing.
	 */
	static ITransformation transformation(@Nullable Object state) {
		if (state instanceof ITransformation transformation) {
			return transformation;
		}
		if (state != null) {
			try {
				Object rotation = method(state.getClass(), "getRotation").invoke(state);
				if (rotation instanceof ITransformation transformation) {
					return transformation;
				}
				if (rotation != null && method(rotation.getClass(), "getMatrix").invoke(rotation) instanceof org.joml.Matrix4f matrix) {
					if ((matrix.properties() & org.joml.Matrix4fc.PROPERTY_IDENTITY) != 0 || matrix.equals(new org.joml.Matrix4f())) {
						return ModelRotation.X0_Y0;
					}
					// vecmath takes rows; JOML gives columns: those of the transposed matrix are the rows
					float[] values = new float[16];
					matrix.transpose(new org.joml.Matrix4f()).get(values);
					return new TRSRTransformation(new javax.vecmath.Matrix4f(values));
				}
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				// no rotation
			}
		}
		return ModelRotation.X0_Y0;
	}

	static boolean uvLocked(@Nullable Object state) {
		if (state instanceof ModelRotation || state == null) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(method(state.getClass(), "isUvLocked").invoke(state));
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return false;
		}
	}

	private static Method method(Class<?> type, String name) throws NoSuchMethodException {
		Method method = type.getMethod(name);
		method.setAccessible(true);
		return method;
	}

	/**
	 * 1.21 {@code blockModel.getGuiLight()}: how the model is lit in a GUI (the carried BlockModel.GuiLight): front
	 * lit for a model whose json says "gui_light": "front" (flat items), from the side for a block (Twilight Forest's
	 * noise-varying aurora block asks its variants so while baking).
	 */
	@javax.annotation.Nullable
	public static Object getGuiLight(ModelBlock model) {
		try {
			Class<?> light = Class.forName("net.minecraft.client.renderer.block.model.BlockModel$GuiLight", true, ModelBakeBridges.class.getClassLoader());
			return light.getField(model.isGui3d() ? "SIDE" : "FRONT").get(null);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/**
	 * NeoForge's {@code blockModel.customData}: the model's geometry baking context (its custom geometry, render type
	 * and visible parts; carried), one per model.
	 */
	@Nullable
	public static Object customData(ModelBlock model) {
		return CUSTOM_DATA.computeIfAbsent(model, key -> {
			try {
				if (customData == null) {
					customData = Class.forName("net.neoforged.neoforge.client.model.geometry.BlockGeometryBakingContext", true,
							ModelBakeBridges.class.getClassLoader()).getConstructor(ModelBlock.class);
				}
				return customData.newInstance(key);
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				return null;
			}
		});
	}

	// --- the element and its faces -----------------------------------------------------------------------------------

	private static org.lwjgl.util.vector.Vector3f lwjgl(@Nullable org.joml.Vector3f vector) {
		return vector == null ? new org.lwjgl.util.vector.Vector3f() : new org.lwjgl.util.vector.Vector3f(vector.x, vector.y, vector.z);
	}

	private static org.joml.Vector3f joml(@Nullable org.lwjgl.util.vector.Vector3f vector) {
		return vector == null ? new org.joml.Vector3f() : new org.joml.Vector3f(vector.x, vector.y, vector.z);
	}

	/** 1.21 {@code new BlockElement(from, to, faces, rotation, shade)}. */
	@SuppressWarnings("unchecked")
	public static BlockPart newBlockPart(org.joml.Vector3f from, org.joml.Vector3f to, Map<?, ?> faces, @Nullable BlockPartRotation rotation, boolean shade) {
		Map<EnumFacing, BlockPartFace> own = new EnumMap<>(EnumFacing.class);
		for (Map.Entry<?, ?> entry : faces.entrySet()) {
			if (entry.getKey() instanceof EnumFacing side && entry.getValue() instanceof BlockPartFace face) {
				own.put(side, face);
			}
		}
		return new BlockPart(lwjgl(from), lwjgl(to), own, rotation, shade);
	}

	/** NeoForge's {@code new BlockElement(from, to, faces, rotation, shade, faceData)}: the face data (emission, color) isn't kept. */
	public static BlockPart newBlockPart(org.joml.Vector3f from, org.joml.Vector3f to, Map<?, ?> faces, @Nullable BlockPartRotation rotation, boolean shade,
			@Nullable Object faceData) {
		return newBlockPart(from, to, faces, rotation, shade);
	}

	/** 1.21 {@code element.from} / {@code element.to} (copies: JOML vectors). */
	public static org.joml.Vector3f from(BlockPart part) {
		return joml(part.positionFrom);
	}

	public static org.joml.Vector3f to(BlockPart part) {
		return joml(part.positionTo);
	}

	public static org.joml.Vector3f positionFrom(BlockPart part) {
		return joml(part.positionFrom);
	}

	public static org.joml.Vector3f positionTo(BlockPart part) {
		return joml(part.positionTo);
	}

	/** NeoForge {@code element.getFaceData()}: the default (1.12.2 elements have none). */
	@Nullable
	public static Object getFaceData(BlockPart part) {
		return defaultFaceData();
	}

	@Nullable
	private static Object defaultFaceData() {
		try {
			return Class.forName("net.neoforged.neoforge.client.model.ExtraFaceData", true, ModelBakeBridges.class.getClassLoader()).getField("DEFAULT").get(null);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** 1.21 {@code new BlockElementFace(cullForDirection, tintIndex, texture, uv)} (1.12.2's BlockPartFace). */
	public static BlockPartFace newBlockPartFace(@Nullable EnumFacing cull, int tintIndex, String texture, BlockFaceUV uv) {
		return new BlockPartFace(cull, tintIndex, texture, uv);
	}

	/** NeoForge's with the face data and the parent element: neither kept. */
	public static BlockPartFace newBlockPartFace(@Nullable EnumFacing cull, int tintIndex, String texture, BlockFaceUV uv, @Nullable Object faceData,
			@Nullable Object parent) {
		return new BlockPartFace(cull, tintIndex, texture, uv);
	}

	/** The 1.21 record's accessors. */
	@Nullable
	public static EnumFacing cullForDirection(BlockPartFace face) {
		return face.cullFace;
	}

	public static int tintIndex(BlockPartFace face) {
		return face.tintIndex;
	}

	public static String texture(BlockPartFace face) {
		return face.texture;
	}

	public static BlockFaceUV uv(BlockPartFace face) {
		return face.blockFaceUV;
	}

	@Nullable
	public static Object faceData(BlockPartFace face) {
		return defaultFaceData();
	}

	public static org.apache.commons.lang3.mutable.MutableObject<BlockPart> parent(BlockPartFace face) {
		return new org.apache.commons.lang3.mutable.MutableObject<>();
	}

	/** 1.21 {@code new BlockElementRotation(origin, axis, angle, rescale)} (1.12.2's BlockPartRotation). */
	public static BlockPartRotation newBlockPartRotation(org.joml.Vector3f origin, EnumFacing.Axis axis, float angle, boolean rescale) {
		return new BlockPartRotation(lwjgl(origin), axis, angle, rescale);
	}

	public static org.joml.Vector3f origin(BlockPartRotation rotation) {
		return joml(rotation.origin);
	}

	public static EnumFacing.Axis axis(BlockPartRotation rotation) {
		return rotation.axis;
	}

	public static float angle(BlockPartRotation rotation) {
		return rotation.angle;
	}

	public static boolean rescale(BlockPartRotation rotation) {
		return rotation.rescale;
	}
}
