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

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The built vertices of a buffer (1.21 {@code MeshData}, neoforge-1.21.1-runtime.tsv): the 1.12.2 buffer still
 * drawing, drawn by {@link BufferUploader}, or closed unused.
 */
@SideOnly(Side.CLIENT)
public final class MeshData implements AutoCloseable {
	private final BufferBuilder buffer;
	private final VertexConsumer consumer;
	private boolean done;

	MeshData(BufferBuilder buffer, VertexConsumer consumer) {
		this.buffer = buffer;
		this.consumer = consumer;
	}

	/** The finished buffer of the mesh for VertexBuffer.upload (its last vertex written), null if it was drawn or closed. */
	@javax.annotation.Nullable
	BufferBuilder finishForUpload() {
		if (this.done) {
			return null;
		}
		this.done = true;
		this.consumer.finishTarget();
		try {
			this.buffer.finishDrawing();
		} catch (IllegalStateException notBuilding) {
			return null;
		}
		return this.buffer;
	}

	void draw() {
		if (!this.done) {
			this.done = true;
			this.consumer.drawTarget();
		}
	}

	@Override
	public void close() {
		if (!this.done) {
			this.done = true;
			this.consumer.finishTarget();
			try {
				this.buffer.finishDrawing();
			} catch (IllegalStateException notBuilding) {
				// nothing was begun
			}
		}
	}
}
