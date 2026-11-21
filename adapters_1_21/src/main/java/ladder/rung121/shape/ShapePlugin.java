/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.shape;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * The 1.21 adapters' mixin config plugin: the shape patches ({@link ShapePatches}) are applied to their classes in the
 * pre-apply of the empty ShapeTargetsMixin. Mixin pre-applies every mixin of a class before it applies any, so the
 * mods' mixins find the patched class.
 */
public final class ShapePlugin implements IMixinConfigPlugin {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ladder");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!mixinClassName.endsWith(".ShapeTargetsMixin")) {
            return;
        }
        for (String why : ShapePatches.apply(targetClass, ShapePatches.load())) {
            if (why != null) {
                // the converter checked every patch on 21.0 (where a failure stops the conversion); below, in a lower
                // game these adapters were converted to, a call site that isn't there leaves the game's code as it is
                LOGGER.warn("shape patch on {} not applied: {}", targetClassName, why);
            }
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
