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
package io.github.xponer2.translator.component;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code PatchedDataComponentMap}: a stack's components (its item's defaults with the stack's own values), what
 * {@code stack.components} is. Read from a stack it is a live view: what is set or removed here is set on the stack
 * ({@link StackComponents}). Made with {@code new} it holds its own values. Sophisticated Backpacks reads
 * {@code stack.components.asPatch()} to save every slot; without it a slot's stack was lost when it was clicked.
 */
public final class PatchedDataComponentMap implements Iterable<TypedDataComponent<?>> {
	@Nullable
	private final ItemStack stack;
	private final DataComponentMap prototype;
	private final Map<DataComponentType<?>, Object> own;

	/** {@code stack.components}: the stack's live view. */
	public static PatchedDataComponentMap of(ItemStack stack) {
		return new PatchedDataComponentMap(stack);
	}

	private PatchedDataComponentMap(ItemStack stack) {
		this.stack = stack;
		this.prototype = DataComponentMap.EMPTY;
		this.own = Collections.emptyMap();
	}

	/** 1.21 {@code new PatchedDataComponentMap(prototype)}: the prototype's values, none of its own yet. */
	public PatchedDataComponentMap(DataComponentMap prototype) {
		this.stack = null;
		this.prototype = prototype;
		this.own = new LinkedHashMap<>();
	}

	public static PatchedDataComponentMap fromPatch(DataComponentMap prototype, DataComponentPatch patch) {
		var map = new PatchedDataComponentMap(prototype);
		map.applyPatch(patch);
		return map;
	}

	private DataComponentMap view() {
		if (this.stack != null) {
			return StackComponents.getComponents(this.stack);
		}
		var values = new LinkedHashMap<DataComponentType<?>, Object>();
		for (var type : this.prototype.keySet()) {
			values.put(type, this.prototype.get(type));
		}
		values.putAll(this.own);
		return new DataComponentMap(Collections.unmodifiableMap(values));
	}

	@Nullable
	public <T> T get(DataComponentType<? extends T> type) {
		return this.view().get(type);
	}

	@Nullable
	public <T> T get(Supplier<? extends DataComponentType<? extends T>> type) {
		return this.get(type.get());
	}

	public <T> T getOrDefault(DataComponentType<? extends T> type, T defaultValue) {
		T value = this.get(type);
		return value != null ? value : defaultValue;
	}

	public boolean has(DataComponentType<?> type) {
		return this.get(type) != null;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T set(DataComponentType<? super T> type, @Nullable T value) {
		if (this.stack != null) {
			return (T) StackComponents.set(this.stack, (DataComponentType<T>) type, value);
		}
		T previous = (T) this.get(type);
		if (value == null) {
			this.own.remove(type);
		} else {
			this.own.put(type, value);
		}
		return previous;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T remove(DataComponentType<? extends T> type) {
		if (this.stack != null) {
			return StackComponents.remove(this.stack, type);
		}
		return (T) this.own.remove(type);
	}

	public void applyPatch(DataComponentPatch patch) {
		if (this.stack != null) {
			StackComponents.applyComponents(this.stack, patch);
		} else {
			this.own.putAll(patch.values());
		}
	}

	/** Back to the prototype and then the patch (1.12.2 stacks keep their values: the patch is applied over them). */
	public void restorePatch(DataComponentPatch patch) {
		if (this.stack == null) {
			this.own.clear();
		}
		this.applyPatch(patch);
	}

	public void setAll(DataComponentMap components) {
		for (var type : components.keySet()) {
			this.setUnchecked(type, components.get(type));
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void setUnchecked(DataComponentType type, Object value) {
		this.set(type, value);
	}

	public void clearPatch() {
		if (this.stack != null) {
			for (var type : StackComponents.getComponentsPatch(this.stack).values().keySet()) {
				StackComponents.remove(this.stack, type);
			}
		} else {
			this.own.clear();
		}
	}

	/** The values of its own (what differs from the item's defaults): a stack's saved components. */
	public DataComponentPatch asPatch() {
		if (this.stack != null) {
			return StackComponents.getComponentsPatch(this.stack);
		}
		return this.own.isEmpty() ? DataComponentPatch.EMPTY : new DataComponentPatch(Collections.unmodifiableMap(new LinkedHashMap<>(this.own)));
	}

	public PatchedDataComponentMap copy() {
		var copy = new PatchedDataComponentMap(this.stack != null ? DataComponentMap.EMPTY : this.prototype);
		if (this.stack != null) {
			copy.setAll(this.view());
		} else {
			copy.own.putAll(this.own);
		}
		return copy;
	}

	public DataComponentMap toImmutableMap() {
		return this.view();
	}

	public Set<DataComponentType<?>> keySet() {
		return this.view().keySet();
	}

	public int size() {
		return this.view().size();
	}

	public boolean isEmpty() {
		return this.view().isEmpty();
	}

	@Override
	public Iterator<TypedDataComponent<?>> iterator() {
		return this.view().iterator();
	}

	@Override
	public String toString() {
		return this.view().toString();
	}
}
