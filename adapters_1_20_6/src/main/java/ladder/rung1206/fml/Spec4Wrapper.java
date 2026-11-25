/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.fml;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;

import ladder.carried_1_20_6.net.neoforged.fml.config.IConfigSpec$ILoadedConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

/**
 * FML 3's IConfigSpec over a mod's FML 4 spec (ConfigSpec4): FML 3 checks, corrects and hands over the file's data as
 * FML 4 does (isCorrect, correct, acceptConfig of the loaded config, null when unloaded); its emptiness is the spec's.
 * The spec's validateSpec runs when it is registered (FML 4 runs it then). Made only for such a spec (its mod names
 * FML 4's loaded config, so the pack carries it): ConfigAdapters.lower.
 */
public final class Spec4Wrapper extends UnmodifiableConfigWrapper<UnmodifiableConfig> implements IConfigSpec<Spec4Wrapper> {
    private final ConfigSpec4 spec;
    private ModConfig config;

    public Spec4Wrapper(ConfigSpec4 spec) {
        super(Config.inMemory());
        this.spec = spec;
    }

    /** the registered config (its saving is the loaded config's) */
    void registered(ModConfig config) {
        this.config = config;
        this.spec.validateSpec(config);
    }

    @Override
    public boolean isEmpty() {
        return this.spec.isEmpty();
    }

    @Override
    public void acceptConfig(CommentedConfig data) {
        if (data == null) {
            this.spec.acceptConfig(null);
            return;
        }
        ModConfig owner = this.config;
        this.spec.acceptConfig(new IConfigSpec$ILoadedConfig() {
            @Override
            public CommentedConfig config() {
                return data;
            }

            @Override
            public void save() {
                if (owner != null) {
                    owner.save();
                }
            }
        });
    }

    @Override
    public boolean isCorrecting() {
        return false;
    }

    @Override
    public boolean isCorrect(CommentedConfig data) {
        return this.spec.isCorrect(data);
    }

    @Override
    public int correct(CommentedConfig data) {
        if (this.spec.isCorrect(data)) {
            return 0;
        }
        this.spec.correct(data);
        return 1;
    }

    @Override
    public void afterReload() {
    }

}
