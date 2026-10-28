package io.github.xponer2.translator.registry;

/**
 * The registries of a world or server (1.21 {@code RegistryAccess}): in 1.12.2 always the same, {@link #INSTANCE}.
 */
public final class RegistryAccess implements HolderLookup.Provider {
    public static final RegistryAccess INSTANCE = new RegistryAccess();
    public static final RegistryAccess EMPTY = INSTANCE;

    private RegistryAccess() { }
}
