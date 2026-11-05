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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ImmutableMap;

import io.github.xponer2.translator.block.BlockEntity;
import io.github.xponer2.translator.client.ModelData;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The block entity's NeoForge model data for the block's model (1.21 asks the block entity, then the model, and
 * hands the data to getQuads): 1.12.2's extended state, which the renderer passes to getQuads, carries it.
 */
@SideOnly(Side.CLIENT)
public final class ModelDataState {
	private ModelDataState() {
	}

	/** The state with the model data of the block entity there (the state itself when there is none). */
	public static IBlockState extend(IBlockState state, IBlockAccess world, BlockPos pos) {
		TileEntity tile;
		try {
			tile = world.getTileEntity(pos);
		} catch (RuntimeException e) {
			return state;
		}
		if (!(tile instanceof BlockEntity entity)) {
			return state;
		}
		ModelData data = entity.getModelData();
		try {
			var model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
			if (model instanceof BakedModelBridge bridge) {
				data = bridge.getModelData(world, pos, state, data == null ? ModelData.EMPTY : data);
			}
		} catch (RuntimeException | LinkageError e) {
			// the block entity's data as it is
		}
		return data == null || data == ModelData.EMPTY ? state : wrap(state, data);
	}

	private static IBlockState wrap(IBlockState state, ModelData data) {
		Collection<IUnlistedProperty<?>> names = Collections.singletonList(ModelDataProperty.INSTANCE);
		return (IBlockState) Proxy.newProxyInstance(IExtendedBlockState.class.getClassLoader(), new Class<?>[] {IExtendedBlockState.class},
				(proxy, method, args) -> {
					switch (method.getName()) {
						case "getValue":
							if (args != null && args.length == 1 && args[0] == ModelDataProperty.INSTANCE) {
								return data;
							}
							if (args != null && args.length == 1 && args[0] instanceof IUnlistedProperty<?>) {
								return state instanceof IExtendedBlockState extended && extended.getUnlistedNames().contains(args[0])
										? extended.getValue((IUnlistedProperty<?>) args[0]) : null;
							}
							break;
						case "getUnlistedNames":
							return names;
						case "getUnlistedProperties":
							return ImmutableMap.of(ModelDataProperty.INSTANCE, java.util.Optional.of(data));
						case "getClean":
							return state instanceof IExtendedBlockState extended ? extended.getClean() : state;
						case "withProperty":
							if (args != null && args.length == 2 && args[0] instanceof IUnlistedProperty<?>) {
								return proxy;
							}
							break;
						case "equals":
							return args[0] == proxy || state.equals(args[0]);
						case "hashCode":
							return state.hashCode();
						case "toString":
							return state.toString();
						default:
							break;
					}
					try {
						return method.invoke(state, args);
					} catch (InvocationTargetException e) {
						throw e.getCause();
					}
				});
	}
}
