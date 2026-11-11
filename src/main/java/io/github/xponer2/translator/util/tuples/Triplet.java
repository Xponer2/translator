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
package io.github.xponer2.translator.util.tuples;

/**
 * OSHI's {@code oshi.util.tuples.Triplet}, which 1.21.1 ships and some mods use as a plain tuple (Ars Nouveau's perks
 * and rituals, the Aether): 1.12.2 has no such class (the mods' classes using it didn't load). The table maps it here.
 */
public class Triplet<A, B, C> {
	private final A a;
	private final B b;
	private final C c;

	public Triplet(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public final A getA() {
		return this.a;
	}

	public final B getB() {
		return this.b;
	}

	public final C getC() {
		return this.c;
	}

	@Override
	public String toString() {
		return "(" + this.a + ", " + this.b + ", " + this.c + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Triplet)) {
			return false;
		}
		Triplet<?, ?, ?> other = (Triplet<?, ?, ?>) obj;
		return java.util.Objects.equals(this.a, other.a) && java.util.Objects.equals(this.b, other.b) && java.util.Objects.equals(this.c, other.c);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(this.a, this.b, this.c);
	}
}
