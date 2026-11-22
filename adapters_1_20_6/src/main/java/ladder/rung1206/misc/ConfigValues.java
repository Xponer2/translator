/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import com.electronwill.nightconfig.core.CommentedConfig;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge 21's members of a config value and of its own client config on 20.6 (the F and X rows' adapters): NeoForge
 * 21's configuration screen, carried down, reads them.
 */
public final class ConfigValues {
    private ConfigValues() {
    }

    /** 21's {@code value.getRaw()}: the value as the config file has it (20.6's get, which rereads it after a set) */
    public static Object getRaw(ModConfigSpec.ConfigValue<?> value) {
        return value.get();
    }

    /** 21's {@code value.getSpec()}: its spec, by its path in its config's spec */
    public static ModConfigSpec.ValueSpec getSpec(ModConfigSpec.ConfigValue<?> value) {
        ModConfigSpec spec = specOf(value);
        return spec == null ? null : spec.getSpec().get(value.getPath());
    }

    private static java.lang.reflect.Field specField;

    /** the value's config spec (a private field below: public at run time by the adapters' access transformer) */
    private static ModConfigSpec specOf(ModConfigSpec.ConfigValue<?> value) {
        try {
            if (specField == null) {
                java.lang.reflect.Field f = ModConfigSpec.ConfigValue.class.getDeclaredField("spec");
                f.trySetAccessible();
                specField = f;
            }
            return (ModConfigSpec) specField.get(value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** 21's {@code spec.isEmpty()}: a config without values */
    public static boolean isEmpty(ModConfigSpec spec) {
        return spec.getSpec().isEmpty();
    }

    private static final ModConfigSpec.BooleanValue LOG_UNTRANSLATED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        LOG_UNTRANSLATED = builder.define("logUntranslatedConfigurationWarnings", false);
        ModConfigSpec spec = builder.build();
        CommentedConfig config = CommentedConfig.inMemory();
        spec.correct(config);
        spec.acceptConfig(config);
    }

    /**
     * NeoForge 21's client option {@code logUntranslatedConfigurationWarnings} (20.6's client config hasn't it): off, as
     * 21 has it by default, in a config of its own that is always loaded
     */
    public static ModConfigSpec.BooleanValue logUntranslatedConfigurationWarnings(net.neoforged.neoforge.common.NeoForgeConfig.Client clientConfig) {
        return LOG_UNTRANSLATED;
    }
}
