/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge 21's {@code ConfigurationScreen} (a mod's config, edited in game) on 1.20.4's config system: every value of
 * the mod's configs (ModConfigSpec), each with its translated name and comment, as a switch (booleans), a cycle (enums)
 * or a text field (numbers and text, checked against the value's own range and type); Done sets and saves the values
 * that changed. Lists and other kinds are shown with their value, to be edited in the config file.
 */
public class ConfigurationScreen extends Screen {
    private final ModContainer mod;
    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private ValueList list;

    public ConfigurationScreen(ModContainer mod, Screen parent) {
        super(Component.translatable("neoforge.configuration.uitext.title", mod.getModInfo().getDisplayName()));
        this.mod = mod;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.rows.clear();
        for (Map.Entry<ModConfig.Type, Set<ModConfig>> set : ConfigTracker.INSTANCE.configSets().entrySet()) {
            for (ModConfig config : set.getValue()) {
                // a server's config is the server's: edited here only in this player's own world, not published (as 1.21)
                boolean serverHere = this.minecraft.getSingleplayerServer() != null && !this.minecraft.getSingleplayerServer().isPublished();
                if (set.getKey() == ModConfig.Type.SERVER && !serverHere) {
                    continue;
                }
                if (config.getModId().equals(this.mod.getModId()) && config.getSpec() instanceof ModConfigSpec spec && spec.isLoaded()) {
                    this.collect(spec, spec.getValues(), set.getKey().name().toLowerCase(java.util.Locale.ROOT));
                }
            }
        }
        this.list = new ValueList();
        this.rows.forEach(row -> this.list.add(row));
        this.addRenderableWidget(this.list);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    private void collect(ModConfigSpec spec, UnmodifiableConfig values, String section) {
        for (UnmodifiableConfig.Entry entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig inner) {
                this.collect(spec, inner, section + "." + entry.getKey());
            } else if (value instanceof ModConfigSpec.ConfigValue<?> configValue) {
                Object found = spec.getSpec().get(configValue.getPath());
                if (found instanceof ModConfigSpec.ValueSpec valueSpec) {
                    this.rows.add(new Row(spec, configValue, valueSpec, section));
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        if (this.rows.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal("No values to edit"), this.width / 2, this.height / 2, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        java.util.Set<ModConfigSpec> changed = new java.util.HashSet<>();
        for (Row row : this.rows) {
            if (row.apply()) {
                changed.add(row.spec);
            }
        }
        changed.forEach(ModConfigSpec::save);
        this.minecraft.setScreen(this.parent);
    }

    /** one value: its name, its widget, the value it would be set to */
    final class Row extends ContainerObjectSelectionList.Entry<Row> {
        final ModConfigSpec spec;
        final ModConfigSpec.ConfigValue<Object> value;
        final ModConfigSpec.ValueSpec valueSpec;
        final Component name;
        final AbstractWidget widget;
        Object edited;

        @SuppressWarnings("unchecked")
        Row(ModConfigSpec spec, ModConfigSpec.ConfigValue<?> value, ModConfigSpec.ValueSpec valueSpec, String section) {
            this.spec = spec;
            this.value = (ModConfigSpec.ConfigValue<Object>) value;
            this.valueSpec = valueSpec;
            String key = valueSpec.getTranslationKey();
            String path = String.join(".", value.getPath());
            this.name = Component.literal(key != null && I18n.exists(key) ? I18n.get(key) : path);
            Object current = value.get();
            this.edited = current;
            int width = 120;
            if (current instanceof Boolean flag) {
                this.widget = CycleButton.onOffBuilder(flag).displayOnlyValue().create(0, 0, width, 20, this.name, (button, v) -> this.edited = v);
            } else if (current instanceof Enum<?> choice) {
                Object[] choices = choice.getDeclaringClass().getEnumConstants();
                this.widget = CycleButton.builder((Object v) -> Component.literal(v.toString())).withValues(choices).withInitialValue(choice)
                        .displayOnlyValue().create(0, 0, width, 20, this.name, (button, v) -> this.edited = v);
            } else if (current instanceof Number || current instanceof String) {
                EditBox box = new EditBox(ConfigurationScreen.this.font, 0, 0, width, 20, this.name);
                box.setMaxLength(1024);
                box.setValue(String.valueOf(current));
                box.setResponder(text -> {
                    Object parsed = this.parse(text, current);
                    boolean ok = parsed != null && this.valueSpec.test(parsed);
                    box.setTextColor(ok ? 0xE0E0E0 : 0xFF5555);
                    this.edited = ok ? parsed : current;
                });
                this.widget = box;
            } else {
                this.widget = Button.builder(Component.literal(String.valueOf(current)), button -> {
                }).bounds(0, 0, width, 20).build();
                this.widget.active = false;
            }
            String comment = valueSpec.getComment();
            if (comment != null && !comment.isEmpty()) {
                this.widget.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(comment)));
            }
        }

        private Object parse(String text, Object like) {
            try {
                if (like instanceof Integer) {
                    return Integer.parseInt(text.trim());
                } else if (like instanceof Long) {
                    return Long.parseLong(text.trim());
                } else if (like instanceof Double) {
                    return Double.parseDouble(text.trim());
                } else if (like instanceof Float) {
                    return Float.parseFloat(text.trim());
                }
                return text;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /** sets the value when it changed; true then */
        boolean apply() {
            if (this.edited == null || this.edited.equals(this.value.get())) {
                return false;
            }
            this.value.set(this.edited);
            return true;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.drawString(ConfigurationScreen.this.font, this.name, left, top + 6, 0xFFFFFF);
            this.widget.setX(left + width - this.widget.getWidth());
            this.widget.setY(top);
            this.widget.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }
    }

    final class ValueList extends ContainerObjectSelectionList<Row> {
        ValueList() {
            super(ConfigurationScreen.this.minecraft, ConfigurationScreen.this.width, ConfigurationScreen.this.height - 64, 32, 24);
        }

        void add(Row row) {
            this.addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return Math.min(400, this.width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + this.getRowWidth() / 2 + 6;
        }
    }
}
