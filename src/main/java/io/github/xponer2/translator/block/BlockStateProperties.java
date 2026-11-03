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
package io.github.xponer2.translator.block;

import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.util.EnumFacing;

/**
 * 1.21 {@code BlockStateProperties}: the vanilla property constants, the 1.12.2 property where vanilla has one (so
 * the states match vanilla blocks), else a property of the same name and values. WATERLOGGED is on no 1.12.2 block
 * (there is no waterlogging): {@code state.hasProperty(WATERLOGGED)} is false.
 */
public final class BlockStateProperties {
	public static final PropertyDirection HORIZONTAL_FACING = BlockHorizontal.FACING;
	public static final PropertyDirection FACING = BlockDirectional.FACING;
	public static final PropertyDirection FACING_HOPPER = PropertyDirection.create("facing", facing -> facing != EnumFacing.UP);
	public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");
	public static final PropertyBool POWERED = PropertyBool.create("powered");
	public static final PropertyBool LIT = PropertyBool.create("lit");
	public static final PropertyBool OPEN = PropertyBool.create("open");
	public static final PropertyBool ENABLED = PropertyBool.create("enabled");
	public static final PropertyBool TRIGGERED = PropertyBool.create("triggered");
	public static final PropertyBool ATTACHED = PropertyBool.create("attached");
	public static final PropertyBool HANGING = PropertyBool.create("hanging");
	public static final PropertyBool PERSISTENT = PropertyBool.create("persistent");
	public static final PropertyBool UP = PropertyBool.create("up");
	public static final PropertyBool DOWN = PropertyBool.create("down");
	public static final PropertyBool NORTH = PropertyBool.create("north");
	public static final PropertyBool EAST = PropertyBool.create("east");
	public static final PropertyBool SOUTH = PropertyBool.create("south");
	public static final PropertyBool WEST = PropertyBool.create("west");
	public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class);
	public static final PropertyEnum<EnumFacing.Axis> HORIZONTAL_AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class,
			EnumFacing.Axis.X, EnumFacing.Axis.Z);
	public static final PropertyInteger AGE_1 = PropertyInteger.create("age", 0, 1);
	public static final PropertyInteger AGE_2 = PropertyInteger.create("age", 0, 2);
	public static final PropertyInteger AGE_3 = PropertyInteger.create("age", 0, 3);
	public static final PropertyInteger AGE_5 = PropertyInteger.create("age", 0, 5);
	public static final PropertyInteger AGE_7 = PropertyInteger.create("age", 0, 7);
	public static final PropertyInteger AGE_15 = PropertyInteger.create("age", 0, 15);
	public static final PropertyInteger AGE_25 = PropertyInteger.create("age", 0, 25);
	public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 15);
	public static final PropertyInteger POWER = PropertyInteger.create("power", 0, 15);
	public static final PropertyInteger ROTATION_16 = PropertyInteger.create("rotation", 0, 15);
	public static final PropertyInteger DISTANCE = PropertyInteger.create("distance", 1, 7);
	public static final PropertyEnum<DoubleBlockHalf> DOUBLE_BLOCK_HALF = PropertyEnum.create("half", DoubleBlockHalf.class);

	private BlockStateProperties() {
	}
}
