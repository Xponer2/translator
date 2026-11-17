/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testa;

import java.util.Map;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * DFU 8's API as 1.21 mods use it: a dispatch codec whose types give map codecs, results read with getOrThrow,
 * isSuccess, error().map(Error::message), validate, lenientOptionalFieldOf. Run natively (DFU 8) and converted (DFU 6).
 */
final class DfuTest {
    record Shape(String kind, int size) {
    }

    static final MapCodec<Shape> SQUARE = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("side").forGetter(Shape::size)).apply(i, side -> new Shape("square", side)));
    static final MapCodec<Shape> CIRCLE = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.lenientOptionalFieldOf("radius", 7).forGetter(Shape::size)).apply(i, r -> new Shape("circle", r)));
    static final Codec<Shape> SHAPE = Codec.STRING.dispatch("kind", Shape::kind, k -> k.equals("square") ? SQUARE : CIRCLE);
    static final Codec<Integer> SMALL = Codec.INT.validate(v -> v < 10 ? DataResult.success(v) : DataResult.error(() -> "too big: " + v));

    private DfuTest() {
    }

    static void run(Map<String, String> results) {
        JsonElement square = SHAPE.encodeStart(JsonOps.INSTANCE, new Shape("square", 3)).getOrThrow();
        results.put("dfuEncoded", square.toString());
        Shape back = SHAPE.parse(JsonOps.INSTANCE, square).getOrThrow();
        results.put("dfuDecoded", back.toString());
        Shape circle = SHAPE.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("{\"kind\":\"circle\"}")).getOrThrow();
        results.put("dfuDefault", circle.toString());
        DataResult<Integer> bad = SMALL.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("12"));
        results.put("dfuValidateSuccess", String.valueOf(bad.isSuccess()));
        results.put("dfuValidateMessage", bad.error().map(DataResult.Error::message).orElse("none"));
        results.put("dfuGood", String.valueOf(SMALL.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("4")).getOrThrow()));
    }
}
