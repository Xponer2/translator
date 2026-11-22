/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;

/**
 * DataFixerUpper 8's {@code DataFixerBuilder.Result} (1.21) on DataFixerUpper 7 (1.20.6), whose builder builds the fixer
 * itself, optimized or not: the result builds it on first use, optimized for the types asked when optimize comes first.
 */
public final class DataFixerResult {
    private final DataFixerBuilder builder;
    private DataFixer fixer;

    private DataFixerResult(DataFixerBuilder builder) {
        this.builder = builder;
    }

    /** DFU 8's {@code builder.build()} */
    public static DataFixerResult build(DataFixerBuilder builder) {
        return new DataFixerResult(builder);
    }

    public synchronized DataFixer fixer() {
        if (this.fixer == null) {
            this.fixer = this.builder.buildUnoptimized();
        }
        return this.fixer;
    }

    /** (DFU 7 optimizes in the background on its own, from the executor; a fixer built already stays as it is) */
    public synchronized CompletableFuture<?> optimize(Set<DSL.TypeReference> types, Executor executor) {
        if (this.fixer == null) {
            this.fixer = this.builder.buildOptimized(types, executor);
        }
        return CompletableFuture.completedFuture(null);
    }
}
