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

import java.lang.reflect.Constructor;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The fields of a 1.21 {@code BlockElement} (a model's cube) read on the 1.12.2 {@link BlockPart}: 1.21 keeps the
 * corners as JOML vectors and the rotation as a record, 1.12.2 as LWJGL vectors and a BlockPartRotation, so the names
 * can't simply be renamed. Mods that bake models from the cubes read them (Sophisticated Backpacks' backpack model
 * computes its shape from from/to/rotation: they read as null and the backpack was invisible).
 */
@SideOnly(Side.CLIENT)
public final class BlockElementFields {
	@Nullable
	private static volatile Constructor<?> rotationRecord;

	private BlockElementFields() {
	}

	/** 1.21 {@code element.from}: the cube's lower corner. */
	public static Vector3f from(BlockPart part) {
		return new Vector3f(part.positionFrom.x, part.positionFrom.y, part.positionFrom.z);
	}

	/** 1.21 {@code element.to}: the cube's upper corner. */
	public static Vector3f to(BlockPart part) {
		return new Vector3f(part.positionTo.x, part.positionTo.y, part.positionTo.z);
	}

	/** 1.21 {@code element.shade}. */
	public static boolean shade(BlockPart part) {
		return part.shade;
	}

	/**
	 * 1.21 {@code element.rotation}: the carried 1.21 record (origin, axis, angle, rescale), null when the cube isn't
	 * rotated, as in 1.21.
	 */
	@Nullable
	public static Object rotation(BlockPart part) {
		BlockPartRotation rotation = part.partRotation;
		if (rotation == null) {
			return null;
		}
		try {
			Constructor<?> constructor = rotationRecord;
			if (constructor == null) {
				Class<?> type = Class.forName("net.minecraft.client.renderer.block.model.BlockElementRotation", true,
						net.minecraft.launchwrapper.Launch.classLoader);
				for (Constructor<?> candidate : type.getDeclaredConstructors()) {
					if (candidate.getParameterCount() == 4) {
						candidate.setAccessible(true);
						constructor = candidate;
						break;
					}
				}
				rotationRecord = constructor;
			}
			return constructor == null ? null : constructor.newInstance(
					new Vector3f(rotation.origin.x, rotation.origin.y, rotation.origin.z), rotation.axis, rotation.angle, rotation.rescale);
		} catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
			return null;
		}
	}
}
