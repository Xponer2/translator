/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.shape;

/** 1.21 {@code BooleanOp}: how {@link Shapes#join} combines two shapes. */
@FunctionalInterface
public interface BooleanOp {
	BooleanOp FALSE = (a, b) -> false;
	BooleanOp NOT_OR = (a, b) -> !a && !b;
	BooleanOp ONLY_SECOND = (a, b) -> b && !a;
	BooleanOp NOT_FIRST = (a, b) -> !a;
	BooleanOp ONLY_FIRST = (a, b) -> a && !b;
	BooleanOp NOT_SECOND = (a, b) -> !b;
	BooleanOp NOT_SAME = (a, b) -> a != b;
	BooleanOp NOT_AND = (a, b) -> !a || !b;
	BooleanOp AND = (a, b) -> a && b;
	BooleanOp SAME = (a, b) -> a == b;
	BooleanOp SECOND = (a, b) -> b;
	BooleanOp CAUSES = (a, b) -> !a || b;
	BooleanOp FIRST = (a, b) -> a;
	BooleanOp CAUSED_BY = (a, b) -> a || !b;
	BooleanOp OR = (a, b) -> a || b;
	BooleanOp TRUE = (a, b) -> true;

	boolean apply(boolean first, boolean second);
}
