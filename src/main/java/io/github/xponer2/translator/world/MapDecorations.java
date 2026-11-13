/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.world;

import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.util.BuiltInRegistries;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.MapDecoration;

/**
 * 1.21's {@code MapDecoration} record (a type holder, x, y, rotation, an optional name) on 1.12.2's MapDecoration (a
 * type enum, x, y, rotation): R line helpers and the factory of {@code new MapDecoration(...)}. A 1.21 decoration is a
 * {@link Typed}, keeping its type and name; its 1.12.2 icon is the vanilla type of the same name, else a target point.
 * Twilight Forest's codecs made one at class init: they failed, and with them every codec of its Codecs class.
 */
public final class MapDecorations {
	private static final ResourceKey<Registry<Object>> TYPES = ResourceKey.createRegistryKey(new ResourceLocation("map_decoration_type"));

	private MapDecorations() {
	}

	/** A 1.21 decoration: its type holder and name kept. */
	public static final class Typed extends MapDecoration {
		private final Holder<?> holder;
		private final Optional<ITextComponent> name;

		Typed(Holder<?> holder, byte x, byte y, byte rotation, Optional<ITextComponent> name) {
			super(legacy(holder), x, y, rotation);
			this.holder = holder;
			this.name = name == null ? Optional.empty() : name;
		}
	}

	/** The vanilla 1.12.2 type of the same name as the 1.21 type (player, frame, red_marker...), else a target point. */
	private static MapDecoration.Type legacy(@Nullable Holder<?> holder) {
		ResourceLocation id = holder == null ? null : holder.unwrapKey().map(ResourceKey::location).orElse(null);
		if (id != null && "minecraft".equals(id.getNamespace())) {
			try {
				return MapDecoration.Type.valueOf(id.getPath().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				// a 1.21 type 1.12.2 hasn't (a banner, a village)
			}
		}
		return MapDecoration.Type.TARGET_POINT;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static MapDecoration newMapDecoration(Holder type, byte x, byte y, byte rotation, Optional name) {
		return new Typed(type, x, y, rotation, name);
	}

	/** 1.21 {@code type()}: the holder of a 1.21 decoration, else the registered type named as the 1.12.2 one. */
	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Holder type(MapDecoration decoration) {
		if (decoration instanceof Typed typed) {
			return typed.holder;
		}
		Registry registry = BuiltInRegistries.byKey(TYPES);
		return (Holder) registry.getHolder(new ResourceLocation(decoration.getType().name().toLowerCase(Locale.ROOT))).orElse(null);
	}

	public static byte x(MapDecoration decoration) {
		return decoration.getX();
	}

	public static byte y(MapDecoration decoration) {
		return decoration.getY();
	}

	public static byte rot(MapDecoration decoration) {
		return decoration.getRotation();
	}

	@SuppressWarnings({"rawtypes"})
	public static Optional name(MapDecoration decoration) {
		return decoration instanceof Typed typed ? typed.name : Optional.empty();
	}
}
