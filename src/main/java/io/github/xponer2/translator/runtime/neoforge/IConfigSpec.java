/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;

/**
 * FML IConfigSpec: a config spec the loader fills from a file. The NeoForge ModConfigSpec of an original 1.21.1 mod
 * (carried with the API) implements it; the Translator loads its file (ConfigLoader).
 */
public interface IConfigSpec {
	boolean isEmpty();

	void validateSpec(ModConfig config);

	boolean isCorrect(UnmodifiableCommentedConfig config);

	void correct(CommentedConfig config);

	void acceptConfig(ILoadedConfig config);

	interface ILoadedConfig {
		CommentedConfig config();

		void save();
	}
}
