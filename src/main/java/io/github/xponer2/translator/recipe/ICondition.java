package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.registry.NeoForgeRegistries;
import io.github.xponer2.translator.util.TagKey;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A condition of a data file (1.21 NeoForge {@code ICondition}): a recipe whose {@code "neoforge:conditions"} don't
 * all hold isn't loaded. Condition types are the {@code MapCodec}s in {@link NeoForgeRegistries#CONDITION_SERIALIZERS}
 * under their {@code "type"} id; the NeoForge ones are in {@link NeoForgeConditions}.
 */
public interface ICondition {
    Codec<ICondition> CODEC = new Codec<ICondition>() {
        @Override
        public <T> DataResult<Pair<ICondition, T>> decode(DynamicOps<T> ops, T input) {
            return ops.get(input, "type").flatMap(ops::getStringValue).flatMap(type -> {
                NeoForgeConditions.bootstrap();
                ResourceLocation id;
                try {
                    id = new ResourceLocation(type);
                } catch (RuntimeException e) {
                    return DataResult.error("Not a valid condition type: " + type);
                }
                var codec = NeoForgeRegistries.CONDITION_SERIALIZERS.get(id);
                if (codec == null) {
                    return DataResult.error("Unknown condition type: " + type);
                }
                return codec.codec().decode(ops, input).map(pair -> pair.mapFirst(c -> (ICondition) c));
            });
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T> DataResult<T> encode(ICondition input, DynamicOps<T> ops, T prefix) {
            var id = NeoForgeRegistries.CONDITION_SERIALIZERS.getKey(input.codec());
            if (id == null) {
                return DataResult.error("Unregistered condition type: " + input);
            }
            var codec = (MapCodec) input.codec();
            DataResult<T> encoded = codec.codec().encode(input, ops, prefix);
            return encoded.flatMap(map -> ops.mergeToMap(map, ops.createString("type"), ops.createString(id.toString())));
        }
    };
    Codec<List<ICondition>> LIST_CODEC = CODEC.listOf();

    boolean test(IContext context);

    MapCodec<? extends ICondition> codec();

    /**
     * What a condition may ask while the data is loaded.
     */
    interface IContext {
        IContext EMPTY = new IContext() {
            @Override
            public <T> Collection<T> getTag(TagKey<T> key) {
                return Collections.emptyList();
            }
        };

        /**
         * The values of a tag (empty if it doesn't exist).
         */
        <T> Collection<T> getTag(TagKey<T> key);
    }
}
