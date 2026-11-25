/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.fml;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;

import ladder.carried_1_20_6.net.neoforged.fml.config.IConfigSpec$ILoadedConfig;
import net.neoforged.fml.config.ModConfig;

/**
 * FML 4's {@code IConfigSpec} (NeoForge 21) of a mod's own config spec: a converted mod class that implements it gets
 * this interface (O rows), its methods being FML 4's. FML 3 (20.6) asks its own IConfigSpec, so such a spec is registered
 * in an FML 3 wrapper that asks these ({@link Spec4Wrapper}, ConfigAdapters' registrations).
 */
public interface ConfigSpec4 {
    boolean isEmpty();

    void validateSpec(ModConfig config);

    boolean isCorrect(UnmodifiableCommentedConfig config);

    void correct(CommentedConfig config);

    void acceptConfig(IConfigSpec$ILoadedConfig config);

    // the O rows' super calls (none in an interface's implementor: FML 4 gives no default)

    static boolean isEmpty(Object self) {
        throw new AbstractMethodError("IConfigSpec.isEmpty");
    }

    static void validateSpec(Object self, ModConfig config) {
        throw new AbstractMethodError("IConfigSpec.validateSpec");
    }

    static boolean isCorrect(Object self, UnmodifiableCommentedConfig config) {
        throw new AbstractMethodError("IConfigSpec.isCorrect");
    }

    static void correct(Object self, CommentedConfig config) {
        throw new AbstractMethodError("IConfigSpec.correct");
    }

    static void acceptConfig(Object self, IConfigSpec$ILoadedConfig config) {
        throw new AbstractMethodError("IConfigSpec.acceptConfig");
    }
}
