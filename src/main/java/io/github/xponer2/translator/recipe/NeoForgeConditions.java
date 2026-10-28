package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.registry.MappedRegistry;
import io.github.xponer2.translator.registry.NeoForgeRegistries;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Registries;
import io.github.xponer2.translator.util.TagKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import java.util.List;

/**
 * The conditions NeoForge defines (ids {@code neoforge:true}, {@code false}, {@code not}, {@code and}, {@code or},
 * {@code mod_loaded}, {@code item_exists}, {@code tag_empty}).
 */
public final class NeoForgeConditions {
    private static final Codec<ResourceLocation> ID = Codec.STRING.xmap(io.github.xponer2.translator.util.ResourceLocations::parse, ResourceLocation::toString);
    private static boolean registered;

    private NeoForgeConditions() { }

    static synchronized void bootstrap() {
        if (registered) {
            return;
        }
        registered = true;
        var registry = (MappedRegistry<MapCodec<? extends ICondition>>) NeoForgeRegistries.CONDITION_SERIALIZERS;
        register(registry, "true", True.CODEC);
        register(registry, "false", False.CODEC);
        register(registry, "not", Not.CODEC);
        register(registry, "and", And.CODEC);
        register(registry, "or", Or.CODEC);
        register(registry, "mod_loaded", ModLoaded.CODEC);
        register(registry, "item_exists", ItemExists.CODEC);
        register(registry, "tag_empty", TagEmpty.CODEC);
    }

    private static void register(MappedRegistry<MapCodec<? extends ICondition>> registry, String name, MapCodec<? extends ICondition> codec) {
        registry.register(new ResourceLocation("neoforge", name), codec);
    }

    public static final class True implements ICondition {
        public static final True INSTANCE = new True();
        public static final MapCodec<True> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public boolean test(IContext context) {
            return true;
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    public static final class False implements ICondition {
        public static final False INSTANCE = new False();
        public static final MapCodec<False> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public boolean test(IContext context) {
            return false;
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    public static final class Not implements ICondition {
        public static final MapCodec<Not> CODEC = ICondition.CODEC.fieldOf("value").xmap(Not::new, c -> c.value);
        private final ICondition value;

        public Not(ICondition value) {
            this.value = value;
        }

        @Override
        public boolean test(IContext context) {
            return !this.value.test(context);
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    public static final class And implements ICondition {
        public static final MapCodec<And> CODEC = ICondition.LIST_CODEC.fieldOf("values").xmap(And::new, c -> c.children);
        private final List<ICondition> children;

        public And(List<ICondition> children) {
            this.children = children;
        }

        @Override
        public boolean test(IContext context) {
            return this.children.stream().allMatch(c -> c.test(context));
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    public static final class Or implements ICondition {
        public static final MapCodec<Or> CODEC = ICondition.LIST_CODEC.fieldOf("values").xmap(Or::new, c -> c.children);
        private final List<ICondition> children;

        public Or(List<ICondition> children) {
            this.children = children;
        }

        @Override
        public boolean test(IContext context) {
            return this.children.stream().anyMatch(c -> c.test(context));
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    public static final class ModLoaded implements ICondition {
        public static final MapCodec<ModLoaded> CODEC = Codec.STRING.fieldOf("modid").xmap(ModLoaded::new, c -> c.modid);
        private final String modid;

        public ModLoaded(String modid) {
            this.modid = modid;
        }

        @Override
        public boolean test(IContext context) {
            return Loader.isModLoaded(this.modid);
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    public static final class ItemExists implements ICondition {
        public static final MapCodec<ItemExists> CODEC = ID.fieldOf("item").xmap(ItemExists::new, c -> c.item);
        private final ResourceLocation item;

        public ItemExists(ResourceLocation item) {
            this.item = item;
        }

        @Override
        public boolean test(IContext context) {
            return BuiltInRegistries.ITEM.containsKey(this.item);
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    public static final class TagEmpty implements ICondition {
        public static final MapCodec<TagEmpty> CODEC = ID.fieldOf("tag").xmap(TagEmpty::new, c -> c.tag.location());
        private final TagKey<Item> tag;

        public TagEmpty(ResourceLocation tag) {
            this.tag = TagKey.create(Registries.ITEM, tag);
        }

        public TagEmpty(TagKey<Item> tag) {
            this.tag = tag;
        }

        @Override
        public boolean test(IContext context) {
            return context.getTag(this.tag).isEmpty();
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }
}
