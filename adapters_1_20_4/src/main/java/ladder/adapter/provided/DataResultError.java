/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

import java.util.Optional;

/** DFU 8's {@code DataResult.Error}, made from a DFU 6 result's error (ladder.adapter.dfu.DfuAdapters). */
public final class DataResultError<R> {
    private final String message;
    private final Optional<R> partialValue;

    public DataResultError(String message, Optional<R> partialValue) {
        this.message = message;
        this.partialValue = partialValue;
    }

    public String message() {
        return this.message;
    }

    public Optional<R> partialValue() {
        return this.partialValue;
    }

    @Override
    public String toString() {
        return "DataResult.Error[" + this.message + "]";
    }
}
