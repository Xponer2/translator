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

/**
 * 1.21 {@code OutlineBufferSource}: the buffers a glowing entity draws into for its outline. 1.12.2 draws the outlines
 * of glowing entities itself (its entity outline pass draws the entity again), so this is an immediate buffer source
 * like the others. GeckoLib's armor renderer asks for it; without it the armor of every GeckoLib mob was skipped and
 * the mob's whole render failed (Iron's Spells' necromancers and wizards: only their shadow showed).
 */
public class OutlineBufferSource extends MultiBufferSource {
	static final OutlineBufferSource INSTANCE = new OutlineBufferSource(null);

	public OutlineBufferSource(@javax.annotation.Nullable MultiBufferSource bufferSource) {
		super(null, null);
	}

	/** 1.21 {@code setColor(r, g, b, a)}: the outline colour, 1.12.2's team colour draws it. */
	public void setColor(int red, int green, int blue, int alpha) {
	}

	/** 1.21 {@code endOutlineBatch()}. */
	public void endOutlineBatch() {
		this.endBatch();
	}
}
