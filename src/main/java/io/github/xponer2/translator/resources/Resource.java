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
package io.github.xponer2.translator.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * A file of a resource pack (1.21 {@code Resource}, neoforge-1.21.1-runtime.tsv): opened from the 1.12.2 resource
 * that holds it.
 */
public class Resource {
	private final String packId;
	private final Opener opener;

	/** Opens the file again for each read. */
	public interface Opener {
		InputStream open() throws IOException;
	}

	public Resource(String packId, Opener opener) {
		this.packId = packId;
		this.opener = opener;
	}

	public InputStream open() throws IOException {
		return this.opener.open();
	}

	public BufferedReader openAsReader() throws IOException {
		return new BufferedReader(new InputStreamReader(this.open(), StandardCharsets.UTF_8));
	}

	public String sourcePackId() {
		return this.packId;
	}

	public boolean isBuiltin() {
		return false;
	}

	@Override
	public String toString() {
		return "Resource[" + this.packId + "]";
	}
}
