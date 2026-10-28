package io.github.xponer2.translator.client;

/**
 * Extra model data (1.21 NeoForge {@code ModelData}); 1.12.2 models get extended block states instead. Only
 * {@link #EMPTY} is used.
 */
public final class ModelData {
    public static final ModelData EMPTY = new ModelData();

    private ModelData() { }
}
