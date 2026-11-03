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
package io.github.xponer2.translator.registry;

import java.util.Optional;

import javax.annotation.Nullable;

/** 1.21 {@code RegistrationInfo} (neoforge-1.21.1-runtime.tsv): where a registry entry came from; nothing here uses it. */
public final class RegistrationInfo {
    public static final RegistrationInfo BUILT_IN = new RegistrationInfo(Optional.empty(), null);

    private final Optional<?> knownPackInfo;
    @Nullable
    private final Object lifecycle;

    public RegistrationInfo(Optional<?> knownPackInfo, @Nullable Object lifecycle) {
        this.knownPackInfo = knownPackInfo;
        this.lifecycle = lifecycle;
    }

    public Optional<?> knownPackInfo() {
        return this.knownPackInfo;
    }

    @Nullable
    public Object lifecycle() {
        return this.lifecycle;
    }
}
