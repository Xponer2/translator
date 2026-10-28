package io.github.xponer2.translator.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A mod config (1.21 NeoForge {@code ModConfigSpec}) stored as a 1.12.2 Forge {@code .cfg} file: the pushed sections
 * are the categories, the comments go to the categories and values. {@link #register(String)} loads the file
 * (writing missing values with their defaults); the values are read with {@code get()} afterwards.
 */
public final class ModConfigSpec {
    private static final Logger LOGGER = LogManager.getLogger("Cucumber Config");

    private final List<ConfigValue<?>> values;
    private final List<String[]> categoryComments;
    private Configuration configuration;

    private ModConfigSpec(List<ConfigValue<?>> values, List<String[]> categoryComments) {
        this.values = values;
        this.categoryComments = categoryComments;
    }

    /**
     * Loads {@code config/<fileName>} (1.21 {@code registerConfig(type, spec, "x.toml")}: the same name with .cfg).
     */
    public void register(String fileName) {
        var name = fileName.endsWith(".toml") ? fileName.substring(0, fileName.length() - 5) + ".cfg" : fileName;
        this.load(new File(Loader.instance().getConfigDir(), name));
    }

    public synchronized void load(File file) {
        this.configuration = new Configuration(file);
        for (var comment : this.categoryComments) {
            this.configuration.setCategoryComment(comment[0], comment[1]);
        }
        for (var value : this.values) {
            value.load(this.configuration);
        }
        if (this.configuration.hasChanged()) {
            this.configuration.save();
        }
    }

    public boolean isLoaded() {
        return this.configuration != null;
    }

    public void save() {
        if (this.configuration != null) {
            for (var value : this.values) {
                value.store(this.configuration);
            }
            this.configuration.save();
        }
    }

    public static final class Builder {
        private final List<String> path = new ArrayList<>();
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String[]> categoryComments = new ArrayList<>();
        private String comment;

        public Builder comment(String... comment) {
            this.comment = String.join("\n", comment);
            return this;
        }

        public Builder translation(String key) {
            return this;
        }

        public Builder worldRestart() {
            return this;
        }

        public Builder gameRestart() {
            return this;
        }

        public Builder push(String path) {
            this.path.addAll(Arrays.asList(path.split("\\.")));
            if (this.comment != null) {
                this.categoryComments.add(new String[] { this.category(), this.comment });
                this.comment = null;
            }
            return this;
        }

        public Builder pop() {
            return this.pop(1);
        }

        public Builder pop(int count) {
            for (int i = 0; i < count; i++) {
                this.path.remove(this.path.size() - 1);
            }
            return this;
        }

        private String category() {
            return this.path.isEmpty() ? Configuration.CATEGORY_GENERAL : String.join(".", this.path).toLowerCase(java.util.Locale.ROOT);
        }

        private <V extends ConfigValue<?>> V add(V value) {
            this.values.add(value);
            this.comment = null;
            return value;
        }

        public BooleanValue define(String name, boolean defaultValue) {
            return this.add(new BooleanValue(this.category(), name, this.comment, defaultValue));
        }

        @SuppressWarnings("unchecked")
        public <T> ConfigValue<T> define(String name, T defaultValue) {
            if (defaultValue instanceof List<?>) {
                return (ConfigValue<T>) this.add(new StringListValue(this.category(), name, this.comment, (List<String>) defaultValue));
            }
            if (defaultValue instanceof String s) {
                return (ConfigValue<T>) this.add(new StringValue(this.category(), name, this.comment, s));
            }
            throw new IllegalArgumentException("Unsupported config value type " + defaultValue.getClass() + " of " + name);
        }

        public <T> ConfigValue<List<? extends T>> defineList(String name, List<? extends T> defaultValue, java.util.function.Predicate<Object> validator) {
            @SuppressWarnings("unchecked")
            var value = (ConfigValue<List<? extends T>>) (ConfigValue<?>) this.add(new StringListValue(this.category(), name, this.comment, toStrings(defaultValue)));
            return value;
        }

        public IntValue defineInRange(String name, int defaultValue, int min, int max) {
            return this.add(new IntValue(this.category(), name, this.comment, defaultValue, min, max));
        }

        public DoubleValue defineInRange(String name, double defaultValue, double min, double max) {
            return this.add(new DoubleValue(this.category(), name, this.comment, defaultValue, min, max));
        }

        public LongValue defineInRange(String name, long defaultValue, long min, long max) {
            return this.add(new LongValue(this.category(), name, this.comment, defaultValue, min, max));
        }

        public ModConfigSpec build() {
            return new ModConfigSpec(Collections.unmodifiableList(new ArrayList<>(this.values)), this.categoryComments);
        }

        private static List<String> toStrings(List<?> list) {
            var result = new ArrayList<String>();
            for (var o : list) {
                result.add(String.valueOf(o));
            }
            return result;
        }
    }

    /**
     * A config value; until the file is loaded it is the default.
     */
    public abstract static class ConfigValue<T> implements Supplier<T> {
        protected final String category;
        protected final String name;
        protected final String comment;
        protected final T defaultValue;
        protected T value;

        ConfigValue(String category, String name, String comment, T defaultValue) {
            this.category = category;
            this.name = name;
            this.comment = comment == null ? "" : comment;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        abstract void load(Configuration configuration);

        abstract void store(Configuration configuration);

        @Override
        public T get() {
            return this.value;
        }

        public T getDefault() {
            return this.defaultValue;
        }

        public void set(T value) {
            this.value = value;
        }

        public List<String> getPath() {
            var path = new ArrayList<>(Arrays.asList(this.category.split("\\.")));
            path.add(this.name);
            return path;
        }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> implements java.util.function.BooleanSupplier {
        BooleanValue(String category, String name, String comment, boolean defaultValue) {
            super(category, name, comment, defaultValue);
        }

        @Override
        void load(Configuration configuration) {
            this.value = configuration.get(this.category, this.name, this.defaultValue, this.comment).getBoolean();
        }

        @Override
        void store(Configuration configuration) {
            configuration.get(this.category, this.name, this.defaultValue, this.comment).set(this.value);
        }

        public boolean getAsBoolean() {
            return this.value;
        }
    }

    public static final class IntValue extends ConfigValue<Integer> implements java.util.function.IntSupplier {
        private final int min;
        private final int max;

        IntValue(String category, String name, String comment, int defaultValue, int min, int max) {
            super(category, name, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        void load(Configuration configuration) {
            var property = configuration.get(this.category, this.name, this.defaultValue, this.comment, this.min, this.max);
            this.value = Math.max(this.min, Math.min(this.max, property.getInt()));
        }

        @Override
        void store(Configuration configuration) {
            configuration.get(this.category, this.name, this.defaultValue, this.comment, this.min, this.max).set(this.value);
        }

        public int getAsInt() {
            return this.value;
        }
    }

    public static final class LongValue extends ConfigValue<Long> implements java.util.function.LongSupplier {
        private final long min;
        private final long max;

        LongValue(String category, String name, String comment, long defaultValue, long min, long max) {
            super(category, name, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        void load(Configuration configuration) {
            var property = configuration.get(this.category, this.name, Long.toString(this.defaultValue), this.comment + " [range: " + this.min + " ~ " + this.max + "]");
            long parsed;
            try {
                parsed = Long.parseLong(property.getString().trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Config {}.{}: {} is not a number, using {}", this.category, this.name, property.getString(), this.defaultValue);
                parsed = this.defaultValue;
            }
            this.value = Math.max(this.min, Math.min(this.max, parsed));
        }

        @Override
        void store(Configuration configuration) {
            configuration.get(this.category, this.name, Long.toString(this.defaultValue), this.comment).set(Long.toString(this.value));
        }

        public long getAsLong() {
            return this.value;
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> implements java.util.function.DoubleSupplier {
        private final double min;
        private final double max;

        DoubleValue(String category, String name, String comment, double defaultValue, double min, double max) {
            super(category, name, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        void load(Configuration configuration) {
            var property = configuration.get(this.category, this.name, this.defaultValue, this.comment, this.min, this.max);
            this.value = Math.max(this.min, Math.min(this.max, property.getDouble()));
        }

        @Override
        void store(Configuration configuration) {
            configuration.get(this.category, this.name, this.defaultValue, this.comment, this.min, this.max).set(this.value);
        }

        public double getAsDouble() {
            return this.value;
        }
    }

    public static final class StringValue extends ConfigValue<String> {
        StringValue(String category, String name, String comment, String defaultValue) {
            super(category, name, comment, defaultValue);
        }

        @Override
        void load(Configuration configuration) {
            this.value = configuration.get(this.category, this.name, this.defaultValue, this.comment).getString();
        }

        @Override
        void store(Configuration configuration) {
            configuration.get(this.category, this.name, this.defaultValue, this.comment).set(this.value);
        }
    }

    public static final class StringListValue extends ConfigValue<List<String>> {
        StringListValue(String category, String name, String comment, List<String> defaultValue) {
            super(category, name, comment, Collections.unmodifiableList(new ArrayList<>(defaultValue)));
        }

        @Override
        void load(Configuration configuration) {
            var values = configuration.get(this.category, this.name, this.defaultValue.toArray(new String[0]), this.comment).getStringList();
            this.value = Collections.unmodifiableList(Arrays.asList(values));
        }

        @Override
        void store(Configuration configuration) {
            configuration.get(this.category, this.name, this.defaultValue.toArray(new String[0]), this.comment).set(this.value.toArray(new String[0]));
        }
    }
}
