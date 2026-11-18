/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Function;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.service.*;

/** A Mixin host service outside a game: class bytes come from the jars the trial reads (no class loading, no transforming). */
public class OfflineMixinService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
    static volatile Function<String, ClassNode> lookup = name -> null;

    @Override
    public String getName() {
        return "Offline";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return List.of();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleVirtual("offline");
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return OfflineMixinService.class.getClassLoader().getResourceAsStream(name);
    }

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, OfflineMixinService.class.getClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return findClass(name, initialize);
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException {
        return getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException {
        ClassNode node = lookup.apply(name.replace('.', '/'));
        if (node == null) {
            throw new ClassNotFoundException(name);
        }
        return node;
    }
}
