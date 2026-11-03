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
