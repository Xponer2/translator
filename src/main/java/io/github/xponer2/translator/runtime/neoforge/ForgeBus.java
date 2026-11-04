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
package io.github.xponer2.translator.runtime.neoforge;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;

/** {@link IEventBus} on a 1.12.2 Forge event bus: listeners go straight into the event's listener list. */
public final class ForgeBus implements IEventBus {
	static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	private static final String SUBSCRIBE_EVENT = "Lnet/minecraftforge/fml/common/eventhandler/SubscribeEvent;";
	public static final ForgeBus FORGE = new ForgeBus(MinecraftForge.EVENT_BUS);

	private final EventBus bus;
	private final int busId;

	public ForgeBus(EventBus bus) {
		this.bus = bus;
		try {
			Field field = EventBus.class.getDeclaredField("busID");
			field.setAccessible(true);
			this.busId = field.getInt(bus);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Forge event bus without busID", e);
		}
	}

	/**
	 * The {@code @SubscribeEvent} methods of an object (or the static ones of a class), each as a guarded listener:
	 * an original 1.21.1 mod's handler that fails is logged, not passed on to the game.
	 */
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void register(Object target) {
		boolean statics = target instanceof Class<?>;
		Class<?> type = statics ? (Class<?>) target : target.getClass();
		for (Class<?> c = type; c != null && c != Object.class; c = statics ? null : c.getSuperclass()) {
			java.lang.reflect.Method[] methods;
			try {
				methods = c.getDeclaredMethods();
			} catch (NoClassDefFoundError e) {
				// a method of the class uses a 1.21 class 1.12.2 lacks (often an event): the handlers one by one
				this.registerEach(c, statics ? null : target, statics);
				continue;
			}
			for (java.lang.reflect.Method method : methods) {
				var subscribe = method.getAnnotation(net.minecraftforge.fml.common.eventhandler.SubscribeEvent.class);
				if (subscribe == null || method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])
						|| statics != java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				method.setAccessible(true);
				Object receiver = statics ? null : target;
				Class eventType = method.getParameterTypes()[0];
				this.addListener(subscribe.priority(), subscribe.receiveCanceled(), eventType, (Consumer) event -> {
					try {
						method.invoke(receiver, event);
					} catch (java.lang.reflect.InvocationTargetException e) {
						throw new RuntimeException(e.getCause());
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				});
			}
		}
	}

	/**
	 * The {@code @SubscribeEvent} methods of a class whose method table can't be loaded, read from its bytecode: a
	 * handler whose event class is missing is left out (that event never fires here), the others are called through
	 * method handles (public handlers only; NeoForge handlers are public by convention).
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void registerEach(Class<?> type, Object receiver, boolean statics) {
		ClassLoader loader = type.getClassLoader();
		ClassNode node = new ClassNode();
		try (InputStream in = loader.getResourceAsStream(type.getName().replace('.', '/') + ".class")) {
			new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Can't read the event handlers of {}: {}", type.getName(), e.toString());
			return;
		}
		for (MethodNode method : node.methods) {
			AnnotationNode subscribe = method.visibleAnnotations == null ? null : method.visibleAnnotations.stream()
					.filter(a -> a.desc.equals(SUBSCRIBE_EVENT)).findFirst().orElse(null);
			Type[] params = Type.getArgumentTypes(method.desc);
			if (subscribe == null || params.length != 1 || statics != ((method.access & Opcodes.ACC_STATIC) != 0)) {
				continue;
			}
			Class<?> eventType;
			MethodHandle handle;
			try {
				eventType = Class.forName(params[0].getClassName(), false, loader);
				Type returned = Type.getReturnType(method.desc);
				Class<?> returnType = returned.getSort() == Type.VOID ? void.class : Class.forName(returned.getClassName(), false, loader);
				MethodType methodType = MethodType.methodType(returnType, eventType);
				handle = statics ? MethodHandles.publicLookup().findStatic(type, method.name, methodType)
						: MethodHandles.publicLookup().findVirtual(type, method.name, methodType).bindTo(receiver);
			} catch (ReflectiveOperationException | LinkageError e) {
				LOGGER.warn("{}.{} not registered: {}", type.getName(), method.name, e.toString());
				continue;
			}
			if (!Event.class.isAssignableFrom(eventType)) {
				continue;
			}
			EventPriority priority = EventPriority.NORMAL;
			boolean receiveCanceled = false;
			for (int i = 0; subscribe.values != null && i + 1 < subscribe.values.size(); i += 2) {
				Object value = subscribe.values.get(i + 1);
				if ("priority".equals(subscribe.values.get(i)) && value instanceof String[]) {
					priority = EventPriority.valueOf(((String[]) value)[1]);
				} else if ("receiveCanceled".equals(subscribe.values.get(i)) && value instanceof Boolean) {
					receiveCanceled = (Boolean) value;
				}
			}
			MethodHandle call = handle;
			this.addListener(priority, receiveCanceled, (Class) eventType, (Consumer) event -> {
				try {
					call.invoke(event);
				} catch (RuntimeException | Error e) {
					throw e;
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
			});
		}
	}

	@Override
	public void unregister(Object target) {
		this.bus.unregister(target);
	}

	@Override
	public <T extends Event> T post(T event) {
		this.bus.post(event);
		return event;
	}

	@Override
	public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> type, Consumer<T> consumer) {
		if (type == null || consumer == null) {
			// an optional mod's event (its class isn't there) or a listener the converter left out: nothing to listen to
			return;
		}
		try {
			// what EventBus#register does: FML gives every event class a no-arg constructor for this
			Event instance = type.getConstructor().newInstance();
			boolean[] reported = new boolean[1];
			IEventListener listener = event -> {
				if (receiveCanceled || !event.isCancelable() || !event.isCanceled()) {
					try {
						consumer.accept(type.cast(event));
					} catch (Throwable t) {
						// a listener of an original 1.21.1 mod: logged once, the game goes on
						if (!reported[0]) {
							reported[0] = true;
							LOGGER.error("A listener for {} failed (further errors of it are not logged)", type.getSimpleName(), t);
						}
					}
				}
			};
			instance.getListenerList().register(this.busId, priority, listener);
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.error("Can't listen to {}: {}", type.getName(), e.toString());
		}
	}
}
